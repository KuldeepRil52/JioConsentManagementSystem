package com.jio.digigov.notification.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for extracting client IP address from HTTP requests.
 * Follows standard proxy header conventions.
 */
@Slf4j
public class IpAddressUtil {

    private IpAddressUtil() {
        // Utility class
    }

    /**
     * Extracts the client IP address from the HTTP request.
     * <p>
     * Checks headers in the following priority order:
     * 1. X-Forwarded-For (takes first IP from comma-separated list)
     * 2. X-Real-IP
     * 3. Direct remote address from request
     *
     * @param request the HTTP servlet request
     * @return the client IP address, or "unknown" if unable to determine
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            log.warn("HttpServletRequest is null, returning unknown IP");
            return "unknown";
        }

        try {
            // Check X-Forwarded-For header (used by proxies and load balancers)
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (isValidIp(xForwardedFor)) {
                // X-Forwarded-For can contain multiple IPs, take the first one (original client)
                String clientIp = xForwardedFor.split(",")[0].trim();
                log.debug("Client IP extracted from X-Forwarded-For: {}", clientIp);
                return clientIp;
            }

            // Check X-Real-IP header (used by some proxies like Nginx)
            String xRealIp = request.getHeader("X-Real-IP");
            if (isValidIp(xRealIp)) {
                log.debug("Client IP extracted from X-Real-IP: {}", xRealIp);
                return xRealIp;
            }

            // Fallback to direct remote address
            String remoteAddr = request.getRemoteAddr();
            if (isValidIp(remoteAddr)) {
                log.debug("Client IP extracted from RemoteAddr: {}", remoteAddr);
                return remoteAddr;
            }

            log.warn("Unable to extract valid client IP, returning unknown");
            return "unknown";

        } catch (Exception e) {
            log.error("Error extracting client IP address: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * Validates if the given IP address string is valid and not a placeholder.
     *
     * @param ip the IP address string to validate
     * @return true if valid, false otherwise
     */
    private static boolean isValidIp(String ip) {
        return ip != null
                && !ip.isEmpty()
                && !ip.isBlank()
                && !"unknown".equalsIgnoreCase(ip);
    }
}
