package com.example.goalbot.agent.dialogue;

import com.example.goalbot.entity.ConversationTaskDraft;
import com.example.goalbot.entity.Task;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RelativeTaskTimeResolver {

    private final TaskService taskService;

    public Resolution resolve(Long userId, ConversationTaskDraft draft, String text) {
        String keyword = extractKeyword(text);
        if (keyword == null) {
            return Resolution.notApplicable();
        }

        return resolveByKeyword(userId, draft, keyword);
    }

    public Resolution resolveStructured(
            Long userId,
            ConversationTaskDraft draft,
            Long taskId,
            String taskQuery,
            String relation,
            String boundary
    ) {
        if (!"AFTER".equalsIgnoreCase(relation) || !"END".equalsIgnoreCase(boundary)) {
            return Resolution.clarification(taskQuery,
                    "目前只支持把当前任务安排在另一任务结束之后，请直接告诉我开始时间。");
        }
        LocalDate planDate = draft.getPlanDate() == null ? LocalDate.now() : draft.getPlanDate();
        if (taskId != null) {
            try {
                Task task = taskService.getOwnedTaskEntity(userId, taskId);
                if (task.getPlanDate() != null && !planDate.equals(task.getPlanDate())) {
                    return Resolution.clarification(taskQuery,
                            "被引用任务「" + task.getTitle() + "」不在 " + planDate + "，请确认日期或直接说开始时间。");
                }
                if (task.getEndTime() == null) {
                    return Resolution.clarification(taskQuery,
                            "「" + task.getTitle() + "」还没有结束时间，请先补全它或直接说开始时间。");
                }
                return Resolution.resolved(taskQuery, task.getId(), task.getTitle(), task.getEndTime());
            } catch (RuntimeException ex) {
                return Resolution.clarification(taskQuery,
                        "我无法访问被引用的任务，请重新选择任务或直接说开始时间。");
            }
        }
        if (!StringUtils.hasText(taskQuery)) {
            return Resolution.clarification(null, "请告诉我要接在哪个任务之后。");
        }
        return resolveByKeyword(userId, draft, taskQuery);
    }

    private Resolution resolveByKeyword(Long userId, ConversationTaskDraft draft, String keyword) {

        LocalDate planDate = draft.getPlanDate() == null ? LocalDate.now() : draft.getPlanDate();
        List<TaskVO> tasks = taskService.listActiveTasksByDate(userId, planDate);
        List<TaskVO> matches = isPreviousReference(keyword)
                ? latestTask(tasks)
                : tasks.stream()
                .filter(task -> matches(task.getTitle(), keyword))
                .toList();

        if (matches.isEmpty()) {
            return Resolution.clarification(keyword,
                    "我没找到 " + planDate + " 名称包含「" + keyword
                            + "」的已排任务。请告诉我具体任务名，或者直接说开始时间。");
        }
        if (matches.size() > 1) {
            String options = matches.stream()
                    .map(task -> "- " + task.getTitle() + formatRange(task))
                    .collect(Collectors.joining("\n"));
            return Resolution.clarification(keyword,
                    "我找到多个包含「" + keyword + "」的任务，你指的是哪一个？\n" + options);
        }

        TaskVO referencedTask = matches.get(0);
        if (referencedTask.getEndTime() == null) {
            return Resolution.clarification(keyword,
                    "「" + referencedTask.getTitle() + "」还没有结束时间，暂时不能计算“接着它”的开始时间。"
                            + "请先补全它的时间，或者直接告诉我当前任务几点开始。");
        }
        return Resolution.resolved(keyword, referencedTask);
    }

    public static boolean isRelativeReference(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String normalized = text.trim().replaceAll("[，。！？、,.!?]+$", "");
        return normalized.matches("^(?:紧接着|紧接|接着|接在|排在|跟在|等).+")
                || normalized.matches("^.+(?:结束后|之后|以后|后面)$");
    }

    private String extractKeyword(String text) {
        if (!isRelativeReference(text)) {
            return null;
        }
        String keyword = text.trim()
                .replaceAll("[，。！？、,.!?]+$", "")
                .replaceFirst("^(?:紧接着|紧接|接着|接在|排在|跟在|等)\\s*", "")
                .replaceFirst("(?:结束后|之后|以后|后面)$", "")
                .replaceFirst("(?:的)?后$", "")
                .replaceAll("^(?:任务)?\\s*|\\s*(?:这个任务)?$", "")
                .trim();
        return StringUtils.hasText(keyword) ? keyword : null;
    }

    private boolean isPreviousReference(String keyword) {
        return keyword.matches("上一个|前一个|刚才那个|刚刚那个");
    }

    private List<TaskVO> latestTask(List<TaskVO> tasks) {
        return tasks.stream()
                .filter(task -> task.getEndTime() != null)
                .max(Comparator.comparing(TaskVO::getEndTime)
                        .thenComparing(TaskVO::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(List::of)
                .orElseGet(List::of);
    }

    private boolean matches(String title, String keyword) {
        if (!StringUtils.hasText(title)) {
            return false;
        }
        String normalizedTitle = title.replaceAll("\\s+", "").toLowerCase();
        String normalizedKeyword = keyword.replaceAll("\\s+", "").toLowerCase();
        return normalizedTitle.contains(normalizedKeyword) || normalizedKeyword.contains(normalizedTitle);
    }

    private String formatRange(TaskVO task) {
        if (task.getStartTime() == null && task.getEndTime() == null) {
            return " | 未排时间";
        }
        return " | " + formatTime(task.getStartTime()) + "-" + formatTime(task.getEndTime());
    }

    private String formatTime(LocalTime time) {
        return time == null ? "未设置" : time.toString();
    }

    public record Resolution(
            boolean applicable,
            LocalTime startTime,
            Long referencedTaskId,
            String referencedTaskTitle,
            String keyword,
            String clarificationQuestion
    ) {

        public static Resolution notApplicable() {
            return new Resolution(false, null, null, null, null, null);
        }

        static Resolution clarification(String keyword, String question) {
            return new Resolution(true, null, null, null, keyword, question);
        }

        static Resolution resolved(String keyword, TaskVO task) {
            return new Resolution(true, task.getEndTime(), task.getId(), task.getTitle(), keyword, null);
        }

        static Resolution resolved(String keyword, Long taskId, String taskTitle, LocalTime endTime) {
            return new Resolution(true, endTime, taskId, taskTitle, keyword, null);
        }

        public boolean resolved() {
            return startTime != null;
        }

        public boolean requiresClarification() {
            return applicable && !resolved() && StringUtils.hasText(clarificationQuestion);
        }
    }
}
