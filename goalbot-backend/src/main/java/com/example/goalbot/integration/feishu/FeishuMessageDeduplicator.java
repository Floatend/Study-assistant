package com.example.goalbot.integration.feishu;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FeishuMessageDeduplicator {

    private static final long TTL_MILLIS = Duration.ofMinutes(10).toMillis();
    private static final int CLEANUP_THRESHOLD = 500;

    private final Map<String, Long> messageExpiresAt = new ConcurrentHashMap<>();

    public boolean markIfNew(String messageId) {
        if (!StringUtils.hasText(messageId)) {
            return true;
        }

        long now = System.currentTimeMillis();
        cleanupIfNeeded(now);
        final boolean[] accepted = {false};
        messageExpiresAt.compute(messageId, (key, previousExpireAt) -> {
            if (previousExpireAt == null || previousExpireAt <= now) {
                accepted[0] = true;
                return now + TTL_MILLIS;
            }
            return previousExpireAt;
        });
        return accepted[0];
    }

    private void cleanupIfNeeded(long now) {
        if (messageExpiresAt.size() < CLEANUP_THRESHOLD) {
            return;
        }

        Iterator<Map.Entry<String, Long>> iterator = messageExpiresAt.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getValue() <= now) {
                iterator.remove();
            }
        }
    }
}
