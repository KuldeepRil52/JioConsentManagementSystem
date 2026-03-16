import { ActionButton, Button, InputFieldV2, Text } from "../custom-components";
import { useNavigate } from "react-router-dom";
import { useNavigate, useLocation } from "react-router-dom";
import { useState, useEffect } from "react";
import { useSelector } from "react-redux";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import "../styles/toast.css";
import Select from "react-select";
import TextAreaForEmailTemplate from "./TextAreaForTemplate";
import config from "../utils/config";
import DOMPurify from 'dompurify';

const CreateEmailTemplate = () => {
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
  const [fromName, setFromName] = useState("");
  const [toEmails, setToEmails] = useState("");
  const [ccEmails, setCcEmails] = useState("");
  const [subject, setSubject] = useState("");
  const [event, setEvent] = useState("");
  const [body, setBody] = useState("");
  const [status, setStatus] = useState("");
  const [saving, setSaving] = useState(false);

  // Validation error states
  const [subjectError, setSubjectError] = useState("");
  const [bodyError, setBodyError] = useState("");
  const [toEmailsError, setToEmailsError] = useState("");
  const [ccEmailsError, setCcEmailsError] = useState("");

  // Validation constraints
  const MAX_SUBJECT_LENGTH = 200;
  const MAX_BODY_LENGTH = 50000; // 50KB for HTML content
  const MAX_EMAIL_LENGTH = 254; // RFC standard

  // Configure DOMPurify for email templates
  const sanitizeHtml = (html) => {
    return DOMPurify.sanitize(html, {
      ALLOWED_TAGS: [
        'p', 'br', 'strong', 'em', 'u', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
        'ul', 'ol', 'li', 'a', 'img', 'table', 'thead', 'tbody', 'tr', 'th', 'td',
        'div', 'span', 'blockquote', 'pre', 'code', 'hr', 'b', 'i'
      ],
      ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'class', 'id', 'style', 'target'],
      ALLOW_DATA_ATTR: false,
      ADD_ATTR: ['target'], // Allow target for links
      FORBID_TAGS: ['script', 'iframe', 'object', 'embed', 'form', 'input', 'button'],
      FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover']
    });
  };

  // Validation functions
  const validateSubject = (value) => {
    if (!value || !value.trim()) {
      return "Subject is required";
    }
    if (value.length > MAX_SUBJECT_LENGTH) {
      return `Subject must not exceed ${MAX_SUBJECT_LENGTH} characters`;
    }
    // Check for script tags or other malicious patterns
    if (/<script|javascript:|onerror=/i.test(value)) {
      return "Subject contains invalid characters";
    }
    return "";
  };

  const validateEmailList = (emailString, fieldName) => {
    if (!emailString || !emailString.trim()) {
      return ""; // Optional field
    }
    
    const emails = emailString.split(',').map(e => e.trim()).filter(e => e);
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    
    for (const email of emails) {
      if (email.length > MAX_EMAIL_LENGTH) {
        return `${fieldName}: Email address too long (max ${MAX_EMAIL_LENGTH} characters)`;
      }
      if (!emailRegex.test(email)) {
        return `${fieldName}: Invalid email format - ${email}`;
      }
      // Check for dangerous patterns
      if (/<|>|javascript:|onerror=/i.test(email)) {
        return `${fieldName}: Email contains invalid characters`;
      }
    }
    return "";
  };

  const validateBody = (html) => {
    if (!html || !html.trim()) {
      return "Message body is required";
    }
    if (html.length > MAX_BODY_LENGTH) {
      return `Message body is too large (max ${MAX_BODY_LENGTH} characters)`;
    }
    return "";
  };

  // Prefill form if in edit mode
  useEffect(() => {
    if (editMode && templateData) {
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
      navigate("/emailTemplate");
    }
  };

  const handleSave = async () => {
    // Validate all fields
    const subjectValidation = validateSubject(subject);
    const bodyValidation = validateBody(body);
    const toEmailsValidation = validateEmailList(toEmails, "To");
    const ccEmailsValidation = validateEmailList(ccEmails, "CC");

    // Set error states
    setSubjectError(subjectValidation);
    setBodyError(bodyValidation);
    setToEmailsError(toEmailsValidation);
    setCcEmailsError(ccEmailsValidation);

    // Check if any validation failed
    if (subjectValidation || bodyValidation || toEmailsValidation || ccEmailsValidation) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please fix validation errors before saving"}
          />
        ),
        { icon: false }
      );
      return;
    }

    // Check required fields
    if (!event) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please select an Event Type"}
          />
        ),
        { icon: false }
      );
      return;
    }

    setSaving(true);

    try {
      // Calculate scope level
      const scopeLevel = tenantId === businessId ? 'TENANT' : 'BUSINESS';

      // Generate transaction ID
      const transactionId = `${Date.now()}-${Math.random().toString(36).substring(2, 15)}`;

      // Sanitize inputs before sending
      const sanitizedSubject = DOMPurify.sanitize(subject.trim(), { ALLOWED_TAGS: [] });
      const sanitizedBody = sanitizeHtml(body);
      const sanitizedFromName = fromName ? DOMPurify.sanitize(fromName.trim(), { ALLOWED_TAGS: [] }) : "DPDP Notification System";

      // Build request body matching the API structure
      const requestBody = {
        eventType: editMode ? templateData.eventType : (event.value || event),
        emailTemplate: {
          to: editMode ? (templateData.emailConfig?.to || ["user@example.com"]) : (toEmails ? toEmails.split(',').map(email => DOMPurify.sanitize(email.trim(), { ALLOWED_TAGS: [] })) : ["user@example.com"]),
          cc: editMode ? (templateData.emailConfig?.cc || []) : (ccEmails ? ccEmails.split(',').map(email => DOMPurify.sanitize(email.trim(), { ALLOWED_TAGS: [] })) : []),
          templateDetails: editMode ? (templateData.emailConfig?.templateDetails || templateData.templateDetails || `${templateData.eventType} email notification`) : `${event.value || event} email notification`,
          templateBody: sanitizedBody,
          templateSubject: sanitizedSubject,
          templateFromName: editMode ? (templateData.emailConfig?.fromName || "DPDP Notification System") : sanitizedFromName,
          emailType: editMode ? (templateData.emailConfig?.emailType || "HTML") : "HTML",
          argumentsSubjectMap: editMode ? (templateData.emailConfig?.argumentsSubjectMap || {}) : {},
          argumentsBodyMap: editMode ? (templateData.emailConfig?.argumentsBodyMap || {}) : {}
        },
        exactlyOneTemplateProvided: true
      };

      // Use PUT method with template ID when editing, POST when creating
      const templateId = editMode ? (templateData?.id || templateData?.templateId || templateData?._id) : null;
      
      // Ensure we use PUT for edit mode and POST for create mode
      if (editMode && !templateId) {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message="Template ID is missing. Cannot update template."
            />
          ),
          { icon: false }
        );
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

      toast.success(
        (props) => (
          <CustomToast
            {...props}
            type="success"
            message={editMode ? "Template updated successfully!" : "Template created successfully!"}
          />
        ),
        { icon: false }
      );

      // Navigate back to the list page
      setTimeout(() => {
        if (location.state?.from === '/notification-templates') {
          navigate("/notification-templates");
        } else {
          navigate("/emailTemplate");
        }
      }, 1000);

    } catch (err) {
      console.error("Error saving template:", err);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={`Failed to save template: ${err.message}`}
          />
        ),
        { icon: false }
      );
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
                {editMode ? "Edit Email Template" : "Create Email Template"}
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

            {/* <InputFieldV2
              label="From Name" 
              size="small" 
              value={fromName}
              onChange={(e) => setFromName(e.target.value)}
            />
            <br></br>
            
            <InputFieldV2 
              label="To (comma separated)" 
              size="small" 
              value={toEmails}
              onChange={(e) => setToEmails(e.target.value)}
              placeholder="user@example.com, admin@example.com"
            />
            <br></br>
            
            <InputFieldV2 
              label="CC (comma separated)" 
              size="small" 
              value={ccEmails}
              onChange={(e) => setCcEmails(e.target.value)}
              placeholder="cc1@example.com, cc2@example.com"
            /> */}
            <br></br>

            <InputFieldV2
              label="Subject *"
              size="small"
              value={subject}
              onChange={(e) => {
                const value = e.target.value;
                setSubject(value);
                setSubjectError(validateSubject(value));
              }}
              maxLength={MAX_SUBJECT_LENGTH}
              error={!!subjectError}
              errorMessage={subjectError}
              placeholder="Enter email subject line"
            />
            {!subjectError && subject && (
              <Text appearance="body-xxs" color="primary-grey-60" style={{ display: "block", marginTop: "4px" }}>
                {subject.length}/{MAX_SUBJECT_LENGTH} characters
              </Text>
            )}


            {/* <InputFieldV2 
              label="Status" 
              size="small" 
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              placeholder="ACTIVE, INACTIVE"
            /> */}
            <br></br>

            <div>
              <TextAreaForEmailTemplate 
                value={body} 
                onChange={(newValue) => {
                  setBody(newValue);
                  setBodyError(validateBody(newValue));
                }} 
              />
              {bodyError && (
                <Text 
                  appearance="body-xxs" 
                  color="primary-red-100" 
                  style={{ display: "block", marginTop: "4px" }}
                >
                  {bodyError}
                </Text>
              )}
              {!bodyError && body && (
                <Text appearance="body-xxs" color="primary-grey-60" style={{ display: "block", marginTop: "4px" }}>
                  {body.length}/{MAX_BODY_LENGTH} characters
                </Text>
              )}
            </div>

            <ActionButton
              label={saving ? "Saving..." : "Save"}
              kind="primary"
              onClick={handleSave}
              disabled={saving}
            />

            {/* Arguments Map Note */}
            {editMode && (templateData?.emailConfig?.argumentsSubjectMap || templateData?.emailConfig?.argumentsBodyMap) && (
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
                  <>
                    <div 
                      style={{
                        padding: "8px 12px",
                        backgroundColor: "#FFF3CD",
                        border: "1px solid #FFC107",
                        borderRadius: "4px",
                        marginBottom: "15px",
                        fontSize: "12px",
                        color: "#856404"
                      }}
                    >
                      🔒 <strong>Sanitized Preview:</strong> Scripts and malicious content are automatically removed
                    </div>
                    <div dangerouslySetInnerHTML={{ __html: sanitizeHtml(body) }} />
                  </>
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
export default CreateEmailTemplate;
