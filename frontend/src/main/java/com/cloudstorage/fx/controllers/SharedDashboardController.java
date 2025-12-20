package com.cloudstorage.fx.controllers;

import com.cloudstorage.controller.ShareController;
import com.cloudstorage.config.SessionManager;
import com.cloudstorage.model.FileMetadata;
import com.cloudstorage.util.ConfigLoader;
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
import javafx.scene.paint.Color; // Import for Colors
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid; // Import for Icons
import org.kordamp.ikonli.javafx.FontIcon; // Import for Icons

import java.awt.Desktop;
import java.net.URI;
import java.util.List;

public class SharedDashboardController {

    @FXML private VBox sharedFilesContainer;

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
        if (SessionManager.getCurrentUser() == null) return;
        if (shareController == null) return;

        long userId = SessionManager.getCurrentUser().getId();
        List<FileMetadata> files = shareController.getSharedFiles(userId);

        sharedFilesContainer.getChildren().clear();

        if (files.isEmpty()) {
            Label emptyLabel = new Label("No files shared with you.");
            emptyLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-padding: 10; -fx-font-size: 14px;");
            sharedFilesContainer.getChildren().add(emptyLabel);
        } else {
            for (FileMetadata file : files) {
                sharedFilesContainer.getChildren().add(createRow(file));
            }
        }
    }

    // --- UPDATED DESIGN METHOD ---
    private HBox createRow(FileMetadata file) {
        HBox row = new HBox(15); // Reduced spacing slightly for tighter look
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(70);
        row.setStyle("-fx-padding: 10 20; -fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 2);");

        // 1. DYNAMIC ICON (Colors based on type)
        Label iconLabel = new Label();
        iconLabel.setMinWidth(45);
        iconLabel.setMinHeight(45);
        iconLabel.setAlignment(Pos.CENTER);

        // Determine extension
        String name = file.getFilename(); // Or use getOriginalFilename if available
        String ext = "";
        int i = name.lastIndexOf('.');
        if (i > 0) ext = name.substring(i + 1).toLowerCase();

        // Icon Logic
        FontAwesomeSolid iconType;
        String iconColor;
        String bgColor;

        switch (ext) {
            case "png": case "jpg": case "jpeg": case "gif":
                iconType = FontAwesomeSolid.CAMERA;
                iconColor = "white";
                bgColor = "#8e44ad"; // Purple
                break;
            case "pdf":
                iconType = FontAwesomeSolid.FILE_PDF;
                iconColor = "#e74c3c"; // Red
                bgColor = "#fadbd8"; // Light Red BG
                break;
            case "doc": case "docx":
                iconType = FontAwesomeSolid.FILE_WORD;
                iconColor = "#2980b9"; // Blue
                bgColor = "#d6eaf8"; // Light Blue BG
                break;
            case "zip": case "rar":
                iconType = FontAwesomeSolid.FILE_ARCHIVE;
                iconColor = "#f39c12"; // Orange
                bgColor = "#fdebd0";
                break;
            default:
                iconType = FontAwesomeSolid.FILE;
                iconColor = "#7f8c8d"; // Grey
                bgColor = "#ecf0f1";
                break;
        }

        FontIcon fontIcon = new FontIcon(iconType);
        fontIcon.setIconSize(22);

        // If the background is light, use the colored icon. If background is dark (like image), use white icon.
        if (ext.matches("png|jpg|jpeg|gif")) {
            fontIcon.setIconColor(Color.WHITE);
            iconLabel.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10;");
        } else {
            fontIcon.setIconColor(Color.web(iconColor));
            iconLabel.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10;");
        }
        iconLabel.setGraphic(fontIcon);


        // 2. TEXT METADATA
        VBox metaBox = new VBox(3); // Small vertical spacing
        metaBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(name);
        // FORCE TEXT COLOR TO DARK GREY (#2c3e50) TO ENSURE VISIBILITY
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");
        nameLabel.setWrapText(false);

        Label sizeLabel = new Label((file.getFileSize() / 1024) + " KB");
        sizeLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 12px;");

        metaBox.getChildren().addAll(nameLabel, sizeLabel);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 3. DOWNLOAD BUTTON
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
            e.printStackTrace();
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