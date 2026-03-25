import "../styles/masterSetup.css";
import "../styles/PreviewFile.css";
import { useState, useEffect, useMemo, useRef } from "react";
import { Text, InputFieldV2, ActionButton, Icon, Spinner, InputCheckbox } from "../custom-components";
import {
  IcClose, IcEditPen, IcSuccess, IcUpload, IcChevronLeft, IcChevronRight, IcWarningColored,
  IcAttachment, IcDocumentViewer, IcInfo
} from "../custom-components/Icon";
import { useDispatch, useSelector } from "react-redux";
import { CLEAR_SESSION } from "../store/constants/Constants";
import "../styles/sessionModal.css";
import {
  submitDataProcessor,
  getProcessor,
  createProcessor,
  updateProcessor,
  getSystemConfig,
  getTranslations,
} from "../store/actions/CommonAction";
import { decodeBase64 } from "../utils/base64";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";

import WordLimitedTextarea from "./wordLimitedtext";
import DataProcessorUpdateModal from "./DataProcessorUpdateModal";
import { useNavigate } from "react-router-dom";
import { languages } from "../utils/languages";
import { 
  validatePDFFile,           // ✅ Using secure validation from fileValidation.js
  validateCertificateFile,   // ✅ Using secure validation from fileValidation.js
  validateDocumentFile,      // ✅ For agreement documents (already imported)
  formatValidationErrors 
} from "../utils/fileValidation";
const DataProcessor = ({ selectedLanguage }) => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const multilingualConfigSaved = useSelector((state) => state.common.multilingualConfigSaved);
  const businessId = useSelector((state) => state.common.business_id);
  const [dataProcessorModal, setDataProcessorModal] = useState(false);
  const [updateProcessorModal, setUpdateProcessorModal] = useState(false);
  const [viewProcessorModal, setViewProcessorModal] = useState(false);
  const [showSessionModal, setShowSessionModal] = useState(false);
  const [showMultilingualWarningModal, setShowMultilingualWarningModal] = useState(false);
  const [selectedProcessor, setSelectedProcessor] = useState("");

  // State for original, untranslated data from the API
  const [originalProcessorList, setOriginalProcessorList] = useState([]);
  // State for the data displayed in the UI (potentially translated)
  const [processorList, setProcessorList] = useState([]);
  const [uploadCertificate, setUplaodCertificate] = useState("");
  const [uploadCertificateSize, setUplaodCertificateSize] = useState(null);
  const [processorName, setProcessorName] = useState("");
  const [processorCallBackUrl, setProcessorCallBackUrl] = useState("");
  const [systemCallbackUrl, setSystemCallbackUrl] = useState(""); // Store system config callback URL
  const [dataProcessorCertificate, setDataProcessorCertificate] = useState("");
  const [riskAssesmentDocument, setRiskAssesmentDocument] = useState("");
  const [agreementDocument, setAgreementDocument] = useState("");
  const [uploadAgreementDocument, setUploadAgreementDocument] = useState("");
  const [uploadAgreementDocumentSize, setUploadAgreementDocumentSize] = useState(null);
  const [agreementDocumentPreviewUrl, setAgreementDocumentPreviewUrl] = useState("");
  const [processorId, setprocessorId] = useState("");
  const [text, setText] = useState("");
  const [loadProcessor, setLoadProcessor] = useState(true);
  const [riskDocumentPreviewUrl, setRiskDocumentPreviewUrl] = useState("");
  const [certificatePreviewUrl, setCertificatePreviewUrl] = useState("");
  const [emailMobile, setEmailMobile] = useState("");
  const [isSpocNameValid, setIsSpocNameValid] = useState(true);
  const [isCrossBordered, setIsCrossBordered] = useState(false);

  const [formData, setFormData] = useState({
    spoc: {
      name: "",
      mobile: "",
      email: "",
    },
    identityType: "",
  });

  const [formErrors, setFormErrors] = useState({
    spoc: {
      name: null,
      mobile: null,
      email: null,
    },
  });

  const handleEmailMobileChange = (e) => {
    const value = e.target.value.trim();
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    // always update input value (keep in one place so typing works)
    setFormData((prev) => ({
      ...prev,
      spoc: { ...prev.spoc, emailMobile: value }, // <-- temporary field for input
    }));

    // validation + mapping to email field only (no mobile support)
    if (emailRegex.test(value)) {
      setFormData((prev) => ({
        ...prev,
        spoc: { ...prev.spoc, email: value, mobile: "", emailMobile: value },
        identityType: "EMAIL",
      }));
      setFormErrors((prev) => ({ ...prev, emailMobile: null }));
      return;
    }

    // Only accept email format
    setFormErrors((prev) => ({
      ...prev,
      emailMobile: "Please enter a valid Email ID",
    }));
  };

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

  // Language selection for add processor modal
  const [modalSelectedLanguage, setModalSelectedLanguage] = useState('en');
  // Store original English values for translation
  const [originalProcessorName, setOriginalProcessorName] = useState("");
  const [originalProcessorDetails, setOriginalProcessorDetails] = useState("");
  // Flag to prevent translation while user is typing
  const isTranslatingRef = useRef(false);
  // Loading state for translation
  const [isTranslating, setIsTranslating] = useState(false);

  const handlePreviewriskDocument = () => {
    if (riskDocumentPreviewUrl) {
      window.open(riskDocumentPreviewUrl, "_blank");
    }
  };

  const handlePreviewCertificte = () => {
    if (certificatePreviewUrl) {
      window.open(certificatePreviewUrl, "_blank");
    }
  };

  const handleViewProcessor = (item) => {
    setViewProcessorModal(true);
    setSelectedProcessor(item);
  };

  const handleEditProcessor = (item) => {
    setUpdateProcessorModal(true);
    setSelectedProcessor(item);
    setProcessorName(item.dataProcessorName);
    // Use processor's callback URL if exists, otherwise use system callback URL as fallback
    setProcessorCallBackUrl(item.callbackUrl || systemCallbackUrl);
    setText(item.details);
    setprocessorId(item.dataProcessorId);

    if (item.certificate) {
      console.log("Yes I have certificate");
      setDataProcessorCertificate(item.certificate);

      try {
        // Ensure base64 string is cleaned
        let base64Data = item.certificate;

        // If it has a prefix like "data:application/pdf;base64,...", strip it
        if (base64Data.includes(",")) {
          base64Data = base64Data.split(",")[1];
        }

        // Remove any whitespace, newlines, etc.
        base64Data = base64Data.replace(/[\r\n\s]+/g, "");

        // Convert to binary
        const byteCharacters = decodeBase64(base64Data);
        const byteNumbers = new Array(byteCharacters.length);

        for (let i = 0; i < byteCharacters.length; i++) {
          byteNumbers[i] = byteCharacters.charCodeAt(i);
        }

        const byteArray = new Uint8Array(byteNumbers);

        // Create a PDF Blob
        const blob = new Blob([byteArray], { type: "application/pdf" });

        // Create object URL and open in new tab
        const fileURL = URL.createObjectURL(blob);

        // Update state
        setCertificatePreviewUrl(fileURL);
        setUplaodCertificate(item.certificateMeta?.name || "");
        setUplaodCertificateSize(item.certificateMeta?.size || 0);
      } catch (error) {
        console.log("Error while decoding base64 certificate:", error);
        setCertificatePreviewUrl("");
        setUplaodCertificate("");
        setUplaodCertificateSize("");
      }
    } else {
      setCertificatePreviewUrl("");
      setUplaodCertificate("");
      setUplaodCertificateSize("");
      setUplaodRiskAssesmentSize("");
      setUplaodRiskAssesment("");
      setRiskDocumentPreviewUrl("");
    }

    if (item.vendorRiskDocument) {
      console.log("Yes I have vendorRisk");
      setRiskAssesmentDocument(item.vendorRiskDocument);

      // Strip off prefix and clean base64
      let base64Data =
        item.vendorRiskDocument.split(",")[1] || item.vendorRiskDocument;

      // Remove any whitespace or line breaks
      base64Data = base64Data.replace(/[\r\n]+/g, "").replace(/\s+/g, "");

      const byteCharacters = decodeBase64(base64Data);
      const byteNumbers = new Array(byteCharacters.length);

      for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
      }

      const byteArray = new Uint8Array(byteNumbers);
      const blob = new Blob([byteArray], { type: "application/pdf" });

      // Create object URL and open
      const fileURL = URL.createObjectURL(blob);

      setRiskDocumentPreviewUrl(fileURL);
      setUplaodRiskAssesment(item.vendorRiskDocumentMeta.name);
      setUplaodRiskAssesmentSize(item.vendorRiskDocumentMeta.size);
    } else {
      setRiskDocumentPreviewUrl("");
      setUplaodRiskAssesment("");
      setUplaodRiskAssesmentSize("");
    }
  };

  const fetchProcessor = async () => {
    try {
      let res = await dispatch(getProcessor());
      if (res?.status === 200 || res?.status === 201) {
        if (res?.data?.searchList) {
          setOriginalProcessorList(res.data.searchList);
          setProcessorList(res.data.searchList);
          setLoadProcessor(false);
        }
      }
      if (res?.status === 403) {
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
    fetchProcessor();
    fetchSystemConfig(); // Fetch system config on mount to get default callback URL
  }, [dispatch, businessId]);

  // Translation effect for processor list
  useEffect(() => {
    const translateProcessors = async () => {
      const lang = selectedLanguage || 'en';
      if (lang === 'en') {
        setProcessorList(originalProcessorList);
        return;
      }
      if (originalProcessorList.length === 0) return;

      const textsToTranslate = originalProcessorList.flatMap(processor => [
        { id: `processor-${processor.dataProcessorId}-name`, source: processor.dataProcessorName },
        { id: `processor-${processor.dataProcessorId}-details`, source: processor.details }
      ]);

      try {
        const translationResult = await dispatch(getTranslations(textsToTranslate, lang));

        if (translationResult && translationResult.output && Array.isArray(translationResult.output)) {
          const translatedProcessors = originalProcessorList.map(processor => {
            const translatedName = translationResult.output.find(t => t.id === `processor-${processor.dataProcessorId}-name`)?.target || processor.dataProcessorName;
            const translatedDetails = translationResult.output.find(t => t.id === `processor-${processor.dataProcessorId}-details`)?.target || processor.details;
            return {
              ...processor,
              dataProcessorName: translatedName,
              details: translatedDetails
            };
          });
          setProcessorList(translatedProcessors);
        }
      } catch (error) {
        console.error('Error translating processors:', error);
        setProcessorList(originalProcessorList);
      }
    };

    translateProcessors();
  }, [selectedLanguage, originalProcessorList, dispatch]);

  const fetchSystemConfig = async () => {
    try {
      const response = await dispatch(getSystemConfig());
      if (response?.status === 200 || response?.status === 201) {
        const systemConfigInfo = response?.data?.searchList?.[0]?.configurationJson;
        if (systemConfigInfo && systemConfigInfo.baseUrl) {
          // Store system callback URL for fallback display in table
          setSystemCallbackUrl(systemConfigInfo.baseUrl);
        }
      }
    } catch (err) {
      console.log("Error fetching system config:", err);
      // Don't show error to user, just log it
    }
  };

  const openDataProcessorModal = () => {
    setDataProcessorModal(true);
    // Reset to English when opening modal
    setModalSelectedLanguage('en');
    setOriginalProcessorName("");
    setOriginalProcessorDetails("");
  };
  const closeDataProcessorModal = () => {
    setDataProcessorModal(false);
    setUplaodCertificate("");
    setUplaodRiskAssesment("");
    setRiskAssesmentDocument("");
    setUploadAgreementDocument("");
    setAgreementDocument("");
    setAgreementDocumentPreviewUrl("");
    setProcessorName("");
    setProcessorCallBackUrl("");
    setText("");
    setOriginalProcessorName("");
    setOriginalProcessorDetails("");
    setModalSelectedLanguage('en');
    
    setIsCrossBordered(false);
    
    // Clear SPOC form data
    setFormData({
      spoc: {
        name: "",
        mobile: "",
        email: "",
        emailMobile: "",
      },
      identityType: "",
    });
    
    // Clear form errors
    setFormErrors({
      spoc: {
        name: null,
        mobile: null,
        email: null,
      },
      emailMobile: null,
    });
    
    setIsSpocNameValid(true);
    
    // Clear file input refs
    if (certificateInputRef.current) {
      certificateInputRef.current.value = "";
    }
    if (riskAssessmentInputRef.current) {
      riskAssessmentInputRef.current.value = "";
    }
    if (agreementDocumentInputRef.current) {
      agreementDocumentInputRef.current.value = "";
    }
    
    // Clear preview URLs
    setCertificatePreviewUrl("");
    setRiskDocumentPreviewUrl("");
  };

  const closeUpdateProcessorModal = () => {
    fetchProcessor();
    setUpdateProcessorModal(false);
    setUplaodCertificate("");
    setUplaodRiskAssesment("");
    setRiskAssesmentDocument("");
    setProcessorName("");
    setProcessorCallBackUrl("");
    setText("");
  };

  const handleProcessorNameBlur = (e) => {
    let value = e.target.value;
    setProcessorName(value);
    // If typing in English, store as original
    if (modalSelectedLanguage === 'en') {
      setOriginalProcessorName(value);
    }
  };

  const handleProcessorCallBackUrlBlur = (e) => {
    let value = e.target.value;
    setProcessorCallBackUrl(value);
  };

  const handleProcessorDetailsChange = (value) => {
    // Don't update if we're currently translating (to prevent flickering)
    if (isTranslatingRef.current) {
      return;
    }
    
    // Always update the display value immediately for responsive typing
    setText(value || "");
    
    // If typing in English, store as original English value
    if (modalSelectedLanguage === 'en') {
      setOriginalProcessorDetails(value || "");
    }
  };

  // Translation function for input fields
  const translateInputFields = async (name, details, targetLanguage) => {
    // Set flag to prevent user input from interfering
    isTranslatingRef.current = true;
    setIsTranslating(true);
    
    try {
      const targetLang = targetLanguage || modalSelectedLanguage;
      
      if (targetLang === 'en') {
        // If English, use original values
        setProcessorName(originalProcessorName || name || "");
        setText(originalProcessorDetails || details || "");
        return;
      }

      // Use original English values for translation
      const nameToTranslate = originalProcessorName || name || "";
      const detailsToTranslate = originalProcessorDetails || details || "";

      const textsToTranslate = [];
      if (nameToTranslate && nameToTranslate.trim()) {
        textsToTranslate.push({ id: 'processor-name', source: nameToTranslate });
      }
      if (detailsToTranslate && detailsToTranslate.trim()) {
        textsToTranslate.push({ id: 'processor-details', source: detailsToTranslate });
      }

      if (textsToTranslate.length === 0) {
        // No text to translate, clear fields
        setProcessorName("");
        setText("");
        return;
      }

      // Always translate from English to target language
      const translationResult = await dispatch(getTranslations(textsToTranslate, targetLang, 'en'));
      
      if (translationResult && translationResult.output && Array.isArray(translationResult.output)) {
        const translatedName = translationResult.output.find(t => t.id === 'processor-name')?.target || nameToTranslate;
        const translatedDetails = translationResult.output.find(t => t.id === 'processor-details')?.target || detailsToTranslate;
        
        // Update all fields at once to prevent flickering
        setProcessorName(translatedName);
        setText(translatedDetails);
      }
    } catch (error) {
      console.error('Error translating input fields:', error);
      // On error, keep original values
      const nameToTranslate = originalProcessorName || name || "";
      const detailsToTranslate = originalProcessorDetails || details || "";
      setProcessorName(nameToTranslate);
      setText(detailsToTranslate);
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
      if (originalProcessorName || originalProcessorDetails) {
        setProcessorName(originalProcessorName || "");
        setText(originalProcessorDetails || "");
      } else if (previousLang !== 'en' && (processorName || text)) {
        // No originals stored but we have values, translate current values back to English
        setIsTranslating(true);
        try {
          const textsToTranslate = [];
          if (processorName && processorName.trim()) {
            textsToTranslate.push({ id: 'processor-name', source: processorName });
          }
          if (text && text.trim()) {
            textsToTranslate.push({ id: 'processor-details', source: text });
          }
          
          if (textsToTranslate.length > 0) {
            const translationResult = await dispatch(getTranslations(textsToTranslate, 'en', previousLang));
            
            if (translationResult && translationResult.output && Array.isArray(translationResult.output)) {
              const englishName = translationResult.output.find(t => t.id === 'processor-name')?.target || processorName;
              const englishDetails = translationResult.output.find(t => t.id === 'processor-details')?.target || text;
              
              setOriginalProcessorName(englishName);
              setOriginalProcessorDetails(englishDetails);
              
              setProcessorName(englishName);
              setText(englishDetails);
            }
          }
        } catch (error) {
          console.error('Error translating back to English:', error);
        } finally {
          setIsTranslating(false);
        }
      } else {
        // No values at all, clear fields
        setProcessorName("");
        setText("");
      }
    } 
    // If switching to non-English
    else {
      // First, ensure we have English originals
      // If we don't have originals, we need to get them first
      if (!originalProcessorName && !originalProcessorDetails) {
        // If previous language was English and we have values, store them as originals
        if (previousLang === 'en' && (processorName || text)) {
          setOriginalProcessorName(processorName || "");
          setOriginalProcessorDetails(text || "");
          // Now translate to the new language
          await translateInputFields(processorName, text, selectedLang);
        } 
        // If previous language was non-English, translate current values back to English first
        else if (previousLang !== 'en' && (processorName || text)) {
          setIsTranslating(true);
          try {
            const textsToTranslate = [];
            if (processorName && processorName.trim()) {
              textsToTranslate.push({ id: 'processor-name', source: processorName });
            }
            if (text && text.trim()) {
              textsToTranslate.push({ id: 'processor-details', source: text });
            }
            
            if (textsToTranslate.length > 0) {
              // Translate from previous language to English
              const translationResult = await dispatch(getTranslations(textsToTranslate, 'en', previousLang));
              
              if (translationResult && translationResult.output && Array.isArray(translationResult.output)) {
                const englishName = translationResult.output.find(t => t.id === 'processor-name')?.target || processorName;
                const englishDetails = translationResult.output.find(t => t.id === 'processor-details')?.target || text;
                
                setOriginalProcessorName(englishName);
                setOriginalProcessorDetails(englishDetails);
                
                // Now translate from English to the new selected language
                await translateInputFields(englishName, englishDetails, selectedLang);
              }
            }
          } catch (error) {
            console.error('Error translating to English first:', error);
            setIsTranslating(false);
          }
        } else {
          // No values at all, clear fields
          setProcessorName("");
          setText("");
        }
      } else {
        // We have English originals, translate to new language
        await translateInputFields(originalProcessorName, originalProcessorDetails, selectedLang);
      }
    }
  };

  const convertToBase64 = (file) => {
    const reader = new FileReader();
    reader.readAsDataURL(file); // reads file as base64
    reader.onload = () => {
      setDataProcessorCertificate(reader.result); // base64 string here
    };
    reader.onerror = (error) => {
      console.log("Error converting file:", error);
    };
  };

  const submitProcessor = async () => {
    let errors = [];

    // Use original English values for validation and submission
    const nameToValidate = modalSelectedLanguage === 'en' ? processorName : originalProcessorName;
    const detailsToValidate = modalSelectedLanguage === 'en' ? text : originalProcessorDetails;

    // Processor Name
    let value1 = nameToValidate.trimStart();
    value1 = value1.replace(/\s+/g, " ");
    const isValid1 = /^(?=.*[A-Za-z])[A-Za-z0-9\s&'().\-]+$/.test(value1);
    if (!isValid1 || value1.trim() === "") {
      errors.push("Please enter valid Processor name.");
    }
    //check for processor name duplicacy
    const duplicate = processorList.some(
      (item) =>
        item.dataProcessorName.toLowerCase() === nameToValidate.toLowerCase()
    );
    if (duplicate) {
      errors.push("Processor name already exists.");
    }

    let value2 = processorCallBackUrl.trim();
    if (value2 === "") {
      errors.push("Please enter valid Callback URL.");
    } else {
      try {
        new URL(value2);
      } catch {
        errors.push("Please enter valid Callback URL.");
      }
    }

    let value3 = detailsToValidate.trimStart();
    value3 = value3.replace(/\s+/g, " ");
    // const isValid3 = /^(?=.*[A-Za-z])[A-Za-z0-9\s]+$/.test(value3);
    if (value3.trim() === "") {
      errors.push("Please enter valid description.");
    }
    if (value3.length > 200) {
      errors.push("Description must not exceed 200 characters.");
    }

    // Certificate upload
    if (!uploadCertificate) {
      errors.push("Please upload a certificate.");
    }

    // Risk Assessment upload
    if (!uploadRiskAssesment) {
      errors.push("Please upload a risk assessment document.");
    }

    if (
      !formData.spoc.name ||
      formData.spoc.name.trim() === "" ||
      formData.spoc.name.length > 30
    ) {
      errors.push("Please enter a valid SPOC name.");
    }

    if (!formData.spoc.emailMobile || formData.spoc.emailMobile.trim() === "") {
      errors.push("Please enter a valid SPOC Email.");
    } else {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(formData.spoc.emailMobile)) {
        errors.push("Please enter a valid Email ID format for the SPOC.");
      }
    }

    const spocPayload = {
      name: formData.spoc.name,
      mobile: formData.identityType === "MOBILE" ? formData.spoc.mobile : "",
      email:
        formData.identityType === "EMAIL"
          ? formData.spoc.email.toUpperCase()
          : "",
    };

    const identityType = formData.identityType;

    console.log("SPOC Payload:", spocPayload);
    console.log("Identity Type:", identityType);

    // Show all errors if any
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
        // Get content type for certificate
        const getCertificateContentType = (filename) => {
          if (!filename) return "application/x-x509-ca-cert";
          const lowerName = filename.toLowerCase();
          if (lowerName.endsWith('.pem')) return "application/x-pem-file";
          if (lowerName.endsWith('.crt')) return "application/x-x509-ca-cert";
          if (lowerName.endsWith('.cer')) return "application/pkix-cert";
          return "application/x-x509-ca-cert";
        };

        const metaDataOfCertificate = {
          documentId: "certificate-doc",
          name: uploadCertificate || "certificate.pem",
          contentType: getCertificateContentType(uploadCertificate),
          size: uploadCertificateSize || 0,
          tag: {
            components: "TEMPLATE",
            documentTag: "VENDOR_RISK",
          },
        };
        const metaDataOfVendorRiskDocument = {
          documentId: "hjhj",
          name: uploadRiskAssesment,
          contentType: "application/pdf",
          size: uplaodriskAssesmentSize,
          tag: {
            components: "TEMPLATE",
            documentTag: "VENDOR_RISK",
          },
        };

        // Get content type for agreement document
        const getAgreementDocumentContentType = (filename) => {
          if (!filename) return "application/pdf";
          const lowerName = filename.toLowerCase();
          if (lowerName.endsWith('.pdf')) return "application/pdf";
          if (lowerName.endsWith('.pem')) return "application/x-pem-file";
          if (lowerName.endsWith('.crt')) return "application/x-x509-ca-cert";
          if (lowerName.endsWith('.cer')) return "application/pkix-cert";
          return "application/pdf";
        };

        const metaDataOfAgreementDocument = {
          documentId: "agreement-doc",
          name: uploadAgreementDocument || "agreement.pdf",
          contentType: getAgreementDocumentContentType(uploadAgreementDocument),
          size: uploadAgreementDocumentSize || 0,
          tag: {
            components: "TEMPLATE",
            documentTag: "VENDOR_RISK",
          },
        };

        // Submit with original English values (or current if English is selected)
        // Trim whitespace from start and end only (not between words)
        const nameToSubmit = (modalSelectedLanguage === 'en' ? processorName : originalProcessorName).trim();
        const detailsToSubmit = modalSelectedLanguage === 'en' ? text : originalProcessorDetails;
        
        let res = await dispatch(
          createProcessor(
            nameToSubmit,
            processorCallBackUrl,
            detailsToSubmit,
            dataProcessorCertificate,
            riskAssesmentDocument,
            metaDataOfCertificate,
            metaDataOfVendorRiskDocument,
            spocPayload,
            identityType,
            isCrossBordered,
            agreementDocument,
            metaDataOfAgreementDocument
          )
        );
        if (res.status == 200 || res.status == 201) {
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"Data processor added successfully."}
              />
            ),
            { icon: false }
          );
          fetchProcessor();
          setDataProcessorModal(false);
          setProcessorName("");
          setProcessorCallBackUrl("");
          setText("");
          setOriginalProcessorName("");
          setOriginalProcessorDetails("");
          setModalSelectedLanguage('en');
          setDataProcessorCertificate("");
          setRiskAssesmentDocument("");
          setAgreementDocument("");
          setFormData((prev) => ({
            ...prev,
            spoc: {
              ...prev.spoc,
              name: "",
              emailMobile: "",
              mobile: "",
            },
          }));
          certificateInputRef.current.value = "";
          riskAssessmentInputRef.current.value = "";
          agreementDocumentInputRef.current.value = "";
          setUplaodCertificate("");
          setCertificatePreviewUrl("");
          setUplaodRiskAssesment("");
          setUploadAgreementDocument("");
          setAgreementDocumentPreviewUrl("");
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
                message={"Error occured while creating purpose."}
              />
            ),
            { icon: false }
          );
        }
      }
    }
  };

  const certificateInputRef = useRef(null);
  const riskAssessmentInputRef = useRef(null);
  const agreementDocumentInputRef = useRef(null);
  const [uploadRiskAssesment, setUplaodRiskAssesment] = useState("");
  const [uplaodriskAssesmentSize, setUplaodRiskAssesmentSize] = useState(null);
  const convertRiskAssesmentToBase64 = (file) => {
    const reader2 = new FileReader();
    reader2.readAsDataURL(file); // reads file as base64
    reader2.onload = () => {
      setRiskAssesmentDocument(reader2.result); // base64 string here
    };
    reader2.onerror = (error) => {
      console.log("Error converting file:", error);
    };
  };
  const handleRiskAssesment = async (e) => {
    if (e.target.files.length > 0) {
      const file = e.target.files[0];
      
      // ✅ Async validation with malicious content scanning
      const validationResult = await validatePDFFile(file);
      
      if (validationResult.valid) {
        setUplaodRiskAssesment(file.name);
        const RiskAssesmentfileSizeInKB = file.size;
        setUplaodRiskAssesmentSize(RiskAssesmentfileSizeInKB);
        convertRiskAssesmentToBase64(file);
        setRiskDocumentPreviewUrl(URL.createObjectURL(file));
      } else {
        e.target.value = ""; // clear invalid file
        setUplaodRiskAssesment("");
        setRiskDocumentPreviewUrl("");
      }
    }
  };

  const handleRiskAssesmentClick = () => {
    riskAssessmentInputRef.current.click();
  };

  const handleRemoveRiskAssesment = () => {
    if (riskAssessmentInputRef.current) {
      riskAssessmentInputRef.current.value = "";
    }
    setUplaodRiskAssesment("");
  };

  const handleRiskAssesmentDragLeave = (e) => {
    e.preventDefault();
    e.currentTarget.classList.remove("dragover");
  };

  const handleRiskAssesmentDragOver = (e) => {
    e.preventDefault();
    e.currentTarget.classList.add("dragover");
  };

  const handleRiskAssesmentDrop = async (e) => {
    e.preventDefault();
    e.currentTarget.classList.remove("dragover");

    if (e.dataTransfer.files.length > 0) {
      const file = e.dataTransfer.files[0];
      
      // ✅ Async validation for drag & drop
      const validationResult = await validatePDFFile(file);
      
      if (validationResult.valid) {
        riskAssessmentInputRef.current.files = e.dataTransfer.files;
        setUplaodRiskAssesment(file.name);
        const RiskAssesmentfileSizeInKB = file.size;
        setUplaodRiskAssesmentSize(RiskAssesmentfileSizeInKB);
        convertRiskAssesmentToBase64(file);
        setRiskDocumentPreviewUrl(URL.createObjectURL(file));
      } else {
        riskAssessmentInputRef.current.value = "";
        setUplaodRiskAssesment("");
        setRiskDocumentPreviewUrl("");
      }
    }
  };

  const handleCertificateClick = () => {
    certificateInputRef.current.click();
  };

  const handleCertificateDragOver = (e) => {
    e.preventDefault();
    e.currentTarget.classList.add("dragover");
  };

  const handleCertificateDragLeave = (e) => {
    e.preventDefault();
    e.currentTarget.classList.remove("dragover");
  };

  const handleCertificateDrop = async (e) => {
    e.preventDefault();
    e.currentTarget.classList.remove("dragover");

    if (e.dataTransfer.files.length > 0) {
      const file = e.dataTransfer.files[0];
      
      // ✅ Async validation for drag & drop
      const validationResult = await validateCertificateFile(file);
      
      if (validationResult.valid) {
        certificateInputRef.current.files = e.dataTransfer.files;
        setUplaodCertificate(file.name);
        const CertificatefileSizeInKB = file.size;
        setUplaodCertificateSize(CertificatefileSizeInKB);
        convertToBase64(file);
        setCertificatePreviewUrl(URL.createObjectURL(file));
      } else {
        certificateInputRef.current.value = "";
        setUplaodCertificate("");
        setCertificatePreviewUrl("");
      }
    }
  };

  // ✅ OLD INSECURE VALIDATION FUNCTIONS REMOVED
  // Now using secure validation from '../utils/fileValidation'
  // which includes magic number verification, XSS detection, and malicious content scanning

  // ✅ SECURE: Agreement document validation
  // Agreement documents should be PDFs, use validateDocumentFile
  const validateAgreementDocument = async (file) => {
    return await validateDocumentFile(file);
  };

  // ✅ OLD VALIDATION LOGIC REMOVED - using secure validation from fileValidation.js

  const convertAgreementDocumentToBase64 = (file) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => {
      setAgreementDocument(reader.result);
    };
    reader.onerror = (error) => {
      console.log("Error converting agreement document:", error);
    };
  };

  const handleAgreementDocumentClick = () => {
    agreementDocumentInputRef.current.click();
  };

  const handleAgreementDocumentChange = async (e) => {
    if (e.target.files.length > 0) {
      const file = e.target.files[0];
      
      // ✅ Async validation (PDF or Certificate)
      const validationResult = await validateAgreementDocument(file);
      
      if (validationResult.valid) {
        setUploadAgreementDocument(file.name);
        const fileSizeInKB = file.size;
        setUploadAgreementDocumentSize(fileSizeInKB);
        convertAgreementDocumentToBase64(file);
        setAgreementDocumentPreviewUrl(URL.createObjectURL(file));
      } else {
        e.target.value = "";
        setUploadAgreementDocument("");
        setAgreementDocumentPreviewUrl("");
      }
    }
  };

  const handleAgreementDocumentDragOver = (e) => {
    e.preventDefault();
    e.currentTarget.classList.add("dragover");
  };

  const handleAgreementDocumentDragLeave = (e) => {
    e.preventDefault();
    e.currentTarget.classList.remove("dragover");
  };

  const handleAgreementDocumentDrop = async (e) => {
    e.preventDefault();
    e.currentTarget.classList.remove("dragover");

    if (e.dataTransfer.files.length > 0) {
      const file = e.dataTransfer.files[0];
      
      // ✅ Async validation for drag & drop
      const validationResult = await validateAgreementDocument(file);
      
      if (validationResult.valid) {
        agreementDocumentInputRef.current.files = e.dataTransfer.files;
        setUploadAgreementDocument(file.name);
        const fileSizeInKB = file.size;
        setUploadAgreementDocumentSize(fileSizeInKB);
        convertAgreementDocumentToBase64(file);
        setAgreementDocumentPreviewUrl(URL.createObjectURL(file));
      } else {
        agreementDocumentInputRef.current.value = "";
        setUploadAgreementDocument("");
        setAgreementDocumentPreviewUrl("");
      }
    }
  };

  const handleRemoveAgreementDocument = () => {
    if (agreementDocumentInputRef.current) {
      agreementDocumentInputRef.current.value = "";
    }
    setUploadAgreementDocument("");
    setAgreementDocument("");
    setAgreementDocumentPreviewUrl("");
    setUploadAgreementDocumentSize(null);
  };

  const handlePreviewAgreementDocument = () => {
    if (agreementDocumentPreviewUrl) {
      window.open(agreementDocumentPreviewUrl, "_blank");
    }
  };

  const handleCertificateChange = async (e) => {
    if (e.target.files.length > 0) {
      const file = e.target.files[0];
      
      // ✅ Async validation with magic number verification
      const validationResult = await validateCertificateFile(file);
      
      if (validationResult.valid) {
        setUplaodCertificate(file.name);
        const CertificatefileSizeInKB = file.size;
        setUplaodCertificateSize(CertificatefileSizeInKB);
        convertToBase64(file);
        setCertificatePreviewUrl(URL.createObjectURL(file));
      } else {
        e.target.value = ""; // clear invalid file
        setUplaodCertificate("");
        setCertificatePreviewUrl("");
      }
    }
  };

  const handleRemoveCertificate = () => {
    if (certificateInputRef.current) {
    certificateInputRef.current.value = "";
}
    setUplaodCertificate("");
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
      <div>
        {/* <div className="custom-divider"></div> */}
        <div className="outer-modal-add-btn">
          <div
            className="outer-modal-add-btn-inner-container"
            onClick={openDataProcessorModal}
          >
            <Text appearance="body-s-bold" color="primary-60">
              Add Data Processor
            </Text>
          </div>
        </div>
        <div className="custom-table-outer-div">
          <table className="custom-table">
            <thead>
              <tr>
                <th>
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    Processor name
                  </Text>
                </th>
                <th>
                  {" "}
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    Call back URL
                  </Text>
                </th>
                <th>
                  {" "}
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    Details
                  </Text>
                </th>
                <th>
                  {" "}
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    Action
                  </Text>
                </th>
              </tr>
            </thead>
            <tbody>
              {loadProcessor ? (
                <tr>
                  <td colSpan="3">
                    <div className="customerActivityLoader">
                      <Spinner
                        kind="normal"
                        label=""
                        labelPosition="right"
                        size="small"
                      />
                    </div>
                  </td>
                </tr>
              ) : processorList.length > 0 ? (
                processorList.map((item) => (
                  <tr key={item.dataProcessorId}>
                    <td>
                      {" "}
                      <Text appearance="body-xs-bold" color="black">
                        {item.dataProcessorName}
                      </Text>
                    </td>
                    <td>
                      {" "}
                      <Text appearance="body-xs-bold" color="black">
                        {item.callbackUrl || systemCallbackUrl}
                      </Text>
                    </td>
                    <td>
                      {" "}
                      <Text appearance="body-xs-bold" color="black">
                        {item.details}
                      </Text>
                    </td>
                    <td>
                      <div style={{ display: "flex", gap: "10px" }}>
                        <Icon
                          ic={<IcEditPen height={24} width={24} />}
                          color="primary_grey_80"
                          kind="default"
                          size="medium"
                          onClick={() => handleEditProcessor(item)}
                          style={{ cursor: "pointer" }}
                        />
                        <Icon
                          ic={<IcDocumentViewer height={24} width={24} />}
                          color="primary_grey_80"
                          kind="default"
                          size="medium"
                          onClick={() => handleViewProcessor(item)}
                          style={{ cursor: "pointer" }}
                        />
                        {/* <ActionButton
                          kind="tertiary"
                          size="small"
                          label="View"
                          onClick={() => handleViewProcessor(item)}
                        /> */}
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="3">No Processor found</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        {/* <div className="common-add-btn">
          <ActionButton label="Save" kind="primary"></ActionButton>
        </div> */}
      </div>
      {dataProcessorModal && (
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
                onClick={closeDataProcessorModal}
                icon={<IcClose />}
                kind="tertiary"
              ></ActionButton>
            </div>{" "}
            <Text appearance="heading-xs" color="primary-grey-100">
              {" "}
              Add processor{" "}
            </Text>{" "}
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
              label="Processor name (Required)"
              value={processorName}
              onChange={(e) => {
                const value = e.target.value;
                setProcessorName(value);
              }}
              onBlur={handleProcessorNameBlur}
            ></InputFieldV2>{" "}
            <br></br>
            <InputFieldV2
              label="Call back URL (Required)"
              value={processorCallBackUrl}
              onChange={(e) => setProcessorCallBackUrl(e.target.value)}
            ></InputFieldV2>{" "}
            <br></br>
            <InputFieldV2
              label="SPOC Name (Required)"
              onChange={handleSpocNameChange}
              value={formData.spoc.name}
              state={formErrors.spoc.name ? "error" : "none"}
              stateText={formErrors.spoc.name || ""}
            ></InputFieldV2>{" "}
            <br></br>
            <InputFieldV2
              label="Email (Required)"
              onChange={handleEmailMobileChange}
              value={formData.spoc.emailMobile || ""}
              state={
                formErrors.emailMobile === null
                  ? "success"
                  : formErrors.emailMobile
                  ? "error"
                  : "none"
              }
              stateText={formErrors.emailMobile || ""}
            ></InputFieldV2>{" "}
            <br></br>
            <WordLimitedTextarea value={text || ""} onTextChange={handleProcessorDetailsChange} />
            <br></br>
            <div style={{ marginBottom: "16px" }}>
              <InputCheckbox
                label="Is Cross Border"
                checked={isCrossBordered}
                onClick={() => setIsCrossBordered((prev) => !prev)}
                
              />
            </div>
            <br></br>
            <div>
              <div style={{display:'flex', flexDirection:'row'}}>
              <Text appearance="body-xs" color="primary-grey-80">
                Upload Certificate
              </Text>
              <div style={{marginLeft:'5px'}} title="Only certificate files (.crt, .pem, .cer) are allowed. Maximum file size: 500KB.">
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
                onClick={handleCertificateClick}
                onDragOver={handleCertificateDragOver}
                onDragLeave={handleCertificateDragLeave}
                onDrop={handleCertificateDrop}
              >
                <input
                  type="file"
                  ref={certificateInputRef}
                  style={{ display: "none" }}
                  accept=".crt,.pem,.cer"
                  onChange={handleCertificateChange}
                />

                <div className="flex items-center justify-center">
                  <Icon
                    ic={<IcUpload height={23} width={23} />}
                    color="primary_60"
                  />
                  <Text appearance="button" color="primary-60">
                    Upload Certificate
                  </Text>
                </div>
              </div>

              <Text appearance="body-xs" color="primary-grey-80">
                Drag and drop certificate (PEM, CRT, or CER) under 500KB
              </Text>

              {uploadCertificate && (
                <div className="master-file-uploader">
                  <div
                    onClick={handlePreviewCertificte}
                    className="previewFile"
                  >
                    <Icon
                      ic={<IcSuccess width={15} height={15} />}
                      color="feedback_success_50"
                    />
                    <Text appearance="body-xs" color="primary-grey-80">
                      {uploadCertificate}
                    </Text>
                  </div>
                  <Icon
                    ic={<IcClose width={23} height={23} />}
                    color="primary_60"
                    className="selecetd-file-display"
                    onClick={handleRemoveCertificate}
                  />
                </div>
              )}
            </div>
            <br></br>
            <div>
              <div style={{display:'flex', flexDirection:'row'}}>
              <Text appearance="body-xs" color="primary-grey-80">
                Upload Risk Assessment
              </Text>
              <div style={{marginLeft:'5px'}} title="Only PDF files are allowed. Maximum file size: 500KB.">
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
                onClick={handleRiskAssesmentClick}
                onDragOver={handleRiskAssesmentDragOver}
                onDragLeave={handleRiskAssesmentDragLeave}
                onDrop={handleRiskAssesmentDrop}
              >
                <input
                  type="file"
                  ref={riskAssessmentInputRef}
                  style={{ display: "none" }}
                  accept="application/pdf"
                  onChange={handleRiskAssesment}
                />

                <div className="flex items-center justify-center">
                  <Icon
                    ic={<IcUpload height={23} width={23} />}
                    color="primary_60"
                  />
                  <Text appearance="button" color="primary-60">
                    Upload Assesment
                  </Text>
                </div>
              </div>

              <Text appearance="body-xs" color="primary-grey-80">
                Drag and drop risk assessment (PDF) under 500KB
              </Text>

              {uploadRiskAssesment && (
                <div className="master-file-uploader">
                  <div
                    onClick={handlePreviewriskDocument}
                    className="previewFile"
                  >
                    <Icon
                      ic={<IcSuccess width={15} height={15} />}
                      color="feedback_success_50"
                    />
                    <Text appearance="body-xs" color="primary-grey-80">
                      {uploadRiskAssesment}
                    </Text>
                  </div>
                  <Icon
                    ic={<IcClose width={23} height={23} />}
                    color="primary_60"
                    className="selecetd-file-display"
                    onClick={handleRemoveRiskAssesment}
                  />
                </div>
              )}
            </div>
            <br></br>
            <div>
              <div style={{display:'flex', flexDirection:'row'}}>
              <Text appearance="body-xs" color="primary-grey-80">
                Upload Agreement Document
              </Text>
              <div style={{marginLeft:'5px'}} title="Only PDF files are allowed. Maximum file size: 500KB.">
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
                onClick={handleAgreementDocumentClick}
                onDragOver={handleAgreementDocumentDragOver}
                onDragLeave={handleAgreementDocumentDragLeave}
                onDrop={handleAgreementDocumentDrop}
              >
                <input
                  type="file"
                  ref={agreementDocumentInputRef}
                  style={{ display: "none" }}
                  accept=".pdf,application/pdf"
                  onChange={handleAgreementDocumentChange}
                />

                <div className="flex items-center justify-center">
                  <Icon
                    ic={<IcUpload height={23} width={23} />}
                    color="primary_60"
                  />
                  <Text appearance="button" color="primary-60">
                    Upload Agreement Document
                  </Text>
                </div>
              </div>

              <Text appearance="body-xs" color="primary-grey-80">
                Drag and drop agreement document (PDF) under 500KB
              </Text>

              {uploadAgreementDocument && (
                <div className="master-file-uploader">
                  <div
                    onClick={handlePreviewAgreementDocument}
                    className="previewFile"
                  >
                    <Icon
                      ic={<IcSuccess width={15} height={15} />}
                      color="feedback_success_50"
                    />
                    <Text appearance="body-xs" color="primary-grey-80">
                      {uploadAgreementDocument}
                    </Text>
                  </div>
                  <Icon
                    ic={<IcClose width={23} height={23} />}
                    color="primary_60"
                    className="selecetd-file-display"
                    onClick={handleRemoveAgreementDocument}
                  />
                </div>
              )}
            </div>
            <div className="modal-add-btn-container">
              {" "}
              <ActionButton
                label="Add"
                onClick={submitProcessor}
              ></ActionButton>{" "}
            </div>{" "}
          </div>{" "}
        </div>
      )}

      {updateProcessorModal && (
        <DataProcessorUpdateModal
          processor={selectedProcessor}
          onClose={closeUpdateProcessorModal}
          systemCallbackUrl={systemCallbackUrl}
        />
      )}

      {/* View Processor Modal */}
      {viewProcessorModal && selectedProcessor && (
        <div className="modal-outer-container">
          <div
            className="master-set-up-modal-container"
            style={{ maxWidth: "600px", width: "90%" }}
          >
            <div className="modal-close-btn-container">
              <ActionButton
                onClick={() => setViewProcessorModal(false)}
                icon={<IcClose />}
                kind="tertiary"
              />
            </div>
            <Text appearance="heading-xs" color="primary-grey-100">
              Processor Details
            </Text>

            <div
              style={{
                marginTop: "20px",
                display: "flex",
                flexDirection: "column",
                gap: "15px",
              }}
            >
              <div>
                <Text appearance="body-s-bold" color="primary-grey-100">
                  Processor Name:{" "}
                </Text>
                <Text
                  appearance="body-s"
                  color="primary-grey-80"
                  style={{ marginTop: "5px" }}
                >
                  {selectedProcessor.dataProcessorName}
                </Text>
              </div>

              <div>
                <Text appearance="body-s-bold" color="primary-grey-100">
                  Callback URL:{" "}
                </Text>
                <Text
                  appearance="body-s"
                  color="primary-grey-80"
                  style={{ marginTop: "5px" }}
                >
                  {selectedProcessor.callbackUrl || systemCallbackUrl}
                </Text>
              </div>

              <div>
                <Text appearance="body-s-bold" color="primary-grey-100">
                  Attachments:{" "}
                </Text>
                <div
                  style={{
                    marginTop: "10px",
                    display: "flex",
                    flexDirection: "column",
                    gap: "10px",
                  }}
                >
               
                  {selectedProcessor.vendorRiskDocument && (
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "10px",
                      }}
                    >
                      <Icon
                        ic={<IcAttachment height={20} width={20} />}
                        color="primary_grey_80"
                        kind="default"
                        size="medium"
                      />
                      <Text appearance="body-s" color="primary-grey-80">
                        Risk Assessment
                      </Text>
                      <ActionButton
                        kind="tertiary"
                        size="small"
                        label="View"
                        onClick={() => {
                          if (selectedProcessor.vendorRiskDocument) {
                            const newWindow = window.open();
                            if (!newWindow) {
                              alert("Please allow pop-ups to view documents");
                              return;
                            }
                            newWindow.document.write(`
                              <html>
                                <body style="margin: 0; height: 100vh;">
                                  <iframe src="${selectedProcessor.vendorRiskDocument}" width="100%" height="100%" style="border: none;"></iframe>
                                </body>
                              </html>
                            `);
                          }
                        }}
                      />
                    </div>
                  )}

                  {selectedProcessor.attachment && (
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "10px",
                      }}
                    >
                      <Icon
                        ic={<IcAttachment height={20} width={20} />}
                        color="primary_grey_80"
                        kind="default"
                        size="medium"
                      />
                      <Text appearance="body-s" color="primary-grey-80">
                        Agreement of the Processor
                      </Text>
                      <ActionButton
                        kind="tertiary"
                        size="small"
                        label="View"
                        onClick={() => {
                          if (selectedProcessor.attachment) {
                            const newWindow = window.open();
                            if (!newWindow) {
                              alert("Please allow pop-ups to view documents");
                              return;
                            }
                            newWindow.document.write(`
                              <html>
                                <body style="margin: 0; height: 100vh;">
                                  <iframe src="${selectedProcessor.attachment}" width="100%" height="100%" style="border: none;"></iframe>
                                </body>
                              </html>
                            `);
                          }
                        }}
                      />
                    </div>
                  )}
                  {selectedProcessor.certificate && (
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "10px",
                      }}
                    >
                      <Icon
                        ic={<IcAttachment height={20} width={20} />}
                        color="primary_grey_80"
                        kind="default"
                        size="medium"
                      />
                      <Text appearance="body-s" color="primary-grey-80">
                        Certificate of the Processor
                      </Text>
                      <ActionButton
                        kind="tertiary"
                        size="small"
                        label="View"
                        onClick={() => {
                          if (selectedProcessor.certificate) {
                            const newWindow = window.open();
                            if (!newWindow) {
                              alert("Please allow pop-ups to view documents");
                              return;
                            }
                            newWindow.document.write(`
                              <html>
                                <body style="margin: 0; height: 100vh;">
                                  <iframe src="${selectedProcessor.certificate}" width="100%" height="100%" style="border: none;"></iframe>
                                </body>
                              </html>
                            `);
                          }
                        }}
                      />
                    </div>
                  )}

                  {!selectedProcessor.certificate &&
                    !selectedProcessor.attachment &&
                    !selectedProcessor.vendorRiskDocument && (
                      <Text appearance="body-s" color="primary-grey-60">
                        No attachments available
                      </Text>
                    )}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

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
export default DataProcessor;
