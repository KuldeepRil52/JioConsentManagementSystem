import { makeAPICall } from "../../utils/ApiCall";
import {
  SAVE_CUSID_TNTID,
  SAVE_INTEGRATION_IDS,
  SAVE_TEMPLATES,
  SET_BUSINESS_LOGO,
  SET_BUSINESS_NAME,
  SET_PARENT_CODE,
  SET_PARENT_CONSENT_METADATA,
  SET_PARENT_STATE,
} from "../constants/Constants";
import config from "../../utils/config";
import ParentConsent from "../../Components/ParentConsent";

const uuidv4 = () => {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0,
      v = c == "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

export const getLogoByBusinessId = (businessId) => {
  return async (dispatch, getState) => {
    const tenant_id = getState().common.tenant_id;
    let secure_code = getState().common.secure_id;
    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "X-Secure-Code": secure_code,
      };

      const body = {};

      const url = `${config.getBusinessLogoByBusinessId}?businessId=${businessId}`;
      let response = await makeAPICall(url, "GET", body, headers);

      let re = response?.data?.searchList;
      const businessData = response?.data?.searchList?.[0];

      if (businessData?.configurationJson?.logo) {
        dispatch({
          type: SET_BUSINESS_LOGO,
          payload: businessData?.configurationJson?.logo,
        });
      }

      // return businessData;
      return re;
    } catch (error) {
      throw error;
    }
  };
};

export const getConsentsByTemplateId = async (cusId, tenantId, secure_code) => {
  try {
    const headers = {
      txn: uuidv4(),
      "tenant-id": tenantId,
      "X-Secure-Code": secure_code,
      Accept: "*/*",
    };

    const body = {};

    const url = `${config.getConsentsByCustomerId}?customerIdentifiers.value=${cusId}`;
    let response = await makeAPICall(url, "GET", body, headers);

    let re = response?.data?.searchList;
    return re;
  } catch (error) {
    throw error;
  }
};

export const searchConsentsByConsentId = (consentId) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        "tenant-id": tenant_id,
        Accept: "*/*",
        txn: uuidv4(),
        "X-Secure-Code": secure_code,
      };

      const body = {};
      const url = `${config.getConsentsByTemplateId}?consentId=${consentId}`;

      let response = await makeAPICall(url, "GET", body, headers);

      return response?.data?.searchList;
    } catch (error) {
      console.log(" Error fetching consent by ID:", error);
      throw error;
    }
  };
};

export const getConsentHandleByHandleID = (consentHandleId) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        "tenant-id": tenant_id,
        Accept: "*/*",
        txn: uuidv4(),
        "X-Secure-Code": secure_code,
      };

      const body = {};
      const url = `${config.getConsentHandleById}${consentHandleId}`;

      let response = await makeAPICall(url, "GET", body, headers);

      return response?.data;
    } catch (error) {
      console.log(" Error fetching consent by ID:", error);
      throw error;
    }
  };
};

export const getConsentMetaDataForParent = (
  parentconsentMetaId,
  tenantId,
  code,
  state,
) => {
  return async (dispatch, getState) => {
    let tenant_id = tenantId;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        consentMetaId: parentconsentMetaId,
        "tenant-id": tenant_id,
        txn: uuidv4(),
        "X-Secure-Code": secure_code,
      };

      const body = {};
      const url = `${config.metaurl}${parentconsentMetaId}`;

      let response = await makeAPICall(url, "GET", body, headers);

      if (response?.status === 200) {
        dispatch({
          type: SET_PARENT_CONSENT_METADATA,
          payload: response?.data,
        });
        dispatch({ type: SET_PARENT_CODE, payload: code });
        dispatch({ type: SET_PARENT_STATE, payload: state });
      }

      return response?.status;
    } catch (error) {
      console.log(" Error fetching consent by consentMetaId:", error);
      throw error;
    }
  };
};

