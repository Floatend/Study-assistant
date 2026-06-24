package com.example.goalbot.agent.tool;

import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class NaturalTaskListParser {

    private static final Pattern SEPARATOR = Pattern.compile("[，,、；;\\n]+|(?:还有|以及|然后)");
    private static final Pattern EXPLICIT_SCHEDULE = Pattern.compile(
            ".*\\d{1,2}\\s*[:：]\\s*\\d{1,2}\\s*(?:-|~|—|–|至|到).*",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DATE_PREFIX = Pattern.compile(
            "^(?:今天|今日|今晚|明天|明日|后天)\\s*(?:要|想|准备|计划|打算)?\\s*"
    );
    private static final Pattern COMMAND_PREFIX = Pattern.compile(
            "^(?:帮我)?(?:创建|新增|添加|安排|记下|记录)\\s*(?:任务|日程)?\\s*"
    );

    private NaturalTaskListParser() {
    }

    public static List<String> parse(String sourceText) {
        if (!StringUtils.hasText(sourceText) || EXPLICIT_SCHEDULE.matcher(sourceText).matches()) {
            return List.of();
        }

        String normalized = sourceText.trim()
                .replace('：', ':')
                .replaceAll("[。！？!?]+$", "");
        String[] parts = SEPARATOR.split(normalized);
        if (parts.length < 2 || parts.length > 8) {
            return List.of();
        }

        Set<String> titles = new LinkedHashSet<>();
        for (int index = 0; index < parts.length; index++) {
            String title = cleanup(parts[index], index == 0);
            if (!isTaskTitle(title)) {
                return List.of();
            }
            titles.add(title);
        }
        return titles.size() >= 2 ? List.copyOf(titles) : List.of();
    }

    public static boolean isLikelyTaskList(String sourceText) {
        List<String> titles = parse(sourceText);
        if (titles.isEmpty()) {
            return false;
        }
        boolean explicitPlanning = sourceText.matches(".*(?:任务|日程|安排|计划|要做|待办|todo).*");
        return explicitPlanning || titles.stream().allMatch(NaturalTaskListParser::looksActionable);
    }

    private static boolean looksActionable(String title) {
        return title.matches(".*(?:写|复习|学习|完成|做|上课|开会|整理|阅读|背|练习|准备|提交|参加|打卡|处理|修改|看|吃|买|跑步|健身).*");
    }

    private static String cleanup(String part, boolean first) {
        String value = part == null ? "" : part.trim();
        if (first) {
            value = COMMAND_PREFIX.matcher(value).replaceFirst("");
            value = DATE_PREFIX.matcher(value).replaceFirst("");
        }
        value = value.replaceFirst("^(?:我)?(?:要|想|准备|计划|打算)\\s*", "")
                .replaceAll("^[：:\\s]+|[：:\\s]+$", "")
                .trim();
        return value;
    }

    private static boolean isTaskTitle(String value) {
        if (!StringUtils.hasText(value) || value.length() < 2 || value.length() > 128) {
            return false;
        }
        if (value.matches("(?i)^\\d+(?:\\.\\d+)?\\s*(分钟|分|min|m|小时|h)$")) {
            return false;
        }
        if (value.matches("^\\d{1,4}[./\\-年]\\d{1,2}(?:[./\\-月]\\d{1,2})?日?$")) {
            return false;
        }
        return value.matches(".*[\\p{IsHan}A-Za-z].*");
    }
}
