package com.wso2wrapper.credentials_generation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WSO2 Tenant Onboarding API")
                        .version("1.0")
                        .description("API for registering tenants and onboarding businesses"));
    }
}