package com.example.goalbot.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class GoalVO {

    private Long id;

    private Long userId;

    private String title;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer priority;

    private Integer status;

    private Long totalTaskCount;

    private Long completedTaskCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
