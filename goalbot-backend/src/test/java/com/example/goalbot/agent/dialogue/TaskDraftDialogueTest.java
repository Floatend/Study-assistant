package com.example.goalbot.agent.dialogue;

import com.example.goalbot.entity.ConversationTaskDraft;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskDraftDialogueTest {

    private final TaskDraftTurnParser parser = new TaskDraftTurnParser();
    private final TaskDraftReducer reducer = new TaskDraftReducer();

    @Test
    void keepsStartTimeAndAcceptsDurationAfterAmbiguousEndTime() {
        ConversationTaskDraft draft = draft("写高数卷子");
        LocalDateTime now = LocalDateTime.of(2026, 6, 24, 19, 35);

        TaskDraftFrame ambiguousFrame = parser.parse(
                draft,
                "现在开始，到中午十二点",
                now,
                null,
                null,
                null,
                null
        );
        TaskDraftTransition conflict = reducer.reduce(draft, ambiguousFrame);

        assertEquals(TaskDraftDecision.CONFLICT, conflict.getDecision());
        assertEquals(LocalTime.of(19, 35), draft.getStartTime());
        assertNull(draft.getEndTime());
        assertTrue(conflict.getClarificationQuestion().contains("明天中午"));

        TaskDraftFrame durationFrame = parser.parse(
                draft,
                "60min",
                now.plusMinutes(1),
                null,
                null,
                null,
                null
        );
        TaskDraftTransition ready = reducer.reduce(draft, durationFrame);

        assertEquals(TaskDraftDecision.READY, ready.getDecision());
        assertEquals(LocalTime.of(19, 35), draft.getStartTime());
        assertEquals(LocalTime.of(20, 35), draft.getEndTime());
        assertEquals(60, draft.getPlannedMinutes());
        assertNull(draft.getMissingSlots());
    }

    @Test
    void extractsChineseStartTimeAndDurationInOneTurn() {
        ConversationTaskDraft draft = draft("复习 web 作业");

        TaskDraftFrame frame = parser.parse(
                draft,
                "七点十分开始，预计两个小时",
                LocalDateTime.of(2026, 6, 24, 10, 0),
                null,
                null,
                null,
                null
        );
        TaskDraftTransition transition = reducer.reduce(draft, frame);

        assertEquals(TaskDraftDecision.READY, transition.getDecision());
        assertEquals(LocalTime.of(7, 10), draft.getStartTime());
        assertEquals(LocalTime.of(9, 10), draft.getEndTime());
        assertEquals(120, draft.getPlannedMinutes());
    }

    @Test
    void nullSlotsNeverEraseExistingDraftState() {
        ConversationTaskDraft draft = draft("复习 web 作业");
        draft.setStartTime(LocalTime.of(19, 10));

        TaskDraftFrame frame = parser.parse(
                draft,
                "两个小时",
                LocalDateTime.of(2026, 6, 24, 18, 0),
                null,
                null,
                null,
                null
        );
        reducer.reduce(draft, frame);

        assertEquals(LocalTime.of(19, 10), draft.getStartTime());
        assertEquals(LocalTime.of(21, 10), draft.getEndTime());
        assertEquals(120, draft.getPlannedMinutes());
    }

    @Test
    void acceptsTomorrowNoonAsEndWithoutMovingTaskStartDate() {
        ConversationTaskDraft draft = draft("写高数卷子");
        draft.setStartTime(LocalTime.of(19, 35));

        TaskDraftFrame frame = parser.parse(
                draft,
                "明天中午十二点",
                LocalDateTime.of(2026, 6, 24, 19, 36),
                null,
                null,
                null,
                null
        );
        TaskDraftTransition transition = reducer.reduce(draft, frame);

        assertEquals(TaskDraftDecision.READY, transition.getDecision());
        assertEquals(LocalDate.of(2026, 6, 24), draft.getPlanDate());
        assertEquals(LocalTime.NOON, draft.getEndTime());
        assertEquals(985, draft.getPlannedMinutes());
    }

    private ConversationTaskDraft draft(String title) {
        ConversationTaskDraft draft = new ConversationTaskDraft();
        draft.setId(10L);
        draft.setSessionId(20L);
        draft.setUserId(1L);
        draft.setTitle(title);
        draft.setPlanDate(LocalDate.of(2026, 6, 24));
        draft.setStatus(0);
        draft.setSourceText("今天" + title);
        return draft;
    }
}
