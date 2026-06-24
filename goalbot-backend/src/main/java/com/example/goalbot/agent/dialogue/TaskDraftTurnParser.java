package com.example.goalbot.agent.dialogue;

import com.example.goalbot.agent.tool.ScheduleTextParser;
import com.example.goalbot.entity.ConversationTaskDraft;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class TaskDraftTurnParser {

    public TaskDraftFrame parse(
            ConversationTaskDraft draft,
            String text,
            LocalDateTime now,
            LocalDate planDateArgument,
            LocalTime startTimeArgument,
            LocalTime endTimeArgument,
            Integer plannedMinutesArgument
    ) {
        String rawText = text == null ? "" : text.trim();
        TaskDraftFrame frame = new TaskDraftFrame();
        frame.setRawText(rawText);

        LocalDate parsedDate = parseRelativeDate(rawText, now.toLocalDate());
        frame.setPlanDate(firstNonNull(planDateArgument, parsedDate));
        if (planDateArgument != null) {
            frame.getSlotSources().put("plan_date", "tool-argument");
        } else if (parsedDate != null) {
            frame.getSlotSources().put("plan_date", "rule-relative-date");
        }

        String timeText = rawText;
        if (!ScheduleTextParser.hasMeridiem(rawText) && draft != null) {
            timeText = ScheduleTextParser.inheritMeridiem(draft.getSourceText(), rawText);
        }
        List<ScheduleTextParser.TimeMention> mentions = ScheduleTextParser.parseTimes(timeText);

        boolean startsNow = containsAny(rawText, "现在", "马上", "立刻");
        LocalTime parsedStart = null;
        LocalTime parsedEnd = null;
        ScheduleTextParser.TimeMention startMention = null;
        ScheduleTextParser.TimeMention endMention = null;

        if (startsNow) {
            parsedStart = now.toLocalTime().truncatedTo(ChronoUnit.MINUTES);
            frame.getSlotSources().put("start_time", "rule-now");
        }
        if (mentions.size() >= 2) {
            startMention = mentions.get(0);
            endMention = mentions.get(1);
            if (parsedStart == null) {
                parsedStart = startMention.time();
                frame.getSlotSources().put("start_time", "rule-time-range");
            }
            parsedEnd = endMention.time();
            frame.getSlotSources().put("end_time", "rule-time-range");
        } else if (mentions.size() == 1) {
            ScheduleTextParser.TimeMention mention = mentions.get(0);
            if (isEndTimeExpression(rawText, mention.raw()) || completesKnownStart(draft)) {
                endMention = mention;
                parsedEnd = mention.time();
                frame.getSlotSources().put("end_time", "rule-end-cue");
            } else if (parsedStart == null) {
                startMention = mention;
                parsedStart = mention.time();
                frame.getSlotSources().put("start_time", "rule-start-time");
            }
        }

        Integer parsedDuration = ScheduleTextParser.parseDuration(rawText);
        LocalTime startTime = firstNonNull(startTimeArgument, parsedStart);
        LocalTime endTime = firstNonNull(endTimeArgument, parsedEnd);
        Integer plannedMinutes = firstPositive(plannedMinutesArgument, parsedDuration);
        boolean explicitNextDayEnd = endTime != null
                && containsAny(rawText, "明天", "明日", "次日", "第二天", "隔天")
                && (endMention != null || endTimeArgument != null)
                && (isEndTimeExpression(rawText, endMention == null ? "" : endMention.raw())
                || completesKnownStart(draft));
        if (explicitNextDayEnd && planDateArgument == null) {
            frame.setPlanDate(null);
            frame.getSlotSources().remove("plan_date");
        }

        frame.setStartTime(startTime);
        frame.setEndTime(endTime);
        frame.setPlannedMinutes(plannedMinutes);
        frame.setStartExplicit(startTime != null);
        frame.setEndExplicit(endTime != null);
        frame.setEndNextDay(explicitNextDayEnd);
        frame.setDurationExplicit(plannedMinutes != null && plannedMinutes > 0);
        if (startTimeArgument != null) {
            frame.getSlotSources().put("start_time", "tool-argument");
        }
        if (endTimeArgument != null) {
            frame.getSlotSources().put("end_time", "tool-argument");
        }
        if (plannedMinutesArgument != null && plannedMinutesArgument > 0) {
            frame.getSlotSources().put("planned_minutes", "tool-argument");
        } else if (parsedDuration != null && parsedDuration > 0) {
            frame.getSlotSources().put("planned_minutes", "rule-duration");
        }

        LocalTime effectiveStart = startTime != null ? startTime : draft == null ? null : draft.getStartTime();
        if (endTime != null && effectiveStart != null && !endTime.isAfter(effectiveStart)
                && !frame.isEndNextDay()
                && !isExplicitOvernight(startMention, endMention)) {
            frame.setConflictCode("END_NOT_AFTER_START");
            frame.setClarificationQuestion(buildEndConflictQuestion(effectiveStart, endTime));
        }
        return frame;
    }

    private boolean isEndTimeExpression(String text, String rawMention) {
        int mentionIndex = text.indexOf(rawMention);
        if (mentionIndex < 0) {
            mentionIndex = text.length();
        }
        String prefix = text.substring(0, mentionIndex);
        return prefix.matches("(?s).*(到|至|截止|结束于|结束到)\\s*$")
                || (containsAny(text, "结束", "截止") && !containsAny(text, "开始"));
    }

    private boolean completesKnownStart(ConversationTaskDraft draft) {
        return draft != null
                && draft.getStartTime() != null
                && draft.getEndTime() == null
                && (draft.getPlannedMinutes() == null || draft.getPlannedMinutes() <= 0);
    }

    private boolean isExplicitOvernight(
            ScheduleTextParser.TimeMention startMention,
            ScheduleTextParser.TimeMention endMention
    ) {
        if (endMention == null) {
            return false;
        }
        boolean explicitMidnight = endMention.time().equals(LocalTime.MIDNIGHT)
                && containsAny(endMention.meridiem(), "晚上", "今晚", "凌晨");
        if (!"凌晨".equals(endMention.meridiem()) && !explicitMidnight) {
            return false;
        }
        return startMention == null || containsAny(startMention.meridiem(), "晚上", "今晚", "下午");
    }

    private String buildEndConflictQuestion(LocalTime startTime, LocalTime endTime) {
        if (endTime.equals(LocalTime.NOON)) {
            return "你说的结束时间早于开始时间。你是指明天中午 12:00，还是今晚 24:00？也可以直接告诉我预计用时。";
        }
        return "结束时间 " + endTime + " 早于开始时间 " + startTime
                + "。这是跨天安排，还是时间说错了？也可以直接告诉我预计用时。";
    }

    private LocalDate parseRelativeDate(String text, LocalDate today) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        if (text.contains("后天")) {
            return today.plusDays(2);
        }
        if (text.contains("明天") || text.contains("明日")) {
            return today.plusDays(1);
        }
        if (containsAny(text, "今天", "今日", "今晚")) {
            return today;
        }
        return null;
    }

    private boolean containsAny(String text, String... values) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private Integer firstPositive(Integer first, Integer second) {
        if (first != null && first > 0) {
            return first;
        }
        return second != null && second > 0 ? second : null;
    }
}
