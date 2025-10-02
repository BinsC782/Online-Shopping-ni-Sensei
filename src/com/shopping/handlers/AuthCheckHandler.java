package com.shopping.handlers;

import com.shopping.ServerMain;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

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
            
            JSONObject response = new JSONObject();
            response.put("authenticated", username != null);
            if (username != null) {
                response.put("username", username);
            }
            
            ServerMain.sendJsonResponse(exchange, 200, response.toString());
        } catch (Exception e) {
            ServerMain.sendErrorResponse(exchange, 500, "Internal server error");
        }
    }
}
