package com.cloudstorage.fx.controllers;

import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.fx.utils.AlertUtils;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
// NEW IMPORTS FOR JAVACV
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Set;

public class ImagePreviewController {

    @FXML private ImageView imageView;
    @FXML private StackPane videoOverlay; // NEW FXML ID
    @FXML private VBox iconContainer;
    @FXML private FontIcon typeIcon;
    @FXML private Label typeLabel;
    @FXML private Label fileNameLabel;
    @FXML private Label dateLabel;

    private MinioClient minioClient;
    private String bucketName;
    private Runnable onFileDeleted;

    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "avi", "mkv", "mov", "webm", "m4v");

    public void setDeleteContext(MinioClient client, String bucket, Runnable onFileDeleted) {
        this.minioClient = client;
        this.bucketName = bucket;
        this.onFileDeleted = onFileDeleted;
    }

    // --- MAIN DISPLAY LOGIC ---
    public void setFileData(Image image, String fileName, String details) {
        this.fileNameLabel.setText(fileName);
        this.dateLabel.setText(details);
        String ext = getExtension(fileName);

        // Reset visibility
        imageView.setVisible(false);
        videoOverlay.setVisible(false); // Hidden by default
        iconContainer.setVisible(false);

        if (image != null) {
            // 1. Standard Image
            imageView.setVisible(true);
            imageView.setImage(image);
        } else if (ext.equals("pdf")) {
            // 2. PDF
            loadPdfPreview(fileName);
        } else if (VIDEO_EXTENSIONS.contains(ext)) {
            // 3. Video -> Generate Thumbnail + Show Overlay
            loadVideoThumbnail(fileName, ext);
        } else {
            // 4. Fallback Icon
            iconContainer.setVisible(true);
            configureIcon(ext);
        }
    }

    // 🟢 NEW: GENERATE VIDEO THUMBNAIL
    private void loadVideoThumbnail(String fileName, String ext) {
        // Show loading state with video icon
        iconContainer.setVisible(true);
        configureIcon(ext);
        typeLabel.setText("Generating Thumbnail...");

        if (minioClient == null) {
            typeLabel.setText("Connection Error");
            return;
        }

        Task<Image> thumbnailTask = new Task<>() {
            @Override
            protected Image call() throws Exception {
                // Stream video from MinIO
                try (InputStream stream = minioClient.getObject(
                        GetObjectArgs.builder().bucket(bucketName).object(fileName).build())) {

                    // Use FFmpeg to grab the first frame
                    // We use the InputStream directly. FFmpeg will read the beginning of the stream.
                    try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(stream)) {
                        grabber.start();

                        // Grab the very first image frame
                        Frame frame = grabber.grabImage();

                        if (frame != null) {
                            Java2DFrameConverter converter = new Java2DFrameConverter();
                            BufferedImage bufferedImage = converter.convert(frame);
                            grabber.stop();
                            return SwingFXUtils.toFXImage(bufferedImage, null);
                        } else {
                            grabber.stop();
                            throw new Exception("Could not grab frame from video stream.");
                        }
                    }
                }
            }
        };

        thumbnailTask.setOnSucceeded(e -> {
            // 1. Hide placeholder icon
            iconContainer.setVisible(false);
            // 2. Show the generated thumbnail image
            imageView.setVisible(true);
            imageView.setImage(thumbnailTask.getValue());
            // 3. SHOW THE PLAY OVERLAY SIGN
            videoOverlay.setVisible(true);
        });

        thumbnailTask.setOnFailed(e -> {
            System.err.println("Thumbnail Generation Failed: " + e.getSource().getException().getMessage());
            // Keep the generic video icon visible if generation fails
            typeLabel.setText("Preview Unavailable");
        });

        new Thread(thumbnailTask).start();
    }

    // ... [loadPdfPreview is the same as before] ...
    private void loadPdfPreview(String fileName) {
        iconContainer.setVisible(true);
        configureIcon("pdf");
        typeLabel.setText("Rendering PDF...");

        if (minioClient == null) return;

        Task<Image> pdfTask = new Task<>() {
            @Override
            protected Image call() throws Exception {
                try (InputStream stream = minioClient.getObject(
                        GetObjectArgs.builder().bucket(bucketName).object(fileName).build())) {
                    try (PDDocument document = PDDocument.load(stream)) {
                        PDFRenderer renderer = new PDFRenderer(document);
                        BufferedImage bufferedImage = renderer.renderImageWithDPI(0, 150);
                        return SwingFXUtils.toFXImage(bufferedImage, null);
                    }
                }
            }
        };

        pdfTask.setOnSucceeded(e -> {
            iconContainer.setVisible(false);
            imageView.setVisible(true);
            imageView.setImage(pdfTask.getValue());
        });

        pdfTask.setOnFailed(e -> {
            typeLabel.setText("Preview Unavailable");
        });

        new Thread(pdfTask).start();
    }


    // ... [configureIcon is the same] ...
    private void configureIcon(String ext) {
        typeIcon.setIconSize(100);
        switch (ext) {
            case "pdf":
                typeIcon.setIconLiteral("fas-file-pdf"); typeIcon.setIconColor(Color.web("#e53e3e")); typeLabel.setText("PDF Document"); break;
            // Add video types here for the loading state/fallback
            case "mp4": case "avi": case "mkv": case "mov": case "webm":
                typeIcon.setIconLiteral("fas-video"); typeIcon.setIconColor(Color.web("#d63031")); typeLabel.setText("Video File"); break;
            case "doc": case "docx":
                typeIcon.setIconLiteral("fas-file-word"); typeIcon.setIconColor(Color.web("#3182ce")); typeLabel.setText("Word Document"); break;
            case "xls": case "xlsx":
                typeIcon.setIconLiteral("fas-file-excel"); typeIcon.setIconColor(Color.web("#38a169")); typeLabel.setText("Spreadsheet"); break;
            case "zip": case "rar":
                typeIcon.setIconLiteral("fas-file-archive"); typeIcon.setIconColor(Color.web("#d69e2e")); typeLabel.setText("Archive"); break;
            default:
                typeIcon.setIconLiteral("fas-file"); typeIcon.setIconColor(Color.web("#718096")); typeLabel.setText(ext.toUpperCase() + " File"); break;
        }
    }

    // ... [Delete logic remains the same as previous answers] ...
    @FXML
    private void handleDeleteButtonAction() {
        String fileName = fileNameLabel.getText();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete File");
        alert.setHeaderText("Delete '" + fileName + "'?");
        alert.setContentText("This action cannot be undone.");
        alert.initStyle(StageStyle.UNDECORATED);
        ButtonType btnYes = new ButtonType("Delete", ButtonBar.ButtonData.YES);
        ButtonType btnNo = new ButtonType("Cancel", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(btnYes, btnNo);
        alert.showAndWait().ifPresent(type -> {
            if (type == btnYes) handleRemove(fileName);
        });
    }

    private void handleRemove(String filename) {
        if (minioClient == null || bucketName == null) {
            AlertUtils.showError("Error", "System is not connected properly.");
            return;
        }

        Task<Void> deleteTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filename).build());
                if (SessionManager.getCurrentUser() != null) {
                    FileDAO.deleteFileRecord(filename, (int) SessionManager.getCurrentUser().getId());
                }
                return null;
            }
        };

        deleteTask.setOnSucceeded(event -> Platform.runLater(() -> {
            if (onFileDeleted != null) onFileDeleted.run();
            closeWindow();
            AlertUtils.showSuccess("Deleted", "File deleted successfully.");
        }));

        deleteTask.setOnFailed(event -> {
            Throwable e = deleteTask.getException();
            Platform.runLater(() -> AlertUtils.showError("Delete Failed", e.getMessage()));
        });

        new Thread(deleteTask).start();
    }

    @FXML
    private void closeWindow() {
        if (fileNameLabel.getScene() != null) {
            Stage stage = (Stage) fileNameLabel.getScene().getWindow();
            stage.close();
        }
    }

    private String getExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) return fileName.substring(i + 1).toLowerCase();
        return "";
    }
}