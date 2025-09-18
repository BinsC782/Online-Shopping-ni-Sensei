package com.shopping;

import com.shopping.data.FileHandler;
import com.shopping.model.Product;
import com.shopping.model.User;
import java.util.*;

public class OnlineShoppingApp {
    private static Scanner scanner = new Scanner(System.in);
    private static User currentUser = null;
    
    public static void main(String[] args) {
        try {
            FileHandler.initializeDataFiles();
            showMainMenu();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
    
    private static void showMainMenu() {
        while (true) {
            System.out.println("\n=== Online Shopping App ===");
            System.out.println("1. Register");
            System.out.println("2. Login");
            
            // Only show "View Products" if user is logged in
            if (currentUser != null) {
                System.out.println("3. View Products");
                System.out.println("4. Logout");
                System.out.println("5. Exit");
            } else {
                System.out.println("3. Exit");
            }
            
            System.out.print("Choose an option: ");
            
            int maxChoice;
            if (currentUser != null) {
                maxChoice = 5;  // Shows all menu options for logged-in users
            } else {
                maxChoice = 3;  // Shows only basic options for guests
            }
            int choice = readIntInput(1, maxChoice);
            
            switch (choice) {
                case 1:
                    registerUser();
                    break;
                case 2:
                    loginUser();
                    break;
                case 3:
                    if (currentUser != null) {
                        viewProducts();
                    } else {
                        System.out.println("Thank you for using our app!");
                        return;
                    }
                    break;
                case 4:
                    if (currentUser != null) {
                        System.out.println("Logging out...");
                        currentUser = null;
                        continue;
                    }
                    break;
                case 5:
                    if (currentUser != null) {
                        System.out.println("Thank you for using our app!");
                        return;
                    }
                    break;
            }
            
            // If user is logged in, show user menu
            if (currentUser != null) {
                showUserMenu();
                // If user logs out from user menu, continue to main menu
                if (currentUser == null) {
                    continue;
                }
            }
        }
    }
    
    private static void showUserMenu() {
        while (currentUser != null) {
            System.out.println("\n=== Welcome, " + currentUser.getUsername() + " ===");
            System.out.println("1. View All Products");
            System.out.println("2. Search Products");
            System.out.println("3. View My Orders");
            System.out.println("4. Logout");
            System.out.print("Choose an option: ");
            
            int choice = readIntInput(1, 4);
            
            switch (choice) {
                case 1:
                    viewProducts();
                    break;
                case 2:
                    searchProducts();
                    break;
                case 3:
                    viewOrders();
                    break;
                case 4:
                    System.out.println("Logging out...");
                    currentUser = null;
                    return;
            }
        }
    }
    
    private static void registerUser() {
        System.out.println("\n=== Register New User ===");
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        System.out.print("Enter email: ");
        String email = scanner.nextLine();
        
        if (FileHandler.registerUser(username, password, email)) {
            System.out.println("Registration successful! Please login.");
        } else {
            System.out.println("Username already exists. Please try again.");
        }
    }
    
    private static void loginUser() {
        System.out.println("\n=== Login ===");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        
        currentUser = FileHandler.authenticateUser(username, password);
        if (currentUser != null) {
            System.out.println("Login successful! Welcome, " + currentUser.getUsername() + "!");
        } else {
            System.out.println("Invalid username or password. Please try again.");
        }
    }
    
    private static void viewProducts() {
        List<Product> products = FileHandler.loadProductsAsObjects();
        System.out.println("\n=== Available Products ===");
        if (products.isEmpty()) {
            System.out.println("No products available.");
            return;
        }
        
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            System.out.printf("%d. %s - $%.2f (%d in stock)\n", 
                i + 1, p.getName(), p.getPrice(), p.getStock());
            System.out.println("   " + p.getDescription());
        }
        
        if (currentUser != null) {
            System.out.print("\nEnter product number to order (0 to cancel): ");
            int choice = readIntInput(0, products.size());
            if (choice > 0) {
                placeOrder(products.get(choice - 1));
            }
        } else {
            System.out.println("\nPlease login to place an order.");
        }
    }
    
    private static void searchProducts() {
        System.out.print("\nEnter search term: ");
        String term = scanner.nextLine().toLowerCase();
        List<Product> products = FileHandler.loadProductsAsObjects();
        
        System.out.println("\n=== Search Results ===");
        boolean found = false;
        
        for (Product p : products) {
            if (p.getName().toLowerCase().contains(term) || 
                p.getDescription().toLowerCase().contains(term)) {
                System.out.printf("%s - $%.2f\n   %s\n", 
                    p.getName(), p.getPrice(), p.getDescription());
                found = true;
            }
        }
        
        if (!found) {
            System.out.println("No products found matching '" + term + "'");
        }
    }
    
    private static void placeOrder(Product product) {
        System.out.printf("\nOrdering: %s - $%.2f\n", product.getName(), product.getPrice());
        System.out.print("Enter quantity: ");
        int quantity = readIntInput(1, product.getStock());
        
        // In a real app, we would create an Order object and save it
        System.out.printf("\nOrder placed! %d x %s for $%.2f\n", 
            quantity, product.getName(), (quantity * product.getPrice()));
    }
    
    private static void viewOrders() {
        // In a real app, we would load and display the user's orders
        System.out.println("\n=== Your Orders ===");
        System.out.println("Order history feature coming soon!");
    }
    
    private static int readIntInput(int min, int max) {
        while (true) {
            try {
                int input = Integer.parseInt(scanner.nextLine());
                if (input >= min && input <= max) {
                    return input;
                }
                System.out.printf("Please enter a number between %d and %d: ", min, max);
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Please enter a number: ");
            }
        }
    }
}