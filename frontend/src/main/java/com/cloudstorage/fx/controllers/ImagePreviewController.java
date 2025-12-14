package com.cloudstorage.fx.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ImagePreviewController {

    @FXML private ImageView imageView;
    @FXML private VBox iconContainer;
    @FXML private Label typeIcon;
    @FXML private Label typeLabel;

    @FXML private Label fileNameLabel;
    @FXML private Label dateLabel;

    /**
     * Call this for any file type.
     * @param image Pass null if it is not an image.
     */
    public void setFileData(Image image, String fileName, String details) {
        this.fileNameLabel.setText(fileName);
        this.dateLabel.setText(details);

        String ext = getExtension(fileName);

        if (image != null) {
            // IT IS AN IMAGE: Show ImageView, Hide Icon
            imageView.setVisible(true);
            iconContainer.setVisible(false);
            imageView.setImage(image);
        } else {
            // IT IS A FILE: Hide ImageView, Show Icon
            imageView.setVisible(false);
            iconContainer.setVisible(true);

            // Configure the icon based on type
            configureIcon(ext);
        }
    }

    private void configureIcon(String ext) {
        switch (ext) {
            case "pdf":
                typeIcon.setText("📄"); // Or use a FontAwesome glyph
                typeIcon.setStyle("-fx-text-fill: #e53e3e; -fx-font-size: 100px;");
                typeLabel.setText("PDF Document");
                break;
            case "doc":
            case "docx":
                typeIcon.setText("📝");
                typeIcon.setStyle("-fx-text-fill: #3182ce; -fx-font-size: 100px;");
                typeLabel.setText("Word Document");
                break;
            case "zip":
            case "rar":
                typeIcon.setText("📦");
                typeIcon.setStyle("-fx-text-fill: #d69e2e; -fx-font-size: 100px;");
                typeLabel.setText("Compressed Archive");
                break;
            default:
                typeIcon.setText("📁");
                typeIcon.setStyle("-fx-text-fill: #718096; -fx-font-size: 100px;");
                typeLabel.setText(ext.toUpperCase() + " File");
                break;
        }
    }

    private String getExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) return fileName.substring(i + 1).toLowerCase();
        return "";
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) fileNameLabel.getScene().getWindow();
        stage.close();
    }
}