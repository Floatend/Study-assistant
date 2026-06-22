package com.example.goalbot.service.impl;

import com.example.goalbot.dto.command.CommandIntent;
import com.example.goalbot.entity.ConversationMessage;
import com.example.goalbot.integration.dify.DifyClient;
import com.example.goalbot.integration.dify.DifyException;
import com.example.goalbot.service.ConversationStateService;
import com.example.goalbot.service.IntentWorkflowService;
import com.example.goalbot.vo.GoalVO;
import com.example.goalbot.vo.TaskVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentWorkflowServiceImpl implements IntentWorkflowService {

    private static final List<String> INTENTS = List.of(
            "TODAY_TASKS",
            "LIST_TASKS_BY_DATE",
            "CREATE_TASK",
            "UPDATE_TASK_SCHEDULE",
            "CANCEL_TASKS",
            "CANCEL_IMPORTED_SCHEDULE",
            "CHECKIN",
            "GOAL_STATUS",
            "ADVICE",
            "DAILY_REVIEW",
            "WEEKLY_REVIEW",
            "HELP",
            "UNKNOWN"
    );

    private final DifyClient difyClient;
    private final ConversationStateService conversationStateService;
    private final ObjectMapper objectMapper;

    @Override
    public CommandIntent parseIntent(Long userId, String text, List<TaskVO> todayTasks, List<GoalVO> goals) {
        if (!difyClient.isWorkflowConfigured()) {
            return unknown("dify-workflow-not-configured");
        }

        try {
            Map<String, Object> outputs = difyClient.runWorkflow(buildInputs(userId, text, todayTasks, goals), String.valueOf(userId));
            return normalizeIntent(outputs, "dify-workflow");
        } catch (DifyException ex) {
            log.warn("Dify intent workflow failed: {}", ex.getMessage());
            return unknown("dify-workflow-error");
        } catch (RuntimeException ex) {
            log.warn("Dify intent workflow output is invalid: {}", ex.getMessage());
            return unknown("dify-workflow-invalid");
        }
    }

    private Map<String, Object> buildInputs(Long userId, String text, List<TaskVO> todayTasks, List<GoalVO> goals) {
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("text", text);
        inputs.put("today", LocalDate.now().toString());
        inputs.put("supported_intents", toJsonText(INTENTS));
        inputs.put("today_tasks", toJsonText(todayTasks.stream().map(this::taskInput).toList()));
        inputs.put("current_goals", toJsonText(goals.stream().map(this::goalInput).toList()));
        inputs.put("recent_messages", toJsonText(conversationStateService.listRecentMessages(userId, "FEISHU", 8)
                .stream()
                .map(this::messageInput)
                .toList()));
        inputs.put("parser_contract", """
                Return a JSON object for GoalBot intent routing.
                Required fields:
                intent, sentence_type, action_type, confidence, task_title, description,
                plan_date, range_start_date, range_end_date, start_time, end_time, planned_minutes, goal_id, goal_keyword,
                task_keyword, actual_minutes, missing_slots, requires_confirmation,
                clarifying_question, assistant_reply.

                Use intent TODAY_TASKS for queries such as "今日有任务吗".
                Use LIST_TASKS_BY_DATE for queries about a specific non-today date, such as "明天有什么任务" or "周五安排了什么"; set plan_date.
                Use CREATE_TASK only when the user wants to add a task or schedule item.
                Use UPDATE_TASK_SCHEDULE when the user wants to adjust an existing task's time or duration, such as "调整任务时间".
                Use CANCEL_TASKS when the user wants to cancel tasks for one date or a date range,
                such as "取消今天所有任务" or "大后周的删掉".
                For one day set plan_date. For a week set range_start_date and range_end_date.
                Treat "大后周" and "大下周" as "下下周". Never default a missing deletion date to today.
                Use CANCEL_IMPORTED_SCHEDULE when the user says imported calendar courses will not happen during a week or date range,
                such as "下周的课不上了" or "6月22日到6月28日的高数课取消".
                For CANCEL_IMPORTED_SCHEDULE, set range_start_date and range_end_date as yyyy-MM-dd and set task_keyword only
                when a specific course is named. Never use this intent for ordinary manually created tasks.
                Use recent_messages to resolve short follow-ups like "七点半出发吧".
                If the previous user/assistant turns established the task title or date, carry them forward.
                Never invent task data. If required slots are missing, include them in missing_slots.
                """);
        return inputs;
    }

    private String toJsonText(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("Failed to serialize workflow input: {}", ex.getMessage());
            return "[]";
        }
    }

    private Map<String, Object> messageInput(ConversationMessage message) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("direction", message.getDirection());
        input.put("content", message.getContent());
        input.put("intent", message.getIntent());
        input.put("created_at", message.getCreatedAt() == null ? null : message.getCreatedAt().toString());
        return input;
    }

    private Map<String, Object> taskInput(TaskVO task) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("id", task.getId());
        input.put("title", task.getTitle());
        input.put("goal_title", task.getGoalTitle());
        input.put("planned_minutes", task.getPlannedMinutes());
        input.put("status", task.getStatus());
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

    @SuppressWarnings("unchecked")
    private CommandIntent normalizeIntent(Map<String, Object> raw, String source) {
        if (raw == null || raw.isEmpty()) {
            return unknown(source);
        }

        Object nested = firstNonNull(raw, "intent_frame", "intentFrame", "result", "answer", "text", "json", "output", "data");
        if (!raw.containsKey("intent") && nested instanceof String nestedText) {
            return normalizeIntent(parseJsonObject(nestedText), source);
        }
        if (!raw.containsKey("intent") && nested instanceof Map<?, ?> nestedMap) {
            return normalizeIntent((Map<String, Object>) nestedMap, source);
        }

        CommandIntent commandIntent = new CommandIntent();
        commandIntent.setIntent(parseIntentValue(asString(firstNonNull(raw, "intent", "command", "action", "type"))));
        commandIntent.setTaskKeyword(asString(firstNonNull(raw, "task_keyword", "taskKeyword", "keyword")));
        commandIntent.setTaskTitle(asString(firstNonNull(raw, "task_title", "taskTitle", "title", "name", "task")));
        commandIntent.setDescription(asString(firstNonNull(raw, "description", "desc", "note")));
        commandIntent.setPlanDate(asString(firstNonNull(raw, "plan_date", "planDate", "date", "task_date", "taskDate")));
        commandIntent.setRangeStartDate(asString(firstNonNull(raw, "range_start_date", "rangeStartDate", "start_date", "startDate")));
        commandIntent.setRangeEndDate(asString(firstNonNull(raw, "range_end_date", "rangeEndDate", "end_date", "endDate")));
        commandIntent.setStartTime(normalizeTimeString(asString(firstNonNull(raw, "start_time", "startTime", "start"))));
        commandIntent.setEndTime(normalizeTimeString(asString(firstNonNull(raw, "end_time", "endTime", "end"))));
        commandIntent.setPlannedMinutes(parseInteger(firstNonNull(raw, "planned_minutes", "plannedMinutes", "minutes", "duration_minutes", "durationMinutes")));
        commandIntent.setGoalId(parseLong(firstNonNull(raw, "goal_id", "goalId")));
        commandIntent.setGoalKeyword(asString(firstNonNull(raw, "goal_keyword", "goalKeyword", "goal_title", "goalTitle", "goal")));
        commandIntent.setActualMinutes(parseInteger(firstNonNull(raw, "actual_minutes", "actualMinutes")));
        commandIntent.setConfidence(parseConfidence(firstNonNull(raw, "confidence", "score")));
        commandIntent.setSentenceType(asString(firstNonNull(raw, "sentence_type", "sentenceType")));
        commandIntent.setActionType(asString(firstNonNull(raw, "action_type", "actionType")));
        commandIntent.setMissingSlots(parseStringList(firstNonNull(raw, "missing_slots", "missingSlots")));
        commandIntent.setRequiresConfirmation(parseBoolean(firstNonNull(raw, "requires_confirmation", "requiresConfirmation")));
        commandIntent.setClarifyingQuestion(asString(firstNonNull(raw, "clarifying_question", "clarifyingQuestion")));
        commandIntent.setAssistantReply(asString(firstNonNull(raw, "assistant_reply", "assistantReply", "reply")));
        commandIntent.setSource(source);

        if (commandIntent.getIntent() == CommandIntent.Intent.CHECKIN && commandIntent.getActualMinutes() == null) {
            commandIntent.setActualMinutes(parseInteger(firstNonNull(raw, "duration", "time_spent", "timeSpent")));
        }
        if (commandIntent.getIntent() == CommandIntent.Intent.CREATE_TASK) {
            if (!StringUtils.hasText(commandIntent.getTaskTitle()) && StringUtils.hasText(commandIntent.getTaskKeyword())) {
                commandIntent.setTaskTitle(commandIntent.getTaskKeyword());
            }
            if (!StringUtils.hasText(commandIntent.getTaskKeyword()) && StringUtils.hasText(commandIntent.getTaskTitle())) {
                commandIntent.setTaskKeyword(commandIntent.getTaskTitle());
            }
        }
        return commandIntent;
    }

    private Map<String, Object> parseJsonObject(String value) {
        if (!StringUtils.hasText(value)) {
            return Map.of();
        }

        String cleaned = value.replaceAll("(?is)<think>.*?</think>", "").trim();
        cleaned = cleaned.replaceAll("(?is)^```json\\s*", "")
                .replaceAll("(?is)^```\\s*", "")
                .replaceAll("(?is)```$", "")
                .trim();

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }

        try {
            return objectMapper.readValue(cleaned, new TypeReference<>() {
            });
        } catch (Exception ex) {
            log.warn("Intent workflow JSON is invalid: {}", ex.getMessage());
            return Map.of();
        }
    }

    private CommandIntent.Intent parseIntentValue(String value) {
        if (!StringUtils.hasText(value)) {
            return CommandIntent.Intent.UNKNOWN;
        }
        String normalized = value.trim().toUpperCase();
        try {
            return CommandIntent.Intent.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            String compact = normalized.replaceAll("[_\\s-]", "");
            if (compact.contains("CANCELIMPORTEDSCHEDULE") || compact.contains("CANCELCOURSE")
                    || compact.contains("DELETEIMPORTEDSCHEDULE") || compact.contains("停课")
                    || compact.contains("课程不上") || compact.contains("不上课")) {
                return CommandIntent.Intent.CANCEL_IMPORTED_SCHEDULE;
            }
            if (compact.contains("CANCELTASK") || compact.contains("DELETETASK") || compact.contains("CLEARTASK")
                    || compact.contains("取消任务") || compact.contains("取消今天") || compact.contains("取消今日")
                    || compact.contains("清空今天") || compact.contains("删除今天")) {
                return CommandIntent.Intent.CANCEL_TASKS;
            }
            if (compact.contains("UPDATETASK") || compact.contains("ADJUSTTASK") || compact.contains("ADJUSTSCHEDULE")
                    || compact.contains("调整任务") || compact.contains("修改任务") || compact.contains("调整时间") || compact.contains("修改时间")) {
                return CommandIntent.Intent.UPDATE_TASK_SCHEDULE;
            }
            if (compact.contains("TASKSBYDATE") || compact.contains("DATETASK") || compact.contains("SCHEDULEQUERY")
                    || compact.contains("QUERYTASK") || compact.contains("查询任务") || compact.contains("查询日程")
                    || compact.contains("明天任务") || compact.contains("后天任务")) {
                return CommandIntent.Intent.LIST_TASKS_BY_DATE;
            }
            if (compact.contains("TODAY") || compact.contains("TASKS") || compact.contains("今日") || compact.contains("今天")) {
                return CommandIntent.Intent.TODAY_TASKS;
            }
            if (compact.contains("CREATE") || compact.contains("ADD") || compact.contains("创建任务") || compact.contains("新增任务")) {
                return CommandIntent.Intent.CREATE_TASK;
            }
            if (compact.contains("CHECKIN") || compact.contains("打卡")) {
                return CommandIntent.Intent.CHECKIN;
            }
            if (compact.contains("GOAL") || compact.contains("PROGRESS") || compact.contains("目标")) {
                return CommandIntent.Intent.GOAL_STATUS;
            }
            if (compact.contains("ADVICE") || compact.contains("建议")) {
                return CommandIntent.Intent.ADVICE;
            }
            if (compact.contains("DAILY") || compact.contains("复盘")) {
                return CommandIntent.Intent.DAILY_REVIEW;
            }
            if (compact.contains("WEEKLY") || compact.contains("周报")) {
                return CommandIntent.Intent.WEEKLY_REVIEW;
            }
            if (compact.contains("HELP") || compact.contains("帮助")) {
                return CommandIntent.Intent.HELP;
            }
            return CommandIntent.Intent.UNKNOWN;
        }
    }

    private String normalizeTimeString(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > 5 ? trimmed.substring(0, 5) : trimmed;
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return Math.max(0, (int) Math.round(number.doubleValue()));
        }
        String text = Objects.toString(value, "").trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Math.max(0, (int) Math.round(Double.parseDouble(text.replaceAll("[^0-9.]", ""))));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(Objects.toString(value, "").trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double parseConfidence(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(Objects.toString(value, "").trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean parseBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = Objects.toString(value, "").trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return Boolean.parseBoolean(text);
    }

    private List<String> parseStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                String text = asString(item);
                if (StringUtils.hasText(text)) {
                    result.add(text);
                }
            }
            return result;
        }
        String text = Objects.toString(value, "").trim();
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        return List.of(text.split("\\s*,\\s*"));
    }

    private Object firstNonNull(Map<String, Object> map, String... keys) {
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

    private CommandIntent unknown(String source) {
        CommandIntent intent = new CommandIntent();
        intent.setIntent(CommandIntent.Intent.UNKNOWN);
        intent.setConfidence(0.0);
        intent.setSource(source);
        return intent;
    }
}
