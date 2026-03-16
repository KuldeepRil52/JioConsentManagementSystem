import React, { useState, useEffect, useMemo } from "react";
import "../Styles/createRequest.css";
import { ActionButton, Icon, Image, Text, Button } from "@jds/core";
import "../Styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { useRef } from "react";
import { IcLanguage } from "@jds/extended-icons";
import "../Styles/loader.css";
import { IcChevronDown, IcUpload, IcSuccess, IcClose } from "@jds/core-icons";
import {
  createGrievanceRequest,
  createIntegrationGrievanceRequest,
  getGrievnaceTemplateData,
  getIntegrationGrievnaceTemplateData,
} from "../store/actions/CommonAction";
import useTranslation from "../hooks/useTranslation";

const IntegrationCreateRequest = () => {
  const logos = new URL("../Assets/popup.svg", import.meta.url).href;

  const navigate = useNavigate();
  const dispatch = useDispatch();

  const [mobile, setMobile] = useState(true);
  const [title, setTitle] = useState("Register your grievance");
  const [purposeHeading, setPurposeHeading] = useState("Purpose: ");
  const [processingHeading, setProcessingHeading] = useState(
    "Processing activity: "
  );
  const [usedByHeading, setUsedByHeading] = useState("Used By: ");
  const [durationHeading, setDurationHeading] = useState("Duration: ");
  const [dataItemHeading, setDataItemHeading] = useState("Data item: ");
  const [selectedType, setSelectedType] = useState("");
  const [selectedGrievanceType, setSelectedGrievanceType] = useState("");
  const [selectedDetail, setSelectedDetail] = useState("");
  const [grievanceFormTemplate, setGrievanceFormTemplate] = useState({});

  // Translation inputs for static text
  const translationInputs = useMemo(
    () => [
      { id: "int_create_title", source: "Register your grievance" },
      { id: "int_create_grievance_details", source: "Grievance Details" },
      { id: "int_create_select_category", source: "Select Category" },
      { id: "int_create_grievance_category", source: "Grievance Category" },
      { id: "int_create_grievance_subcategory", source: "Grievance Subcategory" },
      { id: "int_create_enter_subcategory", source: "Enter Grievance Subcategory" },
      { id: "int_create_description", source: "Description (Required)" },
      { id: "int_create_description_placeholder", source: "Enter description of the group's function or scope." },
      { id: "int_create_user_details", source: "User Details" },
      { id: "int_create_user_type", source: "User Type" },
      { id: "int_create_select_user_type", source: "Select user type" },
      { id: "int_create_upload_files", source: "Upload Files" },
      { id: "int_create_upload", source: "Upload" },
      { id: "int_create_upload_instruction", source: "Drag and drop or upload a document with hyperlink with the consent banner in .pdf or .doc format." },
      { id: "int_create_required", source: "Required" },
      { id: "int_create_enter", source: "Enter" },
      { id: "int_create_submit_request", source: "Submit Request" },
    ],
    []
  );

  // Use translation hook
  const { getTranslation, translateContent, currentLanguage } =
    useTranslation(translationInputs);

  // Translate content when language changes
  useEffect(() => {
    if (currentLanguage !== "ENGLISH") {
      translateContent(translationInputs, currentLanguage);
    }
  }, [currentLanguage, translationInputs, translateContent]);
  let tenantId = "";
  let businessId = "";
  let grievanceTemplateId = "";
  let secureCode = "";
  const params = new URLSearchParams(window.location.search);
  const encodedData = params.get("data");
  if (encodedData) {
    try {
      const decoded = atob(encodedData);
      const parsed = JSON.parse(decoded);

      tenantId = parsed.tenantId || "";
      businessId = parsed.businessId || "";
      grievanceTemplateId = parsed.grievanceTemplateId || "";
      secureCode = parsed.secureCode || "";
    } catch (err) {
      console.log("Error decoding Base64 data param:", err);
    }
  }
  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await dispatch(
          getIntegrationGrievnaceTemplateData(
            tenantId,
            businessId,
            grievanceTemplateId,
            secureCode
          )
        );
        if (response.status === 200) {
          setGrievanceFormTemplate(response.data);

          const data = response.data;

          const supportedLanguages = data?.multilingual?.supportedLanguages;

          const selectedLanguage = supportedLanguages?.[0];

          let title = "";
          let text = "";

          if (selectedLanguage && data?.languages) {
            // Find the entry in languages where key corresponds to that language
            const matchingLanguageEntry = Object.values(data.languages).find(
              (lang) =>
                lang.language?.toUpperCase() ===
                  selectedLanguage.toUpperCase() ||
                lang.heading
                  ?.toUpperCase()
                  .includes(selectedLanguage.toUpperCase())
            );

            if (matchingLanguageEntry) {
              title = matchingLanguageEntry.heading || "";
              text = matchingLanguageEntry.description || "";
            }
          }

          setTitle(title);
          setText(text);
          setLogoPreviewUrl(data?.uiConfig.logo || logos);
        }
      } catch (err) {
        console.log("Error occured in getConsentsByTemplateId", err);
        if (err?.[0]?.errorCode === "JCMP3001") {
          grievanceFormTemplate({});
        }
      } finally {
        // setLoadLogs(false);
      }
    };

    fetchData();
  }, []);

  const [grievanceDesc, setGrievanceDesc] = useState("");
  // for upload component
  const uploadFileInputRef = useRef(null);
  const [uploadFileName, setUploadFileName] = useState("");
  const [uploadFileBase64, setUploadFileBase64] = useState("");
  const [upoladFilePreviewUrl, setUploadFilePreviewUrl] = useState("");
  const [selectedUserType, setSelectedUserType] = useState("");
  const [userDetailInputs, setUserDetailInputs] = useState({});

  // for translation
  const [grievanceDetailsHeading, setGrievanceDetailsHeading] =
    useState("Grievance Details");
  const [selectCategoryPlaceholder, setSelectCategoryPlaceholder] =
    useState("Select Category");

  const [grievanceCategoryLabel, setGrievanceCategoryLabel] =
    useState("Grievance Category");
  const [grievanceSubCategoryLabel, setGrievanceSubCategoryLabel] = useState(
    "Grievance Subcategory"
  );
  const [text, setText] = useState(
    "While using JioMeet, your activities create data which will be used with your consent to offer customised services. Details of data usage are provided below. "
  );
  const [subCategoryPlaceholder, setSubCategoryPlaceholder] = useState(
    "Enter Grievance Subcategory"
  );
  const [descriptionLabel, setDescriptionLabel] = useState(
    "Description (Required)"
  );
  const [descriptionPlaceholder, setDescriptionPlaceholder] = useState(
    "Enter description of the group's function or scope."
  );
  const [userDetailsHeading, setUserDetailsHeading] = useState("User Details");
  const [userTypeLabel, setUserTypeLabel] = useState("User Type");
  const [userTypePlaceholder, setUserTypePlaceholder] =
    useState("Select user type");
  const [uploadFilesHeading, setUploadFilesHeading] = useState("Upload Files");
  const [uploadButtonText, setUploadButtonText] = useState("Upload");
  const [uploadInstruction, setUploadInstruction] = useState(
    "Drag and drop or upload a document with hyperlink with the consent banner in .pdf or .doc format."
  );
  const [requiredLable, setRequiredLable] = useState("Required");
  const [enterLable, setEnterLable] = useState("Enter");

  const handleTextAreaChange = (e) => {
    setGrievanceDesc(e.target.value);
  };

  const toTitleCase = (str) => {
    return str.toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase());
  };
  const [purposes, setPurposes] = useState([
    {
      purposeIds: [],
      purposeNames: [],
      isMandatory: "",
      autoRenew: "",
      preferenceValidity: { value: 1, unit: "" },
      purposeActivityIds: [],
      usedBy: [],
      dataItems: [],
      processingAct: [],
    },
  ]);

  // upoad component
  const handleUploadFilesClick = () => {
    uploadFileInputRef.current?.click();
  };

  const handleUploadFilesDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleUploadFilesDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleUploadFilesDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    const file = e.dataTransfer.files[0];
    if (file) processUploadFile(file);
  };

  const handleUploadFilieChange = (e) => {
    const file = e.target.files[0];
    if (file) processUploadFile(file);
  };

  const processUploadFile = (file) => {
    setUploadFileName(file.name);

    const reader = new FileReader();
    reader.onloadend = () => {
      const base64String = reader.result.split(",")[1];
      setUploadFileBase64(base64String);
      setUploadFilePreviewUrl(reader.result);
    };
    reader.readAsDataURL(file);
  };

  const handleRemoveUploadFile = () => {
    setUploadFileName("");
    setUploadFileBase64("");
    setUploadFilePreviewUrl("");
    if (uploadFileInputRef.current) uploadFileInputRef.current.value = "";
  };

  const handlePreviewUploadFile = () => {
    if (upoladFilePreviewUrl) {
      window.open(upoladFilePreviewUrl, "_blank");
    }
  };

  // Custom style for select
  const customStyles = {
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

  //language container states
  const AccordionItem = ({ title, alwaysActive, hasCheckbox, items }) => {
    const [isOpen, setIsOpen] = useState(true);

    return (
      <div
        className="accordion-item"
        style={{
          backgroundColor: activePopupColors.cardBackground,
          color: activePopupColors.cardFont,
        }}
      >
        {/* Header */}
        <div className="accordion-header" onClick={() => setIsOpen(!isOpen)}>
          <div className="accordion-left">
            <span className={`accordion-icon ${isOpen ? "open" : ""}`}>
              <IcChevronDown height={25} width={25} />
            </span>
            <span className="accordion-title">{title}</span>
          </div>

          {alwaysActive && (
            <span className="accordion-label">Always Active</span>
          )}
          {hasCheckbox && <input type="checkbox" />}
        </div>

        {/* Content */}
        {isOpen && (
          <div
            className="accordion-content"
            style={{
              backgroundColor: activePopupColors.cardBackground,
              color: activePopupColors.cardFont,
            }}
          >
            {items.map((item, idx) => (
              <div key={idx} className="accordion-block">
                {item.purposeNames?.length > 0 ? (
                  <p>
                    <strong>{purposeHeading}</strong>{" "}
                    {item.purposeNames.join(", ")}
                  </p>
                ) : (
                  <p>
                    <strong>{purposeHeading}</strong> Not provided
                  </p>
                )}

                {validitytoBeShown && (
                  <p>
                    <strong>{durationHeading}</strong>{" "}
                    {item.preferenceValidity?.value
                      ? `${item.preferenceValidity.value} ${toTitleCase(
                          item.preferenceValidity.value === 1
                            ? item.preferenceValidity.unit.replace(/s$/i, "") // singular
                            : item.preferenceValidity.unit // plural
                        )}`
                      : "N/A"}
                  </p>
                )}

                {item.usedBy?.length > 0 && processorNameToBeShown && (
                  <p>
                    <strong>{usedByHeading}</strong> {item.usedBy.join(", ")}
                  </p>
                )}

                {processActivityNameToBeShown && (
                  <p>
                    <strong>{processingHeading}</strong>{" "}
                    {item.processingAct?.join(", ")}
                  </p>
                )}

                {dataItemToBeShown && (
                  <p>
                    <strong>{dataItemHeading}</strong>{" "}
                    {item.dataItems?.join(", ")}
                  </p>
                )}

                {/*  Correct usage of items.length */}
                {idx !== items.length - 1 && (
                  <hr className="accordion-separator" />
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    );
  };

  const [rights, setRights] = useState(
    "To withdraw your consent, exercise your rights, or file complaints with the Board click here."
  );
  const [permission, setPermission] = useState(
    "By clicking ‘Allow all’ or ’Save my choices’, you are providing your consent to Reliance Medlab using your data as outlined above."
  );
  const [supportedLanguages, setSupportedLanguages] = useState([]);
  const [language, setLanguage] = useState("");
  const [label, setLabel] = useState("Required");
  const [languageSpecificContentMap, setLanguageSpecificContentMap] = useState({
    ENGLISH: {
      description: text,
      heading: "Register your grievance",
    },
  });

  //purpose container states
  const [purposeItems, setPurposeItems] = useState([0]);
  const [counter, setCounter] = useState(1);
  const [processingActivityList, setProcessingActivityList] = useState([]);
  const [activityOptions, setActivityOptions] = useState([]);
  const [purposeOptions, setPurposeOptions] = useState([]);
  const [processingActivityOptions, setProcessingActivityOptions] = useState(
    []
  );
  const [purposeList, setPurposeList] = useState([]);

  //branding states

  const [fileName, setFileName] = useState("");
  const [certificatePreviewUrl, setCertificatePreviewUrl] = useState("");
  const fileInputRef = useRef(null);
  const [fileBase64, setFileBase64] = useState(null);
  const [logoPreviewUrl, setLogoPreviewUrl] = useState("");
  const [logoName, setLogoName] = useState("");
  const logoInputRef = useRef(null);
  const [logoBase64, setLogoBase64] = useState("");
  const [fileSize, setFileSize] = useState(0);
  const [darkMode, setDarkMode] = useState(false);
  const [mobileView, setMobileView] = useState(true);
  const [parentalControl, setParentalControl] = useState(true);
  const [dataTypeToBeShown, setDataTypeToBeShown] = useState(true);
  const [dataItemToBeShown, setDataItemToBeShown] = useState(true);
  const [processActivityNameToBeShown, setProcessActivityNameToBeShown] =
    useState(true);
  const [processorNameToBeShown, setProcessorNameToBeShown] = useState(true);
  const [validitytoBeShown, setValiditytoBeShown] = useState(true);

  const [colors, setColors] = useState({
    cardBackground: "#FFFFFF",
    cardFont: "#000000",
    buttonBackground: "#0F3CC9",
    buttonFont: "#FFFFFF",
    linkFont: "#0A2885",
  });

  const [darkColors, setDarkColors] = useState({
    cardBackground: "#000000",
    cardFont: "#FFFFFF",
    buttonBackground: "#FFFFFF",
    buttonFont: "#0F3CC9",
    linkFont: "#0A2885",
  });

  const activePopupColors = darkMode ? darkColors : colors;

  const [languageContent, setLanguageContent] = useState({
    ENGLISH: {
      description: "sample description",
      label: "Required",
      rightsText: "sample rights text",
      permissionText: "sample permission text",
    },
  });

  const lightString = JSON.stringify(colors);
  const darkString = JSON.stringify(darkColors);

  const themeObj = {
    light: lightString,
    ...(darkString ? { dark: darkString } : {}), // include dark only if exists
  };

  // Convert to JSON string
  const themeJson = JSON.stringify(themeObj);

  // Convert JSON string → Base64
  const themeBase64 = btoa(themeJson);

  const addPurpose = () => {
    setPurposeItems([...purposeItems, counter]);
    setCounter(counter + 1);
    setPurposes([
      ...purposes,
      {
        purposeIds: [],
        isMandatory: "",
        autoRenew: "",
        preferenceValidity: { value: "1", unit: "" },
        purposeActivityIds: [],
        usedBy: [],
        dataItems: [],
      },
    ]);
  };

  const removePurpose = (id, index) => {
    if (purposeItems.length === 1) return;
    setPurposeItems(purposeItems.filter((item) => item !== id));
    setPurposes(purposes.filter((_, i) => i !== index));
  };

  const handleBack = () => {
    navigate("/integrationGrievanceRequests");
  };

  const buildPayload = (purposes) => {
    return purposes.map((p) => ({
      purposeIds: p.purposeIds,
      mandatory: p.isMandatory === "Required",
      autoRenew: p.autoRenew === "Yes",
      preferenceValidity: p.preferenceValidity,
      processorActivityIds: p.purposeActivityIds,
      preferenceStatus: "ACCEPTED",
    }));
  };

  const openCertificate = (certificatePreviewUrl) => {
    if (certificatePreviewUrl) {
      window.open(certificatePreviewUrl, "_blank");
    }
  };

  const [finalPayload, setFinalPayload] = useState([]);
  const [showSuccessTick, setShowSuccessTick] = useState(false);

  const handlePublish = async () => {
    const errors = [];

    // --- 1. Grievance Type ---
    if (!selectedGrievanceType) {
      errors.push("Please select a Grievance Category.");
    }

    // --- 2. Grievance Detail/Sub-category ---
    if (selectedGrievanceType && !selectedDetail) {
      errors.push("Please select a Grievance Sub-Category.");
    }

    // --- 3. Description ---
    if (
      grievanceFormTemplate?.multilingual?.descriptionCheck &&
      !grievanceDesc.trim()
    ) {
      errors.push("Please enter Description.");
    }

    // --- 4. User Type ---
    if (
      grievanceFormTemplate?.multilingual.userInformation[0]?.userType?.length >
        0 &&
      !selectedUserType
    ) {
      errors.push("Please select a User Type.");
    }

    // --- 5. User Item inputs ---
    if (
      grievanceFormTemplate?.multilingual.userInformation[0]?.userItems
        ?.length > 0
    ) {
      grievanceFormTemplate?.multilingual.userInformation[0].userItems.forEach(
        (item) => {
          if (!userDetailInputs[item] || userDetailInputs[item].trim() === "") {
            errors.push(`Please enter ${item}.`);
          }
        }
      );
    }

    // --- 6. Upload File ---
    if (grievanceFormTemplate?.multilingual.uploadFiles && !uploadFileName) {
      errors.push("Please upload a supporting document.");
    }

    // --- Show Toast if errors exist ---
    if (errors.length > 1) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={
              <ul style={{ margin: 0 }}>
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
    } else if (errors.length === 1) {
      toast.error(
        (props) => <CustomToast {...props} type="error" message={errors[0]} />,
        { icon: false }
      );
      return;
    } else if (true) {
      // Format grievance information (if needed for later use)
      const formattedGrievanceInformation = Object.entries(
        grievanceFormTemplate?.grievances || {}
      ).map(([grievanceType, grievanceItems]) => ({
        grievanceType,
        grievanceItems,
      }));

      // --- Build userDetails object ---
      const userDetails =
        grievanceFormTemplate?.multilingual?.userInformation?.[0]?.userItems?.reduce(
          (acc, label) => {
            // Normalize to uppercase and underscore format like backend expects
            // const key = label.toUpperCase().replace(/\s+/g, "_");
            const key = label;
            acc[key] = userDetailInputs[label] || "";
            return acc;
          },
          {}
        );

      // --- Final request body ---
      const body = {
        userType: selectedUserType || "", // e.g. "Student"
        grievanceType: selectedGrievanceType || "", // e.g. "Consent_G_24Oct"
        grievanceDetail: selectedDetail || "", // e.g. "ACCESS"
        userDetails, // nested user details object
        grievanceDescription: grievanceDesc || "",
        supportingDocs: uploadFileName
          ? [
              {
                docName: uploadFileName,
                doc: uploadFileBase64,
              },
            ]
          : [],
      };

      const res = await dispatch(
        createIntegrationGrievanceRequest({
          grievanceTemplateId: grievanceFormTemplate.grievanceTemplateId,
          body,
          tenantId,
          businessId,
          secureCode,
        })
      );

      if (res?.status === 201) {
        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message={"Grievance Request Raised Succesfully!"}
            />
          ),
          { icon: false }
        );
        setShowSuccessTick(true);
      }

      // toast.success(
      //   (props) => (
      //     <CustomToast
      //       {...props}
      //       type="success"
      //       message={"Grievance Request Saved Succesfully!"}
      //     />
      //   ),
      //   { icon: false }
      // );
    }
  };

  return (
    <div className="page-container">
      <div className="main-content">
        <div className="right-half">
          <div className="preview-header">
            {/* <div className="header-left">
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
            </div> */}
            <div className="header-right">
              <div
                className="popup-mobile"
                style={{
                  backgroundColor: activePopupColors.cardBackground,
                  color: activePopupColors.cardFont,
                }}
              >
                <div className="popup-header">
                  <div
                    style={{
                      display: "flex",
                      flexDirection: "row",
                      gap: "20px",
                      alignItems: "center",
                    }}
                  >
                    <h1
                      className="custom-head"
                      style={{
                        backgroundColor: activePopupColors.cardBackground,
                        color: activePopupColors.cardFont,
                        // marginBottom: "8px",
                      }}
                    >
                      {title ? title : "Register your grievance"}
                    </h1>
                    <Button
                      ariaControls="Button Clickable"
                      ariaDescribedby="Button"
                      ariaExpanded="Expanded"
                      ariaLabel="Button"
                      className="Button"
                      icon="ic_chevron_down"
                      iconLeft={<IcLanguage />}
                      iconAriaLabel="Icon Favorite"
                      kind="secondary"
                      label="English"
                      size="small"
                      state="normal"
                    />
                  </div>

                  <img
                    src={logoPreviewUrl}
                    alt="Logo"
                    style={{
                      width: mobile ? "28px" : "40px",
                      height: mobile ? "28px" : "40px",
                      marginLeft: mobile ? "5px" : "10px",
                    }}
                  />
                </div>

                <div className="popup-body">
                  <p
                    className="custom-text-p"
                    style={{
                      backgroundColor: activePopupColors.cardBackground,
                      color: activePopupColors.cardFont,
                      marginBottom: "8px",
                      fontSize: "14px",
                      letterSpacing: "-0.5px",
                      color: "rgba(0, 0, 0, 0.65)",
                      fontWeight: "500",
                    }}
                  >
                    {text
                      ? text
                      : "Please provide the following details to register your grievance."}
                  </p>
                  {grievanceFormTemplate?.multilingual?.grievanceInformation
                    ?.length > 0 && (
                    <div>
                      {/* --- Section heading --- */}
                      <div style={{ marginBottom: "10px" }}>
                        <Text appearance="heading-xxs" color="primary-grey-80">
                          {grievanceDetailsHeading}
                        </Text>
                      </div>

                      {/* --- Grievance Category Dropdown --- */}
                      <div className="dropdown-group">
                        <label>{grievanceCategoryLabel}</label>
                        <select
                          value={selectedGrievanceType}
                          onChange={(e) => {
                            const newType = e.target.value;
                            setSelectedGrievanceType(newType);
                            setSelectedDetail("");
                          }}
                        >
                          <option value="" disabled>
                            {selectCategoryPlaceholder}
                          </option>
                          {grievanceFormTemplate?.multilingual.grievanceInformation.map(
                            (info, idx) => (
                              <option key={idx} value={info.grievanceType}>
                                {info.grievanceType}
                              </option>
                            )
                          )}
                        </select>
                      </div>

                      {/* --- Grievance Sub-Category Dropdown --- */}
                      {selectedGrievanceType && (
                        <div className="dropdown-group">
                          <label htmlFor={`grievanceTypeSelect`}>
                            {grievanceSubCategoryLabel}
                          </label>
                          <select
                            id={`grievanceTypeSelect`}
                            value={selectedDetail}
                            onChange={(e) => setSelectedDetail(e.target.value)}
                          >
                            <option value="" disabled>
                              {subCategoryPlaceholder}
                            </option>

                            {/* find the selected grievance type and show its items */}
                            {(
                              grievanceFormTemplate?.multilingual.grievanceInformation.find(
                                (info) =>
                                  info.grievanceType === selectedGrievanceType
                              )?.grievanceItems || []
                            ).map((item, idx) => (
                              <option key={idx} value={item}>
                                {item}
                              </option>
                            ))}
                          </select>
                        </div>
                      )}
                    </div>
                  )}

                  {grievanceFormTemplate?.multilingual?.descriptionCheck && (
                    <div className="dropdown-group">
                      <label>{descriptionLabel}</label>
                      <textarea
                        placeholder={descriptionPlaceholder}
                        value={grievanceDesc}
                        onChange={handleTextAreaChange}
                        rows="3"
                        className="custom-text-area"
                      />
                    </div>
                  )}

                  {(grievanceFormTemplate?.multilingual?.userInformation[0]
                    ?.userType?.length > 0 ||
                    grievanceFormTemplate?.multilingual?.userInformation[0]
                      ?.userItems?.length > 0) && (
                    <Text appearance="heading-xxs" color="primary-grey-80">
                      {userDetailsHeading}
                    </Text>
                  )}

                  {grievanceFormTemplate?.multilingual?.userInformation[0]
                    ?.userType &&
                    grievanceFormTemplate?.multilingual?.userInformation[0]
                      ?.userType.length > 0 && (
                      <div
                        className="dropdown-group"
                        style={{ marginBottom: "0px" }}
                      >
                        <label>{userTypeLabel}</label>
                        <select
                          value={selectedUserType}
                          onChange={(e) => setSelectedUserType(e.target.value)}
                        >
                          <option value="" disabled>
                            {userTypePlaceholder}
                          </option>
                          {grievanceFormTemplate?.multilingual?.userInformation[0]?.userType.map(
                            (type, index) => (
                              <option key={index} value={type}>
                                {type}
                              </option>
                            )
                          )}
                        </select>
                      </div>
                    )}
                  {grievanceFormTemplate?.multilingual?.userInformation?.[0]
                    ?.userItems?.length > 0 && (
                    <div>
                      {grievanceFormTemplate?.multilingual?.userInformation[0]?.userItems.map(
                        (detailName, index) => (
                          <div style={{ padding: "10px 0px" }} key={index}>
                            <div className="systemConfig-input">
                              <label
                                style={{
                                  display: "block",
                                  fontSize: "13px",
                                  fontWeight: "600",
                                  fontFamily: "Arial, sans-serif",
                                  marginBottom: "5px",
                                  color: "#767676",
                                }}
                              >
                                {`${detailName} (${requiredLable})`}
                              </label>

                              <input
                                type="text"
                                value={userDetailInputs[detailName] || ""}
                                onChange={(e) =>
                                  setUserDetailInputs({
                                    ...userDetailInputs,
                                    [detailName]: e.target.value,
                                  })
                                }
                                placeholder={`${enterLable} ${detailName.toLowerCase()}`}
                                style={{
                                  width: "100%",
                                  padding: "10px 12px",
                                  border: "1px solid #ccc",
                                  borderRadius: "8px",
                                  fontSize: "14px",
                                  fontWeight: 400,
                                  color: "rgba(20, 20, 20, 1)",
                                  outline: "none",
                                  fontFamily: "Arial, sans-serif",
                                }}
                              />
                            </div>
                          </div>
                        )
                      )}
                    </div>
                  )}

                  {grievanceFormTemplate?.multilingual?.uploadFiles && (
                    <>
                      <div className="">
                        <Text appearance="heading-xxs" color="primary-grey-80">
                          {uploadFilesHeading}
                        </Text>
                      </div>

                      <div
                        style={{
                          marginBottom: "20px",
                        }}
                      >
                        <div
                          className="fileUploader-custom1"
                          onClick={handleUploadFilesClick}
                          onDragOver={handleUploadFilesDragOver}
                          onDragLeave={handleUploadFilesDragLeave}
                          onDrop={handleUploadFilesDrop}
                        >
                          <input
                            type="file"
                            ref={uploadFileInputRef}
                            style={{ display: "none" }}
                            onChange={handleUploadFilieChange}
                            accept=".pdf,.doc,.docx"
                          />

                          <div className="flex items-center justify-center">
                            <Icon
                              ic={<IcUpload height={23} width={23} />}
                              color="primary_60"
                            />
                            <Text appearance="button" color="primary-60">
                              {uploadButtonText}
                            </Text>
                          </div>
                        </div>

                        <div style={{}}>
                          <Text appearance="body-xs" color="primary-grey-80">
                            {uploadInstruction}
                          </Text>
                        </div>
                      </div>
                    </>
                  )}

                  {uploadFileName && (
                    <div className="systemConfiguration-file-uploader">
                      <div
                        onClick={handlePreviewUploadFile}
                        className="previewFile"
                      >
                        <div className="iconandText">
                          <Icon
                            ic={<IcSuccess width={15} height={15} />}
                            color="feedback_success_50"
                          />
                          <Text appearance="body-xs" color="primary-grey-80">
                            {uploadFileName}
                          </Text>
                        </div>
                      </div>
                      <Icon
                        ic={<IcClose width={15} height={15} />}
                        color="primary_60"
                        className="selecetd-file-display"
                        onClick={handleRemoveUploadFile}
                      />
                    </div>
                  )}
                </div>
                {/* <div className='popup-button'> */}
                <div
                  className={`popup-button ${
                    mobile ? "popup-button-mobile" : ""
                  }`}
                >
                  <div style={{}}>
                    <ActionButton
                      kind="primary"
                      size="medium"
                      state="normal"
                      label="Submit Request"
                      onClick={handlePublish}
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {showSuccessTick && (
          <div className="Integrationoverlay">
            <div className="success-icon">✔</div>
          </div>
        )}
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
    </div>
  );
};

export default IntegrationCreateRequest;
