package com.shopping.ui.CheckOutUi;

import com.shopping.model.Product;
import com.shopping.data.FileHandler;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

public class CheckoutDialog extends JDialog {
    private Map<Product, Integer> cartItems;
    private Map<Product, Integer> orderQuantities;
    private String username;
    private boolean orderPlaced = false;

    public CheckoutDialog(Frame parent, Map<Product, Integer> cartItems, String username) {
        super(parent, "Checkout", true);
        this.cartItems = new HashMap<>(cartItems);
        this.username = username;
        this.orderQuantities = new HashMap<>(cartItems);

        setSize(600, 500);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(new Color(255, 165, 0)); // Orange background

        initializeComponents();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());

        // Title
        JLabel titleLabel = new JLabel("Checkout", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setOpaque(true);
        titleLabel.setBackground(new Color(255, 140, 0));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        add(titleLabel, BorderLayout.NORTH);

        // Order details panel
        JPanel orderPanel = new JPanel();
        orderPanel.setLayout(new BoxLayout(orderPanel, BoxLayout.Y_AXIS));
        orderPanel.setBackground(new Color(255, 165, 0));
        orderPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Add order items with quantity controls
        for (Map.Entry<Product, Integer> entry : cartItems.entrySet()) {
            Product product = entry.getKey();
            int currentQuantity = entry.getValue();

            JPanel itemPanel = createOrderItemPanel(product, currentQuantity);
            orderPanel.add(itemPanel);
            orderPanel.add(Box.createVerticalStrut(10));
        }

        JScrollPane scrollPane = new JScrollPane(orderPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel with customer info and buttons
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(255, 165, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Customer info panel
        JPanel infoPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        infoPanel.setBackground(new Color(255, 165, 0));

        infoPanel.add(new JLabel("Delivery Address:"));
        JTextField addressField = new JTextField();
        infoPanel.add(addressField);

        infoPanel.add(new JLabel("Phone Number:"));
        JTextField phoneField = new JTextField();
        infoPanel.add(phoneField);

        bottomPanel.add(infoPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(new Color(255, 165, 0));

        JButton placeOrderButton = createStyledButton("Place Order", new Color(34, 139, 34));
        JButton cancelButton = createStyledButton("Cancel", new Color(220, 20, 60));

        placeOrderButton.addActionListener(new PlaceOrderAction(addressField, phoneField));
        cancelButton.addActionListener(e -> setVisible(false));

        buttonPanel.add(placeOrderButton);
        buttonPanel.add(cancelButton);

        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createOrderItemPanel(Product product, int currentQuantity) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 140, 0), 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Product info
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.add(new JLabel("<html><b>" + product.getName() + "</b></html>"));
        infoPanel.add(new JLabel("$" + String.format("%.2f", product.getPrice()) + " each"));

        // Quantity control
        JPanel quantityPanel = new JPanel(new FlowLayout());
        quantityPanel.setBackground(Color.WHITE);

        JButton minusButton = new JButton("-");
        minusButton.setBackground(new Color(255, 140, 0));
        minusButton.setForeground(Color.WHITE);

        JTextField quantityField = new JTextField(String.valueOf(currentQuantity), 3);
        quantityField.setHorizontalAlignment(JTextField.CENTER);

        JButton plusButton = new JButton("+");
        plusButton.setBackground(new Color(255, 140, 0));
        plusButton.setForeground(Color.WHITE);

        minusButton.addActionListener(e -> {
            int qty = Integer.parseInt(quantityField.getText());
            if (qty > 1) {
                qty--;
                quantityField.setText(String.valueOf(qty));
                orderQuantities.put(product, qty);
            }
        });

        plusButton.addActionListener(e -> {
            int qty = Integer.parseInt(quantityField.getText());
            if (qty < product.getStock()) {
                qty++;
                quantityField.setText(String.valueOf(qty));
                orderQuantities.put(product, qty);
            } else {
                JOptionPane.showMessageDialog(this, "Not enough stock available!");
            }
        });

        quantityPanel.add(new JLabel("Quantity:"));
        quantityPanel.add(minusButton);
        quantityPanel.add(quantityField);
        quantityPanel.add(plusButton);

        // Subtotal
        JLabel subtotalLabel = new JLabel("$" + String.format("%.2f", product.getPrice() * currentQuantity));
        subtotalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        subtotalLabel.setFont(new Font("Arial", Font.BOLD, 14));

        panel.add(infoPanel, BorderLayout.WEST);
        panel.add(quantityPanel, BorderLayout.CENTER);
        panel.add(subtotalLabel, BorderLayout.EAST);

        return panel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);
        return button;
    }

    private class PlaceOrderAction implements ActionListener {
        private JTextField addressField;
        private JTextField phoneField;

        public PlaceOrderAction(JTextField addressField, JTextField phoneField) {
            this.addressField = addressField;
            this.phoneField = phoneField;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String address = addressField.getText().trim();
            String phone = phoneField.getText().trim();

            if (address.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(CheckoutDialog.this, "Please fill in all fields.");
                return;
            }

            // Validate stock availability
            for (Map.Entry<Product, Integer> entry : orderQuantities.entrySet()) {
                Product product = entry.getKey();
                int requestedQuantity = entry.getValue();

                if (requestedQuantity > product.getStock()) {
                    JOptionPane.showMessageDialog(CheckoutDialog.this,
                        "Not enough stock for " + product.getName() + ". Available: " + product.getStock());
                    return;
                }
            }

            // Generate order ID
            String orderId = "ORD" + System.currentTimeMillis();

            // Prepare product list
            java.util.List<String> productList = new java.util.ArrayList<>();
            for (java.util.Map.Entry<Product, Integer> entry : orderQuantities.entrySet()) {
                for (int i = 0; i < entry.getValue(); i++) {
                    productList.add(entry.getKey().getName());
                }
            }

            // Save order
            FileHandler.saveOrder(orderId, username + " (" + phone + ")", productList, "Pending");

            // Update stock
            List<Product> allProducts = FileHandler.loadProductsAsObjects();
            for (Map.Entry<Product, Integer> entry : orderQuantities.entrySet()) {
                Product orderedProduct = entry.getKey();
                int orderedQuantity = entry.getValue();

                // Find and update the product in the list
                for (Product product : allProducts) {
                    if (product.getId() == orderedProduct.getId()) {
                        product.setStock(product.getStock() - orderedQuantity);
                        break;
                    }
                }
            }
            FileHandler.saveProductsAsObjects(allProducts);

            orderPlaced = true;
            JOptionPane.showMessageDialog(CheckoutDialog.this,
                "Order placed successfully!\nOrder ID: " + orderId + "\nDelivery to: " + address);
            setVisible(false);
        }
    }

    public boolean isOrderPlaced() {
        return orderPlaced;
    }

    public Map<Product, Integer> getOrderQuantities() {
        return new HashMap<>(orderQuantities);
    }
}