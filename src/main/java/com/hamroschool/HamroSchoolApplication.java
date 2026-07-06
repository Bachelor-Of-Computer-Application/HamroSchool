package com.hamroschool;

import java.io.IOException;

import com.hamroschool.config.AppConfig;
import com.hamroschool.config.MongoClientProvider;
import com.hamroschool.service.DataInitializationService;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HamroSchoolApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        MongoClientProvider.getInstance();
        
        DataInitializationService.getInstance().initializeAllData();

        FXMLLoader fxmlLoader = new FXMLLoader(
                HamroSchoolApplication.class.getResource("/com/hamroschool/hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 920, 720);
        stage.setTitle(AppConfig.getInstance().getAppName());
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        MongoClientProvider.getInstance().close();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
