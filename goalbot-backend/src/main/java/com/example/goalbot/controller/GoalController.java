package com.example.goalbot.controller;

import com.example.goalbot.common.BaseController;
import com.example.goalbot.common.Result;
import com.example.goalbot.dto.goal.GoalCreateRequest;
import com.example.goalbot.dto.goal.GoalUpdateRequest;
import com.example.goalbot.service.GoalService;
import com.example.goalbot.vo.GoalVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/goals")
public class GoalController extends BaseController {

    private final GoalService goalService;

    @PostMapping
    public Result<GoalVO> createGoal(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Valid @RequestBody GoalCreateRequest request) {
        return Result.success(goalService.createGoal(currentUserId(headerUserId), request));
    }

    @GetMapping
    public Result<List<GoalVO>> listGoals(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) String keyword) {
        return Result.success(goalService.listGoals(currentUserId(headerUserId), status, priority, keyword));
    }

    @GetMapping("/{id}")
    public Result<GoalVO> getGoal(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @PathVariable Long id) {
        return Result.success(goalService.getGoal(currentUserId(headerUserId), id));
    }

    @PutMapping("/{id}")
    public Result<GoalVO> updateGoal(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @PathVariable Long id,
            @Valid @RequestBody GoalUpdateRequest request) {
        return Result.success(goalService.updateGoal(currentUserId(headerUserId), id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteGoal(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @PathVariable Long id) {
        goalService.deleteGoal(currentUserId(headerUserId), id);
        return Result.success();
    }
}
