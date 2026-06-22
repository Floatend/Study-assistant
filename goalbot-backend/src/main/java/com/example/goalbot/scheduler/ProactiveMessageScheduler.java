package com.example.goalbot.scheduler;

import com.example.goalbot.entity.Notification;
import com.example.goalbot.service.AssistantSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProactiveMessageScheduler {

    private final AssistantSettingsService assistantSettingsService;

    @Scheduled(cron = "0 * * * * ?", zone = "Asia/Shanghai")
    public void dispatchDueMessages() {
        List<Notification> notifications = assistantSettingsService.sendDueProactiveMessages(LocalDateTime.now());
        if (!notifications.isEmpty()) {
            log.info("Dispatched {} proactive message(s).", notifications.size());
        }
    }
}
