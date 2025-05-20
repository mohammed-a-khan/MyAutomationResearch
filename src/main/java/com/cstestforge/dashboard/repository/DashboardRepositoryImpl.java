package com.cstestforge.dashboard.repository;

import com.cstestforge.dashboard.model.DashboardStats;
import com.cstestforge.dashboard.model.EnvironmentStatus;
import com.cstestforge.dashboard.model.FailureAnalysis;
import com.cstestforge.project.model.execution.TestExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Implementation of the DashboardRepository interface that uses the file system for storage
 */
@Repository
public class DashboardRepositoryImpl implements DashboardRepository {

    private static final Logger logger = LoggerFactory.getLogger(DashboardRepositoryImpl.class);
    private static final int TIMEOUT_MS = 5000; // 5 seconds timeout for health checks
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final ObjectMapper objectMapper;
    private final String dataDirectoryPath;
    
    public DashboardRepositoryImpl(@Value("${app.data.directory:./data}") String dataDirectoryPath) {
        this.dataDirectoryPath = dataDirectoryPath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // Ensure data directory exists
        try {
            Files.createDirectories(Paths.get(dataDirectoryPath));
            Files.createDirectories(Paths.get(dataDirectoryPath, "executions"));
            Files.createDirectories(Paths.get(dataDirectoryPath, "environments"));
        } catch (IOException e) {
            logger.error("Failed to create data directories", e);
        }
    }
    
