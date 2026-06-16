// Sandbox initialization utility
import { SAVE_LOGIN_INFO, SAVE_USER_PROFILE, UPDATE_BUSINESS_ID, SAVE_BUSINESSES, SET_SELECTED_BUSINESS } from '../store/constants/Constants';
import { isSandboxMode, getSandboxUserData, startFetchInterception } from './sandboxMode';
import { toast } from 'react-toastify';

// Show sandbox welcome notification (once per session)
export const showSandboxWelcomeNotification = () => {
  // Only show if in sandbox mode
  if (!isSandboxMode()) return;
  
  // Check if notification has been shown in this session
  const hasShownNotification = sessionStorage.getItem('sandbox_welcome_shown');
  
  if (!hasShownNotification) {
    // Use setTimeout to ensure toast container is ready
    setTimeout(() => {
      toast.info(
        "👋 Welcome to Sandbox Mode! Explore freely - all changes are simulated and won't be saved.",
        {
          position: "top-center",
          autoClose: 5000,
          hideProgressBar: false,
          closeOnClick: true,
          pauseOnHover: true,
          draggable: true,
          style: {
            background: 'linear-gradient(135deg, #3535f3 0%, #0052CC 100%)',
            color: '#FFFFFF',
            fontSize: '14px',
            fontWeight: '500',
            borderRadius: '8px',
            boxShadow: '0 4px 12px rgba(53, 53, 243, 0.4)',
            padding: '16px 20px',
            minHeight: '60px'
          },
          progressStyle: {
            background: 'rgba(255, 255, 255, 0.7)'
          }
        }
      );
      
      // Mark as shown for this session
      sessionStorage.setItem('sandbox_welcome_shown', 'true');
    }, 800);
  }
};

// Show quick reminder on actions (can be called multiple times)
let lastActionToastTime = 0;
const ACTION_TOAST_COOLDOWN = 3000; // Show max once per 3 seconds

export const showSandboxActionReminder = () => {
  // Only show if in sandbox mode
  if (!isSandboxMode()) return;
  
  // Throttle notifications to avoid spam
  const now = Date.now();
  if (now - lastActionToastTime < ACTION_TOAST_COOLDOWN) return;
  lastActionToastTime = now;
  
  toast.info(
    "💡 Sandbox Mode: Changes won't be saved",
    {
      position: "bottom-right",
      autoClose: 2500,
      hideProgressBar: true,
      closeOnClick: true,
      pauseOnHover: false,
      draggable: false,
      style: {
        background: '#3535f3',
        color: '#FFFFFF',
        fontSize: '13px',
        fontWeight: '500',
        borderRadius: '6px',
        boxShadow: '0 2px 8px rgba(53, 53, 243, 0.3)',
        padding: '10px 14px',
        minHeight: '48px'
      }
    }
  );
};

export const initializeSandboxSession = (dispatch) => {
  if (!isSandboxMode()) return false;

  const userData = getSandboxUserData();
  
  // Start intercepting fetch requests for sandbox mode
  startFetchInterception();
  
  // Show the welcome notification (once per session)
  showSandboxWelcomeNotification();

  // Dispatch sandbox login credentials
  dispatch({
    type: SAVE_LOGIN_INFO,
    payload: {
      pan: "SANDBOX123",
      emailMobile: userData.email,
      tenant_id: "sandbox-tenant-id",
      user_id: "sandbox-user-id",
      session_token: "sandbox-session-token-12345",
    },
  });

  // Dispatch sandbox businesses
  const sandboxBusinesses = [
    {
      businessId: "sandbox-business-id",
      name: "Sandbox Demo Corp",
      description: "Demo business for sandbox environment",
      createdAt: "2024-01-15T10:00:00Z"
    },
    {
      businessId: "sandbox-business-id-2",
      name: "Sandbox Tech Industries",
      description: "Technology business unit",
      createdAt: "2024-02-20T10:00:00Z"
    }
  ];

  dispatch({
    type: SAVE_BUSINESSES,
    payload: sandboxBusinesses,
  });

  dispatch({
    type: SET_SELECTED_BUSINESS,
    payload: "sandbox-business-id",
  });

  // Dispatch sandbox user profile
  dispatch({
    type: SAVE_USER_PROFILE,
    data: {
      userId: "sandbox-user-id",
      username: userData.name,
      email: userData.email,
      designation: "Sandbox Administrator",
      tenantId: "sandbox-tenant-id",
      roles: [
        {
          roleId: "role-1",
          role: "ADMIN",
          businessId: "sandbox-business-id",
          permissions: [
            "READ",
            "WRITE",
            "DELETE",
            "MANAGE_USERS",
            "MANAGE_ROLES",
            "MANAGE_CONSENTS",
            "MANAGE_GRIEVANCES",
            "MANAGE_TEMPLATES",
            "MANAGE_MASTER_DATA",
            "VIEW_REPORTS",
            "EXPORT_DATA"
          ],
        },
      ],
    },
  });

  // Set business ID
  dispatch({
    type: UPDATE_BUSINESS_ID,
    payload: "sandbox-business-id",
  });

  return true;
};

// Check if user should bypass login in sandbox mode
export const shouldBypassLogin = () => {
  return isSandboxMode();
};

