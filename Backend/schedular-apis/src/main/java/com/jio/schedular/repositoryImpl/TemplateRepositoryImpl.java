package com.jio.schedular.repositoryImpl;

import com.jio.schedular.constant.Constants;
import com.jio.schedular.entity.Template;
import com.jio.schedular.multitenancy.TenantMongoTemplateProvider;
import com.jio.schedular.repository.TemplateRepository;
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
            criteria.and(entry.getKey()).is(entry.getValue());
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
