package com.cstestforge.testing.controller;

import com.cstestforge.project.model.ApiResponse;
import com.cstestforge.testing.model.TestSuite;
import com.cstestforge.testing.service.TestSuiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for test suite operations.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/test-suites")
public class TestSuiteController {

    private final TestSuiteService testSuiteService;

    @Autowired
    public TestSuiteController(TestSuiteService testSuiteService) {
        this.testSuiteService = testSuiteService;
    }

    /**
     * Get all test suites for a project
     * 
     * @param projectId Project ID
     * @return List of test suites
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TestSuite>>> getAllTestSuites(@PathVariable String projectId) {
        try {
            List<TestSuite> suites = testSuiteService.findAllByProject(projectId);
            return ResponseEntity.ok(ApiResponse.success(suites));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving test suites", e.getMessage()));
        }
    }

    /**
     * Get a test suite by ID
     * 
     * @param projectId Project ID
     * @param id Test suite ID
     * @return Test suite if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TestSuite>> getTestSuiteById(
            @PathVariable String projectId, 
            @PathVariable String id) {
        try {
            return testSuiteService.findById(projectId, id)
                    .map(suite -> ResponseEntity.ok(ApiResponse.success(suite)))
                    .orElse(ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("Test suite not found", "No test suite found with ID: " + id)));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving test suite", e.getMessage()));
        }
    }

    /**
     * Create a new test suite
     * 
     * @param projectId Project ID
     * @param suite Test suite to create
     * @return Created test suite
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TestSuite>> createTestSuite(
            @PathVariable String projectId, 
            @RequestBody TestSuite suite) {
        try {
            TestSuite createdSuite = testSuiteService.create(projectId, suite);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(createdSuite, "Test suite created successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error creating test suite", e.getMessage()));
        }
    }

    /**
     * Update an existing test suite
     * 
     * @param projectId Project ID
     * @param id Test suite ID
     * @param suite Updated test suite data
     * @return Updated test suite
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TestSuite>> updateTestSuite(
            @PathVariable String projectId, 
            @PathVariable String id, 
            @RequestBody TestSuite suite) {
        try {
            TestSuite updatedSuite = testSuiteService.update(projectId, id, suite);
            return ResponseEntity.ok(ApiResponse.success(updatedSuite, "Test suite updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Test suite not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error updating test suite", e.getMessage()));
        }
    }

    /**
     * Delete a test suite
     * 
     * @param projectId Project ID
     * @param id Test suite ID
     * @return Success message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTestSuite(
            @PathVariable String projectId, 
            @PathVariable String id) {
        try {
            boolean deleted = testSuiteService.delete(projectId, id);
            
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success(null, "Test suite deleted successfully"));
            } else {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Test suite not found", "No test suite found with ID: " + id));
            }
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error deleting test suite", e.getMessage()));
        }
    }

    /**
     * Add a test to a test suite
     * 
     * @param projectId Project ID
     * @param id Test suite ID
     * @param testId Test ID
     * @return Updated test suite
     */
    @PostMapping("/{id}/tests/{testId}")
    public ResponseEntity<ApiResponse<TestSuite>> addTestToSuite(
            @PathVariable String projectId,
            @PathVariable String id,
            @PathVariable String testId) {
        try {
            TestSuite updatedSuite = testSuiteService.addTest(projectId, id, testId);
            return ResponseEntity.ok(ApiResponse.success(updatedSuite, "Test added to suite successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Test suite or test not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error adding test to suite", e.getMessage()));
        }
    }

    /**
     * Remove a test from a test suite
     * 
     * @param projectId Project ID
     * @param id Test suite ID
     * @param testId Test ID
     * @return Updated test suite
     */
    @DeleteMapping("/{id}/tests/{testId}")
    public ResponseEntity<ApiResponse<TestSuite>> removeTestFromSuite(
            @PathVariable String projectId,
            @PathVariable String id,
            @PathVariable String testId) {
        try {
            TestSuite updatedSuite = testSuiteService.removeTest(projectId, id, testId);
            return ResponseEntity.ok(ApiResponse.success(updatedSuite, "Test removed from suite successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Test suite not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error removing test from suite", e.getMessage()));
        }
    }

    /**
     * Add a child suite to a parent suite
     * 
     * @param projectId Project ID
     * @param parentId Parent suite ID
     * @param childId Child suite ID
     * @return Updated parent suite
     */
    @PostMapping("/{parentId}/children/{childId}")
    public ResponseEntity<ApiResponse<TestSuite>> addChildSuite(
            @PathVariable String projectId,
            @PathVariable String parentId,
            @PathVariable String childId) {
        try {
            TestSuite updatedSuite = testSuiteService.addChildSuite(projectId, parentId, childId);
            return ResponseEntity.ok(ApiResponse.success(updatedSuite, "Child suite added successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid operation", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error adding child suite", e.getMessage()));
        }
    }

    /**
     * Remove a child suite from a parent suite
     * 
     * @param projectId Project ID
     * @param parentId Parent suite ID
     * @param childId Child suite ID
     * @return Updated parent suite
     */
    @DeleteMapping("/{parentId}/children/{childId}")
    public ResponseEntity<ApiResponse<TestSuite>> removeChildSuite(
            @PathVariable String projectId,
            @PathVariable String parentId,
            @PathVariable String childId) {
        try {
            TestSuite updatedSuite = testSuiteService.removeChildSuite(projectId, parentId, childId);
            return ResponseEntity.ok(ApiResponse.success(updatedSuite, "Child suite removed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Test suite not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error removing child suite", e.getMessage()));
        }
    }
} 