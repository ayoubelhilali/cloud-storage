package com.cloudstorage.fx.utils;

import com.cloudstorage.fx.controllers.CustomAlertController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur; // Import for Blur
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window; // Import for finding the main window

import java.io.IOException;
import java.net.URL;

public class AlertUtils {

    public static void showSuccess(String title, String message) {
        runOnUIThread(() -> show(title, message, "success"));
    }

    public static void showWarning(String title, String message) {
        runOnUIThread(() -> show(title, message, "warning"));
    }

    public static void showError(String title, String message) {
        runOnUIThread(() -> show(title, message, "danger"));
    }

    private static void runOnUIThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    private static void show(String title, String message, String type) {
        try {
            String fxmlPath = "/com/cloudstorage/fx/CustomAlert.fxml";
            URL fxmlLocation = AlertUtils.class.getResource(fxmlPath);

            if (fxmlLocation == null) {
                System.err.println("❌ CRITICAL ERROR: Could not find FXML at: " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            CustomAlertController controller = loader.getController();
            controller.setAlert(title, message, type);

            Stage alertStage = new Stage();
            alertStage.initStyle(StageStyle.TRANSPARENT);
            alertStage.initModality(Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            alertStage.setScene(scene);

            // CSS Loading
            String cssPath = "/css/alert.css";
            URL cssLocation = AlertUtils.class.getResource(cssPath);
            if (cssLocation != null) {
                scene.getStylesheets().add(cssLocation.toExternalForm());
            }

            // =======================================================
            // 🟢 BLUR EFFECT LOGIC
            // =======================================================

            // 1. Find the Main Window (The "Owner")
            // We look for the first active window that isn't this new alert
            Window owner = Stage.getWindows().stream()
                    .filter(Window::isShowing)
                    .findFirst()
                    .orElse(null);

            if (owner != null && owner.getScene() != null) {
                alertStage.initOwner(owner); // Keep alert on top of main window

                // 2. Apply Blur to the Main Window's Root Node
                Parent mainRoot = owner.getScene().getRoot();
                mainRoot.setEffect(new GaussianBlur(10)); // Radius 10 for a nice soft blur

                // 3. Remove Blur when Alert Closes
                alertStage.setOnHidden(e -> {
                    mainRoot.setEffect(null);
                });
            }
            // =======================================================

            alertStage.centerOnScreen();
            alertStage.showAndWait();

        } catch (IOException e) {
            System.err.println("❌ Error loading CustomAlert.fxml");
            e.printStackTrace();
        }
    }
}