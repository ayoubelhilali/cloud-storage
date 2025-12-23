package com.cloudstorage.fx.components;

import com.cloudstorage.config.MinioConfig;
import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.fx.utils.AlertUtils;
import com.cloudstorage.fx.utils.FileUtils;
import com.cloudstorage.fx.controllers.ShareDialogController;
import com.cloudstorage.model.FileMetadata;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.List;

public class FileRowFactory {

    /**
     * Creates a professional file row with icon, name, metadata, and action buttons.
     * Features: Download button, Favorite (heart) button, Options menu (•••)
     * Enhanced with tooltips, hover effects, and metadata display.
     * Design matches SharedDashboardController for visual consistency.
     */
    public static HBox createRow(Map<String, String> fileData, Consumer<Map<String, String>> onClick, Runnable refreshFoldersCallback) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(75.0);
        row.setSpacing(15.0);
        row.getStyleClass().addAll("recent-item", "shared-file-card");
        row.setPadding(new Insets(12, 20, 12, 20));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 12;");

        // Enhanced shadow effect (matches SharedDashboardController)
        DropShadow shadow = new DropShadow(8, Color.web("#0000001a"));
        shadow.setOffsetY(2);
        row.setEffect(shadow);

        // Click handler for preview
        row.setOnMouseClicked(e -> {
            if (!e.isConsumed()) onClick.accept(fileData);
        });

        // --- Icon Logic ---
        String fileName = fileData.get("name");
        String truncatedName = FileUtils.truncateFileName(fileName, 25);
        String ext = FileUtils.getFileExtension(truncatedName);
        Label icon = new Label();
        icon.getStyleClass().add("recent-icon");
        icon.setMinWidth(48);
        icon.setPrefWidth(48);
        icon.setMinHeight(48);
        icon.setPrefHeight(48);
        icon.setAlignment(Pos.CENTER);

        FontAwesomeSolid iconType;
        String iconColor;
        String bgClass;

        if (FileUtils.isImage(ext)) {
            iconType = FontAwesomeSolid.IMAGE;
            iconColor = "white";
            bgClass = "icon-bg-purple";
        } else if (FileUtils.isAudio(ext)) {
            iconType = FontAwesomeSolid.MUSIC;
            iconColor = "white";
            bgClass = "icon-bg-blue";
        } else if (FileUtils.isVideo(ext)) {
            iconType = FontAwesomeSolid.VIDEO;
            iconColor = "white";
            bgClass = "icon-bg-pink";
        } else if (ext.equals("pdf")) {
            iconType = FontAwesomeSolid.FILE_PDF;
            iconColor = "white";
            bgClass = "icon-bg-red";
        } else if (ext.equals("zip") || ext.equals("rar") || ext.equals("7z")) {
            iconType = FontAwesomeSolid.FILE_ARCHIVE;
            iconColor = "white";
            bgClass = "icon-bg-orange";
        } else if (ext.equals("doc") || ext.equals("docx")) {
            iconType = FontAwesomeSolid.FILE_WORD;
            iconColor = "white";
            bgClass = "icon-bg-blue";
        } else if (ext.equals("xls") || ext.equals("xlsx")) {
            iconType = FontAwesomeSolid.FILE_EXCEL;
            iconColor = "white";
            bgClass = "icon-bg-green";
        } else {
            iconType = FontAwesomeSolid.FILE_ALT;
            iconColor = "white";
            bgClass = "icon-bg-gray";
        }

        FontIcon fileIcon = new FontIcon(iconType);
        fileIcon.setIconSize(18);
        fileIcon.setIconColor(Color.web(iconColor));
        icon.setGraphic(fileIcon);
        icon.getStyleClass().add(bgClass);

        // --- Name and Metadata VBox ---
        VBox nameBox = new VBox(2);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        Label nameLabel = new Label(truncatedName);
        nameLabel.getStyleClass().add("recent-name");
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setEllipsisString("...");
        nameLabel.setWrapText(false);
        
        // Tooltip with full filename
        Tooltip fileNameTooltip = new Tooltip(fileName);
        fileNameTooltip.getStyleClass().add("modern-tooltip");
        Tooltip.install(nameLabel, fileNameTooltip);

        // --- Metadata Row (date, type, size) ---
        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        // File type
        Label typeLabel = new Label(ext.toUpperCase());
        typeLabel.getStyleClass().addAll("file-meta", "meta-type");

        // File size
        Label sizeLabel = new Label(FileUtils.formatSize(fileData.get("size")));
        sizeLabel.getStyleClass().addAll("file-meta", "meta-size");

        // Separator dot
        Label sep1 = new Label("•");
        sep1.getStyleClass().add("meta-separator");
        
        Label sep2 = new Label("•");
        sep2.getStyleClass().add("meta-separator");

