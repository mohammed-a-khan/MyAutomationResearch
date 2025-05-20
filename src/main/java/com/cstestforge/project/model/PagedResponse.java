package com.cstestforge.project.model;

import java.util.List;

/**
 * Response model for paginated data.
 *
 * @param <T> Type of items in the page
 */
public class PagedResponse<T> {
    private List<T> items;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;

    public PagedResponse() {
    }

    public PagedResponse(List<T> items, int page, int size, long totalItems) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalItems = totalItems;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalItems / size) : 0;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
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

    /**
     * Check if this is the first page
     * 
     * @return true if this is the first page
     */
    public boolean isFirst() {
        return page == 1;
    }

    /**
     * Check if this is the last page
     * 
     * @return true if this is the last page
     */
    public boolean isLast() {
        return page >= totalPages;
    }

    /**
     * Check if there is a next page
     * 
     * @return true if there is a next page
     */
    public boolean hasNext() {
        return page < totalPages;
    }

    /**
     * Check if there is a previous page
     * 
     * @return true if there is a previous page
     */
    public boolean hasPrevious() {
        return page > 1;
    }
} 