import {
  SAVE_LOGIN_INFO,
  SAVE_DPO_CONFIG_ID,
  SAVE_SYSTEM_CONFIG_CONFIG_ID,
  SAVE_DATA_PROCESSOR_ID,
  SAVE_PURPOSE_ID,
  SAVE_LOGIN_USER_PROFILE_DETAILS,
  CLEAR_SESSION,
  SAVE_TEMPLATES,
  FETCH_BUSINESS_SUCCESS,
  SAVE_CONSENT_DETAILS,
  SET_TRANSLATIONS,
  SET_SELECTED_LANGUAGE,
  SAVE_BUSINESSES,           // ✅ add this
  SET_SELECTED_BUSINESS,
  UPDATE_BUSINESS_ID,
  SAVE_COMPANY_LOGO,
  SAVE_MULTILINGUAL_CONFIG,
} from "../constants/Constants";
import {
  SAVE_SIGNUP_INFO,
  SAVE_ONBOARD_INFO,
  SAVE_USER_PROFILE,
} from "../constants/Constants";
const initialState = {
  login_pan_no: "",
  login_email_mobile: "",
  dpo_configId: "",
  system_configId: "",
  data_processor_id: "",
  purpose_id: "",
  signup_txnid: "",
  user_profile: null,
  business_id: "",
  user_original_business_id: "", // Original business ID from user profile (never changes)
  tenant_id: "",
  user_id: "",
  session_token: "",
  mobile: "",
  pan: "",
  clientId: "",
  email: "",
  profile: {},
  translations: {},
  raw: null,
  companyName: "",
  firstChar: "",
  error: null,
  consentDetails: null,
  selectedLanguage: 'en',
  businesses: [],
  selectedBusiness: null,
  roles: [],
  permissions: [],
  userRole: null,
  companyLogo: null,
  multilingualConfigSaved: false,
};

const commonReducer = (state = initialState, action) => {
  switch (action.type) {
    case SAVE_LOGIN_INFO:
      return {
        ...state,
        login_pan_no: action.payload.pan,
        login_email_mobile: action.payload.emailMobile,
        tenant_id: action.payload.tenant_id,
        user_id: action.payload.user_id,
        session_token: action.payload.session_token,
      };
    case CLEAR_SESSION:
      return initialState;
    case SAVE_LOGIN_USER_PROFILE_DETAILS:
      return {
        ...state,
        business_id: action.payload?.roles?.[0]?.businessId || "",
      };
    case SAVE_SIGNUP_INFO:
      return {
        ...state,
        signup_txnid: action.responseData.txnId,
      };

    case SAVE_ONBOARD_INFO:
      return {
        ...state,
        tenant_id: action.responseData.tenantId,
        session_token: action.responseData.xSessionToken,
      };

    case SAVE_USER_PROFILE:
      // Extract roles and flatten permissions
      const roles = action.data?.roles || [];
      const allPermissions = roles.flatMap(role => role.permissions || []);
      const userRole = roles[0]?.role || null;
      const userBusinessId = action.data?.roles?.[0]?.businessId || "";

      // Preserve user's selected business if they already picked one from the dropdown.
      // Only set business_id / selectedBusiness on first load (when they are empty).
      const preserveBusinessId =
        state.business_id && state.selectedBusiness
          ? state.business_id
          : userBusinessId;
      const preserveSelectedBusiness =
        state.business_id && state.selectedBusiness
          ? state.selectedBusiness
          : userBusinessId;

      return {
        ...state,
        user_profile: action.data, // full response
        tenant_id: action.data?.tenantId || state.tenant_id, // Update tenant_id from profile
        business_id: preserveBusinessId,
        user_original_business_id: userBusinessId, // Store original business ID (never changes)
        selectedBusiness: preserveSelectedBusiness, // Preserve user's dropdown choice
        user_id: action.data?.userId,
        mobile: action.data?.mobile,
        email: action.data?.email,
        pan: action.data?.pan,
        clientId: action.data?.clientId,
        spocName: action.data?.spocName,
        totpSecret: action.data?.totpSecret,
        roles: roles,
        userRole: userRole,
        permissions: allPermissions,
      };
    case SAVE_DPO_CONFIG_ID:
      return {
        ...state,
        dpo_configId: action.payload.configId,
      };

    case SAVE_SYSTEM_CONFIG_CONFIG_ID:
      return {
        ...state,
        system_configId: action.payload.configId,
      };

    case SAVE_DATA_PROCESSOR_ID:
      return {
        ...state,
        data_processor_id: action.payload.dataProcessorId,
      };

    case SAVE_PURPOSE_ID:
      return {
        ...state,
        purpose_id: action.payload.purposeId,
      };

    case SAVE_TEMPLATES:
      return {
        ...state,
        profile: action.data,
      };

    case FETCH_BUSINESS_SUCCESS:
      return {
        ...state,
        raw: action.payload.raw,
        companyName: action.payload.companyName,
        firstChar: action.payload.firstChar,
        error: null,
      };

    case SAVE_CONSENT_DETAILS:
      return {
        ...state,
        consentDetails: action.payload,
      };

    case SET_TRANSLATIONS:
      return {
        ...state,
        translations: {
          ...state.translations,
          [action.payload.targetLanguage]: action.payload.translations.reduce((acc, item) => {
            acc[item.id] = item.target;
            return acc;
          }, {}),
        },
      };

    case SET_SELECTED_LANGUAGE:
      return {
        ...state,
        selectedLanguage: action.payload,
      };

    case SAVE_BUSINESSES:
      return {
        ...state,
        businesses: action.payload,
      };

    case SET_SELECTED_BUSINESS:
      return {
        ...state,
        selectedBusiness: action.payload,
      };

    case UPDATE_BUSINESS_ID:
      return {
        ...state,
        business_id: action.payload, // update the selected business_id
      };

    case SAVE_COMPANY_LOGO:
      return {
        ...state,
        companyLogo: action.payload,
      };

    case SAVE_MULTILINGUAL_CONFIG:
      return {
        ...state,
        multilingualConfigSaved: action.payload,
      };

    default:
      return state;
  }
};

export default commonReducer;
