import React, { useState, useEffect, useMemo } from "react";
import "../Styles/createRequest.css";
import { textStyle, FONT_FAMILY_STACK } from "../utils/textStyles";
import { ICON_SIZE } from "../utils/iconSizes";
import "../Styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate, useParams } from "react-router-dom";
import { useRef } from "react";
import Select from "react-select";
import { FiChevronLeft } from "react-icons/fi";
import {
  createGrievanceRequest,
  createIntegrationGrievanceRequest,
  getGrievanceRequestDetails,
  getGrievnaceTemplateData,
  getIntegrationGrievanceRequestDetails,
  getIntegrationGrievnaceTemplateData,
} from "../store/actions/CommonAction";
import useTranslation from "../hooks/useTranslation";
import { FaChevronDown, FaLanguage, FaTimes, FaUpload } from "react-icons/fa";

const IntegrationGrievanceRequestsDetails = () => {
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
    "Processing activity: ",
  );
  const [usedByHeading, setUsedByHeading] = useState("Used By: ");
  const [durationHeading, setDurationHeading] = useState("Duration: ");
  const [dataItemHeading, setDataItemHeading] = useState("Data item: ");
  const [selectedGrievanceType, setSelectedGrievanceType] = useState("");
  const [selectedDetail, setSelectedDetail] = useState("");
  const [isViewMode, setIsViewMode] = useState(false);
  const [grievanceFormTemplate, setGrievanceFormTemplate] = useState({});

  // Translation inputs for static text
  const translationInputs = useMemo(
    () => [
      { id: "int_details_title", source: "Register your grievance" },
      { id: "int_details_grievance_details", source: "Grievance Details" },
      { id: "int_details_select_category", source: "Select Category" },
      { id: "int_details_grievance_category", source: "Grievance Category" },
      {
        id: "int_details_grievance_subcategory",
        source: "Grievance Subcategory",
      },
      {
        id: "int_details_enter_subcategory",
        source: "Enter Grievance Subcategory",
      },
      { id: "int_details_description", source: "Description (Required)" },
      {
        id: "int_details_description_placeholder",
        source: "Enter description of the group's function or scope.",
      },
      { id: "int_details_user_details", source: "User Details" },
      { id: "int_details_user_type", source: "User Type" },
      { id: "int_details_select_user_type", source: "Select user type" },
      { id: "int_details_upload_files", source: "Upload Files" },
      { id: "int_details_upload", source: "Upload" },
      {
        id: "int_details_upload_instruction",
        source:
          "Drag and drop or upload a document with hyperlink with the consent banner in .pdf or .doc format.",
      },
      { id: "int_details_required", source: "Required" },
      { id: "int_details_enter", source: "Enter" },
      { id: "int_details_submit_request", source: "Submit Request" },
      { id: "int_details_always_active", source: "Always Active" },
    ],
    [],
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
  useEffect(() => {
    const fetchData = async () => {
      try {
        const templateRes = await dispatch(
          getIntegrationGrievnaceTemplateData(),
        );

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
                  ?.includes(selectedLanguage.toUpperCase()),
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
            getIntegrationGrievanceRequestDetails(grievanceId),
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
                grievanceData.userDetails || {},
              ).find(
                (key) =>
                  key.toUpperCase().replace(/[^A-Z0-9]/g, "") ===
                  normalizedLabel,
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
  const grievanceDetailsHeading = getTranslation(
    "int_details_grievance_details",
    "Grievance Details",
  );
  const selectCategoryPlaceholder = getTranslation(
    "int_details_select_category",
    "Select Category",
  );
  const grievanceCategoryLabel = getTranslation(
    "int_details_grievance_category",
    "Grievance Category",
  );
  const grievanceSubCategoryLabel = getTranslation(
    "int_details_grievance_subcategory",
    "Grievance Subcategory",
  );
  const subCategoryPlaceholder = getTranslation(
    "int_details_enter_subcategory",
    "Enter Grievance Subcategory",
  );
  const descriptionLabel = getTranslation(
    "int_details_description",
    "Description (Required)",
  );
  const descriptionPlaceholder = getTranslation(
    "int_details_description_placeholder",
    "Enter description of the group's function or scope.",
  );
  const userDetailsHeading = getTranslation(
    "int_details_user_details",
    "User Details",
  );
  const userTypeLabel = getTranslation("int_details_user_type", "User Type");
  const userTypePlaceholder = getTranslation(
    "int_details_select_user_type",
    "Select user type",
  );
  const uploadFilesHeading = getTranslation(
    "int_details_upload_files",
    "Upload Files",
  );
  const uploadButtonText = getTranslation("int_details_upload", "Upload");
  const uploadInstruction = getTranslation(
    "int_details_upload_instruction",
    "Drag and drop or upload a document with hyperlink with the consent banner in .pdf or .doc format.",
  );
  const requiredLable = getTranslation("int_details_required", "Required");
  const enterLable = getTranslation("int_details_enter", "Enter");

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
              <FaChevronDown size={ICON_SIZE} />
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

  const [text, setText] = useState(
    "While using JioMeet, your activities create data which will be used with your consent to offer customised services. Details of data usage are provided below. ",
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
    navigate("/integrationGrievanceRequests");
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
        },
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
      const formattedGrievanceInformation = Object.entries(
        grievanceFormTemplate?.grievances || {},
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
          {},
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

      const res = await createIntegrationGrievanceRequest(requestBody);

      if (res.status === 201) {
        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message={"Form Template Published successfully."}
            />
          ),
          { icon: false },
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
        { icon: false },
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
              <div
                role="button"
                tabIndex={0}
                aria-label="Go back"
                onClick={handleBack}
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    handleBack();
                  }
                }}
                style={{
                  cursor: "pointer",
                  width: "36px",
                  height: "36px",
                  borderRadius: "50%",
                  backgroundColor: "#fff",
                  border: "1px solid #d1d5db",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                }}
              >
                <FiChevronLeft size={ICON_SIZE} color="#555" />
              </div>
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
                      {title ? title : "Register your grievance"}
                    </h1>
                    <FaLanguage />
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
                      : "Please provide the following details to register your grievance."}
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
                      <div className="dropdown-group">
                        <label>{grievanceCategoryLabel}</label>
                        <select
                          disabled={isViewMode}
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
                          {grievanceFormTemplate.multilingual.grievanceInformation.map(
                            (info, idx) => (
                              <option key={idx} value={info.grievanceType}>
                                {info.grievanceType}
                              </option>
                            ),
                          )}
                        </select>
                      </div>

                      {/* --- Grievance Sub-Category Dropdown --- */}
                      {selectedGrievanceType && (
                        <div className="dropdown-group">
                          <label htmlFor="grievanceTypeSelect">
                            {grievanceSubCategoryLabel}
                          </label>

                          <select
                            id="grievanceTypeSelect"
                            value={selectedDetail}
                            // onChange={(e) => setSelectedDetail(e.target.value)}
                            disabled={isViewMode} // ✅ still visually disabled
                            style={{
                              pointerEvents: isViewMode ? "none" : "auto", // prevent opening dropdown
                              color: "#333",
                            }}
                          >
                            <option value="" disabled>
                              {subCategoryPlaceholder}
                            </option>

                            <option value={selectedDetail}>
                              {selectedDetail}
                            </option>
                          </select>
                        </div>
                      )}
                    </div>
                  )}

                  {grievanceFormTemplate?.multilingual?.descriptionCheck && (
                    <div className="dropdown-group">
                      <label>{descriptionLabel}</label>
                      <textarea
                        disabled={isViewMode}
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
                    <span style={textStyle("heading-xxs", "primary-grey-80")}>
                      {userDetailsHeading}
                    </span>
                  )}

                  {grievanceFormTemplate?.multilingual?.userInformation[0]
                    .userType &&
                    grievanceFormTemplate?.multilingual?.userInformation[0]
                      .userType.length > 0 && (
                      <div
                        className="dropdown-group"
                        style={{ marginBottom: "0px" }}
                      >
                        <label>{userTypeLabel}</label>
                        <select
                          disabled={isViewMode}
                          value={selectedUserType}
                          onChange={(e) => setSelectedUserType(e.target.value)}
                        >
                          <option value="" disabled>
                            {userTypePlaceholder}
                          </option>
                          {grievanceFormTemplate.multilingual.userInformation[0].userType.map(
                            (type, index) => (
                              <option key={index} value={type}>
                                {type}
                              </option>
                            ),
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
                                  fontSize: "11px",
                                  fontWeight: "600",
                                  fontFamily: FONT_FAMILY_STACK,
                                  marginBottom: "5px",
                                  color: "#767676",
                                }}
                              >
                                {`${detailName} (${requiredLable})`}
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
                                placeholder={`${enterLable} ${detailName.toLowerCase()}`}
                                style={{
                                  width: "100%",
                                  padding: "10px 12px",
                                  border: "1px solid #ccc",
                                  borderRadius: "8px",
                                  fontSize: "11px",
                                  fontWeight: 400,
                                  color: "#333",
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

                  {grievanceFormTemplate?.multilingual?.uploadFiles &&
                    !isViewMode && (
                      <>
                        <div className="">
                          <span style={textStyle("heading-xxs", "primary-grey-80")}>
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
                          >
                            <input
                              type="file"
                              ref={uploadFileInputRef}
                              style={{ display: "none" }}
                              onChange={handleUploadFilieChange}
                              accept=".pdf,.doc,.docx"
                            />

                            <div className="flex items-center justify-center">
                              <FaUpload size={ICON_SIZE} />
                              <span style={textStyle("button", "primary-60")}>
                                {uploadButtonText}
                              </span>
                            </div>
                          </div>

                          <div style={{}}>
                            <span style={textStyle("body-xs", "primary-grey-80")}>
                              {uploadInstruction}
                            </span>
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
                        <FaCheckCircle
                          style={{ color: "green" }}
                          size={ICON_SIZE}
                        />
                        <span style={textStyle("body-xs", "primary-grey-80")}>
                          {uploadFileName}
                        </span>
                      </div>
                      {!isViewMode && (
                        <FaTimes size={ICON_SIZE} onClick={handleRemoveUploadFile} />
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
                      <button
                        onClick={handlePublish}
                        ariaLabel="Submit Request"
                        style={{
                          backgroundColor: "#2563eb",
                          color: "",
                          border: "none",
                          padding: "10px 20px",
                          borderRadius: "999px",
                          fontSize: "11px",
                          fontWeight: "600",
                          cursor: "pointer",
                          transition: "0.2s",
                        }}
                      >
                        Submit Request
                      </button>
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

export default IntegrationGrievanceRequestsDetails;
