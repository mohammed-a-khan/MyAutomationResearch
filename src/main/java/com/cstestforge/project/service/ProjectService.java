package com.cstestforge.project.service;

import com.cstestforge.project.model.PagedResponse;
import com.cstestforge.project.model.Project;
import com.cstestforge.project.model.ProjectFilter;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service for managing projects.
 */
public interface ProjectService {

    /**
     * Find all projects with filtering and pagination
     * 
     * @param filter Filter criteria
     * @return Paged response of projects
     */
    PagedResponse<Project> findAll(ProjectFilter filter);

    /**
     * Find a project by ID
     * 
     * @param id Project ID
     * @return Optional containing the project if found
     */
    Optional<Project> findById(String id);

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
     * @return True if deleted successfully
     */
    boolean delete(String id);

    /**
     * Get all unique tags across all projects
     * 
     * @return Set of unique tags
     */
    Set<String> getAllTags();

    /**
     * Add a tag to a project
     * 
     * @param projectId Project ID
     * @param tag Tag to add
     * @return Updated project
     */
    Project addTag(String projectId, String tag);

    /**
     * Remove a tag from a project
     * 
     * @param projectId Project ID
     * @param tag Tag to remove
     * @return Updated project
     */
    Project removeTag(String projectId, String tag);
} 