export const captureParentConsent = (tenantId, code, state) => {
  return async (dispatch, getState) => {
    const meta = getState().common.parentConsentMetaData;

    let tenant_id = tenantId;
    try {
      const headers = {
        "tenant-id": tenant_id,
        txn: uuidv4(),
        "X-Secure-Code": meta?.secId,
      };

      const body = {
        consentHandleId: meta?.consentHandleId,
        languagePreferences: meta?.languagePreference,
        preferencesStatus: meta?.preferencesStatus,
        isParentalConsent: true,
        parentalKYCType: "DIGILOCKER",
        code: code,
        state: state,
      };

      let response = await makeAPICall(
        config.createParentalConsent,
        "POST",
        body,
        headers,
      );

      return response?.status;
    } catch (error) {
      console.log(" Error fetching consent by consentMetaId:", error);
      throw error;
    }
  };
};

export const withdrawConsentByConsentId = (
  selectedConsentId,
  selectedLanguagePreferences,
  selectedPrefereneces,
  status,
) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        "tenant-id": tenant_id,
        Accept: "*/*",
        txn: uuidv4(),
        "X-Secure-Code": secure_code,
      };

      const body = {
        languagePreferences: selectedLanguagePreferences,
        preferencesStatus: selectedPrefereneces,
        status: status,
      };

      const url = `${config.withdrawConsentByConsentId}${selectedConsentId}`;

      let response = await makeAPICall(url, "PUT", body, headers);

      return response;
    } catch (error) {
      console.log(" Error fetching consent by ID:", error);
      throw error;
    }
  };
};

export const updatePendingConsentByConsentId = (
  selectedConsentId,
  selectedLanguagePreferences,
  selectedPrefereneces,
  status,
) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        "tenant-id": tenant_id,
        Accept: "*/*",
        txn: uuidv4(),
        "X-Secure-Code": secure_code,
      };

      const body = {
        languagePreferences: selectedLanguagePreferences,
        preferencesStatus: selectedPrefereneces,
        status: "ACTIVE",
      };
      const url = `${config.withdrawConsentByConsentId}${selectedConsentId}`;

      let response = await makeAPICall(url, "PUT", body, headers);

      return response;
    } catch (error) {
      console.log(" Error fetching consent by ID:", error);
      throw error;
    }
  };
};

export const captureAllConsent2 = (
  selectedConsentId,
  selectedLanguagePreferences,
  selectedPrefereneces,
  status,
) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        "tenant-id": tenant_id,
        Accept: "*/*",
        txn: uuidv4(),
        "X-Secure-Code": secure_code,
      };

      const body = {
        consentHandleId: selectedConsentId,
        languagePreference: selectedLanguagePreferences,
        preferencesStatus: selectedPrefereneces,
      };

      let response = await makeAPICall(
        config.createConsent,
        "POST",
        body,
        headers,
      );

      return response;
    } catch (error) {
      console.log(" Error fetching consent by ID:", error);
      throw error;
    }
  };
};

export const translateConfiguration = () => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let business_id = getState().common.business_id;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        tenantid: tenant_id,
        businessid: business_id,
        "Content-Type": "application/json",
        txn: uuidv4(),
        "X-Secure-Code": secure_code,
      };

      const body = {
        scopeLevel: "BUSINESS",
        config: {
          provider: "BHASHINI",
          apiBaseUrl: "https://meity-auth.ulcacontrib.org",
          modelPipelineEndpoint: "/ulca/apis/v0/model/getModelsPipeline",
          callbackUrl:
            "https://dhruva-api.bhashini.gov.in/services/inference/pipeline",
          userId: "02c3fec39bbf46cda8ece45ba52e78cb",
          apiKey: "058a0399da-ea84-4077-9e3e-984ff46f8b77",
          pipelineId: "64392f96daac500b55c543cd",
        },
      };

      const response = await fetch(config.translateConfig, {
        method: "POST",
        headers,
        body: JSON.stringify(body),
      });

      const data = await response.json();

      if (!response.ok) {
        const errMsg =
          data?.errorMsg ||
          data?.message ||
          `Error ${response.status}: ${JSON.stringify(data)}`;
        throw new Error(errMsg);
      }

      return data;
    } catch (error) {
      console.log("Error fetching consent by ID:", error);
      throw error;
    }
  };
};

