package com.example.goalbot.agent.tool;

import com.example.goalbot.agent.ToolCall;
import com.example.goalbot.agent.ToolNames;
import com.example.goalbot.agent.ToolResult;
import com.example.goalbot.dto.task.TaskUpdateRequest;
import com.example.goalbot.entity.ConversationTaskDraft;
import com.example.goalbot.service.ConversationTaskDraftService;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UpdateTaskScheduleTool extends AbstractAgentTool {

    private static final String UPDATE_SOURCE_PREFIX = "UPDATE_TASK:";

    private final TaskService taskService;
    private final ConversationTaskDraftService draftService;

    @Override
    public String name() {
        return ToolNames.UPDATE_TASK_SCHEDULE;
    }

    @Override
    public ToolResult execute(Long userId, ToolCall call) {
        String text = stringArg(call, "text");
        if (!StringUtils.hasText(text)) {
            text = "";
        }

        LocalDate planDate = firstNonNull(dateArg(call, "plan_date"), parseRelativeDate(text));
        if (planDate == null) {
            planDate = LocalDate.now();
        }

        List<TaskVO> tasks = taskService.listActiveTasksByDate(userId, planDate);
        if (tasks.isEmpty()) {
            return ToolResult.failed(dateLabel(planDate) + "没有可调整的待处理任务。");
        }

        TaskVO target = resolveTargetTask(text, tasks);
        if (target == null) {
            return ToolResult.failed("我没找到要调整哪一个任务。可以这样说：把复习web作业改到下午 7:10，预计两个小时。");
        }

        LocalTime startTime = firstNonNull(ScheduleTextParser.parseTime(text), timeArg(call, "start_time"));
        LocalTime endTime = timeArg(call, "end_time");
        Integer plannedMinutes = firstNonNull(ScheduleTextParser.parseDuration(text), intArg(call, "planned_minutes"));
        if (endTime == null && startTime != null && plannedMinutes != null && plannedMinutes > 0) {
            endTime = startTime.plusMinutes(plannedMinutes);
        }

        if (startTime == null || (endTime == null && (plannedMinutes == null || plannedMinutes <= 0))) {
            ConversationTaskDraft draft = new ConversationTaskDraft();
            draft.setSessionId(longArg(call, "session_id"));
            draft.setTitle(target.getTitle());
            draft.setDescription(target.getDescription());
            draft.setPlanDate(planDate);
            draft.setStartTime(startTime);
            draft.setEndTime(endTime);
            draft.setPlannedMinutes(plannedMinutes);
            draft.setGoalId(target.getGoalId());
            draft.setGoalKeyword(target.getGoalTitle());
            draft.setMissingSlots(resolveMissingSlots(startTime, endTime, plannedMinutes));
            draft.setSourceText(UPDATE_SOURCE_PREFIX + target.getId() + " " + text);
            draftService.saveActiveDraft(userId, draft.getSessionId(), draft);
            return ToolResult.ok(askForMissing(draft), draft);
        }

        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setPlanDate(planDate);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setPlannedMinutes(plannedMinutes == null ? minutesBetween(startTime, endTime) : plannedMinutes);
        TaskVO updated = taskService.updateTask(userId, target.getId(), request);
        return ToolResult.ok("已调整任务时间：\n"
                + "任务：" + updated.getTitle() + "\n"
                + "日期：" + updated.getPlanDate() + "\n"
                + "时间：" + updated.getStartTime() + " - " + updated.getEndTime() + "\n"
                + "计划用时：" + zero(updated.getPlannedMinutes()) + " 分钟", updated);
    }

    private TaskVO resolveTargetTask(String text, List<TaskVO> tasks) {
        if (StringUtils.hasText(text)) {
            TaskVO matched = tasks.stream()
                    .filter(task -> StringUtils.hasText(task.getTitle()))
                    .filter(task -> text.contains(task.getTitle()) || task.getTitle().contains(cleanTaskKeyword(text)))
                    .max(Comparator.comparingInt(task -> task.getTitle().length()))
                    .orElse(null);
            if (matched != null) {
                return matched;
            }
        }
        return tasks.stream()
                .max(Comparator
                        .comparing(TaskVO::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(TaskVO::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
    }

    private String cleanTaskKeyword(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replaceAll("调整|修改|更改|改一下|改到|改成|任务|时间|日程|安排|把|帮我|给我|今天|今日|明天|今晚|晚上|下午|上午|预计|开始", "")
                .replaceAll("[，。,.！!？?、:：\\s]", "")
                .trim();
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

    private String askForMissing(ConversationTaskDraft draft) {
        if (draft.getStartTime() == null) {
            return "要调整「" + draft.getTitle() + "」的话，准备改到几点开始？";
        }
        return "「" + draft.getTitle() + "」新的预计用时多久？例如：两个小时。";
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

    private String dateLabel(LocalDate date) {
        if (LocalDate.now().equals(date)) {
            return "今天";
        }
        if (LocalDate.now().plusDays(1).equals(date)) {
            return "明天";
        }
        return date.toString();
    }

    private int minutesBetween(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return 0;
        }
        int minutes = (int) java.time.Duration.between(start, end).toMinutes();
        return minutes < 0 ? minutes + 24 * 60 : minutes;
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }
}
