package com.example.goalbot.agent.tool;

import com.example.goalbot.agent.ToolCall;
import com.example.goalbot.agent.ToolNames;
import com.example.goalbot.agent.ToolResult;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CancelImportedScheduleTool extends AbstractAgentTool {

    private final TaskService taskService;

    @Override
    public String name() {
        return ToolNames.CANCEL_IMPORTED_SCHEDULE;
    }

    @Override
    public ToolResult execute(Long userId, ToolCall call) {
        LocalDate startDate = dateArg(call, "range_start_date");
        LocalDate endDate = dateArg(call, "range_end_date");
        String keyword = stringArg(call, "task_keyword");

        if (startDate == null || endDate == null) {
            return ToolResult.ok("我听懂了你想停掉某一周的导入课程，但还不知道具体日期。"
                    + "请说“下周的课不上了”，或“6月22日到6月28日的课不上了”。");
        }

        List<TaskVO> deletedTasks = taskService.deletePendingIcsTasks(userId, startDate, endDate, keyword);
        String scope = startDate + " 至 " + endDate;
        String courseLabel = StringUtils.hasText(keyword) ? "与“" + keyword + "”匹配的" : "";
        if (deletedTasks.isEmpty()) {
            return ToolResult.ok(scope + " 没有找到" + courseLabel + "待完成 ICS 导入课程。"
                    + "手工创建的任务和已完成记录都没有改动。");
        }

        String taskLines = deletedTasks.stream()
                .map(task -> "- " + task.getPlanDate() + " "
                        + (task.getStartTime() == null ? "" : task.getStartTime().toString().substring(0, 5) + " ")
                        + task.getTitle())
                .collect(Collectors.joining("\n"));
        return ToolResult.ok("已移除 " + scope + " 的 " + deletedTasks.size() + " 节"
                        + courseLabel + "导入课程：\n" + taskLines
                        + "\n仅删除了待完成的 ICS 日程，其他任务未受影响。",
                deletedTasks);
    }
}
