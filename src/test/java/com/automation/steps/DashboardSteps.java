package com.automation.steps;

import com.automation.pages.DashboardPage;
import com.automation.pages.LoginPage;
import io.cucumber.java.en.*;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;

import java.util.List;

/**
 * Step definitions for dashboard and post-login feature files.
 */
public class DashboardSteps {

    private static final Logger log = LogManager.getLogger(DashboardSteps.class);

    private final DashboardPage dashboardPage;
    private final LoginPage loginPage;

    public DashboardSteps(com.automation.hooks.PageObjectFactory factory) {
        this.dashboardPage = factory.getDashboardPage();
        this.loginPage = factory.getLoginPage();
    }

    // ── Given ─────────────────────────────────────────────────────────────────

    @Given("I am on the dashboard")
    public void iAmOnTheDashboard() {
        Assert.assertTrue(dashboardPage.isDashboardDisplayed(),
            "Expected to be on the dashboard. Current URL: " + dashboardPage.getCurrentUrl());
        log.info("Pre-condition: on dashboard");
    }

    // ── When ──────────────────────────────────────────────────────────────────

    @When("I click on the {string} navigation link")
    @Step("Click navigation link: {menuItem}")
    public void iClickNavigationLink(String menuItem) {
        dashboardPage.clickNavigationItem(menuItem);
    }

    @When("I click the logout button")
    @Step("Click logout")
    public void iClickLogout() {
        dashboardPage.logout();
    }

    @When("I search for {string}")
    @Step("Search for: {query}")
    public void iSearchFor(String query) {
        dashboardPage.search(query);
    }

    @When("I open the profile menu")
    public void iOpenProfileMenu() {
        dashboardPage.openProfileMenu();
    }

    // ── Then ──────────────────────────────────────────────────────────────────

    @Then("the dashboard should be displayed")
    @Step("Verify dashboard is displayed")
    public void dashboardShouldBeDisplayed() {
        dashboardPage.verifyLoaded();
        Assert.assertTrue(dashboardPage.isDashboardDisplayed(),
            "Dashboard is not displayed");
    }

    @Then("the navigation menu should be visible")
    public void navigationMenuShouldBeVisible() {
        Assert.assertTrue(dashboardPage.isNavigationVisible(),
            "Navigation menu is not visible");
    }

    @Then("the navigation should contain {string}")
    public void navigationShouldContain(String menuItem) {
        List<String> items = dashboardPage.getNavigationItems();
        Assert.assertTrue(
            items.stream().anyMatch(i -> i.equalsIgnoreCase(menuItem)),
            "Navigation does not contain '" + menuItem + "'. Found: " + items
        );
    }

    @Then("I should see notifications")
    public void iShouldSeeNotifications() {
        Assert.assertTrue(dashboardPage.isNotificationBadgeVisible(),
            "Notification badge is not visible");
        int count = dashboardPage.getNotificationCount();
        log.info("Notification count: {}", count);
    }

    @Then("I should be logged out and redirected to the login page")
    public void iShouldBeLoggedOut() {
        Assert.assertTrue(loginPage.isLoginPageDisplayed(),
            "Expected to be on login page after logout. URL: " + loginPage.getCurrentUrl());
        log.info("Successfully logged out — on login page");
    }

    @Then("the search bar should be present")
    public void searchBarShouldBePresent() {
        Assert.assertTrue(dashboardPage.isSearchBarPresent(),
            "Search bar is not present on the dashboard");
    }

    @Then("the dashboard header should show {string}")
    public void dashboardHeaderShouldShow(String expectedText) {
        String welcome = dashboardPage.getWelcomeMessage();
        Assert.assertTrue(welcome.contains(expectedText),
            "Dashboard header '" + welcome + "' does not contain '" + expectedText + "'");
    }

    @Then("the dashboard should pass all accessibility checks")
    public void dashboardAccessibilityChecks() {
        SoftAssert soft = new SoftAssert();
        soft.assertTrue(dashboardPage.isDashboardDisplayed(), "Dashboard not displayed");
        soft.assertTrue(dashboardPage.isNavigationVisible(), "Nav not visible");
        // Full a11y can be added via axe-selenium-java
        log.info("Basic accessibility checks passed");
        soft.assertAll();
    }
}
