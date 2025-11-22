package com.cloudstorage.fx.controllers;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class RegisterController {

    // Main Containers
    @FXML private HBox root;
    @FXML private VBox leftContainer;
    @FXML private VBox rightContainer;

    // Internal Sections for Padding/Alignment
    @FXML private StackPane topLeft;
    @FXML private StackPane topRight;
    @FXML private StackPane bottomLeft;
    @FXML private StackPane bottomRight;
    @FXML private VBox headerBox;
    @FXML private VBox formBox;

    // Inputs
    @FXML private TextField fnameField;
    @FXML private TextField lnameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confPasswordField;

    // Controls & Images
    @FXML private Button registerButton;
    @FXML private Button loginButton;
    @FXML private ImageView mainImage;
    @FXML private ImageView logoImage;

    @FXML
    private void initialize() {
        // 1. Split Layout Columns (55% Left, 45% Right)
        HBox.setHgrow(leftContainer, Priority.ALWAYS);
        HBox.setHgrow(rightContainer, Priority.ALWAYS);

        leftContainer.prefWidthProperty().bind(root.widthProperty().multiply(0.55));
        rightContainer.prefWidthProperty().bind(root.widthProperty().multiply(0.45));

        // 2. Vertical Split for Visuals (Top 25%, Bottom 75%)
        topLeft.prefHeightProperty().bind(root.heightProperty().multiply(0.25));
        topRight.prefHeightProperty().bind(root.heightProperty().multiply(0.25));

        // 3. Bind Image Sizes
        // Main image takes 80% of width
        mainImage.fitWidthProperty().bind(rightContainer.widthProperty().multiply(0.8));
        // Logo height restricted to 60% of the top-right blue area
        logoImage.fitWidthProperty().bind(bottomRight.widthProperty().multiply(0.6));
        logoImage.fitHeightProperty().bind(bottomRight.heightProperty().multiply(0.6));

        // 4. Responsive Listeners (Dynamic Font & Padding)
        ChangeListener<Number> stageSizeListener = (observable, oldValue, newValue) -> {
            updateResponsiveLayout();
        };

        // Add listeners to root width/height
        root.widthProperty().addListener(stageSizeListener);
        root.heightProperty().addListener(stageSizeListener);

        // Run once initially to set correct sizes
        Platform.runLater(this::updateResponsiveLayout);
    }

    private void updateResponsiveLayout() {
        double width = root.getWidth();

        // Prevent calculation if stage is not yet visible
        if (width == 0) return;

        // --- A. Dynamic Font Size ---
        // Base size logic: Window Width / 70.
        // Example: 1400px width / 70 = 20px font size.
        double fontSize = Math.max(12, width / 70);

        // Apply font size to the root. CSS 'em' units will scale relative to this.
        root.setStyle("-fx-font-size: " + fontSize + "px;");

        // --- B. Dynamic Padding ---
        // Keep form centered by adding padding based on container width
        double sidePadding = leftContainer.getWidth() * 0.12; // 12% padding

        // Apply padding to the internal VBoxes
        headerBox.setPadding(new Insets(0, sidePadding, 20, sidePadding));
        formBox.setPadding(new Insets(20, sidePadding, 0, sidePadding));
    }

    // --- NAVIGATION METHODS (Must be Present!) ---

    @FXML
    private void OpenLoginPage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void OpenHomePage(MouseEvent mouseEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/HomePage.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) logoImage.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Home");
            stage.setMinWidth(750);
            stage.setMinHeight(450);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegister() {
        System.out.println("Register clicked. Name: " + fnameField.getText());
        // Add registration logic here
    }
}