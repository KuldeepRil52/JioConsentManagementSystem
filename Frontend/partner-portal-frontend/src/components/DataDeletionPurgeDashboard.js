import { ActionButton, Icon, Image, Text, InputFieldV2, BadgeV2 } from "../custom-components";
import "../styles/pageConfiguration.css";
import "../styles/templates.css";
import { useNavigate } from "react-router-dom";
import { useEffect, useState, useMemo, useCallback } from "react";
import { IcSuccessColored, IcError, IcWarningColored, IcSuccess, IcTrash, IcNotification, IcSort, IcDownload, IcFilter, IcClose, IcRefresh, IcProfile } from "../custom-components/Icon";
import { useDispatch, useSelector } from "react-redux";
import { exportToCSV } from "../utils/csvExport";
import { generateTransactionId } from "../utils/transactionId";
import { formatToIST } from "../utils/dateUtils";
import config from "../utils/config";

const DataDeletionPurgeDashboard = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const dashboard = new URL("../assets/dashboard.svg", import.meta.url).href;

  const tenantId = useSelector((state) => state.common.tenant_id);
  const businessId = useSelector((state) => state.common.business_id);
  const sessionToken = useSelector((state) => state.common.session_token);

  const [searchQuery, setSearchQuery] = useState("");
  const [filterDrawerOpen, setFilterDrawerOpen] = useState(false);
  const [filters, setFilters] = useState({
    dfStatus: [],
    overallStatus: [],
    reason: [],
    dateFrom: "",
    dateTo: "",
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [stats, setStats] = useState({
    deletionRequests: 0,
    completed: 0,
    inProgress: 0,
    deferred: 0,
    userNotified: 0,
    notificationPending: 0,
  });

  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState('asc');

  // Helper function to safely convert value to number, defaulting to 0 (handles 0 explicitly)
  const safeNumber = (value) => {
    if (value === undefined || value === null) return 0;
    const num = Number(value);
    return isNaN(num) ? 0 : num;
  };

  // Fetch deletion requests from API
  const fetchDeletionRequests = useCallback(async () => {
    if (!tenantId || !businessId || !sessionToken) {
      console.warn("Missing tenantId, businessId, or sessionToken, skipping API call");
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const txnId = generateTransactionId();

      // Ensure Bearer prefix is added if not already present
      const authToken = sessionToken?.startsWith('Bearer ') ? sessionToken : `Bearer ${sessionToken || ''}`;

      const response = await fetch(config.consent_deletion_list, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'X-Tenant-Id': tenantId,
          'X-Business-Id': businessId,
          'X-Transaction-Id': txnId,
          'x-session-token': authToken,
        },
      });

      if (!response.ok) {
        throw new Error(`API error: ${response.status} ${response.statusText}`);
      }

      const responseData = await response.json();
      console.log("Deletion requests API response:", responseData);

      // Update stats from data.overview (always set stats, even if zero)
      const overview = responseData?.data?.overview || {};
      setStats({
        deletionRequests: safeNumber(overview.deletionRequests),
        completed: safeNumber(overview.completed),
        inProgress: safeNumber(overview.inProgress),
        deferred: safeNumber(overview.deferred),
        userNotified: safeNumber(overview.userNotified),
        notificationPending: safeNumber(overview.notificationPending),
      });

      // Update deletion requests from data.deletionRequests.data
      if (responseData?.data?.deletionRequests?.data && Array.isArray(responseData.data.deletionRequests.data)) {
        const apiRequests = responseData.data.deletionRequests.data;

        // Map API response to component format
        const mappedRequests = apiRequests.map((item, index) => {
          // Parse processors string (e.g., "0/0 Done" -> processorsCompleted: 0, processorsTotal: 0)
          const processorsMatch = item.processors?.match(/(\d+)\/(\d+)/);
          const processorsCompleted = processorsMatch ? parseInt(processorsMatch[1], 10) : 0;
          const processorsTotal = processorsMatch ? parseInt(processorsMatch[2], 10) : 0;

          // Map trigger to reason
          const reasonMap = {
            'CONSENT_WITHDRAWN': 'Withdraw',
            'CONSENT_EXPIRED': 'Expiry',
            'EXPIRY': 'Expiry',
            'WITHDRAW': 'Withdraw',
          };
          const reason = reasonMap[item.trigger] || item.trigger || 'Withdraw';

          // Map dfStatus (convert to title case: DELETED -> Deleted)
          const dfStatus = item.dfStatus
            ? item.dfStatus.charAt(0).toUpperCase() + item.dfStatus.slice(1).toLowerCase()
            : 'Pending';

          // Map overall status
          const overallMap = {
            'Done': 'All deleted',
            'DONE': 'All deleted',
            'Partial': 'Partial deleted',
            'PARTIAL': 'Partial deleted',
            'Deferred': 'Deferred',
            'DEFERRED': 'Deferred',
          };
          const overallStatus = overallMap[item.overallCompletion] || item.overallCompletion || 'Pending';

          return {
            id: index + 1,
            consentId: item.consentId || '',
            dataPrincipal: item.dataPrincipal || '',
            reason: reason,
            dfStatus: dfStatus,
            processorsCompleted: processorsCompleted,
            processorsTotal: processorsTotal,
            overallStatus: overallStatus,
            eventTimestamp: item.eventTimestamp,
            eventId: item.eventId,
          };
        });

        setDeletionRequests(mappedRequests);
      } else {
        // If no data, set empty array
        setDeletionRequests([]);
      }
    } catch (err) {
      console.error("Error fetching deletion requests:", err);
      setError(err.message);
      // Keep dummy data on error for now
    } finally {
      setLoading(false);
    }
  }, [tenantId, businessId, sessionToken]);

  // Fetch data when component mounts
  useEffect(() => {
    if (tenantId && businessId && sessionToken) {
      fetchDeletionRequests();
    }
  }, [tenantId, businessId, sessionToken, fetchDeletionRequests]);

  // Deletion requests from API
  const [deletionRequests, setDeletionRequests] = useState([]);

  // Filter deletion requests based on search query and filters
  const filteredRequests = deletionRequests.filter((item) => {
    // Search query filter
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      const matchesSearch =
        item.consentId?.toLowerCase().includes(query) ||
        item.dataPrincipal?.toLowerCase().includes(query) ||
        item.reason?.toLowerCase().includes(query);
      if (!matchesSearch) return false;
    }

    // DF Status filter
    if (filters.dfStatus.length > 0) {
      const itemStatus = item.dfStatus?.toUpperCase() || "";
      const matchesStatus = filters.dfStatus.some(
        (status) => status.toUpperCase() === itemStatus
      );
      if (!matchesStatus) return false;
    }

    // Overall Status filter
    if (filters.overallStatus.length > 0) {
      const itemStatus = item.overallStatus?.toLowerCase() || "";
      const matchesStatus = filters.overallStatus.some(
        (status) => status.toLowerCase() === itemStatus
      );
      if (!matchesStatus) return false;
    }

    // Reason filter
    if (filters.reason.length > 0) {
      const itemReason = item.reason?.toUpperCase() || "";
      const matchesReason = filters.reason.some(
        (reason) => reason.toUpperCase() === itemReason
      );
      if (!matchesReason) return false;
    }

    // Date range filter (based on event timestamp - same as event timestamp in API)
    const hasDateFilter = filters.dateFrom || filters.dateTo;
    if (hasDateFilter) {
      const rawTs = item.eventTimestamp;
      if (rawTs == null || rawTs === '') return false;
      const eventTime = typeof rawTs === 'number' ? rawTs : new Date(String(rawTs).endsWith('Z') ? rawTs : rawTs + 'Z').getTime();
      if (isNaN(eventTime)) return false;
      if (filters.dateFrom) {
        const fromTime = new Date(filters.dateFrom + 'T00:00:00.000Z').getTime();
        if (eventTime < fromTime) return false;
      }
      if (filters.dateTo) {
        const toTime = new Date(filters.dateTo + 'T23:59:59.999Z').getTime();
        if (eventTime > toTime) return false;
      }
    }

    return true;
  });

  // Stats are currently using dummy data matching the screenshot
  // TODO: Update with actual API data when available

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

  const sortedRequests = useMemo(() => {
    if (!sortColumn) return filteredRequests;

    return [...filteredRequests].sort((a, b) => {
      // Handle processor fraction sorting (avoid division by zero)
      if (sortColumn === 'processors') {
        const aTotal = a.processorsTotal || 0;
        const bTotal = b.processorsTotal || 0;
        const aRatio = aTotal > 0 ? a.processorsCompleted / aTotal : 0;
        const bRatio = bTotal > 0 ? b.processorsCompleted / bTotal : 0;
        if (aRatio < bRatio) return sortDirection === 'asc' ? -1 : 1;
        if (aRatio > bRatio) return sortDirection === 'asc' ? 1 : -1;
        return 0;
      }

      let aValue = a[sortColumn];
      let bValue = b[sortColumn];
      // Normalize for string comparison
      const aStr = aValue != null ? String(aValue).toLowerCase() : '';
      const bStr = bValue != null ? String(bValue).toLowerCase() : '';

      if (aStr < bStr) return sortDirection === 'asc' ? -1 : 1;
      if (aStr > bStr) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }, [filteredRequests, sortColumn, sortDirection]);

  // Handle CSV download – use explicit headers and row mapping so columns never shift
  const CSV_HEADERS = [
    'Consent ID',
    'Data Principal',
    'Reason',
    'DF Status',
    'Processors',
    'Overall Deletion Status',
    'Event Timestamp',
  ];

  const handleDownloadCSV = () => {
    if (!sortedRequests || sortedRequests.length === 0) {
      alert("No data available to download");
      return;
    }

    const csvData = sortedRequests.map((request) => {
      const processorsCompleted = request.processorsCompleted ?? 0;
      const processorsTotal = request.processorsTotal ?? 0;
      const processorsStr = `${processorsCompleted}/${processorsTotal}`;
      return {
        [CSV_HEADERS[0]]: request.consentId ?? 'N/A',
        [CSV_HEADERS[1]]: request.dataPrincipal ?? 'N/A',
        [CSV_HEADERS[2]]: request.reason ?? 'N/A',
        [CSV_HEADERS[3]]: request.dfStatus ?? 'N/A',
        [CSV_HEADERS[4]]: processorsStr,
        [CSV_HEADERS[5]]: request.overallStatus ?? 'N/A',
        [CSV_HEADERS[6]]: formatToIST(request.eventTimestamp),
      };
    });

    exportToCSV(csvData, 'data_deletion_purge_requests', CSV_HEADERS);
  };

  // Handle filter changes
  const handleDFStatusFilterChange = (status) => {
    setFilters((prev) => ({
      ...prev,
      dfStatus: prev.dfStatus.includes(status)
        ? prev.dfStatus.filter((s) => s !== status)
        : [...prev.dfStatus, status],
    }));
  };

  const handleOverallStatusFilterChange = (status) => {
    setFilters((prev) => ({
      ...prev,
      overallStatus: prev.overallStatus.includes(status)
        ? prev.overallStatus.filter((s) => s !== status)
        : [...prev.overallStatus, status],
    }));
  };

  const handleReasonFilterChange = (reason) => {
    setFilters((prev) => ({
      ...prev,
      reason: prev.reason.includes(reason)
        ? prev.reason.filter((r) => r !== reason)
        : [...prev.reason, reason],
    }));
  };

  const handleClearFilters = () => {
    setFilters({
      dfStatus: [],
      overallStatus: [],
      reason: [],
      dateFrom: "",
      dateTo: "",
    });
  };

  const handleApplyFilters = () => {
    setFilterDrawerOpen(false);
  };

  // Get active filter count
  const activeFilterCount =
    filters.dfStatus.length +
    filters.overallStatus.length +
    filters.reason.length +
    (filters.dateFrom ? 1 : 0) +
    (filters.dateTo ? 1 : 0);

  // Helper function to get DF status badge with custom colors matching screenshot
  const getDFStatusBadge = (status) => {
    const badgeStyle = {
      display: 'inline-block',
      padding: '4px 12px',
      borderRadius: '6px',
      fontSize: '12px',
      fontWeight: '500',
      textAlign: 'center',
    };

    switch (status) {
      case "Deleted":
        return (
          <span
            style={{
              ...badgeStyle,
              backgroundColor: '#D1FAE5', // Light green background
              color: '#065F46', // Dark green text
            }}
          >
            {status}
          </span>
        );
      case "Pending":
        return (
          <span
            style={{
              ...badgeStyle,
              backgroundColor: '#FED7AA', // Light orange background
              color: '#C2410C', // Dark orange text
            }}
          >
            {status}
          </span>
        );
      case "Deferred":
        return (
          <span
            style={{
              ...badgeStyle,
              backgroundColor: '#F3F4F6', // Light grey background
              color: '#6B7280', // Dark grey text
            }}
          >
            {status}
          </span>
        );
      default:
        return <Text appearance="body-xs">{status}</Text>;
    }
  };

  // Helper function to get overall status (plain text, no coloring)
  const getOverallStatusBadge = (status) => {
    return <Text appearance="body-xs" color="black">{status}</Text>;
  };

  return (
    <div className="configurePage">
      {/* Header Section */}
      <div className="template-header-section">
        <div className="template-title-section">
          <div style={{ display: 'flex', flexDirection: 'row', gap: '10px' }}>
            <Text appearance="heading-s" color="primary-grey-100">Data deletion and purge dashboard</Text>
            <div className="dataProtectionOfficer-badge" style={{ marginTop: '5px' }}>
              <Text appearance="body-xs-bold" color="primary-grey-80">Monitoring</Text>
            </div>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="configure-template-page">
          <div className="template-img">
            <Image src={dashboard}></Image>
          </div>
          <Text color="primary-grey-80" style={{ marginTop: "0.5rem" }}>
            Loading deletion requests...
          </Text>
        </div>
      ) : error ? (
        <div className="configure-template-page">
          <div className="template-img">
            <Image src={dashboard}></Image>
          </div>
          <Text color="primary-grey-80" style={{ marginTop: "0.5rem" }}>
            Error loading deletion requests: {error}
          </Text>
        </div>
      ) : (
        <>
          {/* Stats Section */}
          <div style={{ marginTop: '24px', marginBottom: '16px' }}>
            <Text appearance="body-m-bold" color="primary-grey-80">Overview metrics</Text>
          </div>
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(5, 1fr)',
            gap: '16px',
            marginBottom: '0',
            width: '94%'
          }}>
            {/* Card 1: Deletion requests */}
            <div style={{
              backgroundColor: '#FFFFFF',
              borderRadius: '8px',
              padding: '20px',
              display: 'flex',
              flexDirection: 'row',
              alignItems: 'center',
              gap: '16px',
              border: '1px solid #E5E7EB',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)'
            }}>
              <div style={{
                width: '48px',
                height: '48px',
                borderRadius: '50%',
                backgroundColor: '#FEE2E2',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0
              }}>
                <IcTrash height={24} width={24} color="#DC2626" />
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <Text appearance="heading-xs" color="primary-grey-100" style={{ fontWeight: '600' }}>
                  {String(stats.deletionRequests ?? 0)}
                </Text>
                <Text appearance="body-xs" color="primary-grey-80">
                  Deletion requests
                </Text>
              </div>
            </div>

            {/* Card 2: Completed */}
            <div style={{
              backgroundColor: '#FFFFFF',
              borderRadius: '8px',
              padding: '20px',
              display: 'flex',
              flexDirection: 'row',
              alignItems: 'center',
              gap: '16px',
              border: '1px solid #E5E7EB',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)'
            }}>
              <div style={{
                width: '48px',
                height: '48px',
                borderRadius: '50%',
                backgroundColor: '#D1FAE5',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0
              }}>
                <IcSuccessColored height={24} width={24} color="#25AB21" />
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <Text appearance="heading-xs" color="primary-grey-100" style={{ fontWeight: '600' }}>
                  {String(stats.completed ?? 0)}
                </Text>
                <Text appearance="body-xs" color="primary-grey-80">
                  Completed
                </Text>
              </div>
            </div>

            {/* Card 3: In progress */}
            <div style={{
              backgroundColor: '#FFFFFF',
              borderRadius: '8px',
              padding: '20px',
              display: 'flex',
              flexDirection: 'row',
              alignItems: 'center',
              gap: '16px',
              border: '1px solid #E5E7EB',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)'
            }}>
              <div style={{
                width: '48px',
                height: '48px',
                borderRadius: '50%',
                backgroundColor: '#DBEAFE',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0
              }}>
                <IcRefresh height={24} width={24} color="#2563EB" />
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <Text appearance="heading-xs" color="primary-grey-100" style={{ fontWeight: '600' }}>
                  {String(stats.inProgress ?? 0)}
                </Text>
                <Text appearance="body-xs" color="primary-grey-80">
                  In progress
                </Text>
              </div>
            </div>

            {/* Card 4: User notified */}
            <div style={{
              backgroundColor: '#FFFFFF',
              borderRadius: '8px',
              padding: '20px',
              display: 'flex',
              flexDirection: 'row',
              alignItems: 'center',
              gap: '16px',
              border: '1px solid #E5E7EB',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)'
            }}>
              <div style={{
                width: '48px',
                height: '48px',
                borderRadius: '50%',
                backgroundColor: '#FCE7F3',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                position: 'relative',
                flexShrink: 0
              }}>
                <IcProfile height={24} width={24} color="#EC4899" />
                <div style={{
                  position: 'absolute',
                  bottom: '-2px',
                  right: '-2px',
                  width: '20px',
                  height: '20px',
                  borderRadius: '50%',
                  backgroundColor: '#D1FAE5',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  border: '2px solid #FFFFFF'
                }}>
                  <IcSuccess height={12} width={12} color="#25AB21" />
                </div>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <Text appearance="heading-xs" color="primary-grey-100" style={{ fontWeight: '600' }}>
                  {String(stats.userNotified ?? 0)}
                </Text>
                <Text appearance="body-xs" color="primary-grey-80">
                  User notified
                </Text>
              </div>
            </div>

            {/* Card 5: Notification pending */}
            <div style={{
              backgroundColor: '#FFFFFF',
              borderRadius: '8px',
              padding: '20px',
              display: 'flex',
              flexDirection: 'row',
              alignItems: 'center',
              gap: '16px',
              border: '1px solid #E5E7EB',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)'
            }}>
              <div style={{
                width: '48px',
                height: '48px',
                borderRadius: '50%',
                backgroundColor: '#F3E8FF',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                position: 'relative',
                flexShrink: 0
              }}>
                <IcNotification height={24} width={24} color="#9333EA" />
                <div style={{
                  position: 'absolute',
                  bottom: '-2px',
                  right: '-2px',
                  width: '20px',
                  height: '20px',
                  borderRadius: '50%',
                  backgroundColor: '#FED7AA',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  border: '2px solid #FFFFFF'
                }}>
                  <IcWarningColored height={12} width={12} color="#C2410C" />
                </div>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <Text appearance="heading-xs" color="primary-grey-100" style={{ fontWeight: '600' }}>
                  {String(stats.notificationPending ?? 0)}
                </Text>
                <Text appearance="body-xs" color="primary-grey-80">
                  Notification pending
                </Text>
              </div>
            </div>
          </div>

          {/* Consent deletion requests section */}
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginTop: '32px',
            marginBottom: '16px'
          }}>
            <Text appearance="body-m-bold" color="primary-grey-80">
              Consent deletion requests
            </Text>
            <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
              <div className="template-search-container" style={{ margin: 0 }}>
                <input
                  type="text"
                  placeholder="Search system name or description"
                  className="template-search-input"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
                <svg className="template-search-icon" width="20" height="20" viewBox="0 0 24 24" fill="none">
                  <circle cx="11" cy="11" r="8" stroke="currentColor" strokeWidth="2" />
                  <path d="M21 21L16.65 16.65" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                </svg>
              </div>
              <button
                className={`template-icon-button ${filterDrawerOpen ? "active" : ""} ${activeFilterCount > 0 ? "has-filters" : ""}`}
                onClick={() => setFilterDrawerOpen(!filterDrawerOpen)}
                title="Filter"
                style={{ margin: 0 }}
              >
                <IcFilter height={20} width={20} />
                {activeFilterCount > 0 && (
                  <span className="filter-badge">{activeFilterCount}</span>
                )}
              </button>
              <button className="template-icon-button" onClick={handleDownloadCSV} title="Download CSV">
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
                  {/* DF Status Filter */}
                  <div className="filter-section">
                    <Text appearance="body-s-bold" color="primary-grey-100">
                      DF Status
                    </Text>
                    <div className="filter-chips">
                      {["Deleted", "Pending", "Deferred"].map((status) => (
                        <button
                          key={status}
                          className={`filter-chip ${filters.dfStatus.includes(status) ? "active" : ""}`}
                          onClick={() => handleDFStatusFilterChange(status)}
                        >
                          {status}
                          {filters.dfStatus.includes(status) && (
                            <IcClose height={16} width={16} />
                          )}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Overall Status Filter */}
                  <div className="filter-section">
                    <Text appearance="body-s-bold" color="primary-grey-100">
                      Overall Deletion Status
                    </Text>
                    <div className="filter-chips">
                      {["All deleted", "Partial deleted", "Deferred"].map((status) => (
                        <button
                          key={status}
                          className={`filter-chip ${filters.overallStatus.includes(status) ? "active" : ""}`}
                          onClick={() => handleOverallStatusFilterChange(status)}
                        >
                          {status}
                          {filters.overallStatus.includes(status) && (
                            <IcClose height={16} width={16} />
                          )}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Reason Filter */}
                  <div className="filter-section">
                    <Text appearance="body-s-bold" color="primary-grey-100">
                      Reason
                    </Text>
                    <div className="filter-chips">
                      {["Withdraw", "Expiry"].map((reason) => (
                        <button
                          key={reason}
                          className={`filter-chip ${filters.reason.includes(reason) ? "active" : ""}`}
                          onClick={() => handleReasonFilterChange(reason)}
                        >
                          {reason}
                          {filters.reason.includes(reason) && (
                            <IcClose height={16} width={16} />
                          )}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Created Date Range */}
                  <div className="filter-section">
                    <Text appearance="body-s-bold" color="primary-grey-100">
                      Created date range
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
          <div className="template-table-wrapper">
            <table className="template-table">
              <thead>
                <tr>
                  <th onClick={() => handleSort('consentId')} style={{ cursor: 'pointer' }}>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Consent ID
                      </Text>
                      {renderSortIcon('consentId')}
                    </div>
                  </th>
                  <th onClick={() => handleSort('dataPrincipal')} style={{ cursor: 'pointer' }}>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Data principal
                      </Text>
                      {renderSortIcon('dataPrincipal')}
                    </div>
                  </th>
                  <th onClick={() => handleSort('reason')} style={{ cursor: 'pointer' }}>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Reason
                      </Text>
                      {renderSortIcon('reason')}
                    </div>
                  </th>
                  <th onClick={() => handleSort('dfStatus')} style={{ cursor: 'pointer' }}>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        DF status
                      </Text>
                      {renderSortIcon('dfStatus')}
                    </div>
                  </th>
                  <th onClick={() => handleSort('processors')} style={{ cursor: 'pointer' }}>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Processors
                      </Text>
                      {renderSortIcon('processors')}
                    </div>
                  </th>
                  <th onClick={() => handleSort('overallStatus')} style={{ cursor: 'pointer' }}>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Overall deletion status
                      </Text>
                      {renderSortIcon('overallStatus')}
                    </div>
                  </th>
                </tr>
              </thead>
              <tbody>
                {sortedRequests.length === 0 ? (
                  <tr>
                    <td colSpan="6" style={{ textAlign: 'center', padding: '40px' }}>
                      <Text appearance="body-m" color="primary-grey-80">
                        {searchQuery ? `No results found for "${searchQuery}"` : "No deletion requests found."}
                      </Text>
                    </td>
                  </tr>
                ) : (
                  sortedRequests.map((item) => (
                    <tr key={item.id}>
                      <td>
                        <div
                          style={{
                            cursor: 'pointer',
                            display: 'inline-block'
                          }}
                          onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            navigate(`/consent-deletion-detail/${item.eventId}`);
                          }}
                        >
                          <Text
                            appearance='body-xs'
                            color="primary"
                            style={{ textDecoration: 'underline' }}
                          >
                            {item.consentId}
                          </Text>
                        </div>
                      </td>
                      <td>
                        <Text appearance="body-xs" color="black">
                          {item.dataPrincipal}
                        </Text>
                      </td>
                      <td>
                        <Text appearance="body-xs" color="black">
                          {item.reason}
                        </Text>
                      </td>
                      <td>
                        {getDFStatusBadge(item.dfStatus)}
                      </td>
                      <td>
                        <Text appearance="body-xs" color="black">
                          {item.processorsCompleted}/{item.processorsTotal}
                        </Text>
                      </td>
                      <td>
                        {getOverallStatusBadge(item.overallStatus)}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
};

export default DataDeletionPurgeDashboard;

