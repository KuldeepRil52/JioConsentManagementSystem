import React from "react";
import "./Divider.css";

/**
 * Custom Divider Component - Mimics JDS Divider
 * 
 * Props:
 * - variant: default, bold, dashed
 * - orientation: horizontal, vertical
 * - className: additional CSS classes
 * - style: inline styles
 */

const Divider = ({ 
  variant = "default", 
  orientation = "horizontal",
  className = "",
  style = {},
  ...props 
}) => {
  return (
    <div
      className={`custom-divider custom-divider-${variant} custom-divider-${orientation} ${className}`}
      style={style}
      {...props}
    />
  );
};

export default Divider;

