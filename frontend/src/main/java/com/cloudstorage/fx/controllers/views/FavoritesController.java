package com.cloudstorage.fx.controllers.views;

import com.cloudstorage.config.MinioConfig;
import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.fx.components.MediaPreviewDialog;
import com.cloudstorage.model.FileMetadata;
import com.cloudstorage.model.User;
import com.cloudstorage.fx.utils.FileUtils;
import com.cloudstorage.fx.utils.AlertUtils;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.prefs.Preferences;

public class FavoritesController {

    @FXML private FlowPane filesGrid;
    @FXML private ScrollPane scrollPane;

    // --- NEW: Stats Labels ---
    @FXML private Label lblFavImages;
    @FXML private Label lblFavVideos;
    @FXML private Label lblFavAudio;
    @FXML private Label lblFavDocs;

    private MinioClient minioClient;

    @FXML
    public void initialize() {
        filesGrid.setHgap(20);
        filesGrid.setVgap(20);
        // Initialiser le client MinIO
        this.minioClient = MinioConfig.getClient();
        // Load data initially
        refresh();
    }

    public void refresh() {
        // Force refresh chaque fois que la vue est affichée
        // pour s'assurer que les favoris sont toujours à jour
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;

        // 2. SHOW LOADER
        filesGrid.getChildren().clear();
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(40, 40);
        VBox loaderContainer = new VBox(10, spinner, new Label("Loading favorites..."));
        loaderContainer.setAlignment(Pos.CENTER);
        loaderContainer.setPadding(new Insets(20));
        filesGrid.getChildren().add(loaderContainer);

        // 3. Create the Task
        Task<List<FileMetadata>> task = new Task<>() {
            @Override
            protected List<FileMetadata> call() throws Exception {
                return FileDAO.getFavoriteFiles(currentUser.getId());
            }
        };

        // Get user bucket for file operations
        String userBucket = currentUser.getBucketName();
        if (userBucket == null || userBucket.isEmpty()) {
            userBucket = (currentUser.getFirstName() + "-" + currentUser.getLastName())
                    .toLowerCase().replaceAll("[^a-z0-9-]", "");
        }
        final String bucket = userBucket;

        // 4. Handle Success
        task.setOnSucceeded(e -> {
            List<FileMetadata> files = task.getValue();
            filesGrid.getChildren().clear(); // Remove loader

            // --- STATS COUNTERS ---
            int imgCount = 0;
            int vidCount = 0;
            int audioCount = 0;
            int docCount = 0;

            if (files.isEmpty()) {
                Label emptyLabel = new Label("No favorite files yet.");
                emptyLabel.setStyle("-fx-text-fill: #718096; -fx-font-size: 16px;");
                filesGrid.getChildren().add(emptyLabel);
            } else {
                for (FileMetadata file : files) {

                    // A. Update Counters
                    String ext = FileUtils.getFileExtension(file.getFilename());
                    if (FileUtils.isImage(ext)) {
                        imgCount++;
                    } else if (FileUtils.isVideo(ext)) {
                        vidCount++;
                    } else if (FileUtils.isAudio(ext)) {
                        audioCount++;
                    } else {
                        // Count everything else (docs, archives) as Documents for now
                        docCount++;
                    }

                    // B. Create Card using FileCardFactory for consistent design
                    VBox card = com.cloudstorage.fx.components.FileCardFactory.createFileCard(
                        file, 
                        bucket, 
                        minioClient,
                        f -> handleFileClick(f),
                        this::refresh
                    );
                    filesGrid.getChildren().add(card);
                }
            }

            // Update Labels (Clean Numbers)
            lblFavImages.setText(String.valueOf(imgCount));
            lblFavVideos.setText(String.valueOf(vidCount));
            lblFavAudio.setText(String.valueOf(audioCount));
            lblFavDocs.setText(String.valueOf(docCount));

            // Reset favorites changed flag
            SessionManager.setFavoritesChanged(false);
        });

        // 5. Handle Failure
        task.setOnFailed(e -> {
            filesGrid.getChildren().clear();
            Throwable error = task.getException();
            System.err.println("Error loading favorites: " + error.getMessage());

            Label errorLabel = new Label("Could not load files.");
            errorLabel.setStyle("-fx-text-fill: #e53e3e; -fx-font-size: 14px;");
            filesGrid.getChildren().add(errorLabel);
        });

        new Thread(task).start();
    }

