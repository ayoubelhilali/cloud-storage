package com.cloudstorage.fx.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class HomeController {

    @FXML
    private Button registerButton, loginButton;
    @FXML
    private ImageView mainImage;
    @FXML
    private StackPane topLeft, bottomLeft;
    @FXML
    private ImageView logoImage;
    @FXML
    private Label titleLabel, subtitleLabel;
    @FXML
    private VBox rightContainer;

    @FXML
    private void openLoginPage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void OpenRegisterPage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/register.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Create account");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void initialize() {
        setupSmoothHover(loginButton);
        // size dyal l'image ktbdel automatiquement mli ktbdel size dyal lwindow
        mainImage.fitWidthProperty().bind(bottomLeft.widthProperty().multiply(0.9));  // 90% of container width
        mainImage.fitHeightProperty().bind(bottomLeft.heightProperty().multiply(0.9));
        logoImage.fitWidthProperty().bind(topLeft.widthProperty().multiply(0.7));   // 40% of container width
        logoImage.fitHeightProperty().bind(topLeft.heightProperty().multiply(0.7));

        // Bind font size to container width (you can adjust the ratio)
        titleLabel.styleProperty().bind(
                rightContainer.widthProperty().multiply(0.1) // 5% of container width
                        .asString("-fx-font-size: %.0fpx; -fx-font-weight: bold;")
        );

        subtitleLabel.styleProperty().bind(
                rightContainer.widthProperty().multiply(0.05) // 2% of container width
                        .asString("-fx-font-size: %.0fpx;")
        );

        // Bind button width/height to container width
        registerButton.prefWidthProperty().bind(rightContainer.widthProperty().multiply(0.4));
        registerButton.prefHeightProperty().bind(rightContainer.heightProperty().multiply(0.1));

        loginButton.prefWidthProperty().bind(rightContainer.widthProperty().multiply(0.3));
        loginButton.prefHeightProperty().bind(rightContainer.heightProperty().multiply(0.1));
    }
    private void setupSmoothHover(Button button) {
        String originalStyle = button.getStyle();
        String hoverStyle = "-fx-background-color: #3B82F6; -fx-text-fill: white;";

        button.hoverProperty().addListener((obs, oldVal, isHovering) -> {
            if (isHovering) {
                animateStyleTransition(button, originalStyle, hoverStyle, 300);
            } else {
                animateStyleTransition(button, hoverStyle, originalStyle, 300);
            }
        });
    }

    private void animateStyleTransition(Button button, String fromStyle, String toStyle, int duration) {
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.ZERO, new KeyValue(button.styleProperty(), fromStyle)),
                new KeyFrame(Duration.millis(duration), new KeyValue(button.styleProperty(), toStyle))
        );
        timeline.play();
    }

}
