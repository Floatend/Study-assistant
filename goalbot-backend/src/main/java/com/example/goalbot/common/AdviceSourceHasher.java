package com.example.goalbot.common;

import com.example.goalbot.vo.TaskVO;
import com.example.goalbot.vo.GoalVO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class AdviceSourceHasher {

    private static final Pattern HASH_PATTERN = Pattern.compile("\\[source:([a-f0-9]{16})]$");

    private AdviceSourceHasher() {
    }

    public static String adviceSourceHash(List<TaskVO> tasks, List<GoalVO> goals) {
        return adviceSourceHash(tasks, goals, null, null);
    }

    public static String adviceSourceHash(List<TaskVO> tasks, List<GoalVO> goals, LocalDate startDate, LocalDate endDate) {
        String source = "range|" + Objects.toString(startDate, "") + "|" + Objects.toString(endDate, "")
                + "\ngoals\n"
                + goals.stream()
                .sorted(Comparator.comparing(GoalVO::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(AdviceSourceHasher::goalSource)
                .collect(Collectors.joining("\n"))
                + "\ntasks\n"
                + tasks.stream()
                .sorted(Comparator.comparing(TaskVO::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(AdviceSourceHasher::taskSource)
                .collect(Collectors.joining("\n"));
        return sha256(source).substring(0, 16);
    }

    public static String summaryWithHash(String summary, String hash) {
        return displaySummary(summary) + " [source:" + hash + "]";
    }

    public static String displaySummary(String summary) {
        if (summary == null) {
            return "";
        }
        return HASH_PATTERN.matcher(summary).replaceFirst("").trim();
    }

    public static String extractHash(String summary) {
        if (summary == null) {
            return "";
        }
        Matcher matcher = HASH_PATTERN.matcher(summary.trim());
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String taskSource(TaskVO task) {
        return join(
                task.getId(),
                task.getGoalId(),
                task.getGoalTitle(),
                task.getTitle(),
                task.getDescription(),
                task.getPlanDate(),
                task.getStartTime(),
                task.getEndTime(),
                task.getPlannedMinutes(),
                task.getStatus()
        );
    }

    private static String goalSource(GoalVO goal) {
        return join(
                goal.getId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getStartDate(),
                goal.getEndDate(),
                goal.getPriority(),
                goal.getStatus(),
                goal.getTotalTaskCount(),
                goal.getCompletedTaskCount()
        );
    }

    private static String join(Object... values) {
        return Arrays.stream(values)
                .map(value -> Objects.toString(value, ""))
                .collect(Collectors.joining("|"));
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash advice source", ex);
        }
    }
}
