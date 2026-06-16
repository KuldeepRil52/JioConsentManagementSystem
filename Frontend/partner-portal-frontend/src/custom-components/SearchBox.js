import React from "react";
import "./SearchBox.css";

const SearchBox = ({
  value,
  onChange,
  placeholder = "Search...",
  onSearch,
  disabled = false,
  className = "",
  ...rest
}) => {
  const handleKeyPress = (e) => {
    if (e.key === "Enter" && onSearch) {
      onSearch(value);
    }
  };

  return (
    <div className={`searchbox-container ${className}`}>
      <input
        type="text"
        value={value}
        onChange={onChange}
        onKeyPress={handleKeyPress}
        placeholder={placeholder}
        disabled={disabled}
        className="searchbox-input"
        {...rest}
      />
      <span className="searchbox-icon">🔍</span>
    </div>
  );
};

export default SearchBox;

