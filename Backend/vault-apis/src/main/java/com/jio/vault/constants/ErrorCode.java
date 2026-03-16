package com.jio.vault.constants;


public enum ErrorCode {
    INVALID_REQUEST("NV400", "Invalid request, check headers & body"),
    INTERNAL_ERROR("NV500", "Internal server error"),
    NOT_FOUND("NV404", "Resource not found"),
    KEY_NOTFOUND("NVK404", "The Key does not exist"),
    KEY_NOTMATCH("NVK405", "The key does not match with Tenant-ID & Business-ID"),
    ENCRYPTION_FAILED("NVEn500", "Encryption Failed"),
    DECRYPTION_FAILED("NVDe500", "Decryption Failed"),
    JWT_VALIDATION_FAILED("NVJWT400", "JWT validation failed"),
    TENANT_ID_EMPTY("NVT400", "Tenant ID Header is empty or not present"),
    BUSINESS_ID_EMPTY("NVB400", "Business ID Header is empty or not present"),
    KEY_ID_EMPTY("NVK400", "Key ID Header is empty or not present"),
    DATA_CATEGORY_VAL_EMPTY("NVC400", "data-category-value Header is empty or not present"),
    DATA_CATEGORY_EMPTY("NVC400", "data-category-type Header is empty or not present"),
    UUID_EMPTY("NVK400", "UUID Header is empty or not present"),;


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

