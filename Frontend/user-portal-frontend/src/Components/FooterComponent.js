import React from "react";
import "../Styles/footer.css";
import { textStyle } from "../utils/textStyles";

import { useLocation } from "react-router-dom";

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

  const JioDotIcon = ({ size = 25 }) => (
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

  return (
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
          <JioDotIcon size={25} />
          <span style={textStyle("body-xs", "primary-grey-80")}>
            Copyright © 2025 Jio Platforms Limited. All rights reserved.
          </span>
        </div>
        <div
          style={{
            display: "flex",
            gap: "0.5rem",
            alignItems: "center",
          }}
        >
          <span
            style={{
              ...textStyle("body-xs", "primary-grey-80"),
              cursor: "pointer",
            }}
          >
            Regulatory
          </span>
          <div
            style={{
              backgroundColor: "rgba(224, 224, 224, 1)",
              height: "16px",
              width: "1px",
            }}
          ></div>

          <span
            style={{
              ...textStyle("body-xs", "primary-grey-80"),
              cursor: "pointer",
            }}
          >
            Policies
          </span>
          <div
            style={{
              backgroundColor: "rgba(224, 224, 224, 1)",
              height: "16px",
              width: "1px",
            }}
          ></div>
          <span
            style={{
              ...textStyle("body-xs", "primary-grey-80"),
              cursor: "pointer",
            }}
          >
            Terms & Conditions
          </span>
          <div
            style={{
              backgroundColor: "rgba(224, 224, 224, 1)",
              height: "16px",
              width: "1px",
            }}
          ></div>
          <div onClick={openalert} style={{ cursor: "pointer" }}>
            <span style={textStyle("body-xs", "primary-grey-80")}>
              Help & Support
            </span>
          </div>
        </div>
      </div>
  );
};

export default FooterComponent;
