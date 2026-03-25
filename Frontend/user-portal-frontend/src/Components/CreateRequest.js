import React, { useState, useEffect, useMemo } from "react";
import "../Styles/createRequest.css";
import { textStyle, FONT_FAMILY_STACK } from "../utils/textStyles";
import { ICON_SIZE } from "../utils/iconSizes";
import "../Styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { useRef } from "react";

import {
  createGrievanceRequest,
  getGrievnaceTemplateData,
  translateData,
  translateConfiguration,
} from "../store/actions/CommonAction";
import useTranslation, {
  getApiValueFromLanguage,
} from "../hooks/useTranslation";
import {
  FaChevronDown,
  FaChevronUp,
  FaTimes,
  FaTimesCircle,
  FaUpload,
} from "react-icons/fa";
import { FiChevronLeft, FiChevronRight } from "react-icons/fi";

const CreateRequest = () => {
  const logos = new URL("../Assets/popup.svg", import.meta.url).href;

  const navigate = useNavigate();
  const dispatch = useDispatch();

  const [mobile, setMobile] = useState(true);
  const [title, setTitle] = useState("Register your grievance");
  const [purposeHeading, setPurposeHeading] = useState("Purpose: ");
  const [processingHeading, setProcessingHeading] = useState(
    "Processing activity: ",
  );
  const [usedByHeading, setUsedByHeading] = useState("Used By: ");
  const [durationHeading, setDurationHeading] = useState("Duration: ");
  const [dataItemHeading, setDataItemHeading] = useState("Data item: ");
  const [selectedType, setSelectedType] = useState("");
  const [selectedGrievanceType, setSelectedGrievanceType] = useState("");
  const [selectedDetail, setSelectedDetail] = useState("");
  const [grievanceFormTemplate, setGrievanceFormTemplate] = useState({});
  const [dynamicTranslations, setDynamicTranslations] = useState({});

  // Helper to get dynamic translation
  const getDynamicTranslation = (text) => {
    if (!text || currentLanguage === "ENGLISH") return text;
    return dynamicTranslations[text] || text;
  };

  // Translation inputs for static text
  const translationInputs = useMemo(
    () => [
      { id: "create_title", source: "Register your grievance" },
      {
        id: "create_description",
        source:
          "Please provide the following details to register your grievance.",
      },
      { id: "create_purpose", source: "Purpose: " },
      { id: "create_processing", source: "Processing activity: " },
      { id: "create_used_by", source: "Used By: " },
      { id: "create_duration", source: "Duration: " },
      { id: "create_data_item", source: "Data item: " },
      { id: "create_grievance_details", source: "Grievance Details" },
      { id: "create_select_category", source: "Select Category" },
      { id: "create_grievance_category", source: "Grievance Category" },
      { id: "create_grievance_subcategory", source: "Grievance Subcategory" },
      { id: "create_enter_subcategory", source: "Enter Grievance Subcategory" },
      { id: "create_description", source: "Description (Required)" },
      {
        id: "create_description_placeholder",
        source: "Enter description of the group's function or scope.",
      },
      { id: "create_user_details", source: "User Details" },
      { id: "create_user_type", source: "User Type" },
      { id: "create_select_user_type", source: "Select user type" },
      { id: "create_upload_files", source: "Upload Files" },
      { id: "create_upload", source: "Upload" },
      {
        id: "create_upload_instruction",
        source:
          "Drag and drop or upload a document with hyperlink with the consent banner in .pdf or .doc format.",
      },
      { id: "create_required", source: "Required" },
      { id: "create_enter", source: "Enter" },
      { id: "create_submit_request", source: "Submit Request" },
      { id: "create_always_active", source: "Always Active" },
    ],
    [],
  );

  // Use translation hook
  const { getTranslation, translateContent, currentLanguage, isTranslating } =
    useTranslation(translationInputs);

  // Function to translate form field values
  const translateFormFields = async (template, targetLang) => {
    if (!template || targetLang === "en") {
      setDynamicTranslations({});
      return;
    }

    const uniqueTexts = new Set();

    // Collect grievance types and subcategories
    template.multilingual?.grievanceInformation?.forEach((info) => {
      if (info.grievanceType) uniqueTexts.add(info.grievanceType);
      info.grievanceSubCategory?.forEach((sub) => uniqueTexts.add(sub));
      // Also collect grievanceItems (used in dropdown)
      info.grievanceItems?.forEach((item) => uniqueTexts.add(item));
    });

    // Collect user types and field names
    template.multilingual?.userInformation?.forEach((info) => {
      info.userType?.forEach((type) => uniqueTexts.add(type));
      // Collect field names from userItems
      info.userItems?.forEach((item) => uniqueTexts.add(item));
      // Also collect from userDetails if exists
      info.userDetails?.forEach((detail) => {
        if (detail.detailName) uniqueTexts.add(detail.detailName);
      });
    });

    if (uniqueTexts.size === 0) return;

    const inputs = Array.from(uniqueTexts).map((text, idx) => ({
      id: `form_${idx}`,
      source: text,
    }));

    try {
      const response = await dispatch(translateData("en", targetLang, inputs));
      if (response?.status === 200 && response?.data?.output) {
        const translatedMap = {};
        response.data.output.forEach((item, idx) => {
          translatedMap[inputs[idx].source] = item.target;
        });
        setDynamicTranslations(translatedMap);
      }
    } catch (error) {
      console.log("Error translating form fields:", error);
    }
  };

  // Translate content when language changes
  useEffect(() => {
    if (currentLanguage !== "ENGLISH") {
      translateContent(translationInputs, currentLanguage);
      if (
        grievanceFormTemplate &&
        Object.keys(grievanceFormTemplate).length > 0
      ) {
        const targetLang = getApiValueFromLanguage(currentLanguage);
        translateFormFields(grievanceFormTemplate, targetLang);
      }
    } else {
      setDynamicTranslations({});
    }
  }, [
    currentLanguage,
    translationInputs,
    translateContent,
    grievanceFormTemplate,
  ]);
  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await dispatch(getGrievnaceTemplateData());
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
                  .includes(selectedLanguage.toUpperCase()),
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

  // for translation - using translation hook
  const [text, setText] = useState(
    "While using JioMeet, your activities create data which will be used with your consent to offer customised services. Details of data usage are provided below. ",
  );

  // Get translated labels from hook
  const grievanceDetailsHeading = getTranslation(
    "create_grievance_details",
    "Grievance Details",
  );
  const selectCategoryPlaceholder = getTranslation(
    "create_select_category",
    "Select Category",
  );
  const grievanceCategoryLabel = getTranslation(
    "create_grievance_category",
    "Grievance Category",
  );
  const grievanceSubCategoryLabel = getTranslation(
    "create_grievance_subcategory",
    "Grievance Subcategory",
  );
  const subCategoryPlaceholder = getTranslation(
    "create_enter_subcategory",
    "Enter Grievance Subcategory",
  );
  const descriptionLabel = getTranslation(
    "create_description",
    "Description (Required)",
  );
  const descriptionPlaceholder = getTranslation(
    "create_description_placeholder",
    "Enter description of the group's function or scope.",
  );
  const userDetailsHeading = getTranslation(
    "create_user_details",
    "User Details",
  );
  const userTypeLabel = getTranslation("create_user_type", "User Type");
  const userTypePlaceholder = getTranslation(
    "create_select_user_type",
    "Select user type",
  );
  const uploadFilesHeading = getTranslation(
    "create_upload_files",
    "Upload Files",
  );
  const uploadButtonText = getTranslation("create_upload", "Upload");
  const uploadInstruction = getTranslation(
    "create_upload_instruction",
    "Drag and drop or upload a document with hyperlink with the consent banner in .pdf or .doc format.",
  );
  const requiredLable = getTranslation("create_required", "Required");
  const enterLable = getTranslation("create_enter", "Enter");

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
      fontSize: "11px",
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
      fontSize: "11px",
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
      fontSize: "11px",
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
      fontSize: "11px",
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
              <FaChevronDown size={ICON_SIZE} />
            </span>
            <span className="accordion-title">{title}</span>
          </div>

          {alwaysActive && (
            <span className="accordion-label">
              {getTranslation("create_always_active", "Always Active")}
            </span>
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
                            : item.preferenceValidity.unit, // plural
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
    "To withdraw your consent, exercise your rights, or file complaints with the Board click here.",
  );
  const [permission, setPermission] = useState(
    "By clicking ‘Allow all’ or ’Save my choices’, you are providing your consent to Reliance Medlab using your data as outlined above.",
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
    [],
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
    navigate("/requests");
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
        },
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
        { icon: false },
      );
      return;
    } else if (errors.length === 1) {
      toast.error(
        (props) => <CustomToast {...props} type="error" message={errors[0]} />,
        { icon: false },
      );
      return;
    } else if (true) {
      // Format grievance information (if needed for later use)
      const formattedGrievanceInformation = Object.entries(
        grievanceFormTemplate?.grievances || {},
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
          {},
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
        createGrievanceRequest({
          grievanceTemplateId: grievanceFormTemplate.grievanceTemplateId,
          body,
        }),
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
          { icon: false },
        );
        navigate("/requests");
      }

      toast.success(
        (props) => (
          <CustomToast
            {...props}
            type="success"
            message={"Grievance Request Saved Succesfully!"}
          />
        ),
        { icon: false },
      );
    }
  };

  return (
    <main
      className="page-container"
      role="main"
      aria-label="Create Grievance Request"
    >
      <div className="main-content">
        <div className="right-half">
          <nav className="preview-header" aria-label="Page navigation">
            <div className="header-left">
              <div
                role="button"
                tabIndex={0}
                aria-label="Next page"
                onClick={handleBack}
                style={{
                  cursor: "pointer",

                  width: "36px",
                  height: "36px",
                  borderRadius: "50%",
                  backgroundColor: "#fff", // white background
                  border: "1px solid #d1d5db", // light grey border
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                }}
              >
                <FiChevronLeft size={ICON_SIZE} color="#555" />
              </div>
              <span id="back-button-desc" className="sr-only">
                Navigate to previous page
              </span>
            </div>
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
                      {title
                        ? title
                        : getTranslation(
                            "create_title",
                            "Register your grievance",
                          )}
                    </h1>
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
                      fontSize: "11px",
                      letterSpacing: "-0.5px",
                      color: "rgba(0, 0, 0, 0.65)",
                      fontWeight: "500",
                    }}
                  >
                    {text
                      ? text
                      : getTranslation(
                          "create_description",
                          "Please provide the following details to register your grievance.",
                        )}
                  </p>
                  {grievanceFormTemplate?.multilingual?.grievanceInformation
                    ?.length > 0 && (
                    <div>
                      {/* --- Section heading --- */}
                      <div style={{ marginBottom: "10px" }}>
                        <span style={textStyle("heading-xxs", "primary-grey-80")}>
                          {grievanceDetailsHeading}
                        </span>
                      </div>

                      {/* --- Grievance Category Dropdown --- */}
                      <div
                        className="dropdown-group"
                        role="group"
                        aria-labelledby="grievance-category-label"
                      >
                        <label
                          id="grievance-category-label"
                          htmlFor="grievance-category-select"
                        >
                          {grievanceCategoryLabel}
                        </label>
                        <select
                          id="grievance-category-select"
                          value={selectedGrievanceType}
                          onChange={(e) => {
                            const newType = e.target.value;
                            setSelectedGrievanceType(newType);
                            setSelectedDetail("");
                          }}
                          aria-label={grievanceCategoryLabel}
                          aria-required="true"
                        >
                          <option value="" disabled>
                            {selectCategoryPlaceholder}
                          </option>
                          {grievanceFormTemplate?.multilingual.grievanceInformation.map(
                            (info, idx) => (
                              <option key={idx} value={info.grievanceType}>
                                {getDynamicTranslation(info.grievanceType)}
                              </option>
                            ),
                          )}
                        </select>
                      </div>

                      {/* --- Grievance Sub-Category Dropdown --- */}
                      {selectedGrievanceType && (
                        <div
                          className="dropdown-group"
                          role="group"
                          aria-labelledby="grievance-subcategory-label"
                        >
                          <label
                            id="grievance-subcategory-label"
                            htmlFor="grievance-subcategory-select"
                          >
                            {grievanceSubCategoryLabel}
                          </label>
                          <select
                            id="grievance-subcategory-select"
                            value={selectedDetail}
                            onChange={(e) => setSelectedDetail(e.target.value)}
                            aria-label={grievanceSubCategoryLabel}
                            aria-required="true"
                          >
                            <option value="" disabled>
                              {subCategoryPlaceholder}
                            </option>

                            {/* find the selected grievance type and show its items */}
                            {(
                              grievanceFormTemplate?.multilingual.grievanceInformation.find(
                                (info) =>
                                  info.grievanceType === selectedGrievanceType,
                              )?.grievanceItems || []
                            ).map((item, idx) => (
                              <option key={idx} value={item}>
                                {getDynamicTranslation(item)}
                              </option>
                            ))}
                          </select>
                        </div>
                      )}
                    </div>
                  )}

                  {grievanceFormTemplate?.multilingual?.descriptionCheck && (
                    <div
                      className="dropdown-group"
                      role="group"
                      aria-labelledby="description-label"
                    >
                      <label
                        id="description-label"
                        htmlFor="grievance-description"
                      >
                        {descriptionLabel}
                      </label>
                      <textarea
                        id="grievance-description"
                        placeholder={descriptionPlaceholder}
                        value={grievanceDesc}
                        onChange={handleTextAreaChange}
                        rows="3"
                        className="custom-text-area"
                        aria-label={descriptionLabel}
                        aria-required="true"
                        aria-describedby="description-hint"
                      />
                      <span id="description-hint" className="sr-only">
                        {descriptionPlaceholder}
                      </span>
                    </div>
                  )}

                  {(grievanceFormTemplate?.multilingual?.userInformation[0]
                    ?.userType?.length > 0 ||
                    grievanceFormTemplate?.multilingual?.userInformation[0]
                      ?.userItems?.length > 0) && (
                    <span style={textStyle("heading-xxs", "primary-grey-80")}>
                      {userDetailsHeading}
                    </span>
                  )}

                  {grievanceFormTemplate?.multilingual?.userInformation[0]
                    ?.userType &&
                    grievanceFormTemplate?.multilingual?.userInformation[0]
                      ?.userType.length > 0 && (
                      <div
                        className="dropdown-group"
                        style={{ marginBottom: "0px" }}
                        role="group"
                        aria-labelledby="user-type-label"
                      >
                        <label id="user-type-label" htmlFor="user-type-select">
                          {userTypeLabel}
                        </label>
                        <select
                          id="user-type-select"
                          value={selectedUserType}
                          onChange={(e) => setSelectedUserType(e.target.value)}
                          aria-label={userTypeLabel}
                          aria-required="true"
                        >
                          <option value="" disabled>
                            {userTypePlaceholder}
                          </option>
                          {grievanceFormTemplate?.multilingual?.userInformation[0]?.userType.map(
                            (type, index) => (
                              <option key={index} value={type}>
                                {getDynamicTranslation(type)}
                              </option>
                            ),
                          )}
                        </select>
                      </div>
                    )}
                  {grievanceFormTemplate?.multilingual?.userInformation?.[0]
                    ?.userItems?.length > 0 && (
                    <div role="group" aria-label="User Details Form Fields">
                      {grievanceFormTemplate?.multilingual?.userInformation[0]?.userItems.map(
                        (detailName, index) => (
                          <div style={{ padding: "10px 0px" }} key={index}>
                            <div className="systemConfig-input">
                              <label
                                id={`user-field-label-${index}`}
                                htmlFor={`user-field-input-${index}`}
                                style={{
                                  display: "block",
                                  fontSize: "11px",
                                  fontWeight: "600",
                                  fontFamily: FONT_FAMILY_STACK,
                                  marginBottom: "5px",
                                  color: "#767676",
                                }}
                              >
                                {`${getDynamicTranslation(detailName)} (${requiredLable})`}
                              </label>

                              <input
                                id={`user-field-input-${index}`}
                                type="text"
                                value={userDetailInputs[detailName] || ""}
                                onChange={(e) =>
                                  setUserDetailInputs({
                                    ...userDetailInputs,
                                    [detailName]: e.target.value,
                                  })
                                }
                                placeholder={`${enterLable} ${getDynamicTranslation(detailName).toLowerCase()}`}
                                aria-label={getDynamicTranslation(detailName)}
                                aria-required="true"
                                aria-labelledby={`user-field-label-${index}`}
                                style={{
                                  width: "100%",
                                  padding: "10px 12px",
                                  border: "1px solid #ccc",
                                  borderRadius: "8px",
                                  fontSize: "11px",
                                  fontWeight: 400,
                                  color: "rgba(20, 20, 20, 1)",
                                  outline: "none",
                                  fontFamily: FONT_FAMILY_STACK,
                                }}
                              />
                            </div>
                          </div>
                        ),
                      )}
                    </div>
                  )}

                  {grievanceFormTemplate?.multilingual?.uploadFiles && (
                    <div role="group" aria-labelledby="upload-section-label">
                      <div className="">
                        <span
                          id="upload-section-label"
                          style={textStyle("heading-xxs", "primary-grey-80")}
                        >
                          {uploadFilesHeading}
                        </span>
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
                          role="button"
                          tabIndex={0}
                          aria-label={`${uploadButtonText}. ${uploadInstruction}`}
                          onKeyDown={(e) => {
                            if (e.key === "Enter" || e.key === " ") {
                              e.preventDefault();
                              handleUploadFilesClick();
                            }
                          }}
                        >
                          <input
                            type="file"
                            ref={uploadFileInputRef}
                            style={{ display: "none" }}
                            onChange={handleUploadFilieChange}
                            accept=".pdf,.doc,.docx"
                            aria-label="Upload file"
                          />

                          <div
                            className="flex items-center justify-center"
                            aria-hidden="true"
                          >
                            <FaUpload size={ICON_SIZE} />
                            <span style={textStyle("button", "primary-60")}>
                              {uploadButtonText}
                            </span>
                          </div>
                        </div>

                        <div id="upload-instruction">
                          <span style={textStyle("body-xs", "primary-grey-80")}>
                            {uploadInstruction}
                          </span>
                        </div>
                      </div>
                    </div>
                  )}

                  {uploadFileName && (
                    <div className="systemConfiguration-file-uploader">
                      <div
                        onClick={handlePreviewUploadFile}
                        className="previewFile"
                      >
                        <div className="iconandText">
                          <FaCheckCircle size={ICON_SIZE} style={{ color: "green" }} />
                          <span style={textStyle("body-xs", "primary-grey-80")}>
                            {uploadFileName}
                          </span>
                        </div>
                      </div>
                      <FaTimes
                        size={ICON_SIZE}
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
                    <button
                      onClick={handlePublish}
                      aria-label="Submit Request"
                      style={{
                        backgroundColor: "#2563eb", // same modern blue
                        color: "#fff",
                        border: "none",
                        padding: "10px 20px", // slightly smaller than large button
                        borderRadius: "999px",
                        fontSize: "11px",
                        fontWeight: "600",
                        cursor: "pointer",
                        transition: "0.2s",
                      }}
                      onMouseEnter={(e) => {
                        e.currentTarget.style.backgroundColor = "#1d4ed8";
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.backgroundColor = "#2563eb";
                      }}
                    >
                      {getTranslation(
                        "create_submit_request",
                        "Submit Request",
                      )}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </nav>
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
    </main>
  );
};

export default CreateRequest;
