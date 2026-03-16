/**
 * Centralized date/time utility functions
 * All timestamps in the application should use IST (Indian Standard Time)
 */

/**
 * Format timestamp to IST with format: DD-MM-YYYY HH:mm:ss
 * @param {string|number|Date} timestamp - The timestamp to format
 * @returns {string} Formatted timestamp in IST or 'N/A' if invalid
 */
export const formatToIST = (timestamp) => {
  if (!timestamp) return 'N/A';
  
  try {
    let date;
    
    // Handle different input types
    if (timestamp instanceof Date) {
      date = timestamp;
    } else if (typeof timestamp === 'number') {
      // Handle both seconds and milliseconds timestamps
      const ms = timestamp < 1e12 ? timestamp * 1000 : timestamp;
      date = new Date(ms);
    } else if (typeof timestamp === 'string') {
      // Handle string timestamps
      const trimmed = timestamp.trim();
      
      // Check if it's a numeric string
      if (/^\d{10,13}$/.test(trimmed)) {
        const n = Number(trimmed);
        const ms = n < 1e12 ? n * 1000 : n;
        date = new Date(ms);
      } else {
        // Check if timestamp already has timezone info (Z or +/-HH:MM)
        const hasTimezone = trimmed.endsWith('Z') || /[+-]\d{2}:\d{2}$/.test(trimmed);
        
        if (hasTimezone) {
          // Already has timezone, use as is
          date = new Date(trimmed);
        } else {
          // No timezone, append 'Z' to treat as UTC
          date = new Date(trimmed + 'Z');
        }
      }
    } else {
      return 'N/A';
    }
    
    // Check if date is valid
    if (isNaN(date.getTime())) {
      return 'N/A';
    }
    
    // Format to IST with DD-MM-YYYY HH:mm:ss
    const formatter = new Intl.DateTimeFormat('en-IN', {
      timeZone: 'Asia/Kolkata',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    });
    
    const parts = formatter.formatToParts(date);
    const getValue = (type) => parts.find(p => p.type === type)?.value || '';
    
    const day = getValue('day');
    const month = getValue('month');
    const year = getValue('year');
    const hour = getValue('hour');
    const minute = getValue('minute');
    const second = getValue('second');
    
    return `${day}-${month}-${year} ${hour}:${minute}:${second}`;
  } catch (error) {
    console.error('Error formatting timestamp:', error);
    return 'N/A';
  }
};

/**
 * Format timestamp to IST with format: DD-MM-YYYY at HH:mm AM/PM
 * @param {string|number|Date} timestamp - The timestamp to format
 * @returns {string} Formatted timestamp in IST with 12-hour format or 'N/A' if invalid
 */
export const formatToIST12Hour = (timestamp) => {
  if (!timestamp) return 'N/A';
  
  try {
    let date;
    
    // Handle different input types
    if (timestamp instanceof Date) {
      date = timestamp;
    } else if (typeof timestamp === 'number') {
      const ms = timestamp < 1e12 ? timestamp * 1000 : timestamp;
      date = new Date(ms);
    } else if (typeof timestamp === 'string') {
      const trimmed = timestamp.trim();
      if (/^\d{10,13}$/.test(trimmed)) {
        const n = Number(trimmed);
        const ms = n < 1e12 ? n * 1000 : n;
        date = new Date(ms);
      } else {
        // Check if timestamp already has timezone info (Z or +/-HH:MM)
        const hasTimezone = trimmed.endsWith('Z') || /[+-]\d{2}:\d{2}$/.test(trimmed);
        
        if (hasTimezone) {
          // Already has timezone, use as is
          date = new Date(trimmed);
        } else {
          // No timezone, append 'Z' to treat as UTC
          date = new Date(trimmed + 'Z');
        }
      }
    } else {
      return 'N/A';
    }
    
    if (isNaN(date.getTime())) {
      return 'N/A';
    }
    
    // Format date part in IST
    const formatter = new Intl.DateTimeFormat('en-IN', {
      timeZone: 'Asia/Kolkata',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour12: false
    });
    
    const parts = formatter.formatToParts(date);
    const getValue = (type) => parts.find(p => p.type === type)?.value || '';
    
    const day = getValue('day');
    const month = getValue('month');
    const year = getValue('year');
    
    // Format time part with 12-hour format in IST
    const timeString = date.toLocaleTimeString('en-IN', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
      timeZone: 'Asia/Kolkata'
    });
    
    return `${day}-${month}-${year} at ${timeString}`;
  } catch (error) {
    console.error('Error formatting timestamp:', error);
    return 'N/A';
  }
};

/**
 * Format timestamp to MM/DD/YYYY at HH:mm AM/PM (US format with IST timezone)
 * @param {string|number|Date} timestamp - The timestamp to format
 * @returns {string} Formatted timestamp in IST with US date format or empty string if invalid
 */
export const formatToISTUSFormat = (timestamp) => {
  if (!timestamp) return '';
  
  try {
    let date;
    
    if (timestamp instanceof Date) {
      date = timestamp;
    } else if (typeof timestamp === 'number') {
      const ms = timestamp < 1e12 ? timestamp * 1000 : timestamp;
      date = new Date(ms);
    } else if (typeof timestamp === 'string') {
      const trimmed = timestamp.trim();
      if (/^\d{10,13}$/.test(trimmed)) {
        const n = Number(trimmed);
        const ms = n < 1e12 ? n * 1000 : n;
        date = new Date(ms);
      } else {
        // Check if timestamp already has timezone info (Z or +/-HH:MM)
        const hasTimezone = trimmed.endsWith('Z') || /[+-]\d{2}:\d{2}$/.test(trimmed);
        
        if (hasTimezone) {
          // Already has timezone, use as is
          date = new Date(trimmed);
        } else {
          // No timezone, append 'Z' to treat as UTC
          date = new Date(trimmed + 'Z');
        }
      }
    } else {
      return '';
    }
    
    if (isNaN(date.getTime())) {
      return '';
    }
    
    // Convert to IST
    const istDate = new Date(date.toLocaleString('en-US', { timeZone: 'Asia/Kolkata' }));
    
    const month = istDate.getMonth() + 1;
    const day = istDate.getDate();
    const year = istDate.getFullYear();
    let hours = istDate.getHours();
    const minutes = istDate.getMinutes().toString().padStart(2, '0');
    const ampm = hours >= 12 ? 'PM' : 'AM';
    hours = hours % 12;
    hours = hours ? hours : 12;
    
    return `${month}/${day}/${year} at ${hours}:${minutes} ${ampm}`;
  } catch (error) {
    console.error('Error formatting timestamp:', error);
    return '';
  }
};

/**
 * Get current timestamp in IST for datetime-local input
 * @returns {string} Current timestamp in YYYY-MM-DDTHH:mm format (IST)
 */
export const getCurrentISTForInput = () => {
  const now = new Date();
  const istDate = new Date(now.toLocaleString('en-US', { timeZone: 'Asia/Kolkata' }));
  
  const year = istDate.getFullYear();
  const month = String(istDate.getMonth() + 1).padStart(2, '0');
  const day = String(istDate.getDate()).padStart(2, '0');
  const hours = String(istDate.getHours()).padStart(2, '0');
  const minutes = String(istDate.getMinutes()).padStart(2, '0');
  
  return `${year}-${month}-${day}T${hours}:${minutes}`;
};

