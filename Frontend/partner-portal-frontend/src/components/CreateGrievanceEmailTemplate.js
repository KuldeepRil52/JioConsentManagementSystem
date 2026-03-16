import { ActionButton, Button, InputFieldV2, Text } from "../custom-components";
import { useNavigate } from "react-router-dom";
import { useNavigate, useLocation } from "react-router-dom";
import TextAreaForEmailTemplate from "./TextAreaForTemplate";
import { useState, useEffect } from "react";
import { useSelector } from "react-redux";
import { toast } from 'react-toastify';
import Select from "react-select";
import config from "../utils/config";

const CreateGrievanceEmailTemplate = () => {
  const navigate = useNavigate();
  const location = useLocation();

  // Get Redux state
  const tenantId = useSelector((state) => state.common.tenant_id);
  const businessId = useSelector((state) => state.common.business_id);
  const sessionToken = useSelector((state) => state.common.session_token);

  // Event Type options for grievance-related templates
  const eventTypeOptions = [
    { label: "GRIEVANCE_CREATED", value: "GRIEVANCE_CREATED" },
    { label: "GRIEVANCE_UPDATED", value: "GRIEVANCE_UPDATED" },
    { label: "GRIEVANCE_RESOLVED", value: "GRIEVANCE_RESOLVED" },
    { label: "GRIEVANCE_CLOSED", value: "GRIEVANCE_CLOSED" },
    { label: "GRIEVANCE_DENIED", value: "GRIEVANCE_DENIED" },
    { label: "GRIEVANCE_ESCALATED", value: "GRIEVANCE_ESCALATED" },
    { label: "GRIEVANCE_STATUS_UPDATED", value: "GRIEVANCE_STATUS_UPDATED" }
  ];

  // Get edit mode, view mode and template data from navigation state
  const editMode = location.state?.editMode || false;
  const viewMode = location.state?.viewMode || false;
  const templateData = location.state?.templateData || null;

  // Form state
  const [fromName, setFromName] = useState("");
  const [toEmails, setToEmails] = useState("");
  const [ccEmails, setCcEmails] = useState("");
  const [subject, setSubject] = useState("");
  const [event, setEvent] = useState("");
  const [body, setBody] = useState("");
  const [status, setStatus] = useState("");
  const [saving, setSaving] = useState(false);

  // Prefill form if in edit mode or view mode
  useEffect(() => {
    if ((editMode || viewMode) && templateData) {
      setFromName(templateData.emailConfig?.fromName || "");
      setToEmails(templateData.emailConfig?.to?.join(", ") || "");
      setCcEmails(templateData.emailConfig?.cc?.join(", ") || "");
      setSubject(templateData.emailConfig?.subject || "");
      // Set event as the full option object for the Select component
      const eventType = templateData.eventType || "";
      const selectedOption = eventTypeOptions.find(opt => opt.value === eventType);
      setEvent(selectedOption || eventType);
      setBody(templateData.emailConfig?.body || "");
      setStatus(templateData.status || "");
    }
  }, [editMode, templateData]);

  const handleBack = () => {
    // Check if we came from notification-templates page
    if (location.state?.from === '/notification-templates') {
      navigate("/notification-templates");
    } else {
      navigate("/grievanceEmailTemplate");
    }
  };

  const handleSave = async () => {
    // Validation
    if (!event || !subject || !body) {
      toast.error("Please fill in all required fields (Event Type, Subject, and Message Body)");
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
        emailTemplate: {
          to: editMode ? (templateData.emailConfig?.to || ["user@example.com"]) : (toEmails ? toEmails.split(',').map(email => email.trim()) : ["user@example.com"]),
          cc: editMode ? (templateData.emailConfig?.cc || []) : (ccEmails ? ccEmails.split(',').map(email => email.trim()) : []),
          templateDetails: editMode ? (templateData.emailConfig?.templateDetails || templateData.templateDetails || `${templateData.eventType} email notification`) : `${event.value || event} email notification`,
          templateBody: body,
          templateSubject: subject,
          templateFromName: editMode ? (templateData.emailConfig?.fromName || "DPDP Notification System") : (fromName || "DPDP Notification System"),
          emailType: editMode ? (templateData.emailConfig?.emailType || "HTML") : "HTML",
          argumentsSubjectMap: editMode ? (templateData.emailConfig?.argumentsSubjectMap || {}) : {},
          argumentsBodyMap: editMode ? (templateData.emailConfig?.argumentsBodyMap || {}) : {}
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

      const response = await fetch(url, {
        method: method,
        headers: {
          'accept': 'application/json',
          'Content-Type': 'application/json',
          'X-Scope-Level': scopeLevel,
          'X-Type': 'NOTIFICATION',
          'X-Tenant-Id': tenantId,
          'X-Business-Id': businessId,
          'X-Transaction-Id': transactionId,
          'x-session-token': sessionToken
        },
        body: JSON.stringify(requestBody)
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        console.error("API Error Response:", {
          status: response.status,
          statusText: response.statusText,
          errorData: errorData,
          errorCode: errorData.errorCode || errorData.code || "N/A",
          errorMessage: errorData.message || errorData.error || "Unknown error",
          fullError: errorData
        });
        throw new Error(errorData.message || errorData.error || `API error: ${response.status} ${response.statusText}`);
      }

      const data = await response.json();

      toast.success("Saved successfully!");

      // Navigate back to the list page
      if (location.state?.from === '/notification-templates') {
        navigate("/notification-templates");
      } else {
        navigate("/grievanceEmailTemplate");
      }

    } catch (err) {
      console.error("Error saving grievance template - Full Error Details:", {
        message: err.message,
        name: err.name,
        stack: err.stack,
        error: err,
        errorCode: err.errorCode || err.code || "N/A",
        response: err.response,
        status: err.status,
        statusText: err.statusText,
        data: err.data,
        config: err.config
      });
      toast.error(`Failed to save template: ${err.message}`);
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
                {viewMode ? "View Grievance Email Template" : editMode ? "Edit Grievance Email Template" : "Create Grievance Email Template"}
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
                isDisabled={editMode || viewMode}
              />
            </div>
            <br></br>

            <InputFieldV2
              label="Subject"
              size="small"
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              disabled={viewMode}
            />
            <br></br>

            <TextAreaForEmailTemplate value={body} onChange={setBody} disabled={viewMode} />

            {!viewMode && (
              <ActionButton
                label={saving ? "Saving..." : "Save"}
                kind="primary"
                onClick={handleSave}
                disabled={saving}
              />
            )}

            {/* Arguments Map Note */}
            {(editMode || viewMode) && (templateData?.emailConfig?.argumentsSubjectMap || templateData?.emailConfig?.argumentsBodyMap) && (
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

                {/* Subject Placeholders */}
                {templateData?.emailConfig?.argumentsSubjectMap && (
                  <div style={{ marginTop: "15px" }}>
                    <Text appearance="body-xs-bold" color="primary-grey-70">
                      Subject Line:
                    </Text>
                    <div style={{ marginTop: "5px" }}>
                      {Object.entries(templateData.emailConfig.argumentsSubjectMap).map(([key, value]) => (
                        <div key={key} style={{
                          padding: "3px 0",
                          fontSize: "13px",
                          fontFamily: "monospace"
                        }}>
                          <span style={{ color: "#0066cc", fontWeight: "600" }}>{key}</span>
                          <span style={{ color: "#666" }}> → </span>
                          <span style={{ color: "#333" }}>{value}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Body Placeholders */}
                {templateData?.emailConfig?.argumentsBodyMap && (
                  <div style={{ marginTop: "15px" }}>
                    <Text appearance="body-xs-bold" color="primary-grey-70">
                      Message Body:
                    </Text>
                    <div style={{ marginTop: "5px" }}>
                      {Object.entries(templateData.emailConfig.argumentsBodyMap).map(([key, value]) => (
                        <div key={key} style={{
                          padding: "3px 0",
                          fontSize: "13px",
                          fontFamily: "monospace"
                        }}>
                          <span style={{ color: "#0066cc", fontWeight: "600" }}>{key}</span>
                          <span style={{ color: "#666" }}> → </span>
                          <span style={{ color: "#333" }}>{value}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                <div style={{ marginTop: "15px", fontSize: "12px", color: "#666", paddingTop: "10px", borderTop: "1px solid #e0e0e0" }}>
                  <Text appearance="body-xs" color="primary-grey-60">
                    💡 Use these placeholders in your subject line and message body. They will be replaced with actual values when the email is sent.
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
                {body ? (
                  <div dangerouslySetInnerHTML={{ __html: body }} />
                ) : (
                  <div style={{ textAlign: "center", padding: "40px", color: "#999" }}>
                    <Text appearance="body-s" color="primary-grey-60">
                      Preview will appear here as you type the message body
                    </Text>
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

export default CreateGrievanceEmailTemplate;
