package com.automation.utils;

import com.automation.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Centralised wait strategies.
 * Prefer explicit waits over Thread.sleep — these methods all use
 * ExpectedConditions or FluentWait with smart polling.
 */
public class WaitUtils {

    private static final Logger log = LogManager.getLogger(WaitUtils.class);
    private static final long DEFAULT_TIMEOUT = ConfigManager.getInstance().getExplicitWait();
    private static final long POLLING_INTERVAL = 500;

    private WaitUtils() {}

    public static WebDriverWait wait(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
    }

    public static WebDriverWait wait(WebDriver driver, long seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds));
    }

    public static FluentWait<WebDriver> fluentWait(WebDriver driver, long timeoutSeconds) {
        return new FluentWait<>(driver)
            .withTimeout(Duration.ofSeconds(timeoutSeconds))
            .pollingEvery(Duration.ofMillis(POLLING_INTERVAL))
            .ignoring(NoSuchElementException.class)
            .ignoring(StaleElementReferenceException.class);
    }

    // ── Element waits ──────────────────────────────────────────────────────────

    public static WebElement waitForVisible(WebDriver driver, By locator) {
        log.debug("Waiting for visible: {}", locator);
        return wait(driver).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForVisible(WebDriver driver, WebElement element) {
        return wait(driver).until(ExpectedConditions.visibilityOf(element));
    }

    public static WebElement waitForClickable(WebDriver driver, By locator) {
        log.debug("Waiting for clickable: {}", locator);
        return wait(driver).until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static WebElement waitForClickable(WebDriver driver, WebElement element) {
        return wait(driver).until(ExpectedConditions.elementToBeClickable(element));
    }

    public static WebElement waitForPresence(WebDriver driver, By locator) {
        return wait(driver).until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public static boolean waitForInvisible(WebDriver driver, By locator) {
        return wait(driver).until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public static boolean waitForInvisible(WebDriver driver, WebElement element) {
        return wait(driver).until(ExpectedConditions.invisibilityOf(element));
    }

    public static boolean waitForStale(WebDriver driver, WebElement element) {
        return wait(driver).until(ExpectedConditions.stalenessOf(element));
    }

    // ── Page waits ─────────────────────────────────────────────────────────────

    public static void waitForPageLoad(WebDriver driver) {
        log.debug("Waiting for page load...");
        wait(driver, ConfigManager.getInstance().getPageLoadTimeout())
            .until(d -> ((JavascriptExecutor) d)
                .executeScript("return document.readyState").equals("complete"));
    }

    public static void waitForAjax(WebDriver driver) {
        try {
            wait(driver).until(d ->
                (Boolean) ((JavascriptExecutor) d)
                    .executeScript("return (typeof jQuery === 'undefined') || jQuery.active === 0"));
        } catch (Exception e) {
            log.debug("jQuery not present — skipping AJAX wait");
        }
    }

    public static void waitForAngular(WebDriver driver) {
        try {
            wait(driver).until(d ->
                (Boolean) ((JavascriptExecutor) d)
                    .executeScript("return (typeof angular === 'undefined') || " +
                        "angular.element(document).injector().get('$http').pendingRequests.length === 0"));
        } catch (Exception e) {
            log.debug("Angular not present — skipping Angular wait");
        }
    }

    // ── URL / title waits ──────────────────────────────────────────────────────

    public static boolean waitForUrlContains(WebDriver driver, String fragment) {
        return wait(driver).until(ExpectedConditions.urlContains(fragment));
    }

    public static boolean waitForTitleContains(WebDriver driver, String title) {
        return wait(driver).until(ExpectedConditions.titleContains(title));
    }

    // ── Text waits ─────────────────────────────────────────────────────────────

    public static boolean waitForText(WebDriver driver, By locator, String text) {
        return wait(driver).until(ExpectedConditions.textToBe(locator, text));
    }

    public static boolean waitForTextContains(WebDriver driver, WebElement element, String text) {
        return wait(driver).until(ExpectedConditions.textToBePresentInElement(element, text));
    }

    // ── Loading spinner ───────────────────────────────────────────────────────

    public static void waitForSpinnerToDisappear(WebDriver driver) {
        try {
            By spinner = By.cssSelector(".spinner, .loading, [data-loading], .overlay");
            if (!driver.findElements(spinner).isEmpty()) {
                waitForInvisible(driver, spinner);
                log.debug("Loading spinner disappeared");
            }
        } catch (Exception ignored) {}
    }

    // ── Custom condition ───────────────────────────────────────────────────────

    public static <T> T waitFor(WebDriver driver, ExpectedCondition<T> condition, long timeoutSeconds) {
        return wait(driver, timeoutSeconds).until(condition);
    }
}
