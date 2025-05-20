package com.cstestforge.recorder.model;

/**
 * Compatibility adapter for LoopEvent
 * This class extends the actual LoopEvent from the events package
 * to maintain backward compatibility with code that imports from the model package
 */
public class LoopEvent extends com.cstestforge.recorder.model.events.LoopEvent {
    // This class inherits all methods from the parent class
    
    /**
     * Default constructor
     */
    public LoopEvent() {
        super();
    }
    
    /**
     * Constructor with URL and loop config
     */
    public LoopEvent(String url, LoopConfig loopConfig) {
        super(url, loopConfig);
    }
} 