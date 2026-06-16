package com.example.scanner.dto;

import lombok.Data;

import java.util.List;


@Data
public class ScanResultDto {
    private String id;
    private String transactionId;
    private String status;
    private List<CookieDto> cookies;
    private String errorMessage;
    private String url;

}