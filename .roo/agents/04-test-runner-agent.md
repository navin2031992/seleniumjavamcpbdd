# Agent: Test Runner & Analyst

**Roo Code mode slug:** `test-runner`
**Trigger:** Use this agent to run Maven tests, read results, investigate failures, and propose fixes.

---

## What this agent does

1. Runs tests via Maven with the right flags
2. Reads logs and screenshots when tests fail
3. Identifies root causes (broken locator, timing, wrong assertion)
4. Proposes and applies specific fixes
5. Reruns until green

---

## How to use

### Run smoke tests
```
@test-runner

Run the smoke tests
```

### Run specific scenarios
```
@test-runner

Run all @login tests on the staging environment with Chrome headless
```

### Investigate a failure
```
@test-runner

The login scenario is failing. Investigate and fix it.
```

### Run and generate report
```
@test-runner

Run full regression and open the Allure report
```

---

## Command reference

```bash
# Basic run (Chrome, dev, headed)
mvn test '-Dcucumber.filter.tags=@smoke'

# Run specific tag
mvn test '-Dcucumber.filter.tags=@login'

# Run on staging headless
mvn test -Pstaging -Dheadless=true

# Run specific browser
mvn test -Dbrowser=firefox '-Dcucumber.filter.tags=@smoke'

# Run in parallel (4 threads)
mvn test -Pparallel -Dheadless=true

# Rerun only failed scenarios
mvn test -Dtest=FailedTestRunner

# Compile only (find errors before running)
mvn compile -q

# Generate Allure report
mvn allure:serve

# Run against Selenium Grid
mvn test -Dgrid.url=http://localhost:4444 -Dheadless=true
```

---

## Failure investigation workflow

When a scenario fails, the agent reads these files in order:

```
1. target/logs/errors.log          ← exception type and stack trace
2. target/logs/test-execution.log  ← full execution log with debug info
3. target/screenshots/             ← screenshot taken at moment of failure
4. target/cucumber-reports/cucumber.json ← step-by-step result with timing
```

### Common failure types and fixes

| Error | Root cause | Fix |
|---|---|---|
| `NoSuchElementException` | Locator is wrong or page not loaded | Update `@FindBy` or add `WaitUtils.waitForVisible()` |
| `TimeoutException` | Element took too long to appear | Increase `wait.explicit` in properties or add specific wait |
| `ElementClickInterceptedException` | Another element covers the button | Dismiss overlays first; framework auto-retries with JS click |
| `AssertionError: expected true but was false` | Element not visible when checked | Add `waitForVisible()` before assertion |
| `AmbiguousStepDefinitionsException` | Same step text in two step files | Remove the duplicate step definition |
| `StaleElementReferenceException` | Page reloaded after element was found | Re-find the element; use `WaitUtils.waitForPageLoad()` |
| `SessionNotCreatedException` | Chrome version mismatch | WebDriverManager handles automatically; check Chrome version |

---

## Output format

After running tests, the agent reports:

```
Results Summary
───────────────────────────────────────
Total scenarios : 15
  ✅ Passed     : 13
  ❌ Failed     :  2
  ⏭  Skipped   :  0

Execution time  : 4m 32s

Failed scenarios:
  1. Login › Successful login with valid credentials
     Error: NoSuchElementException on .dashboard-title
     Screenshot: target/screenshots/FINAL_FAILURE_Successful_login_20241201_143022.png
     
  2. Dashboard › Navigation menu contains expected items
     Error: AssertionError: Navigation missing "Settings"

Proposed fixes:
  1. LoginPage.java: update @FindBy(css = ".dashboard-title") 
     → @FindBy(css = "[data-testid='welcome-message']")
  2. dashboard.feature: change "Settings" to "Account Settings" to match app
```

---

## Files this agent reads
- `target/logs/errors.log`
- `target/logs/test-execution.log`
- `target/cucumber-reports/cucumber.json`
- `target/screenshots/*.png`
- Any failing page object or step definition file

## Files this agent may write (with your approval)
- Any page object with a broken locator
- Any step definition with a wrong assertion
- `src/test/resources/config/*.properties` (to adjust timeouts)
