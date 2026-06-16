/**
 * Permission utility functions for RBAC
 */

import { isSandboxMode } from './sandboxMode';

/* ─────────────────────────────────────────────────────────────────────────────
 * componentId → sidenav route mapping
 * When the backend returns permissions with componentId but no componentUrl,
 * this map is used as a fallback to determine which routes a user can access.
 * Values can be a single route string OR an array of routes (for umbrella
 * modules like MONITORING that unlock several pages).
 * ───────────────────────────────────────────────────────────────────────────── */
const COMPONENT_ID_TO_ROUTES = {
  // Dashboard
  'DASHBOARD': '/dpo-dashboard',
  'DPO_DASHBOARD': '/dpo-dashboard',

  // System Configuration
  'SYSTEM_CONFIGURATION': '/systemConfiguration',
  'SYSTEM_SETUP': '/systemConfiguration',
  'SYSTEM_CONFIG': '/systemConfiguration',
  'MASTER': '/master',
  'MASTER_DATA': '/master',
  'MASTER_SETUP': '/master',
  'RETENTION_POLICIES': '/retention-policies',
  'RETENTION': '/retention-policies',
  'USER_DASHBOARD': '/user-dashboard',
  'BRANDING': '/user-dashboard',

  // Consent
  'CONSENT_TEMPLATES': '/templates',
  'TEMPLATES': '/templates',
  'CONSENT': '/consent',
  'CONSENT_SETUP': '/consent',
  'CONSENT_CONFIG': '/consent',
  'EMAIL_TEMPLATE': '/templates',      // grouped under consent
  'CONSENT_EMAIL_TEMPLATE': '/templates',
  'SMS_TEMPLATE': '/templates',
  'CONSENT_SMS_TEMPLATE': '/templates',
  'CONSENT_LOGS': '/templates',

  // Module-level: Systems & Datasets (inside Consent section in sidenav)
  'SYSTEM': '/consent-system',
  'SYSTEMS': '/consent-system',
  'DATASET': '/consent-datasets',
  'DATASETS': '/consent-datasets',

  // Cookies
  'REGISTERED_COOKIES': '/registercookies',
  'COOKIES': '/registercookies',
  'COOKIE_TEMPLATES': '/registercookies',
  'COOKIES_LOGS': '/cookieslogs',
  'COOKIE_LOGS': '/cookieslogs',
  'COOKIE_CATEGORY': '/cookie-category',
  'COOKIE_CATEGORIES': '/cookie-category',
  'MANAGE_CATEGORIES': '/cookie-category',

  // Governance
  'DATA_PROTECTION_OFFICER': '/dataProtectionOfficer',
  'DPO': '/dataProtectionOfficer',
  'ROPA': '/ropa',
  'RECORD_OF_PROCESSING_ACTIVITIES': '/ropa',
  'AUDIT_COMPLIANCE': '/audit-compliance',
  'AUDIT_AND_COMPLIANCE': '/audit-compliance',
  'AUDIT': '/audit-compliance',
  'BREACH_NOTIFICATIONS': '/breach-notifications',
  'BREACH': '/breach-notifications',
  'DATA_BREACH': '/breach-notifications',

  // Grievance Redressal
  'GRIEVANCE': '/grievance',
  'GRIEVANCE_SETUP': '/grievance',
  'GRIEVANCE_CONFIG': '/grievance',
  'GRIEVANCE_FORM': '/grievanceFormTemplates',
  'GRIEVANCE_FORM_TEMPLATES': '/grievanceFormTemplates',
  'FORM': '/grievanceFormTemplates',
  'FORMS': '/grievanceFormTemplates',
  'REQUESTS': '/request',
  'GRIEVANCE_REQUESTS': '/request',
  'REQUEST': '/request',
  'PENDING_REQUESTS': '/request',
  'GRIEVANCE_EMAIL_TEMPLATE': '/grievance',
  'GRIEVANCE_EMAIL': '/grievance',
  'GRIEVANCE_SMS_TEMPLATE': '/grievance',
  'GRIEVANCE_SMS': '/grievance',
  'GRIEVANCE_LOGS': '/grievance',

  // Administration
  'BUSINESS_GROUPS': '/business',
  'BUSINESS': '/business',
  'ROLES': '/roles',
  'ROLE': '/roles',
  'USERS': '/users',
  'USER': '/users',
  'ORGANIZATION_MAP': '/organization-map',
  'ORG_MAP': '/organization-map',

  // Notifications
  'NOTIFICATION': '/notification',
  'NOTIFICATION_CONFIG': '/notification',
  'NOTIFICATION_TEMPLATES': '/notification-templates',
  'NOTIFICATION_TEMPLATE': '/notification-templates',

  // Monitoring – umbrella module unlocks all 3 monitoring pages
  'MONITORING': ['/scheduler-stats', '/data-compliance-report', '/data-deletion-purge-dashboard'],
  // Individual monitoring pages
  'SCHEDULER_STATS': '/scheduler-stats',
  'SCHEDULER': '/scheduler-stats',
  'DATA_COMPLIANCE_REPORT': '/data-compliance-report',
  'COMPLIANCE_REPORT': '/data-compliance-report',
  'DATA_DELETION_PURGE_DASHBOARD': '/data-deletion-purge-dashboard',
  'DATA_DELETION_PURGE': '/data-deletion-purge-dashboard',
};

/**
 * Check if user has LEGAL_ADMIN role (full access)
 * @param {string} userRole - The user's role from Redux
 * @returns {boolean}
 */
export const isLegalAdmin = (userRole) => {
  // Grant full access in sandbox mode
  if (isSandboxMode()) {
    return true;
  }
  return userRole === 'LEGAL_ADMIN';
};

