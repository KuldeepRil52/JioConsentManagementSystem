package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.DataBreachReport;

import java.util.List;

public interface DataBreachReportRepository {

    DataBreachReport save(DataBreachReport report);

    DataBreachReport findById(String id);

    List<DataBreachReport> findAllByTenantId(String tenantId);

    DataBreachReport findLatestByTenantId(String tenantId);
}
