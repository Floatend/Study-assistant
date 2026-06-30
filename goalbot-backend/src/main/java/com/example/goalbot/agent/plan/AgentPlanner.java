package com.example.goalbot.agent.plan;

import com.example.goalbot.entity.ConversationTaskDraft;

public interface AgentPlanner {

    AgentPlan plan(Long userId, String channel, String text, ConversationTaskDraft activeDraft);
}
