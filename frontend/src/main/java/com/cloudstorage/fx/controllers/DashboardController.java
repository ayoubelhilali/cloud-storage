package com.cloudstorage.fx.controllers;

import com.cloudstorage.config.MinioConfig; // Ensure this import exists
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
import javafx.application.Platform;
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
    private MinioClient minioClient; // Added MinioClient field
    private String currentUserBucket;

    // View Cache
    private Parent sharedFilesView;
    private Parent uploadFilesView;
    private Parent settingsView;
    private Parent favoritesView;
    private FavoritesController favoritesController; // Add this!

    @FXML
    public void initialize() {
        setActiveButton(btnMyCloud);
        // Initialize MinIO Client using your Config class
        this.minioClient = MinioConfig.getClient();
    }

    public void setUserData(User user) {
        if (user == null) return;
        SessionManager.login(user);
        userLabel.setText("Hello, " + user.getFirstName());

        // Ensure bucket name is set (fallback to username logic if null)
        if (user.getBucketName() != null && !user.getBucketName().isEmpty()) {
            this.currentUserBucket = user.getBucketName();
        } else {
            // Fallback generation if needed
            this.currentUserBucket = (user.getFirstName() + "-" + user.getLastName())
                    .toLowerCase().replaceAll("[^a-z0-9-]", "");
        }

        System.out.println("Dashboard Bucket: " + this.currentUserBucket);
        loadUserFiles();
    }

    // --- MAIN FILE LOADING LOGIC ---
    private void loadUserFiles() {
        // 1. Safety Checks
        if (minioClient == null || currentUserBucket == null) return;

        Task<List<Map<String, String>>> fetchTask = new Task<>() {
            @Override
            protected List<Map<String, String>> call() throws Exception {
                // Check if bucket exists
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(currentUserBucket).build()
                );

                List<Map<String, String>> uiDataList = new ArrayList<>();

                if (exists) {
                    // 2. Fetch ALL Files from MinIO
                    Iterable<Result<Item>> results = minioClient.listObjects(
                            ListObjectsArgs.builder().bucket(currentUserBucket).build());

                    List<Item> allItems = new ArrayList<>();
                    for (Result<Item> result : results) {
                        allItems.add(result.get());
                    }

                    // 3. SORT by Date (Newest First)
                    allItems.sort((item1, item2) ->
                            item2.lastModified().compareTo(item1.lastModified())
                    );

                    // 4. FETCH FAVORITES from Database
                    long userId = SessionManager.getCurrentUser().getId();
                    List<String> myFavorites = FileDAO.getFavoriteFilenames(userId);

                    // 5. Build the Data Maps
                    for (Item item : allItems) {
                        Map<String, String> fileData = new HashMap<>();
                        fileData.put("name", item.objectName());
                        fileData.put("size", String.valueOf(item.size()));
                        fileData.put("bucket", currentUserBucket);

                        // MERGE: Check DB status
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

            // Clear UI
            filesContainer.getChildren().clear();

            if (files.isEmpty()) {
                Label emptyLabel = new Label("No files found.");
                emptyLabel.setStyle("-fx-text-fill: gray; -fx-padding: 20;");
                filesContainer.getChildren().add(emptyLabel);
            } else {
                // Add Rows
                for (Map<String, String> fileData : files) {
                    // Connect the row click to 'handleFileClick'
                    HBox row = FileRowFactory.createRow(fileData, this::handleFileClick);
                    filesContainer.getChildren().add(row);
                }
            }

            // Update Stats Bar
            updateStatistics(files);
        });

        fetchTask.setOnFailed(e -> {
            System.err.println("Error fetching files:");
            e.getSource().getException().printStackTrace();
        });

        new Thread(fetchTask).start();
    }

    // --- STATISTICS LOGIC ---
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

        // UI Update
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

    // --- CLICK HANDLER (Preview) ---
    private void handleFileClick(Map<String, String> fileData) {
        String name = fileData.get("name");
        String ext = FileUtils.getFileExtension(name);

        // Determine if we can preview it as an image
        if (FileUtils.isImage(ext)) {
            // Construct MinIO URL (Assuming local MinIO setup)
            // Ideally, generate a presigned URL using minioClient.getPresignedObjectUrl(...)
            String safeName = name.replaceAll(" ", "%20");

            // For now, using direct local link logic (Update if you use Presigned URLs)
            String imageUrl = "http://localhost:8080/files?bucket=" + currentUserBucket + "&key=" + safeName;

            // Try to use MinIO stream if the URL approach is flaky
            // But for this code, we pass the URL to the Image constructor
            Image image = new Image(imageUrl, true);
            showPopup(image, name, fileData.get("size"));
        } else {
            // Non-image file
            showPopup(null, name, fileData.get("size"));
        }
    }

    private void showPopup(Image image, String fileName, String size) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/ImagePreview.fxml"));
            Parent root = loader.load();

            // Use reflection or cast to get your specific controller
            // Assuming your controller class is ImagePreviewController
            Object controllerObj = loader.getController();

            // We use reflection/dynamic checking to avoid import errors if you named it differently
            if (controllerObj instanceof com.cloudstorage.fx.controllers.ImagePreviewController) {
                var controller = (com.cloudstorage.fx.controllers.ImagePreviewController) controllerObj;

                String details = "Size: " + FileUtils.formatSize(size);

                // Pass context for Deletion
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

    // --- NAVIGATION METHODS ---

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
            loadUserFiles(); // Refresh list when returning
        }
    }

    @FXML
    private void handleShowSharedFiles() {
        setActiveButton(btnSharedFiles);
        loadView("/com/cloudstorage/fx/SharedFiles.fxml");
    }

    @FXML
    private void handleUpload() {
        setActiveButton(btnUpload);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/UploadFiles.fxml"));
            Parent view = loader.load();

            Object controller = loader.getController();
            // Check specific class name
            if (controller instanceof UploadFilesController) {
                UploadFilesController uploadController = (UploadFilesController) controller;
                uploadController.setOnUploadComplete(() -> {
                    System.out.println("Upload finished. Refreshing list...");
                    loadUserFiles();
                });
            }
            mainBorderPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSettings() {
        setActiveButton(btnSettings);
        loadView("/com/cloudstorage/fx/Settings.fxml");
    }

    @FXML
    private void handleShowFavorites() {
        setActiveButton(btnFavorites);

        try {
            if (favoritesView == null) {
                // 1. First time loading: Load from Disk
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/Favorites.fxml"));
                favoritesView = loader.load();

                // 2. Save the controller for later use
                favoritesController = loader.getController();
            } else {
                // 3. Subsequent times: Just REFRESH the data (Don't reload FXML)
                // This makes the UI switch INSTANT, and the images populate quickly after
                favoritesController.refresh();
            }

            // 4. Show the cached view
            mainBorderPane.setCenter(favoritesView);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not load Favorites view.");
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

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            mainBorderPane.setCenter(view);
        } catch (IOException e) {
            System.err.println("Could not load FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }
}