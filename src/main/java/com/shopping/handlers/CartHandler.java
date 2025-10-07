package com.shopping.handlers;

import com.shopping.ServerMain;
import com.shopping.model.OrderItem;
import com.shopping.service.ShoppingService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CartHandler implements HttpHandler {
    private final ShoppingService shoppingService;

    public CartHandler(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            ServerMain.logRequest(method, path);
            
            // Get username from session
            String sessionId = ServerMain.getSessionId(exchange);
            String username = ServerMain.getUsernameFromSession(sessionId);
            
            if (username == null) {
                ServerMain.sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }
            
            switch (method) {
                case "GET":
                    // Get user's cart
                    List<OrderItem> cartItems = shoppingService.getUserCart(username);
                    String jsonResponse = "{\"items\":" + ServerMain.toJson(cartItems) + "}";
                    ServerMain.sendJsonResponse(exchange, 200, jsonResponse);
                    break;
                    
                case "POST":
                    // Add item to cart
                    String body = ServerMain.readBody(exchange);
                    Map<String, Object> data = ServerMain.parseJsonToMap(body);
                    String productId = (String) data.get("productId");
                    int quantity = (int) data.getOrDefault("quantity", 1);
                    
                    boolean added = shoppingService.addToCart(username, productId, quantity);
                    if (added) {
                        ServerMain.sendJsonResponse(exchange, 200, "{\"status\":\"success\"}");
                    } else {
                        ServerMain.sendJsonResponse(exchange, 400, "{\"error\":\"Failed to add item to cart\"}");
                    }
                    break;
                    
                case "DELETE":
                    // Remove item from cart
                    String[] pathParts = path.split("/");
                    if (pathParts.length < 4) {
                        ServerMain.sendJsonResponse(exchange, 400, "{\"error\":\"Product ID is required\"}");
                        return;
                    }
                    String productIdToRemove = pathParts[3];
                    boolean removed = shoppingService.removeFromCart(username, productIdToRemove);
                    if (removed) {
                        ServerMain.sendJsonResponse(exchange, 200, "{\"status\":\"success\"}");
                    } else {
                        ServerMain.sendJsonResponse(exchange, 404, "{\"error\":\"Item not found in cart\"}");
                    }
                    break;
                    
                default:
                    ServerMain.sendStatus(exchange, 405, "Method Not Allowed");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            String errorJson = String.format(
                "{\"error\":\"Cart operation failed: %s\"}", 
                ServerMain.escape(e.getMessage())
            );
            ServerMain.sendJsonResponse(exchange, 500, errorJson);
        }
    }
}
