# Agent: Test Script Writer

**Roo Code mode slug:** `test-script-writer`
**Trigger:** Use this agent after you have a feature file and an XPath registry entry.
It writes the Java code — Page Objects and Step Definitions.

---

## What this agent does

Given a feature file and element locators, this agent:
1. Creates a **Page Object** class (`src/test/java/com/automation/pages/<Name>Page.java`)
2. Creates a **Steps** class (`src/test/java/com/automation/steps/<Name>Steps.java`)
3. Updates `PageObjectFactory` if the new page needs to be shared

---

## How to use

### Create page object and steps for a feature file
```
@test-script-writer

Create the Page Object and Step Definitions for:
- Feature file: src/test/resources/features/registration/PROJ-101.feature
- Page name: RegistrationPage
- URL path: /register
```

### Create only the page object
```
@test-script-writer

Create a Page Object for the registration page.
Use locators from the xpath-registry for page "RegistrationPage"
```

### Create only the step definitions
```
@test-script-writer

Create step definitions for src/test/resources/features/checkout/checkout.feature
The page object CheckoutPage already exists.
```

---

## Page Object template pattern

The agent follows this exact structure:

```java
package com.automation.pages;

import com.automation.core.BasePage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class RegistrationPage extends BasePage {

    // ── Locators — prefer id > name > css > xpath ───────────
    @FindBy(id = "email")
    private WebElement emailField;

    @FindBy(id = "password")
    private WebElement passwordField;

    @FindBy(css = "button[type='submit']")
    private WebElement registerButton;

    @FindBy(css = ".error-message")
    private WebElement errorMessage;

    // ── Constructor ──────────────────────────────────────────
    public RegistrationPage(WebDriver driver) {
        super(driver);   // BasePage calls PageFactory.initElements
    }

    // ── Actions — what a user CAN DO on this page ────────────
    @Step("Enter email: {email}")
    public RegistrationPage enterEmail(String email) {
        clearAndType(emailField, email);
        return this;
    }

    @Step("Click Register")
    public DashboardPage clickRegister() {
        click(registerButton);
        waitForPageLoad();
        return new DashboardPage(driver);
    }

    // ── Assertions — what you can CHECK ─────────────────────
    public boolean isErrorDisplayed() {
        return isDisplayed(errorMessage);
    }

    public String getErrorText() {
        return getText(errorMessage);
    }
}
```

---

## Step Definitions template pattern

```java
package com.automation.steps;

import com.automation.hooks.PageObjectFactory;
import com.automation.pages.RegistrationPage;
import io.cucumber.java.en.*;
import org.testng.Assert;

public class RegistrationSteps {

    private final RegistrationPage registrationPage;

    // PicoContainer injects the factory — never create WebDriver directly here
    public RegistrationSteps(PageObjectFactory factory) {
        this.registrationPage = new RegistrationPage(factory.getDriver());
    }

    @Given("I am on the registration page")
    public void iAmOnRegistrationPage() {
        registrationPage.navigateTo(
            com.automation.config.ConfigManager.getInstance().getBaseUrl() + "/register"
        );
    }

    @When("I enter email {string}")
    public void iEnterEmail(String email) {
        registrationPage.enterEmail(email);
    }

    @When("I click the Register button")
    public void iClickRegister() {
        registrationPage.clickRegister();
    }

    @Then("I should see the registration error {string}")
    public void iShouldSeeRegistrationError(String expectedError) {
        Assert.assertTrue(registrationPage.isErrorDisplayed(),
            "No error message displayed");
        Assert.assertTrue(registrationPage.getErrorText().contains(expectedError),
            "Error text mismatch");
    }
}
```

---

## Rules the agent enforces

| Rule | Why |
|---|---|
| `@FindBy` locators prefer `id` over `css` over `xpath` | id-based locators are most stable |
| Action methods return the **next page** object | Enables fluent chaining in tests |
| Boolean inspection methods start with `is` or `has` | Consistent naming for assertions |
| No `Thread.sleep()` — use `WaitUtils` | Avoids flaky tests |
| No hard-coded URLs — use `ConfigManager.getInstance().getBaseUrl()` | Supports all environments |
| Step text must be unique across ALL step files | Prevents `AmbiguousStepDefinitionsException` |

---

## Available BasePage methods (free to use)

```java
// Navigation
navigateTo(url)         click(element)       clearAndType(element, text)

// Waits
waitForPageLoad()       waitForVisible(el)   waitForClickable(el)
waitForUrlContains(url)

// State
isDisplayed(element)    isEnabled(element)   isSelected(element)
getText(element)        getValue(element)

// Interaction
selectByText(dropdown, text)   hover(element)   scrollTo(element)
executeScript(script)          takeScreenshot(label)
```

---

## Files this agent reads
- `src/test/resources/features/<page>/*.feature`
- `src/test/resources/xpath-registry/elements.json`
- `src/test/java/com/automation/pages/*.java` (for naming conventions)
- `src/test/java/com/automation/steps/*.java` (to avoid duplicate step text)

## Files this agent writes
- `src/test/java/com/automation/pages/<Name>Page.java`
- `src/test/java/com/automation/steps/<Name>Steps.java`
- `src/test/java/com/automation/hooks/PageObjectFactory.java` (if new page added)
