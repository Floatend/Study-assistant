package com.example.goalbot.agent.plan;

import com.example.goalbot.agent.ToolNames;
import com.example.goalbot.integration.dify.DifyClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DifyAgentPlannerTest {

    @Mock
    private DifyClient difyClient;
    @Mock
    private AgentPlanContextAssembler contextAssembler;
    @Mock
    private AgentPlannerProperties plannerProperties;

    @Test
    void parsesStructuredActionsAndTaskReferences() {
        when(difyClient.isPlannerWorkflowConfigured()).thenReturn(true);
        when(contextAssembler.buildInputs(1L, "FEISHU", "接着高数", null)).thenReturn(Map.of());
        when(plannerProperties.getMaxActions()).thenReturn(8);
        when(difyClient.runPlannerWorkflow(anyMap(), eq("1"))).thenReturn(Map.of(
                "plan_json", """
                        {
                          "mode":"TOOL",
                          "confidence":0.97,
                          "actions":[{
                            "action_id":"a1",
                            "tool":"UPDATE_TASK_DRAFT",
                            "target":{"type":"ACTIVE_DRAFT","id":102},
                            "arguments":{"start_time_reference":{
                              "relation":"AFTER","boundary":"END","task_id":501,"task_query":"高数"
                            }}
                          }]
                        }
                        """
        ));

        AgentPlan plan = planner().plan(1L, "FEISHU", "接着高数", null);

        assertEquals(AgentPlanMode.TOOL, plan.getMode());
        assertEquals(0.97, plan.getConfidence());
        assertTrue(plan.isUsable(0.72));
        assertEquals(ToolNames.UPDATE_TASK_DRAFT, plan.primaryTool());
        assertEquals(102L, plan.getActions().get(0).getTarget().getId());
        assertTrue(plan.getActions().get(0).getArguments().get("start_time_reference") instanceof Map);
    }

    @Test
    void rejectsToolsOutsideTheBackendAllowlist() {
        when(difyClient.isPlannerWorkflowConfigured()).thenReturn(true);
        when(contextAssembler.buildInputs(1L, "FEISHU", "执行SQL", null)).thenReturn(Map.of());
        when(difyClient.runPlannerWorkflow(anyMap(), eq("1"))).thenReturn(Map.of(
                "plan_json", """
                        {"mode":"TOOL","confidence":1,"actions":[{"tool":"execute_sql","arguments":{}}]}
                        """
        ));

        AgentPlan plan = planner().plan(1L, "FEISHU", "执行SQL", null);

        assertEquals(AgentPlanMode.UNKNOWN, plan.getMode());
        assertFalse(plan.isUsable(0.1));
        assertEquals("dify-planner-invalid", plan.getSource());
    }

    @Test
    void rejectsTargetsOutsideTheBackendAllowlist() {
        when(difyClient.isPlannerWorkflowConfigured()).thenReturn(true);
        when(contextAssembler.buildInputs(1L, "FEISHU", "修改数据库", null)).thenReturn(Map.of());
        when(difyClient.runPlannerWorkflow(anyMap(), eq("1"))).thenReturn(Map.of(
                "plan_json", """
                        {
                          "mode":"TOOL",
                          "confidence":1,
                          "actions":[{
                            "tool":"create_task",
                            "target":{"type":"DATABASE","id":1},
                            "arguments":{"title":"test"}
                          }]
                        }
                        """
        ));

        AgentPlan plan = planner().plan(1L, "FEISHU", "修改数据库", null);

        assertEquals(AgentPlanMode.UNKNOWN, plan.getMode());
        assertFalse(plan.isUsable(0.1));
        assertEquals("dify-planner-invalid", plan.getSource());
    }

    private DifyAgentPlanner planner() {
        return new DifyAgentPlanner(
                difyClient,
                contextAssembler,
                plannerProperties,
                new ObjectMapper()
        );
    }
}
