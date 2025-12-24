package com.cloudstorage.fx.components;

import com.cloudstorage.config.MinioConfig;
import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.fx.controllers.dialogs.ShareDialogEnhancedController;
import com.cloudstorage.fx.controllers.views.SettingsController;
import com.cloudstorage.fx.utils.AlertUtils;
import com.cloudstorage.fx.utils.FileUtils;
import com.cloudstorage.model.FileMetadata;
import com.cloudstorage.model.User;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Factory class for creating consistent file cards across all interfaces.
 * This ensures visual and functional consistency between:
 * - My Cloud / Recent Files
 * - Favorites
 * - Folder View
 * - Shared Files
 * 
 * Design reference: SharedDashboardController
 */
public class FileCardFactory {

    // Icon colors by file type
    private static final String COLOR_IMAGE = "#9b59b6";
    private static final String COLOR_VIDEO = "#e84393";
    private static final String COLOR_AUDIO = "#3498db";
    private static final String COLOR_PDF = "#e74c3c";
    private static final String COLOR_WORD = "#2b5797";
    private static final String COLOR_EXCEL = "#217346";
    private static final String COLOR_ARCHIVE = "#f39c12";
    private static final String COLOR_DEFAULT = "#7f8c8d";

    /**
     * Creates a horizontal file card (row) - for list views like My Cloud/Recent Files
     * Design matches SharedDashboardController.createFileCard()
     */
    public static HBox createFileRow(FileMetadata file, String bucketName, MinioClient minioClient, 
                                      Consumer<FileMetadata> onPreviewClick, Runnable onRefresh) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefHeight(75);
        card.getStyleClass().add("shared-file-card");
        card.setPadding(new Insets(12, 20, 12, 20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-cursor: hand;");

        // Shadow effect
        DropShadow shadow = new DropShadow(8, Color.web("#0000001a"));
        shadow.setOffsetY(2);
        card.setEffect(shadow);

        // Hover effects with animation
        card.setOnMouseEntered(e -> {
            DropShadow hoverShadow = new DropShadow(12, Color.web("#0984e340"));
            hoverShadow.setOffsetY(4);
            card.setEffect(hoverShadow);
            card.setStyle("-fx-background-color: #fafbfc; -fx-background-radius: 12; -fx-cursor: hand;");
        });
        card.setOnMouseExited(e -> {
            card.setEffect(shadow);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-cursor: hand;");
        });

        String filename = file.getFilename();
        String ext = FileUtils.getFileExtension(filename);

        // --- File Icon ---
        Label iconLabel = createFileIcon(ext);

        // --- File Info VBox ---
        VBox infoBox = new VBox(3);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // File name with tooltip
        Label nameLabel = new Label(FileUtils.truncateFileName(filename, 30));
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");
        Tooltip nameTip = new Tooltip(filename);
        nameTip.getStyleClass().add("modern-tooltip");
        Tooltip.install(nameLabel, nameTip);

        // Metadata row (type • size)
        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label typeLabel = new Label(ext.toUpperCase());
        typeLabel.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 4; -fx-padding: 2 6; " +
                          "-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-font-weight: bold;");

        Label sepLabel = new Label("•");
        sepLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 10px;");

        Label sizeLabel = new Label(FileUtils.formatSize(String.valueOf(file.getFileSize())));
        sizeLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        metaRow.getChildren().addAll(typeLabel, sepLabel, sizeLabel);
        infoBox.getChildren().addAll(nameLabel, metaRow);

        // --- Action Buttons ---
        boolean isFavorite = isFavorite(file.getFilename());
        HBox actionBox = createActionButtons(file, bucketName, minioClient, isFavorite, onRefresh);

        card.getChildren().addAll(iconLabel, infoBox, actionBox);

        // Click to preview (but not on buttons)
        card.setOnMouseClicked(e -> {
            if (!(e.getTarget() instanceof Button) && onPreviewClick != null) {
                onPreviewClick.accept(file);
            }
        });

        return card;
    }

