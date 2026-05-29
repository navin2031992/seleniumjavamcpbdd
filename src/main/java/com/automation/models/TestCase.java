package com.automation.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * AI-generated test case model — output of TestGeneratorAgent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {
    private String id;
    private String jiraTicketKey;
    private String title;
    private String description;
    private String priority;           // HIGH, MEDIUM, LOW
    private String testType;           // POSITIVE, NEGATIVE, BOUNDARY, SMOKE
    private List<String> preconditions;
    private List<TestStep> steps;
    private List<String> expectedResults;
    private List<String> tags;
    private String featureFile;
    private String scenarioName;
    private List<Map<String, String>> exampleData;  // for Scenario Outline
    @JsonProperty("isScenarioOutline")
    private boolean isScenarioOutline;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestStep {
        private String action;       // Given / When / Then / And
        private String description;
        private String elementKey;   // reference to XPath registry
        private String inputData;
    }

    public String toBddScenario() {
        StringBuilder sb = new StringBuilder();
        if (tags != null) {
            tags.forEach(tag -> sb.append("  @").append(tag).append("\n"));
        }
        if (isScenarioOutline) {
            sb.append("  Scenario Outline: ").append(scenarioName).append("\n");
        } else {
            sb.append("  Scenario: ").append(scenarioName).append("\n");
        }
        if (steps != null) {
            steps.forEach(step ->
                sb.append("    ").append(step.getAction())
                  .append(" ").append(step.getDescription()).append("\n")
            );
        }
        if (isScenarioOutline && exampleData != null && !exampleData.isEmpty()) {
            sb.append("\n    Examples:\n");
            List<String> headers = exampleData.get(0).keySet().stream().toList();
            sb.append("      | ").append(String.join(" | ", headers)).append(" |\n");
            exampleData.forEach(row -> {
                sb.append("      | ");
                headers.forEach(h -> sb.append(row.get(h)).append(" | "));
                sb.append("\n");
            });
        }
        return sb.toString();
    }
}
