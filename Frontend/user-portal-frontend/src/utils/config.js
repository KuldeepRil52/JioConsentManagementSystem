const BASE_URL = process.env.REACT_APP_API_URL;
const CONSENT_URL = process.env.REACT_APP_CONSENT_URL;
const META_URL = process.env.REACT_APP_META_URL;
const GRIEVANCE_URL = process.env.REACT_APP_GRIEVANCE_URL;
const TRANSLATE_URL = process.env.REACT_APP_TRANSLATE_URL;
const login_init_otp = `${BASE_URL}/tenant/otp/init`;
const getConsentsByCustomerId = `${CONSENT_URL}/v1.0/consent/searchConsentByIdentity`;
const getConsentsByTemplateId = `${CONSENT_URL}/v1.0/consent/search`;
const getBusinessLogoByBusinessId = `${BASE_URL}/v1.0/system-config/search`;
const getConsentHandleById = `${CONSENT_URL}/v1.0/consent-handle/get/`;
const sendParentalConsentRequest = `${CONSENT_URL}/v1.0/consent-handle/parental-consent`;
const getconsentMetaId = `${CONSENT_URL}/v1.0/consent-meta/`;
const withdrawConsentByConsentId = `${CONSENT_URL}/v1.0/consent/update/`;
const createParentalConsent = `${CONSENT_URL}/v1.0/consent/create`;
const searchConsentHandleByCustomerId = `${CONSENT_URL}/v1.0/consent-handle/search`;
const fetchDocumentById = `${CONSENT_URL}/v1.0/documents/view-document/`;
const getGrievnaceTemplateDataById = `${GRIEVANCE_URL}/api/v1/grievance-templates`;
const getGrievnaceTemplateData = `${GRIEVANCE_URL}/api/v1/grievance-templates/search`;
const getGrievnaceRequest = `${GRIEVANCE_URL}/api/v1/grievances/search`;
const getGrievnaceReq = `${GRIEVANCE_URL}/api/v1/grievances`;
const createGrievanceRequest = `${GRIEVANCE_URL}/api/v1/grievances`;
const translateConfig = `${TRANSLATE_URL}/translateConfig`;
const translateInputs = `${TRANSLATE_URL}/translate`;
const createConsent = `${CONSENT_URL}/v1.0/consent/create`;
const getBusinessDetailsByBusinessid = `${BASE_URL}/v1.0/business-application/search`;
const getDigilockerDetails = `${BASE_URL}/v1.0/digilocker/credential/search`;
const createParenytalConsentMeta = `${CONSENT_URL}/v1.0/consent-meta/create`;
const metaurl = `${META_URL}/consent-meta/`;
export default {
  login_init_otp,
  sendParentalConsentRequest,
  getConsentsByCustomerId,
  getConsentsByTemplateId,
  getConsentHandleById,
  getconsentMetaId,
  withdrawConsentByConsentId,
  searchConsentHandleByCustomerId,
  getGrievnaceTemplateData,
  getGrievnaceRequest,
  getGrievnaceTemplateDataById,
  getGrievnaceReq,
  createGrievanceRequest,
  fetchDocumentById,
  translateConfig,
  translateInputs,
  createConsent,
  createParentalConsent,
  getBusinessDetailsByBusinessid,
  getDigilockerDetails,
  createParenytalConsentMeta,
  metaurl,
  getBusinessLogoByBusinessId,
};
