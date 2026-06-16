package com.jio.digigov.auditmodule.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi auditApi() {
        return GroupedOpenApi.builder()
                .group("audit")
                .packagesToScan("com.jio.digigov.auditmodule.controller") // only scan controllers
                .build();
    }
}
