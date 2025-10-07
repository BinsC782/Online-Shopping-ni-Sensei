package com.shopping;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import com.shopping.service.ShoppingService;
import com.shopping.model.Product;
import com.shopping.model.Cart;
import com.shopping.model.OrderItem;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Main controller for the JavaFX Shopping Application.
 * Handles user interactions and coordinates between UI and business logic.
 */
public class MainController implements Initializable {

    @FXML private BorderPane mainContainer;
    @FXML private VBox navigationBar;
    @FXML private HBox searchBar;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private FlowPane productContainer;
    @FXML private VBox cartPanel;
    @FXML private Label cartTotalLabel;
    @FXML private Button checkoutButton;

    private ShoppingService shoppingService;
    private Cart shoppingCart;
    private List<Product> allProducts;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize shopping cart
        shoppingCart = new Cart();

        // Set up event handlers
        setupEventHandlers();

        // Load and display products
        loadProducts();
    }

    /**
     * Set the shopping service instance
     */
    public void setShoppingService(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    /**
     * Set up all event handlers for UI components
     */
    private void setupEventHandlers() {
        // Search functionality
        searchButton.setOnAction(e -> performSearch());
        searchField.setOnAction(e -> performSearch());

        // Checkout button
        checkoutButton.setOnAction(e -> performCheckout());
    }

    /**
     * Load all products and display them
     */
    private void loadProducts() {
        if (shoppingService == null) {
            showError("Shopping service not initialized");
            return;
        }

        try {
            allProducts = shoppingService.getProducts();
            displayProducts(allProducts);
        } catch (Exception e) {
            showError("Failed to load products: " + e.getMessage());
        }
    }

    /**
     * Display products in the UI
     */
    private void displayProducts(List<Product> products) {
        productContainer.getChildren().clear();

        for (Product product : products) {
            VBox productCard = createProductCard(product);
            productContainer.getChildren().add(productCard);
        }
    }

    /**
     * Create a product card UI component with actual product images
     */
    private VBox createProductCard(Product product) {
        VBox card = new VBox();
        card.getStyleClass().add("product-card");
        card.setPrefWidth(220);

        // Product image - use actual product image if available
        ImageView productImage = createProductImage(product);
        productImage.getStyleClass().add("product-image");

        // Product information
        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("product-name");

        Label priceLabel = new Label(String.format("$%.2f", product.getPrice()));
        priceLabel.getStyleClass().add("product-price");

        Label stockLabel = new Label("Stock: " + product.getStock());
        stockLabel.getStyleClass().add("product-stock");

        // Rating display (if available)
        HBox ratingBox = new HBox();
        if (product.getReviews() != null && !product.getReviews().isEmpty()) {
            Label ratingLabel = new Label("★★★★★");
            ratingLabel.getStyleClass().add("product-rating");
            ratingBox.getChildren().add(ratingLabel);
        }

        // Action buttons
        HBox buttonBox = new HBox();
        Button addToCartButton = new Button("Add to Cart");
        Button viewDetailsButton = new Button("View Details");

        addToCartButton.getStyleClass().add("add-to-cart-btn");
        viewDetailsButton.getStyleClass().add("view-details-btn");

        // Event handlers
        addToCartButton.setOnAction(e -> addToCart(product));
        viewDetailsButton.setOnAction(e -> showProductDetails(product));

        buttonBox.getChildren().addAll(addToCartButton, viewDetailsButton);
        buttonBox.getStyleClass().add("product-buttons");

        card.getChildren().addAll(productImage, nameLabel, priceLabel, stockLabel, ratingBox, buttonBox);

        return card;
    }

    /**
     * Create ImageView for product with fallback handling
     */
    private ImageView createProductImage(Product product) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(200);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);

        try {
            // Try to load product image from resources
            String imageName = getProductImageName(product);
            URL imageUrl = getClass().getResource("/images/" + imageName);

            if (imageUrl != null) {
                Image image = new Image(imageUrl.toString());
                imageView.setImage(image);
            } else {
                // Fallback to placeholder if image not found
                setPlaceholderImage(imageView, product);
            }
        } catch (Exception e) {
            // Fallback to placeholder on any error
            setPlaceholderImage(imageView, product);
        }

        return imageView;
    }

    /**
     * Get the appropriate image name for a product
     */
    private String getProductImageName(Product product) {
        String productName = product.getName().toLowerCase();

        // Map product names to image files
        if (productName.contains("garuda") || productName.contains("mouse")) {
            return "GarudaHawk.jpg";
        } else if (productName.contains("casio") || productName.contains("watch")) {
            return "Casio Watch.jpg";
        } else if (productName.contains("iphone")) {
            return "Iphone 16.jpg";
        } else if (productName.contains("jacket") || productName.contains("varsity")) {
            return "NU jacket.jpg";
        } else if (productName.contains("airpods") || productName.contains("earpods")) {
            return "Airpods.jpg";
        } else if (productName.contains("crocs")) {
            return "Crocs.jpg";
        } else if (productName.contains("adidas") || productName.contains("samba")) {
            return "Adidas_Samba-removebg-preview.png";
        } else if (productName.contains("bag") || productName.contains("fstopper")) {
            return "FstopperBag.jpg";
        } else if (productName.contains("asus") || productName.contains("monitor")) {
            return "AsusMonitor.jpg";
        } else if (productName.contains("attack shark") || productName.contains("keyboard")) {
            return "Attack Shark Keyboard.png";
        } else if (productName.contains("ssd") || productName.contains("kingston")) {
            return "KingstonSSD.jpg";
        } else if (productName.contains("case") || productName.contains("pc")) {
            return "PcCase.jpg";
        } else if (productName.contains("socks")) {
            return "Socks.jpg";
        } else if (productName.contains("sunglass")) {
            return "Sunglass.jpg";
        } else if (productName.contains("therm") || productName.contains("flask")) {
            return "ThermFlask.jpg";
        }

        // Default fallback
        return "GarudaHawk.jpg";
    }

    /**
     * Set placeholder image when actual image is not available
     */
    private void setPlaceholderImage(ImageView imageView, Product product) {
        // Use a generic product icon or first letter of product name
        String placeholder = product.getName().length() > 0 ?
            product.getName().substring(0, 1).toUpperCase() : "P";

        // You could also use emoji or a default image here
        Label placeholderLabel = new Label("📦 " + placeholder);
        placeholderLabel.getStyleClass().add("image-placeholder");
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
     * Show product details (implement modal dialog later)
     */
    private void showProductDetails(Product product) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Product Details");
        alert.setHeaderText(product.getName());
        alert.setContentText(
            "Description: " + product.getDescription() + "\n" +
            "Price: $" + product.getPrice() + "\n" +
            "Stock: " + product.getStock() + "\n" +
            "Category: " + product.getCategory()
        );
        alert.showAndWait();
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