    @Override
    public List<TestExecution> getRecentExecutions(int limit) {
        logger.debug("Getting {} recent executions", limit);
        try {
            // Read all test executions from the file system
            List<TestExecution> allExecutions = getAllExecutions();
            
            // Sort by start time descending and limit
            return allExecutions.stream()
                .sorted(Comparator.comparing(TestExecution::getStartTime).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting recent executions", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<TestExecution> getRecentExecutions(String projectId, int limit) {
        logger.debug("Getting {} recent executions for project {}", limit, projectId);
        try {
            // Read all test executions for the given project
            List<TestExecution> projectExecutions = getAllExecutions().stream()
                .filter(execution -> projectId.equals(execution.getProjectId()))
                .collect(Collectors.toList());
            
            // Sort by start time descending and limit
            return projectExecutions.stream()
                .sorted(Comparator.comparing(TestExecution::getStartTime).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting recent executions for project {}", projectId, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public Map<String, Integer> getTestCounts(String projectId) {
        logger.debug("Getting test counts for project {}", projectId);
        
        Map<String, Integer> counts = new HashMap<>();
        try {
            // Get all executions
            List<TestExecution> executions = getAllExecutions();
            
            // Filter by project if needed
            if (projectId != null && !projectId.isEmpty()) {
                executions = executions.stream()
                    .filter(execution -> projectId.equals(execution.getProjectId()))
                    .collect(Collectors.toList());
            }
            
            // Count by status
            Map<String, Long> statusCounts = executions.stream()
                .collect(Collectors.groupingBy(
                    execution -> execution.getStatus().toString().toLowerCase(),
                    Collectors.counting()
                ));
            
            // Convert Long to Integer
            statusCounts.forEach((status, count) -> 
                counts.put(status.toLowerCase(), count.intValue()));
            
            // Calculate total
            int total = counts.values().stream().mapToInt(Integer::intValue).sum();
            counts.put("total", total);
            
            // Ensure all statuses have an entry
            counts.putIfAbsent("passed", 0);
            counts.putIfAbsent("failed", 0);
            counts.putIfAbsent("skipped", 0);
            counts.putIfAbsent("running", 0);
            
        } catch (Exception e) {
            logger.error("Error getting test counts for project {}", projectId, e);
            // Return default counts on error
            counts.put("total", 0);
            counts.put("passed", 0);
            counts.put("failed", 0);
            counts.put("skipped", 0);
            counts.put("running", 0);
        }
        
        return counts;
    }
    
    @Override
    public Map<String, Integer> getTestCountsByStatus(LocalDate date, String projectId) {
        logger.debug("Getting test counts for date {} and project {}", date, projectId);
        
        Map<String, Integer> counts = new HashMap<>();
        try {
            // Calculate the start and end of the day
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
            
            // Get all executions
            List<TestExecution> executions = getAllExecutions();
            
            // Filter by date and project
            executions = executions.stream()
                .filter(execution -> {
                    LocalDateTime startTime = execution.getStartTime();
                    return startTime != null && 
                           !startTime.isBefore(startOfDay) && 
                           !startTime.isAfter(endOfDay) &&
                           (projectId == null || projectId.isEmpty() || projectId.equals(execution.getProjectId()));
                })
                .collect(Collectors.toList());
                
            // Count by status
            Map<String, Long> statusCounts = executions.stream()
                .collect(Collectors.groupingBy(
                    execution -> execution.getStatus().toString().toLowerCase(),
                    Collectors.counting()
                ));
            
            // Convert Long to Integer
            statusCounts.forEach((status, count) -> 
                counts.put(status.toLowerCase(), count.intValue()));
            
            // Calculate total
            int total = counts.values().stream().mapToInt(Integer::intValue).sum();
            counts.put("total", total);
            
            // Ensure all statuses have an entry
            counts.putIfAbsent("passed", 0);
            counts.putIfAbsent("failed", 0);
            counts.putIfAbsent("skipped", 0);
            
        } catch (Exception e) {
            logger.error("Error getting test counts for date {} and project {}", date, projectId, e);
            // Return default counts on error
            counts.put("total", 0);
            counts.put("passed", 0);
            counts.put("failed", 0);
            counts.put("skipped", 0);
        }
        
        return counts;
    }
    
    @Override
    public List<EnvironmentStatus> getEnvironmentConfigurations() {
        logger.debug("Getting environment configurations");
        
        try {
            // Read environment configurations from files
            return getEnvironmentConfigs();
        } catch (Exception e) {
            logger.error("Error getting environment configurations", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public boolean checkEnvironmentHealth(String url) {
        logger.debug("Checking health for environment {}", url);
        
        try {
            // Simple health check - try to connect to the URL
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestMethod("HEAD");
            
            // Get the response code
            int responseCode = connection.getResponseCode();
            
            // Return true if response code is 2xx (success)
            return responseCode >= 200 && responseCode < 300;
            
        } catch (Exception e) {
            logger.warn("Health check failed for {}: {}", url, e.getMessage());
            return false;
        }
    }
    
    @Override
    public int countExecutionsForDay(LocalDate date, String projectId) {
        logger.debug("Counting executions for date {} and project {}", date, projectId);
        
        try {
            // Calculate the start and end of the day
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
            
            // Get all executions and count those that match the criteria
            return (int) getAllExecutions().stream()
                .filter(execution -> {
                    LocalDateTime startTime = execution.getStartTime();
                    return startTime != null && 
                           !startTime.isBefore(startOfDay) && 
                           !startTime.isAfter(endOfDay) &&
                           (projectId == null || projectId.isEmpty() || projectId.equals(execution.getProjectId()));
                })
                .count();
            
        } catch (Exception e) {
            logger.error("Error counting executions for date {} and project {}", date, projectId, e);
            return 0;
        }
    }
    
    @Override
    public double getPassRateForDay(LocalDate date, String projectId) {
        logger.debug("Getting pass rate for date {} and project {}", date, projectId);
        
        try {
            // Calculate the start and end of the day
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
            
            // Get all executions for the day and project
            List<TestExecution> executions = getAllExecutions().stream()
                .filter(execution -> {
                    LocalDateTime startTime = execution.getStartTime();
                    return startTime != null && 
                           !startTime.isBefore(startOfDay) && 
                           !startTime.isAfter(endOfDay) &&
                           (projectId == null || projectId.isEmpty() || projectId.equals(execution.getProjectId()));
                })
                .collect(Collectors.toList());
            
            // Count total and passed executions
            int totalCount = executions.size();
            long passedCount = executions.stream()
                .filter(execution -> "PASSED".equals(execution.getStatus().toString()))
                .count();
            
            // Calculate pass rate
            if (totalCount > 0) {
                return ((double) passedCount / totalCount) * 100.0;
            } else {
                return 0.0;
            }
            
        } catch (Exception e) {
            logger.error("Error getting pass rate for date {} and project {}", date, projectId, e);
            return 0.0;
        }
    }
    
    @Override
    public double getAverageExecutionTimeForDay(LocalDate date, String projectId) {
        logger.debug("Getting average execution time for date {} and project {}", date, projectId);
        
        try {
            // Calculate the start and end of the day
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
            
            // Get all executions for the day and project
            List<TestExecution> executions = getAllExecutions().stream()
                .filter(execution -> {
                    LocalDateTime startTime = execution.getStartTime();
                    return startTime != null && 
                           execution.getEndTime() != null &&
                           !startTime.isBefore(startOfDay) && 
                           !startTime.isAfter(endOfDay) &&
                           (projectId == null || projectId.isEmpty() || projectId.equals(execution.getProjectId()));
                })
                .collect(Collectors.toList());
            
            // Calculate average execution time (in milliseconds)
            if (!executions.isEmpty()) {
                double totalDuration = executions.stream()
                    .mapToLong(execution -> {
                        long startMillis = execution.getStartTime().toInstant(ZoneOffset.UTC).toEpochMilli();
                        long endMillis = execution.getEndTime().toInstant(ZoneOffset.UTC).toEpochMilli();
                        return endMillis - startMillis;
                    })
                    .sum();
                
                return totalDuration / executions.size();
            } else {
                return 0.0;
            }
            
        } catch (Exception e) {
            logger.error("Error getting average execution time for date {} and project {}", date, projectId, e);
            return 0.0;
        }
    }
    
    @Override
    public List<FailureAnalysis.CommonFailure> getMostCommonFailures(LocalDateTime cutoffDate, String projectId) {
        logger.debug("Getting most common failures since {} for project {}", cutoffDate, projectId);
        
        try {
            // Get all failed executions since the cutoff date
            List<TestExecution> failedExecutions = getAllExecutions().stream()
                .filter(execution -> 
                    execution.getStartTime() != null &&
                    execution.getStartTime().isAfter(cutoffDate) &&
                    "FAILED".equals(execution.getStatus().toString()) &&
                    execution.getErrorMessage() != null &&
                    !execution.getErrorMessage().isEmpty() &&
                    (projectId == null || projectId.isEmpty() || projectId.equals(execution.getProjectId()))
                )
                .collect(Collectors.toList());
            
            // Group by error message and count occurrences
            Map<String, List<TestExecution>> failuresByMessage = failedExecutions.stream()
                .collect(Collectors.groupingBy(TestExecution::getErrorMessage));
            
            // Convert to CommonFailure objects
            List<FailureAnalysis.CommonFailure> commonFailures = failuresByMessage.entrySet().stream()
                .map(entry -> {
                    FailureAnalysis.CommonFailure failure = new FailureAnalysis.CommonFailure();
                    failure.setMessage(entry.getKey());
                    failure.setCount(entry.getValue().size());
                    
                    List<String> testIds = entry.getValue().stream()
                        .map(TestExecution::getId)
                        .collect(Collectors.toList());
                    failure.setTestIds(testIds);
                    
                    return failure;
                })
                .sorted(Comparator.comparing(FailureAnalysis.CommonFailure::getCount).reversed())
                .limit(10)
                .collect(Collectors.toList());
            
            return commonFailures;
            
        } catch (Exception e) {
            logger.error("Error getting most common failures since {} for project {}", cutoffDate, projectId, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<FailureAnalysis.FailureByType> getFailuresByType(LocalDateTime cutoffDate, String projectId) {
        logger.debug("Getting failures by type since {} for project {}", cutoffDate, projectId);
        
        try {
            // Get all failed executions since the cutoff date
            List<TestExecution> failedExecutions = getAllExecutions().stream()
                .filter(execution -> 
                    execution.getStartTime() != null &&
                    execution.getStartTime().isAfter(cutoffDate) &&
                    "FAILED".equals(execution.getStatus().toString()) &&
                    (projectId == null || projectId.isEmpty() || projectId.equals(execution.getProjectId()))
                )
                .collect(Collectors.toList());
            
            // Count total failures
            int totalFailures = failedExecutions.size();
            if (totalFailures == 0) {
                return Collections.emptyList();
            }
            
            // Group by error type and count occurrences
            Map<String, Long> failuresByType = failedExecutions.stream()
                .collect(Collectors.groupingBy(
                    execution -> execution.getErrorType() != null ? execution.getErrorType() : "Unknown",
                    Collectors.counting()
                ));
            
            // Convert to FailureByType objects
            List<FailureAnalysis.FailureByType> result = failuresByType.entrySet().stream()
                .map(entry -> {
                    FailureAnalysis.FailureByType failure = new FailureAnalysis.FailureByType();
                    failure.setType(entry.getKey());
                    failure.setCount(entry.getValue().intValue());
                    failure.setPercentage(((double) entry.getValue() / totalFailures) * 100.0);
                    return failure;
                })
                .sorted(Comparator.comparing(FailureAnalysis.FailureByType::getCount).reversed())
                .collect(Collectors.toList());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error getting failures by type since {} for project {}", cutoffDate, projectId, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<FailureAnalysis.FailureByBrowser> getFailuresByBrowser(LocalDateTime cutoffDate, String projectId) {
        logger.debug("Getting failures by browser since {} for project {}", cutoffDate, projectId);
        
        try {
            // Get all failed executions since the cutoff date
            List<TestExecution> failedExecutions = getAllExecutions().stream()
                .filter(execution -> 
                    execution.getStartTime() != null &&
                    execution.getStartTime().isAfter(cutoffDate) &&
                    "FAILED".equals(execution.getStatus().toString()) &&
                    execution.getBrowser() != null &&
                    !execution.getBrowser().isEmpty() &&
                    (projectId == null || projectId.isEmpty() || projectId.equals(execution.getProjectId()))
                )
                .collect(Collectors.toList());
            
            // Count total failures
            int totalFailures = failedExecutions.size();
            if (totalFailures == 0) {
                return Collections.emptyList();
            }
            
            // Group by browser and count occurrences
            Map<String, Long> failuresByBrowser = failedExecutions.stream()
                .collect(Collectors.groupingBy(TestExecution::getBrowser, Collectors.counting()));
            
            // Convert to FailureByBrowser objects
            List<FailureAnalysis.FailureByBrowser> result = failuresByBrowser.entrySet().stream()
                .map(entry -> {
                    FailureAnalysis.FailureByBrowser failure = new FailureAnalysis.FailureByBrowser();
                    failure.setBrowser(entry.getKey());
                    failure.setCount(entry.getValue().intValue());
                    failure.setPercentage(((double) entry.getValue() / totalFailures) * 100.0);
                    return failure;
                })
                .sorted(Comparator.comparing(FailureAnalysis.FailureByBrowser::getCount).reversed())
                .collect(Collectors.toList());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error getting failures by browser since {} for project {}", cutoffDate, projectId, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<FailureAnalysis.UnstableTest> getUnstableTests(LocalDateTime cutoffDate, String projectId) {
        logger.debug("Getting unstable tests since {} for project {}", cutoffDate, projectId);
        
        try {
            // Get all test executions since the cutoff date
            List<TestExecution> recentExecutions = getAllExecutions().stream()
                .filter(execution -> 
                    execution.getStartTime() != null &&
                    execution.getStartTime().isAfter(cutoffDate) &&
                    execution.getTestId() != null &&
                    !execution.getTestId().isEmpty() &&
                    (projectId == null || projectId.isEmpty() || projectId.equals(execution.getProjectId()))
                )
                .collect(Collectors.toList());
            
            // Group by test ID
            Map<String, List<TestExecution>> executionsByTest = recentExecutions.stream()
                .collect(Collectors.groupingBy(TestExecution::getTestId));
            
            // Find tests with mixed results (both passes and failures)
            List<FailureAnalysis.UnstableTest> unstableTests = new ArrayList<>();
            
            for (Map.Entry<String, List<TestExecution>> entry : executionsByTest.entrySet()) {
                String testId = entry.getKey();
                List<TestExecution> testExecutions = entry.getValue();
                
                // Only consider tests with at least 3 runs
                if (testExecutions.size() >= 3) {
                    long failureCount = testExecutions.stream()
                        .filter(execution -> "FAILED".equals(execution.getStatus().toString()))
                        .count();
                    
                    // Only include tests that have both failures and passes
                    if (failureCount > 0 && failureCount < testExecutions.size()) {
                        double failureRate = (double) failureCount / testExecutions.size();
                        
                        // Find the latest execution
                        TestExecution latestExecution = testExecutions.stream()
                            .max(Comparator.comparing(TestExecution::getStartTime))
                            .orElse(null);
                        
                        if (latestExecution != null) {
                            FailureAnalysis.UnstableTest unstableTest = new FailureAnalysis.UnstableTest();
                            unstableTest.setTestId(testId);
                            unstableTest.setTestName(getTestName(testId));
                            unstableTest.setFailureRate(failureRate * 100.0);
                            unstableTest.setLastExecuted(
                                latestExecution.getStartTime().toInstant(ZoneOffset.UTC).toEpochMilli()
                            );
                            
                            unstableTests.add(unstableTest);
                        }
                    }
                }
            }
            
            // Sort by failure rate descending and return top 10
            return unstableTests.stream()
                .sorted(Comparator.comparing(FailureAnalysis.UnstableTest::getFailureRate).reversed())
                .limit(10)
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error getting unstable tests since {} for project {}", cutoffDate, projectId, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public Map<String, Integer> getErrorCountsByType(String projectId) {
        logger.debug("Getting error counts by type for project {}", projectId);
        
        Map<String, Integer> errorCounts = new HashMap<>();
        try {
            // Get all failed executions for the project
            List<TestExecution> failedExecutions = getAllExecutions().stream()
                .filter(execution -> 
                    "FAILED".equals(execution.getStatus().toString()) &&
                    execution.getErrorType() != null &&
                    !execution.getErrorType().isEmpty() &&
                    (projectId == null || projectId.isEmpty() || projectId.equals(execution.getProjectId()))
                )
                .collect(Collectors.toList());
            
            // Group by error type and count occurrences
            Map<String, Long> countsByType = failedExecutions.stream()
                .collect(Collectors.groupingBy(
                    execution -> execution.getErrorType() != null ? execution.getErrorType() : "Unknown",
                    Collectors.counting()
                ));
            
            // Convert Long to Integer
            countsByType.forEach((type, count) -> 
                errorCounts.put(type, count.intValue()));
            
        } catch (Exception e) {
            logger.error("Error getting error counts by type for project {}", projectId, e);
        }
        
        return errorCounts;
    }
    
    @Override
    public long getTotalTestDuration(String projectId) {
        logger.debug("Getting total test duration for project {}", projectId);
        
        try {
            // Get all completed executions for the project
            return getAllExecutions().stream()
                .filter(execution -> 
                    execution.getStartTime() != null &&
                    execution.getEndTime() != null &&
                    (projectId == null || projectId.isEmpty() || projectId.equals(execution.getProjectId()))
                )
                .mapToLong(execution -> {
                    long startMillis = execution.getStartTime().toInstant(ZoneOffset.UTC).toEpochMilli();
                    long endMillis = execution.getEndTime().toInstant(ZoneOffset.UTC).toEpochMilli();
                    return endMillis - startMillis;
                })
                .sum();
                
        } catch (Exception e) {
            logger.error("Error getting total test duration for project {}", projectId, e);
            return 0L;
        }
    }
    
    @Override
    public byte[] generateReport(DashboardStats stats, String format, String projectId) {
        logger.debug("Generating {} report for project {}", format, projectId);
        
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            switch (format.toLowerCase()) {
                case "pdf":
                    generatePdfReport(stats, outputStream, projectId);
                    break;
                case "csv":
                    generateCsvReport(stats, outputStream, projectId);
                    break;
                case "excel":
                    generateExcelReport(stats, outputStream, projectId);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported report format: " + format);
            }
            
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            logger.error("Error generating {} report for project {}", format, projectId, e);
            return new byte[0];
        }
    }
    
    /**
     * Get all test executions from the file system
     * 
     * @return List of all test executions
     * @throws IOException If an I/O error occurs
     */
    private List<TestExecution> getAllExecutions() throws IOException {
        Path executionsDir = Paths.get(dataDirectoryPath, "executions");
        
        // Ensure directory exists
        Files.createDirectories(executionsDir);
        
        // Read all execution files
        try (Stream<Path> paths = Files.walk(executionsDir)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".json"))
                .map(this::readTestExecution)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Read a test execution from a file
     * 
     * @param path Path to the file
     * @return Optional containing the test execution, or empty if the file could not be read
     */
    private Optional<TestExecution> readTestExecution(Path path) {
        try {
            TestExecution execution = objectMapper.readValue(path.toFile(), TestExecution.class);
            return Optional.of(execution);
        } catch (IOException e) {
            logger.error("Error reading test execution from {}", path, e);
            return Optional.empty();
        }
    }
    
    /**
     * Get all environment configurations from the file system
     * 
     * @return List of environment configurations
     * @throws IOException If an I/O error occurs
     */
    private List<EnvironmentStatus> getEnvironmentConfigs() throws IOException {
        Path environmentsDir = Paths.get(dataDirectoryPath, "environments");
        
        // Ensure directory exists
        Files.createDirectories(environmentsDir);
        
        // Read all environment files
        try (Stream<Path> paths = Files.walk(environmentsDir)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".json"))
                .map(this::readEnvironmentStatus)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Read an environment status from a file
     * 
     * @param path Path to the file
     * @return Optional containing the environment status, or empty if the file could not be read
     */
    private Optional<EnvironmentStatus> readEnvironmentStatus(Path path) {
        try {
            EnvironmentStatus status = objectMapper.readValue(path.toFile(), EnvironmentStatus.class);
            return Optional.of(status);
        } catch (IOException e) {
            logger.error("Error reading environment status from {}", path, e);
            return Optional.empty();
        }
    }
    
    /**
     * Get the name of a test by its ID
     * 
     * @param testId Test ID
     * @return Test name, or a default name if the test cannot be found
     */
    private String getTestName(String testId) {
        try {
            // Try to find a test file with the given ID
            Path testFile = Paths.get(dataDirectoryPath, "tests", testId + ".json");
            if (Files.exists(testFile)) {
                // Read the test file and extract the name
                Map<String, Object> test = objectMapper.readValue(testFile.toFile(), Map.class);
                return (String) test.getOrDefault("name", "Test " + testId);
            }
        } catch (IOException e) {
            logger.error("Error getting test name for test ID {}", testId, e);
        }
        
        return "Test " + testId;
    }
    
    /**
     * Generate PDF report
     * 
     * @param stats Dashboard statistics
     * @param outputStream Output stream to write to
     * @param projectId Optional project ID
     */
    private void generatePdfReport(DashboardStats stats, ByteArrayOutputStream outputStream, String projectId) {
        // Implementation for PDF generation without third-party libraries
        // Use Java's built-in tools to create a PDF
        String content = "CSTestForge Dashboard Report\n\n";
        content += "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n\n";
        
        if (projectId != null && !projectId.isEmpty()) {
            content += "Project ID: " + projectId + "\n\n";
        }
        
        content += "Test Statistics:\n";
        content += "Total Tests: " + stats.getTotalTests() + "\n";
        content += "Passed Tests: " + stats.getPassedTests() + "\n";
        content += "Failed Tests: " + stats.getFailedTests() + "\n";
        content += "Skipped Tests: " + stats.getSkippedTests() + "\n";
        content += "Success Rate: " + String.format("%.2f%%", stats.getSuccessRate()) + "\n";
        
        // Add more sections with detailed data
        
        // Write content as bytes to output stream
        try {
            outputStream.write(content.getBytes());
        } catch (Exception e) {
            logger.error("Error writing PDF report content", e);
        }
    }
    
    /**
     * Generate CSV report
     * 
     * @param stats Dashboard statistics
     * @param outputStream Output stream to write to
     * @param projectId Optional project ID
     */
    private void generateCsvReport(DashboardStats stats, ByteArrayOutputStream outputStream, String projectId) {
        // Implementation for CSV generation without third-party libraries
        StringBuilder csv = new StringBuilder();
        
        // Add header
        csv.append("Metric,Value\n");
        
        // Add data
        csv.append("Generated,").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        
        if (projectId != null && !projectId.isEmpty()) {
            csv.append("Project ID,").append(projectId).append("\n");
        }
        
        csv.append("Total Tests,").append(stats.getTotalTests()).append("\n");
        csv.append("Passed Tests,").append(stats.getPassedTests()).append("\n");
        csv.append("Failed Tests,").append(stats.getFailedTests()).append("\n");
        csv.append("Skipped Tests,").append(stats.getSkippedTests()).append("\n");
        csv.append("Success Rate,").append(String.format("%.2f%%", stats.getSuccessRate())).append("\n");
        
        // Add more sections with detailed data
        
        // Write content as bytes to output stream
        try {
            outputStream.write(csv.toString().getBytes());
        } catch (Exception e) {
            logger.error("Error writing CSV report content", e);
        }
    }
    
    /**
     * Generate Excel report
     * 
     * @param stats Dashboard statistics
     * @param outputStream Output stream to write to
     * @param projectId Optional project ID
     */
    private void generateExcelReport(DashboardStats stats, ByteArrayOutputStream outputStream, String projectId) {
        // Implementation for Excel generation without third-party libraries
        // For this example, we'll just create a CSV that Excel can open
        generateCsvReport(stats, outputStream, projectId);
    }
} 