import React, { useState, useEffect } from "react";
import "../styles/createGrievanceForm.css";
import {
  ActionButton,
  Icon,
  Image,
  Text,
  Button,
  InputFieldV2,
  TabItem,
  Tabs,
  InputCheckbox,
  InputRadio,
  Selectors,
  InputToggle,
} from "../custom-components";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import GuidedTour from "./GuidedTour";
import TourButton from "./TourButton";
import { createGrievanceTourSteps } from "../utils/tourSteps";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { useRef } from "react";
import Select from "react-select";
import { isSandboxMode } from "../utils/sandboxMode";
import {
  IcCode,
  IcKeyboard,
  IcLanguage,
  IcNotes,
  IcStamp,
  IcVoice,
  IcVoiceRecording,
} from "../custom-components/Icon";
import { createFormTemplate } from "../store/actions/CommonAction";
import { IcChevronDown, IcUpload, IcSuccess, IcClose } from "../custom-components/Icon";
import GrievanceContainer from "./GrivanceContainer";
import FormLanguageContainer from "./FormLanguageContainer";
import FormBranding from "./FormBranding";

export const showLoader = () => {
  const overlay = document.createElement("div");
  overlay.id = "loader-overlay";
  Object.assign(overlay.style, {
    position: "fixed",
    top: "0",
    left: "0",
    width: "100vw",
    height: "100vh",
    backgroundColor: "rgba(0,0,0,0.5)",
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    zIndex: "9999", // higher priority
    backdropFilter: "blur(4px)", // optional subtle blur
  });

  // spinner container
  const spinner = document.createElement("div");
  Object.assign(spinner.style, {
    border: "8px solid #f3f3f3",
    borderTop: "8px solid #3498db",
    borderRadius: "50%",
    width: "60px",
    height: "60px",
    animation: "spin 1s linear infinite",
  });

  // inject keyframes if not already added
  if (!document.getElementById("loader-style")) {
    const style = document.createElement("style");
    style.id = "loader-style";
    style.textContent = `
      @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
      }
    `;
    document.head.appendChild(style);
  }

  overlay.appendChild(spinner);
  document.body.appendChild(overlay);
};
export const hideLoader = () => {
  document.getElementById("loader-overlay")?.remove();
};
const CreateGrievanceForm = () => {
  const DEFAULT_DETAILS = {
    "User access": ["Submit request", "Access data", "Erase data"],
    Legal: ["Raise issue", "Add data", "File complaint"],
  };
  //from state
  const businessId = useSelector((state) => state.common.business_id);
  const tenant_id = useSelector((state) => state.common.tenant_id);
  const sessionToken = useSelector((state) => state.common.session_token);
  const mobile_no = useSelector((state) => state.common.mobile);
  const email = useSelector((state) => state.common.email);
  const companyLogo = useSelector((state) => state.common.companyLogo);

  const logos = new URL("../assets/popup.svg", import.meta.url).href;
  const html = new URL("../assets/formHtml.png", import.meta.url).href;
  const screen = new URL("../assets/formScreen.png", import.meta.url).href;
  const tab = new URL("../assets/formTab.png", import.meta.url).href;

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const [isChecked, setIsChecked] = useState(false);
  const [templateName, setTemplateName] = useState("");
  const [dark, setDark] = useState(false);
  const [mobile, setMobile] = useState(false);
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
  const [buttonText, setButtonText] = useState("Allow all");
  const [saveButtonText, setSaveButtonText] = useState("Save my choices");
  const [others, setOthers] = useState("Others");
  // new for user/grievance
  const [detailsMap, setDetailsMap] = useState([]);
  const [activeInfoTab, setActiveInfoTab] = useState("grievance");
  const [grievanceTypes, setGrievanceTypes] = useState([]); // UPDATE FROM API
  const [selectedType, setSelectedType] = useState("");
  const [selectedGrievanceType, setSelectedGrievanceType] = useState("");
  const [selectedDetail, setSelectedDetail] = useState("");
  const [userData, setUserData] = useState({});
  const [grievanceDesc, setGrievanceDesc] = useState("");
  // for upload component
  const uploadFileInputRef = useRef(null);
  const [uploadFileName, setUploadFileName] = useState("");
  const [uploadFileBase64, setUploadFileBase64] = useState("");
  const [upoladFilePreviewUrl, setUploadFilePreviewUrl] = useState("");
  const [selectedUserType, setSelectedUserType] = useState("");
  const [userDetailInputs, setUserDetailInputs] = useState({});
  const [userDetailOptions, setUserDetailOptions] = useState([]);
  const [grievanceTemplateData, setGrievanceTemplateData] = useState({});
  const [userData2, setUserData2] = useState({});
  const [searchParams] = useSearchParams();
  const templateId = searchParams.get("templateId");
  const templateVersion = searchParams.get("version");
  useEffect(() => {
    setUserData2(JSON.parse(JSON.stringify(userData))); // deep copy
  }, [userData]);

  useEffect(() => {
    const fetchData = async () => {
      if (!templateId) return;

      showLoader();
      try {
        const templateRes = await dispatch(
          getGrievnaceTemplateData(templateId, templateVersion)
        );

        if (templateRes.status === 200) {
          const data = templateRes.data;

          setTemplateName(data.grievanceTemplateName);
          setGrievanceTemplateData(data);
          setSupportedLanguages(data.multilingual.supportedLanguages);
          setLanguage(data.multilingual.supportedLanguages[0] || "");
          setLanguageSpecificContentMap(data.languages);

          // Set title and description based on language
          const selectedLangContent = data.languages["ENGLISH"] || {};
          setTitle(selectedLangContent.heading || "");
          setText(selectedLangContent.description || "");

          // Convert grievanceInformation array → object
          const grievanceInfoObject = (
            data.multilingual?.grievanceInformation || []
          ).reduce((acc, { grievanceType, grievanceItems }) => {
            acc[grievanceType] = grievanceItems;
            return acc;
          }, {});

          // Convert userInformation array → purpose & detail names
          const userInfo = data.multilingual?.userInformation?.[0] || {};

          // Final reverse-mapped userData
          const reversedUserData = {
            grievances: grievanceInfoObject,
            purposeNames: userInfo.userType || [],
            detailNames: userInfo.userItems || [],
            addDescription: data.multilingual?.descriptionCheck || false,
            allowFiles: data.multilingual?.uploadFiles || false,
          };

          // 🔹 Add a 1-second delay before setting user data and dark mode
          await new Promise((resolve) => setTimeout(resolve, 1000));

          setUserData(reversedUserData);

          // 🔹 Update detailsMap with the loaded grievance subcategories so checkboxes display correctly
          // Merge with existing detailsMap to include both API items and saved template items
          setDetailsMap((prevDetailsMap) => {
            const mergedDetailsMap = { ...prevDetailsMap };
            Object.entries(grievanceInfoObject).forEach(([type, items]) => {
              const existingItems = mergedDetailsMap[type] || [];
              // Merge and deduplicate items
              const mergedItems = [...new Set([...existingItems, ...items])];
              mergedDetailsMap[type] = mergedItems;
            });
            return mergedDetailsMap;
          });

          // 🔹 Update grievanceTypes with any types from the loaded template
          setGrievanceTypes((prevTypes) => {
            const templateTypes = Object.keys(grievanceInfoObject);
            return [...new Set([...prevTypes, ...templateTypes])];
          });

          setLogoPreviewUrl(data.uiConfig.logo);
          setLogoName(data.uiConfig.logoName || "file_example_JPG_100kB.jpg");
          setLogoBase64(data.uiConfig?.logo || "logo");
          
          if (data.uiConfig?.typographySettings?.ENGLISH) {
            const typo = data.uiConfig.typographySettings.ENGLISH;
            const fontUrl = typo.fontFile
              ? (typo.fontFile.startsWith('data:') ? typo.fontFile : `data:font/woff2;base64,${typo.fontFile}`)
              : '';
            setFontStyles({
              url: fontUrl,
              family: typo.fontFile ? 'CustomFont' : 'inherit',
              size: typo.fontSize ? typo.fontSize + 'px' : '14px',
              weight: typo.fontWeight || '400',
              style: typo.fontStyle || 'normal'
            });
            if (typo.fontFile) {
              setFontName("custom-font"); // Fallback name
              setFontBase64(fontUrl);
            }
          }

          themeBase64(data.uiConfig?.theme || "");
          setDarkMode(data.uiConfig.darkMode || false);
        }
      } catch (err) {
        console.error("Error fetching data:", err);
        if (err?.[0]?.errorCode === "JCMP3001") {
          setGrievanceTemplateData({});
        }
      } finally {
        // ✅ Hide loader *after* everything (even delays) is done
        hideLoader();
      }
    };

    fetchData();
  }, [templateId, templateVersion, dispatch]);


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

  const handleDetailSelect = (e) => {
    setSelectedDetail(e.target.value);
  };
  const handleTextAreaChange = (e) => {
    // let words = e.target.value.split(/\s+/).filter(Boolean);
    // if (words.length <= wordLimit) {
    //   setGrievanceDesc(e.target.value);
    // } else {
    //   setGrievanceDesc(words.slice(0, wordLimit).join(" "));
    // }
    setGrievanceDesc(e.target.value);
  };

  const handleClick = (type) => {
    setView(type);
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

  const requiredPurposes = purposes.filter((p) => p.isMandatory === "Required");
  const optionalPurposes = purposes.filter((p) => p.isMandatory === "Optional");
  const [parentTabIndex, setParentTabIndex] = useState(0);
  const handleTypeSelect = (e) => {
    const selectedValue = e.target.value;

    // Find the selected option object from userData.grievanceDetails
    const selectedOption = userData.grievanceDetails?.find(
      (opt) => opt.value === selectedValue
    );

    if (selectedOption) {
      setSelectedType(selectedOption.value); // update local state
      onChange({ grievanceType: selectedOption.value }); // call your callback
    }
  };

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

  const handleUploadFilieChange = async (e) => {
    const file = e.target.files[0];
    if (file) await processUploadFile(file);
  };

  const processUploadFile = async (file) => {
    // ✅ SECURE: Validate document file before processing
    try {
      const { validateDocumentFile, formatValidationErrors } = await import('../utils/fileValidation');

      const validationResult = await validateDocumentFile(file);

      if (!validationResult.valid) {
        const errorMessage = formatValidationErrors(validationResult);

        // Show error toast
        const { toast } = await import('react-toastify');
        const CustomToast = (await import('./CustomToastContainer')).default;

        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={errorMessage}
            />
          ),
          { icon: false, autoClose: 5000 }
        );

        // Clear the file input
        if (uploadFileInputRef.current) {
          uploadFileInputRef.current.value = '';
        }
        return;
      }

      console.log('✅ Grievance document validated:', {
        sanitizedFilename: validationResult.sanitizedFilename,
        detectedType: validationResult.fileInfo.detectedType
      });

      // Use sanitized filename
      setUploadFileName(validationResult.sanitizedFilename);

      const reader = new FileReader();
      reader.onloadend = () => {
        const base64String = reader.result.split(",")[1];
        setUploadFileBase64(base64String);
        setUploadFilePreviewUrl(reader.result);
      };
      reader.readAsDataURL(file);

    } catch (error) {
      console.error('❌ Document validation error:', error);
      alert('File validation failed. Please try again.');
    }
  };

  const handleRemoveUploadFile = () => {
    if (uploadFileInputRef.current) {
      uploadFileInputRef.current.value = "";
    }
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

  const [text, setText] = useState(
    "While using (Organization Name), your activities create data which will be used with your consent to offer customised services. Details of data usage are provided below. "
  );
  const [rights, setRights] = useState(
    "To withdraw your consent, exercise your rights, or file complaints with the Board click here."
  );
  const [permission, setPermission] = useState(
    "By clicking ‘Allow all’ or ’Save my choices’, you are providing your consent to Reliance Medlab using your data as outlined above."
  );
  const [supportedLanguages, setSupportedLanguages] = useState(['ENGLISH']);
  const [language, setLanguage] = useState("ENGLISH");
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
  const [mobileView, setMobileView] = useState(false);
  const [parentalControl, setParentalControl] = useState(true);
  const [dataTypeToBeShown, setDataTypeToBeShown] = useState(true);
  const [dataItemToBeShown, setDataItemToBeShown] = useState(true);
  const [processActivityNameToBeShown, setProcessActivityNameToBeShown] =
    useState(true);
  const [processorNameToBeShown, setProcessorNameToBeShown] = useState(true);
  const [validitytoBeShown, setValiditytoBeShown] = useState(true);

  const [fontName, setFontName] = useState("");
  const [fontBase64, setFontBase64] = useState("");
  const fontInputRef = useRef(null);
  const [fontStyles, setFontStyles] = useState({
    url: '',
    family: '',
    size: '14px',
    weight: '400',
    style: 'normal'
  });

  useEffect(() => {
    if (fontBase64) {
      const styleId = 'custom-font-style-grievance-main';
      let style = document.getElementById(styleId);
      if (!style) {
        style = document.createElement("style");
        style.id = styleId;
        document.head.appendChild(style);
      }

      const fontSrc = fontBase64.startsWith("data:")
        ? fontBase64
        : `data:application/octet-stream;base64,${fontBase64}`;

      style.innerHTML = `
        @font-face {
          font-family: '${fontStyles.family || 'CustomFont'}';
          src: url(${fontSrc});
        }
      `;
    }
  }, [fontBase64, fontStyles.family]);

  useEffect(() => {
    if (fontBase64) {
      const styleId = 'custom-font-style-grievance-main';
      let style = document.getElementById(styleId);
      if (!style) {
        style = document.createElement("style");
        style.id = styleId;
        document.head.appendChild(style);
      }

      const fontSrc = fontBase64.startsWith("data:")
        ? fontBase64
        : `data:application/octet-stream;base64,${fontBase64}`;

      style.innerHTML = `
        @font-face {
          font-family: '${fontStyles.family || 'CustomFont'}';
          src: url(${fontSrc});
        }
      `;
    }
  }, [fontBase64, fontStyles.family]);

  useEffect(() => {
    if (fontBase64) {
      const styleId = 'custom-font-style-grievance-main';
      let style = document.getElementById(styleId);
      if (!style) {
        style = document.createElement("style");
        style.id = styleId;
        document.head.appendChild(style);
      }

      const fontSrc = fontBase64.startsWith("data:")
        ? fontBase64
        : `data:application/octet-stream;base64,${fontBase64}`;

      style.innerHTML = `
        @font-face {
          font-family: '${fontStyles.family || 'CustomFont'}';
          src: url(${fontSrc});
        }
      `;
    }
  }, [fontBase64, fontStyles.family]);

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

  const toNumericFontSize = (value, fallback = 12) => {
    if (value === null || value === undefined) return fallback;

    // If it's already a number
    if (typeof value === "number") {
      return Number.isFinite(value) ? Math.round(value) : fallback;
    }

    // If it's a string like "12px", " 12 px ", "12.5px"
    const raw = String(value).trim();

    // Extract first numeric portion
    const match = raw.match(/(\d+(\.\d+)?)/);
    if (!match) return fallback;

    const n = Number(match[1]);
    return Number.isFinite(n) ? Math.round(n) : fallback; // use Math.floor if API requires int
  };


  const removePurpose = (id, index) => {
    if (purposeItems.length === 1) return;
    setPurposeItems(purposeItems.filter((item) => item !== id));
    setPurposes(purposes.filter((_, i) => i !== index));
  };

  const handleBack = () => {
    navigate("/grievanceFormTemplates");
  };

  const buildPayload = (purposes) => {
    return purposes.map((p) => ({
      purposeIds: p.purposeIds,
      mandatory: p.isMandatory === "Required",
      autoRenew: p.autoRenew === "Yes",
      preferenceValidity: p.preferenceValidity,
      processorActivityIds: p.purposeActivityIds,
      // preferenceStatus: "ACCEPTED",
    }));
  };

  const openCertificate = (certificatePreviewUrl) => {
    if (certificatePreviewUrl) {
      window.open(certificatePreviewUrl, "_blank");
    }
  };

  const [finalPayload, setFinalPayload] = useState([]);

  const handlePublish = async () => {
    // In sandbox mode, skip validation and API calls, just show success and redirect
    if (isSandboxMode()) {
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

      // Redirect after short delay to allow toast to show
      setTimeout(() => {
        navigate("/grievanceFormTemplates");
      }, 1500);
      return;
    }

    const errors = [];

    if (!templateName || templateName.trim() === "") {
      errors.push("Please fill valid template name.");
    }

    if (supportedLanguages == []) {
      errors.push("Please select supported languages.");
    }
    if (language == "") {
      errors.push("Please select prefered language.");
    }
    if (logoName == "") {
      errors.push("Please upload logo.");
    }
    // if (fileName == "") {
    //   errors.push("Please upload privacy policy document.");
    // }
    const emptyTypes = Object.entries(userData.grievances || {}).filter(
      ([, details]) => Array.isArray(details) && details.length === 0
    );

    if (emptyTypes.length > 0) {
      const typesWithNoDetails = emptyTypes.map(([type]) => type).join(", ");
      errors.push(
        `Please select at least one grievance detail for: ${typesWithNoDetails}`
      );
    }
    if (!userData?.purposeNames || userData?.purposeNames?.length === 0) {
      errors.push(`Please select at least one User type`);
    } else if (!userData?.detailNames?.includes("Mobile Number")) {
      errors.push(`Mobile Number is required`);
    }
    if (errors.length > 0) {
      toast.error(
        (props) => <CustomToast {...props} type="error" message={errors[0]} />,
        { icon: false }
      );
      return;
    } else {
      const formattedGrievanceInformation = Object.entries(
        userData?.grievances || {}
      ).map(([grievanceType, grievanceItems]) => ({
        grievanceType,
        grievanceItems,
      }));
      // request body
      const requestBody = {
        ...(!templateId && {
          grievanceTemplateName: templateName || "DefaultConsent",
        }),
        businessId: businessId,
        multilingual: {
          enabled: true,
          supportedLanguages: supportedLanguages,
          grievanceInformation: formattedGrievanceInformation,
          userInformation: [
            {
              userType: userData?.purposeNames,
              userItems: userData?.detailNames,
            },
          ],
          descriptionCheck: userData?.addDescription || false,
          uploadFiles: userData?.allowFiles || false,
        },
        languages: languageSpecificContentMap,

        uiConfig: {
          logo: logoBase64 || "samplelogo",
          theme: themeBase64 || "sampletheme",
          darkMode: darkMode,
          mobileView: mobileView,
          logoName: logoName,
        },
        typography: {
          fontFile: "JioType",
        },
        status: "PUBLISHED",
      };

      try {
        // In sandbox mode, use default values if missing
        const finalBusinessId = businessId || (isSandboxMode() ? 'sandbox-business-id' : null);
        const finalTenantId = tenant_id || (isSandboxMode() ? 'sandbox-tenant-id' : null);
        const finalToken = sessionToken || (isSandboxMode() ? 'sandbox-session-token-12345' : null);

        if (!finalToken || !finalBusinessId || !finalTenantId) {
          if (!isSandboxMode()) {
            toast.error(
              (props) => (
                <CustomToast
                  {...props}
                  type="error"
                  message="Credentials are missing. Please refresh and try again."
                />
              ),
              { icon: false }
            );
            return;
          }
        }

        const res = await createFormTemplate(
          requestBody,
          finalTenantId,
          finalBusinessId,
          finalToken,
          templateId
        );

        // In sandbox mode, the response might be a mock response object
        const status = res?.status || (res?.ok ? 200 : 500);

        if (status === 200 || status === 201) {
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

          // Redirect after short delay to allow toast to show
          setTimeout(() => {
            navigate("/grievanceFormTemplates");
          }, 1500);
        } else {
          // Try to get error message from response
          let errorMsg = "Something went wrong. Please try again.";
          try {
            if (res?.json) {
              const data = await res.json();
              errorMsg = data?.msg || data?.message || errorMsg;
            } else if (res?.msg) {
              errorMsg = res.msg;
            }
          } catch (e) {
            // Ignore JSON parse errors
          }

          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={errorMsg}
              />
            ),
            { icon: false }
          );
        }
      } catch (err) {
        console.error("Error publishing grievance template:", err);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={
                err?.response?.data?.msg ||
                err?.message ||
                "Something went wrong. Please try again."
              }
            />
          ),
          { icon: false }
        );
      }
    }
  };

  const handleDraft = async () => {
    // In sandbox mode, skip validation and API calls, just show success and redirect
    if (isSandboxMode()) {
      toast.success(
        (props) => (
          <CustomToast
            {...props}
            type="success"
            message={"Form Template Saved successfully."}
          />
        ),
        { icon: false }
      );

      // Redirect after short delay to allow toast to show
      setTimeout(() => {
        navigate("/grievanceFormTemplates");
      }, 1500);
      return;
    }

    const errors = [];

    if (!templateName || templateName.trim() === "") {
      errors.push("Please fill valid template name.");
    }

    if (supportedLanguages == []) {
      errors.push("Please select supported languages.");
    }
    if (language == "") {
      errors.push("Please select prefered language.");
    }
    if (logoName == "") {
      errors.push("Please upload logo.");
    }
    // if (fileName == "") {
    //   errors.push("Please upload privacy policy document.");
    // }

    const emptyTypes = Object.entries(userData.grievances || {}).filter(
      ([, details]) => Array.isArray(details) && details.length === 0
    );

    if (emptyTypes.length > 0) {
      const typesWithNoDetails = emptyTypes.map(([type]) => type).join(", ");
      errors.push(
        `Please select at least one grievance detail for: ${typesWithNoDetails}`
      );
    }
    if (!userData?.purposeNames || userData?.purposeNames?.length === 0) {
      errors.push(`Please select at least one User type`);
    }
    if (!userData?.detailNames || userData?.detailNames?.length === 0) {
      errors.push(`Please select at least one User detail`);
    } else if (!userData?.detailNames?.includes("Mobile Number")) {
      errors.push(`Mobile Number is required`);
    }
    if (errors.length > 0) {
      toast.error(
        (props) => <CustomToast {...props} type="error" message={errors[0]} />,
        { icon: false }
      );
      return;
    } else {
      // request body
      const formattedGrievanceInformation = Object.entries(
        userData?.grievances || {}
      ).map(([grievanceType, grievanceItems]) => ({
        grievanceType,
        grievanceItems,
      }));
      // request body
      const requestBody = {
        ...(!templateId && {
          grievanceTemplateName: templateName || "DefaultConsent",
        }),
        businessId: businessId,
        multilingual: {
          enabled: true,
          supportedLanguages: supportedLanguages,
          grievanceInformation: formattedGrievanceInformation,
          userInformation: [
            {
              userType: userData?.purposeNames,
              userItems: userData?.detailNames,
            },
          ],
          descriptionCheck: userData?.addDescription || false,
          uploadFiles: userData?.allowFiles || false,
        },
        languages: languageSpecificContentMap,

        uiConfig: {
          logo: logoBase64 || "samplelogo",
          theme: themeBase64 || "sampletheme",
          darkMode: darkMode,
          mobileView: mobileView,
          logoName: logoName,
        },
        typography: {
          fontFile: "JioType",
        },
        status: "DRAFT",
      };
      try {
        // In sandbox mode, use default values if missing
        const finalBusinessId = businessId || (isSandboxMode() ? 'sandbox-business-id' : null);
        const finalTenantId = tenant_id || (isSandboxMode() ? 'sandbox-tenant-id' : null);
        const finalToken = sessionToken || (isSandboxMode() ? 'sandbox-session-token-12345' : null);

        if (!finalToken || !finalBusinessId || !finalTenantId) {
          if (!isSandboxMode()) {
            toast.error(
              (props) => (
                <CustomToast
                  {...props}
                  type="error"
                  message="Credentials are missing. Please refresh and try again."
                />
              ),
              { icon: false }
            );
            return;
          }
        }

        const res = await createFormTemplate(
          requestBody,
          finalTenantId,
          finalBusinessId,
          finalToken,
          templateId
        );

        // In sandbox mode, the response might be a mock response object
        const status = res?.status || (res?.ok ? 200 : 500);

        if (status === 200 || status === 201) {
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"Form Template Saved successfully."}
              />
            ),
            { icon: false }
          );

          // Redirect after short delay to allow toast to show
          setTimeout(() => {
            navigate("/grievanceFormTemplates");
          }, 1500);
        } else {
          // Try to get error message from response
          let errorMsg = "Something went wrong. Please try again.";
          try {
            if (res?.json) {
              const data = await res.json();
              errorMsg = data?.msg || data?.message || errorMsg;
            } else if (res?.msg) {
              errorMsg = res.msg;
            }
          } catch (e) {
            // Ignore JSON parse errors
          }

          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={errorMsg}
              />
            ),
            { icon: false }
          );
        }
      } catch (err) {
        console.error("Error saving grievance template:", err);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={
                err?.response?.data?.msg ||
                err?.message ||
                "Something went wrong. Please try again."
              }
            />
          ),
          { icon: false }
        );
      }
    }
  };

  //accessibility tabs

  const [activeTab, setActiveTab] = useState("popup");

  const renderContent = () => {
    switch (activeTab) {
      case "semantics":
        return (
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              gap: "20px",
              marginTop: "20px",
            }}
          >
            <div class="landmark-card">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div class="landmark-badge">
                  <span class="landmark-number">2</span>
                  <span class="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Landmark Details
                </Text>
              </div>
              <div class="landmark-body">
                <Text appearance="body-s" color="primary-grey-100">
                  element
                </Text>
                <span class="landmark-tag">&lt;div&gt;</span>
              </div>
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  justifyContent: "space-between",
                  width: "100%",
                  padding: "5px",
                }}
              >
                <Text appearance="body-xs" color="primary-grey-80">
                  accessible label
                </Text>
                <Text appearance="body-xxs" color="primary-grey-80">
                  Cookie consent options
                </Text>
              </div>
            </div>
            <div class="landmark-card-2">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div class="landmark-badge-2">
                  <span class="landmark-number">1</span>
                  <span class="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Register your grievance
                </Text>
              </div>
              <div class="landmark-body">
                <Text appearance="body-s" color="primary-grey-80">
                  Fill this form to register your grievance or enquire for more
                  information.
                </Text>
              </div>
            </div>
            <div class="landmark-card-3">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div class="landmark-badge-3">
                  <span class="landmark-number">h2</span>
                  <span class="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Select Language
                </Text>
              </div>
              <div
                class="landmark-body"
                marginBottom="0px"
                style={{ borderBottom: "solid 1px #e0e0e0" }}
              >
                <Text appearance="body-s" color="primary-grey-100">
                  element
                </Text>
                <span class="landmark-tag">select</span>
              </div>
              <div
                class="landmark-body"
                style={{ borderBottom: "solid 1px #e0e0e0" }}
              >
                <Text appearance="body-s" color="primary-grey-100">
                  required
                </Text>
                <span class="landmark-tag">true</span>
              </div>
              <div
                class="landmark-body"
                style={{ borderBottom: "solid 1px #e0e0e0" }}
              >
                <Text appearance="body-s" color="primary-grey-100">
                  autocomplete
                </Text>
                <span class="landmark-tag">language</span>
              </div>
            </div>
            <div class="landmark-card-3">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div class="landmark-badge-3">
                  <span class="landmark-number">h2</span>
                  <span class="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Fill Grievance details
                </Text>
              </div>
            </div>
            <div class="landmark-card-4">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div class="landmark-badge-3">
                  <span class="landmark-number">h2</span>
                  <span class="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Fill Grievance Type
                </Text>
              </div>
            </div>
          </div>
        );
      case "focus":
        return (
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              gap: "20px",
              marginTop: "20px",
            }}
          >
            <div class="landmark-card-7">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div class="landmark-badge-7">
                  <span class="landmark-number">1</span>
                  <span class="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Focus Order Details
                </Text>
              </div>
              <div class="landmark-body-2">
                <Text appearance="body-s" color="primary-grey-80">
                  {" "}
                  • When the user is navigating with a keyboard using the tab
                  key, the focus will be on elements that are interactible - eg.
                  buttons, links, checkboxes, etc.{" "}
                </Text>
                <br></br>
                <Text appearance="body-s" color="primary-grey-80">
                  • They will be taken through these elements in the order
                  listed on the preview screen
                </Text>
              </div>
            </div>
          </div>
        );
      case "screen":
        return (
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              gap: "20px",
              marginTop: "20px",
            }}
          >
            <div class="landmark-card-8">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div class="landmark-badge-8">
                  <span class="landmark-number">1</span>
                  <span class="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Reading Order Details
                </Text>
              </div>
              <div class="landmark-body-2">
                <Text appearance="body-s" color="primary-grey-80">
                  {" "}
                  • When user is navigating using a screen reader, it will be
                  reading the content of the screen in the order listed on the
                  preview.
                </Text>
                <br></br>
                <Text appearance="body-s" color="primary-grey-80">
                  • The screen reader will be reading out the accessible names,
                  and ARIA labels as mentioned on the HTML semantics page for
                  each element.{" "}
                </Text>
              </div>
            </div>
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="page-container">
      <div className="main-content">
        <div className="left-half">
          <div className="header-container">
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
                size="small"
                state="normal"
              />
            </div>
            <div className="header-right">
              <Text appearance="heading-xs" color="primary-grey-100">
                Create grievance form template
              </Text>

              <div style={{ width: "80%", marginTop: "10px" }}>
                <InputFieldV2
                  className="systemConfig-input"
                  label="Grievance Form Template Name"
                  helperText={!templateId && "Enter a unique name to identify this template"}
                  value={templateName}
                  disabled={templateId ? true : false}
                  onChange={(e) => setTemplateName(e.target.value)}
                  placeholder="e.g., Customer Complaint Form, Data Request Form"
                  size="small"
                ></InputFieldV2>
              </div>
            </div>
          </div>

          <div className="cc-body">
            <div className="notif-tabs">
              <Tabs
                appearance="normal"
                onTabChange={(index) => {
                  setParentTabIndex(index);

                  // ✅ Reset accessibility subtab when switching away
                  if (index !== 3) {
                    // assuming Accessibility is the 4th tab (0-based index = 3)
                    setActiveTab("popup");
                  }
                }}
                onNextClick={function noRefCheck() { }}
                onPrevClick={function noRefCheck() { }}
                onScroll={function noRefCheck() { }}
                overflow="fit"
                active={parentTabIndex}
              >
                <TabItem
                  // onClick={setActiveTab("popup")}
                  label={
                    <Text appearance="body-xs" color="primary-grey-80">
                      Form Configuration
                    </Text>
                  }
                  children={
                    <div>
                      <div
                        style={{
                          background: "#e7f3ff",
                          padding: "12px 16px",
                          borderRadius: "8px",
                          marginBottom: "20px",
                          border: "1px solid #bfdeff",
                        }}
                      >
                        <Text
                          appearance="body-xs-bold"
                          color="primary-grey-100"
                        >
                          ℹ️ Configure Form Fields
                        </Text>
                        <br />
                        <Text appearance="body-xs" color="primary-grey-80">
                          Set up the grievance categories and user information
                          fields that will be collected when users submit this
                          form.
                        </Text>
                      </div>
                      {purposeItems.map((id, index) => (
                        <GrievanceContainer
                          key={id}
                          index={index}
                          purposeNumber={index + 1}
                          userData={userData}
                          onChange={(updated) => {
                            setUserData((prev) => ({
                              ...prev,
                              ...updated,
                            }));
                          }}
                          onClear={() => removePurpose(id, index)}
                          isFirst={purposeItems.length === 1}
                          processingActivityList={processingActivityList}
                          setProcessingActivityList={setProcessingActivityList}
                          activityOptions={activityOptions}
                          setActivityOptions={setActivityOptions}
                          purposeOptions={purposeOptions}
                          setPurposeOptions={setPurposeOptions}
                          userDetailOptions={userDetailOptions}
                          setUserDetailOptions={setUserDetailOptions}
                          purposeList={purposeList}
                          setPurposeList={setPurposeList}
                          processingActivityOption={processingActivityOptions}
                          setProcessingActivityOptions={
                            setProcessingActivityOptions
                          }
                          detailsMap={detailsMap}
                          setDetailsMap={setDetailsMap}
                          activeInfoTab={activeInfoTab}
                          setActiveInfoTab={setActiveInfoTab}
                          grievanceTypes={grievanceTypes}
                          setGrievanceTypes={setGrievanceTypes}
                          selectedType={selectedType}
                          setSelectedType={setSelectedType}
                          tenant_id={tenant_id}
                          businessId={businessId}
                        />
                      ))}
                    </div>
                  }
                />
                <TabItem
                  label={
                    <Text appearance="body-xs" color="primary-grey-80">
                      Language
                    </Text>
                  }
                  children={
                    <div>
                      <FormLanguageContainer
                        text={text}
                        setText={setText}
                        permission={permission}
                        setPermission={setPermission}
                        rights={rights}
                        setRights={setRights}
                        supportedLanguages={supportedLanguages}
                        setSupportedLanguages={setSupportedLanguages}
                        language={language}
                        setLanguage={setLanguage}
                        label={label}
                        setLabel={setLabel}
                        languageSpecificContentMap={languageSpecificContentMap}
                        setLanguageSpecificContentMap={
                          setLanguageSpecificContentMap
                        }
                        title={title}
                        setTitle={setTitle}
                        cid={cid}
                        setCid={setCid}
                        age={age}
                        setAge={setAge}
                        purposes={purposes}
                        onPurposesChange={(updatedPurposes) =>
                          setPurposes(updatedPurposes)
                        }
                        purposeHeading={purposeHeading}
                        setPurposeHeading={setPurposeHeading}
                        dataItemHeading={dataItemHeading}
                        setDataItemHeading={setDataItemHeading}
                        usedByHeading={usedByHeading}
                        setUsedByHeading={setUsedByHeading}
                        durationHeading={durationHeading}
                        setDurationHeading={setDurationHeading}
                        processingHeading={processingHeading}
                        setProcessingHeading={setProcessingHeading}
                        buttonText={buttonText}
                        setButtonText={setButtonText}
                        saveButtonText={saveButtonText}
                        setSaveButtonText={setSaveButtonText}
                        others={others}
                        setOthers={setOthers}
                        userData2={userData2}
                        //for translation
                        grievanceDetailsHeading={grievanceDetailsHeading}
                        setGrievanceDetailsHeading={setGrievanceDetailsHeading}
                        selectCategoryPlaceholder={selectCategoryPlaceholder}
                        setSelectCategoryPlaceholder={
                          setSelectCategoryPlaceholder
                        }
                        // newly added for translation
                        grievanceCategoryLabel={grievanceCategoryLabel}
                        setGrievanceCategoryLabel={setGrievanceCategoryLabel}
                        grievanceSubCategoryLabel={grievanceSubCategoryLabel}
                        setGrievanceSubCategoryLabel={
                          setGrievanceSubCategoryLabel
                        }
                        subCategoryPlaceholder={subCategoryPlaceholder}
                        setSubCategoryPlaceholder={setSubCategoryPlaceholder}
                        descriptionLabel={descriptionLabel}
                        setDescriptionLabel={setDescriptionLabel}
                        descriptionPlaceholder={descriptionPlaceholder}
                        setDescriptionPlaceholder={setDescriptionPlaceholder}
                        userDetailsHeading={userDetailsHeading}
                        setUserDetailsHeading={setUserDetailsHeading}
                        userTypeLabel={userTypeLabel}
                        setUserTypeLabel={setUserTypeLabel}
                        userTypePlaceholder={userTypePlaceholder}
                        setUserTypePlaceholder={setUserTypePlaceholder}
                        uploadFilesHeading={uploadFilesHeading}
                        setUploadFilesHeading={setUploadFilesHeading}
                        uploadButtonText={uploadButtonText}
                        setUploadButtonText={setUploadButtonText}
                        uploadInstruction={uploadInstruction}
                        setUploadInstruction={setUploadInstruction}
                        requiredLable={requiredLable}
                        setRequiredLable={setRequiredLable}
                        enterLable={enterLable}
                        setEnterLable={setEnterLable}
                      />
                    </div>
                  }
                />
                <TabItem
                  // onClick={setActiveTab("popup")}
                  label={
                    <Text appearance="body-xs" color="primary-grey-80">
                      Branding
                    </Text>
                  }
                  children={
                    <div>
                      <FormBranding
                        fileName={fileName}
                        setFileName={setFileName}
                        certificatePreviewUrl={certificatePreviewUrl}
                        setCertificatePreviewUrl={setCertificatePreviewUrl}
                        fileInputRef={fileInputRef}
                        fileBase64={fileBase64}
                        setFileBase64={setFileBase64}
                        logoPreviewUrl={logoPreviewUrl}
                        setLogoPreviewUrl={setLogoPreviewUrl}
                        logoName={logoName}
                        setLogoName={setLogoName}
                        logoInputRef={logoInputRef}
                        logoBase64={logoBase64}
                        setLogoBase64={setLogoBase64}
                        fileSize={fileSize}
                        setFileSize={setFileSize}
                        darkMode={darkMode}
                        setDarkMode={setDarkMode}
                        mobileView={mobileView}
                        setMobileView={setMobileView}
                        parentalControl={parentalControl}
                        setParentalControl={setParentalControl}
                        dataTypeToBeShown={dataTypeToBeShown}
                        setDataTypeToBeShown={setDataTypeToBeShown}
                        dataItemToBeShown={dataItemToBeShown}
                        setDataItemToBeShown={setDataItemToBeShown}
                        processActivityNameToBeShown={
                          processActivityNameToBeShown
                        }
                        setProcessActivityNameToBeShown={
                          setProcessActivityNameToBeShown
                        }
                        processorNameToBeShown={processorNameToBeShown}
                        setProcessorNameToBeShown={setProcessorNameToBeShown}
                        validitytoBeShown={validitytoBeShown}
                        setValiditytoBeShown={setValiditytoBeShown}
                        colors={colors}
                        setColors={setColors}
                        darkColors={darkColors}
                        setDarkColors={setDarkColors}
                        fontName={fontName}
                        setFontName={setFontName}
                        fontBase64={fontBase64}
                        setFontBase64={setFontBase64}
                        fontInputRef={fontInputRef}
                        fontStyles={fontStyles}
                        setFontStyles={setFontStyles}
                      />
                    </div>
                  }
                />
                <TabItem
                  label={
                    <Text appearance="body-xs" color="primary-grey-80">
                      Accessibility
                    </Text>
                  }
                  children={
                    <div className="acc-con">
                      <div style={{ marginTop: "20px", padding: "15px 20px" }}>
                        <Text appearance="heading-xxs" color="primary-grey-80">
                          Element properties
                        </Text>
                        <br></br>
                        <Text appearance="body-xs" color="primary-grey-80">
                          Accessibility properties of elements and preview how
                          they will be experienced by the user
                        </Text>
                      </div>
                      <div className="tabs-container">
                        <div className="tabs">
                          <button
                            className={`tab-btn ${activeTab === "semantics" ? "active" : ""
                              }`}
                            onClick={() => setActiveTab("semantics")}
                          >
                            <IcCode height={25} width={25} />
                            <Text
                              appearance="body-xs"
                              color={
                                activeTab === "semantics"
                                  ? "primary-inverse"
                                  : "primary-60"
                              }
                            >
                              HTML semantics
                            </Text>
                          </button>
                          <button
                            className={`tab-btn ${activeTab === "focus" ? "active" : ""
                              }`}
                            onClick={() => setActiveTab("focus")}
                          >
                            <IcKeyboard height={25} width={25} />{" "}
                            <Text
                              appearance="body-xs"
                              color={
                                activeTab === "focus"
                                  ? "primary-inverse"
                                  : "primary-60"
                              }
                            >
                              Keyboard focus order
                            </Text>
                          </button>
                          <button
                            className={`tab-btn ${activeTab === "screen" ? "active" : ""
                              }`}
                            onClick={() => setActiveTab("screen")}
                          >
                            <IcVoice height={25} width={25} />
                            <Text
                              appearance="body-xxs"
                              color={
                                activeTab === "screen"
                                  ? "primary-inverse"
                                  : "primary-60"
                              }
                            >
                              Screen reader reading order
                            </Text>
                          </button>
                        </div>
                        <div className="tab-content">{renderContent()}</div>
                      </div>
                    </div>
                  }
                />
              </Tabs>
            </div>
          </div>
        </div>
        <div className="right-half">
          <div className="preview-header">
            <Text appearance="body-s-bold" color="primary-grey-100">
              Preview
            </Text>
            <InputToggle
              checked={mobile}
              labelPosition="left"
              onChange={() => setMobile((prev) => !prev)}
              size="small"
              type="toggle"
              label="Mobile view"
            />

            <InputToggle
              checked={dark}
              labelPosition="left"
              onChange={() => setDarkMode((prev) => !prev)}
              size="small"
              type="toggle"
              label="Dark mode"
            />
          </div>
          {/* <div className='popup'> */}

          {activeTab === "popup" && (
            // <div className={`popup ${mobile ? "popup-mobile" : ""}`}>
            <div
              className={`popup ${mobile ? "popup-mobile" : ""} ${dark ? "popup-dark" : ""
                }`}
              style={{
                backgroundColor: activePopupColors.cardBackground,
                color: activePopupColors.cardFont,
                fontFamily: fontStyles.family,
                fontSize: fontStyles.size,
                fontWeight: fontStyles.weight,
                fontStyle: fontStyles.style,
              }}
            >
              <style>
                {`
                  .popup, .popup * {
                    font-family: ${fontStyles.family} !important;
                    font-size: ${fontStyles.size} !important;
                    font-weight: ${fontStyles.weight} !important;
                    font-style: ${fontStyles.style} !important;
                  }
                  .popup button, .popup input, .popup select, .popup textarea {
                    font-family: inherit !important;
                    font-size: inherit !important;
                    font-weight: inherit !important;
                    font-style: inherit !important;
                  }
                `}
              </style>
              {/* <div className="close-button">
                <Icon
                  color="primary"
                  ic="ic_close"
                  kind="default"
                  onClick={function noRefCheck() {}}
                  size="medium"
                />
              </div> */}
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
                      fontFamily: "inherit",
                      fontSize: "inherit",
                      fontWeight: "inherit",
                      fontStyle: "inherit",
                    }}
                  >
                    {title}
                  </h1>
                  {!mobile && (
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
                  )}
                </div>

                {logoPreviewUrl && (
                  <img
                    src={logoPreviewUrl}
                    alt="Logo"
                    style={{
                      width: mobile ? "28px" : "40px",
                      height: mobile ? "28px" : "40px",
                      marginLeft: mobile ? "5px" : "10px",
                    }}
                  />
                )}
              </div>

              <div className="popup-body">
                <p
                  className="custom-text-p"
                  style={{
                    backgroundColor: activePopupColors.cardBackground,
                    color: activePopupColors.cardFont,
                    marginBottom: "8px",
                    fontSize: "inherit",
                    letterSpacing: "-0.5px",
                    // color: "rgba(0, 0, 0, 0.65)",
                    fontWeight: "inherit",
                    fontFamily: "inherit",
                    fontStyle: "inherit",
                  }}
                >
                  {text}
                </p>
                {userData2.grievances &&
                  Object.keys(userData2.grievances).length > 0 && (
                    <div className="">
                      <Text
                        appearance="heading-xxs"
                        style={{
                          backgroundColor: activePopupColors.cardBackground,
                          color: activePopupColors.cardFont,
                          marginBottom: "8px",
                        }}
                      >
                        {grievanceDetailsHeading}
                      </Text>
                    </div>
                  )}

                {userData2.grievances &&
                  Object.keys(userData2.grievances).length > 0 && (
                    <div className="dropdown-group">
                      <label
                        style={{
                          backgroundColor: activePopupColors.cardBackground,
                          color: activePopupColors.cardFont,
                          marginBottom: "8px",
                        }}
                      >
                        {grievanceCategoryLabel}
                      </label>
                      <select
                        value={selectedGrievanceType}
                        onChange={(e) => {
                          const newType = e.target.value;
                          setSelectedGrievanceType(newType);
                          setSelectedDetail("");
                        }}
                        style={{
                          width: "100%",
                          backgroundColor: activePopupColors.cardBackground,
                          color: activePopupColors.cardFont,
                        }}
                      >
                        <option
                          value=""
                          style={{
                            color: activePopupColors.cardFont,
                            backgroundColor: activePopupColors.cardBackground,
                          }}
                        >
                          {selectCategoryPlaceholder}
                        </option>
                        {Object.keys(userData2.grievances).map((type) => (
                          <option key={type} value={type}>
                            {type}
                          </option>
                        ))}
                      </select>
                    </div>
                  )}

                {selectedGrievanceType &&
                  userData2.grievances[selectedGrievanceType] &&
                  userData2.grievances[selectedGrievanceType].length > 0 && (
                    <div className="dropdown-group">
                      <label
                        style={{
                          backgroundColor: activePopupColors.cardBackground,
                          color: activePopupColors.cardFont,
                        }}
                      >
                        {grievanceSubCategoryLabel}
                      </label>
                      <select
                        value={selectedDetail}
                        onChange={(e) => setSelectedDetail(e.target.value)}
                        style={{
                          width: "100%",
                          backgroundColor: activePopupColors.cardBackground,
                          color: activePopupColors.cardFont,
                        }}
                      >
                        <option
                          value=""
                          style={{
                            color: activePopupColors.cardFont,
                            backgroundColor: activePopupColors.cardBackground,
                          }}
                        >
                          {subCategoryPlaceholder}
                        </option>
                        {userData2.grievances[selectedGrievanceType].map(
                          (detail, idx) => (
                            <option key={idx} value={detail}>
                              {detail}
                            </option>
                          )
                        )}
                      </select>
                    </div>
                  )}

                {userData2.addDescription && (
                  <div
                    className="dropdown-group"
                    style={{ padding: "0px 15px" }}
                  >
                    <label
                      style={{
                        backgroundColor: activePopupColors.cardBackground,
                        color: activePopupColors.cardFont,
                      }}
                    >
                      {descriptionLabel}
                    </label>
                    <style>
                      {`
                        .custom-text-area::placeholder {
                          color: ${activePopupColors.cardFont};
                          opacity: 0.7;
                        }
                      `}
                    </style>
                    <textarea
                      placeholder={descriptionPlaceholder}
                      value={grievanceDesc}
                      onChange={handleTextAreaChange}
                      rows="3"
                      className="custom-text-area"
                      style={{
                        width: "100%",
                        backgroundColor: activePopupColors.cardBackground,
                        color: activePopupColors.cardFont,
                        borderColor: activePopupColors.cardFont,
                      }}
                    />
                  </div>
                )}

                {(userData2.purposeNames?.length > 0 ||
                  userData2.detailNames?.length > 0) && (
                    <Text
                      appearance="heading-xxs"
                      style={{
                        backgroundColor: activePopupColors.cardBackground,
                        color: activePopupColors.cardFont,
                      }}
                    >
                      {userDetailsHeading}
                    </Text>
                  )}

                {userData2.purposeNames &&
                  userData2.purposeNames.length > 0 && (
                    <div
                      className="dropdown-group"
                      style={{ marginBottom: "0px" }}
                    >
                      <label
                        style={{
                          backgroundColor: activePopupColors.cardBackground,
                          color: activePopupColors.cardFont,
                        }}
                      >
                        {userTypeLabel}
                      </label>
                      <select
                        value={selectedUserType}
                        onChange={(e) => setSelectedUserType(e.target.value)}
                        style={{
                          width: "100%",
                          backgroundColor: activePopupColors.cardBackground,
                          color: activePopupColors.cardFont,
                        }}
                      >
                        <option
                          value=""
                          style={{
                            color: activePopupColors.cardFont,
                            backgroundColor: activePopupColors.cardBackground,
                          }}
                        >
                          {userTypePlaceholder}
                        </option>
                        {userData2.purposeNames.map((type, index) => (
                          <option key={index} value={type}>
                            {type}
                          </option>
                        ))}
                      </select>
                    </div>
                  )}

                {userData2.detailNames && userData2.detailNames.length > 0 && (
                  <div>
                    {userData2.detailNames.map((detailName, index) => (
                      <div
                        key={index}
                        className="systemConfig-input"
                        style={{
                          padding: "10px 14px",
                          "--card-bg": activePopupColors.cardBackground,
                          "--card-font": activePopupColors.cardFont,
                          "--card-placeholder": activePopupColors.cardFont,
                        }}
                      >
                        <InputFieldV2
                          label={`${detailName} (${requiredLable})`}
                          value={userDetailInputs[detailName] || ""}
                          onChange={(e) =>
                            setUserDetailInputs({
                              ...userDetailInputs,
                              [detailName]: e.target.value,
                            })
                          }
                          placeholder={`${enterLable} ${detailName.toLowerCase()}`}
                          size="medium"
                        />
                      </div>
                    ))}
                  </div>
                )}

                {userData2.allowFiles && (
                  <>
                    <div className="">
                      <Text
                        appearance="heading-xxs"
                        style={{
                          backgroundColor: activePopupColors.cardBackground,
                          color: activePopupColors.cardFont,
                        }}
                      >
                        {uploadFilesHeading}
                      </Text>
                    </div>

                    <div
                      style={{
                        padding: "0px 15px",
                        marginBottom: "20px",
                        paddingBottom: "20px",
                      }}
                    >
                      <div
                        className="fileUploader-custom1"
                        onClick={handleUploadFilesClick}
                        onDragOver={handleUploadFilesDragOver}
                        onDragLeave={handleUploadFilesDragLeave}
                        onDrop={handleUploadFilesDrop}
                        style={{
                          backgroundColor: activePopupColors.cardBackground,
                          color: activePopupColors.cardFont,
                        }}
                      >
                        <input
                          type="file"
                          ref={uploadFileInputRef}
                          style={{ display: "none" }}
                          onChange={handleUploadFilieChange}
                          accept=".pdf,.doc,.docx"
                        />

                        <div className="flex items-center justify-center" style={{ color: activePopupColors.cardFont }} fill={activePopupColors.cardFont} >
                          <Icon
                            ic={<IcUpload height={23} width={23} stroke={activePopupColors.cardFont} />}
                            color={activePopupColors.cardFont}
                          />
                          <Text appearance="button" style={{
                            backgroundColor: activePopupColors.cardBackground,
                            color: activePopupColors.cardFont,
                          }}>
                            {uploadButtonText}
                          </Text>
                        </div>
                      </div>

                      <div>
                        <Text
                          appearance="body-xs"
                          style={{
                            backgroundColor: activePopupColors.cardBackground,
                            color: activePopupColors.cardFont,
                          }}
                        >
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
                      style={{
                        backgroundColor: activePopupColors.cardBackground,
                        color: activePopupColors.cardFont,
                      }}
                    >
                      <Icon
                        ic={<IcSuccess width={15} height={15} />}
                        color="feedback_success_50"
                      />
                      <Text appearance="body-xs" style={{
                        backgroundColor: activePopupColors.cardBackground,
                        color: activePopupColors.cardFont
                      }}>
                        {uploadFileName}
                      </Text>
                    </div>
                    <Icon
                      ic={<IcClose width={15} height={15} />}
                      color="primary_60"
                      className="selecetd-file-display"
                      onClick={handleRemoveUploadFile}
                    />
                  </div>
                )}

                {/* <p
                  className="custom-text-p"
                  style={{
                    backgroundColor: activePopupColors.cardBackground,
                    color: activePopupColors.cardFont,
                    marginBottom: "8px",
                  }}
                >
                  {cid}: {mobile_no || email}{" "}
                </p> */}

                {/* <div className="accordion">
                  {requiredPurposes.length > 0 && (
                    <AccordionItem
                      title={label}
                      alwaysActive
                      items={requiredPurposes}
                    />
                  )}
                  {optionalPurposes.length > 0 && (
                    <AccordionItem
                      title={others}
                      hasCheckbox
                      items={optionalPurposes}
                    />
                  )}
                </div>
                <p
                  className="custom-text-p"
                  style={{
                    backgroundColor: activePopupColors.cardBackground,
                    color: activePopupColors.cardFont,
                    marginBottom: "8px",
                  }}
                >
                  {rights}
                </p> */}

                {/* {fileName && certificatePreviewUrl && (
                  // <p>
                  //   <a
                  //     href={certificatePreviewUrl}
                  //     target="_blank"
                  //     rel="noopener noreferrer"
                  //     style={{
                  //       color: "#007bff",
                  //       textDecoration: "none",
                  //       cursor: "pointer",
                  //     }}
                  //   >
                  //     {fileName}
                  //   </a>
                  // </p>
                  <span onClick={() => openCertificate(certificatePreviewUrl)}>
                    <Text
                      appearance="body-s-bold"
                      style={{
                        color: activePopupColors.linkFont,
                        cursor: "pointer",
                      }}
                    >
                      {fileName}
                    </Text>
                  </span>
                )} */}

                {/* {parentalControl && (
                  <div
                    style={{
                      display: "flex",
                      flexDirection: "row",
                      alignItems: "center",
                      marginBottom: "5px",
                      gap: "10px",
                    }}
                  >
                    <InputCheckbox
                      helperText=""
                      label=""
                      name="Name"
                      checked={isChecked}
                      onClick={() => setIsChecked((prev) => !prev)}
                      size="small"
                      state="none"
                    />
                    <p
                      className="custom-text-p"
                      style={{
                        backgroundColor: activePopupColors.cardBackground,
                        color: activePopupColors.cardFont,
                      }}
                    >
                      {age}
                    </p>
                  </div>
                )} */}

                {/* <p
                  className="custom-text-p"
                  style={{
                    backgroundColor: activePopupColors.cardBackground,
                    color: activePopupColors.cardFont,
                    marginBottom: "8px",
                  }}
                >
                  {permission}
                </p> */}
              </div>
              {/* <div className='popup-button'> */}
              <div
                className={`popup-button ${mobile ? "popup-button-mobile" : ""
                  }`}
              >
                <button
                  class="btn btn-draft"
                  style={{
                    backgroundColor: activePopupColors.buttonFont,
                    color: activePopupColors.buttonBackground,
                    width: mobile ? "100%" : "auto",
                  }}
                >
                  {saveButtonText}
                </button>
                <button
                  class="btn btn-publish"
                  style={{
                    backgroundColor: activePopupColors.buttonBackground,
                    color: activePopupColors.buttonFont,
                    width: mobile ? "100%" : "auto",
                  }}
                >
                  {buttonText}
                </button>
              </div>
            </div>
          )}
          {activeTab === "semantics" && (
            <img className="image" src={html} alt="HTML semantics" />
          )}

          {activeTab === "focus" && (
            <img className="image" src={tab} alt="Keyboard focus order" />
          )}

          {activeTab === "screen" && (
            <img
              className="image"
              src={screen}
              alt="Screen reader reading order"
            />
          )}
        </div>
      </div>
      <div className="footer">
        <div className="template-consent">
          <ActionButton
            kind="secondary"
            size="small"
            state="normal"
            label="Save as draft"
            onClick={handleDraft}
          />

          <ActionButton
            kind="primary"
            size="small"
            state="normal"
            label="Publish template"
            onClick={handlePublish}
          />
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

      {/* Guided Tour */}
      <GuidedTour
        steps={createGrievanceTourSteps}
        tourId="create-grievance-tour"
        showOnFirstVisit={true}
      />

      {/* Tour Help Button */}
      <TourButton
        tourId="create-grievance-tour"
        label="Help"
        position="bottom-right"
      />
    </div>
  );
};

export default CreateGrievanceForm;
