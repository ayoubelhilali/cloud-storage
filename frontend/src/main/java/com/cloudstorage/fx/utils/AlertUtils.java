package com.cloudstorage.fx.utils;

import com.cloudstorage.fx.controllers.dialogs.CustomAlertController;
import javafx.animation.PauseTransition; // Import for Timer
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration; // Import for Duration

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

    public static void showConfirmation(String title, String message, Runnable onConfirm) {
        runOnUIThread(() -> showConfirmDialog(title, message, onConfirm));
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
            String fxmlPath = "/com/cloudstorage/fx/dialogs/CustomAlert.fxml";
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

            // --- BLUR EFFECT LOGIC ---
            Window owner = Stage.getWindows().stream()
                    .filter(Window::isShowing)
                    .findFirst()
                    .orElse(null);

            if (owner != null && owner.getScene() != null) {
                alertStage.initOwner(owner);
                Parent mainRoot = owner.getScene().getRoot();
                mainRoot.setEffect(new GaussianBlur(10));

                alertStage.setOnHidden(e -> mainRoot.setEffect(null));
            }

            // =======================================================
            // 🟢 AUTO-CLOSE LOGIC (5 Seconds)
            // =======================================================
            PauseTransition delay = new PauseTransition(Duration.seconds(5));
            delay.setOnFinished(event -> alertStage.close());
            delay.play();
            // =======================================================

            alertStage.centerOnScreen();
            alertStage.showAndWait();

        } catch (IOException e) {
            System.err.println("❌ Error loading CustomAlert.fxml");
            e.printStackTrace();
        }
    }

    private static void showConfirmDialog(String title, String message, Runnable onConfirm) {
        try {
            String fxmlPath = "/com/cloudstorage/fx/dialogs/CustomAlert.fxml";
            URL fxmlLocation = AlertUtils.class.getResource(fxmlPath);

            if (fxmlLocation == null) {
                System.err.println("❌ CRITICAL ERROR: Could not find FXML at: " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            CustomAlertController controller = loader.getController();
            controller.setAlert(title, message, "warning");
            controller.setConfirmMode(true, onConfirm);

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

            // --- BLUR EFFECT LOGIC ---
            Window owner = Stage.getWindows().stream()
                    .filter(Window::isShowing)
                    .findFirst()
                    .orElse(null);

            if (owner != null && owner.getScene() != null) {
                alertStage.initOwner(owner);
                Parent mainRoot = owner.getScene().getRoot();
                mainRoot.setEffect(new GaussianBlur(10));

                alertStage.setOnHidden(e -> mainRoot.setEffect(null));
            }

            // No auto-close for confirmation dialogs
            controller.setStage(alertStage);

            alertStage.centerOnScreen();
            alertStage.showAndWait();

        } catch (IOException e) {
            System.err.println("❌ Error loading CustomAlert.fxml for confirmation");
            e.printStackTrace();
        }
    }
}