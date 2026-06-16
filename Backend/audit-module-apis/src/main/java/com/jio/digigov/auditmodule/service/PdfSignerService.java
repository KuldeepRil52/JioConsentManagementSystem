package com.jio.digigov.auditmodule.service;

import com.jio.digigov.auditmodule.service.dto.CertifyingOfficer;

public interface PdfSignerService {
    byte[] signPdf(byte[] pdfBytes, String tenantId) throws Exception;

    CertifyingOfficer getCertifyingOfficer(String tenantId) throws Exception;
}