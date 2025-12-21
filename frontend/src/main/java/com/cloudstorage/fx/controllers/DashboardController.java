package com.cloudstorage.fx.controllers;

import com.cloudstorage.config.MinioConfig;
import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.fx.components.FileRowFactory;
import com.cloudstorage.fx.service.FileApiService;
import com.cloudstorage.fx.utils.AlertUtils;
import com.cloudstorage.fx.utils.AvatarCache;
import com.cloudstorage.fx.utils.FileUtils;
import com.cloudstorage.model.User;
import io.minio.BucketExistsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.prefs.Preferences;

public class DashboardController {

    // --- FXML UI Components ---
    @FXML private Label userLabel;
    @FXML private VBox filesContainer;
    @FXML private HBox foldersContainer;
    @FXML private BorderPane mainBorderPane;
    @FXML private VBox dashboardView;
    @FXML private Circle userPicture;

    // Sidebar Buttons
    @FXML private Button btnMyCloud, btnSharedFiles, btnFavorites, btnUpload, btnSettings, logoutButton;

    // Stats Labels
    @FXML private Label lblTotalFiles, lblImageCount, lblVideoCount, lblDocCount, lblAudioCount;
    @FXML private Label usedStorageCount, sizeLeftPercent;
    @FXML private ProgressBar sizeProgressBar;

    // --- Services & State ---
    private MinioClient minioClient;
    private String currentUserBucket;
    private Long currentFolderId = null; // Used for filtering files by folder

    // View Cache
    private Parent sharedFilesView, uploadFilesView, settingsView, favoritesView;
    private SettingsController settingsController;

    @FXML
    public void initialize() {
        setActiveButton(btnMyCloud);
        this.minioClient = MinioConfig.getClient();
    }

    /**
     * Entry point for setting user data and starting UI sync
     */
    public void setUserData(User user) {
        if (user == null) return;

        SessionManager.login(user);
        userLabel.setText("Hello, " + user.getFirstName());

        // Resolve bucket naming logic
        if (user.getBucketName() != null && !user.getBucketName().isEmpty()) {
            this.currentUserBucket = user.getBucketName();
        } else {
            this.currentUserBucket = (user.getFirstName() + "-" + user.getLastName())
                    .toLowerCase()
                    .replaceAll("[^a-z0-9-]", "");
        }

        // Load visual components
        loadUserAvatar(user.getAvatarUrl());
        loadUserFolders();
        loadUserFiles();
    }

    // =================================================================================
    // SECTION 1: AVATAR LOGIC (With Cache & Placeholder)
    // =================================================================================

    private void loadUserAvatar(String avatarKey) {
        if (userPicture == null) return;

        // Step 1: Check cache for instant load
        if (avatarKey != null && AvatarCache.contains(avatarKey)) {
            Image cached = AvatarCache.get(avatarKey);
            if (cached != null && !cached.isError()) {
                userPicture.setFill(new javafx.scene.paint.ImagePattern(cached));
                return;
            }
        }

        // Step 2: Set Placeholder
        try {
            InputStream is = getClass().getResourceAsStream("/images/default-profile.jpg");
            if (is != null) {
                userPicture.setFill(new javafx.scene.paint.ImagePattern(new Image(is)));
            } else {
                userPicture.setFill(Color.web("#0061FF"));
            }
        } catch (Exception e) {
            userPicture.setFill(Color.LIGHTGRAY);
        }

        if (avatarKey == null || avatarKey.isEmpty() || minioClient == null) return;

        // Step 3: Async Fetch from Minio
        new Thread(() -> {
            try {
                String signedUrl = minioClient.getPresignedObjectUrl(
                        io.minio.GetPresignedObjectUrlArgs.builder()
                                .method(io.minio.http.Method.GET)
                                .bucket(this.currentUserBucket)
                                .object(avatarKey)
                                .expiry(24, java.util.concurrent.TimeUnit.HOURS)
                                .build());

                Image image = new Image(signedUrl, 200, 200, true, true, true);
                image.progressProperty().addListener((obs, oldV, progress) -> {
                    if (progress.doubleValue() >= 1.0 && !image.isError()) {
                        Platform.runLater(() -> {
                            AvatarCache.put(avatarKey, image);
                            userPicture.setFill(new javafx.scene.paint.ImagePattern(image));
                        });
                    }
                });
            } catch (Exception e) {
                System.err.println("Avatar load failed: " + e.getMessage());
            }
        }).start();
    }

