package com.jio.digigov.notification.util;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Utility class for retrieving the server's own IP address.
 * Used for audit logging when HttpServletRequest is not available
 * (e.g., Kafka consumers, scheduled jobs, system operations).
 */
@Slf4j
public class ServerIpAddressUtil {

    private static volatile String cachedServerIp = null;
    private static final Object LOCK = new Object();

    private ServerIpAddressUtil() {
        // Utility class
    }

    /**
     * Gets the server's primary IP address.
     * The result is cached after first retrieval for performance.
     *
     * Priority order:
     * 1. First non-loopback IPv4 address from network interfaces
     * 2. Localhost IP (if no network interfaces found)
     * 3. "SYSTEM" as fallback
     *
     * @return the server's IP address, or "SYSTEM" if unable to determine
     */
    public static String getServerIp() {
        if (cachedServerIp != null) {
            return cachedServerIp;
        }

        synchronized (LOCK) {
            if (cachedServerIp != null) {
                return cachedServerIp;
            }

            try {
                // Try to find non-loopback IPv4 address
                String detectedIp = findNonLoopbackIpv4Address();
                if (detectedIp != null) {
                    cachedServerIp = detectedIp;
                    log.info("Server IP address detected and cached: {}", cachedServerIp);
                    return cachedServerIp;
                }

                // Fallback to localhost IP
                InetAddress localhost = InetAddress.getLocalHost();
                String localhostIp = localhost.getHostAddress();
                if (localhostIp != null && !localhostIp.equals("127.0.0.1")) {
                    cachedServerIp = localhostIp;
                    log.info("Server IP address (localhost fallback) cached: {}", cachedServerIp);
                    return cachedServerIp;
                }

                // Final fallback
                log.warn("Unable to determine server IP address, using SYSTEM as fallback");
                cachedServerIp = "SYSTEM";
                return cachedServerIp;

            } catch (Exception e) {
                log.error("Error determining server IP address: {}", e.getMessage(), e);
                cachedServerIp = "SYSTEM";
                return cachedServerIp;
            }
        }
    }

    /**
     * Finds the first non-loopback IPv4 address from available network interfaces.
     *
     * @return the detected IP address, or null if not found
     */
    private static String findNonLoopbackIpv4Address() throws SocketException {
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();

            // Skip loopback and inactive interfaces
            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();

                // Look for non-loopback IPv4 address
                if (!address.isLoopbackAddress() && address.getHostAddress().indexOf(':') == -1) {
                    String ip = address.getHostAddress();
                    log.debug("Found non-loopback IPv4 address: {} on interface: {}",
                            ip, networkInterface.getName());
                    return ip;
                }
            }
        }

        return null;
    }

    /**
     * Clears the cached server IP address.
     * Useful for testing or when network configuration changes at runtime.
     */
    public static void clearCache() {
        synchronized (LOCK) {
            log.info("Clearing cached server IP address: {}", cachedServerIp);
            cachedServerIp = null;
        }
    }

    /**
     * Gets the server's hostname.
     *
     * @return the server's hostname, or "unknown" if unable to determine
     */
    public static String getServerHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            log.error("Error determining server hostname: {}", e.getMessage());
            return "unknown";
        }
    }
}