export const translateData = (currentLangCode, targetLang, finalInputs) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let business_id = getState().common.business_id;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        "Content-Type": "application/json",
        tenantid: tenant_id,
        businessid: business_id,
        Accept: "*/*",
        txn: uuidv4(),
        "X-Secure-Code": secure_code,
      };

      const body = {
        configId: "e66b0624-61fb-44e9-b087-8bb312b5387b",
        provider: "BHASHINI",
        source: "CONSENTPOPUP",
        language: {
          sourceLanguage: currentLangCode,
          targetLanguage: targetLang,
        },
        input: finalInputs,
      };

      let response = await makeAPICall(
        config.translateInputs,
        "POST",
        body,
        headers,
      );

      return response;
    } catch (error) {
      console.log("Error fetching consent by ID:", error);
      throw error;
    }
  };
};

export const fetchDocumentById = (docId) => {
  return async (dispatch, getState) => {
    const tenant_id = getState().common.tenant_id;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        "tenant-id": tenant_id,
        Accept: "*/*",
        txn: uuidv4(),
        "X-Secure-Code": secure_code,
      };

      const url = `${config.fetchDocumentById}${docId}`;

      const response = await fetch(url, {
        method: "GET",
        headers,
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch document: ${response.status}`);
      }

      const blob = await response.blob();

      return blob;
    } catch (error) {
      console.log("Error fetching document by ID:", error);
      throw error;
    }
  };
};

export const saveCustomerIdAndenantId = (
  cusId,
  tntId,
  bsnId,
  gtId,
  scCode,
  recievedCallBackUrl,
) => {
  return async (dispatch, getState) => {
    let customerId = cusId;
    let tenantId = tntId;
    let businessId = bsnId;
    let grievanceTemplateId = gtId;
    let secureCode = scCode;
    let parentlCbUrl = recievedCallBackUrl;
    dispatch({
      type: SAVE_CUSID_TNTID,
      payload: {
        customerId,
        tenantId,
        businessId,
        grievanceTemplateId,
        secureCode,
        parentlCbUrl,
      },
    });
  };
};

export const searchConsentHandleByCustomerId = () => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let customer_id = getState().common.customer_id;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        "tenant-id": tenant_id,
        Accept: "*/*",
        txn: uuidv4(),
        "X-Secure-Code": secure_code,
      };

      const body = {};
      const url = `${config.searchConsentHandleByCustomerId}?customerIdentifiers.value=${customer_id}`;

      let response = await makeAPICall(url, "GET", body, headers);

      return response?.data?.searchList;
    } catch (error) {
      console.log(" Error fetching consent by Customer Id:", error);
      throw error;
    }
  };
};

export const getBusinessDetails = (businessId) => {
  return async (dispatch, getState) => {
    const tenant_id = getState().common.tenant_id;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        "tenant-id": tenant_id,
        Accept: "*/*",
        txn: uuidv4(),
        "X-Secure-Code": secure_code,
      };

      const url = `${config.getBusinessDetailsByBusinessid}?businessId=${businessId}`;

      const response = await makeAPICall(url, "GET", {}, headers);
      const businessData = response?.data?.searchList?.[0];

      if (businessData?.name) {
        dispatch({
          type: SET_BUSINESS_NAME,
          payload: businessData.name,
        });
      }

      return businessData;
    } catch (error) {
      console.log("Error fetching business Data:", error);
      throw error;
    }
  };
};

export const getGrievnaceTemplateData = () => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let business_id = getState().common.business_id;
    let grievanceTemplateId = getState().common.grievance_template_id;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        "X-Tenant-ID": tenant_id,
        "X-Business-ID": business_id,
        "X-Transaction-ID": uuidv4(),
        "X-Scope-Level": "TENANT",
        Accept: "application/json",
        "X-Secure-Code": secure_code,
      };

      const body = {};

      const url = `${config.getGrievnaceTemplateDataById}/${grievanceTemplateId}`;
      let response = await makeAPICall(url, "GET", body, headers);
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const getGrievnaceRequestByGrievanceTemplateId = () => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let business_id = getState().common.business_id;
    let secure_code = getState().common.secure_id;
    let grievanceTemplateId = getState().common.grievance_template_id;

    try {
      const headers = {
        "X-Tenant-ID": tenant_id,
        "X-Business-ID": business_id,
        "X-Transaction-ID": uuidv4(),
        "X-Scope-Level": "TENANT",
        Accept: "application/json",
        "X-Secure-Code": secure_code,
      };

      const body = {};
      const url = `${config.getGrievnaceTemplateData}?grievanceTemplateId=${grievanceTemplateId}`;

      let response = await makeAPICall(url, "GET", body, headers);
      let finalResponse =
        response?.data?.[0]?.multilingual?.userInformation?.[0]?.userItems;

      return finalResponse;
    } catch (error) {
      throw error;
    }
  };
};
export const getGrievnaceRequestByUserId = (identity) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let business_id = getState().common.business_id;
    let secure_code = getState().common.secure_id;
    let grievanceTemplateId = getState().common.grievance_template_id;
    let customerIdentity = getState().common.customer_id;

    try {
      const headers = {
        "X-Tenant-ID": tenant_id,
        "X-Business-ID": business_id,
        "X-Transaction-ID": uuidv4(),
        "X-Scope-Level": "TENANT",
        Accept: "application/json",
        "X-Secure-Code": secure_code,
      };

      const body = {};
      const url = `${config.getGrievnaceRequest}?userDetails.${identity}=${customerIdentity}`;

      let response = await makeAPICall(url, "GET", body, headers);
      let finalResponse =
        response?.data?.[0]?.multilingual?.userInformation?.[0]?.userItems;

      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const getGrievnaceRequestByTemplateId = () => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let business_id = getState().common.business_id;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        "X-Tenant-ID": tenant_id,
        "X-Business-ID": business_id,
        "X-Transaction-ID": uuidv4(),
        "X-Scope-Level": "TENANT",
        Accept: "application/json",
        "X-Secure-Code": secure_code,
      };

      const body = {};

      let response = await makeAPICall(
        config.getGrievnaceRequest,
        "GET",
        body,
        headers,
      );
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const getGrievnaceRequestByParams = (
  tenantId,
  businessId,
  grievanceTemplateId,
) => {
  return async (dispatch, getState) => {
    let secure_code = getState().common.secure_id;
    try {
      dispatch({
        type: SAVE_INTEGRATION_IDS,
        payload: { tenantId, businessId, grievanceTemplateId },
      });

      const headers = {
        "X-Tenant-ID": tenantId,
        "X-Business-ID": businessId,
        "X-Transaction-ID": uuidv4(),
        "X-Scope-Level": "TENANT",
        Accept: "application/json",
        "X-Secure-Code": secure_code,
      };

      const body = {};

      const response = await makeAPICall(
        config.getGrievnaceRequest,
        "GET",
        body,
        headers,
      );

      return response;
    } catch (error) {
      throw error;
    }
  };
};
export const getGrievanceRequestDetails = (grievanceId) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let business_id = getState().common.business_id;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        "tenant-id": tenant_id,
        "business-id": business_id,
        "X-Transaction-ID": uuidv4(),
        "X-Scope-Level": "TENANT",
        Accept: "application/json",
        "X-Secure-Code": secure_code,
      };

      const body = {};
      const url = `${config.getGrievnaceReq}/${grievanceId}`;

      let response = await makeAPICall(url, "GET", body, headers);
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const getIntegrationGrievanceRequestDetails = (grievanceId) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.integration_tenant_id;

    let business_id = getState().common.integration_business_id;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        "X-Tenant-ID": tenant_id,
        "X-Business-ID": business_id,
        "X-Transaction-ID": uuidv4(),
        "X-Scope-Level": "TENANT",
        Accept: "application/json",
        "X-Secure-Code": secure_code,
      };

      const body = {};

      const url = `${config.getGrievnaceRequest}/${grievanceId}`;

      let response = await makeAPICall(url, "GET", body, headers);

      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const createGrievanceRequest = ({ grievanceTemplateId, body }) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let business_id = getState().common.business_id;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        "Content-Type": "application/json",
        "X-Tenant-ID": tenant_id,
        "X-Business-ID": business_id,
        "X-Transaction-ID": uuidv4(),
        "X-Scope-Level": "TENANT",
        "X-GRIEVANCE-TEMPLATE-ID": grievanceTemplateId,
        "X-Secure-Code": secure_code,
        Accept: "*/*",
      };

      let response = await makeAPICall(
        config.createGrievanceRequest,
        "POST",
        body,
        headers,
      );
      return response;
    } catch (error) {
      console.log(" Error creating grievance request:", error);
      return error;
    }
  };
};

export const createIntegrationGrievanceRequest = ({
  grievanceTemplateId,
  body,
  tenantId,
  businessId,
  secureCode,
}) => {
  return async (dispatch, getState) => {
    let tenant_id = tenantId;

    let business_id = businessId;

    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        "Content-Type": "application/json",

        "X-Tenant-ID": tenant_id,

        "X-Business-ID": business_id,

        "X-Transaction-ID": uuidv4(),

        "X-Scope-Level": "TENANT",

        "X-GRIEVANCE-TEMPLATE-ID": grievanceTemplateId,
        "X-Secure-Code": secureCode,

        Accept: "*/*",
      };

      let response = await makeAPICall(
        config.createGrievanceRequest,

        "POST",

        body,

        headers,
      );

      return response;
    } catch (error) {
      console.log(" Error creating grievance request:", error);

      return error;
    }
  };
};

export const getIntegrationGrievnaceTemplateData = (
  tenantId,
  businessId,
  integrationGrievanceTemplateId,
  secureCode,
) => {
  return async (dispatch, getState) => {
    let tenant_id = tenantId;

    let business_id = businessId;

    let grievanceTemplateId = integrationGrievanceTemplateId;

    try {
      const headers = {
        "X-Tenant-ID": tenant_id,

        "X-Business-ID": business_id,

        "X-Transaction-ID": uuidv4(),

        "X-Scope-Level": "TENANT",

        Accept: "application/json",
        "X-Secure-Code": secureCode,
      };

      const body = {};

      const url = `${config.getGrievnaceTemplateData}/${grievanceTemplateId}`;

      let response = await makeAPICall(url, "GET", body, headers);

      return response;
    } catch (error) {
      throw error;
    }
  };
};
export const updateFeedback = ({ grievanceId, newRating }) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let business_id = getState().common.business_id;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        "Content-Type": "application/json",
        "X-Tenant-ID": tenant_id,
        "X-Business-ID": business_id,
        "X-Transaction-ID": uuidv4(),
        "X-Scope-Level": "TENANT",
        "X-Secure-Code": secure_code,
        Accept: "*/*",
      };
      const url = `${config.createGrievanceRequest}/${grievanceId}/feedback`;
      const body = {
        feedback: newRating,
      };

      let response = await makeAPICall(url, "PUT", body, headers);
      return response;
    } catch (error) {
      console.log(" Error creating grievance request:", error);
      return error;
    }
  };
};

export const updateIntegrationFeedback = ({ grievanceId, newRating }) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.integration_tenant_id;
    let business_id = getState().common.integration_business_id;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        "Content-Type": "application/json",
        "X-Tenant-ID": tenant_id,
        "X-Business-ID": business_id,
        "X-Transaction-ID": uuidv4(),
        "X-Scope-Level": "TENANT",
        Accept: "*/*",
        "X-Secure-Code": secure_code,
      };
      const url = `${config.createGrievanceRequest}/${grievanceId}/feedback`;
      const body = {
        feedback: newRating,
      };

      let response = await makeAPICall(url, "PUT", body, headers);
      return response;
    } catch (error) {
      console.log(" Error creating grievance request:", error);
      return error;
    }
  };
};

export const getDigilockerDetails = () => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let business_id = getState().common.business_id;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token":
          "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJwYXJ0bmVyLXBvcnRhbCIsInRlbmFudElkIjoiNTg5Yzk1YzYtNGJiOC00Njc4LThjMGItYzE1YzczNmNiNDZkIiwic3ViIjoiMzA1NWEwNDItOTFkZi00MWI2LWIxZDMtNDc2YWM5M2ZiNGM2IiwiZXhwIjoxNzYxODg4ODc4LCJpYXQiOjE3NjE4ODUyNzh9.bLREZVmrQrIwxxvIj0BjFmfa9wUiwtbu9GqXmQcmfaznl8UMAuhFq3OEVEKC4wXTh6Ahk_2uwUs0_EmN_wtBc2gVmrZ7tESijFTUPlelzm3NExFXfMR6HcfQxyyvkcZh9Ih_JqEgpJG0akQDdjK0tUd--usZMhFz069T-h5PavvCl6hODDxCwNEtJCWWo0RJ3YEkj6JTjvSRkbIvFULjrA8Qd0uj_31UswcKWheInajoNG-rhEwYO62hpbxFw7svJFDd4whY5JtfXWSV5jC1l1-zy5HaFYn8_q9Euf-S7FP6zCICqGQ7cIK5bgstJhV2QsvdZE6SpRnuabRznzkucg",
        Accept: "application/json",
        "X-Secure-Code": secure_code,
      };
      const body = {};

      let response = await makeAPICall(
        config.getDigilockerDetails,
        "GET",
        body,
        headers,
      );

      return response;
    } catch (error) {
      console.log(" Error in fetching digilocker details:", error);
      return error;
    }
  };
};

export const generateDigilockerTransactionId = (
  selectedConsentId,
  selectedLanguagePreferences,
  selectedPrefereneces,
  relationship,
) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let business_id = getState().common.business_id;
    let secure_code = getState().common.secure_id;

    try {
      const headers = {
        txn: uuidv4(),
        "Content-Type": "application/json",
        "tenant-id": tenant_id,
        "X-Secure-Code": secure_code,
      };
      const body = {
        consentHandleId: selectedConsentId,
        preferencesStatus: selectedPrefereneces,
        languagePreferences: selectedLanguagePreferences,
        isParentalConsent: true,
        secId: secure_code,
        relation: relationship,
      };

      let response = await makeAPICall(
        config.createParenytalConsentMeta,
        "POST",
        body,
        headers,
      );
      return response;
    } catch (error) {
      console.log(" Error in fetching digilocker details:", error);
      return error;
    }
  };
};

export const sendParentalConsentRequest = (
  parentName,
  parentIdentity,
  parentIdentityType,
  selectedConsentId,
) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let business_id = getState().common.business_id;
    let secure_code = getState().common.secure_id;
    let redirect_Url = getState().common.parental_redirect_url;

    try {
      const headers = {
        accept: "*/*",
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "Content-Type": "application/json",
        "X-Secure-Code": secure_code,
      };
      const body = {
        consentHandleId: selectedConsentId,
        parentIdentity: parentIdentity.toUpperCase(),
        parentIdentityType: parentIdentityType,
        isParental: true,
        parentName: parentName,
        redirectUri: redirect_Url,
      };

      let response = await makeAPICall(
        config.sendParentalConsentRequest,
        "POST",
        body,
        headers,
      );

      return response;
    } catch (error) {
      console.log(" Error in sending parent consent request:", error);
      return error;
    }
  };
};
