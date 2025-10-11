package com.shopping;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.shopping.data.FileHandler;
import com.shopping.service.ShoppingService;

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
        // Initialize the shopping service with file handler
        FileHandler fileHandler = new FileHandler();
        fileHandler.initializeDataFiles();
        this.shoppingService = new ShoppingService(fileHandler);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the main FXML layout
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the shopping service
            MainController controller = loader.getController();
            controller.setShoppingService(shoppingService);

            // Create the scene with CSS styling
            Scene scene = new Scene(root, 1200, 800);

            // Load CSS stylesheets (these will be created from your existing CSS)
            scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());

            // Configure the primary stage
            primaryStage.setTitle("Online Shopping Application");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);

            // Set application icon (optional)
            // primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/app-icon.png")));

            // Show the application
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Failed to load application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        // Clean up resources if needed
        if (shoppingService != null) {
            // Add any cleanup logic here
        }
    }

    /**
     * Main method to launch the JavaFX application
     */
    public static void main(String[] args) {
        launch(args);
    }
}
