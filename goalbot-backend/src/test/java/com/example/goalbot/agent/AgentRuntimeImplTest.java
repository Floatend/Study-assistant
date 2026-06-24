package com.example.goalbot.agent;

import com.example.goalbot.dto.command.CommandIntent;
import com.example.goalbot.dto.conversation.ConversationTurn;
import com.example.goalbot.entity.ConversationTaskDraft;
import com.example.goalbot.service.CommandLogService;
import com.example.goalbot.service.ConversationStateService;
import com.example.goalbot.service.ConversationTaskDraftService;
import com.example.goalbot.service.NaturalLanguageCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentRuntimeImplTest {

    @Mock
    private NaturalLanguageCommandService naturalLanguageCommandService;
    @Mock
    private ConversationTaskDraftService draftService;
    @Mock
    private ConversationStateService conversationStateService;
    @Mock
    private CommandLogService commandLogService;
    @Mock
    private ToolExecutor toolExecutor;

    @InjectMocks
    private AgentRuntimeImpl agentRuntime;

    @Test
    void parsesCheckinWithTaskKeywordAndNoDuration() {
        CommandIntent intent = agentRuntime.parseIntent(1L, "打卡物理");

        assertEquals(CommandIntent.Intent.CHECKIN, intent.getIntent());
        assertEquals("物理", intent.getTaskKeyword());
        assertNull(intent.getActualMinutes());
    }

    @Test
    void keepsExplicitCheckinDurationAsOverride() {
        CommandIntent intent = agentRuntime.parseIntent(1L, "/打卡 物理 1.5小时");

        assertEquals(CommandIntent.Intent.CHECKIN, intent.getIntent());
        assertEquals("物理", intent.getTaskKeyword());
        assertEquals(90, intent.getActualMinutes());
    }

    @Test
    void routesEnglishDurationToTheActiveDraftWithoutReclassifyingIntent() {
        UserMessage message = new UserMessage();
        message.setUserId(1L);
        message.setChannel("FEISHU");
        message.setMessageId("om_1");
        message.setText("60min");

        ConversationTaskDraft draft = new ConversationTaskDraft();
        draft.setId(10L);
        draft.setUserId(1L);
        draft.setSessionId(20L);
        draft.setTitle("写高数卷子");

        when(commandLogService.beginCommand(1L, "om_1", "60min")).thenReturn(30L);
        when(conversationStateService.beginTurn(1L, "FEISHU", "om_1", "60min"))
                .thenReturn(new ConversationTurn(20L, 40L, "FEISHU"));
        when(draftService.getActiveDraft(1L)).thenReturn(Optional.of(draft));
        when(toolExecutor.execute(eq(1L), org.mockito.ArgumentMatchers.any(ToolCall.class)))
                .thenReturn(ToolResult.ok("已创建任务"));

        AgentReply reply = agentRuntime.handle(message);

        ArgumentCaptor<ToolCall> callCaptor = ArgumentCaptor.forClass(ToolCall.class);
        verify(toolExecutor).execute(eq(1L), callCaptor.capture());
        assertEquals(ToolNames.UPDATE_TASK_DRAFT, callCaptor.getValue().getTool());
        assertEquals("60min", callCaptor.getValue().arg("text"));
        assertTrue(reply.isSuccess());
        verifyNoInteractions(naturalLanguageCommandService);
    }

    @Test
    void routesNaturalTaskListWithoutDependingOnDifyTitleExtraction() {
        CommandIntent intent = agentRuntime.parseIntent(1L, "今天写高数卷子，新工科英语复习");

        assertEquals(CommandIntent.Intent.CREATE_TASK, intent.getIntent());
        assertEquals("rule-natural-task-list", intent.getSource());
        verifyNoInteractions(naturalLanguageCommandService);
    }
}
