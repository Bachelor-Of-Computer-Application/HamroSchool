package com.hammroschool.controller;

import com.hammroschool.model.auth.UserAccount;
import com.hammroschool.model.auth.UserRole;
import com.hammroschool.service.AuthService;
import com.hammroschool.service.impl.InMemoryAuthService;
import com.hammroschool.util.SceneSwitcher;
import com.hammroschool.util.SessionContext;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AuthController {
    private final AuthService authService = InMemoryAuthService.getInstance();

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ChoiceBox<UserRole> roleChoiceBox;

    @FXML
    private Label statusLabel;

    @FXML
    public void initialize() {
        roleChoiceBox.getItems().setAll(UserRole.values());
        roleChoiceBox.getSelectionModel().select(UserRole.STUDENT);
        statusLabel.setText("Use admin / admin123 to sign in first.");
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        UserRole selectedRole = roleChoiceBox.getValue();

        if (selectedRole == null) {
            statusLabel.setText("Select a role before logging in.");
            return;
        }

        authService.authenticate(username, password, selectedRole)
                .ifPresentOrElse(this::openDashboard, () ->
                        statusLabel.setText("Invalid username, password, or role."));
    }

    private void openDashboard(UserAccount account) {
        SessionContext.getInstance().setCurrentUser(account);

        String fxmlPath;
        String title;
        if (account.getRole() == UserRole.ADMIN) {
            fxmlPath = "/com/hammroschool/admin-view.fxml";
            title = "Admin Dashboard";
        } else if (account.getRole() == UserRole.TEACHER) {
            fxmlPath = "/com/hammroschool/teacher-view.fxml";
            title = "Teacher Dashboard";
        } else {
            fxmlPath = "/com/hammroschool/student-view.fxml";
            title = "Student Dashboard";
        }

        SceneSwitcher.showView(usernameField, fxmlPath, title, 980, 640);
    }
}
