// CookieIntegration.js — Final integrated version (updated translate & language calls)
// Purpose: cookie consent banner + manage preferences UI + helpers
// Usage: import { loadCookieCenter } from "./CookieIntegration";
// Call: await loadCookieCenter({ tenantId, businessId, cookieTemplateId, consentHandleId, secure_code, deviceUUID, token })

const BASE_URL = `${process.env.REACT_APP_COOKIE_URL}`;
const TEMPLATE_VERSION = 1;

/* Translation config */
const TRANSLATE_BASE = `${process.env.REACT_APP_TRANSLATOR_URL}/translate`;
const TRANSLATE_CONFIG_URL = `${process.env.REACT_APP_TRANSLATOR_URL}/translateConfig`;

// Track if translation is configured per business
const translationConfigured = {};

let globalFontFamily = "system-ui";
let globalFontSize = "14px";
let globalFontWeight = "400";
let globalFontStyle = "normal";

async function loadDashboardTheme(tenantId, businessId) {
  try {
    const url = `${process.env.REACT_APP_API_URL}/v1.0/user-dashboard-theme/get`;
    const response = await fetch(url, {
      method: "GET",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
        "tenant-id": tenantId,
        "business-id": businessId,
        "scope-level": "tenant",
        txn: generateUUIDv4(),
      },
    });

    if (response.ok) {
      const data = await response.json();
      if (data.typographySettings) {
        const decodedTypography = JSON.parse(atob(data.typographySettings));
        const settings = decodedTypography.ENGLISH;
        if (settings) {
          globalFontSize = settings.fontSize ? settings.fontSize + "px" : "14px";
          globalFontWeight = settings.fontWeight || "400";
          globalFontStyle = settings.fontStyle || "normal";
          
          if (settings.fontFile) {
            const styleId = "jio-cookie-custom-font";
            if (!document.getElementById(styleId)) {
              const style = document.createElement("style");
              style.id = styleId;
              
              const fontSrc = settings.fontFile.startsWith("data:")
                ? settings.fontFile
                : `data:application/octet-stream;base64,${settings.fontFile}`;

              style.textContent = `
                @font-face {
                  font-family: 'JioCookieCustomFont';
                  src: url(${fontSrc});
                }
              `;
              document.head.appendChild(style);
            }
            globalFontFamily = "'JioCookieCustomFont', system-ui, sans-serif";
          }
        }
      }
    }
  } catch (error) {
    console.error("Error loading dashboard theme:", error);
  }
}

/* ---------- Configure Translation Provider (Bhashini) ---------- */
async function ensureTranslationConfigured(tenantId, businessId, secure_code) {
  // Skip if already configured for this business
  if (translationConfigured[businessId]) {
    console.log("Translation already configured for business:", businessId);
    return true;
  }

  try {
    const configBody = {
      scopeLevel: "BUSINESS",
      config: {
        provider: "BHASHINI",
        apiBaseUrl: "https://meity-auth.ulcacontrib.org",
        modelPipelineEndpoint: "/ulca/apis/v0/model/getModelsPipeline",
        callbackUrl: "https://dhruva-api.bhashini.gov.in/services/inference/pipeline",
        userId: "02c3fec39bbf46cda8ece45ba52e78cb",
        apiKey: "058a0399da-ea84-4077-9e3e-984ff46f8b77",
        pipelineId: "64392f96daac500b55c543cd"
      }
    };

    const headers = {
      "Content-Type": "application/json",
      "Accept": "*/*",
      "txn": generateUUIDv4(),
      "tenantid": tenantId,
      "businessid": businessId,
      "X-Secure-Code": secure_code,
    };

    console.log("Configuring translation provider for business:", businessId);

    const res = await fetch(TRANSLATE_CONFIG_URL, {
      method: "POST",
      headers,
      body: JSON.stringify(configBody),
    });

    const data = await res.json().catch(() => ({}));
    console.log("Translation config response:", data);

    // Success cases:
    // 1. Response OK
    // 2. Status is SUCCESS
    // 3. Already exists (errorCd: JCMPT034) - treat as success
    if (res.ok || data.status === "SUCCESS") {
      translationConfigured[businessId] = true;
      console.log("Translation configured successfully!");
      return true;
    }

    // If already configured (JCMPT034), mark as success and proceed
    if (data.errorCd === "JCMPT034" || (data.errorMsg && data.errorMsg.includes("already exists"))) {
      translationConfigured[businessId] = true;
      console.log("Translation config already exists - proceeding with translate");
      return true;
    }

    console.warn("Translation config failed:", data);
    return false;
  } catch (err) {
    console.error("ensureTranslationConfigured error:", err);
    return false;
  }
}

/* Language options (condensed) */
const LANG_OPTIONS = [
  { value: "ENGLISH", label: "English", apiValue: "en" },
  { value: "HINDI", label: "Hindi", apiValue: "hi" },
  { value: "BENGALI", label: "Bengali", apiValue: "bn" },
  { value: "TAMIL", label: "Tamil", apiValue: "ta" },
  { value: "TELUGU", label: "Telugu", apiValue: "te" },
  { value: "KANNADA", label: "Kannada", apiValue: "kn" },
  { value: "MALAYALAM", label: "Malayalam", apiValue: "ml" },
  { value: "GUJARATI", label: "Gujarati", apiValue: "gu" },
  { value: "MARATHI", label: "Marathi", apiValue: "mr" },
  { value: "PUNJABI", label: "Punjabi", apiValue: "pa" },
  { value: "ODIA", label: "Odia", apiValue: "or" },
  { value: "ASSAMESE", label: "Assamese", apiValue: "as" },
  { value: "URDU", label: "Urdu", apiValue: "ur" },
  { value: "NEPALI", label: "Nepali", apiValue: "ne" },
  { value: "SANSKRIT", label: "Sanskrit", apiValue: "sa" },
  { value: "SINDHI", label: "Sindhi", apiValue: "sd" },
  { value: "KASHMIRI", label: "Kashmiri", apiValue: "ks" },
  { value: "KONKANI", label: "Konkani", apiValue: "kok" },
  { value: "MAITHILI", label: "Maithili", apiValue: "mai" },
  { value: "BHOJPURI", label: "Bhojpuri", apiValue: "bho" },
  { value: "DOGRI", label: "Dogri", apiValue: "doi" },
  { value: "SANTALI", label: "Santali", apiValue: "sat" },
  { value: "BODO", label: "Bodo", apiValue: "brx" },
];

/* ---------- small DOM helper ---------- */
function el(tag, cls = "", style = {}, html = "") {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  Object.assign(e.style, style);
  if (html) e.innerHTML = html;
  return e;
}

/* ---------- Theme decoder (Base64 → JSON) ---------- */
function decodeTheme(base64Theme) {
  if (!base64Theme) return null;
  try {
    const decodedStr = atob(base64Theme);
    console.log("Decoded Base64 string:", decodedStr);
    const themeObj = JSON.parse(decodedStr);
    console.log("Parsed theme object:", themeObj);
    
    // Handle both formats:
    // Format 1: { light: "{...}", dark: "{...}" } - light/dark are stringified JSON
    // Format 2: { light: {...}, dark: {...} } - light/dark are already objects
    let lightTheme = themeObj.light;
    let darkTheme = themeObj.dark;
    
    // If light is a string, parse it
    if (typeof lightTheme === "string") {
      lightTheme = JSON.parse(lightTheme);
    }
    // If dark is a string, parse it
    if (typeof darkTheme === "string") {
      darkTheme = JSON.parse(darkTheme);
    }
    
    return {
      light: lightTheme || null,
      dark: darkTheme || null,
    };
  } catch (err) {
    console.warn("Failed to decode theme:", err);
    return null;
  }
}

/* ---------- Default theme fallback ---------- */
const DEFAULT_THEME = {
  cardBackground: "#F9FAFB",
  cardFont: "#111827",
  buttonBackground: "#0A2885",
  buttonFont: "#FFFFFF",
  linkFont: "#0F3CC9",
  secondaryButtonBackground: "#FFFFFF",
  secondaryButtonFont: "#0F3CC9",
  descriptionFont: "#6B7280",
  borderColor: "#E5E7EB",
};

/* ---------- UUIDv4 generator (fallback included) ---------- */
function generateUUIDv4() {
  if (typeof crypto !== "undefined" && crypto.getRandomValues) {
    const buf = new Uint8Array(16);
    crypto.getRandomValues(buf);
    buf[6] = (buf[6] & 0x0f) | 0x40;
    buf[8] = (buf[8] & 0x3f) | 0x80;
    const hex = Array.from(buf, (b) => b.toString(16).padStart(2, "0")).join("");
    return `${hex.slice(0,8)}-${hex.slice(8,12)}-${hex.slice(12,16)}-${hex.slice(16,20)}-${hex.slice(20)}`;
  }
  let d = Date.now();
  if (typeof performance !== "undefined" && performance.now) d += performance.now();
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
    const r = (d + Math.random() * 16) | 0;
    d = Math.floor(d / 16);
    return (c === "x" ? r : (r & 0x3) | 0x8).toString(16);
  });
}

/* ---------- Vendor prettifier & host extractor ---------- */
  const VENDOR_PRETTY = {
    ".facebook.com": "Meta Platforms, Inc",
  "facebook.com": "Meta Platforms, Inc",
    ".google.com": "Google",
  ".googletagmanager.com": "Google Tag Manager",
    ".doubleclick.net": "Google Ads",
    ".linkedin.com": "LinkedIn",
    ".youtube.com": "YouTube",
  ".instagram.com": "Instagram (Meta Platforms, Inc)",
  ".twitter.com": "Twitter, Inc",
  ".amazon-adsystem.com": "Amazon Ads",
  };
function prettifyVendor(domain) {
  if (!domain) return "Unknown";
  const lower = domain.toLowerCase();
  for (const k of Object.keys(VENDOR_PRETTY)) {
    if (lower.endsWith(k) || lower === k.replace(/^\./, "")) return VENDOR_PRETTY[k];
  }
  return domain.replace(/^\./, "");
}
function extractHost(cookie) {
  if (!cookie) return "Unknown";
  if (cookie.domain) return cookie.domain;
  if (cookie.url) {
    try { return new URL(cookie.url).hostname; } catch {}
  }
  return cookie.provider || cookie.subdomainName || "Unknown";
}

