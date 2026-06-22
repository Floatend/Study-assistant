package com.example.goalbot.agent.tool;

import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BatchScheduleParser {

    private static final String DATE =
            "(?:(?<year>(?:19|20)\\d{2})\\s*[./\\-年]\\s*)?"
                    + "(?<month>\\d{1,2})\\s*(?:[./\\-]|月)\\s*(?<day>\\d{1,2})\\s*(?:日|号)?";
    private static final String TIME_RANGE =
            "(?<startHour>\\d{1,2})\\s*[:：]\\s*(?<startMinute>\\d{1,2})"
                    + "\\s*(?:-|~|—|–|至|到)\\s*"
                    + "(?<endHour>\\d{1,2})\\s*[:：]\\s*(?<endMinute>\\d{1,2})";
    private static final String NEXT_DATE =
            "(?:(?:19|20)\\d{2}\\s*[./\\-年]\\s*)?"
                    + "\\d{1,2}\\s*(?:[./\\-]|月)\\s*\\d{1,2}\\s*(?:日|号)?";
    private static final String NEXT_TIME_RANGE =
            "\\d{1,2}\\s*[:：]\\s*\\d{1,2}"
                    + "\\s*(?:-|~|—|–|至|到)\\s*"
                    + "\\d{1,2}\\s*[:：]\\s*\\d{1,2}";
    private static final Pattern ENTRY_PATTERN = Pattern.compile(
            DATE
                    + "\\s*[,，、;；]?\\s*"
                    + TIME_RANGE
                    + "\\s*[,，、;；]?\\s*"
                    + "(?<title>.+?)"
                    + "(?=(?:\\s*[,，、;；]?\\s*" + NEXT_DATE + "\\s*[,，、;；]?\\s*" + NEXT_TIME_RANGE + ")|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern COMMAND_PREFIX_PATTERN = Pattern.compile(
            "^(?:请|帮我|给我|麻烦)?\\s*(?:创建|新建|新增|添加|导入|安排|记一下|记录)?\\s*(?:日程|任务|课表|课程|安排)?\\s*[,，、;；:]?\\s*"
    );

    private BatchScheduleParser() {
    }

    static List<Entry> parse(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }

        String normalized = normalize(text);
        Matcher matcher = ENTRY_PATTERN.matcher(normalized);
        List<Entry> entries = new ArrayList<>();
        LocalDate today = LocalDate.now();
        while (matcher.find()) {
            LocalDate date = parseDate(matcher, today);
            LocalTime startTime = parseTime(matcher.group("startHour"), matcher.group("startMinute"));
            LocalTime endTime = parseTime(matcher.group("endHour"), matcher.group("endMinute"));
            String title = cleanTitle(matcher.group("title"));
            if (date == null || startTime == null || endTime == null || !StringUtils.hasText(title)) {
                continue;
            }
            if (!endTime.isAfter(startTime)) {
                continue;
            }
            int plannedMinutes = (int) Duration.between(startTime, endTime).toMinutes();
            entries.add(new Entry(date, startTime, endTime, plannedMinutes, title));
        }
        return entries;
    }

    private static String normalize(String text) {
        return text
                .replace('\u00A0', ' ')
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static LocalDate parseDate(Matcher matcher, LocalDate today) {
        try {
            int year = StringUtils.hasText(matcher.group("year"))
                    ? Integer.parseInt(matcher.group("year"))
                    : today.getYear();
            int month = Integer.parseInt(matcher.group("month"));
            int day = Integer.parseInt(matcher.group("day"));
            LocalDate date = LocalDate.of(year, month, day);
            if (!StringUtils.hasText(matcher.group("year")) && date.isBefore(today)) {
                return date.plusYears(1);
            }
            return date;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static LocalTime parseTime(String hourValue, String minuteValue) {
        try {
            int hour = Integer.parseInt(hourValue);
            int minute = Integer.parseInt(minuteValue);
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null;
            }
            return LocalTime.of(hour, minute);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String cleanTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return null;
        }
        String cleaned = title
                .replaceAll("^[\\s,，、;；:：]+", "")
                .replaceAll("[\\s,，、;；:：]+$", "")
                .trim();
        cleaned = COMMAND_PREFIX_PATTERN.matcher(cleaned).replaceFirst("").trim();
        return StringUtils.hasText(cleaned) ? cleaned : null;
    }

    record Entry(LocalDate planDate, LocalTime startTime, LocalTime endTime, int plannedMinutes, String title) {
    }
}
