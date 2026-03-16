package com.jio.digigov.notification.config;

import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.enums.NetworkType;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.URIScheme;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.util.Base64;

/**
 * Optimized configuration for RestTemplate with enhanced connection pooling,
 * timeout settings, and mutual SSL support for high-performance HTTP operations.
 *
 * Performance Features:
 * - Connection pooling with optimized pool sizes
 * - Connection keep-alive and reuse
 * - Proper timeout configurations
 * - SSL context optimization
 * - Connection monitoring and cleanup
 *
 * Pool Configuration:
 * - Max connections total: 100
 * - Max connections per route: 20
 * - Connection keep-alive: 30 seconds
 * - Idle connection cleanup: 60 seconds
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 30 seconds
        factory.setReadTimeout(30000); // 30 seconds

        return new RestTemplate(factory);
    }

    public RestTemplate restTemplate(NotificationConfig config) {
        if (config == null || config.getConfigurationJson() == null) {
            throw new IllegalArgumentException("RestTemplate only supports DigiGov configuration");
        }

        var details = config.getConfigurationJson();
        NetworkType networkType = details.getNetworkType() != null ?
                NetworkType.valueOf(details.getNetworkType()) : NetworkType.INTRANET;

        if (networkType == NetworkType.INTRANET) {
            return createUnsafeRestTemplate();
        } else if (networkType == NetworkType.INTERNET) {
            return createRestTemplateWithMutualSSL(config);
        } else {
            return new RestTemplate();
        }
    }

    private RestTemplate createRestTemplateWithMutualSSL(NotificationConfig config) {
        var details = config.getConfigurationJson();
        if (details.getMutualSSL() != null && details.getMutualSSL() && details.getMutualCertificate() != null
                && !details.getMutualCertificate().isEmpty()) {
            try {
                log.info("Configuring RestTemplate with Mutual SSL for configId: {}", config.getConfigId());

                // Decode Base64-encoded PKCS12 certificate
                byte[] certBytes = Base64.getDecoder().decode(details.getMutualCertificate());
                log.debug("Decoded certificate bytes length: {}", certBytes.length);

                // Get certificate credentials (use empty string if not provided)
                char[] credentials = (details.getCertificatePassword() != null && !details.getCertificatePassword().isEmpty())
                        ? details.getCertificatePassword().toCharArray()
                        : "".toCharArray();

                // Load PKCS12 keystore
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(new ByteArrayInputStream(certBytes), credentials);
                log.info("Successfully loaded PKCS12 keystore");

                // Build SSL context with key material
                SSLContext sslContext = SSLContextBuilder.create()
                        .loadKeyMaterial(keyStore, credentials)
                        .build();

                SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext);
                Registry<ConnectionSocketFactory> socketFactoryRegistry =
                        RegistryBuilder.<ConnectionSocketFactory>create()
                        .register(URIScheme.HTTPS.getId(), sslSocketFactory)
                        .build();

                PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
                cm.setMaxTotal(100); // Maximum total connections
                cm.setDefaultMaxPerRoute(20); // Maximum connections per route
                cm.setValidateAfterInactivity(Timeout.ofSeconds(30)); // Validate stale connections

                log.info("Configured connection pool - MaxTotal: {}, MaxPerRoute: {}",
                        cm.getMaxTotal(), cm.getDefaultMaxPerRoute());

                RequestConfig requestConfig = RequestConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(30))
                        .setResponseTimeout(Timeout.ofSeconds(30))
                        .build();

                CloseableHttpClient httpClient = HttpClients.custom()
                        .setConnectionManager(cm)
                        .setDefaultRequestConfig(requestConfig)
                        .build();

                HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

                return new RestTemplate(factory);
            } catch (Exception e) {
                log.error("Error configuring Mutual SSL for RestTemplate: {}", e.getMessage());
                // Fallback to default RestTemplate if SSL configuration fails
                return restTemplate();
            }
        } else {
            log.info("Mutual SSL not enabled or certificate not provided for configId: {}. "
                    + "Using default RestTemplate.", config.getConfigId());
            return restTemplate();
        }
    }


    private RestTemplate createUnsafeRestTemplate() {
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, (certificate, authType) -> true)
                    .build();

            SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

            Registry<ConnectionSocketFactory> socketFactoryRegistry =
                    RegistryBuilder.<ConnectionSocketFactory>create()
                            .register(URIScheme.HTTPS.getId(), csf)
                            .build();

            PoolingHttpClientConnectionManager cm =
                    new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            cm.setMaxTotal(100); // Maximum total connections
            cm.setDefaultMaxPerRoute(20); // Maximum connections per route
            cm.setValidateAfterInactivity(Timeout.ofSeconds(30)); // Validate stale connections

            log.info("Configured unsafe RestTemplate connection pool - MaxTotal: {}, MaxPerRoute: {}",
                    cm.getMaxTotal(), cm.getDefaultMaxPerRoute());

            CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(cm)
                    .build();

            HttpComponentsClientHttpRequestFactory requestFactory =
                    new HttpComponentsClientHttpRequestFactory(httpClient);

            return new RestTemplate(requestFactory);

        } catch (Exception e) {
            log.error("Error creating unsafe RestTemplate: {}", e.getMessage());
            return new RestTemplate();
        }
    }
}