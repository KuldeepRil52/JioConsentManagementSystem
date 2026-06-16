import React from "react";

const TabItem = ({ children, label, disabled = false }) => {
  return <div className="tab-item-content">{children}</div>;
};

export default TabItem;

