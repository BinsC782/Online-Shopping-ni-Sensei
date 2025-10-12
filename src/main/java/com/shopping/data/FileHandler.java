package com.shopping.data;

import com.shopping.model.Product;
import com.shopping.model.Order;
import com.shopping.model.User;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FileHandler {
    private static final String PRODUCTS_FILE = "products.txt";
    public static final String ORDERS_FILE = "orders.txt";
    private static final String CART_FILE = "cart.txt";
    public FileHandler() {
        initializeDataFiles();
    }

    public void initializeDataFiles() {
        System.out.println("Initializing data files...");
        createFileIfNotExists(PRODUCTS_FILE);
        createFileIfNotExists(ORDERS_FILE);
        createFileIfNotExists(CART_FILE);
    }

    private void createFileIfNotExists(String fileName) {
        Path path = Paths.get(fileName);
        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
                System.out.println("Created new file: " + fileName);
            } catch (IOException e) {
                System.err.println("Error creating file " + fileName + ": " + e.getMessage());
            }
        }
    }

    // =======================================================
    // CSV Utility Methods (FIXED AND INCLUDED)
    // =======================================================

    /**
     * Escapes a string for use in a CSV field by enclosing it in double quotes
     * and escaping existing double quotes with another double quote.
     */
    private String escapeCsv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        // Replace all double quotes with two double quotes
        String escaped = value.replace("\"", "\"\"");
        // Enclose the entire value in double quotes
        return "\"" + escaped + "\"";
    }

    // =======================================================
    // Product Handling (Ensure this uses the new escapeCsv)
    // =======================================================

    public List<Product> loadProducts() {
        List<Product> products = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(PRODUCTS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("(?<!\\\\),");
                if (parts.length >= 8) {
                    try {
                        Product p = new Product(
                            parts[0].trim(),
                            parts[1].trim(),
                            Double.parseDouble(parts[4].trim()),
                            parts[2].trim(),
                            parts[3].trim(),
                            Integer.parseInt(parts[5].trim()),
                            "",
                            "",
                            Double.parseDouble(parts[6].trim()),
                            ""
                        );
                        p.setImage(parts[7].trim());
                        products.add(p);
                    } catch (NumberFormatException e) {
                        System.err.println("Skipping malformed product line: " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading products: " + e.getMessage());
        }
        return products;
    }

    public void saveProducts(List<Product> products) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(PRODUCTS_FILE))) {
            // Write a header if needed
            // writer.write("id,name,description,category,price,stock,rating,imageUrl\n");
            for (Product p : products) {
                String line = String.join(",",
                    p.getId(),
                    escapeCsv(p.getName()),
                    escapeCsv(p.getDescription()),
                    escapeCsv(p.getCategory()),
                    String.valueOf(p.getPrice()),
                    String.valueOf(p.getStock()),
                    String.valueOf(p.getRating()),
                    escapeCsv(p.getImage() != null ? p.getImage() : "")
                );
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving products: " + e.getMessage());
        }
    }

    // =======================================================
    // Order Handling (NEW)
    // =======================================================

    /**
     * Converts an Order object to a string representation for storage
     * @param order The order to convert
     * @return String representation of the order
     */
    private String orderToString(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(order.getOrderId()).append(",")
          .append(order.getUserId()).append(",")
          .append(System.currentTimeMillis()).append(",")
          .append(order.getOrderStatus());

        // Add products
        if (order.getProductList() != null && !order.getProductList().isEmpty()) {
            sb.append(",").append(String.join(";", order.getProductList()));
        }

        return sb.toString();
    }

    /**
     * Saves a single order line to orders.txt.
     * @param orderLine The full CSV-formatted line of the order.
     */
    public void saveOrder(String orderLine) throws IOException {
        try (FileWriter fw = new FileWriter(ORDERS_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(orderLine);
            bw.newLine();
        }
    }

    /**
     * Saves an order to the orders file
     * @param order The order to save
     * @throws IOException if there's an error writing to the file
     */
    public void saveOrder(Order order) throws IOException {
        String orderLine = orderToString(order);
        saveOrder(orderLine);
    }

    // =======================================================
    // Cart Handling (NEW)
    // =======================================================

    /**
     * Clears the cart file for a specific user after successful checkout.
     * Since the frontend is currently only sending the OrderLine to the server,
     * this method is a placeholder for a future state where cart.txt stores
     * multiple users' carts, or we simply clear the whole file as a shortcut.
     * @param userId The ID of the user whose cart should be removed (e.g., "guest").
     * @throws IOException
     */
    public void removeCartForUser(String userId) throws IOException {
        // Since we are not tracking individual user carts yet,
        // the easiest implementation is to clear the cart.txt file for now,
        // assuming only one user is active, or simply do nothing as cart
        // persistence is client-side in the JS.
        // For a simple checkout flow, we can just clear the file.

        // Option 1: Truncate the file (Assuming cart.txt only holds temporary data)
        try (PrintWriter writer = new PrintWriter(CART_FILE)) {
            writer.print("");
            writer.flush();
        }

        // Option 2: Actual user-specific removal (Requires advanced implementation)
        /*
        Path cartPath = Paths.get(CART_FILE);
        List<String> lines = Files.readAllLines(cartPath);
        List<String> updatedLines = lines.stream()
            .filter(line -> !line.startsWith(userId + ","))
            .collect(Collectors.toList());

        Files.write(cartPath, updatedLines);
        */
    }

    // =======================================================
    // User Management Methods (for ShoppingService compatibility)
    // =======================================================

    public User authenticateUser(String username, String password) throws IOException {
        return loadUsers().stream()
            .filter(line -> {
                String[] parts = line.split(",");
                return parts.length >= 3 &&
                       parts[0].trim().equals(username) &&
                       parts[1].trim().equals(password);
            })
            .findFirst()
            .map(line -> {
                String[] parts = line.split(",");
                User user = new User(username, password, parts[2].trim());
                return user;
            })
            .orElse(null);
    }

    public boolean registerUser(String username, String password, String email) throws IOException {
        List<String> users = loadUsers();

        // Check if user already exists
        boolean userExists = users.stream()
            .anyMatch(line -> line.startsWith(username + ","));

        if (userExists) {
            return false;
        }

        // Add new user (format: username,password,email)
        users.add(String.format("%s,%s,%s", username, password, email));
        saveUsers(users);
        return true;
    }

    private List<String> loadUsers() throws IOException {
        Path filePath = Paths.get("users.txt");
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        return Files.readAllLines(filePath)
            .stream()
            .filter(line -> !line.trim().isEmpty())
            .collect(Collectors.toList());
    }

    private void saveUsers(List<String> users) throws IOException {
        Path filePath = Paths.get("users.txt");
        Files.write(filePath, users);
    }

    public List<Order> getUserOrders(String username) {
        List<Order> orders = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(ORDERS_FILE));
            for (String line : lines) {
                String[] parts = line.split(",", 5);
                if (parts.length == 5 && (username == null || username.equals(parts[1]))) {
                    // Parse the order data and create Order objects
                    // This is a simplified implementation
                    List<String> productList = new ArrayList<>();
                    if (parts.length > 4 && !parts[4].trim().isEmpty()) {
                        String[] items = parts[4].split(";");
                        for (String item : items) {
                            productList.add(item);
                        }
                    }

                    Order order = new Order(parts[0], parts[1], productList);
                    order.setOrderStatus(parts[3]);
                    orders.add(order);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return orders;
    }
}