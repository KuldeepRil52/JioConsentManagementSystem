package com.jio.digigov.grievance.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class DynamicUserDetailsFieldLoader {

    @Getter
    private static final Set<String> USER_DETAIL_KEYS = new HashSet<>();

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final String COLLECTION_NAME = "grievance"; // Change to your actual collection name

    @EventListener(ApplicationReadyEvent.class)
    public void loadUserDetailsFields() {
        try {
            log.info("[DynamicUserDetailsFieldLoader] Loading dynamic userDetails.* keys from MongoDB…");

            Aggregation agg = Aggregation.newAggregation(
                    Aggregation.match(Criteria.where("userDetails").exists(true)),
                    Aggregation.project().and("userDetails").as("userDetails"),
                    Aggregation.project()
                            .andExpression("objectToArray($userDetails)")
                            .as("userDetailsArray")
            );

            AggregationResults<Document> result =
                    mongoTemplate.aggregate(agg, COLLECTION_NAME, Document.class);

            Set<String> keys = new HashSet<>();

            for (Document doc : result.getMappedResults()) {
                List<Document> arr = (List<Document>) doc.get("userDetailsArray");
                if (arr != null) {
                    for (Document d : arr) {
                        Object key = d.get("k");
                        if (key != null) {
                            keys.add(key.toString());
                        }
                    }
                }
            }

            USER_DETAIL_KEYS.clear();
            USER_DETAIL_KEYS.addAll(keys);

            log.info("[DynamicUserDetailsFieldLoader] Loaded userDetails keys: {}", USER_DETAIL_KEYS);

        } catch (Exception ex) {
            log.error("[DynamicUserDetailsFieldLoader] Failed to load dynamic keys", ex);
        }
    }
}

