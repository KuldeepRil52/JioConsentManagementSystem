// src/pages/CookieLogs.js
import React, { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useSelector } from "react-redux";
import "../styles/logsCookie.css";
import { Icon, Text, Button } from "../custom-components";
import {
  IcVisible,
  IcSuccessColored,
  IcError,
  IcWarningColored,
  IcDownload,
  IcFilter,
  IcSearch,
  IcCookies,
  IcStatusSuccessful,
  IcSort,
} from "../custom-components/Icon";
import { exportToCSV } from "../utils/csvExport";
import config from "../utils/config";
import { formatToIST } from "../utils/dateUtils";

const BASE_URL = config.cookie_base;
const BASE_URL_AUDIT = "https://api.jcms-st.jiolabs.com:8443/audit/v1/consent/checkIntegrity";
/* STATUS BADGE */
function StatusBadge({ status }) {
  const cls =
    status === "Accepted"
      ? "status-accepted"
      : status === "Partially accepted"
      ? "status-partial"
      : status === "Rejected"
      ? "status-rejected"
      : status === "Revoked"
      ? "status-revoked"
      : status === "No Action"
      ? "status-noaction"
      : "status-expired";

  return <span className={`status-badge ${cls}`}>{status}</span>;
}

/* KPI BOX */
function KPIBox({ label, value, icon }) {
  let bgClass = "kpi-default";
  if (label === "All accepted" || label === "Partially accepted") bgClass = "kpi-accepted";
  if (label === "All rejected") bgClass = "kpi-rejected";
  if (label === "Expired") bgClass = "kpi-noaction";

  return (
    <div className={`cookie-logs-kpi ${bgClass}`}>
      <div className="cookie-logs-kpi-icon">
        <Icon ic={icon} size="xl" color="primary" />
      </div>
      <div>
        <div className="cookie-logs-kpi-value">{value}</div>
        <div className="cookie-logs-kpi-label">{label}</div>
      </div>
    </div>
  );
}

