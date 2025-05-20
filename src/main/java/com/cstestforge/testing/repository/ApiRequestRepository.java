package com.cstestforge.testing.repository;

import com.cstestforge.testing.model.ApiRequest;
import com.cstestforge.testing.model.ApiResponse;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for API request operations
 */
public interface ApiRequestRepository {

    /**
     * Save an API request
     * 
     * @param request API request to save
     * @return Saved API request with assigned ID
     */
    ApiRequest save(ApiRequest request);
    
    /**
     * Update an existing API request
     * 
     * @param request API request to update
     * @return Updated API request
     */
    ApiRequest update(ApiRequest request);
    
    /**
     * Find an API request by ID
     * 
     * @param id API request ID
     * @return Optional containing the API request, or empty if not found
     */
    Optional<ApiRequest> findById(String id);
    
    /**
     * Find all API requests
     * 
     * @return List of all API requests
     */
    List<ApiRequest> findAll();
    
    /**
     * Find API requests by project ID
     * 
     * @param projectId Project ID
     * @return List of API requests for the project
     */
    List<ApiRequest> findByProjectId(String projectId);
    
    /**
     * Delete an API request by ID
     * 
     * @param id API request ID
     * @return True if deleted, false if not found
     */
    boolean deleteById(String id);
    
    /**
     * Save an API response
     * 
     * @param response API response to save
     * @return Saved API response
     */
    ApiResponse saveResponse(ApiResponse response);
    
    /**
     * Get response history for an API request
     * 
     * @param requestId API request ID
     * @param limit Maximum number of responses to return
     * @return List of API responses, sorted by most recent first
     */
    List<ApiResponse> getResponseHistory(String requestId, int limit);
} 