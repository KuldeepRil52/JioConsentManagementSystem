// src/components/RegisteredCookies.js
import React, { useState, useEffect, useRef, useCallback, useMemo } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useSelector } from "react-redux";
import { ActionButton, Icon, Text } from "../custom-components";
import { IcCode, IcTicketDetails, IcSort, IcChevronDown } from "../custom-components/Icon";
import {
  IcTrash,
  IcEditPen,
  IcSuccessColored,
  IcEdit,
  IcWarning,
  IcClose,
} from "../custom-components/Icon";
import "./RegisteredCookies.css";
import { Slide, toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import CustomToast from "./CustomToastContainer";
import cookieIntegrationContentST from "bundle-text:../assets/ST/CookieIntegration.txt";
import cookieIntegrationContentSIT from "bundle-text:../assets/SIT/CookieIntegration.txt";
import config from "../utils/config";
import { isSandboxMode, sandboxAPICall } from "../utils/sandboxMode";

/* -------------------- Config -------------------- */
const BASE = config.cookie_base;
const START_SCAN_URL = `${BASE}/scan`;
const COOKIE_TEMPLATES_TENANT = `${BASE}/cookie-templates/tenant`;
const COOKIE_TEMPLATE_DETAILS = (scanId) =>
  `${BASE}/cookie-templates/tenant?scanId=${encodeURIComponent(scanId)}`;
const emptyIllustration = new URL("../assets/dashboard.svg", import.meta.url).href;

const STORAGE_ROWS_KEY = "registered_cookies_rows_vfinal";
const STORAGE_DOMAIN_MAP_KEY = "registered_cookies_domain_map_vfinal";

/* -------------------- helpers -------------------- */
async function safeFetchJson(url, opts = {}, timeoutMs = 15000) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const resp = await fetch(url, { ...opts, signal: controller.signal });
    clearTimeout(timer);

    if (resp.status === 401) {
      alert("Session expired. Please log in again.");
      window.location.href = "/adminLogin";
      return {};
    }

    if (!resp.ok) {
      const txt = await resp.text().catch(() => "");
      throw new Error(`HTTP ${resp.status} ${txt}`);
    }

    const txt = await resp.text().catch(() => "");
    return txt ? JSON.parse(txt) : {};
  } catch (err) {
    clearTimeout(timer);
    throw err;
  }
}

const loadFromStorage = (key, fallback) => {
  try {
    const raw = sessionStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
};

/* -------------------- Component -------------------- */
export default function RegisteredCookies() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const tenantId = useSelector((s) => s.common?.tenant_id);
  const token = useSelector((s) => s.common?.session_token);

  // ✅ States
  const [rows, setRows] = useState(() => loadFromStorage(STORAGE_ROWS_KEY, []));
  const [templateName, setTemplateName] = useState("");
  const [scanOpen, setScanOpen] = useState(false);
  const [embedOpen, setEmbedOpen] = useState(false);
  const [embedDomain, setEmbedDomain] = useState(null);
  const [domain, setDomain] = useState("");
  const [subDomains, setSubDomains] = useState([""]);
  const [scanLoading, setScanLoading] = useState(false);
  const [scanError, setScanError] = useState("");
  const [summary, setSummary] = useState({
    total: 0,
    published: 0,
    draft: 0,
  });
  const [loading, setLoading] = useState(true);
  const [scanning, setScanning] = useState(false);
  const businessId = useSelector((s) => s.common?.business_id);


  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState('asc');

  const [expandedMap, setExpandedMap] = useState({});
  const [versionsMap, setVersionsMap] = useState({});
  const [loadingVersionsMap, setLoadingVersionsMap] = useState({});

  const mountedRef = useRef(true);
  useEffect(() => {
    try {
      const raw = sessionStorage.getItem("registercookies_flash_toast");
      if (!raw) return;
      const flash = JSON.parse(raw);
      sessionStorage.removeItem("registercookies_flash_toast");

      if (flash?.type === "success" && flash?.message) {
        toast.success(<CustomToast type="success" message={flash.message} />, { icon: false });
      } else if (flash?.type === "error" && flash?.message) {
        toast.error(<CustomToast type="error" message={flash.message} />, { icon: false });
      }
    } catch {
      sessionStorage.removeItem("registercookies_flash_toast");
    }
  }, []);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const updateSummary = (list) => {
    const total = list.length;
    const published = list.filter((g) => g.status === "Active").length;
    const draft = list.filter((g) => g.status === "Draft").length;
    setSummary({ total, published, draft });
  };

  const handleSort = (column) => {
    if (sortColumn === column) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortColumn(column);
      setSortDirection('asc');
    }
  };

  const renderSortIcon = (column) => {
    return <Icon ic={<IcSort />} size="small" color="black" />;
  };

  // ✅ Stable sort: Default sort by createdAt (newest first) to prevent shuffling on update
  const sortedRows = useMemo(() => {
    // If user has selected a column, use that; otherwise default to createdAt (newest first)
    if (sortColumn) {
      return [...rows].sort((a, b) => {
        const aValue = (a[sortColumn] || '').toString().toLowerCase();
        const bValue = (b[sortColumn] || '').toString().toLowerCase();

        if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
        if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
        return 0;
      });
    }

    // Default: sort by createdAt (newest first) - stable order based on creation time
    return [...rows].sort((a, b) => {
      const aDate = a.createdAt ? new Date(a.createdAt).getTime() : 0;
      const bDate = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      return bDate - aDate; // Newest first (descending)
    });
  }, [rows, sortColumn, sortDirection]);

  /* -------------------- NEW FETCH: Single API only -------------------- */
