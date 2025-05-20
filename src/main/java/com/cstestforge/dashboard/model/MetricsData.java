package com.cstestforge.dashboard.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Model class for metrics chart data
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetricsData {
    
    private List<String> labels;
    private List<Dataset> datasets;
    
    /**
     * Default constructor
     */
    public MetricsData() {
    }
    
    /**
     * Constructor with required parameters
     * 
     * @param labels Chart labels (x-axis)
     * @param datasets Chart datasets
     */
    public MetricsData(List<String> labels, List<Dataset> datasets) {
        this.labels = labels;
        this.datasets = datasets;
    }
    
    /**
     * Get chart labels
     * @return List of labels
     */
    public List<String> getLabels() {
        return labels;
    }
    
    /**
     * Set chart labels
     * @param labels List of labels
     */
    public void setLabels(List<String> labels) {
        this.labels = labels;
    }
    
    /**
     * Get chart datasets
     * @return List of datasets
     */
    public List<Dataset> getDatasets() {
        return datasets;
    }
    
    /**
     * Set chart datasets
     * @param datasets List of datasets
     */
    public void setDatasets(List<Dataset> datasets) {
        this.datasets = datasets;
    }
    
    /**
     * Dataset model for chart data
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Dataset {
        private String label;
        private List<Number> data;
        private List<String> backgroundColor;
        private String borderColor;
        private Boolean fill;
        
        /**
         * Default constructor
         */
        public Dataset() {
        }
        
        /**
         * Constructor with required parameters
         * 
         * @param label Dataset label
         * @param data Dataset values
         */
        public Dataset(String label, List<Number> data) {
            this.label = label;
            this.data = data;
        }
        
        /**
         * Constructor for line chart
         * 
         * @param label Dataset label
         * @param data Dataset values
         * @param borderColor Line color
         * @param fill Whether to fill the area under the line
         */
        public Dataset(String label, List<Number> data, String borderColor, Boolean fill) {
            this.label = label;
            this.data = data;
            this.borderColor = borderColor;
            this.fill = fill;
        }
        
        /**
         * Constructor for bar chart or pie chart
         * 
         * @param label Dataset label
         * @param data Dataset values
         * @param backgroundColor Background colors for bars or pie slices
         */
        public Dataset(String label, List<Number> data, List<String> backgroundColor) {
            this.label = label;
            this.data = data;
            this.backgroundColor = backgroundColor;
        }
        
        /**
         * Get dataset label
         * @return Dataset label
         */
        public String getLabel() {
            return label;
        }
        
        /**
         * Set dataset label
         * @param label Dataset label
         */
        public void setLabel(String label) {
            this.label = label;
        }
        
        /**
         * Get dataset values
         * @return List of values
         */
        public List<Number> getData() {
            return data;
        }
        
        /**
         * Set dataset values
         * @param data List of values
         */
        public void setData(List<Number> data) {
            this.data = data;
        }
        
        /**
         * Get background colors
         * @return List of color strings
         */
        public List<String> getBackgroundColor() {
            return backgroundColor;
        }
        
        /**
         * Set background colors
         * @param backgroundColor List of color strings
         */
        public void setBackgroundColor(List<String> backgroundColor) {
            this.backgroundColor = backgroundColor;
        }
        
        /**
         * Get border color
         * @return Border color string
         */
        public String getBorderColor() {
            return borderColor;
        }
        
        /**
         * Set border color
         * @param borderColor Border color string
         */
        public void setBorderColor(String borderColor) {
            this.borderColor = borderColor;
        }
        
        /**
         * Get fill setting
         * @return Whether to fill the area under the line
         */
        public Boolean getFill() {
            return fill;
        }
        
        /**
         * Set fill setting
         * @param fill Whether to fill the area under the line
         */
        public void setFill(Boolean fill) {
            this.fill = fill;
        }
    }
} 