package com.jio.consent.service.signature;

import java.util.Map;

/**
 * Service interface for JWS signature operations.
 * Provides methods for verifying incoming request signatures and generating outgoing response signatures.
 */
public interface RequestResponseSignatureService {

    /**
     * Checks if response signing is enabled.
     *
     * @return true if response signing is enabled, false otherwise
     */
    boolean isSignResponseEnabled();

    /**
     * Checks if request verification is enabled.
     *
     * @return true if request verification is enabled, false otherwise
     */
    boolean isVerifyRequestEnabled();

    /**
     * Verifies the signature of an incoming request.
     *
     * @param payload the request payload object
     * @param headers the request headers containing signature and metadata
     * @return true if signature is valid
     * @throws Exception if signature verification fails
     */
    boolean verifyRequest(Object payload, Map<String, String> headers) throws Exception;

    /**
     * Signs a response payload.
     *
     * @param tenantId the tenant ID
     * @param payload the response payload object
     * @return detached JWT signature string, or null if signing is disabled
     * @throws Exception if signature generation fails
     */
    String signResponse(String tenantId, Object payload) throws Exception;

    /**
     * Decodes a certificate from various encoded formats.
     *
     * @param encodedValue the encoded certificate value
     * @return decoded certificate string, or null if decoding fails
     */
    String decodeCertificate(String encodedValue);
}

