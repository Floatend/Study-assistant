package com.example.goalbot.service;

import com.example.goalbot.entity.ConversationTaskDraft;

import java.util.Optional;

public interface ConversationTaskDraftService {

    Optional<ConversationTaskDraft> getActiveDraft(Long userId);

    ConversationTaskDraft saveActiveDraft(Long userId, Long sessionId, ConversationTaskDraft draft);

    void completeActiveDraft(Long userId);

    void cancelActiveDraft(Long userId);
}
