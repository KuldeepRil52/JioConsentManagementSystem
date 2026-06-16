package com.jio.schedular.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
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
