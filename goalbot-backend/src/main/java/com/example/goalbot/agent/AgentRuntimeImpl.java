package com.example.goalbot.agent;

import com.example.goalbot.dto.command.CommandIntent;
import com.example.goalbot.dto.conversation.ConversationTurn;
import com.example.goalbot.entity.ConversationTaskDraft;
import com.example.goalbot.service.CommandLogService;
import com.example.goalbot.service.ConversationStateService;
import com.example.goalbot.service.ConversationTaskDraftService;
import com.example.goalbot.service.NaturalLanguageCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AgentRuntimeImpl implements AgentRuntime {

    private static final Pattern CHECKIN_PATTERN = Pattern.compile(
            "^/?打卡\\s*(.+?)\\s*(\\d+(?:\\.\\d+)?)\\s*(分钟|分|min|m|小时|h)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DATE_PATTERN = Pattern.compile("(?:((?:19|20)\\d{2})\\s*[-/年.]\\s*)?([0-9]{1,2})\\s*[-/月.]\\s*([0-9]{1,2})\\s*日?");
    private static final Pattern WEEKDAY_PATTERN = Pattern.compile("(下周|本周|这周)?(?:周|星期|礼拜)([一二三四五六日天1-7])");
    private static final Pattern SCHEDULE_ENTRY_PATTERN = Pattern.compile(
            "(?:(?:19|20)\\d{2}\\s*[./\\-年]\\s*)?\\d{1,2}\\s*(?:[./\\-]|月)\\s*\\d{1,2}\\s*(?:日|号)?"
                    + "\\s*[,，、;；]?\\s*\\d{1,2}\\s*[:：]\\s*\\d{1,2}"
                    + "\\s*(?:-|~|—|–|至|到)\\s*\\d{1,2}\\s*[:：]\\s*\\d{1,2}",
            Pattern.CASE_INSENSITIVE
    );

    private final NaturalLanguageCommandService naturalLanguageCommandService;
    private final ConversationTaskDraftService draftService;
    private final ConversationStateService conversationStateService;
    private final CommandLogService commandLogService;
    private final ToolExecutor toolExecutor;

    @Override
    public AgentReply handle(UserMessage message) {
        String text = normalize(message.getText());
        Long userId = message.getUserId();
        Long commandLogId = commandLogService.beginCommand(userId, message.getMessageId(), text);
        if (commandLogId == null) {
            return AgentReply.ok(null, null, null);
        }

        ConversationTurn turn = conversationStateService.beginTurn(userId, message.getChannel(), message.getMessageId(), text);
        CommandIntent intent = null;
        ToolCall call = null;
        ToolResult result = null;
        String errorMessage = null;

        try {
            ConversationTaskDraft activeDraft = draftService.getActiveDraft(userId).orElse(null);
            CommandIntent directIntent = parseDirect(text);
            if (activeDraft != null && shouldHandleOutsideDraft(directIntent)) {
                draftService.cancelActiveDraft(userId);
                intent = directIntent;
                call = toToolCall(intent, text, turn.getSessionId());
                result = toolExecutor.execute(userId, call);
                return toReply(result, intent, call);
            }
            if (activeDraft != null && shouldContinueDraft(text, activeDraft)) {
                intent = CommandIntent.of(CommandIntent.Intent.CREATE_TASK);
                intent.setSource("agent-draft");
                call = new ToolCall();
                call.setTool(ToolNames.UPDATE_TASK_DRAFT);
                call.setArguments(Map.of(
                        "text", text,
                        "source_text", text,
                        "session_id", turn.getSessionId()
                ));
                result = toolExecutor.execute(userId, call);
                return toReply(result, intent, call);
            }
            if (activeDraft != null && shouldCancelDraftForNewTopic(text, activeDraft)) {
                draftService.cancelActiveDraft(userId);
            }

            intent = parseIntent(userId, text);
            call = toToolCall(intent, text, turn.getSessionId());
            result = toolExecutor.execute(userId, call);
            return toReply(result, intent, call);
        } catch (RuntimeException ex) {
            errorMessage = ex.getMessage();
            result = ToolResult.failed("处理失败：" + (StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "未知错误"));
            return AgentReply.failed(result.getMessage(), intent, call == null ? null : call.getTool(), errorMessage);
        } finally {
            String replyContent = result == null ? null : result.getMessage();
            boolean success = result != null && result.isSuccess();
            commandLogService.finishCommand(commandLogId, intent, success, errorMessage, replyContent);
            conversationStateService.finishTurn(turn, intent == null || intent.getIntent() == null ? null : intent.getIntent().name(), replyContent);
        }
    }

    private AgentReply toReply(ToolResult result, CommandIntent intent, ToolCall call) {
        if (result == null) {
            return AgentReply.failed("处理失败：工具没有返回结果。", intent, call == null ? null : call.getTool(), "empty tool result");
        }
        if (result.isSuccess()) {
            return AgentReply.ok(result.getMessage(), intent, call == null ? null : call.getTool());
        }
        return AgentReply.failed(result.getMessage(), intent, call == null ? null : call.getTool(), result.getMessage());
    }

    private boolean shouldHandleOutsideDraft(CommandIntent intent) {
        return intent != null
                && intent.getIntent() != null
                && !intent.is(CommandIntent.Intent.UNKNOWN);
    }

    @Override
    public CommandIntent parseIntent(Long userId, String text) {
        text = normalize(text);
        CommandIntent direct = parseDirect(text);
        if (!direct.is(CommandIntent.Intent.UNKNOWN)) {
            return direct;
        }
        CommandIntent natural = naturalLanguageCommandService.parse(userId, text);
        if (natural != null && !natural.is(CommandIntent.Intent.UNKNOWN)) {
            return natural;
        }
        CommandIntent unknown = CommandIntent.of(CommandIntent.Intent.UNKNOWN);
        unknown.setConfidence(0.0);
        unknown.setSource(natural == null ? "agent-unknown" : natural.getSource());
        return unknown;
    }

    private CommandIntent parseDirect(String text) {
        if (!StringUtils.hasText(text)) {
            return CommandIntent.of(CommandIntent.Intent.HELP);
        }
        var importedScheduleCancellation = ImportedScheduleCancellationParser.parse(text, LocalDate.now());
        if (importedScheduleCancellation.isPresent()) {
            ImportedScheduleCancellationParser.Request request = importedScheduleCancellation.get();
            CommandIntent intent = CommandIntent.of(CommandIntent.Intent.CANCEL_IMPORTED_SCHEDULE);
            intent.setSource("rule-imported-schedule-cancel");
            intent.setSentenceType("COMMAND");
            intent.setActionType("WRITE");
            intent.setRangeStartDate(request.startDate() == null ? null : request.startDate().toString());
            intent.setRangeEndDate(request.endDate() == null ? null : request.endDate().toString());
            intent.setTaskKeyword(request.courseKeyword());
            boolean missingRange = request.startDate() == null || request.endDate() == null;
            intent.setRequiresConfirmation(false);
            intent.setMissingSlots(missingRange
                    ? java.util.List.of("range_start_date", "range_end_date")
                    : java.util.List.of());
            intent.setClarifyingQuestion(missingRange
                    ? "请告诉我这一周对应的日期，例如“6月22日到6月28日”。"
                    : null);
            return intent;
        }
        var taskCancellation = TaskCancellationParser.parse(text, LocalDate.now());
        if (taskCancellation.isPresent()) {
            TaskCancellationParser.Request request = taskCancellation.get();
            CommandIntent intent = CommandIntent.of(CommandIntent.Intent.CANCEL_TASKS);
            intent.setSource("rule-task-cancel");
            intent.setSentenceType("COMMAND");
            intent.setActionType("WRITE");
            intent.setPlanDate(request.planDate() == null ? null : request.planDate().toString());
            intent.setRangeStartDate(request.rangeStartDate() == null ? null : request.rangeStartDate().toString());
            intent.setRangeEndDate(request.rangeEndDate() == null ? null : request.rangeEndDate().toString());
            intent.setTaskKeyword(request.taskKeyword());
            intent.setRequiresConfirmation(false);
            intent.setMissingSlots(java.util.List.of());
            return intent;
        }
        if (isScheduleCreationCommand(text)) {
            CommandIntent intent = CommandIntent.of(CommandIntent.Intent.CREATE_TASK);
            intent.setSource("rule-schedule-create");
            intent.setSentenceType("COMMAND");
            intent.setActionType("WRITE");
            intent.setRequiresConfirmation(false);
            intent.setMissingSlots(java.util.List.of());
            return intent;
        }
        if (isCancelTasksCommand(text)) {
            CommandIntent intent = CommandIntent.of(CommandIntent.Intent.CANCEL_TASKS);
            intent.setSentenceType("COMMAND");
            intent.setActionType("WRITE");
            intent.setPlanDate(resolveCancelDate(text));
            intent.setRequiresConfirmation(false);
            intent.setMissingSlots(java.util.List.of());
            return intent;
        }
        if (isUpdateTaskScheduleCommand(text)) {
            CommandIntent intent = CommandIntent.of(CommandIntent.Intent.UPDATE_TASK_SCHEDULE);
            intent.setSentenceType("COMMAND");
            intent.setActionType("WRITE");
            intent.setPlanDate(resolveTaskQueryDate(text) == null ? LocalDate.now().toString() : resolveTaskQueryDate(text).toString());
            intent.setRequiresConfirmation(false);
            intent.setMissingSlots(java.util.List.of());
            return intent;
        }
        CommandIntent dateTaskQuery = parseDateTaskQuery(text);
        if (!dateTaskQuery.is(CommandIntent.Intent.UNKNOWN)) {
            return dateTaskQuery;
        }
        if (isTodayCommand(text)) {
            CommandIntent intent = CommandIntent.of(CommandIntent.Intent.TODAY_TASKS);
            intent.setSentenceType("QUESTION");
            intent.setActionType("READ");
            intent.setRequiresConfirmation(false);
            intent.setMissingSlots(java.util.List.of());
            return intent;
        }
        if (Objects.equals(text, "/进度") || Objects.equals(text, "进度")
                || Objects.equals(text, "/目标") || Objects.equals(text, "目标")
                || text.contains("目标进度") || text.contains("目标状态")) {
            return CommandIntent.of(CommandIntent.Intent.GOAL_STATUS);
        }
        if (Objects.equals(text, "/建议") || Objects.equals(text, "建议") || text.contains("怎么安排") || text.contains("先做什么")) {
            return CommandIntent.of(CommandIntent.Intent.ADVICE);
        }
        if (Objects.equals(text, "/复盘") || Objects.equals(text, "复盘") || text.contains("今日复盘") || text.contains("今天复盘")) {
            return CommandIntent.of(CommandIntent.Intent.DAILY_REVIEW);
        }
        if (Objects.equals(text, "/周报") || Objects.equals(text, "周报") || text.contains("周报") || text.contains("本周总结")) {
            return CommandIntent.of(CommandIntent.Intent.WEEKLY_REVIEW);
        }
        if (Objects.equals(text, "/帮助") || Objects.equals(text, "帮助")) {
            return CommandIntent.of(CommandIntent.Intent.HELP);
        }
        if (text.startsWith("/打卡") || text.startsWith("打卡")) {
            Matcher matcher = CHECKIN_PATTERN.matcher(text);
            CommandIntent intent = CommandIntent.of(CommandIntent.Intent.CHECKIN);
            if (matcher.matches()) {
                intent.setTaskKeyword(matcher.group(1).trim());
                intent.setActualMinutes(parseMinutes(matcher.group(2), matcher.group(3)));
            }
            return intent;
        }
        return CommandIntent.of(CommandIntent.Intent.UNKNOWN);
    }

    private ToolCall toToolCall(CommandIntent intent, String text, Long sessionId) {
        ToolCall call = new ToolCall();
        if (intent == null || intent.getIntent() == null || intent.is(CommandIntent.Intent.UNKNOWN)) {
            call.setTool(ToolNames.FREE_CHAT);
            call.setArguments(Map.of("text", text));
            return call;
        }

        call.setTool(switch (intent.getIntent()) {
            case TODAY_TASKS -> ToolNames.LIST_TODAY_TASKS;
            case LIST_TASKS_BY_DATE -> ToolNames.LIST_TASKS_BY_DATE;
            case CREATE_TASK -> ToolNames.CREATE_TASK;
            case UPDATE_TASK_SCHEDULE -> ToolNames.UPDATE_TASK_SCHEDULE;
            case CANCEL_TASKS -> ToolNames.CANCEL_TASKS;
            case CANCEL_IMPORTED_SCHEDULE -> ToolNames.CANCEL_IMPORTED_SCHEDULE;
            case CHECKIN -> ToolNames.CHECKIN_TASK;
            case GOAL_STATUS -> ToolNames.GOAL_STATUS;
            case ADVICE -> ToolNames.GENERATE_ADVICE;
            case DAILY_REVIEW -> ToolNames.DAILY_REVIEW;
            case WEEKLY_REVIEW -> ToolNames.WEEKLY_REVIEW;
            case HELP -> ToolNames.HELP;
            case UNKNOWN -> ToolNames.FREE_CHAT;
        });
        call.setConfidence(intent.getConfidence());
        call.setMissingSlots(intent.getMissingSlots() == null ? java.util.List.of() : intent.getMissingSlots());
        call.setRequiresConfirmation(Boolean.TRUE.equals(intent.getRequiresConfirmation()));

        Map<String, Object> args = new LinkedHashMap<>();
        args.put("text", text);
        args.put("source_text", text);
        args.put("session_id", sessionId);
        args.put("task_keyword", intent.getTaskKeyword());
        args.put("task_title", intent.getTaskTitle());
        args.put("title", intent.getTaskTitle());
        args.put("description", intent.getDescription());
        args.put("plan_date", intent.getPlanDate());
        args.put("range_start_date", intent.getRangeStartDate());
        args.put("range_end_date", intent.getRangeEndDate());
        args.put("start_time", intent.getStartTime());
        args.put("end_time", intent.getEndTime());
        args.put("planned_minutes", intent.getPlannedMinutes());
        args.put("goal_id", intent.getGoalId());
        args.put("goal_keyword", intent.getGoalKeyword());
        args.put("actual_minutes", intent.getActualMinutes());
        call.setArguments(args);
        return call;
    }

    private boolean shouldContinueDraft(String text, ConversationTaskDraft draft) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        if (isCancel(text)) {
            return true;
        }
        if (text.startsWith("/") || isTodayCommand(text) || isCancelTasksCommand(text) || looksLikeNewTaskList(text)) {
            return false;
        }
        return isSlotCompletion(text) || mentionsDraftTitle(text, draft);
    }

    private boolean shouldCancelDraftForNewTopic(String text, ConversationTaskDraft draft) {
        if (!StringUtils.hasText(text) || draft == null || text.startsWith("/")) {
            return false;
        }
        if (isSlotCompletion(text) || mentionsDraftTitle(text, draft)) {
            return false;
        }
        return looksLikeNewTaskList(text) || text.contains("安排") || text.contains("计划") || text.contains("要");
    }

    private boolean isSlotCompletion(String text) {
        return text.contains("今天") || text.contains("今日") || text.contains("明天") || text.contains("后天")
                || text.contains("今晚") || text.contains("晚上") || text.contains("下午") || text.contains("上午")
                || text.contains("分钟") || text.contains("小时") || text.contains("半小时")
                || text.contains("点") || text.contains("点半") || text.contains(":");
    }

    private boolean mentionsDraftTitle(String text, ConversationTaskDraft draft) {
        if (draft == null || !StringUtils.hasText(draft.getTitle())) {
            return false;
        }
        return text.contains(draft.getTitle()) || draft.getTitle().contains(text);
    }

    private boolean looksLikeNewTaskList(String text) {
        if (!StringUtils.hasText(text) || isSlotCompletion(text)) {
            return false;
        }
        String[] parts = text.split("[,，、;；]");
        if (parts.length < 2) {
            return false;
        }
        int meaningful = 0;
        for (String part : parts) {
            String cleaned = normalize(part)
                    .replaceAll("今天|今日|还有|有|要|任务|待办|日程|安排", "")
                    .trim();
            if (cleaned.length() >= 2) {
                meaningful++;
            }
        }
        return meaningful >= 2;
    }

    private boolean isScheduleCreationCommand(String text) {
        if (!StringUtils.hasText(text) || !SCHEDULE_ENTRY_PATTERN.matcher(text).find()) {
            return false;
        }
        boolean createSignal = text.contains("创建") || text.contains("新建") || text.contains("新增")
                || text.contains("添加") || text.contains("导入") || text.contains("安排")
                || text.contains("记一下") || text.contains("记录");
        if (createSignal) {
            return true;
        }
        boolean scheduleScope = text.contains("日程") || text.contains("任务") || text.contains("课表")
                || text.contains("课程") || text.contains("安排");
        boolean querySignal = text.contains("吗") || text.contains("有没有") || text.contains("哪些")
                || text.contains("什么") || text.contains("查询") || text.contains("查看");
        return scheduleScope && !querySignal && countScheduleEntries(text) >= 2;
    }

    private int countScheduleEntries(String text) {
        Matcher matcher = SCHEDULE_ENTRY_PATTERN.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
            if (count >= 2) {
                break;
            }
        }
        return count;
    }

    private boolean isTodayCommand(String text) {
        return Objects.equals(text, "/今日")
                || Objects.equals(text, "今日")
                || Objects.equals(text, "/今天")
                || Objects.equals(text, "今天")
                || text.contains("今日任务")
                || text.contains("今天任务")
                || text.contains("今日有任务")
                || text.contains("今天有任务")
                || text.contains("今日还有任务")
                || text.contains("今天还有任务")
                || text.contains("今天有什么任务")
                || text.contains("今日有什么任务")
                || text.contains("今天待办")
                || text.contains("今日待办")
                || text.contains("今天有什么安排")
                || text.contains("今日有什么安排");
    }

    private CommandIntent parseDateTaskQuery(String text) {
        LocalDate date = resolveTaskQueryDate(text);
        if (date == null) {
            return CommandIntent.of(CommandIntent.Intent.UNKNOWN);
        }
        if (!looksLikeTaskQuery(text)) {
            return CommandIntent.of(CommandIntent.Intent.UNKNOWN);
        }

        CommandIntent intent = CommandIntent.of(date.equals(LocalDate.now())
                ? CommandIntent.Intent.TODAY_TASKS
                : CommandIntent.Intent.LIST_TASKS_BY_DATE);
        intent.setPlanDate(date.toString());
        intent.setSentenceType("QUESTION");
        intent.setActionType("READ");
        intent.setRequiresConfirmation(false);
        intent.setMissingSlots(java.util.List.of());
        return intent;
    }

    private boolean looksLikeTaskQuery(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        boolean taskScope = text.contains("任务") || text.contains("待办") || text.contains("日程")
                || text.contains("安排") || text.contains("事情") || text.contains("事");
        boolean querySignal = text.contains("什么") || text.contains("哪些") || text.contains("有没有")
                || text.contains("有吗") || text.contains("有任务") || text.contains("待办")
                || text.contains("安排了") || text.contains("查看") || text.contains("查询")
                || text.endsWith("任务") || text.endsWith("安排") || text.startsWith("/");
        boolean createSignal = text.contains("创建") || text.contains("新建") || text.contains("新增")
                || text.contains("添加") || text.contains("帮我记") || text.contains("提醒我");
        return taskScope && querySignal && !createSignal;
    }

    private LocalDate resolveTaskQueryDate(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        LocalDate today = LocalDate.now();
        if (text.contains("大后天")) {
            return today.plusDays(3);
        }
        if (text.contains("后天")) {
            return today.plusDays(2);
        }
        if (text.contains("明天") || text.contains("明日")) {
            return today.plusDays(1);
        }
        if (text.contains("昨天")) {
            return today.minusDays(1);
        }
        if (text.contains("今天") || text.contains("今日")) {
            return today;
        }

        Matcher dateMatcher = DATE_PATTERN.matcher(text);
        if (dateMatcher.find()) {
            try {
                int year = StringUtils.hasText(dateMatcher.group(1)) ? Integer.parseInt(dateMatcher.group(1)) : today.getYear();
                int month = Integer.parseInt(dateMatcher.group(2));
                int day = Integer.parseInt(dateMatcher.group(3));
                LocalDate date = LocalDate.of(year, month, day);
                if (!StringUtils.hasText(dateMatcher.group(1)) && date.isBefore(today)) {
                    return date.plusYears(1);
                }
                return date;
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        Matcher weekdayMatcher = WEEKDAY_PATTERN.matcher(text);
        if (weekdayMatcher.find()) {
            DayOfWeek dayOfWeek = parseDayOfWeek(weekdayMatcher.group(2));
            if (dayOfWeek == null) {
                return null;
            }
            if ("下周".equals(weekdayMatcher.group(1))) {
                return today.with(TemporalAdjusters.next(dayOfWeek));
            }
            return today.with(TemporalAdjusters.nextOrSame(dayOfWeek));
        }
        return null;
    }

    private DayOfWeek parseDayOfWeek(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return switch (value) {
            case "一", "1" -> DayOfWeek.MONDAY;
            case "二", "2" -> DayOfWeek.TUESDAY;
            case "三", "3" -> DayOfWeek.WEDNESDAY;
            case "四", "4" -> DayOfWeek.THURSDAY;
            case "五", "5" -> DayOfWeek.FRIDAY;
            case "六", "6" -> DayOfWeek.SATURDAY;
            case "日", "天", "7" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    private boolean isCancelTasksCommand(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        boolean cancelVerb = text.contains("取消") || text.contains("清空") || text.contains("删掉") || text.contains("删除");
        boolean taskScope = text.contains("任务") || text.contains("待办") || text.contains("日程") || text.contains("安排");
        boolean dateScope = text.contains("今天") || text.contains("今日") || text.contains("明天") || text.contains("明日");
        boolean bulkScope = text.contains("所有") || text.contains("全部") || text.contains("全都") || text.contains("今天")
                || text.contains("今日") || text.contains("明天") || text.contains("明日");
        return cancelVerb && taskScope && dateScope && bulkScope;
    }

    private boolean isUpdateTaskScheduleCommand(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        boolean updateVerb = text.contains("调整") || text.contains("修改") || text.contains("更改")
                || text.contains("改一下") || text.contains("改到") || text.contains("改成");
        boolean scheduleScope = text.contains("任务") || text.contains("时间") || text.contains("日程")
                || text.contains("安排") || text.contains("几点") || text.contains(":") || text.contains("点");
        boolean createSignal = text.contains("创建") || text.contains("新建") || text.contains("新增") || text.contains("添加");
        return updateVerb && scheduleScope && !createSignal;
    }

    private String resolveCancelDate(String text) {
        if (text.contains("明天") || text.contains("明日")) {
            return java.time.LocalDate.now().plusDays(1).toString();
        }
        if (text.contains("今天") || text.contains("今日")) {
            return java.time.LocalDate.now().toString();
        }
        return null;
    }

    private boolean isCancel(String text) {
        return StringUtils.hasText(text)
                && (text.contains("取消") || text.contains("算了") || text.contains("不用了") || text.contains("先不"));
    }

    private int parseMinutes(String value, String unit) {
        double number = Double.parseDouble(value);
        return ("小时".equals(unit) || "h".equalsIgnoreCase(unit))
                ? (int) Math.round(number * 60)
                : (int) Math.round(number);
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }
}
