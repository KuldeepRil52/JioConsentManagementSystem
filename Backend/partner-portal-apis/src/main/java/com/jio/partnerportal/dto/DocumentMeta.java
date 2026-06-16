package com.jio.partnerportal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("documentId")
    private String documentId;
    @Schema(description = "Name of  the Document", example = "abc.pdf")
    @JsonProperty("name")
    private String name;
    @Schema(description = "Content type of the attachment", example = "application/pdf")
    @JsonProperty("contentType")
    private String contentType;
    @Schema(description = "Size of the attachment", example = "634742348")
    @JsonProperty("size")
    private Long size;
    @Schema(description = "Tag of the attachment")
    @JsonProperty("tag")
    private Tag tag;

}
