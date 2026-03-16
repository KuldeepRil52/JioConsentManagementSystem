package com.jio.digigov.notification.entity.event;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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