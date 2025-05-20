package com.cstestforge.testing.service;

import com.cstestforge.testing.model.TestCase;
import com.cstestforge.testing.model.TestStep;

import java.util.List;
import java.util.Optional;

/**
 * Service for test case management operations
 */
public interface TestCaseService {
    
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
} 