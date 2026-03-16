//Dasboard.js
import { ActionButton, Image, Text } from "../custom-components";
import "../styles/pageConfiguration.css";
import "../styles/dashboard.css";
import { useNavigate } from "react-router-dom";
import { useSelector, useDispatch } from "react-redux";
import { useEffect, useState } from "react";
import { getUserProfile, fetchBusinessApplication, fetchBusinessApplications, translateConfig, getSystemConfig } from "../store/actions/CommonAction";
import { CLEAR_SESSION } from "../store/constants/Constants";
import { isSandboxMode } from "../utils/sandboxMode";
import "../styles/sessionModal.css";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import GuidedTour from "./GuidedTour";
import TourButton from "./TourButton";
import { dashboardTourSteps } from "../utils/tourSteps";

const Dashboard = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const dashboard = new URL("../assets/dashboard.svg", import.meta.url).href;
  const [showSessionModal, setShowSessionModal] = useState(false);
  
  const tenantId = useSelector((state) => state.common.tenant_id);
  const token = useSelector((state) => state.common.session_token);

  const handleConfigureSystem = () => {
    navigate("/systemConfiguration");
  };

  const handleCreateBusinessGroup = () => {
    navigate("/business");
  };

  const userProfile = useSelector((state) => state.common.user_profile);
  const businessId = useSelector((state) => state.common.business_id);

  useEffect(() => {
    if (!tenantId) {
      console.warn(" Tenant ID is missing in Redux, cannot call API.");
      return;
    }

    console.log(" Tenant ID from Redux:", tenantId);

    const fetchUserProfile = async () => {
      try {
        let res = await dispatch(getUserProfile(tenantId, token));
        console.log("Response in dashboard profile call", res);

        if (res === 401 || res?.status === 401) {
          // Skip session expiration handling in sandbox mode
          if (isSandboxMode()) {
            console.log('🏖️ SANDBOX MODE - Ignoring 401 error in Dashboard');
            return;
          }
          
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Session expired"}
              />
            ),
            { icon: false }
          );
          dispatch({ type: CLEAR_SESSION });
          setShowSessionModal(true);
          setTimeout(() => {
            setShowSessionModal(false);
            navigate("/adminLogin");
          }, 7000);
        }
      } catch (error) {
        console.error("Error in profile call", error);

        if (
          error === 401 ||
          error[0]?.errorCode === "JCMP4003" ||
          error[0]?.errorCode === "JCMP4001"
        ) {
          // Skip session expiration handling in sandbox mode
          if (isSandboxMode()) {
            console.log('🏖️ SANDBOX MODE - Ignoring session error in Dashboard');
            return;
          }
          
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Session expired"}
              />
            ),
            { icon: false }
          );
          dispatch({ type: CLEAR_SESSION });
          setShowSessionModal(true);
          setTimeout(() => {
            setShowSessionModal(false);
            navigate("/adminLogin");
          }, 7000);
        }
      }
    };

    const fetchCompanyLogo = async () => {
      try {
        const response = await dispatch(getSystemConfig());
        const systemConfigInfo = response?.data?.searchList?.[0]?.configurationJson;
        
        if (systemConfigInfo?.logo) {
          try {
            // Store logo as base64 data URL for persistence across page reloads
            let logoDataUrl;
            
            if (systemConfigInfo.logo.startsWith('data:')) {
              // Already a data URL
              logoDataUrl = systemConfigInfo.logo;
            } else {
              // Convert base64 to data URL
              const contentType = systemConfigInfo.logoMeta?.contentType || 'image/png';
              logoDataUrl = `data:${contentType};base64,${systemConfigInfo.logo}`;
            }
            
            // Dispatch logo to Redux
            dispatch({
              type: "SAVE_COMPANY_LOGO",
              payload: logoDataUrl,
            });
            console.log('✅ Company logo saved as base64 data URL');
          } catch (error) {
            console.error("Error processing logo:", error);
            // Fallback: use base64 directly if it's already a data URL
            if (systemConfigInfo.logo.startsWith('data:')) {
              dispatch({
                type: "SAVE_COMPANY_LOGO",
                payload: systemConfigInfo.logo,
              });
            }
          }
        }
      } catch (error) {
        console.error("Error fetching company logo:", error);
      }
    };

    fetchUserProfile();
    fetchCompanyLogo();

  }, [dispatch, tenantId]);

  // Fetch business details after business_id is set from user profile
  useEffect(() => {
    if (businessId && token) {
      console.log("Fetching business details for businessId:", businessId);
      
      const getBusinessDetails = async () => {
        try {
          await dispatch(fetchBusinessApplication(businessId, token));
          console.log("Business details fetched successfully");
        } catch (error) {
          console.error("Error fetching business details:", error);
        }
      };

      getBusinessDetails();
    }
  }, [businessId, token, dispatch]);

  useEffect(() => {
    if (userProfile) {
      console.log("📦 User profile from Redux:", userProfile);
      console.log("🏢 Extracted Business ID:", businessId);
    }
  }, [userProfile, businessId]);

      const [applications, setApplications] = useState([]);
  

  //  useEffect(() => {
  //         const loadApplications = async () => {
            
  //           try {
  //             const data = await fetchBusinessApplications(token, tenantId);
  //             console.log("✅ Applications fetched:", data);
  //             setApplications(data?.searchList || []);
             
  //           } catch (err) {
  //             console.error("❌ Failed to fetch applications:", err);
  //           } 
  //         };
      
  //         loadApplications();
  //       }, []);

  useEffect(() => {
    dispatch(fetchBusinessApplications(token, tenantId));
  }, [dispatch, token, tenantId]);

  // useEffect(() => {
  //   // Only call translateConfig when businessId and token are available
  //   if (!businessId || !token || !tenantId) {
  //     console.log("Waiting for businessId, token, or tenantId...", { businessId, token, tenantId });
  //     return;
  //   }

  //   const callTranslateConfig = async () => {
  //     try {
  //       console.log("Business ID: in use effect", businessId);
        
  //       const data = await translateConfig(tenantId, businessId, token);
  //       console.log("Response from translateConfig:", data);
  //     } catch (error) {
  //       console.error("Failed to call translateConfig:", error);
  //     } 
  //   };

  //   callTranslateConfig();
  // }, [businessId, token, tenantId]);

  return (
    <>
      {/* Don't show session modal in sandbox mode */}
      {showSessionModal && !isSandboxMode() && (
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
      <div className="configurePage">
        <Text appearance="heading-s" color="primary-grey-100">
          Dashboard
        </Text>

        <div className="configure-dashboard-page">
          <div className="dashboard-img">
            <Image src={dashboard}></Image>
          </div>

          <Text appearance="body-s-bold" style={{ marginTop: "1rem" }}>
            Dashboard data points will appear here
          </Text>

          <Text color="primary-grey-80" style={{ marginTop: "0.5rem" }}>
            Start by configuring the system or creating a business group
          </Text>

          <div className="dashboard-btn" data-tour="quick-actions">
            <ActionButton
              kind="secondary"
              size="medium"
              state="normal"
              label="Configure System"
              onClick={handleConfigureSystem}
            />
            <ActionButton
              kind="secondary"
              size="medium"
              state="normal"
              label="Create business group"
              onClick={handleCreateBusinessGroup}
            />
          </div>
        </div>

        {/* Guided Tour */}
        <GuidedTour 
          steps={dashboardTourSteps} 
          tourId="dashboard-tour"
          showOnFirstVisit={true}
          onComplete={() => console.log('Dashboard tour completed!')}
        />
        
        {/* Tour Help Button */}
        <TourButton tourId="dashboard-tour" label="Start Tour" position="bottom-right" />
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
export default Dashboard;
