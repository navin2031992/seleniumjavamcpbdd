package com.automation.pages;

import com.automation.core.BasePage;
import com.automation.utils.WaitUtils;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Dashboard / Home Page — represents the authenticated landing page.
 * Locators use semantic and role-based selectors for maximum portability.
 */
public class DashboardPage extends BasePage {

    // ── Locators ───────────────────────────────────────────────────────────────

    @FindBy(css = "[data-testid='welcome-message'], .welcome, .dashboard-title, h1.page-title")
    private WebElement welcomeMessage;

    @FindBy(css = "[data-testid='user-name'], .user-name, .profile-name, .account-name")
    private WebElement userNameDisplay;

    @FindBy(css = "nav, [role='navigation'], .navbar, .sidebar")
    private WebElement navigationMenu;

    @FindBy(css = "nav a, .nav-item a, .sidebar-item a, [role='navigation'] a")
    private List<WebElement> navigationLinks;

    @FindBy(css = "[data-testid='logout'], a[href*='logout'], button[id*='logout' i], .logout-btn")
    private WebElement logoutButton;

    @FindBy(css = ".notification-badge, .notification-count, [data-testid='notifications']")
    private WebElement notificationBadge;

    @FindBy(css = ".profile-avatar, .user-avatar, [data-testid='profile-menu'], [aria-label*='profile' i]")
    private WebElement profileMenu;

    @FindBy(css = "[data-testid='search-input'], input[type='search'], input[placeholder*='search' i]")
    private WebElement searchBar;

    @FindBy(css = ".breadcrumb, [aria-label='breadcrumb'], nav[aria-label='breadcrumb']")
    private WebElement breadcrumb;

    @FindBy(css = "[data-testid='loading'], .spinner, .loading-overlay")
    private WebElement loadingIndicator;

    // ── Constructor ───────────────────────────────────────────────────────────

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    @Step("Verify dashboard is loaded")
    public DashboardPage verifyLoaded() {
        WaitUtils.waitForPageLoad(driver);
        WaitUtils.waitForSpinnerToDisappear(driver);
        log.info("Dashboard loaded at URL: {}", getCurrentUrl());
        return this;
    }

    @Step("Click logout")
    public LoginPage logout() {
        log.info("Logging out");
        scrollTo(waitForVisible(logoutButton));
        click(logoutButton);
        WaitUtils.waitForPageLoad(driver);
        return new LoginPage(driver);
    }

    @Step("Navigate to menu item: {menuItem}")
    public DashboardPage clickNavigationItem(String menuItem) {
        navigationLinks.stream()
            .filter(link -> link.getText().trim().equalsIgnoreCase(menuItem))
            .findFirst()
            .ifPresentOrElse(
                link -> { click(link); waitForPageLoad(); },
                () -> { throw new RuntimeException("Menu item not found: " + menuItem); }
            );
        return this;
    }

    @Step("Open profile menu")
    public DashboardPage openProfileMenu() {
        click(profileMenu);
        return this;
    }

    @Step("Search for: {query}")
    public DashboardPage search(String query) {
        WaitUtils.waitForVisible(driver, searchBar);
        clearAndType(searchBar, query);
        searchBar.submit();
        waitForPageLoad();
        return this;
    }

    // ── State / Assertions ────────────────────────────────────────────────────

    @Step("Get welcome message text")
    public String getWelcomeMessage() {
        return getText(waitForVisible(welcomeMessage));
    }

    @Step("Get displayed user name")
    public String getUserName() {
        return getText(waitForVisible(userNameDisplay));
    }

    @Step("Get all navigation link labels")
    public List<String> getNavigationItems() {
        return navigationLinks.stream()
            .map(WebElement::getText)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    @Step("Get notification count")
    public int getNotificationCount() {
        if (!isDisplayed(notificationBadge)) return 0;
        try {
            return Integer.parseInt(getText(notificationBadge));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean isDashboardDisplayed() {
        return isDisplayed(welcomeMessage) || isDisplayed(navigationMenu);
    }

    public boolean isUserLoggedIn() {
        return !isDisplayed(By.cssSelector("input[type='password']"))
            && isDashboardDisplayed();
    }

    public boolean isNavigationVisible() {
        return isDisplayed(navigationMenu);
    }

    public boolean isSearchBarPresent() {
        return isDisplayed(searchBar);
    }

    public boolean isNotificationBadgeVisible() {
        return isDisplayed(notificationBadge);
    }

    public String getBreadcrumbText() {
        return isDisplayed(breadcrumb) ? getText(breadcrumb) : "";
    }
}
