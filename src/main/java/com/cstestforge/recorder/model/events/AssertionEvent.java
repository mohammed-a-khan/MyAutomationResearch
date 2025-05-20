package com.cstestforge.recorder.model.events;

import com.cstestforge.recorder.model.ElementInfo;
import com.cstestforge.recorder.model.RecordedEvent;
import com.cstestforge.recorder.model.RecordedEventType;

import java.util.Objects;

/**
 * Represents an assertion event recorded during a browser session.
 * This is used to create verifications of element states or page properties.
 */
public class AssertionEvent extends RecordedEvent {

    private ElementInfo targetElement;
    private AssertionType assertionType;
    private String expectedValue;
    private String actualValue;
    private boolean negated;
    private boolean isSoft;
    private String customMessage;
    private boolean isRegex;
    private boolean isCaseSensitive;
    private Double tolerance;
    private AssertionStatus status;

    /**
     * Default constructor
     */
    public AssertionEvent() {
        super(RecordedEventType.ASSERTION);
        this.isCaseSensitive = true;
        this.status = AssertionStatus.NOT_EXECUTED;
    }

    /**
     * Constructor with assertion type and target element
     *
     * @param assertionType The type of assertion
     * @param targetElement The element to assert against
     */
    public AssertionEvent(AssertionType assertionType, ElementInfo targetElement) {
        super(RecordedEventType.ASSERTION);
        this.assertionType = assertionType;
        this.targetElement = targetElement;
        this.isCaseSensitive = true;
        this.status = AssertionStatus.NOT_EXECUTED;
    }

