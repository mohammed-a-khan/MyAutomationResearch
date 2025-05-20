package com.cstestforge.testing.service;

import com.cstestforge.project.exception.ResourceNotFoundException;
import com.cstestforge.project.exception.ConcurrencyException;
import com.cstestforge.project.model.PagedResponse;
import com.cstestforge.project.model.test.Test;
import com.cstestforge.project.model.test.TestStep;
import com.cstestforge.project.model.test.TestStatus;
import com.cstestforge.project.model.test.TestConfig;
import com.cstestforge.project.storage.FileLock;
import com.cstestforge.project.storage.FileStorageService;
import com.cstestforge.testing.model.TestFilter;
import com.cstestforge.testing.model.TestCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the TestService using file-based storage.
 */
@Service
public class TestServiceImpl implements TestService {

    private final FileStorageService fileStorageService;
    private static final String TESTS_INDEX_PATH_TEMPLATE = "projects/%s/tests/_index.json";
    private static final String TESTS_TAGS_PATH_TEMPLATE = "projects/%s/tests/_tags.json";

    @Autowired
    public TestServiceImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Initialize indexes for a project if they don't exist
     * 
     * @param projectId Project ID
     */
    private void initProjectIndexes(String projectId) {
        String testsIndexPath = String.format(TESTS_INDEX_PATH_TEMPLATE, projectId);
        String testsTagsPath = String.format(TESTS_TAGS_PATH_TEMPLATE, projectId);
        
        // Create tests directory if it doesn't exist
        String testsDir = String.format("projects/%s/tests", projectId);
        fileStorageService.createDirectoryIfNotExists(testsDir);
        
        // Create tests index if it doesn't exist
        if (!fileStorageService.fileExists(testsIndexPath)) {
            fileStorageService.saveToJson(testsIndexPath, new HashMap<String, String>());
        }

        // Create tags index if it doesn't exist
        if (!fileStorageService.fileExists(testsTagsPath)) {
            fileStorageService.saveToJson(testsTagsPath, new HashSet<String>());
        }
    }

    @Override
    public PagedResponse<Test> findAll(String projectId, TestFilter filter) {
        // Initialize indexes if they don't exist
        initProjectIndexes(projectId);
        
        // Get all test IDs from the index
        String testsIndexPath = String.format(TESTS_INDEX_PATH_TEMPLATE, projectId);
        Map<String, String> testIndex = fileStorageService.readMapFromJson(
                testsIndexPath, String.class, String.class);

        // Load all tests
        List<Test> allTests = new ArrayList<>();
        for (String testId : testIndex.keySet()) {
            String testPath = String.format("projects/%s/tests/%s.json", projectId, testId);
            Test test = fileStorageService.readFromJson(testPath, Test.class);
            if (test != null) {
                allTests.add(test);
            }
        }

        // Apply filtering
        List<Test> filteredTests = allTests.stream()
                .filter(t -> applyFilter(t, filter))
                .collect(Collectors.toList());

        // Apply sorting
        sortTests(filteredTests, filter.getSortBy(), filter.getSortDirection());

        // Apply pagination
        int total = filteredTests.size();
        int offset = filter.getOffset();
        int limit = filter.getSize();

        List<Test> pagedTests = filteredTests.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

        return new PagedResponse<>(pagedTests, filter.getPage(), filter.getSize(), total);
    }

    @Override
    public Optional<Test> findById(String projectId, String id) {
        String testPath = String.format("projects/%s/tests/%s.json", projectId, id);
        if (!fileStorageService.fileExists(testPath)) {
            return Optional.empty();
        }

        Test test = fileStorageService.readFromJson(testPath, Test.class);
        return Optional.ofNullable(test);
    }

