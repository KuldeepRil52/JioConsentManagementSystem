import { ActionButton, Button, InputFieldV2, Text } from "../custom-components";
import { useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import React, { useState, useEffect, useRef } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useDispatch } from "react-redux";
import { createRole, listComponents, searchRoles } from "../store/actions/CommonAction";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";

const AddRole = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const viewMode = queryParams.get('mode') === 'view';
  const roleId = queryParams.get('id');

  const [roleName, setRoleName] = useState("");
  const [text, setText] = useState("");
  const [accessToItems, setAccessToItems] = useState({});
  const [nameError, setNameError] = useState("");
  const [charError, setCharError] = useState("");
  const [expandedSections, setExpandedSections] = useState({});
  const selectAllCheckboxRef = useRef(null);

  const nameCharLimit = 25;
  const charLimit = 200;

  // Valid UUID pattern – only componentIds matching this are sent to the backend
  const isValidUUID = (id) => /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id);

  // Module-level access (System, Dataset) + Monitoring pages
  // These appear in the UI even if the backend's listComponents doesn't return them.
  // If their componentId is a placeholder string (not a UUID), they act as UI-only
  // umbrella controls and are filtered out before the API call.
  const STATIC_MODULES = [
    { componentName: 'SYSTEM', componentId: 'SYSTEM' },
    { componentName: 'DATASET', componentId: 'DATASET' },
    { componentName: 'MONITORING', componentId: 'MONITORING' },
    { componentName: 'SCHEDULER_STATS', componentId: 'SCHEDULER_STATS' },
    { componentName: 'DATA_COMPLIANCE_REPORT', componentId: 'DATA_COMPLIANCE_REPORT' },
    { componentName: 'DATA_DELETION_PURGE_DASHBOARD', componentId: 'DATA_DELETION_PURGE_DASHBOARD' },
  ];

  // Monitoring child components – toggled when the MONITORING umbrella is checked
  const MONITORING_CHILDREN = ['SCHEDULER_STATS', 'DATA_COMPLIANCE_REPORT', 'DATA_DELETION_PURGE_DASHBOARD'];

  // Function to convert component names to exact sidenav labels
  const getDisplayName = (componentName) => {
    const displayMap = {
      // Dashboard
      'DASHBOARD': 'Dashboard',
      'DPO_DASHBOARD': 'DPO Dashboard',
      
      // System Configuration (exact labels from sidenav)
      'SYSTEM_CONFIGURATION': 'Configuration',
      'SYSTEM_SETUP': 'Configuration',
      'SYSTEM_CONFIG': 'Configuration',
      'MASTER': 'Master Data',
      'MASTER_SETUP': 'Master Data',
      'MASTER_DATA': 'Master Data',
      'RETENTION_POLICIES': 'Retention Policies',
      'RETENTION': 'Retention Policies',
      'USER_DASHBOARD': 'User Portal Branding',
      'BRANDING': 'User Portal Branding',
      
      // Consent section (exact labels from sidenav)
      'CONSENT_TEMPLATES': 'Consent Templates',
      'TEMPLATES': 'Consent Templates',
      'CONSENT': 'Consent Configuration',
      'CONSENT_SETUP': 'Consent Configuration',
      'CONSENT_CONFIG': 'Consent Configuration',
      'EMAIL_TEMPLATE': 'Email template',
      'CONSENT_EMAIL_TEMPLATE': 'Email template',
      'SMS_TEMPLATE': 'SMS template',
      'CONSENT_SMS_TEMPLATE': 'SMS template',
      'CONSENT_LOGS': 'Consent Logs',
      
      // Cookies section (exact labels from sidenav)
      'REGISTERED_COOKIES': 'Cookie Templates',
      'COOKIES': 'Cookie Templates',
      'COOKIE_TEMPLATES': 'Cookie Templates',
      'COOKIES_LOGS': 'Cookies Logs',
      'COOKIE_LOGS': 'Cookies Logs',
      'COOKIE_CATEGORY': 'Manage Cookie Categories',
      'COOKIE_CATEGORIES': 'Manage Cookie Categories',
      'MANAGE_CATEGORIES': 'Manage Cookie Categories',
      
      // Governance section (exact labels from sidenav)
      'DATA_PROTECTION_OFFICER': 'Data Protection Officer',
      'DPO': 'Data Protection Officer',
      'ROPA': 'ROPA',
      'RECORD_OF_PROCESSING_ACTIVITIES': 'ROPA',
      'AUDIT_COMPLIANCE': 'Audit & Compliance Report',
      'AUDIT_AND_COMPLIANCE': 'Audit & Compliance Report',
      'AUDIT': 'Audit & Compliance Report',
      'BREACH_NOTIFICATIONS': 'Breach Notifications',
      'BREACH': 'Breach Notifications',
      'DATA_BREACH': 'Breach Notifications',
      
      // Grievance Redressal section (exact labels from sidenav)
      'GRIEVANCE': 'Grievance Configuration',
      'GRIEVANCE_SETUP': 'Grievance Configuration',
      'GRIEVANCE_CONFIG': 'Grievance Configuration',
      'GRIEVANCE_FORM': 'Grievance Form',
      'GRIEVANCE_FORM_TEMPLATES': 'Grievance Form',
      'FORM': 'Grievance Form',
      'FORMS': 'Grievance Form',
      'REQUESTS': 'Grievance Requests',
      'GRIEVANCE_REQUESTS': 'Grievance Requests',
      'REQUEST': 'Grievance Requests',
      'PENDING_REQUESTS': 'Pending Requests',
      'GRIEVANCE_EMAIL_TEMPLATE': 'Grievance Email Template',
      'GRIEVANCE_EMAIL': 'Grievance Email Template',
      'GRIEVANCE_SMS_TEMPLATE': 'Grievance SMS Template',
      'GRIEVANCE_SMS': 'Grievance SMS Template',
      'GRIEVANCE_LOGS': 'Grievance Logs',
      
      // Administration section (exact labels from sidenav)
      'BUSINESS_GROUPS': 'Business Groups',
      'BUSINESS': 'Business Groups',
      'ROLES': 'Roles',
      'ROLE': 'Roles',
      'USERS': 'Users',
      'USER': 'Users',
      'ORGANIZATION_MAP': 'Organization Map',
      'ORG_MAP': 'Organization Map',
      
      // Notifications section
      'NOTIFICATION': 'Notification Configuration',
      'NOTIFICATION_CONFIG': 'Notification Configuration',
      'NOTIFICATION_TEMPLATES': 'Notification Templates',
      'NOTIFICATION_TEMPLATE': 'Notification Templates',
      
      // Monitoring (3 pages in side nav - exact labels)
      'SCHEDULER_STATS': 'Scheduler Stats',
      'SCHEDULER': 'Scheduler Stats',
      'DATA_COMPLIANCE_REPORT': 'Data Compliance Report',
      'COMPLIANCE_REPORT': 'Data Compliance Report',
      'DATA_DELETION_PURGE_DASHBOARD': 'Data deletion and purge dashboard',
      'DATA_DELETION_PURGE': 'Data deletion and purge dashboard',
      
      // Module-level access (Consent section in side nav: Systems, Datasets)
      'SYSTEM': 'Systems',
      'SYSTEMS': 'Systems',
      'DATASET': 'Datasets',
      'DATASETS': 'Datasets',
      'MONITORING': 'Monitoring',
      
      // User Profile (Available to All)
      'PROFILE': 'Profile (Universal Access)'
    };
    return displayMap[componentName] || componentName;
  };

  useEffect(() => {
    const fetchComponents = async () => {
      try {
        const response = await dispatch(listComponents());
        console.log("listComponents response from backend:", response);
        const components = response || [];
        
        // Build list: ensure component is array of { componentName, componentId }
        let componentList = Array.isArray(components) ? components : [];
        if (!Array.isArray(components) && typeof components === 'object') {
          componentList = components.searchList || components.data || components.components || [];
        }
        
        // Merge static modules (System, Dataset, Monitoring) if not already returned by API
        const existingNames = new Set(componentList.map(c => (c.componentName || c.name || '').toUpperCase()));
        STATIC_MODULES.forEach(mod => {
          if (!existingNames.has(mod.componentName)) {
            componentList.unshift(mod);
            existingNames.add(mod.componentName);
          }
        });
        
        // Map components from backend + static modules
        const newAccessToItems = {};
        const newPermissions = {};
        
        componentList.forEach(component => {
          const name = component.componentName || component.name;
          const id = component.componentId || component.id || component.componentName || component.name;
          if (name) {
            newAccessToItems[name] = id;
            newPermissions[name] = { access: false };
          }
        });
        
        setAccessToItems(newAccessToItems);
        
        // If in view mode, load role data
        if (viewMode && roleId) {
          try {
            const roleResponse = await dispatch(searchRoles(roleId));
            const roleData = roleResponse || {};
            if (roleData) {
              setRoleName(roleData.role || "");
              setText(roleData.description || "");
              const fetchedPermissions = roleData.permissions || [];
              const updatedPermissions = { ...newPermissions };
              fetchedPermissions.forEach(p => {
                const componentName = Object.keys(newAccessToItems).find(key => newAccessToItems[key] === p.componentId);
                if (componentName) {
                  // Check if permission has access (could be "YES" or true or READ/WRITE)
                  // AddRole uses { access: true/false } structure
                  if (p.access === "YES" || p.access === true || (p.action && (p.action.includes('READ') || p.action.includes('WRITE')))) {
                    updatedPermissions[componentName] = { access: true };
                  }
                }
              });
              setPermissions(updatedPermissions);
            }
          } catch (error) {
            console.error("Failed to fetch role data:", error);
          }
        } else {
          setPermissions(newPermissions);
        }
      } catch (error) {
        console.error("Failed to fetch components from backend:", error);
      }
    };
    fetchComponents();
  }, [dispatch, viewMode, roleId]);

  const [permissions, setPermissions] = useState({});
  const [autoSelectedItems, setAutoSelectedItems] = useState({}); // Track items auto-selected due to dependencies

  // Get section for a component to organize display
  const getSection = (componentName) => {
    // Map to sections based on sidenav structure
    const sectionMap = {
      // Dashboard
      'DASHBOARD': 'Dashboard',
      'DPO_DASHBOARD': 'Dashboard',
      
      // System Configuration
      'SYSTEM_CONFIGURATION': 'System Configuration',
      'SYSTEM_SETUP': 'System Configuration',
      'SYSTEM_CONFIG': 'System Configuration',
      'MASTER': 'System Configuration',
      'MASTER_SETUP': 'System Configuration',
      'MASTER_DATA': 'System Configuration',
      'RETENTION_POLICIES': 'System Configuration',
      'RETENTION': 'System Configuration',
      'BRANDING': 'System Configuration',
      
      // Consent
      'CONSENT': 'Consent',
      'CONSENT_SETUP': 'Consent',
      'CONSENT_CONFIG': 'Consent',
      'TEMPLATES': 'Consent',
      'CONSENT_TEMPLATES': 'Consent',
      'EMAIL_TEMPLATE': 'Consent',
      'CONSENT_EMAIL_TEMPLATE': 'Consent',
      'SMS_TEMPLATE': 'Consent',
      'CONSENT_SMS_TEMPLATE': 'Consent',
      'CONSENT_LOGS': 'Consent',
      'SYSTEM': 'Consent',
      'SYSTEMS': 'Consent',
      'DATASET': 'Consent',
      'DATASETS': 'Consent',
      
      // Cookies
      'REGISTERED_COOKIES': 'Cookies',
      'COOKIES': 'Cookies',
      'COOKIE_TEMPLATES': 'Cookies',
      'COOKIES_LOGS': 'Cookies',
      'COOKIE_LOGS': 'Cookies',
      'COOKIE_CATEGORY': 'Cookies',
      'COOKIE_CATEGORIES': 'Cookies',
      'MANAGE_CATEGORIES': 'Cookies',
      
      // Governance
      'DATA_PROTECTION_OFFICER': 'Governance',
      'DPO': 'Governance',
      'ROPA': 'Governance',
      'RECORD_OF_PROCESSING_ACTIVITIES': 'Governance',
      'AUDIT_COMPLIANCE': 'Governance',
      'AUDIT_AND_COMPLIANCE': 'Governance',
      'AUDIT': 'Governance',
      'BREACH_NOTIFICATIONS': 'Governance',
      'BREACH': 'Governance',
      'DATA_BREACH': 'Governance',
      
      // Grievance Redressal
      'GRIEVANCE': 'Grievance Redressal',
      'GRIEVANCE_SETUP': 'Grievance Redressal',
      'GRIEVANCE_CONFIG': 'Grievance Redressal',
      'GRIEVANCE_FORM': 'Grievance Redressal',
      'GRIEVANCE_FORM_TEMPLATES': 'Grievance Redressal',
      'FORM': 'Grievance Redressal',
      'FORMS': 'Grievance Redressal',
      'REQUESTS': 'Grievance Redressal',
      'GRIEVANCE_REQUESTS': 'Grievance Redressal',
      'REQUEST': 'Grievance Redressal',
      'PENDING_REQUESTS': 'Grievance Redressal',
      'GRIEVANCE_EMAIL_TEMPLATE': 'Grievance Redressal',
      'GRIEVANCE_EMAIL': 'Grievance Redressal',
      'GRIEVANCE_SMS_TEMPLATE': 'Grievance Redressal',
      'GRIEVANCE_SMS': 'Grievance Redressal',
      'GRIEVANCE_LOGS': 'Grievance Redressal',
      
      // Administration
      'BUSINESS_GROUPS': 'Administration',
      'BUSINESS': 'Administration',
      'ROLES': 'Administration',
      'ROLE': 'Administration',
      'USERS': 'Administration',
      'USER': 'Administration',
      'ORGANIZATION_MAP': 'Administration',
      'ORG_MAP': 'Administration',
      
      // Notifications
      'NOTIFICATION': 'Notifications',
      'NOTIFICATION_CONFIG': 'Notifications',
      'NOTIFICATION_TEMPLATES': 'Notifications',
      'NOTIFICATION_TEMPLATE': 'Notifications',
      'CONFIGURATION': 'Notifications',
      
      // Monitoring (3 pages matching side nav)
      'SCHEDULER_STATS': 'Monitoring',
      'SCHEDULER': 'Monitoring',
      'DATA_COMPLIANCE_REPORT': 'Monitoring',
      'COMPLIANCE_REPORT': 'Monitoring',
      'DATA_DELETION_PURGE_DASHBOARD': 'Monitoring',
      'DATA_DELETION_PURGE': 'Monitoring',
      'MONITORING': 'Monitoring',
      
      // User Profile (Available to All)
      'PROFILE': 'User Profile (Universal Access)',
      'USER_DASHBOARD': 'User Profile (Universal Access)',
    };
    return sectionMap[componentName] || 'Other';
  };

  // Group components by section
  const groupedComponents = () => {
    const groups = {};
    Object.keys(accessToItems).forEach((item) => {
      const section = getSection(item);
      if (!groups[section]) {
        groups[section] = [];
      }
      groups[section].push(item);
    });
    
    // Sort sections alphabetically, "Other" last (matches side nav grouping)
    const sortedKeys = Object.keys(groups).sort((a, b) => {
      if (a === 'Other') return 1;
      if (b === 'Other') return -1;
      return a.localeCompare(b);
    });
    
    const sortedGroups = {};
    sortedKeys.forEach(key => {
      sortedGroups[key] = groups[key];
    });
    
    return sortedGroups;
  };

  const handlePermissionChange = (item) => {
    const isCurrentlySelected = permissions[item]?.access;
    const newAccessValue = !isCurrentlySelected;
    
    // Check if this is a Consent-related component
    const consentComponents = ['CONSENT', 'CONSENT_SETUP', 'CONSENT_CONFIG', 'TEMPLATES', 'CONSENT_TEMPLATES', 'EMAIL_TEMPLATE', 'CONSENT_EMAIL_TEMPLATE', 'SMS_TEMPLATE', 'CONSENT_SMS_TEMPLATE', 'CONSENT_LOGS'];
    const isConsentComponent = consentComponents.includes(item);
    
    // Find the MASTER component key in accessToItems
    const masterKey = Object.keys(accessToItems).find(key => 
      key === 'MASTER' || key === 'MASTER_DATA' || key === 'MASTER_SETUP'
    );
    
    // Prevent deselecting Master Data if any Consent components are selected
    const isMasterData = item === masterKey;
    if (isMasterData && isCurrentlySelected) {
      const anyConsentSelected = consentComponents.some(comp => permissions[comp]?.access === true);
      if (anyConsentSelected) {
        return; // Silently prevent deselection - user will see "Auto-selected" badge
      }
    }
    
    setPermissions((prev) => {
      const updated = {
        ...prev,
        [item]: {
          access: newAccessValue,
        },
      };
      
      // MONITORING umbrella: toggle all child monitoring pages together
      if (item === 'MONITORING') {
        MONITORING_CHILDREN.forEach(child => {
          if (updated[child] !== undefined) {
            updated[child] = { access: newAccessValue };
          }
        });
      }
      
      // If a monitoring child is unchecked, uncheck the MONITORING umbrella
      if (MONITORING_CHILDREN.includes(item) && !newAccessValue) {
        if (updated['MONITORING'] !== undefined) {
          updated['MONITORING'] = { access: false };
        }
      }
      // If all monitoring children are checked, auto-check the MONITORING umbrella
      if (MONITORING_CHILDREN.includes(item) && newAccessValue) {
        const allChildrenSelected = MONITORING_CHILDREN.every(child => updated[child]?.access === true);
        if (allChildrenSelected && updated['MONITORING'] !== undefined) {
          updated['MONITORING'] = { access: true };
        }
      }
      
      // If selecting a Consent component, auto-select Master Data
      if (isConsentComponent && newAccessValue && masterKey) {
        updated[masterKey] = {
          access: true,
        };
        
        // Track that Master Data was auto-selected
        setAutoSelectedItems(prevAuto => ({
          ...prevAuto,
          [masterKey]: 'Required for Consent Management',
        }));
      }
      
      // If deselecting a Consent component, check if we should remove auto-selection
      if (isConsentComponent && !newAccessValue && masterKey) {
        // Check if any other Consent components are still selected
        const otherConsentSelected = consentComponents.some(comp => 
          comp !== item && updated[comp]?.access === true
        );
        
        // If no Consent components are selected and Master was auto-selected, remove the auto-selection marker
        if (!otherConsentSelected && autoSelectedItems[masterKey]) {
          setAutoSelectedItems(prevAuto => {
            const newAuto = { ...prevAuto };
            delete newAuto[masterKey];
            return newAuto;
          });
        }
      }
      
      return updated;
    });
  };

  // Check if all items are selected
  const areAllSelected = () => {
    const allItems = Object.keys(accessToItems);
    if (allItems.length === 0) return false;
    return allItems.every(item => permissions[item]?.access === true);
  };

  // Check if some (but not all) items are selected
  const areSomeSelected = () => {
    const allItems = Object.keys(accessToItems);
    if (allItems.length === 0) return false;
    const selectedCount = allItems.filter(item => permissions[item]?.access === true).length;
    return selectedCount > 0 && selectedCount < allItems.length;
  };

  // Handle select all checkbox
  const handleSelectAll = () => {
    const allSelected = areAllSelected();
    const allItems = Object.keys(accessToItems);
    const newPermissions = {};
    
    allItems.forEach(item => {
      newPermissions[item] = { access: !allSelected };
    });
    
    setPermissions(newPermissions);
  };

  // Check if all items in a section are selected
  const areSectionItemsAllSelected = (sectionComponents) => {
    if (sectionComponents.length === 0) return false;
    return sectionComponents.every(item => permissions[item]?.access === true);
  };

  // Check if some (but not all) items in a section are selected
  const areSectionItemsSomeSelected = (sectionComponents) => {
    if (sectionComponents.length === 0) return false;
    const selectedCount = sectionComponents.filter(item => permissions[item]?.access === true).length;
    return selectedCount > 0 && selectedCount < sectionComponents.length;
  };

  // Handle section-level select all
  const handleSectionSelectAll = (sectionComponents) => {
    const allSelected = areSectionItemsAllSelected(sectionComponents);
    const newPermissions = { ...permissions };
    
    const consentComponents = ['CONSENT', 'CONSENT_SETUP', 'CONSENT_CONFIG', 'TEMPLATES', 'CONSENT_TEMPLATES', 'EMAIL_TEMPLATE', 'CONSENT_EMAIL_TEMPLATE', 'SMS_TEMPLATE', 'CONSENT_SMS_TEMPLATE', 'CONSENT_LOGS'];
    const masterKey = Object.keys(accessToItems).find(key => 
      key === 'MASTER' || key === 'MASTER_DATA' || key === 'MASTER_SETUP'
    );
    
    let hasConsentComponent = false;
    
    sectionComponents.forEach(item => {
      newPermissions[item] = { access: !allSelected };
      if (consentComponents.includes(item) && !allSelected) {
        hasConsentComponent = true;
      }
    });
    
    // If selecting Consent components, auto-select Master Data
    if (hasConsentComponent && masterKey) {
      newPermissions[masterKey] = { access: true };
      
      setAutoSelectedItems(prevAuto => ({
        ...prevAuto,
        [masterKey]: 'Required for Consent Management',
      }));
    }
    
    // If deselecting Consent section, check if we should remove auto-selection
    if (allSelected && sectionComponents.some(comp => consentComponents.includes(comp)) && masterKey) {
      const otherConsentSelected = consentComponents.some(comp => 
        !sectionComponents.includes(comp) && newPermissions[comp]?.access === true
      );
      
      if (!otherConsentSelected && autoSelectedItems[masterKey]) {
        setAutoSelectedItems(prevAuto => {
          const newAuto = { ...prevAuto };
          delete newAuto[masterKey];
          return newAuto;
        });
      }
    }
    
    setPermissions(newPermissions);
  };

  // Toggle section expand/collapse
  const toggleSection = (section) => {
    setExpandedSections(prev => ({
      ...prev,
      [section]: !prev[section]
    }));
  };

  // Initialize all sections as expanded on first load
  useEffect(() => {
    const grouped = groupedComponents();
    const initialExpanded = {};
    Object.keys(grouped).forEach(section => {
      initialExpanded[section] = true; // Start with all expanded
    });
    setExpandedSections(initialExpanded);
  }, [accessToItems]); // Re-run when components are loaded

  // Update indeterminate state of select all checkbox
  useEffect(() => {
    if (selectAllCheckboxRef.current) {
      const allItems = Object.keys(accessToItems);
      if (allItems.length === 0) {
        selectAllCheckboxRef.current.indeterminate = false;
        return;
      }
      const selectedCount = allItems.filter(item => permissions[item]?.access === true).length;
      selectAllCheckboxRef.current.indeterminate = selectedCount > 0 && selectedCount < allItems.length;
    }
  }, [permissions, accessToItems]);

  const handleNameChange = (e) => {
    const inputValue = e.target.value;
    if (inputValue.length <= nameCharLimit) {
      setRoleName(inputValue);
      if (inputValue.length === nameCharLimit) {
        setNameError("Maximum 25 characters allowed.");
      } else {
        setNameError("");
      }
    } else {
      setRoleName(inputValue.slice(0, nameCharLimit));
      setNameError("Maximum 25 characters allowed.");
    }
  };

  const handleTextAreaChange = (e) => {
    const inputValue = e.target.value;
    if (inputValue.length <= charLimit) {
      setText(inputValue);
      if (inputValue.length === charLimit) {
        setCharError("Maximum 200 characters allowed.");
      } else {
        setCharError("");
      }
    } else {
      setText(inputValue.slice(0, charLimit));
      setCharError("Maximum 200 characters allowed.");
    }
  };

  const handleBack = () => {
    navigate("/roles");
  };

  const handleSubmit = async () => {
    if (!roleName.trim() || !text.trim()) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Role name and description are required."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (roleName.length > nameCharLimit) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Maximum 25 characters allowed for role name."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (text.length > charLimit) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Maximum 200 characters allowed for description."}
          />
        ),
        { icon: false }
      );
      return;
    }

    const permissionsPayload = [];
    for (const componentName in permissions) {
      if (permissions[componentName].access) {
        const compId = accessToItems[componentName];
        // Only send componentIds that the backend recognizes (valid UUIDs).
        // Static/umbrella modules (MONITORING, SYSTEM, DATASET etc.) have
        // string-based placeholder IDs and are UI-only controls.
        if (isValidUUID(compId)) {
          permissionsPayload.push({ 
            componentId: compId, 
            access: "YES"
          });
        }
      }
    }

    if (permissionsPayload.length === 0) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Kindly select access for the role"}
          />
        ),
        { icon: false }
      );
      return;
    }

    try {
      const response = await dispatch(createRole(roleName, text, permissionsPayload));
      console.log("Create role response:", response);
      if (response && (response.status === 200 || response.status === 201)) {
        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message={"Role created successfully"}
            />
          ),
          { icon: false }
        );
        // Add delay before navigation to ensure toast is visible
        setTimeout(() => {
          navigate("/roles");
        }, 1000);
      } else {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Failed to create role. Please try again."}
            />
          ),
          { icon: false }
        );
        console.error("API call succeeded but role creation failed. Response:", response);
      }
    } catch (error) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={error?.message || "Error creating role. Please try again."}
          />
        ),
        { icon: false }
      );
      console.error("Error creating role. See details below.");
      if (error.response) {
        console.error("Server responded with an error:");
        console.error("Status:", error.response.status);
        console.error("Data:", error.response.data);
        console.error("Headers:", error.response.headers);
      } else if (error.request) {
        console.error("No response received from the server. Check network and CORS settings.");
        console.error("Request details:", error.request);
      } else {
        console.error("Error setting up the request:", error.message);
      }
      console.error("Full error object:", error);
    }
  };

  return (
    <>
      <div className="configurePage">
        <div
          style={{
            display: "flex",
            gap: 70,
            width: "50%",
          }}
        >
          <div style={{ flex: "0 0 auto" }}>
            <Button
              ariaControls="Button Clickable"
              ariaDescribedby="Button"
              ariaExpanded="Expanded"
              ariaLabel="Button"
              className="Button"
              icon="ic_back"
              iconAriaLabel="Icon Favorite"
              iconLeft="ic_back"
              kind="secondary"
              onClick={handleBack}
              size="medium"
              state="normal"
            />
          </div>
          <div style={{ flex: 1 }}>
            <div>
              <Text appearance="heading-xs" color="primary-grey-100">
                {viewMode ? 'View Role' : 'Add Role'}
              </Text>
            </div>
            <br></br>
            <InputFieldV2 label="Role name (Required)" size="small"
              value={roleName}
              onChange={handleNameChange}
              maxLength={nameCharLimit}
              state={nameError ? "error" : "none"}
              stateText={nameError || ""}
              placeholder="Enter the name of the role"
              disabled={viewMode}
            ></InputFieldV2>
            <div style={{ display: "flex", justifyContent: "flex-end", marginTop: "4px" }}>
              <Text appearance="body-xs" color={roleName.length === nameCharLimit ? "error" : "primary-grey-80"} style={{ fontSize: "12px" }}>
                {roleName.length}/{nameCharLimit}
              </Text>
            </div>
            <br></br>
            <Text appearance="body-xs" color="primary-grey-80" placeholder="Enter the purpose or details of the role">
              Description (Required)
            </Text>

            <textarea
              placeholder="Enter the purpose or details of the role"
              value={text}
              onChange={handleTextAreaChange}
              maxLength={charLimit}
              rows="4"
              className="custom-text-area"
              disabled={viewMode}
            />
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "4px" }}>
              <div>
                {charError && (
                  <Text appearance="body-xs" color="error" style={{ fontSize: "12px" }}>
                    {charError}
                  </Text>
                )}
              </div>
              <div className="char-counter" style={{ color: text.length === charLimit ? "#f44336" : "#666" }}>
                {text.length}/{charLimit}
              </div>
            </div>
            <br></br>
            <div>
              <div style={{ padding: "5px", display: 'inline-block', width: '110%' }}>
                <table className="business-table">
                  <thead>
                    <tr>
                      <th>Access to</th>
                      <th style={{ textAlign: 'center', verticalAlign: 'middle' }}>
                        <label className="custom-checkbox-container" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', marginLeft: '17%' }}>
                          <input
                            type="checkbox"
                            className="custom-checkbox"
                            ref={selectAllCheckboxRef}
                            checked={areAllSelected()}
                            onChange={handleSelectAll}
                            style={{ cursor: viewMode ? 'not-allowed' : 'pointer' }}
                            disabled={viewMode}
                          />
                          <span style={{ marginLeft: '8px', fontSize: '13px', fontWeight: 'normal' }}>Select All</span>
                        </label>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {Object.entries(groupedComponents()).map(([section, components]) => {
                      const sectionCheckboxId = `section-${section.replace(/\s+/g, '-')}`;
                      const isExpanded = expandedSections[section];
                      return (
                        <React.Fragment key={section}>
                          {/* Section Header with Select All */}
                          <tr style={{ backgroundColor: '#f3f4f6' }}>
                            <td 
                              style={{ fontWeight: 'bold', fontSize: '13px', color: '#374151', padding: '10px', cursor: 'pointer' }}
                              onClick={() => toggleSection(section)}
                            >
                              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <svg 
                                  width="14" 
                                  height="14" 
                                  viewBox="0 0 24 24" 
                                  fill="none" 
                                  style={{ 
                                    transition: 'transform 0.2s ease',
                                    transform: isExpanded ? 'rotate(90deg)' : 'rotate(0deg)',
                                    flexShrink: 0,
                                    opacity: 0.6
                                  }}
                                >
                                  <path 
                                    d="M9 18l6-6-6-6" 
                                    stroke="currentColor" 
                                    strokeWidth="2" 
                                    strokeLinecap="round" 
                                    strokeLinejoin="round"
                                  />
                                </svg>
                                <span>{section}</span>
                              </div>
                            </td>
                            <td style={{ padding: '10px', textAlign: 'center', verticalAlign: 'middle' }}>
                              <label className="custom-checkbox-container">
                                <input
                                  type="checkbox"
                                  id={sectionCheckboxId}
                                  className="custom-checkbox"
                                  checked={areSectionItemsAllSelected(components)}
                                  ref={(el) => {
                                    if (el) {
                                      el.indeterminate = areSectionItemsSomeSelected(components);
                                    }
                                  }}
                                  onChange={(e) => {
                                    e.stopPropagation();
                                    handleSectionSelectAll(components);
                                  }}
                                  onClick={(e) => e.stopPropagation()}
                                  disabled={viewMode}
                                  style={{ cursor: viewMode ? 'not-allowed' : 'pointer' }}
                                  title="Select/Deselect all items in this section"
                                />
                              </label>
                            </td>
                          </tr>
                          {/* Components in this section - only show if expanded */}
                          {isExpanded && components.map((item) => (
                            <tr key={item}>
                              <td style={{ paddingLeft: '24px' }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                  <span>{getDisplayName(item)}</span>
                                  {autoSelectedItems[item] && (
                                    <span 
                                      style={{
                                        fontSize: '11px',
                                        padding: '2px 8px',
                                        background: '#E6F4FF',
                                        color: '#0052CC',
                                        borderRadius: '4px',
                                        fontWeight: '500',
                                        border: '1px solid #B3D9FF'
                                      }}
                                      title={autoSelectedItems[item]}
                                    >
                                      Auto-selected (Required for Consent)
                                    </span>
                                  )}
                                </div>
                              </td>
                              <td style={{ textAlign: 'center', verticalAlign: 'middle' }}>
                                <label className="custom-checkbox-container">
                                  <input
                                    type="checkbox"
                                    className="custom-checkbox"
                                    checked={permissions[item]?.access || false}
                                    onChange={() => handlePermissionChange(item)}
                                    disabled={viewMode}
                                    style={{ cursor: viewMode ? 'not-allowed' : 'pointer' }}
                                    title={autoSelectedItems[item] || ''}
                                  />
                                </label>
                              </td>
                            </tr>
                          ))}
                        </React.Fragment>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>

            {!viewMode && (
              <div className="flex justify-end">
                <ActionButton label="Add" kind="primary" onClick={handleSubmit}></ActionButton>
              </div>
            )}
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
    </>
  );
};
export default AddRole;
