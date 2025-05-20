package com.cstestforge.project.model;

import java.util.List;

/**
 * Filter criteria for querying projects.
 */
public class ProjectFilter {
    private String search;
    private List<ProjectStatus> statuses;
    private List<ProjectType> types;
    private List<String> tags;
    private String sortBy;
    private String sortDirection;
    private int page;
    private int size;

    public ProjectFilter() {
        // Default values
        this.sortBy = "updatedAt";
        this.sortDirection = "desc";
        this.page = 1;
        this.size = 10;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public List<ProjectStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<ProjectStatus> statuses) {
        this.statuses = statuses;
    }

    public List<ProjectType> getTypes() {
        return types;
    }

    public void setTypes(List<ProjectType> types) {
        this.types = types;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page > 0 ? page : 1;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size > 0 ? size : 10;
    }

    /**
     * Calculate the offset for pagination
     * 
     * @return The offset for the current page
     */
    public int getOffset() {
        return (page - 1) * size;
    }
} 