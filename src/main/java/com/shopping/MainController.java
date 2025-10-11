package com.shopping;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.control.Alert.AlertType;

import com.shopping.model.Product;
import com.shopping.model.Cart;
import com.shopping.model.OrderItem;
import com.shopping.service.ShoppingService;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.geometry.Pos;
import javafx.fxml.Initializable; 

/**
 * Main controller for the JavaFX Shopping Application.
 * Handles user interactions and coordinates between UI and business logic.
 */
public class MainController implements Initializable {

    @FXML private VBox mainContainer;
    @FXML private HBox headerBar;
    @FXML private Label logoLabel;
    @FXML private HBox searchSection;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private HBox userActions;
    @FXML private Label cartTotalLabel;
    @FXML private Button checkoutButton;
    @FXML private Button logoutButton;
    @FXML private HBox categoryBar;
    @FXML private Label categoriesLabel;
    @FXML private ToggleButton allCategoriesBtn;
    @FXML private ToggleButton accessoriesBtn;
    @FXML private ToggleButton homeLifestyleBtn;
    @FXML private ToggleButton electronicsBtn;
    @FXML private ToggleButton fashionBtn;
    @FXML private ScrollPane productsScrollPane;
    @FXML private TilePane productGrid;

    // ToggleGroup for category buttons
    private ToggleGroup categoryToggleGroup;

    // ObservableList for reactive product filtering
    private ObservableList<Product> allProductsList;
    private FilteredList<Product> filteredProducts;

    private ShoppingService shoppingService;
    private Cart shoppingCart;
    private List<Product> allProducts;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize shopping cart
        shoppingCart = new Cart();

        // Set up event handlers
        setupEventHandlers();

        // Load and display products - but only if service is available
        System.out.println("=== INITIALIZE CALLED ===");
        System.out.println("ShoppingService at init: " + (shoppingService != null ? "NOT NULL" : "NULL"));

