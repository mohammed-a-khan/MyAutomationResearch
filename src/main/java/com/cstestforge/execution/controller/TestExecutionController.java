package com.cstestforge.execution.controller;

import com.cstestforge.execution.model.TestExecutionInfo;
import com.cstestforge.execution.model.TestExecutionRequest;
import com.cstestforge.execution.model.TestStatus;
import com.cstestforge.execution.service.TestExecutionService;
import com.cstestforge.project.model.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for test execution operations
 */
@RestController
@RequestMapping("/api/execution")
public class TestExecutionController {

    private static final Logger logger = LoggerFactory.getLogger(TestExecutionController.class);
    
    private final TestExecutionService executionService;

    @Autowired
    public TestExecutionController(TestExecutionService executionService) {
        this.executionService = executionService;
    }

    /**
     * Run tests
     * 
     * @param request Test execution request
     * @return Execution info
     */
    @PostMapping("/run")
    public ResponseEntity<ApiResponse<TestExecutionInfo>> runTests(@RequestBody TestExecutionRequest request) {
        try {
            logger.info("Received request to run {} tests for project {}", 
                    request.getTestIds().size(), request.getProjectId());
            
            // Validate request
            if (request.getProjectId() == null || request.getProjectId().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid request", "Project ID is required"));
            }
            
            if (request.getTestIds() == null || request.getTestIds().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid request", "At least one test ID is required"));
            }
            
            if (request.getConfig() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid request", "Execution configuration is required"));
            }
            
            // Run tests
            TestExecutionInfo executionInfo = executionService.runTests(request);
            
            return ResponseEntity.ok(ApiResponse.success(executionInfo, "Test execution started successfully"));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request", e);
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error("Invalid request", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error running tests", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error running tests", e.getMessage()));
        }
    }

    /**
     * Get execution status
     * 
     * @param executionId Execution ID
     * @return Execution info
     */
    @GetMapping("/{executionId}/status")
    public ResponseEntity<ApiResponse<TestExecutionInfo>> getExecutionStatus(@PathVariable String executionId) {
        try {
            TestExecutionInfo executionInfo = executionService.getExecutionStatus(executionId);
            return ResponseEntity.ok(ApiResponse.success(executionInfo));
        } catch (IllegalArgumentException e) {
            logger.error("Execution not found", e);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Execution not found", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error retrieving execution status", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving execution status", e.getMessage()));
        }
    }

    /**
     * Get execution details with test results
     * 
     * @param executionId Execution ID
     * @return Execution info with test results
     */
    @GetMapping("/{executionId}/details")
    public ResponseEntity<ApiResponse<TestExecutionInfo>> getExecutionDetails(@PathVariable String executionId) {
        try {
            TestExecutionInfo executionInfo = executionService.getExecutionDetails(executionId);
            return ResponseEntity.ok(ApiResponse.success(executionInfo));
        } catch (IllegalArgumentException e) {
            logger.error("Execution not found", e);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Execution not found", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error retrieving execution details", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving execution details", e.getMessage()));
        }
    }

    /**
     * Stop an execution
     * 
     * @param executionId Execution ID
     * @return Updated execution info
     */
    @PostMapping("/{executionId}/stop")
    public ResponseEntity<ApiResponse<TestExecutionInfo>> stopExecution(@PathVariable String executionId) {
        try {
            TestExecutionInfo executionInfo = executionService.stopExecution(executionId);
            return ResponseEntity.ok(ApiResponse.success(executionInfo, "Execution stopped successfully"));
        } catch (IllegalArgumentException e) {
            logger.error("Execution not found", e);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Execution not found", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error stopping execution", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error stopping execution", e.getMessage()));
        }
    }

    /**
     * Delete an execution
     * 
     * @param executionId Execution ID
     * @return Success message
     */
    @DeleteMapping("/{executionId}")
    public ResponseEntity<ApiResponse<Void>> deleteExecution(@PathVariable String executionId) {
        try {
            boolean deleted = executionService.deleteExecution(executionId);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success(null, "Execution deleted successfully"));
            } else {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Execution not found", "No execution found with ID: " + executionId));
            }
        } catch (Exception e) {
            logger.error("Error deleting execution", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error deleting execution", e.getMessage()));
        }
    }

    /**
     * Get execution history for a project
     * 
     * @param projectId Project ID
     * @param limit Maximum number of executions to return
     * @param offset Offset for pagination
     * @return List of executions
     */
    @GetMapping("/history/{projectId}")
    public ResponseEntity<ApiResponse<List<TestExecutionInfo>>> getExecutionHistory(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        try {
            List<TestExecutionInfo> executions = executionService.getExecutionHistory(projectId, limit, offset);
            return ResponseEntity.ok(ApiResponse.success(executions));
        } catch (Exception e) {
            logger.error("Error retrieving execution history", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving execution history", e.getMessage()));
        }
    }

    /**
     * Get execution history for a specific test
     * 
     * @param projectId Project ID
     * @param testId Test ID
     * @param limit Maximum number of executions to return
     * @return List of executions for the test
     */
    @GetMapping("/history/{projectId}/test/{testId}")
    public ResponseEntity<ApiResponse<List<TestExecutionInfo>>> getTestExecutionHistory(
            @PathVariable String projectId,
            @PathVariable String testId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<TestExecutionInfo> executions = executionService.getTestExecutionHistory(projectId, testId, limit);
            return ResponseEntity.ok(ApiResponse.success(executions));
        } catch (Exception e) {
            logger.error("Error retrieving test execution history", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving test execution history", e.getMessage()));
        }
    }

    /**
     * Get the latest execution for a test
     * 
     * @param projectId Project ID
     * @param testId Test ID
     * @return Latest execution info
     */
    @GetMapping("/latest/{projectId}/test/{testId}")
    public ResponseEntity<ApiResponse<TestExecutionInfo>> getLatestTestExecution(
            @PathVariable String projectId,
            @PathVariable String testId) {
        try {
            return executionService.getLatestTestExecution(projectId, testId)
                    .map(execution -> ResponseEntity.ok(ApiResponse.success(execution)))
                    .orElse(ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("No executions found", "No executions found for test: " + testId)));
        } catch (Exception e) {
            logger.error("Error retrieving latest test execution", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving latest test execution", e.getMessage()));
        }
    }

    /**
     * Get executions by status
     * 
     * @param projectId Project ID
     * @param status Status to filter by
     * @param limit Maximum number of executions to return
     * @return List of executions with the specified status
     */
    @GetMapping("/byStatus/{projectId}")
    public ResponseEntity<ApiResponse<List<TestExecutionInfo>>> getExecutionsByStatus(
            @PathVariable String projectId,
            @RequestParam TestStatus status,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<TestExecutionInfo> executions = executionService.getExecutionsByStatus(projectId, status, limit);
            return ResponseEntity.ok(ApiResponse.success(executions));
        } catch (Exception e) {
            logger.error("Error retrieving executions by status", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving executions by status", e.getMessage()));
        }
    }

    /**
     * Clean up old executions
     * 
     * @param projectId Project ID
     * @param daysToKeep Number of days to keep executions for
     * @return Number of executions deleted
     */
    @DeleteMapping("/cleanup/{projectId}")
    public ResponseEntity<ApiResponse<Integer>> cleanupOldExecutions(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "30") int daysToKeep) {
        try {
            int deletedCount = executionService.cleanupOldExecutions(projectId, daysToKeep);
            return ResponseEntity.ok(ApiResponse.success(deletedCount, 
                    String.format("Deleted %d executions older than %d days", deletedCount, daysToKeep)));
        } catch (Exception e) {
            logger.error("Error cleaning up old executions", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error cleaning up old executions", e.getMessage()));
        }
    }
} 