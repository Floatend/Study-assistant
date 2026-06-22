package com.example.goalbot.integration.feishu;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.regex.Pattern;

public final class FeishuMessageTextExtractor {

    private static final Pattern AT_TAG_PATTERN = Pattern.compile("<at[^>]*>.*?</at>");

    private FeishuMessageTextExtractor() {
    }

    public static String extractText(String content, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(content, new TypeReference<>() {
            });
            Object text = parsed.get("text");
            return cleanup(text == null ? "" : String.valueOf(text));
        } catch (Exception ignored) {
            return cleanup(content);
        }
    }

    private static String cleanup(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return AT_TAG_PATTERN.matcher(text)
                .replaceAll("")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
