package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.ConfigHistory;

import java.util.List;
import java.util.Map;

public interface ConfigHistoryRepository {

    ConfigHistory save(ConfigHistory history);

    ConfigHistory findByConfigHistoryId(String configHistoryId);

    List<ConfigHistory> findConfigHistoryByParams(Map<String, String> searchParams);

}
