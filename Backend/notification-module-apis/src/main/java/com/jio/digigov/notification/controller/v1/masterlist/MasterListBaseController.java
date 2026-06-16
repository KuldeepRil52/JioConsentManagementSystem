package com.jio.digigov.notification.controller.v1.masterlist;

import com.jio.digigov.notification.controller.BaseController;
import com.jio.digigov.notification.dto.response.masterlist.TenantMasterListResponseDto;
import com.jio.digigov.notification.entity.TenantMasterListConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * Base controller providing common functionality for master list related controllers.
 *
 * This controller extends BaseController and provides shared utility methods
 * that are used across different master list management controllers.
 */
@Slf4j
public abstract class MasterListBaseController extends BaseController {

    /**
     * Converts a TenantMasterListConfig entity to a response DTO.
     *
     * @param config the entity to convert
     * @return the response DTO
     */
    protected TenantMasterListResponseDto convertToResponse(TenantMasterListConfig config) {
        return TenantMasterListResponseDto.builder()
            .id(config.getId())
            .masterListConfig(config.getMasterListConfig())
            .description(config.getDescription())
            .createdAt(config.getCreatedAt())
            .updatedAt(config.getUpdatedAt())
            .entryCount(config.getEntryCount())
            .build();
    }
}