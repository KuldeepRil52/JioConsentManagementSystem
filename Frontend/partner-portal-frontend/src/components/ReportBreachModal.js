import React, { useState } from "react";
import { Text, ActionButton, InputFieldV2 } from "../custom-components";
import { IcClose } from "../custom-components/Icon";
import "../styles/masterSetup.css";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";

const ReportBreachModal = ({ onClose, onAdd }) => {
  const [formData, setFormData] = useState({
    breachType: "",
    dateDetected: "",
    timeDetected: "",
    severity: "",
    affectedRecords: "",
    status: "Under Investigation",
    reportedTo: "",
    notificationSent: "No",
    incidentDescription: "",
    immediateActions: "",
    rootCause: "",
    remediationPlan: "",
    dataCategories: "",
    individualContacted: "No",
    regulatorNotified: "No",
    incidentOwner: "",
    estimatedImpact: "",
  });

  const handleChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const validateForm = () => {
    const errors = [];

    if (!formData.breachType.trim()) {
      errors.push("Breach Type is required");
    }
    if (!formData.dateDetected) {
      errors.push("Date Detected is required");
    }
    if (!formData.severity) {
      errors.push("Severity level is required");
    }
    if (!formData.incidentDescription.trim()) {
      errors.push("Incident Description is required");
    }
    if (!formData.incidentOwner.trim()) {
      errors.push("Incident Owner is required");
    }

    return errors;
  };

  const handleSubmit = () => {
    const errors = validateForm();

    if (errors.length > 0) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={
              <ul style={{ margin: 0, paddingLeft: "20px" }}>
                {errors.map((err, idx) => (
                  <li key={idx}>{err}</li>
                ))}
              </ul>
            }
          />
        ),
        { icon: false }
      );
      return;
    }

    // Generate incident ID
    const incidentId = `BRCH${String(Date.now()).slice(-6)}`;

    // Create new breach entry
    const newBreach = {
      id: Date.now(),
      incidentId: incidentId,
      breachType: formData.breachType,
      dateDetected: formData.dateDetected,
      timeDetected: formData.timeDetected,
      severity: formData.severity,
      affectedRecords: parseInt(formData.affectedRecords) || 0,
      status: formData.status,
      reportedTo: formData.reportedTo || "N/A",
      notificationSent: formData.notificationSent,
      incidentDescription: formData.incidentDescription,
      immediateActions: formData.immediateActions,
      rootCause: formData.rootCause,
      remediationPlan: formData.remediationPlan,
      dataCategories: formData.dataCategories,
      individualContacted: formData.individualContacted,
      regulatorNotified: formData.regulatorNotified,
      incidentOwner: formData.incidentOwner,
      estimatedImpact: formData.estimatedImpact,
    };

    toast.success(
      (props) => (
        <CustomToast
          {...props}
          type="success"
          message={`Breach notification ${incidentId} reported successfully`}
        />
      ),
      { icon: false }
    );

    onAdd(newBreach);
    onClose();
  };

  return (
    <>
      <div className="modal-outer-container">
        <div
          className="master-set-up-modal-container"
          style={{ maxWidth: "900px", maxHeight: "85vh", overflowY: "auto" }}
        >
          <div className="modal-close-btn-container">
            <ActionButton onClick={onClose} icon={<IcClose />} kind="tertiary" />
          </div>

          <Text appearance="heading-xs" color="primary-grey-100">
            Report Breach Notification
          </Text>
          <br />
          <Text appearance="body-s" color="primary-grey-80">
            Fill in the details to report a data breach incident
          </Text>
          <br />

          {/* Incident Details Section */}
          <div className="title-margin">
            <Text appearance="heading-xxs" color="primary-grey-100">
              Incident Details
            </Text>
          </div>
          <br />

          <Text appearance="body-xs" color="primary-grey-80">
            Breach Type (Required)
          </Text>
          <div className="dropdown-group-pa" style={{ marginBottom: "10px" }}>
            <select
              value={formData.breachType}
              onChange={(e) => handleChange("breachType", e.target.value)}
            >
              <option value="" disabled>
                Select Breach Type
              </option>
              <option value="Data Leak">Data Leak</option>
              <option value="Unauthorized Access">Unauthorized Access</option>
              <option value="Phishing Attack">Phishing Attack</option>
              <option value="Malware/Ransomware">Malware/Ransomware</option>
              <option value="Insider Threat">Insider Threat</option>
              <option value="Lost/Stolen Device">Lost/Stolen Device</option>
              <option value="Accidental Disclosure">Accidental Disclosure</option>
              <option value="System Vulnerability">System Vulnerability</option>
              <option value="Third-Party Breach">Third-Party Breach</option>
              <option value="Other">Other</option>
            </select>
          </div>
          <br />

          <div style={{ display: "flex", gap: "1rem" }}>
            <div style={{ flex: 1 }}>
              <InputFieldV2
                label="Date Detected (Required)"
                type="date"
                value={formData.dateDetected}
                onChange={(e) => handleChange("dateDetected", e.target.value)}
                size="medium"
              />
            </div>
            <div style={{ flex: 1 }}>
              <InputFieldV2
                label="Time Detected"
                type="time"
                value={formData.timeDetected}
                onChange={(e) => handleChange("timeDetected", e.target.value)}
                size="medium"
              />
            </div>
          </div>
          <br />

          <Text appearance="body-xs" color="primary-grey-80">
            Severity Level (Required)
          </Text>
          <div className="dropdown-group-pa" style={{ marginBottom: "10px" }}>
            <select
              value={formData.severity}
              onChange={(e) => handleChange("severity", e.target.value)}
            >
              <option value="" disabled>
                Select Severity Level
              </option>
              <option value="Critical">Critical</option>
              <option value="High">High</option>
              <option value="Medium">Medium</option>
              <option value="Low">Low</option>
            </select>
          </div>
          <br />

          <InputFieldV2
            label="Incident Owner/Reporter (Required)"
            value={formData.incidentOwner}
            onChange={(e) => handleChange("incidentOwner", e.target.value)}
            size="medium"
            placeholder="Name of person reporting/managing the incident"
          />
          <br />

          <div>
            <Text appearance="body-xs" color="primary-grey-80">
              Incident Description (Required)
            </Text>
            <textarea
              placeholder="Provide a detailed description of the breach incident"
              value={formData.incidentDescription}
              onChange={(e) =>
                handleChange("incidentDescription", e.target.value)
              }
              rows="4"
            />
          </div>
          <br />

          {/* Impact Assessment Section */}
          <div className="title-margin">
            <Text appearance="heading-xxs" color="primary-grey-100">
              Impact Assessment
            </Text>
          </div>
          <br />

          <InputFieldV2
            label="Number of Affected Records"
            type="number"
            value={formData.affectedRecords}
            onChange={(e) => handleChange("affectedRecords", e.target.value)}
            size="medium"
            placeholder="0"
          />
          <br />

          <div>
            <Text appearance="body-xs" color="primary-grey-80">
              Categories of Data Affected
            </Text>
            <textarea
              placeholder="E.g., Personal identifiable information, Financial data, Health records"
              value={formData.dataCategories}
              onChange={(e) => handleChange("dataCategories", e.target.value)}
              rows="3"
            />
          </div>
          <br />

          <div>
            <Text appearance="body-xs" color="primary-grey-80">
              Estimated Impact
            </Text>
            <textarea
              placeholder="Describe the potential or actual impact of the breach"
              value={formData.estimatedImpact}
              onChange={(e) => handleChange("estimatedImpact", e.target.value)}
              rows="3"
            />
          </div>
          <br />

          {/* Response Actions Section */}
          <div className="title-margin">
            <Text appearance="heading-xxs" color="primary-grey-100">
              Response Actions
            </Text>
          </div>
          <br />

          <Text appearance="body-xs" color="primary-grey-80">
            Incident Status
          </Text>
          <div className="dropdown-group-pa" style={{ marginBottom: "10px" }}>
            <select
              value={formData.status}
              onChange={(e) => handleChange("status", e.target.value)}
            >
              <option value="Under Investigation">Under Investigation</option>
              <option value="Contained">Contained</option>
              <option value="Resolved">Resolved</option>
              <option value="Monitoring">Monitoring</option>
            </select>
          </div>
          <br />

          <div>
            <Text appearance="body-xs" color="primary-grey-80">
              Immediate Actions Taken
            </Text>
            <textarea
              placeholder="Describe immediate actions taken to contain the breach"
              value={formData.immediateActions}
              onChange={(e) => handleChange("immediateActions", e.target.value)}
              rows="3"
            />
          </div>
          <br />

          <div>
            <Text appearance="body-xs" color="primary-grey-80">
              Root Cause Analysis
            </Text>
            <textarea
              placeholder="What caused the breach? (If known)"
              value={formData.rootCause}
              onChange={(e) => handleChange("rootCause", e.target.value)}
              rows="3"
            />
          </div>
          <br />

          <div>
            <Text appearance="body-xs" color="primary-grey-80">
              Remediation Plan
            </Text>
            <textarea
              placeholder="Describe the plan to prevent future incidents"
              value={formData.remediationPlan}
              onChange={(e) => handleChange("remediationPlan", e.target.value)}
              rows="3"
            />
          </div>
          <br />

          {/* Notification & Compliance Section */}
          <div className="title-margin">
            <Text appearance="heading-xxs" color="primary-grey-100">
              Notification & Compliance
            </Text>
          </div>
          <br />

          <div style={{ display: "flex", gap: "1rem" }}>
            <div style={{ flex: 1 }}>
              <Text appearance="body-xs" color="primary-grey-80">
                Affected Individuals Contacted?
              </Text>
              <div className="dropdown-group-pa" style={{ marginBottom: "10px" }}>
                <select
                  value={formData.individualContacted}
                  onChange={(e) =>
                    handleChange("individualContacted", e.target.value)
                  }
                >
                  <option value="Yes">Yes</option>
                  <option value="No">No</option>
                  <option value="In Progress">In Progress</option>
                  <option value="Not Required">Not Required</option>
                </select>
              </div>
            </div>
            <div style={{ flex: 1 }}>
              <Text appearance="body-xs" color="primary-grey-80">
                Regulator Notified?
              </Text>
              <div className="dropdown-group-pa" style={{ marginBottom: "10px" }}>
                <select
                  value={formData.regulatorNotified}
                  onChange={(e) =>
                    handleChange("regulatorNotified", e.target.value)
                  }
                >
                  <option value="Yes">Yes</option>
                  <option value="No">No</option>
                  <option value="Pending">Pending</option>
                  <option value="Not Required">Not Required</option>
                </select>
              </div>
            </div>
          </div>
          <br />

          <div>
            <Text appearance="body-xs" color="primary-grey-80">
              Parties Reported To
            </Text>
            <textarea
              placeholder="E.g., Regulator, Affected Individuals, Law Enforcement, Insurance"
              value={formData.reportedTo}
              onChange={(e) => handleChange("reportedTo", e.target.value)}
              rows="2"
            />
          </div>
          <br />

          <Text appearance="body-xs" color="primary-grey-80">
            Notification Sent?
          </Text>
          <div className="dropdown-group-pa" style={{ marginBottom: "10px" }}>
            <select
              value={formData.notificationSent}
              onChange={(e) => handleChange("notificationSent", e.target.value)}
            >
              <option value="Yes">Yes</option>
              <option value="No">No</option>
              <option value="Partial">Partial</option>
            </select>
          </div>
          <br />

          <div className="modal-add-btn-container">
            <ActionButton
              label="Report Breach"
              kind="primary"
              onClick={handleSubmit}
            />
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

export default ReportBreachModal;

