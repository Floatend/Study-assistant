package com.example.goalbot.agent.tool;

import com.example.goalbot.agent.ToolCall;
import com.example.goalbot.agent.ToolResult;
import com.example.goalbot.entity.ConversationTaskDraft;
import com.example.goalbot.service.ConversationTaskDraftService;
import com.example.goalbot.service.ConversationTransitionLogService;
import com.example.goalbot.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTaskToolTest {

    @Mock
    private TaskService taskService;
    @Mock
    private ConversationTaskDraftService draftService;
    @Mock
    private ConversationTransitionLogService transitionLogService;

    @InjectMocks
    private CreateTaskTool tool;

    @Test
    void queuesEveryTaskEvenWhenTheSingularIntentTitleIsMissing() {
        ToolCall call = new ToolCall();
        LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("source_text", "今天写高数卷子，新工科英语复习");
        arguments.put("session_id", 20L);
        call.setArguments(arguments);

        when(draftService.enqueueDrafts(eq(1L), eq(20L), anyList())).thenAnswer(invocation -> {
            List<ConversationTaskDraft> drafts = invocation.getArgument(2);
            drafts.get(0).setId(101L);
            drafts.get(1).setId(102L);
            return drafts;
        });

        ToolResult result = tool.execute(1L, call);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ConversationTaskDraft>> captor = ArgumentCaptor.forClass(List.class);
        verify(draftService).enqueueDrafts(eq(1L), eq(20L), captor.capture());
        assertEquals(List.of("写高数卷子", "新工科英语复习"),
                captor.getValue().stream().map(ConversationTaskDraft::getTitle).toList());
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("我识别到 2 个任务"));
        assertTrue(result.getMessage().contains("先安排「写高数卷子」"));
        verify(taskService, never()).createTask(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void queuesEveryTaskFromThePlannerTasksArray() {
        ToolCall call = new ToolCall();
        LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("source_text", "把两项复习安排一下");
        arguments.put("session_id", 21L);
        arguments.put("tasks", List.of(
                Map.of("title", "写高数卷子", "plan_date", "2026-06-24"),
                Map.of("title", "新工科英语复习", "plan_date", "2026-06-24")
        ));
        call.setArguments(arguments);

        when(draftService.getActiveDraft(1L)).thenReturn(Optional.empty());
        when(draftService.enqueueDrafts(eq(1L), eq(21L), anyList())).thenAnswer(invocation -> {
            List<ConversationTaskDraft> drafts = invocation.getArgument(2);
            drafts.get(0).setId(201L);
            drafts.get(1).setId(202L);
            return drafts;
        });

        ToolResult result = tool.execute(1L, call);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ConversationTaskDraft>> captor = ArgumentCaptor.forClass(List.class);
        verify(draftService).enqueueDrafts(eq(1L), eq(21L), captor.capture());
        assertEquals(List.of("写高数卷子", "新工科英语复习"),
                captor.getValue().stream().map(ConversationTaskDraft::getTitle).toList());
        assertEquals("start_time,duration", captor.getValue().get(0).getMissingSlots());
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("我识别到 2 个任务"));
        verify(taskService, never()).createTask(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void refusesToOverwriteAnExistingDraftWithAMisroutedCreateIntent() {
        ConversationTaskDraft existing = new ConversationTaskDraft();
        existing.setId(102L);
        existing.setTitle("新工科英语复习");
        when(draftService.getActiveDraft(1L)).thenReturn(Optional.of(existing));

        ToolCall call = new ToolCall();
        LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("task_title", "高数");
        arguments.put("source_text", "接着高数");
        arguments.put("session_id", 20L);
        call.setArguments(arguments);

        ToolResult result = tool.execute(1L, call);

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("不会用新标题覆盖"));
        assertEquals("新工科英语复习", existing.getTitle());
        verify(draftService, never()).saveActiveDraft(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
    }
}
