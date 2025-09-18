package com.shopping.ui.LoginUi;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

public class LoginDialog extends JDialog {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private com.shopping.model.User authenticatedUser;
    private boolean cancelled = false;

    public LoginDialog(Frame parent) {
        super(parent, "PowerPuffGirls OnlineShopping - Welcome!", true);
        initializeComponents();
        setSize(800, 450);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(new Color(255, 165, 0));
        setResizable(false);
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());

        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(255, 140, 0));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel titleLabel = new JLabel("PowerPuffGirls OnlineShopping", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Your one-stop shop for everything!", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        subtitleLabel.setForeground(Color.WHITE);

        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH);
        add(titlePanel, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(255, 165, 0));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // Username
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Arial", Font.BOLD, 14));
        userLabel.setForeground(Color.WHITE);
        mainPanel.add(userLabel, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 14));
        mainPanel.add(usernameField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Arial", Font.BOLD, 14));
        passLabel.setForeground(Color.WHITE);
        mainPanel.add(passLabel, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        mainPanel.add(passwordField, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        buttonPanel.setBackground(new Color(255, 165, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton loginButton = createStyledButton("Login", new Color(34, 139, 34));
        JButton registerButton = createStyledButton("Register", new Color(70, 130, 180));
        JButton exitButton = createStyledButton("Exit", new Color(220, 20, 60));

        loginButton.addActionListener(new LoginAction());
        registerButton.addActionListener(new RegisterAction());
        exitButton.addActionListener(e -> {
            cancelled = true;
            setVisible(false);
        });

        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        buttonPanel.add(exitButton);

        add(buttonPanel, BorderLayout.SOUTH);
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

    private class LoginAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(LoginDialog.this, "Please enter both username and password.");
                return;
            }

            authenticatedUser = com.shopping.data.FileHandler.authenticateUser(username, password);
            if (authenticatedUser != null) {
                // Show welcome message
                String welcomeMessage = "Welcome back, " + username + "!";
                JOptionPane.showMessageDialog(LoginDialog.this, welcomeMessage, "Login Successful", JOptionPane.INFORMATION_MESSAGE);
                setVisible(false);
            } else {
                JOptionPane.showMessageDialog(LoginDialog.this,
                    "Invalid username or password.",
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class RegisterAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Create a comprehensive registration dialog
            JPanel registerPanel = new JPanel(new GridLayout(4, 2, 10, 10));
            registerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JTextField regUsernameField = new JTextField();
            JPasswordField regPasswordField = new JPasswordField();
            JPasswordField regConfirmPasswordField = new JPasswordField();
            JTextField regEmailField = new JTextField();

            registerPanel.add(new JLabel("Username:"));
            registerPanel.add(regUsernameField);
            registerPanel.add(new JLabel("Password:"));
            registerPanel.add(regPasswordField);
            registerPanel.add(new JLabel("Confirm Password:"));
            registerPanel.add(regConfirmPasswordField);
            registerPanel.add(new JLabel("Email:"));
            registerPanel.add(regEmailField);

            int result = JOptionPane.showConfirmDialog(
                LoginDialog.this,
                registerPanel,
                "Create New Account",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );

            if (result == JOptionPane.OK_OPTION) {
                String username = regUsernameField.getText().trim();
                String password = new String(regPasswordField.getPassword());
                String confirmPassword = new String(regConfirmPasswordField.getPassword());
                String email = regEmailField.getText().trim();

                // Validation
                if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
                    JOptionPane.showMessageDialog(LoginDialog.this,
                        "Please fill in all fields.", "Registration Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    JOptionPane.showMessageDialog(LoginDialog.this,
                        "Passwords do not match.", "Registration Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (password.length() < 4) {
                    JOptionPane.showMessageDialog(LoginDialog.this,
                        "Password must be at least 4 characters long.", "Registration Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (com.shopping.data.FileHandler.registerUser(username, password, email)) {
                    JOptionPane.showMessageDialog(LoginDialog.this,
                        "🎉 Registration successful!\n\nYou can now login with your credentials.",
                        "Welcome!", JOptionPane.INFORMATION_MESSAGE);

                    // Auto-fill the login fields
                    usernameField.setText(username);
                    passwordField.setText("");
                    passwordField.requestFocus();
                } else {
                    JOptionPane.showMessageDialog(LoginDialog.this,
                        "Username already exists. Please choose a different username.",
                        "Registration Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    public com.shopping.model.User showDialog() {
        setVisible(true);
        return cancelled ? null : authenticatedUser;
    }
}