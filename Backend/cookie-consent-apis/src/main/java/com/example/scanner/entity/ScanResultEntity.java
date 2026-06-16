package com.example.scanner.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

@Data
@Document(collection = "cookie_scan_results")
public class ScanResultEntity {
    @Id
    private String id;
    private String transactionId;
    private String status;
    private String url;
    private String errorMessage;

    // NEW: Grouped storage
    private Map<String, List<CookieEntity>> cookiesBySubdomain;

}
