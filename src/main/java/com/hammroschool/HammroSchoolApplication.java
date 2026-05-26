package com.hammroschool;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HammroSchoolApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HammroSchoolApplication.class.getResource("/com/hammroschool/hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 980, 640);
        stage.setTitle("Hammro School");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