export default function CookieLogs() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const businessId = useSelector((s) => s.common?.business_id);
  const tenantId = useSelector((s) => s.common?.tenant_id);
  const token = useSelector((s) => s.common?.session_token);

  const templateId = searchParams.get("templateId");
  const scanId = searchParams.get("scanId");

  const [dashboardData, setDashboardData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState("");
  const [sortConfig, setSortConfig] = useState({ key: "date", direction: "desc" });

  /* FILTERS */
  const [filterDrawerOpen, setFilterDrawerOpen] = useState(false);
  const [filters, setFilters] = useState({ status: [], dateFrom: "", dateTo: "" });

  const activeFilterCount =
    filters.status.length + (filters.dateFrom ? 1 : 0) + (filters.dateTo ? 1 : 0);

  const [tableScrolled, setTableScrolled] = useState(false);

  const handleBack = () => navigate(-1);

  /* DOWNLOAD CSV */
  const handleDownloadCSV = () => {
    if (!dashboardData?.logs?.length) {
      alert("No data available to download");
      return;
    }

    const csvData = dashboardData.logs.map((log) => ({
      Version: log.version || " ",
      Site: log.site || "N/A",
      "IP Address": log.ip || "N/A",
      Category: log.category || "N/A",
      Status: log.status || "N/A",
      Timestamp: log.date || "N/A",
      "Consent ID": log.consentId,
    }));

    exportToCSV(csvData, "cookie_logs");
  };

  /* ---------------- INTEGRITY PDF DOWNLOAD FUNCTION ---------------- */
  const downloadIntegrityPDF = async (consentId) => {
    try {
      const res = await fetch(
        `${config.check_integrity}`,
        {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
            "X-Tenant-ID": tenantId,
            "X-Business-id": businessId,
            "consentId": consentId,
            "X-Consent-Type": "consent_cookies",
            "x-session-token": token
          }
        }
      );

      const json = await res.json();
      const base64 = json?.pdfBase64;

      if (!base64) {
        alert("PDF not found for this consent record");
        return;
      }

      const byteCharacters = atob(base64);
      const byteNumbers = new Array(byteCharacters.length)
        .fill(0)
        .map((_, i) => byteCharacters.charCodeAt(i));
      const byteArray = new Uint8Array(byteNumbers);

      const blob = new Blob([byteArray], { type: "application/pdf" });

      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `Form65B-${consentId}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      alert("Failed to download Form 65B PDF");
    }
  };
  async function getConsentExpiryStatus(consentId) {
    try {
      const res = await fetch(`${BASE_URL}/consent/${consentId}/history`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          "X-Tenant-ID": tenantId,
          "business-id": businessId,
          "x-session-token": token
        }
      });
  
      if (!res.ok) return null;
  
      const json = await res.json();
      const history = json?.data || [];
  
      if (!Array.isArray(history) || history.length === 0) return null;
  
      // extract all preference validity dates
      const dates = history
        .map((h) => h.preferenceValidityDate)
        .filter(Boolean)
        .map((d) => new Date(d));
  
      if (dates.length === 0) return null;
  
      // find earliest expiry date
      const minDate = new Date(Math.min(...dates));
  
      // check if expired already
      if (minDate < new Date()) {
        return "Expired";
      }
  
      return null; // not expired
    } catch (e) {
      return null;
    }
  }
  
  /* ---------------- FETCH DASHBOARD DATA ---------------- */
  useEffect(() => {
    async function fetchDashboard() {
      setLoading(true);

      try {
        let url = `${BASE_URL}/dashboard/${tenantId}`;

        if (businessId) {
          url = `${BASE_URL}/dashboard/${tenantId}?bussinessId=${businessId}`;
        }

        if (templateId && scanId) {
          url += `&templateId=${templateId}&scanId=${scanId}`;
        } else if (templateId) {
          url += `&templateId=${templateId}`;
        }

        const res = await fetch(url, {
          headers: {
            "Content-Type": "application/json",
            "x-session-token": token
          }
        });

        if (!res.ok) throw new Error("Failed to load dashboard");

        const json = await res.json();
        const data = json?.data || json;
        const records = Array.isArray(data) ? data : [data];

        let allConsents = [];
        let viewDomain = "All Sites";

        records.forEach((r) =>
          (r.consents || [])
            .filter((c) => {
              // Include if has consentID and templatePreferences
              const hasConsentID = c.consentID && (c.templatePreferences?.length || 0) > 0;
              // Also include if consentHandleStatus is REJECTED (even without consentID)
              const isRejectedHandle = (c.consentHandleStatus || "").toUpperCase() === "REJECTED";
              return hasConsentID || isRejectedHandle;
            })
            .forEach((c) =>
              allConsents.push({
                ...c,
                site: r.scannedSites,
                scanId: r.scanId
              })
            )
        );
        

        let allAccepted = 0,
          partiallyAccepted = 0,
          allRejected = 0,
          revokedCount = 0,
          expiredCount = 0;

          const logs = await Promise.all(
            allConsents.map(async (c, i) => {
              const total = c.templatePreferences?.length || 0;
              const selected = c.userSelectedPreference?.length || 0;
          
              const backendStatus = (c.consentStatus || c.status || "").toLowerCase();
              const handleStatus = (c.consentHandleStatus || c.consentHandlerStatus || "").toLowerCase();
              const isRejected = backendStatus === "rejected" || backendStatus === "deny" || handleStatus === "rejected";
              const isNoAction =
                backendStatus === "created" ||
                backendStatus === "pending" ||
                backendStatus === "" ||
                backendStatus === null;
              const isRevoked = backendStatus === "revoked";
              const isExpiredBackend =
                backendStatus === "expired" || backendStatus === "timeout";
          
              let finalStatus = "Expired";
          
              if (selected === total && total > 0) finalStatus = "Accepted";
              else if (selected > 0) finalStatus = "Partially accepted";
              else if (isRejected) finalStatus = "Rejected";
              else if (isRevoked) finalStatus = "Revoked";
              else if (isExpiredBackend) finalStatus = "Expired";
              else if (isNoAction) finalStatus = "No Action";
          
              // Override only if expiry history says expired
              if (c.consentID) {
                const override = await getConsentExpiryStatus(c.consentID);
                if (override === "Expired") {
                  finalStatus = "Expired";
                }
              }
          
              // increment counters AFTER finalStatus is set
              if (finalStatus === "Accepted") allAccepted++;
              else if (finalStatus === "Partially accepted") partiallyAccepted++;
              else if (finalStatus === "Rejected") allRejected++;
              else if (finalStatus === "Revoked") revokedCount++;
              else if (finalStatus === "Expired") expiredCount++;
          
              // Format date - show "—" if not available
              const formattedDate = c.lastUpdated ? formatToIST(c.lastUpdated) : null;
              const displayDate = formattedDate && formattedDate !== 'N/A' ? formattedDate : "—";

              return {
                consentId: c.consentID || c.consentHandle || null,
                ip: c.customerIdentifier?.value || "—",
                scan: c.scanId || `SC${i + 1000}`,
                date: displayDate,
                category: c.userSelectedPreference?.length > 0 
                  ? c.userSelectedPreference.join(", ") 
                  : (c.templatePreferences?.join(", ") || "—"),
                site: c.site || "—",
                status: finalStatus,
                version: c.consentVersion || c.templateVersion || " ",
                consentHandleStatus: c.consentHandleStatus || c.consentHandlerStatus || "—"
              };
            })
          );
          

        setDashboardData({
          domain: viewDomain,
          totalViews: logs.length,
          totalCookies: logs.length,
          allAccepted,
          partiallyAccepted,
          allRejected,
          revokedCount,
          expiredCount,
          logs
        });
      } catch (err) {
        setError("No logs found");
      } finally {
        setLoading(false);
      }
    }

    fetchDashboard();
  }, [tenantId, businessId, templateId, scanId, token]);

  /* ---------------- SEARCH + FILTER + SORT ---------------- */
  const filteredLogs =
    dashboardData?.logs?.filter((r) => {
      if (search) {
        const match = [r.ip, r.category, r.status, r.site]
          .join(" ")
          .toLowerCase()
          .includes(search.toLowerCase());
        if (!match) return false;
      }

      if (filters.status.length > 0 && !filters.status.includes(r.status)) return false;

      const logDate = new Date(r.date);
      if (filters.dateFrom && logDate < new Date(filters.dateFrom)) return false;
      if (filters.dateTo && logDate > new Date(filters.dateTo)) return false;

      return true;
    }) || [];

  const sortedLogs = React.useMemo(() => {
    const sorted = [...filteredLogs];
    const { key, direction } = sortConfig;
    const dir = direction === "asc" ? 1 : -1;

    sorted.sort((a, b) => {
      if (key === "date") return dir * (new Date(a.date) - new Date(b.date));
      return dir * (a[key] || "").toString().localeCompare((b[key] || "").toString());
    });

    return sorted;
  }, [filteredLogs, sortConfig]);

  const handleSort = (key) => {
    setSortConfig((prev) =>
      prev.key === key
        ? { key, direction: prev.direction === "asc" ? "desc" : "asc" }
        : { key, direction: "asc" }
    );
  };

  const handleStatusFilterChange = (status) => {
    setFilters((prev) => ({
      ...prev,
      status: prev.status.includes(status)
        ? prev.status.filter((s) => s !== status)
        : [...prev.status, status]
    }));
  };

  const handleClearFilters = () =>
    setFilters({ status: [], dateFrom: "", dateTo: "" });

  const handleApplyFilters = () => setFilterDrawerOpen(false);

  /* KPI DATA */
  const kpiList = [
    { label: "Total views", value: dashboardData?.totalViews || 0, icon: <IcVisible /> },
    { label: "Total consent", value: dashboardData?.totalCookies || 0, icon: <IcCookies /> },
    { label: "All accepted", value: dashboardData?.allAccepted || 0, icon: <IcSuccessColored /> },
    { label: "Partially accepted", value: dashboardData?.partiallyAccepted || 0, icon: <IcStatusSuccessful /> },
    { label: "All rejected", value: dashboardData?.allRejected || 0, icon: <IcError /> },
    { label: "Expired", value: dashboardData?.expiredCount || 0, icon: <IcWarningColored /> }
  ];

  return (
    <div className="cookie-logs-page">
      {/* HEADER */}
      <div className="cookie-logs-header">
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <Button
            icon="ic_back"
            kind="secondary"
            size="small"
            state="normal"
            onClick={handleBack}
          />
          <h1 className="cookie-logs-title">Cookies Log</h1>
          <div className="cookie-logs-domain">{dashboardData?.domain}</div>
        </div>
      </div>

      {/* KPI ROW */}
      <div className="cookie-logs-kpi-row">
        {kpiList.map((k) => (
          <KPIBox key={k.label} {...k} />
        ))}
      </div>

      {/* ACTION BAR */}
      <div className="cookie-logs-actions">
        <div className="cookie-logs-search small">
          <Icon ic={<IcSearch />} size="sm" />
          <input
            type="text"
            placeholder="Search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <div className="cookie-logs-action-icons">
          <button
            className={`cookie-logs-filter-btn ${filterDrawerOpen ? "active" : ""} ${
              activeFilterCount > 0 ? "has-filters" : ""
            }`}
            onClick={() => setFilterDrawerOpen(!filterDrawerOpen)}
          >
            <Icon ic={<IcFilter />} size="md" />
            {activeFilterCount > 0 && (
              <span className="cookie-logs-filter-badge">{activeFilterCount}</span>
            )}
          </button>

          <button className="cookie-logs-action-btn" onClick={handleDownloadCSV}>
            <Icon ic={<IcDownload />} size="md" />
          </button>
        </div>
      </div>

      {/* FILTER DRAWER */}
      {filterDrawerOpen && (
        <>
          <div
            className="cookie-logs-filter-overlay"
            onClick={() => setFilterDrawerOpen(false)}
          />

          <div className="cookie-logs-filter-drawer">
            <div className="cookie-logs-filter-header">
              <div className="cookie-logs-filter-title">
                <Icon ic={<IcFilter />} size="sm" />
                <Text appearance="body-m-bold">Filters</Text>
              </div>
              <button
                className="cookie-logs-filter-close"
                onClick={() => setFilterDrawerOpen(false)}
              >
                <Icon ic={<IcClose />} size="sm" />
              </button>
            </div>

            <div className="cookie-logs-filter-content">
              {/* Status Filter */}
              <div className="cookie-logs-filter-section">
                <Text appearance="body-s-bold">Status</Text>

                <div className="cookie-logs-filter-chips">
                  {[
                    "Accepted",
                    "Partially accepted",
                    "Rejected",
                    "Revoked",
                    "Expired",
                    "No Action"
                  ].map((status) => (
                    <button
                      key={status}
                      className={`cookie-logs-filter-chip ${
                        filters.status.includes(status) ? "active" : ""
                      }`}
                      onClick={() => handleStatusFilterChange(status)}
                    >
                      {status}
                      {filters.status.includes(status) && (
                        <Icon ic={<IcClose />} size="xs" />
                      )}
                    </button>
                  ))}
                </div>
              </div>

              {/* DATE RANGE */}
              <div className="cookie-logs-filter-section">
                <Text appearance="body-s-bold">Date Range</Text>

                <div className="cookie-logs-filter-date-range">
                  <div className="cookie-logs-filter-date-input">
                    <label>From</label>
                    <input
                      type="date"
                      value={filters.dateFrom}
                      onChange={(e) =>
                        setFilters({ ...filters, dateFrom: e.target.value })
                      }
                    />
                  </div>

                  <div className="cookie-logs-filter-date-input">
                    <label>To</label>
                    <input
                      type="date"
                      value={filters.dateTo}
                      onChange={(e) =>
                        setFilters({ ...filters, dateTo: e.target.value })
                      }
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* FOOTER */}
            <div className="cookie-logs-filter-footer">
              <Button
                size="small"
                kind="secondary"
                label="Clear all"
                onClick={handleClearFilters}
              />
              <Button
                size="small"
                kind="primary"
                label="Apply"
                onClick={handleApplyFilters}
              />
            </div>
          </div>
        </>
      )}

      {/* TABLE */}
      <div
        className={`cookie-logs-table-container ${tableScrolled ? "scrolled" : ""}`}
        onScroll={(e) => setTableScrolled(e.target.scrollTop > 0)}
      >
        <table className="cookie-logs-table">
          <thead>
            <tr>
              {[
                { key: "ip", label: "IP address" },
                { key: "scan", label: "Scan ID" },
                { key: "date", label: "Date & Time" },
                { key: "version", label: "Version" },
                { key: "category", label: "Cookie category" },
                { key: "site", label: "Scanned site" },
                { key: "status", label: "Status" },
                { key: "form65b", label: "Form 65B" }
              ].map((col) => (
                <th
                  key={col.key}
                  onClick={() => handleSort(col.key)}
                  className={`sortable-header ${
                    sortConfig.key === col.key ? "active-sort" : ""
                  }`}
                >
                  <div className="cookie-logs-th-inner">
                    <span>{col.label}</span>
                    <Icon
                      ic={<IcSort />}
                      size="sm"
                      style={{
                        transform:
                          sortConfig.key === col.key &&
                          sortConfig.direction === "desc"
                            ? "rotate(180deg)"
                            : "none"
                      }}
                    />
                  </div>
                </th>
              ))}
            </tr>
          </thead>

          <tbody>
            {loading ? (
              <tr>
                <td colSpan="8" style={{ textAlign: "center", padding: 40 }}>
                  <div className="cookie-loader-container" style={{ height: "auto", padding: 20 }}>
                    <div className="cookie-spinner" />
                    <div className="cookie-loader-text">Loading logs...</div>
                  </div>
                </td>
              </tr>
            ) : error ? (
              <tr>
                <td colSpan="8" style={{ textAlign: "center", padding: 40 }}>
                  <div style={{ color: "#991b1b", display: "flex", flexDirection: "column", alignItems: "center", gap: 8 }}>
                    <Icon ic={<IcError />} size="xl" />
                    <Text appearance="body-m-bold" color="primary-grey-80">Failed to load logs</Text>
                    <Text appearance="body-s" color="primary-grey-60">Please try again later</Text>
                  </div>
                </td>
              </tr>
            ) : sortedLogs.length ? (
              sortedLogs.map((r, i) => (
                <tr key={i}>
                  <td><Text appearance="body-xs">{r.ip}</Text></td>
                  <td><Text appearance="body-xs">{r.scan}</Text></td>
                  <td><Text appearance="body-xs">{r.date}</Text></td>
                  <td><Text appearance="body-xs">{r.version}</Text></td>
                  <td><Text appearance="body-xs">{r.category}</Text></td>
                  <td><Text appearance="body-xs">{r.site}</Text></td>
                  <td>
                    <StatusBadge status={r.status} />
                  </td>

                  {/* FORM 65B DOWNLOAD BUTTON */}
                  <td>
                    {r.consentId ? (
                      <button
                        className="cookie-65b-download-btn"
                        onClick={() => downloadIntegrityPDF(r.consentId)}
                      >
                        <Icon ic={<IcDownload />} size="sm" />
                      </button>
                    ) : (
                      <Text appearance="body-xs">—</Text>
                    )}
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="8" style={{ textAlign: "center", padding: 40 }}>
                  <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 8 }}>
                    <Icon ic={<IcCookies />} size="xl" color="primary-grey-40" />
                    <Text appearance="body-m-bold" color="primary-grey-80">No logs found</Text>
                    <Text appearance="body-s" color="primary-grey-60">There are no consent logs to display</Text>
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
