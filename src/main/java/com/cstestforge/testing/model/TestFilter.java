package com.cstestforge.testing.model;

import com.cstestforge.project.model.test.TestStatus;
import com.cstestforge.project.model.test.TestType;
import java.util.List;

/**
 * Filter criteria for searching tests.
 */
public class TestFilter {
    
    private String search;
    private List<TestStatus> statuses;
    private List<TestType> types;
    private List<String> tags;
    private String sortBy;
    private String sortDirection;
    private int page;
    private int size;
    
    /**
     * Default constructor
     */
    public TestFilter() {
        this.page = 1;
        this.size = 10;
        this.sortBy = "updatedAt";
        this.sortDirection = "desc";
    }
    
    /**
     * Get the search term
     * 
     * @return Search term
     */
    public String getSearch() {
        return search;
    }
    
    /**
     * Set the search term
     * 
     * @param search Search term
     */
    public void setSearch(String search) {
        this.search = search;
    }
    
    /**
     * Get the status filters
     * 
     * @return List of statuses
     */
    public List<TestStatus> getStatuses() {
        return statuses;
    }
    
    /**
     * Set the status filters
     * 
     * @param statuses List of statuses
     */
    public void setStatuses(List<TestStatus> statuses) {
        this.statuses = statuses;
    }
    
    /**
     * Get the type filters
     * 
     * @return List of types
     */
    public List<TestType> getTypes() {
        return types;
    }
    
    /**
     * Set the type filters
     * 
     * @param types List of types
     */
    public void setTypes(List<TestType> types) {
        this.types = types;
    }
    
    /**
     * Get the tag filters
     * 
     * @return List of tags
     */
    public List<String> getTags() {
        return tags;
    }
    
    /**
     * Set the tag filters
     * 
     * @param tags List of tags
     */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    /**
     * Get the sort field
     * 
     * @return Sort field
     */
    public String getSortBy() {
        return sortBy;
    }
    
    /**
     * Set the sort field
     * 
     * @param sortBy Sort field
     */
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
    
    /**
     * Get the sort direction
     * 
     * @return Sort direction
     */
    public String getSortDirection() {
        return sortDirection;
    }
    
    /**
     * Set the sort direction
     * 
     * @param sortDirection Sort direction
     */
    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
    
    /**
     * Get the page number
     * 
     * @return Page number
     */
    public int getPage() {
        return page;
    }
    
    /**
     * Set the page number
     * 
     * @param page Page number
     */
    public void setPage(int page) {
        this.page = page;
    }
    
    /**
     * Get the page size
     * 
     * @return Page size
     */
    public int getSize() {
        return size;
    }
    
    /**
     * Set the page size
     * 
     * @param size Page size
     */
    public void setSize(int size) {
        this.size = size;
    }
    
    /**
     * Get the offset for pagination
     * 
     * @return Offset
     */
    public int getOffset() {
        return (page - 1) * size;
    }
} 