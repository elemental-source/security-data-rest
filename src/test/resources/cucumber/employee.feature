Feature: the get call to list all employees
  Scenario: client makes call to GET /employees
    Given the client init a call
    When the client calls /employees
    Then the client receives status code of 200
    And the client receives content type application/hal+json
