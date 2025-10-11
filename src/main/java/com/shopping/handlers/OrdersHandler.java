package com.shopping.handlers;

import com.shopping.ServerMain;
import com.shopping.model.Order;
import com.shopping.model.OrderItem;
import com.shopping.service.ShoppingService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OrdersHandler implements HttpHandler {
    private final ShoppingService shoppingService;

    public OrdersHandler(ShoppingService shoppingService) {
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
                    // Get user's orders
                    List<Order> orders = shoppingService.getUserOrders(username);
                    String jsonResponse = "{\"orders\":" + ServerMain.toJson(orders) + "}";
                    ServerMain.sendJsonResponse(exchange, 200, jsonResponse);
                    break;
                    
                case "POST":
                    // Place order from cart
                    List<OrderItem> cartItems = shoppingService.getUserCart(username);
                    if (cartItems == null || cartItems.isEmpty()) {
                        ServerMain.sendJsonResponse(exchange, 400, "{\"error\":\"Cart is empty\"}");
                        return;
                    }
                    
                    String orderId = "ord_" + UUID.randomUUID().toString();
                    Order order = shoppingService.placeOrder(username, cartItems);
                    
                    if (order != null) {
                        shoppingService.clearUserCart(username);
                        ServerMain.sendJsonResponse(exchange, 201, 
                            "{\"status\":\"success\",\"orderId\":\"" + orderId + "\"}");
                    } else {
                        ServerMain.sendJsonResponse(exchange, 500, "{\"error\":\"Failed to place order\"}");
                    }
                    break;
                    
                default:
                    ServerMain.sendStatus(exchange, 405, "Method Not Allowed");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            String errorJson = String.format(
                "{\"error\":\"Order operation failed: %s\"}", 
                ServerMain.escape(e.getMessage())
            );
            ServerMain.sendJsonResponse(exchange, 500, errorJson);
        }
    }
}
