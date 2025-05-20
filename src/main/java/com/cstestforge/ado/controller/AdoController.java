package com.cstestforge.ado.controller;

import com.cstestforge.ado.model.*;
import com.cstestforge.ado.service.AdoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for Azure DevOps integration
 */
@RestController
@RequestMapping("/api/ado")
public class AdoController {

    private final AdoService adoService;
    
    @Autowired
    public AdoController(AdoService adoService) {
        this.adoService = adoService;
    }
    
    /**
     * Get all ADO connections
     */
    @GetMapping("/connections")
    public ResponseEntity<List<AdoConnection>> getConnections() {
        List<AdoConnection> connections = adoService.getConnections();
        return ResponseEntity.ok(connections);
    }
    
    /**
     * Get ADO connection by ID
     */
    @GetMapping("/connections/{id}")
    public ResponseEntity<AdoConnection> getConnection(@PathVariable String id) {
        AdoConnection connection = adoService.getConnectionById(id);
        if (connection == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(connection);
    }
    
    /**
     * Create a new ADO connection
     */
    @PostMapping("/connections")
    public ResponseEntity<AdoConnection> createConnection(@RequestBody AdoConnection connection) {
        try {
            AdoConnection createdConnection = adoService.createConnection(connection);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdConnection);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Update an existing ADO connection
     */
    @PutMapping("/connections/{id}")
    public ResponseEntity<AdoConnection> updateConnection(@PathVariable String id, @RequestBody AdoConnection connection) {
        try {
            AdoConnection updatedConnection = adoService.updateConnection(id, connection);
            return ResponseEntity.ok(updatedConnection);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Delete an ADO connection
     */
    @DeleteMapping("/connections/{id}")
    public ResponseEntity<Void> deleteConnection(@PathVariable String id) {
        boolean deleted = adoService.deleteConnection(id);
        if (deleted) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Validate an ADO connection
     */
    @PostMapping("/connections/validate")
    public ResponseEntity<Map<String, Boolean>> validateConnection(@RequestBody Map<String, String> connectionDetails) {
        String url = connectionDetails.get("url");
        String pat = connectionDetails.get("pat");
        String organizationName = connectionDetails.get("organizationName");
        String projectName = connectionDetails.get("projectName");
        
        boolean valid = adoService.validateConnection(url, pat, organizationName, projectName);
        return ResponseEntity.ok(Map.of("valid", valid));
    }
    
    /**
     * Get projects for a connection
     */
    @GetMapping("/connections/{connectionId}/projects")
    public ResponseEntity<List<AdoProject>> getProjects(@PathVariable String connectionId) {
        try {
            List<AdoProject> projects = adoService.getProjects(connectionId);
            return ResponseEntity.ok(projects);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get test plans for a project
     */
    @GetMapping("/connections/{connectionId}/projects/{projectId}/test-plans")
    public ResponseEntity<List<AdoTestPlan>> getTestPlans(@PathVariable String connectionId, @PathVariable String projectId) {
        try {
            List<AdoTestPlan> testPlans = adoService.getTestPlans(connectionId, projectId);
            return ResponseEntity.ok(testPlans);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get test suites for a test plan
     */
    @GetMapping("/connections/{connectionId}/projects/{projectId}/test-plans/{testPlanId}/test-suites")
    public ResponseEntity<List<AdoTestSuite>> getTestSuites(@PathVariable String connectionId, @PathVariable String projectId, @PathVariable String testPlanId) {
        try {
            List<AdoTestSuite> testSuites = adoService.getTestSuites(connectionId, projectId, testPlanId);
            return ResponseEntity.ok(testSuites);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get pipelines for a project
     */
    @GetMapping("/connections/{connectionId}/projects/{projectId}/pipelines")
    public ResponseEntity<List<AdoPipeline>> getPipelines(@PathVariable String connectionId, @PathVariable String projectId) {
        try {
            List<AdoPipeline> pipelines = adoService.getPipelines(connectionId, projectId);
            return ResponseEntity.ok(pipelines);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get synchronization configuration for a project
     */
    @GetMapping("/sync/config/{projectId}")
    public ResponseEntity<SyncConfig> getSyncConfig(@PathVariable String projectId) {
        SyncConfig config = adoService.getSyncConfig(projectId);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }
    
    /**
     * Save synchronization configuration for a project
     */
    @PostMapping("/sync/config/{projectId}")
    public ResponseEntity<SyncConfig> saveSyncConfig(@PathVariable String projectId, @RequestBody SyncConfig config) {
        try {
            SyncConfig savedConfig = adoService.saveSyncConfig(projectId, config);
            return ResponseEntity.ok(savedConfig);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get synchronization status for a project
     */
    @GetMapping("/sync/status/{projectId}")
    public ResponseEntity<SyncStatus> getSyncStatus(@PathVariable String projectId) {
        SyncStatus status = adoService.getSyncStatus(projectId);
        return ResponseEntity.ok(status);
    }
    
    /**
     * Start synchronization for a project
     */
    @PostMapping("/sync/start/{projectId}")
    public ResponseEntity<Map<String, Boolean>> startSync(@PathVariable String projectId) {
        boolean started = adoService.startSync(projectId);
        return ResponseEntity.ok(Map.of("started", started));
    }
    
    /**
     * Get pipeline configuration for a project
     */
    @GetMapping("/pipeline/config/{projectId}")
    public ResponseEntity<PipelineConfig> getPipelineConfig(@PathVariable String projectId) {
        PipelineConfig config = adoService.getPipelineConfig(projectId);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }
    
    /**
     * Save pipeline configuration for a project
     */
    @PostMapping("/pipeline/config/{projectId}")
    public ResponseEntity<PipelineConfig> savePipelineConfig(@PathVariable String projectId, @RequestBody PipelineConfig config) {
        try {
            PipelineConfig savedConfig = adoService.savePipelineConfig(projectId, config);
            return ResponseEntity.ok(savedConfig);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Trigger a pipeline run for a project
     */
    @PostMapping("/pipeline/trigger/{projectId}")
    public ResponseEntity<Map<String, Boolean>> triggerPipeline(@PathVariable String projectId) {
        boolean triggered = adoService.triggerPipeline(projectId);
        return ResponseEntity.ok(Map.of("triggered", triggered));
    }
} 