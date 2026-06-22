package com.example.goalbot.agent;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ToolCall {

    private String tool;

    private Map<String, Object> arguments = new LinkedHashMap<>();

    private List<String> missingSlots = List.of();

    private Boolean requiresConfirmation = false;

    private Double confidence = 0.0;

    public Object arg(String key) {
        return arguments == null ? null : arguments.get(key);
    }
}
