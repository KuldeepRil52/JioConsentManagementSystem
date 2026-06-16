import React, { useState } from "react";
import { Text, ActionButton, InputFieldV2 } from '../custom-components';
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { createAuditReport } from "../store/actions/CommonAction";
import "../styles/pageConfiguration.css";
import "../styles/toast.css";
import { toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";

const CreateAudit = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const tenantId = useSelector((state) => state.common.tenant_id);
  const businessId = useSelector((state) => state.common.business_id);

  const [formData, setFormData] = useState({
    auditId: "",
    tenantId: tenantId || "",
    businessId: businessId || "",
    transactionId: "",
    actor: {
      id: "",
      role: "",
      type: "",
    },
    group: "",
    component: "",
    actionType: "",
    initiator: "",
    resource: {
      type: "",
      id: "",
    },
    payloadHash: "",
    context: {},
    status: "",
    timestamp: "",
  });

  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState({});

  const handleInputChange = (field, value) => {
    if (field.includes(".")) {
      const [parent, child] = field.split(".");
      setFormData((prev) => ({
        ...prev,
        [parent]: {
          ...prev[parent],
          [child]: value,
        },
      }));
    } else {
      setFormData((prev) => ({
        ...prev,
        [field]: value,
      }));
    }
    // Clear error for this field
    if (errors[field]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  const validateForm = () => {
    const newErrors = {};

    // Basic required field validations
    if (!formData.auditId?.trim()) {
      newErrors.auditId = "Audit ID is required";
    }
    if (!formData.actionType?.trim()) {
      newErrors.actionType = "Action Type is required";
    }
    if (!formData.initiator?.trim()) {
      newErrors.initiator = "Initiator is required";
    }
    if (!formData.status?.trim()) {
      newErrors.status = "Status is required";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="Please fill all required fields"
          />
        ),
        { icon: false }
      );
      return;
    }

    try {
      setLoading(true);

      // Set timestamp if not provided
      const payload = {
        ...formData,
        timestamp: formData.timestamp || new Date().toISOString(),
        tenantId: tenantId || formData.tenantId,
        businessId: businessId || formData.businessId,
      };

      const response = await dispatch(createAuditReport(payload, tenantId, businessId));

      if (response?.status === 200 || response?.status === 201) {
        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message="Audit report created successfully"
            />
          ),
          { icon: false }
        );

        // Redirect back to audit compliance page after a short delay
        setTimeout(() => {
          navigate("/audit-compliance");
        }, 1500);
      } else {
        throw new Error(response?.data?.message || "Failed to create audit report");
      }
    } catch (error) {
      console.error("Error creating audit report:", error);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={error.message || "Failed to create audit report. Please try again."}
          />
        ),
        { icon: false }
      );
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    navigate("/audit-compliance");
  };

  return (
    <div className="configurePage">
      <div style={{ maxWidth: "900px", margin: "0 auto", padding: "20px" }}>
        <div style={{ marginBottom: "24px" }}>
          <Text appearance="heading-m" color="primary-grey-100">
            Create New Audit Report
          </Text>
          <br />
          <Text appearance="body-s" color="primary-grey-80">
            Fill in the details to create a new audit report
          </Text>
        </div>

        <form onSubmit={handleSubmit}>
          <div style={{ backgroundColor: "white", padding: "24px", borderRadius: "8px", border: "1px solid #e5e5e5" }}>
            {/* Basic Information */}
            <div style={{ marginBottom: "32px" }}>
              <Text appearance="heading-xs" color="primary-grey-100" style={{ marginBottom: "16px" }}>
                Basic Information
              </Text>
              
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px", marginBottom: "16px" }}>
                <InputFieldV2
                  label="Audit ID (Required)"
                  placeholder="Enter audit ID"
                  value={formData.auditId}
                  onChange={(e) => handleInputChange("auditId", e.target.value)}
                  required
                  size="medium"
                  state={errors.auditId ? "error" : "none"}
                  stateText={errors.auditId || ""}
                />
                <InputFieldV2
                  label="Transaction ID"
                  placeholder="Enter transaction ID"
                  value={formData.transactionId}
                  onChange={(e) => handleInputChange("transactionId", e.target.value)}
                  size="medium"
                />
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px", marginBottom: "16px" }}>
                <InputFieldV2
                  label="Group"
                  placeholder="Enter group"
                  value={formData.group}
                  onChange={(e) => handleInputChange("group", e.target.value)}
                  size="medium"
                />
                <InputFieldV2
                  label="Component"
                  placeholder="Enter component"
                  value={formData.component}
                  onChange={(e) => handleInputChange("component", e.target.value)}
                  size="medium"
                />
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px", marginBottom: "16px" }}>
                <InputFieldV2
                  label="Action Type (Required)"
                  placeholder="Enter action type"
                  value={formData.actionType}
                  onChange={(e) => handleInputChange("actionType", e.target.value)}
                  required
                  size="medium"
                  state={errors.actionType ? "error" : "none"}
                  stateText={errors.actionType || ""}
                />
                <InputFieldV2
                  label="Initiator (Required)"
                  placeholder="Enter initiator"
                  value={formData.initiator}
                  onChange={(e) => handleInputChange("initiator", e.target.value)}
                  required
                  size="medium"
                  state={errors.initiator ? "error" : "none"}
                  stateText={errors.initiator || ""}
                />
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px", marginBottom: "16px" }}>
                <InputFieldV2
                  label="Status (Required)"
                  placeholder="Enter status"
                  value={formData.status}
                  onChange={(e) => handleInputChange("status", e.target.value)}
                  required
                  size="medium"
                  state={errors.status ? "error" : "none"}
                  stateText={errors.status || ""}
                />
                <InputFieldV2
                  label="Timestamp"
                  placeholder="YYYY-MM-DDTHH:mm:ss.sssZ"
                  value={formData.timestamp}
                  onChange={(e) => handleInputChange("timestamp", e.target.value)}
                  size="medium"
                />
              </div>
            </div>

            {/* Actor Information */}
            <div style={{ marginBottom: "32px", borderTop: "1px solid #e5e5e5", paddingTop: "24px" }}>
              <Text appearance="heading-xs" color="primary-grey-100" style={{ marginBottom: "16px" }}>
                Actor Information
              </Text>
              
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "16px" }}>
                <InputFieldV2
                  label="Actor ID"
                  placeholder="Enter actor ID"
                  value={formData.actor.id}
                  onChange={(e) => handleInputChange("actor.id", e.target.value)}
                  size="medium"
                />
                <InputFieldV2
                  label="Actor Role"
                  placeholder="Enter actor role"
                  value={formData.actor.role}
                  onChange={(e) => handleInputChange("actor.role", e.target.value)}
                  size="medium"
                />
                <InputFieldV2
                  label="Actor Type"
                  placeholder="Enter actor type"
                  value={formData.actor.type}
                  onChange={(e) => handleInputChange("actor.type", e.target.value)}
                  size="medium"
                />
              </div>
            </div>

            {/* Resource Information */}
            <div style={{ marginBottom: "32px", borderTop: "1px solid #e5e5e5", paddingTop: "24px" }}>
              <Text appearance="heading-xs" color="primary-grey-100" style={{ marginBottom: "16px" }}>
                Resource Information
              </Text>
              
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
                <InputFieldV2
                  label="Resource Type"
                  placeholder="Enter resource type"
                  value={formData.resource.type}
                  onChange={(e) => handleInputChange("resource.type", e.target.value)}
                  size="medium"
                />
                <InputFieldV2
                  label="Resource ID"
                  placeholder="Enter resource ID"
                  value={formData.resource.id}
                  onChange={(e) => handleInputChange("resource.id", e.target.value)}
                  size="medium"
                />
              </div>
            </div>

            {/* Additional Information */}
            <div style={{ marginBottom: "32px", borderTop: "1px solid #e5e5e5", paddingTop: "24px" }}>
              <Text appearance="heading-xs" color="primary-grey-100" style={{ marginBottom: "16px" }}>
                Additional Information
              </Text>
              
              <InputFieldV2
                label="Payload Hash"
                placeholder="Enter payload hash"
                value={formData.payloadHash}
                onChange={(e) => handleInputChange("payloadHash", e.target.value)}
                size="medium"
              />
            </div>

            {/* Action Buttons */}
            <div style={{ display: "flex", gap: "12px", justifyContent: "flex-end", marginTop: "32px", borderTop: "1px solid #e5e5e5", paddingTop: "24px" }}>
              <ActionButton
                kind="secondary"
                size="medium"
                label="Cancel"
                onClick={handleCancel}
                disabled={loading}
              />
              <ActionButton
                kind="primary"
                size="medium"
                label={loading ? "Creating..." : "Create Audit Report"}
                type="submit"
                disabled={loading}
              />
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateAudit;

