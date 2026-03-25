import { textStyle } from "../utils/textStyles";
import { ICON_SIZE } from "../utils/iconSizes";
import "../Styles/requests.css";

import { useEffect, useState, useMemo } from "react";
import "../Styles/configurePage.css";
import {
  getGrievnaceRequestByGrievanceTemplateId,
  getGrievnaceRequestByUserId,
  translateData,
  translateConfiguration,
} from "../store/actions/CommonAction";
import { useDispatch, useSelector } from "react-redux";
import React from "react";
import { useNavigate } from "react-router";
import { GRIEVANCE_STATUS } from "../store/constants/Constants";
import StarRating from "./StarRating";
import SessionExpiredModal from "./SessionExpiredModal";
import useTranslation, {
  getApiValueFromLanguage,
} from "../hooks/useTranslation";
import {
  FaCheckCircle,
  FaClock,
  FaEye,
  FaInfoCircle,
  FaSort,
} from "react-icons/fa";
import { FiFilter } from "react-icons/fi";
import { MdUnfoldMore } from "react-icons/md";
import { HiSelector } from "react-icons/hi";

const Requests = () => {
  const [loadLogs, setLoadLogs] = useState(true);
  const tenantId = useSelector((state) => state.common.tenant_id);
  const [expandedRow, setExpandedRow] = useState(null);
  const dispatch = useDispatch();
  const [grievanceData, setGrievanceData] = useState([]);
  const [resolvedCount, setResolvedCount] = useState(0);
  const [inProgressCount, setInProgressCount] = useState(0);
  const [newCount, setNewCount] = useState(0);
  const [sessionExpired, setSessionExpired] = useState(false);
  const grievanceId = useSelector(
    (state) => state.common.grievance_template_id,
  );

  // Calculate counts when grievanceData changes
  useEffect(() => {
    if (grievanceData && grievanceData.length > 0) {
      let newCnt = 0;
      let inProcessCnt = 0;
      let resolvedCnt = 0;

      grievanceData.forEach((grievance) => {
        const status = grievance?.status;
        if (status === GRIEVANCE_STATUS.NEW) {
          newCnt++;
        } else if (status === GRIEVANCE_STATUS.GRIEVANCE_RESOLVED) {
          resolvedCnt++;
        } else if (
          status === GRIEVANCE_STATUS.GRIEVANCE_RAISED ||
          status === GRIEVANCE_STATUS.GRIEVANCE_INPROCESS ||
          status === GRIEVANCE_STATUS.GRIEVANCE_L1_ESCALATED ||
          status === GRIEVANCE_STATUS.GRIEVANCE_L2_ESCALATED
        ) {
          inProcessCnt++;
        }
      });

      setNewCount(newCnt);
      setInProgressCount(inProcessCnt);
      setResolvedCount(resolvedCnt);
    } else {
      setNewCount(0);
      setInProgressCount(0);
      setResolvedCount(0);
    }
  }, [grievanceData]);

  const navigate = useNavigate();
  const createGrievanceRequest = () => {
    navigate("/createRequest");
  };

  const customerIdentity = useSelector((state) => state.common.customer_id);

  const [emailMobile, setEmailMobile] = useState("");

  const [disableRaiseButton, setDisableRaiseButton] = useState(false);
  const [dynamicTranslations, setDynamicTranslations] = useState({}); // Store translated dynamic content

  // Translation inputs for static text in Requests page
  const translationInputs = useMemo(
    () => [
      { id: "requests_title", source: "Requests" },
      { id: "requests_badge", source: "Grievance redressal" },
      { id: "requests_raise_new", source: "Raise new request" },
      { id: "requests_new", source: "New" },
      { id: "requests_in_process", source: "In Process" },
      { id: "requests_resolved", source: "Resolved" },
      { id: "requests_grievance_id", source: "Grievance ID" },
      { id: "requests_date_submitted", source: "Date submitted" },
      { id: "requests_last_updated", source: "Last updated" },
      { id: "requests_grievance_type", source: "Grievance type" },
      { id: "requests_remarks", source: "Remarks" },
      { id: "requests_status", source: "Status" },
      { id: "requests_actions", source: "Actions" },
      { id: "requests_feedback", source: "Feedback" },
      { id: "requests_no_data", source: "No Data to Display" },
      // Dynamic status values
      { id: "requests_status_raised", source: "RAISED" },
      { id: "requests_status_inprocess", source: "INPROCESS" },
      { id: "requests_status_l1_escalated", source: "L1_ESCALATED" },
      { id: "requests_status_l2_escalated", source: "L2_ESCALATED" },
      { id: "requests_status_resolved", source: "RESOLVED" },
      { id: "requests_status_new", source: "NEW" },
      // Grievance type categories
      {
        id: "requests_type_consent_related",
        source: "Consent Related Grievance",
      },
      { id: "requests_type_data_principal", source: "Data Principal Rights" },
      { id: "requests_type_legal", source: "Legal Grievances" },
    ],
    [],
  );

  // Use translation hook
  const { getTranslation, translateContent, currentLanguage, isTranslating } =
    useTranslation(translationInputs);

  // Helper to translate dynamic status values
  const getTranslatedStatus = (status) => {
    const statusMap = {
      RAISED: "requests_status_raised",
      INPROCESS: "requests_status_inprocess",
      L1_ESCALATED: "requests_status_l1_escalated",
      L2_ESCALATED: "requests_status_l2_escalated",
      RESOLVED: "requests_status_resolved",
      NEW: "requests_status_new",
    };
    const translationId = statusMap[status?.toUpperCase()];
    return translationId ? getTranslation(translationId, status) : status;
  };

  // Helper to translate grievance type categories
  const getTranslatedGrievanceType = (type) => {
    const typeMap = {
      "Consent Related Grievance": "requests_type_consent_related",
      "Data Principal Rights": "requests_type_data_principal",
      "Legal Grievances": "requests_type_legal",
    };
    const translationId = typeMap[type];
    return translationId ? getTranslation(translationId, type) : type;
  };

  // Helper to get dynamic translation with fallback
  const getDynamicTranslation = (text) => {
    if (!text || currentLanguage === "ENGLISH") return text;
    return dynamicTranslations[text] || text;
  };

  // Function to translate dynamic table data (remarks/descriptions)
  const translateDynamicData = async (data, targetLang) => {
    if (!data || data.length === 0 || targetLang === "en") {
      setDynamicTranslations({});
      return;
    }

    // Collect all unique translatable text
    const uniqueTexts = new Set();
    data.forEach((item) => {
      if (item.grievanceDescription) uniqueTexts.add(item.grievanceDescription);
    });

    if (uniqueTexts.size === 0) return;

    // Create translation inputs
    const inputs = Array.from(uniqueTexts).map((text, idx) => ({
      id: `dynamic_${idx}`,
      source: text,
    }));

    try {
      const response = await dispatch(translateData("en", targetLang, inputs));
      if (response?.status === 200 && response?.data?.output) {
        const translatedMap = {};
        response.data.output.forEach((item, idx) => {
          const originalText = inputs[idx].source;
          translatedMap[originalText] = item.target;
        });
        setDynamicTranslations(translatedMap);
      }
    } catch (error) {
      console.log("Error translating dynamic data:", error);
    }
  };

  // Translate content when language changes
  useEffect(() => {
    if (currentLanguage !== "ENGLISH") {
      translateContent(translationInputs, currentLanguage);
      // Also translate dynamic table data (remarks/descriptions)
      if (grievanceData && grievanceData.length > 0) {
        const targetLang = getApiValueFromLanguage(currentLanguage);
        translateDynamicData(grievanceData, targetLang);
      }
    } else {
      setDynamicTranslations({}); // Reset for English
    }
  }, [currentLanguage, translationInputs, translateContent, grievanceData]);
  useEffect(() => {
    const fetchData = async () => {
      try {
        const mobileRegex = /^[6-9]\d{9}$/;
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

        let emailMobile = "";

        if (mobileRegex.test(customerIdentity)) {
          emailMobile = "mob";
        } else if (emailRegex.test(customerIdentity)) {
          emailMobile = "ema";
        }

        const searchList = await dispatch(
          getGrievnaceRequestByGrievanceTemplateId(),
        );

        if (emailMobile === "mob" && searchList.includes("Mobile Number")) {
          let res = await dispatch(
            getGrievnaceRequestByUserId("Mobile Number"),
          );

          setGrievanceData(res?.data?.data);
        }

        if (emailMobile === "ema" && searchList.includes("Email address")) {
          let res = await dispatch(
            getGrievnaceRequestByUserId("Email address"),
          );
          setGrievanceData(res?.data?.data);
        }
      } catch (err) {
        setDisableRaiseButton(true);
        if (err.code === "ERR_NETWORK") {
          setSessionExpired(true);
        }
        console.log(
          "Error occured in getConsentsByTemplateId",
          JSON.stringify(err),
        );
        if (err?.[0]?.errorCode === "JCMP3001") {
          setGrievanceData([]);
        }
      } finally {
        setLoadLogs(false);
      }
    };

    fetchData();
  }, [customerIdentity, dispatch]);

  return (
    <main role="main" aria-label="Grievance Requests Page">
      <div className="pageConfig">
        <header className="fiduciaryHeaderContainer" role="banner">
          <div className="fiduciaryHeader">
            <h1>
              <span style={textStyle("heading-s", "primary-grey-100")}>
                {getTranslation("requests_title", "Requests")}
              </span>
            </h1>
            <div
              className="badge"
              role="status"
              aria-label="Page type: Grievance redressal"
            >
              <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                {getTranslation("requests_badge", "Grievance redressal")}
              </span>
            </div>
            {sessionExpired && (
              <SessionExpiredModal
                isOpen={sessionExpired}
                onClose={() => setSessionExpired(false)}
              />
            )}
          </div>
          <div className="requestsButtonContainer">
            <button
              onClick={createGrievanceRequest}
              disabled={disableRaiseButton}
              aria-label="Raise a new grievance request"
              style={{
                backgroundColor: "#1f4ed8", // blue color
                color: "#fff",
                border: "none",
                padding: "12px 24px",
                borderRadius: "999px", // fully rounded
                fontSize: "11px",
                fontWeight: "600",
                cursor: disableRaiseButton ? "not-allowed" : "pointer",
                opacity: disableRaiseButton ? 0.6 : 1,
                transition: "0.2s",
              }}
              onMouseEnter={(e) => {
                if (!disableRaiseButton) {
                  e.currentTarget.style.backgroundColor = "#1a3fb8";
                }
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = "#1f4ed8";
              }}
            >
              {getTranslation("requests_raise_new", "Raise new request")}
            </button>
          </div>
        </header>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "1rem",
            marginTop: "2rem",
            marginBottom: "2rem",
          }}
        >
          <div
            style={{
              width: "214px",
              height: "90px",
              backgroundColor: "rgba(245, 245, 245, 1)",
              borderRadius: "12px",
              display: "flex",

              gap: "0.5rem",
              alignItems: "center",
            }}
          >
            <div
              style={{
                display: "flex",
                paddingTop: "1rem",
                paddingLeft: "1rem",
                paddingBottom: "1rem",
                gap: "0.5rem",
                alignItems: "center",
              }}
            >
              <FaInfoCircle size={ICON_SIZE} />
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "flex-start",
                }}
              >
                <span style={textStyle("body-xs-bold", "primary-grey-100")}>
                  {newCount || "0"}
                </span>
                <span style={textStyle("body-xs", "primary-grey-80")}>
                  {getTranslation("requests_new", "New")}
                </span>
              </div>
            </div>
          </div>
          <div
            style={{
              width: "214px",
              height: "90px",
              backgroundColor: "rgba(231, 235, 248, 1)",
              borderRadius: "12px",
              display: "flex",

              gap: "0.5rem",
              alignItems: "center",
            }}
          >
            <div
              style={{
                display: "flex",
                paddingTop: "1rem",
                paddingLeft: "1rem",
                paddingBottom: "1rem",
                gap: "0.5rem",
                alignItems: "center",
              }}
            >
              <FaClock size={ICON_SIZE} style={{ color: "#0a2885" }} />

              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "flex-start",
                }}
              >
                <span style={textStyle("body-xs-bold", "primary-grey-100")}>
                  {inProgressCount || "0"}
                </span>
                <span style={textStyle("body-xs", "primary-grey-80")}>
                  {getTranslation("requests_in_process", "In Process")}
                </span>
              </div>
            </div>
          </div>

          <div
            style={{
              width: "214px",
              height: "90px",
              backgroundColor: "rgba(233, 247, 233, 1)",
              borderRadius: "12px",
              display: "flex",
              alignItems: "center",
            }}
          >
            <div
              style={{
                display: "flex",
                paddingTop: "1rem",
                paddingLeft: "1rem",
                paddingBottom: "1rem",
                alignItems: "center",
                gap: "0.5rem",
              }}
            >
              <FaCheckCircle size={ICON_SIZE} style={{ color: "green" }} />
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "flex-start",
                }}
              >
                <span style={textStyle("body-xs-bold", "primary-grey-100")}>
                  {resolvedCount || "0"}
                </span>
                <span style={textStyle("body-xs", "primary-grey-80")}>
                  {getTranslation("requests_resolved", "Resolved")}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div
          style={{ overflowX: "auto" }}
          role="region"
          aria-label="Grievance Requests Table"
          tabIndex={0}
        >
          <table
            className="consentLog-table"
            role="table"
            aria-label="List of grievance requests with details"
          >
            <thead>
              <tr role="row">
                <th className="parentheader" scope="col" role="columnheader">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      {getTranslation("requests_grievance_id", "Grievance ID")}
                    </span>
                    <MdUnfoldMore
                      size={ICON_SIZE}
                      style={{ cursor: "pointer", color: "#0a2885" }}
                    />
                  </div>
                </th>
                <th className="parentheader" scope="col" role="columnheader">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      {getTranslation(
                        "requests_date_submitted",
                        "Date submitted",
                      )}
                    </span>
                    <MdUnfoldMore
                      size={ICON_SIZE}
                      style={{ cursor: "pointer", color: "#0a2885" }}
                    />
                  </div>
                </th>
                <th className="parentheader" scope="col" role="columnheader">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      {getTranslation("requests_last_updated", "Last updated")}
                    </span>
                    <MdUnfoldMore
                      size={ICON_SIZE}
                      style={{ cursor: "pointer", color: "#0a2885" }}
                    />
                  </div>
                </th>
                <th className="parentheader" scope="col" role="columnheader">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      {getTranslation(
                        "requests_grievance_type",
                        "Grievance type",
                      )}
                    </span>
                    <MdUnfoldMore
                      size={ICON_SIZE}
                      style={{ cursor: "pointer", color: "#0a2885" }}
                    />
                  </div>
                </th>
                <th className="parentheader" scope="col" role="columnheader">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      {getTranslation("requests_remarks", "Remarks")}
                    </span>
                  </div>
                </th>
                <th className="parentheader" scope="col" role="columnheader">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      {getTranslation("requests_status", "Status")}
                    </span>
                    <MdUnfoldMore
                      size={ICON_SIZE}
                      style={{ cursor: "pointer", color: "#0a2885" }}
                    />
                  </div>
                </th>
                <th className="parentheader" scope="col" role="columnheader">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      {getTranslation("requests_actions", "Actions")}
                    </span>
                  </div>
                </th>
                <th className="parentheader" scope="col" role="columnheader">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      {getTranslation("requests_feedback", "Feedback")}
                    </span>
                  </div>
                </th>
              </tr>
            </thead>

            <tbody>
              {loadLogs ? (
                <tr>
                  <td
                    colSpan="6"
                    style={{ textAlign: "center", padding: "1rem" }}
                  >
                    <div className="customerActivityLoader">
                      <>
                        <style>
                          {`
      @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
      }
    `}
                        </style>

                        <div
                          style={{
                            width: "20px",
                            height: "20px",
                            border: "3px solid #f3f3f3",
                            borderTop: "3px solid #0a2885",
                            borderRadius: "50%",
                            animation: "spin 1s linear infinite",
                          }}
                        />
                      </>
                    </div>
                  </td>
                </tr>
              ) : !Array.isArray(grievanceData) ||
                grievanceData.length === 0 ? (
                <tr>
                  <td
                    colSpan="6"
                    style={{ textAlign: "center", padding: "1rem" }}
                  >
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      {getTranslation("requests_no_data", "No Data to Display")}
                    </span>
                  </td>
                </tr>
              ) : (
                Array.isArray(grievanceData) &&
                grievanceData?.map((grievance, index) => (
                  <tr
                    key={grievance?.grievanceId || `grievance-${index}`}
                    className={
                      expandedRow === `row${index}` ? "expanded-row-parent" : ""
                    }
                  >
                    <td style={{ textAlign: "left" }}>
                      <span style={textStyle("body-xs", "primary-grey-100")}>
                        {grievance.grievanceId || ""}
                      </span>
                    </td>
                    <td>
                      <span style={textStyle("body-xs", "primary-grey-100")}>
                        {grievance.createdAt || ""}
                      </span>
                    </td>
                    <td>
                      <span style={textStyle("body-xs", "primary-grey-100")}>
                        {grievance.updatedAt || ""}
                      </span>
                    </td>
                    <td>
                      <span style={textStyle("body-xs", "primary-grey-100")}>
                        {getTranslatedGrievanceType(grievance.grievanceType) ||
                          ""}
                      </span>
                    </td>
                    <td>
                      <span style={textStyle("body-xs", "primary-grey-100")}>
                        {getDynamicTranslation(
                          grievance.grievanceDescription,
                        ) || ""}
                      </span>
                    </td>
                    <td>
                      <span
                        style={{
                          display: "inline-block",
                          fontWeight: "700",
                          fontSize: "11px",
                          lineHeight: "24px",
                          borderRadius: "6px",
                          padding: "0 10px",
                          textAlign: "center",
                          whiteSpace: "nowrap",
                          ...(grievance?.status ===
                            GRIEVANCE_STATUS.GRIEVANCE_L1_ESCALATED ||
                          grievance?.status ===
                            GRIEVANCE_STATUS.GRIEVANCE_L2_ESCALATED
                            ? {
                                backgroundColor: "rgba(255, 242, 236, 1)",
                                color: "rgba(245, 0, 49, 1)",
                              }
                            : grievance?.status ===
                                GRIEVANCE_STATUS.GRIEVANCE_RESOLVED
                              ? {
                                  backgroundColor: "rgba(233, 247, 233, 1)",
                                  color: "rgba(19, 86, 16, 1)",
                                }
                              : grievance?.status ===
                                  GRIEVANCE_STATUS.GRIEVANCE_INPROCESS
                                ? {
                                    backgroundColor: "rgba(231, 235, 248, 1)",
                                    color: "rgba(10, 40, 133, 1)",
                                  }
                                : {
                                    backgroundColor: "#f1f1f1",
                                    color: "#333",
                                  }),
                        }}
                      >
                        {getTranslatedStatus(grievance.status) || ""}
                      </span>
                    </td>

                    <td className="consent-actions-cell">
                      <span
                        role="button"
                        tabIndex={0}
                        aria-label={`View grievance ${grievance.grievanceId}`}
                        onClick={() =>
                          grievance?.grievanceId &&
                          navigate(`/requests/${grievance.grievanceId}`)
                        }
                        onKeyDown={(e) => {
                          if (e.key === "Enter" || e.key === " ") {
                            e.preventDefault();
                            grievance?.grievanceId &&
                              navigate(`/requests/${grievance.grievanceId}`);
                          }
                        }}
                        style={{ cursor: "pointer", display: "inline-flex" }}
                      >
                        <FaEye size={ICON_SIZE} />
                      </span>
                    </td>
                    <td style={{ whiteSpace: "nowrap" }}>
                      {grievance.status ===
                        GRIEVANCE_STATUS.GRIEVANCE_RESOLVED && (
                        <StarRating
                          initialRating={grievance.feedback || 0}
                          readOnly={false}
                          grievanceId={grievance.grievanceId}
                        />
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </main>
  );
};
export default Requests;
