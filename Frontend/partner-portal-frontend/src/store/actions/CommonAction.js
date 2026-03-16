import login_init_otp from "../../utils/config";
import login_validate_otp from "../../utils/config";
import create_system_config from "../../utils/config";
import create_data_protection_officer from "../../utils/config";
import axios from 'axios';

import { JSEncrypt } from "jsencrypt";
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
  SAVE_BUSINESSES,
  SET_SELECTED_BUSINESS,
  UPDATE_BUSINESS_ID,
} from "../constants/Constants";
import { makeAPICall } from "../../utils/ApiCall";
import config from "../../utils/config";

import {
  SAVE_SIGNUP_INFO,
  SAVE_ONBOARD_INFO,
  SAVE_USER_PROFILE,
} from "../constants/Constants";

import { fetchTranslations as translationApi } from "../../utils/translationApi";

const uuidv4 = () => {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0,
      v = c == "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

export const generateloginOtp = (pan, value, emailMobile) => {
  // alert("generateloginOtp in ca.js");
  return async (dispatch, getState) => {
    var value_type = "";
    if (value == 0) {
      value_type = "MOBILE";
    }
    if (value == 1) {
      value_type = "EMAIL";
    }
    const headers = {
      txn: uuidv4(),
    };

    const body = {
      pan: pan,
      idValue: emailMobile.toUpperCase(),
      idType: value_type,
    };

    try {
      let response = await makeAPICall(
        config.login_init_otp,
        "POST",
        body,
        headers
      );
      return response;
    } catch (error) {
      throw error;
    }
  };
};

const encryptOtp = async (otp) => {
  try {
    const jsEncrypt = new JSEncrypt();
    jsEncrypt.setPublicKey(public_key);
    const encrypted = jsEncrypt.encrypt(otp);

    return encrypted;
  } catch (err) {
    console.log("Error encrypting OTP:", err);
    return null; // return something meaningful
  }
};

export const verifyLoginOtp = (
  panno,
  value,
  emailMobile,
  idType,
  logintxnId,
  totp
) => {
  return async (dispatch, getState) => {
    const originalValue = value;
    const encryptedOtp = await encryptOtp(originalValue);
    var id = "";
    if (idType == 0) {
      id = "MOBILE";
    }
    if (idType == 1) {
      id = "EMAIL";
    }

    if (!encryptedOtp) {
      console.log("OTP encryption failed");
      return;
    }

    if (encryptedOtp === 500 || encryptedOtp === 400) {
      return "Error in verifying Otp";
    }

    const txnId = uuidv4();
    const headers = { txn: txnId };
    const body = {
      pan: panno,
      txnId: logintxnId,
      otp: encryptedOtp,
      idValue: emailMobile.toUpperCase(),
      idType: id,
      totp: totp,
    };

    try {
      let response = await makeAPICall(
        config.login_validate_otp,
        "POST",
        body,
        headers
      );
      if (response.status == 200) {
        dispatch({
          type: SAVE_LOGIN_INFO,
          payload: {
            pan: panno,
            emailMobile: emailMobile.toUpperCase(),
            tenant_id: response?.data?.tenantId || "",
            user_id: response?.data?.userId || "",
            session_token: response?.data?.xsessionToken || "",
          },
        });
      }

      const payload = {
        pan: panno,
        emailMobile,
        login_tenant_id: response?.data?.tenantId ?? "",
        login_user_id: response?.data?.userId ?? "",
        login_session_token: response?.data?.xsessionToken ?? "",
      };

      return response;
    } catch (error) {
      console.log("Error in verifyLoginOtp:", error?.retryCount);
      throw error;
    }
  };
};

export const submitDPODetails = (dpoName, dpoemail, dpophone, dpoaddress) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let session_token = getState().common.session_token;
    let business_id = getState().common.business_id;
    
    // Determine scope-level: if tenantId === businessId then "TENANT", otherwise "BUSINESS"
    const scopeLevel = tenant_id === business_id ? "TENANT" : "BUSINESS";
    
    const headers = {
      txn: uuidv4(),
      "business-id": business_id,
      "scope-level": scopeLevel,
      "tenant-id": tenant_id,
      "x-session-token": session_token,
      "Content-Type": "application/json",
      Accept: "application/json",
    };

    const body = {
      configurationJson: {
        name: dpoName,
        email: dpoemail,
        mobile: dpophone,
        address: dpoaddress,
      },
    };

    try {
      let response = await makeAPICall(
        config.create_data_protection_officer,
        "POST",
        body,
        headers
      );

      if (response.status == 201) {
        const configId = response.data.configId;
        dispatch({
          type: SAVE_DPO_CONFIG_ID,
          payload: { configId },
        });
      }
      return response;
    } catch (error) {
      console.log(error);
      throw error;
    }
  };
};

export const getDPODetails = () => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let session_token = getState().common.session_token;
    let business_id = getState().common.business_id;
    const headers = {
      txn: uuidv4(),
      "tenant-id": tenant_id,
      "x-session-token": session_token,
      Accept: "application/json",
    };

    const body = {};

    try {
      const url = `${config.get_data_protection_officer}?businessId=${business_id}`;
      let response = await makeAPICall(
        url,
        "GET",
        body,
        headers
      );

      if (response.status == 200) {
        const configId = response.data.configId;
        return response;
      }
    } catch (error) {
      console.log(error);
      throw error;
    }
  };
};

export const submitSystemConfiguration = (
  sslCertificate,
  sslCertificateMeta,
  baseUrl,
  consentExpiry,
  tokenInMinutes,
  artifactExpiry,
  clientId,
  clientSecretKey,
  logo,
  logoMeta,
  artefactRetention,
  keystoreData,
  keystoreName,
  keystorePassword,
  keystoreAlias
) => {
  return async (dispatch, getState) => {
    try {
      let tenant_id = getState().common.tenant_id;
      let session_token = getState().common.session_token;
      let business_id = getState().common.business_id;
      const headers = {
        "business-id": business_id,
        "scope-level": "TENANT",
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
        "Content-Type": "application/json",
        Accept: "application/json",
      };
      const systemConfigBody = {
        sslCertificate: sslCertificate,
        sslCertificateMeta: sslCertificateMeta,
        contentType: "application/json",
        baseUrl: baseUrl,
        defaultConsentExpiryDays: consentExpiry,
        jwtTokenTTLMinutes: tokenInMinutes,
        signedArtifactExpiryDays: artifactExpiry,
        clientId: clientId,
        clientSecret: clientSecretKey,
        logo: logo,
        logoMeta: logoMeta,
        dataRetention: artefactRetention,
        keystoreData: keystoreData,
        alias: keystoreAlias || "cms_signer",
        keystoreName: keystoreName,
        keystorePassword: keystorePassword,
      };
      const body = {
        configurationJson: systemConfigBody,
      };

      let response = await makeAPICall(
        config.create_system_config,
        "POST",
        body,
        headers
      );
      if (response.status == 201) {
        let configId = response.data.configId;
        dispatch({
          type: SAVE_SYSTEM_CONFIG_CONFIG_ID,
          payload: { configId },
        });
      }
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const getSystemConfig = () => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let session_token = getState().common.session_token;
    let business_id = getState().common.business_id;
    const headers = {
      txn: uuidv4(),
      "tenant-id": tenant_id,
      "x-session-token": session_token,
      Accept: "application/json",
    };

    const body = {};

    try {
      const url = `${config.get_system_config}?businessId=${business_id}`;
      let response = await makeAPICall(
        url,
        "GET",
        body,
        headers
      );
      return response;
    } catch (error) {
      console.log(error);
      throw error;
    }
  };
};

export const submitDataProcessor = (
  processorName,
  processorCallBackUrl,
  text,
  dataProcessorCertificate,
  riskAssesmentDocument
) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;

    let session_token = getState().common.session_token;

    let business_id = getState().common.business_id;
    try {
      const headers = {
        "business-id": business_id,
        "scope-level": "TENANT",
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
        "Content-Type": "application/json",
        Accept: "application/json",
      };

      const body = {
        dataProcessorId: "dataProcessorId9090",
        dataProcessorName: processorName,
        callbackUrl: processorCallBackUrl,
        details: text,
        attachment: dataProcessorCertificate,
        contentType: "application/pdf",
        vendorRisk: riskAssesmentDocument,
        vendorRiskContentType: "application/json",
      };
      let response = makeAPICall(
        config.create_data_processor,
        "POST",
        body,
        headers
      );
      if (response.status == 200) {
        let dataProcessorId = response.data.dataProcessorId;
        dispatch({
          type: SAVE_DATA_PROCESSOR_ID,
          payload: { dataProcessorId },
        });

        return response;
      }
    } catch (error) {
      return error;
    }
  };
};

