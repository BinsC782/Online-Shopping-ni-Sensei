package com.shopping.handler;

import com.shopping.ServerMain;
import com.shopping.service.ShoppingService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class CartHandler implements HttpHandler {
    private final ShoppingService shoppingService;

    public CartHandler(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            // Handle CORS preflight request
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                ServerMain.addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            
            // Add CORS headers to all responses
            ServerMain.addCorsHeaders(exchange);
            
            // Get username from session
            String sessionId = ServerMain.getSessionId(exchange);
            String username = ServerMain.getUsernameFromSession(sessionId);
            
            if (username == null) {
                ServerMain.sendErrorResponse(exchange, 401, "Not authenticated");
                return;
            }
            
            // Handle different HTTP methods
            String method = exchange.getRequestMethod().toUpperCase();
            switch (method) {
                case "GET":
                    handleGetCart(exchange, username);
                    break;
                case "POST":
                    handleAddToCart(exchange, username);
                    break;
                case "DELETE":
                    handleRemoveFromCart(exchange, username);
                    break;
                default:
                    ServerMain.sendErrorResponse(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            ServerMain.sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        } finally {
            exchange.close();
        }
    }
    
    private void handleGetCart(HttpExchange exchange, String username) throws IOException {
        try {
            List<com.shopping.model.OrderItem> cart = shoppingService.getUserCart(username);
            StringBuilder jsonBuilder = new StringBuilder("[");
            String separator = "";
            for (com.shopping.model.OrderItem item : cart) {
                jsonBuilder.append(separator)
                    .append("{\"productId\":\"")
                    .append(item.getProductId())
                    .append("\",\"quantity\":")
                    .append(item.getQuantity())
                    .append("}");
                separator = ",";
            }
            jsonBuilder.append("]");
            ServerMain.sendJsonResponse(exchange, 200, jsonBuilder.toString());
        } catch (Exception e) {
            throw new IOException("Failed to get cart: " + e.getMessage(), e);
        }
    }
    
    private void handleAddToCart(HttpExchange exchange, String username) throws IOException {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            
            String requestBody = reader.lines().collect(Collectors.joining("\n"));
            
            // Simple JSON parsing for productId and quantity
            String productId = null;
            int quantity = 1;
            
            try {
                String[] parts = requestBody.replaceAll("[{}\"\\s]", "").split(",");
                for (String part : parts) {
                    String[] keyValue = part.split(":");
                    if (keyValue.length == 2) {
                        if (keyValue[0].equals("productId")) {
                            productId = keyValue[1];
                        } else if (keyValue[0].equals("quantity")) {
                            quantity = Integer.parseInt(keyValue[1]);
                        }
                    }
                }
                
                if (productId == null) {
                    ServerMain.sendErrorResponse(exchange, 400, "Product ID is required");
                    return;
                }
                
                shoppingService.addToCart(username, productId, quantity);
                
                // Return the updated cart
                handleGetCart(exchange, username);
                
            } catch (NumberFormatException e) {
                ServerMain.sendErrorResponse(exchange, 400, "Invalid quantity");
            } catch (Exception e) {
                throw new IOException("Failed to add to cart: " + e.getMessage(), e);
            }
        }
    }
    
    private void handleRemoveFromCart(HttpExchange exchange, String username) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length < 4) {
                ServerMain.sendErrorResponse(exchange, 400, "Invalid request");
                return;
            }
            
            String productId = parts[3];
            shoppingService.removeFromCart(username, productId);
            
            // Return the updated cart
            handleGetCart(exchange, username);
            
        } catch (Exception e) {
            throw new IOException("Failed to remove from cart: " + e.getMessage(), e);
        }
    }
}
