/**
 * Utility function to export data to CSV format
 * @param {Array} data - Array of objects to export
 * @param {string} filename - Name of the CSV file
 * @param {Array} headers - Optional custom headers (if not provided, uses object keys)
 */
export const exportToCSV = (data, filename, headers = null) => {
  if (!data || data.length === 0) {
    console.warn('No data to export');
    return;
  }

  try {
    // Get headers from first object if not provided
    const csvHeaders = headers || Object.keys(data[0]);
    
    // Create CSV content – prepend BOM so Excel opens with correct encoding
    let csvContent = '\uFEFF';
    
    // Add headers
    csvContent += csvHeaders.map(header => `"${header}"`).join(',') + '\n';
    
    // Add data rows
    data.forEach(row => {
      const values = csvHeaders.map(header => {
        const value = row[header];
        
        // Handle different data types
        if (value === null || value === undefined) {
          return '""';
        }
        
        // Convert value to string and escape quotes
        let stringValue = String(value).replace(/"/g, '""');

        // Prevent Excel from auto-interpreting fraction-like values (e.g. 0/1)
        // as dates by prefixing with a tab character when the pattern matches.
        if (/^\d+\s*\/\s*\d+$/.test(stringValue)) {
          stringValue = '\t' + stringValue;
        }

        return `"${stringValue}"`;
      });
      
      csvContent += values.join(',') + '\n';
    });
    
    // Create blob and download
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    
    if (link.download !== undefined) {
      // Create a link to the file
      const url = URL.createObjectURL(blob);
      link.setAttribute('href', url);
      link.setAttribute('download', `${filename}_${new Date().toISOString().split('T')[0]}.csv`);
      link.style.visibility = 'hidden';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    }
  } catch (error) {
    console.error('Error exporting CSV:', error);
  }
};

/**
 * Format nested objects for CSV export
 * @param {Object} obj - Object to flatten
 * @param {string} prefix - Prefix for nested keys
 * @returns {Object} Flattened object
 */
export const flattenObject = (obj, prefix = '') => {
  return Object.keys(obj).reduce((acc, key) => {
    const value = obj[key];
    const newKey = prefix ? `${prefix}.${key}` : key;
    
    if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
      Object.assign(acc, flattenObject(value, newKey));
    } else if (Array.isArray(value)) {
      acc[newKey] = value.join(', ');
    } else {
      acc[newKey] = value;
    }
    
    return acc;
  }, {});
};

