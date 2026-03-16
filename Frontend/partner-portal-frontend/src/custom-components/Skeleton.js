import React from "react";
import "./Skeleton.css";

const Skeleton = ({ width, height = "20px", variant = "rect", className = "" }) => {
  const getVariantClass = () => {
    switch (variant) {
      case "circle":
        return "skeleton-circle";
      case "text":
        return "skeleton-text";
      case "rect":
      default:
        return "skeleton-rect";
    }
  };

  return (
    <div
      className={`skeleton ${getVariantClass()} ${className}`}
      style={{ width, height }}
    ></div>
  );
};

export default Skeleton;

