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

public final class ImportedScheduleCancellationParser {

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?:((?:19|20)\\d{2})\\s*[-/年.]\\s*)?([0-9]{1,2})\\s*[-/月.]\\s*([0-9]{1,2})\\s*(?:日|号)?"
    );
    private static final Pattern TERM_WEEK_PATTERN = Pattern.compile("第\\s*[0-9一二三四五六七八九十两]+\\s*周");
    private static final Pattern RELATIVE_WEEK_PATTERN = Pattern.compile(
            "大后周|大下周|下下周|下下个星期|下下星期|下周|下个星期|下星期|本周|这周|这个星期|本星期"
    );
    private static final Pattern ACTION_PATTERN = Pattern.compile(
            "不上了|不上课|不用上了|不用上|停课|取消|删掉|删除|移除|去掉|划掉|清空|作废"
    );
    private static final Pattern COURSE_PATTERN = Pattern.compile("课程|课表|课|ICS|ics|导入日程");

    private ImportedScheduleCancellationParser() {
    }

    public static Optional<Request> parse(String text, LocalDate today) {
        if (!StringUtils.hasText(text) || today == null) {
            return Optional.empty();
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        if (!ACTION_PATTERN.matcher(normalized).find() || !COURSE_PATTERN.matcher(normalized).find()) {
            return Optional.empty();
        }

        List<LocalDate> explicitDates = parseDates(normalized, today);
        LocalDate startDate = null;
        LocalDate endDate = null;
        if (explicitDates.size() >= 2) {
            startDate = explicitDates.get(0);
            endDate = explicitDates.get(1);
            if (endDate.isBefore(startDate)) {
                endDate = endDate.plusYears(1);
            }
        } else if (explicitDates.size() == 1 && containsWeekScope(normalized)) {
            startDate = startOfWeek(explicitDates.get(0));
            endDate = startDate.plusDays(6);
        } else {
            int weekOffset = relativeWeekOffset(normalized);
            if (weekOffset >= 0) {
                startDate = startOfWeek(today).plusWeeks(weekOffset);
                endDate = startDate.plusDays(6);
            }
        }

        boolean hasWeekScope = containsWeekScope(normalized) || explicitDates.size() >= 2;
        if (!hasWeekScope) {
            return Optional.empty();
        }
        return Optional.of(new Request(startDate, endDate, extractCourseKeyword(normalized)));
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
                // Invalid dates are left unresolved so the tool can ask for a clear range.
            }
        }
        return dates;
    }

    private static boolean containsWeekScope(String text) {
        return RELATIVE_WEEK_PATTERN.matcher(text).find()
                || TERM_WEEK_PATTERN.matcher(text).find()
                || text.contains("那周")
                || text.contains("这一周")
                || text.contains("那一周")
                || text.contains("某一周");
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

    private static LocalDate startOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static String extractCourseKeyword(String text) {
        String cleaned = DATE_PATTERN.matcher(text).replaceAll(" ");
        cleaned = TERM_WEEK_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = RELATIVE_WEEK_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = cleaned.replaceAll(
                "请|帮我|麻烦|把|将|从|到|至|起|这一周|那一周|某一周|那周|一周|该周|的|都|全都|全部|所有|"
                        + "导入的|导入|ICS|ics|课程|课表|日程|任务|安排|"
                        + "不用上了|不用上|不上了|不上课|停课|取消掉|取消|删掉|删除|移除|去掉|划掉|清空|作废|课",
                " "
        );
        cleaned = cleaned.replaceAll("[\\s,，、;；:：。.!！?？~～\\-]+", "").trim();
        return StringUtils.hasText(cleaned) ? cleaned : null;
    }

    public record Request(LocalDate startDate, LocalDate endDate, String courseKeyword) {
    }
}
