package com.jio.consent.repositoryImpl;

import com.jio.consent.constant.Constants;
import com.jio.consent.entity.ProcessorActivity;
import com.jio.consent.multitenancy.TenantMongoTemplateProvider;
import com.jio.consent.repository.ProcessorActivityRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProcessorActivityRepositoryImpl implements ProcessorActivityRepository {

    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;

    @Autowired
    public ProcessorActivityRepositoryImpl(TenantMongoTemplateProvider tenantMongoTemplateProvider) {
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
    }

    @Override
    public List<ProcessorActivity> findByProcessorActivityIds(List<String> processorActivityIds) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        
        // Match documents with the given processorActivityIds
        MatchOperation matchOperation = Aggregation.match(
                Criteria.where("processorActivityId").in(processorActivityIds)
        );
        
        // Sort by processorActivityId (ascending) and version (descending)
        // This ensures that for each processorActivityId, the highest version comes first
        SortOperation sortOperation = Aggregation.sort(
                Sort.by(Sort.Direction.ASC, "processorActivityId")
                        .and(Sort.by(Sort.Direction.DESC, "version"))
        );
        
        // Group by processorActivityId and take the first document (highest version)
        GroupOperation groupOperation = Aggregation.group("processorActivityId")
                .first("$$ROOT").as("latest");
        
        // Replace root to extract the document from the group result
        Aggregation aggregation = Aggregation.newAggregation(
                matchOperation,
                sortOperation,
                groupOperation,
                Aggregation.replaceRoot("latest")
        );
        
        AggregationResults<ProcessorActivity> results = mongoTemplate.aggregate(
                aggregation,
                "processor_activities",
                ProcessorActivity.class
        );
        
        return results.getMappedResults();
    }
}


