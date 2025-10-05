package com.shopping.model;

import java.util.List;

public class Order {
    private String orderId;
    private String userId;
    private List<String> productList;
    private String orderStatus;

    public Order(String orderId, String userId, List<String> productList) {
        this.orderId = orderId;
        this.userId = userId;
        this.productList = productList;
        this.orderStatus = "Pending"; // Default status
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getProductList() {
        return productList;
    }

    public void setProductList(List<String> productList) {
        this.productList = productList;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public void updateOrderStatus(String status) {
        this.orderStatus = status;
    }
}