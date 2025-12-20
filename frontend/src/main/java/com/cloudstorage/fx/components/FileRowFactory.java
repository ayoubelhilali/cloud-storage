package com.cloudstorage.fx.components;

import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.fx.utils.AlertUtils;
import com.cloudstorage.fx.utils.FileUtils;
import com.cloudstorage.fx.controllers.ShareDialogController; // Import the new controller
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
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

        row.setOnMouseClicked(e -> onClick.accept(fileData));

        // --- Icon Logic ---
        String truncatedName = FileUtils.truncateFileName(fileData.get("name"), 25);
        String ext = FileUtils.getFileExtension(truncatedName);
        Label icon = new Label();
        icon.getStyleClass().add("recent-icon");
        icon.setMinWidth(35);
        icon.setAlignment(Pos.CENTER);

        FontAwesomeSolid iconType;
        String iconColor;
        String bgClass;

        if (FileUtils.isImage(ext)) {
            iconType = FontAwesomeSolid.CAMERA;
            iconColor = "white";
            bgClass = "icon-bg-purple";
        } else if (ext.equals("mp3") || ext.equals("wav")) {
            iconType = FontAwesomeSolid.MICROPHONE;
            iconColor = "#3182ce";
            bgClass = "icon-bg-blue";
        } else if (ext.equals("mp4") || ext.equals("avi") || ext.equals("mkv")) {
            iconType = FontAwesomeSolid.VIDEO;
            iconColor = "white";
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
            iconColor = "blue";
            bgClass = "icon-bg-blue";
        } else if (ext.equals("xls") || ext.equals("xlsx")) {
            iconType = FontAwesomeSolid.FILE_EXCEL;
            iconColor = "#38a169";
            bgClass = "icon-bg-green";
        } else {
            iconType = FontAwesomeSolid.FILE;
            iconColor = "white";
            bgClass = "icon-bg-green";
        }

        FontIcon fileIcon = new FontIcon(iconType);
        fileIcon.setIconSize(20);
        fileIcon.setIconColor(Color.web(iconColor));
        icon.setGraphic(fileIcon);
        icon.getStyleClass().add(bgClass);

        // --- Labels ---
        Label nameLabel = new Label(truncatedName);
        nameLabel.getStyleClass().add("recent-name");
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setEllipsisString("...");
        nameLabel.setWrapText(false);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label typeLabel = new Label(ext.toUpperCase() + " file");
        typeLabel.getStyleClass().add("recent-meta");
        typeLabel.setMaxWidth(80);

        Label sizeLabel = new Label(FileUtils.formatSize(fileData.get("size")));
        sizeLabel.getStyleClass().add("recent-meta");
        sizeLabel.setMaxWidth(80);

        // --- Buttons ---
        Button linkBtn = new Button();
        FontIcon linkIcon = new FontIcon(FontAwesomeSolid.LINK);
        linkIcon.setIconSize(14);
        linkBtn.setGraphic(linkIcon);
        linkBtn.getStyleClass().add("icon-btn-link");
        linkBtn.setOnMouseClicked(e -> {
            e.consume();
            AlertUtils.showSuccess("Link Copied", "Link copied to clipboard.");
        });

        Button moreBtn = new Button("•••");
        moreBtn.getStyleClass().add("icon-btn-more");

        moreBtn.setOnMouseClicked(e -> {
            e.consume();
            Bounds bounds = moreBtn.localToScreen(moreBtn.getBoundsInLocal());
            showFileMenu(moreBtn, bounds.getMinX(), bounds.getMinY() - 40, truncatedName, fileData);
        });

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#0000000d"));
        shadow.setOffsetY(3.0);
        row.setEffect(shadow);

        row.getChildren().addAll(icon, nameLabel, spacer, typeLabel, sizeLabel, linkBtn, moreBtn);

        return row;
    }

    public static void showFileMenu(Node parent, double x, double y, String truncatedName, Map<String, String> fileData) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        HBox menuBox = new HBox(8);
        menuBox.getStyleClass().add("file-menu-horizontal");
        menuBox.getStylesheets().add(Objects.requireNonNull(FileRowFactory.class.getResource("/css/Dashboard.css")).toExternalForm());

        // 1. Favorite Button
        boolean isFavorite = Boolean.parseBoolean(fileData.getOrDefault("is_favorite", "false"));
        String favTooltip = isFavorite ? "Remove from Favorites" : "Add to Favorites";
        Button favoriteBtn = createMenuButton(FontAwesomeSolid.STAR, favTooltip,
                () -> addToFavorite(fileData, !isFavorite), popup);
        if (isFavorite) favoriteBtn.getStyleClass().add("is_favorite");

        // 2. Share Button (NEW)
        Button shareBtn = createMenuButton(FontAwesomeSolid.SHARE_ALT, "Share File",
                () -> openShareDialog(fileData), popup);

        // 3. Download Button
        Button downloadBtn = createMenuButton(FontAwesomeSolid.DOWNLOAD, "Download",
                () -> System.out.println("Download: " + truncatedName), popup);

        // 4. Add to Folder Button
        Button folderBtn = createMenuButton(FontAwesomeSolid.FOLDER_PLUS, "Add to Folder",
                () -> System.out.println("Add Folder: " + truncatedName), popup);

        menuBox.getChildren().addAll(favoriteBtn, shareBtn, downloadBtn, folderBtn);

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

    // --- NEW: Opens the Share Dialog ---
    private static void openShareDialog(Map<String, String> fileData) {
        try {
            FXMLLoader loader = new FXMLLoader(FileRowFactory.class.getResource("/com/cloudstorage/fx/sharefiledialog.fxml"));
            Parent root = loader.load();

            // Pass the filename/ID to the controller
            ShareDialogController controller = loader.getController();
            controller.setTargetFile(fileData.get("name"));

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setTitle("Share File");
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtils.showError("Error", "Could not open share dialog.");
        }
    }

    private static void addToFavorite(Map<String, String> fileData, boolean makeFavorite) {
        var user = SessionManager.getCurrentUser();
        if (user == null) return;
        String fileName = fileData.get("name");
        String bucketName = fileData.get("bucket");

        fileData.put("is_favorite", String.valueOf(makeFavorite));
        SessionManager.setFavoritesChanged(true);

        Thread dbThread = new Thread(() -> {
            boolean success = FileDAO.setFavorite(user.getId(), fileName, makeFavorite, bucketName);
            if (!success) {
                Platform.runLater(() -> {
                    fileData.put("is_favorite", String.valueOf(!makeFavorite));
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