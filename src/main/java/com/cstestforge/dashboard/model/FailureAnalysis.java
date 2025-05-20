package com.cstestforge.dashboard.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Model class for test failure analysis
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FailureAnalysis {
    
    private List<CommonFailure> mostCommonFailures;
    private List<FailureByType> failuresByType;
    private List<FailureByBrowser> failuresByBrowser;
    private List<UnstableTest> unstableTests;
    
    /**
     * Default constructor
     */
    public FailureAnalysis() {
    }
    
    /**
     * Get the most common failures
     * @return List of most common failures
     */
    public List<CommonFailure> getMostCommonFailures() {
        return mostCommonFailures;
    }
    
    /**
     * Set the most common failures
     * @param mostCommonFailures List of most common failures
     */
    public void setMostCommonFailures(List<CommonFailure> mostCommonFailures) {
        this.mostCommonFailures = mostCommonFailures;
    }
    
    /**
     * Get failures by type
     * @return List of failures by type
     */
    public List<FailureByType> getFailuresByType() {
        return failuresByType;
    }
    
    /**
     * Set failures by type
     * @param failuresByType List of failures by type
     */
    public void setFailuresByType(List<FailureByType> failuresByType) {
        this.failuresByType = failuresByType;
    }
    
    /**
     * Get failures by browser
     * @return List of failures by browser
     */
    public List<FailureByBrowser> getFailuresByBrowser() {
        return failuresByBrowser;
    }
    
    /**
     * Set failures by browser
     * @param failuresByBrowser List of failures by browser
     */
    public void setFailuresByBrowser(List<FailureByBrowser> failuresByBrowser) {
        this.failuresByBrowser = failuresByBrowser;
    }
    
    /**
     * Get unstable tests
     * @return List of unstable tests
     */
    public List<UnstableTest> getUnstableTests() {
        return unstableTests;
    }
    
    /**
     * Set unstable tests
     * @param unstableTests List of unstable tests
     */
    public void setUnstableTests(List<UnstableTest> unstableTests) {
        this.unstableTests = unstableTests;
    }
    
    /**
     * Inner class representing a common failure
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CommonFailure {
        private String message;
        private int count;
        private List<String> testIds;
        
        /**
         * Default constructor
         */
        public CommonFailure() {
        }
        
        /**
         * Constructor with required parameters
         * 
         * @param message Failure message
         * @param count Occurrence count
         * @param testIds Affected test IDs
         */
        public CommonFailure(String message, int count, List<String> testIds) {
            this.message = message;
            this.count = count;
            this.testIds = testIds;
        }
        
        /**
         * Get the failure message
         * @return Failure message
         */
        public String getMessage() {
            return message;
        }
        
        /**
         * Set the failure message
         * @param message Failure message
         */
        public void setMessage(String message) {
            this.message = message;
        }
        
        /**
         * Get the occurrence count
         * @return Occurrence count
         */
        public int getCount() {
            return count;
        }
        
        /**
         * Set the occurrence count
         * @param count Occurrence count
         */
        public void setCount(int count) {
            this.count = count;
        }
        
        /**
         * Get the affected test IDs
         * @return List of affected test IDs
         */
        public List<String> getTestIds() {
            return testIds;
        }
        
        /**
         * Set the affected test IDs
         * @param testIds List of affected test IDs
         */
        public void setTestIds(List<String> testIds) {
            this.testIds = testIds;
        }
    }
    
    /**
     * Inner class representing failures by type
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FailureByType {
        private String type;
        private int count;
        private double percentage;
        
        /**
         * Default constructor
         */
        public FailureByType() {
        }
        
        /**
         * Constructor with required parameters
         * 
         * @param type Failure type
         * @param count Failure count
         * @param percentage Percentage of total failures
         */
        public FailureByType(String type, int count, double percentage) {
            this.type = type;
            this.count = count;
            this.percentage = percentage;
        }
        
        /**
         * Get the failure type
         * @return Failure type
         */
        public String getType() {
            return type;
        }
        
        /**
         * Set the failure type
         * @param type Failure type
         */
        public void setType(String type) {
            this.type = type;
        }
        
        /**
         * Get the failure count
         * @return Failure count
         */
        public int getCount() {
            return count;
        }
        
        /**
         * Set the failure count
         * @param count Failure count
         */
        public void setCount(int count) {
            this.count = count;
        }
        
        /**
         * Get the percentage of total failures
         * @return Percentage of total failures
         */
        public double getPercentage() {
            return percentage;
        }
        
        /**
         * Set the percentage of total failures
         * @param percentage Percentage of total failures
         */
        public void setPercentage(double percentage) {
            this.percentage = percentage;
        }
    }
    
    /**
     * Inner class representing failures by browser
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FailureByBrowser {
        private String browser;
        private int count;
        private double percentage;
        
        /**
         * Default constructor
         */
        public FailureByBrowser() {
        }
        
        /**
         * Constructor with required parameters
         * 
         * @param browser Browser name
         * @param count Failure count
         * @param percentage Percentage of total failures
         */
        public FailureByBrowser(String browser, int count, double percentage) {
            this.browser = browser;
            this.count = count;
            this.percentage = percentage;
        }
        
        /**
         * Get the browser name
         * @return Browser name
         */
        public String getBrowser() {
            return browser;
        }
        
        /**
         * Set the browser name
         * @param browser Browser name
         */
        public void setBrowser(String browser) {
            this.browser = browser;
        }
        
        /**
         * Get the failure count
         * @return Failure count
         */
        public int getCount() {
            return count;
        }
        
        /**
         * Set the failure count
         * @param count Failure count
         */
        public void setCount(int count) {
            this.count = count;
        }
        
        /**
         * Get the percentage of total failures
         * @return Percentage of total failures
         */
        public double getPercentage() {
            return percentage;
        }
        
        /**
         * Set the percentage of total failures
         * @param percentage Percentage of total failures
         */
        public void setPercentage(double percentage) {
            this.percentage = percentage;
        }
    }
    
    /**
     * Inner class representing an unstable test
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UnstableTest {
        private String testId;
        private String testName;
        private double failureRate;
        private long lastExecuted;
        
        /**
         * Default constructor
         */
        public UnstableTest() {
        }
        
        /**
         * Constructor with required parameters
         * 
         * @param testId Test ID
         * @param testName Test name
         * @param failureRate Failure rate (0.0-1.0)
         * @param lastExecuted Last execution timestamp (epoch milliseconds)
         */
        public UnstableTest(String testId, String testName, double failureRate, long lastExecuted) {
            this.testId = testId;
            this.testName = testName;
            this.failureRate = failureRate;
            this.lastExecuted = lastExecuted;
        }
        
        /**
         * Get the test ID
         * @return Test ID
         */
        public String getTestId() {
            return testId;
        }
        
        /**
         * Set the test ID
         * @param testId Test ID
         */
        public void setTestId(String testId) {
            this.testId = testId;
        }
        
        /**
         * Get the test name
         * @return Test name
         */
        public String getTestName() {
            return testName;
        }
        
        /**
         * Set the test name
         * @param testName Test name
         */
        public void setTestName(String testName) {
            this.testName = testName;
        }
        
        /**
         * Get the failure rate
         * @return Failure rate (0.0-1.0)
         */
        public double getFailureRate() {
            return failureRate;
        }
        
        /**
         * Set the failure rate
         * @param failureRate Failure rate (0.0-1.0)
         */
        public void setFailureRate(double failureRate) {
            this.failureRate = failureRate;
        }
        
        /**
         * Get the last executed timestamp
         * @return Last executed timestamp (epoch milliseconds)
         */
        public long getLastExecuted() {
            return lastExecuted;
        }
        
        /**
         * Set the last executed timestamp
         * @param lastExecuted Last executed timestamp (epoch milliseconds)
         */
        public void setLastExecuted(long lastExecuted) {
            this.lastExecuted = lastExecuted;
        }
    }
} 