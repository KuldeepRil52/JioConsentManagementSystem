package com.jio.digigov.grievance.enumeration;

/**
 * Enumeration for notification recipient types.
 *
 * <p>Used to identify the type of entity that should receive notifications
 * in the DPDP (Data Protection and Digital Privacy) notification system.</p>
 *
 * <p><b>Recipient Types:</b></p>
 * <ul>
 *   <li><b>DATA_PRINCIPAL</b> - The individual (data subject) whose personal data is being processed</li>
 *   <li><b>DATA_FIDUCIARY</b> - The data controller/fiduciary responsible for data processing decisions</li>
 *   <li><b>DATA_PROCESSOR</b> - Third-party entities that process data on behalf of the fiduciary</li>
 *   <li><b>DATA_PROTECTION_OFFICER</b> - The Data Protection Officer responsible for compliance and grievances</li>
 * </ul>
 *
 * <p><b>Usage in Callback URL Resolution:</b></p>
 * <pre>
 * // URL resolution based on recipient type
 * if (RecipientType.DATA_FIDUCIARY.name().equals(recipientType)) {
 *     // Fetch callback URL from NGConfiguration
 * } else if (RecipientType.DATA_PROCESSOR.name().equals(recipientType)) {
 *     // Fetch callback URL from DataProcessor entity
 * }
 * </pre>
 *
 * <p><b>Usage in Template Resolution:</b></p>
 * <pre>
 * // Template resolution based on recipient type
 * if (RecipientType.DATA_PRINCIPAL.name().equals(recipientType)) {
 *     // Resolve template for data principal notification
 * } else if (RecipientType.DATA_PROTECTION_OFFICER.name().equals(recipientType)) {
 *     // Resolve template for DPO notification
 *     // Fetch DPO email from dpo_configurations collection
 * }
 * </pre>
 *
 * @since 1.7.0
 */
public enum RecipientType {
    /** Data Principal - the individual (data subject) whose personal data is being processed */
    DATA_PRINCIPAL,

    /** Data Fiduciary - the primary data controller */
    DATA_FIDUCIARY,

    /** Data Processor - processes data on behalf of fiduciary */
    DATA_PROCESSOR,

    /** Data Protection Officer (DPO) - responsible for compliance and handling grievances */
    DATA_PROTECTION_OFFICER
}