const fetchRegisteredList = useCallback(async () => {
  setLoading(true);

  try {
    let res;
    
    // In sandbox mode, use mock data
    if (isSandboxMode()) {
      const finalBusinessId = businessId || 'sandbox-business-id';
      const finalTenantId = tenantId || 'sandbox-tenant-id';
      const finalToken = token || 'sandbox-session-token-12345';
      
      const mockResponse = await sandboxAPICall(
        `${BASE}/cookie-templates/tenant?businessId=${encodeURIComponent(finalBusinessId)}`,
        'GET',
        {},
        {
          "Content-Type": "application/json",
          "X-Tenant-ID": finalTenantId,
          "x-session-token": finalToken,
        }
      );
      
      // sandboxAPICall returns { status, data }, extract data
      res = mockResponse.data || mockResponse;
    } else {
      // 1️⃣ Fetch templates filtered by businessId
      res = await safeFetchJson(
        `${BASE}/cookie-templates/tenant?businessId=${encodeURIComponent(businessId)}`,
        {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
            "X-Tenant-ID": tenantId,
            "x-session-token": token,
          },
        }
      );
    }

    const templates = Array.isArray(res)
      ? res
      : res.templates || res.data || [];

    if (!templates.length) {
      setRows([]);
      setSummary({ total: 0, published: 0, draft: 0 });
      setLoading(false);
      return;
    }

    // 2️⃣ Fetch detailed scan data (also filtered by businessId)
    const detailResults = await Promise.allSettled(
      templates.map((tpl) => {
        const scanId = tpl.scanId || tpl.transactionId || tpl.id;
        if (!scanId) return Promise.resolve({});

        // In sandbox mode, use mock data
        if (isSandboxMode()) {
          const finalBusinessId = businessId || 'sandbox-business-id';
          const finalTenantId = tenantId || 'sandbox-tenant-id';
          const finalToken = token || 'sandbox-session-token-12345';
          
          return sandboxAPICall(
            `${BASE}/cookie-templates/tenant?scanId=${encodeURIComponent(scanId)}&businessId=${encodeURIComponent(finalBusinessId)}`,
            'GET',
            {},
            {
              "Content-Type": "application/json",
              "X-Tenant-ID": finalTenantId,
              "x-session-token": finalToken,
            }
          ).then(mockResponse => {
            // Extract data from mock response
            // sandboxAPICall returns { status, data }, and the handler returns { status, data: { ...template } }
            // So mockResponse.data is { ...template } for detail requests
            return mockResponse.data || mockResponse;
          }).catch(() => ({}));
        } else {
          return safeFetchJson(
            `${BASE}/cookie-templates/tenant?scanId=${encodeURIComponent(
              scanId
            )}&businessId=${encodeURIComponent(businessId)}`,
            {
              method: "GET",
              headers: {
                "Content-Type": "application/json",
                "X-Tenant-ID": tenantId,
                "x-session-token": token,
              },
            }
          ).catch(() => ({}));
        }
      })
    );

    // 3️⃣ Merge list + details
    const enriched = templates.map((tpl, i) => {
      const detailRes = detailResults[i];
      const detailedData =
        detailRes.status === "fulfilled"
          ? detailRes.value?.data || detailRes.value || {}
          : {};

      const fullTemplate = { ...tpl, ...detailedData };

      // ⭐ Extract templateId once here
      const templateId = fullTemplate.templateId;

      // ⭐ Extract domain
      let domain =
        fullTemplate.templateName ||
        fullTemplate.url ||
        fullTemplate.domain ||
        "—";

      if (domain && domain !== "—") {
        try {
          const u = new URL(
            domain.startsWith("http") ? domain : `https://${domain}`
          );
          domain = u.hostname.replace(/^www\./, "");
        } catch {
          domain = domain.replace(/^https?:\/\//, "").replace(/\/$/, "");
        }
      }

      // ⭐ Cookie count (robust fallback chain to avoid null/"null" from API)
      const toSafeCount = (value) => {
        if (value === null || value === undefined || value === "") return null;
        if (typeof value === "string" && value.trim().toLowerCase() === "null") return null;
        const parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : null;
      };

      const prefCount = (fullTemplate.preferencesWithCookies || []).reduce(
        (sum, pref) =>
          sum + ((Array.isArray(pref.cookies) && pref.cookies.length) || 0),
        0
      );
      const topLevelCount = Array.isArray(fullTemplate.cookies)
        ? fullTemplate.cookies.length
        : 0;
      const totalFromPreferences = Array.isArray(fullTemplate.preferences)
        ? fullTemplate.preferences.reduce((sum, pref) => {
            const prefCountValue =
              toSafeCount(pref?.cookieCount) ??
              toSafeCount(pref?.totalCookies) ??
              toSafeCount(pref?.cookiesCount) ??
              0;
            return sum + prefCountValue;
          }, 0)
        : 0;
      const computedCount = prefCount + topLevelCount;
      const apiCount =
        toSafeCount(fullTemplate.totalCookies) ??
        toSafeCount(fullTemplate.totalCookieCount) ??
        toSafeCount(fullTemplate.cookiesCount);
      const totalCookies = apiCount ?? (computedCount > 0 ? computedCount : totalFromPreferences);

      // ⭐ Dates
      let lastScanned = "—";
      let nextScan = "—";
      if (fullTemplate.updatedAt) {
        const d = new Date(fullTemplate.updatedAt);
        if (!isNaN(d)) {
          const fmt = (d) =>
            `${String(d.getDate()).padStart(2, "0")}/${String(
              d.getMonth() + 1
            ).padStart(2, "0")}/${String(d.getFullYear()).slice(-2)}`;
          lastScanned = fmt(d);
          const next = new Date(d);
          next.setMonth(next.getMonth() + 1);
          nextScan = fmt(next);
        }
      }

      // ⭐ Status mapping
      const rawStatus =
        fullTemplate.status || fullTemplate.templateStatus || "INACTIVE";
      const up = rawStatus.toUpperCase();
      let finalStatus = "Inactive";

      if (["ACTIVE", "PUBLISHED", "SUCCESS"].some((k) => up.includes(k)))
        finalStatus = "Active";
      else if (["DRAFT", "IN_PROGRESS"].some((k) => up.includes(k)))
        finalStatus = "Draft";

      const policy = fullTemplate.documentMeta?.name || "—";

      // ⭐ FINAL ENRICHED ROW OBJECT (includes templateId)
      return {
        templateId: templateId || "—", // << ⭐ NEW FIELD
        domain,
        lastScanned,
        scanFrequency: "Monthly",
        nextScan,
        totalCookies,
        policy,
        status: finalStatus,
        // ✅ Store createdAt for stable ordering (used as fallback sort key)
        createdAt: fullTemplate.createdAt || fullTemplate.updatedAt || null,
        scans: [
          {
            scanId: fullTemplate.scanId,
            templateId: templateId,
            status: finalStatus,
            totalCookies,
          },
        ],
      };
    });

    setRows(enriched);
    updateSummary(enriched);
  } catch (err) {
    console.error("❌ fetchRegisteredList failed:", err);
  } finally {
    setLoading(false);
  }
}, [tenantId, token, businessId]);





  /* -------------------- Auto refresh -------------------- */
  const refreshParam = searchParams.get("refresh");
 useEffect(() => {
  // In sandbox mode, fetch even if credentials are not loaded
  if (isSandboxMode()) {
    fetchRegisteredList();
    if (refreshParam === "true") fetchRegisteredList();
  } else if (tenantId && token && businessId) {
    fetchRegisteredList();
    if (refreshParam === "true") fetchRegisteredList();
  }
}, [tenantId, token, businessId, fetchRegisteredList, refreshParam]);


  /* -------------------- Scan website -------------------- */
  /* -------------------- Scan website -------------------- */

