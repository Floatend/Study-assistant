package com.example.goalbot.controller;

import com.example.goalbot.common.BaseController;
import com.example.goalbot.common.Result;
import com.example.goalbot.dto.settings.AssistantSettingsUpdateRequest;
import com.example.goalbot.entity.Notification;
import com.example.goalbot.service.AssistantSettingsService;
import com.example.goalbot.vo.AssistantSettingsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settings/assistant")
public class AssistantSettingsController extends BaseController {

    private final AssistantSettingsService assistantSettingsService;

    @GetMapping
    public Result<AssistantSettingsVO> getSettings(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        return Result.success(assistantSettingsService.getSettings(currentUserId(headerUserId)));
    }

    @PutMapping
    public Result<AssistantSettingsVO> updateSettings(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Valid @RequestBody AssistantSettingsUpdateRequest request) {
        return Result.success(assistantSettingsService.updateSettings(currentUserId(headerUserId), request));
    }

    @PostMapping("/test-message")
    public Result<Notification> sendTestMessage(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        return Result.success(assistantSettingsService.sendTestMessage(currentUserId(headerUserId)));
    }

    @PostMapping("/send-now/{type}")
    public Result<Notification> sendNow(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @PathVariable String type) {
        return Result.success(assistantSettingsService.sendNow(currentUserId(headerUserId), type));
    }
}
