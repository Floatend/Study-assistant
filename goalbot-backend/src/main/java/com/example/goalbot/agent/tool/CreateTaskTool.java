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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
        List<BatchScheduleParser.Entry> batchEntries = BatchScheduleParser.parse(stringArg(call, "source_text"));
        if (!batchEntries.isEmpty()) {
            return createBatchTasks(userId, batchEntries);
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

    private String formatTime(TaskVO task) {
        if (task.getStartTime() == null && task.getEndTime() == null) {
            return "未设置";
        }
        String start = task.getStartTime() == null ? "未设置" : task.getStartTime().toString();
        String end = task.getEndTime() == null ? "未设置" : task.getEndTime().toString();
        return start + " - " + end;
    }
}
