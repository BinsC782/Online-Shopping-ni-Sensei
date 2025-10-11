package com.shopping;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import com.sun.net.httpserver.*;
import com.shopping.data.FileHandler; 
import com.shopping.service.ShoppingService;
import com.shopping.model.Product;
import com.shopping.model.User;
import com.shopping.model.Order;
import com.shopping.model.Cart;
import com.shopping.model.SessionData;
import com.shopping.util.*;

/**
 * Main server class for the Online Shopping application.
 * - Binds to 127.0.0.1:8080 (offline, local-only)
 * - Serves static files from the web directory
 * - Exposes RESTful API endpoints for the shopping application
 */
public class ServerMain implements HttpHandler {
    private static final Path STATIC_ROOT = Paths.get("web");
    private static final int DEFAULT_PORT = 8080;
    private static int PORT = DEFAULT_PORT;
    private static final int THREAD_POOL_SIZE = 10;
    private static final String PORT_FILE = "server_port.txt";
    
    // CORS Configuration
    private final Set<String> allowedOrigins;
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

    public ServerMain() {
        this.allowedOrigins = new HashSet<>(Arrays.asList("*")); // Allow all origins for now
        try {
            fileHandler.initializeDataFiles();
        } catch (IOException e) {
            System.err.println("Error initializing data files: " + e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            
            // Handle CORS preflight
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                handleCorsPreflight(exchange);
                return;
            }

            // Add CORS headers
            addCorsHeaders(exchange);

            // Route requests
            if (path.startsWith("/api/")) {
                handleApiRequest(exchange, path, exchange.getRequestMethod());
            } else {
                handleStaticFileRequest(exchange, path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            String errorJson = String.format("{\"error\":\"%s\"}", escape(e.getMessage()));
            sendJsonResponse(exchange, 500, errorJson);
        }
    }

    private void handleApiRequest(HttpExchange exchange, String path, String method) throws IOException {
        try {
            // Route to appropriate handler based on path
            if (path.startsWith("/api/products")) {
                new ProductsHandler(shoppingService).handle(exchange);
            } else if (path.startsWith("/api/auth")) {
                new LoginHandler(shoppingService).handle(exchange);
            } else if (path.startsWith("/api/register")) {
                new RegisterHandler(shoppingService).handle(exchange);
            } else if (path.startsWith("/api/cart")) {
                new CartHandler(shoppingService).handle(exchange);
            } else if (path.startsWith("/api/orders")) {
                new OrdersHandler(shoppingService).handle(exchange);
            } else if (path.equals("/api/logout")) {
                handleLogout(exchange);
            } else {
                sendStatus(exchange, 404, "Not Found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            String errorJson = String.format("{\"error\":\"%s\"}", escape(e.getMessage()));
            sendJsonResponse(exchange, 500, errorJson);
        }
    }

    private void handleStaticFileRequest(HttpExchange exchange, String path) throws IOException {
        try {
            if (path.equals("/")) {
                path = "/index.html";
            }

            Path filePath = STATIC_ROOT.resolve(path.substring(1)).normalize();
            if (!filePath.startsWith(STATIC_ROOT)) {
                sendStatus(exchange, 403, "Forbidden");
                return;
            }

            if (Files.notExists(filePath)) {
                sendStatus(exchange, 404, "Not Found");
                return;
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
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
        } catch (Exception e) {
            e.printStackTrace();
            sendStatus(exchange, 500, "Internal Server Error");
        }
    }

    // Session management
    public static class SessionData {
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
            System.out.println("Starting Online Shopping Server...");
            ServerMain serverHandler = new ServerMain();
            
            // Create HTTP server
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
            server.createContext("/", serverHandler);
            server.setExecutor(Executors.newFixedThreadPool(THREAD_POOL_SIZE));
            
            // Start server
            server.start();
            System.out.println("Server started on http://localhost:" + PORT);
            
            // Save port to file
            Files.writeString(Paths.get(PORT_FILE), String.valueOf(PORT), StandardCharsets.UTF_8);
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                server.stop(0);
                System.out.println("Server stopped");
            }));
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Utility methods
    private static void logRequest(String method, String path) {
        System.out.printf("[%s] %s %s%n", new Date(), method, path);
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    private static void sendStatus(HttpExchange exchange, int status, String message) throws IOException {
        String response = String.format("{\"status\":%d,\"message\":\"%s\"}", status, escape(message));
        sendJsonResponse(exchange, status, response);
    }

    private static void sendJsonResponse(HttpExchange exchange, int status, String json) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private static String escape(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", String.join(", ", ALLOWED_METHODS));
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", String.join(", ", ALLOWED_HEADERS));
    }

    private void handleCorsPreflight(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", String.join(", ", ALLOWED_METHODS));
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", String.join(", ", ALLOWED_HEADERS));
        exchange.sendResponseHeaders(204, -1);
    }

    // Session management methods
    public static String getSessionId(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) return null;
        
        for (String cookie : cookieHeader.split(";")) {
            String[] parts = cookie.trim().split("=");
            if (parts.length == 2 && "sessionId".equals(parts[0].trim())) {
                String sessionId = parts[1].trim();
                SessionData sessionData = activeSessions.get(sessionId);
                if (sessionData != null && !sessionData.isExpired()) {
                    return sessionId;
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

    public static void removeSession(String sessionId) {
        if (sessionId != null) {
            activeSessions.remove(sessionId);
        }
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String sessionId = getSessionId(exchange);
        if (sessionId != null) {
            removeSession(sessionId);
            exchange.getResponseHeaders().add("Set-Cookie", "sessionId=; Max-Age=0; Path=/");
        }
        sendJsonResponse(exchange, 200, "{\"status\":\"success\"}");
    }

    private void setSessionCookie(HttpExchange exchange, String sessionId) {
        String secureFlag = exchange.getRequestHeaders().getFirst("X-Forwarded-Proto") != null ? 
            "; Secure" : "";
        String cookie = String.format(
            "sessionId=%s; Path=/; HttpOnly; SameSite=Strict%s", 
            sessionId,
            secureFlag
        );
        exchange.getResponseHeaders().add("Set-Cookie", cookie);
    }
    }
