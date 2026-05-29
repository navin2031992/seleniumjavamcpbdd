package com.automation.agents;

import com.automation.models.JiraTicket;
import com.automation.models.TestCase;
import com.automation.models.TestCase.TestStep;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TestGeneratorAgent — uses Claude AI to produce structured test cases
 * from Jira ticket acceptance criteria.
 *
 * Generates:
 *   - Positive / happy-path scenarios
 *   - Negative / error scenarios
 *   - Boundary value scenarios
 *   - Smoke tag scenarios
 *
 * Output: List<TestCase> ready to be serialised as Cucumber feature files
 * by ScriptGeneratorAgent.
 */
public class TestGeneratorAgent extends BaseAgent {

    private static final Logger log = LogManager.getLogger(TestGeneratorAgent.class);

    private static final String SYSTEM_PROMPT = """
        You are a senior QA automation architect with deep expertise in BDD (Gherkin).
        Your task is to generate comprehensive test cases from Jira ticket information.

        Rules:
        1. Always create at minimum: one positive, one negative, and one boundary test.
        2. Use clear, business-readable Gherkin language (no technical jargon).
        3. Test data must be realistic but anonymised (e.g. "test@example.com").
        4. Cover edge cases mentioned in acceptance criteria.
        5. Return ONLY a valid JSON array. No markdown, no explanation.

        Each test case JSON object must have:
        {
          "id": "TC-001",
          "title": "Verify successful login with valid credentials",
          "testType": "POSITIVE",
          "priority": "HIGH",
          "tags": ["smoke", "regression", "login"],
          "scenarioName": "Successful login with valid credentials",
          "isScenarioOutline": false,
          "preconditions": ["User account exists", "Application is running"],
          "steps": [
            {"action": "Given", "description": "I am on the login page"},
            {"action": "When", "description": "I enter username \\"test@example.com\\""},
            {"action": "And", "description": "I enter password \\"Password123!\\""},
            {"action": "And", "description": "I click the Login button"},
            {"action": "Then", "description": "I should be redirected to the dashboard"},
            {"action": "And", "description": "I should see a welcome message"}
          ],
          "expectedResults": ["Dashboard is displayed", "User name shown in header"]
        }
        """;

    /**
     * Generate test cases from a single Jira ticket.
     */
    public List<TestCase> generateFromTicket(JiraTicket ticket) {
        log.info("Generating test cases for ticket: {}", ticket.getKey());

        String userMessage = "Generate comprehensive test cases for this Jira ticket:\n\n"
            + ticket.toPromptContext()
            + "\n\nInclude positive, negative, boundary, and edge case scenarios. "
            + "Tag smoke tests with @smoke, regression tests with @regression.";

        String aiResponse = askClaude(SYSTEM_PROMPT, userMessage, 8192);
        return parseTestCases(aiResponse, ticket.getKey());
    }

    /**
     * Generate test cases for multiple tickets at once (batched for efficiency).
     */
    public Map<String, List<TestCase>> generateFromTickets(List<JiraTicket> tickets) {
        Map<String, List<TestCase>> results = new HashMap<>();
        tickets.forEach(ticket -> results.put(ticket.getKey(), generateFromTicket(ticket)));
        return results;
    }

    /**
     * Generate a Scenario Outline for data-driven testing.
     */
    public TestCase generateScenarioOutline(JiraTicket ticket, List<Map<String, String>> testData) {
        log.info("Generating Scenario Outline for: {}", ticket.getKey());

        String userMessage = String.format("""
            For the Jira ticket below, generate a Cucumber Scenario Outline
            using this test data: %s

            Ticket:
            %s

            Return ONLY a single JSON object (not an array) for the Scenario Outline.
            Set isScenarioOutline=true and include an "exampleData" array.
            """, mapper.valueToTree(testData), ticket.toPromptContext());

        String response = askClaude(SYSTEM_PROMPT, userMessage);
        JsonNode node = parseJsonFromResponse(response);
        return buildTestCase(node, ticket.getKey());
    }

    // ── Parsing ────────────────────────────────────────────────────────────────

    private List<TestCase> parseTestCases(String response, String ticketKey) {
        List<TestCase> testCases = new ArrayList<>();
        JsonNode root = parseJsonFromResponse(response);

        if (root.isArray()) {
            int index = 1;
            for (JsonNode node : root) {
                TestCase tc = buildTestCase(node, ticketKey);
                if (tc.getId() == null || tc.getId().isEmpty()) {
                    tc.setId(ticketKey + "-TC-" + String.format("%03d", index++));
                }
                tc.setJiraTicketKey(ticketKey);
                testCases.add(tc);
            }
        } else {
            log.warn("AI did not return an array; attempting single-object parse");
            TestCase tc = buildTestCase(root, ticketKey);
            tc.setJiraTicketKey(ticketKey);
            testCases.add(tc);
        }

        log.info("Generated {} test cases for {}", testCases.size(), ticketKey);
        return testCases;
    }

    @SuppressWarnings("unchecked")
    private TestCase buildTestCase(JsonNode node, String ticketKey) {
        List<TestStep> steps = new ArrayList<>();
        node.path("steps").forEach(s -> steps.add(
            TestStep.builder()
                .action(s.path("action").asText("When"))
                .description(s.path("description").asText())
                .elementKey(s.path("elementKey").asText(null))
                .inputData(s.path("inputData").asText(null))
                .build()
        ));

        List<String> tags = new ArrayList<>();
        node.path("tags").forEach(t -> tags.add(t.asText()));

        List<String> preconditions = new ArrayList<>();
        node.path("preconditions").forEach(p -> preconditions.add(p.asText()));

        List<String> expected = new ArrayList<>();
        node.path("expectedResults").forEach(e -> expected.add(e.asText()));

        List<Map<String, String>> exampleData = new ArrayList<>();
        node.path("exampleData").forEach(row -> {
            Map<String, String> rowMap = new HashMap<>();
            row.fields().forEachRemaining(entry -> rowMap.put(entry.getKey(), entry.getValue().asText()));
            exampleData.add(rowMap);
        });

        return TestCase.builder()
            .id(node.path("id").asText(""))
            .jiraTicketKey(ticketKey)
            .title(node.path("title").asText(""))
            .testType(node.path("testType").asText("POSITIVE"))
            .priority(node.path("priority").asText("MEDIUM"))
            .tags(tags)
            .scenarioName(node.path("scenarioName").asText(node.path("title").asText()))
            .isScenarioOutline(node.path("isScenarioOutline").asBoolean(false))
            .preconditions(preconditions)
            .steps(steps)
            .expectedResults(expected)
            .exampleData(exampleData)
            .build();
    }
}
