package com.jio.vault.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.vault.client.VaultClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
@Slf4j
public class JwtGenerator {

    private final VaultClient vaultClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtGenerator(VaultClient vaultClient) {
        this.vaultClient = vaultClient;
    }

    public String generateJwt(String clientPayloadJson, String keyName) {
        try {
            String headerJson = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
            String header = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));


            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(clientPayloadJson.getBytes(StandardCharsets.UTF_8));

            String dataToSign = header + "." + payload;

            String vaultInput = Base64.getEncoder()
                    .encodeToString(dataToSign.getBytes(StandardCharsets.UTF_8));

            String requestBody = String.format(
                    "{ \"input\": \"%s\", \"signature_algorithm\": \"pkcs1v15\" }",
                    vaultInput
            );

            String vaultSignature = vaultClient.sign(requestBody, keyName);

            byte[] signatureBytes = Base64.getDecoder().decode(vaultSignature);
            String jwtSignature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(signatureBytes);
            log.info("Successfully Created the JWT for the given payload");
            return dataToSign + "." + jwtSignature;

        } catch (Exception e) {
            log.error("Failed to generate the JWT for the given payload {}", e.getMessage());
            throw new RuntimeException("Error generating JWT", e);
        }
    }


}
