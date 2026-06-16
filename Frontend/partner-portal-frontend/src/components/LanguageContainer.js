import React, { useState, useEffect, useRef } from "react";
import "../styles/languageContainer.css";
import { useState } from "react";
import { ActionButton, Icon, Text, InputFieldV2, InputRadio } from "../custom-components";
import Select from "react-select";
import axios from "axios";
import config from "../utils/config";
import { generateTransactionId } from "../utils/transactionId";
import { useSelector, useDispatch } from "react-redux";
import { useNavigate } from "react-router-dom";
import { Button, ActionButton } from '../custom-components';


const LanguageContainer = ({
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
  onTranslationStateChange,
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
  const TRANSLATE_BASE = config.translator_translate;
  const TENANT_ID = useSelector((state) => state.common.tenant_id);
  const SESSION_TOKEN = useSelector((state) => state.common.session_token);
  const BUSINESS_ID = useSelector((state) => state.common.business_id);
  const multilingualConfigSaved = useSelector((state) => state.common.multilingualConfigSaved);
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [showMultilingualWarningModal, setShowMultilingualWarningModal] = useState(false);

  const API_KEY = "{{token}}";

  // Check and set multilingual config if not already set
  useEffect(() => {
    const verifyMultilingualConfig = async () => {
      // If already verified, skip
      if (multilingualConfigSaved) return;
      if (!TENANT_ID || !SESSION_TOKEN || !BUSINESS_ID) return;

      try {
        const response = await fetch(
          `${config.get_consent_details}?businessId=${BUSINESS_ID}`,
          {
            method: "GET",
            headers: {
              accept: "*/*",
              "tenant-id": TENANT_ID,
              txn: generateTransactionId(),
              "x-session-token": SESSION_TOKEN,
            },
          }
        );

        if (response.ok) {
          const data = await response.json();
          const consentConfig = data?.searchList?.[0]?.configurationJson;
          
          if (consentConfig?.isMultilingualSupport) {
            dispatch({ type: "SAVE_MULTILINGUAL_CONFIG", payload: true });
            console.log('✅ LanguageContainer: Multilingual config verified');
          }
        }
      } catch (error) {
        console.error('❌ LanguageContainer: Error verifying multilingual config:', error);
      }
    };

    verifyMultilingualConfig();
  }, [multilingualConfigSaved, TENANT_ID, SESSION_TOKEN, BUSINESS_ID, dispatch]);

  // const handleLanguageChange = async (e) => {
  //   const selectedValue = e.target.value; // e.g. "HINDI"
  //   setLanguage(selectedValue);

  //   const selectedOption = options.find((o) => o.value === selectedValue);
  //   console.log("Selected option:", selectedOption);

  //   try {
  //     const response = await fetch("http://10.144.34.38:9023/translate/bhashini", {
  //       method: "POST",
  //       headers: { "Content-Type": "application/json" },
  //       body: JSON.stringify({
  //         from: "en",
  //         to: selectedOption.apiValue,
  //         texts: [
  //           text,
  //           permission,
  //           rights
  //         ],
  //       }),
  //     });

  //     const data = await response.json();
  //     console.log("Translated:", data);

  //   } catch (err) {
  //     console.error("API error:", err);
  //   }
  // };

  const wordLimit = 300;

  const handleTextAreaChange = (e) => {
    let words = e.target.value.split(/\s+/).filter(Boolean);
    if (words.length <= wordLimit) {
      setText(e.target.value);
    } else {
      setText(words.slice(0, wordLimit).join(" "));
    }
  };

  const handleLabelChange = (e) => {
    setLabel(e.target.value);
  };

  const handleRightsChange = (e) => {
    let words = e.target.value.split(/\s+/).filter(Boolean);
    if (words.length <= wordLimit) {
      setRights(e.target.value);
    } else {
      setRights(words.slice(0, wordLimit).join(" "));
    }
  };

  const handlePermissionChange = (e) => {
    let words = e.target.value.split(/\s+/).filter(Boolean);
    if (words.length <= wordLimit) {
      setPermission(e.target.value);
    } else {
      setPermission(words.slice(0, wordLimit).join(" "));
    }
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

  // Track if we need to trigger full translation after auto-selecting first language
  const [pendingFullTranslation, setPendingFullTranslation] = useState(null);

  // Ref to track previous language to detect actual language changes
  const prevLanguageRef = useRef(language);

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
      prevLanguageRef.current = language; // Store old value so change is detected
      
      // If switching to English, restore original values
      const langObj = options.find((o) => o.value === firstLang);
      if (langObj && langObj.apiValue === "en") {
        console.log("Restoring English values...");
        // Restore English values
        if (originalEnglishValues.text || originalEnglishValues.title) {
          setText(originalEnglishValues.text);
          setRights(originalEnglishValues.rights);
          setPermission(originalEnglishValues.permission);
          setTitle(originalEnglishValues.title);
          setCid(originalEnglishValues.cid);
          setAge(originalEnglishValues.age);
          setLabel(originalEnglishValues.label);
          setPurposeHeading(originalEnglishValues.purposeHeading);
          setProcessingHeading(originalEnglishValues.processingHeading);
          setUsedByHeading(originalEnglishValues.usedByHeading);
          setDurationHeading(originalEnglishValues.durationHeading);
          setDataItemHeading(originalEnglishValues.dataItemHeading);
          setButtonText(originalEnglishValues.buttonText);
          setSaveButtonText(originalEnglishValues.saveButtonText);
          setOthers(originalEnglishValues.others);
          
          if (originalEnglishValues.purposes && originalEnglishValues.purposes.length > 0 && onPurposesChange) {
            onPurposesChange(originalEnglishValues.purposes);
          }
        } else {
          // Fallback to languageSpecificContentMap
          const englishContent = languageSpecificContentMap.ENGLISH;
          if (englishContent) {
            setText(englishContent.description || text);
            setRights(englishContent.rightsText || rights);
            setPermission(englishContent.permissionText || permission);
            setLabel(englishContent.label || label);
            setTitle(englishContent.title || title);
          }
        }
        setCurrentVal("en");
      } else if (langObj && langObj.apiValue !== "en") {
        // Mark that we need full translation for this non-English language
        setPendingFullTranslation(firstLang);
      }
    }
  }, [supportedLanguages, language]);

  // Load translated content ONLY when language actually changes (not on content updates)
  useEffect(() => {
    if (!language) return;
    
    // Skip if language hasn't actually changed (prevents flickering during typing)
    if (prevLanguageRef.current === language) {
      return;
    }
    
    // Update the previous language ref
    prevLanguageRef.current = language;
    
    const langObj = options.find((o) => o.value === language);
    if (!langObj) return;
    
    // If English, restore original values
    if (langObj.apiValue === "en") {
      if (originalEnglishValues.text || originalEnglishValues.title) {
        setText(originalEnglishValues.text);
        setRights(originalEnglishValues.rights);
        setPermission(originalEnglishValues.permission);
        setTitle(originalEnglishValues.title);
        setCid(originalEnglishValues.cid);
        setAge(originalEnglishValues.age);
        setLabel(originalEnglishValues.label);
      }
      setCurrentVal("en");
      return;
    }
    
    // Check if we have translated content in the map for current language
    const existingContent = languageSpecificContentMap[language];
    if (existingContent && existingContent.description) {
      console.log("Loading existing translation for:", language, existingContent);
      setText(existingContent.description);
      if (existingContent.rightsText) setRights(existingContent.rightsText);
      if (existingContent.permissionText) setPermission(existingContent.permissionText);
      if (existingContent.label) setLabel(existingContent.label);
      if (existingContent.title) setTitle(existingContent.title);
      if (existingContent.cid) setCid(existingContent.cid);
      if (existingContent.age) setAge(existingContent.age);
      setCurrentVal(langObj.apiValue);
    }
  }, [language, languageSpecificContentMap]);

  useEffect(() => {
    if (language === "ENGLISH") {
      setLanguageSpecificContentMap((prev) => ({
        ...prev,
        ENGLISH: {
          description: text,
          rightsText: rights,
          permissionText: permission,
          label,
          title,
          cid,
          age,
          purposeHeading,
          processingHeading,
          usedByHeading,
          durationHeading,
          dataItemHeading,
          buttonText,
          saveButtonText,
          others,
        },
      }));
      
      // Store original English values when in English mode and values exist
      if (text || title) {
        setOriginalEnglishValues({
          text,
          rights,
          permission,
          title,
          cid,
          age,
          label,
          purposeHeading,
          processingHeading,
          usedByHeading,
          durationHeading,
          dataItemHeading,
          buttonText,
          saveButtonText,
          others,
          purposes: purposes.map(p => ({ ...p })) // Deep copy
        });
      }
    } else {
      // If we're not in English mode but have English content in languageSpecificContentMap, store it
      if (languageSpecificContentMap.ENGLISH && !originalEnglishValues.text) {
        const engContent = languageSpecificContentMap.ENGLISH;
        setOriginalEnglishValues({
          text: engContent.description || text,
          rights: engContent.rightsText || rights,
          permission: engContent.permissionText || permission,
          title: engContent.title || title,
          cid: engContent.cid || cid,
          age: engContent.age || age,
          label: engContent.label || label,
          purposeHeading: engContent.purposeHeading || purposeHeading,
          processingHeading: engContent.processingHeading || processingHeading,
          usedByHeading: engContent.usedByHeading || usedByHeading,
          durationHeading: engContent.durationHeading || durationHeading,
          dataItemHeading: engContent.dataItemHeading || dataItemHeading,
          buttonText: engContent.buttonText || buttonText,
          saveButtonText: engContent.saveButtonText || saveButtonText,
          others: engContent.others || others,
          purposes: purposes.map(p => ({ ...p })) // Deep copy
        });
      }
    }
  }, [text, rights, permission, label, title, cid, age, purposeHeading, processingHeading, usedByHeading, durationHeading, dataItemHeading, buttonText, saveButtonText, others, purposes, language, languageSpecificContentMap]);

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
          { id: "description", source: text || "" },
          { id: "label", source: label || "" },
          { id: "rightsText", source: rights || "" },
          { id: "permissionText", source: permission || "" },
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
      const output = res.data.output || [];

      const translatedDescription =
        output.find((item) => item.id === "description")?.target || "";

      const translatedLabel =
        output.find((item) => item.id === "label")?.target || "";

      const rightsTextTranslated =
        output.find((item) => item.id === "rightsText")?.target || "";

      const permissionTextTranslated =
        output.find((item) => item.id === "permissionText")?.target || "";


      setLanguageSpecificContentMap((prev) => ({
        ...prev,
        [langObj.value]: {
          description: translatedDescription,
          label: translatedLabel,
          rightsText: rightsTextTranslated,
          permissionText: permissionTextTranslated,
        },
      }));
    } catch (err) {
      console.error(`Translation failed for ${langObj.label}:`, err);
    }
  };

  // const handleLanguageChange = (selected) => {
  //   console.log("Selected languages:", selected);
  //   const langs = selected ? selected.map((s) => s.value) : [];
  //   const newLangs = langs.filter((lang) => !supportedLanguages.includes(lang));

  //   setSupportedLanguages(langs);
  //   console.log("Newly added languages:", newLangs);

  //   newLangs.forEach((lang) => {
  //     const langObj = options.find((o) => o.value === lang);
  //     translateLanguage(langObj);
  //   });
  // };

  const [currentVal, setCurrentVal] = useState("en");
  const [isTranslating, setIsTranslating] = useState(false);
  
  // Store original English values
  const [originalEnglishValues, setOriginalEnglishValues] = useState({
    text: "",
    rights: "",
    permission: "",
    title: "",
    cid: "",
    age: "",
    label: "",
    purposeHeading: "",
    processingHeading: "",
    usedByHeading: "",
    durationHeading: "",
    dataItemHeading: "",
    buttonText: "",
    saveButtonText: "",
    others: "",
    purposes: []
  });

  // const handleLanguageSelect = async (e) => {
  //   const selectedLang = e.target.value;
  //   setLanguage(selectedLang); // keep track of selected

  //   const langObj = options.find((o) => o.value === selectedLang);

  //   if (!langObj || langObj.apiValue === "en") return;

  //   const allPurposeNames = purposes.flatMap(p => p.purposeNames).join(", ");
  //   const allProcessingActs = purposes.flatMap(p => p.processingAct).join(", ");
  //   const allUsedBy = purposes.flatMap(p => p.usedBy).join(", ");
  //   const allDataItems = purposes.flatMap(p => p.dataItems).join(", ");
  //   const allPreferenceValidity = purposes
  //     .map(p => `${p.preferenceValidity.value} ${p.preferenceValidity.unit}`)
  //     .join(", ");

  //   try {
  //     const res = await axios.post(
  //       "http://10.144.34.38:9023/translate/bhashini",
  //       {
  //         from: currentVal,
  //         to: langObj.apiValue,
  //         texts: [text, rights, permission, title, cid, age, label, 
  //           allPurposeNames,
  //           allProcessingActs,
  //           allUsedBy,
  //           allDataItems,
  //           allPreferenceValidity
  //         ], // always translate latest
  //       }
  //     );

  //     const [
  //       translatedText,
  //       translatedRights,
  //       translatedPermission,
  //       translatedtitle,
  //       translatedcid,
  //       translatedAge,
  //       translateLabel,
  //       translatedPurposeNames,
  //       translatedProcessingActs,
  //       translatedUsedBy,
  //       translatedDataItems,
  //       translatedPreferenceValidity,
  //     ] = res.data.translatedTexts;

  //     setCurrentVal(langObj.apiValue);

  //     // update parent states so both input + popup reflect it
  //     setText(translatedText);
  //     setRights(translatedRights);
  //     setPermission(translatedPermission);
  //     setTitle(translatedtitle);
  //     setCid(translatedcid);
  //     setAge(translatedAge);
  //     setLabel(translateLabel);

  //     setPurposes(prev =>
  //       prev.map(purpose => ({
  //         ...purpose,
  //         purposeNames: translatedPurposeNames.split(", "),     // break back into array
  //         processingAct: translatedProcessingActs.split(", "),
  //         usedBy: translatedUsedBy.split(", "),
  //         dataItems: translatedDataItems.split(", "),
  //         preferenceValidity: {
  //           value: translatedPreferenceValidity.split(" ")[0] || purpose.preferenceValidity.value,
  //           unit: translatedPreferenceValidity.split(" ")[1] || purpose.preferenceValidity.unit,
  //         },
  //       }))
  //     );

  //     console.log("Translated Text:", translatedText);
  //     console.log("Translated Rights:", translatedRights);
  //     console.log("Translated Permission:", translatedPermission);
  //     console.log("Translated Purposes:", translatedPurposeNames);
  //     console.log("Translated ProcessingActs:", translatedProcessingActs);

  //     // optional: also update languageSpecificContentMap
  //     setLanguageSpecificContentMap((prev) => ({
  //       ...prev,
  //       [langObj.value]: {
  //         description: translatedText,
  //         rightsText: translatedRights,
  //         permissionText: translatedPermission,
  //         label: translateLabel,
  //       },
  //     }));
  //   } catch (err) {
  //     console.error(`Translation failed for ${langObj.label}:`, err);
  //   }
  // };

  const handleLanguageSelect = async (e) => {
    const selectedLang = e?.target?.value || e;
    
    const langObj = options.find((o) => o.value === selectedLang);
    if (!langObj) return;
    
    // Handle switching back to English
    if (langObj.apiValue === "en") {
      setIsTranslating(true);
      try {
        // If we have stored original English values, restore them
        if (originalEnglishValues.text || originalEnglishValues.title) {
          setText(originalEnglishValues.text);
          setRights(originalEnglishValues.rights);
          setPermission(originalEnglishValues.permission);
          setTitle(originalEnglishValues.title);
          setCid(originalEnglishValues.cid);
          setAge(originalEnglishValues.age);
          setLabel(originalEnglishValues.label);
          setPurposeHeading(originalEnglishValues.purposeHeading);
          setProcessingHeading(originalEnglishValues.processingHeading);
          setUsedByHeading(originalEnglishValues.usedByHeading);
          setDurationHeading(originalEnglishValues.durationHeading);
          setDataItemHeading(originalEnglishValues.dataItemHeading);
          setButtonText(originalEnglishValues.buttonText);
          setSaveButtonText(originalEnglishValues.saveButtonText);
          setOthers(originalEnglishValues.others);
          
          // Restore original purposes if available
          if (originalEnglishValues.purposes && originalEnglishValues.purposes.length > 0) {
            if (onPurposesChange) {
              onPurposesChange(originalEnglishValues.purposes);
            }
          }
        } else {
          // If no originals stored, try to get from languageSpecificContentMap
          const englishContent = languageSpecificContentMap.ENGLISH;
          if (englishContent) {
            setText(englishContent.description || text);
            setRights(englishContent.rightsText || rights);
            setPermission(englishContent.permissionText || permission);
            setLabel(englishContent.label || label);
          }
        }
        
        setCurrentVal("en");
        setLanguage(selectedLang);
      } catch (err) {
        console.error("Error restoring English values:", err);
      } finally {
        setIsTranslating(false);
        if (onTranslationStateChange) {
          onTranslationStateChange(false);
        }
      }
      return;
    }
    
    // Check if multilingual config is saved
    console.log('🔍 LanguageContainer - multilingualConfigSaved:', multilingualConfigSaved);
    if (!multilingualConfigSaved) {
      console.log('⚠️ Showing multilingual warning modal because multilingualConfigSaved is false');
      setShowMultilingualWarningModal(true);
      // Reset dropdown to previous value
      e.target.value = language || "";
      return;
    }
    
    // Ensure we have English originals before translating
    // If we're switching from non-English and don't have originals, we need to translate current values back to English first
    let sourceTexts = {
      text,
      rights,
      permission,
      title,
      cid,
      age,
      label,
      purposeHeading,
      processingHeading,
      usedByHeading,
      durationHeading,
      dataItemHeading,
      buttonText,
      saveButtonText,
      others
    };
    let sourcePurposes = purposes;
    let sourceLanguage = currentVal;
    
    // If we have stored English originals, always use them as source
    if (originalEnglishValues.text || originalEnglishValues.title) {
      sourceTexts = {
        text: originalEnglishValues.text,
        rights: originalEnglishValues.rights,
        permission: originalEnglishValues.permission,
        title: originalEnglishValues.title,
        cid: originalEnglishValues.cid,
        age: originalEnglishValues.age,
        label: originalEnglishValues.label,
        purposeHeading: originalEnglishValues.purposeHeading,
        processingHeading: originalEnglishValues.processingHeading,
        usedByHeading: originalEnglishValues.usedByHeading,
        durationHeading: originalEnglishValues.durationHeading,
        dataItemHeading: originalEnglishValues.dataItemHeading,
        buttonText: originalEnglishValues.buttonText,
        saveButtonText: originalEnglishValues.saveButtonText,
        others: originalEnglishValues.others
      };
      sourcePurposes = originalEnglishValues.purposes.length > 0 ? originalEnglishValues.purposes : purposes;
      sourceLanguage = "en";
    } else if (currentVal === "en") {
      // Store current English values as originals before translating
      setOriginalEnglishValues({
        text,
        rights,
        permission,
        title,
        cid,
        age,
        label,
        purposeHeading,
        processingHeading,
        usedByHeading,
        durationHeading,
        dataItemHeading,
        buttonText,
        saveButtonText,
        others,
        purposes: purposes.map(p => ({ ...p })) // Deep copy
      });
    } else {
      // We're switching from non-English to another non-English without originals
      // Translate current values back to English first, then to target language
      // For now, we'll translate from current language directly
      console.warn("No English originals found, translating from current language");
    }
    
    setLanguage(selectedLang);
    setIsTranslating(true);
    
    // Notify parent component about translation state
    if (onTranslationStateChange) {
      onTranslationStateChange(true);
    }

    try {
      const txn = generateTransactionId();

      // 1️⃣ Translate main texts - always from English originals
      const mainBody = {
        provider: "BHASHINI",
        source: "CONSENTPOPUP",
        language: {
          sourceLanguage: sourceLanguage,
          targetLanguage: langObj.apiValue,
        },
        input: [
          { id: "text", source: sourceTexts.text || "" },
          { id: "rights", source: sourceTexts.rights || "" },
          { id: "permission", source: sourceTexts.permission || "" },
          { id: "title", source: sourceTexts.title || "" },
          { id: "cid", source: sourceTexts.cid || "" },
          { id: "age", source: sourceTexts.age || "" },
          { id: "label", source: sourceTexts.label || "" },
          { id: "purposeHeading", source: sourceTexts.purposeHeading || "" },
          { id: "processingHeading", source: sourceTexts.processingHeading || "" },
          { id: "usedByHeading", source: sourceTexts.usedByHeading || "" },
          { id: "durationHeading", source: sourceTexts.durationHeading || "" },
          { id: "dataItemHeading", source: sourceTexts.dataItemHeading || "" },
          { id: "buttonText", source: sourceTexts.buttonText || "" },
          { id: "saveButtonText", source: sourceTexts.saveButtonText || "" },
          { id: "others", source: sourceTexts.others || "" },
        ],
      };

      const mainRes = await axios.post(TRANSLATE_BASE, mainBody, {
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

      const output = mainRes.data.output || [];

      const getTranslated = (id) => output.find((o) => o.id === id)?.target || "";

      // Update all text fields with translated values
      setText(getTranslated("text"));
      setRights(getTranslated("rights"));
      setPermission(getTranslated("permission"));
      setTitle(getTranslated("title"));
      setCid(getTranslated("cid"));
      setAge(getTranslated("age"));
      setLabel(getTranslated("label"));
      setPurposeHeading(getTranslated("purposeHeading"));
      setProcessingHeading(getTranslated("processingHeading"));
      setUsedByHeading(getTranslated("usedByHeading"));
      setDurationHeading(getTranslated("durationHeading"));
      setDataItemHeading(getTranslated("dataItemHeading"));
      setButtonText(getTranslated("buttonText"));
      setSaveButtonText(getTranslated("saveButtonText"));
      setOthers(getTranslated("others"));

      // Prepare fields for bulk purpose translation - use source purposes
      const purposeNamesList = sourcePurposes.map((p) => p.purposeNames.join(", "));
      const processingActsList = sourcePurposes.map((p) => p.processingAct.join(", "));
      const usedByList = sourcePurposes.map((p) => p.usedBy.join(", "));
      const dataItemsList = sourcePurposes.map((p) => p.dataItems.join(", "));
      const preferenceValidityList = sourcePurposes.map(
        (p) => `${p.preferenceValidity.value} ${p.preferenceValidity.unit}`
      );

      // 3️⃣ Make one structured API call per field type (in parallel)
      const [
        purposeNamesRes,
        processingActsRes,
        usedByRes,
        dataItemsRes,
        // preferenceValidityRes, // can be enabled later
      ] = await Promise.all([
        axios.post(
          TRANSLATE_BASE,
          {
            provider: "BHASHINI",
            source: "CONSENTPOPUP",
            language: {
              sourceLanguage: sourceLanguage,
              targetLanguage: langObj.apiValue,
            },
            input: purposeNamesList.map((t, i) => ({
              id: `purposeName_${i}`,
              source: t,
            })),
          },
          {
            headers: {
              "Content-Type": "application/json",
              tenantid: TENANT_ID,
              businessid: BUSINESS_ID,
              txn: generateTransactionId(),
              "x-session-token": SESSION_TOKEN,
              "X-API-Key": API_KEY,
            },
          }
        ),
        axios.post(
          TRANSLATE_BASE,
          {
            provider: "BHASHINI",
            source: "CONSENTPOPUP",
            language: {
              sourceLanguage: sourceLanguage,
              targetLanguage: langObj.apiValue,
            },
            input: processingActsList.map((t, i) => ({
              id: `processingAct_${i}`,
              source: t,
            })),
          },
          {
            headers: {
              "Content-Type": "application/json",
              tenantid: TENANT_ID,
              businessid: BUSINESS_ID,
              txn: generateTransactionId(),
              "x-session-token": SESSION_TOKEN,
              "X-API-Key": API_KEY,
            },
          }
        ),
        axios.post(
          TRANSLATE_BASE,
          {
            provider: "BHASHINI",
            source: "CONSENTPOPUP",
            language: {
              sourceLanguage: sourceLanguage,
              targetLanguage: langObj.apiValue,
            },
            input: usedByList.map((t, i) => ({
              id: `usedBy_${i}`,
              source: t,
            })),
          },
          {
            headers: {
              "Content-Type": "application/json",
              tenantid: TENANT_ID,
              businessid: BUSINESS_ID,
              txn: generateTransactionId(),
              "x-session-token": SESSION_TOKEN,
              "X-API-Key": API_KEY,
            },
          }
        ),
        axios.post(
          TRANSLATE_BASE,
          {
            provider: "BHASHINI",
            source: "CONSENTPOPUP",
            language: {
              sourceLanguage: sourceLanguage,
              targetLanguage: langObj.apiValue,
            },
            input: dataItemsList.map((t, i) => ({
              id: `dataItem_${i}`,
              source: t,
            })),
          },
          {
            headers: {
              "Content-Type": "application/json",
              tenantid: TENANT_ID,
              businessid: BUSINESS_ID,
              txn: generateTransactionId(),
              "x-session-token": SESSION_TOKEN,
              "X-API-Key": API_KEY,
            },
          }
        ),
      ]);

      // 4️⃣ Parse outputs
      const translatedPurposeNamesArr = purposeNamesRes.data.output.map(
        (o) => o.target
      );
      const translatedProcessingActsArr = processingActsRes.data.output.map(
        (o) => o.target
      );
      const translatedUsedByArr = usedByRes.data.output.map((o) => o.target);
      const translatedDataItemsArr = dataItemsRes.data.output.map(
        (o) => o.target
      );

      // 5️⃣ Rebuild purposes - preserve all other fields from source
      const translatedPurposes = sourcePurposes.map((p, i) => ({
        ...p,
        purposeNames: translatedPurposeNamesArr[i] ? translatedPurposeNamesArr[i].split(", ") : p.purposeNames,
        processingAct: translatedProcessingActsArr[i] ? translatedProcessingActsArr[i].split(", ") : p.processingAct,
        usedBy: translatedUsedByArr[i] ? translatedUsedByArr[i].split(", ") : p.usedBy,
        dataItems: translatedDataItemsArr[i] ? translatedDataItemsArr[i].split(", ") : p.dataItems,
      }));

      console.log("Translated Purposes:", translatedPurposes);

      // 6️⃣ Send to parent
      if (onPurposesChange) {
        onPurposesChange(translatedPurposes);
      }
      
      setCurrentVal(langObj.apiValue);
    } catch (err) {
      console.error(`Translation failed for ${langObj.label}:`, err);
      // On error, restore previous language
      e.target.value = language || "";
    } finally {
      setIsTranslating(false);
      // Notify parent component that translation is complete
      if (onTranslationStateChange) {
        onTranslationStateChange(false);
      }
    }
  };

  // Trigger full translation for first auto-selected language
  useEffect(() => {
    if (!pendingFullTranslation || !multilingualConfigSaved || isTranslating) return;
    
    const triggerFullTranslation = async () => {
      console.log("Triggering full translation for auto-selected language:", pendingFullTranslation);
      
      // Create synthetic event for handleLanguageSelect
      const syntheticEvent = { 
        target: { value: pendingFullTranslation }
      };
      
      // Clear pending to avoid infinite loop
      setPendingFullTranslation(null);
      
      // Trigger full translation
      await handleLanguageSelect(syntheticEvent);
    };
    
    // Small delay to ensure state is stable
    const timer = setTimeout(triggerFullTranslation, 200);
    return () => clearTimeout(timer);
  }, [pendingFullTranslation, multilingualConfigSaved, isTranslating]);

  const handleLanguageChange = (selected) => {
    // selected = array of { value, label, apiValue } OR null
    const langs = selected ? selected.map((s) => s.value) : [];

    // find which ones are new compared to existing state
    const newLangs = langs.filter((lang) => !supportedLanguages.includes(lang));

    // update supported languages
    setSupportedLanguages(langs);

    // trigger translation for each new language
    newLangs.forEach((lang) => {
      const langObj = options.find((o) => o.value === lang);
      translateLanguage(langObj);
    });
  };

  useEffect(() => {
    const requestBody = {
      multilingual: {
        supportedLanguages,
        languageSpecificContentMap,
      },
    };
    // alert("Updated Request Body:" + JSON.stringify(requestBody));
  }, [supportedLanguages, languageSpecificContentMap]);

  // const requestBody = {
  //   multilingual: {
  //     supportedLanguages,
  //     languageSpecificContentMap,
  //   },
  // };
  // console.log("Request Body:", requestBody);

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
        {/* <Select
          isMulti
          options={options}
          value={options.filter((opt) =>
            supportedLanguages.includes(opt.value)
          )}
          
          onChange={handleLanguageChange}
          styles={customStyles}
        /> */}

        <Select
          isMulti
          options={options}
          value={options.filter((opt) =>
            supportedLanguages.includes(opt.value)
          )}
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
        <label>Preferred language (Required)</label>
        <select
          value={language}
          id="preferred-language"
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
      <div className="dropdown-group" style={{ padding: "0px 15px" }}>
        <label>Description</label>

        <textarea
          placeholder="Enter description of the group's function or scope."
          value={text}
          onChange={handleTextAreaChange}
          rows="3"
          className="custom-text-area"
        />
        <div className="char-counter">{text.length}/200</div>
      </div>

      <div style={{ marginTop: "10px", padding: "0px 15px" }}>
        <InputFieldV2
          label="Label for mandatory consent types (Required)"
          name="Name"
          size="small"
          type="text"
          value={label}
          onChange={handleLabelChange}
        />
      </div>
      <div
        className="dropdown-group"
        style={{ padding: "0px 15px", marginTop: "16px" }}
      >
        <label>Data principle rights text</label>

        <textarea
          placeholder="Enter description of the group's function or scope."
          value={rights}
          onChange={handleRightsChange}
          rows="3"
          className="custom-text-area"
        />
        <div className="char-counter">{text.length}/200</div>
      </div>

      <div className="dropdown-group" style={{ padding: "0px 15px" }}>
        <label>Permission text</label>

        <textarea
          placeholder="Enter description of the group's function or scope."
          value={permission}
          onChange={handlePermissionChange}
          rows="3"
          className="custom-text-area"
        />
        <div className="char-counter">{text.length}/200</div>
      </div>

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

export default LanguageContainer;
