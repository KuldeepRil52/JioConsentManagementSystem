/**
 * EXAMPLE: Secure File Upload Implementation
 * 
 * This is an example showing how to integrate the file validation utility
 * into your existing file upload handlers.
 * 
 * Apply this pattern to ALL file upload components in your project.
 */

import React, { useState, useRef } from 'react';
import { toast } from 'react-toastify';
import CustomToast from './components/CustomToastContainer';
import {
  validatePDFFile,
  validateImageFile,
  validateCertificateFile,
  validateDocumentFile,
  formatValidationErrors
} from './utils/fileValidation';

// ============================================================================
// EXAMPLE 1: PDF Upload (Risk Assessment, Agreement Documents)
// ============================================================================

const SecurePDFUploadExample = () => {
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef(null);

  const handlePDFChange = async (event) => {
    const file = event.target.files?.[0];
    
    if (!file) return;

    // Show loading state
    setUploading(true);

    try {
      // ✅ VALIDATE FILE BEFORE PROCESSING
      const validationResult = await validatePDFFile(file);
      
      if (!validationResult.valid) {
        // Show validation errors to user
        const errorMessage = formatValidationErrors(validationResult);
        
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={errorMessage}
            />
          ),
          { icon: false, autoClose: 5000 }
        );
        
        // Clear the input
        if (fileInputRef.current) {
          fileInputRef.current.value = '';
        }
        
        return;
      }

      // ✅ FILE IS VALID - Proceed with upload
      console.log('✅ PDF validation passed:', {
        originalName: validationResult.fileInfo.name,
        sanitizedName: validationResult.sanitizedFilename,
        size: validationResult.fileInfo.size,
        detectedType: validationResult.fileInfo.detectedType
      });

      // Use the sanitized filename
      setSelectedFile({
        file: file,
        name: validationResult.sanitizedFilename,
        size: validationResult.fileInfo.size
      });

      // Read file as base64 or process as needed
      const reader = new FileReader();
      reader.onload = async (e) => {
        const base64Data = e.target.result;
        
        // Now upload to your API with sanitized filename
        await uploadToAPI({
          fileName: validationResult.sanitizedFilename,
          fileData: base64Data,
          fileType: validationResult.fileInfo.detectedType
        });
      };
      reader.readAsDataURL(file);

    } catch (error) {
      console.error('❌ File validation error:', error);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={`File validation failed: ${error.message}`}
          />
        ),
        { icon: false }
      );
    } finally {
      setUploading(false);
    }
  };

  const handleDrop = async (event) => {
    event.preventDefault();
    const file = event.dataTransfer.files[0];
    
    if (file) {
      // Create a synthetic event to reuse the handler
      await handlePDFChange({ target: { files: [file] } });
    }
  };

  return (
    <div>
      <input
        type="file"
        ref={fileInputRef}
        style={{ display: "none" }}
        accept="application/pdf"
        onChange={handlePDFChange}
        disabled={uploading}
      />
      <button onClick={() => fileInputRef.current?.click()}>
        {uploading ? 'Validating...' : 'Upload PDF'}
      </button>
    </div>
  );
};

// ============================================================================
// EXAMPLE 2: Image Upload (Logo, Company Branding)
// ============================================================================

const SecureImageUploadExample = () => {
  const [logoPreview, setLogoPreview] = useState(null);
  const logoInputRef = useRef(null);

  const handleLogoChange = async (event) => {
    const file = event.target.files?.[0];
    
    if (!file) return;

    try {
      // ✅ VALIDATE IMAGE FILE
      const validationResult = await validateImageFile(file);
      
      if (!validationResult.valid) {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={formatValidationErrors(validationResult)}
            />
          ),
          { icon: false }
        );
        
        if (logoInputRef.current) {
          logoInputRef.current.value = '';
        }
        return;
      }

      // ✅ VALIDATED - Safe to create preview
      const reader = new FileReader();
      reader.onload = (e) => {
        setLogoPreview(e.target.result);
      };
      reader.readAsDataURL(file);

      // Upload to API with sanitized filename
      console.log('✅ Image validated:', validationResult.sanitizedFilename);

    } catch (error) {
      console.error('❌ Image validation error:', error);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="Failed to validate image file"
          />
        ),
        { icon: false }
      );
    }
  };

  return (
    <div>
      <input
        type="file"
        ref={logoInputRef}
        style={{ display: "none" }}
        accept="image/png, image/jpeg, image/jpg"
        onChange={handleLogoChange}
      />
      {logoPreview && <img src={logoPreview} alt="Logo preview" />}
    </div>
  );
};

