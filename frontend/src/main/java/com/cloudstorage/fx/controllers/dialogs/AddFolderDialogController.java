package com.cloudstorage.fx.controllers.dialogs;

import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.fx.utils.AlertUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Objects;

public class AddFolderDialogController {

    @FXML private TextField folderNameField;
    @FXML private Label errorLabel;

    private Stage stage;
    private Runnable onSuccessCallback;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOnSuccess(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    @FXML
    private void initialize() {
        // Add listener to clear error when user types
        folderNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (errorLabel.isVisible()) {
                errorLabel.setVisible(false);
                errorLabel.setManaged(false);
            }
        });
    }


    @FXML
    private void handleCreate() {
        String folderName = folderNameField.getText().trim();

        // Validation
        if (folderName.isEmpty()) {
            showError("Folder name cannot be empty");
            return;
        }

        if (folderName.length() < 2) {
            showError("Folder name must be at least 2 characters");
            return;
        }

        if (folderName.length() > 50) {
            showError("Folder name is too long (max 50 characters)");
            return;
        }

        // Check for invalid characters
        if (!folderName.matches("^[a-zA-Z0-9\\s\\-_]+$")) {
            showError("Invalid characters. Use only letters, numbers, spaces, - and _");
            return;
        }

        // Create folder in database
        new Thread(() -> {
            try {
                long userId = Objects.requireNonNull(SessionManager.getCurrentUser()).getId();
                boolean success = FileDAO.createFolder(folderName, userId);

                Platform.runLater(() -> {
                    if (success) {
                        // ✅ Create notification for folder creation
                        com.cloudstorage.util.NotificationHelper.notifyFolderCreated(userId, folderName);

                        if (onSuccessCallback != null) {
                            onSuccessCallback.run();
                        }
                        AlertUtils.showSuccess("Folder Created", "Folder '" + folderName + "' has been created successfully.");
                        closeDialog();
                    } else {
                        showError("Failed to create folder. It may already exist.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Database error: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void closeDialog() {
        if (stage != null) {
            stage.close();
        }
    }
}

