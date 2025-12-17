package com.cloudstorage.fx.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

public class CustomAlertController {

    @FXML private VBox alertCard;
    @FXML private FontIcon alertIcon;
    @FXML private Label titleLabel;
    @FXML private Label messageLabel;

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

    @FXML
    private void closeAlert() {
        Stage stage = (Stage) alertCard.getScene().getWindow();
        stage.close();
    }
}