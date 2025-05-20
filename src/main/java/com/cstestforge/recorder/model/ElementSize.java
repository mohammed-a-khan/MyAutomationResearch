package com.cstestforge.recorder.model;

/**
 * Represents the size dimensions of an element on a web page.
 * Contains the width and height of the element.
 */
public class ElementSize {
    
    private int width;
    private int height;
    
    /**
     * Default constructor
     */
    public ElementSize() {
        this.width = 0;
        this.height = 0;
    }
    
    /**
     * Constructor with dimensions
     *
     * @param width The width of the element
     * @param height The height of the element
     */
    public ElementSize(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    /**
     * Get the width
     *
     * @return The width
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Set the width
     *
     * @param width The width
     */
    public void setWidth(int width) {
        this.width = width;
    }
    
    /**
     * Get the height
     *
     * @return The height
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Set the height
     *
     * @param height The height
     */
    public void setHeight(int height) {
        this.height = height;
    }
    
    /**
     * Calculates the area of the element
     *
     * @return The area (width * height)
     */
    public int getArea() {
        return width * height;
    }
    
    /**
     * Creates a copy of this size
     *
     * @return A new ElementSize with the same dimensions
     */
    public ElementSize copy() {
        return new ElementSize(this.width, this.height);
    }
    
    @Override
    public String toString() {
        return "ElementSize{" +
                "width=" + width +
                ", height=" + height +
                '}';
    }
} 