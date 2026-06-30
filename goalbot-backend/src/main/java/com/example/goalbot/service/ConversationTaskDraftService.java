package com.example.goalbot.service;

import com.example.goalbot.entity.ConversationTaskDraft;

import java.util.List;
import java.util.Optional;

public interface ConversationTaskDraftService {

    Optional<ConversationTaskDraft> getActiveDraft(Long userId);

    List<ConversationTaskDraft> listActiveDrafts(Long userId);

    ConversationTaskDraft saveActiveDraft(Long userId, Long sessionId, ConversationTaskDraft draft);

    List<ConversationTaskDraft> enqueueDrafts(Long userId, Long sessionId, List<ConversationTaskDraft> drafts);

    void completeActiveDraft(Long userId);

    void cancelActiveDraft(Long userId);
}
