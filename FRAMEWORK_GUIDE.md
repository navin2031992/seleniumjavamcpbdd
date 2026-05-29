# Universal BDD Selenium Framework — Complete Beginner's Guide

> **Who is this guide for?**
> Anyone new to this framework — whether you are a manual tester stepping into automation,
> a developer setting up tests for the first time, or an architect evaluating the stack.
> No prior Selenium or Cucumber experience is assumed.

---

## Table of Contents

1. [What This Framework Does](#1-what-this-framework-does)
2. [Technology Stack At a Glance](#2-technology-stack-at-a-glance)
3. [Prerequisites — What You Need Installed](#3-prerequisites)
4. [Project Structure Explained](#4-project-structure-explained)
5. [Step 1 — Clone and Configure](#5-step-1--clone-and-configure)
6. [Step 2 — Point It at Your Application](#6-step-2--point-it-at-your-application)
7. [Step 3 — Run Your First Test](#7-step-3--run-your-first-test)
8. [Writing Your Own Tests (BDD)](#8-writing-your-own-tests-bdd)
9. [Page Object Model — Adding a New Page](#9-page-object-model--adding-a-new-page)
10. [AI Agents — Auto-Generate Tests from Jira](#10-ai-agents--auto-generate-tests-from-jira)
11. [XPath Registry — Never Hunt for Locators Again](#11-xpath-registry--never-hunt-for-locators-again)
12. [MCP Selenium Integration](#12-mcp-selenium-integration)
13. [Running Tests in Different Modes](#13-running-tests-in-different-modes)
14. [Understanding the Allure Report](#14-understanding-the-allure-report)
15. [Integrating with Any Application](#15-integrating-with-any-application)
16. [CI/CD Pipeline (GitHub Actions)](#16-cicd-pipeline-github-actions)
17. [Docker — Run Without Installing Anything Locally](#17-docker--run-without-installing-anything-locally)
18. [Common Errors and Fixes](#18-common-errors-and-fixes)
19. [Quick-Reference Cheat Sheet](#19-quick-reference-cheat-sheet)

---

## 1. What This Framework Does

This framework lets you write and run browser automation tests in plain English (Gherkin/BDD).
You describe **what** the application should do, and the framework handles **how** to test it.

```
You write:                          Framework does:
──────────────────────────────      ──────────────────────────────────────────
Scenario: Successful login          Opens Chrome
  Given I am on the login page  →   Navigates to https://yourapp.com/login
  When I enter username "alice" →   Finds the username field, types "alice"
  And I enter password "P@ss1"  →   Finds the password field, types "P@ss1"
  And I click the Login button  →   Clicks the Login button
  Then I should see a welcome   →   Checks for a welcome message on screen
       message
```

On top of that, the **AI agents** can:
- Read a Jira story → automatically write the test scenarios
- Open your application → automatically discover all the buttons and inputs
- Generate the Java code → you don't have to write step definitions from scratch

---

## 2. Technology Stack At a Glance

| Layer | Technology | Purpose |
|---|---|---|
| Test language | **Gherkin** (`.feature` files) | Human-readable test scenarios |
| BDD runner | **Cucumber 7** | Connects Gherkin to Java code |
| Browser control | **Selenium 4** | Drives Chrome / Firefox / Edge |
| Driver setup | **WebDriverManager** | Downloads browser drivers automatically |
| Test runner | **TestNG 7** | Executes tests, supports parallel runs |
| AI test generation | **Claude (Anthropic)** | Reads Jira, writes test cases and code |
| AI browser bridge | **MCP Selenium** | Node.js server Claude uses to control the browser |
| Reports | **Allure 2** | Beautiful HTML reports with screenshots |
| Jira integration | **Jira REST API** | Pulls acceptance criteria from tickets |
| Logging | **Log4j 2** | Console + rolling file logs |
| Build tool | **Maven** | Dependency management, test execution |
| Containers | **Docker + Selenium Grid** | Headless cloud execution |
| CI | **GitHub Actions** | Automated runs on every push |

---

## 3. Prerequisites

### 3.1 Required (must have)

| Tool | Minimum Version | Check command | Download |
|---|---|---|---|
| Java JDK | 17 | `java -version` | https://adoptium.net |
| Maven | 3.8 | `mvn -version` | https://maven.apache.org |
| Chrome browser | Latest | — | https://google.com/chrome |
| Git | Any | `git --version` | https://git-scm.com |

### 3.2 Optional (for specific features)

| Tool | Version | Required for |
|---|---|---|
| Node.js + npm | 18+ | MCP Selenium AI agent (`mcp.enabled=true`) |
| Docker Desktop | Latest | Grid-based and containerised runs |
| Allure CLI | 2.25+ | Opening Allure reports locally |

### 3.3 Quick verification

Open a terminal and run:

```bash
java -version      # Should print: openjdk 17.x.x
mvn -version       # Should print: Apache Maven 3.x.x
git --version      # Should print: git version 2.x.x
```

If any of these fail, install the missing tool before continuing.

---

## 4. Project Structure Explained

```
javaseleniumbdd/
│
├── pom.xml                         ← Maven: all libraries are declared here
│
├── src/
│   ├── main/java/com/automation/
│   │   │
│   │   ├── agents/                 ← AI-powered agents
│   │   │   ├── AgentOrchestrator   ← Runs the full Jira→Tests pipeline
│   │   │   ├── JiraAgent           ← Fetches stories from Jira
│   │   │   ├── TestGeneratorAgent  ← Asks Claude to write test cases
│   │   │   ├── XPathCaptureAgent   ← Auto-discovers element locators
│   │   │   └── ScriptGeneratorAgent← Writes .feature files and Java code
│   │   │
│   │   ├── config/
│   │   │   ├── ConfigManager       ← Reads application.properties
│   │   │   └── DriverManager       ← Creates/manages the browser (WebDriver)
│   │   │
│   │   ├── core/
│   │   │   ├── BasePage            ← All page classes extend this
│   │   │   └── BaseTest            ← All non-Cucumber tests extend this
│   │   │
│   │   ├── mcp/
│   │   │   └── MCPSeleniumClient   ← Java ↔ Node.js MCP server bridge
│   │   │
│   │   └── utils/
│   │       ├── WaitUtils           ← Smart waits (never Thread.sleep!)
│   │       ├── ElementUtils        ← Click, type, scroll helpers
│   │       ├── ScreenshotUtils     ← Auto-screenshot on failure
│   │       └── ReportUtils         ← Allure report helpers
│   │
│   └── test/
│       ├── java/com/automation/
│       │   ├── hooks/
│       │   │   ├── Hooks           ← Before/After scenario setup
│       │   │   └── PageObjectFactory ← Provides page instances to steps
│       │   │
│       │   ├── pages/              ← One class per page of your app
│       │   │   ├── LoginPage       ← Login page actions & assertions
│       │   │   └── DashboardPage   ← Dashboard page actions & assertions
│       │   │
│       │   ├── steps/              ← Connects Gherkin text to Java code
│       │   │   ├── CommonSteps     ← Steps used on any page
│       │   │   ├── LoginSteps      ← Login-specific steps
│       │   │   └── DashboardSteps  ← Dashboard-specific steps
│       │   │
│       │   └── runners/
│       │       ├── TestRunner          ← Normal sequential run
│       │       ├── ParallelTestRunner  ← Runs scenarios in parallel
│       │       └── FailedTestRunner    ← Reruns only failed scenarios
│       │
│       └── resources/
│           ├── features/           ← Test scenarios written in Gherkin
│           │   ├── login/login.feature
│           │   └── dashboard/dashboard.feature
│           ├── config/
│           │   ├── application.properties  ← Base settings
│           │   ├── dev.properties          ← Dev overrides
│           │   └── staging.properties      ← Staging overrides
│           ├── testdata/users.json         ← Test user accounts
│           └── xpath-registry/elements.json← Saved element locators
│
├── Dockerfile                      ← Build a test-runner Docker image
├── docker-compose.yml              ← Selenium Grid + Allure server
└── .github/workflows/ci.yml        ← GitHub Actions pipeline
```

**The golden rule:** You will spend most of your time in:
- `src/test/resources/features/` — writing test scenarios
- `src/test/java/.../pages/` — writing page objects
- `src/test/java/.../steps/` — writing step definitions
- `src/test/resources/config/` — configuring your target application

---

## 5. Step 1 — Clone and Configure

### 5.1 Get the project

```bash
# If you have the project as a ZIP, extract it.
# If it is in a git repository:
git clone <your-repo-url>
cd javaseleniumbdd
```

### 5.2 Download all dependencies

```bash
mvn dependency:resolve -q
```

Maven will download everything (~200 MB on first run). You will see a progress log.
Subsequent runs use the local cache and are instant.

### 5.3 Verify the build compiles

```bash
mvn compile -q
```

If this prints nothing and exits 0 — you are ready.

---

## 6. Step 2 — Point It at Your Application

Open `src/test/resources/config/application.properties` and edit these two lines:

```properties
# ── The URL of your application ───────────────────────────
app.base.url=https://your-application.com

# ── The path to the login page (relative to base URL) ────
login.path=/login
```

### 6.1 Environment-specific settings

The framework has three environment files. You switch between them using `-Denv=<name>`.

| File | Used when | Switch |
|---|---|---|
| `application.properties` | Always loaded first (base settings) | — |
| `dev.properties` | Local development | `-Denv=dev` (default) |
| `staging.properties` | Staging/QA server | `-Denv=staging` |
| `prod.properties` | Production (smoke only) | `-Denv=prod` |

**Example:** Override just the URL for your local setup in `dev.properties`:

```properties
app.base.url=http://localhost:3000
```

Now `mvn test -Denv=dev` will use `http://localhost:3000`.

### 6.2 Test user accounts

Edit `src/test/resources/testdata/users.json` with real credentials for your application:

```json
{
  "users": [
    {
      "role": "standard",
      "email": "myuser@mycompany.com",
      "password": "RealPassword123!",
      "displayName": "My User"
    }
  ]
}
```

Also update `application.properties`:
```properties
test.user.email=myuser@mycompany.com
test.user.password=RealPassword123!
```

> **Security note:** Never commit real passwords to git.
> Use environment variables instead:
> ```bash
> export test.user.password=RealPassword123!
> mvn test
> ```

---

## 7. Step 3 — Run Your First Test

### 7.1 Run all smoke tests

```bash
mvn test -Denv=dev '-Dcucumber.filter.tags=@smoke'
```

This opens Chrome, runs all scenarios tagged `@smoke`, and closes Chrome.
You will see output like:

```
[INFO] --- maven-surefire-plugin ---
║ Scenario: Successful login with valid credentials
║ Tags:     [@login, @smoke, @authentication]
[INFO] ✓ Given I am on the login page          [PASSED]
[INFO] ✓ When I enter username "test@example.com"  [PASSED]
[INFO] ✓ And I enter password "Password123!"   [PASSED]
[INFO] ✓ And I click the Login button          [PASSED]
[INFO] ✓ Then I should be redirected to the dashboard [PASSED]
...
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```

### 7.2 Open the Allure report

```bash
mvn allure:serve
```

This opens a browser at `http://localhost:PORT` with a rich HTML report showing:
- Pass/fail per scenario
- Screenshot on every failed step
- Timeline of execution
- Jira ticket links (if configured)

### 7.3 Run in headless mode (no browser window)

```bash
mvn test -Dheadless=true '-Dcucumber.filter.tags=@smoke'
```

---

## 8. Writing Your Own Tests (BDD)

### 8.1 Anatomy of a Feature File

```gherkin
@featureTag                         ← Tags the whole feature
Feature: User Registration          ← Description of what is being tested

  Background:                       ← Runs before EVERY scenario in this file
    Given I am on the login page

  @smoke @positive                  ← Tags for this specific scenario
  Scenario: Successful registration with valid data
    Given I am on the registration page
    When I enter username "newuser@example.com"
    And I enter password "SecurePass1!"
    And I click the Register button
    Then I should be redirected to the dashboard
    And I should see a welcome message
```

**Tag rules:**
| Tag | Meaning |
|---|---|
| `@smoke` | Critical path — runs in every environment including production |
| `@regression` | Full regression — runs on staging/CI |
| `@positive` | Happy path test |
| `@negative` | Error/failure test |
| `@wip` | Work in progress — **excluded from all runs** |
| `@manual` | Manual only — **excluded from automation** |

### 8.2 Create a new feature file

1. Create a folder under `src/test/resources/features/registration/`
2. Create `registration.feature` in that folder
3. Write your scenarios (copy the structure above)

### 8.3 Step definitions — connecting Gherkin to Java

Each Gherkin line maps to a Java method annotated with `@Given`, `@When`, or `@Then`.

**Example:** You wrote in the feature file:
```gherkin
When I click the Register button
```

You write in a `RegistrationSteps.java` file:
```java
@When("I click the Register button")
public void iClickRegisterButton() {
    registrationPage.clickRegister();  // calls your page object
}
```

### 8.4 Step parameters

Use `{string}` to pass data from Gherkin into Java:

```gherkin
# Feature file
When I enter email "user@example.com"
```

```java
// Steps file
@When("I enter email {string}")
public void iEnterEmail(String email) {
    registrationPage.enterEmail(email);
}
```

### 8.5 Scenario Outlines — data-driven testing

Run the same scenario with multiple data sets:

```gherkin
@regression
Scenario Outline: Login with different user roles
  Given I am on the login page
  When I enter username "<email>"
  And I enter password "<password>"
  And I click the Login button
  Then I should be redirected to the dashboard

  Examples:
    | email               | password    |
    | admin@example.com   | Admin@123!  |
    | user@example.com    | User@123!   |
    | viewer@example.com  | View@123!   |
```

This runs **3 separate scenarios** — one per row in the Examples table.

---

## 9. Page Object Model — Adding a New Page

The Page Object Model keeps your test code organised. Each page of your application has one Java class.

### 9.1 Create a new Page class

Create `src/test/java/com/automation/pages/RegistrationPage.java`:

```java
package com.automation.pages;

import com.automation.core.BasePage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class RegistrationPage extends BasePage {

    // ── Locators ── find elements using CSS, ID, XPath, or name
    @FindBy(id = "email")
    private WebElement emailField;

    @FindBy(id = "password")
    private WebElement passwordField;

    @FindBy(css = "button[type='submit']")
    private WebElement registerButton;

    @FindBy(css = ".error-message")
    private WebElement errorMessage;

    // ── Constructor — always call super(driver)
    public RegistrationPage(WebDriver driver) {
        super(driver);       // BasePage sets up PageFactory automatically
    }

    // ── Actions — what a user can DO on this page
    @Step("Enter email: {email}")
    public RegistrationPage enterEmail(String email) {
        clearAndType(emailField, email);
        return this;        // method chaining
    }

    @Step("Enter password")
    public RegistrationPage enterPassword(String password) {
        clearAndType(passwordField, password);
        return this;
    }

    @Step("Click Register button")
    public DashboardPage clickRegister() {
        click(registerButton);
        waitForPageLoad();
        return new DashboardPage(driver);
    }

    // ── Assertions — what you can CHECK on this page
    public boolean isErrorDisplayed() {
        return isDisplayed(errorMessage);
    }

    public String getErrorMessage() {
        return getText(errorMessage);
    }
}
```

**Locator strategies** — choose in this order of preference:

| Strategy | Example | When to use |
|---|---|---|
| `@FindBy(id = "...")` | `@FindBy(id = "email")` | Always prefer if element has an `id` |
| `@FindBy(name = "...")` | `@FindBy(name = "email")` | Good for form inputs |
| `@FindBy(css = "...")` | `@FindBy(css = ".btn-primary")` | When no id/name; use CSS class or attribute |
| `@FindBy(xpath = "...")` | `@FindBy(xpath = "//button[text()='Login']")` | Last resort; fragile but powerful |

### 9.2 Create Step Definitions for the new page

Create `src/test/java/com/automation/steps/RegistrationSteps.java`:

```java
package com.automation.steps;

import com.automation.hooks.PageObjectFactory;
import com.automation.pages.RegistrationPage;
import io.cucumber.java.en.*;
import org.testng.Assert;

public class RegistrationSteps {

    private final RegistrationPage registrationPage;

    // PicoContainer provides pages via the factory
    public RegistrationSteps(PageObjectFactory factory) {
        // factory.getDriver() gives you the shared WebDriver
        this.registrationPage = new RegistrationPage(factory.getDriver());
    }

    @Given("I am on the registration page")
    public void iAmOnTheRegistrationPage() {
        registrationPage.navigateTo(
            com.automation.config.ConfigManager.getInstance().getBaseUrl() + "/register"
        );
    }

    @When("I enter email {string}")
    public void iEnterEmail(String email) {
        registrationPage.enterEmail(email);
    }

    @When("I click the Register button")
    public void iClickRegisterButton() {
        registrationPage.clickRegister();
    }

    @Then("I should see a registration error")
    public void iShouldSeeRegistrationError() {
        Assert.assertTrue(registrationPage.isErrorDisplayed(),
            "Expected registration error message but none appeared");
    }
}
```

### 9.3 Update PageObjectFactory (if sharing page between step classes)

If multiple step files need the same page, add a getter to `PageObjectFactory.java`:

```java
// In PageObjectFactory.java — add alongside existing pages:
private final RegistrationPage registrationPage;

public PageObjectFactory() {
    this.driver = DriverManager.getDriver();
    this.loginPage = new LoginPage(driver);
    this.dashboardPage = new DashboardPage(driver);
    this.registrationPage = new RegistrationPage(driver);   // ← add this
}

public RegistrationPage getRegistrationPage() { return registrationPage; }
```

---

## 10. AI Agents — Auto-Generate Tests from Jira

> **Prerequisites for this section:**
> - Jira account + API token
> - Anthropic API key (get one at https://console.anthropic.com)

### 10.1 Configure credentials

In `application.properties` (or as environment variables):

```properties
# Jira
jira.base.url=https://yourcompany.atlassian.net
jira.username=your.email@company.com
jira.api.token=your_jira_api_token
jira.project.key=PROJ

# Anthropic Claude
anthropic.api.key=sk-ant-...
anthropic.model=claude-sonnet-4-6
```

**Or via environment variables (recommended for CI):**
```bash
export ANTHROPIC_API_KEY=sk-ant-...
export JIRA_API_TOKEN=your_token
export JIRA_USERNAME=your.email@company.com
```

### 10.2 Generate tests from ALL open Jira tickets

```bash
mvn exec:java -Dexec.mainClass="com.automation.agents.AgentOrchestrator"
```

**What happens:**

```
[1/4] Fetching Jira tickets...
      Found 8 open tickets

[2/4] Generating test cases with AI...
      PROJ-101: "User Login" → 5 test cases (2 positive, 2 negative, 1 boundary)
      PROJ-102: "Password Reset" → 4 test cases
      ...

[3/4] Capturing UI element locators...
      Captured 12 login page elements

[4/4] Writing feature files and step definitions...
      ✓ PROJ-101 → Feature: src/test/resources/features/login/PROJ-101.feature
      ✓ PROJ-102 → Feature: src/test/resources/features/login/PROJ-102.feature

Pipeline Complete! Run: mvn test -Pdev
```

### 10.3 Generate tests from a single ticket

```bash
mvn exec:java \
  -Dexec.mainClass="com.automation.agents.AgentOrchestrator" \
  -Dexec.args="--ticket=PROJ-101"
```

### 10.4 What the AI generates

For a Jira story like:
> **PROJ-101: User Login**
> As a registered user, I want to log in with my email and password.
> **Acceptance Criteria:**
> - Valid credentials redirect to dashboard
> - Invalid credentials show error message
> - Empty fields show validation

The AI produces a complete `.feature` file:

```gherkin
@proj101
Feature: User Login
  # Jira: PROJ-101 | Type: Story | Priority: High

  @smoke @positive
  Scenario: Successful login with valid credentials
    Given I am on the login page
    When I enter username "test@example.com"
    And I enter password "Password123!"
    And I click the Login button
    Then I should be redirected to the dashboard
    And I should see a welcome message

  @regression @negative
  Scenario: Login fails with invalid password
    When I enter username "test@example.com"
    And I enter password "WrongPassword"
    And I click the Login button
    Then I should see an error message
    And I should remain on the login page

  @regression @boundary
  Scenario Outline: Login with various invalid inputs
    When I enter username "<email>"
    And I enter password "<password>"
    And I click the Login button
    Then I should see an error message

    Examples:
      | email          | password |
      |                | Pass123! |
      | test@email.com |          |
      | not-an-email   | Pass123! |
```

---

## 11. XPath Registry — Never Hunt for Locators Again

The XPath registry (`src/test/resources/xpath-registry/elements.json`) is a central store of all
element locators. The `XPathCaptureAgent` builds it by scanning your application automatically.

### 11.1 Auto-capture elements from a running page

```java
// In a test setup or main method:
XPathCaptureAgent agent = new XPathCaptureAgent();
WebDriver driver = DriverManager.getDriver();
driver.get("https://yourapp.com/login");

List<XPathElement> elements = agent.capturePageElements(driver, "LoginPage");
// ↑ Scans the page and saves all inputs, buttons, links to elements.json
```

### 11.2 Validate the registry (find broken locators)

```java
Map<String, Boolean> report = agent.validateRegistry(driver);
report.entrySet().stream()
    .filter(e -> !e.getValue())
    .forEach(e -> System.out.println("BROKEN: " + e.getKey()));
```

### 11.3 Look up a locator in your tests

```java
Optional<XPathElement> el = agent.findByKey("LOGIN_USERNAME_INPUT");
el.ifPresent(e -> System.out.println(e.getBestLocator()));
// Prints: css=input[name='username']
```

### 11.4 Registry file format

Each entry in `elements.json` looks like this:

```json
{
  "key": "LOGIN_USERNAME_INPUT",
  "page": "LoginPage",
  "description": "Username / Email address input field",
  "xpath": "//input[@type='email' or @name='username']",
  "cssSelector": "input[type='email'], input[name='username']",
  "id": null,
  "elementType": "INPUT",
  "capturedFrom": "/login",
  "verified": true,
  "failureCount": 0
}
```

**You can also add entries manually** — just follow the same format.

---

## 12. MCP Selenium Integration

MCP (Model Context Protocol) lets Claude AI directly control the browser.
This enables real-time, AI-driven test generation against your live application.

### 12.1 Prerequisites

```bash
# Check Node.js is installed
node --version   # Should print v18.x.x or higher

# Install the MCP Selenium server globally
npm install -g @angiejones/mcp-selenium
```

### 12.2 Enable in configuration

```properties
# application.properties
mcp.enabled=true
mcp.node.path=node
```

### 12.3 Using MCPSeleniumClient in code

```java
import com.automation.mcp.MCPSessionManager;
import com.automation.mcp.MCPSeleniumClient;

MCPSeleniumClient mcp = MCPSessionManager.getInstance().getClient();

// Navigate to a page
mcp.navigate("https://yourapp.com/login");

// Fill in form fields
mcp.type("input[name='email']", "user@example.com");
mcp.type("input[name='password']", "Password123!");

// Click a button
mcp.click("button[type='submit']");

// Take a screenshot
mcp.screenshot();

// Get page title
String title = (String) mcp.getTitle().getResult();
```

### 12.4 MCP vs Standard Selenium

| Feature | Standard Selenium | MCP Selenium |
|---|---|---|
| Who uses it | Your Java step code | Claude AI agent |
| Speed | Fast | Slightly slower (Node.js bridge) |
| Use case | All regular tests | AI-driven exploration / generation |
| Setup | Zero extra setup | Requires Node.js |

Use standard Selenium for your tests. Enable MCP when using AI agents to explore an application.

---

## 13. Running Tests in Different Modes

### 13.1 By tag

```bash
# Only smoke tests
mvn test '-Dcucumber.filter.tags=@smoke'

# Only regression tests
mvn test '-Dcucumber.filter.tags=@regression'

# Login tests only
mvn test '-Dcucumber.filter.tags=@login'

# Smoke AND login
mvn test '-Dcucumber.filter.tags=@smoke and @login'

# Exclude work-in-progress
mvn test '-Dcucumber.filter.tags=not @wip'
```

### 13.2 By environment

```bash
# Run against local dev server
mvn test -Denv=dev

# Run against staging (headless Chrome)
mvn test -Denv=staging -Dheadless=true

# Run against production (smoke only — NEVER run full regression on prod)
mvn test -Denv=prod '-Dcucumber.filter.tags=@smoke'
```

### 13.3 By browser

```bash
# Chrome (default)
mvn test -Dbrowser=chrome

# Firefox
mvn test -Dbrowser=firefox

# Edge
mvn test -Dbrowser=edge
```

### 13.4 Parallel execution (faster)

```bash
# Run 4 scenarios at the same time
mvn test -Pparallel -Dheadless=true
```

Thread count is set in `src/test/resources/testng-parallel.xml`.
Change `data-provider-thread-count="4"` to however many threads you want.

### 13.5 Rerun only failed scenarios

```bash
# Step 1: Run all tests (failures are saved to target/failed-scenarios.txt)
mvn test

# Step 2: Rerun only the ones that failed
mvn test -Dtest=FailedTestRunner
```

### 13.6 Against a Selenium Grid

```bash
# Point to your Selenium Grid hub
mvn test -Dgrid.url=http://selenium-grid:4444 -Dheadless=true
```

---

## 14. Understanding the Allure Report

### 14.1 Generate and open the report

```bash
mvn allure:serve
# Opens automatically in your browser
```

### 14.2 What each section means

```
ALLURE REPORT
├── Overview          ← Total pass/fail count, trend chart, environment info
├── Categories        ← Groups failures by type (assertion error, element not found, etc.)
├── Suites            ← All Feature files → Scenarios → Steps, with timing
├── Timeline          ← Parallel execution visualisation (which thread ran what)
├── Behaviors         ← Organised by feature/epic/story tags
└── Packages          ← Organised by Java package
```

### 14.3 How to read a failed scenario

1. Click on a failed scenario (red)
2. Click on the failed step (the red step)
3. You will see:
   - **Screenshot** — exactly what the browser showed when it failed
   - **Page Source** — the full HTML at the time of failure
   - **Log** — detailed debug messages
   - **Error message** — what the assertion expected vs what it found

### 14.4 Allure attachments added automatically

| Event | What is attached |
|---|---|
| Step failure | Screenshot of the browser |
| Step failure | Full page HTML source |
| Scenario completion | Browser, environment, URL parameters |
| Jira tag present | Clickable link to the Jira ticket |

---

## 15. Integrating with Any Application

This framework has zero hard-coded assumptions about your application.
Here is the complete checklist for integrating it with a new app.

### Checklist: Integrating a new application

```
□ Step 1 — Update configuration
  Edit src/test/resources/config/dev.properties:
    app.base.url=https://your-new-app.com
    login.path=/signin          (or whatever your login path is)
    test.user.email=testuser@newapp.com
    test.user.password=TestPass123!

□ Step 2 — Identify pages to test
  List the pages in your application that need testing.
  Common ones: Login, Registration, Dashboard, Profile, Settings, Checkout

□ Step 3 — Create a Page Object for each page
  Copy src/test/java/com/automation/pages/LoginPage.java as a template.
  Change class name, update @FindBy locators to match your HTML.

□ Step 4 — Write feature files
  Create .feature files under src/test/resources/features/<pagename>/
  Write Given/When/Then scenarios for each user story.

□ Step 5 — Write step definitions
  Create Steps classes under src/test/java/com/automation/steps/
  Map each Gherkin line to a Java method that calls your page object.

□ Step 6 — Add page to PageObjectFactory (if shared across step classes)
  In PageObjectFactory.java, add the new page like LoginPage and DashboardPage.

□ Step 7 — Run and verify
  mvn test -Denv=dev '-Dcucumber.filter.tags=@smoke'
```

### 15.1 Application with custom authentication (SSO, OAuth, 2FA)

If your application uses SSO or OAuth (e.g., login via Google), you need to handle the
redirect flow. In your LoginPage, capture the redirect and add the OAuth steps:

```java
@Step("Complete SSO login")
public DashboardPage completeSSOLogin(String username, String password) {
    // Click "Login with Google" or your SSO provider button
    click(ssoButton);
    // Handle the provider login page (different URL)
    WaitUtils.waitForUrlContains(driver, "accounts.google.com");
    type(emailField, username);
    click(nextButton);
    type(passwordField, password);
    click(signInButton);
    // Wait to return to your app
    WaitUtils.waitForUrlContains(driver, config.getBaseUrl());
    return new DashboardPage(driver);
}
```

### 15.2 Application with API-based authentication (set cookie/token directly)

For API-first apps, bypass the UI login to speed up tests:

```java
// In LoginPage or a test utility:
public DashboardPage loginViaApi(String username, String password) {
    // Call your auth API to get a token
    String token = callAuthApi(username, password);

    // Inject the token as a cookie so the browser is authenticated
    driver.get(config.getBaseUrl());  // must be on the domain first
    driver.manage().addCookie(
        new Cookie("auth_token", token, config.getBaseUrl(), "/", null)
    );

    // Now navigate to dashboard directly (already logged in)
    navigateTo(config.getBaseUrl() + "/dashboard");
    return new DashboardPage(driver);
}
```

### 15.3 Application with iframes

Some older applications embed content in iframes. Use the BasePage iframe methods:

```java
// In your Page Object:
public void fillFormInsideIframe() {
    switchToFrame(iframeElement);     // enter the iframe
    type(inputField, "value");        // interact normally
    switchToDefaultContent();         // exit the iframe
}
```

### 15.4 Application with dynamic/Angular/React loading

For Single Page Applications (SPAs), waits are critical. Use `WaitUtils`:

```java
// Wait for Angular to finish requests
WaitUtils.waitForAngular(driver);

// Wait for React/Vue loading spinner to disappear
WaitUtils.waitForSpinnerToDisappear(driver);

// Wait for AJAX to complete
WaitUtils.waitForAjax(driver);
```

### 15.5 Mobile/Responsive testing

```bash
# Test at tablet viewport
mvn test '-Dcucumber.filter.tags=@smoke' -Dheadless=true

# In your test, resize the window:
# Add this to a @Before step or Hooks.java:
# driver.manage().window().setSize(new Dimension(768, 1024));
```

---

## 16. CI/CD Pipeline (GitHub Actions)

### 16.1 What the pipeline does on every push

```
Push to any branch
       │
       ▼
┌──────────────┐
│  1. Compile  │  Just compiles the code — fast feedback
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  2. Smoke    │  Runs @smoke scenarios on Chrome
│   Tests      │  ~5 minutes
└──────┬───────┘
       │ (only if smoke passed)
       ▼
┌─────────────────────────────────┐
│  3. Regression (Chrome + Firefox)│  Runs in PARALLEL on both browsers
│     parallel matrix              │  ~15 minutes total (not 30)
└──────────────┬──────────────────┘
               │
               ▼
┌──────────────────┐
│  4. Allure Report │  Merges results from all jobs
│  (GitHub Pages)  │  Published to https://yourorg.github.io/repo
└──────────────────┘
```

### 16.2 Add secrets to GitHub

Go to your repository → Settings → Secrets and variables → Actions → New repository secret:

| Secret name | Value |
|---|---|
| `ANTHROPIC_API_KEY` | Your Claude API key |
| `JIRA_API_TOKEN` | Your Jira API token |
| `JIRA_BASE_URL` | `https://yourcompany.atlassian.net` |
| `JIRA_USERNAME` | `your.email@company.com` |

### 16.3 Trigger a manual run

In GitHub → Actions → "BDD Selenium CI" → "Run workflow":
- Choose environment (dev / staging / prod)
- Choose browser (chrome / firefox)
- Choose tag filter (`@smoke`, `@regression`, etc.)

### 16.4 View results

- GitHub Actions tab → click the run → click a job → see live logs
- GitHub Pages (after first merge to main) → see the Allure report

---

## 17. Docker — Run Without Installing Anything Locally

If you have Docker Desktop, you can run the entire framework (including the browser)
inside containers — no Chrome installation required.

### 17.1 Start the Selenium Grid

```bash
# Start the Grid (Hub + Chrome + Firefox)
docker-compose up -d selenium-hub chrome firefox

# Verify the Grid is healthy
# Open http://localhost:4444 in your browser — you should see the Grid UI
```

### 17.2 Run tests against the Grid

```bash
docker-compose --profile test run --rm test-runner
```

Or run locally pointing at the containerised Grid:

```bash
mvn test \
  -Dgrid.url=http://localhost:4444 \
  -Denv=staging \
  -Dheadless=true \
  '-Dcucumber.filter.tags=@smoke'
```

### 17.3 Watch tests running (VNC viewer)

Open your browser and go to `http://localhost:7900`.
You will see the Chrome container's desktop and can watch tests run in real time.
(Password: `secret`)

### 17.4 View the Allure report in Docker

```bash
docker-compose --profile reports up -d allure-server
# Open http://localhost:5050
```

### 17.5 Shut everything down

```bash
docker-compose down
```

---

## 18. Common Errors and Fixes

### Error: `SessionNotCreatedException: Chrome not found`

**Cause:** Chrome is not installed, or the version does not match the driver.

**Fix:**
```bash
# Let WebDriverManager handle it automatically (it does by default)
# If it still fails, force a specific version:
# In application.properties:
# wdm.chromeDriverVersion=121.0.6167.85
```

### Error: `Element not found: NoSuchElementException`

**Cause:** The locator is wrong, or the page hasn't fully loaded.

**Fix:**
```java
// Instead of finding immediately, wait for it:
WaitUtils.waitForVisible(driver, By.cssSelector(".my-element"));

// Or increase the explicit wait in application.properties:
wait.explicit=30
```

### Error: `AmbiguousStepDefinitionsException`

**Cause:** Two step definition methods match the same Gherkin step text.

**Fix:** Search all `@Given/@When/@Then` annotations for duplicate text. Make one more specific
or rename it. Every step text must be unique across ALL step definition files.

### Error: `CucumberException: Step undefined`

**Cause:** A Gherkin step has no matching Java method.

**Fix:** Look at the error message — Cucumber prints the exact snippet to add:
```java
// Cucumber prints:
@When("I click the {string} link")
public void iClickTheLink(String arg0) {
    // TODO: implement
}
// Copy this into your steps file and implement it.
```

### Error: `ElementClickInterceptedException`

**Cause:** Another element (e.g. a popup/cookie banner) is covering the button.

**Fix:** The framework handles this automatically via JS click fallback in `ElementUtils.click()`.
If it still fails, dismiss the overlay first:
```java
// In your page object, add a method to close popups:
public void dismissCookieBanner() {
    if (isDisplayed(By.id("cookie-accept"))) {
        click(driver.findElement(By.id("cookie-accept")));
    }
}
```

### Error: `WebDriverException: invalid session id`

**Cause:** The test tried to use a WebDriver after it was already closed.

**Fix:** This usually means a `@After` hook ran before the step finished.
Ensure `DriverManager.quitDriver()` is only called in `Hooks.java`, not elsewhere.

### Error: `java.lang.OutOfMemoryError` during parallel runs

**Fix:** Increase Maven's memory in `pom.xml`:
```xml
<argLine>-Xmx2g ...</argLine>
<!-- Change 2g to 4g if you have the RAM -->
```

Or reduce the parallel thread count in `testng-parallel.xml`:
```xml
<suite thread-count="2" data-provider-thread-count="2">
```

---

## 19. Quick-Reference Cheat Sheet

### Maven commands

```bash
# Compile only
mvn compile -q

# Run all smoke tests (Chrome, dev, headed)
mvn test '-Dcucumber.filter.tags=@smoke'

# Run all regression (Chrome, staging, headless)
mvn test -Pstaging '-Dcucumber.filter.tags=@regression'

# Run in parallel (4 threads, headless)
mvn test -Pparallel -Dheadless=true

# Rerun only failed scenarios
mvn test -Dtest=FailedTestRunner

# Generate + open Allure report
mvn allure:serve

# Generate tests from Jira (requires API keys)
mvn exec:java -Dexec.mainClass="com.automation.agents.AgentOrchestrator"

# Generate for one Jira ticket
mvn exec:java -Dexec.mainClass="com.automation.agents.AgentOrchestrator" \
              -Dexec.args="--ticket=PROJ-101"
```

### Tag filter cheat sheet

```bash
# Run by tag
'-Dcucumber.filter.tags=@smoke'
'-Dcucumber.filter.tags=@regression'
'-Dcucumber.filter.tags=@login'

# Combine tags (AND)
'-Dcucumber.filter.tags=@smoke and @login'

# Exclude a tag (NOT)
'-Dcucumber.filter.tags=not @wip'

# Run everything except manual and wip
'-Dcucumber.filter.tags=not @wip and not @manual'
```

### Key file locations

| What | Where |
|---|---|
| Base configuration | `src/test/resources/config/application.properties` |
| Dev environment URL | `src/test/resources/config/dev.properties` |
| Feature scenarios | `src/test/resources/features/**/*.feature` |
| Element locators | `src/test/resources/xpath-registry/elements.json` |
| Test user data | `src/test/resources/testdata/users.json` |
| Page objects | `src/test/java/com/automation/pages/` |
| Step definitions | `src/test/java/com/automation/steps/` |
| Cucumber hooks | `src/test/java/com/automation/hooks/Hooks.java` |
| Test runner | `src/test/java/com/automation/runners/TestRunner.java` |
| Screenshots | `target/screenshots/` |
| Allure results | `target/allure-results/` |
| Cucumber HTML report | `target/cucumber-reports/index.html` |
| Execution logs | `target/logs/test-execution.log` |

### 5-minute checklist: adding a new test

```
□ 1. Create a .feature file under src/test/resources/features/<page>/
□ 2. Write Given/When/Then steps describing user actions
□ 3. Create or update the Page Object under src/test/java/.../pages/
□ 4. Add @FindBy locators for each element you interact with
□ 5. Create action methods on the page (clickLogin, enterEmail, etc.)
□ 6. Create or update Steps class under src/test/java/.../steps/
□ 7. Map each Gherkin step to a Java method that calls the page
□ 8. Run: mvn test '-Dcucumber.filter.tags=@your-new-tag'
□ 9. Open: mvn allure:serve to review results
```

---

*For questions or contributions, open an issue in the project repository.*
*Framework maintained by the Automation Architecture team.*
