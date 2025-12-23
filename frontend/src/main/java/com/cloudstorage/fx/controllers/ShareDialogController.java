package com.cloudstorage.fx.controllers;

import com.cloudstorage.config.SessionManager;
import com.cloudstorage.controller.ShareController;
import com.cloudstorage.fx.utils.AlertUtils;
import io.minio.MinioClient;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ShareDialogController {

    @FXML private Label fileNameLabel;
    @FXML private TextField emailField;

    private String targetFilename;
    private Stage stage;
    private final ShareController shareController;

    public ShareDialogController() {
        // Initialize backend controller
        MinioClient minioClient = MinioClient.builder()
                .endpoint("http://127.0.0.1:9000")
                .credentials("YOUR_ACCESS_KEY", "YOUR_SECRET_KEY")
                .build();
        this.shareController = new ShareController(minioClient);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setTargetFile(String filename) {
        this.targetFilename = filename;
        if (fileNameLabel != null) {
            // Truncate long filenames
            String displayName = filename.length() > 40
                ? filename.substring(0, 37) + "..."
                : filename;
            fileNameLabel.setText(displayName);
        }
    }

    @FXML
    private void handleShare() {
        String email = emailField.getText();

        // Validation
        if (email == null || email.trim().isEmpty()) {
            AlertUtils.showError("Input Error", "Please enter an email address.");
            return;
        }

        // Simple email validation
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            AlertUtils.showError("Invalid Email", "Please enter a valid email address.");
            return;
        }

        long senderId = SessionManager.getCurrentUser().getId();

        // Call Backend
        String result = shareController.shareFileByName(targetFilename, senderId, email);

        if ("SUCCESS".equals(result)) {
            AlertUtils.showSuccess("Shared Successfully", "File shared with " + email);
            closeWindow();
        } else {
            AlertUtils.showError("Share Failed", result);
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        if (stage != null) {
            stage.close();
        } else {
            Stage currentStage = (Stage) emailField.getScene().getWindow();
            currentStage.close();
        }
    }
}