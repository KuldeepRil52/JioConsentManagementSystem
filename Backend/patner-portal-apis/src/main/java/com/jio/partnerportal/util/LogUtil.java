package com.jio.partnerportal.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogUtil {

    private static final Logger log = LoggerFactory.getLogger(LogUtil.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private LogUtil() {
        // Utility class; prevent instantiation
    }

    public static void logActivity(HttpServletRequest request,
                                   String activity,
                                   String result) {
        String timestamp = LocalDateTime.now().format(formatter);
        String sourceIp = getClientIp(request);

        String logMessage = String.format(
                "Timestamp: %s | SourceIP: %s | %s | Activity done: %s",
                timestamp, sourceIp, activity, result
        );

        log.info(logMessage);
    }

    private static String getClientIp(HttpServletRequest request) {
        if (request == null) return "UNKNOWN";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
