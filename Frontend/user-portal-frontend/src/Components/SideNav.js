import React, { useMemo, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { textStyle } from "../utils/textStyles";
import {
  NAV_ICON_OUTLINE,
  NAV_ICON_PILL,
  NAV_ICON_PILL_INSET,
} from "../utils/iconSizes";
import "../Styles/sideNav.css";
import { useSelector } from "react-redux";
import useTranslation from "../hooks/useTranslation";

const SideNav = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const path = location.pathname;
  const cusId = useSelector((state) => state.common.customer_id);

  // Translation inputs for navigation labels
  const translationInputs = useMemo(
    () => [
      { id: "nav_consent", source: "Consent" },
      { id: "nav_granted_consents", source: "Granted consents" },
      { id: "nav_pending_requests", source: "Pending requests" },
      { id: "nav_grievance_redressal", source: "Grievance redressal" },
      { id: "nav_requests", source: "Requests" },
    ],
    [],
  );

  const { getTranslation, translateContent, currentLanguage } =
    useTranslation(translationInputs);

  // Translate content when language changes
  useEffect(() => {
    if (currentLanguage !== "ENGLISH") {
      translateContent(translationInputs, currentLanguage);
    }
  }, [currentLanguage, translationInputs, translateContent]);

  const SuccessIcon = ({ isActive }) => {
    const successPath = `M4.6 7.88a1 1 0 00.82-.43 8.06 8.06 0 011.74-1.81A1 1 0 106 4.05a9.93 9.93 0 00-2.22 2.27A1 1 0 004 7.71a1 1 0 00.6.17zm-.49 5.36A8.35 8.35 0 014 12a8.23 8.23 0 01.11-1.24 1.012 1.012 0 00-2-.31 9.31 9.31 0 000 3.1 1 1 0 001 .85h.15a1.001 1.001 0 00.85-1.16zM12 2a10.3 10.3 0 00-2 .2 1.02 1.02 0 10.4 2A7.85 7.85 0 0112 4a8 8 0 010 16 7.85 7.85 0 01-1.61-.16 1.019 1.019 0 10-.4 2 10.3 10.3 0 002 .2A10.02 10.02 0 0012 2zM5.42 16.55a1.001 1.001 0 00-1.863.354 1 1 0 00.223.776A9.93 9.93 0 006 20a1 1 0 001.18.012 1 1 0 00.03-1.602 8.06 8.06 0 01-1.79-1.86zm1.37-5.26a1 1 0 000 1.42l3 3a1 1 0 001.42 0l6-6a1.004 1.004 0 00-1.42-1.42l-5.29 5.3-2.29-2.3a1.001 1.001 0 00-1.42 0z`;

    if (isActive) {
      return (
        <div
          style={{
            width: NAV_ICON_PILL,
            height: NAV_ICON_PILL,
            backgroundColor: "#2563eb",
            borderRadius: "999px",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            flexShrink: 0,
          }}
        >
          <svg
            viewBox="0 0 24 24"
            height={NAV_ICON_PILL_INSET}
            width={NAV_ICON_PILL_INSET}
          >
            <path d={successPath} fill="#fff" />
          </svg>
        </div>
      );
    }

    return (
      <svg
        viewBox="0 0 24 24"
        height={NAV_ICON_OUTLINE}
        width={NAV_ICON_OUTLINE}
        style={{ color: "#6b7280", flexShrink: 0 }}
      >
        <path d={successPath} fill="currentColor" />
      </svg>
    );
  };
  const TimeIcon = ({ isActive }) => {
    if (isActive) {
      return (
        <div
          style={{
            width: NAV_ICON_PILL,
            height: NAV_ICON_PILL,
            backgroundColor: "#2563eb", // blue
            borderRadius: "999px",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            flexShrink: 0,
          }}
        >
          <svg
            viewBox="0 0 24 24"
            height={NAV_ICON_PILL_INSET}
            width={NAV_ICON_PILL_INSET}
          >
            <path
              d="M12 2a10 10 0 100 20 10 10 0 000-20zm1 11a1 1 0 01-1 1H9a1 1 0 010-2h2V9a1 1 0 012 0v4z"
              fill="#fff"
            />
          </svg>
        </div>
      );
    }

    return (
      <svg
        viewBox="0 0 24 24"
        height={NAV_ICON_OUTLINE}
        width={NAV_ICON_OUTLINE}
        style={{ color: "#6b7280", flexShrink: 0 }} // grey
      >
        <path
          d="M12 2a10 10 0 100 20 10 10 0 000-20zm1 11a1 1 0 01-1 1H9a1 1 0 010-2h2V9a1 1 0 012 0v4z"
          fill="currentColor"
        />
      </svg>
    );
  };

  const menuItems = [
    { type: "heading", label: getTranslation("nav_consent", "Consent") },
    {
      label: getTranslation("nav_granted_consents", "Granted consents"),
      icon: "success",
      path: "/",
    },
    {
      label: getTranslation("nav_pending_requests", "Pending requests"),
      icon: "time",
      path: "/systemRequests",
    },
    {
      type: "heading",
      label: getTranslation("nav_grievance_redressal", "Grievance redressal"),
    },
    {
      label: getTranslation("nav_requests", "Requests"),
      icon: "success",
      path: "/requests",
    },
  ];

  const handleNavigation = (path) => {
    navigate(path);
  };

  if (
    path === "/adminLogin" ||
    path === "/app-documents" ||
    path === "/home" ||
    path === "/createRequest"
  ) {
    return null;
  } else {
    return (
      <nav className="sideBar-outer-container" aria-label="Main Navigation">
        <div role="menu" aria-label="Navigation menu">
          {cusId && (
            <div
              className="sideBar-menu-click inactive"
              aria-label={`Customer ID: ${cusId}`}
            >
              <svg
                viewBox="0 0 24 24"
                fill="none"
                height={NAV_ICON_OUTLINE}
                width={NAV_ICON_OUTLINE}
                aria-hidden="true"
                role="img"
                style={{ color: "#555", flexShrink: 0 }}
              >
                <path
                  d="M12 2a10 10 0 100 20 10 10 0 000-20zm0 4a3 3 0 110 6 3 3 0 010-6zm0 14a8 8 0 01-6.54-3.41C6.46 15.08 9 14 12 14s5.54 1.08 6.54 2.59A8 8 0 0112 20z"
                  fill="currentColor"
                />
              </svg>
              <div className="sideBar-cusId" title={cusId}>
                <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                  {cusId}
                </span>
              </div>
            </div>
          )}

          {menuItems.map((item) => {
            if (item.type === "heading") {
              return (
                <p
                  key={item.label}
                  style={{
                    ...textStyle("body-xs-bold", "primary-grey-80"),
                    marginTop: "12px",
                    marginBottom: "4px",
                  }}
                >
                  {item.label}
                </p>
              );
            }

            const isActive = location.pathname === item.path;

            return (
              <div
                key={item.path}
                className={`sideBar-menu-click ${isActive ? "active" : ""}`}
                onClick={() => handleNavigation(item.path)}
                tabIndex={0}
                role="menuitem"
              >
                {item.icon === "success" && <SuccessIcon isActive={isActive} />}
                {item.icon === "time" && <TimeIcon isActive={isActive} />}

                <span
                  style={{
                    ...textStyle("body-xs-bold", "primary-grey-80"),
                    marginLeft: "8px",
                  }}
                >
                  {item.label}
                </span>
              </div>
            );
          })}
        </div>
      </nav>
    );
  }
};

export default SideNav;
