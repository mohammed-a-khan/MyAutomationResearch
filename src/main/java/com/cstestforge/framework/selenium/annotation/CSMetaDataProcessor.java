package com.cstestforge.framework.selenium.annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Processor for CSMetaData annotations.
 * Extracts and manages test metadata for reporting and organization.
 */
public class CSMetaDataProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CSMetaDataProcessor.class);
    private static final String METADATA_DIR = "test-output/metadata";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<String, TestMetadata> testMetadata = new ConcurrentHashMap<>();
    
    static {
        try {
            Files.createDirectories(Paths.get(METADATA_DIR));
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        } catch (IOException e) {
            logger.error("Failed to create metadata directory", e);
        }
    }
    
    /**
     * Class to hold test metadata information
     */
    public static class TestMetadata {
        private String testId;
        private String feature;
        private String story;
        private String description;
        private String[] authors;
        private String[] tags;
        private String[] linkedIssues;
        private String requirement;
        private String severity;
        private Map<String, String> customProperties;
        private String className;
        private String methodName;
        
        public TestMetadata() {
            this.customProperties = new HashMap<>();
        }
        
        public String getTestId() {
            return testId;
        }
        
        public void setTestId(String testId) {
            this.testId = testId;
        }
        
        public String getFeature() {
            return feature;
        }
        
        public void setFeature(String feature) {
            this.feature = feature;
        }
        
        public String getStory() {
            return story;
        }
        
        public void setStory(String story) {
            this.story = story;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String[] getAuthors() {
            return authors;
        }
        
        public void setAuthors(String[] authors) {
            this.authors = authors;
        }
        
        public String[] getTags() {
            return tags;
        }
        
        public void setTags(String[] tags) {
            this.tags = tags;
        }
        
        public String[] getLinkedIssues() {
            return linkedIssues;
        }
        
        public void setLinkedIssues(String[] linkedIssues) {
            this.linkedIssues = linkedIssues;
        }
        
        public String getRequirement() {
            return requirement;
        }
        
        public void setRequirement(String requirement) {
            this.requirement = requirement;
        }
        
        public String getSeverity() {
            return severity;
        }
        
        public void setSeverity(String severity) {
            this.severity = severity;
        }
        
        public Map<String, String> getCustomProperties() {
            return customProperties;
        }
        
        public void setCustomProperties(Map<String, String> customProperties) {
            this.customProperties = customProperties;
        }
        
        public void addCustomProperty(String key, String value) {
            this.customProperties.put(key, value);
        }
        
        public String getClassName() {
            return className;
        }
        
        public void setClassName(String className) {
            this.className = className;
        }
        
        public String getMethodName() {
            return methodName;
        }
        
        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("TestMetadata [");
            if (testId != null) sb.append("testId=").append(testId).append(", ");
            if (feature != null) sb.append("feature=").append(feature).append(", ");
            if (story != null) sb.append("story=").append(story).append(", ");
            sb.append("className=").append(className).append(", ");
            sb.append("methodName=").append(methodName);
            sb.append("]");
            return sb.toString();
        }
    }
    
    /**
     * Process CSMetaData annotation for a test method.
     * 
     * @param testMethod Test method
     * @return Processed metadata or null if not found
     */
    public static TestMetadata processMetaData(Method testMethod) {
        if (testMethod == null) {
            return null;
        }
        
        String key = getTestKey(testMethod.getDeclaringClass().getName(), testMethod.getName());
        
        // Return cached metadata if available
        if (testMetadata.containsKey(key)) {
            return testMetadata.get(key);
        }
        
        // Check method level annotation
        CSMetaData annotation = testMethod.getAnnotation(CSMetaData.class);
        
        // Check class level annotation if method level not found
        if (annotation == null) {
            annotation = testMethod.getDeclaringClass().getAnnotation(CSMetaData.class);
        }
        
        if (annotation == null) {
            logger.debug("No CSMetaData annotation found for {}", key);
            return null;
        }
        
        TestMetadata metadata = createMetadata(testMethod.getDeclaringClass(), testMethod.getName(), annotation);
        testMetadata.put(key, metadata);
        
        logger.debug("Processed metadata for {}: {}", key, metadata);
        return metadata;
    }
    
    /**
     * Process CSMetaData annotation for a TestNG test result.
     * 
     * @param result TestNG test result
     * @return Processed metadata or null if not found
     */
    public static TestMetadata processMetaData(ITestResult result) {
        if (result == null) {
            return null;
        }
        
        ITestNGMethod testMethod = result.getMethod();
        Method method = testMethod.getConstructorOrMethod().getMethod();
        
        return processMetaData(method);
    }
    
    /**
     * Create metadata from an annotation.
     * 
     * @param testClass Test class
     * @param methodName Method name
     * @param annotation CSMetaData annotation
     * @return Test metadata
     */
    private static TestMetadata createMetadata(Class<?> testClass, String methodName, CSMetaData annotation) {
        TestMetadata metadata = new TestMetadata();
        
        metadata.setTestId(annotation.testId());
        metadata.setFeature(annotation.feature());
        metadata.setStory(annotation.story());
        metadata.setDescription(annotation.description());
        metadata.setAuthors(annotation.authors());
        metadata.setTags(annotation.tags());
        metadata.setLinkedIssues(annotation.linkedIssues());
        metadata.setRequirement(annotation.requirement());
        metadata.setSeverity(annotation.severity().name());
        metadata.setClassName(testClass.getName());
        metadata.setMethodName(methodName);
        
        // Process custom properties
        String[] customProps = annotation.customProperties();
        if (customProps != null && customProps.length > 0) {
            for (String prop : customProps) {
                String[] parts = prop.split("=", 2);
                if (parts.length == 2) {
                    metadata.addCustomProperty(parts[0], parts[1]);
                }
            }
        }
        
        return metadata;
    }
    
    /**
     * Get metadata for a test.
     * 
     * @param className Test class name
     * @param methodName Test method name
     * @return Test metadata or null if not found
     */
    public static TestMetadata getTestMetadata(String className, String methodName) {
        return testMetadata.get(getTestKey(className, methodName));
    }
    
    /**
     * Get key for test metadata lookup.
     * 
     * @param className Test class name
     * @param methodName Test method name
     * @return Lookup key
     */
    private static String getTestKey(String className, String methodName) {
        return className + "." + methodName;
    }
    
    /**
     * Export all collected metadata to JSON files.
     */
    public static void exportAllMetadata() {
        logger.info("Exporting {} metadata entries to JSON files", testMetadata.size());
        
        for (Map.Entry<String, TestMetadata> entry : testMetadata.entrySet()) {
            try {
                String fileName = entry.getKey().replace('.', '_') + ".json";
                String filePath = METADATA_DIR + File.separator + fileName;
                objectMapper.writeValue(new File(filePath), entry.getValue());
                logger.debug("Exported metadata for {} to {}", entry.getKey(), filePath);
            } catch (IOException e) {
                logger.error("Failed to export metadata for {}", entry.getKey(), e);
            }
        }
    }
    
    /**
     * Get all tests with specific tags.
     * 
     * @param tags Array of tags to match
     * @return List of matching test metadata
     */
    public static List<TestMetadata> getTestsByTags(String[] tags) {
        List<TestMetadata> matchingTests = new ArrayList<>();
        
        for (TestMetadata meta : testMetadata.values()) {
            if (hasAnyTag(meta, tags)) {
                matchingTests.add(meta);
            }
        }
        
        return matchingTests;
    }
    
    /**
     * Check if metadata has any of the specified tags.
     * 
     * @param metadata Test metadata
     * @param tags Array of tags to match
     * @return True if metadata has any matching tag
     */
    private static boolean hasAnyTag(TestMetadata metadata, String[] tags) {
        if (metadata.getTags() == null || tags == null) {
            return false;
        }
        
        for (String metaTag : metadata.getTags()) {
            for (String searchTag : tags) {
                if (metaTag.equalsIgnoreCase(searchTag)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Add execution result to test metadata and export it.
     * 
     * @param result TestNG test result
     */
    public static void recordTestResult(ITestResult result) {
        if (result == null) {
            return;
        }
        
        String className = result.getTestClass().getName();
        String methodName = result.getMethod().getMethodName();
        String key = getTestKey(className, methodName);
        
        TestMetadata metadata = testMetadata.get(key);
        if (metadata == null) {
            metadata = processMetaData(result);
            if (metadata == null) {
                return;
            }
        }
        
        // Add execution details
        metadata.addCustomProperty("executionResult", getResultName(result.getStatus()));
        metadata.addCustomProperty("executionTime", String.valueOf(result.getEndMillis() - result.getStartMillis()));
        metadata.addCustomProperty("executionDate", 
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        if (result.getThrowable() != null) {
            metadata.addCustomProperty("failureReason", result.getThrowable().getMessage());
        }
        
        // Export single test metadata
        try {
            String fileName = key.replace('.', '_') + ".json";
            String filePath = METADATA_DIR + File.separator + fileName;
            objectMapper.writeValue(new File(filePath), metadata);
            logger.debug("Exported test result metadata for {} to {}", key, filePath);
        } catch (IOException e) {
            logger.error("Failed to export test result metadata for {}", key, e);
        }
    }
    
    /**
     * Convert TestNG result status to human-readable string.
     * 
     * @param status TestNG status code
     * @return Human-readable status
     */
    private static String getResultName(int status) {
        switch (status) {
            case ITestResult.SUCCESS:
                return "PASSED";
            case ITestResult.FAILURE:
                return "FAILED";
            case ITestResult.SKIP:
                return "SKIPPED";
            case ITestResult.SUCCESS_PERCENTAGE_FAILURE:
                return "PERCENTAGE_FAILURE";
            case ITestResult.STARTED:
                return "STARTED";
            default:
                return "UNKNOWN";
        }
    }
    
    /**
     * Generate a test execution report in HTML format.
     * 
     * @param outputPath Output file path
     */
    public static void generateHtmlReport(String outputPath) {
        if (testMetadata.isEmpty()) {
            logger.warn("No test metadata available for report generation");
            return;
        }
        
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write("<!DOCTYPE html>\n");
            writer.write("<html>\n<head>\n");
            writer.write("<title>CSTestForge Test Execution Report</title>\n");
            writer.write("<style>\n");
            writer.write("body { font-family: Arial, sans-serif; margin: 20px; }\n");
            writer.write("table { border-collapse: collapse; width: 100%; }\n");
            writer.write("th, td { border: 1px solid #ddd; padding: 8px; }\n");
            writer.write("th { background-color: #f2f2f2; }\n");
            writer.write("tr:hover { background-color: #f5f5f5; }\n");
            writer.write(".PASSED { background-color: #dff0d8; }\n");
            writer.write(".FAILED { background-color: #f2dede; }\n");
            writer.write(".SKIPPED { background-color: #fcf8e3; }\n");
            writer.write("</style>\n");
            writer.write("</head>\n<body>\n");
            
            writer.write("<h1>CSTestForge Test Execution Report</h1>\n");
            writer.write("<p>Generated on: " + 
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "</p>\n");
            
            writer.write("<table>\n");
            writer.write("<tr><th>Test ID</th><th>Feature</th><th>Class</th><th>Method</th>");
            writer.write("<th>Description</th><th>Status</th><th>Execution Time</th></tr>\n");
            
            for (TestMetadata meta : testMetadata.values()) {
                String status = meta.getCustomProperties().getOrDefault("executionResult", "");
                String rowClass = !status.isEmpty() ? " class=\"" + status + "\"" : "";
                
                writer.write("<tr" + rowClass + ">");
                writer.write("<td>" + nullSafe(meta.getTestId()) + "</td>");
                writer.write("<td>" + nullSafe(meta.getFeature()) + "</td>");
                writer.write("<td>" + getSimpleClassName(meta.getClassName()) + "</td>");
                writer.write("<td>" + nullSafe(meta.getMethodName()) + "</td>");
                writer.write("<td>" + nullSafe(meta.getDescription()) + "</td>");
                writer.write("<td>" + status + "</td>");
                
                String execTime = meta.getCustomProperties().getOrDefault("executionTime", "");
                if (!execTime.isEmpty()) {
                    try {
                        long timeMs = Long.parseLong(execTime);
                        execTime = String.format("%.2f sec", timeMs / 1000.0);
                    } catch (NumberFormatException e) {
                        // Use as is
                    }
                }
                writer.write("<td>" + execTime + "</td>");
                writer.write("</tr>\n");
            }
            
            writer.write("</table>\n");
            writer.write("</body>\n</html>");
            
            logger.info("HTML report generated at {}", outputPath);
        } catch (IOException e) {
            logger.error("Failed to generate HTML report", e);
        }
    }
    
    /**
     * Get simple class name from fully qualified name.
     * 
     * @param className Fully qualified class name
     * @return Simple class name
     */
    private static String getSimpleClassName(String className) {
        if (className == null) {
            return "";
        }
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(lastDot + 1) : className;
    }
    
    /**
     * Convert null to empty string.
     * 
     * @param value String value
     * @return Non-null string
     */
    private static String nullSafe(String value) {
        return value != null ? value : "";
    }
} 