const BASE_URL = process.env.REACT_APP_API_URL;
const BASE_URL_LOGIN_SIGNUP = process.env.REACT_APP_LOGIN_SIGNUP_URL;
const GRIEVANCE_BASE_URL = process.env.REACT_APP_GRIEVANCE_URL;
const CONSENT_URL = process.env.REACT_APP_CONSENT_URL;
const ENC_URL = process.env.REACT_APP_ENCRYPT_URL;
const COOKIE_CATEGORY_BASE_URL = process.env.REACT_APP_COOKIE_URL;
const AUDIT_BASE_URL = process.env.REACT_APP_AUDIT_URL;
const TRANSLATOR_BASE_URL = process.env.REACT_APP_TRANSLATOR_URL;
const NOTIFICATION_BASE_URL = process.env.REACT_APP_NOTIFICATION_URL;
const INTEGRATION_BASE_URL = process.env.REACT_APP_INTEGRATION_BASE_URL;
const BASE_URL_SCHEDULAR = process.env.REACT_APP_SCHEDULAR_URL;
const BASE_URL_RETENTION = process.env.REACT_APP_API_URL;
const REGISTRY_BASE_URL = process.env.REACT_APP_REGISTRY_URL;

// Debug: Log environment variables to help diagnose undefined URLs
if (!BASE_URL) {
  console.error('❌ REACT_APP_API_URL is not defined! Make sure you are using "npm run start:dev" (not "npm start") to load environment variables from .env.dev');
}
if (!CONSENT_URL) {
  console.error('❌ REACT_APP_CONSENT_URL is not defined! Make sure you are using "npm run start:dev" (not "npm start") to load environment variables from .env.dev');
}
if (!GRIEVANCE_BASE_URL) {
  console.error('❌ REACT_APP_GRIEVANCE_URL is not defined! Make sure you are using "npm run start:dev" (not "npm start") to load environment variables from .env.dev');
}

console.log('🔧 Config loaded:', {
  BASE_URL,
  CONSENT_URL,
  GRIEVANCE_BASE_URL,
  COOKIE_CATEGORY_BASE_URL,
  AUDIT_BASE_URL,
  TRANSLATOR_BASE_URL,
  NOTIFICATION_BASE_URL,
  BASE_URL_SCHEDULAR,
  BASE_URL_RETENTION,
});

const get_cookie_categories = `${COOKIE_CATEGORY_BASE_URL}/category`;
const create_cookie_category = `${COOKIE_CATEGORY_BASE_URL}/category`;
const update_cookie_category = `${COOKIE_CATEGORY_BASE_URL}/category`;
const get_audit_reports = `${AUDIT_BASE_URL}/audit/v1/audit`;
const check_integrity = `${AUDIT_BASE_URL}/audit/v1/consent/checkIntegrity`;
const login_init_otp = `${BASE_URL_LOGIN_SIGNUP}/tenant/otp/init`;
const login_validate_otp = `${BASE_URL_LOGIN_SIGNUP}/tenant/otp/validate`;
const create_data_protection_officer = `${BASE_URL}/v1.0/dpo/create`;
const get_data_protection_officer = `${BASE_URL}/v1.0/dpo/search`;
const update_data_protection_officer = `${BASE_URL}/v1.0/dpo/update/`;
const create_system_config = `${BASE_URL}/v1.0/system-config/create`;
const get_system_config = `${BASE_URL}/v1.0/system-config/search`;
const update_system_config = `${BASE_URL}/v1.0/system-config/update/`;
const create_grievance_config = `${BASE_URL}/v1.0/grievance/create`;
const create_consent_set_up_config = `${BASE_URL}/v1.0/consent/create`;
const create_digilocker_config = `${BASE_URL}/v1.0/digilocker/credential/create`;
const create_purpose = `${BASE_URL}/v1.0/purpose/create`;
const create_userTypes = `${GRIEVANCE_BASE_URL}/api/v1/user-types`;
const create_grievance_type = `${GRIEVANCE_BASE_URL}/api/v1/grievance-types`;
const get_userTypes = `${GRIEVANCE_BASE_URL}/api/v1/user-types`;
const create_userDetails = `${GRIEVANCE_BASE_URL}/api/v1/user-details`;
const get_userDetails = `${GRIEVANCE_BASE_URL}/api/v1/user-details`;
const get_grievance_type = `${GRIEVANCE_BASE_URL}/api/v1/grievance-types`;
const create_grievance_template = `${GRIEVANCE_BASE_URL}/api/v1/grievance-templates`;
const update_purpose = `${BASE_URL}/v1.0/purpose/update/`;
const search_purpose = `${BASE_URL}/v1.0/purpose/search`;
const search_dataTypes = `${BASE_URL}/v1.0/data-type/search`;
const create_dataType = `${BASE_URL}/v1.0/data-type/create`;
const update_dataType = `${BASE_URL}/v1.0/data-type/update/`;
const search_processor = `${BASE_URL}/v1.0/data-processor/search`;
const create_processor = `${BASE_URL}/v1.0/data-processor/create`;
const update_processor = `${BASE_URL}/v1.0/data-processor/update/`;
const search_processingActivity = `${BASE_URL}/v1.0/processor-activity/search`;
const create_processingActiity = `${BASE_URL}/v1.0/processor-activity/create`;
const update_processingActiity = `${BASE_URL}/v1.0/processor-activity/update/`;
const login_get_profile = `${BASE_URL}/users/profile`;
const get_consent_details = `${BASE_URL}/v1.0/consent/search`;
const get_digilocker_details = `${BASE_URL}/v1.0/digilocker/credential/search`;
const get_grievance_details = `${BASE_URL}/v1.0/grievance/search`;
const get_notif_details = `${BASE_URL}/v1.0/notification/search`;
const update_consent_config = `${BASE_URL}/v1.0/consent/update/`;
const update_notif_config = `${BASE_URL}/v1.0/notification/update/`;
const update_grievance_config = `${BASE_URL}/v1.0/grievance/update/`;
const create_business = `${BASE_URL}/v1.0/business-application/create`;
const create_role = `${BASE_URL}/role/create`;

