package com.example.goalbot.agent.tool;

import com.example.goalbot.agent.ToolCall;
import com.example.goalbot.agent.ToolNames;
import com.example.goalbot.agent.ToolResult;
import com.example.goalbot.dto.task.TaskCreateRequest;
import com.example.goalbot.entity.ConversationTaskDraft;
import com.example.goalbot.service.ConversationTaskDraftService;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class UpdateTaskDraftTool extends AbstractAgentTool {

    private static final String UPDATE_SOURCE_PREFIX = "UPDATE_TASK:";

    private final ConversationTaskDraftService draftService;
    private final TaskService taskService;

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
            draftService.cancelActiveDraft(userId);
            return ToolResult.ok("好，我先不创建这条任务。");
        }

        LocalDate planDate = firstNonNull(dateArg(call, "plan_date"), parseRelativeDate(text));
        LocalTime startTime = firstNonNull(
                ScheduleTextParser.parseTime(ScheduleTextParser.inheritMeridiem(draft.getSourceText(), text)),
                timeArg(call, "start_time"));
        LocalTime endTime = timeArg(call, "end_time");
        Integer plannedMinutes = firstNonNull(ScheduleTextParser.parseDuration(text), intArg(call, "planned_minutes"));
        boolean startChanged = startTime != null;
        boolean durationChanged = plannedMinutes != null && plannedMinutes > 0;
        boolean endChanged = endTime != null;

        if (planDate != null) {
            draft.setPlanDate(planDate);
        }
        if (startTime != null) {
            draft.setStartTime(startTime);
        }
        if (endTime != null) {
            draft.setEndTime(endTime);
        }
        if (plannedMinutes != null && plannedMinutes > 0) {
            draft.setPlannedMinutes(plannedMinutes);
        }
        if ((draft.getEndTime() == null || startChanged || durationChanged) && !endChanged
                && draft.getStartTime() != null && draft.getPlannedMinutes() != null && draft.getPlannedMinutes() > 0) {
            draft.setEndTime(draft.getStartTime().plusMinutes(draft.getPlannedMinutes()));
        }
        draft.setSourceText(combine(draft.getSourceText(), text));

        if (draft.getStartTime() == null || (draft.getEndTime() == null
                && (draft.getPlannedMinutes() == null || draft.getPlannedMinutes() <= 0))) {
            draftService.saveActiveDraft(userId, draft.getSessionId(), draft);
            return ToolResult.ok(askForMissing(draft), draft);
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
            draftService.completeActiveDraft(userId);
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
        draftService.completeActiveDraft(userId);
        return ToolResult.ok("已创建任务：\n"
                + "任务：" + task.getTitle() + "\n"
                + "日期：" + task.getPlanDate() + "\n"
                + "时间：" + task.getStartTime() + " - " + task.getEndTime() + "\n"
                + "计划用时：" + zero(task.getPlannedMinutes()) + " 分钟", task);
    }

    private String askForMissing(ConversationTaskDraft draft) {
        if (draft.getStartTime() == null) {
            return "「" + draft.getTitle() + "」准备几点开始？";
        }
        return "「" + draft.getTitle() + "」预计安排多久？例如：60 分钟。";
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

    private String combine(String previous, String current) {
        if (!StringUtils.hasText(previous)) {
            return current;
        }
        if (!StringUtils.hasText(current)) {
            return previous;
        }
        return previous + " " + current;
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }
}
