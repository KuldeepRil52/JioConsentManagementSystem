package com.jio.digigov.grievance.dto;

import java.util.List;

/**
 * Generic wrapper for paginated API responses.
 * Keeps API responses clean (data + pagination metadata).
 */
public class PagedDetailResponse<T> {

    private List<T> data;
    private Pagination pagination;

    public PagedDetailResponse(List<T> data, Pagination pagination) {
        this.data = data;
        this.pagination = pagination;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    // Inner static class for pagination details
    public static class Pagination {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;

        public Pagination(int page, int size, long totalElements, int totalPages, boolean last) {
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.last = last;
        }

        public int getPage() {
            return page;
        }

        public int getSize() {
            return size;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public boolean isLast() {
            return last;
        }
    }
}

