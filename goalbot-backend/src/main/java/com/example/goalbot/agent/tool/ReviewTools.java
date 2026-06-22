package com.example.goalbot.agent.tool;

import com.example.goalbot.agent.ToolCall;
import com.example.goalbot.agent.ToolNames;
import com.example.goalbot.agent.ToolResult;
import com.example.goalbot.service.DifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class ReviewTools {

    private ReviewTools() {
    }

    @Component
    @RequiredArgsConstructor
    public static class DailyReviewTool extends AbstractAgentTool {

        private final DifyService difyService;

        @Override
        public String name() {
            return ToolNames.DAILY_REVIEW;
        }

        @Override
        public ToolResult execute(Long userId, ToolCall call) {
            return ToolResult.ok(difyService.generateDailyReview(userId, LocalDate.now()).getAiAdvice());
        }
    }

    @Component
    @RequiredArgsConstructor
    public static class WeeklyReviewTool extends AbstractAgentTool {

        private final DifyService difyService;

        @Override
        public String name() {
            return ToolNames.WEEKLY_REVIEW;
        }

        @Override
        public ToolResult execute(Long userId, ToolCall call) {
            LocalDate start = LocalDate.now().with(DayOfWeek.MONDAY);
            LocalDate end = start.plusDays(6);
            return ToolResult.ok(difyService.generateWeeklyReview(userId, start, end).getAiAdvice());
        }
    }
}
