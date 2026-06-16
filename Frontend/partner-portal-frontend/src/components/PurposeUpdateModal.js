import "../styles/masterSetup.css";
import { useState, useRef } from "react";
import { useMemo } from "react";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import { CLEAR_SESSION } from "../store/constants/Constants";
import "../styles/sessionModal.css";
import CustomToast from "./CustomToastContainer";

import { Text, InputFieldV2, ActionButton } from "../custom-components";
import { IcClose } from "../custom-components/Icon";
import { Text, InputFieldV2, ActionButton, Spinner } from "../custom-components";
import { IcClose } from "../custom-components/Icon";
import { useEffect } from "react";

import { useDispatch, useSelector } from "react-redux";
import {
  submitPurpose,
  getPurposeList,
  submitUpdatedPurpose,
  getTranslations,
} from "../store/actions/CommonAction";
import WordLimitedTextarea from "./wordLimitedtext";
import { useNavigate } from "react-router-dom";

import { getTranslations } from "../utils/translationApi";
import { IcChevronRight, IcChevronLeft } from '../custom-components/Icon';

import { languages } from "../utils/languages";

const UpdatePurpose = ({ purpose, onClose }) => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const multilingualConfigSaved = useSelector((state) => state.common.multilingualConfigSaved);
  const [showSessionModal, setShowSessionModal] = useState(false);
  const [showMultilingualWarningModal, setShowMultilingualWarningModal] = useState(false);
  const [purposeName, setPurposName] = useState(purpose?.purposeName || "");
  const [purposeCode, setPurposeCode] = useState(purpose?.purposeCode || "");
  const [purposeDesc, setPurposeDesc] = useState(purpose?.purposeDescription || "");
  const [purposeId] = useState(purpose?.purposeId || "");
  const [purposeList, setPurposeList] = useState([]);
  const [loadPurpose, setLoadPurpose] = useState(true);
  
  // Language selection for update purpose modal
  const [modalSelectedLanguage, setModalSelectedLanguage] = useState('en');
  // Store original English values for translation
  const [originalPurposeName, setOriginalPurposeName] = useState(purpose?.purposeName || "");
  const [originalPurposeCode, setOriginalPurposeCode] = useState(purpose?.purposeCode || "");
  const [originalPurposeDesc, setOriginalPurposeDesc] = useState(purpose?.purposeDescription || "");
  // Flag to prevent translation while user is typing
  const isTranslatingRef = useRef(false);
  // Loading state for translation
  const [isTranslating, setIsTranslating] = useState(false);


  const purposeWordLimit = 300;

  const submitUpdateDataPurpose = async () => {
    let errors = [];

    // Use original English values for validation and submission
    const nameToValidate = modalSelectedLanguage === 'en' ? purposeName : originalPurposeName;
    const codeToValidate = modalSelectedLanguage === 'en' ? purposeCode : originalPurposeCode;
    const descToValidate = modalSelectedLanguage === 'en' ? purposeDesc : originalPurposeDesc;

    // Processor Name
    let value1 = nameToValidate.trimStart();
    value1 = value1.replace(/\s+/g, " ");
    const isValid1 = /^(?=.*[A-Za-z])[A-Za-z0-9\s&'()\-]+$/.test(value1);
    if (!isValid1 || value1.trim() === "") {
      errors.push("Please enter valid purpose name.");
    }

    let value2 = codeToValidate.trimStart();
    value2 = value2.replace(/\s+/g, " ");
    const isValid2 = /^(?=.*[A-Za-z])[A-Za-z0-9\s]+$/.test(value2);
    if (!isValid2 || value2.trim() === "") {
      errors.push("Purpose Code must contain letters and/or alphanumeric values");
    }

    let value3 = descToValidate.trimStart();
    value3 = value3.replace(/\s+/g, " ");
    const isValid3 = /^(?=.*[A-Za-z])[A-Za-z0-9\s.,'&()\-]+$/.test(value3);
    if (!isValid3 || value3.trim() === "") {
      errors.push("Please enter valid description.");
    }
    if (value3.length > 200) {
      errors.push("Description must not exceed 200 characters.");
    }

    if (errors.length > 1) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={
              <ul style={{ margin: 0 }}>
                {errors.map((err, idx) => (
                  <li key={idx}>{err}</li>
                ))}
              </ul>
            }
          />
        ),
        { icon: false }
      );
      return;
    } else if (errors.length == 1) {
      toast.error(
        (props) => <CustomToast {...props} type="error" message={errors[0]} />,
        { icon: false }
      );
      return;
    } else {
      try {
        // Submit with original English values (or current if English is selected)
        const nameToSubmit = modalSelectedLanguage === 'en' ? purposeName : originalPurposeName;
        const codeToSubmit = modalSelectedLanguage === 'en' ? purposeCode : originalPurposeCode;
        const descToSubmit = modalSelectedLanguage === 'en' ? purposeDesc : originalPurposeDesc;
        
        let res = await dispatch(
          submitUpdatedPurpose(codeToSubmit, nameToSubmit, descToSubmit, purposeId)
        );

        if (res.status == 201) {
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"Purpose updated successfully."}
              />
            ),
            { icon: false }
          );

          onClose();
          fetchPurposes();
          setPurposName("");
          setPurposeCode("");
          setPurposeDesc("");
        }
      } catch (err) {
        if (err[0].errorCode == "JCMP4003" || err[0].errorCode == "JCMP4001") {
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
          setShowSessionModal(true);
          dispatch({ type: CLEAR_SESSION });
          setTimeout(() => {
            setShowSessionModal(false);
            navigate("/adminLogin");
          }, 1235000);
        } else {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Error occured while updating purpose."}
              />
            ),
            { icon: false }
          );
        }
        console.log("Error in update purpose", err);
      }
    }
  };

  const handlePurposeNameBlur = (e) => {
    let value = e.target.value;
    value.replace(/\s+/g, "");
    setPurposName(value);
    // If typing in English, store as original
    if (modalSelectedLanguage === 'en') {
      setOriginalPurposeName(value);
    }
  };

  const handlePurposeCodeBlur = (e) => {
    let value = e.target.value.replace(/\s+/g, "");
    setPurposeCode(value);
    // If typing in English, store as original
    if (modalSelectedLanguage === 'en') {
      setOriginalPurposeCode(value);
    }
  };

  const handlePurposeDescChange = (value) => {
    // Don't update if we're currently translating (to prevent flickering)
    if (isTranslatingRef.current) {
      return;
    }
    
    // Always update the display value immediately for responsive typing
    setPurposeDesc(value || "");
    
    // If typing in English, store as original English value
    if (modalSelectedLanguage === 'en') {
      setOriginalPurposeDesc(value || "");
    }
  };

  // Translation function for input fields
  const translateInputFields = async (name, code, desc, targetLanguage) => {
    // Set flag to prevent user input from interfering
    isTranslatingRef.current = true;
    setIsTranslating(true);
    
    try {
      const targetLang = targetLanguage || modalSelectedLanguage;
      
      if (targetLang === 'en') {
        // If English, use original values
        setPurposName(originalPurposeName || name || "");
        setPurposeCode(originalPurposeCode || code || "");
        setPurposeDesc(originalPurposeDesc || desc || "");
        return;
      }

      // Use original English values for translation
      const nameToTranslate = originalPurposeName || name || "";
      const codeToTranslate = originalPurposeCode || code || "";
      const descToTranslate = originalPurposeDesc || desc || "";

      const textsToTranslate = [];
      if (nameToTranslate && nameToTranslate.trim()) {
        textsToTranslate.push({ id: 'purpose-name', source: nameToTranslate });
      }
      if (codeToTranslate && codeToTranslate.trim()) {
        textsToTranslate.push({ id: 'purpose-code', source: codeToTranslate });
      }
      if (descToTranslate && descToTranslate.trim()) {
        textsToTranslate.push({ id: 'purpose-desc', source: descToTranslate });
      }

      if (textsToTranslate.length === 0) {
        // No text to translate, clear fields
        setPurposName("");
        setPurposeCode("");
        setPurposeDesc("");
        return;
      }

      // Always translate from English to target language
      const translationResult = await dispatch(getTranslations(textsToTranslate, targetLang, 'en'));
      
      if (translationResult && translationResult.output && Array.isArray(translationResult.output)) {
        const translatedName = translationResult.output.find(t => t.id === 'purpose-name')?.target || nameToTranslate;
        const translatedCode = translationResult.output.find(t => t.id === 'purpose-code')?.target || codeToTranslate;
        const translatedDesc = translationResult.output.find(t => t.id === 'purpose-desc')?.target || descToTranslate;
        
        // Update all fields at once to prevent flickering
        setPurposName(translatedName);
        setPurposeCode(translatedCode);
        setPurposeDesc(translatedDesc);
      }
    } catch (error) {
      console.error('Error translating input fields:', error);
      // On error, keep original values
      const nameToTranslate = originalPurposeName || name || "";
      const codeToTranslate = originalPurposeCode || code || "";
      const descToTranslate = originalPurposeDesc || desc || "";
      setPurposName(nameToTranslate);
      setPurposeCode(codeToTranslate);
      setPurposeDesc(descToTranslate);
    } finally {
      // Reset flag after translation completes with a small delay
      // to ensure WordLimitedTextarea has processed the value change
      setTimeout(() => {
        isTranslatingRef.current = false;
        setIsTranslating(false);
      }, 100);
    }
  };

  // Handle language change in modal
  const handleModalLanguageChange = async (e) => {
    const selectedLang = e.target.value;
    const previousLang = modalSelectedLanguage;
    
    // Don't do anything if same language is selected
    if (selectedLang === previousLang) {
      return;
    }
    
    // Check if multilingual config is saved (only for non-English languages)
    if (selectedLang !== 'en' && !multilingualConfigSaved) {
      setShowMultilingualWarningModal(true);
      // Reset dropdown to previous value
      e.target.value = previousLang || "";
      return;
    }
    
    // Update language state first to prevent re-triggering
    setModalSelectedLanguage(selectedLang);
    
    // If switching to English
    if (selectedLang === 'en') {
      // If we have original English values, use them
      if (originalPurposeName || originalPurposeCode || originalPurposeDesc) {
        setPurposName(originalPurposeName || "");
        setPurposeCode(originalPurposeCode || "");
        setPurposeDesc(originalPurposeDesc || "");
      } else if (previousLang !== 'en' && (purposeName || purposeCode || purposeDesc)) {
        // No originals stored but we have values, translate current values back to English
        setIsTranslating(true);
        try {
          const textsToTranslate = [];
          if (purposeName && purposeName.trim()) {
            textsToTranslate.push({ id: 'purpose-name', source: purposeName });
          }
          if (purposeCode && purposeCode.trim()) {
            textsToTranslate.push({ id: 'purpose-code', source: purposeCode });
          }
          if (purposeDesc && purposeDesc.trim()) {
            textsToTranslate.push({ id: 'purpose-desc', source: purposeDesc });
          }
          
          if (textsToTranslate.length > 0) {
            const translationResult = await dispatch(getTranslations(textsToTranslate, 'en', previousLang));
            
            if (translationResult && translationResult.output && Array.isArray(translationResult.output)) {
              const englishName = translationResult.output.find(t => t.id === 'purpose-name')?.target || purposeName;
              const englishCode = translationResult.output.find(t => t.id === 'purpose-code')?.target || purposeCode;
              const englishDesc = translationResult.output.find(t => t.id === 'purpose-desc')?.target || purposeDesc;
              
              setOriginalPurposeName(englishName);
              setOriginalPurposeCode(englishCode);
              setOriginalPurposeDesc(englishDesc);
              
              setPurposName(englishName);
              setPurposeCode(englishCode);
              setPurposeDesc(englishDesc);
            }
          }
        } catch (error) {
          console.error('Error translating back to English:', error);
        } finally {
          setIsTranslating(false);
        }
      } else {
        // No values at all, clear fields
        setPurposName("");
        setPurposeCode("");
        setPurposeDesc("");
      }
    } 
    // If switching to non-English
    else {
      // First, ensure we have English originals
      // If we don't have originals, we need to get them first
      if (!originalPurposeName && !originalPurposeCode && !originalPurposeDesc) {
        // If previous language was English and we have values, store them as originals
        if (previousLang === 'en' && (purposeName || purposeCode || purposeDesc)) {
          setOriginalPurposeName(purposeName || "");
          setOriginalPurposeCode(purposeCode || "");
          setOriginalPurposeDesc(purposeDesc || "");
          // Now translate to the new language
          await translateInputFields(purposeName, purposeCode, purposeDesc, selectedLang);
        } 
        // If previous language was non-English, translate current values back to English first
        else if (previousLang !== 'en' && (purposeName || purposeCode || purposeDesc)) {
          setIsTranslating(true);
          try {
            const textsToTranslate = [];
            if (purposeName && purposeName.trim()) {
              textsToTranslate.push({ id: 'purpose-name', source: purposeName });
            }
            if (purposeCode && purposeCode.trim()) {
              textsToTranslate.push({ id: 'purpose-code', source: purposeCode });
            }
            if (purposeDesc && purposeDesc.trim()) {
              textsToTranslate.push({ id: 'purpose-desc', source: purposeDesc });
            }
            
            if (textsToTranslate.length > 0) {
              // Translate from previous language to English
              const translationResult = await dispatch(getTranslations(textsToTranslate, 'en', previousLang));
              
              if (translationResult && translationResult.output && Array.isArray(translationResult.output)) {
                const englishName = translationResult.output.find(t => t.id === 'purpose-name')?.target || purposeName;
                const englishCode = translationResult.output.find(t => t.id === 'purpose-code')?.target || purposeCode;
                const englishDesc = translationResult.output.find(t => t.id === 'purpose-desc')?.target || purposeDesc;
                
                setOriginalPurposeName(englishName);
                setOriginalPurposeCode(englishCode);
                setOriginalPurposeDesc(englishDesc);
                
                // Now translate from English to the new selected language
                await translateInputFields(englishName, englishCode, englishDesc, selectedLang);
              }
            }
          } catch (error) {
            console.error('Error translating to English first:', error);
            setIsTranslating(false);
          }
        } else {
          // No values at all, clear fields
          setPurposName("");
          setPurposeCode("");
          setPurposeDesc("");
        }
      } else {
        // We have English originals, translate to new language
        await translateInputFields(originalPurposeName, originalPurposeCode, originalPurposeDesc, selectedLang);
      }
    }
  };
  const closeupdateDataPurposeModal = () => {
    setUpdateDataPurposeModal(false);
    setPurposeDesc("");
  };

  const [updateDataPurposeModal, setUpdateDataPurposeModal] = useState(false);

  const fetchPurposes = async () => {
    try {
      let res = await dispatch(getPurposeList()); // wait for thunk to finish
      if (res?.data?.searchList) {
        setPurposeList(res.data.searchList);
        setLoadPurpose(false);
      }
      if (res?.status == 403) {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Facing Network Error..Please try again later."}
            />
          ),
          { icon: false }
        );
      }
    } catch (err) {
      if (err[0].errorCode == "JCMP4003" || err[0].errorCode == "JCMP4001") {
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
      } else {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Error in fetching purpose."}
            />
          ),
          { icon: false }
        );
      }
      console.log("Error fetching purposes:", err);
    }
  };
  
  useEffect(() => {
    if (purpose) {
      const name = purpose.purposeName || "";
      const code = purpose.purposeCode || "";
      const desc = purpose.purposeDescription || "";
      
      setPurposName(name);
      setPurposeCode(code);
      setPurposeDesc(desc);
      
      // Store as original English values
      setOriginalPurposeName(name);
      setOriginalPurposeCode(code);
      setOriginalPurposeDesc(desc);
    }
  }, [purpose]);

  useEffect(() => {
    fetchPurposes();
  }, [dispatch]);
  
  return (
    <>
      {" "}
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
      <div className="modal-outer-container">
        <div className="master-set-up-modal-container" style={{ position: 'relative' }}>
          {isTranslating && (
            <div
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                backgroundColor: 'rgba(255, 255, 255, 0.8)',
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                zIndex: 1000,
                borderRadius: '8px',
              }}
            >
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
                <Spinner
                  kind="normal"
                  label=""
                  labelPosition="right"
                  size="medium"
                />
                <Text appearance="body-s" color="primary-grey-80">
                  Translating...
                </Text>
              </div>
            </div>
          )}
          <div className="modal-close-btn-container">
            <ActionButton
              onClick={onClose}
              icon={<IcClose />}
              kind="tertiary"
            ></ActionButton>
          </div>{" "}
          <Text appearance="heading-xs" color="primary-grey-100">
            {" "}
            Update purpose{" "}
          </Text>
          <div className="language-tabs-container">
                <select
                  value={modalSelectedLanguage}
                  onChange={handleModalLanguageChange}
                >
                  {languages.map((lang) => (
                    <option key={lang.value} value={lang.value}>
                      {lang.label}
                    </option>
                  ))}
                </select>
            </div>
          <div className="title-margin "></div>
          <InputFieldV2
            label="Purpose name (Required)"
            value={purposeName}
            name="purposeName"
            onChange={(e) => {
              const value = e.target.value;
              setPurposName(value);
            }}
            onBlur={handlePurposeNameBlur}
          ></InputFieldV2>{" "}
          <br></br>
          <InputFieldV2
            label="Purpose code (Required)"
            name="purposeCode"
            value={purposeCode}
            onChange={(e) => {
              const value = e.target.value.replace(/\s+/g, "");
              setPurposeCode(value);
            }}
            onBlur={handlePurposeCodeBlur}
          ></InputFieldV2>
          <br></br>
          <WordLimitedTextarea
            value={purposeDesc || ""}
            onTextChange={handlePurposeDescChange}
          />
          <div className="modal-add-btn-container">
            {" "}
            <ActionButton
              label="Update"
              onClick={submitUpdateDataPurpose}
            ></ActionButton>{" "}
          </div>{" "}
        </div>{" "}
      </div>

      {/* Multilingual Warning Modal */}
      {showMultilingualWarningModal && (
        <div className="modal-overlay" onClick={() => setShowMultilingualWarningModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '500px' }}>
            <div className="modal-header">
              <h2 style={{ margin: 0, fontSize: '20px', fontWeight: '600' }}>
                Multilingual Support Required
              </h2>
              <button
                className="modal-close-btn"
                onClick={() => setShowMultilingualWarningModal(false)}
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
              <Text appearance="body-s" color="primary-grey-80" style={{ marginBottom: '24px' }}>
                You need to configure multilingual support in the consent configuration to enable the translate functionality.
              </Text>

              <div style={{
                display: 'flex',
                gap: '12px',
                justifyContent: 'flex-end',
              }}>
                <Button
                  kind="secondary"
                  size="medium"
                  label="Cancel"
                  onClick={() => setShowMultilingualWarningModal(false)}
                />
                <ActionButton
                  kind="primary"
                  size="medium"
                  label="Go to Configuration"
                  onClick={() => {
                    setShowMultilingualWarningModal(false);
                    navigate("/consent");
                  }}
                />
              </div>
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
export default UpdatePurpose;
