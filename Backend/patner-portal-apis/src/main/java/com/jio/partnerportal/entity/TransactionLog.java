package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionLog {

    @Id
    private String id;

    private String txnId;
    private String apiName;
    private int httpStatus;
    private String version;

    private Instant createdAt;
    private Instant updatedAt;

    private String errorResponse;
    private String request;
    private String response;

    private long responseTime; // in ms
}

