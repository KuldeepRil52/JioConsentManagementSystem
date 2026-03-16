import {
  ActionButton,
  Card,
  Icon,
  Image,
  InputCheckbox,
  InputCode,
  InputField,
  InputFieldV2,
  Skeleton,
  Text,
} from "../custom-components";
import React from "react";
import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/adminLogin.css";
import {
  generateloginOtp,
  getLoginUserProfile,
  getUserProfile,
  verifyLoginOtp,
} from "../store/actions/CommonAction";
import { useDispatch, useSelector } from "react-redux";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import { disableSandboxMode } from "../utils/sandboxMode";
import config from "../utils/config";
import { makeAPICall } from "../utils/ApiCall";

// UUID generator function (same as in CommonAction.js)
const uuidv4 = () => {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0,
      v = c == "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

const AdminLogin = () => {
  const jioLogo = new URL("../assets/jio-logo.png", import.meta.url).href;

  // Clear sandbox mode when user accesses login page
  useEffect(() => {
    disableSandboxMode();
    console.log('🚪 Entering login page - Sandbox mode cleared');
    
    // Prevent back navigation after logout
    window.history.pushState(null, '', window.location.href);
    const handlePopState = () => {
      window.history.pushState(null, '', window.location.href);
    };
    window.addEventListener('popstate', handlePopState);
    
    return () => {
      window.removeEventListener('popstate', handlePopState);
    };
  }, []);

  const [emailMobile, setEmailMobile] = useState("");
  const [values, setValues] = useState(null);
  const [panNo, setPanNo] = useState("");
  const [isPanValid, setIsPanValid] = useState(null);
  const [emailMobileValid, setEmailMobileValid] = useState(null);
  const [isOtpValid, setIsOtpValid] = useState(false);
  const [initialPan, setInitialPan] = useState(true);
  const [initTxnId, setInitTxnId] = useState("");
  const [initalOtp, setInitialOtp] = useState(true);
  const [otp, setOtp] = useState("");
  const [showOtpModal, setShowOtpModal] = useState(false);
  const [totp, setTotp] = useState("");
  const [showTotpModal, setShowTotpModal] = useState(false);
  const [isTotpValid, setIsTotpValid] = useState(false);
  const [initialTotp, setInitialTotp] = useState(true);
  const [retryCountMsg, setRetryCountMsg] = useState("");
  const dispatch = useDispatch();
  const panRegex = /^[A-Z]{5}[0-9]{4}[A-Z]$/;
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const mobileRegex = /^[6-9]\d{9}$/;
  const clientIdRegex = /^CLT(?:[0-9]{2})(?:0[1-9]|1[0-2])[A-Z0-9]{4}[A-F0-9]$/;
  const [idType, setIdType] = useState(""); // "PAN" or "CLIENT_ID"

  const emailMob = useSelector((state) => state.common.login_email_mobile);
  const panNumber = useSelector((state) => state.common.login_pan_no);
  const navigate = useNavigate();
  const [counter, setCounter] = useState(60);
  const [isResendDisabled, setIsResendDisabled] = useState(true);
  const [disableAllFields, setDisableAllFields] = useState(false);
  const [isVerifyingOtp, setIsVerifyingOtp] = useState(false);
  const [isVerifyingTotp, setIsVerifyingTotp] = useState(false);

  // Refs for input fields
  const panInputRef = useRef(null);
  const emailMobileInputRef = useRef(null);
  const otpInputRef = useRef(null);
  const totpInputRef = useRef(null);

  // Tab navigation now works natively - no need for custom handling
  // useEffect(() => {
  //   if (showOtpModal || showTotpModal) return;

  //   const panInput = panInputRef.current?.querySelector('input');

  //   const handlePanTab = (e) => {
  //     if (e.key === 'Tab' && !e.shiftKey) {
  //       e.preventDefault();
  //       setTimeout(() => {
  //         const emailInput = emailMobileInputRef.current?.querySelector('input');
  //         if (emailInput) {
  //           emailInput.focus();
  //         }
  //       }, 0);
  //     }
  //   };

  //   if (panInput) {
  //     panInput.addEventListener('keydown', handlePanTab);
  //     return () => {
  //       panInput.removeEventListener('keydown', handlePanTab);
  //     };
  //   }
  // }, [showOtpModal, showTotpModal]);

  // Tab navigation now works natively for OTP modal
  // useEffect(() => {
  //   if (!showOtpModal) return;

  //   const handleOtpTab = (e) => {
  //     if (e.key === 'Tab' && !e.shiftKey) {
  //       e.preventDefault();
  //       setTimeout(() => {
  //         const verifyButton = document.querySelector('.otp-button button');
  //         if (verifyButton) {
  //           verifyButton.focus();
  //         }
  //       }, 0);
  //     }
  //   };

  //   const otpInput = otpInputRef.current?.querySelector('input');
  //   if (otpInput) {
  //     otpInput.addEventListener('keydown', handleOtpTab);
  //     return () => {
  //       otpInput.removeEventListener('keydown', handleOtpTab);
  //     };
  //   }
  // }, [showOtpModal]);

  // Tab navigation now works natively for TOTP modal
  // useEffect(() => {
  //   if (!showTotpModal) return;

  //   const handleTotpTab = (e) => {
  //     if (e.key === 'Tab' && !e.shiftKey) {
  //       e.preventDefault();
  //       setTimeout(() => {
  //         const verifyButton = document.querySelector('.otp-button button');
  //         if (verifyButton) {
  //           verifyButton.focus();
  //         }
  //       }, 0);
  //     }
  //   };

  //   const totpInput = totpInputRef.current?.querySelector('input');
  //   if (totpInput) {
  //     totpInput.addEventListener('keydown', handleTotpTab);
  //     return () => {
  //       totpInput.removeEventListener('keydown', handleTotpTab);
  //     };
  //   }
  // }, [showTotpModal]);

  // const handlePanChange = (e) => {
  //   const value = e.target.value.toUpperCase();

  //   setPanNo(value);

  //   if (panRegex.test(value)) {
  //     setIsPanValid(true);
  //   } else {
  //     setIsPanValid(false);
  //   }
  // };

  const handlePanChange = (e) => {
    const value = e.target.value.toUpperCase();
    setPanNo(value);

    if (panRegex.test(value) || clientIdRegex.test(value)) {
      setIsPanValid(true);
    } else {
      setIsPanValid(false);
    }
  };




  const handleOtpChange = (e) => {
    const value = e.target.value;

    setOtp(value);

    if (value && value.length === 6) {
      setIsOtpValid(true);
    } else {
      setIsOtpValid(false);
    }
  };

  const handleEmailMobileChange = (e) => {
    const value = e.target.value;
    setEmailMobile(value);

    if (emailRegex.test(value)) {
      setEmailMobileValid(true);
      // setEmailMobile(value.toUpperCase());
      setValues(1); // email
    } else if (mobileRegex.test(value)) {
      setEmailMobileValid(true);
      setValues(0); // mobile
    } else {
      setEmailMobileValid(false);
      setValues(null);
    }
  };

  const startTimer = () => {
    setIsResendDisabled(true); // Disable "Resend OTP" while counting down
    const timer = setInterval(() => {
      setCounter((prevCounter) => {
        if (prevCounter === 0) {
          clearInterval(timer);
          setIsResendDisabled(false); // Enable button when timer completes
          return 60;
        }
        return prevCounter - 1; // decrement normally
      });
    }, 1000);
  };
  const handleOtpSubmit = async () => {
    if (otp.length !== 6) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Invalid OTP. Please try again."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (otp === "" || otp === null || otp === undefined) {
      toast.error(
        (props) => (
          <CustomToast {...props} type="error" message={"Please enter OTP."} />
        ),
        { icon: false }
      );
      return;
    }

    // Show loading state briefly before transitioning to TOTP
    setIsVerifyingOtp(true);
    
    // Simulate verification delay for better UX
    setTimeout(() => {
      setIsOtpValid(true);
      setIsVerifyingOtp(false);
      setShowTotpModal(true);
    }, 500);
  };

  // Function to log audit event on login
  const logAuditEvent = async (tenantId, businessId, userId, sessionToken, panNo) => {
    if (!tenantId || !businessId) {
      console.warn('Missing tenantId or businessId for audit log');
      return;
    }

    try {
      const txnId = uuidv4();
      const transactionId = uuidv4();
      
      // Get client IP address (using placeholder as per user's request to keep values static)
      const ipAddress = "192.168.1.1";

      const headers = {
        txn: txnId,
        "X-Tenant-ID": tenantId,
        "X-Business-ID": businessId,
        "X-Transaction-ID": transactionId,
        "Content-Type": "application/json",
      };

      // Build initiator string with PAN
      const initiator = "Data Fiduciary";

      const body = {
        actor: {
          id: "PAN No./Client ID", // Use userId if available, otherwise default
          role: "Legal-Admin",
          type: "USER"
        },
        businessId: businessId,
        group: "Consent",
        component: "Jio Consent Management System",
        actionType: "LOGIN",
        resource: {
          type: "userId",
          id: userId || txnId // Use userId if available, otherwise default
        },
        initiator: initiator,
        context: {
          txnId: transactionId,
          ipAddress: ipAddress
        },
        extra: {
          legalEntityName: "XYZ Pvt Ltd",
          pan: panNo || ""
        }
      };

      const response = await makeAPICall(
        config.get_audit_reports,
        "POST",
        body,
        headers
      );

      console.log('✅ Audit log created successfully:', response);
      return response;
    } catch (error) {
      console.error('❌ Error creating audit log:', error);
      throw error;
    }
  };

  const handleTotpSubmit = async () => {
    if (totp.length !== 6) {
      setInitialTotp(false);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please enter a valid 6-digit TOTP."}
          />
        ),
        { icon: false }
      );
      setIsTotpValid(false);
      return;
    }

    if (totp === "" || totp === null || totp === undefined) {
      setInitialTotp(false);
      toast.error(
        (props) => (
          <CustomToast {...props} type="error" message={"Please enter TOTP."} />
        ),
        { icon: false }
      );
      setIsTotpValid(false);
      return;
    }

    setIsVerifyingTotp(true);
    try {
      let res = await dispatch(
        verifyLoginOtp(panNo, otp, emailMobile, values, initTxnId, totp)
      );

      if (res.status == 200) {
        setIsTotpValid(true);
        
        // Get data from response for audit log
        const tenantId = res?.data?.tenantId || "";
        const userId = res?.data?.userId || "";
        const sessionToken = res?.data?.xsessionToken || "";
        // For login, businessId is same as tenantId
        const businessId = tenantId;
        
        // Call audit API after successful login
        try {
          await logAuditEvent(tenantId, businessId, userId, sessionToken, panNo);
        } catch (error) {
          console.error('Failed to log audit event:', error);
          // Continue with login even if audit fails
        }
        
        // Clear sandbox mode on successful login
        disableSandboxMode();
        console.log('✅ Login successful - Sandbox mode cleared');
        
        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message={"OTP Verified Successfully."}
            />
          ),
          { icon: false }
        );
        setDisableAllFields(true);
        setTimeout(() => {
          navigate("/dpo-dashboard");
        }, 2000);
      }
      setIsVerifyingTotp(false);
    } catch (error) {
      setIsVerifyingTotp(false);
      setInitialTotp(false);
      toast.error(
        (props) => (
          <CustomToast {...props} type="error" message={error.message} />
        ),
        { icon: false }
      );
      return;
    }
  };

  const handleResend = async (e) => {
    e.preventDefault();

    try {
      const res = await dispatch(generateloginOtp(panNo, values, emailMobile));
      if (res.status === 200 || res.status == 201) {
        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message={"OTP Sent successfully."}
            />
          ),
          { icon: false }
        );
        setOtp("");
        setInitTxnId(res.data.txnId);
        startTimer();
      }
    } catch (err) {
      const error = err?.response?.data || err;

      toast.error(
        (props) => (
          <CustomToast {...props} type="error" message={error[0].errorMsg} />
        ),
        { icon: false }
      );
      return;
    }
  };
  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      // alert("handleSubmit");
      const res = await dispatch(generateloginOtp(panNo, values, emailMobile));

      if (res.status === 200 || res.status == 201) {
        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message={"OTP Sent successfully."}
            />
          ),
          { icon: false }
        );
        setInitTxnId(res.data.txnId);
        setShowOtpModal(true);
        setCounter(60);
        setIsResendDisabled(true);
        startTimer();
      }
    } catch (err) {
      const error = err;

      toast.error(
        (props) => (
          <CustomToast {...props} type="error" message={error.errorMsg} />
        ),
        { icon: false }
      );
      return;
    }
  };

  return (
    <>
      <ToastContainer
        position="bottom-left"
        autoClose={3000}
        hideProgressBar
        closeOnClick
        pauseOnHover
        draggable
        closeButton={false}
        toastClassName={() => "toast-wrapper"}
        transition={Slide}
      />
      {!showOtpModal && (
        <div className="login-page">
          <div className="login-card">
            <div className="logoDiv">
              <div className="logoStyle">
                <img src={jioLogo} alt="Jio Logo" style={{ height: '40px', width: '40px', display: 'block' }} />
              </div>
              <Text appearance="heading-xxs">Consent Management</Text>
            </div>
            <br></br>

            <div className="adminLogin-header">
              <Text appearance="heading-s">Log In</Text>
            </div>

            <div ref={panInputRef}>
              <InputFieldV2
                label="Company's PAN/Client ID (Required)"
                value={panNo}
                onChange={handlePanChange}
                autoFocus={true}
                maxLength={13}
                size="medium"
                state={
                  isPanValid ? "success" : isPanValid === false ? "error" : "none"
                }
                tabIndex={1}
              // stateText={isPanValid ? "" : "Please enter a valid PAN Number"}
            >
              
            </InputFieldV2>
            {/* <Icon
              color="primary"
              ic="IcLanguage"
              kind="default"
              onClick={function noRefCheck() { }}
              size="medium"
            /> */}
              <br></br>
            <div ref={emailMobileInputRef}>
            <InputFieldV2
              label="Email/Mobile number (Required)"
              onChange={handleEmailMobileChange}
              size="medium"
              state={
                emailMobileValid
                  ? "success"
                  : emailMobileValid === false
                  ? "error"
                  : "none"
              }
              // stateText={
              //   emailMobileValid
              //     ? ""
              //     : "Please enter a valid Email or Mobile Number"
              // }
              ></InputFieldV2>
            </div>
            </div>
            <button 
              className="continue-btn" 
              onClick={handleSubmit}
              disabled={!isPanValid || !emailMobileValid}
            >
              Continue
            </button>
            <br></br>
            <p className="terms-text" style={{ textAlign: "center", fontWeight: '600' }}>
              Don't have an account?
              <span
              className="focusable-link"
              tabIndex="0"
                style={{ color: "#007bff", cursor: "pointer" }}
                onClick={() => navigate("/signup")}
                onKeyDown={(e) => {
                if (e.key === "Enter" || e.key === " ") {
                  navigate("/signup");   // Enter or Space triggers the click
                }
              }}
              >{" "}
                Sign Up
              </span>
            </p>
          </div>
        </div>
      )}

      {showOtpModal && !showTotpModal && (
        <div className="login-page">
          <div className="login-card">
            <div className="logoDiv">
              <div className="logoStyle">
                <img src={jioLogo} alt="Jio Logo" style={{ height: '40px', width: '40px', display: 'block' }} />
              </div>
              <Text appearance="heading-xxs">Consent Management</Text>
            </div>
            <div className="otp-part">
              <div className="container-header-otp">
                <Text appearance="heading-xs">OTP verification</Text>
              </div>

              <div className="enter-otp">
                <Text appearance="body-xs" color="primary-grey-80">
                  Enter the 6-digit OTP sent to {emailMobile}
                </Text>
              </div>
              <br></br>

              <div ref={otpInputRef}>
                <div style={{ display: 'flex', gap: '12px', justifyContent: 'center', marginBottom: '8px' }}>
                  {[0, 1, 2, 3, 4, 5].map((index) => (
                    <input
                      key={index}
                      type="text"
                      inputMode="numeric"
                      maxLength={1}
                      value={otp[index] || ''}
                      disabled={(isOtpValid && !initalOtp) || disableAllFields}
                      autoFocus={index === 0}
                      style={{
                        width: '48px',
                        height: '48px',
                        textAlign: 'center',
                        fontSize: '20px',
                        fontWeight: '600',
                        border: `2px solid ${!initalOtp && !isOtpValid ? '#dc2626' : '#e5e7eb'}`,
                        borderRadius: '8px',
                        outline: 'none',
                        transition: 'border-color 0.2s',
                      }}
                      onFocus={(e) => e.target.style.borderColor = '#0066FF'}
                      onBlur={(e) => e.target.style.borderColor = !initalOtp && !isOtpValid ? '#dc2626' : '#e5e7eb'}
                      onChange={(e) => {
                        const value = e.target.value.replace(/[^0-9]/g, '');
                        if (value) {
                          const newOtp = otp.split('');
                          newOtp[index] = value;
                          const updatedOtp = newOtp.join('');
                          setOtp(updatedOtp);
                          
                          // Auto-focus next input
                          if (index < 5 && value) {
                            const nextInput = e.target.parentElement?.children[index + 1];
                            if (nextInput) nextInput.focus();
                          }
                        }
                      }}
                      onKeyDown={(e) => {
                        if (e.key === 'Backspace') {
                          if (otp[index]) {
                            // Clear current field
                            const newOtp = otp.split('');
                            newOtp[index] = '';
                            setOtp(newOtp.join(''));
                          } else if (index > 0) {
                            // If current field is empty, move to previous and clear it
                            const newOtp = otp.split('');
                            newOtp[index - 1] = '';
                            setOtp(newOtp.join(''));
                            const prevInput = e.target.parentElement?.children[index - 1];
                            if (prevInput) prevInput.focus();
                          }
                        }
                      }}
                      onPaste={(e) => {
                        e.preventDefault();
                        const pastedData = e.clipboardData.getData('text').replace(/[^0-9]/g, '').slice(0, 6);
                        setOtp(pastedData);
                        // Focus last filled input
                        const lastIndex = Math.min(pastedData.length - 1, 5);
                        const inputs = e.target.parentElement?.children;
                        if (inputs && inputs[lastIndex]) {
                          inputs[lastIndex].focus();
                        }
                      }}
                    />
                  ))}
                </div>
                {!initalOtp && !isOtpValid && (
                  <p style={{ color: '#dc2626', fontSize: '12px', textAlign: 'center', margin: '0' }}>
                    Please enter a valid OTP
                  </p>
                )}
              </div>
              <br></br>
              <p style={{ textAlign: "center", fontWeight: '600', marginTop: '8px' }}>
                <span
                  style={{ color: "#007bff", cursor: "pointer" }}
                  onClick={() => {
                    setShowOtpModal(false);
                    setOtp("");
                    setIsOtpValid(false);
                    setInitialOtp(true);
                  }}
                >
                  Go back
                </span>
              </p>
              <p style={{ textAlign: "center", fontWeight: "600", marginTop: "8px" }}>
              <span
                onClick={
                  isResendDisabled || disableAllFields
                    ? undefined // ⛔ disables click completely
                    : handleResend // ✅ only works when enabled
                }
                style={{
                  color: "#000093",
                  cursor: isResendDisabled || disableAllFields ? "not-allowed" : "pointer",
                  opacity: isResendDisabled || disableAllFields ? 0.5 : 1,
                  // textDecoration: "none", // ❌ no underline by default
                }}
                className="resend-otp-link"
              >
                Resend OTP
              </span>
            </p>




              <div className="resend-otp-outer-div">
                {isResendDisabled && counter > 0 && (
                  <div>
                    <Text appearance="body-s-bold" color="primary-80">
                      {counter} Seconds
                    </Text>
                  </div>
                )}
              </div>
              <br></br>
              <div className="otp-button" style={{ width: '100%' }}>
                <ActionButton
                  label="Verify"
                  onClick={handleOtpSubmit}
                  stretch={true}
                  maxLength={6}
                  state={
                    isVerifyingOtp ? "loading" : (otp.length !== 6 || disableAllFields ? "disabled" : "normal")
                  }
                  tabIndex={2}
                  style={{ width: '100%' }}
                ></ActionButton>
              </div>
            </div>
          </div>
        </div>
      )}

      {showTotpModal && (
        <div className="login-page">
          <div className="login-card">
            <div className="logoDiv">
              <div className="logoStyle">
                <img src={jioLogo} alt="Jio Logo" style={{ height: '40px', width: '40px', display: 'block' }} />
              </div>
              <Text appearance="heading-xxs">Consent Management</Text>
            </div>
            <div className="otp-part">
              <div className="container-header-otp">
                <Text appearance="heading-xs">Verify with Authenticator App</Text>
              </div>

              <div className="enter-otp">
                <Text appearance="body-xs" color="primary-grey-80">
                  Enter the 6-digit code from your Authenticator app
                </Text>
              </div>
              <br></br>

              <div ref={totpInputRef}>
                <div style={{ display: 'flex', gap: '12px', justifyContent: 'center', marginBottom: '8px' }}>
                  {[0, 1, 2, 3, 4, 5].map((index) => (
                    <input
                      key={index}
                      type="text"
                      inputMode="numeric"
                      maxLength={1}
                      value={totp[index] || ''}
                      disabled={(isTotpValid && !initialTotp) || disableAllFields}
                      autoFocus={index === 0}
                      style={{
                        width: '48px',
                        height: '48px',
                        textAlign: 'center',
                        fontSize: '20px',
                        fontWeight: '600',
                        border: '2px solid #e5e7eb',
                        borderRadius: '8px',
                        outline: 'none',
                        transition: 'border-color 0.2s',
                      }}
                      onFocus={(e) => e.target.style.borderColor = '#0066FF'}
                      onBlur={(e) => e.target.style.borderColor = '#e5e7eb'}
                      onChange={(e) => {
                        const value = e.target.value.replace(/[^0-9]/g, '');
                        if (value) {
                          const newTotp = totp.split('');
                          newTotp[index] = value;
                          const updatedTotp = newTotp.join('');
                          setTotp(updatedTotp);
                          
                          // Auto-focus next input
                          if (index < 5 && value) {
                            const nextInput = e.target.parentElement?.children[index + 1];
                            if (nextInput) nextInput.focus();
                          }
                        }
                      }}
                      onKeyDown={(e) => {
                        if (e.key === 'Backspace') {
                          if (totp[index]) {
                            // Clear current field
                            const newTotp = totp.split('');
                            newTotp[index] = '';
                            setTotp(newTotp.join(''));
                          } else if (index > 0) {
                            // If current field is empty, move to previous and clear it
                            const newTotp = totp.split('');
                            newTotp[index - 1] = '';
                            setTotp(newTotp.join(''));
                            const prevInput = e.target.parentElement?.children[index - 1];
                            if (prevInput) prevInput.focus();
                          }
                        }
                      }}
                      onPaste={(e) => {
                        e.preventDefault();
                        const pastedData = e.clipboardData.getData('text').replace(/[^0-9]/g, '').slice(0, 6);
                        setTotp(pastedData);
                        // Focus last filled input
                        const lastIndex = Math.min(pastedData.length - 1, 5);
                        const inputs = e.target.parentElement?.children;
                        if (inputs && inputs[lastIndex]) {
                          inputs[lastIndex].focus();
                        }
                      }}
                    />
                  ))}
                </div>
              </div>
              <br></br>
              <p style={{ textAlign: "center", fontWeight: '600', marginTop: '8px' }}>
                <span
                  style={{ color: "#007bff", cursor: "pointer" }}
                  onClick={() => {
                    setShowTotpModal(false);
                    setTotp("");
                    setIsTotpValid(false);
                    setInitialTotp(true);
                  }}
                >
                  Go back
                </span>
              </p>
              <br></br>
              <div className="otp-button" style={{ width: '100%' }}>
                <ActionButton
                  label="Verify"
                  onClick={handleTotpSubmit}
                  stretch={true}
                  maxLength={6}
                  state={
                    isVerifyingTotp ? "loading" : (totp.length !== 6 || disableAllFields ? "disabled" : "normal")
                  }
                  tabIndex={2}
                  style={{ width: '100%' }}
                ></ActionButton>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default AdminLogin;
