import React from "react";
import "../Styles/SessionExpiredModal.css";

const SessionExpiredModal = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="session-overlay">
      <div className="session-modal">
        <h2 className="session-title">Your Session is Expired</h2>
        <p className="session-subtitle">You may close the window</p>
      </div>
    </div>
  );
};

export default SessionExpiredModal;
