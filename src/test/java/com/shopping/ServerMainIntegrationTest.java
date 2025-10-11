package com.shopping;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ServerMainIntegrationTest {
    private HttpServer server;
    private final int TEST_PORT = 8081;

    @BeforeEach
    void setUp() throws IOException {
        // Start the server on a test port
        server = HttpServer.create(new InetSocketAddress(TEST_PORT), 0);
        server.createContext("/", new ServerMain());
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void testStaticFileServing() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/index.html"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("<!DOCTYPE html>"));
    }

    @Test
    void testApiEndpointNotFound() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/nonexistent"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"success\":false"));
        assertTrue(response.body().contains("Endpoint not found"));
    }

    @Test
    void testCorsHeaders() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertNotNull(response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        assertNotNull(response.headers().firstValue("X-Content-Type-Options").orElse(null));
    }
}
