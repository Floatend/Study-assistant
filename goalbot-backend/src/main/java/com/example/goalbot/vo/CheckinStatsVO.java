package com.example.goalbot.vo;

import lombok.Data;

@Data
public class CheckinStatsVO {

    private Integer totalMinutes;

    private Integer checkinCount;

    private Integer completedTaskCount;

    private Double averageMood;

    private Double averageDifficulty;
}
