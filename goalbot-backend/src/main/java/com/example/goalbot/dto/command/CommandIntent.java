package com.example.goalbot.dto.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandIntent {

    private Intent intent = Intent.UNKNOWN;

    private String taskKeyword;

    private String taskTitle;

    private String description;

    private String planDate;

    private String rangeStartDate;

    private String rangeEndDate;

    private String startTime;

    private String endTime;

    private Integer plannedMinutes;

    private Long goalId;

    private String goalKeyword;

    private Integer actualMinutes;

    private Double confidence;

    private String source;

    private String sentenceType;

    private String actionType;

    private List<String> missingSlots;

    private Boolean requiresConfirmation;

    private String clarifyingQuestion;

    private String assistantReply;

    public static CommandIntent of(Intent intent) {
        CommandIntent commandIntent = new CommandIntent();
        commandIntent.setIntent(intent);
        commandIntent.setConfidence(1.0);
        commandIntent.setSource("rule");
        return commandIntent;
    }

    public boolean is(Intent target) {
        return intent == target;
    }

    public enum Intent {
        TODAY_TASKS,
        LIST_TASKS_BY_DATE,
        CREATE_TASK,
        UPDATE_TASK_SCHEDULE,
        CANCEL_TASKS,
        CANCEL_IMPORTED_SCHEDULE,
        CHECKIN,
        GOAL_STATUS,
        ADVICE,
        DAILY_REVIEW,
        WEEKLY_REVIEW,
        HELP,
        UNKNOWN
    }
}
