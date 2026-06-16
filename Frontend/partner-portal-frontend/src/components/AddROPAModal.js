import React, { useState, useEffect } from "react";
import { Text, ActionButton, InputFieldV2 } from "../custom-components";
import { IcClose } from "../custom-components/Icon";
import { useSelector, useDispatch } from "react-redux";
import "../styles/masterSetup.css";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import Select from "react-select";
import {
  fetchBusinessApplications,
  getProcessingActivity,
  getPurposeList,
} from "../store/actions/CommonAction";

const AddROPAModal = ({ onClose, onAdd }) => {
  const dispatch = useDispatch();
  const tenantId = useSelector((state) => state.common.tenant_id);
  const sessionToken = useSelector((state) => state.common.session_token);

  // Master data lists
  const [businessGroups, setBusinessGroups] = useState([]);
  const [processingActivities, setProcessingActivities] = useState([]);
  const [purposesList, setPurposesList] = useState([]);
  const [loading, setLoading] = useState(true);

  // Form state
  const [selectedBusinessGroup, setSelectedBusinessGroup] = useState("");
  const [selectedProcessingActivity, setSelectedProcessingActivity] = useState(null);
  const [selectedPurposes, setSelectedPurposes] = useState([]);

  const [formData, setFormData] = useState({
    businessFunction: "",
    ownerDetails: "",
    processingActivity: "",
    purposeProcessing: "",
    personalDataExample: "",
    personalDataRelating: "",
    categoriesPersonalData: "",
    categoryIndividual: "",
    permittedReason: "",
    additionalConditions: "",
    caseExemption: "",
    dpiaReference: "",
    dpiaReferenceWhere: "",
    businessFunction2: "",
    geographicalLocation: "",
    externalPartiesNames: "",
    contractReference: "",
    crossBorderFlows: "",
    safeguards: "",
    securityDescription: "",
    retentionPeriod: "",
    whereDataStored: "",
    storageLocation: "",
    breachRecords: "N/A",
  });

  // Fetch master data on component mount
  useEffect(() => {
    const fetchMasterData = async () => {
      try {
        setLoading(true);

        // Fetch business groups
        const businessData = await fetchBusinessApplications(
          sessionToken,
          tenantId
        );
        setBusinessGroups(businessData?.searchList || []);

        // Fetch processing activities
        const activitiesRes = await dispatch(getProcessingActivity());
        if (activitiesRes?.status === 200 || activitiesRes?.status === 201) {
          setProcessingActivities(activitiesRes?.data?.searchList || []);
        }

        // Fetch purposes
        const purposesRes = await dispatch(getPurposeList());
        if (purposesRes?.data?.searchList) {
          setPurposesList(purposesRes.data.searchList);
        }

        setLoading(false);
      } catch (error) {
        console.error("Error fetching master data:", error);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message="Error loading master data"
            />
          ),
          { icon: false }
        );
        setLoading(false);
      }
    };

    fetchMasterData();
  }, [dispatch, sessionToken, tenantId]);

  // Handle business group change
  const handleBusinessGroupChange = (e) => {
    const businessGroupName = e.target.value;
    setSelectedBusinessGroup(businessGroupName);
    setFormData((prev) => ({
      ...prev,
      businessFunction: businessGroupName,
    }));

    // Autofill process owner details (placeholder - replace with actual user data)
    const selectedBusiness = businessGroups.find(
      (bg) => bg.name === businessGroupName
    );
    if (selectedBusiness) {
      // This would ideally fetch user details from the business group
      setFormData((prev) => ({
        ...prev,
        ownerDetails: `Process Owner, Tel: +91-XXXXXXXXXX, Email: owner@example.com`,
      }));
    }
  };

  // Handle processing activity change
  const handleProcessingActivityChange = (e) => {
    const activityName = e.target.value;
    const activity = processingActivities.find(
      (pa) => pa.activityName === activityName
    );

    setSelectedProcessingActivity(activity);
    setFormData((prev) => ({
      ...prev,
      processingActivity: activityName,
    }));

    if (activity) {
      // Autofill categories of personal data from dataTypesList
      const personalDataCategories =
        activity.dataTypesList
          ?.map((dt) => dt.dataItems?.join(", "))
          .filter(Boolean)
          .join("; ") || "";

      setFormData((prev) => ({
        ...prev,
        personalDataExample: personalDataCategories,
      }));
    }
  };

  // Handle purpose selection (multiple)
  const handlePurposeChange = (selectedOptions) => {
    setSelectedPurposes(selectedOptions || []);
    
    // Combine multiple purposes
    const purposesText = selectedOptions
      .map((opt) => opt.label)
      .join("; ");

    setFormData((prev) => ({
      ...prev,
      purposeProcessing: purposesText,
    }));
  };

  const handleChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  // Custom styles for react-select
  const customSelectStyles = {
    control: (base, state) => ({
      ...base,
      color: "#333",
      backgroundColor: "#fff",
      border: "1px solid #ccc",
      borderRadius: "8px",
      padding: "2px 4px",
      fontSize: "14px",
      minHeight: "40px",
      boxShadow: "none",
      borderColor: state.isFocused ? "#ccc" : "#ccc",
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
        ? "#e6f0ff"
        : state.isFocused
        ? "#f5f5f5"
        : "#fff",
      color: "#333",
      ":active": {
        backgroundColor: "#e6f0ff",
      },
    }),
  };

  const validateForm = () => {
    const errors = [];

    if (!formData.businessFunction.trim()) {
      errors.push("Business Function / Department is required");
    }
    if (!formData.ownerDetails.trim()) {
      errors.push("Owner Details are required");
    }
    if (!formData.processingActivity.trim()) {
      errors.push("Processing Activity Name is required");
    }
    if (!formData.purposeProcessing.trim()) {
      errors.push("Purpose for Processing is required");
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

    // Create new entry with generated ID
    const newEntry = {
      id: Date.now(),
      ...formData,
    };

    toast.success(
      (props) => (
        <CustomToast
          {...props}
          type="success"
          message="ROPA entry added successfully"
        />
      ),
      { icon: false }
    );

    onAdd(newEntry);
    onClose();
  };

  return (
    <>
      <div className="modal-outer-container">
        <div className="master-set-up-modal-container" style={{ maxWidth: "800px", maxHeight: "85vh", overflowY: "auto" }}>
          <div className="modal-close-btn-container">
            <ActionButton
              onClick={onClose}
              icon={<IcClose />}
              kind="tertiary"
            />
          </div>

          <Text appearance="heading-xs" color="primary-grey-100">
            Add ROPA Entry
          </Text>
          <br />

          <div className="title-margin">
            <Text appearance="heading-xxs" color="primary-grey-100">
              Business Function / Department
            </Text>
          </div>
          <br />

          <Text appearance="body-xs" color="primary-grey-80">
            Business Function / Department (Required)
          </Text>
          <div className="dropdown-group-pa" style={{ marginBottom: "10px" }}>
            <select
              value={selectedBusinessGroup}
              onChange={handleBusinessGroupChange}
              disabled={loading}
            >
              <option value="" disabled>
                {loading ? "Loading..." : "Select Business Group"}
              </option>
              {businessGroups.map((bg) => (
                <option key={bg.businessId} value={bg.name}>
                  {bg.name}
                </option>
              ))}
            </select>
          </div>
          <br />

          <InputFieldV2
            label="Name and Details of the Process Owner (Autofilled)"
            value={formData.ownerDetails}
            onChange={(e) => handleChange("ownerDetails", e.target.value)}
            size="medium"
          />
          <br />

          <Text appearance="body-xs" color="primary-grey-80">
            Name of Processing Activity (Required)
          </Text>
          <div className="dropdown-group-pa" style={{ marginBottom: "10px" }}>
            <select
              value={formData.processingActivity}
              onChange={handleProcessingActivityChange}
              disabled={loading}
            >
              <option value="" disabled>
                {loading ? "Loading..." : "Select Processing Activity"}
              </option>
              {processingActivities.map((pa) => (
                <option key={pa.activityId} value={pa.activityName}>
                  {pa.activityName}
                </option>
              ))}
            </select>
          </div>
          <br />

          <div>
            <Text appearance="body-xs" color="primary-grey-80">
              Purpose for Processing (Multiple - Autofilled)
            </Text>
            <Select
              isMulti
              options={purposesList.map((p) => ({
                value: p.purposeCode,
                label: p.purposeName,
              }))}
              value={selectedPurposes}
              onChange={handlePurposeChange}
              styles={customSelectStyles}
              placeholder="Select purposes..."
              isDisabled={loading}
            />
          </div>
          <br />

          <div className="title-margin">
            <Text appearance="heading-xxs" color="primary-grey-100">
              Categories of Personal Data
            </Text>
          </div>
          <br />

          <div>
            <Text appearance="body-xs" color="primary-grey-80">
              E.g. Name, DoB, Address, Email ID, Contact Number, etc. (Autofilled from Processing Activity)
            </Text>
            <textarea
              placeholder="Categories of personal data will be autofilled from selected processing activity"
              value={formData.personalDataExample}
              onChange={(e) => handleChange("personalDataExample", e.target.value)}
              rows="3"
            />
          </div>
          <br />

          <div>
            <Text appearance="body-xs" color="primary-grey-80">
              Personal Data Relating to (Special Categories)
            </Text>
            <textarea
              placeholder="E.g. Ethnic origin, Children, Health, Religious Creeds, etc."
              value={formData.personalDataRelating}
              onChange={(e) => handleChange("personalDataRelating", e.target.value)}
              rows="3"
            />
          </div>
          <br />

          <div className="title-margin">
            <Text appearance="heading-xxs" color="primary-grey-100">
              Source of Personal Data
            </Text>
          </div>
          <br />

          <InputFieldV2
            label="Categories of Personal Data"
            value={formData.categoriesPersonalData}
            onChange={(e) => handleChange("categoriesPersonalData", e.target.value)}
            size="medium"
          />
          <br />

          <InputFieldV2
            label="Category of Individual"
            value={formData.categoryIndividual}
            onChange={(e) => handleChange("categoryIndividual", e.target.value)}
            size="medium"
            placeholder="E.g. Employees, Customers, Others"
          />
          <br />

          <div className="title-margin">
            <Text appearance="heading-xxs" color="primary-grey-100">
              Permitted Reason for Processing
            </Text>
          </div>
          <br />

          <InputFieldV2
            label="Permitted Reason for Processing"
            value={formData.permittedReason}
            onChange={(e) => handleChange("permittedReason", e.target.value)}
            size="medium"
          />
          <br />

          <div>
            <Text appearance="body-xs" color="primary-grey-80">
              Additional Conditions (if applicable)
            </Text>
            <textarea
              placeholder="Enter additional conditions"
              value={formData.additionalConditions}
              onChange={(e) => handleChange("additionalConditions", e.target.value)}
              rows="2"
            />
          </div>
          <br />

          <InputFieldV2
            label="Case or Purpose for Exemption"
            value={formData.caseExemption}
            onChange={(e) => handleChange("caseExemption", e.target.value)}
            size="medium"
            placeholder="N/A"
          />
          <br />

          <div className="title-margin">
            <Text appearance="heading-xxs" color="primary-grey-100">
              Data Privacy Impact Assessment (DPIA)
            </Text>
          </div>
          <br />

          <InputFieldV2
            label="DPIA Reference"
            value={formData.dpiaReference}
            onChange={(e) => handleChange("dpiaReference", e.target.value)}
            size="medium"
            placeholder="E.g. DPIA0036u"
          />
          <br />

          <InputFieldV2
            label="DPIA Reference (Where Available)"
            value={formData.dpiaReferenceWhere}
            onChange={(e) => handleChange("dpiaReferenceWhere", e.target.value)}
            size="medium"
          />
          <br />

          <div className="title-margin">
            <Text appearance="heading-xxs" color="primary-grey-100">
              Internal & External Parties
            </Text>
          </div>
          <br />

          <InputFieldV2
            label="Business Function / Organisation"
            value={formData.businessFunction2}
            onChange={(e) => handleChange("businessFunction2", e.target.value)}
            size="medium"
          />
          <br />

          <InputFieldV2
            label="Geographical Location"
            value={formData.geographicalLocation}
            onChange={(e) => handleChange("geographicalLocation", e.target.value)}
            size="medium"
            placeholder="E.g. India"
          />
          <br />

          <InputFieldV2
            label="External Parties Names/Categories"
            value={formData.externalPartiesNames}
            onChange={(e) => handleChange("externalPartiesNames", e.target.value)}
            size="medium"
          />
          <br />

          <InputFieldV2
            label="Contract Reference"
            value={formData.contractReference}
            onChange={(e) => handleChange("contractReference", e.target.value)}
            size="medium"
            placeholder="E.g. Contract#XXu"
          />
          <br />

          <InputFieldV2
            label="Cross-Border Data Flows (Yes/No)"
            value={formData.crossBorderFlows}
            onChange={(e) => handleChange("crossBorderFlows", e.target.value)}
            size="medium"
            placeholder="No"
          />
          <br />


          <div className="title-margin">
            <Text appearance="heading-xxs" color="primary-grey-100">
              Security Measures
            </Text>
          </div>
          <br />

          <div>
            <Text appearance="body-xs" color="primary-grey-80">
              Safeguards for Restricted Transfer
            </Text>
            <textarea
              placeholder="Enter safeguards"
              value={formData.safeguards}
              onChange={(e) => handleChange("safeguards", e.target.value)}
              rows="2"
            />
          </div>
          <br />

          <div>
            <Text appearance="body-xs" color="primary-grey-80">
              General Security Description
            </Text>
            <textarea
              placeholder="E.g. Access control, encryption, audits"
              value={formData.securityDescription}
              onChange={(e) => handleChange("securityDescription", e.target.value)}
              rows="3"
            />
          </div>
          <br />

          <div className="title-margin">
            <Text appearance="heading-xxs" color="primary-grey-100">
              Retention & Storage
            </Text>
          </div>
          <br />

          <InputFieldV2
            label="Retention Period"
            value={formData.retentionPeriod}
            onChange={(e) => handleChange("retentionPeriod", e.target.value)}
            size="medium"
            placeholder="E.g. 2 years"
          />
          <br />

          <InputFieldV2
            label="Where the Personal Data is Stored"
            value={formData.whereDataStored}
            onChange={(e) => handleChange("whereDataStored", e.target.value)}
            size="medium"
          />
          <br />

          <InputFieldV2
            label="Storage Location"
            value={formData.storageLocation}
            onChange={(e) => handleChange("storageLocation", e.target.value)}
            size="medium"
            placeholder="E.g. Shared drives, emails, server"
          />
          <br />

          <InputFieldV2
            label="Breach Records"
            value={formData.breachRecords}
            onChange={(e) => handleChange("breachRecords", e.target.value)}
            size="medium"
            placeholder="N/A"
          />
          <br />

          <div className="modal-add-btn-container">
            <ActionButton
              label="Add ROPA Entry"
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

export default AddROPAModal;

