// App.js
import React, { useEffect, useState } from "react";
// import "./jds-styles/main.scss"; // Commented out - using custom components only
import "./tailwind.css";
import { TokenProvider } from "./custom-components";
import "./styles/footer.css";
import "./styles/responsive.css";
import "./styles/hamburger.css";
import "./styles/tables-responsive.css";
import Nav from "./routes/Nav";
import FooterComponent from "./components/Footer";
import Header from "./components/Header";
import SideNav from "./components/SideNav";
import ScrollToTop from "./components/ScrollToTop";
import { useLocation } from "react-router-dom";
import "./styles/App.css";
import IdleTimerContainer from "./utils/IdleTimerContainer";
import { useDispatch, useSelector } from "react-redux";
import { initializeSandboxSession } from "./utils/sandboxInit";
import { disableSandboxMode, isSandboxMode } from "./utils/sandboxMode";
import config from "./utils/config";
import { v4 as uuidv4 } from "uuid";

import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

const App = () => {
  const location = useLocation();
  const dispatch = useDispatch();
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  
  // Get logged-in user info from Redux
  const tenantId = useSelector((state) => state.common.tenant_id);
  const businessId = useSelector((state) => state.common.business_id);
  const token = useSelector((state) => state.common.session_token);

  // Toggle sidebar function
  const toggleSidebar = () => {
    setIsSidebarOpen(!isSidebarOpen);
    
    // Prevent body scroll when sidebar is open on mobile
    if (!isSidebarOpen) {
      document.body.classList.add('sidebar-open');
    } else {
      document.body.classList.remove('sidebar-open');
    }
  };

  // Close sidebar when route changes (mobile only)
  useEffect(() => {
    if (window.innerWidth <= 1024) {
      setIsSidebarOpen(false);
      document.body.classList.remove('sidebar-open');
    }
  }, [location.pathname]);

  // Initialize sandbox session if in sandbox mode
  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const sandboxParam = urlParams.get('sandbox');
    
    // If URL has sandbox=true, initialize/ensure sandbox session
    if (sandboxParam === 'true') {
      initializeSandboxSession(dispatch);
      console.log('🔧 Sandbox mode activated via URL parameter');
    } 
    // Clear sandbox if user is on login/signup page without sandbox parameter
    // This ensures normal logins don't get stuck in sandbox mode
    else if ((location.pathname === '/adminLogin' || location.pathname === '/signup') && !sandboxParam) {
      if (isSandboxMode()) {
        disableSandboxMode();
        console.log('🚪 Sandbox mode cleared - Accessing login/signup normally');
      }
    }
    // If already in sandbox mode (from localStorage), continue the session
    else if (isSandboxMode()) {
      // Don't re-initialize, just let it continue
      console.log('🔧 Continuing sandbox session');
    }
  }, [location, dispatch]);

  // Fetch company logo on initial load if user is logged in
  useEffect(() => {
    const fetchCompanyLogo = async () => {
      // Only fetch if user is logged in and not on login/signup pages
      if (!tenantId || !token || !businessId) return;
      if (location.pathname === '/adminLogin' || location.pathname === '/signup') return;

      try {
        console.log('🎨 Fetching company logo on app load...');
        
        const response = await fetch(
          `${config.system_config}?businessId=${businessId}`,
          {
            method: "GET",
            headers: {
              accept: "application/json",
              "tenant-id": tenantId,
              txn: uuidv4(),
              "x-session-token": token,
            },
          }
        );

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        const systemConfigInfo = data?.data || {};

        if (systemConfigInfo.logo) {
          // Check if logo is base64 or URL
          if (systemConfigInfo.logo.startsWith('data:')) {
            // It's already a base64 data URL - store directly
            dispatch({
              type: "SAVE_COMPANY_LOGO",
              payload: systemConfigInfo.logo,
            });
            console.log('✅ Company logo loaded (base64)');
          } else {
            // It's a URL, fetch as blob and convert to base64 for persistence
            try {
              const logoResponse = await fetch(systemConfigInfo.logo);
              const blob = await logoResponse.blob();
              
              // Convert blob to base64 for persistence across page reloads
              const reader = new FileReader();
              reader.onloadend = () => {
                const base64data = reader.result;
                dispatch({
                  type: "SAVE_COMPANY_LOGO",
                  payload: base64data,
                });
                console.log('✅ Company logo loaded and converted to base64');
              };
              reader.readAsDataURL(blob);
            } catch (error) {
              console.error('❌ Error fetching logo from URL:', error);
              // Fallback to using the URL directly (won't persist on reload)
              dispatch({
                type: "SAVE_COMPANY_LOGO",
                payload: systemConfigInfo.logo,
              });
            }
          }
        }
      } catch (error) {
        console.error('❌ Error fetching company logo:', error);
      }
    };

    fetchCompanyLogo();
  }, [tenantId, token, businessId, dispatch, location.pathname]);

  // Check multilingual config on app load
  useEffect(() => {
    const checkMultilingualConfig = async () => {
      // Only check if user is logged in
      if (!tenantId || !token || !businessId) return;
      if (location.pathname === '/adminLogin' || location.pathname === '/signup') return;

      try {
        console.log('🔍 Checking multilingual config on app load...');
        
        // First, get consent details to check if multilingual is enabled
        const consentResponse = await fetch(
          `${config.get_consent_details}?businessId=${businessId}`,
          {
            method: "GET",
            headers: {
              accept: "*/*",
              "tenant-id": tenantId,
              txn: uuidv4(),
              "x-session-token": token,
            },
          }
        );

        if (!consentResponse.ok) {
          console.log('❌ Consent details API failed:', consentResponse.status);
          return;
        }

        const consentData = await consentResponse.json();
        console.log('📋 Consent config response:', consentData);
        
        const consentConfig = consentData?.searchList?.[0]?.configurationJson;
        console.log('📋 isMultilingualSupport:', consentConfig?.isMultilingualSupport);

        // If multilingual support is enabled in consent config, set Redux state
        if (consentConfig?.isMultilingualSupport === true) {
          dispatch({ type: "SAVE_MULTILINGUAL_CONFIG", payload: true });
          console.log('✅ Multilingual config verified on app load');
        } else {
          console.log('ℹ️ Multilingual support not enabled in config');
        }
      } catch (error) {
        console.error('❌ Error checking multilingual config:', error);
      }
    };

    checkMultilingualConfig();
  }, [tenantId, token, businessId, dispatch, location.pathname]);

  /* ---------------- Hide Header/Footer ---------------- */
  const hideAll = ["/someRouteToHideAll"].includes(location.pathname) || location.pathname.startsWith("/pdf/");

  const hideHeaderRoutes = ["/signup", "/about", "/adminLogin", "/contact"];
  const hideHeader = hideHeaderRoutes.includes(location.pathname) || location.pathname.startsWith("/pdf/");

  /* ---------------- Hide SideNav / Footer on these ---------------- */
  const hideSideAndFooterRoutes = ["/registercookies/report", "/pdf/"];
  // FIX: exclude the main /registercookies route
  const hideSideAndFooter =
    (hideSideAndFooterRoutes.some((p) =>
      location.pathname.startsWith(p)
    ) && location.pathname !== "/registercookies") || location.pathname.startsWith("/pdf/");

  /* ---------------- Hide only SideNav ---------------- */
  const hideSideNavRoutes = [
    "/createConsent",
    "/createEmailTemplate",
    "/createSmsTemplate",
    "/consentLogs",
    "/addRole",
    "/addUser",
    "/editRequest",
    "/createGrievanceForm",
    "/createGrievanceEmailTemplate",
    "/createGrievanceSmsTemplate",
    "/updateRole",
    "/updateUser",
    "/addROPAEntry",
    "/reportBreach",
    "/reportBreachEntry",
    "/createAudit",
    "/grievacneLogs",
    "/consent-system/add",
    "/consent-system/edit",
    "/consent-datasets/create",
    "/consent-deletion-detail",
  ];
  // Also hide side nav for dataset detail pages
  const hideSideNavForDatasetDetail = location.pathname.startsWith("/consent-datasets/") && 
    location.pathname !== "/consent-datasets" && 
    location.pathname !== "/consent-datasets/create";
  const hideSideNav = hideSideNavRoutes.some((p) =>
    location.pathname.startsWith(p)
  ) || hideSideNavForDatasetDetail;

  /* ---------------- Layout Mode ---------------- */
  const isMinimalPage =
    location.pathname === "/" ||
    location.pathname === "/adminLogin" ||
    location.pathname === "/signup" ||
    location.pathname === "/contact";

  /* ---------------- Suppress Warnings ---------------- */
  const suppressedWarnings = ["_inputFocus", "_isValueEmpty"];
  const originalError = console.error;
  console.error = (...args) => {
    if (
      typeof args[0] === "string" &&
      suppressedWarnings.some((entry) => args[0].includes(entry))
    ) {
      return;
    }
    originalError(...args);
  };

  /* ---------------- Render ---------------- */
  return (
    <TokenProvider value={{ theme: "JioBase", mode: "light" }}>
      <ToastContainer />
      <IdleTimerContainer>
        {/* Header */}
        {!hideAll && !hideHeader && (
          <Header 
            isSidebarOpen={isSidebarOpen} 
            toggleSidebar={!hideAll && !hideSideNav && !hideSideAndFooter ? toggleSidebar : null}
          />
        )}

        {/* Backdrop overlay for sidebar on mobile */}
        {isSidebarOpen && !hideAll && !hideSideNav && !hideSideAndFooter && (
          <div 
            className="sidebar-backdrop show"
            onClick={toggleSidebar}
            aria-hidden="true"
          />
        )}

        {/* SideNav only when not hidden - positioned absolutely */}
        {!hideAll && !hideSideNav && !hideSideAndFooter && (
          <SideNav isOpen={isSidebarOpen} />
        )}

        {/* Main layout */}
        <div
          className={
            !isMinimalPage && !hideSideNav && !hideSideAndFooter
              ? "app-layout"
              : ""
          }
        >
          <ScrollToTop />

          {/* Central router */}
          <Nav />
        </div>

        {/* Footer */}
        {!hideAll &&
          !hideSideAndFooter &&
          !hideHeader &&
          !hideSideNav && <FooterComponent />}
      </IdleTimerContainer>
    </TokenProvider>
  );
};

export default App;
