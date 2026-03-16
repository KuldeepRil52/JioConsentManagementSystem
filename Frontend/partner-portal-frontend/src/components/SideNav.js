import React, { lazy, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { Icon, Text } from "../custom-components";
import { IcHome, IcNotification } from "../custom-components/Icon";
import { IcHome, IcNotification, IcChevronDown, IcChevronUp, IcTime, IcSettings } from "../custom-components/Icon";
import { useSelector, useDispatch } from "react-redux";
import { SET_SELECTED_BUSINESS, UPDATE_BUSINESS_ID } from "../store/constants/Constants";
import { filterMenuByPermissions } from "../utils/permissions";
import "../styles/sideNav.css"; // Assuming you have a CSS file for styling
import {
  IcBusinessman,
  IcCookies,
  IcDocument,
  IcEngineeringRequest,
  IcGroup,
  IcLayout,
  IcMail,
  IcNetwork,
  IcPageSettings,
  IcRequest,
  IcSms,
  IcStatusSuccessful,
  IcTeam,
  IcTicketDetails,
  IcRequest,
  IcForms,
  IcMail
} from "../custom-components/Icon";
import { IcImage } from "../custom-components/Icon";

const SideNav = ({ isOpen }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();

  const businesses = useSelector((state) => state.common.businesses);
  const selectedBusiness = useSelector((state) => state.common.selectedBusiness);
  const userRole = useSelector((state) => state.common.userRole);
  const permissions = useSelector((state) => state.common.permissions);
  const userOriginalBusinessId = useSelector((state) => state.common.user_original_business_id);
  const tenantId = useSelector((state) => state.common.tenant_id);

  const path = location.pathname;

  // State to manage which accordion sections are open (all closed by default)
  const [openSections, setOpenSections] = useState({
    "System Configuration": false,
    "Consent": false,
    "Cookies": false,
    "Governance": false,
    "Grievance Redressal": false,
    "Administration": false,
    "Notifications": false,
  });

  // Toggle accordion section - closes all others when opening one
  const toggleSection = (sectionLabel) => {
    setOpenSections(prev => {
      const isCurrentlyOpen = prev[sectionLabel];
      
      // Close all sections first
      const allClosed = {
        "System Configuration": false,
        "Consent": false,
        "Cookies": false,
        "Governance": false,
        "Grievance Redressal": false,
        "Administration": false,
        "Notifications": false,
      };
      
      // If the clicked section was closed, open it. If it was open, keep it closed.
      return {
        ...allClosed,
        [sectionLabel]: !isCurrentlyOpen
      };
    });
  };

  // Filter business groups based on user type
  // If user_original_business_id === tenant_id, user is super admin and sees all businesses
  // Otherwise, user only sees their own business
  const filteredBusinesses = React.useMemo(() => {
    console.log("SideNav - userOriginalBusinessId:", userOriginalBusinessId);
    console.log("SideNav - tenantId:", tenantId);
    console.log("SideNav - all businesses:", businesses);

    if (!businesses || businesses.length === 0) return [];

    const isSuperAdmin = userOriginalBusinessId === tenantId;
    console.log("SideNav - isSuperAdmin:", isSuperAdmin);

    if (isSuperAdmin) {
      console.log("SideNav - Super Admin: showing all businesses");
      return businesses;
    } else {
      console.log("SideNav - Regular user: filtering to own business");
      const filtered = businesses.filter(b => b.businessId === userOriginalBusinessId);
      console.log("SideNav - filtered businesses:", filtered);
      return filtered;
    }
  }, [businesses, userOriginalBusinessId, tenantId]);

  const handleBusinessChange = (e) => {
    const selectedId = e.target.value;

    console.log("selectedId: ", selectedId);

    dispatch({
      type: SET_SELECTED_BUSINESS,
      payload: selectedId,
    });

    dispatch({ type: UPDATE_BUSINESS_ID, payload: selectedId });

  };

  const menuItems = [
    {
      label: "Dashboard",
      icon: <IcHome height={20} width={20} />,
      path: "/dpo-dashboard",
    },

    { type: "heading", label: "System Configuration", icon: <IcSettings height={20} width={20} />},
    {
      label: "Configuration",
      // icon: <IcEngineeringRequest height={23} width={23} />,
      path: "/systemConfiguration",
    },
    {
      label: "Master Data",
      // icon: <IcPageSettings height={23} width={23} />,
      path: "/master",
    },
    {
      label: "Retention Policies",
      // icon: <IcTheme height={23} width={23} />,
      path: "/retention-policies",
    },
    {
      label: "User Portal Branding",
      // icon: <IcImage height={23} width={23}></IcImage>,
      path: "/user-dashboard",
    },

    { type: "heading", label: "Consent", icon: <IcStatusSuccessful height={20} width={20} /> },
    {
      label: "Templates",
      // icon: <IcStatusSuccessful height={23} width={23}></IcStatusSuccessful>,
      path: "/templates",
    },
   
    {
      label: "Configuration",
      // icon: <IcStatusSuccessful height={23} width={23} />,
      path: "/consent",
    },
    {
      label: "Systems",
      icon: <IcStatusSuccessful height={23} width={23} />,
      path: "/consent-system",
    },
    {
      label: "Datasets",
      icon: <IcStatusSuccessful height={23} width={23} />,
      path: "/consent-datasets",
    },

    { type: "heading", label: "Cookies", icon: <IcCookies height={20} width={20} /> },
    {
      label: "Templates",
      // icon: <IcCookies height={23} width={23} />,
      path: "/registercookies",
    },
    {
      label: "Cookies Logs",
      // icon: <IcTicketDetails height={23} width={23} />,
      path: "/cookieslogs",
    },
    {
      label: "Manage categories",
      // icon: <IcCookies height={23} width={23}></IcCookies>,
      path: "/cookie-category",
    },

    { type: "heading", label: "Governance", icon: <IcLayout height={20} width={20} /> },
    {
      label: "Data protection officer",
      // icon: <IcBusinessman height={23} width={23} />,
      path: "/dataProtectionOfficer",
    },
    {
      label: "ROPA",
      // icon: <IcTicketDetails height={23} width={23} />,
      path: "/ropa",
    },
    // {
    //   label: "DPO Dashboard",
    //   icon: <IcTicketDetails height={23} width={23} />,
    //   path: "/dpo-dashboard",
    // },
    {
      label: "Audit & Compliance Report",
      // icon: <IcTicketDetails height={23} width={23} />,
      path: "/audit-compliance",
    },
    {
      label: "Breach Notifications",
      // icon: <IcTicketDetails height={23} width={23} />,
      path: "/breach-notifications",
    },
    // {
    //   label: "Consent Report",
    //   icon: <IcTicketDetails height={23} width={23} />,
    //   path: "/consent-report",
    // },

    { type: "heading", label: "Grievance Redressal", icon: <IcDocument height={20} width={20} /> },
    {
      label: "Configuration",
      // icon: <IcRequest height={23} width={23} />,
      path: "/grievance",
    },
    {
      label: "Grievance Form",
      // icon: <IcForms height={23} width={23}></IcForms>,
      path: "/grievanceFormTemplates",
    },
    {
      label: "Requests",
      // icon: <IcRequest height={23} width={23}></IcRequest>,
      path: "/request",
    },
    

    { type: "heading", label: "Administration", icon: <IcBusinessman height={20} width={20} /> },
    
    {
      label: "Business groups",
      // icon: <IcGroup height={23} width={23}></IcGroup>,
      path: "/business",
    },
    {
      label: "Roles",
      // icon: <IcNetwork height={23} width={23}></IcNetwork>,
      path: "/roles",
    },
    {
      label: "Users",
      // icon: <IcTeam height={23} width={23}></IcTeam>,
      path: "/users",
    },
    {
      label: "Organization Map",
      // icon: <IcNetwork height={23} width={23}></IcNetwork>,
      path: "/organization-map",
    },
    
    { type: "heading", label: "Notifications", icon: <IcNotification height={20} width={20} /> },
    {
      label: "Configuration",
      // icon: <IcNotification height={23} width={23} />,
      path: "/notification",
    },
    {
      label: "Notification Templates",
      // icon: <IcMail height={23} width={23}></IcMail>,
      path: "/notification-templates",
    },
    { type: "heading", label: "Monitoring", icon: <IcNetwork height={20} width={20} /> },
    {
      label: "Scheduler Stats",
      // icon: <IcTime height={23} width={23}></IcTime>,
      path: "/scheduler-stats",
    },
    {
      label: "Data Compliance Report",
      // icon: <IcDocument height={23} width={23}></IcDocument>,
      path: "/data-compliance-report",
    },
    {
      label: "Data deletion and purge dashboard",
      icon: <IcDocument height={23} width={23}></IcDocument>,
      path: "/data-deletion-purge-dashboard",
    },
    
    { type: "separator" }, // Separator to make User Dashboard standalone
    
    
   
  ];

  // Filter menu items based on user permissions
  const filteredMenuItems = filterMenuByPermissions(menuItems, userRole, permissions);

  const handleNavigation = (path) => {
    navigate(path);
  };

  // Hide SideNav on home/login/signup + cookie report pages
  if (
    path === "/" ||
    path === "/adminLogin" ||
    path === "/signup" ||
    path === "/contact" ||
    path.startsWith("/registercookies/report")
  ) {
    return null;
  } else {
    return (
      <div className={`sideBar-outer-container ${isOpen ? 'show' : ''}`} data-tour="sidebar-menu">
        <div>
          <div className="dropdown-group" style={{ marginBottom: "20px" }}>
            <Text appearance="body-xxs" color="primary-grey-80">
              BUSINESS GROUP
            </Text>
            {/* <select defaultValue="All" id="grievance-type">
              <option value="All" disabled>
                All
              </option>
            </select> */}
            <select
        id="business-dropdown"
        value={selectedBusiness || ""}
        onChange={handleBusinessChange}
        className="border border-gray-300 rounded-md px-2 py-1 mt-2"
      >
        <option value="" disabled>
          Select a business
        </option>

        {/* Dynamically populate from Redux - filtered by user role */}
        {filteredBusinesses?.length > 0 &&
          filteredBusinesses.map((b) => (
            <option key={b.businessId} value={b.businessId}>
              {b.name}
            </option>
          ))}
      </select>
          </div>

          {(() => {
            let currentSection = null;
            let sectionItems = [];
            const result = [];

            filteredMenuItems.forEach((item, index) => {
              if (item.type === "heading" || item.type === "separator") {
                // If we have accumulated items from previous section, render them
                if (currentSection === null && sectionItems.length > 0) {
                  // Render standalone items (like Dashboard) that appear before any section
                  sectionItems.forEach((menuItem) => {
                    const isActive = location.pathname === menuItem.path;
                    result.push(
                      <div
                        key={menuItem.path}
                        className={`sideBar-menu-click ${isActive ? "active" : "inactive"}`}
                        onClick={() => handleNavigation(menuItem.path)}
                      >
                        <Icon
                          kind={isActive ? "background-bold" : "default"}
                          ic={menuItem.icon}
                          color={isActive ? "primary_60" : "primary_grey_80"}
                          size="small"
                        />
                        <Text
                          appearance="body-xs-bold"
                          color={isActive ? "primary_60" : "primary-grey-80"}
                          style={{ marginLeft: "12px" }}
                        >
                          {menuItem.label}
                        </Text>
                      </div>
                    );
                  });
                } else if (currentSection && sectionItems.length > 0) {
                  // Render accordion section
                  const sectionLabel = currentSection.label;
                  const sectionIcon = currentSection.icon;
                  const items = [...sectionItems];
                  const isOpen = openSections[sectionLabel];
                  
                  result.push(
                    <div key={`section-${sectionLabel}`} className="sideBar-accordion-section">
                      <div 
                        className="sideBar-accordion-header"
                        onClick={() => toggleSection(sectionLabel)}
                        data-tour={`menu-${sectionLabel.toLowerCase().replace(/ /g, '-')}`}
                      >
                        {sectionIcon && (
                          <Icon
                            kind="default"
                            ic={sectionIcon}
                            color="primary_grey_80"
                          size="small"
                          style={{ marginRight: "12px" }}
                          />
                        )}
                        <Text
                          appearance="body-xs-bold"
                          color="primary-grey-80"
                          className="sideBar-menu-item"
                        >
                          {sectionLabel}
                        </Text>
                        <span className="sideBar-accordion-icon">
                          {isOpen ? (
                            <IcChevronUp size="small" />
                          ) : (
                            <IcChevronDown size="small" />
                          )}
                        </span>
                      </div>
                      {isOpen && (
                        <div className="sideBar-accordion-content">
                          {items.map((menuItem) => {
                            const isActive = location.pathname === menuItem.path;
                            return (
                              <div
                                key={menuItem.path}
                                className={`sideBar-menu-click ${isActive ? "active" : "inactive"}`}
                                onClick={() => handleNavigation(menuItem.path)}
                              >
                                {/* <Icon
                                  kind={isActive ? "background-bold" : "default"}
                                  ic={menuItem.icon}
                                  color={isActive ? "primary_60" : "primary_grey_80"}
                                  size="medium"
                                /> */}
                                <Text
                                  appearance="body-xs-bold"
                                  color={isActive ? "primary_60" : "primary-grey-80"}
                                  style={{ marginLeft: "8px" }}
                                >
                                  {menuItem.label}
                                </Text>
                              </div>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  );
                }
                
                // Start new section or reset for separator
                if (item.type === "heading") {
                  currentSection = item; // Store the whole item to access icon later
                } else if (item.type === "separator") {
                  currentSection = null; // Reset to make following items standalone
                }
                sectionItems = [];
              } else {
                // Add item to current section
                sectionItems.push(item);
              }

              // Handle last section or standalone items at the end
              if (index === filteredMenuItems.length - 1) {
                if (currentSection && sectionItems.length > 0) {
                  // Render as accordion section
                  const sectionLabel = currentSection.label;
                  const sectionIcon = currentSection.icon;
                  const items = [...sectionItems];
                  const isOpen = openSections[sectionLabel];
                  
                  result.push(
                    <div key={`section-${sectionLabel}`} className="sideBar-accordion-section">
                      <div 
                        className="sideBar-accordion-header"
                        onClick={() => toggleSection(sectionLabel)}
                      >
                        {sectionIcon && (
                          <Icon
                            kind="default"
                            ic={sectionIcon}
                            color="primary_grey_80"
                          size="small"
                          style={{ marginRight: "12px" }}
                          />
                        )}
                        <Text
                          appearance="body-xs-bold"
                          color="primary-grey-80"
                          className="sideBar-menu-item"
                        >
                          {sectionLabel}
                        </Text>
                        <span className="sideBar-accordion-icon">
                          {isOpen ? (
                            <IcChevronUp size="small" />
                          ) : (
                            <IcChevronDown size="small" />
                          )}
                        </span>
                      </div>
                      {isOpen && (
                        <div className="sideBar-accordion-content">
                          {items.map((menuItem) => {
                            const isActive = location.pathname === menuItem.path;
                            return (
                              <div
                                key={menuItem.path}
                                className={`sideBar-menu-click ${isActive ? "active" : "inactive"}`}
                                onClick={() => handleNavigation(menuItem.path)}
                              >
                                {/* <Icon
                                  kind={isActive ? "background-bold" : "default"}
                                  ic={menuItem.icon}
                                  color={isActive ? "primary_60" : "primary_grey_80"}
                                  size="medium"
                                /> */}
                                <Text
                                  appearance="body-xs-bold"
                                  color={isActive ? "primary_60" : "primary-grey-80"}
                                  style={{ marginLeft: "8px" }}
                                >
                                  {menuItem.label}
                                </Text>
                              </div>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  );
                } else if (!currentSection && sectionItems.length > 0) {
                  // Render standalone items at the end
                  sectionItems.forEach((menuItem) => {
                    const isActive = location.pathname === menuItem.path;
                    result.push(
                      <div
                        key={menuItem.path}
                        className={`sideBar-menu-click ${isActive ? "active" : "inactive"}`}
                        onClick={() => handleNavigation(menuItem.path)}
                      >
                        <Icon
                          kind={isActive ? "background-bold" : "default"}
                          ic={menuItem.icon}
                          color={isActive ? "primary_60" : "primary_grey_80"}
                          size="small"
                        />
                        <Text
                          appearance="body-xs-bold"
                          color={isActive ? "primary_60" : "primary-grey-80"}
                          style={{ marginLeft: "12px" }}
                        >
                          {menuItem.label}
                        </Text>
                      </div>
                    );
                  });
                }
              }
            });

            return result;
          })()}
        </div>
      </div>
    );
  };

};


export default SideNav;
