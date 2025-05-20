package com.cstestforge.testing.service;

import com.cstestforge.testing.model.TestCase;
import com.cstestforge.testing.model.TestStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation of the TestCaseService interface
 */
@Service
public class TestCaseServiceImpl implements TestCaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(TestCaseServiceImpl.class);
    
    // In-memory map to store test cases by project ID
    private final Map<String, Map<String, TestCase>> testCasesByProject = new HashMap<>();
    
    @Override
    public List<TestCase> getTestsForProject(String projectId) {
        logger.info("Retrieving test cases for project: {}", projectId);
        
        Map<String, TestCase> projectTests = testCasesByProject.get(projectId);
        if (projectTests == null) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(projectTests.values());
    }
    
    @Override
    public Optional<TestCase> getTestById(String projectId, String testId) {
        logger.info("Retrieving test case: {} from project: {}", testId, projectId);
        
        Map<String, TestCase> projectTests = testCasesByProject.get(projectId);
        if (projectTests == null) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(projectTests.get(testId));
    }
    
    @Override
    public TestCase createTest(String projectId, TestCase test) {
        logger.info("Creating test case for project: {}", projectId);
        
        if (test.getId() == null || test.getId().isEmpty()) {
            test.setId(UUID.randomUUID().toString());
        }
        
        test.setCreatedDate(LocalDateTime.now());
        test.setLastModifiedDate(LocalDateTime.now());
        
        Map<String, TestCase> projectTests = testCasesByProject.computeIfAbsent(projectId, k -> new HashMap<>());
        projectTests.put(test.getId(), test);
        
        return test;
    }
    
    @Override
    public TestCase updateTest(String projectId, String testId, TestCase test) {
        logger.info("Updating test case: {} in project: {}", testId, projectId);
        
        Map<String, TestCase> projectTests = testCasesByProject.get(projectId);
        if (projectTests == null || !projectTests.containsKey(testId)) {
            throw new IllegalArgumentException("Test case not found: " + testId);
        }
        
        test.setId(testId); // Ensure ID is set correctly
        test.setLastModifiedDate(LocalDateTime.now());
        
        projectTests.put(testId, test);
        
        return test;
    }
    
    @Override
    public boolean deleteTest(String projectId, String testId) {
        logger.info("Deleting test case: {} from project: {}", testId, projectId);
        
        Map<String, TestCase> projectTests = testCasesByProject.get(projectId);
        if (projectTests == null) {
            return false;
        }
        
        return projectTests.remove(testId) != null;
    }
    
    @Override
    public TestCase addTestStep(String projectId, String testId, TestStep step) {
        logger.info("Adding test step to test case: {} in project: {}", testId, projectId);
        
        TestCase test = getTestById(projectId, testId)
            .orElseThrow(() -> new IllegalArgumentException("Test case not found: " + testId));
        
        // Generate ID for the step if not provided
        if (step.getId() == null || step.getId().isEmpty()) {
            step.setId(UUID.randomUUID().toString());
        }
        
        // Initialize the steps list if null
        if (test.getSteps() == null) {
            test.setSteps(new ArrayList<>());
        }
        
        test.getSteps().add(step);
        test.setLastModifiedDate(LocalDateTime.now());
        
        // Update the test case
        Map<String, TestCase> projectTests = testCasesByProject.get(projectId);
        projectTests.put(testId, test);
        
        return test;
    }
} 