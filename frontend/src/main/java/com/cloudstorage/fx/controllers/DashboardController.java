package com.cloudstorage.fx.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class DashboardController {

    @FXML private Label userLabel;
    @FXML private Parent favoritesView;

    // Sidebar Buttons
    @FXML private Button btnMyCloud;
    @FXML private Button btnSharedFiles;
    @FXML private Button btnFavorites;
    @FXML private Button btnUpload;
    @FXML private Button btnSettings;
    @FXML private Button logoutButton;

    // Layout Containers
    @FXML private BorderPane mainBorderPane;
    @FXML private VBox dashboardView; // "My Cloud" default view

    // --- VIEW CACHE (Prevents reloading FXML every time) ---
    private Parent sharedFilesView;
    private Parent uploadFilesView; // <--- NEW: Cache for Upload View

    @FXML
    public void initialize() {
        setActiveButton(btnMyCloud);
    }

    private void setActiveButton(Button activeButton) {
        List<Button> buttons = Arrays.asList(btnMyCloud, btnSharedFiles, btnFavorites, btnUpload, btnSettings);

        for (Button btn : buttons) {
            btn.getStyleClass().remove("nav-button-active");
            if (!btn.getStyleClass().contains("nav-button")) {
                btn.getStyleClass().add("nav-button");
            }
        }
        activeButton.getStyleClass().add("nav-button-active");
    }

    public void setUsername(String name) {
        userLabel.setText("Hello, " + name);
    }

    // --- NAVIGATION METHODS ---

    @FXML
    private void handleShowMyCloud() {
        setActiveButton(btnMyCloud);
        if (dashboardView != null) {
            mainBorderPane.setCenter(dashboardView);
        }
    }

    @FXML
    private void handleShowSharedFiles() {
        setActiveButton(btnSharedFiles);
        try {
            if (sharedFilesView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/SharedFiles.fxml"));
                sharedFilesView = loader.load();
            }
            mainBorderPane.setCenter(sharedFilesView);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading SharedFiles.fxml");
        }
    }

    // --- UPDATED UPLOAD HANDLER ---
    @FXML
    private void handleUpload() {
        setActiveButton(btnUpload);
        try {
            // Load the FXML only if we haven't already
            if (uploadFilesView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/UploadFiles.fxml"));
                uploadFilesView = loader.load();
            }
            // Switch the center view
            mainBorderPane.setCenter(uploadFilesView);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading UploadFiles.fxml. Check the file path.");
        }
    }

    @FXML
    private void handleSettings() {
        setActiveButton(btnSettings);
        // Currently just reloading dashboard, change this when you have a Settings.fxml
        if (dashboardView != null) {
            mainBorderPane.setCenter(dashboardView);
        }
    }

    @FXML
    private void handleLogout() {
        try {
            // Swap Scenes
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Cloud Storage - Login");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error: Could not load Login.fxml. Check the file path.");
        }
    }
    @FXML
    private void handleShowFavorites() {
        setActiveButton(btnFavorites);
        try {
            if (favoritesView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/Favorites.fxml"));
                favoritesView = loader.load();
            }
            mainBorderPane.setCenter(favoritesView);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading Favorites.fxml");
        }
    }
}