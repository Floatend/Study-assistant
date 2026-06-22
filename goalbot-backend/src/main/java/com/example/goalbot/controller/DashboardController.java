package com.example.goalbot.controller;

import com.example.goalbot.common.BaseController;
import com.example.goalbot.common.Result;
import com.example.goalbot.service.DashboardService;
import com.example.goalbot.vo.DashboardVO;
import com.example.goalbot.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class DashboardController extends BaseController {

    private final DashboardService dashboardService;

    @GetMapping
    public Result<DashboardVO> getDashboard(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(defaultValue = "2") Integer adviceDays) {
        return Result.success(dashboardService.getDashboard(currentUserId(headerUserId), adviceDays));
    }

    @PostMapping("/advice/refresh")
    public Result<ReviewVO> refreshAdvice(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(defaultValue = "2") Integer adviceDays) {
        return Result.success(dashboardService.refreshAdvice(currentUserId(headerUserId), adviceDays));
    }
}
