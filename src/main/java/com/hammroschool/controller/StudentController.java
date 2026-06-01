package com.hammroschool.controller;

import com.hammroschool.util.SceneSwitcher;
import com.hammroschool.util.SessionContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class StudentController {
    @FXML
    private Label welcomeLabel;

    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome, " + SessionContext.getInstance().requireCurrentUser().getUsername());
    }

    @FXML
    private void handleLogout() {
        SessionContext.getInstance().clear();
        SceneSwitcher.showView(welcomeLabel, "/com/hammroschool/hello-view.fxml", "Hammro School", 920, 720);
    }
}
