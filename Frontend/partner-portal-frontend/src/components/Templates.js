// Templates.js
import React, { useState, useEffect, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { getAllTemplates } from "../store/actions/CommonAction";
import { ActionButton, Icon, Image, Text, InputFieldV2 } from "../custom-components";
import { IcSuccessColored, IcEdit, IcDocument, IcCode, IcTicketDetails, IcEditPen, IcTime, IcSort, IcDownload, IcFilter, IcClose } from "../custom-components/Icon";

import { toast } from "react-toastify";
import CustomToast from "./CustomToastContainer"; // ✅ update path if different

import integrationFileContentST from "bundle-text:../assets/ST/IntegrationFile.txt";
import integrationFileContentSIT from "bundle-text:../assets/SIT/IntegrationFile.txt";

import { exportToCSV } from "../utils/csvExport";

// ✅ React Native Android SDK tgz URL (place file in src/assets/sdk/)
const rnAndroidSdkTgzUrl = new URL(
  "../assets/sdk/jcms-preference-center-1.0.0.tgz",
  import.meta.url
).href;

// ✅ Android Native SDK URL (place file in src/assets/sdk/)
const AndroidSdkUrl = new URL(
  "../assets/sdk/jcms-preference-center-1.0.0.tgz",
  import.meta.url
).href;

// ✅ Helper: Force download by Blob (works for .tgz reliably)
const downloadByUrl = async (url, fileName) => {
  const res = await fetch(url);
  if (!res.ok) throw new Error("Unable to download file");

  const blob = await res.blob();
  const blobUrl = window.URL.createObjectURL(blob);

  const a = document.createElement("a");
  a.href = blobUrl;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  a.remove();

  window.URL.revokeObjectURL(blobUrl);
};

const Templates = () => {
  const navigate = useNavigate();
  const environment = process.env.REACT_APP_ENVIRONMENT;
  const dispatch = useDispatch();

  const dashboard = new URL("../assets/dashboard.svg", import.meta.url).href;

  const tenantId = useSelector((state) => state.common.tenant_id);
  const profile = useSelector((state) => state.common.profile);
  const businessId = useSelector((state) => state.common.business_id);

  const [openCode, setOpenCode] = useState(false);
  const [file, setFile] = useState("JS");
  const [searchQuery, setSearchQuery] = useState("");
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
    inactive: 0,
  });

  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState("asc");

  const searchList = profile?.searchList || [];

  const filteredTemplates = searchList.filter((item) => {
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      const matchesSearch =
        item.templateName?.toLowerCase().includes(query) ||
        item.templateId?.toLowerCase().includes(query) ||
        item.status?.toLowerCase().includes(query);
      if (!matchesSearch) return false;
    }

    if (filters.status.length > 0) {
      const itemStatus = item.status?.toUpperCase() || "";
      const matchesStatus = filters.status.some(
        (status) => status.toUpperCase() === itemStatus
      );
      if (!matchesStatus) return false;
    }

    if (filters.dateFrom || filters.dateTo) {
      const itemDate = new Date(item.createdAt);
      if (filters.dateFrom && itemDate < new Date(filters.dateFrom)) {
        return false;
      }
      if (filters.dateTo && itemDate > new Date(filters.dateTo)) {
        return false;
      }
    }

    return true;
  });

  const handleFileChange = (e) => {
    setFile(e.target.value);
  };

  useEffect(() => {
    if (tenantId && businessId) {
      dispatch(getAllTemplates(tenantId, businessId));
    }
  }, [dispatch, tenantId, businessId]);

  useEffect(() => {
    if (searchList && searchList.length > 0) {
      const published = searchList.filter((item) => item.status === "PUBLISHED").length;
      const draft = searchList.filter((item) => item.status === "DRAFT").length;
      const inactive = searchList.filter((item) => item.status === "INACTIVE").length;

      setStats({
        total: searchList.length,
        published,
        draft,
        inactive,
      });
    } else {
      setStats({ total: 0, published: 0, draft: 0, inactive: 0 });
    }
  }, [searchList]);

  // ✅ UPDATED: handleDownload supports JS + React Native Android SDK (.tgz)
  const handleDownload = async () => {
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

    // ---- JS / React JS download (existing) ----
    if (file === "JS") {
      try {
        const text =
          environment === "NONPROD"
            ? integrationFileContentSIT
            : integrationFileContentST;

        const blob = new Blob([text], { type: "application/javascript" });
        const url = URL.createObjectURL(blob);

        const link = document.createElement("a");
        link.href = url;
        link.download = "IntegrationFile.js";
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        URL.revokeObjectURL(url);
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
      return;
    }

    // ---- ✅ React Native Android SDK download (.tgz) ----
    if (file === "RN_ANDROID") {
      try {
        await downloadByUrl(rnAndroidSdkTgzUrl, "jcms-preference-center-1.0.0.tgz");
      } catch (err) {
        console.error("SDK download failed:", err);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={`Failed to download SDK: ${err.message}`}
            />
          ),
          { icon: false }
        );
      }
      return;
    }

    // ---- ✅ Android Native SDK download ----
    if (file === "ANDROID_NATIVE") {
      try {
        await downloadByUrl(AndroidSdkUrl, "jcms-preference-center-1.0.0.tgz");
      } catch (err) {
        console.error("SDK download failed:", err);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={`Failed to download SDK: ${err.message}`}
            />
          ),
          { icon: false }
        );
      }
      return;
    }

    // fallback
    toast.error(
      (props) => (
        <CustomToast {...props} type="error" message={"File type not yet supported"} />
      ),
      { icon: false }
    );
  };

  const handleClick = () => {
    navigate("/createConsent");
  };

  const handleOpenCL = (tempId) => {
    navigate(`/consentLogs?templateId=${tempId}`);
  };

  const handleOpenPendingRequests = (tempId) => {
    navigate(`/pendingRequests?templateId=${tempId}`);
  };

  const closeCode = () => {
    setOpenCode(false);
  };

  const getStatusClass = (status) => {
    switch (status) {
      case "PUBLISHED":
        return "status-published";
      case "DRAFT":
        return "status-draft";
      case "INACTIVE":
        return "status-inactive";
      default:
        return "";
    }
  };

  const handleSort = (column) => {
    if (sortColumn === column) {
      setSortDirection(sortDirection === "asc" ? "desc" : "asc");
    } else {
      setSortColumn(column);
      setSortDirection("asc");
    }
  };

  const renderSortIcon = () => {
    return <Icon ic={<IcSort />} size="small" color="black" />;
  };

  const sortedTemplates = useMemo(() => {
    if (!sortColumn) return filteredTemplates;

    return [...filteredTemplates].sort((a, b) => {
      const aValue = a[sortColumn] || "";
      const bValue = b[sortColumn] || "";

      if (aValue < bValue) return sortDirection === "asc" ? -1 : 1;
      if (aValue > bValue) return sortDirection === "asc" ? 1 : -1;
      return 0;
    });
  }, [filteredTemplates, sortColumn, sortDirection]);

  const handleDownloadCSV = () => {
    if (!sortedTemplates || sortedTemplates.length === 0) {
      alert("No data available to download");
      return;
    }

    const filteredData = sortedTemplates.filter((template) => {
      if (!searchQuery) return true;
      const query = searchQuery.toLowerCase();
      return (
        template.templateName?.toLowerCase().includes(query) ||
        template.templateId?.toLowerCase().includes(query) ||
        template.status?.toLowerCase().includes(query)
      );
    });

    if (filteredData.length === 0) {
      alert("No matching data to download");
      return;
    }

    const csvData = filteredData.map((template) => {
      const purposesCount = template.preferences?.reduce(
        (acc, pref) => acc + (pref.purposeIds?.length || 0),
        0
      );

      const activitiesCount = template.preferences?.reduce(
        (acc, pref) => acc + (pref.processorActivityIds?.length || 1),
        0
      );

      return {
        "Template Name": template.templateName || "N/A",
        "Created Date": template.createdAt
          ? new Date(template.createdAt).toLocaleDateString()
          : "N/A",
        Purposes: purposesCount || 0,
        Activities: activitiesCount || 0,
        Version: template.version || "N/A",
        "Template ID": template.templateId || "N/A",
        Status: template.status || "N/A",
        "Updated At": template.updatedAt
          ? new Date(template.updatedAt).toLocaleDateString()
          : "N/A",
        "Business ID": template.businessId || "N/A",
      };
    });

    exportToCSV(csvData, "consent_templates");
  };

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

  const activeFilterCount =
    filters.status.length + (filters.dateFrom ? 1 : 0) + (filters.dateTo ? 1 : 0);

  return (
    <div className="configurePage">
      {/* Header Section */}
      <div className="template-header-section">
        <div className="template-title-section">
          <div style={{ display: "flex", flexDirection: "row", gap: "10px" }}>
            <Text appearance="heading-s" color="primary-grey-100">
              Templates
            </Text>
            <div className="dataProtectionOfficer-badge" style={{ marginTop: "5px" }}>
              <Text appearance="body-xs-bold" color="primary-grey-80">
                Consent
              </Text>
            </div>
          </div>
        </div>
        <div className="template-button-group">
          <ActionButton
            kind="primary"
            size="medium"
            state="normal"
            label="Create consent template"
            onClick={handleClick}
          />
        </div>
      </div>

      {searchList.length === 0 ? (
        <div className="configure-template-page">
          <div className="template-img">
            <Image src={dashboard} />
          </div>

          <Text color="primary-grey-80" style={{ marginTop: "0.5rem" }}>
            You have not added any templates yet. <br />
            Create a consent template to get started
          </Text>

          <div className="template-btn">
            <ActionButton
              kind="secondary"
              size="medium"
              state="normal"
              label="Create consent template"
              onClick={handleClick}
            />
          </div>
        </div>
      ) : (
        <>
          {/* Stats Section */}
          <div className="template-stats-cards">
            <div className="template-stat-card template-stat-total">
              <div className="icon-wrapper">
                <IcDocument size="xl" color="#05b5a3" />
              </div>
              <div className="template-stat-content">
                <Text appearance="heading-xs" color="primary-grey-100">
                  {stats.total}
                </Text>
                <Text appearance="body-xs" color="primary-grey-80">
                  Total
                </Text>
              </div>
            </div>

            <div className="template-stat-card template-stat-published">
              <div className="icon-wrapper">
                <IcSuccessColored size="xl" color="#25AB21" />
              </div>
              <div className="template-stat-content">
                <Text appearance="heading-xs" color="primary-grey-100">
                  {stats.published}
                </Text>
                <Text appearance="body-xs" color="primary-grey-80">
                  Active
                </Text>
              </div>
            </div>

            <div className="template-stat-card template-stat-draft">
              <div className="icon-wrapper">
                <IcEdit size="xl" color="#F06D0F" />
              </div>
              <div className="template-stat-content">
                <Text appearance="heading-xs" color="primary-grey-100">
                  {stats.draft}
                </Text>
                <Text appearance="body-xs" color="primary-grey-80">
                  Draft
                </Text>
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
              <svg
                className="template-search-icon"
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
              >
                <circle cx="11" cy="11" r="8" stroke="currentColor" strokeWidth="2" />
                <path
                  d="M21 21L16.65 16.65"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                />
              </svg>
            </div>

            <div className="template-table-actions">
              <button
                className={`template-icon-button ${filterDrawerOpen ? "active" : ""} ${activeFilterCount > 0 ? "has-filters" : ""
                  }`}
                onClick={() => setFilterDrawerOpen(!filterDrawerOpen)}
                title="Filter"
              >
                <IcFilter height={20} width={20} />
                {activeFilterCount > 0 && <span className="filter-badge">{activeFilterCount}</span>}
              </button>
              <button className="template-icon-button" onClick={handleDownloadCSV} title="Download CSV">
                <IcDownload height={20} width={20} />
              </button>
            </div>
          </div>

          {/* Filter Drawer */}
          {filterDrawerOpen && (
            <>
              <div className="filter-drawer-overlay" onClick={() => setFilterDrawerOpen(false)} />
              <div className="filter-drawer">
                <div className="filter-drawer-header">
                  <div className="filter-drawer-title">
                    <IcFilter height={24} width={24} />
                    <Text appearance="heading-xs" color="primary-grey-100">
                      Filters
                    </Text>
                  </div>
                  <button className="filter-drawer-close" onClick={() => setFilterDrawerOpen(false)}>
                    <IcClose height={24} width={24} />
                  </button>
                </div>

                <div className="filter-drawer-content">
                  <div className="filter-section">
                    <InputFieldV2
                      placeholder="Search data item, type and purpose"
                      size="medium"
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                    />
                  </div>

                  <div className="filter-section">
                    <Text appearance="body-s-bold" color="primary-grey-100">
                      Status
                    </Text>
                    <div className="filter-chips">
                      {["PUBLISHED", "DRAFT", "INACTIVE"].map((status) => (
                        <button
                          key={status}
                          className={`filter-chip ${filters.status.includes(status) ? "active" : ""}`}
                          onClick={() => handleStatusFilterChange(status)}
                        >
                          {status === "PUBLISHED"
                            ? "Active"
                            : status === "DRAFT"
                              ? "Draft"
                              : "Inactive"}
                          {filters.status.includes(status) && <IcClose height={16} width={16} />}
                        </button>
                      ))}
                    </div>
                  </div>

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
                          max={filters.dateTo || new Date().toISOString().split('T')[0]}
                          onChange={(e) => setFilters({ ...filters, dateFrom: e.target.value })}
                        />
                      </div>
                      <div className="filter-date-input">
                        <label>To</label>
                        <input
                          type="date"
                          value={filters.dateTo}
                          min={filters.dateFrom || ''}
                          max={new Date().toISOString().split('T')[0]}
                          onChange={(e) => setFilters({ ...filters, dateTo: e.target.value })}
                        />
                      </div>
                    </div>
                  </div>
                </div>

                <div className="filter-drawer-footer">
                  <ActionButton kind="secondary" size="medium" label="Clear all" onClick={handleClearFilters} />
                  <ActionButton kind="primary" size="medium" label="Apply" onClick={handleApplyFilters} />
                </div>
              </div>
            </>
          )}

          {/* Table Section */}
          <div className="template-table-wrapper">
            <table className="template-table">
              <thead>
                <tr>
                  <th onClick={() => handleSort("templateName")} style={{ cursor: "pointer" }}>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Template name
                      </Text>
                      {renderSortIcon("templateName")}
                    </div>
                  </th>

                  <th onClick={() => handleSort("createdAt")} style={{ cursor: "pointer" }}>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Last updated
                      </Text>
                      {renderSortIcon("createdAt")}
                    </div>
                  </th>

                  <th>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Purposes
                      </Text>
                    </div>
                  </th>

                  <th>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Activities
                      </Text>
                    </div>
                  </th>

                  <th onClick={() => handleSort("version")} style={{ cursor: "pointer" }}>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Version
                      </Text>
                      {renderSortIcon("version")}
                    </div>
                  </th>

                  <th onClick={() => handleSort("templateId")} style={{ cursor: "pointer" }}>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Template ID
                      </Text>
                      {renderSortIcon("templateId")}
                    </div>
                  </th>

                  <th onClick={() => handleSort("status")} style={{ cursor: "pointer" }}>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Status
                      </Text>
                      {renderSortIcon("status")}
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
                    <td colSpan="8" style={{ textAlign: "center", padding: "40px" }}>
                      <Text appearance="body-m" color="primary-grey-80">
                        {searchQuery ? `No results found for "${searchQuery}"` : "No templates found."}
                      </Text>
                    </td>
                  </tr>
                ) : (
                  sortedTemplates.map((item, index) => {
                    const purposesCount = item.preferences?.reduce(
                      (acc, pref) => acc + (pref.purposeIds?.length || 0),
                      0
                    );

                    const activitiesCount = item.preferences?.reduce(
                      (acc, pref) => acc + (pref.processorActivityIds?.length || 1),
                      0
                    );

                    return (
                      <tr key={index}>
                        <td>
                          <Text appearance="body-xs" color="black">
                            {item.templateName}
                          </Text>
                        </td>

                        <td>
                          <Text appearance="body-xs" color="black">
                            {new Date(item.createdAt).toLocaleDateString()}
                          </Text>
                        </td>

                        <td>
                          <Text appearance="body-xs" color="black">
                            {purposesCount}
                          </Text>
                        </td>

                        <td>
                          <Text appearance="body-xs" color="black">
                            {activitiesCount}
                          </Text>
                        </td>

                        <td>
                          <Text appearance="body-xs" color="black">
                            {item.version}
                          </Text>
                        </td>

                        <td>
                          <Text appearance="body-xs" color="black">
                            {item.templateId}
                          </Text>
                        </td>

                        <td>
                          <span className={`status-badge ${getStatusClass(item.status)}`}>
                            {item.status}
                          </span>
                        </td>

                        <td>
                          <div className="template-actions">
                            {item.status === "PUBLISHED" && (
                              <button className="icon-btn" onClick={() => setOpenCode(true)} title="Integration Code">
                                <Icon ic={<IcCode />} size="medium" color="primary_grey_80" />
                              </button>
                            )}

                            <button className="icon-btn" onClick={() => handleOpenCL(item.templateId)} title="Consent Logs">
                              <Icon ic={<IcTicketDetails />} size="medium" color="primary_grey_80" />
                            </button>

                            <button
                              className="icon-btn"
                              onClick={() => handleOpenPendingRequests(item.templateId)}
                              title="Pending Requests"
                            >
                              <Icon ic={<IcTime />} size="medium" color="primary_grey_80" />
                            </button>

                            <button
                              className="icon-btn"
                              onClick={() =>
                                navigate(`/createConsent?templateId=${item.templateId}&version=${item.version}`)
                              }
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

      {/* Integration Code Modal */}
      {openCode && (
        <div className="modal-outer-container">
          <div className="modal-container-old">
            <div className="modal-close-btn-container">
              <ActionButton onClick={closeCode} icon={<IcClose />} kind="tertiary" size="small" />
            </div>

            <div style={{ display: "flex", gap: "5px", flexDirection: "column" }}>
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
                <Icon ic={<IcCode />} size="large" color="primary_50" />
              </div>

              <div style={{ marginLeft: "12px" }}>
                <Text appearance="heading-xs" color="primary-grey-100">
                  Embed Modal design in your code
                </Text>
              </div>

              <div style={{ marginLeft: "12px" }}>
                <Text appearance="body-s" color="primary-grey-80">
                  Download the script file in the desired integration format and add it to your file.
                </Text>
              </div>

              <div className="dropdown-group" style={{ marginTop: "10px" }}>
                <label>Select integration type </label>

                <select value={file} onChange={handleFileChange} id="grievance-type">
                  <option value="" disabled>
                    Select
                  </option>
                  <option value="JS">Java Script React JS</option>
                  {/* <option value="RN_ANDROID">React Native Android SDK</option> */}
                  <option value="ANDROID_NATIVE">Android Native SDK</option>
                </select>
              </div>
            </div>

            <br />
            <br />

            <div className="modal-add-btn-container-tem">
              <ActionButton label="I'll do it later" kind="secondary" onClick={closeCode} />
              <ActionButton label="Download file" onClick={handleDownload} />
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Templates;