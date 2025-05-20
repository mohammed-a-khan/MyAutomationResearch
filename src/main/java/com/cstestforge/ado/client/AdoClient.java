package com.cstestforge.ado.client;

import com.cstestforge.ado.model.*;
import com.cstestforge.testing.model.ApiRequest;
import com.cstestforge.testing.model.ApiResponse;
import com.cstestforge.testing.service.HttpClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.util.*;
import java.util.Base64;

/**
 * Client for communicating with Azure DevOps REST API
 */
@Component
public class AdoClient {
    
    private static final Logger logger = LoggerFactory.getLogger(AdoClient.class);
    private static final String API_VERSION = "6.0"; // ADO API version
    
    private final ObjectMapper objectMapper;
    
    @Autowired
    public AdoClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Create an HTTP client with appropriate configuration
     * 
     * @param connection ADO connection with authentication details
     * @param skipCertValidation Whether to skip certificate validation
     * @return Configured HTTP client
     * @throws Exception If there's an error creating the client
     */
    private HttpClient createHttpClient(AdoConnection connection, boolean skipCertValidation) throws Exception {
        // Configure HTTP client with proxy if needed
        String proxyHost = System.getProperty("http.proxyHost");
        int proxyPort = -1;
        
        String proxyPortStr = System.getProperty("http.proxyPort");
        if (proxyPortStr != null && !proxyPortStr.isEmpty()) {
            try {
                proxyPort = Integer.parseInt(proxyPortStr);
            } catch (NumberFormatException e) {
                logger.warn("Invalid proxy port: {}", proxyPortStr);
            }
        }
        
        return new HttpClient(null, null, null, null, proxyHost, proxyPort, skipCertValidation, skipCertValidation, 30000);
    }
    
    /**
     * Create base URL for ADO API requests
     * 
     * @param connection ADO connection
     * @return Base URL for the API
     */
    public String createBaseUrl(AdoConnection connection) {
        return connection.getUrl() + "/" + connection.getOrganizationName() + "/";
    }
    
    /**
     * Add common query parameters to a request
     * 
     * @param request API request to modify
     * @return Modified request with API version parameter
     */
    private ApiRequest addCommonParams(ApiRequest request) {
        Map<String, String> queryParams = request.getQueryParams();
        if (queryParams == null) {
            queryParams = new HashMap<>();
            request.setQueryParams(queryParams);
        }
        
        // Add API version if not present
        if (!queryParams.containsKey("api-version")) {
            queryParams.put("api-version", API_VERSION);
        }
        
        return request;
    }
    
    /**
     * Add authorization header to a request
     * 
     * @param request API request to modify
     * @param connection ADO connection with authentication details
     * @return Modified request with authorization header
     */
    private ApiRequest addAuthHeader(ApiRequest request, AdoConnection connection) {
        Map<String, String> headers = request.getHeaders();
        if (headers == null) {
            headers = new HashMap<>();
            request.setHeaders(headers);
        }
        
        // Add Basic Auth header with PAT token
        String pat = connection.getPat();
        String auth = ":" + pat; // Username is empty when using PAT
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.put("Authorization", "Basic " + encodedAuth);
        
        // Add content type if not present and it's a request with a body
        if (!headers.containsKey("Content-Type") && 
            (request.getMethod().equalsIgnoreCase("POST") || 
             request.getMethod().equalsIgnoreCase("PUT") || 
             request.getMethod().equalsIgnoreCase("PATCH"))) {
            headers.put("Content-Type", "application/json");
        }
        
        return request;
    }
    
