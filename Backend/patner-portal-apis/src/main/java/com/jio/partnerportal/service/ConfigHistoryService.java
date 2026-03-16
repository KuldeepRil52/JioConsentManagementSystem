package com.jio.partnerportal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.ConfigType;
import com.jio.partnerportal.dto.Operation;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.ConfigHistory;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.ConfigHistoryRepository;
import com.jio.partnerportal.util.LogUtil;
import com.jio.partnerportal.util.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ConfigHistoryService {

    ConfigHistoryRepository configHistoryRepository;
    Utils utils;
    ObjectMapper objectMapper;

    @Autowired
    ConfigHistoryService(ConfigHistoryRepository configHistoryRepository,
                         Utils utils,ObjectMapper objectMapper) {
        this.configHistoryRepository = configHistoryRepository;
        this.utils = utils;
        this.objectMapper=objectMapper;
    }

    @Value("${config-history.search.parameters}")
    List<String> configHistorySearchParameter;


    public void createConfigHistoryEntry(Object payloadObject,
                                         String businessId,
                                         ConfigType configType,
                                         Operation operation) {

        String configHistoryId = UUID.randomUUID().toString();
        ConfigHistory configHistory = ConfigHistory.builder()
                .configHistoryId(configHistoryId)
                .payload(payloadObject)
                .configType(configType.toString())
                .businessId(businessId)
                .operation(operation.toString())
                .performedBy(ThreadContext.get("userId"))
                .build();

        this.configHistoryRepository.save(configHistory);
    }

    public SearchResponse<ConfigHistory> search(Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Search Config history";

        Map<String, String> searchParams = this.utils.filterRequestParam(reqParams, configHistorySearchParameter);
        List<ConfigHistory> mongoResponse = this.configHistoryRepository.findConfigHistoryByParams(searchParams);

        if (ObjectUtils.isEmpty(mongoResponse)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        LogUtil.logActivity(req, activity, "Success: Search Config history successfully");
        return SearchResponse.<ConfigHistory>builder()
                .searchList(mongoResponse)
                .build();
    }
}
