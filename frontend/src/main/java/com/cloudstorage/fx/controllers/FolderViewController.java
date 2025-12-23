package com.cloudstorage.fx.controllers;

import com.cloudstorage.config.MinioConfig;
import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.fx.components.MediaPreviewDialog;
import com.cloudstorage.fx.utils.AlertUtils;
import com.cloudstorage.fx.utils.FileUtils;
import com.cloudstorage.model.FileMetadata;
import com.cloudstorage.model.User;
import io.minio.DownloadObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Contrôleur pour l'affichage du contenu d'un dossier.
 * Permet la navigation dans les dossiers et l'upload de fichiers.
 */
public class FolderViewController {

    @FXML private Label folderNameLabel;
    @FXML private Label fileCountLabel;
    @FXML private FlowPane filesGrid;
    @FXML private VBox loaderContainer;
    @FXML private VBox emptyContainer;
    @FXML private Button backButton;
    @FXML private Button uploadButton;

    private Long folderId;
    private String folderName;
    private MinioClient minioClient;
    private String userBucket;
    private Consumer<Void> onBackCallback;

    @FXML
    public void initialize() {
        this.minioClient = MinioConfig.getClient();
    }

    /**
     * Configure le dossier à afficher
     */
    public void setFolder(Long folderId, String folderName, String userBucket) {
        this.folderId = folderId;
        this.folderName = folderName;
        this.userBucket = userBucket;

        folderNameLabel.setText(folderName);
        loadFolderFiles();
    }

    /**
     * Définit le callback pour le retour
     */
    public void setOnBackCallback(Consumer<Void> callback) {
        this.onBackCallback = callback;
    }

