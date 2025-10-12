package com.shopping.util;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiResponseTest {

    private HttpExchange exchange;
    private Headers headers;
    private OutputStream outputStream;

    @BeforeEach
    void setUp() {
        exchange = mock(HttpExchange.class);
        headers = mock(Headers.class);
        outputStream = new ByteArrayOutputStream();

        when(exchange.getResponseHeaders()).thenReturn(headers);
        when(exchange.getResponseBody()).thenReturn(outputStream);
    }

    @Test
    void sendSuccess_WithMessage() throws IOException {
        // When
        ApiResponse.sendSuccess(exchange, "Operation successful");

        // Then
        verifyResponse(200, "Operation successful", null);
    }

    @Test
    void sendError_WithMessage() throws IOException {
        // When
        ApiResponse.sendError(exchange, 400, "Invalid request");

        // Then
        verifyResponse(400, "Invalid request", null);
    }

    @Test
    void sendBadRequest() throws IOException {
        // When
        ApiResponse.sendBadRequest(exchange, "Bad request");

        // Then
        verifyResponse(400, "Bad request", null);
    }

    @Test
    void sendUnauthorized() throws IOException {
        // When
        ApiResponse.sendUnauthorized(exchange, "Unauthorized");

        // Then
        verifyResponse(401, "Unauthorized", null);
    }

    @Test
    void sendForbidden() throws IOException {
        // When
        ApiResponse.sendForbidden(exchange, "Forbidden");

        // Then
        verifyResponse(403, "Forbidden", null);
    }

    @Test
    void sendNotFound() throws IOException {
        // When
        ApiResponse.sendNotFound(exchange, "Not found");

        // Then
        verifyResponse(404, "Not found", null);
    }

    @Test
    void sendInternalError() throws IOException {
        // When
        ApiResponse.sendInternalError(exchange, "Internal server error");

        // Then
        verifyResponse(500, "Internal server error", null);
    }

    @Test
    void sendSuccess_WithData() throws IOException {
        // Given
        String jsonData = "{\"id\":1,\"name\":\"Test\"}";

        // When
        ApiResponse.sendSuccess(exchange, "Success", jsonData);

        // Then
        verifyResponse(200, "Success", jsonData);
    }

    private void verifyResponse(int expectedStatus, String expectedMessage, String expectedData) throws IOException {
        // Verify response code
        verify(exchange).sendResponseHeaders(eq(expectedStatus), anyLong());

        // Verify content type header
        verify(headers).set(eq("Content-Type"), eq("application/json; charset=utf-8"));

        // Verify response body
        String response = outputStream.toString();
        assertTrue(response.contains(String.format("\"success\": %b", expectedStatus < 400)));
        assertTrue(response.contains(String.format("\"message\": \"%s\"", expectedMessage)));
        
        if (expectedData != null) {
            assertTrue(response.contains(String.format("\"data\": %s", expectedData)));
        }
    }
}
