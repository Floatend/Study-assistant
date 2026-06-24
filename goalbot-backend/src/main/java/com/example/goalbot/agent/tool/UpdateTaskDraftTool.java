package com.example.goalbot.agent.tool;

import com.example.goalbot.agent.ToolCall;
import com.example.goalbot.agent.ToolNames;
import com.example.goalbot.agent.ToolResult;
import com.example.goalbot.agent.dialogue.TaskDraftDecision;
import com.example.goalbot.agent.dialogue.TaskDraftFrame;
import com.example.goalbot.agent.dialogue.TaskDraftReducer;
import com.example.goalbot.agent.dialogue.TaskDraftSnapshot;
import com.example.goalbot.agent.dialogue.TaskDraftTransition;
import com.example.goalbot.agent.dialogue.TaskDraftTurnParser;
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
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class UpdateTaskDraftTool extends AbstractAgentTool {

    private static final String UPDATE_SOURCE_PREFIX = "UPDATE_TASK:";

    private final ConversationTaskDraftService draftService;
    private final TaskService taskService;
    private final TaskDraftTurnParser turnParser;
    private final TaskDraftReducer reducer;
    private final ConversationTransitionLogService transitionLogService;

    @Override
    public String name() {
        return ToolNames.UPDATE_TASK_DRAFT;
    }

    @Override
    public ToolResult execute(Long userId, ToolCall call) {
        ConversationTaskDraft draft = draftService.getActiveDraft(userId).orElse(null);
        if (draft == null) {
            return ToolResult.failed("刚才没有正在补全的任务。你可以重新说一遍要安排什么。");
        }

        String text = stringArg(call, "text");
        if (!StringUtils.hasText(text)) {
            text = "";
        }
        if (isCancel(text)) {
            TaskDraftSnapshot before = TaskDraftSnapshot.from(draft);
            draftService.cancelActiveDraft(userId);
            draft.setStatus(2);
            TaskDraftFrame cancelFrame = new TaskDraftFrame();
            cancelFrame.setRawText(text);
            TaskDraftTransition transition = TaskDraftTransition.builder()
                    .rawText(text)
                    .before(before)
                    .frame(cancelFrame)
                    .after(TaskDraftSnapshot.from(draft))
                    .decision(TaskDraftDecision.CANCELLED)
                    .build();
            transitionLogService.recordTaskDraftTransition(userId, draft, "TASK_DRAFT_CANCELLED", transition);
            return ToolResult.ok("好，我先不创建这条任务。");
        }

        TaskDraftFrame frame = turnParser.parse(
                draft,
                text,
                LocalDateTime.now(),
                dateArg(call, "plan_date"),
                timeArg(call, "start_time"),
                timeArg(call, "end_time"),
                intArg(call, "planned_minutes")
        );
        TaskDraftTransition transition = reducer.reduce(draft, frame);
        draft = draftService.saveActiveDraft(userId, draft.getSessionId(), draft);
        transitionLogService.recordTaskDraftTransition(userId, draft, "TASK_DRAFT_REDUCED", transition);

        if (transition.getDecision() != TaskDraftDecision.READY) {
            return ToolResult.ok(transition.getClarificationQuestion(), draft);
        }

        TaskVO task;
        Long updateTaskId = updateTaskId(draft);
        if (updateTaskId != null) {
            com.example.goalbot.dto.task.TaskUpdateRequest request = new com.example.goalbot.dto.task.TaskUpdateRequest();
            request.setPlanDate(draft.getPlanDate() == null ? LocalDate.now() : draft.getPlanDate());
            request.setStartTime(draft.getStartTime());
            request.setEndTime(draft.getEndTime());
            request.setPlannedMinutes(draft.getPlannedMinutes() == null
                    ? minutesBetween(draft.getStartTime(), draft.getEndTime())
                    : draft.getPlannedMinutes());
            task = taskService.updateTask(userId, updateTaskId, request);
            completeDraftWithLog(userId, draft, text);
            return ToolResult.ok("已调整任务时间：\n"
                    + "任务：" + task.getTitle() + "\n"
                    + "日期：" + task.getPlanDate() + "\n"
                    + "时间：" + task.getStartTime() + " - " + task.getEndTime() + "\n"
                    + "计划用时：" + zero(task.getPlannedMinutes()) + " 分钟", task);
        }

        TaskCreateRequest request = new TaskCreateRequest();
        request.setGoalId(draft.getGoalId());
        request.setTitle(draft.getTitle());
        request.setDescription(draft.getDescription());
        request.setPlanDate(draft.getPlanDate() == null ? LocalDate.now() : draft.getPlanDate());
        request.setStartTime(draft.getStartTime());
        request.setEndTime(draft.getEndTime());
        request.setPlannedMinutes(draft.getPlannedMinutes() == null
                ? minutesBetween(draft.getStartTime(), draft.getEndTime())
                : draft.getPlannedMinutes());
        request.setStatus(0);

        task = taskService.createTask(userId, request);
        completeDraftWithLog(userId, draft, text);
        return ToolResult.ok("已创建任务：\n"
                + "任务：" + task.getTitle() + "\n"
                + "日期：" + task.getPlanDate() + "\n"
                + "时间：" + task.getStartTime() + " - " + task.getEndTime() + "\n"
                + "计划用时：" + zero(task.getPlannedMinutes()) + " 分钟", task);
    }

    private void completeDraftWithLog(Long userId, ConversationTaskDraft draft, String rawText) {
        TaskDraftSnapshot before = TaskDraftSnapshot.from(draft);
        draftService.completeActiveDraft(userId);
        draft.setStatus(1);
        TaskDraftFrame frame = new TaskDraftFrame();
        frame.setRawText(rawText);
        TaskDraftTransition transition = TaskDraftTransition.builder()
                .rawText(rawText)
                .before(before)
                .frame(frame)
                .after(TaskDraftSnapshot.from(draft))
                .decision(TaskDraftDecision.COMPLETED)
                .build();
        transitionLogService.recordTaskDraftTransition(userId, draft, "TASK_DRAFT_COMPLETED", transition);
    }

    private int minutesBetween(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return 0;
        }
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < 0) {
            minutes += Duration.ofDays(1).toMinutes();
        }
        return (int) minutes;
    }

    private boolean isCancel(String text) {
        return StringUtils.hasText(text)
                && (text.contains("取消") || text.contains("算了") || text.contains("不用了") || text.contains("先不"));
    }

    private Long updateTaskId(ConversationTaskDraft draft) {
        String source = draft.getSourceText();
        if (!StringUtils.hasText(source)) {
            return null;
        }
        int index = source.indexOf(UPDATE_SOURCE_PREFIX);
        if (index < 0) {
            return null;
        }
        int start = index + UPDATE_SOURCE_PREFIX.length();
        int end = start;
        while (end < source.length() && Character.isDigit(source.charAt(end))) {
            end++;
        }
        if (end == start) {
            return null;
        }
        try {
            return Long.parseLong(source.substring(start, end));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

}
