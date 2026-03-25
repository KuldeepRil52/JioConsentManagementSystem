import React, { useState, useRef, useMemo, useEffect, useCallback } from "react";
import "../styles/cookieTab.css";
import {
  ActionButton,
  Text,
  Button,
  InputFieldV2,
  TabItem,
  Tabs,
  InputToggle,
} from "../custom-components";
import axios from "axios";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate, useSearchParams, useLocation } from "react-router-dom";
import ReportContainer from "./ReportContainer";
import LanguageTab from "./LanguageTab";
import BrandingTab from "./BrandingTab";
import { Slide, ToastContainer, toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import "../styles/toast.css";
import CustomToast from "./CustomToastContainer";
import ManagePreferencesModal from "./ManagePreferencesModal.js";
import AccManagePreviewRight from "./AccManagePreviewRight.js";
import config from "../utils/config";
import { formatToIST } from "../utils/dateUtils";

/* ===========================
   Default branding color schemes
   =========================== */
const DEFAULT_LIGHT_COLORS = {
  cardBackground: "#FFFFFF",
  cardFont: "#111827",
  buttonBackground: "#0F3CC9",
  buttonFont: "#FFFFFF",
  linkFont: "#0A2885",
};

const DEFAULT_DARK_COLORS = {
  cardBackground: "#071026",
  cardFont: "#E6EEF5",
  buttonBackground: "#0F3CC9",
  buttonFont: "#FFFFFF",
  linkFont: "#6AB0FF",
};

/* ===========================
   API endpoints & helpers
   =========================== */
const BASE = config.cookie_base;
const STATUS_URL = (txId) => `${BASE}/status/${encodeURIComponent(txId)}`;
const START_SCAN_URL = `${BASE}/scan`;
const CREATE_TEMPLATE_URL = `${BASE}/cookie-templates`;

/* ===========================
   Error helpers – pass through the actual API message
   =========================== */
function mapError(err) {
  if (!err) return "Something went wrong.";

  const msg = err?.message || err?.toString() || "";

  // The API returns JSON with { errorCode, message, details }.
  // Show `details` first (most specific), then `message`.
  try {
    const parsed = JSON.parse(msg);
    return parsed?.details || parsed?.message || msg;
  } catch { }

  // Non-JSON error strings (timeout / network)
  if (err.name === "AbortError" || msg.includes("abort"))
    return "Request timed out. Please try again.";
  if (msg.includes("NetworkError") || msg.includes("Failed to fetch"))
    return "Network error. Please check your connection.";

  return msg || "Something went wrong.";
}

function parseCookieFieldErrors(err) {
  const result = {
    name: "",
    category: "",
    domainUrl: "",
    description: "",
    privacyPolicyUrl: "",
    expiresOn: "",
    global: "",
  };

  const raw = err?.message || err?.toString() || "";

  // Try to extract the API's own message / details
  try {
    const parsed = JSON.parse(raw);
    result.global = parsed?.details || parsed?.message || raw;
  } catch {
    result.global = raw;
  }

  // Highlight the relevant form field based on keywords in the API message
  const lower = result.global.toLowerCase();
  if (lower.includes("category")) result.category = result.global;
  if (lower.includes("name"))     result.name = result.global;
  if (lower.includes("domain"))   result.domainUrl = result.global;
  if (lower.includes("description")) result.description = result.global;
  if (lower.includes("privacy") || lower.includes("policy"))
    result.privacyPolicyUrl = result.global;
  if (lower.includes("expire"))   result.expiresOn = result.global;

  return result;
}

async function safeFetchJson(url, opts = {}, timeoutMs = 15000) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const headers = {
      ...(opts.headers || {}),
      "Content-Type": "application/json",
    };

    if (opts.tenantId) headers["X-Tenant-ID"] = opts.tenantId;
    if (opts.token) headers["x-session-token"] = opts.token;

    const resp = await fetch(url, { ...opts, headers, signal: controller.signal });
    clearTimeout(timer);

    if (!resp.ok) {
      const rawText = await resp.text().catch(() => "");
      throw new Error(rawText || `HTTP ${resp.status}`);
    }

    const text = await resp.text().catch(() => "");
    return text ? JSON.parse(text) : {};
  } catch (err) {
    clearTimeout(timer);
    throw err;
  }
}

function normalizeResponse(serverData) {
  if (!serverData) return null;

  const categorized = {
    required: {},
    functional: [],
    advertising: [],
    analytics: [],
    unclassified: [],
  };

  // Dynamic categories: holds cookies whose category doesn't match the 5 standard ones
  const dynamicCategories = {};

  const cookiesArr = Array.isArray(serverData.cookies)
    ? serverData.cookies
    : Array.isArray(serverData.subdomains)
      ? serverData.subdomains.flatMap((sd) => (Array.isArray(sd.cookies) ? sd.cookies : []))
      : [];

  cookiesArr.forEach((c) => {
    if (!c || !c.name) return;

    const rawCat = (c.category || "").toString().toLowerCase().trim();
    let key = null;

    if (/required|necessary|essential/i.test(rawCat)) key = "required";
    else if (/functional/i.test(rawCat)) key = "functional";
    else if (/advertis|marketing|tracking/i.test(rawCat)) key = "advertising";
    else if (/analytic|ga|google|mixpanel|segment/i.test(rawCat)) key = "analytics";
    else if (/^others$|^unclassified$/i.test(rawCat)) key = "unclassified";

    const vendor = c.vendor || c.provider || c.source || c.domain || "Unknown";
    const item = {
      name: c.name,
      domain: c.domain || c.host || c.url || "",
      description: c.description || c.description_gpt || "",
      expires: c.expires ?? null,
      raw: c,
      category: c.category,
    };

    if (key === "required") {
      categorized.required[vendor] = categorized.required[vendor] || [];
      categorized.required[vendor].push(item);
    } else if (key) {
      categorized[key].push(item);
    } else if (rawCat) {
      // Dynamic / custom category — preserve under its original name
      const label = (c.category || rawCat).trim();
      if (!dynamicCategories[label]) dynamicCategories[label] = [];
      dynamicCategories[label].push(item);
    } else {
      categorized.unclassified.push(item);
    }
  });

  return {
    status: (serverData.status || "UNKNOWN").toString(),
    transactionId: serverData.transactionId || serverData.txId || serverData.id || null,
    url: serverData.url || serverData.request?.url || null,
    totalCookies: serverData.totalCookies ?? (Array.isArray(cookiesArr) ? cookiesArr.length : 0),
    lastScanned: serverData.lastScanned || serverData.updatedAt || serverData.scannedAt || null,
    subdomains: serverData.subdomains || serverData.subDomain || serverData.subdomain || [],
    dynamicCategories,
    ...categorized,
  };
}

function formatTimestamp(ts) {
  return formatToIST(ts);
}

