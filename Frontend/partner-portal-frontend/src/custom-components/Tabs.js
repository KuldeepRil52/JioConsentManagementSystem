import React, { useState } from "react";
import "./Tabs.css";

export const Tabs = ({
  children,
  defaultActiveKey = 0,
  activeKey,
  onTabChange,
  variant = "default",
  className = "",
}) => {
  const [activeTab, setActiveTab] = useState(
    activeKey !== undefined ? activeKey : defaultActiveKey
  );

  const handleTabClick = (key) => {
    if (activeKey === undefined) {
      setActiveTab(key);
    }
    if (onTabChange) {
      onTabChange(key);
    }
  };

  const currentActiveTab = activeKey !== undefined ? activeKey : activeTab;

  const tabs = React.Children.toArray(children);

  return (
    <div className={`tabs-container ${className}`}>
      <div className={`tabs-header tabs-header-${variant}`}>
        {tabs.map((tab, index) => {
          if (!tab || !tab.props) return null;
          const { label, disabled } = tab.props;
          const isActive = currentActiveTab === index;

          return (
            <button
              key={index}
              className={`tab-button ${isActive ? "tab-button-active" : ""} ${
                disabled ? "tab-button-disabled" : ""
              }`}
              onClick={() => !disabled && handleTabClick(index)}
              disabled={disabled}
            >
              {label}
            </button>
          );
        })}
      </div>

      <div className="tabs-content">
        {tabs[currentActiveTab] || <div>No content</div>}
      </div>
    </div>
  );
};

export default Tabs;

