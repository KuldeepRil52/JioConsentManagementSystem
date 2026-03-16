import React from "react";
import { Icon, Text } from "@jds/core";
import { IcClose, IcError, IcSuccess } from "@jds/core-icons";

const CustomToast = ({ type, message, closeToast }) => {
  return (
    <div className="toast-container">
      {/* Left Icon */}
      <span className="toast-icon">
        {type === "success" ? (
          <Icon
            ic={<IcSuccess width={15} height={15} />}
            color="feedback_success_50"
          />
        ) : (
          <Icon
            ic={<IcError width={15} height={15} />}
            color="feedback_error_50"
          />
        )}
      </span>

      {/* Message */}
      <div className="toast-message">
        <Text appearance="body-s-bold" color="primary-background">
          {message}
        </Text>
      </div>

      {/* Close button */}
      <span className="toast-close">
        <Icon ic={<IcClose />} color="none" onClick={closeToast} />
      </span>
    </div>
  );
};

export default CustomToast;
