package com.shopping.ui.MainProgram;

import com.shopping.data.FileHandler;
import com.shopping.model.Product;
import com.shopping.model.User;
import com.shopping.ui.ProductUi.ProductPanel;
import com.shopping.ui.CartUi.CartPanel;
import com.shopping.ui.LoginUi.LogoutHandler;
import java.awt.*;
import java.util.List;
import javax.swing.*;

public class MainFrame extends JFrame {
    private ProductPanel productPanel;
    private CartPanel cartPanel;
    private User currentUser;

    public MainFrame(User user) {
        this.currentUser = user;
        setTitle("PowerPuffGirls OnlineShopping - Welcome " + user.getUsername());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Set orange theme
        getContentPane().setBackground(new Color(255, 165, 0));

        // Top panel with logout button
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.setBackground(new Color(255, 165, 0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Arial", Font.BOLD, 12));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setBackground(new Color(220, 20, 60));
        logoutButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        logoutButton.setFocusPainted(false);
        logoutButton.addActionListener(new LogoutHandler(this));

        topPanel.add(logoutButton);
        add(topPanel, BorderLayout.NORTH);

        List<Product> products = FileHandler.loadProductsAsObjects();
        cartPanel = new CartPanel(user.getUsername());
        productPanel = new ProductPanel(products, cartPanel);

        add(productPanel, BorderLayout.CENTER);
        add(cartPanel, BorderLayout.EAST);

        // 16:9 aspect ratio: 1600x900
        setSize(1600, 900);

        setLocationRelativeTo(null);
        setVisible(true);
    }

}