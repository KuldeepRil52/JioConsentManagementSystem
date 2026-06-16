import { textStyle } from "../utils/textStyles";
import { ICON_SIZE } from "../utils/iconSizes";
import "../Styles/requests.css";

import { useEffect, useState, useMemo } from "react";
import "../Styles/configurePage.css";
import {
  getGrievnaceRequest,
  getGrievnaceRequestByParams,
} from "../store/actions/CommonAction";
import { useDispatch, useSelector } from "react-redux";
import React from "react";
import { useNavigate } from "react-router";
import { GRIEVANCE_STATUS } from "../store/constants/Constants";
import StarRating from "./StarRating";
import StarIntegrationRating from "./StarIntegrationRating";
import useTranslation from "../hooks/useTranslation";
import { FaInfoCircle, FaSort } from "react-icons/fa";
import { AiFillClockCircle } from "react-icons/ai";

const IntegrationGrievanceRequests = () => {
  const [loadLogs, setLoadLogs] = useState(true);

  const [expandedRow, setExpandedRow] = useState(null);
  const dispatch = useDispatch();
  const [grievanceData, setGrievanceData] = useState([]);
  const [resolvedCount, setResolvedCount] = useState(0);
  const [inProgressCount, setInProgressCount] = useState(0);
  const [newCount, setNewCount] = useState(0);

  const integration_tenant_id = useSelector(
    (state) => state.common.integration_tenant_id,
  );
  const integration_business_id = useSelector(
    (state) => state.common.integration_business_id,
  );
  const integration_grievanceTemplate_id = useSelector(
    (state) => state.common.integration_grievanceTemplate_id,
  );

  // Translation inputs for static text
  const translationInputs = useMemo(
    () => [
      { id: "int_requests_title", source: "Requests" },
      { id: "int_requests_badge", source: "Grievance redressal" },
      { id: "int_requests_raise_new", source: "Raise new request" },
      { id: "int_requests_new", source: "New" },
      { id: "int_requests_in_process", source: "In Process" },
      { id: "int_requests_resolved", source: "Resolved" },
      { id: "int_requests_grievance_id", source: "Grievance ID" },
      { id: "int_requests_date_submitted", source: "Date submitted" },
      { id: "int_requests_last_updated", source: "Last updated" },
      { id: "int_requests_grievance_type", source: "Grievance type" },
      { id: "int_requests_remarks", source: "Remarks" },
      { id: "int_requests_status", source: "Status" },
      { id: "int_requests_actions", source: "Actions" },
      { id: "int_requests_feedback", source: "Feedback" },
      { id: "int_requests_no_data", source: "No Data to Display" },
    ],
    [],
  );

  // Use translation hook
  const { getTranslation, translateContent, currentLanguage } =
    useTranslation(translationInputs);

  // Translate content when language changes
  useEffect(() => {
    if (currentLanguage !== "ENGLISH") {
      translateContent(translationInputs, currentLanguage);
    }
  }, [currentLanguage, translationInputs, translateContent]);
  const params = new URLSearchParams(window.location.search);
  const encodedData = params.get("data");

  let tenantId = "";
  let businessId = "";
  let grievanceTemplateId = "";

  if (encodedData) {
    try {
      const decoded = atob(encodedData);
      const parsed = JSON.parse(decoded);

      tenantId = parsed.tenantId || "";
      businessId = parsed.businessId || "";
      grievanceTemplateId = parsed.grievanceTemplateId || "";
    } catch (err) {
      console.log("Error decoding Base64 data param:", err);
    }
  }
  tenantId = tenantId || integration_tenant_id;
  businessId = businessId || integration_business_id;
  grievanceTemplateId = grievanceTemplateId || integration_grievanceTemplate_id;
  const navigate = useNavigate();
  const createGrievanceRequest = () => {
    navigate("/integrationCreateRequest");
  };

  useEffect(() => {
    const fetchData = async () => {
      try {
        const searchList = await dispatch(
          getGrievnaceRequestByParams(
            tenantId,
            businessId,
            grievanceTemplateId,
          ),
        );
        if (searchList.status === 200) {
          const data = [...(searchList?.data?.data || [])].reverse();

          const resolved = data.filter(
            (item) => item.status === GRIEVANCE_STATUS.GRIEVANCE_RESOLVED,
          ).length;
          const inProgress = data.filter(
            (item) => item.status === GRIEVANCE_STATUS.GRIEVANCE_INPROCESS,
          ).length;
          const newReqCount = data.filter(
            (item) => item.status === GRIEVANCE_STATUS.NEW,
          ).length;
          // Update states
          setResolvedCount(resolved);
          setInProgressCount(inProgress);
          setNewCount(newReqCount);
          setGrievanceData(data);
        }
      } catch (err) {
        console.log("Error occured in getConsentsByTemplateId", err);
        if (err?.[0]?.errorCode === "JCMP3001") {
          setGrievanceData([]);
        }
      } finally {
        setLoadLogs(false);
      }
    };

    fetchData();
  }, []);

  return (
    <>
      <div className="pageConfig">
        <div className="fiduciaryHeaderContainer">
          <div className="fiduciaryHeader">
            <span style={textStyle("heading-s", "primary-grey-100")}>
              Requests
            </span>
            <div className="badge">
              <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                Grievance redressal
              </span>
            </div>
          </div>
          <div className="requestsButtonContainer">
            <button />
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
        </div>
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
                  New
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
              <AiFillClockCircle size={ICON_SIZE} />
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
                  In Process
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
              <FaCheckCircle style={{ color: "green" }} size={ICON_SIZE} />
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
                  Resolved
                </span>
              </div>
            </div>
          </div>
        </div>

        <div style={{ overflowX: "auto" }}>
          <table className="consentLog-table">
            <thead>
              <tr>
                <th className="parentheader">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      Grievance ID
                    </span>
                    <FaSort size={ICON_SIZE} />
                  </div>
                </th>
                <th className="parentheader">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      Date submitted
                    </span>
                    <MdUnfoldMore
                      size={ICON_SIZE}
                      style={{ cursor: "pointer", color: "#0a2885" }}
                    />
                  </div>
                </th>
                <th className="parentheader">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      Last updated
                    </span>
                    <MdUnfoldMore
                      size={ICON_SIZE}
                      style={{ cursor: "pointer", color: "#0a2885" }}
                    />{" "}
                  </div>
                </th>
                <th className="parentheader">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      Grievance type
                    </span>
                    <MdUnfoldMore
                      size={ICON_SIZE}
                      style={{ cursor: "pointer", color: "#0a2885" }}
                    />
                  </div>
                </th>
                <th className="parentheader">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      Remarks
                    </span>
                  </div>
                </th>
                <th className="parentheader">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      Status
                    </span>
                    <MdUnfoldMore
                      size={ICON_SIZE}
                      style={{ cursor: "pointer", color: "#0a2885" }}
                    />
                  </div>
                </th>
                <th className="parentheader">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      Actions
                    </span>
                  </div>
                </th>
                <th className="parentheader">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      Feedback
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
                      No Data to Display
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
                        {grievance.grievanceType || ""}
                      </span>
                    </td>
                    <td>
                      <span style={textStyle("body-xs", "primary-grey-100")}>
                        {grievance.grievanceDescription || ""}
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
                        {grievance.status || ""}
                      </span>
                    </td>

                    <td className="consent-actions-cell">
                      <FaEye
                        size={ICON_SIZE}
                        style={{ cursor: "pointer" }}
                        onClick={() =>
                          grievance?.grievanceId &&
                          navigate(
                            `/integrationGrievanceRequests/${grievance.grievanceId}`,
                          )
                        }
                      />
                    </td>
                    <td style={{ whiteSpace: "nowrap" }}>
                      {grievance.status ===
                        GRIEVANCE_STATUS.GRIEVANCE_RESOLVED && (
                        <StarIntegrationRating
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
    </>
  );
};
export default IntegrationGrievanceRequests;
