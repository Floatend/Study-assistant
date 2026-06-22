package com.example.goalbot.service;

import com.example.goalbot.dto.command.CommandIntent;
import com.example.goalbot.dto.conversation.ConversationTurn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationOrchestrator {

    private static final String FEISHU_CHANNEL = "FEISHU";

    private final ConversationStateService conversationStateService;

    public ConversationTurn beginFeishuTurn(Long userId, String feishuMessageId, String content) {
        return conversationStateService.beginTurn(userId, FEISHU_CHANNEL, feishuMessageId, content);
    }

    public void finishTurn(ConversationTurn turn, CommandIntent intent, String replyContent) {
        String intentName = intent == null || intent.getIntent() == null ? null : intent.getIntent().name();
        conversationStateService.finishTurn(turn, intentName, replyContent);
    }
}
