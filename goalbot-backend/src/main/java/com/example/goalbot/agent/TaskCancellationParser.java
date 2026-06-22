package com.example.goalbot.agent;

import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TaskCancellationParser {

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?:((?:19|20)\\d{2})\\s*[-/年.]\\s*)?([0-9]{1,2})\\s*[-/月.]\\s*([0-9]{1,2})\\s*(?:日|号)?"
    );
    private static final Pattern RELATIVE_WEEK_PATTERN = Pattern.compile(
            "大后周|大下周|下下周|下下个星期|下下星期|下周|下个星期|下星期|本周|这周|这个星期|本星期"
    );
    private static final Pattern ACTION_PATTERN = Pattern.compile("取消|删掉|删除|移除|去掉|划掉|清空|作废");
    private static final Pattern TASK_SCOPE_PATTERN = Pattern.compile("任务|待办|日程|安排");

    private TaskCancellationParser() {
    }

    public static Optional<Request> parse(String text, LocalDate today) {
        if (!StringUtils.hasText(text) || today == null) {
            return Optional.empty();
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        if (!ACTION_PATTERN.matcher(normalized).find()) {
            return Optional.empty();
        }

        List<LocalDate> explicitDates = parseDates(normalized, today);
        LocalDate planDate = null;
        LocalDate rangeStartDate = null;
        LocalDate rangeEndDate = null;
        if (explicitDates.size() >= 2) {
            rangeStartDate = explicitDates.get(0);
            rangeEndDate = explicitDates.get(1);
            if (rangeEndDate.isBefore(rangeStartDate)) {
                rangeEndDate = rangeEndDate.plusYears(1);
            }
        } else if (explicitDates.size() == 1) {
            if (containsWeekScope(normalized)) {
                rangeStartDate = startOfWeek(explicitDates.get(0));
                rangeEndDate = rangeStartDate.plusDays(6);
            } else {
                planDate = explicitDates.get(0);
            }
        } else {
            int weekOffset = relativeWeekOffset(normalized);
            if (weekOffset >= 0) {
                rangeStartDate = startOfWeek(today).plusWeeks(weekOffset);
                rangeEndDate = rangeStartDate.plusDays(6);
            } else {
                planDate = resolveRelativeDay(normalized, today);
            }
        }

        if (planDate == null && (rangeStartDate == null || rangeEndDate == null)) {
            return Optional.empty();
        }

        String keyword = extractTaskKeyword(normalized);
        boolean explicitTaskScope = TASK_SCOPE_PATTERN.matcher(normalized).find();
        if (!explicitTaskScope && StringUtils.hasText(keyword)) {
            return Optional.empty();
        }
        return Optional.of(new Request(planDate, rangeStartDate, rangeEndDate, keyword));
    }

    private static List<LocalDate> parseDates(String text, LocalDate today) {
        List<LocalDate> dates = new ArrayList<>();
        Matcher matcher = DATE_PATTERN.matcher(text);
        while (matcher.find() && dates.size() < 2) {
            try {
                boolean explicitYear = StringUtils.hasText(matcher.group(1));
                int year = explicitYear ? Integer.parseInt(matcher.group(1)) : today.getYear();
                LocalDate date = LocalDate.of(year, Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
                if (!explicitYear && date.isBefore(today.minusDays(7))) {
                    date = date.plusYears(1);
                }
                dates.add(date);
            } catch (RuntimeException ignored) {
                // Invalid dates remain unresolved and cannot reach a destructive tool call.
            }
        }
        return dates;
    }

    private static int relativeWeekOffset(String text) {
        if (text.contains("大后周") || text.contains("大下周") || text.contains("下下周")
                || text.contains("下下个星期") || text.contains("下下星期")) {
            return 2;
        }
        if (text.contains("下周") || text.contains("下个星期") || text.contains("下星期")) {
            return 1;
        }
        if (text.contains("本周") || text.contains("这周") || text.contains("这个星期") || text.contains("本星期")) {
            return 0;
        }
        return -1;
    }

    private static boolean containsWeekScope(String text) {
        return RELATIVE_WEEK_PATTERN.matcher(text).find()
                || text.contains("那周")
                || text.contains("这一周")
                || text.contains("那一周")
                || text.contains("某一周");
    }

    private static LocalDate resolveRelativeDay(String text, LocalDate today) {
        if (text.contains("大后天")) {
            return today.plusDays(3);
        }
        if (text.contains("后天")) {
            return today.plusDays(2);
        }
        if (text.contains("明天") || text.contains("明日")) {
            return today.plusDays(1);
        }
        if (text.contains("今天") || text.contains("今日")) {
            return today;
        }
        return null;
    }

    private static LocalDate startOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static String extractTaskKeyword(String text) {
        String cleaned = DATE_PATTERN.matcher(text).replaceAll(" ");
        cleaned = RELATIVE_WEEK_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = cleaned.replaceAll(
                "请|帮我|麻烦|把|将|从|到|至|起|今天|今日|明天|明日|后天|大后天|"
                        + "这一周|那一周|那周|一周|该周|的|都|全都|全部|所有|"
                        + "任务|待办|日程|安排|取消掉|取消|删掉|删除|移除|去掉|划掉|清空|作废",
                " "
        );
        cleaned = cleaned.replaceAll("[\\s,，、;；:：。.!！?？~～\\-]+", "").trim();
        return StringUtils.hasText(cleaned) ? cleaned : null;
    }

    public record Request(
            LocalDate planDate,
            LocalDate rangeStartDate,
            LocalDate rangeEndDate,
            String taskKeyword
    ) {
    }
}
