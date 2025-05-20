package com.cstestforge.export.service;

import com.cstestforge.export.model.*;
import java.util.List;
import java.util.Optional;

/**
 * Service for exporting tests to different frameworks
 */
public interface ExportService {
    
    /**
     * Get list of available frameworks
     * 
     * @return List of framework definitions
     */
    List<FrameworkDefinition> getFrameworks();
    
    /**
     * Execute export operation
     * 
     * @param request Export request with project and test details
     * @return Export result with status and download information
     */
    ExportResult executeExport(ExportRequest request);
    
    /**
     * Get export file by ID
     * 
     * @param exportId Export operation ID
     * @return Optional containing the export file if found
     */
    Optional<ExportFile> getExportFile(String exportId);
    
    /**
     * Get status of export operation
     * 
     * @param exportId Export operation ID
     * @return Status of the export
     */
    ExportStatus getExportStatus(String exportId);
    
    /**
     * Get export history for a project
     * 
     * @param projectId Project ID
     * @return List of export results
     */
    List<ExportResult> getExportHistory(String projectId);
} 