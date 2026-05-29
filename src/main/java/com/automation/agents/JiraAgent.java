package com.automation.agents;

import com.automation.models.JiraTicket;
import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * JiraAgent — fetches stories/tasks from Jira and converts them into
 * structured JiraTicket objects that other agents can consume.
 *
 * Config keys (application.properties):
 *   jira.base.url       — e.g. https://myorg.atlassian.net
 *   jira.username       — Atlassian account email
 *   jira.api.token      — Jira API token (not password)
 *   jira.project.key    — e.g. PROJ
 */
public class JiraAgent extends BaseAgent {

    private static final Logger log = LogManager.getLogger(JiraAgent.class);

    private RequestSpecification jiraSpec() {
        String credentials = config.getJiraUsername() + ":" + config.getJiraApiToken();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return RestAssured.given()
            .baseUri(config.getJiraBaseUrl())
            .header("Authorization", "Basic " + encoded)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON);
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Fetch all open stories and tasks for the configured project.
     */
    public List<JiraTicket> fetchOpenTickets() {
        return fetchByJql(
            "project = " + config.getJiraProject() +
            " AND issuetype in (Story, Task, Bug)" +
            " AND status not in (Done, Closed, Cancelled)" +
            " ORDER BY priority DESC"
        );
    }

    /**
     * Fetch a single ticket by key (e.g. PROJ-123).
     */
    public JiraTicket fetchTicket(String ticketKey) {
        log.info("Fetching Jira ticket: {}", ticketKey);
        try {
            if (config.getJiraBaseUrl().isEmpty()) {
                return mockTicket(ticketKey);
            }
            Response resp = jiraSpec()
                .get("/rest/api/3/issue/" + ticketKey);

            if (resp.statusCode() == 200) {
                return parseIssue(resp.as(JsonNode.class));
            }
            log.warn("Jira returned status {} for {}", resp.statusCode(), ticketKey);
            return mockTicket(ticketKey);
        } catch (Exception e) {
            log.error("Failed to fetch Jira ticket {}: {}", ticketKey, e.getMessage());
            return mockTicket(ticketKey);
        }
    }

    /**
     * Fetch tickets by arbitrary JQL query.
     */
    public List<JiraTicket> fetchByJql(String jql) {
        log.info("Fetching Jira tickets with JQL: {}", jql);
        List<JiraTicket> tickets = new ArrayList<>();
        try {
            if (config.getJiraBaseUrl().isEmpty()) {
                return List.of(mockTicket("DEMO-1"), mockTicket("DEMO-2"));
            }

            Response resp = jiraSpec()
                .queryParam("jql", jql)
                .queryParam("maxResults", 50)
                .queryParam("fields", "summary,description,issuetype,status,priority,labels,assignee,reporter,customfield_10016,customfield_10014")
                .get("/rest/api/3/search");

            if (resp.statusCode() == 200) {
                JsonNode root = resp.as(JsonNode.class);
                JsonNode issues = root.path("issues");
                for (JsonNode issue : issues) {
                    tickets.add(parseIssue(issue));
                }
                log.info("Fetched {} tickets from Jira", tickets.size());
            } else {
                log.warn("Jira search returned status: {}", resp.statusCode());
            }
        } catch (Exception e) {
            log.error("Failed to fetch Jira tickets: {}", e.getMessage());
        }
        return tickets;
    }

    /**
     * Use AI to extract structured acceptance criteria from raw ticket description.
     */
    public List<String> extractAcceptanceCriteria(JiraTicket ticket) {
        String system = """
            You are a QA expert. Extract clear, testable acceptance criteria
            from the Jira ticket below. Return a JSON array of strings only.
            Each criterion should start with 'Given', 'When', 'Then', or describe
            a verifiable condition. Return at least 3 criteria.
            """;

        String response = askClaude(system, ticket.toPromptContext());
        JsonNode node = parseJsonFromResponse(response);

        List<String> criteria = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> criteria.add(item.asText()));
        }
        return criteria;
    }

    // ── Parsing ────────────────────────────────────────────────────────────────

    private JiraTicket parseIssue(JsonNode issue) {
        JsonNode fields = issue.path("fields");
        JiraTicket ticket = JiraTicket.builder()
            .key(issue.path("key").asText())
            .summary(fields.path("summary").asText())
            .issueType(fields.path("issuetype").path("name").asText())
            .status(fields.path("status").path("name").asText())
            .priority(fields.path("priority").path("name").asText())
            .build();

        // Description (Jira doc format)
        JsonNode descNode = fields.path("description");
        if (!descNode.isMissingNode()) {
            ticket.setDescription(flattenAdf(descNode));
        }

        // Labels
        List<String> labels = new ArrayList<>();
        fields.path("labels").forEach(l -> labels.add(l.asText()));
        ticket.setLabels(labels);

        // Story points (customfield_10016)
        String sp = fields.path("customfield_10016").asText("0");
        ticket.setStoryPoints(sp);

        return ticket;
    }

    /**
     * Flatten Atlassian Document Format (ADF) to plain text.
     */
    private String flattenAdf(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        if (node.isTextual()) return node.asText();
        if (node.has("text")) return node.path("text").asText();
        if (node.has("content")) {
            node.path("content").forEach(child -> sb.append(flattenAdf(child)).append(" "));
        }
        return sb.toString().trim();
    }

    // ── Mock data (no Jira configured) ────────────────────────────────────────

    private JiraTicket mockTicket(String key) {
        return JiraTicket.builder()
            .key(key)
            .summary("User can log in with valid credentials")
            .description("As a registered user, I want to log in to the application " +
                         "using my email and password so that I can access my account.")
            .issueType("Story")
            .status("In Progress")
            .priority("High")
            .acceptanceCriteria(List.of(
                "Given I am on the login page, when I enter valid credentials and click login, then I am redirected to the dashboard",
                "Given I enter an invalid password, when I click login, then I see an error message",
                "Given I leave fields empty, when I click login, then I see validation messages"
            ))
            .labels(List.of("authentication", "smoke"))
            .storyPoints("3")
            .build();
    }
}
