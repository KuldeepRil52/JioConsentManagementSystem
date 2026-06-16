import React from "react";
import "./Card.css";

const Card = ({
  children,
  title,
  subtitle,
  footer,
  variant = "elevated",
  padding = "default",
  className = "",
  onClick,
  ...rest
}) => {
  const getVariantClass = () => {
    switch (variant) {
      case "outlined":
        return "card-outlined";
      case "filled":
        return "card-filled";
      case "elevated":
      default:
        return "card-elevated";
    }
  };

  const getPaddingClass = () => {
    switch (padding) {
      case "none":
        return "card-padding-none";
      case "small":
        return "card-padding-small";
      case "large":
        return "card-padding-large";
      case "default":
      default:
        return "card-padding-default";
    }
  };

  return (
    <div
      className={`card ${getVariantClass()} ${getPaddingClass()} ${className} ${
        onClick ? "card-clickable" : ""
      }`}
      onClick={onClick}
      {...rest}
    >
      {(title || subtitle) && (
        <div className="card-header">
          {title && <h3 className="card-title">{title}</h3>}
          {subtitle && <p className="card-subtitle">{subtitle}</p>}
        </div>
      )}

      <div className="card-content">{children}</div>

      {footer && <div className="card-footer">{footer}</div>}
    </div>
  );
};

export default Card;

