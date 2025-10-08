package com.shopping.data;

import com.shopping.model.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class FileHandler {
    private static final String DATA_DIR = "data/";
    private static final String PRODUCTS_FILE = DATA_DIR + "products.txt";
    private static final String USERS_FILE = DATA_DIR + "users.txt";
    public static final String ORDERS_FILE = DATA_DIR + "orders.txt";
    private static final String CART_STATUS = "CART";
    
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

    /**
     * Converts an Order object to a string representation for storage
     * @param order The order to convert
     * @return String representation of the order
     */
    /**
     * Converts an Order object to a string representation for storage
     * Format: orderId,userId,timestamp,status,product1;product2;...
     * @param order The order to convert
     * @return String representation of the order
     */
    private String orderToString(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(escapeCsv(order.getOrderId())).append(",")
          .append(escapeCsv(order.getUserId())).append(",")
          .append(System.currentTimeMillis()).append(",")
          .append(escapeCsv(order.getOrderStatus()));
        
        // Add products
        if (order.getProductList() != null && !order.getProductList().isEmpty()) {
            sb.append(",").append(String.join(";", order.getProductList()));
        }
        
        return sb.toString();
    }
    
    /**
     * Saves an order to the orders file, updating if it already exists
     * @param order The order to save
     * @throws IOException If there's an error writing to the file
     */
    /**
     * Saves an order to the orders file, updating if it already exists
     * @param order The order to save
     * @throws IOException If there's an error writing to the file
     */
    public synchronized void saveOrder(Order order) throws IOException {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        
        createBackup(ORDERS_FILE);
        
        // Read existing orders
        List<String> orderLines = new ArrayList<>();
        if (Files.exists(getDataPath(ORDERS_FILE))) {
            orderLines = Files.readAllLines(getDataPath(ORDERS_FILE), StandardCharsets.UTF_8);
        }
        
        String orderLine = orderToString(order);
        
        // Remove existing order/cart for this user if it exists
        orderLines.removeIf(line -> {
            if (line == null || line.trim().isEmpty()) return false;
            try {
                String[] parts = line.split(",", 5);
                return parts.length >= 2 && 
                       parts[1].equals(order.getUserId()) && 
                       (CART_STATUS.equals(parts[3]) || 
                       (parts.length > 0 && parts[0].equals(order.getOrderId())));
            } catch (Exception e) {
                System.err.println("Error parsing order line: " + line);
                return false;
            }
        });
        
        // Add the new/updated order
        orderLines.add(orderLine);
        
        // Write all orders back to file
        Path tempFile = Files.createTempFile("orders", ".tmp");
        try {
            Files.write(tempFile, orderLines, StandardCharsets.UTF_8,
                      StandardOpenOption.CREATE,
                      StandardOpenOption.TRUNCATE_EXISTING);
            // Atomic move to replace the original file
            Files.move(tempFile, getDataPath(ORDERS_FILE),
                     StandardCopyOption.REPLACE_EXISTING,
                     StandardCopyOption.ATOMIC_MOVE);
        } finally {
            // Clean up temp file if it still exists
            if (Files.exists(tempFile)) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    System.err.println("Failed to delete temp file: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Creates a backup of a file with a timestamp
     * @param filename The name of the file to back up
     * @throws IOException If there's an error creating the backup
     */
    private void createBackup(String filename) throws IOException {
        Path source = getDataPath(filename);
        if (!Files.exists(source)) return;
        
        // Create backup directory if it doesn't exist
        Path backupDir = getDataPath("backups");
        Files.createDirectories(backupDir);
        
        // Create timestamped backup file
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupFilename = filename + "." + timestamp + ".bak";
        Path target = backupDir.resolve(backupFilename);
        
        // Copy the file
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Removes the cart for a specific user
     * @param username The username whose cart should be removed
     * @throws IOException If there's an error modifying the orders file
     * @throws IllegalArgumentException if username is null
     */
    public synchronized void removeCartForUser(String username) throws IOException {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
    
        // Create backup before modification
        createBackup(ORDERS_FILE);
    
        // Read all orders
        if (Files.exists(getDataPath(ORDERS_FILE))) {
            List<String> orderLines = Files.readAllLines(getDataPath(ORDERS_FILE), StandardCharsets.UTF_8);
            
            // Remove any cart entries for this user
            orderLines.removeIf(line -> {
                if (line == null || line.trim().isEmpty()) return false;
                try {
                    String[] parts = line.split(",", 5);
                    return parts.length >= 2 && 
                           parts[1].equals(username) && 
                           (CART_STATUS.equals(parts[3]) || "Pending".equals(parts[3]));
                } catch (Exception e) {
                    System.err.println("Error parsing order line: " + line);
                    return false;
                }
            });
    
            // Write back the filtered list using atomic operation
            Path tempFile = Files.createTempFile("orders", ".tmp");
            try {
                Files.write(tempFile, orderLines, StandardCharsets.UTF_8,
                          StandardOpenOption.CREATE,
                          StandardOpenOption.TRUNCATE_EXISTING);
                // Atomic move to replace the original file
                Files.move(tempFile, getDataPath(ORDERS_FILE),
                         StandardCopyOption.REPLACE_EXISTING,
                         StandardCopyOption.ATOMIC_MOVE);
            } finally {
                // Clean up temp file if it still exists
                if (Files.exists(tempFile)) {
                    try {
                        Files.deleteIfExists(tempFile);
                    } catch (IOException e) {
                        System.err.println("Failed to delete temp file: " + e.getMessage());
                    }
                }
            }
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

    /**
     * Simple CSV parser that handles quoted fields with commas
     */
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentField = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote
                    currentField.append('"');
                    i++; // Skip next quote
                } else {
                    // Toggle quote mode
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // Field separator
                result.add(currentField.toString());
                currentField.setLength(0);
            } else {
                currentField.append(c);
            }
        }

        // Add the last field
        result.add(currentField.toString());

        return result.toArray(new String[0]);
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
            System.err.println("Products file not found: " + filePath);
            return new ArrayList<>();
        }

        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        System.out.println("Loaded " + lines.size() + " lines from products file");

        List<Product> products = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            try {
                // Use a proper CSV parser that handles commas in quoted fields
                String[] parts = parseCsvLine(line);
                System.out.println("Line " + (i+1) + " has " + parts.length + " parts: " + java.util.Arrays.toString(parts));

                if (parts.length < 4) {
                    System.err.println("Invalid product format at line " + (i+1) + ": " + line);
                    continue;
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
                if (parts.length > 6) product.setStock(Integer.parseInt(parts[6].trim())); // Override stock if provided
                if (parts.length > 7) product.setSellerName(parts[7].trim());
                if (parts.length > 8) product.setSellerLocation(parts[8].trim());
                if (parts.length > 9) product.setRating(Double.parseDouble(parts[9].trim()));
                if (parts.length > 10) product.setReviews(parts[10].trim());
                if (parts.length > 11) product.setImage(parts[11].trim());

                products.add(product);
                System.out.println("Loaded product: " + product.getName() + " (ID: " + product.getId() + ")");

            } catch (Exception e) {
                System.err.println("Error parsing product line " + (i+1) + ": " + line + " - " + e.getMessage());
            }
        }

        System.out.println("Successfully loaded " + products.size() + " products");
        return products;
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