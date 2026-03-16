import axios from "axios";
import { store } from "../store/store";
import { CLEAR_SESSION } from "../store/constants/Constants";
import { toast } from "react-toastify";
import { isSandboxMode, sandboxAPICall } from "./sandboxMode";

// Flag to prevent multiple logout triggers
let isLoggingOut = false;

// Function to handle session expiration
const handleSessionExpired = () => {
  // Don't handle session expiration in sandbox mode
  if (isSandboxMode()) {
    console.log('🏖️ SANDBOX MODE - Ignoring session expiration');
    return;
  }

  if (isLoggingOut) return;

  isLoggingOut = true;

  // Clear session from Redux store
  store.dispatch({ type: CLEAR_SESSION });

  // Show toast notification
  toast.error("Session expired. Please log in again.", {
    position: "bottom-left",
    autoClose: 2000,
    hideProgressBar: true,
  });

  // Redirect to login immediately
  setTimeout(() => {
    isLoggingOut = false;
    window.location.href = "/adminLogin";
  }, 100);
};

// Setup axios request interceptor to catch calls in sandbox mode
axios.interceptors.request.use(
  async (config) => {
    // If in sandbox mode, intercept and return mock data
    if (isSandboxMode()) {

      const url = config.url;
      const method = config.method?.toUpperCase() || 'GET';
      const body = config.data || {};
      const headers = config.headers || {};

      // Get mock response from sandbox
      const mockResponse = await sandboxAPICall(url, method, body, headers);


      // Cancel the real request and return mock data
      // We do this by throwing a special error that we catch in the response interceptor
      const cancelError = new Error('SANDBOX_INTERCEPTED');
      cancelError.config = config;
      cancelError.response = {
        data: mockResponse.data,
        status: mockResponse.status,
        statusText: mockResponse.status === 200 ? 'OK' : 'Error',
        headers: {},
        config: config
      };


      return Promise.reject(cancelError);
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Setup axios response interceptor
axios.interceptors.response.use(
  (response) => {
    // Return successful response as-is
    return response;
  },
  (error) => {
    // Special handling for sandbox-intercepted requests
    if (error.message === 'SANDBOX_INTERCEPTED') {
      // Return the mock response as if it was a real axios response
      return Promise.resolve(error.response);
    }

    // Don't handle errors in sandbox mode - sandbox handles everything
    if (isSandboxMode()) {
      return Promise.reject(error);
    }

    // Check for 401 Unauthorized or specific error codes
    if (error.response) {
      const status = error.response.status;
      const errorData = error.response.data;
      const errorCode = errorData?.errorList?.[0]?.errorCode || errorData?.[0]?.errorCode;

      // Handle session expiration (401 or specific error codes)
      if (
        status === 401 ||
        errorCode === "JCMP4003" ||
        errorCode === "JCMP4001"
      ) {
        handleSessionExpired();
      }
    }

    // Reject with error for further handling if needed
    return Promise.reject(error);
  }
);

export const makeAPICall = async (url, method, body, headers) => {

  // Check if we're in sandbox mode
  if (isSandboxMode()) {
    const mockResponse = await sandboxAPICall(url, method, body, headers);


    // Convert sandbox response format {status, data} to axios response format {data, status}
    const axiosResponse = {
      data: mockResponse.data,
      status: mockResponse.status,
      statusText: mockResponse.status === 200 ? 'OK' : 'Error',
      headers: {},
      config: {}
    };


    return axiosResponse;
  }

  try {
    headers.method = method;
    let apiObjAxios = { url, method };
    apiObjAxios.headers = headers;

    if (method === "POST" || method === "PUT" || method === "PATCH") {
      apiObjAxios.data = body;
    }

    return new Promise((resolve, reject) => {
      axios(apiObjAxios)
        .then(async (response) => {
          resolve(response);
        })
        .catch((err) => {
          if (err.response && err.response.data) {
            reject(err.response.data);
          } else {
            reject(err);
          }
        });
    });
  } catch (error) {
    throw new Error(
      "Something went wrong",
      error.response ? error.response.data : error
    );
  }
};
