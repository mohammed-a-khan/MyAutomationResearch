package com.cstestforge.recorder.model;

/**
 * Represents the viewport dimensions for browser recording.
 */
public class Viewport {
    
    private int width;
    private int height;
    private boolean isResponsive;
    private String device; // For simulating specific devices
    
    /**
     * Default constructor
     */
    public Viewport() {
        this.width = 1366;
        this.height = 768;
        this.isResponsive = false;
    }
    
    /**
     * Constructor with dimensions
     * 
     * @param width Width in pixels
     * @param height Height in pixels
     */
    public Viewport(int width, int height) {
        this.width = width;
        this.height = height;
        this.isResponsive = false;
    }
    
    /**
     * Get the viewport width
     * 
     * @return Width in pixels
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Set the viewport width
     * 
     * @param width Width in pixels
     */
    public void setWidth(int width) {
        this.width = width;
    }
    
    /**
     * Get the viewport height
     * 
     * @return Height in pixels
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Set the viewport height
     * 
     * @param height Height in pixels
     */
    public void setHeight(int height) {
        this.height = height;
    }
    
    /**
     * Check if viewport is responsive
     * 
     * @return True if viewport is responsive
     */
    public boolean isResponsive() {
        return isResponsive;
    }
    
    /**
     * Set whether viewport is responsive
     * 
     * @param responsive True if viewport should be responsive
     */
    public void setResponsive(boolean responsive) {
        isResponsive = responsive;
    }
    
    /**
     * Get the device name for simulation
     * 
     * @return Device name
     */
    public String getDevice() {
        return device;
    }
    
    /**
     * Set the device name for simulation
     * 
     * @param device Device name
     */
    public void setDevice(String device) {
        this.device = device;
    }
} 