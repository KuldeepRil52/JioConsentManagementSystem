import React, { useState, useEffect } from "react";
import { Text, ActionButton, Icon } from "../custom-components";
import { useSelector } from "react-redux";
import { IcEditPen, IcTrash, IcSort } from "../custom-components/Icon";
import { useNavigate } from "react-router-dom";
import { Text, ActionButton, Button, BadgeV2, Icon } from "../custom-components";
import { useSelector, useDispatch } from "react-redux";
import { IcEditPen, IcTrash, IcDownload, IcFilter, IcClose, IcSort } from "../custom-components/Icon";
import "../styles/pageConfiguration.css";
import "../styles/ropa.css";
import AddROPAModal from "./AddROPAModal";
import { generateTransactionId } from "../utils/transactionId";
import { exportToCSV } from "../utils/csvExport";
import config from "../utils/config";
import { IcRequest } from '../custom-components/Icon';
import { deleteRopaEntry } from "../store/actions/CommonAction";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";

const ROPA = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const tenantId = useSelector((state) => state.common.tenant_id);
  const sessionToken = useSelector((state) => state.common.session_token);
  const businessId = useSelector((state) => state.common.business_id);

  const [ropaEntries, setRopaEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [totalCount, setTotalCount] = useState(0);
  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState('asc');

  const [filterDrawerOpen, setFilterDrawerOpen] = useState(false);
  const [filters, setFilters] = useState({
    status: [],
    dateFrom: "",
    dateTo: "",
  });

  // Fetch ROPA entries from API
  const fetchRopaEntries = async () => {
    try {
      setLoading(true);
      setError(null);

      // Generate transaction ID in UUID format
      const txnId = generateTransactionId();

      const response = await fetch(
        `${config.ropa_search}?businessId=${businessId}`,
        {
          method: 'GET',
          headers: {
            'accept': 'application/json',
            'txn': txnId,
            'tenant-id': tenantId,
            'business-id': businessId,
            'x-session-token': sessionToken
          }
        }
      );

      if (!response.ok) {
        throw new Error(`API error: ${response.status} ${response.statusText}`);
      }

      const data = await response.json();
      console.log("ROPA entries received:", data);

      // Extract entries from API response - data is in searchList
      const entries = data?.searchList || [];
      const entriesArray = Array.isArray(entries) ? entries : [];
      
      // Log each entry's ID for debugging
      entriesArray.forEach((entry, index) => {
        console.log(`ROPA Entry ${index}:`, {
          ropaId: entry.ropaId,
          id: entry.id,
          businessId: entry.businessId
        });
      });
      
      setRopaEntries(entriesArray);
      setTotalCount(entriesArray.length);
      console.log("ROPA entries set:", entries);
    } catch (err) {
      console.error("Error fetching ROPA entries:", err);
      setError(err.message);
      // Reset count to 0 on error
      setTotalCount(0);
      setRopaEntries([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (tenantId && sessionToken && businessId) {
      fetchRopaEntries();
    }
  }, [tenantId, sessionToken, businessId]);

  // Keep sample data as fallback
  const [sampleEntries] = useState([
    {
      id: 1,
      businessName: "Human resource",
      dpoDetails: "Aman Gupta, 8860345621",
      processingActivity: "Recruitment",
      purpose: "To hire and recruit employees",
      contactBankDetails: "Vendor representative name and contact details",
      specialNatureData: "Married, has no health issues",
      sourceData: "Recruitment agencies",
      categoryIndividual: "Employment candidate",
      permittedReasons: "Employment candidate",
      additionalCondition: "Employment candidate",
      caseExemption: "Employment candidate",
      dpiaReference: "Employment candidate",
    },
    {
      id: 2,
      businessName: "Human resource",
      dpoDetails: "Aman Gupta, 8860345621",
      processingActivity: "Recruitment",
      purpose: "To hire and recruit employees",
      contactBankDetails: "Vendor representative name and contact details",
      specialNatureData: "Married, has no health issues",
      sourceData: "Recruitment agencies",
      categoryIndividual: "Vendor representative",
      permittedReasons: "Vendor representative",
      additionalCondition: "Vendor representative",
      caseExemption: "Vendor representative",
      dpiaReference: "Vendor representative",
    },
    {
      id: 3,
      businessName: "Human resource",
      dpoDetails: "Aman Gupta, 8860345621",
      processingActivity: "Recruitment",
      purpose: "To hire and recruit employees",
      contactBankDetails: "Vendor representative name and contact details",
      specialNatureData: "Married, has no health issues",
      sourceData: "Recruitment agencies",
      categoryIndividual: "Employees",
      permittedReasons: "Employees",
      additionalCondition: "Employees",
      caseExemption: "Employees",
      dpiaReference: "Employees",
    },
    {
      id: 4,
      businessName: "Human resource",
      dpoDetails: "Aman Gupta, 8860345621",
      processingActivity: "Recruitment",
      purpose: "To hire and recruit employees",
      contactBankDetails: "Vendor representative name and contact details",
      specialNatureData: "Married, has no health issues",
      sourceData: "Recruitment agencies",
      categoryIndividual: "Customer",
      permittedReasons: "Customer",
      additionalCondition: "Customer",
      caseExemption: "Customer",
      dpiaReference: "Customer",
    },
    {
      id: 5,
      businessName: "Human resource",
      dpoDetails: "Aman Gupta, 8860345621",
      processingActivity: "Recruitment",
      purpose: "To hire and recruit employees",
      contactBankDetails: "Vendor representative name and contact details",
      specialNatureData: "Married, has no health issues",
      sourceData: "Recruitment agencies",
      categoryIndividual: "Vendor representative",
      permittedReasons: "Vendor representative",
      additionalCondition: "Vendor representative",
      caseExemption: "Vendor representative",
      dpiaReference: "Vendor representative",
    },
    {
      id: 6,
      businessName: "Human resource",
      dpoDetails: "Aman Gupta, 8860345621",
      processingActivity: "Recruitment",
      purpose: "To hire and recruit employees",
      contactBankDetails: "Vendor representative name and contact details",
      specialNatureData: "Married, has no health issues",
      sourceData: "Recruitment agencies",
      categoryIndividual: "Employees",
      permittedReasons: "Employees",
      additionalCondition: "Employees",
      caseExemption: "Employees",
      dpiaReference: "Employees",
    },
    {
      id: 7,
      businessName: "Human resource",
      dpoDetails: "Aman Gupta, 8860345621",
      processingActivity: "Recruitment",
      purpose: "To hire and recruit employees",
      contactBankDetails: "Vendor representative name and contact details",
      specialNatureData: "Married, has no health issues",
      sourceData: "Recruitment agencies",
      categoryIndividual: "Employment candidate",
      permittedReasons: "Employment candidate",
      additionalCondition: "Employment candidate",
      caseExemption: "Employment candidate",
      dpiaReference: "Employment candidate",
    },
    {
      id: 8,
      businessName: "Human resource",
      dpoDetails: "Aman Gupta, 8860345621",
      processingActivity: "Recruitment",
      purpose: "To hire and recruit employees",
      contactBankDetails: "Vendor representative name and contact details",
      specialNatureData: "Married, has no health issues",
      sourceData: "Recruitment agencies",
      categoryIndividual: "Vendor representative",
      permittedReasons: "Vendor representative",
      additionalCondition: "Vendor representative",
      caseExemption: "Vendor representative",
      dpiaReference: "Vendor representative",
    },
    {
      id: 9,
      businessName: "Human resource",
      dpoDetails: "Aman Gupta, 8860345621",
      processingActivity: "Recruitment",
      purpose: "To hire and recruit employees",
      contactBankDetails: "Vendor representative name and contact details",
      specialNatureData: "Married, has no health issues",
      sourceData: "Recruitment agencies",
      categoryIndividual: "Employees",
      permittedReasons: "Employees",
      additionalCondition: "Employees",
      caseExemption: "Employees",
      dpiaReference: "Employees",
    },
    {
      id: 10,
      businessName: "Human resource",
      dpoDetails: "Aman Gupta, 8860345621",
      processingActivity: "Recruitment",
      purpose: "To hire and recruit employees",
      contactBankDetails: "Vendor representative name and contact details",
      specialNatureData: "Married, has no health issues",
      sourceData: "Recruitment agencies",
      categoryIndividual: "Employees",
      permittedReasons: "Employees",
      additionalCondition: "Employees",
      caseExemption: "Employees",
      dpiaReference: "Employees",
    },
    {
      id: 11,
      businessName: "Human resource",
      dpoDetails: "Aman Gupta, 8860345621",
      processingActivity: "Recruitment",
      purpose: "To hire and recruit employees",
      contactBankDetails: "Vendor representative name and contact details",
      specialNatureData: "Married, has no health issues",
      sourceData: "Recruitment agencies",
      categoryIndividual: "Vendor representative",
      permittedReasons: "Vendor representative",
      additionalCondition: "Vendor representative",
      caseExemption: "Vendor representative",
      dpiaReference: "Vendor representative",
    },
    {
      id: 12,
      businessName: "Human resource",
      dpoDetails: "Aman Gupta, 8860345621",
      processingActivity: "Recruitment",
      purpose: "To hire and recruit employees",
      contactBankDetails: "Vendor representative name and contact details",
      specialNatureData: "Married, has no health issues",
      sourceData: "Recruitment agencies",
      categoryIndividual: "Employment candidate",
      permittedReasons: "Employment candidate",
      additionalCondition: "Employment candidate",
      caseExemption: "Employment candidate",
      dpiaReference: "Employment candidate",
    },
  ]);

  const [showForm, setShowForm] = useState(false);

  useEffect(() => {
    console.log("ROPA component mounted");
  }, []);

  const handleAddEntry = () => {
    navigate("/addROPAEntry");
  };

  const handleCloseModal = () => {
    setShowForm(false);
  };

  const handleAddNewEntry = (newEntry) => {
    setRopaEntries([...ropaEntries, newEntry]);
  };

  const handleEdit = (id) => {
    // Find the entry to edit
    const entryToEdit = ropaEntries.find(entry => entry.ropaId === id || entry.id === id);
    
    if (!entryToEdit) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"ROPA entry not found. Cannot edit."}
          />
        ),
        { icon: false }
      );
      return;
    }

    // Navigate to edit page with entry data
    navigate("/addROPAEntry", {
      state: {
        editMode: true,
        entryData: entryToEdit,
        ropaId: entryToEdit.ropaId || entryToEdit.id
      }
    });
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

  // Helper function to get nested property value
  const getNestedValue = (obj, path) => {
    return path.split('.').reduce((acc, part) => acc && acc[part], obj);
  };

  const sortedRopaEntries = React.useMemo(() => {
    if (!sortColumn) return ropaEntries;

    return [...ropaEntries].sort((a, b) => {
      const aValue = getNestedValue(a, sortColumn) || '';
      const bValue = getNestedValue(b, sortColumn) || '';

      if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }, [ropaEntries, sortColumn, sortDirection]);

  const handleDelete = async (entry) => {
    // Try multiple ID fields to find the correct one
    const ropaId = entry?.ropaId || entry?.id;
    
    if (!ropaId) {
      console.error("ROPA entry missing ID:", entry);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"ROPA ID is missing. Cannot delete entry."}
          />
        ),
        { icon: false }
      );
      return;
    }

    console.log("Attempting to delete ROPA entry with ID:", ropaId);
    console.log("Full entry data:", entry);

    const confirmDelete = window.confirm("Are you sure you want to delete this ROPA entry?");
    if (!confirmDelete) {
      return;
    }

    try {
      const response = await dispatch(deleteRopaEntry(ropaId));
      
      // Check if response is valid and has a successful status
      if (response && (response.status >= 200 && response.status < 300)) {
        // Show success toast
        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message={"ROPA entry deleted successfully."}
            />
          ),
          { icon: false }
        );

        // Refresh the list after successful deletion
        await fetchRopaEntries();
      } else {
        // Handle case where response doesn't have status (e.g., 204 No Content)
        if (response === undefined || response === null) {
          // Some DELETE endpoints return 204 No Content with no body
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"ROPA entry deleted successfully."}
              />
            ),
            { icon: false }
          );
          await fetchRopaEntries();
        } else {
          throw new Error("Failed to delete ROPA entry");
        }
      }
    } catch (error) {
      console.error("Error deleting ROPA entry:", error);
      console.error("Error details:", {
        message: error?.message,
        status: error?.status,
        errorData: error?.errorData,
        ropaId: ropaId
      });
      
      // Extract error message from different error formats
      let errorMessage = "Failed to delete ROPA entry. Please try again.";
      
      if (error?.status === 404) {
        errorMessage = `ROPA entry not found (404). The entry with ID "${ropaId}" may have already been deleted or does not exist.`;
      } else if (error?.message) {
        errorMessage = error.message;
      } else if (typeof error === 'string') {
        errorMessage = error;
      } else if (error?.errorMsg) {
        errorMessage = error.errorMsg;
      } else if (error?.errorData?.message) {
        errorMessage = error.errorData.message;
      } else if (error?.errorData?.errorMsg) {
        errorMessage = error.errorData.errorMsg;
      }
      
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={errorMessage}
          />
        ),
        { icon: false }
      );
    }
  };

  const handleDownloadCSV = () => {
    if (!ropaEntries || ropaEntries.length === 0) {
      alert("No data available to download");
      return;
    }

    // Filter ropaEntries based on search query
    const filteredData = ropaEntries.filter((entry) => {
      if (!searchQuery) return true;
      const query = searchQuery.toLowerCase();
      return (
        entry.processOverview?.businessFunction?.toLowerCase().includes(query) ||
        entry.processOverview?.department?.toLowerCase().includes(query) ||
        entry.processOverview?.processingActivityName?.toLowerCase().includes(query) ||
        entry.processOverview?.purposeForProcessing?.toLowerCase().includes(query)
      );
    });

    if (filteredData.length === 0) {
      alert("No matching data to download");
      return;
    }

    // Helper function to format DPO details — check both nested and top-level
    const getDpoDetails = (entry) => {
      const processOwner =
        entry.processOverview?.processOwner ||
        entry.processOwner ||
        null;
      if (!processOwner) return '-';
      const parts = [];
      if (processOwner.name) parts.push(processOwner.name);
      if (processOwner.mobile) parts.push(processOwner.mobile);
      if (processOwner.email) parts.push(processOwner.email);
      return parts.length > 0 ? parts.join('; ') : '-';
    };

    // Helper function to format arrays
    const formatArray = (arr) => {
      if (!arr || !Array.isArray(arr) || arr.length === 0) return '-';
      return arr.join('; ');
    };

    // Prepare data for CSV export
    const csvData = filteredData.map(entry => ({
      'Business Function / Department': entry.processOverview?.businessFunction || entry.processOverview?.department || entry.businessFunction || entry.department || '-',
      'Process Owner (DPO)': getDpoDetails(entry),
      'Processing Activity Name': entry.processOverview?.processingActivityName || entry.processingActivityName || '-',
      'Purpose for Processing': entry.processOverview?.purposeForProcessing || entry.purposeForProcessing || '-',
      'Categories of Personal Data': formatArray(entry.categoriesOfPersonalData),
      'Categories of Special Nature': formatArray(entry.categoriesOfSpecialNature),
      'Source of Personal Data': formatArray(entry.sourceOfPersonalData),
      'Category of Individual': formatArray(entry.categoryOfIndividual),
      'Activity Reason': entry.activityReason || '-',
      'Additional Condition': entry.additionalCondition || '-',
      'Case or Purpose for Exemption': entry.caseOrPurposeForExemption || '-',
      'DPIA Reference': entry.dpiaReference || '-'
    }));

    exportToCSV(csvData, 'ropa_entries');
    console.log("CSV download completed");
  };

  return (
    <>
      {showForm && (
        <AddROPAModal onClose={handleCloseModal} onAdd={handleAddNewEntry} />
      )}
      <div className="configurePage">
        {/* Header Section */}
        <div className="ropa-header-section">
          <div className="ropa-title-section">
            {/* <h1 className="ropa-main-title">
                Record of processing activities (ROPA)
              </h1>
              <div className="ropa-governance-badge">
                <Text appearance="body-xs-bold" color="primary-grey-80">
                  Governance
                </Text>
              </div> */}
            <div style={{ display: 'flex', flexDirection: 'row', gap: '10px' }}>
              <Text appearance="heading-s" color="primary-grey-100">Record of processing activities (ROPA)</Text>
              <div className="dataProtectionOfficer-badge" style={{ marginTop: '5px' }}>
                <Text appearance="body-xs-bold" color="primary-grey-80">Governance</Text>
              </div>
            </div>
          </div>
          <div className="ropa-button-group">
            <ActionButton
              kind="primary"
              size="medium"
              state="normal"
              label="Add record"
              onClick={handleAddEntry}
            />
          </div>
        </div>

        <div className="breach-stats-cards">
          <div className="template-stat-card template-stat-total" style={{ backgroundColor: '#E7EBF8' }}>
            <div className="icon-wrapper">
              <IcRequest size="xl" color="#0F3CC9" />
            </div>
            <div className="template-stat-content">
              <Text appearance="heading-xs" color="primary-grey-100">{String(totalCount ?? 0)}</Text>
              <Text appearance="body-xs" color="primary-grey-80">Total</Text>
            </div>
          </div>
        </div>

        {/* Table Controls */}
        <div className="ropa-table-controls">
          <div className="ropa-search-container">
            <input
              type="text"
              placeholder="Search"
              className="ropa-search-input"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            <svg className="ropa-search-icon" width="20" height="20" viewBox="0 0 24 24" fill="none">
              <circle cx="11" cy="11" r="8" stroke="currentColor" strokeWidth="2" />
              <path d="M21 21L16.65 16.65" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </div>
          <div className="ropa-table-actions">
            <button className="ropa-icon-button" title="Filter">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M3 6h18M3 12h12M3 18h6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </button>
            <button className="ropa-icon-button" onClick={handleDownloadCSV} title="Download CSV">
              <IcDownload height={20} width={20} />
            </button>
          </div>
        </div>

        {/* Table Section */}
        <div className="ropa-table-wrapper">
          <table className="ropa-table">
            <thead>
              {/* First header row - Section headers */}
              <tr className="ropa-section-row" >
                <th colSpan="4">
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    Process overview
                  </Text>
                </th>
                <th colSpan="1">
                  <Text appearance="body-xs-bold" color="primary-grey-80">Categories of personal data
                  </Text>
                </th>
                <th colSpan="1">
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    Categories of a special nature
                  </Text>
                </th>
                <th colSpan="1">
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    Source of personal data
                  </Text>
                </th>
                <th colSpan="1">
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    Category of indiviual
                  </Text>
                </th>
                <th colSpan="3">
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    Permitted Reason for Processing
                  </Text>
                </th>
                <th colSpan="1">
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    Data Privacy Impact Assessment (DPIA)
                  </Text>
                </th>
                <th colSpan="1">
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    Actions
                  </Text>
                </th>
              </tr>

              {/* Second header row - Column headers */}
              <tr>
                {/* Process Overview - 4 columns */}
                <th onClick={() => handleSort('processOverview.businessFunction')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Business name
                    </Text>
                    {renderSortIcon('processOverview.businessFunction')}
                  </div>
                </th>
                <th onClick={() => handleSort('processOverview.processOwner.name')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      DPO details
                    </Text>
                    {renderSortIcon('processOverview.processOwner.name')}
                  </div>
                </th>
                <th onClick={() => handleSort('processOverview.processingActivityName')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Processing activity
                    </Text>
                    {renderSortIcon('processOverview.processingActivityName')}
                  </div>
                </th>
                <th onClick={() => handleSort('processOverview.purposeForProcessing')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Purpose
                    </Text>
                    {renderSortIcon('processOverview.purposeForProcessing')}
                  </div>
                </th>

                {/* Categories of Personal Data - 1 column */}
                <th onClick={() => handleSort('categoriesOfPersonalData')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Contact and bank details
                    </Text>
                    {renderSortIcon('categoriesOfPersonalData')}
                  </div>
                </th>

                {/* Categories of Special Nature - 1 column */}
                <th onClick={() => handleSort('categoriesOfSpecialNature')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Origin, family and health info
                    </Text>
                    {renderSortIcon('categoriesOfSpecialNature')}
                  </div>
                </th>

                {/* Source of Personal Data - 1 column */}
                <th onClick={() => handleSort('sourceOfPersonalData')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Public register
                    </Text>
                    {renderSortIcon('sourceOfPersonalData')}
                  </div>
                </th>

                {/* Category of Individual - 1 column */}
                <th onClick={() => handleSort('categoryOfIndividual')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Employees, Customers, Others
                    </Text>
                    {renderSortIcon('categoryOfIndividual')}
                  </div>
                </th>

                {/* Permitted Reason for Processing - 3 columns */}
                <th onClick={() => handleSort('activityReason')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Permitted Reasons for Processing Activity
                    </Text>
                    {renderSortIcon('activityReason')}
                  </div>
                </th>
                <th onClick={() => handleSort('additionalCondition')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Additional condition for processing personal data
                    </Text>
                    {renderSortIcon('additionalCondition')}
                  </div>
                </th>
                <th onClick={() => handleSort('caseOrPurposeForExemption')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Case or Purpose for Exemption
                    </Text>
                    {renderSortIcon('caseOrPurposeForExemption')}
                  </div>
                </th>

                {/* DPIA - 1 column */}
                <th onClick={() => handleSort('dpiaReference')} style={{ cursor: 'pointer' }}>
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      DPIA Reference
                    </Text>
                    {renderSortIcon('dpiaReference')}
                  </div>
                </th>
                <th>
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    Actions
                  </Text>
                </th>

              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="12" style={{ textAlign: "center", padding: "2rem" }}>
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Loading ROPA entries...
                    </Text>
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan="12" style={{ textAlign: "center", padding: "2rem" }}>
                    <Text appearance="body-xs-bold" color="red">
                      Error: {error}
                    </Text>
                  </td>
                </tr>
              ) : sortedRopaEntries.length === 0 ? (
                <tr>
                  <td colSpan="12" style={{ textAlign: "center", padding: "2rem" }}>
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      No ROPA Entries to Display
                    </Text>
                  </td>
                </tr>
              ) : (
                sortedRopaEntries
                  .filter((entry) => {
                    if (!searchQuery) return true;
                    const query = searchQuery.toLowerCase();
                    return (
                      getNestedValue(entry, 'processOverview.businessFunction')?.toLowerCase().includes(query) ||
                      getNestedValue(entry, 'processOverview.department')?.toLowerCase().includes(query) ||
                      getNestedValue(entry, 'processOverview.processingActivityName')?.toLowerCase().includes(query) ||
                      getNestedValue(entry, 'processOverview.purposeForProcessing')?.toLowerCase().includes(query)
                    );
                  }).length === 0 ? (
                  <tr>
                    <td colSpan="12" style={{ textAlign: "center", padding: "2rem" }}>
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        No results found for "{searchQuery}"
                      </Text>
                    </td>
                  </tr>
                ) : sortedRopaEntries
                  .filter((entry) => {
                    if (!searchQuery) return true;
                    const query = searchQuery.toLowerCase();
                    return (
                      getNestedValue(entry, 'processOverview.businessFunction')?.toLowerCase().includes(query) ||
                      getNestedValue(entry, 'processOverview.department')?.toLowerCase().includes(query) ||
                      getNestedValue(entry, 'processOverview.processingActivityName')?.toLowerCase().includes(query) ||
                      getNestedValue(entry, 'processOverview.purposeForProcessing')?.toLowerCase().includes(query)
                    );
                  })
                  .map((entry, index) => {
                    // Helper function to format DPO details
                    // Check both processOverview.processOwner and top-level processOwner
                    const getDpoDetails = (entry) => {
                      const processOwner =
                        entry.processOverview?.processOwner ||
                        entry.processOwner ||
                        null;
                      if (!processOwner) return '-';
                      const parts = [];
                      if (processOwner.name) parts.push(processOwner.name);
                      if (processOwner.mobile) parts.push(processOwner.mobile);
                      if (processOwner.email) parts.push(processOwner.email);
                      return parts.length > 0 ? parts.join(', ') : '-';
                    };

                    // Helper function to format arrays
                    const formatArray = (arr) => {
                      if (!arr || !Array.isArray(arr) || arr.length === 0) return '-';
                      return arr.join(', ');
                    };

                    return (
                      <tr key={entry.id || entry.ropaId || index}>
                        {/* Process Overview */}
                        <td>
                          <Text appearance="body-xs-bold" color="black">
                            {getNestedValue(entry, 'processOverview.businessFunction') || getNestedValue(entry, 'processOverview.department') || entry.businessFunction || entry.department || '-'}
                          </Text>
                        </td>
                        <td>
                          <Text appearance="body-xs-bold" color="black">
                            {getDpoDetails(entry)}
                          </Text>
                        </td>
                        <td>
                          <Text appearance="body-xs-bold" color="black">
                            {getNestedValue(entry, 'processOverview.processingActivityName') || entry.processingActivityName || '-'}
                          </Text>
                        </td>
                        <td>
                          <Text appearance="body-xs-bold" color="black">
                            {getNestedValue(entry, 'processOverview.purposeForProcessing') || entry.purposeForProcessing || '-'}
                          </Text>
                        </td>

                        {/* Categories of Personal Data */}
                        <td>
                          <Text appearance="body-xs-bold" color="black">
                            {formatArray(entry.categoriesOfPersonalData)}
                          </Text>
                        </td>

                        {/* Categories of Special Nature */}
                        <td>
                          <Text appearance="body-xs-bold" color="black">
                            {formatArray(entry.categoriesOfSpecialNature)}
                          </Text>
                        </td>

                        {/* Source of Personal Data */}
                        <td>
                          <Text appearance="body-xs-bold" color="black">
                            {formatArray(entry.sourceOfPersonalData)}
                          </Text>
                        </td>

                        {/* Category of Individual */}
                        <td>
                          <Text appearance="body-xs-bold" color="black">
                            {formatArray(entry.categoryOfIndividual)}
                          </Text>
                        </td>

                        {/* Permitted Reason for Processing */}
                        <td>
                          <Text appearance="body-xs-bold" color="black">
                            {entry.activityReason || '-'}
                          </Text>
                        </td>
                        <td>
                          <Text appearance="body-xs-bold" color="black">
                            {entry.additionalCondition || '-'}
                          </Text>
                        </td>
                        <td>
                          <Text appearance="body-xs-bold" color="black">
                            {entry.caseOrPurposeForExemption || '-'}
                          </Text>
                        </td>

                        {/* DPIA Reference */}
                        <td>
                          <Text appearance="body-xs-bold" color="black">
                            {entry.dpiaReference || '-'}
                          </Text>
                        </td>
                        <td>
                          <div className="ropa-actions">
                            <ActionButton
                              icon={<IcEditPen />}
                              kind="tertiary"
                              size="small"
                              onClick={() => handleEdit(entry.ropaId || entry.id)}
                            />
                            <ActionButton
                              icon={<IcTrash />}
                              kind="tertiary"
                              size="small"
                              onClick={() => handleDelete(entry)}
                            />
                          </div>
                        </td>
                      </tr>
                    );
                  })
              )}
            </tbody>
          </table>
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
    </>
  );
};

export default ROPA;
