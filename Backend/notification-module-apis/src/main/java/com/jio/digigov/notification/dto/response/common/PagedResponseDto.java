package com.jio.digigov.notification.dto.response.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Paginated response wrapper")
public class PagedResponseDto<T> {

    @Schema(description = "Response data")
    private List<T> data;

    @Schema(description = "Pagination information")
    private PaginationInfo pagination;

    @Data
    @Builder
    @Schema(description = "Pagination details")
    public static class PaginationInfo {
        @Schema(description = "Total number of items", example = "45")
        private long totalItems;

        @Schema(description = "Total number of pages", example = "3")
        private int totalPages;

        @Schema(description = "Current page number", example = "1")
        private int page;

        @Schema(description = "Items per page", example = "20")
        private int pageSize;

        @Schema(description = "Has next page", example = "true")
        @Builder.Default
        private boolean hasNext = false;

        @Schema(description = "Has previous page", example = "false")
        @Builder.Default
        private boolean hasPrevious = false;
    }
}