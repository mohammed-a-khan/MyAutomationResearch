package com.cstestforge.framework.selenium.reporting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cstestforge.framework.selenium.core.CSDriverManager;

/**
 * Custom reporting class for the CSTestForge framework.
 * Provides HTML reporting with screenshots, dashboards and logs without external dependencies.
 */
public class CSReporting {
    private static final Logger logger = LoggerFactory.getLogger(CSReporting.class);
    
    // Status enum for tests and steps
    public enum Status {
        PASS, FAIL, SKIP, INFO, WARNING, ERROR
    }
    
    // Thread-safe maps for storing test data
    private static final Map<Long, TestNode> testMap = new ConcurrentHashMap<>();
    private static final Map<Long, TestNode> stepMap = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> screenshotMap = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, String>> testMetadataMap = new ConcurrentHashMap<>();
    private static final List<TestNode> allTests = new ArrayList<>();
    
    // Counters for test statistics
    private static final AtomicInteger totalTests = new AtomicInteger(0);
    private static final AtomicInteger passedTests = new AtomicInteger(0);
    private static final AtomicInteger failedTests = new AtomicInteger(0);
    private static final AtomicInteger skippedTests = new AtomicInteger(0);
    
    // Report output directory
    private static final String REPORT_DIR = "test-output/cstestforge-reports";
    private static final String SCREENSHOT_DIR = REPORT_DIR + "/screenshots";
    private static final String ASSETS_DIR = REPORT_DIR + "/assets";
    
    // Timestamp format for uniquely naming files
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private static final String REPORT_TIMESTAMP = DATE_FORMAT.format(new Date());
    
    // Report generation resources
    private static final String REPORT_TITLE = "CSTestForge Test Report";
    
    // Static initialization
    static {
        initializeReporting();
    }
    
    /**
     * Default constructor
     */
    public CSReporting() {
        // Create required directories
        createDirectories();
        createReportAssets();
    }
    
    /**
     * Get the WebDriver instance from CSDriverManager
     * 
     * @return WebDriver instance
     */
    private WebDriver getDriver() {
        return CSDriverManager.getDriver();
    }
    
