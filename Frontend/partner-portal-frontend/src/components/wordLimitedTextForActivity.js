import React, { useState, useMemo, useEffect } from "react";
import { Text } from "../custom-components"; // adjust import
// import "../styles/masterSetup.css";

const WordLimitedTextarea = React.memo(({ value = "", onTextChange }) => {
  const [text, setText] = useState(value);
  const charLimit = 300;

  useEffect(() => {
    setText(value);
  }, [value]);

  const handleTextAreaChange = (e) => {
    const inputValue = e.target.value;

    if (inputValue.length <= charLimit) {
      setText(inputValue);
    }
  };

  const charCount = useMemo(() => {
    if (!text || typeof text !== "string") return 0;
    return text.length;
  }, [text]);

  useEffect(() => {
    if (onTextChange) {
      onTextChange(text);
    }
  }, [text, onTextChange]);

  return (
    <>
      <Text appearance="body-xs" color="primary-grey-80">
        Details (Required)
      </Text>
      <textarea
        placeholder="Enter description of processor"
        value={text}
        onChange={handleTextAreaChange}
        maxLength={charLimit}
        rows="4"
        className="custom-text-area"
      />
      <div className="word-count" style={{ display: "flex", justifyContent: "flex-end", marginTop: "4px" }}>
        <Text appearance="body-xs" color={charCount === charLimit ? "error" : "primary-grey-80"} style={{ fontSize: "12px" }}>
          {charCount}/{charLimit}
        </Text>
      </div>
    </>
  );
});

export default WordLimitedTextarea;
