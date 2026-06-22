package com.example.goalbot.agent.tool;

import com.example.goalbot.agent.AgentTool;
import com.example.goalbot.agent.ToolCall;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public abstract class AbstractAgentTool implements AgentTool {

    protected String stringArg(ToolCall call, String key) {
        Object value = call == null ? null : call.arg(key);
        if (value == null) {
            return null;
        }
        String text = Objects.toString(value, "").trim();
        return StringUtils.hasText(text) && !"null".equalsIgnoreCase(text) ? text : null;
    }

    protected Long longArg(ToolCall call, String key) {
        Object value = call == null ? null : call.arg(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = value == null ? null : Objects.toString(value, "").trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    protected Integer intArg(ToolCall call, String key) {
        Object value = call == null ? null : call.arg(key);
        if (value instanceof Number number) {
            return Math.max(0, (int) Math.round(number.doubleValue()));
        }
        String text = value == null ? null : Objects.toString(value, "").trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Math.max(0, (int) Math.round(Double.parseDouble(text.replaceAll("[^0-9.]", ""))));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    protected LocalDate dateArg(ToolCall call, String key) {
        String text = stringArg(call, key);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return LocalDate.parse(text.length() > 10 ? text.substring(0, 10) : text);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    protected LocalTime timeArg(ToolCall call, String key) {
        String text = stringArg(call, key);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return LocalTime.parse(text.length() > 5 ? text.substring(0, 5) : text);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    protected String dash(String value) {
        return StringUtils.hasText(value) ? value : "无";
    }

    protected int zero(Integer value) {
        return value == null ? 0 : value;
    }
}
