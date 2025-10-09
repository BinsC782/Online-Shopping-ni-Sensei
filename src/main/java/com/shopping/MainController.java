package com.shopping;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.shopping.model.Cart;
import com.shopping.model.Product;
import com.shopping.model.OrderItem;

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
    @FXML private Label cartTotalLabel;
    @FXML private Button checkoutButton;
    @FXML private Button logoutButton;
    @FXML private ToggleButton allCategoriesBtn;
    @FXML private ToggleButton accessoriesBtn;
    @FXML private ToggleButton homeLifestyleBtn;
    @FXML private ToggleButton electronicsBtn;
    @FXML private ToggleButton fashionBtn;
    @FXML private GridPane productsGridPane;

    // 🛒 New embedded cart fields 🛒
    @FXML private AnchorPane cartHostPane;

    // ToggleGroup for category buttons
    private ToggleGroup categoryToggleGroup;

    private Cart shoppingCart;
    private List<Product> allProducts;

    // Embedded cart state management
    private CartController cartController;
    private Parent cartRoot; // Stores the loaded CartView content

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize shopping cart
        shoppingCart = new Cart();

        // Set up event handlers
        setupEventHandlers();

        // Load and display products immediately
        loadProducts();
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
        if (checkoutButton != null) checkoutButton.setOnAction(this::performCheckout);

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
     * Load all products and display them (using hardcoded data for immediate display)
     */
    private void loadProducts() {
        try {
            // Use hardcoded products for immediate display
            allProducts = getHardcodedProducts();
            System.out.println("=== DEBUG: Loaded " + allProducts.size() + " hardcoded products ===");
            for (Product p : allProducts) {
                System.out.println("Product: " + p.getName() + " - ID: " + p.getId() + " - Image: " + p.getImage() + " - Category: " + p.getCategory());
            }
            System.out.println("=== END DEBUG ===");

            displayProducts(allProducts);
        } catch (Exception e) {
            System.err.println("ERROR loading products: " + e.getMessage());
            showError("Failed to load products: " + e.getMessage());
            e.printStackTrace();
            // Initialize empty list as defensive measure
            allProducts = new ArrayList<>();
        }
    }

    /**
     * Get hardcoded product data for immediate display (no file I/O required)
     */
    private List<Product> getHardcodedProducts() {
        List<Product> products = new ArrayList<>();

        // Product 1: Laptop
        Product laptop = new Product("000001", "Laptop", 999.99, 10);
        laptop.setDescription("High-performance laptop with 16GB RAM");
        laptop.setCategory("Electronics");
        laptop.setSellerName("TechStore");
        laptop.setSellerLocation("New York");
        laptop.setRating(4.5);
        laptop.setReviews("Great laptop for work and gaming");
        laptop.setImage("Attack Shark Keyboard.png");
        products.add(laptop);

        // Product 2: Smartphone
        Product smartphone = new Product("000002", "Smartphone", 699.99, 15);
        smartphone.setDescription("Latest smartphone with 128GB storage");
        smartphone.setCategory("Electronics");
        smartphone.setSellerName("MobileHub");
        smartphone.setSellerLocation("California");
        smartphone.setRating(4.7);
        smartphone.setReviews("Excellent camera and battery life");
        smartphone.setImage("Iphone 16.jpg");
        products.add(smartphone);

        // Product 3: Headphones
        Product headphones = new Product("000003", "Headphones", 199.99, 20);
        headphones.setDescription("Wireless noise-cancelling headphones");
        headphones.setCategory("Electronics");
        headphones.setSellerName("AudioWorld");
        headphones.setSellerLocation("Texas");
        headphones.setRating(4.3);
        headphones.setReviews("Comfortable and great sound quality");
        headphones.setImage("Airpods.jpg");
        products.add(headphones);

        // Product 4: Tablet
        Product tablet = new Product("000004", "Tablet", 399.99, 8);
        tablet.setDescription("10-inch tablet with stylus support");
        tablet.setCategory("Electronics");
        tablet.setSellerName("TabletPro");
        tablet.setSellerLocation("Florida");
        tablet.setRating(4.4);
        tablet.setReviews("Perfect for drawing and note-taking");
        tablet.setImage("AsusMonitor.jpg");
        products.add(tablet);

        // Product 5: Smartwatch
        Product smartwatch = new Product("000005", "Smartwatch", 299.99, 12);
        smartwatch.setDescription("Fitness tracking smartwatch");
        smartwatch.setCategory("Electronics");
        smartwatch.setSellerName("WearTech");
        smartwatch.setSellerLocation("Washington");
        smartwatch.setRating(4.6);
        smartwatch.setReviews("Accurate fitness tracking features");
        smartwatch.setImage("Casio Watch.jpg");
        products.add(smartwatch);

        // Product 6: Keyboard
        Product keyboard = new Product("000006", "Mechanical Keyboard", 79.99, 25);
        keyboard.setDescription("Mechanical gaming keyboard");
        keyboard.setCategory("Accessories");
        keyboard.setSellerName("GameGear");
        keyboard.setSellerLocation("Nevada");
        keyboard.setRating(4.2);
        keyboard.setReviews("Reliable for gaming sessions");
        keyboard.setImage("Attack Shark Keyboard.png");
        products.add(keyboard);

        // Product 7: Mouse
        Product mouse = new Product("000007", "Wireless Mouse", 49.99, 30);
        mouse.setDescription("Wireless ergonomic mouse");
        mouse.setCategory("Accessories");
        mouse.setSellerName("PeriTech");
        mouse.setSellerLocation("Arizona");
        mouse.setRating(4.1);
        mouse.setReviews("Comfortable for long use");
        mouse.setImage("GarudaHawk.jpg");
        products.add(mouse);

        // Product 8: Monitor
        Product monitor = new Product("000008", "4K Monitor", 249.99, 5);
        monitor.setDescription("27-inch 4K monitor");
        monitor.setCategory("Electronics");
        monitor.setSellerName("DisplayCorp");
        monitor.setSellerLocation("Georgia");
        monitor.setRating(4.8);
        monitor.setReviews("Stunning visuals and color accuracy");
        monitor.setImage("AsusMonitor.jpg");
        products.add(monitor);

        // Product 9: Printer
        Product printer = new Product("000009", "Wireless Printer", 149.99, 7);
        printer.setDescription("All-in-one wireless printer");
        printer.setCategory("Electronics");
        printer.setSellerName("PrintMaster");
        printer.setSellerLocation("Illinois");
        printer.setRating(4.0);
        printer.setReviews("Good for home office use");
        printer.setImage("PcCase.jpg");
        products.add(printer);

        // Product 11: Gaming Headset
        Product headset = new Product("000011", "Gaming Headset", 89.99, 18);
        headset.setDescription("Professional gaming headset with surround sound");
        headset.setCategory("Electronics");
        headset.setSellerName("GameAudio");
        headset.setSellerLocation("Oregon");
        headset.setRating(4.4);
        headset.setReviews("Excellent sound quality for gaming");
        headset.setImage("Airpods.jpg");
        products.add(headset);

        // Product 12: External SSD
        Product ssd = new Product("000012", "External SSD 1TB", 129.99, 14);
        ssd.setDescription("Portable 1TB external SSD drive");
        ssd.setCategory("Electronics");
        ssd.setSellerName("StoragePro");
        ssd.setSellerLocation("Utah");
        ssd.setRating(4.6);
        ssd.setReviews("Fast transfer speeds and reliable storage");
        ssd.setImage("KingstonSSD.jpg");
        products.add(ssd);

        // Product 13: Camera Bag
        Product cameraBag = new Product("000013", "Professional Camera Bag", 79.99, 22);
        cameraBag.setDescription("Waterproof camera bag with multiple compartments");
        cameraBag.setCategory("Accessories");
        cameraBag.setSellerName("PhotoGear");
        cameraBag.setSellerLocation("Colorado");
        cameraBag.setRating(4.3);
        cameraBag.setReviews("Perfect for photographers on the go");
        cameraBag.setImage("FstopperBag.jpg");
        products.add(cameraBag);

        // Product 14: Sunglasses
        Product sunglasses = new Product("000014", "Polarized Sunglasses", 59.99, 35);
        sunglasses.setDescription("UV400 polarized sunglasses for outdoor activities");
        sunglasses.setCategory("Fashion");
        sunglasses.setSellerName("StyleVision");
        sunglasses.setSellerLocation("Arizona");
        sunglasses.setRating(4.2);
        sunglasses.setReviews("Great for driving and outdoor sports");
        sunglasses.setImage("Sunglass.jpg");
        products.add(sunglasses);

        // Product 15: Casual Shoes
        Product casualShoes = new Product("000015", "Casual Canvas Shoes", 49.99, 40);
        casualShoes.setDescription("Comfortable canvas shoes for everyday wear");
        casualShoes.setCategory("Fashion");
        casualShoes.setSellerName("FootwearCo");
        casualShoes.setSellerLocation("Nevada");
        casualShoes.setRating(4.1);
        casualShoes.setReviews("Lightweight and comfortable for walking");
        casualShoes.setImage("Crocs.jpg");
        products.add(casualShoes);

        // Product 16: Hoodie
        Product hoodie = new Product("000016", "Cotton Hoodie", 39.99, 28);
        hoodie.setDescription("Soft cotton hoodie with front pocket");
        hoodie.setCategory("Fashion");
        hoodie.setSellerName("CasualWear");
        hoodie.setSellerLocation("Washington");
        hoodie.setRating(4.3);
        hoodie.setReviews("Warm and comfortable for casual wear");
        hoodie.setImage("NU jacket.jpg");
        products.add(hoodie);

        // Product 17: Cotton Socks
        Product cottonSocks = new Product("000017", "Cotton Athletic Socks", 14.99, 50);
        cottonSocks.setDescription("Pack of 6 cotton athletic socks");
        cottonSocks.setCategory("Fashion");
        cottonSocks.setSellerName("ComfortWear");
        cottonSocks.setSellerLocation("California");
        cottonSocks.setRating(4.0);
        cottonSocks.setReviews("Comfortable and durable for daily use");
        cottonSocks.setImage("Socks.jpg");
        products.add(cottonSocks);

        // Product 18: Coffee Maker
        Product coffeeMaker = new Product("000018", "Programmable Coffee Maker", 89.99, 12);
        coffeeMaker.setDescription("12-cup programmable coffee maker with timer");
        coffeeMaker.setCategory("Home & Lifestyle");
        coffeeMaker.setSellerName("KitchenEssentials");
        coffeeMaker.setSellerLocation("Texas");
        coffeeMaker.setRating(4.4);
        coffeeMaker.setReviews("Makes great coffee with easy programming");
        coffeeMaker.setImage("ThermFlask.jpg");
        products.add(coffeeMaker);

        // Product 19: Bluetooth Speaker
        Product bluetoothSpeaker = new Product("000019", "Portable Bluetooth Speaker", 69.99, 20);
        bluetoothSpeaker.setDescription("Waterproof portable Bluetooth speaker");
        bluetoothSpeaker.setCategory("Electronics");
        bluetoothSpeaker.setSellerName("AudioTech");
        bluetoothSpeaker.setSellerLocation("Florida");
        bluetoothSpeaker.setRating(4.5);
        bluetoothSpeaker.setReviews("Great sound quality and battery life");
        bluetoothSpeaker.setImage("Airpods.jpg");
        products.add(bluetoothSpeaker);

        // Product 20: Yoga Mat
        Product yogaMat = new Product("000020", "Premium Yoga Mat", 34.99, 25);
        yogaMat.setDescription("Non-slip premium yoga mat 6mm thick");
        yogaMat.setCategory("Sports & Outdoors");
        yogaMat.setSellerName("FitnessGear");
        yogaMat.setSellerLocation("Georgia");
        yogaMat.setRating(4.2);
        yogaMat.setReviews("Perfect grip and comfortable for yoga practice");
        yogaMat.setImage("GarudaHawk.jpg");
        products.add(yogaMat);

        // Product 21: Water Bottle
        Product waterBottle = new Product("000021", "Insulated Water Bottle", 24.99, 30);
        waterBottle.setDescription("Stainless steel insulated water bottle 32oz");
        waterBottle.setCategory("Sports & Outdoors");
        waterBottle.setSellerName("HydrationPro");
        waterBottle.setSellerLocation("Illinois");
        waterBottle.setRating(4.3);
        waterBottle.setReviews("Keeps drinks cold for 24 hours");
        waterBottle.setImage("ThermFlask.jpg");
        products.add(waterBottle);

        // Product 22: Desk Lamp
        Product deskLamp = new Product("000022", "LED Desk Lamp", 45.99, 15);
        deskLamp.setDescription("Adjustable LED desk lamp with wireless charging");
        deskLamp.setCategory("Home & Lifestyle");
        deskLamp.setSellerName("HomeLighting");
        deskLamp.setSellerLocation("Michigan");
        deskLamp.setRating(4.1);
        deskLamp.setReviews("Bright LED light with phone charging base");
        deskLamp.setImage("AsusMonitor.jpg");
        products.add(deskLamp);

        // Product 23: Backpack
        Product backpack = new Product("000023", "Laptop Backpack", 79.99, 18);
        backpack.setDescription("Waterproof laptop backpack with USB charging port");
        backpack.setCategory("Accessories");
        backpack.setSellerName("TravelGear");
        backpack.setSellerLocation("New York");
        backpack.setRating(4.4);
        backpack.setReviews("Comfortable and spacious for daily commute");
        backpack.setImage("FstopperBag.jpg");
        products.add(backpack);

        // Product 24: Wireless Charger
        Product wirelessCharger = new Product("000024", "Fast Wireless Charger", 29.99, 35);
        wirelessCharger.setDescription("Fast wireless charging pad for smartphones");
        wirelessCharger.setCategory("Electronics");
        wirelessCharger.setSellerName("ChargeTech");
        wirelessCharger.setSellerLocation("Pennsylvania");
        wirelessCharger.setRating(4.0);
        wirelessCharger.setReviews("Quick charging with safety features");
        wirelessCharger.setImage("KingstonSSD.jpg");
        products.add(wirelessCharger);

        // Product 25: Fitness Tracker
        Product fitnessTracker = new Product("000025", "Fitness Activity Tracker", 99.99, 22);
        fitnessTracker.setDescription("Advanced fitness tracker with heart rate monitor");
        fitnessTracker.setCategory("Electronics");
        fitnessTracker.setSellerName("HealthTech");
        fitnessTracker.setSellerLocation("Ohio");
        fitnessTracker.setRating(4.5);
        fitnessTracker.setReviews("Accurate tracking for all activities");
        fitnessTracker.setImage("Casio Watch.jpg");
        products.add(fitnessTracker);

        return products;
    }

    /**
     * Display products in the UI using GridPane with 3-column layout
     */
    private void displayProducts(List<Product> products) {
        productsGridPane.getChildren().clear();
        System.out.println("Displaying " + products.size() + " products in GridPane");

        int columns = 3; // 3 columns as requested
        // Note: rows calculation not needed since GridPane handles layout automatically

        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            try {
                VBox productCard = createProductCard(product);

                // Calculate row and column position
                int row = i / columns;
                int col = i % columns;

                // Add to GridPane at calculated position
                productsGridPane.add(productCard, col, row);
                System.out.println("Added product card: " + product.getName() + " at position (" + row + ", " + col + ")");
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
        card.setPrefWidth(300); // Increased from 220 to accommodate buttons
        card.setMinWidth(280);  // Set minimum width to ensure buttons fit

        // Make the entire card clickable - ADD VISUAL FEEDBACK
        card.setOnMouseClicked(e -> {
            card.setStyle("-fx-background-color: #ffcc00;"); // yellow background
            System.out.println("=== CLICKED PRODUCT CARD: " + product.getName() + " ===");
            showProductModal(product);
        });

        // Also add click to image for testing
        ImageView productImage = new ImageView();
        productImage.getStyleClass().add("product-image");
        productImage.setFitWidth(260); // Increased proportionally from 180
        productImage.setFitHeight(200); // Increased proportionally from 150
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
        VBox productInfo = new VBox(8); // Increased spacing slightly
        productInfo.getStyleClass().add("product-info");
        productInfo.setAlignment(Pos.CENTER_LEFT);
        productInfo.setPrefWidth(280); // Match card width

        // Product name - equivalent to h3
        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("product-name");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(260); // Adjusted to match new image width

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
        HBox buttonBox = new HBox(15); // Increased spacing from 10 to 15
        buttonBox.getStyleClass().add("product-buttons");
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPrefWidth(280); // Match card width
        buttonBox.setMinWidth(280);  // Ensure minimum width

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

        // Ensure buttons maintain their preferred size
        addToCartButton.setPrefWidth(120);
        viewDetailsButton.setPrefWidth(120);

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
        javafx.scene.shape.Rectangle bg = new javafx.scene.shape.Rectangle(260, 200); // Updated to match new image dimensions
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
        text.setTranslateX(130 - text.getLayoutBounds().getWidth()/2); // Updated for new width (260/2 = 130)
        text.setTranslateY(100 + text.getLayoutBounds().getHeight()/4); // Updated for new height (200/2 = 100)

        // Create snapshot as image
        javafx.scene.image.WritableImage image = new javafx.scene.image.WritableImage(260, 200); // Updated dimensions
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
            List<Product> searchResults = allProducts.stream()
                .filter(product -> product.getName().toLowerCase().contains(searchTerm.toLowerCase()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            displayProducts(searchResults);
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
        modalImage.setFitWidth(260); // Updated to match new card image dimensions
        modalImage.setFitHeight(200); // Updated to match new card image dimensions
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
     * Handle checkout button click - now shows embedded cart panel
     */
    @FXML
    private void performCheckout(ActionEvent event) {
        if (cartRoot == null) {
            // Load FXML content and controller once
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CartView.fxml"));
                cartRoot = loader.load();
                cartController = loader.getController();

                // Inject the cart model and a reference to the main controller
                cartController.setCart(this.shoppingCart);
                cartController.setMainController(this); // Pass reference for control flow

                // Embed the cart content into the AnchorPane
                cartHostPane.getChildren().add(cartRoot);
                AnchorPane.setTopAnchor(cartRoot, 0.0);
                AnchorPane.setBottomAnchor(cartRoot, 0.0);
                AnchorPane.setLeftAnchor(cartRoot, 0.0);
                AnchorPane.setRightAnchor(cartRoot, 0.0);

            } catch (IOException e) {
                e.printStackTrace();
                // CRITICAL: Stop execution but avoid app crash
                System.err.println("FATAL: Embedded CartView FXML failed to load. Check file path/structure.");
                return;
            }
        }

        // Toggle the cart visibility and manage FXML properties
        boolean isVisible = !cartHostPane.isVisible();
        cartHostPane.setVisible(isVisible);
        cartHostPane.setManaged(isVisible); // Managed = takes up space in layout

        // If the cart is being shown, force a refresh of the contents
        if (isVisible && cartController != null) {
            cartController.updateCartView();
        }
    }

    /**
     * Hide the embedded cart panel (called by CartController)
     */
    public void hideCartView() {
        cartHostPane.setVisible(false);
        cartHostPane.setManaged(false);
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
