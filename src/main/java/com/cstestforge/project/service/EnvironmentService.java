package com.cstestforge.project.service;

import com.cstestforge.project.model.Environment;
import com.cstestforge.project.model.EnvironmentVariable;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing environments and their variables.
 */
public interface EnvironmentService {

    /**
     * Find all environments for a project
     * 
     * @param projectId Project ID
     * @return List of environments
     */
    List<Environment> findAllByProjectId(String projectId);

    /**
     * Find an environment by ID
     * 
     * @param projectId Project ID
     * @param id Environment ID
     * @return Optional containing the environment if found
     */
    Optional<Environment> findById(String projectId, String id);

    /**
     * Create a new environment
     * 
     * @param projectId Project ID
     * @param environment Environment to create
     * @return Created environment
     */
    Environment create(String projectId, Environment environment);

    /**
     * Update an existing environment
     * 
     * @param projectId Project ID
     * @param id Environment ID
     * @param environment Updated environment data
     * @return Updated environment
     */
    Environment update(String projectId, String id, Environment environment);

    /**
     * Delete an environment
     * 
     * @param projectId Project ID
     * @param id Environment ID
     * @return True if deleted successfully
     */
    boolean delete(String projectId, String id);

    /**
     * Set an environment as the default for a project
     * 
     * @param projectId Project ID
     * @param id Environment ID
     * @return Updated environment
     */
    Environment setAsDefault(String projectId, String id);

    /**
     * Add a variable to an environment
     * 
     * @param projectId Project ID
     * @param environmentId Environment ID
     * @param variable Variable to add
     * @return Updated environment
     */
    Environment addVariable(String projectId, String environmentId, EnvironmentVariable variable);

    /**
     * Update a variable in an environment
     * 
     * @param projectId Project ID
     * @param environmentId Environment ID
     * @param variableId Variable ID
     * @param variable Updated variable data
     * @return Updated environment
     */
    Environment updateVariable(String projectId, String environmentId, String variableId, EnvironmentVariable variable);

    /**
     * Delete a variable from an environment
     * 
     * @param projectId Project ID
     * @param environmentId Environment ID
     * @param variableId Variable ID
     * @return Updated environment
     */
    Environment deleteVariable(String projectId, String environmentId, String variableId);
} 