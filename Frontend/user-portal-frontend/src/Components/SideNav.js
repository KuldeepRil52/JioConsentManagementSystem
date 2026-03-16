import React, { useMemo, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { Icon, Text } from "@jds/core";
import {
  IcAddCircle,
  IcHome,
  IcNotification,
  IcTime,
  IcUser,
} from "@jds/core-icons";
import "../Styles/sideNav.css";
import { IcStatusSuccessful } from "@jds/extended-icons";
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
    []
  );

  const { getTranslation, translateContent, currentLanguage } =
    useTranslation(translationInputs);

  // Translate content when language changes
  useEffect(() => {
    if (currentLanguage !== "ENGLISH") {
      translateContent(translationInputs, currentLanguage);
    }
  }, [currentLanguage, translationInputs, translateContent]);

  const menuItems = [
    { type: "heading", label: getTranslation("nav_consent", "Consent") },
    {
      label: getTranslation("nav_granted_consents", "Granted consents"),
      icon: <IcStatusSuccessful height={23} width={23} aria-hidden="true" />,
      path: "/",
    },
    {
      label: getTranslation("nav_pending_requests", "Pending requests"),
      icon: <IcTime height={23} width={23} aria-hidden="true" />,
      path: "/systemRequests",
    },

    { type: "heading", label: getTranslation("nav_grievance_redressal", "Grievance redressal") },
    {
      label: getTranslation("nav_requests", "Requests"),
      icon: <IcStatusSuccessful height={23} width={23} aria-hidden="true" />,
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
      <nav
        className="sideBar-outer-container"
        aria-label="Main Navigation"
      >
        <div role="menu" aria-label="Navigation menu">
          {cusId && (
            <div
              className="sideBar-menu-click inactive"
              aria-label={`Customer ID: ${cusId}`}
            >
              <Icon
                kind="default"
                ic={<IcUser height={23} width={23} aria-hidden="true" />}
                color="primary_grey_80"
                size="medium"
              />
              <div className="sideBar-cusId" title={cusId}>
                <Text appearance="body-xs-bold" color="primary-grey-80">
                  {cusId}
                </Text>
              </div>
            </div>
          )}

          {menuItems.map((item) => {
            if (item.type === "heading") {
              return (
                <Text
                  key={item.label}
                  appearance="body-xs-bold"
                  color="primary-grey-80"
                  className="sideBar-menu-item"
                  aria-label={`${item.label} section`}
                >
                  {item.label}
                </Text>
              );
            }

            const isActive = location.pathname === item.path;

            return (
              <div
                key={item.path}
                className={`sideBar-menu-click ${
                  isActive ? "active" : "inactive"
                }`}
                onClick={() => handleNavigation(item.path)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    handleNavigation(item.path);
                  }
                }}
                tabIndex={0}
                role="menuitem"
                aria-label={`Navigate to ${item.label}`}
                aria-current={isActive ? "page" : undefined}
              >
                <Icon
                  kind={isActive ? "background-bold" : "default"}
                  ic={item.icon}
                  color={isActive ? "primary_60" : "primary_grey_80"}
                  size="medium"
                />
                <Text
                  appearance="body-xs-bold"
                  color={isActive ? "primary_60" : "primary-grey-80"}
                  style={{ marginLeft: "8px" }}
                >
                  {item.label}
                </Text>
              </div>
            );
          })}
        </div>
      </nav>
    );
  }
};

export default SideNav;
