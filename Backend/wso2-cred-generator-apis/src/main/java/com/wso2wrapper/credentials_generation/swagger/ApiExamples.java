package com.wso2wrapper.credentials_generation.swagger;

public class ApiExamples {

    // ==================== TENANT SUCCESS ====================
    public static final String TENANT_SUCCESS = "{\n" +
            "  \"success\": true,\n" +
            "  \"message\": \"Tenant registered successfully\",\n" +
            "  \"code\": \"NEGD_200_SUCCESS\",\n" +
            "  \"data\": {\n" +
            "    \"tenantUniqueId\": \"7e19c8ea-5ffd-4a83-89ae-d190ce53acee\"\n" +
            "  },\n" +
            "  \"timestamp\": \"2025-10-03T13:55:12.271+00:00\"\n" +
            "}";

    // ==================== TENANT ERRORS ====================
    public static final String TENANT_ERROR_400 = "{\n" +
            "  \"success\": false,\n" +
            "  \"code\": \"TENANT_003\",\n" +
            "  \"message\": \"TenantName is required\",\n" +
            "  \"timestamp\": \"2025-10-03T13:59:08Z\"\n" +
            "}";

    public static final String TENANT_ERROR_401 = "{\n" +
            "  \"success\": false,\n" +
            "  \"code\": \"TENANT_002\",\n" +
            "  \"message\": \"Unauthorized access\",\n" +
            "  \"timestamp\": \"2025-10-03T14:00:00Z\"\n" +
            "}";

    public static final String TENANT_ERROR_404 = "{\n" +
            "  \"success\": false,\n" +
            "  \"code\": \"TENANT_007\",\n" +
            "  \"message\": \"TenantDB not found. Onboarding cannot proceed: tenant_db_7e19c8ea-df2f-43f2-8fe4-db94288523e\",\n" +
            "  \"timestamp\": \"2025-10-03T13:58:49Z\"\n" +
            "}";

    public static final String TENANT_ERROR_409 = "{\n" +
            "  \"success\": false,\n" +
            "  \"code\": \"TENANT_005\",\n" +
            "  \"message\": \"Tenant already registered\",\n" +
            "  \"timestamp\": \"2025-10-03T13:49:34Z\"\n" +
            "}";

    public static final String TENANT_ERROR_500 = "{\n" +
            "  \"success\": false,\n" +
            "  \"code\": \"NEGD_500\",\n" +
            "  \"message\": \"Internal server error while creating tenant\",\n" +
            "  \"timestamp\": \"2025-10-03T14:01:00Z\"\n" +
            "}";

    // ==================== BUSINESS SUCCESS ====================
    public static final String BUSINESS_SUCCESS = "{\n" +
            "  \"success\": true,\n" +
            "  \"message\": \"Successfully onboarded\",\n" +
            "  \"code\": \"NEGD_200_SUCCESS\",\n" +
            "  \"data\": {\n" +
            "    \"consumerKey\": \"1afDMfcSBgsAw67kQdJuO6LU6kYa\",\n" +
            "    \"consumerSecret\": \"KaSHzNuNLO2AGAe4jFVseCquNY0a\",\n" +
            "    \"businessUniqueId\": \"54ec38c4-54ec-b0c2-3413-1705b1c6df97\"\n" +
            "  },\n" +
            "  \"timestamp\": \"2025-10-03T13:55:12.271+00:00\"\n" +
            "}";

    public static final String BUSINESS_ALREADY_ONBOARDED = "{\n" +
            "  \"success\": true,\n" +
            "  \"message\": \"Successfully onboarded\",\n" +
            "  \"code\": \"NEGD_200_SUCCESS\",\n" +
            "  \"data\": {\n" +
            "    \"consumerKey\": \"aTyDuIF17gOczDlEMOy0K4WpXS8a\",\n" +
            "    \"consumerSecret\": \"ZhjdDe6ZyNig4hXhDBjo6vuBugoa\",\n" +
            "    \"businessUniqueId\": \"110f90a0-110f-6b3a-d918-18d0d684bc8b\",\n" +
            "    \"message\": \"Business already onboarded\"\n" +
            "  },\n" +
            "  \"timestamp\": \"2025-10-03T14:00:39.695Z\"\n" +
            "}";

    // ==================== BUSINESS ERRORS ====================
    public static final String BUSINESS_ERROR_400 = "{\n" +
            "  \"success\": false,\n" +
            "  \"code\": \"BUSINESS_001\",\n" +
            "  \"message\": \"BusinessId is required\",\n" +
            "  \"timestamp\": \"2025-10-03T13:59:32.228511979Z\"\n" +
            "}";