const AccPreviewRight = ({
  tab = "semantics",
  colors = {},
  darkColors = {},
  darkMode = false,
  previewContent = {},
  previewButtons = [],
}) => {
  const [brandingColors, setBrandingColors] = useState(colors);
  const [brandingDarkColors, setBrandingDarkColors] = useState(darkColors);
  const [isDark, setIsDark] = useState(darkMode);

  useEffect(() => {
    try {
      const storedColors = JSON.parse(sessionStorage.getItem("brandingColors"));
      const storedDark = JSON.parse(sessionStorage.getItem("brandingDarkColors"));
      const storedDarkMode = JSON.parse(sessionStorage.getItem("brandingDarkMode"));
      if (storedColors) setBrandingColors(storedColors);
      if (storedDark) setBrandingDarkColors(storedDark);
      if (typeof storedDarkMode === "boolean") setIsDark(storedDarkMode);
    } catch (e) {
      console.warn("Accessibility Preview: failed to load branding theme", e);
    }
  }, [colors, darkColors, darkMode]);

  const active = isDark ? brandingDarkColors : brandingColors;
  const cardBg = active.cardBackground || (isDark ? "#071026" : "#FFFFFF");
  const cardFont = active.cardFont || (isDark ? "#E6EEF5" : "#111827");
  const btnBg = active.buttonBackground || "#0F3CC9";
  const btnFont = active.buttonFont || "#FFFFFF";
  const linkFont = active.linkFont || "#0A2885";

  const getButtonStyle = (b) => {
    const base = {
      padding: "8px 14px",
      borderRadius: 18,
      fontWeight: 700,
      fontSize: 13,
      cursor: "pointer",
      minWidth: 120,
      transition: "all 0.2s ease",
    };

    if (b.variant === "primary") return { ...base, background: btnBg, color: btnFont, border: "none" };
    if (b.variant === "outline") return { ...base, background: "transparent", color: linkFont, border: `1px solid ${linkFont}` };
    if (b.variant === "ghost") return { ...base, background: "transparent", color: linkFont, border: `1px solid ${linkFont}` };
    return { ...base, background: "transparent", color: cardFont, border: "1px solid #E5E7EB" };
  };

  const semanticsBadge = {
    background: "#9333ea",
    color: "#fff",
    fontSize: 11,
    fontWeight: 700,
    borderRadius: 4,
    padding: "2px 8px",
    marginRight: 8,
  };

  const keyboardBadge = {
    background: "#7B1F3A",
    color: "#fff",
    fontSize: 10,
    fontWeight: 700,
    borderRadius: 4,
    padding: "2px 6px",
    display: "inline-flex",
    alignItems: "center",
    gap: 2,
  };

  const screenReaderBadge = {
    background: "#7B5C00",
    color: "#fff",
    fontSize: 10,
    fontWeight: 700,
    borderRadius: 4,
    padding: "2px 6px",
    display: "inline-flex",
    alignItems: "center",
    gap: 2,
  };

  const greenBadgeStyle = {
    background: "#166534",
    color: "#fff",
    fontSize: 10,
    fontWeight: 700,
    borderRadius: 4,
    padding: "2px 6px",
  };

  const fieldsetBadge = {
    background: "#166534",
    color: "#fff",
    fontSize: 10,
    fontWeight: 700,
    borderRadius: 4,
    padding: "2px 6px",
  };

  const commonWrapperStyle = {
    padding: 16,
    borderRadius: 12,
    background: cardBg,
    color: cardFont,
    boxShadow: "0 4px 12px rgba(0,0,0,0.08)",
    border:
      tab === "semantics"
        ? "2px dashed #9333ea"
        : tab === "focus"
          ? "2px dashed #7B1F3A"
          : "2px dashed #7B5C00",
  };

  const renderBadge = (type, number) => {
    if (tab === "semantics") return <span style={semanticsBadge}>{type}</span>;
    if (tab === "focus") return <span style={keyboardBadge}>Tab<br />→ #{number}</span>;
    if (tab === "screen") return <span style={screenReaderBadge}>🔊 #{number}</span>;
    return null;
  };

  const renderButtonBadge = (idx) => {
    if (tab === "semantics") return <span style={greenBadgeStyle}>{idx + 1} ⊙ Button</span>;
    if (tab === "focus") return <span style={keyboardBadge}>Tab<br />→ #{idx + 3}</span>;
    if (tab === "screen") return <span style={screenReaderBadge}>🔊 #{idx + 3}</span>;
    return null;
  };

  return (
    <div style={{ padding: 12 }}>
      {tab === "semantics" && (
        <div style={{ marginBottom: 8, display: "flex", alignItems: "center", gap: 8 }}>
          <span style={{ ...semanticsBadge, background: "#9333ea" }}>1 🏠</span>
          <span style={{ fontSize: 12, color: "#6B7280" }}>Dialog Container</span>
        </div>
      )}

      <div style={commonWrapperStyle}>
        <div style={{ display: "flex", alignItems: "flex-start", gap: 8, marginBottom: 12 }}>
          {renderBadge("h2", 1)}
          <div style={{ fontWeight: 800, fontSize: 15, color: cardFont }}>
            {previewContent.title || "This site uses cookies to make your experience better"}
          </div>
        </div>

        <div style={{ display: "flex", alignItems: "flex-start", gap: 8, marginBottom: 16 }}>
          {renderBadge("p", 2)}
          <div style={{ fontSize: 13, color: isDark ? "#FFFFFF" : cardFont, lineHeight: 1.5 }}>
            {previewContent.description ||
              'We use essential cookies to make our site work. With your consent, we may also use non-essential cookies to improve user experience and analyze website traffic. By clicking "Accept", you agree to our website\'s cookie use as described in our cookie policy. You can change your cookie settings at any time by clicking "Preferences"'}
          </div>
        </div>

        <div
          style={{
            border: tab === "semantics" ? "2px dashed #166534" : "none",
            borderRadius: 8,
            padding: tab === "semantics" ? 12 : 0,
            marginTop: 8,
            position: "relative",
          }}
        >
          {tab === "semantics" && (
            <div style={{ position: "absolute", top: -10, left: 12, background: cardBg, padding: "0 4px" }}>
              <span style={fieldsetBadge}>1 📋 Fieldset</span>
            </div>
          )}

          <div style={{ display: "flex", flexWrap: "wrap", gap: 10, alignItems: "flex-end", marginTop: tab === "semantics" ? 8 : 0 }}>
            {previewButtons.map((b, idx) => (
              <div key={b.id} style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 4 }}>
                <button style={getButtonStyle(b)}>{b.label}</button>
                {renderButtonBadge(idx)}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

const AccessibilityPanel = ({ accActiveTab, setAccActiveTab, previewContent, previewButtons }) => {
  return (
    <div style={{ padding: 12 }}>
      <div style={{ marginTop: "20px", padding: "15px 20px" }}>
        <Text appearance="heading-xxs" color="primary-grey-80">Element properties</Text>
        <br />
        <Text appearance="body-xs" color="primary-grey-80">
          Accessibility properties of elements and preview how they will be experienced by the user
        </Text>
      </div>

      <div className="tabs-container" style={{ padding: 12 }}>
        <div className="tabs" style={{ display: "flex", gap: 8, marginBottom: 12 }}>
          <button className={`tab-btn ${accActiveTab === "semantics" ? "active" : ""}`} onClick={() => setAccActiveTab("semantics")}>HTML semantics</button>
          <button className={`tab-btn ${accActiveTab === "focus" ? "active" : ""}`} onClick={() => setAccActiveTab("focus")}>Keyboard focus order</button>
          <button className={`tab-btn ${accActiveTab === "screen" ? "active" : ""}`} onClick={() => setAccActiveTab("screen")}>Screen reader reading order</button>
        </div>

        <div className="tab-content" style={{ marginTop: 8 }}>
          {accActiveTab === "semantics" && (
            <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
              <div className="landmark-card">
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  <div className="landmark-badge"><span className="landmark-number">1</span></div>
                  <Text appearance="body-xs-bold" color="primary-grey-100">Banner Container</Text>
                </div>
                <div className="landmark-body">
                  <Text appearance="body-s" color="primary-grey-100">element</Text>
                  <span className="landmark-tag">&lt;div role="dialog"&gt;</span>
                  <div style={{ marginTop: 8, fontSize: 13, color: "#6B7280" }}>Cookie consent banner</div>
                </div>
              </div>

              <div className="landmark-card-3">
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  <div className="landmark-badge-3"><span className="landmark-number">h2</span></div>
                  <Text appearance="body-xs-bold" color="primary-grey-100">Heading &lt;h2&gt;</Text>
                </div>
                <div className="landmark-body" style={{ borderBottom: "1px solid #e0e0e0" }}>
                  <Text appearance="body-s" color="primary-grey-100">element</Text>
                  <span className="landmark-tag">&lt;h2&gt;</span>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between", padding: 6 }}>
                  <Text appearance="body-xs" color="primary-grey-80">accessible label</Text>
                  <Text appearance="body-xxs" color="primary-grey-80">{previewContent.title || "This site uses cookies to make your experience better"}</Text>
                </div>
              </div>

              <div className="landmark-card-3">
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  <div className="landmark-badge-3"><span className="landmark-number">p</span></div>
                  <Text appearance="body-xs-bold" color="primary-grey-100">Paragraph &lt;p&gt;</Text>
                </div>
                <div className="landmark-body" style={{ borderBottom: "1px solid #e0e0e0" }}>
                  <Text appearance="body-s" color="primary-grey-100">element</Text>
                  <span className="landmark-tag">&lt;p&gt;</span>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between", padding: 6 }}>
                  <Text appearance="body-xs" color="primary-grey-80">accessible label</Text>
                  <Text appearance="body-xxs" color="primary-grey-80">{previewContent.description}</Text>
                </div>
              </div>

              <div className="landmark-card-5">
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  <div className="landmark-badge-5"><span className="landmark-number">1</span></div>
                  <Text appearance="body-xs-bold" color="primary-grey-100">Fieldset</Text>
                </div>
                <div className="landmark-body" style={{ borderBottom: "1px solid #e0e0e0" }}>
                  <Text appearance="body-s" color="primary-grey-100">element</Text>
                  <span className="landmark-tag">&lt;fieldset&gt;</span>
                </div>
                <div style={{ padding: 6 }}>
                  <Text appearance="body-xs" color="primary-grey-80">description</Text>
                  <div style={{ marginTop: 6 }}>
                    <Text appearance="body-xxs" color="primary-grey-80">Button group for cookie consent actions</Text>
                  </div>
                </div>
              </div>

              {previewButtons.map((b, idx) => (
                <div key={b.id} className="landmark-card-6">
                  <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                    <div className="landmark-badge-6"><span className="landmark-number">{idx + 1}</span></div>
                    <Text appearance="body-xs-bold" color="primary-grey-100">Button</Text>
                  </div>
                  <div className="landmark-body" style={{ borderBottom: "1px solid #e0e0e0" }}>
                    <Text appearance="body-s" color="primary-grey-100">element</Text>
                    <span className="landmark-tag">&lt;button&gt;</span>
                  </div>
                  <div style={{ display: "flex", justifyContent: "space-between", padding: 6 }}>
                    <Text appearance="body-xs" color="primary-grey-80">accessible name</Text>
                    <Text appearance="body-xxs" color="primary-grey-80">{b.label}</Text>
                  </div>
                </div>
              ))}
            </div>
          )}

          {accActiveTab === "focus" && (
            <div style={{ background: "#fef7f7", border: "1px solid #e8d4d4", borderRadius: 12, padding: 20, marginTop: 8 }}>
              <div style={{ display: "flex", alignItems: "flex-start", gap: 12 }}>
                <div style={{ width: 32, height: 32, background: "#7B1F3A", borderRadius: 6, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                  <span style={{ color: "#fff", fontSize: 16 }}>📋</span>
                </div>
                <div>
                  <Text appearance="body-m-bold" color="primary-grey-100" style={{ marginBottom: 12, display: "block" }}>Focus Order Details</Text>
                  <ul style={{ margin: 0, paddingLeft: 20, color: "#374151" }}>
                    <li style={{ marginBottom: 8, lineHeight: 1.5 }}>
                      <Text appearance="body-s" color="primary-grey-80">When the user is navigating with a keyboard using the tab key, the focus will be on elements that are interactible - eg. buttons, links, checkboxes, etc.</Text>
                    </li>
                    <li style={{ lineHeight: 1.5 }}>
                      <Text appearance="body-s" color="primary-grey-80">They will be taken through these elements in the order listed on the preview screen</Text>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          )}

          {accActiveTab === "screen" && (
            <div style={{ background: "#fefcf3", border: "1px solid #e8e0c4", borderRadius: 12, padding: 20, marginTop: 8 }}>
              <div style={{ display: "flex", alignItems: "flex-start", gap: 12 }}>
                <div style={{ width: 32, height: 32, background: "#7B5C00", borderRadius: 6, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                  <span style={{ color: "#fff", fontSize: 16 }}>📋</span>
                </div>
                <div>
                  <Text appearance="body-m-bold" color="primary-grey-100" style={{ marginBottom: 12, display: "block" }}>Reading Order Details</Text>
                  <ul style={{ margin: 0, paddingLeft: 20, color: "#374151" }}>
                    <li style={{ marginBottom: 8, lineHeight: 1.5 }}>
                      <Text appearance="body-s" color="primary-grey-80">When user is navigating using a screen reader, it will be reading the content of the screen in the order listed on the preview.</Text>
                    </li>
                    <li style={{ lineHeight: 1.5 }}>
                      <Text appearance="body-s" color="primary-grey-80">The screen reader will be reading out the accessible names, and ARIA labels as mentioned on the HTML semantics page for each element.</Text>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

const AccessibilityPanelPreferences = ({ accActiveTab, setAccActiveTab }) => {
  const cookieCategories = [
    { id: "required", name: "Required Cookies", vendors: ["Meta Platforms, Inc", "Facebook, Inc"] },
    { id: "functional", name: "Functional Cookies", vendors: ["Google Analytics"] },
    { id: "advertising", name: "Advertising Cookies", vendors: ["Google Ads"] },
    { id: "analytics", name: "Analytics and customization", vendors: [] },
  ];

  return (
    <div style={{ padding: 12 }}>
      <div style={{ marginTop: "20px", padding: "15px 20px" }}>
        <Text appearance="heading-xxs" color="primary-grey-80">Element properties</Text>
        <br />
        <Text appearance="body-xs" color="primary-grey-80">
          Accessibility properties of elements inside the Manage Preferences modal and how users experience them.
        </Text>
      </div>

      <div className="tabs-container" style={{ padding: 12 }}>
        <div className="tabs" style={{ display: "flex", gap: 8, marginBottom: 12 }}>
          <button className={`tab-btn ${accActiveTab === "semantics" ? "active" : ""}`} onClick={() => setAccActiveTab("semantics")}>HTML semantics</button>
          <button className={`tab-btn ${accActiveTab === "focus" ? "active" : ""}`} onClick={() => setAccActiveTab("focus")}>Keyboard focus order</button>
          <button className={`tab-btn ${accActiveTab === "screen" ? "active" : ""}`} onClick={() => setAccActiveTab("screen")}>Screen reader reading order</button>
        </div>

        <div className="tab-content" style={{ marginTop: 8, maxHeight: 500, overflowY: "auto" }}>
          {accActiveTab === "semantics" && (
            <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
              <div className="landmark-card">
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  <div className="landmark-badge"><span className="landmark-number">1</span></div>
                  <Text appearance="body-xs-bold" color="primary-grey-100">Modal Container</Text>
                </div>
                <div className="landmark-body">
                  <Text appearance="body-s" color="primary-grey-100">element</Text>
                  <span className="landmark-tag">&lt;div role="dialog"&gt;</span>
                  <div style={{ marginTop: 8, fontSize: 13, color: "#6B7280" }}>Cookie preferences dialog</div>
                </div>
              </div>

              <div className="landmark-card-6">
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  <div className="landmark-badge-6"><span className="landmark-number">1</span></div>
                  <Text appearance="body-xs-bold" color="primary-grey-100">Close Button</Text>
                </div>
                <div className="landmark-body" style={{ borderBottom: "1px solid #e0e0e0" }}>
                  <Text appearance="body-s" color="primary-grey-100">element</Text>
                  <span className="landmark-tag">&lt;button aria-label="Close"&gt;</span>
                </div>
              </div>

              <div className="landmark-card-3">
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  <div className="landmark-badge-3"><span className="landmark-number">h2</span></div>
                  <Text appearance="body-xs-bold" color="primary-grey-100">Modal Title &lt;h2&gt;</Text>
                </div>
                <div className="landmark-body" style={{ borderBottom: "1px solid #e0e0e0" }}>
                  <Text appearance="body-s" color="primary-grey-100">element</Text>
                  <span className="landmark-tag">&lt;h2&gt;</span>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between", padding: 6 }}>
                  <Text appearance="body-xs" color="primary-grey-80">accessible label</Text>
                  <Text appearance="body-xxs" color="primary-grey-80">Manage preferences</Text>
                </div>
              </div>

              <div className="landmark-card" style={{ background: "#f8f9fa", border: "1px solid #e9ecef" }}>
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  <div style={{ background: "#6c757d", color: "#fff", fontSize: 10, fontWeight: 700, borderRadius: 4, padding: "2px 6px" }}>Note</div>
                  <Text appearance="body-xs-bold" color="primary-grey-100">Description Note</Text>
                </div>
                <div className="landmark-body">
                  <Text appearance="body-xxs" color="primary-grey-80">Choose the cookies you allow. You can update your preferences anytime.</Text>
                </div>
              </div>

              {cookieCategories.map((cat, catIdx) => (
                <React.Fragment key={cat.id}>
                  <div className="landmark-card-3">
                    <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                      <div className="landmark-badge-3"><span className="landmark-number">h3</span></div>
                      <Text appearance="body-xs-bold" color="primary-grey-100">{cat.name} &lt;h3&gt;</Text>
                    </div>
                    <div className="landmark-body" style={{ borderBottom: "1px solid #e0e0e0" }}>
                      <Text appearance="body-s" color="primary-grey-100">element</Text>
                      <span className="landmark-tag">&lt;h3&gt;</span>
                    </div>
                    <div style={{ display: "flex", justifyContent: "space-between", padding: 6 }}>
                      <Text appearance="body-xs" color="primary-grey-80">accessible label</Text>
                      <Text appearance="body-xxs" color="primary-grey-80">{cat.name}</Text>
                    </div>
                  </div>

                  <div className="landmark-card-5">
                    <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                      <div className="landmark-badge-5"><span className="landmark-number">{catIdx + 1}</span></div>
                      <Text appearance="body-xs-bold" color="primary-grey-100">Fieldset</Text>
                    </div>
                    <div className="landmark-body" style={{ borderBottom: "1px solid #e0e0e0" }}>
                      <Text appearance="body-s" color="primary-grey-100">element</Text>
                      <span className="landmark-tag">&lt;fieldset&gt; with &lt;input type="checkbox"&gt;</span>
                    </div>
                    <div style={{ padding: 6 }}>
                      <Text appearance="body-xxs" color="primary-grey-80">Toggle for {cat.name.toLowerCase()}</Text>
                    </div>
                  </div>
                </React.Fragment>
              ))}
            </div>
          )}

          {accActiveTab === "focus" && (
            <div style={{ background: "#fef7f7", border: "1px solid #e8d4d4", borderRadius: 12, padding: 20, marginTop: 8 }}>
              <div style={{ display: "flex", alignItems: "flex-start", gap: 12 }}>
                <div style={{ width: 32, height: 32, background: "#7B1F3A", borderRadius: 6, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                  <span style={{ color: "#fff", fontSize: 16 }}>📋</span>
                </div>
                <div>
                  <Text appearance="body-m-bold" color="primary-grey-100" style={{ marginBottom: 12, display: "block" }}>Focus Order Details</Text>
                  <ul style={{ margin: 0, paddingLeft: 20, color: "#374151" }}>
                    <li style={{ marginBottom: 8, lineHeight: 1.5 }}>
                      <Text appearance="body-s" color="primary-grey-80">When the user is navigating with a keyboard using the tab key, the focus will be on elements that are interactible - eg. buttons, links, checkboxes, etc.</Text>
                    </li>
                    <li style={{ lineHeight: 1.5 }}>
                      <Text appearance="body-s" color="primary-grey-80">They will be taken through these elements in the order listed on the preview screen</Text>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          )}

          {accActiveTab === "screen" && (
            <div style={{ background: "#fefcf3", border: "1px solid #e8e0c4", borderRadius: 12, padding: 20, marginTop: 8 }}>
              <div style={{ display: "flex", alignItems: "flex-start", gap: 12 }}>
                <div style={{ width: 32, height: 32, background: "#7B5C00", borderRadius: 6, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                  <span style={{ color: "#fff", fontSize: 16 }}>📋</span>
                </div>
                <div>
                  <Text appearance="body-m-bold" color="primary-grey-100" style={{ marginBottom: 12, display: "block" }}>Reading Order Details</Text>
                  <ul style={{ margin: 0, paddingLeft: 20, color: "#374151" }}>
                    <li style={{ marginBottom: 8, lineHeight: 1.5 }}>
                      <Text appearance="body-s" color="primary-grey-80">When user is navigating using a screen reader, it will be reading the content of the screen in the order listed on the preview.</Text>
                    </li>
                    <li style={{ lineHeight: 1.5 }}>
                      <Text appearance="body-s" color="primary-grey-80">The screen reader will be reading out the accessible names, and ARIA labels as mentioned on the HTML semantics page for each element.</Text>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

function AddEditCookieModal({
  open,
  onClose,
  mode = "add",
  formState,
  setFormState,
  onAdd,
  onSave,
  loading = false,
  categories = [],
  formErrors = {},
  clearFieldError,
}) {
  useEffect(() => {
    if (open) document.body.style.overflow = "hidden";
    else document.body.style.overflow = "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [open]);

  if (!open) return null;

  return (
    <>
      <div className="modal-overlay" onClick={onClose} />

      <div
        className="modal-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="add-cookie-title"
        onClick={(e) => e.stopPropagation()}
      >
        <button className="modal-close" aria-label="Close" onClick={onClose}>×</button>

        <div className="modal-card">
          <div className="modal-header">
            <Text id="add-cookie-title" appearance="heading-s" color="primary-grey-100">
              {mode === "add" ? "Add cookie" : "Edit cookie"}
            </Text>
            <Text appearance="body-s" color="primary-grey-80">
              Add cookie details. Fields marked (Required) must be filled.
            </Text>
          </div>

          <div className="modal-body">
            <div>
              <InputFieldV2
                label="Cookie name (Required)"
                placeholder="Enter cookie name"
                value={formState.name}
                onChange={(e) => {
                  setFormState((p) => ({ ...p, name: e.target.value }));
                  clearFieldError?.("name");
                }}
                required
                size="medium"
                disabled={mode === "edit"}
              />
              {formErrors.name && <div style={{ color: "#DC2626", fontSize: 12, marginTop: 4 }}>{formErrors.name}</div>}
            </div>

            <div>
              <label style={{ fontWeight: 600, fontSize: 13, marginBottom: 4, display: "block" }}>
                Select cookie category (Required)*
              </label>
              <select
                value={formState.category}
                onChange={(e) => {
                  setFormState((p) => ({ ...p, category: e.target.value }));
                  clearFieldError?.("category");
                }}
                required
                style={{
                  width: "100%",
                  padding: "10px 12px",
                  borderRadius: 6,
                  border: formErrors.category ? "1px solid #DC2626" : "1px solid #e5e7eb",
                  fontSize: 14,
                  marginBottom: 4,
                }}
              >
                <option value="">-- select --</option>
                {Array.isArray(categories) &&
                  categories.map((cat) => (
                    <option key={cat.categoryId} value={cat.category}>
                      {cat.category}
                    </option>
                  ))}
              </select>
              {formErrors.category && <div style={{ color: "#DC2626", fontSize: 12 }}>{formErrors.category}</div>}
            </div>

            <div>
              <InputFieldV2
                label="Domain URL (Required)*"
                placeholder="example.com"
                value={formState.domainUrl}
                onChange={(e) => {
                  setFormState((p) => ({ ...p, domainUrl: e.target.value }));
                  clearFieldError?.("domainUrl");
                }}
                required
                size="medium"
                disabled={mode === "edit"}
              />
              {formErrors.domainUrl && <div style={{ color: "#DC2626", fontSize: 12, marginTop: 4 }}>{formErrors.domainUrl}</div>}
            </div>

            <div>
              <InputFieldV2
                label="Description (Required)*"
                placeholder="Describe cookie usage"
                value={formState.description}
                onChange={(e) => {
                  setFormState((p) => ({ ...p, description: e.target.value }));
                  clearFieldError?.("description");
                }}
                size="medium"
                as="textarea"
              />
              {formErrors.description && <div style={{ color: "#DC2626", fontSize: 12, marginTop: 4 }}>{formErrors.description}</div>}
            </div>

            <div>
              <InputFieldV2
                label="Privacy policy URL"
                placeholder="https://example.com/privacy-policy"
                value={formState.privacyPolicyUrl}
                onChange={(e) => {
                  setFormState((p) => ({ ...p, privacyPolicyUrl: e.target.value }));
                  clearFieldError?.("privacyPolicyUrl");
                }}
                size="medium"
              />
              {formErrors.privacyPolicyUrl && (
                <div style={{ color: "#DC2626", fontSize: 12, marginTop: 4 }}>{formErrors.privacyPolicyUrl}</div>
              )}
            </div>

            <div>
              <label style={{ fontSize: 12, fontWeight: 700, marginTop: 6, display: "block" }}>Expires on</label>
              <input
                type="date"
                value={formState.expiresOn || ""}
                onChange={(e) => {
                  setFormState((p) => ({ ...p, expiresOn: e.target.value }));
                  clearFieldError?.("expiresOn");
                }}
                style={{
                  width: "100%",
                  padding: 10,
                  marginTop: 6,
                  borderRadius: 6,
                  border: formErrors.expiresOn ? "1px solid #DC2626" : "1px solid #e5e7eb",
                }}
              />
              {formErrors.expiresOn && <div style={{ color: "#DC2626", fontSize: 12, marginTop: 4 }}>{formErrors.expiresOn}</div>}
            </div>
          </div>

          <div className="modal-footer">
            <Button kind="secondary" size="medium" onClick={onClose}>Cancel</Button>
            {mode === "add" ? (
              <Button kind="primary" size="medium" onClick={onAdd} disabled={loading}>
                {loading ? "Adding..." : "Add"}
              </Button>
            ) : (
              <Button kind="primary" size="medium" onClick={onSave} disabled={loading}>
                {loading ? "Saving..." : "Save"}
              </Button>
            )}
          </div>
        </div>
      </div>

      <style>{`
        .modal-overlay {
          position: fixed;
          inset: 0;
          background: rgba(0,0,0,0.45);
          z-index: 1200;
        }
        .modal-dialog {
          position: fixed;
          inset: 0;
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 24px;
          z-index: 1201;
        }
        .modal-close {
          position: absolute;
          top: 20px;
          right: 28px;
          border: none;
          background: transparent;
          font-size: 22px;
          line-height: 1;
          cursor: pointer;
          color: #0A3CCE;
          z-index: 1203;
        }
        .modal-card {
          width: 784px;
          max-width: calc(100% - 48px);
          max-height: calc(100vh - 80px);
          border-radius: 32px;
          background: #fff;
          display: flex;
          flex-direction: column;
          gap: 18px;
          padding: 24px;
          box-shadow: 0 12px 40px rgba(0,0,0,0.18);
          font-family: JioType, system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial;
          overflow: hidden;
        }
        .modal-header {
          display: flex;
          flex-direction: column;
          gap: 8px;
          flex-shrink: 0;
        }
        .modal-body {
          flex: 1;
          overflow-y: auto;
          display: grid;
          grid-template-columns: 1fr;
          gap: 12px;
          padding-right: 4px;
        }
        .modal-footer {
          display: flex;
          justify-content: flex-end;
          gap: 12px;
          margin-top: 6px;
          flex-shrink: 0;
        }
        .modal-body::-webkit-scrollbar { width: 6px; }
        .modal-body::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 4px; }
        @media (max-width: 640px) {
          .modal-card { width: 100%; border-radius: 16px; max-height: calc(100vh - 40px); }
          .modal-footer { flex-direction: column-reverse; }
          .modal-footer button { width: 100%; }
        }
      `}</style>
    </>
  );
}

const Cookie = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [sp] = useSearchParams();
  const location = useLocation();

  const isReportPage = location.pathname.startsWith("/registercookies/report");

  const tenant_id = useSelector((state) => state.common.tenant_id);
  const token = useSelector((state) => state.common.session_token);
  const businessIdPrimary = useSelector((state) => state.common.businessId);
  const businessIdFallback = useSelector((state) => state.common.business_id);
  const businessId = businessIdPrimary || businessIdFallback || sessionStorage.getItem("business_id");

  const [categories, setCategories] = useState([]);
  const cookietitle = sp.get("url") || "www.example.com";
  const txIdParam = sp.get("txId") || sp.get("id") || null;
  const templateIdParam = sp.get("templateId") || null;

  const [prefColors, setPrefColors] = useState({ ...DEFAULT_LIGHT_COLORS });
  const [prefDarkColors, setPrefDarkColors] = useState({ ...DEFAULT_DARK_COLORS });
  const [prefDarkMode, setPrefDarkMode] = useState(false);
  const [publishLoading, setPublishLoading] = useState(false);

  const [templateName, setTemplateName] = useState("");
  const [title, setTitle] = useState("This site uses cookies to make your experience better");
  const [text, setText] = useState(
    "We use essential cookies to make our site work. With your consent, we may also use non-essential cookies to improve user experience and analyze website traffic. By clicking “Accept”, you agree to our website’s cookie use as described in our cookie policy. You can change your cookie settings at any time by clicking “Preferences”"
  );
  const [rights, setRights] = useState("To withdraw your consent, exercise your rights, or file complaints with the Board click here.");
  const [permission, setPermission] = useState("By clicking ‘Allow all’ or ’Save my choices’, you are providing your consent to Reliance Medlab using your data as outlined above.");
  const [supportedLanguages, setSupportedLanguages] = useState([]);
  const [language, setLanguage] = useState("ENGLISH");
  const [label, setLabel] = useState("Required");
  const [languageSpecificContentMap, setLanguageSpecificContentMap] = useState({
    ENGLISH: {
      description: text,
      label: "Required",
      rightsText: rights,
      permissionText: permission,
      title,
      managePrefs: "Manage preferences",
      rejectAll: "Reject all",
      acceptNecessary: "Accept necessary cookies",
      acceptAll: "Accept all",
    },
  });

  const fileInputRef = useRef(null);
  const logoInputRef = useRef(null);
  const fontInputRef = useRef(null);
  const [fileName, setFileName] = useState("");
  const [certificatePreviewUrl, setCertificatePreviewUrl] = useState("");
  const [fileBase64, setFileBase64] = useState(null);
  const [logoPreviewUrl, setLogoPreviewUrl] = useState("");
  const [logoName, setLogoName] = useState("");
  const [logoBase64, setLogoBase64] = useState("");
  const [fontName, setFontName] = useState("");
  const [fontBase64, setFontBase64] = useState("");
  const [fontStyles, setFontStyles] = useState({
    url: "",
    family: "",
    size: "14px",
    weight: "400",
    style: "normal",
  });
  const [fileSize, setFileSize] = useState(0);
  const [darkMode, setDarkMode] = useState(false);
  const [mobileView, setMobileView] = useState(false);
  const [highlightCategory, setHighlightCategory] = useState(null);
  const [showManageModal, setShowManageModal] = useState(false);

  const [colors, setColors] = useState({
    cardBackground: "#FFFFFF",
    cardFont: "#000000",
    buttonBackground: "#0F3CC9",
    buttonFont: "#FFFFFF",
    linkFont: "#0A2885",
  });
  const [darkColors, setDarkColors] = useState({
    cardBackground: "#071026",
    cardFont: "#E6EEF5",
    buttonBackground: "#0F3CC9",
    buttonFont: "#FFFFFF",
    linkFont: "#6AB0FF",
  });

  const [selectedPreviewLang, setSelectedPreviewLang] = useState("ENGLISH");
  const [titleCookie, setTitleCookie] = useState("This site uses cookies to make your experience better");
  const [descCookie, setDescCookie] = useState(
    "We use essential cookies to make our site work. With your consent, we may also use non-essential cookies to improve user experience and analyze website traffic. By clicking “Accept”, you agree to our website’s cookie use as described in our cookie policy. You can change your cookie settings at any time by clicking “Preferences”"
  );
  const [titlePref, setTitlePref] = useState("Manage preferences");
  const [descPref, setDescPref] = useState("Choose the cookies you allow. You can update your preferences anytime");
  const [isTranslating, setIsTranslating] = useState(false);

  const [parentalControl, setParentalControl] = useState(false);
  const [dataTypeToBeShown, setDataTypeToBeShown] = useState(false);
  const [dataItemToBeShown, setDataItemToBeShown] = useState(false);
  const [processActivityNameToBeShown, setProcessActivityNameToBeShown] = useState(false);
  const [processorNameToBeShown, setProcessorNameToBeShown] = useState(false);
  const [validitytoBeShown, setValiditytoBeShown] = useState(false);
  const [purposes, setPurposes] = useState([]);

  const [consentBanner, setConsentBanner] = useState(true);
  const [declineBtn, setDeclineBtn] = useState(true);
  const [managePrefsBtn, setManagePrefsBtn] = useState(true);
  const [acceptNecessaryBtn, setAcceptNecessaryBtn] = useState(true);
  const [acceptAllBtn, setAcceptAllBtn] = useState(true);

  const [data, setData] = useState(null);
  const [status, setStatus] = useState("IDLE");
  const [error, setError] = useState(null);
  const [lastScannedTs, setLastScannedTs] = useState(null);
  const [isRescanning, setIsRescanning] = useState(false);
  const [pollingTxId, setPollingTxId] = useState(null);
  const [scanningVisible, setScanningVisible] = useState(false);

  const [modalOpen, setModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState("add");
  const [formState, setFormState] = useState({
    name: "",
    category: "",
    domainUrl: "",
    description: "",
    privacyPolicyUrl: "",
    expiresOn: "",
  });
  const [formErrors, setFormErrors] = useState({
    name: "",
    category: "",
    domainUrl: "",
    description: "",
    privacyPolicyUrl: "",
    expiresOn: "",
  });
  const [originalCookieNameModal, setOriginalCookieNameModal] = useState(null);
  const [modalLoading, setModalLoading] = useState(false);

  const [dark, setDark] = useState(false);
  const [mobile, setMobile] = useState(false);
  const [placement, setPlacement] = useState("banner");
  const [accActiveTab, setAccActiveTab] = useState("semantics");
  const [activeAccSubTab, setActiveAccSubTab] = useState("cookie");
  const [activePreview, setActivePreview] = useState("cookie");
  const [activeTab, setActiveTab] = useState("report");

  const pollingRef = useRef(null);
  const mountedRef = useRef(true);
  const [leaveModalOpen, setLeaveModalOpen] = useState(false);
  const [scanInProgressModal, setScanInProgressModal] = useState(false);
  const pendingLeaveRef = useRef(null);
  const beforeUnloadRef = useRef(null);
  const onPopStateRef = useRef(null);

  useEffect(() => {
    async function loadTemplateDataFromStateOrApi() {
      try {
        const tplFromState = location.state?.templateData || null;

        if (tplFromState) {
          sessionStorage.setItem("language_tab_sub", "cookie");
          const tpl = tplFromState;

          if (tpl.multilingual) {
            const langs = tpl.multilingual.supportedLanguages || ["ENGLISH"];
            const langMap = { ...(tpl.multilingual.languageSpecificContentMap || {}) };

            Object.keys(langMap).forEach((lang) => {
              const entry = langMap[lang] || {};
              langMap[lang] = {
                ...entry,
                cookieTitle: entry.cookieTitle || entry.title || "",
                cookieDescription: entry.cookieDescription || entry.description || "",
                managePrefTitle: entry.managePrefTitle || entry.rightsText || "",
                managePrefDescription: entry.managePrefDescription || entry.permissionText || "",
                title: entry.title || entry.cookieTitle || "",
                description: entry.description || entry.cookieDescription || "",
                rightsText: entry.rightsText || entry.managePrefTitle || "",
                permissionText: entry.permissionText || entry.managePrefDescription || "",
              };
            });

            if (tpl.uiConfig?.buttonLabels) {
              const btnLabels = tpl.uiConfig.buttonLabels;
              Object.keys(langMap).forEach((lang) => {
                langMap[lang] = {
                  ...langMap[lang],
                  acceptAll: langMap[lang]?.acceptAll || btnLabels.acceptAll || "",
                  rejectAll: langMap[lang]?.rejectAll || btnLabels.rejectAll || "",
                  acceptNecessary: langMap[lang]?.acceptNecessary || btnLabels.save || "",
                  managePrefs: langMap[lang]?.managePrefs || btnLabels.managePreferences || "",
                  saveButtonText: langMap[lang]?.saveButtonText || btnLabels.savePreferences || "",
                };
              });
            }

            setSupportedLanguages(langs);
            setLanguageSpecificContentMap(langMap);
            const primaryLang = langs.find((l) => l !== "ENGLISH") || "ENGLISH";
            setLanguage(primaryLang);
            setSelectedPreviewLang(primaryLang);
          }

          if (tpl.uiConfig) {
            const ui = tpl.uiConfig;
            if (ui.theme) {
              try {
                const decoded = JSON.parse(atob(ui.theme));
                setColors(decoded.light || {});
                setDarkColors(decoded.dark || {});
              } catch (e) {
                console.warn("Theme decoding failed", e);
              }
            }

            if (ui.logo) setLogoPreviewUrl(ui.logo);
            if (typeof ui.darkMode === "boolean") setDarkMode(ui.darkMode);
            if (typeof ui.mobileView === "boolean") setMobileView(ui.mobileView);
            if (typeof ui.parentalControl === "boolean") setParentalControl(ui.parentalControl);
            if (typeof ui.dataTypeToBeShown === "boolean") setDataTypeToBeShown(ui.dataTypeToBeShown);
            if (typeof ui.dataItemToBeShown === "boolean") setDataItemToBeShown(ui.dataItemToBeShown);
            if (typeof ui.processActivityNameToBeShown === "boolean") setProcessActivityNameToBeShown(ui.processActivityNameToBeShown);
            if (typeof ui.processorNameToBeShown === "boolean") setProcessorNameToBeShown(ui.processorNameToBeShown);
            if (typeof ui.validitytoBeShown === "boolean") setValiditytoBeShown(ui.validitytoBeShown);

            if (ui.buttonLabels) {
              setAcceptAllBtn(Object.prototype.hasOwnProperty.call(ui.buttonLabels, "acceptAll"));
              setDeclineBtn(Object.prototype.hasOwnProperty.call(ui.buttonLabels, "rejectAll"));
              setAcceptNecessaryBtn(Object.prototype.hasOwnProperty.call(ui.buttonLabels, "save"));
              setManagePrefsBtn(Object.prototype.hasOwnProperty.call(ui.buttonLabels, "managePreferences"));
            }
          }

          if (tpl.preferences) setPurposes(tpl.preferences || []);
          return;
        }

        if (!templateIdParam) return;
        sessionStorage.setItem("language_tab_sub", "cookie");

        const url = `${BASE}/cookie-templates/${templateIdParam}`;
        const res = await safeFetchJson(url, { method: "GET", tenantId: tenant_id, token });
        const template = res?.data || res;
        if (!template) return;

        if (template.multilingual) {
          const langs = template.multilingual.supportedLanguages || [];
          const langMap = { ...(template.multilingual.languageSpecificContentMap || {}) };

          if (template.uiConfig?.buttonLabels) {
            const btnLabels = template.uiConfig.buttonLabels;
            Object.keys(langMap).forEach((lang) => {
              langMap[lang] = {
                ...langMap[lang],
                acceptAll: langMap[lang]?.acceptAll || btnLabels.acceptAll || "",
                rejectAll: langMap[lang]?.rejectAll || btnLabels.rejectAll || "",
                acceptNecessary: langMap[lang]?.acceptNecessary || btnLabels.save || "",
                managePrefs: langMap[lang]?.managePrefs || btnLabels.managePreferences || "",
                saveButtonText: langMap[lang]?.saveButtonText || btnLabels.savePreferences || "",
              };
            });
          }

          setSupportedLanguages(langs);
          setLanguageSpecificContentMap(langMap);
        }

        if (template.uiConfig) {
          const ui = template.uiConfig;
          setLogoPreviewUrl(ui.logo || "");
          setDarkMode(ui.darkMode || false);
          setMobileView(ui.mobileView || false);
          if (ui.theme) {
            try {
              const decoded = JSON.parse(atob(ui.theme));
              setColors(decoded?.light || {});
              setDarkColors(decoded?.dark || {});
            } catch { }
          }

          if (ui.buttonLabels) {
            setAcceptAllBtn(Object.prototype.hasOwnProperty.call(ui.buttonLabels, "acceptAll"));
            setDeclineBtn(Object.prototype.hasOwnProperty.call(ui.buttonLabels, "rejectAll"));
            setAcceptNecessaryBtn(Object.prototype.hasOwnProperty.call(ui.buttonLabels, "save"));
            setManagePrefsBtn(Object.prototype.hasOwnProperty.call(ui.buttonLabels, "managePreferences"));
          }
        }

        if (template.preferences) setPurposes(template.preferences);
        toast.info(<CustomToast type="info" message="Template loaded from server" />, { icon: false });
      } catch (err) {
        console.error("Failed to load template:", err);
      }
    }

    loadTemplateDataFromStateOrApi();
  }, [location.state, templateIdParam, tenant_id, token]);

  useEffect(() => {
    const stored = sessionStorage.getItem("editTemplateData");
    if (!stored) return;

    try {
      const tpl = JSON.parse(stored);
      if (tpl.multilingual) {
        const langs = tpl.multilingual.supportedLanguages || ["ENGLISH"];
        const langMap = { ...(tpl.multilingual.languageSpecificContentMap || {}) };

        Object.keys(langMap).forEach((lang) => {
          const entry = langMap[lang] || {};
          langMap[lang] = {
            ...entry,
            cookieTitle: entry.cookieTitle || entry.title || "",
            cookieDescription: entry.cookieDescription || entry.description || "",
            managePrefTitle: entry.managePrefTitle || entry.rightsText || "",
            managePrefDescription: entry.managePrefDescription || entry.permissionText || "",
            title: entry.title || entry.cookieTitle || "",
            description: entry.description || entry.cookieDescription || "",
            rightsText: entry.rightsText || entry.managePrefTitle || "",
            permissionText: entry.permissionText || entry.managePrefDescription || "",
          };
        });

        if (tpl.uiConfig?.buttonLabels) {
          const btnLabels = tpl.uiConfig.buttonLabels;
          Object.keys(langMap).forEach((lang) => {
            langMap[lang] = {
              ...langMap[lang],
              acceptAll: langMap[lang]?.acceptAll || btnLabels.acceptAll || "",
              rejectAll: langMap[lang]?.rejectAll || btnLabels.rejectAll || "",
              acceptNecessary: langMap[lang]?.acceptNecessary || btnLabels.save || "",
              managePrefs: langMap[lang]?.managePrefs || btnLabels.managePreferences || "",
              saveButtonText: langMap[lang]?.saveButtonText || btnLabels.savePreferences || "",
            };
          });
        }

        setSupportedLanguages(langs);
        setLanguageSpecificContentMap(langMap);
      }

      if (tpl.uiConfig) {
        const ui = tpl.uiConfig;
        if (ui.theme) {
          try {
            const decoded = JSON.parse(atob(ui.theme));
            setColors(decoded.light || {});
            setDarkColors(decoded.dark || {});
          } catch (e) {
            console.warn("Theme decoding failed:", e);
          }
        }

        if (ui.logo) setLogoPreviewUrl(ui.logo);
        if (typeof ui.darkMode === "boolean") setDarkMode(ui.darkMode);
        if (typeof ui.mobileView === "boolean") setMobileView(ui.mobileView);
        if (typeof ui.parentalControl === "boolean") setParentalControl(ui.parentalControl);
        if (typeof ui.dataTypeToBeShown === "boolean") setDataTypeToBeShown(ui.dataTypeToBeShown);
        if (typeof ui.dataItemToBeShown === "boolean") setDataItemToBeShown(ui.dataItemToBeShown);
        if (typeof ui.processActivityNameToBeShown === "boolean") setProcessActivityNameToBeShown(ui.processActivityNameToBeShown);
        if (typeof ui.processorNameToBeShown === "boolean") setProcessorNameToBeShown(ui.processorNameToBeShown);
        if (typeof ui.validitytoBeShown === "boolean") setValiditytoBeShown(ui.validitytoBeShown);

        if (ui.buttonLabels) {
          setAcceptAllBtn(Object.prototype.hasOwnProperty.call(ui.buttonLabels, "acceptAll"));
          setDeclineBtn(Object.prototype.hasOwnProperty.call(ui.buttonLabels, "rejectAll"));
          setAcceptNecessaryBtn(Object.prototype.hasOwnProperty.call(ui.buttonLabels, "save"));
          setManagePrefsBtn(Object.prototype.hasOwnProperty.call(ui.buttonLabels, "managePreferences"));
        }
      }
    } catch (err) {
      console.error("Failed to parse editTemplateData:", err);
    }
  }, []);

  useEffect(() => {
    if (supportedLanguages.length === 1 && !languageSpecificContentMap[supportedLanguages[0]]) {
      const lang = supportedLanguages[0];
      setLanguage(lang);
      setLanguageSpecificContentMap((prev) => ({ ...prev, [lang]: prev.ENGLISH || {} }));
    }
  }, [supportedLanguages, languageSpecificContentMap]);

  const openLeaveConfirm = (action) => {
    pendingLeaveRef.current = action;
    setLeaveModalOpen(true);
  };

  const cancelLeave = () => {
    pendingLeaveRef.current = null;
    setLeaveModalOpen(false);
  };

  const confirmLeave = () => {
    setLeaveModalOpen(false);
    const action = pendingLeaveRef.current;
    pendingLeaveRef.current = null;
    try {
      if (beforeUnloadRef.current) window.removeEventListener("beforeunload", beforeUnloadRef.current);
      if (onPopStateRef.current) window.removeEventListener("popstate", onPopStateRef.current);
    } catch { }
    try {
      action && action();
    } catch { }
  };

  const handleBack = () => {
    const running = (status || "").toUpperCase().includes("RUN") || isRescanning || !!pollingTxId || scanningVisible;
    if (running) {
      openLeaveConfirm(() => navigate("/registercookies"));
      return;
    }
    navigate("/registercookies");
  };

  useEffect(() => {
    const beforeUnload = (e) => {
      const running = (status || "").toUpperCase().includes("RUN") || isRescanning || !!pollingTxId || scanningVisible;
      if (running) {
        e.preventDefault();
        e.returnValue = "";
        return "";
      }
      return undefined;
    };

    const onPopState = () => {
      const running = (status || "").toUpperCase().includes("RUN") || isRescanning || !!pollingTxId || scanningVisible;
      if (running) {
        try {
          window.history.pushState(null, "", window.location.href);
        } catch { }
        openLeaveConfirm(() => {
          try {
            window.removeEventListener("beforeunload", beforeUnload);
            window.removeEventListener("popstate", onPopState);
            window.history.back();
          } catch {
            navigate("/registercookies");
          }
        });
      }
    };

    beforeUnloadRef.current = beforeUnload;
    onPopStateRef.current = onPopState;

    window.addEventListener("beforeunload", beforeUnload);
    try {
      window.history.pushState(null, "", window.location.href);
    } catch { }
    window.addEventListener("popstate", onPopState);

    return () => {
      try {
        window.removeEventListener("beforeunload", beforeUnloadRef.current);
        window.removeEventListener("popstate", onPopStateRef.current);
      } catch { }
    };
  }, [status, isRescanning, pollingTxId, scanningVisible, navigate]);

  const counts = useMemo(() => {
    if (!data) {
      return { required: 0, functional: 0, advertising: 0, analytics: 0, unclassified: 0 };
    }
    const reqCount = Object.values(data.required || {}).reduce((n, arr) => n + (Array.isArray(arr) ? arr.length : 0), 0);
    const result = {
      required: reqCount,
      functional: (data.functional || []).length,
      advertising: (data.advertising || []).length,
      analytics: (data.analytics || []).length,
      unclassified: (data.unclassified || []).length,
    };
    // Include dynamic category counts
    if (data.dynamicCategories) {
      Object.entries(data.dynamicCategories).forEach(([label, arr]) => {
        result[label] = (arr || []).length;
      });
    }
    return result;
  }, [data]);

  async function fetchStatusOnceByTx(transactionId) {
    if (!transactionId) {
      setStatus("ERROR");
      setError("Missing transaction id");
      return null;
    }
    setStatus("LOADING");
    try {
      const json = await safeFetchJson(STATUS_URL(transactionId), { method: "GET", tenantId: tenant_id, token }, 20000);
      if (!mountedRef.current) return null;

      const rawData = json.data || json;
      const normalized = normalizeResponse(rawData);
      const rawFallback = json?.lastScanned || json?.updatedAt || json?.scannedAt || json?.timestamp || json?.scanned_at;
      const serverStatus = (normalized?.status || json?.status || json?.state || "UNKNOWN").toString().toUpperCase();

      let fallbackTs = normalized?.lastScanned || rawFallback || null;
      if (!fallbackTs && serverStatus.includes("COMPLET")) {
        fallbackTs = new Date().toISOString();
        if (normalized) normalized.lastScanned = fallbackTs;
      }

      setData(normalized);
      setStatus(serverStatus);
      setLastScannedTs(formatTimestamp(fallbackTs) || null);
      setError(null);
      return normalized;
    } catch (err) {
      if (!mountedRef.current) return null;
      console.error("Status fetch failed:", err);
      setError(err.message || String(err));
      setStatus("ERROR");
      return null;
    }
  }

  function ensureHttpProtocol(url) {
    if (!url) return "";
    const trimmed = url.trim();
    if (/^https?:\/\//i.test(trimmed)) return trimmed;
    return `https://${trimmed}`;
  }

  function sanitizeSubdomainsForScan(subdomains = [], mainUrl = "") {
    if (!Array.isArray(subdomains)) return [];

    const extractHost = (s) => {
      if (!s) return null;
      const trimmed = String(s).trim();
      try {
        const u = new URL(trimmed.startsWith("http") ? trimmed : `https://${trimmed}`);
        return (u.hostname || "").toLowerCase().replace(/^www\./, "");
      } catch {
        const noProto = trimmed.replace(/^https?:\/\//i, "");
        const hostOnly = noProto.split("/")[0].toLowerCase().replace(/^www\./, "");
        return hostOnly || null;
      }
    };

    const mainHost = extractHost(mainUrl) || null;
    const set = new Set();

    for (const s of subdomains) {
      const host = extractHost(s);
      if (!host) continue;
      if (mainHost && host === mainHost) continue;
      set.add(host);
    }

    return Array.from(set);
  }

  async function startScanReturningTx(scanUrl, subDomains = []) {
    const safeUrl = ensureHttpProtocol(scanUrl);
    const sanitized = sanitizeSubdomainsForScan(subDomains, safeUrl);

    const res = await safeFetchJson(
      START_SCAN_URL,
      {
        method: "POST",
        tenantId: tenant_id,
        token,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ url: safeUrl, subDomain: sanitized }),
      },
      20000
    );

    const transactionId = res.transactionId || res.id || res.txId || res.tx_id || null;
    return { transactionId, raw: res };
  }

  async function handleRescan() {
    if (isRescanning) return;
    setIsRescanning(true);
    setError(null);
    setScanningVisible(true);

    try {
      const existingSubdomains = (data?.subdomains || [])
        .map((sd) => sd.subdomainUrl || sd.subdomain || sd.subDomain || sd.subdomainName || sd.domain || sd.host || "")
        .filter(Boolean);

      const scanTarget = ensureHttpProtocol(data?.url || cookietitle);
      const { transactionId } = await startScanReturningTx(scanTarget, existingSubdomains);
      if (!transactionId) throw new Error("Server did not return transaction ID for rescanning");

      sessionStorage.setItem("activeRescanTx", transactionId);
      setPollingTxId(transactionId);
      setStatus("RUNNING");

      const pollInterval = 5000;
      const maxWait = 5 * 60 * 1000;
      const start = Date.now();

      while (Date.now() - start < maxWait) {
        const res = await fetchStatusOnceByTx(transactionId);
        const st = (res?.status || "").toUpperCase();
        if (["COMPLETED", "FAILED", "ERROR"].includes(st)) {
          setStatus(st);
          break;
        }
        await new Promise((r) => setTimeout(r, pollInterval));
      }

      const freshData = await fetchStatusOnceByTx(transactionId);
      if (freshData) setData(freshData);

      sessionStorage.removeItem("activeRescanTx");
      setIsRescanning(false);
      setPollingTxId(null);

      setTimeout(() => {
        navigate(`/registercookies/report/${transactionId}?url=${encodeURIComponent(scanTarget)}`);
      }, 1000);
    } catch (err) {
      console.error("Rescan failed:", err);
      setError(err?.message || String(err));
      setStatus("ERROR");
    } finally {
      setIsRescanning(false);
    }
  }

  useEffect(() => {
    const normalizedStatus = (status || "").toUpperCase();
    const running = normalizedStatus.includes("RUN") || isRescanning || !!pollingTxId;

    if (running) {
      setScanningVisible(true);
    } else {
      const timer = setTimeout(() => setScanningVisible(false), 1500);
      return () => clearTimeout(timer);
    }
  }, [status, isRescanning, pollingTxId]);

  useEffect(() => {
    mountedRef.current = true;

    if (!txIdParam) {
      setData(null);
      setStatus("IDLE");
      setLastScannedTs(null);
      return () => {
        mountedRef.current = false;
      };
    }

    let cancelled = false;
    const POLL = 30000;

    async function start() {
      const initial = await fetchStatusOnceByTx(txIdParam);
      if (cancelled || !mountedRef.current) return;
      const st = (initial?.status || "").toUpperCase();
      if (!["COMPLETED", "FAILED", "ERROR"].includes(st)) {
        if (pollingRef.current) clearInterval(pollingRef.current);
        pollingRef.current = setInterval(async () => {
          const next = await fetchStatusOnceByTx(txIdParam);
          const nextStatus = (next?.status || "").toUpperCase();
          if (["COMPLETED", "FAILED", "ERROR"].includes(nextStatus)) {
            if (pollingRef.current) {
              clearInterval(pollingRef.current);
              pollingRef.current = null;
            }
          }
        }, POLL);
      }
    }

    start();

    return () => {
      cancelled = true;
      mountedRef.current = false;
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
    };
  }, [txIdParam]);

  useEffect(() => {
    if (data) {
      const ts = data.lastScanned || data.updatedAt || data.scannedAt || data.timestamp || data.scanned_at;
      setLastScannedTs(formatTimestamp(ts));
    }
  }, [data]);

  useEffect(() => {
    async function loadCategories() {
      try {
        const res = await safeFetchJson(`${BASE}/category`, { method: "GET", tenantId: tenant_id, token });
        if (Array.isArray(res)) setCategories(res);
        else if (Array.isArray(res.data)) setCategories(res.data);
        else setCategories([]);
      } catch (err) {
        console.error("Failed to fetch categories:", err);
        setCategories([]);
      }
    }
    if (tenant_id) loadCategories();
  }, [tenant_id, token]);

  function isValidCookieName(name) {
    return /^[a-zA-Z][a-zA-Z0-9_.-]*$/.test(name);
  }

  function isValidDomain(domain) {
    if (!domain) return false;
    const cleaned = domain.trim().replace(/^https?:\/\//i, "").replace(/\/.*$/, "");
    return /^\.?([a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}$/.test(cleaned);
  }

  function isValidUrl(url) {
    if (!url) return true;
    try {
      const parsed = new URL(url.trim());
      return ["http:", "https:"].includes(parsed.protocol);
    } catch {
      return false;
    }
  }

  function clearFieldError(field) {
    setFormErrors((prev) => ({ ...prev, [field]: "" }));
  }

  function resolveValidCategory(inputCategory, allowUnknownWhenNoOptions = false) {
    const raw = (inputCategory || "").trim();
    if (!raw) return "";

    const available = (Array.isArray(categories) ? categories : [])
      .map((c) => (c?.category || "").trim())
      .filter(Boolean);

    if (available.length === 0) {
      return allowUnknownWhenNoOptions ? raw : "";
    }

    const lowerRaw = raw.toLowerCase();
    const exact = available.find((c) => c.toLowerCase() === lowerRaw);
    if (exact) return exact;

    const aliasTargets = {
      necessary: ["Necessary", "Required"],
      required: ["Required", "Necessary"],
      essential: ["Required", "Necessary"],
      analytics: ["Analytics"],
      functional: ["Functional"],
      advertising: ["Advertising", "Advertisement"],
      advertisement: ["Advertisement", "Advertising"],
      marketing: ["Advertisement", "Advertising"],
      tracking: ["Advertisement", "Advertising"],
      unclassified: ["Unclassified", "Others"],
      others: ["Others", "Unclassified"],
      other: ["Others", "Unclassified"],
    };

    const targets = aliasTargets[lowerRaw] || [];
    for (const target of targets) {
      const match = available.find((c) => c.toLowerCase() === target.toLowerCase());
      if (match) return match;
    }

    return "";
  }

  function formatDateForInput(dateValue) {
    if (!dateValue) return "";
    try {
      const d = new Date(dateValue);
      if (Number.isNaN(d.getTime())) return "";
      return d.toISOString().split("T")[0];
    } catch {
      return "";
    }
  }

  function validateForm() {
    const errorsObj = {
      name: "",
      category: "",
      domainUrl: "",
      description: "",
      privacyPolicyUrl: "",
      expiresOn: "",
    };

    const name = (formState.name || "").trim();
    const category = (formState.category || "").trim();
    const domainUrl = (formState.domainUrl || "").trim();
    const description = (formState.description || "").trim();
    const privacyPolicyUrl = (formState.privacyPolicyUrl || "").trim();
    const expiresOn = (formState.expiresOn || "").trim();

    if (!name) {
      errorsObj.name = "Cookie name is required.";
    } else if (!isValidCookieName(name)) {
      errorsObj.name = "Cookie name must start with a letter and can contain only letters, numbers, dots, underscores, and hyphens.";
    } else if (name.length > 100) {
      errorsObj.name = "Cookie name must not exceed 100 characters.";
    }

    if (!category) {
      errorsObj.category = "Please select a cookie category.";
    } else if (!resolveValidCategory(category, false)) {
      errorsObj.category = "Selected category is not valid for this tenant. Please choose a listed category.";
    }

    if (!domainUrl) {
      errorsObj.domainUrl = "Domain URL is required.";
    } else if (!isValidDomain(domainUrl)) {
      errorsObj.domainUrl = "Please enter a valid domain like example.com or .example.com";
    }

    if (!description) {
      errorsObj.description = "Description is required.";
    } else if (description.length < 5) {
      errorsObj.description = "Description must be at least 5 characters long.";
    } else if (description.length > 500) {
      errorsObj.description = "Description must not exceed 500 characters.";
    }

    if (privacyPolicyUrl && !isValidUrl(privacyPolicyUrl)) {
      errorsObj.privacyPolicyUrl = "Please enter a valid privacy policy URL starting with http:// or https://";
    }

    if (expiresOn) {
      const date = new Date(expiresOn);
      if (Number.isNaN(date.getTime())) {
        errorsObj.expiresOn = "Please select a valid expiry date.";
      }
    }

    setFormErrors(errorsObj);
    const hasErrors = Object.values(errorsObj).some(Boolean);

    if (hasErrors) {
      const missingFields = [];
      if (errorsObj.name) missingFields.push("Cookie name");
      if (errorsObj.category) missingFields.push("Category");
      if (errorsObj.domainUrl) missingFields.push("Domain URL");
      if (errorsObj.description) missingFields.push("Description");
      if (errorsObj.privacyPolicyUrl) missingFields.push("Privacy policy URL");
      if (errorsObj.expiresOn) missingFields.push("Expiry date");

      const message =
        missingFields.length > 0
          ? `Please fix: ${missingFields.join(", ")}.`
          : "Please correct the highlighted fields before submitting.";
      toast.error(<CustomToast type="error" message={message} />, { icon: false });
      return false;
    }
    return true;
  }

  function openAddModal() {
    setModalMode("add");
    setOriginalCookieNameModal(null);
    setFormState({
      name: "",
      category: "",
      domainUrl: "",
      description: "",
      privacyPolicyUrl: "",
      expiresOn: "",
    });
    setFormErrors({
      name: "",
      category: "",
      domainUrl: "",
      description: "",
      privacyPolicyUrl: "",
      expiresOn: "",
    });
    setModalOpen(true);
  }

  function handleEdit(cookieItem) {
    if (!cookieItem) return;
    setModalMode("edit");
    setOriginalCookieNameModal(cookieItem.name || "");
    setFormState({
      name: cookieItem.name || "",
      category: cookieItem.category || cookieItem.raw?.category || "",
      domainUrl: cookieItem.domain || cookieItem.domainUrl || cookieItem.raw?.domain || "",
      description: cookieItem.description || cookieItem.raw?.description || "",
      privacyPolicyUrl: cookieItem.raw?.privacyPolicyUrl || cookieItem.raw?.privacy_policy || "",
      expiresOn: formatDateForInput(cookieItem.expires || cookieItem.raw?.expires || cookieItem.raw?.expiresOn),
    });
    setFormErrors({
      name: "",
      category: "",
      domainUrl: "",
      description: "",
      privacyPolicyUrl: "",
      expiresOn: "",
    });
    setModalOpen(true);
  }

  function handleAddButtonClick() {
    openAddModal();
  }

  function formatItemFromForm() {
    return {
      name: formState.name,
      domain: formState.domainUrl,
      description: formState.description,
      expires: formState.expiresOn || null,
      category: formState.category,
      raw: {
        name: formState.name,
        category: formState.category,
        domain: formState.domainUrl,
        description: formState.description,
        privacyPolicyUrl: formState.privacyPolicyUrl,
        expires: formState.expiresOn,
      },
    };
  }

  async function handleModalAdd() {
    if (!validateForm()) return;
    setModalLoading(true);

    try {
      const resolvedCategory = resolveValidCategory(formState.category, false);
      if (!resolvedCategory) {
        setFormErrors((prev) => ({
          ...prev,
          category: "Selected category is not valid for this tenant. Please choose a listed category.",
        }));
        toast.error(<CustomToast type="error" message="Selected category is not valid for this tenant." />, { icon: false });
        setModalLoading(false);
        return;
      }

      const newItem = {
        ...formatItemFromForm(),
        category: resolvedCategory,
        raw: { ...formatItemFromForm().raw, category: resolvedCategory },
      };
      const transactionId = data?.transactionId || txIdParam || pollingTxId || null;

      if (transactionId) {
        const payload = {
          name: (newItem.name || "").trim(),
          domain: (newItem.domain || newItem.domainUrl || "").trim() || "unknown.com",
          path: "/",
          secure: true,
          httpOnly: false,
          sameSite: "LAX",
          source: "FIRST_PARTY",
          category: resolvedCategory,
          description: newItem.description?.trim() || "",
          provider: " ",
        };

        if (newItem.expires || formState.expiresOn) {
          payload.expires = new Date(formState.expiresOn || newItem.expires).toISOString();
        }

        if (formState.privacyPolicyUrl?.trim()) {
          payload.privacyPolicyUrl = formState.privacyPolicyUrl.trim();
        }

        const url = `${BASE}/transaction/${encodeURIComponent(transactionId)}/cookies`;
        const addResp = await safeFetchJson(
          url,
          {
            method: "POST",
            tenantId: tenant_id,
            token,
            headers: { "Content-Type": "application/json", "x-session-token": token },
            body: JSON.stringify(payload),
          },
          20000
        );

        const addSuccessMsg = addResp?.message || addResp?.details || "Cookie added successfully!";
        toast.success(<CustomToast type="success" message={addSuccessMsg} />, { icon: false });
      }

      setData((prev) => {
        const next = prev ? { ...prev } : { required: {}, functional: [], advertising: [], analytics: [], unclassified: [], dynamicCategories: {}, totalCookies: 0 };
        const catRaw = (newItem.category || "").toString().toLowerCase().trim();

        if (/required|necessary|essential/.test(catRaw)) {
          const vendor = "Manual";
          next.required = { ...(next.required || {}) };
          next.required[vendor] = Array.isArray(next.required[vendor]) ? [...next.required[vendor], newItem] : [newItem];
        } else if (/functional/.test(catRaw)) {
          next.functional = [...(next.functional || []), newItem];
        } else if (/advertis|marketing|tracking/.test(catRaw)) {
          next.advertising = [...(next.advertising || []), newItem];
        } else if (/analytic|ga|google|mixpanel|segment/.test(catRaw)) {
          next.analytics = [...(next.analytics || []), newItem];
        } else if (/^others$|^unclassified$/i.test(catRaw)) {
          next.unclassified = [...(next.unclassified || []), newItem];
        } else if (catRaw) {
          // Dynamic / custom category
          const label = (newItem.category || catRaw).trim();
          next.dynamicCategories = { ...(next.dynamicCategories || {}) };
          next.dynamicCategories[label] = [...(next.dynamicCategories[label] || []), newItem];
        } else {
          next.unclassified = [...(next.unclassified || []), newItem];
        }

        next.totalCookies = (next.totalCookies || 0) + 1;
        return next;
      });

      if (transactionId) fetchStatusOnceByTx(transactionId).catch(() => { });
      setHighlightCategory((newItem.category || "").toLowerCase());
      setModalOpen(false);
      setFormErrors({ name: "", category: "", domainUrl: "", description: "", privacyPolicyUrl: "", expiresOn: "" });
    } catch (err) {
      const fieldErrors = parseCookieFieldErrors(err);
      setFormErrors((prev) => ({
        ...prev,
        name: fieldErrors.name || prev.name,
        category: fieldErrors.category || prev.category,
        domainUrl: fieldErrors.domainUrl || prev.domainUrl,
        description: fieldErrors.description || prev.description,
        privacyPolicyUrl: fieldErrors.privacyPolicyUrl || prev.privacyPolicyUrl,
        expiresOn: fieldErrors.expiresOn || prev.expiresOn,
      }));
      toast.error(<CustomToast type="error" message={fieldErrors.global || mapError(err)} />, { icon: false });
    } finally {
      setModalLoading(false);
    }
  }

  async function handleModalSave() {
    if (!validateForm()) return;
    setModalLoading(true);

    try {
      const resolvedCategory = resolveValidCategory(formState.category, false);
      if (!resolvedCategory) {
        setFormErrors((prev) => ({
          ...prev,
          category: "Selected category is not valid for this tenant. Please choose a listed category.",
        }));
        toast.error(<CustomToast type="error" message="Selected category is not valid for this tenant." />, { icon: false });
        setModalLoading(false);
        return;
      }

      const updatedItem = {
        ...formatItemFromForm(),
        category: resolvedCategory,
        raw: { ...formatItemFromForm().raw, category: resolvedCategory },
      };
      const transactionId = data?.transactionId || txIdParam || pollingTxId || null;
      if (!transactionId) throw new Error("Missing transaction ID. Cannot save cookie.");

      const inferProviderFromDomain = (domain) => {
        if (!domain) return "Manual Entry";
        const lower = domain.toLowerCase();
        const knownProviders = {
          "google.com": "Google LLC",
          "facebook.com": "Meta Platforms, Inc.",
          "doubleclick.net": "Google Ads / DoubleClick",
          "youtube.com": "Google / YouTube",
          "amazon.com": "Amazon Web Services",
          "twitter.com": "Twitter / X Corp.",
          "linkedin.com": "LinkedIn Corporation",
          "spotify.com": "Spotify AB",
          "apple.com": "Apple Inc.",
          "microsoft.com": "Microsoft Corporation",
          "adobe.com": "Adobe Systems",
          "hotjar.com": "Hotjar Ltd.",
          "cloudflare.com": "Cloudflare, Inc.",
          "jio.com": "Reliance Jio Infocomm Ltd.",
        };

        const match = Object.keys(knownProviders).find((key) => lower.endsWith(key));
        if (match) return knownProviders[match];

        const base = lower.replace(/^www\./, "").replace(/^\./, "").split(".")[0];
        return base ? base.charAt(0).toUpperCase() + base.slice(1) : " ";
      };

      const providerName = updatedItem.provider?.trim() || inferProviderFromDomain(updatedItem.domain || updatedItem.domainUrl);

      const payload = {
        name: (updatedItem.name || "").trim(),
        category: resolvedCategory,
        description: updatedItem.description?.trim() || "",
        domain: (updatedItem.domain || updatedItem.domainUrl || "").trim(),
        privacyPolicyUrl: formState.privacyPolicyUrl?.trim() || "",
        provider: providerName,
        expires: formState.expiresOn ? new Date(formState.expiresOn).toISOString() : new Date("2025-12-31T23:59:59Z").toISOString(),
      };

      Object.keys(payload).forEach((key) => payload[key] === "" && delete payload[key]);

      const url = `${BASE}/transaction/${encodeURIComponent(transactionId)}/cookie`;
      const editResp = await safeFetchJson(
        url,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            "X-Tenant-ID": tenant_id,
            "x-session-token": token,
          },
          body: JSON.stringify(payload),
        },
        20000
      );
      const editSuccessMsg = editResp?.message || editResp?.details || "Cookie updated successfully!";

      setData((prev) => {
        if (!prev) return prev;
        const next = { ...prev };
        const categoriesToCheck = ["functional", "advertising", "analytics", "unclassified"];
        let found = false;

        for (const cat of categoriesToCheck) {
          if (Array.isArray(next[cat])) {
            const idx = next[cat].findIndex((it) => it.name === originalCookieNameModal);
            if (idx >= 0) {
              next[cat] = [...next[cat]];
              next[cat][idx] = { ...next[cat][idx], ...updatedItem };
              found = true;
              break;
            }
          }
        }

        if (!found) {
          const catRaw = (updatedItem.category || "").toLowerCase();
          if (/necessary|required/.test(catRaw)) {
            next.required = next.required || {};
            next.required.Manual = [...(next.required.Manual || []), updatedItem];
          } else if (/functional/.test(catRaw)) {
            next.functional = [...(next.functional || []), updatedItem];
          } else if (/advertis|marketing|tracking/.test(catRaw)) {
            next.advertising = [...(next.advertising || []), updatedItem];
          } else if (/analytic/.test(catRaw)) {
            next.analytics = [...(next.analytics || []), updatedItem];
          } else {
            next.unclassified = [...(next.unclassified || []), updatedItem];
          }
        }

        return next;
      });

      if (transactionId) fetchStatusOnceByTx(transactionId).catch(() => { });
      setHighlightCategory((updatedItem.category || "").toLowerCase());
      setModalOpen(false);
      setFormErrors({ name: "", category: "", domainUrl: "", description: "", privacyPolicyUrl: "", expiresOn: "" });
      toast.success(<CustomToast type="success" message={editSuccessMsg} />, { icon: false });
    } catch (err) {
      console.error("Failed to update cookie:", err);
      const errorMsg = mapError(err);
      const fieldErrors = parseCookieFieldErrors(err);
      setFormErrors((prev) => ({
        ...prev,
        name: fieldErrors.name || prev.name,
        category: fieldErrors.category || prev.category,
        domainUrl: fieldErrors.domainUrl || prev.domainUrl,
        description: fieldErrors.description || prev.description,
        privacyPolicyUrl: fieldErrors.privacyPolicyUrl || prev.privacyPolicyUrl,
        expiresOn: fieldErrors.expiresOn || prev.expiresOn,
      }));

      if (errorMsg.toLowerCase().includes("not found")) {
        toast.error(<CustomToast type="error" message={errorMsg} />, { icon: false });
        setData((prev) => {
          if (!prev) return prev;
          const next = { ...prev };
          Object.keys(next).forEach((cat) => {
            if (Array.isArray(next[cat])) {
              next[cat] = next[cat].filter((it) => it.name !== originalCookieNameModal);
            }
          });
          return next;
        });
      } else {
        toast.error(<CustomToast type="error" message={errorMsg} />, { icon: false });
      }
    } finally {
      setModalLoading(false);
    }
  }

  const previewButtons = useMemo(() => {
    const map = languageSpecificContentMap || {};
    const langKey = language || "ENGLISH";
    const fallback = map.ENGLISH || {};
    const current = map[langKey] || {};
    const getLabel = (key, defaultValue) => current[key]?.trim() || fallback[key]?.trim() || defaultValue;

    const buttons = [];
    if (managePrefsBtn) buttons.push({ id: "prefs", label: getLabel("managePrefs", "Manage preferences"), variant: "ghost" });
    if (declineBtn) buttons.push({ id: "reject", label: getLabel("rejectAll", "Reject all"), variant: "outline" });
    if (acceptNecessaryBtn) buttons.push({ id: "necessary", label: getLabel("acceptNecessary", "Accept necessary cookies"), variant: "ghost" });
    if (acceptAllBtn) buttons.push({ id: "accept_all", label: getLabel("acceptAll", "Accept all cookies"), variant: "primary" });
    return buttons;
  }, [languageSpecificContentMap, language, managePrefsBtn, declineBtn, acceptNecessaryBtn, acceptAllBtn]);

  const handlePreviewAction = (id) => {
    if (id === "prefs") toast.info("Open preferences (preview)");
    else if (id === "reject") toast.info("Rejected all (preview)");
    else if (id === "necessary") toast.info("Accepted necessary cookies (preview)");
    else if (id === "accept_all") toast.info("Accepted all cookies (preview)");
  };

  const previewContent = useMemo(() => {
    const map = languageSpecificContentMap || {};
    const langKey = language || "ENGLISH";
    const fallback = map.ENGLISH || {};
    const current = map[langKey] || {};

    return {
      title:
        titleCookie?.trim() ||
        current.cookieTitle?.trim() ||
        current.title?.trim() ||
        fallback.cookieTitle?.trim() ||
        fallback.title?.trim() ||
        "This site uses cookies to make your experience better.",
      description:
        descCookie?.trim() ||
        current.cookieDescription?.trim() ||
        current.description?.trim() ||
        fallback.cookieDescription?.trim() ||
        fallback.description?.trim() ||
        "We use essential cookies to make our site work. With your consent, we may also use non-essential cookies to improve user experience and analyze website traffic.",
      rightsText:
        titlePref?.trim() ||
        current.rightsText?.trim() ||
        current.managePrefTitle?.trim() ||
        fallback.rightsText?.trim() ||
        "You can manage your cookie preferences and withdraw consent at any time.",
      permissionText:
        descPref?.trim() ||
        current.permissionText?.trim() ||
        current.managePrefDescription?.trim() ||
        fallback.permissionText?.trim() ||
        "Click 'Accept All' to allow all cookies or customize your preferences below.",
    };
  }, [languageSpecificContentMap, language, titleCookie, descCookie, titlePref, descPref]);

  function generateThemeBase64(lightColors, darkColorsObj) {
    try {
      return btoa(JSON.stringify({ light: lightColors, dark: darkColorsObj }));
    } catch (err) {
      console.error("Theme encoding failed:", err);
      return "";
    }
  }

  async function handlePublish(statusType) {
    try {
      const isScanRunning = (status || "").toUpperCase().includes("RUN") || isRescanning || !!pollingTxId || scanningVisible;
      if (isScanRunning) {
        setScanInProgressModal(true);
        return;
      }

      setPublishLoading(true);
      const SAMPLE_LOGO_BASE64 =
        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAASsJTYQAAAAASUVORK5CYII=";

      const templateData = location?.state?.templateData || null;
      const templateId = templateData?.templateId || null;
      const scanId = templateData?.scanId || txIdParam || null;
      const isUpdate = Boolean(templateId);

      if (!scanId && !isUpdate) {
        toast.error(<CustomToast type="error" message="Scan is not completed yet. Complete the scan before publishing." />, { icon: false });
        setPublishLoading(false);
        return;
      }

      const updatedLangMap = { ...languageSpecificContentMap };
      const currentLang = language || selectedPreviewLang || "ENGLISH";
      updatedLangMap[currentLang] = {
        ...(updatedLangMap[currentLang] || {}),
        title: titleCookie || updatedLangMap[currentLang]?.title || updatedLangMap[currentLang]?.cookieTitle || "",
        cookieTitle: titleCookie || updatedLangMap[currentLang]?.cookieTitle || updatedLangMap[currentLang]?.title || "",
        description: descCookie || updatedLangMap[currentLang]?.description || updatedLangMap[currentLang]?.cookieDescription || "",
        cookieDescription: descCookie || updatedLangMap[currentLang]?.cookieDescription || updatedLangMap[currentLang]?.description || "",
        rightsText: titlePref || updatedLangMap[currentLang]?.rightsText || updatedLangMap[currentLang]?.managePrefTitle || "",
        managePrefTitle: titlePref || updatedLangMap[currentLang]?.managePrefTitle || updatedLangMap[currentLang]?.rightsText || "",
        permissionText: descPref || updatedLangMap[currentLang]?.permissionText || updatedLangMap[currentLang]?.managePrefDescription || "",
        managePrefDescription: descPref || updatedLangMap[currentLang]?.managePrefDescription || updatedLangMap[currentLang]?.permissionText || "",
      };

      const cleanLangMap = {};
      Object.keys(updatedLangMap || {}).forEach((lang) => {
        const entry = updatedLangMap[lang] || {};
        cleanLangMap[lang] = {
          title: entry.title || entry.cookieTitle || "",
          description: entry.description || entry.cookieDescription || "",
          label: entry.label || "Cookie Settings",
          rightsText: entry.rightsText || entry.managePrefTitle || "",
          permissionText: entry.permissionText || entry.managePrefDescription || "",
        };
      });

      const activeLang = updatedLangMap?.[currentLang] || {};
      const effectiveSupportedLanguages = Object.keys(cleanLangMap).length > 0 ? Object.keys(cleanLangMap) : supportedLanguages?.length ? supportedLanguages : ["ENGLISH"];

      const multilingualPayload = {
        supportedLanguages: effectiveSupportedLanguages,
        languageSpecificContentMap: cleanLangMap,
      };

      const preferencesPayload = [];
      if (data?.required && Object.keys(data.required).length > 0) {
        preferencesPayload.push({ purpose: "Necessary", isMandatory: true, preferenceValidity: { value: 1, unit: "YEARS" } });
      }
      if (data?.analytics?.length > 0) {
        preferencesPayload.push({ purpose: "Analytics", isMandatory: false, preferenceValidity: { value: 12, unit: "MONTHS" } });
      }
      if (data?.advertising?.length > 0) {
        preferencesPayload.push({ purpose: "Advertisement", isMandatory: false, preferenceValidity: { value: 12, unit: "MONTHS" } });
      }
      if (data?.functional?.length > 0) {
        preferencesPayload.push({ purpose: "Functional", isMandatory: false, preferenceValidity: { value: 12, unit: "MONTHS" } });
      }
      // Include "Others/Unclassified" bucket so templates can publish even when
      // scan results are only in non-standard categories.
      if (data?.unclassified?.length > 0) {
        preferencesPayload.push({ purpose: "Others", isMandatory: false, preferenceValidity: { value: 12, unit: "MONTHS" } });
      }
      // Include dynamic categories in preferences
      if (data?.dynamicCategories) {
        Object.entries(data.dynamicCategories).forEach(([label, arr]) => {
          if (arr?.length > 0) {
            preferencesPayload.push({ purpose: label, isMandatory: false, preferenceValidity: { value: 12, unit: "MONTHS" } });
          }
        });
      }

      if (preferencesPayload.length === 0) {
        toast.error(<CustomToast type="error" message="No cookies found. Please scan the website or add cookies before publishing." />, { icon: false });
        setPublishLoading(false);
        return;
      }

      const buttonLabels = {};
      if (acceptAllBtn) buttonLabels.acceptAll = activeLang.acceptAll || "Accept all";
      if (declineBtn) buttonLabels.rejectAll = activeLang.rejectAll || "Reject all";
      if (acceptNecessaryBtn) buttonLabels.save = activeLang.acceptNecessary || "Accept necessary cookies";
      if (managePrefsBtn) buttonLabels.managePreferences = activeLang.managePrefs || "Manage preferences";
      if (activeLang.saveButtonText) buttonLabels.savePreferences = activeLang.saveButtonText;

      const uiConfig = {
        logo: logoBase64 || logoPreviewUrl || SAMPLE_LOGO_BASE64,
        theme: generateThemeBase64(colors, darkColors),
        darkMode,
        mobileView,
        layoutType: (placement || "BANNER").toUpperCase(),
        managePreferencesTitle: activeLang.rightsText || "",
        managePreferencesDescription: activeLang.permissionText || "",
        buttonLabels,
      };

      const payload = {
        templateName: templateName?.trim() || cookietitle,
        status: statusType,
        privacyPolicyDocument: "",
        documentMeta: null,
        preferences: preferencesPayload,
        uiConfig,
        multilingual: multilingualPayload,
      };

      if (!isUpdate) payload.scanId = scanId;

      const headers = {
        "Content-Type": "application/json",
        "X-Tenant-ID": tenant_id,
        "business-id": businessId,
        "x-session-token": token,
      };

      let apiResp;
      if (isUpdate) {
        apiResp = await axios.put(`${config.cookie_base}/cookie-templates/${templateId}/update`, payload, { headers });
      } else {
        apiResp = await axios.post(`${config.cookie_base}/cookie-templates`, payload, { headers });
      }

      const successMsg =
        apiResp?.data?.message ||
        apiResp?.data?.details ||
        `${isUpdate ? "Updated" : "Created"} successfully`;

      sessionStorage.setItem(
        "registercookies_flash_toast",
        JSON.stringify({ type: "success", message: successMsg })
      );
      navigate("/registercookies");
    } catch (publishErr) {
      console.error("Publish Error", publishErr);
      const apiErr =
        publishErr?.response?.data?.details ||
        publishErr?.response?.data?.message ||
        publishErr?.message ||
        "Publish failed";
      toast.error(<CustomToast type="error" message={apiErr} />, { icon: false });
    } finally {
      setPublishLoading(false);
    }
  }

  /* guard: render nothing when not on the report page (must be after all hooks) */
  if (!isReportPage) return null;

  return (
    <div className="page-container" color="primary-grey-20">
      <div className="main-content-cr" color="primary-grey-20">
        <div className={`left-half-cookie ${activeTab === "report" ? "full-width" : ""}`} style={activeTab !== "report" ? { borderRight: "1px solid #e0e0e0" } : {}}>
          <div className="header-container" style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 16, marginTop: 20, marginLeft: 16 }}>
            <div className="header-left">
              <Button icon="ic_back" iconLeft="ic_back" kind="secondary" onClick={handleBack} size="small" state="normal" />
            </div>

            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", width: "100%" }}>
              <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-start", gap: 2 }}>
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", width: "100%", gap: 12 }}>
                  <Text appearance="heading-xs" color="primary-grey-100">{cookietitle}</Text>
                  {activeTab === "report" && lastScannedTs && (
                    <div className="cr-last-scanned" style={{ whiteSpace: "nowrap" }}>
                      <Text appearance="body-xs-bold" color="primary-grey-80">Last scanned on: {lastScannedTs}</Text>
                    </div>
                  )}
                </div>

                {Array.isArray(data?.subdomains) && data.subdomains.length > 0 && (
                  <Text
                    appearance="body-xs"
                    color="primary-grey-80"
                    style={{ marginTop: 2, fontSize: 13, fontWeight: 500, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", maxWidth: "100%" }}
                    title={data.subdomains
                      .map((s) => s.subdomainUrl || s.subdomain || s.subDomain || s.domain || s.host || "")
                      .filter(Boolean)
                      .join(", ")}
                  >
                    {data.subdomains
                      .map((s) => (s.subdomainUrl || s.subdomain || s.subDomain || s.domain || s.host || "").replace(/^https?:\/\//, "").replace(/\/$/, ""))
                      .filter(Boolean)
                      .join(", ")}
                  </Text>
                )}
              </div>

              {activeTab === "report" && (
                <div style={{ marginLeft: "auto", display: "flex", alignItems: "center" }}>
                  {isRescanning || (status || "").toUpperCase().includes("RUN") || (status || "").toUpperCase().includes("SCANN") ? (
                    <div
                      className="scanning-pill"
                      role="status"
                      aria-live="polite"
                      aria-atomic="true"
                      style={{
                        display: "inline-flex",
                        alignItems: "center",
                        gap: 12,
                        padding: "10px 18px",
                        borderRadius: 999,
                        border: "1px solid rgba(10,60,201,0.12)",
                        background: "#fff",
                        color: "#0A3CCE",
                        fontFamily: "JioType, system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial",
                        fontWeight: 700,
                        fontSize: 16,
                        lineHeight: "24px",
                        letterSpacing: "-0.5%",
                      }}
                    >
                      <svg width="22" height="22" viewBox="0 0 50 50" aria-hidden focusable="false" style={{ display: "block" }}>
                        <circle cx="25" cy="25" r="18" fill="none" stroke="#0A3CCE" strokeWidth="4" strokeLinecap="round" strokeDasharray="85" strokeDashoffset="65">
                          <animateTransform attributeName="transform" type="rotate" from="0 25 25" to="360 25 25" dur="1s" repeatCount="indefinite" />
                        </circle>
                      </svg>
                      <span>Scanning in progress</span>
                    </div>
                  ) : (
                    <ActionButton kind="secondary" size="small" state="normal" label="Rescan" onClick={handleRescan} />
                  )}
                </div>
              )}
            </div>
          </div>

          <div className="cc-body">
            <div className="notif-tabs-cr">
              <Tabs
                appearance="normal"
                overflow="fit"
                onTabChange={(index) => {
                  const tabKeys = ["report", "language", "branding", "accessibility"];
                  const nextTab = tabKeys[index] || "report";
                  setActiveTab(nextTab);
                  if (nextTab === "language") sessionStorage.setItem("languageInnerTab", "cookie");
                  if (nextTab === "branding") {
                    setActiveAccSubTab("cookie");
                    setShowManageModal(false);
                  }
                  if (nextTab === "accessibility") {
                    setActiveAccSubTab("cookie");
                    setShowManageModal(false);
                  }
                }}
              >
                <TabItem label={<Text appearance="body-xs" color="primary-grey-80">Report</Text>}>
                  <div style={{ maxHeight: "68vh" }}>
                    <ReportContainer initialData={data} counts={counts} onEdit={handleEdit} highlightCategory={highlightCategory} apiCategories={categories} />
                  </div>
                </TabItem>

                <TabItem label={<Text appearance="body-xs" color="primary-grey-80">Language</Text>}>
                  <LanguageTab
                    scanData={data}
                    text={text}
                    setText={setText}
                    permission={permission}
                    setPermission={setPermission}
                    rights={rights}
                    setRights={setRights}
                    supportedLanguages={supportedLanguages}
                    setSupportedLanguages={setSupportedLanguages}
                    language={language}
                    setLanguage={useCallback((lang) => {
                      setLanguage(lang);
                      setSelectedPreviewLang(lang);
                    }, [])}
                    label={label}
                    setLabel={setLabel}
                    languageSpecificContentMap={languageSpecificContentMap}
                    setLanguageSpecificContentMap={setLanguageSpecificContentMap}
                    title={title}
                    setTitle={setTitle}
                    placement={placement}
                    setPlacement={setPlacement}
                    onOpenManagePreferences={useCallback((state) => setShowManageModal(state), [])}
                    selectedPreviewLang={selectedPreviewLang}
                    setSelectedPreviewLang={setSelectedPreviewLang}
                    titleCookie={titleCookie}
                    setTitleCookie={setTitleCookie}
                    descCookie={descCookie}
                    setDescCookie={setDescCookie}
                    titlePref={titlePref}
                    setTitlePref={setTitlePref}
                    descPref={descPref}
                    setDescPref={setDescPref}
                    isTranslating={isTranslating}
                    setIsTranslating={setIsTranslating}
                  />
                </TabItem>

                <TabItem label={<Text appearance="body-xs" color="primary-grey-80">Branding</Text>}>
                  <BrandingTab
                    colors={colors}
                    setColors={setColors}
                    darkColors={darkColors}
                    setDarkColors={setDarkColors}
                    logoPreviewUrl={logoPreviewUrl}
                    setLogoPreviewUrl={setLogoPreviewUrl}
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
                    processActivityNameToBeShown={processActivityNameToBeShown}
                    setProcessActivityNameToBeShown={setProcessActivityNameToBeShown}
                    processorNameToBeShown={processorNameToBeShown}
                    setProcessorNameToBeShown={setProcessorNameToBeShown}
                    validitytoBeShown={validitytoBeShown}
                    setValiditytoBeShown={setValiditytoBeShown}
                    consentBanner={consentBanner}
                    setConsentBanner={setConsentBanner}
                    declineBtn={declineBtn}
                    setDeclineBtn={setDeclineBtn}
                    managePrefsBtn={managePrefsBtn}
                    setManagePrefsBtn={setManagePrefsBtn}
                    acceptNecessaryBtn={acceptNecessaryBtn}
                    setAcceptNecessaryBtn={setAcceptNecessaryBtn}
                    acceptAllBtn={acceptAllBtn}
                    setAcceptAllBtn={setAcceptAllBtn}
                    onOpenManagePreferences={(isPrefs) => setShowManageModal(isPrefs)}
                    prefColors={prefColors}
                    setPrefColors={setPrefColors}
                    prefDarkColors={prefDarkColors}
                    setPrefDarkColors={setPrefDarkColors}
                    prefDarkMode={prefDarkMode}
                    setPrefDarkMode={setPrefDarkMode}
                    activePreview={activePreview}
                    setActivePreview={setActivePreview}
                    fontName={fontName}
                    setFontName={setFontName}
                    fontBase64={fontBase64}
                    setFontBase64={setFontBase64}
                    fontInputRef={fontInputRef}
                    fontStyles={fontStyles}
                    setFontStyles={setFontStyles}
                  />
                </TabItem>

                <TabItem label={<Text appearance="body-xs" color="primary-grey-80">Accessibility</Text>}>
                  <div className="acc-con" style={{ padding: "0 16px" }}>
                    <div style={{ display: "flex", gap: 32, marginTop: 16, marginBottom: 8 }}>
                      {[{ key: "cookie", label: "Cookie" }, { key: "preferences", label: "Manage preferences" }].map((tab) => (
                        <button
                          key={tab.key}
                          onClick={() => {
                            setActiveAccSubTab(tab.key);
                            if (tab.key === "cookie") setShowManageModal(false);
                            else setShowManageModal(true);
                          }}
                          className="branding-subtab-btn"
                          style={{
                            background: "none",
                            border: "none",
                            borderBottom: activeAccSubTab === tab.key ? "2px solid var(--jds-color-primary-brand, #0A3CCE)" : "2px solid transparent",
                            color: activeAccSubTab === tab.key ? "var(--jds-color-primary-brand, #0A3CCE)" : "#6B7280",
                            fontWeight: activeAccSubTab === tab.key ? 600 : 500,
                            fontSize: 13,
                            cursor: "pointer",
                            paddingBottom: 4,
                            transition: "all 0.2s ease-in-out",
                          }}
                        >
                          <Text appearance="body-s" color={activeAccSubTab === tab.key ? "primary-brand" : "primary-grey-100"}>{tab.label}</Text>
                        </button>
                      ))}
                    </div>

                    {activeAccSubTab === "cookie" ? (
                      <AccessibilityPanel accActiveTab={accActiveTab} setAccActiveTab={setAccActiveTab} previewContent={previewContent} previewButtons={previewButtons} />
                    ) : (
                      <AccessibilityPanelPreferences accActiveTab={accActiveTab} setAccActiveTab={setAccActiveTab} />
                    )}
                  </div>
                </TabItem>
              </Tabs>
            </div>
          </div>
        </div>

        {activeTab !== "report" && (
          <div className="right-half-cookie" style={{ marginLeft: 1 }}>
            <div className="preview-header" style={{ display: "flex", gap: 12, alignItems: "center", marginBottom: 12 }}>
              <Text appearance="body-s-bold" color="primary-grey-100">Preview</Text>
              <InputToggle checked={mobile} labelPosition="left" onChange={() => setMobile((p) => !p)} size="small" type="toggle" label="Mobile view" />
              <InputToggle checked={dark} labelPosition="left" onChange={() => setDark((p) => !p)} size="small" type="toggle" label="Dark mode" />
            </div>

            {activeTab === "accessibility" ? (
              <div style={{ marginTop: 8 }}>
                {activeAccSubTab === "cookie" ? (
                  <AccPreviewRight tab={accActiveTab} dark={dark} colors={dark ? darkColors : colors} previewContent={previewContent} previewButtons={previewButtons} />
                ) : (
                  <AccManagePreviewRight tab={accActiveTab} darkColors={darkColors} colors={colors} darkMode={dark} />
                )}
              </div>
            ) : (
              <div className={`preview-shell ${mobile ? "preview-shell-mobile" : ""} ${dark ? "preview-shell-dark" : ""}`} style={{ padding: 12, display: "flex", justifyContent: "center", alignItems: placement === "modal" ? "center" : "flex-start", position: "relative", minHeight: 260 }}>
                {showManageModal ? (
                  <div style={{ display: "flex", justifyContent: "center" }}>
                    <ManagePreferencesModal
                      dark={dark}
                      mobile={mobile}
                      onEdit={handleEdit}
                      scanData={data}
                      selectedLanguage={language}
                      translations={languageSpecificContentMap}
                      onClose={() => setShowManageModal(false)}
                      colors={prefColors}
                      darkColors={prefDarkColors}
                      darkMode={prefDarkMode}
                    />
                  </div>
                ) : (
                  <div className={`preview-shell ${mobile ? "preview-shell-mobile" : ""} ${dark ? "preview-shell-dark" : ""}`} style={{ padding: 12, display: "flex", justifyContent: "center", alignItems: placement === "modal" ? "center" : "flex-start", position: "relative", minHeight: 260 }}>
                    {placement === "banner" && (
                      <div className={`popup ${mobile ? "popup-mobile" : ""} ${dark ? "popup-dark" : ""}`} style={{ width: mobile ? 320 : 820, borderRadius: 12, overflow: "hidden", background: dark ? darkColors.cardBackground || "#071026" : colors.cardBackground || "#FFFFFF", boxShadow: dark ? "0 10px 30px rgba(2,6,23,0.6)" : "0 6px 18px rgba(2,6,23,0.06)", border: dark ? "1px solid rgba(255,255,255,0.08)" : "1px solid rgba(14,20,30,0.06)", transition: "all 0.3s ease" }}>
                        <div style={{ height: 36, display: "flex", alignItems: "center", padding: "8px 12px", background: dark ? "#0A1220" : "#F3F4F6" }}>
                          <div style={{ display: "flex", gap: 6 }}>
                            <div style={{ width: 10, height: 10, borderRadius: 999, background: "#EF4444" }} />
                            <div style={{ width: 10, height: 10, borderRadius: 999, background: "#FACC15" }} />
                            <div style={{ width: 10, height: 10, borderRadius: 999, background: "#22C55E" }} />
                          </div>
                        </div>
                        <div style={{ height: 240, background: dark ? "#0B1120" : "#FFFFFF" }} />
                        <div role="region" aria-label="Cookie banner preview" style={{ borderTop: `1px solid ${dark ? "rgba(255,255,255,0.08)" : "rgba(14,20,30,0.08)"}`, padding: 18, background: dark ? darkColors.cardBackground || "#061426" : colors.cardBackground || "#FFFFFF", display: "flex", flexDirection: "column", gap: 12 }}>
                          <div>
                            <div style={{ fontWeight: 700, fontSize: 15, marginBottom: 6, color: dark ? darkColors.cardFont || "#E6EEF5" : colors.cardFont || "#111827" }}>{previewContent.title}</div>
                            <div style={{ fontSize: 13, color: dark ? darkColors.cardFont || "#9CA3AF" : colors.cardFont || "#4B5563", lineHeight: 1.5 }}>{previewContent.description}</div>
                          </div>
                          <div style={{ display: "flex", flexWrap: "wrap", gap: 10, marginTop: 6, justifyContent: "flex-end" }}>
                            {previewButtons.map((btn) => {
                              let background;
                              let color;
                              let border;
                              if (btn.variant === "primary") {
                                background = dark ? darkColors.buttonBackground || "#0F3CC9" : colors.buttonBackground || "#0F3CC9";
                                color = dark ? darkColors.buttonFont || "#FFFFFF" : colors.buttonFont || "#FFFFFF";
                                border = "none";
                              } else {
                                background = "transparent";
                                border = `1px solid ${dark ? darkColors.linkFont || "#6AB0FF" : colors.linkFont || "#0A2885"}`;
                                color = dark ? darkColors.linkFont || "#6AB0FF" : colors.linkFont || "#0A2885";
                              }
                              return (
                                <button key={btn.id} onClick={() => handlePreviewAction(btn.id)} style={{ padding: "8px 14px", borderRadius: 18, fontWeight: 600, fontSize: 13, cursor: "pointer", minWidth: 140, border, background, color, transition: "all 0.2s ease" }}>
                                  {btn.label}
                                </button>
                              );
                            })}
                          </div>
                        </div>
                      </div>
                    )}

                    {placement === "modal" && (
                      <div style={{ width: mobile ? "95%" : "80%", maxWidth: mobile ? "95%" : 500, margin: "auto", padding: mobile ? 16 : 20, background: dark ? darkColors.cardBackground || "#0B1120" : colors.cardBackground || "#FFFFFF", color: dark ? darkColors.cardFont || "#E5E7EB" : colors.cardFont || "#111827", borderRadius: 12, boxShadow: dark ? "0 2px 12px rgba(255,255,255,0.05)" : "0 2px 12px rgba(0,0,0,0.15)", transition: "all 0.3s ease", overflowY: mobile ? "auto" : "visible", maxHeight: mobile ? "85vh" : "auto" }}>
                        <h3 style={{ fontSize: mobile ? 16 : 18, fontWeight: 700, marginBottom: 8, textAlign: "center" }}>{previewContent.title}</h3>
                        <p style={{ fontSize: mobile ? 13 : 14, lineHeight: 1.5, marginBottom: 16, textAlign: "center" }}>{previewContent.description}</p>
                        <div style={{ display: "flex", justifyContent: "center", flexWrap: "wrap", gap: 8, marginTop: 10 }}>
                          {previewButtons.map((btn) => {
                            const bg = dark ? darkColors.buttonBackground || "#0F3CC9" : colors.buttonBackground || "#0F3CC9";
                            const font = dark ? darkColors.buttonFont || "#FFFFFF" : colors.buttonFont || "#FFFFFF";
                            const link = dark ? darkColors.linkFont || "#6AB0FF" : colors.linkFont || "#0A2885";
                            const isPrimary = btn.label.toLowerCase().includes("accept all");
                            return (
                              <button
                                key={btn.id}
                                onClick={() => handlePreviewAction(btn.id)}
                                style={{
                                  padding: mobile ? "8px 10px" : "8px 16px",
                                  borderRadius: 24,
                                  fontWeight: 600,
                                  fontSize: mobile ? 13 : 14,
                                  cursor: "pointer",
                                  minWidth: mobile ? 120 : 150,
                                  border: isPrimary ? "none" : `1px solid ${link}`,
                                  background: isPrimary ? bg : "transparent",
                                  color: isPrimary ? font : link,
                                  transition: "all 0.2s ease",
                                }}
                              >
                                {btn.label}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    )}

                    {placement === "tooltip" && (
                      <div style={{ position: mobile ? "relative" : "fixed", top: mobile ? "auto" : "50%", right: mobile ? "auto" : 40, transform: mobile ? "none" : "translateY(-50%)", background: dark ? darkColors.cardBackground || "#071026" : colors.cardBackground || "#fff", color: dark ? darkColors.cardFont || "#E6EEF5" : colors.cardFont || "#111827", borderRadius: 12, padding: mobile ? "12px 10px" : "12px 16px", fontSize: mobile ? 12 : 12.5, width: mobile ? "100%" : "auto", maxWidth: mobile ? "95%" : 280, margin: mobile ? "auto" : 0, boxShadow: dark ? "0 6px 18px rgba(0,0,0,0.6)" : "0 4px 14px rgba(0,0,0,0.15)", border: dark ? "1px solid rgba(255,255,255,0.05)" : "1px solid rgba(14,20,30,0.08)", zIndex: 9999, transition: "all 0.3s ease", textAlign: mobile ? "center" : "left" }}>
                        {!mobile && (
                          <div style={{ position: "absolute", top: "50%", left: -6, transform: "translateY(-50%)", width: 0, height: 0, borderTop: "6px solid transparent", borderBottom: "6px solid transparent", borderRight: `6px solid ${dark ? darkColors.cardBackground || "#071026" : colors.cardBackground || "#fff"}` }} />
                        )}
                        <div style={{ fontWeight: 700, fontSize: mobile ? 12.5 : 13, marginBottom: 6 }}>{previewContent.title}</div>
                        <div style={{ fontSize: mobile ? 12 : 12.5, color: dark ? darkColors.cardFont || "#9CA3AF" : colors.cardFont || "#4B5563", lineHeight: 1.5 }}>{previewContent.description}</div>
                        <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginTop: 10, justifyContent: mobile ? "center" : "flex-end" }}>
                          {previewButtons.map((btn) => {
                            const bg = dark ? darkColors.buttonBackground || "#0F3CC9" : colors.buttonBackground || "#0F3CC9";
                            const font = dark ? darkColors.buttonFont || "#FFFFFF" : colors.buttonFont || "#FFFFFF";
                            const link = dark ? darkColors.linkFont || "#6AB0FF" : colors.linkFont || "#0A2885";
                            const isPrimary = btn.label.toLowerCase().includes("accept all");
                            return (
                              <button
                                key={btn.id}
                                onClick={() => handlePreviewAction(btn.id)}
                                aria-label={btn.label}
                                style={{
                                  padding: mobile ? "6px 10px" : "8px 14px",
                                  borderRadius: 18,
                                  fontWeight: 600,
                                  fontSize: mobile ? 12 : 13,
                                  cursor: "pointer",
                                  minWidth: mobile ? 100 : 140,
                                  border: isPrimary ? "none" : `1px solid ${link}`,
                                  background: isPrimary ? bg : "transparent",
                                  color: isPrimary ? font : link,
                                  transition: "all 0.2s ease",
                                }}
                              >
                                {btn.label}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>

      <div className="footer" style={{ padding: 20 }}>
        <div className="template-consent">
          {activeTab === "report" ? (
            <ActionButton kind="primary" size="small" state="normal" label="Add cookie" onClick={handleAddButtonClick} />
          ) : (
            <>
              <ActionButton kind="secondary" size="small" state={publishLoading ? "disabled" : "normal"} label={publishLoading ? "Saving..." : "Save as draft"} onClick={() => handlePublish("DRAFT")} />
              <ActionButton kind="primary" size="small" state={publishLoading ? "disabled" : "normal"} label={publishLoading ? "Publishing..." : "Publish"} onClick={() => handlePublish("PUBLISHED")} />
            </>
          )}
        </div>
      </div>

      <AddEditCookieModal
        open={modalOpen}
        onClose={() => {
          setModalOpen(false);
          setFormErrors({ name: "", category: "", domainUrl: "", description: "", privacyPolicyUrl: "", expiresOn: "" });
        }}
        mode={modalMode}
        formState={formState}
        setFormState={setFormState}
        onAdd={handleModalAdd}
        onSave={handleModalSave}
        loading={modalLoading}
        categories={categories}
        formErrors={formErrors}
        clearFieldError={clearFieldError}
      />

      {leaveModalOpen && (
        <>
          <div className="modal-overlay" onClick={cancelLeave} style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.45)", zIndex: 1200 }} />
          <div className="modal-dialog" role="dialog" aria-modal="true" aria-labelledby="confirm-leave-title" style={{ position: "fixed", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", padding: 24, zIndex: 1201 }}>
            <div style={{ width: 600, maxWidth: "calc(100% - 48px)", borderRadius: 12, background: "#fff", boxShadow: "0 12px 40px rgba(0,0,0,0.18)", padding: 24 }}>
              <div style={{ marginBottom: 8 }}>
                <Text id="confirm-leave-title" appearance="heading-xs" color="primary-grey-100">Leaving will cancel the running scan</Text>
              </div>
              <div style={{ marginTop: 6 }}>
                <Text appearance="body-s" color="primary-grey-80">Going back now will cancel the running scan and you may lose data. Do you want to continue?</Text>
              </div>
              <div style={{ display: "flex", justifyContent: "flex-end", gap: 8, marginTop: 16 }}>
                <Button kind="secondary" size="medium" onClick={cancelLeave}>Cancel</Button>
                <Button kind="primary" size="medium" onClick={confirmLeave}>Go back</Button>
              </div>
            </div>
          </div>
        </>
      )}

      {scanInProgressModal && (
        <>
          <div className="modal-overlay" onClick={() => setScanInProgressModal(false)} style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.45)", zIndex: 1200 }} />
          <div className="modal-dialog" role="dialog" aria-modal="true" aria-labelledby="scan-progress-title" style={{ position: "fixed", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", padding: 24, zIndex: 1201 }}>
            <div style={{ width: 500, maxWidth: "calc(100% - 48px)", borderRadius: 12, background: "#fff", boxShadow: "0 12px 40px rgba(0,0,0,0.18)", padding: 24 }}>
              <div style={{ display: "flex", justifyContent: "center", marginBottom: 16 }}>
                <div style={{ width: 56, height: 56, borderRadius: "50%", background: "#FEF3C7", display: "flex", alignItems: "center", justifyContent: "center" }}>
                  <span style={{ fontSize: 28 }}>⚠️</span>
                </div>
              </div>
              <div style={{ textAlign: "center", marginBottom: 8 }}>
                <Text id="scan-progress-title" appearance="heading-xs" color="primary-grey-100">Scan In Progress</Text>
              </div>
              <div style={{ textAlign: "center", marginTop: 8 }}>
                <Text appearance="body-s" color="primary-grey-80">A scan is currently in progress. Please wait for the scan to complete before publishing, or cancel the scan to proceed.</Text>
              </div>
              <div style={{ display: "flex", justifyContent: "center", gap: 12, marginTop: 20 }}>
                <Button kind="primary" size="medium" onClick={() => setScanInProgressModal(false)}>Go Back</Button>
              </div>
            </div>
          </div>
        </>
      )}
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

export default Cookie;
