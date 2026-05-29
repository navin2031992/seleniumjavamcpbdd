package com.automation.core;

import com.automation.config.ConfigManager;
import com.automation.config.DriverManager;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

/**
 * Base class for non-Cucumber TestNG tests.
 * For Cucumber scenarios, Hooks.java handles driver lifecycle.
 */
@Listeners(com.automation.core.AllureTestListener.class)
public abstract class BaseTest {

    protected final Logger log = LogManager.getLogger(getClass());
    protected final ConfigManager config = ConfigManager.getInstance();

    protected WebDriver driver;

    @BeforeMethod(alwaysRun = true)
    public void setUp(ITestResult result) {
        log.info("====== Starting test: {} ======", result.getMethod().getMethodName());
        driver = DriverManager.getDriver();
        Allure.parameter("Browser", config.getBrowser());
        Allure.parameter("Environment", config.get("env", "dev"));
        Allure.parameter("Base URL", config.getBaseUrl());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            log.error("Test FAILED: {}", result.getMethod().getMethodName());
        } else {
            log.info("Test PASSED: {}", result.getMethod().getMethodName());
        }
        DriverManager.quitDriver();
        log.info("====== Test complete ======\n");
    }
}
