// src/store/reducers/CommonReducer.js
import {
  SAVE_CUSID_TNTID,
  CLEAR_SESSION,
  SAVE_MOBILE_NO,
  SAVE_CONFIG,
} from "../constants/Constants";
import {} from "../constants/Constants";
const initialState = {
  tenant_id: "589c95c6-4bb8-4678-8c0b-c15c736cb46d",
  customer_id: "",
  mobile_no: "",
  business_id: "589c95c6-4bb8-4678-8c0b-c15c736cb46d",
  template_id: "86e7e715-da74-4410-a05f-5c40d39a6f84",
};

const commonReducer = (state = initialState, action) => {
  switch (action.type) {
    case SAVE_MOBILE_NO:
      return {
        ...state,
        mobile_no: action.payload.mobileNo,
      };

    case SAVE_CONFIG:
      return {
        ...state,
        tenant_id: action.payload.tenantId,
        business_id: action.payload.businessId,
        template_id: action.payload.templateId,
      };
    default:
      return state;
  }
};

export default commonReducer;
