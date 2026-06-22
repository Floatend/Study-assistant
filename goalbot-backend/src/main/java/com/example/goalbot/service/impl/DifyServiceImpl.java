package com.example.goalbot.service.impl;

import com.example.goalbot.common.AdviceSourceHasher;
import com.example.goalbot.entity.ConversationMessage;
import com.example.goalbot.integration.dify.DifyClient;
import com.example.goalbot.integration.dify.DifyException;
import com.example.goalbot.service.CheckinService;
import com.example.goalbot.service.ConversationStateService;
import com.example.goalbot.service.DifyService;
import com.example.goalbot.service.GoalService;
import com.example.goalbot.service.ReviewService;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.CheckinStatsVO;
import com.example.goalbot.vo.CheckinVO;
import com.example.goalbot.vo.GoalVO;
import com.example.goalbot.vo.ReviewVO;
import com.example.goalbot.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DifyServiceImpl implements DifyService {

    private final DifyClient difyClient;
    private final GoalService goalService;
    private final TaskService taskService;
    private final CheckinService checkinService;
    private final ReviewService reviewService;
    private final ConversationStateService conversationStateService;

    @Override
    public ReviewVO generateAdvice(Long userId) {
        return generateAdvice(userId, 2);
    }

    @Override
    public ReviewVO generateAdvice(Long userId, Integer days) {
        int adviceDays = normalizeAdviceDays(days);
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(adviceDays - 1L);
        List<GoalVO> goals = goalService.listGoals(userId, null, null, null);
        List<TaskVO> scheduleTasks = taskService.listActiveCalendarTasks(userId, today, endDate);
        List<CheckinVO> recentCheckins = recentSevenDayCheckins(userId);

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("today", today.toString());
        inputs.put("advice_days", adviceDays);
        inputs.put("advice_start_date", today.toString());
        inputs.put("advice_end_date", endDate.toString());
        inputs.put("goals", goals.stream().map(this::goalInput).toList());
        inputs.put("schedule_tasks", scheduleTasks.stream().map(this::taskInput).toList());
        inputs.put("recent_checkins", recentCheckins.stream().map(this::checkinInput).toList());

        String prompt = """
                You are GoalBot, my personal goal management assistant.
                Please answer in Simplified Chinese.
                Use only the data provided by the backend. Do not invent goals, tasks, or checkins.

                Task:
                Generate actionable scheduling advice for the selected date range.
                Keep it concise and executable.
                Focus on the top 1-3 things to handle first.
                Separate what should be done today from what should be prepared for the next days when the range is longer than one day.
                Explain briefly why this ordering is reasonable.

                Date range:
                %s to %s (%d day%s)

                Current goals:
                %s

                Scheduled tasks in range:
                %s

                Recent checkins:
                %s
                """.formatted(today, endDate, adviceDays, adviceDays > 1 ? "s" : "",
                formatGoals(goals), formatTasks(scheduleTasks), formatCheckins(recentCheckins));

        String advice = callDifyOrFallback(prompt, inputs, userId, "schedule advice " + today + " to " + endDate);
        String sourceHash = AdviceSourceHasher.adviceSourceHash(scheduleTasks, goals, today, endDate);
        String summary = isDifyFallback(advice)
                ? "AI Schedule Advice"
                : AdviceSourceHasher.summaryWithHash("AI Schedule Advice " + today + " to " + endDate, sourceHash);
        return reviewService.saveOrUpdateReview(userId, today, 4,
                summary, advice);
    }

    @Override
    public ReviewVO generateDailyReview(Long userId, LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        List<TaskVO> tasks = taskService.listTasks(userId, targetDate, null, null);
        CheckinStatsVO stats = checkinService.getStats(userId, targetDate, targetDate);

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("date", targetDate.toString());
        inputs.put("tasks", tasks.stream().map(this::taskInput).toList());
        inputs.put("stats", statsInput(stats));

        String prompt = """
                You are GoalBot, my personal review assistant.
                Please answer in Simplified Chinese.
                Use only the data provided by the backend. Do not invent completed tasks or time records.

                Task:
                Generate a daily review for the date below.
                Include: completion summary, time investment, what went well, the main blocker, and one improvement for tomorrow.

                Date: %s

                Tasks:
                %s

                Stats:
                %s
                """.formatted(targetDate, formatTasks(tasks), formatStats(stats));

        String advice = callDifyOrFallback(prompt, inputs, userId, "daily review");
        return reviewService.saveOrUpdateReview(userId, targetDate, 1, "Daily Review", advice);
    }

    @Override
    public ReviewVO generateWeeklyReview(Long userId, LocalDate weekStart, LocalDate weekEnd) {
        LocalDate start = weekStart == null ? LocalDate.now().with(DayOfWeek.MONDAY) : weekStart;
        LocalDate end = weekEnd == null ? start.plusDays(6) : weekEnd;
        List<GoalVO> goals = goalService.listGoals(userId, null, null, null);
        List<TaskVO> tasks = taskService.listCalendarTasks(userId, start, end);
        CheckinStatsVO stats = checkinService.getStats(userId, start, end);

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("week_start", start.toString());
        inputs.put("week_end", end.toString());
        inputs.put("goals", goals.stream().map(this::goalInput).toList());
        inputs.put("tasks", tasks.stream().map(this::taskInput).toList());
        inputs.put("stats", statsInput(stats));

        String prompt = """
                You are GoalBot, my personal weekly review assistant.
                Please answer in Simplified Chinese.
                Use only the data provided by the backend. Do not invent tasks or time records.

                Task:
                Generate a weekly review.
                Include: weekly overview, key completed work, risks or delays, and next week suggestions.

                Week: %s to %s

                Goals:
                %s

                Tasks:
                %s

                Stats:
                %s
                """.formatted(start, end, formatGoals(goals), formatTasks(tasks), formatStats(stats));

        String advice = callDifyOrFallback(prompt, inputs, userId, "weekly review");
        return reviewService.saveOrUpdateReview(userId, end, 2, "Weekly Review", advice);
    }

    @Override
    public String generateArticleBriefing(Long userId, String sourceName, String articleTitle, String articleUrl, String articleContent) {
        String normalizedSourceName = nullToDash(sourceName);
        String normalizedTitle = nullToDash(articleTitle);
        String normalizedUrl = nullToDash(articleUrl);
        String normalizedContent = limitText(articleContent, 9000);

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("source_name", normalizedSourceName);
        inputs.put("article_title", normalizedTitle);
        inputs.put("article_url", normalizedUrl);
        inputs.put("article_content", normalizedContent);

        String prompt = """
                You are GoalBot's daily AI briefing assistant.
                Please answer in Simplified Chinese.
                The user wants a concise daily AI consultation based on the article below.
                Use only the provided article content. Do not invent claims, facts, quotes, or links.

                Output format for Feishu:
                1. 先用一两句话说明这篇内容在讲什么。
                2. 提炼 3 个核心观点。
                3. 结合个人学习、项目开发和长期规划，给出 2-3 条可执行建议。
                4. 最后给一个“今天可以马上做的小动作”。

                Source: %s
                Article title: %s
                Article URL: %s

                Article content:
                %s
                """.formatted(normalizedSourceName, normalizedTitle, normalizedUrl, normalizedContent);

        return callDifyOrFallback(prompt, inputs, userId, "ai briefing");
    }

    @Override
    public String chatForFeishu(Long userId, String message) {
        LocalDate today = LocalDate.now();
        List<GoalVO> goals = goalService.listGoals(userId, null, null, null);
        List<TaskVO> todayTasks = taskService.listTodayTasks(userId);
        List<TaskVO> upcomingTasks = taskService.listActiveCalendarTasks(userId, today, today.plusDays(7));

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("today", today.toString());
        inputs.put("message", message);
        inputs.put("goals", goals.stream().map(this::goalInput).toList());
        inputs.put("today_tasks", todayTasks.stream().map(this::taskInput).toList());
        inputs.put("upcoming_tasks", upcomingTasks.stream().map(this::taskInput).toList());
        inputs.put("recent_messages", conversationStateService.listRecentMessages(userId, "FEISHU", 8)
                .stream()
                .map(this::conversationMessageInput)
                .toList());

        String prompt = """
                You are GoalBot's Feishu chat assistant.
                Please answer in Simplified Chinese, warmly and briefly.

                Your role:
                - You can chat naturally with the user.
                - You help the user turn daily conversation into a concrete schedule.
                - If the user mentions something they need to do but misses date, time, or duration, ask one focused follow-up question.
                - Do not claim that you created, edited, completed, or deleted database records unless the backend explicitly did it.
                - If the user wants to create a task, guide them to provide title, date, start time, and estimated duration.
                - Use recent messages to understand short follow-ups. For example, if the user just said they will go eat something nice tonight, "7:30 departure" means that same plan.
                - Keep replies suitable for Feishu messages: concise, no long essays.

                Today: %s

                User message:
                %s

                Current goals:
                %s

                Today's tasks:
                %s

                Upcoming tasks:
                %s

                Recent conversation:
                %s
                """.formatted(today, message, formatGoals(goals), formatTasks(todayTasks), formatTasks(upcomingTasks),
                formatConversationMessages(conversationStateService.listRecentMessages(userId, "FEISHU", 8)));

        return callDifyOrFallback(prompt, inputs, userId, "feishu chat");
    }

    private Map<String, Object> conversationMessageInput(ConversationMessage message) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("direction", message.getDirection());
        input.put("content", message.getContent());
        input.put("intent", message.getIntent());
        input.put("created_at", message.getCreatedAt() == null ? null : message.getCreatedAt().toString());
        return input;
    }

    private String formatConversationMessages(List<ConversationMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "No recent conversation";
        }
        return messages.stream()
                .map(message -> "- " + message.getDirection() + ": " + message.getContent())
                .collect(Collectors.joining("\n"));
    }

    private String callDifyOrFallback(String prompt, Map<String, Object> inputs, Long userId, String scene) {
        if (!difyClient.isConfigured()) {
            if ("ai briefing".equals(scene)) {
                return """
                        Dify 还没有配置好，所以今天只能先发送原文来源，暂时不能生成 AI 资讯咨询。

                        需要配置：DIFY_ENABLED=true、DIFY_API_URL、DIFY_API_KEY。
                        """;
            }
            if ("feishu chat".equals(scene)) {
                return """
                        我现在可以处理任务、打卡、目标进度和建议类指令。
                        如果你想让我帮你安排日程，可以直接说：今天要写离散作业，下午 3 点，预计 60 分钟。

                        自由聊天需要先配置 Dify：DIFY_ENABLED=true、DIFY_API_URL、DIFY_API_KEY。
                        """;
            }
            return """
                    Dify is disabled or not configured.

                    Configure these backend environment variables and restart the backend:
                    DIFY_ENABLED=true
                    DIFY_API_URL=https://api.dify.ai/v1
                    DIFY_API_KEY=your Dify App API Key

                    GoalBot has already prepared the MySQL-backed goal, task, and checkin data for %s.
                    """.formatted(scene);
        }

        try {
            return sanitizeAnswer(difyClient.chat(prompt, inputs, String.valueOf(userId)));
        } catch (DifyException ex) {
            if ("ai briefing".equals(scene)) {
                return """
                        Dify 暂时不可用，今天的 AI 资讯咨询生成失败。

                        可以稍后重试，或检查 DIFY_API_URL、DIFY_API_KEY、网络连接和 Dify 应用状态。
                        错误：%s
                        """.formatted(ex.getMessage());
            }
            if ("feishu chat".equals(scene)) {
                return """
                        我这会儿连不上 Dify，所以自由聊天先降级一下。
                        但任务系统还能用：你可以直接说“明天下午 3 点安排高数复习 60 分钟”，我会帮你建进日程。
                        """;
            }
            return """
                    Dify is temporarily unavailable. GoalBot saved this generation record.

                    Scene: %s
                    Reason: %s

                    Goal, task, checkin, and reminder features are not affected. Retry later, or check DIFY_API_URL, DIFY_API_KEY, network access, and the Dify app status.
                    """.formatted(scene, ex.getMessage());
        }
    }

    private String sanitizeAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        String cleaned = answer.replaceAll("(?is)<think>.*?</think>", "").trim();
        return cleaned.isBlank() ? answer.trim() : cleaned;
    }

    private boolean isDifyFallback(String advice) {
        return advice != null && advice.startsWith("Dify is ");
    }

    private int normalizeAdviceDays(Integer days) {
        if (days == null) {
            return 2;
        }
        return Math.max(1, Math.min(3, days));
    }

    private String limitText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "No article content";
        }
        String cleaned = value.trim();
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, maxLength) + "\n\n[content truncated]";
    }

    private List<CheckinVO> recentSevenDayCheckins(Long userId) {
        LocalDateTime cutoff = LocalDate.now().minusDays(6).atStartOfDay();
        return checkinService.listRecent(userId, 50)
                .stream()
                .filter(checkin -> checkin.getCreatedAt() != null && !checkin.getCreatedAt().isBefore(cutoff))
                .toList();
    }

    private Map<String, Object> goalInput(GoalVO goal) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("id", goal.getId());
        input.put("title", goal.getTitle());
        input.put("description", goal.getDescription());
        input.put("start_date", goal.getStartDate() == null ? null : goal.getStartDate().toString());
        input.put("end_date", goal.getEndDate() == null ? null : goal.getEndDate().toString());
        input.put("priority", goal.getPriority());
        input.put("status", goal.getStatus());
        input.put("total_task_count", goal.getTotalTaskCount());
        input.put("completed_task_count", goal.getCompletedTaskCount());
        return input;
    }

    private Map<String, Object> taskInput(TaskVO task) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("id", task.getId());
        input.put("goal_id", task.getGoalId());
        input.put("goal_title", task.getGoalTitle());
        input.put("title", task.getTitle());
        input.put("description", task.getDescription());
        input.put("plan_date", task.getPlanDate() == null ? null : task.getPlanDate().toString());
        input.put("start_time", task.getStartTime() == null ? null : task.getStartTime().toString());
        input.put("end_time", task.getEndTime() == null ? null : task.getEndTime().toString());
        input.put("planned_minutes", task.getPlannedMinutes());
        input.put("status", task.getStatus());
        return input;
    }

    private Map<String, Object> checkinInput(CheckinVO checkin) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("id", checkin.getId());
        input.put("task_id", checkin.getTaskId());
        input.put("task_title", checkin.getTaskTitle());
        input.put("actual_minutes", checkin.getActualMinutes());
        input.put("content", checkin.getContent());
        input.put("mood", checkin.getMood());
        input.put("difficulty", checkin.getDifficulty());
        input.put("created_at", checkin.getCreatedAt() == null ? null : checkin.getCreatedAt().toString());
        return input;
    }

    private Map<String, Object> statsInput(CheckinStatsVO stats) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("total_minutes", stats.getTotalMinutes());
        input.put("checkin_count", stats.getCheckinCount());
        input.put("completed_task_count", stats.getCompletedTaskCount());
        input.put("average_mood", stats.getAverageMood());
        input.put("average_difficulty", stats.getAverageDifficulty());
        return input;
    }

    private String formatGoals(List<GoalVO> goals) {
        if (goals.isEmpty()) {
            return "No goals";
        }
        return goals.stream()
                .map(goal -> "- " + goal.getTitle()
                        + " | status " + goal.getStatus()
                        + " | priority " + goal.getPriority())
                .collect(Collectors.joining("\n"));
    }

    private String formatTasks(List<TaskVO> tasks) {
        if (tasks.isEmpty()) {
            return "No tasks";
        }
        return tasks.stream()
                .map(task -> "- " + task.getTitle()
                        + " | goal " + nullToDash(task.getGoalTitle())
                        + " | date " + task.getPlanDate()
                        + " | planned " + nullToZero(task.getPlannedMinutes()) + " minutes"
                        + " | status " + task.getStatus())
                .collect(Collectors.joining("\n"));
    }

    private String formatCheckins(List<CheckinVO> checkins) {
        if (checkins.isEmpty()) {
            return "No checkins";
        }
        return checkins.stream()
                .map(checkin -> "- " + nullToDash(checkin.getTaskTitle())
                        + " | actual " + nullToZero(checkin.getActualMinutes()) + " minutes"
                        + " | content " + nullToDash(checkin.getContent()))
                .collect(Collectors.joining("\n"));
    }

    private String formatStats(CheckinStatsVO stats) {
        return "total minutes " + nullToZero(stats.getTotalMinutes())
                + ", checkins " + nullToZero(stats.getCheckinCount())
                + ", completed tasks " + nullToZero(stats.getCompletedTaskCount())
                + ", average mood " + stats.getAverageMood()
                + ", average difficulty " + stats.getAverageDifficulty();
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
