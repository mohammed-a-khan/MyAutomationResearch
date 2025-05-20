package com.cstestforge.codegen.model.event;

/**
 * Represents an assertion event in a test
 */
public class AssertionEvent extends Event {
    public enum AssertType {
        EQUALS,
        NOT_EQUALS,
        CONTAINS,
        NOT_CONTAINS,
        TRUE,
        FALSE,
        NULL,
        NOT_NULL,
        EMPTY,
        NOT_EMPTY,
        GREATER_THAN,
        LESS_THAN,
        GREATER_THAN_OR_EQUAL,
        LESS_THAN_OR_EQUAL,
        MATCHES_REGEX,
        ELEMENT_PRESENT,
        ELEMENT_NOT_PRESENT,
        ELEMENT_VISIBLE,
        ELEMENT_NOT_VISIBLE,
        ELEMENT_ENABLED,
        ELEMENT_DISABLED,
        CUSTOM
    }
    
    private AssertType assertType;
    private String message; // Assertion message
    private String actualExpression; // Left side of assertion
    private String expectedExpression; // Right side of assertion
    private String customAssertion; // For custom assertions
    private boolean soft; // Whether this is a soft assertion
    
    public AssertionEvent() {
        super("assertion", "Assertion");
        this.soft = false;
    }
    
    public AssertionEvent(AssertType assertType, String actualExpression, String expectedExpression, String message) {
        this();
        this.assertType = assertType;
        this.actualExpression = actualExpression;
        this.expectedExpression = expectedExpression;
        this.message = message;
    }
    
    public AssertionEvent(AssertType assertType, String actualExpression, String message) {
        this();
        this.assertType = assertType;
        this.actualExpression = actualExpression;
        this.message = message;
    }
    
    // Getters and Setters
    public AssertType getAssertType() {
        return assertType;
    }

    public void setAssertType(AssertType assertType) {
        this.assertType = assertType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getActualExpression() {
        return actualExpression;
    }

    public void setActualExpression(String actualExpression) {
        this.actualExpression = actualExpression;
    }

    public String getExpectedExpression() {
        return expectedExpression;
    }

    public void setExpectedExpression(String expectedExpression) {
        this.expectedExpression = expectedExpression;
    }

    public String getCustomAssertion() {
        return customAssertion;
    }

    public void setCustomAssertion(String customAssertion) {
        this.customAssertion = customAssertion;
    }

    public boolean isSoft() {
        return soft;
    }

    public void setSoft(boolean soft) {
        this.soft = soft;
    }

    @Override
    public String toCode(String language, String framework, int indentLevel) {
        // This will be implemented by the code generation service
        return null;
    }
} 