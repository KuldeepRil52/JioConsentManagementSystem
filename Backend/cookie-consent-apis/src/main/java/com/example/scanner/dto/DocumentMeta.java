package com.example.scanner.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentMeta {

    @Schema(
            description = "Unique document identifier",
            example = "eyXXX.EXAMPLE-TOKEN-NOT-REAL.xxxXXX"
    )
    private String documentId;


    @Schema(
            description = "Original filename",
            example = "privacy-policy.pdf"
    )
    private String name;

    @Schema(
            description = "MIME type of the document",
            example = "application/pdf"
    )
    private String contentType;

    @Schema(
            description = "File size in bytes",
            example = "327467"
    )
    private Long size;

    @Schema(
            description = "Document classification tags",
            implementation = Tag.class
    )
    private Tag tag;
}