package com.cloudstorage.fx.controllers;

import com.cloudstorage.database.InsertUser;
import com.cloudstorage.fx.utils.AlertUtils;
import com.cloudstorage.service.MinioService; // Make sure this is imported
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label confirmError;

    // Controls & Images
    @FXML private Button registerButton;
    @FXML private Button loginButton;
    @FXML private ImageView mainImage;
    @FXML private ImageView logoImage;

    private final BooleanProperty isProcessing = new SimpleBooleanProperty(false);

    @FXML
    private void initialize() {
        // Layout responsive logic
        HBox.setHgrow(leftContainer, Priority.ALWAYS);
        HBox.setHgrow(rightContainer, Priority.ALWAYS);

        leftContainer.prefWidthProperty().bind(root.widthProperty().multiply(0.55));
        rightContainer.prefWidthProperty().bind(root.widthProperty().multiply(0.45));

        topLeft.prefHeightProperty().bind(root.heightProperty().multiply(0.25));
        topRight.prefHeightProperty().bind(root.heightProperty().multiply(0.25));

        mainImage.fitWidthProperty().bind(rightContainer.widthProperty().multiply(0.8));
        logoImage.fitWidthProperty().bind(bottomRight.widthProperty().multiply(0.6));
        logoImage.fitHeightProperty().bind(bottomRight.heightProperty().multiply(0.6));

        ChangeListener<Number> stageSizeListener = (obs, oldVal, newVal) -> updateResponsiveLayout();
        root.widthProperty().addListener(stageSizeListener);
        root.heightProperty().addListener(stageSizeListener);

        Platform.runLater(this::updateResponsiveLayout);

        // ✅ Real-time validation
        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                emailError.setText("Email is required");
                emailField.setStyle("-fx-border-color: red;");
            } else if (!newVal.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                emailError.setText("Invalid email format");
                emailField.setStyle("-fx-border-color: red;");
            } else {
                emailError.setText("");
                emailField.setStyle("-fx-border-color: green;");
            }
        });

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                passwordError.setText("Password required");
                passwordField.setStyle("-fx-border-color: red;");
            } else if (!newVal.matches("^(?=.*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/]).{8,}$")) {
                passwordError.setText("Min 8 chars + 1 special symbol");
                passwordField.setStyle("-fx-border-color: red;");
            } else {
                passwordError.setText("");
                passwordField.setStyle("-fx-border-color: green;");
            }
        });

        confPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.equals(passwordField.getText())) {
                confirmError.setText("Passwords do not match");
                confPasswordField.setStyle("-fx-border-color: red;");
            } else {
                confirmError.setText("");
                confPasswordField.setStyle("-fx-border-color: green;");
            }
        });

        // ✅ Disable button until valid
        registerButton.disableProperty().bind(
                emailError.textProperty().isNotEmpty()
                        .or(passwordError.textProperty().isNotEmpty())
                        .or(confirmError.textProperty().isNotEmpty())
                        .or(emailField.textProperty().isEmpty())
                        .or(passwordField.textProperty().isEmpty())
                        .or(confPasswordField.textProperty().isEmpty())
                        .or(isProcessing)
        );
    }

    private void updateResponsiveLayout() {
        double width = root.getWidth();
        if (width == 0) return;

        double fontSize = Math.max(12, width / 70);
        root.setStyle("-fx-font-size: " + fontSize + "px;");

        double sidePadding = leftContainer.getWidth() * 0.12;
        headerBox.setPadding(new Insets(0, sidePadding, 20, sidePadding));
        formBox.setPadding(new Insets(20, sidePadding, 0, sidePadding));
    }

    // ✅ Navigation
    @FXML
    private void OpenLoginPage(ActionEvent event) {
        openScene("/com/cloudstorage/fx/login.fxml", "Login");
    }

    @FXML
    public void OpenHomePage(MouseEvent mouseEvent) {
        openScene("/com/cloudstorage/fx/HomePage.fxml", "Home");
    }

    private void openScene(String path, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ Register Logic with MinIO Integration
    @FXML
    private void handleRegister() {

        String first = fnameField.getText();
        String last = lnameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();

        isProcessing.set(true);
        registerButton.setText("⏳ Creating...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                // 1. Create User in Database
                InsertUser insertUser = new InsertUser();
                String dbResult = insertUser.registerNewUser(email, password, first, last);

                // 2. If Database success (result is null), create Cloud Bucket
                if (dbResult == null) {
                    try {
                        // MinIO/S3 requires bucket names to be lowercase, nums, hyphens only
                        String rawName = first + "-" + last;
                        // Sanitize: Force lowercase and remove anything that isn't a letter, number, or hyphen
                        String safeBucketName = rawName.toLowerCase().replaceAll("[^a-z0-9-]", "");
                        // Call MinioService to create the bucket
                        MinioService minioService = new MinioService();
                        minioService.createBucket(safeBucketName);

                    } catch (Exception e) {
                        e.printStackTrace();
                        // Optional: Return a specific warning if bucket creation fails
                        // For now, we allow the user to exist even if bucket creation glitches
                        System.err.println("Warning: User created but Bucket creation failed: " + e.getMessage());
                    }
                }
                return dbResult;
            }
        };

        task.setOnSucceeded(e -> {
            String result = task.getValue();
            isProcessing.set(false);
            registerButton.setText("CREATE ACCOUNT");

            if (result == null) {
                AlertUtils.showSuccess("Registration Successful", "Your account has been created successfully!");
                OpenLoginPage(null);
            } else {
                AlertUtils.showError("Registration Failed: ", result);
            }
        });

        task.setOnFailed(e -> {
            isProcessing.set(false);
            registerButton.setText("CREATE ACCOUNT");

            // GET THE REAL ERROR
            Throwable error = task.getException();
            error.printStackTrace(); // Print it to console so you can copy it

            // Show it in the Alert
            showAlert(Alert.AlertType.ERROR, "Registration Failed", error.getMessage());
        });

        new Thread(task).start();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}