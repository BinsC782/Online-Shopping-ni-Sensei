package com.shopping.ui.ProductUi;

import com.shopping.model.Product;
import java.awt.*;
import javax.swing.*;

public class ProductDetailsDialog extends JDialog {
    public ProductDetailsDialog(JFrame parent, Product product) {
        super(parent, "Product Details", true);
        initializeComponents(product);
        setSize(600, 700);
        setLocationRelativeTo(parent);
    }

    private void initializeComponents(Product product) {
        setLayout(new BorderLayout());

        JPanel detailsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Product Image placeholder
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JPanel imagePanel = new JPanel();
        imagePanel.setBackground(Color.LIGHT_GRAY);
        imagePanel.setPreferredSize(new Dimension(200, 150));
        JLabel imageLabel = new JLabel("Product Image", SwingConstants.CENTER);
        imagePanel.add(imageLabel);
        detailsPanel.add(imagePanel, gbc);

        // Name
        gbc.gridwidth = 1; gbc.gridx = 0; gbc.gridy = 1;
        detailsPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        detailsPanel.add(new JLabel(product.getName()), gbc);

        // Category
        gbc.gridx = 0; gbc.gridy = 2;
        detailsPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        detailsPanel.add(new JLabel(product.getCategory()), gbc);

        // Price
        gbc.gridx = 0; gbc.gridy = 3;
        detailsPanel.add(new JLabel("Price:"), gbc);
        gbc.gridx = 1;
        detailsPanel.add(new JLabel("$" + String.format("%.2f", product.getPrice())), gbc);

        // Stock
        gbc.gridx = 0; gbc.gridy = 4;
        detailsPanel.add(new JLabel("Stock:"), gbc);
        gbc.gridx = 1;
        detailsPanel.add(new JLabel(String.valueOf(product.getStock())), gbc);

        // Seller Name
        gbc.gridx = 0; gbc.gridy = 5;
        detailsPanel.add(new JLabel("Seller Name:"), gbc);
        gbc.gridx = 1;
        detailsPanel.add(new JLabel(product.getSellerName()), gbc);

        // Seller Location
        gbc.gridx = 0; gbc.gridy = 6;
        detailsPanel.add(new JLabel("Seller Location:"), gbc);
        gbc.gridx = 1;
        detailsPanel.add(new JLabel(product.getSellerLocation()), gbc);

        // Rating
        gbc.gridx = 0; gbc.gridy = 7;
        detailsPanel.add(new JLabel("Rating:"), gbc);
        gbc.gridx = 1;
        JLabel ratingLabel = new JLabel(String.format("%.1f ★", product.getRating()));
        ratingLabel.setFont(new Font("Arial", Font.BOLD, 16));
        ratingLabel.setForeground(Color.ORANGE);
        detailsPanel.add(ratingLabel, gbc);

        // Reviews and Description combined
        gbc.gridx = 0; gbc.gridy = 8;
        detailsPanel.add(new JLabel("Summary:"), gbc);
        gbc.gridx = 1;
        String combinedText = "This product is " + product.getDescription().toLowerCase() + ", and customers say " + product.getReviews().toLowerCase() + ".";
        JTextArea summaryArea = new JTextArea(combinedText, 5, 25);
        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane summaryScroll = new JScrollPane(summaryArea);
        summaryScroll.setPreferredSize(new Dimension(300, 120));
        detailsPanel.add(summaryScroll, gbc);


        add(detailsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
}