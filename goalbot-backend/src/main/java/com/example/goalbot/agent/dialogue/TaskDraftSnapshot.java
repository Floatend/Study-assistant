package com.example.goalbot.agent.dialogue;

import com.example.goalbot.entity.ConversationTaskDraft;

import java.time.LocalDate;
import java.time.LocalTime;

public record TaskDraftSnapshot(
        Long id,
        Long sessionId,
        String title,
        LocalDate planDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer plannedMinutes,
        String missingSlots,
        Integer status
) {

    public static TaskDraftSnapshot from(ConversationTaskDraft draft) {
        if (draft == null) {
            return null;
        }
        return new TaskDraftSnapshot(
                draft.getId(),
                draft.getSessionId(),
                draft.getTitle(),
                draft.getPlanDate(),
                draft.getStartTime(),
                draft.getEndTime(),
                draft.getPlannedMinutes(),
                draft.getMissingSlots(),
                draft.getStatus()
        );
    }
}