    /**
     * Charge les fichiers du dossier depuis la base de données
     */
    private void loadFolderFiles() {
        User user = SessionManager.getCurrentUser();
        if (user == null || folderId == null) return;

        // Afficher le loader
        showLoader(true);
        filesGrid.getChildren().clear();
        emptyContainer.setVisible(false);

        Task<List<FileMetadata>> task = new Task<>() {
            @Override
            protected List<FileMetadata> call() throws Exception {
                return FileDAO.getFilesByFolderId(folderId, user.getId());
            }
        };

        task.setOnSucceeded(e -> {
            showLoader(false);
            List<FileMetadata> files = task.getValue();

            fileCountLabel.setText(files.size() + " file" + (files.size() != 1 ? "s" : ""));

            if (files.isEmpty()) {
                emptyContainer.setVisible(true);
            } else {
                for (FileMetadata file : files) {
                    // Use FileCardFactory for consistent design across all interfaces
                    VBox card = com.cloudstorage.fx.components.FileCardFactory.createFileCard(
                        file, 
                        userBucket, 
                        minioClient,
                        f -> MediaPreviewDialog.showPreview(f.getFilename(), userBucket, minioClient),
                        this::loadFolderFiles
                    );
                    filesGrid.getChildren().add(card);
                }
            }
        });

        task.setOnFailed(e -> {
            showLoader(false);
            emptyContainer.setVisible(true);
            System.err.println("Error loading folder files: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    /**
     * Crée une carte visuelle professionnelle pour un fichier
     * Inclut : icône, nom, taille, type et boutons d'action (Favorite, Download, Share)
     */
    private VBox createFileCard(FileMetadata file) {
        String filename = file.getFilename();
        String ext = FileUtils.getFileExtension(filename);
        User currentUser = SessionManager.getCurrentUser();

        // Vérifier si le fichier est favori
        boolean isFavorite = false;
        if (currentUser != null) {
            List<String> favorites = FileDAO.getFavoriteFilenames(currentUser.getId());
            isFavorite = favorites.contains(filename);
        }
        final boolean[] isFav = {isFavorite};

        // Card Container
        VBox card = new VBox();
        card.getStyleClass().add("grid-item");
        card.setPrefWidth(180);
        card.setPrefHeight(220);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        
        // Shadow effect
        DropShadow normalShadow = new DropShadow(8, Color.web("#0000001a"));
        normalShadow.setOffsetY(2);
        card.setEffect(normalShadow);

        // Hover effects
        card.setOnMouseEntered(e -> {
            DropShadow hoverShadow = new DropShadow(15, Color.web("#0984e340"));
            hoverShadow.setOffsetY(4);
            card.setEffect(hoverShadow);
            card.setStyle("-fx-background-color: #fafbfc; -fx-background-radius: 12;");
        });
        card.setOnMouseExited(e -> {
            card.setEffect(normalShadow);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        });

        // Header avec icône
        StackPane header = new StackPane();
        header.setPrefHeight(90);
        header.setMinHeight(90);

        String backgroundColor;
        FontAwesomeSolid iconType;
        String iconColor;

        if (FileUtils.isImage(ext)) {
            backgroundColor = "#d6bcfa";
            iconType = FontAwesomeSolid.IMAGE;
            iconColor = "#805ad5";
        } else if (FileUtils.isVideo(ext)) {
            backgroundColor = "#fbb6ce";
            iconType = FontAwesomeSolid.VIDEO;
            iconColor = "#d53f8c";
        } else if (FileUtils.isAudio(ext)) {
            backgroundColor = "#bee3f8";
            iconType = FontAwesomeSolid.MUSIC;
            iconColor = "#3182ce";
        } else if (ext.equals("pdf")) {
            backgroundColor = "#fed7d7";
            iconType = FontAwesomeSolid.FILE_PDF;
            iconColor = "#e53e3e";
        } else if (ext.equals("doc") || ext.equals("docx")) {
            backgroundColor = "#bee3f8";
            iconType = FontAwesomeSolid.FILE_WORD;
            iconColor = "#3182ce";
        } else if (ext.equals("xls") || ext.equals("xlsx")) {
            backgroundColor = "#c6f6d5";
            iconType = FontAwesomeSolid.FILE_EXCEL;
            iconColor = "#38a169";
        } else if (ext.equals("zip") || ext.equals("rar") || ext.equals("7z")) {
            backgroundColor = "#feebc8";
            iconType = FontAwesomeSolid.FILE_ARCHIVE;
            iconColor = "#d69e2e";
        } else {
            backgroundColor = "#e2e8f0";
            iconType = FontAwesomeSolid.FILE_ALT;
            iconColor = "#718096";
        }

        header.setStyle("-fx-background-radius: 12 12 0 0; -fx-background-color: " + backgroundColor + "; -fx-cursor: hand;");

        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(36);
        icon.setIconColor(Color.web(iconColor));
        header.getChildren().add(icon);

        // Click on header for preview
        header.setOnMouseClicked(e -> {
            if (minioClient != null && userBucket != null) {
                MediaPreviewDialog.showPreview(filename, userBucket, minioClient);
            }
        });

        // Details Section
        VBox details = new VBox(4);
        details.setPadding(new Insets(10));
        details.setAlignment(Pos.TOP_LEFT);

        // File name with tooltip
        String truncatedName = FileUtils.truncateFileName(filename, 18);
        Label nameLabel = new Label(truncatedName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2d3748;");
        Tooltip nameTip = new Tooltip(filename);
        nameTip.getStyleClass().add("modern-tooltip");
        Tooltip.install(nameLabel, nameTip);

        // Metadata row (type • size)
        HBox metaRow = new HBox(5);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label typeLabel = new Label(ext.toUpperCase());
        typeLabel.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 4; -fx-padding: 1 5; -fx-font-size: 9px; -fx-text-fill: #64748b; -fx-font-weight: bold;");

        Label sepLabel = new Label("•");
        sepLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 8px;");

        Label sizeLabel = new Label(FileUtils.formatSize(String.valueOf(file.getFileSize())));
        sizeLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");

        metaRow.getChildren().addAll(typeLabel, sepLabel, sizeLabel);

        // Action Buttons Row
        HBox actionRow = new HBox(6);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(5, 0, 0, 0));
        actionRow.getStyleClass().add("action-buttons");

        // Favorite Button (Heart)
        Button favBtn = createActionButton(FontAwesomeSolid.HEART, isFav[0] ? "#e74c3c" : "#bdc3c7", 
                isFav[0] ? "Remove from Favorites" : "Add to Favorites");
        if (isFav[0]) favBtn.getStyleClass().add("is-favorite");
        
        favBtn.setOnAction(e -> {
            e.consume();
            toggleFavorite(file, favBtn, isFav);
        });

        // Download Button
        Button downloadBtn = createActionButton(FontAwesomeSolid.DOWNLOAD, "#3498db", "Download File");
        downloadBtn.setOnAction(e -> {
            e.consume();
            downloadFile(file);
        });

        // Share Button
        Button shareBtn = createActionButton(FontAwesomeSolid.SHARE_ALT, "#9b59b6", "Share File");
        shareBtn.setOnAction(e -> {
            e.consume();
            openShareDialog(file);
        });

        // Preview Button
        Button previewBtn = createActionButton(FontAwesomeSolid.EYE, "#27ae60", "Preview File");
        previewBtn.setOnAction(e -> {
            e.consume();
            if (minioClient != null && userBucket != null) {
                MediaPreviewDialog.showPreview(filename, userBucket, minioClient);
            }
        });

        actionRow.getChildren().addAll(favBtn, downloadBtn, shareBtn, previewBtn);

        details.getChildren().addAll(nameLabel, metaRow, actionRow);
        card.getChildren().addAll(header, details);

        return card;
    }

    /**
     * Creates an action button with icon and tooltip
     */
    private Button createActionButton(FontAwesomeSolid iconType, String color, String tooltipText) {
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
        String hoverBg = "-fx-background-color: #f1f5f9; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5;";
        String normalBg = "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5;";
        btn.setOnMouseEntered(e -> btn.setStyle(hoverBg));
        btn.setOnMouseExited(e -> btn.setStyle(normalBg));

        return btn;
    }

    /**
     * Toggle favorite status
     */
    private void toggleFavorite(FileMetadata file, Button favBtn, boolean[] isFav) {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        boolean newState = !isFav[0];
        
        // Update UI immediately
        FontIcon icon = (FontIcon) favBtn.getGraphic();
        icon.setIconColor(Color.web(newState ? "#e74c3c" : "#bdc3c7"));
        favBtn.setTooltip(new Tooltip(newState ? "Remove from Favorites" : "Add to Favorites"));

        // Update database in background
        new Thread(() -> {
            boolean success = FileDAO.setFavorite(user.getId(), file.getFilename(), newState, userBucket, file.getFileSize());
            Platform.runLater(() -> {
                if (success) {
                    isFav[0] = newState;
                    SessionManager.setFavoritesChanged(true);
                    AlertUtils.showSuccess(newState ? "Added to Favorites" : "Removed from Favorites", file.getFilename());
                } else {
                    // Revert UI on failure
                    icon.setIconColor(Color.web(isFav[0] ? "#e74c3c" : "#bdc3c7"));
                    AlertUtils.showError("Error", "Could not update favorites");
                }
            });
        }).start();
    }

    /**
     * Download file to user's download directory
     */
    private void downloadFile(FileMetadata file) {
        if (minioClient == null || userBucket == null) return;

        Preferences prefs = Preferences.userNodeForPackage(SettingsController.class);
        String downloadDir = prefs.get("download_path", System.getProperty("user.home") + File.separator + "Downloads");
        File destination = new File(downloadDir, file.getFilename());

        Thread downloadThread = new Thread(() -> {
            try {
                minioClient.downloadObject(
                    DownloadObjectArgs.builder()
                        .bucket(userBucket)
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
     * Open share dialog for the file (uses enhanced dialog with user search)
     */
    private void openShareDialog(FileMetadata file) {
        // Use the enhanced share dialog with user search
        com.cloudstorage.fx.components.FileCardFactory.openShareDialogEnhanced(file, userBucket);
    }

    /**
     * Gère l'upload de fichiers dans le dossier
     */
    @FXML
    private void handleUpload() {
        User user = SessionManager.getCurrentUser();
        if (user == null || minioClient == null || userBucket == null) {
            AlertUtils.showError("Error", "Not connected");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Files to Upload");
        List<File> files = fileChooser.showOpenMultipleDialog(uploadButton.getScene().getWindow());

        if (files == null || files.isEmpty()) return;

        for (File file : files) {
            uploadFile(file, user);
        }
    }

    /**
     * Upload un fichier vers MinIO et l'enregistre dans la base de données
     */
    private void uploadFile(File file, User user) {
        Task<Void> uploadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String fileName = file.getName();
                String mimeType = URLConnection.guessContentTypeFromName(fileName);
                if (mimeType == null) mimeType = "application/octet-stream";

                // Upload vers MinIO
                try (FileInputStream fis = new FileInputStream(file)) {
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(userBucket)
                                    .object(fileName)
                                    .stream(fis, file.length(), -1)
                                    .contentType(mimeType)
                                    .build()
                    );
                }

                // Enregistrer dans la base de données avec le folderId
                FileMetadata metadata = new FileMetadata(
                        user.getId(),
                        fileName,
                        fileName,
                        file.length(),
                        mimeType,
                        FileUtils.getFileExtension(fileName),
                        fileName,
                        userBucket
                );
                metadata.setFolderId(folderId);
                FileDAO.saveFileRecord(metadata);

                return null;
            }
        };

        uploadTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                AlertUtils.showSuccess("Upload Complete", file.getName() + " uploaded successfully!");
                loadFolderFiles(); // Rafraîchir la liste
            });
        });

        uploadTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                AlertUtils.showError("Upload Failed", uploadTask.getException().getMessage());
            });
        });

        new Thread(uploadTask).start();
    }

    /**
     * Retour à la vue précédente
     */
    @FXML
    private void handleBack() {
        if (onBackCallback != null) {
            onBackCallback.accept(null);
        }
    }

    /**
     * Affiche ou cache le loader
     */
    private void showLoader(boolean show) {
        loaderContainer.setVisible(show);
        loaderContainer.setManaged(show);
    }
}
