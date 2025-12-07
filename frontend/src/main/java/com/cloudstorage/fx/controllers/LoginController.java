package com.cloudstorage.fx.controllers;

import com.cloudstorage.database.LoginUser;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton, registerButton;
    @FXML private Label statusLabel;
    @FXML private ImageView mainImage;
    @FXML private StackPane topRight, bottomRight;
    @FXML private ImageView logoImage;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @FXML
    private void initialize() {
        // Resize images dynamically
        mainImage.fitWidthProperty().bind(bottomRight.widthProperty().multiply(0.9));
        mainImage.fitHeightProperty().bind(bottomRight.heightProperty().multiply(0.9));
        logoImage.fitWidthProperty().bind(bottomRight.widthProperty().multiply(0.6));
        logoImage.fitHeightProperty().bind(bottomRight.heightProperty().multiply(0.6));

        // Check MinIO connection (optional)
        testMinioConnection();
    }

    private void testMinioConnection() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/minioStatus"))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                        boolean isConnected = jsonResponse.get("minioConnected").getAsBoolean();
                        if (isConnected) {
                            statusLabel.setText("✅ MinIO connection successful!");
                            statusLabel.setStyle("-fx-text-fill: green;");
                        } else {
                            //statusLabel.setText("❌ MinIO connection failed on backend.");
                            statusLabel.setStyle("-fx-text-fill: red;");
                        }
                    } else {
                        //statusLabel.setText("❌ Backend API error: " + response.statusCode());
                        statusLabel.setStyle("-fx-text-fill: red;");
                    }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        //statusLabel.setText("❌ Could not connect to backend: " + e.getMessage());
                        statusLabel.setStyle("-fx-text-fill: red;");
                    });
                    return null;
                });
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please fill in all fields");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // Disable login button and show spinner
        loginButton.setDisable(true);
        loginButton.setText("⏳ Logging in...");

        Task<Boolean> loginTask = new Task<>() {
            @Override
            protected Boolean call() {
                LoginUser loginUser = new LoginUser();
                return loginUser.login(email, password);
            }
        };

        loginTask.setOnSucceeded(event -> {
            boolean success = loginTask.getValue();
            loginButton.setDisable(false);
            loginButton.setText("LOGIN");

            if (success) {
                statusLabel.setText("✅ Login successful!");
                statusLabel.setStyle("-fx-text-fill: green;");

                try {
                    // LOAD DASHBOARD
                    // Ensure the path starts with '/' and is spelled correctly
                    var resource = getClass().getResource("/com/cloudstorage/fx/Dashboard.fxml");

                    if (resource == null) {
                        throw new IOException("Dashboard.fxml not found at path: /com/cloudstorage/fx/Dashboard.fxml");
                    }

                    FXMLLoader loader = new FXMLLoader(resource);
                    Parent root = loader.load();

                    // GET CURRENT STAGE (WINDOW)
                    Stage stage = (Stage) loginButton.getScene().getWindow();

                    // SWAP SCENE
                    Scene scene = new Scene(root);
                    stage.setScene(scene);
                    stage.setTitle("Cloud Storage - Dashboard");

                    // OPTIONAL: Resize and Center
                    // If your dashboard is bigger than the login screen, resizing happens here
                    stage.sizeToScene();
                    stage.centerOnScreen();
                    stage.show();

                } catch (IOException e) {
                    e.printStackTrace();
                    statusLabel.setText("❌ Error loading dashboard view");
                    statusLabel.setStyle("-fx-text-fill: red;");
                }

            } else {
                statusLabel.setText("❌ Invalid email or password");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        });

        loginTask.setOnFailed(event -> {
            loginButton.setDisable(false);
            loginButton.setText("LOGIN");
            statusLabel.setText("❌ Unexpected error");
            statusLabel.setStyle("-fx-text-fill: red;");
            event.getSource().getException().printStackTrace();
        });

        new Thread(loginTask).start();
    }

    @FXML
    private void OpenRegisterPage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/register.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Create account");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}