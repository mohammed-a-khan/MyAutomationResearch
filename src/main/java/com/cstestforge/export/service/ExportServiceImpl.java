package com.cstestforge.export.service;

import com.cstestforge.export.model.*;
import com.cstestforge.export.service.framework.PlaywrightTypeScriptExporter;
import com.cstestforge.export.service.framework.SeleniumJavaExporter;
import com.cstestforge.project.service.ProjectService;
import com.cstestforge.testing.service.TestCaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExportServiceImpl implements ExportService {
    private static final Logger logger = LoggerFactory.getLogger(ExportServiceImpl.class);
    
    private final Map<String, ExportResult> exportResults = new ConcurrentHashMap<>();
    private final Map<String, ExportFile> exportFiles = new ConcurrentHashMap<>();
    
    @Autowired
    private ProjectService projectService;
    
        @Autowired    private TestCaseService testCaseService;
    
    @Autowired
    private SeleniumJavaExporter seleniumJavaExporter;
    
    @Autowired
    private PlaywrightTypeScriptExporter playwrightTypeScriptExporter;
    
    // In-memory framework registry
    private final List<FrameworkDefinition> supportedFrameworks = new ArrayList<>();
    
    public ExportServiceImpl() {
        // Initialize supported frameworks
        supportedFrameworks.add(FrameworkDefinition.createSeleniumJava());
        supportedFrameworks.add(FrameworkDefinition.createPlaywrightTypeScript());
    }
    
    @Override
    public List<FrameworkDefinition> getFrameworks() {
        return Collections.unmodifiableList(supportedFrameworks);
    }
    
    @Override
    public ExportResult executeExport(ExportRequest request) {
        logger.info("Starting export for project {}, framework: {}, language: {}", 
                request.getProjectId(), request.getFramework(), request.getLanguage());
        
        // Validate request
        if (request.getProjectId() == null || request.getTestIds() == null || request.getTestIds().isEmpty()) {
            throw new IllegalArgumentException("Project ID and at least one test ID must be provided");
        }
        
        // Generate unique export ID
        String exportId = UUID.randomUUID().toString();
        
        // Create pending export result
        ExportResult result = ExportResult.createPending(
                exportId, 
                request.getProjectId(), 
                request.getFramework(), 
                request.getLanguage(), 
                request.getBuildTool(), 
                request.getTestIds().size()
        );
        
        // Store result
        exportResults.put(exportId, result);
        
        // Start async export process
        new Thread(() -> processExport(exportId, request)).start();
        
        return result;
    }
    
    @Override
    public Optional<ExportFile> getExportFile(String exportId) {
        return Optional.ofNullable(exportFiles.get(exportId));
    }
    
    @Override
    public ExportStatus getExportStatus(String exportId) {
        ExportResult result = exportResults.get(exportId);
        return result != null ? result.getStatus() : ExportStatus.FAILED;
    }
    
    @Override
    public List<ExportResult> getExportHistory(String projectId) {
        return exportResults.values().stream()
                .filter(result -> result.getProjectId().equals(projectId))
                .sorted(Comparator.comparing(ExportResult::getCreateTime).reversed())
                .toList();
    }
    
    /**
     * Process the export request asynchronously
     * 
     * @param exportId Export ID
     * @param request Export request details
     */
    private void processExport(String exportId, ExportRequest request) {
        ExportResult result = exportResults.get(exportId);
        
        try {
            // Update status to IN_PROGRESS
            result.updateStatus(ExportStatus.IN_PROGRESS);
            
            // Generate export package based on framework+language
            String frameworkId = request.getFrameworkIdentifier();
            byte[] exportPackage;
            
            switch (frameworkId) {
                case "selenium-java":
                    exportPackage = seleniumJavaExporter.createExportPackage(request);
                    break;
                case "playwright-typescript":
                    exportPackage = playwrightTypeScriptExporter.createExportPackage(request);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported framework: " + frameworkId);
            }
            
            // Create export file
            String fileName = generateExportFileName(request);
            ExportFile exportFile = new ExportFile(
                    exportId,
                    fileName,
                    "application/zip",
                    exportPackage
            );
            
            // Store export file
            exportFiles.put(exportId, exportFile);
            
            // Mark export as completed
            String downloadUrl = "/api/export/download/" + exportId;
            result.markCompleted(downloadUrl);
            
            logger.info("Export completed successfully for project {}, exportId: {}", 
                    request.getProjectId(), exportId);
            
        } catch (Exception e) {
            logger.error("Export failed for project {}, exportId: {}", 
                    request.getProjectId(), exportId, e);
            result.markFailed(e.getMessage());
        }
    }
    
    /**
     * Generate export file name
     * 
     * @param request Export request
     * @return File name
     */
            private String generateExportFileName(ExportRequest request) {        return String.format("export_%s_%s_%s_%s.zip",                request.getProjectId(),                request.getFramework(),                request.getLanguage(),                LocalDateTime.now().toString().replace(":", "-").replace(".", "-"));    }
} 