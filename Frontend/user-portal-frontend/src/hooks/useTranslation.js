import { useState, useEffect, useCallback } from "react";
import { useDispatch, useSelector } from "react-redux";
import { translateData, translateConfiguration } from "../store/actions/CommonAction";
import { SET_IS_TRANSLATING, SET_TRANSLATIONS } from "../store/constants/Constants";

// Language options with API values for Bhashini translation
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

/**
 * Custom hook for handling Bhashini translations
 * @param {Array} translationInputs - Array of {id, source} objects to translate
 * @returns {Object} - Translation utilities and state
 */
const useTranslation = (translationInputs = []) => {
  const dispatch = useDispatch();
  const currentLanguage = useSelector((state) => state.common?.currentLanguage) || "ENGLISH";
  const currentSourceLang = useSelector((state) => state.common?.currentSourceLang) || "en";
  const globalTranslations = useSelector((state) => state.common?.translations) || {};
  const isTranslating = useSelector((state) => state.common?.isTranslating) || false;
  
  const [localTranslations, setLocalTranslations] = useState({});
  const [previousLanguage, setPreviousLanguage] = useState("ENGLISH");
  const [sourceLangTracker, setSourceLangTracker] = useState("en");

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
    async (inputs, targetLanguageKey = currentLanguage) => {
      if (!inputs || inputs.length === 0) return null;
      
      // If switching to English, reset translations
      if (targetLanguageKey === "ENGLISH") {
        setLocalTranslations({});
        setSourceLangTracker("en");
        return null;
      }

      dispatch({ type: SET_IS_TRANSLATING, payload: true });

      try {
        const targetLang = getApiValueFromLanguage(targetLanguageKey);
        
        const response = await dispatch(
          translateData(sourceLangTracker, targetLang, inputs)
        );

        if (response?.status === 200 && response?.data?.output) {
          const translatedMap = {};
          response.data.output.forEach((item) => {
            translatedMap[item.id] = item.target;
          });

          setLocalTranslations((prev) => ({ ...prev, ...translatedMap }));
          setSourceLangTracker(targetLang);

          // Also update global Redux state
          dispatch({ type: SET_TRANSLATIONS, payload: translatedMap });

          return translatedMap;
        }

        return null;
      } catch (error) {
        console.log("Translation error:", error);
        return null;
      } finally {
        dispatch({ type: SET_IS_TRANSLATING, payload: false });
      }
    },
    [dispatch, sourceLangTracker, currentLanguage]
  );

  // Get translated text by ID with fallback to default
  const getTranslation = useCallback(
    (id, defaultText) => {
      if (!currentLanguage || currentLanguage === "ENGLISH") {
        return defaultText;
      }
      // Add safety checks for undefined objects
      const localValue = localTranslations ? localTranslations[id] : null;
      const globalValue = globalTranslations ? globalTranslations[id] : null;
      return localValue || globalValue || defaultText;
    },
    [localTranslations, globalTranslations, currentLanguage]
  );

  // Auto-translate when language changes
  useEffect(() => {
    if (currentLanguage !== previousLanguage && translationInputs.length > 0) {
      translateContent(translationInputs, currentLanguage);
      setPreviousLanguage(currentLanguage);
    }
  }, [currentLanguage, previousLanguage, translationInputs, translateContent]);

  // Reset translations
  const resetTranslations = useCallback(() => {
    setLocalTranslations({});
    setSourceLangTracker("en");
  }, []);

  return {
    currentLanguage: currentLanguage || "ENGLISH",
    isTranslating: isTranslating || false,
    translations: { ...(globalTranslations || {}), ...(localTranslations || {}) },
    translateContent,
    getTranslation,
    resetTranslations,
    initializeBhashini,
    LANGUAGE_OPTIONS,
  };
};

export default useTranslation;

