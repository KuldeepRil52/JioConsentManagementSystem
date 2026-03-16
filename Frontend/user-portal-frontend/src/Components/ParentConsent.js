import React, { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import {
  captureParentConsent,
  getConsentMetaDataForParent,
} from "../store/actions/CommonAction";
import { useDispatch } from "react-redux";
import "../Styles/toast.css";
import "../Styles/loader.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";

const ParentConsent = () => {
  const location = useLocation();
  const dispatch = useDispatch();

  const [loading, setLoading] = useState(false);
  const [statusIcon, setStatusIcon] = useState(null);

  const queryParams = new URLSearchParams(location.search);
  const code = queryParams.get("code");
  const state = queryParams.get("state");

  let tenantId = "";
  let consentMetaId = "";

  if (state) {
    const parts = state.split(":");
    tenantId = parts[0];
    consentMetaId = state;
  }

  useEffect(() => {
    const fetchData = async () => {
      if (tenantId && consentMetaId) {
        setLoading(true);
        setStatusIcon(null);
        try {
          const status = await dispatch(
            getConsentMetaDataForParent(consentMetaId, tenantId, code, state)
          );

          if (status === 200) {
            const captureStatus = await dispatch(
              captureParentConsent(tenantId, code, state)
            );

            if (captureStatus === 200 || captureStatus === 201) {
              setStatusIcon("success");
            } else {
              setStatusIcon("error");
            }
          } else if (status === 404) {
            toast.error(
              (props) => (
                <CustomToast
                  {...props}
                  type="error"
                  message={"Data Not Found."}
                />
              ),
              { icon: false }
            );
            setStatusIcon("error");
          } else {
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
            setStatusIcon("error");
          }
        } catch (error) {
          console.log("Unexpected error:", error);
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
          setStatusIcon("error");
        } finally {
          setLoading(false);
        }
      }
    };

    fetchData();
  }, [tenantId, consentMetaId, dispatch]);

  useEffect(() => {
    if (statusIcon === "error" || statusIcon === "success") {
      const timer = setTimeout(() => {
        window.close();
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [statusIcon]);

  return (
    <>
      <div className="pageConfig"></div>
      {(loading || statusIcon) && (
        <div className="overlay">
          {loading && <div className="loader"></div>}
          {statusIcon === "success" && <div className="success-icon">✔</div>}
          {statusIcon === "error" && <div className="error-icon">✖</div>}
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
    </>
  );
};

export default ParentConsent;
