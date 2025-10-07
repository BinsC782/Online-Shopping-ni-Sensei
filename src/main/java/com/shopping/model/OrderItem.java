package com.shopping.model;

public class OrderItem {
    private String productId;  // Changed from int to String to support 6-digit format
    private String name;
    private double price;
    private int quantity;
    
    public OrderItem() {}
    
    public OrderItem(String productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public OrderItem(String productId, String name, double price, int quantity) { 
        this.productId = productId; 
        this.name = name; 
        this.price = price; 
        this.quantity = quantity; 
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
}
