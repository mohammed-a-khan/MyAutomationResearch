package com.cstestforge.execution.service;

import com.cstestforge.execution.model.*;
import com.cstestforge.project.model.execution.TestExecution;
import com.cstestforge.project.model.execution.TestExecutionStatus;
import com.cstestforge.project.service.ProjectService;
import com.cstestforge.storage.repository.TestExecutionRepository;
import com.cstestforge.testing.model.TestCase;
import com.cstestforge.testing.service.TestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Implementation of the TestExecutionService
 */
@Service
public class TestExecutionServiceImpl implements TestExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(TestExecutionServiceImpl.class);
    
    private final TestExecutionRepository executionRepository;
    private final TestService testService;
    private final ProjectService projectService;
    
    // Track ongoing executions
    private final Map<String, TestExecutionInfo> activeExecutions = new ConcurrentHashMap<>();
    
    // Executor for running tests in parallel
    private final Executor testExecutor = Executors.newCachedThreadPool();

    @Autowired
    public TestExecutionServiceImpl(
            TestExecutionRepository executionRepository,
            TestService testService,
            ProjectService projectService) {
        this.executionRepository = executionRepository;
        this.testService = testService;
        this.projectService = projectService;
    }

    @Override
    public TestExecutionInfo runTests(TestExecutionRequest request) {
        logger.info("Starting test execution for project {}: {} tests", 
                request.getProjectId(), request.getTestIds().size());
        
        // Validate project exists
        projectService.findById(request.getProjectId())
            .orElseThrow(() -> new IllegalArgumentException("Project not found with ID: " + request.getProjectId()));
        
        // Create execution record
        String executionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        
        // Create test execution info
        TestExecutionInfo executionInfo = new TestExecutionInfo(
                executionId, 
                request.getProjectId(), 
                TestStatus.QUEUED,
                now,
                request.getConfig().getEnvironment(), 
                request.getConfig().getBrowser()
        );
        
        executionInfo.setConfig(request.getConfig());
        executionInfo.setTotalTests(request.getTestIds().size());
        executionInfo.setQueuedTests(request.getTestIds().size());
        
        // Save to repository
        TestExecution execution = convertToEntity(executionInfo);
        executionRepository.create(request.getProjectId(), execution);
        
        // Store in active executions
        activeExecutions.put(executionId, executionInfo);
        
        // Start execution asynchronously
        runTestsAsync(executionId, request);
        
        return executionInfo;
    }

    @Override
    public TestExecutionInfo getExecutionStatus(String executionId) {
        // Check active executions first to get real-time status
        if (activeExecutions.containsKey(executionId)) {
            return activeExecutions.get(executionId);
        }
        
        // Otherwise load from repository
        String projectId = getProjectIdFromExecutionId(executionId);
        Optional<TestExecution> execution = executionRepository.findById(projectId, executionId);
        return execution.map(this::convertToInfo).orElseThrow(() -> 
                new IllegalArgumentException("Execution not found with ID: " + executionId));
    }

    @Override
    public TestExecutionInfo stopExecution(String executionId) {
        logger.info("Stopping execution: {}", executionId);
        
        // Get current execution
        TestExecutionInfo executionInfo = getExecutionStatus(executionId);
        
        if (executionInfo.getStatus() == TestStatus.RUNNING || 
            executionInfo.getStatus() == TestStatus.QUEUED) {
            
            // Update status to aborted
            executionInfo.setStatus(TestStatus.ABORTED);
            executionInfo.setEndTime(LocalDateTime.now());
            
            // Update in repository
            String projectId = getProjectIdFromExecutionId(executionId);
            TestExecution execution = convertToEntity(executionInfo);
            executionRepository.update(projectId, executionId, execution);
            
            // Update active executions cache
            if (activeExecutions.containsKey(executionId)) {
                activeExecutions.put(executionId, executionInfo);
            }
            
            logger.info("Execution {} stopped successfully", executionId);
        }
        
        return executionInfo;
    }

    @Override
    public TestExecutionInfo getExecutionDetails(String executionId) {
        logger.info("Getting detailed execution information for execution ID: {}", executionId);
        
        // Get basic execution information
        TestExecutionInfo executionInfo = getExecutionStatus(executionId);
        String projectId = executionInfo.getProjectId();
        
        try {
            // Load execution from repository
            TestExecution execution = executionRepository.findById(projectId, executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found with ID: " + executionId));
            
            // Load test-specific results if available
            Map<String, Object> metrics = execution.getMetrics();
            Map<String, Object> metadata = execution.getMetadata();
            
            // Add test-level details
            enrichWithTestDetails(executionInfo, projectId, executionId);
            
            // Process execution metrics for detailed analysis
            if (metrics != null && !metrics.isEmpty()) {
                processExecutionMetrics(executionInfo, metrics);
            }
            
            // Add artifact references (screenshots, videos, logs)
            Map<String, List<String>> artifacts = loadExecutionArtifacts(projectId, executionId);
            if (artifacts != null && !artifacts.isEmpty()) {
                // Add artifact information to execution info via custom settings
                Map<String, Object> customSettings = new HashMap<>();
                customSettings.put("artifacts", artifacts);
                executionInfo.getConfig().setCustomSettings(customSettings);
            }
            
            // Load environment variables from execution
            if (metadata != null && metadata.containsKey("environment")) {
                @SuppressWarnings("unchecked")
                Map<String, String> environmentVars = (Map<String, String>) metadata.get("environment");
                enrichExecutionWithEnvironment(executionInfo, environmentVars);
            }
            
            logger.debug("Execution details loaded for {}", executionId);
            return executionInfo;
        } catch (Exception e) {
            logger.error("Error loading detailed execution information for {}", executionId, e);
            // Fall back to basic execution info if we can't load details
            return executionInfo;
        }
    }

    /**
     * Enrich the execution info with test-level details
     * 
     * @param executionInfo Execution info to enhance
     * @param projectId Project ID
     * @param executionId Execution ID
     */
    private void enrichWithTestDetails(TestExecutionInfo executionInfo, String projectId, String executionId) {
        try {
            // In a production system, we would retrieve detailed test results
            // from a test results repository or database
            List<Map<String, Object>> testResults = getTestResults(projectId, executionId);
            
            if (testResults != null && !testResults.isEmpty()) {
                // Calculate additional metrics
                int longestTest = 0;
                int shortestTest = Integer.MAX_VALUE;
                double avgDuration = 0;
                List<Long> durations = new ArrayList<>();
                
                // Process each test result
                for (Map<String, Object> result : testResults) {
                    if (result.containsKey("duration")) {
                        long duration = (Long) result.get("duration");
                        durations.add(duration);
                        longestTest = (int) Math.max(longestTest, duration);
                        shortestTest = (int) Math.min(shortestTest, duration);
                    }
                }
                
                // Calculate average duration
                if (!durations.isEmpty()) {
                    avgDuration = durations.stream().mapToLong(Long::longValue).average().orElse(0);
                    
                    // Add to execution metadata via custom settings
                    Map<String, Object> customSettings = new HashMap<>();
                    customSettings.put("testMetrics", Map.of(
                        "longestTestMs", longestTest,
                        "shortestTestMs", shortestTest,
                        "avgTestDurationMs", avgDuration
                    ));
                    
                    // Initialize custom settings if needed
                    if (executionInfo.getConfig().getCustomSettings() == null) {
                        executionInfo.getConfig().setCustomSettings(customSettings);
                    } else {
                        executionInfo.getConfig().getCustomSettings().putAll(customSettings);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not enrich execution with test details: {}", e.getMessage());
        }
    }
    
    /**
     * Get detailed test results for an execution
     * 
     * @param projectId Project ID
     * @param executionId Execution ID
     * @return List of test result maps
     */
    private List<Map<String, Object>> getTestResults(String projectId, String executionId) {
        // In a production system, this would come from a dedicated test results store
        // For now, we'll query the repository for test-specific results
        try {
            // Get executions for test cases in this execution
            List<TestExecution> testExecutions = executionRepository.findByTestRunId(projectId, executionId);
            
            // Extract test-specific results
            List<Map<String, Object>> testResults = new ArrayList<>();
            
            for (TestExecution testExec : testExecutions) {
                Map<String, Object> result = new HashMap<>();
                result.put("testId", testExec.getMetadata().getOrDefault("testId", "unknown"));
                result.put("testName", testExec.getMetadata().getOrDefault("testName", "Unknown Test"));
                result.put("status", testExec.getStatus().toString());
                
                // Calculate duration if available
                if (testExec.getStartTime() != null && testExec.getEndTime() != null) {
                    long duration = java.time.Duration.between(
                        testExec.getStartTime(), testExec.getEndTime()).toMillis();
                    result.put("duration", duration);
                }
                
                // Add error information if failed
                if (testExec.getStatus() == TestExecutionStatus.FAILED 
                        || testExec.getStatus() == TestExecutionStatus.ERROR) {
                    result.put("errorMessage", testExec.getMetadata().getOrDefault("errorMessage", ""));
                    result.put("stackTrace", testExec.getMetadata().getOrDefault("stackTrace", ""));
                }
                
                testResults.add(result);
            }
            
            return testResults;
        } catch (Exception e) {
            logger.warn("Error retrieving test results for execution {}: {}", executionId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Process execution metrics and add them to execution info
     * 
     * @param executionInfo Execution info to enhance
     * @param metrics Execution metrics
     */
    private void processExecutionMetrics(TestExecutionInfo executionInfo, Map<String, Object> metrics) {
        logger.debug("Processing metrics for execution {}", executionInfo.getId());
        
        try {
            // Extract performance metrics if available
            Map<String, Object> performanceMetrics = new HashMap<>();
            
            // CPU usage - average/peak
            if (metrics.containsKey("cpuUsage")) {
                performanceMetrics.put("cpuUsage", metrics.get("cpuUsage"));
            }
            
            // Memory usage - average/peak
            if (metrics.containsKey("memoryUsage")) {
                performanceMetrics.put("memoryUsage", metrics.get("memoryUsage"));
            }
            
            // Network traffic
            if (metrics.containsKey("networkTraffic")) {
                performanceMetrics.put("networkTraffic", metrics.get("networkTraffic"));
            }
            
            // Page load times
            if (metrics.containsKey("pageLoadTimes")) {
                performanceMetrics.put("pageLoadTimes", metrics.get("pageLoadTimes"));
            }
            
            // Test step durations
            if (metrics.containsKey("stepDurations")) {
                performanceMetrics.put("stepDurations", metrics.get("stepDurations"));
            }
            
            // Calculate success rate
            int total = executionInfo.getTotalTests();
            int passed = executionInfo.getPassedTests();
            if (total > 0) {
                double successRate = (double) passed / total * 100;
                performanceMetrics.put("successRate", Math.round(successRate * 100) / 100.0); // Round to 2 decimal places
            }
            
            // Calculate test execution efficiency
            if (executionInfo.getDuration() != null && total > 0) {
                double avgTestTime = (double) executionInfo.getDuration() / total;
                performanceMetrics.put("avgTestTime", Math.round(avgTestTime * 100) / 100.0);
            }
            
            // Store the performance metrics in the execution info via custom settings
            if (!performanceMetrics.isEmpty()) {
                // Initialize custom settings if needed
                if (executionInfo.getConfig() == null) {
                    executionInfo.setConfig(new ExecutionConfig());
                }
                
                Map<String, Object> customSettings = executionInfo.getConfig().getCustomSettings();
                if (customSettings == null) {
                    customSettings = new HashMap<>();
                    executionInfo.getConfig().setCustomSettings(customSettings);
                }
                
                customSettings.put("performance", performanceMetrics);
            }
            
            logger.debug("Processed {} metrics for execution {}", metrics.size(), executionInfo.getId());
        } catch (Exception e) {
            logger.warn("Error processing metrics for execution {}: {}", executionInfo.getId(), e.getMessage());
        }
    }
    
    /**
     * Load execution artifacts like screenshots, videos, logs
     * 
     * @param projectId Project ID
     * @param executionId Execution ID
     * @return Map of artifact types to artifact paths
     */
    private Map<String, List<String>> loadExecutionArtifacts(String projectId, String executionId) {
        try {
            // In a production environment, this would retrieve artifact locations
            // from a file system, object storage, or database
            Map<String, List<String>> artifacts = new HashMap<>();
            
            // Define artifact paths - in production this would be retrieved from storage
            String basePath = "/api/artifacts/" + projectId + "/" + executionId;
            
            // Check if screenshots exist
            List<String> screenshotPaths = getArtifactPaths(projectId, executionId, "screenshots");
            if (!screenshotPaths.isEmpty()) {
                artifacts.put("screenshots", screenshotPaths);
            }
            
            // Check if videos exist
            List<String> videoPaths = getArtifactPaths(projectId, executionId, "videos");
            if (!videoPaths.isEmpty()) {
                artifacts.put("videos", videoPaths);
            }
            
            // Check if logs exist
            List<String> logPaths = getArtifactPaths(projectId, executionId, "logs");
            if (!logPaths.isEmpty()) {
                artifacts.put("logs", logPaths);
            }
            
            return artifacts;
        } catch (Exception e) {
            logger.warn("Error loading artifacts for execution {}: {}", executionId, e.getMessage());
            return Collections.emptyMap();
        }
    }
    
    /**
     * Get artifact paths for a specific artifact type
     * 
     * @param projectId Project ID
     * @param executionId Execution ID
     * @param artifactType Type of artifact
     * @return List of artifact paths
     */
    private List<String> getArtifactPaths(String projectId, String executionId, String artifactType) {
        try {
            // In a production environment, this would check a storage system
            // and return the paths to artifacts of the specified type
            String basePath = "/api/artifacts/" + projectId + "/" + executionId + "/" + artifactType;
            
            // This is a placeholder implementation that would be replaced
            // by actual filesystem or storage API calls in production
            List<String> paths = new ArrayList<>();
            
            // Simulate checking for artifacts based on test results
            List<Map<String, Object>> testResults = getTestResults(projectId, executionId);
            for (Map<String, Object> result : testResults) {
                String testId = result.get("testId").toString();
                if (artifactType.equals("screenshots")) {
                    // Add screenshots for failed tests
                    if ("FAILED".equals(result.get("status")) || "ERROR".equals(result.get("status"))) {
                        paths.add(basePath + "/" + testId + "/failure.png");
                    }
                } else if (artifactType.equals("videos")) {
                    // Add videos for all tests if video recording was enabled
                    // (would check execution config in production)
                    paths.add(basePath + "/" + testId + "/recording.mp4");
                } else if (artifactType.equals("logs")) {
                    // Add logs for all tests
                    paths.add(basePath + "/" + testId + "/test.log");
                    // Add browser console logs if available
                    paths.add(basePath + "/" + testId + "/browser-console.log");
                }
            }
            
            return paths;
        } catch (Exception e) {
            logger.warn("Error getting {} paths for execution {}: {}", 
                artifactType, executionId, e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Enrich execution with environment information
     * 
     * @param executionInfo Execution info to enhance
     * @param environmentVars Environment variables
     */
    private void enrichExecutionWithEnvironment(TestExecutionInfo executionInfo, Map<String, String> environmentVars) {
        try {
            if (environmentVars != null && !environmentVars.isEmpty()) {
                // Initialize custom settings if needed
                if (executionInfo.getConfig() == null) {
                    executionInfo.setConfig(new ExecutionConfig());
                }
                
                Map<String, Object> customSettings = executionInfo.getConfig().getCustomSettings();
                if (customSettings == null) {
                    customSettings = new HashMap<>();
                    executionInfo.getConfig().setCustomSettings(customSettings);
                }
                
                // Add environment information
                customSettings.put("environmentVars", environmentVars);
            }
        } catch (Exception e) {
            logger.warn("Error enriching execution with environment: {}", e.getMessage());
        }
    }

    @Override
    public List<TestExecutionInfo> getExecutionHistory(String projectId, int limit, int offset) {
        List<TestExecution> executions = executionRepository.findAll(projectId);
        
        // Sort by start time (descending) and apply pagination
        return executions.stream()
                .sorted(Comparator.comparing(TestExecution::getStartTime).reversed())
                .skip(offset)
                .limit(limit)
                .map(this::convertToInfo)
                .collect(Collectors.toList());
    }

    @Override
    public List<TestExecutionInfo> getTestExecutionHistory(String projectId, String testId, int limit) {
        List<TestExecution> executions = executionRepository.findByTestId(projectId, testId);
        
        // Sort by start time (descending) and limit
        return executions.stream()
                .sorted(Comparator.comparing(TestExecution::getStartTime).reversed())
                .limit(limit)
                .map(this::convertToInfo)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteExecution(String executionId) {
        String projectId = getProjectIdFromExecutionId(executionId);
        
        // Remove from active executions if present
        activeExecutions.remove(executionId);
        
        // Delete from repository
        return executionRepository.delete(projectId, executionId);
    }

    @Override
    public Optional<TestExecutionInfo> getLatestTestExecution(String projectId, String testId) {
        Optional<TestExecution> execution = executionRepository.findLatestByTestId(projectId, testId);
        return execution.map(this::convertToInfo);
    }

    @Override
    public List<TestExecutionInfo> getExecutionsByStatus(String projectId, TestStatus status, int limit) {
        // Convert frontend status to backend status
        TestExecutionStatus repoStatus = mapStatus(status);
        
        // Create filter parameters
        Map<String, Object> filters = new HashMap<>();
        filters.put("status", repoStatus);
        
        List<TestExecution> executions = executionRepository.findByFilters(projectId, filters);
        
        // Sort by start time (descending) and limit
        return executions.stream()
                .sorted(Comparator.comparing(TestExecution::getStartTime).reversed())
                .limit(limit)
                .map(this::convertToInfo)
                .collect(Collectors.toList());
    }

    @Override
    public int cleanupOldExecutions(String projectId, int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minus(daysToKeep, ChronoUnit.DAYS);
        return executionRepository.deleteOlderThan(projectId, cutoffDate);
    }
    
    /**
     * Run tests asynchronously
     * 
     * @param executionId Execution ID
     * @param request Test execution request
     */
    @Async
    protected void runTestsAsync(String executionId, TestExecutionRequest request) {
        try {
            String projectId = request.getProjectId();
            TestExecutionInfo executionInfo = activeExecutions.get(executionId);
            
            // Update status to running
            executionInfo.setStatus(TestStatus.RUNNING);
            executionInfo.setRunningTests(0);
            executionInfo.setQueuedTests(request.getTestIds().size());
            
            // Update repository
            updateExecutionInfo(executionInfo);
            
            // Load test cases
            List<TestCase> testCases = loadTestCases(projectId, request.getTestIds());
            
            // Execute tests based on configuration
            if (request.getConfig().isParallel()) {
                executeTestsInParallel(executionInfo, testCases, request.getConfig());
            } else {
                executeTestsSequentially(executionInfo, testCases, request.getConfig());
            }
            
            // All tests are completed at this point
            finalizeExecution(executionInfo);
            
        } catch (Exception e) {
            logger.error("Error executing tests for execution {}", executionId, e);
            
            // Update execution status to error
            TestExecutionInfo executionInfo = activeExecutions.get(executionId);
            if (executionInfo != null) {
                executionInfo.setStatus(TestStatus.ERROR);
                executionInfo.setEndTime(LocalDateTime.now());
                updateExecutionInfo(executionInfo);
            }
        } finally {
            // Remove from active executions after a delay to allow clients to fetch final status
            CompletableFuture.delayedExecutor(5, java.util.concurrent.TimeUnit.MINUTES)
                    .execute(() -> activeExecutions.remove(executionId));
        }
    }
    
    /**
     * Execute tests in parallel
     * 
     * @param executionInfo Execution info to update
     * @param testCases Test cases to execute
     * @param config Execution configuration
     */
    private void executeTestsInParallel(TestExecutionInfo executionInfo, List<TestCase> testCases, ExecutionConfig config) {
        int maxParallel = Math.min(config.getMaxParallel(), testCases.size());
        
        // Prepare test cases for parallel execution
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        List<TestCase> queuedTests = new ArrayList<>(testCases);
        
        executionInfo.setRunningTests(Math.min(maxParallel, queuedTests.size()));
        executionInfo.setQueuedTests(Math.max(0, queuedTests.size() - maxParallel));
        updateExecutionInfo(executionInfo);
        
        // Execute tests in parallel
        for (int i = 0; i < maxParallel; i++) {
            if (!queuedTests.isEmpty()) {
                TestCase testCase = queuedTests.remove(0);
                futures.add(CompletableFuture.runAsync(() -> {
                    executeTest(executionInfo, testCase, config);
                    
                    // Handle queued tests
                    synchronized (queuedTests) {
                        if (!queuedTests.isEmpty()) {
                            TestCase nextTest = queuedTests.remove(0);
                            executionInfo.setQueuedTests(queuedTests.size());
                            updateExecutionInfo(executionInfo);
                            
                            executeTest(executionInfo, nextTest, config);
                        }
                    }
                }, testExecutor));
            }
        }
        
        // Wait for all tests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
    
    /**
     * Execute tests sequentially
     * 
     * @param executionInfo Execution info to update
     * @param testCases Test cases to execute
     * @param config Execution configuration
     */
    private void executeTestsSequentially(TestExecutionInfo executionInfo, List<TestCase> testCases, ExecutionConfig config) {
        executionInfo.setRunningTests(1);
        executionInfo.setQueuedTests(testCases.size() - 1);
        updateExecutionInfo(executionInfo);
        
        for (TestCase testCase : testCases) {
            executeTest(executionInfo, testCase, config);
            
            // Update queue count for next test
            executionInfo.setQueuedTests(Math.max(0, executionInfo.getQueuedTests() - 1));
            updateExecutionInfo(executionInfo);
        }
    }
    
    /**
     * Execute a single test
     * 
     * @param executionInfo Execution info to update
     * @param testCase Test case to execute
     * @param config Execution configuration
     */
    private void executeTest(TestExecutionInfo executionInfo, TestCase testCase, ExecutionConfig config) {
        // Simulation of test execution - in a real implementation, this would execute the test
        // against the actual system under test
        
        logger.info("Executing test: {}", testCase.getName());
        
        try {
            // Simulate test execution time
            Thread.sleep((long) (Math.random() * 2000) + 500);
            
            // Randomly determine test result for simulation purposes
            TestStatus testResult = generateRandomTestResult();
            
            // Update execution counts based on test result
            synchronized (executionInfo) {
                switch (testResult) {
                    case PASSED:
                        executionInfo.setPassedTests(executionInfo.getPassedTests() + 1);
                        break;
                    case FAILED:
                        executionInfo.setFailedTests(executionInfo.getFailedTests() + 1);
                        break;
                    case SKIPPED:
                        executionInfo.setSkippedTests(executionInfo.getSkippedTests() + 1);
                        break;
                    case ERROR:
                        executionInfo.setErrorTests(executionInfo.getErrorTests() + 1);
                        break;
                    default:
                        break;
                }
            }
            
            updateExecutionInfo(executionInfo);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Test execution interrupted: {}", testCase.getName(), e);
        }
    }
    
    /**
     * Finalize execution
     * 
     * @param executionInfo Execution info to update
     */
    private void finalizeExecution(TestExecutionInfo executionInfo) {
        // Determine final status
        TestStatus finalStatus = TestStatus.PASSED;
        
        if (executionInfo.getFailedTests() > 0 || executionInfo.getErrorTests() > 0) {
            finalStatus = TestStatus.FAILED;
        } else if (executionInfo.getTotalTests() == executionInfo.getSkippedTests()) {
            finalStatus = TestStatus.SKIPPED;
        }
        
        // Update execution info
        executionInfo.setStatus(finalStatus);
        executionInfo.setEndTime(LocalDateTime.now());
        executionInfo.setRunningTests(0);
        executionInfo.setQueuedTests(0);
        
        // Save to repository
        updateExecutionInfo(executionInfo);
        
        logger.info("Execution completed: {} - Status: {}, Passed: {}, Failed: {}, Skipped: {}, Errors: {}",
                executionInfo.getId(), executionInfo.getStatus(),
                executionInfo.getPassedTests(), executionInfo.getFailedTests(),
                executionInfo.getSkippedTests(), executionInfo.getErrorTests());
    }
    
    /**
     * Convert TestExecutionInfo to TestExecution entity
     * 
     * @param info Execution info
     * @return TestExecution entity
     */
    private TestExecution convertToEntity(TestExecutionInfo info) {
        TestExecution entity = new TestExecution();
        
        entity.setId(info.getId());
        entity.setProjectId(info.getProjectId());
        entity.setStatus(mapStatus(info.getStatus()));
        entity.setStartTime(info.getStartTime());
        entity.setEndTime(info.getEndTime());
        entity.setEnvironment(info.getEnvironment());
        entity.setBrowser(info.getBrowser().toString());
        
        // Set execution metrics
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalTests", info.getTotalTests());
        metrics.put("passedTests", info.getPassedTests());
        metrics.put("failedTests", info.getFailedTests());
        metrics.put("skippedTests", info.getSkippedTests());
        metrics.put("errorTests", info.getErrorTests());
        entity.setMetrics(metrics);
        
        // Store config as metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("headless", info.getConfig().isHeadless());
        metadata.put("parallel", info.getConfig().isParallel());
        metadata.put("maxParallel", info.getConfig().getMaxParallel());
        metadata.put("timeoutSeconds", info.getConfig().getTimeoutSeconds());
        metadata.put("retryCount", info.getConfig().getRetryCount());
        metadata.put("screenshotsEnabled", info.getConfig().isScreenshotsEnabled());
        metadata.put("videoEnabled", info.getConfig().isVideoEnabled());
        entity.setMetadata(metadata);
        
        return entity;
    }
    
    /**
     * Convert TestExecution entity to TestExecutionInfo
     * 
     * @param entity TestExecution entity
     * @return Execution info
     */
    private TestExecutionInfo convertToInfo(TestExecution entity) {
        TestExecutionInfo info = new TestExecutionInfo();
        
        info.setId(entity.getId());
        info.setProjectId(entity.getProjectId());
        info.setStatus(mapStatus(entity.getStatus()));
        info.setStartTime(entity.getStartTime());
        info.setEndTime(entity.getEndTime());
        info.setEnvironment(entity.getEnvironment());
        info.setBrowser(BrowserType.valueOf(entity.getBrowser().toUpperCase()));
        info.setCreatedBy(entity.getTriggeredBy());
        
        // Calculate duration if start and end times are available
        if (entity.getStartTime() != null && entity.getEndTime() != null) {
            info.setDuration(java.time.Duration.between(entity.getStartTime(), entity.getEndTime()).toMillis());
        }
        
        // Get metrics
        Map<String, Object> metrics = entity.getMetrics();
        if (metrics != null) {
            info.setTotalTests(getMetricValue(metrics, "totalTests", 0));
            info.setPassedTests(getMetricValue(metrics, "passedTests", 0));
            info.setFailedTests(getMetricValue(metrics, "failedTests", 0));
            info.setSkippedTests(getMetricValue(metrics, "skippedTests", 0));
            info.setErrorTests(getMetricValue(metrics, "errorTests", 0));
            info.setRunningTests(getMetricValue(metrics, "runningTests", 0));
            info.setQueuedTests(getMetricValue(metrics, "queuedTests", 0));
        }
        
        // Get config from metadata
        Map<String, Object> metadata = entity.getMetadata();
        ExecutionConfig config = new ExecutionConfig();
        if (metadata != null) {
            config.setHeadless(getMetadataBoolean(metadata, "headless", false));
            config.setParallel(getMetadataBoolean(metadata, "parallel", true));
            config.setMaxParallel(getMetadataInt(metadata, "maxParallel", 3));
            config.setTimeoutSeconds(getMetadataInt(metadata, "timeoutSeconds", 30));
            config.setRetryCount(getMetadataInt(metadata, "retryCount", 1));
            config.setScreenshotsEnabled(getMetadataBoolean(metadata, "screenshotsEnabled", true));
            config.setVideoEnabled(getMetadataBoolean(metadata, "videoEnabled", false));
        }
        info.setConfig(config);
        
        return info;
    }
    
    /**
     * Extract metric value from metrics map
     * 
     * @param metrics Metrics map
     * @param key Metric key
     * @param defaultValue Default value
     * @return Metric value or default
     */
    private int getMetricValue(Map<String, Object> metrics, String key, int defaultValue) {
        Object value = metrics.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * Extract boolean value from metadata map
     * 
     * @param metadata Metadata map
     * @param key Metadata key
     * @param defaultValue Default value
     * @return Boolean value or default
     */
    private boolean getMetadataBoolean(Map<String, Object> metadata, String key, boolean defaultValue) {
        Object value = metadata.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
    
    /**
     * Extract integer value from metadata map
     * 
     * @param metadata Metadata map
     * @param key Metadata key
     * @param defaultValue Default value
     * @return Integer value or default
     */
    private int getMetadataInt(Map<String, Object> metadata, String key, int defaultValue) {
        Object value = metadata.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * Map TestStatus to TestExecutionStatus
     * 
     * @param status TestStatus
     * @return TestExecutionStatus
     */
    private TestExecutionStatus mapStatus(TestStatus status) {
        switch (status) {
            case QUEUED:
                return TestExecutionStatus.QUEUED;
            case RUNNING:
                return TestExecutionStatus.RUNNING;
            case PASSED:
                return TestExecutionStatus.PASSED;
            case FAILED:
                return TestExecutionStatus.FAILED;
            case SKIPPED:
                return TestExecutionStatus.SKIPPED;
            case ERROR:
                return TestExecutionStatus.ERROR;
            case BLOCKED:
                return TestExecutionStatus.BLOCKED;
            case ABORTED:
                return TestExecutionStatus.ABORTED;
            default:
                return TestExecutionStatus.PENDING;
        }
    }
    
    /**
     * Map TestExecutionStatus to TestStatus
     * 
     * @param status TestExecutionStatus
     * @return TestStatus
     */
    private TestStatus mapStatus(TestExecutionStatus status) {
        switch (status) {
            case QUEUED:
                return TestStatus.QUEUED;
            case RUNNING:
                return TestStatus.RUNNING;
            case PASSED:
                return TestStatus.PASSED;
            case FAILED:
                return TestStatus.FAILED;
            case SKIPPED:
                return TestStatus.SKIPPED;
            case ERROR:
                return TestStatus.ERROR;
            case BLOCKED:
                return TestStatus.BLOCKED;
            case ABORTED:
                return TestStatus.ABORTED;
            default:
                return TestStatus.QUEUED;
        }
    }
    
    /**
     * Get project ID from execution ID
     * 
     * @param executionId Execution ID
     * @return Project ID
     */
    private String getProjectIdFromExecutionId(String executionId) {
        logger.debug("Resolving project ID for execution: {}", executionId);
        
        // First check in active executions cache
        if (activeExecutions.containsKey(executionId)) {
            String projectId = activeExecutions.get(executionId).getProjectId();
            logger.debug("Found project ID {} in active executions for execution {}", projectId, executionId);
            return projectId;
        }
        
        try {
            // Since we don't have a direct method to get project ID for execution,
            // we'll need to search through each project's executions
            // Get a list of all projects (Note: This would typically come from ProjectService)
            List<String> projectIds = getProjectIdsFromExecutionHistory();
            
            for (String projectId : projectIds) {
                if (executionRepository.findById(projectId, executionId).isPresent()) {
                    logger.debug("Found execution {} in project {}", executionId, projectId);
                    return projectId;
                }
            }
        } catch (Exception e) {
            logger.error("Error resolving project ID for execution {}", executionId, e);
        }
        
        // If all else fails, throw an exception
        throw new IllegalStateException("Could not resolve project ID for execution: " + executionId);
    }
    
    /**
     * Get a list of project IDs that have execution history
     * In a real implementation, this would come from ProjectService
     * 
     * @return List of project IDs
     */
    private List<String> getProjectIdsFromExecutionHistory() {
        // This is a fallback implementation that would be replaced with
        // a proper service call in a production system
        List<String> projectIds = new ArrayList<>();
        
        try {
            // For now, use the projectService to get all project IDs
            projectService.findAll(null)
                .getItems()
                .forEach(project -> projectIds.add(project.getId()));
                
            // If no projects found, add a default for testing
            if (projectIds.isEmpty()) {
                projectIds.add("default-project");
            }
        } catch (Exception e) {
            logger.error("Error getting project IDs", e);
            // Return a default value for fallback
            projectIds.add("default-project");
        }
        
        return projectIds;
    }
    
    /**
     * Load test cases by IDs
     * 
     * @param projectId Project ID
     * @param testIds Test IDs
     * @return List of test cases
     */
    private List<TestCase> loadTestCases(String projectId, List<String> testIds) {
        List<TestCase> testCases = new ArrayList<>();
        
        for (String testId : testIds) {
            testService.getTestById(projectId, testId)
                    .ifPresent(testCases::add);
        }
        
        return testCases;
    }
    
    /**
     * Update execution info in repository and active executions cache
     * 
     * @param executionInfo Execution info to update
     */
    private void updateExecutionInfo(TestExecutionInfo executionInfo) {
        // Update in repository
        TestExecution entity = convertToEntity(executionInfo);
        executionRepository.update(executionInfo.getProjectId(), executionInfo.getId(), entity);
        
        // Update in active executions
        activeExecutions.put(executionInfo.getId(), executionInfo);
    }
    
    /**
     * Generate random test result for simulation purposes
     * 
     * @return Random test result
     */
    private TestStatus generateRandomTestResult() {
        double rand = Math.random();
        
        if (rand < 0.7) {
            return TestStatus.PASSED;
        } else if (rand < 0.85) {
            return TestStatus.FAILED;
        } else if (rand < 0.95) {
            return TestStatus.SKIPPED;
        } else {
            return TestStatus.ERROR;
        }
    }
} 