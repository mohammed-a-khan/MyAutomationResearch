package com.cstestforge.storage.repository;

import com.cstestforge.project.model.test.Test;
import com.cstestforge.project.model.test.TestStatus;
import com.cstestforge.storage.EnhancedFileLock;
import com.cstestforge.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implementation of TestRepository using StorageManager.
 * Tests are stored at "projects/{projectId}/tests/"
 */
@Repository
public class TestRepositoryImpl implements TestRepository {

    private static final Logger logger = LoggerFactory.getLogger(TestRepositoryImpl.class);
    private static final String TESTS_DIRECTORY = "projects/%s/tests";
    private static final String TEST_FILE = "projects/%s/tests/%s/test.json";
    private static final String TEST_INDEX_FILE = "projects/%s/tests/_index.json";
    private static final String TAGS_INDEX_FILE = "projects/%s/tests/_tags.json";

    private final StorageManager storageManager;

    @Autowired
    public TestRepositoryImpl(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    @Override
    public Optional<Test> findById(String projectId, String testId) {
        String testPath = String.format(TEST_FILE, projectId, testId);
        if (!storageManager.exists(testPath)) {
            return Optional.empty();
        }
        
        Test test = storageManager.read(testPath, Test.class);
        return Optional.ofNullable(test);
    }

    @Override
    public List<Test> findAll(String projectId) {
        // Get the test index
        String indexPath = String.format(TEST_INDEX_FILE, projectId);
        if (!storageManager.exists(indexPath)) {
            // Create empty index if it doesn't exist
            createEmptyIndexes(projectId);
            return Collections.emptyList();
        }
        
        // Read test IDs from index
        Map<String, String> testIndex = storageManager.read(indexPath, Map.class);
        if (testIndex == null || testIndex.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Load each test
        List<Test> tests = new ArrayList<>();
        for (String testId : testIndex.keySet()) {
            findById(projectId, testId).ifPresent(tests::add);
        }
        
        // Sort by updated time (most recent first)
        tests.sort((t1, t2) -> {
            if (t1.getUpdatedAt() == null) return 1;
            if (t2.getUpdatedAt() == null) return -1;
            return t2.getUpdatedAt().compareTo(t1.getUpdatedAt());
        });
        
        return tests;
    }

    @Override
    public List<Test> findByFilters(String projectId, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return findAll(projectId);
        }
        
        List<Test> allTests = findAll(projectId);
        
        // Apply filters
        Predicate<Test> predicate = test -> true;
        
        if (filters.containsKey("name")) {
            String nameFilter = filters.get("name").toString().toLowerCase();
            predicate = predicate.and(test -> 
                test.getName() != null && 
                test.getName().toLowerCase().contains(nameFilter));
        }
        
        if (filters.containsKey("status")) {
            TestStatus statusFilter = (TestStatus) filters.get("status");
            predicate = predicate.and(test -> test.getStatus() == statusFilter);
        }
        
        if (filters.containsKey("tags")) {
            @SuppressWarnings("unchecked")
            List<String> tagFilters = (List<String>) filters.get("tags");
            boolean matchAll = filters.containsKey("matchAllTags") && 
                               (boolean) filters.get("matchAllTags");
            
            if (matchAll) {
                predicate = predicate.and(test -> 
                    test.getTags() != null && 
                    test.getTags().containsAll(tagFilters));
            } else {
                predicate = predicate.and(test -> 
                    test.getTags() != null && 
                    tagFilters.stream().anyMatch(test.getTags()::contains));
            }
        }
        
        if (filters.containsKey("type")) {
            String typeFilter = filters.get("type").toString();
            predicate = predicate.and(test -> 
                test.getType() != null && 
                test.getType().toString().equals(typeFilter));
        }
        
        if (filters.containsKey("createdAfter")) {
            LocalDateTime dateFilter = (LocalDateTime) filters.get("createdAfter");
            predicate = predicate.and(test -> 
                test.getCreatedAt() != null && 
                test.getCreatedAt().isAfter(dateFilter));
        }
        
        if (filters.containsKey("updatedAfter")) {
            LocalDateTime dateFilter = (LocalDateTime) filters.get("updatedAfter");
            predicate = predicate.and(test -> 
                test.getUpdatedAt() != null && 
                test.getUpdatedAt().isAfter(dateFilter));
        }
        
        return allTests.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    @Override
    public Test create(String projectId, Test test) {
        // Ensure test has an ID
        if (test.getId() == null || test.getId().isEmpty()) {
            test.setId(UUID.randomUUID().toString());
        }
        
        // Set project ID
        test.setProjectId(projectId);
        
        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        test.setCreatedAt(now);
        test.setUpdatedAt(now);
        
        // Set initial version
        test.setVersion(1);
        
        // Set default status if not set
        if (test.getStatus() == null) {
            test.setStatus(TestStatus.DRAFT);
        }
        
        // Ensure directories exist
        String testsDir = String.format(TESTS_DIRECTORY, projectId);
        String testDir = testsDir + "/" + test.getId();
        storageManager.createDirectory(testDir);
        
        // Save the test
        String testPath = String.format(TEST_FILE, projectId, test.getId());
        storageManager.write(testPath, test);
        
        // Update indexes in transaction
        updateIndexes(projectId, test, null);
        
        return test;
    }

    @Override
    public Test update(String projectId, String testId, Test test) {
        // Verify test exists
        Optional<Test> existingTest = findById(projectId, testId);
        if (!existingTest.isPresent()) {
            throw new IllegalArgumentException("Test not found with ID: " + testId);
        }
        
        Test original = existingTest.get();
        
        // Update fields but preserve certain original values
        test.setId(testId); // Ensure ID doesn't change
        test.setProjectId(projectId); // Ensure project ID doesn't change
        test.setCreatedAt(original.getCreatedAt()); // Preserve creation time
        test.setCreatedBy(original.getCreatedBy()); // Preserve creator
        test.setUpdatedAt(LocalDateTime.now()); // Set updated time to now
        test.setVersion(original.getVersion() + 1); // Increment version
        
        // Save updated test
        String testPath = String.format(TEST_FILE, projectId, testId);
        storageManager.write(testPath, test);
        
        // Update indexes with changed tags
        updateIndexes(projectId, test, original);
        
        return test;
    }

    @Override
    public boolean delete(String projectId, String testId) {
        // Verify test exists
        Optional<Test> existingTest = findById(projectId, testId);
        if (!existingTest.isPresent()) {
            return false; // Nothing to delete
        }
        
        Test test = existingTest.get();
        
        // Delete test directory and contents
        String testDir = String.format(TESTS_DIRECTORY, projectId) + "/" + testId;
        boolean deleted = storageManager.delete(testDir);
        
        // If directory deleted successfully, update indexes
        if (deleted) {
            // Remove from indexes
            removeFromIndexes(projectId, test);
        }
        
        return deleted;
    }

    @Override
    public List<Test> findByTags(String projectId, List<String> tags, boolean matchAll) {
        if (tags == null || tags.isEmpty()) {
            return findAll(projectId);
        }
        
        // Get tests using the tag index
        String tagsIndexPath = String.format(TAGS_INDEX_FILE, projectId);
        if (!storageManager.exists(tagsIndexPath)) {
            return Collections.emptyList();
        }
        
        Map<String, Set<String>> tagsIndex = storageManager.read(tagsIndexPath, Map.class);
        if (tagsIndex == null || tagsIndex.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Find test IDs matching the tags
        Set<String> matchingTestIds;
        
        if (matchAll) {
            // Find tests that have ALL the specified tags
            matchingTestIds = null;
            
            for (String tag : tags) {
                Set<String> testsWithTag = tagsIndex.get(tag.toLowerCase());
                
                if (testsWithTag == null || testsWithTag.isEmpty()) {
                    // If any tag has no tests, the result is empty
                    return Collections.emptyList();
                }
                
                if (matchingTestIds == null) {
                    matchingTestIds = new HashSet<>(testsWithTag);
                } else {
                    // Intersect with previous results
                    matchingTestIds.retainAll(testsWithTag);
                }
                
                if (matchingTestIds.isEmpty()) {
                    // If intersection becomes empty, exit early
                    return Collections.emptyList();
                }
            }
        } else {
            // Find tests that have ANY of the specified tags
            matchingTestIds = new HashSet<>();
            
            for (String tag : tags) {
                Set<String> testsWithTag = tagsIndex.get(tag.toLowerCase());
                if (testsWithTag != null) {
                    matchingTestIds.addAll(testsWithTag);
                }
            }
        }
        
        if (matchingTestIds == null || matchingTestIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Load the matching tests
        List<Test> matchingTests = new ArrayList<>();
        for (String testId : matchingTestIds) {
            findById(projectId, testId).ifPresent(matchingTests::add);
        }
        
        // Sort by updated time (most recent first)
        matchingTests.sort((t1, t2) -> {
            if (t1.getUpdatedAt() == null) return 1;
            if (t2.getUpdatedAt() == null) return -1;
            return t2.getUpdatedAt().compareTo(t1.getUpdatedAt());
        });
        
        return matchingTests;
    }

    @Override
    public int count(String projectId) {
        String indexPath = String.format(TEST_INDEX_FILE, projectId);
        if (!storageManager.exists(indexPath)) {
            return 0;
        }
        
        Map<String, String> testIndex = storageManager.read(indexPath, Map.class);
        return testIndex != null ? testIndex.size() : 0;
    }

    /**
     * Create empty index files for a new project
     * 
     * @param projectId Project ID
     */
    private void createEmptyIndexes(String projectId) {
        String indexPath = String.format(TEST_INDEX_FILE, projectId);
        String tagsIndexPath = String.format(TAGS_INDEX_FILE, projectId);
        
        if (!storageManager.exists(indexPath)) {
            storageManager.write(indexPath, new HashMap<String, String>());
        }
        
        if (!storageManager.exists(tagsIndexPath)) {
            storageManager.write(tagsIndexPath, new HashMap<String, Set<String>>());
        }
    }

    /**
     * Update index files when a test is created or updated
     * 
     * @param projectId Project ID
     * @param test Updated test
     * @param originalTest Original test (null for new tests)
     */
    private void updateIndexes(String projectId, Test test, Test originalTest) {
        final String indexPath = String.format(TEST_INDEX_FILE, projectId);
        final String tagsIndexPath = String.format(TAGS_INDEX_FILE, projectId);
        
        List<String> pathsToLock = Arrays.asList(indexPath, tagsIndexPath);
        
        storageManager.executeInTransaction(pathsToLock, unused -> {
            // Update test index
            Map<String, String> testIndex = storageManager.exists(indexPath) 
                ? storageManager.read(indexPath, Map.class) 
                : new HashMap<>();
            
            if (testIndex == null) {
                testIndex = new HashMap<>();
            }
            
            testIndex.put(test.getId(), test.getName());
            storageManager.write(indexPath, testIndex);
            
            // Update tags index
            Map<String, Set<String>> tagsIndex = storageManager.exists(tagsIndexPath)
                ? storageManager.read(tagsIndexPath, Map.class)
                : new HashMap<>();
            
            if (tagsIndex == null) {
                tagsIndex = new HashMap<>();
            }
            
            // Remove old tags
            if (originalTest != null && originalTest.getTags() != null) {
                for (String tag : originalTest.getTags()) {
                    String tagLower = tag.toLowerCase();
                    if (tagsIndex.containsKey(tagLower)) {
                        tagsIndex.get(tagLower).remove(test.getId());
                        
                        // Remove tag entry if empty
                        if (tagsIndex.get(tagLower).isEmpty()) {
                            tagsIndex.remove(tagLower);
                        }
                    }
                }
            }
            
            // Add new tags
            if (test.getTags() != null) {
                for (String tag : test.getTags()) {
                    String tagLower = tag.toLowerCase();
                    if (!tagsIndex.containsKey(tagLower)) {
                        tagsIndex.put(tagLower, new HashSet<>());
                    }
                    tagsIndex.get(tagLower).add(test.getId());
                }
            }
            
            storageManager.write(tagsIndexPath, tagsIndex);
            
            return null;
        });
    }

    /**
     * Remove a test from index files when deleted
     * 
     * @param projectId Project ID
     * @param test Test to remove
     */
    private void removeFromIndexes(String projectId, Test test) {
        final String indexPath = String.format(TEST_INDEX_FILE, projectId);
        final String tagsIndexPath = String.format(TAGS_INDEX_FILE, projectId);
        
        List<String> pathsToLock = Arrays.asList(indexPath, tagsIndexPath);
        
        storageManager.executeInTransaction(pathsToLock, unused -> {
            // Update test index
            if (storageManager.exists(indexPath)) {
                Map<String, String> testIndex = storageManager.read(indexPath, Map.class);
                if (testIndex != null) {
                    testIndex.remove(test.getId());
                    storageManager.write(indexPath, testIndex);
                }
            }
            
            // Update tags index
            if (storageManager.exists(tagsIndexPath) && test.getTags() != null) {
                Map<String, Set<String>> tagsIndex = storageManager.read(tagsIndexPath, Map.class);
                
                if (tagsIndex != null) {
                    for (String tag : test.getTags()) {
                        String tagLower = tag.toLowerCase();
                        if (tagsIndex.containsKey(tagLower)) {
                            tagsIndex.get(tagLower).remove(test.getId());
                            
                            // Remove tag entry if empty
                            if (tagsIndex.get(tagLower).isEmpty()) {
                                tagsIndex.remove(tagLower);
                            }
                        }
                    }
                    
                    storageManager.write(tagsIndexPath, tagsIndex);
                }
            }
            
            return null;
        });
    }
} 