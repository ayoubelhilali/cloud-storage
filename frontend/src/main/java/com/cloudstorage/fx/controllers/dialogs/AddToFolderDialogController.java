package com.cloudstorage.fx.controllers.dialogs;

import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.fx.utils.AlertUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AddToFolderDialogController {

    @FXML private Label fileNameLabel;
    @FXML private ComboBox<String> folderComboBox;
    @FXML private Label errorLabel;

    private Stage stage;
    private String fileName;
    private Long currentFolderId; // ID of folder the file is currently in
    private List<Map<String, Object>> folders;
    private Runnable onSuccessCallback;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
        if (fileNameLabel != null) {
            String truncated = fileName.length() > 35
                ? fileName.substring(0, 32) + "..."
                : fileName;
            fileNameLabel.setText("Moving \"" + truncated + "\"");
        }
    }

    public void setCurrentFolderId(Long folderId) {
        this.currentFolderId = folderId;
    }

    public void setOnSuccess(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    @FXML
    private void initialize() {
        loadFolders();
    }

    private void loadFolders() {
        new Thread(() -> {
            try {
                long userId = SessionManager.getCurrentUser().getId();
                folders = FileDAO.getFoldersByUserId(userId);

                // Filter out the current folder if file is already in a folder
                if (currentFolderId != null) {
                    folders = folders.stream()
                            .filter(f -> !currentFolderId.equals(f.get("id")))
                            .collect(Collectors.toList());
                }

                Platform.runLater(() -> {
                    if (folders.isEmpty()) {
                        if (currentFolderId != null) {
                            showError("No other folders available. Create a new folder first.");
                        } else {
                            showError("No folders available. Create a folder first.");
                        }
                        folderComboBox.setDisable(true);
                    } else {
                        List<String> folderNames = folders.stream()
                                .map(f -> {
                                    String name = (String) f.get("name");
                                    Integer fileCount = (Integer) f.get("file_count");
                                    return name + " (" + (fileCount != null ? fileCount : 0) + " files)";
                                })
                                .collect(Collectors.toList());
                        folderComboBox.setItems(FXCollections.observableArrayList(folderNames));
                        if (!folderNames.isEmpty()) {
                            folderComboBox.getSelectionModel().selectFirst();
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Failed to load folders: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }

    @FXML
    private void handleMove() {
        String selectedItem = folderComboBox.getValue();

        if (selectedItem == null || selectedItem.isEmpty()) {
            showError("Please select a destination folder");
            return;
        }

        // Extract folder name (remove the file count suffix)
        String selectedFolderName = selectedItem.replaceAll(" \\(\\d+ files\\)$", "");

        // Find the folder ID
        Long folderId = folders.stream()
                .filter(f -> f.get("name").equals(selectedFolderName))
                .map(f -> (Long) f.get("id"))
                .findFirst()
                .orElse(null);

        if (folderId == null) {
            showError("Selected folder not found");
            return;
        }

        // Move file to folder
        new Thread(() -> {
            try {
                long userId = SessionManager.getCurrentUser().getId();

                // Get bucket from session user
                String bucket = SessionManager.getCurrentUser().getBucketName();

                // Use the updateFileFolder method which handles insert/update
                boolean success = FileDAO.updateFileFolder(userId, fileName, folderId, bucket, 0);

                Platform.runLater(() -> {
                    if (success) {
                        if (onSuccessCallback != null) {
                            onSuccessCallback.run();
                        }
                        AlertUtils.showSuccess("File Moved",
                            "File has been moved to '" + selectedFolderName + "' successfully.");
                        closeDialog();
                    } else {
                        showError("Failed to move file. Please try again.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Error: " + e.getMessage());
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
