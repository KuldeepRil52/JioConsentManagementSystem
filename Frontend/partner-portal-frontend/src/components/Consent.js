import React from "react";
import "../styles/consent.css";
import {
  Text,
  InputCheckbox,
  InputFieldV2,
  InputDropdown,
  Button,
} from "../custom-components";

import "../styles/pageConfiguration.css";
import { CLEAR_SESSION } from "../store/constants/Constants";
import "../styles/sessionModal.css";
import { useState } from "react";
import { IcCalendarWeek } from "../custom-components/Icon";
import { useEffect } from "react";
import { saveConsentConfig, getConsentDetails, updateConsentConfig, saveDigilockerConfig, getDigilockerDetails } from "../store/actions/CommonAction";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import { useDispatch, useSelector } from "react-redux";
import ConfigurationShimmer from "./ConfigurationShimmer";
import { generateTransactionId } from "../utils/transactionId";
import config from "../utils/config";
import { ActionButton } from '../custom-components';

const Consent = () => {

  const dispatch = useDispatch();
  const tenantId = useSelector((state) => state.common.tenant_id);
  const token = useSelector((state) => state.common.session_token);
  const businessId = useSelector((state) => state.common.business_id);

  const [isChecked, setIsChecked] = useState(false);
  const [isApaarChecked, setIsApaarChecked] = useState(false);
  const [isMultiChecked, setIsMultiChecked] = useState(false);
  const [isBlockchainChecked, setIsBlockchainChecked] = useState(false);
  const [language, setLanguage] = useState("en");
  const [showSessionModal, setShowSessionModal] = useState(false);

  const [artefactRetention, setArtefactRetention] = useState({
    value: "99",
    unit: "",
  });
  const [logRetention, setLogRetention] = useState({ value: "99", unit: "" });


  const [artefactInput, setArtefactInput] = useState("99");
  const [logInput, setLogInput] = useState("99");
  const [duration, setDuration] = useState("YEARS");
  const [logDuration, setLogDuration] = useState("YEARS");

  const [configId, setConfigId] = useState("");
  const [clientId, setClientId] = useState("");
  const [clientSecret, setClientSecret] = useState("");
  const [appName, setAppName] = useState("");
  const [redirectUrl, setRedirectUrl] = useState("");
  const [loading, setLoading] = useState(true);

  // Multilingual config modal state
  const [showMultilingualModal, setShowMultilingualModal] = useState(false);
  const [savingMultilingualConfig, setSavingMultilingualConfig] = useState(false);
  const [loadingMultilingualConfig, setLoadingMultilingualConfig] = useState(false);

  // Multilingual config form fields - no default values for security
  const [apiBaseUrl, setApiBaseUrl] = useState("");
  const [provider, setProvider] = useState("");
  const [modelPipelineEndpoint, setModelPipelineEndpoint] = useState("");
  const [callbackUrl, setCallbackUrl] = useState("");
  const [userId, setUserId] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [pipelineId, setPipelineId] = useState("");
  const [subscriptionKey, setSubscriptionKey] = useState("");
  const [region, setRegion] = useState("");
  const [endpoint, setEndpoint] = useState("");
  const [scopeLevel, setScopeLevel] = useState("BUSINESS");



  const languages = [
    { code: "en", name: "English" },
    { code: "hi", name: "Hindi" },
    { code: "bn", name: "Bengali" },
    { code: "ta", name: "Tamil" },
    { code: "te", name: "Telugu" },
    { code: "kn", name: "Kannada" },
    { code: "ml", name: "Malayalam" },
    { code: "gu", name: "Gujarati" },
    { code: "mr", name: "Marathi" },
    { code: "or", name: "Odia" },
    { code: "pa", name: "Punjabi" },
    { code: "as", name: "Assamese" },
    { code: "ne", name: "Nepali" },
    { code: "sd", name: "Sindhi" },
    { code: "ks", name: "Kashmiri" },
    { code: "doi", name: "Dogri" },
    { code: "gom", name: "Konkani" },
    { code: "mai", name: "Maithili" },
    { code: "sa", name: "Sanskrit" },
    { code: "sat", name: "Santali" },
    { code: "brx", name: "Bodo" },
    { code: "mni", name: "Manipuri" },
  ];

  const handleLanguageChange = (e) => {
    setLanguage(e.target.value);
  };


  const handleArtefactChange = (e) => setArtefactInput(e.target.value);
  const handleLogChange = (e) => setLogInput(e.target.value);

  const handleArtefactDuration = (e) => setDuration(e.target.value);
  const handleLogDuration = (e) => setLogDuration(e.target.value);

  const format = ({ value, unit }) => {
    if (!value || !unit) return "";
    // Convert to lowercase for consistency
    return `${value} ${unit.toLowerCase()}`;
  };

  const handleSubmit = async () => {
    if (!language) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please select a preferred language."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (!artefactRetention.unit || !artefactRetention.value) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Artefact Retention Policy is required."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (!logRetention.unit || !logRetention.value) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Log Retention Policy is required."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (isChecked) {
      if (!clientId || !clientSecret || !appName || !redirectUrl) {
        toast.error(
          (props) => (
            <CustomToast {...props} type="error" message={"Please fill all the fields."} />
          ),
          { icon: false }
        );
        return;
      }

      else {
        const digilockerBody = {
          clientId: clientId,
          clientSecret: clientSecret,
          codeVerifier: appName,
          redirectUri: redirectUrl,
        };

        const digilockerHeaders = {
          "business-id": businessId,
          "scope-level": "TENANT",
          "tenant-id": tenantId,
          "x-session-token": token,
        };

        try {
          const resp = await dispatch(saveDigilockerConfig(digilockerBody, digilockerHeaders));
          if (resp.status === 201) {
            toast.success(
              (props) => (
                <CustomToast {...props} type="success" message={"Digilocker config saved successfully."} />
              ),
              { icon: false }
            );
          }
          else {
            console.error("Error saving digilocker config:", resp);
          }
        } catch (error) {
          console.error("Error saving digilocker config:", error);
        }
      }
    }

    const requestBody = {
      configurationJson: {
        isDigilockerIntegration: isChecked,
        isApaarIntegration: isApaarChecked,
        preferredLanguage: language,
        artefactRetention,
        logRetention,
        isMultilingualSupport: isMultiChecked,
        isBlockchainEnabled: isBlockchainChecked,
      },
    };

    const headers = {
      "business-id": businessId,
      "scope-level": "tenant",
      "tenant-id": tenantId,
      "x-session-token": token,
    };

    const putHeaders = {
      "tenant-id": tenantId,
      "business-id": businessId,
      "x-session-token": token,
    };

    if (!configId) {
      try {
        const resp = await dispatch(saveConsentConfig(requestBody, headers));
        if (resp.status === 201) {
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
    else {
      try {


        const resp = await dispatch(updateConsentConfig(requestBody, putHeaders, configId));
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


  useEffect(() => {
    setArtefactRetention({
      value: artefactInput ? String(artefactInput) : "",
      unit: duration || "YEARS",
    });
  }, [artefactInput, duration]);

  useEffect(() => {
    setLogRetention({
      value: logInput ? String(logInput) : "",
      unit: logDuration || "YEARS",
    });
  }, [logInput, logDuration]);


  useEffect(() => {

    const fetchConsentDetails = async () => {
      try {
        setLoading(true);
        const response = await getConsentDetails(token, tenantId, businessId);

        const config = response?.searchList?.[0]?.configurationJson;
        setConfigId(response?.searchList?.[0]?.configId);
        if (!config) {
          setLoading(false);
          return;
        }

        setIsChecked(config.isDigilockerIntegration);
        setIsApaarChecked(config.isApaarIntegration);
        setIsMultiChecked(config.isMultilingualSupport);
        setIsBlockchainChecked(config.isBlockchainEnabled);
        setLanguage(config.preferredLanguage || "");

        // Check if multilingual config exists and update Redux
        if (config.isMultilingualSupport) {
          try {
            const txnId = generateTransactionId();
            const multilingualResponse = await fetch(
              config.translator_translateConfig,
              {
                method: 'GET',
                headers: {
                  'accept': 'application/json',
                  'Content-Type': 'application/json',
                  'tenantid': tenantId,
                  'businessid': businessId,
                  'txn': txnId,
                  'x-session-token': token,
                },
              }
            );
            if (multilingualResponse.ok) {
              dispatch({ type: "SAVE_MULTILINGUAL_CONFIG", payload: true });
            }
          } catch (err) {
            console.error("Error checking multilingual config:", err);
          }
        }


        setArtefactInput(String(config.artefactRetention?.value ?? ""));
        setDuration(config.artefactRetention?.unit ?? "");

        setLogInput(String(config.logRetention?.value ?? ""));
        setLogDuration(config.logRetention?.unit ?? "");


        setArtefactRetention({
          value: String(config.artefactRetention?.value ?? ""),
          unit: config.artefactRetention?.unit ?? "",
        });
        setLogRetention({
          value: String(config.logRetention?.value ?? ""),
          unit: config.logRetention?.unit ?? "",
        });

      } catch (error) {
        console.error("Error fetching consent details:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchConsentDetails();
  }, [token, tenantId, businessId]
  );

  useEffect(() => {

    const fetchDigilockerDetails = async () => {
      try {
        const response = await getDigilockerDetails(token, tenantId, businessId);

        setClientId(response?.searchList?.[0]?.clientId);
        setClientSecret(response?.searchList?.[0]?.clientSecret);
        setAppName(response?.searchList?.[0]?.codeVerifier);
        setRedirectUrl(response?.searchList?.[0]?.redirectUri);

      } catch (error) {
        console.error("Error fetching digilocker details:", error);
      }
    };

    fetchDigilockerDetails();
  }, [token, tenantId, businessId, isChecked]
  );

  // Fetch multilingual config
  const fetchMultilingualConfig = async () => {
    if (!tenantId || !token || !businessId) {
      return;
    }

    try {
      setLoadingMultilingualConfig(true);
      const txnId = generateTransactionId();

      const response = await fetch(
        config.translator_translateConfig,
        {
          method: 'GET',
          headers: {
            'accept': 'application/json',
            'Content-Type': 'application/json',
            'tenantid': tenantId,
            'businessid': businessId,
            'txn': txnId,
            'x-session-token': token,
          },
        }
      );

      if (response.ok) {
        const data = await response.json();

        if (data.config) {
          // Use fetched values if available
          setApiBaseUrl(data.config.apiBaseUrl || "");
          setProvider(data.config.provider || "");
          setModelPipelineEndpoint(data.config.modelPipelineEndpoint || "");
          setCallbackUrl(data.config.callbackUrl || "");
          setUserId(data.config.userId || "");
          setApiKey(data.config.apiKey || "");
          setPipelineId(data.config.pipelineId || "");
          setSubscriptionKey(data.config.subscriptionKey || "");
          setRegion(data.config.region || "");
          setEndpoint(data.config.endpoint || "");
        }
        if (data.scopeLevel) {
          setScopeLevel(data.scopeLevel);
        }
      } else {
        // If no config exists, fields will remain empty
        console.log("No multilingual config found");
      }
    } catch (err) {
      console.error("Error fetching multilingual config:", err);
    } finally {
      setLoadingMultilingualConfig(false);
    }
  };

  // Save multilingual config
  const handleSaveMultilingualConfig = async () => {
    if (!tenantId || !token || !businessId) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="Missing required credentials. Please log in again."
          />
        ),
        { icon: false }
      );
      return;
    }

    try {
      setSavingMultilingualConfig(true);
      const txnId = generateTransactionId();

      const requestBody = {
        config: {
          apiBaseUrl: apiBaseUrl,
          provider: provider,
          modelPipelineEndpoint: modelPipelineEndpoint,
          callbackUrl: callbackUrl,
          userId: userId,
          apiKey: apiKey,
          pipelineId: pipelineId,
          subscriptionKey: subscriptionKey,
          region: region,
          endpoint: endpoint,
        },
        scopeLevel: scopeLevel,
      };

      const response = await fetch(
        config.translator_translateConfig,
        {
          method: 'POST',
          headers: {
            'accept': 'application/json',
            'Content-Type': 'application/json',
            'tenantid': tenantId,
            'businessid': businessId,
            'txn': txnId,
            'x-session-token': token,
          },
          body: JSON.stringify(requestBody),
        }
      );

      if (response.ok) {
        // Store in Redux
        dispatch({ type: "SAVE_MULTILINGUAL_CONFIG", payload: true });

        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message="Multilingual configuration saved successfully."
            />
          ),
          { icon: false }
        );
        // Keep checkbox checked (already checked when modal opened)
        setShowMultilingualModal(false);
      } else {
        const errorData = await response.json().catch(() => null);
        console.error("Failed to save multilingual config:", errorData);
        throw new Error("Failed to save multilingual configuration");
      }
      } catch (err) {
        console.error("Error saving multilingual config:", err);
        // Uncheck the checkbox if save failed
        setIsMultiChecked(false);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={err.message || "Failed to save multilingual configuration. Please try again."}
            />
          ),
          { icon: false }
        );
      } finally {
        setSavingMultilingualConfig(false);
      }
  };

  const handleCancelMultilingualModal = () => {
    setShowMultilingualModal(false);
    // Keep checkbox checked even if user cancels - they can configure it later
    // The checkbox state remains true
  };

  if (loading) {
    return <ConfigurationShimmer />;
  }

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
      <div className="configurePage">
        <div className="consent-page">
          <div className="consent-heading">
            <Text appearance="heading-s" color="primary-grey-100">
              Configuration
            </Text>
            <div className="tag">
              <Text appearance="body-xs-bold" color="primary-grey-80">
                Consent
              </Text>
            </div>
          </div>
          <div className="consent-content">
            <div className="content-1">
              <div className="consent-dropdown-group">
                <label>Preferred language (Required)</label>

                <select
                  value={language}
                  onChange={handleLanguageChange}
                  id="language-select"
                >
                  <option value="" disabled>
                    Select user's default language for consent communication
                  </option>
                  {languages.map((lang) => (
                    <option key={lang.code} value={lang.code}>
                      {lang.name}
                    </option>
                  ))}
                </select>
                <Text appearance="body-xs" color="primary-grey-80">
                  This will be user’s default language for consent communication
                </Text>
              </div>
            </div>
            {/* <div className="content-2" style={{alignItems:'center'}}>

              <div style={{width: '246%'}}>
                <InputFieldV2
                  helperText="Duration for which consent artefacts are retained"
                  label="Artefact retention policy (Required)"
                  name="Name"
                  value={artefactInput}
                  onChange={(e) => handleArtefactChange(e)}
                  placeholder="Enter a number"
                  size="medium"
                  
                  type='number'
                />
              </div>
              <div className="duration-dropdown-group">
              <select value={duration} onChange={handleArtefactDuration} id="language-select" >
                <option value="" disabled> Duration </option>
                <option value="DAYS">DAYS</option>
                <option value="MONTHS">MONTHS</option>
                <option value="YEARS">YEARS</option>
              </select>
              </div>
              
              
            </div> */}

            {/* <div className="content-2" style={{alignItems:'center'}}>
              <div style={{width: '246%'}}>
                <InputFieldV2
                  helperText="Duration for which activity logs are stored"
                  label="Log retention policy (Required)"
                  name="Name"
                  value={logInput}
                  onChange={(e) => handleLogChange(e)}
                  placeholder="Enter a number"
                  size="medium"
                  type="number"
                />
              </div>
              <div className="duration-dropdown-group">
                <select value={logDuration} onChange={handleLogDuration} id="language-select" >
                  <option value="" disabled> Duration </option>
                  <option value="DAYS">DAYS</option>
                  <option value="MONTHS">MONTHS</option>
                  <option value="YEARS">YEARS</option>
                </select>
              </div>

            </div> */}
            <div className="content-4">

              {/* <InputCheckbox
                helperText=""
                label="Connectivity with APAAR"
                name="Name"
                checked={isApaarChecked}
                onClick={() => setIsApaarChecked((prev) => !prev)}
                size="medium"
                state="none"
              />
              <InputCheckbox
                helperText=""
                label="Connectivity with Blockchain"
                name="Name"
                checked={isBlockchainChecked}
                onClick={() => setIsBlockchainChecked((prev) => !prev)}
                size="medium"
                state="none"
              /> */}

              <InputCheckbox
                helperText=""
                label="Multilingual support"
                name="Name"
                checked={isMultiChecked}
                onClick={() => {
                  if (!isMultiChecked) {
                    // Check the checkbox and open modal
                    setIsMultiChecked(true);
                    setShowMultilingualModal(true);
                    fetchMultilingualConfig();
                  } else {
                    // If already checked, allow unchecking
                    setIsMultiChecked(false);
                  }
                }}
                size="medium"
                state="none"
              />

              <InputCheckbox
                helperText=""
                label="Connectivity with DigiLocker"
                name="Name"
                checked={isChecked}
                onClick={() => setIsChecked((prev) => !prev)}
                size="medium"
                state="none"
              />

              {isChecked && (
                <div style={{ marginTop: "1rem" }}>
                  <InputFieldV2
                    label="Client ID"
                    name="clientId"
                    value={clientId}
                    onChange={(e) => setClientId(e.target.value)}
                    placeholder="Enter Client ID"
                    size="small"
                    helperText="Client ID provided by DigiLocker"
                  />
                  <br></br>
                  <InputFieldV2
                    label="Client Secret"
                    name="clientSecret"
                    value={clientSecret}
                    onChange={(e) => setClientSecret(e.target.value)}
                    placeholder="Enter Client Secret"
                    size="small"
                    helperText="Client Secret provided by DigiLocker"
                  />
                  <br></br>
                  <InputFieldV2
                    label="Code verifier"
                    name="appName"
                    value={appName}
                    onChange={(e) => setAppName(e.target.value)}
                    placeholder="Enter Code verifier"
                    size="small"
                    helperText="Code verifier registered with DigiLocker"
                  />
                  <br></br>
                  <InputFieldV2
                    label="Redirect URL"
                    name="redirectUrl"
                    value={redirectUrl}
                    onChange={(e) => setRedirectUrl(e.target.value)}
                    placeholder="Enter Redirect URL"
                    size="small"
                    helperText="Redirect URL registered with DigiLocker"
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
        </div>
      </div>

      {/* Multilingual Config Modal */}
      {showMultilingualModal && (
        <div className="modal-overlay" onClick={handleCancelMultilingualModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '600px', maxHeight: '90vh', overflowY: 'auto' }}>
            <div className="modal-header">
              <h2 style={{ margin: 0, fontSize: '20px', fontWeight: '600' }}>
                Multilingual Configuration
              </h2>
              <button
                className="modal-close-btn"
                onClick={handleCancelMultilingualModal}
                style={{
                  background: 'none',
                  border: 'none',
                  fontSize: '24px',
                  cursor: 'pointer',
                  color: '#666',
                }}
              >
                ×
              </button>
            </div>

            <div className="modal-body">
              {loadingMultilingualConfig ? (
                <div style={{ textAlign: 'center', padding: '20px' }}>
                  <Text appearance="body-s" color="primary-grey-80">
                    Loading configuration...
                  </Text>
                </div>
              ) : (
                <>
                  <div style={{ marginBottom: '16px' }}>
                    <InputFieldV2
                      label="API Base URL (Required)"
                      value={apiBaseUrl}
                      onChange={(e) => setApiBaseUrl(e.target.value)}
                      placeholder="Enter API Base URL"
                      size="small"
                    />
                  </div>

                  <div style={{ marginBottom: '16px' }}>
                    <InputFieldV2
                      label="Provider (Required)"
                      value={provider}
                      onChange={(e) => setProvider(e.target.value)}
                      placeholder="Enter Provider"
                      size="small"
                    />
                  </div>

                  <div style={{ marginBottom: '16px' }}>
                    <InputFieldV2
                      label="Model Pipeline Endpoint(Required)"
                      value={modelPipelineEndpoint}
                      onChange={(e) => setModelPipelineEndpoint(e.target.value)}
                      placeholder="Enter Model Pipeline Endpoint"
                      size="small"
                    />
                  </div>

                  <div style={{ marginBottom: '16px' }}>
                    <InputFieldV2
                      label="Callback URL (Required)"
                      value={callbackUrl}
                      onChange={(e) => setCallbackUrl(e.target.value)}
                      placeholder="Enter Callback URL"
                      size="small"
                    />
                  </div>

                  <div style={{ marginBottom: '16px' }}>
                    <InputFieldV2
                      label="User ID (Required)"
                      value={userId}
                      onChange={(e) => setUserId(e.target.value)}
                      placeholder="Enter User ID"
                      size="small"
                    />
                  </div>

                  <div style={{ marginBottom: '16px' }}>
                    <InputFieldV2
                      label="API Key (Required)"
                      value={apiKey}
                      onChange={(e) => setApiKey(e.target.value)}
                      placeholder="Enter API Key"
                      size="small"
                      type="password"
                    />
                  </div>

                  <div style={{ marginBottom: '16px' }}>
                    <InputFieldV2
                      label="Pipeline ID (Required)"
                      value={pipelineId}
                      onChange={(e) => setPipelineId(e.target.value)}
                      placeholder="Enter Pipeline ID"
                      size="small"
                    />
                  </div>

                  <div style={{ marginBottom: '16px' }}>
                    <InputFieldV2
                      label="Subscription Key"
                      value={subscriptionKey}
                      onChange={(e) => setSubscriptionKey(e.target.value)}
                      placeholder="Enter Subscription Key"
                      size="small"
                      type="password"
                    />
                  </div>

                  <div style={{ marginBottom: '16px' }}>
                    <InputFieldV2
                      label="Region"
                      value={region}
                      onChange={(e) => setRegion(e.target.value)}
                      placeholder="Enter Region"
                      size="small"
                    />
                  </div>

                  <div style={{ marginBottom: '16px' }}>
                    <InputFieldV2
                      label="Endpoint"
                      value={endpoint}
                      onChange={(e) => setEndpoint(e.target.value)}
                      placeholder="Enter Endpoint"
                      size="small"
                    />
                  </div>

                  <div style={{ marginBottom: '24px' }}>
                    <label style={{
                      display: 'block',
                      marginBottom: '8px',
                      fontSize: '14px',
                      fontWeight: '500',
                      color: '#374151',
                    }}>
                      Scope Level
                    </label>
                    <select
                      value={scopeLevel}
                      onChange={(e) => setScopeLevel(e.target.value)}
                      style={{
                        width: '100%',
                        padding: '8px 12px',
                        fontSize: '14px',
                        border: '1px solid #d1d5db',
                        borderRadius: '4px',
                      }}
                    >
                      <option value="BUSINESS">BUSINESS</option>
                      <option value="TENANT">TENANT</option>
                    </select>
                  </div>

                  <div style={{
                    display: 'flex',
                    gap: '12px',
                    justifyContent: 'flex-end',
                  }}>
                    <Button
                      kind="secondary"
                      size="medium"
                      label="Cancel"
                      onClick={handleCancelMultilingualModal}
                      disabled={savingMultilingualConfig}
                    />
                    <ActionButton
                      kind="primary"
                      size="medium"
                      label={savingMultilingualConfig ? "Saving..." : "Save"}
                      onClick={handleSaveMultilingualConfig}
                      disabled={savingMultilingualConfig}
                    />
                  </div>
                </>
              )}
            </div>
          </div>
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

export default Consent;
