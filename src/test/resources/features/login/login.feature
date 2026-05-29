@login @authentication
Feature: User Login
  # Covers Jira: AUTH-001 | Type: Story | Priority: High
  # As a registered user, I want to log in with my credentials
  # so that I can access my personalised dashboard.

  Background:
    Given I am on the login page

  # ─────────────── POSITIVE SCENARIOS ───────────────────────────────────────

  @smoke @regression @positive
  Scenario: Successful login with valid credentials
    When I enter username "test@example.com"
    And I enter password "Password123!"
    And I click the Login button
    Then I should be redirected to the dashboard
    And I should see a welcome message

  @regression @positive
  Scenario: Login with remembered credentials redirects to dashboard
    When I enter username "test@example.com"
    And I enter password "Password123!"
    And I check the Remember Me checkbox
    And I click the Login button
    Then I should be redirected to the dashboard
    And the URL should contain "/dashboard"

  @regression @positive
  Scenario Outline: Login with multiple valid user roles
    When I enter username "<email>"
    And I enter password "<password>"
    And I click the Login button
    Then I should be redirected to the dashboard
    And I should see my username "<display_name>" displayed

    Examples:
      | email                  | password      | display_name |
      | admin@example.com      | Admin@123!    | Admin        |
      | manager@example.com    | Manager@123!  | Manager      |
      | user@example.com       | User@123!     | User         |

  # ─────────────── NEGATIVE SCENARIOS ───────────────────────────────────────

  @regression @negative
  Scenario: Login fails with invalid password
    When I enter username "test@example.com"
    And I enter password "WrongPassword!"
    And I click the Login button
    Then I should see an error message
    And I should remain on the login page

  @regression @negative
  Scenario: Login fails with unregistered email
    When I enter username "notregistered@example.com"
    And I enter password "AnyPassword1!"
    And I click the Login button
    Then I should see an error message
    And I should remain on the login page

  @regression @negative
  Scenario: Login fails with empty credentials
    When I click the Login button
    Then I should see an error message
    And I should remain on the login page

  @regression @negative
  Scenario: Login fails with empty password
    When I enter username "test@example.com"
    And I click the Login button
    Then I should see an error message

  @regression @negative
  Scenario: Login fails with empty username
    When I enter password "Password123!"
    And I click the Login button
    Then I should see an error message

  # ─────────────── BOUNDARY SCENARIOS ───────────────────────────────────────

  @regression @boundary
  Scenario: Login with maximum length password
    When I enter username "test@example.com"
    And I enter password "ThisIsAVeryLongPasswordThatMeetsMaximumLengthRequirements123!"
    And I click the Login button
    Then I should see an error message

  @regression @boundary
  Scenario: Login with SQL injection attempt (security check)
    When I enter username "' OR '1'='1"
    And I enter password "' OR '1'='1"
    And I click the Login button
    Then I should see an error message
    And I should remain on the login page

  @regression @boundary
  Scenario: Login with XSS attempt (security check)
    When I enter username "<script>alert('xss')</script>"
    And I enter password "Password123!"
    And I click the Login button
    Then I should not see "<script>" on the page
    And I should remain on the login page

  # ─────────────── UI / UX SCENARIOS ────────────────────────────────────────

  @smoke @ui
  Scenario: Login page displays required elements
    Then the login page should display username and password fields
    And the Forgot Password link should be visible

  @regression @ui
  Scenario: Forgot password link navigates correctly
    When I click the Forgot Password link
    Then the URL should contain "forgot"
