import { ActionButton, Text, Icon, BadgeV2, SearchBox } from "../custom-components";
import {
  IcEditPen,
  IcError,
  IcSort,
  IcSuccessColored,
  IcSwap,
  IcWarningColored,
  IcDownload,
  IcVisible,
  IcFilter,
  IcClose,
} from "../custom-components/Icon";
import { useNavigate } from "react-router-dom";
import "../styles/EmailTemplate.css";
import "../styles/breachNotifications.css";
import "../styles/auditCompliance.css";
import {
  IcRequest,
  IcSmartSwitchPlug,
  IcTimezone,
  IcWhatsapp,
} from "../custom-components/Icon";
import { Link } from "react-router-dom";
import React, { useEffect, useState, useMemo } from "react";
import { useDispatch } from "react-redux";
import { useSelector } from "react-redux";
import { getProcessor, getGrievances, getGrievanceDetails } from "../store/actions/CommonAction";
import { exportToCSV } from "../utils/csvExport";
import { formatToIST12Hour } from "../utils/dateUtils";

const Request = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [requests, setRequests] = useState([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [visiblePrincipals, setVisiblePrincipals] = useState({});
  const [sortColumn, setSortColumn] = useState('date');
  const [sortDirection, setSortDirection] = useState('desc'); // Latest records on top
  const [loading, setLoading] = useState(false);
  
  // Filter state
  const [filterDrawerOpen, setFilterDrawerOpen] = useState(false);
  const [filters, setFilters] = useState({
    status: [],
    requestType: [],
    dateFrom: "",
    dateTo: "",
  });
  const [slaDays, setSlaDays] = useState(60); // Default SLA, will be fetched from config
  const [slaLoaded, setSlaLoaded] = useState(false); // Track if SLA has been loaded

  // Pagination state
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalRecords, setTotalRecords] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const tenantId = useSelector((state) => state.common.tenant_id);
  const businessId = useSelector((state) => state.common.business_id);
  const sessionToken = useSelector((state) => state.common.session_token);

  // Format date/time in IST timezone
  const formatDateTime = formatToIST12Hour;

  // Fetch SLA from Grievance Configuration
  useEffect(() => {
    const fetchSlaConfig = async () => {
      try {
        const response = await getGrievanceDetails(sessionToken, tenantId, businessId);
        console.log("📋 Grievance Config Response:", response);
        const config = response?.searchList?.[0]?.configurationJson;
        console.log("📋 Grievance Config JSON:", config);
        console.log("📋 SLA Timeline from config:", config?.slaTimeline);
        
        if (config?.slaTimeline?.value) {
          // Convert SLA to days based on unit
          let slaDaysValue = parseInt(config.slaTimeline.value) || 60;
          const unit = config.slaTimeline.unit?.toUpperCase();
          
          console.log("📋 SLA Value:", config.slaTimeline.value, "Unit:", unit);
          
          // Convert to days if unit is different
          if (unit === 'HOURS') {
            slaDaysValue = Math.ceil(slaDaysValue / 24);
          } else if (unit === 'WEEKS') {
            slaDaysValue = slaDaysValue * 7;
          } else if (unit === 'MONTHS') {
            slaDaysValue = slaDaysValue * 30;
          }
          // Default is DAYS, no conversion needed
          
          setSlaDays(slaDaysValue);
          console.log("✅ SLA set to:", slaDaysValue, "days");
        } else {
          console.warn("⚠️ No SLA Timeline found in config, using default 60 days");
        }
      } catch (error) {
        console.error("❌ Error fetching SLA config:", error);
        // Keep default 60 days on error
      } finally {
        setSlaLoaded(true); // Mark SLA as loaded (even on error, use default)
      }
    };

    if (sessionToken && tenantId && businessId) {
      fetchSlaConfig();
    }
  }, [sessionToken, tenantId, businessId]);


  // const handleClick = () => {
  //   navigate("/editRequest");
  // };

  const handleClick = (reqId) => {
    navigate(`/editRequest/${reqId}`);
  };

  // Fetch ALL grievances at once for proper sorting, then paginate client-side
    const fetchData = async () => {
    setLoading(true);
    try {
      // Fetch all records with a large size to get everything
      const res = await dispatch(getGrievances(tenantId, businessId, 1, 10000));

      // Handle both response structures: res.data (real API) or res directly (sandbox)
      const dataArray = Array.isArray(res?.data) ? res.data : Array.isArray(res) ? res : [];
      
      if (dataArray.length > 0) {
        console.log("🔍 Formatting data with slaDays:", slaDays);
        const formatted = dataArray.map((item, index) => {
          // Calculate days left based on SLA from Grievance Configuration
          let daysLeft = null;
          const createdAtValue = item.createdAt;
          
          if (createdAtValue) {
            const createdDate = new Date(createdAtValue);
            const currentDate = new Date();
            if (!isNaN(createdDate.getTime())) {
              const daysPassed = Math.floor((currentDate - createdDate) / (1000 * 60 * 60 * 24));
              daysLeft = Math.max(slaDays - daysPassed, 0);
              
              // Debug first few items
              if (index < 3) {
                console.log(`📅 Item ${index}: createdAt=${createdAtValue}, daysPassed=${daysPassed}, slaDays=${slaDays}, daysLeft=${daysLeft}`);
              }
            } else {
              console.warn(`⚠️ Item ${index}: Invalid date - ${createdAtValue}`);
            }
          } else {
            console.warn(`⚠️ Item ${index}: No createdAt field`);
          }

          // Extract mobile and name from userDetails
          const mobileNumber = item.userDetails?.["Mobile Number"] || item.userDetails?.MOBILE || "-";
          const name = item.userDetails?.Name || item.userDetails?.NAME || "-";

          return {
            id: item.grievanceId,
            type: item.grievanceType,
            date: formatDateTime(item.createdAt),
            rawDate: item.createdAt, // Store raw timestamp for sorting
            principal: { mobile: mobileNumber, name: name },
            daysLeft: daysLeft,
            status: item.status,
          };
        });

        setRequests(formatted);
        setTotalRecords(formatted.length);
        setTotalPages(Math.ceil(formatted.length / pageSize));
      } else {
        setRequests([]);
        setTotalRecords(0);
        setTotalPages(0);
      }
    } catch (error) {
      console.error("Error fetching grievances:", error);
      setRequests([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // Only fetch grievances after SLA config has been loaded
    if (slaLoaded) {
      fetchData();
    }
  }, [dispatch, businessId, slaLoaded, slaDays]);

  // Update total pages when page size changes
  useEffect(() => {
    if (requests.length > 0) {
      setTotalPages(Math.ceil(requests.length / pageSize));
      // Reset to first page if current page is out of bounds
      if (currentPage >= Math.ceil(requests.length / pageSize)) {
        setCurrentPage(0);
      }
    }
  }, [pageSize, requests.length]);

  // Pagination handlers
  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < totalPages) {
      setCurrentPage(newPage);
    }
  };

  const handlePageSizeChange = (newSize) => {
    setPageSize(newSize);
    setCurrentPage(0); // Reset to first page when page size changes
  };

  // Calculate status counts (from current page - for accurate counts, API should return summary)
  const totalCount = totalRecords || requests.length || 0;
  const newCount = requests.filter(req =>
    req.status?.toUpperCase() === 'NEW'
  ).length || 0;
  const inProgressCount = requests.filter(req =>
    req.status?.toUpperCase() === 'INPROCESS'
  ).length || 0;
  const resolvedCount = requests.filter(req =>
    req.status?.toUpperCase() === 'RESOLVED'
  ).length || 0;
  // Count both L1_ESCALATED and L2_ESCALATED statuses
  const escalatedCount = requests.filter(req => {
    const status = req.status?.toUpperCase();
    return status === 'ESCALATED' || status === 'L1_ESCALATED' || status === 'L2_ESCALATED';
  }).length || 0;

  // Handle toggle visibility for data principal
  const togglePrincipalVisibility = (reqId) => {
    setVisiblePrincipals(prev => ({
      ...prev,
      [reqId]: !prev[reqId]
    }));
  };

  // Filter handlers
  const handleStatusFilterChange = (status) => {
    setFilters(prev => ({
      ...prev,
      status: prev.status.includes(status)
        ? prev.status.filter(s => s !== status)
        : [...prev.status, status]
    }));
  };

  const handleRequestTypeFilterChange = (type) => {
    setFilters(prev => ({
      ...prev,
      requestType: prev.requestType.includes(type)
        ? prev.requestType.filter(t => t !== type)
        : [...prev.requestType, type]
    }));
  };

  const handleClearFilters = () => {
    setFilters({
      status: [],
      requestType: [],
      dateFrom: "",
      dateTo: "",
    });
  };

  // Count active filters
  const activeFilterCount = 
    filters.status.length + 
    filters.requestType.length + 
    (filters.dateFrom ? 1 : 0) + 
    (filters.dateTo ? 1 : 0);

  // Get unique request types for filter options
  const uniqueRequestTypes = [...new Set(requests.map(req => req.type).filter(Boolean))];

  // Filter requests based on search query and filters
  const filteredRequests = requests.filter(req => {
    // Search filter
    const matchesSearch = !searchQuery || 
      req.id?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      req.type?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      req.principal?.mobile?.includes(searchQuery) ||
      req.principal?.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      req.status?.toLowerCase().includes(searchQuery.toLowerCase());

    // Status filter
    const matchesStatus = filters.status.length === 0 || 
      filters.status.includes(req.status?.toUpperCase());

    // Request type filter
    const matchesType = filters.requestType.length === 0 || 
      filters.requestType.includes(req.type);

    // Date range filter
    const reqDate = new Date(req.rawDate);
    const matchesDateFrom = !filters.dateFrom || reqDate >= new Date(filters.dateFrom);
    const matchesDateTo = !filters.dateTo || reqDate <= new Date(filters.dateTo + 'T23:59:59');

    return matchesSearch && matchesStatus && matchesType && matchesDateFrom && matchesDateTo;
  });

  // Format status for display
  const formatStatus = (status) => {
    if (!status) return 'N/A';
    const statusMap = {
      'NEW': 'New',
      'INPROCESS': 'In Process',
      'RESOLVED': 'Resolved',
      'ESCALATED': 'Escalated',
      'L1_ESCALATED': 'L1 Escalated',
      'L2_ESCALATED': 'L2 Escalated',
      'REJECTED': 'Rejected',
    };
    return statusMap[status.toUpperCase()] || status;
  };

  // Get status badge styling (matching card colors)
  const getStatusBadgeStyle = (status) => {
    if (!status) return { backgroundColor: '#f5f5f5', color: '#666666' };

    const statusUpper = status.toUpperCase();
    const styles = {
      'NEW': { backgroundColor: '#0d7a5f', color: '#ffffff', padding: '6px 16px', borderRadius: '6px', fontWeight: '600', display: 'inline-block' },
      'INPROCESS': { backgroundColor: '#e3f2fd', color: '#1565c0', padding: '6px 16px', borderRadius: '6px', fontWeight: '600', display: 'inline-block' },
      'RESOLVED': { backgroundColor: '#e8f5e9', color: '#2e7d32', padding: '6px 16px', borderRadius: '6px', fontWeight: '600', display: 'inline-block' },
      'ESCALATED': { backgroundColor: '#fff3e0', color: '#e65100', padding: '6px 16px', borderRadius: '6px', fontWeight: '600', display: 'inline-block' },
      'L1_ESCALATED': { backgroundColor: '#fff3e0', color: '#e65100', padding: '6px 16px', borderRadius: '6px', fontWeight: '600', display: 'inline-block' },
      'L2_ESCALATED': { backgroundColor: '#fff3e0', color: '#e65100', padding: '6px 16px', borderRadius: '6px', fontWeight: '600', display: 'inline-block' },
      'REJECTED': { backgroundColor: '#ffebee', color: '#c62828', padding: '6px 16px', borderRadius: '6px', fontWeight: '600', display: 'inline-block' },
    };
    return styles[statusUpper] || { backgroundColor: '#f5f5f5', color: '#666666', padding: '6px 16px', borderRadius: '6px', fontWeight: '600', display: 'inline-block' };
  };

  // Handle download CSV
  const handleDownloadCSV = () => {
    if (!requests || requests.length === 0) {
      alert("No data available to download");
      return;
    }

    // Filter requests based on search query
    const filteredData = requests.filter((req) => {
      if (!searchQuery) return true;
      const query = searchQuery.toLowerCase();
      return (
        req.id?.toLowerCase().includes(query) ||
        req.type?.toLowerCase().includes(query) ||
        req.status?.toLowerCase().includes(query) ||
        req.principal?.name?.toLowerCase().includes(query)
      );
    });

    if (filteredData.length === 0) {
      alert("No matching data to download");
      return;
    }

    // Prepare data for CSV export
    const csvData = filteredData.map(req => ({
      'Request ID': req.id || 'N/A',
      'Request Type': req.type || 'N/A',
      'Date': req.date || 'N/A',
      'Data Principal Name': req.principal?.name || 'N/A',
      'Data Principal Mobile': req.principal?.mobile || 'N/A',
      'Days Left': req.daysLeft ?? 'N/A',
      'Status': req.status || 'N/A'
    }));

    exportToCSV(csvData, 'grievance_requests');
    console.log("CSV download completed");
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

  // Sort all filtered requests
  const sortedRequests = React.useMemo(() => {
    if (!sortColumn) return filteredRequests;

    const getNestedValue = (obj, path) => {
      return path.split('.').reduce((acc, part) => acc && acc[part], obj);
    };

    return [...filteredRequests].sort((a, b) => {
      // Use rawDate for date column sorting (for proper chronological order)
      const sortKey = sortColumn === 'date' ? 'rawDate' : sortColumn;
      let aValue = getNestedValue(a, sortKey) || '';
      let bValue = getNestedValue(b, sortKey) || '';

      // Handle date comparison
      if (sortKey === 'rawDate') {
        aValue = new Date(aValue).getTime() || 0;
        bValue = new Date(bValue).getTime() || 0;
      }

      if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }, [filteredRequests, sortColumn, sortDirection]);

  // Paginate the sorted requests (client-side pagination)
  const paginatedRequests = React.useMemo(() => {
    const startIndex = currentPage * pageSize;
    const endIndex = startIndex + pageSize;
    return sortedRequests.slice(startIndex, endIndex);
  }, [sortedRequests, currentPage, pageSize]);

  // Update total records and pages based on filtered/sorted data
  React.useEffect(() => {
    setTotalRecords(sortedRequests.length);
    setTotalPages(Math.ceil(sortedRequests.length / pageSize) || 1);
  }, [sortedRequests.length, pageSize]);

  return (
    <>
      <div className="configurePage">
        <div className="emailTemplateWidth">
          <div className="emailTemplate-Heading-Button">
            <div className="emailTemplate-Heading-badge">
              <Text appearance="heading-s" color="primary-grey-100">
                Requests
              </Text>
              <div className="systemConfig-badge">
                <Text appearance="body-xs-bold" color="primary-grey-80">
                  Grievance redressal
                </Text>
              </div>
            </div>
          </div>
          <br></br>

          {/* Stats Cards Section */}
          <div className="container-wrapper">
            <div className="numbers-section">
              <div className="card total">
                <div>
                  <Icon
                    ic={<IcRequest />}
                    size="xtra-large"
                    color="#555555"
                  />
                </div>
                <div style={{ marginLeft: "10px" }}>
                  <Text appearance="heading-xxs" color="primary-grey-100">
                    {totalCount || 0}
                  </Text>
                  <br />
                  <Text appearance="body-xs" color="primary-grey-80">
                    Total
                  </Text>
                </div>
              </div>

              <div className="card inProgress">
                <div>
                  <Icon
                    ic={<IcRequest />}
                    size="xtra-large"
                    color="primary-grey-20"
                  />
                </div>
                <div style={{ marginLeft: "10px" }}>
                  <Text appearance="heading-xxs" color="primary-grey-100">
                    {newCount || 0}
                  </Text>
                  <br />
                  <Text appearance="body-xs" color="primary-grey-80">
                    New
                  </Text>
                </div>
              </div>

              <div className="card inProgress">
                <div>
                  <Icon
                    ic={<IcRequest />}
                    size="xtra-large"
                    color="#1565c0"
                  />
                </div>
                <div style={{ marginLeft: "10px" }}>
                  <Text appearance="heading-xxs" color="primary-grey-100">
                    {inProgressCount || 0}
                  </Text>
                  <br />
                  <Text appearance="body-xs" color="primary-grey-80">
                    In Process
                  </Text>
                </div>
              </div>

              <div className="card success">
                <div>
                  <Icon ic={<IcSuccessColored />} size="xtra-large" />
                </div>
                <div style={{ marginLeft: "10px" }}>
                  <Text appearance="heading-xxs" color="primary-grey-100">
                    {resolvedCount || 0}
                  </Text>
                  <br />
                  <Text appearance="body-xs" color="primary-grey-80">
                    Resolved
                  </Text>
                </div>
              </div>

              <div className="card warning">
                <div>
                  <Icon
                    ic={<IcTimezone />}
                    size="xtra-large"
                    color="feedback_warning_50"
                  />
                </div>
                <div style={{ marginLeft: "10px" }}>
                  <Text appearance="heading-xxs" color="primary-grey-100">
                    {escalatedCount || 0}
                  </Text>
                  <br />
                  <Text appearance="body-xs" color="primary-grey-80">
                    Escalated
                  </Text>
                </div>
              </div>
            </div>
          </div>

          {/* Search, Filter, Download Controls */}
          <div className="breach-table-controls" style={{ marginTop: '20px' }}>
            <div className="breach-search-container">
              <input
                type="text"
                placeholder="Search request ID"
                className="breach-search-input"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
              <svg className="breach-search-icon" width="20" height="20" viewBox="0 0 24 24" fill="none">
                <circle cx="11" cy="11" r="8" stroke="currentColor" strokeWidth="2" />
                <path d="M21 21L16.65 16.65" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </div>
            <div className="breach-table-actions">
              <button 
                className={`breach-icon-button ${filterDrawerOpen ? "active" : ""} ${activeFilterCount > 0 ? "has-filters" : ""}`}
                onClick={() => setFilterDrawerOpen(!filterDrawerOpen)} 
                title="Filter"
              >
                <IcFilter height={20} width={20} />
                {activeFilterCount > 0 && (
                  <span className="filter-badge">{activeFilterCount}</span>
                )}
              </button>
              <button className="breach-icon-button" onClick={handleDownloadCSV} title="Download CSV">
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
                  {/* Status Filter */}
                  <div className="filter-section">
                    <Text appearance="body-s-bold" color="primary-grey-100">
                      Status
                    </Text>
                    <div className="filter-chips">
                      {['NEW', 'INPROCESS', 'RESOLVED', 'L1_ESCALATED', 'L2_ESCALATED'].map((status) => (
                        <button
                          key={status}
                          className={`filter-chip ${filters.status.includes(status) ? 'active' : ''}`}
                          onClick={() => handleStatusFilterChange(status)}
                        >
                          {formatStatus(status)}
                          {filters.status.includes(status) && (
                            <IcClose height={12} width={12} />
                          )}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Request Type Filter */}
                  <div className="filter-section">
                    <Text appearance="body-s-bold" color="primary-grey-100">
                      Request Type
                    </Text>
                    <div className="filter-chips">
                      {uniqueRequestTypes.map((type) => (
                        <button
                          key={type}
                          className={`filter-chip ${filters.requestType.includes(type) ? 'active' : ''}`}
                          onClick={() => handleRequestTypeFilterChange(type)}
                        >
                          {type}
                          {filters.requestType.includes(type) && (
                            <IcClose height={12} width={12} />
                          )}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Date Range Filter */}
                  <div className="filter-section">
                    <Text appearance="body-s-bold" color="primary-grey-100">
                      Date Range
                    </Text>
                    <div style={{ display: 'flex', gap: '10px', flexDirection: 'column' }}>
                      <div>
                        <Text appearance="body-xs" color="primary-grey-80">From</Text>
                        <input
                          type="date"
                          value={filters.dateFrom}
                          onChange={(e) => setFilters(prev => ({ ...prev, dateFrom: e.target.value }))}
                          style={{
                            width: '100%',
                            padding: '8px 12px',
                            border: '1px solid #e0e0e0',
                            borderRadius: '6px',
                            fontSize: '14px',
                            marginTop: '4px'
                          }}
                        />
                      </div>
                      <div>
                        <Text appearance="body-xs" color="primary-grey-80">To</Text>
                        <input
                          type="date"
                          value={filters.dateTo}
                          onChange={(e) => setFilters(prev => ({ ...prev, dateTo: e.target.value }))}
                          style={{
                            width: '100%',
                            padding: '8px 12px',
                            border: '1px solid #e0e0e0',
                            borderRadius: '6px',
                            fontSize: '14px',
                            marginTop: '4px'
                          }}
                        />
                      </div>
                    </div>
                  </div>
                </div>

                {/* Filter Actions */}
                <div className="filter-drawer-footer">
                  <button
                    className="filter-clear-btn"
                    onClick={handleClearFilters}
                    disabled={activeFilterCount === 0}
                  >
                    Clear All
                  </button>
                  <button
                    className="filter-apply-btn"
                    onClick={() => setFilterDrawerOpen(false)}
                  >
                    Apply Filters
                  </button>
                </div>
              </div>
            </>
          )}

          <div>
            <div className="emailTemplate-custom-table-outer-div">
              <table className="emailTempalte-custom-table">
                <thead>
                  <tr>
                    <th style={{ width: "25%", cursor: 'pointer' }} onClick={() => handleSort('id')}>
                      <div className="emailTemplate-table-header-icon">
                        <Text appearance="body-xs-bold" color="primary-grey-80">
                          Request ID
                        </Text>
                        {renderSortIcon('id')}
                      </div>
                    </th>
                    <th style={{ width: "15%", cursor: 'pointer' }} onClick={() => handleSort('type')}>
                      <div className="emailTemplate-table-header-icon">
                        <Text appearance="body-xs-bold" color="primary-grey-80">
                          Request Type
                        </Text>
                        {renderSortIcon('type')}
                      </div>
                    </th>
                    <th style={{ width: "20%", cursor: 'pointer' }} onClick={() => handleSort('date')}>
                      <div className="emailTemplate-table-header-icon">
                        <Text appearance="body-xs-bold" color="primary-grey-80">
                          Received On (date/time)
                        </Text>
                        {renderSortIcon('date')}
                      </div>
                    </th>
                    <th style={{ width: "15%", cursor: 'pointer' }} onClick={() => handleSort('principal.name')}>
                      <div className="emailTemplate-table-header-icon">
                        <Text appearance="body-xs-bold" color="primary-grey-80">
                          Data principal
                        </Text>
                        {renderSortIcon('principal.name')}
                      </div>
                    </th>

                    <th style={{ width: "10%", cursor: 'pointer' }} onClick={() => handleSort('daysLeft')}>
                      <div className="emailTemplate-table-header-icon">
                        <Text appearance="body-xs-bold" color="primary-grey-80">
                          Days left
                        </Text>
                        {renderSortIcon('daysLeft')}
                      </div>
                    </th>

                    <th style={{ width: "15%", cursor: 'pointer' }} onClick={() => handleSort('status')}>
                      <div className="emailTemplate-table-header-icon">
                        <Text appearance="body-xs-bold" color="primary-grey-80">
                          Status
                        </Text>
                      </div>
                    </th>
                  </tr>
                </thead>
                {/* <tbody>
                  {requests.map((req, idx) => (
                    <tr key={idx}>
                      <td>
                        <span
                          style={{ color: "#0066cc", cursor: "pointer" }}
                          onClick={handleClick}
                        >
                          <Text appearance="body-xs-bold" color="black">
                            {req.id}
                          </Text>
                        </span>
                      </td>
                      <td>
                        <Text appearance="body-xs-bold" color="black">
                          {req.type}
                        </Text>
                      </td>
                      <td>
                        <Text appearance="body-xs-bold" color="black">
                          {req.date}
                        </Text>
                      </td>
                      <td>
                        <Text appearance="body-xs-bold" color="black">
                          {req.principal}
                        </Text>
                      </td>
                     
                      <td>
                        <Text appearance="body-xs-bold" color="black">
                          {req.status}
                        </Text>
                      </td>
                    </tr>
                  ))}
                </tbody> */}
                {/* <tbody>
                  {requests.map((req, idx) => (
                    <tr key={idx}>
                      <td>
                        <span style={{ color: "#0066cc", cursor: "pointer" }} onClick={handleClick}>
                          <Text appearance="body-xs-bold" color="black">{req.id}</Text>
                        </span>
                      </td>
                      <td><Text appearance="body-xs-bold" color="black">{req.type}</Text></td>
                      <td><Text appearance="body-xs-bold" color="black">{req.date}</Text></td>
                      <td><Text appearance="body-xs-bold" color="black">{req.principal}</Text></td>
                      <td><Text appearance="body-xs-bold" color="black">{req.status}</Text></td>
                    </tr>
                  ))}
                </tbody> */}
                <tbody>
                  {paginatedRequests.length > 0 ? (
                    paginatedRequests.map((req, idx) => (
                      <tr key={idx} onClick={() => handleClick(req.id)} style={{ cursor: "pointer" }}>
                        <td>

                          <Text appearance="body-xs-bold" color="black">
                            {req.id}
                          </Text>
                        </td>
                        <td>
                          <Text appearance="body-xs-bold" color="black">
                            {req.type}
                          </Text>
                        </td>
                        <td>
                          <Text appearance="body-xs-bold" color="black">
                            {req.date}
                          </Text>
                        </td>
                        <td>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            {visiblePrincipals[req.id] ? (
                              <>
                                <Text appearance="body-xs-bold" color="black">
                                  {req.principal.name} ({req.principal.mobile})
                                </Text>
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation(); // Prevent row click
                                    togglePrincipalVisibility(req.id);
                                  }}
                                  style={{
                                    background: 'none',
                                    border: 'none',
                                    cursor: 'pointer',
                                    padding: '4px',
                                    display: 'flex',
                                    alignItems: 'center'
                                  }}
                                  title="Hide"
                                >
                                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                                    <path d="M3 3l18 18M10.5 10.677a2 2 0 002.823 2.823M7.362 7.561A7.5 7.5 0 0019.5 12a7.5 7.5 0 00-1.5-2.5" strokeWidth="2" strokeLinecap="round" />
                                    <path d="M6.936 6.936C4.325 8.562 2.5 11.5 2.5 12s2.5 5.5 9.5 5.5c1.886 0 3.47-.352 4.832-.936" strokeWidth="2" strokeLinecap="round" />
                                  </svg>
                                </button>
                              </>
                            ) : (
                              <>
                                <Text appearance="body-xs-bold" color="black">
                                  ****
                                </Text>
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation(); // Prevent row click
                                    togglePrincipalVisibility(req.id);
                                  }}
                                  style={{
                                    background: 'none',
                                    border: 'none',
                                    cursor: 'pointer',
                                    padding: '4px',
                                    display: 'flex',
                                    alignItems: 'center'
                                  }}
                                  title="Show"
                                >
                                  <IcVisible height={16} width={16} />
                                </button>
                              </>
                            )}
                          </div>
                        </td>
                        <td>
                          <Text 
                            appearance="body-xs-bold" 
                            color={
                              req.daysLeft === null || req.daysLeft === undefined || isNaN(req.daysLeft) 
                                ? "primary-grey-60" 
                                : req.daysLeft === 0 
                                  ? "feedback_error_50" 
                                  : req.daysLeft <= 3 
                                    ? "feedback_warning_50" 
                                    : "black"
                            }
                          >
                            {req.daysLeft !== null && req.daysLeft !== undefined && !isNaN(req.daysLeft) 
                              ? String(req.daysLeft) 
                              : 'N/A'}
                          </Text>
                        </td>
                        <td>
                          <span style={getStatusBadgeStyle(req.status)}>
                            {formatStatus(req.status)}
                          </span>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="6" style={{ textAlign: "center", padding: "20px" }}>
                        <Text appearance="body-s-bold" color="primary-grey-80">
                          No data present
                        </Text>
                      </td>
                    </tr>
                  )}
                </tbody>


              </table>
            </div>
          </div>

          {/* Pagination Controls */}
          {!loading && (totalRecords > 0 || sortedRequests.length > 0) && (
            <div className="audit-pagination-container">
              <div className="audit-pagination-info">
                <Text appearance="body-xs" color="primary-grey-80">
                  Showing {totalRecords > 0 ? currentPage * pageSize + 1 : 0} to {Math.min(currentPage * pageSize + paginatedRequests.length, totalRecords)} of {totalRecords} records
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
                      <path d="M18 18L8 12L18 6V18Z" fill="currentColor" />
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

                  <span className="audit-page-indicator">
                    <Text appearance="body-xs" color="primary-grey-80">
                      Page {currentPage + 1} of {totalPages || 1}
                    </Text>
                  </span>

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
      </div>
    </>
  );
};

export default Request;