export const updateDPODetails = (
  dpoName,
  dpoemail,
  dpophone,
  dpoaddress,
  dpoConfigId
) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let session_token = getState().common.session_token;
    let business_id = getState().common.business_id;
    let scopeLevel = getState().common.scope_level || "tenant";

    const headers = {
      txn: uuidv4(),
      "tenant-id": tenant_id,
      "business-id": business_id,
      "scope-level": scopeLevel,
      "x-session-token": session_token,
      "Content-Type": "application/json",
      Accept: "application/json",
    };

    const url = `${config.update_data_protection_officer}${dpoConfigId}`;

    const body = {
      configurationJson: {
        name: dpoName,
        email: dpoemail ? dpoemail.toUpperCase() : "",
        mobile: dpophone,
        address: dpoaddress,
      },
    };

    try {
      let response = await makeAPICall(url, "PUT", body, headers);
      return response;
    } catch (error) {
      console.log(error);
      throw error;
    }
  };
};

export const updateSystemConfig = (
  sslCertificate,
  sslCertificateMeta,
  baseUrl,
  consentExpiry,
  tokenInMinutes,
  artifactExpiry,
  clientId,
  clientSecretKey,
  logoBase64,
  logoMeta,
  systemConfigId,
  artefactRetention,
  keystoreData,
  keystoreName,
  keystorePassword,
  keystoreAlias
) => {
  return async (dispatch, getState) => {
    try {
      let tenant_id = getState().common.tenant_id;
      let session_token = getState().common.session_token;
      let business_id = getState().common.business_id;
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
        "Content-Type": "application/json",
        Accept: "application/json",
      };
      const systemConfigBody = {
        sslCertificate: sslCertificate,
        sslCertificateMeta: sslCertificateMeta,
        contentType: "application/json",
        baseUrl: baseUrl,
        defaultConsentExpiryDays: consentExpiry,
        jwtTokenTTLMinutes: tokenInMinutes,
        signedArtifactExpiryDays: artifactExpiry,
        clientId: clientId,
        clientSecret: clientSecretKey,
        logo: logoBase64,
        logoMeta: logoMeta,
        dataRetention: artefactRetention,
        keystoreData: keystoreData,
        alias: keystoreAlias || "cms_signer",
        keystoreName: keystoreName,
        keystorePassword: keystorePassword,
      };
      const url = `${config.update_system_config}${systemConfigId}`;
      const body = {
        configurationJson: systemConfigBody,
      };

      let response = await makeAPICall(url, "PUT", body, headers);

      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const submitPurpose = (purposeCode, purposeName, purposeDesc) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;

    let session_token = getState().common.session_token;

    let business_id = getState().common.business_id;
    try {
      const headers = {
        "business-id": business_id,
        "scope-level": "TENANT",
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
        "Content-Type": "application/json",
        Accept: "application/json",
      };

      const body = {
        purposeCode: purposeCode,
        purposeName: purposeName,
        purposeDescription: purposeDesc,
      };

      let response = await makeAPICall(
        config.create_purpose,
        "POST",
        body,
        headers
      );
      if (response.status == 201) {
        let purposeId = response.data.purposeId;
        dispatch({ type: SAVE_PURPOSE_ID, payload: { purposeId } });
      }

      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const submitUpdatedPurpose = (
  purposeCode,
  purposeName,
  purposeDesc,
  purposeId
) => {
  return async (dispatch, getState) => {
    let session_token = getState().common.session_token;
    let business_id = getState().common.business_id;
    let tenant_id = getState().common.tenant_id;
    try {
      const url = `${config.update_purpose}${purposeId}`;
      const headers = {
        "business-id": business_id,
        "scope-level": "INDIVIDUAL",
        "tenant-id": tenant_id,
        txn: uuidv4(),

        "x-session-token": session_token,
        "Content-Type": "application/json",
        Accept: "application/json",
      };

      const body = {
        purposeCode: purposeCode,
        purposeName: purposeName,
        purposeDescription: purposeDesc,
      };

      let response = await makeAPICall(url, "PUT", body, headers);
      if (response.status == 200) {
        let purposeId = response.data.purposeId;
        dispatch({ type: SAVE_PURPOSE_ID, payload: { purposeId } });
      }
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const getPurposeList = () => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let session_token = getState().common.session_token;
    let business_id = getState().common.business_id;
    try {
      const headers = {
        txn: uuidv4(),
        "business-id": business_id,
        "scope-level": "INDIVIDUAL",
        "tenant-id": tenant_id,
        "x-session-token": session_token,
        "Content-Type": "application/json",
        Accept: "application/json",
      };

      const body = {};
      let response = await makeAPICall(
        config.search_purpose,
        "GET",
        body,
        headers
      );
      if (response.status == 200) return response;
      else {
        return { status: response.status };
      }
    } catch (error) {
      return error;
    }
  };
};

export const createDataType = (dataTypeName, dataItmes) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;

    let session_token = getState().common.session_token;

    let business_id = getState().common.business_id;
    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,

        Accept: "application/json",
        "business-id": business_id,
        "scope-level": "INDIVIDUAL",
      };

      const body = {
        dataTypeName: dataTypeName,
        dataItems: dataItmes,
      };
      let response = await makeAPICall(
        config.create_dataType,
        "POST",
        body,
        headers
      );
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const updateDataType = (dataTypeName, dataItmes, dataTypeId) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;

    let session_token = getState().common.session_token;

    let business_id = getState().common.business_id;
    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,

        Accept: "application/json",
        "business-id": business_id,
        "scope-level": "INDIVIDUAL",
      };

      const body = {
        dataTypeName: dataTypeName,
        dataItems: dataItmes,
      };

      const url = `${config.update_dataType}${dataTypeId}`;
      let response = await makeAPICall(url, "PUT", body, headers);
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const getDataTypes = () => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let session_token = getState().common.session_token;
    let business_id = getState().common.business_id;


    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
        "business-id": business_id,
        Accept: "application/json",
      };

      const body = {};
      const url = `${config.search_dataTypes}?businessId=${business_id}`;

      let response = await makeAPICall(
        url,
        "GET",
        body,
        headers
      );
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const getProcessor = () => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let session_token = getState().common.session_token;
    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,

        Accept: "application/json",
      };

      const body = {};
      let response = await makeAPICall(
        config.search_processor,
        "GET",
        body,
        headers
      );
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const createProcessor = (
  processorName,
  processorCallBackUrl,
  text,
  dataProcessorCertificate,
  riskAssesmentDocument,
  metaDataOfCertificate,
  metaDataOfVendorRiskDocument,
  spocPayload,
  identityType,
  isCrossBordered,
  agreementDocument,
  metaDataOfAgreementDocument
) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;

    let session_token = getState().common.session_token;

    let business_id = getState().common.business_id;
    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,

        "business-id": business_id,
        "scope-level": "TENANT",
        "Content-Type": "application/json",
        Accept: "application/json",
      };

      const body = {
        //dataProcessorId: uuidv4(),
        dataProcessorName: processorName,
        callbackUrl: processorCallBackUrl,
        details: text,
        certificate: dataProcessorCertificate,
        certificateMeta: metaDataOfCertificate,
        vendorRiskDocument: riskAssesmentDocument,
        vendorRiskDocumentMeta: metaDataOfVendorRiskDocument,
        spoc: spocPayload,
        identityType: identityType,
        isCrossBordered: isCrossBordered || false,
      };

      // Add agreement document if provided
      if (agreementDocument && metaDataOfAgreementDocument) {
        body.attachment = agreementDocument;
        body.attachmentMeta = metaDataOfAgreementDocument;
      }


      let response = await makeAPICall(
        config.create_processor,
        "POST",
        body,
        headers
      );
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const updateProcessor = (
  processorName,
  processorCallBackUrl,
  text,
  dataProcessorCertificate,
  riskAssesmentDocument,
  processorId,
  metaDataOfCertificate,
  metaDataOfVendorRiskDocument,
  spocPayload,
  identityType,
  isCrossBordered,
  agreementDocument,
  metaDataOfAgreementDocument
) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;

    let session_token = getState().common.session_token;

    let business_id = getState().common.business_id;
    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,

        "business-id": business_id,
        "scope-level": "TENANT",
        "Content-Type": "application/json",
        Accept: "application/json",
      };

      const body = {
        dataProcessorId: processorId,
        dataProcessorName: processorName,
        callbackUrl: processorCallBackUrl,
        details: text,
        certificate: dataProcessorCertificate,
        certificateMeta: metaDataOfCertificate,
        vendorRiskDocument: riskAssesmentDocument,
        vendorRiskDocumentMeta: metaDataOfVendorRiskDocument,
        spoc: spocPayload,
        identityType: identityType,
        isCrossBordered: isCrossBordered || false,
      };

      // Add agreement document if provided
      if (agreementDocument && metaDataOfAgreementDocument) {
        body.attachment = agreementDocument;
        body.attachmentMeta = metaDataOfAgreementDocument;
      }


      const url = `${config.update_processor}${processorId}`;
      let response = await makeAPICall(url, "PUT", body, headers);
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const getProcessingActivity = () => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let session_token = getState().common.session_token;
    let business_id = getState().common.business_id;
    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
        Accept: "application/json",
      };

      const body = {};
      // Pass business-id as URL parameter
      const url = `${config.search_processingActivity}?businessId=${business_id}`;
      let response = await makeAPICall(
        url,
        "GET",
        body,
        headers
      );
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const createProcessingActivity = (
  activityName,
  selectedProcessor,
  processorId,
  payload,
  activityDetails
) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;

    let session_token = getState().common.session_token;

    let business_id = getState().common.business_id;
    // alert("processorId: ", processorId);
    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
        "business-id": business_id,
        "scope-level": "TENANT",
        "Content-Type": "application/json",
        Accept: "application/json",
      };

      const body = {
        activityName: activityName,
        processorName: selectedProcessor,
        processorId: processorId,
        dataTypeList: payload,
        details: activityDetails,
        status: "ACTIVE",
      };
      let response = await makeAPICall(
        config.create_processingActiity,
        "POST",
        body,
        headers
      );
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const updateProcessingActivity = (
  activityName,
  selectedProcessor,
  processorId,
  payload,
  activityDetails,
  processorActivityId
) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;

    let session_token = getState().common.session_token;

    let business_id = getState().common.business_id;
    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
        "business-id": business_id,
        "scope-level": "TENANT",
        "Content-Type": "application/json",
        Accept: "application/json",
      };

      const body = {
        activityName: activityName,
        processorName: selectedProcessor,
        processorId: processorId,
        dataTypeList: payload,
        details: activityDetails,
        status: "ACTIVE",
      };
      const url = `${config.update_processingActiity}${processorActivityId}`;
      let response = await makeAPICall(url, "PUT", body, headers);
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const clearSession = () => ({
  type: CLEAR_SESSION,
});
export const getLoginUserProfile = () => {
  return async (dispatch, getState) => {
    try {
      let tenant_id = getState().common.login_tenant_id;
      let session_token = getState().common.login_session_token;
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,

        accept: "*/*",
      };

      const body = {};
      let response = await makeAPICall(
        config.login_get_profile,
        "GET",
        body,
        headers
      );
      if (response.status == 200) {
        dispatch({
          type: SAVE_LOGIN_USER_PROFILE_DETAILS,
          payload: response,
        });
      }
      return response;
    } catch (error) {
      throw error;
    }
  };
};

