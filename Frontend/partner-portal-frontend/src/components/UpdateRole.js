import React, { useState, useEffect, useRef } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useDispatch } from "react-redux";
import { ActionButton, Button, InputFieldV2, Text } from "../custom-components";
import { updateRole, searchRoles, listComponents } from "../store/actions/CommonAction";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";

const UpdateRole = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { roleId } = useParams();

  const [roleName, setRoleName] = useState("");
  const [text, setText] = useState("");
  const [accessToItems, setAccessToItems] = useState({});
  const [permissions, setPermissions] = useState({});
  const [nameError, setNameError] = useState("");
  const [charError, setCharError] = useState("");
  const [expandedSections, setExpandedSections] = useState({});
  const [autoSelectedItems, setAutoSelectedItems] = useState({});
  const selectAllCheckboxRef = useRef(null);

  const nameCharLimit = 25;
  const charLimit = 200;

  // Valid UUID pattern – only componentIds matching this are sent to the backend
  const isValidUUID = (id) => /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id);

  // Module-level access – ensure they appear in the list even if not returned by API.
  // If their componentId is a placeholder string (not UUID), they act as UI-only
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

  // Convert component names to sidenav display labels
  const getDisplayName = (componentName) => {
    const displayMap = {
      'DASHBOARD': 'Dashboard',
      'DPO_DASHBOARD': 'DPO Dashboard',
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
      'REGISTERED_COOKIES': 'Cookie Templates',
      'COOKIES': 'Cookie Templates',
      'COOKIE_TEMPLATES': 'Cookie Templates',
      'COOKIES_LOGS': 'Cookies Logs',
      'COOKIE_LOGS': 'Cookies Logs',
      'COOKIE_CATEGORY': 'Manage Cookie Categories',
      'COOKIE_CATEGORIES': 'Manage Cookie Categories',
      'MANAGE_CATEGORIES': 'Manage Cookie Categories',
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
      'BUSINESS_GROUPS': 'Business Groups',
      'BUSINESS': 'Business Groups',
      'ROLES': 'Roles',
      'ROLE': 'Roles',
      'USERS': 'Users',
      'USER': 'Users',
      'ORGANIZATION_MAP': 'Organization Map',
      'ORG_MAP': 'Organization Map',
      'NOTIFICATION': 'Notification Configuration',
      'NOTIFICATION_CONFIG': 'Notification Configuration',
      'NOTIFICATION_TEMPLATES': 'Notification Templates',
      'NOTIFICATION_TEMPLATE': 'Notification Templates',
      'SCHEDULER_STATS': 'Scheduler Stats',
      'SCHEDULER': 'Scheduler Stats',
      'DATA_COMPLIANCE_REPORT': 'Data Compliance Report',
      'COMPLIANCE_REPORT': 'Data Compliance Report',
      'DATA_DELETION_PURGE_DASHBOARD': 'Data deletion and purge dashboard',
      'DATA_DELETION_PURGE': 'Data deletion and purge dashboard',
      'SYSTEM': 'Systems',
      'SYSTEMS': 'Systems',
      'DATASET': 'Datasets',
      'DATASETS': 'Datasets',
      'MONITORING': 'Monitoring',
      'PROFILE': 'Profile (Universal Access)',
    };
    return displayMap[componentName] || componentName;
  };

  // Map component to sidenav section
  const getSection = (componentName) => {
    const sectionMap = {
      'DASHBOARD': 'Dashboard',
      'DPO_DASHBOARD': 'Dashboard',
      'SYSTEM_CONFIGURATION': 'System Configuration',
      'SYSTEM_SETUP': 'System Configuration',
      'SYSTEM_CONFIG': 'System Configuration',
      'MASTER': 'System Configuration',
      'MASTER_SETUP': 'System Configuration',
      'MASTER_DATA': 'System Configuration',
      'RETENTION_POLICIES': 'System Configuration',
      'RETENTION': 'System Configuration',
      'BRANDING': 'System Configuration',
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
      'REGISTERED_COOKIES': 'Cookies',
      'COOKIES': 'Cookies',
      'COOKIE_TEMPLATES': 'Cookies',
      'COOKIES_LOGS': 'Cookies',
      'COOKIE_LOGS': 'Cookies',
      'COOKIE_CATEGORY': 'Cookies',
      'COOKIE_CATEGORIES': 'Cookies',
      'MANAGE_CATEGORIES': 'Cookies',
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
      'BUSINESS_GROUPS': 'Administration',
      'BUSINESS': 'Administration',
      'ROLES': 'Administration',
      'ROLE': 'Administration',
      'USERS': 'Administration',
      'USER': 'Administration',
      'ORGANIZATION_MAP': 'Administration',
      'ORG_MAP': 'Administration',
      'NOTIFICATION': 'Notifications',
      'NOTIFICATION_CONFIG': 'Notifications',
      'NOTIFICATION_TEMPLATES': 'Notifications',
      'NOTIFICATION_TEMPLATE': 'Notifications',
      'CONFIGURATION': 'Notifications',
      'SCHEDULER_STATS': 'Monitoring',
      'SCHEDULER': 'Monitoring',
      'DATA_COMPLIANCE_REPORT': 'Monitoring',
      'COMPLIANCE_REPORT': 'Monitoring',
      'DATA_DELETION_PURGE_DASHBOARD': 'Monitoring',
      'DATA_DELETION_PURGE': 'Monitoring',
      'MONITORING': 'Monitoring',
      'PROFILE': 'User Profile (Universal Access)',
      'USER_DASHBOARD': 'User Profile (Universal Access)',
    };
    return sectionMap[componentName] || 'Other';
  };

  // ── Fetch components + role data ──
  useEffect(() => {
    const fetchData = async () => {
      try {
        // Fetch components from backend
        const componentsResponse = await dispatch(listComponents());
        const components = componentsResponse || [];

        let componentList = Array.isArray(components) ? components : [];
        if (!Array.isArray(components) && typeof components === 'object') {
          componentList = components.searchList || components.data || components.components || [];
        }

        // Merge static modules if not already returned by API
        const existingNames = new Set(componentList.map(c => (c.componentName || c.name || '').toUpperCase()));
        STATIC_MODULES.forEach(mod => {
          if (!existingNames.has(mod.componentName)) {
            componentList.unshift(mod);
            existingNames.add(mod.componentName);
          }
        });

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

        // Fetch existing role details and mark permissions
        if (roleId) {
          const roleResponse = await dispatch(searchRoles(roleId));
          const roleData = roleResponse || {};
          if (roleData) {
            setRoleName(roleData.role || "");
            setText(roleData.description || "");

            const fetchedPermissions = roleData.permissions || [];
            const updatedPermissions = { ...newPermissions };
            fetchedPermissions.forEach(p => {
              const componentName = Object.keys(newAccessToItems).find(
                key => newAccessToItems[key] === p.componentId
              );
              if (componentName) {
                if (
                  p.access === "YES" ||
                  p.access === true ||
                  (p.action && (p.action.includes('READ') || p.action.includes('WRITE')))
                ) {
                  updatedPermissions[componentName] = { access: true };
                }
              }
            });
            setPermissions(updatedPermissions);
          }
        } else {
          setPermissions(newPermissions);
        }
      } catch (error) {
        console.error("Failed to fetch data in UpdateRole:", error);
        toast.error(
          (props) => (
            <CustomToast {...props} type="error" message="Failed to load role data." />
          ),
          { icon: false }
        );
      }
    };
    fetchData();
  }, [dispatch, roleId]);

  // ── Group components by section ──
  const groupedComponents = () => {
    const groups = {};
    Object.keys(accessToItems).forEach((item) => {
      const section = getSection(item);
      if (!groups[section]) groups[section] = [];
      groups[section].push(item);
    });

    const sortedKeys = Object.keys(groups).sort((a, b) => {
      if (a === 'Other') return 1;
      if (b === 'Other') return -1;
      return a.localeCompare(b);
    });

    const sortedGroups = {};
    sortedKeys.forEach(key => { sortedGroups[key] = groups[key]; });
    return sortedGroups;
  };

  // ── Permission helpers ──
  const consentComponents = [
    'CONSENT', 'CONSENT_SETUP', 'CONSENT_CONFIG', 'TEMPLATES', 'CONSENT_TEMPLATES',
    'EMAIL_TEMPLATE', 'CONSENT_EMAIL_TEMPLATE', 'SMS_TEMPLATE', 'CONSENT_SMS_TEMPLATE',
    'CONSENT_LOGS',
  ];

  const getMasterKey = () =>
    Object.keys(accessToItems).find(key =>
      key === 'MASTER' || key === 'MASTER_DATA' || key === 'MASTER_SETUP'
    );

  const handlePermissionChange = (item) => {
    const isCurrentlySelected = permissions[item]?.access;
    const newAccessValue = !isCurrentlySelected;

    const isConsentComponent = consentComponents.includes(item);
    const masterKey = getMasterKey();

    // Prevent deselecting Master Data while any Consent component is selected
    if (item === masterKey && isCurrentlySelected) {
      if (consentComponents.some(comp => permissions[comp]?.access === true)) return;
    }

    setPermissions(prev => {
      const updated = { ...prev, [item]: { access: newAccessValue } };

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

      if (isConsentComponent && newAccessValue && masterKey) {
        updated[masterKey] = { access: true };
        setAutoSelectedItems(p => ({ ...p, [masterKey]: 'Required for Consent Management' }));
      }

      if (isConsentComponent && !newAccessValue && masterKey) {
        const otherConsentSelected = consentComponents.some(
          comp => comp !== item && updated[comp]?.access === true
        );
        if (!otherConsentSelected && autoSelectedItems[masterKey]) {
          setAutoSelectedItems(p => { const n = { ...p }; delete n[masterKey]; return n; });
        }
      }

      return updated;
    });
  };

  // ── Select-all helpers ──
  const areAllSelected = () => {
    const all = Object.keys(accessToItems);
    return all.length > 0 && all.every(item => permissions[item]?.access === true);
  };

  const handleSelectAll = () => {
    const allSelected = areAllSelected();
    const newPerms = {};
    Object.keys(accessToItems).forEach(item => { newPerms[item] = { access: !allSelected }; });
    setPermissions(newPerms);
  };

  const areSectionItemsAllSelected = (components) =>
    components.length > 0 && components.every(item => permissions[item]?.access === true);

  const areSectionItemsSomeSelected = (components) => {
    const count = components.filter(item => permissions[item]?.access === true).length;
    return count > 0 && count < components.length;
  };

  const handleSectionSelectAll = (sectionComponents) => {
    const allSelected = areSectionItemsAllSelected(sectionComponents);
    const newPerms = { ...permissions };
    const masterKey = getMasterKey();
    let hasConsent = false;

    sectionComponents.forEach(item => {
      newPerms[item] = { access: !allSelected };
      if (consentComponents.includes(item) && !allSelected) hasConsent = true;
    });

    if (hasConsent && masterKey) {
      newPerms[masterKey] = { access: true };
      setAutoSelectedItems(p => ({ ...p, [masterKey]: 'Required for Consent Management' }));
    }

    if (allSelected && sectionComponents.some(c => consentComponents.includes(c)) && masterKey) {
      const otherConsentSelected = consentComponents.some(
        c => !sectionComponents.includes(c) && newPerms[c]?.access === true
      );
      if (!otherConsentSelected && autoSelectedItems[masterKey]) {
        setAutoSelectedItems(p => { const n = { ...p }; delete n[masterKey]; return n; });
      }
    }

    setPermissions(newPerms);
  };

  // ── Section expand / collapse ──
  const toggleSection = (section) => {
    setExpandedSections(prev => ({ ...prev, [section]: !prev[section] }));
  };

  useEffect(() => {
    const grouped = groupedComponents();
    const initial = {};
    Object.keys(grouped).forEach(s => { initial[s] = true; });
    setExpandedSections(initial);
  }, [accessToItems]);

  // Indeterminate state for top-level "Select All"
  useEffect(() => {
    if (selectAllCheckboxRef.current) {
      const all = Object.keys(accessToItems);
      const count = all.filter(i => permissions[i]?.access === true).length;
      selectAllCheckboxRef.current.indeterminate = count > 0 && count < all.length;
    }
  }, [permissions, accessToItems]);

  // ── Name / description handlers ──
  const handleNameChange = (e) => {
    const v = e.target.value;
    if (v.length <= nameCharLimit) {
      setRoleName(v);
      setNameError(v.length === nameCharLimit ? "Maximum 25 characters allowed." : "");
    } else {
      setRoleName(v.slice(0, nameCharLimit));
      setNameError("Maximum 25 characters allowed.");
    }
  };

  const handleTextAreaChange = (e) => {
    const v = e.target.value;
    if (v.length <= charLimit) {
      setText(v);
      setCharError(v.length === charLimit ? "Maximum 200 characters allowed." : "");
    } else {
      setText(v.slice(0, charLimit));
      setCharError("Maximum 200 characters allowed.");
    }
  };

  const handleBack = () => navigate("/roles");

  // ── Submit ──
  const handleSubmit = async () => {
    if (!roleName.trim() || !text.trim()) {
      toast.error(
        (props) => <CustomToast {...props} type="error" message="Role name and description are required." />,
        { icon: false }
      );
      return;
    }

    const permissionsPayload = [];
    for (const componentName in permissions) {
      if (permissions[componentName].access) {
        const compId = accessToItems[componentName];
        // Only send componentIds that the backend recognizes (valid UUIDs).
        // Static/umbrella modules use string placeholder IDs and are UI-only.
        if (isValidUUID(compId)) {
          permissionsPayload.push({
            componentId: compId,
            access: "YES",
          });
        }
      }
    }

    if (permissionsPayload.length === 0) {
      toast.error(
        (props) => <CustomToast {...props} type="error" message="Kindly select access for the role" />,
        { icon: false }
      );
      return;
    }

    const payload = { role: roleName, description: text, permissions: permissionsPayload };

    try {
      const response = await dispatch(updateRole(roleId, payload));
      if (response && (response.status === 200 || response.status === 201)) {
        toast.success(
          (props) => <CustomToast {...props} type="success" message="Role updated successfully!" />,
          { icon: false }
        );
        setTimeout(() => navigate("/roles"), 1000);
      } else {
        toast.error(
          (props) => <CustomToast {...props} type="error" message="Failed to update role. Please try again." />,
          { icon: false }
        );
      }
    } catch (error) {
      console.error("Update role error:", error);
      let errorMessage = "An error occurred while updating the role.";
      if (Array.isArray(error) && error[0]?.errorMessage) errorMessage = error[0].errorMessage;
      else if (error?.errorList?.[0]?.errorMessage) errorMessage = error.errorList[0].errorMessage;
      else if (error?.errorMessage) errorMessage = error.errorMessage;
      else if (error?.message) errorMessage = error.message;

      toast.error(
        (props) => <CustomToast {...props} type="error" message={errorMessage} />,
        { icon: false }
      );
    }
  };

  // ── Render ──
  return (
    <>
      <div className="configurePage">
        <div style={{ display: "flex", gap: 70, width: "50%" }}>
          <div style={{ flex: "0 0 auto" }}>
            <Button icon="ic_back" kind="secondary" onClick={handleBack} size="medium" />
          </div>
          <div style={{ flex: 1 }}>
            <Text appearance="heading-xs" color="primary-grey-100">Update Role</Text>
            <br />
            <InputFieldV2
              label="Role name (Required)"
              size="small"
              value={roleName}
              onChange={handleNameChange}
              maxLength={nameCharLimit}
              state={nameError ? "error" : "none"}
              stateText={nameError || ""}
              placeholder="Enter the name of the role"
            />
            <div style={{ display: "flex", justifyContent: "flex-end", marginTop: "4px" }}>
              <Text appearance="body-xs" color={roleName.length === nameCharLimit ? "error" : "primary-grey-80"} style={{ fontSize: "12px" }}>
                {roleName.length}/{nameCharLimit}
              </Text>
            </div>
            <br />
            <Text appearance="body-xs" color="primary-grey-80">Description (Required)</Text>
            <textarea
              placeholder="Enter the purpose or details of the role"
              value={text}
              onChange={handleTextAreaChange}
              maxLength={charLimit}
              rows="4"
              className="custom-text-area"
            />
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "4px" }}>
              <div>
                {charError && (
                  <Text appearance="body-xs" color="error" style={{ fontSize: "12px" }}>{charError}</Text>
                )}
              </div>
              <div className="char-counter" style={{ color: text.length === charLimit ? "#f44336" : "#666" }}>
                {text.length}/{charLimit}
              </div>
            </div>
            <br />

            {/* ── Permissions table ── */}
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
                          style={{ cursor: 'pointer' }}
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
                        {/* Section header */}
                        <tr style={{ backgroundColor: '#f3f4f6' }}>
                          <td
                            style={{ fontWeight: 'bold', fontSize: '13px', color: '#374151', padding: '10px', cursor: 'pointer' }}
                            onClick={() => toggleSection(section)}
                          >
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                              <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
                                style={{ transition: 'transform 0.2s ease', transform: isExpanded ? 'rotate(90deg)' : 'rotate(0deg)', flexShrink: 0, opacity: 0.6 }}>
                                <path d="M9 18l6-6-6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
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
                                ref={(el) => { if (el) el.indeterminate = areSectionItemsSomeSelected(components); }}
                                onChange={(e) => { e.stopPropagation(); handleSectionSelectAll(components); }}
                                onClick={(e) => e.stopPropagation()}
                                style={{ cursor: 'pointer' }}
                                title="Select/Deselect all items in this section"
                              />
                            </label>
                          </td>
                        </tr>

                        {/* Items in section */}
                        {isExpanded && components.map((item) => (
                          <tr key={item}>
                            <td style={{ paddingLeft: '24px' }}>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <span>{getDisplayName(item)}</span>
                                {autoSelectedItems[item] && (
                                  <span style={{
                                    fontSize: '11px', padding: '2px 8px', background: '#E6F4FF',
                                    color: '#0052CC', borderRadius: '4px', fontWeight: '500', border: '1px solid #B3D9FF'
                                  }} title={autoSelectedItems[item]}>
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
                                  style={{ cursor: 'pointer' }}
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

            <div className="flex justify-end">
              <ActionButton label="Update" kind="primary" onClick={handleSubmit} />
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
    </>
  );
};

export default UpdateRole;
