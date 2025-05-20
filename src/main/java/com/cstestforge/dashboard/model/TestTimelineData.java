package com.cstestforge.dashboard.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Model class for test execution timeline data
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestTimelineData {
    
    private String startDate;
    private String endDate;
    private List<ExecutionByDay> executionsByDay;
    
    /**
     * Default constructor
     */
    public TestTimelineData() {
    }
    
    /**
     * Constructor with required parameters
     * 
     * @param startDate Start date of the timeline (YYYY-MM-DD)
     * @param endDate End date of the timeline (YYYY-MM-DD)
     * @param executionsByDay Daily execution counts
     */
    public TestTimelineData(String startDate, String endDate, List<ExecutionByDay> executionsByDay) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.executionsByDay = executionsByDay;
    }
    
    /**
     * Get the start date
     * @return Start date (YYYY-MM-DD)
     */
    public String getStartDate() {
        return startDate;
    }
    
    /**
     * Set the start date
     * @param startDate Start date (YYYY-MM-DD)
     */
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }
    
    /**
     * Get the end date
     * @return End date (YYYY-MM-DD)
     */
    public String getEndDate() {
        return endDate;
    }
    
    /**
     * Set the end date
     * @param endDate End date (YYYY-MM-DD)
     */
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
    
    /**
     * Get daily execution counts
     * @return List of daily execution counts
     */
    public List<ExecutionByDay> getExecutionsByDay() {
        return executionsByDay;
    }
    
    /**
     * Set daily execution counts
     * @param executionsByDay List of daily execution counts
     */
    public void setExecutionsByDay(List<ExecutionByDay> executionsByDay) {
        this.executionsByDay = executionsByDay;
    }
    
    /**
     * Inner class representing execution data for a single day
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExecutionByDay {
        private String date;
        private int count;
        private double passRate;
        
        /**
         * Default constructor
         */
        public ExecutionByDay() {
        }
        
        /**
         * Constructor with required parameters
         * 
         * @param date Date (YYYY-MM-DD)
         * @param count Total execution count
         * @param passRate Pass rate (0-100)
         */
        public ExecutionByDay(String date, int count, double passRate) {
            this.date = date;
            this.count = count;
            this.passRate = passRate;
        }
        
        /**
         * Get the date
         * @return Date (YYYY-MM-DD)
         */
        public String getDate() {
            return date;
        }
        
        /**
         * Set the date
         * @param date Date (YYYY-MM-DD)
         */
        public void setDate(String date) {
            this.date = date;
        }
        
        /**
         * Get the execution count
         * @return Execution count
         */
        public int getCount() {
            return count;
        }
        
        /**
         * Set the execution count
         * @param count Execution count
         */
        public void setCount(int count) {
            this.count = count;
        }
        
        /**
         * Get the pass rate
         * @return Pass rate (0-100)
         */
        public double getPassRate() {
            return passRate;
        }
        
        /**
         * Set the pass rate
         * @param passRate Pass rate (0-100)
         */
        public void setPassRate(double passRate) {
            this.passRate = passRate;
        }
    }
} 