        // Date (current date or from fileData if available)
        String dateStr = fileData.getOrDefault("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        Label dateLabel = new Label(dateStr);
        dateLabel.getStyleClass().addAll("file-meta", "meta-date");

        metaRow.getChildren().addAll(typeLabel, sep1, sizeLabel, sep2, dateLabel);
        nameBox.getChildren().addAll(nameLabel, metaRow);

        // --- Spacer ---
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.SOMETIMES);

        // --- Action Buttons Container ---
        HBox actionButtons = new HBox(8);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);
        actionButtons.getStyleClass().add("action-buttons");

        // 1. Favorite Button (Heart)
        boolean isFavorite = Boolean.parseBoolean(fileData.getOrDefault("is_favorite", "false"));
        Button favoriteBtn = createInlineActionButton(
            isFavorite ? FontAwesomeSolid.HEART : FontAwesomeSolid.HEART,
            isFavorite ? "Remove from Favorites" : "Add to Favorites",
            isFavorite ? "#e74c3c" : "#bdc3c7"
        );
        favoriteBtn.getStyleClass().add("action-btn-favorite");
        if (isFavorite) favoriteBtn.getStyleClass().add("is-favorite");
        
        favoriteBtn.setOnAction(e -> {
            e.consume();
            boolean newState = !Boolean.parseBoolean(fileData.getOrDefault("is_favorite", "false"));
            addToFavorite(fileData, newState, favoriteBtn);
        });

        // 2. Download Button
        Button downloadBtn = createInlineActionButton(
            FontAwesomeSolid.DOWNLOAD,
            "Download File",
            "#3498db"
        );
        downloadBtn.getStyleClass().add("action-btn-download");
        downloadBtn.setOnAction(e -> {
            e.consume();
            downloadFile(fileData);
        });

        // 3. Share Button (NEW - uses enhanced dialog)
        Button shareBtn = createInlineActionButton(
            FontAwesomeSolid.SHARE_ALT,
            "Share File",
            "#9b59b6"
        );
        shareBtn.setOnAction(e -> {
            e.consume();
            openShareDialog(fileData);
        });

        // 4. Options Button (•••)
        Button moreBtn = new Button("•••");
        moreBtn.getStyleClass().add("action-btn-more");
        Tooltip moreTooltip = new Tooltip("More Options");
        moreTooltip.getStyleClass().add("modern-tooltip");
        moreBtn.setTooltip(moreTooltip);

        moreBtn.setOnMouseClicked(e -> {
            e.consume();
            Bounds bounds = moreBtn.localToScreen(moreBtn.getBoundsInLocal());
            showFileMenu(moreBtn, bounds.getMinX(), bounds.getMinY() - 40, truncatedName, fileData, refreshFoldersCallback);
        });

        actionButtons.getChildren().addAll(favoriteBtn, downloadBtn, shareBtn, moreBtn);

        // Enhanced hover effect (matches SharedDashboardController)
        row.setOnMouseEntered(e -> {
            DropShadow hoverShadow = new DropShadow(12, Color.web("#0984e340"));
            hoverShadow.setOffsetY(4);
            row.setEffect(hoverShadow);
            row.setStyle("-fx-background-color: #fafbfc; -fx-background-radius: 12;");
        });
        
        row.setOnMouseExited(e -> {
            DropShadow normalShadow = new DropShadow(8, Color.web("#0000001a"));
            normalShadow.setOffsetY(2);
            row.setEffect(normalShadow);
            row.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        });

        row.getChildren().addAll(icon, nameBox, spacer, actionButtons);

        return row;
    }

    /**
     * Creates an inline action button with icon, tooltip, and hover effects.
     */
    private static Button createInlineActionButton(FontAwesomeSolid iconType, String tooltipText, String iconColor) {
        Button btn = new Button();
        btn.getStyleClass().add("inline-action-btn");
        
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(16);
        icon.setIconColor(Color.web(iconColor));
        btn.setGraphic(icon);
        
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.getStyleClass().add("modern-tooltip");
        btn.setTooltip(tooltip);
        
        return btn;
    }

    public static void showFileMenu(Node parent, double x, double y, String truncatedName, Map<String, String> fileData,Runnable refreshFoldersCallback) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        HBox menuBox = new HBox(8);
        menuBox.getStyleClass().add("file-menu-horizontal");
        menuBox.getStylesheets().add(Objects.requireNonNull(FileRowFactory.class.getResource("/css/Dashboard.css")).toExternalForm());

        // 0. Preview Button (NEW - Prévisualisation média)
        Button previewBtn = createMenuButton(FontAwesomeSolid.EYE, "Preview File",
                () -> openMediaPreview(fileData), popup);

        // 1. Favorite Button
        boolean isFavorite = Boolean.parseBoolean(fileData.getOrDefault("is_favorite", "false"));
        String favTooltip = isFavorite ? "Remove from Favorites" : "Add to Favorites";
        Button favoriteBtn = createMenuButton(FontAwesomeSolid.STAR, favTooltip,
                () -> {}, popup); // Action définie après
        
        // Appliquer le style initial selon l'état favori
        updateFavoriteButtonStyle(favoriteBtn, isFavorite);
        
        // Action du bouton favori avec feedback visuel
        favoriteBtn.setOnAction(e -> {
            boolean newFavoriteState = !Boolean.parseBoolean(fileData.getOrDefault("is_favorite", "false"));
            addToFavorite(fileData, newFavoriteState, favoriteBtn);
            popup.hide();
        });

        // 2. Share Button
        Button shareBtn = createMenuButton(FontAwesomeSolid.SHARE_ALT, "Share File",
                () -> openShareDialog(fileData), popup);

        // 3. Download Button
        Button downloadBtn = createMenuButton(FontAwesomeSolid.DOWNLOAD, "Download",
                () -> downloadFile(fileData), popup);

        // 4. Add to Folder Button
        Button folderBtn = createMenuButton(FontAwesomeSolid.FOLDER_PLUS, "Add to Folder",
                () -> addToFolder(fileData,refreshFoldersCallback), popup);

        menuBox.getChildren().addAll(previewBtn, favoriteBtn, shareBtn, downloadBtn, folderBtn);

        popup.getContent().add(menuBox);
        popup.show(parent, x, y);
    }

    /**
     * Ouvre le dialogue de prévisualisation média
     * Supporte : Images, PDF, Vidéos, Audio et autres documents
     */
    private static void openMediaPreview(Map<String, String> fileData) {
        String fileName = fileData.get("name");
        String bucket = fileData.get("bucket");

        if (fileName == null || bucket == null) {
            AlertUtils.showError("Erreur", "Informations du fichier manquantes.");
            return;
        }

        io.minio.MinioClient client = MinioConfig.getClient();
        if (client == null) {
            AlertUtils.showError("Erreur", "Connexion au serveur non établie.");
            return;
        }

        MediaPreviewDialog.showPreview(fileName, bucket, client);
    }

    private static Button createMenuButton(Ikon iconCode, String tooltipText, Runnable action, Popup popup) {
        Button btn = new Button();
        FontIcon icon = new FontIcon(iconCode);
        btn.setGraphic(icon);
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.getStyleClass().add("modern-tooltip");
        btn.setTooltip(tooltip);
        btn.setOnAction(e -> {
            action.run();
            popup.hide();
        });
        return btn;
    }

    // --- Opens the Enhanced Share Dialog with user search ---
    private static void openShareDialog(Map<String, String> fileData) {
        String filename = fileData.get("name");
        String bucket = fileData.get("bucket");
        
        // Use the enhanced share dialog with user search functionality
        FileCardFactory.openShareDialogEnhanced(filename, bucket);
    }

    private static void addToFolder(Map<String, String> fileData,Runnable refreshCallback) {
        var user = SessionManager.getCurrentUser();
        if (user == null) return;

        String fileName = fileData.get("name");

        // 1. Fetch existing folders from the database
        // We run this on a background thread but need the result for the UI dialog
        new Thread(() -> {
            try {
                List<Map<String, Object>> folders = FileDAO.getFoldersByUserId(user.getId());
                Platform.runLater(() -> {
                    if (folders.isEmpty()) {
                        AlertUtils.showError("No Folders", "Please create a folder first from the dashboard.");
                        return;
                    }

                    // 2. Create a list of folder names for the choice dialog
                    List<String> folderNames = folders.stream()
                            .map(f -> (String) f.get("name"))
                            .collect(Collectors.toList());

                    ChoiceDialog<String> dialog = new ChoiceDialog<>(folderNames.get(0), folderNames);
                    dialog.setTitle("Move to Folder");
                    dialog.setHeaderText("Select destination for: " + fileName);
                    dialog.setContentText("Choose Folder:");

                    dialog.showAndWait().ifPresent(selectedFolderName -> {
                        // Find the ID of the selected folder
                        Long selectedFolderId = folders.stream()
                                .filter(f -> f.get("name").equals(selectedFolderName))
                                .map(f -> (Long) f.get("id"))
                                .findFirst()
                                .orElse(null);

                        if (selectedFolderId != null) {
                            // Pass the extra parameters: bucket and size
                            updateFileFolderInDb(
                                    user.getId(),
                                    fileName,
                                    selectedFolderId,
                                    fileData.get("bucket"),
                                    fileData.get("size"),
                                    refreshCallback
                            );
                        }
                    });
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtils.showError("Error", "Could not load folders."));
            }
        }).start();
    }

    private static void updateFileFolderInDb(long userId, String fileName, long folderId, String bucket, String sizeStr,Runnable refreshCallback) {
        long size = 0;
        try { size = Long.parseLong(sizeStr); } catch (Exception ignored) {}

        final long finalSize = size;
        Thread dbThread = new Thread(() -> {
            // Updated call to handle the new "Upsert" logic
            boolean success = FileDAO.updateFileFolder(userId, fileName, folderId, bucket, finalSize);

            Platform.runLater(() -> {
                if (success) {
                    AlertUtils.showSuccess("Moved", fileName + " moved to folder.");
                    if (refreshCallback != null) {
                        refreshCallback.run();
                    }
                } else {
                    AlertUtils.showError("Database Error", "Check console for SQL errors.");
                }
            });
        });
        dbThread.setDaemon(true);
        dbThread.start();
    }

    private static void downloadFile(Map<String, String> fileData) {
        String fileName = fileData.get("name");
        String bucketName = fileData.get("bucket");

        if (fileName == null || bucketName == null) {
            AlertUtils.showError("Download Error", "File information is missing.");
            return;
        }

        // 1. Retrieve the download path from Preferences (Set in SettingsController)
        Preferences prefs = Preferences.userNodeForPackage(com.cloudstorage.fx.controllers.SettingsController.class);
        String defaultPath = System.getProperty("user.home") + File.separator + "Downloads";
        String downloadDir = prefs.get("download_path", defaultPath);

        // 2. Prepare the destination file
        File destination = new File(downloadDir, fileName);

        // 3. Run download in a background thread to prevent UI freezing
        Thread downloadThread = new Thread(() -> {
            try {
                MinioClient minioClient = MinioConfig.getClient();

                minioClient.downloadObject(
                        DownloadObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fileName)
                                .filename(destination.getAbsolutePath())
                                .build());

                // 4. Show success on the JavaFX UI Thread
                Platform.runLater(() ->
                        AlertUtils.showSuccess("Download Complete", "Saved to: " + destination.getAbsolutePath())
                );

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        AlertUtils.showError("Download Failed", "Could not download " + fileName + ": " + e.getMessage())
                );
            }
        });

        downloadThread.setDaemon(true);
        downloadThread.start();
    }

    /**
     * Met à jour le style visuel du bouton favori
     */
    private static void updateFavoriteButtonStyle(Button favoriteBtn, boolean isFavorite) {
        FontIcon icon = (FontIcon) favoriteBtn.getGraphic();
        if (isFavorite) {
            favoriteBtn.getStyleClass().add("is_favorite");
            icon.setIconColor(Color.web("#FFD700")); // Or/jaune pour favori
        } else {
            favoriteBtn.getStyleClass().remove("is_favorite");
            icon.setIconColor(Color.web("#718096")); // Gris par défaut
        }
        
        // Mettre à jour le tooltip
        String tooltip = isFavorite ? "Remove from Favorites" : "Add to Favorites";
        favoriteBtn.setTooltip(new Tooltip(tooltip));
    }

    private static void addToFavorite(Map<String, String> fileData, boolean makeFavorite, Button favoriteBtn) {
        var user = SessionManager.getCurrentUser();
        if (user == null) return;
        String fileName = fileData.get("name");
        String bucketName = fileData.get("bucket");

        // Parse the file size from the fileData
        long fileSize = 0;
        try {
            String sizeStr = fileData.get("size");
            if (sizeStr != null && !sizeStr.isEmpty()) {
                fileSize = Long.parseLong(sizeStr);
            }
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse file size: " + e.getMessage());
        }

        // Mise à jour immédiate du style du bouton (feedback visuel)
        Platform.runLater(() -> updateFavoriteButtonStyle(favoriteBtn, makeFavorite));

        fileData.put("is_favorite", String.valueOf(makeFavorite));
        SessionManager.setFavoritesChanged(true);

        final long finalFileSize = fileSize;
        Thread dbThread = new Thread(() -> {
            boolean success = FileDAO.setFavorite(user.getId(), fileName, makeFavorite, bucketName, finalFileSize);
            if (!success) {
                Platform.runLater(() -> {
                    fileData.put("is_favorite", String.valueOf(!makeFavorite));
                    // Annuler le feedback visuel en cas d'erreur
                    updateFavoriteButtonStyle(favoriteBtn, !makeFavorite);
                    AlertUtils.showError("Sync Error", "Could not save changes to database.");
                });
            }
        });
        dbThread.setDaemon(true);
        dbThread.start();

        AlertUtils.showSuccess(
                makeFavorite ? "Added to Favorites" : "Removed from Favorites",
                fileName + " was updated."
        );
    }
}