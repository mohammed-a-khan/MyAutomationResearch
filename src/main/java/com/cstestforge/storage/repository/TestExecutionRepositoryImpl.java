package com.cstestforge.storage.repository;

import com.cstestforge.project.model.execution.TestExecution;
import com.cstestforge.project.model.execution.TestExecutionStatus;
import com.cstestforge.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implementation of TestExecutionRepository using StorageManager.
 * Test executions are stored at "projects/{projectId}/executions/"
 */
@Repository
public class TestExecutionRepositoryImpl implements TestExecutionRepository {

    private static final Logger logger = LoggerFactory.getLogger(TestExecutionRepositoryImpl.class);
    private static final String EXECUTIONS_DIRECTORY = "projects/%s/executions";
    private static final String EXECUTION_FILE = "projects/%s/executions/%s/execution.json";
    private static final String EXECUTION_INDEX_FILE = "projects/%s/executions/_index.json";
    private static final String TEST_EXECUTIONS_INDEX_FILE = "projects/%s/executions/by_test/_index.json";
    private static final String TEST_RUN_EXECUTIONS_INDEX_FILE = "projects/%s/executions/by_run/_index.json";

    private final StorageManager storageManager;

    @Autowired
    public TestExecutionRepositoryImpl(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    @Override
    public Optional<TestExecution> findById(String projectId, String executionId) {
        String executionPath = String.format(EXECUTION_FILE, projectId, executionId);
        if (!storageManager.exists(executionPath)) {
            return Optional.empty();
        }
        
        TestExecution execution = storageManager.read(executionPath, TestExecution.class);
        return Optional.ofNullable(execution);
    }

    @Override
    public List<TestExecution> findAll(String projectId) {
        // Get the execution index
        String indexPath = String.format(EXECUTION_INDEX_FILE, projectId);
        if (!storageManager.exists(indexPath)) {
            // Create empty index if it doesn't exist
            createEmptyIndexes(projectId);
            return Collections.emptyList();
        }
        
        // Read execution IDs from index
        Map<String, ExecutionIndexEntry> executionIndex = storageManager.read(indexPath, Map.class);
        if (executionIndex == null || executionIndex.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Load each execution
        List<TestExecution> executions = new ArrayList<>();
        for (String executionId : executionIndex.keySet()) {
            findById(projectId, executionId).ifPresent(executions::add);
        }
        
        // Sort by start time (most recent first)
        executions.sort((e1, e2) -> {
            if (e1.getStartTime() == null) return 1;
            if (e2.getStartTime() == null) return -1;
            return e2.getStartTime().compareTo(e1.getStartTime());
        });
        
        return executions;
    }

    @Override
    public List<TestExecution> findByFilters(String projectId, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return findAll(projectId);
        }
        
        // Get index to filter efficiently
        String indexPath = String.format(EXECUTION_INDEX_FILE, projectId);
        if (!storageManager.exists(indexPath)) {
            return Collections.emptyList();
        }
        
        Map<String, ExecutionIndexEntry> executionIndex = storageManager.read(indexPath, Map.class);
        if (executionIndex == null || executionIndex.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Filter executions by index metadata first when possible
        Set<String> filteredIds = new HashSet<>(executionIndex.keySet());
        
        // Apply test ID filter if present (using index)
        if (filters.containsKey("testId")) {
            String testId = filters.get("testId").toString();
            filteredIds = filterByTestId(projectId, testId, filteredIds);
            
            if (filteredIds.isEmpty()) {
                return Collections.emptyList();
            }
        }
        
        // Apply test run ID filter if present (using index)
        if (filters.containsKey("testRunId")) {
            String testRunId = filters.get("testRunId").toString();
            filteredIds = filterByTestRunId(projectId, testRunId, filteredIds);
            
            if (filteredIds.isEmpty()) {
                return Collections.emptyList();
            }
        }
        
        // Apply date range filters using index
        if (filters.containsKey("startDate") || filters.containsKey("endDate")) {
            LocalDateTime startDate = (LocalDateTime) filters.getOrDefault("startDate", LocalDateTime.MIN);
            LocalDateTime endDate = (LocalDateTime) filters.getOrDefault("endDate", LocalDateTime.MAX);
            
            filteredIds = filterByDateRange(executionIndex, filteredIds, startDate, endDate);
            
            if (filteredIds.isEmpty()) {
                return Collections.emptyList();
            }
        }
        
        // Load filtered executions
        List<TestExecution> executions = new ArrayList<>();
        for (String executionId : filteredIds) {
            findById(projectId, executionId).ifPresent(executions::add);
        }
        
        // Apply remaining filters that require the full object
        Predicate<TestExecution> predicate = execution -> true;
        
        if (filters.containsKey("status")) {
            TestExecutionStatus statusFilter = (TestExecutionStatus) filters.get("status");
            predicate = predicate.and(execution -> execution.getStatus() == statusFilter);
        }
        
        if (filters.containsKey("browser")) {
            String browserFilter = filters.get("browser").toString();
            predicate = predicate.and(execution -> 
                execution.getBrowser() != null && 
                execution.getBrowser().equalsIgnoreCase(browserFilter));
        }
        
        if (filters.containsKey("environment")) {
            String envFilter = filters.get("environment").toString();
            predicate = predicate.and(execution -> 
                execution.getEnvironment() != null && 
                execution.getEnvironment().equalsIgnoreCase(envFilter));
        }
        
        if (filters.containsKey("triggeredBy")) {
            String userFilter = filters.get("triggeredBy").toString();
            predicate = predicate.and(execution -> 
                execution.getTriggeredBy() != null && 
                execution.getTriggeredBy().equalsIgnoreCase(userFilter));
        }
        
        List<TestExecution> result = executions.stream()
                .filter(predicate)
                .collect(Collectors.toList());
        
        // Sort by start time (most recent first)
        result.sort((e1, e2) -> {
            if (e1.getStartTime() == null) return 1;
            if (e2.getStartTime() == null) return -1;
            return e2.getStartTime().compareTo(e1.getStartTime());
        });
        
        return result;
    }

    @Override
    public TestExecution create(String projectId, TestExecution execution) {
        // Ensure execution has an ID
        if (execution.getId() == null || execution.getId().isEmpty()) {
            execution.setId(UUID.randomUUID().toString());
        }
        
        // Set project ID
        execution.setProjectId(projectId);
        
        // Ensure directories exist
        String executionsDir = String.format(EXECUTIONS_DIRECTORY, projectId);
        String executionDir = executionsDir + "/" + execution.getId();
        storageManager.createDirectory(executionDir);
        
        // Save the execution
        String executionPath = String.format(EXECUTION_FILE, projectId, execution.getId());
        storageManager.write(executionPath, execution);
        
        // Update indexes
        updateIndexes(projectId, execution);
        
        return execution;
    }

    @Override
    public TestExecution update(String projectId, String executionId, TestExecution execution) {
        // Verify execution exists
        Optional<TestExecution> existingExecution = findById(projectId, executionId);
        if (!existingExecution.isPresent()) {
            throw new IllegalArgumentException("Execution not found with ID: " + executionId);
        }
        
        // Preserve the ID and project ID
        execution.setId(executionId);
        execution.setProjectId(projectId);
        
        // Save updated execution
        String executionPath = String.format(EXECUTION_FILE, projectId, executionId);
        storageManager.write(executionPath, execution);
        
        // Update indexes
        updateIndexes(projectId, execution);
        
        return execution;
    }

    @Override
    public boolean delete(String projectId, String executionId) {
        // Verify execution exists
        Optional<TestExecution> existingExecution = findById(projectId, executionId);
        if (!existingExecution.isPresent()) {
            return false; // Nothing to delete
        }
        
        TestExecution execution = existingExecution.get();
        
        // Delete execution directory and contents
        String executionDir = String.format(EXECUTIONS_DIRECTORY, projectId) + "/" + executionId;
        boolean deleted = storageManager.delete(executionDir);
        
        // If directory deleted successfully, update indexes
        if (deleted) {
            // Remove from indexes
            removeFromIndexes(projectId, execution);
        }
        
        return deleted;
    }

    @Override
    public List<TestExecution> findByTestId(String projectId, String testId) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("testId", testId);
        return findByFilters(projectId, filters);
    }

    @Override
    public List<TestExecution> findByDateRange(String projectId, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("startDate", startDate);
        filters.put("endDate", endDate);
        return findByFilters(projectId, filters);
    }

    @Override
    public List<TestExecution> findByTestRunId(String projectId, String testRunId) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("testRunId", testRunId);
        return findByFilters(projectId, filters);
    }

