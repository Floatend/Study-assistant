package com.example.goalbot.controller;

import com.example.goalbot.common.BaseController;
import com.example.goalbot.common.Result;
import com.example.goalbot.dto.checkin.CheckinCreateRequest;
import com.example.goalbot.service.CheckinService;
import com.example.goalbot.vo.CheckinStatsVO;
import com.example.goalbot.vo.CheckinVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/checkins")
public class CheckinController extends BaseController {

    private final CheckinService checkinService;

    @PostMapping
    public Result<CheckinVO> createCheckin(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Valid @RequestBody CheckinCreateRequest request) {
        return Result.success(checkinService.createCheckin(currentUserId(headerUserId), request));
    }

    @GetMapping("/recent")
    public Result<List<CheckinVO>> listRecent(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(required = false) Integer limit) {
        return Result.success(checkinService.listRecent(currentUserId(headerUserId), limit));
    }

    @GetMapping("/stats")
    public Result<CheckinStatsVO> getStats(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(checkinService.getStats(currentUserId(headerUserId), startDate, endDate));
    }
}
