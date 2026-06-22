package com.example.goalbot.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CheckinVO {

    private Long id;

    private Long userId;

    private Long taskId;

    private String taskTitle;

    private Integer actualMinutes;

    private String content;

    private Integer mood;

    private Integer difficulty;

    private LocalDateTime createdAt;
}
