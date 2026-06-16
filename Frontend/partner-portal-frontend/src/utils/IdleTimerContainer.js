import React, { useState, useEffect, useRef } from "react";
import { useIdleTimer } from "react-idle-timer";
import { useNavigate, useLocation } from "react-router-dom";
import { useDispatch } from "react-redux";
import { CLEAR_SESSION } from "../store/constants/Constants";
import { isSandboxMode } from "./sandboxMode";
import "../styles/sessionModal.css";

import { Text } from "../custom-components"; // adjust import if needed

const IdleTimerContainer = ({ children }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();
  const [showSessionModal, setShowSessionModal] = useState(false);
  const idleTimerRef = useRef(null);

  const handleOnIdle = () => {
    // Don't trigger idle timeout in sandbox mode
    if (isSandboxMode()) {
      console.log("🏖️ SANDBOX MODE - Idle timer disabled");
      return;
    }

    dispatch({ type: CLEAR_SESSION });

    // Show modal
    setShowSessionModal(true);

    // Redirect immediately after a brief delay
    setTimeout(() => {
      setShowSessionModal(false);
      navigate("/adminLogin");
    }, 100);
  };

  const handleOnActive = () => {
    console.log("User became active again");
  };

  // Disable idle timer completely in sandbox mode
  // Use a very long timeout (24 hours = 86,400,000 ms)
  const idleTimerConfig = isSandboxMode()
    ? {
      timeout: 86400000, // 24 hours (more than enough for sandbox session)
      onIdle: () => console.log("🏖️ SANDBOX MODE - Idle timer disabled"),
      onActive: () => { },
      debounce: 500,
      events: ['mousedown', 'mousemove', 'keypress', 'keydown', 'scroll', 'touchstart', 'click', 'wheel'],
      startOnMount: true,
      startManually: false,
    }
    : {
      timeout: 1000 * 60 * 45, // 45 minutes
      onIdle: handleOnIdle,
      onActive: handleOnActive,
      debounce: 500,
      // Track all user activity events
      events: ['mousedown', 'mousemove', 'keypress', 'keydown', 'scroll', 'touchstart', 'click', 'wheel'],
      // Start timer when component mounts
      startOnMount: true,
      // Don't require manual start
      startManually: false,
      // Cross-tab detection
      crossTab: false,
      // Sync timers across tabs (optional)
      syncTimers: false,
    };

  const { reset, pause, resume } = useIdleTimer(idleTimerConfig);

  // Store the reset function in ref for access
  useEffect(() => {
    idleTimerRef.current = { reset, pause, resume };
  }, [reset, pause, resume]);

  // Reset timer when route changes to ensure timer works on all pages
  useEffect(() => {
    if (!isSandboxMode() && idleTimerRef.current) {
      // Reset the timer when navigating to a new page
      idleTimerRef.current.reset();
    }
  }, [location.pathname]);

  return (
    <>
      {children}

      {/* Don't show session modal in sandbox mode */}
      {showSessionModal && !isSandboxMode() && (
        <div className="session-timeout-overlay">
          <div className="session-timeout-modal">
            <Text appearance="heading-s" color="feedback_error_50">
              Session Time Out
            </Text>
            <br />
            <Text appearance="body-s" color="primary-80">
              Your session has expired. Please log in again.
            </Text>
          </div>
        </div>
      )}
    </>
  );
};

export default IdleTimerContainer;
