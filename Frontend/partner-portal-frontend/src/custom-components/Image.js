import React from "react";
import "./Image.css";

const Image = ({
  src,
  alt = "",
  width,
  height,
  className = "",
  loading = "lazy",
  ...rest
}) => {
  return (
    <img
      src={src}
      alt={alt}
      width={width}
      height={height}
      loading={loading}
      className={`custom-image ${className}`}
      {...rest}
    />
  );
};

export default Image;

