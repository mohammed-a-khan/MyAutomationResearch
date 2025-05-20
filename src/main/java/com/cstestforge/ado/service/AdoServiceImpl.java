package com.cstestforge.ado.service;

import com.cstestforge.ado.client.AdoClient;
import com.cstestforge.ado.model.*;
import com.cstestforge.ado.repository.AdoConnectionRepository;
import com.cstestforge.ado.repository.PipelineConfigRepository;
import com.cstestforge.ado.repository.SyncConfigRepository;
import com.cstestforge.storage.StorageManager;
import com.cstestforge.testing.model.ApiRequest;
import com.cstestforge.testing.model.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cstestforge.storage.repository.TestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementation of the AdoService interface
 */
@Service
public class AdoServiceImpl implements AdoService {

    private static final Logger logger = LoggerFactory.getLogger(AdoServiceImpl.class);
    private static final ExecutorService syncExecutor = Executors.newSingleThreadExecutor();
    
    private final AdoConnectionRepository connectionRepository;
    private final SyncConfigRepository syncConfigRepository;
    private final PipelineConfigRepository pipelineConfigRepository;
    private final AdoClient adoClient;
    private final StorageManager storageManager;
    private final ObjectMapper objectMapper;
    private final TestRepository testRepository;
    
    // Map to track running sync operations
    private final Map<String, CompletableFuture<Void>> runningSyncs = new HashMap<>();
    
    @Autowired
    public AdoServiceImpl(
            AdoConnectionRepository connectionRepository,
            SyncConfigRepository syncConfigRepository,
            PipelineConfigRepository pipelineConfigRepository,
            AdoClient adoClient,
            StorageManager storageManager,
            ObjectMapper objectMapper,
            TestRepository testRepository) {
        this.connectionRepository = connectionRepository;
        this.syncConfigRepository = syncConfigRepository;
        this.pipelineConfigRepository = pipelineConfigRepository;
        this.adoClient = adoClient;
        this.storageManager = storageManager;
        this.objectMapper = objectMapper;
        this.testRepository = testRepository;
    }

    @Override
    public List<AdoConnection> getConnections() {
        return connectionRepository.findAll();
    }

    @Override
    public AdoConnection getConnectionById(String id) {
        return connectionRepository.findById(id)
                .orElse(null);
    }

    @Override
    public AdoConnection createConnection(AdoConnection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        
        // Validate connection by testing it
        if (!validateConnection(connection.getUrl(), connection.getPat(), connection.getOrganizationName(), connection.getProjectName())) {
            throw new IllegalArgumentException("Invalid connection details. Failed to connect to Azure DevOps.");
        }
        
        return connectionRepository.save(connection);
    }