    @Override
    public Optional<TestExecution> findLatestByTestId(String projectId, String testId) {
        List<TestExecution> executions = findByTestId(projectId, testId);
        
        if (executions.isEmpty()) {
            return Optional.empty();
        }
        
        // Sort by start time (most recent first)
        executions.sort((e1, e2) -> {
            if (e1.getStartTime() == null) return 1;
            if (e2.getStartTime() == null) return -1;
            return e2.getStartTime().compareTo(e1.getStartTime());
        });
        
        return Optional.of(executions.get(0));
    }

    @Override
    public int count(String projectId) {
        String indexPath = String.format(EXECUTION_INDEX_FILE, projectId);
        if (!storageManager.exists(indexPath)) {
            return 0;
        }
        
        Map<String, ?> executionIndex = storageManager.read(indexPath, Map.class);
        return executionIndex != null ? executionIndex.size() : 0;
    }

    @Override
    public Map<String, Object> getStatistics(String projectId) {
        // Get all executions first
        List<TestExecution> executions = findAll(projectId);
        
        Map<String, Object> statistics = new HashMap<>();
        
        // Total count
        statistics.put("totalCount", executions.size());
        
        // Status distribution
        Map<TestExecutionStatus, Integer> statusCounts = new HashMap<>();
        for (TestExecutionStatus status : TestExecutionStatus.values()) {
            statusCounts.put(status, 0);
        }
        
        // Environment distribution
        Map<String, Integer> environmentCounts = new HashMap<>();
        
        // Browser distribution
        Map<String, Integer> browserCounts = new HashMap<>();
        
        // Success rate
        int passed = 0;
        int failed = 0;
        
        // Time metrics
        long totalDurationMs = 0;
        long maxDurationMs = 0;
        TestExecution slowestExecution = null;
        
        // Process all executions
        for (TestExecution execution : executions) {
            // Status counts
            TestExecutionStatus status = execution.getStatus();
            if (status != null) {
                statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
                
                if (status == TestExecutionStatus.PASSED) {
                    passed++;
                } else if (status == TestExecutionStatus.FAILED || status == TestExecutionStatus.ERROR) {
                    failed++;
                }
            }
            
            // Environment counts
            if (execution.getEnvironment() != null) {
                String env = execution.getEnvironment();
                environmentCounts.put(env, environmentCounts.getOrDefault(env, 0) + 1);
            }
            
            // Browser counts
            if (execution.getBrowser() != null) {
                String browser = execution.getBrowser();
                browserCounts.put(browser, browserCounts.getOrDefault(browser, 0) + 1);
            }
            
            // Duration metrics
            if (execution.getDuration() != null) {
                long durationMs = execution.getDuration().toMillis();
                totalDurationMs += durationMs;
                
                if (durationMs > maxDurationMs) {
                    maxDurationMs = durationMs;
                    slowestExecution = execution;
                }
            }
        }
        
        // Calculate success rate
        double successRate = (passed + failed > 0) ? (double) passed / (passed + failed) * 100 : 0;
        
        // Calculate average duration
        long averageDurationMs = executions.isEmpty() ? 0 : totalDurationMs / executions.size();
        
        // Add to statistics
        statistics.put("statusCounts", statusCounts);
        statistics.put("environmentCounts", environmentCounts);
        statistics.put("browserCounts", browserCounts);
        statistics.put("successRate", successRate);
        statistics.put("averageDurationMs", averageDurationMs);
        statistics.put("maxDurationMs", maxDurationMs);
        
        if (slowestExecution != null) {
            Map<String, Object> slowestExecutionInfo = new HashMap<>();
            slowestExecutionInfo.put("id", slowestExecution.getId());
            slowestExecutionInfo.put("testId", slowestExecution.getTestId());
            slowestExecutionInfo.put("name", slowestExecution.getName());
            slowestExecutionInfo.put("duration", slowestExecution.getDuration().toMillis());
            statistics.put("slowestExecution", slowestExecutionInfo);
        }
        
        return statistics;
    }

