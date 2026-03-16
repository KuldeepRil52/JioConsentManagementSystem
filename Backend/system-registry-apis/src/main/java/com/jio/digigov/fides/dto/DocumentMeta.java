package com.jio.digigov.fides.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentMeta {

    @Field("documentId")
    private String documentId;

    @Field("name")
    private String name;

    @Field("contentType")
    private String contentType;

    @Field("size")
    private Long size;

    @Field("tag")
    private DocumentTag tag;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentTag {

        @Field("documentTag")
        private String documentTag;
    }
}