    /**
     * Execute an API request against ADO API
     * 
     * @param connection ADO connection with authentication details
     * @param request API request to execute
     * @return API response
     */
    public ApiResponse execute(AdoConnection connection, ApiRequest request) {
        try {
            // Create HTTP client
            HttpClient httpClient = createHttpClient(connection, false);
            
            // Add common parameters and headers
            request = addCommonParams(request);
            request = addAuthHeader(request, connection);
            
            // Execute the request
            ApiResponse response = httpClient.execute(request);
            
            if (!response.isSuccessful()) {
                logger.error("ADO API request failed: {} - {}", response.getStatusCode(), response.getErrorMessage());
            }
            
            return response;
        } catch (Exception e) {
            logger.error("Error executing ADO API request", e);
            
            ApiResponse errorResponse = new ApiResponse();
            errorResponse.setSuccessful(false);
            errorResponse.setErrorMessage("Failed to execute ADO API request: " + e.getMessage());
            
            return errorResponse;
        }
    }
    
    /**
     * Test an ADO connection
     * 
     * @param connection ADO connection to test
     * @return True if connection is valid
     */
    public boolean testConnection(AdoConnection connection) {
        try {
            // Create a request to fetch the project
            String url = createBaseUrl(connection) + "_apis/projects/" + connection.getProjectName();
            
            ApiRequest request = new ApiRequest();
            request.setUrl(url);
            request.setMethod("GET");
            
            // Execute the request
            ApiResponse response = execute(connection, request);
            
            return response.isSuccessful() && response.getStatusCode() == 200;
        } catch (Exception e) {
            logger.error("Error testing ADO connection", e);
            return false;
        }
    }
    
