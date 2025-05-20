package com.cstestforge.recorder.model;

/**
 * Response for recording operations
 */
public class RecordingResponse {
    private String sessionId;
    private String message;
    private boolean success;
    
    public RecordingResponse(String sessionId, String message, boolean success) {
        this.sessionId = sessionId;
        this.message = message;
        this.success = success;
    }
    
    // Getters and setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
} 