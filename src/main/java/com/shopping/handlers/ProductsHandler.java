package com.shopping.handlers;

import com.shopping.ServerMain;
import com.shopping.model.Product;
import com.shopping.service.ShoppingService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

public class ProductsHandler implements HttpHandler {
    private final ShoppingService shoppingService;

    public ProductsHandler(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            ServerMain.logRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath());
            
            // Only allow GET requests
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                ServerMain.sendStatus(exchange, 405, "Method Not Allowed");
                return;
            }
            
            // Get search query parameter
            String query = exchange.getRequestURI().getQuery();
            String searchTerm = null;
            if (query != null && query.contains("q=")) {
                searchTerm = query.split("q=")[1];
                if (searchTerm != null) {
                    searchTerm = java.net.URLDecoder.decode(searchTerm, "UTF-8");
                }
            }
            
            // Get products based on search term
            List<Product> products;
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                products = shoppingService.searchProducts(searchTerm);
            } else {
                products = shoppingService.getProducts();
            }
            
            // Convert products to JSON
            String jsonResponse = ServerMain.toJsonProducts(products);
            ServerMain.sendJsonResponse(exchange, 200, jsonResponse);
            
        } catch (Exception e) {
            e.printStackTrace();
            String errorJson = String.format(
                "{\"error\":\"Failed to fetch products: %s\"}", 
                ServerMain.escape(e.getMessage())
            );
            ServerMain.sendJsonResponse(exchange, 500, errorJson);
        }
    }
}
