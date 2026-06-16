import React from "react";

const IconSuccess = () => (
  <svg
    className="toast-svg toast-svg--success"
    width={15}
    height={15}
    viewBox="0 0 15 15"
    aria-hidden="true"
  >
    <circle cx="7.5" cy="7.5" r="7" fill="currentColor" />
    <path
      d="M4.2 7.6l2.1 2 4.5-4.3"
      fill="none"
      stroke="#fff"
      strokeWidth="1.3"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

const IconError = () => (
  <svg
    className="toast-svg toast-svg--error"
    width={15}
    height={15}
    viewBox="0 0 15 15"
    aria-hidden="true"
  >
    <circle cx="7.5" cy="7.5" r="7" fill="currentColor" />
    <path
      d="M7.5 4.3v4.2M7.5 10.7h.01"
      fill="none"
      stroke="#fff"
      strokeWidth="1.3"
      strokeLinecap="round"
    />
  </svg>
);

const IconClose = () => (
  <svg
    className="toast-svg toast-svg--close"
    width={16}
    height={16}
    viewBox="0 0 16 16"
    aria-hidden="true"
  >
    <path
      d="M4 4l8 8M12 4l-8 8"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
    />
  </svg>
);

const CustomToast = ({ type, message, closeToast }) => {
  return (
    <div className="toast-container">
      <span className="toast-icon">
        {type === "success" ? <IconSuccess /> : <IconError />}
      </span>

      <div className="toast-message">
        <div className="toast-message-text">{message}</div>
      </div>

      <span className="toast-close" onClick={closeToast}>
        <IconClose />
      </span>
    </div>
  );
};

export default CustomToast;
