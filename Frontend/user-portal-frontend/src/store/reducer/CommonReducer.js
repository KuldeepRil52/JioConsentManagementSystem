import {
  SAVE_CUSID_TNTID,
  CLEAR_SESSION,
  SET_BUSINESS_NAME,
  SET_PARENT_CONSENT_METADATA,
  SET_PARENT_CODE,
  SET_PARENT_STATE,
  SAVE_INTEGRATION_IDS,
  SET_CURRENT_LANGUAGE,
  SET_TRANSLATIONS,
  SET_IS_TRANSLATING,
  SET_BUSINESS_LOGO,
} from "../constants/Constants";

const initialState = {
  tenant_id: "",
  customer_id: "",
  business_id: "",
  business_name: "",
  secure_id: "",
  business_logo: "",
  grievance_template_id: "",
  integration_tenant_id: "",
  integration_business_id: "",
  integration_grievanceTemplate_id: "",
  parentConsentMetaData: null,
  parentCode: "",
  parentState: "",
  // Language/Translation state
  currentLanguage: "ENGLISH",
  currentSourceLang: "en",
  translations: {},
  isTranslating: false,
  parental_redirect_url: "",
};

const commonReducer = (state = initialState, action) => {
  switch (action.type) {
    case CLEAR_SESSION:
      return initialState;

    case SET_BUSINESS_NAME:
      return {
        ...state,
        business_name: action.payload,
      };

    case SET_BUSINESS_LOGO:
      return {
        ...state,
        business_logo: action.payload,
      };

    case SAVE_CUSID_TNTID:
      return {
        ...state,
        tenant_id: action.payload.tenantId,
        customer_id: action.payload.customerId,
        business_id: action.payload.businessId,
        grievance_template_id: action.payload.grievanceTemplateId,
        secure_id: action.payload.secureCode,
        parental_redirect_url: action.payload.parentlCbUrl,
      };

    case SAVE_INTEGRATION_IDS:
      return {
        ...state,
        integration_tenant_id: action.payload.tenantId,
        integration_business_id: action.payload.businessId,
        integration_grievanceTemplate_id: action.payload.grievanceTemplateId,
      };

    case SET_PARENT_CONSENT_METADATA:
      return {
        ...state,
        parentConsentMetaData: action.payload,
      };

    case SET_PARENT_CODE:
      return {
        ...state,
        parentCode: action.payload,
      };

    case SET_PARENT_STATE:
      return {
        ...state,
        parentState: action.payload,
      };

    case SET_CURRENT_LANGUAGE:
      return {
        ...state,
        currentLanguage: action.payload.language,
        currentSourceLang: action.payload.sourceLang,
      };

    case SET_TRANSLATIONS:
      return {
        ...state,
        translations: { ...state.translations, ...action.payload },
      };

    case SET_IS_TRANSLATING:
      return {
        ...state,
        isTranslating: action.payload,
      };

    default:
      return state;
  }
};

export default commonReducer;
