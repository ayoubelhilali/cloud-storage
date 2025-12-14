package com.cloudstorage.fx.controllers;

import com.cloudstorage.database.LoginUser; // Make sure this is imported
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.prefs.Preferences;

public class HomeController {

    @FXML private Button registerButton, loginButton;
    @FXML private ImageView mainImage;
    @FXML private StackPane topLeft, bottomLeft;
    @FXML private ImageView logoImage;
    @FXML private Label titleLabel, subtitleLabel;
    @FXML private VBox rightContainer;

    // Preference keys (Must match LoginController)
    private static final String PREF_EMAIL = "user_email";
    private static final String PREF_PASSWORD = "user_password";
    private static final String PREF_TIME = "login_time";
    private static final long SESSION_TIMEOUT = 3600 * 1000; // 1 hour

    @FXML
    public void initialize() {
        // 1. Setup UI bindings
        setupUIBindings();

        // 2. Setup Animations
        setupSmoothHover(loginButton);

        // 3. CHECK FOR SAVED SESSION IMMEDIATELY
        checkSavedSession();
    }

    private void checkSavedSession() {
        Preferences prefs = Preferences.userNodeForPackage(LoginController.class); // Use LoginController class to access same node
        long lastLoginTime = prefs.getLong(PREF_TIME, 0);
        long currentTime = System.currentTimeMillis();

        if ((currentTime - lastLoginTime) < SESSION_TIMEOUT) {
            String savedEmail = prefs.get(PREF_EMAIL, "");
            String savedPass = prefs.get(PREF_PASSWORD, "");

            if (!savedEmail.isEmpty() && !savedPass.isEmpty()) {
                System.out.println("Auto-login triggered from Home...");
                performAutoLogin(savedEmail, savedPass);
            }
        }
    }

    private void performAutoLogin(String email, String password) {
        loginButton.setText("Loading...");
        loginButton.setDisable(true);
        registerButton.setDisable(true);

        // --- FIX: Change Task type from Boolean to LoginUser ---
        Task<LoginUser> loginTask = new Task<>() {
            @Override
            protected LoginUser call() {
                LoginUser loginUser = new LoginUser();
                boolean isLoggedIn = loginUser.login(email, password);
                // Return the object if login worked, otherwise null
                return isLoggedIn ? loginUser : null;
            }
        };

        loginTask.setOnSucceeded(event -> {
            LoginUser validUser = loginTask.getValue(); // Get the result

            if (validUser != null) {
                // --- FIX: GET REAL NAMES ---
                String fName = validUser.getLoggedUser().getFirstName();
                String lName = validUser.getLoggedUser().getLastName();

                navigateToDashboard(fName + " " + lName);
            } else {
                // Login failed (password changed?), reset UI
                loginButton.setText("Login");
                loginButton.setDisable(false);
                registerButton.setDisable(false);
            }
        });

        loginTask.setOnFailed(event -> {
            loginButton.setText("Login");
            loginButton.setDisable(false);
            registerButton.setDisable(false);
            event.getSource().getException().printStackTrace();
        });

        new Thread(loginTask).start();
    }

    private void navigateToDashboard(String fullName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/Dashboard.fxml"));
            Parent root = loader.load();

            DashboardController controller = loader.getController();
            controller.setUsername(fullName);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Cloud Storage - Dashboard");

            // --- FIX RESPONSIVENESS ---
            stage.sizeToScene();
            stage.setMinWidth(1100);
            stage.setMinHeight(700);

            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- EXISTING UI METHODS (Cleaned up for readability) ---

    private void setupUIBindings() {
        mainImage.fitWidthProperty().bind(bottomLeft.widthProperty().multiply(0.9));
        mainImage.fitHeightProperty().bind(bottomLeft.heightProperty().multiply(0.9));
        logoImage.fitWidthProperty().bind(topLeft.widthProperty().multiply(0.7));
        logoImage.fitHeightProperty().bind(topLeft.heightProperty().multiply(0.7));

        titleLabel.styleProperty().bind(
                rightContainer.widthProperty().multiply(0.1)
                        .asString("-fx-font-size: %.0fpx; -fx-font-weight: bold;")
        );

        subtitleLabel.styleProperty().bind(
                rightContainer.widthProperty().multiply(0.05)
                        .asString("-fx-font-size: %.0fpx;")
        );

        registerButton.prefWidthProperty().bind(rightContainer.widthProperty().multiply(0.4));
        registerButton.prefHeightProperty().bind(rightContainer.heightProperty().multiply(0.1));

        loginButton.prefWidthProperty().bind(rightContainer.widthProperty().multiply(0.3));
        loginButton.prefHeightProperty().bind(rightContainer.heightProperty().multiply(0.1));
    }

    @FXML
    private void openLoginPage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
        } catch (Exception e) {
            e.printStackTrace();
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

    private void setupSmoothHover(Button button) {
        String originalStyle = button.getStyle();
        String hoverStyle = "-fx-background-color: #3B82F6; -fx-text-fill: white;";

        button.hoverProperty().addListener((obs, oldVal, isHovering) -> {
            if (isHovering) {
                animateStyleTransition(button, originalStyle, hoverStyle, 300);
            } else {
                animateStyleTransition(button, hoverStyle, originalStyle, 300);
            }
        });
    }

    private void animateStyleTransition(Button button, String fromStyle, String toStyle, int duration) {
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.ZERO, new KeyValue(button.styleProperty(), fromStyle)),
                new KeyFrame(Duration.millis(duration), new KeyValue(button.styleProperty(), toStyle))
        );
        timeline.play();
    }
}