    /**
     * Initialize reporting system
     */
    private static synchronized void initializeReporting() {
        // Add shutdown hook to generate report on JVM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Generating test report");
            generateReport();
        }));
        
        logger.info("CSReporting initialized");
    }
    
    /**
     * Create required directories for reports and screenshots
     */
    private void createDirectories() {
        try {
            Files.createDirectories(Paths.get(REPORT_DIR));
            Files.createDirectories(Paths.get(SCREENSHOT_DIR));
            Files.createDirectories(Paths.get(ASSETS_DIR));
        } catch (IOException e) {
            logger.error("Failed to create report directories", e);
        }
    }
    
    /**
     * Create CSS and JavaScript assets for the report
     */
    private void createReportAssets() {
        try {
            // Create CSS file
            String css = 
                "body { font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; color: #333; }\n" +
                ".container { max-width: 1200px; margin: 0 auto; padding: 20px; }\n" +
                ".header { background: linear-gradient(135deg, #2b63c6 0%, #175aa6 100%); color: white; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }\n" +
                ".header h1 { margin: 0; font-size: 24px; }\n" +
                ".dashboard { display: flex; justify-content: space-between; margin-bottom: 30px; }\n" +
                ".metric { flex: 1; padding: 15px; margin: 0 10px; text-align: center; background: white; border-radius: 5px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }\n" +
                ".metric.total { border-top: 4px solid #175aa6; }\n" +
                ".metric.passed { border-top: 4px solid #4CAF50; }\n" +
                ".metric.failed { border-top: 4px solid #F44336; }\n" +
                ".metric.skipped { border-top: 4px solid #FF9800; }\n" +
                ".metric h2 { margin: 0; font-size: 16px; font-weight: normal; }\n" +
                ".metric .value { font-size: 32px; margin: 10px 0; }\n" +
                ".tests { margin-top: 20px; }\n" +
                ".test { background: white; margin-bottom: 15px; border-radius: 5px; overflow: hidden; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }\n" +
                ".test-header { padding: 15px; cursor: pointer; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #eee; }\n" +
                ".test-header.pass { border-left: 5px solid #4CAF50; }\n" +
                ".test-header.fail { border-left: 5px solid #F44336; }\n" +
                ".test-header.skip { border-left: 5px solid #FF9800; }\n" +
                ".test-title { font-weight: bold; flex: 1; }\n" +
                ".test-time { color: #666; font-size: 0.9em; margin-right: 15px; }\n" +
                ".test-status { font-weight: bold; }\n" +
                ".test-status.pass { color: #4CAF50; }\n" +
                ".test-status.fail { color: #F44336; }\n" +
                ".test-status.skip { color: #FF9800; }\n" +
                ".test-content { display: none; padding: 15px; background: #fafafa; }\n" +
                ".test-content.show { display: block; }\n" +
                ".step { margin: 10px 0; padding: 10px; background: white; border-radius: 3px; border-left: 3px solid #ccc; }\n" +
                ".step.pass { border-left-color: #4CAF50; }\n" +
                ".step.fail { border-left-color: #F44336; }\n" +
                ".step.skip { border-left-color: #FF9800; }\n" +
                ".step-header { display: flex; justify-content: space-between; margin-bottom: 5px; }\n" +
                ".step-title { font-weight: bold; }\n" +
                ".step-time { color: #666; font-size: 0.9em; }\n" +
                ".step-status { font-weight: bold; }\n" +
                ".step-status.pass { color: #4CAF50; }\n" +
                ".step-status.fail { color: #F44336; }\n" +
                ".step-status.skip { color: #FF9800; }\n" +
                ".logs { margin-top: 10px; font-family: monospace; font-size: 0.9em; }\n" +
                ".log { margin: 2px 0; padding: 2px 5px; border-radius: 2px; }\n" +
                ".log.info { background: #E3F2FD; }\n" +
                ".log.warn { background: #FFF8E1; color: #F57F17; }\n" +
                ".log.error { background: #FFEBEE; color: #B71C1C; }\n" +
                ".screenshot { margin: 10px 0; }\n" +
                ".screenshot img { max-width: 100%; border: 1px solid #ddd; }\n" +
                ".toggle-btn { background: none; border: none; cursor: pointer; font-size: 16px; color: #175aa6; }\n";
                
            String javascript = 
                "document.addEventListener('DOMContentLoaded', function() {\n" +
                "  // Toggle test details\n" +
                "  document.querySelectorAll('.test-header').forEach(function(header) {\n" +
                "    header.addEventListener('click', function() {\n" +
                "      var content = this.nextElementSibling;\n" +
                "      content.classList.toggle('show');\n" +
                "    });\n" +
                "  });\n" +
                "  \n" +
                "  // Toggle all button\n" +
                "  document.getElementById('toggle-all-btn').addEventListener('click', function() {\n" +
                "    var expanded = this.getAttribute('data-expanded') === 'true';\n" +
                "    document.querySelectorAll('.test-content').forEach(function(content) {\n" +
                "      if (expanded) {\n" +
                "        content.classList.remove('show');\n" +
                "      } else {\n" +
                "        content.classList.add('show');\n" +
                "      }\n" +
                "    });\n" +
                "    this.setAttribute('data-expanded', !expanded);\n" +
                "    this.textContent = expanded ? 'Expand All' : 'Collapse All';\n" +
                "  });\n" +
                "});\n";
                
            // Write CSS file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(ASSETS_DIR + "/style.css"))) {
                writer.write(css);
            }
            
            // Write JavaScript file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(ASSETS_DIR + "/script.js"))) {
                writer.write(javascript);
            }
            
        } catch (IOException e) {
            logger.error("Failed to create report assets", e);
        }
    }
    
    /**
     * Start a new test
     * 
     * @param testName Test name
     * @param description Test description
     * @return TestNode instance
     */
    public TestNode startTest(String testName, String description) {
        logger.info("Starting test: {}", testName);
        
        TestNode test = new TestNode(testName, description, null);
        test.startTime = System.currentTimeMillis();
        
        // Store test in thread map and global list
        long threadId = Thread.currentThread().getId();
        testMap.put(threadId, test);
        allTests.add(test);
        
        // Initialize screenshot list for this test
        screenshotMap.put(testName, new ArrayList<>());
        
        // Initialize metadata for this test
        testMetadataMap.put(testName, new HashMap<>());
        
        // Increment total tests counter
        totalTests.incrementAndGet();
        
        return test;
    }
    
    /**
     * Get the current test
     * 
     * @return TestNode instance or null if not found
     */
    public TestNode getTest() {
        long threadId = Thread.currentThread().getId();
        return testMap.get(threadId);
    }
    
    /**
     * Start a new step in the current test
     * 
     * @param stepName Step name
     * @param description Step description
     * @return TestNode instance for the step
     */
    public TestNode startStep(String stepName, String description) {
        TestNode test = getTest();
        if (test == null) {
            test = startTest("Test_" + Thread.currentThread().getId(), "Auto-created test");
        }
        
        logger.debug("Starting step: {}", stepName);
        TestNode step = new TestNode(stepName, description, test);
        step.startTime = System.currentTimeMillis();
        
        // Add step to parent test
        test.steps.add(step);
        
        // Store step in thread map
        long threadId = Thread.currentThread().getId();
        stepMap.put(threadId, step);
        
        return step;
    }
    
    /**
     * Get the current step
     * 
     * @return TestNode instance for step or null if not found
     */
    public TestNode getStep() {
        long threadId = Thread.currentThread().getId();
        return stepMap.get(threadId);
    }
    
    /**
     * End the current step with result
     * 
     * @param stepName Step name
     * @param passed Whether the step passed
     * @param executionTime Execution time in milliseconds
     */
    public void endStep(String stepName, boolean passed, long executionTime) {
        endStep(stepName, passed, executionTime, null);
    }
    
    /**
     * End the current step with result and exception
     * 
     * @param stepName Step name
     * @param passed Whether the step passed
     * @param executionTime Execution time in milliseconds
     * @param exception Exception if step failed
     */
    public void endStep(String stepName, boolean passed, long executionTime, Throwable exception) {
        TestNode step = getStep();
        if (step == null) {
            logger.warn("No step found to end: {}", stepName);
            return;
        }
        
        // Add execution time if available
        if (executionTime > 0) {
            step.executionTime = executionTime;
        } else {
            step.executionTime = System.currentTimeMillis() - step.startTime;
        }
        
        // Add result
        if (passed) {
            step.status = Status.PASS;
        } else {
            step.status = Status.FAIL;
            if (exception != null) {
                step.logs.add(new LogEntry(Status.ERROR, exception.toString()));
                for (StackTraceElement element : exception.getStackTrace()) {
                    step.logs.add(new LogEntry(Status.ERROR, "    at " + element.toString()));
                }
            }
            
            // Take screenshot on failure
            String screenshotPath = takeScreenshot(stepName + "_Failure");
            addScreenshotToStep(step, stepName + " Failed", screenshotPath);
        }
        
        // Remove step from map
        long threadId = Thread.currentThread().getId();
        stepMap.remove(threadId);
        
        logger.debug("Ended step: {} with status: {}", stepName, passed ? "PASS" : "FAIL");
    }
    
    /**
     * End the current test with result
     * 
     * @param testName Test name
     * @param passed Whether the test passed
     */
    public void endTest(String testName, boolean passed) {
        TestNode test = getTest();
        if (test == null) {
            logger.warn("No test found to end: {}", testName);
            return;
        }
        
        // Set status and end time
        if (passed) {
            test.status = Status.PASS;
            passedTests.incrementAndGet();
        } else {
            test.status = Status.FAIL;
            failedTests.incrementAndGet();
        }
        
        test.endTime = System.currentTimeMillis();
        test.executionTime = test.endTime - test.startTime;
        
        // Remove test from map
        long threadId = Thread.currentThread().getId();
        testMap.remove(threadId);
        
        logger.info("Ended test: {} with status: {}", testName, passed ? "PASS" : "FAIL");
    }
    
    /**
     * Take a screenshot and save it
     * 
     * @param name Screenshot name
     * @return Path to the saved screenshot
     */
    public String takeScreenshot(String name) {
        WebDriver driver = getDriver();
        if (driver == null || !(driver instanceof TakesScreenshot)) {
            logger.warn("Driver does not support taking screenshots");
            return null;
        }
        
        try {
            // Generate a unique filename with timestamp
            String timestamp = DATE_FORMAT.format(new Date());
            String filename = name.replaceAll("[^a-zA-Z0-9-_]", "_") + "_" + timestamp + ".png";
            String filepath = SCREENSHOT_DIR + "/" + filename;
            
            // Take screenshot
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File destFile = new File(filepath);
            
            // Copy file
            FileUtils.copyFile(srcFile, destFile);
            
            logger.debug("Screenshot saved: {}", filepath);
            return filepath;
        } catch (Exception e) {
            logger.error("Failed to take screenshot", e);
            return null;
        }
    }
    
    /**
     * Add a screenshot to the current step
     * 
     * @param step Step to add screenshot to
     * @param title Screenshot title
     * @param screenshotPath Path to the screenshot file
     */
    private void addScreenshotToStep(TestNode step, String title, String screenshotPath) {
        if (screenshotPath == null) {
            logger.warn("Cannot add null screenshot to step");
            return;
        }
        
        try {
            // Add to step screenshots
            step.screenshots.add(new Screenshot(title, screenshotPath));
            
            // Add to screenshot map for the test
            String testName = step.parent != null ? step.parent.name : step.name;
            List<String> screenshots = screenshotMap.getOrDefault(testName, new ArrayList<>());
            screenshots.add(screenshotPath);
            screenshotMap.put(testName, screenshots);
            
            logger.debug("Added screenshot to step: {}", screenshotPath);
        } catch (Exception e) {
            logger.error("Failed to add screenshot to step", e);
        }
    }
    
    /**
     * Add a screenshot to the current test or step
     * 
     * @param title Screenshot title
     * @param screenshotPath Path to the screenshot
     */
    public void addScreenshot(String title, String screenshotPath) {
        if (screenshotPath == null || !new File(screenshotPath).exists()) {
            logger.warn("Screenshot file does not exist: {}", screenshotPath);
            return;
        }
        
        try {
            // Copy the screenshot to the reports directory
            String timestamp = DATE_FORMAT.format(new Date());
            String filename = title.replaceAll("[^a-zA-Z0-9-_]", "_") + "_" + timestamp + ".png";
            String destPath = SCREENSHOT_DIR + "/" + filename;
            
            Files.copy(Paths.get(screenshotPath), Paths.get(destPath), StandardCopyOption.REPLACE_EXISTING);
            
            // Add to step if available, otherwise to test
            TestNode step = getStep();
            if (step != null) {
                addScreenshotToStep(step, title, destPath);
            } else {
                TestNode test = getTest();
                if (test != null) {
                    addScreenshotToStep(test, title, destPath);
                } else {
                    logger.warn("No test or step found to add screenshot: {}", destPath);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to add screenshot", e);
        }
    }
    
    /**
     * Log info message
     * 
     * @param message Message to log
     */
    public void info(String message) {
        TestNode step = getStep();
        TestNode test = getTest();
        
        if (step != null) {
            step.logs.add(new LogEntry(Status.INFO, message));
        } else if (test != null) {
            test.logs.add(new LogEntry(Status.INFO, message));
        }
        
        logger.info(message);
    }
    
    /**
     * Log debug message
     * 
     * @param message Message to log
     */
    public void debug(String message) {
        TestNode step = getStep();
        TestNode test = getTest();
        
        if (step != null) {
            step.logs.add(new LogEntry(Status.INFO, message));
        } else if (test != null) {
            test.logs.add(new LogEntry(Status.INFO, message));
        }
        
        logger.debug(message);
    }
    
    /**
     * Log warning message
     * 
     * @param message Message to log
     */
    public void warning(String message) {
        TestNode step = getStep();
        TestNode test = getTest();
        
        if (step != null) {
            step.logs.add(new LogEntry(Status.WARNING, message));
        } else if (test != null) {
            test.logs.add(new LogEntry(Status.WARNING, message));
        }
        
        logger.warn(message);
    }
    
    /**
     * Log error message
     * 
     * @param message Message to log
     * @param throwable Exception to log
     */
    public void error(String message, Throwable throwable) {
        TestNode step = getStep();
        TestNode test = getTest();
        
        if (step != null) {
            step.logs.add(new LogEntry(Status.ERROR, message));
            if (throwable != null) {
                step.logs.add(new LogEntry(Status.ERROR, throwable.toString()));
                for (StackTraceElement element : throwable.getStackTrace()) {
                    step.logs.add(new LogEntry(Status.ERROR, "    at " + element.toString()));
                }
            }
        } else if (test != null) {
            test.logs.add(new LogEntry(Status.ERROR, message));
            if (throwable != null) {
                test.logs.add(new LogEntry(Status.ERROR, throwable.toString()));
                for (StackTraceElement element : throwable.getStackTrace()) {
                    test.logs.add(new LogEntry(Status.ERROR, "    at " + element.toString()));
                }
            }
        }
        
        logger.error(message, throwable);
    }
    
    /**
     * Add metadata to the test
     * 
     * @param key Metadata key
     * @param value Metadata value
     */
    public void addMetadata(String key, String value) {
        String testName = getTestName();
        if (testName != null) {
            Map<String, String> metadata = testMetadataMap.getOrDefault(testName, new HashMap<>());
            metadata.put(key, value);
            testMetadataMap.put(testName, metadata);
            
            TestNode test = getTest();
            if (test != null) {
                test.metadata.put(key, value);
            }
        }
    }
    
    /**
     * Get the current test name
     * 
     * @return Test name or null if not found
     */
    private String getTestName() {
        TestNode test = getTest();
        if (test != null) {
            return test.name;
        }
        return null;
    }
    
    /**
     * Generate the HTML report
     */
    public static synchronized void generateReport() {
        try {
            // Create index.html
            String indexPath = REPORT_DIR + "/index.html";
            StringBuilder html = new StringBuilder();
            
            // HTML header
            html.append("<!DOCTYPE html>\n")
                .append("<html>\n<head>\n")
                .append("<meta charset=\"UTF-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                .append("<title>").append(REPORT_TITLE).append("</title>\n")
                .append("<link rel=\"stylesheet\" href=\"assets/style.css\">\n")
                .append("</head>\n<body>\n")
                .append("<div class=\"header\">\n")
                .append("<div class=\"container\">\n")
                .append("<h1>").append(REPORT_TITLE).append(" - ").append(REPORT_TIMESTAMP).append("</h1>\n")
                .append("</div>\n")
                .append("</div>\n")
                .append("<div class=\"container\">\n");
            
            // Dashboard
            html.append("<div class=\"dashboard\">\n")
                .append("<div class=\"metric total\"><h2>Total Tests</h2><div class=\"value\">").append(totalTests.get()).append("</div></div>\n")
                .append("<div class=\"metric passed\"><h2>Passed</h2><div class=\"value\">").append(passedTests.get()).append("</div></div>\n")
                .append("<div class=\"metric failed\"><h2>Failed</h2><div class=\"value\">").append(failedTests.get()).append("</div></div>\n")
                .append("<div class=\"metric skipped\"><h2>Skipped</h2><div class=\"value\">").append(skippedTests.get()).append("</div></div>\n")
                .append("</div>\n");
            
            // Test controls
            html.append("<div style=\"text-align: right; margin-bottom: 10px;\">\n")
                .append("<button id=\"toggle-all-btn\" class=\"toggle-btn\" data-expanded=\"false\">Expand All</button>\n")
                .append("</div>\n");
            
            // Tests section
            html.append("<div class=\"tests\">\n");
            
            // Add each test
            for (TestNode test : allTests) {
                // Test container
                html.append("<div class=\"test\">\n");
                
                // Test header
                String statusClass = test.status == Status.PASS ? "pass" : 
                                    test.status == Status.FAIL ? "fail" : "skip";
                
                html.append("<div class=\"test-header ").append(statusClass).append("\">\n")
                    .append("<div class=\"test-title\">").append(test.name).append("</div>\n")
                    .append("<div class=\"test-time\">").append(formatTime(test.executionTime)).append("</div>\n")
                    .append("<div class=\"test-status ").append(statusClass).append("\">").append(test.status).append("</div>\n")
                    .append("</div>\n");
                
                // Test content
                html.append("<div class=\"test-content\">\n");
                
                // Test description
                if (test.description != null && !test.description.isEmpty()) {
                    html.append("<div>").append(test.description).append("</div>\n");
                }
                
                // Test metadata
                if (!test.metadata.isEmpty()) {
                    html.append("<div style=\"margin: 10px 0;\">\n");
                    for (Map.Entry<String, String> entry : test.metadata.entrySet()) {
                        html.append("<div><strong>").append(entry.getKey()).append(":</strong> ")
                            .append(entry.getValue()).append("</div>\n");
                    }
                    html.append("</div>\n");
                }
                
                // Test logs
                if (!test.logs.isEmpty()) {
                    html.append("<div class=\"logs\">\n");
                    for (LogEntry log : test.logs) {
                        html.append("<div class=\"log ").append(log.status.name().toLowerCase()).append("\">")
                            .append(log.message).append("</div>\n");
                    }
                    html.append("</div>\n");
                }
                
                // Test screenshots
                if (!test.screenshots.isEmpty()) {
                    html.append("<div class=\"screenshots\">\n");
                    for (Screenshot screenshot : test.screenshots) {
                        String relativePath = getRelativePath(screenshot.path, REPORT_DIR);
                        html.append("<div class=\"screenshot\">\n")
                            .append("<div>").append(screenshot.title).append("</div>\n")
                            .append("<a href=\"").append(relativePath).append("\" target=\"_blank\">")
                            .append("<img src=\"").append(relativePath).append("\" alt=\"").append(screenshot.title).append("\">")
                            .append("</a>\n")
                            .append("</div>\n");
                    }
                    html.append("</div>\n");
                }
                
                // Steps
                if (!test.steps.isEmpty()) {
                    html.append("<div style=\"margin-top: 20px;\"><strong>Steps:</strong></div>\n");
                    
                    for (TestNode step : test.steps) {
                        String stepStatusClass = step.status == Status.PASS ? "pass" : 
                                               step.status == Status.FAIL ? "fail" : "skip";
                        
                        html.append("<div class=\"step ").append(stepStatusClass).append("\">\n")
                            .append("<div class=\"step-header\">\n")
                            .append("<div class=\"step-title\">").append(step.name).append("</div>\n")
                            .append("<div class=\"step-time\">").append(formatTime(step.executionTime)).append("</div>\n")
                            .append("<div class=\"step-status ").append(stepStatusClass).append("\">").append(step.status).append("</div>\n")
                            .append("</div>\n");
                            
                        // Step description
                        if (step.description != null && !step.description.isEmpty()) {
                            html.append("<div>").append(step.description).append("</div>\n");
                        }
                        
                        // Step logs
                        if (!step.logs.isEmpty()) {
                            html.append("<div class=\"logs\">\n");
                            for (LogEntry log : step.logs) {
                                html.append("<div class=\"log ").append(log.status.name().toLowerCase()).append("\">")
                                    .append(log.message).append("</div>\n");
                            }
                            html.append("</div>\n");
                        }
                        
                        // Step screenshots
                        if (!step.screenshots.isEmpty()) {
                            html.append("<div class=\"screenshots\">\n");
                            for (Screenshot screenshot : step.screenshots) {
                                String relativePath = getRelativePath(screenshot.path, REPORT_DIR);
                                html.append("<div class=\"screenshot\">\n")
                                    .append("<div>").append(screenshot.title).append("</div>\n")
                                    .append("<a href=\"").append(relativePath).append("\" target=\"_blank\">")
                                    .append("<img src=\"").append(relativePath).append("\" alt=\"").append(screenshot.title).append("\">")
                                    .append("</a>\n")
                                    .append("</div>\n");
                            }
                            html.append("</div>\n");
                        }
                        
                        html.append("</div>\n"); // End step
                    }
                }
                
                html.append("</div>\n"); // End test content
                html.append("</div>\n"); // End test
            }
            
            html.append("</div>\n"); // End tests section
            
            // Footer
            html.append("<div style=\"margin-top: 30px; text-align: center; color: #666; font-size: 0.8em;\">\n")
                .append("Report generated on: ").append(new Date()).append("<br>\n")
                .append("CSTestForge Framework\n")
                .append("</div>\n");
            
            html.append("</div>\n"); // End container
            
            // Add JavaScript
            html.append("<script src=\"assets/script.js\"></script>\n");
            
            html.append("</body>\n</html>");
            
            // Write HTML to file
            File indexFile = new File(indexPath);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile))) {
                writer.write(html.toString());
            }
            
            logger.info("Report generated at: {}", indexPath);
            
        } catch (Exception e) {
            logger.error("Failed to generate report", e);
        }
    }
    
    /**
     * Format time duration in milliseconds
     * 
     * @param millis Time in milliseconds
     * @return Formatted time string
     */
    private static String formatTime(long millis) {
        if (millis < 1000) {
            return millis + " ms";
        } else if (millis < 60000) {
            return String.format("%.2f sec", millis / 1000.0);
        } else {
            long minutes = millis / 60000;
            long seconds = (millis % 60000) / 1000;
            return String.format("%d min %d sec", minutes, seconds);
        }
    }
    
    /**
     * Get relative path from base directory
     * 
     * @param path Absolute path
     * @param baseDir Base directory
     * @return Relative path
     */
    private static String getRelativePath(String path, String baseDir) {
        try {
            return Paths.get(baseDir).relativize(Paths.get(path)).toString();
        } catch (Exception e) {
            return path;
        }
    }
    
    /**
     * TestNode class for representing tests and steps
     */
    public class TestNode {
        String name;
        String description;
        TestNode parent;
        List<TestNode> steps = new ArrayList<>();
        List<LogEntry> logs = new ArrayList<>();
        List<Screenshot> screenshots = new ArrayList<>();
        Map<String, String> metadata = new HashMap<>();
        Status status = Status.INFO;
        long startTime;
        long endTime;
        long executionTime;
        
        public TestNode(String name, String description, TestNode parent) {
            this.name = name;
            this.description = description;
            this.parent = parent;
        }
    }
    
    /**
     * LogEntry class for storing log messages
     */
    public class LogEntry {
        Status status;
        String message;
        long timestamp;
        
        public LogEntry(Status status, String message) {
            this.status = status;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * Screenshot class for storing screenshot information
     */
    public class Screenshot {
        String title;
        String path;
        
        public Screenshot(String title, String path) {
            this.title = title;
            this.path = path;
        }
    }
} 