import React, { useState, useEffect } from "react";
import { Text, Tabs, TabItem, BadgeV2 } from '../custom-components';
import { IcSuccessColored, IcError, IcWarningColored } from '../custom-components/Icon';
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

  useEffect(() => {
    fetchComplianceData();
  }, [tenantId, businessId]);

  const fetchComplianceData = async () => {
    setLoading(true);
    setError(null);
    try {
      if (isSandboxMode()) {
        // Fetch mock data for sandbox
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
        
        // Extract stats data
        if (callbackResponse && callbackResponse.data && callbackResponse.data.stats) {
          setStats(callbackResponse.data.stats);
        }
        
        if (deletionResponse && deletionResponse.data && deletionResponse.data.stats) {
          setPurgeStats(deletionResponse.data.stats);
        }
        
        setCallbackData(transformCallbackData(callbackResponse));
        setDeletionData(transformDeletionData(deletionResponse));
      } else {
        // Real API calls using specific endpoints from config
        
        // Fetch callback stats
        const callbackResponse = await fetch(
          config.compliance_callbacks_stats,
          {
            method: 'GET',
            headers: {
              'Content-Type': 'application/json',
              'X-Tenant-Id': tenantId,
              'x-session-token': token
            }
          }
        );

        // Fetch purge stats
        const deletionResponse = await fetch(
          config.compliance_purge_stats,
          {
            method: 'GET',
            headers: {
              'Content-Type': 'application/json',
              'X-Tenant-Id': tenantId,
              'x-session-token': token
            }
          }
        );

        if (!callbackResponse.ok || !deletionResponse.ok) {
          throw new Error('Failed to fetch compliance data');
        }

        const callbackJson = await callbackResponse.json();
        const deletionJson = await deletionResponse.json();

        // Extract stats data
        if (callbackJson && callbackJson.data && callbackJson.data.stats) {
          setStats(callbackJson.data.stats);
        }
        
        if (deletionJson && deletionJson.data && deletionJson.data.stats) {
          setPurgeStats(deletionJson.data.stats);
        }

        // Transform the API response to match our component's expected format
        setCallbackData(transformCallbackData(callbackJson));
        setDeletionData(transformDeletionData(deletionJson));
      }
    } catch (error) {
      console.error("Error fetching compliance data:", error);
      setError("Failed to load compliance data. Please try again.");
    } finally {
      setLoading(false);
    }
  };

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
    const { dataFiduciary = {}, dataProcessor = {} } = apiData.data;
    
    // Process Data Fiduciaries
    Object.keys(dataFiduciary).forEach(id => {
      const entity = dataFiduciary[id];
      if (entity.byEventType && Array.isArray(entity.byEventType)) {
        entity.byEventType.forEach(eventData => {
          rows.push({
            dfProcessor: entity.name || 'N/A',
            consentCount: typeof eventData.consentCount === 'number' ? eventData.consentCount : 0,
            consentType: eventData.eventType || 'N/A',
            dateOfEvent: eventData.dateOfEvent || 'N/A',
            slaDays: typeof eventData.slaDays === 'number' ? eventData.slaDays : 7,
            piiDataType: eventData.piiDataType || 'N/A',
            totalRecords: typeof eventData.totalRecords === 'number' ? eventData.totalRecords : 0,
            recordsPurged: typeof eventData.recordsPurged === 'number' ? eventData.recordsPurged : 0,
            pendingRecords: typeof eventData.pendingRecords === 'number' ? eventData.pendingRecords : 0,
            daysOverdue: typeof eventData.daysOverdue === 'number' ? eventData.daysOverdue : 0,
            status: eventData.status || (eventData.pendingRecords > 0 ? 'Overdue' : 'Completed')
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
            consentCount: typeof eventData.consentCount === 'number' ? eventData.consentCount : 0,
            consentType: eventData.eventType || 'N/A',
            dateOfEvent: eventData.dateOfEvent || 'N/A',
            slaDays: typeof eventData.slaDays === 'number' ? eventData.slaDays : 7,
            piiDataType: eventData.piiDataType || 'N/A',
            totalRecords: typeof eventData.totalRecords === 'number' ? eventData.totalRecords : 0,
            recordsPurged: typeof eventData.recordsPurged === 'number' ? eventData.recordsPurged : 0,
            pendingRecords: typeof eventData.pendingRecords === 'number' ? eventData.pendingRecords : 0,
            daysOverdue: typeof eventData.daysOverdue === 'number' ? eventData.daysOverdue : 0,
            status: eventData.status || (eventData.pendingRecords > 0 ? 'Overdue' : 'Completed')
          });
        });
      }
    });
    
    return rows;
  };

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
            activeTab={activeTab}
            onChange={(index) => setActiveTab(index)}
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
              onClick={fetchComplianceData} 
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
                            # Consents
                          </Text>
                        </th>
                        <th>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Date of Event
                          </Text>
                        </th>
                        <th style={{ textAlign: "center" }}>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            SLA (Days)
                          </Text>
                        </th>
                        <th>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            PII Data Type
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
                            Days Overdue
                          </Text>
                        </th>
                        <th>
                          <Text appearance="body-xs-bold" color="primary-grey-80">
                            Status
                          </Text>
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {deletionData.length === 0 ? (
                        <tr>
                          <td colSpan="10" style={{ textAlign: "center", padding: "40px" }}>
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
                              <div>
                                <Text appearance="body-sm">{String(row.consentCount)}</Text>
                                <br />
                                <Text appearance="body-xs" color="grey-80">
                                  ({row.consentType})
                                </Text>
                              </div>
                            </td>
                            <td>
                              <Text appearance="body-xs">{row.dateOfEvent}</Text>
                            </td>
                            <td style={{ textAlign: "center" }}>
                              <Text appearance="body-sm">{String(row.slaDays)}</Text>
                            </td>
                            <td>
                              <Text appearance="body-xs">{row.piiDataType}</Text>
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
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                    </div>
                  </div>

                  {/* Right Column: Charts */}
                  <div className="compliance-charts-column">
                    {purgeStats && (
                      <>
                        {/* Purge Statistics Donut Chart */}
                        <div className="chart-section">
                          <div className="donut-chart-container">
                            <div className="donut-header">
                              <Text appearance="body-s-bold" color="primary-grey-100">
                                Purge Statistics
                              </Text>
                              <div className="donut-total-stat">
                                <Text appearance="heading-l" color="primary-grey-100">{purgeStats.totalEvents || 0}</Text>
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
                              {purgeStats.purgedEvents > 0 && (
                                <circle
                                  cx="100"
                                  cy="100"
                                  r="70"
                                  fill="none"
                                  stroke="#10B981"
                                  strokeWidth="30"
                                  strokeDasharray={`${(purgeStats.purgePercentage / 100) * (2 * Math.PI * 70)} ${2 * Math.PI * 70}`}
                                  strokeDashoffset={0}
                                  transform="rotate(-90 100 100)"
                                  strokeLinecap="round"
                                />
                              )}
                              
                              {/* Pending/Overdue arc (red) */}
                              {purgeStats.pendingEvents > 0 && (
                                <circle
                                  cx="100"
                                  cy="100"
                                  r="70"
                                  fill="none"
                                  stroke="#EF4444"
                                  strokeWidth="30"
                                  strokeDasharray={`${(purgeStats.pendingPercentage / 100) * (2 * Math.PI * 70)} ${2 * Math.PI * 70}`}
                                  strokeDashoffset={-(purgeStats.purgePercentage / 100) * (2 * Math.PI * 70)}
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
                                {purgeStats.purgePercentage?.toFixed(1) || 0}%
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
                        {purgeStats.byEventType && purgeStats.byEventType.length > 0 && (
                          <div className="chart-section">
                            <div className="event-type-chart">
                              <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "16px" }}>
                                Purge Events by Type
                              </Text>
                              {purgeStats.byEventType.map((event, index) => {
                                const maxEvents = Math.max(...purgeStats.byEventType.map(e => e.totalEvents));
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

