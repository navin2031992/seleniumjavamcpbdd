package com.automation.utils;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Allure reporting utilities — step logging, attachment helpers,
 * and test metadata tagging.
 */
public class ReportUtils {

    private static final Logger log = LogManager.getLogger(ReportUtils.class);

    private ReportUtils() {}

    // ── Step reporting ─────────────────────────────────────────────────────────

    public static void startStep(String name) {
        AllureLifecycle lifecycle = Allure.getLifecycle();
        String uuid = UUID.randomUUID().toString();
        lifecycle.startStep(uuid, new StepResult().setName(name).setStatus(Status.PASSED));
        log.debug("[STEP] {}", name);
    }

    public static void passStep(String name) {
        Allure.step(name, Status.PASSED);
        log.info("[PASS] {}", name);
    }

    public static void failStep(String name) {
        Allure.step(name, Status.FAILED);
        log.error("[FAIL] {}", name);
    }

    public static void skipStep(String name) {
        Allure.step(name, Status.SKIPPED);
        log.warn("[SKIP] {}", name);
    }

    // ── Attachments ───────────────────────────────────────────────────────────

    public static void attachText(String label, String content) {
        Allure.addAttachment(label, "text/plain",
            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), ".txt");
    }

    public static void attachHtml(String label, String html) {
        Allure.addAttachment(label, "text/html",
            new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)), ".html");
    }

    public static void attachJson(String label, String json) {
        Allure.addAttachment(label, "application/json",
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), ".json");
    }

    public static void attachBytes(String label, byte[] data, String extension) {
        Allure.addAttachment(label, "image/png", new ByteArrayInputStream(data), extension);
    }

    // ── Environment metadata ──────────────────────────────────────────────────

    public static void addEnvironmentInfo(String key, String value) {
        Allure.parameter(key, value);
    }

    public static void addTestLabel(String name, String value) {
        Allure.label(name, value);
    }

    public static void addJiraLink(String ticketKey, String jiraBaseUrl) {
        if (jiraBaseUrl == null || jiraBaseUrl.isEmpty()) return;
        Allure.issue(ticketKey, jiraBaseUrl + "/browse/" + ticketKey);
    }

    public static void addTmsLink(String testId, String tmsBaseUrl) {
        Allure.tms(testId, tmsBaseUrl + "/testcase/" + testId);
    }

    // ── Logging to Allure ─────────────────────────────────────────────────────

    public static void log(String message) {
        attachText("Log", message);
        log.info(message);
    }

    public static void logPageSource(String html) {
        attachHtml("Page Source", html);
    }
}
