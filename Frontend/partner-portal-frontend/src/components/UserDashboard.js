import React, { useState, useEffect, useRef } from "react";
import { Text, SearchBox, ActionButton, Icon, AvatarV2, TabItem, Tabs } from '../custom-components';
import { IcEditPen, IcChevronDown, IcChevronUp, IcFilter, IcDownload, IcError, IcJioDot, IcHome, IcVisible, IcSuccessColored, IcTime } from '../custom-components/Icon';
import { IcStatusSuccessful, IcDocumentViewer, IcGroup, IcNetwork, IcTeam, IcRequest } from '../custom-components/Icon';
import { useSelector } from "react-redux";
import { generateTransactionId } from "../utils/transactionId";
import config from "../utils/config";
import UserDashboardBranding from "./UserDashboardBranding";
import UserDashboardAccessibility from "./UserDashboardAccessibility";
import { languages } from "../utils/languages";
import "../styles/userDashboard.css";
import "../styles/pageConfiguration.css";
import "../styles/sideNav.css";
import "../styles/toast.css";
import "../styles/createConsent.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";

// Import accessibility tab images using URL constructor (Parcel-compatible)
const accTab1Img = new URL("../assets/acc_tab1.png", import.meta.url).href;
const accTab2Img = new URL("../assets/acc_tab2.png", import.meta.url).href;
const accTab3Img = new URL("../assets/acc_tab3.png", import.meta.url).href;

