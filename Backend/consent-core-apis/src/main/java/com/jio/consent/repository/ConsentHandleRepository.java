package com.jio.consent.repository;

import com.jio.consent.entity.ConsentHandle;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

public interface ConsentHandleRepository{

    ConsentHandle save(ConsentHandle consentHandle);

    ConsentHandle getByConsentHandleId(String consentHandleId);

    List<ConsentHandle> findConsentHandleByParams(Map<String, Object> searchParams);

    List<ConsentHandle> findConsentHandleByParamsWithIds(Map<String, Object> searchParams);
    
    List<ConsentHandle> findConsentHandleByParamsWithPagination(Map<String, Object> searchParams, Pageable pageable);

    void deleteById(ObjectId id);

    long count();
}
