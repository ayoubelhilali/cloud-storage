package com.cloudstorage.fx.controllers;

import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.LoginUser;
import com.cloudstorage.model.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

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

    private static final String PREF_EMAIL = "user_email";
    private static final String PREF_PASSWORD = "user_password";
    private static final String PREF_TIME = "login_time";
    private static final long SESSION_TIMEOUT = 3600 * 1000;

    @FXML
    private void initialize() {
        if (bottomRight != null && mainImage != null) {
            mainImage.fitWidthProperty().bind(bottomRight.widthProperty().multiply(0.9));
            mainImage.fitHeightProperty().bind(bottomRight.heightProperty().multiply(0.9));
            logoImage.fitWidthProperty().bind(bottomRight.widthProperty().multiply(0.6));
            logoImage.fitHeightProperty().bind(bottomRight.heightProperty().multiply(0.6));
        }

        testMinioConnection();
        checkSavedSession();
    }

    private void checkSavedSession() {
        Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
        long lastLoginTime = prefs.getLong(PREF_TIME, 0);
        long currentTime = System.currentTimeMillis();

        if ((currentTime - lastLoginTime) < SESSION_TIMEOUT) {
            String savedEmail = prefs.get(PREF_EMAIL, "");
            String savedPass = prefs.get(PREF_PASSWORD, "");

            if (!savedEmail.isEmpty() && !savedPass.isEmpty()) {
                System.out.println("Restoring session for: " + savedEmail);
                emailField.setText(savedEmail);
                passwordField.setText(savedPass);
                performLoginLogic(savedEmail, savedPass);
            }
        }
    }

    private void saveSession(String email, String password) {
        Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
        prefs.put(PREF_EMAIL, email);
        prefs.put(PREF_PASSWORD, password);
        prefs.putLong(PREF_TIME, System.currentTimeMillis());
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
        performLoginLogic(email, password);
    }

    private void performLoginLogic(String email, String password) {
        LoginUser loginUser = new LoginUser();
        loginButton.setDisable(true);
        loginButton.setText("⏳ Logging in...");

        Task<Boolean> loginTask = new Task<>() {
            @Override
            protected Boolean call() {
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

                saveSession(email, password);

                // --- FIX: Get the Real User Object ---
                User authenticatedUser = loginUser.getLoggedUser();

                if (authenticatedUser != null) {
                    SessionManager.login(authenticatedUser); // Set Global Session

                    // PASS THE USER OBJECT, NOT STRINGS
                    navigateToDashboard(authenticatedUser);
                }

            } else {
                statusLabel.setText("❌ Invalid email or password");
                statusLabel.setStyle("-fx-text-fill: red;");
                try {
                    Preferences.userNodeForPackage(LoginController.class).clear();
                } catch (BackingStoreException e) {
                    e.printStackTrace();
                }
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

    // --- UPDATED METHOD: Accepts User object instead of Strings ---
    private void navigateToDashboard(User user) {
        try {
            var resource = getClass().getResource("/com/cloudstorage/fx/Dashboard.fxml");
            if (resource == null) throw new IOException("Dashboard.fxml not found");

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            DashboardController controller = loader.getController();

            // --- PASS THE FULL USER OBJECT (Contains ID 5) ---
            controller.setUserData(user);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            if (stage == null && emailField.getScene() != null) {
                stage = (Stage) emailField.getScene().getWindow();
            }

            if (stage != null) {
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setTitle("Cloud Storage - Dashboard");
                stage.sizeToScene();
                stage.setMinWidth(1100);
                stage.setMinHeight(700);
                stage.centerOnScreen();
                stage.setResizable(true);
                stage.show();
            }

        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Error loading dashboard");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
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

    private void testMinioConnection() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/minioStatus"))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                            boolean isConnected = jsonResponse.get("minioConnected").getAsBoolean();
                            if (isConnected) {
                                statusLabel.setStyle("-fx-text-fill: green;");
                            } else {
                                statusLabel.setStyle("-fx-text-fill: red;");
                            }
                        } catch (Exception e) {}
                    }
                }))
                .exceptionally(e -> null);
    }
}