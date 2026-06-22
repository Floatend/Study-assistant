package com.example.goalbot.service;

import com.example.goalbot.vo.TaskStatusCountVO;
import com.example.goalbot.vo.TrendPointVO;

import java.time.LocalDate;
import java.util.List;

public interface AnalyticsService {

    List<TrendPointVO> getStudyDuration(Long userId, LocalDate startDate, LocalDate endDate);

    List<TaskStatusCountVO> getTaskStatus(Long userId, LocalDate startDate, LocalDate endDate);
}
