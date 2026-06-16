package com.wso2wrapper.credentials_generation.constants;

import java.util.List;

public class ExternalApiConstants {

        // -------------------- TOKEN API --------------------
        public static final String TOKEN_GRANT_TYPE = "password";
        public static final String TOKEN_SCOPE = "apim:subscribe apim:app_manage apim:app_import_export apim:api_key";

        // -------------------- REGISTER API --------------------
        public static final String REGISTER_REALM = "PRIMARY";
        public static final String REGISTER_GIVEN_NAME = "Test";
        public static final String REGISTER_EMAIL_URI = "http://wso2.org/claims/emailaddress";
        public static final String REGISTER_GIVENNAME_URI = "http://wso2.org/claims/givenname";

        // -------------------- APPLICATION --------------------
        public static final String APP_THROTTLING_POLICY = "Unlimited";
        public static final String APP_DESCRIPTION = "Sample application";
        public static final String APP_TOKEN_TYPE = "JWT";

        // -------------------- KEYS --------------------
        public static final String KEY_TYPE = "SANDBOX";
        public static final String KEY_MANAGER = "Resident Key Manager";
        public static final List<String> GRANT_TYPES = List.of("password", "client_credentials");
        public static final String CALLBACK_URL = "http://sample.com/callback/url";
        public static final List<String> SCOPES = List.of("am_application_scope", "default");
        public static final int VALIDITY_TIME = 3600;

        // -------------------- SUBSCRIPTIONS --------------------
        public static final String SUBSCRIPTION_THROTTLING_POLICY = "Unlimited";

        // =================== MongoDB Collection Names ===================
        public static final String DB_WSO2_TENANTS = "wso2_tenants";
        public static final String DB_WSO2_BUSINESS_APPLICATIONS = "wso2_business_applications";
        public static final String DB_BUSINESS_APPLICATIONS = "business_applications";

        public static final String DB_WSO2_DATA_PROCESSOR = "wso2_data_processors";

        public static final String DB_WSO2_AVAILABLE_APIS = "wso2_available_apis";

        // =================== Success / Info Messages ===================
        public static final String SUCCESS_TENANT_REGISTERED = "Tenant registered successfully";
        public static final String INFO_BUSINESS_ALREADY_ONBOARDED = "Business already onboarded";

        public static final String INFO_DATA_PROCESSOR_ALREADY_ONBOARDED = "Data Processor already onboarded";

        // =================== Default Patterns ===================
        public static final String DEFAULT_TENANT_PASSWORD_PATTERN = "%s@123";

        // =================== HTTP Header Keys ===================
        public static final String HEADER_AUTHORIZATION = "Authorization";
        public static final String HEADER_USERNAME = "Username";
        public static final String HEADER_ORG_EMAIL = "orgEmail";

        // =================== Environment Property Keys ===================
        public static final String ENV_TOKEN_API = "TOKEN_API";
        public static final String ENV_REGISTER_API = "REGISTER_API";
        public static final String ENV_CREATE_APPLICATION_URL = "CreateApplicationUrl";
        public static final String ENV_GENERATE_KEYS_URL = "Generate_Keys";
        public static final String ENV_MULTIPLE_SUBSCRIPTION_URL = "MULTIPLE_SUBSCRIPTION";
        public static final String ENV_TOKEN_AUTH = "TOKEN_AUTH";
        public static final String ENV_REGISTER_TOKEN = "REGISTER_TOKEN";
    }
    
