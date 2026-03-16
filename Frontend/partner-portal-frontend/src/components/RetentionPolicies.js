import React, { useState, useEffect, useRef } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { InputFieldV2, Text, ActionButton, Spinner } from '../custom-components';
import config from '../utils/config';
import '../styles/systemConfiguration.css';
import '../styles/pageConfiguration.css';
import '../styles/retentionPolicies.css';
import '../styles/toast.css';
import { Slide, ToastContainer, toast } from 'react-toastify';
import CustomToast from './CustomToastContainer';
import { isSandboxMode } from '../utils/sandboxMode';

const RetentionPolicies = () => {
  const token = useSelector((state) => state.common.session_token);
  const tenantId = useSelector((state) => state.common.tenant_id);
  const businessId = useSelector((state) => state.common.business_id);

  const [retentionId, setRetentionId] = useState('');
  const [isUpdateMode, setIsUpdateMode] = useState(false);
  const [disableSaveBtn, setDisableSaveBtn] = useState(false);
  const [loading, setLoading] = useState(true);

  // Form state
  const [consentArtifactValue, setConsentArtifactValue] = useState('');
  const [consentArtifactUnit, setConsentArtifactUnit] = useState('YEAR');

  const [cookieConsentValue, setCookieConsentValue] = useState('');
  const [cookieConsentUnit, setCookieConsentUnit] = useState('YEAR');

  const [grievanceValue, setGrievanceValue] = useState('');
  const [grievanceUnit, setGrievanceUnit] = useState('YEAR');

  const [logsValue, setLogsValue] = useState('');
  const [logsUnit, setLogsUnit] = useState('YEAR');

  const [dataRetentionValue, setDataRetentionValue] = useState('');
  const [dataRetentionUnit, setDataRetentionUnit] = useState('YEAR');

  // Refs for number inputs to prevent scroll from changing values
  const consentArtifactInputRef = useRef(null);
  const cookieConsentInputRef = useRef(null);
  const grievanceInputRef = useRef(null);
  const logsInputRef = useRef(null);
  const dataRetentionInputRef = useRef(null);

  // Prevent number input from changing on scroll
  useEffect(() => {
    if (loading) return; // Wait for inputs to render

    const handleWheel = (e) => {
      // Check if the target or active element is a number input
      const target = e.target;
      const activeElement = document.activeElement;
      
      const isNumberInput = (element) => {
        return element && (
          element.type === 'number' ||
          (element.tagName === 'INPUT' && element.getAttribute('type') === 'number')
        );
      };

      if (isNumberInput(target) || isNumberInput(activeElement)) {
        e.preventDefault();
        e.stopPropagation();
        if (activeElement && isNumberInput(activeElement)) {
          activeElement.blur();
        }
      }
    };

    // Use capture phase to catch the event early
    document.addEventListener('wheel', handleWheel, { passive: false, capture: true });

    return () => {
      document.removeEventListener('wheel', handleWheel, { capture: true });
    };
  }, [loading]); // Re-run when loading changes to ensure inputs are rendered

  // Fetch existing retention policy on mount
  useEffect(() => {
    const fetchRetentionPolicy = async () => {
      // In sandbox mode, use default values if missing
      const finalBusinessId = businessId || (isSandboxMode() ? 'sandbox-business-id' : null);
      const finalTenantId = tenantId || (isSandboxMode() ? 'sandbox-tenant-id' : null);
      const finalToken = token || (isSandboxMode() ? 'sandbox-session-token-12345' : null);
      
      // Skip if token or businessId is not available yet (and not in sandbox mode)
      if (!finalToken || !finalBusinessId || !finalTenantId) {
        if (!isSandboxMode()) {
          console.log('⏳ Waiting for credentials to load...');
          setLoading(false);
          return;
        }
      }

      setLoading(true);

      try {
        const authToken = finalToken.startsWith('Bearer ') ? finalToken : `Bearer ${finalToken}`;
        
        console.log('🔍 Fetching existing retention policy...');
        
        // Try to fetch existing retention policy for this business
        const response = await fetch(
          `${config.BASE_URL_RETENTION}/retention/search?businessId=${finalBusinessId}`,
          {
            method: 'GET',
            headers: {
              'accept': '*/*',
              'txn': finalTenantId,
              'tenant-id': finalTenantId,
              'business-id': finalBusinessId,
              'x-session-token': authToken,
              'Content-Type': 'application/json'
            }
          }
        );

        if (response.ok) {
          const data = await response.json();
          console.log('Fetched retention policy:', data);

          if (data && data.retentions) {
            const retentions = data.retentions;
            
            // Populate form with existing data
            if (retentions.consent_artifact_retention) {
              setConsentArtifactValue(String(retentions.consent_artifact_retention.value || ''));
              setConsentArtifactUnit(retentions.consent_artifact_retention.unit?.toUpperCase() || 'YEAR');
            }
            
            if (retentions.cookie_consent_artifact_retention) {
              setCookieConsentValue(String(retentions.cookie_consent_artifact_retention.value || ''));
              setCookieConsentUnit(retentions.cookie_consent_artifact_retention.unit?.toUpperCase() || 'MONTH');
            }
            
            if (retentions.grievance_retention) {
              setGrievanceValue(String(retentions.grievance_retention.value || ''));
              setGrievanceUnit(retentions.grievance_retention.unit?.toUpperCase() || 'YEAR');
            }
            
            if (retentions.logs_retention) {
              setLogsValue(String(retentions.logs_retention.value || ''));
              setLogsUnit(retentions.logs_retention.unit?.toUpperCase() || 'YEAR');
            }
            
            if (retentions.data_retention) {
              setDataRetentionValue(String(retentions.data_retention.value || ''));
              setDataRetentionUnit(retentions.data_retention.unit?.toUpperCase() || 'MONTH');
            }

            if (data.retentionId) {
              setRetentionId(data.retentionId);
              setIsUpdateMode(true);
            }
          }
        }
      } catch (error) {
        console.error('Failed to fetch retention policy:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchRetentionPolicy();
  }, [businessId, tenantId, token]);

  const validateForm = () => {
    const errors = [];

    if (!consentArtifactValue || consentArtifactValue <= 0) {
      errors.push('Consent Artifact Retention value is required');
    }
    if (!cookieConsentValue || cookieConsentValue <= 0) {
      errors.push('Cookie Consent Artifact Retention value is required');
    }
    if (!grievanceValue || grievanceValue <= 0) {
      errors.push('Grievance Retention value is required');
    }
    if (!logsValue || logsValue <= 0) {
      errors.push('Logs Retention value is required');
    }
    if (!dataRetentionValue || dataRetentionValue <= 0) {
      errors.push('Data Retention value is required');
    }

    if (errors.length > 0) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={
              errors.length === 1 ? errors[0] : (
                <ul style={{ margin: 0 }}>
                  {errors.map((err, idx) => (
                    <li key={idx}>{err}</li>
                  ))}
                </ul>
              )
            }
          />
        ),
        { icon: false }
      );
      return false;
    }

    return true;
  };

  const handleSave = async () => {
    if (!validateForm()) return;

    // Check if required credentials are available
    if (!token) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="Authentication token is missing. Please login again."
          />
        ),
        { icon: false }
      );
      return;
    }

    // In sandbox mode, use default values if missing
    const finalBusinessId = businessId || (isSandboxMode() ? 'sandbox-business-id' : null);
    const finalTenantId = tenantId || (isSandboxMode() ? 'sandbox-tenant-id' : null);
    
    if (!finalBusinessId || !finalTenantId) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="Business or Tenant information is missing. Please refresh and try again."
          />
        ),
        { icon: false }
      );
      return;
    }

    setDisableSaveBtn(true);

    try {
      const authToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
      
      const payload = {
        businessId: finalBusinessId,
        retentions: {
          consent_artifact_retention: {
            value: parseInt(consentArtifactValue),
            unit: consentArtifactUnit.toLowerCase()
          },
          cookie_consent_artifact_retention: {
            value: parseInt(cookieConsentValue),
            unit: cookieConsentUnit.toLowerCase()
          },
          grievance_retention: {
            value: parseInt(grievanceValue),
            unit: grievanceUnit.toLowerCase()
          },
          logs_retention: {
            value: parseInt(logsValue),
            unit: logsUnit.toLowerCase()
          },
          data_retention: {
            value: parseInt(dataRetentionValue),
            unit: dataRetentionUnit.toLowerCase()
          }
        }
      };

      console.log('🔵 Retention Policy Payload:', payload);
      console.log('🔵 API Base URL:', config.BASE_URL_RETENTION);
      console.log('🔵 Mode:', isUpdateMode ? 'UPDATE' : 'CREATE');
      console.log('🔵 Retention ID:', retentionId);
      console.log('🔵 Business ID:', finalBusinessId);
      console.log('🔵 Tenant ID:', finalTenantId);

      let response;
      const url = isUpdateMode && retentionId
        ? `${config.BASE_URL_RETENTION}/retention/update?retentionId=${retentionId}`
        : `${config.BASE_URL_RETENTION}/retention/create`;
      
      console.log('🔵 API URL:', url);
      
      const responseData = await fetch(url, {
        method: isUpdateMode && retentionId ? 'PUT' : 'POST',
        headers: {
          'accept': '*/*',
          'txn': finalTenantId,
          'tenant-id': finalTenantId,
          'business-id': finalBusinessId,
          'x-session-token': authToken,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      })
      response = await responseData.json();


      console.log('🔵 Response Status:', response.status);

      if (response?.businessId) {
        const data =  response;
        console.log('✅ Save response:', data);

        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message={isUpdateMode ? 'Retention policy updated successfully' : 'Retention policy created successfully'}
            />
          ),
          { icon: false }
        );

        if (data.retentionId && !isUpdateMode) {
          setRetentionId(data.retentionId);
          setIsUpdateMode(true);
        }
      } else {
        const errorData = response;
        console.error('❌ API Error Response:', errorData);
        console.error('❌ Response Status:', response.status);
        console.error('❌ Response Status Text:', response.errorMsg);

  toast.error(
    (props) => (
      <CustomToast
        {...props}
        type="error"
        message={errorData.message || errorData.error || `Failed to save: ${response.errorMsg}`}
      />
    ),
    { icon: false }
  );
}
    } catch (error) {
      console.error('❌ Save error:', error);
      console.error('❌ Error name:', error.name);
      console.error('❌ Error message:', error.message);
      console.error('❌ Error stack:', error.stack);
      
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={`Error: ${error.message || 'Something went wrong. Please try again later.'}`}
          />
        ),
        { icon: false }
      );
    } finally {
      setDisableSaveBtn(false);
    }
  };

  return (
    <>
      <div className="configurePage">
        <div className="systemConfig-outer-div">
          <div className="systemConfig-header-and-badge">
            <Text appearance="heading-s" color="primary-grey-100">
              Retention Policies
            </Text>
            <div className="systemConfig-badge">
              <Text appearance="body-xs-bold" color="primary-grey-80">
                System Configuration
              </Text>
            </div>
          </div>

          {loading ? (
            <div className="retention-loading-container">
              <Spinner kind="normal" size="medium" />
              <Text appearance="body-s" color="primary-grey-80" style={{ marginTop: '16px' }}>
                Loading retention policies...
              </Text>
            </div>
          ) : (
          <div className="retention-single-column">
            <div className="retention-field-row">
              <div className="retention-input-wrapper" ref={consentArtifactInputRef}>
                <InputFieldV2
                  helperText="Retention period for consent artifacts"
                  label="Consent Artifact Retention (Required)"
                  value={consentArtifactValue}
                  onChange={(e) => setConsentArtifactValue(e.target.value.replace(/\D/g, ''))}
                  placeholder="Enter a number"
                  size="medium"
                  type="number"
                />
              </div>
              <div className="retention-dropdown-wrapper">
                <select 
                  value={consentArtifactUnit} 
                  onChange={(e) => setConsentArtifactUnit(e.target.value)}
                  className="retention-unit-dropdown"
                >
                  <option value="DAY">DAY</option>
                  <option value="MONTH">MONTH</option>
                  <option value="YEAR">YEAR</option>
                </select>
              </div>
            </div>

            <div className="retention-field-row">
              <div className="retention-input-wrapper" ref={cookieConsentInputRef}>
                <InputFieldV2
                  helperText="Retention period for cookie consent artifacts"
                  label="Cookie Consent Artifact Retention (Required)"
                  value={cookieConsentValue}
                  onChange={(e) => setCookieConsentValue(e.target.value.replace(/\D/g, ''))}
                  placeholder="Enter a number"
                  size="medium"
                  type="number"
                />
              </div>
              <div className="retention-dropdown-wrapper">
                <select 
                  value={cookieConsentUnit} 
                  onChange={(e) => setCookieConsentUnit(e.target.value)}
                  className="retention-unit-dropdown"
                >
                  <option value="DAY">DAY</option>
                  <option value="MONTH">MONTH</option>
                  <option value="YEAR">YEAR</option>
                </select>
              </div>
            </div>

            <div className="retention-field-row">
              <div className="retention-input-wrapper" ref={grievanceInputRef}>
                <InputFieldV2
                  helperText="Retention period for grievance records"
                  label="Grievance Retention (Required)"
                  value={grievanceValue}
                  onChange={(e) => setGrievanceValue(e.target.value.replace(/\D/g, ''))}
                  placeholder="Enter a number"
                  size="medium"
                  type="number"
                />
              </div>
              <div className="retention-dropdown-wrapper">
                <select 
                  value={grievanceUnit} 
                  onChange={(e) => setGrievanceUnit(e.target.value)}
                  className="retention-unit-dropdown"
                >
                  <option value="DAY">DAY</option>
                  <option value="MONTH">MONTH</option>
                  <option value="YEAR">YEAR</option>
                </select>
              </div>
            </div>

            <div className="retention-field-row">
              <div className="retention-input-wrapper" ref={logsInputRef}>
                <InputFieldV2
                  helperText="Retention period for system logs"
                  label="Logs Retention (Required)"
                  value={logsValue}
                  onChange={(e) => setLogsValue(e.target.value.replace(/\D/g, ''))}
                  placeholder="Enter a number"
                  size="medium"
                  type="number"
                />
              </div>
              <div className="retention-dropdown-wrapper">
                <select 
                  value={logsUnit} 
                  onChange={(e) => setLogsUnit(e.target.value)}
                  className="retention-unit-dropdown"
                >
                  <option value="DAY">DAY</option>
                  <option value="MONTH">MONTH</option>
                  <option value="YEAR">YEAR</option>
                </select>
              </div>
            </div>

            <div className="retention-field-row">
              <div className="retention-input-wrapper" ref={dataRetentionInputRef}>
                <InputFieldV2
                  helperText="Retention period for general data"
                  label="Data Retention (Required)"
                  value={dataRetentionValue}
                  onChange={(e) => setDataRetentionValue(e.target.value.replace(/\D/g, ''))}
                  placeholder="Enter a number"
                  size="medium"
                  type="number"
                />
              </div>
              <div className="retention-dropdown-wrapper">
                <select 
                  value={dataRetentionUnit} 
                  onChange={(e) => setDataRetentionUnit(e.target.value)}
                  className="retention-unit-dropdown"
                >
                  <option value="DAY">DAY</option>
                  <option value="MONTH">MONTH</option>
                  <option value="YEAR">YEAR</option>
                </select>
              </div>
            </div>
          </div>
          )}
        </div>

        {!loading && (
          <div className="content-5">
            <ActionButton
              label="Save"
              onClick={handleSave}
              state={disableSaveBtn ? "disabled" : "normal"}
            />
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
    </>
  );
};

export default RetentionPolicies;
