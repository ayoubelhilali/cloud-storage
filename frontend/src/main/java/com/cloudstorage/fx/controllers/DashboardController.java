package com.cloudstorage.fx.controllers;

import com.cloudstorage.config.SessionManager;
import com.cloudstorage.model.User;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

public class DashboardController {

    @FXML private Label userLabel;
    @FXML private VBox filesContainer;

    // Main layout containers
    @FXML private BorderPane mainBorderPane;
    @FXML private VBox dashboardView; // Make sure your Dashboard.fxml has this ID on the center VBox

    // Sidebar Buttons
    @FXML private Button btnMyCloud;
    @FXML private Button btnSharedFiles;
    @FXML private Button btnFavorites;
    @FXML private Button btnUpload;
    @FXML private Button btnSettings;
    @FXML private Button logoutButton;

    // View Caching
    private Parent sharedFilesView;
    private Parent uploadFilesView;
    private Parent favoritesView;
    private Parent settingsView;

    // MinIO Data
    private MinioClient minioClient;
    private String currentUserBucket;

    @FXML
    public void initialize() {
        setActiveButton(btnMyCloud);
        this.minioClient = com.cloudstorage.config.MinioConfig.getClient();
    }

    // --- USER SETUP ---
    public void setUsername(String fullName) {
        String[] parts = fullName.trim().split(" ", 2);
        String fName = parts[0];
        String lName = parts.length > 1 ? parts[1] : "";
        User tempUser = new User(0L, fName.toLowerCase(), "temp@email.com", "", fName, lName);
        setUserData(tempUser);
    }

    public void setUserData(User user) {
        if (user == null) return;
        SessionManager.login(user);
        userLabel.setText("Hello, " + user.getFirstName());

        String fName = (user.getFirstName() != null) ? user.getFirstName() : "user";
        String lName = (user.getLastName() != null) ? user.getLastName() : "default";
        this.currentUserBucket = (fName + "-" + lName).toLowerCase().replaceAll("[^a-z0-9-]", "");

        loadUserFiles();
    }

    // --- FILE LOADING & UI ---
    private void loadUserFiles() {
        if (minioClient == null || currentUserBucket == null) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Check if bucket exists
                if (minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(currentUserBucket).build())) {
                    Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder().bucket(currentUserBucket).build());
                    List<Item> items = new ArrayList<>();
                    for (Result<Item> res : results) items.add(res.get());

                    // Sort Newest First
                    items.sort((i1, i2) -> i2.lastModified().compareTo(i1.lastModified()));

                    Platform.runLater(() -> {
                        filesContainer.getChildren().clear();
                        for (Item item : items) filesContainer.getChildren().add(createFileRow(item));
                    });
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private HBox createFileRow(Item item) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(60.0);
        row.setSpacing(15.0);
        row.setPadding(new Insets(0, 15, 0, 15));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 5;");
        row.getStyleClass().add("recent-item");

        Label icon = new Label("📄");
        icon.getStyleClass().add("recent-icon");

        VBox nameBox = new VBox();
        nameBox.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(item.objectName());
        name.setPrefWidth(200);
        name.getStyleClass().add("recent-name");
        nameBox.getChildren().add(name);

        Label size = new Label(formatSize(item.size()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // SHARE BUTTON
        Button shareBtn = new Button("Share");
        shareBtn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-cursor: hand;");
        shareBtn.setOnAction(e -> openShareDialog(item.objectName()));

        row.getChildren().addAll(icon, nameBox, spacer, size, shareBtn);

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#0000000d"));
        row.setEffect(shadow);

        return row;
    }

    private void openShareDialog(String filename) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/ShareFileDialog.fxml"));
            Parent root = loader.load();

            ShareDialogController controller = loader.getController();
            // Using SessionManager to get current ID
            long userId = (SessionManager.getCurrentUser() != null) ? SessionManager.getCurrentUser().getId() : 0;
            controller.setShareData(filename, userId, new Stage());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Share File");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- NAVIGATION HANDLERS ---

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
            // Always reload to get fresh data
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/SharedFiles.fxml"));
            sharedFilesView = loader.load();

            SharedDashboardController controller = loader.getController();
            controller.loadSharedFiles();

            mainBorderPane.setCenter(sharedFilesView);
        } catch (IOException e) {
            e.printStackTrace();
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
        }
    }

    @FXML
    private void handleUpload() {
        setActiveButton(btnUpload);
        try {
            if (uploadFilesView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/UploadFiles.fxml"));
                uploadFilesView = loader.load();

                // Link refresh logic
                UploadFilesController controller = loader.getController();
                controller.setOnUploadComplete(() -> {
                    System.out.println("Refreshing file list...");
                    loadUserFiles();
                });
            }
            mainBorderPane.setCenter(uploadFilesView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSettings() {
        setActiveButton(btnSettings);
        try {
            if (settingsView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/Settings.fxml"));
                settingsView = loader.load();
            }
            mainBorderPane.setCenter(settingsView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            Preferences.userNodeForPackage(DashboardController.class).clear();
            SessionManager.logout();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Cloud Storage - Login");
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- HELPERS ---

    private String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private void setActiveButton(Button activeButton) {
        List<Button> buttons = Arrays.asList(btnMyCloud, btnSharedFiles, btnFavorites, btnUpload, btnSettings);
        for (Button btn : buttons) {
            btn.getStyleClass().remove("nav-button-active");
            if (!btn.getStyleClass().contains("nav-button")) btn.getStyleClass().add("nav-button");
        }
        activeButton.getStyleClass().add("nav-button-active");
    }
}