/* ---------- safeFetchJson: fetch with timeout + JSON parse. returns null on error ---------- */
async function safeFetchJson(url, opts = {}, timeoutMs = 15000) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const res = await fetch(url, { ...opts, signal: controller.signal });
    clearTimeout(timer);
    const txt = await res.text().catch(() => "");
    const data = txt ? JSON.parse(txt) : {};
    if (!res.ok) {
      const msg = data?.message || `HTTP ${res.status}`;
      console.warn("safeFetchJson non-ok", url, msg);
      throw new Error(msg);
    }
    return data;
  } catch (err) {
    clearTimeout(timer);
    console.warn("safeFetchJson error", url, err && err.message ? err.message : err);
    return null;
  }
}

/* ---------- simple globe SVG generator ---------- */
function createLanguageIconSVG(size = 16) {
  const ns = "http://www.w3.org/2000/svg";
  const svg = document.createElementNS(ns, "svg");
  svg.setAttribute("width", size);
  svg.setAttribute("height", size);
  svg.setAttribute("viewBox", "0 0 24 24");
  svg.setAttribute("fill", "none");
  svg.innerHTML =
    '<path d="M12 2a10 10 0 100 20 10 10 0 000-20zM4 12h16M12 4c1.3 2.3 2 5 2 8s-.7 5.7-2 8M4 12c1.5-2.6 4-4 8-4s6.5 1.4 8 4" stroke="#141414" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"></path>';
  return svg;
}

/* ---------- Translation call wrapper (compact) ----------
   UPDATED: accepts secureCode and includes X-Secure-Code header when provided.
   Signature: translateForTarget(targetApiCode, payload = {}, tenantForHeader = null, secureCode = null)
*/
/* ---------- translateForTarget ---------- */
// FIX: Define endpoint properly
const TRANSLATE_ENDPOINT = TRANSLATE_BASE;

/* ---------- translateForTarget ---------- */
async function translateForTarget(apiCode, payload, tenantId, businessId, secure_code) {
  try {
    // Ensure translation provider is configured first
    const configured = await ensureTranslationConfigured(tenantId, businessId, secure_code);
    if (!configured) {
      console.warn("Translation provider not configured, skipping translation");
      return null;
    }

    // Filter out entries with empty/undefined source values and convert arrays to strings
    const inputArr = Object.entries(payload)
      .filter(([id, source]) => {
        // Handle arrays (categories, categoryDescriptions)
        if (Array.isArray(source)) {
          return source.length > 0 && source.some(s => s && s.trim && s.trim() !== "");
        }
        // Handle strings
        return source && source.toString().trim() !== "";
      })
      .map(([id, source]) => ({
        id,
        // Convert arrays to comma-separated strings (API expects string, not array)
        source: Array.isArray(source) ? source.filter(Boolean).join(", ") : source,
      }));

    if (inputArr.length === 0) {
      console.warn("translateForTarget: No valid input to translate");
      return null;
    }

    const requestBody = {
      configId: "null",
      input: inputArr,
      language: { sourceLanguage: "en", targetLanguage: apiCode },
      provider: "BHASHINI",
      source: "CONSENTPOPUP",
    };

    console.log("Translation request body:", JSON.stringify(requestBody));
    console.log("Translation headers:", { tenantid: tenantId, businessid: businessId });

    const headers = {
      "Content-Type": "application/json",
      "Accept": "*/*",
      "txn": generateUUIDv4(),
      "tenantid": tenantId,
      "businessid": businessId,
      "X-Secure-Code": secure_code,
    };

    const res = await fetch(TRANSLATE_ENDPOINT, {
      method: "POST",
      headers,
      body: JSON.stringify(requestBody),
    });

    if (!res.ok) {
      const errText = await res.text().catch(() => "");
      console.error("translateForTarget status:", res.status, errText);
      return null;
    }

    const data = await res.json();
    console.log("Translation response:", data);
    const out = {};

    (data?.output || []).forEach((o) => {
      out[o.id] = o.target || o.targetText || "";
    });

    return out;
  } catch (err) {
    console.error("translateForTarget error →", err);
    return null;
  }
}





/* ---------- ensureLanguageLoaded: translate and cache on demand ----------
   UPDATED: accepts secure_code and passes it to translateForTarget.
   Signature: ensureLanguageLoaded(langKey, languageSpecificContentMap, preferences, englishBase, tenantForTranslate = null, secure_code = null)
*/
async function ensureLanguageLoaded(
  langKey,
  languageSpecificContentMap,
  preferences,
  englishBase,
  tenantId,
  businessId,
  secure_code
) {
  if (languageSpecificContentMap[langKey]) return;

  try {
    const raw = localStorage.getItem("jio_lang_map_v1");
    if (raw) {
      const parsed = JSON.parse(raw);
      if (parsed?.[langKey]) {
        languageSpecificContentMap[langKey] = parsed[langKey];
        return;
      }
    }
  } catch {}

  if (langKey === "ENGLISH") {
    languageSpecificContentMap.ENGLISH = englishBase;
    return;
  }

  const opt = LANG_OPTIONS.find((o) => o.value === langKey);
  if (!opt) return;

  const apiCode = opt.apiValue;

  // Build payload with ALL translatable strings
  const payload = {
    // Banner strings
    title: englishBase.title || "This site uses cookies",
    description: englishBase.description || "We use cookies to enhance your experience.",
    managePrefs: englishBase.managePrefs || "Manage preferences",
    rejectAll: englishBase.rejectAll || "Reject all",
    acceptAll: englishBase.acceptAll || "Accept all",
    acceptNecessary: englishBase.acceptNecessary || "Accept necessary cookies",
    
    // Modal strings
    modalTitle: "Manage preferences",
    modalDescription: "Choose the cookies you allow. You can update your preferences anytime.",
    savePreferences: "Save preferences",
    required: "Required",
    
    // Cookie table headers
    cookieName: "Cookie name",
    cookieDescription: "Description",
    
    // Toast messages
    toastSaved: "Your cookie preferences have been saved",
    toastAcceptedAll: "Accepted all cookies",
    toastRejectedAll: "Rejected non-essential cookies",
    toastAcceptedNecessary: "Accepted necessary cookies",
  };
  
  // Add each category purpose with its own ID
  preferences.forEach((p, idx) => {
    if (p.purpose) {
      payload[`purpose_${idx}`] = p.purpose;
    }
    const desc = englishBase.categoryDescriptions?.[p.purpose] || getCategoryDesc(p.purpose);
    if (desc) {
      payload[`purpose_desc_${idx}`] = desc;
    }
    
    // Add individual cookie descriptions for translation
    const cookies = p.cookies || [];
    cookies.forEach((ck, ckIdx) => {
      const cookieDesc = ck.description || ck.description_gpt;
      if (cookieDesc && cookieDesc.trim()) {
        payload[`cookie_${idx}_${ckIdx}_desc`] = cookieDesc;
      }
    });
  });
  
  console.log("Translation payload:", payload);

  const translations = await translateForTarget(apiCode, payload, tenantId, businessId, secure_code);

  console.log("=== TRANSLATION DEBUG ===");
  console.log("Payload sent:", payload);
  console.log("Translations received:", translations);
  console.log("cookieName in response:", translations?.cookieName);
  console.log("cookieDescription in response:", translations?.cookieDescription);
  console.log("=== END DEBUG ===");

  if (!translations) {
    languageSpecificContentMap[langKey] = englishBase;
    return;
  }

  const newMap = {
    // Banner
    title: translations.title || englishBase.title,
    description: translations.description || englishBase.description,
    managePrefs: translations.managePrefs || englishBase.managePrefs,
    rejectAll: translations.rejectAll || englishBase.rejectAll,
    acceptAll: translations.acceptAll || englishBase.acceptAll,
    acceptNecessary: translations.acceptNecessary || englishBase.acceptNecessary,
    
    // Modal
    modalTitle: translations.modalTitle || "Manage preferences",
    modalDescription: translations.modalDescription || "Choose the cookies you allow. You can update your preferences anytime.",
    savePreferences: translations.savePreferences || "Save preferences",
    required: translations.required || "Required",
    
    // Cookie table
    cookieName: translations.cookieName || "Cookie name",
    cookieDescription: translations.cookieDescription || "Description",
    
    // Toasts
    toastSaved: translations.toastSaved || "Your cookie preferences have been saved",
    toastAcceptedAll: translations.toastAcceptedAll || "Accepted all cookies",
    toastRejectedAll: translations.toastRejectedAll || "Rejected non-essential cookies",
    toastAcceptedNecessary: translations.toastAcceptedNecessary || "Accepted necessary cookies",
    
    categoryDescriptions: {}
  };

  preferences.forEach((p, idx) => {
    newMap[`purpose_${idx}`] = translations[`purpose_${idx}`] || p.purpose;
    newMap[`purpose_desc_${idx}`] = translations[`purpose_desc_${idx}`] || "";
    
    // Store translated cookie descriptions
    const cookies = p.cookies || [];
    cookies.forEach((ck, ckIdx) => {
      const translatedCookieDesc = translations[`cookie_${idx}_${ckIdx}_desc`];
      if (translatedCookieDesc) {
        newMap[`cookie_${idx}_${ckIdx}_desc`] = translatedCookieDesc;
      }
    });
  });

  languageSpecificContentMap[langKey] = newMap;

  try {
    const raw = localStorage.getItem("jio_lang_map_v1") || "{}";
    const parsed = JSON.parse(raw);
    parsed[langKey] = newMap;
    localStorage.setItem("jio_lang_map_v1", JSON.stringify(parsed));
  } catch {}
}


/* ---------- category fallback descriptions ---------- */
function getCategoryDesc(purpose) {
  const map = {
    Necessary: "These cookies are necessary to enable the basic features of this site to function.",
    Analytics: "These cookies help us analyze site usage and improve performance.",
    Advertisement: "These cookies are used to show ads relevant to your interests.",
    Functional: "These cookies enable enhanced functionality and personalization.",
    Others: "These cookies are unclassified and may be used for other purposes.",
  };
  return map[purpose] || "";
}

