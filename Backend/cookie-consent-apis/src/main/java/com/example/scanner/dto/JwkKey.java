package com.example.scanner.dto;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Document(collection = "auth_key")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwkKey {

    @Id
    private String kid;   // Key ID

    private String kty;   // Key Type (RSA, EC, oct)
    private String use;   // Intended use: sig, enc
    private String alg;   // Algorithm: RS256, ES256, etc.
    private String[] key_ops; // Optional operations

    private String n;   // Modulus (RSA)
    private String e;   // Exponent (RSA)
    private String d;   // Private exponent
    private String p;   // Prime 1
    private String q;   // Prime 2
    private String dp;  // d mod (p-1)
    private String dq;  // d mod (q-1)
    private String qi;  // (1/q) mod p
    private String x;   // EC X coordinate
    private String y;   // EC Y coordinate
    private String crv; // Curve name (for EC)

    private String[] x5c;
    private String x5t;
    private String x5u;

}