//submit onboarding form
export const submitFormData = (formData, secretCode, identityValue) => {
  return async (dispatch) => {
    try {
      const requestBody = {
        companyName: formData.companyName,
        pan: formData.pan,
        spoc: {
          name: formData.spoc.name,
          email: formData.spoc.email.toUpperCase() || "",
          mobile: formData.spoc.mobile || "",
        },
        identityType: formData.identityType, // EMAIL / MOBILE
      };

      const headers = {
        "Content-Type": "application/json",
        txn: uuidv4(),
      };

      // Add Secret-Code header if provided
      if (secretCode) {
        headers["Secret-Code"] = secretCode;
      }

      // // Add Identity-value header if provided
      // if (identityValue) {
      //   headers["Identity-value"] = identityValue;
      // }

      const response = await fetch(
        config.tenant_onboard,
        {
          method: "POST",
          headers: headers,
          body: JSON.stringify(requestBody),
        }
      );

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw { status: response.status, ...errorData }; // throw structured error
      }

      const responseData = await response.json();
      dispatch({
        type: SAVE_ONBOARD_INFO,
        responseData,
      });

      return responseData;
    } catch (error) {
      console.log("API error:", error);
      throw error;
    }
  };
};

export const triggerOtp = (data) => {
  return async (dispatch) => {

    try {
      const response = await fetch(config.otp_init, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          txn: uuidv4(),
        },
        body: JSON.stringify({
          // pan: data.pan,
          idValue: data.idValue,
          idType: data.idType,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const responseData = await response.json();
      dispatch({
        type: SAVE_SIGNUP_INFO,
        responseData,
      });
      return responseData;
    } catch (error) {
      console.log("OTP trigger failed:", error);
      throw error;
    }
  };
};

export const verifyOtp = async (verifyData) => {
  const normalOtp = verifyData.otp;
  const encryptedOtp = await encryptOnboardOtp(normalOtp);

  try {
    const verifyResponse = await fetch(
      config.otp_validate,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          txn: uuidv4(),
        },
        body: JSON.stringify({
          txnId: verifyData.txn,
          otp: encryptedOtp,
          idValue: verifyData.idValue,
        }),
      }
    );

    if (!verifyResponse.ok) {
      throw new Error(`HTTP error! status: ${verifyResponse.status}`);
    }

    const verify = await verifyResponse.json();
    return verify;
  } catch (error) {
    console.log("Validation failed:", error);
    throw error;
  }
};

const public_key = `MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuoMWq8HXXkGbwLsHHdMJKC6Lk3Jb66xI+pe2Ni44UhaCOxPIYOCbipacLydsBonU1LfKZ8RdCPd60aIqsv5apfoszqqtqxr3/n7drJcUgUKpT7eJ3WCl5qYIutC0Tc5OZ2McqA+tMLCN7wO26cuOAjDfoCLcb6QEk2Lr6xCyjc0V4tEYpqHUBR+Afmd1CIdw8s3PtavEeKo+QWfxIuQB+O9iifto/8QA7uCcksCKRb0tTX0yzHIHXlWiAv1LGY/9C8bW8ju44FQjyr6JBu1WSq8xuzo64qfmfDlRP0GNrVChIW2NK6lhi25TCh2l1PInPJhaUi2DQ9iRUcF9ZSl+eQIDAQAB`;

export const encryptOnboardOtp = async (otp) => {
  try {
    const jsEncrypt = new JSEncrypt(); //create new instance
    jsEncrypt.setPublicKey(public_key);
    const encrypted = jsEncrypt.encrypt(otp);

    return encrypted;
  }
  catch (err) {
    console.log("Error encrypting OTP:", err);
    return null;
  }
};

export const getUserProfile = (tenantId) => {

  return async (dispatch, getState) => {
    try {
      let tenant_id = getState().common.tenant_id;
      let session_token = getState().common.session_token;
      const response = await fetch(
        config.users_profile,
        {
          method: "GET",
          headers: {
            accept: "*/*",
            txn: uuidv4(),
            "tenant-id": tenant_id,
            "x-session-token": session_token,
          },
        }
      );

      if (response.status === 401) {
        // throw new Error(`HTTP error! Status: ${response.status}`);
        return response.status;
      }

      const data = await response.json();

      dispatch({
        type: SAVE_USER_PROFILE,
        data,
      });

      return data;
    } catch (error) {
      console.log("Error fetching user profile:", error);
      throw error;
    }
  };
};

