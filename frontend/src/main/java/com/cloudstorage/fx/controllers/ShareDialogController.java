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
    private final ShareController shareController;

    public ShareDialogController() {
        // Initialize backend controller
        // Ideally, move MinioClient initialization to a global Config class to avoid recreating it everywhere
        MinioClient minioClient = MinioClient.builder()
                .endpoint("http://127.0.0.1:9000")
                .credentials("YOUR_ACCESS_KEY", "YOUR_SECRET_KEY")
                .build();
        this.shareController = new ShareController(minioClient);
    }

    public void setTargetFile(String filename) {
        this.targetFilename = filename;
        this.fileNameLabel.setText(filename);
    }

    @FXML
    private void handleShare() {
        String email = emailField.getText();

        if (email == null || email.trim().isEmpty()) {
            AlertUtils.showError("Input Error", "Please enter an email address.");
            return;
        }

        long senderId = SessionManager.getCurrentUser().getId();

        // Call Backend
        // Note: Make sure 'targetFilename' matches what is in your DB (files table)
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
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.close();
    }
}