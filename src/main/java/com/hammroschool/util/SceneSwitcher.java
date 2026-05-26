package com.hammroschool.util;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class SceneSwitcher {
    private SceneSwitcher() {
    }

    public static void showView(Node source, String fxmlPath, String title, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneSwitcher.class.getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) source.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root, width, height));
            stage.show();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load view: " + fxmlPath, exception);
        }
    }
}