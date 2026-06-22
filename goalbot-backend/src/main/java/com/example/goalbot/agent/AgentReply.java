package com.example.goalbot.agent;

import com.example.goalbot.dto.command.CommandIntent;
import lombok.Data;

@Data
public class AgentReply {

    private String content;

    private CommandIntent intent;

    private String tool;

    private boolean success = true;

    private String errorMessage;

    public static AgentReply ok(String content, CommandIntent intent, String tool) {
        AgentReply reply = new AgentReply();
        reply.setContent(content);
        reply.setIntent(intent);
        reply.setTool(tool);
        reply.setSuccess(true);
        return reply;
    }

    public static AgentReply failed(String content, CommandIntent intent, String tool, String errorMessage) {
        AgentReply reply = ok(content, intent, tool);
        reply.setSuccess(false);
        reply.setErrorMessage(errorMessage);
        return reply;
    }
}
