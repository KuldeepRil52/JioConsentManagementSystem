import { Avatar, Text } from "@jds/core";
import { IcJioDot } from "@jds/core-icons";
import { useSelector, useDispatch } from "react-redux";
import { useLocation } from "react-router-dom";
import { SET_CURRENT_LANGUAGE } from "../store/constants/Constants";

// Language options with API values for Bhashini translation
const LANGUAGE_OPTIONS = [
  { value: "ENGLISH", label: "English", apiValue: "en" },
  { value: "HINDI", label: "हिंदी", apiValue: "hi" },
  { value: "BENGALI", label: "বাংলা", apiValue: "bn" },
  { value: "TAMIL", label: "தமிழ்", apiValue: "ta" },
  { value: "TELUGU", label: "తెలుగు", apiValue: "te" },
  { value: "KANNADA", label: "ಕನ್ನಡ", apiValue: "kn" },
  { value: "MALAYALAM", label: "മലയാളം", apiValue: "ml" },
  { value: "GUJARATI", label: "ગુજરાતી", apiValue: "gu" },
  { value: "MARATHI", label: "मराठी", apiValue: "mr" },
  { value: "ODIA", label: "ଓଡ଼ିଆ", apiValue: "or" },
  { value: "PUNJABI", label: "ਪੰਜਾਬੀ", apiValue: "pa" },
  { value: "ASSAMESE", label: "অসমীয়া", apiValue: "as" },
  { value: "NEPALI", label: "नेपाली", apiValue: "ne" },
  { value: "SINDHI", label: "سنڌي", apiValue: "sd" },
  { value: "KASHMIRI", label: "कॉशुर", apiValue: "ks" },
  { value: "DOGRI", label: "डोगरी", apiValue: "doi" },
  { value: "KONKANI", label: "कोंकणी", apiValue: "gom" },
  { value: "MAITHILI", label: "मैथिली", apiValue: "mai" },
  { value: "SANSKRIT", label: "संस्कृत", apiValue: "sa" },
  { value: "SANTALI", label: "ᱥᱟᱱᱛᱟᱲᱤ", apiValue: "sat" },
  { value: "BODO", label: "बड़ो", apiValue: "brx" },
  { value: "MANIPURI", label: "মৈতৈলোন্", apiValue: "mni" },
];

const HeaderComponent = () => {
  const dispatch = useDispatch();
  const businessName = useSelector((state) => state.common.business_name);
  const businessLogo = useSelector((state) => state.common.business_logo);
  console.log("Business Logo in Header:", businessLogo);
  const currentLanguage = useSelector((state) => state.common.currentLanguage);
  const initials = businessName ? businessName.charAt(0).toUpperCase() : "";
  const location = useLocation();

  const handleLanguageChange = (e) => {
    const selectedValue = e.target.value;
    const selectedOption = LANGUAGE_OPTIONS.find(
      (opt) => opt.value === selectedValue
    );

    if (selectedOption) {
      dispatch({
        type: SET_CURRENT_LANGUAGE,
        payload: {
          language: selectedOption.value,
          sourceLang: selectedOption.apiValue,
        },
      });
    }
  };

  const path = location.pathname;

  if (path === "/adminLogin" || path === "/app-documents" || path === "/home") {
    return null;
  } else {
    return (
      <header
        role="banner"
        aria-label="Application Header"
        style={{
          backgroundColor: "rgba(255, 255, 255, 1)",
          height: "55px",
          boxShadow: "0px 2px 4px rgba(0, 0, 0, 0.08)",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          paddingLeft: "3rem",
          paddingRight: "3rem",
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            gap: "0.5rem",
          }}
          aria-label="Business Information"
        >
          <IcJioDot height={40} width={40} aria-hidden="true" />
          <div style={{ display: "flex", gap: "5px" }}>
            <Text
              appearance="body-s-bold"
              color="primary-grey-100"
              aria-label={`Business name: ${
                businessName || "Test Business Name"
              }`}
            >
              Consent Management |
            </Text>
            <Text
              appearance="body-s"
              color="primary-grey-80"
              aria-label={`Business name: ${
                businessName || "Test Business Name"
              }`}
            >
              {businessName || "Test Business Name"}
            </Text>
          </div>
        </div>

        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "1rem",
          }}
        >
          {/* Language Dropdown */}
          <div
            style={{ position: "relative" }}
            role="group"
            aria-labelledby="language-select-label"
          >
            <span id="language-select-label" className="sr-only">
              Language Selection
            </span>
            <select
              id="language-selector"
              value={currentLanguage || "ENGLISH"}
              onChange={handleLanguageChange}
              aria-label="Select preferred language for the application"
              aria-describedby="language-select-label"
              style={{
                appearance: "none",
                WebkitAppearance: "none",
                MozAppearance: "none",
                backgroundColor: "#fff",
                border: "1px solid #d1d5db",
                borderRadius: "8px",
                padding: "10px 40px 10px 16px",
                fontSize: "16px",
                fontWeight: "500",
                color: "#1a1a1a",
                cursor: "pointer",
                outline: "none",
                minWidth: "140px",
                fontFamily: "inherit",
              }}
            >
              {LANGUAGE_OPTIONS.map((lang) => (
                <option key={lang.value} value={lang.value}>
                  {lang.label}
                </option>
              ))}
            </select>
            <div
              style={{
                position: "absolute",
                right: "14px",
                top: "50%",
                transform: "translateY(-50%)",
                pointerEvents: "none",
              }}
            >
              <svg
                width="12"
                height="8"
                viewBox="0 0 12 8"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
                aria-hidden="true"
                focusable="false"
              >
                <path
                  d="M1 1.5L6 6.5L11 1.5"
                  stroke="#1a1a1a"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </div>
          </div>
          {/* User Avatar */}

          {initials && (
            <div
              className="header-icons"
              aria-label={`User avatar for ${businessName}`}
            >
              {businessLogo ? (
                <img
                  src={businessLogo}
                  alt={`${businessName} logo`}
                  className="business-logo-avatar"
                  style={{ width: "40px", height: "40px", borderRadius: "50%" }}
                />
              ) : (
                <Avatar
                  kind="initials"
                  initials={initials || "J"}
                  aria-label={`User profile: ${initials}`}
                />
              )}
            </div>
          )}
        </div>
      </header>
    );
  }
};

export default HeaderComponent;