    @Override
    public Test create(String projectId, Test test) {
        // Initialize indexes if they don't exist
        initProjectIndexes(projectId);
        
        // Ensure ID is set
        if (test.getId() == null || test.getId().isEmpty()) {
            test.setId(UUID.randomUUID().toString());
        }

        // Set project ID
        test.setProjectId(projectId);
        
        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        test.setCreatedAt(now);
        test.setUpdatedAt(now);
        test.setVersion(1);

        // Set default status if not set
        if (test.getStatus() == null) {
            test.setStatus(TestStatus.DRAFT);
        }

        // Save the test
        String testPath = String.format("projects/%s/tests/%s.json", projectId, test.getId());

        // Update the test index
        String testsIndexPath = String.format(TESTS_INDEX_PATH_TEMPLATE, projectId);
        try (FileLock lock = fileStorageService.lockFile(testsIndexPath)) {
            if (lock != null) {
                Map<String, String> testIndex = fileStorageService.readMapFromJson(
                        testsIndexPath, String.class, String.class);
                testIndex.put(test.getId(), test.getName());
                fileStorageService.saveToJson(testsIndexPath, testIndex);
            } else {
                throw new ConcurrencyException("Test Index", "projectId", projectId);
            }
        }

        // Update tags index
        updateTagsIndex(projectId, test.getTags());

        // Save the test
        fileStorageService.saveToJson(testPath, test);

        return test;
    }

    @Override
    public Test update(String projectId, String id, Test test) {
        String testPath = String.format("projects/%s/tests/%s.json", projectId, id);

        if (!fileStorageService.fileExists(testPath)) {
            throw new ResourceNotFoundException("Test", "id", id);
        }

        // Load the existing test
        Test existingTest = fileStorageService.readFromJson(testPath, Test.class);

        // Save old tags to remove unused ones
        Set<String> oldTags = new HashSet<>(existingTest.getTags());

        // Update fields
        existingTest.setName(test.getName());
        existingTest.setDescription(test.getDescription());
        existingTest.setType(test.getType());
        existingTest.setStatus(test.getStatus());
        existingTest.setTags(test.getTags());
        existingTest.setUpdatedBy(test.getUpdatedBy());
        existingTest.setUpdatedAt(LocalDateTime.now());
        existingTest.setVersion(existingTest.getVersion() + 1);

        // Update steps if provided
        if (test.getSteps() != null && !test.getSteps().isEmpty()) {
            existingTest.setSteps(test.getSteps());
        }

        // Update config if provided
        if (test.getConfig() != null) {
            existingTest.setConfig(test.getConfig());
        }

        // Update test name in the index
        String testsIndexPath = String.format(TESTS_INDEX_PATH_TEMPLATE, projectId);
        try (FileLock lock = fileStorageService.lockFile(testsIndexPath)) {
            if (lock != null) {
                Map<String, String> testIndex = fileStorageService.readMapFromJson(
                        testsIndexPath, String.class, String.class);
                testIndex.put(id, existingTest.getName());
                fileStorageService.saveToJson(testsIndexPath, testIndex);
            } else {
                throw new ConcurrencyException("Test Index", "projectId", projectId);
            }
        }

        // Update tags index
        updateTagsIndex(projectId, test.getTags());
        removeUnusedTags(projectId, oldTags);

        // Save the updated test
        fileStorageService.saveToJson(testPath, existingTest);

        return existingTest;
    }

    @Override
    public boolean delete(String projectId, String id) {
        String testPath = String.format("projects/%s/tests/%s.json", projectId, id);

        if (!fileStorageService.fileExists(testPath)) {
            throw new ResourceNotFoundException("Test", "id", id);
        }

        // Load the test to get tags
        Test test = fileStorageService.readFromJson(testPath, Test.class);
        if (test != null) {
            // Remove test tags from the index
            removeUnusedTags(projectId, test.getTags());
        }

        // Remove from test index
        String testsIndexPath = String.format(TESTS_INDEX_PATH_TEMPLATE, projectId);
        try (FileLock lock = fileStorageService.lockFile(testsIndexPath)) {
            if (lock != null) {
                Map<String, String> testIndex = fileStorageService.readMapFromJson(
                        testsIndexPath, String.class, String.class);
                testIndex.remove(id);
                fileStorageService.saveToJson(testsIndexPath, testIndex);
            } else {
                throw new ConcurrencyException("Test Index", "projectId", projectId);
            }
        }

        // Delete the test file
        return fileStorageService.deleteFile(testPath);
    }

