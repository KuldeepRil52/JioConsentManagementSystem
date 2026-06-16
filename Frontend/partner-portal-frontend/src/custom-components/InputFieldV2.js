import React from "react";
import "./InputFieldV2.css";

const InputFieldV2 = ({
  label,
  name,
  type = "text",
  value,
  onChange,
  onBlur,
  onFocus,
  placeholder,
  size = "medium",
  state = "none",
  stateText = "",
  disabled = false,
  readOnly = false,
  autoFocus = false,
  maxLength,
  minLength,
  min,
  max,
  required = false,
  autoComplete,
  children,
  className = "",
  ...rest
}) => {
  const getStateClass = () => {
    switch (state) {
      case "success":
        return "input-field-success";
      case "error":
        return "input-field-error";
      case "warning":
        return "input-field-warning";
      default:
        return "";
    }
  };

  const getSizeClass = () => {
    switch (size) {
      case "small":
        return "input-field-small";
      case "large":
        return "input-field-large";
      case "medium":
      default:
        return "input-field-medium";
    }
  };

  return (
    <div className={`input-field-v2-wrapper ${className}`}>
      {label && (
        <label className="input-field-label">
          {label}
          {required && <span className="input-field-required">*</span>}
        </label>
      )}
      
      <input
        type={type}
        name={name}
        value={value}
        onChange={onChange}
        onBlur={onBlur}
        onFocus={onFocus}
        placeholder={placeholder}
        disabled={disabled}
        readOnly={readOnly}
        autoFocus={autoFocus}
        maxLength={maxLength}
        minLength={minLength}
        min={min}
        max={max}
        autoComplete={autoComplete}
        className={`input-field-v2 ${getSizeClass()} ${getStateClass()}`}
        {...rest}
      />
      
      {stateText && (
        <span className={`input-field-state-text ${getStateClass()}`}>
          {stateText}
        </span>
      )}
      
      {children}
    </div>
  );
};

export default InputFieldV2;