    @Override
    public int deleteOlderThan(String projectId, LocalDateTime cutoffDate) {
        // Get all executions
        String indexPath = String.format(EXECUTION_INDEX_FILE, projectId);
        if (!storageManager.exists(indexPath)) {
            return 0;
        }
        
        Map<String, ExecutionIndexEntry> executionIndex = storageManager.read(indexPath, Map.class);
        if (executionIndex == null || executionIndex.isEmpty()) {
            return 0;
        }
        
        // Find executions older than the cutoff date
        List<String> toDelete = new ArrayList<>();
        
        for (Map.Entry<String, ExecutionIndexEntry> entry : executionIndex.entrySet()) {
            ExecutionIndexEntry indexEntry = entry.getValue();
            if (indexEntry.startTime != null && indexEntry.startTime.isBefore(cutoffDate)) {
                toDelete.add(entry.getKey());
            }
        }
        
        // Delete each execution
        int deletedCount = 0;
        for (String executionId : toDelete) {
            if (delete(projectId, executionId)) {
                deletedCount++;
            }
        }
        
        return deletedCount;
    }

    /**
     * Create empty index files for a new project
     * 
     * @param projectId Project ID
     */
    private void createEmptyIndexes(String projectId) {
        String indexPath = String.format(EXECUTION_INDEX_FILE, projectId);
        String testIndexPath = String.format(TEST_EXECUTIONS_INDEX_FILE, projectId);
        String runIndexPath = String.format(TEST_RUN_EXECUTIONS_INDEX_FILE, projectId);
        
        if (!storageManager.exists(indexPath)) {
            storageManager.write(indexPath, new HashMap<String, ExecutionIndexEntry>());
        }
        
        if (!storageManager.exists(testIndexPath)) {
            storageManager.write(testIndexPath, new HashMap<String, Set<String>>());
        }
        
        if (!storageManager.exists(runIndexPath)) {
            storageManager.write(runIndexPath, new HashMap<String, Set<String>>());
        }
    }

