package com.cstestforge.recorder.model.events;

import com.cstestforge.recorder.model.ElementInfo;
import com.cstestforge.recorder.model.RecordedEvent;
import com.cstestforge.recorder.model.RecordedEventType;

/**
 * Represents an input event recorded during a browser session.
 */
public class InputEvent extends RecordedEvent {
    
    private ElementInfo targetElement;
    private String inputValue;
    private String previousValue;
    private boolean isClearFirst;
    private boolean isPasswordField;
    private boolean isMasked;
    private InputType inputType;
    private boolean isFilePicker;
    private String[] selectedFiles;
    
    /**
     * Default constructor
     */
    public InputEvent() {
        super(RecordedEventType.INPUT);
    }
    
    /**
     * Constructor with target element and input value
     *
     * @param targetElement The element that received input
     * @param inputValue The value that was input
     */
    public InputEvent(ElementInfo targetElement, String inputValue) {
        super(RecordedEventType.INPUT);
        this.targetElement = targetElement;
        this.inputValue = inputValue;
        
        // Determine if this is a password field based on the element type
        if (targetElement != null && "password".equalsIgnoreCase(targetElement.getType())) {
            this.isPasswordField = true;
            this.isMasked = true;
        }
    }
    
    /**
     * Get the target element
     *
     * @return The target element
     */
    public ElementInfo getTargetElement() {
        return targetElement;
    }
    
    /**
     * Set the target element
     *
     * @param targetElement The target element
     */
    public void setTargetElement(ElementInfo targetElement) {
        this.targetElement = targetElement;
    }
    
    /**
     * Get the input value
     *
     * @return The input value
     */
    public String getInputValue() {
        return inputValue;
    }
    
    /**
     * Set the input value
     *
     * @param inputValue The input value
     */
    public void setInputValue(String inputValue) {
        this.inputValue = inputValue;
    }
    
    /**
     * Get the previous value
     *
     * @return The previous value
     */
    public String getPreviousValue() {
        return previousValue;
    }
    
    /**
     * Set the previous value
     *
     * @param previousValue The previous value
     */
    public void setPreviousValue(String previousValue) {
        this.previousValue = previousValue;
    }
    
    /**
     * Check if field should be cleared first
     *
     * @return True if field should be cleared first
     */
    public boolean isClearFirst() {
        return isClearFirst;
    }
    
    /**
     * Set whether field should be cleared first
     *
     * @param clearFirst True if field should be cleared first
     */
    public void setClearFirst(boolean clearFirst) {
        isClearFirst = clearFirst;
    }
    
    /**
     * Check if this is a password field
     *
     * @return True if this is a password field
     */
    public boolean isPasswordField() {
        return isPasswordField;
    }
    
    /**
     * Set whether this is a password field
     *
     * @param passwordField True if this is a password field
     */
    public void setPasswordField(boolean passwordField) {
        isPasswordField = passwordField;
    }
    
    /**
     * Check if the value should be masked in logs and reports
     *
     * @return True if the value should be masked
     */
    public boolean isMasked() {
        return isMasked;
    }
    
    /**
     * Set whether the value should be masked in logs and reports
     *
     * @param masked True if the value should be masked
     */
    public void setMasked(boolean masked) {
        isMasked = masked;
    }
    
    /**
     * Get the input type
     *
     * @return The input type
     */
    public InputType getInputType() {
        return inputType;
    }
    
    /**
     * Set the input type
     *
     * @param inputType The input type
     */
    public void setInputType(InputType inputType) {
        this.inputType = inputType;
    }
    
    /**
     * Check if this is a file picker input
     *
     * @return True if this is a file picker
     */
    public boolean isFilePicker() {
        return isFilePicker;
    }
    
    /**
     * Set whether this is a file picker input
     *
     * @param filePicker True if this is a file picker
     */
    public void setFilePicker(boolean filePicker) {
        isFilePicker = filePicker;
    }
    
    /**
     * Get the selected files (for file inputs)
     *
     * @return Array of selected file paths
     */
    public String[] getSelectedFiles() {
        return selectedFiles;
    }
    
    /**
     * Set the selected files (for file inputs)
     *
     * @param selectedFiles Array of selected file paths
     */
    public void setSelectedFiles(String[] selectedFiles) {
        this.selectedFiles = selectedFiles;
    }
    
    @Override
    public boolean isValid() {
        return targetElement != null && 
               targetElement.getBestSelector() != null && 
               (inputValue != null || (isFilePicker && selectedFiles != null && selectedFiles.length > 0));
    }
    
    @Override
    public String toHumanReadableDescription() {
        String element = targetElement != null ? targetElement.getBestSelector() : "unknown element";
        
        if (isFilePicker && selectedFiles != null) {
            return String.format("Upload %d file(s) to %s", selectedFiles.length, element);
        } else if (isPasswordField && isMasked) {
            return String.format("Enter password in %s", element);
        } else {
            String displayValue = inputValue;
            if (displayValue != null && displayValue.length() > 20) {
                displayValue = displayValue.substring(0, 17) + "...";
            }
            return String.format("Enter '%s' in %s", displayValue, element);
        }
    }
    
    /**
     * Enum representing types of input interactions
     */
    public enum InputType {
        TEXT,
        SELECT,
        CHECKBOX,
        RADIO,
        FILE,
        DATE,
        COLOR,
        RANGE,
        CONTENTEDITABLE
    }
} 