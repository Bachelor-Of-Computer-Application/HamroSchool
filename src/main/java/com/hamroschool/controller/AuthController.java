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

    @FXML private TextField       usernameField;
    @FXML private PasswordField   passwordField;
    @FXML private ChoiceBox<UserRole> roleChoiceBox;
    @FXML private ToggleGroup     roleToggleGroup;
    @FXML private ToggleButton    studentRoleButton;
    @FXML private ToggleButton    teacherRoleButton;
    @FXML private ToggleButton    adminRoleButton;
    @FXML private ImageView       studentRoleIcon;
    @FXML private ImageView       teacherRoleIcon;
    @FXML private ImageView       adminRoleIcon;
    @FXML private Label           statusLabel;

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
        ToggleButton btn = (ToggleButton) event.getSource();
        roleChoiceBox.setValue(UserRole.valueOf(btn.getUserData().toString()));
        updateRoleButtonStyles();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        UserRole role   = roleChoiceBox.getValue();
        if (role == null) { statusLabel.setText("Select a role before logging in."); return; }
        authService.authenticate(username, password, role)
                .ifPresentOrElse(this::openDashboard,
                        () -> statusLabel.setText("Invalid username, password, or role."));
    }

    private void updateRoleButtonStyles() {
        styleRoleButton(studentRoleButton);
        styleRoleButton(teacherRoleButton);
        styleRoleButton(adminRoleButton);
    }

    private void styleRoleButton(ToggleButton btn) {
        btn.setStyle(btn.isSelected()
                ? "-fx-background-color:#191919;-fx-background-radius:12;-fx-border-color:#191919;-fx-border-radius:12;-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:white;-fx-padding:8 10 8 10;-fx-cursor:hand;"
                : "-fx-background-color:white;-fx-background-radius:12;-fx-border-color:#e3e3e3;-fx-border-radius:12;-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:#202020;-fx-padding:8 10 8 10;-fx-cursor:hand;");
        updateRoleIcon(btn);
    }

    private void updateRoleIcon(ToggleButton btn) {
        String role = btn.getUserData().toString();
        ImageView icon = switch (role) {
            case "TEACHER" -> teacherRoleIcon;
            case "ADMIN"   -> adminRoleIcon;
            default        -> studentRoleIcon;
        };
        if ("STUDENT".equals(role)) {
            icon.setEffect(btn.isSelected() ? null : new ColorAdjust(0, 0, -1, 0));
        } else {
            icon.setEffect(btn.isSelected() ? new ColorAdjust(0, 0, 1, 0) : null);
        }
    }

    private void openDashboard(UserAccount account) {
        SessionContext.getInstance().setCurrentUser(account);
        String path  = switch (account.getRole()) {
            case ADMIN   -> "/com/hamroschool/admin-view.fxml";
            case TEACHER -> "/com/hamroschool/teacher-dashboard-view.fxml";
            default      -> "/com/hamroschool/student-dashboard-view.fxml";
        };
        String title = switch (account.getRole()) {
            case ADMIN   -> "Admin Dashboard";
            case TEACHER -> "Teacher Dashboard";
            default      -> "Student Dashboard";
        };
        SceneSwitcher.showView(usernameField, path, title, SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }
}
