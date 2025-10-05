package com.shopping.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.shopping.model.Product;
import com.shopping.service.ShoppingService;
import com.shopping.util.JsonUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ProductsHandler implements HttpHandler {
    private final ShoppingService shoppingService;

    public ProductsHandler(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            // Set CORS headers
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1); // No content for preflight
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            
            // Handle GET /api/products
            if (path.matches("^/api/products$")) {
                handleGetProducts(exchange);
            } 
            // Handle GET /api/products/{id}
            else if (path.matches("^/api/products/\\d{6}$")) {
                String[] parts = path.split("/");
                String productId = parts[parts.length - 1];
                handleGetProduct(exchange, productId);
            } else {
                sendResponse(exchange, 404, "Not Found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }
    
    /**
     * Handle GET /api/products
     */
    private void handleGetProducts(HttpExchange exchange) throws IOException {
        try {
            // Get search term from query parameter if it exists
            String query = exchange.getRequestURI().getQuery();
            String searchTerm = null;
            
            if (query != null && query.startsWith("search=")) {
                searchTerm = query.substring(7); // Extract search term from query
                // URL decode the search term
                searchTerm = java.net.URLDecoder.decode(searchTerm, StandardCharsets.UTF_8.name());
            }
            
            List<Product> products;
            if (searchTerm != null && !searchTerm.isEmpty()) {
                products = shoppingService.searchProducts(searchTerm);
            } else {
                products = shoppingService.getProducts();
            }
            
            String response = JsonUtils.toJson(products);
            sendResponse(exchange, 200, response);
        } catch (Exception e) {
            e.printStackTrace();
            String errorResponse = "{\"error\":\"Error retrieving products: " + e.getMessage() + "\"}";
            sendResponse(exchange, 500, errorResponse);
        }
    }
    
    /**
     * Handle GET /api/products/{id}
     */
    private void handleGetProduct(HttpExchange exchange, String productId) throws IOException {
        try {
            Product product = shoppingService.getProductById(productId);
            if (product == null) {
                sendResponse(exchange, 404, "{\"error\":\"Product not found\"}");
                return;
            }
            
            String response = JsonUtils.toJson(product);
            sendResponse(exchange, 200, response);
        } catch (Exception e) {
            e.printStackTrace();
            String errorResponse = "{\"error\":\"Error retrieving product: " + e.getMessage() + "\"}";
            sendResponse(exchange, 500, errorResponse);
        }
    }
    
    /**
     * Helper method to send HTTP response
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        
        if (response == null) {
            exchange.sendResponseHeaders(statusCode, -1);
            return;
        }
        
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
