package com.cloudstorage.fx.controllers.views;

import com.cloudstorage.config.MinioConfig;
import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.database.NotificationDAO;
import com.cloudstorage.database.ShareDAO;
import com.cloudstorage.fx.components.FileRowFactory;
import com.cloudstorage.fx.components.NotificationPanelController;
import com.cloudstorage.fx.controllers.dialogs.AddFolderDialogController;
import com.cloudstorage.fx.utils.AlertUtils;
import com.cloudstorage.fx.utils.AvatarCache;
import com.cloudstorage.fx.utils.FileUtils;
import com.cloudstorage.model.User;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
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
    @FXML private VBox sharedFoldersContainer;

    // Notification Components
    @FXML private Button notificationBtn;
    @FXML private Label notificationBadge;

    // Notification popup state
    private Stage currentNotificationPopup = null;

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
    private Timeline notificationBadgeRefreshTimer;

    // View Cache
    private Parent sharedFilesView, uploadFilesView, settingsView, favoritesView;
    private SettingsController settingsController;

    // Colors for shared folder cards
    private static final String[] FOLDER_COLORS = {
        "#dff9fb", "#e0c3fc", "#c7ecee", "#ffeaa7", "#fab1a0", "#81ecec", "#dfe6e9"
    };

    @FXML
    public void initialize() {
        setActiveButton(btnMyCloud);
        this.minioClient = MinioConfig.getClient();
        updateNotificationBadge();
        startNotificationBadgeAutoRefresh();
    }

    /**
     * Starts auto-refresh for notification badge every 15 seconds
     */
    private void startNotificationBadgeAutoRefresh() {
        notificationBadgeRefreshTimer = new Timeline(new KeyFrame(Duration.seconds(15), event -> {
            updateNotificationBadge();
        }));
        notificationBadgeRefreshTimer.setCycleCount(Animation.INDEFINITE);
        notificationBadgeRefreshTimer.play();
    }

    // =================================================================================
    // LOADER METHODS
    // =================================================================================

    private void showLoader() {
        if (filesContainer != null) {
            Platform.runLater(() -> {
                filesContainer.getChildren().clear();
                ProgressIndicator spinner = new ProgressIndicator();
                spinner.setMaxSize(40, 40);
                VBox loaderBox = new VBox(10, spinner, new Label("Loading files..."));
                loaderBox.setAlignment(Pos.CENTER);
                loaderBox.setPadding(new Insets(20));
                loaderBox.setStyle("-fx-text-fill: #64748B;");
                filesContainer.getChildren().add(loaderBox);
            });
        }
    }

    private void hideLoader() {
        // Loader will be cleared when files are loaded
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

        // Show loader and load visual components
        showLoader();
        loadUserAvatar(user.getAvatarUrl());
        loadUserFolders();
        loadUserFiles();
        loadSharedFolders();
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

    // =================================================================================
    // SECTION 2.5: DYNAMIC SHARED FOLDERS (Right Panel)
    // =================================================================================

    /**
     * Loads shared folders dynamically with collaborator avatars
     */
    private void loadSharedFolders() {
        User user = SessionManager.getCurrentUser();
        if (user == null || sharedFoldersContainer == null) return;

        Task<List<Map<String, Object>>> fetchTask = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() throws Exception {
                return ShareDAO.getSharedFoldersWithCollaborators(user.getId());
            }
        };

        fetchTask.setOnSucceeded(e -> {
            sharedFoldersContainer.getChildren().clear();
            List<Map<String, Object>> sharedFolders = fetchTask.getValue();

            if (sharedFolders.isEmpty()) {
                Label emptyLabel = new Label("No shared folders yet");
                emptyLabel.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 13px;");
                sharedFoldersContainer.getChildren().add(emptyLabel);
                return;
            }

            // Display up to 3 shared folders
            int count = Math.min(sharedFolders.size(), 3);
            for (int i = 0; i < count; i++) {
                Map<String, Object> folderData = sharedFolders.get(i);
                HBox folderCard = createSharedFolderCard(folderData, i);
                sharedFoldersContainer.getChildren().add(folderCard);
            }
        });

        fetchTask.setOnFailed(e -> {
            System.err.println("Failed to load shared folders: " + fetchTask.getException().getMessage());
        });

        new Thread(fetchTask).start();
    }

    /**
     * Creates a shared folder card with collaborator avatars
     */
    @SuppressWarnings("unchecked")
    private HBox createSharedFolderCard(Map<String, Object> folderData, int index) {
        HBox card = new HBox();
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefHeight(50);
        card.getStyleClass().addAll("shared-item");
        card.setStyle("-fx-background-color: " + FOLDER_COLORS[index % FOLDER_COLORS.length] + "; -fx-background-radius: 12; -fx-cursor: hand;");
        card.setPadding(new Insets(0, 15, 0, 15));

        // Folder name
        String folderName = (String) folderData.getOrDefault("name", "Shared Folder");
        Label nameLabel = new Label(folderName);
        nameLabel.getStyleClass().add("shared-item-text");
        nameLabel.setMaxWidth(120);
        
        // Tooltip with full name
        Tooltip nameTip = new Tooltip(folderName);
        nameTip.getStyleClass().add("modern-tooltip");
        Tooltip.install(nameLabel, nameTip);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Collaborator avatars
        HBox avatarsBox = new HBox(-8); // Negative spacing for overlap
        avatarsBox.setAlignment(Pos.CENTER_RIGHT);

        List<Map<String, String>> collaborators = (List<Map<String, String>>) folderData.getOrDefault("collaborators", new ArrayList<>());
        int displayCount = Math.min(collaborators.size(), 3);

        for (int i = 0; i < displayCount; i++) {
            Map<String, String> collab = collaborators.get(i);
            Circle avatar = createCollaboratorAvatar(collab, i);
            avatarsBox.getChildren().add(avatar);
        }

        // If more collaborators, show +N badge
        if (collaborators.size() > 3) {
            Label countBadge = new Label("+" + (collaborators.size() - 3));
            countBadge.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 50; -fx-padding: 3 6; -fx-font-size: 10px; -fx-text-fill: #555; -fx-font-weight: bold;");
            HBox.setMargin(countBadge, new Insets(0, 0, 0, 5));
            avatarsBox.getChildren().add(countBadge);
        }

        card.getChildren().addAll(nameLabel, spacer, avatarsBox);

        // Click to navigate to shared files
        card.setOnMouseClicked(e -> handleShowSharedFiles());

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: derive(" + FOLDER_COLORS[index % FOLDER_COLORS.length] + ", -10%); -fx-background-radius: 12; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: " + FOLDER_COLORS[index % FOLDER_COLORS.length] + "; -fx-background-radius: 12; -fx-cursor: hand;"));

        return card;
    }

    /**
     * Creates a circular avatar for a collaborator
     */
    private Circle createCollaboratorAvatar(Map<String, String> collab, int index) {
        Circle avatar = new Circle(14);
        avatar.setStroke(Color.WHITE);
        avatar.setStrokeWidth(2);
        avatar.getStyleClass().add("collaborator-avatar");

        String firstName = collab.getOrDefault("firstName", "U");
        String lastName = collab.getOrDefault("lastName", "");
        
        // Use initials as fallback color
        String[] colors = {"#6c5ce7", "#00b894", "#e84393", "#0984e3", "#f1c40f", "#e74c3c"};
        avatar.setFill(Color.web(colors[index % colors.length]));

        // Try to load avatar image if available
        String avatarUrl = collab.get("avatarUrl");
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            try {
                Image img = new Image("https://i.pravatar.cc/50?img=" + (index + 10), true);
                img.progressProperty().addListener((obs, old, progress) -> {
                    if (progress.doubleValue() >= 1.0 && !img.isError()) {
                        Platform.runLater(() -> avatar.setFill(new ImagePattern(img)));
                    }
                });
            } catch (Exception ignored) {}
        }

        // Tooltip with name
        Tooltip tip = new Tooltip(firstName + " " + lastName);
        tip.getStyleClass().add("modern-tooltip");
        Tooltip.install(avatar, tip);

        return avatar;
    }

    /**
     * Handler for "View All Shared" button
     */
    @FXML
    private void handleViewAllShared() {
        handleShowSharedFiles();
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

        // Ouvrir la vue du dossier au clic
        folder.setOnMouseClicked(e -> openFolderView(id, name));

        return folder;
    }

    /**
     * Ouvre l'interface dédiée d'un dossier
     */
    private void openFolderView(Long folderId, String folderName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/views/FolderView.fxml"));
            Parent folderView = loader.load();

            FolderViewController controller = loader.getController();
            controller.setFolder(folderId, folderName, currentUserBucket);
            controller.setOnBackCallback(v -> {
                // Retour au dashboard principal
                mainBorderPane.setCenter(dashboardView);
                loadUserFolders(); // Rafraîchir les dossiers
                loadUserFiles();   // Rafraîchir les fichiers
            });

            mainBorderPane.setCenter(folderView);

        } catch (IOException ex) {
            ex.printStackTrace();
            AlertUtils.showError("Navigation Error", "Could not open folder view.");
        }
    }

    @FXML
    private void handleAddFolder() {
        try {
            // Load the modern Add Folder Dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/dialogs/AddFolderDialog.fxml"));
            Parent root = loader.load();

            AddFolderDialogController controller = loader.getController();

            // Create and configure the stage
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            dialogStage.setTitle("Create New Folder");

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            scene.getStylesheets().add(getClass().getResource("/css/dialogs.css").toExternalForm());

            dialogStage.setScene(scene);
            controller.setStage(dialogStage);
            controller.setOnSuccess(this::loadUserFolders);

            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtils.showError("Dialog Error", "Could not open folder creation dialog.");
        }
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
                // Pass refresh callback to reload files after operations (delete, move to folder)
                HBox row = FileRowFactory.createRow(data, this::handleFileClick, this::loadUserFiles);
                filesContainer.getChildren().add(row);
            }
            updateStatistics(fetchTask.getValue());
            hideLoader(); // Hide loader after files are loaded
        });

        fetchTask.setOnFailed(e -> {
            hideLoader(); // Hide loader even if loading fails
        });

        new Thread(fetchTask).start();
    }

    /**
     * Updates all dashboard statistics dynamically:
     * - Total storage used (in MB/GB)
     * - Storage progress bar
     * - Percentage used
     * - File counts by category
     * - Storage size by category
     */
    private void updateStatistics(List<Map<String, String>> files) {
        // Storage capacity in bytes (5 GB)
        final long STORAGE_CAPACITY_BYTES = 5L * 1024 * 1024 * 1024;
        
        // Counters
        long totalBytes = 0;
        long imageBytes = 0;
        long videoBytes = 0;
        long audioBytes = 0;
        long docBytes = 0;
        
        int imageCount = 0;
        int videoCount = 0;
        int audioCount = 0;
        int docCount = 0;
        
        for (Map<String, String> file : files) {
            // Parse file size from bytes string
            long fileSize = 0;
            try {
                String sizeStr = file.get("size");
                if (sizeStr != null && !sizeStr.isEmpty()) {
                    fileSize = Long.parseLong(sizeStr);
                }
            } catch (NumberFormatException e) {
                // Try parsing from formatted size
                fileSize = (long)(FileUtils.parseSizeToMB(file.get("size")) * 1024 * 1024);
            }
            
            totalBytes += fileSize;
            
            // Categorize by file extension
            String ext = FileUtils.getFileExtension(file.get("name"));
            if (FileUtils.isImage(ext)) {
                imageCount++;
                imageBytes += fileSize;
            } else if (FileUtils.isVideo(ext)) {
                videoCount++;
                videoBytes += fileSize;
            } else if (FileUtils.isAudio(ext)) {
                audioCount++;
                audioBytes += fileSize;
            } else {
                docCount++;
                docBytes += fileSize;
            }
        }
        
        // Calculate storage values
        double usedGB = totalBytes / (1024.0 * 1024.0 * 1024.0);
        double usedMB = totalBytes / (1024.0 * 1024.0);
        double percentUsed = (totalBytes * 100.0) / STORAGE_CAPACITY_BYTES;
        double progress = totalBytes / (double) STORAGE_CAPACITY_BYTES;
        
        // ⚠️ Check storage usage and send notifications
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            int percentUsedInt = (int) percentUsed;
            if (percentUsedInt >= 95) {
                // Storage almost full
                com.cloudstorage.util.NotificationHelper.notifyStorageFull(currentUser.getId());
            } else if (percentUsedInt >= 80) {
                // Storage warning
                com.cloudstorage.util.NotificationHelper.notifyStorageLow(currentUser.getId(), percentUsedInt);
            }
        }

        // Update storage labels
        if (usedStorageCount != null) {
            if (usedGB >= 1.0) {
                usedStorageCount.setText(String.format("%.2f GB Used of 5 GB", usedGB));
            } else {
                usedStorageCount.setText(String.format("%.2f MB Used of 5 GB", usedMB));
            }
        }
        
        if (sizeLeftPercent != null) {
            sizeLeftPercent.setText(String.format("%.1f%% used", percentUsed));
        }
        
        if (sizeProgressBar != null) {
            sizeProgressBar.setProgress(Math.min(progress, 1.0));
        }
        
        // Update category counters (badges)
        if (lblImageCount != null) lblImageCount.setText(String.valueOf(imageCount));
        if (lblVideoCount != null) lblVideoCount.setText(String.valueOf(videoCount));
        if (lblAudioCount != null) lblAudioCount.setText(String.valueOf(audioCount));
        if (lblDocCount != null) lblDocCount.setText(String.valueOf(docCount));
        
        // Update total files count
        if (lblTotalFiles != null) {
            lblTotalFiles.setText(String.valueOf(files.size()));
        }
    }
    
    /**
     * Refreshes the dashboard after file operations (upload, delete, etc.)
     * Called from other controllers to update statistics.
     */
    public void refreshDashboard() {
        loadUserFiles();
        loadUserFolders();
    }

    /**
     * Gère le clic sur un fichier pour afficher la prévisualisation média
     * Supporte : Images, PDF, Vidéos, Audio et autres documents
     */
    private void handleFileClick(Map<String, String> fileData) {
        String fileName = fileData.get("name");
        String bucket = fileData.get("bucket");
        
        if (fileName == null || bucket == null || minioClient == null) {
            AlertUtils.showError("Erreur", "Impossible d'ouvrir le fichier.");
            return;
        }
        
        // Utiliser le nouveau dialogue de prévisualisation média
        com.cloudstorage.fx.components.MediaPreviewDialog.showPreview(fileName, bucket, minioClient);
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
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/views/SettingsDashboard.fxml"));
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
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/views/SharedFiles.fxml"));
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
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/views/Favorites.fxml"));
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
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/views/UploadFiles.fxml"));
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

    // =================================================================================
    // NOTIFICATION SYSTEM
    // =================================================================================

    /**
     * Updates the notification badge count
     */
    public void updateNotificationBadge() {
        User user = SessionManager.getCurrentUser();
        if (user == null || notificationBadge == null) return;

        new Thread(() -> {
            int unreadCount = NotificationDAO.getUnreadCount(user.getId());

            Platform.runLater(() -> {
                if (unreadCount > 0) {
                    notificationBadge.setText(String.valueOf(unreadCount));
                    notificationBadge.setVisible(true);
                } else {
                    notificationBadge.setVisible(false);
                }
            });
        }).start();
    }

    /**
     * Shows the notification panel
     */
    @FXML
    private void handleShowNotifications() {
        // If popup is already open, close it (toggle behavior)
        if (currentNotificationPopup != null && currentNotificationPopup.isShowing()) {
            currentNotificationPopup.close();
            currentNotificationPopup = null;
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/cloudstorage/fx/components/NotificationPanel.fxml")
            );
            Parent notificationPanel = loader.load();

            NotificationPanelController controller = loader.getController();

            // Create popup stage
            Stage popup = new Stage();
            popup.initStyle(StageStyle.TRANSPARENT);
            popup.initModality(Modality.NONE);
            popup.initOwner(notificationBtn.getScene().getWindow());

            Scene scene = new Scene(notificationPanel);
            scene.setFill(Color.TRANSPARENT);
            popup.setScene(scene);

            // Store reference to current popup
            currentNotificationPopup = popup;

            // Set close callback
            controller.setOnClose(v -> {
                controller.stopAutoRefresh();
                popup.close();
                currentNotificationPopup = null;
            });

            // Set notification read callback to update badge
            controller.setOnNotificationRead(this::updateNotificationBadge);

            // Add listener to close popup when clicking outside
            Stage ownerStage = (Stage) notificationBtn.getScene().getWindow();
            ownerStage.getScene().addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
                if (currentNotificationPopup != null && currentNotificationPopup.isShowing()) {
                    // Check if click is outside the notification panel
                    javafx.geometry.Bounds popupBounds = notificationPanel.localToScreen(notificationPanel.getBoundsInLocal());

                    double mouseX = event.getScreenX();
                    double mouseY = event.getScreenY();

                    // Check if click is outside popup bounds
                    if (popupBounds == null ||
                        mouseX < popupBounds.getMinX() || mouseX > popupBounds.getMaxX() ||
                        mouseY < popupBounds.getMinY() || mouseY > popupBounds.getMaxY()) {

                        // Don't close if clicking the notification button itself (handled by toggle)
                        javafx.geometry.Bounds buttonBounds = notificationBtn.localToScreen(notificationBtn.getBoundsInLocal());
                        if (buttonBounds != null &&
                            !(mouseX >= buttonBounds.getMinX() && mouseX <= buttonBounds.getMaxX() &&
                              mouseY >= buttonBounds.getMinY() && mouseY <= buttonBounds.getMaxY())) {

                            controller.stopAutoRefresh();
                            currentNotificationPopup.close();
                            currentNotificationPopup = null;
                        }
                    }
                }
            });

            // Clear popup reference when closed
            popup.setOnHidden(e -> {
                currentNotificationPopup = null;
                controller.stopAutoRefresh();
            });

            // Position popup directly below and aligned with the notification button
            javafx.geometry.Bounds buttonBounds = notificationBtn.localToScreen(notificationBtn.getBoundsInLocal());

            // Calculate position:
            // X: Align right edge of panel with right edge of button
            // Y: Position just below the button with a small gap
            double panelWidth = 420; // Match FXML prefWidth
            double xPos = buttonBounds.getMaxX() - panelWidth;
            double yPos = buttonBounds.getMaxY() + 10; // 10px gap

            popup.setX(xPos);
            popup.setY(yPos);

            popup.show();

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("Error", "Could not open notifications: " + e.getMessage());
        }
    }
}
