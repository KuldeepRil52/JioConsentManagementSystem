import React, { useState, useEffect } from "react";
import { Text, ActionButton, Icon } from "../custom-components";
import { useSelector } from "react-redux";
import { IcEditPen, IcTrash, IcSort } from "../custom-components/Icon";
import React, { useState, useEffect, useMemo } from "react";
import { Text, ActionButton, Button, Icon } from "../custom-components";
import { useNavigate } from "react-router-dom";
import { IcSort, IcDownload, IcVisible, IcFilter, IcClose } from "../custom-components/Icon";
import { generateTransactionId } from "../utils/transactionId";
import { exportToCSV } from "../utils/csvExport";
import config from "../utils/config";
import "../styles/pageConfiguration.css";
import "../styles/auditCompliance.css";

const AuditCompliance = () => {
  const navigate = useNavigate();
  const tenantId = useSelector((state) => state.common.tenant_id);
  const sessionToken = useSelector((state) => state.common.session_token);
  const businessId = useSelector((state) => state.common.business_id);

  // ── All data from API (fetched once) ──
  const [allAuditReports, setAllAuditReports] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");

  // ── Filter state ──
  const [filterDrawerOpen, setFilterDrawerOpen] = useState(false);
  const [filters, setFilters] = useState({
    status: "", // "", "SUCCESS", "FAILED", "PENDING"
    dateFrom: "",
    dateTo: "",
  });

  // ── Client-side pagination state ──
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState("asc");

  // Format text to uppercase for consistency
  const formatToUpperCase = (text) => {
    if (!text || text === "N/A") return text;
    return text.toUpperCase();
  };

  // Format initiator display
  const formatInitiator = (initiator) => {
    if (!initiator || initiator === "N/A") return initiator;
    if (initiator === "DF") return "DATA_FIDUCIARY";
    return initiator.toUpperCase();
  };

  // ── Fetch ALL audit reports (paginate through every API page) ──
  const fetchAuditReports = async () => {
    try {
      setLoading(true);
      setError(null);

      const PAGE_SIZE = 100; // Typical API max; we'll loop until we get all pages
      let allReports = [];
      let pageNo = 0;
      let hasMore = true;

      while (hasMore) {
        const txnId = generateTransactionId();
        const response = await fetch(
          `${config.audit_reports}?page=${pageNo}&size=${PAGE_SIZE}&sort=DESC`,
          {
            method: "GET",
            headers: {
              accept: "*/*",
              "X-Tenant-ID": tenantId,
              "tenant-id": tenantId,
              "X-Business-ID": businessId,
              "X-Transaction-ID": txnId,
              "Content-Type": "application/json",
              "business-id": businessId,
              "x-session-token": sessionToken,
            },
          }
        );

        if (!response.ok) {
          if (pageNo === 0) {
            const errorText = await response.text();
            console.error("Failed to fetch audit reports:", errorText);
            setError(`Failed to fetch audit reports: ${response.status}`);
            return;
          }
          break; // Got some pages at least, stop here
        }

        const data = await response.json();
        const pageReports = data?.data || data?.content || [];
        if (!Array.isArray(pageReports) || pageReports.length === 0) {
          hasMore = false;
        } else {
          allReports = allReports.concat(pageReports);
          // Stop if we got fewer than requested (last page) or reached API total
          const totalElements = data?.totalElements || data?.total || data?.totalRecords;
          if (pageReports.length < PAGE_SIZE) {
            hasMore = false;
          } else if (totalElements && allReports.length >= totalElements) {
            hasMore = false;
          } else {
            pageNo++;
          }
        }
      }

      if (allReports.length > 0) {
        const reports = allReports;

        const mappedReports = Array.isArray(reports)
          ? reports.map((item) => {
              // ── helper: parse any date-like value into a Date object ──
              const parseAnyDate = (val) => {
                if (!val) return null;
                // epoch (seconds or ms)
                if (typeof val === "number") {
                  const ms = val < 1e12 ? val * 1000 : val;
                  const d = new Date(ms);
                  return isNaN(d.getTime()) ? null : d;
                }
                if (typeof val !== "string") return null;
                // ISO string – append Z if missing timezone info
                let str = val;
                if (!/[Zz]$/.test(str) && !/[+-]\d{2}:\d{2}$/.test(str)) {
                  str += "Z";
                }
                const d = new Date(str);
                return isNaN(d.getTime()) ? null : d;
              };

              // ── helper: format Date → "DD-MM-YYYY at HH:mm:ss AM/PM" in IST ──
              const formatDate = (dateString) => {
                const date = parseAnyDate(dateString);
                if (!date) return "N/A";
                try {
                  const options = {
                    timeZone: "Asia/Kolkata",
                    year: "numeric",
                    month: "2-digit",
                    day: "2-digit",
                    hour: "2-digit",
                    minute: "2-digit",
                    second: "2-digit",
                    hour12: true,
                  };
                  const formatter = new Intl.DateTimeFormat("en-IN", options);
                  const parts = formatter.formatToParts(date);
                  const p = parts.reduce((acc, part) => {
                    acc[part.type] = part.value;
                    return acc;
                  }, {});
                  return `${p.day}-${p.month}-${p.year} at ${p.hour}:${p.minute}:${p.second} ${(p.dayPeriod || "").toUpperCase()}`;
                } catch {
                  return String(dateString);
                }
              };

              // ── helper: Date → "YYYY-MM-DD" in IST (for filtering) ──
              const toISTDateStr = (date) => {
                if (!date) return null;
                // Use formatToParts for guaranteed 2-digit month/day
                const parts = new Intl.DateTimeFormat("en-IN", {
                  timeZone: "Asia/Kolkata",
                  year: "numeric",
                  month: "2-digit",
                  day: "2-digit",
                }).formatToParts(date);
                const p = parts.reduce((acc, part) => {
                  acc[part.type] = part.value;
                  return acc;
                }, {});
                return `${p.year}-${p.month}-${p.day}`;
              };

              // Pick the best available timestamp (try all common field names)
              const eventRaw =
                item.createdAt ||
                item.timestamp ||
                item.created_at ||
                item.eventTime ||
                item.eventTimestamp ||
                item.updatedAt ||
                item.updated_at;
              const eventDateObj = parseAnyDate(eventRaw);

              return {
                auditId: item.auditId || item.id || "N/A",
                status: item.status || "N/A",
                updatedOn: formatDate(item.updatedAt),
                createdOn: formatDate(item.timestamp),
                businessId: item.businessId || "N/A",
                businessName:
                  item.businessName || item.business?.name || "N/A",
                moduleName: formatToUpperCase(item.group) || "N/A",
                component: formatToUpperCase(item.component) || "N/A",
                actionType: formatToUpperCase(item.actionType) || "N/A",
                initiatedBy: item.initiator || "N/A",
                actorId: item.actor?.id || "N/A",
                actorRole: item.actor?.role || "N/A",
                actorType: formatToUpperCase(item.actor?.type) || "N/A",
                resourceId: item.resource?.id || "N/A",
                resourceType: item.resource?.type || "N/A",
                transactionId: item.context?.txnId || "N/A",
                referenceId: item.id || "N/A",
                ipAddress: item.context?.ipAddress || "N/A",
                eventTimestamp: formatDate(eventRaw),
                payloadHash: item.payloadHash || "N/A",
                // Pre-computed IST date for fast & reliable filtering
                _filterDateIST: toISTDateStr(eventDateObj),
                rawData: item,
              };
            })
          : [];

        setAllAuditReports(mappedReports);
      } else {
        // No records fetched at all
        setAllAuditReports([]);
      }
    } catch (err) {
      console.error("Error fetching audit reports:", err);
      setError("Error fetching audit reports. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  // Fetch data once on mount
  useEffect(() => {
    if (tenantId && sessionToken && businessId) {
      fetchAuditReports();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tenantId, sessionToken, businessId]);

  // ── Filtering (status + date + search) applied to full dataset ──
  const filteredAuditReports = useMemo(() => {
    return allAuditReports.filter((audit) => {
      // 1) Status filter
      if (filters.status) {
        const statusUpper = (audit.status || "").toUpperCase();
        if (statusUpper !== filters.status.toUpperCase()) return false;
      }

      // 2) Date range filter (uses pre-computed IST date YYYY-MM-DD)
      const hasDateFilter = filters.dateFrom || filters.dateTo;
      if (hasDateFilter) {
        const eventDateIST = audit._filterDateIST; // e.g. "2026-03-12"
        if (!eventDateIST) return false;

        if (filters.dateFrom && eventDateIST < filters.dateFrom) return false;
        if (filters.dateTo && eventDateIST > filters.dateTo) return false;
      }

      // 3) Search query
      if (searchQuery) {
        const q = searchQuery.toLowerCase();
        const matchesSearch =
          audit.auditId?.toLowerCase().includes(q) ||
          audit.status?.toLowerCase().includes(q) ||
          audit.moduleName?.toLowerCase().includes(q) ||
          audit.component?.toLowerCase().includes(q) ||
          audit.actionType?.toLowerCase().includes(q) ||
          audit.businessName?.toLowerCase().includes(q) ||
          audit.initiatedBy?.toLowerCase().includes(q) ||
          audit.actorRole?.toLowerCase().includes(q);
        if (!matchesSearch) return false;
      }

      return true;
    });
  }, [allAuditReports, filters.status, filters.dateFrom, filters.dateTo, searchQuery]);

  // ── Sorting ──
  const sortedFilteredReports = useMemo(() => {
    if (!sortColumn) return filteredAuditReports;
    return [...filteredAuditReports].sort((a, b) => {
      const aValue = a[sortColumn] || "";
      const bValue = b[sortColumn] || "";
      if (aValue < bValue) return sortDirection === "asc" ? -1 : 1;
      if (aValue > bValue) return sortDirection === "asc" ? 1 : -1;
      return 0;
    });
  }, [filteredAuditReports, sortColumn, sortDirection]);

  // ── Client-side pagination derived values ──
  const totalRecords = sortedFilteredReports.length;
  const totalPages = Math.max(1, Math.ceil(totalRecords / pageSize));

  // Rows for the current page only
  const paginatedReports = useMemo(() => {
    const start = currentPage * pageSize;
    return sortedFilteredReports.slice(start, start + pageSize);
  }, [sortedFilteredReports, currentPage, pageSize]);

  // Reset to page 0 whenever filters, search, or pageSize change
  useEffect(() => {
    setCurrentPage(0);
  }, [filters.status, filters.dateFrom, filters.dateTo, searchQuery, pageSize]);

  // ── Sort handler ──
  const handleSort = (column) => {
    if (sortColumn === column) {
      setSortDirection(sortDirection === "asc" ? "desc" : "asc");
    } else {
      setSortColumn(column);
      setSortDirection("asc");
    }
  };

  const renderSortIcon = () => (
    <Icon ic={<IcSort />} size="small" color="black" />
  );

  // ── Pagination handlers ──
  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < totalPages) {
      setCurrentPage(newPage);
    }
  };

  const handlePageSizeChange = (newSize) => {
    setPageSize(newSize);
    // currentPage reset handled by the useEffect above
  };

  // ── Filter handlers ──
  const handleClearFilters = () => {
    setFilters({ status: "", dateFrom: "", dateTo: "" });
  };

  const handleApplyFilters = () => {
    setFilterDrawerOpen(false);
  };

  const toggleStatusFilter = (status) => {
    setFilters((prev) => ({
      ...prev,
      status: prev.status === status ? "" : status,
    }));
  };

  // Active filter count (for badge)
  const activeFilterCount =
    (filters.status ? 1 : 0) +
    (filters.dateFrom ? 1 : 0) +
    (filters.dateTo ? 1 : 0);

  // ── CSV download ──
  const handleDownloadCSV = () => {
    if (!sortedFilteredReports || sortedFilteredReports.length === 0) {
      alert("No data available to download");
      return;
    }

    const csvData = sortedFilteredReports.map((audit) => ({
      "Audit ID": audit.auditId || "N/A",
      "Business Name": audit.businessName || "N/A",
      "Event Timestamp": audit.eventTimestamp || "N/A",
      "Business ID": audit.businessId || "N/A",
      "Module Name": audit.moduleName || "N/A",
      Component: audit.component || "N/A",
      "Action Type": audit.actionType || "N/A",
      "Initiated By": formatInitiator(audit.initiatedBy) || "N/A",
      "Actor ID": audit.actorId || "N/A",
      "Actor Role": audit.actorRole || "N/A",
      "Actor Type": audit.actorType || "N/A",
      "Resource ID": audit.resourceId || "N/A",
      "Resource Type": audit.resourceType || "N/A",
      "Transaction ID": audit.transactionId || "N/A",
      "Reference ID": audit.referenceId || "N/A",
    }));

    exportToCSV(csvData, "audit_compliance");
  };

  const handleNewAudit = () => {
    navigate("/createAudit");
  };

  // ── Status badge helper ──
  const getStatusBadge = (status) => {
    const s = (status || "").toUpperCase();
    let cls = "status-badge ";
    if (s === "SUCCESS" || s === "COMPLETED") cls += "status-completed";
    else if (s === "FAILED" || s === "ERROR") cls += "status-failed";
    else if (s === "PENDING" || s === "IN_PROGRESS") cls += "status-progress";
    return <span className={cls}>{status}</span>;
  };

  // ── Render ──
  return (
    <>
      <div className="configurePage">
        {/* Header Section */}
        <div className="audit-header-section">
          <div className="audit-title-section">
            <div style={{ display: "flex", flexDirection: "row", gap: "10px" }}>
              <Text appearance="heading-s" color="primary-grey-100">
                Audit & Compliance Report
              </Text>
              <div
                className="dataProtectionOfficer-badge"
                style={{ marginTop: "5px" }}
              >
                <Text appearance="body-xs-bold" color="primary-grey-80">
                  Governance
                </Text>
              </div>
            </div>
          </div>
          <div className="audit-button-group">
            {/* Uncomment when create functionality is ready */}
            {/* <ActionButton kind="primary" size="medium" label="Create New Audit" onClick={handleNewAudit} /> */}
          </div>
        </div>

        {/* Table Controls */}
        <div className="audit-table-controls">
          <div className="audit-search-container">
            <input
              type="text"
              placeholder="Search"
              className="audit-search-input"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            <svg
              className="audit-search-icon"
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
            >
              <circle cx="11" cy="11" r="8" stroke="currentColor" strokeWidth="2" />
              <path d="M21 21L16.65 16.65" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </div>
          <div className="audit-table-actions">
            <button
              className={`audit-icon-button ${filterDrawerOpen ? "active" : ""} ${activeFilterCount > 0 ? "has-filters" : ""}`}
              onClick={() => setFilterDrawerOpen(!filterDrawerOpen)}
              title="Filter"
            >
              <IcFilter height={20} width={20} />
              {activeFilterCount > 0 && (
                <span className="filter-badge">{activeFilterCount}</span>
              )}
            </button>
            <button
              className="audit-icon-button"
              onClick={handleDownloadCSV}
              title="Download CSV"
            >
              <IcDownload height={20} width={20} />
            </button>
          </div>
        </div>

        {/* ── Filter Drawer ── */}
        {filterDrawerOpen && (
          <>
            <div
              className="audit-filter-drawer-overlay"
              onClick={() => setFilterDrawerOpen(false)}
            />
            <div className="audit-filter-drawer">
              <div className="filter-drawer-header">
                <div className="filter-drawer-title">
                  <IcFilter height={24} width={24} />
                  <Text appearance="heading-xs" color="primary-grey-100">
                    Filters
                  </Text>
                </div>
                <button
                  className="filter-drawer-close"
                  onClick={() => setFilterDrawerOpen(false)}
                >
                  <IcClose height={24} width={24} />
                </button>
              </div>

              <div className="filter-drawer-content">
                {/* Search within filters */}
                <div className="filter-section">
                  <Text appearance="body-s-bold" color="primary-grey-100">
                    Search
                  </Text>
                  <input
                    type="text"
                    placeholder="Search audit ID, module, component..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    style={{
                      width: "100%",
                      padding: "10px 12px",
                      border: "1px solid #e0e0e0",
                      borderRadius: "6px",
                      fontSize: "14px",
                    }}
                  />
                </div>

                {/* Status filter chips */}
                <div className="filter-section">
                  <Text appearance="body-s-bold" color="primary-grey-100">
                    Status
                  </Text>
                  <div className="filter-chips">
                    {["SUCCESS", "FAILED", "PENDING"].map((status) => (
                      <button
                        key={status}
                        className={`filter-chip ${filters.status === status ? "active" : ""}`}
                        onClick={() => toggleStatusFilter(status)}
                      >
                        {status.charAt(0) + status.slice(1).toLowerCase()}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Event date range */}
                <div className="filter-section">
                  <Text appearance="body-s-bold" color="primary-grey-100">
                    Event date range
                  </Text>
                  <div className="filter-date-range">
                    <div className="filter-date-input">
                      <label>From</label>
                      <input
                        type="date"
                        value={filters.dateFrom}
                        max={filters.dateTo || new Date().toISOString().split("T")[0]}
                        onChange={(e) =>
                          setFilters({ ...filters, dateFrom: e.target.value })
                        }
                      />
                    </div>
                    <div className="filter-date-input">
                      <label>To</label>
                      <input
                        type="date"
                        value={filters.dateTo}
                        min={filters.dateFrom || ""}
                        max={new Date().toISOString().split("T")[0]}
                        onChange={(e) =>
                          setFilters({ ...filters, dateTo: e.target.value })
                        }
                      />
                    </div>
                  </div>
                </div>
              </div>

              <div className="filter-drawer-footer">
                <ActionButton
                  kind="secondary"
                  size="medium"
                  label="Clear all"
                  onClick={handleClearFilters}
                />
                <ActionButton
                  kind="primary"
                  size="medium"
                  label="Apply"
                  onClick={handleApplyFilters}
                />
              </div>
            </div>
          </>
        )}

        {/* ── Table Section ── */}
        <div className="audit-table-wrapper">
          <table className="audit-table">
            <thead>
              <tr>
                <th onClick={() => handleSort("auditId")} style={{ cursor: "pointer" }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">Audit ID</Text>
                    {renderSortIcon()}
                  </div>
                </th>
                <th onClick={() => handleSort("businessName")} style={{ cursor: "pointer" }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">Business Name</Text>
                    {renderSortIcon()}
                  </div>
                </th>
                <th onClick={() => handleSort("eventTimestamp")} style={{ cursor: "pointer" }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">Event timestamp</Text>
                    {renderSortIcon()}
                  </div>
                </th>
                <th onClick={() => handleSort("businessId")} style={{ cursor: "pointer" }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">Business ID</Text>
                    {renderSortIcon()}
                  </div>
                </th>
                <th onClick={() => handleSort("moduleName")} style={{ cursor: "pointer" }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">Module name</Text>
                    {renderSortIcon()}
                  </div>
                </th>
                <th onClick={() => handleSort("component")} style={{ cursor: "pointer" }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">Component</Text>
                    {renderSortIcon()}
                  </div>
                </th>
                <th onClick={() => handleSort("actionType")} style={{ cursor: "pointer" }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">Action type</Text>
                    {renderSortIcon()}
                  </div>
                </th>
                <th onClick={() => handleSort("initiatedBy")} style={{ cursor: "pointer" }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">Initiated by</Text>
                    {renderSortIcon()}
                  </div>
                </th>
                <th onClick={() => handleSort("actorId")} style={{ cursor: "pointer" }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">Actor ID</Text>
                    {renderSortIcon()}
                  </div>
                </th>
                <th onClick={() => handleSort("actorRole")} style={{ cursor: "pointer" }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">Actor role</Text>
                    {renderSortIcon()}
                  </div>
                </th>
                <th onClick={() => handleSort("actorType")} style={{ cursor: "pointer" }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">Actor type</Text>
                    {renderSortIcon()}
                  </div>
                </th>
                <th onClick={() => handleSort("resourceId")} style={{ cursor: "pointer" }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">Resource ID</Text>
                    {renderSortIcon()}
                  </div>
                </th>
                <th onClick={() => handleSort("resourceType")} style={{ cursor: "pointer" }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">Resource type</Text>
                    {renderSortIcon()}
                  </div>
                </th>
                <th onClick={() => handleSort("transactionId")} style={{ cursor: "pointer" }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">Transaction ID</Text>
                    {renderSortIcon()}
                  </div>
                </th>
                <th onClick={() => handleSort("referenceId")} style={{ cursor: "pointer" }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">Reference ID</Text>
                    {renderSortIcon()}
                  </div>
                </th>
                <th onClick={() => handleSort("ipAddress")} style={{ cursor: "pointer" }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">IP Address</Text>
                    {renderSortIcon()}
                  </div>
                </th>
                <th onClick={() => handleSort("payloadHash")} style={{ cursor: "pointer" }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">Payload hash</Text>
                    {renderSortIcon()}
                  </div>
                </th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="17" style={{ textAlign: "center", padding: "40px" }}>
                    <Text appearance="body-m" color="primary-grey-80">Loading audit reports...</Text>
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan="17" style={{ textAlign: "center", padding: "40px" }}>
                    <Text appearance="body-m" color="feedback_error_50">{error}</Text>
                  </td>
                </tr>
              ) : paginatedReports.length === 0 ? (
                <tr>
                  <td colSpan="17" style={{ textAlign: "center", padding: "40px" }}>
                    <Text appearance="body-m" color="primary-grey-80">
                      {searchQuery || activeFilterCount > 0
                        ? "No results found for current filters"
                        : "No audit reports found."}
                    </Text>
                  </td>
                </tr>
              ) : (
                paginatedReports.map((audit) => (
                  <tr key={audit.auditId + audit.referenceId}>
                    <td>
                      <Text appearance="body-xs-bold" color="black">{audit.auditId}</Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="black">{audit.businessName}</Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="black">{audit.eventTimestamp}</Text>
                    </td>
                    <td>
                      <Text appearance="body-xs-bold" color="black">{audit.businessId}</Text>
                    </td>
                    <td>
                      <Text appearance="body-xs-bold" color="black">{audit.moduleName}</Text>
                    </td>
                    <td>
                      <Text appearance="body-xs-bold" color="black">{audit.component}</Text>
                    </td>
                    <td>
                      <Text appearance="body-xs-bold" color="black">{audit.actionType}</Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="black">{formatInitiator(audit.initiatedBy)}</Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="black">{audit.actorId}</Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="black">{audit.actorRole}</Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="black">{audit.actorType}</Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="black">{audit.resourceId}</Text>
                    </td>
                    <td>
                      <Text appearance="body-xs-bold" color="black">{audit.resourceType}</Text>
                    </td>
                    <td>
                      <Text appearance="body-xs-bold" color="black">{audit.transactionId}</Text>
                    </td>
                    <td>
                      <Text appearance="body-xs-bold" color="black">{audit.referenceId}</Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="black">{audit.ipAddress}</Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="black">{audit.payloadHash}</Text>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* ── Pagination Controls ── */}
        {!loading && !error && totalRecords > 0 && (
          <div className="audit-pagination-container">
            <div className="audit-pagination-info">
              <Text appearance="body-xs" color="primary-grey-80">
                Showing {currentPage * pageSize + 1} to{" "}
                {Math.min((currentPage + 1) * pageSize, totalRecords)} of{" "}
                {totalRecords} records
              </Text>
            </div>

            <div className="audit-pagination-controls">
              <div className="audit-page-size-selector">
                <Text appearance="body-xs" color="primary-grey-80">
                  Rows per page:
                </Text>
                <select
                  value={pageSize}
                  onChange={(e) => handlePageSizeChange(Number(e.target.value))}
                  className="audit-page-size-select"
                >
                  <option value={10}>10</option>
                  <option value={20}>20</option>
                  <option value={50}>50</option>
                  <option value={100}>100</option>
                </select>
              </div>

              <div className="audit-pagination-buttons">
                {/* First page */}
                <button
                  className="audit-pagination-button"
                  onClick={() => handlePageChange(0)}
                  disabled={currentPage === 0}
                  title="First page"
                >
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                    <path d="M18 6L8 12L18 18V6Z" fill="currentColor" />
                    <path d="M6 6L6 18" stroke="currentColor" strokeWidth="2" />
                  </svg>
                </button>

                {/* Previous page */}
                <button
                  className="audit-pagination-button"
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage === 0}
                  title="Previous page"
                >
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                    <path d="M15 18L9 12L15 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                </button>

                {/* Page numbers */}
                <div className="audit-page-numbers">
                  {Array.from(
                    { length: Math.min(5, totalPages) },
                    (_, i) => {
                      let pageNum;
                      if (totalPages <= 5) {
                        pageNum = i;
                      } else if (currentPage < 3) {
                        pageNum = i;
                      } else if (currentPage > totalPages - 3) {
                        pageNum = totalPages - 5 + i;
                      } else {
                        pageNum = currentPage - 2 + i;
                      }
                      return (
                        <button
                          key={pageNum}
                          className={`audit-page-number ${currentPage === pageNum ? "active" : ""}`}
                          onClick={() => handlePageChange(pageNum)}
                        >
                          {pageNum + 1}
                        </button>
                      );
                    }
                  )}
                </div>

                {/* Next page */}
                <button
                  className="audit-pagination-button"
                  onClick={() => handlePageChange(currentPage + 1)}
                  disabled={currentPage >= totalPages - 1}
                  title="Next page"
                >
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                    <path d="M9 6L15 12L9 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                </button>

                {/* Last page */}
                <button
                  className="audit-pagination-button"
                  onClick={() => handlePageChange(totalPages - 1)}
                  disabled={currentPage >= totalPages - 1}
                  title="Last page"
                >
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                    <path d="M6 18L16 12L6 6V18Z" fill="currentColor" />
                    <path d="M18 6L18 18" stroke="currentColor" strokeWidth="2" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </>
  );
};

export default AuditCompliance;
