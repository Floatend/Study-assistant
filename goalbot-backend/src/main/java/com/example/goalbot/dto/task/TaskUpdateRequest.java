package com.example.goalbot.dto.task;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TaskUpdateRequest {

    private Long goalId;

    private String title;

    private String description;

    private LocalDate planDate;

    private LocalTime startTime;

    private LocalTime endTime;

    @Min(0)
    private Integer plannedMinutes;

    @Min(0)
    @Max(2)
    private Integer status;
}
