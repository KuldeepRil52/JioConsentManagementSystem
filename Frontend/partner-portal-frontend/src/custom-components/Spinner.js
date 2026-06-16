import React from "react";
import "./Spinner.css";

/**
 * Custom Spinner Component - Mimics JDS Spinner
 * 
 * Props:
 * - size: small, medium, large
 * - kind: normal, overlay
 * - labelPosition: left, right, top, bottom
 * - label: loading text
 */

const Spinner = ({ 
  size = "medium",
  kind = "normal",
  labelPosition = "right",
  label = "",
  className = "",
  style = {},
  ...props 
}) => {
  return (
    <div 
      className={`custom-spinner-container custom-spinner-label-${labelPosition} ${className}`}
      style={style}
      {...props}
    >
      <div className={`custom-spinner custom-spinner-${size}`}>
        <div className="spinner-circle"></div>
      </div>
      {label && <span className="spinner-label">{label}</span>}
    </div>
  );
};

export default Spinner;

