import React, { useState, useEffect, useRef } from "react";
import "../styles/languageContainer.css";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";

import CustomToast from "./CustomToastContainer";
import { generateTransactionId } from "../utils/transactionId";
import config from "../utils/config";

import {
  ActionButton,
  Icon,
  Text,
  InputFieldV2,
  InputRadio,
  Heading,
} from "../custom-components";
import Select from "react-select";
import axios from "axios";
import { useDispatch, useSelector } from "react-redux";
import { hideLoader, showLoader } from "./CreateGrievanceForm";
import { useNavigate } from "react-router-dom";
import { Button } from '../custom-components';

const FormLanguageContainer = ({
  text,
  setText,
  permission,
  setPermission,
  rights,
  setRights,
  supportedLanguages,
  setSupportedLanguages,
  language,
  setLanguage,
  label,
  setLabel,
  languageSpecificContentMap,
  setLanguageSpecificContentMap,
  title,
  setTitle,
  cid,
  setCid,
  age,
  setAge,
  purposes,
  setPurposes,
  onPurposesChange,
  purposeHeading,
  setPurposeHeading,
  processingHeading,
  setProcessingHeading,
  usedByHeading,
  setUsedByHeading,
  durationHeading,
  setDurationHeading,
  dataItemHeading,
  setDataItemHeading,
  buttonText,
  setButtonText,
  saveButtonText,
  setSaveButtonText,
  others,
  setOthers,
  grievanceDetailsHeading,
  setGrievanceDetailsHeading,
  selectCategoryPlaceholder,
  setSelectCategoryPlaceholder,
  grievanceCategoryLabel,
  setGrievanceCategoryLabel,
  grievanceSubCategoryLabel,
  setGrievanceSubCategoryLabel,
  subCategoryPlaceholder,
  setSubCategoryPlaceholder,
  descriptionLabel,
  setDescriptionLabel,
  descriptionPlaceholder,
  setDescriptionPlaceholder,
  userDetailsHeading,
  setUserDetailsHeading,
  userTypeLabel,
  setUserTypeLabel,
  userTypePlaceholder,
  setUserTypePlaceholder,
  uploadFilesHeading,
  setUploadFilesHeading,
  uploadButtonText,
  setUploadButtonText,
  uploadInstruction,
  setUploadInstruction,
  requiredLable,
  setRequiredLable,
  enterLable,
  setEnterLable,
  userData2,
}) => {
  const options = [
    { value: "ENGLISH", label: "English", apiValue: "en" },
    { value: "HINDI", label: "Hindi", apiValue: "hi" },
    { value: "BENGALI", label: "Bengali", apiValue: "bn" },
    { value: "TAMIL", label: "Tamil", apiValue: "ta" },
    { value: "TELUGU", label: "Telugu", apiValue: "te" },
    { value: "KANNADA", label: "Kannada", apiValue: "kn" },
    { value: "MALAYALAM", label: "Malayalam", apiValue: "ml" },
    { value: "GUJARATI", label: "Gujarati", apiValue: "gu" },
    { value: "MARATHI", label: "Marathi", apiValue: "mr" },
    { value: "ODIA", label: "Odia", apiValue: "or" },
    { value: "PUNJABI", label: "Punjabi", apiValue: "pa" },
    { value: "ASSAMESE", label: "Assamese", apiValue: "as" },
    { value: "NEPALI", label: "Nepali", apiValue: "ne" },
    { value: "SINDHI", label: "Sindhi", apiValue: "sd" },
    { value: "KASHMIRI", label: "Kashmiri", apiValue: "ks" },
    { value: "DOGRI", label: "Dogri", apiValue: "doi" },
    { value: "KONKANI", label: "Konkani", apiValue: "gom" },
    { value: "MAITHILI", label: "Maithili", apiValue: "mai" },
    { value: "SANSKRIT", label: "Sanskrit", apiValue: "sa" },
    { value: "SANTALI", label: "Santali", apiValue: "sat" },
    { value: "BODO", label: "Bodo", apiValue: "brx" },
    { value: "MANIPURI", label: "Manipuri", apiValue: "mni" },
  ];
  const TRANSLATE_BASE =
    config.translator_translate;
  const TENANT_ID = useSelector((state) => state.common.tenant_id);
  const SESSION_TOKEN = useSelector((state) => state.common.session_token);
  const BUSINESS_ID = useSelector((state) => state.common.business_id);

  const API_KEY = "{{token}}"; // <-- Replace dynamically later

  const wordLimit = 200;

  const handleTextAreaChange = (e) => {
    const input = e.target.value;
    setText(input);
  };

  const handleTitleChange = (e) => {
    setTitle(e.target.value);
  };

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

      // remove blue glow
      boxShadow: "none",
      borderColor: state.isFocused ? "#ccc" : "#ccc", // keep same on focus
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
        ? "#e6f0ff" // selected background
        : state.isFocused
        ? "#f5f5f5" // hover background
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

  const [isTranslating, setIsTranslating] = useState(false);

  // Store original English values for all translatable fields
  const [originalEnglishValues, setOriginalEnglishValues] = useState({
    text: "",
    title: "",
    buttonText: "",
    saveButtonText: "",
    grievanceDetailsHeading: "Grievance Details",
    selectCategoryPlaceholder: "Select Category",
    grievanceCategoryLabel: "Grievance Category",
    grievanceSubCategoryLabel: "Grievance Subcategory",
    subCategoryPlaceholder: "Enter Grievance Subcategory",
    descriptionLabel: "Description (Required)",
    descriptionPlaceholder: "Enter description of the group's function or scope.",
    userDetailsHeading: "User Details",
    userTypeLabel: "User Type",
    userTypePlaceholder: "Select user type",
    uploadFilesHeading: "Upload Files",
    uploadButtonText: "Upload",
    uploadInstruction: "Drag and drop or upload a document with hyperlink with the consent banner in .pdf or .doc format.",
    requiredLable: "Required",
    enterLable: "Enter",
  });

  // Ref to track previous language
  const prevLanguageRef = useRef(language);

  // Store original English values when in English mode
  useEffect(() => {
    if (language === "ENGLISH") {
      setOriginalEnglishValues({
        text: text || originalEnglishValues.text,
        title: title || originalEnglishValues.title,
        buttonText: buttonText || originalEnglishValues.buttonText,
        saveButtonText: saveButtonText || originalEnglishValues.saveButtonText,
        grievanceDetailsHeading: grievanceDetailsHeading || originalEnglishValues.grievanceDetailsHeading,
        selectCategoryPlaceholder: selectCategoryPlaceholder || originalEnglishValues.selectCategoryPlaceholder,
        grievanceCategoryLabel: grievanceCategoryLabel || originalEnglishValues.grievanceCategoryLabel,
        grievanceSubCategoryLabel: grievanceSubCategoryLabel || originalEnglishValues.grievanceSubCategoryLabel,
        subCategoryPlaceholder: subCategoryPlaceholder || originalEnglishValues.subCategoryPlaceholder,
        descriptionLabel: descriptionLabel || originalEnglishValues.descriptionLabel,
        descriptionPlaceholder: descriptionPlaceholder || originalEnglishValues.descriptionPlaceholder,
        userDetailsHeading: userDetailsHeading || originalEnglishValues.userDetailsHeading,
        userTypeLabel: userTypeLabel || originalEnglishValues.userTypeLabel,
        userTypePlaceholder: userTypePlaceholder || originalEnglishValues.userTypePlaceholder,
        uploadFilesHeading: uploadFilesHeading || originalEnglishValues.uploadFilesHeading,
        uploadButtonText: uploadButtonText || originalEnglishValues.uploadButtonText,
        uploadInstruction: uploadInstruction || originalEnglishValues.uploadInstruction,
        requiredLable: requiredLable || originalEnglishValues.requiredLable,
        enterLable: enterLable || originalEnglishValues.enterLable,
      });
    }
  }, [language]);

  // Auto-select first language when supportedLanguages change
  // Also handle when current language is removed from supported languages
  useEffect(() => {
    if (supportedLanguages.length === 0) return;
    
    // Check if current language is still in supported languages
    const isCurrentLangSupported = supportedLanguages.includes(language);
    
    if (!language || !isCurrentLangSupported) {
      const firstLang = supportedLanguages[0];
      setLanguage(firstLang);
      console.log("Auto-selected language:", firstLang, "(previous was:", language, ")");
      
      // Update the ref to allow the useEffect to run
      prevLanguageRef.current = language;
      
      // If switching to English, restore original values
      const langObj = options.find((o) => o.value === firstLang);
      if (langObj && langObj.apiValue === "en") {
        console.log("Restoring all English values...");
        restoreEnglishValues();
        setCurrentVal("en");
      }
    }
  }, [supportedLanguages, language]);

  // Load translated content when language changes or when languageSpecificContentMap gets updated
  useEffect(() => {
    if (!language) return;
    
    const langObj = options.find((o) => o.value === language);
    if (!langObj) return;
    
    // Check if we have translated content in the map for current language
    const existingContent = languageSpecificContentMap[language];
    if (existingContent) {
      console.log("Loading existing translation for:", language, existingContent);
      if (existingContent.description) setText(existingContent.description);
      if (existingContent.heading) setTitle(existingContent.heading);
      setCurrentVal(langObj.apiValue);
    }
  }, [language, languageSpecificContentMap]);

  useEffect(() => {
    if (language === "ENGLISH") {
      setLanguageSpecificContentMap((prev) => ({
        ...prev,
        ENGLISH: {
          description: text,
          heading: title,
        },
      }));
    }
  }, [text, title]);

  const translateLanguage = async (langObj) => {
    if (!langObj) return;

    if (langObj.apiValue === "en") {
      // English already handled by useEffect
      return;
    }

    try {
      const txn = generateTransactionId();
      const body = {
        provider: "BHASHINI",
        source: "CONSENTPOPUP",
        language: {
          sourceLanguage: "en",
          targetLanguage: langObj.apiValue,
        },
        input: [
          { id: "title", source: title || "" },
          { id: "text", source: text || "" },
        ],
      };
      const res = await axios.post(TRANSLATE_BASE, body, {
        headers: {
          "Content-Type": "application/json",
          tenantid: TENANT_ID,
          businessid: BUSINESS_ID,
          txn,
          "x-session-token": SESSION_TOKEN,
          "X-API-Key": API_KEY,
        },
        timeout: 20000,
      });

      const translatedTitle =
        res.data.output.find((item) => item.id === "title")?.target || "";

      // Combine all remaining targets into description
      const description = res.data.output
        .filter((item) => item.id !== "title")
        .map((item) => item.target)
        .join(" ");

      setLanguageSpecificContentMap((prev) => ({
        ...prev,
        [langObj.value]: {
          description,
          heading: translatedTitle,
        },
      }));
    } catch (err) {
      console.error(`Translation failed for ${langObj.label}:`, err);
    }
  };

  const [currentVal, setCurrentVal] = useState("en");
  const multilingualConfigSaved = useSelector((state) => state.common.multilingualConfigSaved);
  const navigate = useNavigate();
  const [showMultilingualWarningModal, setShowMultilingualWarningModal] = useState(false);

  // Helper function to restore all English values
  const restoreEnglishValues = () => {
    console.log("Restoring all English values:", originalEnglishValues);
    
    // Restore main content
    if (originalEnglishValues.text) setText(originalEnglishValues.text);
    if (originalEnglishValues.title) setTitle(originalEnglishValues.title);
    
    // Restore button texts
    if (originalEnglishValues.buttonText && setButtonText) setButtonText(originalEnglishValues.buttonText);
    if (originalEnglishValues.saveButtonText && setSaveButtonText) setSaveButtonText(originalEnglishValues.saveButtonText);
    
    // Restore all labels and headings
    if (setGrievanceDetailsHeading) setGrievanceDetailsHeading(originalEnglishValues.grievanceDetailsHeading);
    if (setSelectCategoryPlaceholder) setSelectCategoryPlaceholder(originalEnglishValues.selectCategoryPlaceholder);
    if (setGrievanceCategoryLabel) setGrievanceCategoryLabel(originalEnglishValues.grievanceCategoryLabel);
    if (setGrievanceSubCategoryLabel) setGrievanceSubCategoryLabel(originalEnglishValues.grievanceSubCategoryLabel);
    if (setSubCategoryPlaceholder) setSubCategoryPlaceholder(originalEnglishValues.subCategoryPlaceholder);
    if (setDescriptionLabel) setDescriptionLabel(originalEnglishValues.descriptionLabel);
    if (setDescriptionPlaceholder) setDescriptionPlaceholder(originalEnglishValues.descriptionPlaceholder);
    if (setUserDetailsHeading) setUserDetailsHeading(originalEnglishValues.userDetailsHeading);
    if (setUserTypeLabel) setUserTypeLabel(originalEnglishValues.userTypeLabel);
    if (setUserTypePlaceholder) setUserTypePlaceholder(originalEnglishValues.userTypePlaceholder);
    if (setUploadFilesHeading) setUploadFilesHeading(originalEnglishValues.uploadFilesHeading);
    if (setUploadButtonText) setUploadButtonText(originalEnglishValues.uploadButtonText);
    if (setUploadInstruction) setUploadInstruction(originalEnglishValues.uploadInstruction);
    if (setRequiredLable) setRequiredLable(originalEnglishValues.requiredLable);
    if (setEnterLable) setEnterLable(originalEnglishValues.enterLable);
  };

  const handleLanguageSelect = async (e, skipConfigCheck = false) => {
    const selectedLang = e?.target?.value || e;
    
    const langObj = options.find((o) => o.value === selectedLang);
    if (!langObj) return;
    
    // Handle switching to English - restore original values
    if (langObj.apiValue === "en" || selectedLang === "ENGLISH") {
      setLanguage(selectedLang);
      console.log("Switching to English, restoring original values...");
      restoreEnglishValues();
      setCurrentVal("en");
      return;
    }
    
    // Check if multilingual config is saved (only for non-English languages)
    // Skip check if called from auto-translation
    if (!skipConfigCheck && !multilingualConfigSaved) {
      setShowMultilingualWarningModal(true);
      // Reset dropdown to previous value
      if (e?.target) e.target.value = language || "";
      return;
    }
    
    setLanguage(selectedLang);

    try {
      showLoader();
      console.log("🚀 Starting translation for:", selectedLang);
      const txn = generateTransactionId();

      const translationKeys = [];

      // 1. detailNames - check if userData2 exists
      if (userData2?.detailNames) {
        userData2.detailNames.forEach((name) => {
          translationKeys.push({ id: name, source: name });
        });
      }

      // 2. purposeNames
      if (userData2?.purposeNames) {
        userData2.purposeNames.forEach((name) => {
          translationKeys.push({ id: name, source: name });
        });
      }

      // 3. grievances
      if (userData2?.grievances) {
        Object.entries(userData2.grievances).forEach(([type, details]) => {
          // Add type
          translationKeys.push({ id: type, source: type });

          // Add all details under the type
          if (Array.isArray(details)) {
            details.forEach((detail) => {
              translationKeys.push({ id: detail, source: detail });
            });
          }
        });
      }
      
      console.log("📝 Translation keys:", translationKeys.length);

      const body = {
        provider: "BHASHINI",
        source: "CONSENTPOPUP",
        language: {
          sourceLanguage: currentVal, // previously 'from'
          targetLanguage: langObj.apiValue, // previously 'to'
        },

        input: [
          { id: "text", source: text || "" },
          { id: "title", source: title || "" },
          { id: "buttonText", source: buttonText || "" },
          { id: "saveButtonText", source: saveButtonText || "" },
          { id: "others", source: others || "" },
          {
            id: "grievanceDetailsHeading",
            source: grievanceDetailsHeading || "",
          },
          {
            id: "grievanceCategoryLabel",
            source: grievanceCategoryLabel || "",
          },
          {
            id: "selectCategoryPlaceholder",
            source: selectCategoryPlaceholder || "",
          },
          {
            id: "grievanceSubCategoryLabel",
            source: grievanceSubCategoryLabel || "",
          },
          {
            id: "subCategoryPlaceholder",
            source: subCategoryPlaceholder || "",
          },
          { id: "descriptionLabel", source: descriptionLabel || "" },
          {
            id: "descriptionPlaceholder",
            source: descriptionPlaceholder || "",
          },
          { id: "userDetailsHeading", source: userDetailsHeading || "" },
          { id: "userTypeLabel", source: userTypeLabel || "" },
          { id: "userTypePlaceholder", source: userTypePlaceholder || "" },
          { id: "uploadFilesHeading", source: uploadFilesHeading || "" },
          { id: "uploadButtonText", source: uploadButtonText || "" },
          { id: "uploadInstruction", source: uploadInstruction || "" },
          { id: "requiredLable", source: requiredLable || "" },
          { id: "enterLable", source: enterLable || "" },
          ...translationKeys,
        ],
      };

      const mainRes = await axios.post(TRANSLATE_BASE, body, {
        headers: {
          "Content-Type": "application/json",
          tenantid: TENANT_ID,
          businessid: BUSINESS_ID,
          txn,
          "x-session-token": SESSION_TOKEN,
          "X-API-Key": API_KEY,
        },
        timeout: 20000,
      });
      console.log("translation response", mainRes);

      // Extract translated texts from the API response
      const translatedMap = mainRes.data.output.reduce((acc, item) => {
        acc[item.id] = item.target;
        return acc;
      }, {});
      // Update state with translated values
      setCurrentVal(langObj.apiValue);
      setText(translatedMap.text || "");
      setTitle(translatedMap.title || "");
      setButtonText(translatedMap.buttonText || "");
      setSaveButtonText(translatedMap.saveButtonText || "");
      setOthers(translatedMap.others || "");
      setGrievanceDetailsHeading(translatedMap.grievanceDetailsHeading || "");
      setGrievanceCategoryLabel(translatedMap.grievanceCategoryLabel || "");
      setSelectCategoryPlaceholder(
        translatedMap.selectCategoryPlaceholder || ""
      );
      setGrievanceSubCategoryLabel(
        translatedMap.grievanceSubCategoryLabel || ""
      );
      setSubCategoryPlaceholder(translatedMap.subCategoryPlaceholder || "");
      setDescriptionLabel(translatedMap.descriptionLabel || "");
      setDescriptionPlaceholder(translatedMap.descriptionPlaceholder || "");
      setUserDetailsHeading(translatedMap.userDetailsHeading || "");
      setUserTypeLabel(translatedMap.userTypeLabel || "");
      setUserTypePlaceholder(translatedMap.userTypePlaceholder || "");
      setUploadFilesHeading(translatedMap.uploadFilesHeading || "");
      setUploadButtonText(translatedMap.uploadButtonText || "");
      setUploadInstruction(translatedMap.uploadInstruction || "");
      setRequiredLable(translatedMap.requiredLable || "");
      setEnterLable(translatedMap.enterLable || "");
      //
      // Replace original detailNames with translated values

      userData2.detailNames = userData2.detailNames.map(
        (name) => translatedMap[name] || name
      );
      userData2.purposeNames = userData2.purposeNames.map(
        (name) => translatedMap[name] || name
      );
      // grievances
      Object.keys(userData2.grievances).forEach((type) => {
        const translatedType = translatedMap[type] || type;

        const translatedDetails = userData2.grievances[type].map(
          (detail) => translatedMap[detail] || detail
        );

        // replace with translated type and details
        if (translatedType !== type) {
          userData2.grievances[translatedType] = translatedDetails;
          delete userData2.grievances[type];
        } else {
          userData2.grievances[type] = translatedDetails;
        }
      });
    } catch (err) {
      console.error(`❌ Translation failed for ${langObj.label}:`, err);
      console.error("Error details:", err.response?.data || err.message);

      // Always stop the loader in case of failure
      hideLoader();

      let errors = [];

      // Extract error messages from API response
      if (
        err.response?.data?.errors &&
        Array.isArray(err.response.data.errors)
      ) {
        errors = err.response.data.errors.map(
          (e) => e.errorMessage || "Unknown error occurred."
        );
      } else if (err.response?.data?.message) {
        errors = [err.response.data.message];
      } 

    } finally {
      hideLoader();
    }
  };

  const handleLanguageChange = async (selected) => {
    // selected = array of { value, label, apiValue } OR null
    const langs = selected ? selected.map((s) => s.value) : [];

    // find which ones are new compared to existing state
    const newLangs = langs.filter((lang) => !supportedLanguages.includes(lang));
    
    // Check if this is the first language being added
    const isFirstLanguage = supportedLanguages.length === 0 && langs.length === 1;

    // update supported languages
    setSupportedLanguages(langs);

    // trigger basic translation for each new language (for languageSpecificContentMap)
    newLangs.forEach((lang) => {
      const langObj = options.find((o) => o.value === lang);
      translateLanguage(langObj);
    });
    
    // If this is the first language being added, auto-select it and trigger full translation
    if (isFirstLanguage && newLangs.length === 1) {
      const firstLang = newLangs[0];
      const langObj = options.find((o) => o.value === firstLang);
      
      console.log("🎯 First language added, triggering full translation:", firstLang);
      
      // Set the language
      setLanguage(firstLang);
      
      // If not English, trigger full translation after a small delay
      if (langObj && langObj.apiValue !== "en") {
        setTimeout(async () => {
          console.log("⏳ Delayed full translation for:", firstLang);
          setIsTranslating(true);
          try {
            await handleLanguageSelect({ target: { value: firstLang } }, true);
          } catch (err) {
            console.error("❌ Auto-translation failed:", err);
          } finally {
            setIsTranslating(false);
          }
        }, 500);
      }
    }
  };

  useEffect(() => {
    const requestBody = {
      multilingual: {
        supportedLanguages,
        languageSpecificContentMap,
      },
    };
  }, [supportedLanguages, languageSpecificContentMap]);

  return (
    <div className="language-con">
      <div className="language-heading">
        <Text appearance="heading-xxs" color="primary-grey-80">
          Multi-lingual support
        </Text>
      </div>
      <div className="language-select" style={{ padding: "0px 15px" }}>
        <Text appearance="body-xs" color="primary-grey-80">
          Select languages
        </Text>
        <Select
          isMulti
          options={options}
          value={options.filter((opt) =>
            supportedLanguages.includes(opt.value)
          )}
          // onChange={(selected) =>
          //     setSupportedLanguages(selected ? selected.map((s) => s.value) : [])
          // }
          onChange={handleLanguageChange}
          styles={customStyles}
        />
      </div>
      <div className="language-heading">
        <Text appearance="heading-xxs" color="primary-grey-80">
          Customize content
        </Text>
      </div>

      <div className="dropdown-group">
        <label>Select language</label>
        <select
          value={language}
          id="select-language"
          onChange={handleLanguageSelect}
          style={{ width: "100%" }}
        >
          <option value="" disabled>
            Select
          </option>
          {supportedLanguages.map((lang) => {
            const option = options.find((o) => o.value === lang);
            return (
              <option key={lang} value={option.value}>
                {option.label}
              </option>
            );
          })}
        </select>
      </div>

      <div style={{ marginBottom: "20px", padding: "0px 15px" }}>
        <InputFieldV2
          label="Heading (Required)"
          name="Heading"
          size="small"
          type="text"
          value={title}
          onChange={handleTitleChange}
        />
      </div>

      <div className="dropdown-group" style={{ padding: "0px 15px" }}>
        <label>Description (Required)</label>

        <textarea
          placeholder="Enter description of the group's function or scope."
          value={text}
          onChange={handleTextAreaChange}
          rows="3"
          className="custom-text-area"
        />
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

      {/* Multilingual Warning Modal */}
      {showMultilingualWarningModal && (
        <div className="modal-overlay" onClick={() => setShowMultilingualWarningModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '500px' }}>
            <div className="modal-header">
              <h2 style={{ margin: 0, fontSize: '20px', fontWeight: '600' }}>
                Multilingual Support Required
              </h2>
              <button
                className="modal-close-btn"
                onClick={() => setShowMultilingualWarningModal(false)}
                style={{
                  background: 'none',
                  border: 'none',
                  fontSize: '24px',
                  cursor: 'pointer',
                  color: '#666',
                }}
              >
                ×
              </button>
            </div>
            
            <div className="modal-body">
              <Text appearance="body-s" color="primary-grey-80" style={{ marginBottom: '24px' }}>
                You need to configure multilingual support in the consent configuration to enable the translate functionality.
              </Text>

              <div style={{
                display: 'flex',
                gap: '12px',
                justifyContent: 'flex-end',
              }}>
                <Button
                  kind="secondary"
                  size="medium"
                  label="Cancel"
                  onClick={() => setShowMultilingualWarningModal(false)}
                />
                <ActionButton
                  kind="primary"
                  size="medium"
                  label="Go to Configuration"
                  onClick={() => {
                    setShowMultilingualWarningModal(false);
                    navigate("/consent");
                  }}
                />
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default FormLanguageContainer;
