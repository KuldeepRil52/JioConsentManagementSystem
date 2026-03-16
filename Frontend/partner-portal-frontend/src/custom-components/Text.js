import React from "react";
import "./Text.css";

/**
 * Custom Text Component - Mimics JDS Text component
 * 
 * Props:
 * - appearance: heading-xxl, heading-xl, heading-l, heading-m, heading-s, heading-xs, heading-xxs,
 *               body-l, body-m, body-s, body-xs, body-xxs, body-l-bold, body-m-bold, body-s-bold, body-xs-bold, body-xxs-bold
 * - color: primary-grey-100, primary-grey-80, primary-grey-60, primary_60, feedback_error_50, etc.
 * - className: additional CSS classes
 * - style: inline styles
 * - children: text content
 */

const Text = ({ 
  appearance = "body-m", 
  color = "primary-grey-100", 
  className = "", 
  style = {}, 
  children,
  ...props 
}) => {
  const colorMap = {
    "primary-grey-100": "#111827",
    "primary-grey-80": "#6b7280",
    "primary-grey-60": "#9ca3af",
    "primary_60": "#0f3cc9",
    "primary_grey_80": "#6b7280",
    "grey-100": "#111827",
    "feedback_error_50": "#dc2626",
    "feedback_warning_50": "#f59e0b",
    "feedback_success_50": "#10b981",
  };

  const colorStyle = colorMap[color] || color;

  return (
    <span
      className={`custom-text custom-text-${appearance} ${className}`}
      style={{ color: colorStyle, ...style }}
      {...props}
    >
      {children}
    </span>
  );
};

export default Text;

