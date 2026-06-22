package com.example.goalbot.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class TaskVO {

    private Long id;

    private Long userId;

    private Long goalId;

    private String goalTitle;

    private String title;

    private String description;

    private LocalDate planDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer plannedMinutes;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