const handleScanSubmit = async () => {

  let mainDomain = domain.trim();

  if (!mainDomain) {

    setScanError("Please enter a valid domain URL");

    return;

  }



  // Ensure https:// is present

  if (!/^https?:\/\//i.test(mainDomain)) {

    mainDomain = `https://${mainDomain}`;

    setDomain(mainDomain);

    setTemplateName(mainDomain);

  }



  // Validate main domain format

  try {

    new URL(mainDomain);

  } catch {

    setScanError("Please enter a valid domain URL (e.g., https://example.com)");

    return;

  }



  // Block jio URLs

  if (/jio/i.test(mainDomain)) {

    setScanError("Websites containing 'Jio' are currently not supported.");

    return;

  }



  // FORMAT SUBDOMAINS

  const validSubs = subDomains

    .filter((s) => s.trim() !== "")

    .map((s) => {

      let formatted = s.trim();

      if (!/^https?:\/\//i.test(formatted)) formatted = `https://${formatted}`;

      return formatted.replace(/\/+$/, "");

    });



  // --- SUBDOMAIN VALIDATION ---

  const isValidDomainFormat = (url) => {

    try {

      const u = new URL(url.startsWith("http") ? url : `https://${url}`);

      const host = u.hostname;



      if (!host.includes(".")) return false;



      const tld = host.split(".").pop();

      if (!tld || tld.length < 2) return false;



      return true;

    } catch {

      return false;

    }

  };



  for (const sd of validSubs) {

    if (!isValidDomainFormat(sd)) {

      setScanError(`Invalid subdomain format: ${sd}`);

      return;

    }

  }



  // All validations passed

  setScanError("");

  setScanLoading(true);

  setScanning(true);



  try {

    const payload = { url: mainDomain, subDomain: validSubs };



    const res = await safeFetchJson(START_SCAN_URL, {

      method: "POST",

      headers: {

        "Content-Type": "application/json",

        "X-Tenant-ID": tenantId,

        "x-session-token": token,

      },

      body: JSON.stringify(payload),

    });



    const txId = res.transactionId || res.txId || res.id || `tx-${Date.now()}`;



    navigate(

      `/registercookies/report/${encodeURIComponent(txId)}?txId=${encodeURIComponent(

        txId

      )}&url=${encodeURIComponent(mainDomain)}`

    );



    setScanOpen(false);

    setScanning(false);



  } catch (err) {

    console.error("❌ Scan start error:", err);



    // ---- API ERROR PARSING (Show ONLY details) ----

    let apiMsg = err.message || "";

    try {

      const jsonStart = apiMsg.indexOf("{");

      if (jsonStart !== -1) {

        const jsonStr = apiMsg.slice(jsonStart);

        const parsed = JSON.parse(jsonStr);



        if (parsed.details) {

          setScanError(parsed.details);

        } else if (parsed.message) {

          setScanError(parsed.message);

        } else {

          setScanError("Scan failed. Please try again.");

        }

      } else {

        setScanError("Scan failed. Please try again.");

      }

    } catch {

      setScanError("Scan failed. Please try again.");

    }



    setScanning(false);



  } finally {

    setScanLoading(false);

  }

};



  /* -------------------- NEW: Toggle Expand Handler -------------------- */
  const handleToggleExpand = (idKey, r) => {
    setExpandedMap((prev) => {
      const isOpen = !!prev[idKey];
      return isOpen ? {} : { [idKey]: true }; // close others and open only one
    });

    const templateId = r.scans?.[0]?.templateId;
    if (!expandedMap[idKey] && templateId) {
      fetchVersionHistory(idKey, templateId);
    }
  };
  const fetchVersionHistory = async (idKey, templateId) => {
    setLoadingVersionsMap((prev) => ({ ...prev, [idKey]: true }));
    try {
      const res = await safeFetchJson(
        `${BASE}/cookie-templates/${encodeURIComponent(templateId)}/history`,
        {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
            "X-Tenant-ID": tenantId,
            "x-session-token": token,
          },
        }
      );

      const versionData =
        Array.isArray(res) && res.length ? res : res.data || res.history || [];

      const mapped = versionData.map((v, i) => ({
        versionNumber: v.version || `Version ${i + 1}`,
        scanId: v.scanId || v.transactionId || "—",
        scanDate: v.updatedAt || v.createdAt || "—",
        status: v.status || v.templateStatus || (v.active ? "Active" : "Inactive"),
      }));

      setVersionsMap((prev) => ({ ...prev, [idKey]: mapped }));
    } catch (err) {
      console.error("⚠️ Version history fetch failed:", err);
      setVersionsMap((prev) => ({ ...prev, [idKey]: [] }));
    } finally {
      setLoadingVersionsMap((prev) => ({ ...prev, [idKey]: false }));
    }
  };



  /* -------------------- Edit Template -------------------- */
  const handleEditNavigate = async (scanId, templateId, domain) => {
    if (!scanId) return;
    try {
      const res = await safeFetchJson(COOKIE_TEMPLATE_DETAILS(scanId), {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          "X-Tenant-ID": tenantId,
          "x-session-token": token,
        },
      });
      const templateData = Array.isArray(res) ? res[0] : res?.data || res;
      navigate(
        `/registercookies/report/${encodeURIComponent(scanId)}?txId=${encodeURIComponent(scanId)}&url=${encodeURIComponent(
          domain
        )}`,
        { state: { templateData } }
      );
    } catch (err) {
      console.error("❌ Failed to fetch template details:", err);
    }
  };

  /* -------------------- Embed Modal -------------------- */
  function EmbedModal({ open, onClose }) {
    const [integration, setIntegration] = useState("Java Script React JS");
    if (!open) return null;
    const handleDownload = () => {
      try {
        const environment = process.env.REACT_APP_ENVIRONMENT;
        const text = environment === "NONPROD" ? cookieIntegrationContentSIT : cookieIntegrationContentST;
        
        const blob = new Blob([text], { type: "application/javascript" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = "CookieIntegration.js";
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
        onClose();
      } catch (err) {
        console.error("Download failed:", err);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={`Failed to download file: ${err.message}`}
            />
          ),
          { icon: false }
        );
      }
    };
    return (
      <div className="modal-outer-container">
        <div className="modal-container embed-modal ">
          <div className="modal-close-btn-container-cookie">
            <ActionButton icon={<IcClose />} kind="tertiary" size="small" onClick={onClose} />
          </div>
          <div style={{ alignItems: "center", gap: "12px" }}>
            <div className="embed-icon">{"</>"}</div>
            <div>
              <Text appearance="heading-xs">Embed Modal design in your code</Text>
            <br></br>
              <Text appearance="body-s">
                Download the script file in the desired integration format and add it to your file.
              </Text>
            </div>
          </div>
          <div className="dropdown-group">
            <Text appearance="body-s">Select Integration type (Required)</Text>
            <select value={integration} onChange={(e) => setIntegration(e.target.value)}>
              <option>Java Script React JS</option>
            </select>
          </div>
          <div className="modal-add-btn-container-tem">
            <ActionButton label="Not now" kind="secondary" onClick={onClose} />
            <ActionButton label="Download file" kind="primary" onClick={handleDownload} />
          </div>
        </div>
      </div>
    );
  }

  /* -------------------- Action Buttons -------------------- */
  const ActionButtons = ({ r }) => (
    <div className="action-icons">
      <Icon ic={<IcCode />} size={24} color="primary_grey_80" title="Embed" onClick={() => { setEmbedDomain(r.domain); setEmbedOpen(true); }} />
      <Icon ic={<IcEditPen />} size={24} color="primary_grey_80" title="Edit" onClick={() => { const scanId = r.scans?.[0]?.scanId; const templateId = r.scans?.[0]?.templateId; handleEditNavigate(scanId, templateId, r.domain); }} />
      <Icon ic={<IcTicketDetails />} size={24} color="primary_grey_80" title="Logs" onClick={() => { const scanId = r.scans?.[0]?.scanId; const templateId = r.scans?.[0]?.templateId; navigate(`/cookieslogs?scanId=${encodeURIComponent(scanId)}&templateId=${encodeURIComponent(templateId || "")}&url=${encodeURIComponent(r.domain)}`); }} />
      {/* <Icon ic={<IcTrash />} size={24} color="feedback_error_50" title="Delete" onClick={() => toast.info(<CustomToast type="info" message="Delete clicked" />)} /> */}
    </div>
  );



  /* -------------------- Render -------------------- */
  return (
    <div className="configurePage-rc">
      <div className="rc-page">

        {/* Header */}
        <div className="main-heading-ct-rc">
          <div className="rc-heading">
            <Text appearance="heading-s" color="primary-grey-100">
              Registered cookies
            </Text>
            <div className="tag-rc">
              <Text appearance="body-xs-bold" color="primary-grey-80">
                Cookies
              </Text>
            </div>
          </div>

          <div className="scan-btn-container">
            <ActionButton
              label="Scan website for cookies"
              onClick={() => setScanOpen(true)}
              kind="primary"
              size="medium"
              className="scan-website-btn"
            />
          </div>
        </div>


        {/* Summary */}
        <div className="cookie-summary-group">
          <div className="cookie-summary total">
            <div className="icon-wrapper">
              <IcTicketDetails size={24} color="#0057FF" />
            </div>
            <div className="summary-text">
              <span className="count">{summary.total}</span>
              <Text appearance="body-xs" color="primary-grey-80">Total</Text>
            </div>
          </div>
          <div className="cookie-summary published">
            <div className="icon-wrapper">
              <IcSuccessColored size={24} />
            </div>
            <div className="summary-text">
              <span className="count">{summary.published}</span>
              <Text appearance="body-xs" color="primary-grey-80">Active</Text>
            </div>
          </div>
          <div className="cookie-summary draft">
            <div className="icon-wrapper">
              <IcEdit size={24} color="#F16529" />
            </div>
            <div className="summary-text">
              <span className="count">{summary.draft}</span>
              <Text appearance="body-xs" color="primary-grey-80">Draft</Text>
            </div>
          </div>
        </div>

        {/* Table */}
        <div className="rc-content">
          <div className="rc-business-table-container">
            {loading ? (
              <div className="loader-container">
                <div className="loader-spinner" />
                <div className="loader-text">Loading Templates</div>
              </div>
            ) : scanning ? (
              <p style={{ textAlign: "center", padding: "20px" }}>
                Scanning website... please wait.
              </p>
            ) : sortedRows.length ? (
              <table className="rc-business-table">
                <thead>
                  <tr>
                    {[
                      { label: "Domain name", key: "domain" },
                      { label: "Last scanned", key: "lastScanned" },
                      { label: "Scan frequency", key: "scanFrequency" },
                      { label: "Next scan", key: "nextScan" },
                      { label: "Total cookies", key: "totalCookies" },
                      { label: "Template ID", key: "templateId" },
                  // { label: "Policy", key: "policy" },
                      { label: "Status", key: "status" },
                     

                    ].map((header, i) => (
                      <th key={i} onClick={() => handleSort(header.key)} style={{ cursor: 'pointer' }}>
                        <div
                          style={{
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "space-between",
                            width: "100%",
                          }}
                        >
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            {header.label}
                          </Text>
                          {renderSortIcon(header.key)}
                        </div>
                      </th>
                    ))}
                    <th>
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "space-between",
                          width: "100%",
                        }}
                      >
                        <Text appearance="body-xs-bold" color="primary-grey-80">
                          Action
                        </Text>
                      </div>
                    </th>
                  </tr>
                </thead>

                <tbody>
                  {sortedRows.map((r, idx) => {
                    // ✅ updated unique idKey
                    const idKey = `${r.domain || "row"}-${r.scans?.[0]?.scanId || idx}`;
                    const expanded = !!expandedMap[idKey];
                    const versions = versionsMap[idKey] || [];
                    const loadingVersions = !!loadingVersionsMap[idKey];

                    return (
                      <React.Fragment key={idKey}>
                        {/* Parent domain row */}
                        <tr className="domain-row-rc">
                          <td className="domain-cell">
                            <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                              {/* ✅ Only icon triggers expand */}
                              <span
                                className={`chevron-icon ${expanded ? "rotated" : ""}`}
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleToggleExpand(idKey, r);
                                }}
                              >
                                <Icon ic={<IcChevronDown />} size={12} color="primary-grey-60" />
                              </span>

                              <Text appearance="body-xs" color="primary-grey-100">
                                {r.domain}
                              </Text>
                            </div>
                          </td>



                          <td>
                            <Text
                              appearance="body-xs"
                              color="primary-grey-100"

                            >
                              {r.lastScanned}
                            </Text>
                          </td>

                          <td>
                            <Text
                              appearance="body-xs"
                              color="primary-grey-100"

                            >
                              {r.scanFrequency}
                            </Text>
                          </td>

                          <td>
                            <Text
                              appearance="body-xs"
                              color="primary-grey-100"

                            >
                              {r.nextScan}
                            </Text>
                          </td>

                          <td>
                            <Text
                              appearance="body-xs"
                              color="primary-grey-100"

                            >
                              {Number.isFinite(Number(r.totalCookies)) ? Number(r.totalCookies) : 0}
                            </Text>
                          </td>

                          {/* <td
                            title={r.policy && r.policy !== "—" ? r.policy : ""}
                            style={{
                              maxWidth: 200,
                              overflow: "hidden",
                              textOverflow: "ellipsis",
                              whiteSpace: "nowrap",
                            }}
                          >
                            <Text
                              appearance="body-xs"
                              color="primary-grey-100"

                            >
                              {r.policy && r.policy !== "—"
                                ? r.policy.length > 50
                                  ? r.policy.slice(0, 47) + "..."
                                  : r.policy
                                : "—"}
                            </Text>
                          </td> */}
<td>
  <Text appearance="body-xs" color="primary-grey-100">
    {r.templateId || "—"}
  </Text>
</td>

                          <td>
                            <div
                              className={`activebadge ${r.status === "Active" ? "published" : ""
                                }`}
                            >
                              <Text
                                appearance="body-xs"
                                color="primary-grey-100"

                              >
                                {r.status}
                              </Text>
                            </div>
                          </td>

                          <td>
                            <ActionButtons r={r} />
                          </td>

                        </tr>

                        {/* Expanded version details */}
                        {expanded && (
                          <tr className="version-row">
                            <td colSpan="8" style={{ background: "#fafafa", padding: "10px 20px" }}>
                              {loadingVersions ? (
                                <p style={{ textAlign: "center", color: "#666" }}>
                                  Loading version history...
                                </p>
                              ) : (
                                <table className="nested-version-table">
                                  <thead>
                                    <tr>
                                      <th>
                                        <Text appearance="body-s-bold" color="primary-grey-80">
                                          Version number
                                        </Text>
                                      </th>
                                      <th>
                                        <Text appearance="body-s-bold" color="primary-grey-80">
                                          Scan ID
                                        </Text>
                                      </th>
                                      <th>
                                        <Text appearance="body-s-bold" color="primary-grey-80">
                                          Scan date
                                        </Text>
                                      </th>
                                      <th>
                                        <Text appearance="body-s-bold" color="primary-grey-80">
                                          Status
                                        </Text>
                                      </th>
                                      <th>
                                        <Text appearance="body-s-bold" color="primary-grey-80">
                                          Action
                                        </Text>
                                      </th>
                                    </tr>
                                  </thead>

                                  <tbody>
                                    {versions.length ? (
                                      versions.map((v, vidx) => (
                                        <tr key={`${idKey}-v-${vidx}`}>
                                          <td>
                                            <Text appearance="body-s" color="primary-grey-100">
                                              {v.versionNumber}
                                            </Text>
                                          </td>

                                          <td>
                                            <Text appearance="body-s" color="primary-grey-100">
                                              {v.scanId}
                                            </Text>
                                          </td>

                                          <td>
                                            <Text appearance="body-s" color="primary-grey-100">
  {v.scanDate !== "—"
    ? new Date(v.scanDate).toLocaleDateString("en-GB", {
        day: "2-digit",
        month: "2-digit",
        year: "2-digit",
      })
    : "—"}
</Text>
                                          </td>

                                          <td>
                                            <div
                                              className={`activebadge ${v.status === "Active" ? "published" : ""
                                                }`}
                                            >
                                              <p className="activebadge-text">{v.status}</p>
                                            </div>
                                          </td>
                                          <td>
                                            <div className="dropdown-action-icons">
                                              <Icon
                                                ic={<IcCode />}
                                                size={18}
                                                color='primary_grey_80'

                                                title="Embed"
                                                onClick={() => {
                                                  setEmbedDomain(r.domain);
                                                  setEmbedOpen(true);
                                                }}
                                              />
                                              <Icon
                                                ic={<IcEditPen />}
                                                size={18}
                                                color='primary_grey_80'
                                                title="Edit"
                                                onClick={() => {
                                                  const scanId = r.scans?.[0]?.scanId;
                                                  const templateId = r.scans?.[0]?.templateId;
                                                  if (!scanId) return;
                                                  navigate(
                                                    `/registercookies/report/${encodeURIComponent(scanId)}?txId=${encodeURIComponent(scanId)}&templateId=${encodeURIComponent(templateId || "")}&url=${encodeURIComponent(r.domain)}`
                                                  );
                                                }}
                                              />
                                            </div>
                                          </td>

                                        </tr>
                                      ))
                                    ) : (
                                      <tr>
                                        <td colSpan="5" style={{ textAlign: "center", color: "#888" }}>
                                          No version history available
                                        </td>
                                      </tr>
                                    )}
                                  </tbody>
                                </table>
                              )}
                            </td>
                          </tr>
                        )}
                      </React.Fragment>
                    );
                  })}
                </tbody>
              </table>
            ) : (
              <div className="empty-state">
                <div className="empty-box">
                  <img
                    src={emptyIllustration}
                    alt="Empty dashboard illustration"
                    className="empty-illustration"
                  />
                  <Text appearance="body-s" color="primary-grey-80">
                    You have not scanned any website yet. <br />
                    Scan a website to get started
                  </Text>
                  <ActionButton
                    label="Scan website for cookies"
                    onClick={() => setScanOpen(true)}
                    kind="primary"
                    size="small"
                  />
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Scan Modal */}
        {scanOpen && (
          <div className="modal-outer-container">
            <div className="modal-container" style={{padding: '32px'}}>
              <div className="modal-close-btn-container-re">
                <ActionButton
                  onClick={() => setScanOpen(false)}
                  icon={<IcClose />}
                  kind="tertiary"
                  size="small"
                />
              </div>

              <Text appearance="heading-xs" className="modal-title">
                Scan website for cookies
              </Text>
              <Text appearance="body-xs" className="modal-desc" color="primary-grey-80">
                Enter the URL to scan. Generating the report may take some time, depending on the website size..
              </Text>

              <div className="dropdown-group" style={{ marginTop: 10 }}>
                <Text appearance="body-xs" color="primary-grey-80">
                  Domain URL (Required)
                </Text>
                <input
                  id="domain-input"
                  value={domain}
                  onChange={(e) => setDomain(e.target.value.trim())}
                />
              </div>

              {subDomains.map((s, i) => (
                <div className="dropdown-group subdomain-row-rc-rc" key={i}>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <div style={{ flex: 1 }}>
                      <label htmlFor={`subdomain-${i}`}>
                        <Text appearance="body-xs" color="primary-grey-80">
                          Sub Domain
                        </Text>
                      </label>
                      <input
                        id={`subdomain-${i}`}
                        value={s}
                        onChange={(e) =>
                          setSubDomains((prev) =>
                            prev.map((x, idx) => (idx === i ? e.target.value.trim() : x))
                          )
                        }
                      />
                    </div>

                    {subDomains.length > 1 && (
                      <ActionButton
                        icon={<IcClose />}
                        kind="tertiary"
                        size="small"
                        onClick={() =>
                          setSubDomains((prev) => prev.filter((_, idx) => idx !== i))
                        }
                      />
                    )}
                  </div>
                </div>
              ))}

              <button
                type="button"
                className="add-subdomain"
                onClick={() => setSubDomains([...subDomains, ""])}
              >
                <Text appearance="body-m-bold" color="primary-60">
                  Add another subdomain
                </Text>
              </button>

              {scanError && <p className="error-text">{scanError}</p>}

              <div className="modal-add-btn-container-tem">
                <ActionButton
                  label={scanLoading ? "Starting..." : "Scan"}
                  kind="primary"
                  onClick={handleScanSubmit}
                  disabled={scanLoading}
                />
              </div>
            </div>
          </div>
        )}
        {/* Embed Modal (rendered when embedOpen true) */}
        {embedOpen && (
          <EmbedModal
            open={embedOpen}
            onClose={() => setEmbedOpen(false)}
            siteDomain={embedDomain}
            tenantId={tenantId}
          />

        )}
        <ToastContainer
          position="bottom-left"
          autoClose={3000}
          hideProgressBar={false}
          closeButton={false}
          draggable
          pauseOnHover
          transition={Slide}
          icon={false}
          toastClassName="!bg-transparent !shadow-none !p-0 !border-0"
          bodyClassName="!m-0 !p-0"
        />

      </div>
    </div>
  );
}
