package com.cstestforge.testing.service;

import com.cstestforge.project.exception.ResourceNotFoundException;
import com.cstestforge.project.exception.ConcurrencyException;
import com.cstestforge.project.storage.FileLock;
import com.cstestforge.project.storage.FileStorageService;
import com.cstestforge.testing.model.TestSuite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the TestSuiteService using file-based storage.
 */
@Service
public class TestSuiteServiceImpl implements TestSuiteService {

    private final FileStorageService fileStorageService;
    private static final String TEST_SUITES_INDEX_PATH_TEMPLATE = "projects/%s/test-suites/_index.json";

    @Autowired
    public TestSuiteServiceImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }
    
    /**
     * Initialize indexes for a project if they don't exist
     * 
     * @param projectId Project ID
     */
    private void initProjectIndexes(String projectId) {
        String testSuitesIndexPath = String.format(TEST_SUITES_INDEX_PATH_TEMPLATE, projectId);
        
        // Create test suites directory if it doesn't exist
        String testSuitesDir = String.format("projects/%s/test-suites", projectId);
        fileStorageService.createDirectoryIfNotExists(testSuitesDir);
        
        // Create test suites index if it doesn't exist
        if (!fileStorageService.fileExists(testSuitesIndexPath)) {
            fileStorageService.saveToJson(testSuitesIndexPath, new HashMap<String, String>());
        }
    }

    @Override
    public List<TestSuite> findAllByProject(String projectId) {
        // Initialize indexes if they don't exist
        initProjectIndexes(projectId);
        
        // Get all test suite IDs from the index
        String testSuitesIndexPath = String.format(TEST_SUITES_INDEX_PATH_TEMPLATE, projectId);
        Map<String, String> testSuiteIndex = fileStorageService.readMapFromJson(
                testSuitesIndexPath, String.class, String.class);

        // Load all test suites
        List<TestSuite> allTestSuites = new ArrayList<>();
        for (String suiteId : testSuiteIndex.keySet()) {
            String suitePath = String.format("projects/%s/test-suites/%s.json", projectId, suiteId);
            TestSuite suite = fileStorageService.readFromJson(suitePath, TestSuite.class);
            if (suite != null) {
                allTestSuites.add(suite);
            }
        }
        
        // Build the hierarchy - separate root suites from child suites
        List<TestSuite> rootSuites = allTestSuites.stream()
                .filter(suite -> suite.getParentSuiteId() == null || suite.getParentSuiteId().isEmpty())
                .collect(Collectors.toList());
        
        Map<String, List<TestSuite>> childrenBySuiteId = allTestSuites.stream()
                .filter(suite -> suite.getParentSuiteId() != null && !suite.getParentSuiteId().isEmpty())
                .collect(Collectors.groupingBy(TestSuite::getParentSuiteId));
        
        // Populate child suites
        populateChildSuites(rootSuites, childrenBySuiteId);
        
        return rootSuites;
    }

    /**
     * Recursively populate child suites
     * 
     * @param suites List of suites to populate with children
     * @param childrenBySuiteId Map of child suites by parent ID
     */
    private void populateChildSuites(List<TestSuite> suites, Map<String, List<TestSuite>> childrenBySuiteId) {
        if (suites == null || suites.isEmpty()) {
            return;
        }
        
        for (TestSuite suite : suites) {
            List<TestSuite> children = childrenBySuiteId.get(suite.getId());
            if (children != null) {
                suite.setChildSuites(children);
                populateChildSuites(children, childrenBySuiteId);
            }
        }
    }

    @Override
    public Optional<TestSuite> findById(String projectId, String id) {
        String suitePath = String.format("projects/%s/test-suites/%s.json", projectId, id);
        if (!fileStorageService.fileExists(suitePath)) {
            return Optional.empty();
        }

        TestSuite suite = fileStorageService.readFromJson(suitePath, TestSuite.class);
        
        // If this is a child suite, we don't need to load children
        if (suite != null && (suite.getParentSuiteId() == null || suite.getParentSuiteId().isEmpty())) {
            // Load child suites
            loadChildSuites(projectId, suite);
        }
        
        return Optional.ofNullable(suite);
    }

    /**
     * Recursively load child suites
     * 
     * @param projectId Project ID
     * @param suite Suite to load children for
     */
    private void loadChildSuites(String projectId, TestSuite suite) {
        if (suite == null) {
            return;
        }
        
        // Get all test suite IDs from the index
        String testSuitesIndexPath = String.format(TEST_SUITES_INDEX_PATH_TEMPLATE, projectId);
        Map<String, String> testSuiteIndex = fileStorageService.readMapFromJson(
                testSuitesIndexPath, String.class, String.class);
                
        // Find child suites
        List<TestSuite> childSuites = new ArrayList<>();
        for (String suiteId : testSuiteIndex.keySet()) {
            String suitePath = String.format("projects/%s/test-suites/%s.json", projectId, suiteId);
            TestSuite childSuite = fileStorageService.readFromJson(suitePath, TestSuite.class);
            if (childSuite != null && suite.getId().equals(childSuite.getParentSuiteId())) {
                childSuites.add(childSuite);
            }
        }
        
        suite.setChildSuites(childSuites);
        
        // Recursively load children of children
        for (TestSuite child : childSuites) {
            loadChildSuites(projectId, child);
        }
    }

    @Override
    public TestSuite create(String projectId, TestSuite suite) {
        // Initialize indexes if they don't exist
        initProjectIndexes(projectId);
        
        // Ensure ID is set
        if (suite.getId() == null || suite.getId().isEmpty()) {
            suite.setId(UUID.randomUUID().toString());
        }

        // Set project ID
        suite.setProjectId(projectId);
        
        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        suite.setCreatedAt(now);
        suite.setUpdatedAt(now);
        suite.setVersion(1);
        
        // Validate parent suite if set
        if (suite.getParentSuiteId() != null && !suite.getParentSuiteId().isEmpty()) {
            String parentPath = String.format("projects/%s/test-suites/%s.json", projectId, suite.getParentSuiteId());
            if (!fileStorageService.fileExists(parentPath)) {
                throw new ResourceNotFoundException("Parent TestSuite", "id", suite.getParentSuiteId());
            }
        }

        // Clear child suites when creating (these will be managed separately)
        suite.setChildSuites(new ArrayList<>());

        // Save the test suite
        String suitePath = String.format("projects/%s/test-suites/%s.json", projectId, suite.getId());

        // Update the test suite index
        String testSuitesIndexPath = String.format(TEST_SUITES_INDEX_PATH_TEMPLATE, projectId);
        try (FileLock lock = fileStorageService.lockFile(testSuitesIndexPath)) {
            if (lock != null) {
                Map<String, String> testSuiteIndex = fileStorageService.readMapFromJson(
                        testSuitesIndexPath, String.class, String.class);
                testSuiteIndex.put(suite.getId(), suite.getName());
                fileStorageService.saveToJson(testSuitesIndexPath, testSuiteIndex);
            } else {
                throw new ConcurrencyException("TestSuite Index", "projectId", projectId);
            }
        }
        
        // If this suite has a parent, update the parent's child list
        if (suite.getParentSuiteId() != null && !suite.getParentSuiteId().isEmpty()) {
            try {
                addChildSuite(projectId, suite.getParentSuiteId(), suite.getId());
            } catch (Exception e) {
                // Log the error but continue (the suite is still created)
                System.err.println("Error adding child suite to parent: " + e.getMessage());
            }
        }

        // Save the suite
        fileStorageService.saveToJson(suitePath, suite);

        return suite;
    }

    @Override
    public TestSuite update(String projectId, String id, TestSuite suite) {
        String suitePath = String.format("projects/%s/test-suites/%s.json", projectId, id);

        if (!fileStorageService.fileExists(suitePath)) {
            throw new ResourceNotFoundException("TestSuite", "id", id);
        }

        // Load the existing test suite
        TestSuite existingSuite = fileStorageService.readFromJson(suitePath, TestSuite.class);

        // Update fields
        existingSuite.setName(suite.getName());
        existingSuite.setDescription(suite.getDescription());
        existingSuite.setTags(suite.getTags());
        existingSuite.setUpdatedBy(suite.getUpdatedBy());
        existingSuite.setUpdatedAt(LocalDateTime.now());
        existingSuite.setVersion(existingSuite.getVersion() + 1);

        // Update test IDs if provided
        if (suite.getTestIds() != null) {
            existingSuite.setTestIds(suite.getTestIds());
        }

        // Handle parent suite change
        if (suite.getParentSuiteId() != null && !suite.getParentSuiteId().equals(existingSuite.getParentSuiteId())) {
            // Validate new parent suite
            if (!suite.getParentSuiteId().isEmpty()) {
                String parentPath = String.format("projects/%s/test-suites/%s.json", projectId, suite.getParentSuiteId());
                if (!fileStorageService.fileExists(parentPath)) {
                    throw new ResourceNotFoundException("Parent TestSuite", "id", suite.getParentSuiteId());
                }
                
                // Check for circular reference
                if (id.equals(suite.getParentSuiteId()) || isChildSuite(projectId, id, suite.getParentSuiteId())) {
                    throw new IllegalArgumentException("Circular reference detected: a suite cannot be its own ancestor");
                }
            }
            
            // Remove from old parent if it had one
            if (existingSuite.getParentSuiteId() != null && !existingSuite.getParentSuiteId().isEmpty()) {
                try {
                    removeChildSuite(projectId, existingSuite.getParentSuiteId(), id);
                } catch (Exception e) {
                    // Log the error but continue
                    System.err.println("Error removing child suite from parent: " + e.getMessage());
                }
            }
            
            // Add to new parent if set
            if (!suite.getParentSuiteId().isEmpty()) {
                try {
                    addChildSuite(projectId, suite.getParentSuiteId(), id);
                } catch (Exception e) {
                    // Log the error but continue
                    System.err.println("Error adding child suite to parent: " + e.getMessage());
                }
            }
            
            existingSuite.setParentSuiteId(suite.getParentSuiteId());
        }

        // Update test suite name in the index
        String testSuitesIndexPath = String.format(TEST_SUITES_INDEX_PATH_TEMPLATE, projectId);
        try (FileLock lock = fileStorageService.lockFile(testSuitesIndexPath)) {
            if (lock != null) {
                Map<String, String> testSuiteIndex = fileStorageService.readMapFromJson(
                        testSuitesIndexPath, String.class, String.class);
                testSuiteIndex.put(id, existingSuite.getName());
                fileStorageService.saveToJson(testSuitesIndexPath, testSuiteIndex);
            } else {
                throw new ConcurrencyException("TestSuite Index", "projectId", projectId);
            }
        }

        // Save the updated test suite
        fileStorageService.saveToJson(suitePath, existingSuite);

        return existingSuite;
    }

    /**
     * Check if a suite is a child (or descendant) of another suite
     * 
     * @param projectId Project ID
     * @param parentId Potential parent suite ID
     * @param childId Potential child suite ID
     * @return True if childId is a descendant of parentId
     */
    private boolean isChildSuite(String projectId, String parentId, String childId) {
        String childPath = String.format("projects/%s/test-suites/%s.json", projectId, childId);
        if (!fileStorageService.fileExists(childPath)) {
            return false;
        }
        
        TestSuite childSuite = fileStorageService.readFromJson(childPath, TestSuite.class);
        if (childSuite == null || childSuite.getParentSuiteId() == null || childSuite.getParentSuiteId().isEmpty()) {
            return false;
        }
        
        if (parentId.equals(childSuite.getParentSuiteId())) {
            return true;
        }
        
        return isChildSuite(projectId, parentId, childSuite.getParentSuiteId());
    }

    @Override
    public boolean delete(String projectId, String id) {
        String suitePath = String.format("projects/%s/test-suites/%s.json", projectId, id);

        if (!fileStorageService.fileExists(suitePath)) {
            throw new ResourceNotFoundException("TestSuite", "id", id);
        }

        // Load the suite to check for parent and children
        TestSuite suite = fileStorageService.readFromJson(suitePath, TestSuite.class);
        
        // If this suite has a parent, update the parent's child list
        if (suite != null && suite.getParentSuiteId() != null && !suite.getParentSuiteId().isEmpty()) {
            try {
                removeChildSuite(projectId, suite.getParentSuiteId(), id);
            } catch (Exception e) {
                // Log the error but continue with deletion
                System.err.println("Error removing child suite from parent: " + e.getMessage());
            }
        }
        
        // Handle child suites - either delete them or make them root suites
        if (suite != null) {
            // Get all child suites
            List<TestSuite> childSuites = getChildSuites(projectId, id);
            
            for (TestSuite childSuite : childSuites) {
                // Make child suites root suites
                childSuite.setParentSuiteId(null);
                String childPath = String.format("projects/%s/test-suites/%s.json", projectId, childSuite.getId());
                fileStorageService.saveToJson(childPath, childSuite);
            }
        }

        // Remove from test suite index
        String testSuitesIndexPath = String.format(TEST_SUITES_INDEX_PATH_TEMPLATE, projectId);
        try (FileLock lock = fileStorageService.lockFile(testSuitesIndexPath)) {
            if (lock != null) {
                Map<String, String> testSuiteIndex = fileStorageService.readMapFromJson(
                        testSuitesIndexPath, String.class, String.class);
                testSuiteIndex.remove(id);
                fileStorageService.saveToJson(testSuitesIndexPath, testSuiteIndex);
            } else {
                throw new ConcurrencyException("TestSuite Index", "projectId", projectId);
            }
        }

        // Delete the test suite file
        return fileStorageService.deleteFile(suitePath);
    }
    
    /**
     * Get all direct child suites of a test suite
     * 
     * @param projectId Project ID
     * @param suiteId Parent suite ID
     * @return List of child suites
     */
    private List<TestSuite> getChildSuites(String projectId, String suiteId) {
        // Get all test suite IDs from the index
        String testSuitesIndexPath = String.format(TEST_SUITES_INDEX_PATH_TEMPLATE, projectId);
        Map<String, String> testSuiteIndex = fileStorageService.readMapFromJson(
                testSuitesIndexPath, String.class, String.class);
                
        // Find child suites
        List<TestSuite> childSuites = new ArrayList<>();
        for (String id : testSuiteIndex.keySet()) {
            String suitePath = String.format("projects/%s/test-suites/%s.json", projectId, id);
            TestSuite childSuite = fileStorageService.readFromJson(suitePath, TestSuite.class);
            if (childSuite != null && suiteId.equals(childSuite.getParentSuiteId())) {
                childSuites.add(childSuite);
            }
        }
        
        return childSuites;
    }

    @Override
    public TestSuite addTest(String projectId, String suiteId, String testId) {
        String suitePath = String.format("projects/%s/test-suites/%s.json", projectId, suiteId);
        String testPath = String.format("projects/%s/tests/%s.json", projectId, testId);

        if (!fileStorageService.fileExists(suitePath)) {
            throw new ResourceNotFoundException("TestSuite", "id", suiteId);
        }
        
        if (!fileStorageService.fileExists(testPath)) {
            throw new ResourceNotFoundException("Test", "id", testId);
        }

        // Load the test suite
        TestSuite suite = fileStorageService.readFromJson(suitePath, TestSuite.class);
        suite.addTest(testId);
        suite.setUpdatedAt(LocalDateTime.now());

        // Save the updated test suite
        fileStorageService.saveToJson(suitePath, suite);

        return suite;
    }

    @Override
    public TestSuite removeTest(String projectId, String suiteId, String testId) {
        String suitePath = String.format("projects/%s/test-suites/%s.json", projectId, suiteId);

        if (!fileStorageService.fileExists(suitePath)) {
            throw new ResourceNotFoundException("TestSuite", "id", suiteId);
        }

        // Load the test suite
        TestSuite suite = fileStorageService.readFromJson(suitePath, TestSuite.class);
        suite.removeTest(testId);
        suite.setUpdatedAt(LocalDateTime.now());

        // Save the updated test suite
        fileStorageService.saveToJson(suitePath, suite);

        return suite;
    }

    @Override
    public TestSuite addChildSuite(String projectId, String parentId, String childId) {
        if (parentId.equals(childId)) {
            throw new IllegalArgumentException("A test suite cannot be a child of itself");
        }
        
        String parentPath = String.format("projects/%s/test-suites/%s.json", projectId, parentId);
        String childPath = String.format("projects/%s/test-suites/%s.json", projectId, childId);

        if (!fileStorageService.fileExists(parentPath)) {
            throw new ResourceNotFoundException("Parent TestSuite", "id", parentId);
        }
        
        if (!fileStorageService.fileExists(childPath)) {
            throw new ResourceNotFoundException("Child TestSuite", "id", childId);
        }
        
        // Check for circular reference
        if (isChildSuite(projectId, childId, parentId)) {
            throw new IllegalArgumentException("Circular reference detected: a suite cannot be its own ancestor");
        }
        
        // Load the child suite to update its parent reference
        TestSuite childSuite = fileStorageService.readFromJson(childPath, TestSuite.class);
        
        // If child already has a different parent, remove it from that parent
        if (childSuite.getParentSuiteId() != null && !childSuite.getParentSuiteId().isEmpty() 
                && !childSuite.getParentSuiteId().equals(parentId)) {
            try {
                removeChildSuite(projectId, childSuite.getParentSuiteId(), childId);
            } catch (Exception e) {
                // Log the error but continue
                System.err.println("Error removing child suite from previous parent: " + e.getMessage());
            }
        }
        
        // Update child's parent reference
        childSuite.setParentSuiteId(parentId);
        childSuite.setUpdatedAt(LocalDateTime.now());
        fileStorageService.saveToJson(childPath, childSuite);
        
        // Load the parent suite
        TestSuite parentSuite = fileStorageService.readFromJson(parentPath, TestSuite.class);
        parentSuite.setUpdatedAt(LocalDateTime.now());
        
        // Note: When we load a suite, we populate its children dynamically,
        // so we don't need to modify the parent's child list explicitly here

        // Save the updated parent suite
        fileStorageService.saveToJson(parentPath, parentSuite);

        return parentSuite;
    }

    @Override
    public TestSuite removeChildSuite(String projectId, String parentId, String childId) {
        String parentPath = String.format("projects/%s/test-suites/%s.json", projectId, parentId);
        String childPath = String.format("projects/%s/test-suites/%s.json", projectId, childId);

        if (!fileStorageService.fileExists(parentPath)) {
            throw new ResourceNotFoundException("Parent TestSuite", "id", parentId);
        }
        
        if (!fileStorageService.fileExists(childPath)) {
            throw new ResourceNotFoundException("Child TestSuite", "id", childId);
        }
        
        // Load the child suite to update its parent reference
        TestSuite childSuite = fileStorageService.readFromJson(childPath, TestSuite.class);
        
        // Only update if this parent is actually the child's parent
        if (childSuite.getParentSuiteId() != null && childSuite.getParentSuiteId().equals(parentId)) {
            childSuite.setParentSuiteId(null);
            childSuite.setUpdatedAt(LocalDateTime.now());
            fileStorageService.saveToJson(childPath, childSuite);
        }
        
        // Load the parent suite
        TestSuite parentSuite = fileStorageService.readFromJson(parentPath, TestSuite.class);
        parentSuite.setUpdatedAt(LocalDateTime.now());
        
        // Note: When we load a suite, we populate its children dynamically,
        // so we don't need to modify the parent's child list explicitly here

        // Save the updated parent suite
        fileStorageService.saveToJson(parentPath, parentSuite);

        return parentSuite;
    }
} 