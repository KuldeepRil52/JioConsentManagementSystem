package com.jio.vault.documents;

import com.jio.vault.constants.CollectionConstants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Getter
@Setter
@Document(collection = CollectionConstants.ENCRYPTED_PAYLOAD)
public class EncryptedPayload {

    @Id
    private String id;
    private String tenantId;
    private String businessId;
    private String dataCategoryType;
    private String dataCategoryValue;
    private String uuid;
    private String encryptedString;
    private LocalDateTime createdTimeStamp;
}
