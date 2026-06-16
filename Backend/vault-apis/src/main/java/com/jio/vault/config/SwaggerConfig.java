package com.jio.vault.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Vault Client API")
                        .version("1.0")
                        .description("APIs for managing vault keys, encryption, decryption, and JWT signing"));
    }

    @Bean
    public GroupedOpenApi vaultClientApi() {
        return GroupedOpenApi.builder()
                .group("vault-client")
                .pathsToMatch(
                        "/client/key/onboard",
                        "/client/encryptPayload",
                        "/client/decryptedPayload",
                        "/client/sign",
                        "/client/verify"

                )
                .build();
    }
}
