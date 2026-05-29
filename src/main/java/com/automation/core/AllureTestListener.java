package com.automation.core;

import com.automation.config.DriverManager;
import com.automation.utils.ScreenshotUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG listener that captures screenshots on test failure
 * and attaches them to the Allure report automatically.
 */
public class AllureTestListener implements ITestListener {

    private static final Logger log = LogManager.getLogger(AllureTestListener.class);

    @Override
    public void onTestFailure(ITestResult result) {
        log.error("Test failed: {} — capturing screenshot", result.getMethod().getMethodName());
        WebDriver driver = DriverManager.getDriver();
        if (driver != null) {
            ScreenshotUtils.captureAndAttach(driver, "FAILURE_" + result.getMethod().getMethodName());
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        log.info(">>> Test started: {}", result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("<<< Test passed: {}", result.getMethod().getMethodName());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn("--- Test skipped: {}", result.getMethod().getMethodName());
    }
}