    @Override
    public Test duplicate(String projectId, String id) {
        String testPath = String.format("projects/%s/tests/%s.json", projectId, id);

        if (!fileStorageService.fileExists(testPath)) {
            throw new ResourceNotFoundException("Test", "id", id);
        }

        // Load the existing test
        Test existingTest = fileStorageService.readFromJson(testPath, Test.class);

        // Create a new test as a copy
        Test duplicateTest = new Test();
        duplicateTest.setId(UUID.randomUUID().toString());
        duplicateTest.setName(existingTest.getName() + " (Copy)");
        duplicateTest.setDescription(existingTest.getDescription());
        duplicateTest.setType(existingTest.getType());
        duplicateTest.setStatus(TestStatus.DRAFT); // Always start as draft
        duplicateTest.setProjectId(projectId);
        duplicateTest.setCreatedBy(existingTest.getUpdatedBy() != null ? 
                existingTest.getUpdatedBy() : existingTest.getCreatedBy());
        
        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        duplicateTest.setCreatedAt(now);
        duplicateTest.setUpdatedAt(now);
        duplicateTest.setVersion(1);

        // Copy tags
        for (String tag : existingTest.getTags()) {
            duplicateTest.addTag(tag);
        }

        // Copy steps
        for (TestStep step : existingTest.getSteps()) {
            TestStep newStep = new TestStep();
            newStep.setId(UUID.randomUUID().toString());
            newStep.setName(step.getName());
            newStep.setDescription(step.getDescription());
            newStep.setOrder(step.getOrder());
            newStep.setType(step.getType());
            newStep.setParameters(new HashMap<>(step.getParameters()));
            newStep.setDisabled(step.isDisabled());
            newStep.setMetadata(new HashMap<>(step.getMetadata()));
            
            duplicateTest.addStep(newStep);
        }

        // Copy config
        duplicateTest.setConfig(existingTest.getConfig());

        // Save the duplicated test
        String duplicateTestPath = String.format("projects/%s/tests/%s.json", projectId, duplicateTest.getId());

        // Update the test index
        String testsIndexPath = String.format(TESTS_INDEX_PATH_TEMPLATE, projectId);
        try (FileLock lock = fileStorageService.lockFile(testsIndexPath)) {
            if (lock != null) {
                Map<String, String> testIndex = fileStorageService.readMapFromJson(
                        testsIndexPath, String.class, String.class);
                testIndex.put(duplicateTest.getId(), duplicateTest.getName());
                fileStorageService.saveToJson(testsIndexPath, testIndex);
            } else {
                throw new ConcurrencyException("Test Index", "projectId", projectId);
            }
        }

        // Update tags index
        updateTagsIndex(projectId, duplicateTest.getTags());

        // Save the test
        fileStorageService.saveToJson(duplicateTestPath, duplicateTest);

        return duplicateTest;
    }

    @Override
    public Set<String> getAllTags(String projectId) {
        initProjectIndexes(projectId);
        
        String testsTagsPath = String.format(TESTS_TAGS_PATH_TEMPLATE, projectId);
        return fileStorageService.readFromJson(testsTagsPath, Set.class);
    }

    @Override
    public Test addTag(String projectId, String testId, String tag) {
        String testPath = String.format("projects/%s/tests/%s.json", projectId, testId);

        if (!fileStorageService.fileExists(testPath)) {
            throw new ResourceNotFoundException("Test", "id", testId);
        }

        // Load the test
        Test test = fileStorageService.readFromJson(testPath, Test.class);
        test.addTag(tag);
        test.setUpdatedAt(LocalDateTime.now());

        // Update tags index
        updateTagsIndex(projectId, Set.of(tag));

        // Save the updated test
        fileStorageService.saveToJson(testPath, test);

        return test;
    }

    @Override
    public Test removeTag(String projectId, String testId, String tag) {
        String testPath = String.format("projects/%s/tests/%s.json", projectId, testId);

        if (!fileStorageService.fileExists(testPath)) {
            throw new ResourceNotFoundException("Test", "id", testId);
        }

        // Load the test
        Test test = fileStorageService.readFromJson(testPath, Test.class);
        if (test.getTags() != null) {
            test.getTags().remove(tag);
            test.setUpdatedAt(LocalDateTime.now());
        }

        // Update tags index (check if tag is used elsewhere)
        removeUnusedTags(projectId, Set.of(tag));

        // Save the updated test
        fileStorageService.saveToJson(testPath, test);

        return test;
    }

    @Override
    public Test addStep(String projectId, String testId, TestStep step) {
        String testPath = String.format("projects/%s/tests/%s.json", projectId, testId);

        if (!fileStorageService.fileExists(testPath)) {
            throw new ResourceNotFoundException("Test", "id", testId);
        }

        // Load the test
        Test test = fileStorageService.readFromJson(testPath, Test.class);
        
        // Ensure step has an ID
        if (step.getId() == null || step.getId().isEmpty()) {
            step.setId(UUID.randomUUID().toString());
        }
        
        // Add the step
        test.addStep(step);
        test.setUpdatedAt(LocalDateTime.now());

        // Save the updated test
        fileStorageService.saveToJson(testPath, test);

        return test;
    }

