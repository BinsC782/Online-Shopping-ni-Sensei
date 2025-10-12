package com.shopping.handlers;

import com.shopping.ServerMain;
import com.shopping.model.Product;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CartHandler implements HttpHandler {
    private static List<Product> cartItems = new ArrayList<>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            ServerMain.logRequest(method, path);

            switch (method) {
                case "POST":
                    handlePost(exchange);
                    break;
                case "GET":
                    handleGet(exchange);
                    break;
                default:
                    ServerMain.sendStatus(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            String errorJson = String.format("{\"error\":\"Cart operation failed: %s\"}", ServerMain.escape(e.getMessage()));
            ServerMain.sendJsonResponse(exchange, 500, errorJson);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = ServerMain.readBody(exchange);
        Map<String, Object> data = ServerMain.parseJsonToMap(body);
        String id = (String) data.get("id");
        String name = (String) data.get("name");
        Double priceDouble = (Double) data.get("price");
        double price = priceDouble != null ? priceDouble : 0.0;

        if (id == null || name == null) {
            ServerMain.sendJsonResponse(exchange, 400, "{\"error\":\"Invalid product data\"}");
            return;
        }

        Product product = new Product(id, name, price, 1); // Assuming stock=1 for simplicity
        cartItems.add(product);
        ServerMain.sendJsonResponse(exchange, 200, "{\"message\":\"Product added to cart\"}");
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < cartItems.size(); i++) {
            Product p = cartItems.get(i);
            if (i > 0) json.append(",");
            json.append("{\"id\":\"").append(p.getId()).append("\",\"name\":\"").append(p.getName()).append("\",\"price\":").append(p.getPrice()).append("}");
        }
        json.append("]");
        ServerMain.sendJsonResponse(exchange, 200, json.toString());
    }
}