    /**
     * Creates a vertical file card (grid) - for grid views like Favorites, FolderView
     * Design matches SharedDashboardController style
     */
    public static VBox createFileCard(FileMetadata file, String bucketName, MinioClient minioClient,
                                       Consumer<FileMetadata> onPreviewClick, Runnable onRefresh) {
        String filename = file.getFilename();
        String ext = FileUtils.getFileExtension(filename);
        boolean isFavorite = isFavorite(filename);

        // Card Container
        VBox card = new VBox();
        card.getStyleClass().addAll("grid-item", "favorites-card");
        card.setPrefWidth(200);
        card.setPrefHeight(230);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15;");

        // Shadow effect
        DropShadow normalShadow = new DropShadow(8, Color.web("#0000001a"));
        normalShadow.setOffsetY(2);
        card.setEffect(normalShadow);

        // Hover effects with smooth transition
        card.setOnMouseEntered(e -> {
            DropShadow hoverShadow = new DropShadow(15, Color.web("#0984e333"));
            hoverShadow.setOffsetY(4);
            card.setEffect(hoverShadow);
            card.setStyle("-fx-background-color: #fafbfc; -fx-background-radius: 15;");
        });
        card.setOnMouseExited(e -> {
            card.setEffect(normalShadow);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 15;");
        });

        // Header with Icon
        StackPane header = createCardHeader(ext);
        header.setOnMouseClicked(e -> {
            if (onPreviewClick != null) {
                onPreviewClick.accept(file);
            }
        });

        // Details Section
        VBox details = new VBox(4);
        details.setPadding(new Insets(10));
        details.setAlignment(Pos.TOP_LEFT);

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
        typeLabel.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 4; -fx-padding: 2 6; " +
                          "-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-font-weight: bold;");

        Label sepLabel = new Label("•");
        sepLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 8px;");

        Label sizeLabel = new Label(FileUtils.formatSize(String.valueOf(file.getFileSize())));
        sizeLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        metaRow.getChildren().addAll(typeLabel, sepLabel, sizeLabel);

        // Action Buttons Row
        HBox actionRow = createCardActionButtons(file, bucketName, minioClient, isFavorite, onRefresh);

        details.getChildren().addAll(nameLabel, metaRow, actionRow);
        card.getChildren().addAll(header, details);

        // Card click opens preview
        card.setOnMouseClicked(e -> {
            if (!e.isConsumed() && onPreviewClick != null) {
                onPreviewClick.accept(file);
            }
        });

