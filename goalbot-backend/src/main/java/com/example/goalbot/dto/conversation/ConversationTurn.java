package com.example.goalbot.dto.conversation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationTurn {

    private Long sessionId;

    private Long inboundMessageId;

    private String channel;
}
