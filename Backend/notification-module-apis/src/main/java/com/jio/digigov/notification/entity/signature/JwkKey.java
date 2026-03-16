package com.jio.digigov.notification.entity.signature;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Entity representing a JSON Web Key (JWK) for RSA signature generation.
 *
 * <p>This entity stores JWK keys in the 'auth_key' MongoDB collection.
 * Keys are used for signing responses with RSA256 algorithm.</p>
 *
 * <p><b>JWK Structure:</b></p>
 * <ul>
 *   <li><b>kid</b> - Key ID (unique identifier)</li>
 *   <li><b>kty</b> - Key Type (e.g., RSA, EC, oct)</li>
 *   <li><b>use</b> - Intended use (sig for signature, enc for encryption)</li>
 *   <li><b>alg</b> - Algorithm (e.g., RS256, ES256)</li>
 *   <li><b>n</b> - RSA Modulus (Base64URL encoded)</li>
 *   <li><b>e</b> - RSA Public Exponent (Base64URL encoded)</li>
 *   <li><b>d</b> - RSA Private Exponent (Base64URL encoded)</li>
 *   <li><b>p, q</b> - RSA Prime factors</li>
 *   <li><b>dp, dq, qi</b> - RSA CRT (Chinese Remainder Theorem) parameters</li>
 *   <li><b>x, y</b> - EC (Elliptic Curve) coordinates</li>
 *   <li><b>crv</b> - EC Curve name</li>
 *   <li><b>x5c, x5t, x5u</b> - X.509 certificate chain and thumbprint</li>
 * </ul>
 *
 * @see <a href="https://tools.ietf.org/html/rfc7517">RFC 7517 - JSON Web Key (JWK)</a>
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "auth_key")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwkKey {

    /**
     * Key ID - unique identifier for the key.
     * This serves as the document ID in MongoDB.
     */
    @Id
    private String kid;

    /**
     * Key Type - type of cryptographic key.
     * Common values: RSA, EC, oct
     */
    private String kty;

    /**
     * Public key use - intended use of the key.
     * Values: sig (signature), enc (encryption)
     */
    private String use;

    /**
     * Algorithm - cryptographic algorithm intended for use with the key.
     * Common values: RS256, RS384, RS512, ES256, HS256
     */
    private String alg;

    /**
     * Key operations - allowed operations for the key.
     * Values: sign, verify, encrypt, decrypt, wrapKey, unwrapKey, deriveKey, deriveBits
     */
    private String[] key_ops;

    // RSA Key Parameters

    /**
     * Modulus - RSA public key modulus (Base64URL encoded).
     */
    private String n;

    /**
     * Exponent - RSA public key exponent (Base64URL encoded).
     * Typically "AQAB" (65537 in decimal)
     */
    private String e;

    /**
     * Private Exponent - RSA private key exponent (Base64URL encoded).
     * SENSITIVE: Used for signing operations
     */
    private String d;

    /**
     * First Prime Factor - first prime number in RSA factorization (Base64URL encoded).
     * SENSITIVE: Part of private key
     */
    private String p;

    /**
     * Second Prime Factor - second prime number in RSA factorization (Base64URL encoded).
     * SENSITIVE: Part of private key
     */
    private String q;

    /**
     * First Factor CRT Exponent - d mod (p-1) (Base64URL encoded).
     * SENSITIVE: Chinese Remainder Theorem parameter for optimization
     */
    private String dp;

    /**
     * Second Factor CRT Exponent - d mod (q-1) (Base64URL encoded).
     * SENSITIVE: Chinese Remainder Theorem parameter for optimization
     */
    private String dq;

    /**
     * First CRT Coefficient - (inverse of q) mod p (Base64URL encoded).
     * SENSITIVE: Chinese Remainder Theorem parameter for optimization
     */
    private String qi;

    // Elliptic Curve Key Parameters

    /**
     * X Coordinate - EC public key X coordinate (Base64URL encoded).
     */
    private String x;

    /**
     * Y Coordinate - EC public key Y coordinate (Base64URL encoded).
     */
    private String y;

    /**
     * Curve - EC curve name.
     * Common values: P-256, P-384, P-521
     */
    private String crv;

    // X.509 Certificate Parameters

    /**
     * X.509 Certificate Chain - array of Base64-encoded DER certificates.
     * First certificate contains the public key, subsequent certificates form the chain.
     */
    private String[] x5c;

    /**
     * X.509 Certificate SHA-1 Thumbprint (Base64URL encoded).
     */
    private String x5t;

    /**
     * X.509 Certificate SHA-256 Thumbprint (Base64URL encoded).
     */
    private String x5t_S256;

    /**
     * X.509 URL - URL pointing to X.509 certificate or certificate chain.
     */
    private String x5u;
}
