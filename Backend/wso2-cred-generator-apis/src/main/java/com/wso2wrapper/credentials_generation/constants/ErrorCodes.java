package com.wso2wrapper.credentials_generation.constants;

public class ErrorCodes {
    // ===== Tenant Related Errors =====
    public static final String ERR_REQUEST_NULL = "TENANT_001";
    public static final String ERR_TENANT_ID_REQUIRED = "TENANT_002";
    public static final String ERR_TENANT_NAME_REQUIRED = "TENANT_003";
    public static final String ERR_TENANT_NOT_FOUND = "TENANT_004";
    public static final String ERR_TENANT_ALREADY_EXISTS = "TENANT_005";
    public static final String ERR_TENANT_NAME_MISMATCH = "TENANT_006";

    public static final String ERR_TENANT_DB_NOT_FOUND = "TENANT_007";

    // ===== Business Related Errors =====
    public static final String ERR_BUSINESS_ID_REQUIRED = "BUSINESS_001";
    public static final String ERR_BUSINESS_NAME_NOT_FOUND = "BUSINESS_002";
    public static final String ERR_BUSINESS_ALREADY_ONBOARDED = "BUSINESS_003";

    public static final String ERR_BUSINESS_NAME_ALREADY_EXISTS = "BUSINESS_004";
    public static final String ERR_BUSINESS_NAME_UNMATCHED = "BUSINESS_005";



    //=================DATA PROCESSOR ===================

    public static final String ERR_DATA_PROCESSOR_ID_REQUIRED = "DATA_PROCESSOR_001";
    public static final String ERR_DATA_PROCESSOR_NAME_NOT_FOUND = "DATA_PROCESSOR_002";

    public static final String ERR_DATA_PROCESSOR_NAME_ALREADY_EXISTS = "DATA_PROCESSOR_003";
    public static final String ERR_DATA_PROCESSOR_NAME_UNMATCHED = "DATA_PROCESSOR_004";

    // ===== Authentication / WSO2 Related =====
    public static final String ERR_WS_REGISTRATION_FAILED = "WSO2_001";
    public static final String ERR_WS_TOKEN_FAILED = "WSO2_002";
    public static final String ERR_WS_APP_CREATION_FAILED = "WSO2_003";
    public static final String ERR_WS_KEY_GENERATION_FAILED = "WSO2_004";
    public static final String ERR_WS_SUBSCRIPTION_FAILED = "WSO2_005";

    // ================= Generic Errors =================
    public static final String UNKNOWN_ERROR = "NEGD_500";
    public static final String INVALID_REQUEST = "NEGD_400";
    public static final String MISSING_PARAM = "NEGD_401";

    private ErrorCodes() {}
}
