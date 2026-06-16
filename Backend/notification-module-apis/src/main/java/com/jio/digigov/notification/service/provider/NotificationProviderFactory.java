package com.jio.digigov.notification.service.provider;

import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.enums.NotificationChannel;
import com.jio.digigov.notification.enums.ProviderType;
import com.jio.digigov.notification.exception.BusinessException;
import com.jio.digigov.notification.service.provider.impl.DigiGovProviderService;
import com.jio.digigov.notification.service.provider.impl.SmtpProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for creating notification provider service instances.
 * Uses the Factory pattern to dynamically select the appropriate provider
 * based on configuration.
 *
 * <p><b>Design Pattern:</b> Factory Pattern</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * // Inject factory
 * {@literal @}Autowired
 * private NotificationProviderFactory providerFactory;
 *
 * // Get provider for SMTP email
 * NotificationProviderService provider = providerFactory.getProvider(
 *     ProviderType.SMTP,
 *     NotificationChannel.EMAIL,
 *     notificationConfig
 * );
 *
 * // Use provider
 * ProviderEmailResponse response = provider.sendEmail(request, config);
 * </pre>
 *
 * @author Notification Service Team
 * @since 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProviderFactory {

    private final DigiGovProviderService digiGovProviderService;
    private final SmtpProviderService smtpProviderService;

    // Cache of providers by type
    private Map<ProviderType, NotificationProviderService> providerCache;

    /**
     * Initialize provider cache after dependency injection.
     */
    private void initializeProviderCache() {
        if (providerCache == null) {
            List<NotificationProviderService> allProviders = List.of(
                    digiGovProviderService,
                    smtpProviderService
            );

            providerCache = allProviders.stream()
                    .collect(Collectors.toMap(
                            NotificationProviderService::getProviderType,
                            Function.identity()
                    ));

            log.info("Initialized notification provider cache with {} providers: {}",
                    providerCache.size(),
                    providerCache.keySet());
        }
    }

    /**
     * Get provider service based on provider type and channel.
     *
     * @param providerType The provider type (DIGIGOV, SMTP, etc.)
     * @param channel The notification channel (SMS, EMAIL)
     * @param config The notification configuration
     * @return The appropriate provider service
     * @throws BusinessException if provider not found or doesn't support channel
     */
    public NotificationProviderService getProvider(ProviderType providerType,
                                                    NotificationChannel channel,
                                                    NotificationConfig config) {
        initializeProviderCache();

        log.debug("Getting provider for type: {}, channel: {}, businessId: {}",
                providerType, channel, config.getBusinessId());

        NotificationProviderService provider = providerCache.get(providerType);

        if (provider == null) {
            log.error("No provider implementation found for type: {}", providerType);
            throw new BusinessException(
                    "PROVIDER_NOT_FOUND",
                    "No provider implementation found for type: " + providerType
            );
        }

        if (!provider.supportsChannel(channel)) {
            log.error("Provider {} does not support channel: {}", providerType, channel);
            throw new BusinessException(
                    "CHANNEL_NOT_SUPPORTED",
                    String.format("Provider %s does not support channel: %s", providerType, channel)
            );
        }

        if (!provider.validateConfiguration(config)) {
            log.error("Invalid configuration for provider: {}", providerType);
            throw new BusinessException(
                    "INVALID_PROVIDER_CONFIG",
                    "Invalid configuration for provider: " + providerType
            );
        }

        log.debug("Successfully resolved provider: {} for channel: {}", providerType, channel);
        return provider;
    }

    /**
     * Get provider service based only on provider type.
     * Use this when channel validation is not needed.
     *
     * @param providerType The provider type
     * @return The provider service
     * @throws BusinessException if provider not found
     */
    public NotificationProviderService getProvider(ProviderType providerType) {
        initializeProviderCache();

        NotificationProviderService provider = providerCache.get(providerType);

        if (provider == null) {
            log.error("No provider implementation found for type: {}", providerType);
            throw new BusinessException(
                    "PROVIDER_NOT_FOUND",
                    "No provider implementation found for type: " + providerType
            );
        }

        return provider;
    }

    /**
     * Get provider from configuration.
     * Extracts provider type from config and returns the appropriate provider.
     *
     * @param config The notification configuration
     * @param channel The notification channel
     * @return The provider service
     */
    public NotificationProviderService getProviderFromConfig(NotificationConfig config,
                                                              NotificationChannel channel) {
        if (config == null || config.getProviderType() == null) {
            log.warn("Config or provider type is null, defaulting to DIGIGOV");
            return getProvider(ProviderType.DIGIGOV, channel, config);
        }

        return getProvider(config.getProviderType(), channel, config);
    }

    /**
     * Get all available providers.
     *
     * @return List of all registered providers
     */
    public List<NotificationProviderService> getAllProviders() {
        initializeProviderCache();
        return List.copyOf(providerCache.values());
    }

    /**
     * Check if a provider type is registered.
     *
     * @param providerType The provider type to check
     * @return true if registered, false otherwise
     */
    public boolean isProviderRegistered(ProviderType providerType) {
        initializeProviderCache();
        return providerCache.containsKey(providerType);
    }
}
