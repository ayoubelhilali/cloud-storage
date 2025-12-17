package com.cloudstorage.fx.controllers;

import com.cloudstorage.controller.ShareController;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ShareDialogController {

    @FXML private TextField emailField;
    @FXML private Label statusLabel;

    private String filename;
    private long currentUserId;
    private Stage dialogStage;

    public void setShareData(String filename, long currentUserId, Stage dialogStage) {
        this.filename = filename;
        this.currentUserId = currentUserId;
        this.dialogStage = dialogStage;
    }

    @FXML
    private void handleShare() {
        String email = emailField.getText();

        // Visual feedback immediately
        statusLabel.setText("Processing...");
        statusLabel.setStyle("-fx-text-fill: black;");

        ShareController controller = new ShareController();

        // --- FIX: Store result as String, not boolean ---
        String result = controller.shareFileByName(filename, currentUserId, email);

        if ("SUCCESS".equals(result)) {
            statusLabel.setText("Shared successfully!");
            statusLabel.setStyle("-fx-text-fill: green;");

            // Optional: Close the window automatically after 1 second?
            // dialogStage.close();
        } else {
            // Show the actual error message from the backend (e.g. "User created", "Self-share error")
            statusLabel.setText(result);
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}