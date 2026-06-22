package com.example.goalbot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.goalbot.entity.Notification;

public interface NotificationService extends IService<Notification> {

    Notification sendDailyTaskReminder(Long userId);

    Notification sendDailyTaskReminder(Long userId, String feishuChatId);

    Notification sendDailyReviewReminder(Long userId);

    Notification sendDailyReviewReminder(Long userId, String feishuChatId);

    Notification sendWeeklyReviewReminder(Long userId);

    Notification sendWeeklyReviewReminder(Long userId, String feishuChatId);

    Notification sendPeriodicPlanningNudge(Long userId, String feishuChatId);

    Notification sendCustomMessage(Long userId, String title, String content);

    Notification sendCustomMessage(Long userId, String title, String content, String feishuChatId);
}
