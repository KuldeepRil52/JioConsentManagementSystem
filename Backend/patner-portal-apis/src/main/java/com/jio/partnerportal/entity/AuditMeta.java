package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.partnerportal.client.audit.request.AuditRequest;
import com.jio.partnerportal.client.audit.response.AuditResponse;
import com.jio.partnerportal.dto.AuditMetaStatus;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Document(collection = "audit_meta")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditMeta extends AbstractEntity {

    @Id
    private ObjectId id;
    @Indexed(unique = true)
    private String auditMetaId;
    private String businessId;
    private AuditRequest auditRequest;
    private AuditResponse auditResponse;
    private AuditMetaStatus status;
    private String httpStatus;
    private String auditId;
    private String errorMessage;
}