export const saveConsentConfig = (requestBody, requestHeaders) => {
  return async (dispatch, getState) => {
    try {
      const headers = {
        "Content-Type": "application/json",
        txn: uuidv4(),
        ...requestHeaders,
      };
      const body = JSON.stringify(requestBody);
      let response = await makeAPICall(
        config.create_consent_set_up_config,
        "POST",
        body,
        headers
      );

      return response;
    } catch (error) {
      console.log("Error fetching user profile:", error);
      throw error;
    }
  };
};
export const saveDigilockerConfig = (requestBody, requestHeaders) => {
  return async (dispatch, getState) => {
    try {
      const headers = {
        "Content-Type": "application/json",
        txn: uuidv4(),
        ...requestHeaders,
      };
      const body = JSON.stringify(requestBody);
      let response = await makeAPICall(
        config.create_digilocker_config,
        "POST",
        body,
        headers
      );

      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const updateConsentConfig = (requestBody, requestHeaders, configId) => {
  return async (dispatch, getState) => {
    const url = `${config.update_consent_config}${configId}`;
    try {
      const headers = {
        "Content-Type": "application/json",
        txn: uuidv4(),
        ...requestHeaders,
      };
      const body = JSON.stringify(requestBody);
      let response = await makeAPICall(url, "PUT", body, headers);

      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const saveGrievanceConfig = (requestBody, requestHeaders) => {
  return async (dispatch, getState) => {
    try {
      const headers = {
        "Content-Type": "application/json",
        txn: uuidv4(),
        ...requestHeaders,
      };
      const body = JSON.stringify(requestBody);
      const response = await makeAPICall(
        config.create_grievance_config,
        "POST",
        body,
        headers
      );
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const updateGrievanceConfig = (
  requestBody,
  requestHeaders,
  configId
) => {
  return async (dispatch, getState) => {
    const url = `${config.update_grievance_config}${configId}`;
    try {
      const headers = {
        "Content-Type": "application/json",
        txn: uuidv4(),
        ...requestHeaders,
      };
      const body = JSON.stringify(requestBody);
      let response = await makeAPICall(url, "PUT", body, headers);

      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const createNotificationConfig = async (payload, sessionToken, businessId, tenantId) => {
  try {
    const response = await fetch(
      config.notification_create,
      {
        method: "POST",
        headers: {
          "accept": "application/json",
          "Content-Type": "application/json",
          "business-id": businessId,
          "scope-level": "TENANT",
          "txn": uuidv4(),
          "tenant-id": tenantId,
          "x-session-token": sessionToken,
        },
        body: JSON.stringify(payload),
      }
    );

    if (!response.ok) {
      throw new Error(`Error: ${response.status}`);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error("❌ createNotificationConfig failed:", error);
    throw error;
  }
};

export const createSmtpConfig = async (payload, sessionToken, businessId, tenantId, scopeLevel) => {
  try {
    // Ensure Bearer prefix is added if not already present
    const token = sessionToken.startsWith("Bearer ") ? sessionToken : `Bearer ${sessionToken}`;
    
    const response = await fetch(
      config.smtp_create,
      {
        method: "POST",
        headers: {
          "accept": "application/json",
          "Content-Type": "application/json",
          "txn": uuidv4(),
          "tenant-id": tenantId,
          "x-session-token": token,
          "business-id": businessId,
          "scope-level": scopeLevel,
          "provider-type": "SMTP",
        },
        body: JSON.stringify(payload),
      }
    );

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `Error: ${response.status}`);
    }

    const data = await response.json();
    return { status: response.status, data };
  } catch (error) {
    console.error("❌ createSmtpConfig failed:", error);
    throw error;
  }
};

export const updateSmtpConfig = async (payload, sessionToken, businessId, tenantId, scopeLevel, configId) => {
  try {
    // Ensure Bearer prefix is added if not already present
    const token = sessionToken.startsWith("Bearer ") ? sessionToken : `Bearer ${sessionToken}`;
    
    const response = await fetch(
      `${config.smtp_update}/${configId}`,
      {
        method: "PUT",
        headers: {
          "accept": "application/json",
          "Content-Type": "application/json",
          "txn": uuidv4(),
          "tenant-id": tenantId,
          "x-session-token": token,
          "business-id": businessId,
          "scope-level": scopeLevel,
          "provider-type": "SMTP",
        },
        body: JSON.stringify(payload),
      }
    );

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `Error: ${response.status}`);
    }

    const data = await response.json();
    return { status: response.status, data };
  } catch (error) {
    console.error("❌ updateSmtpConfig failed:", error);
    throw error;
  }
};

export const updateNotifConfig = (requestBody, requestHeaders, configId) => {
  return async (dispatch, getState) => {
    const url = `${config.update_notif_config}${configId}`;
    try {
      const headers = {
        "Content-Type": "application/json",
        txn: uuidv4(),
        ...requestHeaders,
      };
      const body = JSON.stringify(requestBody);
      let response = await makeAPICall(url, "PUT", body, headers);

      return response;
    } catch (error) {
      throw error;
    }
  };
};


export const createBusiness = async (payload, token, tenantId) => {
  const url = config.create_business;
  try {

    const headers = {
      accept: "application/json",
      "scope-level": "BUSINESS",
      txn: uuidv4(),
      "tenant-id": tenantId,
      "x-session-token": token,
      "Content-Type": "application/json",
    }

    const res = await fetch(url, {
      method: "POST",
      headers: headers,
      body: JSON.stringify(payload),
    });

    return res.json();
  } catch (error) {
    throw error;
  }
};

// CommonAction.js

// export const fetchBusinessApplications = async (token, tenantId) => {
//   try {
//     const response = await fetch(
//       "https://api.dscoe.jiolabs.com:8443/partnerportal/v1.0/business-application/search",
//       {
//         method: "GET",
//         headers: {
//           accept: "application/json",
//           txn: uuidv4(),
//           "tenant-id": tenantId,
//           "x-session-token": token,
//         },
//       }
//     );

//     if (!response.ok) {
//       throw new Error(`HTTP error! Status: ${response.status}`);
//     }

//     const data = await response.json();
//     return data;
//   } catch (error) {
//     console.error("❌ Error fetching business applications:", error);
//     throw error;
//   }
// };



export const fetchBusinessApplications = (token, tenantId) => {
  return async (dispatch) => {
    try {
      const response = await fetch(
        config.business_application_search,
        {
          method: "GET",
          headers: {
            accept: "application/json",
            txn: uuidv4(),
            "tenant-id": tenantId,
            "x-session-token": token,
          },
        }
      );

      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }

      const data = await response.json();

      const businessList =
        data?.searchList?.map((item) => ({
          businessId: item.businessId,
          name: item.name,
          description: item.description || "",
          createdAt: item.createdAt || item.creationDate || item.created_on || null,
        })) || [];


      // ✅ Dispatch businesses to Redux
      dispatch({
        type: SAVE_BUSINESSES,
        payload: businessList,
      });

      // Don't auto-select first business - let the user profile set the correct one
    } catch (error) {
      console.error("❌ Error fetching business applications:", error);
    }
  };
};



export const createConsentTemplate = async (requestBody, tenantId, sessionToken) => {
  try {
    const res = await fetch(
      config.create_template,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          txn: uuidv4(),
          "tenant-id": tenantId,
          "x-session-token": sessionToken,
        },
        body: JSON.stringify(requestBody),
      }
    );
    const data = await res.json();
    return data;
  } catch (err) {
    console.error("API Error:", err);
    throw err;
  }
};
export const createFormTemplate = async (requestBody, tenantId, businessId, sessionToken, templateId) => {
  
  try {
   const URL = templateId
  ? `${config.create_grievance_template}/${templateId}`
  : config.create_grievance_template;
    const res = await fetch(URL,
      {
        method: templateId ? "PUT" : "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Tenant-ID": tenantId,
          "X-Business-ID": businessId,
          "X-Scope-Level": tenantId === businessId ? 'TENANT' : 'BUSINESS',
          "X-Transaction-ID": uuidv4(),
          "x-session-token": sessionToken,
          Accept: "application/json",
          txn: uuidv4(),
        },
        body: JSON.stringify(requestBody),
      }
    );
    // const data = await res.json();
    return res;
  } catch (err) {
    console.error("API Error:", err);
    throw err;
  }
};

export const editConsentTemplate = async (
  requestBody,
  tenantId,
  templateId,
  version,
  sessionToken
) => {
  const url = `${config.edit_template}${templateId}`;
  try {
    const payload =
      version !== undefined && version !== null
        ? { ...requestBody, version: isNaN(Number(version)) ? version : Number(version) }
        : requestBody;
    const res = await fetch(url, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        txn: uuidv4(),
        "tenant-id": tenantId,
        "x-session-token": sessionToken,
      },
      body: JSON.stringify(payload),
    });

    const data = await res.json();
    return data;
  } catch (err) {
    console.error("Edit API Error:", err);
    throw err;
  }
};

export const getAllTemplates = (tenantId, businessId) => {
  return async (dispatch, getState) => {
    try {
      const session_token = getState().common.session_token;
      const response = await fetch(
        config.template_details,
        {
          method: "GET",
          headers: {
            accept: "*/*",
            txn: uuidv4(),
            "tenant-id": tenantId,
            "business-id": businessId,
            "x-session-token": session_token,
          },
        }
      );


      if (response.status === 401) {
        return response.status;
      }

      const data = await response.json();

      dispatch({
        type: SAVE_TEMPLATES,
        data,
      });

      return data;
    } catch (error) {
      throw error;
    }
  };
};

// src/actions/grievanceActions.js
export const fetchGrievanceTemplates = async (tenantId, businessId, token) => {
  try {
    const response = await fetch(
      config.grievance_templates_list,
      {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          "X-TENANT-ID": tenantId,
          "X-BUSINESS-ID": businessId,
          "X-TRANSACTION-ID": uuidv4(),
          "x-session-token": token
        },
      }
    );

    if (!response.ok) {
      throw new Error(`HTTP error! Status: ${response.status}`);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error("Error fetching grievance templates:", error);
    throw error;
  }
};


export const getConsentsByTemplateId = (templateId) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let session_token = getState().common.session_token;
    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
        Accept: "*/*",
      };

      const body = {};
      const url = `${config.getConsentsByTemplateId}?templateId=${templateId}`;
      let response = await makeAPICall(url, "GET", body, headers);
      let re = response?.data?.searchList;
      return re;
    } catch (error) {
      throw error;
    }
  };
};
export const getGrievanceByTemplateId = (templateId) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let session_token = getState().common.session_token;
    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
        Accept: "*/*",
      };

      const body = {};
      const url = `${config.getGrievanceByTemplateId}/${templateId}`;
      let response = await makeAPICall(url, "GET", body, headers);
      let re = response?.data?.searchList;
      return re;
    } catch (error) {
      throw error;
    }
  };
};

