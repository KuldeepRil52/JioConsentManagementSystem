import "../styles/masterSetup.css";
import "../styles/PreviewFile.css";
import { useState, useEffect, useMemo } from "react";
import { useRef } from "react";
import { Text, InputFieldV2, ActionButton, Icon, Spinner } from "../custom-components";
import {
  IcClose,
  IcEditPen,
  IcSuccess,
  IcUpload,
  IcWarningColored,
} from "../custom-components/Icon";
import { Text, InputFieldV2, ActionButton, Icon, Spinner, InputCheckbox } from "../custom-components";
import {
  IcClose,
  IcEditPen,
  IcSuccess,
  IcUpload,
  IcWarningColored,
} from "../custom-components/Icon";

import { useDispatch, useSelector } from "react-redux";
import { CLEAR_SESSION } from "../store/constants/Constants";
import "../styles/sessionModal.css";
import { getProcessor, updateProcessor, getTranslations } from "../store/actions/CommonAction";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";

import WordLimitedTextarea from "./wordLimitedtext";
import { useNavigate } from "react-router-dom";

import { decodeBase64 } from "../utils/base64";

import { getTranslations } from "../utils/translationApi";
import { IcChevronRight, IcChevronLeft } from '../custom-components/Icon';

import { languages } from "../utils/languages";
import { 
  validatePDFFile as validatePDFSecurity, 
  validateCertificateFile as validateCertificateSecurity,
  formatValidationErrors 
} from "../utils/fileValidation";

