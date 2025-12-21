package com.cloudstorage.fx.controllers;

import com.cloudstorage.controller.ShareController;
import com.cloudstorage.config.SessionManager;
import com.cloudstorage.model.FileMetadata;
import com.cloudstorage.util.ConfigLoader;
import com.cloudstorage.fx.utils.FileUtils;
import io.minio.MinioClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;

public class SharedDashboardController {

    @FXML private VBox sharedFilesContainer;

    // --- Stats Labels from FXML ---
    @FXML private Label lblSharedImages;
    @FXML private Label lblSharedVideos;
    @FXML private Label lblSharedAudio;
    @FXML private Label lblSharedDocs;

    private ShareController shareController;

    @FXML
    public void initialize() {
        try {
            // 1. Load Credentials
            String endpoint = ConfigLoader.getMinioEndpoint();
            String accessKey = ConfigLoader.getMinioAccessKey();
            String secretKey = ConfigLoader.getMinioSecretKey();

            if (endpoint == null || accessKey == null || secretKey == null) {
                System.err.println("CRITICAL: MinIO config is missing.");
                return;
            }

            // 2. Initialize Client
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            // 3. Setup Controller
            this.shareController = new ShareController(minioClient);

            // 4. Load Data
            loadSharedFiles();

        } catch (Exception e) {
            System.err.println("Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadSharedFiles() {
        if (SessionManager.getCurrentUser() == null || shareController == null) return;

        long userId = SessionManager.getCurrentUser().getId();

        // Fetch data on a background thread to keep UI smooth
        new Thread(() -> {
            try {
                List<FileMetadata> files = shareController.getSharedFiles(userId);

                Platform.runLater(() -> {
                    updateUI(files);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateUI(List<FileMetadata> files) {
        sharedFilesContainer.getChildren().clear();

        // Counters for the stats cards
        int imgCount = 0;
        int vidCount = 0;
        int audioCount = 0;
        int docCount = 0;

        if (files.isEmpty()) {
            Label emptyLabel = new Label("No files shared with you.");
            emptyLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-padding: 10; -fx-font-size: 14px;");
            sharedFilesContainer.getChildren().add(emptyLabel);
        } else {
            for (FileMetadata file : files) {
                String ext = FileUtils.getFileExtension(file.getFilename());

                // Categorize for stats
                if (FileUtils.isImage(ext)) imgCount++;
                else if (FileUtils.isVideo(ext)) vidCount++;
                else if (FileUtils.isAudio(ext)) audioCount++;
                else docCount++;

                // Add to list
                sharedFilesContainer.getChildren().add(createRow(file));
            }
        }

        // Update the top statistics labels
        lblSharedImages.setText(String.valueOf(imgCount));
        lblSharedVideos.setText(String.valueOf(vidCount));
        lblSharedAudio.setText(String.valueOf(audioCount));
        lblSharedDocs.setText(String.valueOf(docCount));
    }

    private HBox createRow(FileMetadata file) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(70);
        row.getStyleClass().add("recent-item"); // Using your dashboard CSS class
        row.setStyle("-fx-padding: 10 20; -fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 2);");

        String name = file.getFilename();
        String ext = FileUtils.getFileExtension(name);

        // Icon Logic
        Label iconLabel = new Label();
        iconLabel.setMinWidth(45);
        iconLabel.setMinHeight(45);
        iconLabel.setAlignment(Pos.CENTER);

        FontAwesomeSolid iconType;
        String iconColor;
        String bgColor;

        if (FileUtils.isImage(ext)) {
            iconType = FontAwesomeSolid.CAMERA;
            iconColor = "white";
            bgColor = "#8e44ad";
        } else if (ext.equals("pdf")) {
            iconType = FontAwesomeSolid.FILE_PDF;
            iconColor = "#e74c3c";
            bgColor = "#fadbd8";
        } else if (FileUtils.isAudio(ext)) {
            iconType = FontAwesomeSolid.MICROPHONE;
            iconColor = "#3498db";
            bgColor = "#d6eaf8";
        } else if (FileUtils.isVideo(ext)) {
            iconType = FontAwesomeSolid.VIDEO;
            iconColor = "#d53f8c";
            bgColor = "#fff0f6";
        } else {
            iconType = FontAwesomeSolid.FILE;
            iconColor = "#7f8c8d";
            bgColor = "#ecf0f1";
        }

        FontIcon fontIcon = new FontIcon(iconType);
        fontIcon.setIconSize(22);
        fontIcon.setIconColor(Color.web(iconColor.equals("white") ? "#FFFFFF" : iconColor));

        iconLabel.setGraphic(fontIcon);
        iconLabel.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10;");

        // Meta Data
        VBox metaBox = new VBox(3);
        metaBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(FileUtils.truncateFileName(name, 40));
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        Label sizeLabel = new Label(FileUtils.formatSize(String.valueOf(file.getFileSize())));
        sizeLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 12px;");

        metaBox.getChildren().addAll(nameLabel, sizeLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button downloadBtn = new Button("Download");
        downloadBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        downloadBtn.setOnAction(e -> handleDownload(file));

        row.getChildren().addAll(iconLabel, metaBox, spacer, downloadBtn);
        return row;
    }

    private void handleDownload(FileMetadata file) {
        try {
            if (shareController == null) {
                showAlert("Connection Error", "Not connected to file server.");
                return;
            }

            String downloadUrl = shareController.getDownloadLink(
                    file.getId(),
                    SessionManager.getCurrentUser().getId()
            );

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(downloadUrl));
            } else {
                showAlert("Error", "Browser not supported.");
            }
        } catch (Exception e) {
            showAlert("Download Failed", "Could not access file: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}