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
import config from "../utils/config";

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
  const [integrationType, setIntegrationType] = useState(null);
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
  const [datasetsList, setDatasetsList] = useState([]);
  // Track the raw datasetId from the integration API so we can resolve it
  // once datasetsList finishes loading (handles the async race condition)
  const [pendingDatasetId, setPendingDatasetId] = useState(null);

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
      // Fetch datasets for the dropdown
      fetchDatasets();
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
          
          // Populate integration fields if available
          if (system.integration || system.dbIntegration) {
            const integration = system.integration || system.dbIntegration;
            setIntegrationType(integration.type || integration.integrationType || "mysql");
            setEnableIntegration(integration.enabled !== false);
            setIntegrationIdentifier(integration.identifier || integration.integrationIdentifier || "");
            setHost(integration.host || "");
            setPort(integration.port?.toString() || "");
            setUsername(integration.username || "");
            setPassword(integration.password || "");
            setDatabase(integration.database || integration.databaseName || "");
            setSsh(integration.ssh || "");
          } else if (!host && !port && !database) {
            // Only fetch integration data if fields are not already populated
            // (handleEditSystem -> fetchSystemById may have already loaded them)
            const systemId = system.id || system.systemId;
            if (systemId) {
              fetchIntegrationData(systemId, datasetsList);
            }
          }
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

  // Fetch integration data when switching to Integration tab in edit mode
  useEffect(() => {
    if (activeTab === 1 && isEditMode && selectedSystem) {
      const systemId = selectedSystem.id || selectedSystem.systemId;
      // Only fetch if form fields are empty (data not loaded yet)
      if (systemId && (!host && !port && !database)) {
        console.log("Fetching integration data when switching to Integration tab");
        fetchIntegrationData(systemId, datasetsList);
      }
    }
  }, [activeTab, isEditMode, selectedSystem]);

  // Re-resolve selectedDataset when datasetsList finally loads
  // This fixes the race condition where fetchIntegrationData runs before
  // fetchDatasets completes, leaving the dataset dropdown empty
  useEffect(() => {
    if (pendingDatasetId && datasetsList.length > 0 && !selectedDataset) {
      const dataset = datasetsList.find(d =>
        d.id === pendingDatasetId ||
        d.datasetId === pendingDatasetId ||
        String(d.id) === String(pendingDatasetId) ||
        String(d.datasetId) === String(pendingDatasetId)
      );
      if (dataset) {
        setSelectedDataset({
          value: dataset.id || dataset.datasetId,
          label: dataset.name || dataset.datasetId || "Unnamed Dataset"
        });
        console.log("Re-resolved pending dataset after datasets loaded:", dataset.name || dataset.datasetId);
        setPendingDatasetId(null); // Clear the pending ID once resolved
      }
    }
  }, [datasetsList, pendingDatasetId, selectedDataset]);

  const fetchSystems = async () => {
    setLoading(true);
    try {
      const headers = {
        "Content-Type": "application/json",
        "X-Tenant-ID": tenant_id || "",
        "X-Business-ID": businessId || "",
      };

      const response = await makeAPICall(
        config.registry_systems,
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

  const fetchDatasets = async () => {
    try {
      const headers = {
        "Content-Type": "application/json",
        "X-Tenant-ID": tenant_id || "",
        "X-Business-ID": businessId || "",
      };

      const response = await makeAPICall(
        config.registry_datasets,
        "GET",
        null,
        headers
      );

      if (response && response.data) {
        // Handle response data - adjust based on actual API response structure
        const datasetsData = Array.isArray(response.data) ? response.data : (response.data.datasets || response.data.data || response.data.searchList || []);
        
        // Parse datasetYaml to extract name if needed
        const parsedDatasets = datasetsData.map(dataset => {
          let name = dataset.name;
          
          // If name is not directly available, try to parse from datasetYaml
          if (dataset.datasetYaml && !name) {
            try {
              const yamlContent = dataset.datasetYaml;
              // Priority: dataset.name > direct name field > first collection name
              // Extract dataset name from YAML (format: "dataset:\n  name: value")
              const datasetNameMatch = yamlContent.match(/dataset:\s*\n\s*name:\s*(.+?)(?:\n|$)/);
              if (datasetNameMatch) {
                name = datasetNameMatch[1].trim();
              } else {
                // Fallback: try to find any top-level "name:" field (but not from collections)
                const nameMatch = yamlContent.match(/^name:\s*(.+?)(?:\n|$)/m);
                if (nameMatch) {
                  name = nameMatch[1].trim();
                } else {
                  // Last fallback: try to get first collection name
                  const collectionNameMatch = yamlContent.match(/collections:\s*\n\s*-\s*name:\s*(.+)/);
                  if (collectionNameMatch) {
                    name = collectionNameMatch[1].trim();
                  }
                }
              }
            } catch (e) {
              console.error("Error parsing dataset YAML:", e);
            }
          }
          
          // Always prioritize dataset.name from YAML if available (even if name was already set)
          if (dataset.datasetYaml) {
            try {
              const yamlContent = dataset.datasetYaml;
              const datasetNameMatch = yamlContent.match(/dataset:\s*\n\s*name:\s*(.+?)(?:\n|$)/);
              if (datasetNameMatch) {
                name = datasetNameMatch[1].trim();
              }
            } catch (e) {
              console.error("Error parsing dataset name from YAML:", e);
            }
          }
          
          return {
            ...dataset,
            name: name || dataset.datasetId || "Unnamed Dataset",
            id: dataset.id || dataset.datasetId
          };
        });
        
        setDatasetsList(parsedDatasets);
      } else {
        setDatasetsList([]);
      }
    } catch (error) {
      console.error("Error fetching datasets:", error);
      // Don't show error toast - datasets might not be critical for system creation
      setDatasetsList([]);
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
        config.registry_systems_by_id(systemId),
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
        
        // Fetch integration data separately (pass datasetsList so it's available)
        await fetchIntegrationData(systemId, datasetsList);
        
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

  // Fetch integration data for a system
  const fetchIntegrationData = async (systemId, datasetsListRef = null) => {
    try {
      const headers = {
        "Content-Type": "application/json",
        "X-Tenant-ID": tenant_id || "",
        "X-Business-ID": businessId || "",
      };

      const response = await makeAPICall(
        `${config.registry_db_integration}?systemId=${systemId}`,
        "GET",
        null,
        headers
      );

      console.log("fetchIntegrationData - Full response:", response);
      console.log("fetchIntegrationData - response.data:", response?.data);
      console.log("fetchIntegrationData - response.status:", response?.status);
      console.log("fetchIntegrationData - Looking for systemId:", systemId);

      // Handle different response structures
      let integration = null;
      if (response && response.data) {
        // Check if response.data has integrations array
        if (response.data.integrations && Array.isArray(response.data.integrations)) {
          console.log("Found integrations array with", response.data.integrations.length, "items");
          console.log("All integrations systemIds:", response.data.integrations.map(int => int.systemId));
          // Find the integration matching the systemId
          integration = response.data.integrations.find(
            (int) => {
              const matches = int.systemId === systemId || String(int.systemId) === String(systemId);
              if (matches) {
                console.log("Matched integration:", int);
              }
              return matches;
            }
          );
          console.log("Found integration from integrations array:", integration);
          if (!integration) {
            console.warn("No integration found for systemId:", systemId, "Available systemIds:", response.data.integrations.map(int => int.systemId));
          }
        } else if (Array.isArray(response.data)) {
          // Response.data is an array directly
          integration = response.data.find(
            (int) => int.systemId === systemId || String(int.systemId) === String(systemId)
          ) || response.data[0];
        } else {
          // Single integration object
          integration = response.data;
        }
      } else if (response && !response.data && typeof response === 'object' && !response.status) {
        // Response might be the integration object directly (not wrapped in data)
        integration = Array.isArray(response) ? response[0] : response;
      }
      
      if (integration) {
        console.log("Integration data received:", integration);
        console.log("Integration connectionDetails:", integration.connectionDetails);
        console.log("Integration systemId:", integration.systemId, "Looking for:", systemId);
        
        // Map dbType back to lowercase for dropdown
        const dbTypeMap = {
          MYSQL: "mysql",
          MONGODB: "mongodb",
          POSTGRESQL: "postgresql",
          ORACLE: "oracle",
          SQL_SERVER: "sqlserver",
          API: "api",
        };
        
        // Set integration type
        if (integration.dbType) {
        setIntegrationType(dbTypeMap[integration.dbType] || integration.dbType?.toLowerCase() || "mysql");
        }
        
        // Set enable integration toggle
        setEnableIntegration(integration.enabled !== false);
        
        // Set integration identifier
        setIntegrationIdentifier(integration.integrationId || integration.identifier || "");
        
        // Extract connection details - check both connectionDetails object and direct properties
        const conn = integration.connectionDetails || integration;
        
        console.log("Connection details object:", conn);
        console.log("conn.host:", conn?.host);
        console.log("conn.port:", conn?.port);
        console.log("conn.username:", conn?.username);
        console.log("conn.database:", conn?.database);
        
        // Set connection fields
        setHost(conn?.host || integration?.host || "");
        setPort(conn?.port?.toString() || integration?.port?.toString() || "");
        setUsername(conn?.username || integration?.username || "");
        setPassword(conn?.password || integration?.password || "");
        setDatabase(conn?.database || conn?.databaseName || integration?.database || integration?.databaseName || "");
        
        // Handle SSH - can be boolean (sshRequired) or string
        if (conn.sshRequired !== undefined) {
        setSsh(conn.sshRequired ? "yes" : "");
        } else if (integration.sshRequired !== undefined) {
          setSsh(integration.sshRequired ? "yes" : "");
        } else if (conn.ssh) {
          setSsh(conn.ssh);
        } else if (integration.ssh) {
          setSsh(integration.ssh);
        } else {
          setSsh("");
        }
        
        // Set dataset if available
        if (integration.datasetId || integration.dataset) {
          const datasetId = integration.datasetId || integration.dataset?.id || integration.dataset;
          // Always store the raw datasetId so it can be resolved later
          // if datasetsList hasn't loaded yet (race condition fix)
          setPendingDatasetId(datasetId);
          // Find the dataset in datasetsList
          const datasetListToUse = datasetsListRef || datasetsList;
          const dataset = datasetListToUse.find(d => 
            d.id === datasetId || 
            d.datasetId === datasetId ||
            String(d.id) === String(datasetId) ||
            String(d.datasetId) === String(datasetId)
          );
          if (dataset) {
            setSelectedDataset({
              value: dataset.id || dataset.datasetId,
              label: dataset.name || dataset.datasetId || "Unnamed Dataset"
            });
            console.log("Set selected dataset:", dataset.name || dataset.datasetId);
            setPendingDatasetId(null); // Already resolved, clear pending
          } else {
            console.log("Dataset not found in list yet, stored as pending:", datasetId, "Available datasets:", datasetListToUse.map(d => ({ id: d.id, datasetId: d.datasetId, name: d.name })));
          }
        }
        
        console.log("Form populated with:", {
          integrationType: dbTypeMap[integration.dbType] || integration.dbType?.toLowerCase(),
          integrationIdentifier: integration.integrationId || integration.identifier,
          host: conn.host || integration.host,
          port: conn.port?.toString() || integration.port?.toString(),
          username: conn.username || integration.username,
          password: conn.password || integration.password ? "***" : "",
          database: conn.database || conn.databaseName || integration.database || integration.databaseName,
          ssh: conn.sshRequired || integration.sshRequired ? "yes" : ""
        });
        
        return integration;
      } else {
        console.warn("No integration data found in response. Response:", response);
      }
      return null;
    } catch (error) {
      console.error("Error fetching integration data:", error);
      // Don't show error toast - integration might not exist yet
      return null;
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
    setIntegrationType(null);
    setEnableIntegration(true);
    setIntegrationIdentifier("");
    setHost("");
    setPort("");
    setUsername("");
    setPassword("");
    setDatabase("");
    setSsh("");
    setSelectedDataset(null);
    setPendingDatasetId(null);
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
    setIntegrationType(null);
    setEnableIntegration(true);
    setIntegrationIdentifier("");
    setHost("");
    setPort("");
    setUsername("");
    setPassword("");
    setDatabase("");
    setSsh("");
    setSelectedDataset(null);
    setPendingDatasetId(null);
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
        config.registry_db_integration_test,
        "POST",
        body,
        headers
      );

      if (response && (response.status === 200 || response.status === 201)) {
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
          config.registry_systems,
          "POST",
          body,
          headers
        );

        if (response && (response.status === 200 || response.status === 201)) {
          toast.success("System created successfully");
          // Extract system ID from response and switch to Integration tab
          const createdId = response.data?.id || response.data?.systemId || response.data?.data?.id || response.data?.data?.systemId;
          if (createdId) {
            setCreatedSystemId(createdId);
            // Update selectedSystem for integration tab
            setSelectedSystem({ id: createdId, systemId: createdId });
            setIsEditMode(true);
          }
          // Switch to Integration tab instead of navigating away
          setActiveTab(1);
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
          config.registry_systems_by_id(systemId),
          "PUT",
          body,
          headers
        );

        if (response && response.status === 200) {
          toast.success("System updated successfully");
          // Refresh systems list but stay on the page
          fetchSystems();
          // Switch to Integration tab instead of navigating away
          setActiveTab(1);
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

        // Extract datasetId from selectedDataset (react-select format: {value, label})
        let datasetId = null;
        
        console.log("=== Saving Integration ===");
        console.log("selectedDataset state:", JSON.stringify(selectedDataset, null, 2));
        console.log("selectedDataset type:", typeof selectedDataset);
        console.log("selectedDataset keys:", selectedDataset ? Object.keys(selectedDataset) : "null/undefined");
        console.log("datasetOptions available:", datasetOptions.length);
        console.log("datasetOptions sample:", datasetOptions.slice(0, 3));
        
        if (selectedDataset) {
          if (typeof selectedDataset === 'object' && selectedDataset !== null) {
            // React-select format: {value, label}
            console.log("selectedDataset.value:", selectedDataset.value);
            console.log("selectedDataset.id:", selectedDataset.id);
            console.log("selectedDataset.datasetId:", selectedDataset.datasetId);
            
            // Priority: value (from react-select) > id > datasetId
            if (selectedDataset.value !== undefined && selectedDataset.value !== null && selectedDataset.value !== "") {
              datasetId = selectedDataset.value;
            } else if (selectedDataset.id !== undefined && selectedDataset.id !== null && selectedDataset.id !== "") {
              datasetId = selectedDataset.id;
            } else if (selectedDataset.datasetId !== undefined && selectedDataset.datasetId !== null && selectedDataset.datasetId !== "") {
              datasetId = selectedDataset.datasetId;
            } else {
              datasetId = null;
            }
          } else if (typeof selectedDataset === 'string' || typeof selectedDataset === 'number') {
            // Direct ID value
            datasetId = selectedDataset;
          }
        } else {
          console.warn("⚠️ selectedDataset is null/undefined - no dataset selected!");
        }
        
        // Ensure datasetId is a valid value (not empty string or undefined)
        if (datasetId === "" || datasetId === undefined) {
          datasetId = null;
        }
        
        console.log("Extracted datasetId:", datasetId);
        console.log("Final datasetId to send:", datasetId);
        console.log("datasetId type:", typeof datasetId);
        
        // Check if we're updating an existing integration (has integrationIdentifier)
        const isUpdate = isEditMode && integrationIdentifier && integrationIdentifier.trim() !== "";
        
        // Build the body object
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
        
        // Explicitly add datasetId only if it has a valid value
        if (datasetId !== null && datasetId !== undefined && datasetId !== "") {
          body.datasetId = datasetId;
        } else {
          body.datasetId = null;
        }
        
        // If updating, include integrationId in the body
        if (isUpdate) {
          body.integrationId = integrationIdentifier.trim();
          console.log("🔄 Updating existing integration with ID:", integrationIdentifier);
        } else {
          console.log("➕ Creating new integration");
        }
        
        console.log("Final body being sent:", JSON.stringify({ ...body, connectionDetails: { ...body.connectionDetails, password: "***" } }, null, 2));
        console.log("datasetId in body:", body.datasetId);
        console.log("typeof datasetId in body:", typeof body.datasetId);

        // Use PUT for updates, POST for creates
        const method = isUpdate ? "PUT" : "POST";
        const url = isUpdate && integrationIdentifier 
          ? config.registry_db_integration_by_id(integrationIdentifier)
          : config.registry_db_integration;
        
        console.log("API Method:", method);
        console.log("API URL:", url);

        const response = await makeAPICall(
          url,
          method,
          body,
          headers
        );

        if (response && (response.status === 200 || response.status === 201)) {
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

  const handleDeleteSystem = async (system) => {
    if (!window.confirm("Are you sure you want to delete this system?")) {
      return;
    }

    const systemId = system.id || system.systemId;
    if (!systemId) {
      toast.error("System ID not found");
      return;
    }

    setLoading(true);
    try {
      const headers = {
        "Content-Type": "application/json",
        "X-Tenant-ID": tenant_id || "",
        "X-Business-ID": businessId || "",
      };

      const response = await makeAPICall(
        config.registry_systems_by_id(systemId),
        "DELETE",
        null,
        headers
      );

      if (response && (response.status === 200 || response.status === 204)) {
        toast.success("System deleted successfully");
        fetchSystems();
      } else {
        toast.error("Failed to delete system");
      }
    } catch (error) {
      console.error("Error deleting system:", error);
      const errorMessage = error?.errorList?.[0]?.errorMessage || error?.message || "Failed to delete system";
      toast.error(errorMessage);
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
    { value: "postgresql", label: "PostgreSQL" },
    { value: "mongodb", label: "MongoDB" },
    { value: "oracle", label: "Oracle" },
    { value: "sqlserver", label: "SQL Server" },
    { value: "api", label: "API" },
  ];

  // Dataset options - dynamically generated from fetched datasets
  const datasetOptions = useMemo(() => {
    return datasetsList.map(dataset => ({
      value: dataset.id || dataset.datasetId,
      label: dataset.name || dataset.datasetId || "Unnamed Dataset"
    }));
  }, [datasetsList]);

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
              onClick={() => {
                // Disable Integration tab if in create mode and system hasn't been saved yet
                if (!isEditMode && !createdSystemId) {
                  return;
                }
                setActiveTab(1);
                // Fetch integration data when switching to Integration tab in edit mode
                if (isEditMode && selectedSystem) {
                  const systemId = selectedSystem.id || selectedSystem.systemId;
                  if (systemId && !host && !port) {
                    // Only fetch if form is empty (data not loaded yet)
                    fetchIntegrationData(systemId, datasetsList);
                  }
                }
              }}
              style={{
                cursor: (!isEditMode && !createdSystemId) ? "not-allowed" : "pointer",
                paddingBottom: "12px",
                position: "relative",
                marginBottom: "-1px",
                opacity: (!isEditMode && !createdSystemId) ? 0.5 : 1
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
              <div style={{ display: "flex", alignItems: "center", gap: "4px", marginBottom: "8px" }}>
                <Text appearance="body-s-bold" color="primary-grey-100" style={{ fontSize: "14px", fontWeight: "600" }}>
                  Name
              </Text>
                <Text appearance="body-s-bold" color="primary-grey-100" style={{ fontSize: "14px", fontWeight: "600", color: "#666" }}>
                  (Required)
                </Text>
              </div>
              <InputFieldV2
                value={systemName}
                onChange={(e) => setSystemName(e.target.value)}
                placeholder="Enter system name"
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
            {/* <div style={{ marginBottom: "24px" }}>
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
            </div> */}

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
                value={integrationType ? integrationTypeOptions.find(opt => opt.value === integrationType) : null}
                onChange={(selected) => setIntegrationType(selected ? selected.value : null)}
                placeholder="Select integration type"
                isClearable={false}
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

            {/* Show other fields: always in edit mode, or when integration type is selected in create mode */}
            {(isEditMode || integrationType) && (
              <>
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
                    {/* Integration identifier field - commented out as it's auto-created in backend */}
                    {/* <div style={{ marginBottom: "24px" }}>
                      <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "8px", fontSize: "14px", fontWeight: "600" }}>
                    Integration identifier
                  </Text>
                  <InputFieldV2
                    value={integrationIdentifier}
                    onChange={(e) => setIntegrationIdentifier(e.target.value)}
                    placeholder="Enter integration identifier"
                  />
                    </div> */}

                <div style={{ marginBottom: "24px" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "4px", marginBottom: "8px" }}>
                    <Text appearance="body-s-bold" color="primary-grey-100" style={{ fontSize: "14px", fontWeight: "600" }}>
                      Host
                  </Text>
                    <Text appearance="body-s-bold" color="primary-grey-100" style={{ fontSize: "14px", fontWeight: "600", color: "#666" }}>
                      (Required)
                    </Text>
                  </div>
                  <InputFieldV2
                    value={host}
                    onChange={(e) => setHost(e.target.value)}
                    placeholder="Enter host"
                  />
                </div>

                <div style={{ marginBottom: "24px" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "4px", marginBottom: "8px" }}>
                    <Text appearance="body-s-bold" color="primary-grey-100" style={{ fontSize: "14px", fontWeight: "600" }}>
                      Port
                  </Text>
                    <Text appearance="body-s-bold" color="primary-grey-100" style={{ fontSize: "14px", fontWeight: "600", color: "#666" }}>
                      (Required)
                    </Text>
                  </div>
                  <InputFieldV2
                    value={port}
                    onChange={(e) => setPort(e.target.value)}
                    placeholder="Enter port"
                  />
                </div>

                <div style={{ marginBottom: "24px" }}>
                  <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "8px", fontSize: "14px", fontWeight: "600" }}>
                    Username
                  </Text>
                  <InputFieldV2
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="Enter username"
                  />
                </div>

                <div style={{ marginBottom: "24px" }}>
                  <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "8px", fontSize: "14px", fontWeight: "600" }}>
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
                  <div style={{ display: "flex", alignItems: "center", gap: "4px", marginBottom: "8px" }}>
                    <Text appearance="body-s-bold" color="primary-grey-100" style={{ fontSize: "14px", fontWeight: "600" }}>
                      Database
                  </Text>
                    <Text appearance="body-s-bold" color="primary-grey-100" style={{ fontSize: "14px", fontWeight: "600", color: "#666" }}>
                      (Required)
                    </Text>
                  </div>
                  <InputFieldV2
                    value={database}
                    onChange={(e) => setDatabase(e.target.value)}
                    placeholder="Enter database name"
                  />
                </div>

                <div style={{ marginBottom: "24px" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "4px", marginBottom: "8px" }}>
                    <Text appearance="body-s-bold" color="primary-grey-100" style={{ fontSize: "14px", fontWeight: "600" }}>
                      SSH
                  </Text>
                    <Text appearance="body-s-bold" color="primary-grey-100" style={{ fontSize: "14px", fontWeight: "600", color: "#666" }}>
                      (Optional)
                    </Text>
                  </div>
                  <InputFieldV2
                    value={ssh}
                    onChange={(e) => setSsh(e.target.value)}
                    placeholder="Enter SSH details (leave empty if not required)"
                  />
                </div>

                <div style={{ marginBottom: "32px" }}>
                  <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "8px", fontSize: "14px", fontWeight: "600" }}>
                    Dataset
                  </Text>
                  <Select
                    options={datasetOptions}
                    value={selectedDataset}
                    onChange={(option) => {
                      console.log("Dataset onChange triggered with option:", option);
                      console.log("Option type:", typeof option);
                      console.log("Option value:", option?.value);
                      console.log("Option label:", option?.label);
                      if (option) {
                        setSelectedDataset(option);
                        console.log("✅ selectedDataset state updated to:", option);
                      } else {
                        console.warn("⚠️ Option is null - clearing selection");
                        setSelectedDataset(null);
                      }
                    }}
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
              </>
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
                {/* System tag column - commented out
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
                */}
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
                  <td colSpan="3" style={{ padding: "80px 20px", textAlign: "center" }}>
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
                    {/* System tag column - commented out
                    <td style={{ padding: "12px 16px" }}>
                      <Text appearance="body-xs" color="primary-grey-100">
                        {system.tag || "-"}
                      </Text>
                    </td>
                    */}
                    <td style={{ padding: "12px 16px" }}>
                      <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
                        <button
                          onClick={() => handleEditSystem(system)}
                          aria-label="Edit system"
                          style={{
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            width: "32px",
                            height: "32px",
                            borderRadius: "50%",
                            border: "none",
                            backgroundColor: "transparent",
                            color: "#666",
                            cursor: "pointer",
                            transition: "background-color 0.2s ease",
                            padding: 0,
                          }}
                          onMouseEnter={(e) => {
                            e.currentTarget.style.backgroundColor = "rgba(0, 0, 0, 0.05)";
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.backgroundColor = "transparent";
                          }}
                        >
                          <IcEditPen height={18} width={18} color="#666" />
                        </button>
                        <button
                          onClick={() => handleDeleteSystem(system)}
                          aria-label="Delete system"
                          style={{
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            width: "32px",
                            height: "32px",
                            borderRadius: "50%",
                            border: "none",
                            backgroundColor: "transparent",
                            color: "#666",
                            cursor: "pointer",
                            transition: "background-color 0.2s ease",
                            padding: 0,
                          }}
                          onMouseEnter={(e) => {
                            e.currentTarget.style.backgroundColor = "rgba(0, 0, 0, 0.05)";
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.backgroundColor = "transparent";
                          }}
                        >
                          <IcTrash height={18} width={18} color="#666" />
                        </button>
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
        position="top-right"
        transition={Slide}
        autoClose={3000}
        hideProgressBar={false}
        newestOnTop
        closeOnClick
        rtl={false}
        pauseOnFocusLoss={false}
        draggable
        pauseOnHover
      />
    </div>
  );
};

export default Systems;
