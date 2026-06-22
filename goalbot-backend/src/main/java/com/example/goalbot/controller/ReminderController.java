package com.example.goalbot.controller;

import com.example.goalbot.common.BaseController;
import com.example.goalbot.common.Result;
import com.example.goalbot.entity.Notification;
import com.example.goalbot.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reminders")
public class ReminderController extends BaseController {

    private final NotificationService notificationService;

    @PostMapping("/daily-task")
    public Result<Notification> sendDailyTaskReminder(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        return Result.success(notificationService.sendDailyTaskReminder(currentUserId(headerUserId)));
    }

    @PostMapping("/daily-review")
    public Result<Notification> sendDailyReviewReminder(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        return Result.success(notificationService.sendDailyReviewReminder(currentUserId(headerUserId)));
    }

    @PostMapping("/weekly-review")
    public Result<Notification> sendWeeklyReviewReminder(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        return Result.success(notificationService.sendWeeklyReviewReminder(currentUserId(headerUserId)));
    }
}
