package com.cloudstorage.fx.controllers;

import com.cloudstorage.config.SessionManager;
import com.cloudstorage.controller.ShareController;
import com.cloudstorage.database.UserDAO;
import com.cloudstorage.fx.utils.AlertUtils;
import com.cloudstorage.model.User;
import io.minio.MinioClient;
import com.cloudstorage.config.MinioConfig;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

/**
 * Enhanced Share Dialog Controller with user search functionality.
 * Features:
 * - Search users by name or email
 * - Visual feedback with user card
 * - Loading indicator during search
 * - Error messages for no results
 */
public class ShareDialogEnhancedController {

    // --- FXML Components ---
    @FXML private Label fileNameLabel;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button shareButton;
    @FXML private Button closeButton;

    // Result containers
    @FXML private StackPane resultsStackPane;
    @FXML private VBox loadingContainer;
    @FXML private VBox noResultsContainer;
    @FXML private VBox userCardContainer;
    @FXML private VBox initialContainer;
    @FXML private Label noResultsLabel;

    // User card components
    @FXML private HBox userCard;
    @FXML private Circle userAvatarCircle;
    @FXML private Label userInitialsLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private FontIcon selectedIcon;

    // --- State ---
    private String targetFilename;
    private String targetBucket;
    private User selectedUser;
    private final UserDAO userDAO = new UserDAO();
    private ShareController shareController;

    @FXML
    public void initialize() {
        // Initialize MinIO and ShareController
        MinioClient minioClient = MinioConfig.getClient();
        this.shareController = new ShareController(minioClient);

        // Setup search field enter key
        searchField.setOnAction(e -> handleSearch());

        // Setup user card click
        userCard.setOnMouseClicked(e -> selectUser());

        // Initial state
        showState("initial");
    }

    /**
     * Sets the file to be shared
     */
    public void setTargetFile(String filename, String bucket) {
        this.targetFilename = filename;
        this.targetBucket = bucket;
        this.fileNameLabel.setText(filename);
    }

    /**
     * Handles user search
     */
    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        
        if (query.isEmpty()) {
            AlertUtils.showError("Search Error", "Please enter a name or email to search.");
            return;
        }

        if (query.length() < 2) {
            AlertUtils.showError("Search Error", "Please enter at least 2 characters.");
            return;
        }

        // Show loading state
        showState("loading");

        // Perform search in background
        Task<List<User>> searchTask = new Task<>() {
            @Override
            protected List<User> call() {
                long currentUserId = SessionManager.getCurrentUser().getId();
                return userDAO.searchUsers(query, currentUserId);
            }
        };

        searchTask.setOnSucceeded(e -> {
            List<User> results = searchTask.getValue();
            
            if (results.isEmpty()) {
                noResultsLabel.setText("No user found with \"" + query + "\"");
                showState("no-results");
            } else {
                // Take the first result
                User user = results.get(0);
                displayUserCard(user);
                showState("found");
            }
        });

        searchTask.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            showState("no-results");
            noResultsLabel.setText("Search failed. Please try again.");
        });

        new Thread(searchTask).start();
    }

    /**
     * Displays user information in the card
     */
    private void displayUserCard(User user) {
        selectedUser = null; // Reset selection
        shareButton.setDisable(true);
        selectedIcon.setVisible(false);

        // Set user info
        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + 
                          " " + (user.getLastName() != null ? user.getLastName() : "");
        userNameLabel.setText(fullName.trim());
        userEmailLabel.setText(user.getEmail());

        // Set initials
        String initials = "";
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            initials += user.getFirstName().charAt(0);
        }
        if (user.getLastName() != null && !user.getLastName().isEmpty()) {
            initials += user.getLastName().charAt(0);
        }
        userInitialsLabel.setText(initials.toUpperCase());

        // Random color for avatar
        String[] colors = {"#3498db", "#e74c3c", "#2ecc71", "#9b59b6", "#f39c12", "#1abc9c"};
        int colorIndex = Math.abs(user.getEmail().hashCode()) % colors.length;
        userAvatarCircle.setFill(Color.web(colors[colorIndex]));

        // Store user for selection
        userCard.setUserData(user);
    }

    /**
     * Selects the displayed user
     */
    private void selectUser() {
        User user = (User) userCard.getUserData();
        if (user == null) return;

        if (selectedUser == user) {
            // Deselect
            selectedUser = null;
            selectedIcon.setVisible(false);
            shareButton.setDisable(true);
            userCard.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 15; -fx-padding: 20; " +
                              "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 8, 0, 0, 2); -fx-cursor: hand;");
        } else {
            // Select
            selectedUser = user;
            selectedIcon.setVisible(true);
            shareButton.setDisable(false);
            userCard.setStyle("-fx-background-color: #e8f6ff; -fx-background-radius: 15; -fx-padding: 20; " +
                              "-fx-effect: dropshadow(three-pass-box, rgba(52, 152, 219, 0.3), 8, 0, 0, 2); " +
                              "-fx-border-color: #3498db; -fx-border-radius: 15; -fx-border-width: 2; -fx-cursor: hand;");
        }
    }

    /**
     * Shows different UI states
     */
    private void showState(String state) {
        // Hide all containers
        loadingContainer.setVisible(false);
        noResultsContainer.setVisible(false);
        userCardContainer.setVisible(false);
        initialContainer.setVisible(false);

        // Show the requested state with fade animation
        VBox targetContainer = switch (state) {
            case "loading" -> loadingContainer;
            case "no-results" -> noResultsContainer;
            case "found" -> userCardContainer;
            default -> initialContainer;
        };

        targetContainer.setVisible(true);
        
        // Add fade-in animation
        FadeTransition fade = new FadeTransition(Duration.millis(200), targetContainer);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    /**
     * Handles the share action
     */
    @FXML
    private void handleShare() {
        if (selectedUser == null) {
            AlertUtils.showError("Selection Required", "Please select a user to share with.");
            return;
        }

        // Disable button during operation
        shareButton.setDisable(true);
        shareButton.setText("Sharing...");

        Task<String> shareTask = new Task<>() {
            @Override
            protected String call() {
                long senderId = SessionManager.getCurrentUser().getId();
                return shareController.shareFileByName(targetFilename, senderId, selectedUser.getEmail());
            }
        };

        shareTask.setOnSucceeded(e -> {
            String result = shareTask.getValue();
            if ("SUCCESS".equals(result)) {
                AlertUtils.showSuccess("File Shared", 
                    targetFilename + " has been shared with " + selectedUser.getFirstName());
                closeWindow();
            } else {
                shareButton.setDisable(false);
                shareButton.setText("Share File");
                AlertUtils.showError("Share Failed", result);
            }
        });

        shareTask.setOnFailed(e -> {
            shareButton.setDisable(false);
            shareButton.setText("Share File");
            AlertUtils.showError("Share Failed", "Could not share file. Please try again.");
        });

        new Thread(shareTask).start();
    }

    /**
     * Handles cancel action
     */
    @FXML
    private void handleCancel() {
        closeWindow();
    }

    /**
     * Closes the dialog window
     */
    private void closeWindow() {
        Stage stage = (Stage) searchField.getScene().getWindow();
        stage.close();
    }
}
