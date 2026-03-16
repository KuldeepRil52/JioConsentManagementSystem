package com.jio.digigov.auditmodule.client.vault.response;

import lombok.Data;

@Data
public class DecryptPayloadResponse {

    private String referenceId;
    private DecryptedPayload decryptedPayload;

    @Data
    public static class DecryptedPayload {
        private String dataString;  // This contains your entire decrypted JSON as a string
    }
}
