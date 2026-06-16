import React, { useState, useEffect } from "react";
import "../styles/createConsent.css";
import {
  ActionButton,
  Icon,
  Image,
  Text,
  Button,
  InputFieldV2,
  TabItem,
  Tabs,
  InputCheckbox,
  InputRadio,
  Selectors,
  InputToggle,
} from "../custom-components";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { useRef } from "react";
import Select from "react-select";
import PurposeContainer from "./PurposeContainer";
import LanguageContainer from "./LanguageContainer";
import Branding from "./Branding";
import { IcNotes } from "../custom-components/Icon";
import { createConsentTemplate } from "../store/actions/CommonAction";
import { IcAlarmOff, IcAlignBottom } from "../custom-components/Icon";
import ConsentTable from "../utils/consentTable";
import { useParams } from "react-router-dom";

// ✅ import your API function
import { getProcessor } from "../store/actions/CommonAction";
import config from "../utils/config";
import { formatToISTUSFormat } from "../utils/dateUtils";
import { isSandboxMode, sandboxAPICall } from "../utils/sandboxMode";

const EditRequest = () => {
  const { reqId } = useParams();
  console.log("reqId: ", reqId);
  const navigate = useNavigate();
  const dispatch = useDispatch();

 
  const [status, setStatus] = useState(""); // initially empty
  const [showIdentifier, setShowIdentifier] = useState(true);
  const [requestDetails, setRequestDetails] = useState(null);
  const [userName, setUserName] = useState("");
  const [statusHistory, setStatusHistory] = useState([]);
  const [consentData, setConsentData] = useState(null);
  const [hasMobileOrEmail, setHasMobileOrEmail] = useState(false);

  const tenantId = useSelector((state) => state.common.tenant_id);
  const businessId = useSelector((state) => state.common.business_id);
  const sessionToken = useSelector((state) => state.common.session_token);

  const handleBack = () => {
    navigate("/request");
  };

  // Generate UUID for transaction ID
  const generateTransactionId = () => {
    return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
      const r = (Math.random() * 16) | 0,
        v = c == "x" ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });
  };

  const handleStatusChange = async (option) => {
    if (!option || !requestDetails?.grievanceId) {
      return;
    }

    const previousStatus = status;
    const newStatus = option.value;
    setStatus(newStatus);

    try {
      const headers = {
        "Content-Type": "application/json",
        "tenant-id": tenantId,
        "business-id": businessId,
        "x-session-token": sessionToken,
        "x-transaction-id": generateTransactionId(),
      };

      const requestBody = {
        // name: userName || "N/A",
        status: newStatus, // Status values are already in API format
        resolutionRemark: "Status updated",
      };

      const response = await fetch(
        `${config.grievance_details}/${requestDetails.grievanceId}`,
        {
          method: "PUT",
          headers: headers,
          body: JSON.stringify(requestBody),
        }
      );

      const data = await response.json();

      if (response.ok) {
        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message={"Status updated successfully."}
            />
          ),
          { icon: false }
        );
        // Refresh the data to get updated history
        const refreshHeaders = {
          "tenant-id": tenantId,
          "business-id": businessId,
          "x-session-token": sessionToken,
          page: 1,
          size: 10,
          "transactionid": generateTransactionId(),
        };
        const refreshRes = await fetch(
          `${config.grievance_details}/${requestDetails.grievanceId}`,
          { method: "GET", headers: refreshHeaders }
        );
        const refreshData = await refreshRes.json();
        if (refreshRes.ok && refreshData) {
          // Update status history
          if (refreshData.history && Array.isArray(refreshData.history)) {
            const timeline = [];
            if (refreshData.history.length > 0) {
              timeline.push({
                status: "NEW",
                updatedAt: refreshData.createdAt || refreshData.updatedAt,
              });
            }
            refreshData.history.forEach((entry) => {
              timeline.push({
                status: entry.newStatus,
                updatedAt: entry.updatedAt,
              });
            });
            setStatusHistory(timeline);
          }
        }
      } else {
        throw new Error(data?.message || "Failed to update status");
      }
    } catch (error) {
      console.error("Error updating status:", error);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={error.message || "Failed to update status. Please try again."}
          />
        ),
        { icon: false }
      );
      // Revert status on error
      setStatus(previousStatus);
    }
  };

  const customStyles = {
    control: (base, state) => ({
      ...base,
      color: "#6b0909ff",
      backgroundColor: "#fff",
      border: "1px solid #ccc",
      borderRadius: "8px",
      padding: "2px 4px",
      fontSize: "14px",
      minHeight: "40px",

      // remove blue glow
      boxShadow: "none",
      borderColor: state.isFocused ? "#ccc" : "#ccc", // keep same on focus
      "&:hover": {
        borderColor: "#999",
      },
    }),

    valueContainer: (base) => ({
      ...base,
      padding: "0 8px",
      gap: "4px",
    }),

    placeholder: (base) => ({
      ...base,
      color: "#666",
      fontSize: "14px",
    }),

    multiValue: (base) => ({
      ...base,
      borderRadius: "6px",
      backgroundColor: "#f0f0f0",
      padding: "2px 6px",
    }),

    multiValueLabel: (base) => ({
      ...base,
      color: "#333",
      fontSize: "13px",
    }),

    multiValueRemove: (base) => ({
      ...base,
      color: "#666",
      cursor: "pointer",
      ":hover": {
        backgroundColor: "#e0e0e0",
        color: "#000",
      },
    }),

    menu: (base) => ({
      ...base,
      borderRadius: "8px",
      border: "1px solid #ccc",
      marginTop: "4px",
      zIndex: 9999,
    }),

    menuList: (base) => ({
      ...base,
      padding: "4px 0",
    }),

    option: (base, state) => ({
      ...base,
      fontSize: "14px",
      padding: "8px 12px",
      cursor: "pointer",
      backgroundColor: state.isSelected
        ? "#e6f0ff" // selected background
        : state.isFocused
        ? "#f5f5f5" // hover background
        : "#fff",
      color: "#333",
      ":active": {
        backgroundColor: "#e6f0ff",
      },
    }),

    dropdownIndicator: (base) => ({
      ...base,
      color: "#666",
      padding: "0 8px",
      "&:hover": {
        color: "#333",
      },
    }),

    clearIndicator: (base) => ({
      ...base,
      padding: "0 8px",
    }),
  };

  const toggleIdentifierVisibility = () => {
    setShowIdentifier((prevState) => !prevState);
  };

  useEffect(() => {
    const fetchRequestDetails = async () => {
      try {
        // In sandbox mode, use mock data
        if (isSandboxMode()) {
          const finalBusinessId = businessId || 'sandbox-business-id';
          const finalTenantId = tenantId || 'sandbox-tenant-id';
          const finalToken = sessionToken || 'sandbox-session-token-12345';
          
          const mockResponse = await sandboxAPICall(
            `${config.grievance_details}/${reqId}`,
            'GET',
            {},
            {
              "tenant-id": finalTenantId,
              "business-id": finalBusinessId,
              "x-session-token": finalToken,
              page: 1,
              size: 10,
              "transactionid": generateTransactionId(),
            }
          );
          
          // Extract data from mock response
          const data = mockResponse.data || mockResponse;
          
          if (data) {
            // ✅ Map fields based on your response (note: field names have spaces!)
            const userInfo = [];
            if (data.userDetails?.Name) userInfo.push(data.userDetails.Name);
            if (data.userDetails?.["Mobile Number"]) userInfo.push(data.userDetails["Mobile Number"]);
            if (data.userDetails?.["Email address"]) userInfo.push(data.userDetails["Email address"]);

            console.log("userInfo: ", userInfo);

            setRequestDetails({
              requestType: data.grievanceType || "N/A",
              policyKey: data.grievanceDetail || data.grievanceType || "N/A",
              identifier: userInfo.join(", ") || "N/A",
              descriptionText: data.grievanceDescription || data.description || "N/A",
              attachment: data.supportingDocs?.[0]?.name || (data.attachments?.[0] || "No attachment available"),
              grievanceId: data.grievanceId || reqId || "N/A",
            });
            
            // Set status
            if (data.status) {
              setStatus(data.status);
            }
            
            // Save user name for update API
            if (data.userDetails?.Name) {
              setUserName(data.userDetails.Name);
            }

            // Set status history
            if (data.history && Array.isArray(data.history)) {
              const timeline = [];
              if (data.history.length > 0) {
                timeline.push({
                  status: "NEW",
                  updatedAt: data.createdAt || data.updatedAt,
                });
              }
              data.history.forEach((entry) => {
                timeline.push({
                  status: entry.newStatus || entry.status,
                  updatedAt: entry.updatedAt,
                });
              });
              setStatusHistory(timeline);
            } else if (data.status) {
              // Create basic history from status
              setStatusHistory([{
                status: data.status,
                updatedAt: data.updatedAt || data.createdAt,
              }]);
            }

            // Make consent search API call if mobile or email exists
            console.log("🔍 Checking for MOBILE or EMAIL...");
            console.log("userDetails:", data.userDetails);
            console.log("Mobile Number:", data.userDetails?.["Mobile Number"]);
            console.log("Email address:", data.userDetails?.["Email address"]);
            
            const mobile = data.userDetails?.["Mobile Number"] || data.userDetails?.MOBILE;
            const email = data.userDetails?.["Email address"] || data.userDetails?.email;
            
            if (mobile || email) {
              setHasMobileOrEmail(true);
              
              try {
                const consentUrl = `${config.consent_search}?${mobile ? `mobile=${encodeURIComponent(mobile)}` : ''}${mobile && email ? '&' : ''}${email ? `email=${encodeURIComponent(email)}` : ''}`;
                
                const consentHeaders = {
                  "accept": "*/*",
                  "tenant-id": finalTenantId,
                  "business-id": finalBusinessId,
                  "x-session-token": finalToken,
                  "txn": generateTransactionId(),
                };

                console.log("🔑 Headers:", consentHeaders);
                
                const consentResponse = await sandboxAPICall(consentUrl, 'GET', {}, consentHeaders);
                
                console.log("📡 Consent API Response:", consentResponse);
                
                if (consentResponse && consentResponse.data) {
                  const consentDataArray = Array.isArray(consentResponse.data) 
                    ? consentResponse.data 
                    : consentResponse.data.searchList || [];
                  
                  if (consentDataArray.length > 0) {
                    setConsentData(consentDataArray);
                  }
                }
              } catch (consentErr) {
                console.error("❌ Consent search failed:", consentErr);
              }
            }
          }
          return;
        }

        const headers = {
          "tenant-id": tenantId,
          "business-id": businessId,
          "x-session-token": sessionToken,
          page: 1,
          size: 10,
          "transactionid": generateTransactionId(),
        };

        const res = await fetch(
          `${config.grievance_details}/${reqId}`,
          { method: "GET", headers }
        );

        const data = await res.json();

        console.log("data: ", data);

        if (res.ok && data) {
          // ✅ Map fields based on your response (note: field names have spaces!)
          const userInfo = [];
          if (data.userDetails?.Name) userInfo.push(data.userDetails.Name);
          if (data.userDetails?.["Mobile Number"]) userInfo.push(data.userDetails["Mobile Number"]);
          if (data.userDetails?.["Email address"]) userInfo.push(data.userDetails["Email address"]);

          console.log("userInfo: ", userInfo);

          setRequestDetails({
            requestType: data.grievanceType || "N/A", // maps to grievanceType
            policyKey: data.grievanceDetail || "N/A", // maps to grievanceDetail
            identifier: userInfo.join(", ") || "N/A", // combines name, mobile, email
            descriptionText: data.grievanceDescription || "N/A", // maps to grievanceDescription
            attachment: data.supportingDocs?.[0]?.name || "No attachment available", 
            grievanceId: data.grievanceId || "N/A",
          });
          
          // Save user name for update API
          if (data.userDetails?.Name) {
            setUserName(data.userDetails.Name);
          }

          // Make consent search API call if mobile or email exists
          console.log("🔍 Checking for MOBILE or EMAIL...");
          console.log("userDetails:", data.userDetails);
          console.log("Mobile Number:", data.userDetails?.["Mobile Number"]);
          console.log("Email address:", data.userDetails?.["Email address"]);
          
          if (data.userDetails?.["Mobile Number"] || data.userDetails?.["Email address"]) {
            console.log("✅ MOBILE or EMAIL found - calling consent search API");
            setHasMobileOrEmail(true);
            const identifierValue = data.userDetails?.["Mobile Number"] || data.userDetails?.["Email address"];
            const identifierType = data.userDetails?.["Mobile Number"] ? "MOBILE" : "EMAIL";
            
            console.log(`📞 Calling consent search with ${identifierType}: ${identifierValue}`);
            
            try {
              const consentHeaders = {
                "accept": "*/*",
                "tenant-id": tenantId,
                "business-id": businessId,
                "x-session-token": sessionToken,
                "txn": generateTransactionId(),
                "customerIdentifiers.type": identifierType,
              };

              const consentUrl = `${config.consent_search}?customerIdentifiers.value=${encodeURIComponent(identifierValue)}`;
              console.log("🌐 Consent API URL:", consentUrl);
              console.log("🔑 Headers:", consentHeaders);
              
              const consentResponse = await fetch(consentUrl, {
                method: "GET",
                headers: consentHeaders,
              });

              console.log("📡 Consent API Response Status:", consentResponse.status);

              if (consentResponse.ok) {
                const consentResponseData = await consentResponse.json();
                console.log("✅ Consent search response:", consentResponseData);
                
                // Extract the searchList array from the response
                const consentList = consentResponseData?.searchList || [];
                console.log("📋 Extracted consent list:", consentList);
                console.log("📊 Number of consents found:", consentList.length);
                
                setConsentData(consentList);
              } else {
                console.error("❌ Consent search API failed:", consentResponse.status);
                const errorText = await consentResponse.text();
                console.error("Error details:", errorText);
                setConsentData(null);
              }
            } catch (error) {
              console.error("❌ Error calling consent search API:", error);
              setConsentData(null);
            }
          } else {
            console.log("❌ No MOBILE or EMAIL found - skipping consent search");
            setHasMobileOrEmail(false);
            setConsentData(null);
          }
          
          setStatus(data.status || "NEW");
          
          // Build status history timeline
          if (data.history && Array.isArray(data.history)) {
            const timeline = [];
            // Add initial status (NEW) if history exists
            if (data.history.length > 0) {
              timeline.push({
                status: "NEW",
                updatedAt: data.createdAt || data.updatedAt,
              });
            }
            // Add history entries
            data.history.forEach((entry) => {
              timeline.push({
                status: entry.newStatus,
                updatedAt: entry.updatedAt,
              });
            });
            setStatusHistory(timeline);
          } else {
            // If no history, just show current status
            setStatusHistory([{
              status: data.status || "NEW",
              updatedAt: data.createdAt || data.updatedAt,
            }]);
          }
        } else {
          console.error("Failed to fetch grievance details:", data);
        }
      } catch (error) {
        console.error("Error fetching grievance details:", error);
      }
    };

    if (reqId) fetchRequestDetails();
  }, [reqId]);

  // Options for status dropdown
  const statusOptions = [
    { value: "NEW", label: "New" },
    { value: "INPROCESS", label: "In Process" },
    { value: "RESOLVED", label: "Resolved" },
  ];

  const availableStatusOptions = React.useMemo(() => {
    switch (status) {
      case 'NEW':
        return statusOptions.filter(opt => ['NEW', 'INPROCESS'].includes(opt.value));
      case 'INPROCESS':
        return statusOptions.filter(opt => ['INPROCESS', 'RESOLVED'].includes(opt.value));
      case 'RESOLVED':
        return statusOptions.filter(opt => ['RESOLVED'].includes(opt.value));
      default:
        // For any other status, just show the current status as the only option
        return statusOptions.filter(opt => opt.value === status);
    }
  }, [status]);

  // Helper function to get status color
  const getStatusColor = (status) => {
    const colorMap = {
      "NEW": { bg: "rgb(237, 255, 237)", text: "rgb(0, 128, 0)" },
      "INPROCESS": { bg: "rgb(220, 240, 255)", text: "rgb(0, 100, 200)" },
      "L1_ESCALATED": { bg: "rgb(255, 245, 220)", text: "rgb(200, 100, 0)" },
      "L2_ESCALATED": { bg: "rgb(255, 245, 220)", text: "rgb(200, 100, 0)" },
      "RESOLVED": { bg: "rgb(237, 255, 237)", text: "rgb(0, 128, 0)" },
    };
    return colorMap[status] || { bg: "#f5f5f5", text: "#595959" };
  };

  // Helper function to get human-readable status name
  const getStatusDisplayName = (status) => {
    const nameMap = {
      "NEW": "New",
      "INPROCESS": "In Process",
      "L1_ESCALATED": "L1 Escalated",
      "L2_ESCALATED": "L2 Escalated",
      "RESOLVED": "Resolved",
    };
    return nameMap[status] || status;
  };

  return (
    <div className="page-container">
      <div style={{ marginTop:'4%', flex:'1'}}>
        <div className="header-container">
          <div className="header-left" style={{marginTop:'2%', paddingLeft:'2%'}}>
            <Button
              ariaControls="Button Clickable"
              ariaDescribedby="Button"
              ariaExpanded="Expanded"
              ariaLabel="Button"
              className="Button"
              icon="ic_back"
              iconAriaLabel="Icon Favorite"
              iconLeft="ic_back"
              kind="secondary"
              onClick={handleBack}
              size="small"
              state="normal"
            />
          </div>
          <div className="header-right" style={{marginTop:'2%'}}>
            <Text appearance="heading-xs" color="primary-grey-100">
              {requestDetails?.grievanceId || "Loading..."}
            </Text>

            <div
              className="request-status-container"
              style={{ marginTop: "36px" }}
            >
              {/* Request Status Section */}
              <div
                className="status-dropdown-header"
                style={{ marginBottom: "16px" }}
              >
                <Text appearance="body-m-bold" color="primary-grey-80">
                  Request status
                </Text>
              </div>

              <div style={{display: 'flex', flexDirection: 'row', gap: '16px'}}>

               {/* Select Status Section */}
               <div className="language-select" style={{ padding: "0px", width: "35%",
  marginRight: "1%",
  borderRight: "1px solid lightgrey",
  paddingRight: "2%" }}>
                <Text appearance="body-xs" color="primary-grey-80" style={{ marginBottom: "8px" }}>
                  Select Status
                </Text>
                <Select
                  value={statusOptions.find((opt) => opt.value === status)}
                  options={availableStatusOptions}
                  onChange={handleStatusChange}
                  styles={customStyles}
                  isDisabled={status === 'RESOLVED'}
                />
              </div>
              
              {/* Status History Timeline */}
              {statusHistory.length > 0 && (
                <div style={{ display: "flex", alignItems: "center", gap: "12px", marginBottom: "24px", flexWrap: "wrap" }}>
                    {statusHistory.map((item, index) => {
                      const statusColor = getStatusColor(item.status);
                      const displayName = getStatusDisplayName(item.status);
                      return (
                        <React.Fragment key={index}>
                          <div style={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
                            <div
                              style={{
                                backgroundColor: statusColor.bg,
                                color: statusColor.text,
                                padding: "6px 16px",
                                borderRadius: "8px",
                                fontSize: "14px",
                                fontWeight: 400,
                                textAlign: "center",
                                minWidth: "120px",
                                border: "none",
                              }}
                            >
                              {displayName}
                            </div>
                            <Text
                              appearance="body-xs"
                              color="primary-grey-80"
                              style={{ marginTop: "8px", textAlign: "center", fontSize: "12px", fontWeight: 400 }}
                            >
                              {formatToISTUSFormat(item.updatedAt)}
                            </Text>
                          </div>
                          {index < statusHistory.length - 1 && (
                            <div style={{ fontSize: "16px", color: "#000", fontWeight: 300 }}>→</div>
                          )}
                        </React.Fragment>
                      );
                    })}
                  </div>
                )}
              
             

            </div>


            </div>

            <div
              className="request-details-container"
              style={{ marginTop: "24px" }}
            >
              <div
                className="request-details-header"
                style={{ marginBottom: "8px" }}
              >
                <Text appearance="body-m-bold" color="primary-grey-80">
                  Request details
                </Text>
              </div>
             {/* <div className="request-details-content">
                <div className="detail-item">
                  <Text appearance="body-s" color="primary-grey-80">
                    Request type:
                  </Text>
                  <Text appearance="body-s" color="primary-grey-100">
                    {` ${requestDetails.requestType}`}
                  </Text>
                </div>
                <div className="detail-item">
                  <Text appearance="body-s" color="primary-grey-80">
                    Policy key:
                  </Text>
                  <Text appearance="body-s" color="primary-grey-100">
                    {` ${requestDetails.policyKey}`}
                  </Text>
                </div>
                {showIdentifier && (
                  <div className="detail-item">
                    <Text appearance="body-s" color="primary-grey-80">
                      Identifier:
                    </Text>
                    <Text appearance="body-s" color="primary-grey-100">
                      {` ${requestDetails.identifier}`}
                    </Text>
                  </div>
                )}
                <div className="detail-item">
                  <Text appearance="body-s" color="primary-grey-80">
                    Description text:
                  </Text>
                  <Text appearance="body-s" color="primary-grey-100">
                    {` ${requestDetails.descriptionText}`}
                  </Text>
                </div>
                <div className="detail-item">
                  <Text appearance="body-s" color="primary-grey-80">
                    Attachment:
                  </Text>
                  <Text appearance="body-s" color="primary-grey-100">
                    {` ${requestDetails.attachment}`}
                  </Text>
                </div>
              </div>  */}

              <div className="request-details-content">
              {requestDetails ? (
                  <>
                    <div className="detail-item">
                      <Text appearance="body-s" color="primary-grey-80">
                        Request type:{" "}
                      </Text>
                      <Text appearance="body-s" color="primary-grey-100">
                        {requestDetails.requestType}
                      </Text>
                    </div>

                    <div className="detail-item">
                      <Text appearance="body-s" color="primary-grey-80">
                        Request details:{" "}
                      </Text>
                      <Text appearance="body-s" color="primary-grey-100">
                        {requestDetails.policyKey}
                      </Text>
                    </div>

                    {showIdentifier && (
                      <div className="detail-item">
                        <Text appearance="body-s" color="primary-grey-80">
                          Identifier:{" "}
                        </Text>
                        <Text appearance="body-s" color="primary-grey-100">
                          {requestDetails.identifier}
                        </Text>
                      </div>
                    )}

                    <div className="detail-item">
                      <Text appearance="body-s" color="primary-grey-80">
                        Description text:{" "}
                      </Text>
                      <Text appearance="body-s" color="primary-grey-100">
                        {requestDetails.descriptionText}
                      </Text>
                    </div>

                    <div className="detail-item">
                      <Text appearance="body-s" color="primary-grey-80">
                        Attachment:{" "}
                      </Text>
                      <Text appearance="body-s" color="primary-grey-100">
                        {requestDetails.attachment}
                      </Text>
                    </div>
                  </>
                ) : (
                  <Text appearance="body-s" color="primary-grey-80">
                    Loading details...
                  </Text>
                )}

              </div>
            </div>

            <div className="consent-details-table" style={{ marginTop: "24px" }}>
              <div
                className="consent-details-table-header"
                style={{ marginBottom: "8px" }}
              >
                <Text appearance="body-m-bold" color="primary-grey-80">
                  Consent details
                </Text>
              </div>
              <div className="consent-details-table">
                {!hasMobileOrEmail ? (
                  <div style={{ padding: "20px", textAlign: "center" }}>
                    <Text appearance="body-s" color="primary-grey-80">
                      Consents are not populated
                    </Text>
                  </div>
                ) : consentData && Array.isArray(consentData) && consentData.length > 0 ? (
                  <ConsentTable consentData={consentData} />
                ) : (
                  <div style={{ padding: "20px", textAlign: "center" }}>
                    <Text appearance="body-s" color="primary-grey-80">
                      Consents are not populated
                    </Text>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EditRequest;
