package com.cloudstorage.fx.components;

import com.cloudstorage.config.MinioConfig;
import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.fx.utils.AlertUtils;
import com.cloudstorage.fx.utils.FileUtils;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
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

        // 4. Add to Folder Button
        Button addToFolderBtn = createInlineActionButton(
            FontAwesomeSolid.FOLDER_PLUS,
            "Add to Folder",
            "#f39c12"
        );
        addToFolderBtn.getStyleClass().add("action-btn-folder");
        addToFolderBtn.setOnAction(e -> {
            e.consume();
            addToFolder(fileData, refreshFoldersCallback);
        });

        actionButtons.getChildren().addAll(favoriteBtn, downloadBtn, shareBtn, addToFolderBtn);

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

    // --- Opens the Enhanced Share Dialog with user search ---
    private static void openShareDialog(Map<String, String> fileData) {
        String filename = fileData.get("name");
        String bucket = fileData.get("bucket");
        
        // Use the enhanced share dialog with user search functionality
        FileCardFactory.openShareDialogEnhanced(filename, bucket);
    }

    private static void addToFolder(Map<String, String> fileData, Runnable refreshCallback) {
        var user = SessionManager.getCurrentUser();
        if (user == null) return;

        String fileName = fileData.get("name");

        try {
            // Load the modern Add to Folder Dialog
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                FileRowFactory.class.getResource("/com/cloudstorage/fx/AddToFolderDialog.fxml")
            );
            javafx.scene.Parent root = loader.load();

            com.cloudstorage.fx.controllers.AddToFolderDialogController controller = loader.getController();

            // Create and configure the stage
            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            dialogStage.setTitle("Move to Folder");

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.getStylesheets().add(
                FileRowFactory.class.getResource("/css/dialogs.css").toExternalForm()
            );

            dialogStage.setScene(scene);
            controller.setStage(dialogStage);
            controller.setFileName(fileName);
            controller.setOnSuccess(refreshCallback);

            dialogStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("Dialog Error", "Could not open folder selection dialog.");
        }
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


    private static void addToFavorite(Map<String, String> fileData, boolean makeFavorite, Button favoriteBtn) {
        var user = SessionManager.getCurrentUser();
        if (user == null) {
            AlertUtils.showError("Error", "User session not found");
            return;
        }

        String fileName = fileData.get("name");
        if (fileName == null || fileName.trim().isEmpty()) {
            AlertUtils.showError("Error", "Invalid file name");
            return;
        }

        String bucketName = fileData.get("bucket");
        if (bucketName == null || bucketName.trim().isEmpty()) {
            AlertUtils.showError("Error", "Invalid bucket name");
            return;
        }

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

        // Update button style immediately for visual feedback
        Platform.runLater(() -> {
            FontIcon icon = (FontIcon) favoriteBtn.getGraphic();
            if (makeFavorite) {
                favoriteBtn.getStyleClass().add("is-favorite");
                icon.setIconColor(Color.web("#e74c3c"));
            } else {
                favoriteBtn.getStyleClass().remove("is-favorite");
                icon.setIconColor(Color.web("#bdc3c7"));
            }
            favoriteBtn.setTooltip(new Tooltip(makeFavorite ? "Remove from Favorites" : "Add to Favorites"));
        });

        fileData.put("is_favorite", String.valueOf(makeFavorite));
        SessionManager.setFavoritesChanged(true);

        final long finalFileSize = fileSize;
        Thread dbThread = new Thread(() -> {
            try {
                boolean success = FileDAO.setFavorite(user.getId(), fileName, makeFavorite, bucketName, finalFileSize);
                if (!success) {
                    Platform.runLater(() -> {
                        fileData.put("is_favorite", String.valueOf(!makeFavorite));
                        // Revert visual feedback on error
                        FontIcon icon = (FontIcon) favoriteBtn.getGraphic();
                        if (!makeFavorite) {
                            favoriteBtn.getStyleClass().add("is-favorite");
                            icon.setIconColor(Color.web("#e74c3c"));
                        } else {
                            favoriteBtn.getStyleClass().remove("is-favorite");
                            icon.setIconColor(Color.web("#bdc3c7"));
                        }
                        AlertUtils.showError("Sync Error", "Could not save changes to database.");
                    });
                } else {
                    Platform.runLater(() -> {
                        AlertUtils.showSuccess(
                                makeFavorite ? "Added to Favorites" : "Removed from Favorites",
                                fileName + " was updated."
                        );
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    fileData.put("is_favorite", String.valueOf(!makeFavorite));
                    // Revert visual feedback on error
                    FontIcon icon = (FontIcon) favoriteBtn.getGraphic();
                    if (!makeFavorite) {
                        favoriteBtn.getStyleClass().add("is-favorite");
                        icon.setIconColor(Color.web("#e74c3c"));
                    } else {
                        favoriteBtn.getStyleClass().remove("is-favorite");
                        icon.setIconColor(Color.web("#bdc3c7"));
                    }
                    AlertUtils.showError("Error", "Failed to update favorites: " + e.getMessage());
                });
            }
        });
        dbThread.setDaemon(true);
        dbThread.start();
    }
}