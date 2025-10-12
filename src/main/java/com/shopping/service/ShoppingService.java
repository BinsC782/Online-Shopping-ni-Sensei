package com.shopping.service;

import com.shopping.data.FileHandler;
import com.shopping.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShoppingService {
    private static final Logger logger = Logger.getLogger(ShoppingService.class.getName());
    private static final String CART_STATUS = "CART";
    private static final int MAX_CART_ITEMS = 100;
    private static final int MAX_QUANTITY = 100;
    
    private final FileHandler fileHandler;
    private final ReentrantLock orderLock = new ReentrantLock(true);
    private final Map<String, List<OrderItem>> userCarts = new HashMap<>();
    
    public ShoppingService(FileHandler fileHandler) {
        this.fileHandler = fileHandler;
        // FileHandler constructor already initializes data files
        // No need to call initializeDataFiles() again
        loadCartsFromFile();
    }

    /**
     * Returns hardcoded product data for initial application setup
     * @return List of hardcoded products
     */
    private List<Product> getHardcodedProducts() {
        List<Product> products = new ArrayList<>();

        products.add(new Product("000001", "Garuda Wireless Mouse", 9.99, 50));
        products.get(0).setDescription("High-precision wireless mouse with ergonomic design");
        products.get(0).setCategory("Electronics");
        products.get(0).setRating(4.5);
        products.get(0).setImage("GarudaHawk.jpg");

        products.add(new Product("000002", "Casio Watch", 29.50, 30));
        products.get(1).setDescription("Classic analog watch with leather strap");
        products.get(1).setCategory("Fashion");
        products.get(1).setRating(4.2);
        products.get(1).setImage("Casio Watch.jpg");

        products.add(new Product("000003", "iPhone 16", 749.00, 15));
        products.get(2).setDescription("Latest iPhone with advanced camera system");
        products.get(2).setCategory("Electronics");
        products.get(2).setRating(4.8);
        products.get(2).setImage("Iphone 16.jpg");

        products.add(new Product("000004", "Varsity Jacket", 4.49, 100));
        products.get(3).setDescription("Classic college-style varsity jacket");
        products.get(3).setCategory("Fashion");
        products.get(3).setRating(4.0);
        products.get(3).setImage("NU jacket.jpg");

        products.add(new Product("000005", "Apple Earpods", 299.00, 25));
        products.get(4).setDescription("Premium wireless earbuds with noise cancellation");
        products.get(4).setCategory("Electronics");
        products.get(4).setRating(4.7);
        products.get(4).setImage("Airpods.jpg");

        products.add(new Product("000006", "Crocs", 49.00, 60));
        products.get(5).setDescription("Comfortable and lightweight casual footwear");
        products.get(5).setCategory("Footwear");
        products.get(5).setRating(4.1);
        products.get(5).setImage("Crocs.jpg");

        products.add(new Product("000007", "Adidas Samba", 59.00, 40));
        products.get(6).setDescription("Classic soccer-inspired casual sneakers");
        products.get(6).setCategory("Footwear");
        products.get(6).setRating(4.3);
        products.get(6).setImage("Adidas_Samba-removebg-preview.png");

        products.add(new Product("000008", "Fstoppers Shoulder Bag", 19.99, 35));
        products.get(7).setDescription("Professional camera bag for photographers");
        products.get(7).setCategory("Accessories");
        products.get(7).setRating(4.4);
        products.get(7).setImage("FstopperBag.jpg");

        products.add(new Product("000009", "Sunglasses", 9.99, 80));
        products.get(8).setDescription("UV protection sunglasses with polarized lenses");
        products.get(8).setCategory("Accessories");
        products.get(8).setRating(3.9);
        products.get(8).setImage("Sunglass.jpg");

        products.add(new Product("000010", "Thermos Flask", 29.50, 45));
        products.get(9).setDescription("Insulated stainless steel water bottle");
        products.get(9).setCategory("Home & Kitchen");
        products.get(9).setRating(4.6);
        products.get(9).setImage("ThermFlask.jpg");

        products.add(new Product("000011", "Iconic Socks", 4.49, 120));
        products.get(10).setDescription("Comfortable cotton blend socks");
        products.get(10).setCategory("Clothing");
        products.get(10).setRating(4.2);
        products.get(10).setImage("Socks.jpg");

        products.add(new Product("000012", "Asus Monitor", 749.00, 10));
        products.get(11).setDescription("4K UHD gaming monitor with high refresh rate");
        products.get(11).setCategory("Electronics");
        products.get(11).setRating(4.7);
        products.get(11).setImage("AsusMonitor.jpg");

        products.add(new Product("000013", "Kingston NVME SSD", 299.00, 20));
        products.get(12).setDescription("High-speed NVMe solid state drive");
        products.get(12).setCategory("Electronics");
        products.get(12).setRating(4.5);
        products.get(12).setImage("KingstonSSD.jpg");

        products.add(new Product("000014", "PC Case", 49.00, 30));
        products.get(13).setDescription("ATX mid-tower gaming computer case");
        products.get(13).setCategory("Electronics");
        products.get(13).setRating(4.0);
        products.get(13).setImage("PcCase.jpg");

        return products;
    }
    
    /**
     * Searches for products by name (case-insensitive)
     * @param searchTerm The search term to look for in product names
     * @return List of matching products, or empty list if none found
     */
    public List<Product> searchProducts(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getProducts();
        }

        String term = searchTerm.toLowerCase().trim();
        return getProducts().stream()
            .filter(p -> p.getName().toLowerCase().contains(term))
            .collect(Collectors.toList());
    }
    public List<Product> getProducts() {
        List<Product> products = fileHandler.loadProducts();
        if (products.isEmpty()) {
            // Return hardcoded products if no file data exists
            return getHardcodedProducts();
        }
        return products;
    }

    public Product getProductById(String productId) { 
        if (productId == null || productId.trim().isEmpty()) {
            return null;
        }
        return getProducts().stream()
            .filter(p -> productId.equals(p.getId()))
            .findFirst()
            .orElse(null);
    }

    public boolean updateStock(String productId, int quantity) { 
        if (productId == null || productId.trim().isEmpty() || quantity < 0) {
            return false;
        }

        orderLock.lock();
        try {
            List<Product> products = getProducts();
            for (Product product : products) {
                if (productId.equals(product.getId())) {
                    int newStock = product.getStock() - quantity;
                    if (newStock < 0) {
                        return false;
                    }
                    product.setStock(newStock);
                    fileHandler.saveProducts(products);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error updating stock: " + e.getMessage());
            return false;
        } finally {
            orderLock.unlock();
        }
    }
    
    private void loadCartsFromFile() {
        orderLock.lock();
        try {
            if (!Files.exists(Paths.get(FileHandler.ORDERS_FILE))) {
                logger.info("No orders file found, starting with empty carts");
                return;
            }
            
            List<String> orderLines = Files.readAllLines(Paths.get(FileHandler.ORDERS_FILE));
            Map<String, List<OrderItem>> tempCarts = new HashMap<>();
            
            for (String line : orderLines) {
                if (line == null || line.trim().isEmpty()) continue;
                
                try {
                    String[] parts = line.split(",", 5);
                    if (parts.length == 5 && CART_STATUS.equals(parts[3])) {
                        String username = parts[1];
                        String[] items = parts[4].split(";");
                        List<OrderItem> cartItems = tempCarts.computeIfAbsent(username, _ -> new ArrayList<>());
                        
                        for (String item : items) {
                            if (item == null || item.trim().isEmpty()) continue;
                            
                            String[] itemParts = item.split(":");
                            if (itemParts.length == 2) {
                                String productId = itemParts[0].trim();
                                try {
                                    int quantity = Math.min(Integer.parseInt(itemParts[1].trim()), MAX_QUANTITY);
                                    if (quantity <= 0) continue;
                                    
                                    Product product = getProductById(productId);
                                    if (product != null && product.getStock() > 0) {
                                        // Check if product already exists in cart
                                        Optional<OrderItem> existingItem = cartItems.stream()
                                            .filter(i -> productId.equals(i.getProductId()))
                                            .findFirst();
                                            
                                        if (existingItem.isPresent()) {
                                            // Update quantity if product already in cart
                                            OrderItem itemToUpdate = existingItem.get();
                                            itemToUpdate.setQuantity(Math.min(itemToUpdate.getQuantity() + quantity, MAX_QUANTITY));
                                        } else if (cartItems.size() < MAX_CART_ITEMS) {
                                            // Add new item if cart not full
                                            cartItems.add(new OrderItem(
                                                productId,
                                                product.getName(),
                                                product.getPrice(),
                                                Math.min(quantity, product.getStock())
                                            ));
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    logger.warning("Invalid quantity in cart item: " + item);
                                }
                            }
                        }
                        
                        // Remove empty carts
                        if (cartItems.isEmpty()) {
                            tempCarts.remove(username);
                        }
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error parsing cart line: " + line, e);
                }
            }
            
            // Only update the carts map if we successfully loaded everything
            userCarts.clear();
            userCarts.putAll(tempCarts);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading carts from file", e);
        } finally {
            orderLock.unlock();
        }
    }
    
    private synchronized void saveCartsToFile() {
        if (fileHandler == null) {
            logger.warning("FileHandler is not initialized");
            return;
        }
        
        orderLock.lock();
        try {
            // First, remove all existing cart entries from the orders file
            for (String username : userCarts.keySet()) {
                try {
                    fileHandler.removeCartForUser(username);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error removing cart for user: " + username, e);
                }
            }
            
            // Then save the current cart state
            for (Map.Entry<String, List<OrderItem>> entry : userCarts.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    String cartId = "cart_" + entry.getKey() + "_" + System.currentTimeMillis();
                    try {
                        // Convert OrderItems to product strings in the format "productId:quantity"
                        List<String> productList = new ArrayList<>();
                        for (OrderItem item : entry.getValue()) {
                            productList.add(item.getProductId() + ":" + item.getQuantity());
                        }
                        
                        // Create and save the cart as an order with CART status
                        Order cartOrder = new Order(cartId, entry.getKey(), productList);
                        cartOrder.setOrderStatus(CART_STATUS);
                        fileHandler.saveOrder(cartOrder);
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Error saving cart for user: " + entry.getKey(), e);
                    }
                }
            }
        } finally {
            orderLock.unlock();
        }
    }

    /**
     * Adds a product to the user's shopping cart
     * @param username The username of the user
     * @param productId The ID of the product to add
     * @param quantity The quantity to add (must be positive)
     * @return true if the item was added successfully, false otherwise
     */
    public boolean addToCart(String username, String productId, int quantity) {
        if (username == null || username.trim().isEmpty() || 
            productId == null || productId.trim().isEmpty() || 
            quantity <= 0 || quantity > MAX_QUANTITY) {
            logger.warning("Invalid parameters for addToCart - username: " + username + 
                         ", productId: " + productId + ", quantity: " + quantity);
            return false;
        }
        
        orderLock.lock();
        try {
            // Check if product exists and has sufficient stock
            Product product = getProductById(productId);
            if (product == null) {
                logger.warning("Product not found: " + productId);
                return false;
            }
            
            if (product.getStock() <= 0) {
                logger.warning("Product out of stock: " + productId);
                return false;
            }
            
            List<OrderItem> userCart = userCarts.computeIfAbsent(username, _ -> new ArrayList<>());
            
            // Check if cart is full and item not already in cart
            Optional<OrderItem> existingItem = userCart.stream()
                .filter(item -> productId.equals(item.getProductId()))
                .findFirst();
                
            if (existingItem.isPresent()) {
                // Update existing item quantity, ensuring it doesn't exceed stock or max quantity
                OrderItem item = existingItem.get();
                int newQuantity = Math.min(item.getQuantity() + quantity, 
                                         Math.min(product.getStock(), MAX_QUANTITY));
                if (newQuantity <= 0) {
                    userCart.removeIf(i -> productId.equals(i.getProductId()));
                    if (userCart.isEmpty()) {
                        userCarts.remove(username);
                    }
                } else {
                    item.setQuantity(newQuantity);
                }
            } else {
                // Add new item if cart isn't full
                if (userCart.size() >= MAX_CART_ITEMS) {
                    logger.warning("Cart is full for user: " + username);
                    return false;
                }
                
                userCart.add(new OrderItem(
                    productId,
                    product.getName(),
                    product.getPrice(),
                    Math.min(quantity, Math.min(product.getStock(), MAX_QUANTITY))
                ));
            }
            
            saveCartsToFile();
            return true;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error adding item to cart", e);
            return false;
        } finally {
            orderLock.unlock();
        }
    }
    
    /**
     * Gets a user's shopping cart with product details
     * @param username The username of the user
     * @return A list of OrderItems in the user's cart, or empty list if none
     */
    public List<OrderItem> getUserCart(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        orderLock.lock();
        try {
            List<OrderItem> cart = userCarts.get(username);
            if (cart == null || cart.isEmpty()) {
                return Collections.emptyList();
            }
            
            // Create a deep copy to prevent external modification
            List<OrderItem> result = new ArrayList<>(cart.size());
            for (OrderItem item : cart) {
                // Verify product still exists and has stock
                Product product = getProductById(item.getProductId());
                if (product != null && product.getStock() > 0) {
                    // Update price and name from current product data
                    OrderItem copy = new OrderItem(
                        item.getProductId(),
                        product.getName(),
                        product.getPrice(),
                        Math.min(item.getQuantity(), product.getStock())
                    );
                    result.add(copy);
                }
            }
            
            // If any items were removed due to being out of stock, update the cart
            if (result.size() != cart.size()) {
                if (result.isEmpty()) {
                    userCarts.remove(username);
                } else {
                    userCarts.put(username, new ArrayList<>(result));
                }
                saveCartsToFile();
            }
            
            return result;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting user cart", e);
            return Collections.emptyList();
        } finally {
            orderLock.unlock();
        }
    }
    
    /**
     * Removes a product from the user's shopping cart
     * @param username The username of the user
     * @param productId The ID of the product to remove
     * @return true if the item was removed, false if it wasn't in the cart
     */
    public boolean removeFromCart(String username, String productId) {
        if (username == null || username.trim().isEmpty() || 
            productId == null || productId.trim().isEmpty()) {
            return false;
        }
        
        orderLock.lock();
        try {
            List<OrderItem> userCart = userCarts.get(username);
            if (userCart != null) {
                boolean removed = userCart.removeIf(item -> productId.equals(item.getProductId()));
                if (removed) {
                    if (userCart.isEmpty()) {
                        userCarts.remove(username);
                    }
                    saveCartsToFile();
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error removing item from cart", e);
            return false;
        } finally {
            orderLock.unlock();
        }
    }
    
    /**
     * Clears the user's shopping cart
     * @param username The username of the user
     * @return true if the cart was cleared, false if it was already empty
     */
    public boolean clearUserCart(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        orderLock.lock();
        try {
            boolean hadItems = userCarts.remove(username) != null;
            if (hadItems) {
                try {
                    fileHandler.removeCartForUser(username);
                    return true;
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error clearing cart for user: " + username, e);
                    return false;
                }
            }
            return false;
        } finally {
            orderLock.unlock();
        }
    }
    
    /**
     * Registers a new user
     * @param username The username (must be unique)
     * @param password The password (will be hashed)
     * @param email The user's email address
     * @return true if registration was successful, false otherwise
     */
    /**
     * Authenticates a user with the given username and password
     * @param username The username to authenticate
     * @param password The password to verify
     * @return true if authentication is successful, false otherwise
     */
    public void processCheckout(String orderLine) throws Exception {
        // 1. Save the order to the orders.txt file
        fileHandler.saveOrder(orderLine);

        // 2. Clear the cart file (using 'guest' as the user ID for consistency)
        fileHandler.removeCartForUser("guest");
    }
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
        if (username == null || items == null || items.isEmpty()) {
            return new OrderResult(null, "error", "Invalid order request");
        }
        
        orderLock.lock();
        try {
            // Check stock first
            for (OrderItem item : items) {
                Product product = getProductById(item.getProductId());
                if (product == null || product.getStock() < item.getQuantity()) {
                    return new OrderResult(null, "error", 
                        "Product out of stock or not available: " + item.getProductId());
                }
            }
            
            // Update stock and create order
            for (OrderItem item : items) {
                if (!updateStock(item.getProductId(), item.getQuantity())) {
                    return new OrderResult(null, "error", 
                        "Failed to update stock for product: " + item.getProductId());
                }
            }
            
            // Create order
            String orderId = "ord_" + System.currentTimeMillis();
            
            // Convert OrderItems to product strings in the format "productId:quantity"
            List<String> productList = new ArrayList<>();
            for (OrderItem item : items) {
                productList.add(item.getProductId() + ":" + item.getQuantity());
            }
            
            // Create and save the order
            Order order = new Order(orderId, username, productList);
            order.setOrderStatus("COMPLETED");
            fileHandler.saveOrder(order);
            
            // Clear cart
            clearUserCart(username);
            
            return new OrderResult(orderId, "success", "Order placed successfully");
            
        } catch (Exception e) {
            System.err.println("Error placing order: " + e.getMessage());
            return new OrderResult(null, "error", "Failed to place order: " + e.getMessage());
        } finally {
            orderLock.unlock();
        }
    }

    /**
     * Authenticates a user with the given username and password.
     * @param username The username to authenticate
     * @param password The password to verify
     * @return true if authentication is successful, false otherwise
     */
    public boolean authenticateUser(String username, String password) {
        try {
            return fileHandler.authenticateUser(username, password) != null;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error authenticating user: " + username, e);
            return false;
        }
    }

    /**
     * Registers a new user.
     * @param username The username (must be unique)
     * @param password The password
     * @param email The user's email address
     * @return true if registration was successful, false otherwise
     */
    public boolean registerUser(String username, String password, String email) {
        try {
            return fileHandler.registerUser(username, password, email);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error registering user: " + username, e);
            return false;
        }
    }

    /**
     * Retrieves orders for a specific user.
     * @param username The username to get orders for
     * @return List of orders for the user
     */
    public List<Order> getUserOrders(String username) {
        return fileHandler.getUserOrders(username);
    }
}