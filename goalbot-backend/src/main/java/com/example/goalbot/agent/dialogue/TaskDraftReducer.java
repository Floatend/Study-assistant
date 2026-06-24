package com.example.goalbot.agent.dialogue;

import com.example.goalbot.entity.ConversationTaskDraft;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalTime;

@Component
public class TaskDraftReducer {

    public TaskDraftTransition reduce(ConversationTaskDraft draft, TaskDraftFrame frame) {
        TaskDraftSnapshot before = TaskDraftSnapshot.from(draft);

        if (frame.getPlanDate() != null) {
            draft.setPlanDate(frame.getPlanDate());
        }
        if (frame.getStartTime() != null) {
            draft.setStartTime(frame.getStartTime());
        }
        if (frame.getPlannedMinutes() != null && frame.getPlannedMinutes() > 0) {
            draft.setPlannedMinutes(frame.getPlannedMinutes());
        }
        if (!frame.hasConflict() && frame.getEndTime() != null) {
            draft.setEndTime(frame.getEndTime());
        }

        if (!frame.hasConflict()) {
            deriveDependentSlots(draft, frame);
        }
        draft.setMissingSlots(resolveMissingSlots(draft));
        draft.setSourceText(combine(draft.getSourceText(), frame.getRawText()));

        TaskDraftDecision decision;
        String question;
        if (frame.hasConflict()) {
            decision = TaskDraftDecision.CONFLICT;
            question = frame.getClarificationQuestion();
        } else if (StringUtils.hasText(draft.getMissingSlots())) {
            decision = TaskDraftDecision.NEEDS_INPUT;
            question = askForMissing(draft);
        } else {
            decision = TaskDraftDecision.READY;
            question = null;
        }

        return TaskDraftTransition.builder()
                .rawText(frame.getRawText())
                .before(before)
                .frame(frame)
                .after(TaskDraftSnapshot.from(draft))
                .decision(decision)
                .clarificationQuestion(question)
                .build();
    }

    private void deriveDependentSlots(ConversationTaskDraft draft, TaskDraftFrame frame) {
        if (draft.getStartTime() == null) {
            return;
        }
        if (frame.isDurationExplicit() && draft.getPlannedMinutes() != null) {
            draft.setEndTime(draft.getStartTime().plusMinutes(draft.getPlannedMinutes()));
            return;
        }
        if (frame.isEndExplicit() && draft.getEndTime() != null) {
            draft.setPlannedMinutes(minutesBetween(draft.getStartTime(), draft.getEndTime()));
            return;
        }
        if (frame.isStartExplicit() && draft.getPlannedMinutes() != null && draft.getPlannedMinutes() > 0) {
            draft.setEndTime(draft.getStartTime().plusMinutes(draft.getPlannedMinutes()));
        }
    }

    private String resolveMissingSlots(ConversationTaskDraft draft) {
        StringBuilder slots = new StringBuilder();
        if (draft.getStartTime() == null) {
            slots.append("start_time");
        }
        if (draft.getEndTime() == null && (draft.getPlannedMinutes() == null || draft.getPlannedMinutes() <= 0)) {
            if (!slots.isEmpty()) {
                slots.append(',');
            }
            slots.append("duration");
        }
        return slots.isEmpty() ? null : slots.toString();
    }

    private String askForMissing(ConversationTaskDraft draft) {
        boolean missingStart = draft.getStartTime() == null;
        boolean missingDuration = draft.getEndTime() == null
                && (draft.getPlannedMinutes() == null || draft.getPlannedMinutes() <= 0);
        if (missingStart && missingDuration) {
            return "「" + draft.getTitle() + "」准备几点开始，预计安排多久？";
        }
        if (missingStart) {
            return "「" + draft.getTitle() + "」准备几点开始？";
        }
        return "「" + draft.getTitle() + "」预计安排多久？例如：60 分钟。";
    }

    private int minutesBetween(LocalTime start, LocalTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < 0) {
            minutes += Duration.ofDays(1).toMinutes();
        }
        return (int) minutes;
    }

    private String combine(String previous, String current) {
        if (!StringUtils.hasText(previous)) {
            return current;
        }
        if (!StringUtils.hasText(current)) {
            return previous;
        }
        return previous + " " + current;
    }
}
