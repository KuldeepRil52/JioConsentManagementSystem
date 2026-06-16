import React from "react";
import "./InputCheckbox.css";

const InputCheckbox = ({
  label,
  checked = false,
  onChange,
  disabled = false,
  name,
  value,
  indeterminate = false,
  className = "",
  ...rest
}) => {
  return (
    <label className={`checkbox-container ${disabled ? "checkbox-disabled" : ""} ${className}`}>
      <input
        type="checkbox"
        checked={checked}
        onChange={onChange}
        disabled={disabled}
        name={name}
        value={value}
        className="checkbox-input"
        {...rest}
      />
      <span className={`checkbox-checkmark ${indeterminate ? "checkbox-indeterminate" : ""}`}></span>
      {label && <span className="checkbox-label">{label}</span>}
    </label>
  );
};

export default InputCheckbox;

