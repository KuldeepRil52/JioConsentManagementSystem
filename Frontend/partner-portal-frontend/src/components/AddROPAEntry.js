import React, { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useSelector, useDispatch } from "react-redux";
import { Text, Button, ActionButton } from '../custom-components';
import Select from "react-select";
import { getDataTypes } from "../store/actions/CommonAction";
import { generateTransactionId } from "../utils/transactionId";
import config from "../utils/config";
import "../styles/pageConfiguration.css";
import "../styles/addROPAEntry.css";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";

const AddROPAEntry = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();
  const tenantId = useSelector((state) => state.common.tenant_id);
  const sessionToken = useSelector((state) => state.common.session_token);
  const businessId = useSelector((state) => state.common.business_id);

  // Check for edit mode and entry data from navigation state
  const editMode = location.state?.editMode || false;
  const entryData = location.state?.entryData || null;
  const ropaId = location.state?.ropaId || null;

  // Form state
  const [formData, setFormData] = useState({
    businessGroup: null,
    processOwner: { name: "", mobile: "", email: "" }, // Store as object
    processingActivity: null,
    purpose: null,
    categoriesPersonalData: [], // Changed to array for multi-select
    categoriesIndividual: "",
    permittedReason: "",
    additionalConditions: "",
    safeguards: "",
    securityDescription: "",
    retentionPeriod: "",
    storageLocation: "",
    storageDetails: "",
    breachRecords: "",
  });

  // Master data state
  const [businessGroups, setBusinessGroups] = useState([]);
  const [processingActivities, setProcessingActivities] = useState([]);
  const [purposes, setPurposes] = useState([]);
  const [dataCategoryOptions, setDataCategoryOptions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadingCategories, setLoadingCategories] = useState(false);
  const [saving, setSaving] = useState(false);

  // Fetch master data on component mount
  useEffect(() => {
    fetchMasterData();
  }, [tenantId, sessionToken, businessId]);

  // Pre-fill form when in edit mode (after master data is loaded)
  useEffect(() => {
    if (!editMode || !entryData) return;

    console.log("Prefilling form with entry data:", entryData);

    // ── Helpers ──
    const findOption = (options, value) =>
      options.find((opt) => opt.value === value) || null;

    const findOptionByLabel = (options, label) => {
      if (!label) return null;
      const trimmed = label.trim().toLowerCase();
      return options.find((opt) => opt.label?.trim().toLowerCase() === trimmed) || null;
    };

    // ── Business Function / Department ──
    const businessFunctionValue =
      entryData.processOverview?.businessFunction ||
      entryData.processOverview?.department ||
      entryData.businessFunction ||
      entryData.department ||
      "";

    let businessGroupOption = null;
    if (businessFunctionValue && businessGroups.length > 0) {
      businessGroupOption =
        findOption(businessGroups, businessFunctionValue) ||
        findOptionByLabel(businessGroups, businessFunctionValue);
    }
    if (!businessGroupOption && businessFunctionValue) {
      businessGroupOption = { value: businessFunctionValue, label: businessFunctionValue };
    }

    // ── Processing Activity ── (needs processingActivities loaded)
    const processingActivityId =
      entryData.processingActivityId ||
      entryData.processOverview?.processingActivityId ||
      entryData.processOverview?.processingActivity?.activityId ||
      entryData.processOverview?.processingActivity?.id ||
      "";

    const processingActivityName =
      entryData.processingActivityName ||
      entryData.processOverview?.processingActivityName ||
      "";

    let processingActivity = null;
    if (processingActivities.length > 0) {
      if (processingActivityId) {
        processingActivity = findOption(processingActivities, processingActivityId);
      }
      if (!processingActivity && processingActivityName) {
        processingActivity = findOptionByLabel(processingActivities, processingActivityName);
      }
    }
    if (!processingActivity && (processingActivityName || processingActivityId)) {
      processingActivity = {
        value: processingActivityId || processingActivityName,
        label: processingActivityName || processingActivityId,
      };
    }

    // ── Purpose of Processing ── (needs purposes loaded)
    const purposeId =
      entryData.purposeForProcessingId ||
      entryData.processOverview?.purposeForProcessingId ||
      entryData.processOverview?.purpose?.purposeId ||
      entryData.processOverview?.purpose?.id ||
      "";

    const purposeName =
      entryData.purposeForProcessing ||
      entryData.processOverview?.purposeForProcessing ||
      "";

    let purpose = null;
    if (purposes.length > 0) {
      if (purposeId) {
        purpose = findOption(purposes, purposeId);
      }
      if (!purpose && purposeName) {
        purpose = findOptionByLabel(purposes, purposeName);
      }
    }
    if (!purpose && (purposeName || purposeId)) {
      purpose = {
        value: purposeId || purposeName,
        label: purposeName || purposeId,
      };
    }

    // ── Data Categories ── (sourceOfPersonalData)
    const sourceData = entryData.sourceOfPersonalData || [];
    const dataCategories = sourceData.map((cat) => {
      const found = dataCategoryOptions.find(
        (opt) => opt.value === cat || opt.value?.toLowerCase() === cat?.toLowerCase()
      );
      return found || { value: cat, label: cat };
    });

    // ── Retention Period ──
    const retentionRaw = entryData.retentionPeriod;
    let retentionPeriodStr = "";
    if (retentionRaw) {
      if (typeof retentionRaw === "string") {
        // Already a string (e.g. "2 years") — use as-is
        retentionPeriodStr = retentionRaw;
      } else if (typeof retentionRaw === "object" && retentionRaw.value != null && retentionRaw.unit) {
        retentionPeriodStr = `${retentionRaw.value} ${retentionRaw.unit.toLowerCase()}`;
      }
    }

    // ── Categories of Individual / Special Nature ──
    const categoriesIndividualArr =
      entryData.categoriesOfSpecialNature ||
      entryData.categoryOfIndividual ||
      [];
    const categoriesIndividualStr = Array.isArray(categoriesIndividualArr)
      ? categoriesIndividualArr.join(", ")
      : String(categoriesIndividualArr || "");

    // ── Process Owner ──
    const processOwnerData =
      entryData.processOverview?.processOwner ||
      entryData.processOwner ||
      {};
    const processOwner = {
      name: processOwnerData.name || "",
      mobile: processOwnerData.mobile || "",
      email: processOwnerData.email || "",
    };

    setFormData({
      businessGroup: businessGroupOption,
      processOwner: processOwner,
      processingActivity: processingActivity,
      purpose: purpose,
      categoriesPersonalData: dataCategories,
      categoriesIndividual: categoriesIndividualStr,
      permittedReason: entryData.activityReason || "",
      additionalConditions: entryData.additionalCondition || "",
      safeguards: entryData.administrativePrecautions || "",
      securityDescription: entryData.technicalPrecautions || "",
      retentionPeriod: retentionPeriodStr,
      storageLocation: entryData.storageLocation || "",
      storageDetails: entryData.storageLocation || "",
      breachRecords: entryData.breachDocumentation || "",
    });
  }, [editMode, entryData, processingActivities, purposes, dataCategoryOptions, businessGroups]);

  // Fetch data categories from PII master data
  useEffect(() => {
    const fetchDataCategories = async () => {
      try {
        setLoadingCategories(true);
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
        setLoadingCategories(false);
      }
    };

    if (tenantId && sessionToken) {
      fetchDataCategories();
    }
  }, [dispatch, tenantId, sessionToken]);

  const fetchMasterData = async () => {
    setLoading(true);
    try {
      // Fetch business groups, processing activities, and purposes in parallel
      await Promise.all([
        fetchBusinessGroups(),
        fetchProcessingActivities(),
        fetchPurposes(),
      ]);
    } catch (error) {
      console.error("Error fetching master data:", error);
    } finally {
      setLoading(false);
    }
  };

  const fetchBusinessGroups = async () => {
    try {
      const txnId = generateTransactionId();

      const response = await fetch(
        config.business_application_search,
        {
          method: "GET",
          headers: {
            accept: "application/json",
            txn: txnId,
            "tenant-id": tenantId,
            "x-session-token": sessionToken,
          },
        }
      );

      if (response.ok) {
        const data = await response.json();
        const options = data?.searchList?.map((item) => ({
          value: item.businessId,
          label: item.name || item.businessId,
        })) || [];
        setBusinessGroups(options);
      }
    } catch (error) {
      console.error("Error fetching business groups:", error);
    }
  };

  const fetchProcessingActivities = async () => {
    try {
      const txnId = generateTransactionId();

      const response = await fetch(
        config.search_processingActivity,
        {
          method: "GET",
          headers: {
            txn: txnId,
            "tenant-id": tenantId,
            "x-session-token": sessionToken,
            Accept: "application/json",
          },
        }
      );

      if (response.ok) {
        const data = await response.json();
        const options = data?.searchList?.map((item) => ({
          value: item.activityId,
          label: item.activityName || item.activityId,
        })) || [];
        setProcessingActivities(options);
      }
    } catch (error) {
      console.error("Error fetching processing activities:", error);
    }
  };

  const fetchPurposes = async () => {
    try {
      const txnId = generateTransactionId();

      const response = await fetch(
        config.search_purpose,
        {
          method: "GET",
          headers: {
            txn: txnId,
            "business-id": businessId,
            "scope-level": "INDIVIDUAL",
            "tenant-id": tenantId,
            "x-session-token": sessionToken,
            "Content-Type": "application/json",
            Accept: "application/json",
          },
        }
      );

      if (response.ok) {
        const data = await response.json();
        const options = data?.searchList?.map((item) => ({
          value: item.purposeId,
          label: item.purposeName || item.purposeId,
        })) || [];
        setPurposes(options);
      }
    } catch (error) {
      console.error("Error fetching purposes:", error);
    }
  };

  const handleBack = () => {
    navigate("/ropa");
  };

  const handleInputChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleSubmit = async () => {
    // Validation
    if (!formData.businessGroup || !formData.processingActivity || !formData.purpose) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please fill all required fields"}
          />
        ),
        { icon: false }
      );
      return;
    }

    setSaving(true);
    try {
      const txnId = generateTransactionId();

      // Parse retention period (e.g., "2 yrs" -> {value: 2, unit: "YEARS"})
      const parseRetentionPeriod = (period) => {
        if (!period) return { value: 0, unit: "YEARS" };

        const match = period.match(/(\d+)\s*(yr|year|month|day|week)/i);
        if (match) {
          const value = parseInt(match[1], 10);
          const unitText = match[2].toLowerCase();
          let unit = "YEARS";

          if (unitText.startsWith("yr") || unitText.startsWith("year")) {
            unit = "YEARS";
          } else if (unitText.startsWith("month")) {
            unit = "MONTHS";
          } else if (unitText.startsWith("day")) {
            unit = "DAYS";
          } else if (unitText.startsWith("week")) {
            unit = "WEEKS";
          }

          return { value, unit };
        }

        // If only a number is entered, default to YEARS
        const numOnly = period.match(/^(\d+)$/);
        if (numOnly) {
          return { value: parseInt(numOnly[1], 10), unit: "YEARS" };
        }

        // Try to extract any number + text combo (e.g. "6 mths", "3yr")
        const fuzzy = period.match(/(\d+)\s*(\w+)/i);
        if (fuzzy) {
          const value = parseInt(fuzzy[1], 10);
          const unitText = fuzzy[2].toLowerCase();
          if (unitText.startsWith("m")) return { value, unit: "MONTHS" };
          if (unitText.startsWith("d")) return { value, unit: "DAYS" };
          if (unitText.startsWith("w")) return { value, unit: "WEEKS" };
          return { value, unit: "YEARS" };
        }

        return { value: 0, unit: "YEARS" };
      };

      // Build processOwner object
      const processOwnerObj = {
        name: formData.processOwner?.name || "",
        mobile: formData.processOwner?.mobile || "",
        email: formData.processOwner?.email || "",
      };

      // Build processOverview to match API structure
      const processOverview = {
        businessFunction: formData.businessGroup?.label || "",
        department: formData.businessGroup?.label || "",
        processingActivityId: formData.processingActivity?.value || "",
        processingActivityName: formData.processingActivity?.label || "",
        purposeForProcessingId: formData.purpose?.value || "",
        purposeForProcessing: formData.purpose?.label || "",
        processOwner: processOwnerObj,
      };

      const requestBody = {
        // Send processOverview as a wrapper (API stores it this way)
        processOverview: processOverview,
        // Also send flat fields for backend compatibility
        processingActivityId: formData.processingActivity?.value || "",
        processingActivityName: formData.processingActivity?.label || "",
        purposeForProcessingId: formData.purpose?.value || "",
        purposeForProcessing: formData.purpose?.label || "",
        businessFunction: formData.businessGroup?.label || "",
        department: formData.businessGroup?.label || "",
        businessId: formData.businessGroup?.value || businessId,
        processOwner: processOwnerObj,
        categoriesOfSpecialNature: formData.categoriesIndividual
          ? formData.categoriesIndividual.split(',').map(item => item.trim()).filter(item => item)
          : [],
        sourceOfPersonalData: formData.categoriesPersonalData && Array.isArray(formData.categoriesPersonalData)
          ? formData.categoriesPersonalData.map(cat => cat.value)
          : [],
        categoryOfIndividual: formData.categoriesIndividual
          ? formData.categoriesIndividual.split(',').map(item => item.trim()).filter(item => item)
          : [],
        activityReason: formData.permittedReason || "",
        additionalCondition: formData.additionalConditions || "",
        caseOrPurposeForExemption: "N/A",
        dpiaReference: "",
        linkOrDocumentRef: null,
        businessFunctionsSharedWith: [],
        geographicalLocations: [],
        thirdPartiesSharedWith: [],
        contractReferences: [],
        crossBorderFlow: false,
        restrictedTransferSafeguards: "N/A",
        administrativePrecautions: formData.safeguards || "",
        financialPrecautions: "",
        technicalPrecautions: formData.securityDescription || "",
        retentionPeriod: parseRetentionPeriod(formData.retentionPeriod),
        storageLocation: formData.storageLocation || formData.storageDetails || "",
        breachDocumentation: formData.breachRecords || "N/A",
        lastBreachDate: null,
        breachSummary: null,
      };

      // For edit mode, include additional fields if they exist in entryData
      if (editMode && entryData) {
        // Preserve ropaId for updates
        if (entryData.ropaId) {
          requestBody.ropaId = entryData.ropaId;
        }
        // Preserve existing values that might not be in the form
        if (entryData.businessFunctionsSharedWith) {
          requestBody.businessFunctionsSharedWith = entryData.businessFunctionsSharedWith;
        }
        if (entryData.geographicalLocations) {
          requestBody.geographicalLocations = entryData.geographicalLocations;
        }
        if (entryData.thirdPartiesSharedWith) {
          requestBody.thirdPartiesSharedWith = entryData.thirdPartiesSharedWith;
        }
        if (entryData.contractReferences) {
          requestBody.contractReferences = entryData.contractReferences;
        }
        if (entryData.crossBorderFlow !== undefined) {
          requestBody.crossBorderFlow = entryData.crossBorderFlow;
        }
        if (entryData.restrictedTransferSafeguards) {
          requestBody.restrictedTransferSafeguards = entryData.restrictedTransferSafeguards;
        }
        if (entryData.financialPrecautions) {
          requestBody.financialPrecautions = entryData.financialPrecautions;
        }
        if (entryData.dpiaReference) {
          requestBody.dpiaReference = entryData.dpiaReference;
        }
        if (entryData.caseOrPurposeForExemption) {
          requestBody.caseOrPurposeForExemption = entryData.caseOrPurposeForExemption;
        }
        if (entryData.lastBreachDate) {
          requestBody.lastBreachDate = entryData.lastBreachDate;
        }
        if (entryData.breachSummary !== undefined) {
          requestBody.breachSummary = entryData.breachSummary;
        }
        // Preserve status
        if (entryData.status) {
          requestBody.status = entryData.status;
        }
      }

      console.log("Sending ROPA entry to API:", JSON.stringify(requestBody, null, 2));

      // Use PUT for edit mode, POST for create mode
      const url = editMode && ropaId
        ? `${config.ropa_update}/${ropaId}`
        : config.ropa_create;
      const method = editMode && ropaId ? "PUT" : "POST";

      const response = await fetch(
        url,
        {
          method: method,
          headers: {
            "Content-Type": "application/json",
            accept: "application/json",
            txn: txnId,
            "tenant-id": tenantId,
            "business-id": businessId,
            "x-session-token": sessionToken,
          },
          body: JSON.stringify(requestBody),
        }
      );

      if (response.ok) {
        const data = await response.json();
        console.log(`ROPA entry ${editMode ? 'updated' : 'created'} successfully:`, data);
        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message={editMode ? "ROPA entry updated successfully" : "ROPA entry added successfully"}
            />
          ),
          { icon: false }
        );
        // Navigate after a short delay to show the toast
        setTimeout(() => {
          navigate("/ropa");
        }, 1500);
      } else {
        const errorData = await response.json().catch(() => ({}));
        console.error("API Error:", errorData);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={errorData.message || `Failed to ${editMode ? 'update' : 'add'} ROPA entry: ${response.statusText}`}
            />
          ),
          { icon: false }
        );
      }
    } catch (error) {
      console.error(`Error ${editMode ? 'updating' : 'adding'} ROPA entry:`, error);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={`An error occurred while ${editMode ? 'updating' : 'adding'} the ROPA entry. Please try again later.`}
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
        <div className="add-ropa-entry-container">
          <div className="add-ropa-entry-header">
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
              {editMode ? 'Edit ROPA entry' : 'Add ROPA entry'}
            </h1>
          </div>

          <div className="add-ropa-entry-content">
            {/* Business function/department Section */}
            <div className="form-section">
              <h2 className="section-heading">
                Business function/department
              </h2>

              <div className="form-field">
                <label>
                  Business function/department (Required)
                </label>
                <Select
                  options={
                    formData.businessGroup &&
                      editMode &&
                      entryData &&
                      !businessGroups.some(
                        (o) =>
                          o.value === formData.businessGroup?.value ||
                          o.label === formData.businessGroup?.label
                      )
                      ? [formData.businessGroup, ...businessGroups]
                      : businessGroups
                  }
                  value={formData.businessGroup}
                  onChange={(value) => handleInputChange("businessGroup", value)}
                  placeholder="Select business group"
                  styles={customSelectStyles}
                  isLoading={loading}
                />
              </div>

              <div className="form-field">
                <label>Process Owner (DPO) - Name</label>
                <input
                  type="text"
                  value={formData.processOwner.name}
                  onChange={(e) => handleInputChange("processOwner", { ...formData.processOwner, name: e.target.value })}
                  placeholder="Enter process owner name"
                />
              </div>

              <div className="form-field">
                <label>Process Owner (DPO) - Mobile</label>
                <input
                  type="text"
                  value={formData.processOwner.mobile}
                  onChange={(e) => handleInputChange("processOwner", { ...formData.processOwner, mobile: e.target.value })}
                  placeholder="Enter mobile number"
                />
              </div>

              <div className="form-field">
                <label>Process Owner (DPO) - Email</label>
                <input
                  type="email"
                  value={formData.processOwner.email}
                  onChange={(e) => handleInputChange("processOwner", { ...formData.processOwner, email: e.target.value })}
                  placeholder="Enter email address"
                />
              </div>

              <div className="form-field">
                <label>Name of processing activity (Required)</label>
                <Select
                  options={
                    formData.processingActivity &&
                      editMode &&
                      entryData &&
                      !processingActivities.some(
                        (o) =>
                          o.value === formData.processingActivity?.value ||
                          o.label === formData.processingActivity?.label
                      )
                      ? [formData.processingActivity, ...processingActivities]
                      : processingActivities
                  }
                  value={formData.processingActivity}
                  onChange={(value) => handleInputChange("processingActivity", value)}
                  placeholder="Select processing activity"
                  styles={customSelectStyles}
                  isLoading={loading}
                />
              </div>

              <div className="form-field">
                <label>Purpose of processing (Required)</label>
                <Select
                  options={
                    formData.purpose &&
                      editMode &&
                      entryData &&
                      !purposes.some(
                        (o) =>
                          o.value === formData.purpose?.value ||
                          o.label === formData.purpose?.label
                      )
                      ? [formData.purpose, ...purposes]
                      : purposes
                  }
                  value={formData.purpose}
                  onChange={(value) => handleInputChange("purpose", value)}
                  placeholder="Select purposes"
                  styles={customSelectStyles}
                  isLoading={loading}
                />
              </div>
            </div>

            {/* Source of personal data Section */}
            <div className="form-section">
              <h2 className="section-heading">
                Source of personal data
              </h2>

              <div className="form-field">
                <label>Categories of personal data</label>
                <Select
                  isMulti
                  options={(() => {
                    if (!editMode || !entryData || !formData.categoriesPersonalData?.length) {
                      return dataCategoryOptions;
                    }
                    const missingOptions = formData.categoriesPersonalData.filter(
                      (sel) => !dataCategoryOptions.some((o) => o.value === sel.value)
                    );
                    return missingOptions.length > 0
                      ? [...missingOptions, ...dataCategoryOptions]
                      : dataCategoryOptions;
                  })()}
                  value={formData.categoriesPersonalData}
                  onChange={(value) => handleInputChange("categoriesPersonalData", value)}
                  placeholder={loadingCategories ? "Loading data categories..." : "Select data categories"}
                  styles={customSelectStyles}
                  isLoading={loadingCategories}
                  isDisabled={loadingCategories}
                />
              </div>

              <div className="form-field">
                <label>Categories of indiviual</label>
                <input
                  type="text"
                  value={formData.categoriesIndividual}
                  onChange={(e) => handleInputChange("categoriesIndividual", e.target.value)}
                  placeholder="E.g. ethinic origin, children, health etc"
                />
              </div>
            </div>

            {/* Permitted reason for processing Section */}
            <div className="form-section">
              <h2 className="section-heading">
                Permitted reason for processing
              </h2>

              <div className="form-field">
                <label>Permitted reason for processing</label>
                <input
                  type="text"
                  value={formData.permittedReason}
                  onChange={(e) => handleInputChange("permittedReason", e.target.value)}
                  placeholder=""
                />
              </div>

              <div className="form-field">
                <label>Additional conditions (If applicable)</label>
                <input
                  type="text"
                  value={formData.additionalConditions}
                  onChange={(e) => handleInputChange("additionalConditions", e.target.value)}
                  placeholder=""
                />
              </div>
            </div>

            {/* Security measures Section */}
            <div className="form-section">
              <h2 className="section-heading">
                Security measures
              </h2>

              <div className="form-field">
                <label>Safeguards for security measure</label>
                <textarea
                  value={formData.safeguards}
                  onChange={(e) => handleInputChange("safeguards", e.target.value)}
                  placeholder="Enter safeguards"
                  rows={4}
                />
              </div>

              <div className="form-field">
                <label>General security description</label>
                <textarea
                  value={formData.securityDescription}
                  onChange={(e) => handleInputChange("securityDescription", e.target.value)}
                  placeholder="E.g access control, encryption, audits"
                  rows={4}
                />
              </div>
            </div>

            {/* Retention and storage Section */}
            <div className="form-section">
              <h2 className="section-heading">
                Retention and storage
              </h2>

              <div className="form-field">
                <label>Retention period</label>
                <input
                  type="text"
                  value={formData.retentionPeriod}
                  onChange={(e) => handleInputChange("retentionPeriod", e.target.value)}
                  placeholder="E.g 2 yrs"
                />
              </div>

              <div className="form-field">
                <label>Where the personal data is stored</label>
                <input
                  type="text"
                  value={formData.storageLocation}
                  onChange={(e) => handleInputChange("storageLocation", e.target.value)}
                  placeholder=""
                />
              </div>

              <div className="form-field">
                <label>Storage location</label>
                <input
                  type="text"
                  value={formData.storageDetails}
                  onChange={(e) => handleInputChange("storageDetails", e.target.value)}
                  placeholder="E.g shared drives, emails, server"
                />
              </div>

              <div className="form-field">
                <label>Breach records</label>
                <input
                  type="text"
                  value={formData.breachRecords}
                  onChange={(e) => handleInputChange("breachRecords", e.target.value)}
                  placeholder=""
                />
              </div>
            </div>

            {/* Submit Button */}
            <div className="form-actions">
              <ActionButton
                kind="primary"
                size="large"
                state="normal"
                label={saving ? (editMode ? "Updating..." : "Adding...") : (editMode ? "Update entry" : "Add entry")}
                onClick={handleSubmit}
                disabled={saving}
              />
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

export default AddROPAEntry;

