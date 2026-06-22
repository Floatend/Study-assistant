package com.example.goalbot.service.impl;

import com.example.goalbot.common.BusinessException;
import com.example.goalbot.entity.AssistantSettings;
import com.example.goalbot.entity.Notification;
import com.example.goalbot.integration.content.ContentArticle;
import com.example.goalbot.integration.content.ContentSourceClient;
import com.example.goalbot.service.AiBriefingService;
import com.example.goalbot.service.DifyService;
import com.example.goalbot.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiBriefingServiceImpl implements AiBriefingService {

    private final ContentSourceClient contentSourceClient;
    private final DifyService difyService;
    private final NotificationService notificationService;

    @Override
    public Notification sendDailyBriefing(Long userId, AssistantSettings settings) {
        if (settings == null || !StringUtils.hasText(settings.getAiBriefingSourceUrl())) {
            throw BusinessException.badRequest("请先在配置页填写每日 AI 资讯源 URL");
        }
        String sourceName = StringUtils.hasText(settings.getAiBriefingSourceName())
                ? settings.getAiBriefingSourceName().trim()
                : "AI 资讯源";
        ContentArticle article = contentSourceClient.fetchLatest(sourceName, settings.getAiBriefingSourceUrl());
        String briefing = difyService.generateArticleBriefing(
                userId,
                article.getSourceName(),
                article.getTitle(),
                article.getUrl(),
                article.getContent()
        );
        String content = buildMessage(article, briefing);
        return notificationService.sendCustomMessage(userId, "每日 AI 资讯 - " + sourceName,
                content, settings.getFeishuChatId());
    }

    private String buildMessage(ContentArticle article, String briefing) {
        String title = StringUtils.hasText(article.getTitle()) ? article.getTitle() : "今日文章";
        String url = StringUtils.hasText(article.getUrl()) ? article.getUrl() : "无";
        return """
                来源：%s
                文章：%s
                链接：%s

                %s
                """.formatted(article.getSourceName(), title, url, briefing == null ? "" : briefing.trim());
    }
}
