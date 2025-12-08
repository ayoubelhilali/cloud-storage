package com.cloudstorage.fx.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;

public class FavoritesController {

    @FXML
    private FlowPane filesGrid;

    @FXML
    public void initialize() {
        // Create 10 dummy items to simulate the grid
        for (int i = 1; i <= 10; i++) {
            addMockItem("IMG_0" + i, "July 01, 2022");
        }
    }

    private void addMockItem(String name, String date) {
        // 1. Create the Card Container
        VBox card = new VBox();
        card.getStyleClass().add("grid-item");
        card.setPrefWidth(200);
        card.setPrefHeight(180);

        // 2. Create Image Placeholder (Replace with ImageView in real app)
        Region imagePlaceholder = new Region();
        imagePlaceholder.setPrefHeight(120);
        imagePlaceholder.getStyleClass().add("image-placeholder");
        // Random color for variety
        String color = (Math.random() > 0.5) ? "#81ecec" : "#fab1a0";
        imagePlaceholder.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 15 15 0 0;");

        // 3. Create Details Section
        VBox details = new VBox(5);
        details.setPadding(new Insets(10));

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("file-name");

        Label dateLabel = new Label(date);
        dateLabel.getStyleClass().add("file-date");

        details.getChildren().addAll(nameLabel, dateLabel);

        // 4. Assemble and Add to Grid
        card.getChildren().addAll(imagePlaceholder, details);
        filesGrid.getChildren().add(card);
    }
}