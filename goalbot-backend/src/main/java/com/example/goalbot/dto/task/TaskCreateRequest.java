package com.example.goalbot.dto.task;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TaskCreateRequest {

    private Long goalId;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private LocalDate planDate;

    private LocalTime startTime;

    private LocalTime endTime;

    @Min(0)
    private Integer plannedMinutes = 0;

    @Min(0)
    @Max(2)
    private Integer status = 0;
}
