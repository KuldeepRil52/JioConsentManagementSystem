import { ActionButton, Button, InputFieldV2, Text } from "../custom-components";
import { useNavigate } from "react-router-dom";
import { useNavigate, useLocation } from "react-router-dom";
import { useState, useEffect } from "react";
import { useSelector } from "react-redux";
import { toast } from 'react-toastify';
import Select from "react-select";
import TextAreaForEmailTemplate from "./TextAreaForTemplate";
import config from "../utils/config";

const CreateSmsTemplate = () => {
  const navigate = useNavigate();
  const location = useLocation();
  
  // Get Redux state
  const tenantId = useSelector((state) => state.common.tenant_id);
  const businessId = useSelector((state) => state.common.business_id);
  const sessionToken = useSelector((state) => state.common.session_token);
  
  // Event Type options for consent-related templates
  const eventTypeOptions = [
    { label: "CONSENT_REQUEST_PENDING", value: "CONSENT_REQUEST_PENDING" },
    { label: "CONSENT_REQUEST_RENEWAL", value: "CONSENT_REQUEST_RENEWAL" },
    { label: "CONSENT_REQUEST_EXPIRED", value: "CONSENT_REQUEST_EXPIRED" },
    { label: "CONSENT_PREFERENCE_EXPIRY", value: "CONSENT_PREFERENCE_EXPIRY" },
    { label: "CONSENT_CREATED", value: "CONSENT_CREATED" },
    { label: "CONSENT_UPDATED", value: "CONSENT_UPDATED" },
    { label: "CONSENT_WITHDRAWN", value: "CONSENT_WITHDRAWN" },
    { label: "CONSENT_EXPIRED", value: "CONSENT_EXPIRED" },
    { label: "CONSENT_RENEWED", value: "CONSENT_RENEWED" }
  ];
  
  // Get edit mode and template data from navigation state
  const editMode = location.state?.editMode || false;
  const templateData = location.state?.templateData || null;
  
  // Form state
  const [event, setEvent] = useState("");
  const [dltTemplateId, setDltTemplateId] = useState("");
  const [message, setMessage] = useState("");
  const [status, setStatus] = useState("");
  const [recipientType, setRecipientType] = useState("");
  const [saving, setSaving] = useState(false);
  
  // Prefill form if in edit mode
  useEffect(() => {
    if (editMode && templateData) {
      // Set event as the full option object for the Select component
      const eventType = templateData.eventType || "";
      const selectedOption = eventTypeOptions.find(opt => opt.value === eventType);
      setEvent(selectedOption || eventType);
      setDltTemplateId(templateData.smsConfig?.dltTemplateId || templateData.templateId || "");
      // Read from 'template' field at root level (API returns it at root, not in smsConfig)
      setMessage(templateData.template || "");
      setStatus(templateData.status || "");
      setRecipientType(templateData.recipientType || "");
    }
  }, [editMode, templateData]);
  
  const handleBack = () => {
    // Check if we came from notification-templates page
    if (location.state?.from === '/notification-templates') {
      navigate("/notification-templates");
    } else {
      navigate("/smsTemplate");
    }
  };
  
  const handleSave = async () => {
    // Validation
    if (!event || !message) {
      toast.error("Please fill in all required fields (Event Type and Message)");
      return;
    }
    
    setSaving(true);
    
    try {
      // Calculate scope level
      const scopeLevel = tenantId === businessId ? 'TENANT' : 'BUSINESS';
      
      // Generate transaction ID
      const transactionId = `${Date.now()}-${Math.random().toString(36).substring(2, 15)}`;
      
      // Build request body matching the API structure - same for both create and edit
      const requestBody = {
        eventType: editMode ? templateData.eventType : (event.value || event),
        language: editMode ? (templateData.language || "english") : "english",
        smsTemplate: {
          template: message,
          whiteListedNumber: editMode ? (templateData.smsConfig?.whiteListedNumber || []) : [],
          templateDetails: editMode ? (templateData.smsConfig?.templateDetails || templateData.templateDetails || `${templateData.eventType} SMS notification`) : `${event.value || event} SMS notification`,
          oprCountries: editMode ? (templateData.smsConfig?.oprCountries || ["IN"]) : ["IN"],
          dltEntityId: editMode ? (templateData.smsConfig?.dltEntityId || "") : "",
          dltTemplateId: dltTemplateId || (editMode ? (templateData.smsConfig?.dltTemplateId || "") : ""),
          from: editMode ? (templateData.smsConfig?.from || "JioGCS-S") : "JioGCS-S",
          argumentsMap: editMode ? (templateData.smsConfig?.argumentsMap || {}) : {}
        }
      };
      
      // Use PUT method with template ID when editing, POST when creating
      const templateId = editMode ? (templateData?.id || templateData?.templateId || templateData?._id) : null;
      
      // Ensure we use PUT for edit mode and POST for create mode
      if (editMode && !templateId) {
        toast.error("Template ID is missing. Cannot update template.");
        setSaving(false);
        return;
      }
      
      const method = editMode ? 'PUT' : 'POST';
      const url = editMode && templateId
        ? `${config.notification_templates}/${templateId}`
        : config.notification_templates;
      
      console.log("Sending SMS template to API:", JSON.stringify(requestBody, null, 2));
      
      const response = await fetch(url, {
        method: method,
        headers: {
          'accept': 'application/json',
          'Content-Type': 'application/json',
          'X-Scope-Level': scopeLevel,
          'X-Type': 'NOTIFICATION',
          'X-Tenant-Id': tenantId,
          'X-Business-Id': businessId,
          'x-session-token': sessionToken,
          'X-Transaction-Id': transactionId
        },
        body: JSON.stringify(requestBody)
      });
      
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `API error: ${response.status} ${response.statusText}`);
      }
      
      const data = await response.json();
      console.log("SMS Template saved successfully:", data);
      
      toast.success(editMode ? "SMS Template updated successfully!" : "SMS Template created successfully!");
      
      // Navigate back to the list page
      setTimeout(() => {
        if (location.state?.from === '/notification-templates') {
          navigate("/notification-templates");
        } else {
          navigate("/smsTemplate");
        }
      }, 1000);
      
    } catch (err) {
      console.error("Error saving SMS template:", err);
      toast.error(`Failed to save SMS template: ${err.message}`);
    } finally {
      setSaving(false);
    }
  };
  return (
    <>
      <div className="configurePage">
        <div
          style={{
            display: "flex",
            gap: 20,
            width: "95%",
          }}
        >
          <div style={{ flex: "0 0 auto" }}>
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
              size="medium"
              state="normal"
            />
          </div>
          
          {/* Left Side - Form */}
          <div style={{ flex: "1", maxWidth: "600px" }}>
            <div>
              <Text appearance="heading-xs" color="primary-grey-100">
                {editMode ? "Edit SMS Template" : "Create SMS Template"}
              </Text>
            </div>
            <br></br>
            
            <div style={{ marginBottom: "16px" }}>
              <Text appearance="body-xs" color="primary-grey-80">
                Event Type
              </Text>
              <Select
                options={eventTypeOptions}
                value={event}
                onChange={setEvent}
                placeholder="Select Event Type"
                isDisabled={editMode}
              />
            </div>
            
            <InputFieldV2 
              label="DLT Template ID" 
              size="small" 
              value={dltTemplateId}
              onChange={(e) => setDltTemplateId(e.target.value)}
            />
            <br></br>
            
            <InputFieldV2 
              label="Status" 
              size="small" 
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              placeholder="ACTIVE, INACTIVE"
            />
            <br></br>
            
            <InputFieldV2 
              label="Recipient Type" 
              size="small" 
              value={recipientType}
              onChange={(e) => setRecipientType(e.target.value)}
              placeholder="USER, ADMIN, etc."
            />
            <br></br>

            <TextAreaForEmailTemplate value={message} onChange={setMessage} />

            <ActionButton 
              label={saving ? "Saving..." : "Save"} 
              kind="primary"
              onClick={handleSave}
              disabled={saving}
            />
            
            {/* Arguments Map Note */}
            {editMode && templateData?.smsConfig?.argumentsMap && (
              <div style={{
                marginTop: "20px",
                padding: "15px",
                backgroundColor: "#f5f5f5",
                borderRadius: "8px",
                border: "1px solid #e0e0e0"
              }}>
                <Text appearance="body-xs-bold" color="primary-grey-80">
                  📝 Available Placeholders:
                </Text>
                <div style={{ marginTop: "10px" }}>
                  {Object.entries(templateData.smsConfig.argumentsMap).map(([key, value]) => (
                    <div key={key} style={{ 
                      padding: "5px 0",
                      fontSize: "13px",
                      fontFamily: "monospace"
                    }}>
                      <span style={{ color: "#0066cc", fontWeight: "600" }}>{key}</span>
                      <span style={{ color: "#666" }}> → </span>
                      <span style={{ color: "#333" }}>{value}</span>
                    </div>
                  ))}
                </div>
                <div style={{ marginTop: "10px", fontSize: "12px", color: "#666" }}>
                  <Text appearance="body-xs" color="primary-grey-60">
                    Use these placeholders in your message. They will be replaced with actual values when the SMS is sent.
                  </Text>
                </div>
              </div>
            )}
          </div>
          
          {/* Right Side - Preview */}
          <div style={{ flex: "1", minWidth: "400px" }}>
            <div style={{ position: "sticky", top: "20px" }}>
              <Text appearance="heading-xs" color="primary-grey-100">
                Preview
              </Text>
              <br></br>
              <div
                style={{
                  backgroundColor: "#ffffff",
                  border: "1px solid #e0e0e0",
                  borderRadius: "8px",
                  padding: "20px",
                  maxHeight: "80vh",
                  overflowY: "auto",
                  boxShadow: "0 2px 8px rgba(0, 0, 0, 0.1)",
                }}
              >
                {message ? (
                  <div style={{ 
                    whiteSpace: "pre-wrap", 
                    wordBreak: "break-word",
                    fontFamily: "system-ui, -apple-system, sans-serif",
                    fontSize: "14px",
                    lineHeight: "1.5",
                    color: "#333"
                  }}>
                    {message}
                  </div>
                ) : (
                  <div style={{ textAlign: "center", padding: "40px", color: "#999" }}>
                    <Text appearance="body-s" color="primary-grey-60">
                      Preview will appear here as you type the message
                    </Text>
                  </div>
                )}
                {message && (
                  <div style={{ 
                    marginTop: "20px", 
                    paddingTop: "10px", 
                    borderTop: "1px solid #e0e0e0",
                    fontSize: "12px",
                    color: "#999"
                  }}>
                    Character count: {message.length}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};
export default CreateSmsTemplate;
