package com.automation.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.stream.Collectors;

/**
 * High-level element interaction utilities.
 * All methods wait for the element to be in the correct state before acting.
 */
public class ElementUtils {

    private static final Logger log = LogManager.getLogger(ElementUtils.class);

    private ElementUtils() {}

    // ── Click ──────────────────────────────────────────────────────────────────

    public static void click(WebDriver driver, WebElement element) {
        try {
            WaitUtils.waitForClickable(driver, element).click();
            log.debug("Clicked: {}", describe(element));
        } catch (ElementClickInterceptedException e) {
            log.debug("Direct click intercepted — using JS click");
            jsClick(driver, element);
        }
    }

    public static void click(WebDriver driver, By locator) {
        click(driver, WaitUtils.waitForClickable(driver, locator));
    }

    public static void jsClick(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        log.debug("JS clicked: {}", describe(element));
    }

    public static void doubleClick(WebDriver driver, WebElement element) {
        new Actions(driver)
            .moveToElement(WaitUtils.waitForClickable(driver, element))
            .doubleClick()
            .perform();
        log.debug("Double-clicked: {}", describe(element));
    }

    public static void rightClick(WebDriver driver, WebElement element) {
        new Actions(driver)
            .contextClick(WaitUtils.waitForVisible(driver, element))
            .perform();
    }

    // ── Type / Input ───────────────────────────────────────────────────────────

    public static void type(WebDriver driver, WebElement element, String text) {
        WebElement el = WaitUtils.waitForVisible(driver, element);
        el.clear();
        el.sendKeys(text);
        log.debug("Typed '{}' into: {}", text, describe(element));
    }

    public static void type(WebDriver driver, By locator, String text) {
        WebElement el = WaitUtils.waitForVisible(driver, locator);
        el.clear();
        el.sendKeys(text);
    }

    public static void slowType(WebDriver driver, WebElement element, String text) {
        WebElement el = WaitUtils.waitForVisible(driver, element);
        el.clear();
        for (char c : text.toCharArray()) {
            el.sendKeys(String.valueOf(c));
        }
    }

    public static void clearAndType(WebDriver driver, WebElement element, String text) {
        WebElement el = WaitUtils.waitForVisible(driver, element);
        el.sendKeys(Keys.CONTROL + "a");
        el.sendKeys(Keys.DELETE);
        el.sendKeys(text);
        log.debug("Cleared and typed '{}' into: {}", text, describe(element));
    }

    public static void pressKey(WebDriver driver, WebElement element, Keys key) {
        WaitUtils.waitForVisible(driver, element).sendKeys(key);
    }

    // ── Dropdowns ─────────────────────────────────────────────────────────────

    public static void selectByVisibleText(WebDriver driver, WebElement element, String text) {
        WaitUtils.waitForVisible(driver, element);
        new Select(element).selectByVisibleText(text);
        log.debug("Selected '{}' from dropdown: {}", text, describe(element));
    }

    public static void selectByValue(WebDriver driver, WebElement element, String value) {
        WaitUtils.waitForVisible(driver, element);
        new Select(element).selectByValue(value);
    }

    public static void selectByIndex(WebDriver driver, WebElement element, int index) {
        WaitUtils.waitForVisible(driver, element);
        new Select(element).selectByIndex(index);
    }

    public static List<String> getDropdownOptions(WebElement element) {
        return new Select(element).getOptions().stream()
            .map(WebElement::getText)
            .collect(Collectors.toList());
    }

    // ── Get values ─────────────────────────────────────────────────────────────

    public static String getText(WebDriver driver, WebElement element) {
        String text = WaitUtils.waitForVisible(driver, element).getText().trim();
        log.debug("Got text '{}' from: {}", text, describe(element));
        return text;
    }

    public static String getText(WebDriver driver, By locator) {
        return getText(driver, WaitUtils.waitForVisible(driver, locator));
    }

    public static String getAttribute(WebDriver driver, WebElement element, String attr) {
        return WaitUtils.waitForVisible(driver, element).getAttribute(attr);
    }

    public static String getValue(WebDriver driver, WebElement element) {
        return WaitUtils.waitForVisible(driver, element).getAttribute("value");
    }

    // ── State checks ───────────────────────────────────────────────────────────

    public static boolean isDisplayed(WebDriver driver, By locator) {
        try {
            List<WebElement> found = driver.findElements(locator);
            return !found.isEmpty() && found.get(0).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isEnabled(WebElement element) {
        try { return element.isEnabled(); } catch (Exception e) { return false; }
    }

    public static boolean isSelected(WebElement element) {
        try { return element.isSelected(); } catch (Exception e) { return false; }
    }

    public static boolean hasClass(WebElement element, String cssClass) {
        String classes = element.getAttribute("class");
        return classes != null && classes.contains(cssClass);
    }

    // ── Scroll ────────────────────────────────────────────────────────────────

    public static void scrollToElement(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver)
            .executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", element);
    }

    public static void scrollToTop(WebDriver driver) {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
    }

    public static void scrollToBottom(WebDriver driver) {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    // ── Hover / Drag ──────────────────────────────────────────────────────────

    public static void hover(WebDriver driver, WebElement element) {
        new Actions(driver)
            .moveToElement(WaitUtils.waitForVisible(driver, element))
            .perform();
    }

    public static void dragAndDrop(WebDriver driver, WebElement source, WebElement target) {
        new Actions(driver).dragAndDrop(source, target).perform();
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    public static void uploadFile(WebElement fileInput, String absolutePath) {
        fileInput.sendKeys(absolutePath);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    public static void highlight(WebDriver driver, WebElement element) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].style.border='3px solid red'", element);
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        js.executeScript("arguments[0].style.border=''", element);
    }

    private static String describe(WebElement element) {
        try {
            String id = element.getAttribute("id");
            String tag = element.getTagName();
            return id != null && !id.isEmpty() ? tag + "#" + id : tag;
        } catch (Exception e) {
            return "element";
        }
    }
}
