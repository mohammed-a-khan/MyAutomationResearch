package com.cstestforge.recorder.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents information about a DOM element
 */
public class ElementInfo {
    
    private String selector;
    private String xpath;
    private String cssSelector;
    private String id;
    private String tagName;
    private String className;
    private String name;
    private String text;
    private String href;
    private String src;
    private String alt;
    private String title;
    private String placeholder;
    private String value;
    private String type;
    private String friendlyName;
    private Map<String, String> attributes;
    private ElementLocation location;
    private ElementSize size;
    private boolean isVisible;
    private boolean isEnabled;
    private boolean isSelected;
    private boolean isRequired;
    private Rectangle boundingRect;
    
    /**
     * Default constructor
     */
    public ElementInfo() {
        this.attributes = new HashMap<>();
        this.location = new ElementLocation();
        this.size = new ElementSize();
    }
    
    /**
     * Get the element selector
     * 
     * @return Element selector
     */
    public String getSelector() {
        return selector;
    }
    
    /**
     * Set the element selector
     * 
     * @param selector Element selector
     */
    public void setSelector(String selector) {
        this.selector = selector;
    }
    
    /**
     * Get the XPath
     * 
     * @return XPath
     */
    public String getXpath() {
        return xpath;
    }
    
    /**
     * Set the XPath
     * 
     * @param xpath XPath
     */
    public void setXpath(String xpath) {
        this.xpath = xpath;
    }
    
    /**
     * Get the CSS selector
     * 
     * @return CSS selector
     */
    public String getCssSelector() {
        return cssSelector;
    }
    
    /**
     * Set the CSS selector
     * 
     * @param cssSelector CSS selector
     */
    public void setCssSelector(String cssSelector) {
        this.cssSelector = cssSelector;
    }
    
    /**
     * Get the element ID
     * 
     * @return Element ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the element ID
     * 
     * @param id Element ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the tag name
     * 
     * @return Tag name
     */
    public String getTagName() {
        return tagName;
    }
    
    /**
     * Set the tag name
     * 
     * @param tagName Tag name
     */
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
    
    /**
     * Get the class name
     * 
     * @return Class name
     */
    public String getClassName() {
        return className;
    }
    
    /**
     * Set the class name
     * 
     * @param className Class name
     */
    public void setClassName(String className) {
        this.className = className;
    }
    
    /**
     * Get the name attribute
     * 
     * @return Name attribute
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the name attribute
     * 
     * @param name Name attribute
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the element text
     * 
     * @return Element text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Set the element text
     * 
     * @param text Element text
     */
    public void setText(String text) {
        this.text = text;
    }
    
    /**
     * Get the href attribute
     * 
     * @return Href attribute
     */
    public String getHref() {
        return href;
    }
    
    /**
     * Set the href attribute
     * 
     * @param href Href attribute
     */
    public void setHref(String href) {
        this.href = href;
    }
    
    /**
     * Get the src attribute
     * 
     * @return Src attribute
     */
    public String getSrc() {
        return src;
    }
    
    /**
     * Set the src attribute
     * 
     * @param src Src attribute
     */
    public void setSrc(String src) {
        this.src = src;
    }
    
    /**
     * Get the alt attribute
     * 
     * @return Alt attribute
     */
    public String getAlt() {
        return alt;
    }
    
    /**
     * Set the alt attribute
     * 
     * @param alt Alt attribute
     */
    public void setAlt(String alt) {
        this.alt = alt;
    }
    
    /**
     * Get the title attribute
     * 
     * @return Title attribute
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Set the title attribute
     * 
     * @param title Title attribute
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    /**
     * Get the placeholder attribute
     * 
     * @return Placeholder attribute
     */
    public String getPlaceholder() {
        return placeholder;
    }
    
