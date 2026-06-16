import React from "react";
import "./Button.css";
import Icon from "./Icon";

/**
 * Custom Button Component - Mimics JDS Button/ActionButton
 * 
 * Props:
 * - kind: primary, secondary, tertiary, danger
 * - size: small, medium, large
 * - label: button text
 * - icon: icon name (string) or icon element
 * - iconLeft: icon on left side
 * - disabled: boolean
 * - onClick: click handler
 * - className: additional CSS classes
 * - style: inline styles
 * - state: normal, hover, active, disabled
 */

const Button = ({ 
  kind = "primary", 
  size = "medium", 
  label = "",
  icon = null,
  iconLeft = null,
  disabled = false,
  onClick = () => {},
  className = "",
  style = {},
  state = "normal",
  children,
  ...props 
}) => {
  const renderIcon = (iconProp) => {
    if (!iconProp) return null;
    
    // If it's a string (icon name), render using Icon component
    if (typeof iconProp === 'string') {
      return <span className="button-icon"><Icon ic={iconProp} size={18} /></span>;
    }
    
    // If it's a React element, render it
    return <span className="button-icon">{iconProp}</span>;
  };

  return (
    <button
      className={`custom-button custom-button-${kind} custom-button-${size} ${disabled || state === 'disabled' ? 'custom-button-disabled' : ''} ${className}`}
      onClick={disabled ? undefined : onClick}
      disabled={disabled}
      style={style}
      {...props}
    >
      {(iconLeft || icon) && renderIcon(iconLeft || icon)}
      {(label || children) && (
        <span className="button-label">{label || children}</span>
      )}
    </button>
  );
};

// Alias for ActionButton
export const ActionButton = Button;

export default Button;

