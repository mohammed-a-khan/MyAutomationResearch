package com.cstestforge.testing.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an API request that can be executed and tested
 */
public class ApiRequest {
    
    private String id;
    private String projectId;
    private String name;
    private String description;
    private String method;
    private String url;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private String body;
    private String bodyType;
    private List<ApiAssertion> assertions;
    private List<ApiVariable> variables;
    private long createdAt;
    private long updatedAt;
    
    /**
     * Default constructor
     */
    public ApiRequest() {
        this.headers = new HashMap<>();
        this.queryParams = new HashMap<>();
        this.assertions = new ArrayList<>();
        this.variables = new ArrayList<>();
        this.bodyType = "none";
    }
    
    /**
     * Get the unique ID of this API request
     * 
     * @return API request ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the unique ID of this API request
     * 
     * @param id API request ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the project ID this API request belongs to
     * 
     * @return Project ID
     */
    public String getProjectId() {
        return projectId;
    }
    
    /**
     * Set the project ID this API request belongs to
     * 
     * @param projectId Project ID
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    /**
     * Get the name of this API request
     * 
     * @return API request name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the name of this API request
     * 
     * @param name API request name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the description of this API request
     * 
     * @return API request description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set the description of this API request
     * 
     * @param description API request description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Get the HTTP method for this API request
     * 
     * @return HTTP method
     */
    public String getMethod() {
        return method;
    }
    
    /**
     * Set the HTTP method for this API request
     * 
     * @param method HTTP method
     */
    public void setMethod(String method) {
        this.method = method;
    }
    
    /**
     * Get the URL for this API request
     * 
     * @return API request URL
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * Set the URL for this API request
     * 
     * @param url API request URL
     */
    public void setUrl(String url) {
        this.url = url;
    }
    
    /**
     * Get the request headers
     * 
     * @return Map of header names to values
     */
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    /**
     * Set the request headers
     * 
     * @param headers Map of header names to values
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    /**
     * Get the query parameters
     * 
     * @return Map of query parameter names to values
     */
    public Map<String, String> getQueryParams() {
        return queryParams;
    }
    
    /**
     * Set the query parameters
     * 
     * @param queryParams Map of query parameter names to values
     */
    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }
    
    /**
     * Get the request body
     * 
     * @return Request body as a string
     */
    public String getBody() {
        return body;
    }
    
    /**
     * Set the request body
     * 
     * @param body Request body as a string
     */
    public void setBody(String body) {
        this.body = body;
    }
    
    /**
     * Get the body content type
     * 
     * @return Body content type
     */
    public String getBodyType() {
        return bodyType;
    }
    
    /**
     * Set the body content type
     * 
     * @param bodyType Body content type
     */
    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }
    
    /**
     * Get the assertions for this API request
     * 
     * @return List of assertions
     */
    public List<ApiAssertion> getAssertions() {
        return assertions;
    }
    
    /**
     * Set the assertions for this API request
     * 
     * @param assertions List of assertions
     */
    public void setAssertions(List<ApiAssertion> assertions) {
        this.assertions = assertions;
    }
    
    /**
     * Get the variables for this API request
     * 
     * @return List of variables
     */
    public List<ApiVariable> getVariables() {
        return variables;
    }
    
    /**
     * Set the variables for this API request
     * 
     * @param variables List of variables
     */
    public void setVariables(List<ApiVariable> variables) {
        this.variables = variables;
    }
    
    /**
     * Get the creation timestamp
     * 
     * @return Creation timestamp in milliseconds since epoch
     */
    public long getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Set the creation timestamp
     * 
     * @param createdAt Creation timestamp in milliseconds since epoch
     */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Get the last update timestamp
     * 
     * @return Last update timestamp in milliseconds since epoch
     */
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * Set the last update timestamp
     * 
     * @param updatedAt Last update timestamp in milliseconds since epoch
     */
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
} 