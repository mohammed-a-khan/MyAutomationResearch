package com.cstestforge.recorder.model.config;

import java.util.Objects;
import java.util.UUID;

/**
 * Configuration for data sources used in data-driven testing.
 * Supports different types of data sources like CSV, JSON, Excel, etc.
 */
public class DataSourceConfig {
    
    private String id;
    private String name;
    private DataSourceType type;
    private String path;
    private String content;
    private boolean isExternal;
    private SourceFormat format;
    private String delimiter;
    private boolean hasHeader;
    private String sheetName;
    
    /**
     * Default constructor
     */
    public DataSourceConfig() {
        this.id = UUID.randomUUID().toString();
        this.format = SourceFormat.JSON;
        this.delimiter = ",";
        this.hasHeader = true;
    }
    
    /**
     * Constructor for embedded data source
     *
     * @param name Name of the data source
     * @param type Type of the data source
     * @param content Content of the data source
     * @param format Format of the content
     */
    public DataSourceConfig(String name, DataSourceType type, String content, SourceFormat format) {
        this();
        this.name = name;
        this.type = type;
        this.content = content;
        this.format = format;
        this.isExternal = false;
    }
    
    /**
     * Constructor for external data source
     *
     * @param name Name of the data source
     * @param type Type of the data source
     * @param path Path to the data source
     * @param format Format of the content
     */
    public DataSourceConfig(String name, DataSourceType type, String path, SourceFormat format, boolean isExternal) {
        this();
        this.name = name;
        this.type = type;
        this.path = path;
        this.format = format;
        this.isExternal = isExternal;
    }
    
    /**
     * Get the data source ID
     *
     * @return The data source ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the data source ID
     *
     * @param id The data source ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the name of the data source
     *
     * @return The name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the name of the data source
     *
     * @param name The name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the type of the data source
     *
     * @return The type
     */
    public DataSourceType getType() {
        return type;
    }
    
    /**
     * Set the type of the data source
     *
     * @param type The type
     */
    public void setType(DataSourceType type) {
        this.type = type;
    }
    
    /**
     * Get the path to the data source
     *
     * @return The path
     */
    public String getPath() {
        return path;
    }
    
    /**
     * Set the path to the data source
     *
     * @param path The path
     */
    public void setPath(String path) {
        this.path = path;
    }
    
    /**
     * Get the content of the data source
     *
     * @return The content
     */
    public String getContent() {
        return content;
    }
    
    /**
     * Set the content of the data source
     *
     * @param content The content
     */
    public void setContent(String content) {
        this.content = content;
    }
    
    /**
     * Check if the data source is external
     *
     * @return True if external, false if embedded
     */
    public boolean isExternal() {
        return isExternal;
    }
    
    /**
     * Set whether the data source is external
     *
     * @param external True if external, false if embedded
     */
    public void setExternal(boolean external) {
        isExternal = external;
    }
    
    /**
     * Get the format of the data source
     *
     * @return The format
     */
    public SourceFormat getFormat() {
        return format;
    }
    
    /**
     * Set the format of the data source
     *
     * @param format The format
     */
    public void setFormat(SourceFormat format) {
        this.format = format;
    }
    
    /**
     * Get the delimiter for CSV files
     *
     * @return The delimiter
     */
    public String getDelimiter() {
        return delimiter;
    }
    
    /**
     * Set the delimiter for CSV files
     *
     * @param delimiter The delimiter
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }
    
    /**
     * Check if the CSV file has a header row
     *
     * @return True if has header, false otherwise
     */
    public boolean isHasHeader() {
        return hasHeader;
    }
    
    /**
     * Set whether the CSV file has a header row
     *
     * @param hasHeader True if has header, false otherwise
     */
    public void setHasHeader(boolean hasHeader) {
        this.hasHeader = hasHeader;
    }
    
    /**
     * Get the sheet name for Excel files
     *
     * @return The sheet name
     */
    public String getSheetName() {
        return sheetName;
    }
    
    /**
     * Set the sheet name for Excel files
     *
     * @param sheetName The sheet name
     */
    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }
    
    /**
     * Validate the data source configuration
     *
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public void validate() throws IllegalArgumentException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Data source name is required");
        }
        
        if (type == null) {
            throw new IllegalArgumentException("Data source type is required");
        }
        
        if (isExternal) {
            if (path == null || path.trim().isEmpty()) {
                throw new IllegalArgumentException("Path is required for external data sources");
            }
        } else {
            if (content == null || content.trim().isEmpty()) {
                throw new IllegalArgumentException("Content is required for embedded data sources");
            }
        }
        
        if (format == SourceFormat.CSV) {
            if (delimiter == null || delimiter.trim().isEmpty()) {
                throw new IllegalArgumentException("Delimiter is required for CSV format");
            }
        }
        
        if (format == SourceFormat.EXCEL) {
            if (sheetName == null || sheetName.trim().isEmpty()) {
                throw new IllegalArgumentException("Sheet name is required for Excel format");
            }
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSourceConfig that = (DataSourceConfig) o;
        return isExternal == that.isExternal &&
               hasHeader == that.hasHeader &&
               Objects.equals(id, that.id) &&
               Objects.equals(name, that.name) &&
               type == that.type &&
               Objects.equals(path, that.path) &&
               format == that.format;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, name, type, path, isExternal, format, hasHeader);
    }
    
    /**
     * Format of the data source
     */
    public enum SourceFormat {
        CSV,
        JSON,
        XML,
        EXCEL,
        DATABASE,
        API
    }
    
    /**
     * Type of the data source
     */
    public enum DataSourceType {
        EMBEDDED,    // Data is embedded in the recording
        FILE,        // Data is stored in a file
        DATABASE,    // Data is stored in a database
        API,         // Data is fetched from an API
        VARIABLE     // Data is stored in a test variable
    }
} 