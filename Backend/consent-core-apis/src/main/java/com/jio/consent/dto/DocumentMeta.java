package com.jio.consent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.constant.ErrorCodes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
    @NotBlank(message = ErrorCodes.JCMP1050)
    private String name;

    @Schema(description = "Content type", example = "application/pdf")
    @NotBlank(message = ErrorCodes.JCMP1050)
    private String contentType;

    @Schema(description = "Document size in bytes", example = "327467")
    private Long size;

    @Schema(description = "Tag object")
    @Valid
    private Tag tag;

}
