package com.example.goalbot.agent;

import com.example.goalbot.dto.command.CommandIntent;
import com.example.goalbot.dto.conversation.ConversationTurn;
import com.example.goalbot.entity.ConversationTaskDraft;
import com.example.goalbot.agent.plan.AgentAction;
import com.example.goalbot.agent.plan.AgentPlan;
import com.example.goalbot.agent.plan.AgentPlanExecution;
import com.example.goalbot.agent.plan.AgentPlanExecutor;
import com.example.goalbot.agent.plan.AgentPlanIntentAdapter;
import com.example.goalbot.agent.plan.AgentPlanMode;
import com.example.goalbot.agent.plan.AgentPlanner;
import com.example.goalbot.agent.plan.AgentPlannerProperties;
import com.example.goalbot.agent.plan.PlannerRunMode;
import com.example.goalbot.service.AgentPlanLogService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

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
    @Mock
    private AgentPlanner agentPlanner;
    @Mock
    private AgentPlanExecutor agentPlanExecutor;
    @Mock
    private AgentPlanIntentAdapter agentPlanIntentAdapter;
    @Mock
    private AgentPlannerProperties agentPlannerProperties;
    @Mock
    private AgentPlanLogService agentPlanLogService;

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

    @Test
    void routesRelativeTaskReferenceToTheActiveDraft() {
        UserMessage message = new UserMessage();
        message.setUserId(1L);
        message.setChannel("FEISHU");
        message.setMessageId("om_relative");
        message.setText("接着高数");

        ConversationTaskDraft draft = new ConversationTaskDraft();
        draft.setId(102L);
        draft.setUserId(1L);
        draft.setSessionId(20L);
        draft.setTitle("新工科英语复习");

        when(commandLogService.beginCommand(1L, "om_relative", "接着高数")).thenReturn(31L);
        when(conversationStateService.beginTurn(1L, "FEISHU", "om_relative", "接着高数"))
                .thenReturn(new ConversationTurn(20L, 41L, "FEISHU"));
        when(draftService.getActiveDraft(1L)).thenReturn(Optional.of(draft));
        when(toolExecutor.execute(eq(1L), org.mockito.ArgumentMatchers.any(ToolCall.class)))
                .thenReturn(ToolResult.ok("请补充时长"));

        agentRuntime.handle(message);

        ArgumentCaptor<ToolCall> callCaptor = ArgumentCaptor.forClass(ToolCall.class);
        verify(toolExecutor).execute(eq(1L), callCaptor.capture());
        assertEquals(ToolNames.UPDATE_TASK_DRAFT, callCaptor.getValue().getTool());
        assertEquals("接着高数", callCaptor.getValue().arg("text"));
        verifyNoInteractions(naturalLanguageCommandService);
    }

    @Test
    void executesAValidPrimaryPlannerPlanBeforeLegacyRules() {
        UserMessage message = new UserMessage();
        message.setUserId(1L);
        message.setChannel("FEISHU");
        message.setMessageId("om_plan");
        message.setText("接着高数");

        ConversationTaskDraft draft = new ConversationTaskDraft();
        draft.setId(102L);
        draft.setUserId(1L);
        draft.setTitle("新工科英语复习");

        AgentAction action = new AgentAction();
        action.setTool(ToolNames.UPDATE_TASK_DRAFT);
        AgentPlan plan = new AgentPlan();
        plan.setMode(AgentPlanMode.TOOL);
        plan.setConfidence(0.97);
        plan.setSource("dify-agent-plan");
        plan.setActions(List.of(action));
        CommandIntent plannedIntent = CommandIntent.of(CommandIntent.Intent.CREATE_TASK);
        plannedIntent.setSource("dify-agent-plan");

        when(commandLogService.beginCommand(1L, "om_plan", "接着高数")).thenReturn(32L);
        when(conversationStateService.beginTurn(1L, "FEISHU", "om_plan", "接着高数"))
                .thenReturn(new ConversationTurn(20L, 42L, "FEISHU"));
        when(draftService.getActiveDraft(1L)).thenReturn(Optional.of(draft));
        when(agentPlannerProperties.getMode()).thenReturn(PlannerRunMode.PRIMARY);
        when(agentPlannerProperties.getMinConfidence()).thenReturn(0.72);
        when(agentPlanner.plan(1L, "FEISHU", "接着高数", draft)).thenReturn(plan);
        when(agentPlanIntentAdapter.toIntent(plan)).thenReturn(plannedIntent);
        when(agentPlanExecutor.execute(1L, 20L, "接着高数", plan))
                .thenReturn(new AgentPlanExecution(ToolResult.ok("还需要时长"), ToolNames.UPDATE_TASK_DRAFT));

        AgentReply reply = agentRuntime.handle(message);

        assertTrue(reply.isSuccess());
        assertEquals("还需要时长", reply.getContent());
        assertEquals(ToolNames.UPDATE_TASK_DRAFT, reply.getTool());
        verify(agentPlanLogService).record(1L, 20L, "om_plan", PlannerRunMode.PRIMARY, true, plan);
        verifyNoInteractions(naturalLanguageCommandService);
        verifyNoInteractions(toolExecutor);
    }
}
