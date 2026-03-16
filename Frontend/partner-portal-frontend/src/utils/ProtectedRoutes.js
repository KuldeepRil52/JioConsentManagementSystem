import { useSelector } from "react-redux";
import { Outlet, Navigate } from "react-router-dom";
import { useState, useEffect } from "react";
import AdminLogin from "../components/AdminLogin";
import { isSandboxMode } from "./sandboxMode";

const ProtectedRoutes = () => {
  const [loading, setLoading] = useState(true);
  const session_token = useSelector((state) => state.common.session_token);

  // decide user status - allow access if authenticated OR in sandbox mode
  const user = session_token !== "" || isSandboxMode();

  useEffect(() => {
    // simulate API/session validation delay
    const timer = setTimeout(() => setLoading(false), 1);
    return () => clearTimeout(timer);
  }, []);

  if (loading) {
    // Show loader overlay during validation
    return (
      <div
        style={{
          position: "fixed",
          top: 0,
          left: 0,
          width: "100%",
          height: "100%",
          backgroundColor: "white",
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          zIndex: 9999,
        }}
      >
        <div
          style={{
            width: "100px",
            height: "100px",
            border: "15px solid #ccc",
            borderTop: "15px solid #007bff",
            borderRadius: "50%",
            animation: "spin 1s linear infinite",
          }}
        />
        <style>
          {`
            @keyframes spin {
              0% { transform: rotate(0deg); }
              100% { transform: rotate(360deg); }
            }
          `}
        </style>
      </div>
    );
  }

  // after loading is complete
  if (user) {
    return <Outlet />;
  } else {
    return <Navigate to="/adminLogin" replace />;
  }
};

export default ProtectedRoutes;
