package com.example.goalbot.integration.dify;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DifyWorkflowRequest {

    private Map<String, Object> inputs;

    @JsonProperty("response_mode")
    private String responseMode;

    private String user;
}
