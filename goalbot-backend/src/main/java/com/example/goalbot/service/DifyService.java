package com.example.goalbot.service;

import com.example.goalbot.vo.ReviewVO;

import java.time.LocalDate;

public interface DifyService {

    ReviewVO generateAdvice(Long userId);

    ReviewVO generateAdvice(Long userId, Integer days);

    ReviewVO generateDailyReview(Long userId, LocalDate date);

    ReviewVO generateWeeklyReview(Long userId, LocalDate weekStart, LocalDate weekEnd);

    String generateArticleBriefing(Long userId, String sourceName, String articleTitle, String articleUrl, String articleContent);

    String chatForFeishu(Long userId, String message);
}
