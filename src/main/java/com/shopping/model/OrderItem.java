package com.shopping.model;

public class OrderItem {
    private String productId;  // Changed from int to String to support 6-digit format
    private int quantity;
    
    public OrderItem() {}
    
    public OrderItem(String productId, int quantity) {
        this.productId = productId;
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
}
