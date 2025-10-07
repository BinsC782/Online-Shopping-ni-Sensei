// src/main/java/com/shopping/model/Product.java
// 
package com.shopping.model;

public class Product {
    private static final int ID_LENGTH = 6;
    private String id;
    private String name;
    private double price;
    private String description;
    private String category;
    private int stock;
    private String sellerName;
    private String sellerLocation;
    private double rating;
    private String reviews;
    // Optional image filename (e.g., "Attack Shark Keyboard.jpg") relative to web/Photos/
    private String image;

    // Simple constructor for basic product creation
    public Product(String id, String name, double price, int stock) {
        validateProductId(id);
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.description = "";
        this.category = "";
        this.sellerName = "";
        this.sellerLocation = "";
        this.rating = 0.0;
        this.reviews = "";
    }
    
    // Full constructor with all fields
    public Product(String id, String name, double price, String description, String category, int stock,
                  String sellerName, String sellerLocation, double rating, String reviews) {

        validateProductId(id);
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.category = category;
        this.stock = stock;
        this.sellerName = sellerName;
        this.sellerLocation = sellerLocation;
        this.rating = rating;
        this.reviews = reviews;
    }

    private void validateProductId(String id) {
        if (id == null || id.length() != ID_LENGTH || !id.matches("\\d{" + ID_LENGTH + "}")) {
            throw new IllegalArgumentException("Product ID must be a " + ID_LENGTH + "-digit number");
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        validateProductId(id);
        this.id = id;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerLocation() {
        return sellerLocation;
    }

    public void setSellerLocation(String sellerLocation) {
        this.sellerLocation = sellerLocation;
    }

    public double getRating() {
        return rating;
    }
    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getReviews() {
        return reviews;
    }

    public void setReviews(String reviews) {
        this.reviews = reviews;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}