package com.cstestforge.testing.service;

import com.cstestforge.project.model.PagedResponse;
import com.cstestforge.project.model.test.Test;
import com.cstestforge.project.model.test.TestStep;
import com.cstestforge.testing.model.TestFilter;
import com.cstestforge.testing.model.TestCase;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service for test management operations
 */
public interface TestService {

    /**
     * Get all tests for a project
     * 
     * @param projectId Project ID
     * @return List of tests
     */
    List<TestCase> getTestsForProject(String projectId);

    /**
     * Get a test case by ID
     * 
     * @param projectId Project ID
     * @param testId Test ID
     * @return Optional containing the test if found
     */
    Optional<TestCase> getTestById(String projectId, String testId);

    /**
     * Create a new test case
     * 
     * @param projectId Project ID
     * @param test Test case to create
     * @return Created test case
     */
    TestCase createTest(String projectId, TestCase test);

    /**
     * Update an existing test case
     * 
     * @param projectId Project ID
     * @param testId Test ID
     * @param test Updated test case
     * @return Updated test case
     */
    TestCase updateTest(String projectId, String testId, TestCase test);

    /**
     * Delete a test case
     * 
     * @param projectId Project ID
     * @param testId Test ID
     * @return True if deleted successfully
     */
    boolean deleteTest(String projectId, String testId);

    /**
     * Add a step to a test case
     * 
     * @param projectId Project ID
     * @param testId Test ID
     * @param step Test step to add
     * @return Updated test case
     */
    TestCase addTestStep(String projectId, String testId, TestStep step);

    /**
     * Find all tests with filtering and pagination
     * 
     * @param projectId Project ID
     * @param filter Filter criteria
     * @return Paged response of tests
     */
    PagedResponse<Test> findAll(String projectId, TestFilter filter);

    /**
     * Find a test by ID
     * 
     * @param projectId Project ID
     * @param id Test ID
     * @return Optional containing the test if found
     */
    Optional<Test> findById(String projectId, String id);

    /**
     * Create a new test
     * 
     * @param projectId Project ID
     * @param test Test to create
     * @return Created test
     */
    Test create(String projectId, Test test);

    /**
     * Update an existing test
     * 
     * @param projectId Project ID
     * @param id Test ID
     * @param test Updated test data
     * @return Updated test
     */
    Test update(String projectId, String id, Test test);

    /**
     * Delete a test
     * 
     * @param projectId Project ID
     * @param id Test ID
     * @return True if deleted successfully
     */
    boolean delete(String projectId, String id);

    /**
     * Duplicate a test
     * 
     * @param projectId Project ID
     * @param id Test ID
     * @return Duplicated test
     */
    Test duplicate(String projectId, String id);

    /**
     * Get all unique tags across all tests in a project
     * 
     * @param projectId Project ID
     * @return Set of unique tags
     */
    Set<String> getAllTags(String projectId);

    /**
     * Add a tag to a test
     * 
     * @param projectId Project ID
     * @param testId Test ID
     * @param tag Tag to add
     * @return Updated test
     */
    Test addTag(String projectId, String testId, String tag);

    /**
     * Remove a tag from a test
     * 
     * @param projectId Project ID
     * @param testId Test ID
     * @param tag Tag to remove
     * @return Updated test
     */
    Test removeTag(String projectId, String testId, String tag);

    /**
     * Add a step to a test
     * 
     * @param projectId Project ID
     * @param testId Test ID
     * @param step Step to add
     * @return Updated test
     */
    Test addStep(String projectId, String testId, TestStep step);

    /**
     * Update a step in a test
     * 
     * @param projectId Project ID
     * @param testId Test ID
     * @param stepId Step ID
     * @param step Updated step data
     * @return Updated test
     */
    Test updateStep(String projectId, String testId, String stepId, TestStep step);

    /**
     * Delete a step from a test
     * 
     * @param projectId Project ID
     * @param testId Test ID
     * @param stepId Step ID
     * @return Updated test
     */
    Test deleteStep(String projectId, String testId, String stepId);

    /**
     * Reorder steps in a test
     * 
     * @param projectId Project ID
     * @param testId Test ID
     * @param stepIds List of step IDs in the new order
     * @return Updated test
     */
    Test reorderSteps(String projectId, String testId, List<String> stepIds);
} 