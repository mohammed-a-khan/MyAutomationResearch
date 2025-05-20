package com.cstestforge.export.model;

import java.time.LocalDateTime;

/**
 * Result model for test export operations
 */
public class ExportResult {
    private String exportId;
    private String projectId;
    private ExportStatus status;
    private String downloadUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String framework;
    private String language;
    private String buildTool;
    private int testCount;
    private String errorMessage;
    
    /**
     * Default constructor
     */
    public ExportResult() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.status = ExportStatus.PENDING;
    }
    
    /**
     * Create a new ExportResult with PENDING status
     * 
     * @param exportId The unique export ID
     * @param projectId The project ID
     * @param framework The framework used for export
     * @param language The programming language
     * @param buildTool The build tool used
     * @param testCount Number of tests being exported
     * @return A new ExportResult instance
     */
    public static ExportResult createPending(String exportId, String projectId, String framework, 
                                            String language, String buildTool, int testCount) {
        ExportResult result = new ExportResult();
        result.exportId = exportId;
        result.projectId = projectId;
        result.framework = framework;
        result.language = language;
        result.buildTool = buildTool;
        result.testCount = testCount;
        result.status = ExportStatus.PENDING;
        return result;
    }
    
    /**
     * Update the status of this result
     * 
     * @param status The new status
     */
    public void updateStatus(ExportStatus status) {
        this.status = status;
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * Mark this export as completed
     * 
     * @param downloadUrl URL for downloading the export package
     */
    public void markCompleted(String downloadUrl) {
        this.status = ExportStatus.COMPLETED;
        this.downloadUrl = downloadUrl;
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * Mark this export as failed
     * 
     * @param errorMessage The error message
     */
    public void markFailed(String errorMessage) {
        this.status = ExportStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updateTime = LocalDateTime.now();
    }

    // Getters and setters
    public String getExportId() {
        return exportId;
    }

    public void setExportId(String exportId) {
        this.exportId = exportId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public ExportStatus getStatus() {
        return status;
    }

    public void setStatus(ExportStatus status) {
        this.status = status;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getBuildTool() {
        return buildTool;
    }

    public void setBuildTool(String buildTool) {
        this.buildTool = buildTool;
    }

    public int getTestCount() {
        return testCount;
    }

    public void setTestCount(int testCount) {
        this.testCount = testCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
} 