import React, { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import "../Styles/toast.css";
import "../Styles/loaderOverlay.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";

const LoadPopUp = () => {
  const location = useLocation();
  const [loading, setLoading] = useState(true);
  let loadPreferenceCenter;

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const encodedData = params.get("details");

    if (process.env.REACT_APP_ENV === "dev") {
      loadPreferenceCenter = require("../Assests/ST/IntegrationFile").default;
    } else if (process.env.REACT_APP_ENV === "nonprod") {
      loadPreferenceCenter = require("../Assests/SIT/IntegrationFile").default;
    } else {
      throw new Error("Unknown environment: " + process.env.REACT_APP_ENV);
    }

    let tenantId = "";
    let businessId = "";
    let consentHandleId = "";
    let callBackUrl = "";
    let secureid = "";

    if (encodedData) {
      try {
        const decoded = atob(encodedData);
        const parsed = JSON.parse(decoded);

        tenantId = parsed.tenantId || "";
        businessId = parsed.businessId || "";
        consentHandleId = parsed.consentHandleId || "";
        callBackUrl = parsed.callBackUrl || "";
        secureid = parsed.secCode || "";
      } catch (err) {
        console.log("Error decoding Base64 data param:", err);
      }
    }

    const errors = [];

    if (!consentHandleId) errors.push("Consent Handle ID is missing");
    if (!tenantId) errors.push("Tenant ID is missing");
    if (!businessId) errors.push("Business ID is missing");
    if (!callBackUrl) errors.push("Callback URL is missing");
    if (!secureid) errors.push("Secure Code is missing");
    if (errors.length > 1) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={errors.map((err, index) => (
              <div key={index}>{`${index + 1}. ${err}`}</div>
            ))}
          />
        ),
        { icon: false }
      );
      return;
    } else if (errors.length === 1) {
      toast.error(
        (props) => <CustomToast {...props} type="error" message={errors[0]} />,
        { icon: false }
      );
      return;
    }

    setTimeout(() => {
      try {
        loadPreferenceCenter(
          consentHandleId,
          tenantId,
          businessId,
          callBackUrl,
          secureid
        );
        setLoading(false);
      } catch (err) {
        console.error("Error loading preference center:", err);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Failed to load preference center."}
            />
          ),
          { icon: false }
        );
      } finally {
        // setLoading(false);
      }
    }, 2000);
  }, [location.search]);
  useEffect(() => {
    return () => console.log("LoadPopUp unmounted");
  }, []);

  return (
    <div className="load-popup-container">
      {loading && (
        <div className="overlay">
          <div className="loader"></div>
          <p className="loader-text">Loading...</p>
        </div>
      )}

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
  );
};

export default LoadPopUp;
