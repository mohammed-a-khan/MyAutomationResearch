package com.cstestforge.recorder.model.config;

import com.cstestforge.recorder.model.RecordedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Configuration for conditional logic in recorded events.
 * Represents an if-else structure with condition, then branch, and optional else branch.
 */
public class ConditionConfig {
    
    private String id;
    private ConditionOperator operator;
    private OperandConfig leftOperand;
    private OperandConfig rightOperand;
    private List<RecordedEvent> thenSteps;
    private List<RecordedEvent> elseSteps;
    private boolean isNegated;
    
    /**
     * Default constructor
     */
    public ConditionConfig() {
        this.id = UUID.randomUUID().toString();
        this.thenSteps = new ArrayList<>();
        this.elseSteps = new ArrayList<>();
    }
    
    /**
     * Constructor with operators
     *
     * @param operator The condition operator
     * @param leftOperand The left operand
     * @param rightOperand The right operand
     */
    public ConditionConfig(ConditionOperator operator, OperandConfig leftOperand, OperandConfig rightOperand) {
        this();
        this.operator = operator;
        this.leftOperand = leftOperand;
        this.rightOperand = rightOperand;
    }
    
    /**
     * Get the condition ID
     *
     * @return The condition ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the condition ID
     *
     * @param id The condition ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the condition operator
     *
     * @return The condition operator
     */
    public ConditionOperator getOperator() {
        return operator;
    }
    
    /**
     * Set the condition operator
     *
     * @param operator The condition operator
     */
    public void setOperator(ConditionOperator operator) {
        this.operator = operator;
    }
    
    /**
     * Get the left operand
     *
     * @return The left operand
     */
    public OperandConfig getLeftOperand() {
        return leftOperand;
    }
    
    /**
     * Set the left operand
     *
     * @param leftOperand The left operand
     */
    public void setLeftOperand(OperandConfig leftOperand) {
        this.leftOperand = leftOperand;
    }
    
    /**
     * Get the right operand
     *
     * @return The right operand
     */
    public OperandConfig getRightOperand() {
        return rightOperand;
    }
    
    /**
     * Set the right operand
     *
     * @param rightOperand The right operand
     */
    public void setRightOperand(OperandConfig rightOperand) {
        this.rightOperand = rightOperand;
    }
    
    /**
     * Get the steps to execute if the condition is true
     *
     * @return List of steps to execute if true
     */
    public List<RecordedEvent> getThenSteps() {
        return thenSteps;
    }
    
    /**
     * Set the steps to execute if the condition is true
     *
     * @param thenSteps List of steps to execute if true
     */
    public void setThenSteps(List<RecordedEvent> thenSteps) {
        this.thenSteps = thenSteps != null ? thenSteps : new ArrayList<>();
    }
    
    /**
     * Get the steps to execute if the condition is false
     *
     * @return List of steps to execute if false
     */
    public List<RecordedEvent> getElseSteps() {
        return elseSteps;
    }
    
    /**
     * Set the steps to execute if the condition is false
     *
     * @param elseSteps List of steps to execute if false
     */
    public void setElseSteps(List<RecordedEvent> elseSteps) {
        this.elseSteps = elseSteps != null ? elseSteps : new ArrayList<>();
    }
    
    /**
     * Check if the condition is negated
     *
     * @return True if the condition is negated
     */
    public boolean isNegated() {
        return isNegated;
    }
    
    /**
     * Set whether the condition is negated
     *
     * @param negated True if the condition is negated
     */
    public void setNegated(boolean negated) {
        isNegated = negated;
    }
    
    /**
     * Add a step to the then branch
     *
     * @param event The event to add
     * @return This condition config for method chaining
     */
    public ConditionConfig addThenStep(RecordedEvent event) {
        if (event != null) {
            if (this.thenSteps == null) {
                this.thenSteps = new ArrayList<>();
            }
            this.thenSteps.add(event);
        }
        return this;
    }
    
    /**
     * Add a step to the else branch
     *
     * @param event The event to add
     * @return This condition config for method chaining
     */
    public ConditionConfig addElseStep(RecordedEvent event) {
        if (event != null) {
            if (this.elseSteps == null) {
                this.elseSteps = new ArrayList<>();
            }
            this.elseSteps.add(event);
        }
        return this;
    }
    
    /**
     * Check if the condition has an else branch
     *
     * @return True if there are steps in the else branch
     */
    public boolean hasElseBranch() {
        return elseSteps != null && !elseSteps.isEmpty();
    }
    
    /**
     * Get the condition description
     *
     * @return Human-readable description of the condition
     */
    public String getDescription() {
        if (leftOperand == null || operator == null) {
            return "Invalid condition";
        }
        
        StringBuilder description = new StringBuilder();
        
        if (isNegated) {
            description.append("NOT (");
        }
        
        description.append(leftOperand.getDescription());
        description.append(" ").append(operator.getSymbol()).append(" ");
        
        if (rightOperand != null) {
            description.append(rightOperand.getDescription());
        } else if (operator != ConditionOperator.IS_TRUE && operator != ConditionOperator.IS_FALSE) {
            description.append("NULL");
        }
        
        if (isNegated) {
            description.append(")");
        }
        
        return description.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConditionConfig that = (ConditionConfig) o;
        return isNegated == that.isNegated &&
                Objects.equals(id, that.id) &&
                operator == that.operator &&
                Objects.equals(leftOperand, that.leftOperand) &&
                Objects.equals(rightOperand, that.rightOperand);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, operator, leftOperand, rightOperand, isNegated);
    }
    
    /**
     * Operators for conditions
     */
    public enum ConditionOperator {
        EQUALS("=="),
        NOT_EQUALS("!="),
        GREATER_THAN(">"),
        LESS_THAN("<"),
        GREATER_THAN_OR_EQUALS(">="),
        LESS_THAN_OR_EQUALS("<="),
        CONTAINS("contains"),
        NOT_CONTAINS("does not contain"),
        STARTS_WITH("starts with"),
        ENDS_WITH("ends with"),
        MATCHES("matches pattern"),
        IS_TRUE("is true"),
        IS_FALSE("is false");
        
        private final String symbol;
        
        ConditionOperator(String symbol) {
            this.symbol = symbol;
        }
        
        public String getSymbol() {
            return symbol;
        }
    }
}