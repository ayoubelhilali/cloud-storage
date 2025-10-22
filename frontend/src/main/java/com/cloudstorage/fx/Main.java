package main.java.com.cloudstorage.fx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        // Create a simple layout with a label
        StackPane root = new StackPane();
        root.getChildren().add(new Label("Welcome to Cloud Storage!"));

        // Create a scene with the layout
        Scene scene = new Scene(root, 400, 200); // width=400, height=200

        // Set stage properties
        stage.setTitle("Cloud Storage Login");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
