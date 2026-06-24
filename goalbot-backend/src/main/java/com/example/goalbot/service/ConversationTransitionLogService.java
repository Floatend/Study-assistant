package com.example.goalbot.service;

import com.example.goalbot.agent.dialogue.TaskDraftTransition;
import com.example.goalbot.entity.ConversationTaskDraft;

public interface ConversationTransitionLogService {

    void recordTaskDraftTransition(
            Long userId,
            ConversationTaskDraft draft,
            String transitionType,
            TaskDraftTransition transition
    );
}
