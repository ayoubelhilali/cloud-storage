package com.cloudstorage.fx.controllers;

import com.cloudstorage.config.MinioConfig;
import com.cloudstorage.config.SessionManager;
import com.cloudstorage.controller.ShareController;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.database.ShareDAO;
import com.cloudstorage.fx.components.MediaPreviewDialog;
import com.cloudstorage.fx.utils.AlertUtils;
import com.cloudstorage.fx.utils.FileUtils;
import com.cloudstorage.model.SharedFileInfo;
import com.cloudstorage.model.User;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SharedDashboardController {

    @FXML private VBox sharedFilesContainer;

    // --- Stats Labels from FXML ---
    @FXML private Label lblSharedImages;
    @FXML private Label lblSharedVideos;
    @FXML private Label lblSharedAudio;
    @FXML private Label lblSharedDocs;

    private ShareController shareController;
    private ShareDAO shareDAO;
    private MinioClient minioClient;

    @FXML
    public void initialize() {
        try {
            // Initialize MinIO client using global config
            this.minioClient = MinioConfig.getClient();
            this.shareController = new ShareController(minioClient);
            this.shareDAO = new ShareDAO();

            // Load Data
            loadSharedFiles();

        } catch (Exception e) {
            System.err.println("Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadSharedFiles() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null || shareDAO == null) return;

        long userId = currentUser.getId();

        // Fetch data on a background thread
        new Thread(() -> {
            try {
                List<SharedFileInfo> files = shareDAO.getFilesSharedWithUserEnhanced(userId);
                Platform.runLater(() -> updateUI(files));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Could not load shared files."));
            }
        }).start();
    }

    private void updateUI(List<SharedFileInfo> files) {
        sharedFilesContainer.getChildren().clear();

        // Counters for the stats cards
        int imgCount = 0, vidCount = 0, audioCount = 0, docCount = 0;

        if (files.isEmpty()) {
            Label emptyLabel = new Label("No files shared with you.");
            emptyLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-padding: 10; -fx-font-size: 14px;");
            sharedFilesContainer.getChildren().add(emptyLabel);
        } else {
            for (SharedFileInfo file : files) {
                String ext = FileUtils.getFileExtension(file.getFilename());

                // Categorize for stats
                if (FileUtils.isImage(ext)) imgCount++;
                else if (FileUtils.isVideo(ext)) vidCount++;
                else if (FileUtils.isAudio(ext)) audioCount++;
                else docCount++;

                // Add card to container
                sharedFilesContainer.getChildren().add(createFileCard(file));
            }
        }

        // Update the top statistics labels
        if (lblSharedImages != null) lblSharedImages.setText(String.valueOf(imgCount));
        if (lblSharedVideos != null) lblSharedVideos.setText(String.valueOf(vidCount));
        if (lblSharedAudio != null) lblSharedAudio.setText(String.valueOf(audioCount));
        if (lblSharedDocs != null) lblSharedDocs.setText(String.valueOf(docCount));
    }

    /**
     * Creates a professional file card with modern design
     * Features: Icon, file info, sender info, action buttons (Download, Favorite, Share)
     */
    private HBox createFileCard(SharedFileInfo file) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefHeight(80);
        card.getStyleClass().add("shared-file-card");
        card.setPadding(new Insets(12, 20, 12, 20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-cursor: hand;");
        
        // Shadow effect
        DropShadow shadow = new DropShadow(8, Color.web("#0000001a"));
        shadow.setOffsetY(2);
        card.setEffect(shadow);

        // Hover effects
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

        String name = file.getFilename();
        String ext = FileUtils.getFileExtension(name);

        // --- File Icon ---
        Label iconLabel = createFileIcon(ext);

        // --- File Info VBox ---
        VBox infoBox = new VBox(3);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // File name with tooltip
        Label nameLabel = new Label(FileUtils.truncateFileName(name, 30));
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");
        Tooltip nameTip = new Tooltip(name);
        nameTip.getStyleClass().add("modern-tooltip");
        Tooltip.install(nameLabel, nameTip);

        // Metadata row (type • size)
        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        
        Label typeLabel = new Label(ext.toUpperCase());
        typeLabel.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 4; -fx-padding: 2 6; -fx-font-size: 10px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
        
        Label sepLabel = new Label("•");
        sepLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 10px;");
        
        Label sizeLabel = new Label(FileUtils.formatSize(String.valueOf(file.getFileSize())));
        sizeLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        
        metaRow.getChildren().addAll(typeLabel, sepLabel, sizeLabel);

        // Sender info with icon
        HBox senderRow = new HBox(6);
        senderRow.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon userIcon = new FontIcon(FontAwesomeSolid.USER_CIRCLE);
        userIcon.setIconSize(12);
        userIcon.setIconColor(Color.web("#94a3b8"));
        
        String senderInfo = file.getSenderName() != null ? file.getSenderName().trim() : "Unknown";
        Label senderLabel = new Label("Shared by " + senderInfo);
        senderLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        
        // Date
        Label dateLabel = new Label(" • " + file.getFormattedSharedDate());
        dateLabel.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 10px;");
        
        senderRow.getChildren().addAll(userIcon, senderLabel, dateLabel);

        infoBox.getChildren().addAll(nameLabel, metaRow, senderRow);

        // --- Action Buttons ---
        HBox actionBox = createActionButtons(file);

        card.getChildren().addAll(iconLabel, infoBox, actionBox);

        // Click to preview (but not on buttons)
        card.setOnMouseClicked(e -> {
            if (!(e.getTarget() instanceof Button)) {
                handleFileClick(file);
            }
        });

        return card;
    }

    /**
     * Creates the file type icon with modern styling
     */
    private Label createFileIcon(String ext) {
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
            bgColor = "#9b59b6";
        } else if (ext.equals("pdf")) {
            iconType = FontAwesomeSolid.FILE_PDF;
            iconColor = "#e74c3c";
            bgColor = "#fdecea";
        } else if (FileUtils.isAudio(ext)) {
            iconType = FontAwesomeSolid.MUSIC;
            iconColor = "#ffffff";
            bgColor = "#3498db";
        } else if (FileUtils.isVideo(ext)) {
            iconType = FontAwesomeSolid.VIDEO;
            iconColor = "#ffffff";
            bgColor = "#e84393";
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
            iconColor = "#7f8c8d";
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
     * Creates action buttons (Preview, Favorite, Download, Share, Options)
     */
    private HBox createActionButtons(SharedFileInfo file) {
        HBox actionBox = new HBox(6);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.getStyleClass().add("action-buttons");

        // Favorite Button (Heart icon) - FIRST for visibility
        boolean isFavorite = file.isFavorite();
        Button favoriteBtn = createActionButton(FontAwesomeSolid.HEART, 
            isFavorite ? "#e74c3c" : "#cbd5e1", 
            isFavorite ? "Remove from Favorites" : "Add to Favorites");
        favoriteBtn.getStyleClass().add("action-btn-favorite");
        if (isFavorite) favoriteBtn.getStyleClass().add("is-favorite");
        favoriteBtn.setOnAction(e -> toggleFavorite(file, favoriteBtn));

        // Download Button
        Button downloadBtn = createActionButton(FontAwesomeSolid.DOWNLOAD, "#3498db", "Download");
        downloadBtn.getStyleClass().add("action-btn-download");
        downloadBtn.setOnAction(e -> handleDownload(file));

        // Share Button
        Button shareBtn = createActionButton(FontAwesomeSolid.SHARE_ALT, "#9b59b6", "Share");
        shareBtn.setOnAction(e -> handleShare(file));

        // More Options Button (Move to folder, etc.)
        Button moreBtn = createActionButton(FontAwesomeSolid.ELLIPSIS_H, "#94a3b8", "More options");
        moreBtn.getStyleClass().add("action-btn-more");
        moreBtn.setOnAction(e -> showOptionsMenu(moreBtn, file));

        actionBox.getChildren().addAll(favoriteBtn, downloadBtn, shareBtn, moreBtn);
        return actionBox;
    }

    private Button createActionButton(FontAwesomeSolid icon, String color, String tooltipText) {
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
     * Shows the options popup menu
     */
    private void showOptionsMenu(Button parent, SharedFileInfo file) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        VBox menuBox = new VBox(5);
        menuBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 10; " +
                         "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 3);");

        // Share option
        Button shareBtn = createMenuOption(FontAwesomeSolid.SHARE_ALT, "Share with others", "#3498db");
        shareBtn.setOnAction(e -> {
            popup.hide();
            handleShare(file);
        });

        // Move to folder option
        Button moveBtn = createMenuOption(FontAwesomeSolid.FOLDER_PLUS, "Move to folder", "#f39c12");
        moveBtn.setOnAction(e -> {
            popup.hide();
            handleMoveToFolder(file);
        });

        menuBox.getChildren().addAll(shareBtn, moveBtn);
        popup.getContent().add(menuBox);

        var bounds = parent.localToScreen(parent.getBoundsInLocal());
        popup.show(parent, bounds.getMinX() - 100, bounds.getMaxY() + 5);
    }

    private Button createMenuOption(FontAwesomeSolid icon, String text, String color) {
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

    // --- Action Handlers ---

    private void toggleFavorite(SharedFileInfo file, Button favoriteBtn) {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        boolean newState = !file.isFavorite();
        file.setFavorite(newState);
        
        // Update UI immediately (red = favorite, gray = not)
        FontIcon icon = (FontIcon) favoriteBtn.getGraphic();
        if (icon != null) {
            icon.setIconColor(Color.web(newState ? "#e74c3c" : "#bdc3c7"));
        }
        favoriteBtn.setTooltip(new Tooltip(newState ? "Remove from Favorites" : "Add to Favorites"));
        SessionManager.setFavoritesChanged(true);

        // Update database in background
        new Thread(() -> {
            boolean success = FileDAO.setFavorite(user.getId(), file.getFilename(), 
                    newState, file.getStorageBucket(), file.getFileSize());
            if (!success) {
                Platform.runLater(() -> {
                    file.setFavorite(!newState);
                    if (icon != null) {
                        icon.setIconColor(Color.web(!newState ? "#e74c3c" : "#bdc3c7"));
                    }
                    AlertUtils.showError("Error", "Could not update favorites.");
                });
            }
        }).start();

        AlertUtils.showSuccess(
            newState ? "Added to Favorites" : "Removed from Favorites",
            file.getFilename() + " was updated."
        );
    }

    private void handleDownload(SharedFileInfo file) {
        try {
            if (minioClient == null) {
                showError("Not connected to file server.");
                return;
            }

            // Generate presigned URL for download
            String downloadUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .bucket(file.getStorageBucket())
                    .object(file.getStorageKey() != null ? file.getStorageKey() : file.getFilename())
                    .method(Method.GET)
                    .expiry(1, TimeUnit.HOURS)
                    .build()
            );

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(downloadUrl));
            } else {
                showError("Browser not supported.");
            }
        } catch (Exception e) {
            showError("Could not download file: " + e.getMessage());
        }
    }

    private void handleShare(SharedFileInfo file) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/cloudstorage/fx/ShareFileDialogEnhanced.fxml")
            );
            javafx.scene.Parent root = loader.load();

            ShareDialogEnhancedController controller = loader.getController();
            controller.setTargetFile(file.getFilename(), file.getStorageBucket());

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            stage.setTitle("Share File");
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("Error", "Could not open share dialog.");
        }
    }

    private void handleMoveToFolder(SharedFileInfo file) {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            AlertUtils.showError("Error", "You must be logged in.");
            return;
        }

        // Fetch folders in background, then show dialog on UI thread
        new Thread(() -> {
            try {
                List<Map<String, Object>> folders = FileDAO.getFoldersByUserId(currentUser.getId());
                
                Platform.runLater(() -> {
                    if (folders.isEmpty()) {
                        // Offer to create a new folder
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("No Folders");
                        alert.setHeaderText("You don't have any folders yet.");
                        alert.setContentText("Would you like to create a new folder?");
                        
                        ButtonType createBtn = new ButtonType("Create Folder");
                        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                        alert.getButtonTypes().setAll(createBtn, cancelBtn);
                        
                        alert.showAndWait().ifPresent(response -> {
                            if (response == createBtn) {
                                showCreateFolderDialog(file);
                            }
                        });
                        return;
                    }

                    // Show folder selection dialog
                    showMoveToFolderDialog(file, folders);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> AlertUtils.showError("Error", "Could not load folders: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Shows a professional dialog to select destination folder
     */
    private void showMoveToFolderDialog(SharedFileInfo file, List<Map<String, Object>> folders) {
        // Create custom dialog
        Dialog<Long> dialog = new Dialog<>();
        dialog.setTitle("Move to Folder");
        dialog.setHeaderText("Move \"" + FileUtils.truncateFileName(file.getFilename(), 30) + "\" to:");

        // Set button types
        ButtonType moveButtonType = new ButtonType("Move", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(moveButtonType, ButtonType.CANCEL);

        // Create folder list
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #f8f9fa;");

        ToggleGroup folderGroup = new ToggleGroup();
        
        for (Map<String, Object> folder : folders) {
            String folderName = (String) folder.get("name");
            Long folderId = (Long) folder.get("id");
            Object fileCountObj = folder.get("file_count");
            String fileCount = fileCountObj != null ? fileCountObj.toString() + " files" : "";

            RadioButton rb = new RadioButton();
            rb.setToggleGroup(folderGroup);
            rb.setUserData(folderId);

            HBox folderRow = new HBox(12);
            folderRow.setAlignment(Pos.CENTER_LEFT);
            folderRow.setPadding(new Insets(10, 15, 10, 15));
            folderRow.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand;");
            
            // Folder icon
            FontIcon folderIcon = new FontIcon(FontAwesomeSolid.FOLDER);
            folderIcon.setIconSize(20);
            folderIcon.setIconColor(Color.web("#f39c12"));

            VBox nameBox = new VBox(2);
            Label nameLabel = new Label(folderName);
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");
            Label countLabel = new Label(fileCount);
            countLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;");
            nameBox.getChildren().addAll(nameLabel, countLabel);
            HBox.setHgrow(nameBox, Priority.ALWAYS);

            folderRow.getChildren().addAll(rb, folderIcon, nameBox);
            
            // Click anywhere on row to select
            folderRow.setOnMouseClicked(e -> rb.setSelected(true));
            folderRow.setOnMouseEntered(e -> folderRow.setStyle("-fx-background-color: #e8f4fc; -fx-background-radius: 8; -fx-cursor: hand;"));
            folderRow.setOnMouseExited(e -> folderRow.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand;"));

            content.getChildren().add(folderRow);
        }

        // Add "Create new folder" option
        Button newFolderBtn = new Button("+ Create New Folder");
        newFolderBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3498db; -fx-cursor: hand; -fx-font-size: 13px;");
        newFolderBtn.setOnAction(e -> {
            dialog.close();
            showCreateFolderDialog(file);
        });
        content.getChildren().add(newFolderBtn);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(250);
        scrollPane.setStyle("-fx-background: #f8f9fa; -fx-border-color: transparent;");

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setPrefWidth(400);

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == moveButtonType) {
                Toggle selected = folderGroup.getSelectedToggle();
                if (selected != null) {
                    return (Long) selected.getUserData();
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(folderId -> {
            if (folderId != null) {
                executeMoveToFolder(file, folderId);
            } else {
                AlertUtils.showError("No Selection", "Please select a folder.");
            }
        });
    }

    /**
     * Shows dialog to create a new folder
     */
    private void showCreateFolderDialog(SharedFileInfo file) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create New Folder");
        dialog.setHeaderText("Enter folder name:");
        dialog.setContentText("Folder name:");

        dialog.showAndWait().ifPresent(folderName -> {
            if (folderName.trim().isEmpty()) {
                AlertUtils.showError("Invalid Name", "Folder name cannot be empty.");
                return;
            }

            User currentUser = SessionManager.getCurrentUser();
            if (currentUser == null) return;

            new Thread(() -> {
                boolean created = FileDAO.createFolder(folderName.trim(), currentUser.getId());
                if (created) {
                    // Get the new folder ID and move the file
                    List<Map<String, Object>> folders = FileDAO.getFoldersByUserId(currentUser.getId());
                    Long newFolderId = folders.stream()
                            .filter(f -> folderName.trim().equals(f.get("name")))
                            .map(f -> (Long) f.get("id"))
                            .findFirst()
                            .orElse(null);

                    if (newFolderId != null) {
                        Platform.runLater(() -> executeMoveToFolder(file, newFolderId));
                    } else {
                        Platform.runLater(() -> AlertUtils.showSuccess("Folder Created", "Folder \"" + folderName + "\" created. You can now move files to it."));
                    }
                } else {
                    Platform.runLater(() -> AlertUtils.showError("Error", "Could not create folder."));
                }
            }).start();
        });
    }

    /**
     * Executes the move operation: copies file to user's bucket and removes from shared
     */
    private void executeMoveToFolder(SharedFileInfo file, Long folderId) {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;

        // Get user's bucket name
        String userBucket = currentUser.getBucketName();
        if (userBucket == null || userBucket.isEmpty()) {
            userBucket = (currentUser.getFirstName() + "-" + currentUser.getLastName())
                    .toLowerCase().replaceAll("[^a-z0-9-]", "");
        }

        final String targetBucket = userBucket;
        final String fileName = file.getFilename();
        final String sourceBucket = file.getStorageBucket();
        final String sourceKey = file.getStorageKey() != null ? file.getStorageKey() : fileName;

        // Show loading indicator
        AlertUtils.showSuccess("Moving...", "Moving file to your folder...");

        new Thread(() -> {
            try {
                // 1. Copy file from source bucket to user's bucket
                minioClient.copyObject(
                    CopyObjectArgs.builder()
                        .bucket(targetBucket)
                        .object(fileName)
                        .source(CopySource.builder()
                            .bucket(sourceBucket)
                            .object(sourceKey)
                            .build())
                        .build()
                );

                // 2. Add file record to user's files in database with folder
                boolean dbSuccess = FileDAO.updateFileFolder(
                    currentUser.getId(),
                    fileName,
                    folderId,
                    targetBucket,
                    file.getFileSize()
                );

                // 3. Remove from shared files (optional - depends on business logic)
                // For now, we keep it shared but user also has a copy
                boolean removeFromShared = ShareDAO.removeShare(file.getId(), currentUser.getId());

                Platform.runLater(() -> {
                    if (dbSuccess) {
                        AlertUtils.showSuccess("File Moved", 
                            "\"" + fileName + "\" has been moved to your folder successfully!");
                        // Refresh the shared files list
                        loadSharedFiles();
                    } else {
                        AlertUtils.showError("Partial Success", 
                            "File copied but database update failed. Check your folders.");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> AlertUtils.showError("Move Failed", 
                    "Could not move file: " + e.getMessage()));
            }
        }).start();
    }

    private void handleFileClick(SharedFileInfo file) {
        if (file == null || minioClient == null) return;

        String bucketName = file.getStorageBucket();
        if (bucketName == null || bucketName.isEmpty()) {
            showError("Could not determine file location.");
            return;
        }

        MediaPreviewDialog.showPreview(file.getFilename(), bucketName, minioClient);
    }

    private void showError(String message) {
        Platform.runLater(() -> AlertUtils.showError("Error", message));
    }
}