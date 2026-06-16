/**
 * Safely decodes a Base64 string to a binary string.
 * Handles:
 *  - data URL prefixes (data:application/pdf;base64,...)
 *  - URL-safe chars (- and _)
 *  - missing padding (=)
 *  - stray whitespace/newlines
 *
 * @param {string} base64Input
 * @returns {string} binary string for further processing
 * @throws {Error} if decoding fails
 */
export function decodeBase64(base64Input) {
  if (!base64Input) throw new Error("Empty Base64 input");

  let str = base64Input.trim();

  // Strip data URL prefix if present
  if (str.includes(",")) {
    str = str.split(",")[1];
  }

  // Remove any non-base64 characters (spaces, newlines, etc.)
  str = str.replace(/[^A-Za-z0-9+/=_-]/g, "");

  // Convert URL-safe to standard Base64
  str = str.replace(/-/g, "+").replace(/_/g, "/");

  // Pad with "=" to reach a length multiple of 4
  while (str.length % 4 !== 0) {
    str += "=";
  }

  try {
    return atob(str);
  } catch (err) {
    console.error("decodeBase64: Failed to decode string", err);
    throw err;
  }
}

/**
 * Converts a Base64 string directly to a Uint8Array (for Blobs).
 *
 * @param {string} base64Input
 * @returns {Uint8Array}
 */
export function base64ToUint8Array(base64Input) {
  const binaryStr = decodeBase64(base64Input);
  const bytes = new Uint8Array(binaryStr.length);
  for (let i = 0; i < binaryStr.length; i++) {
    bytes[i] = binaryStr.charCodeAt(i);
  }
  return bytes;
}
