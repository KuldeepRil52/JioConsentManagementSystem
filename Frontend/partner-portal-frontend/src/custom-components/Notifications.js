import React from "react";

const Notifications = ({ children, ...rest }) => {
  return <div className="notifications-container" {...rest}>{children}</div>;
};

export default Notifications;

