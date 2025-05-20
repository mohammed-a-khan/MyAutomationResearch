package com.cstestforge.project.model;

/**
 * Pagination parameters for API responses
 */
public class PaginationParams {
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;

    public PaginationParams() {
    }

    public PaginationParams(int page, int size, long totalItems, int totalPages) {
        this.page = page;
        this.size = size;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}