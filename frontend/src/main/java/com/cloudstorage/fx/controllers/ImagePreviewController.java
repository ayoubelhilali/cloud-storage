package com.cloudstorage.fx.controllers;

import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.fx.utils.AlertUtils; // Assuming you have this from previous steps
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;

public class ImagePreviewController {

    @FXML private ImageView imageView;
    @FXML private VBox iconContainer;
    @FXML private FontIcon typeIcon;
    @FXML private Label typeLabel;
    @FXML private Label fileNameLabel;
    @FXML private Label dateLabel;

    // --- CONTEXT VARIABLES ---
    private MinioClient minioClient;
    private String bucketName;
    private Runnable onFileDeleted;

    /**
     * Updated to match DashboardController calls.
     * Receives direct connection objects instead of using HTTP.
     */
    public void setDeleteContext(MinioClient client, String bucket, Runnable onFileDeleted) {
        this.minioClient = client;
        this.bucketName = bucket;
        this.onFileDeleted = onFileDeleted;
    }

    // --- DISPLAY LOGIC ---
    public void setFileData(Image image, String fileName, String details) {
        this.fileNameLabel.setText(fileName);
        this.dateLabel.setText(details);
        String ext = getExtension(fileName);

        if (image != null) {
            imageView.setVisible(true);
            iconContainer.setVisible(false);
            imageView.setImage(image);
        } else {
            imageView.setVisible(false);
            iconContainer.setVisible(true);
            configureIcon(ext);
        }
    }

    private void configureIcon(String ext) {
        typeIcon.setIconSize(100);
        switch (ext) {
            case "pdf":
                typeIcon.setIconLiteral("fas-file-pdf");
                typeIcon.setIconColor(Color.web("#e53e3e"));
                typeLabel.setText("PDF Document");
                break;
            case "doc": case "docx":
                typeIcon.setIconLiteral("fas-file-word");
                typeIcon.setIconColor(Color.web("#3182ce"));
                typeLabel.setText("Word Document");
                break;
            case "xls": case "xlsx":
                typeIcon.setIconLiteral("fas-file-excel");
                typeIcon.setIconColor(Color.web("#38a169"));
                typeLabel.setText("Spreadsheet");
                break;
            case "zip": case "rar":
                typeIcon.setIconLiteral("fas-file-archive");
                typeIcon.setIconColor(Color.web("#d69e2e"));
                typeLabel.setText("Archive");
                break;
            default:
                typeIcon.setIconLiteral("fas-file");
                typeIcon.setIconColor(Color.web("#718096"));
                typeLabel.setText(ext.toUpperCase() + " File");
                break;
        }
    }

    // --- DELETE LOGIC ---
    @FXML
    private void handleDeleteButtonAction() {
        String fileName = fileNameLabel.getText();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete File");
        alert.setHeaderText("Delete '" + fileName + "'?");
        alert.setContentText("This action cannot be undone.");

        // Apply CSS if available
        try {
            String css = getClass().getResource("/css/ImagePreview.css").toExternalForm();
            alert.getDialogPane().getStylesheets().add(css);
            alert.getDialogPane().getStyleClass().add("custom-alert");
        } catch (Exception ignored) { }

        alert.initStyle(StageStyle.UNDECORATED);

        // Use YES/NO buttons for clarity
        ButtonType btnYes = new ButtonType("Delete", ButtonBar.ButtonData.YES);
        ButtonType btnNo = new ButtonType("Cancel", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(btnYes, btnNo);

        alert.showAndWait().ifPresent(type -> {
            if (type == btnYes) {
                handleRemove(fileName);
            }
        });
    }

    private void handleRemove(String filename) {
        if (minioClient == null || bucketName == null) {
            AlertUtils.showError("Error", "System is not connected properly.");
            return;
        }

        Task<Void> deleteTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 1. Remove from MinIO (Physical File)
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(filename)
                                .build()
                );

                // 2. Remove from Database (Metadata & Favorites)
                if (SessionManager.getCurrentUser() != null) {
                    long userId = SessionManager.getCurrentUser().getId();
                    // Ensure you have this method in FileDAO (we added it in previous steps)
                    FileDAO.deleteFileRecord(filename, (int) userId);
                }
                return null;
            }
        };

        deleteTask.setOnSucceeded(event -> Platform.runLater(() -> {
            // Optional: Show success message? Or just close smoothly.
            // AlertUtils.showSuccess("Deleted", filename + " has been deleted.");

            // 3. Refresh Dashboard
            if (onFileDeleted != null) onFileDeleted.run();
            closeWindow();
        }));

        deleteTask.setOnFailed(event -> {
            Throwable e = deleteTask.getException();
            e.printStackTrace();
            Platform.runLater(() ->
                    AlertUtils.showError("Delete Failed", "Could not delete file: " + e.getMessage())
            );
        });

        new Thread(deleteTask).start();
    }

    private String getExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) return fileName.substring(i + 1).toLowerCase();
        return "";
    }

    @FXML
    private void closeWindow() {
        if (fileNameLabel.getScene() != null) {
            Stage stage = (Stage) fileNameLabel.getScene().getWindow();
            stage.close();
        }
    }
}