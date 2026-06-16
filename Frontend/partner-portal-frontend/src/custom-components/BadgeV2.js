import React from "react";
import "./BadgeV2.css";

const BadgeV2 = ({
  children,
  label, // Support both label and children
  variant = "default",
  color = "primary",
  size = "medium",
  className = "",
  ...rest
}) => {
  return (
    <span
      className={`badge badge-${variant} badge-${color} badge-${size} ${className}`}
      {...rest}
    >
      {label || children}
    </span>
  );
};

export default BadgeV2;

