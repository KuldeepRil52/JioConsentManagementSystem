package com.jio.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.auth.constants.ErrorCode;
import com.jio.auth.exception.CustomException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ClientIpLoggingFilter extends OncePerRequestFilter {

    @Value("${whitelistips.file.path}")
    private String whitelistPath;

    @Value("${whitelistips.check:false}")
    private boolean whitelistCheck;

    private volatile Map<String, Integer> whitelist = Collections.emptyMap();
    private final Object lock = new Object();

    private void loadWhitelistIfNeeded() throws IOException {
        if (whitelist.isEmpty()) {
            synchronized (lock) {
                if (whitelist.isEmpty() && whitelistCheck) {
                    ObjectMapper mapper = new ObjectMapper();
                    whitelist = mapper.readValue(new File(whitelistPath), Map.class);
                    log.info("Whitelist loaded: {}", whitelist.keySet());
                }
            }
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        loadWhitelistIfNeeded(); // safe lazy load

        String actualIP = request.getRemoteAddr();
        log.info("Request from IP: {}", actualIP);
        log.debug("whitelist IPs contain IP: " + actualIP +" "+ whitelist.containsKey(actualIP));
        log.debug("whitelist IPs config " +whitelistCheck );
        if (whitelistCheck && !whitelist.containsKey(actualIP)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"errorCode\": \"AUTH401\" , \"errorMessage\": \"Unauthorized IP\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
