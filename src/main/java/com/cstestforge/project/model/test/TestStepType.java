package com.cstestforge.project.model.test;

/**
 * Enum representing different types of test steps.
 */
public enum TestStepType {
    // UI Test Steps
    NAVIGATION("Navigation"),
    CLICK("Click"),
    INPUT("Input"),
    SELECT("Select"),
    ASSERTION("Assertion"),
    WAIT("Wait"),
    SCREENSHOT("Screenshot"),
    SCROLL("Scroll"),
    HOVER("Hover"),
    DRAG_DROP("Drag and Drop"),
    
    // API Test Steps
    API_REQUEST("API Request"),
    API_RESPONSE_VALIDATION("API Response Validation"),
    API_AUTHENTICATION("API Authentication"),
    
    // Flow Control Steps
    CONDITION("Conditional Logic"),
    LOOP("Loop"),
    GROUP("Step Group"),
    TRY_CATCH("Try-Catch Block"),
    
    // Data Management Steps
    DATA_SOURCE("Data Source"),
    VARIABLE_SET("Set Variable"),
    VARIABLE_GET("Get Variable"),
    
    // General Steps
    COMMENT("Comment"),
    CUSTOM_CODE("Custom Code"),
    EXTERNAL_COMMAND("External Command");
    
    private final String displayName;
    
    TestStepType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
} 