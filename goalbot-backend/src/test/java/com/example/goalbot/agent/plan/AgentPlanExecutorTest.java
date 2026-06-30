package com.example.goalbot.agent.plan;

import com.example.goalbot.agent.ToolCall;
import com.example.goalbot.agent.ToolExecutor;
import com.example.goalbot.agent.ToolNames;
import com.example.goalbot.agent.ToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentPlanExecutorTest {

    @Mock
    private ToolExecutor toolExecutor;

    @Test
    void executesOnlyThroughTheRegisteredToolExecutor() {
        AgentTarget target = new AgentTarget();
        target.setType("ACTIVE_DRAFT");
        target.setId(102L);
        AgentAction action = new AgentAction();
        action.setActionId("a1");
        action.setTool(ToolNames.UPDATE_TASK_DRAFT);
        action.setTarget(target);
        action.setArguments(new LinkedHashMap<>(Map.of(
                "start_time_reference", Map.of(
                        "relation", "AFTER",
                        "boundary", "END",
                        "task_id", 501,
                        "task_query", "高数"
                )
        )));
        AgentPlan plan = new AgentPlan();
        plan.setMode(AgentPlanMode.TOOL);
        plan.setConfidence(0.97);
        plan.setActions(List.of(action));

        when(toolExecutor.execute(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(ToolCall.class)))
                .thenReturn(ToolResult.ok("还需要时长"));

        AgentPlanExecution execution = new AgentPlanExecutor(toolExecutor)
                .execute(1L, 20L, "接着高数", plan);

        ArgumentCaptor<ToolCall> captor = ArgumentCaptor.forClass(ToolCall.class);
        verify(toolExecutor).execute(org.mockito.ArgumentMatchers.eq(1L), captor.capture());
        assertEquals(ToolNames.UPDATE_TASK_DRAFT, captor.getValue().getTool());
        assertEquals(102L, captor.getValue().arg("draft_id"));
        assertEquals("接着高数", captor.getValue().arg("source_text"));
        assertTrue(captor.getValue().arg("start_time_reference") instanceof Map);
        assertEquals("还需要时长", execution.result().getMessage());
    }

    @Test
    void doesNotExecutePlansThatRequireConfirmation() {
        AgentPlan plan = new AgentPlan();
        plan.setMode(AgentPlanMode.TOOL);
        plan.setConfidence(0.99);
        plan.setRequiresConfirmation(true);
        plan.setClarifyingQuestion("确认删除这些任务吗？");

        AgentPlanExecution execution = new AgentPlanExecutor(toolExecutor)
                .execute(1L, 20L, "删除任务", plan);

        assertEquals("确认删除这些任务吗？", execution.result().getMessage());
        verify(toolExecutor, never()).execute(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
    }
}