    private VBox createCard(FileMetadata file) {
        String filename = file.getFilename();
        String ext = FileUtils.getFileExtension(filename);

        // 1. Card Container
        VBox card = new VBox();
        card.getStyleClass().addAll("grid-item", "favorites-card");
        card.setPrefWidth(200);
        card.setPrefHeight(230);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15;");
        
        // Hover effect
        DropShadow normalShadow = new DropShadow(8, Color.web("#0000001a"));
        normalShadow.setOffsetY(2);
        card.setEffect(normalShadow);
        
        card.setOnMouseEntered(e -> {
            DropShadow hoverShadow = new DropShadow(15, Color.web("#0984e333"));
            hoverShadow.setOffsetY(4);
            card.setEffect(hoverShadow);
        });
        card.setOnMouseExited(e -> card.setEffect(normalShadow));

        // 2. Header with Icon
        StackPane header = new StackPane();
        header.setPrefHeight(100);
        header.setMinHeight(100);

        String backgroundColor;
        FontAwesomeSolid iconType;
        String iconColor;

        if (FileUtils.isImage(ext)) {
            backgroundColor = "#d6bcfa";
            iconType = FontAwesomeSolid.IMAGE;
            iconColor = "#805ad5";
        } else if (ext.equals("pdf")) {
            backgroundColor = "#fed7d7";
            iconType = FontAwesomeSolid.FILE_PDF;
            iconColor = "#e53e3e";
        } else if (FileUtils.isAudio(ext)) {
            backgroundColor = "#bee3f8";
            iconType = FontAwesomeSolid.MUSIC;
            iconColor = "#3182ce";
        } else if (FileUtils.isVideo(ext)) {
            backgroundColor = "#fbb6ce";
            iconType = FontAwesomeSolid.VIDEO;
            iconColor = "#d53f8c";
        } else if (ext.equals("zip") || ext.equals("rar") || ext.equals("7z")) {
            backgroundColor = "#feebc8";
            iconType = FontAwesomeSolid.FILE_ARCHIVE;
            iconColor = "#d69e2e";
        } else if (ext.equals("doc") || ext.equals("docx")) {
            backgroundColor = "#bee3f8";
            iconType = FontAwesomeSolid.FILE_WORD;
            iconColor = "#3182ce";
        } else if (ext.equals("xls") || ext.equals("xlsx")) {
            backgroundColor = "#c6f6d5";
            iconType = FontAwesomeSolid.FILE_EXCEL;
            iconColor = "#38a169";
        } else {
            backgroundColor = "#e2e8f0";
            iconType = FontAwesomeSolid.FILE_ALT;
            iconColor = "#4a5568";
        }

        header.setStyle("-fx-background-radius: 15 15 0 0; -fx-background-color: " + backgroundColor + ";");

        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(40);
        icon.setIconColor(Color.web(iconColor));

        header.getChildren().add(icon);

        // 3. Details Section
        VBox details = new VBox(4);
        details.setPadding(new Insets(10));
        details.setAlignment(Pos.CENTER_LEFT);

        // File name with tooltip
        String truncatedName = FileUtils.truncateFileName(filename, 20);
        Label nameLabel = new Label(truncatedName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2d3748;");
        nameLabel.setMaxWidth(180);
        Tooltip nameTip = new Tooltip(filename);
        nameTip.getStyleClass().add("modern-tooltip");
        Tooltip.install(nameLabel, nameTip);

        // Metadata row (type • size)
        HBox metaRow = new HBox(6);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        
        Label typeLabel = new Label(ext.toUpperCase());
        typeLabel.getStyleClass().add("meta-type");
        typeLabel.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 4; -fx-padding: 2 6; -fx-font-size: 10px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
        
        Label sepLabel = new Label("•");
        sepLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 8px;");
        
        Label sizeLabel = new Label(FileUtils.formatSize(String.valueOf(file.getFileSize())));
        sizeLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        
        metaRow.getChildren().addAll(typeLabel, sepLabel, sizeLabel);

        // 4. Action Buttons Row
        HBox actionRow = new HBox(6);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(5, 0, 0, 0));
        actionRow.getStyleClass().add("action-buttons");

        // Favorite button (already favorite - red heart)
        Button favBtn = createCardActionButton(FontAwesomeSolid.HEART, "#e74c3c", "Remove from Favorites");
        favBtn.setOnAction(e -> {
            e.consume();
            removeFavorite(file);
        });

        // Download button
        Button downloadBtn = createCardActionButton(FontAwesomeSolid.DOWNLOAD, "#3498db", "Download File");
        downloadBtn.setOnAction(e -> {
            e.consume();
            downloadFile(file);
        });

        // Share button (uses enhanced dialog with user search)
        Button shareBtn = createCardActionButton(FontAwesomeSolid.SHARE_ALT, "#9b59b6", "Share File");
        shareBtn.setOnAction(e -> {
            e.consume();
            openShareDialog(file);
        });

        // Preview button
        Button previewBtn = createCardActionButton(FontAwesomeSolid.EYE, "#27ae60", "Preview File");
        previewBtn.setOnAction(e -> {
            e.consume();
            handleFileClick(file);
        });

        actionRow.getChildren().addAll(favBtn, downloadBtn, shareBtn, previewBtn);

        details.getChildren().addAll(nameLabel, metaRow, actionRow);
        card.getChildren().addAll(header, details);

        // Card click opens preview
        card.setOnMouseClicked(e -> {
            if (!e.isConsumed()) handleFileClick(file);
        });

        return card;
    }