    /**
     * Update index files when an execution is created or updated
     * 
     * @param projectId Project ID
     * @param execution Execution to index
     */
    private void updateIndexes(String projectId, TestExecution execution) {
        final String indexPath = String.format(EXECUTION_INDEX_FILE, projectId);
        final String testIndexPath = String.format(TEST_EXECUTIONS_INDEX_FILE, projectId);
        final String runIndexPath = String.format(TEST_RUN_EXECUTIONS_INDEX_FILE, projectId);
        
        List<String> pathsToLock = Arrays.asList(indexPath, testIndexPath, runIndexPath);
        
        storageManager.executeInTransaction(pathsToLock, unused -> {
            // Update main execution index
            Map<String, ExecutionIndexEntry> executionIndex = storageManager.exists(indexPath) 
                ? storageManager.read(indexPath, Map.class) 
                : new HashMap<>();
            
            if (executionIndex == null) {
                executionIndex = new HashMap<>();
            }
            
            // Create index entry
            ExecutionIndexEntry indexEntry = new ExecutionIndexEntry();
            indexEntry.name = execution.getName();
            indexEntry.testId = execution.getTestId();
            indexEntry.testRunId = execution.getTestRunId();
            indexEntry.status = execution.getStatus();
            indexEntry.startTime = execution.getStartTime();
            indexEntry.environment = execution.getEnvironment();
            indexEntry.browser = execution.getBrowser();
            
            executionIndex.put(execution.getId(), indexEntry);
            storageManager.write(indexPath, executionIndex);
            
            // Update test index if test ID is present
            if (execution.getTestId() != null) {
                updateTestIndex(testIndexPath, execution);
            }
            
            // Update run index if test run ID is present
            if (execution.getTestRunId() != null) {
                updateRunIndex(runIndexPath, execution);
            }
            
            return null;
        });
    }
    
    /**
     * Update the test-to-executions index
     * 
     * @param testIndexPath Path to the test index file
     * @param execution Execution to index
     */
    private void updateTestIndex(String testIndexPath, TestExecution execution) {
        Map<String, Set<String>> testIndex = storageManager.exists(testIndexPath)
            ? storageManager.read(testIndexPath, Map.class)
            : new HashMap<>();
        
        if (testIndex == null) {
            testIndex = new HashMap<>();
        }
        
        String testId = execution.getTestId();
        if (!testIndex.containsKey(testId)) {
            testIndex.put(testId, new HashSet<>());
        }
        
        testIndex.get(testId).add(execution.getId());
        storageManager.write(testIndexPath, testIndex);
    }
    
    /**
     * Update the run-to-executions index
     * 
     * @param runIndexPath Path to the run index file
     * @param execution Execution to index
     */
    private void updateRunIndex(String runIndexPath, TestExecution execution) {
        Map<String, Set<String>> runIndex = storageManager.exists(runIndexPath)
            ? storageManager.read(runIndexPath, Map.class)
            : new HashMap<>();
        
        if (runIndex == null) {
            runIndex = new HashMap<>();
        }
        
        String runId = execution.getTestRunId();
        if (!runIndex.containsKey(runId)) {
            runIndex.put(runId, new HashSet<>());
        }
        
        runIndex.get(runId).add(execution.getId());
        storageManager.write(runIndexPath, runIndex);
    }
    
