package com.example.goalbot.integration.feishu;

import com.example.goalbot.service.FeishuCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.ws.Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuLongConnectionClient implements ApplicationRunner {

    private final FeishuProperties properties;
    private final FeishuCommandService feishuCommandService;
    private final FeishuOpenApiClient feishuOpenApiClient;
    private final FeishuUserResolver feishuUserResolver;
    private final FeishuMessageDeduplicator feishuMessageDeduplicator;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isLongConnectionEnabled()) {
            return;
        }
        if (!feishuOpenApiClient.isConfigured()) {
            log.warn("Feishu long connection is enabled, but FEISHU_APP_ID or FEISHU_APP_SECRET is empty.");
            return;
        }

        Thread thread = new Thread(this::startLongConnection, "feishu-long-connection");
        thread.setDaemon(true);
        thread.start();
    }

    private void startLongConnection() {
        try {
            Client client = new Client.Builder(properties.getAppId(), properties.getAppSecret())
                    .eventHandler(EventDispatcher.newBuilder(
                                    blankToEmpty(properties.getVerificationToken()),
                                    blankToEmpty(properties.getEncryptKey()))
                            .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                                @Override
                                public void handle(P2MessageReceiveV1 event) {
                                    handleMessage(event);
                                }
                            })
                            .build())
                    .autoReconnect(true)
                    .build();
            log.info("Feishu long connection client is starting.");
            client.start();
        } catch (Exception ex) {
            log.error("Feishu long connection client stopped unexpectedly: {}", ex.getMessage(), ex);
        }
    }

    private void handleMessage(P2MessageReceiveV1 event) {
        try {
            if (event == null || event.getEvent() == null || event.getEvent().getMessage() == null) {
                return;
            }

            var message = event.getEvent().getMessage();
            if (isBotMessage(event)) {
                log.debug("Ignore Feishu bot message. messageId={}", message.getMessageId());
                return;
            }
            if (!"text".equalsIgnoreCase(message.getMessageType())) {
                return;
            }
            if (!feishuMessageDeduplicator.markIfNew(message.getMessageId())) {
                log.info("Ignore duplicated Feishu message. messageId={}", message.getMessageId());
                return;
            }

            String text = FeishuMessageTextExtractor.extractText(message.getContent(), objectMapper);
            if (!StringUtils.hasText(text)) {
                return;
            }

            Long userId = resolveUserId(event);
            if (userId == null) {
                String openId = event.getEvent().getSender() == null
                        || event.getEvent().getSender().getSenderId() == null
                        ? null
                        : event.getEvent().getSender().getSenderId().getOpenId();
                feishuOpenApiClient.replyText(message.getMessageId(),
                        "你的飞书账号还没有绑定 GoalBot 用户。请让管理员在用户管理中填写飞书用户 ID："
                                + (StringUtils.hasText(openId) ? openId : "未能读取 open_id"));
                return;
            }
            log.info("Feishu app bot received text message. chatId={}, messageId={}",
                    message.getChatId(), message.getMessageId());
            String reply = feishuCommandService.handleText(userId, text, message.getMessageId());
            if (!StringUtils.hasText(reply)) {
                return;
            }

            feishuOpenApiClient.replyText(message.getMessageId(), reply);
        } catch (Exception ex) {
            log.warn("Failed to handle Feishu message event: {}", ex.getMessage(), ex);
        }
    }

    private boolean isBotMessage(P2MessageReceiveV1 event) {
        return event.getEvent().getSender() != null
                && "app".equalsIgnoreCase(event.getEvent().getSender().getSenderType());
    }

    private Long resolveUserId(P2MessageReceiveV1 event) {
        if (event.getEvent().getSender() == null || event.getEvent().getSender().getSenderId() == null) {
            return null;
        }
        var senderId = event.getEvent().getSender().getSenderId();
        return feishuUserResolver.resolveUserId(senderId.getUserId(), senderId.getOpenId(), senderId.getUnionId());
    }

    private String blankToEmpty(String value) {
        return StringUtils.hasText(value) ? value : "";
    }
}