// User APIs
const createUser = `${BASE_URL}/users/create`;
const updateUser = `${BASE_URL}/users/update`;
const searchUsers = `${BASE_URL}/users/search`;
const listUsers = `${BASE_URL}/users/list`;
const deleteUser = `${BASE_URL}/users/delete`;

// Role APIs
const updateRole = `${BASE_URL}/role/update`;
const searchRoles = `${BASE_URL}/role/search`;
const listRoles = `${BASE_URL}/role/list`;
const deleteRole = `${BASE_URL}/role/delete`;
const listComponents = `${BASE_URL}/role/component/list`;

const public_key = `MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCgFGVfrY4jQSoZQWWygZ83roKXWD4YeT2x2p41dGkPixe73rT2IW04glagN2vgoZoHuOPqa5and6kAmK2ujmCHu6D1auJhE2tXP+yLkpSiYMQucDKmCsWMnW9XlC5K7OSL77TXXcfvTvyZcjObEz6LIBRzs6+FqpFbUO9SJEfh6wIDAQAB`;

const create_template = `${CONSENT_URL}/v1.0/templates/create`;
const edit_template = `${CONSENT_URL}/v1.0/templates/update/`;
const template_details = `${CONSENT_URL}/v1.0/templates/search`;
const getConsentsByTemplateId = `${CONSENT_URL}/v1.0/consent/search`;
const getGrievanceByTemplateId = `${BASE_URL}/v1.0/grievance`;
const getPendingConsentsByTemplateId = `${CONSENT_URL}/v1.0/consent-handle/search`;
const getConsentHandleDetailsByConsentHandleId = `${CONSENT_URL}/v1.0/consent-handle/get`;
const getConsentCountsByTemplateId = `${CONSENT_URL}/v1.0/consent/count-status-by-params`;
const searchBusiness = `${BASE_URL}/v1.0/business-application/search`;
const getGrievnaceTemplateData = `${GRIEVANCE_BASE_URL}/api/v1/grievance-templates`;

// Additional endpoints from CommonAction.js
const tenant_onboard = `${BASE_URL}/v1.0/tenant/onboard`;
const otp_init = `${BASE_URL_LOGIN_SIGNUP}/otp/init`;
const otp_validate = `${BASE_URL_LOGIN_SIGNUP}/otp/validate`;
const users_profile = `${BASE_URL}/users/profile`;
const notification_create = `${BASE_URL}/v1.0/notification/create`;
const smtp_create = `${BASE_URL}/v1.0/smtp/create`;
const smtp_search = `${BASE_URL}/v1.0/smtp/search`;
const smtp_update = `${BASE_URL}/v1.0/smtp/update`;
const business_application_search = `${BASE_URL}/v1.0/business-application/search`;
const templates_create = `${CONSENT_URL}/v1.0/templates/create`;
const templates_search = `${CONSENT_URL}/v1.0/templates/search`;
const grievance_templates_list = `${GRIEVANCE_BASE_URL}/api/v1/grievance-templates/list`;
const translator_translate = `${TRANSLATOR_BASE_URL}/translate`;
const translator_translateConfig = `${TRANSLATOR_BASE_URL}/translateConfig`;
const client_credential = `${BASE_URL}/v1.0/client-credential`;
const grievances_search = `${GRIEVANCE_BASE_URL}/api/v1/grievances`;

