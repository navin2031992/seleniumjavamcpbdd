package com.automation.pages;

import com.automation.core.BasePage;
import com.automation.utils.WaitUtils;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Login Page — works with any standard username/password login form.
 *
 * Locators use relative XPaths and semantic attributes (aria-label, name, id)
 * so they remain stable across minor UI refactors.
 */
public class LoginPage extends BasePage {

    // ── Locators ───────────────────────────────────────────────────────────────

    @FindBy(css = "input[type='email'], input[name='username'], input[id*='email'], input[id*='user'], input[placeholder*='email' i], input[placeholder*='username' i]")
    private WebElement usernameField;

    @FindBy(css = "input[type='password'], input[name='password'], input[id*='pass']")
    private WebElement passwordField;

    @FindBy(css = "button[type='submit'], input[type='submit'], button[id*='login' i], button[id*='signin' i], button[data-testid='login-button']")
    private WebElement loginButton;

    @FindBy(css = ".error, .alert-danger, [role='alert'], [data-testid='error-message'], .login-error")
    private WebElement errorMessage;

    @FindBy(css = "a[href*='forgot'], a[href*='reset'], a[id*='forgot' i]")
    private WebElement forgotPasswordLink;

    @FindBy(css = "a[href*='register'], a[href*='signup'], a[id*='register' i]")
    private WebElement registerLink;

    @FindBy(id = "remember-me")
    private WebElement rememberMeCheckbox;

    // ── Constructor ───────────────────────────────────────────────────────────

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    @Step("Navigate to login page")
    public LoginPage open() {
        navigateTo(config.getBaseUrl() + config.get("login.path", "/login"));
        WaitUtils.waitForPageLoad(driver);
        log.info("Login page opened: {}", getCurrentUrl());
        return this;
    }

    @Step("Enter username: {username}")
    public LoginPage enterUsername(String username) {
        clearAndType(waitForVisible(usernameField), username);
        return this;
    }

    @Step("Enter password")
    public LoginPage enterPassword(String password) {
        clearAndType(waitForVisible(passwordField), password);
        return this;
    }

    @Step("Click Login button")
    public void clickLogin() {
        click(loginButton);
        WaitUtils.waitForPageLoad(driver);
    }

    @Step("Login with username: {username}")
    public DashboardPage loginAs(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
        return new DashboardPage(driver);
    }

    @Step("Login with invalid credentials: {username}")
    public LoginPage loginWithInvalidCredentials(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
        return this;
    }

    @Step("Check 'Remember Me' checkbox")
    public LoginPage checkRememberMe() {
        if (isDisplayed(rememberMeCheckbox) && !isSelected(rememberMeCheckbox)) {
            click(rememberMeCheckbox);
        }
        return this;
    }

    @Step("Click 'Forgot Password' link")
    public void clickForgotPassword() {
        click(forgotPasswordLink);
    }

    @Step("Click 'Register' link")
    public void clickRegister() {
        click(registerLink);
    }

    // ── Assertions / State ────────────────────────────────────────────────────

    @Step("Get login error message")
    public String getErrorMessage() {
        return getText(waitForVisible(errorMessage));
    }

    public boolean isErrorDisplayed() {
        return isDisplayed(By.cssSelector(".error, .alert-danger, [role='alert']"));
    }

    public boolean isLoginPageDisplayed() {
        return isDisplayed(By.cssSelector("input[type='password']"));
    }

    public boolean isForgotPasswordLinkDisplayed() {
        return isDisplayed(forgotPasswordLink);
    }

    public boolean isUsernameFieldDisplayed() {
        return isDisplayed(usernameField);
    }

    public boolean isPasswordFieldDisplayed() {
        return isDisplayed(passwordField);
    }
}
