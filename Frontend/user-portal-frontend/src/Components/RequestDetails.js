import React, { useState, useEffect, useMemo } from "react";
import "../Styles/createRequest.css";
import { ActionButton, Icon, Text, Button } from "@jds/core";
import "../Styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate, useParams } from "react-router-dom";
import { useRef } from "react";
import Select from "react-select";
import { IcLanguage } from "@jds/extended-icons";
import { IcChevronDown, IcUpload, IcSuccess, IcClose } from "@jds/core-icons";
import {
  createGrievanceRequest,
  getGrievanceRequestDetails,
  getGrievnaceTemplateData,
  translateData,
} from "../store/actions/CommonAction";
import useTranslation, { getApiValueFromLanguage } from "../hooks/useTranslation";

const RequestDetails = () => {
  const DEFAULT_DETAILS = {
    "User access": ["Submit request", "Access data", "Erase data"],
    Legal: ["Raise issue", "Add data", "File complaint"],
  };
  const logos = new URL("../Assets/popup.svg", import.meta.url).href;
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { grievanceId } = useParams();

  const [isChecked, setIsChecked] = useState(false);
  const [templateName, setTemplateName] = useState("");
  const [dark, setDark] = useState(false);
  const [mobile, setMobile] = useState(true);
  const [view, setView] = useState("popup");
  const [title, setTitle] = useState("Register your grievance");
  const [cid, setCid] = useState("Customer ID");
  const [age, setAge] = useState("I'm below 18 years of age.");
  const [purposeHeading, setPurposeHeading] = useState("Purpose: ");
  const [processingHeading, setProcessingHeading] = useState(
    "Processing activity: "
  );
  const [usedByHeading, setUsedByHeading] = useState("Used By: ");
  const [durationHeading, setDurationHeading] = useState("Duration: ");
  const [dataItemHeading, setDataItemHeading] = useState("Data item: ");
  const [selectedGrievanceType, setSelectedGrievanceType] = useState("");
  const [selectedDetail, setSelectedDetail] = useState("");
  const [isViewMode, setIsViewMode] = useState(false);
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
      { id: "details_title", source: "Register your grievance" },
      { id: "details_description_text", source: "Please provide the following details to register your grievance." },
      { id: "details_purpose", source: "Purpose: " },
      { id: "details_processing", source: "Processing activity: " },
      { id: "details_used_by", source: "Used By: " },
      { id: "details_duration", source: "Duration: " },
      { id: "details_data_item", source: "Data item: " },
      { id: "details_grievance_details", source: "Grievance Details" },
      { id: "details_select_category", source: "Select Category" },
      { id: "details_grievance_category", source: "Grievance Category" },
      { id: "details_grievance_subcategory", source: "Grievance Subcategory" },
      { id: "details_enter_subcategory", source: "Enter Grievance Subcategory" },
      { id: "details_description", source: "Description (Required)" },
      { id: "details_description_placeholder", source: "Enter description of the group's function or scope." },
      { id: "details_user_details", source: "User Details" },
      { id: "details_user_type", source: "User Type" },
      { id: "details_select_user_type", source: "Select user type" },
      { id: "details_upload_files", source: "Upload Files" },
      { id: "details_upload", source: "Upload" },
      { id: "details_upload_instruction", source: "Drag and drop or upload a document with hyperlink with the consent banner in .pdf or .doc format." },
      { id: "details_required", source: "Required" },
      { id: "details_enter", source: "Enter" },
      { id: "details_submit_request", source: "Submit Request" },
      { id: "details_always_active", source: "Always Active" },
    ],
    []
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
      if (grievanceFormTemplate && Object.keys(grievanceFormTemplate).length > 0) {
        const targetLang = getApiValueFromLanguage(currentLanguage);
        translateFormFields(grievanceFormTemplate, targetLang);
      }
    } else {
      setDynamicTranslations({});
    }
  }, [currentLanguage, translationInputs, translateContent, grievanceFormTemplate]);
  useEffect(() => {
    const fetchData = async () => {
      try {
        const templateRes = await dispatch(getGrievnaceTemplateData());

        if (templateRes.status === 200) {
          const data = templateRes.data;
          setGrievanceFormTemplate(data);

          // --- Handle multilingual title/text ---
          const supportedLanguages = data?.multilingual?.supportedLanguages;
          const selectedLanguage = supportedLanguages?.[0];
          let title = "";
          let text = "";

          if (selectedLanguage && data?.languages) {
            const matchingLanguageEntry = Object.values(data.languages).find(
              (lang) =>
                lang.language?.toUpperCase() ===
                  selectedLanguage.toUpperCase() ||
                lang.heading
                  ?.toUpperCase()
                  ?.includes(selectedLanguage.toUpperCase())
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

        if (grievanceId) {
          const grievanceRes = await dispatch(
            getGrievanceRequestDetails(grievanceId)
          );

          if (grievanceRes.status === 200) {
            const grievanceData = grievanceRes.data;

            // --- Prefill base fields ---
            setSelectedGrievanceType(grievanceData.grievanceType || "");
            setSelectedDetail(grievanceData.grievanceDetail || "");
            setSelectedUserType(grievanceData.userType || "");
            setGrievanceDesc(grievanceData.grievanceDescription || "");

            // --- Prefill user details ---
            const filledInputs = { ...userDetailInputs };
            const userItems =
              templateRes.data?.multilingual?.userInformation?.[0]?.userItems ||
              [];

            userItems.forEach((label) => {
              const normalizedLabel = label
                .toUpperCase()
                .replace(/[^A-Z0-9]/g, "");
              const matchedKey = Object.keys(
                grievanceData.userDetails || {}
              ).find(
                (key) =>
                  key.toUpperCase().replace(/[^A-Z0-9]/g, "") ===
                  normalizedLabel
              );
              if (matchedKey) {
                filledInputs[label] = grievanceData.userDetails[matchedKey];
              }
            });

            setUserDetailInputs(filledInputs);

            // --- Handle uploaded documents ---
            if (grievanceData.supportingDocs?.length > 0) {
              const doc = grievanceData.supportingDocs[0];
              setUploadFileName(doc.name);
              setUploadFileBase64(doc.documentId || "");
            }

            setIsViewMode(true);
          }
        }
      } catch (err) {
        console.log("Error fetching data:", err);
        if (err?.[0]?.errorCode === "JCMP3001") {
          setConsentsByTempId([]);
        }
      }
    };

    fetchData();
  }, [grievanceId]);

  const [grievanceDesc, setGrievanceDesc] = useState("");
  // for upload component
  const uploadFileInputRef = useRef(null);
  const [uploadFileName, setUploadFileName] = useState("");
  const [uploadFileBase64, setUploadFileBase64] = useState("");
  const [upoladFilePreviewUrl, setUploadFilePreviewUrl] = useState("");
  const [selectedUserType, setSelectedUserType] = useState("");
  const [userDetailInputs, setUserDetailInputs] = useState({});

  // Get translated labels from hook
  const grievanceDetailsHeading = getTranslation("details_grievance_details", "Grievance Details");
  const selectCategoryPlaceholder = getTranslation("details_select_category", "Select Category");
  const grievanceCategoryLabel = getTranslation("details_grievance_category", "Grievance Category");
  const grievanceSubCategoryLabel = getTranslation("details_grievance_subcategory", "Grievance Subcategory");
  const subCategoryPlaceholder = getTranslation("details_enter_subcategory", "Enter Grievance Subcategory");
  const descriptionLabel = getTranslation("details_description", "Description (Required)");
  const descriptionPlaceholder = getTranslation("details_description_placeholder", "Enter description of the group's function or scope.");
  const userDetailsHeading = getTranslation("details_user_details", "User Details");
  const userTypeLabel = getTranslation("details_user_type", "User Type");
  const userTypePlaceholder = getTranslation("details_select_user_type", "Select user type");
  const uploadFilesHeading = getTranslation("details_upload_files", "Upload Files");
  const uploadButtonText = getTranslation("details_upload", "Upload");
  const uploadInstruction = getTranslation("details_upload_instruction", "Drag and drop or upload a document with hyperlink with the consent banner in .pdf or .doc format.");
  const requiredLable = getTranslation("details_required", "Required");
  const enterLable = getTranslation("details_enter", "Enter");

  const handleDetailSelect = (e) => {
    setSelectedDetail(e.target.value);
  };
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
            <span className="accordion-label">{getTranslation("details_always_active", "Always Active")}</span>
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

  const [text, setText] = useState(
    "While using JioMeet, your activities create data which will be used with your consent to offer customised services. Details of data usage are provided below. "
  );

  //purpose container states
  const [purposeItems, setPurposeItems] = useState([0]);
  const [counter, setCounter] = useState(1);

  const [logoPreviewUrl, setLogoPreviewUrl] = useState("");
  const [darkMode, setDarkMode] = useState(false);
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

  const handleBack = () => {
    navigate("/requests");
  };

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
      grievanceFormTemplate.multilingual.descriptionCheck &&
      !grievanceDesc.trim()
    ) {
      errors.push("Please enter Description.");
    }

    // --- 4. User Type ---
    if (
      grievanceFormTemplate.multilingual.userInformation[0]?.userType?.length >
        0 &&
      !selectedUserType
    ) {
      errors.push("Please select a User Type.");
    }

    // --- 5. User Item inputs ---
    if (
      grievanceFormTemplate.multilingual.userInformation[0]?.userItems?.length >
      0
    ) {
      grievanceFormTemplate.multilingual.userInformation[0].userItems.forEach(
        (item) => {
          if (!userDetailInputs[item] || userDetailInputs[item].trim() === "") {
            errors.push(`Please enter ${item}.`);
          }
        }
      );
    }

    // --- 6. Upload File ---
    if (grievanceFormTemplate.multilingual.uploadFiles && !uploadFileName) {
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
      const formattedGrievanceInformation = Object.entries(
        grievanceFormTemplate?.grievances || {}
      ).map(([grievanceType, grievanceItems]) => ({
        grievanceType,
        grievanceItems,
      }));
      const userDetails =
        grievanceFormTemplate.multilingual.userInformation[0].userItems.reduce(
          (acc, label) => {
            const key = label.toLowerCase().replace(/\s+/g, "_");
            acc[key] = userDetailInputs[label] || "";
            return acc;
          },
          {}
        );
      // request body
      const requestBody = {
        grievanceRelatedTo: selectedGrievanceType,
        grievanceRaisedBy: selectedUserType,
        grievanceDescription: grievanceDesc,
        ...userDetails,
        supportingDocs: uploadFileName
          ? [
              {
                docName: uploadFileName,
                doc: uploadFileBase64,
              },
            ]
          : [],
      };

      const res = await createGrievanceRequest(requestBody);

      if (res.status === 201) {
        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message={"Form Template Published successfully."}
            />
          ),
          { icon: false }
        );
      }

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

      // navigate("/grievanceFormTemplates");
    }
  };

  const [activeTab, setActiveTab] = useState("popup");

  return (
    <div className="page-container">
      <div className="main-content">
        <div className="right-half">
          <div className="preview-header">
            <div className="header-left">
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
                      {title ? title : getTranslation("details_title", "Register your grievance")}
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
                      fontSize: "14px",
                      letterSpacing: "-0.5px",
                      color: "rgba(0, 0, 0, 0.65)",
                      fontWeight: "500",
                    }}
                  >
                    {text
                      ? text
                      : getTranslation("details_description_text", "Please provide the following details to register your grievance.")}
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
                      <div className="dropdown-group" role="group" aria-labelledby="details-category-label">
                        <label id="details-category-label" htmlFor="details-category-select">
                          {grievanceCategoryLabel}
                        </label>
                        <select
                          id="details-category-select"
                          disabled={isViewMode}
                          value={selectedGrievanceType}
                          onChange={(e) => {
                            const newType = e.target.value;
                            setSelectedGrievanceType(newType);
                            setSelectedDetail("");
                          }}
                          aria-label={grievanceCategoryLabel}
                          aria-required="true"
                          aria-disabled={isViewMode}
                        >
                          <option value="" disabled>
                            {selectCategoryPlaceholder}
                          </option>
                          {grievanceFormTemplate.multilingual.grievanceInformation.map(
                            (info, idx) => (
                              <option key={idx} value={info.grievanceType}>
                                {getDynamicTranslation(info.grievanceType)}
                              </option>
                            )
                          )}
                        </select>
                      </div>

                      {/* --- Grievance Sub-Category Dropdown --- */}
                      {selectedGrievanceType && (
                        <div className="dropdown-group" role="group" aria-labelledby="details-subcategory-label">
                          <label id="details-subcategory-label" htmlFor="details-subcategory-select">
                            {grievanceSubCategoryLabel}
                          </label>

                          <select
                            id="details-subcategory-select"
                            value={selectedDetail}
                            disabled={isViewMode}
                            aria-label={grievanceSubCategoryLabel}
                            aria-required="true"
                            aria-disabled={isViewMode}
                            style={{
                              pointerEvents: isViewMode ? "none" : "auto",
                              color: "#333",
                            }}
                          >
                            <option value="" disabled>
                              {subCategoryPlaceholder}
                            </option>

                            <option value={selectedDetail}>
                              {getDynamicTranslation(selectedDetail)}
                            </option>
                          </select>
                        </div>
                      )}
                    </div>
                  )}

                  {grievanceFormTemplate?.multilingual?.descriptionCheck && (
                    <div className="dropdown-group" role="group" aria-labelledby="details-description-label">
                      <label id="details-description-label" htmlFor="details-description">
                        {descriptionLabel}
                      </label>
                      <textarea
                        id="details-description"
                        disabled={isViewMode}
                        placeholder={descriptionPlaceholder}
                        value={grievanceDesc}
                        onChange={handleTextAreaChange}
                        rows="3"
                        className="custom-text-area"
                        aria-label={descriptionLabel}
                        aria-required="true"
                        aria-disabled={isViewMode}
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
                    .userType &&
                    grievanceFormTemplate?.multilingual?.userInformation[0]
                      .userType.length > 0 && (
                      <div
                        className="dropdown-group"
                        style={{ marginBottom: "0px" }}
                        role="group"
                        aria-labelledby="details-usertype-label"
                      >
                        <label id="details-usertype-label" htmlFor="details-usertype-select">
                          {userTypeLabel}
                        </label>
                        <select
                          id="details-usertype-select"
                          disabled={isViewMode}
                          value={selectedUserType}
                          onChange={(e) => setSelectedUserType(e.target.value)}
                          aria-label={userTypeLabel}
                          aria-required="true"
                          aria-disabled={isViewMode}
                        >
                          <option value="" disabled>
                            {userTypePlaceholder}
                          </option>
                          {grievanceFormTemplate.multilingual.userInformation[0].userType.map(
                            (type, index) => (
                              <option key={index} value={type}>
                                {getDynamicTranslation(type)}
                              </option>
                            )
                          )}
                        </select>
                      </div>
                    )}
                  {grievanceFormTemplate?.multilingual?.userInformation?.[0]
                    ?.userItems?.length > 0 && (
                    <div>
                      {grievanceFormTemplate?.multilingual?.userInformation[0].userItems.map(
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
                                {`${getDynamicTranslation(detailName)} (${requiredLable})`}
                              </label>

                              <input
                                type="text"
                                disabled={isViewMode}
                                value={userDetailInputs[detailName] || ""}
                                onChange={(e) =>
                                  setUserDetailInputs({
                                    ...userDetailInputs,
                                    [detailName]: e.target.value,
                                  })
                                }
                                placeholder={`${enterLable} ${getDynamicTranslation(detailName).toLowerCase()}`}
                                style={{
                                  width: "100%",
                                  padding: "10px 12px",
                                  border: "1px solid #ccc",
                                  borderRadius: "8px",
                                  fontSize: "14px",
                                  fontWeight: 400,
                                  color: "#333",
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

                  {grievanceFormTemplate?.multilingual?.uploadFiles &&
                    !isViewMode && (
                      <>
                        <div className="">
                          <Text
                            appearance="heading-xxs"
                            color="primary-grey-80"
                          >
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
                        <Icon
                          ic={<IcSuccess width={15} height={15} />}
                          color="feedback_success_50"
                        />
                        <Text appearance="body-xs" color="primary-grey-80">
                          {uploadFileName}
                        </Text>
                      </div>
                      {!isViewMode && (
                        <Icon
                          ic={<IcClose width={15} height={15} />}
                          color="primary_60"
                          className="selecetd-file-display"
                          onClick={handleRemoveUploadFile}
                        />
                      )}
                    </div>
                  )}
                </div>
                {/* <div className='popup-button'> */}
                <div
                  className={`popup-button ${
                    mobile ? "popup-button-mobile" : ""
                  }`}
                >
                  {!isViewMode && (
                    <div style={{}}>
                      <ActionButton
                        kind="primary"
                        size="medium"
                        state="normal"
                        label="Submit Request"
                        onClick={handlePublish}
                      />
                    </div>
                  )}
                </div>
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
    </div>
  );
};

export default RequestDetails;
