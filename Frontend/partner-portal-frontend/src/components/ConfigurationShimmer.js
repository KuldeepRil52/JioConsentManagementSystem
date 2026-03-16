import React from "react";
import "../styles/configurationShimmer.css";

const ConfigurationShimmer = () => {
  return (
    <div className="configurePage">
      <div className="shimmer-config-container">
        {/* Header Section */}
        <div className="shimmer-header">
          <div className="shimmer-title shimmer-box"></div>
          <div className="shimmer-badge shimmer-box"></div>
        </div>

        {/* Content Section */}
        <div className="shimmer-content">
          {/* Left Column */}
          <div className="shimmer-column">
            {/* Dropdown/Select Fields */}
            <div className="shimmer-field-group">
              <div className="shimmer-label shimmer-box"></div>
              <div className="shimmer-select shimmer-box"></div>
              <div className="shimmer-helper shimmer-box"></div>
            </div>

            <div className="shimmer-field-group">
              <div className="shimmer-label shimmer-box"></div>
              <div className="shimmer-input-group">
                <div className="shimmer-input shimmer-box"></div>
                <div className="shimmer-dropdown-small shimmer-box"></div>
              </div>
              <div className="shimmer-helper shimmer-box"></div>
            </div>

            <div className="shimmer-field-group">
              <div className="shimmer-label shimmer-box"></div>
              <div className="shimmer-input-group">
                <div className="shimmer-input shimmer-box"></div>
                <div className="shimmer-dropdown-small shimmer-box"></div>
              </div>
              <div className="shimmer-helper shimmer-box"></div>
            </div>

            {/* Checkbox Fields */}
            <div className="shimmer-checkbox-group">
              <div className="shimmer-checkbox shimmer-box"></div>
              <div className="shimmer-checkbox-label shimmer-box"></div>
            </div>

            <div className="shimmer-checkbox-group">
              <div className="shimmer-checkbox shimmer-box"></div>
              <div className="shimmer-checkbox-label shimmer-box"></div>
            </div>

            <div className="shimmer-checkbox-group">
              <div className="shimmer-checkbox shimmer-box"></div>
              <div className="shimmer-checkbox-label shimmer-box"></div>
            </div>

            <div className="shimmer-checkbox-group">
              <div className="shimmer-checkbox shimmer-box"></div>
              <div className="shimmer-checkbox-label shimmer-box"></div>
            </div>

            {/* Conditional Fields (for DigiLocker) */}
            <div className="shimmer-field-group">
              <div className="shimmer-label shimmer-box"></div>
              <div className="shimmer-input shimmer-box"></div>
              <div className="shimmer-helper shimmer-box"></div>
            </div>

            <div className="shimmer-field-group">
              <div className="shimmer-label shimmer-box"></div>
              <div className="shimmer-input shimmer-box"></div>
              <div className="shimmer-helper shimmer-box"></div>
            </div>
          </div>

          {/* Right Column (for some pages) */}
          <div className="shimmer-column">
            {/* File Upload Section */}
            <div className="shimmer-field-group">
              <div className="shimmer-label shimmer-box"></div>
              <div className="shimmer-upload-box shimmer-box"></div>
              <div className="shimmer-helper shimmer-box"></div>
            </div>

            {/* Additional Input Fields */}
            <div className="shimmer-field-group">
              <div className="shimmer-label shimmer-box"></div>
              <div className="shimmer-input shimmer-box"></div>
              <div className="shimmer-helper shimmer-box"></div>
            </div>

            <div className="shimmer-field-group">
              <div className="shimmer-label shimmer-box"></div>
              <div className="shimmer-input shimmer-box"></div>
              <div className="shimmer-helper shimmer-box"></div>
            </div>
          </div>
        </div>

        {/* Save Button */}
        <div className="shimmer-button-container">
          <div className="shimmer-button shimmer-box"></div>
        </div>
      </div>
    </div>
  );
};

export default ConfigurationShimmer;


