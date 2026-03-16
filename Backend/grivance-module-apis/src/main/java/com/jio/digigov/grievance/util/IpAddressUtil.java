package com.jio.digigov.grievance.util;

import jakarta.servlet.http.HttpServletRequest;

public class IpAddressUtil {
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Convert IPv6 localhost to IPv4 localhost
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        // In case of multiple IPs (proxies), pick the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0];
        }

        return ip;
    }
}
