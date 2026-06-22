package com.example.goalbot.agent;

import lombok.Data;

@Data
public class UserMessage {

    private Long userId;

    private String channel;

    private String messageId;

    private String text;
}
