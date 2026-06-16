import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { useSelector, useDispatch } from "react-redux";
import { Text, Icon, Spinner } from '../custom-components';
import { IcSearch, IcClose } from '../custom-components/Icon';
import "../styles/globalSearch.css";
import { getAllTemplates, fetchGrievanceTemplates, listUsers } from "../store/actions/CommonAction";
import { generateTransactionId } from "../utils/transactionId";
import config from "../utils/config";
import { formatToIST } from "../utils/dateUtils";

const GlobalSearch = ({ isOpen, onClose }) => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const tenantId = useSelector((state) => state.common.tenant_id);
  const businessId = useSelector((state) => state.common.business_id);
  const token = useSelector((state) => state.common.session_token);
  
  // Get businesses directly from Redux state (already fetched on app load)
  const businessesFromRedux = useSelector((state) => state.common.businesses) || [];

  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState({
    templates: [],
    grievances: [],
    consents: [],
    breaches: [],
    audits: [],
    notificationTemplates: [],
    users: [],
    businesses: [],
  });
  const [loading, setLoading] = useState(false);
  const [loadingMessage, setLoadingMessage] = useState("Initializing search...");
  const [activeTab, setActiveTab] = useState("all");
  const [selectedConsent, setSelectedConsent] = useState(null);
  const [showConsentDetails, setShowConsentDetails] = useState(false);
  const searchInputRef = useRef(null);
  const modalRef = useRef(null);

  // Focus input when modal opens
  useEffect(() => {
    if (isOpen && searchInputRef.current) {
      searchInputRef.current.focus();
    }
  }, [isOpen]);

  // Close on escape key
  useEffect(() => {
    const handleEscape = (e) => {
      if (e.key === "Escape" && isOpen) {
        onClose();
      }
    };
    document.addEventListener("keydown", handleEscape);
    return () => document.removeEventListener("keydown", handleEscape);
  }, [isOpen, onClose]);

  // Close when clicking outside
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (modalRef.current && !modalRef.current.contains(e.target)) {
        onClose();
      }
    };
    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isOpen, onClose]);

  // Debounced search function
  useEffect(() => {
    if (!searchQuery.trim() || !isOpen) {
      setSearchResults({ templates: [], grievances: [], consents: [], breaches: [], audits: [], notificationTemplates: [], users: [], businesses: [] });
      setLoading(false);
      return;
    }

    // Check if authentication credentials are available
    if (!tenantId || !businessId || !token) {
      console.warn("⚠️ Missing authentication credentials - tenantId:", tenantId, "businessId:", businessId, "token:", token ? "present" : "missing");
      setLoading(false);
      return;
    }

    setLoading(true);
    setLoadingMessage("Preparing search...");

    const timer = setTimeout(() => {
      performSearch(searchQuery);
    }, 500); // Increased debounce time to reduce API calls

    return () => clearTimeout(timer);
  }, [searchQuery, isOpen, tenantId, businessId, token]);

  const performSearch = async (query) => {
    setLoading(true);
    setLoadingMessage("Starting search...");
    console.log("🔍 Starting search for:", query);
    console.log("📋 Search context - TenantId:", tenantId, "BusinessId:", businessId);
    
    try {
      const results = {
        templates: [],
        grievances: [],
        consents: [],
        breaches: [],
        audits: [],
        notificationTemplates: [],
        users: [],
        businesses: [],
      };

      // Search Consent Templates
      try {
        setLoadingMessage("Searching consent templates...");
        console.log("📄 Fetching consent templates...");
        const templatesData = await dispatch(getAllTemplates(tenantId, businessId));
        console.log("📄 Templates data received:", templatesData);
        const allTemplates = templatesData?.searchList || [];
        console.log("📄 Total templates available:", allTemplates.length);
        
        // Log all template names for debugging
        if (allTemplates.length > 0) {
          console.log("📄 Available template names:", allTemplates.map(t => t.templateName));
        }
        
        results.templates = allTemplates.filter((template) =>
          template.templateName?.toLowerCase().includes(query.toLowerCase()) ||
          template.templateId?.toLowerCase().includes(query.toLowerCase())
        );
        console.log("📄 Filtered templates:", results.templates.length);
      } catch (err) {
        console.error("❌ Error searching templates:", err);
      }

      // Search Grievance Templates
      try {
        setLoadingMessage("Searching grievance templates...");
        console.log("📋 Fetching grievance templates...");
        const grievanceData = await fetchGrievanceTemplates(tenantId, businessId, token);
        console.log("📋 Grievances data received:", grievanceData);
        
        // The response is directly an array, not nested in a 'data' property
        const allGrievances = Array.isArray(grievanceData) ? grievanceData : (grievanceData?.data || []);
        console.log("📋 Total grievances available:", allGrievances.length);
        
        // Log all grievance names for debugging
        if (allGrievances.length > 0) {
          console.log("📋 Available grievance names:", allGrievances.map(g => g.grievanceTemplateName));
        }
        
        results.grievances = allGrievances.filter((grievance) =>
          grievance.grievanceTemplateName?.toLowerCase().includes(query.toLowerCase()) ||
          grievance.grievanceTemplateId?.toLowerCase().includes(query.toLowerCase())
        );
        console.log("📋 Filtered grievances:", results.grievances.length);
      } catch (err) {
        console.error("❌ Error searching grievances:", err);
      }

      // Search Consents by Customer ID
      try {
        setLoadingMessage("Searching consents by customer ID...");
        console.log("👤 Searching consents by customer ID...");
        const txnIdConsent = generateTransactionId();
        const consentResponse = await fetch(
          `${config.consent_search}?customerIdentifiers.value=${encodeURIComponent(query)}`,
          {
            method: "GET",
            headers: {
              'Accept': '*/*',
              'tenant-id': tenantId,
              'business-id': businessId,
              'x-session-token': token,
              'txn': txnIdConsent,
            },
          }
        );
        if (consentResponse.ok) {
          const consentData = await consentResponse.json();
          console.log("👤 Consent data received:", consentData);
          const allConsents = consentData?.searchList || consentData?.data || [];
          console.log("👤 Total consents available:", allConsents.length);
          
          // Log sample consent entry for debugging
          if (allConsents.length > 0) {
            console.log("👤 Sample consent entry:", allConsents[0]);
          }
          
          results.consents = allConsents;
          console.log("👤 Filtered consents:", results.consents.length);
        } else {
          console.error("❌ Consent API returned status:", consentResponse.status);
          const errorText = await consentResponse.text();
          console.error("❌ Consent API error:", errorText);
        }
      } catch (err) {
        console.error("❌ Error searching consents:", err);
      }

      // Search Breach Notifications
      try {
        setLoadingMessage("Searching breach notifications...");
        console.log("🔴 Fetching breach notifications...");
        const txnIdBreach = generateTransactionId();
        const breachResponse = await fetch(
          `${config.data_breach_base}?businessId=${businessId}`,
          {
            method: "GET",
            headers: {
              'accept': 'application/json',
              'txn': txnIdBreach,
              'tenant-id': tenantId,
              'business-id': businessId,
              'x-session-token': token,
              'Content-Type': 'application/json',
            },
          }
        );
        if (breachResponse.ok) {
          const breachData = await breachResponse.json();
          console.log("🔴 Breach data received:", breachData);
          const allBreaches = breachData?.searchList || [];
          console.log("🔴 Total breaches available:", allBreaches.length);
          
          // Log sample breach entry for debugging
          if (allBreaches.length > 0) {
            console.log("🔴 Sample breach entry:", allBreaches[0]);
          }
          
          results.breaches = allBreaches.filter((breach) =>
            breach.incidentId?.toLowerCase().includes(query.toLowerCase()) ||
            breach.incidentDetails?.breachType?.toLowerCase().includes(query.toLowerCase()) ||
            breach.incidentDetails?.briefDescription?.toLowerCase().includes(query.toLowerCase())
          );
          console.log("🔴 Filtered breaches:", results.breaches.length);
        } else {
          console.error("❌ Breach API returned status:", breachResponse.status);
          const errorText = await breachResponse.text();
          console.error("❌ Breach API error:", errorText);
        }
      } catch (err) {
        console.error("❌ Error searching breaches:", err);
      }

      // Search Audit Reports
      try {
        setLoadingMessage("Searching audit reports...");
        console.log("📈 Fetching audit reports...");
        const txnIdAudit = generateTransactionId();
        const auditResponse = await fetch(
          `${config.audit_reports}?page=0&size=100&sort=DESC`,
          {
            method: "GET",
            headers: {
              'accept': '*/*',
              'X-Tenant-ID': tenantId,
              'X-Business-ID': businessId,
              'X-Transaction-ID': txnIdAudit,
              'Content-Type': 'application/json',
              'business-id': businessId,
              'x-session-token': token,
            },
          }
        );
        if (auditResponse.ok) {
          const auditData = await auditResponse.json();
          console.log("📈 Audit data received:", auditData);
          const allAudits = auditData?.data || auditData?.content || [];
          console.log("📈 Total audits available:", allAudits.length);
          
          // Log sample audit entry for debugging
          if (allAudits.length > 0) {
            console.log("📈 Sample audit entry:", allAudits[0]);
          }
          
          results.audits = allAudits.filter((audit) =>
            audit.auditId?.toLowerCase().includes(query.toLowerCase()) ||
            audit.group?.toLowerCase().includes(query.toLowerCase()) ||
            audit.component?.toLowerCase().includes(query.toLowerCase()) ||
            audit.actionType?.toLowerCase().includes(query.toLowerCase())
          );
          console.log("📈 Filtered audits:", results.audits.length);
        } else {
          console.error("❌ Audit API returned status:", auditResponse.status);
          const errorText = await auditResponse.text();
          console.error("❌ Audit API error:", errorText);
        }
      } catch (err) {
        console.error("❌ Error searching audits:", err);
      }

      // Search Notification Templates
      try {
        setLoadingMessage("Searching notification templates...");
        console.log("📧 Fetching notification templates...");
        const scopeLevel = tenantId === businessId ? 'TENANT' : 'BUSINESS';
        const notifResponse = await fetch(
          `${config.notification_templates}?type=NOTIFICATION&page=1&pageSize=100`,
          {
            method: "GET",
            headers: {
              'X-Scope-Level': scopeLevel,
              'X-Tenant-Id': tenantId,
              'X-Business-Id': businessId,
              'x-session-token': token,
              'Content-Type': 'application/json',
              'Accept': 'application/json'
            },
          }
        );
        if (notifResponse.ok) {
          const notifData = await notifResponse.json();
          console.log("📧 Notification templates received:", notifData);
          const allNotifTemplates = notifData?.data?.data || [];
          console.log("📧 Total notification templates:", allNotifTemplates.length);
          
          results.notificationTemplates = allNotifTemplates.filter((template) =>
            template.eventType?.toLowerCase().includes(query.toLowerCase()) ||
            template.channel?.toLowerCase().includes(query.toLowerCase()) ||
            template.template?.toLowerCase().includes(query.toLowerCase()) ||
            template.emailConfig?.subject?.toLowerCase().includes(query.toLowerCase())
          );
          console.log("📧 Filtered notification templates:", results.notificationTemplates.length);
        } else {
          console.error("❌ Notification templates API returned status:", notifResponse.status);
        }
      } catch (err) {
        console.error("❌ Error searching notification templates:", err);
      }

      // Search Users using dispatch action
      try {
        setLoadingMessage("Searching users...");
        console.log("👥 Fetching users via dispatch...");
        const usersData = await dispatch(listUsers());
        console.log("👥 Users data received:", usersData);
        const allUsers = usersData?.data || usersData?.users || usersData || [];
        console.log("👥 Total users:", Array.isArray(allUsers) ? allUsers.length : 0);
        
        results.users = (Array.isArray(allUsers) ? allUsers : []).filter((user) =>
          user.name?.toLowerCase().includes(query.toLowerCase()) ||
          user.email?.toLowerCase().includes(query.toLowerCase()) ||
          user.userId?.toLowerCase().includes(query.toLowerCase()) ||
          user.role?.toLowerCase().includes(query.toLowerCase()) ||
          user.firstName?.toLowerCase().includes(query.toLowerCase()) ||
          user.lastName?.toLowerCase().includes(query.toLowerCase())
        );
        console.log("👥 Filtered users:", results.users.length);
      } catch (err) {
        console.error("❌ Error searching users:", err);
      }

      // Search Businesses from Redux state (already loaded)
      try {
        setLoadingMessage("Searching businesses...");
        console.log("🏢 Searching businesses from Redux state...");
        console.log("🏢 Total businesses in Redux:", businessesFromRedux.length);
        
        if (businessesFromRedux.length > 0) {
          results.businesses = businessesFromRedux.filter((business) =>
            business.businessName?.toLowerCase().includes(query.toLowerCase()) ||
            business.businessId?.toLowerCase().includes(query.toLowerCase()) ||
            business.description?.toLowerCase().includes(query.toLowerCase()) ||
            business.name?.toLowerCase().includes(query.toLowerCase())
          );
          console.log("🏢 Filtered businesses:", results.businesses.length);
        } else {
          console.log("🏢 No businesses in Redux state, trying API...");
          // Fallback to API if Redux is empty
          const businessesResponse = await fetch(
            `${config.business_application_search}?tenantId=${tenantId}`,
            {
              method: "GET",
              headers: {
                'accept': 'application/json',
                'tenant-id': tenantId,
                'x-session-token': token,
              },
            }
          );
          if (businessesResponse.ok) {
            const businessesData = await businessesResponse.json();
            const allBusinesses = businessesData?.searchList || businessesData?.data || [];
            results.businesses = allBusinesses.filter((business) =>
              business.businessName?.toLowerCase().includes(query.toLowerCase()) ||
              business.businessId?.toLowerCase().includes(query.toLowerCase()) ||
              business.description?.toLowerCase().includes(query.toLowerCase()) ||
              business.name?.toLowerCase().includes(query.toLowerCase())
            );
          }
        }
      } catch (err) {
        console.error("❌ Error searching businesses:", err);
      }

      console.log("🎯 Final search results:", results);
      setLoadingMessage("Finalizing results...");
      setSearchResults(results);
    } catch (error) {
      console.error("❌ Error performing search:", error);
      setLoadingMessage("Search completed with errors");
    } finally {
      setLoading(false);
    }
  };

  const handleResultClick = (type, item) => {
    switch (type) {
      case "template":
        // Navigate to edit consent template page
        onClose();
        setSearchQuery("");
        navigate(`/createConsent?templateId=${item.templateId}&version=${item.version}`);
        break;
      case "grievance":
        // Navigate to edit grievance form page
        onClose();
        setSearchQuery("");
        navigate(`/createGrievanceForm?templateId=${item.grievanceTemplateId}`);
        break;
      case "consent":
        // Show consent details in modal
        setSelectedConsent(item);
        setShowConsentDetails(true);
        break;
      case "breach":
        // Navigate to Breach Notifications page
        onClose();
        setSearchQuery("");
        navigate(`/breach-notifications`);
        break;
      case "audit":
        // Navigate to Audit Compliance page
        onClose();
        setSearchQuery("");
        navigate(`/audit-compliance`);
        break;
      case "notification":
        // Navigate to Notification Templates page
        onClose();
        setSearchQuery("");
        navigate(`/notification-templates`);
        break;
      case "user":
        // Navigate to Users page
        onClose();
        setSearchQuery("");
        navigate(`/users`);
        break;
      case "business":
        // Navigate to Business Groups page
        onClose();
        setSearchQuery("");
        navigate(`/businessGroups`);
        break;
      default:
        break;
    }
  };

  const handleBackToResults = () => {
    setShowConsentDetails(false);
    setSelectedConsent(null);
  };

  const getTotalResults = () => {
    return (
      searchResults.templates.length +
      searchResults.grievances.length +
      searchResults.consents.length +
      searchResults.breaches.length +
      searchResults.audits.length +
      searchResults.notificationTemplates.length +
      searchResults.users.length +
      searchResults.businesses.length
    );
  };

  const filteredResults = () => {
    const emptyResults = { templates: [], grievances: [], consents: [], breaches: [], audits: [], notificationTemplates: [], users: [], businesses: [] };
    if (activeTab === "all") return searchResults;
    if (activeTab === "templates") return { ...emptyResults, templates: searchResults.templates };
    if (activeTab === "grievances") return { ...emptyResults, grievances: searchResults.grievances };
    if (activeTab === "consents") return { ...emptyResults, consents: searchResults.consents };
    if (activeTab === "breaches") return { ...emptyResults, breaches: searchResults.breaches };
    if (activeTab === "audits") return { ...emptyResults, audits: searchResults.audits };
    if (activeTab === "notifications") return { ...emptyResults, notificationTemplates: searchResults.notificationTemplates };
    if (activeTab === "users") return { ...emptyResults, users: searchResults.users };
    if (activeTab === "businesses") return { ...emptyResults, businesses: searchResults.businesses };
    return searchResults;
  };

  if (!isOpen) return null;

  return (
    <div className="global-search-overlay">
      <div className="global-search-modal" ref={modalRef}>
        {/* Search Header */}
        <div className="search-header">
          <div className="search-input-container">
            <Icon ic={<IcSearch />} size="md" color="primary-grey-60" />
            <input
              ref={searchInputRef}
              type="text"
              placeholder="Search templates, notifications, users, businesses, consents, breaches, audits..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="search-input"
            />
            {searchQuery && (
              <button
                className="clear-search-btn"
                onClick={() => setSearchQuery("")}
              >
                <Icon ic={<IcClose />} size="sm" color="primary-grey-60" />
              </button>
            )}
          </div>
          <button className="close-search-btn" onClick={onClose}>
            <Icon ic={<IcClose />} size="md" color="primary-grey-80" />
          </button>
        </div>

        {/* Tabs */}
        {searchQuery && (
          <div className="search-tabs">
            <button
              className={`search-tab ${activeTab === "all" ? "active" : ""}`}
              onClick={() => setActiveTab("all")}
            >
              All ({getTotalResults()})
            </button>
            <button
              className={`search-tab ${activeTab === "templates" ? "active" : ""}`}
              onClick={() => setActiveTab("templates")}
            >
              Templates ({searchResults.templates.length})
            </button>
            <button
              className={`search-tab ${activeTab === "grievances" ? "active" : ""}`}
              onClick={() => setActiveTab("grievances")}
            >
              Grievances ({searchResults.grievances.length})
            </button>
            <button
              className={`search-tab ${activeTab === "consents" ? "active" : ""}`}
              onClick={() => setActiveTab("consents")}
            >
              Consents ({searchResults.consents.length})
            </button>
            <button
              className={`search-tab ${activeTab === "breaches" ? "active" : ""}`}
              onClick={() => setActiveTab("breaches")}
            >
              Breaches ({searchResults.breaches.length})
            </button>
            <button
              className={`search-tab ${activeTab === "audits" ? "active" : ""}`}
              onClick={() => setActiveTab("audits")}
            >
              Audits ({searchResults.audits.length})
            </button>
            <button
              className={`search-tab ${activeTab === "notifications" ? "active" : ""}`}
              onClick={() => setActiveTab("notifications")}
            >
              Notifications ({searchResults.notificationTemplates.length})
            </button>
            <button
              className={`search-tab ${activeTab === "users" ? "active" : ""}`}
              onClick={() => setActiveTab("users")}
            >
              Users ({searchResults.users.length})
            </button>
            <button
              className={`search-tab ${activeTab === "businesses" ? "active" : ""}`}
              onClick={() => setActiveTab("businesses")}
            >
              Businesses ({searchResults.businesses.length})
            </button>
          </div>
        )}

        {/* Search Results */}
        <div className="search-results-container">
          {showConsentDetails && selectedConsent ? (
            // Consent Details View
            <div className="consent-details-view">
              <div className="consent-details-header">
                <button className="back-button" onClick={handleBackToResults}>
                  <Text appearance="body-s">← Back to Results</Text>
                </button>
                <div className={`consent-status-badge status-${selectedConsent.status?.toLowerCase() || "active"}`}>
                  {selectedConsent.status || "ACTIVE"}
                </div>
              </div>

              <div className="consent-details-content">
                {/* Basic Info */}
                <div className="detail-section">
                  <Text appearance="body-m-bold" color="primary-grey-80">
                    📋 Consent Information
                  </Text>
                  <div className="detail-grid">
                    <div className="detail-item">
                      <Text appearance="body-xs" color="primary-grey-60">Consent ID</Text>
                      <Text appearance="body-s" color="primary-grey-100">{selectedConsent.consentId}</Text>
                    </div>
                    <div className="detail-item">
                      <Text appearance="body-xs" color="primary-grey-60">Template Name</Text>
                      <Text appearance="body-s" color="primary-grey-100">{selectedConsent.templateName}</Text>
                    </div>
                    <div className="detail-item">
                      <Text appearance="body-xs" color="primary-grey-60">Template Version</Text>
                      <Text appearance="body-s" color="primary-grey-100">{selectedConsent.templateVersion}</Text>
                    </div>
                    <div className="detail-item">
                      <Text appearance="body-xs" color="primary-grey-60">Customer Identifier</Text>
                      <Text appearance="body-s" color="primary-grey-100">
                        {selectedConsent.customerIdentifiers?.type}: {selectedConsent.customerIdentifiers?.value}
                      </Text>
                    </div>
                  </div>
                </div>

                {/* Dates */}
                <div className="detail-section">
                  <Text appearance="body-m-bold" color="primary-grey-80">
                    📅 Timeline
                  </Text>
                  <div className="detail-grid">
                    <div className="detail-item">
                      <Text appearance="body-xs" color="primary-grey-60">Created At</Text>
                      <Text appearance="body-s" color="primary-grey-100">
                        {formatToIST(selectedConsent.createdAt)}
                      </Text>
                    </div>
                    <div className="detail-item">
                      <Text appearance="body-xs" color="primary-grey-60">Updated At</Text>
                      <Text appearance="body-s" color="primary-grey-100">
                        {formatToIST(selectedConsent.updatedAt)}
                      </Text>
                    </div>
                    <div className="detail-item">
                      <Text appearance="body-xs" color="primary-grey-60">Start Date</Text>
                      <Text appearance="body-s" color="primary-grey-100">
                        {formatToIST(selectedConsent.startDate)}
                      </Text>
                    </div>
                    <div className="detail-item">
                      <Text appearance="body-xs" color="primary-grey-60">End Date</Text>
                      <Text appearance="body-s" color="primary-grey-100">
                        {formatToIST(selectedConsent.endDate)}
                      </Text>
                    </div>
                  </div>
                </div>

                {/* Preferences */}
                {selectedConsent.preferences && selectedConsent.preferences.length > 0 && (
                  <div className="detail-section">
                    <Text appearance="body-m-bold" color="primary-grey-80">
                      🎯 Preferences
                    </Text>
                    {selectedConsent.preferences.map((pref, index) => (
                      <div key={pref.preferenceId || index} className="preference-card">
                        <div className="preference-header">
                          <Text appearance="body-s-bold" color="primary-grey-100">
                            {pref.mandatory ? "🔒 Mandatory" : "⚙️ Optional"} - {pref.preferenceStatus}
                          </Text>
                          <span className="preference-validity">
                            Valid for: {pref.preferenceValidity?.value} {pref.preferenceValidity?.unit}
                          </span>
                        </div>
                        
                        {/* Purposes */}
                        {pref.purposeList && pref.purposeList.length > 0 && (
                          <div className="purpose-list">
                            <Text appearance="body-xs" color="primary-grey-60">Purposes:</Text>
                            {pref.purposeList.map((purpose, idx) => (
                              <div key={purpose.purposeId || idx} className="purpose-item">
                                <Text appearance="body-xs-bold">{purpose.purposeInfo?.purposeName}</Text>
                                <Text appearance="body-xs" color="primary-grey-60">
                                  {purpose.purposeInfo?.purposeDescription}
                                </Text>
                              </div>
                            ))}
                          </div>
                        )}

                        {/* Processor Activities */}
                        {pref.processorActivityList && pref.processorActivityList.length > 0 && (
                          <div className="processor-list">
                            <Text appearance="body-xs" color="primary-grey-60">Processor Activities:</Text>
                            {pref.processorActivityList.map((activity, idx) => (
                              <div key={activity.processorActivityId || idx} className="processor-item">
                                <Text appearance="body-xs-bold">
                                  {activity.processActivityInfo?.activityName} - {activity.processActivityInfo?.processorName}
                                </Text>
                                <Text appearance="body-xs" color="primary-grey-60">
                                  {activity.processActivityInfo?.details}
                                </Text>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}

                {/* Multilingual Content */}
                {selectedConsent.multilingual && (
                  <div className="detail-section">
                    <Text appearance="body-m-bold" color="primary-grey-80">
                      🌐 Content
                    </Text>
                    <div className="multilingual-content">
                      {Object.entries(selectedConsent.multilingual.languageSpecificContentMap || {}).map(([lang, content]) => (
                        <div key={lang} className="language-section">
                          <Text appearance="body-s-bold" color="primary-grey-80">{lang}</Text>
                          <div className="content-item">
                            <Text appearance="body-xs" color="primary-grey-60">Description:</Text>
                            <Text appearance="body-xs">{content.description}</Text>
                          </div>
                          {content.rightsText && (
                            <div className="content-item">
                              <Text appearance="body-xs" color="primary-grey-60">Rights:</Text>
                              <Text appearance="body-xs">{content.rightsText}</Text>
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>
          ) : !searchQuery ? (
            <div className="search-empty-state">
              <Text appearance="heading-xs" color="primary-grey-80">
                Search Everything
              </Text>
              <Text appearance="body-s" color="primary-grey-60" style={{ marginTop: '8px' }}>
                Find templates, users, businesses, notifications, and more
              </Text>
              <div className="search-tips">
                <Text appearance="body-s-bold" color="primary-grey-80">
                  🔍 Quick Search Tips
                </Text>
                <ul>
                  <li>Search consent templates by name or ID</li>
                  <li>Search notification templates by event type</li>
                  <li>Search users by name, email, or role</li>
                  <li>Search businesses by name or description</li>
                  <li>Search consents by customer phone number</li>
                  <li>Search breaches by incident ID or type</li>
                  <li>Search audits by ID or component</li>
                </ul>
              </div>
            </div>
          ) : loading ? (
            <div className="search-loading">
              <Spinner size="medium" />
              <Text appearance="body-m" color="primary-grey-60">
                {loadingMessage}
              </Text>
              <Text appearance="body-xs" color="primary-grey-40">
                This may take a few seconds...
              </Text>
            </div>
          ) : getTotalResults() === 0 ? (
            <div className="search-empty-state">
              <Text appearance="body-m" color="primary-grey-60">
                No results found for "{searchQuery}"
              </Text>
              <Text appearance="body-xs" color="primary-grey-60">
                Try searching with different keywords
              </Text>
            </div>
          ) : (
            <div className="search-results">
              {/* Templates Results */}
              {(activeTab === "all" || activeTab === "templates") &&
                searchResults.templates.length > 0 && (
                  <div className="result-section">
                    <Text appearance="body-s-bold" color="primary-grey-80">
                      📄 Consent Templates
                    </Text>
                    {searchResults.templates.slice(0, 5).map((template) => (
                      <div
                        key={template.templateId}
                        className="result-item"
                        onClick={() => handleResultClick("template", template)}
                      >
                        <div className="result-content">
                          <Text appearance="body-s-bold" color="primary-grey-100">
                            {template.templateName}
                          </Text>
                          <Text appearance="body-xs" color="primary-grey-60">
                            ID: {template.templateId} • Version: {template.version}
                          </Text>
                        </div>
                        <div className="result-badge">
                          {template.status}
                        </div>
                      </div>
                    ))}
                  </div>
                )}

              {/* Grievances Results */}
              {(activeTab === "all" || activeTab === "grievances") &&
                searchResults.grievances.length > 0 && (
                  <div className="result-section">
                    <Text appearance="body-s-bold" color="primary-grey-80">
                      📋 Grievance Templates
                    </Text>
                    {searchResults.grievances.slice(0, 5).map((grievance) => (
                      <div
                        key={grievance.grievanceTemplateId}
                        className="result-item"
                        onClick={() => handleResultClick("grievance", grievance)}
                      >
                        <div className="result-content">
                          <Text appearance="body-s-bold" color="primary-grey-100">
                            {grievance.grievanceTemplateName}
                          </Text>
                          <Text appearance="body-xs" color="primary-grey-60">
                            ID: {grievance.grievanceTemplateId}
                          </Text>
                        </div>
                        <div className="result-badge">
                          {grievance.status || "Active"}
                        </div>
                      </div>
                    ))}
                  </div>
                )}

              {/* Consent Results */}
              {(activeTab === "all" || activeTab === "consents") &&
                searchResults.consents.length > 0 && (
                  <div className="result-section">
                    <Text appearance="body-s-bold" color="primary-grey-80">
                      👤 Consents by Customer
                    </Text>
                    {searchResults.consents.slice(0, 5).map((consent, index) => (
                      <div
                        key={consent.consentId || index}
                        className="result-item"
                        onClick={() => handleResultClick("consent", consent)}
                      >
                        <div className="result-content">
                          <Text appearance="body-s-bold" color="primary-grey-100">
                            {consent.templateName} - {consent.customerIdentifiers?.value || consent.customerIdentifier || "Customer"}
                          </Text>
                          <Text appearance="body-xs" color="primary-grey-60">
                            ID: {consent.consentId?.substring(0, 8)}... • Version: {consent.templateVersion} • Created: {new Date(consent.createdAt).toLocaleDateString()}
                          </Text>
                        </div>
                        <div className={`result-badge status-${consent.status?.toLowerCase() || "active"}`}>
                          {consent.status || "ACTIVE"}
                        </div>
                      </div>
                    ))}
                  </div>
                )}

              {/* Breach Notifications Results */}
              {(activeTab === "all" || activeTab === "breaches") &&
                searchResults.breaches.length > 0 && (
                  <div className="result-section">
                    <Text appearance="body-s-bold" color="primary-grey-80">
                      🔴 Breach Notifications
                    </Text>
                    {searchResults.breaches.slice(0, 5).map((breach, index) => (
                      <div
                        key={breach.id || breach.incidentId || index}
                        className="result-item"
                        onClick={() => handleResultClick("breach", breach)}
                      >
                        <div className="result-content">
                          <Text appearance="body-s-bold" color="primary-grey-100">
                            {breach.incidentId || `Breach ${index + 1}`}
                          </Text>
                          <Text appearance="body-xs" color="primary-grey-60">
                            Type: {breach.incidentDetails?.breachType || "N/A"} • {breach.incidentDetails?.briefDescription?.substring(0, 50) || "No description"}...
                          </Text>
                        </div>
                        <div className={`result-badge status-${breach.status?.toLowerCase() || "investigation"}`}>
                          {breach.status || "Investigation"}
                        </div>
                      </div>
                    ))}
                  </div>
                )}

              {/* Audit Reports Results */}
              {(activeTab === "all" || activeTab === "audits") &&
                searchResults.audits.length > 0 && (
                  <div className="result-section">
                    <Text appearance="body-s-bold" color="primary-grey-80">
                      📈 Audit Reports
                    </Text>
                    {searchResults.audits.slice(0, 5).map((audit, index) => (
                      <div
                        key={audit.auditId || audit.id || index}
                        className="result-item"
                        onClick={() => handleResultClick("audit", audit)}
                      >
                        <div className="result-content">
                          <Text appearance="body-s-bold" color="primary-grey-100">
                            {audit.auditId || audit.id || `Audit ${index + 1}`}
                          </Text>
                          <Text appearance="body-xs" color="primary-grey-60">
                            {audit.group || audit.component} • {audit.actionType} • {audit.initiator || "N/A"}
                          </Text>
                        </div>
                        <div className="result-badge">
                          {audit.status || "Active"}
                        </div>
                      </div>
                    ))}
                  </div>
                )}

              {/* Notification Templates Results */}
              {(activeTab === "all" || activeTab === "notifications") &&
                searchResults.notificationTemplates.length > 0 && (
                  <div className="result-section">
                    <Text appearance="body-s-bold" color="primary-grey-80">
                      📧 Notification Templates
                    </Text>
                    {searchResults.notificationTemplates.slice(0, 5).map((template, index) => (
                      <div
                        key={template.id || template.templateId || index}
                        className="result-item"
                        onClick={() => handleResultClick("notification", template)}
                      >
                        <div className="result-content">
                          <Text appearance="body-s-bold" color="primary-grey-100">
                            {template.eventType || "Notification Template"}
                          </Text>
                          <Text appearance="body-xs" color="primary-grey-60">
                            Channel: {template.channel} • {template.channel === 'EMAIL' ? template.emailConfig?.subject : template.smsConfig?.dltTemplateId || "N/A"}
                          </Text>
                        </div>
                        <div className={`result-badge status-${template.channel?.toLowerCase()}`}>
                          {template.channel}
                        </div>
                      </div>
                    ))}
                  </div>
                )}

              {/* Users Results */}
              {(activeTab === "all" || activeTab === "users") &&
                searchResults.users.length > 0 && (
                  <div className="result-section">
                    <Text appearance="body-s-bold" color="primary-grey-80">
                      👥 Users
                    </Text>
                    {searchResults.users.slice(0, 5).map((user, index) => (
                      <div
                        key={user.userId || user.id || index}
                        className="result-item"
                        onClick={() => handleResultClick("user", user)}
                      >
                        <div className="result-content">
                          <Text appearance="body-s-bold" color="primary-grey-100">
                            {user.name || user.email || "User"}
                          </Text>
                          <Text appearance="body-xs" color="primary-grey-60">
                            {user.email} • Role: {user.role || "N/A"}
                          </Text>
                        </div>
                        <div className={`result-badge status-${user.status?.toLowerCase() || "active"}`}>
                          {user.status || "Active"}
                        </div>
                      </div>
                    ))}
                  </div>
                )}

              {/* Businesses Results */}
              {(activeTab === "all" || activeTab === "businesses") &&
                searchResults.businesses.length > 0 && (
                  <div className="result-section">
                    <Text appearance="body-s-bold" color="primary-grey-80">
                      🏢 Businesses
                    </Text>
                    {searchResults.businesses.slice(0, 5).map((business, index) => (
                      <div
                        key={business.businessId || business.id || index}
                        className="result-item"
                        onClick={() => handleResultClick("business", business)}
                      >
                        <div className="result-content">
                          <Text appearance="body-s-bold" color="primary-grey-100">
                            {business.businessName || "Business"}
                          </Text>
                          <Text appearance="body-xs" color="primary-grey-60">
                            ID: {business.businessId} • {business.description || "No description"}
                          </Text>
                        </div>
                        <div className={`result-badge status-${business.status?.toLowerCase() || "active"}`}>
                          {business.status || "Active"}
                        </div>
                      </div>
                    ))}
                  </div>
                )}

            </div>
          )}
        </div>

        {/* Keyboard Shortcuts Help */}
        <div className="search-footer">
          <Text appearance="body-xs" color="primary-grey-60">
            Press <kbd>ESC</kbd> to close • <kbd>↑</kbd><kbd>↓</kbd> to navigate
          </Text>
        </div>
      </div>
    </div>
  );
};

export default GlobalSearch;

