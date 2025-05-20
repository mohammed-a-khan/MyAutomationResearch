package com.cstestforge.testing.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a response from an API request execution
 */
public class ApiResponse {
    
    private String id;
    private String requestId;
    private int statusCode;
    private String statusText;
    private Map<String, String> headers;
    private String body;
    private long responseTimeMs;
    private LocalDateTime timestamp;
    private boolean successful;
    private String errorMessage;
    private Map<String, Boolean> assertionResults;
    private String contentType;
    private long contentLength;
    
    /**
     * Default constructor
     */
    public ApiResponse() {
        this.headers = new HashMap<>();
        this.assertionResults = new HashMap<>();
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Get the unique ID of this response
     * 
     * @return Response ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the unique ID of this response
     * 
     * @param id Response ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the request ID that generated this response
     * 
     * @return Request ID
     */
    public String getRequestId() {
        return requestId;
    }
    
    /**
     * Set the request ID that generated this response
     * 
     * @param requestId Request ID
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    /**
     * Get the HTTP status code
     * 
     * @return HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * Set the HTTP status code
     * 
     * @param statusCode HTTP status code
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    
    /**
     * Get the HTTP status text
     * 
     * @return HTTP status text
     */
    public String getStatusText() {
        return statusText;
    }
    
    /**
     * Set the HTTP status text
     * 
     * @param statusText HTTP status text
     */
    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }
    
    /**
     * Get the response headers
     * 
     * @return Map of header names to values
     */
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    /**
     * Set the response headers
     * 
     * @param headers Map of header names to values
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    /**
     * Add a header to the response
     * 
     * @param name Header name
     * @param value Header value
     */
    public void addHeader(String name, String value) {
        if (this.headers == null) {
            this.headers = new HashMap<>();
        }
        this.headers.put(name, value);
    }
    
    /**
     * Get the response body
     * 
     * @return Response body as a string
     */
    public String getBody() {
        return body;
    }
    
    /**
     * Set the response body
     * 
     * @param body Response body as a string
     */
    public void setBody(String body) {
        this.body = body;
    }
    
    /**
     * Get the response time in milliseconds
     * 
     * @return Response time in milliseconds
     */
    public long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    /**
     * Set the response time in milliseconds
     * 
     * @param responseTimeMs Response time in milliseconds
     */
    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
    
    /**
     * Get the timestamp of when this response was received
     * 
     * @return Response timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Set the timestamp of when this response was received
     * 
     * @param timestamp Response timestamp
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Check if the request was successful
     * 
     * @return True if successful, false if an error occurred
     */
    public boolean isSuccessful() {
        return successful;
    }
    
    /**
     * Set whether the request was successful
     * 
     * @param successful True if successful, false if an error occurred
     */
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
    
    /**
     * Get the error message if the request failed
     * 
     * @return Error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Set the error message if the request failed
     * 
     * @param errorMessage Error message
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    /**
     * Get the assertion results for this response
     * 
     * @return Map of assertion IDs to boolean results
     */
    public Map<String, Boolean> getAssertionResults() {
        return assertionResults;
    }
    
    /**
     * Set the assertion results for this response
     * 
     * @param assertionResults Map of assertion IDs to boolean results
     */
    public void setAssertionResults(Map<String, Boolean> assertionResults) {
        this.assertionResults = assertionResults;
    }
    
    /**
     * Add an assertion result
     * 
     * @param assertionId Assertion ID
     * @param passed Whether the assertion passed
     */
    public void addAssertionResult(String assertionId, boolean passed) {
        if (this.assertionResults == null) {
            this.assertionResults = new HashMap<>();
        }
        this.assertionResults.put(assertionId, passed);
    }
    
    /**
     * Get the content type of the response
     * 
     * @return Content type
     */
    public String getContentType() {
        return contentType;
    }
    
    /**
     * Set the content type of the response
     * 
     * @param contentType Content type
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    /**
     * Get the content length of the response
     * 
     * @return Content length in bytes
     */
    public long getContentLength() {
        return contentLength;
    }
    
    /**
     * Set the content length of the response
     * 
     * @param contentLength Content length in bytes
     */
    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }
} 