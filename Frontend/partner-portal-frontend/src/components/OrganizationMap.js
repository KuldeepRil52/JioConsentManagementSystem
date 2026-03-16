import React, { useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Text, Spinner, InputFieldV2, ActionButton } from '../custom-components';
import { IcSearch, IcUser } from '../custom-components/Icon';
import { IcGroup, IcNetwork, IcTeam } from '../custom-components/Icon';
import { Icon } from '../custom-components';
import { 
  listUsers, 
  listRoles, 
  searchBusiness, 
  getProcessor,
  getGrievances,
  getConsentCountsByTemplateId 
} from "../store/actions/CommonAction";
import { generateTransactionId } from "../utils/transactionId";
import config from "../utils/config";
import "../styles/organizationMap.css";
import "../styles/professionalTree.css";
import "../styles/organizationMapCompact.css";
import "../styles/organizationTreeExplorer.css";

const OrganizationMap = () => {
  const dispatch = useDispatch();
  const tenantId = useSelector((state) => state.common.tenant_id);
  const businessId = useSelector((state) => state.common.business_id);
  const token = useSelector((state) => state.common.session_token);
  
  const [loading, setLoading] = useState(true);
  const [orgData, setOrgData] = useState({
    businessGroups: [],
    legalEntity: null,
    childBusinesses: [],
    roles: [],
    users: [],
    processors: [],
    processingActivities: [],
    grievances: [],
    breaches: [],
    consents: [],
    rawData: {},
  });
  const [stats, setStats] = useState({
    totalBusinesses: 0,
    totalProcessors: 0,
    totalUsers: 0,
    totalRoles: 0,
  });
  
  // State for tree expansion - all closed by default
  const [expandedNodes, setExpandedNodes] = useState({
    legalEntity: false,
    businesses: {},
    processors: {},
    users: {},
    businessGroups: false
  });

  const toggleNode = (nodeType, nodeId) => {
    setExpandedNodes(prev => ({
      ...prev,
      [nodeType]: {
        ...prev[nodeType],
        [nodeId]: !prev[nodeType]?.[nodeId]
      }
    }));
  };

  // Helper function to get initials from name
  const getInitials = (name) => {
    if (!name) return "?";
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .substring(0, 2);
  };

  // Helper function to get avatar color based on name
  const getAvatarColor = (name) => {
    if (!name) return "#1976d2";
    
    const colors = [
      "#1976d2", // Blue
      "#7e57c2", // Purple
      "#43a047", // Green
      "#e91e63", // Pink
      "#ff6f00", // Orange
      "#0097a7", // Cyan
    ];
    
    // Generate a consistent color based on the name
    const hash = name.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    return colors[hash % colors.length];
  };

  useEffect(() => {
    fetchOrganizationData();
  }, [dispatch, tenantId, businessId, token]);

  const fetchOrganizationData = async () => {
    setLoading(true);
    try {
      console.log("🔍 Fetching organization data from dashboard API...");
      
      // Call the dashboard API
      const txnId = generateTransactionId();
      const response = await fetch(
        `${config.dashboard_data}`,
          {
            method: "GET",
            headers: {
            'accept': '*/*',
              'tenant-id': tenantId,
            'txn': txnId,
              'x-session-token': token,
            },
          }
        );

      if (!response.ok) {
        throw new Error(`Dashboard API failed with status: ${response.status}`);
      }

      const apiResponse = await response.json();
      console.log("📊 Dashboard API Response:", apiResponse);

      // Parse the new response structure
      const dashboardData = apiResponse?.dashboardData || {};
      
      // Convert the dashboard data into structured format
      const businessGroups = [];
      const allProcessors = [];
      const allUsers = [];
      const allRoles = new Map(); // Use Map to collect unique roles
      
      let legalEntity = null;
      const childBusinesses = [];
      
      // Process each business group in the dashboard data
      Object.entries(dashboardData).forEach(([businessName, businessData]) => {
        const { businessInfo, processors = [], users = [] } = businessData;
        
        // Create business group object
        const businessGroup = {
          businessId: businessInfo.businessId,
          name: businessInfo.name,
          description: businessInfo.description,
          scopeLevel: businessInfo.scopeLevel,
          processors: processors.map(p => ({
            ...p.processorInfo,
            processorName: p.processorName,
          })),
          users: users.map(u => {
            const userInfo = u.userInfo;
            const userRoles = Object.values(userInfo.roles || {});
            
            // Collect all unique roles
            userRoles.forEach(role => {
              if (!allRoles.has(role.roleId)) {
                allRoles.set(role.roleId, {
                  roleId: role.roleId,
                  role: role.role,
                  description: role.description,
                  businessId: role.businessId,
                });
              }
            });

          return {
              userId: u.userId,
              username: userInfo.username,
              email: userInfo.email,
              mobile: userInfo.mobile,
              designation: userInfo.designation,
              identityType: userInfo.identityType,
              roles: userRoles.map(role => ({
                roleId: role.roleId,
                roleName: role.role,
                businessId: role.businessId,
                businessName: businessInfo.name,
                permissions: role.permissions || [],
              })),
            };
          }),
        };
        
        businessGroups.push(businessGroup);
        allProcessors.push(...businessGroup.processors);
        allUsers.push(...businessGroup.users);
        
        // Separate legal entity from child businesses
        if (businessInfo.scopeLevel === 'TENANT') {
          legalEntity = businessGroup;
        } else {
          childBusinesses.push(businessGroup);
        }
      });
      
      console.log("📊 Parsed Dashboard Data:", {
        businessGroups: businessGroups.length,
        legalEntity: legalEntity?.name,
        childBusinesses: childBusinesses.length,
        totalProcessors: allProcessors.length,
        totalUsers: allUsers.length,
        totalRoles: allRoles.size,
      });

      // Calculate statistics
      const finalStats = {
        totalBusinesses: businessGroups.length,
        totalProcessors: allProcessors.length,
        totalUsers: allUsers.length,
        totalRoles: allRoles.size,
      };

      const finalOrgData = {
        businessGroups: businessGroups,
        legalEntity: legalEntity,
        childBusinesses: childBusinesses,
        roles: Array.from(allRoles.values()),
        users: allUsers,
        processors: allProcessors,
        rawData: dashboardData, // Keep raw data for reference
      };

      console.log("✅ Final Organization Data:", {
        businessGroups: finalOrgData.businessGroups.length,
        legalEntity: finalOrgData.legalEntity?.name,
        childBusinesses: finalOrgData.childBusinesses.length,
        roles: finalOrgData.roles.length,
        users: finalOrgData.users.length,
        processors: finalOrgData.processors.length,
        stats: finalStats,
      });

      console.log("📝 Setting orgData state with:", {
        legalEntity: finalOrgData.legalEntity,
        childBusinessesCount: finalOrgData.childBusinesses.length,
        childBusinesses: finalOrgData.childBusinesses.map(b => ({ name: b.name, id: b.businessId })),
      });

      setStats(finalStats);
      setOrgData(finalOrgData);
      
      console.log("✅ State updated successfully");
    } catch (error) {
      console.error("❌ Failed to fetch organization data:", error);
      
      // Fallback: Try to fetch individual data if dashboard API fails
      console.log("⚠️ Attempting fallback data fetch...");
      try {
        const [rolesData, businessData, usersData, processorsData] = await Promise.all([
          dispatch(listRoles()),
          dispatch(searchBusiness()),
          dispatch(listUsers()),
          dispatch(getProcessor()),
        ]);

        const processedUsers = (usersData || []).map((user) => ({
          ...user,
          roles: (user.roles || []).map((role) => ({
            ...role,
            roleName: rolesData?.find(r => r.roleId === role.roleId)?.role || 'Unknown Role',
            businessName: businessData?.searchList?.find(b => b.businessId === role.businessId)?.name || 'Unknown Business',
          })),
        }));

        setOrgData({
          businessGroups: businessData?.searchList || [],
          legalEntity: businessData?.searchList?.[0] || null,
          childBusinesses: businessData?.searchList?.slice(1) || [],
          roles: rolesData || [],
          users: processedUsers,
          processors: processorsData?.data?.searchList || [],
        });

        setStats({
          totalBusinesses: businessData?.searchList?.length || 0,
          totalProcessors: processorsData?.data?.searchList?.length || 0,
          totalUsers: processedUsers.length,
          totalRoles: rolesData?.length || 0,
        });

        console.log("✅ Fallback data loaded");
      } catch (fallbackError) {
        console.error("❌ Fallback fetch also failed:", fallbackError);
      }
    } finally {
      setLoading(false);
    }
  };

  const getUsersByBusinessAndRole = (businessId, roleId) => {
    return orgData.users.filter(user => 
      user.roles.some(r => r.businessId === businessId && r.roleId === roleId)
    );
  };

  const getUserCountByBusiness = (businessId) => {
    return orgData.users.filter(user =>
      user.roles.some(r => r.businessId === businessId)
    ).length;
  };

  const getUserCountByRole = (roleId) => {
    return orgData.users.filter(user =>
      user.roles.some(r => r.roleId === roleId)
    ).length;
  };


  if (loading) {
    return (
      <div className="configurePage">
        <div className="org-map-loading">
          <Spinner size="large" />
          <Text appearance="body-m" color="primary-grey-60">
            Loading organization map...
          </Text>
        </div>
      </div>
    );
  }

  return (
    <div className="configurePage">
      <div className="org-map-container">
        {/* Header */}
        <div className="org-map-header-simple">
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <Text appearance="heading-s" color="primary-grey-100">
                Organization Structure
              </Text>
              <Text appearance="body-s" color="primary-grey-60">
                Business hierarchy and data processors
              </Text>
              {/* <div style={{ display: 'flex', gap: '16px', marginTop: '8px', flexWrap: 'wrap' }}>
                <Text appearance="body-s" color="primary-grey-80">
                  <strong>{stats.totalBusinesses || orgData.businessGroups?.length || 0}</strong> {(stats.totalBusinesses || orgData.businessGroups?.length || 0) === 1 ? 'business' : 'businesses'}
                </Text>
                <Text appearance="body-s" color="primary-grey-80">
                  <strong>{stats.totalProcessors || orgData.processors?.length || 0}</strong> {(stats.totalProcessors || orgData.processors?.length || 0) === 1 ? 'processor' : 'processors'}
                </Text>
                <Text appearance="body-s" color="primary-grey-80">
                  <strong>{stats.totalUsers || orgData.users?.length || 0}</strong> {(stats.totalUsers || orgData.users?.length || 0) === 1 ? 'user' : 'users'}
                </Text>
              </div> */}
            </div>
        </div>

        {/* Organization Content */}
        <div className="org-map-content-simple">
          {!orgData.legalEntity && orgData.businessGroups.length === 0 ? (
                <div className="empty-state">
                  <Icon ic={<IcGroup />} size="xlarge" color="primary-grey-40" />
                  <Text appearance="heading-s" color="primary-grey-80">
                    No Organization Data Available
                  </Text>
                  <Text appearance="body-m" color="primary-grey-60">
                    No business groups, roles, or users found in the system.
                  </Text>
                </div>
              ) : (
            <div className="org-tree-explorer">
              {/* Tree View Structure */}
                  {orgData.legalEntity && (
                <div className="tree-container">
                  {/* Root Node - Legal Entity */}
                  <div className="tree-node root-node">
                    <div 
                      className="tree-node-header"
                      onClick={() => setExpandedNodes(prev => ({ ...prev, legalEntity: !prev.legalEntity }))}
                    >
                      <span className="tree-expand-icon">
                        {expandedNodes.legalEntity ? '▼' : '▶'}
                      </span>
                      <Icon ic={<IcGroup />} size="sm" color="primary-blue-100" />
                            <Text appearance="body-m-bold" color="primary-grey-100">
                              {orgData.legalEntity.name}
                            </Text>
                      <span className="tree-badge tenant">ORGANIZATION</span>
                      <Text appearance="body-xs" color="primary-grey-60" style={{ marginLeft: 'auto' }}>
                        {orgData.childBusinesses?.length || 0} {(orgData.childBusinesses?.length || 0) === 1 ? 'business' : 'businesses'}, {orgData.legalEntity.processors?.length || 0} processors, {orgData.legalEntity.users?.length || 0} users
                            </Text>
                          </div>

                    {expandedNodes.legalEntity && (
                      <div className="tree-node-children">
                        {/* Processors */}
                        {orgData.legalEntity.processors && orgData.legalEntity.processors.length > 0 && (
                          <div className="tree-node">
                            <div 
                              className="tree-node-header"
                              onClick={() => toggleNode('processors', 'legal-entity')}
                            >
                              <span className="tree-expand-icon">
                                {expandedNodes.processors?.['legal-entity'] ? '▼' : '▶'}
                              </span>
                              <Icon ic={<IcNetwork />} size="sm" color="primary-grey-60" />
                              <Text appearance="body-s-bold" color="primary-grey-80">
                                Data Processors
                              </Text>
                              <span className="tree-count">({orgData.legalEntity.processors.length})</span>
                        </div>
                        
                            {expandedNodes.processors?.['legal-entity'] && (
                              <div className="tree-node-children">
                                {orgData.legalEntity.processors.map((processor) => (
                                  <div key={processor.dataProcessorId} className="tree-node leaf-node">
                                    <div className="tree-node-header">
                                      <span className="tree-expand-icon-empty"></span>
                                      <Icon ic={<IcNetwork />} size="sm" color="primary-grey-40" />
                                      <Text appearance="body-s" color="primary-grey-100">
                                        {processor.dataProcessorName || processor.processorName}
                                            </Text>
                                      <span className="tree-badge active">ACTIVE</span>
                                        </div>
                                      </div>
                                    ))}
                                </div>
                              )}
                            </div>
                          )}
                          
                        {/* Users */}
                        {orgData.legalEntity.users && orgData.legalEntity.users.length > 0 && (
                          <div className="tree-node">
                            <div 
                              className="tree-node-header"
                              onClick={() => toggleNode('users', 'legal-entity')}
                            >
                              <span className="tree-expand-icon">
                                {expandedNodes.users?.['legal-entity'] ? '▼' : '▶'}
                              </span>
                              <Icon ic={<IcTeam />} size="sm" color="primary-grey-60" />
                              <Text appearance="body-s-bold" color="primary-grey-80">
                                Users
                                        </Text>
                              <span className="tree-count">({orgData.legalEntity.users.length})</span>
                                    </div>
                                    
                            {expandedNodes.users?.['legal-entity'] && (
                              <div className="tree-node-children">
                                {orgData.legalEntity.users.map((user) => (
                                  <div key={user.userId} className="tree-node leaf-node">
                                    <div className="tree-node-header">
                                      <span className="tree-expand-icon-empty"></span>
                                      <div className="tree-user-avatar" style={{ backgroundColor: getAvatarColor(user.username) }}>
                                        {getInitials(user.username)}
                                      </div>
                                      <div className="tree-user-info">
                                        <Text appearance="body-s" color="primary-grey-100">
                                          {user.username}
                                        </Text>
                                        <Text appearance="body-xs" color="primary-grey-60">
                                          {(user.email || user.mobile || '').toLowerCase()}
                                        </Text>
                                      </div>
                                      <div className="tree-user-roles">
                                        {user.roles && user.roles.map((role, idx) => (
                                          <span key={idx} className="tree-badge role">
                                            {role.roleName}
                                          </span>
                                        ))}
                                      </div>
                                    </div>
                                  </div>
                                ))}
                              </div>
                            )}
                          </div>
                        )}

                        {/* Child Businesses */}
                        {orgData.childBusinesses && orgData.childBusinesses.length > 0 && (
                          <div className="tree-node">
                            <div 
                              className="tree-node-header"
                              onClick={() => setExpandedNodes(prev => ({ ...prev, businessGroups: !prev.businessGroups }))}
                            >
                              <span className="tree-expand-icon">
                                {expandedNodes.businessGroups ? '▼' : '▶'}
                              </span>
                              <Icon ic={<IcGroup />} size="sm" color="primary-grey-60" />
                              <Text appearance="body-s-bold" color="primary-grey-80">
                                Business Groups
                              </Text>
                              <span className="tree-count">({orgData.childBusinesses.length})</span>
                            </div>

                            {expandedNodes.businessGroups && (
                              <div className="tree-node-children">
                              {orgData.childBusinesses.map((business) => (
                                <div key={business.businessId} className="tree-node">
                                  <div 
                                    className="tree-node-header"
                                    onClick={() => toggleNode('businesses', business.businessId)}
                                  >
                                    <span className="tree-expand-icon">
                                      {expandedNodes.businesses?.[business.businessId] ? '▼' : '▶'}
                                    </span>
                                    <Icon ic={<IcGroup />} size="sm" color="primary-purple-80" />
                                    <Text appearance="body-s-bold" color="primary-grey-100">
                                      {business.name}
                                    </Text>
                                    <span className="tree-badge business">BUSINESS</span>
                                    <Text appearance="body-xs" color="primary-grey-60" style={{ marginLeft: 'auto' }}>
                                      0 businesses, {business.processors?.length || 0} processors, {business.users?.length || 0} users
                                    </Text>
                                  </div>

                                  {expandedNodes.businesses?.[business.businessId] && (
                                    <div className="tree-node-children">
                                      {/* Business Processors */}
                                      {business.processors && business.processors.length > 0 && (
                                        <div className="tree-node">
                                          <div 
                                            className="tree-node-header"
                                            onClick={() => toggleNode('processors', business.businessId)}
                                          >
                                            <span className="tree-expand-icon">
                                              {expandedNodes.processors?.[business.businessId] ? '▼' : '▶'}
                                            </span>
                                            <Icon ic={<IcNetwork />} size="sm" color="primary-grey-60" />
                                            <Text appearance="body-s-bold" color="primary-grey-80">
                                              Data Processors
                                            </Text>
                                            <span className="tree-count">({business.processors.length})</span>
                                          </div>

                                          {expandedNodes.processors?.[business.businessId] && (
                                            <div className="tree-node-children">
                                              {business.processors.map((processor) => (
                                                <div key={processor.dataProcessorId} className="tree-node leaf-node">
                                                  <div className="tree-node-header">
                                                    <span className="tree-expand-icon-empty"></span>
                                                    <Icon ic={<IcNetwork />} size="sm" color="primary-grey-40" />
                          <Text appearance="body-s" color="primary-grey-100">
                                                        {processor.dataProcessorName || processor.processorName}
                                                      </Text>
                                                    <span className="tree-badge active">ACTIVE</span>
                                                  </div>
                                                </div>
                                              ))}
                                            </div>
                                          )}
                                        </div>
                                      )}

                                      {/* Business Users */}
                                      {business.users && business.users.length > 0 && (
                                        <div className="tree-node">
                                          <div 
                                            className="tree-node-header"
                                            onClick={() => toggleNode('users', business.businessId)}
                                          >
                                            <span className="tree-expand-icon">
                                              {expandedNodes.users?.[business.businessId] ? '▼' : '▶'}
                                            </span>
                                            <Icon ic={<IcTeam />} size="sm" color="primary-grey-60" />
                                            <Text appearance="body-s-bold" color="primary-grey-80">
                                              Users
                                            </Text>
                                            <span className="tree-count">({business.users.length})</span>
                                          </div>

                                          {expandedNodes.users?.[business.businessId] && (
                                            <div className="tree-node-children">
                                              {business.users.map((user) => (
                                                <div key={user.userId} className="tree-node leaf-node">
                                                  <div className="tree-node-header">
                                                    <span className="tree-expand-icon-empty"></span>
                                                    <div className="tree-user-avatar" style={{ backgroundColor: getAvatarColor(user.username) }}>
                                                      {getInitials(user.username)}
                                                    </div>
                                                    <div className="tree-user-info">
                                                      <Text appearance="body-s" color="primary-grey-100">
                                                        {user.username}
                                                      </Text>
                              <Text appearance="body-xs" color="primary-grey-60">
                                                          {(user.email || user.mobile || '').toLowerCase()}
                                                        {user.designation && ` • ${user.designation}`}
                                                        </Text>
                                                      </div>
                                                    <div className="tree-user-roles">
                                                      {user.roles && user.roles.map((role, idx) => (
                                                        <span key={idx} className="tree-badge role">
                                                          {role.roleName}
                                                        </span>
                                                      ))}
                                                    </div>
                                                  </div>
                                                </div>
                                              ))}
                                          </div>
                                        )}
                                      </div>
                                    )}
                                  </div>
                                  )}
                                    </div>
                                  ))}
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default OrganizationMap;
