package com.cloudstorage.fx.controllers;

import com.cloudstorage.config.MinioConfig;
import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.UserDAO;
import com.cloudstorage.fx.utils.AlertUtils;
import com.cloudstorage.fx.utils.AvatarCache;
import com.cloudstorage.model.User;
import com.cloudstorage.util.PasswordUtil;
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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public class SettingsController {

    @FXML private Circle avatarCircle;
    @FXML private TextField firstNameField, lastNameField, emailField, downloadPathField;
    @FXML private ProgressBar storageProgressBar;
    @FXML private PieChart storagePieChart;
    @FXML private Label usageTextLabel, percentageLabel;
    @FXML private CheckBox notifyShare, notifyLogin, notifyStorage;
    @FXML private PasswordField currentPasswordField, newPasswordField;

    private DashboardController dashboardController;
    private final UserDAO userDAO = new UserDAO();
    private MinioClient minioClient;
    private final Preferences prefs = Preferences.userNodeForPackage(SettingsController.class);

    private static final String DEFAULT_AVATAR = "/images/default-profile.jpg";
    private static final long MAX_STORAGE_BYTES = 15L * 1024 * 1024 * 1024;

    private final Map<String, String> CATEGORY_COLORS = Map.of(
            "Images", "#10B981",
            "Videos", "#8B5CF6",
            "Documents", "#F59E0B",
            "Audio", "#EF4444",
            "Others", "#6366F1"
    );

    @FXML
    public void initialize() {
        this.minioClient = MinioConfig.getClient();
        if (storagePieChart != null) {
            storagePieChart.setLabelsVisible(true);
            storagePieChart.setLegendVisible(true);
            // Fix size: disable auto-sizing constraints that might shrink it
            storagePieChart.setAnimated(true);
        }
    }

    public void setDashboardController(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    public void refresh() {
        loadUserProfile();
        loadStorageStats();
        loadPreferences();
    }

    private void loadUserProfile() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        firstNameField.setText(user.getFirstName());
        lastNameField.setText(user.getLastName());
        emailField.setText(user.getEmail());

        // Use a small delay to ensure the UI is rendered before filling the circle
        Platform.runLater(() -> loadAvatarImage(user.getAvatarUrl()));
    }

    private void loadAvatarImage(String objectKey) {
        if (avatarCircle == null) return;

        // 1. Instant Cache Check
        if (objectKey != null && !objectKey.isEmpty() && AvatarCache.contains(objectKey)) {
            Image cached = AvatarCache.get(objectKey);
            if (cached != null && !cached.isError()) {
                avatarCircle.setFill(new ImagePattern(cached));
                return;
            }
        }

        // 2. Set Placeholder immediately (The light blue you see in your screenshot)
        setInitialPlaceholder();

        if (objectKey == null || objectKey.isEmpty() || minioClient == null) {
            return;
        }

        // 3. Robust Async Loading
        new Thread(() -> {
            try {
                User user = SessionManager.getCurrentUser();
                String bucket = user.getBucketName();

                // LOGGING: Check these values in your console!
                System.out.println("Settings: Attempting load from Bucket: " + bucket + " | Key: " + objectKey);

                String url = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(bucket)
                                .object(objectKey)
                                .expiry(2, TimeUnit.HOURS)
                                .build());

                // backgroundLoading = true is vital to keep the UI smooth
                Image image = new Image(url, true);

                image.progressProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.doubleValue() >= 1.0) {
                        if (!image.isError()) {
                            Platform.runLater(() -> {
                                AvatarCache.put(objectKey, image);
                                avatarCircle.setFill(new ImagePattern(image));
                                System.out.println("Settings: Avatar loaded successfully.");
                            });
                        } else {
                            System.err.println("Settings: Image loading error: " + image.getException());
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("Settings: Failed to get Presigned URL: " + e.getMessage());
            }
        }).start();
    }

    private void setInitialPlaceholder() {
        try {
            InputStream is = getClass().getResourceAsStream(DEFAULT_AVATAR);
            if (is != null) {
                avatarCircle.setFill(new ImagePattern(new Image(is)));
            } else {
                // This is likely what you are seeing in the screenshot
                avatarCircle.setFill(Color.web("#EBF2FF"));
            }
        } catch (Exception e) {
            avatarCircle.setFill(Color.LIGHTGRAY);
        }
    }

    @FXML
    private void handleUploadAvatar() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(avatarCircle.getScene().getWindow());

        if (file != null) {
            new Thread(() -> {
                try {
                    String objectName = "/images/" + user.getId() + "_avatar.jpg";
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
                        AvatarCache.remove(objectName); // Force refresh
                        Platform.runLater(() -> {
                            loadAvatarImage(objectName);
                            AlertUtils.showSuccess("Success", "Avatar updated.");
                            if (dashboardController != null) {
                                dashboardController.refreshAvatar(objectName);
                                // Optional: Return to dashboard
                                dashboardController.handleShowMyCloud();
                            }
                        });
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }).start();
        }
    }

    private void loadStorageStats() {
        User user = SessionManager.getCurrentUser();
        if (user == null || minioClient == null) return;

        new Thread(() -> {
            try {
                String bucket = user.getBucketName();
                Map<String, Long> typeSizes = new HashMap<>();
                long totalUsedBytes = 0;

                Iterable<Result<Item>> results = minioClient.listObjects(
                        ListObjectsArgs.builder().bucket(bucket).recursive(true).build());

                for (Result<Item> result : results) {
                    Item item = result.get();
                    long size = item.size();
                    totalUsedBytes += size;
                    String category = getFileCategory(item.objectName());
                    typeSizes.put(category, typeSizes.getOrDefault(category, 0L) + size);
                }

                final long finalTotal = totalUsedBytes;
                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                typeSizes.forEach((cat, size) -> pieData.add(new PieChart.Data(cat, size / (1024.0 * 1024.0))));

                Platform.runLater(() -> {
                    storagePieChart.setData(pieData);
                    updateStatsUI(finalTotal);
                    applyPieColors();
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void applyPieColors() {
        for (PieChart.Data data : storagePieChart.getData()) {
            String color = CATEGORY_COLORS.getOrDefault(data.getName(), "#6366F1");
            if (data.getNode() != null) {
                data.getNode().setStyle("-fx-pie-color: " + color + "; -fx-border-color: white; -fx-border-width: 1;");
            }
        }
    }

    private void updateStatsUI(long usedBytes) {
        double usedGB = usedBytes / (1024.0 * 1024.0 * 1024.0);
        double progress = (double) usedBytes / MAX_STORAGE_BYTES;
        usageTextLabel.setText(String.format("%.2f GB Used", usedGB));
        percentageLabel.setText(String.format("%.1f%% of total", progress * 100));
        storageProgressBar.setProgress(progress);
    }

    private String getFileCategory(String filename) {
        String name = filename.toLowerCase();
        if (name.matches(".*\\.(jpg|jpeg|png|gif|webp)$")) return "Images";
        if (name.matches(".*\\.(mp4|mov|avi)$")) return "Videos";
        if (name.matches(".*\\.(pdf|docx|txt)$")) return "Documents";
        if (name.matches(".*\\.(mp3|wav)$")) return "Audio";
        return "Others";
    }

    @FXML
    private void handleSaveProfile() {
        User user = SessionManager.getCurrentUser();
        String fName = firstNameField.getText().trim();
        String lName = lastNameField.getText().trim();
        String email = emailField.getText().trim();

        if (fName.isEmpty() || lName.isEmpty() || email.isEmpty()) return;

        new Thread(() -> {
            if (userDAO.updateProfile(user.getId(), fName, lName, email)) {
                user.setFirstName(fName);
                user.setLastName(lName);
                user.setEmail(email);
                Platform.runLater(() -> {
                    AlertUtils.showSuccess("Success", "Profile updated.");
                    if (dashboardController != null) {
                        dashboardController.setUserData(user);
                        dashboardController.handleShowMyCloud();
                    }
                });
            }
        }).start();
    }

    private void loadPreferences() {
        downloadPathField.setText(prefs.get("download_path", System.getProperty("user.home") + File.separator + "Downloads"));
        notifyShare.setSelected(prefs.getBoolean("notify_share", true));
        notifyLogin.setSelected(prefs.getBoolean("notify_login", false));
        notifyStorage.setSelected(prefs.getBoolean("notify_storage", true));
    }

    @FXML private void handleBrowseLocation() {
        DirectoryChooser dc = new DirectoryChooser();
        File dir = dc.showDialog(downloadPathField.getScene().getWindow());
        if (dir != null) {
            downloadPathField.setText(dir.getAbsolutePath());
            prefs.put("download_path", dir.getAbsolutePath());
        }
    }

    @FXML private void handleClearCache() {
        AvatarCache.clear();
        AlertUtils.showSuccess("Cache Cleared", "Images will reload.");
    }

    @FXML
    public void handleChangePassword(ActionEvent actionEvent) {
        User user = SessionManager.getCurrentUser();
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();

        // 1. Validation: Check if fields are empty
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            AlertUtils.showError("Validation Error", "Please enter your current password.");
            return;
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            AlertUtils.showError("Validation Error", "Please enter a new password.");
            return;
        }

        // 2. Validation: Ensure new password meets length requirements
        if (newPassword.trim().length() < 6) {
            AlertUtils.showError("Security", "New password must be at least 6 characters.");
            return;
        }

        // 3. Check if current and new passwords are the same
        if (currentPassword.equals(newPassword)) {
            AlertUtils.showError("Validation Error", "New password must be different from current password.");
            return;
        }

        new Thread(() -> {
            try {
                // 4. Verify current password - Hash the entered password and compare
                String enteredPasswordHash = PasswordUtil.hash(currentPassword);
                String storedPasswordHash = user.getPassword();

                if (!enteredPasswordHash.equals(storedPasswordHash)) {
                    Platform.runLater(() -> {
                        AlertUtils.showError("Authentication Failed", "Current password is incorrect.");
                        currentPasswordField.clear();
                    });
                    return;
                }

                // 5. Hash the new password
                String newPasswordHash = PasswordUtil.hash(newPassword);

                // 6. Update password in database
                boolean success = userDAO.updatePassword(user.getId(), newPasswordHash);

                Platform.runLater(() -> {
                    if (success) {
                        // Update the session user's password hash
                        user.setPassword(newPasswordHash);
                        AlertUtils.showSuccess("Success", "Password updated successfully. Please use the new password next time you login.");
                        newPasswordField.clear();
                        currentPasswordField.clear();
                    } else {
                        AlertUtils.showError("Update Failed", "We couldn't update the password in the database.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    AlertUtils.showError("Database Error", "An error occurred: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }
    @FXML
    public void handleDeleteAccount(ActionEvent actionEvent) {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        // Use custom confirmation alert
        AlertUtils.showConfirmation(
            "Delete Account",
            "This will permanently delete your profile and all stored files. This action cannot be undone.",
            () -> {
                // This runs when user clicks "Delete" button
                new Thread(() -> {
                    try {
                        if (userDAO.deleteUser(user.getId())) {
                            Platform.runLater(() -> {
                                AlertUtils.showSuccess("Account Deleted", "Your account has been successfully removed.");
                                // Cleanup session and exit application
                                SessionManager.logout();
                                Platform.exit();
                            });
                        } else {
                            Platform.runLater(() ->
                                AlertUtils.showError("Error", "Could not delete account. Please try again.")
                            );
                        }
                    } catch (Exception e) {
                        Platform.runLater(() ->
                            AlertUtils.showError("Error", "Could not delete account: " + e.getMessage())
                        );
                    }
                }).start();
            }
        );
    }
}