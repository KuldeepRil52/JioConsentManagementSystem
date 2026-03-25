import React, { useState, useEffect } from "react";
import "../styles/createConsent.css";
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
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { useRef } from "react";
import Select from "react-select";
import PurposeContainer from "./PurposeContainer";
import LanguageContainer from "./LanguageContainer";
import Branding from "./Branding";
import GuidedTour from "./GuidedTour";
import TourButton from "./TourButton";
import { createConsentTourSteps } from "../utils/tourSteps";
import {
  IcCode,
  IcKeyboard,
  IcLanguage,
  IcNotes,
  IcStamp,
  IcVoice,
  IcVoiceRecording,
} from "../custom-components/Icon";
import { createConsentTemplate, getConsentTemplateDetails, editConsentTemplate, getProcessingActivity, getPurposeList} from "../store/actions/CommonAction";
import { IcChevronDown } from "../custom-components/Icon";
import { useLocation } from "react-router-dom";

const CreateConsent = () => {
  //for getting template id from param
  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const templateId = queryParams.get("templateId");
  const version = queryParams.get("version");

  //from state
  const businessId = useSelector((state) => state.common.business_id);
  const tenant_id = useSelector((state) => state.common.tenant_id);
  const session_token = useSelector((state) => state.common.session_token);
  const mobile_no = useSelector((state) => state.common.mobile);
  const email = useSelector((state) => state.common.email);
  const companyLogo = useSelector((state) => state.common.companyLogo);

  const logos = new URL("../assets/popup.svg", import.meta.url).href;
  const html = new URL("../assets/html.png", import.meta.url).href;
  const screen = new URL("../assets/screen.png", import.meta.url).href;
  const tab = new URL("../assets/tab.png", import.meta.url).href;

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const [isChecked, setIsChecked] = useState(false);
  const [templateName, setTemplateName] = useState("");
  const [originalTemplateStatus, setOriginalTemplateStatus] = useState(null); // Store original template status
  const [dark, setDark] = useState(false);
  const [mobile, setMobile] = useState(false);
  const [view, setView] = useState("popup");
  const [title, setTitle] = useState("Manage Consent");
  const [cid, setCid] = useState("Customer ID");
  const [age, setAge] = useState("I'm below 18 years of age.");
  const [purposeHeading, setPurposeHeading] = useState("Purpose: ");
  const [processingHeading, setProcessingHeading] = useState(
    "Processing activity "
  );
  const [usedByHeading, setUsedByHeading] = useState("Used By: ");
  const [durationHeading, setDurationHeading] = useState("Duration: ");
  const [dataItemHeading, setDataItemHeading] = useState("Data item: ");
  const [buttonText, setButtonText] = useState("Allow all");
  const [saveButtonText, setSaveButtonText] = useState("Save my choices");
  const [others, setOthers] = useState("Others");
  const [isTranslating, setIsTranslating] = useState(false);

  var purposeOpts = [];
  var activityOpts = [];

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
      preferenceValidity: { value: "", unit: "" },
      purposeActivityIds: [],
      usedBy: [],
      dataItems: [],
      dataTypes: [], // Array of { dataTypeName, dataItems } for each processing activity
      processingAct: [],
      preferenceId: null, // Will be set when editing existing template
    },
  ]);

  const requiredPurposes = purposes.filter((p) => p.isMandatory === "Required");
  const optionalPurposes = purposes.filter((p) => p.isMandatory === "Optional");

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
            {/* {items.map((item, idx) => (
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

                
                {idx !== items.length - 1 && (
                  <hr className="accordion-separator" />
                )}
              </div>
            ))} */}
            {items.map((item, idx) => (
              <div key={idx} className="accordion-block">
                {/* Purpose */}
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

                {/* Duration */}
                {validitytoBeShown && (
                  <p>
                    <strong>{durationHeading}</strong>{" "}
                    {item.preferenceValidity?.value
                      ? `${item.preferenceValidity.value} ${toTitleCase(
                        item.preferenceValidity.value === 1
                          ? item.preferenceValidity.unit.replace(/s$/i, "")
                          : item.preferenceValidity.unit
                      )}`
                      : "N/A"}
                  </p>
                )}

                {item.processingAct?.length > 0 &&
                  item.processingAct.map((act, i) => (
                    <div
                      key={i}
                      className="processing-activity-block"
                      style={{
                        marginTop: "10px",
                        paddingTop: "5px",
                        borderTop: "1px solid #ebe6e6",
                      }}
                    >
                      {processActivityNameToBeShown && (
                        <p>
                          <strong>
                            {processingHeading} {i + 1}:
                          </strong>{" "}
                          {act}
                        </p>
                      )}

                      {item.usedBy?.[i] && processorNameToBeShown && (
                        <p>
                          <strong>{usedByHeading}</strong> {item.usedBy[i]}
                        </p>
                      )}

                      {item.dataTypes?.[i] && (dataTypeToBeShown || dataItemToBeShown) && Array.isArray(item.dataTypes[i]) && item.dataTypes[i].length > 0 && (
                        <div>
                          {item.dataTypes[i].map((dataType, dtIndex) => (
                            <div key={dtIndex} style={{ marginTop: dtIndex > 0 ? "12px" : "0" }}>
                              {dataType.dataTypeName && dataTypeToBeShown && (
                                <p style={{ marginBottom: dataType.dataItems && dataType.dataItems.length > 0 ? "4px" : "0" }}>
                                  <strong>Data Type:</strong> {dataType.dataTypeName}
                                </p>
                              )}
                              {dataType.dataItems && dataType.dataItems.length > 0 && dataItemToBeShown && (
                                <p>
                                  <strong>{dataItemHeading}</strong> {Array.isArray(dataType.dataItems) ? dataType.dataItems.join(", ") : dataType.dataItems}
                                </p>
                              )}
                            </div>
                          ))}
                        </div>
                      )}
                      {/* Fallback to old dataItems display if dataTypes is not available */}
                      {(!item.dataTypes?.[i] || !Array.isArray(item.dataTypes[i]) || item.dataTypes[i].length === 0) && item.dataItems?.[i] && dataItemToBeShown && (
                        <p>
                          <strong>{dataItemHeading}</strong> {Array.isArray(item.dataItems[i]) ? item.dataItems[i].join(", ") : item.dataItems[i]}
                        </p>
                      )}
                    </div>
                  ))}

                {/* Separator between purposes */}
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
  const [supportedLanguages, setSupportedLanguages] = useState([]);
  const [language, setLanguage] = useState("");
  const [label, setLabel] = useState("Required");
  const [languageSpecificContentMap, setLanguageSpecificContentMap] = useState({
    ENGLISH: {
      description: text,
      label: "Required",
      rightsText: rights,
      permissionText: permission,
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
        purposeNames: [],
        isMandatory: "",
        autoRenew: "",
        preferenceValidity: { value: "1", unit: "" },
        purposeActivityIds: [],
        usedBy: [],
        dataItems: [],
        dataTypes: [],
        processingAct: [],
        preferenceId: null, // New purposes don't have preferenceId
      },
    ]);
  };

  const removePurpose = (id, index) => {
    if (purposeItems.length === 1) return;
    setPurposeItems(purposeItems.filter((item) => item !== id));
    setPurposes(purposes.filter((_, i) => i !== index));
  };

  const handleBack = () => {
    navigate("/templates");
  };

  const buildPayload = (purposes) => {
    return purposes.map((p) => {
      const payload = {
        purposeIds: p.purposeIds,
        mandatory: p.isMandatory === "Required",
        autoRenew: p.autoRenew === "Yes",
        preferenceValidity: p.preferenceValidity,
        processorActivityIds: p.purposeActivityIds,
        preferenceStatus: "ACCEPTED",
      };
      // Only include preferenceId if it exists (for editing existing preferences)
      if (p.preferenceId) {
        payload.preferenceId = p.preferenceId;
      }
      return payload;
    });
  };

  const openCertificate = (certificatePreviewUrl) => {
    if (certificatePreviewUrl) {
      window.open(certificatePreviewUrl, "_blank");
    }
  };

  const [finalPayload, setFinalPayload] = useState([]);

  // const fetchPurposes = async () => {
  //   try {
  //     let res = await dispatch(getPurposeList());
  //     if (res?.data?.searchList) {
  //       // setPurposeList(res.data.searchList);

  //       const opts = res.data.searchList.map((item) => ({
  //         value: item.purposeId,
  //         label: item.purposeName,
  //       }));
  //       setPurposeOptions(opts);
  //       alert("purp opts: "+JSON.stringify(opts));
  //       //return opts;
  //     }
  //     if (res?.status == 403) {
  //       toast.error(
  //         (props) => (
  //           <CustomToast
  //             {...props}
  //             type="error"
  //             message={"Facing Network Error..Please try again later."}
  //           />
  //         ),
  //         { icon: false }
  //       );
  //     }
  //   } catch (err) {
  //     if (err[0].errorCode == "JCMP4003" || err[0].errorCode == "JCMP4001") {
  //       toast.error(
  //         (props) => (
  //           <CustomToast {...props} type="error" message={"Session expired"} />
  //         ),
  //         { icon: false }
  //       );
  //     } else {
  //       toast.error(
  //         (props) => (
  //           <CustomToast
  //             {...props}
  //             type="error"
  //             message={"Error in fetching purpose."}
  //           />
  //         ),
  //         { icon: false }
  //       );
  //     }
  //     console.log("Error fetching purposes:", err);
  //   }
  // };

  const fetchPurposes = async () => {
    try {
      let res = await dispatch(getPurposeList());
      if (res?.data?.searchList) {
        // setPurposeList(res.data.searchList);

        const opts = res.data.searchList.map((item) => ({
          value: item.purposeId,
          label: item.purposeName,
        }));
        setPurposeOptions(opts);
        purposeOpts = opts;
      }
      if (res?.status == 403) {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Facing Network Error..Please try again later."}
            />
          ),
          { icon: false }
        );
      }
    } catch (err) {
      if (err[0].errorCode == "JCMP4003" || err[0].errorCode == "JCMP4001") {
        toast.error(
          (props) => (
            <CustomToast {...props} type="error" message={"Session expired"} />
          ),
          { icon: false }
        );
      } else {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Error in fetching purpose."}
            />
          ),
          { icon: false }
        );
      }
    }
  };

  // const fetchProcessingActivity = async () => {

  //   try {
  //     let res = await dispatch(getProcessingActivity());
  //     if (res?.status === 200 || res?.status === 201) {
  //       if (res?.data?.searchList) {
  //         setProcessingActivityList(res.data.searchList);
  //         const opts = res.data.searchList.map((item) => ({
  //           value: item.processorActivityId,
  //           label: item.activityName,
  //           processorName: item.processorName,
  //           dataItems: item.dataTypesList?.flatMap((d) => d.dataItems) || [],
  //         }));

  //         setProcessingActivityOptions(opts);
  //         console.log("Processing Activity List:", processingActivityList);
  //       }
  //     }
  //     if (res?.status === 403) {
  //       toast.error(
  //         (props) => (
  //           <CustomToast
  //             {...props}
  //             type="error"
  //             message={"Facing Network Error..Please try again later."}
  //           />
  //         ),
  //         { icon: false }
  //       );
  //     }
  //   } catch (err) {
  //     if (err[0]?.errorCode == "JCMP4003" || err[0]?.errorCode == "JCMP4001") {
  //       toast.error(
  //         (props) => (
  //           <CustomToast {...props} type="error" message={"Session expired"} />
  //         ),
  //         { icon: false }
  //       );
  //     } else {
  //       toast.error(
  //         (props) => (
  //           <CustomToast
  //             {...props}
  //             type="error"
  //             message={"Error in fetching processing activity"}
  //           />
  //         ),
  //         { icon: false }
  //       );
  //     }
  //     console.log("Error fetching processing activity:", err);
  //   }
  // };

  // Helper function to filter processing activities to keep only latest version per processorActivityId
  const filterLatestVersions = (activityList) => {
    if (!activityList || activityList.length === 0) return [];
    
    // Group by processorActivityId
    const grouped = activityList.reduce((acc, item) => {
      const id = item.processorActivityId;
      if (!acc[id]) {
        acc[id] = [];
      }
      acc[id].push(item);
      return acc;
    }, {});
    
    // For each group, keep the one with highest version (and latest updatedAt if versions are equal)
    return Object.values(grouped).map((group) => {
      return group.reduce((latest, current) => {
        if (current.version > latest.version) {
          return current;
        } else if (current.version === latest.version) {
          // If versions are equal, prefer the one with latest updatedAt
          const currentDate = new Date(current.updatedAt);
          const latestDate = new Date(latest.updatedAt);
          return currentDate > latestDate ? current : latest;
        }
        return latest;
      });
    });
  };

  const fetchProcessingActivity = async () => {
    try {
      let res = await dispatch(getProcessingActivity());
      if (res?.status === 200 || res?.status === 201) {
        if (res?.data?.searchList) {
          // Filter to keep only latest versions
          const filteredList = filterLatestVersions(res.data.searchList);
          setProcessingActivityList(filteredList);
          const opts = filteredList.map((item) => ({
            value: item.processorActivityId,
            label: item.activityName,
            processorName: item.processorName,
            dataItems: item.dataTypesList?.flatMap((d) => d.dataItems) || [],
            dataTypesList: item.dataTypesList || [], // Store full dataTypesList structure
          }));

          setProcessingActivityOptions(opts);
          activityOpts = opts;
        } else {
          // Handle case when no processing activity exists
          setProcessingActivityList([]);
          setProcessingActivityOptions([]);
          activityOpts = [];
        }
      }
      if (res?.status === 403) {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Facing Network Error..Please try again later."}
            />
          ),
          { icon: false }
        );
      }
    } catch (err) {
      // Better error handling to check if err is an array before accessing
      if (Array.isArray(err) && err[0] && (err[0]?.errorCode == "JCMP4003" || err[0]?.errorCode == "JCMP4001")) {
        toast.error(
          (props) => (
            <CustomToast {...props} type="error" message={"Session expired"} />
          ),
          { icon: false }
        );
      } else {
        // Set empty arrays to prevent undefined errors in UI - don't show error toast
        // User will see a helpful message in the UI to create processing activity
        setProcessingActivityList([]);
        setProcessingActivityOptions([]);
        activityOpts = [];
      }
    }
  };

  const handlePublish = async () => {
    const errors = []; // Re-introducing the errors array

    if (!templateName || templateName.trim() === "") {
      errors.push("Template name is required.");
    }

    if (supportedLanguages.length === 0) {
      errors.push("Please select supported languages before selecting preferred language.");
    }
    if (language == "") {
      errors.push("Please select a preferred language from the dropdown.");
    }
    if (!templateId) {
      if (logoName == "") {
        errors.push("Please upload logo before selecting privacy policy document.");
      }
    }

    if (fileName == "") {
      errors.push("Please upload privacy policy document before publishing the template.");
    }
    if (purposes.length === 1) {
      if (
        !purposes ||
        purposes.length === 0 ||
        purposes.some((p) => !p.purposeIds || p.purposeIds.length === 0)
      ) {
        errors.push("Please select purpose before selecting consent requirement.");
      }
      if (
        !purposes ||
        purposes.length === 0 ||
        purposes.some((p) => !p.isMandatory || p.isMandatory.trim() === "")
      ) {
        errors.push("Please select consent requirement before selecting purpose validity.");
      }
      if (
        !purposes ||
        purposes.length === 0 ||
        purposes.some(
          (p) =>
            !p.preferenceValidity?.unit ||
            p.preferenceValidity?.unit.trim() === ""
        )
      ) {
        errors.push("Please select purpose validity before selecting preference validity value.");
      }
      if (
        !purposes ||
        purposes.length === 0 ||
        purposes.some(
          (p) =>
            !p.preferenceValidity?.value ||
            p.preferenceValidity?.value === "" ||
            isNaN(Number(p.preferenceValidity?.value)) ||
            Number(p.preferenceValidity?.value) <= 0
        )
      ) {
        errors.push(
          "Please enter a valid positive number for preference validity value before selecting auto renew validity."
        );
      }
      if (
        !purposes ||
        purposes.length === 0 ||
        purposes.some((p) => !p.autoRenew || p.autoRenew == "")
      ) {
        errors.push("Please select auto renew validity before selecting processing activities.");
      }
      if (
        !purposes ||
        purposes.length === 0 ||
        purposes.some(
          (p) => !p.purposeActivityIds || p.purposeActivityIds.length === 0
        )
      ) {
        errors.push("Please select processing activities before selecting data processor.");
      }
      if (
        !purposes ||
        purposes.length === 0 ||
        purposes.some((p) => !p.usedBy || p.usedBy.length === 0)
      ) {
        errors.push("Please select data processor before selecting data items.");
      }
      if (
        !purposes ||
        purposes.length === 0 ||
        purposes.some((p) => !p.dataItems || p.dataItems.length === 0)
      ) {
        errors.push("Please select data items.");
      }
    } else {
      for (const [index, p] of purposes.entries()) {
        const num = index + 1;

        if (!p.purposeIds || p.purposeIds.length === 0) {
          errors.push(`Purpose ${num}: Please select purpose before selecting consent requirement.`);
        }
        if (!p.isMandatory || p.isMandatory.trim() === "") {
          errors.push(`Purpose ${num}: Please select consent requirement.`);
        }
        if (
          !p.preferenceValidity?.unit ||
          p.preferenceValidity.unit.trim() === ""
        ) {
          errors.push(`Purpose ${num}: Please select validity period.`);
        }
        if (
          !p.preferenceValidity?.value ||
          p.preferenceValidity?.value === "" ||
          isNaN(Number(p.preferenceValidity?.value)) ||
          Number(p.preferenceValidity?.value) <= 0
        ) {
          errors.push(
            `Purpose ${num}: Please enter a valid positive number for preference validity value.`
          );
        }
        if (!p.autoRenew || p.autoRenew.trim() === "") {
          errors.push(`Purpose ${num}: Please select auto renew validity.`);
        }
        if (!p.purposeActivityIds || p.purposeActivityIds.length === 0) {
          errors.push(`Purpose ${num}: Please select processing activities.`);
        }
        if (!p.usedBy || p.usedBy.length === 0) {
          errors.push(`Purpose ${num}: Please select data processor.`);
        }
        if (!p.dataItems || p.dataItems.length === 0) {
          errors.push(`Purpose ${num}: Please select data items.`);
        }
      }
    }

    if (errors.length > 1) {
      const firstError = errors[0];
      toast.error(
        (props) => <CustomToast {...props} type="error" message={firstError} />,
        { icon: false }
      );
      return;
    } else if (errors.length == 1) {
      toast.error(
        (props) => <CustomToast {...props} type="error" message={errors[0]} />,
        { icon: false }
      );
    } else {
      const commonBody = {
        preferences: buildPayload(purposes),
        multilingual: {
          supportedLanguages: supportedLanguages,
          languageSpecificContentMap: languageSpecificContentMap,
        },
        uiConfig: {
          logo: logoBase64 || "samplelogo",
          theme: themeBase64 || "sampletheme",
          darkMode: darkMode,
          mobileView: mobileView,
          parentalControl: parentalControl,
          dataTypeToBeShown: dataTypeToBeShown,
          dataItemToBeShown: dataItemToBeShown,
          processActivityNameToBeShown: processActivityNameToBeShown,
          processorNameToBeShown: processorNameToBeShown,
          validitytoBeShown: validitytoBeShown,
        },
        status: "PUBLISHED",
      };

      // request body
      const requestBody = {
        templateName: templateName || "DefaultConsent",
        businessId: businessId,
        ...commonBody,
        privacyPolicyDocument: fileBase64 || "samplebase64",
        privacyPolicyDocumentMeta: {
          name: fileName || "sample-local-pdf.pdf",
          contentType: "application/pdf",
          size: fileSize || 327467,
          tag: {
            component: "TEMPLATE",
            documentTag: "AGGREMENT",
          },
        },
        status: "PUBLISHED",
      };

      if (templateId) {
        const response = await editConsentTemplate(
          commonBody,
          tenant_id,
          templateId,
          version,
          session_token
        );

        if (response.status === 200) {
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"Settings saved successfully."}
              />
            ),
            { icon: false }
          );
        }

        navigate("/templates");
      } else {
        const res = await createConsentTemplate(requestBody, tenant_id, session_token);

        if (res.status === 201) {
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"Settings saved successfully."}
              />
            ),
            { icon: false }
          );
        }

        navigate("/templates");
      }
    }
  };

  const handleDraft = async () => {
    // Check if editing a published template
    if (templateId && originalTemplateStatus === "PUBLISHED") {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Published template cannot be saved as draft again."}
          />
        ),
        { icon: false }
      );
      return;
    }

    const errors = []; // Re-introducing the errors array

    if (!templateName || templateName.trim() === "") {
      errors.push("Template name is required.");
    }

    if (supportedLanguages.length === 0) {
      errors.push("Please select supported languages before selecting preferred language.");
    }
    if (language == "") {
      errors.push("Please select a preferred language from the dropdown.");
    }
    if (!templateId) {
      if (logoName == "") {
        errors.push("Please upload logo before selecting privacy policy document.");
      }
    }
    if (fileName == "") {
      errors.push("Please upload privacy policy document before drafting the template.");
    }
    if (purposes.length === 1) {
      if (
        !purposes ||
        purposes.length === 0 ||
        purposes.some((p) => !p.purposeIds || p.purposeIds.length === 0)
      ) {
        errors.push("Please select purpose before selecting consent requirement.");
      }
      if (
        !purposes ||
        purposes.length === 0 ||
        purposes.some((p) => !p.isMandatory || p.isMandatory.trim() === "")
      ) {
        errors.push("Please select consent requirement before selecting purpose validity.");
      }
      if (
        !purposes ||
        purposes.length === 0 ||
        purposes.some(
          (p) =>
            !p.preferenceValidity?.unit ||
            p.preferenceValidity?.unit.trim() === ""
        )
      ) {
        errors.push("Please select purpose validity before selecting preference validity value.");
      }
      if (
        !purposes ||
        purposes.length === 0 ||
        purposes.some(
          (p) =>
            !p.preferenceValidity?.value ||
            p.preferenceValidity?.value === "" ||
            isNaN(Number(p.preferenceValidity?.value)) ||
            Number(p.preferenceValidity?.value) <= 0
        )
      ) {
        errors.push(
          "Please enter a valid positive number for preference validity value before selecting auto renew validity."
        );
      }
      if (
        !purposes ||
        purposes.length === 0 ||
        purposes.some((p) => !p.autoRenew || p.autoRenew == "")
      ) {
        errors.push("Please select auto renew validity before selecting processing activities.");
      }
      if (
        !purposes ||
        purposes.length === 0 ||
        purposes.some(
          (p) => !p.purposeActivityIds || p.purposeActivityIds.length === 0
        )
      ) {
        errors.push("Please select processing activities before selecting data processor.");
      }
      if (
        !purposes ||
        purposes.length === 0 ||
        purposes.some((p) => !p.usedBy || p.usedBy.length === 0)
      ) {
        errors.push("Please select data processor before selecting data items.");
      }
      if (
        !purposes ||
        purposes.length === 0 ||
        purposes.some((p) => !p.dataItems || p.dataItems.length === 0)
      ) {
        errors.push("Please select data items before drafting the template.");
      }
    } else {
      purposes.forEach((p, index) => {
        const num = index + 1;

        if (!p.purposeIds || p.purposeIds.length === 0) {
          errors.push(`Purpose ${num}: Please select purpose before selecting consent requirement.`);
        }
        if (!p.isMandatory || p.isMandatory.trim() === "") {
          errors.push(`Purpose ${num}: Please select consent requirement.`);
        }
        if (
          !p.preferenceValidity?.unit ||
          p.preferenceValidity.unit.trim() === ""
        ) {
          errors.push(`Purpose ${num}: Please select validity period.`);
        }
        if (
          !p.preferenceValidity?.value ||
          p.preferenceValidity?.value === "" ||
          isNaN(Number(p.preferenceValidity?.value)) ||
          Number(p.preferenceValidity?.value) <= 0
        ) {
          errors.push(
            `Purpose ${num}: Please enter a valid positive number for preference validity value.`
          );
        }
        if (!p.autoRenew || p.autoRenew.trim() === "") {
          errors.push(`Purpose ${num}: Please select auto renew validity.`);
        }
        if (!p.purposeActivityIds || p.purposeActivityIds.length === 0) {
          errors.push(`Purpose ${num}: Please select processing activities.`);
        }
        if (!p.usedBy || p.usedBy.length === 0) {
          errors.push(`Purpose ${num}: Please select data processor.`);
        }
        if (!p.dataItems || p.dataItems.length === 0) {
          errors.push(`Purpose ${num}: Please select data items.`);
        }
      });
    }

    if (errors.length > 1) {
      const firstError = errors[0];
      toast.error(
        (props) => <CustomToast {...props} type="error" message={firstError} />,
        { icon: false }
      );
      return;
    }
    // If no errors, proceed with publishing
    const commonBody = {
      preferences: buildPayload(purposes),
      multilingual: {
        supportedLanguages: supportedLanguages,
        languageSpecificContentMap: languageSpecificContentMap,
      },
      uiConfig: {
        logo: logoBase64 || "samplelogo",
        theme: themeBase64 || "sampletheme",
        darkMode: darkMode,
        mobileView: mobileView,
        parentalControl: parentalControl,
        dataTypeToBeShown: dataTypeToBeShown,
        dataItemToBeShown: dataItemToBeShown,
        processActivityNameToBeShown: processActivityNameToBeShown,
        processorNameToBeShown: processorNameToBeShown,
        validitytoBeShown: validitytoBeShown,
      },
      status: "DRAFT",
    };
    // request body
    const requestBody = {
      templateName: templateName || "DefaultConsent",
      businessId: businessId,
      ...commonBody,
      privacyPolicyDocument: fileBase64 || "samplebase64",
      privacyPolicyDocumentMeta: {
        name: fileName || "sample-local-pdf.pdf",
        contentType: "application/pdf",
        size: fileSize || 327467,
        tag: {
          component: "TEMPLATE",
          documentTag: "AGGREMENT",
        },
      },
      status: "DRAFT",
    };

    if (templateId) {
      const response = await editConsentTemplate(
        commonBody,
        tenant_id,
        templateId,
        version,
        session_token
      );

      if (response.status === 200) {
        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message={"Settings saved successfully."}
            />
          ),
          { icon: false }
        );
      }

      navigate("/templates");
    } else {
      const res = await createConsentTemplate(requestBody, tenant_id, session_token);

      if (res.status === 201) {
        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message={"Settings saved successfully."}
            />
          ),
          { icon: false }
        );
      }

      navigate("/templates");
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
                  <span class="landmark-number">1</span>
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
                <span class="landmark-tag">&lt;header&gt;</span>
              </div>
            </div>
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
                <span class="landmark-tag">&lt;main&gt;</span>
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
                  Image Details
                </Text>
              </div>
              <div class="landmark-body">
                <Text appearance="body-s" color="primary-grey-100">
                  accessible name
                </Text>
                <span class="landmark-tag">
                  Logo image is decorative. It will not be focussable
                </span>
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
                  Heading &lt;h2&gt;
                </Text>
              </div>
              <div
                class="landmark-body"
                style={{ borderBottom: "solid 1px #e0e0e0" }}
              >
                <Text appearance="body-s" color="primary-grey-100">
                  element
                </Text>
                <span class="landmark-tag">&lt;h2&gt;</span>
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
                  Manage data consent for ‘(Organization Name)'
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
                  Heading &lt;h2&gt;
                </Text>
              </div>
              <div
                class="landmark-body"
                style={{ borderBottom: "solid 1px #e0e0e0" }}
              >
                <Text appearance="body-s" color="primary-grey-100">
                  element
                </Text>
                <span class="landmark-tag">&lt;h2&gt;</span>
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
                  Customer ID : {mobile_no || email}
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
                  <span class="landmark-number">h4</span>
                  <span class="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Heading &lt;h4&gt;
                </Text>
              </div>
              <div
                class="landmark-body"
                style={{ borderBottom: "solid 1px #e0e0e0" }}
              >
                <Text appearance="body-s" color="primary-grey-100">
                  element
                </Text>
                <span class="landmark-tag">&lt;h4&gt;</span>
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
                  screen reader content
                </Text>
                <Text appearance="body-xxs" color="primary-grey-80">
                  Required data shared (Refer button 2) details
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
                <div class="landmark-badge-4">
                  <span class="landmark-number">1</span>
                  <span class="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Note
                </Text>
              </div>
              <div
                class="landmark-body"
                style={{ borderBottom: "solid 1px #e0e0e0" }}
              >
                <Text appearance="body-s" color="primary-grey-100">
                  element
                </Text>
                <span class="landmark-tag">&lt;p&gt;</span>
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
                  Paragraph content
                </Text>
                <div style={{ width: "50%" }}>
                  <Text appearance="body-xxs" color="primary-grey-80">
                    While using (Organization Name), your activities create data which will
                    be used with your consent to offer customised services.
                    Details of data usage are provided below.
                  </Text>
                </div>
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
                <div class="landmark-badge-4">
                  <span class="landmark-number">2</span>
                  <span class="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Note
                </Text>
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
                  Notification polite
                </Text>
                <Text appearance="body-xxs" color="primary-grey-80">
                  Required data shared - Button - List expanded
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
                <div class="landmark-badge-4">
                  <span class="landmark-number">3</span>
                  <span class="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Note
                </Text>
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
                  Notification polite
                </Text>
                <div style={{ width: "50%" }}>
                  <Text appearance="body-xxs" color="primary-grey-80">
                    You will need to confirm that you are over 18 years of age
                    by selecting the checkbox to proceed
                  </Text>
                </div>
              </div>
            </div>
            <div class="landmark-card-5">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div class="landmark-badge-5">
                  <span class="landmark-number">1</span>
                  <span class="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Fieldset
                </Text>
              </div>
              <div
                class="landmark-body"
                style={{ borderBottom: "solid 1px #e0e0e0" }}
              >
                <Text appearance="body-s" color="primary-grey-100">
                  element
                </Text>
                <span class="landmark-tag">fieldset</span>
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
                  additional details
                </Text>
                <div style={{ width: "50%" }}>
                  <Text appearance="body-xxs" color="primary-grey-80">
                    Allow the following data to be shared with : Jio Platforms
                    limited - Medical documents, profile, phone number, full
                    name, date of birth and email id, for the purpose of
                    ‘Targetted advertisements’ and for a duration of ‘1 year’
                  </Text>
                </div>
              </div>
            </div>
            <div class="landmark-card-5">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div class="landmark-badge-5">
                  <span class="landmark-number">2</span>
                  <span class="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Checkbox details
                </Text>
              </div>
              <div
                class="landmark-body"
                style={{ borderBottom: "solid 1px #e0e0e0" }}
              >
                <Text appearance="body-s" color="primary-grey-100">
                  input type
                </Text>
                <span class="landmark-tag">checkbox</span>
              </div>
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  justifyContent: "space-between",
                  width: "100%",
                  padding: "5px",
                  borderBottom: "solid 1px #e0e0e0",
                }}
              >
                <Text appearance="body-xs" color="primary-grey-80">
                  required
                </Text>
                <Text appearance="body-xxs" color="primary-grey-80">
                  true
                </Text>
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
                  additional details
                </Text>
                <Text appearance="body-xxs" color="primary-grey-80">
                  I confirm that i'm above 18 years of age
                </Text>
              </div>
            </div>
            <div class="landmark-card-6">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div class="landmark-badge-6">
                  <span class="landmark-number">1</span>
                  <span class="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Button details
                </Text>
              </div>
              <div
                class="landmark-body"
                style={{ borderBottom: "solid 1px #e0e0e0" }}
              >
                <Text appearance="body-s" color="primary-grey-100">
                  element
                </Text>
                <span class="landmark-tag">&lt;button&gt;</span>
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
                  Close dialog
                </Text>
              </div>
            </div>
            <div class="landmark-card-6">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div class="landmark-badge-6">
                  <span class="landmark-number">2</span>
                  <span class="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Button details
                </Text>
              </div>
              <div
                class="landmark-body"
                style={{ borderBottom: "solid 1px #e0e0e0" }}
              >
                <Text appearance="body-s" color="primary-grey-100">
                  element
                </Text>
                <span class="landmark-tag">&lt;button&gt;</span>
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
                  Required data shared - Always active - Expanded
                </Text>
              </div>
            </div>
            <div class="landmark-card-6">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div class="landmark-badge-6">
                  <span class="landmark-number">3</span>
                  <span class="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Button details
                </Text>
              </div>
              <div
                class="landmark-body"
                style={{ borderBottom: "solid 1px #e0e0e0" }}
              >
                <Text appearance="body-s" color="primary-grey-100">
                  element
                </Text>
                <span class="landmark-tag">&lt;button&gt;</span>
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
                  Save my choices of selected data to be shared.
                </Text>
              </div>
            </div>
            <div class="landmark-card-6">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div class="landmark-badge-6">
                  <span class="landmark-number">4</span>
                  <span class="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Button details
                </Text>
              </div>
              <div
                class="landmark-body"
                style={{ borderBottom: "solid 1px #e0e0e0" }}
              >
                <Text appearance="body-s" color="primary-grey-100">
                  element
                </Text>
                <span class="landmark-tag">&lt;button&gt;</span>
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
                  Allow all required and other data to be shared.
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

  // useEffect(() => {
  //   if (templateId) {
  //     fetchPurposes();
  //     fetchProcessingActivity();
  //     const fetchTemplateDetails = async () => {
  //       try {
  //         alert("making api call");
  //         const data = await getConsentTemplateDetails(templateId, tenant_id);
  //         console.log("Fetched template:", data);
  //         const template = data?.searchList?.[0]; // ✅ safely get the first template
  //         console.log("Template object:", template);

  //         if (template) {
  //           setTemplateName(template.templateName || "default template");
  //           console.log("template name form api call: ", template.templateName);
  //           // setPurposes(template.preferences || []);
  //           setSupportedLanguages(template.multilingual?.supportedLanguages || []);
  //           setLanguageSpecificContentMap(template.multilingual?.languageSpecificContentMap || {});
  //           setParentalControl(template.uiConfig.parentalControl || false);
  //           setDataTypeToBeShown(template.uiConfig.dataTypeToBeShown || false);
  //           setDataItemToBeShown(template.uiConfig.dataItemToBeShown || false);
  //           setProcessActivityNameToBeShown(template.uiConfig.processActivityNameToBeShown || false);
  //           setProcessorNameToBeShown(template.uiConfig.processorNameToBeShown || false);
  //           setValiditytoBeShown(template.uiConfig.validitytoBeShown || false);
  //           setDarkMode(template.uiConfig.darkMode || false);
  //           setFileName(template.documentMeta.name || "sample.pdf");
  //           setLogoBase64(template.uiConfig.logo || "logo");
  //           setLogoPreviewUrl(template.uiConfig.logo);

  //           const themeEncoded = template.uiConfig?.theme;
  //             if (!themeEncoded) return;

  //             try {
  //               // decode base64 → string
  //               const themeDecoded = atob(themeEncoded);

  //               // parse to object
  //               const themeObj = JSON.parse(themeDecoded);
  //               console.log("theme obj: ", themeObj);

  //               // light theme exists
  //               if (themeObj.light) {
  //                 const lightParsed = JSON.parse(themeObj.light);
  //                 setColors(lightParsed);
  //                 console.log("colors: ",colors);
  //               }

  //               // dark theme exists
  //               if (themeObj.dark) {
  //                 const darkParsed = JSON.parse(themeObj.dark);
  //                 setDarkColors(darkParsed);
  //               }
  //             } catch (err) {
  //               console.error("Error decoding theme:", err);
  //             }

  //           // if (template.preferences?.length > 0) {
  //           //   const mapped = template.preferences.map((pref) => ({
  //           //     purposeIds: pref.purposeIds || [],
  //           //     purposeNames: [], // you can fill this after fetching purposeList
  //           //     isMandatory: pref.mandatory ? "Required" : "Optional",
  //           //     autoRenew: pref.autoRenew ? "Yes" : "No",
  //           //     preferenceValidity: pref.preferenceValidity || { value: 1, unit: "" },
  //           //     purposeActivityIds: pref.processorActivityIds || [],
  //           //     usedBy: [],       // populate later if API gives processor names
  //           //     dataItems: [],    // populate later if API gives data items
  //           //     processingAct: [],// same as above
  //           //   }));
  //           //   setPurposes(mapped);
  //           //   setPurposeItems(mapped.map((_, idx) => idx)); // update UI for multiple purposes
  //           //   setCounter(mapped.length);
  //           // }

  //           if (template.preferences?.length > 0) {
  //             const mapped = template.preferences.map((pref) => {
  //               // Match purposeIds with purposeOptions to get names
  //               const matchedPurposeNames = (pref.purposeIds || [])
  //                 .map((id) => {
  //                   const match = purposeOptions.find((opt) => opt.value === id);
  //                   return match ? match.label : null;
  //                 })
  //                 .filter(Boolean); // remove nulls

  //                 console.log('matched names: ', matchedPurposeNames);

  //               return {
  //                 purposeIds: pref.purposeIds || [],
  //                 purposeNames: matchedPurposeNames,  // ✅ filled from purposeOptions
  //                 isMandatory: pref.mandatory ? "Required" : "Optional",
  //                 autoRenew: pref.autoRenew ? "Yes" : "No",
  //                 preferenceValidity: pref.preferenceValidity || { value: 1, unit: "" },
  //                 purposeActivityIds: pref.processorActivityIds || [],
  //                 usedBy: [],       // to be filled from processor API
  //                 dataItems: [],    // to be filled from data items API
  //                 processingAct: [],// to be filled later
  //               };
  //             });

  //             setPurposes(mapped);
  //             console.log("purposes after mapping: ", purposes);
  //             setPurposeItems(mapped.map((_, idx) => idx));
  //             setCounter(mapped.length);
  //           }

  //         }
  //       } catch (err) {
  //         // Show error toast if needed
  //       }
  //     };

  //     fetchTemplateDetails();
  //   }
  // }, [templateId, tenant_id]);

  // useEffect(() => {
  //   alert("inside useeffct");
  //   if (!templateId) return;

  //   const fetchAll = async () => {
  //     try {
  //       // Run in parallel
  //       // let purposeOptionsReturned = fetchPurposes();           // updates purposeOptions
  //       // setPurposeOptions(purposeOptionsReturned);

  //       const [purposeRes, processingRes, templateRes] = await Promise.all([
  //         fetchPurposes(),
  //         fetchProcessingActivity(), // updates processing options
  //         getConsentTemplateDetails(templateId, tenant_id), // returns template

  //       ]);

  //       console.log("Fetched template:", templateRes);
  //       const template = templateRes?.searchList?.[0];
  //       if (!template) return;

  //       setTemplateName(template.templateName || "default template");
  //       setSupportedLanguages(template.multilingual?.supportedLanguages || []);
  //       setLanguageSpecificContentMap(template.multilingual?.languageSpecificContentMap || {});
  //       setParentalControl(template.uiConfig.parentalControl || false);
  //       setDataTypeToBeShown(template.uiConfig.dataTypeToBeShown || false);
  //       setDataItemToBeShown(template.uiConfig.dataItemToBeShown || false);
  //       setProcessActivityNameToBeShown(template.uiConfig.processActivityNameToBeShown || false);
  //       setProcessorNameToBeShown(template.uiConfig.processorNameToBeShown || false);
  //       setValiditytoBeShown(template.uiConfig.validitytoBeShown || false);
  //       setDarkMode(template.uiConfig.darkMode || false);
  //       setFileName(template.documentMeta.name || "sample.pdf");
  //       setLogoBase64(template.uiConfig.logo || "logo");
  //       setLogoPreviewUrl(template.uiConfig.logo);

  //       // decode theme
  //       const themeEncoded = template.uiConfig?.theme;
  //       if (themeEncoded) {
  //         try {
  //           const themeDecoded = atob(themeEncoded);
  //           const themeObj = JSON.parse(themeDecoded);

  //           if (themeObj.light) setColors(JSON.parse(themeObj.light));
  //           if (themeObj.dark) setDarkColors(JSON.parse(themeObj.dark));
  //         } catch (err) {
  //           console.error("Error decoding theme:", err);
  //         }
  //       }
  //       console.log("theme: ", colors );

  //       // ✅ Map template.preferences with purposeOptions
  //       if (template.preferences?.length > 0) {

  //         const mapped = template.preferences.map((pref) => {
  //           alert("re : "+pref.purposeIds)
  //           const matchedPurposeNames = (pref.purposeIds || [])
  //             .map((id) => {
  //               alert("purposeRes : "+JSON.stringify(purposeRes));
  //               const match = purposeRes.find((opt) => opt.value === id);
  //               return match ? match.label : null;
  //             })
  //             .filter(Boolean);

  //           alert("matched response: ", matchedPurposeNames);

  //           return {
  //             purposeIds: pref.purposeIds || [],
  //             purposeNames: matchedPurposeNames,
  //             isMandatory: pref.mandatory ? "Required" : "Optional",
  //             autoRenew: pref.autoRenew ? "Yes" : "No",
  //             preferenceValidity: pref.preferenceValidity || { value: 1, unit: "" },
  //             purposeActivityIds: pref.processorActivityIds || [],
  //             usedBy: [],
  //             dataItems: [],
  //             processingAct: [],
  //           };
  //         });

  //         console.log("mapped before setPurposes:", mapped);

  //         setPurposes(mapped);
  //         console.log("final purposes: ", purposes);
  //         setPurposeItems(mapped.map((_, idx) => idx));
  //         setCounter(mapped.length);
  //       }
  //     } catch (err) {
  //       console.error("Error fetching all details:", err);
  //     }
  //   };

  //   fetchAll();
  // }, [templateId, tenant_id]);

  useEffect(() => {
    if (!templateId) return;

    const fetchAll = async () => {
      try {
        // Run in parallel
        const [purposeRes, processingRes, templateRes] = await Promise.all([
          fetchPurposes(), // updates purposeOptions
          fetchProcessingActivity(), // updates processing options
          getConsentTemplateDetails(templateId, tenant_id, version, session_token), // returns specific version
        ]);

        const template = templateRes?.searchList?.[0];
        if (!template) return;

        setTemplateName(template.templateName || "default template");
        setOriginalTemplateStatus(template.status || null); // Store original status
        setSupportedLanguages(template.multilingual?.supportedLanguages || []);
        setLanguageSpecificContentMap(
          template.multilingual?.languageSpecificContentMap || {}
        );
        setLanguage(template?.multilingual?.supportedLanguages[0])

        // Extract language content for the first/current language
        const firstLang = template?.multilingual?.supportedLanguages[0];
        const langContent = template.multilingual?.languageSpecificContentMap?.[firstLang];
        if (langContent) {
          setText(langContent.description || text);
          setRights(langContent.rightsText || rights);
          setPermission(langContent.permissionText || permission);
          setLabel(langContent.label || label);
          setTitle(langContent.title || title);
          setCid(langContent.cid || cid);
          setAge(langContent.age || age);
          setPurposeHeading(langContent.purposeHeading || purposeHeading);
          setProcessingHeading(langContent.processingHeading || processingHeading);
          setUsedByHeading(langContent.usedByHeading || usedByHeading);
          setDurationHeading(langContent.durationHeading || durationHeading);
          setDataItemHeading(langContent.dataItemHeading || dataItemHeading);
          setButtonText(langContent.buttonText || buttonText);
          setSaveButtonText(langContent.saveButtonText || saveButtonText);
          setOthers(langContent.others || others);
        }

        setParentalControl(template.uiConfig.parentalControl !== undefined ? template.uiConfig.parentalControl : true);
        setDataTypeToBeShown(template.uiConfig.dataTypeToBeShown !== undefined ? template.uiConfig.dataTypeToBeShown : true);
        setDataItemToBeShown(template.uiConfig.dataItemToBeShown !== undefined ? template.uiConfig.dataItemToBeShown : true);
        setProcessActivityNameToBeShown(
          template.uiConfig.processActivityNameToBeShown !== undefined ? template.uiConfig.processActivityNameToBeShown : true
        );
        setProcessorNameToBeShown(
          template.uiConfig.processorNameToBeShown !== undefined ? template.uiConfig.processorNameToBeShown : true
        );
        setValiditytoBeShown(template.uiConfig.validitytoBeShown !== undefined ? template.uiConfig.validitytoBeShown : true);
        setDarkMode(template.uiConfig.darkMode || false);
        setFileName(template.documentMeta.name || "sample.pdf");
        setLogoBase64(template.uiConfig.logo || "logo");
        setLogoPreviewUrl(template.uiConfig.logo);

        // decode theme
        const themeEncoded = template.uiConfig?.theme;
        if (themeEncoded) {
          try {
            const themeDecoded = atob(themeEncoded);
            const themeObj = JSON.parse(themeDecoded);

            if (themeObj.light) setColors(JSON.parse(themeObj.light));
            if (themeObj.dark) setDarkColors(JSON.parse(themeObj.dark));
          } catch (err) {
            console.error("Error decoding theme:", err);
          }
        }

        // ✅ Map template.preferences with purposeOptions
        if (template.preferences?.length > 0) {
          const mapped = template.preferences.map((pref) => {
            const matchedPurposeNames = (pref.purposeIds || [])
              .map((id) => {
                const match = purposeOpts.find((opt) => opt.value === id);
                return match ? match.label : null;
              })
              .filter(Boolean);

            const matchProcessorNames = (pref.processorActivityIds || [])
              .map((id) => {
                const match = activityOpts.find((opt) => opt.value === id);
                return match ? match.processorName : null;
              })
              .filter(Boolean);

            const matchDataItemNames = (pref.processorActivityIds || [])
              .map((id) => {
                const match = activityOpts.find((opt) => opt.value === id);
                return match ? match.dataItems : null;
              })
              .filter(Boolean);

            const matchDataTypes = (pref.processorActivityIds || [])
              .map((id) => {
                const match = activityOpts.find((opt) => opt.value === id);
                return match ? match.dataTypesList : null;
              })
              .filter(Boolean);

            const matchProcessingActivityNames = (
              pref.processorActivityIds || []
            )
              .map((id) => {
                const match = activityOpts.find((opt) => opt.value === id);
                return match ? match.label : null;
              })
              .filter(Boolean);
            return {
              purposeIds: pref.purposeIds || [],
              purposeNames: matchedPurposeNames,
              isMandatory: pref.mandatory ? "Required" : "Optional",
              autoRenew: pref.autoRenew ? "Yes" : "No",
              preferenceValidity: pref.preferenceValidity || {
                value: 1,
                unit: "",
              },
              purposeActivityIds: pref.processorActivityIds || [],
              usedBy: matchProcessorNames,
              dataItems: matchDataItemNames,
              dataTypes: matchDataTypes, // Array of dataTypesList for each processing activity
              processingAct: matchProcessingActivityNames,
              preferenceId: pref.preferenceId || null, // Capture preferenceId from API
            };
          });


          setPurposes(mapped);
          setPurposeItems(mapped.map((_, idx) => idx));
          setCounter(mapped.length);
        }
      } catch (err) {
        console.error("Error fetching all details:", err);
      }
    };

    fetchAll();
  }, [templateId, tenant_id]);

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
                {templateId ? 'Update consent template' : 'Create consent template'}
              </Text>

              <div
                style={{ width: "80%", marginTop: "10px" }}
                data-tour="template-name"
              >
                <InputFieldV2
                  className="systemConfig-input"
                  label="Enter template name (Required)"
                  onChange={(e) => setTemplateName(e.target.value)}
                  placeholder="Template name"
                  size="small"
                  value={templateName}
                  disabled={!!templateId}
                ></InputFieldV2>
              </div>
            </div>
          </div>

          <div className="cc-body">
            <div className="notif-tabs">
              <Tabs
                onTabChange={() => {
                  setActiveTab("popup");
                }}
              >
                <TabItem
                  data-tour="purpose-tab"
                  label={
                    <Text appearance="body-xs" color="primary-grey-80">
                      Purpose
                    </Text>
                  }
                  children={
                    <div>
                      {purposeItems.map((id, index) => (
                        <PurposeContainer
                          key={id}
                          index={index}
                          purposeNumber={index + 1}
                          data={purposes[index]}
                          onChange={(updated) => {
                            const newPurposes = [...purposes];
                            newPurposes[index] = {
                              ...newPurposes[index],
                              ...updated,
                            };
                            setPurposes(newPurposes);
                          }}
                          onClear={() => removePurpose(id, index)}
                          isFirst={purposeItems.length === 1}
                          processingActivityList={processingActivityList}
                          setProcessingActivityList={setProcessingActivityList}
                          activityOptions={activityOptions}
                          setActivityOptions={setActivityOptions}
                          purposeOptions={purposeOptions}
                          setPurposeOptions={setPurposeOptions}
                          purposeList={purposeList}
                          setPurposeList={setPurposeList}
                          processingActivityOption={processingActivityOptions}
                          setProcessingActivityOptions={
                            setProcessingActivityOptions
                          }
                        />
                      ))}

                      <ActionButton
                        kind="secondary"
                        size="small"
                        state="normal"
                        label="Add another purpose"
                        onClick={addPurpose}
                      />
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
                      <LanguageContainer
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
                        onTranslationStateChange={setIsTranslating}
                      />
                    </div>
                  }
                />
                <TabItem
                  data-tour="branding-tab"
                  label={
                    <Text appearance="body-xs" color="primary-grey-80">
                      Branding
                    </Text>
                  }
                  children={
                    <div>
                      <Branding
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
                position: 'relative',
              }}
            >
              {isTranslating && (
                <div
                  style={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    right: 0,
                    bottom: 0,
                    backgroundColor: 'rgba(255, 255, 255, 0.9)',
                    display: 'flex',
                    justifyContent: 'center',
                    alignItems: 'center',
                    zIndex: 1000,
                    borderRadius: '8px',
                  }}
                >
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
                    <Spinner
                      kind="normal"
                      label=""
                      labelPosition="right"
                      size="medium"
                    />
                    <Text appearance="body-s" color="primary-grey-80">
                      Translating...
                    </Text>
                  </div>
                </div>
              )}
              <div className="close-button">
                <Icon
                  color="primary"
                  ic="ic_close"
                  kind="default"
                  onClick={function noRefCheck() { }}
                  size="medium"
                />
              </div>
              <div className="popup-header">
                <div
                  style={{
                    display: "flex",
                    flexDirection: "row",
                    gap: "20px",
                    alignItems: "center",
                  }}
                >
                  {/* <Text appearance="heading-s" color="primary-grey-100" style={{padding: '16px'}}>
                                Manage Consent
                            </Text> */}
                  <h1
                    className="custom-head"
                    style={{
                      backgroundColor: activePopupColors.cardBackground,
                      color: activePopupColors.cardFont,
                      marginBottom: "8px",
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

                {(logoPreviewUrl || companyLogo) && (
                  <img
                    src={logoPreviewUrl || companyLogo}
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
                  }}
                >
                  {text}
                </p>
                <p
                  className="custom-text-p"
                  style={{
                    backgroundColor: activePopupColors.cardBackground,
                    color: activePopupColors.cardFont,
                    marginBottom: "8px",
                  }}
                >
                  {cid}: {mobile_no || email}{" "}
                </p>

                <div className="accordion">
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
                </p>

                {fileName && certificatePreviewUrl && (
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
                )}

                {parentalControl && (
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
                )}

                <p
                  className="custom-text-p"
                  style={{
                    backgroundColor: activePopupColors.cardBackground,
                    color: activePopupColors.cardFont,
                    marginBottom: "8px",
                  }}
                >
                  {permission}
                </p>
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
        steps={createConsentTourSteps}
        tourId="create-consent-tour"
        showOnFirstVisit={true}
      />

      {/* Tour Help Button */}
      <TourButton
        tourId="create-consent-tour"
        label="Help"
        position="bottom-right"
      />
    </div>
  );
};

export default CreateConsent;
