package com.jio.partnerportal.repositoryImpl;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.entity.DataType;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.repository.DataTypeRepository;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class DataTypeRepositoryImpl implements DataTypeRepository {

    private final TenantMongoTemplateProvider mongoTemplateProvider;

    public DataTypeRepositoryImpl(TenantMongoTemplateProvider mongoTemplateProvider){
        this.mongoTemplateProvider = mongoTemplateProvider;
    }

    @Override
    public DataType save(DataType dataType) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.save(dataType);    }

    @Override
    public DataType findByDataTypeId(String dataTypeId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("dataTypeId").is(dataTypeId);
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, DataType.class);
    }

    @Override
    public List<DataType> findDataTypeByParams(Map<String, String> searchParams) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        for (Map.Entry<String, String> entry : searchParams.entrySet()) {
            criteria.and(entry.getKey()).is(entry.getValue());
        }

        Query query = new Query(criteria);
        query.fields().exclude("_id");
        return mongoTemplate.find(query, DataType.class);
    }

    @Override
    public long count() {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        return mongoTemplate.count(new Query(), DataType.class);
    }

    @Override
    public boolean existsByDataTypeName(String dataTypeName) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("dataTypeName").is(dataTypeName);
        Query query = new Query(criteria);
        return mongoTemplate.exists(query, DataType.class);
    }

    @Override
    public boolean existsByDataTypeNameExcludingDataTypeId(String dataTypeName, String excludeDataTypeId) {
        MongoTemplate mongoTemplate = this.mongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));
        Criteria criteria = new Criteria();
        criteria.and("dataTypeName").is(dataTypeName);
        criteria.and("dataTypeId").ne(excludeDataTypeId);
        Query query = new Query(criteria);
        return mongoTemplate.exists(query, DataType.class);
    }

}