export const getPendingConsentsByTemplateId = (templateId) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let session_token = getState().common.session_token;
    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
        Accept: "*/*",
      };

      const body = {};
      const url = `${config.getPendingConsentsByTemplateId}?templateId=${templateId}`;
      let response = await makeAPICall(url, "GET", body, headers);
      let re = response?.data?.searchList;
      return re;
    } catch (error) {
      throw error;
    }
  };
};

//getConsentHandleDetailsByConsentHandleId
export const getConsentHandleDetailsByConsentHandleId = (consentHandleId) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let session_token = getState().common.session_token;
    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
        Accept: "*/*",
      };

      const body = {};
      const url = `${config.getConsentHandleDetailsByConsentHandleId}/${consentHandleId}`;
      let response = await makeAPICall(url, "GET", body, headers);
      // Return the direct data object, not searchList
      let re = response?.data;
      return re;
    } catch (error) {
      throw error;
    }
  };
};


export const getConsentCountsByTemplateId = (templateId) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let session_token = getState().common.session_token;
    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
        Accept: "*/*",
      };

      const body = {};
      const url = `${config.getConsentCountsByTemplateId}?templateId=${templateId}`;
      let response = await makeAPICall(url, "GET", body, headers);
      let re = response?.data;
      return re;
    } catch (error) {
      throw error;
    }
  };
};

export const getBusinessyDetails = (legalEntityId) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let session_token = getState().common.session_token;
    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
        Accept: "*/*",
      };

      const body = {};
      const url = `${config.searchBusiness}?businessId=${legalEntityId}`;
      let response = await makeAPICall(url, "GET", body, headers);
      let re = response?.data;
      return re;
    } catch (error) {
      throw error;
    }
  };
};

export const getConsentTemplateDetails = async (templateId, tenantId, version, sessionToken) => {

  try {
    const headers = {
      txn: uuidv4(),
      "tenant-id": tenantId,
      "x-session-token": sessionToken,
      accept: "*/*",
    };

    const body = {}; // GET → body not really used, keep empty
    const url = `${config.template_details}?templateId=${templateId}${version ? `&version=${encodeURIComponent(version)}` : ''}`;

    let response = await makeAPICall(url, "GET", body, headers);

    return response?.data;
  } catch (error) {
    console.error("Error fetching template details:", error);
    throw error;
  }
};

export const getConsentDetails = async (token, tenantId, businessId) => {
  try {
    const headers = {
      txn: uuidv4(),
      "tenant-id": tenantId,
      "x-session-token": token,
      accept: "*/*",
    };

    const url = `${config.get_consent_details}?businessId=${businessId}`;
    const consentRes = await fetch(url, {
      method: "GET",
      headers: headers,
    });
    const data = await consentRes.json();



    return data;
  } catch (error) {
    throw error;
  }
};

export const getDigilockerDetails = async (token, tenantId, businessId) => {
  try {
    const headers = {
      txn: uuidv4(),
      "tenant-id": tenantId,
      "x-session-token": token,
      accept: "*/*",
    };

    const url = `${config.get_digilocker_details}?businessId=${businessId}`;
    const response = await fetch(url, {
      method: "GET",
      headers: headers,
    });
    const data = await response.json();

    return data;
  } catch (error) {
    throw error;
  }
};

// export const getConsentDetails = (token, tenantId) => async (dispatch) => {
//   try {
//     const headers = {
//       txn: uuidv4(),
//       "tenant-id": tenantId,
//       "x-session-token": token,
//       accept: "*/*",
//     };

//     const consentRes = await fetch(config.get_consent_details, {
//       method: "GET",
//       headers: headers,
//     });

//     const data = await consentRes.json();
//     console.log("Response:", data);

//     dispatch({ type: SAVE_CONSENT_DETAILS, data }); // ✅ Now dispatch works

//     return data;
//   } catch (error) {
//     console.error("Error in getConsentDetails:", error);
//     throw error;
//   }
// };