    @Override
    public Test updateStep(String projectId, String testId, String stepId, TestStep step) {
        String testPath = String.format("projects/%s/tests/%s.json", projectId, testId);

        if (!fileStorageService.fileExists(testPath)) {
            throw new ResourceNotFoundException("Test", "id", testId);
        }

        // Load the test
        Test test = fileStorageService.readFromJson(testPath, Test.class);
        
        // Find and update the step
        boolean stepFound = false;
        for (int i = 0; i < test.getSteps().size(); i++) {
            TestStep existingStep = test.getSteps().get(i);
            if (existingStep.getId().equals(stepId)) {
                // Preserve step ID
                step.setId(stepId);
                test.getSteps().set(i, step);
                stepFound = true;
                break;
            }
        }
        
        if (!stepFound) {
            throw new ResourceNotFoundException("TestStep", "id", stepId);
        }
        
        test.setUpdatedAt(LocalDateTime.now());

        // Save the updated test
        fileStorageService.saveToJson(testPath, test);

        return test;
    }

    @Override
    public Test deleteStep(String projectId, String testId, String stepId) {
        String testPath = String.format("projects/%s/tests/%s.json", projectId, testId);

        if (!fileStorageService.fileExists(testPath)) {
            throw new ResourceNotFoundException("Test", "id", testId);
        }

        // Load the test
        Test test = fileStorageService.readFromJson(testPath, Test.class);
        
        // Find and remove the step
        boolean stepFound = test.getSteps().removeIf(step -> step.getId().equals(stepId));
        
        if (!stepFound) {
            throw new ResourceNotFoundException("TestStep", "id", stepId);
        }
        
        test.setUpdatedAt(LocalDateTime.now());

        // Save the updated test
        fileStorageService.saveToJson(testPath, test);

        return test;
    }

    @Override
    public Test reorderSteps(String projectId, String testId, List<String> stepIds) {
        String testPath = String.format("projects/%s/tests/%s.json", projectId, testId);

        if (!fileStorageService.fileExists(testPath)) {
            throw new ResourceNotFoundException("Test", "id", testId);
        }

        // Load the test
        Test test = fileStorageService.readFromJson(testPath, Test.class);
        
        // Create a map of steps by ID for quick lookup
        Map<String, TestStep> stepsMap = test.getSteps().stream()
                .collect(Collectors.toMap(TestStep::getId, step -> step));
        
        // Validate all step IDs exist
        for (String stepId : stepIds) {
            if (!stepsMap.containsKey(stepId)) {
                throw new ResourceNotFoundException("TestStep", "id", stepId);
            }
        }
        
        // Reorder steps
        List<TestStep> reorderedSteps = new ArrayList<>();
        for (int i = 0; i < stepIds.size(); i++) {
            TestStep step = stepsMap.get(stepIds.get(i));
            step.setOrder(i + 1);
            reorderedSteps.add(step);
        }
        
        test.setSteps(reorderedSteps);
        test.setUpdatedAt(LocalDateTime.now());

        // Save the updated test
        fileStorageService.saveToJson(testPath, test);

        return test;
    }

    /**
     * Apply filter criteria to a test
     * 
     * @param test Test to filter
     * @param filter Filter criteria
     * @return True if test matches the filter
     */
    private boolean applyFilter(Test test, TestFilter filter) {
        // Check search term
        if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
            String search = filter.getSearch().toLowerCase();
            boolean matches = false;
            
            if (test.getName() != null && test.getName().toLowerCase().contains(search)) {
                matches = true;
            } else if (test.getDescription() != null && test.getDescription().toLowerCase().contains(search)) {
                matches = true;
            }
            
            if (!matches) {
                return false;
            }
        }
        
        // Check statuses
        if (filter.getStatuses() != null && !filter.getStatuses().isEmpty()) {
            if (test.getStatus() == null || !filter.getStatuses().contains(test.getStatus())) {
                return false;
            }
        }
        
        // Check types
        if (filter.getTypes() != null && !filter.getTypes().isEmpty()) {
            if (test.getType() == null || !filter.getTypes().contains(test.getType())) {
                return false;
            }
        }
        
