package com.shopping.data;

import com.shopping.model.Product;
import com.shopping.model.User;
import java.io.*;
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
        return loadData(PRODUCT_FILE); // returns data in List of <String> characters 
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
            // Split the CSV line into individual fields
            String[] parts = line.split(",");
            
            // Check if we have at least the required fields (first 6)
            if (parts.length >= 6) {
                try {
                    // Parse required fields (first 6)
                    int id = Integer.parseInt(parts[0]);         // Product ID (must be integer)
                    String name = parts[1];                      // Product name
                    double price = Double.parseDouble(parts[2]); // Product price
                    String description = parts[3];               // Product description
                    String category = parts[4];                  // Product category
                    int stock = Integer.parseInt(parts[5]);      // Available stock count
                    
                    // Parse optional fields with default values if not present
                    String sellerName = parts.length > 6 ? parts[6] : "Default Seller";
                    String sellerLocation = parts.length > 7 ? parts[7] : "Unknown Location";
                    double rating = parts.length > 8 ? Double.parseDouble(parts[8]) : 0.0;
                    String reviews = parts.length > 9 ? parts[9] : "No reviews yet";
                    
                    // Create new Product object and add to the list
                    products.add(new Product(id, name, price, description, category, stock,
                                          sellerName, sellerLocation, rating, reviews));
                                          
                } catch (NumberFormatException e) {
                    // Skip any lines with invalid number formats (e.g., non-numeric ID or price)
                    System.err.println("Skipping invalid product line: " + line);
                }
            } else {
                // Skip lines that don't have enough fields
                System.err.println("Skipping incomplete product line: " + line);
            }
        }
        return products;
    }

    public static List<String> loadUsers() {
        return loadData(USER_FILE);
    }

    public static List<String> loadOrders() {
        return loadData(ORDER_FILE);
    }

    private static List<String> loadData(String fileName) {
        List<String> data = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                data.add(line);
            }
        } catch (IOException e) {
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
}