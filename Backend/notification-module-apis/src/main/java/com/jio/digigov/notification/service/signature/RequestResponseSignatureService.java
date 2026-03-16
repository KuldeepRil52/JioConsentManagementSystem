package com.jio.digigov.notification.service.signature;

import com.jio.digigov.notification.exception.SignatureGenerationException;
import com.jio.digigov.notification.exception.SignatureVerificationException;

import java.util.Map;

/**
 * Service for verifying request signatures and signing response payloads.
 *
 * <p>This service implements JWS (JSON Web Signature) with RSA256 algorithm
 * using detached JWT format where the payload is transmitted separately
 * from the signature header and signature.</p>
 *
 * <p><b>Detached JWT Format:</b></p>
 * <pre>
 * Normal JWT:    header.payload.signature
 * Detached JWT:  header..signature (payload sent separately in request/response body)
 * </pre>
 *
 * <p><b>Request Verification Flow:</b></p>
 * <ol>
 *   <li>Extract x-jws-signature header from request</li>
 *   <li>Extract entity headers (tenant-id, business-id, requestor-type, etc.)</li>
 *   <li>Determine entity type (DATA_FIDUCIARY or DATA_PROCESSOR)</li>
 *   <li>Fetch appropriate certificate from database</li>
 *   <li>Serialize request payload to alphabetically sorted JSON</li>
 *   <li>Reconstruct full JWT: header.base64(payload).signature</li>
 *   <li>Verify signature using RSA public key from certificate</li>
 * </ol>
 *
 * <p><b>Response Signing Flow:</b></p>
 * <ol>
 *   <li>Fetch JWK private key from auth_key collection</li>
 *   <li>Serialize response DTO to alphabetically sorted JSON</li>
 *   <li>Create JWT header with RS256 algorithm</li>
 *   <li>Sign payload using RSA private key</li>
 *   <li>Return detached signature: header..signature</li>
 * </ol>
 *
 * @see com.jio.digigov.notification.util.SignatureJsonSerializer
 * @see com.jio.digigov.notification.repository.signature.JwkKeyRepository
 * @see com.jio.digigov.notification.repository.signature.BusinessKeyRepository
 * @since 2.0.0
 */
public interface RequestResponseSignatureService {

    /**
     * Verifies the signature of an incoming request payload.
     *
     * <p>This method validates that the request payload has not been tampered with
     * by verifying the JWS signature against the sender's public key certificate.</p>
     *
     * <p><b>Required Headers:</b></p>
     * <ul>
     *   <li><b>tenant-id</b> - Tenant identifier (always required)</li>
     *   <li><b>requestor-type</b> - Either DATA_FIDUCIARY or DATA_PROCESSOR</li>
     *   <li><b>business-id</b> - Required when requestor-type is DATA_FIDUCIARY</li>
     *   <li><b>data-processor-id</b> - Required when requestor-type is DATA_PROCESSOR</li>
     *   <li><b>x-jws-signature</b> - Detached JWT signature (header..signature)</li>
     * </ul>
     *
     * @param payload the request payload (DTO or Map)
     * @param headers map of request headers containing signature and entity information
     * @return true if signature is valid
     * @throws SignatureVerificationException if signature verification fails
     * @throws IllegalArgumentException if required headers are missing
     */
    boolean verifyRequest(Object payload, Map<String, String> headers)
            throws SignatureVerificationException;

    /**
     * Signs a response payload and returns the detached JWT signature.
     *
     * <p>This method generates a JWS signature for the response payload using
     * the tenant's RSA private key from the auth_key collection. The signature
     * is returned in detached format and should be sent in the x-jws-signature
     * response header.</p>
     *
     * <p><b>Signature Format:</b></p>
     * <pre>
     * eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..RlBQzZXNzb3JJZDEyMyIsImV4cCI6M...
     * └─────────── header ──────────┘  └─────────── signature ────────────┘
     *                                 ↑
     *                            empty payload
     * </pre>
     *
     * @param tenantId the tenant ID for multi-tenancy isolation
     * @param payload the response payload (DTO) to sign
     * @return detached JWT signature string (header..signature format)
     * @throws SignatureGenerationException if signature generation fails
     */
    String signResponse(String tenantId, Object payload) throws SignatureGenerationException;

    /**
     * Checks if response signing is enabled.
     *
     * <p>This method reads the 'app.sign.response' configuration property
     * to determine if response signing should be performed.</p>
     *
     * @return true if response signing is enabled, false otherwise
     */
    boolean isSignResponseEnabled();

    /**
     * Checks if request verification is enabled.
     *
     * <p>This method reads the 'app.verify.request' configuration property
     * to determine if request verification should be performed.</p>
     *
     * @return true if request verification is enabled, false otherwise
     */
    boolean isVerifyRequestEnabled();

    /**
     * Decodes a certificate from various formats (Base64 with prefix, PEM, etc.).
     *
     * <p>This method handles multiple certificate encoding formats:
     * <ul>
     *   <li><b>Base64 with prefix:</b> "base64,MIIDdzCCAl+gAwIBAgI..."</li>
     *   <li><b>PEM format:</b> "-----BEGIN CERTIFICATE-----\n..."</li>
     *   <li><b>Direct Base64:</b> "MIIDdzCCAl+gAwIBAgI..."</li>
     * </ul>
     *
     * @param encodedCertificate the encoded certificate string
     * @return decoded certificate string in PEM format
     * @throws SignatureVerificationException if certificate decoding fails
     */
    String decodeCertificate(String encodedCertificate) throws SignatureVerificationException;
}