    /**
     * Set the placeholder attribute
     * 
     * @param placeholder Placeholder attribute
     */
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }
    
    /**
     * Get the value attribute
     * 
     * @return Value attribute
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Set the value attribute
     * 
     * @param value Value attribute
     */
    public void setValue(String value) {
        this.value = value;
    }
    
    /**
     * Get the type attribute
     * 
     * @return Type attribute
     */
    public String getType() {
        return type;
    }
    
    /**
     * Set the type attribute
     * 
     * @param type Type attribute
     */
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * Get all attributes
     * 
     * @return Map of attributes
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }
    
    /**
     * Set all attributes
     * 
     * @param attributes Map of attributes
     */
    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
    
    /**
     * Get the element location
     * 
     * @return Element location
     */
    public ElementLocation getLocation() {
        return location;
    }
    
    /**
     * Set the element location
     * 
     * @param location Element location
     */
    public void setLocation(ElementLocation location) {
        this.location = location;
    }
    
    /**
     * Get the element size
     * 
     * @return Element size
     */
    public ElementSize getSize() {
        return size;
    }
    
    /**
     * Set the element size
     * 
     * @param size Element size
     */
    public void setSize(ElementSize size) {
        this.size = size;
    }
    
    /**
     * Check if the element is visible
     * 
     * @return True if the element is visible
     */
    public boolean isVisible() {
        return isVisible;
    }
    
    /**
     * Set whether the element is visible
     * 
     * @param visible True if the element is visible
     */
    public void setVisible(boolean visible) {
        isVisible = visible;
    }
    
    /**
     * Check if the element is enabled
     * 
     * @return True if the element is enabled
     */
    public boolean isEnabled() {
        return isEnabled;
    }
    
    /**
     * Set whether the element is enabled
     * 
     * @param enabled True if the element is enabled
     */
    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
    
    /**
     * Check if the element is selected
     * 
     * @return True if the element is selected
     */
    public boolean isSelected() {
        return isSelected;
    }
    
    /**
     * Set whether the element is selected
     * 
     * @param selected True if the element is selected
     */
    public void setSelected(boolean selected) {
        isSelected = selected;
    }
    
    /**
     * Check if the element is required
     * 
     * @return True if the element is required
     */
    public boolean isRequired() {
        return isRequired;
    }
    
    /**
     * Set whether the element is required
     * 
     * @param required True if the element is required
     */
    public void setRequired(boolean required) {
        isRequired = required;
    }
    
    /**
     * Add an attribute
     * 
     * @param name Attribute name
     * @param value Attribute value
     * @return This element info instance for method chaining
     */
    public ElementInfo addAttribute(String name, String value) {
        if (this.attributes == null) {
            this.attributes = new HashMap<>();
        }
        this.attributes.put(name, value);
        return this;
    }
    
    /**
     * Get the most reliable selector for this element
     * 
     * @return The most reliable selector
     */
    public String getBestSelector() {
        if (id != null && !id.isEmpty()) {
            return "#" + id;
        } else if (cssSelector != null && !cssSelector.isEmpty()) {
            return cssSelector;
        } else if (xpath != null && !xpath.isEmpty()) {
            return xpath;
        } else {
            return selector;
        }
    }
    
    /**
     * Get the friendly name for this element
     * 
     * @return Friendly name
     */
    public String getFriendlyName() {
        return friendlyName;
    }
    
    /**
     * Set the friendly name for this element
     * 
     * @param friendlyName Friendly name
     */
    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public Rectangle getBoundingRect() {
        return boundingRect;
    }

    public void setBoundingRect(Rectangle boundingRect) {
        this.boundingRect = boundingRect;
    }
    
    /**
     * Inner class representing a rectangle
     */
    public static class Rectangle {
        private double x;
        private double y;
        private double width;
        private double height;
        
        public Rectangle() {
        }
        
        public Rectangle(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        // Getters and Setters
        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public double getWidth() {
            return width;
        }

        public void setWidth(double width) {
            this.width = width;
        }

        public double getHeight() {
            return height;
        }

        public void setHeight(double height) {
            this.height = height;
        }
    }
} 