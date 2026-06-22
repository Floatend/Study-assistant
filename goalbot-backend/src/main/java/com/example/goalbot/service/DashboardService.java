package com.example.goalbot.service;

import com.example.goalbot.vo.DashboardVO;
import com.example.goalbot.vo.ReviewVO;

public interface DashboardService {

    DashboardVO getDashboard(Long userId);

    DashboardVO getDashboard(Long userId, Integer adviceDays);

    ReviewVO refreshTodayAdvice(Long userId);

    ReviewVO refreshAdvice(Long userId, Integer adviceDays);
}
