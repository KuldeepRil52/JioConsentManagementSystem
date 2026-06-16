import React from "react";
import { useNavigate } from "react-router-dom";
import { ActionButton, Text } from '../custom-components';
import "../styles/unauthorized.css";

const Unauthorized = () => {
  const navigate = useNavigate();

  return (
    <div className="unauthorized-container">
      <div className="unauthorized-content">
        <div className="unauthorized-icon">
          <svg
            width="120"
            height="120"
            viewBox="0 0 24 24"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 11c-.55 0-1-.45-1-1V8c0-.55.45-1 1-1s1 .45 1 1v4c0 .55-.45 1-1 1zm1 4h-2v-2h2v2z"
              fill="#E53E3E"
            />
          </svg>
        </div>

        <Text className="unauthorized-title" weight="bold" size="xlarge">
          Access Denied
        </Text>

        <Text className="unauthorized-message" size="medium">
          You don't have permission to access this page.
        </Text>

        <Text className="unauthorized-submessage" size="small">
          Please contact your administrator if you believe this is an error.
        </Text>

        <div className="unauthorized-actions">
          <ActionButton
            onClick={() => navigate(-1)}
            variant="secondary"
            size="medium"
          >
            Go Back
          </ActionButton>
          <ActionButton
            onClick={() => navigate("/dashboard")}
            variant="primary"
            size="medium"
          >
            Go to Dashboard
          </ActionButton>
        </div>
      </div>
    </div>
  );
};

export default Unauthorized;

