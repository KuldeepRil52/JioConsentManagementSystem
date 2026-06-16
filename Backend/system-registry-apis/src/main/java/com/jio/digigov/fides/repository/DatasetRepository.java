package com.jio.digigov.fides.repository;

import com.jio.digigov.fides.entity.Dataset;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DatasetRepository extends MongoRepository<Dataset, String> {
    Optional<Dataset> findByDatasetId(String datasetId);
}