export const getGrievanceDetails = async (token, tenantId, businessId) => {
  try {
    const headers = {
      txn: uuidv4(),
      "tenant-id": tenantId,
      "x-session-token": token,
      accept: "*/*",
    };
    const url = `${config.get_grievance_details}?businessId=${businessId}`;
    const res = await fetch(url, {
      method: "GET",
      headers: headers,
    });
    const data = await res.json();
    return data;
  } catch (error) {
    throw error;
  }
};
export const getGrievnaceTemplateData = (templateId, version = null) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let business_id = getState().common.business_id;

    // for testing
    // let business_id = "bc14f61e-137e-4221-934e-300f7730412a";
    // let tenant_id = "bbca49bf-542e-4ee8-869f-885aafbbb9a8";
    let session_token = getState().common.session_token;

    try {
      const headers = {
        "X-Tenant-ID": tenant_id,
        "X-Business-ID": business_id,
        "X-Transaction-ID": uuidv4(),
        "X-Scope-Level": tenant_id === business_id ? 'TENANT' : 'BUSINESS',
        "x-session-token": session_token,
        Accept: "application/json",
      };

      const body = {};

      // Include version parameter if provided
      let url = `${config.getGrievnaceTemplateData}/${templateId}`;
      if (version) {
        url += `?version=${version}`;
      }
      
      let response = await makeAPICall(
        url,
        "GET",
        body,
        headers
      );
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const getNotifDetails = async (token, tenantId, businessId) => {
  try {
    const headers = {
      txn: uuidv4(),
      "tenant-id": tenantId,
      "x-session-token": token,
      accept: "*/*",
    };

    const url = `${config.get_notif_details}?businessId=${businessId}`;
    const consentRes = await fetch(url, {
      method: "GET",
      headers: headers,
    });
    const data = await consentRes.json();
    return data;
  } catch (error) {
    throw error;
  }
};

export const getSmtpDetails = async (token, tenantId, businessId) => {
  try {
    // Ensure Bearer prefix is added if not already present
    const authToken = token.startsWith("Bearer ") ? token : `Bearer ${token}`;
    
    const headers = {
      accept: "application/json",
      txn: uuidv4(),
      "tenant-id": tenantId,
      "x-session-token": authToken,
    };

    const url = `${config.smtp_search}?businessId=${businessId}`;
    const response = await fetch(url, {
      method: "GET",
      headers: headers,
    });
    
    // If 404, return empty result (no config exists yet)
    if (response.status === 404) {
      return { searchList: [] };
    }
    
    if (!response.ok) {
      throw new Error(`Error: ${response.status}`);
    }
    
    const data = await response.json();
    return data;
  } catch (error) {
    console.error("❌ getSmtpDetails failed:", error);
    // Return empty result instead of throwing for 404 cases
    if (error.message && error.message.includes("404")) {
      return { searchList: [] };
    }
    throw error;
  }
};

// export const fetchBusinessApplication = async (tenantId, token) => {
//   return async (dispatch) => {
//   try {
//     const url = `http://10.173.184.32:30002/partnerportal/v1.0/business-application/search?businessId=${tenantId}`;

//     const response = await fetch(url, {
//       method: "GET",
//       headers: {
//         accept: "application/json",
//         txn: uuidv4(), 
//         "tenant-id": tenantId,
//         "x-session-token": token,
//       },
//     });

//     if (!response.ok) {
//       throw new Error(`HTTP error! Status: ${response.status}`);
//     }

//     const data = await response.json();

//     // ✅ Extract name and first character
//     const companyName = data?.searchList?.[0]?.name || "";
//     const firstChar = companyName ? companyName.charAt(0).toUpperCase() : "";
//     alert(companyName);
//     console.log("Fetched business application:", { companyName, firstChar });

//     dispatch({
//       type: FETCH_BUSINESS_SUCCESS,
//       payload: {
//         raw: data,
//         companyName,
//         firstChar,
//       },
//     });

//     return { companyName, firstChar };
//   } catch (error) {
//     console.error("❌ Error fetching business application:", error);
//     return { companyName: "", firstChar: "" };
//   }
// }
// };

// commonaction.js



export const getTranslations = (inputTexts, targetLanguage, sourceLanguage = 'en') => {
  return async (dispatch, getState) => {
    if (!targetLanguage || targetLanguage.toLowerCase() === sourceLanguage.toLowerCase()) {
      return {
        output: inputTexts.map(item => ({ id: item.id, target: item.source }))
      };
    }

    try {
      const tenant_id = getState().common.tenant_id;
      const business_id = getState().common.business_id;
      const session_token = getState().common.session_token;

      const headers = {
        'Content-Type': 'application/json',
        'tenantid': tenant_id,
        'businessid': business_id,
        'txn': uuidv4(),
        'x-session-token': session_token,
      };

      const body = {
        provider: 'BHASHINI',
        source: 'CONSENTPOPUP',
        language: {
          sourceLanguage: sourceLanguage,
          targetLanguage: targetLanguage,
        },
        input: inputTexts,
      };

      const response = await axios.post(config.translator_translate, body, { headers });
      return response.data;
    } catch (error) {
      console.error('Error fetching translations:', error);
      return {
        output: inputTexts.map(item => ({ id: item.id, target: item.source }))
      };
    }
  };
};

export const fetchBusinessApplication = (tenantId, token) => {
  return async (dispatch) => {
    try {
      const url = `${config.business_application_search}?businessId=${tenantId}`;

      const response = await fetch(url, {
        method: "GET",
        headers: {
          accept: "application/json",
          txn: uuidv4(),
          "tenant-id": tenantId,
          "x-session-token": token,
        },
      });

      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }

      const data = await response.json();
      const companyName = data?.searchList?.[0]?.name || "";
      const firstChar = companyName ? companyName.charAt(0).toUpperCase() : "";

      dispatch({
        type: FETCH_BUSINESS_SUCCESS,
        payload: {
          raw: data,
          companyName,
          firstChar,
        },
      });

      return { companyName, firstChar };
    } catch (error) {
      console.error("❌ Error fetching business application:", error);
      return { companyName: "", firstChar: "" };
    }
  };
};


export const getClientCredential = async (businessId, token, tenantId) => {
  try {
    const response = await fetch(
      `${config.client_credential}?businessId=${businessId}`,
      {
        method: "GET",
        headers: {
          accept: "application/json",
          "tenant-id": tenantId,
          txn: uuidv4(),
          "x-session-token": token,
        },
      }
    );

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error("Error fetching client credential:", error);
    throw error;
  }
};

export const createRole = (role, description, permissions) => {
  return async (dispatch, getState) => {
    const tenant_id = getState().common.tenant_id;
    const session_token = getState().common.session_token;

    try {
      const headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "txn": uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
      };

      const body = {
        role,
        description,
        permissions,
      };

      const requestDetails = {
        method: "POST",
        headers: headers,
        body: JSON.stringify(body),
      };


      const response = await fetch(config.create_role, requestDetails);

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({
          errorMsg: "Request failed with status: " + response.status
        }));
        throw errorData;
      }

      const responseData = await response.json().catch(() => ({}));

      return {
        status: response.status,
        data: responseData
      };

    } catch (error) {
      console.error("Error in createRole action:", error);
      throw error;
    }
  };
};

export const createUser = (userData) => {
  return async (dispatch, getState) => {
    const tenant_id = getState().common.tenant_id;
    const session_token = getState().common.session_token;
    const business_id = getState().common.business_id;

    try {
      const headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "txn": uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
        "business-id": business_id,
      };

      // Determine identity type based on email field value
      // Check if it's a valid email format
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      // Check if it's a valid mobile number format (10 digits)
      const mobileRegex = /^\d{10}$/;

      let identityType = "EMAIL"; // Default
      let isMobile = false;

      if (userData.email) {
        if (mobileRegex.test(userData.email.trim())) {
          identityType = "MOBILE";
          isMobile = true;
        } else if (emailRegex.test(userData.email.trim())) {
          identityType = "EMAIL";
          isMobile = false;
        }
      }

      // Build body with dynamic key based on identity type
      // Convert email to uppercase when sending
      const body = {
        username: userData.name,
        ...(isMobile ? { mobile: userData.email } : { email: userData.email.toUpperCase() }),
        designation: userData.designation,
        identityType: identityType,
        roles: userData.roles
      };

      const requestDetails = {
        method: "POST",
        headers: headers,
        body: JSON.stringify(body),
      };


      const response = await fetch(config.createUser, requestDetails);

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({
          errorMsg: "Request failed with status: " + response.status
        }));
        throw errorData;
      }

      const responseData = await response.json().catch(() => ({}));

      return {
        status: response.status,
        data: responseData
      };

    } catch (error) {
      console.error("Error in createUser action:", error);
      throw error;
    }
  };
};


