package com.cloudstorage.fx.components;

import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.fx.utils.AlertUtils;
import com.cloudstorage.fx.utils.FileUtils;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class FileRowFactory {

    public static HBox createRow(Map<String, String> fileData, Consumer<Map<String, String>> onClick) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(65.0);
        row.setSpacing(15.0);
        row.getStyleClass().add("recent-item");
        row.setPadding(new Insets(0, 15, 0, 15));

        // Click Listener
        row.setOnMouseClicked(e -> onClick.accept(fileData));

        // Icon
        String truncatedName = FileUtils.truncateFileName(fileData.get("name"), 25);
        String ext = FileUtils.getFileExtension(truncatedName);
        Label icon = new Label();
        icon.getStyleClass().add("recent-icon");
        icon.setMinWidth(35);
        icon.setAlignment(Pos.CENTER);

        // Determine icon type and style based on file extension
        FontAwesomeSolid iconType;
        String iconColor;
        String bgClass;

        if (FileUtils.isImage(ext)) {
            System.out.println("Image file detected: " + truncatedName);
            iconType = FontAwesomeSolid.CAMERA;
            iconColor = "#805ad5";
            bgClass = "icon-bg-purple";
        } else if (ext.equals("mp3") || ext.equals("wav")) {
            iconType = FontAwesomeSolid.MICROPHONE;
            iconColor = "#3182ce";
            bgClass = "icon-bg-blue";
        } else if (ext.equals("mp4") || ext.equals("avi") || ext.equals("mkv")) {
            iconType = FontAwesomeSolid.VIDEO;
            iconColor = "#d53f8c";
            bgClass = "icon-bg-pink";
        } else if (ext.equals("pdf")) {
            iconType = FontAwesomeSolid.FILE_PDF;
            iconColor = "#e53e3e";
            bgClass = "icon-bg-red";
        } else if (ext.equals("zip") || ext.equals("rar") || ext.equals("7z")) {
            iconType = FontAwesomeSolid.FILE_ARCHIVE;
            iconColor = "#d69e2e";
            bgClass = "icon-bg-orange";
        } else if (ext.equals("doc") || ext.equals("docx")) {
            iconType = FontAwesomeSolid.FILE_WORD;
            iconColor = "#3182ce";
            bgClass = "icon-bg-blue";
        } else if (ext.equals("xls") || ext.equals("xlsx")) {
            iconType = FontAwesomeSolid.FILE_EXCEL;
            iconColor = "#38a169";
            bgClass = "icon-bg-green";
        } else {
            System.out.println("Default file type with ext: : " + ext);
            iconType = FontAwesomeSolid.FILE;
            iconColor = "#4a5568";
            bgClass = "icon-bg-green";
        }

        // Create and configure FontIcon
        FontIcon fileIcon = new FontIcon(iconType);
        fileIcon.setIconSize(20);
        fileIcon.setIconColor(Color.web(iconColor));

        icon.setGraphic(fileIcon);
        icon.getStyleClass().add(bgClass);

        // File Name Label
        Label nameLabel = new Label(truncatedName);
        nameLabel.getStyleClass().add("recent-name");
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setEllipsisString("...");
        nameLabel.setWrapText(false);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Type & Size Labels
        Label typeLabel = new Label(ext.toUpperCase() + " file");
        typeLabel.getStyleClass().add("recent-meta");
        typeLabel.setMaxWidth(80);
        typeLabel.setEllipsisString("...");
        typeLabel.setWrapText(false);

        Label sizeLabel = new Label(FileUtils.formatSize(fileData.get("size")));
        sizeLabel.getStyleClass().add("recent-meta");
        sizeLabel.setMaxWidth(80);
        sizeLabel.setEllipsisString("...");
        sizeLabel.setWrapText(false);

        // Buttons
        Button linkBtn = new Button();
        FontIcon linkIcon = new FontIcon(FontAwesomeSolid.LINK);
        linkIcon.setIconSize(14);
        linkBtn.setGraphic(linkIcon);
        linkBtn.getStyleClass().add("icon-btn-link");
        linkBtn.setOnMouseClicked(e -> {
            e.consume();
            System.out.println("Copy link for: " + truncatedName);
        });

        Button moreBtn = new Button("•••");
        moreBtn.getStyleClass().add("icon-btn-more");

        moreBtn.setOnMouseClicked(e -> {
            e.consume();
            Bounds bounds = moreBtn.localToScreen(moreBtn.getBoundsInLocal());
            // Pass 'fileData' to the next method
            showFileMenu(moreBtn, bounds.getMinX(), bounds.getMinY() - 40, truncatedName, fileData);
        });

        // Shadow Effect
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#0000000d"));
        shadow.setOffsetY(3.0);
        row.setEffect(shadow);

        // Assemble
        row.getChildren().addAll(icon, nameLabel, spacer, typeLabel, sizeLabel, linkBtn, moreBtn);

        return row;
    }

    public static void showFileMenu(Node parent, double x, double y, String truncatedName, Map<String, String> fileData) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        HBox menuBox = new HBox(8);
        menuBox.getStyleClass().add("file-menu-horizontal");

        menuBox.getStylesheets().add(
                Objects.requireNonNull(FileRowFactory.class.getResource("/css/Dashboard.css")).toExternalForm()
        );

        // 1. Get current status (Ensure your DB/Map has this key set to "true" or "false")
        // If the map doesn't have the key, default to false.
        boolean isFavorite = Boolean.parseBoolean(fileData.getOrDefault("is_favorite", "false"));
        System.out.println("Is Favorite: " + isFavorite);
        // 2. Dynamic Tooltip Text
        String favTooltip = isFavorite ? "Remove from Favorites" : "Add to Favorites";

        // 3. Create Button with Toggle Logic
        // We pass '!isFavorite' to the handler so it knows to flip the state (True -> False, False -> True)
        Button favoriteBtn = createMenuButton(FontAwesomeSolid.STAR, favTooltip,
                () -> addToFavorite(fileData, !isFavorite), popup);

        // 4. Apply CSS Class if it is currently a favorite (turns the star gold/yellow)
        if (isFavorite) {
            favoriteBtn.getStyleClass().add("is_favorite");
        }

        Button downloadBtn = createMenuButton(FontAwesomeSolid.DOWNLOAD, "Download",
                () -> System.out.println("Download: " + truncatedName), popup);

        Button copyBtn = createMenuButton(FontAwesomeSolid.LINK, "Copy Link",
                () -> System.out.println("Copy Link: " + truncatedName), popup);

        Button folderBtn = createMenuButton(FontAwesomeSolid.FOLDER_PLUS, "Add to Folder",
                () -> System.out.println("Add Folder: " + truncatedName), popup);

        menuBox.getChildren().addAll(favoriteBtn, downloadBtn, copyBtn, folderBtn);

        popup.getContent().add(menuBox);
        popup.show(parent, x, y);
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

    // Logic to toggle favorite status
