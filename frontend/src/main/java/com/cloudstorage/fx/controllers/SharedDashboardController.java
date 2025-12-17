package com.cloudstorage.fx.controllers;

import com.cloudstorage.controller.ShareController;
import com.cloudstorage.config.SessionManager;
import com.cloudstorage.model.FileMetadata;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.util.List;

public class SharedDashboardController {

    @FXML private VBox sharedFilesContainer; // Linked to FXML
    private final ShareController shareController = new ShareController();

    // Call this method when the view is loaded
    public void loadSharedFiles() {
        if (SessionManager.getCurrentUser() == null) return;

        long userId = SessionManager.getCurrentUser().getId();
        List<FileMetadata> files = shareController.getSharedFiles(userId);

        sharedFilesContainer.getChildren().clear();

        if (files.isEmpty()) {
            sharedFilesContainer.getChildren().add(new Label("No files shared with you."));
        } else {
            for (FileMetadata file : files) {
                sharedFilesContainer.getChildren().add(createRow(file));
            }
        }
    }

    private HBox createRow(FileMetadata file) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(70);
        row.getStyleClass().add("list-item");
        row.setStyle("-fx-padding: 0 20; -fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        Label icon = new Label("📄");
        icon.setStyle("-fx-font-size: 24px;");

        VBox metaBox = new VBox();
        metaBox.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(file.getFilename());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label size = new Label((file.getFileSize()/1024) + " KB");
        metaBox.getChildren().addAll(name, size);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(icon, metaBox, spacer);
        return row;
    }
}