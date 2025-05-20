package com.cstestforge.export.controller;

import com.cstestforge.export.model.*;
import com.cstestforge.export.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for export operations
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private ExportService exportService;
    
    /**
     * Get available frameworks
     * 
     * @return List of framework definitions
     */
    @GetMapping("/frameworks")
    public ResponseEntity<List<FrameworkDefinition>> getFrameworks() {
        return ResponseEntity.ok(exportService.getFrameworks());
    }
    
    /**
     * Execute export operation
     * 
     * @param request Export request details
     * @return Export result with status
     */
    @PostMapping("/execute")
    public ResponseEntity<ExportResult> executeExport(@Valid @RequestBody ExportRequest request) {
        return ResponseEntity.ok(exportService.executeExport(request));
    }
    
    /**
     * Get status of an export
     * 
     * @param exportId Export ID
     * @return Export status
     */
    @GetMapping("/status/{exportId}")
    public ResponseEntity<ExportStatus> getExportStatus(@PathVariable String exportId) {
        return ResponseEntity.ok(exportService.getExportStatus(exportId));
    }
    
    /**
     * Download export file
     * 
     * @param exportId Export ID
     * @return File download response
     */
    @GetMapping("/download/{exportId}")
    public ResponseEntity<Resource> downloadExport(@PathVariable String exportId) {
        // Get export file
        Optional<ExportFile> exportFile = exportService.getExportFile(exportId);
        
        if (exportFile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ExportFile file = exportFile.get();
        ByteArrayResource resource = new ByteArrayResource(file.getContent());
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .body(resource);
    }
    
    /**
     * Get export history for a project
     * 
     * @param projectId Project ID
     * @return List of export results
     */
    @GetMapping("/history/{projectId}")
    public ResponseEntity<List<ExportResult>> getExportHistory(@PathVariable String projectId) {
        return ResponseEntity.ok(exportService.getExportHistory(projectId));
    }
} 