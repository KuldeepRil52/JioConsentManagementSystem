package com.jio.auth.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.Date;

@Data
@Document(collection = "auth_failed_audits")
public class FailedAudit {

    @Id
    private String id;

    private String txnId;
    private String tenantId;
    private String businessId;
    private String transactionId;
    private String ip;
    private String requestUri;
    private String method;
    private String requestPayload;
    private String responsePayload;
    private String actor;

    @Indexed(name = "ttl_index", expireAfter = "P7D")
    private Date createdAt;

}
