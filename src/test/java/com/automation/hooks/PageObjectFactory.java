package com.automation.hooks;

import com.automation.config.DriverManager;
import com.automation.pages.DashboardPage;
import com.automation.pages.LoginPage;
import org.openqa.selenium.WebDriver;

/**
 * PicoContainer object factory — supplies shared page objects to all step
 * definition classes within the same scenario scope.
 *
 * Cucumber-PicoContainer automatically discovers this class and injects
 * LoginPage / DashboardPage into any step class that declares them as
 * constructor parameters.
 */
public class PageObjectFactory {

    private final WebDriver driver;
    private final LoginPage loginPage;
    private final DashboardPage dashboardPage;

    public PageObjectFactory() {
        this.driver = DriverManager.getDriver();
        this.loginPage = new LoginPage(driver);
        this.dashboardPage = new DashboardPage(driver);
    }

    public LoginPage getLoginPage()         { return loginPage; }
    public DashboardPage getDashboardPage() { return dashboardPage; }
    public WebDriver getDriver()            { return driver; }
}
