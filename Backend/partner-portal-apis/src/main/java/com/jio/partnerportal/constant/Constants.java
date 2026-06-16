package com.jio.partnerportal.constant;

import java.util.List;

public class Constants {

    private Constants() {
    }

    public static final String PAN = "pan";
    public static final String COMPANY_NAME = "companyName";
    public static final String LOGO_URL = "logoUrl";
    public static final String IDENTITY_TYPE = "identityType";
    public static final String EMAIL = "email";
    public static final String MOBILE = "mobile";
    public static final String ERROR_CODE = "errorCode";
    public static final String PARAMETER = "parameter";
    public static final String LEGAL_ADMIN_DESCRIPTION = "Legal Admin Role";
    public static final String SPOC = "spoc";
    public static final String TXN_ID = "txn";
    public static final String HEADER = "header";
    public static final String TENANT_ID_HEADER = "tenant-id";
    public static final String BUSINESS_ID_HEADER = "business-id";
    public static final String SCOPE_LEVEL_HEADER = "scope-level";
    public static final String PROVIDER_TYPE_HEADER = "provider-type";
    public static final String PURPOSE_NAME = "purposeName";
    public static final String PURPOSE_CODE = "purposeCode";
    public static final String PURPOSE_DESCRIPTION = "purposeDescription";
    public static final String DATA_ITEMS = "dataItems";
    public static final String DATA_TYPE_NAME = "dataTypeName";
    public static final String DATA_PROCESSOR_NAME = "dataProcessorName";
    public static final String CALLBACK_URL = "callbackUrl";
    public static final String DETAILS = "details";
    public static final String ATTACHMENT = "attachment";
    public static final String CONTENT_TYPE = "contentType";
    public static final String ACTIVITY_NAME = "activityName";
    public static final String PROCESSOR_NAME = "processorName";
    public static final String DATA_TYPE_ID = "dataTypeId";
    public static final String STATUS = "status";
    public static final String DATA_TYPE_LIST = "dataTypeList";
    public static final String NAME = "name";

    public static final String ATTACHMENT_META = "attachmentMeta";
    public static final String SIZE = "size";
    public static final String TAG = "tag";
    public static final String DOCUMENT_TAG = "documentTag";
    public static final String COMPONENTS = "components";
    public static final String VENDOR_RISK_DOCUMENT = "vendorRiskDocument";

    public static final String SUCCESS = "Success";

    public static final String TXN = "txnId";
    public static final String VALIDATED = "VALIDATED";
    public static final String USERS = "users";

    public static final String USER_ID = "userId";
    public static final String FAILED = "FAILED";
    public static final String GRANT_TYPE = "grant_type";
    public static final String CLIENT_CRED = "client_credentials";
    public static final String AUTHORIZATION = "Authorization";
    public static final String NOTIFICATION_TOKEN_AUTH = "NOTIFICATION_TOKEN_AUTH";
    public static final String OTP_SID = "OTP_SID";
    public static final String BEARER = "Bearer ";
    public static final String ROLES = "roles";
    public static final String PERMISSIONS = "permissions";
    public static final String ROLE_ID = "roleId";
    public static final String ERROR = "error";

    public static final String BUSINESS_APPLICATIONS = "business_applications";

    public static final String RETENTION_CONFIG= "retention_config";

    public static final List<String> EXCLUDED_PATHS = List.of(
            "/partnerportal/v1.0/tenant/onboard",

            //Swagger
            "/partnerportal/swagger-ui.html",
            "/partnerportal/swagger-ui",
            "/partnerportal/v3/api-docs",
            "/partnerportal/webjars",
            "/partnerportal/v1.0/tenant/onboard",

            //Registration OTP Api
            "/partnerportal/otp/init",
            "/partnerportal/otp/validate",

            //Login OTP Api
            "/partnerportal/tenant/otp/init",
            "/partnerportal/tenant/otp/validate",

            //Generate token
            "/partnerportal/role/generate"


    );
    public static final List<String> INCLUDED_PATHS = List.of(
            //With TenantId and no xsessiontoken
            "/partnerportal/v1.0/business-application/search",
            "/partnerportal/v1.0/system-config/search",
            "/partnerportal/v1.0/digilocker/credential/search",
            "/partnerportal/v1.0/dashboard/data"
    );


