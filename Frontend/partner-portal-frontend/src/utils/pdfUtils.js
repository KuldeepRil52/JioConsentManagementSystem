/**
 * Utility functions for PDF operations
 */

/**
 * Downloads a PDF from a base64 string
 * @param {string} base64String - The base64 encoded PDF data
 * @param {string} filename - The name for the downloaded file (default: 'form65b.pdf')
 */
export const downloadPDFFromBase64 = (base64String, filename = 'form65b.pdf') => {
  try {

    // Validate input
    if (!base64String) {
      console.error('❌ No base64 string provided');
      return { success: false, message: 'No PDF data provided' };
    }

    if (typeof base64String !== 'string') {
      console.error('❌ base64String is not a string:', typeof base64String);
      return { success: false, message: 'Invalid PDF data format' };
    }


    // Remove any data URL prefix if present and clean whitespace
    let base64Data = base64String
      .replace(/^data:application\/pdf;base64,/, '')
      .replace(/\s/g, ''); // Remove all whitespace including newlines


    // Validate base64 string format
    const base64Pattern = /^[A-Za-z0-9+/]*={0,2}$/;
    if (!base64Pattern.test(base64Data)) {
      console.error('❌ Invalid base64 format detected');
      return { success: false, message: 'Invalid base64 format' };
    }


    // Convert base64 to binary
    let binaryString;
    try {
      binaryString = window.atob(base64Data);
    } catch (atobError) {
      console.error('❌ atob() failed:', atobError);
      return { success: false, message: 'Failed to decode base64 data', error: atobError };
    }

    // Check if it's a valid PDF (should start with %PDF)
    if (!binaryString.startsWith('%PDF')) {
      console.error('❌ Data does not appear to be a PDF. First 10 bytes:', binaryString.substring(0, 10));
      return { success: false, message: 'Data is not a valid PDF' };
    }


    // Convert to Uint8Array
    const len = binaryString.length;
    const bytes = new Uint8Array(len);

    for (let i = 0; i < len; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }


    // Create blob from binary data
    const blob = new Blob([bytes], { type: 'application/pdf' });

    // Create download link
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.style.display = 'none';

    // Trigger download
    document.body.appendChild(link);
    link.click();

    // Cleanup with a slight delay to ensure download starts
    setTimeout(() => {
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    }, 100);

    return { success: true, message: 'PDF downloaded successfully' };
  } catch (error) {
    console.error('❌ Unexpected error downloading PDF:', error);
    console.error('Error stack:', error.stack);
    return { success: false, message: 'Failed to download PDF: ' + error.message, error };
  }
};

/**
 * Opens a PDF from base64 string in a new window
 * @param {string} base64String - The base64 encoded PDF data
 */
export const openPDFInNewTab = (base64String) => {
  try {
    // Remove any data URL prefix if present
    const base64Data = base64String.replace(/^data:application\/pdf;base64,/, '');

    // Convert base64 to binary
    const binaryString = window.atob(base64Data);
    const len = binaryString.length;
    const bytes = new Uint8Array(len);

    for (let i = 0; i < len; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }

    // Create blob from binary data
    const blob = new Blob([bytes], { type: 'application/pdf' });

    // Create URL and open in new tab
    const url = window.URL.createObjectURL(blob);
    window.open(url, '_blank');

    // Note: We don't revoke the URL immediately as it needs to stay valid for the new tab
    // The browser will clean it up when the tab is closed

    return { success: true, message: 'PDF opened in new tab' };
  } catch (error) {
    console.error('Error opening PDF:', error);
    return { success: false, message: 'Failed to open PDF', error };
  }
};

/**
 * Validates if a base64 string appears to be a valid PDF
 * @param {string} base64String - The base64 encoded string to validate
 * @returns {boolean} - True if the string appears to be a valid PDF
 */
export const isValidPDFBase64 = (base64String) => {
  try {
    if (!base64String || typeof base64String !== 'string') {
      return false;
    }

    // Remove any data URL prefix if present
    const base64Data = base64String.replace(/^data:application\/pdf;base64,/, '');

    // Decode the first few bytes to check PDF header
    const binaryString = window.atob(base64Data.substring(0, 20));

    // PDF files start with '%PDF'
    return binaryString.startsWith('%PDF');
  } catch (error) {
    console.error('Error validating PDF base64:', error);
    return false;
  }
};

/**
 * Alternative method using data URI (better for large files)
 * @param {string} base64String - The base64 encoded PDF data
 * @param {string} filename - The name for the downloaded file
 */
export const downloadPDFFromBase64_DataURI = (base64String, filename = 'form65b.pdf') => {
  try {

    if (!base64String) {
      return { success: false, message: 'No PDF data provided' };
    }

    // Clean the base64 string
    let base64Data = base64String
      .replace(/^data:application\/pdf;base64,/, '')
      .replace(/\s/g, '');

    // Create data URI
    const dataURI = `data:application/pdf;base64,${base64Data}`;

    // Create download link
    const link = document.createElement('a');
    link.href = dataURI;
    link.download = filename;
    link.style.display = 'none';

    // Trigger download
    document.body.appendChild(link);
    link.click();

    // Cleanup
    setTimeout(() => {
      document.body.removeChild(link);
    }, 100);

    return { success: true, message: 'PDF downloaded successfully' };
  } catch (error) {
    console.error('❌ Error with Data URI method:', error);
    return { success: false, message: 'Failed to download PDF', error };
  }
};

/**
 * Smart download function - tries multiple methods
 * @param {string} base64String - The base64 encoded PDF data
 * @param {string} filename - The name for the downloaded file
 */
export const smartDownloadPDF = (base64String, filename = 'form65b.pdf') => {

  // Try method 1: Blob method (better for most browsers)
  const blobResult = downloadPDFFromBase64(base64String, filename);
  if (blobResult.success) {
    return blobResult;
  }


  // Try method 2: Data URI method (fallback)
  const dataURIResult = downloadPDFFromBase64_DataURI(base64String, filename);
  if (dataURIResult.success) {
    return dataURIResult;
  }

  console.error('❌ All download methods failed');
  return { success: false, message: 'Failed to download PDF using all available methods' };
};

/**
 * Generates filename from consent data
 * @param {object} data - Response data from form65b API
 * @returns {string} - Generated filename
 */
export const generateForm65bFilename = (data) => {
  const {
    consentId = 'unknown',
    referenceId = '',
    overallStatus = ''
  } = data || {};

  const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
  const status = overallStatus ? overallStatus.toLowerCase() : 'unknown';

  return `form65b_${consentId.substring(0, 8)}_${status}_${timestamp}.pdf`;
};