/* ---------- small non-blocking saved toast ---------- */
function showSavedToast(message = "Preferences saved") {
  const id = `jio-toast-${Date.now()}`;
  const t = el("div", "", {
    position: "fixed", right: "28px", bottom: "28px", background: "#0A2885",
    color: "#fff", padding: "12px 16px", borderRadius: "10px", boxShadow: "0 8px 24px rgba(2,6,23,0.2)",
    zIndex: 300000, opacity: "0", transform: "translateY(10px)", transition: "all 260ms ease",
    fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
  }, message);
  t.id = id;
  document.body.appendChild(t);
  requestAnimationFrame(()=> { t.style.opacity = "1"; t.style.transform = "translateY(0)"; });
  setTimeout(()=> { t.style.opacity = "0"; t.style.transform = "translateY(10px)"; setTimeout(()=> t.remove(), 320); }, 2800);
}
/* ---------- Exported loader (signature accepts options object) ----------
 * loadCookieCenter({ tenantId, businessId, cookieTemplateId, consentHandleId, secure_code, deviceUUID })
 */
export async function loadCookieCenter(opts = {}) {
  let openedFromFloatingIcon = false;
  const { tenantId, businessId, cookieTemplateId, consentHandleId, secure_code, deviceUUID } = opts || {};

  // Fetch dashboard theme/typography settings
  await loadDashboardTheme(tenantId, businessId);

  // store of the last fetched consent version object (used to prefill toggles)
  let currentConsentVersionData = null;

  try {
  if (!tenantId || !businessId || !cookieTemplateId) {
      console.warn("loadCookieCenter: missing tenantId/businessId/cookieTemplateId — will not call APIs that require them.");
    return;
  }

    // device id
  let deviceuuid = deviceUUID || localStorage.getItem("device_uuid");
  if (!deviceuuid) {
      deviceuuid = (crypto && crypto.randomUUID && crypto.randomUUID()) || `dev-${Date.now()}`;
    localStorage.setItem("device_uuid", deviceuuid);
  }

    // If caller supplied a consentHandleId, use it; otherwise null
    let cookieId = consentHandleId || null;

    // fetch consent handle details (preferences + multilingual data)
  const consentData = await safeFetchJson(`${BASE_URL}/consent-handle/get/${cookieId}`, {
    headers: {
      "Content-Type": "application/json",
      "X-Tenant-ID": tenantId,
      "business-id": businessId,
        "X-Secure-Code": secure_code
    },
  });

    if (!consentData) {
      console.warn("Consent handle fetch returned no data. Exiting.");
      return;
    }

    // Extract and decode theme - check both possible locations
    console.log("=== THEME DEBUG ===");
    console.log("Full consentData:", consentData);
    
    // Theme can be at consentData.theme (cookie API) or consentData.uiConfig.theme (consent API)
    const rawTheme = consentData?.theme || consentData?.uiConfig?.theme;
    console.log("Raw theme (Base64):", rawTheme);
    
    const decodedThemes = decodeTheme(rawTheme);
    console.log("Decoded themes:", decodedThemes);
    
    // Check if dark mode is enabled from API
    const isDarkMode = consentData?.darkMode === true || consentData?.uiConfig?.darkMode === true || 
                       consentData?.isDarkMode === true || consentData?.uiConfig?.isDarkMode === true;
    console.log("Dark mode enabled:", isDarkMode);
    
    // Use dark or light theme based on darkMode setting
    const activeTheme = isDarkMode 
      ? (decodedThemes?.dark || DEFAULT_THEME) 
      : (decodedThemes?.light || DEFAULT_THEME);
    console.log("Active theme:", activeTheme);
    
    // Theme colors with fallbacks
    const themeColors = {
      cardBackground: activeTheme.cardBackground || DEFAULT_THEME.cardBackground,
      cardFont: activeTheme.cardFont || DEFAULT_THEME.cardFont,
      buttonBackground: activeTheme.buttonBackground || DEFAULT_THEME.buttonBackground,
      buttonFont: activeTheme.buttonFont || DEFAULT_THEME.buttonFont,
      linkFont: activeTheme.linkFont || DEFAULT_THEME.linkFont,
      descriptionFont: activeTheme.descriptionFont || DEFAULT_THEME.descriptionFont,
      borderColor: activeTheme.borderColor || DEFAULT_THEME.borderColor,
    };
    
    console.log("Final themeColors applied:", themeColors);
    console.log("=== END THEME DEBUG ===");

    // Extract layout type from API response
    const layoutType = consentData?.layoutType || consentData?.uiConfig?.layoutType || "BANNER";
    console.log("Layout Type:", layoutType);

    // language map + preferences fallback
    let languageSpecificContentMap = (consentData.multilingual && consentData.multilingual.languageSpecificContentMap) || {};
  const preferences = consentData.preferences || [
    { purpose: "Necessary", isMandatory: true, cookies: [] },
    { purpose: "Functional", isMandatory: false, cookies: [] },
    { purpose: "Analytics", isMandatory: false, cookies: [] },
  ];

    // Default button labels
    const DEFAULT_BUTTON_LABELS = {
      managePrefs: "Manage preferences",
      rejectAll: "Reject all",
      acceptNecessary: "Accept necessary cookies",
      acceptAll: "Accept all"
    };
    
    // Get button labels from API (check multiple locations)
    const mlEnglish = languageSpecificContentMap.ENGLISH || consentData?.multilingual?.languageSpecificContentMap?.ENGLISH || {};
    const btnLabels = consentData?.buttonLabels || consentData?.uiConfig?.buttonLabels || {};
    
    // Use API labels if provided, otherwise use defaults
    // A button is hidden only if API explicitly sets it to false or includes it in hideButtons array
    const hideButtons = consentData?.hideButtons || consentData?.uiConfig?.hideButtons || [];
    
    const apiButtonLabels = {
      managePrefs: mlEnglish.managePrefs || btnLabels.managePrefs || consentData?.managePrefs || DEFAULT_BUTTON_LABELS.managePrefs,
      rejectAll: mlEnglish.rejectAll || btnLabels.rejectAll || consentData?.rejectAll || DEFAULT_BUTTON_LABELS.rejectAll,
      acceptNecessary: mlEnglish.acceptNecessary || btnLabels.acceptNecessary || consentData?.acceptNecessary || DEFAULT_BUTTON_LABELS.acceptNecessary,
      acceptAll: mlEnglish.acceptAll || btnLabels.acceptAll || consentData?.acceptAll || DEFAULT_BUTTON_LABELS.acceptAll,
    };
    
    // Check if buttons should be hidden
    const showButtons = {
      managePrefs: !hideButtons.includes('managePrefs') && apiButtonLabels.managePrefs !== false,
      rejectAll: !hideButtons.includes('rejectAll') && apiButtonLabels.rejectAll !== false,
      acceptNecessary: !hideButtons.includes('acceptNecessary') && apiButtonLabels.acceptNecessary !== false,
      acceptAll: !hideButtons.includes('acceptAll') && apiButtonLabels.acceptAll !== false,
    };
    
    console.log("API Button Labels:", apiButtonLabels);
    console.log("Show Buttons:", showButtons);

    /* set English base if missing, or fill in missing button labels */
  if (!languageSpecificContentMap.ENGLISH) {
    languageSpecificContentMap.ENGLISH = {
        title: consentData?.label || "This site uses cookies to make your experience better",
        description: (consentData?.multilingual && consentData.multilingual.defaultDescription) || consentData?.description || "We use essential cookies to make our site work. With your consent, we may also use non-essential cookies to improve user experience and analyse website traffic.",
        managePrefs: apiButtonLabels.managePrefs,
        rejectAll: apiButtonLabels.rejectAll,
        acceptNecessary: apiButtonLabels.acceptNecessary,
        acceptAll: apiButtonLabels.acceptAll,
      categoryDescriptions: {},
    };
  } else {
    // Ensure button labels have defaults even if ENGLISH exists from API
    languageSpecificContentMap.ENGLISH.managePrefs = languageSpecificContentMap.ENGLISH.managePrefs || apiButtonLabels.managePrefs;
    languageSpecificContentMap.ENGLISH.rejectAll = languageSpecificContentMap.ENGLISH.rejectAll || apiButtonLabels.rejectAll;
    languageSpecificContentMap.ENGLISH.acceptNecessary = languageSpecificContentMap.ENGLISH.acceptNecessary || apiButtonLabels.acceptNecessary;
    languageSpecificContentMap.ENGLISH.acceptAll = languageSpecificContentMap.ENGLISH.acceptAll || apiButtonLabels.acceptAll;
  }

    // merge cached maps
  try {
    const raw = localStorage.getItem("jio_lang_map_v1");
    if (raw) {
      const parsed = JSON.parse(raw);
        if (parsed && typeof parsed === "object") {
      languageSpecificContentMap = { ...languageSpecificContentMap, ...parsed };
        }
    }
  } catch {}

    let availableLangKeys = Object.keys(languageSpecificContentMap);
    if (!availableLangKeys || availableLangKeys.length === 0) availableLangKeys = LANG_OPTIONS.map(o => o.value);

  let selectedLang = localStorage.getItem("selected_lang") || "ENGLISH";
    if (!availableLangKeys.includes(selectedLang)) selectedLang = availableLangKeys.includes("ENGLISH") ? "ENGLISH" : availableLangKeys[0];

    /* ---------- Build consent UI based on layoutType ---------- */
    let banner;
    let modalBackdrop = null;
    let floatingIcon = null; // Will be created for BANNER/TOOLTIP layouts
    let categoryStates = {}; // Track accordion open/closed states
    let toggleStates = {};   // Track toggle on/off states
    
    // Safe function to show floating icon (checks if it exists)
    function showFloatingIcon() {
      if (floatingIcon) {
        floatingIcon.style.display = "flex";
      }
    }
    
    // Initialize default states for preferences
    preferences.forEach((p) => {
      categoryStates[p.purpose] = false; // closed by default
      toggleStates[p.purpose] = p.isMandatory ? true : false;
    });
    
    // Check if consent already exists and load version to pre-fill toggles
    const existingConsentId = localStorage.getItem("jio_consent_id");
    const existingConsentVersion = localStorage.getItem("jio_consent_version");
    
    if (existingConsentId && existingConsentVersion) {
      console.log("Existing consent found, fetching version data...");
      const consentVersionData = await loadConsentVersion(existingConsentId, existingConsentVersion);
      
      if (consentVersionData) {
        console.log("Consent version data loaded:", consentVersionData);
        
        // Extract preference status from loaded consent
        let prefStatusFromVersion = {};
        
        // Handle preferencesStatus object format
        if (consentVersionData.preferencesStatus && !consentVersionData.preferences) {
          Object.keys(consentVersionData.preferencesStatus).forEach((purpose) => {
            prefStatusFromVersion[purpose] = consentVersionData.preferencesStatus[purpose];
          });
        }
        
        // Handle preferences array format
        if (Array.isArray(consentVersionData.preferences)) {
          consentVersionData.preferences.forEach((p) => {
            const purpose = p.purpose || p.purposeName || p.purposeLabel;
            const status = p.preferenceStatus || p.preferencesStatus || p.preference_status || "NOTACCEPTED";
            if (purpose) {
              prefStatusFromVersion[purpose] = status;
            }
          });
        }
        
        // Update toggle states based on loaded consent
        preferences.forEach((p) => {
          const backendValue = prefStatusFromVersion[p.purpose];
          if (backendValue === "ACCEPTED") {
            toggleStates[p.purpose] = true;
          } else if (backendValue === "NOTACCEPTED") {
            toggleStates[p.purpose] = p.isMandatory ? true : false; // mandatory always true
          }
        });
        
        console.log("Toggle states after loading consent:", toggleStates);
      }
    }
    
    if (layoutType === "MODAL") {
      // MODAL Layout - Centered popup with consent info and buttons (NOT manage preferences)
      // Similar content to BANNER but displayed as a centered modal with backdrop
      
      // Create backdrop
      modalBackdrop = el("div", "jio-modal-backdrop", {
        position: "fixed", inset: "0",
        background: "rgba(0,0,0,0.4)",
        backdropFilter: "blur(2px)",
        zIndex: "99998"
      });
      document.body.appendChild(modalBackdrop);
      
      // Create modal container - centered popup style
      banner = el("div", "jio-banner jio-modal-layout", {
        position: "fixed", top: "50%", left: "50%", transform: "translate(-50%, -50%)",
        background: themeColors.cardBackground, 
        border: `1px solid ${themeColors.borderColor}`,
        boxShadow: "0 16px 48px rgba(0,0,0,0.15)", 
        padding: "32px",
        display: "flex", flexDirection: "column",
        alignItems: "center", textAlign: "center",
        zIndex: "99999", fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif", 
        width: "460px", maxWidth: "90vw",
        boxSizing: "border-box", borderRadius: "24px",
        color: themeColors.cardFont,
      });
      
      // Close button (optional for modal)
      const closeBtn = el("button", "jio-modal-close", {
        position: "absolute", top: "12px", right: "16px",
        fontSize: "20px", background: "transparent", border: "none",
        color: themeColors.descriptionFont, cursor: "pointer",
        width: "32px", height: "32px", borderRadius: "50%",
        display: "flex", alignItems: "center", justifyContent: "center",
        transition: "background 150ms ease"
      }, "×");
      closeBtn.onmouseover = () => closeBtn.style.background = themeColors.borderColor;
      closeBtn.onmouseout = () => closeBtn.style.background = "transparent";
      closeBtn.onclick = () => {
        banner.remove();
        if (modalBackdrop) modalBackdrop.remove();
        showFloatingIcon();
      };
      banner.appendChild(closeBtn);
      
      const map = languageSpecificContentMap[selectedLang] || languageSpecificContentMap.ENGLISH;
      
      // Title
      const titleEl = el("div", "jio-banner-title", {
        fontWeight: "800", fontSize: "22px",
        color: themeColors.cardFont, marginBottom: "12px"
      }, map.title || languageSpecificContentMap.ENGLISH.title);
      
      // Description
      const descEl = el("div", "jio-banner-desc", {
        fontWeight: "500", fontSize: "14px",
        color: themeColors.descriptionFont,
        lineHeight: "1.6", marginBottom: "24px",
        maxWidth: "380px"
      }, map.description || languageSpecificContentMap.ENGLISH.description);
      
      banner.append(titleEl, descEl);
      
      // Button container - vertical stack for modal
      const buttonWrap = el("div", "jio-btn-wrap", {
        display: "flex", flexDirection: "column",
        gap: "10px", width: "100%", maxWidth: "280px"
      });
      
      // Button helper
      const makeModalBtn = (text, variant) => {
        const isP = variant === "primary";
        return el("button", "jio-action-btn", {
          padding: "12px 20px", borderRadius: "250px",
          fontWeight: "700", fontSize: "14px", cursor: "pointer",
          border: isP ? "none" : `1.5px solid ${themeColors.linkFont}`,
          background: isP ? themeColors.buttonBackground : "transparent",
          color: isP ? themeColors.buttonFont : themeColors.linkFont,
          width: "100%", transition: "transform 150ms ease, opacity 150ms ease"
        }, text);
      };
      
      // Create buttons based on showButtons config - always use defaults as final fallback
      let manageBtn = null, rejectBtn = null, acceptNecBtn = null, acceptAllBtn = null;
      
      if (showButtons.managePrefs) {
        manageBtn = makeModalBtn(map.managePrefs || apiButtonLabels.managePrefs || "Manage preferences", "secondary");
        buttonWrap.appendChild(manageBtn);
      }
      if (showButtons.rejectAll) {
        rejectBtn = makeModalBtn(map.rejectAll || apiButtonLabels.rejectAll || "Reject all", "secondary");
        buttonWrap.appendChild(rejectBtn);
      }
      if (showButtons.acceptNecessary) {
        acceptNecBtn = makeModalBtn(map.acceptNecessary || apiButtonLabels.acceptNecessary || "Accept necessary cookies", "secondary");
        buttonWrap.appendChild(acceptNecBtn);
      }
      if (showButtons.acceptAll) {
        acceptAllBtn = makeModalBtn(map.acceptAll || apiButtonLabels.acceptAll || "Accept all", "primary");
        buttonWrap.appendChild(acceptAllBtn);
      }
      
      banner.appendChild(buttonWrap);
      
      // Append to body
      document.body.appendChild(banner);
      
      // Wire up button handlers (same logic as BANNER/TOOLTIP)
      // These will be connected after common code below
      // Store references for later wiring
      banner._manageBtn = manageBtn;
      banner._rejectBtn = rejectBtn;
      banner._acceptNecBtn = acceptNecBtn;
      banner._acceptAllBtn = acceptAllBtn;
      banner._titleEl = titleEl;
      banner._descEl = descEl;
      
    } else if (layoutType === "TOOLTIP") {
      // TOOLTIP Layout - Small corner popup
      banner = el("div", "jio-banner jio-tooltip-layout", {
        position: "fixed", bottom: "80px", right: "24px",
        background: themeColors.cardBackground, border: `1px solid ${themeColors.borderColor}`,
        boxShadow: "0 12px 40px rgba(2,6,23,0.18)", padding: "18px 22px",
        display: "flex", flexDirection: "column", alignItems: "stretch",
        zIndex: "99999", fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
        width: "380px", maxWidth: "calc(100vw - 48px)",
        boxSizing: "border-box", borderRadius: "12px",
      });
    } else {
      // BANNER Layout - Default full-width bottom banner
      banner = el("div", "jio-banner jio-banner-layout", {
        position: "fixed", bottom: "30px", left: "24px", right: "24px",
        background: themeColors.cardBackground, borderTop: `1px solid ${themeColors.borderColor}`,
        boxShadow: "0 -6px 30px rgba(2,6,23,0.06)", padding: "22px 28px",
        display: "flex", justifyContent: "space-between", alignItems: "center",
        zIndex: "99999", fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif", minHeight: "188px",
        boxSizing: "border-box", borderRadius: "12px",
      });
    }

    // Adjust layout structure based on layoutType
    const isVerticalLayout = layoutType === "TOOLTIP";
    const isModalLayout = layoutType === "MODAL";
    
    // Variables for elements (MODAL has them already, BANNER/TOOLTIP create them below)
    let titleEl, descEl, leftWrap, textCol;
    
    if (!isModalLayout) {
      // text column - only for BANNER/TOOLTIP
      leftWrap = el("div", "jio-left-wrap", { 
        display: "flex", alignItems: "flex-start", gap: "16px", 
        flex: isVerticalLayout ? "none" : "1", 
        minWidth: 0,
        marginBottom: isVerticalLayout ? "16px" : "0"
      });
      textCol = el("div", "jio-text-col", { 
        display: "flex", flexDirection: "column", gap: "8px", minWidth: 0, 
        maxWidth: isVerticalLayout ? "100%" : "1376px" 
      });

      titleEl = el("div", "jio-banner-title", { 
        fontWeight: 800, 
        fontSize: layoutType === "TOOLTIP" ? "16px" : "20px", 
        color: themeColors.cardFont, 
        margin: 0 
      }, languageSpecificContentMap[selectedLang]?.title || languageSpecificContentMap.ENGLISH.title);
      
      descEl = el("div", "jio-banner-desc", { 
        fontWeight: 500, 
        fontSize: layoutType === "TOOLTIP" ? "13px" : "14px", 
        color: themeColors.descriptionFont, 
        marginTop: "6px", 
        maxHeight: layoutType === "TOOLTIP" ? "60px" : "96px", 
        overflow: "hidden" 
      }, languageSpecificContentMap[selectedLang]?.description || languageSpecificContentMap.ENGLISH.description);

      textCol.append(titleEl, descEl);
      leftWrap.append(textCol);
    } else {
      // MODAL layout - get references from banner
      titleEl = banner._titleEl;
      descEl = banner._descEl;
    }
  
    // Add close button for TOOLTIP layout
    if (isVerticalLayout) {
      const closeBtn = el("button", "jio-close-btn", {
        position: "absolute", top: "12px", right: "12px",
        width: "28px", height: "28px", border: "none",
        background: "transparent", fontSize: "18px",
        cursor: "pointer", color: themeColors.cardFont,
        display: "flex", alignItems: "center", justifyContent: "center",
        borderRadius: "50%", transition: "background 150ms ease"
      }, "✕");
      closeBtn.onmouseover = () => closeBtn.style.background = themeColors.borderColor;
      closeBtn.onmouseout = () => closeBtn.style.background = "transparent";
      closeBtn.onclick = () => {
        banner.remove();
        if (modalBackdrop) modalBackdrop.remove();
        showFloatingIcon();
      };
      banner.style.position = "fixed";
      banner.appendChild(closeBtn);
    }

    // Cookie icon SVG helper (used by floating icon)
    function createCookieIconSVG() {
      const ns = "http://www.w3.org/2000/svg";
      const svg = document.createElementNS(ns, "svg");
      svg.setAttribute("width", "24");
      svg.setAttribute("height", "24");
      svg.setAttribute("viewBox", "0 0 24 24");
      svg.setAttribute("fill", "none");
      svg.innerHTML = `
        <path d="M17.5 6C18.3284 6 19 5.32843 19 4.5C19 3.67157 18.3284 3 17.5 3C16.6716 3 16 3.67157 16 4.5C16 5.32843 16.6716 6 17.5 6Z" fill="#FFFFFF"/>
        <path d="M22 11.95C21.84 11.97 21.67 12 21.5 12C20.2 12 19.06 11.37 18.33 10.41C18.06 10.47 17.79 10.5 17.5 10.5C15.29 10.5 13.5 8.71 13.5 6.5C13.5 6.07 13.59 5.66 13.71 5.27C12.68 4.55 12 3.35 12 2C6.48 2 2 6.48 2 12C2 17.52 6.48 22 12 22C17.52 22 22 17.52 22 12C22 11.98 22 11.97 22 11.95ZM7.5 16C6.67 16 6 15.33 6 14.5C6 13.67 6.67 13 7.5 13C8.33 13 9 13.67 9 14.5C9 15.33 8.33 16 7.5 16ZM8.5 10.5C7.4 10.5 6.5 9.6 6.5 8.5C6.5 7.4 7.4 6.5 8.5 6.5C9.6 6.5 10.5 7.4 10.5 8.5C10.5 9.6 9.6 10.5 8.5 10.5ZM17 17C16.17 17 15.5 16.33 15.5 15.5C15.5 14.67 16.17 14 17 14C17.83 14 18.5 14.67 18.5 15.5C18.5 16.33 17.83 17 17 17Z" fill="#FFFFFF"/>
      `;
      return svg;
    }

    // Variables for buttons (will be assigned below)
    let manageBtn, rejectBtn, acceptNecBtn, acceptAllBtn, langBtn, rightWrap;
    
    if (isModalLayout) {
      // MODAL layout - get button references from banner
      manageBtn = banner._manageBtn;
      rejectBtn = banner._rejectBtn;
      acceptNecBtn = banner._acceptNecBtn;
      acceptAllBtn = banner._acceptAllBtn;
      
      // Create language button for MODAL (add to banner)
      langBtn = el("button", "jio-lang-btn", { 
        position: "absolute", top: "12px", left: "16px",
        display: "flex", alignItems: "center", gap: "6px", 
        border: `1px solid ${themeColors.borderColor}`, borderRadius: "24px", 
        padding: "6px 12px", cursor: "pointer", height: "30px", 
        background: themeColors.cardBackground, fontWeight: 600, fontSize: "13px", 
        color: themeColors.cardFont 
      });
      const svgIcon = createLanguageIconSVG(14);
      const langLabel = el("span", "jio-lang-label", { fontWeight: 600, fontSize: "13px" }, selectedLang);
      const langCaret = el("span", "jio-lang-caret", { marginLeft: "4px", fontSize: "10px", color: "#6B7280" }, "▾");
      langBtn.append(svgIcon, langLabel, langCaret);
      banner.appendChild(langBtn);
      
    } else {
      // BANNER/TOOLTIP layout - create buttons column
      rightWrap = el("div", "jio-right-wrap", { 
        display: "flex", 
        flexDirection: "column", 
        alignItems: isVerticalLayout ? "stretch" : "center", 
        gap: isVerticalLayout ? "12px" : "50px", 
        minWidth: isVerticalLayout ? "auto" : "320px", 
        marginLeft: isVerticalLayout ? "0" : "24px" 
      });

      langBtn = el("button", "jio-lang-btn", { display: "flex", alignItems: "center", gap: "8px", border: `1px solid ${themeColors.borderColor}`, borderRadius: "24px", padding: "6px 10px", cursor: "pointer", height: "30px", background: themeColors.cardBackground, minWidth: "140px", fontWeight: 600, fontSize: "14px", color: themeColors.cardFont });
      langBtn.setAttribute("aria-haspopup", "true");
      langBtn.setAttribute("aria-label", "Select language");

      const svgIcon = createLanguageIconSVG(16);
      svgIcon.style.flex = "0 0 18px";
      svgIcon.style.marginRight = "4px";
      const langLabel = el("span", "jio-lang-label", { fontWeight: 600, fontSize: "14px" }, selectedLang);
      const langCaret = el("span", "jio-lang-caret", { marginLeft: "auto", fontSize: "12px", color: "#6B7280" }, "▾");
      langBtn.append(svgIcon, langLabel, langCaret);

      // buttons - adjust layout for vertical layouts
      const buttonWrap = el("div", "jio-button-wrap", { 
        display: "flex", 
        gap: isVerticalLayout ? "8px" : "12px", 
        alignItems: "center", 
        justifyContent: isVerticalLayout ? "stretch" : "center", 
        flexWrap: "wrap",
        flexDirection: layoutType === "TOOLTIP" ? "column" : "row"
      });
      const makeBtn = (label, type = "primary") => {
        const b = el("button", "jio-action-btn", {
          padding: layoutType === "TOOLTIP" ? "6px 12px" : "8px 16px", 
          borderRadius: "250px", 
          fontWeight: 700, 
          fontSize: layoutType === "TOOLTIP" ? "12px" : "14px", 
          cursor: "pointer",
          border: type === "primary" ? "none" : `1px solid ${themeColors.linkFont}`,
          background: type === "primary" ? themeColors.buttonBackground : themeColors.cardBackground,
          color: type === "primary" ? themeColors.buttonFont : themeColors.linkFont,
          height: layoutType === "TOOLTIP" ? "32px" : "40px", 
          minWidth: layoutType === "TOOLTIP" ? "auto" : "140px",
          width: layoutType === "TOOLTIP" ? "100%" : "auto"
        }, label);
        return b;
      };

      // Create buttons based on showButtons config - always use defaults as final fallback
      if (showButtons.managePrefs) {
        manageBtn = makeBtn(languageSpecificContentMap.ENGLISH.managePrefs || apiButtonLabels.managePrefs || "Manage preferences", "secondary");
        manageBtn.setAttribute("aria-label", "Manage preferences");
        buttonWrap.appendChild(manageBtn);
      }
      if (showButtons.rejectAll) {
        rejectBtn = makeBtn(languageSpecificContentMap.ENGLISH.rejectAll || apiButtonLabels.rejectAll || "Reject all", "secondary");
        buttonWrap.appendChild(rejectBtn);
      }
      if (showButtons.acceptNecessary) {
        acceptNecBtn = makeBtn(languageSpecificContentMap.ENGLISH.acceptNecessary || apiButtonLabels.acceptNecessary || "Accept necessary cookies", "secondary");
        buttonWrap.appendChild(acceptNecBtn);
      }
      if (showButtons.acceptAll) {
        acceptAllBtn = makeBtn(languageSpecificContentMap.ENGLISH.acceptAll || apiButtonLabels.acceptAll || "Accept all", "primary");
        buttonWrap.appendChild(acceptAllBtn);
      }
      rightWrap.append(langBtn, buttonWrap);
      banner.append(leftWrap, rightWrap);
      
      document.body.appendChild(banner);
    }

    /* -------- Floating Cookie Icon (left) ---------- */
    floatingIcon = el("div", "jio-floating-cookie");
    floatingIcon.style.background = themeColors.buttonBackground;
  floatingIcon.appendChild(createCookieIconSVG());
  document.body.appendChild(floatingIcon);

    // Hide initially — only show after saving
    floatingIcon.style.display = "none";

    // If clicked → open preferences modal (toggle)
   floatingIcon.onclick = async () => {
      openedFromFloatingIcon = true;

  // close existing modal
  if (currentModal && currentModal.overlayEl && document.body.contains(currentModal.overlayEl)) {
    currentModal.overlayEl.remove();
    currentModal = null;
    openedFromFloatingIcon = false;
    return;
  }

  // ⭐ ALWAYS fetch latest consent before opening modal
  const cid = localStorage.getItem("jio_consent_id");
  const ver = localStorage.getItem("jio_consent_version");
  if (cid && ver) {
    const latest = await loadConsentVersion(cid, ver);
    if (latest) currentConsentVersionData = latest;
  }

  manageBtn.click();
};


    // showFloatingIcon is defined earlier and handles null check

    // style tweaks - Apply theme colors (only if buttons exist)
    if (acceptAllBtn) { acceptAllBtn.style.background = themeColors.buttonBackground; acceptAllBtn.style.color = themeColors.buttonFont; }
    if (rejectBtn) { rejectBtn.style.background = themeColors.cardBackground; rejectBtn.style.color = themeColors.linkFont; }
    if (acceptNecBtn) { acceptNecBtn.style.background = themeColors.cardBackground; acceptNecBtn.style.color = themeColors.linkFont; }
    if (manageBtn) { manageBtn.style.background = themeColors.cardBackground; manageBtn.style.color = themeColors.linkFont; }

    // inject styles once
    (function injectStyles() {
      if (document.getElementById("jio-cookie-styles-v2")) return;
      
      // Import Google Font - Inter (clean, modern, highly readable)
      if (!document.getElementById("jio-google-font")) {
        const fontLink = document.createElement("link");
        fontLink.id = "jio-google-font";
        fontLink.rel = "stylesheet";
        fontLink.href = "https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap";
        document.head.appendChild(fontLink);
      }
      
      const css = `
        @keyframes jio-slideUp {0%{transform:translateY(20px);opacity:0}100%{transform:translateY(0);opacity:1}}
        
        /* Global font for all cookie UI elements */
        [class^="jio-"], [class*=" jio-"] {
          font-family: ${globalFontFamily} !important;
          font-size: ${globalFontSize} !important;
          font-weight: ${globalFontWeight} !important;
          font-style: ${globalFontStyle} !important;
          -webkit-font-smoothing: antialiased;
          -moz-osx-font-smoothing: grayscale;
        }
        
        .jio-banner{max-width:1600px;transition:box-shadow 200ms ease}
        .jio-left-wrap{padding-right:12px}
        .jio-banner-title{font-weight:700;font-size:20px;color:#111827;letter-spacing:-0.02em}
        .jio-banner-desc{color:#6B7280;font-size:14px;line-height:1.5}
        .jio-lang-btn{background:#fff;border:1px solid #E5E7EB;padding:6px 10px;cursor:pointer;font-weight:500}
        .jio-action-btn{min-width:120px;transition:transform 160ms ease;font-weight:600;letter-spacing:-0.01em}
        .jio-action-btn:hover{transform:translateY(-2px)}
        .jio-toggle{appearance:none;width:44px;height:24px;background:#D9D9D9;border-radius:12px;position:relative;cursor:pointer;border:none}
        .jio-toggle:checked{background:#0F3CC9}
        .jio-toggle::before{content:"";position:absolute;width:18px;height:18px;top:3px;left:3px;background:#fff;border-radius:50%;transition:transform .22s}
        .jio-toggle:checked::before{transform:translateX(20px)}
        .jio-modal-overlay{position:fixed;inset:0;background:rgba(0,0,0,0.55);backdrop-filter:blur(6px);z-index:100000;display:flex;align-items:center;justify-content:center}
        .jio-modal{width:584px;max-height:80vh;border-radius:24px;background:#fff;box-shadow:0 18px 50px rgba(2,6,23,0.18);display:flex;flex-direction:column;overflow:hidden}
        .jio-modal .jio-modal-header{padding:28px 28px 12px;border-bottom:1px solid #E6E9EF;display:flex;justify-content:space-between}
        .jio-modal .jio-modal-content{padding:20px 28px;overflow:auto;flex:1}
        .jio-modal .jio-modal-footer{padding:16px 28px 20px;border-top:1px solid #E6E9EF;text-align:right}
        /* Floating Cookie Icon (left) */
        .jio-floating-cookie {
          position: fixed;
          bottom: 26px;
          left: 28px;        /* ← left placement */
          width: 54px;
          height: 54px;
          background: #0A2885;
          border-radius: 50%;
          display: none;
          align-items: center;
          justify-content: center;
          cursor: pointer;
          box-shadow: 0 8px 20px rgba(0,0,0,0.18);
          z-index: 200000;
          transition: transform 160ms ease, box-shadow 200ms ease;
        }
        .jio-floating-cookie:hover { transform: translateY(-3px); box-shadow: 0 12px 28px rgba(0,0,0,0.22); }
        .jio-floating-cookie svg { width: 26px; height: 26px; fill: #fff; }
      `;
      const s = document.createElement("style");
      s.id = "jio-cookie-styles-v2";
      s.textContent = css;
      document.head.appendChild(s);
    })();
    /* ---------- consent status check API ---------- */
  async function checkConsentStatus() {
    const deviceId = deviceuuid;
    const currentUrl = window.location.href;
      const consentIdStored = localStorage.getItem("jio_consent_id") || "";
      const apiUrl = `${BASE_URL}/consent/check?deviceId=${encodeURIComponent(deviceId)}&url=${encodeURIComponent(currentUrl)}&consentId=${encodeURIComponent(consentIdStored)}`;
      const result = await safeFetchJson(apiUrl, {
        method: "GET",
        headers: {
          "X-Tenant-ID": tenantId,
          "business-id": businessId,
          "X-Secure-Code": secure_code
        }
      });
      return result;
    }

    /* ---------- load a specific consent version (to restore toggles) ---------- */
async function loadConsentVersion(consentId, version) {
  if (!consentId || version == null) return null;
  const url = `${BASE_URL}/consent/${encodeURIComponent(consentId)}/versions/${encodeURIComponent(version)}`;
  const res = await safeFetchJson(url, {
      method: "GET",
      headers: {
      "Content-Type": "application/json",
        "X-Tenant-ID": tenantId,
        "business-id": businessId,
      "X-Secure-Code": secure_code
    }
  });

  // ⭐ PATCH: Normalize backend fields for UI compatibility
  if (res) {
    if (res.preferencesStatus && !res.preferences) {
      res.preferences = Object.keys(res.preferencesStatus).map(key => ({
        purpose: key,
        preferenceStatus: res.preferencesStatus[key]
      }));
    }

    if (Array.isArray(res.preferences)) {
      res.preferences = res.preferences.map(p => ({
        ...p,
        purpose: p.purpose || p.purposeName || p.purposeLabel,
        preferenceStatus:
          p.preferenceStatus ||
          p.preferencesStatus ||
          p.preference_status ||
          p.preferenceStatusValue ||
          "NOTACCEPTED"
      }));
    }
  }

  return res;
}



    
/* ---------- send (create) consent ---------- */
  async function sendConsent(prefStatus) {
    const body = {
    consentHandleId: consentHandleId || cookieId,
      languagePreference: selectedLang,
      preferencesStatus: prefStatus,
    };

    const res = await safeFetchJson(`${BASE_URL}/consent/create`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Tenant-ID": tenantId,
        "business-id": businessId,
      "X-Secure-Code": secure_code
      },
      body: JSON.stringify(body),
    });

  // store consent id + version
    if (res?.consentId) localStorage.setItem("jio_consent_id", res.consentId);
  const version = res?.version ?? res?.newVersion;
  if (version != null) localStorage.setItem("jio_consent_version", String(version));

  if (banner && banner.parentNode) banner.remove();
  if (modalBackdrop && modalBackdrop.parentNode) modalBackdrop.remove();
    showFloatingIcon();

  // ⭐ NEW: immediately fetch created version so UI toggles are correct
  if (res?.consentId && version != null) {
    const latest = await loadConsentVersion(res.consentId, version);
    if (latest) currentConsentVersionData = latest;
    }

    return res;
  }




    /* ---------- update consent (must use consentId) ---------- */
  async function updateConsent(prefStatus) {
      // Use consentId from create/update stored locally
      const consentId = localStorage.getItem("jio_consent_id");
      if (!consentId) {
        console.warn("Consent ID missing — cannot update. Falling back to create.");
        return sendConsent(prefStatus);
      }

    const body = {
        consentHandleId: consentHandleId || cookieId,
      languagePreference: selectedLang,
      preferencesStatus: prefStatus,
    };

      const res = await safeFetchJson(`${BASE_URL}/consent/${encodeURIComponent(consentId)}/update`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        "X-Tenant-ID": tenantId,
        "business-id": businessId,
          "X-Secure-Code": secure_code
      },
      body: JSON.stringify(body),
    });

      // update stored version/consentId if returned
      if (res?.consentId) {
        localStorage.setItem("jio_consent_id", res.consentId);
      }
      if (res?.newVersion != null) {
        localStorage.setItem("jio_consent_version", String(res.newVersion));
      } else if (res?.version != null) {
        localStorage.setItem("jio_consent_version", String(res.version));
      }

    return res;
  }

    /* ---------- quick actions wiring ---------- */
    // acceptAllBtn.onclick = () => {
    //   const allAccepted = {};
    //   preferences.forEach((p) => (allAccepted[p.purpose] = "ACCEPTED"));
    //   sendConsent(allAccepted);
    //   showSavedToast("Accepted all cookies");
    // };
    // rejectBtn.onclick = () => {
    //   const allRejected = {};
    //   preferences.forEach((p) => (allRejected[p.purpose] = "NOTACCEPTED"));
    //   sendConsent(allRejected);
    //   showSavedToast("Rejected non-essential cookies");
    // };
    // acceptNecBtn.onclick = () => {
    //   const mandatoryOnly = {};
    //   preferences.forEach((p) => { mandatoryOnly[p.purpose] = p.isMandatory ? "ACCEPTED" : "NOTACCEPTED"; });
    //   sendConsent(mandatoryOnly);
    //   showSavedToast("Accepted necessary cookies");
    // };
