package com.example.goalbot.integration.dify;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DifyClient {

    private final DifyProperties properties;
    private final RestTemplate restTemplate;

    public boolean isConfigured() {
        return properties.isEnabled()
                && StringUtils.hasText(properties.getApiUrl())
                && StringUtils.hasText(properties.getApiKey());
    }

    public boolean isWorkflowConfigured() {
        return properties.isEnabled()
                && StringUtils.hasText(resolveWorkflowBaseUrl())
                && StringUtils.hasText(resolveWorkflowApiKey());
    }

    public String chat(String query, Map<String, Object> inputs, String user) {
        if (!isConfigured()) {
            throw new DifyException("Dify chat is not configured");
        }

        DifyChatRequest request = new DifyChatRequest(
                inputs == null ? Map.of() : inputs,
                query,
                "blocking",
                user
        );

        try {
            DifyChatResponse response = restTemplate.postForObject(
                    resolveChatUrl(),
                    new HttpEntity<>(request, headers(properties.getApiKey())),
                    DifyChatResponse.class
            );
            if (response == null || !StringUtils.hasText(response.getAnswer())) {
                throw new DifyException("Dify chat response is empty");
            }
            return response.getAnswer();
        } catch (HttpStatusCodeException ex) {
            throw new DifyException("Dify chat request failed: HTTP "
                    + ex.getStatusCode().value()
                    + " "
                    + truncate(ex.getResponseBodyAsString()), ex);
        } catch (ResourceAccessException ex) {
            throw new DifyException("Dify chat request failed: network or timeout - "
                    + truncate(ex.getMessage()), ex);
        } catch (RestClientException ex) {
            throw new DifyException("Dify chat request failed: " + truncate(ex.getMessage()), ex);
        }
    }

    public Map<String, Object> runWorkflow(Map<String, Object> inputs, String user) {
        if (!isWorkflowConfigured()) {
            throw new DifyException("Dify workflow is not configured");
        }

        DifyWorkflowRequest request = new DifyWorkflowRequest(
                inputs == null ? Map.of() : inputs,
                "blocking",
                user
        );

        try {
            DifyWorkflowResponse response = restTemplate.postForObject(
                    resolveWorkflowUrl(),
                    new HttpEntity<>(request, headers(resolveWorkflowApiKey())),
                    DifyWorkflowResponse.class
            );
            if (response == null || response.getData() == null) {
                throw new DifyException("Dify workflow response is empty");
            }
            if (StringUtils.hasText(response.getData().getError())) {
                throw new DifyException("Dify workflow failed: " + response.getData().getError());
            }
            return response.getData().getOutputs() == null ? Map.of() : response.getData().getOutputs();
        } catch (HttpStatusCodeException ex) {
            throw new DifyException("Dify workflow request failed: HTTP "
                    + ex.getStatusCode().value()
                    + " "
                    + truncate(ex.getResponseBodyAsString()), ex);
        } catch (ResourceAccessException ex) {
            throw new DifyException("Dify workflow request failed: network or timeout - "
                    + truncate(ex.getMessage()), ex);
        } catch (RestClientException ex) {
            throw new DifyException("Dify workflow request failed: " + truncate(ex.getMessage()), ex);
        }
    }

    private HttpHeaders headers(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    private String resolveChatUrl() {
        return appendPath(properties.getApiUrl(), "chat-messages");
    }

    private String resolveWorkflowUrl() {
        return appendPath(resolveWorkflowBaseUrl(), "workflows/run");
    }

    private String resolveWorkflowBaseUrl() {
        return StringUtils.hasText(properties.getWorkflowApiUrl())
                ? properties.getWorkflowApiUrl()
                : properties.getApiUrl();
    }

    private String resolveWorkflowApiKey() {
        return StringUtils.hasText(properties.getWorkflowApiKey())
                ? properties.getWorkflowApiKey()
                : properties.getApiKey();
    }

    private String appendPath(String apiUrl, String path) {
        if (!StringUtils.hasText(apiUrl)) {
            return "";
        }
        String normalized = stripKnownEndpoint(apiUrl);
        if (normalized.endsWith("/" + path)) {
            return normalized;
        }
        if (normalized.endsWith("/")) {
            return normalized + path;
        }
        return normalized + "/" + path;
    }

    private String stripKnownEndpoint(String apiUrl) {
        if (apiUrl.endsWith("/chat-messages")) {
            return apiUrl.substring(0, apiUrl.length() - "/chat-messages".length());
        }
        if (apiUrl.endsWith("/workflows/run")) {
            return apiUrl.substring(0, apiUrl.length() - "/workflows/run".length());
        }
        return apiUrl;
    }

    private String truncate(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() <= 500 ? compact : compact.substring(0, 500) + "...";
    }
}
