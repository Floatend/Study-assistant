package com.example.goalbot.agent.plan;

import com.example.goalbot.agent.ToolNames;
import com.example.goalbot.entity.ConversationTaskDraft;
import com.example.goalbot.integration.dify.DifyClient;
import com.example.goalbot.integration.dify.DifyException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DifyAgentPlanner implements AgentPlanner {

    private static final Set<String> ALLOWED_TOOLS = Set.of(
            ToolNames.LIST_TODAY_TASKS,
            ToolNames.LIST_TASKS_BY_DATE,
            ToolNames.CREATE_TASK,
            ToolNames.UPDATE_TASK_DRAFT,
            ToolNames.UPDATE_TASK_SCHEDULE,
            ToolNames.CANCEL_TASKS,
            ToolNames.CANCEL_IMPORTED_SCHEDULE,
            ToolNames.CHECKIN_TASK,
            ToolNames.GOAL_STATUS,
            ToolNames.GENERATE_ADVICE,
            ToolNames.DAILY_REVIEW,
            ToolNames.WEEKLY_REVIEW,
            ToolNames.HELP,
            ToolNames.FREE_CHAT
    );
    private static final Set<String> ALLOWED_TARGET_TYPES = Set.of(
            "NONE",
            "ACTIVE_DRAFT",
            "TASK",
            "GOAL"
    );

    private final DifyClient difyClient;
    private final AgentPlanContextAssembler contextAssembler;
    private final AgentPlannerProperties plannerProperties;
    private final ObjectMapper objectMapper;

    @Override
    public AgentPlan plan(Long userId, String channel, String text, ConversationTaskDraft activeDraft) {
        if (!difyClient.isPlannerWorkflowConfigured()) {
            return AgentPlan.unknown("dify-planner-not-configured", null);
        }
        try {
            Map<String, Object> outputs = difyClient.runPlannerWorkflow(
                    contextAssembler.buildInputs(userId, channel, text, activeDraft),
                    String.valueOf(userId)
            );
            return normalize(outputs);
        } catch (DifyException ex) {
            log.warn("Dify agent planner failed: {}", ex.getMessage());
            return AgentPlan.unknown("dify-planner-error", ex.getMessage());
        } catch (RuntimeException ex) {
            log.warn("Dify agent planner output is invalid: {}", ex.getMessage());
            return AgentPlan.unknown("dify-planner-invalid", ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private AgentPlan normalize(Map<String, Object> rawOutputs) {
        Map<String, Object> raw = unwrap(rawOutputs);
        if (raw.isEmpty()) {
            return AgentPlan.unknown("dify-agent-plan", "Planner output is empty");
        }

        AgentPlan plan = new AgentPlan();
        plan.setMode(parseMode(first(raw, "mode", "plan_mode", "planMode")));
        plan.setConfidence(parseDouble(first(raw, "confidence", "score"), 0.0));
        plan.setRequiresConfirmation(parseBoolean(first(raw,
                "requires_confirmation", "requiresConfirmation"), false));
        plan.setClarifyingQuestion(asString(first(raw,
                "clarifying_question", "clarifyingQuestion", "question")));
        plan.setAssistantReply(asString(first(raw,
                "assistant_reply", "assistantReply", "reply")));
        plan.setSource("dify-agent-plan");

        Object actionsValue = first(raw, "actions", "tool_calls", "toolCalls");
        if (actionsValue == null && raw.get("action") != null) {
            actionsValue = List.of(raw.get("action"));
        }
        List<AgentAction> actions = parseActions(actionsValue);
        if (actions.size() > plannerProperties.getMaxActions()) {
            return AgentPlan.unknown("dify-agent-plan", "Planner returned too many actions");
        }
        plan.setActions(actions);
        if (plan.getMode() == AgentPlanMode.UNKNOWN && !actions.isEmpty()) {
            plan.setMode(AgentPlanMode.TOOL);
        }
        if (plan.getMode() == AgentPlanMode.TOOL && actions.isEmpty()) {
            return AgentPlan.unknown("dify-agent-plan", "TOOL plan has no actions");
        }
        return plan;
    }

    @SuppressWarnings("unchecked")
    private List<AgentAction> parseActions(Object value) {
        List<?> rawActions;
        if (value instanceof List<?> list) {
            rawActions = list;
        } else if (value instanceof Map<?, ?> map) {
            rawActions = List.of(map);
        } else if (value instanceof String text) {
            Object parsed = parseJsonValue(text);
            return parseActions(parsed);
        } else {
            return List.of();
        }

        List<AgentAction> actions = new ArrayList<>();
        Set<String> actionIds = new LinkedHashSet<>();
        for (int index = 0; index < rawActions.size(); index++) {
            Object item = rawActions.get(index);
            if (!(item instanceof Map<?, ?> rawMap)) {
                throw new IllegalArgumentException("Action at index " + index + " is not an object");
            }
            Map<String, Object> map = (Map<String, Object>) rawMap;
            String tool = normalizeTool(asString(first(map, "tool", "name", "action", "type")));
            if (!ALLOWED_TOOLS.contains(tool)) {
                throw new IllegalArgumentException("Unsupported planner tool: " + tool);
            }

            AgentAction action = new AgentAction();
            String actionId = asString(first(map, "action_id", "actionId", "id"));
            if (!StringUtils.hasText(actionId)) {
                actionId = "a" + (index + 1);
            }
            if (!actionIds.add(actionId)) {
                throw new IllegalArgumentException("Duplicated action_id: " + actionId);
            }
            action.setActionId(actionId);
            action.setTool(tool);
            action.setTarget(parseTarget(first(map, "target", "action_target", "actionTarget")));
            action.setArguments(parseArguments(first(map, "arguments", "args", "parameters", "inputs")));
            action.setMissingSlots(parseStringList(first(map, "missing_slots", "missingSlots")));
            action.setRequiresConfirmation(parseBoolean(first(map,
                    "requires_confirmation", "requiresConfirmation"), false));
            actions.add(action);
        }
        return List.copyOf(actions);
    }

    @SuppressWarnings("unchecked")
    private AgentTarget parseTarget(Object value) {
        AgentTarget target = new AgentTarget();
        target.setType("NONE");
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = (Map<String, Object>) rawMap;
            String type = asString(first(map, "type", "target_type", "targetType"));
            String normalizedType = StringUtils.hasText(type) ? type.toUpperCase(Locale.ROOT) : "NONE";
            if (!ALLOWED_TARGET_TYPES.contains(normalizedType)) {
                throw new IllegalArgumentException("Unsupported planner target type: " + normalizedType);
            }
            target.setType(normalizedType);
            target.setId(parseLong(first(map, "id", "target_id", "targetId")));
        }
        return target;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(Object value) {
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        if (value instanceof String text) {
            Object parsed = parseJsonValue(text);
            if (parsed instanceof Map<?, ?> map) {
                return new LinkedHashMap<>((Map<String, Object>) map);
            }
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrap(Map<String, Object> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return Map.of();
        }
        if (outputs.containsKey("mode") || outputs.containsKey("actions")) {
            return outputs;
        }
        Object nested = first(outputs,
                "plan_json", "planJson", "agent_plan", "agentPlan",
                "result", "answer", "output", "text", "json", "data");
        if (nested instanceof Map<?, ?> map) {
            return unwrap((Map<String, Object>) map);
        }
        if (nested instanceof String text) {
            Object parsed = parseJsonValue(text);
            if (parsed instanceof Map<?, ?> map) {
                return unwrap((Map<String, Object>) map);
            }
        }
        return Map.of();
    }

    private Object parseJsonValue(String value) {
        if (!StringUtils.hasText(value)) {
            return Map.of();
        }
        String cleaned = value.replaceAll("(?is)<think>.*?</think>", "").trim()
                .replaceAll("(?is)^```json\\s*", "")
                .replaceAll("(?is)^```\\s*", "")
                .replaceAll("(?is)```$", "")
                .trim();
        int objectStart = cleaned.indexOf('{');
        int arrayStart = cleaned.indexOf('[');
        int start = objectStart < 0 ? arrayStart : arrayStart < 0 ? objectStart : Math.min(objectStart, arrayStart);
        int objectEnd = cleaned.lastIndexOf('}');
        int arrayEnd = cleaned.lastIndexOf(']');
        int end = Math.max(objectEnd, arrayEnd);
        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }
        try {
            return objectMapper.readValue(cleaned, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException("Planner JSON is invalid", ex);
        }
    }

    private AgentPlanMode parseMode(Object value) {
        String text = asString(value);
        if (!StringUtils.hasText(text)) {
            return AgentPlanMode.UNKNOWN;
        }
        try {
            return AgentPlanMode.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return AgentPlanMode.UNKNOWN;
        }
    }

    private String normalizeTool(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "today_tasks", "list_today" -> ToolNames.LIST_TODAY_TASKS;
            case "tasks_by_date", "list_date_tasks" -> ToolNames.LIST_TASKS_BY_DATE;
            case "create_task_draft", "add_task" -> ToolNames.CREATE_TASK;
            case "checkin" -> ToolNames.CHECKIN_TASK;
            case "advice" -> ToolNames.GENERATE_ADVICE;
            case "chat" -> ToolNames.FREE_CHAT;
            default -> normalized;
        };
    }

    private Object first(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key) && map.get(key) != null) {
                return map.get(key);
            }
        }
        return null;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = Objects.toString(value, "").trim();
        return StringUtils.hasText(text) && !"null".equalsIgnoreCase(text) ? text : null;
    }

    private Double parseDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? fallback : Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private Long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? null : Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean parseBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value == null ? fallback : Boolean.parseBoolean(value.toString());
    }

    private List<String> parseStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(this::asString)
                    .filter(StringUtils::hasText)
                    .toList();
        }
        String text = asString(value);
        return StringUtils.hasText(text) ? List.of(text.split("\\s*,\\s*")) : List.of();
    }
}
