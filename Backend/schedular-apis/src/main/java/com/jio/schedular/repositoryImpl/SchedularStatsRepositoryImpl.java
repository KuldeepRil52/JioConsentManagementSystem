package com.jio.schedular.repositoryImpl;

import com.jio.schedular.constant.Constants;
import com.jio.schedular.entity.SchedularStats;
import com.jio.schedular.multitenancy.TenantMongoTemplateProvider;
import com.jio.schedular.repository.SchedularStatsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
@Slf4j
public class SchedularStatsRepositoryImpl implements SchedularStatsRepository {

    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;

    @Autowired
    public SchedularStatsRepositoryImpl(TenantMongoTemplateProvider tenantMongoTemplateProvider) {
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
    }

    @Override
    public void saveTenantSummary(String tenantId, SchedularStats stats) {
        MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);
        mongoTemplate.save(stats, Constants.SCHEDULAR_STATS);
    }

    @Override
    public List<SchedularStats> findStatsByDateRangeAndBusinessId(String tenantId, Instant startDate, Instant endDate, String businessId) {
        MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);

        Query query = buildQuery(null, businessId, startDate, endDate);
        return mongoTemplate.find(query, SchedularStats.class, Constants.SCHEDULAR_STATS);
    }

    @Override
    public List<SchedularStats> findStatsByJobName(String tenantId, String jobName, Instant startDate, Instant endDate, String businessId) {
        MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);

        Query query = buildQuery(jobName, businessId, startDate, endDate);
        return mongoTemplate.find(query, SchedularStats.class, Constants.SCHEDULAR_STATS);
    }

    /**
     * Shared internal query builder.
     */
    private Query buildQuery(String jobName, String businessId, Instant startDate, Instant endDate) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (jobName != null && !jobName.isEmpty()) {
            criteriaList.add(Criteria.where("jobName").is(jobName));
        }

        if (businessId != null && !businessId.isEmpty()) {
            criteriaList.add(Criteria.where("resources").elemMatch(Criteria.where("businessId").is(businessId)));
        }

        if (startDate != null && endDate != null) {
            criteriaList.add(Criteria.where(Constants.TIMESTAMP).gte(startDate).lte(endDate));
        } else if (startDate != null) {
            criteriaList.add(Criteria.where(Constants.TIMESTAMP).gte(startDate));
        } else if (endDate != null) {
            criteriaList.add(Criteria.where(Constants.TIMESTAMP).lte(endDate));
        }

        Criteria finalCriteria = new Criteria();
        if (!criteriaList.isEmpty()) {
            finalCriteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        Query query = new Query(finalCriteria);
        query.with(Sort.by(Sort.Direction.DESC, Constants.TIMESTAMP));
        log.debug("[SchedularStatsRepositoryImpl] Query built: {}", query);
        return query;
    }
}
