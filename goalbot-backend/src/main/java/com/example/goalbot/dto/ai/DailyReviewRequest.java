package com.example.goalbot.dto.ai;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyReviewRequest {

    private LocalDate date;
}
