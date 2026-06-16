package com.example.scanner.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ScanRequestDto {

    @NotBlank(message = "URL is required and cannot be empty")
    private String url;

    private List<String> subDomain;

}