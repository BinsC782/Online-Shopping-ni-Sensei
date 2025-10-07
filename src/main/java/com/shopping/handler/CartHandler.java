package com.shopping.handler;

import com.shopping.service.ShoppingService;
import com.shopping.model.OrderItem;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CartHandler implements HttpHandler {
    private final ShoppingService shoppingService;

    public CartHandler(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            
            // Handle CORS preflight
            if ("OPTIONS".equals(method)) {
                handleCorsPreflight(exchange);
                return;
            }

            // Get username from session or token
            String username = getUsernameFromRequest(exchange);
            if (username == null) {
                sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }

            switch (method) {
                case "GET":
                    handleGetCart(exchange, username);
                    break;
                case "POST":
                    handleAddToCart(exchange, username);
                    break;
                case "PUT":
                    handleUpdateCart(exchange, username);
                    break;
                case "DELETE":
                    handleRemoveFromCart(exchange, username);
                    break;
                default:
                    sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleGetCart(HttpExchange exchange, String username) throws IOException {
        try {
            List<OrderItem> cartItems = shoppingService.getUserCart(username);
            String response = formatCartResponse(cartItems);
            sendResponse(exchange, 200, response);
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"Failed to get cart: " + e.getMessage() + "\"}");
        }
    }

    private void handleAddToCart(HttpExchange exchange, String username) throws IOException {
        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String[] parts = requestBody.split("&");
            
            String productId = null;
            int quantity = 1; // Default quantity
            
            for (String part : parts) {
                String[] keyValue = part.split("=");
                if (keyValue.length == 2) {
                    if (keyValue[0].equals("productId")) {
                        productId = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name());
                    } else if (keyValue[0].equals("quantity")) {
                        quantity = Integer.parseInt(java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name()));
                    }
                }
            }
            
            if (productId == null) {
                sendResponse(exchange, 400, "{\"error\":\"Product ID is required\"}");
                return;
            }
            
            shoppingService.addToCart(username, productId, quantity);
            sendResponse(exchange, 200, "{\"status\":\"success\"}");
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid request: " + e.getMessage() + "\"}");
        }
    }

    private void handleUpdateCart(HttpExchange exchange, String username) throws IOException {
        // Similar to addToCart but with quantity update
        // Implementation needed based on requirements
        sendResponse(exchange, 501, "{\"error\":\"Not implemented\"}");
    }

    private void handleRemoveFromCart(HttpExchange exchange, String username) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length < 4) {
                sendResponse(exchange, 400, "{\"error\":\"Invalid request\"}");
                return;
            }
            
            String productId = parts[3]; // Assuming URL is /api/cart/{productId}
            shoppingService.removeFromCart(username, productId);
            sendResponse(exchange, 200, "{\"status\":\"success\"}");
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid request: " + e.getMessage() + "\"}");
        }
    }

    private String getUsernameFromRequest(HttpExchange exchange) {
        // Extract username from session or token
        // This is a placeholder - implement your actual authentication logic
        return "test"; // For testing
    }

    private String formatCartResponse(List<OrderItem> items) {
        StringBuilder json = new StringBuilder("[");
        String delimiter = "";
        for (OrderItem item : items) {
            json.append(delimiter)
                .append("{\"productId\":\"")
                .append(item.getProductId())
                .append("\",\"name\":\"")
                .append(escapeJson(item.getName()))
                .append("\",\"price\":")
                .append(item.getPrice())
                .append(",\"quantity\":")
                .append(item.getQuantity())
                .append("}");
            delimiter = ",";
        }
        json.append("]");
        return json.toString();
    }

    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private void handleCorsPreflight(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(204, -1);
        exchange.getResponseBody().close();
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }
}