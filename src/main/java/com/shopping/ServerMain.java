package com.shopping;

import com.shopping.data.FileHandler;
import com.shopping.model.*;
// Handler imports removed as they're not directly used
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
 * Localhost-only HTTP server for serving the web UI and minimal JSON APIs.
 * - Binds to 127.0.0.1:8080 (offline, local-only)
 * - Serves static files from ../web (because run-server.bat executes in src/)
 * - Exposes /api endpoints that call existing FileHandler and model classes
 */
public class ServerMain implements HttpHandler {
    private static final Path STATIC_ROOT = Paths.get("..", "web");
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Handle the request
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        try {
            // Handle different paths and methods here
            if (path.startsWith("/api/")) {
                handleApiRequest(exchange, path, method);
            } else {
                handleStaticFileRequest(exchange, path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    private void handleApiRequest(HttpExchange exchange, String path, String method) throws IOException {
        // Handle API requests here
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        
        if ("OPTIONS".equals(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        
        // Default not found response
        String response = "{\"error\":\"Not Found\"}";
        exchange.sendResponseHeaders(404, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
    
    private void handleStaticFileRequest(HttpExchange exchange, String path) throws IOException {
        if ("/".equals(path)) {
            path = "/index.html";
        }
        
        Path filePath = STATIC_ROOT.resolve(path.substring(1)).normalize();
        if (!filePath.startsWith(STATIC_ROOT)) {
            // Security check: prevent directory traversal
            String response = "{\"error\":\"Access denied\"}";
            exchange.sendResponseHeaders(403, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            return;
        }
        
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            // File not found
            String response = "{\"error\":\"File not found\"}";
            exchange.sendResponseHeaders(404, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            return;
        }
        
        // Determine content type
        String contentType = "application/octet-stream";
        if (path.endsWith(".html")) {
            contentType = "text/html";
        } else if (path.endsWith(".css")) {
            contentType = "text/css";
        } else if (path.endsWith(".js")) {
            contentType = "application/javascript";
        } else if (path.endsWith(".png")) {
            contentType = "image/png";
        } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        }
        
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, Files.size(filePath));
        
        try (InputStream is = Files.newInputStream(filePath);
             OutputStream os = exchange.getResponseBody()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = is.read(buffer)) > 0) {
                os.write(buffer, 0, count);
            }
        }
    }
    // Single instances for the application
    private static final FileHandler fileHandler = new FileHandler();
    private static final ShoppingService shoppingService = new ShoppingService(fileHandler);
    
    public static ShoppingService getShoppingService() {
        return shoppingService;
    }
    
    public static FileHandler getFileHandler() {
        return fileHandler;
    }
    
    // Session management
    private static final Map<String, SessionData> activeSessions = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000; // 30 minutes
    // Session data class
    private static class SessionData {
        final String username;
        final long expiryTime;
        
        SessionData(String username) {
            this.username = username;
            this.expiryTime = System.currentTimeMillis() + SESSION_TIMEOUT_MS;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    public static void main(String[] args) {
        try {
            // Initialize data files through ShoppingService
            System.out.println("Initializing data files...");
            
            // Load some sample data if needed
            if (new File("products.txt").length() == 0) {
                System.out.println("Creating sample products...");
                shoppingService.getProducts(); // This will trigger sample data creation
            }

            // Find a free localhost port starting at 8080
            System.out.println("Starting server...");
            int port = findAvailablePort(8080, 8090);
            
            // Create server with a thread pool
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            
            // Set up thread pool with 4 worker threads
            server.setExecutor(Executors.newFixedThreadPool(4));

            // Static file handler for frontend
            System.out.println("Registering static file handler...");
            server.createContext("/", new StaticFileHandler());
            
            // API endpoints - using inline handlers for simplicity
            server.createContext("/api/products", exchange -> {
                try {
                    List<Product> products = shoppingService.getProducts();
                    String json = toJsonProducts(products);
                    writeJson(exchange, 200, json);
                } catch (Exception e) {
                    e.printStackTrace();
                    writeJson(exchange, 500, "{\"error\":\"Error fetching products\"}");
                }
            });
            
            // Create an instance of ServerMain to handle requests
            ServerMain serverHandler = new ServerMain();
            
            // Set up the server context
            server.createContext("/api/orders", exchange -> {
                try {
                    serverHandler.handle(exchange);
                } catch (Exception e) {
                    e.printStackTrace();
                    writeJson(exchange, 500, "{\"error\":\"Error processing order\"}");
                }
            });
            
            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down server...");
                server.stop(1);
                System.out.println("Server stopped.");
            }));

            // Start the server
            server.start();
            
            // Print server info
            String serverUrl = "http://127.0.0.1:" + port;
            System.out.println("\n===============================================");
            System.out.println("Server is running at: " + serverUrl);
            System.out.println("Static files served from: " + STATIC_ROOT.toAbsolutePath());
            System.out.println("Available endpoints:");
            System.out.println("  GET    " + serverUrl + "/api/products");
            System.out.println("  POST   " + serverUrl + "/api/auth/login");
            System.out.println("  POST   " + serverUrl + "/api/auth/register");
            System.out.println("  POST   " + serverUrl + "/api/orders");
            System.out.println("===============================================\n");
            
            // Write the chosen port to a file for scripts
            try {
                Files.writeString(Paths.get("server_port.txt"), String.valueOf(port), StandardCharsets.UTF_8);
            } catch (IOException ioe) {
                System.err.println("Warning: Could not write server port to file: " + ioe.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Returns the first available port in [start, end], inclusive. Throws if none found.
     */
    private static int findAvailablePort(int start, int end) throws IOException {
        for (int p = start; p <= end; p++) {
            try (java.net.ServerSocket socket = new java.net.ServerSocket()) {
                socket.setReuseAddress(true);
                socket.bind(new java.net.InetSocketAddress("127.0.0.1", p));
                return p; // bind succeeded; port is free
            } catch (IOException ignore) {
                // port taken; try next
            }
        }
        throw new IOException("No available port in range " + start + "-" + end);
    }

    // Utility methods
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
        String response = String.format("{\"status\":\"%s\"}", message);
        writeJson(exchange, status, response);
    }
    
    public static String contentTypeFor(Path path) {
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
    
    static String readBody(HttpExchange exchange) throws IOException {
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
    
    private static Map<String, Object> parseJsonToMap(String json) {
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
        
        // Standard JSON object parsing
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
                
                // Handle different value types
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    // String
                    result.put(key, value.substring(1, value.length() - 1));
                } else if (value.matches("-?\\d+")) {
                    // Integer
                    result.put(key, Integer.parseInt(value));
                } else if (value.matches("-?\\d+\\.\\d+")) {
                    // Double
                    result.put(key, Double.parseDouble(value));
                } else if (value.equals("true") || value.equals("false")) {
                    // Boolean
                    result.put(key, Boolean.parseBoolean(value));
                } else if (value.startsWith("[") && value.endsWith("]")) {
                    // Simple array handling (for items array in orders)
                    result.put(key, parseJsonArray(value));
                } else {
                    // Default to string
                    result.put(key, value);
                }
            }
        }
        return result;
    }
    
    // Helper to parse simple JSON arrays (for order items)
    private static List<Map<String, Object>> parseJsonArray(String jsonArray) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (jsonArray == null || jsonArray.length() < 2 || !jsonArray.startsWith("[") || !jsonArray.endsWith("]")) {
            return result;
        }
        
        String content = jsonArray.substring(1, jsonArray.length() - 1).trim();
        if (content.isEmpty()) {
            return result;
        }
        
        // Simple split by },{ and handle first/last elements
        String[] elements = content.split("\\}\\s*,\\s*\\{");
        for (String element : elements) {
            if (!element.startsWith("{")) {
                element = "{" + element;
            }
            if (!element.endsWith("}")) {
                element = element + "}";
            }
            result.add(parseJsonToMap(element));
        }
        
        return result;
    }
    
    static String escape(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    
    /* ---------------------- Static Files ---------------------- */
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String rawPath = exchange.getRequestURI().getPath();
            logRequest(method, rawPath);

            if (!method.equalsIgnoreCase("GET")) {
                sendStatus(exchange, 405, "Method Not Allowed");
                return;
            }

            String path = decode(rawPath);
            if (path.equals("/")) {
                path = "/index.html"; // default file
            }

            // Prevent path traversal
            if (path.contains("..")) {
                sendStatus(exchange, 400, "Bad Request");
                return;
            }

            Path filePath = STATIC_ROOT.resolve(path.substring(1)); // drop leading '/'
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                sendStatus(exchange, 404, "Not Found");
                return;
            }

            String contentType = contentTypeFor(filePath);
            byte[] bytes = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    /* ---------------------- API: Products ---------------------- */
    static class ProductsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                logRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath());
                
                if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                    sendStatus(exchange, 405, "Method Not Allowed");
                    return;
                }
                
                // Handle search query parameter
                String query = exchange.getRequestURI().getQuery();
                List<Product> products;
                
                if (query != null && query.startsWith("q=")) {
                    String searchTerm = URLDecoder.decode(query.substring(2), StandardCharsets.UTF_8);
                    products = shoppingService.searchProducts(searchTerm);
                } else {
                    products = shoppingService.getProducts();
                }
                
                String json = toJsonProducts(products);
                writeJson(exchange, 200, json);
                
            } catch (Exception e) {
                e.printStackTrace();
                String errorJson = String.format("{\"error\":\"%s\"}", escape(e.getMessage()));
                writeJson(exchange, 500, errorJson);
            }
        }
    }

    /* ---------------------- API: Auth (Login) ---------------------- */
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                logRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath());
                
                // Only allow POST requests
                if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    sendStatus(exchange, 405, "Method Not Allowed");
                    return;
                }
                
                // Read and validate request body
                String body = readBody(exchange);
                if (body == null || body.trim().isEmpty()) {
                    writeJson(exchange, 400, "{\"error\":\"Request body is required\"}");
                    return;
                }
                
                // Parse credentials
                Map<String, Object> credentials = parseJsonToMap(body);
                String username = ((String) credentials.getOrDefault("username", "")).trim();
                String password = ((String) credentials.getOrDefault("password", "")).trim();
                
                // Validate input
                if (username.isEmpty() || password.isEmpty()) {
                    writeJson(exchange, 400, "{\"error\":\"Username and password are required\"}");
                    return;
                }
                
                // Authenticate user
                boolean isAuthenticated = shoppingService.authenticateUser(username, password);
                if (isAuthenticated) {
                    // Create session
                    String sessionId = UUID.randomUUID().toString();
                    activeSessions.put(sessionId, new SessionData(username));
                    
                    // Set secure session cookie
                    String cookie = String.format(
                        "sessionId=%s; Path=/; HttpOnly; SameSite=Strict%s", 
                        sessionId,
                        exchange.getRequestHeaders().getFirst("X-Forwarded-Proto") != null ? 
                            "; Secure" : ""
                    );
                    exchange.getResponseHeaders().add("Set-Cookie", cookie);
                    
                    // Return success response with user info (excluding sensitive data)
                    String json = String.format(
                        "{\"status\":\"success\",\"username\":\"%s\"}", 
                        escape(username)
                    );
                    writeJson(exchange, 200, json);
                } else {
                    // Authentication failed
                    writeJson(exchange, 401, "{\"error\":\"Invalid username or password\"}");
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                String errorJson = String.format(
                    "{\"error\":\"Login failed: %s\"}", 
                    escape(e.getMessage())
                );
                writeJson(exchange, 500, errorJson);
            }
        }
    }

    /* ---------------------- API: Register User ---------------------- */
    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                logRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath());
                
                // Only allow POST requests
                if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    sendStatus(exchange, 405, "Method Not Allowed");
                    return;
                }
                
                // Read and validate request body
                String body = readBody(exchange);
                if (body == null || body.trim().isEmpty()) {
                    writeJson(exchange, 400, "{\"error\":\"Request body is required\"}");
                    return;
                }
                
                // Parse registration data
                Map<String, Object> data = parseJsonToMap(body);
                String username = ((String) data.getOrDefault("username", "")).trim();
                String password = ((String) data.getOrDefault("password", "")).trim();
                String email = ((String) data.getOrDefault("email", "")).trim();
                
                // Input validation
                if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
                    writeJson(exchange, 400, "{\"error\":\"Username, password, and email are required\"}");
                    return;
                }
                
                // Username validation (alphanumeric, 3-20 chars)
                if (!username.matches("^[a-zA-Z0-9]{3,20}$")) {
                    writeJson(exchange, 400, "{\"error\":\"Username must be 3-20 alphanumeric characters\"}");
                    return;
                }
                
                // Password validation (at least 6 chars)
                if (password.length() < 6) {
                    writeJson(exchange, 400, "{\"error\":\"Password must be at least 6 characters\"}");
                    return;
                }
                
                // Email validation (basic format check)
                if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    writeJson(exchange, 400, "{\"error\":\"Invalid email format\"}");
                    return;
                }
                
                // Attempt to register user
                boolean success = shoppingService.registerUser(username, password, email);
                if (success) {
                    writeJson(exchange, 201, "{\"status\":\"success\",\"message\":\"User registered successfully\"}");
                } else {
                    writeJson(exchange, 409, "{\"error\":\"Username already exists\"}");
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                String errorJson = String.format(
                    "{\"error\":\"Registration failed: %s\"}", 
                    escape(e.getMessage())
                );
                writeJson(exchange, 500, errorJson);
            }
        }
    }

    /* ---------------------- API: Orders ---------------------- */
    static class OrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                logRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath());
                
                if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    sendStatus(exchange, 405, "Method Not Allowed");
                    return;
                }
                
                String body = readBody(exchange);
                Map<String, Object> map = parseJsonToMap(body);
                // Get username from session
                String username = getUsernameFromSession(getSessionId(exchange));
                if (username == null) {
                    writeJson(exchange, 401, "{\"error\":\"Not authenticated\"}");
                    return;
                }
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) map.getOrDefault("items", new ArrayList<>());

                // Validate items
                if (items.isEmpty()) {
                    writeJson(exchange, 400, "{\"error\":\"No items in order\"}");
                    return;
                }
                
                // Convert items to OrderItems
                List<OrderItem> orderItems = new ArrayList<>();
                for (Map<String, Object> item : items) {
                    String productId = String.valueOf(item.get("id"));
                    int quantity = toInt(item.get("quantity"));
                    if (quantity <= 0) {
                        writeJson(exchange, 400, "{\"error\":\"Invalid quantity for product " + productId + "\"}");
                        return;
                    }
                    
                    OrderItem orderItem = new OrderItem();
                    orderItem.setProductId(productId);
                    orderItem.setQuantity(quantity);
                    orderItems.add(orderItem);
                }
                
                // Place the order
                ShoppingService.OrderResult result = shoppingService.placeOrder(username, orderItems);
                if (!"ok".equalsIgnoreCase(result.status)) {
                    writeJson(exchange, 400, "{\"error\":\"" + escape(String.valueOf(result.message)) + "\"}");
                    return;
                }
                
                // Return success response
                String json = "{\"orderId\":\"" + escape(result.orderId) + "\",\"status\":\"ok\"}";
                writeJson(exchange, 201, json);
            } catch (Exception e) {
                e.printStackTrace();
                String errorJson = String.format(
                    "{\"error\":\"Failed to process order: %s\"}", 
                    escape(e.getMessage())
                );
                writeJson(exchange, 500, errorJson);
            }
        }
    }
    
    /* ---------------------- Utilities ---------------------- */
    // Session management
    public static String getSessionId(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            for (String cookie : cookieHeader.split(";")) {
                String[] parts = cookie.trim().split("=");
                if (parts.length == 2 && parts[0].trim().equals("sessionId")) {
                    String sessionId = parts[1].trim();
                    // Validate session exists and is not expired
                    SessionData sessionData = activeSessions.get(sessionId);
                    if (sessionData != null && !sessionData.isExpired()) {
                        return sessionId;
                    }
                }
            }
        }
        return null;
    }
    
    // Get username from session
    public static String getUsernameFromSession(String sessionId) {
        if (sessionId == null) return null;
        SessionData sessionData = activeSessions.get(sessionId);
        return (sessionData != null && !sessionData.isExpired()) ? sessionData.username : null;
    }
    
    // Remove session
    public static void removeSession(String sessionId) {
        activeSessions.remove(sessionId);
    }

    // Add CORS headers to the response
    public static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization, Content-Length, X-Requested-With");
        exchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
        exchange.getResponseHeaders().add("Access-Control-Max-Age", "3600");
    }
    
    // Handle OPTIONS request for CORS preflight
    private static void handleCorsPreflight(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().add("Content-Type", "text/plain");
        exchange.sendResponseHeaders(204, -1);
        exchange.getResponseBody().close();
    }
    
    public static void writeJson(HttpExchange exchange, int status, String json) throws IOException {
        // Add CORS headers to all responses
        addCorsHeaders(exchange);
        
        // Handle OPTIONS request (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            handleCorsPreflight(exchange);
            return;
        }
        
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    // Send error response as JSON
    public static void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        String json = String.format("{\"error\":\"%s\"}", escape(message));
        writeJson(exchange, statusCode, json);
    }
    
    // Send success response as JSON
    public static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        writeJson(exchange, statusCode, json);
    }

    private static String toJsonProducts(List<Product> products) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
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
