import React, { useState, useEffect } from "react";
import { Text, ActionButton, Select, Checkbox } from "../custom-components";
import { useSelector, useDispatch } from "react-redux";
import { useNavigate } from "react-router-dom";
import "../styles/pageConfiguration.css";
import "../styles/dpoDashboard.css";
import { generateTransactionId } from "../utils/transactionId";
import config from "../utils/config";
import { IcEngineeringRequest, IcDatabase, IcSnake, IcPlan, IcFlipVertical, IcTime, IcDataLoan } from "../custom-components/Icon";
import { getUserProfile, fetchBusinessApplication, fetchBusinessApplications, translateConfig, getSystemConfig } from "../store/actions/CommonAction";
import { CLEAR_SESSION } from "../store/constants/Constants";
import { isSandboxMode } from "../utils/sandboxMode";
import "../styles/sessionModal.css";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import GuidedTour from "./GuidedTour";
import TourButton from "./TourButton";
import { dpoDashboardTourSteps } from "../utils/tourSteps";

/**
 * Actual API Response Structure:
 * {
 *   totalPurposeCreated: number,
 *   dataTypes: number,
 *   processingActivities: number,
 *   retentionPeriod: number (days),
 *   logs: number,
 *   consentArtefacts: number,
 *   publishedTemp: number,
 *   pendingRenewal: number,
 *   cookiesPublished: number,
 *   cookiesDraft: number,
 *   cookiesInactive: number,
 *   totalConsents: number,
 *   activeConsents: number,
 *   revokedConsents: number,
 *   expiredConsents: number,
 *   notificationSent: number,
 *   notificationEmail: number,
 *   notificationSms: number,
 *   cookiesTotal: number,
 *   cookiesAllAccepted: number,
 *   cookiesPartiallyAccepted: number,
 *   cookiesAllRejected: number,
 *   cookiesNoAction: number,
 *   grievanceTotalRequests: number,
 *   grievanceResolved: number,
 *   grievanceInProgress: number,
 *   grievanceEscalated: number,
 *   grievanceRejected: number,
 *   resolvedSla: number,
 *   exceededSla: number,
 *   grievanceSms: number,
 *   grievanceEmail: number
 * }
 */

