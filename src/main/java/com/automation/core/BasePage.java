package com.automation.core;

import com.automation.config.ConfigManager;
import com.automation.utils.ElementUtils;
import com.automation.utils.ScreenshotUtils;
import com.automation.utils.WaitUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

/**
 * Base class for all Page Objects.
 * Wraps WebDriver interactions with wait logic, logging, and screenshot hooks.
 * Extend this class, call PageFactory.initElements(driver, this) in your constructor.
 */
public abstract class BasePage {

    protected final Logger log = LogManager.getLogger(getClass());
    protected final WebDriver driver;
    protected final ConfigManager config = ConfigManager.getInstance();

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Navigation ─────────────────────────────────────────────────────────────

    public void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        driver.get(url);
        WaitUtils.waitForPageLoad(driver);
    }

    public void navigateToBaseUrl() {
        navigateTo(config.getBaseUrl());
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    public void refreshPage() {
        driver.navigate().refresh();
        WaitUtils.waitForPageLoad(driver);
    }

    public void goBack() {
        driver.navigate().back();
        WaitUtils.waitForPageLoad(driver);
    }

    // ── Interaction wrappers ───────────────────────────────────────────────────

    protected void click(WebElement element) {
        ElementUtils.click(driver, element);
    }

    protected void click(By locator) {
        ElementUtils.click(driver, locator);
    }

    protected void jsClick(WebElement element) {
        ElementUtils.jsClick(driver, element);
    }

    protected void type(WebElement element, String text) {
        ElementUtils.type(driver, element, text);
    }

    protected void clearAndType(WebElement element, String text) {
        ElementUtils.clearAndType(driver, element, text);
    }

    protected void selectByText(WebElement dropdown, String text) {
        ElementUtils.selectByVisibleText(driver, dropdown, text);
    }

    protected void selectByValue(WebElement dropdown, String value) {
        ElementUtils.selectByValue(driver, dropdown, value);
    }

    protected String getText(WebElement element) {
        return ElementUtils.getText(driver, element);
    }

    protected String getValue(WebElement element) {
        return ElementUtils.getValue(driver, element);
    }

    protected void hover(WebElement element) {
        ElementUtils.hover(driver, element);
    }

    protected void scrollTo(WebElement element) {
        ElementUtils.scrollToElement(driver, element);
    }

    protected void highlight(WebElement element) {
        ElementUtils.highlight(driver, element);
    }

    // ── Waits ──────────────────────────────────────────────────────────────────

    protected WebElement waitForVisible(WebElement element) {
        return WaitUtils.waitForVisible(driver, element);
    }

    protected WebElement waitForClickable(WebElement element) {
        return WaitUtils.waitForClickable(driver, element);
    }

    protected void waitForPageLoad() {
        WaitUtils.waitForPageLoad(driver);
        WaitUtils.waitForSpinnerToDisappear(driver);
    }

    protected void waitForAjax() {
        WaitUtils.waitForAjax(driver);
    }

    protected boolean waitForUrlContains(String fragment) {
        return WaitUtils.waitForUrlContains(driver, fragment);
    }

    // ── State checks ───────────────────────────────────────────────────────────

    protected boolean isDisplayed(By locator) {
        return ElementUtils.isDisplayed(driver, locator);
    }

    protected boolean isDisplayed(WebElement element) {
        try { return element.isDisplayed(); } catch (Exception e) { return false; }
    }

    protected boolean isEnabled(WebElement element) {
        return ElementUtils.isEnabled(element);
    }

    protected boolean isSelected(WebElement element) {
        return ElementUtils.isSelected(element);
    }

    protected List<WebElement> findElements(By locator) {
        return driver.findElements(locator);
    }

    protected int getElementCount(By locator) {
        return driver.findElements(locator).size();
    }

    // ── JavaScript ────────────────────────────────────────────────────────────

    protected Object executeScript(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    protected void setInputValue(WebElement element, String value) {
        executeScript("arguments[0].value = arguments[1];", element, value);
    }

    // ── Frames ─────────────────────────────────────────────────────────────────

    protected void switchToFrame(WebElement frame) {
        driver.switchTo().frame(frame);
    }

    protected void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }

    // ── Screenshots ───────────────────────────────────────────────────────────

    protected String takeScreenshot(String label) {
        return ScreenshotUtils.captureAndAttach(driver, label);
    }

    // ── Window management ─────────────────────────────────────────────────────

    protected void switchToNewTab() {
        String original = driver.getWindowHandle();
        driver.getWindowHandles().stream()
            .filter(h -> !h.equals(original))
            .findFirst()
            .ifPresent(h -> driver.switchTo().window(h));
    }

    protected void closeCurrentTab() {
        driver.close();
    }

    // ── Alerts ────────────────────────────────────────────────────────────────

    protected void acceptAlert() {
        driver.switchTo().alert().accept();
    }

    protected void dismissAlert() {
        driver.switchTo().alert().dismiss();
    }

    protected String getAlertText() {
        return driver.switchTo().alert().getText();
    }
}
