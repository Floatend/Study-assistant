package com.example.goalbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("assistant_settings")
public class AssistantSettings {

    @TableId(type = IdType.AUTO)
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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
