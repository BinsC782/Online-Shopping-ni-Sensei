package com.shopping.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.shopping.service.ShoppingService;
import com.shopping.model.Product;
import com.shopping.util.JsonUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class ProductHandler implements HttpHandler {
    private final ShoppingService shoppingService;

    public ProductHandler(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            handleGetProducts(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
        }
    }

    private void handleGetProducts(HttpExchange exchange) throws IOException {
        try {
            String query = exchange.getRequestURI().getQuery();
            String searchTerm = null;
            
            if (query != null && query.startsWith("search=")) {
                searchTerm = query.substring(7); // Extract search term from query
            }
            
            List<Product> products;
            if (searchTerm != null && !searchTerm.isEmpty()) {
                products = shoppingService.searchProducts(searchTerm);
            } else {
                products = shoppingService.getProducts();
            }
            
            String response = JsonUtils.toJson(products);
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        } catch (Exception e) {
            String errorResponse = "{\"error\":\"Error retrieving products: " + e.getMessage() + "\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, errorResponse.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorResponse.getBytes());
            }
        }
    }
}
