package com.cloudstorage.fx.controllers;

import com.cloudstorage.config.SessionManager; // IMPORTED
import com.cloudstorage.model.User;            // IMPORTED
import io.minio.GetObjectArgs;
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
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

public class DashboardController {

    @FXML private Label userLabel;
    @FXML private VBox filesContainer;

    // Sidebar Buttons
    @FXML private Button btnMyCloud;
    @FXML private Button btnSharedFiles;
    @FXML private Button btnFavorites;
    @FXML private Button btnUpload;
    @FXML private Button btnSettings;
    @FXML private Button logoutButton;

    // Layout Containers
    @FXML private BorderPane mainBorderPane;
    @FXML private VBox dashboardView;

    // Cache for Views
    private Parent sharedFilesView;
    private Parent uploadFilesView;
    private Parent favoritesView;
    private Parent settingsView;

    // MinIO Configuration
    private MinioClient minioClient;
    private String currentUserBucket;

    @FXML
    public void initialize() {
        setActiveButton(btnMyCloud);
        // Use the centralized MinioConfig
        this.minioClient = com.cloudstorage.config.MinioConfig.getClient();
    }

    // --- USER & MINIO SETUP ---

    // 1. COMPATIBILITY FIX: ADDED setUsername BACK
    // This handles calls from other controllers (like Register) that still pass a String
    public void setUsername(String fullName) {
        String[] parts = fullName.trim().split(" ", 2);
        String fName = parts[0];
        String lName = parts.length > 1 ? parts[1] : "";

        // Create a temporary User object to satisfy the new logic.
        // Note: ID is 0, so uploads might fail if you use this method.
        // You should update RegisterController to pass a real User object later.
        User tempUser = new User(0L, fName.toLowerCase(), "temp@email.com", "", fName, lName);

        setUserData(tempUser);
    }

    // 2. PRIMARY METHOD: Accept Full User Object
    public void setUserData(User user) {
        if (user == null) return;

        // Force SessionManager to hold this user
        SessionManager.login(user);

        // Set UI Text
        userLabel.setText("Hello, " + user.getFirstName());

        // Generate Bucket Name (Sanitized)
        String fName = (user.getFirstName() != null) ? user.getFirstName() : "user";
        String lName = (user.getLastName() != null) ? user.getLastName() : "default";

        this.currentUserBucket = (fName + "-" + lName).toLowerCase().replaceAll("[^a-z0-9-]", "");

        System.out.println("Dashboard Loaded for: " + user.getUsername());
        System.out.println("Target Bucket: " + this.currentUserBucket);

        // Load Files
        loadUserFiles();
    }

    private void loadUserFiles() {
        if (minioClient == null || currentUserBucket == null) return;

        Task<Void> fetchTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Check if bucket exists first
                boolean exists = minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(currentUserBucket).build());

