package com.example.goalbot.service.impl;

import com.example.goalbot.agent.ImportedScheduleCancellationParser;
import com.example.goalbot.agent.TaskCancellationParser;
import com.example.goalbot.dto.command.CommandIntent;
import com.example.goalbot.integration.dify.DifyClient;
import com.example.goalbot.integration.dify.DifyException;
import com.example.goalbot.integration.dify.DifyProperties;
import com.example.goalbot.service.GoalService;
import com.example.goalbot.service.IntentWorkflowService;
import com.example.goalbot.service.NaturalLanguageCommandService;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.GoalVO;
import com.example.goalbot.vo.TaskVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaturalLanguageCommandServiceImpl implements NaturalLanguageCommandService {

    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*(个小时|小时|h|分钟|分|min|m)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DATE_PATTERN = Pattern.compile("(?:([0-9]{4})[-/年])?([0-9]{1,2})[-/月]([0-9]{1,2})日?");
    private static final Pattern WEEKDAY_PATTERN = Pattern.compile("(下周|本周|这周)?(?:周|星期|礼拜)([一二三四五六日天1-7])");
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile(
            "(?:(上午|早上|下午|晚上|中午|凌晨)\\s*)?(\\d{1,2})(?::|点)(\\d{1,2})?(半)?\\s*(?:到|至|-|~|—)\\s*(?:(上午|早上|下午|晚上|中午|凌晨)\\s*)?(\\d{1,2})(?::|点)(\\d{1,2})?(半)?",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SINGLE_TIME_PATTERN = Pattern.compile(
            "(?:(上午|早上|下午|晚上|中午|凌晨)\\s*)?(\\d{1,2})(?::|点)(\\d{1,2})?(半)?",
            Pattern.CASE_INSENSITIVE
    );

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
    private final DifyProperties difyProperties;
    private final TaskService taskService;
    private final GoalService goalService;
    private final IntentWorkflowService intentWorkflowService;
    private final ObjectMapper objectMapper;

    @Override
    public CommandIntent parse(Long userId, String text) {
        String normalized = normalize(text);
        if (!StringUtils.hasText(normalized)) {
            return unknown("empty");
        }

        List<TaskVO> todayTasks = taskService.listTodayTasks(userId);
        List<GoalVO> goals = goalService.listGoals(userId, null, null, null);

        CommandIntent workflowIntent = intentWorkflowService.parseIntent(userId, normalized, todayTasks, goals);
        if (isUsable(workflowIntent)) {
            return workflowIntent;
        }

        CommandIntent ruleIntent = parseByRules(normalized, todayTasks, goals);
        if (isUsable(ruleIntent)) {
            return ruleIntent;
        }

        return workflowIntent == null ? unknown("workflow-rule") : workflowIntent;
    }

    private CommandIntent parseByRules(String text, List<TaskVO> todayTasks, List<GoalVO> goals) {
        var importedScheduleCancellation = ImportedScheduleCancellationParser.parse(text, LocalDate.now());
        if (importedScheduleCancellation.isPresent()) {
            ImportedScheduleCancellationParser.Request request = importedScheduleCancellation.get();
            CommandIntent commandIntent = intent(CommandIntent.Intent.CANCEL_IMPORTED_SCHEDULE, 0.96, "rule");
            commandIntent.setRangeStartDate(request.startDate() == null ? null : request.startDate().toString());
            commandIntent.setRangeEndDate(request.endDate() == null ? null : request.endDate().toString());
            commandIntent.setTaskKeyword(request.courseKeyword());
            commandIntent.setSentenceType("COMMAND");
            commandIntent.setActionType("WRITE");
            commandIntent.setRequiresConfirmation(false);
            commandIntent.setMissingSlots(request.startDate() == null || request.endDate() == null
                    ? List.of("range_start_date", "range_end_date")
                    : List.of());
            return commandIntent;
        }
        var taskCancellation = TaskCancellationParser.parse(text, LocalDate.now());
        if (taskCancellation.isPresent()) {
            TaskCancellationParser.Request request = taskCancellation.get();
            CommandIntent commandIntent = intent(CommandIntent.Intent.CANCEL_TASKS, 0.96, "rule");
            commandIntent.setPlanDate(request.planDate() == null ? null : request.planDate().toString());
            commandIntent.setRangeStartDate(request.rangeStartDate() == null ? null : request.rangeStartDate().toString());
            commandIntent.setRangeEndDate(request.rangeEndDate() == null ? null : request.rangeEndDate().toString());
            commandIntent.setTaskKeyword(request.taskKeyword());
            commandIntent.setSentenceType("COMMAND");
            commandIntent.setActionType("WRITE");
            commandIntent.setRequiresConfirmation(false);
            commandIntent.setMissingSlots(List.of());
            return commandIntent;
        }
        if (containsAny(text, "帮助", "怎么用", "有哪些命令", "指令")) {
            return intent(CommandIntent.Intent.HELP, 0.9, "rule");
        }
        if (containsAny(text, "周报", "本周总结", "周总结", "周复盘")) {
            return intent(CommandIntent.Intent.WEEKLY_REVIEW, 0.9, "rule");
        }
        if (containsAny(text, "今日复盘", "今天复盘", "每日复盘", "日复盘", "生成今天复盘", "今天总结", "总结今天")) {
            return intent(CommandIntent.Intent.DAILY_REVIEW, 0.9, "rule");
        }
        if (containsAny(text, "建议", "怎么安排", "如何安排", "先做什么", "该先做什么", "今天怎么做", "规划一下", "安排一下")) {
            return intent(CommandIntent.Intent.ADVICE, 0.86, "rule");
        }
        if (containsAny(text, "目标概览", "当前目标", "目标状态", "目标情况", "目标进展", "目标怎么样", "目标现在怎么样")
                || (text.contains("目标") && containsAny(text, "进度", "状态", "完成", "情况", "怎么样", "如何", "咋样"))) {
            return intent(CommandIntent.Intent.GOAL_STATUS, 0.86, "rule");
        }
        if (looksLikeCancelTasks(text)) {
            CommandIntent commandIntent = intent(CommandIntent.Intent.CANCEL_TASKS, 0.9, "rule");
            LocalDate planDate = resolvePlanDate(text);
            commandIntent.setPlanDate(planDate == null ? null : planDate.toString());
            commandIntent.setSentenceType("COMMAND");
            commandIntent.setActionType("WRITE");
            commandIntent.setRequiresConfirmation(false);
            commandIntent.setMissingSlots(planDate == null ? List.of("plan_date") : List.of());
            return commandIntent;
        }
        if (looksLikeTaskQuery(text)) {
            LocalDate planDate = resolvePlanDate(text);
            if (planDate != null) {
                CommandIntent commandIntent = intent(planDate.equals(LocalDate.now())
                                ? CommandIntent.Intent.TODAY_TASKS
                                : CommandIntent.Intent.LIST_TASKS_BY_DATE,
                        0.88,
                        "rule");
                commandIntent.setPlanDate(planDate.toString());
                commandIntent.setSentenceType("QUESTION");
                commandIntent.setActionType("READ");
                commandIntent.setRequiresConfirmation(false);
                commandIntent.setMissingSlots(List.of());
                return commandIntent;
            }
        }
        if (containsAny(text,
                "今日有任务", "今天有任务", "今日还有任务", "今天还有任务",
                "今天有什么任务", "今日有什么任务", "今日任务", "今天任务",
                "今天待办", "今日待办", "今天有什么待办", "今日有什么待办",
                "今天有什么安排", "今日有什么安排", "今天安排了什么", "今日安排了什么",
                "今天要做什么", "我今天要做什么")) {
            return intent(CommandIntent.Intent.TODAY_TASKS, 0.86, "rule");
        }

        DurationMatch duration = findDuration(text);
        String taskKeyword = resolveTaskKeyword(text, todayTasks, duration);
        if (looksLikeCheckin(text, duration, taskKeyword)) {
            CommandIntent commandIntent = intent(CommandIntent.Intent.CHECKIN,
                    StringUtils.hasText(taskKeyword) && duration != null ? 0.86 : 0.65,
                    "rule");
            commandIntent.setTaskKeyword(taskKeyword);
            commandIntent.setActualMinutes(duration == null ? null : duration.minutes());
            return commandIntent;
        }

        CommandIntent createTaskIntent = parseCreateTask(text, goals, duration);
        if (isUsable(createTaskIntent)) {
            return createTaskIntent;
        }

        return unknown("rule");
    }

    private CommandIntent parseCreateTask(String text, List<GoalVO> goals, DurationMatch duration) {
        if (looksLikeTaskQuery(text)) {
            return unknown("rule");
        }
        if (containsAny(text, "打卡", "完成了", "做完", "写完", "学了", "学习了")) {
            return unknown("rule");
        }

        TimeRange timeRange = findTimeRange(text);
        LocalDate planDate = resolvePlanDate(text);
        boolean explicit = containsAny(text, "创建", "新建", "新增", "添加", "安排", "加入", "建一个", "加一个", "提醒我", "帮我记", "记一个");
        boolean scheduleLike = planDate != null || timeRange != null || duration != null;
        boolean taskLike = containsAny(text, "任务", "待办", "日程", "计划");
        if (!explicit && !(scheduleLike && taskLike)) {
            return unknown("rule");
        }

        String title = resolveCreateTaskTitle(text, duration, timeRange);
        if (!StringUtils.hasText(title)) {
            return unknown("rule");
        }

        GoalVO matchedGoal = matchGoal(text, goals);
        Integer plannedMinutes = duration == null ? null : duration.minutes();
        if (plannedMinutes == null && timeRange != null && timeRange.endTime() != null) {
            plannedMinutes = minutesBetween(timeRange.startTime(), timeRange.endTime());
        }

        CommandIntent commandIntent = intent(CommandIntent.Intent.CREATE_TASK,
                planDate != null || timeRange != null || duration != null ? 0.84 : 0.72,
                "rule");
        commandIntent.setTaskTitle(title);
        commandIntent.setTaskKeyword(title);
        commandIntent.setPlanDate((planDate == null ? LocalDate.now() : planDate).toString());
        commandIntent.setStartTime(timeRange == null || timeRange.startTime() == null ? null : timeRange.startTime().toString());
        commandIntent.setEndTime(timeRange == null || timeRange.endTime() == null ? null : timeRange.endTime().toString());
        commandIntent.setPlannedMinutes(plannedMinutes == null ? 0 : plannedMinutes);
        if (matchedGoal != null) {
            commandIntent.setGoalId(matchedGoal.getId());
            commandIntent.setGoalKeyword(matchedGoal.getTitle());
        }
        return commandIntent;
    }

    private boolean looksLikeTaskQuery(String text) {
        boolean questionLike = containsAny(text, "吗", "么", "有没有", "有吗", "有什么", "哪些", "几项", "多少", "安排了什么");
        boolean taskLike = containsAny(text, "任务", "待办", "日程", "安排", "事情", "事");
        boolean dateLike = containsAny(text, "今天", "今日", "明天", "明日", "后天", "大后天", "昨天", "本周", "这周")
                || resolvePlanDate(text) != null;
        return questionLike && taskLike && dateLike;
    }

    private boolean looksLikeCancelTasks(String text) {
        boolean cancelVerb = containsAny(text, "取消", "清空", "删掉", "删除");
        boolean taskScope = containsAny(text, "任务", "待办", "日程", "安排");
        boolean dateScope = containsAny(text, "今天", "今日", "明天", "明日");
        boolean bulkScope = containsAny(text, "所有", "全部", "全都", "今天", "今日", "明天", "明日");
        return cancelVerb && taskScope && dateScope && bulkScope;
    }

    private CommandIntent parseByDify(Long userId, String text, List<TaskVO> todayTasks, List<GoalVO> goals) {
        Map<String, Object> inputs = buildInputs(text, todayTasks, goals);
        String user = String.valueOf(userId);

        if (shouldUseWorkflowParser()) {
            try {
                return normalizeIntent(difyClient.runWorkflow(inputs, user), "dify-workflow");
            } catch (DifyException ex) {
                log.warn("Dify workflow command parser failed: {}", ex.getMessage());
            }
        }

        if (difyClient.isConfigured()) {
            try {
                String answer = difyClient.chat(buildPrompt(text, todayTasks, goals), inputs, user);
                return normalizeIntent(parseJsonObject(answer), "dify-chat");
            } catch (DifyException ex) {
                log.warn("Dify chat command parser failed: {}", ex.getMessage());
            } catch (RuntimeException ex) {
                log.warn("Failed to parse Dify command parser answer: {}", ex.getMessage());
            }
        }

        return unknown("dify");
    }

    private boolean shouldUseWorkflowParser() {
        return difyClient.isWorkflowConfigured()
                && (StringUtils.hasText(difyProperties.getWorkflowApiUrl())
                || StringUtils.hasText(difyProperties.getWorkflowApiKey()));
    }

    private Map<String, Object> buildInputs(String text, List<TaskVO> todayTasks, List<GoalVO> goals) {
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("text", text);
        inputs.put("today", LocalDate.now().toString());
        inputs.put("supported_intents", toJsonText(INTENTS));
        inputs.put("today_tasks", todayTasks.stream().map(this::taskInput).toList());
        inputs.put("current_goals", goals.stream().map(this::goalInput).toList());
        return inputs;
    }

    private String toJsonText(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("Failed to serialize command parser input: {}", ex.getMessage());
            return "[]";
        }
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

    private String buildPrompt(String text, List<TaskVO> todayTasks, List<GoalVO> goals) {
        return """
                You are the natural language command parser for GoalBot.
                Return one JSON object only. Do not include Markdown, explanation, or chain-of-thought.

                Supported intents:
                TODAY_TASKS: user asks for today's task list.
                LIST_TASKS_BY_DATE: user asks for the task list or schedule for a specific non-today date.
                CREATE_TASK: user wants to create a future or current task/schedule item in GoalBot.
                UPDATE_TASK_SCHEDULE: user wants to adjust an existing task's start time or duration.
                CANCEL_TASKS: user wants to cancel tasks for a date or date range, such as "取消今天所有任务" or "把下下周的任务删掉".
                CANCEL_IMPORTED_SCHEDULE: user wants to remove ICS-imported courses for a week or date range.
                CHECKIN: user wants to record completed work or study time.
                GOAL_STATUS: user asks about current goals or goal status.
                ADVICE: user asks for today's suggestion, schedule, or what to do first.
                DAILY_REVIEW: user asks for today's daily review or today's summary.
                WEEKLY_REVIEW: user asks for weekly review or weekly report.
                HELP: user asks what commands are supported.
                UNKNOWN: none of the above.

                Required JSON shape:
                {
                  "intent": "CREATE_TASK",
                  "task_title": "task title, null if unknown",
                  "description": "optional description",
                  "plan_date": "yyyy-MM-dd; only CREATE_TASK may default to today when omitted",
                  "range_start_date": "yyyy-MM-dd, for a date-range cancellation",
                  "range_end_date": "yyyy-MM-dd, for a date-range cancellation",
                  "start_time": "HH:mm, null if unknown",
                  "end_time": "HH:mm, null if unknown",
                  "planned_minutes": 50,
                  "goal_id": 1,
                  "goal_keyword": "matching goal title or keyword",
                  "task_keyword": "for CHECKIN only",
                  "actual_minutes": 50,
                  "confidence": 0.92
                }

                Rules:
                - Use only the provided inputs.
                - For LIST_TASKS_BY_DATE, set plan_date to the requested date.
                - For UPDATE_TASK_SCHEDULE, set plan_date if known and extract start_time, end_time, planned_minutes when present.
                - For CREATE_TASK, extract a concise task_title. Do not include date/time/duration words in the title.
                - For CREATE_TASK, convert relative dates using today's date. Convert hours to minutes.
                - For CANCEL_TASKS, set plan_date for one day, or range_start_date and range_end_date for a whole week/range.
                - Treat "大后周" and "大下周" as "下下周". Never default a missing deletion date to today.
                - If a destructive request has no resolvable date, leave all date fields null and add the date to missing slots.
                - For CANCEL_IMPORTED_SCHEDULE, set range_start_date and range_end_date. Set task_keyword only for a named course.
                - CANCEL_IMPORTED_SCHEDULE is only for imported calendar courses, never ordinary manual tasks.
                - For CHECKIN, extract task_keyword from the user's words or a matching today_tasks title.
                - If task or minutes are missing, still return the likely intent with null missing fields.
                - If uncertain, return UNKNOWN with confidence below 0.5.

                User text:
                %s

                Today's tasks:
                %s

                Current goals:
                %s
                """.formatted(text, formatTodayTasks(todayTasks), formatGoals(goals));
    }

    private String formatTodayTasks(List<TaskVO> todayTasks) {
        if (todayTasks.isEmpty()) {
            return "No today tasks";
        }
        return todayTasks.stream()
                .map(task -> "- id=" + task.getId()
                        + ", title=" + task.getTitle()
                        + ", planned_minutes=" + task.getPlannedMinutes()
                        + ", status=" + task.getStatus())
                .collect(Collectors.joining("\n"));
    }

    private String formatGoals(List<GoalVO> goals) {
        if (goals.isEmpty()) {
            return "No goals";
        }
        return goals.stream()
                .map(goal -> "- id=" + goal.getId()
                        + ", title=" + goal.getTitle()
                        + ", status=" + goal.getStatus())
                .collect(Collectors.joining("\n"));
    }

    @SuppressWarnings("unchecked")
    private CommandIntent normalizeIntent(Map<String, Object> raw, String source) {
        if (raw == null || raw.isEmpty()) {
            return unknown(source);
        }

        Object nested = firstNonNull(raw, "result", "answer", "text", "json", "output", "data");
        if (!raw.containsKey("intent") && nested instanceof String nestedText) {
            return normalizeIntent(parseJsonObject(nestedText), source);
        }
        if (!raw.containsKey("intent") && nested instanceof Map<?, ?> nestedMap) {
            return normalizeIntent((Map<String, Object>) nestedMap, source);
        }

        CommandIntent commandIntent = new CommandIntent();
        commandIntent.setIntent(parseIntent(asString(firstNonNull(raw, "intent", "command", "action", "type"))));
        commandIntent.setTaskKeyword(asString(firstNonNull(raw, "task_keyword", "taskKeyword", "keyword")));
        commandIntent.setTaskTitle(asString(firstNonNull(raw, "task_title", "taskTitle", "title", "name", "task")));
        commandIntent.setDescription(asString(firstNonNull(raw, "description", "desc", "note")));
        commandIntent.setPlanDate(asString(firstNonNull(raw, "plan_date", "planDate", "date", "task_date", "taskDate")));
        commandIntent.setRangeStartDate(asString(firstNonNull(raw, "range_start_date", "rangeStartDate", "start_date", "startDate")));
        commandIntent.setRangeEndDate(asString(firstNonNull(raw, "range_end_date", "rangeEndDate", "end_date", "endDate")));
        commandIntent.setStartTime(normalizeTimeString(asString(firstNonNull(raw, "start_time", "startTime", "start"))));
        commandIntent.setEndTime(normalizeTimeString(asString(firstNonNull(raw, "end_time", "endTime", "end"))));
        commandIntent.setPlannedMinutes(parseActualMinutes(firstNonNull(raw, "planned_minutes", "plannedMinutes", "minutes", "duration_minutes", "durationMinutes")));
        commandIntent.setGoalId(parseLong(firstNonNull(raw, "goal_id", "goalId")));
        commandIntent.setGoalKeyword(asString(firstNonNull(raw, "goal_keyword", "goalKeyword", "goal_title", "goalTitle", "goal")));
        commandIntent.setActualMinutes(parseActualMinutes(firstNonNull(raw, "actual_minutes", "actualMinutes")));
        commandIntent.setConfidence(parseConfidence(firstNonNull(raw, "confidence", "score")));
        commandIntent.setSource(source);

        if (commandIntent.getIntent() == CommandIntent.Intent.CHECKIN
                && commandIntent.getActualMinutes() == null) {
            commandIntent.setActualMinutes(parseActualMinutes(asString(firstNonNull(raw, "duration", "time_spent", "timeSpent"))));
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
            log.warn("Command parser JSON is invalid: {}", ex.getMessage());
            return Map.of();
        }
    }

    private CommandIntent.Intent parseIntent(String value) {
        if (!StringUtils.hasText(value)) {
            return CommandIntent.Intent.UNKNOWN;
        }

        String compact = value.trim();
        String normalized = compact.toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        if (normalized.equals("CANCEL_IMPORTED_SCHEDULE")
                || normalized.equals("CANCEL_COURSES")
                || normalized.equals("DELETE_IMPORTED_SCHEDULE")
                || compact.contains("停课")
                || compact.contains("课程不上")
                || compact.contains("不上课")) {
            return CommandIntent.Intent.CANCEL_IMPORTED_SCHEDULE;
        }
        if (normalized.equals("CANCEL_TASKS")
                || normalized.equals("CANCEL_TASK")
                || normalized.equals("DELETE_TASKS")
                || normalized.equals("CLEAR_TASKS")
                || compact.contains("取消任务")
                || compact.contains("取消今天")
                || compact.contains("取消今日")
                || compact.contains("清空今天")
                || compact.contains("删除今天")) {
            return CommandIntent.Intent.CANCEL_TASKS;
        }
        if (normalized.equals("LIST_TASKS_BY_DATE")
                || normalized.equals("TASKS_BY_DATE")
                || normalized.equals("DATE_TASKS")
                || normalized.equals("SCHEDULE_QUERY")
                || normalized.equals("QUERY_TASKS")
                || normalized.equals("QUERY_SCHEDULE")) {
            return CommandIntent.Intent.LIST_TASKS_BY_DATE;
        }
        if (normalized.equals("TODAY_TASKS")
                || normalized.equals("TODAY_TASK")
                || normalized.equals("TODAY")
                || normalized.equals("TASKS")
                || compact.contains("今日")
                || compact.contains("今天")) {
            return CommandIntent.Intent.TODAY_TASKS;
        }
        if (normalized.equals("CREATE_TASK")
                || normalized.equals("TASK_CREATE")
                || normalized.equals("ADD_TASK")
                || normalized.equals("SCHEDULE")
                || compact.contains("创建任务")
                || compact.contains("新增任务")
                || compact.contains("安排任务")
                || compact.contains("日程")) {
            return CommandIntent.Intent.CREATE_TASK;
        }
        if (normalized.equals("UPDATE_TASK_SCHEDULE")
                || normalized.equals("UPDATE_TASK")
                || normalized.equals("ADJUST_TASK")
                || normalized.equals("ADJUST_SCHEDULE")
                || compact.contains("调整任务")
                || compact.contains("修改任务")
                || compact.contains("更改任务")
                || compact.contains("调整时间")
                || compact.contains("修改时间")) {
            return CommandIntent.Intent.UPDATE_TASK_SCHEDULE;
        }
        if (normalized.equals("CHECKIN")
                || normalized.equals("CHECK_IN")
                || normalized.equals("PUNCH")
                || normalized.equals("RECORD")
                || compact.contains("打卡")
                || compact.contains("记录")) {
            return CommandIntent.Intent.CHECKIN;
        }
        if (normalized.equals("GOAL_STATUS")
                || normalized.equals("GOALS")
                || normalized.equals("GOAL")
                || normalized.equals("PROGRESS")
                || compact.contains("目标")
                || compact.contains("进度")) {
            return CommandIntent.Intent.GOAL_STATUS;
        }
        if (normalized.equals("ADVICE")
                || normalized.equals("SUGGESTION")
                || normalized.equals("PLAN")
                || compact.contains("建议")
                || compact.contains("安排")) {
            return CommandIntent.Intent.ADVICE;
        }
        if (normalized.equals("DAILY_REVIEW")
                || normalized.equals("DAILY")
                || normalized.equals("DAY_REVIEW")
                || normalized.equals("TODAY_REVIEW")
                || compact.contains("今日复盘")
                || compact.contains("今天复盘")
                || compact.contains("每日复盘")
                || compact.contains("今天总结")
                || compact.contains("总结今天")) {
            return CommandIntent.Intent.DAILY_REVIEW;
        }
        if (normalized.equals("WEEKLY_REVIEW")
                || normalized.equals("WEEKLY")
                || normalized.equals("WEEK_REPORT")
                || compact.contains("周报")
                || compact.contains("周复盘")
                || compact.contains("本周总结")) {
            return CommandIntent.Intent.WEEKLY_REVIEW;
        }
        if (normalized.equals("HELP") || compact.contains("帮助")) {
            return CommandIntent.Intent.HELP;
        }
        return CommandIntent.Intent.UNKNOWN;
    }

    private DurationMatch findDuration(String text) {
        if (text.contains("半小时")) {
            return new DurationMatch("半小时", 30);
        }

        Matcher matcher = DURATION_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }

        double value = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2);
        int minutes = isHourUnit(unit) ? (int) Math.round(value * 60) : (int) Math.round(value);
        return new DurationMatch(matcher.group(0), minutes);
    }

    private LocalDate resolvePlanDate(String text) {
        LocalDate today = LocalDate.now();
        if (containsAny(text, "大后天")) {
            return today.plusDays(3);
        }
        if (containsAny(text, "后天")) {
            return today.plusDays(2);
        }
        if (containsAny(text, "明天", "明日")) {
            return today.plusDays(1);
        }
        if (containsAny(text, "今天", "今日")) {
            return today;
        }

        Matcher dateMatcher = DATE_PATTERN.matcher(text);
        if (dateMatcher.find()) {
            int year = StringUtils.hasText(dateMatcher.group(1)) ? Integer.parseInt(dateMatcher.group(1)) : today.getYear();
            int month = Integer.parseInt(dateMatcher.group(2));
            int day = Integer.parseInt(dateMatcher.group(3));
            LocalDate date = LocalDate.of(year, month, day);
            if (!StringUtils.hasText(dateMatcher.group(1)) && date.isBefore(today)) {
                date = date.plusYears(1);
            }
            return date;
        }

        Matcher weekdayMatcher = WEEKDAY_PATTERN.matcher(text);
        if (weekdayMatcher.find()) {
            DayOfWeek dayOfWeek = parseDayOfWeek(weekdayMatcher.group(2));
            String prefix = weekdayMatcher.group(1);
            if ("下周".equals(prefix)) {
                return today.with(TemporalAdjusters.next(dayOfWeek));
            }
            return today.with(TemporalAdjusters.nextOrSame(dayOfWeek));
        }
        return null;
    }

    private TimeRange findTimeRange(String text) {
        Matcher rangeMatcher = TIME_RANGE_PATTERN.matcher(text);
        if (rangeMatcher.find()) {
            String startPart = rangeMatcher.group(0);
            String startModifier = rangeMatcher.group(1);
            String endModifier = rangeMatcher.group(5);
            if (!StringUtils.hasText(endModifier)) {
                endModifier = startModifier;
            }
            LocalTime start = parseTime(startModifier, rangeMatcher.group(2), rangeMatcher.group(3), rangeMatcher.group(4));
            LocalTime end = parseTime(endModifier, rangeMatcher.group(6), rangeMatcher.group(7), rangeMatcher.group(8));
            return new TimeRange(startPart, start, end);
        }

        Matcher singleMatcher = SINGLE_TIME_PATTERN.matcher(text);
        if (singleMatcher.find()) {
            LocalTime start = parseTime(singleMatcher.group(1), singleMatcher.group(2), singleMatcher.group(3), singleMatcher.group(4));
            return new TimeRange(singleMatcher.group(0), start, null);
        }
        return null;
    }

    private LocalTime parseTime(String modifier, String hourText, String minuteText, String half) {
        int hour = Integer.parseInt(hourText);
        int minute = StringUtils.hasText(minuteText) ? Integer.parseInt(minuteText) : 0;
        if (StringUtils.hasText(half)) {
            minute = 30;
        }
        if (containsAny(Objects.toString(modifier, ""), "下午", "晚上") && hour < 12) {
            hour += 12;
        }
        if ("中午".equals(modifier) && hour < 11) {
            hour += 12;
        }
        if ("凌晨".equals(modifier) && hour == 12) {
            hour = 0;
        }
        return LocalTime.of(Math.floorMod(hour, 24), minute);
    }

    private boolean looksLikeCheckin(String text, DurationMatch duration, String taskKeyword) {
        boolean explicit = containsAny(text, "打卡", "记录一下", "记一下", "登记");
        boolean doneVerb = containsAny(text,
                "完成", "做完", "写完", "学了", "学习了", "看了", "看完", "刷了", "练了", "搞定", "提交了");
        return duration != null && (explicit || doneVerb || StringUtils.hasText(taskKeyword) && doneVerb);
    }

    private String resolveTaskKeyword(String text, List<TaskVO> todayTasks, DurationMatch duration) {
        String matchedTitle = todayTasks.stream()
                .map(TaskVO::getTitle)
                .filter(StringUtils::hasText)
                .filter(text::contains)
                .max((left, right) -> Integer.compare(left.length(), right.length()))
                .orElse(null);
        if (StringUtils.hasText(matchedTitle)) {
            return matchedTitle;
        }

        String cleaned = text;
        if (duration != null) {
            cleaned = cleaned.replace(duration.raw(), " ");
        }
        cleaned = cleaned.replaceAll("[，。！？、,.!?]", " ");
        cleaned = cleaned.replaceAll("/?打卡", " ");
        cleaned = cleaned.replaceAll("帮我|给我|麻烦|一下|已经|刚刚|今天|今日|我|把", " ");
        cleaned = cleaned.replaceAll("记录|记一下|登记|完成了|完成|做完了|做完|写完了|写完|学习了|学了|看完了|看了|刷了|练了|搞定了|搞定|提交了|提交|用了|用时|花了", " ");
        cleaned = normalize(cleaned);
        return cleaned.length() > 40 ? "" : cleaned;
    }

    private String resolveCreateTaskTitle(String text, DurationMatch duration, TimeRange timeRange) {
        String cleaned = text;
        if (duration != null) {
            cleaned = cleaned.replace(duration.raw(), " ");
        }
        if (timeRange != null) {
            cleaned = cleaned.replace(timeRange.raw(), " ");
        }
        cleaned = cleaned.replaceAll("[，。！？、,.!?]", " ");
        cleaned = cleaned.replaceAll("(?:[0-9]{4}[-/年])?[0-9]{1,2}[-/月][0-9]{1,2}日?", " ");
        cleaned = cleaned.replaceAll("(下周|本周|这周)?(?:周|星期|礼拜)[一二三四五六日天1-7]", " ");
        cleaned = cleaned.replaceAll("大后天|后天|明天|明日|今天|今日|上午|早上|下午|晚上|中午|凌晨", " ");
        cleaned = cleaned.replaceAll("帮我|给我|麻烦|请|我想|我要|把|在|于|一个|一下", " ");
        cleaned = cleaned.replaceAll("创建|新建|新增|添加|安排|加入|建立|建|加|提醒我|提醒|记一个|帮我记|计划", " ");
        cleaned = cleaned.replaceAll("任务|待办|日程|事项", " ");
        cleaned = normalize(cleaned);
        return cleaned.length() > 60 ? cleaned.substring(0, 60) : cleaned;
    }

    private GoalVO matchGoal(String text, List<GoalVO> goals) {
        return goals.stream()
                .filter(goal -> StringUtils.hasText(goal.getTitle()) && text.contains(goal.getTitle()))
                .max((left, right) -> Integer.compare(left.getTitle().length(), right.getTitle().length()))
                .orElse(null);
    }

    private int minutesBetween(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return 0;
        }
        int minutes = (int) java.time.Duration.between(start, end).toMinutes();
        return minutes < 0 ? minutes + 24 * 60 : minutes;
    }

    private Integer parseActualMinutes(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return Math.max(0, (int) Math.round(number.doubleValue()));
        }
        String text = asString(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        DurationMatch duration = findDuration(text);
        if (duration != null) {
            return duration.minutes();
        }
        try {
            return Math.max(0, (int) Math.round(Double.parseDouble(text.trim())));
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

    private String normalizeTimeString(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        if (text.matches("\\d{1,2}:\\d{2}(:\\d{2})?")) {
            return text.length() == 5 ? text : text.substring(0, 5);
        }
        TimeRange timeRange = findTimeRange(text);
        if (timeRange != null && timeRange.startTime() != null) {
            return timeRange.startTime().toString();
        }
        return text;
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

    private DayOfWeek parseDayOfWeek(String value) {
        return switch (value) {
            case "一", "1" -> DayOfWeek.MONDAY;
            case "二", "2" -> DayOfWeek.TUESDAY;
            case "三", "3" -> DayOfWeek.WEDNESDAY;
            case "四", "4" -> DayOfWeek.THURSDAY;
            case "五", "5" -> DayOfWeek.FRIDAY;
            case "六", "6" -> DayOfWeek.SATURDAY;
            default -> DayOfWeek.SUNDAY;
        };
    }

    private Object firstNonNull(Map<String, Object> raw, String... keys) {
        for (String key : keys) {
            if (raw.containsKey(key) && raw.get(key) != null) {
                return raw.get(key);
            }
        }
        return null;
    }

    private boolean isConfident(CommandIntent intent) {
        return isUsable(intent) && nullToZero(intent.getConfidence()) >= 0.8;
    }

    private boolean isUsable(CommandIntent intent) {
        return intent != null
                && intent.getIntent() != null
                && intent.getIntent() != CommandIntent.Intent.UNKNOWN
                && (intent.getConfidence() == null || intent.getConfidence() >= 0.5);
    }

    private CommandIntent intent(CommandIntent.Intent intent, double confidence, String source) {
        CommandIntent commandIntent = new CommandIntent();
        commandIntent.setIntent(intent);
        commandIntent.setConfidence(confidence);
        commandIntent.setSource(source);
        return commandIntent;
    }

    private CommandIntent unknown(String source) {
        return intent(CommandIntent.Intent.UNKNOWN, 0.0, source);
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean isHourUnit(String unit) {
        return "小时".equals(unit) || "个小时".equals(unit) || "h".equalsIgnoreCase(unit);
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = Objects.toString(value, "").trim();
        return StringUtils.hasText(text) ? text : null;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }

    private double nullToZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private record DurationMatch(String raw, int minutes) {
    }

    private record TimeRange(String raw, LocalTime startTime, LocalTime endTime) {
    }
}
