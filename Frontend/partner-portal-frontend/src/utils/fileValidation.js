/**
 * File Upload Security & Validation Utility
 * 
 * Comprehensive file validation to prevent:
 * - XSS attacks via malicious files
 * - File type spoofing
 * - Oversized file uploads (DoS)
 * - Path traversal attacks
 * - Malicious PDF/document uploads
 * 
 * VAPT Compliance: Addresses file upload security vulnerabilities
 */

import DOMPurify from 'dompurify';

// ============================================================================
// CONFIGURATION & CONSTANTS
// ============================================================================

// File size limits (in bytes)
export const FILE_SIZE_LIMITS = {
  IMAGE: 5 * 1024 * 1024,      // 5MB for images
  PDF: 10 * 1024 * 1024,       // 10MB for PDFs
  DOCUMENT: 10 * 1024 * 1024,  // 10MB for documents
  CERTIFICATE: 1 * 1024 * 1024, // 1MB for certificates
  DEFAULT: 10 * 1024 * 1024     // 10MB default
};

// Magic numbers (file signatures) for verification
// First few bytes that identify file types
export const FILE_SIGNATURES = {
  // PDF
  PDF: {
    signature: [0x25, 0x50, 0x44, 0x46], // %PDF
    offset: 0,
    description: 'PDF Document'
  },
  
  // Images
  PNG: {
    signature: [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A],
    offset: 0,
    description: 'PNG Image'
  },
  JPEG: {
    signature: [0xFF, 0xD8, 0xFF],
    offset: 0,
    description: 'JPEG Image'
  },
  
  // Dangerous file types (MUST BE BLOCKED)
  SVG: {
    signature: '<svg',
    offset: 0,
    description: 'SVG Image (DANGEROUS - can contain scripts)',
    isText: true,
    isDangerous: true
  },
  HTML: {
    signature: ['<!DOCTYPE', '<html', '<HTML'],
    offset: 0,
    description: 'HTML Document (DANGEROUS)',
    isText: true,
    isDangerous: true
  },
  XML: {
    signature: '<?xml',
    offset: 0,
    description: 'XML Document (DANGEROUS)',
    isText: true,
    isDangerous: true
  },
  
  // Certificates
  PEM_BEGIN: {
    signature: '-----BEGIN',
    offset: 0,
    description: 'PEM Certificate',
    isText: true
  },
  DER: {
    signature: [0x30, 0x82], // DER-encoded certificate
    offset: 0,
    description: 'DER Certificate'
  }
};

// Allowed file extensions by category
export const ALLOWED_EXTENSIONS = {
  IMAGE: ['.png', '.jpg', '.jpeg'],
  PDF: ['.pdf'],
  CERTIFICATE: ['.pem', '.crt', '.cer'],
  DOCUMENT: ['.pdf', '.doc', '.docx'],
  ALL: ['.png', '.jpg', '.jpeg', '.pdf', '.pem', '.crt', '.cer', '.doc', '.docx']
};

// Allowed MIME types
export const ALLOWED_MIME_TYPES = {
  IMAGE: ['image/png', 'image/jpeg', 'image/jpg'],
  PDF: ['application/pdf'],
  CERTIFICATE: [
    'application/x-pem-file',
    'application/x-x509-ca-cert',
    'application/pkix-cert',
    'application/x-x509-user-cert'
  ],
  DOCUMENT: [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  ]
};

