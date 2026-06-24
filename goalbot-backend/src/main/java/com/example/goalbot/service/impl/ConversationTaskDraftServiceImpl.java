package com.example.goalbot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.goalbot.dto.conversation.DialogueState;
import com.example.goalbot.entity.ConversationTaskDraft;
import com.example.goalbot.mapper.ConversationTaskDraftMapper;
import com.example.goalbot.service.ConversationStateService;
import com.example.goalbot.service.ConversationTaskDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConversationTaskDraftServiceImpl implements ConversationTaskDraftService {

    private static final int DRAFT_COLLECTING = 0;
    private static final int DRAFT_COMPLETED = 1;
    private static final int DRAFT_CANCELLED = 2;
    private static final int DRAFT_EXPIRED = 3;
    private static final Duration DRAFT_TTL = Duration.ofMinutes(30);

    private final ConversationTaskDraftMapper conversationTaskDraftMapper;
    private final ConversationStateService conversationStateService;

    @Override
    @Transactional
    public Optional<ConversationTaskDraft> getActiveDraft(Long userId) {
        expireStaleDrafts(userId);
        return Optional.ofNullable(conversationTaskDraftMapper.selectOne(new LambdaQueryWrapper<ConversationTaskDraft>()
                .eq(ConversationTaskDraft::getUserId, userId)
                .eq(ConversationTaskDraft::getStatus, DRAFT_COLLECTING)
                .ge(ConversationTaskDraft::getExpiresAt, LocalDateTime.now())
                .orderByAsc(ConversationTaskDraft::getId)
                .last("LIMIT 1")));
    }

    @Override
    @Transactional
    public ConversationTaskDraft saveActiveDraft(Long userId, Long sessionId, ConversationTaskDraft draft) {
        expireStaleDrafts(userId);
        LocalDateTime expiresAt = LocalDateTime.now().plus(DRAFT_TTL);
        ConversationTaskDraft active = getActiveDraft(userId).orElse(null);
        if (active == null) {
            draft.setUserId(userId);
            draft.setSessionId(sessionId);
            draft.setStatus(DRAFT_COLLECTING);
            draft.setExpiresAt(expiresAt);
            conversationTaskDraftMapper.insert(draft);
            conversationStateService.updateState(sessionId, DialogueState.COLLECTING_TASK, "CREATE_TASK");
            return draft;
        }

        active.setSessionId(active.getSessionId() == null ? sessionId : active.getSessionId());
        active.setTitle(draft.getTitle());
        active.setDescription(draft.getDescription());
        active.setPlanDate(draft.getPlanDate());
        active.setStartTime(draft.getStartTime());
        active.setEndTime(draft.getEndTime());
        active.setPlannedMinutes(draft.getPlannedMinutes());
        active.setGoalId(draft.getGoalId());
        active.setGoalKeyword(draft.getGoalKeyword());
        active.setMissingSlots(draft.getMissingSlots());
        active.setSourceText(draft.getSourceText());
        active.setExpiresAt(expiresAt);
        conversationTaskDraftMapper.updateById(active);
        conversationTaskDraftMapper.update(null, new LambdaUpdateWrapper<ConversationTaskDraft>()
                .eq(ConversationTaskDraft::getUserId, userId)
                .eq(ConversationTaskDraft::getStatus, DRAFT_COLLECTING)
                .set(ConversationTaskDraft::getExpiresAt, expiresAt));
        conversationStateService.updateState(active.getSessionId(), DialogueState.COLLECTING_TASK, "CREATE_TASK");
        return active;
    }

    @Override
    @Transactional
    public List<ConversationTaskDraft> enqueueDrafts(
            Long userId,
            Long sessionId,
            List<ConversationTaskDraft> drafts
    ) {
        if (drafts == null || drafts.isEmpty()) {
            return List.of();
        }
        expireStaleDrafts(userId);
        LocalDateTime expiresAt = LocalDateTime.now().plus(DRAFT_TTL);
        List<ConversationTaskDraft> saved = new ArrayList<>(drafts.size());
        for (ConversationTaskDraft draft : drafts) {
            draft.setId(null);
            draft.setUserId(userId);
            draft.setSessionId(sessionId);
            draft.setStatus(DRAFT_COLLECTING);
            draft.setExpiresAt(expiresAt);
            conversationTaskDraftMapper.insert(draft);
            saved.add(draft);
        }
        conversationStateService.updateState(sessionId, DialogueState.COLLECTING_TASK, "CREATE_TASK");
        return List.copyOf(saved);
    }

    @Override
    @Transactional
    public void completeActiveDraft(Long userId) {
        ConversationTaskDraft active = getActiveDraft(userId).orElse(null);
        if (active == null) {
            return;
        }
        active.setStatus(DRAFT_COMPLETED);
        conversationTaskDraftMapper.updateById(active);
        updateStateAfterCurrentDraft(active.getUserId(), active.getSessionId());
    }

    @Override
    @Transactional
    public void cancelActiveDraft(Long userId) {
        ConversationTaskDraft active = getActiveDraft(userId).orElse(null);
        if (active == null) {
            return;
        }
        active.setStatus(DRAFT_CANCELLED);
        conversationTaskDraftMapper.updateById(active);
        updateStateAfterCurrentDraft(active.getUserId(), active.getSessionId());
    }

    private void updateStateAfterCurrentDraft(Long userId, Long sessionId) {
        boolean hasNext = getActiveDraft(userId).isPresent();
        conversationStateService.updateState(
                sessionId,
                hasNext ? DialogueState.COLLECTING_TASK : DialogueState.IDLE,
                hasNext ? "CREATE_TASK" : null
        );
    }

    private void expireStaleDrafts(Long userId) {
        conversationTaskDraftMapper.update(null, new LambdaUpdateWrapper<ConversationTaskDraft>()
                .eq(ConversationTaskDraft::getUserId, userId)
                .eq(ConversationTaskDraft::getStatus, DRAFT_COLLECTING)
                .lt(ConversationTaskDraft::getExpiresAt, LocalDateTime.now())
                .set(ConversationTaskDraft::getStatus, DRAFT_EXPIRED));
    }
}
