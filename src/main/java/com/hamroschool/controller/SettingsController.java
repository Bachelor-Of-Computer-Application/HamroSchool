package com.hamroschool.controller;

import com.hamroschool.model.auth.UserAccount;
import com.hamroschool.service.AuthService;
import com.hamroschool.service.impl.MongoAuthService;
import com.hamroschool.util.SceneSwitcher;
import com.hamroschool.util.SessionContext;
import com.hamroschool.util.Utils;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class SettingsController {

    private final AuthService authService = MongoAuthService.getInstance();


    @FXML private Label userInitialsLabel;
    @FXML private Label userNameLabel;

    @FXML private Label     avatarLabel;
    @FXML private TextField displayNameField;
    @FXML private Label     profileStatusLabel;

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         securityStatusLabel;

    @FXML private Button logoutButton;


    @FXML
    public void initialize() {
        SessionContext.getInstance().getCurrentUser().ifPresent(user -> {
            String initials = Utils.initials(user.getUsername());
            userInitialsLabel.setText(initials);
            userNameLabel.setText(user.getUsername());
            avatarLabel.setText(initials);
            displayNameField.setText(Utils.formatName(user.getUsername()));
        });
    }


    @FXML
    private void handleSaveProfile() {
        String displayName = displayNameField.getText().trim();
        if (displayName.isBlank()) {
            setProfileStatus("Display name cannot be empty.", false);
            return;
        }
        avatarLabel.setText(Utils.initials(displayName));
        userNameLabel.setText(displayName);
        setProfileStatus("Profile updated successfully.", true);
    }


    @FXML
    private void handleUpdatePassword() {
        String current = currentPasswordField.getText();
        String newPwd  = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (current.isBlank() || newPwd.isBlank() || confirm.isBlank()) {
            setSecurityStatus("All password fields are required.", false);
            return;
        }

        if (!newPwd.equals(confirm)) {
            setSecurityStatus("New passwords do not match.", false);
            return;
        }

        UserAccount user = SessionContext.getInstance().requireCurrentUser();

        boolean currentValid = authService.authenticate(user.getUsername(), current, user.getRole()).isPresent();
        if (!currentValid) {
            setSecurityStatus("Current password is incorrect.", false);
            return;
        }

        boolean updated = authService.updatePassword(user.getUsername(), newPwd);
        if (updated) {
            setSecurityStatus("Password updated successfully.", true);
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
        } else {
            setSecurityStatus("Failed to update password. Please try again.", false);
        }
    }

    @FXML
    private void handleDeleteAccount() {
        SessionContext.getInstance().clear();
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/hello-view.fxml", "Hamro School", SceneSwitcher.LOGIN_WIDTH, SceneSwitcher.LOGIN_HEIGHT);
    }


    @FXML
    private void handleNavDashboard() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/admin-view.fxml", "Admin Dashboard", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }

    @FXML
    private void handleNavAccounts() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/account-view.fxml", "Accounts", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }

    @FXML
    private void handleNavClasses() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/class-view.fxml", "Classes", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }

    @FXML
    private void handleNavTeachers() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/teacher-view.fxml", "Teachers", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }

    @FXML
    private void handleNavStudents() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/student-view.fxml", "Students", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }

    @FXML
    private void handleLogout() {
        SessionContext.getInstance().clear();
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/hello-view.fxml", "Hamro School", SceneSwitcher.LOGIN_WIDTH, SceneSwitcher.LOGIN_HEIGHT);
    }


    private void setProfileStatus(String message, boolean success) {
        profileStatusLabel.setText(message);
        profileStatusLabel.setStyle(
            "-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: " +
            (success ? "#16a34a" : "#dc2626") + ";"
        );
    }

    private void setSecurityStatus(String message, boolean success) {
        securityStatusLabel.setText(message);
        securityStatusLabel.setStyle(
            "-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: " +
            (success ? "#16a34a" : "#dc2626") + ";"
        );
    }
}
