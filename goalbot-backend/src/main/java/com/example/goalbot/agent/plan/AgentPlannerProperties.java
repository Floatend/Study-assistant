package com.example.goalbot.agent.plan;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "goalbot.agent.planner")
public class AgentPlannerProperties {

    private PlannerRunMode mode = PlannerRunMode.OFF;
    private double minConfidence = 0.72;
    private int maxActions = 8;
}
