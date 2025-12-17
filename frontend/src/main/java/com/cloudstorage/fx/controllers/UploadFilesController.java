package com.cloudstorage.fx.controllers;

import com.cloudstorage.config.SessionManager;
import com.cloudstorage.service.FileUploadService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.List;

public class UploadFilesController {

    @FXML private VBox dropZone;
    @FXML private ListView<String> fileListView;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private Button uploadButton;

    private List<File> selectedFiles;
    private final FileUploadService uploadService = new FileUploadService();
    private Runnable onUploadComplete;

    @FXML
    public void initialize() {
        progressBar.setVisible(false);
        uploadButton.setDisable(true);
    }

    @FXML
    private void handleBrowse() {
        FileChooser fileChooser = new FileChooser();
        selectedFiles = fileChooser.showOpenMultipleDialog(dropZone.getScene().getWindow());
        if (selectedFiles != null) {
            updateFileList();
            uploadButton.setDisable(false);
        }
    }

    private void updateFileList() {
        fileListView.getItems().clear();
        for (File file : selectedFiles) {
            fileListView.getItems().add(file.getName());
        }
    }

    @FXML
    private void handleUpload() {
        if (selectedFiles == null || selectedFiles.isEmpty()) return;
        long userId = SessionManager.getCurrentUser().getId();

        progressBar.setVisible(true);
        uploadButton.setDisable(true);
        statusLabel.setText("Uploading...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (File file : selectedFiles) {
                    // This calls the service which saves to DB automatically
                    uploadService.uploadFile(file, userId, progress -> {});
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            statusLabel.setText("Upload Complete!");
            progressBar.setVisible(false);
            fileListView.getItems().clear();
            if (onUploadComplete != null) onUploadComplete.run();
        });

        task.setOnFailed(e -> {
            statusLabel.setText("Upload Failed");
            e.getSource().getException().printStackTrace();
        });

        new Thread(task).start();
    }

    public void setOnUploadComplete(Runnable callback) {
        this.onUploadComplete = callback;
    }
}