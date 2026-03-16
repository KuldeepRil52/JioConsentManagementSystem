import React from "react";
import { InputFieldV2, Text, Icon, InputToggle } from "../custom-components";
import { IcClose, IcSuccess, IcUpload } from "../custom-components/Icon";
import { useState } from "react";
import { useRef } from "react";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import "../styles/branding.css";
import { validateImageFile, formatValidationErrors } from "../utils/fileValidation";

const FormBranding = ({
  fileName,
  setFileName,
  certificatePreviewUrl,
  setCertificatePreviewUrl,
  fileInputRef,
  fileBase64,
  setFileBase64,
  logoPreviewUrl,
  setLogoPreviewUrl,
  logoName,
  setLogoName,
  logoInputRef,
  logoBase64,
  setLogoBase64,
  fileSize,
  setFileSize,
  darkMode,
  setDarkMode,
  mobileView,
  setMobileView,
  parentalControl,
  setParentalControl,
  dataTypeToBeShown,
  setDataTypeToBeShown,
  dataItemToBeShown,
  setDataItemToBeShown,
  processActivityNameToBeShown,
  setProcessActivityNameToBeShown,
  processorNameToBeShown,
  setProcessorNameToBeShown,
  validitytoBeShown,
  setValiditytoBeShown,
  colors,
  setColors,
  darkColors,
  setDarkColors,
}) => {
  const handleDarkMode = () => {
    setDarkMode((prev) => !prev);
  };

  const handleColorChange = (key, value) => {
    setColors((prev) => ({ ...prev, [key]: value }));
  };

  const handleDarkColorChange = (key, value) => {
    setDarkColors((prev) => ({ ...prev, [key]: value }));
  };

  const convertToBase64 = (file) => {
    const reader = new FileReader();
    reader.readAsDataURL(file); // reads file as base64
    reader.onload = () => {
      setFileBase64(reader.result); // base64 string here
    };
    reader.onerror = (error) => {
      console.log("Error converting file:", error);
    };
  };

  const logoToBase64 = (file) => {
    const reader = new FileReader();
    reader.readAsDataURL(file); // reads file as base64
    reader.onload = () => {
      setLogoBase64(reader.result); // base64 string here
    };
    reader.onerror = (error) => {
      console.log("Error converting logo:", error);
    };
  };

  const handleClick = () => {
    fileInputRef.current.click(); // trigger hidden input
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

  const handleDrop = (e) => {
    e.preventDefault();
    e.currentTarget.classList.remove("dragover");

    if (e.dataTransfer.files.length > 0) {
      const file = e.dataTransfer.files[0];
      if (validateFile(file)) {
        fileInputRef.current.files = e.dataTransfer.files;
        setFileName(file.name);
        setCertificatePreviewUrl(URL.createObjectURL(file));
        convertToBase64(file);
      } else {
        fileInputRef.current.value = "";
        setFileName("");
        setCertificatePreviewUrl("");
      }
    }
  };

  const handleRemoveFile = () => {
    setFileName("");
    // also clear the input value
    //document.getElementById("fileInput").value = "";
  };

  const validateFile = (file) => {
    const allowedTypes = ["application/pdf"];
    const maxSize = 500 * 1024; // 500KB

    if (!allowedTypes.includes(file.type)) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Only PDF files are allowed."}
          />
        ),
        { icon: false }
      );
      return false;
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
      return false;
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
    return true;
  };

  const handleChange = (e) => {
    if (e.target.files.length > 0) {
      const file = e.target.files[0];
      if (validateFile(file)) {
        setFileName(file.name);
        setFileSize(file.size);
        convertToBase64(file);
        setCertificatePreviewUrl(URL.createObjectURL(file));
      } else {
        e.target.value = ""; // clear invalid file
        setFileName("");
        setCertificatePreviewUrl("");
      }
    }
  };

  const handleLogoClick = () => {
    logoInputRef.current.click();
  };
  
  const validateLogoDimensions = (file) => {
    return new Promise((resolve) => {
      const img = new Image();
      const objectUrl = URL.createObjectURL(file);
      
      img.onload = () => {
        URL.revokeObjectURL(objectUrl);
        const { width, height } = img;
        
        // Check minimum dimensions
        if (width < 100 || height < 100) {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={`Image too small. Minimum dimensions: 100x100px. Your image: ${width}x${height}px`}
              />
            ),
            { icon: false }
          );
          resolve(false);
          return;
        }
        
        // Check maximum dimensions
        if (width > 1000 || height > 1000) {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={`Image too large. Maximum dimensions: 1000x1000px. Your image: ${width}x${height}px`}
              />
            ),
            { icon: false }
          );
          resolve(false);
          return;
        }
        
        // Check aspect ratio (warn if not square)
        const aspectRatio = width / height;
        if (aspectRatio < 0.9 || aspectRatio > 1.1) {
          toast.warning(
            (props) => (
              <CustomToast
                {...props}
                type="warning"
                message={`Recommended: Square images (1:1 ratio) for best circular display. Your image: ${width}x${height}px`}
              />
            ),
            { icon: false }
          );
          // Still allow but show warning
        }
        
        resolve(true);
      };
      
      img.onerror = () => {
        URL.revokeObjectURL(objectUrl);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Failed to load image. Please try another file."}
            />
          ),
          { icon: false }
        );
        resolve(false);
      };
      
      img.src = objectUrl;
    });
  };
  
  // ✅ SECURE: Image validation with XSS detection + dimension check
  const validateLogo = async (file) => {
    console.log("file.type", file.type);
    
    try {
      // First, run secure validation (magic numbers, XSS detection, size check)
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
      
      // Then, validate dimensions for logo-specific requirements
      const dimensionsValid = await validateLogoDimensions(file);
      if (!dimensionsValid) {
        return false;
      }

      // ✅ Validation passed - don't show toast yet, wait for actual upload success
      return true;
    } catch (error) {
      console.error('Logo validation error:', error);
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

  const handleLogoChange = async (e) => {
    if (e.target.files.length > 0) {
      const file = e.target.files[0];

      const validationResult = await validateLogo(file);
      if (validationResult) {  // validateLogo returns boolean, not object
        setLogoName(file.name);
        logoToBase64(file);
        setLogoPreviewUrl(URL.createObjectURL(file));
      } else {
        setLogoName("");
        setLogoPreviewUrl("");
        e.target.value = ""; // Clear the input
      }
    }
  };

  const handlePreviewLogo = () => {
    if (logoPreviewUrl) {
      window.open(logoPreviewUrl, "_blank");
    }
  };

  const handleRemoveLogo = () => {
    setLogoName("");
    setLogoPreviewUrl("");
  };

  const handleLogoDragOver = (e) => {
    e.preventDefault();
    e.currentTarget.classList.add("dragover");
  };

  const handleLogoDragLeave = (e) => {
    e.preventDefault();
    e.currentTarget.classList.remove("dragover");
  };

  const handleLogoDrop = async (e) => {
    e.preventDefault();
    e.currentTarget.classList.remove("dragover");

    if (e.dataTransfer.files.length > 0) {
      const file = e.dataTransfer.files[0];
      const validationResult = await validateLogo(file);
      if (validationResult) {  // validateLogo returns boolean, not object
        logoInputRef.current.files = e.dataTransfer.files;
        setLogoName(file.name);
        logoToBase64(file);
        setLogoPreviewUrl(URL.createObjectURL(file));
      } else {
        setLogoName("");
        setLogoPreviewUrl("");
      }
    }
  };

  return (
    <div className="purpose-con">
      <div className="language-heading-upload">
        <Text appearance="heading-xxs" color="primary-grey-80">
          Upload app logo
        </Text>
      </div>

      <div
        style={{
          padding: "0px 15px",
          marginBottom: "20px",
        }}
      >
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
            accept="image/png, image/jpeg, image/jpg"

          />

          <div className="flex items-center justify-center">
            <Icon ic={<IcUpload height={23} width={23} />} color="primary_60" />
            <Text appearance="button" color="primary-60">
              Upload
            </Text>
          </div>
        </div>

        <Text appearance="body-xs" color="primary-grey-80">
          Drag and drop or upload image under 500KB. Formats: PNG, JPEG, JPG. 
          Dimensions: 100x100px to 1000x1000px (Square recommended for best display).
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

      <div className="language-heading">
        <Text appearance="heading-xxs" color="primary-grey-80">
          Privacy centre theme
        </Text>
      </div>

      <div
        className="container"
        style={{ padding: "0px 15px", marginBottom: "20px" }}
      >
        {/* Left column */}
        <div className="column">
          <div className="section">
            <Text appearance="body-xs" color="primary-grey-80">
              Card background
            </Text>
            <div className="color-input-wrapper">
              <input
                type="color"
                value={colors.cardBackground}
                onChange={(e) =>
                  handleColorChange("cardBackground", e.target.value)
                }
                className="color-picker"
              />
              <input
                type="text"
                value={colors.cardBackground}
                readOnly
                className="hex-input"
              />
            </div>
          </div>

          <div className="section">
            <Text appearance="body-xs" color="primary-grey-80">
              Button background
            </Text>
            <div className="color-input-wrapper">
              <input
                type="color"
                value={colors.buttonBackground}
                onChange={(e) =>
                  handleColorChange("buttonBackground", e.target.value)
                }
                className="color-picker"
              />
              <input
                type="text"
                value={colors.buttonBackground}
                readOnly
                className="hex-input"
              />
            </div>
          </div>

          {/* <div className="section">
            <Text appearance="body-xs" color="primary-grey-80">
              Link font
            </Text>
            <div className="color-input-wrapper">
              <input
                type="color"
                value={colors.linkFont}
                onChange={(e) => handleColorChange("linkFont", e.target.value)}
                className="color-picker"
              />
              <input
                type="text"
                value={colors.linkFont}
                readOnly
                className="hex-input"
              />
            </div>
          </div> */}
        </div>

        {/* Right column */}
        <div className="column">
          <div className="section">
            <Text appearance="body-xs" color="primary-grey-80">
              Card font
            </Text>
            <div className="color-input-wrapper">
              <input
                type="color"
                value={colors.cardFont}
                onChange={(e) => handleColorChange("cardFont", e.target.value)}
                className="color-picker"
              />
              <input
                type="text"
                value={colors.cardFont}
                readOnly
                className="hex-input"
              />
            </div>
          </div>

          <div className="section">
            <Text appearance="body-xs" color="primary-grey-80">
              Button font
            </Text>
            <div className="color-input-wrapper">
              <input
                type="color"
                value={colors.buttonFont}
                onChange={(e) =>
                  handleColorChange("buttonFont", e.target.value)
                }
                className="color-picker"
              />
              <input
                type="text"
                value={colors.buttonFont}
                readOnly
                className="hex-input"
              />
            </div>
          </div>
        </div>
      </div>

      <div className="language-heading">
        <Text appearance="heading-xxs" color="primary-grey-80">
          Supporting data theme
        </Text>
      </div>

      {/* <div
        className="toggle-switch"
        style={{ padding: "0px 15px", marginBottom: "20px" }}
      >
        <Text appearance="body-s" color="primary-grey-80">
          Enable custom dark mode theme
        </Text>
        <InputToggle
          checked={darkMode}
          labelPosition="left"
          onChange={handleDarkMode}
          size="medium"
          type="toggle"
        />
      </div> */}

      <div
        className="container"
        style={{
          padding: "0px 15px",
          marginBottom: "20px",
          opacity:  1,
          pointerEvents: darkMode ? "auto" : "none",
        }}
      >
        {/* Left column */}
        <div className="column">
          <div className="section">
            <Text appearance="body-xs" color="primary-grey-80">
              Card background
            </Text>
            <div className="color-input-wrapper">
              <input
                type="color"
                value={darkColors.cardBackground}
                onChange={(e) =>
                  handleDarkColorChange("cardBackground", e.target.value)
                }
                className="color-picker"
              />
              <input
                type="text"
                value={darkColors.cardBackground}
                readOnly
                className="hex-input"
              />
            </div>
          </div>

          <div className="section">
            <Text appearance="body-xs" color="primary-grey-80">
              Button background
            </Text>
            <div className="color-input-wrapper">
              <input
                type="color"
                value={darkColors.buttonBackground}
                onChange={(e) =>
                  handleDarkColorChange("buttonBackground", e.target.value)
                }
                className="color-picker"
              />
              <input
                type="text"
                value={darkColors.buttonBackground}
                readOnly
                className="hex-input"
              />
            </div>
          </div>

          {/* <div className="section">
            <Text appearance="body-xs" color="primary-grey-80">
              Link font
            </Text>
            <div className="color-input-wrapper">
              <input
                type="color"
                value={darkColors.linkFont}
                onChange={(e) =>
                  handleDarkColorChange("linkFont", e.target.value)
                }
                className="color-picker"
              />
              <input
                type="text"
                value={darkColors.linkFont}
                readOnly
                className="hex-input"
              />
            </div>
          </div> */}
        </div>

        {/* Right column */}
        <div className="column">
          <div className="section">
            <Text appearance="body-xs" color="primary-grey-80">
              Card font
            </Text>
            <div className="color-input-wrapper">
              <input
                type="color"
                value={darkColors.cardFont}
                onChange={(e) =>
                  handleDarkColorChange("cardFont", e.target.value)
                }
                className="color-picker"
              />
              <input
                type="text"
                value={darkColors.cardFont}
                readOnly
                className="hex-input"
              />
            </div>
          </div>

          <div className="section">
            <Text appearance="body-xs" color="primary-grey-80">
              Button font
            </Text>
            <div className="color-input-wrapper">
              <input
                type="color"
                value={darkColors.buttonFont}
                onChange={(e) =>
                  handleDarkColorChange("buttonFont", e.target.value)
                }
                className="color-picker"
              />
              <input
                type="text"
                value={darkColors.buttonFont}
                readOnly
                className="hex-input"
              />
            </div>
          </div>
        </div>
      </div>

      {/* <div className="brand-controls">
        <div>
          <Text appearance="heading-xxs" color="primary-grey-80">
            Parental Control
          </Text>
          <div className="toggle-switch">
            <Text appearance="body-s" color="primary-grey-80">
              Enable age verification
            </Text>
            <InputToggle
              checked={parentalControl}
              labelPosition="left"
              onChange={() => setParentalControl((prev) => !prev)}
              size="medium"
              type="toggle"
            />
          </div>
        </div>
        <div>
          <Text appearance="heading-xxs" color="primary-grey-80">
            Data type
          </Text>
          <div className="toggle-switch">
            <Text appearance="body-s" color="primary-grey-80">
              Allow users to see data type
            </Text>
            <InputToggle
              checked={dataTypeToBeShown}
              labelPosition="left"
              onChange={() => setDataTypeToBeShown((prev) => !prev)}
              size="medium"
              type="toggle"
            />
          </div>
        </div>
        <div>
          <Text appearance="heading-xxs" color="primary-grey-80">
            Data item
          </Text>
          <div className="toggle-switch">
            <Text appearance="body-s" color="primary-grey-80">
              Allow users to see data item
            </Text>
            <InputToggle
              checked={dataItemToBeShown}
              labelPosition="left"
              onChange={() => setDataItemToBeShown((prev) => !prev)}
              size="medium"
              type="toggle"
            />
          </div>
        </div>
        <div>
          <Text appearance="heading-xxs" color="primary-grey-80">
            Processing activity
          </Text>
          <div className="toggle-switch">
            <Text appearance="body-s" color="primary-grey-80">
              Allow users to see activity name
            </Text>
            <InputToggle
              checked={processActivityNameToBeShown}
              labelPosition="left"
              onChange={() => setProcessActivityNameToBeShown((prev) => !prev)}
              size="medium"
              type="toggle"
            />
          </div>
        </div>
        <div>
          <Text appearance="heading-xxs" color="primary-grey-80">
            Processor name
          </Text>
          <div className="toggle-switch">
            <Text appearance="body-s" color="primary-grey-80">
              Allow users to see data processor
            </Text>
            <InputToggle
              checked={processorNameToBeShown}
              labelPosition="left"
              onChange={() => setProcessorNameToBeShown((prev) => !prev)}
              size="medium"
              type="toggle"
            />
          </div>
        </div>
        <div>
          <Text appearance="heading-xxs" color="primary-grey-80">
            Validity
          </Text>
          <div className="toggle-switch">
            <Text appearance="body-s" color="primary-grey-80">
              Allow users to see validity
            </Text>
            <InputToggle
              checked={validitytoBeShown}
              labelPosition="left"
              onChange={() => setValiditytoBeShown((prev) => !prev)}
              size="medium"
              type="toggle"
            />
          </div>
        </div>
      </div> */}

      {/* <div className="language-heading-upload">
        <Text appearance="heading-xxs" color="primary-grey-80">
          Upload privacy policy
        </Text>
      </div> */}

    </div>
  );
};

export default FormBranding;
