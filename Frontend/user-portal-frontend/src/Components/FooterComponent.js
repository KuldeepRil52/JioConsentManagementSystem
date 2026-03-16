import React from "react";
import "../JDS_Styles/main.scss";
import "../Styles/footer.css";
import { Divider, Footer, Text, TokenProvider } from "@jds/core";
import { IcJioDot } from "@jds/core-icons";
import { useNavigate, useLocation } from "react-router-dom";

const FooterComponent = () => {
  const location = useLocation();
  const path = location.pathname;

  const openalert = () => {};

  // Hide footer on specific routes
  if (
    path === "/adminLogin" ||
    path === "/app-documents" ||
    path.startsWith("/home")
  ) {
    return null;
  }

  return (
    <TokenProvider
      value={{
        theme: "JioBase",
        mode: "light",
      }}
    >
      <div
        style={{
          height: "55px",
          backgroundColor: "rgba(245, 245, 245, 1)",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          paddingLeft: "3rem",
          paddingRight: "3rem",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
          <IcJioDot height={25} width={25} color="primary" />
          <Text color="primary-grey-80" appearance="body-xs">
            Copyright © 2025 Jio Platforms Limited. All rights reserved.
          </Text>
        </div>
        <div
          style={{
            display: "flex",
            gap: "0.5rem",
            alignItems: "center",
          }}
        >
          <Text
            color="primary-grey-80"
            appearance="body-xs"
            style={{ cursor: "pointer" }}
          >
            Regulatory
          </Text>
          <div
            style={{
              backgroundColor: "rgba(224, 224, 224, 1)",
              height: "16px",
              width: "1px",
            }}
          ></div>

          <Text
            color="primary-grey-80"
            appearance="body-xs"
            style={{ cursor: "pointer" }}
          >
            Policies
          </Text>
          <div
            style={{
              backgroundColor: "rgba(224, 224, 224, 1)",
              height: "16px",
              width: "1px",
            }}
          ></div>
          <Text
            color="primary-grey-80"
            appearance="body-xs"
            style={{ cursor: "pointer" }}
          >
            Terms & Conditions
          </Text>
          <div
            style={{
              backgroundColor: "rgba(224, 224, 224, 1)",
              height: "16px",
              width: "1px",
            }}
          ></div>
          <div onClick={openalert} style={{ cursor: "pointer" }}>
            <Text color="primary-grey-80" appearance="body-xs">
              Help & Support
            </Text>
          </div>
        </div>
      </div>
    </TokenProvider>
  );
};

export default FooterComponent;
