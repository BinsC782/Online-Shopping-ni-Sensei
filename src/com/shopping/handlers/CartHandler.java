package com.shopping.handlers;

import com.shopping.ServerMain;
import com.shopping.ShoppingService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class CartHandler implements HttpHandler {
    private final ShoppingService shoppingService;

    public CartHandler(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String sessionId = ServerMain.getSessionId(exchange);
            String username = ServerMain.getUsernameFromSession(sessionId);
            
            if (username == null) {
                ServerMain.sendErrorResponse(exchange, 401, "Not authenticated");
                return;
            }
            
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                // Get cart contents
                List<ShoppingService.OrderItem> cart = shoppingService.getUserCart(username);
                JSONArray items = new JSONArray();
                for (ShoppingService.OrderItem item : cart) {
                    JSONObject itemJson = new JSONObject();
                    itemJson.put("productId", item.getProductId());
                    itemJson.put("quantity", item.getQuantity());
                    items.put(itemJson);
                }
                ServerMain.sendJsonResponse(exchange, 200, items.toString());
                
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                // Add to cart
                JSONObject request = ServerMain.parseRequestBody(exchange);
                int productId = request.getInt("productId");
                int quantity = request.getInt("quantity");
                
                shoppingService.addToCart(username, productId, quantity);
                ServerMain.sendJsonResponse(exchange, 200, "{\"status\":\"success\"}");
                
            } else if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                // Remove from cart
                String path = exchange.getRequestURI().getPath();
                String[] parts = path.split("/");
                if (parts.length < 4) {
                    ServerMain.sendErrorResponse(exchange, 400, "Missing product ID");
                    return;
                }
                
                try {
                    int productId = Integer.parseInt(parts[3]);
                    shoppingService.removeFromCart(username, productId);
                    ServerMain.sendJsonResponse(exchange, 200, "{\"status\":\"success\"}");
                } catch (NumberFormatException e) {
                    ServerMain.sendErrorResponse(exchange, 400, "Invalid product ID");
                }
                
            } else {
                ServerMain.sendErrorResponse(exchange, 405, "Method not allowed");
            }
            
        } catch (Exception e) {
            ServerMain.sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}
