package com.jio.digigov.auditmodule.service;

public interface SignPdfService {
    byte[] generateAndSignForm65B(String consentId, String decryptedJson, String referenceId,
                                  String storedHash, String computedHash, boolean valid,
                                  String auditRecord, String tenantId, String businessId,
                                  String previousChain, String currentChainHash, String status) throws Exception;
}