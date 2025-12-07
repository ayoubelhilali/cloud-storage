package com.cloudstorage.fx;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;

import java.io.IOException;

public class DashboardController {

    @FXML
    private Label userLabel;

    @FXML
    private Button logoutButton;

    public void initialize() {
        // Update this to match your name or dynamic data
        if (userLabel != null) {
            userLabel.setText("Hello, Ayoub");
        }
    }

    @FXML
    private void handleLogout() {
        try {
            // Ensure the path is correct: /com/cloudstorage/fx/Login.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/Login.fxml"));
            Parent root = loader.load();

            // Get Current Stage from the button that triggered the event
            Stage stage = (Stage) logoutButton.getScene().getWindow();

            // Swap Scenes
            stage.setScene(new Scene(root));
            stage.setTitle("Cloud Storage - Login");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error: Could not load Login.fxml. Check the file path.");
        }
    }
}