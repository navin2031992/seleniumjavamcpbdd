package com.automation.agents;

import com.automation.config.ConfigManager;
import com.automation.config.DriverManager;
import com.automation.models.JiraTicket;
import com.automation.models.TestCase;
import com.automation.models.XPathElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Map;

/**
 * AgentOrchestrator — coordinates the full AI-powered test generation pipeline:
 *
 *   1. JiraAgent        → fetch open stories from Jira
 *   2. TestGeneratorAgent → generate BDD test cases (via Claude AI)
 *   3. XPathCaptureAgent  → capture element locators from the live app
 *   4. ScriptGeneratorAgent → write feature files, step defs, page objects
 *
 * Can be invoked from the CI pipeline:
 *   mvn exec:java -Dexec.mainClass="com.automation.agents.AgentOrchestrator"
 *
 * Or programmatically from test setup / a custom TestNG listener.
 */
public class AgentOrchestrator {

    private static final Logger log = LogManager.getLogger(AgentOrchestrator.class);
    private static final ConfigManager config = ConfigManager.getInstance();

    private final JiraAgent         jiraAgent         = new JiraAgent();
    private final TestGeneratorAgent testGenerator    = new TestGeneratorAgent();
    private final XPathCaptureAgent xpathAgent        = new XPathCaptureAgent();
    private final ScriptGeneratorAgent scriptGenerator = new ScriptGeneratorAgent();

    // ── Full pipeline ──────────────────────────────────────────────────────────

    /**
     * Run the complete Jira → Test Cases → Feature Files pipeline.
     */
    public void runFullPipeline() {
        log.info("╔══════════════════════════════════════════════════════");
        log.info("║   AI-Powered Test Generation Pipeline Starting");
        log.info("╚══════════════════════════════════════════════════════");

        // Step 1: Fetch Jira tickets
        log.info("[1/4] Fetching Jira tickets...");
        List<JiraTicket> tickets = jiraAgent.fetchOpenTickets();
        log.info("      Found {} open tickets", tickets.size());

        if (tickets.isEmpty()) {
            log.warn("No tickets found — check jira.base.url and jira.project.key in config");
            return;
        }

        // Step 2: Generate test cases per ticket
        log.info("[2/4] Generating test cases with AI...");
        Map<String, List<TestCase>> allTestCases = testGenerator.generateFromTickets(tickets);

        // Step 3: Capture XPaths from live application
        log.info("[3/4] Capturing UI element locators...");
        List<XPathElement> elements = captureElementsIfAppAvailable();

        // Step 4: Generate BDD artifacts
        log.info("[4/4] Writing feature files and step definitions...");
        tickets.forEach(ticket -> {
            List<TestCase> tcs = allTestCases.getOrDefault(ticket.getKey(), List.of());
            if (!tcs.isEmpty()) {
                ScriptGeneratorAgent.GenerationResult result =
                    scriptGenerator.generateAll(ticket, tcs, elements);
                log.info("      ✓ {} → {}", ticket.getKey(), result);
            }
        });

        log.info("╔══════════════════════════════════════════════════════");
        log.info("║   Pipeline Complete! Run: mvn test -Pdev");
        log.info("╚══════════════════════════════════════════════════════");
    }

    /**
     * Regenerate tests for a specific ticket only.
     */
    public void runForTicket(String ticketKey) {
        log.info("Generating tests for single ticket: {}", ticketKey);
        JiraTicket ticket = jiraAgent.fetchTicket(ticketKey);
        List<TestCase> testCases = testGenerator.generateFromTicket(ticket);
        List<XPathElement> elements = xpathAgent.loadRegistry();
        scriptGenerator.generateAll(ticket, testCases, elements);
    }

    /**
     * Capture XPaths from the application if the base URL is reachable.
     */
    private List<XPathElement> captureElementsIfAppAvailable() {
        // If registry already has elements, use those
        List<XPathElement> existing = xpathAgent.loadRegistry();
        if (!existing.isEmpty()) {
            log.info("      Using {} elements from existing XPath registry", existing.size());
            return existing;
        }

        // Attempt live capture if app URL is set
        String baseUrl = config.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            log.warn("      app.base.url not set — skipping live XPath capture");
            return existing;
        }

        WebDriver driver = null;
        try {
            driver = DriverManager.getDriver();
            driver.get(baseUrl + config.get("login.path", "/login"));
            List<XPathElement> captured = xpathAgent.capturePageElements(driver, "LoginPage");
            log.info("      Captured {} login page elements", captured.size());
            return captured;
        } catch (Exception e) {
            log.warn("      Live XPath capture failed (app may not be running): {}", e.getMessage());
            return existing;
        } finally {
            if (driver != null) {
                try { DriverManager.quitDriver(); } catch (Exception ignored) {}
            }
        }
    }

    // ── Main (for CI invocation) ───────────────────────────────────────────────

    public static void main(String[] args) {
        AgentOrchestrator orchestrator = new AgentOrchestrator();

        boolean generateFromJira = false;
        String specificTicket = null;

        for (String arg : args) {
            if (arg.equals("--generate-from-jira")) generateFromJira = true;
            if (arg.startsWith("--ticket=")) specificTicket = arg.substring(9);
        }

        if (specificTicket != null) {
            orchestrator.runForTicket(specificTicket);
        } else if (generateFromJira || args.length == 0) {
            orchestrator.runFullPipeline();
        }
    }
}
