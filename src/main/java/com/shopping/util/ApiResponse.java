package com.shopping.util;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for sending consistent API responses
 */
public class ApiResponse {
    
    public static void sendSuccess(HttpExchange exchange, String message) throws IOException {
        sendResponse(exchange, 200, true, message, null);
    }
    
    public static void sendSuccess(HttpExchange exchange, String message, Object data) throws IOException {
        sendResponse(exchange, 200, true, message, data);
    }
    
    public static void sendError(HttpExchange exchange, int statusCode, String error) throws IOException {
        sendResponse(exchange, statusCode, false, error, null);
    }
    
    public static void sendBadRequest(HttpExchange exchange, String error) throws IOException {
        sendError(exchange, 400, error);
    }
    
    public static void sendUnauthorized(HttpExchange exchange, String error) throws IOException {
        sendError(exchange, 401, error);
    }
    
    public static void sendForbidden(HttpExchange exchange, String error) throws IOException {
        sendError(exchange, 403, error);
    }
    
    public static void sendNotFound(HttpExchange exchange, String error) throws IOException {
        sendError(exchange, 404, error);
    }
    
    public static void sendInternalError(HttpExchange exchange, String error) throws IOException {
        sendError(exchange, 500, error);
    }
    
    private static void sendResponse(
            HttpExchange exchange, 
            int statusCode, 
            boolean success, 
            String message, 
            Object data
    ) throws IOException {
        StringBuilder response = new StringBuilder("{\n");
        response.append(String.format("  \"success\": %b,\n", success));
        response.append(String.format("  \"message\": \"%s\"", escapeJson(message)));
        
        if (data != null) {
            // In a real application, use a proper JSON library like Jackson or Gson
            response.append(",\n  \"data\": ").append(data.toString());
        }
        
        response.append("\n}");
        
        byte[] responseBytes = response.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    
    private static String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