    public void refreshAvatar(String avatarKey) {
        AvatarCache.remove(avatarKey);
        loadUserAvatar(avatarKey);
    }

    // =================================================================================
    // SECTION 2: DYNAMIC FOLDER LOGIC (Database Driven)
    // =================================================================================

    public void loadUserFolders() {
        User user = SessionManager.getCurrentUser();
        if (user == null || foldersContainer == null) return;

        Task<List<Map<String, Object>>> fetchFoldersTask = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() throws Exception {
                return FileDAO.getFoldersByUserId(user.getId());
            }
        };

        fetchFoldersTask.setOnSucceeded(e -> {
            List<Map<String, Object>> folders = fetchFoldersTask.getValue();
            int childCount = foldersContainer.getChildren().size();
            if (childCount > 1) {
                foldersContainer.getChildren().remove(0, childCount - 1);
            }

            for (Map<String, Object> folderData : folders) {
                VBox folderUI = createFolderComponent(
                        (Long) folderData.get("id"),
                        (String) folderData.get("name"),
                        folderData.get("file_count") + " files"
                );
                foldersContainer.getChildren().add(foldersContainer.getChildren().size() - 1, folderUI);
            }
        });

        new Thread(fetchFoldersTask).start();
    }

    private VBox createFolderComponent(Long id, String name, String count) {
        VBox folder = new VBox();
        folder.getStyleClass().add("file-folder");
        folder.setPadding(new javafx.geometry.Insets(15));
        folder.setPrefHeight(100);
        folder.setMinWidth(100);
        HBox.setHgrow(folder, javafx.scene.layout.Priority.ALWAYS);

        Label icon = new Label("📁");
        icon.getStyleClass().add("folder-icon");
        icon.setTextFill(Color.web(getRandomFolderColor()));

        Label nameLbl = new Label(name);
        nameLbl.getStyleClass().add("folder-name");

        Label countLbl = new Label(count);
        countLbl.getStyleClass().add("folder-count");

        folder.getChildren().addAll(icon, nameLbl, countLbl);
        folder.setEffect(new javafx.scene.effect.DropShadow(3.0, Color.web("#0000000d")));

        folder.setOnMouseClicked(e -> {
            this.currentFolderId = id;
            loadUserFiles(); // Filter by this folder
            foldersContainer.getChildren().forEach(n -> n.getStyleClass().remove("folder-active"));
            folder.getStyleClass().add("folder-active");
        });

        return folder;
    }

    @FXML
    private void handleAddFolder() {
        TextInputDialog dialog = new TextInputDialog("New Folder");
        dialog.showAndWait().ifPresent(name -> {
            if (name.trim().isEmpty()) return;
            new Thread(() -> {
                if (FileDAO.createFolder(name.trim(), SessionManager.getCurrentUser().getId())) {
                    Platform.runLater(this::loadUserFolders);
                }
            }).start();
        });
    }

    private String getRandomFolderColor() {
        String[] colors = {"#6c5ce7", "#00b894", "#e84393", "#0984e3", "#f1c40f"};
        return colors[new Random().nextInt(colors.length)];
    }

    // =================================================================================
    // SECTION 3: FILE LOADING & SYNC
    // =================================================================================

    private void loadUserFiles() {
        if (minioClient == null || currentUserBucket == null) return;

        Task<List<Map<String, String>>> fetchTask = new Task<>() {
            @Override
            protected List<Map<String, String>> call() throws Exception {
                Iterable<Result<Item>> results = minioClient.listObjects(
                        ListObjectsArgs.builder().bucket(currentUserBucket).build());

                List<Map<String, String>> uiDataList = new ArrayList<>();
                long userId = SessionManager.getCurrentUser().getId();
                List<String> myFavorites = FileDAO.getFavoriteFilenames(userId);

                for (Result<Item> result : results) {
                    Item item = result.get();
                    if (item.isDir()) continue;

                    Map<String, String> fileData = new HashMap<>();
                    fileData.put("name", item.objectName());
                    fileData.put("size", String.valueOf(item.size()));
                    fileData.put("bucket", currentUserBucket);
                    fileData.put("is_favorite", String.valueOf(myFavorites.contains(item.objectName())));
                    uiDataList.add(fileData);
                }
                return uiDataList;
            }
        };

        fetchTask.setOnSucceeded(e -> {
            filesContainer.getChildren().clear();
            for (Map<String, String> data : fetchTask.getValue()) {
                // Pass refresh callback (loadUserFolders) to factory
                HBox row = FileRowFactory.createRow(data, this::handleFileClick, this::loadUserFolders);
                filesContainer.getChildren().add(row);
            }
            updateStatistics(fetchTask.getValue());
        });

        new Thread(fetchTask).start();
    }

    private void updateStatistics(List<Map<String, String>> files) {
        double totalMB = 0;
        for (Map<String, String> file : files) {
            totalMB += FileUtils.parseSizeToMB(file.get("size"));
        }
        if (usedStorageCount != null)
            usedStorageCount.setText(String.format("%.2f MB Used of 5GB", totalMB));
        if (sizeProgressBar != null)
            sizeProgressBar.setProgress(totalMB / 5120.0);
    }

    private void handleFileClick(Map<String, String> fileData) {
        // Implementation for preview popup
    }

    // =================================================================================
    // SECTION 4: NAVIGATION & SETTINGS
    // =================================================================================

    @FXML
    public void handleShowMyCloud() {
        setActiveButton(btnMyCloud);
        this.currentFolderId = null;
        mainBorderPane.setCenter(dashboardView);
        loadUserFiles();
        loadUserFolders();
    }

    @FXML
    private void handleSettings() {
        setActiveButton(btnSettings);
        try {
            if (settingsView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/SettingsDashboard.fxml"));
                settingsView = loader.load();
                settingsController = loader.getController();
                if (settingsController != null) settingsController.setDashboardController(this);
            }
            mainBorderPane.setCenter(settingsView);
            Platform.runLater(() -> settingsController.refresh());
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void setActiveButton(Button active) {
        List<Button> btns = Arrays.asList(btnMyCloud, btnSharedFiles, btnFavorites, btnUpload, btnSettings);
        btns.forEach(b -> b.getStyleClass().remove("nav-button-active"));
        active.getStyleClass().add("nav-button-active");
    }

    @FXML
    private void handleShowSharedFiles() {
        setActiveButton(btnSharedFiles);
        try {
            if (sharedFilesView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/SharedFiles.fxml"));
                sharedFilesView = loader.load();
                // Optional: link controller if needed
            }
            mainBorderPane.setCenter(sharedFilesView);
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtils.showError("Navigation Error", "Could not load Shared Files view.");
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
                Object controller = loader.getController();
                // This ensures that when an upload finishes, it returns to the main view
                if (controller instanceof UploadFilesController) {
                    ((UploadFilesController) controller).setOnUploadComplete(this::handleShowMyCloud);
                }
            }
            mainBorderPane.setCenter(uploadFilesView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            // 1. Clear saved login preferences (node for this class)
            Preferences.userNodeForPackage(DashboardController.class).clear();

            // 2. Clear the current session in SessionManager
            SessionManager.logout();

            // 3. Navigate back to the Login screen
            openScene("/com/cloudstorage/fx/Login.fxml", "Login");

        } catch (Exception e) {
            System.err.println("Logout failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper method to switch scenes by loading a new FXML file
     */
    private void openScene(String path, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();

            // Get the current stage using any visible node (the logout button)
            Stage stage = (Stage) logoutButton.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            System.err.println("Error opening scene: " + path);
            e.printStackTrace();
        }
    }
}
