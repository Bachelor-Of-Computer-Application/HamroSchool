package com.hamroschool.controller;

import com.hamroschool.model.auth.UserAccount;
import com.hamroschool.model.auth.UserRole;
import com.hamroschool.service.AuthService;
import com.hamroschool.service.impl.MongoAuthService;
import com.hamroschool.util.SceneSwitcher;
import com.hamroschool.util.SessionContext;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;

public class AuthController {
    private final AuthService authService = MongoAuthService.getInstance();

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ChoiceBox<UserRole> roleChoiceBox;

    @FXML
    private ToggleGroup roleToggleGroup;

    @FXML
    private ToggleButton studentRoleButton;

    @FXML
    private ToggleButton teacherRoleButton;

    @FXML
    private ToggleButton adminRoleButton;

    @FXML
    private ImageView studentRoleIcon;

    @FXML
    private ImageView teacherRoleIcon;

    @FXML
    private ImageView adminRoleIcon;

    @FXML
    private Label statusLabel;

    @FXML
    public void initialize() {
        roleChoiceBox.getItems().setAll(UserRole.values());
        roleChoiceBox.setValue(UserRole.STUDENT);
        roleToggleGroup.selectToggle(studentRoleButton);
        statusLabel.setText("");
        updateRoleButtonStyles();
    }

    @FXML
    private void selectRole(ActionEvent event) {
        ToggleButton selectedButton = (ToggleButton) event.getSource();
        roleChoiceBox.setValue(UserRole.valueOf(selectedButton.getUserData().toString()));
        updateRoleButtonStyles();
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

    private void updateRoleButtonStyles() {
        styleRoleButton(studentRoleButton);
        styleRoleButton(teacherRoleButton);
        styleRoleButton(adminRoleButton);
    }

    private void styleRoleButton(ToggleButton button) {
        if (button.isSelected()) {
            button.setStyle("-fx-background-color: #191919; -fx-background-radius: 12; -fx-border-color: #191919; -fx-border-radius: 12; -fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: white; -fx-padding: 8 10 8 10; -fx-cursor: hand;");
        } else {
            button.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e3e3e3; -fx-border-radius: 12; -fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #202020; -fx-padding: 8 10 8 10; -fx-cursor: hand;");
        }
        updateRoleIcon(button);
    }

    private void updateRoleIcon(ToggleButton button) {
        ImageView icon = switch (button.getUserData().toString()) {
            case "TEACHER" -> teacherRoleIcon;
            case "ADMIN" -> adminRoleIcon;
            default -> studentRoleIcon;
        };

        if ("STUDENT".equals(button.getUserData().toString())) {
            icon.setEffect(button.isSelected() ? null : new ColorAdjust(0, 0, -1, 0));
        } else {
            icon.setEffect(button.isSelected() ? new ColorAdjust(0, 0, 1, 0) : null);
        }
    }

    private void openDashboard(UserAccount account) {
        SessionContext.getInstance().setCurrentUser(account);

        String fxmlPath;
        String title;
        switch (account.getRole()) {
            case ADMIN -> {
                fxmlPath = "/com/hamroschool/admin-view.fxml";
                title = "Admin Dashboard";
                SceneSwitcher.showView(usernameField, fxmlPath, title, 1280, 860);
                return;
            }
            case TEACHER -> {
                fxmlPath = "/com/hamroschool/teacher-dashboard-view.fxml";
                title = "Teacher Dashboard";
            }
            default -> {
                fxmlPath = "/com/hamroschool/student-dashboard-view.fxml";
                title = "Student Dashboard";
            }
        }

        SceneSwitcher.showView(usernameField, fxmlPath, title, 1280, 860);
    }
}
