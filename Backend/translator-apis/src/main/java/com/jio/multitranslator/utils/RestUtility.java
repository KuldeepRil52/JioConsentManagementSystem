package com.jio.multitranslator.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.multitranslator.constant.Constants;
import com.jio.multitranslator.exceptions.TranslateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
public class RestUtility {

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final ObjectMapper objectMapper;

    public RestUtility(RestTemplate restTemplate, RetryTemplate retryTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Queries a remote service with retry logic and error handling.
     *
     * @param url            the service URL
     * @param headers        request headers
     * @param method         HTTP method
     * @param requestBody    request body object
     * @param responseClass  expected response class
     * @param responseHeaders optional map to capture response headers
     * @param <R>          request type
     * @param <S>          response type
     * @return the parsed response object
     */
    public <R, S> S queryRemoteService(
            String url,
            Map<String, String> headers,
            HttpMethod method,
            R requestBody,
            Class<S> responseClass,
            Map<String, String> responseHeaders) {

        return retryTemplate.execute(context -> {
            long startMillis = System.currentTimeMillis();

            try {
                HttpHeaders httpHeaders = buildHttpHeaders(headers);
                HttpEntity<R> entity = buildHttpEntity(method, requestBody, httpHeaders);
                ResponseEntity<String> responseEntity = restTemplate.exchange(url, method, entity, String.class);

                captureResponseHeaders(responseEntity, responseHeaders);

                long elapsed = System.currentTimeMillis() - startMillis;
                int retryCount = context.getRetryCount();
                if (elapsed > Constants.SLOW_API_CALL_THRESHOLD_MS) {
                    log.warn("Slow API call detected - Method: {}, URL: {}, Duration: {} ms, RetryAttempt: {}", 
                            method, url, elapsed, retryCount + 1);
                } else {
                    log.debug("API call completed - Method: {}, URL: {}, Duration: {} ms, RetryAttempt: {}", 
                            method, url, elapsed, retryCount + 1);
                }

                return parseResponse(responseEntity, responseClass);

            } catch (HttpClientErrorException | HttpServerErrorException e) {
                long elapsed = System.currentTimeMillis() - startMillis;
                log.warn("HTTP error during API call - Method: {}, URL: {}, Duration: {} ms, Status: {}", 
                        method, url, elapsed, e.getStatusCode());
                handleHttpException(url, e);
                throw new IllegalStateException("handleHttpException should always throw an exception");
            } catch (IOException e) {
                long elapsed = System.currentTimeMillis() - startMillis;
                log.error("Failed to parse response - Method: {}, URL: {}, Duration: {} ms", 
                        method, url, elapsed, e);
                throw new TranslateException(
                        "Failed to parse response from remote service",
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        e.getMessage()
                );
            } catch (RuntimeException e) {
                long elapsed = System.currentTimeMillis() - startMillis;
                log.error("Unexpected runtime error - Method: {}, URL: {}, Duration: {} ms", 
                        method, url, elapsed, e);
                // Re-throw if it's already a TranslateException, otherwise wrap it
                if (e instanceof TranslateException) {
                    throw e;
                }
                throw new TranslateException(
                        "Unexpected error calling remote service",
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        e.getMessage()
                );
            }
        });
    }

    /**
     * Builds HTTP headers from the provided map.
     */
    private HttpHeaders buildHttpHeaders(Map<String, String> headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        if (headers != null) {
            headers.forEach(httpHeaders::add);
        }
        return httpHeaders;
    }

    /**
     * Builds HTTP entity based on method type.
     */
    private <R> HttpEntity<R> buildHttpEntity(HttpMethod method, R requestBody, HttpHeaders httpHeaders) {
        if (method == HttpMethod.POST || method == HttpMethod.PUT) {
            return new HttpEntity<>(requestBody, httpHeaders);
        }
        return new HttpEntity<>(httpHeaders);
    }

    /**
     * Captures response headers if requested.
     */
    private void captureResponseHeaders(ResponseEntity<String> responseEntity, Map<String, String> responseHeaders) {
        if (responseHeaders != null) {
            responseEntity.getHeaders().forEach((key, values) -> {
                if (!values.isEmpty()) {
                    responseHeaders.put(key, values.get(0));
                }
            });
        }
    }

    /**
     * Parses the response entity into the expected response class.
     */
    @SuppressWarnings("unchecked")
    private <S> S parseResponse(ResponseEntity<String> responseEntity, Class<S> responseClass) throws IOException {
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            log.error("Non-OK response from {}: {} - {}", 
                    responseEntity.getStatusCode(), responseEntity.getBody());
            throw new TranslateException(
                    "Non-OK response from remote service",
                    HttpStatus.valueOf(responseEntity.getStatusCode().value()),
                    "Response status: " + responseEntity.getStatusCode()
            );
        }

        String body = responseEntity.getBody();
        if (String.class.equals(responseClass)) {
            return (S) body;
        }
        return objectMapper.readValue(body, responseClass);
    }

    /**
     * Handles HTTP exceptions with appropriate error messages.
     */
    private void handleHttpException(String url, RuntimeException e) {
        HttpExceptionInfo exceptionInfo = extractExceptionInfo(e);
        log.error("HTTP error calling {}: Status {} Message: {}", url, exceptionInfo.statusCode(), e.getMessage());

        if (exceptionInfo.errorMessage() != null && exceptionInfo.errorMessage().trim().startsWith("<")) {
            log.error("Blocked by proxy/WAF while calling {}. Status: {}", url, exceptionInfo.statusCode());
            throw new TranslateException(
                    "Request blocked by enterprise proxy",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    exceptionInfo.errorMessage()
            );
        }

        String extractedMessage = extractErrorMessage(exceptionInfo.errorMessage());
        HttpStatus httpStatus = determineHttpStatus(exceptionInfo.statusCode());
        String serviceName = determineServiceName(url);
        throw new TranslateException(serviceName + " Generated", httpStatus, extractedMessage);
    }

    /**
     * Extracts exception information from HTTP exceptions.
     */
    private HttpExceptionInfo extractExceptionInfo(RuntimeException e) {
        if (e instanceof HttpClientErrorException clientError) {
            return new HttpExceptionInfo(clientError.getStatusCode(), clientError.getResponseBodyAsString());
        } else if (e instanceof HttpServerErrorException serverError) {
            return new HttpExceptionInfo(serverError.getStatusCode(), serverError.getResponseBodyAsString());
        }
        return new HttpExceptionInfo(null, null);
    }

    /**
     * Determines HTTP status based on status code.
     */
    private HttpStatus determineHttpStatus(HttpStatusCode statusCode) {
        return statusCode != null && statusCode.is4xxClientError()
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.SERVICE_UNAVAILABLE;
    }

    /**
     * Determines service name from URL.
     */
    private String determineServiceName(String url) {
        return url.contains("bhashini") || url.contains("ulca") ? "Bhashini" : "Translation";
    }

    /**
     * Record to hold HTTP exception information.
     */
    private record HttpExceptionInfo(HttpStatusCode statusCode, String errorMessage) {}

    /**
     * Extracts error message from JSON response if available.
     */
    private String extractErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            return "Unknown error";
        }

        try {
            JsonNode root = objectMapper.readTree(errorMessage);
            if (root.has("message")) {
                return root.get("message").asText();
            }
        } catch (IOException ignored) {
            // If parsing fails, return original message
        }
        return errorMessage;
    }

}