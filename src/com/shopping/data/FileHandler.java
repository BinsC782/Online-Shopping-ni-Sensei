package com.shopping.data;

import com.shopping.model.Product;
import com.shopping.model.User;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FileHandler {
    private static final String PRODUCT_FILE = "products.txt";
    private static final String USER_FILE = "users.txt";
    private static final String ORDER_FILE = "orders.txt";

    public static void initializeDataFiles() {
        try {
            createFileIfNotExists(PRODUCT_FILE);
            createFileIfNotExists(USER_FILE);
            createFileIfNotExists(ORDER_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createFileIfNotExists(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            file.createNewFile(); // creates a newfile
        }
    }

    public static List<String> loadProducts() {
        List<String> products = loadData(PRODUCT_FILE);
        if (products.isEmpty()) {
            // Add sample products if file is empty
            products.add("1,Keyboard,59.99,Mechanical gaming keyboard,Electronics,50,GameGear,Manila,4.5,Great keyboard!");
            products.add("2,Mouse,29.99,Wireless gaming mouse,Electronics,75,GameGear,Manila,4.2,Very responsive");
            products.add("3,Headset,99.99,Noise-cancelling headset,Electronics,30,AudioPro,Manila,4.8,Excellent sound quality");
            saveProducts(products);
        }
        return products;
    }

    /**
     * Loads products from file and converts them into Product objects
     * @return List of Product objects parsed from the products file
     */
    public static List<Product> loadProductsAsObjects() {
        // Read all product lines from the file as raw strings
        List<String> productStrings = loadProducts();
        // Initialize an empty list to store Product objects
        List<Product> products = new ArrayList<>();
        
        // Process each line from the file
        for (String line : productStrings) {
            // Parse CSV with support for quoted fields containing commas
            java.util.List<String> fields = parseCsvLine(line);

            // Expect at least the required fields (first 6)
            if (fields.size() >= 6) {
                try {
                    int id = Integer.parseInt(fields.get(0).trim());
                    String name = fields.get(1).trim();
                    double price = Double.parseDouble(fields.get(2).trim());
                    String description = fields.get(3).trim();
                    String category = fields.get(4).trim();
                    int stock = Integer.parseInt(fields.get(5).trim());

                    String sellerName = fields.size() > 6 ? fields.get(6).trim() : "Default Seller";
                    String sellerLocation = fields.size() > 7 ? fields.get(7).trim() : "Unknown Location";
                    double rating = 0.0;
                    if (fields.size() > 8) {
                        try { rating = Double.parseDouble(fields.get(8).trim()); } catch (NumberFormatException ignore) {}
                    }
                    String reviews = fields.size() > 9 ? fields.get(9).trim() : "No reviews yet";

                    Product prod = new Product(id, name, price, description, category, stock,
                                             sellerName, sellerLocation, rating, reviews);
                    // Optional image filename at index 10
                    if (fields.size() > 10) {
                        String image = fields.get(10).trim();
                        if (!image.isEmpty()) prod.setImage(image);
                    }
                    products.add(prod);
                } catch (NumberFormatException e) {
                    // If still malformed, skip quietly to reduce noise, but keep a concise hint
                    System.err.println("Skipping invalid product line (number format): " + truncate(line));
                }
            } else {
                // Skip lines that don't have enough fields (concise log)
                System.err.println("Skipping incomplete product line: " + truncate(line));
            }
        }
        return products;
    }

    /**
     * Minimal CSV parser supporting double-quoted fields with commas and escaped quotes ("").
     * No external dependencies.
     */
    private static java.util.List<String> parseCsvLine(String line) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (line == null) return out;
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    // Handle escaped quote ""
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++; // skip next quote
                    } else {
                        inQuotes = false; // closing quote
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    out.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
        }
        out.add(sb.toString()); // last field
        return out;
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() > 120 ? s.substring(0, 120) + "..." : s;
    }

    public static List<String> loadUsers() {
        List<String> users = loadData(USER_FILE);
        if (users.isEmpty()) {
            // Add a default admin user if no users exist
            users.add("admin,admin123,admin@example.com");
            saveUsers(users);
        }
        return users;
    }

    public static List<String> loadOrders() {
        return loadData(ORDER_FILE);
    }

    private static List<String> loadData(String fileName) {
        List<String> data = new ArrayList<>();
        File file = new File(fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("Error creating file: " + fileName);
                e.printStackTrace();
            }
            return data;
        }
        
        try (BufferedReader br = new BufferedReader(new FileReader(fileName, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    data.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + fileName);
            e.printStackTrace();
        }
        return data;
    }

    public static void saveProducts(List<String> products) {
        saveData(PRODUCT_FILE, products);
    }

    /**
     * Converts a list of Product objects into a list of CSV-formatted strings
     * and saves them to the products file.
     * 
     * @param products List of Product objects to be saved
     */
    public static void saveProductsAsObjects(List<Product> products) {
        // Initialize a list to hold the string representations of products
        List<String> productStrings = new ArrayList<>();
        
        // Convert each Product object to a CSV-formatted string
        for (Product product : products) {
            // Create a comma-separated string of product attributes
            String line = product.getId() + "," + 
                         product.getName() + "," + 
                         product.getPrice() + "," +
                         product.getDescription() + "," + 
                         product.getCategory() + "," + 
                         product.getStock() + "," +
                         product.getSellerName() + "," + 
                         product.getSellerLocation() + "," + 
                         product.getRating() + "," +
                         product.getReviews();
            // Append optional image filename if present
            if (product.getImage() != null && !product.getImage().isEmpty()) {
                line += "," + product.getImage();
            }
                         
            // Add the formatted string to our list
            productStrings.add(line);
        }
        
        // Save the list of formatted strings to the products file
        saveProducts(productStrings);
    }

    public static void saveUsers(List<String> users) {
        saveData(USER_FILE, users);
    }

    public static void saveOrders(List<String> orders) {
        saveData(ORDER_FILE, orders);
    }

    public static void saveOrder(String orderId, String userId, List<String> productList, String status) {
        String orderLine = orderId + "," + userId + "," + String.join(";", productList) + "," + status;
        List<String> orders = loadOrders();
        orders.add(orderLine);
        saveOrders(orders);
    }

    public static List<User> loadUsersAsObjects() {
        List<String> userStrings = loadUsers();
        List<User> users = new java.util.ArrayList<>();
        for (String line : userStrings) {
            String[] parts = line.split(",");
            if (parts.length >= 3) {
                String username = parts[0];
                String password = parts[1];
                String email = parts[2];
                users.add(new User(username, password, email));
            }
        }
        return users;
    }

    public static void saveUsersAsObjects(List<User> users) {
        List<String> userStrings = new ArrayList<>();
        for (User user : users) {
            String line = user.getUsername() + "," + user.getPassword() + "," +
                         user.getEmail();
            userStrings.add(line);
        }
        saveUsers(userStrings);
    }

    public static User authenticateUser(String username, String password) {
        List<User> users = loadUsersAsObjects();
        for (User user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }

    public static boolean registerUser(String username, String password, String email) {
        List<User> users = loadUsersAsObjects();
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return false; // Username already exists
            }
        }
        users.add(new User(username, password, email));
        saveUsersAsObjects(users);
        return true;
    }

    private static void saveData(String fileName, List<String> data) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            for (String line : data) {
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean removeProduct(Product p) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeProduct'");
    }
}