        // Check tags
        if (filter.getTags() != null && !filter.getTags().isEmpty()) {
            if (test.getTags() == null) {
                return false;
            }
            
            boolean hasTag = false;
            for (String tag : filter.getTags()) {
                if (test.getTags().contains(tag)) {
                    hasTag = true;
                    break;
                }
            }
            
            if (!hasTag) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Sort a list of tests
     * 
     * @param tests Tests to sort
     * @param sortBy Field to sort by
     * @param sortDirection Sort direction
     */
    private void sortTests(List<Test> tests, String sortBy, String sortDirection) {
        Comparator<Test> comparator;
        
        switch (sortBy) {
            case "name":
                comparator = Comparator.comparing(Test::getName, Comparator.nullsLast(String::compareTo));
                break;
            case "type":
                comparator = Comparator.comparing(test -> test.getType() != null ? test.getType().name() : "", 
                        Comparator.nullsLast(String::compareTo));
                break;
            case "status":
                comparator = Comparator.comparing(test -> test.getStatus() != null ? test.getStatus().name() : "", 
                        Comparator.nullsLast(String::compareTo));
                break;
            case "createdAt":
                comparator = Comparator.comparing(Test::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo));
                break;
            case "updatedAt":
            default:
                comparator = Comparator.comparing(Test::getUpdatedAt, Comparator.nullsLast(LocalDateTime::compareTo));
                break;
        }
        
        if ("asc".equalsIgnoreCase(sortDirection)) {
            tests.sort(comparator);
        } else {
            tests.sort(comparator.reversed());
        }
    }

    /**
     * Update the tags index with new tags
     * 
     * @param projectId Project ID
     * @param tags Tags to add
     */
    private void updateTagsIndex(String projectId, Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        
        String testsTagsPath = String.format(TESTS_TAGS_PATH_TEMPLATE, projectId);
        try (FileLock lock = fileStorageService.lockFile(testsTagsPath)) {
            if (lock != null) {
                Set<String> existingTags = fileStorageService.readFromJson(testsTagsPath, Set.class);
                if (existingTags == null) {
                    existingTags = new HashSet<>();
                }
                existingTags.addAll(tags);
                fileStorageService.saveToJson(testsTagsPath, existingTags);
            } else {
                throw new ConcurrencyException("Tags Index", "projectId", projectId);
            }
        }
    }

    /**
     * Remove tags from the index if they are no longer used
     * 
     * @param projectId Project ID
     * @param tagsToCheck Tags to check
     */
    private void removeUnusedTags(String projectId, Set<String> tagsToCheck) {
        if (tagsToCheck == null || tagsToCheck.isEmpty()) {
            return;
        }
        
        String testsIndexPath = String.format(TESTS_INDEX_PATH_TEMPLATE, projectId);
        Map<String, String> testIndex = fileStorageService.readMapFromJson(
                testsIndexPath, String.class, String.class);
        
        // Load all tests to check if tags are still in use
        Set<String> usedTags = new HashSet<>();
        for (String testId : testIndex.keySet()) {
            String testPath = String.format("projects/%s/tests/%s.json", projectId, testId);
            Test test = fileStorageService.readFromJson(testPath, Test.class);
            if (test != null && test.getTags() != null) {
                usedTags.addAll(test.getTags());
            }
        }
        
        // Remove unused tags
        Set<String> tagsToRemove = new HashSet<>();
        for (String tag : tagsToCheck) {
            if (!usedTags.contains(tag)) {
                tagsToRemove.add(tag);
            }
        }
        
        if (!tagsToRemove.isEmpty()) {
            String testsTagsPath = String.format(TESTS_TAGS_PATH_TEMPLATE, projectId);
            try (FileLock lock = fileStorageService.lockFile(testsTagsPath)) {
                if (lock != null) {
                    Set<String> allTags = fileStorageService.readFromJson(testsTagsPath, Set.class);
                    allTags.removeAll(tagsToRemove);
                    fileStorageService.saveToJson(testsTagsPath, allTags);
                }
            }
        }
    }

    // TestCase implementation methods

