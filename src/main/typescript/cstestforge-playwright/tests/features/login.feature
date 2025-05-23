Feature: User Authentication
  As a user
  I want to be able to log in to the application
  So that I can access my account

  @smoke @authentication
  Scenario: Successful login with valid credentials
    Given I am on the login page
    When I enter "validuser" as username
    And I enter "password123" as password
    And I click the login button
    Then I should be redirected to the dashboard
    And I should see a welcome message

  @authentication
  Scenario: Failed login with invalid credentials
    Given I am on the login page
    When I enter "invaliduser" as username
    And I enter "wrongpassword" as password
    And I click the login button
    Then I should see an error message "Invalid username or password"
    And I should remain on the login page

  @authentication @data-driven
  Scenario Outline: Login with different user types
    Given I am on the login page
    When I enter "<username>" as username
    And I enter "<password>" as password
    And I click the login button
    Then the login should be "<result>"
    And I should see message "<message>"

    Examples:
      | username   | password    | result  | message                      |
      | admin      | admin123    | success | Welcome, Administrator       |
      | customer   | customer123 | success | Welcome back, Customer       |
      | locked     | locked123   | failure | Your account has been locked |
      | inactive   | inactive123 | failure | Account is inactive          |
