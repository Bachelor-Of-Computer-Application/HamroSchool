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

    // ── FXML nodes ──────────────────────────────────────────────────────────

    @FXML private Label userInitialsLabel;
    @FXML private Label userNameLabel;

    // Profile card
    @FXML private Label     avatarLabel;
    @FXML private TextField displayNameField;
    @FXML private Label     profileStatusLabel;

    // Security card
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         securityStatusLabel;

    @FXML private Button logoutButton;

    // ── Lifecycle ────────────────────────────────────────────────────────────

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

    // ── Profile handlers ─────────────────────────────────────────────────────

    @FXML
    private void handleSaveProfile() {
        String displayName = displayNameField.getText().trim();
        if (displayName.isBlank()) {
            setProfileStatus("Display name cannot be empty.", false);
            return;
        }
        // Display name is cosmetic only — no persistence layer for it yet
        avatarLabel.setText(Utils.initials(displayName));
        userNameLabel.setText(displayName);
        setProfileStatus("Profile updated successfully.", true);
    }

    // ── Security handlers ─────────────────────────────────────────────────────

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

        // Verify the current password is correct before allowing the change
        boolean currentValid = authService.authenticate(user.getUsername(), current, user.getRole()).isPresent();
        if (!currentValid) {
            setSecurityStatus("Current password is incorrect.", false);
            return;
        }

        // Persist the new password to the database
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
        // Guarded by confirmation in a real app — for now just log out and clear session
        SessionContext.getInstance().clear();
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/hello-view.fxml", "Hamro School", 920, 720);
    }

    // ── Nav handlers ──────────────────────────────────────────────────────────

    @FXML
    private void handleNavDashboard() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/admin-view.fxml", "Admin Dashboard", 1280, 860);
    }

    @FXML
    private void handleNavAccounts() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/account-view.fxml", "Accounts", 1280, 860);
    }

    @FXML
    private void handleNavTeachers() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/teacher-view.fxml", "Teachers", 1280, 860);
    }

    @FXML
    private void handleNavStudents() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/student-view.fxml", "Students", 1280, 860);
    }

    @FXML
    private void handleLogout() {
        SessionContext.getInstance().clear();
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/hello-view.fxml", "Hamro School", 920, 720);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
