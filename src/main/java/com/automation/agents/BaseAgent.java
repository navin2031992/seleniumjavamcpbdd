package com.automation.agents;

import com.automation.config.ConfigManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Base class for all AI agents.
 * Calls the Anthropic Claude API (claude-sonnet-4-6) to generate intelligent
 * outputs from natural-language prompts.
 */
public abstract class BaseAgent {

    protected final Logger log = LogManager.getLogger(getClass());
    protected final ConfigManager config = ConfigManager.getInstance();
    protected final ObjectMapper mapper = new ObjectMapper();

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();

    /**
     * Send a prompt to Claude and return the text response.
     */
    protected String askClaude(String systemPrompt, String userMessage) {
        return askClaude(systemPrompt, userMessage, 4096);
    }

    protected String askClaude(String systemPrompt, String userMessage, int maxTokens) {
        String apiKey = config.getAnthropicApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("ANTHROPIC_API_KEY not set — returning mock AI response");
            return mockResponse(userMessage);
        }

        try {
            Map<String, Object> body = Map.of(
                "model", config.getAnthropicModel(),
                "max_tokens", maxTokens,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userMessage))
            );

            Request request = new Request.Builder()
                .url(ANTHROPIC_API_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(RequestBody.create(mapper.writeValueAsString(body), JSON))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    log.error("Anthropic API error {}: {}", response.code(), responseBody);
                    return "ERROR: " + responseBody;
                }
                JsonNode root = mapper.readTree(responseBody);
                return root.path("content").get(0).path("text").asText();
            }
        } catch (IOException e) {
            log.error("Failed to call Anthropic API: {}", e.getMessage());
            return mockResponse(userMessage);
        }
    }

    /**
     * Returns a deterministic mock response when the API key is absent.
     * Subclasses may override for richer mock data.
     */
    protected String mockResponse(String prompt) {
        return "[MOCK] AI response for: " + prompt.substring(0, Math.min(80, prompt.length()));
    }

    /**
     * Parse a JSON array from the Claude response (strips markdown fences if present).
     */
    protected JsonNode parseJsonFromResponse(String response) {
        try {
            String cleaned = response
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
            return mapper.readTree(cleaned);
        } catch (Exception e) {
            log.warn("Could not parse JSON from AI response: {}", e.getMessage());
            return mapper.createObjectNode();
        }
    }
}
