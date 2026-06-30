package com.example.goalbot.service.impl;

import com.example.goalbot.agent.plan.AgentPlan;
import com.example.goalbot.agent.plan.PlannerRunMode;
import com.example.goalbot.entity.AgentPlanLog;
import com.example.goalbot.mapper.AgentPlanLogMapper;
import com.example.goalbot.service.AgentPlanLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentPlanLogServiceImpl implements AgentPlanLogService {

    private final AgentPlanLogMapper agentPlanLogMapper;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean schemaWarningLogged = new AtomicBoolean(false);

    @Override
    public void record(
            Long userId,
            Long sessionId,
            String messageId,
            PlannerRunMode runMode,
            boolean selected,
            AgentPlan plan
    ) {
        log.info("agent_plan userId={} sessionId={} runMode={} selected={} mode={} confidence={} tool={} source={}",
                userId,
                sessionId,
                runMode,
                selected,
                plan == null ? null : plan.getMode(),
                plan == null ? null : plan.getConfidence(),
                plan == null ? null : plan.primaryTool(),
                plan == null ? null : plan.getSource());

        AgentPlanLog record = new AgentPlanLog();
        record.setUserId(userId);
        record.setSessionId(sessionId);
        record.setMessageId(messageId);
        record.setRunMode(runMode == null ? null : runMode.name());
        record.setSelected(selected ? 1 : 0);
        record.setPlanMode(plan == null || plan.getMode() == null ? null : plan.getMode().name());
        record.setConfidence(plan == null ? null : plan.getConfidence());
        record.setPrimaryTool(plan == null ? null : plan.primaryTool());
        record.setPlanJson(toJson(plan));
        record.setErrorMessage(plan == null ? "Planner returned null" : plan.getErrorMessage());
        try {
            agentPlanLogMapper.insert(record);
        } catch (DataAccessException ex) {
            if (schemaWarningLogged.compareAndSet(false, true)) {
                log.warn("Agent plan persistence is unavailable. Run sql/agent_plan_log.sql. reason={}", ex.getMessage());
            }
        }
    }

    private String toJson(AgentPlan plan) {
        if (plan == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(plan);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize agent plan: {}", ex.getMessage());
            return null;
        }
    }
}
