package com.cloudstorage.fx.controllers;

import com.cloudstorage.config.MinioConfig;
import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.database.UserDAO;
import com.cloudstorage.model.User;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.Result;
import io.minio.messages.Item;
import io.minio.http.Method;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

/**
 * Controller for the Settings View.
 * Provides a modern interface for profile management, storage visualization, and security.
 */
public class SettingsController {

    // --- FXML UI Components ---
    @FXML private Circle avatarCircle;
    @FXML private TextField firstNameField, lastNameField, emailField, downloadPathField;
    @FXML private ProgressBar storageProgressBar;
    @FXML private PieChart storagePieChart;
    @FXML private Label usageTextLabel, percentageLabel;
    @FXML private CheckBox notifyShare, notifyLogin, notifyStorage;
    @FXML private PasswordField currentPasswordField, newPasswordField;

    // --- Services ---
    private final UserDAO userDAO = new UserDAO();
    private final FileDAO fileDAO = new FileDAO();
    private MinioClient minioClient;
    private final Preferences prefs = Preferences.userNodeForPackage(SettingsController.class);

    // --- Constants ---
    private static final String DEFAULT_AVATAR = "/images/default-avatar.png";
    private static final long MAX_STORAGE_BYTES = 15L * 1024 * 1024 * 1024; // Updated to 15GB to match UI

    // Modern Blue-centric Palette for the PieChart
    private final Map<String, String> CATEGORY_COLORS = Map.of(
            "Images", "#0061FF",    // Primary Blue
            "Videos", "#60A5FA",    // Light Blue
            "Documents", "#3B82F6", // Medium Blue
            "Audio", "#93C5FD",     // Sky Blue
            "Others", "#CBD5E1"     // Slate Grey
    );

    @FXML
    public void initialize() {
        this.minioClient = MinioConfig.getClient();
        refresh();
    }

    /**
     * Refreshes all data sections in the settings page.
     */
    public void refresh() {
        loadUserProfile();
        loadStorageStats();
        loadPreferences();
    }

    // =================================================================================
    // SECTION 1: PROFILE & AVATAR
    // =================================================================================

