package com.jio.multitranslator.constant;

/**
 * Constants used throughout the application.
 */
public final class Constants {

    private Constants() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** HTTP Header names */
    public static final String TENANT_ID_HEADER = "tenantid";
    public static final String BUSINESS_ID_HEADER = "businessid";
    public static final String TNX_ID = "txn";
    public static final String VERSION_HEADER = "version";

    /** API Request Header names */
    public static final String USER_ID_HEADER = "userID";
    public static final String ULCA_API_KEY_HEADER = "ulcaApiKey";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** Error response field names */
    public static final String ERROR_CODE = "errorCode";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String ERRORS = "errors";
    public static final String TIMESTAMP = "timestamp";
    public static final String HEADER = "header";
    public static final String BODY = "body";
    public static final String PARAMETER = "parameter";

    /** Configuration field names */
    public static final String APIBASEURL = "apiBaseUrl";
    public static final String MODELPIPELINEENDPOINT = "modelPipelineEndpoint";
    public static final String CALLBACKURL = "callbackUrl";
    public static final String USERID = "userId";
    public static final String APIKEY = "apiKey";
    public static final String PIPELINEID = "pipelineId";
    public static final String SUBSCRIPTIONKEY = "subscriptionKey";
    public static final String REGION = "region";
    public static final String CONFIG = "config";
    public static final String LANGUAGE = "language";

    /** Error messages */
    public static final String DUP_BUSINESSID = "Translate Configuration already exists for given BusinessId header in the request.";

    /** Task types */
    public static final String TASK_TYPE_TRANSLATION = "translation";

    /** MongoDB field names */
    public static final String MONGO_ID_FIELD = "_id";
    public static final String MONGO_BUSINESS_ID_FIELD = "businessId";
    public static final String MONGO_TENANT_ID_FIELD = "tenantId";
    public static final String MONGO_SOURCE_LANGUAGE_FIELD = "sourceLanguage";
    public static final String MONGO_TARGET_LANGUAGE_FIELD = "targetLanguage";
    public static final String MONGO_CONFIG_PROVIDER_FIELD = "config.provider";

    /** Performance thresholds */
    public static final long SLOW_API_CALL_THRESHOLD_MS = 5000L;

    /** Package prefixes */
    public static final String JAVA_PACKAGE_PREFIX = "java.";
    public static final String APPLICATION_PACKAGE_PREFIX = "com.jio.multitranslator";

    /** Supported language codes */
    public static final java.util.Set<String> SUPPORTED_LANGUAGES = java.util.Set.of(
            "en", "as", "bn", "brx", "doi", "gom", "gu", "hi", "kn", "ks",
            "mai", "ml", "mni", "mr", "ne", "or", "pa", "sa", "sat", "sd",
            "ta", "te", "ur"
    );
}
