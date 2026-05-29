package com.automation.hooks;

import com.automation.config.ConfigManager;
import com.automation.config.DriverManager;
import com.automation.utils.ReportUtils;
import com.automation.utils.ScreenshotUtils;
import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

/**
 * Cucumber lifecycle hooks.
 *
 * Execution order per scenario:
 *   @Before (order=1) → setUp: driver creation, Allure tagging
 *   ... scenario steps ...
 *   @AfterStep        → screenshot on failure (configurable)
 *   @After  (order=1) → tearDown: screenshot on failure, driver quit
 */
public class Hooks {

    private static final Logger log = LogManager.getLogger(Hooks.class);
    private final ConfigManager config = ConfigManager.getInstance();

    @Before(order = 1)
    public void setUp(Scenario scenario) {
        log.info("╔══════════════════════════════════════════════════════");
        log.info("║ Scenario: {}", scenario.getName());
        log.info("║ Tags:     {}", scenario.getSourceTagNames());
        log.info("╚══════════════════════════════════════════════════════");

        WebDriver driver = DriverManager.getDriver();

        // Tag Allure report with scenario metadata
        Allure.parameter("Browser",     config.getBrowser());
        Allure.parameter("Environment", config.get("env", "dev"));
        Allure.parameter("Base URL",    config.getBaseUrl());
        Allure.parameter("Headless",    String.valueOf(config.isHeadless()));

        scenario.getSourceTagNames().stream()
            .filter(t -> t.startsWith("@PROJ-") || t.startsWith("@JIRA-"))
            .map(t -> t.substring(1))
            .findFirst()
            .ifPresent(key -> ReportUtils.addJiraLink(key, config.getJiraBaseUrl()));
    }

    @AfterStep
    public void afterStep(Scenario scenario) {
        // Capture screenshot after every failing step for detailed debugging
        if (scenario.isFailed()) {
            WebDriver driver = DriverManager.getDriver();
            if (driver != null) {
                ScreenshotUtils.captureAndAttach(driver, "FAILED_STEP_" + scenario.getName());
                // Attach page source for deep diagnosis
                try {
                    ReportUtils.logPageSource(driver.getPageSource());
                } catch (Exception ignored) {}
            }
        }
    }

    @After(order = 1)
    public void tearDown(Scenario scenario) {
        WebDriver driver = DriverManager.getDriver();

        if (scenario.isFailed() && driver != null) {
            log.error("Scenario FAILED: {}", scenario.getName());
            ScreenshotUtils.captureAndAttach(driver, "FINAL_FAILURE_" + scenario.getName());
        } else {
            log.info("Scenario PASSED: {}", scenario.getName());
            // Optional: screenshot on pass (useful for smoke test evidence)
            if (config.getBoolean("screenshot.on.pass", false) && driver != null) {
                ScreenshotUtils.captureAndAttach(driver, "PASS_" + scenario.getName());
            }
        }

        DriverManager.quitDriver();
        log.info("══════════════════════════════════════════════════════\n");
    }
}
