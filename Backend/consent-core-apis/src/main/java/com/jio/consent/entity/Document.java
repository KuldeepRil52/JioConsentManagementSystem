package com.jio.consent.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.dto.Tag;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@org.springframework.data.mongodb.core.mapping.Document("documents")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class Document extends AbstractEntity{

    @Id
    private ObjectId id;
    @Indexed(unique = true, name = "documentId")
    private String documentId;
    private String businessId;
    private String documentName;
    private String contentType;
    private boolean isBase64Document;
    private Long documentSize;
    String data;
    private int version;
    private String status;
    private Tag tag;
}
