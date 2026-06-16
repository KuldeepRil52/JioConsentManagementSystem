import React, { useState, useEffect, useMemo, useCallback } from "react";
import { Text, Tabs, TabItem, BadgeV2, Icon } from "../custom-components";
import { IcSuccessColored, IcError, IcWarningColored, IcCode, IcTicketDetails, IcTimelapse } from "../custom-components/Icon";
import { useSelector } from "react-redux";
import "../styles/dataComplianceReport.css";
import { isSandboxMode } from "../utils/sandboxMode";
import { sandboxAPICall } from "../utils/sandboxMode";
import config from "../utils/config";

const DataComplianceReport = () => {
  const [activeTab, setActiveTab] = useState(0);
  const [callbackData, setCallbackData] = useState([]);
  const [deletionData, setDeletionData] = useState([]);
  const [stats, setStats] = useState(null);
  const [purgeStats, setPurgeStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const tenantId = useSelector((state) => state.common.tenant_id);
  const businessId = useSelector((state) => state.common.business_id);
  const token = useSelector((state) => state.common.session_token);

  const fetchComplianceData = useCallback(async (signal) => {
    setLoading(true);
    setError(null);
    try {
      if (isSandboxMode()) {
        const callbackResponse = await sandboxAPICall(
          'compliance/callbacks',
          'GET',
          {},
          {}
        );
        const deletionResponse = await sandboxAPICall(
          'compliance/deletions',
          'GET',
          {},
          {}
        );
        
        if (signal?.aborted) return;

        if (callbackResponse && callbackResponse.data && callbackResponse.data.stats) {
          setStats(callbackResponse.data.stats);
        }
        
        if (deletionResponse && deletionResponse.data && deletionResponse.data.stats) {
          setPurgeStats(deletionResponse.data.stats);
        }
        
        setCallbackData(transformCallbackData(callbackResponse));
        setDeletionData(transformDeletionData(deletionResponse));
      } else {
        const callbackResponse = await fetch(
          config.compliance_callbacks_stats,
          {
            method: 'GET',
            signal,
            headers: {
              'Content-Type': 'application/json',
              'X-Tenant-Id': tenantId,
              'tenant-id': tenantId,
              'business-id': businessId,
              'x-session-token': token
            }
          }
        );

        const deletionResponse = await fetch(
          config.compliance_purge_stats,
          {
            method: 'GET',
            signal,
            headers: {
              'Content-Type': 'application/json',
              'X-Tenant-Id': tenantId,
              'tenant-id': tenantId,
              'business-id': businessId,
              'x-session-token': token
            }
          }
        );

        if (!callbackResponse.ok || !deletionResponse.ok) {
          throw new Error('Failed to fetch compliance data');
        }

        const callbackJson = await callbackResponse.json();
        const deletionJson = await deletionResponse.json();

        if (signal?.aborted) return;

        if (callbackJson && callbackJson.data && callbackJson.data.stats) {
          setStats(callbackJson.data.stats);
        }
        
        if (deletionJson && deletionJson.data && deletionJson.data.stats) {
          setPurgeStats(deletionJson.data.stats);
        }

        setCallbackData(transformCallbackData(callbackJson));
        setDeletionData(transformDeletionData(deletionJson));
      }
    } catch (error) {
      if (error.name === 'AbortError') return;
      console.error("Error fetching compliance data:", error);
      setError("Failed to load compliance data. Please try again.");
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, [tenantId, businessId, token]);

  useEffect(() => {
    if (!tenantId || !businessId) return;
    const controller = new AbortController();
    fetchComplianceData(controller.signal);
    return () => controller.abort();
  }, [fetchComplianceData]);

  // Transform callback API response to component format
  const transformCallbackData = (apiData) => {
    if (!apiData || !apiData.data) return [];
    
    const rows = [];
    const { dataFiduciary = {}, dataProcessor = {} } = apiData.data;
    
    // Process Data Fiduciaries
    Object.keys(dataFiduciary).forEach(id => {
      const entity = dataFiduciary[id];
      if (entity.byEventType && Array.isArray(entity.byEventType)) {
        entity.byEventType.forEach(eventData => {
          rows.push({
            dfProcessor: entity.name || 'N/A',
            eventType: eventData.eventType || 'N/A',
            callbacksSent: typeof eventData.totalCallbacks === 'number' ? eventData.totalCallbacks : 0,
            successful: typeof eventData.successfulCallbacks === 'number' ? eventData.successfulCallbacks : 0,
            failed: typeof eventData.failedCallbacks === 'number' ? eventData.failedCallbacks : 0,
            successRate: typeof eventData.successPercentage === 'number' ? eventData.successPercentage : 0,
            failureRate: typeof eventData.failurePercentage === 'number' ? eventData.failurePercentage : 0
          });
        });
      }
    });
    
    // Process Data Processors
    Object.keys(dataProcessor).forEach(id => {
      const entity = dataProcessor[id];
      if (entity.byEventType && Array.isArray(entity.byEventType)) {
        entity.byEventType.forEach(eventData => {
          rows.push({
            dfProcessor: entity.name || 'N/A',
            eventType: eventData.eventType || 'N/A',
            callbacksSent: typeof eventData.totalCallbacks === 'number' ? eventData.totalCallbacks : 0,
            successful: typeof eventData.successfulCallbacks === 'number' ? eventData.successfulCallbacks : 0,
            failed: typeof eventData.failedCallbacks === 'number' ? eventData.failedCallbacks : 0,
            successRate: typeof eventData.successPercentage === 'number' ? eventData.successPercentage : 0,
            failureRate: typeof eventData.failurePercentage === 'number' ? eventData.failurePercentage : 0
          });
        });
      }
    });
    
    return rows;
  };

  // Transform deletion API response to component format
  const transformDeletionData = (apiData) => {
    if (!apiData || !apiData.data) return [];
    
    const rows = [];
    const { dataProcessor = {} } = apiData.data;
    
    // Process Data Processors only
    Object.keys(dataProcessor).forEach(id => {
      const entity = dataProcessor[id];
      if (entity.byEventType && Array.isArray(entity.byEventType)) {
        entity.byEventType.forEach(eventData => {
          // Determine status based on pending and overdue events
          const pendingEvents = typeof eventData.pendingEvents === 'number' ? eventData.pendingEvents : 0;
          const overdueEvents = typeof eventData.overdueEvents === 'number' ? eventData.overdueEvents : 0;
          let status = 'Completed';
          if (overdueEvents > 0) {
            status = 'Overdue';
          } else if (pendingEvents > 0) {
            status = 'Pending';
          }
          
          rows.push({
            dfProcessor: entity.name || 'N/A',
            eventType: eventData.eventType || 'N/A',
            totalRecords: typeof eventData.totalEvents === 'number' ? eventData.totalEvents : 0,
            recordsPurged: typeof eventData.purgedEvents === 'number' ? eventData.purgedEvents : 0,
            pendingRecords: pendingEvents,
            purgePercentage: typeof eventData.purgePercentage === 'number' ? eventData.purgePercentage : 0,
            pendingPercentage: typeof eventData.pendingPercentage === 'number' ? eventData.pendingPercentage : 0,
            overduePercentage: typeof eventData.overduePercentage === 'number' ? eventData.overduePercentage : 0,
            daysOverdue: overdueEvents, // Using overdueEvents count as days overdue
            status: status
          });
        });
      }
    });
    
    return rows;
  };

  // Calculate purge statistics from table data
  const calculatedPurgeStats = useMemo(() => {
    if (!deletionData || deletionData.length === 0) {
      return null;
    }

    // Calculate totals from all rows
    const totalEvents = deletionData.reduce((sum, row) => sum + (row.totalRecords || 0), 0);
    const purgedEvents = deletionData.reduce((sum, row) => sum + (row.recordsPurged || 0), 0);
    const pendingEvents = deletionData.reduce((sum, row) => sum + (row.pendingRecords || 0), 0);
    const overdueEvents = deletionData.reduce((sum, row) => sum + (row.daysOverdue || 0), 0);

    // Calculate percentages
    const purgePercentage = totalEvents > 0 ? (purgedEvents / totalEvents) * 100 : 0;
    const pendingPercentage = totalEvents > 0 ? (pendingEvents / totalEvents) * 100 : 0;
    const overduePercentage = totalEvents > 0 ? (overdueEvents / totalEvents) * 100 : 0;

    // Group by event type
    const eventTypeMap = {};
    deletionData.forEach(row => {
      const eventType = row.eventType || 'N/A';
      if (!eventTypeMap[eventType]) {
        eventTypeMap[eventType] = {
          eventType: eventType,
          totalEvents: 0,
          purgedEvents: 0,
          pendingEvents: 0,
          overdueEvents: 0
        };
      }
      eventTypeMap[eventType].totalEvents += row.totalRecords || 0;
      eventTypeMap[eventType].purgedEvents += row.recordsPurged || 0;
      eventTypeMap[eventType].pendingEvents += row.pendingRecords || 0;
      eventTypeMap[eventType].overdueEvents += row.daysOverdue || 0;
    });

    const byEventType = Object.values(eventTypeMap).map(event => ({
      ...event,
      purgePercentage: event.totalEvents > 0 ? (event.purgedEvents / event.totalEvents) * 100 : 0,
      pendingPercentage: event.totalEvents > 0 ? (event.pendingEvents / event.totalEvents) * 100 : 0,
      overduePercentage: event.totalEvents > 0 ? (event.overdueEvents / event.totalEvents) * 100 : 0
    }));

    return {
      totalEvents,
      purgedEvents,
      pendingEvents,
      overdueEvents,
      purgePercentage,
      pendingPercentage,
      overduePercentage,
      byEventType
    };
  }, [deletionData]);

  const getStatusBadge = (status) => {
    if (status === "Completed") {
      return (
        <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
          <IcSuccessColored height={16} width={16} />
          <Text appearance="body-xs-bold" color="success-100">
            Completed
          </Text>
        </div>
      );
    } else if (status === "Overdue") {
      return (
        <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
          <IcError height={16} width={16} />
          <Text appearance="body-xs-bold" color="error-100">
            Overdue
          </Text>
        </div>
      );
    } else {
      return (
        <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
          <IcWarningColored height={16} width={16} />
          <Text appearance="body-xs-bold" color="warning-100">
            Pending
          </Text>
        </div>
      );
    }
  };

  // Donut Chart Component
  const DonutChart = ({ successful, failed, total }) => {
    const successPercentage = total > 0 ? (successful / total) * 100 : 0;
    const failurePercentage = total > 0 ? (failed / total) * 100 : 0;
    
    // Calculate stroke dash array for donut segments
    const radius = 70;
    const circumference = 2 * Math.PI * radius;
    const successLength = (successPercentage / 100) * circumference;
    const failureLength = (failurePercentage / 100) * circumference;
    
    return (
      <div className="donut-chart-container">
        <div className="donut-header">
          <Text appearance="body-s-bold" color="primary-grey-100">
            Callback Statistics
          </Text>
          <div className="donut-total-stat">
            <Text appearance="heading-l" color="primary-grey-100">{total}</Text>
            <Text appearance="body-xs" color="primary-grey-60">Total Callbacks</Text>
          </div>
        </div>
        
        <svg width="200" height="200" viewBox="0 0 200 200">
          {/* Background circle */}
          <circle
            cx="100"
            cy="100"
            r={radius}
            fill="none"
            stroke="#f3f4f6"
            strokeWidth="30"
          />
          
          {/* Success arc */}
          {successful > 0 && (
            <circle
              cx="100"
              cy="100"
              r={radius}
              fill="none"
              stroke="#10B981"
              strokeWidth="30"
              strokeDasharray={`${successLength} ${circumference}`}
              strokeDashoffset={0}
              transform="rotate(-90 100 100)"
              strokeLinecap="round"
            />
          )}
          
          {/* Failure arc */}
          {failed > 0 && (
            <circle
              cx="100"
              cy="100"
              r={radius}
              fill="none"
              stroke="#EF4444"
              strokeWidth="30"
              strokeDasharray={`${failureLength} ${circumference}`}
              strokeDashoffset={-successLength}
              transform="rotate(-90 100 100)"
              strokeLinecap="round"
            />
          )}
          
          {/* Center text - show percentage */}
          <text
            x="100"
            y="95"
            textAnchor="middle"
            fontSize="24"
            fontWeight="bold"
            fill="#10B981"
          >
            {successPercentage.toFixed(1)}%
          </text>
          <text
            x="100"
            y="115"
            textAnchor="middle"
            fontSize="12"
            fill="#6B7280"
          >
            Success Rate
          </text>
        </svg>
      </div>
    );
  };

  // Event Type Bar Chart Component
  const EventTypeChart = ({ byEventType }) => {
    if (!byEventType || byEventType.length === 0) return null;
    
    const maxCallbacks = Math.max(...byEventType.map(e => e.totalCallbacks));
    
    // Color palette for different event types
    const eventColors = [
      '#3B82F6',      // Blue
      '#10B981',      // Green
      '#F59E0B',      // Amber
      '#6366F1',      // Indigo
      '#EC4899',      // Pink
      '#14B8A6',      // Teal
      '#F97316',      // Orange
      '#8B5CF6'       // Purple
    ];
    
    return (
      <div className="event-type-chart">
        <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "16px" }}>
          Callbacks by Event Type
        </Text>
        {byEventType.map((event, index) => {
          const percentage = maxCallbacks > 0 ? (event.totalCallbacks / maxCallbacks) * 100 : 0;
          const color = eventColors[index % eventColors.length];
          
          return (
            <div key={index} className="event-bar-item">
              <div className="event-bar-row">
                <Text appearance="body-xs" color="primary-grey-80" style={{ fontSize: "12px", fontWeight: '500', minWidth: '140px' }}>
                  {event.eventType.replace(/_/g, ' ')}
                </Text>
                <div className="event-bar-wrapper">
                  <div 
                    className="event-bar-simple" 
                    style={{ 
                      width: `${percentage}%`,
                      backgroundColor: color
                    }}
                  ></div>
                </div>
                <Text appearance="body-xs-bold" style={{ fontSize: "12px", color: color, minWidth: '30px', textAlign: 'right' }}>
                  {event.totalCallbacks}
                </Text>
              </div>
            </div>
          );
        })}
      </div>
    );
  };

  return (
    <div className="compliance-report-container">
      <div className="compliance-header">
        <Text appearance="heading-xs">Data Compliance Report</Text><br />
        <Text appearance="body-sm" color="grey-100">
          Monitor consent callbacks and data deletion confirmations
        </Text>
      </div>

      <div className="compliance-tabs-wrapper">
        <div className="compliance-tabs">
          <Tabs
            activeKey={activeTab}
            onTabChange={(index) => setActiveTab(index)}
            variant="default"
          >
            <TabItem label="Consent Event & Grievance Callbacks" />
            <TabItem label="Data Deletion / Purge Confirmation" />
          </Tabs>
        </div>
      </div>

      <div className="compliance-content">
        {loading ? (
          <div className="compliance-loading">
            <Text appearance="body-md">Loading compliance data...</Text>
          </div>
        ) : error ? (
          <div className="compliance-error">
            <IcError height={48} width={48} />
            <Text appearance="body-md" color="error-100">{error}</Text>
            <button 
              onClick={() => fetchComplianceData()} 
              className="retry-button"
              style={{
                marginTop: '16px',
                padding: '8px 16px',
                backgroundColor: '#0F3CC9',
                color: 'white',
                border: 'none',
                borderRadius: '6px',
                cursor: 'pointer'
              }}
            >
              Retry
            </button>
          </div>
        ) : (
          <>
            {/* Tab 1: Callbacks */}
            {activeTab === 0 && (
              <div className="compliance-section">
                <div className="section-description">
                  <Text appearance="body-sm" color="grey-80">
                    This table shows callbacks sent by the CMS to Data Fiduciaries / Processors 
                    for consent lifecycle events and grievance events.
                  </Text>
                </div>

                {callbackData.length === 0 ? (
                  <div className="compliance-loading">
                    <Text appearance="body-md" color="grey-80">No data available</Text>
                  </div>
                ) : (
                  <div className="compliance-two-column-layout">
                    {/* Left Column: Table */}
                    <div className="compliance-table-column">
                <div className="compliance-table-container">
                  <table className="compliance-table">
                    <thead>
                      <tr>
                        <th>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            DF / Processor
                          </Text>
                        </th>
                        <th>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Event Type
                          </Text>
                        </th>
                        <th style={{ textAlign: "right" }}>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Callbacks Sent
                          </Text>
                        </th>
                        <th style={{ textAlign: "right" }}>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Successful
                          </Text>
                        </th>
                        <th style={{ textAlign: "right" }}>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Failed
                          </Text>
                        </th>
                        <th style={{ textAlign: "right" }}>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Success %
                          </Text>
                        </th>
                        <th style={{ textAlign: "right" }}>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Failure %
                          </Text>
                        </th>
                        <th>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Logs
                          </Text>
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {callbackData.map((row, index) => (
                        <tr key={index}>
                          <td>
                            <Text appearance="body-sm">{row.dfProcessor}</Text>
                          </td>
                          <td>
                            <BadgeV2
                              label={row.eventType}
                              size="small"
                              variant={
                                    row.eventType.includes("Consent") || row.eventType.includes("CONSENT")
                                  ? "info"
                                  : "warning"
                              }
                            />
                          </td>
                          <td style={{ textAlign: "right" }}>
                                <Text appearance="body-sm-bold">
                                  {String(typeof row.callbacksSent === 'number' ? row.callbacksSent : 0)}
                                </Text>
                          </td>
                          <td style={{ textAlign: "right" }}>
                                <Text appearance="body-sm">
                                  {String(typeof row.successful === 'number' ? row.successful : 0)}
                            </Text>
                          </td>
                          <td style={{ textAlign: "right" }}>
                              <Text appearance="body-sm">
                                {String(typeof row.failed === 'number' ? row.failed : 0)}
                            </Text>
                          </td>
                          <td style={{ textAlign: "right" }}>
                            <span
                              className="percentage-badge"
                              style={{
                                  backgroundColor: "#10B981",
                                  padding: "4px 12px",
                                  borderRadius: "12px",
                                  display: "inline-block"
                              }}
                            >
                              <Text appearance="body-xs-bold" color="white">
                                {row.successRate.toFixed(1)}%
                              </Text>
                            </span>
                          </td>
                          <td style={{ textAlign: "right" }}>
                              <span
                                className="percentage-badge"
                                style={{
                                  backgroundColor: "#EF4444",
                                  padding: "4px 12px",
                                  borderRadius: "12px",
                                  display: "inline-block"
                                }}
                              >
                                <Text appearance="body-xs-bold" color="white">
                              {row.failureRate.toFixed(1)}%
                            </Text>
                              </span>
                          </td>
                          <td>
                            <Icon ic={<IcTicketDetails />} size="medium" color="primary_grey_80" />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                    </div>
                    
                    {/* Right Column: Charts */}
                    <div className="compliance-charts-column">
                      {stats && (
                        <>
                          {/* Donut Chart with integrated stats */}
                          <div className="chart-section">
                            <DonutChart
                              successful={stats.successfulCallbacks || 0}
                              failed={stats.failedCallbacks || 0}
                              total={stats.totalCallbacks || 0}
                            />
                          </div>
                          
                          {/* Event Type Bar Chart */}
                          {stats.byEventType && stats.byEventType.length > 0 && (
                            <div className="chart-section">
                              <EventTypeChart byEventType={stats.byEventType} />
                            </div>
                          )}
                        </>
                      )}
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* Tab 2: Data Deletion */}
            {activeTab === 1 && (
              <div className="compliance-section">
                <div className="section-description">
                  <Text appearance="body-sm" color="grey-80">
                    Tracks deletion confirmations from Data Fiduciaries / Processors after 
                    consent withdrawal or expiry. Includes consent count, SLA, and purge status.
                  </Text>
                </div>

                <div className="compliance-two-column-layout">
                  {/* Left Column: Table */}
                  <div className="compliance-table-column">
                    <div className="compliance-table-container">
                      <table className="compliance-table deletion-table">
                    <thead>
                      <tr>
                        <th>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            DF / Processor
                          </Text>
                        </th>
                        <th>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Event types
                          </Text>
                        </th>
                        <th style={{ textAlign: "right" }}>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Total Records
                          </Text>
                        </th>
                        <th style={{ textAlign: "right" }}>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Purged
                          </Text>
                        </th>
                        <th style={{ textAlign: "right" }}>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Pending
                          </Text>
                        </th>
                        <th style={{ textAlign: "center" }}>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Purge %
                          </Text>
                        </th>
                        <th style={{ textAlign: "center" }}>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Pending %
                          </Text>
                        </th>
                        <th style={{ textAlign: "center" }}>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Overdue %
                          </Text>
                        </th>
                        <th style={{ textAlign: "center" }}>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Overdue
                          </Text>
                        </th>
                        <th>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Status
                          </Text>
                        </th>
                        <th className="logs-column-sticky">
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Logs
                          </Text>
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {deletionData.length === 0 ? (
                        <tr>
                          <td colSpan="11" style={{ textAlign: "center", padding: "40px" }} className="logs-column-sticky">
                            <Text appearance="body-md" color="grey-80">No data available</Text>
                          </td>
                        </tr>
                      ) : (
                        deletionData.map((row, index) => (
                          <tr key={index} className={row.status === "Overdue" ? "overdue-row" : ""}>
                            <td>
                              <Text appearance="body-sm-bold">{row.dfProcessor}</Text>
                            </td>
                            <td>
                              <Text appearance="body-sm">{row.eventType || row.consentType || "-"}</Text>
                            </td>
                            <td style={{ textAlign: "right" }}>
                              <Text appearance="body-sm-bold">{String(row.totalRecords)}</Text>
                            </td>
                            <td style={{ textAlign: "right" }}>
                              <Text appearance="body-sm" color="success-100">
                                {String(row.recordsPurged)}
                              </Text>
                            </td>
                            <td style={{ textAlign: "right" }}>
                              <Text
                                appearance="body-sm-bold"
                                color={row.pendingRecords > 0 ? "error-100" : "grey-80"}
                              >
                                {String(row.pendingRecords)}
                              </Text>
                            </td>
                            <td style={{ textAlign: "center" }}>
                              <Text appearance="body-sm">
                                {typeof row.purgePercentage === 'number' ? row.purgePercentage.toFixed(1) : '0.0'}%
                              </Text>
                            </td>
                            <td style={{ textAlign: "center" }}>
                              <Text appearance="body-sm" color={row.pendingPercentage > 0 ? "error-100" : "grey-80"}>
                                {typeof row.pendingPercentage === 'number' ? row.pendingPercentage.toFixed(1) : '0.0'}%
                              </Text>
                            </td>
                            <td style={{ textAlign: "center" }}>
                              <Text appearance="body-sm" color={row.overduePercentage > 0 ? "error-100" : "grey-80"}>
                                {typeof row.overduePercentage === 'number' ? row.overduePercentage.toFixed(1) : '0.0'}%
                              </Text>
                            </td>
                            <td style={{ textAlign: "center" }}>
                              {row.daysOverdue > 0 ? (
                                <span className="overdue-badge">
                                  <Text appearance="body-xs-bold" color="error-100">
                                    {String(row.daysOverdue)}
                                  </Text>
                                </span>
                              ) : (
                                <Text appearance="body-sm" color="grey-80">
                                  0
                                </Text>
                              )}
                            </td>
                            <td>{getStatusBadge(row.status)}</td>
                            <td className="logs-column-sticky">
                              <Icon ic={<IcTicketDetails />} size="medium" color="primary_grey_80" />
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                    </div>
                  </div>

                  {/* Right Column: Charts */}
                  <div className="compliance-charts-column">
                    {calculatedPurgeStats && (
                      <>
                        {/* Purge Statistics Donut Chart */}
                        <div className="chart-section">
                          <div className="donut-chart-container">
                            <div className="donut-header">
                              <Text appearance="body-s-bold" color="primary-grey-100">
                                Purge Statistics
                              </Text>
                              <div className="donut-total-stat">
                                <Text appearance="heading-l" color="primary-grey-100">{calculatedPurgeStats.totalEvents || 0}</Text>
                                <Text appearance="body-xs" color="primary-grey-60">Total Events</Text>
                              </div>
                            </div>
                            
                            <svg width="200" height="200" viewBox="0 0 200 200">
                              {/* Background circle */}
                              <circle
                                cx="100"
                                cy="100"
                                r="70"
                                fill="none"
                                stroke="#f3f4f6"
                                strokeWidth="30"
                              />
                              
                              {/* Purged arc (green) */}
                              {calculatedPurgeStats.purgedEvents > 0 && (
                                <circle
                                  cx="100"
                                  cy="100"
                                  r="70"
                                  fill="none"
                                  stroke="#10B981"
                                  strokeWidth="30"
                                  strokeDasharray={`${(calculatedPurgeStats.purgePercentage / 100) * (2 * Math.PI * 70)} ${2 * Math.PI * 70}`}
                                  strokeDashoffset={0}
                                  transform="rotate(-90 100 100)"
                                  strokeLinecap="round"
                                />
                              )}
                              
                              {/* Pending/Overdue arc (red) */}
                              {calculatedPurgeStats.pendingEvents > 0 && (
                                <circle
                                  cx="100"
                                  cy="100"
                                  r="70"
                                  fill="none"
                                  stroke="#EF4444"
                                  strokeWidth="30"
                                  strokeDasharray={`${(calculatedPurgeStats.pendingPercentage / 100) * (2 * Math.PI * 70)} ${2 * Math.PI * 70}`}
                                  strokeDashoffset={-(calculatedPurgeStats.purgePercentage / 100) * (2 * Math.PI * 70)}
                                  transform="rotate(-90 100 100)"
                                  strokeLinecap="round"
                                />
                              )}
                              
                              {/* Center text - show purge percentage */}
                              <text
                                x="100"
                                y="95"
                                textAnchor="middle"
                                fontSize="24"
                                fontWeight="bold"
                                fill="#10B981"
                              >
                                {calculatedPurgeStats.purgePercentage?.toFixed(1) || 0}%
                              </text>
                              <text
                                x="100"
                                y="115"
                                textAnchor="middle"
                                fontSize="12"
                                fill="#6B7280"
                              >
                                Purge Rate
                              </text>
                            </svg>
                          </div>
                        </div>
                        
                        {/* Event Type Chart */}
                        {calculatedPurgeStats.byEventType && calculatedPurgeStats.byEventType.length > 0 && (
                          <div className="chart-section">
                            <div className="event-type-chart">
                              <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "16px" }}>
                                Purge Events by Type
                              </Text>
                              {calculatedPurgeStats.byEventType.map((event, index) => {
                                const maxEvents = Math.max(...calculatedPurgeStats.byEventType.map(e => e.totalEvents));
                                const percentage = maxEvents > 0 ? (event.totalEvents / maxEvents) * 100 : 0;
                                const colors = ['#3B82F6', '#10B981', '#F59E0B', '#6366F1'];
                                const color = colors[index % colors.length];
                                
                                return (
                                  <div key={index} className="event-bar-item">
                                    <div className="event-bar-row">
                                      <Text appearance="body-xs" color="primary-grey-80" style={{ fontSize: "12px", fontWeight: '500', minWidth: '140px' }}>
                                        {event.eventType.replace(/_/g, ' ')}
                                      </Text>
                                      <div className="event-bar-wrapper">
                                        <div 
                                          className="event-bar-simple" 
                                          style={{ 
                                            width: `${percentage}%`,
                                            backgroundColor: color
                                          }}
                                        ></div>
                                      </div>
                                      <Text appearance="body-xs-bold" style={{ fontSize: "12px", color: color, minWidth: '30px', textAlign: 'right' }}>
                                        {event.totalEvents}
                                      </Text>
                                    </div>
                                  </div>
                                );
                              })}
                            </div>
                          </div>
                        )}
                      </>
                    )}
                  </div>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default DataComplianceReport;

