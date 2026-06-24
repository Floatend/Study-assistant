package com.example.goalbot.agent.tool;

import com.example.goalbot.agent.ToolCall;
import com.example.goalbot.agent.ToolResult;
import com.example.goalbot.agent.dialogue.TaskDraftReducer;
import com.example.goalbot.agent.dialogue.TaskDraftTurnParser;
import com.example.goalbot.entity.ConversationTaskDraft;
import com.example.goalbot.service.ConversationTaskDraftService;
import com.example.goalbot.service.ConversationTransitionLogService;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.TaskVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
