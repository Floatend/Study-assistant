package com.example.goalbot.agent.plan;

import com.example.goalbot.agent.ToolNames;
import com.example.goalbot.entity.ConversationMessage;
import com.example.goalbot.entity.ConversationTaskDraft;
import com.example.goalbot.service.ConversationStateService;
import com.example.goalbot.service.ConversationTaskDraftService;
import com.example.goalbot.service.GoalService;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.GoalVO;
import com.example.goalbot.vo.TaskVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AgentPlanContextAssembler {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");
    private static final List<String> SUPPORTED_TOOLS = List.of(
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

    private final TaskService taskService;
    private final GoalService goalService;
    private final ConversationStateService conversationStateService;
    private final ConversationTaskDraftService draftService;
    private final ObjectMapper objectMapper;

    public Map<String, Object> buildInputs(
            Long userId,
            String channel,
            String text,
            ConversationTaskDraft activeDraft
    ) {
        LocalDate today = LocalDate.now(APP_ZONE);
        LocalDate rangeStart = today.minusDays(1);
        LocalDate rangeEnd = today.plusDays(7);
        if (activeDraft != null && activeDraft.getPlanDate() != null) {
            if (activeDraft.getPlanDate().isBefore(rangeStart)) {
                rangeStart = activeDraft.getPlanDate();
            }
            if (activeDraft.getPlanDate().isAfter(rangeEnd)) {
                rangeEnd = activeDraft.getPlanDate();
            }
        }

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("active_draft", draftInput(activeDraft));
        context.put("queued_drafts", draftService.listActiveDrafts(userId).stream()
                .map(this::draftInput)
                .toList());
        context.put("candidate_tasks", taskService.listCalendarTasks(userId, rangeStart, rangeEnd).stream()
                .map(this::taskInput)
                .toList());
        context.put("current_goals", goalService.listGoals(userId, null, null, null).stream()
                .map(this::goalInput)
                .toList());
        context.put("recent_messages", conversationStateService.listRecentMessages(userId, channel, 10).stream()
                .map(this::messageInput)
                .toList());

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("user_text", text == null ? "" : text);
        inputs.put("current_time", OffsetDateTime.now(APP_ZONE).toString());
        inputs.put("timezone", APP_ZONE.getId());
        inputs.put("context_json", toJson(context));
        inputs.put("supported_tools", toJson(SUPPORTED_TOOLS));
        inputs.put("planner_contract", plannerContract());
        return inputs;
    }

    private Map<String, Object> draftInput(ConversationTaskDraft draft) {
        if (draft == null) {
            return Map.of();
        }
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("id", draft.getId());
        input.put("title", draft.getTitle());
        input.put("plan_date", value(draft.getPlanDate()));
        input.put("start_time", value(draft.getStartTime()));
        input.put("end_time", value(draft.getEndTime()));
        input.put("planned_minutes", draft.getPlannedMinutes());
        input.put("missing_slots", draft.getMissingSlots());
        return input;
    }

    private Map<String, Object> taskInput(TaskVO task) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("id", task.getId());
        input.put("title", task.getTitle());
        input.put("plan_date", value(task.getPlanDate()));
        input.put("start_time", value(task.getStartTime()));
        input.put("end_time", value(task.getEndTime()));
        input.put("planned_minutes", task.getPlannedMinutes());
        input.put("status", task.getStatus());
        input.put("goal_id", task.getGoalId());
        input.put("goal_title", task.getGoalTitle());
        return input;
    }

    private Map<String, Object> goalInput(GoalVO goal) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("id", goal.getId());
        input.put("title", goal.getTitle());
        input.put("status", goal.getStatus());
        input.put("priority", goal.getPriority());
        return input;
    }

    private Map<String, Object> messageInput(ConversationMessage message) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("direction", message.getDirection());
        input.put("content", message.getContent());
        input.put("intent", message.getIntent());
        input.put("created_at", value(message.getCreatedAt()));
        return input;
    }

    private String value(Object value) {
        return value == null ? null : value.toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize agent planner context", ex);
        }
    }

    private String plannerContract() {
        return """
                Return one JSON object only. Do not return Markdown or chain-of-thought.
                Shape:
                {
                  "mode": "TOOL|CHAT|CLARIFY|UNKNOWN",
                  "confidence": 0.0,
                  "actions": [{
                    "action_id": "a1",
                    "tool": "one supported tool name",
                    "target": {"type": "ACTIVE_DRAFT|TASK|GOAL|NONE", "id": null},
                    "arguments": {},
                    "missing_slots": [],
                    "requires_confirmation": false
                  }],
                  "requires_confirmation": false,
                  "clarifying_question": null,
                  "assistant_reply": null
                }
                When an active draft exists, interpret short answers as updates to that draft.
                For phrases such as 接着高数 or 高数之后, use update_task_draft with:
                "start_time_reference": {"relation":"AFTER","boundary":"END","task_id":null,"task_query":"高数"}.
                Replace task_id only with the unique matching candidate task ID from context_json.
                Use only task and goal IDs present in context_json. Never invent IDs.
                If a reference is ambiguous, use CLARIFY and ask one focused question.
                For several new tasks, return one create_task action with a "tasks" array.
                Do not execute operations and do not claim an operation succeeded.
                """;
    }
}
