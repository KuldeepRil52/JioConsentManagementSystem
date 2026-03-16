package com.jio.digigov.grievance.dto;

import com.jio.digigov.grievance.entity.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentMeta {
    private String documentId;

    @Schema(description = "Document name", example = "sample-local-pdf.pdf")
    private String name;

    @Schema(description = "Content type", example = "application/pdf")
    private String contentType;

    @Schema(description = "Document size in bytes", example = "327467")
    private Long size;

    @Schema(description = "Tag object")
    private Tag tag;
}
