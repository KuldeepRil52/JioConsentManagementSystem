package com.jio.consent.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Entity representing a JWK (JSON Web Key) for RSA signature operations.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@Document(collection = "auth_key")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class JwkKey extends AbstractEntity {

    @Id
    private ObjectId id;

    @Indexed(name = "tenantId")
    private String tenantId;

    @Indexed(name = "kty")
    private String kty; // Key type (e.g., "RSA")

    @Indexed(name = "use")
    private String use; // Key use (e.g., "sig")

    @Indexed(name = "kid")
    private String kid; // Key ID

    // RSA public key parameters (Base64URL encoded)
    private String n; // Modulus
    private String e; // Public exponent

    // RSA private key parameters (Base64URL encoded)
    private String d; // Private exponent
    private String p; // First prime factor
    private String q; // Second prime factor
    private String dp; // First factor CRT exponent
    private String dq; // Second factor CRT exponent
    private String qi; // First CRT coefficient
}

