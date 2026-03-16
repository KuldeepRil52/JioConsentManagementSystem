import React from "react";
import { InputFieldV2, Text, Icon, InputToggle, ActionButton } from '../custom-components';
import { IcClose, IcSuccess, IcUpload } from '../custom-components/Icon';
import { useState } from "react";
import { useRef } from "react";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import "../styles/branding.css";

const UserDashboardBranding = ({
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
  customColor,
  onColorChange,
  onSaveTheme,
  savingTheme,
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
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
    setFileName("");
    setFileSize(0);
    setFileBase64(null);
    setCertificatePreviewUrl("");
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
          message={"Your document is sucessfully uploaded."}
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

  const validateLogo = async (file) => {
    const allowedTypes = ["image/jpeg", "image/jpg", "image/png"];
    const maxSize = 500 * 1024; // 500KB

    if (!allowedTypes.includes(file.type)) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Only JPG, JPEG, and PNG files are allowed."}
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

    // Validate dimensions for image files
    const dimensionsValid = await validateLogoDimensions(file);
    if (!dimensionsValid) {
      return false;
    }

    toast.success(
      (props) => (
        <CustomToast
          {...props}
          type="success"
          message={"Your Logo is successfully uploaded."}
        />
      ),
      { icon: false }
    );
    return true;
  };

  const handleLogoChange = async (e) => {
    if (e.target.files.length > 0) {
      const file = e.target.files[0];

      const isValid = await validateLogo(file);
      if (isValid) {
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
    if (logoInputRef.current) {
      logoInputRef.current.value = "";
    }
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
      const isValid = await validateLogo(file);
      if (isValid) {
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

      <div className="language-heading" style={{padding:'0px 15px'}}>
        <Text appearance="heading-xxs" color="primary-grey-80">
          Brand color 
        </Text>
      </div>
      <div style={{marginLeft:'10px'}}>
      <Text appearance="body-xs" color="primary-grey-80">
        Selected color will reflect in the navigation menu (left panel) and table headers
        </Text>

      </div>

      <div
        className="container"
        style={{ padding: "0px 15px", marginBottom: "20px", marginTop:'20px' }}
      >
        {/* Left column */}
        <div className="column">
          <div className="section">
            <Text appearance="body-xs" color="primary-grey-80">
              Select color
            </Text>
            <div className="color-input-wrapper">
              <input
                type="color"
                value={customColor || colors.cardBackground || "#FFFFFF"}
                onChange={(e) => {
                  const newColor = e.target.value.toUpperCase();
                  handleColorChange("cardBackground", newColor);
                  // Notify parent component about color change
                  if (onColorChange) {
                    onColorChange(newColor);
                  }
                }}
                className="color-picker"
              />
              <input
                type="text"
                value={customColor || colors.cardBackground || "#FFFFFF"}
                readOnly
                className="hex-input"
              />
            </div>
          </div>
          
          {/* Save Button */}
          <div style={{ marginTop: "20px" }}>
            <ActionButton
              kind="primary"
              size="medium"
              state={savingTheme ? "loading" : "normal"}
              label={savingTheme ? "Saving..." : "Save"}
              onClick={() => {
                if (onSaveTheme) {
                  onSaveTheme(customColor);
                }
              }}
              disabled={!customColor || savingTheme}
            />
          </div>

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
    </div>
  );
};

export default UserDashboardBranding;

