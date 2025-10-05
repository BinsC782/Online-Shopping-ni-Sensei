package com.shopping.service;

import com.shopping.data.FileHandler;
import com.shopping.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class ShoppingService {
    private static final String CART_STATUS = "CART";
    private final FileHandler fileHandler;
    private final ReentrantLock orderLock = new ReentrantLock(true); // Fair lock to prevent thread starvation
    private final Map<String, List<OrderItem>> userCarts = new HashMap<>();
    
    public ShoppingService(FileHandler fileHandler) {
        this.fileHandler = fileHandler;
        try {
            this.fileHandler.initializeDataFiles();
            loadCartsFromFile();
        } catch (IOException e) {
            System.err.println("Error initializing ShoppingService: " + e.getMessage());
        }
    }
    
    private void loadCartsFromFile() {
        orderLock.lock();
        try {
            List<String> orderLines = Files.readAllLines(Paths.get(FileHandler.ORDERS_FILE));
            for (String line : orderLines) {
                String[] parts = line.split(",", 5);
                if (parts.length == 5 && CART_STATUS.equals(parts[3])) {
                    String username = parts[1];
                    String[] items = parts[4].split(";");
                    List<OrderItem> cartItems = new ArrayList<>();
                    
                    for (String item : items) {
                        String[] itemParts = item.split(":");
                        if (itemParts.length == 2) {
                            String productId = itemParts[0];
                            int quantity = Integer.parseInt(itemParts[1]);
                            OrderItem orderItem = new OrderItem();
                            orderItem.setProductId(productId);
                            orderItem.setQuantity(quantity);
                            cartItems.add(orderItem);
                        }
                    }
                    
                    if (!cartItems.isEmpty()) {
                        userCarts.put(username, cartItems);
                    }
                }
            }
            System.out.println("Loaded carts from " + FileHandler.ORDERS_FILE);
        } catch (NoSuchFileException e) {
            System.out.println("No existing orders file found. Starting with empty carts.");
        } catch (Exception e) {
            System.err.println("Error loading carts from file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private synchronized void saveCartsToFile() {
        orderLock.lock();
        try {
            // First, remove all existing cart entries from orders file
            List<String> allOrders = new ArrayList<>();
            if (Files.exists(Paths.get(FileHandler.ORDERS_FILE))) {
                allOrders = Files.readAllLines(Paths.get(FileHandler.ORDERS_FILE));
                allOrders.removeIf(line -> {
                    String[] parts = line.split(",", 5);
                    return parts.length == 5 && CART_STATUS.equals(parts[3]);
                });
            }
            
            // Add current cart entries
            for (Map.Entry<String, List<OrderItem>> entry : userCarts.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    String cartId = "cart_" + entry.getKey() + "_" + System.currentTimeMillis();
                    fileHandler.saveOrder(cartId, entry.getKey(), entry.getValue(), CART_STATUS);
                }
            }
            
            System.out.println("Saved carts to " + FileHandler.ORDERS_FILE);
        } catch (IOException e) {
            System.err.println("Error saving carts to file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            orderLock.unlock();
        }
    }
    
    // ===== Product Related Methods =====
    
    /**
     * Get all products
     */
    public List<Product> getProducts() {
        orderLock.lock();
        try {
            return fileHandler.loadProducts();
        } catch (IOException e) {
            System.err.println("Error loading products: " + e.getMessage());
            return new ArrayList<>(); // Return empty list on error
        } finally {
            orderLock.unlock();
        }
    }
    
    /**
     * Get a product by ID
     * @param productId The 6-digit product ID as string
     * @return Product or null if not found
     */
    public Product getProductById(String productId) {
        try {
            return fileHandler.loadProducts().stream()
                .filter(p -> productId.equals(p.getId()))
                .findFirst()
                .orElse(null);
        } catch (IOException e) {
            System.err.println("Error finding product: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Search products by name or description
     * @param searchTerm Search term
     * @return List of matching products
     */
    public List<Product> searchProducts(String searchTerm) {
        try {
            String lowerTerm = searchTerm.toLowerCase();
            return fileHandler.loadProducts().stream()
                .filter(p -> p.getName().toLowerCase().contains(lowerTerm) || 
                            (p.getDescription() != null && p.getDescription().toLowerCase().contains(lowerTerm)))
                .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error searching products: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    // ===== Cart Related Methods =====
    
    public synchronized void addToCart(String username, String productId, int quantity) {
        if (username == null || username.trim().isEmpty() || productId == null || quantity <= 0) {
            return;
        }
        
        List<OrderItem> userCart = userCarts.computeIfAbsent(username, _ -> new ArrayList<>());
        
        // Check if product already in cart
        boolean found = false;
        for (OrderItem item : userCart) {
            if (productId.equals(item.getProductId())) {
                item.setQuantity(item.getQuantity() + quantity);
                found = true;
                break;
            }
        }
        
        if (!found) {
            OrderItem newItem = new OrderItem();
            newItem.setProductId(productId);
            newItem.setQuantity(quantity);
            userCart.add(newItem);
        }
        
        saveCartsToFile();
    }
    
    public synchronized List<com.shopping.model.OrderItem> getUserCart(String username) {
        if (username == null || username.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(userCarts.getOrDefault(username, new ArrayList<>()));
    }
    
    public synchronized void clearUserCart(String username) {
        if (username != null && !username.trim().isEmpty()) {
            userCarts.remove(username);
            saveCartsToFile();
        }
    }
    
    public synchronized void removeFromCart(String username, String productId) {
        if (username == null || username.trim().isEmpty()) {
            return;
        }
        
        List<com.shopping.model.OrderItem> userCart = userCarts.get(username);
        if (userCart != null) {
            userCart.removeIf(item -> productId.equals(item.getProductId()));
            saveCartsToFile();
        }
    }

    // User Authentication
    public boolean authenticateUser(String username, String password) {
        try {
            return fileHandler.authenticateUser(username, password);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean registerUser(String username, String password, String email) {
        try {
            return fileHandler.registerUser(username, password, email);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Keep only one implementation of getProducts() and searchProducts()
    // The implementations at the top of the file are the ones we want to keep
    
    // Order and Cart Management
    public static class OrderResult {
        public final String orderId;
        public final String status;
        public final String message;
        
        public OrderResult(String orderId, String status, String message) {
            this.orderId = orderId;
            this.status = status;
            this.message = message;
        }
    }
    
    public OrderResult placeOrder(String username, List<OrderItem> items) {
        if (username == null || username.trim().isEmpty()) {
            return new OrderResult(null, "error", "Username is required");
        }
        
        if (items == null || items.isEmpty()) {
            return new OrderResult(null, "error", "No items in cart");
        }
        
        try {
            // Validate stock before proceeding
            List<Product> products = fileHandler.loadProductsAsObjects();
            Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
            
            // Check stock for all items
            for (OrderItem item : items) {
                Product product = productMap.get(item.getProductId());
                if (product == null) {
                    return new OrderResult(null, "error", "Product not found: " + item.getProductId());
                }
                if (product.getStock() < item.getQuantity()) {
                    return new OrderResult(null, "error", "Insufficient stock for product: " + product.getName());
                }
            }
            
            // Update stock
            for (OrderItem item : items) {
                Product product = productMap.get(item.getProductId());
                product.setStock(product.getStock() - item.getQuantity());
            }
            
            // Save updated products
            fileHandler.saveProductsAsObjects(products);
            
            // Generate order ID
            String orderId = "ORD" + System.currentTimeMillis();
            
            // Save order
            fileHandler.saveOrder(orderId, username, items, "pending");
            
            // Clear cart only after successful order
            userCarts.remove(username);
            saveCartsToFile(); // This will remove the cart entries from orders.txt
            
            return new OrderResult(orderId, "success", "Order placed successfully");
            
        } catch (IOException e) {
            e.printStackTrace();
            return new OrderResult(null, "error", "Failed to place order: " + e.getMessage());
        }
    }
}
