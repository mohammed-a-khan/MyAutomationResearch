package com.cstestforge.export.model;

import java.time.LocalDateTime;

/**
 * Represents an exported test package file
 */
public class ExportFile {
    private String exportId;
    private String fileName;
    private String contentType;
    private byte[] content;
    private LocalDateTime createTime;
    private long fileSize;
    
    /**
     * Default constructor
     */
    public ExportFile() {
        this.createTime = LocalDateTime.now();
    }
    
    /**
     * Constructor with all required fields
     * 
     * @param exportId ID of the export operation
     * @param fileName Name of the generated file
     * @param contentType MIME type of the file
     * @param content Binary content of the file
     */
    public ExportFile(String exportId, String fileName, String contentType, byte[] content) {
        this.exportId = exportId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.content = content;
        this.createTime = LocalDateTime.now();
        this.fileSize = content != null ? content.length : 0;
    }
    
    // Getters and setters
    public String getExportId() {
        return exportId;
    }

    public void setExportId(String exportId) {
        this.exportId = exportId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
        this.fileSize = content != null ? content.length : 0;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    /**
     * Get a readable file size string
     * 
     * @return Formatted file size (e.g. "1.2 MB")
     */
    public String getReadableFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }
} 