package com.cstestforge.testing.service;

import com.cstestforge.testing.model.ApiAssertion;
import com.cstestforge.testing.model.ApiRequest;
import com.cstestforge.testing.model.ApiResponse;
import com.cstestforge.testing.model.ApiVariable;
import com.cstestforge.testing.repository.ApiRequestRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the API testing service
 */
@Service
public class ApiTestingServiceImpl implements ApiTestingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiTestingServiceImpl.class);
    private static final int DEFAULT_HISTORY_LIMIT = 20;
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    
    private final ApiRequestRepository repository;
    private HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public ApiTestingServiceImpl(ApiRequestRepository repository) {
        this.repository = repository;
        this.httpClient = new HttpClient(); // Default HTTP client
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public List<ApiRequest> getAllRequests(String projectId) {
        if (projectId != null && !projectId.isEmpty()) {
            return repository.findByProjectId(projectId);
        } else {
            return repository.findAll();
        }
    }
    
    @Override
    public ApiRequest getRequestById(String id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("API request not found with ID: " + id));
    }
    
    @Override
    public ApiRequest createRequest(ApiRequest request) {
        return repository.save(request);
    }
    
    @Override
    public ApiRequest updateRequest(ApiRequest request) {
        return repository.update(request);
    }
    
    @Override
    public boolean deleteRequest(String id) {
        return repository.deleteById(id);
    }
    
    @Override
    public ApiResponse executeRequest(ApiRequest request) {
        try {
            // Process variables in the request
            ApiRequest processedRequest = processVariables(request, null);
            
            // Execute the request
            ApiResponse response = httpClient.execute(processedRequest);
            
            // Validate the response against assertions
            ApiResponse validatedResponse = validateResponse(response, request);
            
            // Save the response
            repository.saveResponse(validatedResponse);
            
            return validatedResponse;
        } catch (Exception e) {
            logger.error("Error executing API request", e);
            ApiResponse errorResponse = new ApiResponse();
            errorResponse.setRequestId(request.getId());
            errorResponse.setSuccessful(false);
            errorResponse.setErrorMessage("Failed to execute request: " + e.getMessage());
            return errorResponse;
        }
    }
    
    @Override
    public ApiResponse executeRequestById(String id) {
        ApiRequest request = getRequestById(id);
        return executeRequest(request);
    }
    
    @Override
    public ApiResponse executeRequestWithVariables(ApiRequest request, Map<String, String> variables) {
        try {
            // Process variables in the request
            ApiRequest processedRequest = processVariables(request, variables);
            
            // Execute the request
            ApiResponse response = httpClient.execute(processedRequest);
            
            // Validate the response against assertions
            ApiResponse validatedResponse = validateResponse(response, request);
            
            // Save the response
            repository.saveResponse(validatedResponse);
            
            return validatedResponse;
        } catch (Exception e) {
            logger.error("Error executing API request with variables", e);
            ApiResponse errorResponse = new ApiResponse();
            errorResponse.setRequestId(request.getId());
            errorResponse.setSuccessful(false);
            errorResponse.setErrorMessage("Failed to execute request: " + e.getMessage());
            return errorResponse;
        }
    }
    
    @Override
    public ApiResponse validateResponse(ApiResponse response, ApiRequest request) {
        if (request.getAssertions() == null || request.getAssertions().isEmpty()) {
            return response;
        }
        
        for (ApiAssertion assertion : request.getAssertions()) {
            boolean passed = evaluateAssertion(response, assertion);
            response.addAssertionResult(assertion.getId(), passed);
        }
        
        return response;
    }
    
    @Override
    public List<ApiResponse> getRequestExecutionHistory(String requestId, int limit) {
        int actualLimit = limit > 0 ? limit : DEFAULT_HISTORY_LIMIT;
        return repository.getResponseHistory(requestId, actualLimit);
    }
    
    /**
     * Process variables in an API request
     * 
     * @param request Original request
     * @param overrideVariables Optional map of variable names to values to override
     * @return Processed request with variables replaced
     */
    private ApiRequest processVariables(ApiRequest request, Map<String, String> overrideVariables) {
        // Create a deep copy of the request
        ApiRequest processedRequest = new ApiRequest();
        processedRequest.setId(request.getId());
        processedRequest.setProjectId(request.getProjectId());
        processedRequest.setName(request.getName());
        processedRequest.setDescription(request.getDescription());
        processedRequest.setMethod(request.getMethod());
        processedRequest.setBodyType(request.getBodyType());
        processedRequest.setCreatedAt(request.getCreatedAt());
        processedRequest.setUpdatedAt(request.getUpdatedAt());
        
        // Create a variable context
        Map<String, String> variableContext = new HashMap<>();
        
        // Add default variables from the request
        if (request.getVariables() != null) {
            for (ApiVariable variable : request.getVariables()) {
                if (variable.isEnabled()) {
                    variableContext.put(variable.getName(), variable.getValue());
                }
            }
        }
        
        // Override with custom variables
        if (overrideVariables != null) {
            variableContext.putAll(overrideVariables);
        }
        
        // Process URL
        processedRequest.setUrl(replaceVariables(request.getUrl(), variableContext));
        
        // Process headers
        Map<String, String> processedHeaders = new HashMap<>();
        if (request.getHeaders() != null) {
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                processedHeaders.put(
                    header.getKey(),
                    replaceVariables(header.getValue(), variableContext)
                );
            }
        }
        processedRequest.setHeaders(processedHeaders);
        
        // Process query parameters
        Map<String, String> processedQueryParams = new HashMap<>();
        if (request.getQueryParams() != null) {
            for (Map.Entry<String, String> param : request.getQueryParams().entrySet()) {
                processedQueryParams.put(
                    param.getKey(),
                    replaceVariables(param.getValue(), variableContext)
                );
            }
        }
        processedRequest.setQueryParams(processedQueryParams);
        
        // Process body if present
        if (request.getBody() != null) {
            processedRequest.setBody(replaceVariables(request.getBody(), variableContext));
        }
        
        // Copy assertions and variables without processing
        processedRequest.setAssertions(request.getAssertions());
        processedRequest.setVariables(request.getVariables());
        
        return processedRequest;
    }
    
    /**
     * Replace variables in a string
     * 
     * @param input Input string
     * @param variables Map of variable names to values
     * @return String with variables replaced
     */
    private String replaceVariables(String input, Map<String, String> variables) {
        if (input == null || input.isEmpty() || variables == null || variables.isEmpty()) {
            return input;
        }
        
        StringBuilder result = new StringBuilder(input);
        Matcher matcher = VARIABLE_PATTERN.matcher(input);
        
        // Find all variables and replace them
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String variableValue = variables.getOrDefault(variableName, "");
            
            // Replace the variable in the result
            int start = result.indexOf("{{" + variableName + "}}");
            if (start != -1) {
                result.replace(start, start + variableName.length() + 4, variableValue);
                
                // Update matcher to account for the replacement
                matcher = VARIABLE_PATTERN.matcher(result.toString());
            }
        }
        
        return result.toString();
    }
    
    /**
     * Evaluate an assertion against an API response
     * 
     * @param response API response
     * @param assertion Assertion to evaluate
     * @return True if the assertion passes, false otherwise
     */
    private boolean evaluateAssertion(ApiResponse response, ApiAssertion assertion) {
        try {
            String type = assertion.getType();
            String property = assertion.getProperty();
            String operator = assertion.getOperator();
            String expected = assertion.getExpected();
            
            switch (type.toLowerCase()) {
                case "status":
                    return evaluateStatusAssertion(response.getStatusCode(), operator, expected);
                    
                case "header":
                    return evaluateHeaderAssertion(response.getHeaders(), property, operator, expected);
                    
                case "responsetime":
                    return evaluateResponseTimeAssertion(response.getResponseTimeMs(), operator, expected);
                    
                case "body":
                    return evaluateBodyAssertion(response.getBody(), operator, expected);
                    
                case "jsonpath":
                    return evaluateJsonPathAssertion(response.getBody(), property, operator, expected);
                    
                default:
                    logger.warn("Unsupported assertion type: {}", type);
                    return false;
            }
        } catch (Exception e) {
            logger.error("Error evaluating assertion: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Evaluate a status code assertion
     */
    private boolean evaluateStatusAssertion(int statusCode, String operator, String expected) {
        int expectedStatus;
        try {
            expectedStatus = Integer.parseInt(expected);
        } catch (NumberFormatException e) {
            return false;
        }
        
        switch (operator.toLowerCase()) {
            case "=":
                return statusCode == expectedStatus;
            case "!=":
                return statusCode != expectedStatus;
            case ">":
                return statusCode > expectedStatus;
            case "<":
                return statusCode < expectedStatus;
            case ">=":
                return statusCode >= expectedStatus;
            case "<=":
                return statusCode <= expectedStatus;
            default:
                return false;
        }
    }
    
    /**
     * Evaluate a header assertion
     */
    private boolean evaluateHeaderAssertion(Map<String, String> headers, String headerName, String operator, String expected) {
        if (headers == null || !headers.containsKey(headerName)) {
            return "not_exists".equals(operator.toLowerCase());
        }
        
        String headerValue = headers.get(headerName);
        
        switch (operator.toLowerCase()) {
            case "exists":
                return true;
            case "not_exists":
                return false;
            case "=":
                return expected.equals(headerValue);
            case "!=":
                return !expected.equals(headerValue);
            case "contains":
                return headerValue != null && headerValue.contains(expected);
            case "not_contains":
                return headerValue == null || !headerValue.contains(expected);
            case "matches":
                return headerValue != null && headerValue.matches(expected);
            default:
                return false;
        }
    }
    
    /**
     * Evaluate a response time assertion
     */
    private boolean evaluateResponseTimeAssertion(long responseTimeMs, String operator, String expected) {
        long expectedTime;
        try {
            expectedTime = Long.parseLong(expected);
        } catch (NumberFormatException e) {
            return false;
        }
        
        switch (operator.toLowerCase()) {
            case "=":
                return responseTimeMs == expectedTime;
            case "!=":
                return responseTimeMs != expectedTime;
            case ">":
                return responseTimeMs > expectedTime;
            case "<":
                return responseTimeMs < expectedTime;
            case ">=":
                return responseTimeMs >= expectedTime;
            case "<=":
                return responseTimeMs <= expectedTime;
            default:
                return false;
        }
    }
    
    /**
     * Evaluate a body assertion
     */
    private boolean evaluateBodyAssertion(String body, String operator, String expected) {
        if (body == null) {
            body = "";
        }
        
        switch (operator.toLowerCase()) {
            case "=":
                return expected.equals(body);
            case "!=":
                return !expected.equals(body);
            case "contains":
                return body.contains(expected);
            case "not_contains":
                return !body.contains(expected);
            case "matches":
                return body.matches(expected);
            case "exists":
                return !body.isEmpty();
            case "not_exists":
                return body.isEmpty();
            default:
                return false;
        }
    }
    
    /**
     * Evaluate a JSON path assertion
     */
    private boolean evaluateJsonPathAssertion(String body, String path, String operator, String expected) {
        if (body == null || body.isEmpty()) {
            return "not_exists".equals(operator.toLowerCase());
        }
        
        try {
            JsonNode rootNode = objectMapper.readTree(body);
            
            // Simple path parsing - split by dots or brackets
            String[] pathComponents = path.split("\\.|\\[|\\]");
            JsonNode currentNode = rootNode;
            
            // Traverse the path
            for (String component : pathComponents) {
                if (component == null || component.isEmpty()) {
                    continue;
                }
                
                // Handle array indices
                if (component.matches("\\d+")) {
                    int index = Integer.parseInt(component);
                    if (!currentNode.isArray() || index >= currentNode.size()) {
                        return "not_exists".equals(operator.toLowerCase());
                    }
                    currentNode = currentNode.get(index);
                } else {
                    // Handle object properties
                    if (!currentNode.has(component)) {
                        return "not_exists".equals(operator.toLowerCase());
                    }
                    currentNode = currentNode.get(component);
                }
                
                if (currentNode == null) {
                    return "not_exists".equals(operator.toLowerCase());
                }
            }
            
            // At this point we have the node, evaluate based on operator
            switch (operator.toLowerCase()) {
                case "exists":
                    return true;
                case "not_exists":
                    return false;
                case "=":
                    if (currentNode.isNumber()) {
                        return expected.equals(currentNode.asText());
                    } else if (currentNode.isBoolean()) {
                        return Boolean.parseBoolean(expected) == currentNode.asBoolean();
                    } else {
                        return expected.equals(currentNode.asText());
                    }
                case "!=":
                    if (currentNode.isNumber()) {
                        return !expected.equals(currentNode.asText());
                    } else if (currentNode.isBoolean()) {
                        return Boolean.parseBoolean(expected) != currentNode.asBoolean();
                    } else {
                        return !expected.equals(currentNode.asText());
                    }
                case "contains":
                    return currentNode.asText().contains(expected);
                case "not_contains":
                    return !currentNode.asText().contains(expected);
                case ">":
                    if (currentNode.isNumber()) {
                        double nodeValue = currentNode.asDouble();
                        double expectedValue = Double.parseDouble(expected);
                        return nodeValue > expectedValue;
                    }
                    return false;
                case "<":
                    if (currentNode.isNumber()) {
                        double nodeValue = currentNode.asDouble();
                        double expectedValue = Double.parseDouble(expected);
                        return nodeValue < expectedValue;
                    }
                    return false;
                case ">=":
                    if (currentNode.isNumber()) {
                        double nodeValue = currentNode.asDouble();
                        double expectedValue = Double.parseDouble(expected);
                        return nodeValue >= expectedValue;
                    }
                    return false;
                case "<=":
                    if (currentNode.isNumber()) {
                        double nodeValue = currentNode.asDouble();
                        double expectedValue = Double.parseDouble(expected);
                        return nodeValue <= expectedValue;
                    }
                    return false;
                default:
                    return false;
            }
        } catch (Exception e) {
            logger.error("Error evaluating JSON path assertion: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Set a custom HTTP client with specific configuration
     * 
     * @param client Custom HTTP client
     */
    public void setHttpClient(HttpClient client) {
        if (client != null) {
            this.httpClient = client;
        }
    }
} 