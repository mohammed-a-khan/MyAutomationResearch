package com.cstestforge.export.service.framework;

import com.cstestforge.export.model.ExportRequest;
import com.cstestforge.export.model.ProjectTemplate;

import java.io.IOException;
import java.util.Map;

/**
 * Interface for framework-specific exporters
 */
public interface FrameworkExporter {
    
    /**
     * Initialize project template for export
     * 
     * @param request Export request
     * @return Project template with directories and configuration
     */
    ProjectTemplate initializeTemplate(ExportRequest request);
    
    /**
     * Generate all test files and supporting code
     * 
     * @param request Export request
     * @return Map of file paths to file contents
     * @throws IOException if an error occurs during generation
     */
    Map<String, String> generateFiles(ExportRequest request) throws IOException;
    
    /**
     * Create a package with all files for export
     * 
     * @param request Export request
     * @return Byte array containing the export package
     * @throws IOException if an error occurs during packaging
     */
    byte[] createExportPackage(ExportRequest request) throws IOException;
} 