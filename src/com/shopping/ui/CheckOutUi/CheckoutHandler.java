package com.shopping.ui.CheckOutUi;

import com.shopping.model.Product;
import com.shopping.ui.CartUi.CartPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import javax.swing.*;

public class CheckoutHandler implements ActionListener {
    private CartPanel cartPanel;
    private Map<Product, Integer> cartItems;
    private String username;

    public CheckoutHandler(CartPanel cartPanel, Map<Product, Integer> cartItems, String username) {
        this.cartPanel = cartPanel;
        this.cartItems = cartItems;
        this.username = username;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(cartPanel, "Your cart is empty!");
            return;
        }

        // Collect user information
        JTextField addressField = new JTextField();
        JTextField phoneField = new JTextField();

        Object[] message = {
            "Delivery Address:", addressField,
            "Phone Number:", phoneField
        };

        int option = JOptionPane.showConfirmDialog(cartPanel, message, "Checkout", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String address = addressField.getText().trim();
            String phone = phoneField.getText().trim();

            if (address.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(cartPanel, "Please fill in all fields.");
                return;
            }

            // Generate order ID
            String orderId = "ORD" + System.currentTimeMillis();

            // Prepare product list
            List<String> productList = new java.util.ArrayList<>();
            for (Map.Entry<Product, Integer> entry : cartItems.entrySet()) {
                for (int i = 0; i < entry.getValue(); i++) {
                    productList.add(entry.getKey().getName());
                }
            }

            // Save order
            com.shopping.data.FileHandler.saveOrder(orderId, username + " (" + phone + ")", productList, "Pending");

            // Clear cart
            cartItems.clear();
            cartPanel.updateDisplay();

            JOptionPane.showMessageDialog(cartPanel,
                "Order placed successfully!\nOrder ID: " + orderId + "\nDelivery to: " + address);
        }
    }
}