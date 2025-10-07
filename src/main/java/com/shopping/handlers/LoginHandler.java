package com.shopping.handlers;

import com.shopping.ServerMain;
import com.shopping.service.ShoppingService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class LoginHandler implements HttpHandler {
    private final ShoppingService shoppingService;

    public LoginHandler(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            ServerMain.logRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath());
            
            // Only allow POST requests
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                ServerMain.sendStatus(exchange, 405, "Method Not Allowed");
                return;
            }
            
            // Read and validate request body
            String body = ServerMain.readBody(exchange);
            if (body == null || body.trim().isEmpty()) {
                ServerMain.writeJson(exchange, 400, "{\"error\":\"Request body is required\"}");
                return;
            }
            
            // Parse login data
            Map<String, Object> data = ServerMain.parseJsonToMap(body);
            String username = ((String) data.getOrDefault("username", "")).trim();
            String password = (String) data.getOrDefault("password", "");
            
            // Input validation
            if (username.isEmpty() || password.isEmpty()) {
                ServerMain.writeJson(exchange, 400, "{\"error\":\"Username and password are required\"}");
                return;
            }
            
            // Authenticate user
            boolean isAuthenticated = shoppingService.authenticateUser(username, password);
            
            if (isAuthenticated) {
                // Create session
                String sessionId = UUID.randomUUID().toString();
                ServerMain.getActiveSessions().put(sessionId, new ServerMain.SessionData(username));
                
                // Set session cookie
                exchange.getResponseHeaders().add("Set-Cookie", 
                    String.format("session=%s; Path=/; HttpOnly; SameSite=Strict", sessionId));
                
                ServerMain.writeJson(exchange, 200, "{\"status\":\"success\",\"username\":\"" + username + "\"}");
            } else {
                ServerMain.writeJson(exchange, 401, "{\"error\":\"Invalid username or password\"}");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            String errorJson = String.format(
                "{\"error\":\"Login failed: %s\"}", 
                ServerMain.escape(e.getMessage())
            );
            ServerMain.writeJson(exchange, 500, errorJson);
        }
    }
}
