package com.example.goalbot.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DashboardVO {

    private Integer todayActualMinutes;

    private Integer todayTaskCount;

    private Integer completedTaskCount;

    private List<TaskVO> todayTasks;

    private Integer adviceDays;

    private LocalDate adviceStartDate;

    private LocalDate adviceEndDate;

    private String latestAiAdvice;

    private Boolean aiAdviceStale;

    private String aiAdviceSourceHash;
}
