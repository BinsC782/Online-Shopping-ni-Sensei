package com.shopping;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.time.Instant;

import com.shopping.model.Product;
import com.shopping.model.User;
import com.shopping.data.FileHandler;


public class ShoppingService {
    // Lock to ensure atomic order placement (validate -> decrement -> persist -> record)
    private final Object orderLock = new Object();

    // Products
    public List<Product> getProducts() {
        return FileHandler.loadProductsAsObjects();
    }

    public List<Product> searchProducts(String term) {
        String q = term == null ? "" : term.toLowerCase(Locale.ROOT);
        List<Product> all = getProducts();
        List<Product> out = new ArrayList<>();
        for (Product p : all) {
            if (p.getName().toLowerCase(Locale.ROOT).contains(q)
             || p.getDescription().toLowerCase(Locale.ROOT).contains(q)
             || p.getCategory().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(p);
            }
        }
        return out;
    }

    // Users
    public User authenticateUser(String username, String password) {
        return FileHandler.authenticateUser(username, password);
    }

    public boolean registerUser(String username, String password, String email) {
        return FileHandler.registerUser(username, password, email);
    }

    // Orders
    public OrderResult placeOrder(String username, List<OrderItem> items) {
        if (username == null || username.isBlank()) {
            return new OrderResult(null, "error", "Missing username");
        }
        if (items == null || items.isEmpty()) {
            return new OrderResult(null, "error", "Order has no items");
        }

        synchronized (orderLock) {
            List<Product> products = FileHandler.loadProductsAsObjects();
            Map<Integer, Product> byId = new HashMap<>();
            for (Product p : products) byId.put(p.getId(), p);

            // Validate
            for (OrderItem it : items) {
                Product p = byId.get(it.id);
                if (p == null) {
                    return new OrderResult(null, "error", "Invalid product id: " + it.id);
                }
                if (it.qty <= 0) {
                    return new OrderResult(null, "error", "Invalid quantity for id: " + it.id);
                }
                if (p.getStock() < it.qty) {
                    return new OrderResult(null, "error", "Insufficient stock for id: " + it.id);
                }
            }

            // Decrement stock
            for (OrderItem it : items) {
                Product p = byId.get(it.id);
                p.setStock(p.getStock() - it.qty);
            }

            // Persist updates
            FileHandler.saveProductsAsObjects(products);

            // Record order in orders.txt (compact products list)
            String orderId = "ORD-" + Instant.now().toEpochMilli();
            List<String> compact = new ArrayList<>();
            for (OrderItem it : items) compact.add(it.id + "x" + it.qty);
            FileHandler.saveOrder(orderId, username, compact, "placed");

            return new OrderResult(orderId, "ok", null);
        }
    }

    // Helper DTOs to avoid adding new files for now
    public static class OrderItem {
        public int id;
        public int qty;
        public OrderItem(int id, int qty) { this.id = id; this.qty = qty; }
    }

    public static class OrderResult {
        public String orderId;
        public String status;
        public String message;
        public OrderResult(String orderId, String status, String message) {
            this.orderId = orderId; this.status = status; this.message = message;
        }
    }
}