        // If service is not set yet, wait for it
        if (shoppingService == null) {
            System.out.println("Service not ready yet, will load products when service is set");
        } else {
            System.out.println("Service is ready, loading products immediately...");
            loadProducts();
        }
    }

    /**
     * Set the shopping service instance
     */
    public void setShoppingService(ShoppingService shoppingService) {
        System.out.println("=== SETTING SHOPPING SERVICE ===");
        System.out.println("Service provided: " + (shoppingService != null ? "NOT NULL" : "NULL"));
        this.shoppingService = shoppingService;
        System.out.println("Service set on controller: " + (this.shoppingService != null ? "SUCCESS" : "FAILED"));

        // If initialize() already ran but service wasn't ready, load products now
        if (this.shoppingService != null && allProducts == null) {
            System.out.println("Service is now ready, loading products...");
            loadProducts();
        }
    }

    /**
     * Set up all event handlers for UI components
     */
    private void setupEventHandlers() {
        // Initialize category toggle group
        categoryToggleGroup = new ToggleGroup();

        // Set up category toggle buttons if they exist
        if (allCategoriesBtn != null) allCategoriesBtn.setToggleGroup(categoryToggleGroup);
        if (accessoriesBtn != null) accessoriesBtn.setToggleGroup(categoryToggleGroup);
        if (homeLifestyleBtn != null) homeLifestyleBtn.setToggleGroup(categoryToggleGroup);
        if (electronicsBtn != null) electronicsBtn.setToggleGroup(categoryToggleGroup);
        if (fashionBtn != null) fashionBtn.setToggleGroup(categoryToggleGroup);

        // Category filtering
        if (categoryToggleGroup != null) {
            categoryToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
                if (newToggle != null) {
                    filterProductsByCategory();
                }
            });
        }

        // Search functionality
        if (searchButton != null) searchButton.setOnAction(e -> performSearch());
        if (searchField != null) searchField.setOnAction(e -> performSearch());

        // Checkout button
        if (checkoutButton != null) checkoutButton.setOnAction(e -> performCheckout());

        // Logout button
        if (logoutButton != null) logoutButton.setOnAction(e -> performLogout());
    }

    /**
     * Filter products by selected category
     */
    private void filterProductsByCategory() {
        if (allProducts == null || allProducts.isEmpty()) {
            return;
        }

        ToggleButton selectedButton = (ToggleButton) categoryToggleGroup.getSelectedToggle();
        if (selectedButton == null) {
            return;
        }

        String category = selectedButton.getText();
        List<Product> filtered;

        if ("All Categories".equals(category)) {
            filtered = allProducts;
        } else {
            filtered = allProducts.stream()
                .filter(product -> category.equals(product.getCategory()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }

        displayProducts(filtered);
        System.out.println("Filtered to " + filtered.size() + " products in category: " + category);
    }

    /**
     * Perform logout action
     */
    private void performLogout() {
        // Clear cart
        shoppingCart.getItems().clear();
        updateCartDisplay();

        // Show confirmation
        showInfo("Logged out successfully");

        // TODO: Navigate back to login screen if needed
    }

    /**
     * Load all products and display them
     */
    private void loadProducts() {
        if (shoppingService == null) {
            System.err.println("ERROR: Shopping service is null!");
            // Don't show error dialog - wait for service to be properly injected
            return;
        }

        try {
            allProducts = shoppingService.getProducts();
            System.out.println("=== DEBUG: Loaded " + allProducts.size() + " products ===");
            for (Product p : allProducts) {
                System.out.println("Product: " + p.getName() + " - ID: " + p.getId() + " - Image: " + p.getImage());
            }
            System.out.println("=== END DEBUG ===");

            if (allProducts.isEmpty()) {
                System.err.println("ERROR: No products loaded!");
                showError("No products found in database");
                return;
            }

            displayProducts(allProducts);
        } catch (Exception e) {
            System.err.println("ERROR loading products: " + e.getMessage());
            showError("Failed to load products: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Display products in the UI using TilePane
     */
    private void displayProducts(List<Product> products) {
        productGrid.getChildren().clear();
        System.out.println("Displaying " + products.size() + " products in TilePane");

        for (Product product : products) {
            try {
                VBox productCard = createProductCard(product);
                productGrid.getChildren().add(productCard);
                System.out.println("Added product card: " + product.getName());
            } catch (Exception e) {
                System.err.println("Error creating product card for: " + product.getName() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Create a product card UI component that matches the original HTML design exactly
     * Structure: Image on top, then product-info section with name, price, and rating
     */
    private VBox createProductCard(Product product) {
        VBox card = new VBox();
        card.getStyleClass().add("product-card");
        card.setPrefWidth(220);

        // Make the entire card clickable - ADD VISUAL FEEDBACK
        card.setOnMouseClicked(e -> {
            card.setStyle("-fx-background-color: #ffcc00;"); // yellow background
            System.out.println("=== CLICKED PRODUCT CARD: " + product.getName() + " ===");
            showProductModal(product);
        });

        // Also add click to image for testing
        ImageView productImage = new ImageView();
        productImage.getStyleClass().add("product-image");
        productImage.setFitWidth(180);
        productImage.setFitHeight(150);
        productImage.setPreserveRatio(true);
        productImage.setOnMouseClicked(e -> {
            productImage.setStyle("-fx-border-color: #ff0000; -fx-border-width: 2;"); // red border
            System.out.println("=== CLICKED PRODUCT IMAGE: " + product.getName() + " ===");
            showProductModal(product);
        });

        // Load image from resources - match HTML img src structure
        String imageName = getProductImageName(product);
        URL imageUrl = getClass().getResource("/images/" + imageName);

        if (imageUrl != null) {
            try {
                Image image = new Image(imageUrl.toString());
                productImage.setImage(image);
                System.out.println("Loaded image for " + product.getName() + ": " + imageUrl.toString());
            } catch (Exception e) {
                System.err.println("Failed to load image for " + product.getName() + ": " + e.getMessage());
                setSimplePlaceholderImage(productImage, product);
            }
        } else {
            System.err.println("Image URL is null for " + product.getName() + " (imageName: " + imageName + ")");
            setSimplePlaceholderImage(productImage, product);
        }

        // Product information section - exactly like HTML div.product-info
        VBox productInfo = new VBox(6);
        productInfo.getStyleClass().add("product-info");
        productInfo.setAlignment(Pos.CENTER_LEFT);

        // Product name - equivalent to h3
        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("product-name");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(180);

        // Price - equivalent to p
        Label priceLabel = new Label(String.format("$%.2f", product.getPrice()));
        priceLabel.getStyleClass().add("product-price");

        // Rating - equivalent to span with stars (using actual rating from business logic)
        String stars = generateStarsFromRating(product.getRating());
        Label ratingLabel = new Label(stars);
        ratingLabel.getStyleClass().add("product-rating");

        // Add all elements to product info section
        productInfo.getChildren().addAll(nameLabel, priceLabel, ratingLabel);

        // Action buttons section (enhanced for JavaFX functionality)
        HBox buttonBox = new HBox(10);
        buttonBox.getStyleClass().add("product-buttons");
        buttonBox.setAlignment(Pos.CENTER);

        Button addToCartButton = new Button("Add to Cart");
        Button viewDetailsButton = new Button("View Details");

        addToCartButton.getStyleClass().add("add-to-cart-btn");
        viewDetailsButton.getStyleClass().add("view-details-btn");

        // Set button actions
        addToCartButton.setOnAction(e -> {
            System.out.println("=== ADD TO CART BUTTON CLICKED: " + product.getName() + " ===");
            addToCart(product);
        });
        viewDetailsButton.setOnAction(e -> {
            System.out.println("=== VIEW DETAILS BUTTON CLICKED: " + product.getName() + " ===");
            showProductModal(product);
        });

        buttonBox.getChildren().addAll(addToCartButton, viewDetailsButton);

        // Assemble card - Image on top, then product-info, then buttons
        card.getChildren().addAll(productImage, productInfo, buttonBox);

        return card;
    }


    /**
     * Get the appropriate image name for a product
     */
    private String getProductImageName(Product product) {
        // Return the actual image filename from the product data
        String imageName = product.getImage();
        return imageName != null && !imageName.isEmpty() ? imageName : "placeholder.jpg";
    }

    /**
     * Set simple placeholder image when actual image is not available
     * Creates a simple colored rectangle with product initial - matches HTML design
     */
    private void setSimplePlaceholderImage(ImageView imageView, Product product) {
        // Create a simple colored background
        javafx.scene.shape.Rectangle bg = new javafx.scene.shape.Rectangle(180, 150);
        bg.setFill(javafx.scene.paint.Color.LIGHTGRAY);

        // Add text with product initial
        String initial = product.getName().length() > 0 ?
            product.getName().substring(0, 1).toUpperCase() : "P";

        javafx.scene.text.Text text = new javafx.scene.text.Text(initial);
        text.setFont(javafx.scene.text.Font.font("Arial", 36));
        text.setFill(javafx.scene.paint.Color.WHITE);

        // Center the text
        javafx.scene.Group group = new javafx.scene.Group();
        group.getChildren().addAll(bg, text);

        // Position text in center
        text.setTranslateX(90 - text.getLayoutBounds().getWidth()/2);
        text.setTranslateY(75 + text.getLayoutBounds().getHeight()/4);

        // Create snapshot as image
        javafx.scene.image.WritableImage image = new javafx.scene.image.WritableImage(180, 150);
        group.snapshot(null, image);
        imageView.setImage(image);
    }

    /**
     * Perform product search
     */
    private void performSearch() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            displayProducts(allProducts);
        } else {
            try {
                List<Product> searchResults = shoppingService.searchProducts(searchTerm);
                displayProducts(searchResults);
            } catch (Exception e) {
                showError("Search failed: " + e.getMessage());
            }
        }
    }

    /**
     * Add product to shopping cart
     */
    private void addToCart(Product product) {
        try {
            // Create OrderItem from Product
            OrderItem orderItem = new OrderItem(
                String.valueOf(product.getId()),
                product.getName(),
                product.getPrice(),
                1  // quantity
            );

            // Add to cart
            shoppingCart.addItem(orderItem);
            updateCartDisplay();
            showInfo("Product added to cart: " + product.getName());
        } catch (Exception e) {
            showError("Failed to add to cart: " + e.getMessage());
        }
    }

    /**
     * Show product modal dialog with detailed product information
     * Matches the HTML modal structure with image, details, quantity selector, and Add to Cart button
     */
    private void showProductModal(Product product) {
        // Create modal dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Product Details");
        dialog.setHeaderText(product.getName());

        // Create main content layout
        VBox content = new VBox(15);
        content.setPrefWidth(400);

        // Product image section
        ImageView modalImage = new ImageView();
        modalImage.setFitWidth(200);
        modalImage.setFitHeight(150);
        modalImage.setPreserveRatio(true);

        // Load product image
        String imageName = getProductImageName(product);
        URL imageUrl = getClass().getResource("/images/" + imageName);

        if (imageUrl != null) {
            try {
                Image image = new Image(imageUrl.toString());
                modalImage.setImage(image);
            } catch (Exception e) {
                setSimplePlaceholderImage(modalImage, product);
            }
        } else {
            setSimplePlaceholderImage(modalImage, product);
        }

        // Product details section
        VBox detailsSection = new VBox(10);

        // Product name (large title)
        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("product-name");
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Rating from business logic
        HBox ratingBox = new HBox(5);
        ratingBox.setAlignment(Pos.CENTER_LEFT);

        // Create stars based on product rating
        String stars = generateStarsFromRating(product.getRating());
        Label ratingLabel = new Label(stars);
        ratingLabel.setStyle("-fx-text-fill: #ffa500; -fx-font-size: 16px;");

        ratingBox.getChildren().add(ratingLabel);

        // Price from business logic
        Label priceLabel = new Label(String.format("$%.2f", product.getPrice()));
        priceLabel.getStyleClass().add("product-price");
        priceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Stock from business logic
        Label stockLabel = new Label("Stock: " + product.getStock() + " available");
        stockLabel.getStyleClass().add("product-stock");
        stockLabel.setStyle("-fx-font-size: 14px;");

        detailsSection.getChildren().addAll(nameLabel, ratingBox, priceLabel, stockLabel);

        // Quantity selector section (like HTML modal)
        VBox quantitySection = new VBox(8);
        quantitySection.setAlignment(Pos.CENTER_LEFT);

        Label quantityLabel = new Label("Quantity:");
        quantityLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        HBox quantityControls = new HBox(10);
        quantityControls.setAlignment(Pos.CENTER);

        // Quantity buttons and display
        Button minusBtn = new Button("-");
        minusBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 5 10;");

        Label quantityValue = new Label("1");
        quantityValue.setStyle("-fx-font-size: 16px; -fx-padding: 0 15;");
        quantityValue.setPrefWidth(30);
        quantityValue.setAlignment(Pos.CENTER);

        Button plusBtn = new Button("+");
        plusBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 5 10;");

        quantityControls.getChildren().addAll(minusBtn, quantityValue, plusBtn);

        // Quantity event handlers
        final int[] currentQuantity = {1};
        final int maxStock = product.getStock();

        minusBtn.setOnAction(e -> {
            if (currentQuantity[0] > 1) {
                currentQuantity[0]--;
                quantityValue.setText(String.valueOf(currentQuantity[0]));
            }
        });

        plusBtn.setOnAction(e -> {
            if (currentQuantity[0] < maxStock) {
                currentQuantity[0]++;
                quantityValue.setText(String.valueOf(currentQuantity[0]));
            }
        });

        quantitySection.getChildren().addAll(quantityLabel, quantityControls);

        // Action buttons section
        HBox actionButtons = new HBox(15);
        actionButtons.setAlignment(Pos.CENTER);

        // Blue Add to Cart button (as requested)
        Button addToCartBtn = new Button("Add to Cart");
        addToCartBtn.setStyle(
            "-fx-background-color: #007bff; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 10 20; " +
            "-fx-background-radius: 5;"
        );

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
            "-fx-background-color: #6c757d; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-padding: 10 20; " +
            "-fx-background-radius: 5;"
        );

        actionButtons.getChildren().addAll(addToCartBtn, cancelBtn);

        // Add to Cart action
        addToCartBtn.setOnAction(e -> {
            try {
                // Create OrderItem with selected quantity
                OrderItem orderItem = new OrderItem(
                    String.valueOf(product.getId()),
                    product.getName(),
                    product.getPrice(),
                    currentQuantity[0]  // Use selected quantity
                );

                // Add to cart
                shoppingCart.addItem(orderItem);
                updateCartDisplay();
                showInfo("Added " + currentQuantity[0] + " x " + product.getName() + " to cart");

                dialog.close();
            } catch (Exception ex) {
                showError("Failed to add to cart: " + ex.getMessage());
            }
        });

        // Cancel action
        cancelBtn.setOnAction(e -> dialog.close());

        // Assemble all content
        content.getChildren().addAll(modalImage, detailsSection, quantitySection, actionButtons);

        // Create dialog pane and set content
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setContent(content);
        dialogPane.getButtonTypes().addAll(ButtonType.CLOSE);

        // Show dialog
        dialog.showAndWait();
    }

    /**
     * Generate star rating display from numeric rating value
     */
    private String generateStarsFromRating(double rating) {
        int fullStars = (int) rating;
        boolean hasHalfStar = (rating - fullStars) >= 0.5;
        StringBuilder stars = new StringBuilder();

        // Add full stars
        for (int i = 0; i < fullStars; i++) {
            stars.append("★");
        }

        // Add half star if needed
        if (hasHalfStar) {
            stars.append("☆"); // Using half star symbol
        }

        // Fill remaining with empty stars (max 5 stars)
        int remainingStars = 5 - fullStars - (hasHalfStar ? 1 : 0);
        for (int i = 0; i < remainingStars; i++) {
            stars.append("☆");
        }

        return stars.toString();
    }

    /**
     * Update cart display
     */
    private void updateCartDisplay() {
        double total = shoppingCart.getTotal();
        cartTotalLabel.setText(String.format("Total: $%.2f", total));

        // Update cart items count
        // TODO: Display cart items count in UI when cart view is implemented
        shoppingCart.getItems().size();  // Use the value to avoid unused variable warning
    }

    /**
     * Perform checkout process
     */
    private void performCheckout() {
        if (shoppingCart.getItems().isEmpty()) {
            showWarning("Your cart is empty");
            return;
        }

        try {
            // Convert cart items to order format and process
            var orderResult = shoppingService.placeOrder("current_user", shoppingCart.getItems());

            if ("success".equals(orderResult.status)) {
                showInfo("Order placed successfully!\nOrder ID: " + orderResult.orderId);
            } else {
                showError("Order failed: " + orderResult.message);
            }

            // Clear cart after successful order
            shoppingCart.getItems().clear();
            updateCartDisplay();

        } catch (Exception e) {
            showError("Checkout failed: " + e.getMessage());
        }
    }

    /**
     * Utility methods for showing dialogs
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
