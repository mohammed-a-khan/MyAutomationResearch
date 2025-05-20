package com.cstestforge.testing.service;

import com.cstestforge.testing.model.TestSuite;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing test suites.
 */
public interface TestSuiteService {

    /**
     * Find all test suites in a project
     * 
     * @param projectId Project ID
     * @return List of test suites
     */
    List<TestSuite> findAllByProject(String projectId);

    /**
     * Find a test suite by ID
     * 
     * @param projectId Project ID
     * @param id Test suite ID
     * @return Optional containing the test suite if found
     */
    Optional<TestSuite> findById(String projectId, String id);

    /**
     * Create a new test suite
     * 
     * @param projectId Project ID
     * @param suite Test suite to create
     * @return Created test suite
     */
    TestSuite create(String projectId, TestSuite suite);

    /**
     * Update an existing test suite
     * 
     * @param projectId Project ID
     * @param id Test suite ID
     * @param suite Updated test suite data
     * @return Updated test suite
     */
    TestSuite update(String projectId, String id, TestSuite suite);

    /**
     * Delete a test suite
     * 
     * @param projectId Project ID
     * @param id Test suite ID
     * @return True if deleted successfully
     */
    boolean delete(String projectId, String id);

    /**
     * Add a test to a test suite
     * 
     * @param projectId Project ID
     * @param suiteId Test suite ID
     * @param testId Test ID
     * @return Updated test suite
     */
    TestSuite addTest(String projectId, String suiteId, String testId);

    /**
     * Remove a test from a test suite
     * 
     * @param projectId Project ID
     * @param suiteId Test suite ID
     * @param testId Test ID
     * @return Updated test suite
     */
    TestSuite removeTest(String projectId, String suiteId, String testId);

    /**
     * Add a child suite to a parent suite
     * 
     * @param projectId Project ID
     * @param parentId Parent suite ID
     * @param childId Child suite ID
     * @return Updated parent test suite
     */
    TestSuite addChildSuite(String projectId, String parentId, String childId);

    /**
     * Remove a child suite from a parent suite
     * 
     * @param projectId Project ID
     * @param parentId Parent suite ID
     * @param childId Child suite ID
     * @return Updated parent test suite
     */
    TestSuite removeChildSuite(String projectId, String parentId, String childId);
} 