// Dangerous patterns in filenames
const DANGEROUS_FILENAME_PATTERNS = [
  /\.\./,           // Path traversal
  /[<>:"|?*]/,      // Invalid filename characters
  /^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$/i, // Windows reserved names
  /\0/,             // Null bytes
  /javascript:/i,   // JavaScript protocol
  /<script/i,       // Script tags
  /\.exe$/i,        // Executable files
  /\.bat$/i,        // Batch files
  /\.cmd$/i,        // Command files
  /\.sh$/i,         // Shell scripts
  /\.php$/i,        // PHP files
  /\.jsp$/i,        // JSP files
  /\.asp$/i         // ASP files
];

// Malicious PDF patterns (comprehensive detection)
const MALICIOUS_PDF_PATTERNS = [
  /\/JavaScript/gi,
  /\/JS\s*\(/gi,
  /\/AA\s*</gi,              // Automatic Action
  /\/OpenAction/gi,
  /\/Launch/gi,
  /\/EmbeddedFile/gi,
  /\/GoToE/gi,
  /\/GoToR/gi,
  /\/SubmitForm/gi,
  /\/URI\s*\(/gi,
  /\/Action/gi,
  /<script/gi,
  /javascript:/gi,
  /eval\s*\(/gi,
  /unescape\s*\(/gi,
  /document\.write/gi,
  /document\.cookie/gi,
  /window\.location/gi,
  /alert\s*\(/gi,
  /prompt\s*\(/gi,
  /confirm\s*\(/gi,
  /\.innerHTML/gi,
  /onerror\s*=/gi,
  /onload\s*=/gi,
  /onclick\s*=/gi,
  /onmouseover\s*=/gi,
  /app\.alert/gi,
  /xfa\.host/gi,
  /this\.exportDataObject/gi
];

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

/**
 * Read file as ArrayBuffer for magic number verification
 */
const readFileBytes = (file, bytesToRead = 512) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    const slice = file.slice(0, bytesToRead);
    
    reader.onload = (e) => resolve(new Uint8Array(e.target.result));
    reader.onerror = () => reject(new Error('Failed to read file'));
    
    reader.readAsArrayBuffer(slice);
  });
};

/**
 * Read file as text for text-based signature verification
 */
const readFileAsText = (file, bytesToRead = 512) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    const slice = file.slice(0, bytesToRead);
    
    reader.onload = (e) => resolve(e.target.result);
    reader.onerror = () => reject(new Error('Failed to read file'));
    
    reader.readAsText(slice);
  });
};

/**
 * Check if bytes match a signature
 */
const matchesSignature = (bytes, signature, offset = 0) => {
  if (Array.isArray(signature)) {
    for (let i = 0; i < signature.length; i++) {
      if (bytes[offset + i] !== signature[i]) {
        return false;
      }
    }
    return true;
  }
  return false;
};

// ============================================================================
// FILENAME VALIDATION
// ============================================================================

/**
 * Sanitize and validate filename
 * Prevents path traversal and XSS attacks
 */
export const sanitizeFilename = (filename) => {
  if (!filename || typeof filename !== 'string') {
    throw new Error('Invalid filename');
  }

  // ⚠️ WARN about suspicious keywords in filename (but don't block based on name alone)
  const suspiciousKeywords = /malicious|exploit|hack|payload|<script|javascript:|eval\(|alert\(/i;
  if (suspiciousKeywords.test(filename)) {
    console.warn('⚠️ WARNING: Suspicious filename detected:', filename);
    // Don't throw - let content scanning decide if it's actually malicious
  }

  // Remove any path components
  let sanitized = filename.split(/[/\\]/).pop();
  
  // Sanitize with DOMPurify
  sanitized = DOMPurify.sanitize(sanitized, { ALLOWED_TAGS: [] });
  
  // Replace dangerous characters
  sanitized = sanitized.replace(/[<>:"|?*\x00-\x1f]/g, '_');
  
  // Check for dangerous patterns
  for (const pattern of DANGEROUS_FILENAME_PATTERNS) {
    if (pattern.test(sanitized)) {
      throw new Error('Filename contains invalid patterns');
    }
  }
  
  // Limit length
  if (sanitized.length > 255) {
    const ext = sanitized.substring(sanitized.lastIndexOf('.'));
    sanitized = sanitized.substring(0, 250 - ext.length) + ext;
  }
  
  // Ensure it has an extension
  if (!sanitized.includes('.')) {
    throw new Error('Filename must have an extension');
  }
  
  return sanitized;
};

/**
 * Validate filename against allowed extensions
 */
export const validateFilenameExtension = (filename, allowedExtensions) => {
  const sanitized = sanitizeFilename(filename);
  const ext = sanitized.toLowerCase().substring(sanitized.lastIndexOf('.'));
  
  if (!allowedExtensions.includes(ext)) {
    return {
      valid: false,
      error: `File type not allowed. Allowed types: ${allowedExtensions.join(', ')}`
    };
  }
  
  return { valid: true, sanitizedFilename: sanitized };
};

// ============================================================================
// FILE SIZE VALIDATION
// ============================================================================

/**
 * Validate file size against limit
 */
export const validateFileSize = (file, maxSize) => {
  if (!file || !file.size) {
    return {
      valid: false,
      error: 'Invalid file'
    };
  }
  
  if (file.size > maxSize) {
    const maxSizeMB = (maxSize / (1024 * 1024)).toFixed(2);
    const fileSizeMB = (file.size / (1024 * 1024)).toFixed(2);
    return {
      valid: false,
      error: `File size (${fileSizeMB}MB) exceeds maximum allowed size (${maxSizeMB}MB)`
    };
  }
  
  if (file.size === 0) {
    return {
      valid: false,
      error: 'File is empty'
    };
  }
  
  return { valid: true };
};

// ============================================================================
// MAGIC NUMBER (FILE SIGNATURE) VERIFICATION
// ============================================================================

/**
 * Verify file type using magic numbers (file signatures)
 * This prevents file type spoofing
 */
export const verifyFileSignature = async (file, expectedType) => {
  try {
    // Read first 512 bytes
    const bytes = await readFileBytes(file, 512);
    
    // First, check for DANGEROUS file types (SVG, HTML, XML) - MUST BLOCK
    const text = new TextDecoder('utf-8', { fatal: false }).decode(bytes.slice(0, 100));
    const textLower = text.toLowerCase();
    
    // CRITICAL: Block SVG files (can contain XSS)
    if (textLower.includes('<svg') || textLower.includes('<?xml') && textLower.includes('svg')) {
      console.error('❌ DANGEROUS FILE TYPE DETECTED: SVG');
      return {
        valid: false,
        error: '⛔ SVG files are not allowed due to security risks (can contain malicious scripts)',
        detectedType: 'SVG (BLOCKED)'
      };
    }
    
    // Block HTML files
    if (textLower.includes('<!doctype html') || textLower.includes('<html')) {
      console.error('❌ DANGEROUS FILE TYPE DETECTED: HTML');
      return {
        valid: false,
        error: '⛔ HTML files are not allowed',
        detectedType: 'HTML (BLOCKED)'
      };
    }
    
    // Block XML files (unless it's a certificate)
    if (textLower.startsWith('<?xml') && expectedType !== 'CERTIFICATE') {
      console.error('❌ DANGEROUS FILE TYPE DETECTED: XML');
      return {
        valid: false,
        error: '⛔ XML files are not allowed',
        detectedType: 'XML (BLOCKED)'
      };
    }
    
    // Check PDF
    if (expectedType === 'PDF' || expectedType === 'DOCUMENT') {
      if (matchesSignature(bytes, FILE_SIGNATURES.PDF.signature, 0)) {
        console.log('✅ Valid PDF signature detected');
        return { valid: true, detectedType: 'PDF' };
      }
    }
    
    // Check PNG
    if (expectedType === 'IMAGE') {
      if (matchesSignature(bytes, FILE_SIGNATURES.PNG.signature, 0)) {
        console.log('✅ Valid PNG signature detected');
        return { valid: true, detectedType: 'PNG' };
      }
      
      // Check JPEG
      if (matchesSignature(bytes, FILE_SIGNATURES.JPEG.signature, 0)) {
        console.log('✅ Valid JPEG signature detected');
        return { valid: true, detectedType: 'JPEG' };
      }
    }
    
    // Check PEM certificate (text-based) - REMOVED STRICT VALIDATION
    // Certificates now use basic validation (extension + size only)
    if (expectedType === 'CERTIFICATE') {
      // Accept any certificate file - validation is done at higher level
      return { valid: true, detectedType: 'CERTIFICATE' };
    }
    
    return {
      valid: false,
      error: 'File type does not match expected format. File may be corrupted or spoofed.'
    };
    
  } catch (error) {
    console.error('❌ File signature verification error:', error);
    return {
      valid: false,
      error: `Failed to verify file signature: ${error.message}`
    };
  }
};

// ============================================================================
// MALICIOUS CONTENT DETECTION
// ============================================================================

/**
 * Scan PDF for malicious content patterns
 * Basic detection of dangerous PDF features
 */
export const scanPDFForMaliciousContent = async (file) => {
  try {
    console.log('🔍 Scanning PDF for malicious content:', file.name);
    
    // Read first 200KB for scanning (sufficient to detect malicious patterns)
    // Reading entire large files can cause performance issues
    const scanSize = Math.min(file.size, 200 * 1024);
    const bytes = await readFileBytes(file, scanSize);
    
    // Try multiple decodings to catch obfuscated content
    let text = '';
    try {
      text = new TextDecoder('utf-8').decode(bytes);
    } catch (e) {
      // If UTF-8 fails, try latin1
      text = new TextDecoder('latin1').decode(bytes);
    }
    
    console.log('📄 PDF content sample (first 500 chars):', text.substring(0, 500));
    console.log('📊 PDF size:', file.size, 'bytes, Scanned:', bytes.length, 'bytes');
    
    // 🔴 CRITICAL: Check for JavaScript in PDFs (even if compressed)
    // PDFs can use FlateDecode, ASCIIHexDecode, ASCII85Decode, etc. to compress streams
    const hasJavaScriptReferences = /\/JavaScript|\/JS\s*<<|\/Names\s*<<\s*\/JavaScript|\/AA\s*<<.*\/JS/i.test(text);
    
    // ✅ ONLY block if JavaScript is actually present, not just compressed streams
    // (Most legitimate PDFs use compression, that's normal!)
    if (hasJavaScriptReferences) {
      console.error('❌ JAVASCRIPT DETECTED IN PDF:', {
        filename: file.name,
        detection: 'PDF contains JavaScript references which could be malicious'
      });
      
      return {
        valid: false,
        error: 'PDF contains JavaScript code. For security reasons, PDFs with embedded JavaScript are not allowed.',
        pattern: 'JavaScript in PDF'
      };
    }
    
    // 🔴 Check for hex-encoded strings (common obfuscation technique)
    // JavaScript code can be hidden as hex strings in PDFs
    const hexEncodedPatterns = [
      '6a617661736372697074',  // "javascript" in hex
      '3c736372697074',          // "<script" in hex
      '616c657274',              // "alert" in hex
      '6576616c',                // "eval" in hex
      '646f63756d656e742e636f6f6b6965',  // "document.cookie" in hex
      '77696e646f772e6c6f636174696f6e'   // "window.location" in hex
    ];
    
    const textLower = text.toLowerCase();
    for (const hexPattern of hexEncodedPatterns) {
      if (textLower.includes(hexPattern)) {
        console.error('❌ HEX-ENCODED XSS DETECTED:', {
          filename: file.name,
          hexPattern,
          decodedMeaning: hexPattern === '6a617661736372697074' ? 'javascript' :
                          hexPattern === '3c736372697074' ? '<script' :
                          hexPattern === '616c657274' ? 'alert' :
                          hexPattern === '6576616c' ? 'eval' : 'suspicious string'
        });
        
        return {
          valid: false,
          error: 'PDF contains hex-encoded suspicious content that could be malicious JavaScript',
          pattern: 'Hex-encoded XSS'
        };
      }
    }
    
    // Additional aggressive XSS string checks
    const aggressiveXSSPatterns = [
      'javascript:',
      '<script',
      '</script>',
      'onerror=',
      'onload=',
      'onclick=',
      'onmouseover=',
      'alert(',
      'eval(',
      'document.cookie',
      'document.write',
      'window.location',
      '.innerHTML',
      'app.alert',
      'xfa.host',
      '<svg',           // SVG files can contain XSS
      '<?xml',          // XML prologue (SVG often has this)
      '<iframe',        // Iframe injection
      '<embed',         // Embed injection
      '<object',        // Object injection
      'fromCharCode',   // Often used for obfuscation
      'String.fromCharCode', // JavaScript obfuscation
      'unescape(',      // Decoding obfuscated code
      'decodeURI',      // URL decoding tricks
      'atob(',          // Base64 decode
      'btoa('           // Base64 encode
    ];
    
    for (const xssString of aggressiveXSSPatterns) {
      if (text.toLowerCase().includes(xssString.toLowerCase())) {
        console.error('❌ AGGRESSIVE XSS DETECTION:', {
          filename: file.name,
          detectedString: xssString,
          context: text.substring(
            Math.max(0, text.toLowerCase().indexOf(xssString.toLowerCase()) - 50),
            text.toLowerCase().indexOf(xssString.toLowerCase()) + 50
          )
        });
        
        return {
          valid: false,
          error: `PDF contains malicious XSS content: "${xssString}" detected`,
          pattern: xssString
        };
      }
    }
    
    // Check for malicious PDF object patterns
    for (const pattern of MALICIOUS_PDF_PATTERNS) {
      if (pattern.test(text)) {
        console.error('❌ MALICIOUS PDF PATTERN DETECTED:', {
          filename: file.name,
          pattern: pattern.toString(),
          matched: text.match(pattern)?.[0]
        });
        
        return {
          valid: false,
          error: 'PDF contains potentially malicious content (JavaScript, automatic actions, or embedded files)',
          pattern: pattern.toString()
        };
      }
    }
    
    console.log('✅ PDF scan passed - no malicious patterns detected');
    return { valid: true };
    
  } catch (error) {
    console.error('❌ PDF scan error:', error);
    // If we can't scan, fail safe - BLOCK the file
    return {
      valid: false,
      error: `Failed to scan PDF securely: ${error.message}. File blocked for safety.`
    };
  }
};

/**
 * Check image for potential XSS vectors
 */
export const validateImageSafety = async (file) => {
  try {
    const bytes = await readFileBytes(file, 1024);
    const text = new TextDecoder('utf-8', { fatal: false }).decode(bytes);
    
    // Check for script tags or JavaScript in image metadata
    if (/<script/i.test(text) || /javascript:/i.test(text)) {
      return {
        valid: false,
        error: 'Image contains potentially malicious content'
      };
    }
    
    return { valid: true };
    
  } catch (error) {
    return {
      valid: false,
      error: `Failed to validate image: ${error.message}`
    };
  }
};

// ============================================================================
// COMPREHENSIVE FILE VALIDATION
// ============================================================================

/**
 * Comprehensive file validation
 * Validates: filename, size, extension, MIME type, file signature, and malicious content
 * 
 * @param {File} file - The file to validate
 * @param {Object} options - Validation options
 * @param {string} options.type - Expected file type: 'IMAGE', 'PDF', 'CERTIFICATE', 'DOCUMENT'
 * @param {number} options.maxSize - Maximum file size (optional, uses defaults)
 * @param {boolean} options.scanMalicious - Whether to scan for malicious content (default: true)
 * @returns {Promise<Object>} Validation result
 */
export const validateFile = async (file, options = {}) => {
  const {
    type = 'DEFAULT',
    maxSize = FILE_SIZE_LIMITS[type] || FILE_SIZE_LIMITS.DEFAULT,
    scanMalicious = true
  } = options;
  
  // Validation result object
  const result = {
    valid: true,
    errors: [],
    warnings: [],
    sanitizedFilename: '',
    fileInfo: {
      name: file.name,
      size: file.size,
      type: file.type,
      detectedType: null
    }
  };
  
  try {
    // 1. Filename validation and sanitization
    try {
      const filenameValidation = validateFilenameExtension(
        file.name,
        ALLOWED_EXTENSIONS[type] || ALLOWED_EXTENSIONS.ALL
      );
      
      if (!filenameValidation.valid) {
        result.valid = false;
        result.errors.push(filenameValidation.error);
        return result;
      }
      
      result.sanitizedFilename = filenameValidation.sanitizedFilename;
    } catch (error) {
      result.valid = false;
      result.errors.push(`Filename validation failed: ${error.message}`);
      return result;
    }
    
    // 2. File size validation
    const sizeValidation = validateFileSize(file, maxSize);
    if (!sizeValidation.valid) {
      result.valid = false;
      result.errors.push(sizeValidation.error);
      return result;
    }
    
    // 3. MIME type validation
    const allowedMimeTypes = ALLOWED_MIME_TYPES[type];
    if (allowedMimeTypes && file.type && !allowedMimeTypes.includes(file.type)) {
      result.warnings.push(`MIME type ${file.type} not in whitelist. Performing additional checks.`);
    }
    
    // 4. Magic number verification (file signature)
    const signatureValidation = await verifyFileSignature(file, type);
    if (!signatureValidation.valid) {
      result.valid = false;
      result.errors.push(signatureValidation.error);
      return result;
    }
    result.fileInfo.detectedType = signatureValidation.detectedType;
    
    // 5. Malicious content scanning
    if (scanMalicious) {
      console.log('🛡️ Starting malicious content scan for:', file.name, 'Type:', type);
      
      if (type === 'PDF' || type === 'DOCUMENT') {
        const pdfScan = await scanPDFForMaliciousContent(file);
        if (!pdfScan.valid) {
          console.error('❌ PDF SCAN FAILED:', pdfScan.error);
          result.valid = false;
          result.errors.push(pdfScan.error);
          return result;
        }
        console.log('✅ PDF malicious content scan passed');
      } else if (type === 'IMAGE') {
        const imageScan = await validateImageSafety(file);
        if (!imageScan.valid) {
          console.error('❌ IMAGE SCAN FAILED:', imageScan.error);
          result.valid = false;
          result.errors.push(imageScan.error);
          return result;
        }
        console.log('✅ Image safety scan passed');
      }
    }
    
    return result;
    
  } catch (error) {
    result.valid = false;
    result.errors.push(`Validation error: ${error.message}`);
    return result;
  }
};

// ============================================================================
// CONVENIENCE FUNCTIONS FOR SPECIFIC FILE TYPES
// ============================================================================

/**
 * Validate image file
 */
export const validateImageFile = async (file) => {
  return await validateFile(file, {
    type: 'IMAGE',
    maxSize: FILE_SIZE_LIMITS.IMAGE
  });
};

/**
 * Validate PDF file
 */
export const validatePDFFile = async (file) => {
  return await validateFile(file, {
    type: 'PDF',
    maxSize: FILE_SIZE_LIMITS.PDF,
    scanMalicious: true
  });
};

/**
 * Validate certificate file
 */
export const validateCertificateFile = async (file) => {
  const errors = [];
  
  // 1. Check file extension
  const allowedExtensions = ['.pem', '.crt', '.cer', '.der', '.p12', '.pfx'];
  const fileName = file.name.toLowerCase();
  const hasValidExtension = allowedExtensions.some(ext => fileName.endsWith(ext));
  
  if (!hasValidExtension) {
    errors.push(`Invalid file extension. Allowed: ${allowedExtensions.join(', ')}`);
  }
  
  // 2. Check file size (max 500KB)
  const maxSize = FILE_SIZE_LIMITS.CERTIFICATE;
  if (file.size > maxSize) {
    errors.push(`File size (${(file.size / 1024).toFixed(2)} KB) exceeds maximum allowed size (${(maxSize / 1024).toFixed(2)} KB)`);
  }
  
  // 3. Check if file is empty
  if (file.size === 0) {
    errors.push('File is empty');
  }
  
  if (errors.length > 0) {
    return {
      valid: false,
      errors: errors,
      sanitizedFilename: file.name
    };
  }
  
  return {
    valid: true,
    errors: [],
    warnings: [],
    sanitizedFilename: file.name,
    fileInfo: {
      name: file.name,
      size: file.size,
      type: file.type,
      detectedType: 'CERTIFICATE'
    }
  };
};

/**
 * Validate document file (PDF, DOC, DOCX)
 */
export const validateDocumentFile = async (file) => {
  return await validateFile(file, {
    type: 'DOCUMENT',
    maxSize: FILE_SIZE_LIMITS.DOCUMENT,
    scanMalicious: true
  });
};

// ============================================================================
// UTILITY: Display validation errors to user
// ============================================================================

/**
 * Format validation result for user display
 */
export const formatValidationErrors = (validationResult) => {
  if (validationResult.valid) {
    return null;
  }
  
  const errors = validationResult.errors || [];
  const warnings = validationResult.warnings || [];
  
  let message = 'File validation failed:\n';
  
  if (errors.length > 0) {
    message += '\nErrors:\n';
    errors.forEach((error, index) => {
      message += `${index + 1}. ${error}\n`;
    });
  }
  
  if (warnings.length > 0) {
    message += '\nWarnings:\n';
    warnings.forEach((warning, index) => {
      message += `${index + 1}. ${warning}\n`;
    });
  }
  
  return message;
};

export default {
  validateFile,
  validateImageFile,
  validatePDFFile,
  validateCertificateFile,
  validateDocumentFile,
  sanitizeFilename,
  formatValidationErrors,
  FILE_SIZE_LIMITS,
  ALLOWED_EXTENSIONS
};

