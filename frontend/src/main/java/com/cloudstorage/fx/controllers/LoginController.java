package com.cloudstorage.fx.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class LoginController {

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Label statusLabel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @FXML
    private void initialize() {
        // Test MinIO connection on login page load
        testMinioConnection();
    }

    private void testMinioConnection() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/minioStatus"))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                            boolean isConnected = jsonResponse.get("minioConnected").getAsBoolean();
                            if (isConnected) {
                                statusLabel.setText("✅ MinIO connection successful!");
                                statusLabel.setStyle("-fx-text-fill: green;");
                            } else {
                                statusLabel.setText("❌ MinIO connection failed on backend.");
                                statusLabel.setStyle("-fx-text-fill: red;");
                            }
                        } else {
                            statusLabel.setText("❌ Backend API error: " + response.statusCode());
                            statusLabel.setStyle("-fx-text-fill: red;");
                        }
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("❌ Could not connect to backend: " + e.getMessage());
                        statusLabel.setStyle("-fx-text-fill: red;");
                    });
                    return null;
                });
    }

    @FXML
    private void handleLogin() {
        // Existing login logic can be added here
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please fill in all fields");
            statusLabel.setStyle("-fx-text-fill: red;");
        } else {
            // TODO: Implement actual login logic
            statusLabel.setText("Login functionality not implemented yet");
            statusLabel.setStyle("-fx-text-fill: blue;");
        }
    }
}