                if (exists) {
                    // 1. Fetch all items
                    Iterable<Result<Item>> results = minioClient.listObjects(
                            ListObjectsArgs.builder().bucket(currentUserBucket).build());

                    List<Item> allItems = new ArrayList<>();

                    // 2. Extract items from Result wrapper
                    for (Result<Item> result : results) {
                        allItems.add(result.get());
                    }

                    // 3. Sort by Time: Descending (Newest file first)
                    // We compare item2 against item1 to get descending order
                    allItems.sort((item1, item2) ->
                            item2.lastModified().compareTo(item1.lastModified())
                    );

                    // 4. Update UI
                    Platform.runLater(() -> {
                        filesContainer.getChildren().clear(); // Clear old list to avoid duplicates

                        for (Item item : allItems) {
                            HBox fileRow = createFileRow(item);
                            // Simply add to the container, as the list is already sorted Newest -> Oldest
                            filesContainer.getChildren().add(fileRow);
                        }
                    });
                }
                return null;
            }
        };

        fetchTask.setOnFailed(e -> {
            System.err.println("Error fetching files:");
            e.getSource().getException().printStackTrace();
        });

        new Thread(fetchTask).start();
    }

    private HBox createFileRow(Item item) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(65.0);
        row.setSpacing(15.0);
        row.getStyleClass().add("recent-item");
        row.setPadding(new Insets(0, 15, 0, 15));

        // --- CLICK HANDLER ---
        row.setOnMouseClicked(event -> {
            String ext = getFileExtension(item.objectName());
            if (isImage(ext)) {
                displayImage(item);
            } else {
                showPopup(null, item);
            }
        });

        Label icon = new Label();
        icon.getStyleClass().add("recent-icon");
        String extension = getFileExtension(item.objectName());

        if (isImage(extension)) {
            icon.setText("📷");
            icon.getStyleClass().add("icon-bg-purple");
        } else if (extension.equals("mp3") || extension.equals("wav")) {
            icon.setText("🎤");
            icon.getStyleClass().add("icon-bg-blue");
        } else if (extension.equals("mp4") || extension.equals("avi")) {
            icon.setText("🎥");
            icon.getStyleClass().add("icon-bg-pink");
        } else {
            icon.setText("📄");
            icon.getStyleClass().add("icon-bg-green");
        }

        VBox nameBox = new VBox();
        nameBox.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(item.objectName());
        nameLabel.getStyleClass().add("recent-name");
        nameBox.getChildren().add(nameLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label typeLabel = new Label(extension.toUpperCase() + " file");
        typeLabel.setPrefWidth(60.0);
        typeLabel.getStyleClass().add("recent-meta");

        Label sizeLabel = new Label(formatSize(item.size()));
        sizeLabel.setPrefWidth(60.0);
        sizeLabel.getStyleClass().add("recent-meta");

        Button linkBtn = new Button("🔗");
        linkBtn.getStyleClass().add("icon-btn-link");

        Button moreBtn = new Button("•••");
        moreBtn.getStyleClass().add("icon-btn-more");

        row.getChildren().addAll(icon, nameBox, spacer, typeLabel, sizeLabel, linkBtn, moreBtn);

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#0000000d"));
        shadow.setOffsetY(3.0);
        row.setEffect(shadow);

        return row;
    }

    private boolean isImage(String ext) {
        return ext.equals("png") || ext.equals("jpg") || ext.equals("jpeg") || ext.equals("gif");
    }

    // --- THREADED DISPLAY METHOD ---
    private void displayImage(Item item) {
        Task<Image> fetchImageTask = new Task<>() {
            @Override
            protected Image call() throws Exception {
                try (InputStream stream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(currentUserBucket)
                                .object(item.objectName())
                                .build())) {
                    return new Image(stream);
                }
            }
        };

        fetchImageTask.setOnSucceeded(e -> {
            Image loadedImage = fetchImageTask.getValue();
            showPopup(loadedImage, item);
        });

        fetchImageTask.setOnFailed(e -> {
            System.err.println("Failed to load image preview.");
            e.getSource().getException().printStackTrace();
        });

        new Thread(fetchImageTask).start();
    }

    // --- POPUP WINDOW HELPER ---
    private void showPopup(Image image, Item item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/ImagePreview.fxml"));
            Parent root = loader.load();

            ImagePreviewController controller = loader.getController();

            String details = "Size: " + formatSize(item.size());
            controller.setFileData(image, item.objectName(), details);

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initStyle(StageStyle.TRANSPARENT);

            if (mainBorderPane.getScene() != null) {
                popupStage.initOwner(mainBorderPane.getScene().getWindow());
            }

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.centerOnScreen();
            popupStage.showAndWait();

        } catch (IOException ex) {
            ex.printStackTrace();
            System.err.println("Error loading ImagePreview.fxml");
        }
    }

    private String getFileExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) return fileName.substring(i + 1).toLowerCase();
        return "file";
    }

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
            if (!btn.getStyleClass().contains("nav-button")) {
                btn.getStyleClass().add("nav-button");
            }
        }
        activeButton.getStyleClass().add("nav-button-active");
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
        }
    }

    @FXML
    private void handleUpload() {
        setActiveButton(btnUpload);
        try {
            if (uploadFilesView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/UploadFiles.fxml"));
                uploadFilesView = loader.load();

                // --- LINKING THE REFRESH LOGIC ---
                UploadFilesController uploadController = loader.getController();

                // When an upload finishes, run loadUserFiles() to refresh the list
                uploadController.setOnUploadComplete(() -> {
                    System.out.println("Upload finished signal received. Refreshing list...");

                    // 1. Refresh the file list in the background
                    loadUserFiles();

                    // Optional: If you want to automatically jump back to the "My Cloud" view:
                    // handleShowMyCloud();
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
    private void handleLogout() {
        try {
            Preferences.userNodeForPackage(DashboardController.class).clear();
            SessionManager.logout();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Cloud Storage - Login");
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}