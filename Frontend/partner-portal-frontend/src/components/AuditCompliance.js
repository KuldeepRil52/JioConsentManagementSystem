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

  const [auditReports, setAuditReports] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");

  const [filterDrawerOpen, setFilterDrawerOpen] = useState(false);
  const [filters, setFilters] = useState({
    dateFrom: "",
    dateTo: "",
  });

  // Pagination state
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalRecords, setTotalRecords] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [correctedTotal, setCorrectedTotal] = useState(null); // Track corrected total when we detect last page

  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState('asc');

  // Format text to uppercase for consistency
  const formatToUpperCase = (text) => {
    if (!text || text === "N/A") return text;
    return text.toUpperCase();
  };

  // Format initiator display
  const formatInitiator = (initiator) => {
    if (!initiator || initiator === "N/A") return initiator;
    if (initiator === 'DF') {
      return 'DATA_FIDUCIARY';
    }
    return initiator.toUpperCase();
  };

  // Fetch audit reports from API
  const fetchAuditReports = async () => {
    try {
      setLoading(true);
      setError(null);

      const txnId = generateTransactionId();

      const response = await fetch(
        `${config.audit_reports}?page=${currentPage}&size=${pageSize}&sort=DESC`,
        {
          method: 'GET',
          headers: {
            'accept': '*/*',
            'X-Tenant-ID': tenantId,
            'X-Business-ID': businessId,
            'X-Transaction-ID': txnId,
            'Content-Type': 'application/json',
            'business-id': businessId,
            'x-session-token': sessionToken,
          },
        }
      );

      if (response.ok) {
        const data = await response.json();

        // Map API response to table format first
        const reports = data?.data || data?.content || [];

        const mappedReports = Array.isArray(reports) ? reports.map((item, index) => {
          // Format date in IST (Indian Standard Time) - convert from UTC
          const formatDate = (dateString) => {
            if (!dateString) return "N/A";
            try {
              // IMPORTANT: Append 'Z' to treat the date string as UTC if it's not already present.
              // This ensures correct parsing before converting to the target timezone.
              const date = new Date(dateString.endsWith('Z') ? dateString : dateString + 'Z');

              // Options for formatting. All parts will be in the specified timezone.
              const options = {
                timeZone: 'Asia/Kolkata',
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
                hour12: true,
              };

              // Create a formatter for Indian English locale
              const formatter = new Intl.DateTimeFormat('en-IN', options);

              // Get the parts of the date
              const parts = formatter.formatToParts(date);
              const partMap = parts.reduce((acc, part) => {
                acc[part.type] = part.value;
                return acc;
              }, {});

              // Assemble the final string in "DD-MM-YYYY at HH:mm:ss AM/PM" format
              const formattedDate = `${partMap.day}-${partMap.month}-${partMap.year}`;
              const timeString = `${partMap.hour}:${partMap.minute}:${partMap.second} ${partMap.dayPeriod.toUpperCase()}`;

              return `${formattedDate} at ${timeString}`;

            } catch (error) {
              console.error("Error formatting date:", error);
              return dateString; // Fallback to original string on error
            }
          };

          return {
            // Basic audit info
            auditId: item.auditId || item.id || "N/A",
            status: item.status || "N/A",
            updatedOn: formatDate(item.updatedAt),
            createdOn: formatDate(item.timestamp), // Using Event timestamp for Created on
            businessId: item.businessId || "N/A",
            businessName: item.businessName || item.business?.name || "N/A",
            moduleName: formatToUpperCase(item.group) || "N/A",
            component: formatToUpperCase(item.component) || "N/A",
            actionType: formatToUpperCase(item.actionType) || "N/A",
            initiatedBy: item.initiator || "N/A",

            // Actor details
            actorId: item.actor?.id || "N/A",
            actorRole: item.actor?.role || "N/A",
            actorType: formatToUpperCase(item.actor?.type) || "N/A",

            // Resource details
            resourceId: item.resource?.id || "N/A",
            resourceType: item.resource?.type || "N/A",

            // Transaction details
            transactionId: item.context?.txnId || "N/A",
            referenceId: item.id || "N/A",
            ipAddress: item.context?.ipAddress || "N/A",
            eventTimestamp: formatDate(item.createdAt || item.timestamp),
            payloadHash: item.payloadHash || "N/A",

            rawData: item,
          };
        }) : [];

        setAuditReports(mappedReports);

        // Update pagination info - handle different API response structures
        // Get total from API response first
        let apiTotal = data?.totalElements || data?.total || data?.totalRecords;

        // If we're on the last page (fewer records than pageSize), 
        // calculate the actual total from the data we have
        // This corrects cases where API returns incorrect total
        let total;
        if (mappedReports.length < pageSize) {
          // We're on the last page - total is exactly: (current page * pageSize) + records on this page
          const actualTotal = (currentPage * pageSize) + mappedReports.length;
          // Use the actual total and remember it for future pages
          total = actualTotal;
          setCorrectedTotal(actualTotal);
        } else if (correctedTotal !== null) {
          // We've previously detected the last page, use the corrected total
          total = correctedTotal;
        } else if (apiTotal) {
          // API provided total - use it (even if it might be wrong, we'll correct when we reach last page)
          total = apiTotal;
        } else {
          // API didn't provide total - estimate based on current page
          // If we have full page, assume there might be more
          if (mappedReports.length === pageSize) {
            // We have a full page, so there might be more - estimate conservatively
            total = (currentPage + 2) * pageSize; // Assume at least one more page
          } else {
            // Less than full page, this is the last page
            total = (currentPage * pageSize) + mappedReports.length;
          }
        }

        // Calculate total pages
        const apiTotalPages = data?.totalPages;
        let pages;
        if (apiTotalPages) {
          pages = apiTotalPages;
        } else {
          pages = Math.ceil(total / pageSize);
        }

        // If we have exactly pageSize records and we're not on the last detected page,
        // ensure we can navigate to the next page to check for more records
        // This handles cases where API total might be incorrect
        if (mappedReports.length === pageSize && correctedTotal === null) {
          // We have a full page, so there might be more records
          // Ensure we can navigate to at least the next page
          const minPages = currentPage + 2; // At least current page + next page
          pages = Math.max(pages, minPages);
          // Update total to allow navigation (conservative estimate)
          // This ensures the "of X records" text allows for more records
          const minTotal = minPages * pageSize;
          if (!apiTotal || total < minTotal) {
            total = minTotal; // Conservative estimate to allow navigation
          }
        }

        // Ensure at least 1 page if we have data
        if (pages === 0 && mappedReports.length > 0) {
          pages = 1;
        }

        setTotalRecords(total);
        setTotalPages(pages);
      } else {
        const errorText = await response.text();
        console.error("Failed to fetch audit reports:", errorText);
        setError(`Failed to fetch audit reports: ${response.status}`);
      }
    } catch (err) {
      console.error("Error fetching audit reports:", err);
      setError("Error fetching audit reports. Please try again.");
    } finally {
      setLoading(false);
    }
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

  // Apply filters: date range based on Event timestamp column (rawData.createdAt || rawData.timestamp)
  const filteredAuditReports = useMemo(() => {
    return auditReports.filter((audit) => {
      const eventDateStr = audit.rawData?.createdAt || audit.rawData?.timestamp;
      const hasDateFilter = filters.dateFrom || filters.dateTo;

      if (!hasDateFilter) return true;

      // When date filter is set but row has no event timestamp, exclude it
      if (!eventDateStr) return false;

      const eventTime = new Date(eventDateStr.endsWith('Z') ? eventDateStr : eventDateStr + 'Z').getTime();
      if (filters.dateFrom) {
        const fromTime = new Date(filters.dateFrom + 'T00:00:00.000Z').getTime();
        if (eventTime < fromTime) return false;
      }
      if (filters.dateTo) {
        const toTime = new Date(filters.dateTo + 'T23:59:59.999Z').getTime();
        if (eventTime > toTime) return false;
      }
      return true;
    });
  }, [auditReports, filters.dateFrom, filters.dateTo]);

  // Sort the filtered reports (table and CSV use this)
  const sortedFilteredReports = useMemo(() => {
    if (!sortColumn) return filteredAuditReports;

    return [...filteredAuditReports].sort((a, b) => {
      const aValue = a[sortColumn] || '';
      const bValue = b[sortColumn] || '';

      if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }, [filteredAuditReports, sortColumn, sortDirection]);

  // Fetch data on component mount and when pagination changes
  useEffect(() => {
    if (tenantId && sessionToken && businessId) {
      fetchAuditReports();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tenantId, sessionToken, businessId, currentPage, pageSize]);

  // Reset corrected total when search query changes
  useEffect(() => {
    setCorrectedTotal(null);
  }, [searchQuery]);

  const handleView = (id) => {
    // TODO: Implement view functionality
  };

  const handleDownload = (id) => {
    // TODO: Implement download functionality
  };

  const handleDownloadCSV = () => {
    if (!sortedFilteredReports || sortedFilteredReports.length === 0) {
      alert("No data available to download");
      return;
    }

    // Filter audit reports based on search query
    const filteredData = sortedFilteredReports.filter((audit) => {
      if (!searchQuery) return true;
      const query = searchQuery.toLowerCase();
      return (
        audit.auditId?.toLowerCase().includes(query) ||
        audit.status?.toLowerCase().includes(query) ||
        audit.moduleName?.toLowerCase().includes(query) ||
        audit.component?.toLowerCase().includes(query) ||
        audit.actionType?.toLowerCase().includes(query)
      );
    });

    if (filteredData.length === 0) {
      alert("No matching data to download");
      return;
    }

    // Prepare data for CSV export
    const csvData = filteredData.map(audit => ({
      'Audit ID': audit.auditId || 'N/A',
      'Business Name': audit.businessName || 'N/A',
      'Event Timestamp': audit.eventTimestamp || 'N/A',
      'Business ID': audit.businessId || 'N/A',
      'Module Name': audit.moduleName || 'N/A',
      'Component': audit.component || 'N/A',
      'Action Type': audit.actionType || 'N/A',
      'Initiated By': formatInitiator(audit.initiatedBy) || 'N/A',
      'Actor ID': audit.actorId || 'N/A',
      'Actor Role': audit.actorRole || 'N/A',
      'Actor Type': audit.actorType || 'N/A',
      'Resource ID': audit.resourceId || 'N/A',
      'Resource Type': audit.resourceType || 'N/A',
      'Transaction ID': audit.transactionId || 'N/A',
      'Reference ID': audit.referenceId || 'N/A'
    }));

    exportToCSV(csvData, 'audit_compliance');
  };

  const handleNewAudit = () => {
    navigate("/createAudit");
  };

  // Pagination handlers
  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < totalPages) {
      setCurrentPage(newPage);
    }
  };

  const handlePageSizeChange = (newSize) => {
    setPageSize(newSize);
    setCurrentPage(0); // Reset to first page when changing page size
    setCorrectedTotal(null); // Reset corrected total when page size changes
  };

  // Handle filter changes
  const handleClearFilters = () => {
    setFilters({
      dateFrom: "",
      dateTo: "",
    });
  };

  const handleApplyFilters = () => {
    setFilterDrawerOpen(false);
  };

  // Get active filter count (date range only)
  const activeFilterCount =
    (filters.dateFrom ? 1 : 0) + (filters.dateTo ? 1 : 0);

  return (
    <>
      <div className="configurePage">
        {/* Header Section */}
        <div className="audit-header-section">
          <div className="audit-title-section">
            <div style={{ display: 'flex', flexDirection: 'row', gap: '10px' }}>
              <Text appearance="heading-s" color="primary-grey-100">Audit & Compliance Report</Text>
              <div className="dataProtectionOfficer-badge" style={{ marginTop: '5px' }}>
                <Text appearance="body-xs-bold" color="primary-grey-80">Governance</Text>
              </div>
            </div>
          </div>
          <div className="audit-button-group">
            {/* Uncomment when create functionality is ready */}
            {/* <ActionButton
              kind="primary"
              size="medium"
              state="normal"
              label="Create New Audit"
              onClick={handleNewAudit}
            /> */}
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
            <svg className="audit-search-icon" width="20" height="20" viewBox="0 0 24 24" fill="none">
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
            <button className="audit-icon-button" onClick={handleDownloadCSV} title="Download CSV">
              <IcDownload height={20} width={20} />
            </button>
          </div>
        </div>

        {/* Filter Drawer */}
        {filterDrawerOpen && (
          <>
            <div
              className="filter-drawer-overlay"
              onClick={() => setFilterDrawerOpen(false)}
            />
            <div className="filter-drawer">
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
                      width: '100%',
                      padding: '10px 12px',
                      border: '1px solid #e0e0e0',
                      borderRadius: '6px',
                      fontSize: '14px',
                    }}
                  />
                </div>

                {/* Event date range - filters table by Event timestamp column */}
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

        {/* Table Section */}
        <div className="audit-table-wrapper">
          <table className="audit-table">
            <thead>
              <tr>
                <th onClick={() => handleSort('auditId')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Audit ID
                    </Text>
                    {renderSortIcon('auditId')}
                  </div>
                </th>
                <th onClick={() => handleSort('businessName')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Business Name
                    </Text>
                    {renderSortIcon('businessName')}
                  </div>
                </th>
                <th onClick={() => handleSort('eventTimestamp')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Event timestamp
                    </Text>
                    {renderSortIcon('eventTimestamp')}
                  </div>
                </th>
                <th onClick={() => handleSort('businessId')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Business ID
                    </Text>
                    {renderSortIcon('businessId')}
                  </div>
                </th>
                <th onClick={() => handleSort('moduleName')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Module name
                    </Text>
                    {renderSortIcon('moduleName')}
                  </div>
                </th>
                <th onClick={() => handleSort('component')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Component
                    </Text>
                    {renderSortIcon('component')}
                  </div>
                </th>
                <th onClick={() => handleSort('actionType')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Action type
                    </Text>
                    {renderSortIcon('actionType')}
                  </div>
                </th>
                <th onClick={() => handleSort('initiatedBy')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Initiated by
                    </Text>
                    {renderSortIcon('initiatedBy')}
                  </div>
                </th>
                <th onClick={() => handleSort('actorId')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Actor ID
                    </Text>
                    {renderSortIcon('actorId')}
                  </div>
                </th>
                <th onClick={() => handleSort('actorRole')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Actor role
                    </Text>
                    {renderSortIcon('actorRole')}
                  </div>
                </th>
                <th onClick={() => handleSort('actorType')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Actor type
                    </Text>
                    {renderSortIcon('actorType')}
                  </div>
                </th>
                <th onClick={() => handleSort('resourceId')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Resource ID
                    </Text>
                    {renderSortIcon('resourceId')}
                  </div>
                </th>
                <th onClick={() => handleSort('resourceType')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Resource type
                    </Text>
                    {renderSortIcon('resourceType')}
                  </div>
                </th>
                <th onClick={() => handleSort('transactionId')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Transaction ID
                    </Text>
                    {renderSortIcon('transactionId')}
                  </div>
                </th>
                <th onClick={() => handleSort('referenceId')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Reference ID
                    </Text>
                    {renderSortIcon('referenceId')}
                  </div>
                </th>
                <th onClick={() => handleSort('ipAddress')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      IP Address
                    </Text>
                    {renderSortIcon('ipAddress')}
                  </div>
                </th>
                <th onClick={() => handleSort('payloadHash')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Payload hash
                    </Text>
                    {renderSortIcon('payloadHash')}
                  </div>
                </th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="17" style={{ textAlign: 'center', padding: '40px' }}>
                    <Text appearance="body-m" color="primary-grey-80">
                      Loading audit reports...
                    </Text>
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan="17" style={{ textAlign: 'center', padding: '40px' }}>
                    <Text appearance="body-m" color="feedback_error_50">
                      {error}
                    </Text>
                  </td>
                </tr>
              ) : sortedFilteredReports.length === 0 ? (
                <tr>
                  <td colSpan="17" style={{ textAlign: 'center', padding: '40px' }}>
                    <Text appearance="body-m" color="primary-grey-80">
                      {searchQuery || activeFilterCount > 0 ? `No results found for current filters` : "No audit reports found."}
                    </Text>
                  </td>
                </tr>
              ) : (
                sortedFilteredReports.map((audit) => (
                  <tr key={audit.auditId}>
                    <td>
                      <Text appearance="body-xs-bold" color="black">
                        {audit.auditId}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="black">
                        {audit.businessName}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="black">
                        {audit.eventTimestamp}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs-bold" color="black">
                        {audit.businessId}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs-bold" color="black">
                        {audit.moduleName}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs-bold" color="black">
                        {audit.component}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs-bold" color="black">
                        {audit.actionType}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="black">
                        {formatInitiator(audit.initiatedBy)}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="black">
                        {audit.actorId}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="black">
                        {audit.actorRole}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="black">
                        {audit.actorType}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="black">
                        {audit.resourceId}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs-bold" color="black">
                        {audit.resourceType}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs-bold" color="black">
                        {audit.transactionId}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs-bold" color="black">
                        {audit.referenceId}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="black">
                        {audit.ipAddress}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs" color="black">
                        {audit.payloadHash}
                      </Text>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Controls */}
        {!loading && !error && (totalRecords > 0 || sortedFilteredReports.length > 0) && (
          <div className="audit-pagination-container">
            <div className="audit-pagination-info">
              <Text appearance="body-xs" color="primary-grey-80">
                Showing {currentPage * pageSize + 1} to {Math.min(currentPage * pageSize + sortedFilteredReports.length, totalRecords)} of {totalRecords} records
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

                <div className="audit-page-numbers">
                  {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
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
                        className={`audit-page-number ${currentPage === pageNum ? 'active' : ''}`}
                        onClick={() => handlePageChange(pageNum)}
                      >
                        {pageNum + 1}
                      </button>
                    );
                  })}
                </div>

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
