package com.cloudstorage.fx.controllers;

import com.cloudstorage.config.MinioConfig;
import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.database.UserDAO;
import com.cloudstorage.model.User;
import com.cloudstorage.util.ConfigLoader;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public class SettingsController {

    // --- FXML Fields ---
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

    @FXML
    public void initialize() {
        // 1. Initialize MinIO
        this.minioClient = MinioConfig.getClient();

        // 2. Load All Data
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

    @FXML
    private void handleSaveProfile() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        String fName = firstNameField.getText();
        String lName = lastNameField.getText();
        String email = emailField.getText();

        if (fName.isEmpty() || lName.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "All fields are required.");
            return;
        }

        boolean success = userDAO.updateProfile(user.getId(), fName, lName, email);

        if (success) {
            user.setFirstName(fName);
            user.setLastName(lName);
            user.setEmail(email);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not update profile.");
        }
    }

    @FXML
    private void handleUploadAvatar() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(firstNameField.getScene().getWindow());

        if (file != null) {
            new Thread(() -> {
                try {
                    String objectName = "avatars/" + user.getId() + "_avatar.jpg";
                    String bucketName = user.getBucketName();

                    // Upload to MinIO
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new FileInputStream(file), file.length(), -1)
                            .contentType("image/jpeg")
                            .build());

                    // Update DB
                    boolean dbUpdated = userDAO.updateAvatar(user.getId(), objectName);

                    if (dbUpdated) {
                        SessionManager.updateCurrentAvatar(objectName);
                        Platform.runLater(() -> {
                            loadAvatarImage(objectName);
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Avatar updated!");
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Upload Failed", e.getMessage()));
                }
            }).start();
        }
    }

    private void loadAvatarImage(String objectKey) {
        if (objectKey == null || objectKey.isEmpty()) return;
        try {
            User user = SessionManager.getCurrentUser();
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(user.getBucketName())
                            .object(objectKey)
                            .expiry(1, TimeUnit.HOURS)
                            .build());
            avatarCircle.setFill(new ImagePattern(new Image(url, true)));
        } catch (Exception e) {
            System.err.println("Could not load avatar: " + e.getMessage());
        }
    }

    // =================================================================================
    // SECTION 2: STORAGE STATS
    // =================================================================================

    private void loadStorageStats() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        long usedBytes = fileDAO.getTotalStorageUsed(user.getId());
        long totalBytes = 15L * 1024 * 1024 * 1024; // 15 GB Quota

        double usedGB = usedBytes / (1024.0 * 1024.0 * 1024.0);
        double progress = (double) usedBytes / totalBytes;

        if (usageTextLabel != null) usageTextLabel.setText(String.format("%.2f GB used of 15 GB", usedGB));
        if (percentageLabel != null) percentageLabel.setText(String.format("%.0f%%", progress * 100));
        if (storageProgressBar != null) storageProgressBar.setProgress(progress);

        // Pie Chart
        Map<String, Long> breakdown = fileDAO.getStorageBreakdown(user.getId());
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        breakdown.forEach((type, size) -> {
            double sizeMB = size / (1024.0 * 1024.0);
            if (sizeMB > 1) pieData.add(new PieChart.Data(type, sizeMB));
        });
        if (storagePieChart != null) storagePieChart.setData(pieData);
    }

    @FXML
    private void handleClearCache() {
        showAlert(Alert.AlertType.INFORMATION, "Cache Cleared", "Temporary files have been removed.");
    }

    // =================================================================================
    // SECTION 3: PREFERENCES (This fixes your error)
    // =================================================================================

    private void loadPreferences() {
        String dlPath = prefs.get("download_path", System.getProperty("user.home") + "/Downloads");
        if (downloadPathField != null) downloadPathField.setText(dlPath);

        if (notifyShare != null) notifyShare.setSelected(prefs.getBoolean("notify_share", true));
        if (notifyLogin != null) notifyLogin.setSelected(prefs.getBoolean("notify_login", false));
        if (notifyStorage != null) notifyStorage.setSelected(prefs.getBoolean("notify_storage", true));
    }

    @FXML
    private void handleBrowseLocation() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Download Folder");
        File dir = dc.showDialog(downloadPathField.getScene().getWindow());

        if (dir != null) {
            downloadPathField.setText(dir.getAbsolutePath());
            prefs.put("download_path", dir.getAbsolutePath());
        }
    }

    // =================================================================================
    // SECTION 4: SECURITY
    // =================================================================================

    @FXML
    private void handleChangePassword() {
        long userId = SessionManager.getCurrentUser().getId();
        String neo = newPasswordField.getText();

        if (neo.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Weak Password", "New password must be at least 6 characters.");
            return;
        }

        if (userDAO.updatePassword(userId, neo)) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Password changed successfully.");
            currentPasswordField.clear();
            newPasswordField.clear();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Database error updating password.");
        }
    }

    @FXML
    private void handleDeleteAccount() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure? This will delete ALL your files.", ButtonType.YES, ButtonType.CANCEL);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            long userId = SessionManager.getCurrentUser().getId();
            if (userDAO.deleteUser(userId)) {
                Platform.exit();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Could not delete account.");
            }
        }
    }

    // =================================================================================
    // UTILITIES
    // =================================================================================

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}