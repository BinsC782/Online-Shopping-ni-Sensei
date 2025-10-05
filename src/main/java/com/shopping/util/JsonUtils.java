package com.shopping.util;

import com.shopping.model.Product;
import java.util.List;

public class JsonUtils {
    
    /**
     * Convert a list of products to JSON array string
     * @param products List of products to convert
     * @return JSON array string
     */
    public static String toJson(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return "[]";
        }
        
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            json.append(toJson(p));
            if (i < products.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");
        return json.toString();
    }
    
    /**
     * Convert a single product to JSON object string
     * @param product Product to convert
     * @return JSON object string
     */
    public static String toJson(Product product) {
        if (product == null) {
            return "{}";
        }
        
        return String.format(
            "{\"id\":\"%s\",\"name\":\"%s\",\"price\":%.2f,\"description\":\"%s\",\"imageUrl\":\"%s\",\"category\":\"%s\"}",
            escapeJson(product.getId()),
            escapeJson(product.getName()),
            product.getPrice(),
            escapeJson(product.getDescription()),
            escapeJson(product.getImage() != null ? product.getImage() : ""),
            escapeJson(product.getCategory())
        );
    }
    
    /**
     * Escape special characters in JSON strings
     * @param input String to escape
     * @return Escaped string
     */
    private static String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
