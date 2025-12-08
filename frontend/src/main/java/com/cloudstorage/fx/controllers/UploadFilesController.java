package com.cloudstorage.fx.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.scene.input.TransferMode;

import java.io.File;
import java.util.List;

public class UploadFilesController {

    @FXML
    private VBox dropZone;

    @FXML
    private Button btnBrowse;

    @FXML
    private VBox progressListContainer;

    @FXML
    public void initialize() {
        setupDragAndDrop();
    }

    private void setupDragAndDrop() {
        // 1. Drag Over Event
        dropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                if (!dropZone.getStyleClass().contains("upload-card-drag")) {
                    dropZone.getStyleClass().add("upload-card-drag");
                }
            }
            event.consume();
        });

        // 2. Drag Exited Event
        dropZone.setOnDragExited(event -> {
            dropZone.getStyleClass().remove("upload-card-drag");
            event.consume();
        });

        // 3. Drag Dropped Event
        dropZone.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getDragboard().hasFiles()) {
                List<File> files = event.getDragboard().getFiles();
                for (File file : files) {
                    addFileToUploadQueue(file);
                }
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
            for (File file : selectedFiles) {
                addFileToUploadQueue(file);
            }
        }
    }

    /**
     * Creates a new visual row in the progress list for the file
     * and starts the upload process.
     */
    private void addFileToUploadQueue(File file) {
        // 1. Create UI Elements dynamically
        HBox itemBox = new HBox();
        itemBox.getStyleClass().add("progress-item");
        itemBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        itemBox.setSpacing(15);
        itemBox.setPadding(new javafx.geometry.Insets(10, 15, 10, 15));

        // Icon
        Label iconLabel = new Label("📄"); // Dynamic icon based on extension could go here
        iconLabel.getStyleClass().addAll("file-icon", "icon-teal");

        // Info VBox (Name + Bar)
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

        // Cancel Button
        Button cancelBtn = new Button("✖");
        cancelBtn.getStyleClass().add("cancel-btn");
        cancelBtn.setOnAction(e -> {
            // Logic to cancel upload task would go here
            progressListContainer.getChildren().remove(itemBox);
        });

        itemBox.getChildren().addAll(iconLabel, infoBox, cancelBtn);

        // 2. Add to the View
        progressListContainer.getChildren().add(0, itemBox); // Add to top

        // 3. Start Mock Upload (Replace this with MinIO logic later)
        simulateUpload(progressBar, percentLabel);
    }

    private void simulateUpload(ProgressBar bar, Label percentLabel) {
        // This is a dummy thread to simulate MinIO upload progress
        new Thread(() -> {
            try {
                for (int i = 0; i <= 100; i++) {
                    double progress = i / 100.0;
                    int finalI = i;

                    // Update UI on JavaFX Thread
                    javafx.application.Platform.runLater(() -> {
                        bar.setProgress(progress);
                        percentLabel.setText(finalI + "%");
                    });

                    Thread.sleep(30); // Simulate network delay
                }
                // Upload Complete
                javafx.application.Platform.runLater(() -> {
                    percentLabel.setText("Done");
                    percentLabel.setStyle("-fx-text-fill: #00b894;"); // Green color
                });

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
