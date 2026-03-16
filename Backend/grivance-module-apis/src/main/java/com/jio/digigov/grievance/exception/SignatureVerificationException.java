package com.jio.digigov.grievance.exception;

import com.jio.digigov.grievance.enumeration.JdnmErrorCode;

import java.util.Map;

/**
 * Exception thrown when request signature verification fails.
 *
 * <p>This exception is thrown when:
 * <ul>
 *   <li>The x-jws-signature header is missing or invalid</li>
 *   <li>The signature does not match the request payload</li>
 *   <li>The certificate for signature verification cannot be found</li>
 *   <li>The signature algorithm is not supported</li>
 *   <li>The signature has been tampered with</li>
 * </ul>
 *
 * <p><b>HTTP Status:</b> 403 Forbidden (JDNM4004)</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * if (!signatureValid) {
 *     throw new SignatureVerificationException("Invalid signature for request")
 *         .addMetadata("tenantId", tenantId)
 *         .addMetadata("businessId", businessId);
 * }
 * </pre>
 *
 * @see JdnmException
 * @see com.jio.digigov.grievance.enumeration.JdnmErrorCode#JDNM4004
 * @since 2.0.0
 */
public class SignatureVerificationException extends JdnmException {

    /**
     * Creates a signature verification exception with default message.
     */
    public SignatureVerificationException() {
        super(JdnmErrorCode.JDNM4004);
    }

    /**
     * Creates a signature verification exception with custom message.
     *
     * @param message detailed error message
     */
    public SignatureVerificationException(String message) {
        super(JdnmErrorCode.JDNM4004, message);
    }

    /**
     * Creates a signature verification exception with custom message and cause.
     *
     * @param message detailed error message
     * @param cause the underlying cause
     */
    public SignatureVerificationException(String message, Throwable cause) {
        super(JdnmErrorCode.JDNM4004, message, cause);
    }

    /**
     * Creates a signature verification exception with custom message and metadata.
     *
     * @param message detailed error message
     * @param metadata additional error context
     */
    public SignatureVerificationException(String message, Map<String, Object> metadata) {
        super(JdnmErrorCode.JDNM4004, message, metadata);
    }

    /**
     * Creates a signature verification exception with all parameters.
     *
     * @param message detailed error message
     * @param cause the underlying cause
     * @param metadata additional error context
     */
    public SignatureVerificationException(String message, Throwable cause, Map<String, Object> metadata) {
        super(JdnmErrorCode.JDNM4004, message, cause, metadata);
    }

    // Static factory methods for common scenarios

    /**
     * Creates exception for missing signature header.
     *
     * @return SignatureVerificationException
     */
    public static SignatureVerificationException missingSignature() {
        return new SignatureVerificationException("x-jws-signature header is missing or empty");
    }

    /**
     * Creates exception for invalid signature format.
     *
     * @return SignatureVerificationException
     */
    public static SignatureVerificationException invalidFormat() {
        return new SignatureVerificationException("Signature format is invalid - expected detached JWT format (header..signature)");
    }

    /**
     * Creates exception for signature mismatch.
     *
     * @return SignatureVerificationException
     */
    public static SignatureVerificationException signatureMismatch() {
        return new SignatureVerificationException("Signature verification failed - payload has been tampered with");
    }

    /**
     * Creates exception for certificate not found.
     *
     * @param entityType the type of entity (DATA_FIDUCIARY or DATA_PROCESSOR)
     * @param entityId the entity ID
     * @return SignatureVerificationException with metadata
     */
    public static SignatureVerificationException certificateNotFound(String entityType, String entityId) {
        return (SignatureVerificationException) new SignatureVerificationException(
                String.format("Certificate not found for %s: %s", entityType, entityId))
                .addMetadata("entityType", entityType)
                .addMetadata("entityId", entityId);
    }

    /**
     * Creates exception for unsupported algorithm.
     *
     * @param algorithm the unsupported algorithm
     * @return SignatureVerificationException with metadata
     */
    public static SignatureVerificationException unsupportedAlgorithm(String algorithm) {
        return (SignatureVerificationException) new SignatureVerificationException(
                String.format("Unsupported signature algorithm: %s", algorithm))
                .addMetadata("algorithm", algorithm);
    }

    /**
     * Creates exception for missing required headers.
     *
     * @param missingHeaders comma-separated list of missing headers
     * @return SignatureVerificationException with metadata
     */
    public static SignatureVerificationException missingRequiredHeaders(String missingHeaders) {
        return (SignatureVerificationException) new SignatureVerificationException(
                String.format("Missing required headers for signature verification: %s", missingHeaders))
                .addMetadata("missingHeaders", missingHeaders);
    }

    @Override
    public SignatureVerificationException addMetadata(String key, Object value) {
        super.addMetadata(key, value);
        return this;
    }

    @Override
    public SignatureVerificationException addMetadata(Map<String, Object> additionalMetadata) {
        super.addMetadata(additionalMetadata);
        return this;
    }
}

