package com.example.goalbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "site.auth")
public class AuthProperties {

    private int sessionDays = 30;

    private String bootstrapAdminUsername = "local_user";

    private String bootstrapAdminPassword;
}
