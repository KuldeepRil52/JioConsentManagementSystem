package com.jio.digigov.notification.exception;

import com.jio.digigov.notification.enums.JdnmErrorCode;

import java.util.Map;

/**
 * Exception thrown when response signature generation fails.
 *
 * <p>This exception is thrown when:
 * <ul>
 *   <li>The JWK private key cannot be found in the database</li>
 *   <li>The JWK key format is invalid or corrupted</li>
 *   <li>The RSA key parameters are missing or invalid</li>
 *   <li>The signature algorithm fails during signing</li>
 *   <li>JSON serialization of response fails</li>
 *   <li>Cryptographic operations encounter errors</li>
 * </ul>
 *
 * <p><b>HTTP Status:</b> 500 Internal Server Error (JDNM5011)</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * try {
 *     String signature = signResponse(responseDto);
 * } catch (Exception e) {
 *     throw new SignatureGenerationException("Failed to sign response", e)
 *         .addMetadata("responseType", responseDto.getClass().getSimpleName());
 * }
 * </pre>
 *
 * @see JdnmException
 * @see JdnmErrorCode#JDNM5011
 * @since 2.0.0
 */
public class SignatureGenerationException extends JdnmException {

    /**
     * Creates a signature generation exception with default message.
     */
    public SignatureGenerationException() {
        super(JdnmErrorCode.JDNM5011);
    }

    /**
     * Creates a signature generation exception with custom message.
     *
     * @param message detailed error message
     */
    public SignatureGenerationException(String message) {
        super(JdnmErrorCode.JDNM5011, message);
    }

    /**
     * Creates a signature generation exception with custom message and cause.
     *
     * @param message detailed error message
     * @param cause the underlying cause
     */
    public SignatureGenerationException(String message, Throwable cause) {
        super(JdnmErrorCode.JDNM5011, message, cause);
    }

    /**
     * Creates a signature generation exception with custom message and metadata.
     *
     * @param message detailed error message
     * @param metadata additional error context
     */
    public SignatureGenerationException(String message, Map<String, Object> metadata) {
        super(JdnmErrorCode.JDNM5011, message, metadata);
    }

    /**
     * Creates a signature generation exception with all parameters.
     *
     * @param message detailed error message
     * @param cause the underlying cause
     * @param metadata additional error context
     */
    public SignatureGenerationException(String message, Throwable cause, Map<String, Object> metadata) {
        super(JdnmErrorCode.JDNM5011, message, cause, metadata);
    }

    // Static factory methods for common scenarios

    /**
     * Creates exception for missing JWK key.
     *
     * @return SignatureGenerationException
     */
    public static SignatureGenerationException jwkKeyNotFound() {
        return new SignatureGenerationException("JWK signing key not found in database - please configure auth_key collection");
    }

    /**
     * Creates exception for invalid JWK key format.
     *
     * @param reason the specific reason for invalid format
     * @return SignatureGenerationException with metadata
     */
    public static SignatureGenerationException invalidJwkFormat(String reason) {
        return (SignatureGenerationException) new SignatureGenerationException(
                String.format("Invalid JWK key format: %s", reason))
                .addMetadata("reason", reason);
    }

    /**
     * Creates exception for missing RSA parameters.
     *
     * @param missingParam the missing parameter name
     * @return SignatureGenerationException with metadata
     */
    public static SignatureGenerationException missingRsaParameter(String missingParam) {
        return (SignatureGenerationException) new SignatureGenerationException(
                String.format("Missing required RSA parameter in JWK key: %s", missingParam))
                .addMetadata("missingParameter", missingParam);
    }

    /**
     * Creates exception for JSON serialization failure.
     *
     * @param dtoType the type of DTO that failed to serialize
     * @param cause the underlying exception
     * @return SignatureGenerationException with metadata
     */
    public static SignatureGenerationException jsonSerializationFailed(String dtoType, Throwable cause) {
        return (SignatureGenerationException) new SignatureGenerationException(
                String.format("Failed to serialize %s to JSON for signing", dtoType), cause)
                .addMetadata("dtoType", dtoType);
    }

    /**
     * Creates exception for cryptographic operation failure.
     *
     * @param operation the operation that failed (e.g., "RSA key generation", "signature creation")
     * @param cause the underlying exception
     * @return SignatureGenerationException with metadata
     */
    public static SignatureGenerationException cryptoOperationFailed(String operation, Throwable cause) {
        return (SignatureGenerationException) new SignatureGenerationException(
                String.format("Cryptographic operation failed: %s", operation), cause)
                .addMetadata("operation", operation);
    }

    /**
     * Creates exception for Base64 decoding failure.
     *
     * @param parameterName the JWK parameter that failed to decode
     * @param cause the underlying exception
     * @return SignatureGenerationException with metadata
     */
    public static SignatureGenerationException base64DecodingFailed(String parameterName, Throwable cause) {
        return (SignatureGenerationException) new SignatureGenerationException(
                String.format("Failed to decode Base64 parameter: %s", parameterName), cause)
                .addMetadata("parameter", parameterName);
    }

    @Override
    public SignatureGenerationException addMetadata(String key, Object value) {
        super.addMetadata(key, value);
        return this;
    }

    @Override
    public SignatureGenerationException addMetadata(Map<String, Object> additionalMetadata) {
        super.addMetadata(additionalMetadata);
        return this;
    }
}
