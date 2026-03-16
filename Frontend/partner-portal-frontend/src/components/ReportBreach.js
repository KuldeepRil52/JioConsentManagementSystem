import React, { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { ActionButton, Button } from '../custom-components';
import { useSelector, useDispatch } from "react-redux";
import Select from "react-select";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import { getDataTypes } from "../store/actions/CommonAction";
import { generateTransactionId } from "../utils/transactionId";
import config from "../utils/config";
import "../styles/pageConfiguration.css";
import "../styles/reportBreach.css";
import "react-toastify/dist/ReactToastify.css";

const ReportBreach = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();
  const tenantId = useSelector((state) => state.common.tenant_id);
  const sessionToken = useSelector((state) => state.common.session_token);
  const businessId = useSelector((state) => state.common.business_id);

  // Get mode and data from navigation state
  const viewMode = location.state?.viewMode || false;
  const editMode = location.state?.editMode || false;
  const breachData = location.state?.breachData || null;

  const [formData, setFormData] = useState({
    // Incident Details
    dateTimeOfDiscovery: "",
    timeOfDiscovery: "",
    dateTimeOfOccurrence: "",
    timeOfOccurrence: "",
    typeOfBreach: null,
    briefDescription: "",
    systemOrServiceAffected: "",
    
    // Data Involved
    categoriesOfPersonalData: [], // Changed to array for multi-select
    numberOfDataPrincipalsAffected: "",
    wasDataEncrypted: null,
    potentialImpactDescription: "",
    
    // Notification Details
    dateNotifiedToDPBI: "",
    dpbiAcknowledgementId: "",
    dateNotifiedToDataPrincipals: "",
    notificationChannel: null,
    notificationStatus: null,
  });

  const [saving, setSaving] = useState(false);
  const [dataItemOptions, setDataItemOptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [incidentStatus, setIncidentStatus] = useState({ value: "NEW", label: "New" }); // Default to NEW

  // Status options for create and edit mode
  const statusOptions = [
    { value: "NEW", label: "New" },
    { value: "Under_Investigation", label: "Under Investigation" },
    { value: "Contained", label: "Contained" },
    { value: "Resolved", label: "Resolved" },
    { value: "Monitoring", label: "Monitoring" },
    { value: "Closed", label: "Closed" },
  ];

  // Fetch data types and data items from master data
  useEffect(() => {
    const fetchDataTypes = async () => {
      try {
        setLoading(true);
        const res = await dispatch(getDataTypes());
        
        console.log("Data Types Response:", res);
        console.log("Full response data:", JSON.stringify(res?.data, null, 2));
        
        if (res?.status === 200 || res?.status === 201) {
          if (res?.data?.searchList) {
            console.log("Search List:", res.data.searchList);
            // Flatten all data items from all data types
            const allDataItems = [];
            res.data.searchList.forEach((dataType) => {
              console.log("Data Type:", dataType);
              console.log("Data Items in this type:", dataType.dataItems);
              if (dataType.dataItems && Array.isArray(dataType.dataItems)) {
                dataType.dataItems.forEach((item) => {
                  console.log("Processing item:", item);
                  allDataItems.push({
                    value: item.dataItemId || item.id || item.dataItemName,
                    label: item.dataItemName || item.name || item.dataItemId,
                    dataTypeId: dataType.dataTypeId,
                    dataTypeName: dataType.dataTypeName,
                  });
                });
              }
            });
            console.log("All Data Items flattened:", allDataItems);
            setDataItemOptions(allDataItems);
          }
        } else {
          console.log("API response status not 200/201:", res?.status);
        }
      } catch (err) {
        console.error("Error fetching data types:", err);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message="Error loading data categories"
            />
          ),
          { icon: false }
        );
      } finally {
        setLoading(false);
      }
    };

    if (tenantId && sessionToken) {
      console.log("Fetching data types with tenantId:", tenantId, "sessionToken:", sessionToken ? "present" : "missing");
      fetchDataTypes();
    } else {
      console.log("Not fetching - tenantId or sessionToken missing:", { tenantId, sessionToken: sessionToken ? "present" : "missing" });
    }
  }, [dispatch, tenantId, sessionToken]);

  // Prefill form data when in view or edit mode
  useEffect(() => {
    if ((viewMode || editMode) && breachData) {
      // Convert data from API format to form format
      const categoriesArray = breachData.impactAssessment?.categoriesOfDataAffected
        ? breachData.impactAssessment.categoriesOfDataAffected.split(',').map(cat => ({
            value: cat.trim(),
            label: cat.trim()
          }))
        : [];

      setFormData({
        dateTimeOfDiscovery: breachData.incidentDetails?.dateDetected || "",
        timeOfDiscovery: breachData.incidentDetails?.timeDetected || "",
        dateTimeOfOccurrence: breachData.incidentDetails?.dateOfOccurrence || "",
        timeOfOccurrence: breachData.incidentDetails?.timeOfOccurrence || "",
        typeOfBreach: breachData.incidentDetails?.breachType 
          ? { value: breachData.incidentDetails.breachType, label: breachData.incidentDetails.breachType.replace(/_/g, " ") }
          : null,
        briefDescription: breachData.incidentDetails?.incidentDescription || "",
        systemOrServiceAffected: breachData.incidentDetails?.systemAffected || "",
        categoriesOfPersonalData: categoriesArray,
        numberOfDataPrincipalsAffected: breachData.impactAssessment?.numberOfAffectedRecords?.toString() || "",
        wasDataEncrypted: breachData.impactAssessment?.dataEncrypted !== undefined
          ? { value: breachData.impactAssessment.dataEncrypted, label: breachData.impactAssessment.dataEncrypted ? "Yes" : "No" }
          : null,
        potentialImpactDescription: breachData.impactAssessment?.estimatedImpact || "",
        dateNotifiedToDPBI: breachData.notificationCompliance?.dpbiNotificationDate || "",
        dpbiAcknowledgementId: breachData.notificationCompliance?.partiesReportedTo || "",
        dateNotifiedToDataPrincipals: breachData.notificationCompliance?.dataPrincipalsNotificationDate || "",
        notificationChannel: breachData.notificationCompliance?.notificationChannel
          ? { value: breachData.notificationCompliance.notificationChannel, label: breachData.notificationCompliance.notificationChannel }
          : null,
        notificationStatus: breachData.notificationCompliance?.notificationSent
          ? { value: "Sent", label: "Sent" }
          : { value: "Pending", label: "Pending" },
      });

      // Set current status for edit mode
      if (editMode && breachData.responseActions?.incidentStatus) {
        const statusValue = breachData.responseActions.incidentStatus;
        const statusLabel = statusValue.replace(/_/g, " ");
        setIncidentStatus({ value: statusValue, label: statusLabel });
      }
    }
  }, [viewMode, editMode, breachData]);

  // Dropdown options
  const breachTypeOptions = [
    { value: "Unauthorized_Access", label: "Unauthorized Access" },
    { value: "Data_Leak", label: "Data Leak" },
    { value: "Data_Loss", label: "Data Loss" },
    { value: "Misuse", label: "Misuse" },
  ];

  const booleanOptions = [
    { value: true, label: "Yes" },
    { value: false, label: "No" },
  ];

  const notificationChannelOptions = [
    { value: "Email", label: "Email" },
    { value: "SMS", label: "SMS" },
    { value: "App", label: "App" },
    { value: "Portal", label: "Portal" },
  ];

  const notificationStatusOptions = [
    { value: "Pending", label: "Pending" },
    { value: "Sent", label: "Sent" },
    { value: "Completed", label: "Completed" },
  ];

  const handleBack = () => {
    navigate("/breach-notifications");
  };

  const handleInputChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleSubmit = async () => {
    // In edit mode, only update status
    if (editMode) {
      if (!incidentStatus) {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message="Please select a status"
            />
          ),
          { icon: false }
        );
        return;
      }

      try {
        setSaving(true);
        const txnId = generateTransactionId();

        // Update only the status
        const requestBody = {
          ...breachData,
          responseActions: {
            ...breachData.responseActions,
            incidentStatus: incidentStatus.value,
          },
        };

        const response = await fetch(
          `${config.data_breach_base}/${breachData.id}`,
          {
            method: "PUT",
            headers: {
              "Content-Type": "application/json",
              accept: "*/*",
              txn: txnId,
              "tenant-id": tenantId,
              "business-id": businessId,
              "x-session-token": sessionToken,
            },
            body: JSON.stringify(requestBody),
          }
        );

        if (!response.ok) {
          throw new Error("Failed to update breach status");
        }

        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message="Breach status updated successfully!"
            />
          ),
          { icon: false }
        );

        setTimeout(() => {
          navigate("/breach-notifications");
        }, 1500);
      } catch (error) {
        console.error("Error updating breach status:", error);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={error.message || "Failed to update breach status"}
            />
          ),
          { icon: false }
        );
      } finally {
        setSaving(false);
      }
      return;
    }

    // Validation for create mode
    if (!formData.dateTimeOfDiscovery) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="Please select date & time of discovery"
          />
        ),
        { icon: false }
      );
      return;
    }

    if (!formData.typeOfBreach) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="Please select type of breach"
          />
        ),
        { icon: false }
      );
      return;
    }

    if (!formData.briefDescription.trim()) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="Please enter a brief description"
          />
        ),
        { icon: false }
      );
      return;
    }

    try {
      setSaving(true);

      // Generate transaction ID in UUID format
      const txnId = generateTransactionId();

      // Get selected categories as comma-separated string
      const selectedCategories = formData.categoriesOfPersonalData
        .map(item => item.label)
        .join(', ');

      // Construct request body matching the API structure
      const requestBody = {
        tenantId: tenantId,
        incidentDetails: {
          breachType: formData.typeOfBreach?.value || "",
          dateDetected: formData.dateTimeOfDiscovery,
          timeDetected: formData.timeOfDiscovery || "00:00:00",
          dateOfOccurrence: formData.dateTimeOfOccurrence || "",
          timeOfOccurrence: formData.timeOfOccurrence || "",
          severityLevel: "Critical", // Default value
          incidentOwner: "System Generated",
          incidentDescription: formData.briefDescription,
          systemAffected: formData.systemOrServiceAffected || "",
        },
        impactAssessment: {
          numberOfAffectedRecords: parseInt(formData.numberOfDataPrincipalsAffected) || 0,
          categoriesOfDataAffected: selectedCategories || "",
          estimatedImpact: formData.potentialImpactDescription || "",
          dataEncrypted: formData.wasDataEncrypted?.value ?? false,
        },
        responseActions: {
          incidentStatus: incidentStatus?.value || "NEW",
          immediateActionsTaken: "",
          rootCauseAnalysis: "",
          remediationPlan: "",
        },
        notificationCompliance: {
          affectedIndividualsContacted: formData.dateNotifiedToDataPrincipals ? true : false,
          regulatorNotified: formData.dateNotifiedToDPBI ? true : false,
          partiesReportedTo: formData.dpbiAcknowledgementId || "",
          notificationSent: formData.notificationStatus?.value === "Sent" || formData.notificationStatus?.value === "Completed",
          dpbiNotificationDate: formData.dateNotifiedToDPBI || "",
          dataPrincipalsNotificationDate: formData.dateNotifiedToDataPrincipals || "",
          notificationChannel: formData.notificationChannel?.value || "",
        },
      };

      console.log("Sending breach report to API:", JSON.stringify(requestBody, null, 2));

      const response = await fetch(
        config.data_breach_base,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            accept: "*/*",
            txn: txnId,
            "tenant-id": tenantId,
            "business-id": businessId,
            "x-session-token": sessionToken,
          },
          body: JSON.stringify(requestBody),
        }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || "Failed to report breach");
      }

      toast.success(
        (props) => (
          <CustomToast
            {...props}
            type="success"
            message="Breach reported successfully!"
          />
        ),
        { icon: false }
      );

      // Navigate back after a short delay
      setTimeout(() => {
        navigate("/breach-notifications");
      }, 1500);
    } catch (error) {
      console.error("Error reporting breach:", error);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={error.message || "Failed to report breach"}
          />
        ),
        { icon: false }
      );
    } finally {
      setSaving(false);
    }
  };

  const customSelectStyles = {
    control: (base) => ({
      ...base,
      minHeight: "40px",
      fontSize: "14px",
    }),
    menu: (base) => ({
      ...base,
      fontSize: "14px",
    }),
  };

  return (
    <>
      <div className="configurePage">
        <div className="report-breach-container">
          <div className="report-breach-header">
            <Button
              kind="tertiary"
              size="medium"
              state="normal"
              icon="ic_back"
              iconLeft="ic_back"
              onClick={handleBack}
            />
            <h1 style={{ 
              fontSize: '32px', 
              fontWeight: '700', 
              margin: 0, 
              color: '#1a1a1a',
              lineHeight: '40px'
            }}>
              {viewMode ? "View Breach Notification" : editMode ? "Edit Breach Status" : "Report Breach Notification"}
            </h1>
          </div>

          <div className="report-breach-content">
            {/* 1️⃣ Incident Details Section */}
            <div className="form-section">
              <h2 className="section-heading">1️⃣ Incident Details</h2>
              
              {/* Status field - Fixed to NEW during creation, editable only when editing */}
              <div className="form-field">
                <label>Incident Status <span className="required">*</span></label>
                <Select
                  value={incidentStatus}
                  onChange={(value) => setIncidentStatus(value)}
                  options={statusOptions}
                  styles={customSelectStyles}
                  placeholder="Select incident status"
                  isDisabled={viewMode || !editMode}
                />
                {!editMode && !viewMode && (
                  <small style={{ display: "block", marginTop: "4px", color: "#6b7280", fontSize: "12px" }}>
                    Status is set to "New" by default. You can update it later when editing the breach.
                  </small>
                )}
              </div>

              <div className="form-field">
                <div style={{ display: "flex", gap: "16px" }}>
                  <div style={{ flex: 1 }}>
                    <label>Date of Discovery <span className="required">*</span></label>
                    <input
                      type="date"
                      value={formData.dateTimeOfDiscovery}
                      onChange={(e) =>
                        handleInputChange("dateTimeOfDiscovery", e.target.value)
                      }
                      disabled={viewMode || editMode}
                    />
                  </div>
                  <div style={{ flex: 1 }}>
                    <label>Time of Discovery <span className="required">*</span></label>
                    <input
                      type="time"
                      value={formData.timeOfDiscovery}
                      onChange={(e) =>
                        handleInputChange("timeOfDiscovery", e.target.value)
                      }
                      disabled={viewMode || editMode}
                    />
                  </div>
                </div>
              </div>

              <div className="form-field">
                <div style={{ display: "flex", gap: "16px" }}>
                  <div style={{ flex: 1 }}>
                    <label>Date of Occurrence (if known)</label>
                    <input
                      type="date"
                      value={formData.dateTimeOfOccurrence}
                      onChange={(e) =>
                        handleInputChange("dateTimeOfOccurrence", e.target.value)
                      }
                      disabled={viewMode || editMode}
                    />
                  </div>
                  <div style={{ flex: 1 }}>
                    <label>Time of Occurrence</label>
                    <input
                      type="time"
                      value={formData.timeOfOccurrence}
                      onChange={(e) =>
                        handleInputChange("timeOfOccurrence", e.target.value)
                      }
                      disabled={viewMode || editMode}
                    />
                  </div>
                </div>
              </div>

              <div className="form-field">
                <label>Type of Breach <span className="required">*</span></label>
                <Select
                  value={formData.typeOfBreach}
                  onChange={(value) => handleInputChange("typeOfBreach", value)}
                  options={breachTypeOptions}
                  styles={customSelectStyles}
                  placeholder="Select type of breach"
                  isDisabled={viewMode || editMode}
                />
              </div>

              <div className="form-field">
                <label>Brief Description of Incident <span className="required">*</span></label>
                <textarea
                  value={formData.briefDescription}
                  onChange={(e) =>
                    handleInputChange("briefDescription", e.target.value)
                  }
                  placeholder="E.g., Email with user data sent to unintended recipient"
                  rows="3"
                  disabled={viewMode || editMode}
                />
              </div>

              <div className="form-field">
                <label>System or Service Affected</label>
                <input
                  type="text"
                  value={formData.systemOrServiceAffected}
                  onChange={(e) =>
                    handleInputChange("systemOrServiceAffected", e.target.value)
                  }
                  placeholder="E.g., Consent API, Dashboard, Database"
                  disabled={viewMode || editMode}
                />
              </div>
            </div>

            {/* 2️⃣ Data Involved Section */}
            <div className="form-section">
              <h2 className="section-heading">2️⃣ Data Involved</h2>

              <div className="form-field">
                <label>Categories of Personal Data (Multi-Select)</label>
                <Select
                  isMulti
                  value={formData.categoriesOfPersonalData}
                  onChange={(value) => handleInputChange("categoriesOfPersonalData", value)}
                  options={dataItemOptions}
                  styles={customSelectStyles}
                  placeholder="Select data categories..."
                  isLoading={loading}
                  isDisabled={loading || viewMode || editMode}
                  closeMenuOnSelect={false}
                />
              </div>

              <div className="form-field">
                <label>Approx. Number of Data Principals Affected</label>
                <input
                  type="number"
                  value={formData.numberOfDataPrincipalsAffected}
                  onChange={(e) =>
                    handleInputChange("numberOfDataPrincipalsAffected", e.target.value)
                  }
                  placeholder="E.g., 500"
                  disabled={viewMode || editMode}
                />
              </div>

              <div className="form-field">
                <label>Was the Data Encrypted or Protected?</label>
                <Select
                  value={formData.wasDataEncrypted}
                  onChange={(value) => handleInputChange("wasDataEncrypted", value)}
                  options={booleanOptions}
                  styles={customSelectStyles}
                  placeholder="Select"
                  isDisabled={viewMode || editMode}
                />
              </div>

              <div className="form-field">
                <label>Potential Impact / Risk Description</label>
                <textarea
                  value={formData.potentialImpactDescription}
                  onChange={(e) =>
                    handleInputChange("potentialImpactDescription", e.target.value)
                  }
                  placeholder="E.g., Minor risk of unauthorized disclosure"
                  rows="3"
                  disabled={viewMode || editMode}
                />
              </div>
            </div>

            {/* 3️⃣ Notification Details Section - Always Disabled */}
            <div className="form-section">
              <h2 className="section-heading">3️⃣ Notification Details</h2>
              <p style={{ color: "#6b7280", fontSize: "13px", marginBottom: "16px" }}>
                This section is auto-filled after notification APIs are called
              </p>

              <div className="form-field">
                <label>Date Notified to Data Protection Board of India (DPBI)</label>
                <input
                  type="date"
                  value={formData.dateNotifiedToDPBI}
                  disabled
                  style={{ backgroundColor: "#f5f5f5", cursor: "not-allowed" }}
                />
                <small style={{ display: "block", marginTop: "4px", color: "#6b7280", fontSize: "12px" }}>
                  Within 72 hours of discovery
                </small>
              </div>

              <div className="form-field">
                <label>DPBI Acknowledgement / Reference ID</label>
                <input
                  type="text"
                  value={formData.dpbiAcknowledgementId}
                  disabled
                  placeholder="To record confirmation"
                  style={{ backgroundColor: "#f5f5f5", cursor: "not-allowed" }}
                />
              </div>

              <div className="form-field">
                <label>Date Notified to Data Principals</label>
                <input
                  type="date"
                  value={formData.dateNotifiedToDataPrincipals}
                  disabled
                  style={{ backgroundColor: "#f5f5f5", cursor: "not-allowed" }}
                />
                <small style={{ display: "block", marginTop: "4px", color: "#6b7280", fontSize: "12px" }}>
                  "Without delay" after discovery
                </small>
              </div>

              <div className="form-field">
                <label>Notification Channel</label>
                <Select
                  value={formData.notificationChannel}
                  options={notificationChannelOptions}
                  styles={customSelectStyles}
                  placeholder="Select notification channel"
                  isDisabled={true}
                />
              </div>

              <div className="form-field">
                <label>Notification Status</label>
                <Select
                  value={formData.notificationStatus}
                  options={notificationStatusOptions}
                  styles={customSelectStyles}
                  placeholder="Select notification status"
                  isDisabled={true}
                />
              </div>
            </div>

            {/* Submit Button - Hidden in view mode */}
            {!viewMode && (
              <div className="form-actions">
                <ActionButton
                  label={saving ? "Saving..." : editMode ? "Update Status" : "Report Breach"}
                  kind="primary"
                  size="large"
                  onClick={handleSubmit}
                  disabled={saving}
                />
              </div>
            )}
          </div>
        </div>
      </div>

      <ToastContainer
        position="bottom-left"
        autoClose={3000}
        hideProgressBar
        closeOnClick
        pauseOnHover
        draggable
        closeButton={false}
        toastClassName={() => "toast-wrapper"}
        transition={Slide}
      />
    </>
  );
};

export default ReportBreach;

