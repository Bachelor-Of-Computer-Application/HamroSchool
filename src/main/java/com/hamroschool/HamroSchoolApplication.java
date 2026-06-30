package com.hamroschool;

import java.io.IOException;

import com.hamroschool.config.AppConfig;
import com.hamroschool.config.MongoClientProvider;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HamroSchoolApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Warm up the MongoDB connection early (fails fast if not running)
        MongoClientProvider.getInstance();

        FXMLLoader fxmlLoader = new FXMLLoader(
                HamroSchoolApplication.class.getResource("/com/hamroschool/hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 920, 720);
        stage.setTitle(AppConfig.getInstance().getAppName());
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        // Close MongoDB connection cleanly when the JavaFX window closes
        MongoClientProvider.getInstance().close();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
