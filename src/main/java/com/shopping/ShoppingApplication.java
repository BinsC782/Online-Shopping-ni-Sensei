package com.shopping;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.shopping.service.ShoppingService;
import com.shopping.data.FileHandler;
import java.io.IOException;

/**
 * Main JavaFX Application class for the Online Shopping desktop application.
 * This replaces the web-based ServerMain and provides a native desktop interface.
 */
public class ShoppingApplication extends Application {

    private ShoppingService shoppingService;

    @Override
    public void init() throws Exception {
        super.init();
        System.out.println("=== Initializing ShoppingApplication ===");

        try {
            // Initialize the shopping service with file handler
            FileHandler fileHandler = new FileHandler();
            this.shoppingService = new ShoppingService(fileHandler);
            System.out.println("ShoppingService initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize ShoppingService: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to prevent application startup
        }
    }

    @Override
    public void start(Stage primaryStage) {
        System.out.println("=== Starting JavaFX Application ===");

        try {
            // Load the FXML file
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/MainView.fxml"));
            System.out.println("Loading FXML from: " + getClass().getResource("/fxml/MainView.fxml"));

            Parent root = loader.load();
            System.out.println("FXML loaded successfully");

            // Get the controller and set the shopping service
            MainController controller = loader.getController();
            System.out.println("Controller loaded: " + (controller != null ? "SUCCESS" : "FAILED"));

            if (controller != null && shoppingService != null) {
                controller.setShoppingService(shoppingService);
                System.out.println("ShoppingService injected successfully");
            } else {
                System.err.println("ERROR: Controller or ShoppingService is null");
                System.err.println("Controller: " + controller);
                System.err.println("ShoppingService: " + shoppingService);
            }

            // Create scene and apply CSS
            Scene scene = new Scene(root, 1200, 800);
            String cssPath = getClass().getResource("/css/application.css").toExternalForm();
            if (cssPath != null) {
                scene.getStylesheets().add(cssPath);
                System.out.println("CSS loaded successfully");
            } else {
                System.err.println("WARNING: CSS file not found");
            }

            // Configure stage
            primaryStage.setTitle("🛒 Online Shopping Application");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.show();

            System.out.println("Application started successfully");

        } catch (Exception e) {
            System.err.println("Failed to start application: " + e.getMessage());
            e.printStackTrace();

            // Show error dialog
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Application Error");
            alert.setHeaderText("Failed to Load Application");
            alert.setContentText("The application could not be started due to an error:\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Main method to launch the JavaFX application
     */
    public static void main(String[] args) {
        System.out.println("=== Launching Shopping Application ===");
        launch(args);
    }
}
