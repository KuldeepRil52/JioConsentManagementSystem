import React, { useState, useMemo, useEffect } from "react";
import { Text } from "../custom-components"; // adjust import
// import "../styles/masterSetup.css";

const TextAreaForEmailTemplate = React.memo(({ value = "", onChange, onTextChange, disabled = false }) => {
  const [text, setText] = useState(value);
  const wordLimit = 200;

  // Update internal state when value prop changes (for edit mode)
  useEffect(() => {
    setText(value);
  }, [value]);

  const handleTextAreaChange = (e) => {
    const words = e.target.value.split(/\s+/).filter(Boolean);

    if (words.length <= wordLimit) {
      setText(e.target.value);
    } else {
      setText(words.slice(0, wordLimit).join(" "));
    }
  };

  const wordCount = useMemo(() => {
    if (!text || typeof text !== "string") return 0;
    const trimmed = text.trim();
    return trimmed === "" ? 0 : trimmed.split(/\s+/).length;
  }, [text]);

  // Support both onChange and onTextChange prop names
  useEffect(() => {
    if (onChange) {
      onChange(text);
    }
    if (onTextChange) {
      onTextChange(text);
    }
  }, [text, onChange, onTextChange]);

  return (
    <>
      <Text appearance="body-xs" color="primary-grey-80">
        Message body
      </Text>
      <textarea
        // placeholder="Enter description of processor"
        value={text}
        onChange={handleTextAreaChange}
        rows="4"
        className="custom-text-area"
        disabled={disabled}
      />
      <div className="word-count">
        <Text appearance="body-xs" color="primary-grey-80">
          {wordCount}/{wordLimit}
        </Text>
      </div>
    </>
  );
});

export default TextAreaForEmailTemplate;
