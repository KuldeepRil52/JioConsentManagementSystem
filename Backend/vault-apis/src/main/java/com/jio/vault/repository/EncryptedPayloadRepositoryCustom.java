package com.jio.vault.repository;

import com.jio.vault.documents.EncryptedPayload;
import java.util.Optional;

public interface EncryptedPayloadRepositoryCustom {

    Optional<EncryptedPayload> findByUuid(String dbName, String uuid);

    EncryptedPayload save(String dbName, EncryptedPayload payload);
}
