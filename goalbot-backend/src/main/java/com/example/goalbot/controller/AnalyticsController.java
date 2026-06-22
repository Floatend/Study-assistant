package com.example.goalbot.controller;

import com.example.goalbot.common.BaseController;
import com.example.goalbot.common.Result;
import com.example.goalbot.service.AnalyticsService;
import com.example.goalbot.service.CheckinService;
import com.example.goalbot.vo.CheckinVO;
import com.example.goalbot.vo.TaskStatusCountVO;
import com.example.goalbot.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analytics")
public class AnalyticsController extends BaseController {

    private final AnalyticsService analyticsService;
    private final CheckinService checkinService;

    @GetMapping("/study-duration")
    public Result<List<TrendPointVO>> getStudyDuration(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(analyticsService.getStudyDuration(currentUserId(headerUserId), startDate, endDate));
    }

    @GetMapping("/task-status")
    public Result<List<TaskStatusCountVO>> getTaskStatus(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(analyticsService.getTaskStatus(currentUserId(headerUserId), startDate, endDate));
    }

    @GetMapping("/recent-checkins")
    public Result<List<CheckinVO>> getRecentCheckins(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(required = false) Integer limit) {
        return Result.success(checkinService.listRecent(currentUserId(headerUserId), limit));
    }
}
