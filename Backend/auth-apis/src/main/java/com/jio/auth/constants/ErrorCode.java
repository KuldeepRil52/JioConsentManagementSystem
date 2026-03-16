package com.jio.auth.constants;

public enum ErrorCode {
    INVALID_REQUEST("AUTH400", "Invalid request, check headers & body"),
    INTERNAL_ERROR("AUTH500", "Internal server error"),
    UNAUTHORIZED("AUTH401", "Unauthorized Request"),
    NOT_FOUND("AUTH404", "Resource not found"),
    DB_ERROR("AUTHDB500", "DB error"),
    UNKNOWN_ERROR("AUTHDB500", "DB error"),
    NO_TOKEN_HEADER("AUTH400", "Request does not contain x-session-token"),
    UNSUPPORTED_MEDIA_TYPE("AUTH415", "Unsupported Media Type"),
    MISSING_SUB("AUTH400", "Missing/Empty Subject(sub) in the body"),
    MISSING_ISSUER("AUTH400", "Missing/Empty Issuer(iss) in the body"),
    MISSING_TENANT_ID("AUTH400", "Missing/Empty tenantId in the body"),
    MISSING_BUSINESS_ID("AUTH400", "Missing/Empty businessId in the body"),
    MISSING_IDENTITY("AUTH400", "Missing/Empty identityValue in the header/body"),
    MISSING_IDENTITY_TYPE("AUTH400", "Missing/Empty identityType in the body"),

    INVALID_TENANT_ID("AUTH400", "Invalid tenantId"),
    INVALID_BUSINESS_ID("AUTH400", "Invalid businessId"),
    USER_NOT_FOUND("AUTH400", "User not found for given identity"),
    MISSING_ACCESS_TOKEN("AUTH400", "Missing access token in the header"),
    SESSION_EXPIRED("AUTH400", "Session expired" ),;


    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

