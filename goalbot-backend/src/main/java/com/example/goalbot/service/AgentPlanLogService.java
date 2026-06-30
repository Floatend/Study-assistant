package com.example.goalbot.service;

import com.example.goalbot.agent.plan.AgentPlan;
import com.example.goalbot.agent.plan.PlannerRunMode;

public interface AgentPlanLogService {

    void record(
            Long userId,
            Long sessionId,
            String messageId,
            PlannerRunMode runMode,
            boolean selected,
            AgentPlan plan
    );
}
