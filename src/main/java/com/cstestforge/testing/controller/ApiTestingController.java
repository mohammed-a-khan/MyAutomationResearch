package com.cstestforge.testing.controller;

import com.cstestforge.testing.model.ApiRequest;
import com.cstestforge.testing.model.ApiResponse;
import com.cstestforge.testing.service.ApiTestingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for API testing functionality
 */
@RestController
@RequestMapping("/api/testing")
public class ApiTestingController {
    
    private final ApiTestingService apiTestingService;
    
    @Autowired
    public ApiTestingController(ApiTestingService apiTestingService) {
        this.apiTestingService = apiTestingService;
    }
    
    /**
     * Get all API requests, optionally filtered by project
     * 
     * @param projectId Optional project ID to filter by
     * @return List of API requests
     */
    @GetMapping("/requests")
    public ResponseEntity<List<ApiRequest>> getAllRequests(
            @RequestParam(required = false) String projectId) {
        return ResponseEntity.ok(apiTestingService.getAllRequests(projectId));
    }
    
    /**
     * Get an API request by ID
     * 
     * @param id API request ID
     * @return API request
     */
    @GetMapping("/requests/{id}")
    public ResponseEntity<ApiRequest> getRequestById(@PathVariable String id) {
        try {
            ApiRequest request = apiTestingService.getRequestById(id);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Create a new API request
     * 
     * @param request API request to create
     * @return Created API request
     */
    @PostMapping("/requests")
    public ResponseEntity<ApiRequest> createRequest(@RequestBody ApiRequest request) {
        ApiRequest createdRequest = apiTestingService.createRequest(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(createdRequest);
    }
    
    /**
     * Update an existing API request
     * 
     * @param id API request ID
     * @param request API request to update
     * @return Updated API request
     */
    @PutMapping("/requests/{id}")
    public ResponseEntity<ApiRequest> updateRequest(
            @PathVariable String id,
            @RequestBody ApiRequest request) {
        try {
            request.setId(id);
            ApiRequest updatedRequest = apiTestingService.updateRequest(request);
            return ResponseEntity.ok(updatedRequest);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Delete an API request
     * 
     * @param id API request ID
     * @return No content if successful
     */
    @DeleteMapping("/requests/{id}")
    public ResponseEntity<Void> deleteRequest(@PathVariable String id) {
        boolean deleted = apiTestingService.deleteRequest(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
    
    /**
     * Execute an API request
     * 
     * @param id API request ID
     * @param request Optional request body with overrides
     * @return API response
     */
    @PostMapping("/requests/{id}/execute")
    public ResponseEntity<ApiResponse> executeRequest(
            @PathVariable String id,
            @RequestBody(required = false) ApiRequest request) {
        ApiResponse response;
        
        if (request != null) {
            request.setId(id);
            response = apiTestingService.executeRequest(request);
        } else {
            response = apiTestingService.executeRequestById(id);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Execute an API request with custom variables
     * 
     * @param id API request ID
     * @param variables Map of variable names to values
     * @return API response
     */
    @PostMapping("/requests/{id}/execute-with-variables")
    public ResponseEntity<ApiResponse> executeRequestWithVariables(
            @PathVariable String id,
            @RequestBody Map<String, String> variables) {
        try {
            ApiRequest request = apiTestingService.getRequestById(id);
            ApiResponse response = apiTestingService.executeRequestWithVariables(request, variables);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get execution history for an API request
     * 
     * @param id API request ID
     * @param limit Maximum number of responses to return
     * @return List of API responses
     */
    @GetMapping("/requests/{id}/history")
    public ResponseEntity<List<ApiResponse>> getExecutionHistory(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        try {
            // Verify the request exists
            apiTestingService.getRequestById(id);
            
            List<ApiResponse> history = apiTestingService.getRequestExecutionHistory(id, limit);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Execute a one-off API request without saving it
     * 
     * @param request API request to execute
     * @return API response
     */
    @PostMapping("/execute")
    public ResponseEntity<ApiResponse> executeOneOffRequest(@RequestBody ApiRequest request) {
        ApiResponse response = apiTestingService.executeRequest(request);
        return ResponseEntity.ok(response);
    }
} 