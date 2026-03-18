package com.jio.partnerportal.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SmscDetails {

    private String host;
    private int port;
    private String systemId;
    private String password; // In a real application, this should be encrypted/securely handled
    private String bindType; // e.g., "transceiver", "transmitter", "receiver"
    private String systemType;
    private int addressTON; // Type of Number
    private int addressNPI; // Numbering Plan Indicator
    private String sourceAddress; // Source address for messages
    private int destinationTON;
    private int destinationNPI;
    private int enquireLinkInterval; // Interval for sending enquire_link messages, in seconds
    private String encoding; // e.g., "USC2", "GSM"
    private String deliveryReceipts; // e.g., "ENABLED", "DISABLED"

}
