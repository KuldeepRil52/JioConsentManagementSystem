import React, { useEffect } from "react";
import "./Modal.css";

const Modal = ({
  isOpen = false,
  onClose,
  title,
  children,
  footer,
  size = "medium",
  closeOnOverlayClick = true,
  showCloseButton = true,
  className = "",
}) => {
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "unset";
    }

    return () => {
      document.body.style.overflow = "unset";
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget && closeOnOverlayClick && onClose) {
      onClose();
    }
  };

  const getSizeClass = () => {
    switch (size) {
      case "small":
        return "modal-small";
      case "large":
        return "modal-large";
      case "full":
        return "modal-full";
      case "medium":
      default:
        return "modal-medium";
    }
  };

  return (
    <div className="modal-overlay" onClick={handleOverlayClick}>
      <div className={`modal-content ${getSizeClass()} ${className}`}>
        {(title || showCloseButton) && (
          <div className="modal-header">
            {title && <h2 className="modal-title">{title}</h2>}
            {showCloseButton && onClose && (
              <button
                className="modal-close-button"
                onClick={onClose}
                aria-label="Close"
              >
                ×
              </button>
            )}
          </div>
        )}

        <div className="modal-body">{children}</div>

        {footer && <div className="modal-footer">{footer}</div>}
      </div>
    </div>
  );
};

export default Modal;

