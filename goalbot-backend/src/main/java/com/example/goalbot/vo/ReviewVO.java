package com.example.goalbot.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ReviewVO {

    private Long id;

    private Long userId;

    private LocalDate reviewDate;

    private Integer type;

    private String summary;

    private String aiAdvice;

    private LocalDateTime createdAt;
}
