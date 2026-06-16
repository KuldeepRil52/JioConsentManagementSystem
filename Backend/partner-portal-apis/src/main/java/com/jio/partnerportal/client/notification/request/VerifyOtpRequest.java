package com.jio.partnerportal.client.notification.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for verifying OTP via notification service")
public class VerifyOtpRequest {

    @Schema(description = "Event ID from OTP initiation", example = "EVT_20251107_73774")
    @JsonProperty("eventId")
    private String eventId;

    @Schema(description = "Transaction ID", example = "SYS-TXN-5c9a61b8-87a6-4bac-85f7-1e873209ce75")
    @JsonProperty("txnId")
    private String txnId;

    @Schema(description = "Encrypted OTP value", example = "m+C7xStv7syz8duwUqHGWWQNzpvQb9viMA+E4zKJw1Sp0D6PRZXsk46GNv5p2akYuYAPhHueXZ1AGmFiWpauIYs4RcC7h02gXPhzWUJhPqOGUDVZpBeE+LbMbx4pn98ODw0RLw+elgGlshGHfMu8QRPoN9Y4pbMJZWRLQj3yFrBz5BXSkNyFInQF78ojXCecHJtp31/vyzkohOWR+U7du1Pj50mzJy9F0nUJubmlpjUtU0yLxWZ/ROd01UqCtu8ZahvCFnWaY4d27zHzpwr3brJqs637Gd3yQvzTSEtCvJqfYWoOR/f4OdSL5saUZuKSYf+wIL2ryzDwWNsTkKko7Q==")
    @JsonProperty("encryptedOtpValue")
    private String encryptedOtpValue;
}

