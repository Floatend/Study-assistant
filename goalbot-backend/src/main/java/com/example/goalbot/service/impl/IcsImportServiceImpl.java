package com.example.goalbot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.goalbot.common.BusinessException;
import com.example.goalbot.dto.task.TaskCreateRequest;
import com.example.goalbot.entity.Task;
import com.example.goalbot.service.IcsImportService;
import com.example.goalbot.service.TaskService;
import com.example.goalbot.vo.IcsImportEventVO;
import com.example.goalbot.vo.IcsImportResultVO;
import com.example.goalbot.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IcsImportServiceImpl implements IcsImportService {

    private static final long MAX_FILE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_TOTAL_EVENTS = 1000;
    private static final int MAX_OCCURRENCES_PER_EVENT = 300;
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"),
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm"),
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HH")
    );

    private final TaskService taskService;

    @Override
    @Transactional
    public IcsImportResultVO importIcs(
            Long userId,
            MultipartFile file,
            boolean dryRun,
            LocalDate startDate,
            LocalDate endDate,
            boolean skipExisting
    ) {
        validateFile(file);

        LocalDate rangeStart = startDate == null ? LocalDate.now().minusMonths(1) : startDate;
        LocalDate rangeEnd = endDate == null ? LocalDate.now().plusMonths(6) : endDate;
        if (rangeStart.isAfter(rangeEnd)) {
            throw BusinessException.badRequest("Import start date cannot be after end date");
        }

        IcsImportResultVO result = new IcsImportResultVO();
        result.setDryRun(dryRun);

        String content = readContent(file);
        List<IcsEvent> sourceEvents = parseEvents(content, result.getWarnings());
        result.setSourceEventCount(sourceEvents.size());

        List<IcsImportEventVO> expandedEvents = expandEvents(sourceEvents, rangeStart, rangeEnd, result.getWarnings());
        expandedEvents.sort(Comparator
                .comparing(IcsImportEventVO::getPlanDate)
                .thenComparing(event -> event.getStartTime() == null ? LocalTime.MIN : event.getStartTime())
                .thenComparing(IcsImportEventVO::getTitle));

        result.setExpandedEventCount(expandedEvents.size());
        for (IcsImportEventVO event : expandedEvents) {
            if (result.getEvents().size() >= MAX_TOTAL_EVENTS) {
                result.getWarnings().add("Only the first " + MAX_TOTAL_EVENTS + " expanded events are shown/imported.");
                break;
            }

            if (skipExisting && existsSameTask(userId, event)) {
                event.setSkipped(true);
                event.setSkipReason("Duplicate task already exists");
                result.setSkippedCount(result.getSkippedCount() + 1);
                result.getEvents().add(event);
                continue;
            }

            if (dryRun) {
                event.setSkipped(false);
                result.getEvents().add(event);
                continue;
            }

            TaskVO task = taskService.createTask(userId, toTaskCreateRequest(event));
            result.getImportedTasks().add(task);
            result.setImportedCount(result.getImportedCount() + 1);
            event.setSkipped(false);
            result.getEvents().add(event);
        }

        return result;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("ICS file is required");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw BusinessException.badRequest("ICS file is too large. Max size is 5 MB");
        }
    }

    private String readContent(MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (content.startsWith("\uFEFF")) {
                return content.substring(1);
            }
            return content;
        } catch (IOException ex) {
            throw BusinessException.badRequest("Failed to read ICS file");
        }
    }

    private List<IcsEvent> parseEvents(String content, List<String> warnings) {
        List<String> lines = unfoldLines(content);
        List<IcsEvent> events = new ArrayList<>();
        IcsEvent current = null;

        for (String line : lines) {
            if ("BEGIN:VEVENT".equalsIgnoreCase(line)) {
                current = new IcsEvent();
                continue;
            }
            if ("END:VEVENT".equalsIgnoreCase(line)) {
                if (current != null) {
                    events.add(current);
                    current = null;
                }
                continue;
            }
            if (current == null) {
                continue;
            }
            IcsProperty property = parseProperty(line);
            if (property != null) {
                current.add(property);
            }
        }

        if (events.isEmpty()) {
            warnings.add("No VEVENT entries were found in the ICS file.");
        }
        return events;
    }

    private List<String> unfoldLines(String content) {
        String normalized = content == null ? "" : content.replace("\r\n", "\n").replace('\r', '\n');
        List<String> lines = new ArrayList<>();
        for (String rawLine : normalized.split("\n", -1)) {
            if ((rawLine.startsWith(" ") || rawLine.startsWith("\t")) && !lines.isEmpty()) {
                int lastIndex = lines.size() - 1;
                lines.set(lastIndex, lines.get(lastIndex) + rawLine.substring(1));
            } else {
                lines.add(rawLine.trim());
            }
        }
        return lines;
    }

    private IcsProperty parseProperty(String line) {
        int colonIndex = line.indexOf(':');
        if (colonIndex <= 0) {
            return null;
        }

        String left = line.substring(0, colonIndex);
        String value = unescapeText(line.substring(colonIndex + 1));
        String[] parts = left.split(";");
        String name = parts[0].trim().toUpperCase(Locale.ROOT);
        Map<String, String> params = new HashMap<>();
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            int equalsIndex = part.indexOf('=');
            if (equalsIndex <= 0) {
                continue;
            }
            String paramName = part.substring(0, equalsIndex).trim().toUpperCase(Locale.ROOT);
            String paramValue = stripQuotes(part.substring(equalsIndex + 1).trim());
            params.put(paramName, paramValue);
        }
        return new IcsProperty(name, params, value);
    }

    private List<IcsImportEventVO> expandEvents(List<IcsEvent> sourceEvents, LocalDate rangeStart, LocalDate rangeEnd, List<String> warnings) {
        List<IcsImportEventVO> events = new ArrayList<>();
        for (IcsEvent sourceEvent : sourceEvents) {
            if (events.size() >= MAX_TOTAL_EVENTS) {
                warnings.add("The ICS file contains many events; expansion stopped at " + MAX_TOTAL_EVENTS + " events.");
                break;
            }
            events.addAll(expandEvent(sourceEvent, rangeStart, rangeEnd, warnings));
        }
        return events;
    }

    private List<IcsImportEventVO> expandEvent(IcsEvent event, LocalDate rangeStart, LocalDate rangeEnd, List<String> warnings) {
        IcsDateTime start = parseDateTime(event.first("DTSTART"));
        if (start == null) {
            warnings.add("Skipped an event without DTSTART: " + event.text("SUMMARY", "Untitled event"));
            return List.of();
        }

        Duration duration = resolveDuration(event, start);
        IcsProperty rruleProperty = event.first("RRULE");
        if (rruleProperty == null || !StringUtils.hasText(rruleProperty.value())) {
            IcsImportEventVO occurrence = toImportEvent(event, start.dateTime(), duration, start.allDay());
            return isInRange(occurrence.getPlanDate(), rangeStart, rangeEnd) ? List.of(occurrence) : List.of();
        }

        Map<String, String> rule = parseRRule(rruleProperty.value());
        String frequency = rule.getOrDefault("FREQ", "").toUpperCase(Locale.ROOT);
        return switch (frequency) {
            case "DAILY" -> expandDaily(event, start, duration, rule, rangeStart, rangeEnd);
            case "WEEKLY" -> expandWeekly(event, start, duration, rule, rangeStart, rangeEnd);
            case "MONTHLY" -> expandMonthly(event, start, duration, rule, rangeStart, rangeEnd);
            default -> {
                warnings.add("Unsupported RRULE frequency " + frequency + "; imported first occurrence only: "
                        + event.text("SUMMARY", "Untitled event"));
                IcsImportEventVO occurrence = toImportEvent(event, start.dateTime(), duration, start.allDay());
                yield isInRange(occurrence.getPlanDate(), rangeStart, rangeEnd) ? List.of(occurrence) : List.of();
            }
        };
    }

    private List<IcsImportEventVO> expandDaily(
            IcsEvent event,
            IcsDateTime start,
            Duration duration,
            Map<String, String> rule,
            LocalDate rangeStart,
            LocalDate rangeEnd
    ) {
        int interval = parsePositiveInt(rule.get("INTERVAL"), 1);
        Integer count = parseNullablePositiveInt(rule.get("COUNT"));
        LocalDate until = parseUntilDate(rule.get("UNTIL"), rangeEnd);
        LocalDate end = minDate(until, rangeEnd);
        List<IcsImportEventVO> events = new ArrayList<>();
        int generated = 0;

        for (LocalDate date = start.dateTime().toLocalDate(); !date.isAfter(end); date = date.plusDays(interval)) {
            generated++;
            if (generated > MAX_OCCURRENCES_PER_EVENT || (count != null && generated > count)) {
                break;
            }
            if (isInRange(date, rangeStart, rangeEnd)) {
                events.add(toImportEvent(event, withDate(start.dateTime(), date), duration, start.allDay()));
            }
        }
        return events;
    }

    private List<IcsImportEventVO> expandWeekly(
            IcsEvent event,
            IcsDateTime start,
            Duration duration,
            Map<String, String> rule,
            LocalDate rangeStart,
            LocalDate rangeEnd
    ) {
        int interval = parsePositiveInt(rule.get("INTERVAL"), 1);
        Integer count = parseNullablePositiveInt(rule.get("COUNT"));
        LocalDate until = parseUntilDate(rule.get("UNTIL"), rangeEnd);
        LocalDate end = minDate(until, rangeEnd);
        Set<DayOfWeek> days = parseByDay(rule.get("BYDAY"));
        if (days.isEmpty()) {
            days.add(start.dateTime().getDayOfWeek());
        }

        List<IcsImportEventVO> events = new ArrayList<>();
        int generated = 0;
        LocalDate startDate = start.dateTime().toLocalDate();
        LocalDate startWeek = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        for (LocalDate date = startDate; !date.isAfter(end); date = date.plusDays(1)) {
            if (!days.contains(date.getDayOfWeek())) {
                continue;
            }
            long weeks = ChronoUnit.WEEKS.between(startWeek, date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
            if (weeks % interval != 0) {
                continue;
            }
            generated++;
            if (generated > MAX_OCCURRENCES_PER_EVENT || (count != null && generated > count)) {
                break;
            }
            if (isInRange(date, rangeStart, rangeEnd)) {
                events.add(toImportEvent(event, withDate(start.dateTime(), date), duration, start.allDay()));
            }
        }
        return events;
    }

    private List<IcsImportEventVO> expandMonthly(
            IcsEvent event,
            IcsDateTime start,
            Duration duration,
            Map<String, String> rule,
            LocalDate rangeStart,
            LocalDate rangeEnd
    ) {
        int interval = parsePositiveInt(rule.get("INTERVAL"), 1);
        Integer count = parseNullablePositiveInt(rule.get("COUNT"));
        LocalDate until = parseUntilDate(rule.get("UNTIL"), rangeEnd);
        LocalDate end = minDate(until, rangeEnd);
        List<IcsImportEventVO> events = new ArrayList<>();
        int generated = 0;

        for (LocalDate date = start.dateTime().toLocalDate(); !date.isAfter(end); date = date.plusMonths(interval)) {
            generated++;
            if (generated > MAX_OCCURRENCES_PER_EVENT || (count != null && generated > count)) {
                break;
            }
            if (isInRange(date, rangeStart, rangeEnd)) {
                events.add(toImportEvent(event, withDate(start.dateTime(), date), duration, start.allDay()));
            }
        }
        return events;
    }

    private Duration resolveDuration(IcsEvent event, IcsDateTime start) {
        IcsDateTime end = parseDateTime(event.first("DTEND"));
        if (end != null) {
            Duration duration = Duration.between(start.dateTime(), end.dateTime());
            return duration.isNegative() ? Duration.ZERO : duration;
        }

        String durationValue = event.text("DURATION", "");
        if (StringUtils.hasText(durationValue)) {
            try {
                Duration duration = Duration.parse(durationValue);
                return duration.isNegative() ? Duration.ZERO : duration;
            } catch (RuntimeException ignored) {
                return Duration.ZERO;
            }
        }
        return Duration.ZERO;
    }

    private IcsImportEventVO toImportEvent(IcsEvent event, LocalDateTime start, Duration duration, boolean allDay) {
        IcsImportEventVO vo = new IcsImportEventVO();
        vo.setUid(event.text("UID", null));
        vo.setTitle(event.text("SUMMARY", "未命名日程"));
        vo.setLocation(event.text("LOCATION", null));
        vo.setDescription(buildDescription(event));
        vo.setPlanDate(start.toLocalDate());
        vo.setAllDay(allDay);
        vo.setSkipped(false);
        if (allDay) {
            vo.setStartTime(null);
            vo.setEndTime(null);
            vo.setPlannedMinutes(0);
        } else {
            vo.setStartTime(start.toLocalTime().truncatedTo(ChronoUnit.MINUTES));
            vo.setEndTime(duration.isZero() ? null : start.plus(duration).toLocalTime().truncatedTo(ChronoUnit.MINUTES));
            long minutes = duration.toMinutes();
            vo.setPlannedMinutes(minutes > 0 && minutes <= Integer.MAX_VALUE ? (int) minutes : 0);
        }
        return vo;
    }

    private String buildDescription(IcsEvent event) {
        List<String> lines = new ArrayList<>();
        String description = event.text("DESCRIPTION", null);
        String location = event.text("LOCATION", null);
        String uid = event.text("UID", null);
        if (StringUtils.hasText(description)) {
            lines.add(description);
        }
        if (StringUtils.hasText(location)) {
            lines.add("地点：" + location);
        }
        lines.add("来源：ICS 导入");
        if (StringUtils.hasText(uid)) {
            lines.add("ICS UID：" + uid);
        }
        return String.join("\n", lines);
    }

    private IcsDateTime parseDateTime(IcsProperty property) {
        if (property == null || !StringUtils.hasText(property.value())) {
            return null;
        }

        String value = property.value().trim();
        boolean allDay = "DATE".equalsIgnoreCase(property.params().get("VALUE")) || value.matches("\\d{8}");
        if (allDay) {
            LocalDate date = LocalDate.parse(value.substring(0, 8), BASIC_DATE);
            return new IcsDateTime(date.atStartOfDay(), true);
        }

        boolean utc = value.endsWith("Z");
        String normalized = utc ? value.substring(0, value.length() - 1) : value;
        LocalDateTime localDateTime = null;
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                localDateTime = LocalDateTime.parse(normalized, formatter);
                break;
            } catch (RuntimeException ignored) {
                // Try next formatter.
            }
        }
        if (localDateTime == null) {
            return null;
        }

        if (utc) {
            Instant instant = localDateTime.toInstant(ZoneOffset.UTC);
            return new IcsDateTime(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()), false);
        }

        String timeZoneId = property.params().get("TZID");
        if (StringUtils.hasText(timeZoneId)) {
            try {
                ZoneId sourceZone = ZoneId.of(timeZoneId);
                return new IcsDateTime(localDateTime.atZone(sourceZone)
                        .withZoneSameInstant(ZoneId.systemDefault())
                        .toLocalDateTime(), false);
            } catch (RuntimeException ignored) {
                return new IcsDateTime(localDateTime, false);
            }
        }
        return new IcsDateTime(localDateTime, false);
    }

    private Map<String, String> parseRRule(String value) {
        Map<String, String> rule = new LinkedHashMap<>();
        if (!StringUtils.hasText(value)) {
            return rule;
        }
        for (String part : value.split(";")) {
            int equalsIndex = part.indexOf('=');
            if (equalsIndex <= 0) {
                continue;
            }
            rule.put(part.substring(0, equalsIndex).trim().toUpperCase(Locale.ROOT),
                    part.substring(equalsIndex + 1).trim());
        }
        return rule;
    }

    private Set<DayOfWeek> parseByDay(String value) {
        Set<DayOfWeek> days = new HashSet<>();
        if (!StringUtils.hasText(value)) {
            return days;
        }
        for (String item : value.split(",")) {
            String normalized = item.replaceAll("[-+]?\\d+", "").trim().toUpperCase(Locale.ROOT);
            DayOfWeek day = switch (normalized) {
                case "MO" -> DayOfWeek.MONDAY;
                case "TU" -> DayOfWeek.TUESDAY;
                case "WE" -> DayOfWeek.WEDNESDAY;
                case "TH" -> DayOfWeek.THURSDAY;
                case "FR" -> DayOfWeek.FRIDAY;
                case "SA" -> DayOfWeek.SATURDAY;
                case "SU" -> DayOfWeek.SUNDAY;
                default -> null;
            };
            if (day != null) {
                days.add(day);
            }
        }
        return days;
    }

    private LocalDate parseUntilDate(String value, LocalDate fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String normalized = value.trim();
        try {
            if (normalized.matches("\\d{8}")) {
                return LocalDate.parse(normalized, BASIC_DATE);
            }
            IcsDateTime dateTime = parseDateTime(new IcsProperty("UNTIL", Map.of(), normalized));
            return dateTime == null ? fallback : dateTime.dateTime().toLocalDate();
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private TaskCreateRequest toTaskCreateRequest(IcsImportEventVO event) {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle(event.getTitle());
        request.setDescription(event.getDescription());
        request.setPlanDate(event.getPlanDate());
        request.setStartTime(event.getStartTime());
        request.setEndTime(event.getEndTime());
        request.setPlannedMinutes(event.getPlannedMinutes() == null ? 0 : Math.max(0, event.getPlannedMinutes()));
        request.setStatus(0);
        return request;
    }

    private boolean existsSameTask(Long userId, IcsImportEventVO event) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .eq(Task::getTitle, event.getTitle())
                .eq(Task::getPlanDate, event.getPlanDate());
        if (event.getStartTime() == null) {
            wrapper.isNull(Task::getStartTime);
        } else {
            wrapper.eq(Task::getStartTime, event.getStartTime());
        }
        if (event.getEndTime() == null) {
            wrapper.isNull(Task::getEndTime);
        } else {
            wrapper.eq(Task::getEndTime, event.getEndTime());
        }
        return taskService.count(wrapper) > 0;
    }

    private boolean isInRange(LocalDate date, LocalDate start, LocalDate end) {
        return date != null && !date.isBefore(start) && !date.isAfter(end);
    }

    private LocalDateTime withDate(LocalDateTime source, LocalDate date) {
        return LocalDateTime.of(date, source.toLocalTime());
    }

    private LocalDate minDate(LocalDate first, LocalDate second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isBefore(second) ? first : second;
    }

    private int parsePositiveInt(String value, int fallback) {
        Integer parsed = parseNullablePositiveInt(value);
        return parsed == null ? fallback : parsed;
    }

    private Integer parseNullablePositiveInt(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String unescapeText(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\N", "\n")
                .replace("\\,", ",")
                .replace("\\;", ";")
                .replace("\\\\", "\\");
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private record IcsProperty(String name, Map<String, String> params, String value) {
    }

    private record IcsDateTime(LocalDateTime dateTime, boolean allDay) {
    }

    private static class IcsEvent {

        private final Map<String, List<IcsProperty>> properties = new LinkedHashMap<>();

        private void add(IcsProperty property) {
            properties.computeIfAbsent(property.name(), ignored -> new ArrayList<>()).add(property);
        }

        private IcsProperty first(String name) {
            List<IcsProperty> values = properties.get(name);
            return values == null || values.isEmpty() ? null : values.get(0);
        }

        private String text(String name, String fallback) {
            IcsProperty property = first(name);
            if (property == null || !StringUtils.hasText(property.value())) {
                return fallback;
            }
            return property.value();
        }
    }
}