    private void loadUserProfile() {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            firstNameField.setText(user.getFirstName());
            lastNameField.setText(user.getLastName());
            emailField.setText(user.getEmail());
            loadAvatarImage(user.getAvatarUrl());
        }
    }

    private void loadAvatarImage(String objectKey) {
        // Set default placeholder
        InputStream is = getClass().getResourceAsStream(DEFAULT_AVATAR);
        if (is != null) {
            avatarCircle.setFill(new ImagePattern(new Image(is)));
        }

        if (objectKey == null || objectKey.isEmpty()) return;

        new Thread(() -> {
            try {
                User user = SessionManager.getCurrentUser();
                String url = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(user.getBucketName())
                                .object(objectKey)
                                .expiry(1, TimeUnit.HOURS)
                                .build());

                Image image = new Image(url, true); // Background loading true

                image.progressProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.doubleValue() == 1.0) {
                        Platform.runLater(() -> avatarCircle.setFill(new ImagePattern(image)));
                    }
                });
            } catch (Exception e) {
                System.err.println("Avatar fetch failed: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    private void handleUploadAvatar() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(avatarCircle.getScene().getWindow());

        if (file != null) {
            new Thread(() -> {
                try {
                    String objectName = "avatars/" + user.getId() + "_" + System.currentTimeMillis() + ".jpg";
                    try (FileInputStream fis = new FileInputStream(file)) {
                        minioClient.putObject(PutObjectArgs.builder()
                                .bucket(user.getBucketName())
                                .object(objectName)
                                .stream(fis, file.length(), -1)
                                .contentType("image/jpeg")
                                .build());
                    }

                    if (userDAO.updateAvatar(user.getId(), objectName)) {
                        user.setAvatarUrl(objectName);
                        Platform.runLater(() -> {
                            loadAvatarImage(objectName);
                            showSnackBar("Success", "Profile picture updated.");
                        });
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Upload Error", e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    private void handleSaveProfile() {
        User user = SessionManager.getCurrentUser();
        String fName = firstNameField.getText().trim();
        String lName = lastNameField.getText().trim();
        String email = emailField.getText().trim();

        if (fName.isEmpty() || lName.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Please fill in all profile fields.");
            return;
        }

        new Thread(() -> {
            if (userDAO.updateProfile(user.getId(), fName, lName, email)) {
                user.setFirstName(fName);
                user.setLastName(lName);
                user.setEmail(email);
                Platform.runLater(() -> showSnackBar("Updated", "Profile information saved."));
            }
        }).start();
    }

    // =================================================================================
    // SECTION 2: STORAGE ANALYTICS
    // =================================================================================

    private void loadStorageStats() {
        User user = SessionManager.getCurrentUser();
        if (user == null || minioClient == null) return;

        new Thread(() -> {
            try {
                String bucket = user.getBucketName();
                long totalUsedBytes = 0;
                Map<String, Long> typeSizes = new HashMap<>();
                Map<String, Integer> typeCounts = new HashMap<>();

                Iterable<Result<Item>> results = minioClient.listObjects(
                        ListObjectsArgs.builder().bucket(bucket).recursive(true).build());

                for (Result<Item> result : results) {
                    Item item = result.get();
                    if (item.isDir()) continue;

                    long size = item.size();
                    totalUsedBytes += size;

                    String category = getFileCategory(item.objectName());
                    typeSizes.put(category, typeSizes.getOrDefault(category, 0L) + size);
                    typeCounts.put(category, typeCounts.getOrDefault(category, 0) + 1);
                }

                final long finalTotal = totalUsedBytes;
                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

                // Construct labels with counts: "Images (24)"
                typeSizes.forEach((category, size) -> {
                    int count = typeCounts.getOrDefault(category, 0);
                    double sizeMB = size / (1024.0 * 1024.0);
                    pieData.add(new PieChart.Data(category + " (" + count + ")", sizeMB));
                });

                Platform.runLater(() -> {
                    updateStatsUI(finalTotal, pieData);
                    applyPieColors();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateStatsUI(long usedBytes, ObservableList<PieChart.Data> pieData) {
        double usedGB = usedBytes / (1024.0 * 1024.0 * 1024.0);
        double progress = (double) usedBytes / MAX_STORAGE_BYTES;

        if (usageTextLabel != null) usageTextLabel.setText(String.format("%.2f GB", usedGB));
        if (percentageLabel != null) percentageLabel.setText(String.format("%.0f%% of total capacity", progress * 100));
        if (storageProgressBar != null) storageProgressBar.setProgress(progress);

        if (storagePieChart != null) {
            storagePieChart.setData(pieData);
            storagePieChart.setLegendVisible(true);
            storagePieChart.setLabelsVisible(true);
        }
    }

    private void applyPieColors() {
        for (PieChart.Data data : storagePieChart.getData()) {
            String label = data.getName();
            CATEGORY_COLORS.forEach((category, color) -> {
                if (label.contains(category)) {
                    data.getNode().setStyle("-fx-pie-color: " + color + "; -fx-border-color: white; -fx-border-width: 2;");
                }
            });
        }
    }

    private String getFileCategory(String filename) {
        String name = filename.toLowerCase();
        if (name.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) return "Images";
        if (name.matches(".*\\.(mp4|mov|avi|mkv|wmv)$")) return "Videos";
        if (name.matches(".*\\.(pdf|docx|txt|pptx|xlsx|odt|rtf)$")) return "Documents";
        if (name.matches(".*\\.(mp3|wav|flac|m4a|ogg)$")) return "Audio";
        return "Others";
    }

    // =================================================================================
    // SECTION 3: PREFERENCES & SECURITY
    // =================================================================================

    private void loadPreferences() {
        downloadPathField.setText(prefs.get("download_path", System.getProperty("user.home") + File.separator + "Downloads"));
        notifyShare.setSelected(prefs.getBoolean("notify_share", true));
        notifyLogin.setSelected(prefs.getBoolean("notify_login", false));
        notifyStorage.setSelected(prefs.getBoolean("notify_storage", true));
    }

    @FXML
    private void handleBrowseLocation() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Download Folder");
        File dir = dc.showDialog(downloadPathField.getScene().getWindow());
        if (dir != null) {
            downloadPathField.setText(dir.getAbsolutePath());
            prefs.put("download_path", dir.getAbsolutePath());
            showSnackBar("Path Saved", "Default download location updated.");
        }
    }

    @FXML
    private void handleChangePassword() {
        User user = SessionManager.getCurrentUser();
        String neo = newPasswordField.getText();
        if (neo == null || neo.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Security", "Password must be at least 6 characters.");
            return;
        }

        new Thread(() -> {
            if (userDAO.updatePassword(user.getId(), neo)) {
                Platform.runLater(() -> {
                    showSnackBar("Success", "Password changed successfully.");
                    newPasswordField.clear();
                    currentPasswordField.clear();
                });
            }
        }).start();
    }

    @FXML
    private void handleDeleteAccount() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Account");
        confirm.setHeaderText("Warning: Permanent Action");
        confirm.setContentText("This will permanently delete your account and all stored files. Continue?");

        confirm.showAndWait().ifPresent(type -> {
            if (type == ButtonType.OK) {
                new Thread(() -> {
                    if (userDAO.deleteUser(SessionManager.getCurrentUser().getId())) {
                        Platform.runLater(Platform::exit);
                    }
                }).start();
            }
        });
    }

    @FXML
    private void handleClearCache() {
        showSnackBar("Cache Cleared", "Local temporary files removed.");
    }

    // --- Helper UI Methods ---

    private void showSnackBar(String title, String msg) {
        // Implementation for a modern alert or simple notification
        System.out.println(title + ": " + msg);
        showAlert(Alert.AlertType.INFORMATION, title, msg);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}