    @Override
    public List<TestCase> getTestsForProject(String projectId) {
        // Initialize indexes if they don't exist
        initProjectIndexes(projectId);
        
        // Get all test IDs from the index
        String testsIndexPath = String.format(TESTS_INDEX_PATH_TEMPLATE, projectId);
        Map<String, String> testIndex = fileStorageService.readMapFromJson(
                testsIndexPath, String.class, String.class);

        // Load and convert tests
        List<TestCase> testCases = new ArrayList<>();
        for (String testId : testIndex.keySet()) {
            String testPath = String.format("projects/%s/tests/%s.json", projectId, testId);
            Test test = fileStorageService.readFromJson(testPath, Test.class);
            if (test != null) {
                testCases.add(convertToTestCase(test));
            }
        }

        return testCases;
    }

    @Override
    public Optional<TestCase> getTestById(String projectId, String testId) {
        Optional<Test> testOpt = findById(projectId, testId);
        return testOpt.map(this::convertToTestCase);
    }

    @Override
    public TestCase createTest(String projectId, TestCase testCase) {
        // Convert TestCase to Test
        Test test = convertToTest(testCase);
        // Create using existing method
        Test createdTest = create(projectId, test);
        // Convert back to TestCase
        return convertToTestCase(createdTest);
    }

    @Override
    public TestCase updateTest(String projectId, String testId, TestCase testCase) {
        Test test = convertToTest(testCase);
        Test updatedTest = update(projectId, testId, test);
        return convertToTestCase(updatedTest);
    }

    @Override
    public boolean deleteTest(String projectId, String testId) {
        // Use existing delete method
        return delete(projectId, testId);
    }

    @Override
    public TestCase addTestStep(String projectId, String testId, TestStep step) {
        Test updatedTest = addStep(projectId, testId, step);
        return convertToTestCase(updatedTest);
    }
    
    /**
     * Convert a Test to a TestCase
     * 
     * @param test The Test to convert
     * @return Converted TestCase
     */
    private TestCase convertToTestCase(Test test) {
        TestCase testCase = new TestCase();
        testCase.setId(test.getId());
        testCase.setName(test.getName());
        testCase.setProjectId(test.getProjectId());
        testCase.setDescription(test.getDescription());
        
        // Determine baseUrl from test config if available
        if (test.getConfig() != null && test.getConfig().getFrameworkOptions() != null && 
            test.getConfig().getFrameworkOptions().containsKey("baseUrl")) {
            testCase.setBaseUrl(test.getConfig().getFrameworkOptions().get("baseUrl").toString());
        }
        
        // Convert and copy steps
        if (test.getSteps() != null) {
            List<com.cstestforge.testing.model.TestStep> convertedSteps = new ArrayList<>();
            for (com.cstestforge.project.model.test.TestStep step : test.getSteps()) {
                convertedSteps.add(convertProjectTestStepToTestingTestStep(step));
            }
            testCase.setSteps(convertedSteps);
        }
        
        // Set dates
        testCase.setCreatedDate(test.getCreatedAt());
        testCase.setLastModifiedDate(test.getUpdatedAt());
        
        return testCase;
    }
    
    /**
     * Convert a TestCase to a Test
     * 
     * @param testCase The TestCase to convert
     * @return Converted Test
     */
    private Test convertToTest(TestCase testCase) {
        Test test = new Test();
        test.setId(testCase.getId());
        test.setName(testCase.getName());
        test.setProjectId(testCase.getProjectId());
        test.setDescription(testCase.getDescription());
        
        // Convert and copy steps
        if (testCase.getSteps() != null) {
            List<com.cstestforge.project.model.test.TestStep> convertedSteps = new ArrayList<>();
            for (com.cstestforge.testing.model.TestStep step : testCase.getSteps()) {
                convertedSteps.add(convertTestingTestStepToProjectTestStep(step));
            }
            test.setSteps(convertedSteps);
        }
        
        // Set config with baseUrl
        if (testCase.getBaseUrl() != null) {
            TestConfig config = new TestConfig();
            config.addFrameworkOption("baseUrl", testCase.getBaseUrl());
            test.setConfig(config);
        }
        
        // Status defaults to DRAFT
        test.setStatus(TestStatus.DRAFT);
        
        return test;
    }
    
