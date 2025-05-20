package com.cstestforge.recorder.model;

/**
 * Represents the location of an element on a web page.
 * Contains the x and y coordinates relative to the viewport.
 */
public class ElementLocation {
    
    private int x;
    private int y;
    
    /**
     * Default constructor
     */
    public ElementLocation() {
        this.x = 0;
        this.y = 0;
    }
    
    /**
     * Constructor with coordinates
     *
     * @param x The x-coordinate
     * @param y The y-coordinate
     */
    public ElementLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Get the x-coordinate
     *
     * @return The x-coordinate
     */
    public int getX() {
        return x;
    }
    
    /**
     * Set the x-coordinate
     *
     * @param x The x-coordinate
     */
    public void setX(int x) {
        this.x = x;
    }
    
    /**
     * Get the y-coordinate
     *
     * @return The y-coordinate
     */
    public int getY() {
        return y;
    }
    
    /**
     * Set the y-coordinate
     *
     * @param y The y-coordinate
     */
    public void setY(int y) {
        this.y = y;
    }
    
    /**
     * Creates a copy of this location
     *
     * @return A new ElementLocation with the same coordinates
     */
    public ElementLocation copy() {
        return new ElementLocation(this.x, this.y);
    }
    
    @Override
    public String toString() {
        return "ElementLocation{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
} 