package com.jio.digigov.auditmodule.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Audit Service API")
                        .description("APIs for managing audit logs per tenant")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Audit Team")
                                .email("support@digigov.com")
                                .url("https://digigov.com"))
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }
}
