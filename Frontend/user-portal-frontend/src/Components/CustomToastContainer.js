import React from "react";
import { textStyle } from "../utils/textStyles";
import { ICON_SIZE } from "../utils/iconSizes";
import {
  FaCheckCircle,
  FaExclamationCircle,
  FaTimesCircle,
} from "react-icons/fa";

const CustomToast = ({ type, message, closeToast }) => {
  return (
    <div className="toast-container">
      {/* Left Icon */}
      <span className="toast-icon">
        {type === "success" ? (
          <FaCheckCircle style={{ color: "green" }} size={ICON_SIZE} />
        ) : (
          <FaExclamationCircle style={{ color: "red" }} size={ICON_SIZE} />
        )}
      </span>

      {/* Message */}
      <div className="toast-message">
        <span style={textStyle("body-s-bold", "primary-background")}>
          {message}
        </span>
      </div>

      {/* Close button */}
      <span className="toast-close">
        <FaTimesCircle
          style={{ color: "red" }}
          size={ICON_SIZE}
          onClick={closeToast}
        />
      </span>
    </div>
  );
};

export default CustomToast;