export const updateUser = (userId, payload) => {
  return async (dispatch, getState) => {
    const tenant_id = getState().common.tenant_id;
    const session_token = getState().common.session_token;
    const business_id = getState().common.business_id;

    try {
      const headers = {
        "Content-Type": "application/json",
        "txn": uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
        "business-id": business_id,
      };

      // Determine identity type based on email field value
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      const mobileRegex = /^\d{10}$/;

      let identityType = "EMAIL"; // Default
      let isMobile = false;

      if (payload.email) {
        if (mobileRegex.test(payload.email.trim())) {
          identityType = "MOBILE";
          isMobile = true;
        } else if (emailRegex.test(payload.email.trim())) {
          identityType = "EMAIL";
          isMobile = false;
        }
      } else if (payload.mobile) {
        identityType = "MOBILE";
        isMobile = true;
      }

      // Build body with dynamic key and identityType
      const body = {
        username: payload.username,
        ...(isMobile
          ? { mobile: payload.email || payload.mobile }
          : { email: (payload.email || "").toUpperCase() }),
        designation: payload.designation,
        identityType: identityType,
        roles: payload.roles
      };


      const response = await fetch(`${config.updateUser}?userId=${userId}`, {
        method: "PUT",
        headers: headers,
        body: JSON.stringify(body),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw errorData;
      }

      const responseData = await response.json().catch(() => ({}));
      return { status: response.status, data: responseData };

    } catch (error) {
      console.error("Error in updateUser action:", error);
      throw error;
    }
  };
};

export const searchUsers = (userId) => {
  return async (dispatch, getState) => {
    const tenant_id = getState().common.tenant_id;
    const session_token = getState().common.session_token;

    try {
      const headers = {
        "Content-Type": "application/json",
        "txn": uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
      };

      const response = await fetch(`${config.searchUsers}?userId=${userId}`, {
        method: "GET",
        headers: headers,
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw errorData;
      }

      const responseData = await response.json();
      return responseData;

    } catch (error) {
      throw error;
    }
  };
};
export const getCookieCategories = () => {
  return async (dispatch, getState) => {
    const tenant_id = getState().common.tenant_id;
    const session_token = getState().common.session_token;
    try {
      const headers = {
        "Accept": "application/json",
        "X-Tenant-ID": tenant_id,
        "x-session-token": session_token,
      };
      const response = await makeAPICall(
        config.get_cookie_categories,
        "GET",
        null,
        headers
      );
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const createCookieCategory = (category, description) => {
  return async (dispatch, getState) => {
    const tenant_id = getState().common.tenant_id;
    const session_token = getState().common.session_token;
    const business_id = getState().common.business_id;
    try {
      const headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "X-Tenant-ID": tenant_id,
        "x-session-token": session_token,
        "business-id": business_id,
        "txn": uuidv4(),
      };
      const body = {
        category,
        description,
      };
      const response = await makeAPICall(
        config.create_cookie_category,
        "POST",
        body,
        headers
      );
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const updateCookieCategory = (category, description) => {
  return async (dispatch, getState) => {
    const tenant_id = getState().common.tenant_id;
    const session_token = getState().common.session_token;
    try {
      const headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "X-Tenant-ID": tenant_id,
        "x-session-token": session_token,
      };
      const body = {
        category,
        description,
      };
      const response = await makeAPICall(
        config.update_cookie_category,
        "PUT",
        body,
        headers
      );
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const updateRole = (roleId, payload) => {
  return async (dispatch, getState) => {
    const tenant_id = getState().common.tenant_id;
    const session_token = getState().common.session_token;
    try {
      const headers = {
        "Content-Type": "application/json", "Accept": "application/json", "txn": uuidv4(), "tenant-id": tenant_id, "x-session-token": session_token,
      };
      const response = await fetch(`${config.updateRole}?roleId=${roleId}`, {
        method: "PUT", headers: headers, body: JSON.stringify(payload),
      });
      if (!response.ok) { throw await response.json().catch(() => ({})) }
      return { status: response.status, data: await response.json().catch(() => ({})) };
    } catch (error) {
      console.error("Error in updateRole action:", error);
      throw error;
    }
  };
};

export const searchRoles = (roleId) => {
  return async (dispatch, getState) => {
    const tenant_id = getState().common.tenant_id;
    const session_token = getState().common.session_token;
    try {
      const headers = {
        "Content-Type": "application/json", "Accept": "application/json", "txn": uuidv4(), "tenant-id": tenant_id, "x-session-token": session_token,
      };
      const response = await fetch(`${config.searchRoles}?roleId=${roleId}`, {
        method: "GET", headers: headers,
      });
      if (!response.ok) { throw await response.json().catch(() => ({})) }
      return await response.json();
    } catch (error) {
      console.error("Error in searchRoles action:", error);
      throw error;
    }
  };
};

export const listRoles = () => {
  return async (dispatch, getState) => {
    const tenant_id = getState().common.tenant_id;
    const session_token = getState().common.session_token;
    try {
      const headers = {
        "Content-Type": "application/json", "Accept": "application/json", "txn": uuidv4(), "tenant-id": tenant_id, "x-session-token": session_token,
      };
      const response = await fetch(config.listRoles, { method: "GET", headers: headers });
      if (!response.ok) { throw await response.json().catch(() => ({})) }
      return await response.json();
    } catch (error) {
      console.error("Error in listRoles action:", error);
      throw error;
    }
  };
};

export const deleteRole = (roleId) => {
  return async (dispatch, getState) => {
    const tenant_id = getState().common.tenant_id;
    const session_token = getState().common.session_token;
    try {
      const headers = {
        "Content-Type": "application/json", "Accept": "application/json", "txn": uuidv4(), "tenant-id": tenant_id, "x-session-token": session_token,
      };
      const response = await fetch(`${config.deleteRole}?roleId=${roleId}`, {
        method: "DELETE", headers: headers,
      });
      if (!response.ok) { throw await response.json().catch(() => ({})) }
      return { status: response.status };
    } catch (error) {
      console.error("Error in deleteRole action:", error);
      throw error;
    }
  };
};

export const listUsers = () => {
  return async (dispatch, getState) => {
    const tenant_id = getState().common.tenant_id;
    const session_token = getState().common.session_token;
    try {
      const headers = {
        "Content-Type": "application/json", "Accept": "application/json", "txn": uuidv4(), "tenant-id": tenant_id, "x-session-token": session_token,
      };
      const response = await fetch(config.listUsers, { method: "GET", headers: headers });
      if (!response.ok) { throw await response.json().catch(() => ({})) }
      return await response.json();
    } catch (error) {
      console.error("Error in listUsers action:", error);
      throw error;
    }
  };
};

export const deleteUser = (userId) => {
  return async (dispatch, getState) => {
    const tenant_id = getState().common.tenant_id;
    const session_token = getState().common.session_token;
    try {
      const headers = {
        "Content-Type": "application/json", "Accept": "application/json", "txn": uuidv4(), "tenant-id": tenant_id, "x-session-token": session_token,
      };
      const response = await fetch(`${config.deleteUser}?userId=${userId}`, {
        method: "DELETE", headers: headers,
      });
      if (!response.ok) { throw await response.json().catch(() => ({})) }
      return { status: response.status };
    } catch (error) {
      console.error("Error in deleteUser action:", error);
      throw error;
    }
  };
};

export const deleteRopaEntry = (ropaId) => {
  return async (dispatch, getState) => {
    const tenant_id = getState().common.tenant_id;
    const session_token = getState().common.session_token;
    
    if (!ropaId) {
      throw new Error("ROPA ID is required for deletion");
    }
    
    try {
      // Ensure Bearer prefix is added if not already present
      const authToken = session_token.startsWith("Bearer ") ? session_token : `Bearer ${session_token}`;
      
      const txnId = uuidv4();
      const url = `${config.ropa_delete}/${ropaId}`;
      
      const headers = {
        "accept": "application/json",
        "txn": txnId,
        "tenant-id": tenant_id,
        "x-session-token": authToken,
      };
      
      console.log("Delete ROPA Request:", {
        url,
        ropaId,
        tenant_id,
        method: "DELETE"
      });
      
      const response = await fetch(url, {
        method: "DELETE",
        headers: headers,
      });
      
      console.log("Delete ROPA Response:", {
        status: response.status,
        statusText: response.statusText,
        ok: response.ok
      });
      
      if (!response.ok) {
        let errorData;
        try {
          errorData = await response.json();
        } catch (e) {
          errorData = { message: `HTTP ${response.status}: ${response.statusText}` };
        }
        
        console.error("Delete ROPA Error:", {
          status: response.status,
          statusText: response.statusText,
          errorData
        });
        
        // Create a structured error object
        const error = new Error(errorData.message || errorData.errorMsg || `Failed to delete ROPA entry: ${response.status} ${response.statusText}`);
        error.status = response.status;
        error.errorData = errorData;
        throw error;
      }
      
      // Handle 204 No Content or other success statuses
      return { status: response.status };
    } catch (error) {
      console.error("Error in deleteRopaEntry action:", error);
      // Re-throw with more context
      if (error.status) {
        throw error; // Already structured
      }
      throw new Error(error.message || "Failed to delete ROPA entry");
    }
  };
};

export const searchBusiness = (businessId) => {
  return async (dispatch, getState) => {
    const tenant_id = getState().common.tenant_id;
    const session_token = getState().common.session_token;

    try {
      const headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "txn": uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
      };

      // If businessId is provided, use query parameter, otherwise fetch all
      const url = businessId
        ? `${config.searchBusiness}?businessId=${businessId}`
        : config.searchBusiness;

      const response = await fetch(url, {
        method: "GET",
        headers: headers,
      });

      if (!response.ok) {
        throw await response.json().catch(() => ({}));
      }

      return await response.json();

    } catch (error) {
      console.error("Error in searchBusiness action:", error);
      throw error;
    }
  };
};




export const getEmailTemplates = () => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let session_token = getState().common.session_token;
    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
        Accept: "application/json",
      };

      const body = {};
      let response = await makeAPICall(
        "dfsfd", // <-- your endpoint key from config
        "GET",
        body,
        headers
      );
      return response;
    } catch (error) {
      throw error;
    }
  };
};
export const createEmailTemplate = (payload) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let session_token = getState().common.session_token;
    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenant_id,
        "x-session-token": session_token,
        Accept: "application/json",
      };

      const body = { payload };
      let response = await makeAPICall(
        "email_template_create", // TODO- chnage endpoint key from config
        "POST",
        body,
        headers
      );
      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const postUserType = ({ name, description }) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;

    let session_token = getState().common.session_token;

    let business_id = getState().common.business_id;
    try {
      const headers = {
        "X-Tenant-ID": tenant_id,
        "X-Business-ID": business_id,
        "X-Scope-Level": tenant_id === business_id ? 'TENANT' : 'BUSINESS',
        "X-Transaction-ID": uuidv4(),
        "x-session-token": session_token,
        Accept: "application/json",
      };

      const body = {
        name,
        description: description || "",
      };

      let response = await makeAPICall(
        config.create_userTypes,
        "POST",
        body,
        headers
      );
      if (response.status == 201) {
        let purposeId = response.data.purposeId;
        dispatch({ type: SAVE_PURPOSE_ID, payload: { purposeId } });
      }

      return response;
    } catch (error) {
      throw error;
    }
  };
};
export const postGrievanceType = ({
  grievanceType,
  grievanceItem,
  description,
  tenant_id,
  businessId,
}) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let business_id = getState().common.business_id;

    let session_token = getState().common.session_token;
    try {
      const headers = {
        "X-Tenant-ID": tenant_id,
        "X-Business-ID": business_id,
        "X-Transaction-ID": uuidv4(),
        "X-Scope-Level": tenant_id === business_id ? 'TENANT' : 'BUSINESS',
        "x-session-token": session_token,
        Accept: "application/json",
      };

      const body = {
        grievanceType,
        grievanceItem,
        description: description || "",
      };

      let response = await makeAPICall(
        config.create_grievance_type,
        "POST",
        body,
        headers
      );
      if (response.status == 201) {
        let purposeId = response.data.purposeId;
        dispatch({ type: SAVE_PURPOSE_ID, payload: { purposeId } });
      }

      return response;
    } catch (error) {
      throw error;
    }
  };
};
export const putGrievanceType = ({
  grievanceType,
  grievanceItem,
  description,
  grievanceTypeId,
}) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let business_id = getState().common.business_id;

    let session_token = getState().common.session_token;
    try {
      const headers = {
        "X-Tenant-ID": tenant_id,
        "X-Business-ID": business_id,
        "X-Transaction-ID": uuidv4(),
        "X-Scope-Level": tenant_id === business_id ? 'TENANT' : 'BUSINESS',
        "x-session-token": session_token,
        "Accept": "application/json",
      };

      const body = {
        grievanceType,
        grievanceItem,
        description: description || "",
      };
      const url = `${config.create_grievance_type}/${grievanceTypeId}`;

      let response = await makeAPICall(url, "PUT", body, headers);
      if (response.status == 201) {
        let purposeId = response.data.purposeId;
        dispatch({ type: SAVE_PURPOSE_ID, payload: { purposeId } });
      }

      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const listComponents = () => {
  return async (dispatch, getState) => {
    const tenant_id = getState().common.tenant_id;
    const session_token = getState().common.session_token;
    try {
      const headers = {
        "Content-Type": "application/json", "Accept": "application/json", "txn": uuidv4(), "tenant-id": tenant_id, "x-session-token": session_token,
      };
      const response = await fetch(config.listComponents, { method: "GET", headers: headers });
      if (!response.ok) { throw await response.json().catch(() => ({})) }
      return await response.json();
    } catch (error) {
      console.error("Error in listComponents action:", error);
      throw error;
    }
  };
};


export const getUserTypes = () => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let session_token = getState().common.session_token;
    let business_id = getState().common.business_id;
    const headers = {
      "X-Tenant-ID": tenant_id,
      "X-Business-ID": business_id,
      "X-Scope-Level": tenant_id === business_id ? 'TENANT' : 'BUSINESS',
      "X-Transaction-ID": uuidv4(),
      "x-session-token": session_token,
      Accept: "application/json",
    };

    const body = {};

    try {
      let response = await makeAPICall(
        config.get_userTypes,
        "GET",
        body,
        headers
      );
      return response;
    } catch (error) {
      throw error;
    }
  };
};
export const getUserDetails = () => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let session_token = getState().common.session_token;
    let business_id = getState().common.business_id;
    const headers = {
      txn: uuidv4(),
      "X-Tenant-ID": tenant_id,
      "X-Business-ID": business_id,
      "X-Scope-Level": tenant_id === business_id ? 'TENANT' : 'BUSINESS',
      "X-Transaction-ID": uuidv4(),
      "x-session-token": session_token,
      Accept: "application/json",
    };

    const body = {};

    try {
      let response = await makeAPICall(
        config.get_userDetails,
        "GET",
        body,
        headers
      );
      return response;
    } catch (error) {
      throw error;
    }
  };
};
export const postUserDetail = ({ name, description }) => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;

    let session_token = getState().common.session_token;

    let business_id = getState().common.business_id;
    try {
      const headers = {
        "X-Tenant-ID": tenant_id,
        "X-Business-ID": business_id,
        "X-Scope-Level": tenant_id === business_id ? 'TENANT' : 'BUSINESS',
        "X-Transaction-ID": uuidv4(),
        "x-session-token": session_token,
        "Accept": "application/json",
        "Content-Type": "application/json"
      };

      const body = {
        name,
        description: description || "",
      };

      let response = await makeAPICall(
        config.create_userDetails,
        "POST",
        body,
        headers
      );
      if (response.status == 201) {
        let purposeId = response.data.purposeId;
        dispatch({ type: SAVE_PURPOSE_ID, payload: { purposeId } });
      }

      return response;
    } catch (error) {
      throw error;
    }
  };
};

