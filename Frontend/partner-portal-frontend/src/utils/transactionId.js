/**
 * Generates a UUID v4 format transaction ID
 * This implementation works in all environments
 * 
 * @returns {string} A unique transaction ID in UUID v4 format
 */
const uuidv4 = () => {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0,
      v = c == "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

/**
 * Safely generates a transaction ID (UUID v4 format)
 * Tries to use crypto.randomUUID first, falls back to custom UUID v4 implementation
 * 
 * @returns {string} A unique transaction ID in UUID v4 format
 */
export const generateTransactionId = () => {
  // Check if crypto and randomUUID are available
  if (typeof crypto !== 'undefined' && 
      crypto.randomUUID && 
      typeof crypto.randomUUID === 'function') {
    try {
      return crypto.randomUUID();
    } catch (error) {
      console.warn('crypto.randomUUID() failed, using fallback:', error);
    }
  }
  
  // Fallback: Use custom UUID v4 implementation
  return uuidv4();
};

