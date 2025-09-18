package com.shopping.ui.ProductUi;

import com.shopping.model.Product;
import com.shopping.ui.CartUi.CartPanel;
import com.shopping.ui.ProductUi.ProductCard;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;

public class ProductPanel extends JPanel {
    private List<Product> allProducts;
    private List<Product> filteredProducts;
    private JPanel productGridPanel;
    private JTextField searchField;
    private JPanel categoryPanel;
    private String selectedCategory;
    private CartPanel cartPanel;

    public ProductPanel(List<Product> products, CartPanel cartPanel) {
        this.allProducts = products;
        this.filteredProducts = products;
        this.cartPanel = cartPanel;
        setLayout(new BorderLayout());

        // Set orange theme
        setBackground(new Color(255, 165, 0));

        initializeComponents();
    }

    private void initializeComponents() {
        // Top panel with search and categories
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(255, 165, 0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout());
        searchPanel.setBackground(new Color(255, 165, 0));

        JLabel searchLabel = new JLabel("Search Products: ");
        searchLabel.setFont(new Font("Arial", Font.BOLD, 14));
        searchLabel.setForeground(Color.WHITE);

        searchField = new JTextField(25);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));

        JButton searchButton = createStyledButton("Search", new Color(34, 139, 34));
        searchButton.addActionListener(e -> filterProducts());

        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        topPanel.add(searchPanel, BorderLayout.NORTH);

        // Category panel
        categoryPanel = new JPanel(new FlowLayout());
        categoryPanel.setBackground(new Color(255, 165, 0));

        JButton allButton = createStyledButton("All Categories", new Color(70, 130, 180));
        allButton.addActionListener(e -> filterByCategory(null));
        categoryPanel.add(allButton);

        // Get unique categories
        java.util.Set<String> categories = new java.util.HashSet<>();
        for (Product p : allProducts) {
            categories.add(p.getCategory());
        }
        for (String category : categories) {
            JButton catButton = createStyledButton(category, new Color(255, 140, 0));
            catButton.addActionListener(e -> filterByCategory(category));
            categoryPanel.add(catButton);
        }
        topPanel.add(categoryPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        // Product grid
        productGridPanel = new JPanel(new GridLayout(0, 3, 10, 10)); // 3 columns
        JScrollPane scrollPane = new JScrollPane(productGridPanel);
        add(scrollPane, BorderLayout.CENTER);

        displayProducts();
    }

    private void displayProducts() {
        productGridPanel.removeAll();
        for (Product product : filteredProducts) {
            productGridPanel.add(new ProductCard(product, cartPanel));
        }
        productGridPanel.revalidate();
        productGridPanel.repaint();
    }


    private void filterProducts() {
        String query = searchField.getText().toLowerCase();
        java.util.stream.Stream<Product> stream = allProducts.stream();

        if (!query.isEmpty()) {
            stream = stream.filter(p -> p.getName().toLowerCase().contains(query) ||
                                       p.getDescription().toLowerCase().contains(query));
        }

        if (selectedCategory != null) {
            stream = stream.filter(p -> p.getCategory().equals(selectedCategory));
        }

        filteredProducts = stream.collect(Collectors.toList());
        displayProducts();
    }

    private void filterByCategory(String category) {
        selectedCategory = category;
        filterProducts();
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setFocusPainted(false);
        return button;
    }

    public void refreshProducts(List<Product> newProducts) {
        this.allProducts = newProducts;
        this.filteredProducts = newProducts;
        displayProducts();
        // Refresh category buttons
        categoryPanel.removeAll();
        JButton allButton = createStyledButton("All Categories", new Color(70, 130, 180));
        allButton.addActionListener(e -> filterByCategory(null));
        categoryPanel.add(allButton);

        java.util.Set<String> categories = new java.util.HashSet<>();
        for (Product p : allProducts) {
            categories.add(p.getCategory());
        }
        for (String category : categories) {
            JButton catButton = createStyledButton(category, new Color(255, 140, 0));
            catButton.addActionListener(e -> filterByCategory(category));
            categoryPanel.add(catButton);
        }
        categoryPanel.revalidate();
        categoryPanel.repaint();
    }
}