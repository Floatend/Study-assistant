package com.example.goalbot.service.impl;

import com.example.goalbot.agent.dialogue.TaskDraftSnapshot;
import com.example.goalbot.agent.dialogue.TaskDraftTransition;
import com.example.goalbot.entity.ConversationTaskDraft;
import com.example.goalbot.entity.ConversationTransitionLog;
import com.example.goalbot.mapper.ConversationTransitionLogMapper;
import com.example.goalbot.service.ConversationTransitionLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationTransitionLogServiceImpl implements ConversationTransitionLogService {

    private final ConversationTransitionLogMapper transitionLogMapper;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean schemaWarningLogged = new AtomicBoolean(false);

    @Override
    public void recordTaskDraftTransition(
            Long userId,
            ConversationTaskDraft draft,
            String transitionType,
            TaskDraftTransition transition
    ) {
        if (transition == null) {
            return;
        }

        List<String> changedSlots = changedSlots(transition.getBefore(), transition.getAfter());
        log.info("dialogue_transition userId={} sessionId={} draftId={} type={} decision={} changedSlots={}",
                userId,
                draft == null ? null : draft.getSessionId(),
                draft == null ? null : draft.getId(),
                transitionType,
                transition.getDecision(),
                changedSlots);

        ConversationTransitionLog record = new ConversationTransitionLog();
        record.setSessionId(draft == null ? null : draft.getSessionId());
        record.setUserId(userId);
        record.setDraftId(draft == null ? null : draft.getId());
        record.setTransitionType(transitionType);
        record.setRawText(transition.getRawText());
        record.setStateBefore(toJson(transition.getBefore()));
        record.setSemanticFrame(toJson(transition.getFrame()));
        record.setStateAfter(toJson(transition.getAfter()));
        record.setDecision(transition.getDecision() == null ? null : transition.getDecision().name());
        record.setClarificationQuestion(transition.getClarificationQuestion());

        try {
            transitionLogMapper.insert(record);
        } catch (DataAccessException ex) {
            if (schemaWarningLogged.compareAndSet(false, true)) {
                log.warn("Conversation transition persistence is unavailable. Run sql/conversation_transition_log.sql. reason={}",
                        ex.getMessage());
            }
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize conversation transition state: {}", ex.getMessage());
            return null;
        }
    }

    private List<String> changedSlots(TaskDraftSnapshot before, TaskDraftSnapshot after) {
        if (after == null) {
            return List.of();
        }
        List<String> changed = new ArrayList<>();
        if (before == null || !Objects.equals(before.planDate(), after.planDate())) {
            changed.add("plan_date");
        }
        if (before == null || !Objects.equals(before.startTime(), after.startTime())) {
            changed.add("start_time");
        }
        if (before == null || !Objects.equals(before.endTime(), after.endTime())) {
            changed.add("end_time");
        }
        if (before == null || !Objects.equals(before.plannedMinutes(), after.plannedMinutes())) {
            changed.add("planned_minutes");
        }
        if (before == null || !Objects.equals(before.missingSlots(), after.missingSlots())) {
            changed.add("missing_slots");
        }
        return changed;
    }
}
