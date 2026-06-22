package com.example.goalbot.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class AssistantSettingsVO {

    private Long id;

    private Long userId;

    private Boolean proactiveEnabled;

    private Boolean feishuEnabled;

    private String feishuChatId;

    private Boolean morningEnabled;

    private LocalTime morningTime;

    private Boolean reviewEnabled;

    private LocalTime reviewTime;

    private Boolean weeklyEnabled;

    private Integer weeklyDay;

    private LocalTime weeklyTime;

    private Boolean periodicNudgeEnabled;

    private Integer periodicNudgeIntervalHours;

    private Boolean aiBriefingEnabled;

    private LocalTime aiBriefingTime;

    private String aiBriefingSourceName;

    private String aiBriefingSourceUrl;

    private Integer adviceDays;

    private Boolean quietEnabled;

    private LocalTime quietStartTime;

    private LocalTime quietEndTime;

    private Boolean globalReminderEnabled;

    private Boolean feishuAppConfigured;

    private Boolean feishuDefaultChatConfigured;

    private Boolean effectiveFeishuChatConfigured;

    private Boolean feishuLongConnectionEnabled;

    private Boolean difyChatConfigured;

    private Boolean difyWorkflowConfigured;

    private Boolean aiBriefingSourceConfigured;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
