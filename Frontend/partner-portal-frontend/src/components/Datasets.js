import React, { useState, useEffect, useMemo } from "react";
import "../styles/masterSetup.css";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import { Text, InputFieldV2, ActionButton, Icon, Spinner, Button } from "../custom-components";
import { IcClose, IcEditPen, IcTrash, IcSort, IcFilter, IcChevronLeft } from "../custom-components/Icon";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate, useLocation } from "react-router-dom";
import Select from "react-select";
import { makeAPICall } from "../utils/ApiCall";
import config from "../utils/config";
import { getAllTemplates, getDataTypes } from "../store/actions/CommonAction";

// UUID generator function (same as in CommonAction.js)
const uuidv4 = () => {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0,
      v = c == "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

const Datasets = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  
  // Always check pathname directly to ensure form shows on edit route
  const currentPath = location.pathname;
  const isEditRoute = currentPath.startsWith("/consent-datasets/edit/");
  const isCreateRoute = currentPath === "/consent-datasets/create";
  
  // Check URL on mount to set initial state
  const getInitialShowCreateForm = () => {
    if (typeof window !== 'undefined') {
      const path = window.location.pathname;
      return path === "/consent-datasets/create" || path.startsWith("/consent-datasets/edit/");
    }
    return false;
  };
  
  const [showCreateForm, setShowCreateForm] = useState(getInitialShowCreateForm());
  const [showDatasetDetail, setShowDatasetDetail] = useState(false);
  const [selectedDataset, setSelectedDataset] = useState(null);
  const [datasetType, setDatasetType] = useState("yaml"); // "yaml", "database", or "openmetadata"
  const [uploadedCsvFile, setUploadedCsvFile] = useState(null);
  const [csvValidationError, setCsvValidationError] = useState(null);
  const [fieldSortColumn, setFieldSortColumn] = useState(null);
  const [fieldSortDirection, setFieldSortDirection] = useState("asc");
  const [showEditFieldModal, setShowEditFieldModal] = useState(false);
  const [editingField, setEditingField] = useState(null);
  const [fieldDescription, setFieldDescription] = useState("");
  const [selectedDataCategories, setSelectedDataCategories] = useState([]);
  const [selectedConsentTemplates, setSelectedConsentTemplates] = useState([]);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [fieldToDelete, setFieldToDelete] = useState(null);
  const [isEditMode, setIsEditMode] = useState(false);
  const [editingDatasetId, setEditingDatasetId] = useState(null);
  const [fieldUpdates, setFieldUpdates] = useState({}); // Store field updates: { fieldId: { consentTemplates, description, category } }

  // Data category options - will be fetched from API
  const [dataCategoryOptions, setDataCategoryOptions] = useState([
    { value: "system", label: "system" },
    { value: "user.content", label: "user.content" },
    { value: "user.demographic", label: "user.demographic" },
    { value: "user.contact", label: "user.contact" },
  ]);

  // Consent template options - will be fetched from API
  const [consentTemplateOptions, setConsentTemplateOptions] = useState([]);
  const [yamlContent, setYamlContent] = useState(`dataset:
  name: user_pii_dataset
  description: User PII dataset with core profile, addresses, and identity documents
  data_categories:
    - user
  retention:
    policy: no_retention_policy_defined
  collections:
    - name: users
      description: Core user profile table
      primary_key: user_id
      fields:
        - name: user_id
          data_categories: [user.unique_id]
          is_primary_key: true
        - name: full_name
          data_categories: [user.name]
        - name: email
          data_categories: [user.contact.email]
        - name: phone_number
          data_categories: [user.contact.phone_number]
        - name: date_of_birth
          data_categories: [user.demographic.date_of_birth]
        - name: created_at
          data_categories: [system.operations]`);
  const [databaseUrl, setDatabaseUrl] = useState("");
  const [selectedCollection, setSelectedCollection] = useState("all"); // "all" shows all collections
  
  // Parse YAML to extract fields from collections
  const getDatasetFields = (dataset) => {
    if (!dataset) {
      console.log("getDatasetFields: No dataset provided");
      return [];
    }
    
    if (!dataset.datasetYaml) {
      console.log("getDatasetFields: No datasetYaml in dataset", dataset);
    return [];
    }

    try {
      const yamlContent = dataset.datasetYaml;
      console.log("getDatasetFields: Parsing YAML, length:", yamlContent.length);
      const fields = [];
      let fieldId = 1;

      // Split YAML into lines for easier processing
      const lines = yamlContent.split('\n');
      let inCollections = false;
      let inCollection = false;
      let inFields = false;
      let currentCollection = "";
      let currentField = null;
      let collectionIndentLevel = 0;
      let fieldsIndentLevel = 0;
      
      for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        const trimmed = line.trim();
        const leadingSpaces = line.length - line.trimStart().length;
        
        // Skip empty lines
        if (!trimmed) continue;
        
        // Detect collections section
        if (trimmed.startsWith('collections:')) {
          inCollections = true;
          inCollection = false;
          inFields = false;
          continue;
        }
        
        // Detect fields section within a collection FIRST
        // This must be checked before collection detection
        if (inCollection && trimmed.startsWith('fields:')) {
          inFields = true;
          fieldsIndentLevel = leadingSpaces;
          console.log("getDatasetFields: Found fields section in collection:", currentCollection, "at indent:", fieldsIndentLevel);
          continue;
        }
        
        // Detect field start - must be inside fields section
        // Fields are indented more than "fields:" (usually 6+ spaces)
        if (inFields && trimmed.startsWith('- name:')) {
          // Double-check we're actually in fields by checking indentation
          // Fields should be indented more than the "fields:" line
          if (leadingSpaces > fieldsIndentLevel || fieldsIndentLevel === 0) {
            // Save previous field if exists
            if (currentField && currentField.name) {
            fields.push({
              id: fieldId++,
              fieldName: currentField.name,
              description: currentField.description || `Field from ${currentCollection} collection`,
              category: currentField.category || "personal identifier",
              tags: currentField.tags.length > 0 ? currentField.tags : ["personal identifier"],
              consentTemplates: currentField.consentTemplateIds || [], // Consent template IDs from YAML
              consentTemplateIds: currentField.consentTemplateIds || [], // Keep IDs for reference
              collectionName: currentCollection
            });
            }
            
            // Start new field
            currentField = {
              name: trimmed.replace('- name:', '').trim(),
              description: "",
              category: "personal identifier",
              tags: [],
              consentTemplates: [],
              consentTemplateIds: [] // Store template IDs from YAML
            };
            console.log("getDatasetFields: Found field:", currentField.name, "in collection:", currentCollection, "at indent:", leadingSpaces, "fieldsIndentLevel:", fieldsIndentLevel);
            continue;
          } else {
            // This might be a new collection at the same level, exit fields
            console.log("getDatasetFields: Exiting fields section, found - name: at indent", leadingSpaces, "which is <= fieldsIndentLevel", fieldsIndentLevel);
            inFields = false;
          }
        }
        
        // Detect collection start - only if NOT in fields section
        // Collections are at 2-4 spaces indent, directly under collections:
        if (inCollections && !inFields && trimmed.startsWith('- name:')) {
          // Save previous field if we're ending a collection
          if (currentField && currentField.name) {
            fields.push({
              id: fieldId++,
              fieldName: currentField.name,
              description: currentField.description || `Field from ${currentCollection} collection`,
              category: currentField.category || "personal identifier",
              tags: currentField.tags.length > 0 ? currentField.tags : ["personal identifier"],
              collectionName: currentCollection
            });
            currentField = null;
          }
          
          inCollection = true;
          inFields = false;
          currentCollection = trimmed.replace('- name:', '').trim();
          collectionIndentLevel = leadingSpaces;
          console.log("getDatasetFields: Found collection:", currentCollection, "at indent:", collectionIndentLevel);
          continue;
        }
        
        // Parse field properties - must be indented more than the field name
        if (inFields && currentField && currentField.name && leadingSpaces > fieldsIndentLevel + 2) {
          if (trimmed.startsWith('data_categories:')) {
            // Handle array format: data_categories: [user.unique_id]
            const arrayMatch = trimmed.match(/data_categories:\s*\[(.+?)\]/);
            if (arrayMatch) {
              currentField.tags = arrayMatch[1].split(',').map(cat => cat.trim().replace(/['"]/g, ''));
              currentField.category = currentField.tags[0] || "personal identifier";
              console.log("getDatasetFields: Found data_categories (array) for", currentField.name, ":", currentField.tags);
            } else {
              // Handle list format - check next lines
              let j = i + 1;
              while (j < lines.length) {
                const nextLine = lines[j];
                const nextTrimmed = nextLine.trim();
                const nextLeadingSpaces = nextLine.length - nextLine.trimStart().length;
                
                // Stop if we hit a line at same or less indentation (new field or end of fields)
                if (nextLeadingSpaces <= leadingSpaces && (nextTrimmed.startsWith('-') || nextTrimmed === '')) {
                  break;
                }
                
                if (nextTrimmed.startsWith('-')) {
                  const catValue = nextTrimmed.replace(/^-\s*/, '').trim();
                  if (catValue) {
                    currentField.tags.push(catValue);
                  }
                }
                j++;
              }
              if (currentField.tags.length > 0) {
                currentField.category = currentField.tags[0];
                console.log("getDatasetFields: Found data_categories (list) for", currentField.name, ":", currentField.tags);
              }
              i = j - 1; // Skip processed lines
            }
          } else if (trimmed.startsWith('description:')) {
            currentField.description = trimmed.replace('description:', '').trim();
          } else if (trimmed.startsWith('consent_templates:')) {
            // Handle array format: consent_templates: [901c7207-9613-4f16-990c-be5533bd993e]
            const arrayMatch = trimmed.match(/consent_templates:\s*\[(.+?)\]/);
            if (arrayMatch) {
              const templateIds = arrayMatch[1].split(',').map(id => id.trim().replace(/['"]/g, ''));
              currentField.consentTemplateIds = templateIds;
              console.log("getDatasetFields: Found consent_templates for", currentField.name, ":", templateIds);
            } else {
              // Handle list format - check next lines
              let j = i + 1;
              while (j < lines.length) {
                const nextLine = lines[j];
                const nextTrimmed = nextLine.trim();
                const nextLeadingSpaces = nextLine.length - nextLine.trimStart().length;
                
                // Stop if we hit a line at same or less indentation (new field or end of fields)
                if (nextLeadingSpaces <= leadingSpaces && (nextTrimmed.startsWith('-') || nextTrimmed === '')) {
                  break;
                }
                
                if (nextTrimmed.startsWith('-')) {
                  const templateId = nextTrimmed.replace(/^-\s*/, '').trim();
                  if (templateId) {
                    currentField.consentTemplateIds.push(templateId);
                  }
                }
                j++;
              }
              console.log("getDatasetFields: Found consent_templates (list) for", currentField.name, ":", currentField.consentTemplateIds);
              i = j - 1; // Skip processed lines
            }
          }
        }
        
        // Detect end of collection (when we hit a new collection at same or less indent)
        if (inCollection && !inFields && trimmed.startsWith('- name:') && leadingSpaces <= collectionIndentLevel) {
          // This is a new collection, save previous field if exists
          if (currentField && currentField.name) {
            fields.push({
              id: fieldId++,
              fieldName: currentField.name,
              description: currentField.description || `Field from ${currentCollection} collection`,
              category: currentField.category || "personal identifier",
              tags: currentField.tags.length > 0 ? currentField.tags : ["personal identifier"],
              collectionName: currentCollection
            });
          }
          currentField = null;
          currentCollection = trimmed.replace('- name:', '').trim();
          collectionIndentLevel = leadingSpaces;
          console.log("getDatasetFields: Starting new collection:", currentCollection);
        }
        
        // Detect end of fields section (when we hit a new collection or end)
        if (inFields && leadingSpaces <= collectionIndentLevel && trimmed.startsWith('- name:')) {
          // Save last field before starting new collection
          if (currentField && currentField.name) {
            fields.push({
              id: fieldId++,
              fieldName: currentField.name,
              description: currentField.description || `Field from ${currentCollection} collection`,
              category: currentField.category || "personal identifier",
              tags: currentField.tags.length > 0 ? currentField.tags : ["personal identifier"],
              consentTemplates: currentField.consentTemplateIds || [], // Consent template IDs from YAML
              consentTemplateIds: currentField.consentTemplateIds || [], // Keep IDs for reference
              collectionName: currentCollection
            });
          }
          currentField = null;
          inFields = false;
          inCollection = true;
          currentCollection = trimmed.replace('- name:', '').trim();
          collectionIndentLevel = leadingSpaces;
          console.log("getDatasetFields: End of fields, starting new collection:", currentCollection);
        }
      }
      
      // Save last field if exists
      if (currentField && currentField.name) {
        fields.push({
          id: fieldId++,
          fieldName: currentField.name,
          description: currentField.description || `Field from ${currentCollection} collection`,
          category: currentField.category || "personal identifier",
          tags: currentField.tags.length > 0 ? currentField.tags : ["personal identifier"],
          collectionName: currentCollection
        });
      }
      
      console.log("getDatasetFields: Extracted", fields.length, "fields");
      return fields;
    } catch (error) {
      console.error("Error parsing dataset YAML:", error);
      return [];
    }
  };
  
  const [datasetsList, setDatasetsList] = useState([
    {
      id: 1,
      name: "User meta information",
      description: "JioSign events schema",
      key: "user-meta-information"
    },
    {
      id: 2,
      name: "User document information",
      description: "JioSign document schema",
      key: "user-document-information"
    },
    {
      id: 3,
      name: "User database dataset",
      description: "Dataset representing user identifiers and PII datasets",
      key: "user-db-dataset"
    },
    {
      id: 4,
      name: "User account details",
      description: "JioSign ba schema",
      key: "user-account-information"
    }
  ]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState("asc");

  // Redux selectors
  const businessId = useSelector((state) => state.common.business_id);
  const tenant_id = useSelector((state) => state.common.tenant_id);
  const session_token = useSelector((state) => state.common.session_token);

  const fetchDatasets = async () => {
    setLoading(true);
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
        
        // Parse datasetYaml to extract name, description, and key
        const parsedDatasets = datasetsData.map(dataset => {
          let name = dataset.name;
          let description = dataset.description;
          let key = dataset.key;
          
          // If name/description/key are not directly available, try to parse from datasetYaml
          if (dataset.datasetYaml && (!name || !description || !key)) {
            try {
              const yamlContent = dataset.datasetYaml;
              // Priority: dataset.name > direct name field > first collection name
              // Extract dataset name from YAML (format: "dataset:\n  name: value")
              const datasetNameMatch = yamlContent.match(/dataset:\s*\n\s*name:\s*(.+?)(?:\n|$)/);
              if (datasetNameMatch && !name) {
                name = datasetNameMatch[1].trim();
              } else if (!name) {
                // Fallback: try to find any "name:" field (but not from collections)
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
              
              // Extract description if available in YAML (format: "dataset:\n  description: value")
              const descMatch = yamlContent.match(/dataset:\s*\n\s*description:\s*(.+?)(?:\n|$)/);
              if (descMatch && !description) {
                description = descMatch[1].trim();
              } else if (!description) {
                // Fallback: try to find description anywhere
                const descMatchFallback = yamlContent.match(/description:\s*(.+?)(?:\n|$)/);
                if (descMatchFallback) {
                  description = descMatchFallback[1].trim();
                }
              }
              
              // Use datasetId as key if key is not available
              if (!key && dataset.datasetId) {
                key = dataset.datasetId;
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
            name: name || dataset.datasetId || "-",
            description: description || "-",
            key: key || dataset.datasetId || "-",
            id: dataset.id || dataset.datasetId
          };
        });
        
        setDatasetsList(parsedDatasets);
      } else {
        setDatasetsList([]);
      }
    } catch (error) {
      console.error("Error fetching datasets:", error);
      const errorMessage = error?.errorList?.[0]?.errorMessage || error?.message || "Failed to fetch datasets";
      toast.error(errorMessage);
      setDatasetsList([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchDatasetById = async (datasetId) => {
    if (!datasetId) {
      console.error("Dataset ID is required");
      return null;
    }
    
    setLoading(true);
    try {
      const headers = {
        "Content-Type": "application/json",
        "X-Tenant-ID": tenant_id || "",
        "X-Business-ID": businessId || "",
      };

      const response = await makeAPICall(
        config.registry_datasets_by_id(datasetId),
        "GET",
        null,
        headers
      );

      if (response && response.data) {
        const dataset = response.data;
        // Extract YAML content from response
        const yamlContent = dataset.datasetYaml || dataset.yamlContent || "";
        console.log("Fetched YAML content length:", yamlContent.length);
        setYamlContent(yamlContent || "");
        setDatasetType("yaml"); // Assuming edit is always for YAML type
        // Ensure form is shown
        setShowCreateForm(true);
        setIsEditMode(true);
        return dataset;
      } else {
        // If no data, set empty YAML so form still shows
        setYamlContent("");
        setShowCreateForm(true);
        setIsEditMode(true);
        toast.error("Dataset data not found");
      return null;
      }
    } catch (error) {
      console.error("Error fetching dataset:", error);
      const errorMessage = error?.errorList?.[0]?.errorMessage || error?.message || "Failed to fetch dataset";
      toast.error(errorMessage);
      // Set empty YAML so form still shows even on error
      setYamlContent("");
      setShowCreateForm(true);
      setIsEditMode(true);
      return null;
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    console.log("useEffect triggered - pathname:", location.pathname);
    const isCreateRoute = location.pathname === "/consent-datasets/create";
    const isEditRoute = location.pathname.startsWith("/consent-datasets/edit/");
    const isDetailRoute = location.pathname.startsWith("/consent-datasets/") && 
                          location.pathname !== "/consent-datasets" && 
                          location.pathname !== "/consent-datasets/create" &&
                          !location.pathname.startsWith("/consent-datasets/edit/");
    
    console.log("Route checks - isCreateRoute:", isCreateRoute, "isEditRoute:", isEditRoute, "isDetailRoute:", isDetailRoute);
    
    if (isCreateRoute) {
      setShowCreateForm(true);
      setShowDatasetDetail(false);
      setIsEditMode(false);
      setEditingDatasetId(null);
    } else if (isEditRoute) {
      const pathParts = location.pathname.split("/");
      const datasetId = pathParts[pathParts.length - 1];
      console.log("Edit route detected, datasetId:", datasetId, "full pathname:", location.pathname);
      if (datasetId && datasetId !== "edit") {
        console.log("Setting showCreateForm to true");
        // Set form to show immediately - this is critical
        setShowCreateForm(true);
        setShowDatasetDetail(false);
        setIsEditMode(true);
        setEditingDatasetId(datasetId);
        setDatasetType("yaml");
        // Reset YAML content while loading
        setYamlContent("");
        // Fetch dataset data
        fetchDatasetById(datasetId).catch(error => {
          console.error("Error fetching dataset:", error);
          toast.error("Failed to load dataset. You can still edit manually.");
          // Keep form visible even if fetch fails
        });
      } else {
        console.error("Invalid dataset ID:", datasetId);
        toast.error("Dataset ID not found");
        navigate("/consent-datasets");
      }
    } else if (isDetailRoute) {
      const datasetKey = location.pathname.split("/consent-datasets/")[1];
      const dataset = datasetsList.find(d => d.key === datasetKey || d.id === datasetKey || d.datasetId === datasetKey);
      if (dataset) {
        // If dataset doesn't have YAML, fetch it
        if (!dataset.datasetYaml && (dataset.id || dataset.datasetId)) {
          const datasetId = dataset.id || dataset.datasetId;
          fetchDatasetById(datasetId).then(fullDataset => {
            if (fullDataset) {
              setSelectedDataset({ ...dataset, ...fullDataset });
              setShowDatasetDetail(true);
              setShowCreateForm(false);
            } else {
              setSelectedDataset(dataset);
              setShowDatasetDetail(true);
              setShowCreateForm(false);
            }
          });
        } else {
          setSelectedDataset(dataset);
          setShowDatasetDetail(true);
          setShowCreateForm(false);
        }
      } else {
        // Try to fetch by ID if not in list
        fetchDatasetById(datasetKey).then(fullDataset => {
          if (fullDataset) {
            setSelectedDataset(fullDataset);
            setShowDatasetDetail(true);
            setShowCreateForm(false);
          } else {
            navigate("/consent-datasets");
          }
        }).catch(() => {
          navigate("/consent-datasets");
        });
      }
    } else {
      setShowCreateForm(false);
      setShowDatasetDetail(false);
      setSelectedDataset(null);
      setIsEditMode(false);
      setEditingDatasetId(null);
      fetchDatasets();
    }
  }, [location.pathname, location.state, navigate]);
  
  // Fetch consent templates when dataset detail is shown
  useEffect(() => {
    if (showDatasetDetail && consentTemplateOptions.length === 0) {
      fetchConsentTemplates();
    }
  }, [showDatasetDetail]);

  // Fetch data types and data items on component mount
  useEffect(() => {
    const fetchDataTypes = async () => {
      try {
        const res = await dispatch(getDataTypes());
        if (res?.status === 200 || res?.status === 201) {
          if (res?.data?.searchList && res.data.searchList.length > 0) {
            // Flatten all data items from all data types into a single array
            const allDataItems = [];
            res.data.searchList.forEach((dataType) => {
              if (dataType.dataItems && Array.isArray(dataType.dataItems)) {
                dataType.dataItems.forEach((item) => {
                  // Check if item already exists (case-insensitive)
                  const exists = allDataItems.some(
                    (existing) => existing.value.toLowerCase() === item.toLowerCase()
                  );
                  if (!exists) {
                    allDataItems.push({
                      value: item,
                      label: item,
                    });
                  }
                });
              }
            });
            
            // Sort alphabetically by label
            allDataItems.sort((a, b) => a.label.localeCompare(b.label));
            
            // Update data category options
            setDataCategoryOptions(allDataItems);
            console.log("Fetched data items:", allDataItems.length, "items");
          }
        }
      } catch (error) {
        console.error("Error fetching data types:", error);
        // Keep default static options on error
      }
    };

    fetchDataTypes();
  }, [dispatch]);
  
  // Reset collection filter when dataset changes
  useEffect(() => {
    if (showDatasetDetail && selectedDataset) {
      setSelectedCollection("all");
    }
  }, [selectedDataset?.id || selectedDataset?.datasetId, showDatasetDetail]);

  // Filter datasets based on search query
  const filteredDatasets = useMemo(() => {
    if (!searchQuery) return datasetsList;
    const query = searchQuery.toLowerCase();
    return datasetsList.filter(
      (dataset) =>
        dataset.name?.toLowerCase().includes(query) ||
        dataset.description?.toLowerCase().includes(query) ||
        dataset.key?.toLowerCase().includes(query)
    );
  }, [datasetsList, searchQuery]);

  // Sort datasets
  const sortedDatasets = useMemo(() => {
    if (!sortColumn) return filteredDatasets;

    return [...filteredDatasets].sort((a, b) => {
      let aValue = a[sortColumn] || "";
      let bValue = b[sortColumn] || "";

      if (aValue < bValue) return sortDirection === "asc" ? -1 : 1;
      if (aValue > bValue) return sortDirection === "asc" ? 1 : -1;
      return 0;
    });
  }, [filteredDatasets, sortColumn, sortDirection]);

  const handleSort = (column) => {
    if (sortColumn === column) {
      setSortDirection(sortDirection === "asc" ? "desc" : "asc");
    } else {
      setSortColumn(column);
      setSortDirection("asc");
    }
  };

  // CSV parsing function that handles quoted fields
  const parseCSVLine = (line) => {
    const result = [];
    let current = '';
    let inQuotes = false;
    
    for (let i = 0; i < line.length; i++) {
      const char = line[i];
      const nextChar = line[i + 1];
      
      if (char === '"') {
        if (inQuotes && nextChar === '"') {
          current += '"';
          i++; // Skip next quote
        } else {
          inQuotes = !inQuotes;
        }
      } else if (char === ',' && !inQuotes) {
        result.push(current.trim());
        current = '';
      } else {
        current += char;
      }
    }
    result.push(current.trim());
    return result;
  };

  // Helper function to extract data category from tags
  const extractDataCategoryFromTags = (tags, columnName) => {
    if (!tags) {
      // If no tags, fall through to column name patterns
    } else {
    
    const tagsArray = tags.split(';').map(t => t.trim());
    
    // Check for PII tags first
    if (tagsArray.some(t => t.includes('PII.Email'))) {
      return 'user.contact.email';
    }
    if (tagsArray.some(t => t.includes('PII.Name'))) {
      return 'user.name';
    }
    if (tagsArray.some(t => t.includes('PII.Phone'))) {
      return 'user.contact.phone_number';
    }
    if (tagsArray.some(t => t.includes('PII.Address'))) {
      return 'user.contact.address';
    }
    if (tagsArray.some(t => t.includes('PII.DateOfBirth'))) {
      return 'user.demographic.date_of_birth';
    }
    }
    
    // Fallback to column name patterns
    const colName = (columnName || '').toLowerCase();
    if (colName.includes('email')) {
      return 'user.contact.email';
    }
    if (colName.includes('phone') || colName.includes('mobile')) {
      return 'user.contact.phone_number';
    }
    if (colName.includes('name') && (colName.includes('first') || colName.includes('last') || colName.includes('full'))) {
      return 'user.name';
    }
    if (colName.includes('dob') || colName.includes('birth') || colName.includes('age')) {
      return 'user.demographic.date_of_birth';
    }
    if (colName.includes('address')) {
      return 'user.contact.address';
    }
    if ((colName.includes('id') && (colName.includes('user') || colName.includes('customer')))) {
      return 'user.unique_id';
    }
    if (colName.includes('created') || colName.includes('updated') || colName.includes('timestamp')) {
      return 'system.operations';
    }
    
    return 'user'; // Default
  };

  // CSV to YAML conversion function for Open Metadata format
  const convertCsvToYaml = (csvText) => {
    try {
      const lines = csvText.split(/\r?\n/).filter(line => line.trim());
      if (lines.length < 2) {
        throw new Error("CSV file must contain at least a header row and one data row");
      }

      // Parse CSV header with proper CSV parsing
      const headers = parseCSVLine(lines[0]).map(h => h.trim().replace(/^"|"$/g, ''));
      
      // Required columns validation for Open Metadata format
      const requiredColumns = ['name*', 'entitytype*', 'fullyqualifiedname'];
      const missingColumns = requiredColumns.filter(col => !headers.some(h => h.toLowerCase().replace(/\s+/g, '') === col.toLowerCase().replace(/\s+/g, '')));
      if (missingColumns.length > 0) {
        // Check if it's the old format
        if (headers.includes('table_name') && headers.includes('column_name')) {
          throw new Error("This appears to be the old CSV format. Please use Open Metadata CSV format with columns: name*, entityType*, fullyQualifiedName");
        }
        throw new Error(`Missing required columns: ${missingColumns.join(', ')}. Found columns: ${headers.join(', ')}`);
      }

      // Find column indices (case-insensitive)
      const getColumnIndex = (colName) => {
        const normalized = colName.toLowerCase().replace(/\s+/g, '').replace(/\*/g, '');
        return headers.findIndex(h => h.toLowerCase().replace(/\s+/g, '').replace(/\*/g, '') === normalized);
      };

      const nameIdx = getColumnIndex('name*');
      const entityTypeIdx = getColumnIndex('entityType*');
      const fullyQualifiedNameIdx = getColumnIndex('fullyQualifiedName');
      const descriptionIdx = getColumnIndex('description');
      const tagsIdx = getColumnIndex('tags');
      const dataTypeIdx = getColumnIndex('column.dataType');

      if (nameIdx === -1 || entityTypeIdx === -1 || fullyQualifiedNameIdx === -1) {
        throw new Error("Required columns not found in CSV");
      }

      // Parse data rows
      const rows = [];
      for (let i = 1; i < lines.length; i++) {
        if (!lines[i].trim()) continue;
        const values = parseCSVLine(lines[i]).map(v => v.trim().replace(/^"|"$/g, ''));
        if (values.length !== headers.length) {
          console.warn(`Row ${i + 1} has ${values.length} columns but expected ${headers.length}, skipping`);
          continue;
        }
        
        const row = {
          name: values[nameIdx] || '',
          entityType: values[entityTypeIdx] || '',
          fullyQualifiedName: values[fullyQualifiedNameIdx] || '',
          description: descriptionIdx >= 0 ? values[descriptionIdx] || '' : '',
          tags: tagsIdx >= 0 ? values[tagsIdx] || '' : '',
          dataType: dataTypeIdx >= 0 ? values[dataTypeIdx] || '' : '',
        };
        rows.push(row);
      }

      if (rows.length === 0) {
        throw new Error("No valid data rows found in CSV");
      }

      // Parse fullyQualifiedName to extract table and column info
      // Format: database.schema.table.column (for columns)
      // Format: database.schema.table (for tables)
      // Format: database.schema (for schemas)
      
      const tablesMap = {};
      const tableDescriptions = {};
      
      // First pass: collect table information
      rows.forEach(row => {
        if (row.entityType.toLowerCase() === 'table') {
          const parts = row.fullyQualifiedName.split('.');
          if (parts.length >= 3) {
            const tableName = parts[parts.length - 1]; // Last part is table name
            tablesMap[tableName] = {
              name: tableName,
              description: row.description || `${tableName} table`,
              fields: []
          };
            tableDescriptions[tableName] = row.description || `${tableName} table`;
          }
        }
      });

      // Second pass: collect column information
      rows.forEach(row => {
        if (row.entityType.toLowerCase() === 'column') {
          const parts = row.fullyQualifiedName.split('.');
          if (parts.length >= 4) {
            const tableName = parts[parts.length - 2]; // Second to last is table name
            const columnName = parts[parts.length - 1]; // Last part is column name
            
            if (!tablesMap[tableName]) {
              tablesMap[tableName] = {
                name: tableName,
                description: `${tableName} table`,
                fields: []
              };
            }

            // Determine data category from tags or column name
            const dataCategory = extractDataCategoryFromTags(row.tags, columnName);
            
            const field = {
              name: columnName,
              data_categories: [dataCategory]
            };

            // Check if it's a primary key (usually ends with _id and matches table pattern)
            const normalizedTableName = tableName.toLowerCase().replace(/\s+/g, '_');
            const normalizedColumnName = columnName.toLowerCase();
            if (normalizedColumnName === `${normalizedTableName}_id` || 
                normalizedColumnName === 'id' ||
                (normalizedColumnName.includes('_id') && normalizedColumnName.endsWith('_id'))) {
              field.is_primary_key = true;
            }

            // Add description if available
            if (row.description) {
              // Remove HTML tags from description
              field.description = row.description.replace(/<[^>]*>/g, '').trim();
            }

            tablesMap[tableName].fields.push(field);
          }
        }
      });

      // Convert to YAML structure
      const collections = Object.values(tablesMap).filter(table => table.fields.length > 0);
      
      if (collections.length === 0) {
        throw new Error("No tables with columns found in CSV. Please ensure the CSV contains table and column entries.");
      }

      const firstTable = collections[0];
      
      const yamlStructure = {
        dataset: {
          name: firstTable.name.replace(/\s+/g, '_').toLowerCase() + '_dataset',
          description: firstTable.description || `Dataset with ${collections.length} collection(s)`,
          data_categories: ['user'],
          retention: {
            policy: 'no_retention_policy_defined'
          },
          collections: collections.map(table => ({
            name: table.name,
            description: table.description,
            primary_key: table.fields.find(f => f.is_primary_key)?.name || table.fields[0]?.name || 'id',
            fields: table.fields
          }))
        }
      };

      // Convert to YAML string
      const yamlString = convertObjectToYaml(yamlStructure);
      return yamlString;
    } catch (error) {
      throw new Error(`CSV parsing error: ${error.message}`);
    }
  };

  // Helper function to convert object to YAML string
  const convertObjectToYaml = (obj, indent = 0) => {
    let yaml = '';
    const indentStr = '  '.repeat(indent);

    for (const [key, value] of Object.entries(obj)) {
      if (Array.isArray(value)) {
        yaml += `${indentStr}${key}:\n`;
        value.forEach((item, idx) => {
          if (typeof item === 'object') {
            yaml += `${indentStr}  - ${Object.keys(item)[0]}: ${Object.values(item)[0]}\n`;
            const remaining = { ...item };
            delete remaining[Object.keys(item)[0]];
            if (Object.keys(remaining).length > 0) {
              yaml += convertObjectToYaml(remaining, indent + 2).replace(/^/, indentStr + '    ');
            }
          } else {
            yaml += `${indentStr}  - ${item}\n`;
          }
        });
      } else if (typeof value === 'object' && value !== null) {
        yaml += `${indentStr}${key}:\n`;
        yaml += convertObjectToYaml(value, indent + 1);
      } else {
        yaml += `${indentStr}${key}: ${value}\n`;
      }
    }

    return yaml;
  };

  // Better YAML conversion function
  const convertObjectToYamlBetter = (obj, indent = 0) => {
    let yaml = '';
    const indentStr = '  '.repeat(indent);

    const processValue = (key, value, currentIndent) => {
      if (Array.isArray(value)) {
        yaml += `${currentIndent}${key}:\n`;
        value.forEach(item => {
          if (typeof item === 'object' && item !== null) {
            yaml += `${currentIndent}  - name: ${item.name || ''}\n`;
            if (item.data_categories && item.data_categories.length > 0) {
              yaml += `${currentIndent}    data_categories: [${item.data_categories.join(', ')}]\n`;
            }
            if (item.is_primary_key) {
              yaml += `${currentIndent}    is_primary_key: true\n`;
            }
            if (item.description) {
              yaml += `${currentIndent}    description: ${item.description}\n`;
            }
          } else {
            yaml += `${currentIndent}  - ${item}\n`;
          }
        });
      } else if (typeof value === 'object' && value !== null) {
        yaml += `${currentIndent}${key}:\n`;
        for (const [k, v] of Object.entries(value)) {
          processValue(k, v, currentIndent + '  ');
        }
      } else {
        yaml += `${currentIndent}${key}: ${value}\n`;
      }
    };

    for (const [key, value] of Object.entries(obj)) {
      processValue(key, value, indentStr);
    }

    return yaml;
  };

  // Handle CSV file upload
  const handleCsvUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    if (!file.name.endsWith('.csv')) {
      setCsvValidationError("Please upload a CSV file");
      return;
    }

    setUploadedCsvFile(file);
    setCsvValidationError(null);
    setLoading(true);

    try {
      const text = await file.text();
      
      // Parse CSV to get structure
      const lines = text.split(/\r?\n/).filter(line => line.trim());
      if (lines.length < 2) {
        throw new Error("CSV file must contain at least a header row and one data row");
      }

      const headers = parseCSVLine(lines[0]).map(h => h.trim().replace(/^"|"$/g, ''));
      
      // Check if it's Open Metadata format or old format
      const isOpenMetadataFormat = headers.some(h => 
        h.toLowerCase().replace(/\s+/g, '').replace(/\*/g, '') === 'entitytype' ||
        h.toLowerCase().replace(/\s+/g, '').replace(/\*/g, '') === 'fullyqualifiedname'
      );

      let yamlOutput = '';

      if (isOpenMetadataFormat) {
        // Open Metadata format
        const getColumnIndex = (colName) => {
          const normalized = colName.toLowerCase().replace(/\s+/g, '').replace(/\*/g, '');
          const index = headers.findIndex(h => {
            const hNormalized = h.toLowerCase().replace(/\s+/g, '').replace(/\*/g, '');
            return hNormalized === normalized;
          });
          if (index === -1) {
            // Try alternative names
            const alternatives = {
              'name*': ['name'],
              'entitytype*': ['entitytype', 'entity type'],
              'fullyqualifiedname': ['fullyqualifiedname', 'fully qualified name', 'fqn']
            };
            const altKey = normalized;
            if (alternatives[altKey]) {
              for (const alt of alternatives[altKey]) {
                const altIndex = headers.findIndex(h => h.toLowerCase().replace(/\s+/g, '').replace(/\*/g, '') === alt);
                if (altIndex !== -1) return altIndex;
              }
            }
          }
          return index;
        };

        const nameIdx = getColumnIndex('name*');
        const entityTypeIdx = getColumnIndex('entityType*');
        const fullyQualifiedNameIdx = getColumnIndex('fullyQualifiedName');
        const descriptionIdx = getColumnIndex('description');
        const tagsIdx = getColumnIndex('tags');

        if (nameIdx === -1 || entityTypeIdx === -1 || fullyQualifiedNameIdx === -1) {
          console.error("Headers found:", headers);
          console.error("nameIdx:", nameIdx, "entityTypeIdx:", entityTypeIdx, "fullyQualifiedNameIdx:", fullyQualifiedNameIdx);
          throw new Error(`Required columns not found. Looking for: name*, entityType*, fullyQualifiedName. Found headers: ${headers.join(', ')}`);
        }

        // Parse rows
        const rows = [];
        const skippedRows = [];
        for (let i = 1; i < lines.length; i++) {
          if (!lines[i].trim()) continue;
          const values = parseCSVLine(lines[i]).map(v => v.trim().replace(/^"|"$/g, ''));
          
          // Handle rows with trailing empty columns (common in CSV exports)
          // Pad with empty strings if row has fewer columns than headers
          while (values.length < headers.length) {
            values.push('');
          }
          
          // Truncate if row has more columns (shouldn't happen, but handle it)
          if (values.length > headers.length) {
            values = values.slice(0, headers.length);
          }
          
          const row = {
            name: values[nameIdx] || '',
            entityType: (values[entityTypeIdx] || '').trim(),
            fullyQualifiedName: (values[fullyQualifiedNameIdx] || '').trim(),
            description: descriptionIdx >= 0 ? (values[descriptionIdx] || '').trim() : '',
            tags: tagsIdx >= 0 ? (values[tagsIdx] || '').trim() : '',
          };
          
          // Only add rows that have entityType and fullyQualifiedName
          if (row.entityType && row.fullyQualifiedName) {
            rows.push(row);
          } else {
            skippedRows.push({ rowNum: i + 1, row, reason: !row.entityType ? 'missing entityType' : 'missing fullyQualifiedName' });
          }
        }
        
        if (skippedRows.length > 0) {
          console.warn("Skipped rows:", skippedRows.slice(0, 5)); // Show first 5 skipped rows
        }
        
        console.log(`Parsed ${rows.length} valid rows from ${lines.length - 1} total rows`);
        console.log("Headers count:", headers.length);
        console.log("Sample parsed row:", rows.find(r => r.entityType?.toLowerCase() === 'column') || 'No column rows found');

        // Group by table
        const tablesMap = {};
        
        // Debug: log rows to see what we're getting
        console.log("Total rows parsed:", rows.length);
        console.log("Sample rows:", rows.slice(0, 5));
        
        // Count entity types
        const entityTypeCounts = {};
        rows.forEach(row => {
          const et = (row.entityType || '').toLowerCase().trim();
          entityTypeCounts[et] = (entityTypeCounts[et] || 0) + 1;
        });
        console.log("Entity type counts:", entityTypeCounts);
        
        // First pass: collect tables
        rows.forEach(row => {
          const entityType = (row.entityType || '').toLowerCase().trim();
          if (entityType === 'table') {
            const parts = (row.fullyQualifiedName || '').split('.');
            if (parts.length >= 3) {
              const tableName = parts[parts.length - 1];
              tablesMap[tableName] = {
                name: tableName,
                description: (row.description || '').replace(/<[^>]*>/g, '').trim() || `${tableName} table`,
                fields: []
              };
              console.log("Found table:", tableName, "from fullyQualifiedName:", row.fullyQualifiedName);
            } else {
              console.warn("Table row with invalid fullyQualifiedName:", row.fullyQualifiedName, "parts:", parts.length);
            }
          }
        });

        console.log("Tables found in first pass:", Object.keys(tablesMap));

        // Second pass: collect columns
        let columnsProcessed = 0;
        rows.forEach(row => {
          const entityType = (row.entityType || '').toLowerCase().trim();
          if (entityType === 'column') {
            const parts = (row.fullyQualifiedName || '').split('.');
            console.log("Processing column row:", {
              fullyQualifiedName: row.fullyQualifiedName,
              parts: parts,
              partsLength: parts.length
            });
            
            if (parts.length >= 4) {
              const tableName = parts[parts.length - 2];
              const columnName = parts[parts.length - 1];
              
              console.log("Extracted tableName:", tableName, "columnName:", columnName);
              console.log("Tables in map:", Object.keys(tablesMap));
              console.log("Table exists?", tableName in tablesMap);
              
              if (!tablesMap[tableName]) {
                tablesMap[tableName] = {
                  name: tableName,
                  description: `${tableName} table`,
                  fields: []
                };
                console.log("Created table from column:", tableName);
              }

              const dataCategory = extractDataCategoryFromTags(row.tags, columnName);
              
              const field = {
                name: columnName,
                data_categories: [dataCategory]
              };

              // Check primary key
              const normalizedTableName = tableName.toLowerCase().replace(/\s+/g, '_');
              const normalizedColumnName = columnName.toLowerCase();
              if (normalizedColumnName === `${normalizedTableName}_id` || 
                  normalizedColumnName === 'id' ||
                  (normalizedColumnName.includes('_id') && normalizedColumnName.endsWith('_id'))) {
                field.is_primary_key = true;
              }

              if (row.description) {
                field.description = row.description.replace(/<[^>]*>/g, '').trim();
              }

              tablesMap[tableName].fields.push(field);
              columnsProcessed++;
              console.log("Added column:", columnName, "to table:", tableName, "Total fields:", tablesMap[tableName].fields.length);
            } else {
              console.warn("Column row with invalid fullyQualifiedName:", row.fullyQualifiedName, "parts:", parts.length, "parts:", parts);
            }
          }
        });
        
        console.log("Total columns processed:", columnsProcessed);

        console.log("Tables map:", Object.keys(tablesMap));
        console.log("Tables with fields:", Object.values(tablesMap).map(t => ({ name: t.name, fieldCount: t.fields.length })));

        const collections = Object.values(tablesMap).filter(table => table.fields.length > 0);
        
        if (collections.length === 0) {
          const tablesWithoutFields = Object.values(tablesMap).filter(table => table.fields.length === 0);
          console.error("No collections with fields found. Tables found:", Object.keys(tablesMap));
          console.error("Tables without fields:", tablesWithoutFields.map(t => t.name));
          throw new Error(`No tables with columns found in CSV. Found ${Object.keys(tablesMap).length} table(s) but none have columns. Please check the CSV format.`);
        }

        const firstTable = collections[0];
        
        // Generate YAML
        yamlOutput = 'dataset:\n';
        yamlOutput += `  name: ${firstTable.name.replace(/\s+/g, '_').toLowerCase()}_dataset\n`;
        yamlOutput += `  description: ${firstTable.description || `Dataset with ${collections.length} collection(s)`}\n`;
        yamlOutput += '  data_categories:\n';
        yamlOutput += '    - user\n';
        yamlOutput += '  retention:\n';
        yamlOutput += '    policy: no_retention_policy_defined\n';
        yamlOutput += '  collections:\n';
        
        collections.forEach(collection => {
          yamlOutput += `    - name: ${collection.name}\n`;
          yamlOutput += `      description: ${collection.description}\n`;
          yamlOutput += `      primary_key: ${collection.fields.find(f => f.is_primary_key)?.name || collection.fields[0]?.name || 'id'}\n`;
          yamlOutput += '      fields:\n';
          collection.fields.forEach(field => {
            yamlOutput += `        - name: ${field.name}\n`;
            yamlOutput += `          data_categories: [${field.data_categories.join(', ')}]\n`;
            if (field.is_primary_key) {
              yamlOutput += '          is_primary_key: true\n';
            }
            if (field.description) {
              yamlOutput += `          description: ${field.description}\n`;
            }
          });
        });
      } else {
        // Old format (table_name, column_name) - keep for backward compatibility
        throw new Error("Please use Open Metadata CSV format with columns: name*, entityType*, fullyQualifiedName");
      }

      // Validate generated YAML
      if (!yamlOutput.includes('dataset:') || !yamlOutput.includes('collections:')) {
        throw new Error("Generated YAML is invalid. Please check your CSV format.");
      }

      setYamlContent(yamlOutput);
      setCsvValidationError(null);
      toast.success("CSV converted to YAML successfully");
    } catch (error) {
      console.error("Error converting CSV to YAML:", error);
      setCsvValidationError(error.message || "Failed to convert CSV to YAML. Please check the CSV format.");
      toast.error(error.message || "Failed to convert CSV to YAML");
      setYamlContent(""); // Clear YAML on error
    } finally {
      setLoading(false);
    }
  };

  const handleCreateDataset = () => {
    setDatasetType("yaml");
    setYamlContent(`dataset:
  name: user_pii_dataset
  description: User PII dataset with core profile, addresses, and identity documents
  data_categories:
    - user
  retention:
    policy: no_retention_policy_defined
  collections:
    - name: users
      description: Core user profile table
      primary_key: user_id
      fields:
        - name: user_id
          data_categories: [user.unique_id]
          is_primary_key: true
        - name: full_name
          data_categories: [user.name]
        - name: email
          data_categories: [user.contact.email]
        - name: phone_number
          data_categories: [user.contact.phone_number]
        - name: date_of_birth
          data_categories: [user.demographic.date_of_birth]
        - name: created_at
          data_categories: [system.operations]`);
    setIsEditMode(false);
    setEditingDatasetId(null);
    navigate("/consent-datasets/create");
  };

  const handleEditDataset = async (dataset) => {
    console.log("handleEditDataset called with dataset:", dataset);
    const datasetId = dataset.id || dataset.datasetId;
    console.log("Extracted datasetId:", datasetId);
    if (datasetId) {
      const editPath = `/consent-datasets/edit/${datasetId}`;
      console.log("Navigating to:", editPath);
      navigate(editPath);
    } else {
      console.error("Dataset ID not found in dataset object");
      toast.error("Dataset ID not found");
    }
  };

  const handleDeleteDataset = async (dataset) => {
    const datasetId = dataset.id || dataset.datasetId;
    if (!datasetId) {
      toast.error("Dataset ID not found");
      return;
    }

    if (!window.confirm("Are you sure you want to delete this dataset?")) {
      return;
    }

    setLoading(true);
    try {
      const headers = {
        "X-Tenant-ID": tenant_id || "",
        "X-Business-ID": businessId || "",
      };

      const response = await makeAPICall(
        config.registry_datasets_by_id(datasetId),
        "DELETE",
        null,
        headers
      );

      if (response && (response.status === 200 || response.status === 204)) {
        toast.success("Dataset deleted successfully");
        fetchDatasets();
      } else {
        toast.error("Failed to delete dataset");
      }
    } catch (error) {
      console.error("Error deleting dataset:", error);
      const errorMessage = error?.errorList?.[0]?.errorMessage || error?.message || "Failed to delete dataset";
      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleDatasetClick = async (dataset) => {
    // Fetch full dataset data including YAML if not already available
    if (!dataset.datasetYaml && (dataset.id || dataset.datasetId)) {
      const datasetId = dataset.id || dataset.datasetId;
      const fullDataset = await fetchDatasetById(datasetId);
      if (fullDataset) {
        setSelectedDataset({ ...dataset, ...fullDataset });
        navigate(`/consent-datasets/${dataset.key || datasetId}`);
        return;
      }
    }
    setSelectedDataset(dataset);
    navigate(`/consent-datasets/${dataset.key || dataset.id || dataset.datasetId}`);
  };

  const handleBackToList = () => {
    setShowCreateForm(false);
    setShowDatasetDetail(false);
    setSelectedDataset(null);
    setIsEditMode(false);
    setEditingDatasetId(null);
    setYamlContent(`dataset:
  name: user_pii_dataset
  description: User PII dataset with core profile, addresses, and identity documents
  data_categories:
    - user
  retention:
    policy: no_retention_policy_defined
  collections:
    - name: users
      description: Core user profile table
      primary_key: user_id
      fields:
        - name: user_id
          data_categories: [user.unique_id]
          is_primary_key: true
        - name: full_name
          data_categories: [user.name]
        - name: email
          data_categories: [user.contact.email]
        - name: phone_number
          data_categories: [user.contact.phone_number]
        - name: date_of_birth
          data_categories: [user.demographic.date_of_birth]
        - name: created_at
          data_categories: [system.operations]`);
    navigate("/consent-datasets");
  };

  const handleSaveDataset = async () => {
    if ((datasetType === "yaml" || datasetType === "openmetadata") && !yamlContent.trim()) {
      toast.error("Please provide YAML content");
      return;
    }

    if (datasetType === "database" && !databaseUrl.trim()) {
      toast.error("Please provide a database URL");
      return;
    }

    // Handle YAML and OpenMetadata types (both use YAML content)
    if (datasetType !== "yaml" && datasetType !== "openmetadata") {
      if (datasetType === "database") {
        toast.error("Database type dataset creation not yet implemented");
      }
      return;
    }

    setLoading(true);
    try {
      const headers = {
        "Content-Type": "application/json",
        "X-Tenant-ID": tenant_id || "",
        "X-Business-ID": businessId || "",
      };

      const body = {
        datasetYaml: yamlContent.trim(),
      };

      // Handle edit mode (PUT) vs create mode (POST)
      if (isEditMode && editingDatasetId) {
        const response = await makeAPICall(
          config.registry_datasets_by_id(editingDatasetId),
          "PUT",
          body,
          headers
        );

        if (response && (response.status === 200 || response.status === 201)) {
          toast.success("Dataset updated successfully");
          handleBackToList();
          fetchDatasets();
        } else {
          toast.error("Failed to update dataset");
        }
      } else {
        // Create mode
        headers["X-Transaction-ID"] = uuidv4();
        const response = await makeAPICall(
          config.registry_datasets,
          "POST",
          body,
          headers
        );

        if (response && (response.status === 200 || response.status === 201)) {
          toast.success("Dataset created successfully");
          handleBackToList();
          fetchDatasets();
        } else {
          toast.error("Failed to create dataset");
        }
      }
    } catch (error) {
      console.error(`Error ${isEditMode ? "updating" : "creating"} dataset:`, error);
      const errorMessage = error?.errorList?.[0]?.errorMessage || error?.message || `Failed to ${isEditMode ? "update" : "create"} dataset`;
      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateDataset = async () => {
    if (!databaseUrl.trim()) {
      toast.error("Please provide a database URL");
      return;
    }

    setLoading(true);
    try {
      // TODO: Implement API call to generate dataset from database
      toast.success("Dataset generated successfully");
      handleBackToList();
      fetchDatasets();
    } catch (error) {
      toast.error("Failed to generate dataset");
    } finally {
      setLoading(false);
    }
  };

  // Update field in YAML content
  const updateFieldInYaml = (yamlContent, fieldName, collectionName, updates) => {
    const lines = yamlContent.split('\n');
    let inCollections = false;
    let inCollection = false;
    let inFields = false;
    let currentCollection = "";
    let fieldsIndentLevel = 0;
    let fieldStartIndex = -1;
    let fieldEndIndex = -1;
    let fieldIndent = 0;
    const fieldLines = [];
    let foundField = false;
    
    // Find the field in YAML and extract existing properties
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      const trimmed = line.trim();
      const leadingSpaces = line.length - line.trimStart().length;
      
      if (trimmed.startsWith('collections:')) {
        inCollections = true;
        continue;
      }
      
      if (inCollections && !inFields && trimmed.startsWith('- name:')) {
        const collName = trimmed.replace('- name:', '').trim();
        if (collName === collectionName) {
          inCollection = true;
          continue;
        } else if (inCollection) {
          // We've moved to a different collection, reset
          inCollection = false;
          inFields = false;
        }
      }
      
      if (inCollection && trimmed.startsWith('fields:')) {
        inFields = true;
        fieldsIndentLevel = leadingSpaces;
        continue;
      }
      
      if (inFields && trimmed.startsWith('- name:')) {
        const fName = trimmed.replace('- name:', '').trim();
        if (fName === fieldName && !foundField) {
          // Found our target field
          foundField = true;
          fieldStartIndex = i;
          fieldIndent = leadingSpaces;
          fieldLines.push(line); // Keep the name line
          continue;
        } else if (foundField) {
          // We've found the next field, so this is the end
          fieldEndIndex = i;
          break;
        }
      }
      
      // If we're in the target field, collect existing properties
      if (foundField && fieldStartIndex !== -1) {
        // Check if we've moved to a different field or collection
        if (trimmed.startsWith('- name:') && leadingSpaces <= fieldIndent) {
          fieldEndIndex = i;
          break;
        }
        // Check if we've moved out of fields section
        if (leadingSpaces <= fieldsIndentLevel && !trimmed.startsWith(' ') && trimmed !== '' && !trimmed.startsWith('-')) {
          fieldEndIndex = i;
          break;
        }
        
        // Collect existing field properties (except ones we're updating)
        if (leadingSpaces > fieldIndent) {
          if (!trimmed.startsWith('description:') && 
              !trimmed.startsWith('data_categories:') && 
              !trimmed.startsWith('consent_templates:')) {
            fieldLines.push(line); // Preserve other properties
          }
        }
      }
    }
    
    // If field end not found, set to end of file
    if (fieldEndIndex === -1) {
      fieldEndIndex = lines.length;
    }
    
    // Build updated field section
    if (foundField && fieldStartIndex !== -1 && fieldEndIndex > fieldStartIndex) {
      const beforeField = lines.slice(0, fieldStartIndex);
      const afterField = lines.slice(fieldEndIndex);
      
      // Build the updated field lines (insert updates after name)
      const updatedFieldLines = [];
      updatedFieldLines.push(fieldLines[0]); // Keep the name line
      
      // Add description if provided
      if (updates.description) {
        updatedFieldLines.push(`${' '.repeat(fieldIndent)}  description: ${updates.description}`);
      }
      
      // Add data_categories
      if (updates.dataCategories && updates.dataCategories.length > 0) {
        updatedFieldLines.push(`${' '.repeat(fieldIndent)}  data_categories: [${updates.dataCategories.join(', ')}]`);
      }
      
      // Add consent_templates if provided (as a new property)
      if (updates.consentTemplates && updates.consentTemplates.length > 0) {
        updatedFieldLines.push(`${' '.repeat(fieldIndent)}  consent_templates: [${updates.consentTemplates.join(', ')}]`);
      }
      
      // Add preserved properties (skip the name line which is already added)
      updatedFieldLines.push(...fieldLines.slice(1));
      
      // Combine all parts
      return [...beforeField, ...updatedFieldLines, ...afterField].join('\n');
    }
    
    // If field not found, return original YAML
    console.warn("Field not found in YAML:", fieldName, "in collection:", collectionName);
    return yamlContent;
  };

  // Fetch consent templates from API
  const fetchConsentTemplates = async () => {
    try {
      const templatesData = await dispatch(getAllTemplates(tenant_id, businessId));
      const templatesList = templatesData?.searchList || templatesData?.data || [];
      
      // Map templates to options format
      const options = templatesList.map(template => ({
        value: template.templateId || template.id,
        label: template.templateName || template.name || "Unnamed Template"
      }));
      
      setConsentTemplateOptions(options);
      console.log("Fetched consent templates:", options.length, "templates");
      console.log("Template options:", options);
    } catch (error) {
      console.error("Error fetching consent templates:", error);
      toast.error("Failed to fetch consent templates");
    }
  };

  // Empty state illustration - using dashboard.svg as reference
  const emptyIllustration = new URL("../assets/dashboard.svg", import.meta.url).href;

  // Parse dataset fields and prepare collections/filtered fields (always compute, even if not showing detail)
  const datasetFields = useMemo(() => {
    if (!showDatasetDetail || !selectedDataset) {
      return [];
    }
    let fields = getDatasetFields(selectedDataset);
    
    // Apply field updates from state
    fields = fields.map(field => {
      const update = fieldUpdates[field.id];
      if (update) {
        return {
          ...field,
          ...update
        };
      }
      return field;
    });
    
    return fields;
  }, [showDatasetDetail, selectedDataset, fieldUpdates]);
  
  // Extract unique collection names from fields
  const collections = useMemo(() => {
    if (!datasetFields || datasetFields.length === 0) {
      return [];
    }
    const uniqueCollections = [...new Set(datasetFields.map(field => field.collectionName).filter(Boolean))];
    return uniqueCollections;
  }, [datasetFields]);
  
  // Filter fields based on selected collection
  const filteredFields = useMemo(() => {
    if (!datasetFields || datasetFields.length === 0) {
      return [];
    }
    if (selectedCollection === "all") {
      return datasetFields;
    }
    return datasetFields.filter(field => field.collectionName === selectedCollection);
  }, [datasetFields, selectedCollection]);

  // If showing dataset detail, render the detail page
  if (showDatasetDetail && selectedDataset) {
    console.log("Rendering dataset detail for:", selectedDataset);
    console.log("Dataset has YAML:", !!selectedDataset.datasetYaml);
    console.log("Extracted fields:", datasetFields);

    const handleFieldSort = (column) => {
      if (fieldSortColumn === column) {
        setFieldSortDirection(fieldSortDirection === "asc" ? "desc" : "asc");
      } else {
        setFieldSortColumn(column);
        setFieldSortDirection("asc");
      }
    };

    return (
      <div style={{ 
        width: "100%",
        padding: "24px 24px 24px 0",
        maxWidth: "1200px", 
        margin: "0 auto",
        paddingTop: "100px",
        minHeight: "calc(100vh - 200px)",
        boxSizing: "border-box"
      }}>
        {/* Header with Back Button */}
        <div style={{ marginBottom: "32px" }}>
          <div style={{ display: "flex", alignItems: "flex-start", gap: "16px", justifyContent: "space-between" }}>
            <div style={{ display: "flex", alignItems: "flex-start", gap: "16px", flex: 1 }}>
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
                  {selectedDataset.name}
                </Text>
              </div>
            </div>
            <div style={{ flexShrink: 0 }}>
              <select
                value={selectedCollection}
                onChange={(e) => setSelectedCollection(e.target.value)}
                style={{
                  padding: "8px 32px 8px 12px",
                  border: "1px solid #E0E0E0",
                  borderRadius: "8px",
                  fontSize: "14px",
                  backgroundColor: "#fff",
                  cursor: "pointer",
                  outline: "none",
                }}
              >
                <option value="all">All Collections</option>
                {collections.map(collection => (
                  <option key={collection} value={collection}>{collection}</option>
                ))}
              </select>
            </div>
          </div>
        </div>

        {/* Fields Table */}
        <div style={{ backgroundColor: "#fff", borderRadius: "8px", overflow: "hidden", border: "1px solid #E0E0E0" }}>
          <table
            style={{
              width: "100%",
              borderCollapse: "collapse",
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
                  onClick={() => handleFieldSort("fieldName")}
                >
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Field name
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
                  onClick={() => handleFieldSort("description")}
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
                  onClick={() => handleFieldSort("category")}
                >
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Personal data categories
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
                  onClick={() => handleFieldSort("tags")}
                >
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Tags
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
              {filteredFields.map((field) => (
                <tr
                  key={field.id}
                  style={{
                    borderBottom: "1px solid #E0E0E0",
                    transition: "background-color 0.2s",
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = "#F9F9F9")}
                  onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = "transparent")}
                >
                  <td style={{ padding: "12px 16px" }}>
                    <Text appearance="body-xs" color="primary-grey-100" style={{ fontFamily: "monospace" }}>
                      {field.fieldName}
                    </Text>
                  </td>
                  <td style={{ padding: "12px 16px" }}>
                    <Text appearance="body-xs" color="primary-grey-100">
                      {field.description}
                    </Text>
                  </td>
                  <td style={{ padding: "12px 16px" }}>
                    <div style={{ display: "flex", flexWrap: "wrap", gap: "8px" }}>
                      {(() => {
                        // Get all categories from fieldUpdates or field.tags or fallback to field.category
                        const fieldUpdateForDisplay = fieldUpdates[field.id];
                        const categories = fieldUpdateForDisplay?.dataCategories || 
                                         (field.tags && field.tags.length > 0 ? field.tags : [field.category].filter(Boolean));
                        
                        if (categories && categories.length > 0) {
                          return categories.map((cat, idx) => (
                            <span
                              key={idx}
                              style={{
                                display: "inline-block",
                                padding: "4px 12px",
                                borderRadius: "12px",
                                backgroundColor: "#E6F8EC",
                                color: "#16794C",
                                fontSize: "12px",
                                fontWeight: "500",
                              }}
                            >
                              {cat}
                            </span>
                          ));
                        }
                        return (
                          <Text appearance="body-xs" color="primary-grey-60">
                            -
                          </Text>
                        );
                      })()}
                    </div>
                  </td>
                  <td style={{ padding: "12px 16px" }}>
                    <div style={{ display: "flex", flexWrap: "wrap", gap: "8px" }}>
                      {(field.consentTemplates && field.consentTemplates.length > 0) || (field.consentTemplateIds && field.consentTemplateIds.length > 0) ? (
                        (field.consentTemplateIds || field.consentTemplates || []).map((templateId, idx) => {
                          // templateId is a string (template ID from YAML)
                          // Try to find the label from consentTemplateOptions
                          const templateOption = consentTemplateOptions.find(opt => opt.value === templateId);
                          const displayLabel = templateOption ? templateOption.label : templateId;
                          
                          return (
                          <span
                            key={idx}
                            style={{
                              display: "inline-block",
                              padding: "4px 12px",
                              borderRadius: "12px",
                              backgroundColor: "#F0F4FF",
                              color: "#0F3CC9",
                              fontSize: "12px",
                              fontWeight: "500",
                            }}
                          >
                              {displayLabel}
                          </span>
                          );
                        })
                      ) : (
                        <Text appearance="body-xs" color="primary-grey-60">
                          -
                        </Text>
                      )}
                    </div>
                  </td>
                  <td style={{ padding: "12px 16px" }}>
                    <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
                      <button
                        onClick={async (e) => {
                          e.stopPropagation();
                          setEditingField(field);
                          setFieldDescription(field.description || "");
                          
                          // Get all categories from fieldUpdates or field.tags or fallback to field.category
                          const fieldUpdate = fieldUpdates[field.id];
                          const categories = fieldUpdate?.dataCategories || 
                                           (field.tags && field.tags.length > 0 ? field.tags : 
                                            (field.category ? [field.category] : []));
                          
                          // Map all categories to dataCategoryOptions
                          const categoryOptions = [];
                          categories.forEach(cat => {
                            if (cat) {
                              // Try to find exact match first
                              let categoryOption = dataCategoryOptions.find(
                                opt => opt.value === cat || opt.label === cat
                              );
                              
                              // If not found, try case-insensitive match
                              if (!categoryOption) {
                                categoryOption = dataCategoryOptions.find(
                                  opt => opt.value.toLowerCase() === cat?.toLowerCase() || 
                                         opt.label.toLowerCase() === cat?.toLowerCase()
                                );
                              }
                              
                              // Only add if we found a match in the options
                              if (categoryOption && !categoryOptions.find(opt => opt.value === categoryOption.value)) {
                                categoryOptions.push(categoryOption);
                              }
                            }
                          });
                          
                          // Set selected categories (only matched ones, otherwise keep blank for user to select)
                          setSelectedDataCategories(categoryOptions);
                          
                          // Load existing consent templates from field.consentTemplates or fieldUpdates
                          // Use consentTemplateIds if available (from YAML), otherwise use consentTemplates
                          const templateIds = fieldUpdate?.consentTemplates?.map(t => t.value || t) || 
                                             field.consentTemplateIds || 
                                             field.consentTemplates || [];
                          
                          console.log("Loading templates for field:", field.fieldName, "templateIds:", templateIds);
                          
                          // Fetch templates first if not loaded
                          if (consentTemplateOptions.length === 0) {
                            console.log("Fetching consent templates...");
                            await fetchConsentTemplates();
                          }
                          
                          console.log("Available template options:", consentTemplateOptions.length, "options");
                          
                          // Map template IDs to template options with labels
                          const mappedTemplates = templateIds.map(templateId => {
                            // Try to find it in the options by value (template ID) - compare as strings
                            const found = consentTemplateOptions.find(opt => {
                              const match = String(opt.value) === String(templateId);
                              if (match) {
                                console.log("Found template:", templateId, "->", opt.label);
                              }
                              return match;
                            });
                            if (found) {
                              return found; // Return the full option object with label
                            }
                            // If not found, create a placeholder (template might not exist anymore)
                            console.warn("Template not found in options:", templateId, "Available:", consentTemplateOptions.map(o => ({ value: o.value, label: o.label })));
                            return { value: templateId, label: templateId };
                          });
                          
                          console.log("Mapped templates:", mappedTemplates.map(t => ({ value: t.value, label: t.label })));
                          setSelectedConsentTemplates(mappedTemplates);
                          
                          setShowEditFieldModal(true);
                        }}
                        aria-label="Edit field"
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
                        onClick={(e) => {
                          e.stopPropagation();
                          setFieldToDelete(field);
                          setShowDeleteModal(true);
                        }}
                        aria-label="Delete field"
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
              ))}
            </tbody>
          </table>
        </div>

        {/* Delete Field Modal */}
        {showDeleteModal && fieldToDelete && (
          <div
            style={{
              position: "fixed",
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              backgroundColor: "rgba(0, 0, 0, 0.5)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              zIndex: 1001,
            }}
            onClick={() => setShowDeleteModal(false)}
          >
            <div
              style={{
                backgroundColor: "#fff",
                borderRadius: "16px",
                padding: "32px",
                width: "90%",
                maxWidth: "500px",
                boxShadow: "0 20px 60px rgba(0, 0, 0, 0.3)",
                position: "relative",
              }}
              onClick={(e) => e.stopPropagation()}
            >
              {/* Close Button */}
              <button
                onClick={() => setShowDeleteModal(false)}
                style={{
                  position: "absolute",
                  top: "20px",
                  right: "20px",
                  background: "none",
                  border: "none",
                  fontSize: "24px",
                  cursor: "pointer",
                  color: "#666",
                  width: "32px",
                  height: "32px",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  borderRadius: "50%",
                  transition: "background-color 0.2s",
                }}
                onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = "#F5F5F5")}
                onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = "transparent")}
              >
                <IcClose size={20} />
              </button>

              {/* Modal Title */}
              <Text
                appearance="heading-s"
                color="primary-grey-100"
                style={{
                  fontSize: "20px",
                  fontWeight: "600",
                  marginBottom: "16px",
                  display: "block",
                }}
              >
                Delete field?
              </Text>

              {/* Modal Message */}
              <Text
                appearance="body-s"
                color="primary-grey-80"
                style={{
                  fontSize: "14px",
                  lineHeight: "20px",
                  marginBottom: "32px",
                  display: "block",
                }}
              >
                You are about to permanently delete the field named <strong>{fieldToDelete.fieldName}</strong> from this dataset. Are you sure you want to delete.
              </Text>

              {/* Delete Button */}
              <div style={{ display: "flex", justifyContent: "flex-end" }}>
                <Button
                  label="Delete"
                  onClick={() => {
                    // TODO: Implement delete field functionality
                    toast.success("Field deleted successfully");
                    setShowDeleteModal(false);
                    setFieldToDelete(null);
                  }}
                  variant="primary"
                  size="medium"
                  style={{
                    fontSize: "14px",
                    fontWeight: "600",
                    padding: "10px 24px",
                    minWidth: "100px",
                    backgroundColor: "#DC2626",
                    borderColor: "#DC2626",
                    color: "#fff",
                  }}
                />
              </div>
            </div>
          </div>
        )}

        {/* Edit Field Modal */}
        {showEditFieldModal && editingField && (
          <div
            style={{
              position: "fixed",
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              backgroundColor: "rgba(0, 0, 0, 0.5)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              zIndex: 1000,
            }}
            onClick={() => setShowEditFieldModal(false)}
          >
            <div
              style={{
                backgroundColor: "#fff",
                borderRadius: "16px",
                padding: "32px",
                width: "90%",
                maxWidth: "600px",
                maxHeight: "90vh",
                overflowY: "auto",
                boxShadow: "0 20px 60px rgba(0, 0, 0, 0.3)",
                position: "relative",
              }}
              onClick={(e) => e.stopPropagation()}
            >
              {/* Close Button */}
              <button
                onClick={() => setShowEditFieldModal(false)}
                style={{
                  position: "absolute",
                  top: "20px",
                  right: "20px",
                  background: "none",
                  border: "none",
                  fontSize: "24px",
                  cursor: "pointer",
                  color: "#666",
                  width: "32px",
                  height: "32px",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  borderRadius: "50%",
                  transition: "background-color 0.2s",
                }}
                onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = "#F5F5F5")}
                onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = "transparent")}
              >
                <IcClose size={20} />
              </button>

              {/* Modal Title */}
              <Text
                appearance="heading-s"
                color="primary-grey-100"
                style={{
                  fontSize: "20px",
                  fontWeight: "600",
                  marginBottom: "8px",
                  display: "block",
                }}
              >
                {editingField.fieldName}
              </Text>

              {/* Instructions */}
              <Text
                appearance="body-s"
                color="primary-grey-80"
                style={{
                  fontSize: "14px",
                  lineHeight: "20px",
                  marginBottom: "24px",
                  display: "block",
                }}
              >
                Fields are an array of objects that describe the collection's fields. Provide additional context to this field by filling out the fields below.
              </Text>

              {/* Description Field */}
              <div style={{ marginBottom: "24px" }}>
                <Text
                  appearance="body-s-bold"
                  color="primary-grey-100"
                  style={{ marginBottom: "8px", fontSize: "14px", fontWeight: "600", display: "block" }}
                >
                  Description
                </Text>
                <InputFieldV2
                  value={fieldDescription}
                  onChange={(e) => setFieldDescription(e.target.value)}
                  placeholder="Enter field description"
                  multiline
                  rows={4}
                />
              </div>

              {/* Data Categories Field */}
              <div style={{ marginBottom: "24px" }}>
                <Text
                  appearance="body-s-bold"
                  color="primary-grey-100"
                  style={{ marginBottom: "8px", fontSize: "14px", fontWeight: "600", display: "block" }}
                >
                  Data categories
                </Text>
                <Select
                  isMulti
                  options={dataCategoryOptions}
                  value={selectedDataCategories}
                  onChange={setSelectedDataCategories}
                  placeholder="Select data categories"
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

              {/* Consent Template Mapping Field */}
              <div style={{ marginBottom: "32px" }}>
                <Text
                  appearance="body-s-bold"
                  color="primary-grey-100"
                  style={{ marginBottom: "8px", fontSize: "14px", fontWeight: "600", display: "block" }}
                >
                  Consent template mapping
                </Text>
                <Select
                  isMulti
                  options={consentTemplateOptions}
                  value={selectedConsentTemplates}
                  onChange={setSelectedConsentTemplates}
                  placeholder="Select consent templates"
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
              <div style={{ display: "flex", justifyContent: "flex-end", gap: "12px", marginTop: "32px" }}>
                <Button
                  label="Cancel"
                  onClick={() => setShowEditFieldModal(false)}
                  variant="secondary"
                  size="medium"
                  style={{
                    fontSize: "14px",
                    fontWeight: "600",
                    padding: "10px 24px",
                    minWidth: "100px",
                  }}
                />
                <Button
                  label="Save"
                  onClick={async () => {
                    // Update the field with new data
                    if (editingField && selectedDataset) {
                      setLoading(true);
                      try {
                        // Update YAML content with field changes
                        const updatedYaml = updateFieldInYaml(
                          selectedDataset.datasetYaml,
                          editingField.fieldName,
                          editingField.collectionName,
                          {
                            description: fieldDescription,
                            dataCategories: selectedDataCategories.map(cat => cat.value),
                            consentTemplates: selectedConsentTemplates.map(t => t.value)
                          }
                        );

                        // Call update dataset API
                        const headers = {
                          "Content-Type": "application/json",
                          "X-Tenant-ID": tenant_id || "",
                          "X-Business-ID": businessId || "",
                        };

                        const body = {
                          datasetYaml: updatedYaml.trim(),
                        };

                        const datasetId = selectedDataset.id || selectedDataset.datasetId;
                        const response = await makeAPICall(
                          config.registry_datasets_by_id(datasetId),
                          "PUT",
                          body,
                          headers
                        );

                        if (response && (response.status === 200 || response.status === 201)) {
                          // Update local state with all selected categories
                          setFieldUpdates(prev => ({
                            ...prev,
                            [editingField.id]: {
                              description: fieldDescription,
                              category: selectedDataCategories[0]?.value || editingField.category,
                              dataCategories: selectedDataCategories.map(cat => cat.value), // Store all categories
                              consentTemplates: selectedConsentTemplates,
                              tags: selectedDataCategories.map(cat => cat.value) // Use dataCategories for tags display
                            }
                          }));

                          // Refresh dataset data
                          const updatedDataset = await fetchDatasetById(datasetId);
                          if (updatedDataset) {
                            setSelectedDataset({ ...selectedDataset, ...updatedDataset });
                          }

                          toast.success("Field updated successfully");
                          setShowEditFieldModal(false);
                        } else {
                          toast.error("Failed to update field");
                        }
                      } catch (error) {
                        console.error("Error updating field:", error);
                        toast.error("Failed to update field");
                      } finally {
                        setLoading(false);
                      }
                    }
                  }}
                  variant="primary"
                  size="medium"
                  style={{
                    fontSize: "14px",
                    fontWeight: "600",
                    padding: "10px 24px",
                    minWidth: "100px",
                  }}
                />
              </div>
            </div>
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

  // Force show form if on create/edit route (this ensures it works even if state is wrong)
  const shouldShowForm = showCreateForm || isCreateRoute || isEditRoute;

  // If showing create form, render the form page
  if (shouldShowForm) {
    return (
      <div style={{ 
        width: "100%",
        padding: "24px 24px 24px 0",
        maxWidth: "1200px", 
        margin: "0 auto",
        paddingTop: "100px",
        minHeight: "calc(100vh - 200px)",
        boxSizing: "border-box"
      }}>
        {/* Header with Back Button */}
        <div style={{ marginBottom: "32px" }}>
          <div style={{ display: "flex", alignItems: "flex-start", gap: "16px" }}>
            <button
              onClick={handleBackToList}
              type="button"
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
                {isEditMode ? "Edit dataset" : "Create datasets"}
              </Text>
              <Text appearance="body-s" color="primary-grey-80" style={{ fontSize: "14px", lineHeight: "20px", display: "block" }}>
                {isEditMode ? "Update the dataset YAML below." : "Create a dataset using YAML or connect to a database."}
              </Text>
            </div>
          </div>
        </div>

        {/* Dataset Type Selection */}
        <div style={{ marginBottom: "24px" }}>
          <div style={{ display: "flex", gap: "32px", marginBottom: "24px" }}>
            <label style={{ display: "flex", alignItems: "center", gap: "8px", cursor: "pointer" }}>
              <input
                type="radio"
                name="datasetType"
                value="yaml"
                checked={datasetType === "yaml"}
                onChange={(e) => setDatasetType(e.target.value)}
                style={{ width: "18px", height: "18px", cursor: "pointer" }}
              />
              <Text appearance="body-s" color="primary-grey-100" style={{ fontSize: "14px" }}>
                Upload a new dataset YAML
              </Text>
            </label>
            <label style={{ display: "flex", alignItems: "center", gap: "8px", cursor: "pointer" }}>
              <input
                type="radio"
                name="datasetType"
                value="database"
                checked={datasetType === "database"}
                onChange={(e) => setDatasetType(e.target.value)}
                style={{ width: "18px", height: "18px", cursor: "pointer" }}
              />
              <Text appearance="body-s" color="primary-grey-100" style={{ fontSize: "14px" }}>
                Connect to a database
              </Text>
            </label>
            <label style={{ display: "flex", alignItems: "center", gap: "8px", cursor: "pointer" }}>
              <input
                type="radio"
                name="datasetType"
                value="openmetadata"
                checked={datasetType === "openmetadata"}
                onChange={(e) => {
                  setDatasetType(e.target.value);
                  setUploadedCsvFile(null);
                  setCsvValidationError(null);
                }}
                style={{ width: "18px", height: "18px", cursor: "pointer" }}
              />
              <Text appearance="body-s" color="primary-grey-100" style={{ fontSize: "14px" }}>
                Create YAML from Open Metadata
              </Text>
            </label>
          </div>
        </div>

        {/* Instructions */}
        {datasetType === "yaml" && (
          <div style={{ marginBottom: "24px" }}>
            <Text appearance="body-s" color="primary-grey-80" style={{ fontSize: "14px", lineHeight: "20px" }}>
              Get started creating your first dataset by pasting your dataset yaml below! You may have received this yaml from a colleague or your Ethyca developer support engineer.
            </Text>
          </div>
        )}


        {/* Database Connection Form */}
        {datasetType === "database" && (
          <div style={{ marginBottom: "32px", maxWidth: "600px" }}>
            <Text appearance="body-s" color="primary-grey-80" style={{ fontSize: "14px", lineHeight: "20px", marginBottom: "24px" }}>
              Connect to a database using the connection URL. You may have received this URL from a colleague or your Ethyca developer support engineer.
            </Text>
            
            {/* Database URL Input */}
            <div style={{ marginBottom: "24px" }}>
              <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "8px", fontSize: "14px", fontWeight: "600" }}>
                Database URL
              </Text>
              <InputFieldV2
                value={databaseUrl}
                onChange={(e) => setDatabaseUrl(e.target.value)}
                placeholder="www.databaseurl.com"
              />
            </div>
          </div>
        )}

        {/* Open Metadata CSV Upload Form */}
        {datasetType === "openmetadata" && (
          <div style={{ marginBottom: "32px", maxWidth: "800px" }}>
            {/* Help Text Area */}
            <div style={{ 
              marginBottom: "24px", 
              padding: "16px", 
              backgroundColor: "#F0F4FF", 
              borderRadius: "8px",
              border: "1px solid #D1D5DB"
            }}>
              <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "8px", fontSize: "14px", fontWeight: "600", display: "block" }}>
                How to download Open Metadata CSV:
              </Text>
              <Text appearance="body-s" color="primary-grey-80" style={{ fontSize: "14px", lineHeight: "20px" }}>
                <strong>Note:</strong> This feature requires Open Metadata version 1.11.x<br/><br/>
                1. Open your Open Metadata instance (version 1.11.x)<br/>
                2. Navigate to the database schema you want to export<br/>
                3. Click on "Export" or "Download Schema" option<br/>
                4. Select CSV format<br/>
                5. The CSV should contain columns: name*, entityType*, fullyQualifiedName, description, tags, and other metadata fields
              </Text>
            </div>

            {/* CSV File Upload */}
            <div style={{ marginBottom: "24px" }}>
              <Text appearance="body-s-bold" color="primary-grey-100" style={{ marginBottom: "8px", fontSize: "14px", fontWeight: "600" }}>
                Upload Open Metadata CSV
              </Text>
              <input
                type="file"
                accept=".csv"
                onChange={handleCsvUpload}
                style={{
                  width: "100%",
                  padding: "10px",
                  border: "1px solid #E0E0E0",
                  borderRadius: "8px",
                  fontSize: "14px",
                  cursor: "pointer",
                }}
              />
              {uploadedCsvFile && (
                <Text appearance="body-s" color="primary-grey-80" style={{ marginTop: "8px", fontSize: "12px" }}>
                  File selected: {uploadedCsvFile.name}
                </Text>
              )}
              {csvValidationError && (
                <Text appearance="body-s" color="feedback-error-50" style={{ marginTop: "8px", fontSize: "12px" }}>
                  {csvValidationError}
                </Text>
              )}
            </div>
          </div>
        )}

        {/* YAML Editor - Show for both yaml and openmetadata (after conversion) */}
        {(datasetType === "yaml" || (datasetType === "openmetadata" && yamlContent)) && (
          <div style={{ marginBottom: "32px" }}>
            {datasetType === "openmetadata" && (
              <Text appearance="body-s" color="primary-grey-80" style={{ fontSize: "14px", lineHeight: "20px", marginBottom: "12px", display: "block" }}>
                Generated YAML from Open Metadata CSV:
              </Text>
            )}
            <textarea
              value={yamlContent || ""}
              onChange={(e) => setYamlContent(e.target.value)}
              placeholder="Paste your YAML content here..."
              style={{
                width: "100%",
                minHeight: "400px",
                padding: "16px",
                border: "1px solid #E0E0E0",
                borderRadius: "8px",
                fontSize: "13px",
                fontFamily: "monospace",
                backgroundColor: "#fff",
                color: "#333",
                lineHeight: "1.6",
                resize: "vertical",
                outline: "none",
                boxSizing: "border-box",
              }}
            />
          </div>
        )}

        {/* Action Buttons */}
        <div style={{ display: "flex", gap: "12px", marginTop: "32px" }}>
          {datasetType === "yaml" || datasetType === "openmetadata" ? (
            <>
              <Button
                label="Save"
                onClick={handleSaveDataset}
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
              <Button
                label="Cancel"
                onClick={handleBackToList}
                variant="secondary"
                size="medium"
                style={{
                  fontSize: "14px",
                  fontWeight: "600",
                  padding: "10px 24px",
                  minWidth: "100px"
                }}
              />
            </>
          ) : (
            <>
              <Button
                label="Generate dataset"
                onClick={handleGenerateDataset}
                variant="primary"
                size="medium"
                disabled={loading}
                style={{
                  fontSize: "14px",
                  fontWeight: "600",
                  padding: "10px 24px",
                  minWidth: "140px"
                }}
              />
              <Button
                label="Cancel"
                onClick={handleBackToList}
                variant="secondary"
                size="medium"
                style={{
                  fontSize: "14px",
                  fontWeight: "600",
                  padding: "10px 24px",
                  minWidth: "100px"
                }}
              />
            </>
          )}
        </div>

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
            <Text appearance="heading-s" color="primary-grey-100">Datasets</Text>
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
            Datasets
          </Text> */}
          <ActionButton
            kind="primary"
            size="medium"
            label="Create dataset"
            onClick={handleCreateDataset}
            style={{ flexShrink: 0, minWidth: "auto" }}
          />
        </div>
      </div>

      {/* Search and Filter Controls */}
      <div style={{ marginBottom: "16px", display: "flex", justifyContent: "flex-end", gap: "12px", width: "100%", boxSizing: "border-box" }}>
        <div style={{ position: "relative", width: "100%", maxWidth: "400px", minWidth: "280px" }}>
          <input
            type="text"
            placeholder="Search dataset name or description"
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
      {loading && datasetsList.length === 0 ? (
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
                      Name
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
                  onClick={() => handleSort("key")}
                >
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Key
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
                  </div>
                </th>
              </tr>
            </thead>
            <tbody>
              {sortedDatasets.length === 0 ? (
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
                        You have not added any dataset yet.
                      </Text>
                      <Button
                        label="Create dataset"
                        onClick={handleCreateDataset}
                        variant="primary"
                        size="medium"
                      />
                    </div>
                  </td>
                </tr>
              ) : (
                sortedDatasets.map((dataset) => (
                  <tr
                    key={dataset.id || dataset.datasetId}
                    onClick={() => handleDatasetClick(dataset)}
                    style={{
                      borderBottom: "1px solid #E0E0E0",
                      transition: "background-color 0.2s",
                      cursor: "pointer",
                    }}
                    onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = "#F9F9F9")}
                    onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = "transparent")}
                  >
                    <td style={{ padding: "12px 16px" }}>
                      <Text appearance="body-xs" color="primary-grey-100">
                        {dataset.name || "-"}
                      </Text>
                    </td>
                    <td style={{ padding: "12px 16px" }}>
                      <Text appearance="body-xs" color="primary-grey-100">
                        {dataset.description || "-"}
                      </Text>
                    </td>
                    <td style={{ padding: "12px 16px" }}>
                      <Text appearance="body-xs" color="primary-grey-100">
                        {dataset.key || "-"}
                      </Text>
                    </td>
                    <td style={{ padding: "12px 16px" }} onClick={(e) => e.stopPropagation()}>
                      <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
                        <button
                          onClick={() => handleEditDataset(dataset)}
                          aria-label="Edit dataset"
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
                          onClick={() => handleDeleteDataset(dataset)}
                          aria-label="Delete dataset"
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
        transition={Slide}
        autoClose={3000}
        hideProgressBar
        closeButton={CustomToast.CloseButton}
        closeOnClick
      />
    </div>
  );
};

export default Datasets;
