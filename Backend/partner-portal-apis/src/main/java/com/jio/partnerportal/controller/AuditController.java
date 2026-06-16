package com.jio.partnerportal.controller;

import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.ConfigHistory;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.service.ConfigHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/v1.0/audit")
public class AuditController {

    ConfigHistoryService configHistoryService;

    public AuditController(ConfigHistoryService configHistoryService) {
        this.configHistoryService = configHistoryService;
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search Config History",
            description = "Searches for configHistory configurations based on provided criteria like configType, businessId, operation and performedBy.",
            parameters = {
                    @Parameter(name = "businessId", description = "ID of the business to search for", in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "operation", description = "Operation Type", in = ParameterIn.QUERY, example = "CREATE", schema = @Schema(type = "string", allowableValues = {"CREATE", "UPDATE"})),
                    @Parameter(name = "configType", description = "Configuration Type", in = ParameterIn.QUERY, example = "SMTP", schema = @Schema(type = "string", allowableValues = {"SMSC", "SMTP", "GRIEVANCE", "DPO", "CONSENT", "SYSTEM"})),
                    @Parameter(name = "configHistoryId", description = "Audit ID", in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "performedBy", description = "user ID of the user who performed the operation", in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>"))
            })
    public ResponseEntity<SearchResponse<ConfigHistory>> search(@Parameter(hidden = true) @RequestParam Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {
        SearchResponse<ConfigHistory> response = this.configHistoryService.search(reqParams,req);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
