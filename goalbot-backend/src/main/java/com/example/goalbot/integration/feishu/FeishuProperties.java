package com.example.goalbot.integration.feishu;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "goalbot.feishu")
public class FeishuProperties {

    private String appId;

    private String appSecret;

    private String verificationToken;

    private String encryptKey;

    private String defaultChatId;

    private boolean longConnectionEnabled = false;
}