const UserDashboard = () => {
  const tenantId = useSelector((state) => state.common.tenant_id);
  const sessionToken = useSelector((state) => state.common.session_token);
  const businessId = useSelector((state) => state.common.business_id);

  const [expandedRowId, setExpandedRowId] = useState("PRWE10134567");
  const [searchText, setSearchText] = useState("");
  const [selectedTheme, setSelectedTheme] = useState("blue");
  const [savingTheme, setSavingTheme] = useState(false);
  const [loadingTheme, setLoadingTheme] = useState(false);
  const [activeTab, setActiveTab] = useState(0); // 0 = Branding, 1 = Accessibility
  const [selectedLanguage, setSelectedLanguage] = useState("en");
  const [customColor, setCustomColor] = useState(null); // Custom color from color picker
  const [useCustomColor, setUseCustomColor] = useState(false); // Flag to use custom color
  const [accessibilityTab, setAccessibilityTab] = useState("semantics"); // Track active accessibility tab - default to "semantics"

  // Helper function to adjust color brightness
  const adjustColorBrightness = (hex, percent) => {
    if (!hex || !hex.startsWith('#')) return hex;
    const num = parseInt(hex.replace('#', ''), 16);
    if (isNaN(num)) return hex;

    const r = (num >> 16) + Math.round(percent * 255 / 100);
    const g = ((num >> 8) & 0x00FF) + Math.round(percent * 255 / 100);
    const b = (num & 0x0000FF) + Math.round(percent * 255 / 100);
    const newR = Math.max(0, Math.min(255, r));
    const newG = Math.max(0, Math.min(255, g));
    const newB = Math.max(0, Math.min(255, b));
    return '#' + ((newR << 16) | (newG << 8) | newB).toString(16).padStart(6, '0').toUpperCase();
  };

  // Branding states
  const [fileName, setFileName] = useState("");
  const [certificatePreviewUrl, setCertificatePreviewUrl] = useState("");
  const fileInputRef = useRef(null);
  const [fileBase64, setFileBase64] = useState(null);
  const [logoPreviewUrl, setLogoPreviewUrl] = useState("");
  const [logoName, setLogoName] = useState("");
  const logoInputRef = useRef(null);
  const [logoBase64, setLogoBase64] = useState("");
  const [fileSize, setFileSize] = useState(0);
  const [darkMode, setDarkMode] = useState(false);
  const [mobileView, setMobileView] = useState(false);
  const [parentalControl, setParentalControl] = useState(true);
  const [dataTypeToBeShown, setDataTypeToBeShown] = useState(true);
  const [dataItemToBeShown, setDataItemToBeShown] = useState(true);
  const [processActivityNameToBeShown, setProcessActivityNameToBeShown] = useState(true);
  const [processorNameToBeShown, setProcessorNameToBeShown] = useState(true);
  const [validitytoBeShown, setValiditytoBeShown] = useState(true);
  const [colors, setColors] = useState({
    cardBackground: "#FFFFFF",
    cardFont: "#000000",
    buttonBackground: "#0F3CC9",
    buttonFont: "#FFFFFF",
    linkFont: "#0A2885",
  });
  const [darkColors, setDarkColors] = useState({
    cardBackground: "#000000",
    cardFont: "#FFFFFF",
    buttonBackground: "#FFFFFF",
    buttonFont: "#0F3CC9",
    linkFont: "#0A2885",
  });
  const [fontStyles, setFontStyles] = useState({
    url: '',
    family: '',
    size: '14px',
    weight: '400',
    style: 'normal'
  });

  // Color themes for table header and sidenav
  const themes = {
    blue: {
      name: "Base",
      tableHeader: "#e3f2fd",
      sidenav: "#bbdefb"
    },
    green: {
      name: "Camouflage",
      tableHeader: "#e8f5e9",
      sidenav: "#c8e6c9"
    },
    purple: {
      name: "Hazardous",
      tableHeader: "#f3e5f5",
      sidenav: "#e1bee7"
    },
    orange: {
      name: "Bright",
      tableHeader: "#fff3e0",
      sidenav: "#ffe0b2"
    },
    teal: {
      name: "Elevated",
      tableHeader: "#e0f2f1",
      sidenav: "#b2dfdb"
    },
    navy: {
      name: "Deep sea",
      tableHeader: "#e3eaf7",
      sidenav: "#c5d4f0"
    },
    indigo: {
      name: "Indigo",
      tableHeader: "#e8eaf6",
      sidenav: "#c5cae9"
    }
  };

  // Static data based on the image
  const consents = [
    {
      consentId: "PRWE10134567",
      createdDate: "25/09/2025",
      expiryDate: "25/09/2026",
      status: "Active",
      details: [
        {
          purpose: "Account creation (collect data to create user account)",
          dataType: "Profile",
          dataItem: "Gender",
          dataUsedBy: "Jio Platforms Limited",
          status: "Accepted"
        },
        {
          purpose: "Service delivery (process personal data for service)",
          dataType: "Preference",
          dataItem: "PAN, Aadhaar",
          dataUsedBy: "Jio Platforms Limited",
          status: "Accepted"
        }
      ]
    }
  ];

  const handleRowExpand = (consentId) => {
    setExpandedRowId(expandedRowId === consentId ? null : consentId);
  };

  const filteredConsents = consents.filter((consent) =>
    consent.consentId.toLowerCase().includes(searchText.toLowerCase()) ||
    consent.createdDate.toLowerCase().includes(searchText.toLowerCase()) ||
    consent.expiryDate.toLowerCase().includes(searchText.toLowerCase())
  );

  // Handle accessibility tab change
  const handleAccessibilityTabChange = (tab) => {
    setAccessibilityTab(tab);
  };

  // Render accessibility image based on active tab
  const renderAccessibilityImage = () => {
    const imageMap = {
      semantics: accTab1Img,
      focus: accTab2Img,
      screen: accTab3Img
    };

    const altTextMap = {
      semantics: "HTML Semantics Accessibility",
      focus: "Keyboard Focus Order Accessibility",
      screen: "Screen Reader Reading Order Accessibility"
    };

    if (!accessibilityTab || !imageMap[accessibilityTab]) {
      return null;
    }

    return (
      <div
        style={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          width: "100%",
          height: "100%",
          padding: "20px",
          backgroundColor: "#f5f5f5"
        }}
      >
        <img
          src={imageMap[accessibilityTab]}
          alt={altTextMap[accessibilityTab]}
          style={{
            maxWidth: "100%",
            maxHeight: "90vh",
            height: "auto",
            borderRadius: "8px",
            boxShadow: "0 4px 12px rgba(0, 0, 0, 0.15)"
          }}
        />
      </div>
    );
  };

  // Fetch saved theme on component mount
  useEffect(() => {
    const fetchSavedTheme = async () => {
      if (!tenantId || !sessionToken || !businessId) {
        return;
      }

      try {
        setLoadingTheme(true);
        const txnId = generateTransactionId();

        const response = await fetch(
          config.user_dashboard_theme_get,
          {
            method: 'GET',
            headers: {
              'Accept': 'application/json',
              'Content-Type': 'application/json',
              'tenant-id': tenantId,
              'business-id': businessId,
              'scope-level': 'tenant',
              'txn': txnId,
              'x-session-token': sessionToken,
            },
          }
        );

        if (response.ok) {
          const data = await response.json();

          // Decode base64 theme data
          if (data.theme) {
            try {
              const decodedThemeJson = atob(data.theme);
              const decodedTheme = JSON.parse(decodedThemeJson);

              // Find matching theme key by comparing theme data
              const matchingThemeKey = Object.keys(themes).find(themeKey => {
                const theme = themes[themeKey];
                return (
                  theme.tableHeader === decodedTheme.tableHeader &&
                  theme.sidenav === decodedTheme.sidenav
                );
              });

              if (matchingThemeKey) {
                setSelectedTheme(matchingThemeKey);
                setUseCustomColor(false);
              } else {
                // If no matching theme, it's a custom color
                // Use the first color value as custom color (assuming both are same or use tableHeader)
                setCustomColor(decodedTheme.tableHeader || decodedTheme.sidenav);
                setUseCustomColor(true);
                console.log("Custom color theme found:", decodedTheme);
              }
              if (data.typographySettings) {
                try {
                  const decodedTypographySettingsJson = atob(data.typographySettings);
                  const decodedTypographySettings = JSON.parse(decodedTypographySettingsJson);
                  if (decodedTypographySettings.ENGLISH) {
                    const { fontFile, fontSize, fontWeight, fontStyle } = decodedTypographySettings.ENGLISH;
                    const fontUrl = fontFile 
                      ? (fontFile.startsWith('data:') ? fontFile : `data:font/woff2;base64,${fontFile}`)
                      : '';
                    setFontStyles({
                      url: fontUrl,
                      family: fontFile ? 'CustomFont' : 'inherit',
                      size: fontSize ? fontSize + 'px' : '14px',
                      weight: fontWeight || '400',
                      style: fontStyle || 'normal'
                    });
                    if (fontFile) {
                      setFontName("custom-font");
                      setFontBase64(fontUrl);
                    }
                  }
                } catch (decodeError) {
                  console.error("Error decoding typographySettings:", decodeError);
                }
              }
            } catch (decodeError) {
              console.error("Error decoding theme:", decodeError);
            }
          }
        } else {
          // If no theme exists (404 or other error), use default
          console.log("No saved theme found, using default");
        }
      } catch (err) {
        console.error("Error fetching theme:", err);
        // Use default theme on error
      } finally {
        setLoadingTheme(false);
      }
    };

    fetchSavedTheme();
  }, [tenantId, sessionToken, businessId]);

  // Handle theme change (just update state)
  const handleThemeChange = (themeKey) => {
    setSelectedTheme(themeKey);
  };


  // Save theme to API
  const handleSaveTheme = async (colorToSave = null) => {
    if (!tenantId || !sessionToken || !businessId) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="Missing required credentials. Please log in again."
          />
        ),
        { icon: false }
      );
      return;
    }

    try {
      setSavingTheme(true);
      const txnId = generateTransactionId();

      // Determine which color to save
      const color = colorToSave || customColor;

      // Convert theme data to base64
      // If custom color is being used, create theme object with custom color
      // Otherwise use the selected theme
      let themeData;
      if (useCustomColor && color) {
        // Create theme object with custom color for both sidenav and tableHeader
        // Make sidenav slightly darker for contrast
        const normalizedColor = color.toUpperCase().startsWith('#') ? color.toUpperCase() : '#' + color.toUpperCase();
        themeData = {
          tableHeader: normalizedColor,
          sidenav: adjustColorBrightness(normalizedColor, -10) // Slightly darker for sidenav
        };
      } else {
        themeData = themes[selectedTheme];
      }

      if (!themeData) {
        themeData = themes.blue; // Ultimate fallback
      }
      // ✅ Always returns a NUMBER (or fallback) — never "12px" chages by shailendra9.sharma
      const toNumericFontSize = (value, fallback = 12) => {
        if (value === null || value === undefined) return fallback;

        // If it's already a number
        if (typeof value === "number") {
          return Number.isFinite(value) ? Math.round(value) : fallback;
        }

        // If it's a string like "12px", " 12 px ", "12.5px"
        const raw = String(value).trim();

        // Extract first numeric portion
        const match = raw.match(/(\d+(\.\d+)?)/);
        if (!match) return fallback;

        const n = Number(match[1]);
        return Number.isFinite(n) ? Math.round(n) : fallback; // use Math.floor if API requires int
      };

      // normalize before building the config
      const fontSizeNumber = toNumericFontSize(fontStyles?.size, 12);


      const themeJson = JSON.stringify(themeData);
      const themeBase64 = btoa(themeJson);
      const typographySettings = {
        ENGLISH: {
          fontFile: fontStyles.url.split(",").pop(),
          fontSize: fontSizeNumber,
          fontWeight: fontStyles.weight,
          fontStyle: fontStyles.style,
        },
      };
      //const typographySettingsBase64 = btoa(JSON.stringify(typographySettings));

      // Make API call to save theme
      const response = await fetch(
        config.user_dashboard_theme_create,
        {
          method: 'POST',
          headers: {
            'accept': 'application/json',
            'Content-Type': 'application/json',
            'tenant-id': tenantId,
            'business-id': businessId,
            'txn': txnId,
            'x-session-token': sessionToken,
          },
          body: JSON.stringify({
            theme: themeBase64,
            typographySettings: typographySettings,
          }),
        }
      );

      if (response.ok) {
        // Update state to reflect saved custom color
        if (useCustomColor && color) {
          setCustomColor(color);
        }

        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message="Theme saved successfully."
            />
          ),
          { icon: false }
        );
      } else {
        const errorData = await response.json().catch(() => null);
        console.error("Failed to save theme:", errorData);

        // Check for duplicate theme error
        if (errorData && Array.isArray(errorData) && errorData.length > 0) {
          const error = errorData[0];
          if (error.errorCode === "JCMP3003") {
            toast.error(
              (props) => (
                <CustomToast
                  {...props}
                  type="error"
                  message="Theme already exists for this business."
                />
              ),
              { icon: false }
            );
            setSavingTheme(false);
            return;
          }
        }

        throw new Error("Failed to save theme");
      }
    } catch (err) {
      console.error("Error saving theme:", err);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={err.message || "Failed to save theme. Please try again."}
          />
        ),
        { icon: false }
      );
    } finally {
      setSavingTheme(false);
    }
  };

  return (
    <div className="configurePage">
      {/* Header Section - Full Width */}
      <div className="user-dashboard-header">
        <div>
          <div style={{ display: 'flex', flexDirection: 'row', gap: '8px', alignItems: 'center', marginBottom: '12px' }}>
            <Text appearance="heading-s" color="primary-grey-100">
              User Dashboard
            </Text>
            <div className="tag" style={{ marginTop: "8px" }}>
              <Text appearance="body-xs-bold" color="primary-grey-80">
                User Management
              </Text>
            </div>
          </div>
          <Text appearance="body-s" color="primary-grey-80" style={{ marginTop: "12px" }}>
            View and manage user accounts and their activities.
          </Text>
        </div>
      </div>

      {/* Content Area - Split into two columns */}
      <div className="user-dashboard-split-container">
        <div className="user-dashboard-left">
          {/* Tabs Container */}
          <div className="notif-tabs-setup">
            <Tabs
              appearance="normal"
              value={activeTab}
              onChange={(index) => {
                setActiveTab(index);
                // Reset accessibility tab to "semantics" when switching to Accessibility tab (index 1)
                if (index === 1) {
                  setAccessibilityTab("semantics");
                }
              }}
              onNextClick={function noRefCheck() { }}
              onPrevClick={function noRefCheck() { }}
              onScroll={function noRefCheck() { }}
              overflow="fit"
            >

              <TabItem
                label={<Text appearance='body-xs' color="primary-grey-80">Branding</Text>}
                children={
                  <div style={{ marginTop: "20px" }}>
                    <UserDashboardBranding
                      fileName={fileName}
                      setFileName={setFileName}
                      certificatePreviewUrl={certificatePreviewUrl}
                      setCertificatePreviewUrl={setCertificatePreviewUrl}
                      fileInputRef={fileInputRef}
                      fileBase64={fileBase64}
                      setFileBase64={setFileBase64}
                      logoPreviewUrl={logoPreviewUrl}
                      setLogoPreviewUrl={setLogoPreviewUrl}
                      logoName={logoName}
                      setLogoName={setLogoName}
                      logoInputRef={logoInputRef}
                      logoBase64={logoBase64}
                      setLogoBase64={setLogoBase64}
                      fileSize={fileSize}
                      setFileSize={setFileSize}
                      darkMode={darkMode}
                      setDarkMode={setDarkMode}
                      mobileView={mobileView}
                      setMobileView={setMobileView}
                      parentalControl={parentalControl}
                      setParentalControl={setParentalControl}
                      dataTypeToBeShown={dataTypeToBeShown}
                      setDataTypeToBeShown={setDataTypeToBeShown}
                      dataItemToBeShown={dataItemToBeShown}
                      setDataItemToBeShown={setDataItemToBeShown}
                      processActivityNameToBeShown={processActivityNameToBeShown}
                      setProcessActivityNameToBeShown={setProcessActivityNameToBeShown}
                      processorNameToBeShown={processorNameToBeShown}
                      setProcessorNameToBeShown={setProcessorNameToBeShown}
                      validitytoBeShown={validitytoBeShown}
                      setValiditytoBeShown={setValiditytoBeShown}
                      colors={colors}
                      setColors={setColors}
                      darkColors={darkColors}
                      setDarkColors={setDarkColors}
                      customColor={customColor}
                      onColorChange={(color) => {
                        setCustomColor(color);
                        setUseCustomColor(true);
                      }}
                      onSaveTheme={handleSaveTheme}
                      savingTheme={savingTheme}
                      fontStyles={fontStyles}
                      setFontStyles={setFontStyles}
                    />
                  </div>
                }
              />
              <TabItem
                label={<Text appearance='body-xs' color="primary-grey-80">Accessibility</Text>}
                children={
                  <UserDashboardAccessibility
                    onTabChange={handleAccessibilityTabChange}
                    activeAccessibilityTab={accessibilityTab}
                  />
                }
              />
            </Tabs>
          </div>
        </div>
        <div className="user-dashboard-right">
          {/* Show accessibility image or portal layout */}
          {activeTab === 1 && accessibilityTab ? (
            renderAccessibilityImage()
          ) : (
            /* Portal Layout Container */
            <div className="portal-layout-container">
              <style>
                {`
                  .portal-layout-container, .portal-layout-container * {
                    font-family: ${fontStyles.family} !important;
                    font-size: ${fontStyles.size} !important;
                    font-weight: ${fontStyles.weight} !important;
                    font-style: ${fontStyles.style} !important;
                  }
                  .portal-layout-container button, 
                  .portal-layout-container input, 
                  .portal-layout-container select, 
                  .portal-layout-container textarea {
                    font-family: inherit !important;
                    font-size: inherit !important;
                    font-weight: inherit !important;
                    font-style: inherit !important;
                  }
                `}
              </style>
              {/* Header */}
              <div className="portal-header">
                <div className="portal-header-left">
                  <Icon ic={<IcJioDot height={40} width={40} />} />
                  <Text appearance="heading-xxs" color="primary-grey-100" style={{ marginLeft: "12px" }}>
                    Consent Management
                  </Text>
                  <div style={{ width: "1px", height: "24px", backgroundColor: "#e0e0e0", margin: "0 12px" }}></div>
                  <Text appearance="body-xs" color="primary-grey-100">
                    Jio Platforms Ltd.
                  </Text>
                </div>
                <div className="portal-header-right">
                  <AvatarV2 size="small" label="AS" />
                </div>
              </div>

              {/* Main Layout */}
              <div className="portal-main-layout" style={{
                fontFamily: fontStyles.family,
                fontSize: fontStyles.size,
                fontWeight: fontStyles.weight,
                fontStyle: fontStyles.style,
              }}>
                {/* SideNav */}
                <div
                  className="portal-sidenav"
                  style={{
                    backgroundColor: useCustomColor && customColor
                      ? adjustColorBrightness(customColor, -10) // Slightly darker for sidenav
                      : themes[selectedTheme].sidenav
                  }}
                >
                  <div style={{ marginTop: "20%" }}>
                    <Text appearance="body-xs-bold" color="primary-grey-80" className="sideBar-menu-item">
                      Consent
                    </Text>
                    <div className="sideBar-menu-click active">
                      <Icon ic={<IcStatusSuccessful height={23} width={23} />} kind="background-bold" color="primary_60" size="medium" />
                      <Text appearance="body-xs-bold" color="primary_60" style={{ marginLeft: "8px" }}>
                        Granted consents
                      </Text>
                    </div>
                    <div className="sideBar-menu-click inactive">
                      <Icon ic={<IcTime height={23} width={23} />} color="primary_grey_80" size="medium" />
                      <Text appearance="body-xs-bold" color="primary-grey-80" style={{ marginLeft: "8px" }}>
                        Pending requests
                      </Text>
                    </div>

                    <Text appearance="body-xs-bold" color="primary-grey-80" className="sideBar-menu-item" style={{ marginTop: "16px" }}>
                      Grievance redressal
                    </Text>
                    <div className="sideBar-menu-click inactive">
                      <Icon ic={<IcRequest height={23} width={23} />} color="primary_grey_80" size="medium" />
                      <Text appearance="body-xs-bold" color="primary-grey-80" style={{ marginLeft: "8px" }}>
                        Requests
                      </Text>
                    </div>
                  </div>
                </div>

                {/* Main Content */}
                <div className="portal-main-content">
                  {/* Page Header */}
                  <div className="content-page-header">
                    <div className="content-header-left">
                      <div className="content-title-row" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <Text appearance="heading-s" color="primary-grey-100">
                            Granted consents
                          </Text>
                          <div className="content-tag">
                            <Text appearance="body-xs-bold" color="primary-grey-80">
                              Consent
                            </Text>
                          </div>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center' }}>
                          <select
                            value={selectedLanguage}
                            onChange={(e) => setSelectedLanguage(e.target.value)}
                            style={{
                              padding: '8px 12px',
                              borderRadius: '4px',
                              border: '1px solid #e0e0e0',
                              fontSize: '14px',
                              fontFamily: 'inherit',
                              backgroundColor: '#FFFFFF',
                              color: '#333',
                              cursor: 'pointer',
                              minWidth: '180px'
                            }}
                          >
                            {languages.map((lang) => (
                              <option key={lang.value} value={lang.value}>
                                {lang.label}
                              </option>
                            ))}
                          </select>
                        </div>
                      </div>
                      <Text appearance="body-s" color="primary-grey-80" style={{ marginTop: "8px" }}>
                        View and respond to your consent requests awaiting your action.
                      </Text>
                    </div>
                  </div>

                  {/* Summary Cards and Search Row */}
                  <div className="content-main-row">
                    {/* Summary Cards */}
                    <div className="summary-cards-container">
                      <div className="summary-card active-card">
                        <div className="card-icon-circle active-icon">
                          <Icon ic={<IcSuccessColored height={24} width={24} />} />
                        </div>
                        <div className="card-content-vertical">
                          <Text appearance="heading-xs" color="primary-grey-100" style={{ margin: 0 }}>
                            0
                          </Text>
                          <Text appearance="body-s" color="primary-grey-80" style={{ margin: 0, marginTop: "4px" }}>
                            Active
                          </Text>
                        </div>
                      </div>

                      <div className="summary-card withdrawn-card">
                        <div className="card-icon-circle withdrawn-icon">
                          <Icon ic={<IcError height={24} width={24} />} />
                        </div>
                        <div className="card-content-vertical">
                          <Text appearance="heading-xs" color="primary-grey-100" style={{ margin: 0 }}>
                            0
                          </Text>
                          <Text appearance="body-s" color="primary-grey-80" style={{ margin: 0, marginTop: "4px" }}>
                            Withdrawn
                          </Text>
                        </div>
                      </div>
                    </div>

                    {/* Search and Actions */}
                    <div className="search-actions-container">
                      <div className="search-box-wrapper">
                        <SearchBox
                          kind="normal"
                          label="Search data type, items and purpose"
                          value={searchText}
                          onChange={(e) => setSearchText(e.target.value)}
                          prefix="ic_search"
                        />
                      </div>
                      <div className="action-icons">
                        <div className="icon-button" title="Filter">
                          <Icon ic={<IcFilter height={20} width={20} />} color="primary-grey-100" />
                        </div>
                        <div className="icon-button" title="Download">
                          <Icon ic={<IcDownload height={20} width={20} />} color="primary-grey-100" />
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Data Table */}
                  <div className="user-table-container">
                    <table className="user-table">
                      <thead style={{ backgroundColor: useCustomColor && customColor ? customColor : themes[selectedTheme].tableHeader }}>
                        <tr>
                          <th>Consent ID</th>
                          <th>Created date</th>
                          <th>Expiry date</th>
                          <th>Status</th>
                          <th>Action</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredConsents.length > 0 ? (
                          filteredConsents.map((consent) => (
                            <React.Fragment key={consent.consentId}>
                              <tr
                                className={expandedRowId === consent.consentId ? "expanded-row" : ""}
                                onClick={() => handleRowExpand(consent.consentId)}
                                style={{ cursor: "pointer" }}
                              >
                                <td>
                                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                                    {consent.consentId}
                                    <Icon
                                      ic={expandedRowId === consent.consentId ? <IcChevronUp height={16} width={16} /> : <IcChevronDown height={16} width={16} />}
                                      color="primary-grey-80"
                                    />
                                  </div>
                                </td>
                                <td>{consent.createdDate}</td>
                                <td>{consent.expiryDate}</td>
                                <td>
                                  <span className={`status-badge ${consent.status.toLowerCase()}`}>
                                    {consent.status}
                                  </span>
                                </td>
                                <td>
                                  <div className="action-icons-row">
                                    <Icon
                                      ic={<IcVisible height={18} width={18} />}
                                      color="primary-60"
                                      style={{ cursor: "pointer", marginRight: "12px" }}
                                      onClick={(e) => {
                                        e.stopPropagation();
                                        // Handle view action
                                      }}
                                    />
                                    <Icon
                                      ic={<IcEditPen height={18} width={18} />}
                                      color="primary-60"
                                      style={{ cursor: "pointer" }}
                                      onClick={(e) => {
                                        e.stopPropagation();
                                        // Handle edit action
                                      }}
                                    />
                                  </div>
                                </td>
                              </tr>
                              {expandedRowId === consent.consentId && (
                                <tr className="expanded-details-row">
                                  <td colSpan="5">
                                    <div className="expanded-details-table">
                                      <table className="expanded-table">
                                        <thead style={{ backgroundColor: useCustomColor && customColor ? customColor : themes[selectedTheme].tableHeader }}>
                                          <tr>
                                            <th>Purpose</th>
                                            <th>Data type</th>
                                            <th>Data item</th>
                                            <th>Data used by</th>
                                            <th>Status</th>
                                          </tr>
                                        </thead>
                                        <tbody>
                                          {consent.details.map((detail, idx) => (
                                            <tr key={idx}>
                                              <td>{detail.purpose}</td>
                                              <td>{detail.dataType}</td>
                                              <td>{detail.dataItem}</td>
                                              <td>{detail.dataUsedBy}</td>
                                              <td>
                                                <span className="status-badge accepted">
                                                  {detail.status}
                                                </span>
                                              </td>
                                            </tr>
                                          ))}
                                        </tbody>
                                      </table>
                                    </div>
                                  </td>
                                </tr>
                              )}
                            </React.Fragment>
                          ))
                        ) : (
                          <tr>
                            <td colSpan="5" style={{ textAlign: "center", padding: "20px" }}>
                              <Text appearance="body-s" color="primary-grey-80">
                                No consents found
                              </Text>
                            </td>
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>

              {/* Footer */}
              <div className="portal-footer">
                <div className="portal-footer-left">
                  <Icon ic={<IcJioDot height={20} width={20} />} />
                  <Text appearance="body-xs" color="primary-grey-80" style={{ marginLeft: "8px" }}>
                    Copyright © 2025 Jio Platforms Limited. All rights reserved.
                  </Text>
                </div>
                <div className="portal-footer-right">
                  <Text appearance="body-xs" color="primary-60" style={{ cursor: "pointer", marginLeft: "16px" }}>
                    Regulatory
                  </Text>
                  <Text appearance="body-xs" color="primary-60" style={{ cursor: "pointer", marginLeft: "16px" }}>
                    Policies
                  </Text>
                  <Text appearance="body-xs" color="primary-60" style={{ cursor: "pointer", marginLeft: "16px" }}>
                    Terms & Conditions
                  </Text>
                  <Text appearance="body-xs" color="primary-60" style={{ cursor: "pointer", marginLeft: "16px" }}>
                    Help & Support
                  </Text>
                </div>
              </div>
            </div>
          )}
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
    </div>
  );
};

export default UserDashboard;

