package com.shopping.handlers;

import com.shopping.ServerMain;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class LogoutHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            ServerMain.logRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath());
            
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                ServerMain.sendErrorResponse(exchange, 405, "Method not allowed");
                return;
            }
            
            String sessionId = ServerMain.getSessionId(exchange);
            if (sessionId != null) {
                ServerMain.removeSession(sessionId);
            }
            
            // Clear the session cookie
            exchange.getResponseHeaders().add("Set-Cookie", 
                "sessionId=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
            
            ServerMain.sendJsonResponse(exchange, 200, "{\"status\":\"success\"}");
        } catch (Exception e) {
            ServerMain.sendErrorResponse(exchange, 500, "Internal server error");
        }
    }
}
