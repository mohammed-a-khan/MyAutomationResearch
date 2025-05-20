package com.cstestforge.project.model;

/**
 * Standard API response format for all endpoints.
 * 
 * @param <T> Type of data being returned
 */
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private Object error;

    public ApiResponse() {
        this.success = true;
    }

    public ApiResponse(T data) {
        this.success = true;
        this.data = data;
        this.message = "Operation successful";
    }

    public ApiResponse(T data, String message) {
        this.success = true;
        this.data = data;
        this.message = message;
    }

    public ApiResponse(String message, Object error) {
        this.success = false;
        this.message = message;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(data, message);
    }

    public static <T> ApiResponse<T> error(String message, Object error) {
        return new ApiResponse<>(message, error);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getError() {
        return error;
    }

    public void setError(Object error) {
        this.error = error;
    }
} 