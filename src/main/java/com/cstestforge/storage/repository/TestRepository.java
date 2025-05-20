package com.cstestforge.storage.repository;

import com.cstestforge.project.model.test.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for managing test cases.
 * Tests are stored at "projects/{projectId}/tests/"
 */
public interface TestRepository {

    /**
     * Find a test by its ID
     *
     * @param projectId Project ID
     * @param testId Test ID
     * @return Optional containing the test if found
     */
    Optional<Test> findById(String projectId, String testId);

    /**
     * Find all tests for a project
     *
     * @param projectId Project ID
     * @return List of tests
     */
    List<Test> findAll(String projectId);
    
    /**
     * Find tests by filter criteria
     *
     * @param projectId Project ID
     * @param filters Map of filter criteria
     * @return List of matching tests
     */
    List<Test> findByFilters(String projectId, Map<String, Object> filters);
    
    /**
     * Create a new test
     *
     * @param projectId Project ID
     * @param test Test to create
     * @return Created test with ID
     */
    Test create(String projectId, Test test);
    
    /**
     * Update an existing test
     *
     * @param projectId Project ID
     * @param testId Test ID
     * @param test Updated test data
     * @return Updated test
     */
    Test update(String projectId, String testId, Test test);
    
    /**
     * Delete a test
     *
     * @param projectId Project ID
     * @param testId Test ID
     * @return true if deleted successfully
     */
    boolean delete(String projectId, String testId);
    
    /**
     * Find tests by tags
     *
     * @param projectId Project ID
     * @param tags List of tags to match
     * @param matchAll If true, all tags must match; if false, any tag matches
     * @return List of matching tests
     */
    List<Test> findByTags(String projectId, List<String> tags, boolean matchAll);
    
    /**
     * Get the count of tests in a project
     *
     * @param projectId Project ID
     * @return Number of tests
     */
    int count(String projectId);
} 