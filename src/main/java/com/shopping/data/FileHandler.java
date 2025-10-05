package com.shopping.data;

import com.shopping.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class FileHandler {
    private static final String DATA_DIR = "data/";
    private static final String PRODUCTS_FILE = DATA_DIR + "products.txt";
    private static final String USERS_FILE = DATA_DIR + "users.txt";
    public static final String ORDERS_FILE = DATA_DIR + "orders.txt";
    
    public FileHandler() {
        // Ensure the data directory exists
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            initializeDataFiles();
        } catch (IOException e) {
            System.err.println("Error initializing data directory: " + e.getMessage());
        }
    }
    
    public void initializeDataFiles() throws IOException {
        createFileIfNotExists(PRODUCTS_FILE);
        createFileIfNotExists(USERS_FILE);
        createFileIfNotExists(ORDERS_FILE);
    }
    
    private void createFileIfNotExists(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
    }
    
    private String escapeCsv(String input) {
        if (input == null) {
            return "";
        }
        // Escape quotes and wrap in quotes if contains comma or newline
        if (input.contains(",") || input.contains("\n") || input.contains("\"")) {
            return "\"" + input.replace("\"", "\"\"") + "\"";
        }
        return input;
    }
    
    private Path getDataPath(String filename) {
        return Paths.get(DATA_DIR + filename).toAbsolutePath().normalize();
    }
    
    /**
     * Loads orders for a specific user or all orders if username is null
     * @param username The username to filter by, or null for all orders
     * @return List of orders with their details
     */
    public List<Map<String, Object>> loadOrders(String username) {
        List<Map<String, Object>> orders = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(ORDERS_FILE));
            for (String line : lines) {
                String[] parts = line.split(",", 5);
                if (parts.length == 5 && (username == null || username.equals(parts[1]))) {
                    Map<String, Object> order = new HashMap<>();
                    order.put("orderId", parts[0]);
                    order.put("username", parts[1]);
                    order.put("timestamp", Long.parseLong(parts[2]));
                    order.put("status", parts[3]);
                    
                    List<Map<String, Object>> items = new ArrayList<>();
                    String[] itemParts = parts[4].split(";");
                    for (String item : itemParts) {
                        String[] itemData = item.split(":");
                        if (itemData.length == 2) {
                            Map<String, Object> itemMap = new HashMap<>();
                            itemMap.put("productId", itemData[0]); // Keep as string
                            itemMap.put("quantity", Integer.parseInt(itemData[1]));
                            items.add(itemMap);
                        }
                    }
                    order.put("items", items);
                    orders.add(order);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return orders;
    }
    
    public List<Product> loadProducts() throws IOException {
        Path filePath = getDataPath(PRODUCTS_FILE);
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        return Files.lines(filePath)
            .filter(line -> !line.trim().isEmpty())
            .map(line -> {
                try {
                    String[] parts = line.split(",");
                    if (parts.length < 4) {
                        throw new IllegalArgumentException("Invalid product format: " + line);
                    }
                    Product product = new Product(
                        parts[0].trim(),  // ID
                        parts[1].trim(),  // name
                        Double.parseDouble(parts[2].trim()),  // price
                        Integer.parseInt(parts[3].trim())     // stock
                    );
                    
                    // Set additional fields if they exist
                    if (parts.length > 4) product.setDescription(parts[4].trim());
                    if (parts.length > 5) product.setCategory(parts[5].trim());
                    if (parts.length > 6) product.setSellerName(parts[6].trim());
                    if (parts.length > 7) product.setSellerLocation(parts[7].trim());
                    if (parts.length > 8) product.setRating(Double.parseDouble(parts[8].trim()));
                    if (parts.length > 9) product.setReviews(parts[9].trim());
                    if (parts.length > 10) product.setImage(parts[10].trim());
                    
                    return product;
                } catch (Exception e) {
                    System.err.println("Error parsing product line: " + line + " - " + e.getMessage());
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    public List<Product> loadProductsAsObjects() throws IOException {
        return loadProducts();
    }
    
    /**
     * Generates a new unique 6-digit product ID
     * @return A new unique product ID as a 6-digit string
     */
    /**
     * Generates a new unique 6-digit product ID
     * @return A new unique product ID as a 6-digit string
     */
    public String generateProductId() throws IOException {
        List<Product> products = loadProducts();
        Set<String> existingIds = products.stream()
            .map(Product::getId)
            .collect(Collectors.toSet());
            
        Random random = new Random();
        String newId;
        
        // Try to generate a unique ID (max 100 attempts to prevent infinite loop)
        int attempts = 0;
        do {
            if (attempts++ > 100) {
                throw new IOException("Failed to generate unique product ID after 100 attempts");
            }
            newId = String.format("%06d", random.nextInt(1_000_000));
        } while (existingIds.contains(newId));
        
        return newId;
    }
    
    public void saveProducts(List<Product> products) throws IOException {
        Path filePath = getDataPath(PRODUCTS_FILE);
        List<String> lines = products.stream()
            .map(p -> String.format("%s,%s,%.2f,%d,%s,%s,%s,%s,%.1f,%s,%s",
                p.getId(),
                escapeCsv(p.getName()),
                p.getPrice(),
                p.getStock(),
                escapeCsv(p.getDescription()),
                escapeCsv(p.getCategory()),
                escapeCsv(p.getSellerName()),
                escapeCsv(p.getSellerLocation()),
                p.getRating(),
                escapeCsv(p.getReviews()),
                escapeCsv(p.getImage() != null ? p.getImage() : "")))
            .collect(Collectors.toList());
        Files.write(filePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    public void saveProductsAsObjects(List<Product> products) throws IOException {
        saveProducts(products);
    }
    
    /**
     * Saves an order to the orders file
     * @param orderId The order ID
     * @param username The username
     * @param items List of order items
     * @param status Order status
     * @throws IOException If there's an error writing to the file
     */
    public void saveOrder(String orderId, String username, List<OrderItem> items, String status) throws IOException {
        Path filePath = getDataPath(ORDERS_FILE);
        String line = String.format("%s,%s,%s,%s,%s", 
            orderId, 
            username, 
            System.currentTimeMillis(), 
            status,
            items.stream()
                .map(i -> String.format("%s:%d", i.getProductId(), i.getQuantity()))
                .collect(Collectors.joining(";")));
        
        Files.write(filePath, 
            Collections.singletonList(line), 
            StandardOpenOption.CREATE, 
            StandardOpenOption.APPEND);
    }
    
    public List<String> loadUsers() throws IOException {
        Path filePath = getDataPath(USERS_FILE);
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        return Files.readAllLines(filePath)
            .stream()
            .filter(line -> !line.trim().isEmpty())
            .collect(Collectors.toList());
    }
    
    public void saveUsers(List<String> users) throws IOException {
        Path filePath = getDataPath(USERS_FILE);
        Files.write(filePath, users);
    }
    
    public boolean authenticateUser(String username, String password) throws IOException {
        return loadUsers().stream()
            .anyMatch(line -> {
                String[] parts = line.split(",");
                return parts.length >= 2 && 
                       parts[0].trim().equals(username) && 
                       parts[1].trim().equals(password);
            });
    }
    
    public boolean registerUser(String username, String password, String email) throws IOException {
        List<String> users = loadUsers();
        
        // Check if user already exists
        boolean userExists = users.stream()
            .anyMatch(line -> line.startsWith(username + ","));
            
        if (userExists) {
            return false;
        }
        
        // Add new user
        users.add(String.format("%s,%s,%s", username, password, email));
        saveUsers(users);
        return true;
    }
}