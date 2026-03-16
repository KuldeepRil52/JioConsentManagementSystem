package com.jio.digigov.fides.integration.mask.impl;

import com.jio.digigov.fides.dto.CustomerIdentifiers;
import com.jio.digigov.fides.integration.mask.DbDataMasker;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class MongoDbDataMasker implements DbDataMasker {

    @Override
    @SuppressWarnings("unchecked")
    public void maskData(
            Map<String, Object> details,
            String datasetYaml,
            Set<String> dataItems,
            CustomerIdentifiers customerIdentifiers) {

        log.info("Starting data masking for MongoDB");
        log.debug("Data Items to mask: {}", dataItems);

        Map<String, Object> yaml = new Yaml().load(datasetYaml);

        //dataset is a MAP, not a LIST
        Map<String, Object> dataset =
                (Map<String, Object>) yaml.get("dataset");

        if (dataset == null) {
            log.warn("No dataset defined in datasetYaml, skipping masking");
            return;
        }
        Set<String> normalizedWithdrawn =
                dataItems.stream()
                        .map(this::normalize)
                        .collect(Collectors.toSet());


        //collections come from dataset
        List<Map<String, Object>> collections =
                (List<Map<String, Object>>) dataset.get("collections");

        if (collections == null || collections.isEmpty()) {
            log.warn("No collections defined in datasetYaml, skipping masking");
            return;
        }

        String uri = buildUri(details);
        String database = details.get("database").toString();
        String identityCategory = identityCategory(customerIdentifiers);
        log.info(
                "MongoMasking params | uri={} | database={} | identityCategory={}",
                uri,
                database,
                identityCategory
        );

        try (MongoClient client = MongoClients.create(uri)) {

            Set<String> userIds = new HashSet<>();
            String primaryCollectionName = null;
            String identityField = null;

            /* -------------------------------------------------
             * PASS 1: Identify & mask PRIMARY collection
             * ------------------------------------------------- */
            for (Map<String, Object> collectionDef : collections) {

                List<Map<String, Object>> fields =
                        (List<Map<String, Object>>) collectionDef.get("fields");

                if (fields == null) continue;

                Optional<Map<String, Object>> identityFieldOpt =
                        fields.stream()
                                .filter(f -> {
                                    List<String> cats =
                                            (List<String>) f.get("data_categories");
                                    return cats != null && cats.contains(identityCategory);
                                })
                                .findFirst();

                Optional<Map<String, Object>> userIdFieldOpt =
                        fields.stream()
                                .filter(f -> {
                                    List<String> cats =
                                            (List<String>) f.get("data_categories");
                                    return cats != null && cats.contains("user.unique_id");
                                })
                                .findFirst();

                if (identityFieldOpt.isPresent() && userIdFieldOpt.isPresent()) {

                    primaryCollectionName =
                            collectionDef.get("name").toString();

                    identityField =
                            identityFieldOpt.get().get("name").toString();

                    String userIdField =
                            userIdFieldOpt.get().get("name").toString();
                    log.info("PASS-1 | UserId field             : {}", userIdField);

                    MongoCollection<Document> coll =
                            client.getDatabase(database)
                                    .getCollection(primaryCollectionName);

                    coll.find(new Document(identityField,
                                    customerIdentifiers.getValue()))
                            .forEach(d ->
                                    userIds.add(d.getString(userIdField)));

                    maskCollection(
                            coll,
                            fields,
                            normalizedWithdrawn,
                            new Document(identityField,
                                    customerIdentifiers.getValue())
                    );

                    break; // only one primary collection
                }
            }

            if (primaryCollectionName == null || userIds.isEmpty()) {
                log.info(
                        "No primary collection or users found for {}, skipping related masking",
                        customerIdentifiers.getValue()
                );
                return;
            }

            /* -------------------------------------------------
             * PASS 2: Mask RELATED collections
             * ------------------------------------------------- */
            log.debug("PASS-1 | Primary collection       : {}", primaryCollectionName);
            log.debug("PASS-1 | Identity field           : {}", identityField);
            log.debug("PASS-1 | Identity value           : {}", customerIdentifiers.getValue());
            log.debug("PASS-1 | Collected userIds (size) : {}", userIds.size());
            log.debug("PASS-1 | Collected userIds (vals) : {}", userIds);

            for (Map<String, Object> collectionDef : collections) {

                String collectionName =
                        collectionDef.get("name").toString();


                if (collectionName.equals(primaryCollectionName)) {
                    continue;
                }

                List<Map<String, Object>> fields =
                        (List<Map<String, Object>>) collectionDef.get("fields");

                if (fields == null) continue;

                Optional<Map<String, Object>> userIdFieldOpt =
                        fields.stream()
                                .filter(f -> {
                                    List<String> cats =
                                            (List<String>) f.get("data_categories");
                                    return cats != null &&
                                            cats.contains("user.unique_id");
                                })
                                .findFirst();

                if (userIdFieldOpt.isEmpty()) {
                    continue;
                }

                String userIdField =
                        userIdFieldOpt.get().get("name").toString();

                MongoCollection<Document> coll =
                        client.getDatabase(database)
                                .getCollection(collectionName);


                maskCollection(
                        coll,
                        fields,
                        normalizedWithdrawn,
                        new Document(userIdField,
                                new Document("$in", userIds))
                );
            }
        }
    }


    /* -------------------------------------------------
     * Mask helper
     * ------------------------------------------------- */

    private void maskCollection(
            MongoCollection<Document> coll,
            List<Map<String, Object>> fields,
            Set<String> dataItems,
            Document filter) {

        Document setExpressions = new Document();

        log.debug(
                "MASK | Collection={} | Withdrawn dataItems={}",
                coll.getNamespace().getCollectionName(),
                dataItems
        );

        for (Map<String, Object> field : fields) {

            String fieldName = field.get("name").toString();
            List<String> dataCategories =
                    (List<String>) field.get("data_categories");
            String joinedFieldCategory =
                    dataCategories == null
                            ? null
                            : String.join("", dataCategories);

            String normalizedFieldCategory = normalize(joinedFieldCategory);

            boolean shouldMask =
                    dataCategories != null && dataItems.contains(normalizedFieldCategory);

            log.debug(
                    "MASK | Field={} | data_categories={} | shouldMask={}",
                    fieldName,
                    dataCategories,
                    shouldMask
            );

            if (shouldMask) {
                setExpressions.put(
                        fieldName,
                        buildMaskedExpression(fieldName)
                );
            }
        }

        if (setExpressions.isEmpty()) {
            log.info(
                    "MASK | Collection={} | No fields qualified for masking for filter={}",
                    coll.getNamespace().getCollectionName(),
                    filter
            );
            return;
        }

        coll.updateMany(filter, List.of(new Document("$set", setExpressions)));

        log.info(
                "MASK | Collection={} | Masked fields={} | filter={}",
                coll.getNamespace().getCollectionName(),
                setExpressions.keySet(),
                filter
        );
    }


    /* -------------------------------------------------
     * Type-safe masking expression
     * ------------------------------------------------- */
    private Document buildMaskedExpression(String fieldName) {

        return new Document("$switch",
                new Document("branches", List.of(

                        new Document("case",
                                new Document("$eq",
                                        List.of(new Document("$type", "$" + fieldName), "string")))
                                .append("then", "REDACTED"),

                        new Document("case",
                                new Document("$in",
                                        List.of(new Document("$type", "$" + fieldName),
                                                List.of("int", "long", "double", "decimal"))))
                                .append("then", 0),

                        new Document("case",
                                new Document("$eq",
                                        List.of(new Document("$type", "$" + fieldName), "bool")))
                                .append("then", false),

                        new Document("case",
                                new Document("$eq",
                                        List.of(new Document("$type", "$" + fieldName), "date")))
                                .append("then", null),

                        new Document("case",
                                new Document("$eq",
                                        List.of(new Document("$type", "$" + fieldName), "array")))
                                .append("then", List.of()),

                        new Document("case",
                                new Document("$eq",
                                        List.of(new Document("$type", "$" + fieldName), "object")))
                                .append("then", new Document())
                ))
                        .append("default", null)
        );
    }

    private String buildUri(Map<String, Object> details) {

        String host = details.get("host").toString();
        int port = Integer.parseInt(details.get("port").toString());
        String database = details.get("database").toString();

        Object username = details.get("username");
        Object password = details.get("password");

        if (username != null && password != null) {
            return String.format(
                    "mongodb://%s:%s@%s:%d/%s?authSource=admin",
                    username, password, host, port, database
            );
        }

        return String.format(
                "mongodb://%s:%d/%s",
                host, port, database
        );
    }

    private static String identityCategory(CustomerIdentifiers identifier) {
        return switch (identifier.getType()) {
                case EMAIL -> "Email Address";
                case MOBILE -> "Phone Number (Mobile / Landline)";
        };
    }

    private String normalize(String s) {
        return s == null
                ? null
                : s.toLowerCase()
                .replaceAll("[\\s()\\[\\],/]", "");
    }

}