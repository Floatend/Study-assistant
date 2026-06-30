package com.example.goalbot.integration.dify;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "goalbot.dify")
public class DifyProperties {

    private boolean enabled = true;

    private String apiUrl;

    private String apiKey;

    private String workflowApiUrl;

    private String workflowApiKey;

    private String plannerApiUrl;

    private String plannerApiKey;

    private int timeoutSeconds = 60;
}
