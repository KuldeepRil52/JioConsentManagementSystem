/**
 * Debug test utility for PDF download
 * Use this in your browser console to test the PDF download
 */

export const testPDFDownload = (responseData) => {

  // Step 1: Check if responseData exists
  if (!responseData) {
    console.error('❌ No response data provided!');
    return false;
  }

  // Step 2: Check if pdfBase64 exists
  if (!responseData.pdfBase64) {
    console.error('❌ No pdfBase64 field in response!');
    console.log('Available fields:', Object.keys(responseData));
    return false;
  }

  // Step 3: Clean the base64 string
  const cleaned = responseData.pdfBase64.replace(/\s/g, '');

  // Step 4: Test base64 decoding
  try {
    const decoded = window.atob(cleaned.substring(0, 100));

    // Check PDF signature
    if (decoded.startsWith('%PDF')) {
      console.log('✅ Valid PDF signature detected');
    } else {
      console.error('❌ Invalid PDF signature. Expected %PDF, got:', decoded.substring(0, 10));
    }
  } catch (e) {
    console.error('❌ Base64 decoding failed:', e.message);
    return false;
  }

  // Step 5: Try full decode
  try {
    const fullDecoded = window.atob(cleaned);
  } catch (e) {
    console.error('❌ Full decode failed:', e.message);
    return false;
  }

  // Step 6: Try creating blob
  try {
    const binaryString = window.atob(cleaned);
    const len = binaryString.length;
    const bytes = new Uint8Array(len);
    for (let i = 0; i < len; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }
    const blob = new Blob([bytes], { type: 'application/pdf' });
  } catch (e) {
    console.error('❌ Blob creation failed:', e.message);
    return false;
  }

  return true;
};

/**
 * Run this in browser console to test with your actual data
 */
export const runQuickTest = () => {
  console.log('To test, call: testPDFDownload(yourResponseData)');
  console.log('Example:');
  console.log('const data = { pdfBase64: "JVBERi0x..." };');
  console.log('testPDFDownload(data);');
};

// Make it available globally for console testing
if (typeof window !== 'undefined') {
  window.testPDFDownload = testPDFDownload;
  window.runQuickTest = runQuickTest;
}

