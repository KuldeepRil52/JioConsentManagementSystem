import { mockData, getMockDataByUrl } from './sandboxMockData';

// Check if we're in sandbox mode
export const isSandboxMode = () => {
  if (typeof window === 'undefined') return false;

  const urlParams = new URLSearchParams(window.location.search);
  const sandboxParam = urlParams.get('sandbox');

  // Also check localStorage for persistent sandbox flag
  const sandboxStorage = localStorage.getItem('sandboxMode');

  // If URL has sandbox=true, set it in localStorage for this session
  if (sandboxParam === 'true') {
    localStorage.setItem('sandboxMode', 'true');
    return true;
  }

  return sandboxStorage === 'true';
};

// Store original fetch (but don't wrap it yet)
let originalFetch = window.fetch;
let fetchInterceptionActive = false;

// Function to start intercepting fetch
export const startFetchInterception = () => {
  if (fetchInterceptionActive) return; // Already intercepting
  
  originalFetch = window.fetch;
  window.fetch = async (...args) => {
    if (isSandboxMode()) {
      const [url, options = {}] = args;
      const method = options.method || 'GET';
      const body = options.body ? JSON.parse(options.body) : {};
      const headers = options.headers || {};

      // Use sandbox API call
      const mockResponse = await sandboxAPICall(url, method, body, headers);

      // Return a Response-like object
      return {
        ok: mockResponse.status >= 200 && mockResponse.status < 300,
        status: mockResponse.status,
        statusText: mockResponse.status === 200 ? 'OK' : 'Error',
        json: async () => mockResponse.data,
        text: async () => JSON.stringify(mockResponse.data),
        headers: new Headers(),
      };
    }

    return originalFetch(...args);
  };
  fetchInterceptionActive = true;
  console.log('🔒 Fetch interception ENABLED for sandbox mode');
};

// Function to stop intercepting fetch
const stopFetchInterception = () => {
  if (!fetchInterceptionActive) return; // Not intercepting
  
  if (originalFetch) {
    window.fetch = originalFetch;
  }
  fetchInterceptionActive = false;
  console.log('🔓 Fetch interception DISABLED');
};

// Enable sandbox mode
export const enableSandboxMode = (userData = {}) => {
  try {
    localStorage.setItem('sandboxMode', 'true');
    if (userData.name) localStorage.setItem('sandboxUserName', userData.name);
    if (userData.email) localStorage.setItem('sandboxUserEmail', userData.email);
    
    // Start intercepting fetch requests
    startFetchInterception();
  } catch (error) {
    console.error('Failed to enable sandbox mode:', error);
  }
};

// Disable sandbox mode (call when user closes sandbox)
export const disableSandboxMode = () => {
  try {
    // Clear all known sandbox-related localStorage items
    const sandboxLocalStorageKeys = [
      'sandboxMode',
      'sandboxUserName',
      'sandboxUserEmail'
    ];
    
    sandboxLocalStorageKeys.forEach(key => {
      localStorage.removeItem(key);
    });

    // Clear all sandbox-related sessionStorage items (prefixed with 'sandbox_')
    if (typeof sessionStorage !== 'undefined') {
      const sessionStorageKeys = Object.keys(sessionStorage);
      sessionStorageKeys.forEach(key => {
        if (key.startsWith('sandbox_')) {
          sessionStorage.removeItem(key);
        }
      });
    }

    // Clear any other localStorage keys that start with 'sandbox' (case-insensitive)
    // This catches any additional sandbox-related keys that might have been added
    const allLocalStorageKeys = Object.keys(localStorage);
    allLocalStorageKeys.forEach(key => {
      const lowerKey = key.toLowerCase();
      if (lowerKey.startsWith('sandbox')) {
        localStorage.removeItem(key);
      }
    });

    // Stop intercepting fetch requests
    stopFetchInterception();

    console.log('✅ Sandbox mode disabled - All sandbox data cleared from storage');
  } catch (error) {
    console.error('❌ Error clearing sandbox storage:', error);
  }
};

// Get sandbox user data
export const getSandboxUserData = () => {
  return {
    name: localStorage.getItem('sandboxUserName') || 'Demo User',
    email: localStorage.getItem('sandboxUserEmail') || 'demo@sandbox.com'
  };
};

// Mock API call interceptor
export const sandboxAPICall = async (url, method, body, headers) => {
  // Simulate network delay
  await new Promise(resolve => setTimeout(resolve, 300 + Math.random() * 200));


  // Get appropriate mock data based on URL and method
  let mockResponse = getMockDataByUrl(url, method, body);

  // Handle specific cases - only if mockResponse wasn't already set by getMockDataByUrl
  if (url.includes('translate') || url.includes('translation')) {
    // For translations, return input as output (no actual translation in sandbox)
    const inputTexts = body?.input || [];
    mockResponse = {
      status: 200,
      data: {
        output: inputTexts.map(item => ({
          id: item.id,
          source: item.source,
          target: item.source  // In sandbox, just return source as target
        }))
      }
    };
  } else if (!mockResponse || !mockResponse.data) {
    // Only apply generic handlers if no specific mock was found
    if (method === 'POST') {
      // For create operations, return success with generated ID
      mockResponse = {
        status: 201,
        data: {
          ...mockData.createSuccess.data,
          ...body,
          id: `generated-${Date.now()}`,
          createdAt: new Date().toISOString()
        }
      };
    } else if (method === 'PUT') {
      // For update operations
      mockResponse = {
        status: 200,
        data: {
          ...mockData.updateSuccess.data,
          ...body
        }
      };
    } else if (method === 'DELETE') {
      mockResponse = mockData.deleteSuccess;
    }
  }

  // If no specific mock found, return default success
  if (!mockResponse) {
    mockResponse = {
      status: 200,
      data: {
        message: 'Success',
        searchList: []
      }
    };
  }

  // Return promise that resolves like axios
  return Promise.resolve(mockResponse);
};

// Sandbox-aware localStorage/sessionStorage wrapper
export const sandboxStorage = {
  setItem: (key, value) => {
    if (isSandboxMode()) {
      sessionStorage.setItem(`sandbox_${key}`, value);
    } else {
      localStorage.setItem(key, value);
    }
  },

  getItem: (key) => {
    if (isSandboxMode()) {
      return sessionStorage.getItem(`sandbox_${key}`);
    } else {
      return localStorage.getItem(key);
    }
  },

  removeItem: (key) => {
    if (isSandboxMode()) {
      sessionStorage.removeItem(`sandbox_${key}`);
    } else {
      localStorage.removeItem(key);
    }
  },

  clear: () => {
    if (isSandboxMode()) {
      // Clear only sandbox items
      Object.keys(sessionStorage).forEach(key => {
        if (key.startsWith('sandbox_')) {
          sessionStorage.removeItem(key);
        }
      });
    } else {
      localStorage.clear();
    }
  }
};

// Helper to wrap actions with sandbox reminder
// Usage: onClick={withSandboxReminder(() => handleSave())}
export const withSandboxReminder = (action) => {
  return (...args) => {
    if (isSandboxMode()) {
      // Dynamically import to avoid circular dependencies
      import('./sandboxInit').then(({ showSandboxActionReminder }) => {
        showSandboxActionReminder();
      });
    }
    // Execute the original action
    return action(...args);
  };
};

