package com.shopping.handlers;

import com.shopping.ServerMain;
import com.shopping.service.ShoppingService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

public class RegisterHandler implements HttpHandler {
    private final ShoppingService shoppingService;

    public RegisterHandler(ShoppingService shoppingService) {
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
            
            // Parse registration data
            Map<String, Object> data = ServerMain.parseJsonToMap(body);
            String username = ((String) data.getOrDefault("username", "")).trim();
            String password = ((String) data.getOrDefault("password", "")).trim();
            String email = ((String) data.getOrDefault("email", "")).trim();
            
            // Input validation
            if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
                ServerMain.writeJson(exchange, 400, "{\"error\":\"Username, password, and email are required\"}");
                return;
            }
            
            // Username validation (alphanumeric, 3-20 chars)
            if (!username.matches("^[a-zA-Z0-9]{3,20}$")) {
                ServerMain.writeJson(exchange, 400, "{\"error\":\"Username must be 3-20 alphanumeric characters\"}");
                return;
            }
            
            // Password validation (at least 6 chars)
            if (password.length() < 6) {
                ServerMain.writeJson(exchange, 400, "{\"error\":\"Password must be at least 6 characters\"}");
                return;
            }
            
            // Email validation (basic format check)
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                ServerMain.writeJson(exchange, 400, "{\"error\":\"Invalid email format\"}");
                return;
            }
            
            // Attempt to register user
            boolean success = shoppingService.registerUser(username, password, email);
            if (success) {
                ServerMain.writeJson(exchange, 201, "{\"status\":\"success\",\"message\":\"User registered successfully\"}");
            } else {
                ServerMain.writeJson(exchange, 409, "{\"error\":\"Username already exists\"}");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            String errorJson = String.format(
                "{\"error\":\"Registration failed: %s\"}", 
                ServerMain.escape(e.getMessage())
            );
            ServerMain.writeJson(exchange, 500, errorJson);
        }
    }
}
