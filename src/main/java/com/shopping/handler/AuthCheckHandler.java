package com.shopping.handler;

import com.shopping.ServerMain;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

public class AuthCheckHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            ServerMain.logRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath());
            
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                ServerMain.sendErrorResponse(exchange, 405, "Method not allowed");
                return;
            }
            
            String sessionId = ServerMain.getSessionId(exchange);
            String username = ServerMain.getUsernameFromSession(sessionId);
            
            // Build JSON response using string concatenation
            String response = String.format(
                "{\"authenticated\":%b,\"username\":\"%s\"}",
                username != null,
                username != null ? username.replace("\"", "\\\"") : ""
            );
            
            ServerMain.sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            ServerMain.sendErrorResponse(exchange, 500, "Internal server error");
        }
    }
}
