package com.shopping;

import com.shopping.data.FileHandler;
import com.shopping.handlers.*;
import com.shopping.model.*;
import com.shopping.service.ShoppingService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * HTTP server for the Online Shopping application.
 * Serves static files and provides REST API endpoints.
 *
 * Note: Currently uses manual dependency injection. Consider implementing
 * a lightweight DI framework in the future for better testability.
 */
public class ServerMain {
    private static final Path STATIC_ROOT = Paths.get("web");
    private static final int THREAD_POOL_SIZE = 10;
    private static final String PORT_FILE = "server_port.txt";

    // CORS Configuration
    private static final Set<String> ALLOWED_METHODS = Set.of(
        "GET", "POST", "PUT", "DELETE", "OPTIONS"
    );

    private static final Set<String> ALLOWED_HEADERS = Set.of(
        "Content-Type", "Authorization", "X-Requested-With"
    );

    private static final FileHandler fileHandler = new FileHandler();
    private static final ShoppingService shoppingService = new ShoppingService(fileHandler);
    private static final Map<String, SessionData> activeSessions = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000; // 30 minutes

    public static void main(String[] args) {
        try {
            // FileHandler constructor already initializes data files
            // No need to call initializeDataFiles() again

            // Load sample data if needed
            if (new File("products.txt").length() == 0) {
                System.out.println("Creating sample products...");
                shoppingService.getProducts(); // This will trigger sample data creation
            }

            // Find available port
            System.out.println("Starting server...");
            int port = findAvailablePort(8080, 8090);

            // Create server
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            server.setExecutor(Executors.newFixedThreadPool(THREAD_POOL_SIZE));

            // Register handlers
            server.createContext("/", new StaticFileHandler());
            server.createContext("/api/products", new ProductsHandler(shoppingService));
            server.createContext("/api/auth/login", new LoginHandler(shoppingService));
            server.createContext("/api/auth/register", new RegisterHandler(shoppingService));
            server.createContext("/api/cart", new CartHandler());
            server.createContext("/api/orders", new OrderHandler(shoppingService));

            // Start server
            server.start();
            System.out.println("Server started on http://localhost:" + port);

            // Save port to file
            Files.writeString(Paths.get(PORT_FILE), String.valueOf(port), StandardCharsets.UTF_8);

            // Keep server running - wait for shutdown
            System.out.println("Press Ctrl+C to stop the server...");
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                System.out.println("Server interrupted");
            }

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                server.stop(0);
                System.out.println("Server stopped");
            }));

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Returns the first available port in [start, end], inclusive.
     */
    private static int findAvailablePort(int start, int end) throws IOException {
        for (int p = start; p <= end; p++) {
            try (ServerSocket socket = new ServerSocket()) {
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress("127.0.0.1", p));
                return p;
            } catch (IOException ignore) {
                // Port taken, try next
            }
        }
        throw new IOException("No available port in range " + start + "-" + end);
    }

    // Static file handler
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if (!method.equalsIgnoreCase("GET")) {
                sendStatus(exchange, 405, "Method Not Allowed");
                return;
            }

            if ("/".equals(path)) {
                path = "/homepage.html";
            }

            Path filePath = STATIC_ROOT.resolve(path.substring(1)).normalize();
            if (!filePath.startsWith(STATIC_ROOT) || !Files.exists(filePath) || Files.isDirectory(filePath)) {
                // Send 404 as JSON with CORS headers
                String errorResponse = String.format("{\"success\":false,\"message\":\"%s\"}", "Endpoint not found");
                sendJsonResponse(exchange, 404, errorResponse);
                return;
            }

            // Determine content type
            String contentType = getContentType(filePath);
            byte[] bytes = Files.readAllBytes(filePath);

            exchange.getResponseHeaders().set("Content-Type", contentType);
            // Add CORS headers for static files too
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", String.join(", ", ALLOWED_METHODS));
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", String.join(", ", ALLOWED_HEADERS));
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // Session data class - made public for handlers
    public static class SessionData {
        final String username;
        final long expiryTime;

        public SessionData(String username) {
            this.username = username;
            this.expiryTime = System.currentTimeMillis() + SESSION_TIMEOUT_MS;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    // Utility methods - made public static for handlers to use
    public static void logRequest(String method, String path) {
        System.out.printf("[%s] %s %s%n", new Date(), method, path);
    }

    public static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    public static void sendStatus(HttpExchange exchange, int status, String message) throws IOException {
        String response = String.format("{\"status\":%d,\"message\":\"%s\"}", status, escape(message));
        sendJsonResponse(exchange, status, response);
    }

    public static String getContentType(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".html")) return "text/html";
        if (name.endsWith(".css")) return "text/css";
        if (name.endsWith(".js")) return "application/javascript";
        if (name.endsWith(".json")) return "application/json";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }

    public static String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    public static Map<String, Object> parseJsonToMap(String json) {
        Map<String, Object> result = new HashMap<>();
        if (json == null || json.trim().isEmpty()) {
            return result;
        }

        // Handle compact form: {"user":"u","items":"1x2;3x1"}
        if (json.contains("\"items\"") && json.contains("x") && !json.contains("{\"id\"") && !json.contains("{\"qty\"")) {
            String[] parts = json.replace("{", "").replace("}", "").replace("\"", "").split(",");
            for (String part : parts) {
                String[] kv = part.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim();
                    String value = kv[1].trim();

                    if ("items".equals(key)) {
                        List<Map<String, Object>> items = new ArrayList<>();
                        for (String token : value.split(";")) {
                            if (token.isBlank()) continue;
                            String[] p = token.split("x");
                            if (p.length == 2) {
                                Map<String, Object> item = new HashMap<>();
                                item.put("id", toInt(p[0]));
                                item.put("qty", toInt(p[1]));
                                items.add(item);
                            }
                        }
                        result.put(key, items);
                    } else {
                        result.put(key, value);
                    }
                }
            }
            return result;
        }

        // Standard JSON parsing
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1).trim();
        }

        if (json.isEmpty()) {
            return result;
        }

        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replaceAll("^[\"]+|[\"]+$", "");
                String value = keyValue[1].trim();

                if (value.startsWith("\"") && value.endsWith("\"")) {
                    result.put(key, value.substring(1, value.length() - 1));
                } else if (value.matches("-?\\d+")) {
                    result.put(key, Integer.parseInt(value));
                } else if (value.matches("-?\\d+\\.\\d+")) {
                    result.put(key, Double.parseDouble(value));
                } else if (value.equals("true") || value.equals("false")) {
                    result.put(key, Boolean.parseBoolean(value));
                } else {
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    public static String getSessionId(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            for (String cookie : cookieHeader.split(";")) {
                String[] parts = cookie.trim().split("=");
                if (parts.length == 2 && parts[0].trim().equals("sessionId")) {
                    String sessionId = parts[1].trim();
                    SessionData sessionData = activeSessions.get(sessionId);
                    if (sessionData != null && !sessionData.isExpired()) {
                        return sessionId;
                    }
                }
            }
        }
        return null;
    }

    public static String getUsernameFromSession(String sessionId) {
        if (sessionId == null) return null;
        SessionData sessionData = activeSessions.get(sessionId);
        return (sessionData != null && !sessionData.isExpired()) ? sessionData.username : null;
    }

    public static Map<String, SessionData> getActiveSessions() {
        return activeSessions;
    }

    public static void removeSession(String sessionId) {
        if (sessionId != null) {
            activeSessions.remove(sessionId);
        }
    }

    public static void writeJson(HttpExchange exchange, int status, String json) throws IOException {
        sendJsonResponse(exchange, status, json);
    }

    public static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", String.join(", ", ALLOWED_METHODS));
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", String.join(", ", ALLOWED_HEADERS));
        exchange.sendResponseHeaders(statusCode, response.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    public static String toJsonOrderItems(List<OrderItem> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            OrderItem item = items.get(i);
            sb.append("{")
              .append("\"productId\":\"").append(escape(item.getProductId())).append("\"")
              .append(",\"name\":\"").append(escape(item.getName())).append("\"")
              .append(",\"price\":").append(item.getPrice())
              .append(",\"quantity\":").append(item.getQuantity())
              .append("}");
            if (i < items.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toJsonOrders(List<Order> orders) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            sb.append("{")
              .append("\"id\":").append(order.getOrderId())
              .append(",\"username\":\"").append(escape(order.getUserId())).append("\"")
              .append(",\"status\":\"").append(escape(order.getOrderStatus())).append("\"")
              .append("}");
            if (i < orders.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toJsonProducts(List<Product> products) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            sb.append("{")
              .append("\"id\":").append(p.getId()).append(",")
              .append("\"name\":\"").append(escape(p.getName())).append("\"")
              .append(",\"price\":").append(p.getPrice())
              .append(",\"description\":\"").append(escape(p.getDescription())).append("\"")
              .append(",\"category\":\"").append(escape(p.getCategory())).append("\"")
              .append(",\"stock\":").append(p.getStock());

            if (p.getImage() != null && !p.getImage().isEmpty()) {
                sb.append(",\"image\":\"").append(escape(p.getImage())).append("\"");
            }
            sb.append("}");
            if (i < products.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String escape(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private static int toInt(Object o) {
        if (o == null) return 0;
        try {
            if (o instanceof Number) return ((Number) o).intValue();
            return Integer.parseInt(String.valueOf(o).replaceAll("[^0-9-]", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