/* ---------- quick actions wiring (FULL PATCH) ---------- */
if (acceptAllBtn) {
  acceptAllBtn.onclick = async () => {
    try {
      const allAccepted = {};
      preferences.forEach((p) => (allAccepted[p.purpose] = "ACCEPTED"));

      // Check if consent already exists - update instead of create
      const existingId = localStorage.getItem("jio_consent_id");
      let res;
      if (existingId) {
        res = await updateConsent(allAccepted);
      } else {
        res = await sendConsent(allAccepted);
      }

      // ⭐ Fetch version after creation/update
      const cid = res?.consentId || existingId;
      const ver = res?.version ?? res?.newVersion ?? localStorage.getItem("jio_consent_version");
      if (cid && ver != null) {
        const latest = await loadConsentVersion(cid, ver);
        if (latest) currentConsentVersionData = latest;
      }

      const toastMap = languageSpecificContentMap[selectedLang] || {};
      showSavedToast(toastMap.toastAcceptedAll || "Accepted all cookies");
    } catch (err) {
      console.error("Error accepting all cookies:", err);
      showSavedToast("Accepted all cookies");
    }
  };
}

if (rejectBtn) {
  rejectBtn.onclick = async () => {
    try {
      const allRejected = {};
      preferences.forEach((p) => (allRejected[p.purpose] = "NOTACCEPTED"));

      // Check if consent already exists - update instead of create
      const existingId = localStorage.getItem("jio_consent_id");
      let res;
      if (existingId) {
        res = await updateConsent(allRejected);
      } else {
        res = await sendConsent(allRejected);
      }

      // ⭐ Fetch version after creation/update
      const cid = res?.consentId || existingId;
      const ver = res?.version ?? res?.newVersion ?? localStorage.getItem("jio_consent_version");
      if (cid && ver != null) {
        const latest = await loadConsentVersion(cid, ver);
        if (latest) currentConsentVersionData = latest;
      }

      const toastMap = languageSpecificContentMap[selectedLang] || {};
      showSavedToast(toastMap.toastRejectedAll || "Rejected non-essential cookies");
    } catch (err) {
      console.error("Error rejecting cookies:", err);
      showSavedToast("Rejected non-essential cookies");
    }
  };
}

