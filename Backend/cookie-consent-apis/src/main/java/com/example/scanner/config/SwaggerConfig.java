package com.example.scanner.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI cookieScannerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cookie Scanner API")
                        .description("DPDPA Compliant Cookie Scanning and Categorization Service")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Cookie Scanner Team")
                                .email("support@cookiescanner.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development")
                ));
    }
}