        return card;
    }

    /**
     * Creates the file type icon with modern styling (for row cards)
     */
    private static Label createFileIcon(String ext) {
        Label iconLabel = new Label();
        iconLabel.setMinWidth(48);
        iconLabel.setMinHeight(48);
        iconLabel.setPrefWidth(48);
        iconLabel.setPrefHeight(48);
        iconLabel.setAlignment(Pos.CENTER);

        FontAwesomeSolid iconType;
        String iconColor;
        String bgColor;

        if (FileUtils.isImage(ext)) {
            iconType = FontAwesomeSolid.IMAGE;
            iconColor = "#ffffff";
            bgColor = COLOR_IMAGE;
        } else if (ext.equals("pdf")) {
            iconType = FontAwesomeSolid.FILE_PDF;
            iconColor = "#e74c3c";
            bgColor = "#fdecea";
        } else if (FileUtils.isAudio(ext)) {
            iconType = FontAwesomeSolid.MUSIC;
            iconColor = "#ffffff";
            bgColor = COLOR_AUDIO;
        } else if (FileUtils.isVideo(ext)) {
            iconType = FontAwesomeSolid.VIDEO;
            iconColor = "#ffffff";
            bgColor = COLOR_VIDEO;
        } else if (ext.equals("doc") || ext.equals("docx")) {
            iconType = FontAwesomeSolid.FILE_WORD;
            iconColor = "#2b5797";
            bgColor = "#e8f0fe";
        } else if (ext.equals("xls") || ext.equals("xlsx")) {
            iconType = FontAwesomeSolid.FILE_EXCEL;
            iconColor = "#217346";
            bgColor = "#e6f4ea";
        } else if (ext.equals("zip") || ext.equals("rar") || ext.equals("7z")) {
            iconType = FontAwesomeSolid.FILE_ARCHIVE;
            iconColor = "#f39c12";
            bgColor = "#fef9e7";
        } else {
            iconType = FontAwesomeSolid.FILE_ALT;
            iconColor = COLOR_DEFAULT;
            bgColor = "#ecf0f1";
        }

        FontIcon fontIcon = new FontIcon(iconType);
        fontIcon.setIconSize(20);
        fontIcon.setIconColor(Color.web(iconColor));

        iconLabel.setGraphic(fontIcon);
        iconLabel.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12;");

        return iconLabel;
    }

    /**
     * Creates the card header with icon (for grid cards)
     */
    private static StackPane createCardHeader(String ext) {
        StackPane header = new StackPane();
        header.setPrefHeight(100);
        header.setMinHeight(100);
        header.setStyle("-fx-cursor: hand;");

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

        header.setStyle("-fx-background-radius: 15 15 0 0; -fx-background-color: " + backgroundColor + "; -fx-cursor: hand;");

        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(40);
        icon.setIconColor(Color.web(iconColor));
        header.getChildren().add(icon);

        return header;
    }

    /**
     * Creates action buttons for row cards (horizontal layout)
     */
    private static HBox createActionButtons(FileMetadata file, String bucketName, MinioClient minioClient,
                                            boolean isFavorite, Runnable onRefresh) {
        HBox actionBox = new HBox(6);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.getStyleClass().add("action-buttons");

        // Favorite Button
        Button favoriteBtn = createActionButton(FontAwesomeSolid.HEART,
                isFavorite ? "#e74c3c" : "#cbd5e1",
                isFavorite ? "Remove from Favorites" : "Add to Favorites");
        favoriteBtn.getStyleClass().add("action-btn-favorite");
        if (isFavorite) favoriteBtn.getStyleClass().add("is-favorite");
        favoriteBtn.setOnAction(e -> {
            e.consume();
            toggleFavorite(file, bucketName, favoriteBtn, onRefresh);
        });

        // Download Button
        Button downloadBtn = createActionButton(FontAwesomeSolid.DOWNLOAD, "#3498db", "Download");
        downloadBtn.getStyleClass().add("action-btn-download");
        downloadBtn.setOnAction(e -> {
            e.consume();
            downloadFile(file, bucketName, minioClient);
        });

        // Share Button
        Button shareBtn = createActionButton(FontAwesomeSolid.SHARE_ALT, "#9b59b6", "Share");
        shareBtn.setOnAction(e -> {
            e.consume();
            openShareDialogEnhanced(file, bucketName);
        });

        // More Options Button
        Button moreBtn = createActionButton(FontAwesomeSolid.ELLIPSIS_H, "#94a3b8", "More options");
        moreBtn.getStyleClass().add("action-btn-more");
        moreBtn.setOnAction(e -> {
            e.consume();
            showOptionsMenu(moreBtn, file, bucketName, onRefresh);
        });

        actionBox.getChildren().addAll(favoriteBtn, downloadBtn, shareBtn, moreBtn);
        return actionBox;
    }

    /**
     * Creates action buttons for grid cards (compact layout)
     */
    private static HBox createCardActionButtons(FileMetadata file, String bucketName, MinioClient minioClient,
                                                 boolean isFavorite, Runnable onRefresh) {
        HBox actionRow = new HBox(6);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(5, 0, 0, 0));
        actionRow.getStyleClass().add("action-buttons");

        // Favorite Button
        Button favBtn = createCompactActionButton(FontAwesomeSolid.HEART,
                isFavorite ? "#e74c3c" : "#bdc3c7",
                isFavorite ? "Remove from Favorites" : "Add to Favorites");
        if (isFavorite) favBtn.getStyleClass().add("is-favorite");
        favBtn.setOnAction(e -> {
            e.consume();
            toggleFavorite(file, bucketName, favBtn, onRefresh);
        });

        // Download Button
        Button downloadBtn = createCompactActionButton(FontAwesomeSolid.DOWNLOAD, "#3498db", "Download File");
        downloadBtn.setOnAction(e -> {
            e.consume();
            downloadFile(file, bucketName, minioClient);
        });

        // Share Button
        Button shareBtn = createCompactActionButton(FontAwesomeSolid.SHARE_ALT, "#9b59b6", "Share File");
        shareBtn.setOnAction(e -> {
            e.consume();
            openShareDialogEnhanced(file, bucketName);
        });

        // Preview Button
        Button previewBtn = createCompactActionButton(FontAwesomeSolid.EYE, "#27ae60", "Preview File");
        previewBtn.setOnAction(e -> {
            e.consume();
            if (minioClient != null && bucketName != null) {
                MediaPreviewDialog.showPreview(file.getFilename(), bucketName, minioClient);
            }
        });

        actionRow.getChildren().addAll(favBtn, downloadBtn, shareBtn, previewBtn);
        return actionRow;
    }

    /**
     * Creates a standard action button with icon
     */
    private static Button createActionButton(FontAwesomeSolid icon, String color, String tooltipText) {
        Button btn = new Button();
        btn.getStyleClass().add("inline-action-btn");
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(14);
        fontIcon.setIconColor(Color.web(color));
        btn.setGraphic(fontIcon);
        btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6;");

        Tooltip tip = new Tooltip(tooltipText);
        tip.getStyleClass().add("modern-tooltip");
        btn.setTooltip(tip);

        // Hover effect
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6;"));

        return btn;
    }

    /**
     * Creates a compact action button for grid cards
     */
    private static Button createCompactActionButton(FontAwesomeSolid iconType, String color, String tooltipText) {
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
     * Shows the options popup menu
     */
    private static void showOptionsMenu(Button parent, FileMetadata file, String bucketName, Runnable onRefresh) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        VBox menuBox = new VBox(5);
        menuBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 3);");

        // Preview option
        Button previewBtn = createMenuOption(FontAwesomeSolid.EYE, "Preview", "#27ae60");
        previewBtn.setOnAction(e -> {
            popup.hide();
            MinioClient minioClient = MinioConfig.getClient();
            if (minioClient != null) {
                MediaPreviewDialog.showPreview(file.getFilename(), bucketName, minioClient);
            }
        });

        // Share option
        Button shareBtn = createMenuOption(FontAwesomeSolid.SHARE_ALT, "Share with others", "#3498db");
        shareBtn.setOnAction(e -> {
            popup.hide();
            openShareDialogEnhanced(file, bucketName);
        });

        // Move to folder option
        Button moveBtn = createMenuOption(FontAwesomeSolid.FOLDER_PLUS, "Move to folder", "#f39c12");
        moveBtn.setOnAction(e -> {
            popup.hide();
            moveToFolder(file, bucketName, onRefresh);
        });

        menuBox.getChildren().addAll(previewBtn, shareBtn, moveBtn);

        // Fade in animation
        FadeTransition fade = new FadeTransition(Duration.millis(150), menuBox);
        fade.setFromValue(0);
        fade.setToValue(1);

        popup.getContent().add(menuBox);
        var bounds = parent.localToScreen(parent.getBoundsInLocal());
        popup.show(parent, bounds.getMinX() - 100, bounds.getMaxY() + 5);
        fade.play();
    }

    private static Button createMenuOption(FontAwesomeSolid icon, String text, String color) {
        Button btn = new Button(text);
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(14);
        fontIcon.setIconColor(Color.web(color));
        btn.setGraphic(fontIcon);
        btn.setGraphicTextGap(10);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2c3e50; -fx-cursor: hand; " +
                "-fx-font-size: 13px; -fx-alignment: CENTER_LEFT; -fx-padding: 8 15;");
        btn.setPrefWidth(180);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #2c3e50; " +
                "-fx-cursor: hand; -fx-font-size: 13px; -fx-alignment: CENTER_LEFT; -fx-padding: 8 15; -fx-background-radius: 5;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2c3e50; " +
                "-fx-cursor: hand; -fx-font-size: 13px; -fx-alignment: CENTER_LEFT; -fx-padding: 8 15;"));
        return btn;
    }

    // --- ACTION HANDLERS ---

    /**
     * Check if file is in favorites
     */
    private static boolean isFavorite(String filename) {
        User user = SessionManager.getCurrentUser();
        if (user == null) return false;
        List<String> favorites = FileDAO.getFavoriteFilenames(user.getId());
        return favorites.contains(filename);
    }

    /**
     * Toggle favorite status with visual feedback
     */
    private static void toggleFavorite(FileMetadata file, String bucketName, Button favoriteBtn, Runnable onRefresh) {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            AlertUtils.showError("Error", "User session not found");
            return;
        }

        if (file == null || file.getFilename() == null) {
            AlertUtils.showError("Error", "Invalid file");
            return;
        }

        boolean currentFav = isFavorite(file.getFilename());
        boolean newState = !currentFav;

        // Update UI immediately with animation
        FontIcon icon = (FontIcon) favoriteBtn.getGraphic();
        if (icon != null) {
            icon.setIconColor(Color.web(newState ? "#e74c3c" : "#cbd5e1"));
        }
        favoriteBtn.setTooltip(new Tooltip(newState ? "Remove from Favorites" : "Add to Favorites"));

        // Scale animation for feedback
        ScaleTransition scale = new ScaleTransition(Duration.millis(150), favoriteBtn);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.2);
        scale.setToY(1.2);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();

        // Update database in background
        new Thread(() -> {
            try {
                boolean success = FileDAO.setFavorite(user.getId(), file.getFilename(), newState, bucketName, file.getFileSize());
                Platform.runLater(() -> {
                    if (success) {
                        SessionManager.setFavoritesChanged(true);
                        if (onRefresh != null) onRefresh.run();
                        AlertUtils.showSuccess(newState ? "Added to Favorites" : "Removed from Favorites", file.getFilename());
                    } else {
                        // Revert UI on failure
                        if (icon != null) {
                            icon.setIconColor(Color.web(currentFav ? "#e74c3c" : "#cbd5e1"));
                        }
                        AlertUtils.showError("Error", "Could not update favorites");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    // Revert UI on error
                    if (icon != null) {
                        icon.setIconColor(Color.web(currentFav ? "#e74c3c" : "#cbd5e1"));
                    }
                    AlertUtils.showError("Error", "Failed to update favorites: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Download file to user's download directory
     */
    private static void downloadFile(FileMetadata file, String bucketName, MinioClient minioClient) {
        if (minioClient == null || bucketName == null) {
            AlertUtils.showError("Error", "Not connected to file server.");
            return;
        }

        Preferences prefs = Preferences.userNodeForPackage(SettingsController.class);
        String downloadDir = prefs.get("download_path", System.getProperty("user.home") + File.separator + "Downloads");
        File destination = new File(downloadDir, file.getFilename());

        Thread downloadThread = new Thread(() -> {
            try {
                minioClient.downloadObject(
                        DownloadObjectArgs.builder()
                                .bucket(bucketName)
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
     * Opens the enhanced share dialog with user search
     */
    public static void openShareDialogEnhanced(FileMetadata file, String bucketName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    FileCardFactory.class.getResource("/com/cloudstorage/fx/dialogs/ShareFileDialogEnhanced.fxml")
            );
            Parent root = loader.load();

            ShareDialogEnhancedController controller = loader.getController();
            controller.setTargetFile(file.getFilename(), bucketName);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setTitle("Share File");
            stage.setScene(new Scene(root));

            // Add fade-in animation
            root.setOpacity(0);
            stage.show();
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtils.showError("Error", "Could not open share dialog.");
        }
    }

    /**
     * Opens the enhanced share dialog with filename only (for backward compatibility)
     */
    public static void openShareDialogEnhanced(String filename, String bucketName) {
        FileMetadata file = new FileMetadata();
        file.setFilename(filename);
        openShareDialogEnhanced(file, bucketName);
    }

    /**
     * Move file to a folder
     */
    private static void moveToFolder(FileMetadata file, String bucketName, Runnable onRefresh) {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            AlertUtils.showError("Error", "You must be logged in.");
            return;
        }

        try {
            // Load the modern Add to Folder Dialog
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                FileCardFactory.class.getResource("/com/cloudstorage/fx/dialogs/AddToFolderDialog.fxml")
            );
            javafx.scene.Parent root = loader.load();

            com.cloudstorage.fx.controllers.dialogs.AddToFolderDialogController controller = loader.getController();

            // Create and configure the stage
            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            dialogStage.setTitle("Move to Folder");

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.getStylesheets().add(
                FileCardFactory.class.getResource("/css/dialogs.css").toExternalForm()
            );

            dialogStage.setScene(scene);
            controller.setStage(dialogStage);
            controller.setFileName(file.getFilename());
            controller.setOnSuccess(onRefresh);

            dialogStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("Dialog Error", "Could not open folder selection dialog.");
        }
    }
}