    //---------------- Notification Constants ------------------
    public static final String INIT_TXN_ID2 = "1234567890ASDFGHJKL1234560";

    public static final String HEADER_TXN = "txn";
    public static final String OTP_TXN_ID = "1234567890qwertyuiop123456";
    public static final String HEADER_SID = "sid";
    public static final String INIT_ID_TYPE = "idType";
    public static final String INIT_ID_VALUE = "idValue";
    public static final String INIT_TXN_ID = "txnId";
    public static final String SYSTM_NAME = "systemName";
    public static final String SYS_NAME = "JioConsentManager";

    // DigiLocker Credential Constants
    public static final String CLIENT_ID = "clientId";

    // FortifyFuture: HardcodedPassword false positive — this is a field name, not a credential
    @SuppressWarnings("fortify:hardcoded-password")
    public static final String CLIENT_SECRET = "clientSecret";
    public static final String REDIRECT_URI = "redirectUri";
    public static final String CODE_VERIFIER = "codeVerifier";

    // Data Breach Report Constants
    public static final String INCIDENT_DETAILS = "incidentDetails";
    public static final String DATA_INVOLVED = "dataInvolved";
    public static final String DISCOVERY_DATE_TIME = "discoveryDateTime";
    public static final String OCCURRENCE_DATE_TIME = "occurrenceDateTime";
    public static final String BREACH_TYPE = "breachType";
    public static final String BRIEF_DESCRIPTION = "briefDescription";
    public static final String AFFECTED_SYSTEM_OR_SERVICE = "affectedSystemOrService";
    public static final String PERSONAL_DATA_CATEGORIES = "personalDataCategories";
    public static final String AFFECTED_DATA_PRINCIPALS_COUNT = "affectedDataPrincipalsCount";
    public static final String DATA_ENCRYPTED_OR_PROTECTED = "dataEncryptedOrProtected";
    public static final String POTENTIAL_IMPACT_DESCRIPTION = "potentialImpactDescription";
    public static final String REMARKS = "remarks";
    public static final String TYPOGRAPHY_SETTINGS = "typographySettings";
    public static final String FONT_FILE_ABSENT = "fontFile absent";
    public static final String INVALID_FONT_FILE = "Invalid fontFile";
    

    // Audit Meta Constants
    public static final String DATA_FIDUCIARY = "DATA_FIDUCIARY";
    public static final String SOURCE_IP = "SOURCE_IP";
    public static final String TXN_ID_THREAD_CONTEXT = "TXN_ID";
    public static final String USER_ID_THREAD_CONTEXT = "userId";
    public static final String USER_ID_TYPE = "USER_ID";
    public static final String USER = "USER";
    public static final String PARTNER_PORTAL_GROUP = "PARTNER-PORTAL";
    public static final String DATA = "data";
    public static final String BUSINESS_ID = "BUSINESS_ID";

    public static final String CONFIG_ID = "configId";
    public static final String BUSINESS_UNIQUE_ID = "businessUniqueId";
    public static final String INCIDENT_ID = "incidentId";
    public static final String DATA_PROCESSOR_ID = "dataProcessorId";
    public static final String DATA_TYPE_ID_CONSTANT = "dataTypeId";
    public static final String CREDENTIAL_ID = "credentialId";
    public static final String LEGAL_ENTITY_ID = "legalEntityId";
    public static final String PROCESSOR_ACTIVITY_ID = "processorActivityId";
    public static final String PURPOSE_ID = "purposeId";
    public static final String ROPA_ID = "ropaId";
    public static final String LICENCE_ID = "licenceId";
    public static final String TENANT_ID = "tenantId";
    public static final String ROLE_ID_CONSTANT = "roleId";
    public static final String USER_ID_CONSTANT = "userId";
}