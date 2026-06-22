package com.example.goalbot.service;

import com.example.goalbot.dto.conversation.ConversationTurn;
import com.example.goalbot.dto.conversation.DialogueState;
import com.example.goalbot.entity.ConversationMessage;

import java.util.List;

public interface ConversationStateService {

    ConversationTurn beginTurn(Long userId, String channel, String messageId, String content);

    void finishTurn(ConversationTurn turn, String intent, String replyContent);

    void updateState(Long sessionId, DialogueState state, String topic);

    List<ConversationMessage> listRecentMessages(Long userId, String channel, int limit);
}
