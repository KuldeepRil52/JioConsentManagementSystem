package com.jio.digigov.fides.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DatasetYamlUtil {

    private DatasetYamlUtil() {}

    /**
     * Returns PII data items governed by TRAI consent template
     */
    public static Set<String> extractTraiDataItems(String datasetYaml) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(datasetYaml);

            if (root == null) return Set.of();

            Map<String, Object> dataset = (Map<String, Object>) root.get("dataset");
            if (dataset == null) return Set.of();

            List<Map<String, Object>> collections = (List<Map<String, Object>>) dataset.get("collections");
            if (collections == null) return Set.of();

            Set<String> traiItems = new HashSet<>();

            for (Map<String, Object> collection : collections) {
                List<Map<String, Object>> fields = (List<Map<String, Object>>) collection.get("fields");
                if (fields == null) continue;

                for (Map<String, Object> field : fields) {
                    String consentTemplate = (String) field.get("consent_template");
                    if (!"TRAI".equalsIgnoreCase(consentTemplate)) continue;

                    Object dataCategoriesObj = field.get("data_categories");
                    List<String> categories = parseDataCategories(dataCategoriesObj);
                    traiItems.addAll(categories);
                }
            }
            return traiItems;

        } catch (Exception e) {
            return Set.of();
        }
    }

    private static List<String> parseDataCategories(Object dataCategoriesObj) {
        List<String> categories = new ArrayList<>();
        if (dataCategoriesObj instanceof List) {
            List<?> list = (List<?>) dataCategoriesObj;
            if (list.size() == 1) {
                categories.add(list.get(0).toString().trim());
            } else if (list.size() > 1) {
                String first = list.get(0).toString().trim();
                String last = list.get(list.size() - 1).toString().trim();
                if (first.contains("(") && last.contains(")")) {
                    // Join as one category
                    String joined = list.stream().map(Object::toString).reduce((a, b) -> a + ", " + b).orElse("");
                    categories.add(joined.trim());
                } else {
                    // Add each
                    for (Object item : list) {
                        categories.add(item.toString().trim());
                    }
                }
            }
        } else if (dataCategoriesObj instanceof String) {
            String str = (String) dataCategoriesObj;
            String[] parts = str.split(",");
            for (String part : parts) {
                categories.add(part.trim());
            }
        }
        return categories;
    }

    public static String extractDatasetName(String datasetYaml) {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(datasetYaml);

        if (root == null) return null;

        Map<String, Object> dataset = (Map<String, Object>) root.get("dataset");
        return dataset != null ? (String) dataset.get("name") : null;
    }
}