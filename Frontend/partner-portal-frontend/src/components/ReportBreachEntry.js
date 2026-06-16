import React, { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useSelector, useDispatch } from "react-redux";
import { Text, Button, ActionButton } from '../custom-components';
import Select from "react-select";
import { getDataTypes } from "../store/actions/CommonAction";
import { generateTransactionId } from "../utils/transactionId";
import config from "../utils/config";
import { getCurrentISTForInput } from "../utils/dateUtils";
import "../styles/pageConfiguration.css";
import "../styles/addROPAEntry.css";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";

const ReportBreachEntry = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();
  const tenantId = useSelector((state) => state.common.tenant_id);
  const sessionToken = useSelector((state) => state.common.session_token);
  const businessId = useSelector((state) => state.common.business_id);

  // Check for view mode and breach data from navigation state
  const viewMode = location.state?.viewMode || false;
  const breachData = location.state?.breachData || null;

  // Form state based on JSON structure
  const [formData, setFormData] = useState({
    // Incident Details
    discoveryDateTime: "",
    occurrenceDateTime: "",
    breachType: null,
    status: { value: "NEW", label: "New" }, // Default to NEW for new breach reports
    briefDescription: "",
    affectedSystemOrService: "",
    
    // Data Involved
    dataCategories: [],
    affectedDataPrincipalsCount: "",
    dataEncryptedIfProtected: null,
    potentialImpactDescription: "",
    
    // Notification Details
    dpbiNotificationDate: "",
    dpbiAcknowledgementId: "",
    datePrincipalNotificationDate: "",
    notificationChannel: null,
  });

  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(false);
  const [dataCategoryOptions, setDataCategoryOptions] = useState([]);

  // Dropdown options (must match backend enum exactly)
  const breachTypeOptions = [
    { value: "UNAUTHORIZED_ACCESS", label: "Unauthorized Access" },
    { value: "DATA_LEAK", label: "Data Leak" },
    { value: "DATA_LOSS", label: "Data Loss" },
    { value: "MISUSE", label: "Misuse" },
  ];

  const statusOptions = [
    { value: "NEW", label: "New" },
    { value: "INVESTIGATION", label: "Investigation" },
    { value: "NOTIFIED_TO_DATA_PRINCIPALS", label: "Notified to Data Principals" },
    { value: "NOTIFIED_TO_DPBI", label: "Notified to DPBI" },
    { value: "RESOLVED", label: "Resolved" },
  ];

  const encryptionOptions = [
    { value: true, label: "Yes" },
    { value: false, label: "No" },
  ];

  const notificationChannelOptions = [
    { value: "EMAIL", label: "Email" },
    { value: "SMS", label: "SMS" },
  ];

  // Fetch data types and data items from master data on mount
  useEffect(() => {
    const fetchDataCategories = async () => {
      try {
        setLoading(true);
        const res = await dispatch(getDataTypes());
        
        if (res?.status === 200 || res?.status === 201) {
          if (res?.data?.searchList) {
            // Flatten all data items from all data types
            const allDataItems = [];
            res.data.searchList.forEach((dataType) => {
              if (dataType.dataItems && Array.isArray(dataType.dataItems)) {
                dataType.dataItems.forEach((item) => {
                  // dataItems are strings, not objects
                  allDataItems.push({
                    value: item,
                    label: `${item} (${dataType.dataTypeName})`,
                    dataTypeId: dataType.dataTypeId,
                    dataTypeName: dataType.dataTypeName,
                  });
                });
              }
            });
            setDataCategoryOptions(allDataItems);
          }
        }
      } catch (err) {
        console.error("Error fetching data categories:", err);
      } finally {
        setLoading(false);
      }
    };

    if (tenantId && sessionToken) {
      fetchDataCategories();
    }
  }, [dispatch, tenantId, sessionToken]);

  // Prefill form when in view mode
  useEffect(() => {
    if (viewMode && breachData) {
      console.log("Prefilling form with breach data:", breachData);
      
      // Helper to convert ISO string to datetime-local format (YYYY-MM-DDTHH:mm) in IST
      const formatDateTimeForInput = (isoString) => {
        if (!isoString) return "";
        try {
          const date = new Date(isoString);
          if (isNaN(date.getTime())) return ""; // Invalid date
          
          // Convert to IST
          const istDate = new Date(date.toLocaleString('en-US', { timeZone: 'Asia/Kolkata' }));
          
          const year = istDate.getFullYear();
          const month = String(istDate.getMonth() + 1).padStart(2, '0');
          const day = String(istDate.getDate()).padStart(2, '0');
          const hours = String(istDate.getHours()).padStart(2, '0');
          const minutes = String(istDate.getMinutes()).padStart(2, '0');
          return `${year}-${month}-${day}T${hours}:${minutes}`;
        } catch (e) {
          console.error("Error formatting date:", e);
          return "";
        }
      };

      // Format date only (for date inputs) in IST
      const formatDateForInput = (isoString) => {
        if (!isoString) return "";
        try {
          const date = new Date(isoString);
          if (isNaN(date.getTime())) return ""; // Invalid date
          
          // Convert to IST
          const istDate = new Date(date.toLocaleString('en-US', { timeZone: 'Asia/Kolkata' }));
          
          const year = istDate.getFullYear();
          const month = String(istDate.getMonth() + 1).padStart(2, '0');
          const day = String(istDate.getDate()).padStart(2, '0');
          return `${year}-${month}-${day}`;
        } catch (e) {
          console.error("Error formatting date:", e);
          return "";
        }
      };

      // Find matching options for select fields
      const findOption = (options, value) => {
        return options.find(opt => opt.value === value) || null;
      };

      // Convert data categories array to select options
      const dataCategoriesOptions = breachData.dataInvolved?.personalDataCategories?.map(cat => ({
        value: cat,
        label: cat,
      })) || [];

      // Debug logging for notification details
      console.log("Full breach data:", breachData);
      console.log("Notification details:", breachData.notificationDetails);
      console.log("DPBI Notification Date:", breachData.notificationDetails?.dpbiNotificationDate);
      console.log("Data Principal Notification Date:", breachData.notificationDetails?.dataPrincipalNotificationDate);

      // Check multiple possible field locations for notification dates
      const dpbiDate = breachData.notificationDetails?.dpbiNotificationDate 
        || breachData.dpbiNotificationDate 
        || breachData.notificationDetails?.dpbiNotifiedDate
        || breachData.notificationDetails?.dpbiDate;
      
      const dataPrincipalDate = breachData.notificationDetails?.dataPrincipalNotificationDate 
        || breachData.dataPrincipalNotificationDate 
        || breachData.notificationDetails?.datePrincipalNotificationDate
        || breachData.datePrincipalNotificationDate
        || breachData.notificationDetails?.dataPrincipalDate;

      console.log("Extracted DPBI Date:", dpbiDate);
      console.log("Extracted Data Principal Date:", dataPrincipalDate);
      
      // Debug warning for notification details structure
      if (!dpbiDate && !dataPrincipalDate && breachData.notificationDetails) {
        console.warn("Notification dates not found! Available notification fields:", 
          Object.keys(breachData.notificationDetails));
      }

      setFormData({
        // Incident Details
        discoveryDateTime: formatDateTimeForInput(breachData.incidentDetails?.discoveryDateTime),
        occurrenceDateTime: formatDateTimeForInput(breachData.incidentDetails?.occurrenceDateTime),
        breachType: findOption(breachTypeOptions, breachData.incidentDetails?.breachType),
        status: findOption(statusOptions, breachData.status),
        briefDescription: breachData.incidentDetails?.briefDescription || "",
        affectedSystemOrService: Array.isArray(breachData.incidentDetails?.affectedSystemOrService)
          ? breachData.incidentDetails.affectedSystemOrService.join(', ')
          : (breachData.incidentDetails?.affectedSystemOrService || ""),
        
        // Data Involved
        dataCategories: dataCategoriesOptions,
        affectedDataPrincipalsCount: breachData.dataInvolved?.affectedDataPrincipalsCount?.toString() || "",
        dataEncryptedIfProtected: findOption(encryptionOptions, breachData.dataInvolved?.dataEncryptedOrProtected),
        potentialImpactDescription: breachData.dataInvolved?.potentialImpactDescription || "",
        
        // Notification Details - check multiple possible field locations
        dpbiNotificationDate: formatDateForInput(dpbiDate),
        dpbiAcknowledgementId: breachData.notificationDetails?.dpbiAcknowledgementId || "",
        datePrincipalNotificationDate: formatDateForInput(dataPrincipalDate),
        notificationChannel: breachData.notificationDetails?.channels?.[0]?.notificationChannel
          ? findOption(notificationChannelOptions, breachData.notificationDetails.channels[0].notificationChannel)
          : null,
      });
    }
  }, [viewMode, breachData]);

  const handleBack = () => {
    navigate("/breach-notifications");
  };

  // Get today's date and time in datetime-local format (YYYY-MM-DDTHH:mm)
  const getMaxDateTime = () => {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSelectChange = (name, selectedOption) => {
    setFormData((prev) => ({
      ...prev,
      [name]: selectedOption,
    }));
  };

  const handleSubmit = async () => {
    // Basic validation
    if (!formData.discoveryDateTime || !formData.breachType) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="Please fill in all required fields (Discovery Date & Time and Breach Type)"
          />
        ),
        { icon: false }
      );
      return;
    }

    setSaving(true);
    try {
      const txnId = generateTransactionId();
      
      // Convert datetime-local format to ISO 8601 format with timezone (without milliseconds)
      const formatDateTime = (dateTimeStr) => {
        if (!dateTimeStr) return null;
        // datetime-local returns format like "2025-10-30T10:00"
        // Convert to ISO 8601: "2025-10-30T10:00:00Z" (without milliseconds to match API format)
        const date = new Date(dateTimeStr);
        return date.toISOString().replace(/\.\d{3}Z$/, 'Z');
      };
      
      // Convert affectedSystemOrService from comma-separated string to array
      const systemsArray = formData.affectedSystemOrService
        ? formData.affectedSystemOrService.split(',').map(s => s.trim()).filter(s => s)
        : [];
      
      // Build incidentDetails
      const incidentDetails = {
        discoveryDateTime: formatDateTime(formData.discoveryDateTime),
        breachType: formData.breachType?.value || "",
        briefDescription: formData.briefDescription || "",
        affectedSystemOrService: systemsArray,
      };
      
      // Add occurrenceDateTime only if it's provided
      if (formData.occurrenceDateTime) {
        incidentDetails.occurrenceDateTime = formatDateTime(formData.occurrenceDateTime);
      }
      
      // Build dataInvolved
      const dataInvolved = {
        personalDataCategories: formData.dataCategories?.map(cat => cat.value) || [],
        affectedDataPrincipalsCount: parseInt(formData.affectedDataPrincipalsCount) || 0,
        dataEncryptedOrProtected: formData.dataEncryptedIfProtected?.value ?? false,
        potentialImpactDescription: formData.potentialImpactDescription || "",
      };
      
      // Prepare request body matching the API structure from curl
      const requestBody = {
        incidentDetails,
        dataInvolved,
        status: formData.status?.value || "NEW", // Default to NEW if not selected
      };

      console.log("=== BREACH REPORT REQUEST ===");
      console.log("Form Data:", formData);
      console.log("Status being sent:", formData.status?.value || "NEW");
      console.log("Request Body:", JSON.stringify(requestBody, null, 2));
      console.log("Headers:", {
        "Content-Type": "application/json",
        "accept": "application/json",
        "txn": txnId,
        "tenant-id": tenantId,
        "x-session-token": sessionToken?.substring(0, 50) + "...",
      });

      const response = await fetch(
        config.data_breach_create,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "accept": "application/json",
            "txn": txnId,
            "tenant-id": tenantId,
            "business-id": businessId,
            "x-session-token": sessionToken,
          },
          body: JSON.stringify(requestBody),
        }
      );

      if (response.ok) {
        const result = await response.json();
        console.log("=== SUCCESS ===");
        console.log("Breach created successfully:", result);
        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message="Breach report submitted successfully!"
            />
          ),
          { icon: false }
        );
        navigate("/breach-notifications");
      } else {
        console.error("=== API ERROR ===");
        console.error("Status:", response.status);
        console.error("Status Text:", response.statusText);
        const errorText = await response.text();
        console.error("Error Response:", errorText);
        let errorMessage = "Failed to submit breach report";
        try {
          const errorJson = JSON.parse(errorText);
          console.error("Parsed Error:", errorJson);
          errorMessage = errorJson.message || errorJson.error || errorText;
        } catch (e) {
          console.error("Could not parse error as JSON");
          errorMessage = errorText || "Failed to submit breach report";
        }
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={errorMessage}
            />
          ),
          { icon: false }
        );
      }
    } catch (error) {
      console.error("Error submitting breach report:", error);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="Error submitting breach report. Please try again."
          />
        ),
        { icon: false }
      );
    } finally {
      setSaving(false);
    }
  };

  // Custom styles for react-select to match the page
  const customSelectStyles = {
    control: (base) => ({
      ...base,
      minHeight: "40px",
      borderColor: "#d1d5db",
      "&:hover": {
        borderColor: "#9ca3af",
      },
    }),
    option: (base, state) => ({
      ...base,
      backgroundColor: state.isSelected ? "#0066cc" : state.isFocused ? "#e5e7eb" : "white",
      color: state.isSelected ? "white" : "#374151",
    }),
  };

  return (
    <div className="configurePage">
      <div className="add-ropa-entry-container">
        {/* Header */}
        <div className="add-ropa-entry-header">
          <Button
            kind="tertiary"
            size="medium"
            icon="ic_back"
            onClick={handleBack}
            style={{ minWidth: "auto" }}
          />
          <h1 style={{ fontSize: '32px', fontWeight: '700', margin: 0, color: '#1a1a1a', lineHeight: '40px' }}>
            {viewMode ? "View Breach Report" : "Report New Breach"}
          </h1>
        </div>

        {/* Form Content */}
        <div className="add-ropa-entry-content">
          {/* Incident Details Section */}
          <div className="form-section">
            <h2 className="section-heading">Incident Details</h2>
            
            <div className="form-field">
              <label>Discovery Date & Time *</label>
              <input
                type="datetime-local"
                name="discoveryDateTime"
                value={formData.discoveryDateTime}
                onChange={handleInputChange}
                required
                disabled={viewMode}
                max={getMaxDateTime()}
              />
            </div>

            <div className="form-field">
              <label>Occurrence Date & Time</label>
              <input
                type="datetime-local"
                name="occurrenceDateTime"
                value={formData.occurrenceDateTime}
                onChange={handleInputChange}
                disabled={viewMode}
                max={getMaxDateTime()}
              />
            </div>

            <div className="form-field">
              <label>Breach Type *</label>
              <Select
                options={breachTypeOptions}
                value={formData.breachType}
                onChange={(option) => handleSelectChange("breachType", option)}
                styles={customSelectStyles}
                placeholder="Select breach type"
                isDisabled={viewMode}
              />
            </div>

            <div className="form-field">
              <label>Status *</label>
              <Select
                options={statusOptions}
                value={formData.status}
                onChange={(option) => handleSelectChange("status", option)}
                styles={customSelectStyles}
                placeholder="Select status"
                isDisabled={true}
              />
              {!viewMode && (
                <small style={{ display: "block", marginTop: "4px", color: "#6b7280", fontSize: "12px" }}>
                  Status is set to "New" by default for new breach reports.
                </small>
              )}
            </div>

            <div className="form-field">
              <label>Brief Description</label>
              <textarea
                name="briefDescription"
                value={formData.briefDescription}
                onChange={handleInputChange}
                placeholder="Email with user data sent to unintended recipient"
                rows="3"
                disabled={viewMode}
              />
            </div>

            <div className="form-field">
              <label>Affected System or Service (comma-separated)</label>
              <input
                type="text"
                name="affectedSystemOrService"
                value={formData.affectedSystemOrService}
                onChange={handleInputChange}
                placeholder="Consent API, Consent API1"
                disabled={viewMode}
              />
            </div>
          </div>

          {/* Data Involved Section */}
          <div className="form-section">
            <h2 className="section-heading">Data Involved</h2>
            
            <div className="form-field">
              <label>Data Categories</label>
              <Select
                isMulti
                options={dataCategoryOptions}
                value={formData.dataCategories}
                onChange={(option) => handleSelectChange("dataCategories", option)}
                styles={customSelectStyles}
                placeholder={loading ? "Loading data categories..." : "Select data categories"}
                isLoading={loading}
                isDisabled={loading || viewMode}
              />
            </div>

            <div className="form-field">
              <label>Affected Data Principals Count</label>
              <input
                type="number"
                name="affectedDataPrincipalsCount"
                value={formData.affectedDataPrincipalsCount}
                onChange={handleInputChange}
                placeholder="500"
                disabled={viewMode}
              />
            </div>

            <div className="form-field">
              <label>Data Encrypted/Protected</label>
              <Select
                options={encryptionOptions}
                value={formData.dataEncryptedIfProtected}
                onChange={(option) => handleSelectChange("dataEncryptedIfProtected", option)}
                styles={customSelectStyles}
                placeholder="Select yes or no"
                isDisabled={viewMode}
              />
            </div>

            <div className="form-field">
              <label>Potential Impact Description</label>
              <textarea
                name="potentialImpactDescription"
                value={formData.potentialImpactDescription}
                onChange={handleInputChange}
                placeholder="Minor risk of unauthorized disclosure"
                rows="3"
                disabled={viewMode}
              />
            </div>
          </div>

          {/* Notification Details Section */}
          <div className="form-section">
            <h2 className="section-heading">Notification Details (Auto-populated)</h2>
            
            <div className="form-field">
              <label>DPBI Notification Date</label>
              <input
                type="date"
                name="dpbiNotificationDate"
                value={formData.dpbiNotificationDate}
                onChange={handleInputChange}
                disabled
                style={{ backgroundColor: '#f3f4f6', cursor: 'not-allowed' }}
              />
            </div>

            <div className="form-field">
              <label>DPBI Acknowledgement ID</label>
              <input
                type="text"
                name="dpbiAcknowledgementId"
                value={formData.dpbiAcknowledgementId}
                onChange={handleInputChange}
                placeholder="DPBI-ACK-2025-0987"
                disabled
                style={{ backgroundColor: '#f3f4f6', cursor: 'not-allowed' }}
              />
            </div>

            <div className="form-field">
              <label>Data Principal Notification Date</label>
              <input
                type="date"
                name="datePrincipalNotificationDate"
                value={formData.datePrincipalNotificationDate}
                onChange={handleInputChange}
                disabled
                style={{ backgroundColor: '#f3f4f6', cursor: 'not-allowed' }}
              />
            </div>

            <div className="form-field">
              <label>Notification Channel</label>
              <Select
                options={notificationChannelOptions}
                value={formData.notificationChannel}
                onChange={(option) => handleSelectChange("notificationChannel", option)}
                styles={customSelectStyles}
                placeholder="Select notification channel"
                isDisabled
              />
            </div>
          </div>

          {/* Submit Button - Only show in create mode */}
          {!viewMode && (
            <div className="form-actions">
              <ActionButton
                kind="primary"
                size="medium"
                label={saving ? "Submitting..." : "Submit Report"}
                onClick={handleSubmit}
                disabled={saving}
              />
            </div>
          )}
        </div>
      </div>
      <ToastContainer
        position="bottom-left"
        autoClose={3000}
        hideProgressBar
        closeOnClick
        pauseOnHover
        transition={Slide}
      />
    </div>
  );
};

export default ReportBreachEntry;

