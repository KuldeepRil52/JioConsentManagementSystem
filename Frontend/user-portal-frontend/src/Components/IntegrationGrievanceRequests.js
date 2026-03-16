import {
  ActionButton,
  BadgeV2,
  Icon,
  Modal,
  SearchBox,
  Spinner,
  Text,
} from "@jds/core";
import "../Styles/requests.css";

import {
  IcInfo,
  IcSort,
  IcStar,
  IcSuccessColored,
  IcTime,
  IcVisible,
} from "@jds/core-icons";
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

const IntegrationGrievanceRequests = () => {
  const [loadLogs, setLoadLogs] = useState(true);

  const [expandedRow, setExpandedRow] = useState(null);
  const dispatch = useDispatch();
  const [grievanceData, setGrievanceData] = useState([]);
  const [resolvedCount, setResolvedCount] = useState(0);
  const [inProgressCount, setInProgressCount] = useState(0);
  const [newCount, setNewCount] = useState(0);

  const integration_tenant_id = useSelector(
    (state) => state.common.integration_tenant_id
  );
  const integration_business_id = useSelector(
    (state) => state.common.integration_business_id
  );
  const integration_grievanceTemplate_id = useSelector(
    (state) => state.common.integration_grievanceTemplate_id
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
    []
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
          getGrievnaceRequestByParams(tenantId, businessId, grievanceTemplateId)
        );
        if (searchList.status === 200) {
          const data = [...(searchList?.data?.data || [])].reverse();

          const resolved = data.filter(
            (item) => item.status === GRIEVANCE_STATUS.GRIEVANCE_RESOLVED
          ).length;
          const inProgress = data.filter(
            (item) => item.status === GRIEVANCE_STATUS.GRIEVANCE_INPROCESS
          ).length;
          const newReqCount = data.filter(
            (item) => item.status === GRIEVANCE_STATUS.NEW
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
            <Text appearance="heading-s" color="primary-grey-100">
              Requests
            </Text>
            <div className="badge">
              <Text appearance="body-xs-bold" color="primary-grey-80">
                Grievance redressal
              </Text>
            </div>
          </div>
          <div className="requestsButtonContainer">
            <ActionButton
              kind="primary"
              size="large"
              label="Raise new request"
              onClick={createGrievanceRequest}
            ></ActionButton>
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
              <IcInfo height={35} width={35} />
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "flex-start",
                }}
              >
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  {newCount || "0"}
                </Text>
                <Text appearance="body-xs" color="primary-grey-80">
                  New
                </Text>
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
              <Icon
                ic={<IcTime height={35} width={35} />}
                color="primary_60"
                size="large"
              />
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "flex-start",
                }}
              >
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  {inProgressCount || "0"}
                </Text>
                <Text appearance="body-xs" color="primary-grey-80">
                  In Process
                </Text>
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
              <IcSuccessColored height={35} width={35} />
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "flex-start",
                }}
              >
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  {resolvedCount || "0"}
                </Text>
                <Text appearance="body-xs" color="primary-grey-80">
                  Resolved
                </Text>
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
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Grievance ID
                    </Text>
                    <Icon ic={<IcSort />} color="primary_60" size="small" />
                  </div>
                </th>
                <th className="parentheader">
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Date submitted
                    </Text>
                    <Icon ic={<IcSort />} color="primary_60" size="small" />
                  </div>
                </th>
                <th className="parentheader">
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Last updated
                    </Text>
                    <Icon ic={<IcSort />} color="primary_60" size="small" />
                  </div>
                </th>
                <th className="parentheader">
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Grievance type
                    </Text>
                    <Icon ic={<IcSort />} color="primary_60" size="small" />
                  </div>
                </th>
                <th className="parentheader">
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Remarks
                    </Text>
                  </div>
                </th>
                <th className="parentheader">
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Status
                    </Text>
                    <Icon ic={<IcSort />} color="primary_60" size="small" />
                  </div>
                </th>
                <th className="parentheader">
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Actions
                    </Text>
                  </div>
                </th>
                <th className="parentheader">
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Feedback
                    </Text>
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
                      <Spinner
                        kind="normal"
                        labelPosition="right"
                        size="small"
                      />
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
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      No Data to Display
                    </Text>
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
                      <Text appearance="body-xs" color="primary-grey-100">
                        {grievance.grievanceId || ""}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="primary-grey-100">
                        {grievance.createdAt || ""}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="primary-grey-100">
                        {grievance.updatedAt || ""}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="primary-grey-100">
                        {grievance.grievanceType || ""}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="primary-grey-100">
                        {grievance.grievanceDescription || ""}
                      </Text>
                    </td>
                    <td>
                      <span
                        style={{
                          display: "inline-block",
                          fontWeight: "700",
                          fontSize: "14px",
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
                            : { backgroundColor: "#f1f1f1", color: "#333" }),
                        }}
                      >
                        {grievance.status || ""}
                      </span>
                    </td>

                    <td>
                      <IcVisible
                        height={20}
                        width={20}
                        style={{ cursor: "pointer" }}
                        onClick={() =>
                          grievance?.grievanceId &&
                          navigate(
                            `/integrationGrievanceRequests/${grievance.grievanceId}`
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
