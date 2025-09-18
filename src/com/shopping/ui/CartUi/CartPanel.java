package com.shopping.ui.CartUi;

import com.shopping.model.Product;
import com.shopping.ui.CheckOutUi.CheckoutHandler;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

public class CartPanel extends JPanel {
    private Map<Product, Integer> cartItems;
    private JPanel cartItemsPanel;
    private JLabel totalLabel;
    private JButton checkoutButton;
    private String username;

    public CartPanel(String username) {
        this.username = username;
        cartItems = new HashMap<>();
        setLayout(new BorderLayout());

        // Set orange theme
        setBackground(new Color(255, 165, 0));

        // Cart items panel
        cartItemsPanel = new JPanel();
        cartItemsPanel.setLayout(new BoxLayout(cartItemsPanel, BoxLayout.Y_AXIS));
        cartItemsPanel.setBackground(new Color(255, 165, 0));
        JScrollPane scrollPane = new JScrollPane(cartItemsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Total and buttons
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(255, 165, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        totalLabel = new JLabel("Total: $0.00");
        totalLabel.setHorizontalAlignment(SwingConstants.CENTER);
        totalLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalLabel.setForeground(Color.WHITE);
        totalLabel.setOpaque(true);
        totalLabel.setBackground(new Color(255, 140, 0));
        totalLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(new Color(255, 165, 0));

        JButton viewCartButton = createStyledButton("View Full Cart", new Color(70, 130, 180));
        viewCartButton.addActionListener(e -> openFullCartView());

        checkoutButton = createStyledButton("Quick Checkout", new Color(34, 139, 34));
        checkoutButton.addActionListener(new CheckoutHandler(this, cartItems, username));

        buttonPanel.add(viewCartButton);
        buttonPanel.add(checkoutButton);

        bottomPanel.add(totalLabel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        updateDisplay();
    }

    public void addProduct(Product product) {
        cartItems.put(product, cartItems.getOrDefault(product, 0) + 1);
        updateDisplay();
    }

    public void removeProduct(Product product) {
        if (cartItems.containsKey(product)) {
            int quantity = cartItems.get(product);
            if (quantity > 1) {
                cartItems.put(product, quantity - 1);
            } else {
                cartItems.remove(product);
            }
            updateDisplay();
        }
    }

    public void updateDisplay() {
        cartItemsPanel.removeAll();

        for (Map.Entry<Product, Integer> entry : cartItems.entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();

            JPanel itemPanel = new JPanel(new BorderLayout());
            itemPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 140, 0), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
            ));
            itemPanel.setBackground(Color.WHITE);

            // Product info
            JPanel infoPanel = new JPanel(new GridLayout(2, 1));
            infoPanel.setBackground(Color.WHITE);
            infoPanel.add(new JLabel("<html><b>" + product.getName() + "</b></html>"));
            infoPanel.add(new JLabel("$" + String.format("%.2f", product.getPrice()) + " x " + quantity));

            // Remove button
            JButton removeButton = createStyledButton("Remove", new Color(220, 20, 60));
            removeButton.addActionListener(e -> removeProduct(product));

            itemPanel.add(infoPanel, BorderLayout.CENTER);
            itemPanel.add(removeButton, BorderLayout.EAST);

            cartItemsPanel.add(itemPanel);
            cartItemsPanel.add(Box.createVerticalStrut(5));
        }

        updateTotal();
        cartItemsPanel.revalidate();
        cartItemsPanel.repaint();
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        button.setFocusPainted(false);
        return button;
    }

    private void openFullCartView() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Your cart is empty!");
            return;
        }

        CartViewDialog cartDialog = new CartViewDialog((JFrame) SwingUtilities.getWindowAncestor(this), cartItems, username);
        cartDialog.setVisible(true);

        // Update cart with any changes from the dialog
        Map<Product, Integer> updatedCart = cartDialog.getUpdatedCartItems();
        cartItems.clear();
        cartItems.putAll(updatedCart);
        updateDisplay();
    }

    private void updateTotal() {
        double total = 0.0;
        for (Map.Entry<Product, Integer> entry : cartItems.entrySet()) {
            total += entry.getKey().getPrice() * entry.getValue();
        }
        totalLabel.setText("Total: $" + String.format("%.2f", total));
    }


    public Map<Product, Integer> getCartItems() {
        return new HashMap<>(cartItems);
    }
}