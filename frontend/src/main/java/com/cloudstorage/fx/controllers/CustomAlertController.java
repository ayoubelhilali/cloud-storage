package com.cloudstorage.fx.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

public class CustomAlertController {

    @FXML private VBox alertCard;
    @FXML private FontIcon alertIcon;
    @FXML private Label titleLabel;
    @FXML private Label messageLabel;
    @FXML private HBox buttonBox;
    @FXML private Button okButton;

    private Stage stage;
    private Runnable onConfirmCallback;
    private boolean isConfirmMode = false;

    public void setAlert(String title, String message, String type) {
        titleLabel.setText(title);
        messageLabel.setText(message);

        // Reset classes
        alertCard.getStyleClass().removeAll("type-success", "type-warning", "type-danger");
        alertIcon.getStyleClass().removeAll("icon-success", "icon-warning", "icon-danger");

        switch (type.toLowerCase()) {
            case "success":
                alertCard.getStyleClass().add("type-success");
                alertIcon.getStyleClass().add("icon-success");
                alertIcon.setIconLiteral("fas-check-circle");
                break;
            case "warning":
                alertCard.getStyleClass().add("type-warning");
                alertIcon.getStyleClass().add("icon-warning");
                alertIcon.setIconLiteral("fas-exclamation-triangle");
                break;
            case "danger":
            case "error":
                alertCard.getStyleClass().add("type-danger");
                alertIcon.getStyleClass().add("icon-danger");
                alertIcon.setIconLiteral("fas-times-circle");
                break;
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setConfirmMode(boolean isConfirm, Runnable onConfirm) {
        this.isConfirmMode = isConfirm;
        this.onConfirmCallback = onConfirm;

        if (isConfirm && buttonBox != null && okButton != null) {
            // Clear existing buttons
            buttonBox.getChildren().clear();

            // Create Cancel button
            Button cancelButton = new Button("Cancel");
            cancelButton.getStyleClass().add("alert-btn-cancel");
            cancelButton.setOnAction(e -> closeAlert());

            // Create Confirm button
            Button confirmButton = new Button("Delete");
            confirmButton.getStyleClass().addAll("alert-btn", "alert-btn-danger");
            confirmButton.setOnAction(e -> handleConfirm());

            // Add buttons
            buttonBox.getChildren().addAll(cancelButton, confirmButton);
            buttonBox.setSpacing(10);
        }
    }

    @FXML
    private void closeAlert() {
        Stage stageToClose = stage != null ? stage : (Stage) alertCard.getScene().getWindow();
        stageToClose.close();
    }

    private void handleConfirm() {
        if (onConfirmCallback != null) {
            onConfirmCallback.run();
        }
        closeAlert();
    }
}