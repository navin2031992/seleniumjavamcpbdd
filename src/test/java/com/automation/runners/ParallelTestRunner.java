package com.automation.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * Parallel Cucumber runner.
 * Runs scenarios in parallel threads — safe because DriverManager uses ThreadLocal.
 *
 * Execute:  mvn test -Pparallel -Denv=staging -Dbrowser=chrome -Dheadless=true
 *
 * Thread count is controlled by testng-parallel.xml (dataproviderthreadcount).
 */
@CucumberOptions(
    features  = "src/test/resources/features",
    glue      = {"com.automation.hooks", "com.automation.steps"},
    tags      = "not @wip and not @manual and not @serial",
    plugin    = {
        "pretty",
        "html:target/cucumber-reports-parallel/index.html",
        "json:target/cucumber-reports-parallel/cucumber.json",
        "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm",
        "rerun:target/failed-scenarios-parallel.txt"
    },
    monochrome = true
)
public class ParallelTestRunner extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
