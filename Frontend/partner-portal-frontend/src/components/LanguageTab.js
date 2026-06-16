import React, { useEffect, useState } from "react";
import "../styles/languageContainer.css";
import { ActionButton, Text, InputFieldV2, Button } from "../custom-components";
import Select from "react-select";
import axios from "axios";
import { v4 as uuidv4 } from "uuid";
import { useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import config from "../utils/config";

const OPTIONS = [
  { value: "ENGLISH", label: "English (English)", apiValue: "en" },
  { value: "HINDI", label: "हिन्दी (Hindi)", apiValue: "hi" },
  { value: "BENGALI", label: "বাংলা (Bengali)", apiValue: "bn" },
  { value: "TAMIL", label: "தமிழ் (Tamil)", apiValue: "ta" },
  { value: "TELUGU", label: "తెలుగు (Telugu)", apiValue: "te" },
  { value: "KANNADA", label: "ಕನ್ನಡ (Kannada)", apiValue: "kn" },
  { value: "MALAYALAM", label: "മലയാളം (Malayalam)", apiValue: "ml" },
  { value: "GUJARATI", label: "ગુજરાતી (Gujarati)", apiValue: "gu" },
  { value: "MARATHI", label: "मराठी (Marathi)", apiValue: "mr" },
  { value: "ODIA", label: "ଓଡ଼ିଆ (Odia)", apiValue: "or" },
  { value: "PUNJABI", label: "ਪੰਜਾਬੀ (Punjabi)", apiValue: "pa" },
  { value: "ASSAMESE", label: "অসমীয়া (Assamese)", apiValue: "as" },
  { value: "NEPALI", label: "नेपाली (Nepali)", apiValue: "ne" },
  { value: "SINDHI", label: "سنڌي (Sindhi)", apiValue: "sd" },
  { value: "KASHMIRI", label: "कॉशुर / كٲشُر (Kashmiri)", apiValue: "ks" },
  { value: "DOGRI", label: "डोगरी (Dogri)", apiValue: "doi" },
  { value: "KONKANI", label: "कोंकणी (Konkani)", apiValue: "gom" },
  { value: "MAITHILI", label: "मैथिली (Maithili)", apiValue: "mai" },
  { value: "SANSKRIT", label: "संस्कृतम् (Sanskrit)", apiValue: "sa" },
  { value: "SANTALI", label: "ᱥᱟᱱᱛᱟᱲᱤ (Santali)", apiValue: "sat" },
  { value: "BODO", label: "बर' (Bodo)", apiValue: "brx" },
  { value: "MANIPURI", label: "ꯃꯅꯤꯄꯨꯔꯤ (Manipuri)", apiValue: "mni" },
];


const customSelectStyles = {
  control: (base) => ({
    ...base,
    minHeight: 40,
    borderRadius: 32,
    border: "1px solid #E5E7EB",
    boxShadow: "none",
  }),
  menu: (base) => ({ ...base, zIndex: 9999 }),
};

const DESCRIPTION_LIMIT = 500;
const BASE_URL = `${config.translator_base}/`;
const TRANSLATE_BASE = `${BASE_URL}translate`;
const TRANSLATE_CONFIG_URL = `${BASE_URL}translateConfig`;

export default function LanguageTab({
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
  scanData,
  setLabel,
  languageSpecificContentMap,
  setLanguageSpecificContentMap,
  title,
  setTitle,
  placement,
  setPlacement,
  onOpenManagePreferences,
  selectedPreviewLang,
  setSelectedPreviewLang,
  titleCookie,
  setTitleCookie,
  descCookie,
  setDescCookie,
  titlePref,
  setTitlePref,
  descPref,
  setDescPref,
  isTranslating,
  setIsTranslating,
}) {
  const TENANT_ID = useSelector((state) => state.common.tenant_id);
  const BUSINESS_ID = useSelector((state) => state.common.business_id);
  const sessionToken = useSelector((state) => state.common.session_token);
  const multilingualConfigSaved = useSelector((state) => state.common.multilingualConfigSaved);
  const navigate = useNavigate();
  const [showMultilingualWarningModal, setShowMultilingualWarningModal] = useState(false);
  const [activeSubTab, setActiveSubTab] = useState(
    sessionStorage.getItem("language_tab_sub") || "cookie"
  );
  const [initialized, setInitialized] = useState(false);

  // ───────────────────────────────
  // 🧩 Setup + Restore Session
  // ───────────────────────────────
  useEffect(() => {
    const savedTab = sessionStorage.getItem("language_tab_sub") || "cookie";
    setActiveSubTab(savedTab);
    onOpenManagePreferences(savedTab === "preferences");

    const savedLang = sessionStorage.getItem("language_tab_lang");
    if (savedLang) {
      setSelectedPreviewLang(savedLang);
      setLanguage(savedLang);
    }
    setInitialized(true);
  }, [onOpenManagePreferences]);

  // ───────────────────────────────
  // 🌐 Configure Translation Provider
  // ───────────────────────────────
  async function configureTranslateProvider() {
    const txn = uuidv4();
    const body = {
      scopeLevel: "BUSINESS",
      config: {
        provider: "BHASHINI",
        apiBaseUrl: "https://meity-auth.ulcacontrib.org",
        modelPipelineEndpoint: "/ulca/apis/v0/model/getModelsPipeline",
        callbackUrl:
          "https://dhruva-api.bhashini.gov.in/services/inference/pipeline",
        userId: "02c3fec39bbf46cda8ece45ba52e78cb",
        apiKey: "058a0399da-ea84-4077-9e3e-984ff46f8b77",
        pipelineId: "64392f96daac500b55c543cd",
      },
    };
    try {
      await axios.post(TRANSLATE_CONFIG_URL, body, {
        headers: {
          "Content-Type": "application/json",
          tenantid: TENANT_ID,
          businessid: BUSINESS_ID || TENANT_ID,
          txn,
          "x-session-token":sessionToken
        },
      });
    } catch {
      console.warn("⚠️ Translator already configured");
    }
  }

  // ───────────────────────────────
  // 🔁 Translate Everything (Banner + Pref + Buttons + Categories)
  // ───────────────────────────────
 // ───────────────────────────────
// 🌐 Translate Everything (FINAL)
// ───────────────────────────────
async function translateAll(targetApiCode, baseLang = {}, scanData = {}) {
  await configureTranslateProvider();
  const txn = uuidv4();

  // 🧠 Prepare all text elements to translate
  const input = [
    { id: "title", source: baseLang.title || "" },
    { id: "body", source: baseLang.description || "" },
    { id: "managePrefTitle", source: baseLang.managePrefTitle || "" },
    { id: "managePrefDescription", source: baseLang.managePrefDescription || "" },
    { id: "managePrefbutton", source: "Manage preferences" },
    { id: "rejectAll", source: "Reject all" },
    { id: "acceptNecessary", source: "Accept necessary cookies" },
    { id: "acceptAll", source: "Accept all cookies" },
    { id: "saveButtonText", source: "Save preferences" },
    // Category labels (names)
    { id: "label_required", source: "Required Cookies" },
    { id: "label_functional", source: "Functional Cookies" },
    { id: "label_analytics", source: "Analytics Cookies" },
    { id: "label_advertising", source: "Advertising Cookies" },
    { id: "label_others", source: "Other Cookies" },
    // Category descriptions
    { id: "desc_required", source: "These cookies are necessary to enable the basic features of this site to function, such as providing secure log-in, allowing images to load, or allowing you to select your cookie preferences." },
    { id: "desc_functional", source: "These cookies allow us to analyze your use of the site to evaluate and improve its performance. They may also be used to provide a better customer experience on this site." },
    { id: "desc_analytics", source: "These cookies help us measure site usage and improve our services." },
    { id: "desc_advertising", source: "These cookies are used to show you ads that are more relevant to you and to understand your interests." },
    { id: "desc_others", source: "These cookies have not yet been classified into a category." }
  ];

  // 🟣 Include cookie categories and their descriptions from scan data
  if (scanData?.subdomains) {
    scanData.subdomains.forEach((sub) =>
      sub.cookies?.forEach((c) => {
        input.push({
          id: c.name,
          source: c.description_gpt || c.description || c.name,
        });
      })
    );
  }

  // ❌ No inputs? Skip API call
  if (input.length === 0) return {};

  // 🌍 Translation request payload
  const body = {
    provider: "BHASHINI",
    source: "CONSENTPOPUP",
    language: { sourceLanguage: "en", targetLanguage: targetApiCode },
    input,
  };

  // 🚀 API call
  const resp = await axios.post(TRANSLATE_BASE, body, {
    headers: {
      "Content-Type": "application/json",
      tenantid: TENANT_ID,
      businessid: BUSINESS_ID || TENANT_ID,
      txn,
      "x-session-token":sessionToken
    },
  });

  // 🗺️ Convert response array into a dictionary
  const map = {};
  resp?.data?.output?.forEach((o) => {
    const id = o?.id;
    const text = o?.target ?? o?.translated ?? o?.targetText ?? "";
    if (id && text) map[id] = text;
  });

  console.log("🌐 Translation map ready:", map);
  return map;
}


  // ───────────────────────────────
  // ⚙️ Auto Translate Handler
  // ───────────────────────────────
 // ───────────────────────────────
// ⚙️ Auto Translate Handler (FINAL)
// ───────────────────────────────
async function handleAutoTranslate(langOpt) {
  try {
    setIsTranslating(true);

    // 🌍 Base language source (English or whatever user edited)
    const baseLang = {
      title: titleCookie || title,
      description: descCookie || text,
      managePrefTitle: titlePref,
      managePrefDescription: descPref,
    };

    // 🔄 Translate everything: banner, preferences, buttons, cookies
    const map = await translateAll(langOpt.apiValue, baseLang, scanData);

    // 🧩 Build full translated entry (aligned with Cookie.js keys)
    const newEntry = {
      cookieTitle: map.title || baseLang.title,
      cookieDescription: map.body || baseLang.description,
      managePrefTitle: map.managePrefTitle || baseLang.managePrefTitle,
      managePrefDescription: map.managePrefDescription || baseLang.managePrefDescription,
      managePrefs: map.managePrefbutton || "Manage preferences",
      rejectAll: map.rejectAll || "Reject all",
      acceptNecessary: map.acceptNecessary || "Accept necessary cookies",
      acceptAll: map.acceptAll || "Accept all cookies",
      saveButtonText: map.saveButtonText || "Save preferences",
      // ✅ Category labels (translated names)
      categoryLabels: {
        required: map.label_required || "Required Cookies",
        functional: map.label_functional || "Functional Cookies",
        analytics: map.label_analytics || "Analytics Cookies",
        advertising: map.label_advertising || "Advertising Cookies",
        others: map.label_others || "Other Cookies",
      },
      // ✅ Category descriptions
      categoryDescriptions: {
        required: map.desc_required,
        functional: map.desc_functional,
        analytics: map.desc_analytics,
        advertising: map.desc_advertising,
        others: map.desc_others,
      },
      scanTranslations: Object.keys(map)
        .filter(
          (k) =>
            ![
              "title",
              "body",
              "managePrefTitle",
              "managePrefDescription",
              "managePrefbutton",
              "rejectAll",
              "acceptNecessary",
              "acceptAll",
              "saveButtonText",
              "label_required",
              "label_functional",
              "label_analytics",
              "label_advertising",
              "label_others",
              "desc_required",
              "desc_functional",
              "desc_analytics",
              "desc_advertising",
              "desc_others",
            ].includes(k)
        )
        .reduce((acc, k) => {
          acc[k] = { name: k, description: map[k] };
          return acc;
        }, {}),
    };


    // 🧠 Store translation results into language map
    setLanguageSpecificContentMap((prev) => ({
      ...prev,
      [langOpt.value]: newEntry,
    }));

    // 🪄 Update preview UI instantly if the same language is open
    if (selectedPreviewLang === langOpt.value) {
      setTitleCookie(newEntry.cookieTitle);
      setDescCookie(newEntry.cookieDescription);
      setTitlePref(newEntry.managePrefTitle);
      setDescPref(newEntry.managePrefDescription);
      // Note: managePrefs button label is stored in languageSpecificContentMap
    }

    console.log(`✅ Translated + synced: ${langOpt.value}`);
  } catch (err) {
    console.error("❌ Auto-translation failed:", err);
  } finally {
    setIsTranslating(false);
  }
}

// 🧩 Rehydrate when parent passes updated data (Edit mode)
useEffect(() => {
  if (!languageSpecificContentMap || Object.keys(languageSpecificContentMap).length === 0) return;

  setSupportedLanguages(Object.keys(languageSpecificContentMap));

  const currentLang = selectedPreviewLang || "ENGLISH";
  const langContent = languageSpecificContentMap[currentLang];
  if (!langContent) return;

  // 🧠 Handle both formats: pre-publish (cookieTitle, managePrefTitle)
  // and post-publish (title, rightsText)
  setTitleCookie(langContent.cookieTitle || langContent.title || "");
  setDescCookie(langContent.cookieDescription || langContent.description || "");
  setTitlePref(langContent.managePrefTitle || langContent.rightsText || "");
  setDescPref(langContent.managePrefDescription || langContent.permissionText || "");
}, [selectedPreviewLang, languageSpecificContentMap]);



  // ───────────────────────────────
  // 🧩 Language Multi-Select
  // ───────────────────────────────
  // Default content when no language is selected (only for Language tab display)
  const DEFAULT_TITLE = "This site uses cookies to make your experience better";
  const DEFAULT_DESC = "We use essential cookies to make our site work. With your consent, we may also use non-essential cookies to improve user experience and analyze website traffic. By clicking 'Accept', you agree to our website's cookie use as described in our cookie policy. You can change your cookie settings at any time by clicking 'Preferences'";
  const DEFAULT_PREF_TITLE = "Manage preferences";
  const DEFAULT_PREF_DESC = "Choose the cookies you allow. You can update your preferences anytime.";

  const handleMultiSelectChange = async (selected) => {
    let list = selected ? selected.map((s) => s.value) : [];
    const prevList = supportedLanguages || [];
    
    setSupportedLanguages(list);

    // ✅ If no languages selected, show default content in Language tab only
    if (list.length === 0) {
      setTitleCookie(DEFAULT_TITLE);
      setDescCookie(DEFAULT_DESC);
      setTitlePref(DEFAULT_PREF_TITLE);
      setDescPref(DEFAULT_PREF_DESC);
      setSelectedPreviewLang("");
      setLanguage("");
      sessionStorage.removeItem("language_tab_lang");
      setLanguageSpecificContentMap({});
      return;
    }

    // ✅ Clean up and maintain only selected languages
    setLanguageSpecificContentMap((prev) => {
      const updated = Object.keys(prev || {})
        .filter((lang) => list.includes(lang))
        .reduce((acc, lang) => {
          acc[lang] = prev[lang];
          return acc;
        }, {});

      // ✅ Add missing languages (if new)
      list.forEach((lang) => {
        if (!updated[lang]) {
          updated[lang] = {
            cookieTitle: "",
            cookieDescription: "",
            managePrefTitle: "",
            managePrefDescription: "",
          };
        }
      });

      return updated;
    });

    // ✅ Determine which language to show in the "Customise Content" dropdown
    let newActiveLang;
    
    // Find if a language was JUST added (it would be in list but not in prevList)
    const added = list.filter(x => !prevList.includes(x));
    
    if (added.length === 1) {
      // If exactly one language was added, switch to it
      newActiveLang = added[0];
    } else if (list.length === 1) {
      // If only one language remains, switch to it
      newActiveLang = list[0];
    } else {
      // Otherwise, keep current if it's still valid, else use the first in list
      newActiveLang = list.includes(selectedPreviewLang) ? selectedPreviewLang : list[0];
    }

    setSelectedPreviewLang(newActiveLang);
    setLanguage(newActiveLang);
    sessionStorage.setItem("language_tab_lang", newActiveLang);

    // ✅ AUTO-TRANSLATE: If newly selected language is not ENGLISH and has no content
    if (newActiveLang && newActiveLang !== "ENGLISH") {
      const langOpt = OPTIONS.find((o) => o.value === newActiveLang);
      if (langOpt?.apiValue) {
        const existingContent = languageSpecificContentMap?.[newActiveLang];
        const hasContent = existingContent?.cookieTitle || existingContent?.cookieDescription || existingContent?.title;
        
        if (!hasContent) {
          await handleAutoTranslate(langOpt);
        } else {
          setTitleCookie(existingContent.cookieTitle || existingContent.title || "");
          setDescCookie(existingContent.cookieDescription || existingContent.description || "");
          setTitlePref(existingContent.managePrefTitle || existingContent.rightsText || "");
          setDescPref(existingContent.managePrefDescription || existingContent.permissionText || "");
        }
      }
    }
  };



  // ───────────────────────────────
  // 🔁 Change Preview Language
  // ───────────────────────────────
  const handlePreviewLangChange = async (e) => {
    const val = e.target.value;
    if (!val) return;
    setSelectedPreviewLang(val);
    setLanguage(val);
    sessionStorage.setItem("language_tab_lang", val);
    
    // ✅ Check if content already exists for this language
    const existingContent = languageSpecificContentMap?.[val];
    const hasCookieContent = existingContent?.cookieTitle || existingContent?.cookieDescription || existingContent?.title;
    const hasPrefsContent = existingContent?.managePrefTitle || existingContent?.rightsText;
    const hasCategoryDescs = existingContent?.categoryDescriptions && Object.keys(existingContent.categoryDescriptions).length > 0;
    const hasCategoryLabels = existingContent?.categoryLabels && Object.keys(existingContent.categoryLabels).length > 0;
    
    // ✅ Update preview fields with existing content first
    if (hasCookieContent || hasPrefsContent) {
      setTitleCookie(existingContent.cookieTitle || existingContent.title || "");
      setDescCookie(existingContent.cookieDescription || existingContent.description || "");
      setTitlePref(existingContent.managePrefTitle || existingContent.rightsText || "");
      setDescPref(existingContent.managePrefDescription || existingContent.permissionText || "");
    }
    
    // ✅ Check if translations are INCOMPLETE (missing category data, cookie table, etc.)
    // If so, translate to fill in the gaps
    const hasIncompleteTranslations = val !== "ENGLISH" && (!hasCategoryDescs || !hasCategoryLabels || !hasPrefsContent);
    
    if (hasIncompleteTranslations) {
      const langOpt = OPTIONS.find((o) => o.value === val);
      if (langOpt?.apiValue) {
        await handleAutoTranslate(langOpt);
      }
      return;
    }
    
    // ✅ No content at all - translate if not English
    if (!hasCookieContent && !hasPrefsContent && val !== "ENGLISH") {
      const langOpt = OPTIONS.find((o) => o.value === val);
      if (langOpt?.apiValue) {
        await handleAutoTranslate(langOpt);
      }
    }
  };

  const selectValue = OPTIONS.filter(
    (o) => supportedLanguages && supportedLanguages.includes(o.value)
  );

  if (!initialized) {
    return (
      <div style={{ padding: 20, textAlign: "center", color: "#6B7280" }}>
        Loading language data...
      </div>
    );
  }

  // 🔵 Full-screen loader
const LoaderOverlay = () => (
  <div
    style={{
      position: "fixed",
      top: 0,
      left: 0,
      width: "100vw",
      height: "100vh",
      background: "rgba(255,255,255,0.6)",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      zIndex: 999999,
      backdropFilter: "blur(2px)",
    }}
  >
    <div
      style={{
        width: 50,
        height: 50,
        border: "5px solid #d1d5db",
        borderTopColor: "#0052CC",
        borderRadius: "50%",
        animation: "spin 0.8s linear infinite",
      }}
    ></div>

    <style>
      {`
        @keyframes spin {
          from { transform: rotate(0deg); }
          to   { transform: rotate(360deg); }
        }
      `}
    </style>
  </div>
);

  // ───────────────────────────────
  // 🖼️ RENDER
  // ───────────────────────────────
  return (
    <>
    {isTranslating && <LoaderOverlay />}
    <div
      className="language-con"
      style={{
        padding: 20,
        borderRadius: 12,
        background: "#fff",
        border: "1px solid #E5E7EB",
      }}
    >
      {/* Tabs */}
      <div style={{ display: "flex", gap: 32, marginBottom: 35 }}>
        {[
          { key: "cookie", label: "Cookie" },
          { key: "preferences", label: "Manage preferences" },
        ].map((tab) => (
          <button
            key={tab.key}
            onClick={async () => {
              setActiveSubTab(tab.key);
              sessionStorage.setItem("language_tab_sub", tab.key);
              onOpenManagePreferences(tab.key === "preferences");
              
              // ✅ Auto-translate when switching to Manage Preferences tab
              // if selected language is not English and translations are incomplete
              if (tab.key === "preferences" && selectedPreviewLang && selectedPreviewLang !== "ENGLISH") {
                const existingContent = languageSpecificContentMap?.[selectedPreviewLang];
                
                // Check if we have COMPLETE translated prefs content
                // We need: managePrefTitle/rightsText, categoryDescriptions, categoryLabels
                const hasTranslatedTitle = existingContent?.managePrefTitle || existingContent?.rightsText;
                const hasCategoryDescs = existingContent?.categoryDescriptions && 
                  Object.keys(existingContent.categoryDescriptions).length > 0;
                const hasCategoryLabels = existingContent?.categoryLabels && 
                  Object.keys(existingContent.categoryLabels).length > 0;
                
                // If missing category content, translate to fill it in
                const needsTranslation = !hasTranslatedTitle || !hasCategoryDescs || !hasCategoryLabels;
                
                if (needsTranslation) {
                  const langOpt = OPTIONS.find((o) => o.value === selectedPreviewLang);
                  if (langOpt?.apiValue) {
                    await handleAutoTranslate(langOpt);
                  }
                }
              }
            }}
            style={{
              padding: "10px 20px",
              border: "none",
              borderBottom:
                activeSubTab === tab.key
                  ? "3px solid #0052CC"
                  : "3px solid transparent",
              background: "none",
              cursor: "pointer",
              fontWeight: activeSubTab === tab.key ? 600 : 500,
            }}
          >
            <Text
              appearance="body_s"
              color={
                activeSubTab === tab.key
                  ? "primary-brand"
                  : "primary-grey-100"
              }
            >
              {tab.label}
            </Text>
          </button>
        ))}
      </div>
{/* ─────────────── Placement (Cookie Only) ─────────────── */}

    {activeSubTab === "cookie" && (

      <div style={{ marginBottom: 20 }}>

        <Text appearance="heading-xxs" color="primary-grey-80">

          Placement of cookie

        </Text>

        <div

          style={{

            display: "flex",

            gap: 16,

            marginTop: 8,

            alignItems: "center",

          }}

        >

          {[

            { label: "Banner", value: "banner" },

            { label: "Modal", value: "modal" },

            { label: "Tool-tip", value: "tooltip" },

          ].map((opt) => (

            <label

              key={opt.value}

              style={{

                display: "flex",

                alignItems: "center",

                gap: 8,

                cursor: "pointer",

              }}

            >

              <input

                type="radio"

                name="placement"

                checked={placement === opt.value}

                onChange={() => {

                  setPlacement(opt.value);

                  setLanguageSpecificContentMap((prev) => ({

                    ...prev,

                    [selectedPreviewLang]: {

                      ...(prev[selectedPreviewLang] || {}),

                      placement: opt.value,

                    },

                  }));

                }}

                style={{ cursor: "pointer" }}

              />

              <Text appearance="body-s" color="primary-grey-80">

                {opt.label}

              </Text>

            </label>

          ))}

        </div>

      </div>

    )}
      {/* Multi-lingual Support */}
      <Text appearance="heading-xxs" color="primary-grey-80">
        Multi-lingual support
      </Text>
      <Select
        isMulti
        options={OPTIONS}
        value={selectValue}
        onChange={handleMultiSelectChange}
        styles={customSelectStyles}
        placeholder={<Text appearance="body-xs">Select languages...</Text>}
      />

      {/* Customise Content */}
      <div style={{ marginTop: 20 }}>
        <Text appearance="heading-xxs">Customise content</Text>
        <select
  value={selectedPreviewLang || ""}
  onChange={handlePreviewLangChange}
  disabled={isTranslating}
   style={{
            marginTop: 6,
            width: "100%",
            border: "1px solid #E5E7EB",
            borderRadius: 8,
            padding: 8,
          }}
>
  <option value="" disabled hidden>
    Select language
  </option>
  {supportedLanguages.map((lang) => {
    const opt = OPTIONS.find((o) => o.value === lang);
    if (!opt) return null;
    return (
      <option key={lang} value={opt.value}>
        {opt.label}
      </option>
    );
  })}
</select>

      </div>

      {/* Cookie Banner Inputs */}
      {activeSubTab === "cookie" && (
        <>
          <div style={{ marginTop: 16 }}>
            <Text appearance="body-s">Title for cookie banner</Text>
            <input
              type="text"
              value={titleCookie}
              onChange={(e) => setTitleCookie(e.target.value)}
              style={{
                width: "100%",
                border: "1px solid #E5E7EB",
                borderRadius: 8,
                padding: 8,
              }}
            />
          </div>
          <div style={{ marginTop: 16 }}>
            <Text appearance="body-s">Description text</Text>
            <textarea
              value={descCookie}
              onChange={(e) => setDescCookie(e.target.value)}
              rows={6}
              style={{
                width: "100%",
                border: "1px solid #E5E7EB",
                borderRadius: 8,
                padding: 12,
              }}
            />
          </div>
        </>
      )}

      {/* Manage Preferences Inputs */}
      {activeSubTab === "preferences" && (
        <>
          <div style={{ marginTop: 16 }}>
            <Text appearance="body-s">Manage Preference Title</Text>
            <input
              type="text"
              value={titlePref}
              onChange={(e) => setTitlePref(e.target.value)}
              style={{
                width: "100%",
                border: "1px solid #E5E7EB",
                borderRadius: 8,
                padding: 8,
              }}
            />
          </div>
          <div style={{ marginTop: 16 }}>
            <Text appearance="body-s">Description text</Text>
            <textarea
              value={descPref}
              onChange={(e) => setDescPref(e.target.value)}
              rows={4}
              style={{
                width: "100%",
                border: "1px solid #E5E7EB",
                borderRadius: 8,
                padding: 12,
              }}
            />
          </div>

          <div style={{ textAlign: "right", marginTop: 20 }}>
            
          </div>
        </>
      )}

      {/* Save Button */}
      <div style={{ marginTop: 24 }}>
        <ActionButton
          kind="primary"
          size="small"
          label={isTranslating ? "Translating..." : "Save"}
          state={isTranslating ? "disabled" : "normal"}
          onClick={() => {
            console.log("💾 Saved", {
              titleCookie,
              descCookie,
              titlePref,
              descPref,
            });
          }}
        />
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
  
      
  </>
);

}
