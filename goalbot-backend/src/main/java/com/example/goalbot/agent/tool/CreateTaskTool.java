package com.example.goalbot.agent.tool;

import com.example.goalbot.agent.ToolCall;
import com.example.goalbot.agent.ToolNames;
import com.example.goalbot.agent.ToolResult;
import com.example.goalbot.agent.dialogue.TaskDraftDecision;
import com.example.goalbot.agent.dialogue.TaskDraftFrame;
import com.example.goalbot.agent.dialogue.TaskDraftSnapshot;
import com.example.goalbot.agent.dialogue.TaskDraftTransition;
import com.example.goalbot.dto.task.TaskCreateRequest;
import com.example.goalbot.entity.ConversationTaskDraft;
import com.example.goalbot.service.ConversationTaskDraftService;
import com.example.goalbot.service.ConversationTransitionLogService;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CreateTaskTool extends AbstractAgentTool {

    private final TaskService taskService;
    private final ConversationTaskDraftService draftService;
    private final ConversationTransitionLogService transitionLogService;

    @Override
    public String name() {
        return ToolNames.CREATE_TASK;
    }

    @Override
    public ToolResult execute(Long userId, ToolCall call) {
        String sourceText = stringArg(call, "source_text");
        Object structuredTasks = call.arg("tasks");
        if (structuredTasks instanceof List<?> taskItems && !taskItems.isEmpty()) {
            return executeStructuredTasks(userId, call, sourceText, taskItems);
        }
        List<BatchScheduleParser.Entry> batchEntries = BatchScheduleParser.parse(sourceText);
        if (!batchEntries.isEmpty()) {
            return createBatchTasks(userId, batchEntries);
        }
        List<String> naturalTaskTitles = NaturalTaskListParser.parse(sourceText);
        if (!naturalTaskTitles.isEmpty()) {
            return enqueueNaturalTasks(userId, call, sourceText, naturalTaskTitles);
        }

        String title = stringArg(call, "task_title");
        if (!StringUtils.hasText(title)) {
            title = stringArg(call, "title");
        }
        if (!StringUtils.hasText(title)) {
            return ToolResult.failed("我理解你想创建任务，但还缺任务名。");
        }

        LocalDate planDate = dateArg(call, "plan_date");
        LocalTime startTime = timeArg(call, "start_time");
        LocalTime endTime = timeArg(call, "end_time");
        Integer plannedMinutes = intArg(call, "planned_minutes");
        if (endTime == null && startTime != null && plannedMinutes != null && plannedMinutes > 0) {
            endTime = startTime.plusMinutes(plannedMinutes);
        }

        if (startTime == null || (endTime == null && (plannedMinutes == null || plannedMinutes <= 0))) {
            ConversationTaskDraft existingDraft = draftService.getActiveDraft(userId).orElse(null);
            if (existingDraft != null) {
                return ToolResult.ok("我还在安排「" + existingDraft.getTitle() + "」，不会用新标题覆盖这个草稿。\n"
                        + "如果你是在补充时间，可以说“下午 3 点”“60 分钟”或“接着高数”；"
                        + "如果要放弃它，请说“取消当前任务”。", existingDraft);
            }
            ConversationTaskDraft draft = new ConversationTaskDraft();
            draft.setSessionId(longArg(call, "session_id"));
            draft.setTitle(title);
            draft.setDescription(stringArg(call, "description"));
            draft.setPlanDate(planDate == null ? LocalDate.now() : planDate);
            draft.setStartTime(startTime);
            draft.setEndTime(endTime);
            draft.setPlannedMinutes(plannedMinutes);
            draft.setGoalId(longArg(call, "goal_id"));
            draft.setGoalKeyword(stringArg(call, "goal_keyword"));
            draft.setMissingSlots(resolveMissingSlots(startTime, endTime, plannedMinutes));
            draft.setSourceText(stringArg(call, "source_text"));
            ConversationTaskDraft savedDraft = draftService.saveActiveDraft(userId, draft.getSessionId(), draft);
            String question = askForMissing(title, savedDraft.getPlanDate(), startTime, endTime, plannedMinutes);
            TaskDraftFrame frame = initialFrame(savedDraft, stringArg(call, "source_text"));
            TaskDraftTransition transition = TaskDraftTransition.builder()
                    .rawText(frame.getRawText())
                    .frame(frame)
                    .after(TaskDraftSnapshot.from(savedDraft))
                    .decision(TaskDraftDecision.NEEDS_INPUT)
                    .clarificationQuestion(question)
                    .build();
            transitionLogService.recordTaskDraftTransition(userId, savedDraft, "TASK_DRAFT_CREATED", transition);
            return ToolResult.ok(question, savedDraft);
        }

        TaskCreateRequest request = new TaskCreateRequest();
        request.setGoalId(longArg(call, "goal_id"));
        request.setTitle(title);
        request.setDescription(stringArg(call, "description"));
        request.setPlanDate(planDate == null ? LocalDate.now() : planDate);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setPlannedMinutes(plannedMinutes == null ? 0 : plannedMinutes);
        request.setStatus(0);

        TaskVO task = taskService.createTask(userId, request);
        draftService.completeActiveDraft(userId);
        return ToolResult.ok(taskCreatedReply(task), task);
    }

    private ToolResult executeStructuredTasks(
            Long userId,
            ToolCall parentCall,
            String sourceText,
            List<?> taskItems
    ) {
        List<ConversationTaskDraft> readyDrafts = new ArrayList<>();
        List<ConversationTaskDraft> pendingDrafts = new ArrayList<>();
        for (Object taskItem : taskItems) {
            if (!(taskItem instanceof Map<?, ?> taskMap)) {
                return ToolResult.failed("任务列表格式不正确，请重新描述要安排的任务。");
            }
            ConversationTaskDraft draft = structuredTaskDraft(parentCall, sourceText, taskMap);
            if (!StringUtils.hasText(draft.getTitle())) {
                return ToolResult.failed("我识别到了多个任务，但其中有任务缺少名称，请补充后再试。");
            }
            if (StringUtils.hasText(draft.getMissingSlots())) {
                pendingDrafts.add(draft);
            } else {
                readyDrafts.add(draft);
            }
        }

        if (!pendingDrafts.isEmpty()) {
            ConversationTaskDraft existingDraft = draftService.getActiveDraft(userId).orElse(null);
            if (existingDraft != null) {
                return ToolResult.ok("我还在安排「" + existingDraft.getTitle() + "」，不会用新任务覆盖这个草稿。\n"
                        + "请先补完当前任务，或说“取消当前任务”。", existingDraft);
            }
        }

        List<TaskVO> createdTasks = readyDrafts.stream()
                .map(draft -> createTask(userId, draft))
                .toList();
        if (pendingDrafts.isEmpty()) {
            draftService.completeActiveDraft(userId);
            return ToolResult.ok(batchCreatedReply(createdTasks), createdTasks);
        }

        Long sessionId = longArg(parentCall, "session_id");
        List<ConversationTaskDraft> savedDrafts = draftService.enqueueDrafts(userId, sessionId, pendingDrafts);
        recordQueuedDraftTransitions(userId, sourceText, savedDrafts);
        return ToolResult.ok(structuredTasksReply(createdTasks, savedDrafts), savedDrafts);
    }

    private ConversationTaskDraft structuredTaskDraft(
            ToolCall parentCall,
            String sourceText,
            Map<?, ?> taskMap
    ) {
        ToolCall itemCall = new ToolCall();
        Map<String, Object> arguments = new LinkedHashMap<>();
        taskMap.forEach((key, value) -> {
            if (key != null) {
                arguments.put(key.toString(), value);
            }
        });
        itemCall.setArguments(arguments);

        String title = stringArg(itemCall, "task_title");
        if (!StringUtils.hasText(title)) {
            title = stringArg(itemCall, "title");
        }
        LocalDate planDate = dateArg(itemCall, "plan_date");
        if (planDate == null) {
            planDate = dateArg(parentCall, "plan_date");
        }
        if (planDate == null) {
            planDate = parseRelativeDate(sourceText);
        }
        LocalTime startTime = timeArg(itemCall, "start_time");
        LocalTime endTime = timeArg(itemCall, "end_time");
        Integer plannedMinutes = intArg(itemCall, "planned_minutes");
        if (endTime == null && startTime != null && plannedMinutes != null && plannedMinutes > 0) {
            endTime = startTime.plusMinutes(plannedMinutes);
        }
        if (plannedMinutes == null && startTime != null && endTime != null) {
            long minutes = Duration.between(startTime, endTime).toMinutes();
            if (minutes > 0 && minutes <= Integer.MAX_VALUE) {
                plannedMinutes = (int) minutes;
            }
        }

        Long goalId = longArg(itemCall, "goal_id");
        if (goalId == null) {
            goalId = longArg(parentCall, "goal_id");
        }
        String goalKeyword = stringArg(itemCall, "goal_keyword");
        if (!StringUtils.hasText(goalKeyword)) {
            goalKeyword = stringArg(parentCall, "goal_keyword");
        }

        ConversationTaskDraft draft = new ConversationTaskDraft();
        draft.setSessionId(longArg(parentCall, "session_id"));
        draft.setTitle(title);
        draft.setDescription(stringArg(itemCall, "description"));
        draft.setPlanDate(planDate == null ? LocalDate.now() : planDate);
        draft.setStartTime(startTime);
        draft.setEndTime(endTime);
        draft.setPlannedMinutes(plannedMinutes);
        draft.setGoalId(goalId);
        draft.setGoalKeyword(goalKeyword);
        draft.setMissingSlots(resolveMissingSlots(startTime, endTime, plannedMinutes));
        draft.setSourceText(sourceText);
        return draft;
    }

    private TaskVO createTask(Long userId, ConversationTaskDraft draft) {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setGoalId(draft.getGoalId());
        request.setTitle(draft.getTitle());
        request.setDescription(draft.getDescription());
        request.setPlanDate(draft.getPlanDate());
        request.setStartTime(draft.getStartTime());
        request.setEndTime(draft.getEndTime());
        request.setPlannedMinutes(zero(draft.getPlannedMinutes()));
        request.setStatus(0);
        return taskService.createTask(userId, request);
    }

    private ToolResult createBatchTasks(Long userId, List<BatchScheduleParser.Entry> entries) {
        List<TaskVO> tasks = entries.stream()
                .map(entry -> {
                    TaskCreateRequest request = new TaskCreateRequest();
                    request.setTitle(entry.title());
                    request.setPlanDate(entry.planDate());
                    request.setStartTime(entry.startTime());
                    request.setEndTime(entry.endTime());
                    request.setPlannedMinutes(entry.plannedMinutes());
                    request.setStatus(0);
                    return taskService.createTask(userId, request);
                })
                .toList();
        draftService.completeActiveDraft(userId);
        return ToolResult.ok(batchCreatedReply(tasks), tasks);
    }

    private ToolResult enqueueNaturalTasks(
            Long userId,
            ToolCall call,
            String sourceText,
            List<String> titles
    ) {
        LocalDate planDate = dateArg(call, "plan_date");
        if (planDate == null) {
            planDate = parseRelativeDate(sourceText);
        }
        LocalDate resolvedDate = planDate == null ? LocalDate.now() : planDate;
        Long sessionId = longArg(call, "session_id");
        List<ConversationTaskDraft> drafts = titles.stream()
                .map(title -> {
                    ConversationTaskDraft draft = new ConversationTaskDraft();
                    draft.setTitle(title);
                    draft.setPlanDate(resolvedDate);
                    draft.setGoalId(longArg(call, "goal_id"));
                    draft.setGoalKeyword(stringArg(call, "goal_keyword"));
                    draft.setMissingSlots("start_time,duration");
                    draft.setSourceText(sourceText);
                    return draft;
                })
                .toList();
        List<ConversationTaskDraft> savedDrafts = draftService.enqueueDrafts(userId, sessionId, drafts);
        recordQueuedDraftTransitions(userId, sourceText, savedDrafts);
        return ToolResult.ok(queuedTasksReply(savedDrafts), savedDrafts);
    }

    private void recordQueuedDraftTransitions(
            Long userId,
            String sourceText,
            List<ConversationTaskDraft> savedDrafts
    ) {
        for (int index = 0; index < savedDrafts.size(); index++) {
            ConversationTaskDraft savedDraft = savedDrafts.get(index);
            TaskDraftFrame frame = initialFrame(savedDraft, sourceText);
            String question = index == 0 ? askForMissing(
                    savedDraft.getTitle(), savedDraft.getPlanDate(), savedDraft.getStartTime(),
                    savedDraft.getEndTime(), savedDraft.getPlannedMinutes()) : null;
            TaskDraftTransition transition = TaskDraftTransition.builder()
                    .rawText(sourceText)
                    .frame(frame)
                    .after(TaskDraftSnapshot.from(savedDraft))
                    .decision(index == 0 ? TaskDraftDecision.NEEDS_INPUT : TaskDraftDecision.QUEUED)
                    .clarificationQuestion(question)
                    .build();
            transitionLogService.recordTaskDraftTransition(
                    userId,
                    savedDraft,
                    index == 0 ? "TASK_DRAFT_CREATED" : "TASK_DRAFT_QUEUED",
                    transition
            );
        }
    }

    private String resolveMissingSlots(LocalTime startTime, LocalTime endTime, Integer plannedMinutes) {
        StringBuilder builder = new StringBuilder();
        if (startTime == null) {
            builder.append("start_time");
        }
        if (endTime == null && (plannedMinutes == null || plannedMinutes <= 0)) {
            if (!builder.isEmpty()) {
                builder.append(",");
            }
            builder.append("duration");
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    private String askForMissing(String title, LocalDate planDate, LocalTime startTime, LocalTime endTime, Integer plannedMinutes) {
        boolean missingStart = startTime == null;
        boolean missingDuration = endTime == null && (plannedMinutes == null || plannedMinutes <= 0);
        if (missingStart && missingDuration) {
            return "可以，我先把「" + title + "」记成任务草稿。\n我先按 " + planDate + " 理解。准备几点开始，预计多久？";
        }
        if (missingStart) {
            return "「" + title + "」准备放在几点开始？";
        }
        return "「" + title + "」预计安排多久？例如：60 分钟。";
    }

    private String taskCreatedReply(TaskVO task) {
        return "已创建任务：\n"
                + "任务：" + task.getTitle() + "\n"
                + "日期：" + task.getPlanDate() + "\n"
                + "时间：" + formatTime(task) + "\n"
                + "计划用时：" + zero(task.getPlannedMinutes()) + " 分钟\n"
                + "目标：" + dash(task.getGoalTitle());
    }

    private TaskDraftFrame initialFrame(ConversationTaskDraft draft, String rawText) {
        TaskDraftFrame frame = new TaskDraftFrame();
        frame.setRawText(rawText);
        frame.setPlanDate(draft.getPlanDate());
        frame.setStartTime(draft.getStartTime());
        frame.setEndTime(draft.getEndTime());
        frame.setPlannedMinutes(draft.getPlannedMinutes());
        frame.setStartExplicit(draft.getStartTime() != null);
        frame.setEndExplicit(draft.getEndTime() != null);
        frame.setDurationExplicit(draft.getPlannedMinutes() != null && draft.getPlannedMinutes() > 0);
        frame.getSlotSources().put("title", "intent-frame");
        if (draft.getPlanDate() != null) {
            frame.getSlotSources().put("plan_date", "intent-frame");
        }
        if (draft.getStartTime() != null) {
            frame.getSlotSources().put("start_time", "intent-frame");
        }
        if (draft.getEndTime() != null) {
            frame.getSlotSources().put("end_time", "intent-frame");
        }
        if (draft.getPlannedMinutes() != null) {
            frame.getSlotSources().put("planned_minutes", "intent-frame");
        }
        return frame;
    }

    private String batchCreatedReply(List<TaskVO> tasks) {
        String lines = tasks.stream()
                .map(task -> "- " + task.getPlanDate() + " " + formatTime(task) + " " + task.getTitle()
                        + " | " + zero(task.getPlannedMinutes()) + " 分钟")
                .collect(Collectors.joining("\n"));
        return "已创建 " + tasks.size() + " 个日程：\n" + lines;
    }

    private String queuedTasksReply(List<ConversationTaskDraft> drafts) {
        String lines = java.util.stream.IntStream.range(0, drafts.size())
                .mapToObj(index -> (index + 1) + ". " + drafts.get(index).getTitle())
                .collect(Collectors.joining("\n"));
        ConversationTaskDraft first = drafts.get(0);
        return "我识别到 " + drafts.size() + " 个任务：\n"
                + lines
                + "\n\n先安排「" + first.getTitle() + "」。"
                + missingSchedulePrompt(first);
    }

    private String missingSchedulePrompt(ConversationTaskDraft draft) {
        boolean missingStart = draft.getStartTime() == null;
        boolean missingDuration = draft.getEndTime() == null
                && (draft.getPlannedMinutes() == null || draft.getPlannedMinutes() <= 0);
        if (missingStart && missingDuration) {
            return "准备几点开始，预计多久？";
        }
        if (missingStart) {
            return "准备几点开始？";
        }
        return "预计安排多久？例如：60 分钟。";
    }

    private String structuredTasksReply(List<TaskVO> createdTasks, List<ConversationTaskDraft> pendingDrafts) {
        StringBuilder reply = new StringBuilder();
        if (!createdTasks.isEmpty()) {
            reply.append(batchCreatedReply(createdTasks)).append("\n\n");
        }
        reply.append(queuedTasksReply(pendingDrafts));
        return reply.toString();
    }

    private LocalDate parseRelativeDate(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        if (text.contains("后天")) {
            return LocalDate.now().plusDays(2);
        }
        if (text.contains("明天") || text.contains("明日")) {
            return LocalDate.now().plusDays(1);
        }
        if (text.contains("今天") || text.contains("今日") || text.contains("今晚")) {
            return LocalDate.now();
        }
        return null;
    }

    private String formatTime(TaskVO task) {
        if (task.getStartTime() == null && task.getEndTime() == null) {
            return "未设置";
        }
        String start = task.getStartTime() == null ? "未设置" : task.getStartTime().toString();
        String end = task.getEndTime() == null ? "未设置" : task.getEndTime().toString();
        return start + " - " + end;
    }
}