const DPODashboard = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const tenantId = useSelector((state) => state.common.tenant_id);
  const sessionToken = useSelector((state) => state.common.session_token);
  const businessId = useSelector((state) => state.common.business_id);
  const userProfile = useSelector((state) => state.common.user_profile);
  const [showSessionModal, setShowSessionModal] = useState(false);

  const [selectedPeriod, setSelectedPeriod] = useState("Last 90 days");
  const [dashboardData, setDashboardData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isInitialLoad, setIsInitialLoad] = useState(true);

  // Calculate date range based on selected period
  const getDateRange = (period) => {
    const toDate = new Date();
    const fromDate = new Date();
    
    switch(period) {
      case "Last 7 days":
        fromDate.setDate(toDate.getDate() - 7);
        break;
      case "Last 30 days":
        fromDate.setDate(toDate.getDate() - 30);
        break;
      case "Last 60 days":
        fromDate.setDate(toDate.getDate() - 60);
        break;
      case "Last 90 days":
        fromDate.setDate(toDate.getDate() - 90);
        break;
      default:
        // Default to Last 90 days if period doesn't match
        fromDate.setDate(toDate.getDate() - 90);
    }
    
    return {
      fromDate: fromDate.toISOString().split('T')[0],
      toDate: toDate.toISOString().split('T')[0]
    };
  };

  // Fetch DPO Dashboard Data
  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);
      
      // In sandbox mode, use default values if credentials aren't ready
      const finalTenantId = tenantId || (isSandboxMode() ? 'sandbox-tenant-id' : '');
      const finalSessionToken = sessionToken || (isSandboxMode() ? 'sandbox-session-token-12345' : '');
      const finalBusinessId = businessId || (isSandboxMode() ? 'sandbox-business-id' : '');
      
      if (!finalTenantId || !finalSessionToken || !finalBusinessId) {
        console.warn("Missing credentials for dashboard fetch");
        if (!isSandboxMode()) {
          setError("Missing credentials. Please log in again.");
          setLoading(false);
          return;
        }
      }
      
      const { fromDate, toDate } = getDateRange(selectedPeriod);
      const txnId = generateTransactionId();
      
      const response = await fetch(
        `${config.dpo_dashboard}?fromDate=${fromDate}&toDate=${toDate}`,
        {
          method: 'GET',
          headers: {
            'txn': txnId,
            'tenant-id': finalTenantId,
            'business-id': finalBusinessId,
            'x-session-token': finalSessionToken,
            'Content-Type': 'application/json'
          }
        }
      );

      if (!response.ok) {
        throw new Error(`API error: ${response.status} ${response.statusText}`);
      }

      const data = await response.json();
      console.log("Dashboard API Response:", JSON.stringify(data, null, 2));
      console.log("Dashboard data structure:", data);
      setDashboardData(data);
    } catch (err) {
      console.error("Error fetching dashboard data:", err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Fetch user profile first, then company logo (which needs businessId)
  useEffect(() => {
    if (!tenantId) {
      console.warn(" Tenant ID is missing in Redux, cannot call API.");
      return;
    }

    console.log(" Tenant ID from Redux:", tenantId);

    const fetchUserProfile = async () => {
      try {
        let res = await dispatch(getUserProfile(tenantId, sessionToken));
        console.log("Response in dashboard profile call", res);

        if (res === 401 || res?.status === 401) {
          // Skip session expiration handling in sandbox mode
          if (isSandboxMode()) {
            console.log('🏖️ SANDBOX MODE - Ignoring 401 error in DPO Dashboard');
            return;
          }
          
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Session expired"}
              />
            ),
            { icon: false }
          );
          dispatch({ type: CLEAR_SESSION });
          setShowSessionModal(true);
          setTimeout(() => {
            setShowSessionModal(false);
            navigate("/adminLogin");
          }, 7000);
        }
      } catch (error) {
        console.error("Error in profile call", error);

        if (
          error === 401 ||
          error[0]?.errorCode === "JCMP4003" ||
          error[0]?.errorCode === "JCMP4001"
        ) {
          // Skip session expiration handling in sandbox mode
          if (isSandboxMode()) {
            console.log('🏖️ SANDBOX MODE - Ignoring session error in DPO Dashboard');
            return;
          }
          
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Session expired"}
              />
            ),
            { icon: false }
          );
          dispatch({ type: CLEAR_SESSION });
          setShowSessionModal(true);
          setTimeout(() => {
            setShowSessionModal(false);
            navigate("/adminLogin");
          }, 7000);
        }
      }
    };

    // Fetch user profile first to get businessId
    fetchUserProfile();

  }, [dispatch, tenantId, sessionToken, navigate]);

  // Fetch company logo after businessId is available
  useEffect(() => {
    if (!businessId || !tenantId || !sessionToken) {
      return;
    }

    const fetchCompanyLogo = async () => {
      try {
        console.log('Fetching company logo with businessId:', businessId);
        const response = await dispatch(getSystemConfig());
        const systemConfigInfo = response?.data?.searchList?.[0]?.configurationJson;
        
        if (systemConfigInfo?.logo) {
          try {
            // Store logo as base64 data URL for persistence across page reloads
            let logoDataUrl;
            
            if (systemConfigInfo.logo.startsWith('data:')) {
              // Already a data URL
              logoDataUrl = systemConfigInfo.logo;
            } else {
              // Convert base64 to data URL
              const contentType = systemConfigInfo.logoMeta?.contentType || 'image/png';
              logoDataUrl = `data:${contentType};base64,${systemConfigInfo.logo}`;
            }
            
            // Dispatch logo to Redux
            dispatch({
              type: "SAVE_COMPANY_LOGO",
              payload: logoDataUrl,
            });
            console.log('✅ Company logo saved as base64 data URL');
          } catch (error) {
            console.error("Error processing logo:", error);
            // Fallback: use base64 directly if it's already a data URL
            if (systemConfigInfo.logo.startsWith('data:')) {
              dispatch({
                type: "SAVE_COMPANY_LOGO",
                payload: systemConfigInfo.logo,
              });
            }
          }
        }
      } catch (error) {
        console.error("Error fetching company logo:", error);
      }
    };

    fetchCompanyLogo();
  }, [dispatch, businessId, tenantId, sessionToken]);

  // Fetch business details after business_id is set from user profile
  useEffect(() => {
    if (businessId && sessionToken) {
      console.log("Fetching business details for businessId:", businessId);
      
      const getBusinessDetails = async () => {
        try {
          await dispatch(fetchBusinessApplication(businessId, sessionToken));
          console.log("Business details fetched successfully");
        } catch (error) {
          console.error("Error fetching business details:", error);
        }
      };

      getBusinessDetails();
    }
  }, [businessId, sessionToken, dispatch]);

  useEffect(() => {
    if (userProfile) {
      console.log("📦 User profile from Redux:", userProfile);
      console.log("🏢 Extracted Business ID:", businessId);
    }
  }, [userProfile, businessId]);

  // Fetch business applications
  useEffect(() => {
    dispatch(fetchBusinessApplications(sessionToken, tenantId));
  }, [dispatch, sessionToken, tenantId]);

  // Call translateConfig
  // useEffect(() => {
  //   // Only call translateConfig when businessId and token are available
  //   if (!businessId || !sessionToken || !tenantId) {
  //     console.log("Waiting for businessId, token, or tenantId...", { businessId, sessionToken, tenantId });
  //     return;
  //   }

  //   const callTranslateConfig = async () => {
  //     try {
  //       console.log("Business ID: in use effect", businessId);
        
  //       const data = await translateConfig(tenantId, businessId, sessionToken);
  //       console.log("Response from translateConfig:", data);
  //     } catch (error) {
  //       console.error("Failed to call translateConfig:", error);
  //     } 
  //   };

  //   callTranslateConfig();
  // }, [businessId, sessionToken, tenantId]);

  // Fetch DPO Dashboard Data
  useEffect(() => {
    console.log("DPO Dashboard mounted or dependencies changed", { tenantId, sessionToken, businessId, selectedPeriod });
    
    // Reset loading state when navigating back to dashboard
    if (!isInitialLoad && dashboardData === null) {
      setLoading(true);
    }
    
    // In sandbox mode, use default values if credentials aren't ready
    const finalTenantId = tenantId || (isSandboxMode() ? 'sandbox-tenant-id' : null);
    const finalSessionToken = sessionToken || (isSandboxMode() ? 'sandbox-session-token-12345' : null);
    const finalBusinessId = businessId || (isSandboxMode() ? 'sandbox-business-id' : null);
    
    if (finalTenantId && finalSessionToken && finalBusinessId) {
      // Use a small delay to ensure state is ready, especially when navigating back
      const timer = setTimeout(() => {
      fetchDashboardData();
        setIsInitialLoad(false);
      }, 100);
      return () => clearTimeout(timer);
    } else if (!isSandboxMode()) {
      // In non-sandbox mode, wait for credentials
      setLoading(false);
    }
  }, [tenantId, sessionToken, businessId, selectedPeriod]);

  // Donut Chart Component
  const DonutChart = ({ data, total, centerLabel, size = 200, showPercentages = false }) => {
    const outerRadius = 85;
    const innerRadius = 70;
    const centerX = 100;
    const centerY = 100;
    
    let currentAngle = -90; // Start from top
    const segments = [];
    const percentageLabels = [];
    
    // Check if there's any data with percentage > 0
    const hasData = data && data.length > 0 && data.some(item => item.percentage > 0);
    
    // Create all slices first
    data && data.forEach((item, index) => {
      if (item.percentage === 0) return;
      
      const percentage = item.percentage;
      const color = item.color;
      const angle = (percentage / 100) * 360;
      const startAngle = currentAngle;
      const endAngle = currentAngle + angle;
      const midAngle = startAngle + angle / 2;
      
      // Convert to radians
      const startRad = (startAngle * Math.PI) / 180;
      const endRad = (endAngle * Math.PI) / 180;
      const midRad = (midAngle * Math.PI) / 180;
      
      // Outer arc points
      const x1Outer = centerX + outerRadius * Math.cos(startRad);
      const y1Outer = centerY + outerRadius * Math.sin(startRad);
      const x2Outer = centerX + outerRadius * Math.cos(endRad);
      const y2Outer = centerY + outerRadius * Math.sin(endRad);
      
      // Inner arc points
      const x1Inner = centerX + innerRadius * Math.cos(startRad);
      const y1Inner = centerY + innerRadius * Math.sin(startRad);
      const x2Inner = centerX + innerRadius * Math.cos(endRad);
      const y2Inner = centerY + innerRadius * Math.sin(endRad);
      
      const largeArc = angle > 180 ? 1 : 0;
      
      // Create donut segment path
      const path = [
        `M ${x1Outer} ${y1Outer}`,
        `A ${outerRadius} ${outerRadius} 0 ${largeArc} 1 ${x2Outer} ${y2Outer}`,
        `L ${x2Inner} ${y2Inner}`,
        `A ${innerRadius} ${innerRadius} 0 ${largeArc} 0 ${x1Inner} ${y1Inner}`,
        'Z'
      ].join(' ');
      
      segments.push(
        <path key={`slice-${index}`} d={path} fill={color} />
      );
      
      // Calculate label position (outside the donut) - only show if > 5% to avoid crowding
      if (showPercentages && percentage >= 5) {
        const labelRadius = outerRadius + 15;
        const labelX = centerX + labelRadius * Math.cos(midRad);
        const labelY = centerY + labelRadius * Math.sin(midRad);
        percentageLabels.push(
          <text
            key={`label-${index}`}
            x={labelX}
            y={labelY}
            textAnchor="middle"
            dominantBaseline="middle"
            fontSize="11"
            fontWeight="700"
            fill={color}
          >
            {percentage}%
          </text>
        );
      }
      
      currentAngle = endAngle;
    });

    // Create grey background donut (always visible)
    const greyDonutPath = (() => {
      const outerCircle = `M ${centerX + outerRadius} ${centerY} A ${outerRadius} ${outerRadius} 0 1 1 ${centerX - outerRadius} ${centerY} A ${outerRadius} ${outerRadius} 0 1 1 ${centerX + outerRadius} ${centerY}`;
      const innerCircle = `M ${centerX + innerRadius} ${centerY} A ${innerRadius} ${innerRadius} 0 1 0 ${centerX - innerRadius} ${centerY} A ${innerRadius} ${innerRadius} 0 1 0 ${centerX + innerRadius} ${centerY}`;
      return `${outerCircle} ${innerCircle} Z`;
    })();

  return (
      <div className="donut-chart" style={{ width: size, height: size }}>
        <svg viewBox="0 0 200 200" width={size} height={size} style={{ overflow: 'visible' }}>
          {/* Always show grey background donut */}
          <path 
            d={greyDonutPath}
            fill="#f5f5f5" 
            stroke="#e5e5e5" 
            strokeWidth="1"
            fillRule="evenodd"
          />
          
          {hasData && (
            <>
              <g>{segments}</g>
              {showPercentages && <g>{percentageLabels}</g>}
            </>
          )}
        </svg>
        {(total || centerLabel) && (
          <div className="donut-center">
            {total && <div className="donut-center-value">{total}</div>}
            {centerLabel && <div className="donut-center-label">{centerLabel}</div>}
          </div>
        )}
      </div>
    );
  };

  // Helper function to format retention period from days to human-readable format
  // Handles both API response format (e.g., "10 year", "10 month") and numeric days
  const formatRetentionPeriod = (value) => {
    if (!value || value === 'N/A' || value === 0) return 'N/A';
    
    // Check if value is already a formatted string (contains year/month/day)
    if (typeof value === 'string') {
      const lowerValue = value.toLowerCase().trim();
      
      // Check if it already contains time unit keywords
      if (lowerValue.includes('year') || lowerValue.includes('month') || lowerValue.includes('day')) {
        // Return as-is, but normalize pluralization if needed
        // API might return "10 year" or "10 years", we'll keep it as-is
        return value.trim();
      }
      
      // If it's a string but doesn't contain time units, try to parse as number
      const daysNum = parseInt(value, 10);
      if (!isNaN(daysNum)) {
        // Convert days to years, months, or days
        const years = Math.floor(daysNum / 365);
        const months = Math.floor((daysNum % 365) / 30);
        const remainingDays = daysNum % 30;
        
        if (years > 0) {
          return `${years} ${years === 1 ? 'year' : 'years'}`;
        } else if (months > 0) {
          return `${months} ${months === 1 ? 'month' : 'months'}`;
        } else {
          return `${remainingDays} ${remainingDays === 1 ? 'day' : 'days'}`;
        }
      }
    }
    
    // If value is a number, convert from days
    if (typeof value === 'number') {
      const daysNum = value;
      const years = Math.floor(daysNum / 365);
      const months = Math.floor((daysNum % 365) / 30);
      const remainingDays = daysNum % 30;
      
      if (years > 0) {
        return `${years} ${years === 1 ? 'year' : 'years'}`;
      } else if (months > 0) {
        return `${months} ${months === 1 ? 'month' : 'months'}`;
      } else {
        return `${remainingDays} ${remainingDays === 1 ? 'day' : 'days'}`;
      }
    }
    
    // If we can't parse it, return as-is or 'N/A'
    return value || 'N/A';
  };

  // Helper functions to safely extract data from API response
  const getData = (path, defaultValue = 0) => {
    if (!dashboardData) return defaultValue;
    
    // Handle direct access (no dots in path)
    if (!path.includes('.')) {
      const value = dashboardData[path];
      // Explicitly check for null/undefined, but allow 0 and other falsy numbers
      return value !== null && value !== undefined ? value : defaultValue;
    }
    
    // Handle nested path access
    const value = path.split('.').reduce((obj, key) => {
      return obj && obj[key] !== undefined ? obj[key] : null;
    }, dashboardData);
    
    // Explicitly check for null/undefined, but allow 0 and other falsy numbers
    return value !== null && value !== undefined ? value : defaultValue;
  };

  const calculatePercentage = (value, total) => {
    if (!total || total === 0) return 0;
    return Math.round((value / total) * 100);
  };

  // Helper to get the entire data object (for debugging)
  const getApiData = () => dashboardData || {};

  // Loading state
  if (loading) {
    return (
      <div className="configurePage">
        <div className="dpo-dashboard-container">
          <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '400px' }}>
            <div style={{ fontSize: '18px', color: '#666' }}>Loading dashboard data...</div>
          </div>
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="configurePage">
        <div className="dpo-dashboard-container">
          <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', height: '400px', gap: '16px' }}>
            <div style={{ fontSize: '18px', color: '#f44336' }}>Error loading dashboard data</div>
            <div style={{ fontSize: '14px', color: '#666' }}>{error}</div>
            <button 
              onClick={fetchDashboardData} 
              style={{ padding: '8px 16px', cursor: 'pointer', borderRadius: '4px', border: '1px solid #ccc', background: '#fff' }}
            >
              Retry
            </button>
          </div>
              </div>
            </div>
    );
  }

  return (
    <>
      {/* Don't show session modal in sandbox mode */}
      {showSessionModal && !isSandboxMode() && (
        <div className="session-timeout-overlay">
          <div className="session-timeout-modal">
            <Text appearance="heading-s" color="feedback_error_50">
              Session Time Out
            </Text>
            <br></br>
            <Text appearance="body-s" color="primary-80">
              Your session has expired. Please log in again.
              </Text>
            </div>
        </div>
      )}
      <div className="configurePage">
        <div className="dpo-dashboard-container">
        {/* Header Section */}
        <div className="dashboard-header">
          <Text appearance="heading-s" className="dashboard-title">Dashboard</Text>
          <div className="dashboard-header-actions">
          <div className="dropdown-group">
            
            <select
              id="business-dropdown"
              value={selectedPeriod}
              onChange={(e) => setSelectedPeriod(e.target.value)}
            >
                    <option>Last 7 days</option>
                    <option>Last 30 days</option>
                    <option>Last 60 days</option>
                    <option>Last 90 days</option>
            </select>
          </div>

            <button className="audit-button" onClick={() => navigate("/audit-compliance")}>
              Audit & reporting
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6M15 3h6v6M10 14L21 3"/>
              </svg>
            </button>
        </div>
              </div>

        {/* Data Processing Register Stats */}
        <div className="register-section" data-tour="stats-section">
          <Text appearance="body-m-bold" color="primary-grey-80">Data processing register</Text>
          {/* <h2 className="section-title">Data processing register</h2> */}
          <div className="dpo-stats-grid" style={{marginTop:10}}>
            <div className="dpo-stat-card dpo-stat-blue">
              <div className="dpo-stat-icon dpo-blue-icon">
                <IcEngineeringRequest width={24} height={24} />
            </div>
            <div className="dpo-stat-content">
              <Text appearance="heading-xs">{getData('totalPurposeCreated', 0)}</Text>
                <Text appearance="body-xs" color="primary-grey-80">Total purposes created</Text>
            </div>
          </div>

            <div className="dpo-stat-card dpo-stat-purple">
              <div className="dpo-stat-icon dpo-purple-icon">
                <IcDatabase width={24} height={24} />
            </div>
            <div className="dpo-stat-content">
              <Text appearance="heading-xs">{getData('dataTypes', 0)}</Text>
                <Text appearance="body-xs" color="primary-grey-80">Data Types & Items</Text>
               
              </div>
            </div>

            <div className="dpo-stat-card dpo-stat-pink">
              <div className="dpo-stat-icon dpo-pink-icon">
                <IcSnake width={24} height={24} />
              </div>
              <div className="dpo-stat-content">
                <Text appearance="heading-xs">{getData('processingActivities', 0)}</Text>
                <Text appearance="body-xs" color="primary-grey-80">Processing Activities</Text>
            </div>
          </div>

            <div className="dpo-stat-card dpo-stat-green">
              <div className="dpo-stat-icon dpo-green-icon">
                <IcDataLoan width={24} height={24} />
              </div>
              <div className="dpo-stat-content">
                <Text appearance="heading-xs">{String(getData('dataProcessor', 0))}</Text>
                <Text appearance="body-xs" color="primary-grey-80">Data Processor</Text>
              </div>
            </div>

            <div className="dpo-stat-card dpo-stat-orange">
              <div className="dpo-stat-icon dpo-orange-icon">
                <IcTime width={24} height={24} />
            </div>
            <div className="dpo-stat-content">
              <Text appearance="heading-xs">{formatRetentionPeriod(getData('dataretentionPeriod', 'N/A'))}</Text>
                <Text appearance="body-xs" color="primary-grey-80">Data Retention</Text>
              
              </div>
            </div>

            <div className="dpo-stat-card dpo-stat-indigo">
              <div className="dpo-stat-icon dpo-indigo-icon">
                <IcPlan width={24} height={24} />
              </div>
              <div className="dpo-stat-content">
              <Text appearance="heading-xs">{formatRetentionPeriod(getData('logsretentionPeriod', 'N/A'))}</Text>
                <Text appearance="body-xs" color="primary-grey-80">Log Retention Period</Text>
               
            </div>
          </div>

            <div className="dpo-stat-card dpo-stat-teal">
              <div className="dpo-stat-icon dpo-teal-icon">
                <IcFlipVertical width={24} height={24} />
            </div>
            <div className="dpo-stat-content">
              <Text appearance="heading-xs">{formatRetentionPeriod(getData('consentArtefactsretentionPeriod', 'N/A'))}</Text>
                <Text appearance="body-xs" color="primary-grey-80">Consent Artefact Retention Period</Text>
               
              </div>
            </div>
          </div>
        </div>

      {/* Consents Section */}
        <div className="content-grid" data-tour="consent-section">
          <div className="content-column">
            <div className="section-header-with-info">
              <Text appearance="body-m-bold" color="primary-grey-80">Consents</Text>
            </div>
            <div className="content-section">
            
            
            {/* Templates Cards */}
            <div className="templates-header">
              <Text appearance="body-s-bold" color="primary-grey-80">Templates</Text>
              <div className="info-icon-circle">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="#666">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
                </svg>
              </div>
            </div>

            <div className="template-cards-row">
              <div className="template-card template-success">
                <div className="template-icon template-icon-success">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="white">
                    <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                  </svg>
                </div>
                <div className="template-info">
                  <Text appearance="body-l-bold" color="primary-grey-100">{String(getData('publishedTemp', 0))}</Text>
                  <Text appearance="body-xxs" color="primary-grey-60">Published templates</Text>
                </div>
              </div>

              <div className="template-card template-warning">
                <div className="template-icon template-icon-warning">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="white">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
                  </svg>
                </div>
                <div className="template-info">
                  <Text appearance="body-l-bold" color="primary-grey-100">{String(getData('pendingRequests', 0))}</Text>
                  <Text appearance="body-xxs" color="primary-grey-60">Pending requests</Text>
                </div>
              </div>
            </div>

            {/* Consents Stats */}
            <div className="consents-stats-header">
              <Text appearance="body-s-bold" color="primary-grey-80">Consents</Text>
              <div className="info-icon-circle">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="#666">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
                </svg>
            </div>
          </div>

            <div className="consents-stats-container">
              <div className="consents-chart">
                {(() => {
                  const total = getData('totalConsents', 0);
                  const active = getData('activeConsents', 0);
                  const revoked = getData('revokedConsents', 0);
                  const pending = getData('pendingRenewal', 0);
                  const autoRenew = getData('autorenewalConsents', 0);
                  
                  // Calculate total excluding pending renewal - only consider active, auto renewal, and revoked
                  const calculatedTotal = active + autoRenew + revoked;
                  
                  // If calculated total is 0, show empty chart
                  if (calculatedTotal === 0) {
                  return (
                    <DonutChart
                        data={[]}
                        total="0"
                      centerLabel="Total consents"
                      size={200}
                        showPercentages={false}
                      />
                    );
                  }
                  
                  // Build data array, only including segments with values > 0
                  const chartData = [];
                  
                  // If only one segment has data, show it as 100%
                  if (active > 0 && autoRenew === 0 && revoked === 0) {
                    chartData.push({ percentage: 100, color: '#22c55e' });
                  } else if (autoRenew > 0 && active === 0 && revoked === 0) {
                    chartData.push({ percentage: 100, color: '#14b8a6' });
                  } else if (revoked > 0 && active === 0 && autoRenew === 0) {
                    chartData.push({ percentage: 100, color: '#ef4444' });
                  } else {
                    // Calculate percentages based on calculated total
                    const activePercent = calculatePercentage(active, calculatedTotal);
                    const autoRenewPercent = calculatePercentage(autoRenew, calculatedTotal);
                    const revokedPercent = calculatePercentage(revoked, calculatedTotal);
                    
                    // Ensure percentages don't exceed 100% by capping each at 100
                    const cappedActive = Math.min(activePercent, 100);
                    const cappedAutoRenew = Math.min(autoRenewPercent, 100);
                    const cappedRevoked = Math.min(revokedPercent, 100);
                    
                    // Add segments with values > 0
                    if (active > 0) {
                      chartData.push({ percentage: cappedActive, color: '#22c55e' });
                    }
                    if (autoRenew > 0) {
                      chartData.push({ percentage: cappedAutoRenew, color: '#14b8a6' });
                    }
                    if (revoked > 0) {
                      chartData.push({ percentage: cappedRevoked, color: '#ef4444' });
                    }
                    
                    // Normalize to ensure total doesn't exceed 100%
                    const totalPercent = cappedActive + cappedAutoRenew + cappedRevoked;
                    if (totalPercent > 100 && chartData.length > 0) {
                      // Scale down proportionally
                      const scale = 100 / totalPercent;
                      chartData.forEach(item => {
                        item.percentage = Math.round(item.percentage * scale);
                      });
                    }
                  }
                  
                  return (
                    <DonutChart
                      data={chartData}
                      total={calculatedTotal.toString()}
                      centerLabel="Total consents"
                      size={200}
                      showPercentages={calculatedTotal > 0}
                    />
                  );
                })()}
              </div>

              <div className="consents-stats-grid">
                <div className="consent-stat-item">
                  <div className="consent-stat-icon consent-stat-success">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="white">
                      <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                    </svg>
                  </div>
                  <div className="consent-stat-details">
                    <Text appearance="body-m-bold" color="primary-grey-100">{String(getData('activeConsents', 0))}</Text>
                    <Text appearance="body-xxs" color="primary-grey-60">Active consents</Text>
                  </div>
                </div>

                <div className="consent-stat-item">
                  <div className="consent-stat-icon consent-stat-info">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="white">
                      <path d="M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46C19.54 15.03 20 13.57 20 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74C4.46 8.97 4 10.43 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z"/>
                    </svg>
                  </div>
                  <div className="consent-stat-details">
                    <Text appearance="body-m-bold" color="primary-grey-100">{String(getData('autorenewalConsents', 0))}</Text>
                    <Text appearance="body-xxs" color="primary-grey-60">Auto renewal</Text>
                  </div>
                </div>

                <div className="consent-stat-item">
                  <div className="consent-stat-icon consent-stat-warning">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="white">
                      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
                    </svg>
                  </div>
                  <div className="consent-stat-details">
                    <Text appearance="body-m-bold" color="primary-grey-100">{String(getData('pendingRenewal', 0))}</Text>
                    <Text appearance="body-xxs" color="primary-grey-60">Pending renewal</Text>
                  </div>
                </div>

                <div className="consent-stat-item">
                  <div className="consent-stat-icon consent-stat-error">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="white">
                      <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
                    </svg>
                  </div>
                  <div className="consent-stat-details">
                    <Text appearance="body-m-bold" color="primary-grey-100">{String(getData('revokedConsents', 0))}</Text>
                    <Text appearance="body-xxs" color="primary-grey-60">Revoked consents</Text>
                  </div>
                </div>
              </div>
            </div>
            </div>
          </div>

          <div className="content-column" data-tour="cookies-section">
            <div className="section-header-with-info">
              <Text appearance="body-m-bold" color="primary-grey-80">Cookies</Text>
            </div>
            {/* Cookies Section */}
            <div className="content-section">
           
            
            {/* Registered Cookies */}
            <div className="templates-header">
              <Text appearance="body-s-bold" color="primary-grey-80">Registered cookies</Text>
              <div className="info-icon-circle">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="#666">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
                </svg>
              </div>
            </div>
            
            <div className="template-cards-row" style={{marginBottom: '12px', width: '50%'}}>
              <div className="template-card template-success">
                <div className="template-icon template-icon-success">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="white">
                    <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                  </svg>
                </div>
                <div className="template-info">
                  <Text appearance="body-l-bold" color="primary-grey-100">{String(getData('cookiesPublished', 0))}</Text>
                  <Text appearance="body-xxs" color="primary-grey-60">Published cookies</Text>
                </div>
              </div>
            </div>

            {/* Cookie Consents Stats */}
            <div className="consents-stats-header">
              <Text appearance="body-s-bold" color="primary-grey-80">Cookie consents</Text>
              <div className="info-icon-circle">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="#666">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
                </svg>
              </div>
            </div>

            <div className="consents-stats-container">
              <div className="consents-chart">
                {(() => {
                  const total = getData('cookiesTotal', 0);
                  const allAccepted = getData('cookiesAllAccepted', 0);
                  const allRejected = getData('cookiesAllRejected', 0);
                  const partiallyAccepted = getData('cookiesPartiallyAccepted', 0);
                  const noAction = getData('cookiesNoAction', 0);
                  
                  const segmentsTotal = allAccepted + allRejected + partiallyAccepted + noAction;
                  const actualTotal = total > 0 ? total : segmentsTotal;
                  
                  // If no data, show empty chart
                  if (actualTotal === 0) {
                  return (
                    <DonutChart
                        data={[]}
                        total="0"
                        centerLabel="Total consents"
                        size={200}
                        showPercentages={false}
                      />
                    );
                  }
                  
                  // Build data array, only including segments with values > 0
                  const chartData = [];
                  
                  // If only one segment has data, show it as 100%
                  if (allAccepted > 0 && partiallyAccepted === 0 && noAction === 0 && allRejected === 0) {
                    chartData.push({ percentage: 100, color: '#22c55e' });
                  } else if (partiallyAccepted > 0 && allAccepted === 0 && noAction === 0 && allRejected === 0) {
                    chartData.push({ percentage: 100, color: '#3b82f6' });
                  } else if (noAction > 0 && allAccepted === 0 && partiallyAccepted === 0 && allRejected === 0) {
                    chartData.push({ percentage: 100, color: '#14b8a6' });
                  } else if (allRejected > 0 && allAccepted === 0 && partiallyAccepted === 0 && noAction === 0) {
                    chartData.push({ percentage: 100, color: '#f59e0b' });
                  } else {
                    // Calculate percentages based on actual total
                    const allAcceptedPercent = calculatePercentage(allAccepted, actualTotal);
                    const partiallyAcceptedPercent = calculatePercentage(partiallyAccepted, actualTotal);
                    const noActionPercent = calculatePercentage(noAction, actualTotal);
                    const allRejectedPercent = calculatePercentage(allRejected, actualTotal);
                    
                    // Ensure percentages don't exceed 100% by capping each at 100
                    const cappedAllAccepted = Math.min(allAcceptedPercent, 100);
                    const cappedPartiallyAccepted = Math.min(partiallyAcceptedPercent, 100);
                    const cappedNoAction = Math.min(noActionPercent, 100);
                    const cappedAllRejected = Math.min(allRejectedPercent, 100);
                    
                    // Add segments with values > 0
                    if (allAccepted > 0) {
                      chartData.push({ percentage: cappedAllAccepted, color: '#22c55e' });
                    }
                    if (partiallyAccepted > 0) {
                      chartData.push({ percentage: cappedPartiallyAccepted, color: '#3b82f6' });
                    }
                    if (noAction > 0) {
                      chartData.push({ percentage: cappedNoAction, color: '#14b8a6' });
                    }
                    if (allRejected > 0) {
                      chartData.push({ percentage: cappedAllRejected, color: '#f59e0b' });
                    }
                    
                    // Normalize to ensure total doesn't exceed 100%
                    const totalPercent = cappedAllAccepted + cappedPartiallyAccepted + cappedNoAction + cappedAllRejected;
                    if (totalPercent > 100 && chartData.length > 0) {
                      // Scale down proportionally
                      const scale = 100 / totalPercent;
                      chartData.forEach(item => {
                        item.percentage = Math.round(item.percentage * scale);
                      });
                    }
                  }
                  
                  return (
                    <DonutChart
                      data={chartData}
                      total={actualTotal.toString()}
                      centerLabel="Total consents"
                      size={200}
                      showPercentages={actualTotal > 0}
                    />
                  );
                })()}
              </div>

              <div className="consents-stats-grid">
                <div className="consent-stat-item">
                  <div className="consent-stat-icon consent-stat-success">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="white">
                      <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                    </svg>
                  </div>
                  <div className="consent-stat-details">
                    <Text appearance="body-m-bold" color="primary-grey-100">{String(getData('cookiesAllAccepted', 0))}</Text>
                    <Text appearance="body-xxs" color="primary-grey-60">All accepted</Text>
                  </div>
                </div>

                <div className="consent-stat-item">
                  <div className="consent-stat-icon consent-stat-info">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="white">
                      <path d="M12 2a10 10 0 100 20 10 10 0 000-20zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
                    </svg>
                  </div>
                  <div className="consent-stat-details">
                    <Text appearance="body-m-bold" color="primary-grey-100">{String(getData('cookiesPartiallyAccepted', 0))}</Text>
                    <Text appearance="body-xxs" color="primary-grey-60">Partially accepted</Text>
                  </div>
                </div>

                <div className="consent-stat-item">
                  <div className="consent-stat-icon consent-stat-warning">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="white">
                      <path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/>
                    </svg>
                  </div>
                  <div className="consent-stat-details">
                    <Text appearance="body-m-bold" color="primary-grey-100">{String(getData('cookiesNoAction', 0))}</Text>
                    <Text appearance="body-xxs" color="primary-grey-60">No action</Text>
                  </div>
                </div>

                <div className="consent-stat-item">
                  <div className="consent-stat-icon consent-stat-error">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="white">
                      <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
                    </svg>
                  </div>
                  <div className="consent-stat-details">
                    <Text appearance="body-m-bold" color="primary-grey-100">{String(getData('cookiesAllRejected', 0))}</Text>
                    <Text appearance="body-xxs" color="primary-grey-60">All rejected</Text>
                  </div>
                </div>
              </div>
              </div>
            </div>
          </div>
        </div>

        {/* Grievance Section */}
        <div className="section-header-with-info" data-tour="grievance-section">
              <Text appearance="body-m-bold" color="primary-grey-80">Grievance</Text>
            </div>
        <div className="grievance-container">
          <div className="grievance-column">
            <div className="section-header-with-info">
              <Text appearance="body-s-bold" color="primary-grey-80">Grievance requests</Text>
              <div className="info-icon-circle">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="#666">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
                </svg>
              </div>
            </div>
            <div className="grievance-content">
              <div className="consents-stats-container">
                <div className="consents-chart">
                  {(() => {
                    const total = getData('grievanceTotalRequests', 0);
                    const resolved = getData('grievanceResolved', 0);
                    const inProgress = getData('grievanceInProgress', 0);
                    const escalatedL1 = getData('grievanceEscalatedL1', 0);
                    const escalatedL2 = getData('grievanceEscalatedL2', 0);
                    const escalated = escalatedL1 + escalatedL2; // Total escalated
                    const rejected = getData('grievanceRejected', 0);
                    
                    const segmentsTotal = resolved + inProgress + escalated + rejected;
                    const actualTotal = total > 0 ? total : segmentsTotal;
                    
                    return (
                      <DonutChart
                        data={[
                          { percentage: calculatePercentage(resolved, actualTotal), color: '#22c55e' },
                          { percentage: calculatePercentage(rejected, actualTotal), color: '#ef4444' },
                          { percentage: calculatePercentage(escalated, actualTotal), color: '#f59e0b' },
                          { percentage: calculatePercentage(inProgress, actualTotal), color: '#0891b2' }
                        ]}
                        total={total > 0 ? total.toString() : segmentsTotal.toString()}
                        centerLabel="Total grievances"
                        size={200}
                        showPercentages={actualTotal > 0}
                      />
                    );
                  })()}
                </div>

                <div className="consents-stats-grid">
                  <div className="consent-stat-item">
                    <div className="consent-stat-icon" style={{ backgroundColor: '#0d7a5f' }}>
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="white">
                        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                      </svg>
                    </div>
                    <div className="consent-stat-details">
                      <Text appearance="body-m-bold" color="primary-grey-100">{String(getData('grievanceNew', 0))}</Text>
                      <Text appearance="body-xxs" color="primary-grey-60">New</Text>
                    </div>
                  </div>

                  <div className="consent-stat-item">
                    <div className="consent-stat-icon consent-stat-success">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="white">
                        <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                      </svg>
                    </div>
                    <div className="consent-stat-details">
                      <Text appearance="body-m-bold" color="primary-grey-100">{String(getData('grievanceResolved', 0))}</Text>
                      <Text appearance="body-xxs" color="primary-grey-60">Resolved</Text>
                    </div>
                  </div>

                  <div className="consent-stat-item">
                    <div className="consent-stat-icon consent-stat-info">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="white">
                        <path d="M6 4h4v16H6V4zm8 0h4v16h-4V4z"/>
                      </svg>
                    </div>
                    <div className="consent-stat-details">
                      <Text appearance="body-m-bold" color="primary-grey-100">{String(getData('grievanceInProgress', 0))}</Text>
                      <Text appearance="body-xxs" color="primary-grey-60">In Progress</Text>
                    </div>
                  </div>

                  <div className="consent-stat-item">
                    <div className="consent-stat-icon consent-stat-warning">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="white">
                        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
                      </svg>
                    </div>
                    <div className="consent-stat-details">
                      <Text appearance="body-m-bold" color="primary-grey-100">{String(getData('grievanceEscalatedL1', 0) + getData('grievanceEscalatedL2', 0))}</Text>
                      <Text appearance="body-xxs" color="primary-grey-60">Escalated</Text>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="grievance-column">
            <div className="section-header-with-info">
              <Text appearance="body-s-bold" color="primary-grey-80">Grievance SLA</Text>
              <div className="info-icon-circle">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="#666">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
                </svg>
              </div>
            </div>
            <div className="grievance-content">
              <div className="consents-stats-container">
                <div className="consents-chart">
                  {(() => {
                    const resolvedSla = getData('resolvedSla', 0);
                    const exceededSla = getData('exceededSla', 0);
                    const slaTotal = resolvedSla + exceededSla;
                    
                    return (
                      <DonutChart
                        data={[
                          { percentage: calculatePercentage(resolvedSla, slaTotal), color: '#22c55e' },
                          { percentage: calculatePercentage(exceededSla, slaTotal), color: '#f59e0b' }
                        ]}
                        total={String(slaTotal)}
                        centerLabel="Total"
                        size={200}
                        showPercentages={slaTotal > 0}
                      />
                    );
                  })()}
                </div>

                <div className="consents-stats-grid" style={{gridTemplateColumns: '1fr'}}>
                  <div className="consent-stat-item">
                    <div className="consent-stat-icon consent-stat-success">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="white">
                        <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                      </svg>
                    </div>
                    <div className="consent-stat-details">
                      <Text appearance="body-m-bold" color="primary-grey-100">{String(getData('resolvedSla', 0))}</Text>
                      <Text appearance="body-xxs" color="primary-grey-60">Resolved in SLA</Text>
                    </div>
                  </div>

                  <div className="consent-stat-item">
                    <div className="consent-stat-icon consent-stat-warning">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="white">
                        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
                      </svg>
              </div>
                    <div className="consent-stat-details">
                      <Text appearance="body-m-bold" color="primary-grey-100">{String(getData('exceededSla', 0))}</Text>
                      <Text appearance="body-xxs" color="primary-grey-60">Exceeded SLA</Text>
              </div>
              </div>
              </div>
              </div>
            </div>
          </div>
        </div>
        
        {/* Notifications Section */}
        <div data-tour="notifications-section">
        <Text appearance="body-m-bold" color="primary-grey-80" style={{marginTop: '16px', marginBottom: '12px'}}>Notifications</Text>
        <div className="notifications-white-container">
          <div className="notifications-grid-inner">
            <div className="notification-column">
              <div className="section-header-with-info">
                <Text appearance="body-s-bold" color="primary-grey-80">Consent</Text>
                <div className="info-icon-circle">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="#666">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
                </svg>
                </div>
              </div>
              <div className="notification-cards-row">
              <div className="notification-card">
                <div className="notification-icon-wrapper">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="#0891b2">
                    <path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/>
                  </svg>
                </div>
                <div className="notification-content">
                  <Text appearance="heading-m" color="primary-grey-100">{String(getData('notificationEmail', 0))}</Text>
                  <Text appearance="body-xxs" color="primary-grey-60">Email</Text>
            </div>
              </div>
              
              <div className="notification-card">
                <div className="notification-icon-wrapper">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="#0891b2">
                    <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/>
                  </svg>
              </div>
                <div className="notification-content">
                  <Text appearance="heading-m" color="primary-grey-100">{String(getData('notificationSms', 0))}</Text>
                  <Text appearance="body-xxs" color="primary-grey-60">SMS</Text>
              </div>
              </div>
            </div>
          </div>

            <div className="notification-column">
              <div className="section-header-with-info">
                <Text appearance="body-s-bold" color="primary-grey-80">Grievance</Text>
                <div className="info-icon-circle">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="#666">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
                  </svg>
                </div>
              </div>
              <div className="notification-cards-row">
              <div className="notification-card">
                <div className="notification-icon-wrapper">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="#0891b2">
                    <path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/>
                  </svg>
                </div>
                <div className="notification-content">
                  <Text appearance="heading-m" color="primary-grey-100">{String(getData('grievanceEmail', 0))}</Text>
                  <Text appearance="body-xxs" color="primary-grey-60">Email</Text>
                </div>
              </div>

              <div className="notification-card">
                <div className="notification-icon-wrapper">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="#0891b2">
                    <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/>
                  </svg>
                </div>
                <div className="notification-content">
                  <Text appearance="heading-m" color="primary-grey-100">{String(getData('grievanceSms', 0))}</Text>
                  <Text appearance="body-xxs" color="primary-grey-60">SMS</Text>
                </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        </div>
        </div>
    </div>
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
      
      {/* Guided Tour */}
      <GuidedTour 
        steps={dpoDashboardTourSteps} 
        tourId="dpo-dashboard-tour"
        showOnFirstVisit={true}
        onComplete={() => console.log('DPO Dashboard tour completed!')}
      />
      
      {/* Tour Help Button */}
      <TourButton tourId="dpo-dashboard-tour" label="Start Tour" position="bottom-right" />
    </>
  );
};

export default DPODashboard;


