package com.jio.multitranslator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(getInfo());
    }

    public Info getInfo() {
        return new Info()
                .title("Translator APIs")
                .version("1.0")
                .description("List of all Translator related APIs");
    }

}
