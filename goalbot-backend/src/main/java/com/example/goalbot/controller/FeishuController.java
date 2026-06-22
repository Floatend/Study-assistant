package com.example.goalbot.controller;

import com.example.goalbot.common.BaseController;
import com.example.goalbot.common.Result;
import com.example.goalbot.dto.command.CommandIntent;
import com.example.goalbot.dto.feishu.FeishuCommandTestRequest;
import com.example.goalbot.integration.feishu.FeishuEventHandler;
import com.example.goalbot.service.FeishuCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feishu")
public class FeishuController extends BaseController {

    private final FeishuEventHandler feishuEventHandler;
    private final FeishuCommandService feishuCommandService;

    @PostMapping("/events")
    public Map<String, Object> handleEvents(@RequestBody Map<String, Object> payload) {
        return feishuEventHandler.handleEvent(payload);
    }

    @PostMapping("/command/test")
    public Result<String> testCommand(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestBody FeishuCommandTestRequest request) {
        return Result.success(feishuCommandService.handleText(currentUserId(headerUserId), request.getText()));
    }

    @PostMapping("/command/parse")
    public Result<CommandIntent> parseCommand(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestBody FeishuCommandTestRequest request) {
        return Result.success(feishuCommandService.parseText(currentUserId(headerUserId), request.getText()));
    }
}
