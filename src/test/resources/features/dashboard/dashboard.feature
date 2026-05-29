@dashboard @authenticated
Feature: Dashboard Functionality
  # Covers Jira: DASH-001 | Type: Story | Priority: High
  # As a logged-in user, I want to see my personalised dashboard
  # with navigation and key information.

  Background:
    Given I am on the login page
    And I am logged in as "test@example.com" with password "Password123!"
    And I am on the dashboard

  # ─────────────── NAVIGATION SCENARIOS ─────────────────────────────────────

  @smoke @regression
  Scenario: Dashboard displays after successful login
    Then the dashboard should be displayed
    And the navigation menu should be visible
    And the URL should contain "/dashboard"

  @regression @positive
  Scenario: Navigation menu contains expected items
    Then the navigation should contain "Home"
    And the navigation should contain "Profile"
    And the navigation should contain "Settings"

  @regression @positive
  Scenario: Search bar is present on dashboard
    Then the search bar should be present

  # ─────────────── LOGOUT SCENARIOS ─────────────────────────────────────────

  @smoke @regression
  Scenario: User can log out successfully
    When I click the logout button
    Then I should be logged out and redirected to the login page
    And the URL should contain "/login"

  @regression @security
  Scenario: Accessing dashboard after logout shows login page
    When I click the logout button
    Then I should be logged out and redirected to the login page
    When I navigate to "/dashboard"
    Then I should remain on the login page

  # ─────────────── UI SCENARIOS ─────────────────────────────────────────────

  @smoke @ui
  Scenario: Dashboard passes basic display checks
    Then the dashboard should be displayed
    And the navigation menu should be visible
    And the page title should contain "Dashboard"

  @regression @ui
  Scenario: Dashboard accessibility check
    Then the dashboard should pass all accessibility checks
