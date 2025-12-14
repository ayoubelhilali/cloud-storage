package com.cloudstorage.fx.controllers;

import com.cloudstorage.config.SessionManager;
import com.cloudstorage.service.FileUploadService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.scene.input.TransferMode;

import java.io.File;
import java.util.List;

public class UploadFilesController {

    @FXML private VBox dropZone;
    @FXML private Button btnBrowse;
    @FXML private VBox progressListContainer;

    private final FileUploadService uploadService = new FileUploadService();

    // --- 1. NEW: Define a Callback ---
    private Runnable onUploadComplete;

    // --- 2. NEW: Setter for the Callback ---
    public void setOnUploadComplete(Runnable action) {
        this.onUploadComplete = action;
    }

    @FXML
    public void initialize() {
        setupDragAndDrop();
    }

    // [setupDragAndDrop... same as before]
    private void setupDragAndDrop() {
        dropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        });
        dropZone.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getDragboard().hasFiles()) {
                List<File> files = event.getDragboard().getFiles();
                for (File file : files) addFileToUploadQueue(file);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    @FXML
    private void handleBrowseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Files to Upload");
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(btnBrowse.getScene().getWindow());

        if (selectedFiles != null) {
            for (File file : selectedFiles) addFileToUploadQueue(file);
        }
    }

    private void addFileToUploadQueue(File file) {
        long currentUserId = SessionManager.getCurrentUser().getId();

        if (currentUserId <= 0) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Session Error: Please logout and login again.");
            alert.show();
            return;
        }

        // UI Creation
        HBox itemBox = new HBox();
        itemBox.getStyleClass().add("progress-item");
        itemBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        itemBox.setSpacing(15);
        itemBox.setPadding(new javafx.geometry.Insets(10, 15, 10, 15));

        Label iconLabel = new Label("📄");
        iconLabel.getStyleClass().addAll("file-icon", "icon-teal");

        VBox infoBox = new VBox(5);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        HBox detailsRow = new HBox();
        Label nameLabel = new Label(file.getName());
        nameLabel.getStyleClass().add("filename-text");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label percentLabel = new Label("0%");
        percentLabel.getStyleClass().add("percentage-text");

        detailsRow.getChildren().addAll(nameLabel, spacer, percentLabel);
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("custom-progress-bar");
        infoBox.getChildren().addAll(detailsRow, progressBar);

        Button cancelBtn = new Button("✖");
        cancelBtn.getStyleClass().add("cancel-btn");
        cancelBtn.setOnMouseClicked(event -> {
            itemBox.getChildren().clear();
        });

        itemBox.getChildren().addAll(iconLabel, infoBox, cancelBtn);
        progressListContainer.getChildren().add(0, itemBox);

        // Background Task
        Task<Void> uploadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                uploadService.uploadFile(file, currentUserId, (progress) -> {
                    updateProgress(progress, 1.0);
                });
                return null;
            }
        };

        progressBar.progressProperty().bind(uploadTask.progressProperty());

        uploadTask.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() < 0.99) {
                int percent = (int) (newVal.doubleValue() * 100);
                Platform.runLater(() -> percentLabel.setText(percent + "%"));
            }
        });

        uploadTask.setOnSucceeded(e -> Platform.runLater(() -> {
            percentLabel.setText("Done");
            percentLabel.setStyle("-fx-text-fill: #00b894;");
            progressBar.getStyleClass().add("progress-bar-success");

            // --- 3. NEW: Trigger the Refresh Callback ---
            if (onUploadComplete != null) {
                System.out.println("Triggering Dashboard Refresh...");
                onUploadComplete.run();
            }
        }));

        uploadTask.setOnFailed(e -> Platform.runLater(() -> {
            Throwable error = uploadTask.getException();
            String msg = (error != null) ? error.getMessage() : "Unknown Error";
            percentLabel.setText("Failed");
            percentLabel.setStyle("-fx-text-fill: #d63031;");
            percentLabel.setTooltip(new Tooltip(msg));
            error.printStackTrace();
        }));

        cancelBtn.setOnAction(e -> {
            if (uploadTask.isRunning()) uploadTask.cancel();
            progressListContainer.getChildren().remove(itemBox);
        });

        Thread thread = new Thread(uploadTask);
        thread.setDaemon(true);
        thread.start();
    }
}