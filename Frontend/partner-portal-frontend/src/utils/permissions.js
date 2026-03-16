/**
 * Permission utility functions for RBAC
 */

import { isSandboxMode } from './sandboxMode';

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
 * @param {string} access - The access type (e.g., 'READ', 'WRITE') - defaults to 'READ'
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
    const matchesRoute = permission.componentUrl === route;
    // Handle both string and array formats for access
    const hasAccess = permission.access && (
      typeof permission.access === 'string' 
        ? permission.access === access 
        : permission.access.includes(access)
    );
    return matchesRoute && hasAccess;
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

  return permissions
    .filter(permission => {
      // Handle both string and array formats for access
      return permission.access && (
        typeof permission.access === 'string'
          ? permission.access === 'YES'
          : permission.access.includes('YES')
      );
    })
    .map(permission => permission.componentUrl);
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

