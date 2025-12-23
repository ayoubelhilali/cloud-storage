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
    @FXML private Label uploadCountLabel;

    // 🟢 Service
    private final FileUploadService uploadService = new FileUploadService();

    // 🟡 Active uploads counter
    private int activeUploads = 0;

    // 🟣 Callback après upload
    private Runnable onUploadComplete;
    public void setOnUploadComplete(Runnable action) {
        this.onUploadComplete = action;
    }

    // 🔵 Init
    @FXML
    public void initialize() {
        setupDragAndDrop();
        updateUploadCountLabel();
    }

    // =========================================================
    // 🟡 Update Upload Counter
    // =========================================================
    private void updateUploadCountLabel() {
        Platform.runLater(() -> {
            if (uploadCountLabel != null) {
                if (activeUploads == 0) {
                    uploadCountLabel.setText("No files uploading");
                    uploadCountLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px; -fx-font-weight: 600;");
                } else if (activeUploads == 1) {
                    uploadCountLabel.setText("1 file uploading");
                    uploadCountLabel.setStyle("-fx-text-fill: #0061FF; -fx-font-size: 13px; -fx-font-weight: 700;");
                } else {
                    uploadCountLabel.setText(activeUploads + " files uploading");
                    uploadCountLabel.setStyle("-fx-text-fill: #0061FF; -fx-font-size: 13px; -fx-font-weight: 700;");
                }
            }
        });
    }

    private void incrementActiveUploads() {
        activeUploads++;
        updateUploadCountLabel();
    }

    private void decrementActiveUploads() {
        activeUploads--;
        if (activeUploads < 0) activeUploads = 0;
        updateUploadCountLabel();
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

        // 🟦 UI item with modern design
        HBox itemBox = new HBox(15);
        itemBox.getStyleClass().add("progress-item");
        itemBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        itemBox.setPadding(new javafx.geometry.Insets(15));

        // Determine file type and icon
        String fileName = file.getName().toLowerCase();
        Label iconLabel = new Label();
        iconLabel.getStyleClass().add("file-icon");
        iconLabel.setMinSize(50, 50);
        iconLabel.setMaxSize(50, 50);
        iconLabel.setAlignment(javafx.geometry.Pos.CENTER);

        String iconText = "📄";
        String iconClass = "icon-blue";

        if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".gif")) {
            iconText = "🖼️";
            iconClass = "icon-purple";
        } else if (fileName.endsWith(".pdf")) {
            iconText = "📕";
            iconClass = "icon-red";
        } else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav")) {
            iconText = "🎵";
            iconClass = "icon-pink";
        } else if (fileName.endsWith(".mp4") || fileName.endsWith(".avi") || fileName.endsWith(".mkv")) {
            iconText = "🎬";
            iconClass = "icon-orange";
        } else if (fileName.endsWith(".zip") || fileName.endsWith(".rar")) {
            iconText = "📦";
            iconClass = "icon-teal";
        }

        iconLabel.setText(iconText);
        iconLabel.getStyleClass().add(iconClass);

        VBox infoBox = new VBox(8);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(file.getName());
        nameLabel.getStyleClass().add("filename-text");
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setWrapText(false);

        Label sizeLabel = new Label(formatFileSize(file.length()));
        sizeLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label percentLabel = new Label("0%");
        percentLabel.getStyleClass().add("percentage-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(nameLabel, spacer, percentLabel);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.getStyleClass().add("custom-progress-bar");
        progressBar.setMaxWidth(Double.MAX_VALUE);

        infoBox.getChildren().addAll(header, progressBar, sizeLabel);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("cancel-btn");

        itemBox.getChildren().addAll(iconLabel, infoBox, cancelBtn);
        progressListContainer.getChildren().add(0, itemBox);

        // 🟡 Increment active uploads counter
        incrementActiveUploads();

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
            decrementActiveUploads(); // Decrement counter
            percentLabel.setText("✓ Complete");
            percentLabel.getStyleClass().clear();
            percentLabel.getStyleClass().add("status-complete");
            itemBox.getStyleClass().add("progress-item-success");
            cancelBtn.setVisible(false);

            if (onUploadComplete != null) onUploadComplete.run();
            AlertUtils.showSuccess(
                    "Upload Complete",
                    file.getName() + " uploaded successfully!"
            );
        }));

        uploadTask.setOnFailed(e -> Platform.runLater(() -> {
            decrementActiveUploads(); // Decrement counter
            percentLabel.setText("✗ Failed");
            percentLabel.getStyleClass().clear();
            percentLabel.getStyleClass().add("status-error");
            itemBox.getStyleClass().add("progress-item-error");
            cancelBtn.setText("Remove");

            // --- ERROR DEBUGGING ---
            Throwable err = uploadTask.getException();
            System.err.println("UPLOAD FAILED:");
            err.printStackTrace(); // Print full error to console

            AlertUtils.showError("Upload Failed", err.getMessage());
        }));

        cancelBtn.setOnAction(e -> {
            if (uploadTask.isRunning()) {
                uploadTask.cancel();
                decrementActiveUploads(); // Decrement counter when cancelled
            }
            progressListContainer.getChildren().remove(itemBox);
        });

        Thread t = new Thread(uploadTask);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Format file size in human-readable format
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
}