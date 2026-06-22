package com.example.goalbot.integration.dify;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class DifyWorkflowResponse {

    @JsonProperty("workflow_run_id")
    private String workflowRunId;

    @JsonProperty("task_id")
    private String taskId;

    private WorkflowData data;

    @Data
    public static class WorkflowData {

        private String id;

        private String status;

        private Map<String, Object> outputs;

        private String error;
    }
}