// ============================================================================
// EXAMPLE 3: Certificate Upload (.pem, .crt, .cer)
// ============================================================================

const SecureCertificateUploadExample = () => {
  const certificateInputRef = useRef(null);

  const handleCertificateChange = async (event) => {
    const file = event.target.files?.[0];
    
    if (!file) return;

    try {
      // ✅ VALIDATE CERTIFICATE FILE
      const validationResult = await validateCertificateFile(file);
      
      if (!validationResult.valid) {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={formatValidationErrors(validationResult)}
            />
          ),
          { icon: false }
        );
        
        if (certificateInputRef.current) {
          certificateInputRef.current.value = '';
        }
        return;
      }

      // ✅ VALIDATED - Proceed with upload
      console.log('✅ Certificate validated:', {
        type: validationResult.fileInfo.detectedType, // PEM or DER
        filename: validationResult.sanitizedFilename
      });

      // Read and upload certificate
      const reader = new FileReader();
      reader.onload = (e) => {
        const certContent = e.target.result;
        // Upload to your API
      };
      reader.readAsText(file);

    } catch (error) {
      console.error('❌ Certificate validation error:', error);
    }
  };

  return (
    <div>
      <input
        type="file"
        ref={certificateInputRef}
        style={{ display: "none" }}
        accept=".crt,.pem,.cer"
        onChange={handleCertificateChange}
      />
    </div>
  );
};

// ============================================================================
// EXAMPLE 4: Document Upload (Grievance attachments)
// ============================================================================

const SecureDocumentUploadExample = () => {
  const fileInputRef = useRef(null);

  const handleDocumentChange = async (event) => {
    const file = event.target.files?.[0];
    
    if (!file) return;

    try {
      // ✅ VALIDATE DOCUMENT FILE
      const validationResult = await validateDocumentFile(file);
      
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
          { icon: false, autoClose: 5000 }
        );
        
        if (fileInputRef.current) {
          fileInputRef.current.value = '';
        }
        return;
      }

      // ✅ VALIDATED - Safe to proceed
      console.log('✅ Document validated:', validationResult);

      // Process the file...

    } catch (error) {
      console.error('❌ Document validation error:', error);
    }
  };

  return (
    <div>
      <input
        type="file"
        ref={fileInputRef}
        style={{ display: "none" }}
        accept=".pdf,.doc,.docx"
        onChange={handleDocumentChange}
      />
    </div>
  );
};

// ============================================================================
// HELPER: Mock API upload function
// ============================================================================

const uploadToAPI = async (fileData) => {
  // Your actual API upload logic here
  console.log('Uploading to API:', fileData.fileName);
  return { success: true };
};

// ============================================================================
// EXPORT EXAMPLES
// ============================================================================

export {
  SecurePDFUploadExample,
  SecureImageUploadExample,
  SecureCertificateUploadExample,
  SecureDocumentUploadExample
};

/**
 * INTEGRATION CHECKLIST:
 * 
 * For EACH file upload component in your project:
 * 
 * 1. ✅ Import the appropriate validation function
 *    - validatePDFFile() for PDFs
 *    - validateImageFile() for images
 *    - validateCertificateFile() for certificates
 *    - validateDocumentFile() for mixed documents
 * 
 * 2. ✅ Call validation BEFORE processing the file
 *    - await validateXXXFile(file)
 *    - Check validationResult.valid
 * 
 * 3. ✅ Handle validation failures
 *    - Show error message using formatValidationErrors()
 *    - Clear the file input
 *    - Return early
 * 
 * 4. ✅ Use sanitized filename
 *    - validationResult.sanitizedFilename
 *    - Never use original file.name for storage
 * 
 * 5. ✅ Add loading states
 *    - Show "Validating..." during async validation
 *    - Disable input while validating
 * 
 * 6. ✅ Test with malicious files
 *    - Try uploading PDFs with JavaScript
 *    - Try uploading images with <script> tags
 *    - Try file type spoofing (rename .exe to .pdf)
 *    - Try oversized files
 *    - Try path traversal filenames (../../etc/passwd)
 */

