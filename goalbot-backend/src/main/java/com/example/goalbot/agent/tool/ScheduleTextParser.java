package com.example.goalbot.agent.tool;

import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScheduleTextParser {

    private static final String NUMBER = "[0-9]{1,3}(?:\\.\\d+)?|[零一二两三四五六七八九十百]{1,6}";
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(今晚|晚上|下午|上午|早上|中午|凌晨)?\\s*(" + NUMBER + ")\\s*(?:[:点]\\s*(?:(半)|(" + NUMBER + ")\\s*(?:分|分钟)?)?)?",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern HALF_HOUR_DURATION_PATTERN = Pattern.compile(
            "(" + NUMBER + ")?\\s*(?:个)?半小时",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(" + NUMBER + ")\\s*(分钟|分|min|m|个小时|小时|h)",
            Pattern.CASE_INSENSITIVE
    );

    private ScheduleTextParser() {
    }

    public static LocalTime parseTime(String text) {
        List<TimeMention> mentions = parseTimes(text);
        return mentions.isEmpty() ? null : mentions.get(0).time();
    }

    public static List<TimeMention> parseTimes(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        List<TimeMention> mentions = new ArrayList<>();
        Matcher matcher = TIME_PATTERN.matcher(text);
        while (matcher.find()) {
            String raw = matcher.group(0);
            if (!StringUtils.hasText(raw) || !isTimeLike(raw)) {
                continue;
            }
            Double hourNumber = parseNumber(matcher.group(2));
            if (hourNumber == null) {
                continue;
            }
            int hour = (int) Math.round(hourNumber);
            int minute = 0;
            if (StringUtils.hasText(matcher.group(3))) {
                minute = 30;
            } else if (StringUtils.hasText(matcher.group(4))) {
                Double minuteNumber = parseNumber(matcher.group(4));
                if (minuteNumber == null) {
                    continue;
                }
                minute = (int) Math.round(minuteNumber);
            }

            String meridiem = matcher.group(1);
            if (("今晚".equals(meridiem) || "晚上".equals(meridiem)) && hour == 12) {
                hour = 0;
            } else if (("今晚".equals(meridiem) || "晚上".equals(meridiem) || "下午".equals(meridiem)) && hour < 12) {
                hour += 12;
            }
            if ("中午".equals(meridiem) && hour < 11) {
                hour += 12;
            }
            if ("凌晨".equals(meridiem) && hour == 12) {
                hour = 0;
            }
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                continue;
            }
            mentions.add(new TimeMention(LocalTime.of(hour, minute), meridiem,
                    matcher.start(), matcher.end(), raw.trim()));
        }
        return List.copyOf(mentions);
    }

    public static Integer parseDuration(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }

        Matcher halfMatcher = HALF_HOUR_DURATION_PATTERN.matcher(text);
        if (halfMatcher.find()) {
            Double value = parseNumber(halfMatcher.group(1));
            int baseMinutes = value == null ? 0 : (int) Math.round(value * 60);
            return baseMinutes + 30;
        }

        Matcher matcher = DURATION_PATTERN.matcher(text);
        Integer minuteFallback = null;
        while (matcher.find()) {
            Double value = parseNumber(matcher.group(1));
            if (value == null) {
                continue;
            }
            String unit = matcher.group(2);
            if ("小时".equals(unit) || "个小时".equals(unit) || "h".equalsIgnoreCase(unit)) {
                return (int) Math.round(value * 60);
            }
            if (minuteFallback == null) {
                minuteFallback = (int) Math.round(value);
            }
        }
        return minuteFallback;
    }

    public static String inheritMeridiem(String source, String text) {
        if (hasMeridiem(text) || !StringUtils.hasText(source)) {
            return text;
        }
        if (!looksLikeTimeText(text)) {
            return text;
        }
        if (source.contains("今晚") || source.contains("晚上")) {
            return "晚上 " + text;
        }
        if (source.contains("下午")) {
            return "下午 " + text;
        }
        if (source.contains("上午") || source.contains("早上")) {
            return "上午 " + text;
        }
        if (source.contains("中午")) {
            return "中午 " + text;
        }
        if (source.contains("凌晨")) {
            return "凌晨 " + text;
        }
        return text;
    }

    private static boolean looksLikeTimeText(String text) {
        return StringUtils.hasText(text)
                && (text.contains(":") || text.contains("点"));
    }

    public static boolean hasMeridiem(String text) {
        return StringUtils.hasText(text)
                && (text.contains("今晚") || text.contains("晚上") || text.contains("下午")
                || text.contains("上午") || text.contains("早上") || text.contains("中午") || text.contains("凌晨"));
    }

    private static boolean isTimeLike(String raw) {
        return raw.contains(":")
                || raw.contains("点")
                || raw.contains("半")
                || raw.contains("分")
                || hasMeridiem(raw);
    }

    private static Double parseNumber(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim().replace("两", "二");
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            // Continue with simple Chinese numeral parsing.
        }

        int result = 0;
        int current = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '百') {
                result += (current == 0 ? 1 : current) * 100;
                current = 0;
            } else if (ch == '十') {
                result += (current == 0 ? 1 : current) * 10;
                current = 0;
            } else {
                Integer digit = chineseDigit(ch);
                if (digit == null) {
                    return null;
                }
                current = digit;
            }
        }
        return (double) (result + current);
    }

    private static Integer chineseDigit(char ch) {
        return switch (ch) {
            case '零' -> 0;
            case '一' -> 1;
            case '二' -> 2;
            case '三' -> 3;
            case '四' -> 4;
            case '五' -> 5;
            case '六' -> 6;
            case '七' -> 7;
            case '八' -> 8;
            case '九' -> 9;
            default -> null;
        };
    }

    public record TimeMention(LocalTime time, String meridiem, int start, int end, String raw) {

        public boolean hasExplicitMeridiem() {
            return StringUtils.hasText(meridiem);
        }
    }
}
