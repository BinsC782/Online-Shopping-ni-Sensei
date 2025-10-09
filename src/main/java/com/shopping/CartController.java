package com.shopping;

import com.shopping.model.Cart;
import com.shopping.model.OrderItem;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import java.text.DecimalFormat;

/**
 * Controller for the CartView.fxml. Handles displaying cart contents and checkout logic.
 */
public class CartController {

    // --- FXML Field Declarations (Must match fx:id in CartView.fxml) ---
    @FXML private BorderPane cartMainBorderPane;
    @FXML private VBox emptyCartVBox;
    @FXML private VBox filledCartVBox;
    @FXML private Label subtotalLabel;
    @FXML private Label totalPriceLabel;
    @FXML private Button checkoutButton;

    // --- Model Fields ---
    private Cart cart;
    private MainController mainController;

    // Currency formatter
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("$0.00");

    /**
     * Called by FXMLLoader after fields are injected.
     */
    @FXML
    public void initialize() {
        // Initial setup: Ensure only one container is visible (e.g., empty cart)
        filledCartVBox.setVisible(false);
        emptyCartVBox.setVisible(true);
        emptyCartVBox.setManaged(true);
    }

    /**
     * Called by MainController to pass the Cart object.
     * This is the entry point for data after the FXML is loaded.
     */
    public void setCart(Cart cart) {
        this.cart = cart;
        // CRUCIAL: Load the cart contents immediately after setting the model
        updateCartView();
    }

    /**
     * Called by MainController to pass a reference for refreshing the main view if needed.
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    // --- Core Display Logic ---

    /**
     * Updates the UI based on the current state of the Cart object.
     */
    public void updateCartView() {
        if (this.cart == null || this.cart.getItems().isEmpty()) {
            // Show empty cart state
            emptyCartVBox.setVisible(true);
            emptyCartVBox.setManaged(true);
            filledCartVBox.setVisible(false);
            filledCartVBox.setManaged(false);
            subtotalLabel.setText("Subtotal (0 items):");
            totalPriceLabel.setText(CURRENCY_FORMAT.format(0.00));
        } else {
            // Show filled cart state
            emptyCartVBox.setVisible(false);
            emptyCartVBox.setManaged(false);
            filledCartVBox.setVisible(true);
            filledCartVBox.setManaged(true);

            // Populates the list of items
            populateCartItems();

            // Update totals (formatted for currency)
            subtotalLabel.setText(String.format("Subtotal (%d items):", this.cart.getItems().size()));
            totalPriceLabel.setText(CURRENCY_FORMAT.format(this.cart.getTotal()));
        }
    }

    /**
     * Clears and repopulates the VBox with individual cart item components.
     * This is a placeholder for dynamic item UI generation.
     */
    private void populateCartItems() {
        // Clear old items first
        filledCartVBox.getChildren().clear();

        System.out.println("DEBUG: Cart is ready to be populated with " + this.cart.getItems().size() + " items.");

        // NOTE: The full logic to create complex item HBoxes (with images, names, quantity, buttons)
        // goes here. For now, we will add a simple label for proof of concept.

        for (OrderItem item : this.cart.getItems()) {
            Label itemInfo = new Label(
                String.format("%s (x%d) - %s",
                              item.getName(),
                              item.getQuantity(),
                              CURRENCY_FORMAT.format(item.getPrice() * item.getQuantity())
                )
            );
            itemInfo.setStyle("-fx-font-size: 16px; -fx-padding: 10px; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");
            filledCartVBox.getChildren().add(itemInfo);
        }
    }

    // --- FXML Action Handler Methods ---

    @FXML
    private void handleCloseCart(ActionEvent event) {
        if (mainController != null) {
            mainController.hideCartView();
        }
    }

    @FXML
    private void handleContinueShopping(ActionEvent event) {
        handleCloseCart(event);
    }

    @FXML
    private void handleProceedToCheckout(ActionEvent event) {
        // Implement actual checkout/payment logic here
        System.out.println("Processing Checkout...");
        handleCloseCart(event);
    }
}
