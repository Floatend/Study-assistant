package com.example.goalbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "goalbot.reminder")
public class ReminderProperties {

    private boolean enabled = true;
}
