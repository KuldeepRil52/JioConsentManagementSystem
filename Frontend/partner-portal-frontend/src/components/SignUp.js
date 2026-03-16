import React, { useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import CustomToast from "./CustomToastContainer";
import { useNavigate } from "react-router-dom";
import "../styles/signup.css";
import { ActionButton, InputFieldV2, Modal, Text } from "../custom-components";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import { CLEAR_SESSION } from "../store/constants/Constants";
import "../styles/sessionModal.css";
import { Link } from "react-router-dom";
import {
  triggerOtp,
  submitFormData,
  verifyOtp,
} from "../store/actions/CommonAction";

// PDFs are now accessed via routes: /pdf/terms-of-use and /pdf/privacy-policy

const SignUp = () => {
  const navigate = useNavigate();
  const jioLogo = new URL("../assets/jio-logo.png", import.meta.url).href;

  const [step, setStep] = useState("signup");
  const [emailMobile, setEmailMobile] = useState("");
  const [panNo, setPanNo] = useState("");
  const [isPanValid, setIsPanValid] = useState(null);
  const [emailMobileValid, setEmailMobileValid] = useState(null);
  const [isOtpValid, setIsOtpValid] = useState(false);
  const [initialPan, setInitialPan] = useState(true);
  const [initalOtp, setInitialOtp] = useState(true);
  const [otp, setOtp] = useState("");
  const [showOtpModal, setShowOtpModal] = useState(false);
  const [companyName, setCompanyName] = useState("");
  const [spocName, setSpocName] = useState("");
  const [isCompanyNameValid, setIsCompanyNameValid] = useState(true);
  const [isSpocNameValid, setIsSpocNameValid] = useState(true);
  const [showSessionModal, setShowSessionModal] = useState(false);
  const [secretCode, setSecretCode] = useState("");

  const [counter, setCounter] = useState(60);
  const [isResendDisabled, setIsResendDisabled] = useState(true);
  const [isVerifying, setIsVerifying] = useState(false);

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

  const dispatch = useDispatch();

  const txn = useSelector((state) => state.common.signup_txnid);

  const [formData, setFormData] = useState({
    companyName: "",
    logoUrl: "",
    pan: "",
    spoc: {
      name: "",
      mobile: "",
      email: "",
    },
    identityType: "",
  });

  // const [formErrors, setFormErrors] = useState({
  //   companyName: null,
  //   logoUrl: null,
  //   pan: null,
  //   spocName: null,
  //   spocMobile: null,
  //   spocEmail: null,
  // });

  const [formErrors, setFormErrors] = useState({
    companyName: null,
    logoUrl: null,
    pan: null,
    spoc: {
      name: null,
      mobile: null,
      email: null,
    },
  });

  const handlePanChange = (e) => {
    const value = e.target.value.toUpperCase();
    const panRegex = /^[A-Z]{5}[0-9]{4}[A-Z]{1}$/; // standard PAN format

    // update formData
    setFormData((prev) => ({
      ...prev,
      pan: value,
    }));


    // update validation in formErrors
    if (value === "" || panRegex.test(value)) {
      setIsPanValid(true);
      setFormErrors((prev) => ({ ...prev, pan: null }));
    } else {
      setIsPanValid(false);
      setFormErrors((prev) => ({
        ...prev,
        pan: "Please enter a valid PAN number",
      }));
    }
  };

  const handleEmailMobileChange = (e) => {
    const value = e.target.value.trim();
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    const mobileRegex = /^[6-9]\d{9}$/; // Indian 10-digit mobile numbers

    // always update input value (keep in one place so typing works)
    setFormData((prev) => ({
      ...prev,
      spoc: { ...prev.spoc, emailMobile: value }, // <-- temporary field for input
    }));

    // validation + mapping to actual email/mobile fields
    if (emailRegex.test(value)) {
      setFormData((prev) => ({
        ...prev,
        spoc: { ...prev.spoc, email: value, mobile: "", emailMobile: value },
        identityType: "EMAIL",
      }));
      setFormErrors((prev) => ({ ...prev, emailMobile: null }));
      return;
    }

    if (/^\d{10}$/.test(value)) {
      if (mobileRegex.test(value)) {
        setFormData((prev) => ({
          ...prev,
          spoc: { ...prev.spoc, mobile: value, email: "", emailMobile: value },
          identityType: "MOBILE",
        }));
        setFormErrors((prev) => ({ ...prev, emailMobile: null }));
      } else {
        setFormErrors((prev) => ({
          ...prev,
          emailMobile: "Invalid mobile number",
        }));
      }
      return;
    }
    setFormErrors((prev) => ({
      ...prev,
      emailMobile: "Please enter a valid Email or 10-digit Mobile Number",
    }));
  };

  const handleCompanyNameChange = (e) => {
    const value = e.target.value;
    // update formData
    setFormData((prev) => ({
      ...prev,
      companyName: value,
    }));

    // Check if value contains at least one alphanumeric character (letter or number)
    const hasAlphanumeric = /[A-Za-z0-9]/.test(value);
    const trimmedValue = value.trim();
    // Check if trimmed value starts and ends with alphanumeric character
    const startsWithAlphanumeric = trimmedValue.length > 0 && /[A-Za-z0-9]/.test(trimmedValue[0]);
    const endsWithAlphanumeric = trimmedValue.length > 0 && /[A-Za-z0-9]/.test(trimmedValue[trimmedValue.length - 1]);

    // update validation
    if (value.trim() === "" || value.length > 60) {
      setIsCompanyNameValid(false);
      setFormErrors((prev) => ({
        ...prev,
        companyName: "Please enter a valid Company Name",
      }));
    } else if (!hasAlphanumeric) {
      setIsCompanyNameValid(false);
      setFormErrors((prev) => ({
        ...prev,
        companyName: "Company Name must contain at least one letter or number",
      }));
    } else if (!startsWithAlphanumeric || !endsWithAlphanumeric) {
      setIsCompanyNameValid(false);
      setFormErrors((prev) => ({
        ...prev,
        companyName: "Company Name must start and end with a letter or number",
      }));
    } else {
      setFormErrors((prev) => ({ ...prev, companyName: null }));
      setIsCompanyNameValid(true);
    }
  };

  // const handleSpocNameChange = (e) => {
  //   const value = e.target.value;

  //   setFormData((prev) => ({
  //     ...prev,
  //     spoc: {
  //       ...prev.spoc,
  //       name: value,
  //     },
  //   }));

  //   if (value.trim() === "" || value.length > 30) {
  //     setIsSpocNameValid(true);
  //     setFormErrors((prev) => ({
  //       ...prev,
  //       spocName: "Please enter a valid SPOC Name",
  //     }));
  //   } else {
  //     setIsSpocNameValid(false);
  //     setFormErrors((prev) => ({
  //       ...prev,
  //       spocName: null,
  //     }));
  //   }
  // };

  const handleSpocNameChange = (e) => {
    const value = e.target.value;

    setFormData((prev) => ({
      ...prev,
      spoc: {
        ...prev.spoc,
        name: value,
      },
    }));

    if (value.trim() === "" || value.length > 30) {
      setIsSpocNameValid(true);
      setFormErrors((prev) => ({
        ...prev,
        spoc: {
          ...prev.spoc,
          name: "Please enter a valid Name",
        },
      }));
    } else {
      setIsSpocNameValid(false);
      setFormErrors((prev) => ({
        ...prev,
        spoc: {
          ...prev.spoc,
          name: null,
        },
      }));
    }
  };

  const handleOtpSubmit = async () => {
    if (otp.length !== 6) {
      setInitialOtp(false);
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
      setIsOtpValid(false);
      return;
    }

    if (otp === "" || otp === null || otp === undefined) {
      setInitialOtp(false);
      toast.error(
        (props) => (
          <CustomToast {...props} type="error" message={"Please enter OTP."} />
        ),
        { icon: false }
      );
      setIsOtpValid(false);
      return;
    }

    setIsVerifying(true);
    try {
      const verifyData = {
        idValue:
          formData.identityType === "MOBILE"
            ? formData.spoc.mobile
            : formData.spoc.email.toUpperCase(),
        txn: txn,
        otp: otp,
      };
      const verifyRes = await verifyOtp(verifyData);
      console.log("OTP verify response:", verifyRes);

      if (verifyRes?.status === "OTP validated successfully") {
        setIsOtpValid(true);
        
        // Extract and store secretCode from response
        const secretCodeValue = verifyRes?.secretCode;
        if (!secretCodeValue) {
          setIsVerifying(false);
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Secret code not received. Please try again."}
              />
            ),
            { icon: false }
          );
          return;
        }
        setSecretCode(secretCodeValue);
        
        // Get identity value (email or mobile)
        const identityValue = formData.identityType === "MOBILE"
          ? formData.spoc.mobile
          : formData.spoc.email.toUpperCase();
        
        try {
          const result = await dispatch(submitFormData(formData, secretCodeValue, identityValue));
          console.log("API response:", result);

          setIsVerifying(false);
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"Client Onboarded Successfully."}
              />
            ),
            { icon: false }
          );
          setTimeout(() => {
            navigate("/dpo-dashboard");
          }, 2000);
        }
        catch (error) {
          setIsVerifying(false);
          console.log("error from submitfromdata: ", error);

          if (error?.errorCd === "JCMP0001") {
            toast.error(
              (props) => (
                <CustomToast
                  {...props}
                  type="error"
                  message={"Company name already exists. Please use a different name."}
                />
              ),
              { icon: false }
            );
          }

          else if (
            error?.status === 500 &&
            error?.errorMsg?.includes("already exists in the system")
          ) {
            toast.error(
              (props) => (
                <CustomToast
                  {...props}
                  type="error"
                  message={"Company name already exists. Please use a different name."}
                />
              ),
              { icon: false }
            );
          }

          if (error.status === 409) {

            console.log("err msg: ", error.errors[0].errorMessage);
            toast.error(
              (props) => (
                <CustomToast {...props} type="error" message={error.errors[0].errorMessage} />
              ),
              { icon: false }
            );

          }
        }

      }
    } catch (error) {
      setIsVerifying(false);
      setInitialOtp(false);
      console.log("Error: ", error, error.type);
      if (error?.toString()?.includes("400")) {
        setIsOtpValid(false);
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
      }

      else {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Something went wrong. Please try again."}
            />
          ),
          { icon: false }
        );
      }


    }
  };


  const handleResendOtp = async () => {
    try {
      const requestData = {
        pan: formData.pan,
        idValue:
          formData.identityType === "MOBILE"
            ? formData.spoc.mobile
            : formData.spoc.email.toUpperCase(),
        idType: formData.identityType, // "MOBILE" or "EMAIL"
      };

      const response = await dispatch(triggerOtp(requestData));
      console.log("OTP trigger response:", response);
      setOtp("");
      setSecretCode(""); // Reset secret code when resending OTP
      startTimer();
      setIsOtpValid(false); // reset validation
      setInitialOtp(true);
    } catch (error) {
      console.log("Error triggering OTP:", error);
    }
  };

  // Handle document navigation to PDF viewer routes
  const handleTermsOfServiceClick = (e) => {
    e.preventDefault();
    navigate("/pdf/terms-of-use");
  };

  const handlePrivacyPolicyClick = (e) => {
    e.preventDefault();
    navigate("/pdf/privacy-policy");
  };

  const handleTriggerOtp = async () => {

    const name = formData.spoc?.name;
    const emailMobile = formData.spoc?.emailMobile;
    const companyName = formData.companyName;

    if (!companyName.trim()) {
      toast.error((props) => (
        <CustomToast {...props} type="error" message={"Company Name is required."} />
      ), { icon: false });
      return;
    } else if (companyName.length < 2) {
      toast.error((props) => (
        <CustomToast {...props} type="error" message={"Company Name must be at least 2 characters long"} />
      ), { icon: false });
      return;
    } else if (companyName.length > 50) {
      toast.error((props) => (
        <CustomToast {...props} type="error" message={"Company Name must not exceed 60 characters"} />
      ), { icon: false });
      return;
    } else if (!/[A-Za-z0-9]/.test(companyName)) {
      toast.error((props) => (
        <CustomToast {...props} type="error" message={"Company Name must contain at least one letter or number"} />
      ), { icon: false });
      return;
    }
    
    const trimmedCompanyName = companyName.trim();
    const startsWithAlphanumeric = trimmedCompanyName.length > 0 && /[A-Za-z0-9]/.test(trimmedCompanyName[0]);
    const endsWithAlphanumeric = trimmedCompanyName.length > 0 && /[A-Za-z0-9]/.test(trimmedCompanyName[trimmedCompanyName.length - 1]);
    
    if (!startsWithAlphanumeric || !endsWithAlphanumeric) {
      toast.error((props) => (
        <CustomToast {...props} type="error" message={"Company Name must start and end with a letter or number"} />
      ), { icon: false });
      return;
    }

    if (!name.trim()) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Name is required."}
          />
        ),
        { icon: false }
      );
    } else if (name.length < 2) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Name must be at least 2 characters long"}
          />
        ),
        { icon: false }
      );
    } else if (name.length > 30) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Name must not exceed 30 characters"}
          />
        ),
        { icon: false }
      );

    } else if (!/^[A-Za-z\s]+$/.test(name)) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Name should only contain alphabets and spaces"}
          />
        ),
        { icon: false }
      );
      return;

    }

    else if (!/^[A-Z]{5}[0-9]{4}[A-Z]{1}$/.test(formData.pan)) {
      toast.error(
        (props) => (
          <CustomToast {...props} type="error" message={"Enter valid PAN."} />
        ),
        { icon: false }
      );
      return;
    }

    else if (isPanValid === false) {

      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Enter valid PAN."}
          />
        ),
        { icon: false }
      );

    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    const mobileRegex = /^[6-9]\d{9}$/;

    if (!emailMobile) {
      toast.error((props) => (
        <CustomToast {...props} type="error" message={"Email or Mobile number is required."} />
      ), { icon: false });
      return;
    } else if (!emailRegex.test(emailMobile) && !mobileRegex.test(emailMobile)) {
      toast.error((props) => (
        <CustomToast {...props} type="error" message={"Please enter a valid Email or 10-digit Mobile number"} />
      ), { icon: false });
      return;
    }

    else if (
      !formData.spoc?.emailMobile ||
      !formData.pan ||
      !formData.companyName ||
      !formData.spoc?.name
    ) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please Fill Required Details."}
          />
        ),
        { icon: false }
      );
      return;
    }

    try {
      const requestData = {
        
        pan: formData.pan,
        idValue:
          formData.identityType === "MOBILE"
            ? formData.spoc.mobile
            : formData.spoc.email.toUpperCase(),
        idType: formData.identityType, // "MOBILE" or "EMAIL"
      };

      const response = await dispatch(triggerOtp(requestData));
      console.log("OTP trigger response:", response);
      setCounter(60);
      setIsResendDisabled(true);
      setStep("otp");
      startTimer();
    } catch (error) {
      console.log("Error triggering OTP:", error);
      if (
        error.response &&
        error.response.status === 409 &&
        error.response.data?.errors?.[0]?.parameter === "pan"
      ) {
        console.log("err msg: ", error.response.data.errors[0].errorMessage);
        toast.error(
          (props) => (
            <CustomToast {...props} type="error" message={error.response.data.errors[0].errorMessage} />
          ),
          { icon: false }
        );
      }
    }
  };

  return (
    <>
      {showSessionModal && (
        <div className="session-timeout-overlay">
          <div className="session-timeout-modal">
            <Text appearance="heading-s" color="feedback_error_50">
              Session Time Out
            </Text>
            <br></br>
            <Text appearance="body-s" color="primary-80">
              Your session has expired. Please log in again.
            </Text>
          </div>
        </div>
      )}
      <div className="signup-page">
        <div className="signup-card">
          <div className="logoDiv">
            <div className="logoStyle">
              <img src={jioLogo} alt="Jio Logo" style={{ height: '40px', width: '40px', display: 'block' }} />
            </div>
            <Text appearance="heading-xxs">Consent Management</Text>
          </div>

          {step === "signup" && (
            <div className="signup-part">
              <div className="container-header">
                <Text appearance="heading-xs">Create an account</Text>
              </div>

              <div className="container-input">
                <div className="container-input-1">
                  <InputFieldV2
                    label="Company's Name (Required)"
                    name="companyName"
                    size="small"
                    type="text"
                    value={formData.companyName}
                    onChange={handleCompanyNameChange}
                    autoFocus={true}
                    state={formErrors.companyName ? "error" : "none"}
                    stateText={formErrors.companyName || ""}
                  />
                </div>
                <div className="container-input-2">
                  <InputFieldV2
                    label="Company's PAN (Required)"
                    name="pan"
                    maxLength={10}
                    size="small"
                    type="text"
                    value={formData.pan}
                    onChange={handlePanChange}
                    state={
                      isPanValid ? "success" : isPanValid === false ? "error" : "none"
                    }
                    stateText={formErrors.pan || ""}
                  />
                </div>
                <div className="container-input-3">
                  {/* <InputFieldV2
                    label="Name (Required)"
                    name="name"
                    size="small"
                    type="text"
                    value={formData.spoc.name}
                    onChange={handleSpocNameChange}
                    // state={
                    //   isSpocNameValid
                    //     ? "success"
                    //     : isSpocNameValid === false
                    //     ? "error"
                    //     : "none"
                    // }
                    state={
                      isSpocNameValid ? "success" : isSpocNameValid === false ? "error" : "none"
                    }
                    stateText={
                      isSpocNameValid
                        ? ""
                        : "Please enter a valid Email or Mobile Number"
                    }
                  /> */}

                  <InputFieldV2
                    label="SPOC Name (Required)"
                    name="name"
                    size="small"
                    type="text"
                    value={formData.spoc.name}
                    onChange={handleSpocNameChange}
                    state={formErrors.spoc.name ? "error" : "none"}
                    stateText={formErrors.spoc.name || ""}
                  />
                </div>
                <div className="container-input-4">
                  <InputFieldV2
                    label="Email/Mobile number (Required)"
                    name="name"
                    size="small"
                    type="text"
                    value={formData.spoc.emailMobile || ""}
                    onChange={handleEmailMobileChange}
                    state={
                      formErrors.emailMobile === null
                        ? "success"
                        : formErrors.emailMobile
                          ? "error"
                          : "none"
                    }
                    stateText={formErrors.emailMobile || ""}
                  />
                </div>
              </div>

              <div className="container-links">
                <p className="terms-text">
                  By continuing, you agree to our
                  <a 
                    href="#" 
                    className="focus-link"
                    onClick={handleTermsOfServiceClick}
                    style={{ cursor: "pointer" }}
                  > Terms of Service</a> and
                  <a 
                    href="#" 
                    className="focus-link"
                    onClick={handlePrivacyPolicyClick}
                    style={{ cursor: "pointer" }}
                  > Privacy & Legal Policy</a>.
                </p>
              </div>
              <ActionButton
              label="Continue"
              onClick={handleTriggerOtp}
              stretch={true}
              style={{width: '100%'}}
              className="continue-btn"
             state={
              !formData.companyName ||
              !formData.pan ||
              !formData.spoc.name ||
              !formData.spoc.emailMobile ||
              formErrors.companyName ||
              formErrors.pan ||
              formErrors.spoc.name ||
              formErrors.emailMobile
                ? "disabled"
                : "normal"
            }

            ></ActionButton>
              <br></br>
              <p className="terms-text" style={{ textAlign: "center", fontWeight: '600' }}>
                Already have an account?
                <span
                className="focusable-link"
                tabIndex="0"
                style={{ color: "#007bff", cursor: "pointer" }}
                onClick={() => navigate("/adminLogin")}
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    navigate("/adminLogin");
                  }
                }}
              >
                Login
              </span>
              </p>
            </div>
          )}

          {step === "otp" && (
            <div className="otp-part">
              <div className="container-header-otp">
                <Text appearance="heading-xs">OTP verification</Text>
              </div>

              <div className="enter-otp">
                <Text appearance="body-xs" color="primary-grey-80">
                  Enter the 6-digit OTP sent to {formData.spoc.emailMobile || ""}
                </Text>
              </div>
              <br></br>

              <div>
                <div style={{ display: 'flex', gap: '12px', justifyContent: 'center', marginBottom: '8px' }}>
                  {[0, 1, 2, 3, 4, 5].map((index) => (
                    <input
                      key={index}
                      type="text"
                      inputMode="numeric"
                      maxLength={1}
                      value={otp[index] || ''}
                      disabled={isOtpValid && !initalOtp}
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
                  <p style={{ color: '#dc2626', fontSize: '12px', textAlign: 'center', margin: '0 0 8px 0' }}>
                    Please enter a valid OTP
                  </p>
                )}
              </div>
              <br></br>
              <p style={{ textAlign: "center", fontWeight: '600', marginTop: '8px' }}>
                <span
                  style={{ color: "#007bff", cursor: "pointer" }}
                  onClick={() => {
                    setStep("signup");
                    setOtp("");
                    setSecretCode("");
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
                  isResendDisabled
                    ? undefined // Block clicks in disabled mode
                    : handleResendOtp
                }
                style={{
                  color: "#000093",
                  cursor: isResendDisabled  ? "not-allowed" : "pointer",
                  opacity: isResendDisabled  ? 0.5 : 1,
                }}
                className={`resend-otp-link ${isResendDisabled  ? "disabled" : ""}`}
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
                  style={{ width: '100%' }}
                ></ActionButton>
              </div>
            </div>
          )}

        </div>
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
      </div>
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
    </>
  );
};

export default SignUp;
