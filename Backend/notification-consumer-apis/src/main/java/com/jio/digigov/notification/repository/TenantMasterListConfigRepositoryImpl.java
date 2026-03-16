package com.jio.digigov.notification.repository;

import com.jio.digigov.notification.entity.TenantMasterListConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.query.FluentQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Implementation of TenantMasterListConfigRepository using MongoTemplate.
 * This is necessary for multi-tenant database support where we need to use
 * tenant-specific MongoTemplates.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-15
 */
@RequiredArgsConstructor
public class TenantMasterListConfigRepositoryImpl implements TenantMasterListConfigRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<TenantMasterListConfig> findByIsActive(Boolean isActive) {
        Query query = new Query(Criteria.where("isActive").is(isActive));
        TenantMasterListConfig result = mongoTemplate.findOne(query, TenantMasterListConfig.class);
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<TenantMasterListConfig> findByVersion(Integer version) {
        Query query = new Query(Criteria.where("version").is(version));
        TenantMasterListConfig result = mongoTemplate.findOne(query, TenantMasterListConfig.class);
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<TenantMasterListConfig> findTopByOrderByVersionDesc() {
        Query query = new Query().with(Sort.by(Sort.Direction.DESC, "version")).limit(1);
        TenantMasterListConfig result = mongoTemplate.findOne(query, TenantMasterListConfig.class);
        return Optional.ofNullable(result);
    }

    @Override
    public boolean existsByIsActive(Boolean isActive) {
        Query query = new Query(Criteria.where("isActive").is(isActive));
        return mongoTemplate.exists(query, TenantMasterListConfig.class);
    }

    // Standard MongoRepository methods
    @Override
    public <S extends TenantMasterListConfig> S save(S entity) {
        return mongoTemplate.save(entity);
    }

    @Override
    public <S extends TenantMasterListConfig> List<S> saveAll(Iterable<S> entities) {
        List<S> result = new ArrayList<>();
        for (S entity : entities) {
            result.add(mongoTemplate.save(entity));
        }
        return result;
    }

    @Override
    public Optional<TenantMasterListConfig> findById(String id) {
        TenantMasterListConfig result = mongoTemplate.findById(id, TenantMasterListConfig.class);
        return Optional.ofNullable(result);
    }

    @Override
    public boolean existsById(String id) {
        return mongoTemplate.exists(Query.query(Criteria.where("id").is(id)), TenantMasterListConfig.class);
    }

    @Override
    public List<TenantMasterListConfig> findAll() {
        return mongoTemplate.findAll(TenantMasterListConfig.class);
    }

    @Override
    public List<TenantMasterListConfig> findAllById(Iterable<String> ids) {
        Query query = new Query(Criteria.where("id").in(ids));
        return mongoTemplate.find(query, TenantMasterListConfig.class);
    }

    @Override
    public long count() {
        return mongoTemplate.count(new Query(), TenantMasterListConfig.class);
    }

    @Override
    public void deleteById(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        mongoTemplate.remove(query, TenantMasterListConfig.class);
    }

    @Override
    public void delete(TenantMasterListConfig entity) {
        mongoTemplate.remove(entity);
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        Query query = new Query(Criteria.where("id").in(ids));
        mongoTemplate.remove(query, TenantMasterListConfig.class);
    }

    @Override
    public void deleteAll(Iterable<? extends TenantMasterListConfig> entities) {
        entities.forEach(mongoTemplate::remove);
    }

    @Override
    public void deleteAll() {
        mongoTemplate.remove(new Query(), TenantMasterListConfig.class);
    }

    // Methods not implemented as they're not commonly used in this context
    @Override
    public List<TenantMasterListConfig> findAll(Sort sort) {
        throw new UnsupportedOperationException("findAll(Sort) not implemented");
    }

    @Override
    public Page<TenantMasterListConfig> findAll(Pageable pageable) {
        throw new UnsupportedOperationException("findAll(Pageable) not implemented");
    }

    @Override
    public <S extends TenantMasterListConfig> S insert(S entity) {
        return mongoTemplate.insert(entity);
    }

    @Override
    public <S extends TenantMasterListConfig> List<S> insert(Iterable<S> entities) {
        List<S> result = new ArrayList<>();
        for (S entity : entities) {
            result.add(mongoTemplate.insert(entity));
        }
        return result;
    }

    @Override
    public <S extends TenantMasterListConfig> Optional<S> findOne(Example<S> example) {
        throw new UnsupportedOperationException("findOne(Example) not implemented");
    }

    @Override
    public <S extends TenantMasterListConfig> List<S> findAll(Example<S> example) {
        throw new UnsupportedOperationException("findAll(Example) not implemented");
    }

    @Override
    public <S extends TenantMasterListConfig> List<S> findAll(Example<S> example, Sort sort) {
        throw new UnsupportedOperationException("findAll(Example, Sort) not implemented");
    }

    @Override
    public <S extends TenantMasterListConfig> Page<S> findAll(Example<S> example, Pageable pageable) {
        throw new UnsupportedOperationException("findAll(Example, Pageable) not implemented");
    }

    @Override
    public <S extends TenantMasterListConfig> long count(Example<S> example) {
        throw new UnsupportedOperationException("count(Example) not implemented");
    }

    @Override
    public <S extends TenantMasterListConfig> boolean exists(Example<S> example) {
        throw new UnsupportedOperationException("exists(Example) not implemented");
    }

    @Override
    public <S extends TenantMasterListConfig, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        throw new UnsupportedOperationException("findBy(Example, Function) not implemented");
    }
}