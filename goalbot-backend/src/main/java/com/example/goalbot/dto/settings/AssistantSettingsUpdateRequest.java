package com.example.goalbot.dto.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

@Data
public class AssistantSettingsUpdateRequest {

    private Boolean proactiveEnabled;

    private Boolean feishuEnabled;

    @Size(max = 128)
    private String feishuChatId;

    private Boolean morningEnabled;

    private LocalTime morningTime;

    private Boolean reviewEnabled;

    private LocalTime reviewTime;

    private Boolean weeklyEnabled;

    @Min(1)
    @Max(7)
    private Integer weeklyDay;

    private LocalTime weeklyTime;

    private Boolean periodicNudgeEnabled;

    @Min(1)
    @Max(24)
    private Integer periodicNudgeIntervalHours;

    private Boolean aiBriefingEnabled;

    private LocalTime aiBriefingTime;

    @Size(max = 128)
    private String aiBriefingSourceName;

    @Size(max = 512)
    private String aiBriefingSourceUrl;

    @Min(1)
    @Max(3)
    private Integer adviceDays;

    private Boolean quietEnabled;

    private LocalTime quietStartTime;

    private LocalTime quietEndTime;
}
