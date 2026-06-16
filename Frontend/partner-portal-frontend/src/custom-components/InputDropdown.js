import React from "react";
import "./InputDropdown.css";

const InputDropdown = ({
  label,
  options = [],
  value,
  onChange,
  placeholder = "Select an option",
  disabled = false,
  name,
  size = "medium",
  state = "none",
  stateText = "",
  required = false,
  className = "",
  ...rest
}) => {
  const getStateClass = () => {
    switch (state) {
      case "success":
        return "dropdown-success";
      case "error":
        return "dropdown-error";
      case "warning":
        return "dropdown-warning";
      default:
        return "";
    }
  };

  const getSizeClass = () => {
    switch (size) {
      case "small":
        return "dropdown-small";
      case "large":
        return "dropdown-large";
      case "medium":
      default:
        return "dropdown-medium";
    }
  };

  return (
    <div className={`dropdown-wrapper ${className}`}>
      {label && (
        <label className="dropdown-label">
          {label}
          {required && <span className="dropdown-required">*</span>}
        </label>
      )}
      
      <select
        name={name}
        value={value}
        onChange={onChange}
        disabled={disabled}
        className={`dropdown-select ${getSizeClass()} ${getStateClass()}`}
        {...rest}
      >
        {placeholder && (
          <option value="" disabled>
            {placeholder}
          </option>
        )}
        {options.map((option, index) => (
          <option 
            key={index} 
            value={option.value !== undefined ? option.value : option}
          >
            {option.label !== undefined ? option.label : option}
          </option>
        ))}
      </select>
      
      {stateText && (
        <span className={`dropdown-state-text ${getStateClass()}`}>
          {stateText}
        </span>
      )}
    </div>
  );
};

export default InputDropdown;

