package com.cloudstorage.fx.controllers;

import com.cloudstorage.config.MinioConfig;
import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.fx.components.FileRowFactory;
import com.cloudstorage.fx.service.FileApiService;
import com.cloudstorage.fx.utils.FileUtils;
import com.cloudstorage.model.User;
import io.minio.BucketExistsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
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
import java.time.ZonedDateTime; // Import needed for the fix
import java.util.*;
import java.util.prefs.Preferences;

public class DashboardController {

    @FXML private Label userLabel;
    @FXML private VBox filesContainer;
    @FXML private BorderPane mainBorderPane;
    @FXML private VBox dashboardView;
    @FXML private Circle userPicture;

    // Sidebar Buttons
    @FXML private Button btnMyCloud;
    @FXML private Button btnSharedFiles;
    @FXML private Button btnFavorites;
    @FXML private Button btnUpload;
    @FXML private Button btnSettings;
    @FXML private Button logoutButton;

    // Stats Labels
    @FXML private Label lblTotalFiles;
    @FXML private Label lblImageCount;
    @FXML private Label lblVideoCount;
    @FXML private Label lblDocCount;
    @FXML private Label lblAudioCount;
    @FXML private Label usedStorageCount;
    @FXML private Label sizeLeftPercent;
    @FXML private ProgressBar sizeProgressBar;

    // Services
    private final FileApiService fileService = new FileApiService();
    private MinioClient minioClient;
    private String currentUserBucket;

    // View Cache
    private Parent sharedFilesView;
    private SharedDashboardController sharedFilesController;
    private Parent uploadFilesView;
    private Parent settingsView;
    private Parent favoritesView;
    private FavoritesController favoritesController;

    @FXML
    public void initialize() {
        setActiveButton(btnMyCloud);
        this.minioClient = MinioConfig.getClient();
    }

    public void setUserData(User user) {
        if (user == null) return;
        SessionManager.login(user);
        userLabel.setText("Hello, " + user.getFirstName());

        if (user.getBucketName() != null && !user.getBucketName().isEmpty()) {
            this.currentUserBucket = user.getBucketName();
        } else {
            this.currentUserBucket = (user.getFirstName() + "-" + user.getLastName())
                    .toLowerCase().replaceAll("[^a-z0-9-]", "");
        }
        System.out.println("Dashboard Bucket: " + this.currentUserBucket);
        loadUserFiles();
    }

    // --- MAIN FILE LOADING LOGIC (FIXED) ---
    private void loadUserFiles() {
        if (minioClient == null || currentUserBucket == null) return;

        Task<List<Map<String, String>>> fetchTask = new Task<>() {
            @Override
            protected List<Map<String, String>> call() throws Exception {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(currentUserBucket).build()
                );

                List<Map<String, String>> uiDataList = new ArrayList<>();

                if (exists) {
                    Iterable<Result<Item>> results = minioClient.listObjects(
                            ListObjectsArgs.builder().bucket(currentUserBucket).build());

                    List<Item> allItems = new ArrayList<>();
                    for (Result<Item> result : results) {
                        allItems.add(result.get());
                    }

                    // --- FIX: Null-Safe Sorting ---
                    // Some items (like folders) might have null lastModified dates.
                    allItems.sort((item1, item2) -> {
                        ZonedDateTime d1 = null;
                        ZonedDateTime d2 = null;

                        // Safely get dates, catching any potential internal nulls
                        try { d1 = item1.lastModified(); } catch (Exception ignored) {}
                        try { d2 = item2.lastModified(); } catch (Exception ignored) {}

                        if (d1 == null && d2 == null) return 0;
                        if (d1 == null) return 1;  // Nulls go to bottom
                        if (d2 == null) return -1; // Nulls go to bottom

                        return d2.compareTo(d1); // Newest First
                    });
                    // ------------------------------

                    long userId = SessionManager.getCurrentUser().getId();
                    List<String> myFavorites = FileDAO.getFavoriteFilenames(userId);

                    for (Item item : allItems) {
                        // Skip folders or weird items with 0 size and no date
                        if (item.isDir()) continue;

                        Map<String, String> fileData = new HashMap<>();
                        fileData.put("name", item.objectName());
                        fileData.put("size", String.valueOf(item.size()));
                        fileData.put("bucket", currentUserBucket);

                        if (myFavorites.contains(item.objectName())) {
                            fileData.put("is_favorite", "true");
                        } else {
                            fileData.put("is_favorite", "false");
                        }
                        uiDataList.add(fileData);
                    }
                }
                return uiDataList;
            }
        };

