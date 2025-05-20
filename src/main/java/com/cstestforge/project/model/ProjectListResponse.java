package com.cstestforge.project.model;

import java.util.List;

/**
 * Response model for project list API
 */
public class ProjectListResponse {
    private List<Project> projects;
    private PaginationParams pagination;

    public ProjectListResponse() {
    }

    public ProjectListResponse(List<Project> projects, PaginationParams pagination) {
        this.projects = projects;
        this.pagination = pagination;
    }

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }

    public PaginationParams getPagination() {
        return pagination;
    }

    public void setPagination(PaginationParams pagination) {
        this.pagination = pagination;
    }
}