package com.example.goalbot.agent.tool;

import com.example.goalbot.agent.ToolCall;
import com.example.goalbot.agent.ToolResult;
import com.example.goalbot.agent.dialogue.TaskDraftReducer;
import com.example.goalbot.agent.dialogue.TaskDraftTurnParser;
import com.example.goalbot.agent.dialogue.RelativeTaskTimeResolver;
import com.example.goalbot.entity.ConversationTaskDraft;
import com.example.goalbot.entity.Task;
import com.example.goalbot.service.ConversationTaskDraftService;
import com.example.goalbot.service.ConversationTransitionLogService;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.TaskVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UpdateTaskDraftToolTest {

    @Mock
    private ConversationTaskDraftService draftService;
    @Mock
    private TaskService taskService;
    @Mock
    private ConversationTransitionLogService transitionLogService;

    @Test
    void promptsForTheNextQueuedTaskAfterCompletingTheCurrentOne() {
        ConversationTaskDraft first = draft(101L, "写高数卷子");
        ConversationTaskDraft second = draft(102L, "新工科英语复习");
        when(draftService.getActiveDraft(1L))
                .thenReturn(Optional.of(first))
                .thenReturn(Optional.of(second));
        when(draftService.saveActiveDraft(eq(1L), eq(20L), any(ConversationTaskDraft.class)))
                .thenAnswer(invocation -> invocation.getArgument(2));

        TaskVO created = new TaskVO();
        created.setId(501L);
        created.setTitle("写高数卷子");
        created.setPlanDate(LocalDate.of(2026, 6, 24));
        created.setPlannedMinutes(60);
        when(taskService.createTask(eq(1L), any())).thenReturn(created);

        UpdateTaskDraftTool tool = new UpdateTaskDraftTool(
                draftService,
                taskService,
                new TaskDraftTurnParser(),
                new TaskDraftReducer(),
                new RelativeTaskTimeResolver(taskService),
                transitionLogService
        );
        ToolCall call = new ToolCall();
        LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("text", "下午7点，60min");
        call.setArguments(arguments);

        ToolResult result = tool.execute(1L, call);

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("已创建任务"));
        assertTrue(result.getMessage().contains("接着安排「新工科英语复习」"));
        verify(draftService).completeActiveDraft(1L);
    }

    @Test
    void resolvesFollowingTaskAsTheCurrentDraftStartTime() {
        ConversationTaskDraft englishDraft = draft(102L, "新工科英语复习");
        when(draftService.getActiveDraft(1L)).thenReturn(Optional.of(englishDraft));
        when(draftService.saveActiveDraft(eq(1L), eq(20L), any(ConversationTaskDraft.class)))
                .thenAnswer(invocation -> invocation.getArgument(2));

        TaskVO highMath = new TaskVO();
        highMath.setId(501L);
        highMath.setTitle("写高数卷子");
        highMath.setPlanDate(LocalDate.of(2026, 6, 24));
        highMath.setStartTime(LocalTime.of(13, 39));
        highMath.setEndTime(LocalTime.of(14, 39));
        when(taskService.listActiveTasksByDate(1L, LocalDate.of(2026, 6, 24)))
                .thenReturn(java.util.List.of(highMath));

        UpdateTaskDraftTool tool = new UpdateTaskDraftTool(
                draftService,
                taskService,
                new TaskDraftTurnParser(),
                new TaskDraftReducer(),
                new RelativeTaskTimeResolver(taskService),
                transitionLogService
        );
        ToolCall call = new ToolCall();
        LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("text", "接着高数");
        call.setArguments(arguments);

        ToolResult result = tool.execute(1L, call);

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("新工科英语复习"));
        assertTrue(result.getMessage().contains("预计安排多久"));
        assertEquals("新工科英语复习", englishDraft.getTitle());
        assertEquals(LocalTime.of(14, 39), englishDraft.getStartTime());
        verify(taskService, never()).createTask(eq(1L), any());
    }

    @Test
    void resolvesStructuredPlannerReferenceInsideTheDraftTool() {
        ConversationTaskDraft englishDraft = draft(102L, "新工科英语复习");
        when(draftService.getActiveDraft(1L)).thenReturn(Optional.of(englishDraft));
        when(draftService.saveActiveDraft(eq(1L), eq(20L), any(ConversationTaskDraft.class)))
                .thenAnswer(invocation -> invocation.getArgument(2));

        Task highMath = new Task();
        highMath.setId(501L);
        highMath.setUserId(1L);
        highMath.setTitle("写高数卷子");
        highMath.setPlanDate(LocalDate.of(2026, 6, 24));
        highMath.setEndTime(LocalTime.of(14, 39));
        when(taskService.getOwnedTaskEntity(1L, 501L)).thenReturn(highMath);

        UpdateTaskDraftTool tool = new UpdateTaskDraftTool(
                draftService,
                taskService,
                new TaskDraftTurnParser(),
                new TaskDraftReducer(),
                new RelativeTaskTimeResolver(taskService),
                transitionLogService
        );
        ToolCall call = new ToolCall();
        LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("text", "接着高数");
        arguments.put("draft_id", 102L);
        arguments.put("start_time_reference", Map.of(
                "relation", "AFTER",
                "boundary", "END",
                "task_id", 501L,
                "task_query", "高数"
        ));
        call.setArguments(arguments);

        ToolResult result = tool.execute(1L, call);

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("预计安排多久"));
        assertEquals(LocalTime.of(14, 39), englishDraft.getStartTime());
        verify(taskService).getOwnedTaskEntity(1L, 501L);
        verify(taskService, never()).listActiveTasksByDate(eq(1L), any());
        verify(taskService, never()).createTask(eq(1L), any());
    }

    private ConversationTaskDraft draft(Long id, String title) {
        ConversationTaskDraft draft = new ConversationTaskDraft();
        draft.setId(id);
        draft.setSessionId(20L);
        draft.setUserId(1L);
        draft.setTitle(title);
        draft.setPlanDate(LocalDate.of(2026, 6, 24));
        draft.setStatus(0);
        draft.setMissingSlots("start_time,duration");
        draft.setSourceText("今天写高数卷子，新工科英语复习");
        return draft;
    }
}
