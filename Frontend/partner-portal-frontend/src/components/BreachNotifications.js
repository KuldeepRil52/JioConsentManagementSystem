import React, { useState, useEffect } from "react";
import { Text, ActionButton, Icon } from "../custom-components";
import { useSelector } from "react-redux";
import { IcEditPen, IcTrash, IcSort, IcWarning } from "../custom-components/Icon";
import React, { useState, useEffect, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { Text, ActionButton, Button, Icon } from "../custom-components";
import { IcEditPen, IcTrash, IcSort, IcDownload, IcVisible, IcNotification } from "../custom-components/Icon";
import { Slide, ToastContainer, toast } from "react-toastify";
import Select from "react-select";
import CustomToast from "./CustomToastContainer";
import { generateTransactionId } from "../utils/transactionId";
import { exportToCSV } from "../utils/csvExport";
import config from "../utils/config";
import "../styles/pageConfiguration.css";
import "../styles/breachNotifications.css";
import "../styles/toast.css";
import { IcAlarmSensor } from '../custom-components/Icon';
import { IcWarningColored } from '../custom-components/Icon';
import { IcSuccessColored } from '../custom-components/Icon';

const BreachNotifications = () => {
  const navigate = useNavigate();
  const tenantId = useSelector((state) => state.common.tenant_id);
  const sessionToken = useSelector((state) => state.common.session_token);
  const businessId = useSelector((state) => state.common.business_id);

  const [stats, setStats] = useState({
    totalIncidents: 0,
    underInvestigation: 0,
    resolved: 0,
    affectedRecords: 0,
  });

  const [breachIncidents, setBreachIncidents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [selectedBreach, setSelectedBreach] = useState(null);
  const [newStatus, setNewStatus] = useState(null);
  const [remarks, setRemarks] = useState("");
  const [updatingStatus, setUpdatingStatus] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState('asc');

  // Status options
  const statusOptions = [
    { value: "NEW", label: "New" },
    { value: "INVESTIGATION", label: "Investigation" },
    { value: "NOTIFIED_TO_DATA_PRINCIPALS", label: "Notified to Data Principals" },
    { value: "NOTIFIED_TO_DPBI", label: "Notified to DPBI" },
    { value: "RESOLVED", label: "Resolved" },
  ];

  // Helper function to format datetime in IST (Indian Standard Time)
  const formatDateTime = (isoString) => {
    if (!isoString) return 'N/A';
    try {
      const date = new Date(isoString);

      // Convert to IST and format as DD-MM-YYYY HH:mm:ss
      const formatter = new Intl.DateTimeFormat('en-IN', {
        timeZone: 'Asia/Kolkata',
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      });

      const parts = formatter.formatToParts(date);
      const getValue = (type) => parts.find(p => p.type === type)?.value || '';

      const day = getValue('day');
      const month = getValue('month');
      const year = getValue('year');
      const hour = getValue('hour');
      const minute = getValue('minute');
      const second = getValue('second');

      return `${day}-${month}-${year} ${hour}:${minute}:${second}`;
    } catch (e) {
      return isoString;
    }
  };

  // Helper function to format breach type
  const formatBreachType = (type) => {
    if (!type) return 'N/A';
    // Convert UNAUTHORIZED_ACCESS to "Unauthorized Access"
    return type
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');
  };

  // Fetch breach notifications from API
  const fetchBreachNotifications = async () => {
    try {
      setLoading(true);
      setError(null);

      const txnId = generateTransactionId();

      const response = await fetch(
        `${config.data_breach_base}?businessId=${businessId}`,
        {
          method: 'GET',
          headers: {
            'accept': 'application/json',
            'txn': txnId,
            'tenant-id': tenantId,
            'business-id': businessId,
            'x-session-token': sessionToken,
            'Content-Type': 'application/json',
          },
        }
      );

      if (response.ok) {
        const data = await response.json();

        // Map API response to table format
        const breaches = data?.searchList || data || [];
        const mappedBreaches = breaches.map((breach, index) => ({
          id: breach.id || `BR-${index + 1}`,
          incidentId: breach.incidentId || `BR-${index + 1}`,
          status: breach.status || 'INVESTIGATION',
          discoveryDateTime: formatDateTime(breach.incidentDetails?.discoveryDateTime),
          occurrenceDateTime: formatDateTime(breach.incidentDetails?.occurrenceDateTime),
          breachType: formatBreachType(breach.incidentDetails?.breachType),
          briefDescription: breach.incidentDetails?.briefDescription || 'N/A',
          systemAffected: Array.isArray(breach.incidentDetails?.affectedSystemOrService)
            ? breach.incidentDetails.affectedSystemOrService.join(', ')
            : (breach.incidentDetails?.affectedSystemOrService || 'N/A'),
          dataCategories: Array.isArray(breach.dataInvolved?.personalDataCategories)
            ? breach.dataInvolved.personalDataCategories.join(', ')
            : 'N/A',
          affectedRecords: breach.dataInvolved?.affectedDataPrincipalsCount || 0,
          dataEncrypted: breach.dataInvolved?.dataEncryptedOrProtected ? 'Yes' : 'No',
          riskDescription: breach.dataInvolved?.potentialImpactDescription || 'N/A',
          dpbiNotificationDate: breach.notificationDetails?.dpbiNotificationDate
            ? formatDateTime(breach.notificationDetails.dpbiNotificationDate)
            : 'N/A',
          referenceId: breach.notificationDetails?.dpbiAcknowledgementId || 'N/A',
          dataPrincipalNotificationDate: breach.notificationDetails?.dataPrincipalNotificationDate
            ? formatDateTime(breach.notificationDetails.dataPrincipalNotificationDate)
            : 'N/A',
          notificationChannels: breach.notificationDetails?.channels
            ? breach.notificationDetails.channels.map(ch => ch.notificationChannel).join(', ')
            : 'N/A',
          rawData: breach, // Store raw data for edit/view
        }));

        setBreachIncidents(mappedBreaches);
        updateStats(mappedBreaches);
      } else {
        const errorText = await response.text();
        console.error("Failed to fetch breach notifications:", errorText);
        setError(`Failed to fetch breach notifications: ${response.status}`);
        // Reset stats to 0 on error
        updateStats([]);
      }
    } catch (err) {
      console.error("Error fetching breach notifications:", err);
      setError("Error fetching breach notifications. Please try again.");
      // Reset stats to 0 on error
      updateStats([]);
    } finally {
      setLoading(false);
    }
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

  const sortedBreachIncidents = useMemo(() => {
    if (!sortColumn) return breachIncidents;

    return [...breachIncidents].sort((a, b) => {
      const aValue = a[sortColumn] || '';
      const bValue = b[sortColumn] || '';

      if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }, [breachIncidents, sortColumn, sortDirection]);

  // Fetch data on component mount
  useEffect(() => {
    if (tenantId && sessionToken && businessId) {
      fetchBreachNotifications();
    }
  }, [tenantId, sessionToken, businessId]);

  // Handle Edit - Open status edit modal
  const handleEdit = (incidentId) => {
    const breach = sortedBreachIncidents.find(b => b.incidentId === incidentId);
    if (breach) {
      setSelectedBreach(breach);
      setNewStatus(statusOptions.find(opt => opt.value === breach.status));
      setRemarks("");
      setShowStatusModal(true);
    }
  };

  // Handle Update Status
  const handleUpdateStatus = async () => {
    if (!selectedBreach || !newStatus) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="Please select a status."
          />
        ),
        { icon: false }
      );
      return;
    }

    setUpdatingStatus(true);
    try {
      const txnId = generateTransactionId();

      // Call PUT API to update status
      const response = await fetch(
        `${config.data_breach_base}/${selectedBreach.incidentId}`,
        {
          method: 'PUT',
          headers: {
            'accept': 'application/json',
            'Content-Type': 'application/json',
            'tenant-id': tenantId,
            'business-id': businessId,
            'txn': txnId,
            'x-session-token': sessionToken,
          },
          body: JSON.stringify({
            status: newStatus.value,
            remarks: remarks || `${newStatus.label} for the data breach`,
          }),
        }
      );

      if (response.ok) {
        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message="Breach status updated successfully."
            />
          ),
          { icon: false }
        );
        setShowStatusModal(false);
        setSelectedBreach(null);
        setNewStatus(null);
        setRemarks("");
        // Refresh the list
        fetchBreachNotifications();
      } else {
        const errorText = await response.text();
        console.error("Failed to update breach status:", errorText);
        throw new Error("Failed to update status");
      }
    } catch (err) {
      console.error("Error updating breach status:", err);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={err.message || "Failed to update breach status. Please try again."}
          />
        ),
        { icon: false }
      );
    } finally {
      setUpdatingStatus(false);
    }
  };

  const handleCancelStatusEdit = () => {
    setShowStatusModal(false);
    setSelectedBreach(null);
    setNewStatus(null);
    setRemarks("");
  };

  const handleDelete = (id) => {
    const confirmDelete = window.confirm(
      "Are you sure you want to delete this breach notification?"
    );
    if (confirmDelete) {
      const updatedIncidents = sortedBreachIncidents.filter(
        (incident) => incident.id !== id
      );
      setBreachIncidents(updatedIncidents);

      // Update stats
      updateStats(updatedIncidents);
    }
  };

  const handleReportBreach = () => {
    navigate("/reportBreachEntry");
  };

  // Handle View - Fetch breach by ID and navigate to view mode
  const handleView = async (incidentId) => {
    try {
      const txnId = generateTransactionId();

      const response = await fetch(
        `${config.data_breach_base}/${incidentId}`,
        {
          method: 'GET',
          headers: {
            'accept': 'application/json',
            'tenant-id': tenantId,
            'business-id': businessId,
            'txn': txnId,
            'x-session-token': sessionToken,
            'Content-Type': 'application/json',
          },
        }
      );

      if (response.ok) {
        const breachData = await response.json();


        // Navigate to ReportBreachEntry with view mode
        navigate("/reportBreachEntry", {
          state: {
            viewMode: true,
            breachData: breachData,
          },
        });
      } else {
        const errorText = await response.text();
        console.error("Failed to fetch breach details:", errorText);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message="Failed to fetch breach details. Please try again."
            />
          ),
          { icon: false }
        );
      }
    } catch (err) {
      console.error("Error fetching breach details:", err);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="Error fetching breach details. Please try again."
          />
        ),
        { icon: false }
      );
    }
  };

  // Handle Notify DPBI
  const handleNotifyDPBI = (incidentId) => {
    toast.error(
      (props) => (
        <CustomToast
          {...props}
          type="error"
          message="Notify DPBI functionality will be implemented."
        />
      ),
      { icon: false }
    );
    // TODO: Implement Notify DPBI API call
  };

  // Handle Notify Data Principals
  const handleNotifyDataPrincipals = async (incidentId) => {

    const confirmNotify = window.confirm(
      "Are you sure you want to notify all affected data principals?"
    );

    if (confirmNotify) {
      try {
        const txnId = generateTransactionId();

        const response = await fetch(
          `${config.data_breach_notify}/${incidentId}`,
          {
            method: 'POST',
            headers: {
              'accept': 'application/json',
              'X-Tenant-Id': tenantId,
              'business-id': businessId,
              'x-session-token': sessionToken,
              'txn': txnId,
            },
          }
        );

        if (response.ok) {
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message="Data principals have been notified successfully."
              />
            ),
            { icon: false }
          );
          // Refresh the list
          fetchBreachNotifications();
        } else {
          const errorData = await response.json().catch(() => ({}));
          throw new Error(errorData.message || "Failed to notify data principals");
        }
      } catch (err) {
        console.error("Error notifying data principals:", err);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={err.message || "Failed to notify data principals. Please try again."}
            />
          ),
          { icon: false }
        );
      }
    }
  };

  const updateStats = (incidents) => {
    const totalIncidents = incidents.length;
    const underInvestigation = incidents.filter(
      (i) => i.status === "NEW" ||
        i.status === "INVESTIGATION" ||
        i.status === "NOTIFIED_TO_DATA_PRINCIPALS" ||
        i.status === "NOTIFIED_TO_DPBI"
    ).length;
    const resolved = incidents.filter(
      (i) => i.status === "RESOLVED"
    ).length;
    const affectedRecords = incidents.reduce(
      (sum, i) => sum + (i.affectedRecords || 0),
      0
    );

    setStats({
      totalIncidents,
      underInvestigation,
      resolved,
      affectedRecords,
    });
  };

  const getSeverityClass = (severity) => {
    switch (severity) {
      case "High":
        return "severity-high";
      case "Medium":
        return "severity-medium";
      case "Low":
        return "severity-low";
      default:
        return "";
    }
  };

  const getStatusClass = (status) => {
    switch (status) {
      case "NEW":
        return "status-new";
      case "INVESTIGATION":
        return "status-investigating";
      case "NOTIFIED_TO_DATA_PRINCIPALS":
        return "status-notified-principals";
      case "NOTIFIED_TO_DPBI":
        return "status-notified-dpbi";
      case "RESOLVED":
        return "status-resolved";
      default:
        return "";
    }
  };

  const formatStatus = (status) => {
    const statusMap = {
      "NEW": "New",
      "INVESTIGATION": "Investigation",
      "NOTIFIED_TO_DATA_PRINCIPALS": "Notified to Data Principals",
      "NOTIFIED_TO_DPBI": "Notified to DPBI",
      "RESOLVED": "Resolved",
    };
    return statusMap[status] || status;
  };

  const handleDownloadCSV = () => {
    if (!sortedBreachIncidents || sortedBreachIncidents.length === 0) {
      toast.error((props) => (<CustomToast {...props} type="error" message={"No data available to download"} />), { icon: false });
      return;
    }

    // Filter breachIncidents based on search query
    const filteredData = sortedBreachIncidents.filter((breach) => {
      if (!searchQuery) return true;
      const query = searchQuery.toLowerCase();
      return (
        breach.incidentId?.toLowerCase().includes(query) ||
        breach.status?.toLowerCase().includes(query) ||
        breach.breachType?.toLowerCase().includes(query) ||
        breach.systemAffected?.toLowerCase().includes(query) ||
        breach.briefDescription?.toLowerCase().includes(query)
      );
    });

    if (filteredData.length === 0) {
      toast.error((props) => (<CustomToast {...props} type="error" message={"No matching data to download"} />), { icon: false });
      return;
    }

    // Prepare data for CSV export
    const csvData = filteredData.map(breach => ({
      'Incident ID': breach.incidentId || 'N/A',
      'Status': formatStatus(breach.status) || 'N/A',
      'Date & Time Discovered': breach.discoveryDateTime || 'N/A',
      'Date & Time of Occurrence': breach.occurrenceDateTime || 'N/A',
      'Breach Type': breach.breachType || 'N/A',
      'Brief Description': breach.briefDescription || 'N/A',
      'System Affected': breach.systemAffected || 'N/A',
      'Data Categories': breach.dataCategories || 'N/A',
      'Affected Records': breach.affectedRecords || 'N/A',
      'Data Encrypted': breach.dataEncrypted || 'N/A',
      'Risk Description': breach.riskDescription || 'N/A',
      'Date Notified to DPBI': breach.dateNotifiedToDPBI || 'N/A',
      'Reference ID': breach.referenceId || 'N/A',
      'Date Notified to Data Principals': breach.dateNotifiedToDataPrincipals || 'N/A',
      'Notification Channels': breach.notificationChannels || 'N/A'
    }));

    exportToCSV(csvData, 'breach_notifications');
    toast.success((props) => (<CustomToast {...props} type="success" message={"CSV file downloaded successfully"} />), { icon: false });
  };

  return (
    <>
      <div className="configurePage">
        {/* Header Section */}
        <div className="breach-header-section">
          <div className="breach-title-section">
            <div style={{ display: 'flex', flexDirection: 'row', gap: '10px' }}>
              <Text appearance="heading-s" color="primary-grey-100">Breach Notifications</Text>
              <div className="dataProtectionOfficer-badge" style={{ marginTop: '5px' }}>
                <Text appearance="body-xs-bold" color="primary-grey-80">Governance</Text>
              </div>
            </div>
          </div>
          <div className="breach-button-group">
            <ActionButton
              kind="primary"
              size="medium"
              state="normal"
              label="Report New Breach"
              onClick={handleReportBreach}
            />
          </div>
        </div>

        {/* Stats Section */}
        {/* <div className="breach-stats-cards">
          <div className="breach-stat-card breach-stat-total">
            <div className="breach-stat-icon breach-stat-icon-total">
              <svg width="40" height="40" viewBox="0 0 24 24" fill="none">
                <rect x="3" y="3" width="18" height="18" rx="2" stroke="white" strokeWidth="2"/>
                <path d="M9 9h6M9 12h6M9 15h4" stroke="white" strokeWidth="2" strokeLinecap="round"/>
              </svg>
            </div>
            <div className="breach-stat-content">
              <div className="breach-stat-number">{stats.totalIncidents}</div>
              <div className="breach-stat-label">Total Incidents</div>
            </div>
          </div>
          
          <div className="breach-stat-card breach-stat-investigation">
            <div className="breach-stat-icon breach-stat-icon-investigation">
              <svg width="40" height="40" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="10" stroke="white" strokeWidth="2"/>
                <path d="M12 8V12M12 16H12.01" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </div>
            <div className="breach-stat-content">
              <div className="breach-stat-number">{stats.underInvestigation}</div>
              <div className="breach-stat-label">Under Investigation</div>
            </div>
          </div>

          <div className="breach-stat-card breach-stat-resolved">
            <div className="breach-stat-icon breach-stat-icon-resolved">
              <svg width="40" height="40" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="10" stroke="white" strokeWidth="2"/>
                <path d="M8 12L11 15L16 9" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </div>
            <div className="breach-stat-content">
              <div className="breach-stat-number">{stats.resolved}</div>
              <div className="breach-stat-label">Resolved</div>
            </div>
          </div>
        </div> */}

        <div className="template-stats-cards">
          <div className="template-stat-card template-stat-total">
            <div className="icon-wrapper">
              <IcAlarmSensor size="xl" color="#05b5a3" />
            </div>
            <div className="template-stat-content">
              <Text appearance="heading-xs" color="primary-grey-100">{String(stats.totalIncidents ?? 0)}</Text>
              <Text appearance="body-xs" color="primary-grey-80">Total incidents</Text>
            </div>
          </div>

          <div className="template-stat-card template-stat-published">
            <div className="icon-wrapper">
              <IcSuccessColored size="xl" color="#25AB21" />
            </div>
            <div className="template-stat-content">
              <Text appearance="heading-xs" color="primary-grey-100">{String(stats.resolved ?? 0)}</Text>
              <Text appearance="body-xs" color="primary-grey-80">Resolved</Text>
            </div>
          </div>

          <div className="template-stat-card template-stat-draft">

            <div className="icon-wrapper">
              <IcWarningColored size="xl" color="#F06D0F" />
            </div>
            <div className="template-stat-content">
              <Text appearance="heading-xs" color="primary-grey-100">{String(stats.underInvestigation ?? 0)}</Text>
              <Text appearance="body-xs" color="primary-grey-80">Under investigation</Text>
            </div>
          </div>
        </div>

        {/* Table Controls */}
        <div className="breach-table-controls">
          <div className="breach-search-container">
            <input
              type="text"
              placeholder="Search"
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
            <button className="breach-icon-button" title="Filter">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M3 6h18M3 12h12M3 18h6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </button>
            <button className="breach-icon-button" onClick={handleDownloadCSV} title="Download CSV">
              <IcDownload height={20} width={20} />
            </button>
          </div>
        </div>

        {/* Table Section */}
        <div className="breach-table-wrapper">
          <table className="breach-table">
            <thead>
              <tr>
                <th onClick={() => handleSort('incidentId')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Incident ID
                    </Text>
                    {renderSortIcon('incidentId')}
                  </div>
                </th>
                <th onClick={() => handleSort('status')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Status
                    </Text>
                    {renderSortIcon('status')}
                  </div>
                </th>
                <th onClick={() => handleSort('discoveryDateTime')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Date & time of discovery
                    </Text>
                    {renderSortIcon('discoveryDateTime')}
                  </div>
                </th>
                <th onClick={() => handleSort('occurrenceDateTime')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Date & time of occurrence
                    </Text>
                    {renderSortIcon('occurrenceDateTime')}
                  </div>
                </th>
                <th onClick={() => handleSort('breachType')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Type of breach
                    </Text>
                    {renderSortIcon('breachType')}
                  </div>
                </th>
                <th onClick={() => handleSort('briefDescription')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Brief description of incident
                    </Text>
                    {renderSortIcon('briefDescription')}
                  </div>
                </th>
                <th onClick={() => handleSort('systemAffected')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      System or service affected
                    </Text>
                    {renderSortIcon('systemAffected')}
                  </div>
                </th>
                <th onClick={() => handleSort('dataCategories')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Categories of personal data
                    </Text>
                    {renderSortIcon('dataCategories')}
                  </div>
                </th>
                <th onClick={() => handleSort('affectedRecords')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      No of data principals affected
                    </Text>
                    {renderSortIcon('affectedRecords')}
                  </div>
                </th>
                <th onClick={() => handleSort('dataEncrypted')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Data encryption
                    </Text>
                    {renderSortIcon('dataEncrypted')}
                  </div>
                </th>
                <th onClick={() => handleSort('riskDescription')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Risk description
                    </Text>
                    {renderSortIcon('riskDescription')}
                  </div>
                </th>
                <th onClick={() => handleSort('dpbiNotificationDate')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Date notified to DPBI
                    </Text>
                    {renderSortIcon('dpbiNotificationDate')}
                  </div>
                </th>
                <th onClick={() => handleSort('referenceId')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Reference ID
                    </Text>
                    {renderSortIcon('referenceId')}
                  </div>
                </th>
                <th onClick={() => handleSort('dataPrincipalNotificationDate')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Date notified to data principals
                    </Text>
                    {renderSortIcon('dataPrincipalNotificationDate')}
                  </div>
                </th>
                <th onClick={() => handleSort('notificationChannels')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Notification channels
                    </Text>
                    {renderSortIcon('notificationChannels')}
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
              {loading ? (
                <tr>
                  <td colSpan="16" style={{ textAlign: 'center', padding: '40px' }}>
                    <Text appearance="body-m" color="primary-grey-80">
                      Loading breach notifications...
                    </Text>
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan="16" style={{ textAlign: 'center', padding: '40px' }}>
                    <Text appearance="body-m" color="feedback_error_50">
                      {error}
                    </Text>
                  </td>
                </tr>
              ) : sortedBreachIncidents.length === 0 ? (
                <tr>
                  <td colSpan="16" style={{ textAlign: 'center', padding: '40px' }}>
                    <Text appearance="body-m" color="primary-grey-80">
                      No breach notifications found.
                    </Text>
                  </td>
                </tr>
              ) : (
                sortedBreachIncidents
                  .filter((breach) => {
                    if (!searchQuery) return true;
                    const query = searchQuery.toLowerCase();
                    return (
                      breach.incidentId?.toLowerCase().includes(query) ||
                      breach.status?.toLowerCase().includes(query) ||
                      breach.breachType?.toLowerCase().includes(query) ||
                      breach.systemAffected?.toLowerCase().includes(query) ||
                      breach.briefDescription?.toLowerCase().includes(query)
                    );
                  }).length === 0 ? (
                  <tr>
                    <td colSpan="16" style={{ textAlign: 'center', padding: '40px' }}>
                      <Text appearance="body-m" color="primary-grey-80">
                        No results found for "{searchQuery}"
                      </Text>
                    </td>
                  </tr>
                ) : sortedBreachIncidents
                  .filter((breach) => {
                    if (!searchQuery) return true;
                    const query = searchQuery.toLowerCase();
                    return (
                      breach.incidentId?.toLowerCase().includes(query) ||
                      breach.status?.toLowerCase().includes(query) ||
                      breach.breachType?.toLowerCase().includes(query) ||
                      breach.systemAffected?.toLowerCase().includes(query) ||
                      breach.briefDescription?.toLowerCase().includes(query)
                    );
                  })
                  .map((breach) => (
                    <tr key={breach.id}>
                      <td>
                        <Text appearance="body-xs-bold" color="black">
                          {breach.incidentId}
                        </Text>
                      </td>
                      <td>
                        <span
                          className={`status-badge ${getStatusClass(
                            breach.status
                          )}`}
                        >
                          {formatStatus(breach.status)}
                        </span>
                      </td>
                      <td>
                        <Text appearance="body-xs-bold" color="black">
                          {breach.discoveryDateTime}
                        </Text>
                      </td>
                      <td>
                        <Text appearance="body-xs-bold" color="black">
                          {breach.occurrenceDateTime}
                        </Text>
                      </td>
                      <td>
                        <Text appearance="body-xs-bold" color="black">
                          {breach.breachType}
                        </Text>
                      </td>
                      <td>
                        <Text appearance="body-xs" color="black">
                          {breach.briefDescription}
                        </Text>
                      </td>
                      <td>
                        <Text appearance="body-xs-bold" color="black">
                          {breach.systemAffected}
                        </Text>
                      </td>
                      <td>
                        <Text appearance="body-xs" color="black">
                          {breach.dataCategories}
                        </Text>
                      </td>
                      <td>
                        <Text appearance="body-xs-bold" color="black">
                          {breach.affectedRecords}
                        </Text>
                      </td>
                      <td>
                        <Text appearance="body-xs-bold" color="black">
                          {breach.dataEncrypted}
                        </Text>
                      </td>
                      <td>
                        <Text appearance="body-xs" color="black">
                          {breach.riskDescription}
                        </Text>
                      </td>
                      <td>
                        <Text appearance="body-xs" color="black">
                          {breach.dpbiNotificationDate}
                        </Text>
                      </td>
                      <td>
                        <Text appearance="body-xs-bold" color="black">
                          {breach.referenceId}
                        </Text>
                      </td>
                      <td>
                        <Text appearance="body-xs" color="black">
                          {breach.dataPrincipalNotificationDate}
                        </Text>
                      </td>
                      <td>
                        <Text appearance="body-xs" color="black">
                          {breach.notificationChannels}
                        </Text>
                      </td>
                      <td>
                        <div className="breach-actions">
                          <div className="tooltip-container">
                            <button
                              className="icon-btn"
                              onClick={() => handleView(breach.incidentId)}
                            >
                              <IcVisible height={18} width={18} />
                            </button>
                            <span className="tooltip-text">View Details</span>
                          </div>

                          <div className="tooltip-container">
                            <button
                              className="icon-btn"
                              onClick={() => handleEdit(breach.incidentId)}
                            >
                              <IcEditPen height={18} width={18} />
                            </button>
                            <span className="tooltip-text">Edit Breach</span>
                          </div>

                          <div className="tooltip-container">
                            <button
                              className="icon-btn icon-btn-notify"
                              onClick={() => handleNotifyDPBI(breach.incidentId)}
                              disabled
                            >
                              <IcNotification height={18} width={18} />
                            </button>
                            <span className="tooltip-text">Notify DPBI (Coming Soon)</span>
                          </div>

                          <div className="tooltip-container">
                            <button
                              className="icon-btn icon-btn-notify"
                              onClick={() => handleNotifyDataPrincipals(breach.incidentId)}
                            >
                              <IcNotification height={18} width={18} />
                            </button>
                            <span className="tooltip-text">Notify Data Principals</span>
                          </div>
                        </div>
                      </td>
                    </tr>
                  ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Status Edit Modal */}
      {showStatusModal && (
        <div className="modal-overlay" onClick={handleCancelStatusEdit}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 style={{ margin: 0, fontSize: '20px', fontWeight: '600' }}>
                Edit Breach Status
              </h2>
              <button
                className="modal-close-btn"
                onClick={handleCancelStatusEdit}
                style={{
                  background: 'none',
                  border: 'none',
                  fontSize: '24px',
                  cursor: 'pointer',
                  color: '#666',
                }}
              >
                ×
              </button>
            </div>

            <div className="modal-body">
              <div style={{ marginBottom: '16px' }}>
                <Text appearance="body-s" color="primary-grey-80">
                  Incident ID: <strong>{selectedBreach?.incidentId}</strong>
                </Text>
              </div>

              <div style={{ marginBottom: '24px' }}>
                <label style={{
                  display: 'block',
                  marginBottom: '8px',
                  fontSize: '14px',
                  fontWeight: '500',
                  color: '#374151',
                }}>
                  Status *
                </label>
                <Select
                  options={statusOptions}
                  value={newStatus}
                  onChange={setNewStatus}
                  placeholder="Select status"
                  styles={{
                    control: (base) => ({
                      ...base,
                      minHeight: '40px',
                      borderColor: '#d1d5db',
                      '&:hover': {
                        borderColor: '#9ca3af',
                      },
                    }),
                  }}
                />
              </div>

              <div style={{ marginBottom: '24px' }}>
                <label style={{
                  display: 'block',
                  marginBottom: '8px',
                  fontSize: '14px',
                  fontWeight: '500',
                  color: '#374151',
                }}>
                  Remarks
                </label>
                <textarea
                  placeholder="Enter remarks (optional)"
                  value={remarks}
                  onChange={(e) => setRemarks(e.target.value)}
                  rows={3}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '14px',
                    border: '1px solid #d1d5db',
                    borderRadius: '4px',
                    fontFamily: 'inherit',
                    resize: 'vertical',
                    minHeight: '60px',
                  }}
                />
              </div>

              <div style={{
                display: 'flex',
                gap: '12px',
                justifyContent: 'flex-end',
              }}>
                <Button
                  kind="secondary"
                  size="medium"
                  label="Cancel"
                  onClick={handleCancelStatusEdit}
                  disabled={updatingStatus}
                />
                <ActionButton
                  kind="primary"
                  size="medium"
                  label={updatingStatus ? "Updating..." : "Update Status"}
                  onClick={handleUpdateStatus}
                  disabled={updatingStatus}
                />
              </div>
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
    </>
  );
};

export default BreachNotifications;
