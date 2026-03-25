import { useState, useEffect, useCallback, useMemo } from "react";
import { Text, Tabs, TabItem, Icon, Button, BadgeV2, Image, ActionButton, Spinner } from "../custom-components";
import { useNavigate, useParams } from "react-router-dom";
import { useSelector } from "react-redux";
import { IcChevronLeft, IcSort, IcClose } from "../custom-components/Icon";
import "../styles/pageConfiguration.css";
import "../styles/templates.css";
import config from "../utils/config";
import { generateTransactionId } from "../utils/transactionId";
import { formatToIST } from "../utils/dateUtils";

const ConsentDeletionDetail = () => {
  const navigate = useNavigate();
  const { eventId } = useParams();
  const [activeTab, setActiveTab] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const dashboard = new URL("../assets/dashboard.svg", import.meta.url).href;

  // Redux selectors
  const tenantId = useSelector((state) => state.common.tenant_id);
  const businessId = useSelector((state) => state.common.business_id);
  const sessionToken = useSelector((state) => state.common.session_token);

  // State for consent detail modal
  const [showConsentModal, setShowConsentModal] = useState(false);
  const [consentDetailLoading, setConsentDetailLoading] = useState(false);
  const [consentDetailData, setConsentDetailData] = useState(null);
  const [consentDetailError, setConsentDetailError] = useState(null);

  // State for API data
  const [consentInfo, setConsentInfo] = useState({
    consentId: "",
    trigger: "",
    template: "",
    timestamp: "",
    retentionPolicy: "",
  });

  const [dataFiduciary, setDataFiduciary] = useState({
    systems: [],
    overallStatus: "",
    executedAt: "",
  });

  const [dataProcessors, setDataProcessors] = useState([]);
  const [piiItems, setPiiItems] = useState([]);
  const [notifications, setNotifications] = useState([]);

  // Fetch consent by consent ID
  const fetchConsentById = useCallback(async (consentId) => {
    if (!tenantId || !sessionToken || !consentId) {
      console.warn("Missing required parameters for consent fetch");
      return;
    }

    try {
      setConsentDetailLoading(true);
      setConsentDetailError(null);
      setShowConsentModal(true);

      const txnId = generateTransactionId();
      const authToken = sessionToken?.startsWith('Bearer ') ? sessionToken : `Bearer ${sessionToken || ''}`;

      const response = await fetch(`${config.consent_search}?consentId=${encodeURIComponent(consentId)}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'tenant-id': tenantId,
          'txn': txnId,
          'x-session-token': authToken,
        },
      });

      if (!response.ok) {
        throw new Error(`API error: ${response.status} ${response.statusText}`);
      }

      const responseData = await response.json();
      console.log("Consent details API response:", responseData);

      // Handle response - could be in searchList or data array
      const consents = responseData?.searchList || responseData?.data || [];
      if (consents.length > 0) {
        setConsentDetailData(consents[0]);
      } else {
        setConsentDetailError("No consent found with this ID");
      }
    } catch (err) {
      console.error("Error fetching consent details:", err);
      setConsentDetailError(err.message);
    } finally {
      setConsentDetailLoading(false);
    }
  }, [tenantId, sessionToken]);

  // Close consent modal
  const closeConsentModal = () => {
    setShowConsentModal(false);
    setConsentDetailData(null);
    setConsentDetailError(null);
  };

  // Fetch business application name (DF name)
  const fetchBusinessApplicationName = useCallback(async () => {
    if (!tenantId || !businessId || !sessionToken) {
      return null;
    }

    try {
      const txnId = generateTransactionId();
      const authToken = sessionToken?.startsWith('Bearer ') ? sessionToken : `Bearer ${sessionToken || ''}`;

      const response = await fetch(`${config.business_application_search}?businessId=${businessId}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'tenant-id': tenantId,
          'txn': txnId,
          'x-session-token': authToken,
        },
      });

      if (!response.ok) {
        console.warn("Failed to fetch business application name");
        return null;
      }

      const data = await response.json();
      const businessName = data?.searchList?.[0]?.name || null;
      return businessName;
    } catch (err) {
      console.error("Error fetching business application name:", err);
      return null;
    }
  }, [tenantId, businessId, sessionToken]);

  // Fetch consent deletion details from API
  const fetchConsentDeletionDetails = useCallback(async () => {
    if (!tenantId || !businessId || !sessionToken || !eventId) {
      console.warn("Missing required parameters for API call");
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const txnId = generateTransactionId();
      const authToken = sessionToken?.startsWith('Bearer ') ? sessionToken : `Bearer ${sessionToken || ''}`;

      const response = await fetch(config.consent_deletion_details(eventId), {
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
      console.log("Consent deletion details API response:", responseData);

      if (responseData?.success && responseData?.data) {
        const data = responseData.data;

        // Map consentInformation
        if (data.consentInformation) {
          const ci = data.consentInformation;
          setConsentInfo({
            consentId: ci.consentId || "",
            trigger: ci.trigger || "",
            template: ci.template || "",
            timestamp: ci.timestamp ? formatToIST(ci.timestamp) : "",
            retentionPolicy: ci.retentionPolicy 
              ? `${ci.retentionPolicy.value} ${ci.retentionPolicy.unit}`
              : "",
          });
        }

        // Map dataFiduciary
        if (data.dataFiduciary) {
          const df = data.dataFiduciary;
          setDataFiduciary({
            systems: df.systems || [],
            overallStatus: df.overallStatus || "",
            executedAt: df.executedAt ? formatToIST(df.executedAt) : "",
          });
        }

        // Map dataProcessors
        let processorsData = [];
        if (data.dataProcessors && Array.isArray(data.dataProcessors)) {
          processorsData = data.dataProcessors.map(dp => ({
            processorId: dp.processorId || "",
            processorName: dp.processorName || "",
            status: dp.status || "",
            reason: dp.reason || "",
            reviewDate: dp.reviewDate ? formatToIST(dp.reviewDate) : "",
            reviewDateRaw: dp.reviewDate || "",
            executedAt: dp.executedAt ? formatToIST(dp.executedAt) : "",
            executedAtRaw: dp.executedAt || "",
            deletedAt: dp.deletedAt ? formatToIST(dp.deletedAt) : "",
            deletedAtRaw: dp.deletedAt || "",
          }));
          setDataProcessors(processorsData);
        }

        // Map piiItems and group duplicates by piiItem
        if (data.piiItems && Array.isArray(data.piiItems)) {
          // Group by piiItem
          const groupedPiiItems = {};
          
          data.piiItems.forEach(pii => {
            const piiItemKey = pii.piiItem || "";
            
            if (!groupedPiiItems[piiItemKey]) {
              groupedPiiItems[piiItemKey] = {
                piiItem: piiItemKey,
                sourceSystems: [],
                location: pii.location || "",
                status: pii.status || "",
                deferredReasonType: pii.deferredReasonType || "",
                deferredReasonMessage: pii.deferredReasonMessage || "",
                consentIds: pii.consentIds || [],
              };
            }
            
            // Add sourceSystem if not already present
            const sourceSystem = pii.sourceSystem || "";
            if (sourceSystem && !groupedPiiItems[piiItemKey].sourceSystems.includes(sourceSystem)) {
              groupedPiiItems[piiItemKey].sourceSystems.push(sourceSystem);
            }
            
            // If statuses differ, keep the first one (or you could combine them)
            // If locations differ, keep the first one
            // If deferredReasonMessage differs, combine them
            if (pii.deferredReasonMessage && 
                groupedPiiItems[piiItemKey].deferredReasonMessage && 
                !groupedPiiItems[piiItemKey].deferredReasonMessage.includes(pii.deferredReasonMessage)) {
              groupedPiiItems[piiItemKey].deferredReasonMessage += `; ${pii.deferredReasonMessage}`;
            } else if (pii.deferredReasonMessage && !groupedPiiItems[piiItemKey].deferredReasonMessage) {
              groupedPiiItems[piiItemKey].deferredReasonMessage = pii.deferredReasonMessage;
            }
          });
          
          // Convert grouped object to array
          const groupedArray = Object.values(groupedPiiItems);
          setPiiItems(groupedArray);
        }

        // Fetch business application name (DF name) for DF Deletion Completed label
        const dfName = await fetchBusinessApplicationName();
        
        // Map notificationTimeline and add deleted processor entries
        const timelineEntries = [];
        
        // Get deleted processors for label updates
        const deletedProcessors = processorsData && processorsData.length > 0
          ? processorsData.filter(dp => dp.status && dp.status.toUpperCase() === "DELETED")
          : [];
        
        // Add existing notification timeline entries with label updates
        if (data.notificationTimeline && Array.isArray(data.notificationTimeline)) {
          data.notificationTimeline.forEach(nt => {
            let eventLabel = nt.label || "";
            
            // Rename "DF Deletion Completed" to "Data Fiduciary: Data Deletion : <DF Name>"
            if (eventLabel === "DF Deletion Completed") {
              if (dfName) {
                eventLabel = `Data Fiduciary: Data Deletion : ${dfName}`;
              } else {
                eventLabel = `Data Fiduciary: Data Deletion`;
              }
            } 
            // Rename "User Notified" to "User Notified of Data Deletion"
            else if (eventLabel === "User Notified") {
              eventLabel = "User Notified of Data Deletion";
            }
            // Rename "Deletion Triggered" to "Data Deletion Request Raised"
            else if (eventLabel === "Deletion Triggered") {
              eventLabel = "Data Deletion Request Raised";
            }

            // Handle "Processor Updates" - create separate entry for each deleted processor
            if (nt.label === "Processor Updates" && deletedProcessors.length > 0) {
              deletedProcessors.forEach((processor, procIndex) => {
                const processorTimestamp = processor.reviewDateRaw || processor.executedAtRaw || processor.deletedAtRaw;
                const processorDate = processor.reviewDate || processor.executedAt || processor.deletedAt;
                
                timelineEntries.push({
                  eventType: `Data Processor: Data Deletion : ${processor.processorName}`,
                  dateTime: processorDate || (processorTimestamp ? formatToIST(processorTimestamp) : (nt.timestamp ? formatToIST(nt.timestamp) : "")),
                  order: (nt.order || 0) + (procIndex * 0.1),
                  timestamp: processorTimestamp || nt.timestamp || "",
                });
              });
            } else {
              timelineEntries.push({
                eventType: eventLabel,
                dateTime: nt.timestamp ? formatToIST(nt.timestamp) : "",
                order: nt.order || 0,
                timestamp: nt.timestamp || "",
              });
            }
          });
        }
        
        // Add entries for each deleted processor (if not already in timeline as "Processor: Update")
        // Note: "Processor: Update" entries are already added above, so we only add "Data Deleted by" entries
        // if they don't have a corresponding "Processor: Update" entry
        if (deletedProcessors && deletedProcessors.length > 0) {
          deletedProcessors.forEach((processor, index) => {
            // Get deletion date from executedAtRaw, deletedAtRaw, or reviewDateRaw (raw timestamps)
            // Use formatted dates for display
            const deletionTimestamp = processor.executedAtRaw || processor.deletedAtRaw || processor.reviewDateRaw;
            const deletionDate = processor.executedAt || processor.deletedAt || processor.reviewDate;
            
            // Check if this processor already has a "Processor: Update" entry in the timeline
            const hasProcessorUpdateEntry = timelineEntries.some(entry => {
              return entry.eventType && entry.eventType.includes(`Processor: Update : ${processor.processorName}`);
            });
            
            // Check if this processor deletion is already in the timeline (by timestamp)
            const alreadyInTimeline = timelineEntries.some(entry => {
              if (entry.timestamp && deletionTimestamp) {
                const timeDiff = Math.abs(new Date(entry.timestamp) - new Date(deletionTimestamp));
                return timeDiff < 60000; // 1 minute tolerance
              }
              return false;
            });
            
            // Only add "Data Deleted by" entry if there's no "Processor: Update" entry and it's not already in timeline
            if (deletionTimestamp && !hasProcessorUpdateEntry && !alreadyInTimeline) {
              timelineEntries.push({
                eventType: `Data Deleted by ${processor.processorName}`,
                dateTime: deletionDate || (deletionTimestamp ? formatToIST(deletionTimestamp) : ""),
                order: 1000 + index, // Place after regular timeline entries
                timestamp: deletionTimestamp,
                processorName: processor.processorName,
              });
            }
          });
        }
        
        // Sort all timeline entries by timestamp (or order if timestamp not available)
        const sortedTimeline = timelineEntries.sort((a, b) => {
          if (a.timestamp && b.timestamp) {
            return new Date(a.timestamp) - new Date(b.timestamp);
          }
          return (a.order || 0) - (b.order || 0);
        });
        
        setNotifications(sortedTimeline.map(entry => ({
          eventType: entry.eventType,
          dateTime: entry.dateTime,
          order: entry.order || 0,
        })));
      } else {
        setError("No data found for this event");
      }
    } catch (err) {
      console.error("Error fetching consent deletion details:", err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [tenantId, businessId, sessionToken, eventId, fetchBusinessApplicationName]);

  // Fetch data on mount
  useEffect(() => {
    if (tenantId && businessId && sessionToken && eventId) {
      fetchConsentDeletionDetails();
    }
  }, [tenantId, businessId, sessionToken, eventId, fetchConsentDeletionDetails]);

  // PII table sorting
  const [piiSortColumn, setPiiSortColumn] = useState(null);
  const [piiSortDirection, setPiiSortDirection] = useState('asc');

  const handlePiiSort = (column) => {
    if (piiSortColumn === column) {
      setPiiSortDirection(piiSortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setPiiSortColumn(column);
      setPiiSortDirection('asc');
    }
  };

  const sortedPiiItems = useMemo(() => {
    if (!piiSortColumn) return piiItems;
    
    return [...piiItems].sort((a, b) => {
      let aValue;
      let bValue;
      
      // Handle sourceSystems array for sorting
      if (piiSortColumn === 'sourceSystem') {
        aValue = Array.isArray(a.sourceSystems) ? a.sourceSystems.join(', ') : (a.sourceSystem || '');
        bValue = Array.isArray(b.sourceSystems) ? b.sourceSystems.join(', ') : (b.sourceSystem || '');
      } else {
        aValue = a[piiSortColumn] || '';
        bValue = b[piiSortColumn] || '';
      }

      if (aValue < bValue) return piiSortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return piiSortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }, [piiItems, piiSortColumn, piiSortDirection]);

  // Notification table sorting
  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState('asc');

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

  const sortedNotifications = useMemo(() => {
    if (!sortColumn) return notifications;
    
    return [...notifications].sort((a, b) => {
      let aValue = a[sortColumn] || '';
      let bValue = b[sortColumn] || '';

      if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }, [notifications, sortColumn, sortDirection]);

  // Helper function to get status badge
  const getStatusBadge = (status) => {
    const statusUpper = (status || '').toUpperCase();
    
    const badgeStyle = {
      display: 'inline-block',
      padding: '4px 12px',
      borderRadius: '6px',
      fontSize: '12px',
      fontWeight: '500',
      textAlign: 'center',
    };

    switch (statusUpper) {
      case "DELETED":
      case "PSEUDONYMIZED":
      case "COMPLETED":
      case "ACKNOWLEDGED":
        return (
          <span
            style={{
              ...badgeStyle,
              backgroundColor: '#D1FAE5',
              color: '#065F46',
            }}
          >
            {status}
          </span>
        );
      case "DEFERRED":
      case "PENDING":
        return (
          <span
            style={{
              ...badgeStyle,
              backgroundColor: '#FED7AA',
              color: '#C2410C',
            }}
          >
            {status}
          </span>
        );
      case "FAILED":
        return (
          <span
            style={{
              ...badgeStyle,
              backgroundColor: '#FEE2E2',
              color: '#DC2626',
            }}
          >
            {status}
          </span>
        );
      default:
        return (
          <span
            style={{
              ...badgeStyle,
              backgroundColor: '#F3F4F6',
              color: '#6B7280',
            }}
          >
            {status}
          </span>
        );
    }
  };

  const renderKeyValue = (label, value) => (
    <div style={{ marginBottom: "16px" }}>
      <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "4px" }}>
        {label}: 
      </Text>
      <Text appearance="body-s" color="primary-grey-80" style={{ fontWeight: "500" }}>
        {" "}{value || "—"}
      </Text>
    </div>
  );

  if (loading) {
    return (
      <div className="configurePage">
        <div className="configure-template-page">
          <div className="template-img">
            <Image src={dashboard}></Image>
          </div>
          <Text color="primary-grey-80" style={{ marginTop: "0.5rem" }}>
            Loading consent deletion details...
          </Text>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="configurePage">
        <div style={{ display: "flex", alignItems: "center", gap: "16px", marginBottom: "24px" }}>
          <Button
            kind="tertiary"
            onClick={() => navigate("/data-deletion-purge-dashboard")}
            style={{
              width: "40px",
              height: "40px",
              borderRadius: "50%",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              padding: 0,
              minWidth: "40px",
            }}
          >
            <Icon ic={<IcChevronLeft />} size="medium" color="primary_grey_80" />
          </Button>
          <Text appearance="heading-xs" color="primary-grey-100" style={{ fontWeight: "600" }}>
            Error
          </Text>
        </div>
        <div className="configure-template-page">
          <div className="template-img">
            <Image src={dashboard}></Image>
          </div>
          <Text color="primary-grey-80" style={{ marginTop: "0.5rem" }}>
            Error loading details: {error}
          </Text>
        </div>
      </div>
    );
  }

  return (
    <div className="configurePage">
      <style>
        {`
          .consent-detail-tabs [role="tablist"] {
            border-bottom: 1px solid #E5E7EB;
          }
          .consent-modal-scrollable::-webkit-scrollbar {
            display: none;
          }
        `}
      </style>
      {/* Header with back button and title */}
      <div style={{ display: "flex", alignItems: "center", gap: "16px", marginBottom: "24px" }}>
        <Button
          kind="tertiary"
          onClick={() => navigate("/data-deletion-purge-dashboard")}
          style={{
            width: "40px",
            height: "40px",
            borderRadius: "50%",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            padding: 0,
            minWidth: "40px",
          }}
        >
          <Icon ic={<IcChevronLeft />} size="medium" color="primary_grey_80" />
        </Button>
        <Text appearance="heading-xs" color="primary-grey-100" style={{ fontWeight: "600" }}>
          {consentInfo.consentId || eventId}
        </Text>
      </div>

      {/* Tabs */}
      <div className="consent-detail-tabs" style={{ marginBottom: "24px" , marginLeft:'3%', marginRight:'3%' }}>
        <Tabs
          activeKey={activeTab}
          onTabChange={(index) => setActiveTab(index)}
        >
          <TabItem
            label={
              <Text
                appearance="body-xs"
                color={activeTab === 0 ? "primary" : "primary-grey-80"}
              >
                Consent Information
              </Text>
            }
          >
            <div style={{ padding: "24px", backgroundColor: "#FFFFFF", borderRadius: "8px" }}>
              {/* Clickable Consent ID */}
              <div style={{ marginBottom: "16px" }}>
                <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "4px" }}>
                  Consent ID: 
                </Text>
                {consentInfo.consentId ? (
                  <span
                    onClick={() => fetchConsentById(consentInfo.consentId)}
                    style={{
                      color: "#0F3CC9",
                      textDecoration: "underline",
                      cursor: "pointer",
                      fontWeight: "500",
                    }}
                  >
                    {" "}{consentInfo.consentId}
                  </span>
                ) : (
                  <Text appearance="body-s" color="primary-grey-80" style={{ fontWeight: "500" }}>
                    {" "}—
                  </Text>
                )}
              </div>
              {renderKeyValue("Trigger", consentInfo.trigger)}
              {renderKeyValue("Template", consentInfo.template)}
              {renderKeyValue("Time stamp", consentInfo.timestamp)}
              {renderKeyValue("Retention policy", consentInfo.retentionPolicy)}
            </div>
          </TabItem>

          <TabItem
            label={
              <Text
                appearance="body-xs"
                color={activeTab === 1 ? "primary" : "primary-grey-80"}
              >
                Data Fiduciary
              </Text>
            }
          >
            <div style={{ padding: "24px", backgroundColor: "#FFFFFF", borderRadius: "8px" }}>
              {renderKeyValue("Overall Status", dataFiduciary.overallStatus)}
              {renderKeyValue("Executed At", dataFiduciary.executedAt)}
              
              {dataFiduciary.systems && dataFiduciary.systems.length > 0 && (
                <>
                  <Text 
                    appearance="body-s-bold" 
                    color="primary-grey-100" 
                    style={{ marginTop: "24px", marginBottom: "16px", display: "block" }}
                  >
                    Systems
                  </Text>
                  {dataFiduciary.systems.map((system, index) => (
                    <div 
                      key={index} 
                      style={{ 
                        marginBottom: index < dataFiduciary.systems.length - 1 ? "24px" : "0",
                        paddingBottom: index < dataFiduciary.systems.length - 1 ? "24px" : "0",
                        borderBottom: index < dataFiduciary.systems.length - 1 ? "1px solid #E5E7EB" : "none",
                        backgroundColor: "#F9FAFB",
                        padding: "16px",
                        borderRadius: "8px",
                        marginTop: "8px"
                      }}
                    >
                      {renderKeyValue("System ID", system.systemId)}
                      {renderKeyValue("Deletion Type", system.deletionType)}
                      {system.yamlMappings && renderKeyValue("YAML Mappings", system.yamlMappings)}
                      <div style={{ marginBottom: "16px" }}>
                        <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "4px" }}>
                          Status: 
                        </Text>
                        {" "}{getStatusBadge(system.status)}
                      </div>
                      {renderKeyValue("Executed At", system.executedAt ? formatToIST(system.executedAt) : "—")}
                      {system.deferredReason && renderKeyValue("Deferred Reason", system.deferredReason)}
                    </div>
                  ))}
                </>
              )}
            </div>
          </TabItem>

          <TabItem
            label={
              <Text
                appearance="body-xs"
                color={activeTab === 2 ? "primary" : "primary-grey-80"}
              >
                Data processor
              </Text>
            }
          >
            <div style={{ padding: "24px", backgroundColor: "#FFFFFF", borderRadius: "8px" }}>
              {dataProcessors.length === 0 ? (
                <Text appearance="body-s" color="primary-grey-80">
                  No data processors found
                </Text>
              ) : (
                dataProcessors.map((processor, index) => (
                  <div 
                    key={index} 
                    style={{ 
                      marginBottom: index < dataProcessors.length - 1 ? "32px" : "0",
                      paddingBottom: index < dataProcessors.length - 1 ? "32px" : "0",
                      borderBottom: index < dataProcessors.length - 1 ? "1px solid #E5E7EB" : "none"
                    }}
                  >
                    <Text 
                      appearance="body-s-bold" 
                      color="primary-grey-100" 
                      style={{ marginBottom: "16px", display: "block" }}
                    >
                      Data processor - {processor.processorName}
                    </Text>
                    {renderKeyValue("Processor ID", processor.processorId)}
                    <div style={{ marginBottom: "16px" }}>
                      <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "4px" }}>
                        Status: 
                      </Text>
                      {" "}{getStatusBadge(processor.status)}
                    </div>
                    {processor.reason && renderKeyValue("Reason", processor.reason)}
                    {processor.reviewDate && renderKeyValue("Review Date", processor.reviewDate)}
                  </div>
                ))
              )}
            </div>
          </TabItem>

          <TabItem
            label={
              <Text
                appearance="body-xs"
                color={activeTab === 3 ? "primary" : "primary-grey-80"}
              >
                PII item
              </Text>
            }
          >
            <div style={{ padding: "24px", backgroundColor: "#FFFFFF", borderRadius: "8px" }}>
              {piiItems.length === 0 ? (
                <Text appearance="body-s" color="primary-grey-80">
                  No PII items found
                </Text>
              ) : (
                <table style={{ width: "100%", borderCollapse: "collapse" }}>
                  <thead>
                    <tr style={{ borderBottom: "1px solid #E5E7EB" }}>
                      <th
                        onClick={() => handlePiiSort('piiItem')}
                        style={{
                          cursor: 'pointer',
                          padding: "12px 16px",
                          textAlign: "left",
                          borderBottom: "1px solid #E5E7EB",
                          backgroundColor: "#F5F5F5"
                        }}
                      >
                        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            PII item
                          </Text>
                          {renderSortIcon('piiItem')}
                        </div>
                      </th>
                      <th
                        onClick={() => handlePiiSort('sourceSystem')}
                        style={{
                          cursor: 'pointer',
                          padding: "12px 16px",
                          textAlign: "left",
                          borderBottom: "1px solid #E5E7EB",
                          backgroundColor: "#F5F5F5"
                        }}
                      >
                        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Source System
                          </Text>
                          {renderSortIcon('sourceSystem')}
                        </div>
                      </th>
                      <th
                        onClick={() => handlePiiSort('location')}
                        style={{
                          cursor: 'pointer',
                          padding: "12px 16px",
                          textAlign: "left",
                          borderBottom: "1px solid #E5E7EB",
                          backgroundColor: "#F5F5F5"
                        }}
                      >
                        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Location
                          </Text>
                          {renderSortIcon('location')}
                        </div>
                      </th>
                      <th
                        onClick={() => handlePiiSort('status')}
                        style={{
                          cursor: 'pointer',
                          padding: "12px 16px",
                          textAlign: "left",
                          borderBottom: "1px solid #E5E7EB",
                          backgroundColor: "#F5F5F5"
                        }}
                      >
                        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Status
                          </Text>
                          {renderSortIcon('status')}
                        </div>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {sortedPiiItems.map((item, index) => (
                      <tr 
                        key={index} 
                        style={{ borderBottom: index < sortedPiiItems.length - 1 ? "1px solid #E5E7EB" : "none" }}
                        title={item.deferredReasonMessage || ''}
                      >
                        <td style={{ padding: "12px 16px" }}>
                          <Text appearance="body-xs" color="black">
                            {item.piiItem}
                          </Text>
                        </td>
                        <td style={{ padding: "12px 16px" }}>
                          {Array.isArray(item.sourceSystems) && item.sourceSystems.length > 0 ? (
                            <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                              {item.sourceSystems.map((sourceSystem, sysIdx) => (
                                <Text 
                                  key={sysIdx}
                                  appearance="body-xs" 
                                  color="black" 
                                  style={{ fontSize: "11px", wordBreak: "break-all" }}
                                >
                                  {sourceSystem}
                                </Text>
                              ))}
                            </div>
                          ) : (
                            <Text appearance="body-xs" color="black" style={{ fontSize: "11px", wordBreak: "break-all" }}>
                              {item.sourceSystem || "—"}
                            </Text>
                          )}
                        </td>
                        <td style={{ padding: "12px 16px" }}>
                          <Text appearance="body-xs" color="black">
                            {item.location}
                          </Text>
                        </td>
                        <td style={{ padding: "12px 16px" }}>
                          {getStatusBadge(item.status)}
                          {item.deferredReasonMessage && (
                            <div style={{ marginTop: "4px" }}>
                              <Text appearance="body-xxs" color="primary-grey-60" style={{ fontSize: "10px" }}>
                                {item.deferredReasonMessage}
                              </Text>
                            </div>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </TabItem>

          <TabItem
            label={
              <Text
                appearance="body-xs"
                color={activeTab === 4 ? "primary" : "primary-grey-80"}
              >
                Notification timeline
              </Text>
            }
          >
            <div style={{ padding: "24px", backgroundColor: "#FFFFFF", borderRadius: "8px" }}>
              {notifications.length === 0 ? (
                <Text appearance="body-s" color="primary-grey-80">
                  No notifications found
                </Text>
              ) : (
                <table style={{ width: "100%", borderCollapse: "collapse" }}>
                  <thead>
                    <tr style={{ borderBottom: "1px solid #E5E7EB" }}>
                      <th
                        onClick={() => handleSort('eventType')}
                        style={{
                          cursor: 'pointer',
                          padding: "12px 16px",
                          textAlign: "left",
                          borderBottom: "1px solid #E5E7EB",
                          backgroundColor: "#F5F5F5"
                        }}
                      >
                        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Event type
                          </Text>
                          {renderSortIcon('eventType')}
                        </div>
                      </th>
                      <th
                        onClick={() => handleSort('dateTime')}
                        style={{
                          cursor: 'pointer',
                          padding: "12px 16px",
                          textAlign: "left",
                          borderBottom: "1px solid #E5E7EB",
                          backgroundColor: "#F5F5F5"
                        }}
                      >
                        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Date and time
                          </Text>
                          {renderSortIcon('dateTime')}
                        </div>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {sortedNotifications.map((item, index) => (
                      <tr key={index} style={{ borderBottom: index < sortedNotifications.length - 1 ? "1px solid #E5E7EB" : "none" }}>
                        <td style={{ padding: "12px 16px" }}>
                          <Text appearance="body-xs" color="black">
                            {item.eventType}
                          </Text>
                        </td>
                        <td style={{ padding: "12px 16px" }}>
                          <Text appearance="body-xs" color="black">
                            {item.dateTime}
                          </Text>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </TabItem>
        </Tabs>
      </div>

      {/* Consent Detail Modal */}
      {showConsentModal && (
        <div className="modal-outer-container">
          <div 
            className="master-set-up-modal-container consent-modal-scrollable"
            style={{ 
              maxWidth: "600px", 
              width: "90%",
              maxHeight: "85vh",
              overflow: "auto",
              scrollbarWidth: "none",
              msOverflowStyle: "none",
            }}
          >
            <div className="modal-close-btn-container">
              <ActionButton
                onClick={closeConsentModal}
                icon={<IcClose />}
                kind="tertiary"
              />
            </div>

            <Text appearance="heading-xs" color="primary-grey-100">
              Consent Details
            </Text>

            {/* Modal Content */}
            <div style={{ marginTop: "20px" }}>
              {consentDetailLoading ? (
                <div style={{ display: "flex", flexDirection: "column", alignItems: "center", padding: "40px 20px" }}>
                  <Spinner size="medium" />
                  <Text appearance="body-s" color="primary-grey-80" style={{ marginTop: "16px" }}>
                    Loading consent details...
                  </Text>
                </div>
              ) : consentDetailError ? (
                <div style={{ padding: "20px", textAlign: "center" }}>
                  <Text appearance="body-s" color="feedback-error-50">
                    Error: {consentDetailError}
                  </Text>
                </div>
              ) : consentDetailData ? (
                <div style={{ display: "flex", flexDirection: "column", gap: "15px" }}>
                  {/* Customer ID */}
                  <div>
                    <Text appearance="body-s-bold" color="primary-grey-100">
                      Customer ID:{" "}
                    </Text>
                    <Text appearance="body-s" color="primary-grey-80" style={{ marginTop: "5px" }}>
                      {(() => {
                        if (consentDetailData.customerIdentifiers) {
                          if (Array.isArray(consentDetailData.customerIdentifiers)) {
                            return consentDetailData.customerIdentifiers.map(ci => ci.value).join(", ") || "—";
                          }
                          return consentDetailData.customerIdentifiers.value || "—";
                        }
                        return consentDetailData.customerId || consentDetailData.customerIdentifier || "—";
                      })()}
                    </Text>
                  </div>

                  {/* Status */}
                  <div>
                    <Text appearance="body-s-bold" color="primary-grey-100">
                      Status:{" "}
                    </Text>
                    <span style={{ marginTop: "5px", display: "inline-block" }}>
                      {getStatusBadge(consentDetailData.status || consentDetailData.consentStatus || "ACTIVE")}
                    </span>
                  </div>

                  {/* Template Name */}
                  <div>
                    <Text appearance="body-s-bold" color="primary-grey-100">
                      Template Name:{" "}
                    </Text>
                    <Text appearance="body-s" color="primary-grey-80" style={{ marginTop: "5px" }}>
                      {consentDetailData.templateName || "—"}
                    </Text>
                  </div>

                  {/* Created Date */}
                  <div>
                    <Text appearance="body-s-bold" color="primary-grey-100">
                      Created Date:{" "}
                    </Text>
                    <Text appearance="body-s" color="primary-grey-80" style={{ marginTop: "5px" }}>
                      {consentDetailData.startDate || consentDetailData.createdAt
                        ? formatToIST(consentDetailData.startDate || consentDetailData.createdAt) 
                        : "—"}
                    </Text>
                  </div>

                  {/* Valid Until */}
                  <div>
                    <Text appearance="body-s-bold" color="primary-grey-100">
                      Valid Until:{" "}
                    </Text>
                    <Text appearance="body-s" color="primary-grey-80" style={{ marginTop: "5px" }}>
                      {consentDetailData.endDate || consentDetailData.expiryDate 
                        ? formatToIST(consentDetailData.endDate || consentDetailData.expiryDate) 
                        : "—"}
                    </Text>
                  </div>

                  {/* Preferences with Details - Based on API structure */}
                  {consentDetailData.preferences && consentDetailData.preferences.length > 0 && (
                    <div>
                      <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "10px", display: "block" }}>
                        Preferences:
                      </Text>
                      <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                        {consentDetailData.preferences.map((preference, prefIdx) => {
                          // Get purpose from purposeList
                          const purpose = preference.purposeList && preference.purposeList.length > 0 
                            ? preference.purposeList[0].purposeInfo 
                            : null;
                          
                          // Get duration from preferenceValidity
                          const duration = preference.preferenceValidity;
                          
                          // Get processor activities
                          const processorActivities = preference.processorActivityList || [];
                          
                          return (
                            <div 
                              key={prefIdx} 
                              style={{ 
                                padding: "12px 16px", 
                                backgroundColor: "#F9FAFB", 
                                borderRadius: "8px",
                                border: "1px solid #E5E7EB"
                              }}
                            >
                              {/* Purpose */}
                              {purpose && (
                                <div style={{ marginBottom: "8px" }}>
                                  <Text appearance="body-s-bold" color="primary-grey-100">
                                    Purpose:{" "}
                                  </Text>
                                  <Text appearance="body-s" color="primary-grey-80" style={{ marginTop: "4px" }}>
                                    {purpose.purposeName || purpose.name || "—"}
                                  </Text>
                                </div>
                              )}

                              {/* Duration */}
                              {duration && (
                                <div style={{ marginBottom: "8px" }}>
                                  <Text appearance="body-s-bold" color="primary-grey-100">
                                    Duration:{" "}
                                  </Text>
                                  <Text appearance="body-s" color="primary-grey-80" style={{ marginTop: "4px" }}>
                                    {duration.value && duration.unit 
                                      ? `${duration.value} ${duration.unit}`
                                      : "—"}
                                  </Text>
                                </div>
                              )}

                              {/* Processing Activities */}
                              {processorActivities.length > 0 && processorActivities.map((procActivity, actIdx) => {
                                const activityInfo = procActivity.processActivityInfo || procActivity;
                                const dataTypesList = activityInfo.dataTypesList || [];
                                
                                return (
                                  <div key={actIdx} style={{ marginBottom: actIdx < processorActivities.length - 1 ? "16px" : "0", 
                                    paddingBottom: actIdx < processorActivities.length - 1 ? "16px" : "0",
                                    borderBottom: actIdx < processorActivities.length - 1 ? "1px solid #E5E7EB" : "none" }}>
                                    
                                    {/* Processing Activity */}
                                    {activityInfo.activityName && (
                                      <div style={{ marginBottom: "8px" }}>
                                        <Text appearance="body-s-bold" color="primary-grey-100">
                                          Processing activity {actIdx + 1}:{" "}
                                        </Text>
                                        <Text appearance="body-s" color="primary-grey-80" style={{ marginTop: "4px" }}>
                                          {activityInfo.activityName}
                                        </Text>
                                      </div>
                                    )}

                                    {/* Used By */}
                                    {activityInfo.processorName && (
                                      <div style={{ marginBottom: "8px" }}>
                                        <Text appearance="body-s-bold" color="primary-grey-100">
                                          Used By:{" "}
                                        </Text>
                                        <Text appearance="body-s" color="primary-grey-80" style={{ marginTop: "4px" }}>
                                          {activityInfo.processorName}
                                        </Text>
                                      </div>
                                    )}

                                    {/* Data Types and Items */}
                                    {dataTypesList.length > 0 && dataTypesList.map((dataType, dtIdx) => (
                                      <div key={dtIdx} style={{ marginBottom: dtIdx < dataTypesList.length - 1 ? "8px" : "0" }}>
                                        {/* Data Type */}
                                        {dataType.dataTypeName && (
                                          <div style={{ marginBottom: "4px" }}>
                                            <Text appearance="body-s-bold" color="primary-grey-100">
                                              Data Type:{" "}
                                            </Text>
                                            <Text appearance="body-s" color="primary-grey-80" style={{ marginTop: "4px" }}>
                                              {dataType.dataTypeName}
                                            </Text>
                                          </div>
                                        )}

                                        {/* Data Item */}
                                        {dataType.dataItems && dataType.dataItems.length > 0 && (
                                          <div>
                                            <Text appearance="body-s-bold" color="primary-grey-100">
                                              Data item:{" "}
                                            </Text>
                                            <Text appearance="body-s" color="primary-grey-80" style={{ marginTop: "4px" }}>
                                              {dataType.dataItems.join(", ")}
                                            </Text>
                                          </div>
                                        )}
                                      </div>
                                    ))}
                                  </div>
                                );
                              })}
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div style={{ padding: "20px", textAlign: "center" }}>
                  <Text appearance="body-s" color="primary-grey-80">
                    No consent data available
                  </Text>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ConsentDeletionDetail;
