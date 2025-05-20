package com.cstestforge.project.controller;

import com.cstestforge.project.model.ApiResponse;
import com.cstestforge.project.model.Environment;
import com.cstestforge.project.model.EnvironmentVariable;
import com.cstestforge.project.service.EnvironmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for environment operations.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/environments")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    @Autowired
    public EnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    /**
     * Get all environments for a project
     * 
     * @param projectId Project ID
     * @return List of environments
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Environment>>> getAllEnvironments(@PathVariable String projectId) {
        try {
            List<Environment> environments = environmentService.findAllByProjectId(projectId);
            return ResponseEntity.ok(ApiResponse.success(environments));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving environments", e.getMessage()));
        }
    }

    /**
     * Get an environment by ID
     * 
     * @param projectId Project ID
     * @param id Environment ID
     * @return Environment if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Environment>> getEnvironmentById(
            @PathVariable String projectId, @PathVariable String id) {
        try {
            return environmentService.findById(projectId, id)
                    .map(env -> ResponseEntity.ok(ApiResponse.success(env)))
                    .orElse(ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("Environment not found", 
                                    "No environment found with ID: " + id)));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving environment", e.getMessage()));
        }
    }

    /**
     * Create a new environment
     * 
     * @param projectId Project ID
     * @param environment Environment to create
     * @return Created environment
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Environment>> createEnvironment(
            @PathVariable String projectId, @RequestBody Environment environment) {
        try {
            Environment createdEnv = environmentService.create(projectId, environment);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(createdEnv, "Environment created successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Project not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error creating environment", e.getMessage()));
        }
    }

    /**
     * Update an existing environment
     * 
     * @param projectId Project ID
     * @param id Environment ID
     * @param environment Updated environment data
     * @return Updated environment
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Environment>> updateEnvironment(
            @PathVariable String projectId, @PathVariable String id, @RequestBody Environment environment) {
        try {
            Environment updatedEnv = environmentService.update(projectId, id, environment);
            return ResponseEntity.ok(ApiResponse.success(updatedEnv, "Environment updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Environment not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error updating environment", e.getMessage()));
        }
    }

    /**
     * Delete an environment
     * 
     * @param projectId Project ID
     * @param id Environment ID
     * @return Success message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEnvironment(
            @PathVariable String projectId, @PathVariable String id) {
        try {
            boolean deleted = environmentService.delete(projectId, id);
            
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success(null, "Environment deleted successfully"));
            } else {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Environment not found", 
                                "No environment found with ID: " + id));
            }
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error deleting environment", e.getMessage()));
        }
    }

    /**
     * Set an environment as the default for a project
     * 
     * @param projectId Project ID
     * @param id Environment ID
     * @return Updated environment
     */
    @PutMapping("/{id}/default")
    public ResponseEntity<ApiResponse<Environment>> setAsDefault(
            @PathVariable String projectId, @PathVariable String id) {
        try {
            Environment updatedEnv = environmentService.setAsDefault(projectId, id);
            return ResponseEntity.ok(ApiResponse.success(updatedEnv, "Environment set as default"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Environment not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error setting environment as default", e.getMessage()));
        }
    }

    /**
     * Add a variable to an environment
     * 
     * @param projectId Project ID
     * @param environmentId Environment ID
     * @param variable Variable to add
     * @return Updated environment
     */
    @PostMapping("/{environmentId}/variables")
    public ResponseEntity<ApiResponse<Environment>> addVariable(
            @PathVariable String projectId, 
            @PathVariable String environmentId, 
            @RequestBody EnvironmentVariable variable) {
        try {
            Environment updatedEnv = environmentService.addVariable(projectId, environmentId, variable);
            return ResponseEntity.ok(ApiResponse.success(updatedEnv, "Variable added successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Environment not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error adding variable", e.getMessage()));
        }
    }

    /**
     * Update a variable in an environment
     * 
     * @param projectId Project ID
     * @param environmentId Environment ID
     * @param variableId Variable ID
     * @param variable Updated variable data
     * @return Updated environment
     */
    @PutMapping("/{environmentId}/variables/{variableId}")
    public ResponseEntity<ApiResponse<Environment>> updateVariable(
            @PathVariable String projectId, 
            @PathVariable String environmentId, 
            @PathVariable String variableId, 
            @RequestBody EnvironmentVariable variable) {
        try {
            Environment updatedEnv = environmentService.updateVariable(
                    projectId, environmentId, variableId, variable);
            return ResponseEntity.ok(ApiResponse.success(updatedEnv, "Variable updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Environment or variable not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error updating variable", e.getMessage()));
        }
    }

    /**
     * Delete a variable from an environment
     * 
     * @param projectId Project ID
     * @param environmentId Environment ID
     * @param variableId Variable ID
     * @return Updated environment
     */
    @DeleteMapping("/{environmentId}/variables/{variableId}")
    public ResponseEntity<ApiResponse<Environment>> deleteVariable(
            @PathVariable String projectId, 
            @PathVariable String environmentId, 
            @PathVariable String variableId) {
        try {
            Environment updatedEnv = environmentService.deleteVariable(projectId, environmentId, variableId);
            return ResponseEntity.ok(ApiResponse.success(updatedEnv, "Variable deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Environment or variable not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error deleting variable", e.getMessage()));
        }
    }
} 