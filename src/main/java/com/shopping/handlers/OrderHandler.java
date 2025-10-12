package com.shopping.handlers;

import com.shopping.service.ShoppingService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class OrderHandler implements HttpHandler {
    private final ShoppingService shoppingService;

    public OrderHandler(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            handlePost(exchange);
        } else {
            // Only allow POST
            exchange.sendResponseHeaders(405, 0); // Method Not Allowed
            exchange.close();
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        try {
            // Read the raw plain text body (the order line string)
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder orderLineBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                orderLineBuilder.append(line);
            }
            String orderLine = orderLineBuilder.toString();

            if (orderLine.isEmpty()) {
                sendResponse(exchange, 400, "Error: Order data is empty.");
                return;
            }

            // Process the order and clear the cart
            shoppingService.processCheckout(orderLine);

            sendResponse(exchange, 200, "Order saved successfully.");

        } catch (Exception e) {
            System.err.println("Error processing order: " + e.getMessage());
            e.printStackTrace();
            sendResponse(exchange, 500, "Internal Server Error: Could not save order.");
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(statusCode, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}
