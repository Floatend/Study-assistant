package com.example.goalbot.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.goalbot.common.BaseController;
import com.example.goalbot.common.Result;
import com.example.goalbot.entity.Notification;
import com.example.goalbot.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController extends BaseController {

    private final NotificationService notificationService;

    @GetMapping
    public Result<List<Notification>> listNotifications(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(required = false) Integer status) {
        return Result.success(notificationService.list(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, currentUserId(headerUserId))
                .eq(status != null, Notification::getStatus, status)
                .orderByDesc(Notification::getNotifyTime)));
    }
}
