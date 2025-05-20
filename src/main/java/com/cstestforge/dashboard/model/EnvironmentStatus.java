package com.cstestforge.dashboard.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * Model class representing environment status for the dashboard
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnvironmentStatus {
    
    private String name;
    private String status; // "online", "offline", "degraded", "maintenance"
    private String url;
    private LocalDateTime lastChecked;
    private long responseTime; // in milliseconds
    private String version;
    private String description;
    
    /**
     * Default constructor
     */
    public EnvironmentStatus() {
    }
    
    /**
     * Constructor with required parameters
     * 
     * @param name Environment name
     * @param status Environment status
     * @param url Environment URL
     * @param lastChecked Time of last health check
     */
    public EnvironmentStatus(String name, String status, String url, LocalDateTime lastChecked) {
        this.name = name;
        this.status = status;
        this.url = url;
        this.lastChecked = lastChecked;
    }
    
    /**
     * Get the environment name
     * @return Environment name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the environment name
     * @param name Environment name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the environment status
     * @return Status (online, offline, degraded, maintenance)
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * Set the environment status
     * @param status Status (online, offline, degraded, maintenance)
     */
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * Get the environment URL
     * @return Environment URL
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * Set the environment URL
     * @param url Environment URL
     */
    public void setUrl(String url) {
        this.url = url;
    }
    
    /**
     * Get the last checked timestamp
     * @return Last checked timestamp
     */
    public LocalDateTime getLastChecked() {
        return lastChecked;
    }
    
    /**
     * Set the last checked timestamp
     * @param lastChecked Last checked timestamp
     */
    public void setLastChecked(LocalDateTime lastChecked) {
        this.lastChecked = lastChecked;
    }
    
    /**
     * Get the response time in milliseconds
     * @return Response time in milliseconds
     */
    public long getResponseTime() {
        return responseTime;
    }
    
    /**
     * Set the response time in milliseconds
     * @param responseTime Response time in milliseconds
     */
    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }
    
    /**
     * Get the environment version
     * @return Environment version
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Set the environment version
     * @param version Environment version
     */
    public void setVersion(String version) {
        this.version = version;
    }
    
    /**
     * Get the environment description
     * @return Environment description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set the environment description
     * @param description Environment description
     */
    public void setDescription(String description) {
        this.description = description;
    }
} 