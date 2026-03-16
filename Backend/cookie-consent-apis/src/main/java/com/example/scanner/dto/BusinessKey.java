package com.example.scanner.dto;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "business_keys")
public class BusinessKey {
    private String tenantId;
    private String businessId;
    private String publicKey;
}
