package com.jio.digigov.fides.entity;

import com.jio.digigov.fides.enumeration.DeferredReasonType;
import com.jio.digigov.fides.enumeration.PIIStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
@Data
public class PIIItem {

    @NonNull
    private String piiItem;
    @NonNull
    private String sourceSystem;
    @NonNull
    private String location;
    @NonNull
    private PIIStatus status;

    private DeferredReasonType deferredReasonType;
    private String deferredReasonMessage;
    private Set<String> consentIds; // optional, only for ACTIVE_CONSENT

    public void resolveDeferredReason() {
        if (deferredReasonType == null) return;

        switch (deferredReasonType) {
            case ACTIVE_CONSENT:
                this.deferredReasonMessage =
                        "Active Consents Exist: " + consentIds;
                break;

            case TRAI_REGULATION:
                this.deferredReasonMessage =
                        "Regulatory Hold: Telecom Data Retention (TRAI)";
                break;

            case OTHER:
                this.deferredReasonMessage =
                        "Deferred for other reasons";
                break;
        }
    }
}