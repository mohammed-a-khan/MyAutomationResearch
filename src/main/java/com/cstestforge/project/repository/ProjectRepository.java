package com.cstestforge.project.repository;

import com.cstestforge.project.model.Project;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing project data
 */
public interface ProjectRepository {
    
    /**
     * Find a project by ID
     * 
     * @param id Project ID
     * @return Optional containing the project if found
     */
    Optional<Project> findById(String id);
    
    /**
     * Find all projects
     * 
     * @return List of all projects
     */
    List<Project> findAll();
    
    /**
     * Create a new project
     * 
     * @param project Project to create
     * @return Created project
     */
    Project create(Project project);
    
    /**
     * Update an existing project
     * 
     * @param id Project ID
     * @param project Updated project data
     * @return Updated project
     */
    Project update(String id, Project project);
    
    /**
     * Delete a project
     * 
     * @param id Project ID
     * @return true if deleted, false otherwise
     */
    boolean delete(String id);
    
    /**
     * Get all project IDs
     * 
     * @return List of project IDs
     */
    List<String> getAllProjectIds();
} 