export const getGrievanceType = () => {
  return async (dispatch, getState) => {
    let tenant_id = getState().common.tenant_id;
    let session_token = getState().common.session_token;
    let business_id = getState().common.business_id;
    const headers = {
      txn: uuidv4(),
      "X-Tenant-ID": tenant_id,
      "X-Business-ID": business_id,
      "X-Scope-Level": tenant_id === business_id ? 'TENANT' : 'BUSINESS',
      "X-Transaction-ID": uuidv4(),
      "x-session-token": session_token,
      Accept: "application/json",
    };

    const body = {};

    try {
      let response = await makeAPICall(
        config.get_grievance_type,
        "GET",
        body,
        headers
      );
      return response;
    } catch (error) {
      throw error;
    }
  };
};

// src/actions/translationActions.js
export const translateConfig = async (tenantId, businessId, sessionToken) => {
  try {
    const body = {
      scopeLevel: "BUSINESS",
      config: {
        provider: "BHASHINI",
        apiBaseUrl: "https://meity-auth.ulcacontrib.org",
        modelPipelineEndpoint: "/ulca/apis/v0/model/getModelsPipeline",
        callbackUrl: "https://dhruva-api.bhashini.gov.in/services/inference/pipeline",
        userId: "02c3fec39bbf46cda8ece45ba52e78cb",
        apiKey: "058a0399da-ea84-4077-9e3e-984ff46f8b77",
        pipelineId: "64392f96daac500b55c543cd",
      },
    };

    const response = await fetch(config.translator_translateConfig, {
      method: "POST",
      headers: {
        "txn": uuidv4(),
        "tenantid": tenantId,
        "businessid": businessId,
        "x-session-token": sessionToken,
        "Content-Type": "application/json",
        Accept: "*/*",
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! Status: ${response.status}`);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error("Error in translateConfig API:", error);
    throw error;
  }
};

export const getGrievances = (tenantId, businessId, page = 1, size = 10) => {
  return async (dispatch, getState) => {
    try {
      const session_token = getState().common.session_token;
      const res = await fetch(
        `${config.grievances_search}?page=${page}&size=${size}`,
        {
          method: "GET",
          headers: {
            "X-Tenant-ID": tenantId,
            "X-Business-ID": businessId,
            "X-Transaction-ID": uuidv4(),
            "x-session-token": session_token,
            Accept: "*/*",
          },
        }
      );

      const json = await res.json();
      return json; // IMPORTANT: Return response to component
    } catch (error) {
      console.error("Error fetching grievances:", error);
      return null;
    }
  };
};

export const fetchAuditReports = (tenantId, businessId) => {
  return async () => {
    try {
      const headers = {
        accept: "*/*",
        "X-Tenant-ID": tenantId,
        "X-Business-ID": businessId,
        "X-Transaction-ID": uuidv4(),
      };

      const response = await makeAPICall(
        config.get_audit_reports,
        "GET",
        null,
        headers
      );
      return response;
    } catch (error) {
      console.error("Error fetching audit reports:", error);
      throw error;
    }
  };
};

export const createAuditReport = (requestBody, tenantId, businessId) => {
  return async () => {
    try {
      const headers = {
        accept: "*/*",
        "X-Tenant-ID": tenantId,
        "X-Business-ID": businessId,
        "X-Transaction-ID": uuidv4(),
        "Content-Type": "application/json",
      };

      const response = await makeAPICall(
        config.get_audit_reports,
        "POST",
        requestBody,
        headers
      );
      return response;
    } catch (error) {
      console.error("Error creating audit report:", error);
      throw error;
    }
  };
};


