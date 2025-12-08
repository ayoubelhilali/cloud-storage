package com.cloudstorage.fx.controllers;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

public class SharedFilesController {

    // Often useful to inject the root element if you need to manipulate it
    @FXML
    private VBox sharedFilesRoot;

    @FXML
    public void initialize() {
        // This method is called when the FXML loads.
        // In the future, you would load data from a database here
        // and dynamically populate the lists instead of hardcoding them in FXML.
        System.out.println("Shared Files View loaded successfully.");
    }
}