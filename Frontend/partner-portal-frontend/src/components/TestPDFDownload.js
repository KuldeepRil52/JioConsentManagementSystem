import React, { useState } from 'react';
import { ActionButton, InputFieldV2 } from '../custom-components';

/**
 * Test component for PDF download functionality
 * Add this temporarily to test PDF downloads
 */
const TestPDFDownload = () => {
  const [base64Input, setBase64Input] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  const downloadPDF = (base64String, filename = 'test.pdf') => {
    try {
      setMessage('Processing...');

      if (!base64String) {
        throw new Error('No base64 string provided');
      }

      // Clean the base64 string (remove whitespace)
      const cleaned = base64String.replace(/\s/g, '');

      // Decode base64 to binary
      const binaryString = window.atob(cleaned);

      // Check PDF signature
      if (!binaryString.startsWith('%PDF')) {
        throw new Error('Not a valid PDF (missing PDF signature)');
      }

      // Convert to Uint8Array
      const len = binaryString.length;
      const bytes = new Uint8Array(len);
      for (let i = 0; i < len; i++) {
        bytes[i] = binaryString.charCodeAt(i);
      }

      // Create blob and download
      const blob = new Blob([bytes], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      setMessage('✅ PDF downloaded successfully!');
      return true;
    } catch (error) {
      console.error('❌ Error:', error);
      setMessage('❌ Error: ' + error.message);
      return false;
    }
  };

  const handleDownload = () => {
    setLoading(true);
    downloadPDF(base64Input, 'test-form65b.pdf');
    setTimeout(() => setLoading(false), 1000);
  };

  const handleTestWithSample = async () => {
    setLoading(true);
    setMessage('Fetching from API...');

    try {
      // Replace with your actual API endpoint
      const consentId = '9b580a53-ba86-4a20-8f31-1bbcdded499b'; // Replace with actual ID
      const response = await fetch(`${window.location.origin}/api/form65b/${consentId}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          // Add your actual headers here
          'tenantId': 'YOUR_TENANT_ID',
          'businessId': 'YOUR_BUSINESS_ID',
          'x-session-token': 'YOUR_TOKEN'
        }
      });

      if (!response.ok) {
        throw new Error(`API Error: ${response.status}`);
      }

      const data = await response.json();

      if (data.pdfBase64) {
        setBase64Input(data.pdfBase64);
        downloadPDF(data.pdfBase64, 'api-form65b.pdf');
      } else {
        setMessage('❌ No pdfBase64 in response');
      }
    } catch (error) {
      console.error('API Error:', error);
      setMessage('❌ API Error: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: '20px', maxWidth: '800px' }}>
      <h2>PDF Download Test</h2>

      <div style={{ marginBottom: '20px' }}>
        <label>
          <strong>Paste Base64 String:</strong>
        </label>
        <textarea
          value={base64Input}
          onChange={(e) => setBase64Input(e.target.value)}
          placeholder="Paste your base64 PDF string here..."
          style={{
            width: '100%',
            height: '150px',
            padding: '10px',
            marginTop: '10px',
            fontFamily: 'monospace',
            fontSize: '12px'
          }}
        />
      </div>

      <div style={{ display: 'flex', gap: '10px', marginBottom: '20px' }}>
        <ActionButton
          kind="primary"
          label={loading ? 'Processing...' : 'Download PDF'}
          onClick={handleDownload}
          disabled={!base64Input || loading}
        />

        <ActionButton
          kind="secondary"
          label="Test with API"
          onClick={handleTestWithSample}
          disabled={loading}
        />
      </div>

      {message && (
        <div style={{
          padding: '15px',
          backgroundColor: message.includes('✅') ? '#d4edda' : '#f8d7da',
          border: `1px solid ${message.includes('✅') ? '#c3e6cb' : '#f5c6cb'}`,
          borderRadius: '4px',
          color: message.includes('✅') ? '#155724' : '#721c24'
        }}>
          {message}
        </div>
      )}

      <div style={{ marginTop: '20px', padding: '15px', backgroundColor: '#f8f9fa', borderRadius: '4px' }}>
        <strong>Instructions:</strong>
        <ol>
          <li>Get your API response with pdfBase64</li>
          <li>Copy the pdfBase64 value</li>
          <li>Paste it in the textarea above</li>
          <li>Click "Download PDF"</li>
        </ol>
        <p style={{ marginTop: '10px', color: '#666' }}>
          <strong>Or:</strong> Click "Test with API" to fetch directly from your endpoint
        </p>
      </div>
    </div>
  );
};

export default TestPDFDownload;

