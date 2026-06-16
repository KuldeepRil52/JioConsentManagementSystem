import React, { useState, useEffect, useMemo, useCallback } from "react";
import { ActionButton, Text, BadgeV2, Icon } from '../custom-components';
import { IcEditPen, IcSort, IcFilter, IcClose, IcVisible } from '../custom-components/Icon';
import { useNavigate } from "react-router-dom";
import { useSelector } from "react-redux";
import config from "../utils/config";
import { isSandboxMode, sandboxAPICall } from "../utils/sandboxMode";
import "../styles/auditCompliance.css";

const NotificationTemplate = () => {
  const navigate = useNavigate();
  const tenantId = useSelector((state) => state.common.tenant_id);
  const businessId = useSelector((state) => state.common.business_id);
  const sessionToken = useSelector((state) => state.common.session_token);

  // All templates fetched from API (master list)
  const [allTemplates, setAllTemplates] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  
  // Client-side pagination state
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  
  // Filter drawer state
  const [filterDrawerOpen, setFilterDrawerOpen] = useState(false);
  const [filters, setFilters] = useState({
    templateType: [], // CONSENT, GRIEVANCE
    channel: [], // EMAIL, SMS
  });
  
  // Sorting
  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState("asc");

  // Fetch ALL templates by paginating through all pages
  const fetchAllTemplates = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      if (isSandboxMode()) {
        console.log('🏖️ SANDBOX MODE - Fetching all notification templates');
        
        try {
          const mockResponse = await sandboxAPICall(
            `notification/templates?type=NOTIFICATION&page=1&pageSize=100`,
            'GET',
            {},
            {}
          );

          console.log('📧 Mock response:', mockResponse);

          const templatesArray = mockResponse?.data?.data || mockResponse?.data || [];
          
          console.log('📦 All templates received:', templatesArray.length);
          
          setAllTemplates(Array.isArray(templatesArray) ? templatesArray : []);
        } catch (error) {
          console.error('❌ Error fetching mock templates:', error);
          setError(error.message);
          setAllTemplates([]);
        } finally {
          setLoading(false);
        }
        return;
      }

      const scopeLevel = tenantId === businessId ? 'TENANT' : 'BUSINESS';
      const PAGE_SIZE = 100; // Max allowed by backend

      console.log('Fetching all notification templates...');

      // Fetch first page to get pagination info
      const firstResponse = await fetch(
        `${config.notification_templates}?type=NOTIFICATION&page=1&pageSize=${PAGE_SIZE}`,
        {
          method: 'GET',
          headers: {
            'X-Scope-Level': scopeLevel,
            'X-Tenant-Id': tenantId,
            'X-Business-Id': businessId,
            'x-session-token': sessionToken,
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          },
        }
      );

      if (!firstResponse.ok) {
        throw new Error(`Failed to fetch templates`);
      }

      const firstData = await firstResponse.json();
      const firstPageTemplates = firstData?.data?.data || [];
      const paginationInfo = firstData?.data?.pagination || {};
      
      console.log('📄 First page received:', firstPageTemplates.length);
      console.log('📄 Pagination info:', paginationInfo);

      let allFetchedTemplates = [...firstPageTemplates];

      // If there are more pages, fetch them all
      const totalPages = paginationInfo.totalPages || 1;
      
      if (totalPages > 1) {
        console.log(`📄 Fetching remaining ${totalPages - 1} pages...`);
        
        // Create promises for all remaining pages
        const pagePromises = [];
        for (let page = 2; page <= totalPages; page++) {
          pagePromises.push(
            fetch(
              `${config.notification_templates}?type=NOTIFICATION&page=${page}&pageSize=${PAGE_SIZE}`,
        {
          method: 'GET',
          headers: {
            'X-Scope-Level': scopeLevel,
            'X-Tenant-Id': tenantId,
            'X-Business-Id': businessId,
            'x-session-token': sessionToken,
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          },
        }
            ).then(res => res.json())
          );
        }

        // Fetch all remaining pages in parallel
        const remainingPages = await Promise.all(pagePromises);
        
        remainingPages.forEach((pageData, index) => {
          const pageTemplates = pageData?.data?.data || [];
          console.log(`📄 Page ${index + 2} received:`, pageTemplates.length);
          allFetchedTemplates = [...allFetchedTemplates, ...pageTemplates];
        });
      }

      console.log('📦 Total templates fetched:', allFetchedTemplates.length);
      
      setAllTemplates(allFetchedTemplates);
    } catch (err) {
      console.error("Error fetching notification templates:", err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [tenantId, businessId, sessionToken]);

  // Fetch all templates once on mount
  useEffect(() => {
    if (isSandboxMode()) {
      fetchAllTemplates();
    } else if (tenantId && businessId && sessionToken) {
      fetchAllTemplates();
    }
  }, [tenantId, businessId, sessionToken, fetchAllTemplates]);

  // Reset to page 1 when search or filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery, filters.channel, filters.templateType]);

  // Map event types to template type categories
  const getTemplateTypeFromEventType = (eventType) => {
    if (!eventType) return null;
    
    // Special mappings for specific event types
    const specialMappings = {
      'DATA_BREACHED': 'BREACH',
      'DATA_DELETED': 'DATA',
      'DATA_SHARED': 'DATA',
    };
    
    // Check for special mapping first
    if (specialMappings[eventType]) {
      return specialMappings[eventType];
    }
    
    // Default: extract prefix before first underscore
    return eventType.split('_')[0];
  };

  // Extract unique template type categories dynamically from fetched data
  const availableTemplateTypes = useMemo(() => {
    const typeSet = new Set();
    
    allTemplates.forEach(template => {
      const type = getTemplateTypeFromEventType(template.eventType);
      if (type) {
        typeSet.add(type);
      }
    });
    
    // Convert to array and sort alphabetically
    return Array.from(typeSet).sort();
  }, [allTemplates]);

  // Extract unique channels dynamically from fetched data
  const availableChannels = useMemo(() => {
    const channelSet = new Set();
    
    allTemplates.forEach(template => {
      if (template.channel) {
        channelSet.add(template.channel);
      }
    });
    
    // Convert to array and sort alphabetically
    return Array.from(channelSet).sort();
  }, [allTemplates]);

  // Format template type for display (e.g., "CONSENT" -> "Consent")
  const formatTemplateType = (type) => {
    return type.charAt(0).toUpperCase() + type.slice(1).toLowerCase();
  };

  // Format channel for display (e.g., "EMAIL" -> "Email")
  const formatChannel = (channel) => {
    return channel.charAt(0).toUpperCase() + channel.slice(1).toLowerCase();
  };

  // Filter, search, and sort templates (all client-side)
  const filteredAndSortedTemplates = useMemo(() => {
    let result = [...allTemplates];

    // Apply channel filter
    if (filters.channel.length > 0) {
      result = result.filter(template =>
        filters.channel.includes(template.channel)
      );
    }

    // Apply template type filter (using mapped categories)
    if (filters.templateType.length > 0) {
      result = result.filter(template => {
        const templateType = getTemplateTypeFromEventType(template.eventType);
        return filters.templateType.includes(templateType);
      });
    }

    // Apply search
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      result = result.filter(template =>
        (template.eventType?.toLowerCase() || '').includes(query) ||
        (template.emailConfig?.subject?.toLowerCase() || '').includes(query) ||
        (template.template?.toLowerCase() || '').includes(query) ||
        (template.smsConfig?.dltTemplateId?.toLowerCase() || '').includes(query)
      );
    }

    // Apply sorting
    if (sortColumn) {
      result.sort((a, b) => {
      let aVal, bVal;

      switch (sortColumn) {
        case 'eventType':
          aVal = a.eventType || '';
          bVal = b.eventType || '';
          break;
        case 'channel':
          aVal = a.channel || '';
          bVal = b.channel || '';
          break;
        case 'subject':
          aVal = a.channel === 'EMAIL' 
            ? (a.emailConfig?.subject || '') 
            : (a.smsConfig?.dltTemplateId || '');
          bVal = b.channel === 'EMAIL' 
            ? (b.emailConfig?.subject || '') 
            : (b.smsConfig?.dltTemplateId || '');
          break;
        case 'content':
          aVal = a.channel === 'EMAIL' 
            ? (a.emailConfig?.body?.replace(/<[^>]*>/g, '') || '') 
            : (a.template || '');
          bVal = b.channel === 'EMAIL' 
            ? (b.emailConfig?.body?.replace(/<[^>]*>/g, '') || '') 
            : (b.template || '');
          break;
        default:
          aVal = a[sortColumn] || '';
          bVal = b[sortColumn] || '';
      }

      if (typeof aVal === "string") aVal = aVal.toLowerCase();
      if (typeof bVal === "string") bVal = bVal.toLowerCase();

      if (aVal < bVal) return sortDirection === "asc" ? -1 : 1;
      if (aVal > bVal) return sortDirection === "asc" ? 1 : -1;
      return 0;
    });
    }

    return result;
  }, [allTemplates, filters, searchQuery, sortColumn, sortDirection]);

  // Calculate pagination from filtered results (client-side)
  const pagination = useMemo(() => {
    const totalItems = filteredAndSortedTemplates.length;
    const totalPages = Math.ceil(totalItems / pageSize);
    return {
      totalItems,
      totalPages,
      hasNext: currentPage < totalPages,
      hasPrevious: currentPage > 1
    };
  }, [filteredAndSortedTemplates.length, pageSize, currentPage]);

  // Get current page's templates
  const paginatedTemplates = useMemo(() => {
    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    return filteredAndSortedTemplates.slice(startIndex, endIndex);
  }, [filteredAndSortedTemplates, currentPage, pageSize]);

  // Page navigation handlers
  const handlePageChange = (newPage) => {
    if (newPage >= 1 && newPage <= pagination.totalPages) {
      setCurrentPage(newPage);
    }
  };

  const handlePageSizeChange = (newSize) => {
    setPageSize(newSize);
    setCurrentPage(1); // Reset to first page when page size changes
  };

  // Generate page numbers for pagination
  const getPageNumbers = () => {
    const pages = [];
    const totalPages = pagination.totalPages;
    const current = currentPage;
    
    if (totalPages <= 7) {
      for (let i = 1; i <= totalPages; i++) {
        pages.push(i);
      }
    } else {
      if (current <= 3) {
        pages.push(1, 2, 3, 4, '...', totalPages);
      } else if (current >= totalPages - 2) {
        pages.push(1, '...', totalPages - 3, totalPages - 2, totalPages - 1, totalPages);
      } else {
        pages.push(1, '...', current - 1, current, current + 1, '...', totalPages);
      }
    }
    
    return pages;
  };

  // Handle sort
  const handleSort = (column) => {
    if (sortColumn === column) {
      setSortDirection(sortDirection === "asc" ? "desc" : "asc");
    } else {
      setSortColumn(column);
      setSortDirection("asc");
    }
  };


  const handleCreateNew = (channel) => {
    // For now, default to consent templates
    const path = channel === "EMAIL" ? "/createEmailTemplate" : "/createSmsTemplate";
    navigate(path, {
      state: {
        from: '/notification-templates'
      }
    });
  };

  const toggleFilter = (filterType, value) => {
    setFilters(prev => ({
      ...prev,
      [filterType]: prev[filterType].includes(value)
        ? prev[filterType].filter(v => v !== value)
        : [...prev[filterType], value]
    }));
  };

  const clearFilters = () => {
    setFilters({
      templateType: [],
      channel: [],
    });
    setSearchQuery("");
    setCurrentPage(1); // Reset to first page
  };

  const handleApplyFilters = () => {
    setFilterDrawerOpen(false);
  };

  const activeFilterCount = filters.templateType.length + filters.channel.length;

  const getChannelBadge = (channel) => {
    if (channel === 'EMAIL') {
      return <BadgeV2 label="Email" variant="info" size="small" />;
    } else if (channel === 'SMS') {
      return <BadgeV2 label="SMS" variant="success" size="small" />;
    }
    return <BadgeV2 label="Unknown" variant="default" size="small" />;
  };

  const getSubjectOrDltId = (template) => {
    if (template.channel === 'EMAIL') {
      return template.emailConfig?.subject || 'N/A';
    } else if (template.channel === 'SMS') {
      return template.smsConfig?.dltTemplateId || 'N/A';
    }
    return 'N/A';
  };

  const getMessageDisplay = (template) => {
    if (template.channel === 'EMAIL') {
      const body = template.emailConfig?.body || '';
      const strippedBody = body.replace(/<[^>]*>/g, '');
      return strippedBody.substring(0, 100) + (strippedBody.length > 100 ? '...' : '');
    } else if (template.channel === 'SMS') {
      const message = template.template || '';
      return message.substring(0, 100) + (message.length > 100 ? '...' : '');
    }
    return 'N/A';
  };

  return (
    <>
      <div className="configurePage">
        {/* Header Section */}
        <div className="audit-header-section">
          <div className="audit-title-section">
            <div style={{ display: 'flex', flexDirection: 'row', gap: '10px' }}>
              <Text appearance="heading-s" color="primary-grey-100">
                Notification Templates
              </Text>
              <div className="dataProtectionOfficer-badge" style={{ marginTop: '5px' }}>
                <Text appearance="body-xs-bold" color="primary-grey-80">
                  Notifications
                </Text>
              </div>
            </div>
          </div>
          <div className="audit-button-group">
            <ActionButton
              label="Add Email Template"
              onClick={() => handleCreateNew('EMAIL')}
              kind="primary"
              size="medium"
            />
            <ActionButton
              label="Add SMS Template"
              onClick={() => handleCreateNew('SMS')}
              kind="secondary"
              size="medium"
            />
          </div>
        </div>

        {/* Table Controls */}
        <div className="audit-table-controls">
          <div className="audit-search-container">
            <input
              type="text"
              placeholder="Search templates..."
              className="audit-search-input"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            <svg className="audit-search-icon" width="20" height="20" viewBox="0 0 24 24" fill="none">
              <circle cx="11" cy="11" r="8" stroke="currentColor" strokeWidth="2"/>
              <path d="M21 21L16.65 16.65" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
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
                {/* Template Type Filter - Dynamic */}
                <div className="filter-section">
                  <Text appearance="body-s-bold" color="primary-grey-100">
                    Template Type
                  </Text>
                  <div className="filter-chips" style={{ flexWrap: 'wrap', gap: '8px' }}>
                    {availableTemplateTypes.map((type) => (
                    <div
                        key={type}
                        className={`filter-chip ${filters.templateType.includes(type) ? 'active' : ''}`}
                        onClick={() => toggleFilter('templateType', type)}
                    >
                        {formatTemplateType(type)}
                    </div>
                    ))}
                    {availableTemplateTypes.length === 0 && (
                      <Text appearance="body-xs" color="primary-grey-60">
                        Loading...
                      </Text>
                    )}
                  </div>
                </div>

                {/* Channel Filter - Dynamic */}
                <div className="filter-section">
                  <Text appearance="body-s-bold" color="primary-grey-100">
                    Channel
                  </Text>
                  <div className="filter-chips" style={{ flexWrap: 'wrap', gap: '8px' }}>
                    {availableChannels.map((channel) => (
                    <div
                        key={channel}
                        className={`filter-chip ${filters.channel.includes(channel) ? 'active' : ''}`}
                        onClick={() => toggleFilter('channel', channel)}
                    >
                        {formatChannel(channel)}
                    </div>
                    ))}
                    {availableChannels.length === 0 && (
                      <Text appearance="body-xs" color="primary-grey-60">
                        Loading...
                      </Text>
                    )}
                  </div>
                </div>
              </div>

              <div className="filter-drawer-footer">
                <ActionButton
                  label="Clear All"
                  kind="secondary"
                  onClick={clearFilters}
                />
                <ActionButton
                  label="Apply Filters"
                  kind="primary"
                  onClick={handleApplyFilters}
                />
              </div>
            </div>
          </>
        )}

        {/* Table */}
        <div className="audit-table-wrapper">
          <table className="audit-table">
            <thead>
              <tr>
                <th onClick={() => handleSort("eventType")} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    Event Type
                    <IcSort height={14} width={14} />
                  </div>
                </th>
                <th onClick={() => handleSort("channel")} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    Channel
                    <IcSort height={14} width={14} />
                  </div>
                </th>
                <th onClick={() => handleSort("subject")} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    Subject / DLT ID
                    <IcSort height={14} width={14} />
                  </div>
                </th>
                <th onClick={() => handleSort("content")} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    Message
                    <IcSort height={14} width={14} />
                  </div>
                </th>
                <th style={{ position: 'sticky', right: 0, backgroundColor: '#f5f5f5', zIndex: 1 }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="5" style={{ textAlign: "center", padding: "40px" }}>
                    Loading templates...
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan="5" style={{ textAlign: "center", padding: "40px" }}>
                    <Text appearance="body-md" color="error">
                      Error: {error}
                    </Text>
                    <br />
                    <ActionButton
                      label="Retry"
                      onClick={fetchAllTemplates}
                      kind="secondary"
                      size="small"
                      style={{ marginTop: "16px" }}
                    />
                  </td>
                </tr>
              ) : paginatedTemplates.length === 0 ? (
                <tr>
                  <td colSpan="5" style={{ textAlign: "center", padding: "40px" }}>
                    {searchQuery || activeFilterCount > 0 
                      ? "No templates match your search or filters" 
                      : "No templates found"}
                  </td>
                </tr>
              ) : (
                paginatedTemplates.map((template, index) => (
                  <tr key={template.id || template.templateId || index}>
                    <td>{template.eventType || 'N/A'}</td>
                    <td>{getChannelBadge(template.channel)}</td>
                    <td>{getSubjectOrDltId(template)}</td>
                    <td>{getMessageDisplay(template)}</td>
                    <td style={{ position: 'sticky', right: 0, backgroundColor: '#f9f9f9' }}>
                      <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                        <button
                          className="icon-btn"
                          onClick={() => {
                            const isConsent = template.eventType?.startsWith("CONSENT");
                            const isEmail = template.channel === "EMAIL";
                            
                            let path;
                            if (isConsent) {
                              path = isEmail ? "/createEmailTemplate" : "/createSmsTemplate";
                            } else {
                              path = isEmail ? "/createGrievanceEmailTemplate" : "/createGrievanceSmsTemplate";
                            }
                            
                            navigate(path, {
                              state: {
                                viewMode: true,
                                templateData: template,
                                from: '/notification-templates'
                              }
                            });
                          }}
                          title="View"
                        >
                          <IcVisible height={18} width={18} />
                        </button>
                        <button
                          className="icon-btn"
                          onClick={() => {
                            const isConsent = template.eventType?.startsWith("CONSENT");
                            const isEmail = template.channel === "EMAIL";
                            
                            let path;
                            if (isConsent) {
                              path = isEmail ? "/createEmailTemplate" : "/createSmsTemplate";
                            } else {
                              path = isEmail ? "/createGrievanceEmailTemplate" : "/createGrievanceSmsTemplate";
                            }
                            
                            navigate(path, {
                              state: {
                                editMode: true,
                                templateData: template,
                                from: '/notification-templates'
                              }
                            });
                          }}
                          title="Edit"
                        >
                          <IcEditPen height={18} width={18} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        <div className="audit-pagination-container">
          <div className="audit-pagination-info">
            <Text appearance="body-s" color="primary-grey-80">
              {pagination.totalItems > 0 
                ? `Showing ${((currentPage - 1) * pageSize) + 1} - ${Math.min(currentPage * pageSize, pagination.totalItems)} of ${pagination.totalItems} templates`
                : 'No templates found'
              }
              {allTemplates.length !== pagination.totalItems && ` (filtered from ${allTemplates.length} total)`}
            </Text>
          </div>
          
          <div className="audit-pagination-controls" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {/* Page Size Selector */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginRight: '16px' }}>
              <Text appearance="body-s" color="primary-grey-80">Rows per page:</Text>
              <select
                value={pageSize}
                onChange={(e) => handlePageSizeChange(Number(e.target.value))}
                style={{
                  padding: '4px 8px',
                  borderRadius: '4px',
                  border: '1px solid #ccc',
                  backgroundColor: '#fff',
                  cursor: 'pointer'
                }}
              >
                <option value={10}>10</option>
                <option value={20}>20</option>
                <option value={50}>50</option>
                <option value={100}>100</option>
              </select>
            </div>

            {/* Previous Button */}
            <button
              className="pagination-btn"
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={!pagination.hasPrevious}
              style={{
                padding: '6px 12px',
                borderRadius: '4px',
                border: '1px solid #ccc',
                backgroundColor: pagination.hasPrevious ? '#fff' : '#f5f5f5',
                cursor: pagination.hasPrevious ? 'pointer' : 'not-allowed',
                opacity: pagination.hasPrevious ? 1 : 0.5
              }}
            >
              Previous
            </button>

            {/* Page Numbers */}
            <div style={{ display: 'flex', gap: '4px' }}>
              {getPageNumbers().map((pageNum, index) => (
                pageNum === '...' ? (
                  <span key={`ellipsis-${index}`} style={{ padding: '6px 8px' }}>...</span>
                ) : (
                  <button
                    key={pageNum}
                    onClick={() => handlePageChange(pageNum)}
                    style={{
                      padding: '6px 12px',
                      borderRadius: '4px',
                      border: currentPage === pageNum ? '1px solid #0066cc' : '1px solid #ccc',
                      backgroundColor: currentPage === pageNum ? '#0066cc' : '#fff',
                      color: currentPage === pageNum ? '#fff' : '#333',
                      cursor: 'pointer',
                      fontWeight: currentPage === pageNum ? 'bold' : 'normal'
                    }}
                  >
                    {pageNum}
                  </button>
                )
              ))}
            </div>

            {/* Next Button */}
            <button
              className="pagination-btn"
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={!pagination.hasNext}
              style={{
                padding: '6px 12px',
                borderRadius: '4px',
                border: '1px solid #ccc',
                backgroundColor: pagination.hasNext ? '#fff' : '#f5f5f5',
                cursor: pagination.hasNext ? 'pointer' : 'not-allowed',
                opacity: pagination.hasNext ? 1 : 0.5
              }}
            >
              Next
            </button>
          </div>
        </div>
      </div>
    </>
  );
};

export default NotificationTemplate;
