package com.cstestforge.testing.service;

import com.cstestforge.testing.model.ApiRequest;
import com.cstestforge.testing.model.ApiResponse;

import java.util.List;
import java.util.Map;

/**
 * Service interface for API testing functionality
 */
public interface ApiTestingService {
    
    /**
     * Get all API requests, optionally filtered by project
     * 
     * @param projectId Optional project ID to filter by
     * @return List of API requests
     */
    List<ApiRequest> getAllRequests(String projectId);
    
    /**
     * Get an API request by ID
     * 
     * @param id API request ID
     * @return API request
     * @throws IllegalArgumentException if the request is not found
     */
    ApiRequest getRequestById(String id);
    
    /**
     * Create a new API request
     * 
     * @param request API request to create
     * @return Created API request with assigned ID
     */
    ApiRequest createRequest(ApiRequest request);
    
    /**
     * Update an existing API request
     * 
     * @param request API request to update
     * @return Updated API request
     * @throws IllegalArgumentException if the request is not found
     */
    ApiRequest updateRequest(ApiRequest request);
    
    /**
     * Delete an API request
     * 
     * @param id API request ID
     * @return True if deleted, false if not found
     */
    boolean deleteRequest(String id);
    
    /**
     * Execute an API request
     * 
     * @param request API request to execute
     * @return API response
     */
    ApiResponse executeRequest(ApiRequest request);
    
    /**
     * Execute an existing API request by ID
     * 
     * @param id API request ID
     * @return API response
     * @throws IllegalArgumentException if the request is not found
     */
    ApiResponse executeRequestById(String id);
    
    /**
     * Execute an API request with custom variables
     * 
     * @param request API request to execute
     * @param variables Map of variable names to values to override or add
     * @return API response
     */
    ApiResponse executeRequestWithVariables(ApiRequest request, Map<String, String> variables);
    
    /**
     * Validate response against assertions
     * 
     * @param response API response to validate
     * @param request API request containing assertions
     * @return API response with assertion results added
     */
    ApiResponse validateResponse(ApiResponse response, ApiRequest request);
    
    /**
     * Get execution history for an API request
     * 
     * @param requestId API request ID
     * @param limit Maximum number of executions to return
     * @return List of API responses
     */
    List<ApiResponse> getRequestExecutionHistory(String requestId, int limit);
} 