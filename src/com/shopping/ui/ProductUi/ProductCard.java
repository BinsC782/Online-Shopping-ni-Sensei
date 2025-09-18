package com.shopping.ui.ProductUi;

import com.shopping.model.Product;
import com.shopping.ui.CartUi.CartPanel;
import com.shopping.ui.ProductUi.ProductDetailsDialog;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

public class ProductCard extends JPanel {
    private Product product;
    private CartPanel cartPanel;

    public ProductCard(Product product, CartPanel cartPanel) {
        this.product = product;
        this.cartPanel = cartPanel;
        initializeCard();
    }

    private void initializeCard() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createRaisedBevelBorder());
        setPreferredSize(new Dimension(200, 300));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Image placeholder
        JPanel imagePanel = new JPanel();
        imagePanel.setBackground(Color.LIGHT_GRAY);
        imagePanel.setPreferredSize(new Dimension(180, 120));
        JLabel imageLabel = new JLabel("Product Image", SwingConstants.CENTER);
        imagePanel.add(imageLabel);
        add(imagePanel, BorderLayout.NORTH);

        // Product details
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel nameLabel = new JLabel("<html><b>" + product.getName() + "</b></html>");
        JLabel categoryLabel = new JLabel("<html><i>" + product.getCategory() + "</i></html>");
        categoryLabel.setForeground(Color.BLUE);
        JLabel priceLabel = new JLabel("$" + String.format("%.2f", product.getPrice()));
        priceLabel.setForeground(Color.RED);
        JLabel stockLabel = new JLabel("Stock: " + product.getStock());
        stockLabel.setForeground(product.getStock() > 0 ? Color.GREEN : Color.RED);
        JLabel sellerLabel = new JLabel("Seller: " + product.getSellerName() + " (" + product.getSellerLocation() + ")");
        sellerLabel.setForeground(Color.DARK_GRAY);
        JLabel ratingLabel = new JLabel("Rating: " + String.format("%.1f", product.getRating()) + " ★");
        ratingLabel.setForeground(Color.ORANGE);
        JLabel reviewsLabel = new JLabel("<html><small>Reviews: " + product.getReviews() + "</small></html>");
        reviewsLabel.setForeground(Color.GRAY);
        JTextArea descArea = new JTextArea(product.getDescription());
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setFont(new Font("Arial", Font.PLAIN, 10));
        descArea.setBackground(detailsPanel.getBackground());
        descArea.setBorder(BorderFactory.createEmptyBorder());

        detailsPanel.add(nameLabel);
        detailsPanel.add(categoryLabel);
        detailsPanel.add(priceLabel);
        detailsPanel.add(stockLabel);
        detailsPanel.add(sellerLabel);
        detailsPanel.add(ratingLabel);
        detailsPanel.add(reviewsLabel);
        detailsPanel.add(Box.createVerticalStrut(5));
        detailsPanel.add(descArea);
        detailsPanel.add(Box.createVerticalGlue());

        // Quantity selector
        JPanel quantityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        quantityPanel.setBackground(Color.WHITE);
        JLabel qtyLabel = new JLabel("Qty:");
        JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, product.getStock(), 1));
        quantityPanel.add(qtyLabel);
        quantityPanel.add(quantitySpinner);
        detailsPanel.add(quantityPanel);

        // Add to cart button
        JButton addButton = new JButton("Add to Cart");
        addButton.addActionListener(e -> {
            int quantity = (Integer) quantitySpinner.getValue();
            for (int i = 0; i < quantity; i++) {
                cartPanel.addProduct(product);
            }
            JOptionPane.showMessageDialog(this, quantity + " x " + product.getName() + " added to cart!");
        });
        detailsPanel.add(addButton);

        add(detailsPanel, BorderLayout.CENTER);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new ProductDetailsDialog((JFrame) SwingUtilities.getWindowAncestor(ProductCard.this), product).setVisible(true);
            }
        });
    }
}