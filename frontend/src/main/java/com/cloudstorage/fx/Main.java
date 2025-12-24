package com.cloudstorage.fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/cloudstorage/fx/auth/HomePage.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            stage.setTitle("One Cloud");
            stage.setScene(scene);

            // Add app logo (make sure the path is correct)
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo-sm.png")));

            // Set default and min sizes
            stage.setHeight(500);
            stage.setWidth(800);
            stage.setMinWidth(850);
            stage.setMinHeight(550);

            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
