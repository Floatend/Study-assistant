package com.example.goalbot.integration.feishu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lark.oapi.service.im.v1.model.CreateMessageReq;
import com.lark.oapi.service.im.v1.model.CreateMessageReqBody;
import com.lark.oapi.service.im.v1.model.CreateMessageResp;
import com.lark.oapi.service.im.v1.model.ReplyMessageReq;
import com.lark.oapi.service.im.v1.model.ReplyMessageReqBody;
import com.lark.oapi.service.im.v1.model.ReplyMessageResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuOpenApiClient {

    private final FeishuProperties properties;
    private final ObjectMapper objectMapper;

    private volatile com.lark.oapi.Client client;

    public boolean isConfigured() {
        return StringUtils.hasText(properties.getAppId()) && StringUtils.hasText(properties.getAppSecret());
    }

    public boolean replyText(String messageId, String text) {
        if (!isConfigured() || !StringUtils.hasText(messageId)) {
            return false;
        }
        try {
            ReplyMessageReq request = ReplyMessageReq.newBuilder()
                    .messageId(messageId)
                    .replyMessageReqBody(ReplyMessageReqBody.newBuilder()
                            .msgType("text")
                            .content(textContent(text))
                            .build())
                    .build();
            ReplyMessageResp response = client().im().v1().message().reply(request);
            if (!response.success()) {
                log.warn("Feishu reply failed. code={}, msg={}, requestId={}",
                        response.getCode(), response.getMsg(), response.getRequestId());
            }
            return response.success();
        } catch (Exception ex) {
            log.warn("Feishu reply request failed: {}", ex.getMessage());
            return false;
        }
    }

    public boolean sendTextToChat(String chatId, String text) {
        if (!isConfigured() || !StringUtils.hasText(chatId)) {
            return false;
        }
        try {
            CreateMessageReq request = CreateMessageReq.newBuilder()
                    .receiveIdType("chat_id")
                    .createMessageReqBody(CreateMessageReqBody.newBuilder()
                            .receiveId(chatId)
                            .msgType("text")
                            .content(textContent(text))
                            .uuid(UUID.randomUUID().toString())
                            .build())
                    .build();
            CreateMessageResp response = client().im().v1().message().create(request);
            if (!response.success()) {
                log.warn("Feishu send message failed. code={}, msg={}, requestId={}",
                        response.getCode(), response.getMsg(), response.getRequestId());
            }
            return response.success();
        } catch (Exception ex) {
            log.warn("Feishu send message request failed: {}", ex.getMessage());
            return false;
        }
    }

    public boolean sendMarkdownCardToChat(String chatId, String title, String markdown) {
        if (!isConfigured() || !StringUtils.hasText(chatId)) {
            return false;
        }
        try {
            CreateMessageReq request = CreateMessageReq.newBuilder()
                    .receiveIdType("chat_id")
                    .createMessageReqBody(CreateMessageReqBody.newBuilder()
                            .receiveId(chatId)
                            .msgType("interactive")
                            .content(markdownCardContent(title, markdown))
                            .uuid(UUID.randomUUID().toString())
                            .build())
                    .build();
            CreateMessageResp response = client().im().v1().message().create(request);
            if (!response.success()) {
                log.warn("Feishu send markdown card failed. code={}, msg={}, requestId={}",
                        response.getCode(), response.getMsg(), response.getRequestId());
            }
            return response.success();
        } catch (Exception ex) {
            log.warn("Feishu send markdown card request failed: {}", ex.getMessage());
            return false;
        }
    }

    private com.lark.oapi.Client client() {
        com.lark.oapi.Client local = client;
        if (local == null) {
            synchronized (this) {
                local = client;
                if (local == null) {
                    local = com.lark.oapi.Client.newBuilder(properties.getAppId(), properties.getAppSecret()).build();
                    client = local;
                }
            }
        }
        return local;
    }

    private String textContent(String text) throws Exception {
        return objectMapper.writeValueAsString(Map.of("text", text == null ? "" : text));
    }

    private String markdownCardContent(String title, String markdown) throws Exception {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("config", Map.of("wide_screen_mode", true));
        card.put("header", Map.of(
                "template", "blue",
                "title", Map.of(
                        "tag", "plain_text",
                        "content", StringUtils.hasText(title) ? title : "GoalBot"
                )
        ));
        card.put("elements", List.of(
                Map.of(
                        "tag", "div",
                        "text", Map.of(
                                "tag", "lark_md",
                                "content", normalizeLarkMarkdown(markdown)
                        )
                )
        ));
        return objectMapper.writeValueAsString(card);
    }

    private String normalizeLarkMarkdown(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return "";
        }
        String normalized = markdown
                .replaceAll("(?m)^#{1,6}\\s+(.+)$", "**$1**")
                .replaceAll("(?m)^\\s*[-*]\\s+", "- ")
                .trim();
        int maxLength = 9000;
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength) + "\n\n[内容过长，已截断]";
    }
}