    public static final String BUSINESS_ERROR_401 = "{\n" +
            "  \"success\": false,\n" +
            "  \"code\": \"BUSINESS_002\",\n" +
            "  \"message\": \"Unauthorized access\",\n" +
            "  \"timestamp\": \"2025-10-03T14:00:00Z\"\n" +
            "}";

    public static final String BUSINESS_ERROR_404 = "{\n" +
            "  \"success\": false,\n" +
            "  \"code\": \"BUSINESS_003\",\n" +
            "  \"message\": \"BusinessId not found in tenantDB\",\n" +
            "  \"timestamp\": \"2025-10-03T14:00:12Z\"\n" +
            "}";

    public static final String BUSINESS_ERROR_409 = "{\n" +
            "  \"success\": false,\n" +
            "  \"code\": \"BUSINESS_004\",\n" +
            "  \"message\": \"Business already onboarded\",\n" +
            "  \"timestamp\": \"2025-10-03T14:00:30Z\"\n" +
            "}";

    public static final String BUSINESS_ERROR_500 = "{\n" +
            "  \"success\": false,\n" +
            "  \"code\": \"NEGD_500\",\n" +
            "  \"message\": \"Internal server error while onboarding business\",\n" +
            "  \"timestamp\": \"2025-10-03T14:01:12Z\"\n" +
            "}";



    //==================== DATA PROCESSOR SUCCESS ====================
    public static final String DATA_PROCESSOR_SUCCESS = "{\n" +
            "  \"success\": true,\n" +
            "  \"message\": \"Successfully onboarded\",\n" +
            "  \"code\": \"NEGD_200_SUCCESS\",\n" +
            "  \"data\": {\n" +
            "    \"consumerKey\": \"1afDMfcSBgsAw67kQdJuO6LU6kYa\",\n" +
            "    \"consumerSecret\": \"KaSHzNuNLO2AGAe4jFVseCquNY0a\",\n" +
            "    \"dataProcessorUniqueId\": \"54ec38c4-54ec-b0c2-3413-1705b1c6df97\"\n" +
            "  },\n" +
            "  \"timestamp\": \"2025-10-03T13:55:12.271+00:00\"\n" +
            "}";

    public static final String DATA_PROCESSOR_ALREADY_ONBOARDED = "{\n" +
            "  \"success\": true,\n" +
            "  \"message\": \"Successfully onboarded\",\n" +
            "  \"code\": \"NEGD_200_SUCCESS\",\n" +
            "  \"data\": {\n" +
            "    \"consumerKey\": \"aTyDuIF17gOczDlEMOy0K4WpXS8a\",\n" +
            "    \"consumerSecret\": \"ZhjdDe6ZyNig4hXhDBjo6vuBugoa\",\n" +
            "    \"dataProcessorUniqueId\": \"110f90a0-110f-6b3a-d918-18d0d684bc8b\",\n" +
            "    \"message\": \"Data Processor already onboarded\"\n" +
            "  },\n" +
            "  \"timestamp\": \"2025-10-03T14:00:39.695Z\"\n" +
            "}";

    // ==================== DATA PROCESSOR ERRORS ====================
    public static final String DATA_PROCESSOR_ERROR_400 = "{\n" +
            "  \"success\": false,\n" +
            "  \"code\": \"DP_001\",\n" +
            "  \"message\": \"dataProcessorId is required\",\n" +
            "  \"timestamp\": \"2025-10-03T13:59:32.228511979Z\"\n" +
            "}";

    public static final String DATA_PROCESSOR_ERROR_401 = "{\n" +
            "  \"success\": false,\n" +
            "  \"code\": \"DP_002\",\n" +
            "  \"message\": \"Unauthorized access\",\n" +
            "  \"timestamp\": \"2025-10-03T14:00:00Z\"\n" +
            "}";

    public static final String DATA_PROCESSOR_ERROR_404 = "{\n" +
            "  \"success\": false,\n" +
            "  \"code\": \"DP_003\",\n" +
            "  \"message\": \"dataProcessorId not found in tenantDB\",\n" +
            "  \"timestamp\": \"2025-10-03T14:00:12Z\"\n" +
            "}";

    public static final String DATA_PROCESSOR_ERROR_409 = "{\n" +
            "  \"success\": false,\n" +
            "  \"code\": \"DP_004\",\n" +
            "  \"message\": \"Data Processor already onboarded\",\n" +
            "  \"timestamp\": \"2025-10-03T14:00:30Z\"\n" +
            "}";

    public static final String DATA_PROCESSOR_ERROR_500 = "{\n" +
            "  \"success\": false,\n" +
            "  \"code\": \"NEGD_500\",\n" +
            "  \"message\": \"Internal server error while onboarding data processor\",\n" +
            "  \"timestamp\": \"2025-10-03T14:01:12Z\"\n" +
            "}";


}
