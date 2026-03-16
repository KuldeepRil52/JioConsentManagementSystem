package com.jio.digigov.notification.util;

import com.jio.digigov.notification.constant.NotificationConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

/**
 * HttpServletRequestWrapper that ensures a transaction ID header is always present.
 *
 * This wrapper intercepts header access and automatically generates a UUID for the
 * X-Transaction-ID header if it's not present in the original request. This ensures
 * that all downstream code can safely retrieve the transaction ID without null checks.
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
public class TransactionIdRequestWrapper extends HttpServletRequestWrapper {

    private final String transactionId;
    private final Map<String, String> customHeaders;

    /**
     * Constructs a new TransactionIdRequestWrapper.
     *
     * @param request the original HttpServletRequest
     * @param transactionId the transaction ID to use (generated if original request doesn't have one)
     */
    public TransactionIdRequestWrapper(HttpServletRequest request, String transactionId) {
        super(request);
        this.transactionId = transactionId;
        this.customHeaders = new HashMap<>();
        this.customHeaders.put(NotificationConstants.HEADER_TRANSACTION_ID, transactionId);
    }

    @Override
    public String getHeader(String name) {
        // Check if this is the transaction ID header
        if (NotificationConstants.HEADER_TRANSACTION_ID.equalsIgnoreCase(name)) {
            return transactionId;
        }

        // Check custom headers
        String customValue = customHeaders.get(name);
        if (customValue != null) {
            return customValue;
        }

        // Otherwise return from original request
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> headerNames = new HashSet<>();

        // Add original header names
        Enumeration<String> originalHeaders = super.getHeaderNames();
        if (originalHeaders != null) {
            while (originalHeaders.hasMoreElements()) {
                headerNames.add(originalHeaders.nextElement());
            }
        }

        // Add custom headers
        headerNames.addAll(customHeaders.keySet());

        return Collections.enumeration(headerNames);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        // Check if this is the transaction ID header
        if (NotificationConstants.HEADER_TRANSACTION_ID.equalsIgnoreCase(name)) {
            return Collections.enumeration(Collections.singletonList(transactionId));
        }

        // Check custom headers
        String customValue = customHeaders.get(name);
        if (customValue != null) {
            return Collections.enumeration(Collections.singletonList(customValue));
        }

        // Otherwise return from original request
        return super.getHeaders(name);
    }

    /**
     * Gets the transaction ID for this request.
     *
     * @return the transaction ID (never null)
     */
    public String getTransactionId() {
        return transactionId;
    }
}
