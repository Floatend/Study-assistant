package com.example.goalbot.agent.plan;

import com.example.goalbot.agent.ToolCall;
import com.example.goalbot.agent.ToolExecutor;
import com.example.goalbot.agent.ToolNames;
import com.example.goalbot.agent.ToolResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AgentPlanExecutor {

    private final ToolExecutor toolExecutor;

    public AgentPlanExecution execute(Long userId, Long sessionId, String rawText, AgentPlan plan) {
        if (plan == null || plan.getMode() == null) {
            return failed("规划结果为空。", null);
        }
        if (Boolean.TRUE.equals(plan.getRequiresConfirmation())) {
            String question = StringUtils.hasText(plan.getClarifyingQuestion())
                    ? plan.getClarifyingQuestion()
                    : "这个操作需要确认，请明确回复是否继续。";
            return new AgentPlanExecution(ToolResult.ok(question), plan.primaryTool());
        }
        if (plan.getMode() == AgentPlanMode.CLARIFY) {
            return new AgentPlanExecution(ToolResult.ok(plan.getClarifyingQuestion()), null);
        }
        if (plan.getMode() == AgentPlanMode.CHAT) {
            if (StringUtils.hasText(plan.getAssistantReply())) {
                return new AgentPlanExecution(ToolResult.ok(plan.getAssistantReply()), ToolNames.FREE_CHAT);
            }
            ToolCall chatCall = baseCall(ToolNames.FREE_CHAT, sessionId, rawText, Map.of("text", rawText));
            return new AgentPlanExecution(toolExecutor.execute(userId, chatCall), ToolNames.FREE_CHAT);
        }
        if (plan.getMode() != AgentPlanMode.TOOL || plan.getActions() == null || plan.getActions().isEmpty()) {
            return failed("规划结果没有可执行动作。", null);
        }

        List<ToolResult> results = new ArrayList<>();
        for (AgentAction action : plan.getActions()) {
            if (Boolean.TRUE.equals(action.getRequiresConfirmation())) {
                return new AgentPlanExecution(
                        ToolResult.ok("操作「" + action.getTool() + "」需要确认后才能执行。"),
                        plan.primaryTool()
                );
            }
            ToolCall call = baseCall(action.getTool(), sessionId, rawText, action.getArguments());
            call.setConfidence(plan.getConfidence());
            call.setMissingSlots(action.getMissingSlots() == null ? List.of() : action.getMissingSlots());
            call.setRequiresConfirmation(false);
            if (action.getTarget() != null) {
                call.getArguments().putIfAbsent("target_type", action.getTarget().getType());
                call.getArguments().putIfAbsent("target_id", action.getTarget().getId());
                if ("ACTIVE_DRAFT".equalsIgnoreCase(action.getTarget().getType())) {
                    call.getArguments().putIfAbsent("draft_id", action.getTarget().getId());
                }
            }
            ToolResult result = toolExecutor.execute(userId, call);
            results.add(result);
            if (result == null || !result.isSuccess()) {
                return new AgentPlanExecution(
                        result == null ? ToolResult.failed("工具没有返回结果。") : result,
                        plan.primaryTool()
                );
            }
        }

        String message = results.stream()
                .map(ToolResult::getMessage)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n\n"));
        Object data = results.size() == 1 ? results.get(0).getData() : results.stream().map(ToolResult::getData).toList();
        return new AgentPlanExecution(ToolResult.ok(message, data), plan.primaryTool());
    }

    private ToolCall baseCall(String tool, Long sessionId, String rawText, Map<String, Object> arguments) {
        ToolCall call = new ToolCall();
        call.setTool(tool);
        Map<String, Object> merged = new LinkedHashMap<>();
        if (arguments != null) {
            merged.putAll(arguments);
        }
        merged.putIfAbsent("text", rawText);
        merged.putIfAbsent("source_text", rawText);
        merged.putIfAbsent("session_id", sessionId);
        call.setArguments(merged);
        return call;
    }

    private AgentPlanExecution failed(String message, String tool) {
        return new AgentPlanExecution(ToolResult.failed(message), tool);
    }
}
