package com.example.scanner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiConfig {

    @Value("${api.context.path:/}")
    private String apiContextPath;

    @Value("${api.versioning.enabled:false}")
    private boolean versioningEnabled;

    public String getApiContextPath() {
        return versioningEnabled ? apiContextPath : "/";
    }
}