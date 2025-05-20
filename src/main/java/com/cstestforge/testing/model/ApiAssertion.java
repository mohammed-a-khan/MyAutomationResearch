package com.cstestforge.testing.model;

/**
 * Represents an assertion to validate an API response
 */
public class ApiAssertion {
    
    private String id;
    private String type;
    private String property;
    private String operator;
    private String expected;
    
    /**
     * Default constructor
     */
    public ApiAssertion() {
        // Default constructor
    }
    
    /**
     * Constructor with all fields
     * 
     * @param id Unique assertion ID
     * @param type Assertion type
     * @param property Property to check
     * @param operator Comparison operator
     * @param expected Expected value
     */
    public ApiAssertion(String id, String type, String property, String operator, String expected) {
        this.id = id;
        this.type = type;
        this.property = property;
        this.operator = operator;
        this.expected = expected;
    }
    
    /**
     * Get the unique ID for this assertion
     * 
     * @return Assertion ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the unique ID for this assertion
     * 
     * @param id Assertion ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the assertion type
     * 
     * @return Assertion type (status, header, body, jsonPath, responseTime)
     */
    public String getType() {
        return type;
    }
    
    /**
     * Set the assertion type
     * 
     * @param type Assertion type (status, header, body, jsonPath, responseTime)
     */
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * Get the property to check
     * 
     * @return Property name
     */
    public String getProperty() {
        return property;
    }
    
    /**
     * Set the property to check
     * 
     * @param property Property name
     */
    public void setProperty(String property) {
        this.property = property;
    }
    
    /**
     * Get the comparison operator
     * 
     * @return Operator (=, !=, >, <, >=, <=, contains, not_contains, exists, not_exists, matches)
     */
    public String getOperator() {
        return operator;
    }
    
    /**
     * Set the comparison operator
     * 
     * @param operator Operator (=, !=, >, <, >=, <=, contains, not_contains, exists, not_exists, matches)
     */
    public void setOperator(String operator) {
        this.operator = operator;
    }
    
    /**
     * Get the expected value
     * 
     * @return Expected value
     */
    public String getExpected() {
        return expected;
    }
    
    /**
     * Set the expected value
     * 
     * @param expected Expected value
     */
    public void setExpected(String expected) {
        this.expected = expected;
    }
} 