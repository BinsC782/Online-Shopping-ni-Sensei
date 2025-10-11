package com.shopping.handler;

import com.shopping.service.ShoppingService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;

public class OrderHandler implements HttpHandler {
    private static final String ORDERS_FILE = "orders.txt";
    private final ShoppingService shoppingService;
    
    public OrderHandler(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            // Handle CORS
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            
            addCorsHeaders(exchange);
            
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendErrorResponse(exchange, 405, "Method not allowed");
                return;
            }
            
            // Read request body
            String requestBody = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
                
            System.out.println("Received order data: " + requestBody);
            
            // Save order to file
            saveOrderToFile(requestBody);
            
            // Send success response
            sendJsonResponse(exchange, 200, "{\"status\":\"success\",\"message\":\"Order saved successfully\"}");
            
        } catch (Exception e) {
            System.err.println("Error processing order: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        } finally {
            exchange.close();
        }
    }
    
    private synchronized void saveOrderToFile(String orderData) throws IOException {
        if (orderData == null || orderData.trim().isEmpty()) {
            throw new IOException("Order data is empty");
        }

        // Ensure orders file exists
        File file = new File(ORDERS_FILE);
        if (!file.exists()) {
            file.createNewFile();
        }

        // Write order to file
        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(ORDERS_FILE),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            
            // Clean up the order data
            String orderLine = orderData.trim();
            
            // Add timestamp if not already present
            if (!orderLine.startsWith("ORD")) {
                orderLine = "ORD" + System.currentTimeMillis() + "," + orderLine;
            }
            
            // Ensure line ends with status
            if (!orderLine.endsWith("Pending") && !orderLine.endsWith("Processing") && !orderLine.endsWith("Completed")) {
                orderLine += ",Pending";
            }
            
            System.out.println("Saving order: " + orderLine);
            writer.write(orderLine);
            writer.newLine();
        }
    }
    
    // Helper methods
    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
        exchange.getResponseHeaders().add("Access-Control-Max-Age", "3600");
    }
    
    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
    
    
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        String errorResponse = "{\"status\":\"error\",\"message\":\"" + message.replace("\"", "\\\"") + "\"}";
        sendJsonResponse(exchange, statusCode, errorResponse);
    }
}
