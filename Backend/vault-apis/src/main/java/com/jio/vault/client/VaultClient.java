package com.jio.vault.client;

import com.jio.vault.config.VaultProperties;
import com.jio.vault.dto.VaultAesResponse;
import com.jio.vault.dto.VaultKeyResponse;
import com.jio.vault.dto.VaultSignResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class VaultClient {

    private final RestTemplate restTemplate;
    private final VaultProperties vaultProperties;

    public VaultClient(VaultProperties vaultProperties) {
        this.restTemplate = new RestTemplate();
        this.vaultProperties = vaultProperties;
    }
    public VaultKeyResponse createTransitKey(String keyName, String certType) {
        String url = vaultProperties.getBaseUrl() + "/v1/transit/keys/" + keyName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Vault-Token", vaultProperties.getToken());

        Map<String, Object> body = new HashMap<>();
        body.put("type", certType);
        body.put("deletion_allowed", true);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        return restTemplate.postForObject(url, entity, VaultKeyResponse.class);
    }
    public VaultAesResponse createAesKey(String keyName) {
        log.info("Saving the AES key for the client");
        String url = vaultProperties.getBaseUrl() + "/v1/transit/keys/" + keyName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Vault-Token", vaultProperties.getToken());

        Map<String, Object> body = new HashMap<>();
        body.put("type", "aes256-gcm96");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        log.info("Set headers and Body as: " + entity.toString());
        log.info("Url as: " + url);
        return restTemplate.postForObject(url, entity, VaultAesResponse.class);
    }

    public String checkVaultHealth() {
        String url = vaultProperties.getBaseUrl() + "/v1/sys/health";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Vault-Token", vaultProperties.getToken());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            return "Vault health okay";
        } else {
            return "Vault health not okay: " + response.getStatusCode();
        }
    }

    public String sign(String payload, String keyName) {
        String url = vaultProperties.getBaseUrl() + "/v1/transit/sign/" + keyName + "/sha2-256";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Vault-Token", vaultProperties.getToken());

        // Forward the payload as-is
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        VaultSignResponse response = restTemplate.postForObject(url, entity, VaultSignResponse.class);

        if (response != null && response.getData() != null) {
            String signature = response.getData().getSignature();
            if (signature != null) {
                return signature.replaceFirst("^vault:v\\d+:", ""); // trim vault:vX: prefix
            }
        }

        return null; // or throw a custom exception
    }
    public String encrypt(String keyName, String base64Text) {
        String url = vaultProperties.getBaseUrl() + vaultProperties.getEncryptUri() + keyName;


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Vault-Token", vaultProperties.getToken());

        Map<String, Object> body = new HashMap<>();
        body.put("plaintext", base64Text);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // Vault returns JSON with data.ciphertext
        Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

        if (response != null && response.get("data") instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            String ciphertext = (String) data.get("ciphertext");
            if (ciphertext != null) {
                return ciphertext.trim();
            }
        }

        return null; // or throw custom exception
    }
    public String decrypt(String keyName, String ciphertext) {
        String url = vaultProperties.getBaseUrl() + vaultProperties.getDecryptUri() + keyName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Vault-Token", vaultProperties.getToken());

        Map<String, Object> body = new HashMap<>();
        body.put("ciphertext", ciphertext);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

        if (response != null && response.get("data") instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            String plaintext = (String) data.get("plaintext");
            if (plaintext != null) {
                return plaintext.trim();
            }
        }
        return null;
    }

}