    /**
     * Remove an execution from all indexes
     * 
     * @param projectId Project ID
     * @param execution Execution to remove
     */
    private void removeFromIndexes(String projectId, TestExecution execution) {
        final String indexPath = String.format(EXECUTION_INDEX_FILE, projectId);
        final String testIndexPath = String.format(TEST_EXECUTIONS_INDEX_FILE, projectId);
        final String runIndexPath = String.format(TEST_RUN_EXECUTIONS_INDEX_FILE, projectId);
        
        List<String> pathsToLock = Arrays.asList(indexPath, testIndexPath, runIndexPath);
        
        storageManager.executeInTransaction(pathsToLock, unused -> {
            // Remove from main execution index
            if (storageManager.exists(indexPath)) {
                Map<String, ExecutionIndexEntry> executionIndex = storageManager.read(indexPath, Map.class);
                if (executionIndex != null) {
                    executionIndex.remove(execution.getId());
                    storageManager.write(indexPath, executionIndex);
                }
            }
            
            // Remove from test index if test ID is present
            if (execution.getTestId() != null && storageManager.exists(testIndexPath)) {
                Map<String, Set<String>> testIndex = storageManager.read(testIndexPath, Map.class);
                if (testIndex != null) {
                    String testId = execution.getTestId();
                    if (testIndex.containsKey(testId)) {
                        testIndex.get(testId).remove(execution.getId());
                        
                        if (testIndex.get(testId).isEmpty()) {
                            testIndex.remove(testId);
                        }
                        
                        storageManager.write(testIndexPath, testIndex);
                    }
                }
            }
            
            // Remove from run index if test run ID is present
            if (execution.getTestRunId() != null && storageManager.exists(runIndexPath)) {
                Map<String, Set<String>> runIndex = storageManager.read(runIndexPath, Map.class);
                if (runIndex != null) {
                    String runId = execution.getTestRunId();
                    if (runIndex.containsKey(runId)) {
                        runIndex.get(runId).remove(execution.getId());
                        
                        if (runIndex.get(runId).isEmpty()) {
                            runIndex.remove(runId);
                        }
                        
                        storageManager.write(runIndexPath, runIndex);
                    }
                }
            }
            
            return null;
        });
    }
    
    /**
     * Filter execution IDs by test ID
     * 
     * @param projectId Project ID
     * @param testId Test ID
     * @param executionIds Set of execution IDs to filter
     * @return Filtered execution IDs
     */
    private Set<String> filterByTestId(String projectId, String testId, Set<String> executionIds) {
        String testIndexPath = String.format(TEST_EXECUTIONS_INDEX_FILE, projectId);
        if (!storageManager.exists(testIndexPath)) {
            return Collections.emptySet();
        }
        
        Map<String, Set<String>> testIndex = storageManager.read(testIndexPath, Map.class);
        if (testIndex == null || !testIndex.containsKey(testId)) {
            return Collections.emptySet();
        }
        
        Set<String> testExecutionIds = testIndex.get(testId);
        return executionIds.stream()
                .filter(testExecutionIds::contains)
                .collect(Collectors.toSet());
    }
    
    /**
     * Filter execution IDs by test run ID
     * 
     * @param projectId Project ID
     * @param testRunId Test run ID
     * @param executionIds Set of execution IDs to filter
     * @return Filtered execution IDs
     */
    private Set<String> filterByTestRunId(String projectId, String testRunId, Set<String> executionIds) {
        String runIndexPath = String.format(TEST_RUN_EXECUTIONS_INDEX_FILE, projectId);
        if (!storageManager.exists(runIndexPath)) {
            return Collections.emptySet();
        }
        
        Map<String, Set<String>> runIndex = storageManager.read(runIndexPath, Map.class);
        if (runIndex == null || !runIndex.containsKey(testRunId)) {
            return Collections.emptySet();
        }
        
        Set<String> runExecutionIds = runIndex.get(testRunId);
        return executionIds.stream()
                .filter(runExecutionIds::contains)
                .collect(Collectors.toSet());
    }
    
    /**
     * Filter execution IDs by date range
     * 
     * @param executionIndex Execution index
     * @param executionIds Set of execution IDs to filter
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Filtered execution IDs
     */
    private Set<String> filterByDateRange(Map<String, ExecutionIndexEntry> executionIndex, Set<String> executionIds, 
                                          LocalDateTime startDate, LocalDateTime endDate) {
        return executionIds.stream()
                .filter(id -> {
                    ExecutionIndexEntry entry = executionIndex.get(id);
                    if (entry == null || entry.startTime == null) {
                        return false;
                    }
                    return !entry.startTime.isBefore(startDate) && !entry.startTime.isAfter(endDate);
                })
                .collect(Collectors.toSet());
    }
    
    /**
     * Index entry for an execution
     */
    private static class ExecutionIndexEntry {
        public String name;
        public String testId;
        public String testRunId;
        public TestExecutionStatus status;
        public LocalDateTime startTime;
        public String environment;
        public String browser;
    }
} 