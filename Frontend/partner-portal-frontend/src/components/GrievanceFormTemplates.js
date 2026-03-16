//Dasboard.js

import { ActionButton, Icon, Image, Text, InputFieldV2 } from "../custom-components";
import "../styles/pageConfiguration.css";
import "../styles/templates.css";
import { useNavigate } from "react-router-dom";
import { useEffect, useState, useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";
import {
  getAllTemplates,
  getConsentsByTemplateId,
  fetchGrievanceTemplates,
} from "../store/actions/CommonAction";

import { IcCode, IcTicketDetails } from "../custom-components/Icon";
import { IcEditPen } from "../custom-components/Icon";
import { IcClose } from "../custom-components/Icon";
import { IcEditPen, IcSort, IcDownload, IcFilter } from "../custom-components/Icon";
import { IcClose } from "../custom-components/Icon";
import { Slide, toast, ToastContainer } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import config from "../utils/config";
import { exportToCSV } from "../utils/csvExport";
import { IcSuccessColored, IcEdit } from "../custom-components/Icon";
import { IcDocument } from "../custom-components/Icon";

const GrievanceFormTemplates = () => {

  const navigate = useNavigate();
  const dispatch = useDispatch();

  const dashboard = new URL("../assets/dashboard.svg", import.meta.url).href;

  const tenantId = useSelector((state) => state.common.tenant_id);
  const businessId = useSelector((state) => state.common.business_id);
  const token = useSelector((state) => state.common.session_token);

  const profile = useSelector((state) => state.common.profile);

  const [openCode, setOpenCode] = useState(false);

  const [file, setFile] = useState("");

  const handleFileChange = (e) => {
    setFile(e.target.value);
  };

  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showUrlModal, setShowUrlModal] = useState(false);
  const [selectedTemplateId, setSelectedTemplateId] = useState('');
  const [searchQuery, setSearchQuery] = useState("");
  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState("asc");

  const [filterDrawerOpen, setFilterDrawerOpen] = useState(false);
  const [filters, setFilters] = useState({
    status: [],
    dateFrom: "",
    dateTo: "",
  });

  const [stats, setStats] = useState({
    total: 0,
    published: 0,
    draft: 0,
  });

  const handleSort = (column) => {
    if (sortColumn === column) {
      setSortDirection(sortDirection === "asc" ? "desc" : "asc");
    } else {
      setSortColumn(column);
      setSortDirection("asc");
    }
  };

  // Show integration guide modal
  const handleShowUrlModal = (grievanceTemplateId) => {
    setSelectedTemplateId(grievanceTemplateId);
    setShowUrlModal(true);
  };

  useEffect(() => {
    const getTemplates = async () => {
      try {

        const data = await fetchGrievanceTemplates(tenantId, businessId, token);
        setTemplates(data);
        console.log("Templates: ", data);

      } catch (err) {
        console.error(err);
      }
    };

    getTemplates();
  }, [tenantId, businessId, token]);

  // Update stats whenever templates change
  useEffect(() => {
    if (templates && templates.length > 0) {
      const published = templates.filter(item => item.status === "PUBLISHED").length;
      const draft = templates.filter(item => item.status === "DRAFT").length;

      setStats({
        total: templates.length,
        published,
        draft,
      });
    }
  }, [templates]);

  // Helper function to get status badge class
  const getStatusClass = (status) => {
    switch (status) {
      case "PUBLISHED":
        return "status-published";
      case "DRAFT":
        return "status-draft";
      default:
        return "";
    }
  };

  // Handle CSV download
  const handleDownloadCSV = () => {
    if (!templates || templates.length === 0) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"No data available to download"}
          />
        ),
        { icon: false }
      );
      return;
    }

    const filteredData = templates.filter((template) => {
      if (!searchQuery) return true;
      const query = searchQuery.toLowerCase();
      return (
        template.grievanceTemplateName?.toLowerCase().includes(query) ||
        template.grievanceTemplateId?.toLowerCase().includes(query) ||
        template.status?.toLowerCase().includes(query)
      );
    });

    if (filteredData.length === 0) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"No matching data to download"}
          />
        ),
        { icon: false }
      );
      return;
    }

    const csvData = filteredData.map(template => ({
      'Form Name': template.grievanceTemplateName || 'N/A',
      'Template ID': template.grievanceTemplateId || 'N/A',
      'Last Updated': template.updatedAt ? new Date(template.updatedAt).toLocaleDateString() : 'N/A',
      'Status': template.status || 'N/A',
      'Business ID': template.businessId || 'N/A'
    }));

    exportToCSV(csvData, 'grievance_form_templates');
    console.log("CSV download completed");
  };

  // Handle filter changes
  const handleStatusFilterChange = (status) => {
    setFilters((prev) => ({
      ...prev,
      status: prev.status.includes(status)
        ? prev.status.filter((s) => s !== status)
        : [...prev.status, status],
    }));
  };

  const handleClearFilters = () => {
    setFilters({
      status: [],
      dateFrom: "",
      dateTo: "",
    });
  };

  const handleApplyFilters = () => {
    setFilterDrawerOpen(false);
  };

  // Filter templates based on search query and filters
  const filteredTemplates = templates.filter((item) => {
    // Search query filter
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      const matchesSearch =
        item.grievanceTemplateName?.toLowerCase().includes(query) ||
        item.grievanceTemplateId?.toLowerCase().includes(query) ||
        item.status?.toLowerCase().includes(query);
      if (!matchesSearch) return false;
    }

    // Status filter
    if (filters.status.length > 0) {
      const itemStatus = item.status?.toUpperCase() || "";
      const matchesStatus = filters.status.some(
        (status) => status.toUpperCase() === itemStatus
      );
      if (!matchesStatus) return false;
    }

    // Date range filter
    if (filters.dateFrom || filters.dateTo) {
      const itemDate = new Date(item.updatedAt || item.createdAt);
      if (filters.dateFrom && itemDate < new Date(filters.dateFrom)) {
        return false;
      }
      if (filters.dateTo && itemDate > new Date(filters.dateTo)) {
        return false;
      }
    }

    return true;
  });

  const sortedTemplates = useMemo(() => {
    if (!sortColumn) return filteredTemplates;

    return [...filteredTemplates].sort((a, b) => {
      let aValue = a[sortColumn] || '';
      let bValue = b[sortColumn] || '';

      if (sortColumn === 'updatedAt') {
        aValue = new Date(aValue);
        bValue = new Date(bValue);
      }

      if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }, [filteredTemplates, sortColumn, sortDirection]);

  // Get active filter count
  const activeFilterCount =
    filters.status.length + (filters.dateFrom ? 1 : 0) + (filters.dateTo ? 1 : 0);

  const handleDownload = () => {
    if (!file) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please select a file before downloading!"}
          />
        ),
        { icon: false }
      );
      return;
    }

    const fileMap = {
      JS: "/IntegrationFile.js",

      // iOS: "/assets/ios-sdk.zip",

      // Android: "/assets/android-sdk.zip",
    };

    const filePath = fileMap[file];

    if (filePath) {
      const link = document.createElement("a");

      link.href = filePath;

      link.download = filePath.split("/").pop();

      document.body.appendChild(link);

      link.click();

      document.body.removeChild(link);
    }
  };

  const handleClick = () => {
    navigate("/createGrievanceForm");
  };

  const handleOpenCL = (tempId) => {
    navigate(`/grievacneLogs?templateId=${tempId}`);
  };

  const closeCode = () => {
    setOpenCode(false);
  };



  return (
    <div className="configurePage">
      {/* Header Section */}
      <div className="template-header-section">
        <div className="template-title-section">
          {/* <h1 className="template-main-title">Grievance Form Templates</h1>
          <div className="template-consent-badge">
            <Text appearance="body-xs-bold" color="primary-grey-80">
              Grievance Redressal
            </Text>
          </div> */}
          <div style={{ display: 'flex', flexDirection: 'row', gap: '10px' }}>
            <Text appearance="heading-s" color="primary-grey-100">Grievance Form Templates</Text>
            <div className="dataProtectionOfficer-badge" style={{ marginTop: '5px' }}>
              <Text appearance="body-xs-bold" color="primary-grey-80">Grievance Redressal</Text>
            </div>
          </div>
        </div>
        <div className="template-button-group">
          <ActionButton
            kind="primary"
            size="medium"
            state="normal"
            label="Create grievance form template"
            onClick={handleClick}
          />
        </div>
      </div>

      {templates.length === 0 ? (
        <div className="configure-template-page">
          <div className="template-img">
            <Image src={dashboard}></Image>
          </div>

          <Text color="primary-grey-80" style={{ marginTop: "0.5rem" }}>
            You have not added any templates yet. <br></br>
            Create a grievance form template to get started
          </Text>

          <div className="template-btn">
            <ActionButton
              kind="secondary"
              size="medium"
              state="normal"
              label="Create grievance form template"
              onClick={handleClick}
            />
          </div>
        </div>
      ) : (
        <>
          {/* Stats Section */}
          {/* <div className="template-stats-cards">
            <div className="template-stat-card template-stat-total">
              <div className="template-stat-icon template-stat-icon-total">
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none">
                  <rect x="3" y="3" width="18" height="18" rx="2" stroke="white" strokeWidth="2" />
                  <path d="M9 9h6M9 12h6M9 15h4" stroke="white" strokeWidth="2" strokeLinecap="round" />
                </svg>
              </div>
              <div className="template-stat-content">
                <div className="template-stat-number">{stats.total}</div>
                <div className="template-stat-label">Total Templates</div>
              </div>
            </div>

            <div className="template-stat-card template-stat-published">
              <div className="template-stat-icon template-stat-icon-published">
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="10" stroke="white" strokeWidth="2" />
                  <path d="M8 12L11 15L16 9" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </div>
              <div className="template-stat-content">
                <div className="template-stat-number">{stats.published}</div>
                <div className="template-stat-label">Published</div>
              </div>
            </div>

            <div className="template-stat-card template-stat-draft">
              <div className="template-stat-icon template-stat-icon-draft">
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none">
                  <path d="M9 12h6M9 16h6M9 8h6" stroke="white" strokeWidth="2" strokeLinecap="round" />
                  <rect x="3" y="3" width="18" height="18" rx="2" stroke="white" strokeWidth="2" fill="none" />
                </svg>
              </div>
              <div className="template-stat-content">
                <div className="template-stat-number">{stats.draft}</div>
                <div className="template-stat-label">Draft</div>
              </div>
            </div>
          </div> */}

          <div className="template-stats-cards">
            <div className="template-stat-card template-stat-total">
              <div className="icon-wrapper">
                <IcDocument size="xl" color="#05b5a3" />
              </div>
              <div className="template-stat-content">
                <Text appearance="heading-xs" color="primary-grey-100">{stats.total}</Text>
                <Text appearance="body-xs" color="primary-grey-80">Total</Text>
              </div>
            </div>

            <div className="template-stat-card template-stat-published">
              <div className="icon-wrapper">
                <IcSuccessColored size="xl" color="#25AB21" />
              </div>
              <div className="template-stat-content">
                <Text appearance="heading-xs" color="primary-grey-100">{stats.published}</Text>
                <Text appearance="body-xs" color="primary-grey-80">Active</Text>
              </div>
            </div>

            <div className="template-stat-card template-stat-draft">

              <div className="icon-wrapper">
                <IcEdit size="xl" color="#F06D0F" />
              </div>
              <div className="template-stat-content">
                <Text appearance="heading-xs" color="primary-grey-100">{stats.draft}</Text>
                <Text appearance="body-xs" color="primary-grey-80">Draft</Text>
              </div>
            </div>
          </div>

          {/* Table Controls */}
          <div className="template-table-controls">
            <div className="template-search-container">
              <input
                type="text"
                placeholder="Search"
                className="template-search-input"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
              <svg className="template-search-icon" width="20" height="20" viewBox="0 0 24 24" fill="none">
                <circle cx="11" cy="11" r="8" stroke="currentColor" strokeWidth="2" />
                <path d="M21 21L16.65 16.65" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </div>
            <div className="template-table-actions">
              <button
                className={`template-icon-button ${filterDrawerOpen ? "active" : ""} ${activeFilterCount > 0 ? "has-filters" : ""}`}
                onClick={() => setFilterDrawerOpen(!filterDrawerOpen)}
                title="Filter"
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
                  {/* Search within filters */}
                  <div className="filter-section">
                    <InputFieldV2
                      placeholder="Search template name or ID"
                      size="medium"
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                    />
                  </div>

                  {/* Status Filter */}
                  <div className="filter-section">
                    <Text appearance="body-s-bold" color="primary-grey-100">
                      Status
                    </Text>
                    <div className="filter-chips">
                      {["PUBLISHED", "DRAFT"].map((status) => (
                        <button
                          key={status}
                          className={`filter-chip ${filters.status.includes(status) ? "active" : ""}`}
                          onClick={() => handleStatusFilterChange(status)}
                        >
                          {status === "PUBLISHED" ? "Published" : "Draft"}
                          {filters.status.includes(status) && (
                            <IcClose height={16} width={16} />
                          )}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Created Date Range */}
                  <div className="filter-section">
                    <Text appearance="body-s-bold" color="primary-grey-100">
                      Updated date range
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
                  <th>
                    <div className="header-with-icon" onClick={() => handleSort('grievanceTemplateName')} style={{ cursor: 'pointer' }}>
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Form name
                      </Text>
                      <Icon ic={<IcSort />} color="primary_60" size="small" />
                    </div>
                  </th>
                  <th>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Template ID
                      </Text>
                    </div>
                  </th>
                  <th>
                    <div className="header-with-icon" onClick={() => handleSort('version')} style={{ cursor: 'pointer' }}>
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Version
                      </Text>
                      <Icon ic={<IcSort />} color="primary_60" size="small" />
                    </div>
                  </th>
                  <th>
                    <div className="header-with-icon" onClick={() => handleSort('updatedAt')} style={{ cursor: 'pointer' }}>
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Last updated
                      </Text>
                      <Icon ic={<IcSort />} color="primary_60" size="small" />
                    </div>
                  </th>
                  <th>
                    <div className="header-with-icon" onClick={() => handleSort('status')} style={{ cursor: 'pointer' }}>
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Status
                      </Text>
                      <Icon ic={<IcSort />} color="primary_60" size="small" />
                    </div>
                  </th>
                  <th>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Actions
                      </Text>
                    </div>
                  </th>
                </tr>
              </thead>
              <tbody>
                {sortedTemplates.length === 0 ? (
                  <tr>
                    <td colSpan="6" style={{ textAlign: 'center', padding: '40px' }}>
                      <Text appearance="body-m" color="primary-grey-80">
                        {searchQuery ? `No results found for "${searchQuery}"` : "No templates found."}
                      </Text>
                    </td>
                  </tr>
                ) : (
                  sortedTemplates.map((item, index) => {
                    return (
                      <tr key={index}>
                        <td>
                          <Text appearance="body-xs" color="black">
                            {item.grievanceTemplateName}
                          </Text>
                        </td>
                        <td>
                          <Text appearance="body-xs" color="black">
                            {item.grievanceTemplateId}
                          </Text>
                        </td>
                        <td>
                          <Text appearance="body-xs" color="black">
                            {item.version || '1.0'}
                          </Text>
                        </td>
                        <td>
                          <Text appearance="body-xs" color="black">
                            {new Date(item.createdAt).toLocaleDateString()}
                          </Text>
                        </td>
                        <td>
                          <span className={`status-badge ${getStatusClass(item.status)}`}>
                            {item.status}
                          </span>
                        </td>
                        <td>
                          <div className="template-actions">
                            <button
                              className="icon-btn"
                              onClick={() => handleShowUrlModal(item.grievanceTemplateId)}
                              title="Integration URL"
                            >
                              <Icon ic={<IcCode />} size="medium" color="primary_grey_80" />
                            </button>
                            <button
                              className="icon-btn"
                              onClick={() => handleOpenCL(item.grievanceTemplateId)}
                              title="Grievance Logs"
                            >
                              <Icon ic={<IcTicketDetails />} size="medium" color="primary_grey_80" />
                            </button>
                            <button
                              className="icon-btn"
                              onClick={() => navigate(`/createGrievanceForm?templateId=${item.grievanceTemplateId}&version=${item.version || 1}`)}
                              title="Edit Template"
                            >
                              <Icon ic={<IcEditPen />} size="medium" color="primary_grey_80" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </>
      )}

      {openCode && (
        <div className="modal-outer-container">
          <div className="modal-container">
            <div className="modal-close-btn-container">
              <ActionButton
                onClick={closeCode}
                icon={<IcClose />}
                kind="tertiary"
                size="small"
              ></ActionButton>
            </div>

            <div
              style={{
                display: "flex",

                gap: "5px",

                flexDirection: "column",
              }}
            >
              <div
                style={{
                  borderRadius: "50%",

                  backgroundColor: "#E7EBF8",

                  width: "40px",

                  height: "40px",

                  display: "flex",

                  justifyContent: "center",

                  alignItems: "center",

                  marginLeft: "10px",
                }}
              >
                <Icon
                  ic={<IcCode></IcCode>}
                  size="large"
                  color="primary_50"
                ></Icon>
              </div>

              <div style={{ marginLeft: "12px" }}>
                <Text appearance="heading-xs" color="primary-grey-100">
                  Embed Modal design in your code
                </Text>
              </div>

              <div style={{ marginLeft: "12px" }}>
                <Text appearance="body-s" color="primary-grey-80">
                  Download the script file in the desired integration format
                  and add it to your file.
                </Text>
              </div>

              <div className="dropdown-group" style={{ marginTop: "10px" }}>
                <label>Select integration type (Required)</label>

                <select
                  value={file}
                  onChange={handleFileChange}
                  id="grievance-type"
                >
                  <option value="" disabled>
                    Select
                  </option>

                  <option value="JS">Java Script React JS</option>

                  {/* <option value="iOS">SDK for iOS</option>

                  <option value="Android">SDK for Android</option> */}
                </select>
              </div>
            </div>

            <br></br>

            <br></br>

            <div className="modal-add-btn-container-tem">
              <ActionButton label="I'll do it later" kind="secondary"></ActionButton>

              <ActionButton
                label="Download file"
                onClick={handleDownload}
              ></ActionButton>
            </div>
          </div>
        </div>
      )}

      {/* Integration Guide Modal */}
      {showUrlModal && (
        <div className="modal-outer-container" onClick={() => setShowUrlModal(false)}>
          <div className="url-modal-container" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '700px' }}>
            <div className="url-modal-header">
              <div className="url-modal-title-section">
                <div className="url-modal-icon">
                  <Icon ic={<IcCode />} size="large" color="primary_50" />
                </div>
                <div>
                  <Text appearance="heading-xs" color="primary-grey-100">
                    Integration URL Guide
                  </Text>
                  <Text appearance="body-xs" color="primary-grey-80">
                    Follow these steps to create your grievance form integration URL
                  </Text>
                </div>
              </div>
              <ActionButton
                onClick={() => setShowUrlModal(false)}
                icon={<IcClose />}
                kind="tertiary"
                size="small"
              />
            </div>

            <div className="url-modal-body" style={{ padding: '24px', maxHeight: '70vh', overflowY: 'auto' }}>
              {/* Step 1: Get Secure Code */}
              <div style={{ marginBottom: '24px' }}>
                <Text appearance="body-s-bold" color="primary-grey-100" style={{ display: 'block', marginBottom: '10px' }}>
                  Step 1: Get Secure Code from API
                  </Text>
                <Text appearance="body-xs" color="primary-grey-80" style={{ display: 'block', lineHeight: '1.6', marginBottom: '12px' }}>
                  Call the secure code generation API to obtain a unique secure code for authentication.
                </Text>
                <div style={{ 
                  backgroundColor: '#F9FAFB', 
                  padding: '12px', 
                  borderRadius: '6px',
                  border: '1px solid #E5E7EB',
                  fontFamily: 'monospace',
                  fontSize: '12px',
                  color: '#374151'
                }}>
                  API Endpoint: /secureCode/create
                </div>
              </div>

              {/* Step 2: Create JSON Object */}
              <div style={{ marginBottom: '24px' }}>
                <Text appearance="body-s-bold" color="primary-grey-100" style={{ display: 'block', marginBottom: '10px' }}>
                  Step 2: Create JSON with Required Parameters
                </Text>
                <Text appearance="body-xs" color="primary-grey-80" style={{ display: 'block', lineHeight: '1.6', marginBottom: '12px' }}>
                  Build a JSON object with your tenant ID, business ID, template ID, and the secure code from Step 1:
                </Text>
                <div style={{ 
                  backgroundColor: '#F9FAFB', 
                  padding: '12px', 
                  borderRadius: '6px',
                  border: '1px solid #E5E7EB',
                  fontFamily: 'monospace',
                  fontSize: '12px',
                  overflowX: 'auto'
                }}>
                  <pre style={{ margin: 0, color: '#374151' }}>
{`{
  "tenantId": "your-tenant-id",
  "businessId": "your-business-id",
  "grievanceTemplateId": "${selectedTemplateId}",
  "secureCode": "code-from-api"
}`}
                  </pre>
              </div>
            </div>

              {/* Step 3: Convert to Base64 */}
              <div style={{ marginBottom: '24px' }}>
                <Text appearance="body-s-bold" color="primary-grey-100" style={{ display: 'block', marginBottom: '10px' }}>
                  Step 3: Encode JSON to Base64
                </Text>
                <Text appearance="body-xs" color="primary-grey-80" style={{ display: 'block', lineHeight: '1.6', marginBottom: '12px' }}>
                  Convert the JSON string to base64 encoding:
                </Text>
                <div style={{ 
                  backgroundColor: '#F9FAFB', 
                  padding: '12px', 
                  borderRadius: '6px',
                  border: '1px solid #E5E7EB',
                  fontFamily: 'monospace',
                  fontSize: '11px',
                  overflowX: 'auto',
                  color: '#374151'
                }}>
                  {`const jsonString = JSON.stringify(params);
const base64Data = btoa(jsonString);`}
                </div>
              </div>

              {/* Step 4: Build Final URL */}
              <div style={{ marginBottom: '0' }}>
                <Text appearance="body-s-bold" color="primary-grey-100" style={{ display: 'block', marginBottom: '10px' }}>
                  Step 4: Build the Final Integration URL
                </Text>
                <Text appearance="body-xs" color="primary-grey-80" style={{ display: 'block', lineHeight: '1.6', marginBottom: '12px' }}>
                  Append the base64-encoded data to the integration base URL:
                </Text>
                <div style={{ 
                  backgroundColor: '#F9FAFB', 
                  padding: '12px', 
                  borderRadius: '6px',
                  border: '1px solid #E5E7EB',
                  fontFamily: 'monospace',
                  fontSize: '11px',
                  wordBreak: 'break-all',
                  color: '#374151',
                  lineHeight: '1.6'
                }}>
                  {config.INTEGRATION_BASE_URL}?data=eyJ0ZW5hbnR...base64EncodedData
                </div>
              </div>
            </div>

            <div className="url-modal-footer" style={{ padding: '16px 20px', borderTop: '1px solid #E5E7EB' }}>
              <ActionButton
                label="Close"
                kind="primary"
                onClick={() => setShowUrlModal(false)}
              />
            </div>
          </div>
        </div>
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
export default GrievanceFormTemplates;
