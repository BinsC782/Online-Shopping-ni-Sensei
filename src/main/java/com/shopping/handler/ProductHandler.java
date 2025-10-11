package com.shopping.handler;

import com.shopping.service.ShoppingService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.shopping.model.Product;
import com.shopping.util.JsonUtils;




import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class ProductHandler implements HttpHandler {
    private final ShoppingService shoppingService;
    private final Gson gson = new Gson(); 

    public ProductHandler(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod(); 
        String path = exchange.getRequestURI().getPath();
    try {
        if ("GET".equals(method)) {
            if (path.endsWith("/api/products")) {
                handleGetProducts(exchange);
            } else if (path.endsWith("/api/products/")) {
                handleGetProduct(exchange);
            }
            
        } catch (Exception e) {
            sendError(exchange, 500, "Internal server error"); // Method Not Allowed
        }
    }
    }
        

    private void handleGetProducts(HttpExchange exchange) throws IOException {
        try {
            List<Product> products = shoppingService.getProducts();
            String response = gson.toJson(products);
            sendResponse(exchange, 200, response);
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"Error retrieving products\"}");
        }
    }
    
    private void handleGetProduct(HttpExchange exchange) throws IOException {
        String[] parts = exchange.getRequestURI().getPath().split("/");
        String productId = parts[parts.length - 1];
        Product product = productService.getProductById(productId);
        
        if (product != null) {
            sendResponse(exchange, 200, JsonUtils.toJson(product));
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Product not found\"}");
        }
    }

    // In ProductService.java
public void validateProduct(Product product) {
    if (product.getPrice() < 0) {
        throw new ValidationException("Price cannot be negative");
    }
    // Other validations
}
    
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}
