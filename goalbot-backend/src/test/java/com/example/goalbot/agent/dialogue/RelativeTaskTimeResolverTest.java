package com.example.goalbot.agent.dialogue;

import com.example.goalbot.entity.ConversationTaskDraft;
import com.example.goalbot.entity.Task;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.TaskVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RelativeTaskTimeResolverTest {

    @Test
    void resolvesEndTimeFromReferencedTask() {
        TaskService taskService = mock(TaskService.class);
        ConversationTaskDraft draft = draft();
        TaskVO highMath = task(501L, "写高数卷子", LocalTime.of(13, 39), LocalTime.of(14, 39));
        when(taskService.listActiveTasksByDate(1L, draft.getPlanDate())).thenReturn(List.of(highMath));

        RelativeTaskTimeResolver.Resolution result =
                new RelativeTaskTimeResolver(taskService).resolve(1L, draft, "接着高数");

        assertTrue(result.resolved());
        assertEquals(LocalTime.of(14, 39), result.startTime());
        assertEquals(501L, result.referencedTaskId());
    }

    @Test
    void asksForClarificationWhenSeveralTasksMatch() {
        TaskService taskService = mock(TaskService.class);
        ConversationTaskDraft draft = draft();
        when(taskService.listActiveTasksByDate(1L, draft.getPlanDate())).thenReturn(List.of(
                task(501L, "写高数卷子", LocalTime.of(13, 39), LocalTime.of(14, 39)),
                task(502L, "高数复习", LocalTime.of(15, 0), LocalTime.of(16, 0))
        ));

        RelativeTaskTimeResolver.Resolution result =
                new RelativeTaskTimeResolver(taskService).resolve(1L, draft, "高数之后");

        assertFalse(result.resolved());
        assertTrue(result.requiresClarification());
        assertTrue(result.clarificationQuestion().contains("多个"));
    }

    @Test
    void resolvesAPlannerReferenceByOwnedTaskId() {
        TaskService taskService = mock(TaskService.class);
        ConversationTaskDraft draft = draft();
        Task highMath = new Task();
        highMath.setId(501L);
        highMath.setUserId(1L);
        highMath.setTitle("写高数卷子");
        highMath.setPlanDate(draft.getPlanDate());
        highMath.setEndTime(LocalTime.of(14, 39));
        when(taskService.getOwnedTaskEntity(1L, 501L)).thenReturn(highMath);

        RelativeTaskTimeResolver.Resolution result = new RelativeTaskTimeResolver(taskService)
                .resolveStructured(1L, draft, 501L, "高数", "AFTER", "END");

        assertTrue(result.resolved());
        assertEquals(LocalTime.of(14, 39), result.startTime());
        assertEquals(501L, result.referencedTaskId());
        assertEquals("写高数卷子", result.referencedTaskTitle());
    }

    private ConversationTaskDraft draft() {
        ConversationTaskDraft draft = new ConversationTaskDraft();
        draft.setTitle("新工科英语复习");
        draft.setPlanDate(LocalDate.of(2026, 6, 24));
        return draft;
    }

    private TaskVO task(Long id, String title, LocalTime start, LocalTime end) {
        TaskVO task = new TaskVO();
        task.setId(id);
        task.setTitle(title);
        task.setStartTime(start);
        task.setEndTime(end);
        return task;
    }
}
