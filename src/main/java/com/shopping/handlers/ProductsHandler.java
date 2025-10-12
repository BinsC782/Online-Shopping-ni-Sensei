package com.shopping.handlers;

import com.shopping.ServerMain;
import com.shopping.model.*;
import com.shopping.service.ShoppingService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
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

            // Get products based on search term using ShoppingService
            List<Product> products;
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                products = shoppingService.searchProducts(searchTerm.trim());
            } else {
                products = shoppingService.getProducts();
            }

            // Convert products to JSON using ServerMain utility
            String jsonResponse = ServerMain.toJsonProducts(products);

            // Send JSON response
            byte[] response = jsonResponse.getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, response.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }

        } catch (Exception e) {
            e.printStackTrace();
            String errorJson = String.format(
                "{\"error\":\"Failed to fetch products: %s\"}",
                e.getMessage()
            );
            ServerMain.sendJsonResponse(exchange, 500, errorJson);
        }
    }
}
