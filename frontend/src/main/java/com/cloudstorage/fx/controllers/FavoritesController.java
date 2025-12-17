package com.cloudstorage.fx.controllers;

import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.model.FileMetadata;
import com.cloudstorage.model.User;
import com.cloudstorage.fx.utils.FileUtils;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class FavoritesController {

    @FXML private FlowPane filesGrid;
    @FXML private ScrollPane scrollPane;

    // --- NEW: Stats Labels ---
    @FXML private Label lblFavImages;
    @FXML private Label lblFavVideos;
    @FXML private Label lblFavAudio;
    @FXML private Label lblFavDocs;

    private boolean isFirstLoad = true;

    @FXML
    public void initialize() {
        filesGrid.setHgap(20);
        filesGrid.setVgap(20);
        // Load data initially
        refresh();
    }

    public void refresh() {
        // 1. OPTIMIZATION CHECK
        if (!isFirstLoad && !SessionManager.isFavoritesChanged()) {
            return;
        }

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

                    // B. Create Card
                    VBox card = createCard(file);
                    filesGrid.getChildren().add(card);
                }
            }

            // Update Labels (Clean Numbers)
            lblFavImages.setText(String.valueOf(imgCount));
            lblFavVideos.setText(String.valueOf(vidCount));
            lblFavAudio.setText(String.valueOf(audioCount));
            lblFavDocs.setText(String.valueOf(docCount));

            // 5. UPDATE STATE FLAGS
            isFirstLoad = false;
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
        card.getStyleClass().add("grid-item");
        card.setPrefWidth(180);
        card.setPrefHeight(200);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        // 2. Header with Icon
        StackPane header = new StackPane();
        header.setPrefHeight(120);

        String backgroundColor;
        FontAwesomeSolid iconType;
        String iconColor;

        if (FileUtils.isImage(ext)) {
            backgroundColor = "#d6bcfa";
            iconType = FontAwesomeSolid.CAMERA;
            iconColor = "#805ad5";
        } else if (ext.equals("pdf")) {
            backgroundColor = "#fc8181";
            iconType = FontAwesomeSolid.FILE_PDF;
            iconColor = "#e53e3e";
        } else if (ext.equals("mp3") || ext.equals("wav")) {
            backgroundColor = "#90cdf4";
            iconType = FontAwesomeSolid.MICROPHONE;
            iconColor = "#3182ce";
        } else if (ext.equals("mp4") || ext.equals("avi") || ext.equals("mkv")) {
            backgroundColor = "#fbb6ce";
            iconType = FontAwesomeSolid.VIDEO;
            iconColor = "#d53f8c";
        } else if (ext.equals("zip") || ext.equals("rar") || ext.equals("7z")) {
            backgroundColor = "#fbd38d";
            iconType = FontAwesomeSolid.FILE_ARCHIVE;
            iconColor = "#d69e2e";
        } else if (ext.equals("doc") || ext.equals("docx")) {
            backgroundColor = "#90cdf4";
            iconType = FontAwesomeSolid.FILE_WORD;
            iconColor = "#3182ce";
        } else if (ext.equals("xls") || ext.equals("xlsx")) {
            backgroundColor = "#9ae6b4";
            iconType = FontAwesomeSolid.FILE_EXCEL;
            iconColor = "#38a169";
        } else {
            backgroundColor = "#a0aec0";
            iconType = FontAwesomeSolid.FILE;
            iconColor = "#4a5568";
        }

        header.setStyle("-fx-background-radius: 10 10 0 0; -fx-background-color: " + backgroundColor + ";");

        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(50);
        icon.setIconColor(Color.web(iconColor));

        header.getChildren().add(icon);

        // 3. Details
        VBox details = new VBox(5);
        details.setPadding(new Insets(10));
        details.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(filename);
        nameLabel.getStyleClass().add("file-name");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3748;");
        nameLabel.setWrapText(false);
        nameLabel.setMaxWidth(160);

        Label metaLabel = new Label(FileUtils.formatSize(String.valueOf(file.getFileSize())));
        metaLabel.getStyleClass().add("file-date");
        metaLabel.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");

        details.getChildren().addAll(nameLabel, metaLabel);
        card.getChildren().addAll(header, details);

        return card;
    }
}