    /**
     * Get list of ADO projects for a connection
     * 
     * @param connection ADO connection
     * @return List of projects
     */
    public List<AdoProject> getProjects(AdoConnection connection) {
        try {
            // Create request to fetch projects
            String url = createBaseUrl(connection) + "_apis/projects";
            
            ApiRequest request = new ApiRequest();
            request.setUrl(url);
            request.setMethod("GET");
            
            // Execute the request
            ApiResponse response = execute(connection, request);
            
            if (!response.isSuccessful()) {
                return Collections.emptyList();
            }
            
            // Parse the response
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode values = root.path("value");
            
            List<AdoProject> projects = new ArrayList<>();
            
            if (values.isArray()) {
                for (JsonNode project : values) {
                    AdoProject adoProject = new AdoProject();
                    adoProject.setId(project.path("id").asText());
                    adoProject.setName(project.path("name").asText());
                    adoProject.setDescription(project.path("description").asText());
                    adoProject.setUrl(project.path("url").asText());
                    adoProject.setState(project.path("state").asText());
                    
                    projects.add(adoProject);
                }
            }
            
            return projects;
        } catch (Exception e) {
            logger.error("Error fetching ADO projects", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get list of test plans for a project
     * 
     * @param connection ADO connection
     * @param projectId Project ID
     * @return List of test plans
     */
    public List<AdoTestPlan> getTestPlans(AdoConnection connection, String projectId) {
        try {
            // Create request to fetch test plans
            String url = createBaseUrl(connection) + projectId + "/_apis/test/plans";
            
            ApiRequest request = new ApiRequest();
            request.setUrl(url);
            request.setMethod("GET");
            
            // Execute the request
            ApiResponse response = execute(connection, request);
            
            if (!response.isSuccessful()) {
                return Collections.emptyList();
            }
            
            // Parse the response
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode values = root.path("value");
            
            List<AdoTestPlan> testPlans = new ArrayList<>();
            
            if (values.isArray()) {
                for (JsonNode plan : values) {
                    AdoTestPlan testPlan = new AdoTestPlan();
                    testPlan.setId(plan.path("id").asText());
                    testPlan.setName(plan.path("name").asText());
                    testPlan.setProjectId(projectId);
                    
                    if (plan.has("description")) {
                        testPlan.setDescription(plan.path("description").asText());
                    }
                    
                    if (plan.has("url")) {
                        testPlan.setUrl(plan.path("url").asText());
                    }
                    
                    if (plan.has("areaPath")) {
                        testPlan.setAreaPath(plan.path("areaPath").asText());
                    }
                    
                    if (plan.has("iteration")) {
                        testPlan.setIteration(plan.path("iteration").asText());
                    }
                    
                    if (plan.has("rootSuite") && plan.path("rootSuite").has("id")) {
                        testPlan.setRootSuiteId(plan.path("rootSuite").path("id").asText());
                    }
                    
                    testPlans.add(testPlan);
                }
            }
            
            return testPlans;
        } catch (Exception e) {
            logger.error("Error fetching ADO test plans", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get list of test suites for a test plan
     * 
     * @param connection ADO connection
     * @param projectId Project ID
     * @param testPlanId Test plan ID
     * @return List of test suites
     */
    public List<AdoTestSuite> getTestSuites(AdoConnection connection, String projectId, String testPlanId) {
        try {
            // Create request to fetch test suites
            String url = createBaseUrl(connection) + projectId + "/_apis/test/plans/" + testPlanId + "/suites";
            
            ApiRequest request = new ApiRequest();
            request.setUrl(url);
            request.setMethod("GET");
            
            // Add query param to include child suites
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("includeChildSuites", "true");
            request.setQueryParams(queryParams);
            
            // Execute the request
            ApiResponse response = execute(connection, request);
            
            if (!response.isSuccessful()) {
                return Collections.emptyList();
            }
            
            // Parse the response
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode values = root.path("value");
            
            List<AdoTestSuite> testSuites = new ArrayList<>();
            
            if (values.isArray()) {
                for (JsonNode suite : values) {
                    AdoTestSuite testSuite = new AdoTestSuite();
                    testSuite.setId(suite.path("id").asText());
                    testSuite.setName(suite.path("name").asText());
                    testSuite.setTestPlanId(testPlanId);
                    testSuite.setProjectId(projectId);
                    
                    if (suite.has("description")) {
                        testSuite.setDescription(suite.path("description").asText());
                    }
                    
                    if (suite.has("url")) {
                        testSuite.setUrl(suite.path("url").asText());
                    }
                    
                    if (suite.has("parentSuite") && suite.path("parentSuite").has("id")) {
                        testSuite.setParentSuiteId(suite.path("parentSuite").path("id").asText());
                    }
                    
                    if (suite.has("isDefault")) {
                        testSuite.setDefault(suite.path("isDefault").asBoolean());
                    }
                    
                    if (suite.has("suiteType")) {
                        testSuite.setRequirementBased(suite.path("suiteType").asText().equals("requirementTestSuite"));
                    }
                    
                    testSuites.add(testSuite);
                }
            }
            
            return testSuites;
        } catch (Exception e) {
            logger.error("Error fetching ADO test suites", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get list of pipelines for a project
     * 
     * @param connection ADO connection
     * @param projectId Project ID
     * @return List of pipelines
     */
    public List<AdoPipeline> getPipelines(AdoConnection connection, String projectId) {
        try {
            // Create request to fetch pipelines
            String url = createBaseUrl(connection) + projectId + "/_apis/pipelines";
            
            ApiRequest request = new ApiRequest();
            request.setUrl(url);
            request.setMethod("GET");
            
            // Execute the request
            ApiResponse response = execute(connection, request);
            
            if (!response.isSuccessful()) {
                return Collections.emptyList();
            }
            
            // Parse the response
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode values = root.path("value");
            
            List<AdoPipeline> pipelines = new ArrayList<>();
            
            if (values.isArray()) {
                for (JsonNode pipeline : values) {
                    AdoPipeline adoPipeline = new AdoPipeline();
                    adoPipeline.setId(pipeline.path("id").asText());
                    adoPipeline.setName(pipeline.path("name").asText());
                    adoPipeline.setProjectId(projectId);
                    adoPipeline.setUrl(pipeline.path("url").asText());
                    
                    if (pipeline.has("revision")) {
                        adoPipeline.setRevision(pipeline.path("revision").asText());
                    }
                    
                    if (pipeline.has("folder")) {
                        adoPipeline.setFolderPath(pipeline.path("folder").asText());
                    }
                    
                    pipelines.add(adoPipeline);
                }
            }
            
            return pipelines;
        } catch (Exception e) {
            logger.error("Error fetching ADO pipelines", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Trigger a pipeline run
     * 
     * @param connection ADO connection
     * @param projectId Project ID
     * @param pipelineId Pipeline ID
     * @param parameters Pipeline parameters (optional)
     * @return True if pipeline was triggered successfully
     */
    public boolean triggerPipeline(AdoConnection connection, String projectId, String pipelineId, Map<String, String> parameters) {
        try {
            // Create request to trigger pipeline
            String url = createBaseUrl(connection) + projectId + "/_apis/pipelines/" + pipelineId + "/runs";
            
            ApiRequest request = new ApiRequest();
            request.setUrl(url);
            request.setMethod("POST");
            
            // Create request body
            Map<String, Object> body = new HashMap<>();
            
            if (parameters != null && !parameters.isEmpty()) {
                body.put("parameters", parameters);
            }
            
            request.setBody(objectMapper.writeValueAsString(body));
            
            // Execute the request
            ApiResponse response = execute(connection, request);
            
            return response.isSuccessful() && (response.getStatusCode() == 200 || response.getStatusCode() == 201);
        } catch (Exception e) {
            logger.error("Error triggering ADO pipeline", e);
            return false;
        }
    }
    
    /**
     * Get test cases from a test suite
     * 
     * @param connection ADO connection
     * @param projectId Project ID
     * @param testPlanId Test plan ID
     * @param testSuiteId Test suite ID
     * @return API response containing test cases
     */
    public ApiResponse getTestCasesForSuite(AdoConnection connection, String projectId, String testPlanId, String testSuiteId) {
        String url = createBaseUrl(connection) + projectId + "/_apis/test/Plans/" + testPlanId + 
                     "/Suites/" + testSuiteId + "/TestCases";
        
        ApiRequest request = new ApiRequest();
        request.setUrl(url);
        request.setMethod("GET");
        
        return execute(connection, request);
    }
    
    /**
     * Get details of a test case work item
     * 
     * @param connection ADO connection
     * @param projectId Project ID
     * @param testCaseId Test case ID
     * @return API response containing test case details
     */
    public ApiResponse getTestCaseDetails(AdoConnection connection, String projectId, String testCaseId) {
        String url = createBaseUrl(connection) + projectId + "/_apis/wit/workitems/" + testCaseId;
        
        ApiRequest request = new ApiRequest();
        request.setUrl(url);
        request.setMethod("GET");
        
        return execute(connection, request);
    }
    
    /**
     * Create a test case work item
     * 
     * @param connection ADO connection
     * @param projectId Project ID
     * @param requestBody JSON request body
     * @return API response from work item creation
     */
    public ApiResponse createTestCaseWorkItem(AdoConnection connection, String projectId, String requestBody) {
        String url = createBaseUrl(connection) + projectId + "/_apis/wit/workitems/$Test%20Case";
        
        ApiRequest request = new ApiRequest();
        request.setUrl(url);
        request.setMethod("POST");
        
        // Add headers
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json-patch+json");
        request.setHeaders(headers);
        
        request.setBody(requestBody);
        
        return execute(connection, request);
    }
    
    /**
     * Add a test case to a test suite
     * 
     * @param connection ADO connection
     * @param projectId Project ID
     * @param testPlanId Test plan ID
     * @param testSuiteId Test suite ID
     * @param testCaseId Test case ID
     * @return API response from adding test case to suite
     */
    public ApiResponse addTestCaseToSuite(AdoConnection connection, String projectId, String testPlanId, 
                                         String testSuiteId, String testCaseId) {
        String url = createBaseUrl(connection) + projectId + "/_apis/test/plans/" + testPlanId + 
                    "/suites/" + testSuiteId + "/testcases/" + testCaseId;
        
        ApiRequest request = new ApiRequest();
        request.setUrl(url);
        request.setMethod("POST");
        
        return execute(connection, request);
    }
} 