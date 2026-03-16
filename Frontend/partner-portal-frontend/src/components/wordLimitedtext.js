import React, { useState, useMemo, useEffect } from "react";
import { Text } from "../custom-components"; // adjust import
import React, { useState, useMemo, useEffect, useRef } from "react";
// import "../styles/masterSetup.css";

const WordLimitedTextarea = React.memo(
  ({ value = "", onTextChange, title = "" }) => {
    const [text, setText] = useState(value);
    const charLimit = 200;
    const isUserInputRef = useRef(false);
    const previousValueRef = useRef(value);

    useEffect(() => {
      // Only update if value changed from parent (not from user input)
      if (value !== previousValueRef.current) {
        if (!isUserInputRef.current) {
          // This is a parent update (e.g., from translation)
          setText(value);
        }
        // Always update the ref to track the latest value
        previousValueRef.current = value;
        // Reset flag after processing
        isUserInputRef.current = false;
      }
    }, [value]);

    const handleTextAreaChange = (e) => {
      const inputValue = e.target.value;

      if (inputValue.length <= charLimit) {
        isUserInputRef.current = true;
        setText(inputValue);
        // Call onTextChange immediately for user input
        if (onTextChange) {
          onTextChange(inputValue);
        }
      }
    };

    const charCount = useMemo(() => {
      if (!text || typeof text !== "string") return 0;
      return text.length;
    }, [text]);

    return (
      <>
        <Text appearance="body-xs" color="primary-grey-80">
          {title || "Details (Required)"}
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
  }
);

export default WordLimitedTextarea;
