package com.example.goalbot.integration.feishu;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FeishuEventHandler {

    public Map<String, Object> handleEvent(Map<String, Object> payload) {
        Object challenge = payload.get("challenge");
        if (challenge != null) {
            return Map.of("challenge", challenge);
        }

        // The full Feishu app event schema and reply API will be wired after app credentials are configured.
        return Map.of("code", 0, "message", "event accepted");
    }
}
