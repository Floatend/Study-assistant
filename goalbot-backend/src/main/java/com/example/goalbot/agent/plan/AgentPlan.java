package com.example.goalbot.agent.plan;

import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.List;

@Data
public class AgentPlan {

    private AgentPlanMode mode = AgentPlanMode.UNKNOWN;
    private List<AgentAction> actions = List.of();
    private Double confidence = 0.0;
    private Boolean requiresConfirmation = false;
    private String clarifyingQuestion;
    private String assistantReply;
    private String source;
    private String errorMessage;

    public static AgentPlan unknown(String source, String errorMessage) {
        AgentPlan plan = new AgentPlan();
        plan.setSource(source);
        plan.setErrorMessage(errorMessage);
        return plan;
    }

    public boolean isUsable(double minConfidence) {
        if (mode == null || mode == AgentPlanMode.UNKNOWN || confidence == null || confidence < minConfidence) {
            return false;
        }
        return switch (mode) {
            case TOOL -> actions != null && !actions.isEmpty();
            case CLARIFY -> StringUtils.hasText(clarifyingQuestion);
            case CHAT -> true;
            case UNKNOWN -> false;
        };
    }

    public String primaryTool() {
        if (actions == null || actions.isEmpty()) {
            return null;
        }
        return actions.get(0).getTool();
    }
}
