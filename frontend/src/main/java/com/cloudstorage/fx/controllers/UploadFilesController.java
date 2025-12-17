package com.cloudstorage.fx.controllers;

import com.cloudstorage.config.SessionManager;
import com.cloudstorage.fx.utils.AlertUtils;
import com.cloudstorage.service.FileUploadService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class UploadFilesController {

    // 🔵 FXML
    @FXML private VBox dropZone;
    @FXML private Button btnBrowse;
    @FXML private VBox progressListContainer;

    // 🟢 Service
    private final FileUploadService uploadService = new FileUploadService();

    // 🟣 Callback après upload
    private Runnable onUploadComplete;
    public void setOnUploadComplete(Runnable action) {
        this.onUploadComplete = action;
    }

    // 🔵 Init
    @FXML
    public void initialize() {
        setupDragAndDrop();
    }

    // =========================================================
    // 🟢 FILE CHOOSER (Images + Documents seulement)
    // =========================================================
    @FXML
    private void handleBrowseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select file");

        FileChooser.ExtensionFilter allFiles =
                new FileChooser.ExtensionFilter(
                        "All Supported Files",
                        "*.png", "*.jpg", "*.jpeg", "*.gif",
                        "*.pdf", "*.doc", "*.docx", "*.txt",
                        "*.mp3", "*.wav",
                        "*.mp4", "*.avi", "*.mkv"
                );

        FileChooser.ExtensionFilter images =
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif");

        FileChooser.ExtensionFilter documents =
                new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.txt");

        FileChooser.ExtensionFilter audio =
                new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav");

        FileChooser.ExtensionFilter video =
                new FileChooser.ExtensionFilter("Video", "*.mp4", "*.avi", "*.mkv");

        fileChooser.getExtensionFilters().addAll(allFiles, images, documents, audio, video);
        fileChooser.setSelectedExtensionFilter(allFiles);

        List<File> selectedFiles =
                fileChooser.showOpenMultipleDialog(btnBrowse.getScene().getWindow());

        if (selectedFiles != null) {
            for (File file : selectedFiles) {
                addFileToUploadQueue(file);
            }
        }
    }

    // =========================================================
    // 🟢 DRAG & DROP sécurisé
    // =========================================================
    private void setupDragAndDrop() {
        dropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            boolean success = false;

            if (event.getDragboard().hasFiles()) {
                for (File file : event.getDragboard().getFiles()) {
                    if (isAllowedFile(file)) {
                        addFileToUploadQueue(file);
                    } else {
                        AlertUtils.showError(
                                "Invalid file",
                                "Only images and documents are allowed."
                        );
                    }
                }
                success = true;
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

    // =========================================================
    // 🟢 Sécurité formats autorisés
    // =========================================================
    private boolean isAllowedFile(File file) {
        String name = file.getName().toLowerCase();

        return name.endsWith(".png")
                || name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".gif")
                || name.endsWith(".pdf")
                || name.endsWith(".doc")
                || name.endsWith(".docx")
                || name.endsWith(".txt")
                || name.endsWith(".mp3")
                || name.endsWith(".wav")
                || name.endsWith(".mp4")
                || name.endsWith(".avi")
                || name.endsWith(".mkv");
    }

    // =========================================================
    // 🟢 Ajout + Upload
    // =========================================================
    private void addFileToUploadQueue(File file) {

        // 🔴 Sécurité
        if (!isAllowedFile(file)) {
            AlertUtils.showError(
                    "Invalid file type",
                    "Only images and documents are allowed."
            );
            return;
        }

        // 1. Get User Data
        var currentUser = SessionManager.getCurrentUser();
        long currentUserId = currentUser.getId();

        // --- FIX: Get Bucket Name from Session ---
        // Ensure your User model has this, or fallback to the same generation logic used in Dashboard
        String fName = (currentUser.getFirstName() != null) ? currentUser.getFirstName() : "user";
        String lName = (currentUser.getLastName() != null) ? currentUser.getLastName() : "default";
        String generatedBucket = (fName + "-" + lName).toLowerCase().replaceAll("[^a-z0-9-]", "");

        String bucketName = (currentUser.getBucketName() != null && !currentUser.getBucketName().isEmpty())
                ? currentUser.getBucketName()
                : generatedBucket;

        if (currentUserId <= 0) {
            AlertUtils.showError("Session Error", "Please logout and login again.");
            return;
        }

        // 🟦 UI item
        HBox itemBox = new HBox(15);
        itemBox.getStyleClass().add("progress-item");
        itemBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        itemBox.setPadding(new javafx.geometry.Insets(10));

        Label iconLabel = new Label("📄");
        iconLabel.getStyleClass().add("file-icon");

        VBox infoBox = new VBox(5);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(file.getName());
        Label percentLabel = new Label("0%");
        percentLabel.getStyleClass().add("percentage-text");

        HBox header = new HBox(nameLabel, new Region(), percentLabel);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        infoBox.getChildren().addAll(header, progressBar);

        Button cancelBtn = new Button("✖");
        cancelBtn.getStyleClass().add("cancel-btn");

        itemBox.getChildren().addAll(iconLabel, infoBox, cancelBtn);
        progressListContainer.getChildren().add(0, itemBox);

        // 🟡 Task Upload
        Task<Void> uploadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // --- FIX: Pass bucketName to the service ---
                uploadService.uploadFile(file, currentUserId, bucketName,
                        progress -> updateProgress(progress, 1.0));
                return null;
            }
        };

        progressBar.progressProperty().bind(uploadTask.progressProperty());

        uploadTask.progressProperty().addListener((obs, o, n) -> {
            int p = (int) (n.doubleValue() * 100);
            Platform.runLater(() -> percentLabel.setText(p + "%"));
        });

        uploadTask.setOnSucceeded(e -> Platform.runLater(() -> {
            percentLabel.setText("Done");
            percentLabel.setStyle("-fx-text-fill: #00b894;");
            if (onUploadComplete != null) onUploadComplete.run();
            AlertUtils.showSuccess(
                    "Upload success",
                    "Your file has been uploaded successfully."
            );
        }));

        uploadTask.setOnFailed(e -> Platform.runLater(() -> {
            percentLabel.setText("Failed");
            percentLabel.setStyle("-fx-text-fill: #d63031;");

            // --- ERROR DEBUGGING ---
            Throwable err = uploadTask.getException();
            System.err.println("UPLOAD FAILED:");
            err.printStackTrace(); // Print full error to console

            AlertUtils.showError("Upload Failed", err.getMessage());
        }));

        cancelBtn.setOnAction(e -> {
            if (uploadTask.isRunning()) uploadTask.cancel();
            progressListContainer.getChildren().remove(itemBox);
        });

        Thread t = new Thread(uploadTask);
        t.setDaemon(true);
        t.start();
    }
}