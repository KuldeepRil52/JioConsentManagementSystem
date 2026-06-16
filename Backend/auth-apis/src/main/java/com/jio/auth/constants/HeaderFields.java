package com.jio.auth.constants;


public final class HeaderFields {

    private HeaderFields() {
        // Prevent instantiation
    }

    public static final String SESSION_TOKEN = "x-session-token";
    public static final String AUTHORIZATION = "authorization";
    public static final String TENANT_ID = "tenantid";
    public static final String BUSINESS_ID = "businessid";
    public static final String USER_ID = "userid";
    public static final String ACCESS_TOKEN = "accesstoken";
    public static final String IDENTITY = "identity";
    public static final String IDENTITY_VALUE = "identityvalue";
    public static final String TENANT_ID_CODE = "tenant-id";
    public static final String BUSINESS_ID_CODE = "business-id";
    public static final String REQUESTOR_TYPE = "requestor-type";
    public static final String SIGNATURE =  "x-jws-signature";
    public static final String  SECRET_CODE = "secret-code";


}