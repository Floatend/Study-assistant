package com.example.goalbot.agent.dialogue;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class TaskDraftFrame {

    private String rawText;
    private LocalDate planDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer plannedMinutes;
    private boolean startExplicit;
    private boolean endExplicit;
    private boolean endNextDay;
    private boolean durationExplicit;
    private String conflictCode;
    private String clarificationQuestion;
    private Map<String, String> slotSources = new LinkedHashMap<>();

    public boolean hasConflict() {
        return conflictCode != null && !conflictCode.isBlank();
    }
}
