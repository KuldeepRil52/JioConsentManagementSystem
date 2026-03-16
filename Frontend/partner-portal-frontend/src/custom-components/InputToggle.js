import React from "react";
import "./InputToggle.css";

const InputToggle = ({
  label,
  checked = false,
  onChange,
  disabled = false,
  name,
  className = "",
  ...rest
}) => {
  // Debug: log when component receives props
  React.useEffect(() => {
    console.log(`InputToggle "${label}" mounted with checked:`, checked);
  }, []);
  
  React.useEffect(() => {
    console.log(`InputToggle "${label}" checked changed to:`, checked);
  }, [checked]);

  const handleChange = (e) => {
    if (disabled) return;
    console.log(`InputToggle "${label}" handleChange called`);
    if (onChange) {
      // Call onChange as it's used in parent components
      onChange(e);
    }
  };

  const handleLabelClick = (e) => {
    // Prevent default to handle it manually
    e.preventDefault();
    e.stopPropagation();
    
    if (disabled) return;
    
    console.log(`InputToggle "${label}" clicked! Current:`, checked, '→ New:', !checked);
    
    // Call onChange - parent components use (prev) => !prev pattern
    if (onChange) {
      onChange();
    }
  };

  return (
    <label 
      className={`toggle-container ${disabled ? "toggle-disabled" : ""} ${className}`}
      onClick={handleLabelClick}
    >
      {label && <span className="toggle-label">{label}</span>}
      <input
        type="checkbox"
        checked={checked}
        onChange={handleChange}
        disabled={disabled}
        name={name}
        className="toggle-input"
        {...rest}
      />
      <span 
        className={`toggle-slider ${checked ? 'toggle-slider-checked' : ''}`}
        style={{
          backgroundColor: checked ? '#0050d4' : '#ccc'
        }}
      >
        <span 
          className="toggle-slider-knob"
          style={{
            transform: checked ? 'translateX(20px)' : 'translateX(0px)'
          }}
        />
      </span>
    </label>
  );
};

export default InputToggle;