    @Override
    public AdoConnection updateConnection(String id, AdoConnection connection) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Connection ID cannot be null or empty");
        }
        
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        
        // Check if connection exists
        if (!connectionRepository.exists(id)) {
            throw new IllegalArgumentException("Connection not found with ID: " + id);
        }
        
        // Validate connection credentials if they've changed
        AdoConnection existing = getConnectionById(id);
        boolean credentialsChanged = !Objects.equals(existing.getUrl(), connection.getUrl()) || 
                                    !Objects.equals(existing.getPat(), connection.getPat()) || 
                                    !Objects.equals(existing.getOrganizationName(), connection.getOrganizationName()) || 
                                    !Objects.equals(existing.getProjectName(), connection.getProjectName());
        
        if (credentialsChanged && 
            !validateConnection(connection.getUrl(), connection.getPat(), connection.getOrganizationName(), connection.getProjectName())) {
            throw new IllegalArgumentException("Invalid connection details. Failed to connect to Azure DevOps.");
        }
        
        return connectionRepository.update(id, connection);
    }

    @Override
    public boolean deleteConnection(String id) {
        // First check if this connection is being used in any configurations
        List<SyncConfig> syncConfigs = syncConfigRepository.findAllConfigs();
        for (SyncConfig config : syncConfigs) {
            if (Objects.equals(config.getConnectionId(), id)) {
                // Delete the sync configuration
                syncConfigRepository.deleteConfig(config.getProjectId());
            }
        }
        
        List<PipelineConfig> pipelineConfigs = pipelineConfigRepository.findAll();
        for (PipelineConfig config : pipelineConfigs) {
            if (Objects.equals(config.getConnectionId(), id)) {
                // Delete the pipeline configuration
                pipelineConfigRepository.delete(config.getProjectId());
            }
        }
        
        return connectionRepository.delete(id);
    }

    @Override
    public boolean validateConnection(String url, String pat, String organizationName, String projectName) {
        AdoConnection connection = new AdoConnection();
        connection.setUrl(url);
        connection.setPat(pat);
        connection.setOrganizationName(organizationName);
        connection.setProjectName(projectName);
        
        return adoClient.testConnection(connection);
    }

    @Override
    public List<AdoProject> getProjects(String connectionId) {
        AdoConnection connection = getConnectionById(connectionId);
        if (connection == null) {
            throw new IllegalArgumentException("Connection not found with ID: " + connectionId);
        }
        
        return adoClient.getProjects(connection);
    }

    @Override
    public List<AdoTestPlan> getTestPlans(String connectionId, String projectId) {
        AdoConnection connection = getConnectionById(connectionId);
        if (connection == null) {
            throw new IllegalArgumentException("Connection not found with ID: " + connectionId);
        }
        
        return adoClient.getTestPlans(connection, projectId);
    }

    @Override
    public List<AdoTestSuite> getTestSuites(String connectionId, String projectId, String testPlanId) {
        AdoConnection connection = getConnectionById(connectionId);
        if (connection == null) {
            throw new IllegalArgumentException("Connection not found with ID: " + connectionId);
        }
        
        return adoClient.getTestSuites(connection, projectId, testPlanId);
    }

    @Override
    public List<AdoPipeline> getPipelines(String connectionId, String projectId) {
        AdoConnection connection = getConnectionById(connectionId);
        if (connection == null) {
            throw new IllegalArgumentException("Connection not found with ID: " + connectionId);
        }
        
        return adoClient.getPipelines(connection, projectId);
    }

    @Override
    public SyncConfig getSyncConfig(String projectId) {
        return syncConfigRepository.findConfigByProjectId(projectId)
                .orElse(null);
    }

    @Override
    public SyncConfig saveSyncConfig(String projectId, SyncConfig config) {
        if (projectId == null || projectId.isEmpty()) {
            throw new IllegalArgumentException("Project ID cannot be null or empty");
        }
        
        if (config == null) {
            throw new IllegalArgumentException("Sync configuration cannot be null");
        }
        
        // Validate the connection ID if set
        String connectionId = config.getConnectionId();
        if (connectionId != null && !connectionId.isEmpty() && !connectionRepository.exists(connectionId)) {
            throw new IllegalArgumentException("Connection not found with ID: " + connectionId);
        }
        
        return syncConfigRepository.saveConfig(projectId, config);
    }

    @Override
    public SyncStatus getSyncStatus(String projectId) {
        SyncStatus status = syncConfigRepository.findStatusByProjectId(projectId).orElse(null);
        
        // If no status exists yet, create a default one
        if (status == null) {
            status = new SyncStatus(projectId, "not-started");
            syncConfigRepository.saveStatus(projectId, status);
        }
        
        return status;
    }

    @Override
    public boolean startSync(String projectId) {
        // Check if a sync is already running for this project
        if (runningSyncs.containsKey(projectId) && !runningSyncs.get(projectId).isDone()) {
            logger.info("Sync already in progress for project: {}", projectId);
            return false;
        }
        
        // Get the sync configuration
        SyncConfig syncConfig = getSyncConfig(projectId);
        if (syncConfig == null) {
            logger.error("No sync configuration found for project: {}", projectId);
            return false;
        }
        
        // Get the connection
        String connectionId = syncConfig.getConnectionId();
        AdoConnection connection = getConnectionById(connectionId);
        if (connection == null) {
            logger.error("Connection not found for sync: {}", connectionId);
            return false;
        }
        
        // Update sync status to in-progress
        SyncStatus status = getSyncStatus(projectId);
        status.setLastSyncStatus("in-progress");
        status.setLastSyncTime(System.currentTimeMillis());
        status.setLastSyncMessage("Synchronization started");
        syncConfigRepository.saveStatus(projectId, status);
        
        // Start sync in background
        CompletableFuture<Void> syncFuture = CompletableFuture.runAsync(() -> {
            try {
                performSync(projectId, syncConfig, connection);
            } catch (Exception e) {
                logger.error("Error during synchronization for project: {}", projectId, e);
                
                // Update status to failed
                SyncStatus errorStatus = getSyncStatus(projectId);
                errorStatus.setLastSyncStatus("failed");
                errorStatus.setLastSyncMessage("Sync failed: " + e.getMessage());
                syncConfigRepository.saveStatus(projectId, errorStatus);
            } finally {
                // Remove from running syncs
                runningSyncs.remove(projectId);
            }
        }, syncExecutor);
        
        // Store the future
        runningSyncs.put(projectId, syncFuture);
        
        return true;
    }
    
    /**
     * Perform the actual synchronization
     * 
     * @param projectId Project ID
     * @param syncConfig Sync configuration
     * @param connection ADO connection
     */
    private void performSync(String projectId, SyncConfig syncConfig, AdoConnection connection) {
        try {
            // Initialize sync status
            SyncStatus status = getSyncStatus(projectId);
            status.setProcessedItems(0);
            status.setTotalItems(0);
            syncConfigRepository.saveStatus(projectId, status);
            
            // Get ADO test plan and suite
            String testPlanId = syncConfig.getTestPlanId();
            String testSuiteId = syncConfig.getTestSuiteId();
            
            if (testPlanId == null || testPlanId.isEmpty() || testSuiteId == null || testSuiteId.isEmpty()) {
                throw new IllegalArgumentException("Test plan and test suite must be configured for synchronization");
            }
            
            logger.info("Starting synchronization for project {}, test plan {}, test suite {}", 
                       projectId, testPlanId, testSuiteId);
                       
            // Get ADO test cases from the test suite
            List<Map<String, Object>> adoTestCases = getAdoTestCases(connection, projectId, testPlanId, testSuiteId);
            
            if (adoTestCases.isEmpty()) {
                logger.info("No test cases found in ADO test suite");
                updateSyncStatusSuccess(status, syncConfig, 0);
                return;
            }
            
            // Get test cases from CSTestForge for this project
            List<Map<String, Object>> csTestForgeTests = getProjectTests(projectId);
            
            // Set total items for status tracking
            int totalOperations = adoTestCases.size();
            status.setTotalItems(totalOperations);
            syncConfigRepository.saveStatus(projectId, status);
            
            // Process ADO test cases and sync with CSTestForge tests
            int processed = 0;
            for (Map<String, Object> adoTestCase : adoTestCases) {
                try {
                    syncTestCase(projectId, adoTestCase, csTestForgeTests, syncConfig.isTwoWaySync());
                    processed++;
                    
                    // Update status
                    status.setProcessedItems(processed);
                    syncConfigRepository.saveStatus(projectId, status);
                } catch (Exception e) {
                    logger.error("Error syncing test case: {}", e.getMessage(), e);
                }
            }
            
            // Handle two-way sync from CSTestForge to ADO if configured
            if (syncConfig.isTwoWaySync()) {
                // Filter tests that are in CSTestForge but not in ADO
                List<Map<String, Object>> testsToCreateInAdo = findTestsToCreateInAdo(csTestForgeTests, adoTestCases, syncConfig.getMappingField());
                
                if (!testsToCreateInAdo.isEmpty()) {
                    // Update total items for status tracking
                    totalOperations += testsToCreateInAdo.size();
                    status.setTotalItems(totalOperations);
                    syncConfigRepository.saveStatus(projectId, status);
                    
                    // Create tests in ADO
                    for (Map<String, Object> test : testsToCreateInAdo) {
                        try {
                            createAdoTestCase(connection, projectId, testPlanId, testSuiteId, test);
                            processed++;
                            
                            // Update status
                            status.setProcessedItems(processed);
                            syncConfigRepository.saveStatus(projectId, status);
                        } catch (Exception e) {
                            logger.error("Error creating ADO test case: {}", e.getMessage(), e);
                        }
                    }
                }
            }
            
            // Update final status
            updateSyncStatusSuccess(status, syncConfig, processed);
            
        } catch (Exception e) {
            logger.error("Error during test case synchronization", e);
            throw new RuntimeException("Test case synchronization failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get ADO test cases from a test suite
     * 
     * @param connection ADO connection
     * @param projectId Project ID
     * @param testPlanId Test plan ID
     * @param testSuiteId Test suite ID
     * @return List of test cases
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getAdoTestCases(AdoConnection connection, String projectId, 
                                                     String testPlanId, String testSuiteId) {
        // Execute the request to fetch test cases
        ApiResponse response = adoClient.getTestCasesForSuite(connection, projectId, testPlanId, testSuiteId);
        
        if (!response.isSuccessful()) {
            logger.error("Failed to get ADO test cases: {}", response.getErrorMessage());
            return Collections.emptyList();
        }
        
        try {
            // Parse the response
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseData = objectMapper.readValue(response.getBody(), Map.class);
            List<Map<String, Object>> testCases = (List<Map<String, Object>>) responseData.get("value");
            
            // Fetch details for each test case
            List<Map<String, Object>> detailedTestCases = new ArrayList<>();
            for (Map<String, Object> testCase : testCases) {
                String testCaseId = testCase.get("testCase").toString().replaceAll("[^0-9]", "");
                Map<String, Object> details = getAdoTestCaseDetails(connection, projectId, testCaseId);
                if (!details.isEmpty()) {
                    detailedTestCases.add(details);
                }
            }
            
            return detailedTestCases;
        } catch (Exception e) {
            logger.error("Error parsing ADO test cases", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get detailed information for an ADO test case
     * 
     * @param connection ADO connection
     * @param projectId Project ID
     * @param testCaseId Test case ID
     * @return Test case details
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getAdoTestCaseDetails(AdoConnection connection, String projectId, String testCaseId) {
        // Execute the request to fetch test case details
        ApiResponse response = adoClient.getTestCaseDetails(connection, projectId, testCaseId);
        
        if (!response.isSuccessful()) {
            logger.error("Failed to get ADO test case details: {}", response.getErrorMessage());
            return Collections.emptyMap();
        }
        
        try {
            // Parse the response
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> workItem = objectMapper.readValue(response.getBody(), Map.class);
            
            // Extract relevant fields
            Map<String, Object> fields = (Map<String, Object>) workItem.get("fields");
            Map<String, Object> testCase = new HashMap<>();
            
            testCase.put("id", workItem.get("id"));
            testCase.put("title", fields.get("System.Title"));
            testCase.put("description", fields.getOrDefault("System.Description", ""));
            testCase.put("state", fields.get("System.State"));
            testCase.put("priority", fields.getOrDefault("Microsoft.VSTS.Common.Priority", ""));
            testCase.put("steps", parseSteps(fields.getOrDefault("Microsoft.VSTS.TCM.Steps", "")));
            
            return testCase;
        } catch (Exception e) {
            logger.error("Error parsing ADO test case details", e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Parse test case steps from ADO XML format
     * 
     * @param stepsXml Steps in XML format
     * @return Parsed steps
     */
    private List<Map<String, String>> parseSteps(Object stepsXml) {
        List<Map<String, String>> steps = new ArrayList<>();
        
        if (stepsXml == null || !(stepsXml instanceof String) || ((String)stepsXml).isEmpty()) {
            return steps;
        }
        
        String xml = (String) stepsXml;
        
        try {
            // Simple parsing of step elements using regex
            // In a production environment, use a proper XML parser
            java.util.regex.Pattern stepPattern = java.util.regex.Pattern.compile(
                "<step id=\"[^\"]*\"[^>]*>\\s*<parameterizedString>([^<]*)</parameterizedString>\\s*<description>([^<]*)</description>\\s*<expectedResult>([^<]*)</expectedResult>\\s*</step>"
            );
            java.util.regex.Matcher matcher = stepPattern.matcher(xml);
            
            while (matcher.find()) {
                Map<String, String> step = new HashMap<>();
                step.put("action", matcher.group(1).trim());
                step.put("description", matcher.group(2).trim());
                step.put("expectedResult", matcher.group(3).trim());
                steps.add(step);
            }
        } catch (Exception e) {
            logger.error("Error parsing test case steps", e);
        }
        
        return steps;
    }
    
    /**
     * Get test cases from CSTestForge for a project
     * 
     * @param projectId Project ID
     * @return List of tests
     */
    private List<Map<String, Object>> getProjectTests(String projectId) {
        // Implementation using TestRepository to fetch tests from the project's storage
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            // Get all tests for the project using TestRepository
            List<com.cstestforge.project.model.test.Test> tests = testRepository.findAll(projectId);
            
            // Convert Test objects to Map format
            for (com.cstestforge.project.model.test.Test test : tests) {
                Map<String, Object> testMap = new HashMap<>();
                testMap.put("id", test.getId());
                testMap.put("name", test.getName());
                testMap.put("description", test.getDescription());
                
                // Convert test steps to the expected format
                List<Map<String, Object>> steps = new ArrayList<>();
                if (test.getSteps() != null) {
                    for (com.cstestforge.project.model.test.TestStep step : test.getSteps()) {
                        Map<String, Object> stepMap = new HashMap<>();
                        stepMap.put("id", step.getId());
                        stepMap.put("order", step.getOrder());
                        stepMap.put("action", step.getName()); // Use name as action
                        stepMap.put("description", step.getDescription());
                        // Get expected result from parameters or metadata if available
                        String expectedResult = "";
                        if (step.getParameters() != null && step.getParameters().containsKey("expectedResult")) {
                            expectedResult = String.valueOf(step.getParameters().get("expectedResult"));
                        } else if (step.getMetadata() != null && step.getMetadata().containsKey("expectedResult")) {
                            expectedResult = String.valueOf(step.getMetadata().get("expectedResult"));
                        }
                        stepMap.put("expectedResult", expectedResult);
                        steps.add(stepMap);
                    }
                }
                testMap.put("steps", steps);
                
                // Create metadata map if not exists
                Map<String, Object> metadata = new HashMap<>();
                
                // Add metadata from test config if available
                if (test.getConfig() != null && test.getConfig().getFrameworkOptions() != null) {
                    Map<String, Object> frameworkOptions = test.getConfig().getFrameworkOptions();
                    if (frameworkOptions.containsKey("adoReference")) {
                        metadata.put("adoReference", frameworkOptions.get("adoReference"));
                    }
                    if (frameworkOptions.containsKey("adoState")) {
                        metadata.put("adoState", frameworkOptions.get("adoState"));
                    }
                }
                
                testMap.put("metadata", metadata);
                
                result.add(testMap);
            }
        } catch (Exception e) {
            logger.error("Error fetching tests for project: {}", projectId, e);
        }
        
        return result;
    }
    
    /**
     * Synchronize a test case between ADO and CSTestForge
     * 
     * @param projectId Project ID
     * @param adoTestCase ADO test case
     * @param csTestForgeTests CSTestForge tests
     * @param twoWaySync Whether two-way sync is enabled
     */
    private void syncTestCase(String projectId, Map<String, Object> adoTestCase, 
                              List<Map<String, Object>> csTestForgeTests, boolean twoWaySync) {
        String adoTestCaseId = adoTestCase.get("id").toString();
        String adoTestCaseTitle = (String) adoTestCase.get("title");
        
        // Try to find matching test in CSTestForge
        Map<String, Object> matchingTest = findMatchingTest(csTestForgeTests, adoTestCaseId);
        
        if (matchingTest == null) {
            // Test doesn't exist in CSTestForge, create it
            createTestFromAdo(projectId, adoTestCase);
        } else if (twoWaySync) {
            // Test exists, update it if needed based on which one is newer
            // In a real implementation, compare timestamps and update accordingly
            updateTestFromAdo(projectId, matchingTest, adoTestCase);
        }
    }
    
    /**
     * Find a matching test in CSTestForge based on ADO reference
     * 
     * @param csTestForgeTests CSTestForge tests
     * @param adoTestCaseId ADO test case ID
     * @return Matching test or null if not found
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> findMatchingTest(List<Map<String, Object>> csTestForgeTests, String adoTestCaseId) {
        for (Map<String, Object> test : csTestForgeTests) {
            Map<String, Object> metadata = (Map<String, Object>) test.getOrDefault("metadata", Collections.emptyMap());
            String adoReference = (String) metadata.getOrDefault("adoReference", "");
            
            if (adoReference.equals(adoTestCaseId)) {
                return test;
            }
        }
        
        return null;
    }
    
    /**
     * Create a new test in CSTestForge from an ADO test case
     * 
     * @param projectId Project ID
     * @param adoTestCase ADO test case
     * @return Created test
     */
    private Map<String, Object> createTestFromAdo(String projectId, Map<String, Object> adoTestCase) {
        try {
            // Create new Test object
            com.cstestforge.project.model.test.Test newTest = new com.cstestforge.project.model.test.Test();
            
            // Set test properties
            newTest.setId(UUID.randomUUID().toString());
            newTest.setProjectId(projectId);
            newTest.setName((String) adoTestCase.get("title"));
            newTest.setDescription((String) adoTestCase.get("description"));
            newTest.setType(com.cstestforge.project.model.test.TestType.UI); // Default to UI test
            newTest.setStatus(com.cstestforge.project.model.test.TestStatus.ACTIVE);
            
            // Set up config and metadata
            com.cstestforge.project.model.test.TestConfig config = new com.cstestforge.project.model.test.TestConfig();
            Map<String, Object> frameworkOptions = new HashMap<>();
            frameworkOptions.put("adoReference", adoTestCase.get("id").toString());
            frameworkOptions.put("adoState", adoTestCase.get("state"));
            frameworkOptions.put("adoPriority", adoTestCase.get("priority"));
            config.setFrameworkOptions(frameworkOptions);
            newTest.setConfig(config);
            
            // Convert ADO steps to CSTestForge steps
            @SuppressWarnings("unchecked")
            List<Map<String, String>> adoSteps = (List<Map<String, String>>) adoTestCase.getOrDefault("steps", Collections.emptyList());
            List<com.cstestforge.project.model.test.TestStep> steps = new ArrayList<>();
            
            int stepOrder = 1;
            for (Map<String, String> adoStep : adoSteps) {
                com.cstestforge.project.model.test.TestStep step = new com.cstestforge.project.model.test.TestStep();
                step.setId(UUID.randomUUID().toString());
                step.setOrder(stepOrder++);
                step.setName(adoStep.get("action"));
                step.setDescription(adoStep.get("description"));
                
                // Store expected result in parameters
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("expectedResult", adoStep.get("expectedResult"));
                step.setParameters(parameters);
                
                steps.add(step);
            }
            
            newTest.setSteps(steps);
            
            // Save test using repository
            com.cstestforge.project.model.test.Test savedTest = testRepository.create(projectId, newTest);
            
            // Convert to map for return
            return convertTestToMap(savedTest);
        } catch (Exception e) {
            logger.error("Error creating test from ADO test case", e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Update a test in CSTestForge from an ADO test case
     * 
     * @param projectId Project ID
     * @param existingTest Existing test
     * @param adoTestCase ADO test case
     * @return Updated test
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> updateTestFromAdo(String projectId, Map<String, Object> existingTestMap, Map<String, Object> adoTestCase) {
        try {
            // Get the existing test ID
            String testId = (String) existingTestMap.get("id");
            
            // Fetch the actual Test object
            Optional<com.cstestforge.project.model.test.Test> testOpt = testRepository.findById(projectId, testId);
            if (!testOpt.isPresent()) {
                logger.error("Test not found for update: {}", testId);
                return Collections.emptyMap();
            }
            
            com.cstestforge.project.model.test.Test test = testOpt.get();
            
            // Update basic properties
            test.setName((String) adoTestCase.get("title"));
            test.setDescription((String) adoTestCase.get("description"));
            
            // Update ADO metadata in config
            com.cstestforge.project.model.test.TestConfig config = test.getConfig();
            if (config == null) {
                config = new com.cstestforge.project.model.test.TestConfig();
                test.setConfig(config);
            }
            
            Map<String, Object> frameworkOptions = config.getFrameworkOptions();
            if (frameworkOptions == null) {
                frameworkOptions = new HashMap<>();
                config.setFrameworkOptions(frameworkOptions);
            }
            
            frameworkOptions.put("adoState", adoTestCase.get("state"));
            frameworkOptions.put("adoPriority", adoTestCase.get("priority"));
            
            // Update steps
            List<Map<String, String>> adoSteps = (List<Map<String, String>>) adoTestCase.getOrDefault("steps", Collections.emptyList());
            List<com.cstestforge.project.model.test.TestStep> steps = new ArrayList<>();
            
            int stepOrder = 1;
            for (Map<String, String> adoStep : adoSteps) {
                com.cstestforge.project.model.test.TestStep step = new com.cstestforge.project.model.test.TestStep();
                step.setId(UUID.randomUUID().toString());
                step.setOrder(stepOrder++);
                step.setName(adoStep.get("action"));
                step.setDescription(adoStep.get("description"));
                
                // Store expected result in parameters
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("expectedResult", adoStep.get("expectedResult"));
                step.setParameters(parameters);
                
                steps.add(step);
            }
            
            test.setSteps(steps);
            
            // Save updated test
            com.cstestforge.project.model.test.Test updatedTest = testRepository.update(projectId, testId, test);
            
            // Convert to map for return
            return convertTestToMap(updatedTest);
        } catch (Exception e) {
            logger.error("Error updating test from ADO test case", e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Convert a Test object to a Map
     * 
     * @param test Test to convert
     * @return Map representation of the test
     */
    private Map<String, Object> convertTestToMap(com.cstestforge.project.model.test.Test test) {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("id", test.getId());
        testMap.put("name", test.getName());
        testMap.put("description", test.getDescription());
        
        // Convert steps
        List<Map<String, Object>> stepMaps = new ArrayList<>();
        if (test.getSteps() != null) {
            for (com.cstestforge.project.model.test.TestStep step : test.getSteps()) {
                Map<String, Object> stepMap = new HashMap<>();
                stepMap.put("id", step.getId());
                stepMap.put("order", step.getOrder());
                stepMap.put("action", step.getName());
                stepMap.put("description", step.getDescription());
                
                // Get expected result from parameters
                String expectedResult = "";
                if (step.getParameters() != null && step.getParameters().containsKey("expectedResult")) {
                    expectedResult = String.valueOf(step.getParameters().get("expectedResult"));
                }
                stepMap.put("expectedResult", expectedResult);
                
                stepMaps.add(stepMap);
            }
        }
        testMap.put("steps", stepMaps);
        
        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        if (test.getConfig() != null && test.getConfig().getFrameworkOptions() != null) {
            Map<String, Object> frameworkOptions = test.getConfig().getFrameworkOptions();
            if (frameworkOptions.containsKey("adoReference")) {
                metadata.put("adoReference", frameworkOptions.get("adoReference"));
            }
            if (frameworkOptions.containsKey("adoState")) {
                metadata.put("adoState", frameworkOptions.get("adoState"));
            }
        }
        testMap.put("metadata", metadata);
        
        return testMap;
    }
    
    /**
     * Find tests that need to be created in ADO
     * 
     * @param csTestForgeTests CSTestForge tests
     * @param adoTestCases ADO test cases
     * @param mappingField Field to use for mapping
     * @return Tests to create in ADO
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> findTestsToCreateInAdo(List<Map<String, Object>> csTestForgeTests, 
                                                           List<Map<String, Object>> adoTestCases, 
                                                           String mappingField) {
        List<Map<String, Object>> testsToCreate = new ArrayList<>();
        Set<String> adoTestCaseIds = new HashSet<>();
        
        // Collect all ADO test case IDs
        for (Map<String, Object> adoTestCase : adoTestCases) {
            adoTestCaseIds.add(adoTestCase.get("id").toString());
        }
        
        // Find CSTestForge tests that don't have a matching ADO reference
        for (Map<String, Object> test : csTestForgeTests) {
            Map<String, Object> metadata = (Map<String, Object>) test.getOrDefault("metadata", Collections.emptyMap());
            String adoReference = (String) metadata.getOrDefault("adoReference", "");
            
            // Skip tests that already have an ADO reference
            if (adoReference.isEmpty() || !adoTestCaseIds.contains(adoReference)) {
                testsToCreate.add(test);
            }
        }
        
        return testsToCreate;
    }
    
    /**
     * Create a test case in ADO from a CSTestForge test
     * 
     * @param connection ADO connection
     * @param projectId Project ID
     * @param testPlanId Test plan ID
     * @param testSuiteId Test suite ID
     * @param csTest CSTestForge test
     * @return ADO test case ID
     */
    private String createAdoTestCase(AdoConnection connection, String projectId, 
                                   String testPlanId, String testSuiteId, 
                                   Map<String, Object> csTest) {
        try {
            // Step 1: Create a work item for the test case
            String testCaseId = createAdoWorkItem(connection, projectId, csTest);
            
            if (testCaseId == null || testCaseId.isEmpty()) {
                return null;
            }
            
            // Step 2: Add the test case to the test suite
            addTestCaseToSuite(connection, projectId, testPlanId, testSuiteId, testCaseId);
            
            // Step 3: Update the CSTestForge test with the ADO reference
            updateTestWithAdoReference(csTest, projectId, testCaseId);
            
            return testCaseId;
        } catch (Exception e) {
            logger.error("Error creating ADO test case", e);
            return null;
        }
    }
    
    /**
     * Create a work item in ADO for a test case
     * 
     * @param connection ADO connection
     * @param projectId Project ID
     * @param csTest CSTestForge test
     * @return Work item ID
     */
    private String createAdoWorkItem(AdoConnection connection, String projectId, Map<String, Object> csTest) {
        try {
            // Create request body - JSON patch document
            List<Map<String, Object>> operations = new ArrayList<>();
            
            // Add title
            Map<String, Object> titleOp = new HashMap<>();
            titleOp.put("op", "add");
            titleOp.put("path", "/fields/System.Title");
            titleOp.put("value", csTest.get("name"));
            operations.add(titleOp);
            
            // Add description if available
            if (csTest.containsKey("description") && csTest.get("description") != null) {
                Map<String, Object> descOp = new HashMap<>();
                descOp.put("op", "add");
                descOp.put("path", "/fields/System.Description");
                descOp.put("value", csTest.get("description"));
                operations.add(descOp);
            }
            
            // Add priority if available
            if (csTest.containsKey("priority") && csTest.get("priority") != null) {
                Map<String, Object> priorityOp = new HashMap<>();
                priorityOp.put("op", "add");
                priorityOp.put("path", "/fields/Microsoft.VSTS.Common.Priority");
                priorityOp.put("value", mapCsTestForgePriorityToAdoPriority((String) csTest.get("priority")));
                operations.add(priorityOp);
            }
            
            // Add steps if available
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> steps = (List<Map<String, Object>>) csTest.getOrDefault("steps", Collections.emptyList());
            if (!steps.isEmpty()) {
                String stepsHtml = convertStepsToAdoFormat(steps);
                Map<String, Object> stepsOp = new HashMap<>();
                stepsOp.put("op", "add");
                stepsOp.put("path", "/fields/Microsoft.VSTS.TCM.Steps");
                stepsOp.put("value", stepsHtml);
                operations.add(stepsOp);
            }
            
            // Convert operations to JSON
            String requestBody = objectMapper.writeValueAsString(operations);
            
            // Execute the request
            ApiResponse response = adoClient.createTestCaseWorkItem(connection, projectId, requestBody);
            
            if (!response.isSuccessful()) {
                logger.error("Failed to create ADO work item: {}", response.getErrorMessage());
                return null;
            }
            
            // Parse the response to get the work item ID
            Map<String, Object> responseData = objectMapper.readValue(response.getBody(), Map.class);
            return responseData.get("id").toString();
        } catch (Exception e) {
            logger.error("Error creating ADO work item", e);
            return null;
        }
    }
    
    /**
     * Add a test case to a test suite in ADO
     * 
     * @param connection ADO connection
     * @param projectId Project ID
     * @param testPlanId Test plan ID
     * @param testSuiteId Test suite ID
     * @param testCaseId Test case ID
     * @return True if successful
     */
    private boolean addTestCaseToSuite(AdoConnection connection, String projectId, 
                                     String testPlanId, String testSuiteId, String testCaseId) {
        try {
            // Execute the request to add test case to suite
            ApiResponse response = adoClient.addTestCaseToSuite(connection, projectId, testPlanId, testSuiteId, testCaseId);
            
            if (!response.isSuccessful()) {
                logger.error("Failed to add test case to suite: {}", response.getErrorMessage());
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Error adding test case to suite", e);
            return false;
        }
    }
    
    /**
     * Update a CSTestForge test with ADO reference
     * 
     * @param csTest CSTestForge test
     * @param projectId Project ID
     * @param adoTestCaseId ADO test case ID
     * @return Updated test
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> updateTestWithAdoReference(Map<String, Object> csTest, String projectId, String adoTestCaseId) {
        // Update metadata with ADO reference
        Map<String, Object> metadata = (Map<String, Object>) csTest.getOrDefault("metadata", new HashMap<>());
        metadata.put("adoReference", adoTestCaseId);
        csTest.put("metadata", metadata);
        csTest.put("updatedAt", System.currentTimeMillis());
        
        // Save test to storage
        String testId = (String) csTest.get("id");
        String testPath = "projects/" + projectId + "/tests/" + testId + "/test.json";
        storageManager.write(testPath, csTest);
        
        return csTest;
    }
    
    /**
     * Convert CSTestForge steps to ADO format
     * 
     * @param steps CSTestForge steps
     * @return Steps in ADO format
     */
    private String convertStepsToAdoFormat(List<Map<String, Object>> steps) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<steps id=\"0\" last=\"").append(steps.size()).append("\">\n");
        
        int stepNum = 1;
        for (Map<String, Object> step : steps) {
            String action = (String) step.getOrDefault("action", "");
            String description = (String) step.getOrDefault("description", "");
            String expectedResult = (String) step.getOrDefault("expectedResult", "");
            
            xml.append("  <step id=\"").append(stepNum).append("\" type=\"ActionStep\">\n");
            xml.append("    <parameterizedString>").append(escapeXml(action)).append("</parameterizedString>\n");
            xml.append("    <description>").append(escapeXml(description)).append("</description>\n");
            xml.append("    <expectedResult>").append(escapeXml(expectedResult)).append("</expectedResult>\n");
            xml.append("  </step>\n");
            
            stepNum++;
        }
        
        xml.append("</steps>");
        return xml.toString();
    }
    
    /**
     * Map CSTestForge priority to ADO priority
     * 
     * @param csTestForgePriority CSTestForge priority
     * @return ADO priority
     */
    private int mapCsTestForgePriorityToAdoPriority(String csTestForgePriority) {
        if (csTestForgePriority == null || csTestForgePriority.isEmpty()) {
            return 3; // Medium priority
        }
        
        switch (csTestForgePriority) {
            case "CRITICAL":
                return 1;
            case "HIGH":
                return 2;
            case "MEDIUM":
                return 3;
            case "LOW":
                return 4;
            default:
                return 3;
        }
    }
    
    /**
     * Escape XML special characters
     * 
     * @param input Input string
     * @return Escaped string
     */
    private String escapeXml(String input) {
        if (input == null) {
            return "";
        }
        
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
    
    /**
     * Update sync status after successful sync
     * 
     * @param status Sync status
     * @param syncConfig Sync configuration
     * @param processedItems Number of processed items
     */
    private void updateSyncStatusSuccess(SyncStatus status, SyncConfig syncConfig, int processedItems) {
        status.setLastSyncStatus("success");
        status.setLastSyncMessage("Synchronized " + processedItems + " test cases");
        status.setLastSyncTime(System.currentTimeMillis());
        status.setProcessedItems(processedItems);
        
        // Calculate next sync time if auto sync is enabled
        if (syncConfig.isAutoSync()) {
            status.setNextScheduledSync(System.currentTimeMillis() + syncConfig.getSyncInterval());
        } else {
            status.setNextScheduledSync(0);
        }
        
        syncConfigRepository.saveStatus(syncConfig.getProjectId(), status);
        
        // Update sync config with last sync time
        syncConfig.setLastSyncTimestamp(System.currentTimeMillis());
        syncConfigRepository.saveConfig(syncConfig.getProjectId(), syncConfig);
    }
    
    /**
     * Scheduled task to run automatic synchronizations
     */
    @Scheduled(fixedDelay = 60000) // Every minute
    public void scheduledSync() {
        // Find all sync configurations with auto sync enabled
        List<SyncConfig> configs = syncConfigRepository.findAllConfigs();
        long now = System.currentTimeMillis();
        
        for (SyncConfig config : configs) {
            if (config.isAutoSync() && 
                now - config.getLastSyncTimestamp() >= config.getSyncInterval()) {
                
                // Check if sync is already running
                String projectId = config.getProjectId();
                if (!runningSyncs.containsKey(projectId) || runningSyncs.get(projectId).isDone()) {
                    logger.info("Starting scheduled sync for project: {}", projectId);
                    startSync(projectId);
                }
            }
        }
    }

    @Override
    public PipelineConfig getPipelineConfig(String projectId) {
        return pipelineConfigRepository.findByProjectId(projectId)
                .orElse(null);
    }

    @Override
    public PipelineConfig savePipelineConfig(String projectId, PipelineConfig config) {
        if (projectId == null || projectId.isEmpty()) {
            throw new IllegalArgumentException("Project ID cannot be null or empty");
        }
        
        if (config == null) {
            throw new IllegalArgumentException("Pipeline configuration cannot be null");
        }
        
        // Validate the connection ID if set
        String connectionId = config.getConnectionId();
        if (connectionId != null && !connectionId.isEmpty() && !connectionRepository.exists(connectionId)) {
            throw new IllegalArgumentException("Connection not found with ID: " + connectionId);
        }
        
        return pipelineConfigRepository.save(projectId, config);
    }

    @Override
    public boolean triggerPipeline(String projectId) {
        // Get the pipeline configuration
        PipelineConfig pipelineConfig = getPipelineConfig(projectId);
        if (pipelineConfig == null) {
            logger.error("No pipeline configuration found for project: {}", projectId);
            return false;
        }
        
        // Get the connection
        String connectionId = pipelineConfig.getConnectionId();
        AdoConnection connection = getConnectionById(connectionId);
        if (connection == null) {
            logger.error("Connection not found for pipeline: {}", connectionId);
            return false;
        }
        
        // Convert string array parameters to map
        Map<String, String> parameters = new HashMap<>();
        if (pipelineConfig.getParameters() != null) {
            for (String param : pipelineConfig.getParameters()) {
                String[] parts = param.split("=", 2);
                if (parts.length == 2) {
                    parameters.put(parts[0], parts[1]);
                }
            }
        }
        
        // Add test inclusion parameter if configured
        if (pipelineConfig.isIncludeTests()) {
            parameters.put("includeTests", "true");
        }
        
        // Trigger the pipeline
        return adoClient.triggerPipeline(connection, 
                                         connection.getProjectName(), 
                                         pipelineConfig.getPipelineId(), 
                                         parameters);
    }
} 