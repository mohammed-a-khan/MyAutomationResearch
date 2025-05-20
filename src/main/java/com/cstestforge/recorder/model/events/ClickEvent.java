package com.cstestforge.recorder.model.events;

import com.cstestforge.recorder.model.ElementInfo;
import com.cstestforge.recorder.model.RecordedEvent;
import com.cstestforge.recorder.model.RecordedEventType;

/**
 * Represents a click event recorded during a browser session.
 */
public class ClickEvent extends RecordedEvent {
    
    private ElementInfo targetElement;
    private boolean isDoubleClick;
    private boolean isRightClick;
    private boolean isMiddleClick;
    private boolean ctrlKey;
    private boolean shiftKey;
    private boolean altKey;
    private boolean metaKey;
    
    /**
     * Default constructor
     */
    public ClickEvent() {
        super(RecordedEventType.CLICK);
    }
    
    /**
     * Constructor with target element
     *
     * @param targetElement The element that was clicked
     */
    public ClickEvent(ElementInfo targetElement) {
        super(RecordedEventType.CLICK);
        this.targetElement = targetElement;
    }
    
    /**
     * Get the target element
     *
     * @return The target element
     */
    public ElementInfo getTargetElement() {
        return targetElement;
    }
    
    /**
     * Set the target element
     *
     * @param targetElement The target element
     */
    public void setTargetElement(ElementInfo targetElement) {
        this.targetElement = targetElement;
    }
    
    /**
     * Check if this is a double click
     *
     * @return True if this is a double click
     */
    public boolean isDoubleClick() {
        return isDoubleClick;
    }
    
    /**
     * Set whether this is a double click
     *
     * @param doubleClick True if this is a double click
     */
    public void setDoubleClick(boolean doubleClick) {
        isDoubleClick = doubleClick;
        
        // Update event type if this is a double click
        if (doubleClick) {
            setType(RecordedEventType.DOUBLE_CLICK);
        } else {
            setType(RecordedEventType.CLICK);
        }
    }
    
    /**
     * Check if this is a right click
     *
     * @return True if this is a right click
     */
    public boolean isRightClick() {
        return isRightClick;
    }
    
    /**
     * Set whether this is a right click
     *
     * @param rightClick True if this is a right click
     */
    public void setRightClick(boolean rightClick) {
        isRightClick = rightClick;
        
        // Update event type if this is a right click
        if (rightClick) {
            setType(RecordedEventType.RIGHT_CLICK);
        } else if (!isDoubleClick) {
            setType(RecordedEventType.CLICK);
        }
    }
    
    /**
     * Check if this is a middle click
     *
     * @return True if this is a middle click
     */
    public boolean isMiddleClick() {
        return isMiddleClick;
    }
    
    /**
     * Set whether this is a middle click
     *
     * @param middleClick True if this is a middle click
     */
    public void setMiddleClick(boolean middleClick) {
        isMiddleClick = middleClick;
    }
    
    /**
     * Check if ctrl key was pressed during the click
     *
     * @return True if ctrl key was pressed
     */
    public boolean isCtrlKey() {
        return ctrlKey;
    }
    
    /**
     * Set whether ctrl key was pressed during the click
     *
     * @param ctrlKey True if ctrl key was pressed
     */
    public void setCtrlKey(boolean ctrlKey) {
        this.ctrlKey = ctrlKey;
    }
    
    /**
     * Check if shift key was pressed during the click
     *
     * @return True if shift key was pressed
     */
    public boolean isShiftKey() {
        return shiftKey;
    }
    
    /**
     * Set whether shift key was pressed during the click
     *
     * @param shiftKey True if shift key was pressed
     */
    public void setShiftKey(boolean shiftKey) {
        this.shiftKey = shiftKey;
    }
    
    /**
     * Check if alt key was pressed during the click
     *
     * @return True if alt key was pressed
     */
    public boolean isAltKey() {
        return altKey;
    }
    
    /**
     * Set whether alt key was pressed during the click
     *
     * @param altKey True if alt key was pressed
     */
    public void setAltKey(boolean altKey) {
        this.altKey = altKey;
    }
    
    /**
     * Check if meta key was pressed during the click
     *
     * @return True if meta key was pressed
     */
    public boolean isMetaKey() {
        return metaKey;
    }
    
    /**
     * Set whether meta key was pressed during the click
     *
     * @param metaKey True if meta key was pressed
     */
    public void setMetaKey(boolean metaKey) {
        this.metaKey = metaKey;
    }
    
    @Override
    public boolean isValid() {
        return targetElement != null && targetElement.getBestSelector() != null;
    }
    
    @Override
    public String toHumanReadableDescription() {
        String clickType = isDoubleClick ? "Double click" : (isRightClick ? "Right click" : "Click");
        String element = targetElement != null ? targetElement.getBestSelector() : "unknown element";
        String modifiers = "";
        
        if (ctrlKey) modifiers += "Ctrl+";
        if (shiftKey) modifiers += "Shift+";
        if (altKey) modifiers += "Alt+";
        if (metaKey) modifiers += "Meta+";
        
        return modifiers.isEmpty() 
                ? String.format("%s on %s", clickType, element)
                : String.format("%s%s on %s", modifiers, clickType, element);
    }
} 