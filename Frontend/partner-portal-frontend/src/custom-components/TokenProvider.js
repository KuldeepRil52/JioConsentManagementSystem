import React from "react";

const TokenProvider = ({ children, ...rest }) => {
  return <div {...rest}>{children}</div>;
};

export default TokenProvider;

