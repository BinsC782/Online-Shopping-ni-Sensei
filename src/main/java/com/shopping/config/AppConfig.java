package com.shopping.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Application configuration manager that loads settings from environment variables
 * and configuration files.
 */
public class AppConfig {
    private static final Properties props = new Properties();
    private static final String CONFIG_FILE = "app.properties";
    
    static {
        loadConfig();
    }
    
    private static void loadConfig() {
        // Load from environment variables first
        loadFromEnv();
        
        // Then try to load from config file
        try (InputStream input = Files.newInputStream(Paths.get(CONFIG_FILE))) {
            props.load(input);
        } catch (IOException e) {
            System.err.println("Warning: Could not load " + CONFIG_FILE + ". Using default values.");
        }
    }
    
    private static void loadFromEnv() {
        // Server configuration
        setIfNotExists("SERVER_PORT", "8080");
        setIfNotExists("SERVER_HOST", "127.0.0.1");
        
        // Security
        setIfNotExists("JWT_SECRET", "change-this-in-production");
        setIfNotExists("JWT_EXPIRATION_MS", "86400000"); // 24 hours
        
        // File paths
        setIfNotExists("USERS_FILE", "users.txt");
        setIfNotExists("PRODUCTS_FILE", "products.txt");
        setIfNotExists("ORDERS_FILE", "orders.txt");
        
        // CORS
        setIfNotExists("CORS_ALLOWED_ORIGINS", "http://localhost:8080,http://127.0.0.1:8080");
    }
    
    private static void setIfNotExists(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(key, defaultValue);
        }
        props.setProperty(key, value);
    }
    
    // Getters for configuration values
    public static int getServerPort() {
        return Integer.parseInt(props.getProperty("SERVER_PORT"));
    }
    
    public static String getServerHost() {
        return props.getProperty("SERVER_HOST");
    }
    
    public static String getJwtSecret() {
        return props.getProperty("JWT_SECRET");
    }
    
    public static long getJwtExpirationMs() {
        return Long.parseLong(props.getProperty("JWT_EXPIRATION_MS"));
    }
    
    public static String getUsersFile() {
        return props.getProperty("USERS_FILE");
    }
    
    public static String getProductsFile() {
        return props.getProperty("PRODUCTS_FILE");
    }
    
    public static String getOrdersFile() {
        return props.getProperty("ORDERS_FILE");
    }
    
    public static String[] getAllowedOrigins() {
        return props.getProperty("CORS_ALLOWED_ORIGINS", "").split(",");
    }
}