    /**
     * Creates a compact action button for cards
     */
    private Button createCardActionButton(FontAwesomeSolid iconType, String color, String tooltipText) {
        Button btn = new Button();
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5;");
        
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(14);
        icon.setIconColor(Color.web(color));
        btn.setGraphic(icon);
        
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.getStyleClass().add("modern-tooltip");
        btn.setTooltip(tooltip);
        
        // Hover effect
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5;"));
        
        return btn;
    }

    /**
     * Downloads the file to user's download directory
     */
    private void downloadFile(FileMetadata file) {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null || minioClient == null) return;

        String bucketName = currentUser.getBucketName();
        if (bucketName == null || bucketName.isEmpty()) {
            bucketName = (currentUser.getFirstName() + "-" + currentUser.getLastName())
                    .toLowerCase().replaceAll("[^a-z0-9-]", "");
        }

        Preferences prefs = Preferences.userNodeForPackage(SettingsController.class);
        String downloadDir = prefs.get("download_path", System.getProperty("user.home") + File.separator + "Downloads");
        File destination = new File(downloadDir, file.getFilename());

        final String finalBucket = bucketName;
        Thread downloadThread = new Thread(() -> {
            try {
                minioClient.downloadObject(
                    DownloadObjectArgs.builder()
                        .bucket(finalBucket)
                        .object(file.getFilename())
                        .filename(destination.getAbsolutePath())
                        .build()
                );
                Platform.runLater(() -> AlertUtils.showSuccess("Download Complete", "Saved to: " + destination.getAbsolutePath()));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> AlertUtils.showError("Download Failed", e.getMessage()));
            }
        });
        downloadThread.setDaemon(true);
        downloadThread.start();
    }

    /**
     * Removes file from favorites
     */
    private void removeFavorite(FileMetadata file) {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            AlertUtils.showError("Error", "User session not found");
            return;
        }

        if (file == null || file.getFilename() == null) {
            AlertUtils.showError("Error", "Invalid file");
            return;
        }

        String bucketName = currentUser.getBucketName();
        if (bucketName == null || bucketName.isEmpty()) {
            bucketName = (currentUser.getFirstName() + "-" + currentUser.getLastName())
                    .toLowerCase().replaceAll("[^a-z0-9-]", "");
        }

        final String finalBucket = bucketName;
        new Thread(() -> {
            try {
                boolean success = FileDAO.setFavorite(currentUser.getId(), file.getFilename(), false, finalBucket, file.getFileSize());
                Platform.runLater(() -> {
                    if (success) {
                        SessionManager.setFavoritesChanged(true);
                        refresh(); // Reload the favorites list
                        AlertUtils.showSuccess("Removed", file.getFilename() + " removed from favorites.");
                    } else {
                        AlertUtils.showError("Error", "Could not remove from favorites.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    AlertUtils.showError("Error", "Failed to remove from favorites: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Opens the enhanced share dialog with user search functionality
     */
    private void openShareDialog(FileMetadata file) {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;

        String bucketName = currentUser.getBucketName();
        if (bucketName == null || bucketName.isEmpty()) {
            bucketName = (currentUser.getFirstName() + "-" + currentUser.getLastName())
                    .toLowerCase().replaceAll("[^a-z0-9-]", "");
        }

        // Use the enhanced share dialog with user search
        com.cloudstorage.fx.components.FileCardFactory.openShareDialogEnhanced(file, bucketName);
    }

    /**
     * Gère le clic sur un fichier favori pour afficher la prévisualisation média
     * Supporte : Images, PDF, Vidéos, Audio et autres documents
     */
    private void handleFileClick(FileMetadata file) {
        if (file == null || minioClient == null) {
            return;
        }

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        // Obtenir le nom du bucket de l'utilisateur
        String bucketName = currentUser.getBucketName();
        if (bucketName == null || bucketName.isEmpty()) {
            bucketName = (currentUser.getFirstName() + "-" + currentUser.getLastName())
                    .toLowerCase()
                    .replaceAll("[^a-z0-9-]", "");
        }

        // Afficher le dialogue de prévisualisation
        MediaPreviewDialog.showPreview(file.getFilename(), bucketName, minioClient);
    }
}