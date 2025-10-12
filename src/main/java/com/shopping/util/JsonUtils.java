package com.shopping.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.shopping.model.Product;
import java.util.List;

/**
 * Utility class for JSON serialization and deserialization using Jackson
 */
public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convert a list of products to JSON array string
     * @param products List of products to convert
     * @return JSON array string
     * @throws JsonProcessingException if serialization fails
     */
    public static String toJson(List<Product> products) throws JsonProcessingException {
        if (products == null || products.isEmpty()) {
            return "[]";
        }
        return objectMapper.writeValueAsString(products);
    }

    /**
     * Convert a single product to JSON object string
     * @param product Product to convert
     * @return JSON object string
     * @throws JsonProcessingException if serialization fails
     */
    public static String toJson(Product product) throws JsonProcessingException {
        if (product == null) {
            return "{}";
        }
        return objectMapper.writeValueAsString(product);
    }

    /**
     * Convert a JSON string back to a Product object
     * @param json JSON string to deserialize
     * @return Product object
     * @throws JsonProcessingException if deserialization fails
     */
    public static Product fromJson(String json, Class<Product> clazz) throws JsonProcessingException {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        return objectMapper.readValue(json, clazz);
    }

    /**
     * Convert a JSON string back to a list of Product objects
     * @param json JSON string to deserialize
     * @return List of Product objects
     * @throws JsonProcessingException if deserialization fails
     */
    public static List<Product> fromJsonList(String json) throws JsonProcessingException {
        if (json == null || json.trim().isEmpty()) {
            return List.of();
        }
        return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, Product.class));
    }
}
