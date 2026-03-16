package com.jio.consent.repositoryImpl;

import com.jio.consent.constant.Constants;
import com.jio.consent.entity.Template;
import com.jio.consent.multitenancy.TenantMongoTemplateProvider;
import com.jio.consent.repository.TemplateRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class TemplateRepositoryImpl implements TemplateRepository {

    TenantMongoTemplateProvider tenantMongoTemplateProvider;

    @Autowired
    public TemplateRepositoryImpl(TenantMongoTemplateProvider tenantMongoTemplateProvider) {
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
    }

    public Template saveTemplate(Template template) {
        return tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER)).save(template);
    }

    @Override
    public Template getByTemplateId(String templateId) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("templateId").is(templateId);
        Query query = new Query();
        query.addCriteria(criteria);
        // Sort by version in descending order and get the first result (highest version)
        query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "version"));
        query.limit(1);
        return mongoTemplate.findOne(query, Template.class);
    }

    @Override
    public List<Template> findTemplatesByParams(Map<String, Object> searchParams) {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        
        for (Map.Entry<String, Object> entry : searchParams.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Handle type conversion for specific fields
            if ("version".equals(key) && value instanceof String) {
                try {
                    int intValue = Integer.parseInt((String) value);
                    criteria.and(key).is(intValue);
                } catch (NumberFormatException e) {
                    // If conversion fails, skip this parameter
                    continue;
                }
            } else {
                criteria.and(key).is(value);
            }
        }

        Query query = new Query(criteria);
        query.fields().exclude("_id");
        return mongoTemplate.find(query, Template.class);
    }

    @Override
    public long count() {
        MongoTemplate mongoTemplate = this.tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.count(new Query(), Template.class);
    }
}
