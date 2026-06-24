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
}
