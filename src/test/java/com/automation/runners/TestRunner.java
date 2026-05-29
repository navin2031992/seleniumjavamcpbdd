package com.automation.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * Primary Cucumber test runner.
 * Runs all features tagged with @smoke and @regression.
 *
 * Execute:  mvn test -Denv=dev -Dbrowser=chrome
 * Specific: mvn test -Dcucumber.filter.tags="@smoke"
 */
@CucumberOptions(
    features  = "src/test/resources/features",
    glue      = {"com.automation.hooks", "com.automation.steps"},
    tags      = "not @wip and not @manual",
    plugin    = {
        "pretty",
        "html:target/cucumber-reports/index.html",
        "json:target/cucumber-reports/cucumber.json",
        "junit:target/cucumber-reports/cucumber.xml",
        "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm",
        "rerun:target/failed-scenarios.txt"
    },
    monochrome   = true,
    dryRun       = false,
    publish      = false
)
public class TestRunner extends AbstractTestNGCucumberTests {

    // Single-threaded runner (parallel is in ParallelTestRunner)
    @Override
    @DataProvider(parallel = false)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
