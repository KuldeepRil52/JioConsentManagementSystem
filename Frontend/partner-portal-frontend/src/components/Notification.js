import React from "react";
import "../styles/notification.css";
import "../styles/pageConfiguration.css";
import { Text , InputFieldV2, InputCheckbox, TabItem, Tabs, Icon, TokenProvider, Button} from "../custom-components";
import { createNotificationConfig, getNotifDetails, updateNotifConfig} from "../store/actions/CommonAction";
import { Text, InputFieldV2, InputCheckbox, TabItem, Tabs, Icon, TokenProvider, Button } from "../custom-components";
import { createNotificationConfig, getNotifDetails, updateNotifConfig, createSmtpConfig, getSmtpDetails, updateSmtpConfig } from "../store/actions/CommonAction";
import { useDispatch, useSelector } from "react-redux";
import { useState } from "react";
import { useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import { IcInfo, IcUpload, IcSuccess, IcClose } from "../custom-components/Icon";
import { CLEAR_SESSION } from "../store/constants/Constants";
import "../styles/sessionModal.css";
import ConfigurationShimmer from "./ConfigurationShimmer";
import { isSandboxMode } from "../utils/sandboxMode";
import { 
  validateCertificateFile as validateCertificateSecurity,
  formatValidationErrors 
} from "../utils/fileValidation";

const Notification = () => {

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const [url, setUrl] = useState("");
  const [callbackUrl, setCallbackUrl] = useState("");
  const [clientId, setClientId] = useState("");
  const [clientSecret, setClientSecret] = useState("");
  const [sid, setSid] = useState("");
  const [configId, setConfigId] = useState(null);
  const [certificateDocumentId, setCertificateDocumentId] = useState(null);
  const [isUrlValid, setIsUrlValid] = useState(true);
  const [isCallbackUrlValid, setIsCallbackUrlValid] = useState(true);
  const [networkType, setNetworkType] = useState("INTERNET");
  const [loading, setLoading] = useState(true);
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [showSessionModal, setShowSessionModal] = useState(false);

  const tenantId = useSelector((state) => state.common.tenant_id);
  const token = useSelector((state) => state.common.session_token);
  const businessId = useSelector((state) => state.common.business_id);

  const urlRegex = /^(https?:\/\/)?([\w-]+\.)+[\w-]{2,}(\/\S*)?$/;

  const [fileName, setFileName] = useState("");
  const fileInputRef = useRef(null);
  const [fileBase64, setFileBase64] = useState(null);
  const [certificatePreviewUrl, setCertificatePreviewUrl] = useState("");
  const [fileSize, setFileSize] = useState(0);

  // SMTP state variables
  const [smtpServerAddress, setSmtpServerAddress] = useState("");
  const [smtpPort, setSmtpPort] = useState("");
  const [smtpFromEmail, setSmtpFromEmail] = useState("");
  const [smtpUsername, setSmtpUsername] = useState("");
  const [smtpPassword, setSmtpPassword] = useState("");
  const [smtpTlsSsl, setSmtpTlsSsl] = useState("TLS");
  const [smtpSenderDisplayName, setSmtpSenderDisplayName] = useState("");
  const [smtpConnectionTimeout, setSmtpConnectionTimeout] = useState("5000");
  const [smtpAuthEnabled, setSmtpAuthEnabled] = useState(false);
  const [smtpSocketTimeout, setSmtpSocketTimeout] = useState("5000");
  const [smtpReplyTo, setSmtpReplyTo] = useState("");
  const [smtpTestEmail, setSmtpTestEmail] = useState("");
  const [smtpConfigId, setSmtpConfigId] = useState(null);
  const [smtpDetailsExist, setSmtpDetailsExist] = useState(false);


  const convertToBase64 = (file) => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file); // reads file as base64
      reader.onload = () => {
        setFileBase64(reader.result); // base64 string here
        console.log("Base64 File:", reader.result);
        resolve(reader.result);
      };
      reader.onerror = (error) => {
        console.log("Error converting file:", error);
        reject(error);
      };
    });
  };

  const validateFile = (file) => {
    const allowedExtensions = ['.pem', '.crt', '.cer'];
    const maxSize = 500 * 1024; // 500KB

    // Get file extension
    const fileName = file.name.toLowerCase();
    const hasValidExtension = allowedExtensions.some(ext => fileName.endsWith(ext));

    if (!hasValidExtension) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Only certificate files (PEM, CRT, or CER) are allowed."}
          />
        ),
        { icon: false }
      );
      return { valid: false };
    }

    if (file.size > maxSize) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"File size must be 500KB or less."}
          />
        ),
        { icon: false }
      );
      return { valid: false };
    }

    toast.success(
      (props) => (
        <CustomToast
          {...props}
          type="success"
          message={"Your Certificate is Sucessfully Uploaded."}
        />
      ),
      { icon: false }
    );
    return { valid: true };
  };


  const handleClick = () => {
    fileInputRef.current.click(); // trigger hidden input
  };



  const handleChange = async (e) => {
    if (e.target.files.length > 0) {
      const file = e.target.files[0];
      
      // ✅ Async validation
      const validationResult = await validateFile(file);
      
      if (validationResult.valid) {
        setFileName(file.name);
        setFileSize(file.size);
        setCertificateDocumentId(null); // Clear documentId when new file is uploaded
        await convertToBase64(file);
        setCertificatePreviewUrl(URL.createObjectURL(file));
      } else {
        e.target.value = ""; // clear invalid file
        setFileName("");
        setFileSize(0);
        setFileBase64(null);
        setCertificatePreviewUrl("");
      }
    }
  };


  const handleRemoveFile = () => {
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
    setFileName("");
    setFileSize(0);
    setFileBase64(null);
    setCertificateDocumentId(null);
    setCertificatePreviewUrl("");
    // also clear the input value
    //document.getElementById("fileInput").value = "";
  };

  const handlePreviewCertificte = () => {
    if (certificatePreviewUrl) {
      window.open(certificatePreviewUrl, "_blank");
    }
  };


  const handleDragOver = (e) => {
    e.preventDefault();
    e.currentTarget.classList.add("dragover");
  };



  const handleDragLeave = (e) => {
    e.preventDefault();
    e.currentTarget.classList.remove("dragover");
  };



  const handleDrop = async (e) => {
    e.preventDefault();
    e.currentTarget.classList.remove("dragover");

    if (e.dataTransfer.files.length > 0) {
      const file = e.dataTransfer.files[0];
      
      // ✅ Async validation for drag & drop
      const validationResult = await validateFile(file);
      
      if (validationResult.valid) {
        fileInputRef.current.files = e.dataTransfer.files;
        setFileName(file.name);
        setFileSize(file.size);
        setCertificateDocumentId(null); // Clear documentId when new file is uploaded
        setCertificatePreviewUrl(URL.createObjectURL(file));
        await convertToBase64(file);
      } else {
        fileInputRef.current.value = "";
        setFileName("");
        setFileSize(0);
        setFileBase64(null);
        setCertificatePreviewUrl("");
      }
    }
  };


  const handleUrlChange = (e) => {
    const value = e.target.value.trim();
    setUrl(value);

    if (value === "") {
      setIsUrlValid(true); // don’t show error when empty
      return;
    }

    if (urlRegex.test(value)) {
      setIsUrlValid(true);
    } else {
      setIsUrlValid(false);
    }
  };

  const handleCallbackUrlChange = (e) => {
    const value = e.target.value.trim();
    setCallbackUrl(value);

    if (value === "") {
      setIsCallbackUrlValid(true);
      return;
    }
    if (urlRegex.test(value)) {
      setIsCallbackUrlValid(true);
    } else {
      setIsCallbackUrlValid(false);
    }
  };

  const handleSmtpSubmit = async () => {
    // Validation
    if (!smtpServerAddress) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please enter SMTP server address."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (!smtpPort) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please enter SMTP port."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (!smtpFromEmail) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please enter from email."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (!smtpUsername) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please enter SMTP username."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (!smtpPassword) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please enter SMTP password."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (!smtpSenderDisplayName) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please enter sender display name."}
          />
        ),
        { icon: false }
      );
      return;
    }

    // In sandbox mode, use default values if missing
    const finalBusinessId = businessId || (isSandboxMode() ? 'sandbox-business-id' : null);
    const finalTenantId = tenantId || (isSandboxMode() ? 'sandbox-tenant-id' : null);
    const finalToken = token || (isSandboxMode() ? 'sandbox-session-token-12345' : null);
    
    // Determine scope-level: if businessId === tenantId then "TENANT", otherwise "BUSINESS"
    const scopeLevel = finalBusinessId === finalTenantId ? "TENANT" : "BUSINESS";

    const payload = {
      configurationJson: {
        serverAddress: smtpServerAddress,
        port: parseInt(smtpPort),
        fromEmail: smtpFromEmail,
        username: smtpUsername,
        password: smtpPassword,
        tlsSsl: smtpTlsSsl,
        senderDisplayName: smtpSenderDisplayName,
        connectionTimeout: parseInt(smtpConnectionTimeout) || 5000,
        smtpAuthEnabled: smtpAuthEnabled,
        smtpConnectionTimeout: parseInt(smtpConnectionTimeout) || 5000,
        smtpSocketTimeout: parseInt(smtpSocketTimeout) || 5000,
        replyTo: smtpReplyTo || null,
        testEmail: smtpTestEmail || null,
      },
    };

    try {
      let response;
      // Check if smtpDetails exists - fetch again to be sure, or use state
      // First check state, if false, fetch to verify
      let shouldUpdate = smtpDetailsExist;
      
      if (!shouldUpdate) {
        // Double-check by fetching current details
        try {
          const currentResponse = await getSmtpDetails(token, tenantId, businessId);
          const currentSearchList = currentResponse?.searchList || [];
          const currentLatestConfig = currentSearchList.length > 0 ? currentSearchList[currentSearchList.length - 1] : null;
          const currentSmtpDetails = currentLatestConfig?.smtpDetails;
          shouldUpdate = !!currentSmtpDetails;
          
          // Also update configId if we found it
          if (shouldUpdate && !smtpConfigId) {
            if (currentLatestConfig?.configId) {
              setSmtpConfigId(currentLatestConfig.configId);
            } else if (currentLatestConfig?.id) {
              setSmtpConfigId(currentLatestConfig.id);
            }
          }
          console.log("Re-checked smtpDetails exists:", shouldUpdate);
        } catch (err) {
          console.error("Error re-checking SMTP details:", err);
        }
      }
      
      console.log("smtpDetailsExist (state): ", smtpDetailsExist);
      console.log("shouldUpdate (final): ", shouldUpdate);
      console.log("smtpConfigId: ", smtpConfigId);
      
      if (shouldUpdate) {
        // Update existing config using PUT
        if (!smtpConfigId) {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Config ID is missing. Cannot update SMTP settings."}
              />
            ),
            { icon: false }
          );
          return;
        }
        response = await updateSmtpConfig(
          payload,
          finalToken,
          finalBusinessId,
          finalTenantId,
          scopeLevel,
          smtpConfigId
        );
        console.log("✅ SMTP config updated:", response);
        if (response?.status === 200 || response?.status === 201) {
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"SMTP settings updated successfully."}
              />
            ),
            { icon: false }
          );
          // Trigger refetch after successful update
          setRefreshTrigger(prev => prev + 1);
        }
      } else {
        // Create new config using POST
        response = await createSmtpConfig(
          payload,
          finalToken,
          finalBusinessId,
          finalTenantId,
          scopeLevel
        );
        console.log("✅ SMTP config created:", response);
        if (response?.status === 200 || response?.status === 201) {
          // Store configId from create response if available
          if (response?.data?.configId) {
            setSmtpConfigId(response.data.configId);
          } else if (response?.data?.id) {
            setSmtpConfigId(response.data.id);
          }
          // Mark that smtpDetails now exists
          setSmtpDetailsExist(true);
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"SMTP settings saved successfully."}
              />
            ),
            { icon: false }
          );
          // Trigger refetch after successful save
          setRefreshTrigger(prev => prev + 1);
        }
      }
    } catch (err) {
      console.error("⚠️ Error saving SMTP config:", err);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={err?.message || "Failed to save SMTP settings."}
          />
        ),
        { icon: false }
      );
    }
  };

  const handleSubmit = async () => {
    if (!url) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please enter url."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (!clientId) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please enter clientId."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (!clientSecret) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please enter client secret."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (!sid) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please enter sid."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (!callbackUrl) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please enter callback url."}
          />
        ),
        { icon: false }
      );
      return;
    }
    if (fileName == "" || fileName == undefined || fileName == null) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please upload a certificate file."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (!fileBase64) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Certificate file is still being processed. Please wait a moment and try again."}
          />
        ),
        { icon: false }
      );
      return;
    }
    const payload = {
      configurationJson: {
        networkType,
        baseUrl: url,
        clientId,
        clientSecret,
        sid,
        callbackUrl,
        mutualSSL: true,
        mutualCertificate: fileBase64,
        mutualCertificateMeta: {
          ...(certificateDocumentId && { documentId: certificateDocumentId }), // Include documentId only if it exists (for updates)
          name: fileName || "mutual_cert.pem",
          contentType: "application/x-x509-ca-cert",
          size: fileSize || 3274,
        },
      },
    };

    // In sandbox mode, use default values if missing
    const finalBusinessId = businessId || (isSandboxMode() ? 'sandbox-business-id' : null);
    const finalTenantId = tenantId || (isSandboxMode() ? 'sandbox-tenant-id' : null);
    const finalToken = token || (isSandboxMode() ? 'sandbox-session-token-12345' : null);

    const putHeaders = {
      "tenant-id": finalTenantId,
      "x-session-token": finalToken,
    };

    if (!configId) {
      
      try {
        const response = await createNotificationConfig(
          payload,
          finalToken,
          finalBusinessId,
          finalTenantId
        );
        console.log("✅ Notification created:", response);
        if (response?.status === 200) {
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"Settings saved successfully."}
              />
            ),
            { icon: false }
          );
          // Trigger refetch after successful save
          setRefreshTrigger(prev => prev + 1);
        }
      } catch (err) {
        console.error("⚠️ Error creating notification:", err);
      }
    } else {
      try {
        const resp = await dispatch(updateNotifConfig(payload, putHeaders, configId));
        console.log("notification update resp: ", resp);

        if (resp.status === 200) {
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"Settings updated successfully."}
              />
            ),
            { icon: false }
          );
          // Trigger refetch after successful update
          setRefreshTrigger(prev => prev + 1);
        }
      } catch (error) {
        console.error("Error saving configuration:", error);
        if (
          error[0].errorCode == "JCMP4003" ||
          error[0].errorCode == "JCMP4001"
        ) {
          toast.error(
            (props) => (
              <CustomToast {...props} type="error" message={"Session expired"} />
            ),
            { icon: false }
          );
          dispatch({ type: CLEAR_SESSION });
          setShowSessionModal(true);
          setTimeout(() => {
            setShowSessionModal(false);
            navigate("/adminLogin");
          }, 7000);
        } else if (error[0]?.errorCode == "JCMP3003") {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Consent Set up is already exists."}
              />
            ),
            { icon: false }
          );
        }
      }
    }
  };

  const fetchNotifDetails = async () => {
    try {
      setLoading(true);
      // In sandbox mode, use default values if missing
      const finalBusinessId = businessId || (isSandboxMode() ? 'sandbox-business-id' : null);
      const finalTenantId = tenantId || (isSandboxMode() ? 'sandbox-tenant-id' : null);
      const finalToken = token || (isSandboxMode() ? 'sandbox-session-token-12345' : null);
      
      if (!finalToken || !finalBusinessId || !finalTenantId) {
        if (!isSandboxMode()) {
          console.log('⏳ Waiting for credentials to load...');
          setLoading(false);
          return;
        }
      }
      
      const response = await getNotifDetails(finalToken, finalTenantId, finalBusinessId);
      console.log("consent response:", response);

      // Get the latest config (last item in searchList array)
      const searchList = response?.searchList || [];
      const latestConfig = searchList.length > 0 ? searchList[searchList.length - 1] : null;
      const config = latestConfig?.configurationJson;
      console.log("Config fetched:", config);
      setConfigId(latestConfig?.configId);
      if (!config) {
        setLoading(false);
        return;
      }

      setClientId(config.clientId || "");
      setClientSecret(config.clientSecret || "");
      setSid(config.sid || "");
      setUrl(config.baseUrl || "");
      setCallbackUrl(config.callbackUrl || "");
      setNetworkType(config.networkType || "INTERNET");

      // Set Mutual Certificate details
      if (config.mutualCertificate) {
        setFileBase64(config.mutualCertificate);
        if (config.mutualCertificateMeta) {
          setFileName(config.mutualCertificateMeta.name || "");
          setFileSize(config.mutualCertificateMeta.size || 0);
          setCertificateDocumentId(config.mutualCertificateMeta.documentId || null);
          // Create preview URL from base64
          if (config.mutualCertificateMeta.contentType === "application/x-pdf-file" ||
            config.mutualCertificateMeta.contentType === "application/pdf") {
            // For PDF, use the base64 data URL directly
            setCertificatePreviewUrl(config.mutualCertificate);
          } else {
            // For other certificate types, try to create blob URL
            try {
              const base64Data = config.mutualCertificate.includes(',')
                ? config.mutualCertificate.split(',')[1]
                : config.mutualCertificate;
              const byteCharacters = atob(base64Data);
              const byteNumbers = new Array(byteCharacters.length);
              for (let i = 0; i < byteCharacters.length; i++) {
                byteNumbers[i] = byteCharacters.charCodeAt(i);
              }
              const byteArray = new Uint8Array(byteNumbers);
              const blob = new Blob([byteArray], { type: config.mutualCertificateMeta.contentType });
              setCertificatePreviewUrl(URL.createObjectURL(blob));
            } catch (error) {
              console.error("Error creating certificate preview URL:", error);
            }
          }
        }
      }

    } catch (error) {
      console.error("Error fetching consent details:", error);
    } finally {
      setLoading(false);
    }
  };

  const fetchSmtpDetails = async () => {
    try {
      // In sandbox mode, use default values if missing
      const finalBusinessId = businessId || (isSandboxMode() ? 'sandbox-business-id' : null);
      const finalTenantId = tenantId || (isSandboxMode() ? 'sandbox-tenant-id' : null);
      const finalToken = token || (isSandboxMode() ? 'sandbox-session-token-12345' : null);
      
      if (!finalToken || !finalBusinessId || !finalTenantId) {
        if (!isSandboxMode()) {
          console.log('⏳ Waiting for credentials to load...');
          return;
        }
      }
      
      const response = await getSmtpDetails(finalToken, finalTenantId, finalBusinessId);
      console.log("SMTP response:", response);

      // Get the latest config (last item in searchList array)
      const searchList = response?.searchList || [];
      const latestConfig = searchList.length > 0 ? searchList[searchList.length - 1] : null;
      
      if (!latestConfig) {
        // No config exists yet - leave fields empty for first-time setup
        console.log("No SMTP config found");
        setSmtpDetailsExist(false);
        return;
      }

      // smtpDetails is at the same level as configurationJson, not inside it
      const smtpDetails = latestConfig?.smtpDetails;
      console.log("SMTP Details fetched:", smtpDetails);
      console.log("Latest config object:", latestConfig);
      
      // Store configId if available (check multiple possible fields)
      if (latestConfig?.configId) {
        setSmtpConfigId(latestConfig.configId);
        console.log("SMTP ConfigId set:", latestConfig.configId);
      } else if (latestConfig?.id) {
        setSmtpConfigId(latestConfig.id);
        console.log("SMTP Id set:", latestConfig.id);
      } else {
        console.warn("No configId found in response. Update will require configId.");
      }
      
      if (!smtpDetails) {
        // No SMTP details exist yet - leave fields empty for first-time setup
        console.log("No SMTP details found in config");
        setSmtpDetailsExist(false);
        return;
      }

      // Mark that smtpDetails exists
      setSmtpDetailsExist(true);

      // Populate SMTP input fields from smtpDetails
      setSmtpServerAddress(smtpDetails.serverAddress || "");
      setSmtpPort(smtpDetails.port ? String(smtpDetails.port) : "");
      setSmtpFromEmail(smtpDetails.fromEmail || "");
      setSmtpUsername(smtpDetails.username || "");
      setSmtpPassword(smtpDetails.password || "");
      setSmtpTlsSsl(smtpDetails.tlsSsl || "TLS");
      setSmtpSenderDisplayName(smtpDetails.senderDisplayName || "");
      setSmtpConnectionTimeout(smtpDetails.smtpConnectionTimeout ? String(smtpDetails.smtpConnectionTimeout) : (smtpDetails.connectionTimeout ? String(smtpDetails.connectionTimeout) : "5000"));
      setSmtpAuthEnabled(smtpDetails.smtpAuthEnabled !== undefined ? smtpDetails.smtpAuthEnabled : false);
      setSmtpSocketTimeout(smtpDetails.smtpSocketTimeout ? String(smtpDetails.smtpSocketTimeout) : "5000");
      setSmtpReplyTo(smtpDetails.replyTo || "");
      setSmtpTestEmail(smtpDetails.testEmail || "");
      
      console.log("SMTP fields populated successfully");
    } catch (error) {
      console.error("Error fetching SMTP details:", error);
      // Silently handle errors - user can still create new config
    }
  };

  useEffect(() => {
    // In sandbox mode, proceed even if credentials are not yet loaded (will use fallbacks)
    const finalBusinessId = businessId || (isSandboxMode() ? 'sandbox-business-id' : null);
    const finalTenantId = tenantId || (isSandboxMode() ? 'sandbox-tenant-id' : null);
    const finalToken = token || (isSandboxMode() ? 'sandbox-session-token-12345' : null);
    
    if (finalToken && finalTenantId && finalBusinessId) {
      fetchNotifDetails();
      fetchSmtpDetails();
    } else if (isSandboxMode()) {
      // In sandbox mode, proceed with fallback values
      fetchNotifDetails();
      fetchSmtpDetails();
    }
  }, [token, tenantId, businessId, refreshTrigger]
  );

  if (loading) {
    return <ConfigurationShimmer />;
  }

  return (
    <TokenProvider
      value={{
        theme: "JioBase",
        mode: "light",
      }}
    >

      <div className="configurePage">
        <div className="notif-page">
          <div className="notif-heading">
            <Text appearance="heading-s" color="primary-grey-100">
              Notification set up
            </Text>
            <div className="tag">
              <Text appearance="body-xs-bold" color="primary-grey-80">
                System Configuration
              </Text>
            </div>
          </div>
        </div>
        <div className="notif-tabs-setup">
        <Tabs
                onTabChange={function noRefCheck(){}}
                onNextClick={function noRefCheck(){}}
                onPrevClick={function noRefCheck(){}}
                onScroll={function noRefCheck(){}}
                overflow="fit"
                >
                <TabItem
                    label={<Text appearance='body-xs' color="primary-grey-80">Notification service</Text>}
                    children={
                        <> 
                          
            <div className="service-inputs-main" style={{ marginTop: "20px" }}>
              <InputFieldV2
                className="service-input"
                label="URL (Required)"
                placeholder="Enter"
                size="medium"
                value={url}
                onChange={handleUrlChange}
                stateText={
                  isUrlValid
                    ? ""
                    : "Please enter a valid URL (e.g. https://api.example.com)"
                }
              ></InputFieldV2>
              <InputFieldV2
                className="service-input"
                label="Callback URL (Required)"
                placeholder="Enter"
                size="medium"
                value={callbackUrl}
                onChange={handleCallbackUrlChange}
                stateText={
                  isUrlValid
                    ? ""
                    : "Please enter a valid URL (e.g. https://api.example.com)"
                }
              ></InputFieldV2>
              <InputFieldV2
                className="service-input"
                label="Network type (Required)"
                placeholder="Enter"
                size="medium"
                value={networkType}
                onChange={(e) => setNetworkType(e.target.value)}
                readOnly={true}
              ></InputFieldV2>
              <InputFieldV2
                className="service-input"
                label="Client ID (Required)"
                placeholder="Enter"
                size="medium"
                value={clientId}
                onChange={(e) => setClientId(e.target.value)}
              ></InputFieldV2>
              <InputFieldV2
                className="service-input"
                label="Client secret key (Required)"
                placeholder="Enter"
                size="medium"
                value={clientSecret}
                onChange={(e) => setClientSecret(e.target.value)}
              ></InputFieldV2>
              <InputFieldV2
                className="service-input"
                label="SID (Required)"
                placeholder="Enter"
                size="medium"
                value={sid}
                onChange={(e) => setSid(e.target.value)}
              ></InputFieldV2>
              <div>
                <div style={{ display: 'flex', flexDirection: 'row' }}>
                  <Text appearance="body-xs" color="primary-grey-80">
                    Upload Certificate
                  </Text>
                  <div style={{ marginLeft: '5px' }} title="Only certificate files (PEM, CRT, or CER) are allowed. Maximum file size: 500KB.">
                    <Icon
                      ic={<IcInfo />}
                      size="small"
                      color="primary_grey_80"

                    />
                  </div>

                </div>

                {/* Drag & Drop area */}
                <div
                  className="fileUploader-custom1"
                  onClick={handleClick}
                  onDragOver={handleDragOver}
                  onDragLeave={handleDragLeave}
                  onDrop={handleDrop}
                >
                  <input
                    type="file"
                    ref={fileInputRef}
                    style={{ display: "none" }}
                    accept=".pem,.crt,.cer,application/x-pem-file,application/x-x509-ca-cert,application/pkix-cert"
                    onChange={handleChange}
                  />

                  <div className="flex items-center justify-center">
                    <Icon
                      ic={<IcUpload height={23} width={23} />}
                      color="primary_60"
                    />
                    <Text appearance="button" color="primary-60">
                      Upload
                    </Text>
                  </div>
                </div>

                <Text appearance="body-xs" color="primary-grey-80">
                  Drag and drop signing certificate (PEM, CRT, or CER) requested for encryption under 500KB
                </Text>

                {fileName && (
                  <div className="systemConfiguration-file-uploader">
                    <div
                      onClick={handlePreviewCertificte}
                      className="previewFile"
                    >
                      <Icon
                        ic={<IcSuccess width={15} height={15} />}
                        color="feedback_success_50"
                      />
                      <Text appearance="body-xs" color="primary-grey-80">
                        {fileName}
                      </Text>
                    </div>
                    <Icon
                      ic={<IcClose width={15} height={15} />}
                      color="primary_60"
                      className="selecetd-file-display"
                      onClick={handleRemoveFile}
                    />
                  </div>
                )}
              </div>
            </div>
            <div className="content-5">
              <Button
                ariaControls="Button Clickable"
                ariaDescribedby="Button"
                ariaExpanded="Expanded"
                ariaLabel="Button"
                className="Button"
                icon=""
                iconAriaLabel="Icon Favorite"
                iconLeft=""
                kind="primary"
                label="Save"
                onClick={handleSubmit}
                state="normal"
              />
            </div>
            </>
          
                        
                    }
                />
                <TabItem
                    label={<Text appearance='body-xs' color="primary-grey-80">Simple mail transfer protocol (SMTP)</Text>}
                    children={
                      <div className="service-inputs-main" style={{marginTop: "20px"}}>
                        <InputFieldV2
                          className="service-input"
                          label="Server Address (Required)"
                          placeholder="Enter SMTP server address"
                          size="small"
                          value={smtpServerAddress}
                          onChange={(e) => setSmtpServerAddress(e.target.value)}
                        />
                        <InputFieldV2
                          className="service-input"
                          label="Port (Required)"
                          placeholder="Enter port number"
                          size="small"
                          type="number"
                          value={smtpPort}
                          onChange={(e) => setSmtpPort(e.target.value)}
                        />
                        <InputFieldV2
                          className="service-input"
                          label="From Email (Required)"
                          placeholder="Enter email address to send from"
                          size="small"
                          type="email"
                          value={smtpFromEmail}
                          onChange={(e) => setSmtpFromEmail(e.target.value)}
                        />
                        <InputFieldV2
                          className="service-input"
                          label="Username (Required)"
                          placeholder="Enter username for SMTP authentication"
                          size="small"
                          value={smtpUsername}
                          onChange={(e) => setSmtpUsername(e.target.value)}
                        />
                        <InputFieldV2
                          className="service-input"
                          label="Password (Required)"
                          placeholder="Enter password for SMTP authentication"
                          size="small"
                          type="password"
                          value={smtpPassword}
                          onChange={(e) => setSmtpPassword(e.target.value)}
                        />
                        <InputFieldV2
                          className="service-input"
                          label="TLS/SSL (Required)"
                          placeholder="Enter encryption type (TLS/SSL/NONE)"
                          size="small"
                          value={smtpTlsSsl}
                          onChange={(e) => setSmtpTlsSsl(e.target.value)}
                        />
                        <InputFieldV2
                          className="service-input"
                          label="Sender Display Name (Required)"
                          placeholder="Enter display name for the sender"
                          size="small"
                          value={smtpSenderDisplayName}
                          onChange={(e) => setSmtpSenderDisplayName(e.target.value)}
                        />
                        <InputFieldV2
                          className="service-input"
                          label="Connection Timeout (ms)"
                          placeholder="Enter connection timeout in milliseconds"
                          size="small"
                          type="number"
                          value={smtpConnectionTimeout}
                          onChange={(e) => setSmtpConnectionTimeout(e.target.value)}
                        />
                        <div style={{ marginTop: "10px", marginBottom: "10px" }}>
                          <InputCheckbox
                            label="SMTP Authentication Enabled"
                            checked={smtpAuthEnabled}
                            onClick={() => setSmtpAuthEnabled((prev) => !prev)}
                          />
                        </div>
                        <InputFieldV2
                          className="service-input"
                          label="SMTP Socket Timeout (ms)"
                          placeholder="Enter socket timeout in milliseconds"
                          size="small"
                          type="number"
                          value={smtpSocketTimeout}
                          onChange={(e) => setSmtpSocketTimeout(e.target.value)}
                        />
                        <InputFieldV2
                          className="service-input"
                          label="Reply To"
                          placeholder="Enter reply-to email address"
                          size="small"
                          type="email"
                          value={smtpReplyTo}
                          onChange={(e) => setSmtpReplyTo(e.target.value)}
                        />
                        <InputFieldV2
                          className="service-input"
                          label="Test Email"
                          placeholder="Enter test email address"
                          size="small"
                          type="email"
                          value={smtpTestEmail}
                          onChange={(e) => setSmtpTestEmail(e.target.value)}
                        />
                        <div className="content-5" style={{ marginTop: "20px" }}>
                          <Button
                            ariaControls="Button Clickable"
                            ariaDescribedby="Button"
                            ariaExpanded="Expanded"
                            ariaLabel="Button"
                            className="Button"
                            icon=""
                            iconAriaLabel="Icon Favorite"
                            iconLeft=""
                            kind="primary"
                            label="Save"
                            onClick={handleSmtpSubmit}
                            state="normal"
                          />
                        </div>
                      </div>
                    }
                />
               
                </Tabs>
          
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
    </TokenProvider>
  );

};

export default Notification;