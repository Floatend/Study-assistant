package com.example.goalbot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.goalbot.dto.conversation.ConversationTurn;
import com.example.goalbot.dto.conversation.DialogueState;
import com.example.goalbot.entity.ConversationMessage;
import com.example.goalbot.entity.ConversationSession;
import com.example.goalbot.mapper.ConversationMessageMapper;
import com.example.goalbot.mapper.ConversationSessionMapper;
import com.example.goalbot.service.ConversationStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationStateServiceImpl implements ConversationStateService {

    private static final int SESSION_ACTIVE = 0;
    private static final Duration SESSION_TTL = Duration.ofHours(2);

    private final ConversationSessionMapper conversationSessionMapper;
    private final ConversationMessageMapper conversationMessageMapper;

    @Override
    @Transactional
    public ConversationTurn beginTurn(Long userId, String channel, String messageId, String content) {
        LocalDateTime now = LocalDateTime.now();
        ConversationSession session = findActiveSession(userId, channel, now);
        if (session == null) {
            session = new ConversationSession();
            session.setUserId(userId);
            session.setChannel(channel);
            session.setStatus(SESSION_ACTIVE);
            session.setTopic("chat");
            session.setState(DialogueState.IDLE.name());
            session.setExpiresAt(now.plus(SESSION_TTL));
            conversationSessionMapper.insert(session);
        } else {
            session.setExpiresAt(now.plus(SESSION_TTL));
            conversationSessionMapper.updateById(session);
        }

        ConversationMessage inbound = new ConversationMessage();
        inbound.setSessionId(session.getId());
        inbound.setUserId(userId);
        inbound.setChannel(channel);
        inbound.setDirection("IN");
        inbound.setMessageId(StringUtils.hasText(messageId) ? messageId : null);
        inbound.setContent(content);
        conversationMessageMapper.insert(inbound);

        return new ConversationTurn(session.getId(), inbound.getId(), channel);
    }

    @Override
    @Transactional
    public void finishTurn(ConversationTurn turn, String intent, String replyContent) {
        if (turn == null || turn.getSessionId() == null) {
            return;
        }

        ConversationSession session = conversationSessionMapper.selectById(turn.getSessionId());
        if (session != null) {
            session.setLastIntent(intent);
            session.setExpiresAt(LocalDateTime.now().plus(SESSION_TTL));
            conversationSessionMapper.updateById(session);
        }

        if (StringUtils.hasText(replyContent)) {
            ConversationMessage outbound = new ConversationMessage();
            outbound.setSessionId(turn.getSessionId());
            outbound.setChannel(turn.getChannel());
            outbound.setDirection("OUT");
            outbound.setContent(replyContent);
            outbound.setIntent(intent);
            if (session != null) {
                outbound.setUserId(session.getUserId());
            }
            conversationMessageMapper.insert(outbound);
        }
    }

    @Override
    @Transactional
    public void updateState(Long sessionId, DialogueState state, String topic) {
        if (sessionId == null || state == null) {
            return;
        }
        ConversationSession session = conversationSessionMapper.selectById(sessionId);
        if (session == null) {
            return;
        }
        session.setState(state.name());
        if (StringUtils.hasText(topic)) {
            session.setTopic(topic);
        }
        session.setExpiresAt(LocalDateTime.now().plus(SESSION_TTL));
        conversationSessionMapper.updateById(session);
    }

    @Override
    public List<ConversationMessage> listRecentMessages(Long userId, String channel, int limit) {
        if (userId == null || !StringUtils.hasText(channel) || limit <= 0) {
            return List.of();
        }
        List<ConversationMessage> messages = conversationMessageMapper.selectList(new LambdaQueryWrapper<ConversationMessage>()
                .eq(ConversationMessage::getUserId, userId)
                .eq(ConversationMessage::getChannel, channel)
                .orderByDesc(ConversationMessage::getCreatedAt)
                .orderByDesc(ConversationMessage::getId)
                .last("LIMIT " + Math.min(limit, 20)));
        Collections.reverse(messages);
        return messages;
    }

    private ConversationSession findActiveSession(Long userId, String channel, LocalDateTime now) {
        return conversationSessionMapper.selectOne(new LambdaQueryWrapper<ConversationSession>()
                .eq(ConversationSession::getUserId, userId)
                .eq(ConversationSession::getChannel, channel)
                .eq(ConversationSession::getStatus, SESSION_ACTIVE)
                .ge(ConversationSession::getExpiresAt, now)
                .orderByDesc(ConversationSession::getUpdatedAt)
                .last("LIMIT 1"));
    }
}
