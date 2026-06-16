package com.jio.digigov.grievance.entity;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "documents")
public class DocumentEntity {

    @Id
    private ObjectId id;

    private String documentId;       // UUID
    private String businessId;
    private String documentName;
    private String contentType;
    private boolean isBase64Document;
    private Long documentSize;
    private String data;             // base64 encoded content
    private int version;
    private String status;

    private Tag tag;
}