    /**
     * Constructor with all basic parameters
     *
     * @param assertionType The type of assertion
     * @param targetElement The element to assert against
     * @param expectedValue The expected value
     * @param negated Whether the assertion is negated
     */
    public AssertionEvent(AssertionType assertionType, ElementInfo targetElement, String expectedValue, boolean negated) {
        this(assertionType, targetElement);
        this.expectedValue = expectedValue;
        this.negated = negated;
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
     * Get the assertion type
     *
     * @return The assertion type
     */
    public AssertionType getAssertionType() {
        return assertionType;
    }

    /**
     * Set the assertion type
     *
     * @param assertionType The assertion type
     */
    public void setAssertionType(AssertionType assertionType) {
        this.assertionType = assertionType;
    }

    /**
     * Get the expected value
     *
     * @return The expected value
     */
    public String getExpectedValue() {
        return expectedValue;
    }

    /**
     * Set the expected value
     *
     * @param expectedValue The expected value
     */
    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    /**
     * Get the actual value
     *
     * @return The actual value
     */
    public String getActualValue() {
        return actualValue;
    }

    /**
     * Set the actual value
     *
     * @param actualValue The actual value
     */
    public void setActualValue(String actualValue) {
        this.actualValue = actualValue;
    }

    /**
     * Check if the assertion is negated
     *
     * @return True if the assertion is negated
     */
    public boolean isNegated() {
        return negated;
    }

    /**
     * Set whether the assertion is negated
     *
     * @param negated True if the assertion is negated
     */
    public void setNegated(boolean negated) {
        this.negated = negated;
    }

    /**
     * Check if this is a soft assertion (test continues on failure)
     *
     * @return True if this is a soft assertion
     */
    public boolean isSoft() {
        return isSoft;
    }

    /**
     * Set whether this is a soft assertion
     *
     * @param soft True if this is a soft assertion
     */
    public void setSoft(boolean soft) {
        isSoft = soft;
    }

    /**
     * Get the custom message
     *
     * @return The custom message
     */
    public String getCustomMessage() {
        return customMessage;
    }

    /**
     * Set the custom message
     *
     * @param customMessage The custom message
     */
    public void setCustomMessage(String customMessage) {
        this.customMessage = customMessage;
    }

    /**
     * Check if the expected value is a regex pattern
     *
     * @return True if the expected value is a regex pattern
     */
    public boolean isRegex() {
        return isRegex;
    }

    /**
     * Set whether the expected value is a regex pattern
     *
     * @param regex True if the expected value is a regex pattern
     */
    public void setRegex(boolean regex) {
        isRegex = regex;
    }

    /**
     * Check if the comparison is case-sensitive
     *
     * @return True if the comparison is case-sensitive
     */
    public boolean isCaseSensitive() {
        return isCaseSensitive;
    }

    /**
     * Set whether the comparison is case-sensitive
     *
     * @param caseSensitive True if the comparison is case-sensitive
     */
    public void setCaseSensitive(boolean caseSensitive) {
        isCaseSensitive = caseSensitive;
    }

    /**
     * Get the tolerance value for numeric comparisons
     *
     * @return The tolerance value
     */
    public Double getTolerance() {
        return tolerance;
    }

    /**
     * Set the tolerance value for numeric comparisons
     *
     * @param tolerance The tolerance value
     */
    public void setTolerance(Double tolerance) {
        this.tolerance = tolerance;
    }

    /**
     * Get the assertion status
     *
     * @return The assertion status
     */
    public AssertionStatus getStatus() {
        return status;
    }

    /**
     * Set the assertion status
     *
     * @param status The assertion status
     */
    public void setStatus(AssertionStatus status) {
        this.status = status;
    }

    @Override
    public boolean isValid() {
        return assertionType != null && 
               (targetElement != null || 
                assertionType == AssertionType.URL || 
                assertionType == AssertionType.TITLE || 
                assertionType == AssertionType.CUSTOM_JAVASCRIPT);
    }

    /**
     * Evaluate the assertion against the provided actual value
     *
     * @param actualValue The actual value to compare against the expected value
     * @return True if the assertion passes, false otherwise
     */
    public boolean evaluate(String actualValue) {
        if (actualValue == null && expectedValue == null) {
            this.status = AssertionStatus.PASSED;
            return true;
        }
        
        if (actualValue == null || expectedValue == null) {
            this.status = AssertionStatus.FAILED;
            return false;
        }
        
        this.actualValue = actualValue;
        boolean result;
        
        switch (assertionType) {
            case EQUALS:
                if (isCaseSensitive) {
                    result = expectedValue.equals(actualValue);
                } else {
                    result = expectedValue.equalsIgnoreCase(actualValue);
                }
                break;
                
            case CONTAINS:
                if (isCaseSensitive) {
                    result = actualValue.contains(expectedValue);
                } else {
                    result = actualValue.toLowerCase().contains(expectedValue.toLowerCase());
                }
                break;
                
            case STARTS_WITH:
                if (isCaseSensitive) {
                    result = actualValue.startsWith(expectedValue);
                } else {
                    result = actualValue.toLowerCase().startsWith(expectedValue.toLowerCase());
                }
                break;
                
            case ENDS_WITH:
                if (isCaseSensitive) {
                    result = actualValue.endsWith(expectedValue);
                } else {
                    result = actualValue.toLowerCase().endsWith(expectedValue.toLowerCase());
                }
                break;
                
            case REGEX_MATCH:
                result = actualValue.matches(expectedValue);
                break;
                
            case GREATER_THAN:
            case LESS_THAN:
            case GREATER_THAN_OR_EQUALS:
            case LESS_THAN_OR_EQUALS:
                result = evaluateNumericComparison(actualValue);
                break;
                
            default:
                result = false;
                break;
        }
        
        // Handle negation
        if (negated) {
            result = !result;
        }
        
        this.status = result ? AssertionStatus.PASSED : AssertionStatus.FAILED;
        return result;
    }
    
    /**
     * Evaluate numeric comparison assertions
     *
     * @param actualValue The actual value as a string
     * @return True if the assertion passes, false otherwise
     */
    private boolean evaluateNumericComparison(String actualValue) {
        try {
            double actualNum = Double.parseDouble(actualValue);
            double expectedNum = Double.parseDouble(expectedValue);
            
            switch (assertionType) {
                case GREATER_THAN:
                    return actualNum > expectedNum;
                case LESS_THAN:
                    return actualNum < expectedNum;
                case GREATER_THAN_OR_EQUALS:
                    return actualNum >= expectedNum;
                case LESS_THAN_OR_EQUALS:
                    return actualNum <= expectedNum;
                case EQUALS:
                    if (tolerance == null) {
                        return actualNum == expectedNum;
                    } else {
                        return Math.abs(actualNum - expectedNum) <= tolerance;
                    }
                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public String toHumanReadableDescription() {
        StringBuilder description = new StringBuilder();
        
        if (negated) {
            description.append("Assert that ");
        } else {
            description.append("Assert that ");
        }
        
        switch (assertionType) {
            case VISIBLE:
                description.append(getElementDescription()).append(" is ");
                if (negated) description.append("not ");
                description.append("visible");
                break;
                
            case ENABLED:
                description.append(getElementDescription()).append(" is ");
                if (negated) description.append("not ");
                description.append("enabled");
                break;
                
            case SELECTED:
                description.append(getElementDescription()).append(" is ");
                if (negated) description.append("not ");
                description.append("selected");
                break;
                
            case PRESENT:
                description.append(getElementDescription()).append(" is ");
                if (negated) description.append("not ");
                description.append("present");
                break;
                
            case TEXT_EQUALS:
                description.append(getElementDescription()).append(" text ");
                if (negated) description.append("does not equal ");
                else description.append("equals ");
                description.append("'").append(expectedValue).append("'");
                break;
                
            case TEXT_CONTAINS:
                description.append(getElementDescription()).append(" text ");
                if (negated) description.append("does not contain ");
                else description.append("contains ");
                description.append("'").append(expectedValue).append("'");
                break;
                
            case ATTRIBUTE_EQUALS:
                description.append(getElementDescription()).append(" attribute ");
                description.append("'").append(expectedValue).append("'");
                if (negated) description.append(" does not equal ");
                else description.append(" equals ");
                description.append("'").append(actualValue).append("'");
                break;
                
            case URL:
                description.append("URL ");
                if (assertionType == AssertionType.CONTAINS) {
                    if (negated) description.append("does not contain ");
                    else description.append("contains ");
                } else {
                    if (negated) description.append("does not equal ");
                    else description.append("equals ");
                }
                description.append("'").append(expectedValue).append("'");
                break;
                
            case TITLE:
                description.append("Page title ");
                if (assertionType == AssertionType.CONTAINS) {
                    if (negated) description.append("does not contain ");
                    else description.append("contains ");
                } else {
                    if (negated) description.append("does not equal ");
                    else description.append("equals ");
                }
                description.append("'").append(expectedValue).append("'");
                break;
                
            default:
                description.append("Custom assertion");
                if (customMessage != null && !customMessage.isEmpty()) {
                    description.append(": ").append(customMessage);
                }
                break;
        }
        
        if (status != null && status != AssertionStatus.NOT_EXECUTED) {
            description.append(" (").append(status).append(")");
        }
        
        return description.toString();
    }
    
    /**
     * Get a description of the target element
     *
     * @return A string describing the target element
     */
    private String getElementDescription() {
        if (targetElement == null) {
            return "element";
        }
        
        if (targetElement.getTagName() != null && !targetElement.getTagName().isEmpty()) {
            if (targetElement.getId() != null && !targetElement.getId().isEmpty()) {
                return targetElement.getTagName() + " with ID '" + targetElement.getId() + "'";
            } else if (targetElement.getText() != null && !targetElement.getText().isEmpty()) {
                return targetElement.getTagName() + " with text '" + 
                       (targetElement.getText().length() > 20 ? 
                        targetElement.getText().substring(0, 17) + "..." : targetElement.getText()) + "'";
            } else {
                return targetElement.getTagName() + " element " + targetElement.getBestSelector();
            }
        } else {
            return "element " + targetElement.getBestSelector();
        }
    }

    /**
     * The type of assertion to perform
     */
    public enum AssertionType {
        PRESENT("Present"),
        VISIBLE("Visible"),
        ENABLED("Enabled"),
        SELECTED("Selected"),
        TEXT_EQUALS("Text Equals"),
        TEXT_CONTAINS("Text Contains"),
        ATTRIBUTE_EQUALS("Attribute Equals"),
        ATTRIBUTE_CONTAINS("Attribute Contains"),
        URL("URL"),
        URL_CONTAINS("URL Contains"),
        TITLE("Title"),
        TITLE_CONTAINS("Title Contains"),
        EQUALS("Equals"),
        CONTAINS("Contains"),
        STARTS_WITH("Starts With"),
        ENDS_WITH("Ends With"),
        REGEX_MATCH("Regex Match"),
        GREATER_THAN("Greater Than"),
        LESS_THAN("Less Than"),
        GREATER_THAN_OR_EQUALS("Greater Than Or Equals"),
        LESS_THAN_OR_EQUALS("Less Than Or Equals"),
        COUNT_EQUALS("Count Equals"),
        COUNT_GREATER_THAN("Count Greater Than"),
        COUNT_LESS_THAN("Count Less Than"),
        CUSTOM_JAVASCRIPT("Custom JavaScript");

        private final String displayName;

        AssertionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * The status of the assertion
     */
    public enum AssertionStatus {
        NOT_EXECUTED,
        PASSED,
        FAILED,
        ERROR
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssertionEvent that = (AssertionEvent) o;
        return negated == that.negated && 
               isSoft == that.isSoft && 
               isRegex == that.isRegex && 
               isCaseSensitive == that.isCaseSensitive && 
               Objects.equals(targetElement, that.targetElement) && 
               assertionType == that.assertionType && 
               Objects.equals(expectedValue, that.expectedValue) && 
               Objects.equals(customMessage, that.customMessage) && 
               Objects.equals(tolerance, that.tolerance) && 
               status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetElement, assertionType, expectedValue, negated, 
                            isSoft, customMessage, isRegex, isCaseSensitive, 
                            tolerance, status);
    }
} 