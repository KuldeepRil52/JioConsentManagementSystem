import React, { useState, useRef, useEffect } from "react";
import "./InputCode.css";

const InputCode = ({
  length = 6,
  onChange,
  value = "",
  disabled = false,
  ...rest
}) => {
  const [values, setValues] = useState(Array(length).fill(""));
  const inputsRef = useRef([]);

  useEffect(() => {
    if (value && value.length > 0) {
      const newValues = value.split("").slice(0, length);
      while (newValues.length < length) newValues.push("");
      setValues(newValues);
    }
  }, [value, length]);

  const handleChange = (index, newValue) => {
    if (newValue.length > 1) {
      newValue = newValue[newValue.length - 1];
    }

    const newValues = [...values];
    newValues[index] = newValue;
    setValues(newValues);

    if (onChange) {
      onChange(newValues.join(""));
    }

    if (newValue && index < length - 1) {
      inputsRef.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === "Backspace" && !values[index] && index > 0) {
      inputsRef.current[index - 1]?.focus();
    }
  };

  return (
    <div className="input-code-container">
      {values.map((val, index) => (
        <input
          key={index}
          ref={(el) => (inputsRef.current[index] = el)}
          type="text"
          maxLength={1}
          value={val}
          onChange={(e) => handleChange(index, e.target.value)}
          onKeyDown={(e) => handleKeyDown(index, e)}
          disabled={disabled}
          className="input-code-box"
          {...rest}
        />
      ))}
    </div>
  );
};

export default InputCode;

