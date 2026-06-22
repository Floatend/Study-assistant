package com.example.goalbot.controller;

import com.example.goalbot.common.BaseController;
import com.example.goalbot.common.Result;
import com.example.goalbot.dto.ai.DailyReviewRequest;
import com.example.goalbot.dto.ai.WeeklyReviewRequest;
import com.example.goalbot.service.DifyService;
import com.example.goalbot.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiController extends BaseController {

    private final DifyService difyService;

    @PostMapping("/advice")
    public Result<ReviewVO> generateAdvice(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(defaultValue = "2") Integer days) {
        return Result.success(difyService.generateAdvice(currentUserId(headerUserId), days));
    }

    @PostMapping("/daily-review")
    public Result<ReviewVO> generateDailyReview(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestBody(required = false) DailyReviewRequest request) {
        return Result.success(difyService.generateDailyReview(
                currentUserId(headerUserId),
                request == null ? null : request.getDate()
        ));
    }

    @PostMapping("/weekly-review")
    public Result<ReviewVO> generateWeeklyReview(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestBody(required = false) WeeklyReviewRequest request) {
        return Result.success(difyService.generateWeeklyReview(
                currentUserId(headerUserId),
                request == null ? null : request.getWeekStart(),
                request == null ? null : request.getWeekEnd()
        ));
    }
}
