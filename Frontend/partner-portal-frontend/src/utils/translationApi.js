import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';
import config from './config';
import { store } from '../store/store';

const TRANSLATION_API_URL = config.translator_translate;

// Original function - kept for backward compatibility
export const fetchTranslations = async (sourceLanguage, targetLanguage, sourceContext, inputTexts) => {
  try {
    // Get credentials from Redux store
    const state = store.getState();
    const tenant_id = state?.common?.tenant_id;
    const business_id = state?.common?.business_id;
    const session_token = state?.common?.session_token;

    // Build headers
    const headers = {
      'Content-Type': 'application/json',
    };

    // Add tenant and business IDs if available
    if (tenant_id) {
      headers['tenantid'] = tenant_id;
    }
    if (business_id) {
      headers['businessid'] = business_id;
    }
    
    // Add transaction ID
    headers['txn'] = uuidv4();
    
    // Add session token if available
    if (session_token) {
      headers['x-session-token'] = session_token;
    }

    const response = await axios.post(TRANSLATION_API_URL, {
      provider: 'BHASHINI',
      source: sourceContext,
      language: {
        sourceLanguage: sourceLanguage,
        targetLanguage: targetLanguage,
      },
      input: inputTexts,
    }, {
      headers,
    });
    
    // Ensure the response has the expected structure
    const responseData = response.data;
    if (responseData && responseData.output) {
      return responseData;
    }
    
    // If response doesn't have output property, return fallback structure
    console.warn('Translation API response missing output property, using fallback');
    return {
      output: (inputTexts || []).map(item => ({ id: item.id, target: item.source }))
    };
  } catch (error) {
    console.error('Error fetching translations:', error);
    // Return fallback structure instead of throwing
    return {
      output: (inputTexts || []).map(item => ({ id: item.id, target: item.source }))
    };
  }
};

// Adapter with the signature used by components
export const getTranslations = async (
  inputTexts,
  targetLanguage,
  sourceLanguage = 'en',
  sourceContext = 'CONSENTPOPUP'
) => {
  // Short-circuit if no translation needed
  if (!targetLanguage || targetLanguage.toLowerCase() === sourceLanguage.toLowerCase()) {
    return {
      output: (inputTexts || []).map(item => ({ id: item.id, target: item.source }))
    };
  }

  // Delegate to the original function to keep behavior consistent
  return await fetchTranslations(sourceLanguage, targetLanguage, sourceContext, inputTexts);
};