    /**
     * Convert a project TestStep to a testing TestStep
     * 
     * @param projectStep Project model TestStep
     * @return Testing model TestStep
     */
    private com.cstestforge.testing.model.TestStep convertProjectTestStepToTestingTestStep(com.cstestforge.project.model.test.TestStep projectStep) {
        com.cstestforge.testing.model.TestStep testingStep = new com.cstestforge.testing.model.TestStep();
        
        testingStep.setId(projectStep.getId());
        testingStep.setSequence(projectStep.getOrder());
        
        // Convert step type (mapping between different enum types)
        if (projectStep.getType() != null) {
            switch (projectStep.getType()) {
                case NAVIGATION:
                    testingStep.setType(com.cstestforge.testing.model.TestStep.StepType.NAVIGATE);
                    break;
                case CLICK:
                    testingStep.setType(com.cstestforge.testing.model.TestStep.StepType.CLICK);
                    break;
                case INPUT:
                    testingStep.setType(com.cstestforge.testing.model.TestStep.StepType.TYPE);
                    break;
                case SELECT:
                    testingStep.setType(com.cstestforge.testing.model.TestStep.StepType.SELECT);
                    break;
                case ASSERTION:
                    testingStep.setType(com.cstestforge.testing.model.TestStep.StepType.ASSERT);
                    break;
                case WAIT:
                    testingStep.setType(com.cstestforge.testing.model.TestStep.StepType.WAIT);
                    break;
                case SCREENSHOT:
                    testingStep.setType(com.cstestforge.testing.model.TestStep.StepType.SCREENSHOT);
                    break;
                default:
                    testingStep.setType(com.cstestforge.testing.model.TestStep.StepType.CUSTOM);
                    break;
            }
        }
        
        // Map parameters to target/value fields in TestCase model
        if (projectStep.getParameters() != null) {
            Map<String, Object> params = projectStep.getParameters();
            if (params.containsKey("target")) {
                testingStep.setTarget(params.get("target").toString());
            }
            if (params.containsKey("value")) {
                testingStep.setValue(params.get("value").toString());
            }
            
            // Convert remaining parameters to attributes
            Map<String, String> attributes = new HashMap<>();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (!entry.getKey().equals("target") && !entry.getKey().equals("value")) {
                    attributes.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
                }
            }
            testingStep.setAttributes(attributes);
        }
        
        return testingStep;
    }
    
    /**
     * Convert a testing TestStep to a project TestStep
     * 
     * @param testingStep Testing model TestStep
     * @return Project model TestStep
     */
    private com.cstestforge.project.model.test.TestStep convertTestingTestStepToProjectTestStep(com.cstestforge.testing.model.TestStep testingStep) {
        com.cstestforge.project.model.test.TestStep projectStep = new com.cstestforge.project.model.test.TestStep();
        
        projectStep.setId(testingStep.getId());
        projectStep.setOrder(testingStep.getSequence());
        
        // Convert step type (mapping between different enum types)
        if (testingStep.getType() != null) {
            switch (testingStep.getType()) {
                case NAVIGATE:
                    projectStep.setType(com.cstestforge.project.model.test.TestStepType.NAVIGATION);
                    break;
                case CLICK:
                    projectStep.setType(com.cstestforge.project.model.test.TestStepType.CLICK);
                    break;
                case TYPE:
                    projectStep.setType(com.cstestforge.project.model.test.TestStepType.INPUT);
                    break;
                case SELECT:
                    projectStep.setType(com.cstestforge.project.model.test.TestStepType.SELECT);
                    break;
                case ASSERT:
                    projectStep.setType(com.cstestforge.project.model.test.TestStepType.ASSERTION);
                    break;
                case WAIT:
                    projectStep.setType(com.cstestforge.project.model.test.TestStepType.WAIT);
                    break;
                case SCREENSHOT:
                    projectStep.setType(com.cstestforge.project.model.test.TestStepType.SCREENSHOT);
                    break;
                case CUSTOM:
                default:
                    projectStep.setType(com.cstestforge.project.model.test.TestStepType.CUSTOM_CODE);
                    break;
            }
        }
        
        // Convert target/value fields to parameters in project model
        Map<String, Object> parameters = new HashMap<>();
        
        if (testingStep.getTarget() != null) {
            parameters.put("target", testingStep.getTarget());
        }
        
        if (testingStep.getValue() != null) {
            parameters.put("value", testingStep.getValue());
        }
        
        // Add attributes as additional parameters
        if (testingStep.getAttributes() != null) {
            testingStep.getAttributes().forEach(parameters::put);
        }
        
        projectStep.setParameters(parameters);
        
        return projectStep;
    }
}