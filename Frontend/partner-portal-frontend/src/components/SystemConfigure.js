import {
  BadgeV2,
  InputFieldV2,
  Text,
  FileUploader,
  Icon,
  ActionButton,
  Notifications,
} from "../custom-components";
import {
  IcClose,
  IcError,
  IcErrorColored,
  IcInfo,
  IcSuccess,
  IcUpload,
} from "../custom-components/Icon";
import { use, useState, useRef } from "react";
import { CLEAR_SESSION } from "../store/constants/Constants";
import "../styles/sessionModal.css";
import "../styles/systemConfiguration.css";
import "../styles/pageConfiguration.css";
import "../styles/PreviewFile.css";
import { Ic404Error } from "../custom-components/Icon";
import { useEffect } from "react";
import { useDispatch } from "react-redux";
import { useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import {
  getSystemConfig,
  submitSystemConfiguration,
  updateSystemConfig,
  getClientCredential
} from "../store/actions/CommonAction";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import ConfigurationShimmer from "./ConfigurationShimmer";
import { 
  validateImageFile,
  validateCertificateFile as validateCertificateSecurity,
  formatValidationErrors 
} from "../utils/fileValidation";
const SystemConfigure = () => {
  const [files1, setFiles1] = useState([]);
  const [files2, setFiles2] = useState([]);
  const [baseUrl, setBaseUrl] = useState("");
  const [consentExpiry, setConsentExpiry] = useState("99");
  const [token, setToken] = useState("60");
  const [artifactExpiry, setArtifactExpiry] = useState("99");
  const [businessUniqueId, setBusinessUniqueId] = useState("");
  const [consumerSecret, setConsumerSecret] = useState("");
  const [clientId, setClientId] = useState(businessUniqueId);
  const [clientSecretKey, setClientSecretKey] = useState(consumerSecret);
  const [uploadFile, setUplaodFile] = useState("");
  const [uplaodLogo, setUploadLogo] = useState("");
  const [fileBase64, setFileBase64] = useState(null);
  const [logoBase64, setLogoBase64] = useState(null);
  const [logoSize, setLogoSize] = useState(0);
  const [disableSaveBtn, setDisableSaveBtn] = useState(false);
  const [logoPreviewUrl, setLogoPreviewUrl] = useState("");
  const [certificatePreviewUrl, setCertificatePreviewUrl] = useState("");
  const [showSessionModal, setShowSessionModal] = useState(false);
  const [updateSystemConfiguration, setUpdateSystemConfiguration] =
    useState(false);

  const [systemConfigId, setSystemConfigId] = useState("");
  const [artefactRetention, setArtefactRetention] = useState({
    value: "99",
    unit: "",
  });

  
  const [artefactInput, setArtefactInput] = useState("99");
  const [duration, setDuration] = useState("YEARS");
  const [refresh, setRefresh] = useState(false);
  const [loading, setLoading] = useState(true);

  const handleArtefactChange = (e) => setArtefactInput(e.target.value);

const handleArtefactDuration = (e) => setDuration(e.target.value);

  const tenantId = useSelector((state) => state.common.tenant_id);
  const sessionToken = useSelector((state) => state.common.session_token);
  const businessId = useSelector((state) => state.common.business_id);
  
  // Check if P12 upload is allowed (only when businessId === tenantId)
  const isP12UploadAllowed = businessId === tenantId;

  const [fileName, setFileName] = useState("");
  const [certificateSize, setCertificateSize] = useState(0);
  const fileInputRef = useRef(null);

  const [logoName, setLogoName] = useState("");
  const logoInputRef = useRef(null);
  
  // P12 Certificate state
  const [p12FileName, setP12FileName] = useState("");
  const [p12Size, setP12Size] = useState(0);
  const [p12Base64, setP12Base64] = useState(null);
  const [p12PreviewUrl, setP12PreviewUrl] = useState("");
  const [p12Password, setP12Password] = useState("");
  const [p12Alias, setP12Alias] = useState("");
  const [showP12PasswordModal, setShowP12PasswordModal] = useState(false);
  const [pendingP12File, setPendingP12File] = useState(null);
  const p12InputRef = useRef(null);
  
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const convertToBase64 = (file) => {
    const reader = new FileReader();
    reader.readAsDataURL(file); // reads file as base64
    reader.onload = () => {
      setFileBase64(reader.result); // base64 string here
      console.log("Base64 File:", reader.result);
    };
    reader.onerror = (error) => {
      console.log("Error converting file:", error);
    };
  };

  const convertLogoToBase64 = (file) => {
    const reader = new FileReader();
    reader.readAsDataURL(file); // reads file as base64
    reader.onload = () => {
      setLogoBase64(reader.result); // base64 string here
      console.log("Base64 Logo:", reader.result);
    };
    reader.onerror = (error) => {
      console.log("Error converting file:", error);
    };
  };

  const convertP12ToBase64 = (file) => {
    const reader = new FileReader();
    reader.readAsDataURL(file); // reads file as base64
    reader.onload = () => {
      setP12Base64(reader.result); // base64 string here
      console.log("Base64 P12:", reader.result);
    };
    reader.onerror = (error) => {
      console.log("Error converting P12 file:", error);
    };
  };

  // ✅ SECURE: Certificate validation with magic number verification
  const validateFile = async (file) => {
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
        return false;
      }
      
      // ✅ Validation passed - don't show toast yet, wait for actual upload success
      return true;
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
      return false;
    }
  };

  // ✅ SECURE: P12 Certificate validation
  const validateP12File = async (file) => {
    try {
      // Check file extension
      const fileName = file.name.toLowerCase();
      const validExtensions = ['.p12', '.pfx'];
      const hasValidExtension = validExtensions.some(ext => fileName.endsWith(ext));
      
      if (!hasValidExtension) {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message="Invalid file type. Please upload a .p12 or .pfx certificate file."
            />
          ),
          { icon: false }
        );
        return false;
      }

      // Check file size (500KB limit)
      const maxSize = 500 * 1024; // 500KB in bytes
      if (file.size > maxSize) {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message="File size exceeds 500KB limit. Please upload a smaller file."
            />
          ),
          { icon: false }
        );
        return false;
      }

      // Check MIME type
      const validMimeTypes = [
        'application/x-pkcs12',
        'application/pkcs12',
        'application/x-pkcs12-cert',
      ];
      
      if (file.type && !validMimeTypes.includes(file.type)) {
        // Some browsers may not set MIME type correctly, so we'll be lenient
        // and only check if MIME type is present and invalid
        console.warn('P12 file MIME type may not be recognized by browser:', file.type);
      }

      return true;
    } catch (error) {
      console.error('P12 validation error:', error);
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
      return false;
    }
  };

  // ✅ SECURE: Image validation with XSS detection
  const validateLogo = async (file) => {
    try {
      const validationResult = await validateImageFile(file);
      
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
        return false;
      }
      
      // ✅ Validation passed - don't show toast yet, wait for actual upload success
      return true;
    } catch (error) {
      console.error('Image validation error:', error);
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
      return false;
    }
  };

  const handleClick = () => {
    fileInputRef.current.click(); // trigger hidden input
  };

  const handleLogoClick = () => {
    logoInputRef.current.click();
  };

  const handleP12Click = () => {
    if (!isP12UploadAllowed) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="P12 certificate upload is only available when Business ID matches Tenant ID."
          />
        ),
        { icon: false }
      );
      return;
    }
    p12InputRef.current.click();
  };

  const handleChange = async (e) => {
    if (e.target.files.length > 0) {
      const file = e.target.files[0];
      
      // ✅ Async validation
      const isValid = await validateFile(file);
      
      if (isValid) {
        setFileName(file.name);
        setCertificateSize(file.size);
        convertToBase64(file);
        setCertificatePreviewUrl(URL.createObjectURL(file));
      } else {
        e.target.value = ""; // clear invalid file
        setFileName("");
        setCertificateSize(0);
        setCertificatePreviewUrl("");
      }
    }
  };

  const handleLogoChange = async (e) => {
    if (e.target.files.length > 0) {
      const file = e.target.files[0];

      // ✅ Async validation
      const isValid = await validateLogo(file);
      
      if (isValid) {
        setLogoName(file.name);
        setLogoSize(file.size);
        convertLogoToBase64(file);
        setLogoPreviewUrl(URL.createObjectURL(file));
      } else {
        setLogoName("");
        setLogoSize(0);
        setLogoPreviewUrl("");
        
      }
    }
  };

  const handleP12Change = async (e) => {
    if (!isP12UploadAllowed) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="P12 certificate upload is only available when Business ID matches Tenant ID."
          />
        ),
        { icon: false }
      );
      if (e.target) {
        e.target.value = "";
      }
      return;
    }

    if (e.target.files.length > 0) {
      const file = e.target.files[0];
      
      // ✅ Async validation
      const isValid = await validateP12File(file);
      
      if (isValid) {
        // Store file temporarily and show password modal
        setPendingP12File(file);
        setShowP12PasswordModal(true);
      } else {
        e.target.value = ""; // clear invalid file
        setP12FileName("");
        setP12Size(0);
        setP12PreviewUrl("");
      }
    }
  };

  const handleP12PasswordSubmit = () => {
    if (!p12Password.trim()) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="Please enter the P12 certificate password"
          />
        ),
        { icon: false }
      );
      return;
    }

    const trimmedAlias = (p12Alias || "").trim();
    if (!trimmedAlias) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="Please enter the certificate alias (alphanumeric only)"
          />
        ),
        { icon: false }
      );
      return;
    }
    if (!/^[a-zA-Z0-9]+$/.test(trimmedAlias)) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="Alias must contain only letters and numbers (no spaces or special characters)"
          />
        ),
        { icon: false }
      );
      return;
    }

    if (pendingP12File) {
      setP12FileName(pendingP12File.name);
      setP12Size(pendingP12File.size);
      setP12Alias(trimmedAlias);
      convertP12ToBase64(pendingP12File);
      setP12PreviewUrl(URL.createObjectURL(pendingP12File));
      setPendingP12File(null);
      setShowP12PasswordModal(false);
    }
  };

  const handleP12PasswordCancel = () => {
    if (p12InputRef.current) {
      p12InputRef.current.value = "";
    }
    setPendingP12File(null);
    setP12Password("");
    setP12Alias("");
    setShowP12PasswordModal(false);
  };

  const handleRemoveFile = () => {
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
    setFileName("");
    setCertificateSize(0);
    // also clear the input value
    //document.getElementById("fileInput").value = "";
  };

  const handlePreviewLogo = () => {
    if (logoPreviewUrl) {
      window.open(logoPreviewUrl, "_blank");
    }
  };

  const handlePreviewCertificte = () => {
    if (certificatePreviewUrl) {
      window.open(certificatePreviewUrl, "_blank");
    }
  };
  const handleRemoveLogo = () => {
    if (logoInputRef.current) {
      logoInputRef.current.value = "";
    }
    setLogoName("");
    setLogoSize(0);
    setLogoPreviewUrl("");
  };

  const handleRemoveP12 = () => {
    if (p12InputRef.current) {
      p12InputRef.current.value = "";
    }
    setP12FileName("");
    setP12Size(0);
    setP12PreviewUrl("");
    setP12Base64(null);
    setP12Password("");
    setP12Alias("");
  };

  const handlePreviewP12 = () => {
    if (p12PreviewUrl) {
      window.open(p12PreviewUrl, "_blank");
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    e.currentTarget.classList.add("dragover");
  };

  const handleLogoDragOver = (e) => {
    e.preventDefault();
    e.currentTarget.classList.add("dragover");
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.currentTarget.classList.remove("dragover");
  };

  const handleLogoDragLeave = (e) => {
    e.preventDefault();
    e.currentTarget.classList.remove("dragover");
  };

  const handleP12DragOver = (e) => {
    e.preventDefault();
    e.currentTarget.classList.add("dragover");
  };

  const handleP12DragLeave = (e) => {
    e.preventDefault();
    e.currentTarget.classList.remove("dragover");
  };

  const handleDrop = async (e) => {
    e.preventDefault();
    e.currentTarget.classList.remove("dragover");

    if (e.dataTransfer.files.length > 0) {
      const file = e.dataTransfer.files[0];
      
      // ✅ Async validation for drag & drop
      const isValid = await validateFile(file);
      
      if (isValid) {
        fileInputRef.current.files = e.dataTransfer.files;
        setFileName(file.name);
        setCertificateSize(file.size);
        setCertificatePreviewUrl(URL.createObjectURL(file));
        convertToBase64(file);
      } else {
        fileInputRef.current.value = "";
        setFileName("");
        setCertificateSize(0);
        setCertificatePreviewUrl("");
      }
    }
  };

  const handleLogoDrop = async (e) => {
    e.preventDefault();
    e.currentTarget.classList.remove("dragover");

    if (e.dataTransfer.files.length > 0) {
      const file = e.dataTransfer.files[0];
      
      // ✅ Async validation for drag & drop
      const isValid = await validateLogo(file);
      
      if (isValid) {
        logoInputRef.current.files = e.dataTransfer.files;
        setLogoName(file.name);
        setLogoSize(file.size);
        convertLogoToBase64(file);
        setLogoPreviewUrl(URL.createObjectURL(file));
      } else {
        setLogoName("");
        setLogoSize(0);
        setLogoPreviewUrl("");
      }
    }
  };

  const handleP12Drop = async (e) => {
    e.preventDefault();
    e.currentTarget.classList.remove("dragover");

    if (!isP12UploadAllowed) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="P12 certificate upload is only available when Business ID matches Tenant ID."
          />
        ),
        { icon: false }
      );
      return;
    }

    if (e.dataTransfer.files.length > 0) {
      const file = e.dataTransfer.files[0];
      
      // ✅ Async validation for drag & drop
      const isValid = await validateP12File(file);
      
      if (isValid) {
        p12InputRef.current.files = e.dataTransfer.files;
        // Store file temporarily and show password modal
        setPendingP12File(file);
        setShowP12PasswordModal(true);
      } else {
        p12InputRef.current.value = "";
        setP12FileName("");
        setP12Size(0);
        setP12PreviewUrl("");
      }
    }
  };

  const handleBaseUrlChange = (e) => {
    let value = e.target.value.replace(/\s+/g, ""); // no spaces
    setBaseUrl(value);
  };

  // Consent Expiry (digits only)
  const handleConsentExpiryChange = (e) => {
    let value = e.target.value.replace(/\D/g, ""); // only digits
    setConsentExpiry(value);
  };

  // Token (digits only)
  const handleTokenInMinutesChange = (e) => {
    let value = e.target.value.replace(/\D/g, "");
    setToken(value);
  };

  //  Artifact Expiry (digits only)
  const handleArtifactExpiryChange = (e) => {
    let value = e.target.value.replace(/\D/g, "");
    setArtifactExpiry(value);
  };

  //  Client ID (alphabets + digits only, no spaces)
  const handleClientIdChange = (e) => {
    let value = e.target.value.replace(/[^a-zA-Z0-9]/g, "");
    setClientId(value);
  };

  //  Client Secret Key (no spaces, allow alphanum only)
  const handleClientSecretKeyChange = (e) => {
    let value = e.target.value.replace(/\s+/g, "");
    setClientSecretKey(value);
  };

  //  Final Validation on Save
  const SubmitSystemSetUpData = async () => {
    const errors = [];

    // Base URL - allows domain names or IP addresses with optional port
    const urlRegex =
      /^(https?:\/\/)?(([a-zA-Z0-9.-]+\.[a-zA-Z]{2,})|(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}))(:[0-9]+)?(\/.*)?$/;
    if (!urlRegex.test(baseUrl)) {
      errors.push("Please enter a valid Base URL");
    }

    // Consent Expiry
    if (!/^\d+$/.test(consentExpiry) || consentExpiry === "") {
      errors.push("Consent Expiry must be a number");
    }

    // Token
    if (!/^\d+$/.test(token) || token === "") {
      errors.push("Token must be a number");
    }

    // Artifact Expiry
    if (!/^\d+$/.test(artifactExpiry) || artifactExpiry === "") {
      errors.push("Signed Artefact Expiry must be a number");
    }

    // Client ID
    // if (!/^[A-Za-z0-9]+$/.test(clientId)) {
    //   errors.push("Please enter a Valid Client ID. ");
    // }

    // // Client Secret Key
    // if (!/^[A-Za-z0-9-]+$/.test(clientSecretKey)) {
    //   errors.push("Please enter a valid Client Secret.");
    // }

    // File Upload
    if (fileName == "" || fileName == undefined || fileName == null) {
      errors.push("Please upload a certificate file");
    }
    if (logoName == "" || logoName == undefined || logoName == null) {
      errors.push("Please upload a company logo");
    }

    // P12 alias required when P12 certificate is uploaded
    if (p12Base64 && (!(p12Alias || "").trim() || !/^[a-zA-Z0-9]+$/.test((p12Alias || "").trim()))) {
      errors.push("Please enter a valid P12 certificate alias (alphanumeric only)");
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
    } else if (errors.length == 1) {
      toast.error(
        (props) => <CustomToast {...props} type="error" message={errors[0]} />,
        { icon: false }
      );
    } else {
      try {
        const getCertificateContentType = (filename) => {
          if (!filename) return "application/x-pem-file";
          const lowerName = filename.toLowerCase();
          if (lowerName.endsWith('.pem')) return "application/x-pem-file";
          if (lowerName.endsWith('.crt')) return "application/x-x509-ca-cert";
          if (lowerName.endsWith('.cer')) return "application/pkix-cert";
          return "application/x-pem-file";
        };

        const sslCertificateMeta = {
          name: fileName || "ssl-certificate.pem",
          contentType: getCertificateContentType(fileName),
          size: certificateSize || 1024,
          tag: {
            tagName: "SSL_CERTIFICATE",
            tagValue: "system-ssl-cert"
          }
        };

        const logoMeta = {
          name: logoName || "company-logo.png",
          contentType: logoName?.toLowerCase().endsWith('.png') ? "image/png" : 
                       logoName?.toLowerCase().endsWith('.jpg') || logoName?.toLowerCase().endsWith('.jpeg') ? "image/jpeg" : 
                       "image/png",
          size: logoSize || 2048,
          tag: {
            tagName: "LOGO",
            tagValue: "company-logo"
          }
        };

        let res = await dispatch(
          submitSystemConfiguration(
            fileBase64,
            sslCertificateMeta,
            baseUrl,
            consentExpiry,
            token,
            artifactExpiry,
            clientId,
            clientSecretKey,
            logoBase64,
            logoMeta,
            artefactRetention,
            p12Base64,
            p12FileName || "cms_keystore.p12",
            p12Password,
            (p12Alias || "").trim() || "cms_signer",
          )
        );

        if (res.status === 500) {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Something went wrong..Please try again later."}
              />
            ),
            { icon: false }
          );
        } else if (res.status === 201) {
          setRefresh(true);
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
        } else if (res.status === 401) {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Enter Valid System Configuration details"}
              />
            ),
            { icon: false }
          );
        }
      } catch (error) {
        console.log("Error Error", error[0]);
        if (error[0]?.errorCode == "JCMP3002") {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"system Configuration is already exists."}
              />
            ),
            { icon: false }
          );
          setDisableSaveBtn(true);
          return;
        } else if (
          error[0].errorCode == "JCMP4003" ||
          error[0].errorCode == "JCMP4001"
        ) {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Session expored"}
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
                message={"something went wrong please try again later."}
              />
            ),
            { icon: false }
          );
          return;
        }
      }
    }
  };

  const updateSystemSetUpData = async () => {
    const errors = [];

    // Base URL - allows domain names or IP addresses with optional port
    const urlRegex =
      /^(https?:\/\/)?(([a-zA-Z0-9.-]+\.[a-zA-Z]{2,})|(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}))(:[0-9]+)?(\/.*)?$/;
    if (!urlRegex.test(baseUrl)) {
      errors.push("Please enter a valid Base URL");
    }

    // Consent Expiry
    if (!/^\d+$/.test(consentExpiry) || consentExpiry === "") {
      errors.push("Consent Expiry must be a number");
    }

    // Token
    if (!/^\d+$/.test(token) || token === "") {
      errors.push("Token must be a number");
    }

    // Artifact Expiry
    if (!/^\d+$/.test(artifactExpiry) || artifactExpiry === "") {
      errors.push("Signed Artefact Expiry must be a number");
    }

    // // Client ID
    // if (!/^[A-Za-z0-9]+$/.test(clientId)) {
    //   errors.push("Please enter a Valid Client ID. ");
    // }

    // // Client Secret Key
    // if (!/^[A-Za-z0-9-]+$/.test(clientSecretKey)) {
    //   errors.push("Please enter a valid Client Secret.");
    // }

    // File Upload
    if (fileName == "" || fileName == undefined || fileName == null) {
      errors.push("Please upload a certificate file");
    }
    if (logoName == "" || logoName == undefined || logoName == null) {
      errors.push("Please upload a company logo");
    }

    // P12 alias required when P12 certificate is uploaded
    if (p12Base64 && (!(p12Alias || "").trim() || !/^[a-zA-Z0-9]+$/.test((p12Alias || "").trim()))) {
      errors.push("Please enter a valid P12 certificate alias (alphanumeric only)");
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
    } else if (errors.length == 1) {
      toast.error(
        (props) => <CustomToast {...props} type="error" message={errors[0]} />,
        { icon: false }
      );
    } else {
      try {
        const getCertificateContentType = (filename) => {
          if (!filename) return "application/x-pem-file";
          const lowerName = filename.toLowerCase();
          if (lowerName.endsWith('.pem')) return "application/x-pem-file";
          if (lowerName.endsWith('.crt')) return "application/x-x509-ca-cert";
          if (lowerName.endsWith('.cer')) return "application/pkix-cert";
          return "application/x-pem-file";
        };

        const sslCertificateMeta = {
          name: fileName || "ssl-certificate.pem",
          contentType: getCertificateContentType(fileName),
          size: certificateSize || 1024,
          tag: {
            tagName: "SSL_CERTIFICATE",
            tagValue: "system-ssl-cert"
          }
        };

        const logoMeta = {
          name: logoName || "company-logo.png",
          contentType: logoName?.toLowerCase().endsWith('.png') ? "image/png" : 
                       logoName?.toLowerCase().endsWith('.jpg') || logoName?.toLowerCase().endsWith('.jpeg') ? "image/jpeg" : 
                       "image/png",
          size: logoSize || 2048,
          tag: {
            tagName: "LOGO",
            tagValue: "company-logo"
          }
        };

        let res = await dispatch(
          updateSystemConfig(
            fileBase64,
            sslCertificateMeta,
            baseUrl,
            consentExpiry,
            token,
            artifactExpiry,
            clientId,
            clientSecretKey,
            logoBase64,
            logoMeta,
            systemConfigId,
            artefactRetention,
            p12Base64,
            p12FileName || "cms_keystore.p12",
            p12Password,
            (p12Alias || "").trim() || "cms_signer",
          )
        );

        if (res.status === 500) {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Something went wrong..Please try again later."}
              />
            ),
            { icon: false }
          );
        } else if (res.status === 200) {
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
        } else if (res.status === 401) {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Enter Valid System Configuration details"}
              />
            ),
            { icon: false }
          );
        }
      } catch (error) {
        console.log("Error Error", error[0]);
        if (error[0]?.errorCode == "JCMP3002") {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"system Configuration is already exists."}
              />
            ),
            { icon: false }
          );
          setDisableSaveBtn(true);
          return;
        } else if (
          error[0].errorCode == "JCMP4003" ||
          error[0].errorCode == "JCMP4001"
        ) {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Session expored"}
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
                message={"something went wrong please try again later."}
              />
            ),
            { icon: false }
          );
          return;
        }
      }
    }
  };


  useEffect(() => {
    const fetchData = async () => {
      try {
        
        const data = await getClientCredential(businessId, sessionToken, tenantId);

        setBusinessUniqueId(data.consumerKey);
        setConsumerSecret(data.consumerSecret);

        console.log("✅ Business Unique ID:", data.consumerKey);
        console.log("✅ Consumer Secret:", data.consumerSecret);
      } catch (error) {
        console.error("Error fetching client credential:", error);
      }
    };

    fetchData();
  }, []);

  useEffect(() => {
    const fetchSystemConfig = async () => {
      try {
        setLoading(true);
        const response = await dispatch(getSystemConfig());
        console.log(
          "Searched System Config details:",
          response?.data?.searchList?.[0]
        );

        const systemConfigInfo =
          response?.data?.searchList?.[0]?.configurationJson;
        if (systemConfigInfo) {
          setBaseUrl(systemConfigInfo.baseUrl || "");
          setConsentExpiry(systemConfigInfo.defaultConsentExpiryDays || "");
          setToken(systemConfigInfo.jwtTokenTTLMinutes || "");
          setArtifactExpiry(systemConfigInfo.signedArtifactExpiryDays || "");
          setClientId(systemConfigInfo.clientId || "");
          setClientSecretKey(systemConfigInfo.clientSecret || "");
          setUpdateSystemConfiguration(true);
          setSystemConfigId(response?.data?.searchList?.[0]?.configId);
          setArtefactInput(String(systemConfigInfo.dataRetention?.value ?? ""));
          setDuration(systemConfigInfo.dataRetention?.unit ?? "");

          // Set SSL Certificate details
          if (systemConfigInfo.sslCertificate) {
            setFileBase64(systemConfigInfo.sslCertificate);
            if (systemConfigInfo.sslCertificateMeta) {
              setFileName(systemConfigInfo.sslCertificateMeta.name || "");
              setCertificateSize(systemConfigInfo.sslCertificateMeta.size || 0);
              // Create preview URL from base64
              try {
                const base64Data = systemConfigInfo.sslCertificate.split(',')[1] || systemConfigInfo.sslCertificate;
                const byteCharacters = atob(base64Data);
                const byteNumbers = new Array(byteCharacters.length);
                for (let i = 0; i < byteCharacters.length; i++) {
                  byteNumbers[i] = byteCharacters.charCodeAt(i);
                }
                const byteArray = new Uint8Array(byteNumbers);
                const blob = new Blob([byteArray], { type: systemConfigInfo.sslCertificateMeta.contentType });
                setCertificatePreviewUrl(URL.createObjectURL(blob));
              } catch (error) {
                console.error("Error creating certificate preview URL:", error);
              }
            }
          }

          // Set Logo details
          if (systemConfigInfo.logo) {
            setLogoBase64(systemConfigInfo.logo);
            if (systemConfigInfo.logoMeta) {
              setLogoName(systemConfigInfo.logoMeta.name || "");
              setLogoSize(systemConfigInfo.logoMeta.size || 0);
              // Create preview URL from base64
              try {
                // Handle both data URL format and plain base64
                const base64Data = systemConfigInfo.logo.includes(',') 
                  ? systemConfigInfo.logo.split(',')[1] 
                  : systemConfigInfo.logo;
                const byteCharacters = atob(base64Data);
                const byteNumbers = new Array(byteCharacters.length);
                for (let i = 0; i < byteCharacters.length; i++) {
                  byteNumbers[i] = byteCharacters.charCodeAt(i);
                }
                const byteArray = new Uint8Array(byteNumbers);
                const blob = new Blob([byteArray], { type: systemConfigInfo.logoMeta.contentType });
                const logoUrl = URL.createObjectURL(blob);
                setLogoPreviewUrl(logoUrl);
                
                // Dispatch logo to Redux for use in Header
                dispatch({
                  type: "SAVE_COMPANY_LOGO",
                  payload: logoUrl,
                });
              } catch (error) {
                console.error("Error creating logo preview URL:", error);
                // Fallback: use base64 directly if it's already a data URL
                if (systemConfigInfo.logo.startsWith('data:')) {
                  setLogoPreviewUrl(systemConfigInfo.logo);
                  // Dispatch fallback logo to Redux
                  dispatch({
                    type: "SAVE_COMPANY_LOGO",
                    payload: systemConfigInfo.logo,
                  });
                }
              }
            }
          }

          // Set P12 Certificate details (keystoreData)
          if (systemConfigInfo.keystoreData) {
            setP12Base64(systemConfigInfo.keystoreData);
            setP12Alias(systemConfigInfo.alias || "cms_signer");
            if (systemConfigInfo.keystoreName) {
              setP12FileName(systemConfigInfo.keystoreName);
              // Create preview URL from base64
              try {
                const base64Data = systemConfigInfo.keystoreData.split(',')[1] || systemConfigInfo.keystoreData;
                const byteCharacters = atob(base64Data);
                const byteNumbers = new Array(byteCharacters.length);
                for (let i = 0; i < byteCharacters.length; i++) {
                  byteNumbers[i] = byteCharacters.charCodeAt(i);
                }
                const byteArray = new Uint8Array(byteNumbers);
                const blob = new Blob([byteArray], { type: "application/x-pkcs12" });
                setP12PreviewUrl(URL.createObjectURL(blob));
                // Set size from blob
                setP12Size(blob.size);
              } catch (error) {
                console.error("Error creating P12 certificate preview URL:", error);
              }
            }
          }

          console.log(
            "Config id for system configuration",
            response?.data?.searchList?.[0]?.configId
          );
        } else {
          console.warn("No system configuration found in API response");
        }
      } catch (error) {
        console.error("Failed to fetch DPO details:", error);
        // ✅ Optional: show toast or fallback UI
        // toast.error("Failed to fetch DPO details. Please try again later.");
      } finally {
        setLoading(false);
      }
    };

    fetchSystemConfig();
  }, [dispatch, refresh ]);

  useEffect(() => {
    setArtefactRetention({
      value: artefactInput ? String(artefactInput) : "",
      unit: duration || "YEARS",
    });
  }, [artefactInput, duration]);

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

      {/* P12 Password Modal */}
      {showP12PasswordModal && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: "rgba(0, 0, 0, 0.5)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
          }}
          onClick={handleP12PasswordCancel}
        >
          <div
            style={{
              backgroundColor: "#fff",
              borderRadius: "16px",
              padding: "32px",
              width: "90%",
              maxWidth: "500px",
              boxShadow: "0 20px 60px rgba(0, 0, 0, 0.3)",
              position: "relative",
            }}
            onClick={(e) => e.stopPropagation()}
          >
            {/* Close Button */}
            <button
              onClick={handleP12PasswordCancel}
              style={{
                position: "absolute",
                top: "20px",
                right: "20px",
                background: "none",
                border: "none",
                fontSize: "24px",
                cursor: "pointer",
                color: "#666",
                width: "32px",
                height: "32px",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                borderRadius: "50%",
                transition: "background-color 0.2s",
              }}
              onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = "#F5F5F5")}
              onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = "transparent")}
            >
              <IcClose size={20} />
            </button>

            {/* Modal Title */}
            <Text
              appearance="heading-s"
              color="primary-grey-100"
              style={{
                fontSize: "20px",
                fontWeight: "600",
                marginBottom: "8px",
                display: "block",
              }}
            >
              Enter Certificate Password
            </Text>

            {/* Modal Message */}
            <Text
              appearance="body-s"
              color="primary-grey-80"
              style={{
                fontSize: "14px",
                lineHeight: "20px",
                marginBottom: "24px",
                display: "block",
              }}
            >
              Please enter the password and alias for the P12 certificate file: <strong>{pendingP12File?.name}</strong>
            </Text>

            {/* Alias Input */}
            <div style={{ marginBottom: "20px" }}>
              <InputFieldV2
                type="text"
                label="Alias (Required)"
                placeholder="Enter alphanumeric alias (e.g. cmssigner)"
                value={p12Alias}
                onChange={(e) => setP12Alias(e.target.value.replace(/[^a-zA-Z0-9]/g, ""))}
                size="medium"
              />
            </div>

            {/* Password Input */}
            <div style={{ marginBottom: "32px" }}>
              <InputFieldV2
                type="password"
                label="Password (Required)"
                placeholder="Enter P12 certificate password"
                value={p12Password}
                onChange={(e) => setP12Password(e.target.value)}
                size="medium"
                autoFocus
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    handleP12PasswordSubmit();
                  }
                }}
              />
            </div>

            {/* Action Buttons */}
            <div style={{ display: "flex", justifyContent: "flex-end", gap: "12px" }}>
              <ActionButton
                label="Cancel"
                onClick={handleP12PasswordCancel}
                variant="ghost"
                size="medium"
                style={{
                  fontSize: "14px",
                  fontWeight: "600",
                  padding: "10px 24px",
                  minWidth: "100px",
                }}
              />
              <ActionButton
                label="Submit"
                onClick={handleP12PasswordSubmit}
                variant="primary"
                size="medium"
                style={{
                  fontSize: "14px",
                  fontWeight: "600",
                  padding: "10px 24px",
                  minWidth: "100px",
                }}
              />
            </div>
          </div>
        </div>
      )}
      <div className="configurePage">
        <div className="systemConfig-outer-div">
          <div className="systemConfig-header-and-badge">
            <Text appearance="heading-s" color="primary-grey-100">
            Configuration
            </Text>
            <div className="systemConfig-badge">
              <Text appearance="body-xs-bold" color="primary-grey-80">
                System Configuration
              </Text>
            </div>
          </div>
          <div className="systemConfig-inputs-column-div">
            <div>
              <div className="systemconfigure-input">
                <InputFieldV2
                  className="systemConfig-input"
                  label="Callback URL (Required)"
                  helperText="Callback URL of the system used in APIs, links, callbacks"
                  placeholder="https://example.com/callback"
                  size="medium"
                  value={baseUrl}
                  onChange={handleBaseUrlChange}
                ></InputFieldV2>
              </div>
              <div className="systemconfigure-input">
                <InputFieldV2
                  className="systemConfig-input"
                  label="Default consent expiry (in years)"
                  helperText="Default number of years after which consent expires"
                  placeholder="Enter"
                  size="medium"
                  value={consentExpiry}
                  type="number"
                  onChange={handleConsentExpiryChange}
                ></InputFieldV2>
              </div>
              <div className="systemconfigure-input">
                <InputFieldV2
                  className="systemConfig-input"
                  label="Token (in minutes)"
                  helperText="Time-to-live (TTL) of generated JWT tokens in minutes "
                  placeholder="Enter"
                  size="medium"
                  value={token}
                  type="number"
                  onChange={handleTokenInMinutesChange}
                ></InputFieldV2>
              </div>

              <div>
                <div style={{display:'flex', flexDirection:'row'}}>
                <Text appearance="body-xs" color="primary-grey-80">
                  Upload Certificate
                </Text>
                <div style={{marginLeft:'5px'}} title="Upload PEM, CRT, or CER files only.">
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
                    onChange={handleChange}
                    accept=".pem,.crt,.cer,application/x-pem-file,application/x-x509-ca-cert,application/pkix-cert"
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
                  Drag and drop signing certificate (PEM, CRT, or CER) requested for encryption
                  under 500KB
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
              <br></br>
              <div>
              <div style={{display:'flex', flexDirection:'row'}}>

                <Text appearance="body-xs" color="primary-grey-80">
                  Company’s logo
                </Text>
                <div style={{marginLeft:'5px'}} title="Upload .jpg/.png/.jpeg format only.">
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
                  onClick={handleLogoClick}
                  onDragOver={handleLogoDragOver}
                  onDragLeave={handleLogoDragLeave}
                  onDrop={handleLogoDrop}
                >
                  <input
                    type="file"
                    ref={logoInputRef}
                    style={{ display: "none" }}
                    onChange={handleLogoChange}
                    accept=".png,.jpg,.jpeg,image/png,image/jpeg,image/jpg"
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
                  Drag and drop or upload image under 500KB
                </Text>

                {logoName && (
                  <div className="systemConfiguration-file-uploader">
                    <div className="previewFile" onClick={handlePreviewLogo}>
                      <Icon
                        ic={<IcSuccess width={15} height={15} />}
                        color="feedback_success_50"
                      />
                      <Text appearance="body-xs" color="primary-grey-80">
                        {logoName}
                      </Text>
                    </div>
                    <Icon
                      ic={<IcClose width={15} height={15} />}
                      color="primary_60"
                      className="selecetd-file-display"
                      onClick={handleRemoveLogo}
                    />
                  </div>
                )}
              </div>
              
            </div>
            <div>
              
             
              <div className="systemconfigure-input">
                <InputFieldV2
                  className="systemConfig-input"
                  label="Client ID"
                  helperText="Unique system-generated identifier for the client/application "
                  placeholder="Enter"
                  size="medium"
                  value={businessUniqueId}
                  onChange={handleClientIdChange}
                  readOnly={true}

                ></InputFieldV2>
              </div>
              <div className="systemconfigure-input">
                <InputFieldV2
                  className="systemConfig-input"
                  label="Client secret key"
                  helperText="Secret key used for authentication and API access "
                  placeholder="Enter"
                  size="medium"
                  onChange={handleClientSecretKeyChange}
                  value={consumerSecret}
                  readOnly={true}
                ></InputFieldV2>
              </div>
              <br></br>
              <div className="systemconfigure-input">
                <InputFieldV2
                  className="systemConfig-input"
                  label="Consent request expiry (in years)"
                  helperText="Expiry time for signed consent artefacts"
                  placeholder="Enter"
                  size="medium"
                  value={artifactExpiry}
                  type="number"
                  onChange={handleArtifactExpiryChange}
                  disabled={true}
                ></InputFieldV2>
              </div>
              <br></br>
              <div>
                <div style={{display:'flex', flexDirection:'row'}}>
                  <Text appearance="body-xs" color="primary-grey-80">
                    Upload P12 Certificate (To sign Form 65B)
                  </Text>
                  <div style={{marginLeft:'5px'}} title="Upload .p12 or .pfx files only.">
                    <Icon
                      ic={<IcInfo />}
                      size="small"
                      color="primary_grey_80"
                    />
                  </div>
                </div>
                
                {!isP12UploadAllowed && (
                  <div
                    style={{
                      backgroundColor: "#FEF3C7",
                      border: "1px solid #F59E0B",
                      borderRadius: "8px",
                      padding: "12px",
                      marginBottom: "12px",
                    }}
                  >
                    <Text appearance="body-xs" color="warning-100">
                      P12 certificate upload is only available for Tenant.
                    </Text>
                  </div>
                )}
                
                {/* Drag & Drop area */}
                <div
                  className="fileUploader-custom1"
                  onClick={isP12UploadAllowed ? handleP12Click : undefined}
                  onDragOver={isP12UploadAllowed ? handleP12DragOver : undefined}
                  onDragLeave={isP12UploadAllowed ? handleP12DragLeave : undefined}
                  onDrop={isP12UploadAllowed ? handleP12Drop : undefined}
                  style={{
                    opacity: isP12UploadAllowed ? 1 : 0.5,
                    cursor: isP12UploadAllowed ? "pointer" : "not-allowed",
                    pointerEvents: isP12UploadAllowed ? "auto" : "none",
                  }}
                >
                  <input
                    type="file"
                    ref={p12InputRef}
                    style={{ display: "none" }}
                    onChange={handleP12Change}
                    accept=".p12,.pfx,application/x-pkcs12,application/pkcs12"
                    disabled={!isP12UploadAllowed}
                  />

                  <div className="flex items-center justify-center">
                    <Icon
                      ic={<IcUpload height={23} width={23} />}
                      color={isP12UploadAllowed ? "primary_60" : "primary_grey_60"}
                    />
                    <Text appearance="button" color={isP12UploadAllowed ? "primary-60" : "primary-grey-60"}>
                      Upload
                    </Text>
                  </div>
                </div>

                <Text appearance="body-xs" color={isP12UploadAllowed ? "primary-grey-80" : "primary-grey-60"}>
                  Drag and drop P12 certificate (P12 or PFX) under 500KB
                </Text>

                {p12FileName && (
                  <div className="systemConfiguration-file-uploader">
                    <div
                      onClick={handlePreviewP12}
                      className="previewFile"
                    >
                      <Icon
                        ic={<IcSuccess width={15} height={15} />}
                        color="feedback_success_50"
                      />
                      <Text appearance="body-xs" color="primary-grey-80">
                        {p12FileName}
                      </Text>
                    </div>
                    <Icon
                      ic={<IcClose width={15} height={15} />}
                      color="primary_60"
                      className="selecetd-file-display"
                      onClick={isP12UploadAllowed ? handleRemoveP12 : undefined}
                    />
                  </div>
                )}
              </div>
              <div style={{display:'flex', flexDirection:'row', gap:'20', alignItems:'center'}}>
              <div style={{width: '220%', marginRight:'15px', display:'none'}}>
                <InputFieldV2
                  helperText="Duration for which consent logs are retained"
                  label="Data retention (Required)"
                  name="Name"
                  value={artefactInput}
                  onChange={(e) => handleArtefactChange(e)}
                  placeholder="Enter a number"
                  size="medium"
                  disabled={true}
                  type='number'
                />
              </div>
              <div className="duration-dropdown-group" style={{display:'none'}}>
              <select value={duration} onChange={handleArtefactDuration} disabled={true} id="language-select" >
                <option value="" disabled> Duration </option>
                <option value="DAYS">DAYS</option>
                <option value="MONTHS">MONTHS</option>
                <option value="YEARS">YEARS</option>
              </select>
              </div>
              </div>
            </div>
          </div>
        </div>
        <div className="content-5">
          <ActionButton
            label= "Save"
            onClick={
              updateSystemConfiguration
                ? updateSystemSetUpData
                : SubmitSystemSetUpData
            }
            state={disableSaveBtn ? "disabled" : "normal"}
          ></ActionButton>
        </div>
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
export default SystemConfigure;
