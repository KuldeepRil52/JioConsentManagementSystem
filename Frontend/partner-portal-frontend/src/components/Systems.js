import React, { useState, useEffect, useMemo } from "react";
import "../styles/masterSetup.css";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import { Text, InputFieldV2, ActionButton, Icon, Spinner, Button, Tabs, TabItem, InputToggle } from "../custom-components";
import { IcClose, IcEditPen, IcSort, IcFilter, IcChevronLeft, IcTrash } from "../custom-components/Icon";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate, useLocation } from "react-router-dom";
import Select from "react-select";
import { makeAPICall } from "../utils/ApiCall";

const Systems = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  
  const [systemsList, setSystemsList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showAddForm, setShowAddForm] = useState(false);
  const [systemName, setSystemName] = useState("");
  const [systemDescription, setSystemDescription] = useState("");
  const [systemTags, setSystemTags] = useState([]);
  const [selectedTags, setSelectedTags] = useState([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState("asc");
  const [selectedSystem, setSelectedSystem] = useState(null);
  const [isEditMode, setIsEditMode] = useState(false);
  const [activeTab, setActiveTab] = useState(0);
  
  // Integration tab state
  const [integrationType, setIntegrationType] = useState("mysql");
  const [enableIntegration, setEnableIntegration] = useState(true);
  const [integrationIdentifier, setIntegrationIdentifier] = useState("");
  const [host, setHost] = useState("");
  const [port, setPort] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [database, setDatabase] = useState("");
  const [ssh, setSsh] = useState("");
  const [selectedDataset, setSelectedDataset] = useState(null);
  const [createdSystemId, setCreatedSystemId] = useState(null);

  // Redux selectors
  const businessId = useSelector((state) => state.common.business_id);
  const tenant_id = useSelector((state) => state.common.tenant_id);
  const session_token = useSelector((state) => state.common.session_token);

  useEffect(() => {
    // Check if we're on add/edit route
    const isAddRoute = location.pathname === "/consent-system/add";
    const isEditRoute = location.pathname.startsWith("/consent-system/edit/");
    
    if (isAddRoute || isEditRoute) {
      setShowAddForm(true);
      if (isEditRoute) {
        const systemId = location.pathname.split("/").pop();
        // Get system data from navigation state
        if (location.state?.system) {
          const system = location.state.system;
          setSystemName(system.systemName || system.name || "");
          setSystemDescription(system.description || "");
          // Convert tag string to array format for multi-select
          const tags = system.tag ? system.tag.split(",").map(t => ({ value: t.trim(), label: t.trim() })) : [];
          setSelectedTags(tags);
          setSelectedSystem(system);
          setIsEditMode(true);
        } else {
          // Fetch system data by ID if not in state
          fetchSystemById(systemId);
        }
      } else {
        // Reset form for add mode
        setSystemName("");
        setSystemDescription("");
        setSelectedTags([]);
        setSelectedSystem(null);
        setIsEditMode(false);
      }
      setActiveTab(0);
    } else {
      setShowAddForm(false);
      // Fetch systems list on component mount
      fetchSystems();
    }
  }, [location.pathname, location.state]);

  const fetchSystems = async () => {
    setLoading(true);
    try {
      const headers = {
        "Content-Type": "application/json",
        "X-Tenant-ID": tenant_id || "",
        "X-Business-ID": businessId || "",
      };

      const response = await makeAPICall(
        "https://api.jcms-st.jiolabs.com:8443/registry/api/v1/systems",
        "GET",
        null,
        headers
      );

      if (response && response.data) {
        // Handle response data - adjust based on actual API response structure
        const systemsData = Array.isArray(response.data) ? response.data : (response.data.systems || response.data.data || []);
        setSystemsList(systemsData);
      } else {
        setSystemsList([]);
      }
    } catch (error) {
      console.error("Error fetching systems:", error);
      const errorMessage = error?.errorList?.[0]?.errorMessage || error?.message || "Failed to fetch systems";
      toast.error(errorMessage);
      setSystemsList([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchSystemById = async (systemId) => {
    setLoading(true);
    try {
      const headers = {
        "Content-Type": "application/json",
        "X-Tenant-ID": tenant_id || "",
        "X-Business-ID": businessId || "",
      };

      const response = await makeAPICall(
        `https://api.jcms-st.jiolabs.com:8443/registry/api/v1/systems/${systemId}`,
        "GET",
        null,
        headers
      );

      if (response && response.data) {
        const system = response.data;
        setSystemName(system.systemName || system.name || "");
        setSystemDescription(system.description || "");
        // Convert tag string to array format for multi-select if tags exist
        const tags = system.tag ? system.tag.split(",").map(t => ({ value: t.trim(), label: t.trim() })) : [];
        setSelectedTags(tags);
        setSelectedSystem(system);
        setIsEditMode(true);
        return system;
      }
      return null;
    } catch (error) {
      console.error("Error fetching system:", error);
      const errorMessage = error?.errorList?.[0]?.errorMessage || error?.message || "Failed to fetch system";
      toast.error(errorMessage);
      return null;
    } finally {
      setLoading(false);
    }
  };

  // Filter systems based on search query
  const filteredSystems = useMemo(() => {
    if (!searchQuery) return systemsList;
    const query = searchQuery.toLowerCase();
    return systemsList.filter(
      (system) =>
        (system.name || system.systemName || "").toLowerCase().includes(query) ||
        system.description?.toLowerCase().includes(query) ||
        (system.tag && system.tag.toLowerCase().includes(query))
    );
  }, [systemsList, searchQuery]);

  // Sort systems
  const sortedSystems = useMemo(() => {
    if (!sortColumn) return filteredSystems;

    return [...filteredSystems].sort((a, b) => {
      let aValue = a[sortColumn] || "";
      let bValue = b[sortColumn] || "";

      if (aValue < bValue) return sortDirection === "asc" ? -1 : 1;
      if (aValue > bValue) return sortDirection === "asc" ? 1 : -1;
      return 0;
    });
  }, [filteredSystems, sortColumn, sortDirection]);

  const handleSort = (column) => {
    if (sortColumn === column) {
      setSortDirection(sortDirection === "asc" ? "desc" : "asc");
    } else {
      setSortColumn(column);
      setSortDirection("asc");
    }
  };

  const handleAddSystem = () => {
    setSystemName("");
    setSystemDescription("");
    setSelectedTags([]);
    setSelectedSystem(null);
    setIsEditMode(false);
    setActiveTab(0);
    setCreatedSystemId(null);
    // Reset integration fields
    setIntegrationType("mysql");
    setEnableIntegration(true);
    setIntegrationIdentifier("");
    setHost("");
    setPort("");
    setUsername("");
    setPassword("");
    setDatabase("");
    setSsh("");
    setSelectedDataset(null);
    // Navigate to add route to hide side nav
    navigate("/consent-system/add");
  };

  const handleBackToList = () => {
    setSystemName("");
    setSystemDescription("");
    setSelectedTags([]);
    setSelectedSystem(null);
    setIsEditMode(false);
    setCreatedSystemId(null);
    // Reset integration fields
    setIntegrationType("mysql");
    setEnableIntegration(true);
    setIntegrationIdentifier("");
    setHost("");
    setPort("");
    setUsername("");
    setPassword("");
    setDatabase("");
    setSsh("");
    setSelectedDataset(null);
    // Navigate back to list
    navigate("/consent-system");
  };

  const handleEditSystem = async (system) => {
    const systemId = system.id || system.systemId;
    
    if (systemId) {
      // Fetch the complete system data first
      const fetchedSystem = await fetchSystemById(systemId);
      
      // Navigate to edit route with fetched system data in state
      if (fetchedSystem) {
        navigate(`/consent-system/edit/${systemId}`, {
          state: { system: fetchedSystem }
        });
      } else {
        // Fallback: navigate with the system from list if fetch fails
        navigate(`/consent-system/edit/${systemId}`, {
          state: { system }
        });
        // Set state from list data as fallback
        setSystemName(system.systemName || system.name || "");
        setSystemDescription(system.description || "");
        const tags = system.tag ? system.tag.split(",").map(t => ({ value: t.trim(), label: t.trim() })) : [];
        setSelectedTags(tags);
        setSelectedSystem(system);
        setIsEditMode(true);
      }
    } else {
      // No ID available, use list data and navigate
      navigate(`/consent-system/edit/unknown`, {
        state: { system }
      });
      setSystemName(system.systemName || system.name || "");
      setSystemDescription(system.description || "");
      const tags = system.tag ? system.tag.split(",").map(t => ({ value: t.trim(), label: t.trim() })) : [];
      setSelectedTags(tags);
      setSelectedSystem(system);
      setIsEditMode(true);
    }
    setActiveTab(0);
  };

  const handleTestIntegration = async () => {
    // Validate required fields
    if (!host.trim()) {
      toast.error("Please enter host");
      return;
    }
    if (!port.trim()) {
      toast.error("Please enter port");
      return;
    }
    if (!database.trim()) {
      toast.error("Please enter database name");
      return;
    }

    setLoading(true);
    try {
      const headers = {
        "Content-Type": "application/json",
        "X-Tenant-ID": tenant_id || "",
        "X-Business-ID": businessId || "",
      };

      // Map integrationType to uppercase dbType
      const dbTypeMap = {
        mysql: "MYSQL",
        mongodb: "MONGODB",
        postgresql: "POSTGRESQL",
        oracle: "ORACLE",
        sqlserver: "SQL_SERVER",
        api: "API",
      };
      const dbType = dbTypeMap[integrationType] || integrationType.toUpperCase();

      const body = {
        dbType: dbType,
        connectionDetails: {
          host: host.trim(),
          port: parseInt(port.trim()) || 0,
          username: username.trim() || "",
          password: password.trim() || "",
          database: database.trim(),
          sshRequired: ssh.trim().length > 0,
        },
      };

      const response = await makeAPICall(
        "http://10.173.184.32:8080/registry/api/v1/db-integration/test",
        "POST",
        body,
        headers
      );

      if (response && response.status === 200) {
        toast.success("Integration test successful");
      } else {
        toast.error("Integration test failed");
      }
    } catch (error) {
      console.error("Error testing integration:", error);
      const errorMessage = error?.errorList?.[0]?.errorMessage || error?.message || "Integration test failed";
      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveSystem = async () => {
    if (!systemName.trim()) {
      toast.error("Please enter a system name");
      return;
    }

    // Handle create mode - System Information tab
    if (activeTab === 0 && !isEditMode) {
      setLoading(true);
      try {
        const headers = {
          "Content-Type": "application/json",
          "X-Tenant-ID": tenant_id || "",
          "X-Business-ID": businessId || "",
        };

        const body = {
          systemName: systemName.trim(),
          description: systemDescription.trim() || "",
          status: "ACTIVE",
        };

        const response = await makeAPICall(
          "http://10.173.184.32:30011/api/v1/system-register",
          "POST",
          body,
          headers
        );

        if (response && response.status === 200) {
          // Extract systemId from response
          const systemId = response.data?.id || response.data?.systemId || response.data?.data?.id || response.data?.data?.systemId;
          if (systemId) {
            setCreatedSystemId(systemId);
          }
          toast.success("System created successfully");
          // Don't navigate back - allow user to continue to Integration tab
          // handleBackToList();
          // fetchSystems();
        } else {
          toast.error("Failed to create system");
        }
      } catch (error) {
        console.error("Error creating system:", error);
        const errorMessage = error?.errorList?.[0]?.errorMessage || error?.message || "Failed to create system";
        toast.error(errorMessage);
      } finally {
        setLoading(false);
      }
      return;
    }

    // Handle edit mode - System Information tab
    if (activeTab === 0 && isEditMode) {
      if (!selectedSystem || (!selectedSystem.id && !selectedSystem.systemId)) {
        toast.error("System ID not found");
        return;
      }

      setLoading(true);
      try {
        const systemId = selectedSystem.id || selectedSystem.systemId;
        const headers = {
          "Content-Type": "application/json",
          "X-Tenant-ID": tenant_id || "",
          "X-Business-ID": businessId || "",
        };

        const body = {
          systemName: systemName.trim(),
          description: systemDescription.trim() || "",
          status: "ACTIVE",
        };

        const response = await makeAPICall(
          `https://api.jcms-st.jiolabs.com:8443/registry/api/v1/systems/${systemId}`,
          "PUT",
          body,
          headers
        );

        if (response && response.status === 200) {
          toast.success("System updated successfully");
          handleBackToList();
          fetchSystems();
        } else {
          toast.error("Failed to update system");
        }
      } catch (error) {
        console.error("Error updating system:", error);
        const errorMessage = error?.errorList?.[0]?.errorMessage || error?.message || "Failed to update system";
        toast.error(errorMessage);
      } finally {
        setLoading(false);
      }
      return;
    }

    // Handle Integration tab (activeTab === 1)
    if (activeTab === 1) {
      // Validate required fields
      if (!host.trim()) {
        toast.error("Please enter host");
        return;
      }
      if (!port.trim()) {
        toast.error("Please enter port");
        return;
      }
      if (!database.trim()) {
        toast.error("Please enter database name");
        return;
      }

      // Get systemId - from created system or selected system (edit mode)
      let systemId = null;
      if (isEditMode) {
        systemId = selectedSystem?.id || selectedSystem?.systemId;
        if (!systemId) {
          toast.error("System ID not found");
          return;
        }
      } else {
        systemId = createdSystemId;
        if (!systemId) {
          toast.error("Please save system information first");
          return;
        }
      }

      setLoading(true);
      try {
        const headers = {
          "Content-Type": "application/json",
          "X-Tenant-ID": tenant_id || "",
          "X-Business-ID": businessId || "",
        };

        // Map integrationType to uppercase dbType
        const dbTypeMap = {
          mysql: "MYSQL",
          mongodb: "MONGODB",
          postgresql: "POSTGRESQL",
          oracle: "ORACLE",
          sqlserver: "SQL_SERVER",
          api: "API",
        };
        const dbType = dbTypeMap[integrationType] || integrationType.toUpperCase();

        const body = {
          systemId: systemId,
          dbType: dbType,
          connectionDetails: {
            host: host.trim(),
            port: parseInt(port.trim()) || 0,
            username: username.trim() || "",
            password: password.trim() || "",
            database: database.trim(),
            sshRequired: ssh.trim().length > 0,
          },
        };

        const response = await makeAPICall(
          "http://10.173.184.32:8080/system/api/v1/db-integration",
          "POST",
          body,
          headers
        );

        if (response && response.status === 200) {
          toast.success("Integration saved successfully");
          handleBackToList();
          fetchSystems();
        } else {
          toast.error("Failed to save integration");
        }
      } catch (error) {
        console.error("Error saving integration:", error);
        const errorMessage = error?.errorList?.[0]?.errorMessage || error?.message || "Failed to save integration";
        toast.error(errorMessage);
      } finally {
        setLoading(false);
      }
      return;
    }
  };

  const handleDeleteSystem = async (systemId) => {
    if (!window.confirm("Are you sure you want to delete this system?")) {
      return;
    }

    setLoading(true);
    try {
      // TODO: Implement API call to delete system
      // await dispatch(deleteSystem(systemId));
      toast.success("System deleted successfully");
      fetchSystems();
    } catch (error) {
      toast.error("Failed to delete system");
    } finally {
      setLoading(false);
    }
  };

  // Empty state illustration - using dashboard.svg as reference
  const emptyIllustration = new URL("../assets/dashboard.svg", import.meta.url).href;

  // System tags options (can be fetched from API)
  const systemTagOptions = [
    { value: "web-app", label: "Web App" },
    { value: "database", label: "Database" },
    { value: "data-warehouse", label: "Data Warehouse" },
    { value: "api", label: "API" },
    { value: "mobile-app", label: "Mobile App" },
    { value: "cloud-service", label: "Cloud Service" },
  ];

  // Integration type options
  const integrationTypeOptions = [
    { value: "mysql", label: "MySQL" },
    // { value: "postgresql", label: "PostgreSQL" },
    { value: "mongodb", label: "MongoDB" },
    // { value: "oracle", label: "Oracle" },
    // { value: "sqlserver", label: "SQL Server" },
    // { value: "api", label: "API" },
  ];

  // Dataset options (can be fetched from API)
  const datasetOptions = [
    { value: "dataset1", label: "Customer Data" },
    { value: "dataset2", label: "Transaction Data" },
    { value: "dataset3", label: "User Profile Data" },
  ];

  // Get connection instructions based on integration type
  const getConnectionInstructions = (type) => {
    const instructions = {
      mysql: "Provide the details below to connect to MySQL. After saving, test the integration to ensure it works.",
      postgresql: "Provide the details below to connect to PostgreSQL. After saving, test the integration to ensure it works.",
      mongodb: "Provide the details below to connect to MongoDB. After saving, test the integration to ensure it works.",
      oracle: "Provide the details below to connect to Oracle. After saving, test the integration to ensure it works.",
      sqlserver: "Provide the details below to connect to SQL Server. After saving, test the integration to ensure it works.",
      api: "Provide the details below to connect to the API. After saving, test the integration to ensure it works.",
    };
    return instructions[type] || instructions.mysql;
  };

  // If showing add form, render the form page
  if (showAddForm) {
    return (
      <div style={{ 
        width: "100%",
        padding: "24px 24px 24px 0", // Remove left padding to allow back button to move left
        maxWidth: "1200px", 
        margin: "0 auto",
        paddingTop: "100px", // Account for fixed header height (typically 64-80px)
        minHeight: "calc(100vh - 200px)", // Account for header and footer
        boxSizing: "border-box"
      }}>
         {/* Header with Back Button */}
         <div style={{ marginBottom: "32px" }}>
           <div style={{ display: "flex", alignItems: "flex-start", gap: "16px" }}>
             <button
               onClick={handleBackToList}
               style={{
                 width: "40px",
                 height: "40px",
                 borderRadius: "50%",
                 border: "1px solid #E0E0E0",
                 backgroundColor: "#F5F5F5",
                 cursor: "pointer",
                 display: "flex",
                 alignItems: "center",
                 justifyContent: "center",
                 padding: 0,
                 transition: "all 0.2s",
                 boxShadow: "none",
                 flexShrink: 0,
                 marginTop: "0",
               }}
               onMouseEnter={(e) => {
                 e.currentTarget.style.backgroundColor = "#E5E5E5";
                 e.currentTarget.style.borderColor = "#D0D0D0";
               }}
               onMouseLeave={(e) => {
                 e.currentTarget.style.backgroundColor = "#F5F5F5";
                 e.currentTarget.style.borderColor = "#E0E0E0";
               }}
               aria-label="Go back"
             >
               <IcChevronLeft size={20} color="#666" />
             </button>
             <div style={{ flex: 1 }}>
               <Text 
                 appearance="heading-s" 
                 color="primary-grey-100"
                 style={{ 
                   fontSize: "20px", 
                   fontWeight: "600",
                   lineHeight: "28px",
                   marginBottom: "8px",
                   display: "block"
                 }}
               >
                 {isEditMode ? "Edit system" : "Add system"}
               </Text>
               <Text appearance="body-s" color="primary-grey-80" style={{ fontSize: "14px", lineHeight: "20px", display: "block" }}>
                 Systems store or process data in your organization, such as web apps, databases, or data warehouses. Describe the application below to register it.
               </Text>
             </div>
           </div>
         </div>

        {/* Tabs */}
        <div style={{ marginBottom: "32px" }}>
          <div style={{ 
            display: "flex", 
            gap: "32px",
            borderBottom: "1px solid #E0E0E0",
            paddingBottom: "0"
          }}>
            <div
              onClick={() => setActiveTab(0)}
              style={{
                cursor: "pointer",
                paddingBottom: "12px",
                position: "relative",
                marginBottom: "-1px"
              }}
            >
              <Text 
                appearance="body-xs" 
                color={activeTab === 0 ? "primary-grey-100" : "primary-grey-80"}
                style={{
                  fontWeight: activeTab === 0 ? "600" : "400",
                  fontSize: "14px"
                }}
              >
                System information
              </Text>
              {activeTab === 0 && (
                <div style={{
                  position: "absolute",
                  bottom: "-1px",
                  left: 0,
                  right: 0,
                  height: "3px",
                  backgroundColor: "#0F3CC9",
                  borderRadius: "2px 2px 0 0"
                }} />
              )}
            </div>
            <div
              onClick={() => setActiveTab(1)}
              style={{
                cursor: "pointer",
                paddingBottom: "12px",
                position: "relative",
                marginBottom: "-1px"
              }}
            >
              <Text 
                appearance="body-xs" 
                color={activeTab === 1 ? "primary-grey-100" : "primary-grey-80"}
                style={{
                  fontWeight: activeTab === 1 ? "600" : "400",
                  fontSize: "14px"
                }}
              >
                Integration
              </Text>
              {activeTab === 1 && (
                <div style={{
                  position: "absolute",
                  bottom: "-1px",
                  left: 0,
                  right: 0,
                  height: "3px",
                  backgroundColor: "#0F3CC9",
                  borderRadius: "2px 2px 0 0"
                }} />
              )}
            </div>
          </div>
        </div>

        {/* Tab Content */}
        {activeTab === 0 && (
          <div style={{ maxWidth: "600px" }}>
            <Text appearance="body-s" color="primary-grey-80" style={{ marginBottom: "24px", fontSize: "14px", lineHeight: "20px" }}>
              Adding brief context to each system makes our tech stack easier to understand and report on for everyone.
            </Text>

            {/* Name Field */}
            <div style={{ marginBottom: "24px" }}>
              <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "8px", fontSize: "14px", fontWeight: "600" }}>
                Name (Required)
              </Text>
              <InputFieldV2
                value={systemName}
                onChange={(e) => setSystemName(e.target.value)}
                placeholder="Enter system name"
                required
              />
              <Text appearance="body-xs" color="primary-grey-60" style={{ marginTop: "4px", fontSize: "12px" }}>
                Unique name of system
              </Text>
            </div>

            {/* Description Field */}
            <div style={{ marginBottom: "24px" }}>
              <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "8px", fontSize: "14px", fontWeight: "600" }}>
                Description
              </Text>
              <InputFieldV2
                value={systemDescription}
                onChange={(e) => setSystemDescription(e.target.value)}
                placeholder="What services does this system perform"
                multiline
                rows={4}
              />
            </div>

            {/* System Tags Field */}
            <div style={{ marginBottom: "24px" }}>
              <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "8px", fontSize: "14px", fontWeight: "600" }}>
                System tags
              </Text>
              <Select
                isMulti
                options={systemTagOptions}
                value={selectedTags}
                onChange={setSelectedTags}
                placeholder="Select system tags"
                styles={{
                  control: (base) => ({
                    ...base,
                    border: "1px solid #E0E0E0",
                    borderRadius: "8px",
                    minHeight: "40px",
                    boxShadow: "none",
                    "&:hover": {
                      border: "1px solid #E0E0E0",
                    },
                  }),
                  placeholder: (base) => ({
                    ...base,
                    color: "#999",
                  }),
                }}
              />
              <Text appearance="body-xs" color="primary-grey-60" style={{ marginTop: "4px", fontSize: "12px" }}>
                What services does this system perform
              </Text>
            </div>

            {/* Save Button */}
            <div style={{ marginTop: "32px" }}>
              <Button
                label="Save"
                onClick={handleSaveSystem}
                variant="primary"
                size="medium"
                disabled={loading}
                style={{
                  fontSize: "14px",
                  fontWeight: "600",
                  padding: "10px 24px",
                  minWidth: "100px"
                }}
              />
            </div>
          </div>
        )}

        {activeTab === 1 && (
          <div style={{ maxWidth: "600px" }}>
            {/* Integration Description */}
            <Text appearance="body-s" color="primary-grey-80" style={{ marginBottom: "24px" }}>
              Integrations are used to process privacy requests for access, erasure, portability, rectification, and consent.
            </Text>

            {/* Integration Type Dropdown */}
            <div style={{ marginBottom: "24px" }}>
              <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "8px", fontSize: "14px", fontWeight: "600" }}>
                Integration type
              </Text>
              <Select
                options={integrationTypeOptions}
                value={integrationTypeOptions.find(opt => opt.value === integrationType)}
                onChange={(selected) => setIntegrationType(selected.value)}
                styles={{
                  control: (base) => ({
                    ...base,
                    border: "1px solid #E0E0E0",
                    borderRadius: "8px",
                    minHeight: "40px",
                    boxShadow: "none",
                    "&:hover": {
                      border: "1px solid #E0E0E0",
                    },
                  }),
                  placeholder: (base) => ({
                    ...base,
                    color: "#999",
                  }),
                }}
              />
            </div>

            {/* Connection Instructions Box */}
            <div
              style={{
                backgroundColor: "#F5F5F5",
                padding: "16px",
                borderRadius: "8px",
                marginBottom: "24px",
              }}
            >
              <Text appearance="body-xs" color="primary-grey-80">
                {getConnectionInstructions(integrationType)}
              </Text>
            </div>

            {/* Enable Integration Toggle */}
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                marginBottom: enableIntegration ? "32px" : "0",
                padding: "12px 0",
              }}
            >
              <Text appearance="body-s" color="primary-grey-100">
                Enable integration
              </Text>
              <InputToggle
                checked={enableIntegration}
                onChange={() => setEnableIntegration(!enableIntegration)}
                size="medium"
                type="toggle"
              />
            </div>

            {/* Integration Fields - Only show when toggle is enabled */}
            {enableIntegration && (
              <>

                {/* Integration Fields */}
                <div style={{ marginBottom: "24px" }}>
                  <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "8px" }}>
                    Integration identifier
                  </Text>
                  <InputFieldV2
                    value={integrationIdentifier}
                    onChange={(e) => setIntegrationIdentifier(e.target.value)}
                    placeholder="Enter integration identifier"
                  />
                </div>

                <div style={{ marginBottom: "24px" }}>
                  <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "8px" }}>
                    Host (Required)
                  </Text>
                  <InputFieldV2
                    value={host}
                    onChange={(e) => setHost(e.target.value)}
                    placeholder="Enter host"
                    required
                  />
                </div>

                <div style={{ marginBottom: "24px" }}>
                  <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "8px" }}>
                    Port (Required)
                  </Text>
                  <InputFieldV2
                    value={port}
                    onChange={(e) => setPort(e.target.value)}
                    placeholder="Enter port"
                    required
                  />
                </div>

                <div style={{ marginBottom: "24px" }}>
                  <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "8px" }}>
                    Username
                  </Text>
                  <InputFieldV2
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="Enter username"
                  />
                </div>

                <div style={{ marginBottom: "24px" }}>
                  <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "8px" }}>
                    Password
                  </Text>
                  <InputFieldV2
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Enter password"
                  />
                </div>

                <div style={{ marginBottom: "24px" }}>
                  <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "8px" }}>
                    Database (Required)
                  </Text>
                  <InputFieldV2
                    value={database}
                    onChange={(e) => setDatabase(e.target.value)}
                    placeholder="Enter database name"
                    required
                  />
                </div>

                <div style={{ marginBottom: "24px" }}>
                  <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "8px" }}>
                    SSH (Required)
                  </Text>
                  <InputFieldV2
                    value={ssh}
                    onChange={(e) => setSsh(e.target.value)}
                    placeholder="Enter SSH details"
                    required
                  />
                </div>

                <div style={{ marginBottom: "32px" }}>
                  <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "8px" }}>
                    Dataset
                  </Text>
                  <Select
                    options={datasetOptions}
                    value={selectedDataset}
                    onChange={setSelectedDataset}
                    placeholder="Select dataset"
                    styles={{
                      control: (base) => ({
                        ...base,
                        border: "1px solid #E0E0E0",
                        borderRadius: "8px",
                        minHeight: "40px",
                        boxShadow: "none",
                        "&:hover": {
                          border: "1px solid #E0E0E0",
                        },
                      }),
                      placeholder: (base) => ({
                        ...base,
                        color: "#999",
                      }),
                    }}
                  />
                </div>

                {/* Action Buttons */}
                <div style={{ display: "flex", gap: "12px", marginTop: "32px" }}>
                  <Button
                    label="Save"
                    onClick={handleSaveSystem}
                    variant="primary"
                    size="medium"
                    disabled={loading}
                  />
                  <Button
                    label="Test integration"
                    onClick={handleTestIntegration}
                    variant="ghost"
                    size="medium"
                    disabled={loading}
                  />
                  <Button
                    label="Delete integration"
                    onClick={() => {
                      if (window.confirm("Are you sure you want to delete this integration?")) {
                        // TODO: Implement delete integration
                        toast.success("Integration deleted successfully");
                      }
                    }}
                    variant="ghost"
                    size="medium"
                    style={{ color: "#DC2626" }}
                  />
                </div>
              </>
            )}

            {/* Save Button - Show when toggle is off */}
            {!enableIntegration && (
              <div style={{ marginTop: "32px" }}>
                <Button
                  label="Save"
                  onClick={handleSaveSystem}
                  variant="primary"
                  size="medium"
                  disabled={loading}
                />
              </div>
            )}
          </div>
        )}

        <ToastContainer
          transition={Slide}
          autoClose={3000}
          hideProgressBar
          closeButton={CustomToast.CloseButton}
          closeOnClick
        />
      </div>
    );
  }

  return (
      <div className="configurePage" style={{ padding: "32px 24px 24px 24px", minHeight: "calc(100vh - 80px)", boxSizing: "border-box", width: "100%", maxWidth: "100%", overflowX: "hidden" }}>
      {/* Breadcrumb and Header Section */}
      <div style={{ marginBottom: "24px", width: "100%", boxSizing: "border-box" }}>
      <div style={{ display: 'flex', flexDirection: 'row', gap: '10px' }}>
            <Text appearance="heading-s" color="primary-grey-100">System</Text>
            <div className="dataProtectionOfficer-badge" style={{ marginTop: '5px' }}>
              <Text appearance="body-xs-bold" color="primary-grey-80">Consent</Text>
            </div>
          </div>
        {/* <div style={{ marginBottom: "16px" }}>
          <Text appearance="body-xs" color="primary-grey-60">
            Consent
          </Text>
        </div> */}

        <div style={{ display: "flex", flexDirection: 'row-reverse', alignItems: "center", flexWrap: "wrap", gap: "16px", width: "100%", boxSizing: "border-box" }}>
          {/* <Text appearance="heading-s" color="primary-grey-100" style={{ flexShrink: 0 }}>
            System
          </Text> */}
          <ActionButton
            kind="primary"
            size="medium"
            label="Add system"
            onClick={handleAddSystem}
            style={{ flexShrink: 0, minWidth: "auto" }}
          />
        </div>
      </div>

      {/* Search and Filter Controls */}
      <div style={{ marginBottom: "16px", display: "flex", justifyContent: "flex-end", gap: "12px", width: "100%", boxSizing: "border-box" }}>
        <div style={{ position: "relative", width: "100%", maxWidth: "400px", minWidth: "280px" }}>
          <input
            type="text"
            placeholder="Search system name or description"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{
              width: "100%",
              padding: "10px 40px 10px 40px",
              border: "1px solid #E0E0E0",
              borderRadius: "8px",
              fontSize: "14px",
              outline: "none",
              boxSizing: "border-box",
            }}
          />
          <svg
            style={{
              position: "absolute",
              left: "12px",
              top: "50%",
              transform: "translateY(-50%)",
              width: "20px",
              height: "20px",
              color: "#666",
            }}
            viewBox="0 0 24 24"
            fill="none"
          >
            <circle cx="11" cy="11" r="8" stroke="currentColor" strokeWidth="2" />
            <path d="M21 21L16.65 16.65" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
          </svg>
          <button
            style={{
              position: "absolute",
              right: "12px",
              top: "50%",
              transform: "translateY(-50%)",
              background: "none",
              border: "none",
              cursor: "pointer",
              padding: "4px",
            }}
            title="Filter"
          >
            <IcFilter height={20} width={20} />
          </button>
        </div>
      </div>

      {/* Table Section */}
      {loading && systemsList.length === 0 ? (
        <div style={{ textAlign: "center", padding: "40px" }}>
          <Spinner size="large" />
        </div>
      ) : (
        <div style={{ backgroundColor: "#fff", borderRadius: "8px", overflow: "hidden", border: "1px solid #E0E0E0", width: "100%" }}>
          <table
            style={{
              width: "100%",
              borderCollapse: "collapse",
              tableLayout: "auto",
            }}
          >
            <thead>
              <tr style={{ backgroundColor: "#F5F5F5", borderBottom: "1px solid #E0E0E0" }}>
                <th
                  style={{
                    padding: "12px 16px",
                    textAlign: "left",
                    cursor: "pointer",
                  }}
                  onClick={() => handleSort("name")}
                >
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      System name
                    </Text>
                    <Icon ic={<IcSort />} color="primary_60" size="small" />
                  </div>
                </th>
                <th
                  style={{
                    padding: "12px 16px",
                    textAlign: "left",
                    cursor: "pointer",
                  }}
                  onClick={() => handleSort("description")}
                >
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Description
                    </Text>
                    <Icon ic={<IcSort />} color="primary_60" size="small" />
                  </div>
                </th>
                <th
                  style={{
                    padding: "12px 16px",
                    textAlign: "left",
                    cursor: "pointer",
                  }}
                  onClick={() => handleSort("tag")}
                >
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      System tag
                    </Text>
                    <Icon ic={<IcSort />} color="primary_60" size="small" />
                  </div>
                </th>
                <th
                  style={{
                    padding: "12px 16px",
                    textAlign: "left",
                  }}
                >
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Action
                    </Text>
                    <Icon ic={<IcSort />} color="primary_60" size="small" />
                  </div>
                </th>
              </tr>
            </thead>
            <tbody>
              {sortedSystems.length === 0 ? (
                <tr>
                  <td colSpan="4" style={{ padding: "80px 20px", textAlign: "center" }}>
                    <div
                      style={{
                        display: "flex",
                        flexDirection: "column",
                        alignItems: "center",
                        justifyContent: "center",
                        gap: "16px",
                      }}
                    >
                      <img
                        src={emptyIllustration}
                        alt="Empty state illustration"
                        style={{ width: "180px", height: "auto", marginBottom: "8px" }}
                      />
                      <Text appearance="body-m" color="primary-grey-80" style={{ marginBottom: "8px" }}>
                        You have not added any system yet.
                      </Text>
                      <Button
                        label="Add system"
                        onClick={handleAddSystem}
                        variant="primary"
                        size="medium"
                      />
                    </div>
                  </td>
                </tr>
              ) : (
                sortedSystems.map((system) => (
                  <tr
                    key={system.id || system.systemId}
                    style={{
                      borderBottom: "1px solid #E0E0E0",
                      transition: "background-color 0.2s",
                    }}
                    onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = "#F9F9F9")}
                    onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = "transparent")}
                  >
                    <td style={{ padding: "12px 16px" }}>
                      <Text appearance="body-xs" color="primary-grey-100">
                        {system.name || system.systemName || "-"}
                      </Text>
                    </td>
                    <td style={{ padding: "12px 16px" }}>
                      <Text appearance="body-xs" color="primary-grey-100">
                        {system.description || "-"}
                      </Text>
                    </td>
                    <td style={{ padding: "12px 16px" }}>
                      <Text appearance="body-xs" color="primary-grey-100">
                        {system.tag || "-"}
                      </Text>
                    </td>
                    <td style={{ padding: "12px 16px" }}>
                      <div style={{ display: "flex", gap: "8px" }}>
                        <ActionButton
                          icon={<IcEditPen />}
                          onClick={() => handleEditSystem(system)}
                          variant="ghost"
                          size="small"
                          aria-label="Edit system"
                          style={{ backgroundColor: "transparent", color: "#666" }}
                        />
                        <ActionButton
                          icon={<IcTrash />}
                          onClick={() => handleDeleteSystem(system.id)}
                          variant="ghost"
                          size="small"
                          aria-label="Delete system"
                          style={{ backgroundColor: "transparent", color: "#666" }}
                        />
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}


      <ToastContainer
        transition={Slide}
        autoClose={3000}
        hideProgressBar
        closeButton={CustomToast.CloseButton}
        closeOnClick
      />
    </div>
  );
};

export default Systems;