if (acceptNecBtn) {
  acceptNecBtn.onclick = async () => {
    try {
      const mandatoryOnly = {};
      preferences.forEach((p) => {
        mandatoryOnly[p.purpose] = p.isMandatory ? "ACCEPTED" : "NOTACCEPTED";
      });

      // Check if consent already exists - update instead of create
      const existingId = localStorage.getItem("jio_consent_id");
      let res;
      if (existingId) {
        res = await updateConsent(mandatoryOnly);
      } else {
        res = await sendConsent(mandatoryOnly);
      }

      // ⭐ Fetch version after creation/update
      const cid = res?.consentId || existingId;
      const ver = res?.version ?? res?.newVersion ?? localStorage.getItem("jio_consent_version");
      if (cid && ver != null) {
        const latest = await loadConsentVersion(cid, ver);
        if (latest) currentConsentVersionData = latest;
      }

      const toastMap = languageSpecificContentMap[selectedLang] || {};
      showSavedToast(toastMap.toastAcceptedNecessary || "Accepted necessary cookies");
    } catch (err) {
      console.error("Error accepting necessary cookies:", err);
      showSavedToast("Accepted necessary cookies");
    }
  };
}

    /* ---------- modal builder & content ---------- */
    function createPreferencesModal() {
      const map = languageSpecificContentMap[selectedLang] || languageSpecificContentMap.ENGLISH || {};
      
      const overlay = el("div", "jio-modal-overlay", {});
      const modal = el("div", "jio-modal", { background: themeColors.cardBackground });
      const headerWrap = el("div", "jio-modal-header", { background: themeColors.cardBackground });
      const headerLeft = el("div", "", { display: "flex", flexDirection: "column" });
      const h2 = el("h2", "", { margin: 0, fontWeight: 900, fontSize: "24px", color: themeColors.cardFont }, map.modalTitle || "Manage preferences");
      const p = el("p", "", { margin: 0, marginTop: "6px", color: themeColors.descriptionFont }, map.modalDescription || "Choose the cookies you allow. You can update your preferences anytime.");
      headerLeft.append(h2, p);

      const headerRight = el("div", "", { display: "flex", gap: "8px", alignItems: "center" });
      const modalLangBtn = langBtn.cloneNode(true);
      // Reset position styles that may have been inherited from MODAL layout langBtn
      modalLangBtn.style.position = "relative";
      modalLangBtn.style.top = "auto";
      modalLangBtn.style.left = "auto";
      if (!modalLangBtn.querySelector("svg")) modalLangBtn.insertBefore(createLanguageIconSVG(16), modalLangBtn.firstChild);
      const closeBtn = el("button", "", { width: "44px", height: "44px", border: "none", background: "transparent", fontSize: "20px", cursor: "pointer", color: themeColors.cardFont }, "✕");
      headerRight.append(modalLangBtn, closeBtn);
      headerWrap.append(headerLeft, headerRight);

      const scrollArea = el("div", "scroll-area", { flex: "1", overflowY: "auto", padding: "20px 28px", background: themeColors.cardBackground });
      const footer = el("div", "", { padding: "16px 28px 28px", borderTop: `1px solid ${themeColors.borderColor}`, textAlign: "right", background: themeColors.cardBackground });
      const saveBtn = el("button", "", { padding: "8px 16px", borderRadius: "250px", fontWeight: 700, fontSize: "14px", cursor: "pointer", border: "none", background: themeColors.buttonBackground, color: themeColors.buttonFont, height: "40px" }, map.savePreferences || "Save preferences");

      footer.append(saveBtn);
      modal.append(headerWrap, scrollArea, footer);
      overlay.appendChild(modal);
      document.body.appendChild(overlay);
      closeBtn.onclick = () => overlay.remove();

      return { overlay, modal, header: { h2, p, modalLangBtn }, scrollArea, saveBtn, closeBtn, overlayEl: overlay };
    }

    function buildModalContent(scrollAreaEl, langKey, languageSpecificContentMapLocal, consentVersionData = null) {
  scrollAreaEl.innerHTML = "";
  const map = (languageSpecificContentMapLocal && languageSpecificContentMapLocal[langKey]) || languageSpecificContentMap.ENGLISH;
  
  console.log("=== BUILD MODAL DEBUG ===");
  console.log("langKey:", langKey);
  console.log("map:", map);
  console.log("map.cookieName:", map?.cookieName);
  console.log("map.cookieDescription:", map?.cookieDescription);
  console.log("=== END BUILD MODAL DEBUG ===");
  
    const switchStates = {};

  // ⭐ STEP 1: Normalize backend status formats into a single map
  let prefStatusFromVersion = null;

  if (consentVersionData) {
    prefStatusFromVersion = {};

    // Case 1 — backend returns preferencesStatus: { Analytics: "ACCEPTED" }
    if (consentVersionData.preferencesStatus && !consentVersionData.preferences) {
      Object.keys(consentVersionData.preferencesStatus).forEach((purpose) => {
        prefStatusFromVersion[purpose] = consentVersionData.preferencesStatus[purpose];
      });
    }

    // Case 2 — backend returns preferences array
    if (Array.isArray(consentVersionData.preferences)) {
      consentVersionData.preferences.forEach((p) => {
        const purpose = p.purpose || p.purposeName || p.purposeLabel;
        const status =
          p.preferenceStatus ||
          p.preferencesStatus ||
          p.preference_status ||
          p.preferenceStatusValue ||
          "NOTACCEPTED";

        if (purpose) {
          prefStatusFromVersion[purpose] = status;
        }
        });
      }
    }

  // ⭐ STEP 2: Render categories with correct prefilled status
  preferences.forEach((p, idx) => {
    // default state: mandatory → true, non-mandatory → false
    let initial = p.isMandatory ? true : false;

    // override using loaded version data
    if (prefStatusFromVersion) {
      const purpose = p.purpose;
      const backendValue = prefStatusFromVersion[purpose];

      if (backendValue === "ACCEPTED") initial = true;
      if (backendValue === "NOTACCEPTED") initial = false;
    }

    switchStates[p.purpose] = initial;

    // UI section for each category
    const section = el("div", "jio-cat-section", {
      borderBottom: `1px solid ${themeColors.borderColor}`,
      marginBottom: "16px",
      paddingBottom: "16px",
    });

    const head = el("div", "", {
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
      });

    const purposeLabel =
      (map && map[`purpose_${idx}`]) ||
      map.purposeMap?.[p.purpose] ||
      p.purpose;

    const title = el("h3", "", { color: themeColors.cardFont }, "");
    const titleTextSpan = el("span", "", {}, purposeLabel);
    title.appendChild(titleTextSpan);

    if (p.isMandatory) {
        title.appendChild(
          el(
            "span",
          "mandatory-lock",
            {
              fontSize: "12px",
            color: themeColors.descriptionFont,
            background: themeColors.borderColor,
              padding: "2px 6px",
            borderRadius: "8px",
            marginLeft: "8px",
            fontWeight: "700",
            },
            `🔒 ${map.required || "Required"}`
          )
        );
      }

    const toggleWrap = el("div", "", {
        display: "flex",
        alignItems: "center",
      gap: "8px",
      });

    const toggle = el("input");
      toggle.type = "checkbox";
    toggle.className = "jio-toggle";
    toggle.checked = initial;
    toggle.disabled = p.isMandatory;
    toggle.setAttribute("aria-label", `${purposeLabel} toggle`);
    toggle.onchange = () => {
      switchStates[p.purpose] = toggle.checked;
    };

      toggleWrap.append(toggle);
    head.append(title, toggleWrap);
    section.append(head);

    // description
    const descText =
      (map && map[`purpose_desc_${idx}`]) ||
      map.categoryDescriptions?.[p.purpose] ||
      getCategoryDesc(p.purpose);

    section.append(el("p", "jio-cat-desc", { color: themeColors.descriptionFont }, descText));

    // cookies list (accordion)
    const cookies = p.cookies || [];
    const vendors = {};

    cookies.forEach((c, ckIdx) => {
        const host = extractHost(c);
      const vname = prettifyVendor(host);
      if (!vendors[vname]) vendors[vname] = [];
      // Store the cookie with its original index for translation lookup
      vendors[vname].push({ ...c, _ckIdx: ckIdx, _prefIdx: idx });
    });

    Object.entries(vendors).forEach(([vendor, clist]) => {
      const acc = el("div", "vendor-accordion", {
          marginTop: "10px",
        border: `1px solid ${themeColors.borderColor}`,
        borderRadius: "8px",
          overflow: "hidden",
        });

      const header = el(
          "button",
        "jio-vendor-header",
          {
            width: "100%",
            textAlign: "left",
          background: themeColors.cardBackground,
          border: "none",
            padding: "12px",
            cursor: "pointer",
          fontWeight: "500",
          fontSize: "16px",
          color: themeColors.cardFont,
          },
          vendor
        );
      header.setAttribute("aria-expanded", "false");

      const chevron = el("span", "jio-vendor-chevron", { color: themeColors.descriptionFont }, "▾");
      header.appendChild(chevron);

        const body = el("div", "", {
          display: "none",
        background: themeColors.cardBackground,
          padding: "8px 12px",
        });

      header.onclick = () => {
        const shown = body.style.display === "block";
        body.style.display = shown ? "none" : "block";
        header.setAttribute("aria-expanded", shown ? "false" : "true");
        };

        const table = el("table", "jio-cookie-table", {
          width: "100%",
          borderCollapse: "collapse",
        });

      const thead = el("thead", "", {}, `<tr><th>${map.cookieName || "Cookie name"}</th><th>${map.cookieDescription || "Description"}</th></tr>`);
        const tbody = el("tbody");

      clist.forEach((ck) => {
          const tr = document.createElement("tr");
          // Use translated cookie description if available
          const translatedDesc = map[`cookie_${ck._prefIdx}_${ck._ckIdx}_desc`];
          const cookieDesc = translatedDesc || ck.description || ck.description_gpt || "-";
        tr.innerHTML = `<td>${ck.name || "-"}</td><td>${cookieDesc}</td>`;
          tbody.append(tr);
        });

        table.append(thead, tbody);
        body.append(table);
      acc.append(header, body);
        section.append(acc);
      });

    scrollAreaEl.append(section);
    });

    return {
      readStates: () => {
      const states = {};
      const sections = Array.from(scrollAreaEl.querySelectorAll(".jio-cat-section"));
      preferences.forEach((p, idx) => {
        const sec = sections[idx];
        if (sec) {
          const input = sec.querySelector(".jio-toggle");
          states[p.purpose] = input && input.checked ? "ACCEPTED" : "NOTACCEPTED";
        } else {
          states[p.purpose] = p.isMandatory ? "ACCEPTED" : "NOTACCEPTED";
        }
      });
      return states;
      },
    };
  }


    // manageBtn opens modal
    let currentModal = null;
    
    // Function to update all modal text with current language
    function updateModalLanguage(modal) {
      const map = languageSpecificContentMap[selectedLang] || languageSpecificContentMap.ENGLISH || {};
      if (modal.header.h2) modal.header.h2.textContent = map.modalTitle || "Manage preferences";
      if (modal.header.p) modal.header.p.textContent = map.modalDescription || "Choose the cookies you allow. You can update your preferences anytime.";
      if (modal.saveBtn) modal.saveBtn.textContent = map.savePreferences || "Save preferences";
      const modalLangLabel = modal.header.modalLangBtn?.querySelector("span");
      if (modalLangLabel) modalLangLabel.textContent = selectedLang;
    }
    
    if (manageBtn) {
      manageBtn.onclick = async () => {
        // Hide the consent banner while Manage Preferences modal is open
        if (banner) banner.style.display = "none";
        if (modalBackdrop) modalBackdrop.style.display = "none";
      
    currentModal = createPreferencesModal();
      updateModalLanguage(currentModal);
      
      // Override close button to show banner again (unless opened from floating icon)
      currentModal.closeBtn.onclick = () => {
        currentModal.overlayEl.remove();
        if (!openedFromFloatingIcon) {
          // Show banner again since user didn't save
          if (banner) banner.style.display = "";
          if (modalBackdrop) modalBackdrop.style.display = "";
        }
        openedFromFloatingIcon = false;
      };

    currentModal.header.modalLangBtn.onclick = () => {
      showLanguagePicker(currentModal.header.modalLangBtn, async (newLang) => {
          // IMPORTANT: pass secure_code here
        if (!languageSpecificContentMap[newLang]) {
            await ensureLanguageLoaded(newLang, languageSpecificContentMap, preferences, languageSpecificContentMap.ENGLISH, tenantId, businessId, secure_code);
          }
        selectedLang = newLang;
        applyLanguageToBanner(selectedLang, languageSpecificContentMap);
        
        // Update ALL modal text with new language
        updateModalLanguage(currentModal);
        
        // Rebuild modal content with translations
        buildModalContent(currentModal.scrollArea, selectedLang, languageSpecificContentMap, currentConsentVersionData);
      });
    };

  // Always fetch the latest consent version when opening modal
// Always fetch latest consent version when opening modal
const cid = localStorage.getItem("jio_consent_id");
const ver = localStorage.getItem("jio_consent_version");

currentConsentVersionData = null;

if (cid && ver) {
  const loaded = await loadConsentVersion(cid, ver);
  if (loaded && loaded.preferences) {
    currentConsentVersionData = loaded;
  }
}



 const builder = buildModalContent(currentModal.scrollArea, selectedLang, languageSpecificContentMap, currentConsentVersionData);

    currentModal.saveBtn.onclick = async () => {
      const prefStatus = builder.readStates();
      
      // Check if consent already exists - update instead of create
      const existingConsentId = localStorage.getItem("jio_consent_id");
      
      if (existingConsentId) {
        // update flow - consent already exists
        const res = await updateConsent(prefStatus);
        // if update returns new version, refresh local stored version and cached consentVersionData
        if (res) {
          if (res.consentId) localStorage.setItem("jio_consent_id", res.consentId);

          const v = res.newVersion != null ? res.newVersion : res.version;
          if (v != null) localStorage.setItem("jio_consent_version", String(v));

          // Reload version data
          const cid = localStorage.getItem("jio_consent_id");
          const ver = localStorage.getItem("jio_consent_version");
          if (cid && ver) {
            const latest = await loadConsentVersion(cid, ver);
            if (latest) currentConsentVersionData = latest;
          }
        }
      } else {
        // create flow - no existing consent
        const res = await sendConsent(prefStatus);
        // store version info already handled in sendConsent
        if (res) {
          const cid = localStorage.getItem("jio_consent_id");
          const ver = localStorage.getItem("jio_consent_version");
          if (cid && ver) {
            const latest = await loadConsentVersion(cid, ver);
            if (latest) currentConsentVersionData = latest;
          }
        }
      }
      
      openedFromFloatingIcon = false;

      if (currentModal && currentModal.overlayEl) currentModal.overlayEl.remove();
      const toastMap = languageSpecificContentMap[selectedLang] || {};
      showSavedToast(toastMap.toastSaved || "Your cookie preferences have been saved");
      };
    };
  }

 function showLanguagePicker(positionElement, onSelect) {
  const existing = document.getElementById("jio-lang-picker");
  if (existing) existing.remove();

  const rect = positionElement.getBoundingClientRect();
  const picker = el("div", "", {
    position: "absolute",
    top: `${rect.bottom + window.scrollY + 8}px`,
    left: `${Math.max(12, rect.left + window.scrollX)}px`,
    background: "#fff",
    border: "1px solid #E5E7EB",
    borderRadius: "8px",
    boxShadow: "0 8px 20px rgba(0,0,0,0.12)",
    zIndex: "200000",
    maxHeight: "320px",
    overflow: "auto",
    width: "220px",
    padding: "8px 8px"
  });

  picker.id = "jio-lang-picker";
  document.body.appendChild(picker);

  LANG_OPTIONS.forEach((opt) => {
    const item = el(
      "div",
      "",
      {
        padding: "8px 10px",
        cursor: "pointer",
        borderRadius: "6px",
        display: "flex",
        alignItems: "center",
        gap: "8px"
      },
      opt.label
    );

    if (opt.value === selectedLang) item.style.background = "#F3F4F6";

    item.onclick = async (e) => {
      e.stopPropagation();

      // ⭐ CRITICAL FIX: ensure callback fires BEFORE removing picker
      if (onSelect) {
        await onSelect(opt.value);
      }

      picker.remove();
      document.removeEventListener("click", handleDocClick);
    };

    picker.appendChild(item);
  });

  // Do NOT close instantly
  const handleDocClick = (ev) => {
    if (!picker.contains(ev.target) && !positionElement.contains(ev.target)) {
      picker.remove();
      document.removeEventListener("click", handleDocClick);
    }
  };

  // Attach after small delay
  setTimeout(() => {
    document.addEventListener("click", handleDocClick);
  }, 50);
}


    // banner lang button wiring
    langBtn.onclick = () => {
      showLanguagePicker(langBtn, async (newLang) => {
        if (!languageSpecificContentMap[newLang]) {
          await ensureLanguageLoaded(
            newLang,
            languageSpecificContentMap,
            preferences,
            languageSpecificContentMap.ENGLISH,
            tenantId,
            businessId,
            secure_code
          );
        }

        selectedLang = newLang;
        applyLanguageToBanner(selectedLang, languageSpecificContentMap);
        if (currentModal && currentModal.scrollArea) {
          buildModalContent(currentModal.scrollArea, selectedLang, languageSpecificContentMap, currentConsentVersionData);
        }
      });
    };

    // ensure selectedLang loaded (pass secure_code)
    if (!languageSpecificContentMap[selectedLang]) {
      await ensureLanguageLoaded(selectedLang, languageSpecificContentMap, preferences, languageSpecificContentMap.ENGLISH, tenantId, businessId, secure_code);
    }

    // apply text updates to banner
    function applyLanguageToBanner(langKey, languageSpecificContentMapLocal) {
      const map = (languageSpecificContentMapLocal && languageSpecificContentMapLocal[langKey]) || languageSpecificContentMap.ENGLISH;
      if (titleEl) titleEl.textContent = map.title || languageSpecificContentMap.ENGLISH.title || "This site uses cookies";
      if (descEl) descEl.textContent = map.description || languageSpecificContentMap.ENGLISH.description || "";
      if (manageBtn) manageBtn.textContent = map.managePrefs || languageSpecificContentMap.ENGLISH.managePrefs || "Manage preferences";
      if (rejectBtn) rejectBtn.textContent = map.rejectAll || languageSpecificContentMap.ENGLISH.rejectAll || "Reject all";
      if (acceptNecBtn) acceptNecBtn.textContent = map.acceptNecessary || languageSpecificContentMap.ENGLISH.acceptNecessary || "Accept necessary cookies";
      if (acceptAllBtn) acceptAllBtn.textContent = map.acceptAll || languageSpecificContentMap.ENGLISH.acceptAll || "Accept all";
      const lbls = document.querySelectorAll(".jio-lang-label"); lbls.forEach(l => l.textContent = langKey);
      localStorage.setItem("selected_lang", langKey);
    }

    // initial apply
    applyLanguageToBanner(selectedLang, languageSpecificContentMap);

    /* ------------------------------------------------------
       CHECK CONSENT STATUS BEFORE SHOWING BANNER
       - If ACTIVE: hide banner, show floating icon and load stored consent version (if available)
       - If No_Record: show banner normally
    ------------------------------------------------------- */
    const status = await checkConsentStatus();
    if (status && status.consentStatus === "ACTIVE") {
      // Save consentHandleId (if returned)
      if (status.consentHandleId && status.consentHandleId !== "No_Record") {
        localStorage.setItem("jio_consent_id", status.consentHandleId);
      }
      // If we have stored consent id and version, load the version to populate toggles later
      const cid = localStorage.getItem("jio_consent_id");
      const ver = localStorage.getItem("jio_consent_version");
      if (cid && ver) {
        const loaded = await loadConsentVersion(cid, ver);
        if (loaded) currentConsentVersionData = loaded;
      }
      // remove banner and show floating icon only
      if (banner && banner.parentNode) banner.remove();
      if (modalBackdrop && modalBackdrop.parentNode) modalBackdrop.remove();
      showFloatingIcon();
      // Return the API surface for outer code
      return {
        consentHandleId,
        deviceUUID: deviceuuid,
        close: () => { 
          if (banner && banner.parentNode) banner.remove(); 
          if (modalBackdrop && modalBackdrop.parentNode) modalBackdrop.remove();
        },
        openPreferences: () => manageBtn.click(),
      };
    }

    // If No_Record — keep banner, normal flow.

    // return a small API to outer world for debugging or programmatic control
    return {
      consentHandleId,
      deviceUUID: deviceuuid,
      close: () => { 
        if (banner && banner.parentNode) banner.remove(); 
        if (modalBackdrop && modalBackdrop.parentNode) modalBackdrop.remove();
      },
      openPreferences: () => manageBtn.click(),
    };

  } catch (err) {
    console.error("cookie center init failed", err);
    return null;
  }
}

// (no DOMContentLoaded auto-run) — caller should call loadCookieCenter with proper args
