package com.example.goalbot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.goalbot.entity.Notification;
import com.example.goalbot.mapper.NotificationMapper;
import com.example.goalbot.service.FeishuService;
import com.example.goalbot.service.NotificationService;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    private final FeishuService feishuService;
    private final TaskService taskService;

    @Override
    public Notification sendDailyTaskReminder(Long userId) {
        return sendDailyTaskReminder(userId, null);
    }

    @Override
    public Notification sendDailyTaskReminder(Long userId, String feishuChatId) {
        List<TaskVO> tasks = taskService.listTodayTasks(userId);
        String title = "今日任务提醒";
        String content = buildDailyTaskMessage(tasks);
        return saveNotification(userId, title, content, feishuService.sendRichTextToChat(feishuChatId, title, content));
    }

    @Override
    public Notification sendDailyReviewReminder(Long userId) {
        return sendDailyReviewReminder(userId, null);
    }

    @Override
    public Notification sendDailyReviewReminder(Long userId, String feishuChatId) {
        String title = "每日复盘提醒";
        String content = """
                今天快结束了，可以做一次简短复盘。
                建议记录：
                1. 今天完成了什么
                2. 哪个任务最卡
                3. 实际投入了多少时间
                4. 明天最重要的一件事

                你也可以在系统的复盘页手动生成 AI 每日复盘。
                """;
        return saveNotification(userId, title, content, feishuService.sendRichTextToChat(feishuChatId, title, content));
    }

    @Override
    public Notification sendWeeklyReviewReminder(Long userId) {
        return sendWeeklyReviewReminder(userId, null);
    }

    @Override
    public Notification sendWeeklyReviewReminder(Long userId, String feishuChatId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = today.with(DayOfWeek.SUNDAY);
        List<TaskVO> tasks = taskService.listCalendarTasks(userId, weekStart, weekEnd);
        String title = "每周周报提醒";
        String content = buildWeeklyReviewMessage(weekStart, weekEnd, tasks);
        return saveNotification(userId, title + " " + today, content, feishuService.sendRichTextToChat(feishuChatId, title, content));
    }

    @Override
    public Notification sendPeriodicPlanningNudge(Long userId, String feishuChatId) {
        List<TaskVO> tasks = taskService.listTodayTasks(userId);
        String title = "主动规划提醒";
        String content = buildPeriodicPlanningMessage(tasks);
        return saveNotification(userId, title, content, feishuService.sendRichTextToChat(feishuChatId, title, content));
    }

    @Override
    public Notification sendCustomMessage(Long userId, String title, String content) {
        return sendCustomMessage(userId, title, content, null);
    }

    @Override
    public Notification sendCustomMessage(Long userId, String title, String content, String feishuChatId) {
        return saveNotification(userId, title, content, feishuService.sendRichTextToChat(feishuChatId, title, content));
    }

    private String buildDailyTaskMessage(List<TaskVO> tasks) {
        if (tasks.isEmpty()) {
            return """
                    早上好。今天还没看到你的安排。
                    今天有什么要干的，直接回复我就行。
                    比如：今天要写离散作业，下午 3 点，预计 60 分钟。
                    如果你一时说不完整，我会继续问你要放在几点、预计多久。
                    """;
        }

        int totalMinutes = sumPlannedMinutes(tasks);
        long completed = countCompleted(tasks);
        String taskLines = tasks.stream()
                .map(this::formatTaskLine)
                .collect(Collectors.joining("\n"));

        return """
                早上好，今日任务如下。
                今天共 %d 个任务，预计 %d 分钟，已完成 %d 个。
                %s

                要调整计划也可以直接回我，比如：把高数复习放到下午 4 点，预计 60 分钟。
                """.formatted(tasks.size(), totalMinutes, completed, taskLines);
    }

    private String buildPeriodicPlanningMessage(List<TaskVO> tasks) {
        if (tasks.isEmpty()) {
            return """
                    我来轻轻推一下今天的节奏。
                    今天还没有任务。你可以直接告诉我接下来要做什么、几点开始、预计多久。
                    例如：晚上七点复习 Web 作业，预计两个小时。
                    """;
        }

        int totalMinutes = sumPlannedMinutes(tasks);
        long completed = countCompleted(tasks);
        long pending = tasks.size() - completed;
        TaskVO nextTask = tasks.stream()
                .filter(task -> !Objects.equals(task.getStatus(), 2))
                .findFirst()
                .orElse(null);

        String nextTaskLine = nextTask == null
                ? "今天的任务都已经完成了，可以考虑做一次简短复盘。"
                : "下一步建议先处理：\n" + formatTaskLine(nextTask);

        return """
                我来轻轻推一下今天的节奏。
                今日任务：%d 个；已完成：%d 个；待完成：%d 个；计划投入：%d 分钟。
                %s

                如果计划变了，直接回我“把某个任务改到晚上七点”或“删除某个任务”。
                """.formatted(tasks.size(), completed, pending, totalMinutes, nextTaskLine);
    }

    private String buildWeeklyReviewMessage(LocalDate weekStart, LocalDate weekEnd, List<TaskVO> tasks) {
        int total = tasks.size();
        long completed = countCompleted(tasks);
        int plannedMinutes = sumPlannedMinutes(tasks);
        long completionRate = total == 0 ? 0 : Math.round(completed * 100.0 / total);

        return """
                本周已经到尾声了，可以生成一次周报。
                周期：%s 至 %s
                本周任务：%d 个
                已完成：%d 个
                完成率：%d%%
                计划投入：%d 分钟

                建议复盘：
                1. 本周最有价值的进展
                2. 没完成的任务卡在哪里
                3. 下周要保留和减少的安排
                4. 是否需要调整目标优先级
                """.formatted(weekStart, weekEnd, total, completed, completionRate, plannedMinutes);
    }

    private int sumPlannedMinutes(List<TaskVO> tasks) {
        return tasks.stream()
                .mapToInt(task -> task.getPlannedMinutes() == null ? 0 : task.getPlannedMinutes())
                .sum();
    }

    private long countCompleted(List<TaskVO> tasks) {
        return tasks.stream()
                .filter(task -> Objects.equals(task.getStatus(), 2))
                .count();
    }

    private String formatTaskLine(TaskVO task) {
        String time = task.getStartTime() == null ? "未排时间" : task.getStartTime().toString().substring(0, 5);
        if (task.getEndTime() != null) {
            time += "-" + task.getEndTime().toString().substring(0, 5);
        }
        String goal = task.getGoalTitle() == null ? "无绑定目标" : task.getGoalTitle();
        String status = Objects.equals(task.getStatus(), 2) ? "已完成" : "待完成";
        int minutes = task.getPlannedMinutes() == null ? 0 : task.getPlannedMinutes();
        return "- " + task.getTitle() + " | " + time + " | " + minutes + " 分钟 | " + status + " | " + goal;
    }

    private Notification saveNotification(Long userId, String title, String content, boolean sent) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setNotifyTime(LocalDateTime.now());
        notification.setChannel(3);
        notification.setStatus(sent ? 1 : 2);
        save(notification);
        return notification;
    }
}
