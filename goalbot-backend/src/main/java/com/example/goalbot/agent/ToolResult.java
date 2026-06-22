package com.example.goalbot.agent;

import lombok.Data;

@Data
public class ToolResult {

    private boolean success;

    private String message;

    private Object data;

    public static ToolResult ok(String message) {
        ToolResult result = new ToolResult();
        result.setSuccess(true);
        result.setMessage(message);
        return result;
    }

    public static ToolResult ok(String message, Object data) {
        ToolResult result = ok(message);
        result.setData(data);
        return result;
    }

    public static ToolResult failed(String message) {
        ToolResult result = new ToolResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
}
