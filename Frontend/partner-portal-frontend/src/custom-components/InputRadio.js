import React from "react";
import "./InputRadio.css";

const InputRadio = ({
  label,
  checked = false,
  onChange,
  disabled = false,
  name,
  value,
  className = "",
  ...rest
}) => {
  return (
    <label className={`radio-container ${disabled ? "radio-disabled" : ""} ${className}`}>
      <input
        type="radio"
        checked={checked}
        onChange={onChange}
        disabled={disabled}
        name={name}
        value={value}
        className="radio-input"
        {...rest}
      />
      <span className="radio-checkmark"></span>
      {label && <span className="radio-label">{label}</span>}
    </label>
  );
};

export default InputRadio;

