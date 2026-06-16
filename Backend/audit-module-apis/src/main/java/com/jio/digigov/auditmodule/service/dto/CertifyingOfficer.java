package com.jio.digigov.auditmodule.service.dto;

/**
 * Holds Certifying Officer details derived from signing certificate
 * Used for Section 65B PDF generation
 */
public record CertifyingOfficer(
        String name,
        String designation,
        String organisation,
        String contact
) {}