// Component-specific endpoints
const cookie_base = `${COOKIE_CATEGORY_BASE_URL}`;
const consent_search = `${CONSENT_URL}/v1.0/consent/search`;
const audit_search = `${BASE_URL}/v1.0/audit/search`;
const audit_reports = `${AUDIT_BASE_URL}/audit/v1/audit`;
const data_breach_base = `${BASE_URL}/v1.0/data-breach`;
const data_breach_create = `${BASE_URL}/v1.0/data-breach/create`;
const data_breach_notify = `${BASE_URL}/v1.0/data-breach/notify`;
const dpo_dashboard = `${BASE_URL}/v1.0/dpo/dpodashboard`;
const ropa_create = `${BASE_URL}/v1.0/ropa/create`;
const ropa_search = `${BASE_URL}/v1.0/ropa`;
const ropa_update = `${BASE_URL}/v1.0/ropa/update`;
const ropa_delete = `${BASE_URL}/v1.0/ropa/delete`;
const user_dashboard_theme_create = `${BASE_URL}/v1.0/user-dashboard-theme/create`;
const user_dashboard_theme_get = `${BASE_URL}/v1.0/user-dashboard-theme/get`;
const notification_templates = `${NOTIFICATION_BASE_URL}/v1/templates`;
const translator_base = `${TRANSLATOR_BASE_URL}`;
const grievance_details = `${GRIEVANCE_BASE_URL}/api/v1/grievances`;
const dashboard_data = `${BASE_URL}/v1.0/dashboard/data`;

// Compliance Report endpoints
const compliance_callbacks_stats = `${BASE_URL_SCHEDULAR}/notification/v1/notifications/callbacks/stats`;
const compliance_purge_stats = `${BASE_URL_SCHEDULAR}/notification/v1/notifications/callbacks/purge-stats`;
const consent_deletion_list = `${BASE_URL_SCHEDULAR}/notification/v1/notifications/callbacks/consent-deletion`;
const consent_deletion_details = (eventId) => `${BASE_URL_SCHEDULAR}/notification/v1/notifications/callbacks/consent-deletion-details/${eventId}`;

// Registry API endpoints (Systems and Datasets)
const registry_systems = `${REGISTRY_BASE_URL}/api/v1/systems`;
const registry_systems_by_id = (id) => `${REGISTRY_BASE_URL}/api/v1/systems/${id}`;
const registry_datasets = `${REGISTRY_BASE_URL}/api/v1/datasets`;
const registry_datasets_by_id = (id) => `${REGISTRY_BASE_URL}/api/v1/datasets/${id}`;
const registry_db_integration = `${REGISTRY_BASE_URL}/api/v1/db-integration`;
const registry_db_integration_by_id = (id) => `${REGISTRY_BASE_URL}/api/v1/db-integration/${id}`;
const registry_db_integration_test = `${REGISTRY_BASE_URL}/api/v1/db-integration/test`;


export default {
  login_init_otp,
  login_validate_otp,
  create_data_protection_officer,
  get_data_protection_officer,
  update_data_protection_officer,
  create_system_config,
  get_system_config,
  update_system_config,
  ENC_URL,
  create_purpose,
  create_userTypes,
  create_grievance_template,
  get_userTypes,
  create_userDetails,
  get_userDetails,
  get_grievance_type,
  create_grievance_type,
  search_purpose,
  public_key,
  update_purpose,
  search_dataTypes,
  create_dataType,
  update_dataType,
  search_processor,
  create_processor,
  search_processingActivity,
  create_processingActiity,
  update_processingActiity,
  update_processor,
  login_get_profile,
  create_template,
  create_grievance_config,
  create_consent_set_up_config,
  create_digilocker_config,
  getConsentsByTemplateId,
  getPendingConsentsByTemplateId,
  getConsentHandleDetailsByConsentHandleId,
  getConsentCountsByTemplateId,
  getGrievanceByTemplateId,
  getGrievnaceTemplateData,
  searchBusiness,
  template_details,
  edit_template,
  get_consent_details,
  get_digilocker_details,
  get_grievance_details,
  update_consent_config,
  update_grievance_config,
  create_business,
  get_notif_details,
  update_notif_config,
  create_role,
  createUser,
  updateUser,
  searchUsers,
  listUsers,
  deleteUser,
  updateRole,
  searchRoles,
  listRoles,
  deleteRole,
  listComponents,
  get_cookie_categories,
  create_cookie_category,
  update_cookie_category,
  get_audit_reports,
  check_integrity,
  INTEGRATION_BASE_URL,
  BASE_URL_SCHEDULAR,
  BASE_URL_RETENTION,
  tenant_onboard,
  otp_init,
  otp_validate,
  users_profile,
  notification_create,
  smtp_create,
  smtp_search,
  smtp_update,
  business_application_search,
  templates_create,
  templates_search,
  grievance_templates_list,
  translator_translate,
  translator_translateConfig,
  client_credential,
  grievances_search,
  cookie_base,
  consent_search,
  audit_search,
  audit_reports,
  data_breach_base,
  data_breach_create,
  data_breach_notify,
  dpo_dashboard,
  ropa_create,
  ropa_search,
  ropa_update,
  ropa_delete,
  user_dashboard_theme_create,
  user_dashboard_theme_get,
  notification_templates,
  translator_base,
  grievance_details,
  dashboard_data,
  compliance_callbacks_stats,
  compliance_purge_stats,
  consent_deletion_list,
  consent_deletion_details,
  // Registry API endpoints
  registry_systems,
  registry_systems_by_id,
  registry_datasets,
  registry_datasets_by_id,
  registry_db_integration,
  registry_db_integration_by_id,
  registry_db_integration_test,
  REGISTRY_BASE_URL,
};
