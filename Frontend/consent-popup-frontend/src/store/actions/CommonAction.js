import { makeAPICall } from "../../utils/ApiCall";
import {
  SAVE_CONFIG,
  SAVE_CUSID_TNTID,
  SAVE_MOBILE_NO,
} from "../constants/Constants";
import config from "../../utils/config";

const uuidv4 = () => {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0,
      v = c == "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

export const createConsentHandleId =
  (tenantId, templateId, mobileNo) => async (dispatch) => {
    try {
      const headers = {
        txn: uuidv4(),
        "tenant-id": tenantId,
        Accept: "*/*",
        "Content-Type": "application/json",
      };

      const body = {
        customerIdentifiers: {
          value: mobileNo,
          type: "MOBILE",
        },
        templateId: templateId,
        templateVersion: 1,
      };

      const response = await makeAPICall(
        config.createHandleId,
        "POST",
        body,
        headers
      );
      console.log("Response from API:", response);

      return response;
    } catch (error) {
      console.log("Error in createConsentHandleId:", error);
      throw error;
    }
  };

export const saveCustomerMobileNo = (mobileNo) => {
  return async (dispatch, getState) => {
    dispatch({
      type: SAVE_MOBILE_NO,
      payload: { mobileNo },
    });
  };
};

export const saveConfigurationDetails = (tenantId, businessId, templateId) => {
  return async (dispatch, getState) => {
    dispatch({
      type: SAVE_CONFIG,
      payload: { tenantId, businessId, templateId },
    });
  };
};
