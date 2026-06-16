package com.jio.digigov.notification.enums;

/**
 * Enumeration for notification provider types.
 *
 * <p>Represents the different notification service providers that can be used
 * to send notifications. This enum provides type safety and abstraction for
 * multi-provider support throughout the notification system.</p>
 *
 * <p><b>Provider Types:</b></p>
 * <ul>
 *   <li><b>DIGIGOV</b> - DigiGov notification gateway (SMS and EMAIL via JioGov platform)</li>
 *   <li><b>SMTP</b> - Direct SMTP email delivery (for EMAIL channel only)</li>
 * </ul>
 *
 * <p><b>Key Differences:</b></p>
 * <table border="1">
 *   <tr>
 *     <th>Feature</th>
 *     <th>DIGIGOV</th>
 *     <th>SMTP</th>
 *   </tr>
 *   <tr>
 *     <td>Channels Supported</td>
 *     <td>SMS, EMAIL</td>
 *     <td>EMAIL only</td>
 *   </tr>
 *   <tr>
 *     <td>Template Approval</td>
 *     <td>Required (via DigiGov Admin API)</td>
 *     <td>Not required (direct DB save)</td>
 *   </tr>
 *   <tr>
 *     <td>Configuration</td>
 *     <td>baseUrl, clientId, clientSecret, sid</td>
 *     <td>host, port, username, password, fromEmail</td>
 *   </tr>
 *   <tr>
 *     <td>Authentication</td>
 *     <td>OAuth2 token-based</td>
 *     <td>SMTP AUTH (username/password)</td>
 *   </tr>
 * </table>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>
 * // In configuration
 * NotificationConfig config = NotificationConfig.builder()
 *     .businessId("BUS001")
 *     .providerType(ProviderType.SMTP)
 *     .build();
 *
 * // In template creation
 * NotificationTemplate template = NotificationTemplate.builder()
 *     .eventType("USER_REGISTRATION")
 *     .providerType(ProviderType.SMTP)
 *     .channelType(NotificationChannel.EMAIL)
 *     .build();
 *
 * // Provider selection in factory
 * NotificationProviderService provider = factory.getProvider(ProviderType.DIGIGOV, NotificationChannel.EMAIL);
 * </pre>
 *
 * @see NotificationChannel
 * @see com.jio.digigov.notification.entity.NotificationConfig
 * @see com.jio.digigov.notification.entity.template.NotificationTemplate
 * @since 2.0.0
 * @author Notification Service Team
 */
public enum ProviderType {
    /** DigiGov notification gateway provider */
    DIGIGOV,

    /** Direct SMTP email provider */
    SMTP
}
