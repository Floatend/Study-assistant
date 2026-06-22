package com.example.goalbot.dto.ai;

import lombok.Data;

import java.time.LocalDate;

@Data
public class WeeklyReviewRequest {

    private LocalDate weekStart;

    private LocalDate weekEnd;
}
