package com.example.goalbot.service.impl;

import com.example.goalbot.integration.feishu.FeishuOpenApiClient;
import com.example.goalbot.integration.feishu.FeishuProperties;
import com.example.goalbot.service.FeishuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FeishuServiceImpl implements FeishuService {

    private final FeishuOpenApiClient feishuOpenApiClient;
    private final FeishuProperties properties;

    @Override
    public boolean sendText(String content) {
        return sendTextToChat(null, content);
    }

    @Override
    public boolean sendTextToChat(String chatId, String content) {
        String targetChatId = StringUtils.hasText(chatId) ? chatId : properties.getDefaultChatId();
        if (!StringUtils.hasText(targetChatId)) {
            return false;
        }
        return feishuOpenApiClient.sendTextToChat(targetChatId, content);
    }

    @Override
    public boolean sendRichText(String title, String content) {
        return sendRichTextToChat(null, title, content);
    }

    @Override
    public boolean sendRichTextToChat(String chatId, String title, String content) {
        String targetChatId = StringUtils.hasText(chatId) ? chatId : properties.getDefaultChatId();
        if (!StringUtils.hasText(targetChatId)) {
            return false;
        }
        boolean sent = feishuOpenApiClient.sendMarkdownCardToChat(targetChatId, title, content);
        if (sent) {
            return true;
        }
        String message = StringUtils.hasText(title) ? title + "\n\n" + content : content;
        return feishuOpenApiClient.sendTextToChat(targetChatId, message);
    }
}
