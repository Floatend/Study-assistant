package com.example.goalbot.agent.plan;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class AgentAction {

    private String actionId;
    private String tool;
    private AgentTarget target;
    private Map<String, Object> arguments = new LinkedHashMap<>();
    private List<String> missingSlots = List.of();
    private Boolean requiresConfirmation = false;
}
