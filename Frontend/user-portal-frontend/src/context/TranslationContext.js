import React, { createContext, useContext, useState, useCallback } from "react";
import { useDispatch, useSelector } from "react-redux";
import { translateData, translateConfiguration } from "../store/actions/CommonAction";

// Language options with API values
export const LANGUAGE_OPTIONS = [
  { value: "ENGLISH", label: "English", apiValue: "en" },
  { value: "HINDI", label: "हिंदी", apiValue: "hi" },
  { value: "BENGALI", label: "বাংলা", apiValue: "bn" },
  { value: "TAMIL", label: "தமிழ்", apiValue: "ta" },
  { value: "TELUGU", label: "తెలుగు", apiValue: "te" },
  { value: "KANNADA", label: "ಕನ್ನಡ", apiValue: "kn" },
  { value: "MALAYALAM", label: "മലയാളം", apiValue: "ml" },
  { value: "GUJARATI", label: "ગુજરાતી", apiValue: "gu" },
  { value: "MARATHI", label: "मराठी", apiValue: "mr" },
  { value: "ODIA", label: "ଓଡ଼ିଆ", apiValue: "or" },
  { value: "PUNJABI", label: "ਪੰਜਾਬੀ", apiValue: "pa" },
  { value: "ASSAMESE", label: "অসমীয়া", apiValue: "as" },
  { value: "NEPALI", label: "नेपाली", apiValue: "ne" },
  { value: "SINDHI", label: "سنڌي", apiValue: "sd" },
  { value: "KASHMIRI", label: "कॉशुर", apiValue: "ks" },
  { value: "DOGRI", label: "डोगरी", apiValue: "doi" },
  { value: "KONKANI", label: "कोंकणी", apiValue: "gom" },
  { value: "MAITHILI", label: "मैथिली", apiValue: "mai" },
  { value: "SANSKRIT", label: "संस्कृत", apiValue: "sa" },
  { value: "SANTALI", label: "ᱥᱟᱱᱛᱟᱲᱤ", apiValue: "sat" },
  { value: "BODO", label: "बड़ो", apiValue: "brx" },
  { value: "MANIPURI", label: "মৈতৈলোন্", apiValue: "mni" },
];

// Get API value from language key
export const getApiValueFromLanguage = (languageKey) => {
  const option = LANGUAGE_OPTIONS.find((opt) => opt.value === languageKey);
  return option ? option.apiValue : "en";
};

// Get language option from API value
export const getLanguageFromApiValue = (apiValue) => {
  const option = LANGUAGE_OPTIONS.find((opt) => opt.apiValue === apiValue);
  return option ? option.value : "ENGLISH";
};

// Create context
const TranslationContext = createContext();

// Translation Provider Component
export const TranslationProvider = ({ children }) => {
  const dispatch = useDispatch();
  const [currentLanguage, setCurrentLanguage] = useState("ENGLISH");
  const [currentSourceLang, setCurrentSourceLang] = useState("en");
  const [translations, setTranslations] = useState({});
  const [isTranslating, setIsTranslating] = useState(false);

  // Initialize Bhashini configuration
  const initializeBhashini = useCallback(async () => {
    try {
      await dispatch(translateConfiguration());
    } catch (error) {
      console.log("Error initializing Bhashini config:", error);
    }
  }, [dispatch]);

  // Translate content using Bhashini API
  const translateContent = useCallback(
    async (inputs, targetLanguageKey) => {
      if (targetLanguageKey === "ENGLISH") {
        // Reset to original English content
        setCurrentLanguage("ENGLISH");
        setCurrentSourceLang("en");
        return null;
      }

      setIsTranslating(true);
      try {
        const targetLang = getApiValueFromLanguage(targetLanguageKey);
        
        const response = await dispatch(
          translateData(currentSourceLang, targetLang, inputs)
        );

        if (response?.status === 200 && response?.data?.output) {
          const translatedMap = {};
          response.data.output.forEach((item) => {
            translatedMap[item.id] = item.target;
          });

          setTranslations((prev) => ({ ...prev, ...translatedMap }));
          setCurrentLanguage(targetLanguageKey);
          setCurrentSourceLang(targetLang);

          return translatedMap;
        }

        return null;
      } catch (error) {
        console.log("Translation error:", error);
        return null;
      } finally {
        setIsTranslating(false);
      }
    },
    [dispatch, currentSourceLang]
  );

  // Get translated text by ID
  const getTranslation = useCallback(
    (id, defaultText) => {
      if (currentLanguage === "ENGLISH") {
        return defaultText;
      }
      return translations[id] || defaultText;
    },
    [translations, currentLanguage]
  );

  // Reset translations
  const resetTranslations = useCallback(() => {
    setTranslations({});
    setCurrentLanguage("ENGLISH");
    setCurrentSourceLang("en");
  }, []);

  const value = {
    currentLanguage,
    setCurrentLanguage,
    currentSourceLang,
    translations,
    isTranslating,
    translateContent,
    getTranslation,
    resetTranslations,
    initializeBhashini,
    LANGUAGE_OPTIONS,
  };

  return (
    <TranslationContext.Provider value={value}>
      {children}
    </TranslationContext.Provider>
  );
};

// Custom hook to use translation context
export const useTranslation = () => {
  const context = useContext(TranslationContext);
  if (!context) {
    throw new Error("useTranslation must be used within a TranslationProvider");
  }
  return context;
};

export default TranslationContext;