// Logic to toggle favorite status
    private static void addToFavorite(Map<String, String> fileData, boolean makeFavorite) {
        // 1. Get Current User
        var user = SessionManager.getCurrentUser();
        if (user == null) return;

        String fileName = fileData.get("name");
        String bucketName = fileData.get("bucket");

        // --- STEP 1: INSTANT VISUAL UPDATE (Optimistic) ---
        // Update the internal data immediately
        fileData.put("is_favorite", String.valueOf(makeFavorite));
        SessionManager.setFavoritesChanged(true);

        // --- STEP 2: START BACKGROUND SAVE ---
        // We start the database save NOW, but we don't wait for it to finish.
        Thread dbThread = new Thread(() -> {
            boolean success = FileDAO.setFavorite(user.getId(), fileName, makeFavorite, bucketName);

            // ONLY notify the user if it FAILS (Rollback)
            if (!success) {
                Platform.runLater(() -> {
                    // Revert the star icon
                    fileData.put("is_favorite", String.valueOf(!makeFavorite));
                    // Show error message
                    AlertUtils.showError("Sync Error", "Could not save changes to database.");
                });
            }
        });
        dbThread.setDaemon(true);
        dbThread.start();

        // --- STEP 3: SHOW SUCCESS ALERT IMMEDIATELY ---
        // We show this *while* the database is still saving in the background.
        // This removes the delay completely.
        String actionText = makeFavorite ? "added to" : "removed from";
        AlertUtils.showSuccess(
                makeFavorite ? "Added to Favorites" : "Removed from Favorites",
                fileName + " was " + actionText + " your favorites."
        );
    }}