package com.automation.steps;

import com.automation.config.ConfigManager;
import com.automation.pages.DashboardPage;
import com.automation.pages.LoginPage;
import io.cucumber.java.en.*;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;

/**
 * Step definitions for login-related feature files.
 *
 * Uses PicoContainer constructor injection so pages share the
 * same WebDriver instance as the hooks (no static state).
 */
public class LoginSteps {

    private static final Logger log = LogManager.getLogger(LoginSteps.class);
    private final ConfigManager config = ConfigManager.getInstance();

    private final LoginPage loginPage;
    private final DashboardPage dashboardPage;

    // PicoContainer injects page objects via PageObjectFactory
    public LoginSteps(com.automation.hooks.PageObjectFactory factory) {
        this.loginPage = factory.getLoginPage();
        this.dashboardPage = factory.getDashboardPage();
    }

    // ── Given ─────────────────────────────────────────────────────────────────

    @Given("I am on the login page")
    @Step("Open the login page")
    public void iAmOnTheLoginPage() {
        loginPage.open();
        Assert.assertTrue(loginPage.isLoginPageDisplayed(),
            "Login page is not displayed — check base URL: " + config.getBaseUrl());
        log.info("Login page displayed successfully");
    }

    @Given("I am logged in as {string} with password {string}")
    public void iAmLoggedInAs(String username, String password) {
        loginPage.open();
        loginPage.loginAs(username, password);
        Assert.assertTrue(dashboardPage.isUserLoggedIn(),
            "Login failed for user: " + username);
        log.info("Pre-condition: logged in as {}", username);
    }

    // ── When ──────────────────────────────────────────────────────────────────

    @When("I enter username {string}")
    @Step("Enter username: {username}")
    public void iEnterUsername(String username) {
        loginPage.enterUsername(username);
    }

    @When("I enter password {string}")
    @Step("Enter password")
    public void iEnterPassword(String password) {
        loginPage.enterPassword(password);
    }

    @When("I enter valid credentials")
    public void iEnterValidCredentials() {
        loginPage.enterUsername(config.get("test.user.email", "test@example.com"));
        loginPage.enterPassword(config.get("test.user.password", "Password123!"));
    }

    @When("I enter invalid credentials")
    public void iEnterInvalidCredentials() {
        loginPage.enterUsername("invalid.user@example.com");
        loginPage.enterPassword("WrongPassword!");
    }

    @When("I enter username {string} and password {string}")
    public void iEnterUsernameAndPassword(String username, String password) {
        loginPage.enterUsername(username);
        loginPage.enterPassword(password);
    }

    @When("I click the Login button")
    @Step("Click Login button")
    public void iClickLoginButton() {
        loginPage.clickLogin();
    }

    @When("I click the Forgot Password link")
    public void iClickForgotPassword() {
        loginPage.clickForgotPassword();
    }

    @When("I check the Remember Me checkbox")
    public void iCheckRememberMe() {
        loginPage.checkRememberMe();
    }

    @When("I submit the login form with username {string} and password {string}")
    public void iSubmitLoginForm(String username, String password) {
        loginPage.loginAs(username, password);
    }

    // ── Then ──────────────────────────────────────────────────────────────────

    @Then("I should be redirected to the dashboard")
    @Step("Verify dashboard is displayed")
    public void iShouldBeRedirectedToDashboard() {
        dashboardPage.verifyLoaded();
        Assert.assertTrue(dashboardPage.isDashboardDisplayed(),
            "Expected to be on dashboard but current URL is: " + dashboardPage.getCurrentUrl());
        log.info("Successfully redirected to dashboard");
    }

    @Then("I should see a welcome message")
    public void iShouldSeeWelcomeMessage() {
        String message = dashboardPage.getWelcomeMessage();
        Assert.assertNotNull(message, "Welcome message is null");
        Assert.assertFalse(message.isEmpty(), "Welcome message is empty");
        log.info("Welcome message displayed: {}", message);
    }

    @Then("I should see an error message")
    public void iShouldSeeAnErrorMessage() {
        Assert.assertTrue(loginPage.isErrorDisplayed(),
            "Expected an error message but none was displayed");
    }

    @Then("I should see the error message {string}")
    public void iShouldSeeErrorMessage(String expectedError) {
        Assert.assertTrue(loginPage.isErrorDisplayed(),
            "Error message not displayed");
        String actual = loginPage.getErrorMessage();
        Assert.assertTrue(actual.contains(expectedError),
            "Error message '" + actual + "' does not contain '" + expectedError + "'");
        log.info("Error message verified: {}", actual);
    }

    @Then("I should remain on the login page")
    public void iShouldRemainOnLoginPage() {
        Assert.assertTrue(loginPage.isLoginPageDisplayed(),
            "Expected to remain on login page but URL is: " + loginPage.getCurrentUrl());
    }

    @Then("the login page should display username and password fields")
    public void loginPageShouldHaveFields() {
        SoftAssert soft = new SoftAssert();
        soft.assertTrue(loginPage.isUsernameFieldDisplayed(), "Username field not visible");
        soft.assertTrue(loginPage.isPasswordFieldDisplayed(), "Password field not visible");
        soft.assertAll();
    }

    @Then("the Forgot Password link should be visible")
    public void forgotPasswordLinkShouldBeVisible() {
        Assert.assertTrue(loginPage.isForgotPasswordLinkDisplayed(),
            "Forgot Password link is not visible");
    }

    @Then("I should see my username {string} displayed")
    public void usernameShouldBeDisplayed(String expectedName) {
        String actualName = dashboardPage.getUserName();
        Assert.assertTrue(actualName.contains(expectedName),
            "Expected username '" + expectedName + "' but got '" + actualName + "'");
    }
}
