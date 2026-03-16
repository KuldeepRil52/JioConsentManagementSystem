import React, { useState, useEffect } from 'react';
import { useSelector } from 'react-redux';
import { Text, Spinner, ActionButton, InputFieldV2 } from '../custom-components';
import { IcRefresh, IcClose, IcTime } from '../custom-components/Icon';
import { IcStatusSuccessful } from '../custom-components/Icon';
import { Icon } from '../custom-components';
import config from '../utils/config';
import { isSandboxMode, sandboxAPICall } from '../utils/sandboxMode';
import '../styles/schedulerStats.css';
import '../styles/templates.css';

const SchedulerStats = () => {
  const tenantId = useSelector((state) => state.common.tenant_id);
  const businessId = useSelector((state) => state.common.business_id);
  const token = useSelector((state) => state.common.session_token);

  console.log('🔧 SchedulerStats component mounted/updated with Redux state:', {
    tenantId,
    businessId,
    hasToken: !!token
  });

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [schedulerData, setSchedulerData] = useState(null);
  const [error, setError] = useState(null);

  // Date filters - default to last 7 days
  const [startDate, setStartDate] = useState(() => {
    const date = new Date();
    date.setDate(date.getDate() - 7);
    return date.toISOString().split('T')[0];
  });
  
  const [endDate, setEndDate] = useState(() => {
    return new Date().toISOString().split('T')[0];
  });

  // Fetch scheduler stats
  const fetchSchedulerStats = async (isRefresh = false) => {
    if (isRefresh) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }
    setError(null);

    try {
      // Check if we're in sandbox mode
      if (isSandboxMode()) {
        console.log('🏖️ SANDBOX MODE - Fetching mock scheduler stats');
        const mockResponse = await sandboxAPICall(
          'scheduler/stats',
          'GET',
          {},
          {}
        );
        console.log('✅ Sandbox scheduler data received:', mockResponse.data);
        
        // Transform sandbox data to expected format (wrap array in data property)
        const transformedData = {
          data: Array.isArray(mockResponse.data) ? mockResponse.data : [mockResponse.data]
        };
        
        setSchedulerData(transformedData);
        setLoading(false);
        setRefreshing(false);
        return;
      }

      console.log('🔄 Fetching scheduler stats...', {
        url: `${config.BASE_URL_SCHEDULAR}/schedular/v1/stats`,
        tenantId,
        businessId,
        startDate,
        endDate
      });

      const headers = {
        'Content-Type': 'application/json',
        'tenantId': tenantId,
        'startDate': startDate,
        'endDate': endDate
      };

      // Add session token if available
      if (token) {
        headers['x-session-token'] = token;
      }

      const response = await fetch(`${config.BASE_URL_SCHEDULAR}/schedular/v1/stats`, {
        method: 'GET',
        headers: headers
      });

      console.log('📡 Response status:', response.status);
      console.log('📡 Response content-type:', response.headers.get('content-type'));

      // Check if response is JSON first
      const contentType = response.headers.get('content-type');
      const isJson = contentType && contentType.includes('application/json');

      if (!response.ok) {
        let errorText;
        if (isJson) {
          const errorData = await response.json();
          console.error('❌ API Error (JSON):', errorData);
          errorText = errorData.errorMessage || errorData.message || JSON.stringify(errorData);
        } else {
          errorText = await response.text();
          console.error('❌ API Error (non-JSON):', errorText.substring(0, 500));
        }
        
        // Parse error message for user-friendly display
        let userMessage = `Unable to load scheduler statistics (Status: ${response.status}).`;
        
        if (response.status === 401) {
          userMessage = 'Authentication failed. Please log in again.';
        } else if (response.status === 403) {
          userMessage = 'Access denied. You do not have permission to view scheduler statistics.';
        } else if (response.status === 404) {
          userMessage = 'Scheduler API endpoint not found. The service may not be available.';
        } else if (response.status >= 500) {
          userMessage = 'Server error. Please try again later or contact support.';
        } else if (isJson) {
          try {
            const parsed = typeof errorText === 'string' ? JSON.parse(errorText) : errorText;
            if (Array.isArray(parsed) && parsed[0]?.errorMessage) {
              userMessage = parsed[0].errorMessage;
            } else if (parsed.errorMessage) {
              userMessage = parsed.errorMessage;
            } else if (parsed.message) {
              userMessage = parsed.message;
            }
          } catch (parseError) {
            // Keep default message
          }
        }
        
        throw new Error(userMessage);
      }

      // Check if successful response is JSON
      if (!isJson) {
        const text = await response.text();
        console.error('❌ Non-JSON response received (Status 200):', text.substring(0, 500));
        throw new Error('The scheduler service is not responding correctly. Please contact your administrator.');
      }

      const data = await response.json();
      console.log('✅ Scheduler data received:', data);
      
      // Transform the API response to the expected format
      // API returns an array directly, we need to wrap it in a data property
      const transformedData = {
        data: Array.isArray(data) ? data : [data]
      };
      
      setSchedulerData(transformedData);
    } catch (err) {
      console.error('❌ Error fetching scheduler stats:', err);
      
      // Set user-friendly error message
      if (err.message.includes('Failed to fetch') || err.message.includes('NetworkError')) {
        setError('Unable to connect to the server. Please check your connection and try again.');
      } else {
        setError(err.message);
      }
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    console.log('🔍 SchedulerStats useEffect triggered:', { tenantId, businessId, startDate, endDate });
    if (tenantId && businessId) {
      console.log('✅ tenantId and businessId present, calling fetchSchedulerStats');
      fetchSchedulerStats();
    } else {
      console.warn('⚠️ Missing required values:', { 
        tenantId: tenantId || 'MISSING', 
        businessId: businessId || 'MISSING' 
      });
    }
  }, [tenantId, businessId, startDate, endDate]);

  const handleRefresh = () => {
    fetchSchedulerStats(true);
  };

  const handleDateFilterApply = () => {
    fetchSchedulerStats();
  };

  // Calculate summary metrics
  const getSummaryMetrics = () => {
    if (!schedulerData || !schedulerData.data) {
      return { total: 0, success: 0, failed: 0, running: 0, pending: 0 };
    }

    const data = Array.isArray(schedulerData.data) ? schedulerData.data : [schedulerData.data];
    
    let total = data.length; // Each record is an execution
    let success = 0;
    let failed = 0;
    let running = 0;
    let pending = 0;

    data.forEach(item => {
      // Map API response fields to our metrics
      if (item.status === 'SUCCESS') {
        success++;
      } else if (item.status === 'FAILED' || item.status === 'FAILURE' || item.errorCount > 0) {
        failed++;
      } else if (item.status === 'RUNNING' || item.status === 'IN_PROGRESS') {
        running++;
      } else if (item.status === 'PENDING' || item.status === 'QUEUED') {
        pending++;
      }
    });

    return { total, success, failed, running, pending };
  };

  const metrics = getSummaryMetrics();
  const successRate = metrics.total > 0 ? ((metrics.success / metrics.total) * 100).toFixed(1) : 0;

  if (loading) {
    return (
      <div className="configurePage">
        <div className="scheduler-stats-loading">
          <Spinner size="large" />
          <Text appearance="body-m" color="primary-grey-60">
            Loading scheduler statistics...
          </Text>
        </div>
      </div>
    );
  }

  return (
    <div className="configurePage">
      {/* Header Section */}
      <div className="template-header-section">
        <div className="template-title-section">
          <div style={{ display: 'flex', flexDirection: 'row', gap: '10px' }}>
            <Text appearance="heading-s" color="primary-grey-100">Scheduler Statistics</Text>
            <div className="dataProtectionOfficer-badge" style={{ marginTop: '5px' }}>
              <Text appearance="body-xs-bold" color="primary-grey-80">Monitoring</Text>
            </div>
          </div>
        </div>
        <div className="template-button-group">
          <ActionButton
            icon="IcRefresh"
            kind="tertiary"
            size="medium"
            onClick={handleRefresh}
            disabled={refreshing}
            label={refreshing ? 'Refreshing...' : 'Refresh'}
          />
        </div>
      </div>

        {/* Date Range Filter */}
        <div className="template-table-controls" style={{ justifyContent: 'flex-start', marginBottom: '1.5rem' }}>
          <div className="date-filter-inputs" style={{ display: 'flex', gap: '16px', alignItems: 'flex-end', flexWrap: 'wrap' }}>
            <div className="date-input-group" style={{ display: 'flex', flexDirection: 'column', gap: '6px', minWidth: '180px' }}>
              <Text appearance="body-s-bold" color="primary-grey-80">
                Start Date
              </Text>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="date-input"
                max={endDate}
                style={{
                  padding: '8px 12px',
                  fontSize: '13px',
                  border: '1px solid #d1d5db',
                  borderRadius: '6px',
                  backgroundColor: '#fff',
                  color: '#333',
                  fontFamily: 'inherit',
                }}
              />
            </div>
            <div className="date-input-group" style={{ display: 'flex', flexDirection: 'column', gap: '6px', minWidth: '180px' }}>
              <Text appearance="body-s-bold" color="primary-grey-80">
                End Date
              </Text>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="date-input"
                min={startDate}
                max={new Date().toISOString().split('T')[0]}
                style={{
                  padding: '8px 12px',
                  fontSize: '13px',
                  border: '1px solid #d1d5db',
                  borderRadius: '6px',
                  backgroundColor: '#fff',
                  color: '#333',
                  fontFamily: 'inherit',
                }}
              />
            </div>
            <ActionButton
              kind="primary"
              size="medium"
              label="Apply Filter"
              onClick={handleDateFilterApply}
            />
          </div>
        </div>

        {/* Error State or No Data */}
        {(error || !schedulerData || !schedulerData.data || (Array.isArray(schedulerData.data) && schedulerData.data.length === 0)) && (
          <div style={{ textAlign: 'center', padding: '40px', marginRight: '2rem' }}>
            <Text appearance="body-m" color="primary-grey-80">
              No data available
            </Text>
          </div>
        )}

        {/* Summary Cards */}
        {!error && schedulerData && schedulerData.data && (!Array.isArray(schedulerData.data) || schedulerData.data.length > 0) && (
          <>
            <div className="template-stats-cards">
              <div className="template-stat-card template-stat-total">
                <div className="icon-wrapper">
                  <IcTime size="xl" color="#2196f3" />
                </div>
                <div className="template-stat-content">
                  <Text appearance="heading-xs" color="primary-grey-100">{metrics.total.toLocaleString()}</Text>
                  <Text appearance="body-xs" color="primary-grey-80">Total Executions</Text>
                </div>
              </div>

              <div className="template-stat-card template-stat-published">
                <div className="icon-wrapper">
                  <IcStatusSuccessful size="xl" color="#25AB21" />
                </div>
                <div className="template-stat-content">
                  <Text appearance="heading-xs" color="primary-grey-100">{metrics.success.toLocaleString()}</Text>
                  <Text appearance="body-xs" color="primary-grey-80">Successful ({successRate}%)</Text>
                </div>
              </div>

              <div className="template-stat-card template-stat-draft">
                <div className="icon-wrapper">
                  <IcClose size="xl" color="#e53935" />
                </div>
                <div className="template-stat-content">
                  <Text appearance="heading-xs" color="primary-grey-100">{metrics.failed.toLocaleString()}</Text>
                  <Text appearance="body-xs" color="primary-grey-80">Failed</Text>
                </div>
              </div>

              <div className="template-stat-card template-stat-draft">
                <div className="icon-wrapper" style={{ backgroundColor: '#fff3e0' }}>
                  <Text style={{ fontSize: '24px' }}>⚡</Text>
                </div>
                <div className="template-stat-content">
                  <Text appearance="heading-xs" color="primary-grey-100">{metrics.running.toLocaleString()}</Text>
                  <Text appearance="body-xs" color="primary-grey-80">Running</Text>
                </div>
              </div>

              <div className="template-stat-card template-stat-total">
                <div className="icon-wrapper" style={{ backgroundColor: '#e3f2fd' }}>
                  <Text style={{ fontSize: '24px' }}>⏱️</Text>
                </div>
                <div className="template-stat-content">
                  <Text appearance="heading-xs" color="primary-grey-100">{metrics.pending.toLocaleString()}</Text>
                  <Text appearance="body-xs" color="primary-grey-80">Pending</Text>
                </div>
              </div>
            </div>

            {/* Detailed Stats Table */}
            <div className="template-table-wrapper">
              <table className="template-table">
                <thead>
                  <tr>
                    <th>
                      <div className="header-with-icon">
                        <Text appearance="body-xs-bold" color="primary-grey-80">
                          Scheduler Name
                        </Text>
                      </div>
                    </th>
                    <th>
                      <div className="header-with-icon">
                        <Text appearance="body-xs-bold" color="primary-grey-80">
                          Total
                        </Text>
                      </div>
                    </th>
                    <th>
                      <div className="header-with-icon">
                        <Text appearance="body-xs-bold" color="primary-grey-80">
                          Success
                        </Text>
                      </div>
                    </th>
                    <th>
                      <div className="header-with-icon">
                        <Text appearance="body-xs-bold" color="primary-grey-80">
                          Failed
                        </Text>
                      </div>
                    </th>
                    <th>
                      <div className="header-with-icon">
                        <Text appearance="body-xs-bold" color="primary-grey-80">
                          Running
                        </Text>
                      </div>
                    </th>
                    <th>
                      <div className="header-with-icon">
                        <Text appearance="body-xs-bold" color="primary-grey-80">
                          Pending
                        </Text>
                      </div>
                    </th>
                    <th>
                      <div className="header-with-icon">
                        <Text appearance="body-xs-bold" color="primary-grey-80">
                          Success Rate
                        </Text>
                      </div>
                    </th>
                    <th>
                      <div className="header-with-icon">
                        <Text appearance="body-xs-bold" color="primary-grey-80">
                          Last Execution
                        </Text>
                      </div>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {(() => {
                    // Aggregate data by jobName
                    const aggregatedData = {};
                    
                    schedulerData.data.forEach(item => {
                      const jobName = item.jobName || 'Unknown';
                      
                      if (!aggregatedData[jobName]) {
                        aggregatedData[jobName] = {
                          jobName,
                          totalExecutions: 0,
                          successCount: 0,
                          failureCount: 0,
                          runningCount: 0,
                          pendingCount: 0,
                          lastExecutionTime: item.timestamp
                        };
                      }
                      
                      aggregatedData[jobName].totalExecutions++;
                      
                      if (item.status === 'SUCCESS') {
                        aggregatedData[jobName].successCount++;
                      } else if (item.status === 'FAILED' || item.status === 'FAILURE' || item.errorCount > 0) {
                        aggregatedData[jobName].failureCount++;
                      } else if (item.status === 'RUNNING' || item.status === 'IN_PROGRESS') {
                        aggregatedData[jobName].runningCount++;
                      } else if (item.status === 'PENDING' || item.status === 'QUEUED') {
                        aggregatedData[jobName].pendingCount++;
                      }
                      
                      // Keep the most recent timestamp
                      if (item.timestamp && (!aggregatedData[jobName].lastExecutionTime || 
                          new Date(item.timestamp) > new Date(aggregatedData[jobName].lastExecutionTime))) {
                        aggregatedData[jobName].lastExecutionTime = item.timestamp;
                      }
                    });
                    
                    const schedulerList = Object.values(aggregatedData);
                    
                    if (schedulerList.length === 0) {
                      return (
                        <tr>
                          <td colSpan="8" style={{ textAlign: 'center', padding: '40px' }}>
                            <Text appearance="body-m" color="primary-grey-80">
                              No scheduler data available
                            </Text>
                          </td>
                        </tr>
                      );
                    }
                    
                    return schedulerList.map((scheduler, index) => {
                      const rate = scheduler.totalExecutions > 0 
                        ? ((scheduler.successCount / scheduler.totalExecutions) * 100).toFixed(1)
                        : 0;
                      return (
                        <tr key={index}>
                          <td>
                            <Text appearance="body-xs" color="black">
                              {scheduler.jobName}
                            </Text>
                          </td>
                          <td>
                            <Text appearance="body-xs" color="black">
                              {scheduler.totalExecutions}
                            </Text>
                          </td>
                          <td>
                            <Text appearance="body-xs" color="#43a047" style={{ fontWeight: '600' }}>
                              {scheduler.successCount}
                            </Text>
                          </td>
                          <td>
                            <Text appearance="body-xs" color="#e53935" style={{ fontWeight: '600' }}>
                              {scheduler.failureCount}
                            </Text>
                          </td>
                          <td>
                            <Text appearance="body-xs" color="#ff9800" style={{ fontWeight: '600' }}>
                              {scheduler.runningCount}
                            </Text>
                          </td>
                          <td>
                            <Text appearance="body-xs" color="#757575" style={{ fontWeight: '600' }}>
                              {scheduler.pendingCount}
                            </Text>
                          </td>
                          <td>
                            <span className={`status-badge ${rate >= 90 ? 'status-published' : rate >= 70 ? 'status-draft' : 'status-inactive'}`}>
                              {rate}%
                            </span>
                          </td>
                          <td>
                            <Text appearance="body-xs" color="black">
                              {scheduler.lastExecutionTime 
                                ? new Date(scheduler.lastExecutionTime).toLocaleDateString()
                                : 'N/A'}
                            </Text>
                          </td>
                        </tr>
                      );
                    });
                  })()}
                </tbody>
              </table>
            </div>
          </>
        )}
    </div>
  );
};

export default SchedulerStats;

