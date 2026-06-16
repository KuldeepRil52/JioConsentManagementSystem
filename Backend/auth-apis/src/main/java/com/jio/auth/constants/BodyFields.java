package com.jio.auth.constants;


public final class BodyFields {



    private BodyFields() {
        // Prevent instantiation
    }

    public static final String ISSUER = "iss";         // Issuer
    public static final String SUBJECT = "sub";        // Subject
    public static final String AUDIENCE = "aud";       // Audience
    public static final String EXPIRATION = "exp";     // Expiration Time
    public static final String NOT_BEFORE = "nbf";     // Not Before
    public static final String ISSUED_AT = "iat";      // Issued At
    public static final String JWT_ID = "jti";         // JWT ID

    public static final String USERNAME = "username";
    public static final String EMAIL = "email";
    public static final String ROLE = "role";
    public static final String NAME = "name";
    public static final String SURNAME = "surname";
    public static final String PASSWORD = "password"; // if used in your payload (not recommended)
    public static final String TENANT_ID = "tenantId";
    public static final String BUSINESS_ID = "businessId";
    public static final String IDENTITY_TYPE = "identityType";
    public static final String IDENTITY = "identityValue";
    public static final String ORGANIZATION = "organization";
    public static final String PERMISSIONS = "permissions";
}

