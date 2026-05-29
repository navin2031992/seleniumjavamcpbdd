# Agent: Jira → BDD Test Writer

**Roo Code mode slug:** `jira-to-bdd`
**Trigger:** Use this agent when you have a Jira ticket key or a user story description and want to turn it into a Cucumber feature file.

---

## What this agent does

1. Reads a Jira ticket (or user story text you paste)
2. Extracts all acceptance criteria
3. Generates a complete `.feature` file with:
   - At least one `@smoke @positive` scenario (happy path)
   - Two or more `@regression @negative` scenarios (error cases)
   - One `@boundary` scenario (empty inputs, max length, invalid format)
   - A `Scenario Outline` when multiple data sets make the same point
4. Saves the file to the correct location in the project

---

## How to use

### Option A — Give it a Jira ticket key
```
@jira-to-bdd

Write BDD tests for PROJ-101
```
The agent reads your Jira configuration from `application.properties` and fetches the story.

### Option B — Paste the story directly
```
@jira-to-bdd

User story:
As a registered user I want to reset my password so that I can regain access
if I forget it.

Acceptance criteria:
- Given I am on the login page, when I click Forgot Password and enter my email,
  then I receive a reset link
- When the link expires (24h), clicking it shows an error
- Entering an unregistered email shows a "not found" message
```

---

## Output

The agent creates a file at:
```
src/test/resources/features/password/PROJ-101.feature
```

Example output:
```gherkin
@proj101 @password-reset
Feature: Password Reset
  # Jira: PROJ-101 | Type: Story | Priority: High

  Background:
    Given I am on the login page
    And I click the Forgot Password link

  @smoke @positive
  Scenario: User receives reset email with valid registered address
    When I enter email "registered@example.com"
    And I click the Send Reset Link button
    Then I should see a confirmation message
    And the URL should contain "/forgot-password/sent"

  @regression @negative
  Scenario: Unregistered email shows not-found error
    When I enter email "unknown@example.com"
    And I click the Send Reset Link button
    Then I should see the error message "No account found"

  @regression @negative
  Scenario: Expired reset link shows error
    When I navigate to an expired password reset link
    Then I should see the error message "This link has expired"

  @regression @boundary
  Scenario Outline: Invalid email format is rejected
    When I enter email "<email>"
    And I click the Send Reset Link button
    Then I should see an error message

    Examples:
      | email            |
      |                  |
      | notanemail       |
      | @nodomain.com    |
```

---

## Tags reference

| Tag | Meaning |
|---|---|
| `@smoke` | Run this in every environment — critical path |
| `@regression` | Run in staging CI — full coverage |
| `@positive` | Tests that should succeed |
| `@negative` | Tests for error/failure conditions |
| `@boundary` | Edge cases — empty, max length, invalid format |
| `@wip` | Work in progress — **excluded from all runs** |
| `@manual` | Not automatable — **excluded from runs** |

---

## Files this agent reads
- `src/test/resources/config/application.properties` — to know the app URL and Jira config
- `src/test/java/com/automation/steps/*.java` — to reuse existing step text
- Jira REST API (if configured)

## Files this agent writes
- `src/test/resources/features/<page>/<TICKET>.feature`
