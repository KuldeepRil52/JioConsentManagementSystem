import { textStyle } from "../utils/textStyles";
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

  const JioDotIcon = ({ size = 55 }) => (
    <svg
      viewBox="0 0 24 24"
      height={size}
      width={size}
      xmlns="http://www.w3.org/2000/svg"
    >
      <g clipPath="url(#clip0)">
        <rect width="24" height="24" rx="12" fill="#3535F3" />
        <path
          d="M8.478 7.237h-.4c-.76 0-1.174.428-1.174 1.285v4.129c0 1.063-.359 1.436-1.201 1.436-.663 0-1.202-.29-1.63-.815-.041-.055-.91.36-.91 1.381 0 1.105 1.034 1.782 2.955 1.782 2.333 0 3.563-1.174 3.563-3.742V8.521c-.002-.856-.416-1.285-1.203-1.285zm9.3 2.017c-2.265 0-3.77 1.436-3.77 3.577 0 2.196 1.45 3.605 3.728 3.605 2.265 0 3.756-1.409 3.756-3.59.001-2.156-1.477-3.592-3.714-3.592zm-.028 5.15c-.884 0-1.491-.648-1.491-1.574 0-.91.622-1.56 1.491-1.56.87 0 1.491.65 1.491 1.574 0 .898-.634 1.56-1.49 1.56zm-5.656-5.082h-.277c-.676 0-1.187.318-1.187 1.285v4.419c0 .98.497 1.285 1.215 1.285h.277c.676 0 1.16-.332 1.16-1.285v-4.42c0-.993-.47-1.284-1.188-1.284zm-.152-3.203c-.856 0-1.395.484-1.395 1.243 0 .773.553 1.256 1.436 1.256.857 0 1.395-.483 1.395-1.256s-.552-1.243-1.436-1.243z"
          fill="#fff"
        />
      </g>
      <defs>
        <clipPath id="clip0">
          <path fill="#fff" d="M0 0h24v24H0z" />
        </clipPath>
      </defs>
    </svg>
  );
  const handleLanguageChange = (e) => {
    const selectedValue = e.target.value;
    const selectedOption = LANGUAGE_OPTIONS.find(
      (opt) => opt.value === selectedValue,
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
          <JioDotIcon size={40} />
          <div style={{ display: "flex", gap: "5px" }}>
            <span
              style={textStyle("body-s-bold", "primary-grey-100")}
              aria-label={`Business name: ${
                businessName || "Test Business Name"
              }`}
            >
              Consent Management |
            </span>
            <span
              style={textStyle("body-s", "primary-grey-80")}
              aria-label={`Business name: ${
                businessName || "Test Business Name"
              }`}
            >
              {businessName || "Test Business Name"}
            </span>
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
                fontSize: "11px",
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
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  height="23"
                  width="23"
                  aria-hidden="true"
                  role="img"
                  style={{ color: "#555" }}
                >
                  <path
                    d="M12 2a10 10 0 100 20 10 10 0 000-20zm0 4a3 3 0 110 6 3 3 0 010-6zm0 14a8 8 0 01-6.54-3.41C6.46 15.08 9 14 12 14s5.54 1.08 6.54 2.59A8 8 0 0112 20z"
                    fill="currentColor"
                  />
                </svg>
              )}
            </div>
          )}
        </div>
      </header>
    );
  }
};

export default HeaderComponent;
