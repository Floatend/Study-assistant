package com.example.goalbot.agent.tool;

import com.example.goalbot.agent.ToolCall;
import com.example.goalbot.agent.ToolNames;
import com.example.goalbot.agent.ToolResult;
import com.example.goalbot.dto.checkin.CheckinCreateRequest;
import com.example.goalbot.service.CheckinService;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.CheckinVO;
import com.example.goalbot.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CheckinTaskTool extends AbstractAgentTool {

    private final TaskService taskService;
    private final CheckinService checkinService;

    @Override
    public String name() {
        return ToolNames.CHECKIN_TASK;
    }

    @Override
    public ToolResult execute(Long userId, ToolCall call) {
        String keyword = stringArg(call, "task_keyword");
        if (!StringUtils.hasText(keyword)) {
            keyword = stringArg(call, "task_title");
        }
        Integer minutes = intArg(call, "actual_minutes");
        if (minutes == null) {
            minutes = intArg(call, "minutes");
        }

        if (!StringUtils.hasText(keyword) && (minutes == null || minutes <= 0)) {
            return ToolResult.failed("我理解你想打卡，但还缺任务名和用时。比如：英语听力学了 30 分钟。");
        }
        if (!StringUtils.hasText(keyword)) {
            return ToolResult.failed("我理解你想打卡，但还缺任务名。");
        }
        if (minutes == null || minutes <= 0) {
            return ToolResult.failed("我理解你想给「" + keyword + "」打卡，但还缺实际用时。");
        }

        List<TaskVO> matches = findTodayTaskMatches(userId, keyword);
        if (matches.isEmpty()) {
            return ToolResult.failed("没有找到包含「" + keyword + "」的今日任务。请先创建任务，或换一个更准确的任务名。");
        }
        if (matches.size() > 1) {
            String options = matches.stream()
                    .map(task -> "- " + task.getTitle() + " | " + zero(task.getPlannedMinutes()) + " 分钟")
                    .collect(Collectors.joining("\n", "找到多个相关任务，请说得更具体一点：\n", ""));
            return ToolResult.failed(options);
        }

        TaskVO task = matches.get(0);
        CheckinCreateRequest request = new CheckinCreateRequest();
        request.setTaskId(task.getId());
        request.setActualMinutes(minutes);
        request.setContent("飞书自然语言打卡：" + stringArg(call, "source_text"));

        CheckinVO checkin = checkinService.createCheckin(userId, request);
        return ToolResult.ok("已记录打卡：\n"
                + "任务：" + task.getTitle() + "\n"
                + "实际用时：" + checkin.getActualMinutes() + " 分钟\n"
                + "任务状态已更新。", checkin);
    }

    private List<TaskVO> findTodayTaskMatches(Long userId, String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<TaskVO> todayTasks = taskService.listTodayTasks(userId);
        List<TaskVO> exact = todayTasks.stream()
                .filter(task -> task.getTitle() != null && task.getTitle().equals(normalizedKeyword))
                .toList();
        if (!exact.isEmpty()) {
            return exact;
        }
        return todayTasks.stream()
                .filter(task -> task.getTitle() != null
                        && (task.getTitle().contains(normalizedKeyword) || normalizedKeyword.contains(task.getTitle())))
                .toList();
    }
}
