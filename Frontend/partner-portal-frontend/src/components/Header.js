import { ActionButton, Text, Divider, AvatarV2, Avatar } from "../custom-components";
import { IcSearch } from "../custom-components/Icon";
import "../styles/header.css";
import "../styles/hamburger.css";
import { useLocation, useNavigate } from "react-router-dom";
import SignUp from "./SignUp";
import SandboxIndicator from "./SandboxIndicator";
import GlobalSearch from "./GlobalSearch";
import AssistantBot from "./AssistantBot";
import { useState, useEffect, useRef } from "react";
import { getUserProfile, fetchBusinessApplication } from "../store/actions/CommonAction";
import { useSelector, useDispatch } from "react-redux";
import { CLEAR_SESSION } from "../store/constants/Constants";
import { disableSandboxMode } from "../utils/sandboxMode";
import config from "../utils/config";
import { makeAPICall } from "../utils/ApiCall";

// UUID generator function (same as in CommonAction.js)
const uuidv4 = () => {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0,
      v = c == "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

const Header = ({ isSidebarOpen, toggleSidebar }) => {

  const navigate = useNavigate();
  const dispatch = useDispatch();

  const jioLogo = new URL("../assets/jio-logo.png", import.meta.url).href;

  const handleClick = () => {
    navigate("/signup");
  };

  const handleLogIn = () => {
    navigate("/adminLogin");
  };

  const tenantId = useSelector((state) => state.common.tenant_id);
  const token = useSelector((state) => state.common.session_token);
  const businessId = useSelector((state) => state.common.business_id);
  const userId = useSelector((state) => state.common.user_id);
  const companyName = useSelector((state) => state.common.companyName);
  const pan = useSelector((state) => state.common.pan);

  const handleConfigureSystem = () => {
    navigate("/consent");
  };
  const [showLogout, setShowLogout] = useState(false);
  const [showSearch, setShowSearch] = useState(false);
  const [showAssistant, setShowAssistant] = useState(false);
  const avatarRef = useRef(null);

  const handleAvatarClick = (e) => {
    e.stopPropagation();
    setShowLogout(!showLogout);
  };
  
  const handleLogOut = async () => {
    setShowLogout(false);
    
    // Call audit API before logout
    try {
      await logAuditEvent(tenantId, businessId, userId, token, companyName, pan);
    } catch (error) {
      console.error('Failed to log audit event:', error);
      // Continue with logout even if audit fails
    }
    
    // Clear sandbox mode data
    disableSandboxMode();
    console.log('🔓 Logging out - Sandbox mode cleared');
    
    // Clear any stored session data if needed
    dispatch({ type: CLEAR_SESSION });
    
    // Replace history to prevent back navigation after logout
    navigate("/adminLogin", { replace: true });
    
    // Clear all navigation history
    window.history.pushState(null, '', window.location.href);
    window.addEventListener('popstate', function(event) {
      window.history.pushState(null, '', window.location.href);
    });
  };

  // Function to log audit event on logout
  const logAuditEvent = async (tenantId, businessId, userId, sessionToken, companyName, pan) => {
    if (!tenantId || !businessId) {
      console.warn('Missing tenantId or businessId for audit log');
      return;
    }

    try {
      const txnId = uuidv4();
      const transactionId = uuidv4();
      
      // Get client IP address (using placeholder as per user's request to keep values static)
      const ipAddress = "192.168.1.1";

      const headers = {
        txn: txnId,
        "X-Tenant-ID": tenantId,
        "X-Business-ID": businessId,
        "X-Transaction-ID": transactionId,
        "Content-Type": "application/json",
      };

      // Add session token if available
      // if (sessionToken) {
      //   headers["x-session-token"] = sessionToken;
      // }

      // Build initiator string with PAN
      const initiator = "Data Fiduciary";

      const body = {
        actor: {
          id: "PAN No./Client ID", // Use userId if available, otherwise default
          role: "Legal-Admin",
          type: "USER"
        },
        businessId: businessId,
        group: "Consent",
        component: "Jio Consent Management System",
        actionType: "LOGOUT",
        resource: {
          type: "userId",
          id: userId || txnId // Use userId if available, otherwise default
        },
        initiator: initiator,
        context: {
          txnId: transactionId,
          ipAddress: ipAddress
        },
        extra: {
          legalEntityName: companyName || "XYZ Pvt Ltd",
          pan: pan || ""
        }
      };

      const response = await makeAPICall(
        config.get_audit_reports,
        "POST",
        body,
        headers
      );

      console.log('✅ Audit log created successfully:', response);
      return response;
    } catch (error) {
      console.error('❌ Error creating audit log:', error);
      throw error;
    }
  };

  const handleProfile = () => {
    setShowLogout(false);
    navigate("/profile");
  };

  // Close logout menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (avatarRef.current && !avatarRef.current.contains(event.target)) {
        setShowLogout(false);
      }
    };

    if (showLogout) {
      document.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [showLogout]);

  const firstChar = useSelector((state) => state.common.firstChar);
  const companyLogo = useSelector((state) => state.common.companyLogo);
  console.log("Company Name from Redux:", companyName);
  console.log("First Character from Redux:", firstChar);
  console.log("Company Logo from Redux:", companyLogo);

  // useEffect(() => {
  //   if (!tenantId) {
  //     console.warn(" Tenant ID is missing in Redux, cannot call API.");
  //     return;
  //   }

  //   console.log(" Tenant ID from Redux:", tenantId);

  
  //   const getBusinessDetails = async () => {
  //     const { companyName, firstChar } = await fetchBusinessApplication(tenantId, token);
  //     console.log("Business Name:", companyName);
  //     console.log("First Character:", firstChar);
  //   };

  //   getBusinessDetails();

  // }, [token, tenantId]);

  const location = useLocation();
  const path = location.pathname;
  if (
    path === "/adminLogin" ||
    path === "/signup"  //new
    // path === "/createConsent" //new
  ) {
    return null;
  }
  if (path === "/") {
    return (
      <div className="mainDiv">
        <div className="logoDiv">
          <img src={jioLogo} alt="Jio Logo" style={{ height: '40px', width: '40px', display: 'block' }} />
          <Text appearance="heading-xxs">Consent Management</Text>
        </div>
        <div className="header-buttons">
          <ActionButton
            appearance="default"
            kind="tertiary"
            size="medium"
            label="Sign Up"
            onClick={handleClick}
          ></ActionButton>
          <ActionButton
            appearance="default"
            kind="primary"
            size="medium"
            label="Log In"
            onClick={handleLogIn}
          ></ActionButton>
        </div>
      </div>
    );
  } else {
    return (
      <div className="mainDiv">
        <div className="logoDiv">
          {/* Hamburger Menu - Only visible on mobile/tablet */}
          {toggleSidebar && (
            <button
              className={`hamburger-menu ${isSidebarOpen ? 'active' : ''}`}
              onClick={toggleSidebar}
              aria-label="Toggle navigation menu"
              aria-expanded={isSidebarOpen}
              style={{ marginRight: '12px' }}
            >
              <span></span>
              <span></span>
              <span></span>
            </button>
          )}
          
          <img src={jioLogo} alt="Jio Logo" style={{ height: '40px', width: '40px', display: 'block' }} />
          <Text appearance="heading-xxs">Consent Management</Text>
          <div style={{ width: '1px', height: '24px', backgroundColor: '#d1d5db', margin: '0 12px' }}></div>
          <Text appearance="body-sm" style={{ color: '#666', fontWeight: '500' }}>
            Jio Platforms Limited
          </Text>
          
          {/* Sandbox Mode Indicator */}
          <SandboxIndicator />
        </div>
        <div className="header-icons">
          <ActionButton 
            icon="IcSearch" 
            kind="tertiary"
            onClick={() => setShowSearch(true)}
          />
          <button 
            className="assistant-bot-trigger"
            onClick={() => setShowAssistant(true)}
            title="AI Assistant"
          >
            <span className="bot-icon">🤖</span>
          </button>
          <div className="avatar-container" ref={avatarRef} onClick={handleAvatarClick}>
            {companyLogo ? (
              <img 
                src={companyLogo} 
                alt="Company Logo" 
                className="company-logo"
              />
            ) : (
              <Avatar
                kind="initials"
                initials={firstChar}
              />
            )}
            {showLogout && (
              <div className="dropdown-menu">
                <div className="menu-item" onClick={handleProfile}>
                  <Text appearance="body-s" color="primary-grey-100">
                    Profile
                  </Text>
                </div>
                <div className="menu-divider"></div>
                <div className="menu-item" onClick={handleLogOut}>
                  <Text appearance="body-s" color="primary-grey-100">
                    Log Out
                  </Text>
                </div>
              </div>
            )}
          </div>  
        </div>

        {/* Global Search Modal */}
        <GlobalSearch isOpen={showSearch} onClose={() => setShowSearch(false)} />
        
        {/* Assistant Bot */}
        <AssistantBot isOpen={showAssistant} onClose={() => setShowAssistant(false)} />
      </div>
    );
  }
};

export default Header;
