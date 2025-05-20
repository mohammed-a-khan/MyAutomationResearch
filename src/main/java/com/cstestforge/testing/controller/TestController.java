package com.cstestforge.testing.controller;

import com.cstestforge.project.model.ApiResponse;
import com.cstestforge.project.model.PagedResponse;
import com.cstestforge.project.model.test.Test;
import com.cstestforge.project.model.test.TestStep;
import com.cstestforge.project.model.test.TestStatus;
import com.cstestforge.project.model.test.TestType;
import com.cstestforge.testing.model.TestFilter;
import com.cstestforge.testing.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * REST controller for test operations.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/tests")
public class TestController {

    private final TestService testService;

    @Autowired
    public TestController(TestService testService) {
        this.testService = testService;
    }

    /**
     * Get all tests with filtering, pagination and sorting
     * 
     * @param projectId Project ID
     * @param search Search term
     * @param statuses List of test statuses
     * @param types List of test types
     * @param tags List of tags
     * @param sortBy Sort field
     * @param sortDirection Sort direction
     * @param page Page number
     * @param size Page size
     * @return Paged response of tests
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<Test>>> getAllTests(
            @PathVariable String projectId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String[] statuses,
            @RequestParam(required = false) String[] types,
            @RequestParam(required = false) String[] tags,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        
        try {
            // Create filter from request parameters
            TestFilter filter = new TestFilter();
            filter.setSearch(search);
            filter.setPage(page);
            filter.setSize(size);
            filter.setSortBy(sortBy);
            filter.setSortDirection(sortDirection);
            
            // Convert string arrays to enums if provided
            if (statuses != null && statuses.length > 0) {
                filter.setStatuses(java.util.Arrays.stream(statuses)
                        .map(s -> TestStatus.valueOf(s))
                        .collect(java.util.stream.Collectors.toList()));
            }
            
            if (types != null && types.length > 0) {
                filter.setTypes(java.util.Arrays.stream(types)
                        .map(t -> TestType.valueOf(t))
                        .collect(java.util.stream.Collectors.toList()));
            }
            
            if (tags != null && tags.length > 0) {
                filter.setTags(java.util.Arrays.asList(tags));
            }
            
            PagedResponse<Test> response = testService.findAll(projectId, filter);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving tests", e.getMessage()));
        }
    }

    /**
     * Get a test by ID
     * 
     * @param projectId Project ID
     * @param id Test ID
     * @return Test if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Test>> getTestById(
            @PathVariable String projectId, 
            @PathVariable String id) {
        try {
            return testService.findById(projectId, id)
                    .map(test -> ResponseEntity.ok(ApiResponse.success(test)))
                    .orElse(ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("Test not found", "No test found with ID: " + id)));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving test", e.getMessage()));
        }
    }

    /**
     * Create a new test
     * 
     * @param projectId Project ID
     * @param test Test to create
     * @return Created test
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Test>> createTest(
            @PathVariable String projectId, 
            @RequestBody Test test) {
        try {
            Test createdTest = testService.create(projectId, test);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(createdTest, "Test created successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error creating test", e.getMessage()));
        }
    }

    /**
     * Update an existing test
     * 
     * @param projectId Project ID
     * @param id Test ID
     * @param test Updated test data
     * @return Updated test
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Test>> updateTest(
            @PathVariable String projectId, 
            @PathVariable String id, 
            @RequestBody Test test) {
        try {
            Test updatedTest = testService.update(projectId, id, test);
            return ResponseEntity.ok(ApiResponse.success(updatedTest, "Test updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Test not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error updating test", e.getMessage()));
        }
    }

    /**
     * Delete a test
     * 
     * @param projectId Project ID
     * @param id Test ID
     * @return Success message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTest(
            @PathVariable String projectId, 
            @PathVariable String id) {
        try {
            boolean deleted = testService.delete(projectId, id);
            
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success(null, "Test deleted successfully"));
            } else {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Test not found", "No test found with ID: " + id));
            }
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error deleting test", e.getMessage()));
        }
    }

    /**
     * Duplicate a test
     * 
     * @param projectId Project ID
     * @param id Test ID
     * @return Duplicated test
     */
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<ApiResponse<Test>> duplicateTest(
            @PathVariable String projectId, 
            @PathVariable String id) {
        try {
            Test duplicatedTest = testService.duplicate(projectId, id);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(duplicatedTest, "Test duplicated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Test not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error duplicating test", e.getMessage()));
        }
    }

    /**
     * Reorder steps in a test
     * 
     * @param projectId Project ID
     * @param id Test ID
     * @param stepIds List of step IDs in the new order
     * @return Updated test
     */
    @PutMapping("/{id}/steps/reorder")
    public ResponseEntity<ApiResponse<Test>> reorderTestSteps(
            @PathVariable String projectId,
            @PathVariable String id,
            @RequestBody List<String> stepIds) {
        try {
            Test updatedTest = testService.reorderSteps(projectId, id, stepIds);
            return ResponseEntity.ok(ApiResponse.success(updatedTest, "Test steps reordered successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Test or step not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error reordering test steps", e.getMessage()));
        }
    }

    /**
     * Add a step to a test
     * 
     * @param projectId Project ID
     * @param id Test ID
     * @param step Step to add
     * @return Updated test
     */
    @PostMapping("/{id}/steps")
    public ResponseEntity<ApiResponse<Test>> addTestStep(
            @PathVariable String projectId,
            @PathVariable String id,
            @RequestBody TestStep step) {
        try {
            Test updatedTest = testService.addStep(projectId, id, step);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(updatedTest, "Test step added successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Test not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error adding test step", e.getMessage()));
        }
    }

    /**
     * Update a step in a test
     * 
     * @param projectId Project ID
     * @param id Test ID
     * @param stepId Step ID
     * @param step Updated step data
     * @return Updated test
     */
    @PutMapping("/{id}/steps/{stepId}")
    public ResponseEntity<ApiResponse<Test>> updateTestStep(
            @PathVariable String projectId,
            @PathVariable String id,
            @PathVariable String stepId,
            @RequestBody TestStep step) {
        try {
            Test updatedTest = testService.updateStep(projectId, id, stepId, step);
            return ResponseEntity.ok(ApiResponse.success(updatedTest, "Test step updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Test or step not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error updating test step", e.getMessage()));
        }
    }

    /**
     * Delete a step from a test
     * 
     * @param projectId Project ID
     * @param id Test ID
     * @param stepId Step ID
     * @return Updated test
     */
    @DeleteMapping("/{id}/steps/{stepId}")
    public ResponseEntity<ApiResponse<Test>> deleteTestStep(
            @PathVariable String projectId,
            @PathVariable String id,
            @PathVariable String stepId) {
        try {
            Test updatedTest = testService.deleteStep(projectId, id, stepId);
            return ResponseEntity.ok(ApiResponse.success(updatedTest, "Test step deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Test or step not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error deleting test step", e.getMessage()));
        }
    }

    /**
     * Get all unique tags
     * 
     * @param projectId Project ID
     * @return Set of all tags
     */
    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<Set<String>>> getAllTags(
            @PathVariable String projectId) {
        try {
            Set<String> tags = testService.getAllTags(projectId);
            return ResponseEntity.ok(ApiResponse.success(tags));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving tags", e.getMessage()));
        }
    }

    /**
     * Add a tag to a test
     * 
     * @param projectId Project ID
     * @param id Test ID
     * @param tag Tag to add
     * @return Updated test
     */
    @PostMapping("/{id}/tags/{tag}")
    public ResponseEntity<ApiResponse<Test>> addTag(
            @PathVariable String projectId,
            @PathVariable String id,
            @PathVariable String tag) {
        try {
            Test updatedTest = testService.addTag(projectId, id, tag);
            return ResponseEntity.ok(ApiResponse.success(updatedTest, "Tag added successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Test not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error adding tag", e.getMessage()));
        }
    }

    /**
     * Remove a tag from a test
     * 
     * @param projectId Project ID
     * @param id Test ID
     * @param tag Tag to remove
     * @return Updated test
     */
    @DeleteMapping("/{id}/tags/{tag}")
    public ResponseEntity<ApiResponse<Test>> removeTag(
            @PathVariable String projectId,
            @PathVariable String id,
            @PathVariable String tag) {
        try {
            Test updatedTest = testService.removeTag(projectId, id, tag);
            return ResponseEntity.ok(ApiResponse.success(updatedTest, "Tag removed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Test not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error removing tag", e.getMessage()));
        }
    }
} 