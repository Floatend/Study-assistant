package com.example.goalbot.service;

import com.example.goalbot.entity.AssistantSettings;
import com.example.goalbot.entity.Notification;

public interface AiBriefingService {

    Notification sendDailyBriefing(Long userId, AssistantSettings settings);
}
