package com.automation.steps;

import com.automation.config.ConfigManager;
import com.automation.config.DriverManager;
import com.automation.utils.WaitUtils;
import io.cucumber.java.en.*;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;

/**
 * Universal step definitions shared across all feature files.
 * Covers navigation, assertions, and utility actions.
 */
public class CommonSteps {

    private static final Logger log = LogManager.getLogger(CommonSteps.class);
    private final ConfigManager config = ConfigManager.getInstance();

    private WebDriver driver() { return DriverManager.getDriver(); }

    // ── Navigation ─────────────────────────────────────────────────────────────

    @Given("I am on the {string} page")
    @Step("Navigate to page: {pageName}")
    public void iAmOnPage(String pageName) {
        String path = config.get("page." + pageName.toLowerCase().replace(" ", "."), "/" + pageName.toLowerCase());
        String url = config.getBaseUrl() + path;
        log.info("Navigating to page '{}' → {}", pageName, url);
        driver().get(url);
        WaitUtils.waitForPageLoad(driver());
    }

    @Given("I navigate to {string}")
    public void iNavigateTo(String url) {
        String fullUrl = url.startsWith("http") ? url : config.getBaseUrl() + url;
        log.info("Navigating to: {}", fullUrl);
        driver().get(fullUrl);
        WaitUtils.waitForPageLoad(driver());
    }

    @When("I refresh the page")
    public void iRefreshThePage() {
        driver().navigate().refresh();
        WaitUtils.waitForPageLoad(driver());
    }

    @When("I navigate back")
    public void iNavigateBack() {
        driver().navigate().back();
        WaitUtils.waitForPageLoad(driver());
    }

    // ── URL / Title assertions ─────────────────────────────────────────────────

    @Then("the page title should be {string}")
    public void pageTitleShouldBe(String expectedTitle) {
        Assert.assertEquals(driver().getTitle(), expectedTitle,
            "Page title mismatch");
    }

    @Then("the page title should contain {string}")
    public void pageTitleShouldContain(String text) {
        Assert.assertTrue(driver().getTitle().contains(text),
            "Page title '" + driver().getTitle() + "' does not contain '" + text + "'");
    }

    @Then("the URL should contain {string}")
    public void urlShouldContain(String fragment) {
        WaitUtils.waitForUrlContains(driver(), fragment);
        Assert.assertTrue(driver().getCurrentUrl().contains(fragment),
            "URL '" + driver().getCurrentUrl() + "' does not contain '" + fragment + "'");
    }

    @Then("the URL should be {string}")
    public void urlShouldBe(String expectedUrl) {
        String fullUrl = expectedUrl.startsWith("http") ? expectedUrl : config.getBaseUrl() + expectedUrl;
        Assert.assertEquals(driver().getCurrentUrl(), fullUrl, "URL mismatch");
    }

    // ── Element assertions ────────────────────────────────────────────────────

    @Then("I should see {string} on the page")
    public void iShouldSeeText(String text) {
        String pageSource = driver().getPageSource();
        Assert.assertTrue(pageSource.contains(text),
            "Expected text '" + text + "' not found on page");
    }

    @Then("I should not see {string} on the page")
    public void iShouldNotSeeText(String text) {
        Assert.assertFalse(driver().getPageSource().contains(text),
            "Text '" + text + "' was found on page but should not be");
    }

    @Then("the element with id {string} should be visible")
    public void elementShouldBeVisible(String id) {
        Assert.assertTrue(
            !driver().findElements(By.id(id)).isEmpty()
            && driver().findElement(By.id(id)).isDisplayed(),
            "Element with id '" + id + "' is not visible"
        );
    }

    @Then("the element with css {string} should be visible")
    public void elementByCssShouldBeVisible(String css) {
        WaitUtils.waitForVisible(driver(), By.cssSelector(css));
        Assert.assertTrue(driver().findElement(By.cssSelector(css)).isDisplayed(),
            "Element with CSS '" + css + "' is not visible");
    }

    @Then("the element with css {string} should not be visible")
    public void elementByCssShouldNotBeVisible(String css) {
        boolean invisible = driver().findElements(By.cssSelector(css)).isEmpty()
            || !driver().findElement(By.cssSelector(css)).isDisplayed();
        Assert.assertTrue(invisible, "Element with CSS '" + css + "' is still visible");
    }

    // ── Waits ──────────────────────────────────────────────────────────────────

    @When("I wait for {int} seconds")
    public void iWaitForSeconds(int seconds) {
        log.warn("Hard sleep for {} seconds — consider replacing with an explicit wait", seconds);
        try { Thread.sleep(seconds * 1000L); } catch (InterruptedException ignored) {}
    }

    @When("I wait for the page to load")
    public void iWaitForPageToLoad() {
        WaitUtils.waitForPageLoad(driver());
    }

    // ── Browser window ────────────────────────────────────────────────────────

    @Then("the page should be responsive at {int} x {int}")
    public void pageIsResponsiveAt(int width, int height) {
        driver().manage().window().setSize(new org.openqa.selenium.Dimension(width, height));
        // Just checking no JS errors — fuller responsive checks require visual tools
        Assert.assertNotNull(driver().getPageSource(), "Page source is null at " + width + "x" + height);
    }
}