/**
 * Check if user has full admin access (LEGAL_ADMIN or DPO)
 * DPO (DATA_PROTECTION_OFFICER) should have same access as LEGAL_ADMIN
 * @param {string} userRole - The user's role from Redux
 * @returns {boolean}
 */
export const hasFullAccess = (userRole) => {
  // Grant full access in sandbox mode
  if (isSandboxMode()) {
    return true;
  }
  
  // Check for LEGAL_ADMIN or DPO (both short and long form)
  return userRole === 'LEGAL_ADMIN' || 
         userRole === 'DPO' || 
         userRole === 'DATA_PROTECTION_OFFICER';
};

/**
 * Check if user has permission to access a specific route
 * @param {Array} permissions - Array of permission objects from Redux
 * @param {string} route - The route path (e.g., '/dashboard', '/consent')
 * @param {string} access - The access type (e.g., 'READ', 'WRITE') - defaults to 'YES'
 * @returns {boolean}
 */
export const hasPermission = (permissions, route, access = 'YES') => {
  // Grant all permissions in sandbox mode
  if (isSandboxMode()) {
    return true;
  }
  
  // Universal routes that all authenticated users can access
  const universalRoutes = ['/profile', '/user-dashboard'];
  if (universalRoutes.includes(route)) {
    return true;
  }
  
  if (!permissions || !Array.isArray(permissions)) {
    return false;
  }

  return permissions.some(permission => {
    // Check by componentUrl (if the backend provides it)
    const matchesByUrl = permission.componentUrl === route;

    // Fallback: check by componentId via the mapping table
    let matchesByComponentId = false;
    if (!matchesByUrl && permission.componentId) {
      const mapped = COMPONENT_ID_TO_ROUTES[permission.componentId];
      if (Array.isArray(mapped)) {
        matchesByComponentId = mapped.includes(route);
      } else if (typeof mapped === 'string') {
        matchesByComponentId = mapped === route;
      }
    }

    // Check access
    const hasAccess = permission.access && (
      typeof permission.access === 'string' 
        ? permission.access === access 
        : permission.access.includes(access)
    );

    return (matchesByUrl || matchesByComponentId) && hasAccess;
  });
};

/**
 * Get accessible routes based on user permissions
 * @param {string} userRole - The user's role
 * @param {Array} permissions - Array of permission objects
 * @returns {Array} - Array of accessible route paths
 */
export const getAccessibleRoutes = (userRole, permissions) => {
  // LEGAL_ADMIN and DPO have access to all routes
  if (hasFullAccess(userRole)) {
    return ['*']; // Wildcard means all routes
  }

  if (!permissions || !Array.isArray(permissions)) {
    return [];
  }

  const routes = new Set();

  permissions.forEach(permission => {
    // Check if permission grants access
    const hasAccess = permission.access && (
      typeof permission.access === 'string'
        ? permission.access === 'YES'
        : permission.access.includes('YES')
    );

    if (!hasAccess) return;

    // 1. Use componentUrl if provided by backend
    if (permission.componentUrl) {
      routes.add(permission.componentUrl);
    }

    // 2. Fallback: resolve via componentId → route mapping
    if (permission.componentId) {
      const mapped = COMPONENT_ID_TO_ROUTES[permission.componentId];
      if (Array.isArray(mapped)) {
        mapped.forEach(r => routes.add(r));
      } else if (typeof mapped === 'string') {
        routes.add(mapped);
      }
    }
  });

  return Array.from(routes);
};

/**
 * Check if user can access a route (LEGAL_ADMIN/DPO or has permission)
 * @param {string} userRole - The user's role
 * @param {Array} permissions - Array of permission objects
 * @param {string} route - The route path
 * @returns {boolean}
 */
export const canAccessRoute = (userRole, permissions, route) => {
  // LEGAL_ADMIN and DPO can access everything
  if (hasFullAccess(userRole)) {
    return true;
  }

  // Check specific permission
  return hasPermission(permissions, route);
};

/**
 * Filter menu items based on user permissions
 * @param {Array} menuItems - Array of menu item objects with 'path' property
 * @param {string} userRole - The user's role
 * @param {Array} permissions - Array of permission objects
 * @returns {Array} - Filtered menu items
 */
export const filterMenuByPermissions = (menuItems, userRole, permissions) => {
  // LEGAL_ADMIN and DPO see all menu items
  if (hasFullAccess(userRole)) {
    return menuItems;
  }

  if (!permissions || !Array.isArray(permissions)) {
    return [];
  }

  const accessibleRoutes = getAccessibleRoutes(userRole, permissions);

  // First pass: Filter out all non-heading items that are not accessible
  const filteredItems = menuItems.filter(item => {
    if (item.type === 'heading' || item.type === 'separator') {
      return true;
    }
    return accessibleRoutes.includes(item.path);
  });

  // Second pass: Filter out headings/separators that have no items until the next heading/separator
  const finalItems = [];
  for (let i = 0; i < filteredItems.length; i++) {
    const currentItem = filteredItems[i];
    
    if (currentItem.type === 'heading' || currentItem.type === 'separator') {
      // Look ahead to see if there are any non-heading items before the next heading
      let hasSubItems = false;
      for (let j = i + 1; j < filteredItems.length; j++) {
        if (filteredItems[j].type === 'heading' || filteredItems[j].type === 'separator') {
          break;
        }
        hasSubItems = true;
        break;
      }
      
      if (hasSubItems) {
        finalItems.push(currentItem);
      }
    } else {
      finalItems.push(currentItem);
    }
  }

  return finalItems;
};
