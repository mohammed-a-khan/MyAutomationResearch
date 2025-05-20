Feature: User Login
  As a user
  I want to be able to log in to the application
  So that I can access my account

  Background:
    Given the user is on the login page

  @smoke @authentication
  Scenario: Successful login with valid credentials
    When the user logs in with username "validUser" and password "validPass"
    Then the login should be successful

  @smoke @authentication
  Scenario: Failed login with invalid credentials
    When the user logs in with username "invalidUser" and password "invalidPass"
    Then the login should fail
    And an error message should be displayed

  @smoke @authentication
  Scenario Outline: Login with various credentials
    When the user enters username "<username>" and password "<password>"
    And the user clicks the login button
    Then the login should <result>
    And the error message should contain "<error_message>"

    Examples:
      | username    | password    | result        | error_message         |
      | validUser   | validPass   | be successful |                       |
      | invalidUser | invalidPass | fail          | Invalid credentials   |
      | validUser   | wrongPass   | fail          | Invalid password      |
      | emptyUser   |             | fail          | Username is required  |
      |             | somePass    | fail          | Username is required  |