        fetchTask.setOnSucceeded(e -> {
            List<Map<String, String>> files = fetchTask.getValue();
            filesContainer.getChildren().clear();

            if (files.isEmpty()) {
                Label emptyLabel = new Label("No files found.");
                emptyLabel.setStyle("-fx-text-fill: gray; -fx-padding: 20;");
                filesContainer.getChildren().add(emptyLabel);
            } else {
                for (Map<String, String> fileData : files) {
                    HBox row = FileRowFactory.createRow(fileData, this::handleFileClick);
                    filesContainer.getChildren().add(row);
                }
            }
            updateStatistics(files);
        });

        fetchTask.setOnFailed(e -> {
            System.err.println("Error fetching files:");
            e.getSource().getException().printStackTrace();
        });

        new Thread(fetchTask).start();
    }

    private void updateStatistics(List<Map<String, String>> files) {
        double totalStorageUsed = 0.0;
        int images = 0, videos = 0, docs = 0, audio = 0, others = 0;

        for (Map<String, String> file : files) {
            String sizeStr = file.get("size");
            String name = file.get("name");
            String ext = FileUtils.getFileExtension(name);

            totalStorageUsed += FileUtils.parseSizeToMB(sizeStr);

            if (FileUtils.isImage(ext)) images++;
            else if (FileUtils.isVideo(ext)) videos++;
            else if (FileUtils.isDocument(ext)) docs++;
            else if (FileUtils.isAudio(ext)) audio++;
            else others++;
        }

        double sizeLeftPercentVal = FileUtils.sizeLeftPercent(totalStorageUsed);

        if (lblTotalFiles != null) lblTotalFiles.setText(files.size() + " Files");
        if (lblImageCount != null) lblImageCount.setText(images + " Images");
        if (lblVideoCount != null) lblVideoCount.setText(videos + " Videos");
        if (lblDocCount != null) lblDocCount.setText(docs + " Documents");
        if (lblAudioCount != null) lblAudioCount.setText(audio + " Audio");

        if (usedStorageCount != null)
            usedStorageCount.setText(String.format("%.2f MB Used of 5GB", totalStorageUsed));

        if (sizeLeftPercent != null)
            sizeLeftPercent.setText(String.format("%d%% Left", (int) sizeLeftPercentVal));

        if (sizeProgressBar != null)
            sizeProgressBar.setProgress((100.0 - sizeLeftPercentVal) / 100.0);
    }

    private void handleFileClick(Map<String, String> fileData) {
        String name = fileData.get("name");
        String ext = FileUtils.getFileExtension(name);

        if (FileUtils.isImage(ext)) {
            String safeName = name.replaceAll(" ", "%20");
            // NOTE: In production, generate a real presigned URL here using minioClient
            String imageUrl = "http://localhost:9000/" + currentUserBucket + "/" + safeName;

            Image image = new Image(imageUrl, true);
            showPopup(image, name, fileData.get("size"));
        } else {
            showPopup(null, name, fileData.get("size"));
        }
    }

    private void showPopup(Image image, String fileName, String size) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/ImagePreview.fxml"));
            Parent root = loader.load();

            Object controllerObj = loader.getController();
            if (controllerObj instanceof com.cloudstorage.fx.controllers.ImagePreviewController) {
                var controller = (com.cloudstorage.fx.controllers.ImagePreviewController) controllerObj;
                String details = "Size: " + FileUtils.formatSize(size);
                controller.setDeleteContext(this.minioClient, this.currentUserBucket, this::loadUserFiles);
                controller.setFileData(image, fileName, details);
            }

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
            System.err.println("Error loading ImagePreview.fxml");
            ex.printStackTrace();
        }
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

    @FXML
    private void handleShowMyCloud() {
        setActiveButton(btnMyCloud);
        if (dashboardView != null) {
            mainBorderPane.setCenter(dashboardView);
            loadUserFiles();
        }
    }

    @FXML
    private void handleShowSharedFiles() {
        setActiveButton(btnSharedFiles);
        try {
            if (sharedFilesView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/SharedFiles.fxml"));
                sharedFilesView = loader.load();
                Object controller = loader.getController();
                if (controller instanceof SharedDashboardController) {
                    sharedFilesController = (SharedDashboardController) controller;
                }
            } else if (sharedFilesController != null) {
                sharedFilesController.loadSharedFiles();
            }
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
                favoritesController = loader.getController();
            } else {
                favoritesController.refresh();
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/UploadFiles.fxml"));
            uploadFilesView = loader.load();
            Object controller = loader.getController();
            if (controller instanceof UploadFilesController) {
                ((UploadFilesController) controller).setOnUploadComplete(this::handleShowMyCloud);
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
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/SettingsDashboard.fxml"));
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
            openScene("/com/cloudstorage/fx/Login.fxml", "Login");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void openScene(String path, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}