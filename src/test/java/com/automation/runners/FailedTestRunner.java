package com.automation.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Rerun runner — reruns only the scenarios that failed in the last run.
 * The rerun file is produced by TestRunner's "rerun:target/failed-scenarios.txt" plugin.
 *
 * Execute:  mvn test -Dtest=FailedTestRunner
 */
@CucumberOptions(
    features = "@target/failed-scenarios.txt",
    glue     = {"com.automation.hooks", "com.automation.steps"},
    plugin   = {
        "pretty",
        "html:target/cucumber-reports-rerun/index.html",
        "json:target/cucumber-reports-rerun/cucumber.json",
        "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
    },
    monochrome = true
)
public class FailedTestRunner extends AbstractTestNGCucumberTests {}