const DataProcessorUpdateModal = ({ processor, onClose, systemCallbackUrl }) => {

  const dispatch = useDispatch();
  const navigate = useNavigate();
  const multilingualConfigSaved = useSelector((state) => state.common.multilingualConfigSaved);
  const [showSessionModal, setShowSessionModal] = useState(false);
  const [showMultilingualWarningModal, setShowMultilingualWarningModal] = useState(false);
  const [processorList, setProcessorList] = useState([]);
  const [certificatePreviewUrl, setCertificatePreviewUrl] = useState('');
  const [riskDocumentPreviewUrl, setRiskDocumentPreviewUrl] = useState('');
  const [agreementDocumentPreviewUrl, setAgreementDocumentPreviewUrl] = useState('');
  const [uplaodCertificateSize, setUplaodCertificateSize] = useState(null);

  // Language selection for update processor modal
  const [activeLanguage, setActiveLanguage] = useState('en');
  // Store original English values for translation
  const [originalProcessorName, setOriginalProcessorName] = useState("");
  const [originalProcessorDetails, setOriginalProcessorDetails] = useState("");
  const [originalSpocName, setOriginalSpocName] = useState("");
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

  const handlePreviewAgreementDocument = () => {
    if (agreementDocumentPreviewUrl) {
      window.open(agreementDocumentPreviewUrl, "_blank");
    }
  };
  const fetchProcessor = async () => {
    try {
      let res = await dispatch(getProcessor());
      if (res?.status === 200 || res?.status === 201) {
        if (res?.data?.searchList) {
          setProcessorList(res.data.searchList);
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
      console.log("Error fetching processors:", err);
    }
  };
  
  useEffect(() => {
    fetchProcessor();
  }, [dispatch]);

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

  const submitUpdateProcessor = async () => {
    let errors = [];

    console.log("Starting update processor validation...");
    console.log("Processor Name:", processorName);
    console.log("Callback URL:", processorCallBackUrl);
    console.log("Description:", selectedProcessor);

    // Use original English values for validation and submission
    const nameToValidate = activeLanguage === 'en' ? processorName : originalProcessorName;
    const detailsToValidate = activeLanguage === 'en' ? selectedProcessor : originalProcessorDetails;
    const spocNameToValidate = activeLanguage === 'en' ? spocName : originalSpocName;

    // Processor Name
    let value1 = (nameToValidate || "").trimStart();
    value1 = value1.replace(/\s+/g, " ");
    const isValid1 = /^(?=.*[A-Za-z])[A-Za-z0-9\s&'().\-]+$/.test(value1);
    if (!isValid1 || value1.trim() === "") {
      errors.push("Please enter valid Processor name.");
      console.log("Processor name validation failed");
    }
    //check for processor name duplicacy (exclude current processor)
    const duplicate = processorList.some(
      (item) =>
        item.dataProcessorName.toLowerCase() === nameToValidate.toLowerCase() &&
        item.dataProcessorId !== processor.dataProcessorId
    );
    if (duplicate) {
      errors.push("Processor name already exists.");
    }
    // Callback URL (no spaces allowed)
    const urlRegex = /^(https?:\/\/(?:www\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\.[^\s]{2,}|www\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\.[^\s]{2,}|https?:\/\/[a-zA-Z0-9]+\.[^\s]{2,}|[a-zA-Z0-9]+\.[^\s]{2,})$/i;

    let value2 = (processorCallBackUrl || "").trim(); // Trim all spaces
    if (value2 === "") {
      errors.push("Please enter valid Callback URL.");
    } else if (!urlRegex.test(value2)) {
      errors.push("Please enter valid Callback URL (e.g., https://example.com).");
    }

    // Text area description
    let value3 = (detailsToValidate || "").trimStart();
    value3 = value3.replace(/\s+/g, " ");
    const isValid3 = /^(?=.*[A-Za-z]).+$/.test(value3);
    if (!isValid3 || value3.trim() === "") {
      errors.push("Please enter valid description");
    }

    // SPOC Name validation
    if (!spocNameToValidate || spocNameToValidate.trim() === "" || spocNameToValidate.length > 30) {
      errors.push("Please enter a valid SPOC name (max 30 characters).");
    }

    // SPOC Email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!spocEmail || !emailRegex.test(spocEmail)) {
      errors.push("Please enter a valid SPOC Email.");
    }

    // Certificate upload (optional if already exists)
    if (!uploadCertificate && !processor.certificate && !processor.attachment) {
      errors.push("Please upload a certificate.");
    }

    // Risk Assessment upload (optional if already exists, not required for "own use")
    if (!isOwnUseProcessor && !uploadRiskAssesment && !processor.vendorRiskDocument) {
      errors.push("Please upload a risk assessment document.");
    }

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
        console.log("All validations passed, preparing API call...");
        console.log("Processor ID:", processorId);
        console.log("Upload Certificate:", uploadCertificate);
        console.log("Upload Risk Assessment:", uploadRiskAssesment);
        
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
          documentId: processor.certificateMeta?.documentId || "certificate-doc",
          name: uploadCertificate || processor.certificateMeta?.name || "certificate.pem",
          contentType: getCertificateContentType(uploadCertificate || processor.certificateMeta?.name),
          size: uplaodCertificateSize || processor.certificateMeta?.size || 0,
          tag: {
            components: "TEMPLATE",
            documentTag: "VENDOR_RISK",
          },
        };
        // For "own use" processor, use dummy PDFs if not uploaded
        const finalRiskAssessmentDoc = isOwnUseProcessor && !uploadRiskAssesment && !processor.vendorRiskDocument 
          ? DUMMY_RISK_ASSESSMENT_PDF 
          : (riskAssesmentDocument || processor.vendorRiskDocument);
        
        const finalRiskAssessmentName = isOwnUseProcessor && !uploadRiskAssesment && !processor.vendorRiskDocumentMeta?.name
          ? "own-use-risk-assessment.pdf"
          : (uploadRiskAssesment || processor.vendorRiskDocumentMeta?.name || "");
        
        const finalRiskAssessmentSize = isOwnUseProcessor && !uploadRiskAssesment && !processor.vendorRiskDocumentMeta?.size
          ? 390  // Size of dummy PDF
          : (uplaodriskAssesmentSize || processor.vendorRiskDocumentMeta?.size || 0);

        const metaDataOfVendorRiskDocument = {
          documentId: processor.vendorRiskDocumentMeta?.documentId || "",
          name: finalRiskAssessmentName,
          contentType: "application/pdf",
          size: finalRiskAssessmentSize,
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

        // For "own use" processor, use dummy PDF if not uploaded
        const finalAgreementDoc = isOwnUseProcessor && !uploadAgreementDocument && !processor?.attachment && !processor?.agreementDocument
          ? DUMMY_AGREEMENT_PDF
          : (agreementDocument || processor?.attachment || processor?.agreementDocument);
        
        const finalAgreementName = isOwnUseProcessor && !uploadAgreementDocument && !processor?.attachmentMeta?.name && !processor?.agreementDocumentMeta?.name
          ? "own-use-agreement.pdf"
          : (uploadAgreementDocument || processor?.attachmentMeta?.name || processor?.agreementDocumentMeta?.name || "agreement.pdf");
        
        const finalAgreementSize = isOwnUseProcessor && !uploadAgreementDocument && processor?.attachmentMeta?.size == null && processor?.agreementDocumentMeta?.size == null
          ? 390  // Size of dummy PDF
          : (uploadAgreementDocumentSize ?? processor?.attachmentMeta?.size ?? processor?.agreementDocumentMeta?.size ?? 0);

        const metaDataOfAgreementDocument = {
          documentId: processor?.attachmentMeta?.documentId || processor?.agreementDocumentMeta?.documentId || "agreement-doc",
          name: finalAgreementName,
          contentType: getAgreementDocumentContentType(finalAgreementName),
          size: finalAgreementSize,
          tag: {
            components: "TEMPLATE",
            documentTag: "VENDOR_RISK",
          },
        };
        
        console.log("Metadata of Certificate:", metaDataOfCertificate);
        console.log("Metadata of Vendor Risk:", metaDataOfVendorRiskDocument);
        console.log("Metadata of Agreement Document:", metaDataOfAgreementDocument);
        console.log("Processor Name:", processorName);
        console.log("Callback URL:", processorCallBackUrl);
        console.log("Description:", selectedProcessor);
        console.log("SPOC Name:", spocName);
        console.log("SPOC Email:", spocEmail);
        console.log("Has Certificate:", !!dataProcessorCertificate, "or using existing:", !!(processor.certificate || processor.attachment));
        console.log("Is Own Use Processor:", isOwnUseProcessor);
        console.log("Using Risk Assessment:", finalRiskAssessmentDoc ? "Provided" : "None");
        console.log("Using Agreement Document:", finalAgreementDoc ? "Provided" : "None");
        console.log("Processor ID:", processorId);
        console.log("Certificate from API:", processor.certificate ? "YES" : "NO");
        console.log("Certificate Meta from API:", processor.certificateMeta ? processor.certificateMeta : "NO");
        
        // Submit with original English values (or current if English is selected)
        // Trim whitespace from start and end only (not between words)
        const nameToSubmit = (activeLanguage === 'en' ? processorName : originalProcessorName).trim();
        const detailsToSubmit = activeLanguage === 'en' ? selectedProcessor : originalProcessorDetails;
        const spocNameToSubmit = activeLanguage === 'en' ? spocName : originalSpocName;
        
        const spocPayload = {
          name: spocNameToSubmit,
          mobile: "",
          email: spocEmail.toUpperCase(),
        };
        
        let res = await dispatch(
          updateProcessor(
            nameToSubmit,
            processorCallBackUrl,
            detailsToSubmit,
            dataProcessorCertificate || processor.certificate || processor.attachment,
            finalRiskAssessmentDoc,  // Use dummy PDF for "own use" if needed
            processorId,
            metaDataOfCertificate,
            metaDataOfVendorRiskDocument,
            spocPayload,
            "EMAIL",
            isCrossBordered,
            finalAgreementDoc,  // Use dummy PDF for "own use" if needed
            metaDataOfAgreementDocument
          )
        );
        
        console.log("Update processor API response:", res);
        if (res.status == 200 || res.status == 201) {
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"Data processor updated successfully."}
              />
            ),
            { icon: false }
          );
          // Close modal after a short delay to ensure toast is visible
          setTimeout(() => {
            onClose();
          }, 500);
        }
      } catch (err) {
        console.error("Full error object:", err);
        console.error("Error array:", Array.isArray(err) ? err : [err]);
        
        if (err[0]?.errorCd == "JCMP4003" || err[0]?.errorCd == "JCMP4001") {
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
          console.error("Error updating processor:", err);
          const errorMessage = err[0]?.errorMsg || err?.message || "Error occurred while updating processor.";
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={errorMessage}
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
      
      if (validationResult.valid) {  // ✅ FIX: Check validationResult.valid, not the object itself!
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
    setRiskAssesmentDocument("");
    setRiskDocumentPreviewUrl("");
    setUplaodRiskAssesmentSize(null);
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
      
      if (validationResult.valid) {  // ✅ FIX: Check validationResult.valid
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
  const [processorName, setProcessorName] = useState(
    processor?.dataProcessorName || ""
  );
  const [processorId, setprocessorId] = useState(
    processor?.dataProcessorId || ""
  );
  
  // Check if this is "own use" processor - disable Risk Assessment and Agreement uploads
  const isOwnUseProcessor = processor?.description?.toLowerCase().includes('own use') || 
                           processor?.details?.toLowerCase().includes('own use') ||
                           processor?.dataProcessorName?.toLowerCase().includes('own use');
  
  // Hardcoded base64 PDFs for "own use" processor (minimal valid PDFs)
  const DUMMY_RISK_ASSESSMENT_PDF = "data:application/pdf;base64,JVBERi0xLjQKJeLjz9MKMyAwIG9iago8PC9UeXBlL1BhZ2UvUGFyZW50IDIgMCBSL1Jlc291cmNlczw8L0ZvbnQ8PC9GMSA1IDAgUj4+Pj4vTWVkaWFCb3hbMCAwIDU5NSA4NDJdL0NvbnRlbnRzIDQgMCBSPj4KZW5kb2JqCjQgMCBvYmoKPDwvTGVuZ3RoIDQ0Pj4Kc3RyZWFtCkJUCi9GMSA0OCBUZgoxMCAxMCBUZAooT3duIFVzZSkgVGoKRVQKZW5kc3RyZWFtCmVuZG9iago1IDAgb2JqCjw8L1R5cGUvRm9udC9TdWJ0eXBlL1R5cGUxL0Jhc2VGb250L1RpbWVzLVJvbWFuPj4KZW5kb2JqCjIgMCBvYmoKPDwvVHlwZS9QYWdlcy9LaWRzWzMgMCBSXS9Db3VudCAxPj4KZW5kb2JqCjEgMCBvYmoKPDwvVHlwZS9DYXRhbG9nL1BhZ2VzIDIgMCBSPj4KZW5kb2JqCjYgMCBvYmoKPDwvUHJvZHVjZXIoaVRleHQpL0NyZWF0aW9uRGF0ZShEOjIwMjQwMTAxMTAwMDAwKS9Nb2REYXRlKEQ6MjAyNDAxMDExMDAwMDApPj4KZW5kb2JqCnhyZWYKMCA3CjAwMDAwMDAwMDAgNjU1MzUgZiAKMDAwMDAwMDIzOSAwMDAwMCBuIAowMDAwMDAwMTg4IDAwMDAwIG4gCjAwMDAwMDAwMTUgMDAwMDAgbiAKMDAwMDAwMDEyNCAwMDAwMCBuIAowMDAwMDAwMTY4IDAwMDAwIG4gCjAwMDAwMDAyODggMDAwMDAgbiAKdHJhaWxlcgo8PC9TaXplIDcvUm9vdCAxIDAgUi9JbmZvIDYgMCBSPj4Kc3RhcnR4cmVmCjM5MAolJUVPRgo=";
  
  const DUMMY_AGREEMENT_PDF = "data:application/pdf;base64,JVBERi0xLjQKJeLjz9MKMyAwIG9iago8PC9UeXBlL1BhZ2UvUGFyZW50IDIgMCBSL1Jlc291cmNlczw8L0ZvbnQ8PC9GMSA1IDAgUj4+Pj4vTWVkaWFCb3hbMCAwIDU5NSA4NDJdL0NvbnRlbnRzIDQgMCBSPj4KZW5kb2JqCjQgMCBvYmoKPDwvTGVuZ3RoIDQ0Pj4Kc3RyZWFtCkJUCi9GMSA0OCBUZgoxMCAxMCBUZAooT3duIFVzZSkgVGoKRVQKZW5kc3RyZWFtCmVuZG9iago1IDAgb2JqCjw8L1R5cGUvRm9udC9TdWJ0eXBlL1R5cGUxL0Jhc2VGb250L1RpbWVzLVJvbWFuPj4KZW5kb2JqCjIgMCBvYmoKPDwvVHlwZS9QYWdlcy9LaWRzWzMgMCBSXS9Db3VudCAxPj4KZW5kb2JqCjEgMCBvYmoKPDwvVHlwZS9DYXRhbG9nL1BhZ2VzIDIgMCBSPj4KZW5kb2JqCjYgMCBvYmoKPDwvUHJvZHVjZXIoaVRleHQpL0NyZWF0aW9uRGF0ZShEOjIwMjQwMTAxMTAwMDAwKS9Nb2REYXRlKEQ6MjAyNDAxMDExMDAwMDApPj4KZW5kb2JqCnhyZWYKMCA3CjAwMDAwMDAwMDAgNjU1MzUgZiAKMDAwMDAwMDIzOSAwMDAwMCBuIAowMDAwMDAwMTg4IDAwMDAwIG4gCjAwMDAwMDAwMTUgMDAwMDAgbiAKMDAwMDAwMDEyNCAwMDAwMCBuIAowMDAwMDAwMTY4IDAwMDAwIG4gCjAwMDAwMDAyODggMDAwMDAgbiAKdHJhaWxlcgo8PC9TaXplIDcvUm9vdCAxIDAgUi9JbmZvIDYgMCBSPj4Kc3RhcnR4cmVmCjM5MAolJUVPRgo=";
  
  const [uploadCertificate, setUplaodCertificate] = useState(
    processor?.certificateMeta?.name || processor?.attachmentMeta?.name || ""
  );

  const [riskAssesmentDocument, setRiskAssesmentDocument] = useState(
    processor?.vendorRiskDocument || ""
  );

  const [dataProcessorCertificate, setDataProcessorCertificate] = useState(
    processor?.certificate || processor?.attachment || ""
  );
  const [uploadRiskAssesment, setUplaodRiskAssesment] = useState(
    processor?.vendorRiskDocumentMeta?.name || ""
  );
  const [agreementDocument, setAgreementDocument] = useState(
    processor?.attachment || processor?.agreementDocument || ""
  );
  const [uploadAgreementDocument, setUploadAgreementDocument] = useState(
    processor?.attachmentMeta?.name || processor?.agreementDocumentMeta?.name || ""
  );
  const [uploadAgreementDocumentSize, setUploadAgreementDocumentSize] = useState(
    processor?.attachmentMeta?.size ?? processor?.agreementDocumentMeta?.size ?? null
  );

  // SPOC fields
  const [spocName, setSpocName] = useState(processor?.spoc?.name || "");
  const [spocEmail, setSpocEmail] = useState(processor?.spoc?.email || "");
  const [spocEmailError, setSpocEmailError] = useState(null);
  const [spocNameError, setSpocNameError] = useState(null);
  const [isCrossBordered, setIsCrossBordered] = useState(processor?.isCrossBordered || false);

  const setInitialValuesForUpdate = () => {
    if (dataProcessorCertificate) {
      console.log("Yes I have attachment");

      try {
        // Ensure base64 string is cleaned
        let base64Data = dataProcessorCertificate;

        // If it has a prefix like "data:application/pdf;base64,...", strip it
        if (base64Data.includes(",")) {
          base64Data = base64Data.split(",")[1];
        }

        // Remove any whitespace, newlines, etc.
        base64Data = base64Data.replace(/[\r\n\s]+/g, "");

        // Convert to binary
        //const byteCharacters = atob(base64Data);
        const byteCharacters = decodeBase64(dataProcessorCertificate);
        const byteNumbers = new Array(byteCharacters.length);

        for (let i = 0; i < byteCharacters.length; i++) {
          byteNumbers[i] = byteCharacters.charCodeAt(i);
        }

        const byteArray = new Uint8Array(byteNumbers);

        // Get the content type from metadata
        const contentType = processor.attachmentMeta?.contentType || processor.certificateMeta?.contentType || "application/pdf";
        
        // Create a Blob with the correct content type
        const blob = new Blob([byteArray], { type: contentType });

        // Create object URL and open in new tab
        const fileURL = URL.createObjectURL(blob);

        // Update state
        setCertificatePreviewUrl(fileURL);
        setUplaodCertificate(processor.certificateMeta?.name || processor.attachmentMeta?.name);
        setUplaodCertificateSize(processor.certificateMeta?.size || processor.attachmentMeta?.size);
      } catch (error) {
        console.log("Error while decoding base64 attachment:", error);
        setCertificatePreviewUrl("");
        setUplaodCertificate("");
        setUplaodCertificateSize("");
      }
    } else {
      setCertificatePreviewUrl("");
      setUplaodCertificate("");
      setUplaodCertificateSize("");
    }
    
    if (riskAssesmentDocument) {
      let base64Data =
        riskAssesmentDocument.split(",")[1] || riskAssesmentDocument;

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
      setUplaodRiskAssesment(processor.vendorRiskDocumentMeta.name);
      setUplaodRiskAssesmentSize(processor.vendorRiskDocumentMeta.size);
    } else {
      setRiskDocumentPreviewUrl("");
      setUplaodRiskAssesment("");
      setUplaodRiskAssesmentSize("");
    }

    // Handle agreement document if it exists (API uses attachment + attachmentMeta for agreement doc)
    const agreementDocData = processor?.attachment || processor?.agreementDocument;
    const agreementDocMeta = processor?.attachmentMeta || processor?.agreementDocumentMeta;
    
    if (agreementDocData) {
      setAgreementDocument(agreementDocData);
      if (agreementDocMeta) {
        setUploadAgreementDocument(agreementDocMeta.name || "");
        setUploadAgreementDocumentSize(agreementDocMeta.size ?? null);
        
        // Create preview URL from base64
        try {
          let base64Data = agreementDocData;
          if (base64Data.includes(",")) {
            base64Data = base64Data.split(",")[1];
          }
          base64Data = base64Data.replace(/[\r\n\s]+/g, "");
          
          const byteCharacters = decodeBase64(base64Data);
          const byteNumbers = new Array(byteCharacters.length);
          for (let i = 0; i < byteCharacters.length; i++) {
            byteNumbers[i] = byteCharacters.charCodeAt(i);
          }
          const byteArray = new Uint8Array(byteNumbers);
          
          const contentType = agreementDocMeta.contentType || "application/pdf";
          const blob = new Blob([byteArray], { type: contentType });
          const fileURL = URL.createObjectURL(blob);
          setAgreementDocumentPreviewUrl(fileURL);
        } catch (error) {
          console.log("Error while decoding agreement document:", error);
          setAgreementDocumentPreviewUrl("");
        }
      }
    } else {
      setAgreementDocument("");
      setUploadAgreementDocument("");
      setAgreementDocumentPreviewUrl("");
      setUploadAgreementDocumentSize(null);
    }
  };

  useEffect(() => {
    console.log("🔍 DataProcessorUpdateModal - Processor data received:", {
      processorName: processor?.dataProcessorName,
      details: processor?.details,
      hasCertificate: !!processor?.certificate,
      certificateName: processor?.certificateMeta?.name,
      certificateSize: processor?.certificateMeta?.size,
      hasAttachment: !!processor?.attachment,
      attachmentName: processor?.attachmentMeta?.name,
      hasVendorRisk: !!processor?.vendorRiskDocument,
      vendorRiskName: processor?.vendorRiskDocumentMeta?.name,
      hasAgreementDoc: !!(processor?.attachment || processor?.agreementDocument),
      agreementDocName: processor?.attachmentMeta?.name || processor?.agreementDocumentMeta?.name
    });
    
    setInitialValuesForUpdate();
    // Update SPOC fields when processor changes
    if (processor?.spoc) {
      setSpocName(processor.spoc.name || "");
      setSpocEmail(processor.spoc.email || "");
    }
    // Update isCrossBordered when processor changes
    setIsCrossBordered(processor?.isCrossBordered || false);
    
    // Initialize original values for translation
    if (processor) {
      setOriginalProcessorName(processor.dataProcessorName || "");
      setOriginalProcessorDetails(processor.details || "");
      setOriginalSpocName(processor.spoc?.name || "");
      setProcessorName(processor.dataProcessorName || "");
      setSelectedProcessor(processor.details || "");
      setSpocName(processor.spoc?.name || "");
    }
  }, [processor]);
  const [processorCallBackUrl, setProcessorCallBackUrl] = useState(
    processor?.callbackUrl || systemCallbackUrl || ""
  );

  const [selectedProcessor, setSelectedProcessor] = useState(
    processor.details || ""
  );

  // Translation function for input fields
  const translateInputFields = async (name, details, spocNameValue, targetLanguage) => {
    // Set flag to prevent user input from interfering
    isTranslatingRef.current = true;
    setIsTranslating(true);
    
    try {
      const targetLang = targetLanguage || activeLanguage;
      
      if (targetLang === 'en') {
        // If English, use original values
        setProcessorName(originalProcessorName || name || "");
        setSelectedProcessor(originalProcessorDetails || details || "");
        setSpocName(originalSpocName || spocNameValue || "");
          return;
        }

      // Use original English values for translation
      const nameToTranslate = originalProcessorName || name || "";
      const detailsToTranslate = originalProcessorDetails || details || "";
      const spocNameToTranslate = originalSpocName || spocNameValue || "";

      const textsToTranslate = [];
      if (nameToTranslate && nameToTranslate.trim()) {
        textsToTranslate.push({ id: 'processor-name', source: nameToTranslate });
      }
      if (detailsToTranslate && detailsToTranslate.trim()) {
        textsToTranslate.push({ id: 'processor-details', source: detailsToTranslate });
      }
      if (spocNameToTranslate && spocNameToTranslate.trim()) {
        textsToTranslate.push({ id: 'spoc-name', source: spocNameToTranslate });
      }

      if (textsToTranslate.length === 0) {
        // No text to translate, clear fields
        setProcessorName("");
        setSelectedProcessor("");
        setSpocName("");
        return;
      }

      // Always translate from English to target language
      const translationResult = await dispatch(getTranslations(textsToTranslate, targetLang, 'en'));
      
      if (translationResult && translationResult.output && Array.isArray(translationResult.output)) {
        const translatedName = translationResult.output.find(t => t.id === 'processor-name')?.target || nameToTranslate;
        const translatedDetails = translationResult.output.find(t => t.id === 'processor-details')?.target || detailsToTranslate;
        const translatedSpocName = translationResult.output.find(t => t.id === 'spoc-name')?.target || spocNameToTranslate;
        
        // Update all fields at once to prevent flickering
        setProcessorName(translatedName);
        setSelectedProcessor(translatedDetails);
        setSpocName(translatedSpocName);
      }
      } catch (error) {
      console.error('Error translating input fields:', error);
      // On error, keep original values
      const nameToTranslate = originalProcessorName || name || "";
      const detailsToTranslate = originalProcessorDetails || details || "";
      const spocNameToTranslate = originalSpocName || spocNameValue || "";
      setProcessorName(nameToTranslate);
      setSelectedProcessor(detailsToTranslate);
      setSpocName(spocNameToTranslate);
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
  const handleLanguageChange = async (e) => {
    const selectedLang = e.target.value;
    const previousLang = activeLanguage;
    
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
    setActiveLanguage(selectedLang);
    
    // If switching to English
    if (selectedLang === 'en') {
      // If we have original English values, use them
      if (originalProcessorName || originalProcessorDetails || originalSpocName) {
        setProcessorName(originalProcessorName || "");
        setSelectedProcessor(originalProcessorDetails || "");
        setSpocName(originalSpocName || "");
      } else if (previousLang !== 'en' && (processorName || selectedProcessor || spocName)) {
        // No originals stored but we have values, translate current values back to English
        setIsTranslating(true);
        try {
          const textsToTranslate = [];
          if (processorName && processorName.trim()) {
            textsToTranslate.push({ id: 'processor-name', source: processorName });
          }
          if (selectedProcessor && selectedProcessor.trim()) {
            textsToTranslate.push({ id: 'processor-details', source: selectedProcessor });
          }
          if (spocName && spocName.trim()) {
            textsToTranslate.push({ id: 'spoc-name', source: spocName });
          }
          
          if (textsToTranslate.length > 0) {
            const translationResult = await dispatch(getTranslations(textsToTranslate, 'en', previousLang));
            
            if (translationResult && translationResult.output && Array.isArray(translationResult.output)) {
              const englishName = translationResult.output.find(t => t.id === 'processor-name')?.target || processorName;
              const englishDetails = translationResult.output.find(t => t.id === 'processor-details')?.target || selectedProcessor;
              const englishSpocName = translationResult.output.find(t => t.id === 'spoc-name')?.target || spocName;
              
              setOriginalProcessorName(englishName);
              setOriginalProcessorDetails(englishDetails);
              setOriginalSpocName(englishSpocName);
              
              setProcessorName(englishName);
              setSelectedProcessor(englishDetails);
              setSpocName(englishSpocName);
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
        setSelectedProcessor("");
        setSpocName("");
      }
    } 
    // If switching to non-English
    else {
      // First, ensure we have English originals
      // If we don't have originals, we need to get them first
      if (!originalProcessorName && !originalProcessorDetails && !originalSpocName) {
        // If previous language was English and we have values, store them as originals
        if (previousLang === 'en' && (processorName || selectedProcessor || spocName)) {
          setOriginalProcessorName(processorName || "");
          setOriginalProcessorDetails(selectedProcessor || "");
          setOriginalSpocName(spocName || "");
          // Now translate to the new language
          await translateInputFields(processorName, selectedProcessor, spocName, selectedLang);
        } 
        // If previous language was non-English, translate current values back to English first
        else if (previousLang !== 'en' && (processorName || selectedProcessor || spocName)) {
          setIsTranslating(true);
          try {
            const textsToTranslate = [];
            if (processorName && processorName.trim()) {
              textsToTranslate.push({ id: 'processor-name', source: processorName });
            }
            if (selectedProcessor && selectedProcessor.trim()) {
              textsToTranslate.push({ id: 'processor-details', source: selectedProcessor });
            }
            if (spocName && spocName.trim()) {
              textsToTranslate.push({ id: 'spoc-name', source: spocName });
            }
            
            if (textsToTranslate.length > 0) {
              // Translate from previous language to English
              const translationResult = await dispatch(getTranslations(textsToTranslate, 'en', previousLang));
              
              if (translationResult && translationResult.output && Array.isArray(translationResult.output)) {
                const englishName = translationResult.output.find(t => t.id === 'processor-name')?.target || processorName;
                const englishDetails = translationResult.output.find(t => t.id === 'processor-details')?.target || selectedProcessor;
                const englishSpocName = translationResult.output.find(t => t.id === 'spoc-name')?.target || spocName;
                
                setOriginalProcessorName(englishName);
                setOriginalProcessorDetails(englishDetails);
                setOriginalSpocName(englishSpocName);
                
                // Now translate from English to the new selected language
                await translateInputFields(englishName, englishDetails, englishSpocName, selectedLang);
              }
            }
          } catch (error) {
            console.error('Error translating to English first:', error);
            setIsTranslating(false);
          }
        } else {
          // No values at all, clear fields
          setProcessorName("");
          setSelectedProcessor("");
          setSpocName("");
        }
      } else {
        // We have English originals, translate to new language
        await translateInputFields(originalProcessorName, originalProcessorDetails, originalSpocName, selectedLang);
      }
    }
  };

  const handleProcessorNameChange = (e) => {
    const value = e.target.value;
    setProcessorName(value);
    // If typing in English, store as original
    if (activeLanguage === 'en') {
      setOriginalProcessorName(value);
    }
  };

  const handleProcessorCallBackUrlChange = (e) => {
    setProcessorCallBackUrl(e.target.value);
  };

  const handleProcessorDetailsChange = (value) => {
    // Don't update if we're currently translating (to prevent flickering)
    if (isTranslatingRef.current) {
      return;
    }
    
    // Always update the display value immediately for responsive typing
    setSelectedProcessor(value || "");
    
    // If typing in English, store as original English value
    if (activeLanguage === 'en') {
      setOriginalProcessorDetails(value || "");
    }
  };

  const handleSpocNameChange = (e) => {
    const value = e.target.value;
    setSpocName(value);
    
    // If typing in English, store as original
    if (activeLanguage === 'en') {
      setOriginalSpocName(value);
    }
    
    if (value.trim() === "" || value.length > 30) {
      setSpocNameError("Please enter a valid Name (max 30 characters)");
    } else {
      setSpocNameError(null);
    }
  };

  const handleSpocEmailChange = (e) => {
    const value = e.target.value.trim();
    setSpocEmail(value);
    
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    
    if (emailRegex.test(value)) {
      setSpocEmailError(null);
    } else {
      setSpocEmailError("Please enter a valid Email ID");
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
      
      if (validationResult.valid) {  // ✅ FIX: Check validationResult.valid
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

  // ✅ SECURE: Certificate validation with magic number verification
  const validateCertificateFile = async (file) => {
    try {
      const validationResult = await validateCertificateSecurity(file);
      
      if (!validationResult.valid) {
        const errorMessage = formatValidationErrors(validationResult);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={errorMessage}
            />
          ),
          { icon: false }
        );
        return { valid: false };
      }
      
      return { valid: true };
    } catch (error) {
      console.error('Certificate validation error:', error);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="File validation failed. Please try again."
          />
        ),
        { icon: false }
      );
      return { valid: false };
    }
  };

  // ✅ SECURE: PDF validation with malicious content scanning  
  const validatePDFFile = async (file) => {
    try {
      const validationResult = await validatePDFSecurity(file);
      
      if (!validationResult.valid) {
        const errorMessage = formatValidationErrors(validationResult);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={errorMessage}
            />
          ),
          { icon: false }
        );
        return { valid: false };
      }
      
      console.log('✅ PDF validated:', {
        sanitizedFilename: validationResult.sanitizedFilename,
        detectedType: validationResult.fileInfo.detectedType
      });
      
      return { valid: true };
    } catch (error) {
      console.error('PDF validation error:', error);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="File validation failed. Please try again."
          />
        ),
        { icon: false }
      );
      return { valid: false };
    }
  };

  // ✅ SECURE: Agreement document validation
  // Use validateDocumentFile from fileValidation.js which handles PDFs
  const validateAgreementDocument = async (file) => {
    const validationResult = await validatePDFSecurity(file);
      
      if (!validationResult.valid) {
        const errorMessage = formatValidationErrors(validationResult);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={errorMessage}
            />
          ),
          { icon: false }
        );
    }
    
    return validationResult;
  };

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
      
      if (validationResult.valid) {  // ✅ FIX: Check validationResult.valid
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
      
      if (validationResult.valid) {  // ✅ FIX: Check validationResult.valid
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

  const handleCertificateChange = async (e) => {
    if (e.target.files.length > 0) {
      const file = e.target.files[0];
      
      // ✅ Async validation with magic number verification
      const validationResult = await validateCertificateFile(file);
      
      if (validationResult.valid) {  // ✅ FIX: Check validationResult.valid
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
    setDataProcessorCertificate("");
    setCertificatePreviewUrl("");
    setUplaodCertificateSize(null);
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
            Update processor{" "}
          </Text>{" "}
          <div className="language-tabs-container">
                <select
                  value={activeLanguage}
                  onChange={handleLanguageChange}
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
            onChange={handleProcessorNameChange}
            name="dataProcessorName"
          ></InputFieldV2>{" "}
          <br></br>
          <InputFieldV2
            label="Call back URL (Required)"
            value={processorCallBackUrl}
            onChange={handleProcessorCallBackUrlChange}
            name="callbackUrl"
          ></InputFieldV2>{" "}
          <br></br>
          <InputFieldV2
            label="SPOC Name (Required)"
            value={spocName}
            onChange={handleSpocNameChange}
            name="spocName"
            state={spocNameError ? "error" : "none"}
            stateText={spocNameError || ""}
          ></InputFieldV2>{" "}
          <br></br>
          <InputFieldV2
            label="Email (Required)"
            value={spocEmail}
            onChange={handleSpocEmailChange}
            name="spocEmail"
            state={spocEmailError === null && spocEmail ? "success" : spocEmailError ? "error" : "none"}
            stateText={spocEmailError || ""}
          ></InputFieldV2>{" "}
          <br></br>
          <WordLimitedTextarea
            value={selectedProcessor || ""}
            onTextChange={handleProcessorDetailsChange}
          />
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
                <div className="previewFile" onClick={handlePreviewCertificte}>
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
          
          {/* Disable Risk Assessment upload for "own use" processor */}
          {!isOwnUseProcessor && (
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
          )}
          
          {!isOwnUseProcessor && <br />}
          
          {/* Disable Agreement Document upload for "own use" processor */}
          {!isOwnUseProcessor && (
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
          )}
          <div className="modal-add-btn-container">
            {" "}
            <ActionButton
              label="Update"
              onClick={submitUpdateProcessor}
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

export default DataProcessorUpdateModal;
